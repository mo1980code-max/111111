package com.clockadventure.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observe(id: Int = UserProfileEntity.SINGLETON_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun get(id: Int = UserProfileEntity.SINGLETON_ID): UserProfileEntity?

    @Upsert
    suspend fun upsert(entity: UserProfileEntity)

    @Query("UPDATE user_profile SET xp = xp + :amount WHERE id = :id")
    suspend fun addXp(amount: Int, id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET coins = coins + :amount WHERE id = :id")
    suspend fun addCoins(amount: Int, id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET coins = coins - :amount WHERE id = :id AND coins >= :amount")
    suspend fun spendCoins(amount: Int, id: Int = UserProfileEntity.SINGLETON_ID): Int

    @Query("UPDATE user_profile SET total_play_seconds = total_play_seconds + :seconds WHERE id = :id")
    suspend fun addPlaySeconds(seconds: Int, id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET current_level = :levelId WHERE id = :id")
    suspend fun setCurrentLevel(levelId: Int, id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET parent_verified = :verified WHERE id = :id")
    suspend fun setParentVerified(verified: Boolean, id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET intro_seen = 1 WHERE id = :id")
    suspend fun markIntroSeen(id: Int = UserProfileEntity.SINGLETON_ID)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress ORDER BY levelId ASC")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAll(): List<LessonProgressEntity>

    @Query("SELECT * FROM lesson_progress WHERE levelId = :levelId")
    suspend fun get(levelId: Int): LessonProgressEntity?

    @Upsert
    suspend fun upsert(entity: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun clear()
}

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity)

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun count(): Int

    @Query("DELETE FROM achievements")
    suspend fun clear()
}

@Dao
interface UnlockDao {

    @Query("SELECT * FROM unlocks")
    fun observeAll(): Flow<List<UnlockEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: UnlockEntity)

    @Query("SELECT * FROM unlocks")
    suspend fun getAll(): List<UnlockEntity>

    @Query("SELECT COUNT(*) FROM unlocks WHERE itemId = :itemId")
    suspend fun count(itemId: String): Int

    @Query("DELETE FROM unlocks")
    suspend fun clear()
}

@Dao
interface DailyStatDao {

    @Query("SELECT * FROM daily_stats ORDER BY dateKey DESC LIMIT :days")
    fun observeRecent(days: Int): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey")
    fun observe(dateKey: String): Flow<DailyStatEntity?>

    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey")
    suspend fun get(dateKey: String): DailyStatEntity?

    @Upsert
    suspend fun upsert(entity: DailyStatEntity)

    @Query("UPDATE daily_stats SET sessions = sessions + 1 WHERE dateKey = :dateKey")
    suspend fun addSession(dateKey: String)

    @Query("DELETE FROM daily_stats")
    suspend fun clear()
}

@Dao
interface GameScoreDao {

    @Query("SELECT * FROM game_scores")
    fun observeAll(): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE gameId = :gameId")
    suspend fun get(gameId: String): GameScoreEntity?

    @Upsert
    suspend fun upsert(entity: GameScoreEntity)

    @Query("DELETE FROM game_scores")
    suspend fun clear()
}
