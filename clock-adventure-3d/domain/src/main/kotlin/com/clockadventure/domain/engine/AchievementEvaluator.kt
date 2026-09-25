package com.clockadventure.domain.engine

import com.clockadventure.domain.model.Achievement
import com.clockadventure.domain.model.AchievementCatalog
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.UserProgress

/**
 * Turns the raw counters of [UserProgress] into achievement progress.
 *
 * Achievements are unlocked automatically as soon as their goal is reached, so the Rewards screen
 * only has to render the state.
 */
object AchievementEvaluator {

    fun evaluate(progress: UserProgress, unlockedIds: Set<String>): List<AchievementState> =
        AchievementCatalog.items.map { achievement ->
            val value = progress.metricValue(achievement.metric)
            AchievementState(
                achievement = achievement,
                progress = value.coerceAtMost(achievement.goal),
                unlocked = unlockedIds.contains(achievement.id) || value >= achievement.goal
            )
        }

    /** Achievements whose goal was reached but which are not stored as unlocked yet. */
    fun newlyReached(
        previous: List<AchievementState>,
        current: List<AchievementState>
    ): List<Achievement> {
        val before = previous.filter { it.unlocked }.map { it.achievement.id }.toSet()
        return current.filter { it.unlocked && it.achievement.id !in before }.map { it.achievement }
    }
}
