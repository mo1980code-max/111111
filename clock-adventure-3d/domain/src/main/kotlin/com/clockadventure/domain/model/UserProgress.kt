package com.clockadventure.domain.model

import com.clockadventure.domain.engine.RewardEngine
import kotlin.math.roundToInt

/**
 * Everything the child has achieved so far. Emitted as one immutable snapshot by the repository
 * so screens can render it without pulling from several sources.
 */
data class UserProgress(
    val xp: Int = 0,
    val coins: Int = 0,
    val stars: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val totalPlaySeconds: Int = 0,
    val lessonsCompleted: Int = 0,
    val currentLevelId: Int = 1,
    val flawlessLessons: Int = 0,
    val daysPlayed: Int = 0,
    val routineCorrect: Int = 0,
    val fastCorrect: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDateKey: String? = null,
    val lastDailyRewardDateKey: String? = null,
    val parentVerified: Boolean = false
) {
    val totalAnswers: Int get() = totalCorrect + totalWrong

    /** 0f..1f, or 1f when nothing has been answered yet (no punishment for an empty slate). */
    val accuracy: Float
        get() = if (totalAnswers == 0) 1f else totalCorrect.toFloat() / totalAnswers.toFloat()

    val accuracyPercent: Int get() = (accuracy * 100f).roundToInt()

    /** The level shown in the xp badge (1..). */
    val playerLevel: Int get() = RewardEngine.playerLevelFor(xp)

    /** Progress inside the current player level, 0f..1f. */
    val levelProgress: Float get() = RewardEngine.levelProgress(xp)

    val xpForNextLevel: Int get() = RewardEngine.xpNeededForLevel(playerLevel + 1)

    val xpIntoLevel: Int get() = xp - RewardEngine.xpNeededForLevel(playerLevel)

    fun metricValue(metric: com.clockadventure.domain.model.AchievementMetric): Int = when (metric) {
        AchievementMetric.STARS -> stars
        AchievementMetric.COINS -> coins
        AchievementMetric.CORRECT_ANSWERS -> totalCorrect
        AchievementMetric.LESSONS_COMPLETED -> lessonsCompleted
        AchievementMetric.FLAWLESS_LESSONS -> flawlessLessons
        AchievementMetric.DAYS_PLAYED -> daysPlayed
        AchievementMetric.ROUTINE_ANSWERS -> routineCorrect
        AchievementMetric.FAST_ANSWERS -> fastCorrect
        AchievementMetric.PLAYER_LEVEL -> playerLevel
    }
}

/** Per learning level statistics, used by the level map and the parent dashboard. */
data class LessonProgress(
    val levelId: Int,
    val bestStars: Int = 0,
    val completions: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val playSeconds: Int = 0,
    val updatedAt: Long = 0L
) {
    val answers: Int get() = correct + wrong
    val accuracy: Float get() = if (answers == 0) 0f else correct.toFloat() / answers.toFloat()
    val isCompleted: Boolean get() = bestStars > 0
}

/** Best score of a mini game. */
data class GameScore(
    val gameId: String,
    val bestScore: Int = 0,
    val plays: Int = 0
)

/** One day of learning, shown as a bar in the parent dashboard. */
data class DailyStat(
    val dateKey: String,
    val secondsLearned: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val sessions: Int = 0
)
