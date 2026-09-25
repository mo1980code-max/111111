package com.clockadventure.data.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Day keys used by the daily statistics and the daily reward.
 *
 * `java.time` is available from API 26 (the app's minSdk) so no desugaring is needed.
 */
object DateKeys {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(FORMATTER)

    fun daysAgo(days: Int): String = LocalDate.now().minusDays(days.toLong()).format(FORMATTER)

    fun isToday(dateKey: String?): Boolean = dateKey != null && dateKey == today()

    fun isYesterday(dateKey: String?): Boolean = dateKey != null && dateKey == daysAgo(1)

    /** Oldest first, today last - the order the parent dashboard bars are drawn in. */
    fun recentKeys(days: Int): List<String> =
        (days - 1 downTo 0).map { daysAgo(it) }
}
