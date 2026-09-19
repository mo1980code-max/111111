package com.clock.livewallpaper.catalog;

/** One wallpaper in the local catalog. Immutable, and safe to pass around by id. */
public final class WallpaperEntry implements Unlockable {

    private final String sectionId;
    private final String id;
    private final String assetPath;
    private final String title;
    private final boolean free;

    public WallpaperEntry(String sectionId, String id, String assetPath, String title, boolean free) {
        this.sectionId = sectionId;
        this.id = id;
        this.assetPath = assetPath;
        this.title = title;
        this.free = free;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getId() {
        return id;
    }

    /** Path of the image inside {@code assets/}, e.g. {@code wallpapers/aqsa/aqsa_2.jpg}. */
    public String getAssetPath() {
        return assetPath;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    @Override
    public String getUnlockId() {
        return "wallpaper:" + id;
    }

    @Override
    public boolean isClockContent() {
        return false;
    }


    @Override
    public String getPreviewAsset() {
        return assetPath;
    }

    @Override
    public boolean isFreeByDefault() {
        return free;
    }

    @Override
    public String toString() {
        return "WallpaperEntry{" + id + ", " + assetPath + ", free=" + free + "}";
    }
}
