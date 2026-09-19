package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.quran.QuranMetadata;
import com.clock.livewallpaper.quran.QuranSettings;
import com.clock.livewallpaper.quran.QuranThemeColors;
import com.clock.livewallpaper.quran.SurahIndex;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.util.ArrayList;
import java.util.List;

/**
 * Quran reading screen: a horizontal {@link ViewPager2} with one {@link QuranFragment} per surah,
 * read entirely offline from the bundled {@code quran.ar.uthmani.db}.
 *
 * <h2>Entry contract</h2>
 * <pre>
 * Intent intent = new Intent(this, QuranActivity.class);
 * intent.putExtra(QuranActivity.EXTRA_SURAH_ID, 2);         // 1-114, defaults to 1
 * intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, "البقرة"); // optional, see below
 * intent.putExtra(QuranActivity.EXTRA_AYAH, 255);           // optional one-shot ayah target
 * intent.putExtra(QuranActivity.EXTRA_SCROLL_Y, 4321);      // optional one-shot px offset
 * startActivity(intent);
 * </pre>
 * {@code surah_id} is the single source of truth: it selects the starting page and drives the
 * header. A {@code scroll_y} target wins over an {@code ayah} target; both are consumed once by the
 * target page. {@code surah_name} is only used to validate the caller: the header is always
 * rendered from {@link SurahIndex}, because swiping reaches surahs no caller ever passed in.
 *
 * <h2>Navigation</h2>
 * The Next/Previous buttons of the earlier reader are gone: swiping left/right moves between
 * surahs, and {@code FragmentStateAdapter} destroys pages far from the current one, so memory stays
 * flat across the 114-page Mushaf. The pinned header and footer follow the selected page.
 *
 * <h2>Toolbar</h2>
 * Font +/- (persisted size), a bookmark icon that saves {@code last_read_surah_id} plus the page's
 * current {@code scrollY} to {@link QuranSettings}, and a moon/sun icon that flips the palette
 * applied by {@link QuranThemeColors}. Every preference survives process death.
 *
 * <h2>Why AppCompatActivity</h2>
 * {@code FragmentStateAdapter} hosts AndroidX fragments, which require a {@code FragmentActivity}
 * host — the framework {@code android.app.Activity} used by the earlier reader cannot hold them.
 * The theme in {@code res/values/quran.xml} was moved to an AppCompat parent accordingly.
 */
public final class QuranActivity extends AppCompatActivity implements QuranFragment.Listener {

    /** 1-based surah number to open, 1 to 114. */
    public static final String EXTRA_SURAH_ID = "surah_id";
    /** Display name of the surah, paired with {@link #EXTRA_SURAH_ID}. */
    public static final String EXTRA_SURAH_NAME = "surah_name";
    /** Optional 1-based ayah to scroll to once the surah has loaded. */
    public static final String EXTRA_AYAH = "ayah";
    /** Optional vertical scroll offset in px to restore once the surah has loaded. */
    public static final String EXTRA_SCROLL_Y = "scroll_y";

    /** Accepted for callers written against the earlier reader; prefer {@link #EXTRA_SURAH_ID}. */
    private static final String LEGACY_EXTRA_SURAH = "quran.surah";
    /** Accepted for callers written against the earlier reader; prefer {@link #EXTRA_AYAH}. */
    private static final String LEGACY_EXTRA_AYAH = "quran.ayah";

    private static final String TAG = "QuranActivity";
    private static final int DEFAULT_SURAH = 1;

    private QuranSettings settings;

    private View root;
    private ViewPager2 pager;
    private SurahPagerAdapter adapter;

    private TextView surahNameView;
    private TextView juzView;
    private TextView positionView;
    private TextView pageView;
    private View dividerTop;
    private View dividerBottom;
    private ImageButton themeToggleButton;

    /** Pages currently attached to the FragmentManager; targets of live font/theme updates. */
    private final List<QuranFragment> attachedPages = new ArrayList<>();

    /** One-shot targets for the starting page, consumed by the adapter when it creates the page. */
    private int pendingScrollSurah = -1;
    private int pendingScrollY;
    private int pendingAyahSurah = -1;
    private int pendingAyah;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran);

        // Requirement 12: the whole Quran section is ad free. The flag is raised here rather than in
        // each call site so a later screen cannot forget to check it: while it is up, the ad layer
        // refuses every format at the source, including the app open ad on a foreground return.
        AdPolicy.enterQuranScreen();

        settings = QuranSettings.get(this);
        root = findViewById(R.id.quran_root);
        EdgeToEdgeInsets.apply(this, root, QuranThemeColors.background(this));

        bindViews();
        applyThemeToChrome();

        Intent intent = getIntent();
        int requestedSurah = readRequestedSurah(intent);
        if (!SurahIndex.isValid(requestedSurah)) {
            // A pager cannot display "no such page"; report and leave instead.
            Toast.makeText(this, R.string.quran_invalid_surah, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        applyWindowTitle(requestedSurah, intent.getStringExtra(EXTRA_SURAH_NAME));

        if (savedInstanceState == null) {
            // Fresh launch: hand the one-shot targets to the starting page. ViewPager2 restores
            // its own selected page across configuration changes, so this branch only runs once.
            int requestedScroll = Math.max(0, intent.getIntExtra(EXTRA_SCROLL_Y, 0));
            int requestedAyah = clampAyah(intent.getIntExtra(EXTRA_AYAH,
                    intent.getIntExtra(LEGACY_EXTRA_AYAH, 1)), requestedSurah);
            if (requestedScroll > 0) {
                pendingScrollSurah = requestedSurah;
                pendingScrollY = requestedScroll;
            } else if (requestedAyah > 1) {
                pendingAyahSurah = requestedSurah;
                pendingAyah = requestedAyah;
            }
        }

        adapter = new SurahPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                describeSurah(position + 1);
            }
        });

        if (savedInstanceState == null) {
            // setCurrentItem does not animate the initial placement.
            pager.setCurrentItem(requestedSurah - 1, false);
        }
        // Immediate paint: onPageSelected also fires on first layout, this only avoids a blank
        // header for one frame.
        describeSurah(savedInstanceState == null ? requestedSurah : pager.getCurrentItem() + 1);
    }

    private void bindViews() {
        pager = findViewById(R.id.quran_pager);
        surahNameView = findViewById(R.id.quran_surah_name);
        juzView = findViewById(R.id.quran_juz);
        positionView = findViewById(R.id.quran_position);
        pageView = findViewById(R.id.quran_page);
        dividerTop = findViewById(R.id.quran_divider_top);
        dividerBottom = findViewById(R.id.quran_divider_bottom);

        ImageButton back = findViewById(R.id.quran_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ImageButton fontDecrease = findViewById(R.id.quran_font_decrease);
        fontDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFontSize(-QuranSettings.FONT_SIZE_STEP);
            }
        });

        ImageButton fontIncrease = findViewById(R.id.quran_font_increase);
        fontIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFontSize(QuranSettings.FONT_SIZE_STEP);
            }
        });

        ImageButton bookmark = findViewById(R.id.quran_bookmark);
        bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveBookmark();
            }
        });

        themeToggleButton = findViewById(R.id.quran_theme_toggle);
        themeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleTheme();
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // QuranFragment.Listener: live registry of attached pages
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onPageFragmentAttached(@NonNull QuranFragment fragment) {
        if (!attachedPages.contains(fragment)) {
            attachedPages.add(fragment);
        }
    }

    @Override
    public void onPageFragmentDetached(@NonNull QuranFragment fragment) {
        attachedPages.remove(fragment);
    }

    /** @return the page at a pager position when it is currently instantiated, else {@code null}. */
    private QuranFragment pageAt(int position) {
        if (adapter == null) {
            return null;
        }
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("f" + adapter.getItemId(position));
        return fragment instanceof QuranFragment ? (QuranFragment) fragment : null;
    }

    @Override
    protected void onDestroy() {
        // Drops the flag only when the last Quran screen of the task is gone, and the policy records the
        // moment so no ad is shown on the very next frame after leaving the section.
        AdPolicy.exitQuranScreen();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------------
    // Pager adapter: one QuranFragment per surah
    // ---------------------------------------------------------------------------------------------

    /** Maps pager positions 0..113 onto surahs 1..114 and hands out one-shot scroll targets. */
    private final class SurahPagerAdapter extends FragmentStateAdapter {

        SurahPagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @Override
        public int getItemCount() {
            return SurahIndex.TOTAL_SURAHS;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            int surahId = position + 1;
            int scrollY = 0;
            int targetAyah = 1;
            if (surahId == pendingScrollSurah) {
                scrollY = pendingScrollY;
                pendingScrollSurah = -1; // one-shot: never reapply after the page is recycled
                pendingScrollY = 0;
            }
            if (surahId == pendingAyahSurah) {
                targetAyah = pendingAyah;
                pendingAyahSurah = -1;
                pendingAyah = 1;
            }
            return QuranFragment.newInstance(surahId, scrollY, targetAyah);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Header / footer
    // ---------------------------------------------------------------------------------------------

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
     * trusting it for the header — the header must survive swiping to surahs nobody passed in.
     */
    private void applyWindowTitle(int surahId, String passedName) {
        String arabicName = SurahIndex.arabicName(surahId);
        if (passedName != null && !passedName.trim().isEmpty() && !passedName.trim().equals(arabicName)) {
            Log.w(TAG, "surah_name extra \"" + passedName + "\" does not match surah " + surahId
                    + " (\"" + arabicName + "\"); using the index value");
        }
    }

    /** Binds the pinned header and footer to the surah currently selected in the pager. */
    private void describeSurah(int surahId) {
        if (!SurahIndex.isValid(surahId)) {
            return;
        }
        SurahIndex.Surah surah = SurahIndex.get(surahId);

        surahNameView.setText(getString(R.string.quran_surah_label, surah.arabicName));
        juzView.setText(surah.spansMultipleJuz()
                ? getString(R.string.quran_juz_range_label,
                QuranMetadata.arabicNumber(surah.firstJuz),
                QuranMetadata.arabicNumber(surah.lastJuz))
                : getString(R.string.quran_juz_label, QuranMetadata.arabicNumber(surah.firstJuz)));

        pageView.setText(surah.spansMultiplePages()
                ? getString(R.string.quran_pages_label,
                QuranMetadata.arabicNumber(surah.firstPage),
                QuranMetadata.arabicNumber(surah.lastPage))
                : getString(R.string.quran_page_label, QuranMetadata.arabicNumber(surah.firstPage)));

        positionView.setText(getString(R.string.quran_surah_position, surahId, SurahIndex.TOTAL_SURAHS));
        setTitle(getString(R.string.quran_surah_label, surah.arabicName));
    }

    // ---------------------------------------------------------------------------------------------
    // Font size
    // ---------------------------------------------------------------------------------------------

    /** Moves the persisted body size by one step and repaints every attached page. */
    private void changeFontSize(float deltaSp) {
        float updated = settings.adjustFontSize(deltaSp);
        for (int i = 0; i < attachedPages.size(); i++) {
            attachedPages.get(i).applyFontSize(updated);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Bookmark
    // ---------------------------------------------------------------------------------------------

    /** Saves the selected surah plus its live scroll offset as the last-read position. */
    private void saveBookmark() {
        int surahId = pager.getCurrentItem() + 1;
        if (!SurahIndex.isValid(surahId)) {
            return;
        }
        QuranFragment page = pageAt(pager.getCurrentItem());
        int scrollY = page == null ? 0 : page.getCurrentScrollY();
        settings.saveBookmark(surahId, scrollY);
        Toast.makeText(this, R.string.quran_bookmark_saved, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------------------------------

    /** Flips the palette, persists it, and repaints the chrome plus every attached page. */
    private void toggleTheme() {
        boolean dark = settings.toggleDarkMode();
        applyThemeToChrome();
        for (int i = 0; i < attachedPages.size(); i++) {
            attachedPages.get(i).applyThemeColors();
        }
    }

    /** Paints everything the activity owns (root, toolbar, header, footer) with the active palette. */
    private void applyThemeToChrome() {
        boolean dark = settings.isDarkMode();

        root.setBackgroundColor(QuranThemeColors.background(this));
        dividerTop.setBackgroundColor(QuranThemeColors.divider(this));
        dividerBottom.setBackgroundColor(QuranThemeColors.divider(this));

        int chrome = QuranThemeColors.header(this);
        surahNameView.setTextColor(chrome);
        juzView.setTextColor(chrome);
        positionView.setTextColor(chrome);
        pageView.setTextColor(chrome);

        ImageButton back = findViewById(R.id.quran_back);
        ImageButton fontDecrease = findViewById(R.id.quran_font_decrease);
        ImageButton fontIncrease = findViewById(R.id.quran_font_increase);
        ImageButton bookmark = findViewById(R.id.quran_bookmark);
        back.setColorFilter(chrome);
        fontDecrease.setColorFilter(chrome);
        fontIncrease.setColorFilter(chrome);
        bookmark.setColorFilter(chrome);

        // Show the destination of the next toggle: moon means "tap to go dark".
        themeToggleButton.setImageResource(dark ? R.drawable.ic_sun : R.drawable.ic_moon);
        themeToggleButton.setColorFilter(chrome);
        themeToggleButton.setContentDescription(getString(dark
                ? R.string.quran_cd_switch_to_light
                : R.string.quran_cd_switch_to_dark));

        applySystemBarAppearance(dark);
    }

    /**
     * Keeps the status/navigation bar icons legible against the new background: dark icons on the
     * Madani paper, light icons on the dark palette. Mirrors what {@link EdgeToEdgeInsets} sets up
     * for light mode at launch.
     */
    @SuppressWarnings("deprecation")
    private void applySystemBarAppearance(boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(dark ? 0 : lightBars, lightBars);
            }
            return;
        }
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        int lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lightFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().setNavigationBarColor(QuranThemeColors.background(this));
        }
        getWindow().setStatusBarColor(QuranThemeColors.background(this));
        getWindow().getDecorView().setSystemUiVisibility(dark
                ? flags & ~lightFlags
                : flags | lightFlags);
    }
}
