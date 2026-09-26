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
        assertTrue("progress at 0 xp", RewardEngine.levelProgress(0) == 0f)
        // 200 xp is exactly the middle of level 2 (100..300), so pick points around it.
        assertEquals(0.25f, RewardEngine.levelProgress(150), 0.001f)
        assertTrue("progress at 250 xp = ${RewardEngine.levelProgress(250)}", RewardEngine.levelProgress(250) > 0.5f)
        // Level 14 starts at 9 100 xp and level 15 at 10 500, so 10 000 sits inside level 14.
        assertTrue("progress restarts at a level floor", RewardEngine.levelProgress(9_100) == 0f)
        assertTrue(
            "progress at 10000 xp = ${RewardEngine.levelProgress(10_000)}",
            RewardEngine.levelProgress(10_000) in 0.1f..0.9f
        )
        assertTrue("progress is never above 1", RewardEngine.levelProgress(1_000_000) <= 1f)
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
