package com.clockadventure.domain.engine

import com.clockadventure.domain.model.Difficulty

/**
 * Stars, coins and xp.
 *
 * The rules are deliberately generous: a finished lesson always earns at least one star, and a
 * wrong answer never removes coins. Children are rewarded for trying, not punished for missing.
 */
object RewardEngine {

    /** Cumulative xp needed to reach the given player level (level 1 starts at 0). */
    fun xpNeededForLevel(level: Int): Int {
        val l = level.coerceAtLeast(1)
        return 50 * (l - 1) * l
    }

    fun playerLevelFor(xp: Int): Int {
        var level = 1
        while (xpNeededForLevel(level + 1) <= xp && level < 99) level++
        return level
    }

    /** 0f..1f progress inside the current player level. */
    fun levelProgress(xp: Int): Float {
        val level = playerLevelFor(xp)
        val floor = xpNeededForLevel(level)
        val ceiling = xpNeededForLevel(level + 1)
        if (ceiling <= floor) return 0f
        return ((xp - floor).toFloat() / (ceiling - floor).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Three star scoring:
     * * 3 stars - 90% correct or better and no hint was needed
     * * 2 stars - 75% or better
     * * 1 star - the lesson was finished (every child leaves with a star)
     */
    fun starsFor(totalQuestions: Int, correct: Int, hintsUsed: Int): Int {
        if (totalQuestions <= 0) return 0
        val accuracy = correct.toFloat() / totalQuestions.toFloat()
        return when {
            correct == 0 -> 0
            accuracy >= 0.9f && hintsUsed == 0 -> 3
            accuracy >= 0.75f -> 2
            else -> 1
        }
    }

    fun coinsFor(stars: Int, correct: Int, isNewBest: Boolean, flawless: Boolean): Int {
        var coins = stars * 10 + correct * 2
        if (isNewBest) coins += 15
        if (flawless) coins += 20
        return coins
    }

    fun xpFor(correct: Int, stars: Int, difficulty: Difficulty): Int =
        correct * 10 + stars * 25 + difficulty.ordinal * 10

    fun dailyRewardCoins(streakDays: Int): Int = 20 + (streakDays.coerceAtMost(7) * 5)

    fun dailyRewardXp(streakDays: Int): Int = 10 + (streakDays.coerceAtMost(7) * 2)

    /** Stars needed to unlock a level: finishing the previous one. */
    fun isLevelUnlocked(levelId: Int, bestStarsOfPrevious: Int): Boolean =
        levelId <= 1 || bestStarsOfPrevious > 0
}
