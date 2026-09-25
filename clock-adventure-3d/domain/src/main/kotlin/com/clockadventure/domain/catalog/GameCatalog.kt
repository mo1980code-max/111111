package com.clockadventure.domain.catalog

import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.QuestionKind

/** Canvas drawn artwork for the mini game cards. */
enum class GameIcon { QUESTION, HANDS, MATCH, RACE, TRUE_FALSE, ROUTINE }

enum class GameId { WHAT_TIME, SET_CLOCK, MATCH_TIME, TIME_RACE, TRUE_FALSE, DAILY_ROUTINE }

data class GameSpec(
    val id: GameId,
    val title: LocalizedText,
    val subtitle: LocalizedText,
    val icon: GameIcon,
    val kinds: List<QuestionKind>,
    /** Number of questions in one round; ignored when [endless] is true. */
    val questionCount: Int,
    val stepEasy: Int,
    val stepMedium: Int,
    val stepHard: Int,
    /** Total time for the whole round (Time Race). */
    val totalTimeLimitMs: Long? = null,
    /** Time per question. */
    val perQuestionLimitMs: Long? = null,
    /** Endless rounds keep generating questions until the time is up. */
    val endless: Boolean = false,
    val baseDifficulty: Difficulty = Difficulty.MEDIUM
) {
    fun minuteStepFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> stepEasy
        Difficulty.MEDIUM -> stepMedium
        Difficulty.HARD -> stepHard
    }
}

object GameCatalog {
    val games: List<GameSpec> = listOf(
        GameSpec(
            id = GameId.WHAT_TIME,
            title = LocalizedText("What Time Is It?", "كم الساعة؟"),
            subtitle = LocalizedText("Read the clock, pick the time", "اقرأ الساعة واختر الوقت"),
            icon = GameIcon.QUESTION,
            kinds = listOf(QuestionKind.READ_TIME),
            questionCount = 8,
            stepEasy = 30, stepMedium = 5, stepHard = 1
        ),
        GameSpec(
            id = GameId.SET_CLOCK,
            title = LocalizedText("Set The Clock", "اضبط الساعة"),
            subtitle = LocalizedText("Move the hands to the right time", "حرّك العقارب إلى الوقت الصحيح"),
            icon = GameIcon.HANDS,
            kinds = listOf(QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 60, stepMedium = 15, stepHard = 5
        ),
        GameSpec(
            id = GameId.MATCH_TIME,
            title = LocalizedText("Match The Time", "طابق الوقت"),
            subtitle = LocalizedText("Pair clocks with digital times", "وصّل الساعات بالأوقات الرقمية"),
            icon = GameIcon.MATCH,
            // Match builds its own round of clock/digital pairs (MatchRound) instead of the
            // usual one-question-at-a-time flow, so it declares no question kinds.
            kinds = emptyList(),
            questionCount = 4,
            stepEasy = 30, stepMedium = 15, stepHard = 5
        ),
        GameSpec(
            id = GameId.TIME_RACE,
            title = LocalizedText("Time Race", "سباق الوقت"),
            subtitle = LocalizedText("60 seconds - as many as you can!", "60 ثانية - أكبر عدد ممكن!"),
            icon = GameIcon.RACE,
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK, QuestionKind.TRUE_FALSE),
            questionCount = 0,
            stepEasy = 30, stepMedium = 15, stepHard = 5,
            totalTimeLimitMs = 60_000L,
            endless = true
        ),
        GameSpec(
            id = GameId.TRUE_FALSE,
            title = LocalizedText("True or False", "صح أم خطأ"),
            subtitle = LocalizedText("Does the clock match the time?", "هل الساعة تطابق الوقت؟"),
            icon = GameIcon.TRUE_FALSE,
            kinds = listOf(QuestionKind.TRUE_FALSE),
            questionCount = 8,
            stepEasy = 30, stepMedium = 15, stepHard = 5
        ),
        GameSpec(
            id = GameId.DAILY_ROUTINE,
            title = LocalizedText("My Daily Routine", "روتيني اليومي"),
            subtitle = LocalizedText("Your whole day on the clock", "يومك كله على الساعة"),
            icon = GameIcon.ROUTINE,
            kinds = listOf(QuestionKind.ROUTINE_CHOICE, QuestionKind.ROUTINE_SET_CLOCK),
            questionCount = 8,
            stepEasy = 30, stepMedium = 15, stepHard = 5
        )
    )

    fun byId(id: GameId): GameSpec = games.first { it.id == id }
}
