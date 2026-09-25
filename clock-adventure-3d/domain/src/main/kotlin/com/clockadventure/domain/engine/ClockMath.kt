package com.clockadventure.domain.engine

import com.clockadventure.domain.model.ClockTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Geometry of the analog clock face.
 *
 * Angles in this file are **clock degrees**: 0° is 12 o'clock and they grow clockwise, which is
 * what teachers use. The UI converts them to canvas degrees with [clockAngleToCanvasDegrees].
 */
object ClockMath {

    /** The current wall clock time, used by the home screen clock. */
    fun now(): ClockTime {
        val time = java.time.LocalTime.now()
        return ClockTime(hour = time.hour, minute = time.minute)
    }

    const val DEGREES_PER_HOUR = 30f
    const val DEGREES_PER_MINUTE = 6f

    fun normalizeDegrees(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }

    /** Canvas `rotate()` measures from the +x axis, so 12 o'clock is at -90°. */
    fun clockAngleToCanvasDegrees(clockAngle: Float): Float = clockAngle - 90f

    /** Where a pointer sits on the face, in clock degrees (0 = top, clockwise). */
    fun angleOfPoint(centerX: Float, centerY: Float, x: Float, y: Float): Float {
        val dx = x - centerX
        val dy = y - centerY
        val radians = atan2(dx, -dy)
        return normalizeDegrees(Math.toDegrees(radians.toDouble()).toFloat())
    }

    /** End point of a hand of the given length drawn at a clock angle. */
    fun handTip(centerX: Float, centerY: Float, length: Float, clockAngle: Float): Pair<Float, Float> {
        val radians = Math.toRadians(clockAngleToCanvasDegrees(clockAngle).toDouble())
        return Pair(
            centerX + (length * cos(radians)).toFloat(),
            centerY + (length * sin(radians)).toFloat()
        )
    }

    /** Nearest minute to an angle (0..59). */
    fun minuteFromAngle(clockAngle: Float): Int =
        ((normalizeDegrees(clockAngle) / DEGREES_PER_MINUTE).roundToInt()) % 60

    /** Nearest hour number (1..12) to an angle. */
    fun hourNumberFromAngle(clockAngle: Float): Int {
        val index = ((normalizeDegrees(clockAngle) / DEGREES_PER_HOUR).roundToInt()) % 12
        return if (index == 0) 12 else index
    }

    /**
     * Rounds a minute value to the closest allowed step (60 = full hours, 30 = half hours,
     * 15 = quarters, 5 = five minute steps, 1 = no snapping).
     */
    fun snapMinute(minute: Int, step: Int): Int {
        if (step <= 1) return minute.coerceIn(0, 59)
        val snapped = ((minute.toFloat() / step).roundToInt() * step) % 60
        return snapped
    }

    /** Rounds an hour angle to the closest hour number, keeping the minute part of the hour hand. */
    fun snapHourWithMinute(hourNumber: Int, minute: Int, step: Int): ClockTime {
        val snappedMinute = snapMinute(minute, step)
        return ClockTime.of12(hourNumber, snappedMinute, pm = false)
    }

    /** The hour hand angle for a given hour number and minute (smooth, like a real clock). */
    fun hourHandAngle(hourNumber: Int, minute: Int): Float =
        ((hourNumber % 12) * DEGREES_PER_HOUR) + (minute * 0.5f)

    fun minuteHandAngle(minute: Int): Float = minute * DEGREES_PER_MINUTE

    /**
     * Builds a time from where the child dragged the hands.
     * [hourNumber] is the number the short hand points at (1..12), [minute] the raw minute.
     */
    fun timeFromHands(hourNumber: Int, minute: Int, snapStep: Int): ClockTime =
        ClockTime.of12(hourNumber, snapMinute(minute, snapStep), pm = false)
}
