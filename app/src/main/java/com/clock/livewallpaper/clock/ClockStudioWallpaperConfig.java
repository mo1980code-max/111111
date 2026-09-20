package com.clock.livewallpaper.clock;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.clock.livewallpaper.catalog.AllahNamesCatalog;
import com.clock.livewallpaper.catalog.ContentAccess;
import com.clock.livewallpaper.model.AllahName;

/**
 * The durable, validated snapshot shared by Clock Studio and its WallpaperService.
 *
 * <p>Editor preferences retain the original Stage 2 keys. Applying a wallpaper copies the current
 * editor state into a separate snapshot namespace, so changing a preview after applying it does not
 * mutate the wallpaper until the user explicitly presses "Set as Live Wallpaper" again.</p>
 */
public final class ClockStudioWallpaperConfig {

    public static final String PREFS_NAME = "names_allah_clock_studio";

    // Existing Clock Studio keys: do not rename these preference keys.
    public static final String KEY_STYLE = "style_id";
    public static final String KEY_POSITION = "clock_position";
    public static final String KEY_SIZE = "clock_size";
    public static final String KEY_24_HOUR = "twenty_four_hour";
    public static final String KEY_SECONDS = "show_seconds";
    public static final String KEY_SHOW_HIJRI = "show_hijri_date";
    public static final String KEY_SHOW_GREGORIAN = "show_gregorian_date";
    public static final String KEY_SHOW_DAY = "show_day_name";
    public static final String KEY_HIJRI_ADJUSTMENT = "hijri_date_adjustment";

    // Snapshot keys are deliberately namespaced so "preview" changes are not applied implicitly.
    public static final String KEY_WALLPAPER_ACTIVE = "live_wallpaper_enabled";
    public static final String KEY_WALLPAPER_NAME_ID = "live_wallpaper_name_id";
    public static final String KEY_WALLPAPER_THEME_ID = "live_wallpaper_theme_id";
    public static final String KEY_WALLPAPER_STYLE = "live_wallpaper_style_id";
    public static final String KEY_WALLPAPER_POSITION = "live_wallpaper_position";
    public static final String KEY_WALLPAPER_SIZE = "live_wallpaper_size";
    public static final String KEY_WALLPAPER_24_HOUR = "live_wallpaper_twenty_four_hour";
    public static final String KEY_WALLPAPER_SECONDS = "live_wallpaper_show_seconds";
    public static final String KEY_WALLPAPER_HIJRI = "live_wallpaper_show_hijri_date";
    public static final String KEY_WALLPAPER_GREGORIAN = "live_wallpaper_show_gregorian_date";
    public static final String KEY_WALLPAPER_DAY = "live_wallpaper_show_day_name";
    public static final String KEY_WALLPAPER_ADJUSTMENT = "live_wallpaper_hijri_adjustment";
    public static final String KEY_WALLPAPER_COLOR = "live_wallpaper_clock_color";
    public static final String KEY_WALLPAPER_OPACITY = "live_wallpaper_clock_opacity";

    public static final int POSITION_TOP = 0;
    public static final int POSITION_CENTER = 1;
    public static final int POSITION_BOTTOM = 2;
    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_LARGE = 2;
    public static final int MIN_HIJRI_ADJUSTMENT = -2;
    public static final int MAX_HIJRI_ADJUSTMENT = 2;

    private final AllahName name;
    private final String themeId;
    private final ClockStyle style;
    private final int position;
    private final int size;
    private final boolean twentyFourHour;
    private final boolean seconds;
    private final boolean showHijri;
    private final boolean showGregorian;
    private final boolean showDay;
    private final int hijriAdjustment;
    private final int clockColor;
    private final float clockOpacity;

    private ClockStudioWallpaperConfig(@NonNull AllahName name,
                                       @NonNull String themeId,
                                       @NonNull ClockStyle style,
                                       int position,
                                       int size,
                                       boolean twentyFourHour,
                                       boolean seconds,
                                       boolean showHijri,
                                       boolean showGregorian,
                                       boolean showDay,
                                       int hijriAdjustment,
                                       @ColorInt int clockColor,
                                       float clockOpacity) {
        this.name = name;
        this.themeId = themeId;
        this.style = style;
        this.position = position;
        this.size = size;
        this.twentyFourHour = twentyFourHour;
        this.seconds = seconds;
        this.showHijri = showHijri;
        this.showGregorian = showGregorian;
        this.showDay = showDay;
        this.hijriAdjustment = hijriAdjustment;
        this.clockColor = clockColor;
        this.clockOpacity = clockOpacity;
    }

    /** Saves only after the same ContentAccess gate used by the gallery has approved the Name. */
    public static boolean save(@NonNull Context context,
                               @NonNull AllahName name,
                               @NonNull ClockStyle style,
                               @NonNull String themeId,
                               int position,
                               int size,
                               boolean twentyFourHour,
                               boolean seconds,
                               boolean showHijri,
                               boolean showGregorian,
                               boolean showDay,
                               int hijriAdjustment,
                               @ColorInt int clockColor,
                               float clockOpacity) {
        if (!ContentAccess.isAvailable(context, name)) {
            return false;
        }
        SharedPreferences.Editor editor = preferences(context).edit();
        editor.putBoolean(KEY_WALLPAPER_ACTIVE, true);
        editor.putString(KEY_WALLPAPER_NAME_ID, name.getStableId());
        editor.putString(KEY_WALLPAPER_THEME_ID, ClockStudioTheme.safeId(themeId, name.getNumber()));
        editor.putString(KEY_WALLPAPER_STYLE, style.getId());
        editor.putInt(KEY_WALLPAPER_POSITION, clamp(position, POSITION_TOP, POSITION_BOTTOM));
        editor.putInt(KEY_WALLPAPER_SIZE, clamp(size, SIZE_SMALL, SIZE_LARGE));
        editor.putBoolean(KEY_WALLPAPER_24_HOUR, twentyFourHour);
        editor.putBoolean(KEY_WALLPAPER_SECONDS, seconds);
        editor.putBoolean(KEY_WALLPAPER_HIJRI, showHijri);
        editor.putBoolean(KEY_WALLPAPER_GREGORIAN, showGregorian);
        editor.putBoolean(KEY_WALLPAPER_DAY, showDay);
        editor.putInt(KEY_WALLPAPER_ADJUSTMENT,
                clamp(hijriAdjustment, MIN_HIJRI_ADJUSTMENT, MAX_HIJRI_ADJUSTMENT));
        editor.putInt(KEY_WALLPAPER_COLOR, clockColor == 0 ? style.getPreviewColor() : clockColor);
        editor.putFloat(KEY_WALLPAPER_OPACITY, clampOpacity(clockOpacity));
        return editor.commit();
    }

    /** The legacy Editor must explicitly hand ownership back to its original wallpaper path. */
    public static void disable(@NonNull Context context) {
        preferences(context).edit().putBoolean(KEY_WALLPAPER_ACTIVE, false).commit();
    }

    public static boolean isEnabled(@NonNull Context context) {
        return preferences(context).getBoolean(KEY_WALLPAPER_ACTIVE, false);
    }

    /** Reads a safe configuration. Invalid IDs or a locked Name never reach the renderer. */
    @NonNull
    public static ClockStudioWallpaperConfig read(@NonNull Context context) {
        SharedPreferences prefs = preferences(context);
        AllahName name = AllahNamesCatalog.byId(prefs.getString(KEY_WALLPAPER_NAME_ID, null));
        if (name == null || !ContentAccess.isAvailable(context, name)) {
            name = AllahNamesCatalog.byNumber(1);
        }
        String theme = ClockStudioTheme.safeId(
                prefs.getString(KEY_WALLPAPER_THEME_ID, null), name.getNumber());
        ClockStyle style = ClockStyleRegistry.byId(prefs.getString(
                KEY_WALLPAPER_STYLE, ClockStyleRegistry.defaultStyle().getId()));
        int color = prefs.getInt(KEY_WALLPAPER_COLOR, style.getPreviewColor());
        return new ClockStudioWallpaperConfig(
                name,
                theme,
                style,
                clamp(prefs.getInt(KEY_WALLPAPER_POSITION, POSITION_CENTER), POSITION_TOP, POSITION_BOTTOM),
                clamp(prefs.getInt(KEY_WALLPAPER_SIZE, SIZE_MEDIUM), SIZE_SMALL, SIZE_LARGE),
                prefs.getBoolean(KEY_WALLPAPER_24_HOUR, false),
                prefs.getBoolean(KEY_WALLPAPER_SECONDS, true),
                prefs.getBoolean(KEY_WALLPAPER_HIJRI, true),
                prefs.getBoolean(KEY_WALLPAPER_GREGORIAN, false),
                prefs.getBoolean(KEY_WALLPAPER_DAY, false),
                clamp(prefs.getInt(KEY_WALLPAPER_ADJUSTMENT, 0),
                        MIN_HIJRI_ADJUSTMENT, MAX_HIJRI_ADJUSTMENT),
                color,
                clampOpacity(prefs.getFloat(KEY_WALLPAPER_OPACITY, 1f)));
    }

    @NonNull
    public AllahName getName() {
        return name;
    }

    @NonNull
    public String getThemeId() {
        return themeId;
    }

    @NonNull
    public ClockStyle getStyle() {
        return style;
    }

    public int getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public boolean isTwentyFourHour() {
        return twentyFourHour;
    }

    public boolean showSeconds() {
        return seconds;
    }

    public boolean showHijri() {
        return showHijri;
    }

    public boolean showGregorian() {
        return showGregorian;
    }

    public boolean showDay() {
        return showDay;
    }

    public int getHijriAdjustment() {
        return hijriAdjustment;
    }

    @ColorInt
    public int getClockColor() {
        return clockColor;
    }

    public float getClockOpacity() {
        return clockOpacity;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampOpacity(float opacity) {
        if (Float.isNaN(opacity) || Float.isInfinite(opacity)) {
            return 1f;
        }
        return Math.max(0.25f, Math.min(1f, opacity));
    }
}
