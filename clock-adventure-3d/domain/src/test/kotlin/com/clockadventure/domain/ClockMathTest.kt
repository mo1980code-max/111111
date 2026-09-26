package com.clockadventure.domain

import com.clockadventure.domain.engine.ClockMath
import com.clockadventure.domain.model.ClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geometry of the analog clock face. */
class ClockMathTest {

    @Test
    fun `angle of a point is measured clockwise from twelve o'clock`() {
        assertEquals(0f, ClockMath.angleOfPoint(100f, 100f, 100f, 0f))
        assertEquals(90f, ClockMath.angleOfPoint(100f, 100f, 200f, 100f))
        assertEquals(180f, ClockMath.angleOfPoint(100f, 100f, 100f, 200f))
        assertEquals(270f, ClockMath.angleOfPoint(100f, 100f, 0f, 100f))
    }

    @Test
    fun `angles are normalized into the 0 to 360 range`() {
        assertEquals(0f, ClockMath.normalizeDegrees(360f))
        assertEquals(10f, ClockMath.normalizeDegrees(-350f))
        assertEquals(359f, ClockMath.normalizeDegrees(359f))
    }

    @Test
    fun `minutes are read back from an angle`() {
        assertEquals(0, ClockMath.minuteFromAngle(0f))
        assertEquals(15, ClockMath.minuteFromAngle(90f))
        assertEquals(30, ClockMath.minuteFromAngle(180f))
        assertEquals(45, ClockMath.minuteFromAngle(270f))
    }

    @Test
    fun `hour numbers are read back from an angle`() {
        assertEquals(12, ClockMath.hourNumberFromAngle(0f))
        assertEquals(3, ClockMath.hourNumberFromAngle(90f))
        assertEquals(6, ClockMath.hourNumberFromAngle(180f))
        assertEquals(9, ClockMath.hourNumberFromAngle(270f))
    }

    @Test
    fun `minute snapping respects the difficulty step`() {
        assertEquals(0, ClockMath.snapMinute(0, 60))
        assertEquals(0, ClockMath.snapMinute(29, 60))
        assertEquals(60 % 60, ClockMath.snapMinute(31, 60))
        assertEquals(30, ClockMath.snapMinute(22, 30))
        assertEquals(15, ClockMath.snapMinute(13, 15))
        assertEquals(25, ClockMath.snapMinute(24, 5))
        assertEquals(37, ClockMath.snapMinute(37, 1))
    }

    @Test
    fun `hour hand moves with the minutes`() {
        assertEquals(0f, ClockMath.hourHandAngle(12, 0))
        assertEquals(15f, ClockMath.hourHandAngle(12, 30))
        assertEquals(97.5f, ClockMath.hourHandAngle(3, 15))
    }

    @Test
    fun `a dragged hand produces a valid time`() {
        val time = ClockMath.timeFromHands(hourNumber = 7, minute = 32, snapStep = 5)
        assertEquals(ClockTime.of12(7, 30, pm = false), time)
        assertTrue(time.minute % 5 == 0)
    }

    @Test
    fun `hand tip lands where the angle says`() {
        val (x, y) = ClockMath.handTip(0f, 0f, 10f, 0f)
        assertTrue("x was $x", kotlin.math.abs(x) < 0.001f)
        assertTrue("y was $y", kotlin.math.abs(y + 10f) < 0.001f)

        val (rightX, rightY) = ClockMath.handTip(0f, 0f, 10f, 90f)
        assertTrue("x was $rightX", kotlin.math.abs(rightX - 10f) < 0.001f)
        assertTrue("y was $rightY", kotlin.math.abs(rightY) < 0.001f)
    }
}
