package com.clock.livewallpaper.azkar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.clock.livewallpaper.R;

/**
 * The three Azkar sections. Each category is a self-contained list imported from its own
 * mandatory source; counts are never shared or assumed between categories.
 */
public enum AzkarCategory {

    /** Morning Azkar (أذكار الصباح), 31 items. */
    MORNING("morning",
            R.string.azkar_morning_title,
            R.string.azkar_morning_subtitle,
            R.drawable.ic_azkar_morning,
            AzkarRepository.SOURCE_MORNING,
            "islambook.com · أذكار الصباح"),

    /** Evening Azkar (أذكار المساء), 30 items. */
    EVENING("evening",
            R.string.azkar_evening_title,
            R.string.azkar_evening_subtitle,
            R.drawable.ic_azkar_evening,
            AzkarRepository.SOURCE_EVENING,
            "islambook.com · أذكار المساء"),

    /** Tasbeeh (تسابيح), 17 items. */
    TASBEEH("tasbeeh",
            R.string.azkar_tasbeeh_title,
            R.string.azkar_tasbeeh_subtitle,
            R.drawable.ic_azkar_tasbeeh,
            AzkarRepository.SOURCE_TASBEEH,
            "islamiokul.com · تسابيح");

    /** Key of the category array inside {@code assets/azkar.json}. */
    private final String key;
    private final int titleRes;
    private final int subtitleRes;
    private final int iconRes;
    private final String sourceUrl;
    private final String sourceLabel;

    AzkarCategory(String key,
                  @StringRes int titleRes,
                  @StringRes int subtitleRes,
                  int iconRes,
                  String sourceUrl,
                  String sourceLabel) {
        this.key = key;
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.iconRes = iconRes;
        this.sourceUrl = sourceUrl;
        this.sourceLabel = sourceLabel;
    }

    /** @return the JSON key, e.g. {@code "morning"} */
    @NonNull
    public String key() {
        return key;
    }

    /** @return the English section title, e.g. {@code R.string.azkar_morning_title} */
    @StringRes
    public int titleRes() {
        return titleRes;
    }

    /** @return the Arabic section subtitle, e.g. {@code R.string.azkar_morning_subtitle} */
    @StringRes
    public int subtitleRes() {
        return subtitleRes;
    }

    /** @return the section icon drawable */
    public int iconRes() {
        return iconRes;
    }

    /** @return the mandatory source URL this category was imported from */
    @NonNull
    public String sourceUrl() {
        return sourceUrl;
    }

    /** @return the short attribution line shown in the list footer */
    @NonNull
    public String sourceLabel() {
        return sourceLabel;
    }

    /** @return the category for {@code key}, or {@code null} for unknown keys */
    @Nullable
    public static AzkarCategory fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (AzkarCategory category : values()) {
            if (category.key.equals(key)) {
                return category;
            }
        }
        return null;
    }
}
