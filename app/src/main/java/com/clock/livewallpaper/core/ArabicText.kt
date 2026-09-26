package com.clock.livewallpaper.core

import android.content.Context
import com.clock.livewallpaper.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Arabic number / date / time formatting used by every screen, the widgets and the notifications.
 *
 * Counters, dates and times are rendered with Arabic-Indic digits so a screen never mixes two
 * numeral systems. Digits themselves stay in their natural left-to-right order - only the glyphs
 * change - so "12 / 25" reads as "١٢ / ٢٥" and never gets reversed.
 */
object ArabicText {

    private val ARABIC_INDIC = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Arabic locale used for month / day names; the app ships Arabic-only UI copy. */
    val LOCALE: Locale = Locale("ar")

    fun digits(value: Int): String = digits(value.toString())

    fun digits(value: Long): String = digits(value.toString())

    fun digits(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (c in '0'..'9') {
                chars[i] = ARABIC_INDIC[c - '0']
            }
        }
        return String(chars)
    }

    /**
     * Turns whatever digits a keyboard produced - ASCII, Arabic-Indic (٠-٩) or Eastern
     * Arabic-Indic (۰-۹) - into ASCII, and drops everything else. Input fields accept all three.
     */
    fun toLatinDigits(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { c ->
            when (c) {
                in '0'..'9' -> builder.append(c)
                in '\u0660'..'\u0669' -> builder.append(('0' + (c - '\u0660')))
                in '\u06F0'..'\u06F9' -> builder.append(('0' + (c - '\u06F0')))
            }
        }
        return builder.toString()
    }

    /** Parses a user-typed number written with any of the supported digit sets. */
    fun parseNumber(text: String): Int? = toLatinDigits(text).takeIf { it.isNotEmpty() }?.let {
        // Nine digits at most: longer input is a typing accident, not a count.
        if (it.length > 9) null else it.toIntOrNull()
    }

    /** "١١:٠٠ م" - 12 hour clock with an Arabic meridiem, safe for RTL layout. */
    fun time(context: Context, minuteOfDay: Int): String {
        val normalized = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val hour24 = normalized / 60
        val minute = normalized % 60
        val meridiem = context.getString(if (hour24 < 12) R.string.time_am else R.string.time_pm)
        var hour12 = hour24 % 12
        if (hour12 == 0) hour12 = 12
        val minuteText = digits(if (minute < 10) "0$minute" else minute.toString())
        return context.getString(R.string.time_format, digits(hour12), minuteText, meridiem)
    }

    /** Duration label used by the reminder chips: "٥ د", "٤٥ د", "ساعة", "٣ ساعات". */
    fun duration(context: Context, totalMinutes: Int): String {
        val minutes = totalMinutes.coerceAtLeast(1)
        if (minutes < 60) return context.getString(R.string.duration_minutes, digits(minutes))
        val hours = minutes / 60
        val rest = minutes % 60
        val hoursLabel = when (hours) {
            1 -> context.getString(R.string.duration_hour_one)
            2 -> context.getString(R.string.duration_hour_two)
            in 3..10 -> context.getString(R.string.duration_hours_few, digits(hours))
            else -> context.getString(R.string.duration_hours_many, digits(hours))
        }
        return if (rest == 0) {
            hoursLabel
        } else {
            context.getString(R.string.duration_hours_and_minutes, hoursLabel, digits(rest))
        }
    }

    /** Long Gregorian date, e.g. "الجمعة ٢٦ سبتمبر ٢٠٢٦". */
    fun gregorianDate(date: Date = Date()): String {
        val formatter = SimpleDateFormat("EEEE d MMMM yyyy", LOCALE)
        return digits(formatter.format(date))
    }

    /** Tabular Hijri date computed on device, e.g. "١٤ ربيع الآخر ١٤٤٨ هـ". */
    fun hijriDate(context: Context, calendar: Calendar = Calendar.getInstance()): String =
        digits(HijriDate.from(calendar).formatArabic(context))

    /** Local calendar day key ("2026-09-26") used to scope the daily tasbeeh total. */
    fun dayKey(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    const val MINUTES_PER_DAY: Int = 24 * 60
}
