package com.clock.livewallpaper.date;

/** Immutable Islamic calendar date, with a 1-based month number. */
public final class HijriDate {

    private final int day;
    private final int month;
    private final int year;

    public HijriDate(int day, int month, int year) {
        if (day < 1 || day > 30 || month < 1 || month > 12 || year < 1) {
            throw new IllegalArgumentException("Invalid Hijri date");
        }
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public int getDay() {
        return day;
    }

    /** 1 = Muharram, 12 = Dhu al-Hijjah. */
    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }
}
