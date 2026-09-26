package com.clockadventure.domain.model

/**
 * A wall-clock time.
 *
 * [hour] is stored in 24 hour form (0..23) so that the 12/24 hour display formats can both be
 * derived without losing the AM/PM information. [minute] is 0..59.
 */
data class ClockTime(val hour: Int, val minute: Int) {

    init {
        require(hour in 0..23) { "hour must be in 0..23 but was $hour" }
        require(minute in 0..59) { "minute must be in 0..59 but was $minute" }
    }

    /** 1..12 as printed on a clock face. */
    val hour12: Int
        get() {
            val h = hour % 12
            return if (h == 0) 12 else h
        }

    val isPm: Boolean get() = hour >= 12

    val totalMinutes: Int get() = hour * 60 + minute

    /** Angle of the minute hand in degrees, 0° = 12 o'clock, clockwise. */
    val minuteAngle: Float get() = minute * 6f

    /** Angle of the hour hand in degrees, including the smooth shift driven by the minutes. */
    val hourAngle: Float get() = (hour % 12) * 30f + minute * 0.5f

    /** The number the hour hand points at (1..12) - used by the "tap the number" questions. */
    val hourNumber: Int get() = hour12

    /** The number the minute hand points at (1..12, 12 for :00) - used by the same questions. */
    val minuteNumber: Int
        get() {
            val index = minute / 5
            return if (index == 0) 12 else index
        }

    fun isFullHour(): Boolean = minute == 0
    fun isHalfHour(): Boolean = minute == 30
    fun isQuarterPast(): Boolean = minute == 15
    fun isQuarterTo(): Boolean = minute == 45

    companion object {
        val MIDNIGHT = ClockTime(0, 0)
        val NOON = ClockTime(12, 0)

        fun fromTotalMinutes(total: Int): ClockTime {
            val normalized = ((total % 1440) + 1440) % 1440
            return ClockTime(normalized / 60, normalized % 60)
        }

        /** Builds a time from a 12 hour face value (1..12) plus AM/PM. */
        fun of12(hour12: Int, minute: Int, pm: Boolean): ClockTime {
            require(hour12 in 1..12) { "hour12 must be in 1..12 but was $hour12" }
            val hour24 = when {
                pm -> if (hour12 == 12) 12 else hour12 + 12
                else -> if (hour12 == 12) 0 else hour12
            }
            return ClockTime(hour24, minute)
        }
    }
}
