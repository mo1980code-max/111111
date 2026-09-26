package com.clock.livewallpaper.ui.screens.tasbeeh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.data.local.DhikrEntity
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.data.prefs.TasbeehSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasbeehUiState(
    val loading: Boolean = true,
    val count: Int = 0,
    val target: Int = 33,
    val customTarget: Int = 100,
    val dailyTotal: Int = 0,
    val haptic: Boolean = true,
    val presetId: Long = 0L,
    val customText: String = "",
    val presets: List<DhikrEntity> = emptyList()
) {
    /** What the ring shows: the chosen preset, the user's own text, or the first preset. */
    val label: String
        get() = presets.firstOrNull { it.id == presetId }?.arabicText
            ?: customText.takeIf { it.isNotBlank() }
            ?: presets.firstOrNull()?.arabicText
            ?: ""

    val isCustomTarget: Boolean get() = !TasbeehSettings.TARGETS.contains(target)
}

@HiltViewModel
class TasbeehViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: DhikrRepository
) : ViewModel() {

    /** Emitted when a round reaches its target, so the screen can congratulate once. */
    private val _roundCompleted = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val roundCompleted: SharedFlow<Unit> = _roundCompleted

    init {
        viewModelScope.launch { runCatching { repository.ensureSeeded() } }
    }

    val state: StateFlow<TasbeehUiState> = combine(
        settings.tasbeeh,
        repository.observeCategory(DhikrCategory.TASBEEH)
    ) { tasbeeh, presets ->
        TasbeehUiState(
            loading = false,
            count = tasbeeh.count,
            target = tasbeeh.target,
            customTarget = tasbeeh.customTarget,
            dailyTotal = tasbeeh.dailyTotal,
            haptic = tasbeeh.haptic,
            presetId = tasbeeh.presetDhikrId,
            customText = tasbeeh.customText,
            presets = presets.filter { it.isEnabled }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasbeehUiState()
    )

    fun increment() {
        viewModelScope.launch {
            val updated = settings.incrementTasbeeh()
            if (updated.count >= updated.target) _roundCompleted.tryEmit(Unit)
        }
    }

    fun resetRound() {
        viewModelScope.launch { settings.resetTasbeehCounter() }
    }

    fun setTarget(target: Int) {
        viewModelScope.launch { settings.setTasbeehTarget(target) }
    }

    fun selectPreset(id: Long) {
        viewModelScope.launch { settings.setTasbeehSelection(id, "") }
    }

    /** Custom text clears the preset selection; the counter restarts for the new dhikr. */
    fun setCustomText(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch { settings.setTasbeehSelection(0L, cleaned) }
    }

    fun setHaptic(enabled: Boolean) {
        viewModelScope.launch { settings.setTasbeehHaptic(enabled) }
    }
}
