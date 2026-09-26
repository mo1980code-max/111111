package com.clock.livewallpaper.data.seed

import android.content.Context
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.data.local.DhikrEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the bundled verified content (assets/azkar.json) into seed rows.
 *
 * Religious-content rules enforced here:
 *  - the Arabic text, the per-item repetition count, the order and the virtue are copied VERBATIM;
 *    nothing is trimmed, normalised, reflowed, paraphrased or generated;
 *  - the source URL recorded with each section travels with every row, so "المصدر" in the reader
 *    always shows the real attribution;
 *  - an item without text is skipped rather than replaced by invented content.
 */
@Singleton
class AzkarSeedSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun load(): List<DhikrEntity> {
        val raw = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return emptyList()

        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val sources = root.optJSONObject("sources")
        val rows = ArrayList<DhikrEntity>()

        for (category in SEEDED_CATEGORIES) {
            val array = root.optJSONArray(category.key) ?: continue
            val reference = sources?.optString(category.key)?.takeIf { it.isNotBlank() }
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("text")
                if (text.isBlank()) continue
                val id = item.optInt("id", index + 1)
                val virtue = item.optString("virtue").takeIf { it.isNotBlank() }
                rows += DhikrEntity(
                    arabicText = text,
                    category = category.key,
                    repeatCount = item.optInt("repeat", 1).coerceAtLeast(1),
                    virtue = virtue,
                    sourceName = reference?.let { sourceLabel(it) },
                    sourceReference = reference,
                    orderIndex = item.optInt("order", index + 1),
                    isDefault = true,
                    isEnabled = true,
                    includeInOverlay = text.length <= OVERLAY_TEXT_LIMIT,
                    seedKey = "${category.key}:$id",
                    createdAt = 0L
                )
            }
        }
        return rows
    }

    /** Host of the recorded source URL - derived mechanically, never invented. */
    private fun sourceLabel(reference: String): String? {
        val withoutScheme = reference.substringAfter("://", reference)
        val host = withoutScheme.substringBefore('/')
        return host.removePrefix("www.").takeIf { it.isNotBlank() }
    }

    companion object {
        const val ASSET_NAME = "azkar.json"

        /** Bumped only when the bundled content file itself changes. */
        const val SEED_VERSION = 1

        /**
         * A dhikr longer than this stays out of the floating card and the widget by default -
         * a display decision about which verified text fits a small card, never an edit of it.
         */
        const val OVERLAY_TEXT_LIMIT = 180

        private val SEEDED_CATEGORIES = listOf(
            DhikrCategory.MORNING,
            DhikrCategory.EVENING,
            DhikrCategory.TASBEEH
        )
    }
}
