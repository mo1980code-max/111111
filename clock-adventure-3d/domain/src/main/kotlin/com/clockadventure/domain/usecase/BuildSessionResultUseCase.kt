package com.clockadventure.domain.usecase

import com.clockadventure.domain.engine.RewardEngine
import com.clockadventure.domain.model.Achievement
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.SessionResult
import com.clockadventure.domain.model.Unlockable
import javax.inject.Inject

/** Turns the counters of a finished session into stars, coins, xp and a result screen model. */
class BuildSessionResultUseCase @Inject constructor() {

    operator fun invoke(
        title: LocalizedText,
        levelId: Int,
        totalQuestions: Int,
        correct: Int,
        hintsUsed: Int,
        difficulty: Difficulty,
        durationMs: Long,
        newAchievements: List<Achievement> = emptyList(),
        newUnlocks: List<Unlockable> = emptyList(),
        leveledUp: Boolean = false,
        isNewBest: Boolean = false,
        practiceMode: Boolean = false,
        newLevel: Int = 0
    ): SessionResult {
        val wrong = (totalQuestions - correct).coerceAtLeast(0)
        val stars = RewardEngine.starsFor(totalQuestions, correct, hintsUsed)
        val flawless = totalQuestions > 0 && wrong == 0
        return SessionResult(
            title = title,
            message = messageFor(stars, correct, totalQuestions),
            levelId = levelId,
            totalQuestions = totalQuestions,
            correct = correct,
            wrong = wrong,
            stars = stars,
            coinsEarned = RewardEngine.coinsFor(stars, correct, isNewBest, flawless),
            xpEarned = RewardEngine.xpFor(correct, stars, difficulty),
            durationMs = durationMs,
            newAchievements = newAchievements,
            newUnlocks = newUnlocks,
            leveledUp = leveledUp,
            isNewBest = isNewBest,
            practiceMode = practiceMode,
            newLevel = newLevel
        )
    }

    /** Short, kind sentence shown under the stars - never discouraging, always pointing forward. */
    private fun messageFor(stars: Int, correct: Int, totalQuestions: Int): LocalizedText = when {
        totalQuestions == 0 -> LocalizedText("Nice try! Shall we go again?", "محاولة جميلة! هل نعيدها؟")
        stars == 3 -> LocalizedText("Perfect! You are a clock expert!", "ممتاز! أنت خبير في الساعة!")
        stars == 2 -> LocalizedText("Very good! Almost perfect!", "جيد جداً! قريب من الكمال!")
        stars == 1 -> LocalizedText("Good start - let's try once more!", "بداية جيدة - هيا نحاول مرة أخرى!")
        else -> LocalizedText(
            "Every try makes you better. Let's look at the clock together!",
            "كل محاولة تجعلك أفضل. هيا ننظر إلى الساعة معاً!"
        )
    }
}
