package com.clock.livewallpaper.activity;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ScrollView;
import android.widget.TextView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranDatabaseHelper.Ayah;
import com.clock.livewallpaper.quran.QuranMetadata;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native, reflowable offline Surah reader. Page/Juz labels refer to the selected ayah. */
public final class QuranActivity extends Activity {
    public static final String EXTRA_SURAH = "quran.surah";
    public static final String EXTRA_AYAH = "quran.ayah";
    private static final String STATE_AYAH = "selected_ayah";
    private static final String STATE_SCROLL = "scroll_y";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean destroyed;
    private int surah;
    private int selectedAyah;
    private TextView versesView;
    private TextView juzView;
    private TextView pageView;
    private TextView statusView;
    private ScrollView scrollView;
    private View progress;
    private Spannable renderedText;
    private BackgroundColorSpan selection;
    private int[] verseStarts;
    private int[] verseEnds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran);
        configureSystemBars();
        versesView = findViewById(R.id.quran_verses);
        juzView = findViewById(R.id.quran_juz);
        pageView = findViewById(R.id.quran_page);
        statusView = findViewById(R.id.quran_status);
        scrollView = findViewById(R.id.quran_scroll);
        progress = findViewById(R.id.quran_progress);

        surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
        if (surah < 1 || surah > 114) {
            showError(R.string.quran_invalid_surah);
            return;
        }
        selectedAyah = savedInstanceState == null
                ? getIntent().getIntExtra(EXTRA_AYAH, 1)
                : savedInstanceState.getInt(STATE_AYAH, 1);
        if (selectedAyah < 1 || selectedAyah > QuranMetadata.ayahCount(surah)) {
            showError(R.string.quran_invalid_ayah);
            return;
        }
        TextView surahView = findViewById(R.id.quran_surah_name);
        String name = getResources().getStringArray(R.array.quran_surah_names)[surah - 1];
        surahView.setText(getString(R.string.quran_surah_label, name));
        setTitle(surahView.getText());
        versesView.setMovementMethod(LinkMovementMethod.getInstance());

        QuranDatabaseHelper database = new QuranDatabaseHelper(getApplicationContext());
        AssetManager assets = getAssets();
        int requestedSurah = surah;
        int restoredScroll = savedInstanceState == null
                ? -1 : savedInstanceState.getInt(STATE_SCROLL, 0);
        executor.execute(() -> {
            try {
                // Do not silently replace a missing Quran font with a system fallback.
                Typeface font = Typeface.createFromAsset(assets, "fonts/quran_font.ttf");
                List<Ayah> verses = database.getVersesBySurah(requestedSurah);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (destroyed || isFinishing()) return;
                    versesView.setTypeface(font);
                    displayVerses(verses);
                    progress.setVisibility(View.GONE);
                    statusView.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    scrollView.post(() -> {
                        if (destroyed || isFinishing()) return;
                        if (restoredScroll >= 0) {
                            scrollView.scrollTo(0, restoredScroll);
                        } else if (versesView.getLayout() != null) {
                            int line = versesView.getLayout().getLineForOffset(
                                    verseStarts[selectedAyah - 1]);
                            scrollView.scrollTo(0, versesView.getLayout().getLineTop(line));
                        }
                    });
                });
            } catch (IOException | RuntimeException error) {
                Log.e("QuranActivity", "Unable to open the bundled Quran assets", error);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (!destroyed && !isFinishing()) showError(R.string.quran_load_error);
                });
            }
        });
    }

    /** Android 15/16 enforce edge-to-edge for this target; protect header/footer/cutouts. */
    @SuppressWarnings("deprecation")
    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            View root = findViewById(R.id.quran_root);
            int left = root.getPaddingLeft();
            int top = root.getPaddingTop();
            int right = root.getPaddingRight();
            int bottom = root.getPaddingBottom();
            root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets safe = windowInsets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                // Always add to the original padding, not previously applied insets.
                view.setPadding(left + safe.left, top + safe.top,
                        right + safe.right, bottom + safe.bottom);
                return WindowInsets.CONSUMED;
            });
            root.requestApplyInsets();
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars, lightBars);
            }
            if (Build.VERSION.SDK_INT < 35) {
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
        } else {
            // API 23–29: keep the platform's default non-edge-to-edge content fitting.
            getWindow().setStatusBarColor(getColor(R.color.quran_paper));
            getWindow().setNavigationBarColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? getColor(R.color.quran_paper) : Color.BLACK);
            int flags = getWindow().getDecorView().getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void displayVerses(List<Ayah> verses) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        verseStarts = new int[verses.size()];
        verseEnds = new int[verses.size()];
        for (Ayah verse : verses) {
            if (builder.length() > 0) builder.append(' ');
            int start = builder.length();
            // Preserve Quran text verbatim. Nonbreaking spaces keep each marker together.
            builder.append(verse.text).append(" \uFD3F\u00A0")
                    .append(QuranMetadata.arabicNumber(verse.number)).append("\u00A0\uFD3E");
            int end = builder.length();
            verseStarts[verse.number - 1] = start;
            verseEnds[verse.number - 1] = end;
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    selectAyah(verse.number);
                }

                @Override
                public void updateDrawState(TextPaint paint) {
                    paint.setUnderlineText(false); // Preserve ink color, not link blue.
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        versesView.setText(builder, TextView.BufferType.SPANNABLE);
        renderedText = (Spannable) versesView.getText();
        selectAyah(selectedAyah);
    }

    private void selectAyah(int ayah) {
        selectedAyah = ayah;
        if (selection != null) renderedText.removeSpan(selection);
        selection = new BackgroundColorSpan(getColor(R.color.quran_selected));
        renderedText.setSpan(selection, verseStarts[ayah - 1], verseEnds[ayah - 1],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        juzView.setText(getString(R.string.quran_juz_label,
                QuranMetadata.arabicNumber(QuranMetadata.juzFor(surah, ayah))));
        pageView.setText(getString(R.string.quran_page_label,
                QuranMetadata.arabicNumber(QuranMetadata.pageFor(surah, ayah))));
        versesView.invalidate();
    }

    private void showError(int message) {
        progress.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(message);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_AYAH, selectedAyah);
        outState.putInt(STATE_SCROLL, scrollView.getScrollY());
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
