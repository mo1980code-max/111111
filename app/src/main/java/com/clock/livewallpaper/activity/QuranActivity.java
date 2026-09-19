package com.clock.livewallpaper.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranMetadata;
import com.clock.livewallpaper.quran.SurahIndex;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quran reading screen: one whole surah at a time, read entirely offline from the bundled
 * {@code quran.ar.uthmani.db}.
 *
 * <h2>Entry contract</h2>
 * <pre>
 * Intent intent = new Intent(this, QuranActivity.class);
 * intent.putExtra(QuranActivity.EXTRA_SURAH_ID, 2);        // 1-114, defaults to 1
 * intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, "البقرة"); // optional, see below
 * intent.putExtra(QuranActivity.EXTRA_AYAH, 255);          // optional scroll target
 * startActivity(intent);
 * </pre>
 * {@code surah_id} is the single source of truth: it drives the database query, the header and the
 * Next/Previous navigation. {@code surah_name} is used for the window title; the on-screen header is
 * rendered from {@link SurahIndex} instead, because the name has to stay correct after the user
 * navigates to a surah no caller ever passed in. A mismatch between the two is logged, not displayed.
 *
 * <h2>Navigation</h2>
 * "Next Surah" and "Previous Surah" re-run the query and rebind the same views. No second activity,
 * no intent, no back-stack growth: pressing Back leaves the reader entirely rather than walking back
 * through 100 surahs. Formatted surahs are cached in {@link QuranDatabaseHelper}, so paging between
 * neighbours is effectively free. Both buttons are disabled at the ends of the Mushaf.
 *
 * <h2>Threading</h2>
 * The first-run asset copy and every query run on a single worker thread. Results posted to a
 * destroyed activity are dropped, so rotating mid-load cannot crash.
 */
public final class QuranActivity extends Activity {

    /** 1-based surah number to open, 1 to 114. */
    public static final String EXTRA_SURAH_ID = "surah_id";
    /** Display name of the surah, paired with {@link #EXTRA_SURAH_ID}. */
    public static final String EXTRA_SURAH_NAME = "surah_name";
    /** Optional 1-based ayah to scroll to once the surah has loaded. */
    public static final String EXTRA_AYAH = "ayah";

    /** Accepted for callers written against the earlier reader; prefer {@link #EXTRA_SURAH_ID}. */
    private static final String LEGACY_EXTRA_SURAH = "quran.surah";
    /** Accepted for callers written against the earlier reader; prefer {@link #EXTRA_AYAH}. */
    private static final String LEGACY_EXTRA_AYAH = "quran.ayah";

    /** Uthmani script face shipped at {@code assets/fonts/quran_font.ttf} (Amiri Quran, OFL 1.1). */
    private static final String FONT_ASSET_PATH = "fonts/quran_font.ttf";

    private static final String TAG = "QuranActivity";
    private static final String STATE_SURAH = "current_surah";
    private static final String STATE_SCROLL = "scroll_y";
    private static final int DEFAULT_SURAH = 1;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;

    private QuranDatabaseHelper database;
    private Typeface quranTypeface;

    /** Surah currently on screen, or being loaded. */
    private int currentSurah = DEFAULT_SURAH;
    /** Ayah to scroll to after the next successful load; 1 means "top of the surah". */
    private int scrollTargetAyah = 1;
    /** Exact scroll offset to restore after a configuration change, or -1 for "use the ayah target". */
    private int restoredScroll = -1;

    private TextView surahNameView;
    private TextView juzView;
    private TextView positionView;
    private TextView pageView;
    private TextView textView;
    private TextView statusView;
    private ScrollView scrollView;
    private View loadingContainer;
    private ProgressBar progressBar;
    private Button retryButton;
    private Button previousButton;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran);

        bindViews();
        EdgeToEdgeInsets.apply(this, findViewById(R.id.quran_root), getColor(R.color.quran_background));

        database = new QuranDatabaseHelper(this);

        if (!loadQuranFont()) {
            return; // Error is already on screen; nothing else can render correctly without the face.
        }

        Intent intent = getIntent();
        int requestedSurah = readRequestedSurah(intent);
        if (savedInstanceState != null) {
            // Returning to a recreated activity: same surah, same scroll offset, no ayah jump.
            requestedSurah = savedInstanceState.getInt(STATE_SURAH, requestedSurah);
            restoredScroll = savedInstanceState.getInt(STATE_SCROLL, 0);
            scrollTargetAyah = 1;
        } else {
            restoredScroll = -1;
            int requestedAyah = intent.getIntExtra(EXTRA_AYAH,
                    intent.getIntExtra(LEGACY_EXTRA_AYAH, 1));
            scrollTargetAyah = clampAyah(requestedAyah, requestedSurah);
        }

        if (!SurahIndex.isValid(requestedSurah)) {
            showError(getString(R.string.quran_invalid_surah), false);
            return;
        }
        currentSurah = requestedSurah;
        applyWindowTitle(requestedSurah, intent.getStringExtra(EXTRA_SURAH_NAME));

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigate(-1);
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigate(+1);
            }
        });
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadSurah(currentSurah);
            }
        });

        loadSurah(currentSurah);
    }

    private void bindViews() {
        surahNameView = findViewById(R.id.quran_surah_name);
        juzView = findViewById(R.id.quran_juz);
        positionView = findViewById(R.id.quran_position);
        pageView = findViewById(R.id.quran_page);
        textView = findViewById(R.id.quran_text);
        statusView = findViewById(R.id.quran_status);
        scrollView = findViewById(R.id.quran_scroll);
        loadingContainer = findViewById(R.id.quran_loading_container);
        progressBar = findViewById(R.id.quran_progress);
        retryButton = findViewById(R.id.quran_retry_btn);
        previousButton = findViewById(R.id.quran_prev_btn);
        nextButton = findViewById(R.id.quran_next_btn);

        ImageButton back = findViewById(R.id.quran_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * Loads the bundled Uthmani face from assets and applies it to every Arabic view.
     *
     * <p>Applied to the body and to the Arabic header labels (surah name, Juz, page). The bundled
     * Amiri Quran face covers all 114 surah names, the Arabic-Indic digits and both ornate
     * parentheses, which {@code tests/test_offline_quran.py} asserts -- so the header cannot turn to
     * tofu. Latin chrome ("2 of 114", the nav buttons) keeps the system face.
     *
     * @return {@code true} when the font is applied. A missing or corrupt face is reported on screen
     *     rather than silently falling back to a system font that cannot shape Uthmani marks.
     */
    private boolean loadQuranFont() {
        try {
            quranTypeface = Typeface.createFromAsset(getAssets(), FONT_ASSET_PATH);
            if (quranTypeface == null) {
                throw new IllegalStateException("Typeface.createFromAsset returned null");
            }
            textView.setTypeface(quranTypeface);
            surahNameView.setTypeface(quranTypeface);
            juzView.setTypeface(quranTypeface);
            pageView.setTypeface(quranTypeface);
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not load " + FONT_ASSET_PATH, error);
            showError(getString(R.string.quran_font_error), false);
            return false;
        }
    }

    /** Reads the requested surah from the intent, tolerating the legacy extra key. */
    private int readRequestedSurah(Intent intent) {
        // Validation happens in onCreate so an out-of-range id reaches the user as a message.
        return intent.getIntExtra(EXTRA_SURAH_ID, intent.getIntExtra(LEGACY_EXTRA_SURAH, DEFAULT_SURAH));
    }

    private int clampAyah(int ayah, int surah) {
        if (!SurahIndex.isValid(surah)) {
            return 1;
        }
        int count = QuranMetadata.ayahCount(surah);
        return ayah < 1 ? 1 : Math.min(ayah, count);
    }

    /**
     * Uses the caller's {@code surah_name} for the window title, but logs a mismatch instead of
     * trusting it for the header -- the header must survive navigation to surahs nobody passed in.
     */
    private void applyWindowTitle(int surahId, String passedName) {
        String arabicName = SurahIndex.arabicName(surahId);
        if (passedName != null && !passedName.trim().isEmpty() && !passedName.trim().equals(arabicName)) {
            Log.w(TAG, "surah_name extra \"" + passedName + "\" does not match surah " + surahId
                    + " (\"" + arabicName + "\"); using the index value");
        }
        String title = passedName == null || passedName.trim().isEmpty()
                ? arabicName : passedName.trim();
        setTitle(getString(R.string.quran_surah_label, title));
    }

    // ---------------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------------

    /**
     * Moves to a neighbouring surah without starting another activity.
     *
     * @param delta -1 for the previous surah, +1 for the next
     */
    private void navigate(int delta) {
        int target = currentSurah + delta;
        if (!SurahIndex.isValid(target)) {
            return; // Buttons are disabled at the ends of the Mushaf; this is the second line of defence.
        }
        scrollTargetAyah = 1;
        restoredScroll = -1;
        loadSurah(target);
    }

    /**
     * Queries {@code arabic_text} for one surah on a worker thread and rebinds every view on success.
     *
     * @param surahId 1-based surah number
     */
    private void loadSurah(final int surahId) {
        if (!SurahIndex.isValid(surahId)) {
            showError(getString(R.string.quran_invalid_surah), false);
            return;
        }
        // Optimistically claim the surah so a rapid double tap cannot queue two conflicting loads.
        currentSurah = surahId;
        showLoading();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Installs the asset on first use, then returns the cached, formatted surah.
                    final String surahText = database.getSurahText(surahId);
                    if (destroyed) {
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (destroyed || isFinishing()) {
                                return;
                            }
                            showSurah(surahId, surahText);
                        }
                    });
                } catch (IOException | RuntimeException error) {
                    Log.e(TAG, "Could not read surah " + surahId + " from the offline database", error);
                    if (destroyed) {
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (destroyed || isFinishing()) {
                                return;
                            }
                            showError(getString(R.string.quran_load_error), true);
                        }
                    });
                }
            }
        });
    }

    /** Binds a loaded surah: body text, header, footer, navigation state and scroll position. */
    private void showSurah(int surahId, String surahText) {
        SurahIndex.Surah surah = SurahIndex.get(surahId);

        textView.setText(surahText);

        // Header: surah name on the left, Juz on the right, both in quran_header (#7A7A7A).
        surahNameView.setText(getString(R.string.quran_surah_label, surah.arabicName));
        juzView.setText(surah.spansMultipleJuz()
                ? getString(R.string.quran_juz_range_label,
                QuranMetadata.arabicNumber(surah.firstJuz),
                QuranMetadata.arabicNumber(surah.lastJuz))
                : getString(R.string.quran_juz_label, QuranMetadata.arabicNumber(surah.firstJuz)));

        // Footer: page number between the two navigation buttons.
        pageView.setText(surah.spansMultiplePages()
                ? getString(R.string.quran_pages_label,
                QuranMetadata.arabicNumber(surah.firstPage),
                QuranMetadata.arabicNumber(surah.lastPage))
                : getString(R.string.quran_page_label, QuranMetadata.arabicNumber(surah.firstPage)));

        positionView.setText(getString(R.string.quran_surah_position, surahId, SurahIndex.TOTAL_SURAHS));
        setTitle(getString(R.string.quran_surah_label, surah.arabicName));

        previousButton.setEnabled(surahId > 1);
        nextButton.setEnabled(surahId < SurahIndex.TOTAL_SURAHS);

        loadingContainer.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        restoreScrollPosition(surahId);
    }

    /**
     * Scrolls to the restored offset, the requested ayah, or the top of the surah.
     *
     * <p>Runs in a {@code View#post(Runnable)} so it wins over the layout and focus passes, including
     * the auto-scroll a selectable {@code TextView} can trigger.
     */
    private void restoreScrollPosition(final int surahId) {
        final int offsetToRestore = restoredScroll;
        final int ayahToReach = scrollTargetAyah;
        // Consume both: they describe one transition only.
        restoredScroll = -1;
        scrollTargetAyah = 1;

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (destroyed || isFinishing() || currentSurah != surahId) {
                    return; // A newer surah is already on screen; do not yank the scroll back.
                }
                if (offsetToRestore > 0) {
                    scrollView.scrollTo(0, offsetToRestore);
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
            // Ayah 1 means "top of the surah", which must keep any basmallah header on screen.
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
        scrollView.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        statusView.setTextColor(getColor(R.color.quran_header));
        statusView.setText(R.string.quran_loading);
        // Freeze navigation while a load is in flight so the buttons cannot queue stale surahs.
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    /**
     * @param message      user-facing, actionable explanation
     * @param allowRetry   whether the Retry button is shown
     */
    private void showError(String message, boolean allowRetry) {
        scrollView.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        statusView.setTextColor(getColor(R.color.quran_error));
        statusView.setText(message);
        retryButton.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SURAH, currentSurah);
        outState.putInt(STATE_SCROLL, scrollView != null ? scrollView.getScrollY() : 0);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        super.onDestroy();
    }
}
