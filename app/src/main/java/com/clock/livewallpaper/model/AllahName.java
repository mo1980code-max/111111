package com.clock.livewallpaper.model;

import androidx.annotation.NonNull;

import com.clock.livewallpaper.catalog.Unlockable;

import java.util.Locale;

/**
 * One of the 99 Names of Allah.
 *
 * <p>The identifier is deliberately independent from the display text so that the catalog can be
 * translated or reworded later without changing the identity of an item.</p>
 */
public final class AllahName implements Unlockable {

    private final int number;
    private final String id;
    private final String arabicName;
    private final String transliteration;

    public AllahName(int number,
                     @NonNull String id,
                     @NonNull String arabicName,
                     @NonNull String transliteration) {
        if (number < 1 || number > 99) {
            throw new IllegalArgumentException("Allah name number must be between 1 and 99");
        }
        this.number = number;
        this.id = id;
        this.arabicName = arabicName;
        this.transliteration = transliteration;
    }

    /** The canonical 1-based position in the traditional list. */
    public int getNumber() {
        return number;
    }

    /** Stable machine-readable id, safe to persist. */
    @NonNull
    public String getId() {
        return id;
    }

    /** Alias that makes the stability contract explicit at call sites. */
    @NonNull
    public String getStableId() {
        return id;
    }

    /** Arabic text rendered by Android TextView, never baked into an image. */
    @NonNull
    public String getArabicName() {
        return arabicName;
    }

    @NonNull
    public String getTransliteration() {
        return transliteration;
    }

    /** Permanent unlock namespace, independent of RecyclerView positions or display text. */
    @Override
    @NonNull
    public String getUnlockId() {
        return String.format(Locale.US, "allah_name_%02d", number);
    }

    /** Names 1 through 9 are the permanent free tier for Stage 4. */
    @Override
    public boolean isFreeByDefault() {
        return number <= 9;
    }

    /** This feature has no image asset; the Clock Studio is opened after access is granted. */
    @Override
    @NonNull
    public String getPreviewAsset() {
        return "";
    }

    @Override
    public boolean isClockContent() {
        return false;
    }

    @Override
    public boolean isAllahNameContent() {
        return true;
    }
}
