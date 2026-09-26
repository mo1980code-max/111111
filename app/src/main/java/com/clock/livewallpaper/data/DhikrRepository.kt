package com.clock.livewallpaper.data

import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.data.local.DhikrDao
import com.clock.livewallpaper.data.local.DhikrEntity
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.data.seed.AzkarSeedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

sealed interface DhikrSaveResult {
    data class Saved(val id: Long) : DhikrSaveResult
    data object BlankText : DhikrSaveResult
    data object InvalidRepeat : DhikrSaveResult
}

/**
 * The single door to dhikr content: seeding, reading lists, reminder selection and user CRUD.
 */
@Singleton
class DhikrRepository @Inject constructor(
    private val dao: DhikrDao,
    private val seedSource: AzkarSeedSource,
    private val settings: SettingsRepository
) {

    /**
     * Idempotent seed. Runs only while the stored seed version is behind the bundled content and
     * inserts with IGNORE on the unique seedKey, so a dhikr the user edited, disabled or deleted is
     * never resurrected or overwritten, and user-created rows are never touched.
     */
    suspend fun ensureSeeded() {
        if (settings.seedVersion() >= AzkarSeedSource.SEED_VERSION) return
        val rows = seedSource.load()
        if (rows.isNotEmpty()) {
            dao.insertAllIgnoring(rows)
        }
        settings.setSeedVersion(AzkarSeedSource.SEED_VERSION)
    }

    fun observeCategory(category: DhikrCategory): Flow<List<DhikrEntity>> =
        dao.observeCategory(category.key)

    fun observeUserDhikr(): Flow<List<DhikrEntity>> = dao.observeUserDhikr()

    fun observeCategoryCounts(): Flow<Map<String, Int>> =
        dao.observeCategoryCounts().map { counts -> counts.associate { it.category to it.total } }

    fun observeById(id: Long): Flow<DhikrEntity?> = dao.observeById(id)

    suspend fun byId(id: Long): DhikrEntity? = dao.byId(id)

    suspend fun categoryItems(category: DhikrCategory): List<DhikrEntity> =
        dao.byCategory(category.key)

    /**
     * Picks the dhikr for a reminder / widget rotation, avoiding what was shown recently while the
     * pool is large enough to allow it. Works with a two-row database as well as a full one.
     */
    suspend fun pickForReminder(): DhikrEntity? {
        val candidates = dao.overlayCandidates(AzkarSeedSource.OVERLAY_TEXT_LIMIT)
            .ifEmpty { dao.enabled() }
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val recent = settings.recentDhikrIds().toSet()
        val fresh = candidates.filterNot { recent.contains(it.id) }
        val pool = if (fresh.isNotEmpty()) fresh else candidates
        val picked = pool.random()
        settings.pushRecentDhikrId(picked.id, (candidates.size - 1).coerceIn(1, RECENT_HISTORY))
        return picked
    }

    /** "ذكر اليوم" - stable for the whole local day, no randomness between recompositions. */
    suspend fun dailyDhikr(calendar: Calendar = Calendar.getInstance()): DhikrEntity? {
        val pool = dao.overlayCandidates(AzkarSeedSource.OVERLAY_TEXT_LIMIT)
            .ifEmpty { dao.enabled() }
        if (pool.isEmpty()) return null
        val key = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        return pool[abs(key) % pool.size]
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun setIncludeInOverlay(id: Long, include: Boolean) = dao.setIncludeInOverlay(id, include)

    /**
     * Creates or updates a dhikr.
     *
     * The text of a seeded (verified) row is protected: for those rows only the user switches are
     * written back, the Arabic text, repetition count, virtue and source stay exactly as bundled.
     */
    suspend fun save(
        id: Long?,
        text: String,
        repeatCount: Int,
        category: DhikrCategory,
        isEnabled: Boolean,
        includeInOverlay: Boolean
    ): DhikrSaveResult {
        val existing = id?.let { dao.byId(it) }

        if (existing != null && existing.isDefault) {
            dao.update(
                existing.copy(isEnabled = isEnabled, includeInOverlay = includeInOverlay)
            )
            return DhikrSaveResult.Saved(existing.id)
        }

        // Only surrounding whitespace is removed; the dhikr the user typed is stored as typed.
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return DhikrSaveResult.BlankText
        if (repeatCount < MIN_REPEAT || repeatCount > MAX_REPEAT) return DhikrSaveResult.InvalidRepeat

        return if (existing == null) {
            val newId = dao.insert(
                DhikrEntity(
                    arabicText = cleaned,
                    category = category.key,
                    repeatCount = repeatCount,
                    isDefault = false,
                    isEnabled = isEnabled,
                    includeInOverlay = includeInOverlay,
                    orderIndex = 0,
                    seedKey = null,
                    createdAt = System.currentTimeMillis()
                )
            )
            DhikrSaveResult.Saved(newId)
        } else {
            dao.update(
                existing.copy(
                    arabicText = cleaned,
                    category = category.key,
                    repeatCount = repeatCount,
                    isEnabled = isEnabled,
                    includeInOverlay = includeInOverlay
                )
            )
            DhikrSaveResult.Saved(existing.id)
        }
    }

    /** Seeded rows are protected: the query only deletes user-created dhikr. */
    suspend fun deleteUserDhikr(id: Long): Boolean = dao.deleteUserDhikr(id) > 0

    /** Short label used by the widget and the notification fallback. */
    fun shortText(entity: DhikrEntity, limit: Int = WIDGET_TEXT_LIMIT): String {
        val single = entity.arabicText.replace('\n', ' ').replace("  ", " ")
        return if (single.length <= limit) single else single.take(limit).trimEnd() + "…"
    }

    fun repeatLabel(entity: DhikrEntity): String = ArabicText.digits(entity.repeatCount)

    companion object {
        const val MIN_REPEAT = 1
        const val MAX_REPEAT = 1000
        const val RECENT_HISTORY = 12
        const val WIDGET_TEXT_LIMIT = 160
    }
}
