package com.clock.livewallpaper.quran;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistent user preferences for the Quran feature, backed by one {@link SharedPreferences} file.
 *
 * <p>Owns three concerns that must survive process death and reinstalls of the reader:
 * <ul>
 *   <li><b>Font size</b> — the reader body size in sp, adjustable with the +/- toolbar icons.</li>
 *   <li><b>Theme</b> — light (Madani paper) or dark, toggled with the moon/sun icon. Colours are
 *       switched programmatically from {@link QuranThemeColors}; the flag stored here decides which
 *       palette every Quran screen paints itself with.</li>
 *   <li><b>Bookmark</b> — the last-read surah and the exact {@code scrollY} inside it, written when
 *       the user taps the bookmark icon in the reader toolbar and read back by the "Continue
 *       Reading" button on the Surah Index screen.</li>
 * </ul>
 *
 * <p>Process-wide singleton: always obtain it through {@link #get(Context)} so every screen sees the
 * same in-memory values. All writes use {@link SharedPreferences.Editor#apply()}; preferences are
 * small and losing a preference write to a killed process is acceptable, losing UI responsiveness
 * to a synchronous disk write is not.
 */
public final class QuranSettings {

    /** Name of the preferences file, stable across versions — this is part of the stored contract. */
    public static final String PREFS_NAME = "quran_prefs";

    private static final String KEY_FONT_SIZE = "quran_font_size";
    private static final String KEY_DARK_MODE = "quran_dark_mode";
    /** Key named after the feature: the surah the bookmark points at, 1..114. */
    private static final String KEY_BOOKMARK_SURAH = "last_read_surah_id";
    /** Vertical scroll offset (px) inside the bookmarked surah, as reported by the ScrollView. */
    private static final String KEY_BOOKMARK_SCROLL = "last_read_scroll_y";

    /** Body size the reader ships with; matches the design default in the page layout. */
    public static final float DEFAULT_FONT_SIZE = 26f;
    /** Smallest readable size for the Uthmani script. */
    public static final float MIN_FONT_SIZE = 16f;
    /** Largest size before long ayahs collapse into one-word lines. */
    public static final float MAX_FONT_SIZE = 44f;
    /** One tap on + or - moves the size by this many sp. */
    public static final float FONT_SIZE_STEP = 1f;

    /** Returned by {@link #getBookmarkSurah()} when no bookmark has been saved yet. */
    public static final int NO_BOOKMARK = -1;

    private static volatile QuranSettings instance;

    private final SharedPreferences prefs;

    private QuranSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return the process-wide instance, creating it on first use. */
    public static QuranSettings get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        QuranSettings local = instance;
        if (local == null) {
            synchronized (QuranSettings.class) {
                local = instance;
                if (local == null) {
                    local = new QuranSettings(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    // ---------------------------------------------------------------------------------------------
    // Font size
    // ---------------------------------------------------------------------------------------------

    /** @return the preferred reader body size in sp, always within {@code [MIN, MAX]}. */
    public float getFontSize() {
        float stored = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        return clampFontSize(stored);
    }

    /** Persists a new body size; out-of-range values are clamped, never rejected. */
    public void setFontSize(float sizeSp) {
        prefs.edit().putFloat(KEY_FONT_SIZE, clampFontSize(sizeSp)).apply();
    }

    /**
     * Applies one +/- tap and persists the result.
     *
     * @param deltaSp {@code +FONT_SIZE_STEP} or {@code -FONT_SIZE_STEP}
     * @return the new effective size, so callers can apply it immediately
     */
    public float adjustFontSize(float deltaSp) {
        float updated = clampFontSize(getFontSize() + deltaSp);
        prefs.edit().putFloat(KEY_FONT_SIZE, updated).apply();
        return updated;
    }

    private static float clampFontSize(float sizeSp) {
        if (Float.isNaN(sizeSp)) {
            return DEFAULT_FONT_SIZE;
        }
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, sizeSp));
    }

    // ---------------------------------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------------------------------

    /** @return {@code true} when the reader paints itself with the dark palette. */
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    /** Switches the whole Quran feature to the given palette on the next apply pass. */
    public void setDarkMode(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    /**
     * Flips the palette, persists the choice and reports it.
     *
     * @return the new state: {@code true} means dark mode is now active
     */
    public boolean toggleDarkMode() {
        boolean dark = !isDarkMode();
        setDarkMode(dark);
        return dark;
    }

    // ---------------------------------------------------------------------------------------------
    // Bookmark (last read position)
    // ---------------------------------------------------------------------------------------------

    /**
     * Saves where the user stopped reading. Called from the bookmark icon in the reader toolbar.
     *
     * @param surahId 1-based surah number, 1..114
     * @param scrollY vertical scroll offset inside that surah in px; 0 means "top of the surah"
     */
    public void saveBookmark(int surahId, int scrollY) {
        if (!SurahIndex.isValid(surahId)) {
            throw new IllegalArgumentException("Surah must be between 1 and "
                    + SurahIndex.TOTAL_SURAHS + ", got " + surahId);
        }
        prefs.edit()
                .putInt(KEY_BOOKMARK_SURAH, surahId)
                .putInt(KEY_BOOKMARK_SCROLL, Math.max(0, scrollY))
                .apply();
    }

    /** @return {@code true} when a bookmark exists and points at a valid surah. */
    public boolean hasBookmark() {
        return SurahIndex.isValid(prefs.getInt(KEY_BOOKMARK_SURAH, NO_BOOKMARK));
    }

    /** @return the bookmarked surah, or {@link #NO_BOOKMARK} when nothing was saved. */
    public int getBookmarkSurah() {
        return prefs.getInt(KEY_BOOKMARK_SURAH, NO_BOOKMARK);
    }

    /** @return the bookmarked scroll offset in px; 0 when nothing was saved. */
    public int getBookmarkScrollY() {
        return Math.max(0, prefs.getInt(KEY_BOOKMARK_SCROLL, 0));
    }
}
