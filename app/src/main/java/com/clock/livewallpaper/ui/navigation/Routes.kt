package com.clock.livewallpaper.ui.navigation

import com.clock.livewallpaper.data.local.DhikrCategory

/**
 * Every navigation destination in one place.
 *
 * Notifications and widgets send a route string through the launch Intent; [isKnown] is the guard
 * that keeps an unexpected extra from throwing inside NavController.
 */
object Routes {

    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADHKAR = "adhkar"
    const val TASBEEH = "tasbeeh"
    const val MY_DHIKR = "my_dhikr"
    const val SETTINGS = "settings"
    const val OVERLAY_SETTINGS = "overlay_settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    const val ARG_CATEGORY = "category"
    const val ARG_DHIKR_ID = "dhikrId"

    const val READING_PATTERN = "reading/{$ARG_CATEGORY}"
    const val EDITOR_PATTERN = "dhikr_editor?$ARG_DHIKR_ID={$ARG_DHIKR_ID}"

    const val NEW_DHIKR_ID = -1L

    fun reading(category: DhikrCategory): String = "reading/${category.key}"

    fun editor(dhikrId: Long = NEW_DHIKR_ID): String = "dhikr_editor?$ARG_DHIKR_ID=$dhikrId"

    val MORNING: String = reading(DhikrCategory.MORNING)
    val EVENING: String = reading(DhikrCategory.EVENING)

    /** Destinations that own a bottom-navigation tab. */
    val BOTTOM_ROUTES: List<String> = listOf(HOME, ADHKAR, TASBEEH, MY_DHIKR)

    /** Routes an external component (notification / widget) is allowed to open. */
    private val EXTERNAL_ROUTES: Set<String> = buildSet {
        add(HOME)
        add(ADHKAR)
        add(TASBEEH)
        add(MY_DHIKR)
        add(SETTINGS)
        add(OVERLAY_SETTINGS)
        DhikrCategory.READING.forEach { add(reading(it)) }
    }

    fun isKnown(route: String?): Boolean = route != null && EXTERNAL_ROUTES.contains(route)
}
