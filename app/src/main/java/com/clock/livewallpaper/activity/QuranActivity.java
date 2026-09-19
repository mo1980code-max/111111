package com.clock.livewallpaper.activity;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranApiClient;
import com.clock.livewallpaper.quran.QuranMetadata;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Dynamic online Quran Surah reader with caching, retry handling, and responsive RTL UI. */
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
    private int restoredScroll = -1;
    private TextView versesView;
    private TextView juzView;
    private TextView pageView;
    private TextView statusView;
    private ScrollView scrollView;
    private View progress;
    private Button retryBtn;
    private Spannable renderedText;
    private BackgroundColorSpan selection;
    private int[] verseStarts;
    private int[] verseEnds;
    private List<QuranApiClient.Ayah> currentVerses;

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
        retryBtn = findViewById(R.id.quran_retry_btn);

        surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
        if (surah < 1 || surah > 114) {
            showError(R.string.quran_invalid_surah);
            if (retryBtn != null) retryBtn.setVisibility(View.GONE);
            return;
        }
        selectedAyah = savedInstanceState == null
                ? getIntent().getIntExtra(EXTRA_AYAH, 1)
                : savedInstanceState.getInt(STATE_AYAH, 1);
        if (selectedAyah < 1 || selectedAyah > QuranMetadata.ayahCount(surah)) {
            showError(R.string.quran_invalid_ayah);
            if (retryBtn != null) retryBtn.setVisibility(View.GONE);
            return;
        }

        restoredScroll = savedInstanceState == null
                ? -1 : savedInstanceState.getInt(STATE_SCROLL, 0);

        TextView surahView = findViewById(R.id.quran_surah_name);
        String name = getResources().getStringArray(R.array.quran_surah_names)[surah - 1];
        surahView.setText(getString(R.string.quran_surah_label, name));
        setTitle(surahView.getText());
        versesView.setMovementMethod(LinkMovementMethod.getInstance());

        if (retryBtn != null) {
            retryBtn.setOnClickListener(v -> loadSurahData());
        }

        loadSurahData();
    }

    private void loadSurahData() {
        progress.setVisibility(View.VISIBLE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(R.string.quran_loading);
        if (retryBtn != null) retryBtn.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);

        final int requestedSurah = surah;
        executor.execute(() -> {
            try {
                QuranApiClient.SurahData surahData = QuranApiClient.getSurah(getApplicationContext(), requestedSurah);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (destroyed || isFinishing()) return;
                    currentVerses = surahData.ayahs;
                    displayVerses(surahData.ayahs);
                    progress.setVisibility(View.GONE);
                    statusView.setVisibility(View.GONE);
                    if (retryBtn != null) retryBtn.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    scrollView.post(() -> {
                        if (destroyed || isFinishing()) return;
                        if (restoredScroll >= 0) {
                            scrollView.scrollTo(0, restoredScroll);
                        } else if (versesView.getLayout() != null && verseStarts != null && selectedAyah <= verseStarts.length) {
                            int line = versesView.getLayout().getLineForOffset(
                                    verseStarts[selectedAyah - 1]);
                            scrollView.scrollTo(0, versesView.getLayout().getLineTop(line));
                        }
                    });
                });
            } catch (IOException | RuntimeException error) {
                Log.e("QuranActivity", "Unable to fetch Surah data from online API", error);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (!destroyed && !isFinishing()) {
                        showError(R.string.quran_load_error);
                    }
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

    private void displayVerses(List<QuranApiClient.Ayah> verses) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        verseStarts = new int[verses.size()];
        verseEnds = new int[verses.size()];
        for (int i = 0; i < verses.size(); i++) {
            QuranApiClient.Ayah verse = verses.get(i);
            if (builder.length() > 0) builder.append(' ');
            int start = builder.length();
            int ayahNumber = verse.numberInSurah;
            builder.append(verse.text).append(" \uFD3F\u00A0")
                    .append(QuranMetadata.arabicNumber(ayahNumber)).append("\u00A0\uFD3E");
            int end = builder.length();
            verseStarts[i] = start;
            verseEnds[i] = end;
            final int index = i;
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    selectAyah(index + 1);
                }

                @Override
                public void updateDrawState(TextPaint paint) {
                    paint.setUnderlineText(false);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        versesView.setText(builder, TextView.BufferType.SPANNABLE);
        renderedText = (Spannable) versesView.getText();
        selectAyah(selectedAyah);
    }

    private void selectAyah(int ayah) {
        if (ayah < 1 || (verseStarts != null && ayah > verseStarts.length)) {
            return;
        }
        selectedAyah = ayah;
        if (renderedText != null && verseStarts != null && ayah - 1 < verseStarts.length) {
            if (selection != null) renderedText.removeSpan(selection);
            selection = new BackgroundColorSpan(getColor(R.color.quran_selected));
            renderedText.setSpan(selection, verseStarts[ayah - 1], verseEnds[ayah - 1],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            versesView.invalidate();
        }
        int juz = (currentVerses != null && ayah - 1 < currentVerses.size())
                ? currentVerses.get(ayah - 1).juz
                : QuranMetadata.juzFor(surah, ayah);
        int page = (currentVerses != null && ayah - 1 < currentVerses.size())
                ? currentVerses.get(ayah - 1).page
                : QuranMetadata.pageFor(surah, ayah);
        juzView.setText(getString(R.string.quran_juz_label, QuranMetadata.arabicNumber(juz)));
        pageView.setText(getString(R.string.quran_page_label, QuranMetadata.arabicNumber(page)));
    }

    private void showError(int messageRes) {
        progress.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(messageRes);
        if (retryBtn != null) retryBtn.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_AYAH, selectedAyah);
        outState.putInt(STATE_SCROLL, scrollView != null ? scrollView.getScrollY() : 0);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
