package com.clock.livewallpaper.reminder

import androidx.annotation.StringRes
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ui.navigation.Routes

/** The three time-of-day reminders, each with its own alarm slot, notification and destination. */
enum class DailyReminderKind(
    val key: String,
    val requestCode: Int,
    val notificationId: Int,
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
    val route: String,
    /** Friday only: the alarm must land on the next Friday, not simply tomorrow. */
    val weeklyOnFriday: Boolean
) {
    MORNING(
        key = "morning",
        requestCode = 2101,
        notificationId = 2201,
        titleRes = R.string.notification_morning_title,
        textRes = R.string.notification_morning_text,
        route = Routes.MORNING,
        weeklyOnFriday = false
    ),
    EVENING(
        key = "evening",
        requestCode = 2102,
        notificationId = 2202,
        titleRes = R.string.notification_evening_title,
        textRes = R.string.notification_evening_text,
        route = Routes.EVENING,
        weeklyOnFriday = false
    ),
    FRIDAY(
        key = "friday",
        requestCode = 2103,
        notificationId = 2203,
        titleRes = R.string.notification_friday_title,
        textRes = R.string.notification_friday_text,
        route = Routes.HOME,
        weeklyOnFriday = true
    );

    companion object {
        fun fromKey(key: String?): DailyReminderKind? = entries.firstOrNull { it.key == key }
    }
}
