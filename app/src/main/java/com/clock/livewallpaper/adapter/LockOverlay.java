package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.catalog.Unlockable;
import com.clock.livewallpaper.image.LocalImage;

/**
 * Shared tile behaviour for the clock and wallpaper grids.
 *
 * <p>A tile has exactly two looks, and they come from the same code path so a list can never show a
 * stale state: available content is drawn at full brightness with no badge, locked content is dimmed
 * by {@code lockScrim} and carries the padlock pill {@code lockBadge}. Tapping either kind is decided
 * by the Activity through {@code ContentAccess}, never here.
 */
public final class LockOverlay {

    private LockOverlay() {
    }

    /** True when the item may be opened without an ad (free tier, or a reward already earned). */
    public static boolean isAvailable(@NonNull Context context, @NonNull Unlockable item) {
        return com.clock.livewallpaper.catalog.ContentAccess.isAvailable(context, item);
    }

    /**
     * Applies the locked or unlocked look to a tile.
     *
     * <p>A locked tile is always drawn as <b>the original preview, dimmed by {@code lockScrim}, with
     * the padlock pill {@code lockBadge} on top</b>. The primary content view (a bundled preview, or
     * the one live editor view) is kept <b>VISIBLE in both states</b> -- the user must keep seeing the
     * preview of what they are about to unlock, so it must never be hidden with {@code View.GONE}.
     *
     * @param primary primary content view (normally a tile image; the editor may supply a live view).
     *                Always kept visible; the lock state only adds the scrim and the badge above it.
     */
    public static void apply(@NonNull View tile, @Nullable View primary, boolean locked) {
        View scrim = tile.findViewById(R.id.lockScrim);
        View badge = tile.findViewById(R.id.lockBadge);
        if (scrim != null) {
            scrim.setVisibility(locked ? View.VISIBLE : View.GONE);
        }
        if (badge != null) {
            badge.setVisibility(locked ? View.VISIBLE : View.GONE);
        }
        if (primary != null) {
            // Never hide the preview for locked content: locked look = preview + scrim + badge.
            primary.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Loads the bundled preview artwork. Every preview ships inside the APK, so a locked tile is fully
     * drawn with no network request; the same call is used for unlocked tiles that cannot render live.
     */
    public static void loadPreview(@NonNull ImageView target, @NonNull String assetPath,
                                   @NonNull Context context) {
        LocalImage.into(target, assetPath, previewEdgePx(context));
    }

    /** Decoding width for a grid tile: about 200dp, enough for a two-column card and cheap to keep. */
    public static int previewEdgePx(@NonNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f,
                context.getResources().getDisplayMetrics());
    }
}
