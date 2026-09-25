package com.clockadventure.data.di

import android.content.Context
import androidx.room.Room
import com.clockadventure.data.db.AppDatabase
import com.clockadventure.data.db.AchievementDao
import com.clockadventure.data.db.DailyStatDao
import com.clockadventure.data.db.GameScoreDao
import com.clockadventure.data.db.LessonProgressDao
import com.clockadventure.data.db.UnlockDao
import com.clockadventure.data.db.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt wiring for the Room database. Everything is a singleton: one file, one connection. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Local learning progress only: rebuilding the file is harmless, a crash is not.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideLessonProgressDao(database: AppDatabase): LessonProgressDao = database.lessonProgressDao()

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao = database.achievementDao()

    @Provides
    fun provideUnlockDao(database: AppDatabase): UnlockDao = database.unlockDao()

    @Provides
    fun provideDailyStatDao(database: AppDatabase): DailyStatDao = database.dailyStatDao()

    @Provides
    fun provideGameScoreDao(database: AppDatabase): GameScoreDao = database.gameScoreDao()
}
