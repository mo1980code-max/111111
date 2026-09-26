package com.clock.livewallpaper.data.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.clock.livewallpaper.R

/**
 * The only categories the product exposes are the ones backed by real local content:
 * the three sections of assets/azkar.json plus the user's own dhikr. No empty category is
 * invented to fill the grid.
 */
enum class DhikrCategory(
    val key: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    MORNING("morning", R.string.category_morning, R.drawable.ic_sun),
    EVENING("evening", R.string.category_evening, R.drawable.ic_moon),
    TASBEEH("tasbeeh", R.string.category_tasbeeh, R.drawable.ic_nav_tasbeeh),
    CUSTOM("custom", R.string.category_custom, R.drawable.ic_nav_my_dhikr);

    companion object {
        /** Categories offered as a guided reading list. */
        val READING: List<DhikrCategory> = listOf(MORNING, EVENING, TASBEEH, CUSTOM)

        /** Categories a user may choose for their own dhikr. */
        val ASSIGNABLE: List<DhikrCategory> = listOf(CUSTOM, MORNING, EVENING, TASBEEH)

        fun fromKey(key: String?): DhikrCategory? = entries.firstOrNull { it.key == key }
    }
}
