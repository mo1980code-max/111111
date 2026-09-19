package com.clock.livewallpaper.model;

import com.clock.livewallpaper.catalog.Unlockable;

/**
 * One analog dial: the face art, the three hand images and the colour the card and editor use.
 *
 * <p>It also carries the unlock metadata ({@link Unlockable}) so the browser and the live wallpaper
 * read exactly the same object -- a clock is previewed, unlocked and applied from one definition.
 */
public class Clocks implements Unlockable {
    public int backroundImage;
    public String bgColor;
    public int hourHand;
    public int minuteHand;
    public int secondHand;
    public String textColor;
    public String id;
    public String previewAsset;
    public boolean free;

    public Clocks() {
    }

    public Clocks(int i, int i2, int i3, int i4, String str, String str2) {
        this.backroundImage = i;
        this.hourHand = i2;
        this.minuteHand = i3;
        this.secondHand = i4;
        this.textColor = str;
        this.bgColor = str2;
    }

    /** Full form used by the catalog: dial art + colours + unlock identity. */
    public Clocks(int backroundImage, int hourHand, int minuteHand, int secondHand, String textColor,
                  String bgColor, String id, String previewAsset, boolean free) {
        this(backroundImage, hourHand, minuteHand, secondHand, textColor, bgColor);
        this.id = id;
        this.previewAsset = previewAsset;
        this.free = free;
    }

    public String getBgColor() {
        return this.bgColor;
    }

    public void setBgColor(String str) {
        this.bgColor = str;
    }

    public String getTextColor() {
        return this.textColor;
    }

    public void setTextColor(String str) {
        this.textColor = str;
    }

    public int getBackroundImage() {
        return this.backroundImage;
    }

    public void setBackroundImage(int i) {
        this.backroundImage = i;
    }

    public int getHourHand() {
        return this.hourHand;
    }

    public void setHourHand(int i) {
        this.hourHand = i;
    }

    public int getMinuteHand() {
        return this.minuteHand;
    }

    public void setMinuteHand(int i) {
        this.minuteHand = i;
    }

    public int getSecondHand() {
        return this.secondHand;
    }

    public void setSecondHand(int i) {
        this.secondHand = i;
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

    public String getId() {
        return id;
    }
}
