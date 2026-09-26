package com.clock.livewallpaper.ui.screens.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.prefs.OverlayPositionOption
import com.clock.livewallpaper.data.prefs.OverlaySettings
import com.clock.livewallpaper.data.prefs.OverlayStyleOption
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.reminder.ReminderPresenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OverlaySettingsUiState(
    val loading: Boolean = true,
    val overlay: OverlaySettings = OverlaySettings(),
    /** A real dhikr from the local database, so the preview shows real typography. */
    val previewText: String = ""
)

@HiltViewModel
class OverlaySettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: DhikrRepository,
    private val presenter: ReminderPresenter
) : ViewModel() {

    private val previewText = MutableStateFlow("")

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureSeeded()
                val sample = repository.dailyDhikr() ?: repository.pickForReminder()
                if (sample != null) previewText.value = repository.shortText(sample)
            }
        }
    }

    val state: StateFlow<OverlaySettingsUiState> =
        combine(settings.overlay, previewText) { overlay, text ->
            OverlaySettingsUiState(loading = false, overlay = overlay, previewText = text)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverlaySettingsUiState()
        )

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setOverlayEnabled(enabled) }
    }

    fun setStyle(style: OverlayStyleOption) {
        viewModelScope.launch { settings.setOverlayStyle(style) }
    }

    fun setPosition(position: OverlayPositionOption) {
        viewModelScope.launch { settings.setOverlayPosition(position) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setOverlayFontScale(scale) }
    }

    fun setOpacity(opacity: Float) {
        viewModelScope.launch { settings.setOverlayOpacity(opacity) }
    }

    fun setAutoDismiss(seconds: Int) {
        viewModelScope.launch { settings.setOverlayAutoDismiss(seconds) }
    }

    fun setHaptic(enabled: Boolean) {
        viewModelScope.launch { settings.setOverlayHaptic(enabled) }
    }

    /** Shows the real floating card over the current screen; false means it could not be shown. */
    fun showTestCard(onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(presenter.previewOverlay()) }
    }
}
