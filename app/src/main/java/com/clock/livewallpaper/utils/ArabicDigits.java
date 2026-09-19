package com.clock.livewallpaper.utils;

/**
 * Converts ASCII digits inside a formatted string to Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩).
 *
 * <p>The catalog hints are written with Arabic-Indic numerals, so a count interpolated with
 * {@code %d} has to be converted before it is shown, otherwise one screen would mix two numeral
 * systems.
 */
public final class ArabicDigits {

    private static final char[] ARABIC_INDIC = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};

    private ArabicDigits() {
    }

    public static String toArabicIndic(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                chars[i] = ARABIC_INDIC[chars[i] - '0'];
            }
        }
        return new String(chars);
    }
}
