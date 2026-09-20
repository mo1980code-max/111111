package com.clock.livewallpaper.clock;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/** Immutable description of one Clock Studio style. */
public final class ClockStyle {

    public enum Kind {
        ANALOG,
        DIGITAL
    }

    public enum Family {
        ANALOG,
        DIGITAL,
        NEON,
        GLASS,
        HYBRID
    }

    private final String id;
    private final int titleResId;
    private final int familyResId;
    private final Kind kind;
    private final Family family;
    private final int previewColor;
    private final boolean secondaryDigitalClock;

    public ClockStyle(@NonNull String id,
                      int titleResId,
                      int familyResId,
                      @NonNull Kind kind,
                      @NonNull Family family,
                      @ColorInt int previewColor) {
        this(id, titleResId, familyResId, kind, family, previewColor, false);
    }

    public ClockStyle(@NonNull String id,
                      int titleResId,
                      int familyResId,
                      @NonNull Kind kind,
                      @NonNull Family family,
                      @ColorInt int previewColor,
                      boolean secondaryDigitalClock) {
        this.id = id;
        this.titleResId = titleResId;
        this.familyResId = familyResId;
        this.kind = kind;
        this.family = family;
        this.previewColor = previewColor;
        this.secondaryDigitalClock = secondaryDigitalClock;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getFamilyResId() {
        return familyResId;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public Family getFamily() {
        return family;
    }

    @ColorInt
    public int getPreviewColor() {
        return previewColor;
    }

    public boolean isAnalog() {
        return kind == Kind.ANALOG;
    }

    public boolean isNeon() {
        return family == Family.NEON;
    }

    public boolean isGlass() {
        return family == Family.GLASS;
    }

    public boolean isHybrid() {
        return family == Family.HYBRID;
    }

    public boolean hasSecondaryDigitalClock() {
        return secondaryDigitalClock;
    }

    /** Hybrid variants can borrow the visual treatment of Stage 2 neon styles. */
    public boolean usesNeonTreatment() {
        return isNeon() || "hybrid_neon".equals(id);
    }

    /** Hybrid glass keeps the same translucent, border-and-shadow treatment. */
    public boolean usesGlassTreatment() {
        return isGlass() || "hybrid_glass".equals(id);
    }
}
