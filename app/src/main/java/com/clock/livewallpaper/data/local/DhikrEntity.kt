package com.clock.livewallpaper.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One dhikr row.
 *
 * Seeded rows (isDefault = true) carry the Arabic text exactly as it is stored in
 * assets/azkar.json; nothing in the app rewrites, normalises or "corrects" that text. [seedKey]
 * is the stable identity of a seeded row ("morning:12") and is unique, which makes seeding
 * idempotent: re-running it can never duplicate or overwrite an existing row.
 */
@Entity(
    tableName = "dhikr",
    indices = [
        Index(value = ["seedKey"], unique = true),
        Index(value = ["category"]),
        Index(value = ["isEnabled", "includeInOverlay"])
    ]
)
data class DhikrEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Verified Arabic text. Stored and displayed verbatim. */
    val arabicText: String,

    /** One of [DhikrCategory.key]. */
    val category: String,

    val repeatCount: Int = 1,

    /** Transmitted virtue, when the source provides one. Never generated. */
    val virtue: String? = null,

    /** Human readable source label, when the data provides one. */
    val sourceName: String? = null,

    /** Exact reference/URL recorded with the content. Never generated. */
    val sourceReference: String? = null,

    /** Position inside its category, as ordered by the source. */
    val orderIndex: Int = 0,

    /** True for rows that came from the bundled verified content. */
    val isDefault: Boolean = false,

    /** User switch: take part in reminders and reading lists. */
    val isEnabled: Boolean = true,

    /** User switch: may appear inside the floating dhikr card. */
    val includeInOverlay: Boolean = true,

    /** Stable seed identity, null for user-created dhikr. */
    val seedKey: String? = null,

    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L
)
