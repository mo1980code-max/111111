package com.clockadventure.domain.model

/** Result of a finished lesson or mini game round, used by the celebration screen. */
data class SessionResult(
    val title: LocalizedText,
    val message: LocalizedText,
    val levelId: Int,
    val totalQuestions: Int,
    val correct: Int,
    val wrong: Int,
    val stars: Int,
    val coinsEarned: Int,
    val xpEarned: Int,
    val durationMs: Long,
    val newAchievements: List<Achievement> = emptyList(),
    val newUnlocks: List<Unlockable> = emptyList(),
    val leveledUp: Boolean = false,
    val isNewBest: Boolean = false,
    val practiceMode: Boolean = false,
    val newLevel: Int = 0
) {
    val accuracy: Float
        get() = if (totalQuestions == 0) 0f else correct.toFloat() / totalQuestions.toFloat()
}

/** What the repository returns after a lesson is graded and stored. */
data class LessonCompletionResult(
    val stars: Int,
    val coinsEarned: Int,
    val xpEarned: Int,
    val newAchievements: List<Achievement>,
    val newUnlocks: List<Unlockable>,
    val leveledUp: Boolean,
    val isNewBest: Boolean,
    val unlockedNextLevelId: Int?,
    val currentLevel: Int = 1
)

/** Result of claiming the once-a-day gift on the Rewards screen. */
data class DailyRewardResult(
    val coins: Int,
    val xp: Int,
    val streakDays: Int
)
