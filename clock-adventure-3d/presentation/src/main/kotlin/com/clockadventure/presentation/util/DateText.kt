package com.clockadventure.presentation.util

import java.time.LocalDate

/** Small helpers around the ISO date keys used by the database. */
object DateText {

    fun today(): LocalDate = LocalDate.now()

    fun isToday(key: String?): Boolean = key != null && runCatching { LocalDate.parse(key) == LocalDate.now() }.getOrDefault(false)

    /** "2026-09-25" -> "Thu" style short label for the parent chart. */
    fun shortDay(key: String): String = runCatching {
        LocalDate.parse(key).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    }.getOrDefault(key.takeLast(2))

    fun dayOfMonth(key: String): String = runCatching { LocalDate.parse(key).dayOfMonth.toString() }.getOrDefault("?")
}
