package com.clock.livewallpaper.azkar;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * The persisted Azkar display settings: dhikr text size, Arabic font family, and the
 * Azkar-only night mode. This is the single place in the Azkar code that touches storage.
 *
 * <p>Counters are deliberately NOT stored anywhere (see {@link AzkarItem}): every time the
 * category screen opens, fresh items are built and each counter starts again from its own
 * original {@code repeatCount}. Only the three display settings below survive closing the
 * app — counters always restart.
 *
 * <p>Storage: {@link SharedPreferences} file {@code "azkar_prefs"}. All writes use
 * {@link SharedPreferences.Editor#apply()}.
 *
 * <p>Process-wide singleton: obtain it through {@link #get(Context)}.
 */
public final class AzkarFontStore {

    /** Name of the preferences file, stable across versions. */
    public static final String PREFS_NAME = "azkar_prefs";

    /** Persisted key: the dhikr text size in sp (float). */
    public static final String KEY_FONT_SIZE = "dhikr_font_size";
    /** Persisted key: the Arabic font family (string, one of the FAMILY_* values). */
    public static final String KEY_FONT_FAMILY = "dhikr_font_family";
    /** Persisted key: the Azkar-section night mode (boolean). */
    public static final String KEY_NIGHT_MODE = "azkar_night_mode";

    /** System default typeface (no bundled font). */
    public static final String FAMILY_DEFAULT = "default";
    /** Amiri Quran (bundled, also the reader's face) — the default, keeps current look. */
    public static final String FAMILY_AMIRI = "amiri";
    /** Cairo (bundled). */
    public static final String FAMILY_CAIRO = "cairo";
    /** Tajawal (bundled). */
    public static final String FAMILY_TAJAWAL = "tajawal";

    /** Default reading size, matching the original card design. */
    public static final float DEFAULT_SP = 20f;
    /** Smallest size the minus button can reach. */
    public static final float MIN_SP = 14f;
    /** Largest size the plus button can reach. */
    public static final float MAX_SP = 32f;
    /** One tap on + / − changes the size by this many sp. */
    public static final float STEP_SP = 2f;

    private static volatile AzkarFontStore instance;

    private final SharedPreferences prefs;

    private AzkarFontStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return the process-wide instance, creating it on first use */
    @NonNull
    public static AzkarFontStore get(@NonNull Context context) {
        AzkarFontStore local = instance;
        if (local == null) {
            synchronized (AzkarFontStore.class) {
                local = instance;
                if (local == null) {
                    local = new AzkarFontStore(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    /** @return the saved size in sp, or {@link #DEFAULT_SP} on first launch */
    public float getSp() {
        return clamp(prefs.getFloat(KEY_FONT_SIZE, DEFAULT_SP));
    }

    /** Saves the size in sp (clamped), so it survives closing the app. */
    public void setSp(float sp) {
        prefs.edit().putFloat(KEY_FONT_SIZE, clamp(sp)).apply();
    }

    /** @return {@code sp} clamped into [{@link #MIN_SP}, {@link #MAX_SP}] */
    public static float clamp(float sp) {
        return Math.max(MIN_SP, Math.min(sp, MAX_SP));
    }

    /**
     * @return the saved font family ({@link #FAMILY_DEFAULT}, {@link #FAMILY_AMIRI},
     *         {@link #FAMILY_CAIRO} or {@link #FAMILY_TAJAWAL}); unknown stored values fall
     *         back to {@link #FAMILY_AMIRI}, the historical look
     */
    @NonNull
    public String getFontFamily() {
        String family = prefs.getString(KEY_FONT_FAMILY, FAMILY_AMIRI);
        if (FAMILY_DEFAULT.equals(family) || FAMILY_AMIRI.equals(family)
                || FAMILY_CAIRO.equals(family) || FAMILY_TAJAWAL.equals(family)) {
            return family;
        }
        return FAMILY_AMIRI;
    }

    /** Saves the font family so it survives closing the app. */
    public void setFontFamily(@NonNull String family) {
        if (!FAMILY_DEFAULT.equals(family) && !FAMILY_AMIRI.equals(family)
                && !FAMILY_CAIRO.equals(family) && !FAMILY_TAJAWAL.equals(family)) {
            family = FAMILY_AMIRI;
        }
        prefs.edit().putString(KEY_FONT_FAMILY, family).apply();
    }

    /** @return {@code true} when the Azkar-section night mode is enabled */
    public boolean isNightMode() {
        return prefs.getBoolean(KEY_NIGHT_MODE, false);
    }

    /** Saves the night-mode choice so it survives closing the app. */
    public void setNightMode(boolean night) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, night).apply();
    }
}
