package com.clock.livewallpaper.quran;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs and reads the bundled Quran database. Call getVersesBySurah off the UI thread.
 * Deliberately not a SQLiteOpenHelper: this prebuilt, read-only database must not be
 * replaced with an empty onCreate database or have its upstream user_version changed.
 */
public final class QuranDatabaseHelper {
    public static final String DATABASE_NAME = "quran.ar.uthmani.db";
    private static final String ASSET_PATH = "databases/" + DATABASE_NAME;
    private static final String FALLBACK_ASSET_PATH = "quran_fallback.json";
    private static final Object COPY_LOCK = new Object();
    private final Context context;

    public QuranDatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Installs/validates the bundled database. Invoke on a worker thread at app startup. */
    public void prepareDatabase() throws IOException {
        installIfNeeded();
    }

    public List<Ayah> getVersesBySurah(int surah) throws IOException {
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("Surah must be between 1 and 114.");
        }
        File file = installIfNeeded();
        List<Ayah> verses = new ArrayList<>();
        // Both handles are closed before returning; no Cursor escapes to the Activity.
        try (SQLiteDatabase database = openReadOnly(file);
             Cursor cursor = database.query("arabic_text",
                     new String[]{"sura", "ayah", "text"},
                     "sura = ?", new String[]{String.valueOf(surah)},
                     null, null, "ayah ASC")) {
            int suraColumn = cursor.getColumnIndexOrThrow("sura");
            int ayahColumn = cursor.getColumnIndexOrThrow("ayah");
            int textColumn = cursor.getColumnIndexOrThrow("text");
            while (cursor.moveToNext()) {
                int number = cursor.getInt(ayahColumn);
                String text = cursor.getString(textColumn);
                if (number != verses.size() + 1 || text == null || text.trim().isEmpty()) {
                    throw new IOException("Missing, duplicate or empty ayah in Surah " + surah);
                }
                verses.add(new Ayah(cursor.getInt(suraColumn), number, text));
            }
        }
        if (verses.size() != QuranMetadata.ayahCount(surah)) {
            throw new IOException("Incomplete Surah " + surah + " in " + DATABASE_NAME);
        }
        return verses;
    }

    /**
     * Reads the tiny checked-in emergency dataset. It intentionally contains Al-Fatiha only,
     * so the screen can still be used in airplane mode if the full database is damaged or an
     * older APK was installed without the database asset.
     */
    public List<Ayah> getBundledFallbackVerses() throws IOException {
        try (InputStream input = context.getAssets().open(FALLBACK_ASSET_PATH)) {
            String json = readUtf8(input);
            try {
                JSONObject root = new JSONObject(json);
                if (root.getInt("surah") != 1) {
                    throw new IOException("The Quran fallback must contain Surah 1.");
                }
                JSONArray items = root.getJSONArray("verses");
                if (items.length() != QuranMetadata.ayahCount(1)) {
                    throw new IOException("The Quran fallback is incomplete.");
                }
                List<Ayah> verses = new ArrayList<>(items.length());
                for (int index = 0; index < items.length(); index++) {
                    JSONObject item = items.getJSONObject(index);
                    int number = item.getInt("ayah");
                    String text = item.getString("text");
                    if (number != index + 1 || text.trim().isEmpty()) {
                        throw new IOException("The Quran fallback contains an invalid ayah.");
                    }
                    verses.add(new Ayah(1, number, text));
                }
                return verses;
            } catch (JSONException error) {
                throw new IOException("The Quran fallback is not valid JSON.", error);
            }
        }
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private File installIfNeeded() throws IOException {
        // Serializes first-use installation across helper instances in this app process.
        synchronized (COPY_LOCK) {
            File destination = context.getDatabasePath(DATABASE_NAME);
            if (destination.isFile() && destination.length() > 0) {
                try {
                    validateDatabase(destination);
                    return destination;
                } catch (SQLiteException | IOException invalidCopy) {
                    // Recover an old corrupt/partial copy from the packaged asset.
                }
            }
            File directory = destination.getParentFile();
            if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
                throw new IOException("Cannot create the private database directory.");
            }
            // Never copy directly to the final name: a killed first run must be retryable.
            File temporary = File.createTempFile("quran-install-", ".db", directory);
            try {
                try (InputStream input = context.getAssets().open(ASSET_PATH);
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                    output.flush();
                    output.getFD().sync();
                }
                validateDatabase(temporary);
                // Rename within the same filesystem only after a complete, validated copy.
                if (destination.exists() && !destination.delete()) {
                    throw new IOException("Cannot replace the invalid Quran database.");
                }
                if (!temporary.renameTo(destination)) {
                    throw new IOException("Cannot install the Quran database.");
                }
                return destination;
            } finally {
                if (temporary.exists()) {
                    // Best-effort cleanup after an unsuccessful install.
                    temporary.delete();
                }
            }
        }
    }

    private static SQLiteDatabase openReadOnly(File file) {
        return SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    private static void validateDatabase(File file) throws IOException {
        try (SQLiteDatabase database = openReadOnly(file)) {
            try (Cursor check = database.rawQuery("PRAGMA quick_check(1)", null)) {
                if (!check.moveToFirst() || !"ok".equalsIgnoreCase(check.getString(0))) {
                    throw new IOException("The Quran database failed its integrity check.");
                }
            }
            // Explicit projection checks the actual table and all required column names.
            try (Cursor schema = database.rawQuery(
                    "SELECT sura, ayah, text FROM arabic_text LIMIT 1", null)) {
                if (!schema.moveToFirst()) {
                    throw new IOException("The arabic_text table is empty.");
                }
            }
        }
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
