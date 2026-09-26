package com.clock.livewallpaper.data.prefs

import androidx.annotation.StringRes
import com.clock.livewallpaper.R

/** "فاتح" / "داكن" / "حسب النظام". */
enum class ThemeMode(val key: String, @StringRes val labelRes: Int) {
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark),
    SYSTEM("system", R.string.theme_system);

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** The four floating-card palettes. */
enum class OverlayStyleOption(val key: String, @StringRes val labelRes: Int) {
    NOOR("noor", R.string.overlay_style_noor),
    EMERALD("emerald", R.string.overlay_style_emerald),
    NIGHT("night", R.string.overlay_style_night),
    CLEAR("clear", R.string.overlay_style_clear);

    companion object {
        fun fromKey(key: String?): OverlayStyleOption = entries.firstOrNull { it.key == key } ?: NOOR
    }
}

/** Where the floating card sits inside the safe area. */
enum class OverlayPositionOption(val key: String, @StringRes val labelRes: Int) {
    TOP("top", R.string.overlay_position_top),
    CENTER("center", R.string.overlay_position_center),
    BOTTOM("bottom", R.string.overlay_position_bottom);

    companion object {
        fun fromKey(key: String?): OverlayPositionOption =
            entries.firstOrNull { it.key == key } ?: TOP
    }
}

data class OverlaySettings(
    val enabled: Boolean = false,
    val style: OverlayStyleOption = OverlayStyleOption.NOOR,
    val position: OverlayPositionOption = OverlayPositionOption.TOP,
    val fontScale: Float = 1f,
    val opacity: Float = 1f,
    val autoDismissSeconds: Int = 10,
    val haptic: Boolean = true
) {
    companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.35f
        const val MIN_OPACITY = 0.6f
        const val MAX_OPACITY = 1f

        /** 0 means "عند اللمس فقط" - the card waits for a touch. */
        val AUTO_DISMISS_OPTIONS = listOf(0, 5, 10, 15, 30)
    }
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 30
) {
    companion object {
        val PRESET_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 120, 180)

        /** Quick chips on the home reminder card. */
        val QUICK_MINUTES = listOf(5, 10, 15, 30, 60)

        const val MIN_MINUTES = 5
        const val MAX_MINUTES = 720

        fun sanitizeInterval(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
    }
}

data class QuietHoursSettings(
    val enabled: Boolean = false,
    val startMinute: Int = 23 * 60,
    val endMinute: Int = 6 * 60
) {
    /** Handles windows that cross midnight (23:00 -> 06:00). */
    fun isQuietAt(minuteOfDay: Int): Boolean {
        if (!enabled) return false
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            minuteOfDay >= startMinute && minuteOfDay < endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }
}

data class DailyReminderSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int = 0
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val readerFontScale: Float = 1f,
    val showHijri: Boolean = true
) {
    companion object {
        const val MIN_READER_SCALE = 0.9f
        const val MAX_READER_SCALE = 1.4f
    }
}

data class TasbeehSettings(
    /** 0 means the custom text below is in use. */
    val presetDhikrId: Long = 0L,
    val customText: String = "",
    val count: Int = 0,
    val target: Int = 33,
    val customTarget: Int = 100,
    val dailyTotal: Int = 0,
    val haptic: Boolean = true
) {
    companion object {
        val TARGETS = listOf(33, 100)
        const val MIN_TARGET = 1
        const val MAX_TARGET = 10_000

        fun sanitizeTarget(target: Int): Int = target.coerceIn(MIN_TARGET, MAX_TARGET)
    }
}

/** One consistent read of everything the alarm receivers need, taken off the UI thread. */
data class SettingsSnapshot(
    val reminder: ReminderSettings,
    val quietHours: QuietHoursSettings,
    val overlay: OverlaySettings,
    val morning: DailyReminderSettings,
    val evening: DailyReminderSettings,
    val friday: DailyReminderSettings
)
