package com.clock.livewallpaper.core

import android.content.Context
import com.clock.livewallpaper.R
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Offline tabular Hijri date.
 *
 * Ported unchanged (same epoch, same arithmetic) from the verified implementation the project
 * already shipped, so the displayed Hijri date keeps the behaviour it had before: no network, no
 * second implementation, and a value that rolls over naturally after local midnight.
 */
data class HijriDate(val year: Int, val month: Int, val day: Int) {

    /** Month names and the surrounding format live in res/values/strings.xml, like all copy. */
    fun formatArabic(context: Context): String {
        val months = context.resources.getStringArray(R.array.hijri_months)
        val monthName = months.getOrElse(month - 1) { months.first() }
        return context.getString(
            R.string.hijri_date_format,
            day.toString(),
            monthName,
            year.toString()
        )
    }

    companion object {
        // Integer Julian-day form of 1 Muharram 1 AH. The +1 keeps civil dates aligned with the
        // conventional tabular result (for example, 2024-04-10 is 1 Shawwal 1445).
        private const val ISLAMIC_EPOCH = 1948440

        fun from(source: Calendar = Calendar.getInstance()): HijriDate {
            val date = source.clone() as Calendar
            date.set(Calendar.HOUR_OF_DAY, 12)
            date.set(Calendar.MINUTE, 0)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)
            val julianDay = gregorianToJulianDay(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH)
            )
            val year = (30 * (julianDay - ISLAMIC_EPOCH) + 10646) / 10631
            val month = min(
                12,
                ceil((julianDay - (29 + islamicToJulianDay(year, 1, 1))) / 29.5).toInt() + 1
            )
            val day = julianDay - islamicToJulianDay(year, month, 1) + 1
            return HijriDate(
                year = year,
                month = max(1, min(12, month)),
                day = max(1, min(30, day))
            )
        }

        private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + (12 * a) - 3
            return day + ((153 * m + 2) / 5) + (365 * y) + (y / 4) - (y / 100) + (y / 400) - 32045
        }

        private fun islamicToJulianDay(year: Int, month: Int, day: Int): Int =
            day + ceil(29.5 * (month - 1)).toInt() + ((year - 1) * 354) +
                ((3 + (11 * year)) / 30) + ISLAMIC_EPOCH - 1
    }
}
