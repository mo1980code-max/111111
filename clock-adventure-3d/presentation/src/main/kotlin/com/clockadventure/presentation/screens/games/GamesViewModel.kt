package com.clockadventure.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.catalog.GameCatalog
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.GameScore
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GameCardState(
    val id: com.clockadventure.domain.catalog.GameId,
    val bestScore: Int = 0,
    val plays: Int = 0
)

data class GamesUiState(
    val settings: AppSettings = AppSettings(),
    val scores: Map<String, GameCardState> = emptyMap()
)

@HiltViewModel
class GamesViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val audio: AudioController
) : ViewModel() {

    val uiState: StateFlow<GamesUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeGameScores()
    ) { settings, scores: List<GameScore> ->
        GamesUiState(
            settings = settings,
            scores = scores.associate { score ->
                score.gameId to GameCardState(
                    id = runCatching {
                        com.clockadventure.domain.catalog.GameId.valueOf(score.gameId)
                    }.getOrDefault(GameCatalog.games.first().id),
                    bestScore = score.bestScore,
                    plays = score.plays
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())

    fun onButtonSound() = audio.button()
}
