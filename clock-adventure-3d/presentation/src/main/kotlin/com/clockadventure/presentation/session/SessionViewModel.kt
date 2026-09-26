package com.clockadventure.presentation.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.catalog.GameId
import com.clockadventure.domain.engine.ClockMath
import com.clockadventure.domain.engine.DifficultyEngine
import com.clockadventure.domain.engine.RewardEngine
import com.clockadventure.domain.engine.TimeFormatter
import com.clockadventure.domain.model.AnswerResult
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.DifficultyMode
import com.clockadventure.domain.model.LessonCompletionResult
import com.clockadventure.domain.model.GateQuestion
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.QuestionKind
import com.clockadventure.domain.model.SessionResult
import com.clockadventure.domain.repository.AnswerEvent
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import com.clockadventure.domain.usecase.BuildSessionResultUseCase
import com.clockadventure.domain.usecase.EvaluateAnswerUseCase
import com.clockadventure.domain.usecase.CreateParentGateQuestionUseCase
import com.clockadventure.domain.usecase.GenerateQuestionUseCase
import com.clockadventure.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.collect

/** What the session screen is currently showing. */
enum class SessionPhase { TEACH, QUESTION, RESULT }

enum class FeedbackKind { CORRECT, WRONG, TIME_UP }

/** State of the Match The Time mini game. */
data class MatchState(
    val clocks: List<com.clockadventure.domain.engine.MatchPair> = emptyList(),
    val times: List<com.clockadventure.domain.engine.MatchPair> = emptyList(),
    val selectedClock: String? = null,
    val matched: Set<String> = emptySet(),
    val attempts: Int = 0,
    val wrongFlash: Long = 0L
) {
    val matchedCount: Int get() = matched.size
    val totalCount: Int get() = clocks.size
    val isFinished: Boolean get() = clocks.isNotEmpty() && matched.size == clocks.size
}

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.QUESTION,
    val title: LocalizedText = LocalizedText("", ""),
    val teach: LocalizedText? = null,
    val question: Question? = null,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val hintsUsed: Int = 0,
    val difficulty: Difficulty = Difficulty.EASY,
    val hintsOn: Boolean = false,
    val feedback: AnswerResult? = null,
    val feedbackKind: FeedbackKind? = null,
    val revealAnswer: Boolean = false,
    val retrying: Boolean = false,
    val selectedChoiceId: String? = null,
    val clockTime: ClockTime = ClockTime.of12(12, 0, pm = false),
    val snapMinutes: Int = 5,
    val remainingMs: Long? = null,
    val result: SessionResult? = null,
    val matchState: MatchState? = null,
    val dailyLimitReached: Boolean = false,
    val settings: AppSettings = AppSettings(),
    /** The daily limit was reached: the session pauses behind a kind break dialog. */
    val breakTime: Boolean = false,
    /** Set while the "ask a grown-up" gate is on screen. */
    val gateQuestion: GateQuestion? = null,
    /** Minutes a grown-up granted on top of the daily limit. */
    val grantedExtraMinutes: Int = 0,
    val isLesson: Boolean = false,
    val levelId: Int = 0,
    val gameId: GameId? = null,
    val nextLevelId: Int? = null
) {
    val progressFraction: Float
        get() = if (totalQuestions <= 0) 0f else questionNumber.toFloat() / totalQuestions.toFloat()

    val isEndless: Boolean get() = totalQuestions <= 0 && matchState == null
}

/**
 * Drives every exercise in the app: the ten lessons, the six mini games and the four challenges.
 *
 * Rules that matter for a children's app:
 * * a wrong answer is never punished - the child retries, gets a hint, or asks to see the answer;
 * * the difficulty adapts after every answer (three right in a row is harder, two wrong is easier);
 * * everything is recorded, so the parent dashboard and the reward screen stay truthful.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val generateQuestion: GenerateQuestionUseCase,
    private val evaluateAnswer: EvaluateAnswerUseCase,
    private val buildSessionResult: BuildSessionResultUseCase,
    private val createGateQuestion: CreateParentGateQuestionUseCase,
    private val audio: AudioController
) : ViewModel() {

    private val levelId: Int = savedStateHandle.get<Int>("levelId") ?: -1
    private val gameIdRaw: String? = savedStateHandle.get<String>("gameId")
    private val challengeId: String? = savedStateHandle.get<String>("challengeId")

    private val spec: SessionSpec = SessionSpec.from(levelId, gameIdRaw, challengeId)
    private val random = Random(System.currentTimeMillis())

    private lateinit var engine: DifficultyEngine
    private var settings: AppSettings = AppSettings()
    private var sessionStartMs = System.currentTimeMillis()
    private var questionStartMs = System.currentTimeMillis()
    private var answeredCurrentQuestion = false
    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var limitJob: Job? = null
    private var todaySeconds = 0
    private var grantedExtraMinutes = 0
    /** When the break dialog appeared, so the paused time is not counted as play time. */
    private var pausedAtMs = 0L

    private val _uiState = MutableStateFlow(
        SessionUiState(
            title = spec.title,
            teach = spec.teach,
            totalQuestions = if (spec.endless) 0 else spec.questionCount,
            isLesson = spec.isLesson,
            levelId = spec.levelId,
            gameId = spec.gameId
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                settings = newSettings
                audio.applySettings(newSettings.musicEnabled, newSettings.soundEnabled, newSettings.voiceEnabled)
                _uiState.update { it.copy(settings = newSettings) }
            }
        }
        viewModelScope.launch {
            progressRepository.observeTodayStat().collect { stat -> todaySeconds = stat.secondsLearned }
        }
        sessionStartMs = System.currentTimeMillis()
        startLimitWatcher()
        engine = DifficultyEngine(mode = DifficultyMode.AUTO, start = spec.baseDifficulty)

        if (spec.isMatchGame) {
            startMatchRound()
        } else if (spec.teach != null && spec.isLesson) {
            _uiState.update {
                it.copy(
                    phase = SessionPhase.TEACH,
                    difficulty = spec.baseDifficulty,
                    snapMinutes = spec.minuteStepFor(spec.baseDifficulty)
                )
            }
        } else {
            nextQuestion(first = true)
        }
    }

    // ------------------------------------------------------------- session flow

    fun startQuestions() {
        if (_uiState.value.phase == SessionPhase.TEACH) {
            audio.button()
            nextQuestion(first = true)
        }
    }

    private fun nextQuestion(first: Boolean) {
        _uiState.update { state ->
            if (!first && !spec.endless && state.questionNumber >= spec.questionCount) {
                state
            } else {
                state
            }
        }
        if (!first && !spec.endless && _uiState.value.questionNumber >= spec.questionCount) {
            finishSession()
            return
        }

        val difficulty = engine.current()
        val index = _uiState.value.questionNumber
        val question = when (spec) {
            is SessionSpec.Lesson -> generateQuestion.forLesson(spec.spec, difficulty, index, random)
            is SessionSpec.Game -> generateQuestion.forGame(spec.spec, difficulty, index, random)
            is SessionSpec.Challenge -> generateQuestion.forLesson(
                kind = spec.kinds[index % spec.kinds.size],
                spec = challengeLessonSpec(),
                difficulty = difficulty,
                index = index,
                random = random
            )
        }

        answeredCurrentQuestion = false
        questionStartMs = System.currentTimeMillis()

        _uiState.update { state ->
            state.copy(
                phase = SessionPhase.QUESTION,
                question = question,
                questionNumber = if (first) 1 else state.questionNumber + 1,
                difficulty = difficulty,
                hintsOn = engine.hintsOn && settings.hintsEnabled,
                snapMinutes = spec.minuteStepFor(difficulty),
                feedback = null,
                feedbackKind = null,
                revealAnswer = false,
                retrying = false,
                selectedChoiceId = null,
                clockTime = ClockTime.of12(12, 0, pm = false)
            )
        }

        if (settings.voiceEnabled) {
            when (question.kind) {
                QuestionKind.SET_CLOCK, QuestionKind.ROUTINE_SET_CLOCK ->
                    audio.speakTime(question.targetTime, settings.language)
                else -> Unit
            }
        }
        startTimers()
    }

    /** Challenges reuse the lesson generator through a temporary lesson like spec. */
    private fun challengeLessonSpec(): com.clockadventure.domain.catalog.LessonSpec {
        val challenge = (spec as SessionSpec.Challenge).spec
        return com.clockadventure.domain.catalog.LessonSpec(
            id = 0,
            title = challenge.title,
            subtitle = LocalizedText("", ""),
            teach = LocalizedText("", ""),
            kinds = challenge.kinds,
            questionCount = challenge.questionCount,
            stepEasy = challenge.stepEasy,
            stepMedium = challenge.stepMedium,
            stepHard = challenge.stepHard,
            timeLimitMs = challenge.perQuestionLimitMs,
            baseDifficulty = challenge.baseDifficulty
        )
    }

    private fun startTimers() {
        timerJob?.cancel()
        val totalLimit = spec.totalTimeLimitMs
        val perQuestion = spec.perQuestionLimitMs
        if (totalLimit == null && perQuestion == null) return

        val deadline = System.currentTimeMillis() + (perQuestion ?: totalLimit ?: 0L)
        val totalDeadline = if (totalLimit != null) sessionStartMs + totalLimit else null

        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remainingForQuestion = (deadline - now).coerceAtLeast(0L)
                val remainingTotal = totalDeadline?.let { (it - now).coerceAtLeast(0L) }
                _uiState.update { it.copy(remainingMs = remainingTotal ?: remainingForQuestion) }
                if (remainingForQuestion <= 0L || (remainingTotal != null && remainingTotal <= 0L)) {
                    onTimeUp()
                    break
                }
                if (totalDeadline == null && remainingForQuestion <= 0L) break
                delay(200L)
            }
        }
    }

    private fun onTimeUp() {
        if (_uiState.value.phase != SessionPhase.QUESTION) return
        if (spec.endless && spec.totalTimeLimitMs != null) {
            finishSession()
            return
        }
        val question = _uiState.value.question ?: return
        val result = AnswerResult(
            correct = false,
            expected = question.targetTime,
            given = null,
            hint = null,
            message = LocalizedText("Time is up - let's look at it together.", "انتهى الوقت - هيا ننظر معاً.")
        )
        registerAnswer(result, kind = FeedbackKind.TIME_UP)
    }

    // ------------------------------------------------------------- answering

    fun onChoiceSelected(choiceId: String) {
        val question = _uiState.value.question ?: return
        if (_uiState.value.feedbackKind == FeedbackKind.CORRECT) return
        audio.button()
        val result = evaluateAnswer(question, choiceId = choiceId, random = random)
        _uiState.update { it.copy(selectedChoiceId = choiceId) }
        registerAnswer(result)
    }

    fun onClockChanged(time: ClockTime) {
        val state = _uiState.value
        if (state.feedbackKind == FeedbackKind.CORRECT) return
        _uiState.update { it.copy(clockTime = time) }
    }

    fun onCheckClock() {
        val question = _uiState.value.question ?: return
        if (_uiState.value.feedbackKind == FeedbackKind.CORRECT) return
        audio.button()
        val result = evaluateAnswer(question, setTime = _uiState.value.clockTime, random = random)
        registerAnswer(result)
    }

    fun onShowAnswer() {
        audio.button()
        _uiState.update { it.copy(revealAnswer = true, hintsUsed = it.hintsUsed + 1) }
    }

    fun onRetry() {
        audio.button()
        _uiState.update {
            it.copy(
                feedback = null,
                feedbackKind = null,
                selectedChoiceId = null,
                retrying = true,
                clockTime = ClockTime.of12(12, 0, pm = false)
            )
        }
    }

    fun onContinue() {
        audio.button()
        val state = _uiState.value
        when {
            state.feedbackKind == FeedbackKind.CORRECT -> nextQuestion(first = false)
            state.revealAnswer -> nextQuestion(first = false)
            else -> Unit
        }
    }

    private fun registerAnswer(result: AnswerResult, kind: FeedbackKind? = null) {
        val question = _uiState.value.question ?: return
        val feedbackKind = kind ?: if (result.correct) FeedbackKind.CORRECT else FeedbackKind.WRONG
        val durationMs = System.currentTimeMillis() - questionStartMs
        val firstAttempt = !answeredCurrentQuestion

        if (feedbackKind == FeedbackKind.CORRECT) {
            audio.correct()
            if (settings.voiceEnabled) {
                audio.speakTime(question.targetTime, settings.language)
            }
        } else {
            audio.wrong()
        }

        val difficulty = engine.record(result.correct)

        if (firstAttempt) {
            answeredCurrentQuestion = true
            viewModelScope.launch {
                progressRepository.recordAnswer(
                    AnswerEvent(
                        levelId = spec.levelId,
                        kind = question.kind,
                        difficulty = difficulty,
                        correct = result.correct,
                        durationMs = durationMs,
                        routine = question.routine != null,
                        fast = result.correct && durationMs <= FAST_ANSWER_MS,
                        playSeconds = (durationMs / 1000L).toInt().coerceAtLeast(1)
                    )
                )
            }
            _uiState.update { state ->
                if (result.correct) {
                    state.copy(correct = state.correct + 1)
                } else {
                    state.copy(wrong = state.wrong + 1)
                }
            }
        }

        _uiState.update { state ->
            state.copy(
                feedback = result,
                feedbackKind = feedbackKind,
                difficulty = difficulty,
                hintsOn = engine.hintsOn && settings.hintsEnabled
            )
        }

        if (result.correct && (spec.endless)) {
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(900L)
                nextQuestion(first = false)
            }
        }
    }

    // ------------------------------------------------------------- daily limit

    /**
     * Checks every 20 seconds whether the daily learning time is up.
     *
     * The limit is a setting for grown-ups, but it is applied kindly: the session pauses behind a
     * break dialog instead of throwing the child out, and a grown-up can grant more time.
     */
    private fun startLimitWatcher() {
        limitJob?.cancel()
        limitJob = viewModelScope.launch {
            while (true) {
                delay(20_000L)
                checkDailyLimit()
            }
        }
    }

    private fun checkDailyLimit() {
        val limitMinutes = settings.dailyLimitMinutes + grantedExtraMinutes
        if (limitMinutes <= 0) return
        if (_uiState.value.phase == SessionPhase.RESULT) return
        val sessionSeconds = (System.currentTimeMillis() - sessionStartMs) / 1000L
        if ((todaySeconds + sessionSeconds) / 60 >= limitMinutes) {
            timerJob?.cancel()
            pausedAtMs = System.currentTimeMillis()
            _uiState.update { it.copy(breakTime = true) }
        }
    }

    /** The child accepts the break: the session ends and the result screen is shown. */
    fun onTakeBreak() {
        audio.button()
        finishSession()
    }

    fun onAskGrownUp() {
        audio.button()
        _uiState.update { it.copy(gateQuestion = createGateQuestion(random)) }
    }

    fun onCancelGate() {
        _uiState.update { it.copy(gateQuestion = null) }
    }

    fun onGateAnswer(value: Int) {
        val question = _uiState.value.gateQuestion ?: return
        if (value == question.answer) {
            audio.celebrate()
            grantedExtraMinutes += EXTRA_MINUTES
            if (pausedAtMs > 0L) {
                sessionStartMs += System.currentTimeMillis() - pausedAtMs
                pausedAtMs = 0L
            }
            _uiState.update {
                it.copy(
                    gateQuestion = null,
                    breakTime = false,
                    grantedExtraMinutes = grantedExtraMinutes
                )
            }
            // Give the countdown a fresh window so the child is not punished for the pause.
            startTimers()
        } else {
            audio.wrong()
            _uiState.update { it.copy(gateQuestion = createGateQuestion(random)) }
        }
    }

    // ------------------------------------------------------------- match game

    private fun startMatchRound() {
        val difficulty = spec.baseDifficulty
        val round = generateQuestion.matchRound(difficulty, pairs = MATCH_PAIRS, random = random)
        _uiState.update {
            it.copy(
                phase = SessionPhase.QUESTION,
                matchState = MatchState(
                    clocks = round.pairs,
                    times = round.pairs.shuffled(random)
                ),
                difficulty = difficulty,
                snapMinutes = spec.minuteStepFor(difficulty),
                totalQuestions = 0
            )
        }
    }

    fun onMatchClockSelected(pairId: String) {
        audio.button()
        _uiState.update { state ->
            val match = state.matchState ?: return@update state
            state.copy(matchState = match.copy(selectedClock = pairId))
        }
    }

    fun onMatchTimeSelected(pairId: String) {
        val state = _uiState.value
        val match = state.matchState ?: return
        val selectedClock = match.selectedClock
        if (selectedClock == null) {
            audio.button()
            return
        }
        if (selectedClock == pairId) {
            audio.correct()
            val updated = match.copy(
                matched = match.matched + pairId,
                selectedClock = null,
                attempts = match.attempts + 1
            )
            _uiState.update { it.copy(matchState = updated) }
            viewModelScope.launch {
                progressRepository.recordAnswer(
                    AnswerEvent(
                        levelId = 0,
                        kind = QuestionKind.READ_TIME,
                        difficulty = state.difficulty,
                        correct = true,
                        durationMs = 0L,
                        fast = false,
                        playSeconds = 2
                    )
                )
            }
            if (updated.isFinished) {
                viewModelScope.launch {
                    delay(500L)
                    finishSession()
                }
            }
        } else {
            audio.wrong()
            _uiState.update {
                it.copy(
                    matchState = match.copy(
                        selectedClock = null,
                        attempts = match.attempts + 1,
                        wrongFlash = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------- finishing

    fun finishSession() {
        timerJob?.cancel()
        val state = _uiState.value
        if (state.phase == SessionPhase.RESULT) return
        val durationMs = System.currentTimeMillis() - sessionStartMs
        val totalQuestions = when {
            spec.endless -> state.correct + state.wrong
            spec.isMatchGame -> state.matchState?.totalCount ?: 0
            else -> spec.questionCount
        }
        val correct = when {
            spec.isMatchGame -> state.matchState?.matchedCount ?: 0
            else -> state.correct
        }
        val wrong = (totalQuestions - correct).coerceAtLeast(0)
        val difficulty = state.difficulty

        audio.celebrate()

        viewModelScope.launch {
            var completion: LessonCompletionResult? = null
            when {
                spec.isLesson -> {
                    completion = progressRepository.completeLesson(
                        levelId = spec.levelId,
                        stars = RewardEngine.starsFor(totalQuestions, correct, state.hintsUsed),
                        correct = correct,
                        wrong = wrong,
                        hintsUsed = state.hintsUsed,
                        flawless = totalQuestions > 0 && wrong == 0 && state.hintsUsed == 0,
                        durationMs = durationMs,
                        difficulty = difficulty
                    )
                }
                spec.gameId != null -> {
                    progressRepository.recordGameScore(spec.gameId!!, correct)
                    progressRepository.addCoins(RewardEngine.coinsFor(
                        stars = RewardEngine.starsFor(totalQuestions, correct, state.hintsUsed),
                        correct = correct,
                        isNewBest = false,
                        flawless = wrong == 0
                    ))
                    progressRepository.addXp(RewardEngine.xpFor(correct, RewardEngine.starsFor(totalQuestions, correct, state.hintsUsed), difficulty))
                }
                else -> {
                    progressRepository.recordSession((durationMs / 1000L).toInt())
                    progressRepository.addCoins(RewardEngine.coinsFor(
                        stars = RewardEngine.starsFor(totalQuestions, correct, state.hintsUsed),
                        correct = correct,
                        isNewBest = false,
                        flawless = wrong == 0
                    ))
                    progressRepository.addXp(RewardEngine.xpFor(correct, RewardEngine.starsFor(totalQuestions, correct, state.hintsUsed), difficulty))
                }
            }

            val result = buildSessionResult(
                title = spec.title,
                levelId = spec.levelId,
                totalQuestions = totalQuestions,
                correct = correct,
                hintsUsed = state.hintsUsed,
                difficulty = difficulty,
                durationMs = durationMs,
                newAchievements = completion?.newAchievements ?: emptyList(),
                newUnlocks = completion?.newUnlocks ?: emptyList(),
                leveledUp = completion?.leveledUp ?: false,
                isNewBest = completion?.isNewBest ?: false,
                newLevel = completion?.currentLevel ?: 0
            )

            val limitMinutes = settings.dailyLimitMinutes + grantedExtraMinutes
            val limitReached = limitMinutes > 0 && (todaySeconds + durationMs / 1000L) / 60 >= limitMinutes

            _uiState.update {
                it.copy(
                    phase = SessionPhase.RESULT,
                    result = result,
                    nextLevelId = completion?.unlockedNextLevelId,
                    dailyLimitReached = limitReached
                )
            }
        }
    }

    fun onReplay() {
        sessionStartMs = System.currentTimeMillis()
        grantedExtraMinutes = 0
        startLimitWatcher()
        engine = DifficultyEngine(mode = DifficultyMode.AUTO, start = spec.baseDifficulty)
        _uiState.update {
            SessionUiState(
                title = spec.title,
                teach = null,
                totalQuestions = if (spec.endless) 0 else spec.questionCount,
                isLesson = spec.isLesson,
                levelId = spec.levelId,
                gameId = spec.gameId,
                settings = settings,
                difficulty = spec.baseDifficulty
            )
        }
        if (spec.isMatchGame) startMatchRound() else nextQuestion(first = true)
    }

    fun onQuit() {
        audio.button()
    }

    override fun onCleared() {
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        limitJob?.cancel()
        super.onCleared()
    }

    /** Text shown under the clock while a SET_CLOCK question is active. */
    fun digitalText(time: ClockTime): String =
        TimeFormatter.digital(time, use24Hour = settings.use24Hour, language = settings.language)

    /** Hint sentence shown when the child asks for help. */
    fun hintText(): LocalizedText {
        val question = _uiState.value.question ?: return LocalizedText("", "")
        return when (_uiState.value.question?.tapHand) {
            ClockHand.MINUTE -> LocalizedText(
                "Count by fives from 12: ${question.targetTime.minute / 5} steps = ${question.targetTime.minute} minutes.",
                "عد خمساتٍ من الرقم 12: ${question.targetTime.minute / 5} خطوات = ${question.targetTime.minute} دقيقة."
            )
            else -> evaluateAnswer.explanation(question)
        }
    }

    companion object {
        const val MATCH_PAIRS = 4
        const val FAST_ANSWER_MS = 8_000L
        /** Minutes a grown-up grants when they solve the gate after the daily limit. */
        const val EXTRA_MINUTES = 15
    }
}
