package com.clock.livewallpaper.quran;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Backward-compatibility wrapper for Quran data operations.
 * Delegates data retrieval dynamically to QuranApiClient without requiring local database files.
 */
public final class QuranDatabaseHelper {
    public static final String DATABASE_NAME = "quran.ar.uthmani.db";
    private final Context context;

    public QuranDatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /** No-op in online dynamic fetching mode. */
    public void prepareDatabase() {
        // Dynamic online mode fetches and caches on-demand
    }

    /**
     * Retrieves verses for the specified Surah using QuranApiClient.
     */
    public List<Ayah> getVersesBySurah(int surah) throws IOException {
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("Surah must be between 1 and 114.");
        }
        QuranApiClient.SurahData surahData = QuranApiClient.getSurah(context, surah);
        List<Ayah> verses = new ArrayList<>(surahData.ayahs.size());
        for (QuranApiClient.Ayah a : surahData.ayahs) {
            verses.add(new Ayah(a.surah, a.numberInSurah, a.text));
        }
        return verses;
    }

    public static final class Ayah {
        public final int surah;
        public final int number;
        public final String text;

        public Ayah(int surah, int number, String text) {
            this.surah = surah;
            this.number = number;
            this.text = text;
        }
    }
}
