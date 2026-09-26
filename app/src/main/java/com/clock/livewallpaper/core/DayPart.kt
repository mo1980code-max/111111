package com.clock.livewallpaper.core

import java.util.Calendar

/** Which half of the day the home hero and the "ذكر اليوم" rotation follow. */
enum class DayPart { MORNING, EVENING }

/**
 * Morning runs from 04:00 until 15:59 local time; the rest of the day is evening. Local time only,
 * no location and no network are involved.
 */
fun currentDayPart(calendar: Calendar = Calendar.getInstance()): DayPart =
    if (calendar.get(Calendar.HOUR_OF_DAY) in 4..15) DayPart.MORNING else DayPart.EVENING
