package com.clock.livewallpaper.clock;

import java.util.Calendar;
import java.util.Locale;

/**
 * Offline tabular Hijri date provider shared by Clock Studio and the live wallpaper.
 *
 * <p>It deliberately has no network or second implementation in a renderer.  The adjustment is
 * limited to the values exposed by the UI (-2 through +2) and is applied to the local calendar date
 * before conversion, so the result changes naturally after midnight.
 */
public final class HijriDateProvider {
    // Integer Julian-day form of 1 Muharram 1 AH. The +1 keeps civil dates aligned with the
    // conventional tabular result (for example, 2024-04-10 is 1 Shawwal 1445).
    private static final int ISLAMIC_EPOCH = 1948440;
    private static final String[] ENGLISH_MONTHS = {
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
            "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qidah",
            "Dhu al-Hijjah"
    };
    private static final String[] ARABIC_MONTHS = {
            "محرّم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    };

    private HijriDateProvider() {
    }

    public static HijriDate today(int adjustment) {
        return from(Calendar.getInstance(), adjustment);
    }

    public static HijriDate from(Calendar source, int adjustment) {
        Calendar date = (Calendar) source.clone();
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, ClockStudioConfig.clamp(adjustment, -2, 2));
        int julianDay = gregorianToJulianDay(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
        int year = (30 * (julianDay - ISLAMIC_EPOCH) + 10646) / 10631;
        int month = Math.min(12, (int) Math.ceil(
                (julianDay - (29 + islamicToJulianDay(year, 1, 1))) / 29.5d) + 1);
        int day = julianDay - islamicToJulianDay(year, month, 1) + 1;
        return new HijriDate(year, month, day);
    }

    private static int gregorianToJulianDay(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + (12 * a) - 3;
        return day + ((153 * m + 2) / 5) + (365 * y) + (y / 4) - (y / 100) + (y / 400) - 32045;
    }

    private static int islamicToJulianDay(int year, int month, int day) {
        return day + (int) Math.ceil(29.5d * (month - 1)) + ((year - 1) * 354)
                + ((3 + (11 * year)) / 30) + ISLAMIC_EPOCH - 1;
    }

    public static final class HijriDate {
        public final int year;
        public final int month;
        public final int day;

        private HijriDate(int year, int month, int day) {
            this.year = year;
            this.month = Math.max(1, Math.min(12, month));
            this.day = Math.max(1, Math.min(30, day));
        }

        public String format(Locale locale) {
            if (locale != null && "ar".equalsIgnoreCase(locale.getLanguage())) {
                return day + " " + ARABIC_MONTHS[month - 1] + " " + year + " هـ";
            }
            return day + " " + ENGLISH_MONTHS[month - 1] + " " + year + " AH";
        }

        public String formatArabic() {
            return format(new Locale("ar"));
        }

        public String formatEnglish() {
            return format(Locale.ENGLISH);
        }
    }
}
