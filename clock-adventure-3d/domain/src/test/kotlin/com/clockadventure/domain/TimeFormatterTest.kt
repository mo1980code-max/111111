package com.clockadventure.domain

import com.clockadventure.domain.engine.TimeFormatter
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.ClockTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Digital display and the sentences handed to the text-to-speech engine. */
class TimeFormatterTest {

    @Test
    fun `24 hour format pads the hour`() {
        assertEquals("07:05", TimeFormatter.digital(ClockTime.of12(7, 5, false), true, AppLanguage.ENGLISH))
        assertEquals("19:30", TimeFormatter.digital(ClockTime.of12(7, 30, true), true, AppLanguage.ENGLISH))
    }

    @Test
    fun `12 hour format adds am and pm in both languages`() {
        assertEquals("7:05 AM", TimeFormatter.digital(ClockTime.of12(7, 5, false), false, AppLanguage.ENGLISH))
        assertEquals("7:30 PM", TimeFormatter.digital(ClockTime.of12(7, 30, true), false, AppLanguage.ENGLISH))
        assertEquals("7:05 ص", TimeFormatter.digital(ClockTime.of12(7, 5, false), false, AppLanguage.ARABIC))
        assertEquals("7:30 م", TimeFormatter.digital(ClockTime.of12(7, 30, true), false, AppLanguage.ARABIC))
    }

    @Test
    fun `the clock face readout has no am or pm`() {
        assertEquals("12:00", TimeFormatter.face(ClockTime.of12(12, 0, false)))
        assertEquals("8:05", TimeFormatter.face(ClockTime.of12(8, 5, false)))
    }

    @Test
    fun `english speech uses natural time words`() {
        assertEquals("seven o'clock", TimeFormatter.spoken(ClockTime.of12(7, 0, false), AppLanguage.ENGLISH))
        assertEquals("quarter past seven", TimeFormatter.spoken(ClockTime.of12(7, 15, false), AppLanguage.ENGLISH))
        assertEquals("half past seven", TimeFormatter.spoken(ClockTime.of12(7, 30, false), AppLanguage.ENGLISH))
        assertEquals("quarter to eight", TimeFormatter.spoken(ClockTime.of12(7, 45, false), AppLanguage.ENGLISH))
        assertEquals("ten past seven", TimeFormatter.spoken(ClockTime.of12(7, 10, false), AppLanguage.ENGLISH))
        assertEquals("seven forty-two", TimeFormatter.spoken(ClockTime.of12(7, 42, false), AppLanguage.ENGLISH))
    }

    @Test
    fun `arabic speech uses natural time words`() {
        assertEquals("الساعة السابعة تماماً", TimeFormatter.spoken(ClockTime.of12(7, 0, false), AppLanguage.ARABIC))
        assertEquals("الساعة السابعة والربع", TimeFormatter.spoken(ClockTime.of12(7, 15, false), AppLanguage.ARABIC))
        assertEquals("الساعة السابعة والنصف", TimeFormatter.spoken(ClockTime.of12(7, 30, false), AppLanguage.ARABIC))
        assertEquals("الساعة الثامنة إلا ربع", TimeFormatter.spoken(ClockTime.of12(7, 45, false), AppLanguage.ARABIC))
        assertTrue(TimeFormatter.spoken(ClockTime.of12(7, 20, false), AppLanguage.ARABIC).contains("عشرون"))
    }
}
