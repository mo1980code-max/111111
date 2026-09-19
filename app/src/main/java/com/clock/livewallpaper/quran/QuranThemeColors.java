package com.clock.livewallpaper.quran;

import android.content.Context;

import androidx.annotation.ColorInt;

/**
 * Programmatic colour palette for the Quran screens.
 *
 * <p>The reader switches themes at runtime from a toolbar icon, so colours cannot live in static
 * XML references alone: every Quran view asks this class for its colour and reapplies it when the
 * preference flips. The persisted choice itself lives in {@link QuranSettings}.
 *
 * <h2>Palettes</h2>
 * <pre>
 *   Light (Madani)   background #FBF9F0   text #1A1A1A   header/footer #7A7A7A
 *   Dark             background #121212   text #E0E0E0   header/footer #A0A0A0
 * </pre>
 * The five light values double as the resource colours in {@code res/values/colors.xml} used by the
 * XML layouts; the constants here mirror them so XML defaults and programmatic application can
 * never drift. Supporting shades (divider, badge, highlight) are derived per palette because the
 * layouts reference them too. The accent green is identical in both modes.
 */
public final class QuranThemeColors {

    // --- Light mode (Madani paper) ---------------------------------------------------------------
    private static final int LIGHT_BACKGROUND = 0xFFFBF9F0;
    private static final int LIGHT_TEXT = 0xFF1A1A1A;
    private static final int LIGHT_HEADER = 0xFF7A7A7A;
    private static final int LIGHT_DIVIDER = 0xFFE8E4D5;
    private static final int LIGHT_BADGE = 0xFFEFEADA;
    private static final int LIGHT_HIGHLIGHT = 0xFFD0ECE4;
    private static final int LIGHT_ERROR = 0xFFC62828;

    // --- Dark mode ---------------------------------------------------------------------------------
    private static final int DARK_BACKGROUND = 0xFF121212;
    private static final int DARK_TEXT = 0xFFE0E0E0;
    /** Spec: header AND footer labels use this muted grey in dark mode. */
    private static final int DARK_HEADER = 0xFFA0A0A0;
    private static final int DARK_DIVIDER = 0xFF2B2B2B;
    private static final int DARK_BADGE = 0xFF1F1F1F;
    private static final int DARK_HIGHLIGHT = 0xFF1F3D31;
    private static final int DARK_ERROR = 0xFFEF9A9A;

    /** Accent green for buttons and references; identical in both modes. */
    private static final int BUTTON_GREEN = 0xFF2D7D46;

    private QuranThemeColors() { }

    /** Screen background: the paper behind the reader and the index. */
    @ColorInt
    public static int background(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }

    /** Primary ink: Arabic verse text and primary list labels. */
    @ColorInt
    public static int text(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_TEXT : LIGHT_TEXT;
    }

    /** Muted chrome: surah name, Juz, page, position, status and footer labels. */
    @ColorInt
    public static int header(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_HEADER : LIGHT_HEADER;
    }

    /** Hairline separators between toolbar, body and footer. */
    @ColorInt
    public static int divider(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_DIVIDER : LIGHT_DIVIDER;
    }

    /** Fill for the surah-number badge and the search field. */
    @ColorInt
    public static int badge(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_BADGE : LIGHT_BADGE;
    }

    /** Pressed/selected tint, e.g. a tapped surah row ripple. */
    @ColorInt
    public static int highlight(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_HIGHLIGHT : LIGHT_HIGHLIGHT;
    }

    /** Load and install errors. */
    @ColorInt
    public static int error(Context context) {
        return QuranSettings.get(context).isDarkMode() ? DARK_ERROR : LIGHT_ERROR;
    }

    /** Accent green used by buttons regardless of the active palette. */
    @ColorInt
    public static int button(Context context) {
        return BUTTON_GREEN;
    }
}
