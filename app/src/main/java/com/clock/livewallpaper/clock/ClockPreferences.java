package com.clock.livewallpaper.clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.utils.TinyDB;

/**
 * The single persistence boundary for a Clock Studio composition.
 *
 * <p>The project originally stored editor values as unrelated keys in the default preferences.  Those
 * keys are still mirrored for the old preview views, but the wallpaper reads this namespaced record,
 * which also carries the Name, theme, date switches and adjustment.  A one-time, defensive migration
 * keeps existing users' clocks intact and leaves unrelated preferences untouched.
 */
public final class ClockPreferences {
    public static final String PREFS_NAME = "clock_studio_preferences_v1";
    public static final String KEY_MIGRATED = "migrated";
    public static final String KEY_NAME_ID = "name_id";
    public static final String KEY_THEME_ID = "theme_id";
    public static final String KEY_CLOCK_STYLE_ID = "clock_style_id";
    public static final String KEY_CLOCK_TYPE = "clock_type";
    public static final String KEY_CLOCK_STYLE_INDEX = "clock_style_index";
    public static final String KEY_POSITION_X = "position_x";
    public static final String KEY_POSITION_Y = "position_y";
    public static final String KEY_SIZE_FRACTION = "size_fraction";
    public static final String KEY_CLOCK_COLOR = "clock_color";
    public static final String KEY_OPACITY = "clock_opacity";
    public static final String KEY_24_HOUR = "hour_format_24";
    public static final String KEY_SHOW_SECONDS = "show_seconds";
    public static final String KEY_SHOW_HIJRI = "show_hijri_date";
    public static final String KEY_SHOW_GREGORIAN = "show_gregorian_date";
    public static final String KEY_SHOW_DAY_NAME = "show_day_name";
    public static final String KEY_HIJRI_ADJUSTMENT = "hijri_adjustment";
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_BACKGROUND_RESOURCE = "background_resource";
    public static final String KEY_BACKGROUND_PATH = "background_path";
    public static final String KEY_IMAGE_BACKGROUND = "image_background";
    public static final String KEY_CUSTOM_BACKGROUND = "custom_background";
    public static final String KEY_TEXT_COLOR_1 = "text_color_1";
    public static final String KEY_TEXT_COLOR_2 = "text_color_2";

    private final Context context;
    private final SharedPreferences preferences;
    private final SharedPreferences legacyPreferences;

    private ClockPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.legacyPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public static ClockPreferences get(Context context) {
        return new ClockPreferences(context);
    }

    /** Reads the current safe snapshot, migrating the old editor keys if necessary. */
    public ClockStudioConfig load() {
        if (!preferences.getBoolean(KEY_MIGRATED, false)) {
            migrateLegacy();
        }

        ClockStudioConfig defaults = ClockStudioConfig.safeDefault();
        int requestedName = getInt(preferences, KEY_NAME_ID, defaults.nameId);
        int nameId = AllahNameCatalog.usableId(context, requestedName);
        int themeId = ClockStudioConfig.clamp(getInt(preferences, KEY_THEME_ID, defaults.themeId), 1, 6);
        int type = getInt(preferences, KEY_CLOCK_TYPE, defaults.clockType);
        int styleIndex = Math.max(0, getInt(preferences, KEY_CLOCK_STYLE_INDEX, defaults.clockStyleIndex));
        String styleId = getString(preferences, KEY_CLOCK_STYLE_ID, "");
        if (styleId.isEmpty()) {
            styleId = defaultStyleForType(type);
        }
        if (!isKnownStyle(styleId)) {
            styleId = ClockStudioConfig.DEFAULT_CLOCK_STYLE_ID;
            type = ClockStudioConfig.DEFAULT_CLOCK_TYPE;
            styleIndex = ClockStudioConfig.DEFAULT_CLOCK_STYLE_INDEX;
        }
        if (type < 0 || type > 2) {
            type = typeForStyle(styleId);
        }

        return new ClockStudioConfig(
                nameId,
                themeId,
                styleId,
                type,
                styleIndex,
                getFloat(preferences, KEY_POSITION_X, defaults.positionX),
                getFloat(preferences, KEY_POSITION_Y, defaults.positionY),
                getFloat(preferences, KEY_SIZE_FRACTION, defaults.sizeFraction),
                getInt(preferences, KEY_CLOCK_COLOR, defaults.clockColor),
                getFloat(preferences, KEY_OPACITY, defaults.opacity),
                getBoolean(preferences, KEY_24_HOUR, defaults.is24Hour),
                getBoolean(preferences, KEY_SHOW_SECONDS, defaults.showSeconds),
                getBoolean(preferences, KEY_SHOW_HIJRI, defaults.showHijriDate),
                getBoolean(preferences, KEY_SHOW_GREGORIAN, defaults.showGregorianDate),
                getBoolean(preferences, KEY_SHOW_DAY_NAME, defaults.showDayName),
                ClockStudioConfig.clamp(getInt(preferences, KEY_HIJRI_ADJUSTMENT, defaults.hijriAdjustment), -2, 2),
                getInt(preferences, KEY_BACKGROUND_COLOR, defaults.backgroundColor),
                Math.max(0, getInt(preferences, KEY_BACKGROUND_RESOURCE, 0)),
                getString(preferences, KEY_BACKGROUND_PATH, ""),
                getBoolean(preferences, KEY_IMAGE_BACKGROUND, false),
                getBoolean(preferences, KEY_CUSTOM_BACKGROUND, false),
                getInt(preferences, KEY_TEXT_COLOR_1, defaults.textColor1),
                getInt(preferences, KEY_TEXT_COLOR_2, defaults.textColor2));
    }

    /** Saves every composition field in one transaction and mirrors legacy editor values. */
    public void save(ClockStudioConfig value) {
        ClockStudioConfig safe = value == null ? ClockStudioConfig.safeDefault() : value;
        int safeName = AllahNameCatalog.usableId(context, safe.nameId);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_MIGRATED, true);
        editor.putInt(KEY_NAME_ID, safeName);
        editor.putInt(KEY_THEME_ID, ClockStudioConfig.clamp(safe.themeId, 1, 6));
        boolean validStyle = isKnownStyle(safe.clockStyleId);
        editor.putString(KEY_CLOCK_STYLE_ID, validStyle
                ? safe.clockStyleId : ClockStudioConfig.DEFAULT_CLOCK_STYLE_ID);
        editor.putInt(KEY_CLOCK_TYPE, validStyle ? safe.clockType : ClockStudioConfig.DEFAULT_CLOCK_TYPE);
        editor.putInt(KEY_CLOCK_STYLE_INDEX, validStyle
                ? Math.max(0, safe.clockStyleIndex) : ClockStudioConfig.DEFAULT_CLOCK_STYLE_INDEX);
        editor.putFloat(KEY_POSITION_X, safe.positionX);
        editor.putFloat(KEY_POSITION_Y, safe.positionY);
        editor.putFloat(KEY_SIZE_FRACTION, safe.sizeFraction);
        editor.putInt(KEY_CLOCK_COLOR, safe.clockColor);
        editor.putFloat(KEY_OPACITY, safe.opacity);
        editor.putBoolean(KEY_24_HOUR, safe.is24Hour);
        editor.putBoolean(KEY_SHOW_SECONDS, safe.showSeconds);
        editor.putBoolean(KEY_SHOW_HIJRI, safe.showHijriDate);
        editor.putBoolean(KEY_SHOW_GREGORIAN, safe.showGregorianDate);
        editor.putBoolean(KEY_SHOW_DAY_NAME, safe.showDayName);
        editor.putInt(KEY_HIJRI_ADJUSTMENT, ClockStudioConfig.clamp(safe.hijriAdjustment, -2, 2));
        editor.putInt(KEY_BACKGROUND_COLOR, safe.backgroundColor);
        editor.putInt(KEY_BACKGROUND_RESOURCE, Math.max(0, safe.backgroundResource));
        editor.putString(KEY_BACKGROUND_PATH, safe.backgroundPath);
        editor.putBoolean(KEY_IMAGE_BACKGROUND, safe.imageBackground);
        editor.putBoolean(KEY_CUSTOM_BACKGROUND, safe.customBackground);
        editor.putInt(KEY_TEXT_COLOR_1, safe.textColor1);
        editor.putInt(KEY_TEXT_COLOR_2, safe.textColor2);
        editor.apply();
        mirrorLegacy(safe);
    }

    /** Updates the chosen clock while retaining date, name, theme and background settings. */
    public void selectClock(String styleId, int type, int styleIndex, int backgroundColor,
                            int backgroundResource, boolean customBackground) {
        ClockStudioConfig old = load();
        save(new ClockStudioConfig(old.nameId, old.themeId,
                isKnownStyle(styleId) ? styleId : ClockStudioConfig.DEFAULT_CLOCK_STYLE_ID,
                type, styleIndex, old.positionX, old.positionY, old.sizeFraction, old.clockColor,
                old.opacity, old.is24Hour, old.showSeconds, old.showHijriDate,
                old.showGregorianDate, old.showDayName, old.hijriAdjustment, backgroundColor,
                backgroundResource, old.backgroundPath, false, customBackground,
                old.textColor1, old.textColor2));
    }

    /** Persists the old editor's pixel-based controls as normalized, wallpaper-safe values. */
    public void saveEditorState(float xPixels, float yPixels, int widthPixels, int heightPixels,
                                int sizePixels, int textClockPosition, int textColor1,
                                int textColor2) {
        ClockStudioConfig old = load();
        float x = widthPixels <= 0 ? old.positionX : xPixels / (float) widthPixels;
        float y = heightPixels <= 0 ? old.positionY : yPixels / (float) heightPixels;
        int maxDimension = Math.max(widthPixels, heightPixels);
        float size = maxDimension <= 0 ? old.sizeFraction : sizePixels / (float) maxDimension;
        // The legacy editor still owns the background picker. Read its current values here so the
        // button saves the configuration the user is actually looking at, not an older Studio copy.
        int backgroundColor = getInt(legacyPreferences, "bgColor", old.backgroundColor);
        int backgroundResource = getInt(legacyPreferences, "customBg", old.backgroundResource);
        boolean imageBackground = getBoolean(legacyPreferences, "isImage", old.imageBackground);
        boolean customBackground = getBoolean(legacyPreferences, "isCustomBg", old.customBackground);
        String backgroundPath = getString(legacyPreferences, "ImageString", old.backgroundPath);
        int legacyType = getInt(legacyPreferences, "clockType", old.clockType);
        String styleId = old.clockStyleId;
        if (legacyType != old.clockType) {
            styleId = styleForLegacy(legacyType, textClockPosition);
        }
        save(new ClockStudioConfig(old.nameId, old.themeId, styleId, legacyType,
                textClockPosition, x, y, size, old.clockColor, old.opacity, old.is24Hour,
                old.showSeconds, old.showHijriDate, old.showGregorianDate, old.showDayName,
                old.hijriAdjustment, backgroundColor, backgroundResource, backgroundPath,
                imageBackground, customBackground, textColor1, textColor2));
    }

    /** Saves the presentation switches without replacing the selected clock or background. */
    public void saveDisplaySettings(int themeId, boolean is24Hour, boolean showSeconds,
                                    boolean showHijriDate, boolean showGregorianDate,
                                    boolean showDayName, int hijriAdjustment) {
        ClockStudioConfig old = load();
        save(new ClockStudioConfig(old.nameId, themeId, old.clockStyleId, old.clockType,
                old.clockStyleIndex, old.positionX, old.positionY, old.sizeFraction,
                old.clockColor, old.opacity, is24Hour, showSeconds, showHijriDate,
                showGregorianDate, showDayName, hijriAdjustment, old.backgroundColor,
                old.backgroundResource, old.backgroundPath, old.imageBackground,
                old.customBackground, old.textColor1, old.textColor2));
    }

    /** Selects a Name only when it is free or already permanently unlocked. */
    public boolean selectName(int requestedId) {
        if (!AllahNameCatalog.isValid(requestedId)
                || !AllahNameCatalog.isUsable(context, requestedId)) {
            return false;
        }
        if (!preferences.getBoolean(KEY_MIGRATED, false)) {
            load();
        }
        preferences.edit().putBoolean(KEY_MIGRATED, true)
                .putInt(KEY_NAME_ID, requestedId).apply();
        return true;
    }

    public boolean canUseSelectedName() {
        return AllahNameCatalog.isUsable(context, getInt(preferences, KEY_NAME_ID,
                ClockStudioConfig.DEFAULT_NAME_ID));
    }

    private void migrateLegacy() {
        ClockStudioConfig defaults = ClockStudioConfig.safeDefault();
        int type = getInt(legacyPreferences, "clockType", defaults.clockType);
        if (type < 0 || type > 2) {
            type = defaults.clockType;
        }
        int styleIndex = Math.max(0, getInt(legacyPreferences, "textClockPosition", defaults.clockStyleIndex));
        String styleId = styleForLegacy(type, styleIndex);
        String serializedClock = getString(legacyPreferences, "clocks", "");
        if (type == 0 && !serializedClock.isEmpty()) {
            try {
                Clocks clock = (Clocks) new TinyDB(context).getObject("clocks", Clocks.class);
                if (clock != null && isKnownStyle(clock.id)) {
                    styleId = clock.id;
                }
            } catch (Exception ignored) {
                // A damaged legacy value is handled by the safe analog default below.
            }
        }
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        float x = getFloat(legacyPreferences, "prefClockPosX", width / 2.0f)
                / Math.max(1, width);
        float y = getFloat(legacyPreferences, "prefClockPosY", height / 2.0f)
                / Math.max(1, height);
        int oldSize = getInt(legacyPreferences, "prefSize", 0);
        float size = oldSize <= 0 ? defaults.sizeFraction
                : oldSize / (float) Math.max(width, height);
        int backgroundColor = getInt(legacyPreferences, "bgColor", defaults.backgroundColor);
        boolean image = getBoolean(legacyPreferences, "isImage", false);
        boolean custom = getBoolean(legacyPreferences, "isCustomBg", false);
        int resource = getInt(legacyPreferences, "customBg", 0);
        String path = getString(legacyPreferences, "ImageString", "");
        save(new ClockStudioConfig(defaults.nameId, defaults.themeId, styleId, type, styleIndex,
                x, y, size, defaults.clockColor, defaults.opacity, false,
                true, false, true, true, 0, backgroundColor, resource, path, image,
                custom, getInt(legacyPreferences, "textColor1", defaults.textColor1),
                getInt(legacyPreferences, "textColor2", defaults.textColor2)));
    }

    private void mirrorLegacy(ClockStudioConfig value) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        SharedPreferences.Editor editor = legacyPreferences.edit();
        editor.putInt("clockType", value.clockType);
        editor.putInt("textClockPosition", value.clockStyleIndex);
        editor.putFloat("prefClockPosX", value.positionX * Math.max(1, width));
        editor.putFloat("prefClockPosY", value.positionY * Math.max(1, height));
        editor.putInt("prefSize", Math.round(value.sizeFraction * Math.max(width, height)));
        editor.putInt("bgColor", value.backgroundColor);
        editor.putInt("customBg", value.backgroundResource);
        editor.putBoolean("isImage", value.imageBackground);
        editor.putBoolean("isCustomBg", value.customBackground);
        editor.putString("ImageString", value.backgroundPath);
        editor.putInt("textColor1", value.textColor1);
        editor.putInt("textColor2", value.textColor2);
        editor.apply();
    }

    private static String styleForLegacy(int type, int styleIndex) {
        String prefix = type == 0 ? "analog" : type == 1 ? "smart" : "digital";
        int number = Math.max(1, styleIndex + 1);
        return prefix + "_" + (number < 10 ? "0" + number : String.valueOf(number));
    }

    private static String defaultStyleForType(int type) {
        return type == 1 ? "smart_01" : type == 2 ? "digital_01" : ClockStudioConfig.DEFAULT_CLOCK_STYLE_ID;
    }

    public static int typeForStyle(String styleId) {
        if (styleId == null) {
            return 0;
        }
        if (styleId.startsWith("smart")) {
            return 1;
        }
        if (styleId.startsWith("digital") || styleId.startsWith("neon")
                || styleId.startsWith("glass") || styleId.startsWith("luxury")
                || styleId.startsWith("hybrid")) {
            return 2;
        }
        return 0;
    }

    /** Validates both the existing catalog ids and the named premium style families. */
    public static boolean isKnownStyle(String styleId) {
        if (styleId == null || styleId.isEmpty()) {
            return false;
        }
        if (styleId.matches("analog_[0-9]{1,2}")) {
            return catalogNumber(styleId) <= 14;
        }
        if (styleId.matches("digital_[0-9]{1,2}")) {
            return catalogNumber(styleId) <= 12;
        }
        if (styleId.matches("smart_[0-9]{1,2}")) {
            return catalogNumber(styleId) <= 12;
        }
        return styleId.matches("(neon|glass|luxury|hybrid)_[A-Za-z0-9_-]+");
    }

    private static int catalogNumber(String styleId) {
        try {
            return Integer.parseInt(styleId.substring(styleId.indexOf('_') + 1));
        } catch (RuntimeException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static String getString(SharedPreferences source, String key, String fallback) {
        try {
            return source.getString(key, fallback);
        } catch (ClassCastException ignored) {
            return fallback;
        }
    }

    private static int getInt(SharedPreferences source, String key, int fallback) {
        try {
            return source.getInt(key, fallback);
        } catch (ClassCastException ignored) {
            return fallback;
        }
    }

    private static float getFloat(SharedPreferences source, String key, float fallback) {
        try {
            return source.getFloat(key, fallback);
        } catch (ClassCastException ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(SharedPreferences source, String key, boolean fallback) {
        try {
            return source.getBoolean(key, fallback);
        } catch (ClassCastException ignored) {
            return fallback;
        }
    }
}
