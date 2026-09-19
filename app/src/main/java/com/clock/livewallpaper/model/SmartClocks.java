package com.clock.livewallpaper.model;

import com.clock.livewallpaper.catalog.Unlockable;

/**
 * A smart clock: a widget style laid over one of the bundled background images.
 *
 * <p>{@code backgroundRes} is what the editor stores as {@code customBg} and {@code style} is the
 * {@code SmartClockPreview} branch, again independent of the row index in the browser.
 */
public class SmartClocks implements Unlockable {
    int backgroundRes;
    int thumb;
    String id;
    String previewAsset;
    boolean free;
    int style;

    public SmartClocks() {
    }

    public SmartClocks(int thumb, int backgroundRes) {
        this.thumb = thumb;
        this.backgroundRes = backgroundRes;
        this.style = 0;
    }

    public SmartClocks(int thumb, int backgroundRes, String id, String previewAsset, int style, boolean free) {
        this(thumb, backgroundRes);
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

    /** Drawable resource of the wallpaper behind the widget. */
    public int getBackgroundRes() {
        return this.backgroundRes;
    }

    public void setBackgroundRes(int i) {
        this.backgroundRes = i;
    }

    /** Legacy name kept for the editor call sites; this is a drawable id, not a colour. */
    public int getBgColor() {
        return this.backgroundRes;
    }

    public void setBgColor(int i) {
        this.backgroundRes = i;
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
