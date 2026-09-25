package com.clockadventure.domain

import com.clockadventure.domain.engine.RewardEngine
import com.clockadventure.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rewards: generous, never punishing, and always explainable. */
class RewardEngineTest {

    @Test
    fun `perfect lesson earns three stars`() {
        assertEquals(3, RewardEngine.starsFor(8, 8, hintsUsed = 0))
    }

    @Test
    fun `using a hint costs the third star`() {
        assertEquals(2, RewardEngine.starsFor(8, 8, hintsUsed = 1))
        assertEquals(3, RewardEngine.starsFor(8, 8, hintsUsed = 0))
    }

    @Test
    fun `finishing a lesson always leaves with at least one star`() {
        assertTrue(RewardEngine.starsFor(8, 3, hintsUsed = 4) >= 1)
        assertEquals(1, RewardEngine.starsFor(8, 4, hintsUsed = 4))
    }

    @Test
    fun `an empty session earns nothing`() {
        assertEquals(0, RewardEngine.starsFor(0, 0, 0))
    }

    @Test
    fun `xp thresholds grow per level`() {
        assertEquals(0, RewardEngine.xpNeededForLevel(1))
        assertEquals(100, RewardEngine.xpNeededForLevel(2))
        assertEquals(300, RewardEngine.xpNeededForLevel(3))
        assertEquals(1, RewardEngine.playerLevelFor(0))
        assertEquals(2, RewardEngine.playerLevelFor(100))
        assertEquals(3, RewardEngine.playerLevelFor(300))
        assertTrue(RewardEngine.levelProgress(0) == 0f)
        assertTrue(RewardEngine.levelProgress(200) > 0.5f)
    }

    @Test
    fun `harder difficulty pays more xp`() {
        assertTrue(RewardEngine.xpFor(5, 3, Difficulty.HARD) > RewardEngine.xpFor(5, 3, Difficulty.EASY))
    }

    @Test
    fun `coins grow with stars and flawless runs`() {
        assertTrue(RewardEngine.coinsFor(3, 8, isNewBest = true, flawless = true) >
            RewardEngine.coinsFor(1, 3, isNewBest = false, flawless = false))
    }

    @Test
    fun `daily reward grows with the streak but stays bounded`() {
        assertTrue(RewardEngine.dailyRewardCoins(2) > RewardEngine.dailyRewardCoins(1))
        assertEquals(RewardEngine.dailyRewardCoins(9), RewardEngine.dailyRewardCoins(7))
    }

    @Test
    fun `levels unlock by finishing the previous one`() {
        assertTrue(RewardEngine.isLevelUnlocked(1, 0))
        assertTrue(RewardEngine.isLevelUnlocked(2, 1))
        assertTrue(!RewardEngine.isLevelUnlocked(2, 0))
    }
}
