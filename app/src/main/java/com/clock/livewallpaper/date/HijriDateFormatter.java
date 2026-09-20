package com.clock.livewallpaper.date;

import android.content.Context;
import android.os.Build;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.utils.ArabicDigits;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Locale-aware, Android-rendered date strings for the Clock Studio. */
public final class HijriDateFormatter {

    private HijriDateFormatter() {
    }

    public static boolean isArabicLocale(Context context) {
        Locale locale = currentLocale(context);
        return "ar".equalsIgnoreCase(locale.getLanguage());
    }

    public static String formatHijri(Context context, HijriDate date) {
        if (isArabicLocale(context)) {
            String[] months = context.getResources().getStringArray(R.array.hijri_months_arabic);
            return ArabicDigits.toArabicIndic(String.valueOf(date.getDay())) + " "
                    + months[date.getMonth() - 1] + " "
                    + ArabicDigits.toArabicIndic(String.valueOf(date.getYear())) + " هـ";
        }
        String[] months = context.getResources().getStringArray(R.array.hijri_months_english);
        return date.getDay() + " " + months[date.getMonth() - 1] + " "
                + date.getYear() + " AH";
    }

    public static String formatGregorian(Context context, Date date) {
        Locale locale = currentLocale(context);
        if (!isArabicLocale(context)) {
            // The order is intentionally the readable international form requested for English.
            return new SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(date);
        }
        return androidDateFormat(date, locale, true);
    }

    public static String formatDay(Context context, Date date) {
        Locale locale = currentLocale(context);
        return new SimpleDateFormat("EEEE", locale).format(date);
    }

    private static String androidDateFormat(Date date, Locale locale, boolean full) {
        java.text.DateFormat formatter = java.text.DateFormat.getDateInstance(
                full ? java.text.DateFormat.FULL : java.text.DateFormat.DEFAULT, locale);
        return formatter.format(date);
    }

    private static Locale currentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        }
        // Configuration.locale is available on the API 23 minimum.
        return context.getResources().getConfiguration().locale;
    }
}
