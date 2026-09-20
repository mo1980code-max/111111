package com.clock.livewallpaper.catalog;

/**
 * A piece of content that is either free or unlocked permanently with a rewarded ad.
 *
 * <p>Implementations must return a stable {@link #getUnlockId()}: the id is the SharedPreferences key
 * that records the earned reward, so it may never be derived from a list position that can shift when
 * the catalog grows.
 */
public interface Unlockable {

    /** Namespaced, stable id, e.g. {@code clock:analog_03} or {@code wallpaper:mecca_5}. */
    String getUnlockId();

    /** Asset path of the local preview image, e.g. {@code previews/clock/analog/analog_03.png}. */
    String getPreviewAsset();

    /** True for the free tier of every section: usable without ever touching an ad. */
    boolean isFreeByDefault();

    /**
     * True for clocks, false for wallpapers. It only picks the wording of the unlock dialog ("watch an
     * ad to unlock this clock" vs "... this wallpaper"), never the mechanics: both kinds use the same
     * rewarded flow and the same permanent local unlock record.
     */
    boolean isClockContent();

    /** True for the Names of Allah namespace; lets the shared prompt use name-specific wording. */
    default boolean isAllahNameContent() {
        return false;
    }
}
