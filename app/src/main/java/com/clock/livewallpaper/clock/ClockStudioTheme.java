package com.clock.livewallpaper.clock;

import android.graphics.Color;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.clock.livewallpaper.R;

/**
 * Stable theme identifiers shared by Clock Studio and the live-wallpaper renderer.
 *
 * <p>The gallery theme is deterministic per Name, but the identifier is persisted with a wallpaper
 * snapshot so a future catalog change cannot silently change an already-applied composition.</p>
 */
public final class ClockStudioTheme {

    public static final String NAVY = "navy";
    public static final String EMERALD = "emerald";
    public static final String BLACK_GOLD = "black_gold";
    public static final String SAPPHIRE = "sapphire";
    public static final String FOREST = "forest";
    public static final String PLUM = "plum";

    private ClockStudioTheme() {
    }

    @NonNull
    public static String idForName(int number) {
        switch ((number - 1) % 6) {
            case 1:
                return EMERALD;
            case 2:
                return BLACK_GOLD;
            case 3:
                return SAPPHIRE;
            case 4:
                return FOREST;
            case 5:
                return PLUM;
            default:
                return NAVY;
        }
    }

    @NonNull
    public static String safeId(String id, int nameNumber) {
        if (isValid(id)) {
            return id;
        }
        return idForName(nameNumber);
    }

    public static boolean isValid(String id) {
        return NAVY.equals(id) || EMERALD.equals(id) || BLACK_GOLD.equals(id)
                || SAPPHIRE.equals(id) || FOREST.equals(id) || PLUM.equals(id);
    }

    @DrawableRes
    public static int drawableForName(int number) {
        return drawableForId(idForName(number));
    }

    @DrawableRes
    public static int drawableForId(String id) {
        if (EMERALD.equals(id)) {
            return R.drawable.bg_allah_theme_emerald;
        }
        if (BLACK_GOLD.equals(id)) {
            return R.drawable.bg_allah_theme_black_gold;
        }
        if (SAPPHIRE.equals(id)) {
            return R.drawable.bg_allah_theme_sapphire;
        }
        if (FOREST.equals(id)) {
            return R.drawable.bg_allah_theme_forest;
        }
        if (PLUM.equals(id)) {
            return R.drawable.bg_allah_theme_plum;
        }
        return R.drawable.bg_allah_theme_navy;
    }

    public static int startColor(String id) {
        if (EMERALD.equals(id)) {
            return Color.rgb(17, 116, 92);
        }
        if (BLACK_GOLD.equals(id)) {
            return Color.rgb(74, 55, 18);
        }
        if (SAPPHIRE.equals(id)) {
            return Color.rgb(38, 63, 116);
        }
        if (FOREST.equals(id)) {
            return Color.rgb(40, 88, 68);
        }
        if (PLUM.equals(id)) {
            return Color.rgb(85, 58, 101);
        }
        return Color.rgb(23, 59, 89);
    }

    public static int centerColor(String id) {
        if (EMERALD.equals(id)) {
            return Color.rgb(11, 75, 64);
        }
        if (BLACK_GOLD.equals(id)) {
            return Color.rgb(33, 24, 10);
        }
        if (SAPPHIRE.equals(id)) {
            return Color.rgb(25, 44, 85);
        }
        if (FOREST.equals(id)) {
            return Color.rgb(22, 61, 54);
        }
        if (PLUM.equals(id)) {
            return Color.rgb(48, 34, 71);
        }
        return Color.rgb(13, 38, 63);
    }

    public static int endColor(String id) {
        if (EMERALD.equals(id)) {
            return Color.rgb(6, 42, 42);
        }
        if (BLACK_GOLD.equals(id)) {
            return Color.rgb(5, 5, 5);
        }
        if (SAPPHIRE.equals(id)) {
            return Color.rgb(11, 24, 53);
        }
        if (FOREST.equals(id)) {
            return Color.rgb(10, 33, 31);
        }
        if (PLUM.equals(id)) {
            return Color.rgb(20, 22, 45);
        }
        return Color.rgb(7, 21, 34);
    }

    public static int accentColor(String id) {
        if (EMERALD.equals(id)) {
            return Color.rgb(201, 162, 76);
        }
        if (BLACK_GOLD.equals(id)) {
            return Color.rgb(224, 185, 94);
        }
        if (SAPPHIRE.equals(id)) {
            return Color.rgb(184, 199, 232);
        }
        if (FOREST.equals(id)) {
            return Color.rgb(143, 200, 164);
        }
        if (PLUM.equals(id)) {
            return Color.rgb(208, 168, 213);
        }
        return Color.rgb(109, 155, 171);
    }
}
