package com.clock.livewallpaper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Row count per category, used by the category cards. */
data class CategoryCount(val category: String, val total: Int)

@Dao
interface DhikrDao {

    @Query("SELECT * FROM dhikr WHERE category = :category ORDER BY orderIndex ASC, id ASC")
    fun observeCategory(category: String): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE isDefault = 0 ORDER BY createdAt DESC, id DESC")
    fun observeUserDhikr(): Flow<List<DhikrEntity>>

    @Query("SELECT category AS category, COUNT(*) AS total FROM dhikr GROUP BY category")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM dhikr WHERE category = :category ORDER BY orderIndex ASC, id ASC")
    suspend fun byCategory(category: String): List<DhikrEntity>

    @Query("SELECT * FROM dhikr WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): DhikrEntity?

    @Query("SELECT * FROM dhikr WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<DhikrEntity?>

    /**
     * Candidates for the floating card / notification fallback: enabled, marked for the overlay and
     * short enough to stay readable inside a small card.
     */
    @Query(
        "SELECT * FROM dhikr WHERE isEnabled = 1 AND includeInOverlay = 1 " +
            "AND LENGTH(arabicText) <= :maxLength ORDER BY id ASC"
    )
    suspend fun overlayCandidates(maxLength: Int): List<DhikrEntity>

    @Query("SELECT * FROM dhikr WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun enabled(): List<DhikrEntity>

    @Query("SELECT COUNT(*) FROM dhikr")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM dhikr WHERE isDefault = 1")
    suspend fun seededCount(): Int

    /** IGNORE keeps seeding idempotent: an existing seedKey is never overwritten. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoring(items: List<DhikrEntity>): List<Long>

    @Insert
    suspend fun insert(item: DhikrEntity): Long

    @Update
    suspend fun update(item: DhikrEntity)

    @Delete
    suspend fun delete(item: DhikrEntity)

    /** Seeded rows are protected from deletion; only user rows can be removed. */
    @Query("DELETE FROM dhikr WHERE id = :id AND isDefault = 0")
    suspend fun deleteUserDhikr(id: Long): Int

    @Query("UPDATE dhikr SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE dhikr SET includeInOverlay = :include WHERE id = :id")
    suspend fun setIncludeInOverlay(id: Long, include: Boolean)
}
