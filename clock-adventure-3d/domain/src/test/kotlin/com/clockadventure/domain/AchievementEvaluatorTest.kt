package com.clockadventure.domain

import com.clockadventure.domain.engine.AchievementEvaluator
import com.clockadventure.domain.model.AchievementCatalog
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.UserProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Achievements unlock themselves as soon as the counters reach their goal. */
class AchievementEvaluatorTest {

    private fun evaluate(progress: UserProgress, unlocked: Set<String> = emptySet()) =
        AchievementEvaluator.evaluate(progress, unlocked)

    @Test
    fun `nothing is unlocked on a fresh profile`() {
        val states = evaluate(UserProgress())
        assertEquals(AchievementCatalog.items.size, states.size)
        assertTrue(states.none { it.unlocked })
    }

    @Test
    fun `the first star unlocks the first achievement`() {
        val states = evaluate(UserProgress(stars = 1))
        val first = states.first { it.achievement.id == "first_star" }
        assertTrue(first.unlocked)
        assertEquals(1, first.progress)
    }

    @Test
    fun `progress is capped at the goal`() {
        val states = evaluate(UserProgress(stars = 999))
        val first = states.first { it.achievement.id == "first_star" }
        assertEquals(1, first.progress)
        assertEquals(1f, first.progressFraction)
    }

    @Test
    fun `newly reached achievements are reported once`() {
        val before: List<AchievementState> = evaluate(UserProgress())
        val after = evaluate(UserProgress(stars = 1))
        val reached = AchievementEvaluator.newlyReached(before, after)
        assertEquals(listOf("first_star"), reached.map { it.id })
        assertTrue(AchievementEvaluator.newlyReached(after, after).isEmpty())
    }

    @Test
    fun `player level achievements follow the xp curve`() {
        val states = evaluate(UserProgress(xp = 1000))
        val level5 = states.first { it.achievement.id == "player_level_5" }
        assertTrue(level5.progress >= 5)
    }
}
