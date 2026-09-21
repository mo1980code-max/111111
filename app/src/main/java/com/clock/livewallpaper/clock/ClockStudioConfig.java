package com.clock.livewallpaper.clock;

import android.graphics.Color;

/**
 * Immutable snapshot of the composition selected in Clock Studio.
 *
 * <p>The Activity and the live wallpaper exchange this value through {@link ClockPreferences}; the
 * wallpaper never keeps an Activity or a View alive.  Keeping the snapshot small also makes it safe
 * to rebuild the renderer when Android recreates a wallpaper engine.
 */
public final class ClockStudioConfig {
    public static final int DEFAULT_NAME_ID = 1;
    public static final int DEFAULT_THEME_ID = 1;
    public static final String DEFAULT_CLOCK_STYLE_ID = "analog_01";
    public static final int DEFAULT_CLOCK_TYPE = 0;
    public static final int DEFAULT_CLOCK_STYLE_INDEX = 0;
    public static final float DEFAULT_POSITION_X = 0.5f;
    public static final float DEFAULT_POSITION_Y = 0.58f;
    public static final float DEFAULT_SIZE_FRACTION = 0.34f;
    public static final int DEFAULT_CLOCK_COLOR = Color.WHITE;
    public static final float DEFAULT_OPACITY = 1.0f;
    public static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;

    public final int nameId;
    public final int themeId;
    public final String clockStyleId;
    public final int clockType;
    public final int clockStyleIndex;
    public final float positionX;
    public final float positionY;
    public final float sizeFraction;
    public final int clockColor;
    public final float opacity;
    public final boolean is24Hour;
    public final boolean showSeconds;
    public final boolean showHijriDate;
    public final boolean showGregorianDate;
    public final boolean showDayName;
    public final int hijriAdjustment;
    public final int backgroundColor;
    public final int backgroundResource;
    public final String backgroundPath;
    public final boolean imageBackground;
    public final boolean customBackground;
    public final int textColor1;
    public final int textColor2;

    public ClockStudioConfig(int nameId, int themeId, String clockStyleId, int clockType,
                             int clockStyleIndex, float positionX, float positionY,
                             float sizeFraction, int clockColor, float opacity, boolean is24Hour,
                             boolean showSeconds, boolean showHijriDate, boolean showGregorianDate,
                             boolean showDayName, int hijriAdjustment, int backgroundColor,
                             int backgroundResource, String backgroundPath, boolean imageBackground,
                             boolean customBackground, int textColor1, int textColor2) {
        this.nameId = nameId;
        this.themeId = themeId;
        this.clockStyleId = clockStyleId == null ? DEFAULT_CLOCK_STYLE_ID : clockStyleId;
        this.clockType = safeClockType(clockType);
        this.clockStyleIndex = Math.max(0, clockStyleIndex);
        this.positionX = clamp(positionX, 0.05f, 0.95f);
        this.positionY = clamp(positionY, 0.08f, 0.92f);
        this.sizeFraction = clamp(sizeFraction, 0.12f, 0.72f);
        this.clockColor = clockColor;
        this.opacity = clamp(opacity, 0.05f, 1.0f);
        this.is24Hour = is24Hour;
        this.showSeconds = showSeconds;
        this.showHijriDate = showHijriDate;
        this.showGregorianDate = showGregorianDate;
        this.showDayName = showDayName;
        this.hijriAdjustment = clamp(hijriAdjustment, -2, 2);
        this.backgroundColor = backgroundColor;
        this.backgroundResource = backgroundResource;
        this.backgroundPath = backgroundPath == null ? "" : backgroundPath;
        this.imageBackground = imageBackground;
        this.customBackground = customBackground;
        this.textColor1 = textColor1;
        this.textColor2 = textColor2;
    }

    /** A conservative composition used when preferences are missing or damaged. */
    public static ClockStudioConfig safeDefault() {
        return new ClockStudioConfig(
                DEFAULT_NAME_ID,
                DEFAULT_THEME_ID,
                DEFAULT_CLOCK_STYLE_ID,
                DEFAULT_CLOCK_TYPE,
                DEFAULT_CLOCK_STYLE_INDEX,
                DEFAULT_POSITION_X,
                DEFAULT_POSITION_Y,
                DEFAULT_SIZE_FRACTION,
                DEFAULT_CLOCK_COLOR,
                DEFAULT_OPACITY,
                false,
                true,
                false,
                true,
                true,
                0,
                DEFAULT_BACKGROUND_COLOR,
                0,
                "",
                false,
                false,
                DEFAULT_CLOCK_COLOR,
                DEFAULT_CLOCK_COLOR);
    }

    private static int safeClockType(int value) {
        return value >= 0 && value <= 2 ? value : DEFAULT_CLOCK_TYPE;
    }

    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
