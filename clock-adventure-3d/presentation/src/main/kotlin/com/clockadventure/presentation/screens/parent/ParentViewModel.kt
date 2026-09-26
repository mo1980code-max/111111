package com.clockadventure.presentation.screens.parent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.model.GateQuestion
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.QuestionKind
import com.clockadventure.domain.model.UserProgress
import com.clockadventure.domain.usecase.CreateParentGateQuestionUseCase
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class ParentUiState(
    val settings: AppSettings = AppSettings(),
    val progress: UserProgress = UserProgress(),
    val lessons: List<LessonProgress> = emptyList(),
    val daily: List<DailyStat> = emptyList(),
    val strongTopics: List<String> = emptyList(),
    val weakTopics: List<String> = emptyList(),
    val resetDone: Boolean = false
)

@HiltViewModel
class ParentGateViewModel @Inject constructor(
    private val createGateQuestion: CreateParentGateQuestionUseCase
) : ViewModel() {

    private val random = Random(System.currentTimeMillis())

    private val _question = MutableStateFlow(createGateQuestion(random))
    val question: StateFlow<GateQuestion> = _question

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed

    fun submit(value: Int, onSuccess: () -> Unit) {
        if (value == _question.value.answer) {
            onSuccess()
        } else {
            _failed.value = true
            _question.value = createGateQuestion(random)
        }
    }
}

@HiltViewModel
class ParentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val verified: Boolean = savedStateHandle.get<Boolean>("verified") ?: false

    private val reset = MutableStateFlow(false)

    val uiState: StateFlow<ParentUiState> = combine(
        settingsRepository.settings,
        progressRepository.observeProgress(),
        progressRepository.observeLessons(),
        progressRepository.observeDailyStats(7),
        reset
    ) { settings, progress, lessons, daily, resetDone ->
        ParentUiState(
            settings = settings,
            progress = progress,
            lessons = lessons,
            daily = daily,
            strongTopics = strongTopics(lessons),
            weakTopics = weakTopics(lessons),
            resetDone = resetDone
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ParentUiState())

    val isVerified: Boolean get() = verified || uiState.value.progress.parentVerified

    /** Flips the optional daily reminder. The alarm itself is (re)scheduled by MainActivity. */
    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun setDailyLimit(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(dailyLimitMinutes = minutes) }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            progressRepository.resetProgress()
            reset.value = true
        }
    }

    companion object {
        /** Levels with the best accuracy, named by their level title. */
        private fun strongTopics(lessons: List<LessonProgress>): List<String> = lessons
            .filter { it.answers >= 4 && it.accuracy >= 0.8f }
            .sortedByDescending { it.accuracy }
            .take(3)
            .map { LevelCatalog.byId(it.levelId).title.en }

        /** Levels below 60 % accuracy - these deserve another try. */
        private fun weakTopics(lessons: List<LessonProgress>): List<String> = lessons
            .filter { it.answers >= 3 && it.accuracy < 0.6f }
            .sortedBy { it.accuracy }
            .take(3)
            .map { LevelCatalog.byId(it.levelId).title.en }

        val ROUTINE_KINDS: List<QuestionKind> = listOf(
            QuestionKind.ROUTINE_CHOICE,
            QuestionKind.ROUTINE_SET_CLOCK
        )
    }
}
