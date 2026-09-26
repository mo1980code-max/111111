package com.clockadventure.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.AppTheme
import com.clockadventure.domain.model.ClockStyle
import com.clockadventure.domain.model.DifficultyMode
import com.clockadventure.domain.model.MascotId
import com.clockadventure.domain.model.Unlockable
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val owned: List<Unlockable> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
    private val audio: AudioController
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeUnlockStates()
    ) { settings, unlocks ->
        SettingsUiState(
            settings = settings,
            owned = unlocks.filter { it.unlocked }.map { it.unlockable }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(block)
            audio.button()
        }
    }

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }
    fun setDifficulty(mode: DifficultyMode) = update { it.copy(difficultyMode = mode) }
    fun setMusic(enabled: Boolean) = update { it.copy(musicEnabled = enabled) }
    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setVoice(enabled: Boolean) = update { it.copy(voiceEnabled = enabled) }
    fun set24Hour(enabled: Boolean) = update { it.copy(use24Hour = enabled) }
    fun setNotifications(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }
    fun setHints(enabled: Boolean) = update { it.copy(hintsEnabled = enabled) }
    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }
    fun setClockStyle(style: ClockStyle) = update { it.copy(clockStyle = style) }
    fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }
    fun setCharacter(character: MascotId) = update { it.copy(character = character) }
}
