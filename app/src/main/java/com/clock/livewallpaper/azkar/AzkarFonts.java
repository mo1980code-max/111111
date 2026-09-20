package com.clock.livewallpaper.azkar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads the Arabic typefaces offered for the dhikr text. All bundled faces ship inside the
 * APK ({@code assets/fonts/}, SIL OFL), so switching fonts never needs the network:
 *
 * <ul>
 *   <li>{@code default} — the platform default typeface (nothing loaded);</li>
 *   <li>{@code amiri} — Amiri Quran ({@code quran_font.ttf}), already shipped for the Quran
 *       reader and the historical Azkar look;</li>
 *   <li>{@code cairo} — Cairo ({@code cairo_regular.ttf});</li>
 *   <li>{@code tajawal} — Tajawal ({@code tajawal_regular.ttf}).</li>
 * </ul>
 *
 * <p>Each face is loaded once and cached; when an asset is missing the caller falls back
 * to the platform default instead of crashing.
 */
public final class AzkarFonts {

    private static final String TAG = "AzkarFonts";
    private static final String AMIRI_ASSET = "fonts/quran_font.ttf";
    private static final String CAIRO_ASSET = "fonts/cairo_regular.ttf";
    private static final String TAJAWAL_ASSET = "fonts/tajawal_regular.ttf";

    private static final Map<String, Typeface> cache = new HashMap<>();
    private static final Object lock = new Object();

    private AzkarFonts() { }

    /**
     * @return the cached Amiri face, or {@code null} when it cannot be loaded
     */
    @Nullable
    public static Typeface arabic(@NonNull Context context) {
        return typefaceFor(context, AzkarFontStore.FAMILY_AMIRI);
    }

    /**
     * @return the typeface for {@code family}, or {@code null} for
     *         {@link AzkarFontStore#FAMILY_DEFAULT} (platform default) and for any face
     *         that cannot be loaded — callers treat {@code null} as "use default"
     */
    @Nullable
    public static Typeface typefaceFor(@NonNull Context context, @NonNull String family) {
        String asset;
        if (AzkarFontStore.FAMILY_CAIRO.equals(family)) {
            asset = CAIRO_ASSET;
        } else if (AzkarFontStore.FAMILY_TAJAWAL.equals(family)) {
            asset = TAJAWAL_ASSET;
        } else if (AzkarFontStore.FAMILY_AMIRI.equals(family)) {
            asset = AMIRI_ASSET;
        } else {
            return null; // النظام الافتراضي: لا خط مخصص.
        }
        synchronized (lock) {
            if (cache.containsKey(asset)) {
                return cache.get(asset);
            }
            Typeface face = null;
            try {
                face = Typeface.createFromAsset(
                        context.getApplicationContext().getAssets(), asset);
            } catch (Exception e) {
                Log.e(TAG, "Unable to load " + asset, e);
            }
            cache.put(asset, face);
            return face;
        }
    }
}
