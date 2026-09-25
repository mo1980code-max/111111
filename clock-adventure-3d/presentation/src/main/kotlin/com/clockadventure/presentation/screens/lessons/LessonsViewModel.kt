package com.clockadventure.presentation.screens.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.model.AppSettings
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the lessons list. */
data class LessonCardState(
    val levelId: Int,
    val bestStars: Int = 0,
    val completed: Boolean = false,
    val unlocked: Boolean = false,
    val playSeconds: Int = 0
)

data class LessonsUiState(
    val settings: AppSettings = AppSettings(),
    val progress: UserProgress = UserProgress(),
    val cards: List<LessonCardState> = LevelCatalog.levels.map {
        LessonCardState(levelId = it.id, unlocked = it.id == 1)
    }
) {
    val finishedCount: Int get() = cards.count { it.completed }
}

@HiltViewModel
class LessonsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val audio: AudioController
) : ViewModel() {

    val uiState: StateFlow<LessonsUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
        progressRepository.observeLessons()
    ) { settings, progress, lessons ->
        val byId = lessons.associateBy { it.levelId }
        val unlockedThrough = LevelCatalog.unlockedThrough(lessons)
        LessonsUiState(
            settings = settings,
            progress = progress,
            cards = LevelCatalog.levels.map { spec ->
                val record = byId[spec.id]
                LessonCardState(
                    levelId = spec.id,
                    bestStars = record?.bestStars ?: 0,
                    completed = record?.isCompleted ?: false,
                    unlocked = spec.id <= unlockedThrough,
                    playSeconds = record?.playSeconds ?: 0
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonsUiState())

    fun onSelectLevel(levelId: Int) {
        viewModelScope.launch { progressRepository.setCurrentLevel(levelId) }
        audio.button()
    }
}
