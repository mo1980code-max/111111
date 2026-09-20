package com.clock.livewallpaper.date;

import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Calendar;

/**
 * Offline provider for the current Hijri date.
 *
 * <p>On Android 7.0 and newer it asks the platform ICU IslamicCalendar, which follows the
 * calendar implementation supplied by the device. On older supported devices it falls back to the
 * well-known tabular civil conversion. No network or hardcoded current date is used.</p>
 */
public final class HijriDateProvider {

    private static final int MIN_ADJUSTMENT = -2;
    private static final int MAX_ADJUSTMENT = 2;

    private HijriDateProvider() {
    }

    public static HijriDate today() {
        return today(0);
    }

    /**
     * Calculates today's Hijri date after applying only the display adjustment.
     * The supplied adjustment never changes the device Calendar or Android system date.
     */
    public static HijriDate today(int adjustment) {
        int safeAdjustment = Math.max(MIN_ADJUSTMENT, Math.min(MAX_ADJUSTMENT, adjustment));
        Calendar localDate = Calendar.getInstance();
        localDate.add(Calendar.DAY_OF_MONTH, safeAdjustment);

        HijriDate platformDate = fromAndroidIcu(localDate);
        return platformDate != null ? platformDate : fromCivilTabular(localDate);
    }

    /** Uses reflection so the app remains safe on the project's API 23 minimum device. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static HijriDate fromAndroidIcu(Calendar localDate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null;
        }
        try {
            Class<?> calendarClass = Class.forName("android.icu.util.IslamicCalendar");
            Constructor<?> constructor = calendarClass.getConstructor();
            Object islamicCalendar = constructor.newInstance();
            // Pin the supported ICU implementation to Umm al-Qura instead of relying on a
            // locale mapping that can vary between Android/ICU releases.
            try {
                Class<?> calculationTypeClass = Class.forName(
                        "android.icu.util.IslamicCalendar$CalculationType");
                Object umAlQura = Enum.valueOf((Class) calculationTypeClass, "ISLAMIC_UMALQURA");
                Method setCalculationType = calendarClass.getMethod(
                        "setCalculationType", calculationTypeClass);
                setCalculationType.invoke(islamicCalendar, umAlQura);
            } catch (Exception ignored) {
                // Older ICU variants fall back to their documented default implementation.
            }
            Method setTimeInMillis = calendarClass.getMethod("setTimeInMillis", long.class);
            Method get = calendarClass.getMethod("get", int.class);
            setTimeInMillis.invoke(islamicCalendar, localDate.getTimeInMillis());
            int year = (Integer) get.invoke(islamicCalendar, Calendar.YEAR);
            int monthZeroBased = (Integer) get.invoke(islamicCalendar, Calendar.MONTH);
            int day = (Integer) get.invoke(islamicCalendar, Calendar.DAY_OF_MONTH);
            return new HijriDate(day, monthZeroBased + 1, year);
        } catch (Exception ignored) {
            // A vendor ICU implementation can be absent or incomplete; use the offline fallback.
            return null;
        }
    }

    /**
     * Tabular Islamic civil conversion used only when android.icu is unavailable. This is a pure
     * Gregorian-to-Hijri calculation and therefore works offline on API 23 as well.
     */
    private static HijriDate fromCivilTabular(Calendar gregorian) {
        int year = gregorian.get(Calendar.YEAR);
        int month = gregorian.get(Calendar.MONTH) + 1;
        int day = gregorian.get(Calendar.DAY_OF_MONTH);

        long julianDay = (1461L * (year + 4800 + (month - 14) / 12)) / 4
                + (367L * (month - 2 - 12 * ((month - 14) / 12))) / 12
                - (3L * ((year + 4900 + (month - 14) / 12) / 100)) / 4
                + day - 32075;
        long l = julianDay - 1948440L + 10632L;
        long n = (l - 1L) / 10631L;
        l = l - 10631L * n + 354L;
        long j = ((10985L - l) / 5316L) * ((50L * l) / 17719L)
                + (l / 5670L) * ((43L * l) / 15238L);
        l = l - ((30L - j) / 15L) * ((17719L * j) / 50L)
                - (j / 16L) * ((15238L * j) / 43L) + 29L;
        int hijriMonth = (int) ((24L * l) / 709L);
        int hijriYear = (int) (30L * n + j - 30L);
        int hijriDay = (int) (l - (709L * hijriMonth) / 24L);
        return new HijriDate(hijriDay, hijriMonth, hijriYear);
    }
}
