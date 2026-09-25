package com.clockadventure.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.UserProgress
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import com.clockadventure.presentation.util.DateText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val progress: UserProgress = UserProgress(),
    val lessons: List<LessonProgress> = emptyList(),
    val today: DailyStat = DailyStat(""),
    val dailyClaimed: Boolean = false,
    val lastRewardCoins: Int = 0
) {
    /** Next unfinished lesson, or the last one when everything is done. */
    fun nextLevelId(): Int {
        val next = lessons.firstOrNull { !it.isCompleted }?.levelId
        return next ?: (progress.currentLevelId.coerceIn(1, 10))
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val audio: AudioController
) : ViewModel() {

    private val reward = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
        progressRepository.observeLessons(),
        progressRepository.observeTodayStat(),
        reward
    ) { settings, progress, lessons, today, rewardCoins ->
        HomeUiState(
            settings = settings,
            progress = progress,
            lessons = lessons,
            today = today,
            dailyClaimed = DateText.isToday(progress.lastDailyRewardDateKey),
            lastRewardCoins = rewardCoins
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun claimDailyReward() {
        viewModelScope.launch {
            val result = progressRepository.claimDailyReward()
            if (result != null) {
                audio.coin()
                reward.value = result.coins
            }
        }
    }

    fun onButtonSound() = audio.button()
}
