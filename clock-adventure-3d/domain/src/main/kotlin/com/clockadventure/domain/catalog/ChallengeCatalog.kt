package com.clockadventure.domain.catalog

import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.QuestionKind

/**
 * Extra challenge modes, reachable from the Challenges button on the home screen.
 *
 * They are harder and shorter than the lessons, always optional, and never block progress: a child
 * who only plays the ten lessons still finishes the whole learning path.
 */
data class ChallengeSpec(
    val id: String,
    val title: LocalizedText,
    val subtitle: LocalizedText,
    val icon: GameIcon,
    val kinds: List<QuestionKind>,
    val questionCount: Int,
    val stepEasy: Int,
    val stepMedium: Int,
    val stepHard: Int,
    val totalTimeLimitMs: Long? = null,
    val perQuestionLimitMs: Long? = null,
    val endless: Boolean = false,
    val baseDifficulty: Difficulty = Difficulty.MEDIUM
) {
    fun minuteStepFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> stepEasy
        Difficulty.MEDIUM -> stepMedium
        Difficulty.HARD -> stepHard
    }
}

object ChallengeCatalog {
    val items: List<ChallengeSpec> = listOf(
        ChallengeSpec(
            id = "daily",
            title = LocalizedText("Daily Challenge", "تحدي اليوم"),
            subtitle = LocalizedText("Ten mixed questions", "عشرة أسئلة متنوعة"),
            icon = GameIcon.QUESTION,
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK, QuestionKind.TRUE_FALSE),
            questionCount = 10,
            stepEasy = 30, stepMedium = 15, stepHard = 5
        ),
        ChallengeSpec(
            id = "time_attack",
            title = LocalizedText("Time Attack", "هجوم الوقت"),
            subtitle = LocalizedText("45 seconds, no stopping!", "45 ثانية بلا توقف!"),
            icon = GameIcon.RACE,
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.TRUE_FALSE),
            questionCount = 0,
            stepEasy = 30, stepMedium = 15, stepHard = 5,
            totalTimeLimitMs = 45_000L,
            endless = true,
            baseDifficulty = Difficulty.MEDIUM
        ),
        ChallengeSpec(
            id = "perfect_run",
            title = LocalizedText("Perfect Run", "جولة مثالية"),
            subtitle = LocalizedText("Can you finish without a mistake?", "هل تنهيها دون خطأ؟"),
            icon = GameIcon.HANDS,
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 60, stepMedium = 30, stepHard = 15,
            perQuestionLimitMs = 20_000L,
            baseDifficulty = Difficulty.EASY
        ),
        ChallengeSpec(
            id = "boss",
            title = LocalizedText("Boss Clock", "الساعة الكبيرة"),
            subtitle = LocalizedText("Every minute counts", "كل دقيقة مهمة"),
            icon = GameIcon.TRUE_FALSE,
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK, QuestionKind.TRUE_FALSE),
            questionCount = 6,
            stepEasy = 15, stepMedium = 5, stepHard = 1,
            perQuestionLimitMs = 30_000L,
            baseDifficulty = Difficulty.HARD
        )
    )

    fun byId(id: String): ChallengeSpec = items.first { it.id == id }
}
