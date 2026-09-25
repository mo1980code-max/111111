package com.clockadventure.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.UserProgress
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProgressUiState(
    val settings: AppSettings = AppSettings(),
    val progress: UserProgress = UserProgress(),
    val lessons: List<LessonProgress> = emptyList(),
    val daily: List<DailyStat> = emptyList()
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val audio: AudioController
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
        progressRepository.observeLessons(),
        progressRepository.observeDailyStats(7)
    ) { settings, progress, lessons, daily ->
        ProgressUiState(settings, progress, lessons, daily)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun onButtonSound() = audio.button()
}
