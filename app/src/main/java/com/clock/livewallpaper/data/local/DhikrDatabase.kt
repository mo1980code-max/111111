package com.clock.livewallpaper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Local database, version 1.
 *
 * There is no destructive fallback anywhere in the build chain: user-created dhikr and the
 * enabled/overlay switches of the seeded rows are user data. Schemas are exported to
 * app/schemas, so the next version bump ships a real [Migration] appended to [MIGRATIONS].
 */
@Database(
    entities = [DhikrEntity::class],
    version = DhikrDatabase.VERSION,
    exportSchema = true
)
abstract class DhikrDatabase : RoomDatabase() {

    abstract fun dhikrDao(): DhikrDao

    companion object {
        const val VERSION = 1
        const val NAME = "dhikr.db"

        /** Empty at version 1 - every future version appends its migration here. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
