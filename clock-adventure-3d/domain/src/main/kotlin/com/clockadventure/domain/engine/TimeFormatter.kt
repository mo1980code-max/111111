package com.clockadventure.domain.engine

import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.ClockTime
import java.util.Locale

/**
 * Digital display and spoken pronunciation of a time.
 *
 * Digits are always formatted with a Western digit locale: children learn the same numeral shapes
 * in both languages, and it keeps the 24 hour readout stable on devices set to Arabic numerals.
 */
object TimeFormatter {

    private val hourWordsEn = arrayOf(
        "twelve", "one", "two", "three", "four", "five",
        "six", "seven", "eight", "nine", "ten", "eleven", "twelve"
    )

    private val hourWordsAr = arrayOf(
        "الثانية عشرة", "الواحدة", "الثانية", "الثالثة", "الرابعة", "الخامسة",
        "السادسة", "السابعة", "الثامنة", "التاسعة", "العاشرة", "الحادية عشرة", "الثانية عشرة"
    )

    /** "07:30" / "7:30 AM" / "7:30 ص" depending on format and language. */
    fun digital(time: ClockTime, use24Hour: Boolean, language: AppLanguage): String {
        val mm = String.format(Locale.US, "%02d", time.minute)
        if (use24Hour) {
            return String.format(Locale.US, "%02d:%s", time.hour, mm)
        }
        val hh = time.hour12
        return when (language) {
            AppLanguage.ARABIC -> if (time.isPm) "$hh:$mm م" else "$hh:$mm ص"
            AppLanguage.ENGLISH -> if (time.isPm) "$hh:$mm PM" else "$hh:$mm AM"
        }
    }

    /** Short readout used on the clock face itself (always 12 hour, no AM/PM). */
    fun face(time: ClockTime): String =
        String.format(Locale.US, "%d:%02d", time.hour12, time.minute)

    /** Text handed to the text-to-speech engine. */
    fun spoken(time: ClockTime, language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> spokenArabic(time)
        AppLanguage.ENGLISH -> spokenEnglish(time)
    }

    private fun spokenEnglish(time: ClockTime): String {
        val hour = time.hour12
        val nextHour = if (hour == 12) 1 else hour + 1
        val hourWord = hourWordsEn[hour]
        val nextWord = hourWordsEn[nextHour]
        return when (val m = time.minute) {
            0 -> "$hourWord o'clock"
            5 -> "five past $hourWord"
            10 -> "ten past $hourWord"
            15 -> "quarter past $hourWord"
            20 -> "twenty past $hourWord"
            25 -> "twenty-five past $hourWord"
            30 -> "half past $hourWord"
            35 -> "twenty-five to $nextWord"
            40 -> "twenty to $nextWord"
            45 -> "quarter to $nextWord"
            50 -> "ten to $nextWord"
            55 -> "five to $nextWord"
            else -> if (m < 10) "$hourWord oh ${numberWordEn(m)}" else "$hourWord ${numberWordEn(m)}"
        }
    }

    private fun spokenArabic(time: ClockTime): String {
        val hour = time.hour12
        val hourWord = hourWordsAr[hour]
        val nextHourWord = hourWordsAr[if (hour == 12) 1 else hour + 1]
        return when (val m = time.minute) {
            0 -> "الساعة $hourWord تماماً"
            15 -> "الساعة $hourWord والربع"
            30 -> "الساعة $hourWord والنصف"
            45 -> "الساعة $nextHourWord إلا ربع"
            50 -> "الساعة $nextHourWord إلا عشر دقائق"
            55 -> "الساعة $nextHourWord إلا خمس دقائق"
            else -> {
                val unit = if (m in 3..10) "دقائق" else "دقيقة"
                "الساعة $hourWord و${arabicNumberWord(m)} $unit"
            }
        }
    }

    private val numberWordsEn = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen"
    )

    private fun numberWordEn(value: Int): String = when {
        value < 20 -> numberWordsEn[value]
        value < 60 -> {
            val tens = value / 10
            val rest = value % 10
            val tensWord = when (tens) {
                2 -> "twenty"
                3 -> "thirty"
                4 -> "forty"
                5 -> "fifty"
                else -> ""
            }
            if (rest == 0) tensWord else "$tensWord-${numberWordsEn[rest]}"
        }
        else -> value.toString()
    }

    private val arabicUnits = arrayOf(
        "", "واحدة", "اثنتان", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثماني", "تسع",
        "عشر", "إحدى عشرة", "اثنتا عشرة", "ثلاث عشرة", "أربع عشرة", "خمس عشرة",
        "ست عشرة", "سبع عشرة", "ثماني عشرة", "تسع عشرة"
    )

    private fun arabicNumberWord(value: Int): String {
        if (value < 20) return arabicUnits[value]
        val rest = value % 10
        val tensWord = when (value / 10) {
            2 -> "عشرون"
            3 -> "ثلاثون"
            4 -> "أربعون"
            5 -> "خمسون"
            else -> ""
        }
        return if (rest == 0) tensWord else "${arabicUnits[rest]} و$tensWord"
    }
}
