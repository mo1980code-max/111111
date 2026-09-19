package com.clock.livewallpaper.model;

import com.clock.livewallpaper.catalog.Unlockable;

/**
 * A digital clock style.
 *
 * <p>{@code style} is the branch the renderers take ({@code TextClockPreview} in the editor and the
 * live wallpaper service), while {@code bgColor} is the card/wallpaper colour. The catalog therefore
 * can offer several colourways of one style without the wallpaper ever falling back to a default: the
 * list position and the render style are separate numbers on purpose.
 */
public class TextClocks implements Unlockable {
    String bgColor;
    int thumb;
    String id;
    String previewAsset;
    boolean free;
    int style;

    public TextClocks() {
    }

    public TextClocks(int thumb, String bgColor) {
        this.thumb = thumb;
        this.bgColor = bgColor;
        this.style = 0;
    }

    public TextClocks(int thumb, String bgColor, String id, String previewAsset, int style, boolean free) {
        this(thumb, bgColor);
        this.id = id;
        this.previewAsset = previewAsset;
        this.style = style;
        this.free = free;
    }

    public int getThumb() {
        return this.thumb;
    }

    public void setThumb(int i) {
        this.thumb = i;
    }

    public String getBgColor() {
        return this.bgColor;
    }

    public void setBgColor(String str) {
        this.bgColor = str;
    }

    /** Index into the clock renderer's style switch (0 based). */
    public int getStyle() {
        return style;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getUnlockId() {
        return "clock:" + id;
    }

    @Override
    public boolean isClockContent() {
        return true;
    }


    @Override
    public String getPreviewAsset() {
        return previewAsset;
    }

    @Override
    public boolean isFreeByDefault() {
        return free;
    }
}
