package com.clock.livewallpaper.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.core.DayPart
import com.clock.livewallpaper.core.currentDayPart
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.prefs.ReminderSettings
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val dayPart: DayPart = DayPart.MORNING,
    val gregorianDate: String = "",
    val hijriDate: String? = null,
    val reminderEnabled: Boolean = false,
    val intervalMinutes: Int = ReminderSettings().intervalMinutes,
    val dailyDhikrText: String = "",
    val overlayEnabled: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val repository: DhikrRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    /** Bumped on resume and after midnight-sensitive changes, so the date and dhikr stay current. */
    private val refreshKey = MutableStateFlow(0L)

    private val dailyDhikr: Flow<String> = refreshKey
        .map {
            repository.ensureSeeded()
            repository.dailyDhikr()?.arabicText.orEmpty()
        }
        .flowOn(Dispatchers.IO)

    val state: StateFlow<HomeUiState> = combine(
        settings.reminder,
        settings.appearance,
        settings.overlay,
        dailyDhikr,
        refreshKey
    ) { reminder, appearance, overlay, dhikrText, _ ->
        HomeUiState(
            loading = false,
            dayPart = currentDayPart(),
            gregorianDate = ArabicText.gregorianDate(),
            hijriDate = if (appearance.showHijri) ArabicText.hijriDate(context) else null,
            reminderEnabled = reminder.enabled,
            intervalMinutes = reminder.intervalMinutes,
            dailyDhikrText = dhikrText,
            overlayEnabled = overlay.enabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun refresh() {
        refreshKey.value = System.currentTimeMillis()
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            scheduler.apply(settings.snapshot())
        }
    }

    /** Choosing an interval also switches the reminder on - that is what the tap means. */
    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            settings.setReminderInterval(minutes)
            settings.setReminderEnabled(true)
            scheduler.apply(settings.snapshot())
        }
    }
}
