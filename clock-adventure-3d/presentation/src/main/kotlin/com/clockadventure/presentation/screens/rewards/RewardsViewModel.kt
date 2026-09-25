package com.clockadventure.presentation.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.AppTheme
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.ClockStyle
import com.clockadventure.domain.model.MascotId
import com.clockadventure.domain.model.UnlockState
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

data class RewardsUiState(
    val settings: AppSettings = AppSettings(),
    val progress: UserProgress = UserProgress(),
    val achievements: List<AchievementState> = emptyList(),
    val unlocks: List<UnlockState> = emptyList(),
    val dailyClaimed: Boolean = false,
    val justClaimedCoins: Int = 0
) {
    val unlockedAchievements: Int get() = achievements.count { it.unlocked }
}

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val audio: AudioController
) : ViewModel() {

    private val claim = MutableStateFlow(0)

    val uiState: StateFlow<RewardsUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
        progressRepository.observeAchievements(),
        progressRepository.observeUnlockStates(),
        claim
    ) { settings, progress, achievements, unlocks, claimCoins ->
        RewardsUiState(
            settings = settings,
            progress = progress,
            achievements = achievements,
            unlocks = unlocks,
            dailyClaimed = DateText.isToday(progress.lastDailyRewardDateKey),
            justClaimedCoins = claimCoins
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsUiState())

    fun claimDailyReward() {
        viewModelScope.launch {
            val result = progressRepository.claimDailyReward()
            if (result != null) {
                audio.coin()
                claim.value = result.coins
            }
        }
    }

    fun buy(unlockId: String) {
        viewModelScope.launch {
            val item = uiState.value.unlocks.firstOrNull { it.unlockable.id == unlockId }?.unlockable ?: return@launch
            val bought = progressRepository.purchase(item)
            if (bought) {
                audio.coin()
                equip(item)
            } else {
                audio.wrong()
            }
        }
    }

    /** Equipping an owned look writes it straight into the settings, so it survives a restart. */
    fun equip(item: com.clockadventure.domain.model.Unlockable) {
        viewModelScope.launch {
            val alreadyOwned = uiState.value.unlocks.firstOrNull { it.unlockable.id == item.id }?.unlocked == true
            if (!alreadyOwned) return@launch
            settingsRepository.update { settings ->
                when (item.type) {
                    com.clockadventure.domain.model.UnlockableType.CLOCK_STYLE -> settings.copy(
                        clockStyle = runCatching { ClockStyle.valueOf(item.refId) }.getOrDefault(settings.clockStyle)
                    )
                    com.clockadventure.domain.model.UnlockableType.THEME -> settings.copy(
                        theme = runCatching { AppTheme.valueOf(item.refId) }.getOrDefault(settings.theme)
                    )
                    com.clockadventure.domain.model.UnlockableType.CHARACTER -> settings.copy(
                        character = runCatching { MascotId.valueOf(item.refId) }.getOrDefault(settings.character)
                    )
                }
            }
            audio.button()
        }
    }
}
