package com.clock.livewallpaper.azkar;

import androidx.annotation.NonNull;

/**
 * One Azkar: the imported content plus the live counter state.
 *
 * <p>Content fields ({@code id}, {@code category}, {@code order}, {@code arabicText},
 * {@code repeatCount}, {@code virtue}) come verbatim from {@code assets/azkar.json} and never
 * change at runtime. {@code currentCount} is the persisted tap count; {@link #isCompleted()} is
 * derived, never stored — an item is complete exactly when
 * {@code currentCount == repeatCount}.
 *
 * <p>Every item carries its own {@code repeatCount} from the source (1, 3, 4, 7, 10, 100, ...);
 * the counter target is always that number, never a shared constant.
 */
public final class AzkarItem {

    private final int id;
    private final AzkarCategory category;
    private final int order;
    private final String arabicText;
    private final int repeatCount;
    private final String virtue;

    private int currentCount;

    public AzkarItem(int id,
                     @NonNull AzkarCategory category,
                     int order,
                     @NonNull String arabicText,
                     int repeatCount,
                     @NonNull String virtue,
                     int currentCount) {
        this.id = id;
        this.category = category;
        this.order = order;
        this.arabicText = arabicText;
        this.repeatCount = Math.max(1, repeatCount);
        this.virtue = virtue;
        setCurrentCount(currentCount);
    }

    /** @return the 1-based Azkar number within its category, as numbered by the source */
    public int id() {
        return id;
    }

    /** @return the section this item belongs to */
    @NonNull
    public AzkarCategory category() {
        return category;
    }

    /** @return the display order, matching the source order */
    public int order() {
        return order;
    }

    /** @return the full Arabic dhikr text, verbatim from the source */
    @NonNull
    public String arabicText() {
        return arabicText;
    }

    /** @return the exact required repetitions for THIS item, from the source */
    public int repeatCount() {
        return repeatCount;
    }

    /** @return the virtue/reward line from the source, or {@code ""} when the source gives none */
    @NonNull
    public String virtue() {
        return virtue;
    }

    /** @return taps so far, always within {@code [0, repeatCount]} */
    public int currentCount() {
        return currentCount;
    }

    /** @return {@code true} exactly when {@code currentCount == repeatCount} */
    public boolean isCompleted() {
        return currentCount >= repeatCount;
    }

    /**
     * Advances the counter by one tap. Never exceeds {@link #repeatCount()}.
     *
     * @return {@code true} if the tap changed the count, {@code false} if already complete
     */
    public boolean countUp() {
        if (currentCount >= repeatCount) {
            return false;
        }
        currentCount++;
        return true;
    }

    /** Resets the counter to zero so the Azkar can be repeated. */
    public void reset() {
        currentCount = 0;
    }

    private void setCurrentCount(int value) {
        currentCount = Math.max(0, Math.min(value, repeatCount));
    }
}
