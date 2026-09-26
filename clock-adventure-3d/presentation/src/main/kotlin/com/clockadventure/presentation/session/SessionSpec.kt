package com.clockadventure.presentation.session

import com.clockadventure.domain.catalog.ChallengeSpec
import com.clockadventure.domain.catalog.GameId
import com.clockadventure.domain.catalog.GameSpec
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.catalog.LessonSpec
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.QuestionKind

/**
 * One abstraction over "a run of questions", so lessons, mini games and challenges all share the
 * same screen, the same interactive clock and the same reward logic.
 */
sealed class SessionSpec {

    abstract val key: String
    abstract val title: LocalizedText
    abstract val kinds: List<QuestionKind>
    abstract val questionCount: Int
    abstract val endless: Boolean
    abstract val totalTimeLimitMs: Long?
    abstract val perQuestionLimitMs: Long?
    abstract val showDigitalHelper: Boolean
    abstract val baseDifficulty: Difficulty
    abstract val levelId: Int
    abstract val gameId: GameId?
    abstract val teach: LocalizedText?

    abstract fun minuteStepFor(difficulty: Difficulty): Int

    val isLesson: Boolean get() = levelId > 0
    val isMatchGame: Boolean get() = this is Game && gameId == GameId.MATCH_TIME

    data class Lesson(val spec: LessonSpec) : SessionSpec() {
        override val key: String get() = "lesson_${spec.id}"
        override val title: LocalizedText get() = spec.title
        override val kinds: List<QuestionKind> get() = spec.kinds
        override val questionCount: Int get() = spec.questionCount
        override val endless: Boolean get() = false
        override val totalTimeLimitMs: Long? get() = null
        override val perQuestionLimitMs: Long? get() = spec.timeLimitMs
        override val showDigitalHelper: Boolean get() = spec.showDigitalHelper
        override val baseDifficulty: Difficulty get() = spec.baseDifficulty
        override val levelId: Int get() = spec.id
        override val gameId: GameId? get() = null
        override val teach: LocalizedText? get() = spec.teach
        override fun minuteStepFor(difficulty: Difficulty): Int = spec.minuteStepFor(difficulty)
        fun snapMinutesFor(difficulty: Difficulty): Int = spec.snapMinutesFor(difficulty)
    }

    data class Game(val spec: GameSpec) : SessionSpec() {
        override val key: String get() = "game_${spec.id.name}"
        override val title: LocalizedText get() = spec.title
        override val kinds: List<QuestionKind> get() = spec.kinds
        override val questionCount: Int get() = spec.questionCount
        override val endless: Boolean get() = spec.endless
        override val totalTimeLimitMs: Long? get() = spec.totalTimeLimitMs
        override val perQuestionLimitMs: Long? get() = spec.perQuestionLimitMs
        override val showDigitalHelper: Boolean get() = false
        override val baseDifficulty: Difficulty get() = spec.baseDifficulty
        override val levelId: Int get() = 0
        override val gameId: GameId? get() = spec.id
        override val teach: LocalizedText? get() = null
        override fun minuteStepFor(difficulty: Difficulty): Int = spec.minuteStepFor(difficulty)
    }

    data class Challenge(val spec: ChallengeSpec) : SessionSpec() {
        override val key: String get() = "challenge_${spec.id}"
        override val title: LocalizedText get() = spec.title
        override val kinds: List<QuestionKind> get() = spec.kinds
        override val questionCount: Int get() = spec.questionCount
        override val endless: Boolean get() = spec.endless
        override val totalTimeLimitMs: Long? get() = spec.totalTimeLimitMs
        override val perQuestionLimitMs: Long? get() = spec.perQuestionLimitMs
        override val showDigitalHelper: Boolean get() = false
        override val baseDifficulty: Difficulty get() = spec.baseDifficulty
        override val levelId: Int get() = 0
        override val gameId: GameId? get() = null
        override val teach: LocalizedText? get() = null
        override fun minuteStepFor(difficulty: Difficulty): Int = spec.minuteStepFor(difficulty)
    }

    companion object {
        /** Builds the session described by the navigation arguments. */
        fun from(levelId: Int, gameId: String?, challengeId: String?): SessionSpec = when {
            levelId > 0 -> Lesson(LevelCatalog.byId(levelId))
            !gameId.isNullOrBlank() -> Game(
                com.clockadventure.domain.catalog.GameCatalog.byId(
                    runCatching { GameId.valueOf(gameId) }.getOrDefault(GameId.WHAT_TIME)
                )
            )
            !challengeId.isNullOrBlank() -> Challenge(
                com.clockadventure.domain.catalog.ChallengeCatalog.byId(challengeId)
            )
            else -> Lesson(LevelCatalog.levels.first())
        }
    }
}
