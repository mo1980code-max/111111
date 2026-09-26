package com.clockadventure.domain.repository

import com.clockadventure.domain.catalog.GameId
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.DailyRewardResult
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.GameScore
import com.clockadventure.domain.model.LessonCompletionResult
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.QuestionKind
import com.clockadventure.domain.model.Unlockable
import com.clockadventure.domain.model.UnlockState
import com.clockadventure.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow

/** Everything the app knows about one answered question. */
data class AnswerEvent(
    val levelId: Int,
    val kind: QuestionKind,
    val difficulty: Difficulty,
    val correct: Boolean,
    val durationMs: Long,
    /** True for real-life / routine questions. */
    val routine: Boolean = false,
    /** Answered quicker than the "fast answer" threshold. */
    val fast: Boolean = false,
    /** Seconds of play time to add to today's learning time. */
    val playSeconds: Int = 0
)

/**
 * Persistence of everything the child earns and learns.
 *
 * Implemented in the `data` module with Room; the domain only sees cold flows of immutable
 * snapshots, which keeps the UI reactive and the rules testable.
 */
interface ProgressRepository {

    fun observeProgress(): Flow<UserProgress>

    fun observeLessons(): Flow<List<LessonProgress>>

    fun observeAchievements(): Flow<List<AchievementState>>

    fun observeUnlockStates(): Flow<List<UnlockState>>

    fun observeGameScores(): Flow<List<GameScore>>

    fun observeDailyStats(days: Int): Flow<List<DailyStat>>

    fun observeTodayStat(): Flow<DailyStat>

    suspend fun recordAnswer(event: AnswerEvent)

    /** Counts a finished session (challenges) for the daily statistics. */
    suspend fun recordSession(seconds: Int)

    suspend fun completeLesson(
        levelId: Int,
        stars: Int,
        correct: Int,
        wrong: Int,
        hintsUsed: Int,
        flawless: Boolean,
        durationMs: Long,
        difficulty: Difficulty
    ): LessonCompletionResult

    suspend fun recordGameScore(gameId: GameId, score: Int)

    suspend fun addCoins(amount: Int)

    suspend fun addXp(amount: Int)

    /** Buys an unlockable with coins. Returns false when the child cannot afford it yet. */
    suspend fun purchase(item: Unlockable): Boolean

    suspend fun isUnlocked(itemId: String): Boolean

    suspend fun claimDailyReward(): DailyRewardResult?

    suspend fun addPlaySeconds(seconds: Int)

    suspend fun setCurrentLevel(levelId: Int)

    suspend fun markIntroSeen()

    suspend fun resetProgress()

    suspend fun unlockedItems(): List<Unlockable>
}
