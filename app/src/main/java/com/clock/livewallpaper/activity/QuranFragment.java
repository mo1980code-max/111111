package com.clock.livewallpaper.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranSettings;
import com.clock.livewallpaper.quran.QuranThemeColors;
import com.clock.livewallpaper.quran.SurahIndex;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One page of the reader's {@code ViewPager2}: a whole surah, read offline from the bundled
 * {@code quran.ar.uthmani.db}.
 *
 * <h2>Contract with the host</h2>
 * The host ({@link QuranActivity}) creates pages with {@link #newInstance(int, int, int)} and
 * implements {@link Listener} so it can push live preference changes (font size, theme) into every
 * instantiated page. Pages never talk to each other; swiping is owned by the pager.
 *
 * <h2>Scroll restoration</h2>
 * Three targets, in priority order:
 * <ol>
 *   <li>The offset saved by this fragment's own {@code onSaveInstanceState} — survives rotation.</li>
 *   <li>{@link #ARG_SCROLL_Y} — a one-shot pixel offset, used by the "Continue Reading" bookmark.</li>
 *   <li>{@link #ARG_TARGET_AYAH} — a one-shot jump to an ayah, used by search results.</li>
 * </ol>
 * The one-shot targets are consumed after the first successful render, so scrolling around a page
 * and returning to it never yanks the reader back.
 *
 * <h2>Threading</h2>
 * Each page owns a single worker thread for its database read; neighbouring pages therefore load in
 * parallel instead of queueing behind each other. {@link QuranDatabaseHelper} serialises the
 * first-run install itself. Results posted to a detached fragment are dropped.
 */
public final class QuranFragment extends Fragment {

    /** 1-based surah number this page renders, 1 to 114. */
    public static final String ARG_SURAH_ID = "surah_id";
    /** One-shot vertical scroll offset in px to apply after the first render; 0 means "top". */
    public static final String ARG_SCROLL_Y = "initial_scroll_y";
    /** One-shot 1-based ayah to bring into view after the first render; 1 means "top". */
    public static final String ARG_TARGET_AYAH = "target_ayah";

    private static final String STATE_SCROLL_Y = "scroll_y";
    private static final String STATE_LOADED = "loaded";

    /** Uthmani script face shipped at {@code assets/fonts/quran_font.ttf} (Amiri Quran, OFL 1.1). */
    private static final String FONT_ASSET_PATH = "fonts/quran_font.ttf";

    private static final String TAG = "QuranFragment";

    /** Shared by every page: the face is immutable once loaded from assets. */
    private static volatile Typeface cachedTypeface;

    /** Host activity receives attach/detach notifications to keep its live-page registry current. */
    public interface Listener {
        /** @param fragment the page being attached, already bound to this host */
        void onPageFragmentAttached(@NonNull QuranFragment fragment);

        /** @param fragment the page leaving this host */
        void onPageFragmentDetached(@NonNull QuranFragment fragment);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;

    private Listener listener;
    private QuranDatabaseHelper database;

    private View pageRoot;
    private ScrollView scrollView;
    private TextView textView;
    private View loadingContainer;
    private ProgressBar progressBar;
    private TextView statusView;
    private Button retryButton;

    /** One-shot targets from the arguments; cleared once applied. */
    private int pendingScrollY;
    private int pendingAyah;
    private boolean contentLoaded;
    /** True while the status label carries an error, so a theme toggle keeps it red, not grey. */
    private boolean showingError;

    /**
     * @param surahId       1-based surah number, 1 to 114
     * @param initialScrollY one-shot scroll offset in px, 0 to start at the top
     * @param targetAyah    one-shot 1-based ayah target, 1 to start at the top
     */
    @NonNull
    public static QuranFragment newInstance(int surahId, int initialScrollY, int targetAyah) {
        QuranFragment fragment = new QuranFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SURAH_ID, surahId);
        args.putInt(ARG_SCROLL_Y, Math.max(0, initialScrollY));
        args.putInt(ARG_TARGET_AYAH, Math.max(1, targetAyah));
        fragment.setArguments(args);
        return fragment;
    }

    /** @return the surah this page renders, or -1 when the arguments are missing. */
    public int getSurahId() {
        Bundle args = getArguments();
        return args == null ? -1 : args.getInt(ARG_SURAH_ID, -1);
    }

    /** @return the page's current vertical scroll offset in px; 0 before the body exists. */
    public int getCurrentScrollY() {
        return scrollView == null ? 0 : Math.max(0, scrollView.getScrollY());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
            listener.onPageFragmentAttached(this);
        }
        database = new QuranDatabaseHelper(context);
    }

    @Override
    public void onDetach() {
        if (listener != null) {
            listener.onPageFragmentDetached(this);
            listener = null;
        }
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        pendingScrollY = args == null ? 0 : Math.max(0, args.getInt(ARG_SCROLL_Y, 0));
        pendingAyah = args == null ? 1 : Math.max(1, args.getInt(ARG_TARGET_AYAH, 1));
        if (savedInstanceState != null) {
            // Rotation: the instance state beats the one-shot targets, which were already applied.
            pendingScrollY = Math.max(0, savedInstanceState.getInt(STATE_SCROLL_Y, 0));
            pendingAyah = 1;
            contentLoaded = savedInstanceState.getBoolean(STATE_LOADED, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quran_page, container, false);
        pageRoot = view;
        scrollView = view.findViewById(R.id.quran_page_scroll);
        textView = view.findViewById(R.id.quran_page_text);
        loadingContainer = view.findViewById(R.id.quran_page_loading);
        progressBar = view.findViewById(R.id.quran_page_progress);
        statusView = view.findViewById(R.id.quran_page_status);
        retryButton = view.findViewById(R.id.quran_page_retry);

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSurah();
            }
        });

        applyThemeColors();
        applyFontSize(QuranSettings.get(view.getContext()).getFontSize());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!applyQuranFont()) {
            // Reported on screen instead of silently substituting a face that cannot shape
            // Uthmani marks; matches the behaviour of the pre-pager reader.
            return;
        }
        // Fresh load, or re-render after rotation (cheap when the surah is still in the helper's
        // cache). The scroll target to restore comes from onCreate().
        loadSurah();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SCROLL_Y, getCurrentScrollY());
        outState.putBoolean(STATE_LOADED, contentLoaded);
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------------
    // Live preference application, called by the host on +/- taps and theme toggles
    // ---------------------------------------------------------------------------------------------

    /** Applies the persisted body size to this page's Arabic text. */
    public void applyFontSize(float sizeSp) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        }
    }

    /** Repaints this page with the palette for the current light/dark preference. */
    public void applyThemeColors() {
        if (pageRoot == null) {
            return;
        }
        Context context = pageRoot.getContext();
        pageRoot.setBackgroundColor(QuranThemeColors.background(context));
        if (textView != null) {
            textView.setTextColor(QuranThemeColors.text(context));
        }
        if (statusView != null) {
            statusView.setTextColor(showingError
                    ? QuranThemeColors.error(context)
                    : QuranThemeColors.header(context));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------------

    private boolean applyQuranFont() {
        try {
            Typeface typeface = cachedTypeface;
            if (typeface == null) {
                typeface = Typeface.createFromAsset(requireContext().getAssets(), FONT_ASSET_PATH);
                if (typeface == null) {
                    throw new IllegalStateException("Typeface.createFromAsset returned null");
                }
                cachedTypeface = typeface;
            }
            textView.setTypeface(typeface);
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not load " + FONT_ASSET_PATH, error);
            showError(getString(R.string.quran_font_error), false);
            return false;
        }
    }

    /** Reads {@code arabic_text} for this page's surah on the page's worker thread. */
    private void loadSurah() {
        final int surahId = getSurahId();
        if (!SurahIndex.isValid(surahId)) {
            showError(getString(R.string.quran_invalid_surah), false);
            return;
        }
        showLoading();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Installs the asset on first use, then returns the cached, formatted surah.
                    final String surahText = database.getSurahText(surahId);
                    postToUi(new Runnable() {
                        @Override
                        public void run() {
                            showContent(surahText);
                        }
                    });
                } catch (IOException | RuntimeException error) {
                    Log.e(TAG, "Could not read surah " + surahId + " from the offline database", error);
                    postToUi(new Runnable() {
                        @Override
                        public void run() {
                            showError(getString(R.string.quran_load_error), true);
                        }
                    });
                }
            }
        });
    }

    private void postToUi(@NonNull Runnable action) {
        if (destroyed) {
            return;
        }
        final android.app.Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (destroyed || !isAdded()) {
                    return;
                }
                action.run();
            }
        });
    }

    private void showContent(String surahText) {
        contentLoaded = true;
        textView.setText(surahText);
        loadingContainer.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        restoreScrollPosition();
    }

    /**
     * Scrolls to the saved offset, the bookmarked offset, the requested ayah, or the top — and
     * consumes the one-shot targets so later renders start at the top.
     *
     * <p>Runs in a {@code View#post(Runnable)} so it wins over the layout and focus passes,
     * including the auto-scroll a selectable {@code TextView} can trigger.
     */
    private void restoreScrollPosition() {
        final int offsetToRestore = pendingScrollY;
        final int ayahToReach = pendingAyah;
        pendingScrollY = 0;
        pendingAyah = 1;

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (destroyed || scrollView == null) {
                    return;
                }
                if (offsetToRestore > 0) {
                    // Never scroll past the bottom of a short surah.
                    int maxScroll = Math.max(0, textView.getBottom() - scrollView.getHeight()
                            + scrollView.getPaddingBottom());
                    scrollView.scrollTo(0, Math.min(offsetToRestore, maxScroll));
                    return;
                }
                int offset = ayahToReach > 1 ? findAyahOffset(ayahToReach) : 0;
                if (offset <= 0) {
                    scrollView.scrollTo(0, 0);
                    return;
                }
                Layout layout = textView.getLayout();
                if (layout == null || offset > layout.getText().length()) {
                    scrollView.scrollTo(0, 0);
                    return;
                }
                int line = layout.getLineForOffset(offset);
                scrollView.scrollTo(0, Math.max(0, layout.getLineTop(line)));
            }
        });
    }

    /**
     * Locates an ayah inside the rendered surah by its ornate marker, e.g. {@code ﴿٢٥٥﴾}.
     *
     * @return the character offset where the ayah's text begins, or -1 when it cannot be located
     */
    private int findAyahOffset(int ayahNumber) {
        if (ayahNumber <= 1) {
            // Ayah 1 means "top of the surah", which keeps any basmallah header on screen.
            return 0;
        }
        CharSequence rendered = textView.getText();
        if (rendered == null || rendered.length() == 0) {
            return -1;
        }
        String body = rendered.toString();
        String marker = QuranDatabaseHelper.ayahMarker(ayahNumber);
        int markerIndex = body.indexOf(marker);
        if (markerIndex < 0) {
            return -1;
        }

        // An ayah begins immediately after the previous ayah's marker. Markers are unique within a
        // surah, so scanning backwards from this one cannot land on the wrong ayah.
        String previousMarker = QuranDatabaseHelper.ayahMarker(ayahNumber - 1);
        int previousIndex = body.lastIndexOf(previousMarker, markerIndex);
        if (previousIndex < 0) {
            // Fall back to the marker itself: still scrolls the requested ayah into view.
            return markerIndex;
        }
        return previousIndex + previousMarker.length();
    }

    private void showLoading() {
        showingError = false;
        scrollView.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        statusView.setTextColor(QuranThemeColors.header(statusView.getContext()));
        statusView.setText(R.string.quran_loading);
    }

    /**
     * @param message    user-facing, actionable explanation
     * @param allowRetry whether the Retry button is shown
     */
    private void showError(String message, boolean allowRetry) {
        showingError = true;
        scrollView.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        statusView.setTextColor(QuranThemeColors.error(statusView.getContext()));
        statusView.setText(message);
        retryButton.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
    }
}
