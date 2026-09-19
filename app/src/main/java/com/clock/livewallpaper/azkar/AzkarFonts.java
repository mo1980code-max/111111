package com.clock.livewallpaper.azkar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Loads the Arabic typeface for the dhikr text: the bundled Amiri Quran font
 * ({@code assets/fonts/quran_font.ttf}, SIL OFL) already shipped for the Quran reader, so the
 * Azkar screens reuse a proven, highly readable Arabic face with zero extra APK weight.
 *
 * <p>The typeface is loaded once and cached; when the asset is missing the callers fall back
 * to the platform default instead of crashing.
 */
public final class AzkarFonts {

    private static final String TAG = "AzkarFonts";
    private static final String ARABIC_FONT_ASSET = "fonts/quran_font.ttf";

    private static volatile Typeface arabic;
    private static volatile boolean attempted;

    private AzkarFonts() { }

    /**
     * @return the cached Arabic typeface, or {@code null} when it cannot be loaded
     */
    @Nullable
    public static Typeface arabic(@NonNull Context context) {
        if (!attempted) {
            synchronized (AzkarFonts.class) {
                if (!attempted) {
                    attempted = true;
                    try {
                        arabic = Typeface.createFromAsset(
                                context.getApplicationContext().getAssets(), ARABIC_FONT_ASSET);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to load " + ARABIC_FONT_ASSET, e);
                        arabic = null;
                    }
                }
            }
        }
        return arabic;
    }
}
