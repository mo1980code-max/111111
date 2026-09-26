package com.clockadventure.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities for everything the app remembers about the child.
 *
 * Only primitives and strings are stored: no type converters, no relations, no embedded objects.
 * That keeps the schema trivially readable (and exportable for a parent) and lets the whole
 * database live in a single file that never needs a migration story for v1.
 */

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val xp: Int = 0,
    val coins: Int = 0,
    val stars: Int = 0,
    @ColumnInfo(name = "total_correct") val totalCorrect: Int = 0,
    @ColumnInfo(name = "total_wrong") val totalWrong: Int = 0,
    @ColumnInfo(name = "total_play_seconds") val totalPlaySeconds: Int = 0,
    @ColumnInfo(name = "lessons_completed") val lessonsCompleted: Int = 0,
    @ColumnInfo(name = "current_level") val currentLevel: Int = 1,
    @ColumnInfo(name = "flawless_lessons") val flawlessLessons: Int = 0,
    @ColumnInfo(name = "days_played") val daysPlayed: Int = 0,
    @ColumnInfo(name = "routine_correct") val routineCorrect: Int = 0,
    @ColumnInfo(name = "fast_correct") val fastCorrect: Int = 0,
    @ColumnInfo(name = "streak_days") val streakDays: Int = 0,
    @ColumnInfo(name = "last_active_date") val lastActiveDate: String? = null,
    @ColumnInfo(name = "last_daily_reward_date") val lastDailyRewardDate: String? = null,
    @ColumnInfo(name = "parent_verified") val parentVerified: Boolean = false,
    @ColumnInfo(name = "intro_seen") val introSeen: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val levelId: Int,
    @ColumnInfo(name = "best_stars") val bestStars: Int = 0,
    val completions: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    @ColumnInfo(name = "play_seconds") val playSeconds: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long = 0L
)

@Entity(tableName = "unlocks")
data class UnlockEntity(
    @PrimaryKey val itemId: String,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long = 0L
)

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val dateKey: String,
    @ColumnInfo(name = "seconds_learned") val secondsLearned: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val sessions: Int = 0
)

@Entity(tableName = "game_scores")
data class GameScoreEntity(
    @PrimaryKey val gameId: String,
    @ColumnInfo(name = "best_score") val bestScore: Int = 0,
    val plays: Int = 0
)
