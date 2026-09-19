package com.clock.livewallpaper.quran;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic online client for fetching Surahs and Ayahs from the Alquran Cloud API.
 * Uses in-memory and persistent disk caching so each Surah is requested only once.
 */
public final class QuranApiClient {
    private static final String TAG = "QuranApiClient";
    public static final String API_URL_FORMAT = "https://api.alquran.cloud/v1/surah/%d/quran-uthmani";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;

    // In-memory cache for fast subsequent lookups
    private static final Map<Integer, SurahData> MEMORY_CACHE = new ConcurrentHashMap<>();

    private QuranApiClient() { }

    /**
     * Retrieves Surah data, checking in-memory cache, disk cache, or fetching dynamically from API.
     *
     * @param context Application context for caching
     * @param surahNumber Surah number between 1 and 114
     * @return Parsed SurahData
     * @throws IOException If network or parsing fails
     */
    public static SurahData getSurah(Context context, int surahNumber) throws IOException {
        if (surahNumber < 1 || surahNumber > 114) {
            throw new IllegalArgumentException("Surah must be between 1 and 114.");
        }

        // 1. Check in-memory cache
        SurahData inMemory = MEMORY_CACHE.get(surahNumber);
        if (inMemory != null) {
            return inMemory;
        }

        // 2. Check disk cache
        if (context != null) {
            SurahData onDisk = loadFromDiskCache(context, surahNumber);
            if (onDisk != null) {
                MEMORY_CACHE.put(surahNumber, onDisk);
                return onDisk;
            }
        }

        // 3. Fetch from Alquran Cloud API
        String endpoint = String.format(API_URL_FORMAT, surahNumber);
        String jsonResponse = fetchHttp(endpoint);

        try {
            SurahData parsed = parseSurahJson(jsonResponse);
            // Save to memory cache
            MEMORY_CACHE.put(surahNumber, parsed);
            // Save to disk cache
            if (context != null) {
                saveToDiskCache(context, surahNumber, jsonResponse);
            }
            return parsed;
        } catch (JSONException e) {
            throw new IOException("Failed to parse API response for Surah " + surahNumber, e);
        }
    }

    /**
     * Clears all in-memory cache entries.
     */
    public static void clearMemoryCache() {
        MEMORY_CACHE.clear();
    }

    private static String fetchHttp(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP request failed with status: " + responseCode);
            }

            try (InputStream in = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append('\n');
                }
                return result.toString();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static SurahData parseSurahJson(String jsonStr) throws JSONException, IOException {
        JSONObject root = new JSONObject(jsonStr);
        int code = root.optInt("code", 0);
        if (code != 200) {
            String status = root.optString("status", "Unknown error");
            throw new IOException("API error status: " + status);
        }

        JSONObject data = root.getJSONObject("data");
        int surahNumber = data.getInt("number");
        String name = data.optString("name", "");
        String englishName = data.optString("englishName", "");
        JSONArray ayahsArray = data.getJSONArray("ayahs");

        List<Ayah> ayahs = new ArrayList<>(ayahsArray.length());
        for (int i = 0; i < ayahsArray.length(); i++) {
            JSONObject ayahObj = ayahsArray.getJSONObject(i);
            int number = ayahObj.getInt("number");
            int numberInSurah = ayahObj.getInt("numberInSurah");
            String text = ayahObj.getString("text");
            int juz = ayahObj.optInt("juz", QuranMetadata.juzFor(surahNumber, numberInSurah));
            int page = ayahObj.optInt("page", QuranMetadata.pageFor(surahNumber, numberInSurah));
            ayahs.add(new Ayah(surahNumber, number, numberInSurah, text, juz, page));
        }

        return new SurahData(surahNumber, name, englishName, ayahs);
    }

    private static SurahData loadFromDiskCache(Context context, int surahNumber) {
        try {
            File cacheDir = new File(context.getCacheDir(), "quran_cache");
            File file = new File(cacheDir, "surah_" + surahNumber + ".json");
            if (!file.exists() || file.length() == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return parseSurahJson(sb.toString());
        } catch (Exception e) {
            Log.w(TAG, "Could not load Surah " + surahNumber + " from disk cache", e);
            return null;
        }
    }

    private static void saveToDiskCache(Context context, int surahNumber, String jsonResponse) {
        try {
            File cacheDir = new File(context.getCacheDir(), "quran_cache");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                return;
            }
            File file = new File(cacheDir, "surah_" + surahNumber + ".json");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not save Surah " + surahNumber + " to disk cache", e);
        }
    }

    public static final class Ayah {
        public final int surah;
        public final int globalNumber;
        public final int numberInSurah;
        public final String text;
        public final int juz;
        public final int page;

        public Ayah(int surah, int globalNumber, int numberInSurah, String text, int juz, int page) {
            this.surah = surah;
            this.globalNumber = globalNumber;
            this.numberInSurah = numberInSurah;
            this.text = text;
            this.juz = juz;
            this.page = page;
        }
    }

    public static final class SurahData {
        public final int number;
        public final String name;
        public final String englishName;
        public final List<Ayah> ayahs;

        public SurahData(int number, String name, String englishName, List<Ayah> ayahs) {
            this.number = number;
            this.name = name;
            this.englishName = englishName;
            this.ayahs = Collections.unmodifiableList(ayahs);
        }
    }
}
