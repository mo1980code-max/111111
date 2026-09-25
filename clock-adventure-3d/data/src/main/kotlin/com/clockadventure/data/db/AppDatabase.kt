package com.clockadventure.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The single offline database of the app.
 *
 * `exportSchema` stays false: this is a child's local progress file, there is no server to sync
 * with and no need to keep historical schema dumps in the repository.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LessonProgressEntity::class,
        AchievementEntity::class,
        UnlockEntity::class,
        DailyStatEntity::class,
        GameScoreEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun unlockDao(): UnlockDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun gameScoreDao(): GameScoreDao

    companion object {
        const val DATABASE_NAME = "clock_adventure.db"
    }
}
