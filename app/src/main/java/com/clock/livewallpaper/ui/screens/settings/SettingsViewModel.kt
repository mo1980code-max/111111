package com.clock.livewallpaper.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.prefs.AppearanceSettings
import com.clock.livewallpaper.data.prefs.DailyReminderSettings
import com.clock.livewallpaper.data.prefs.OverlaySettings
import com.clock.livewallpaper.data.prefs.QuietHoursSettings
import com.clock.livewallpaper.data.prefs.ReminderSettings
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.data.prefs.TasbeehSettings
import com.clock.livewallpaper.data.prefs.ThemeMode
import com.clock.livewallpaper.reminder.DailyReminderKind
import com.clock.livewallpaper.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Grouped so the whole screen still fits one `combine`. */
private data class ScheduleBundle(
    val reminder: ReminderSettings,
    val quietHours: QuietHoursSettings,
    val morning: DailyReminderSettings,
    val evening: DailyReminderSettings,
    val friday: DailyReminderSettings
)

data class SettingsUiState(
    val loading: Boolean = true,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val overlay: OverlaySettings = OverlaySettings(),
    val reminder: ReminderSettings = ReminderSettings(),
    val quietHours: QuietHoursSettings = QuietHoursSettings(),
    val morning: DailyReminderSettings = DailyReminderSettings(),
    val evening: DailyReminderSettings = DailyReminderSettings(),
    val friday: DailyReminderSettings = DailyReminderSettings(),
    val tasbeeh: TasbeehSettings = TasbeehSettings()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        combine(
            settings.reminder,
            settings.quietHours,
            settings.morningReminder,
            settings.eveningReminder,
            settings.fridayReminder
        ) { reminder, quiet, morning, evening, friday ->
            ScheduleBundle(reminder, quiet, morning, evening, friday)
        },
        settings.appearance,
        settings.overlay,
        settings.tasbeeh
    ) { schedules, appearance, overlay, tasbeeh ->
        SettingsUiState(
            loading = false,
            appearance = appearance,
            overlay = overlay,
            reminder = schedules.reminder,
            quietHours = schedules.quietHours,
            morning = schedules.morning,
            evening = schedules.evening,
            friday = schedules.friday,
            tasbeeh = tasbeeh
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    // ------------------------------------------------------------- appearance

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setReaderFontScale(scale: Float) {
        viewModelScope.launch { settings.setReaderFontScale(scale) }
    }

    fun setShowHijri(show: Boolean) {
        viewModelScope.launch { settings.setShowHijri(show) }
    }

    fun setTasbeehHaptic(enabled: Boolean) {
        viewModelScope.launch { settings.setTasbeehHaptic(enabled) }
    }

    // -------------------------------------------------------------- reminders

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setOverlayEnabled(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            scheduler.apply(settings.snapshot())
        }
    }

    fun setReminderInterval(minutes: Int) {
        viewModelScope.launch {
            settings.setReminderInterval(minutes)
            scheduler.apply(settings.snapshot())
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setQuietHoursEnabled(enabled) }
    }

    fun setQuietHours(startMinute: Int, endMinute: Int) {
        viewModelScope.launch { settings.setQuietHours(startMinute, endMinute) }
    }

    fun setDailyReminder(kind: DailyReminderKind, enabled: Boolean, minuteOfDay: Int) {
        viewModelScope.launch {
            when (kind) {
                DailyReminderKind.MORNING -> settings.setMorningReminder(enabled, minuteOfDay)
                DailyReminderKind.EVENING -> settings.setEveningReminder(enabled, minuteOfDay)
                DailyReminderKind.FRIDAY -> settings.setFridayReminder(enabled, minuteOfDay)
            }
            scheduler.applyDaily(kind, enabled, minuteOfDay)
        }
    }
}
