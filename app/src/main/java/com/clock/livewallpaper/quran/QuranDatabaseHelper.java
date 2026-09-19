package com.clock.livewallpaper.quran;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline access to the bundled Mushaf database {@code quran.ar.uthmani.db}.
 *
 * <p><b>Nothing in this class touches the network.</b> On first run the database is copied out of
 * {@code assets/databases/} into app-private storage; every later run reuses that installed copy.
 *
 * <h2>Why this does not extend SQLiteOpenHelper</h2>
 * The shipped database is a read-only, externally produced artifact with its own internal version.
 * Extending {@code SQLiteOpenHelper} would mean writing an empty {@code onCreate()} that must never
 * run, and accepting that Android may bump or rewrite the upstream schema version. Copying the file
 * and opening it read-only keeps the bytes on disk identical to the bytes in the APK.
 *
 * <h2>Install safety</h2>
 * The asset is copied to a temporary file in the same private database directory, flushed, fully
 * validated, and only then renamed into place. A truncated copy from a killed process or a full disk
 * therefore can never be opened under the final database name. A process-wide lock stops two
 * activities from installing concurrently. This is single-process by design; do not call the
 * installer from several Android processes without an inter-process lock.
 *
 * <h2>Schema contract</h2>
 * <pre>
 * CREATE TABLE arabic_text (sura INTEGER, ayah INTEGER, text TEXT);
 * SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC;
 * </pre>
 * Surahs and ayahs are one-based. Extra columns and tables are ignored. Text is returned verbatim:
 * no diacritic stripping, no normalisation and no basmallah injected into ayah text.
 */
public final class QuranDatabaseHelper {

    /** File name of the database, both in assets and in app-private storage. */
    public static final String DATABASE_NAME = "quran.ar.uthmani.db";

    /** Asset path the database is copied from on first run. */
    public static final String ASSET_PATH = "databases/" + DATABASE_NAME;

    private static final String TAG = "QuranDatabaseHelper";
    private static final String TABLE = "arabic_text";
    private static final String QUERY_SURAH =
            "SELECT sura, ayah, text FROM " + TABLE + " WHERE sura = ? ORDER BY ayah ASC";
    private static final String QUERY_AYAH =
            "SELECT text FROM " + TABLE + " WHERE sura = ? AND ayah = ? LIMIT 1";
    private static final String QUERY_SURAH_COUNTS =
            "SELECT sura, COUNT(*) FROM " + TABLE + " GROUP BY sura";
    /**
     * Full-text search contract. LIKE with a bound pattern cannot be injected into, and the ESCAPE
     * clause lets {@link #searchQuran(String)} quote the wildcards so a typed {@code %} or {@code _}
     * is matched literally. Results are capped so a one-letter query cannot bind the UI.
     */
    private static final String QUERY_SEARCH =
            "SELECT sura, ayah, text FROM " + TABLE
                    + " WHERE text LIKE ? ESCAPE '\\' ORDER BY sura ASC, ayah ASC LIMIT ";
    private static final int SEARCH_LIMIT = 200;
    private static final String TEMP_SUFFIX = ".installing";
    private static final int COPY_BUFFER_BYTES = 8 * 1024;

    /** Ornate parentheses used by the Mushaf around an ayah number: ﴿ ٢ ﴾. */
    private static final char AYAH_MARK_OPEN = '\uFD3F';
    private static final char AYAH_MARK_CLOSE = '\uFD3E';
    private static final char NO_BREAK_SPACE = '\u00A0';

    /** Serialises installs across threads and activities in this process. */
    private static final Object INSTALL_LOCK = new Object();

    /** Guards the cache only, so a cache hit never waits behind an in-progress first-run copy. */
    private static final Object CACHE_LOCK = new Object();

    /**
     * Set once the installed file has passed full validation in this process. Validation reads the
     * whole {@code arabic_text} table, so repeating it on every swipe between surahs would be the most
     * expensive part of navigation. Cleared by {@link #clearCache()}.
     */
    private static volatile boolean installVerified;

    /** Formatted surahs are expensive to rebuild, and swiping re-reads neighbours constantly. */
    private static final LruCache<Integer, String> SURAH_TEXT_CACHE = new LruCache<>(8);

    /**
     * The basmallah is needed by 113 of the 114 surahs but lives in surah 1, so without this it would
     * cost a second database open on every single surah load. The text is immutable once installed.
     */
    private static volatile String cachedBasmallah;

    private final Context context;

    public QuranDatabaseHelper(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context.getApplicationContext();
    }

    // ---------------------------------------------------------------------------------------------
    // Installation
    // ---------------------------------------------------------------------------------------------

    /**
     * Makes sure a valid database is installed, copying it from assets on first run.
     *
     * <p>Safe to call repeatedly and from any thread. A valid installed copy is reused as-is, so this
     * is cheap after the first launch. <b>Never call it on the main thread</b>: the first-run copy
     * moves roughly 1.5 MB.
     *
     * @throws IOException if the asset is missing, cannot be read, or the copy fails validation
     */
    public void prepareDatabase() throws IOException {
        File installed = databaseFile();
        // Fast path: already validated in this process and still on disk. Reading this flag needs no
        // lock, so navigating between surahs never contends with an unrelated install.
        if (installVerified && installed.exists()) {
            return;
        }
        synchronized (INSTALL_LOCK) {
            installed = databaseFile();
            if (installVerified && installed.exists()) {
                return; // Another thread finished validating while this one waited for the lock.
            }
            if (installed.exists()) {
                if (isValidDatabase(installed)) {
                    installVerified = true;
                    return;
                }
                // A structurally broken installed copy is recovered from the packaged asset.
                Log.w(TAG, "Installed " + DATABASE_NAME + " failed validation; reinstalling from assets");
                deleteQuietly(installed);
            }
            installFromAssets(installed);
            installVerified = true;
        }
    }

    /**
     * @return {@code true} when a validated database is already installed and no copy is needed.
     *     Never throws, so it is safe to call from the main thread for UI decisions.
     */
    public boolean isDatabaseReady() {
        File installed = databaseFile();
        return installed.exists() && (installVerified || isValidDatabase(installed));
    }

    /** @return the app-private path of the installed database; it may not exist yet. */
    public File databaseFile() {
        // getDatabasePath, never a hard-coded /data/data path: it respects the real package/user id.
        return context.getDatabasePath(DATABASE_NAME);
    }

    private void installFromAssets(File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Could not create database directory: " + parent);
        }

        File temporary = new File(destination.getPath() + TEMP_SUFFIX);
        deleteQuietly(temporary);
        long copiedBytes;
        try {
            copiedBytes = copyAssetToFile(ASSET_PATH, temporary);
        } catch (IOException error) {
            deleteQuietly(temporary);
            throw error;
        }

        try {
            if (copiedBytes == 0) {
                throw new IOException("Asset " + ASSET_PATH + " is empty");
            }
            if (!isValidDatabase(temporary)) {
                throw new IOException("Copied database failed validation: " + ASSET_PATH);
            }
            // Rename within the same directory is atomic, so readers never see a partial database.
            if (!temporary.renameTo(destination)) {
                throw new IOException("Could not move " + temporary.getName() + " into place");
            }
        } catch (IOException | RuntimeException error) {
            deleteQuietly(temporary);
            throw error;
        }
        Log.i(TAG, "Installed " + DATABASE_NAME + " (" + copiedBytes + " bytes)");
    }

    private long copyAssetToFile(String assetPath, File destination) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        long total = 0;
        try (InputStream input = context.getAssets().open(assetPath);
             OutputStream output = new FileOutputStream(destination)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                total += read;
            }
            output.flush();
            // Force the bytes to disk before the rename, so a crash cannot leave a hollow file.
            if (output instanceof FileOutputStream) {
                ((FileOutputStream) output).getFD().sync();
            }
        } catch (IOException error) {
            throw new IOException("Could not copy asset " + assetPath + " to " + destination
                    + ". Confirm the file exists at app/src/main/assets/" + assetPath, error);
        }
        return total;
    }

    /**
     * Structural validation: SQLite integrity, the required schema, and a complete set of surahs and
     * ayahs matching {@link QuranMetadata}. This proves the file is a well-formed, complete Mushaf
     * database; it cannot prove the text is authentic, so always source it from a verified provider.
     */
    private boolean isValidDatabase(File file) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        try (SQLiteDatabase database = openReadOnly(file)) {
            if (!hasRequiredSchema(database)) {
                Log.w(TAG, "Missing table or columns: " + TABLE + "(sura, ayah, text)");
                return false;
            }
            return hasCompleteAyahs(database);
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not validate " + file.getName(), error);
            return false;
        }
    }

    private boolean hasRequiredSchema(SQLiteDatabase database) {
        try (Cursor tables = database.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{TABLE})) {
            if (!tables.moveToFirst()) {
                return false;
            }
            String definition = tables.getString(0);
            if (definition == null) {
                return false;
            }
            String lower = definition.toLowerCase(java.util.Locale.ROOT);
            return lower.contains("sura") && lower.contains("ayah") && lower.contains("text");
        }
    }

    private boolean hasCompleteAyahs(SQLiteDatabase database) {
        int total = 0;
        int surahsSeen = 0;
        try (Cursor cursor = database.rawQuery(QUERY_SURAH_COUNTS, null)) {
            while (cursor.moveToNext()) {
                int surah = cursor.getInt(0);
                int count = cursor.getInt(1);
                if (!SurahIndex.isValid(surah)) {
                    Log.w(TAG, "Unexpected sura value in database: " + surah);
                    return false;
                }
                int expected = QuranMetadata.ayahCount(surah);
                if (count != expected) {
                    Log.w(TAG, "Surah " + surah + " has " + count + " ayahs, expected " + expected);
                    return false;
                }
                surahsSeen++;
                total += count;
            }
        }
        if (surahsSeen != SurahIndex.TOTAL_SURAHS || total != SurahIndex.TOTAL_AYAHS) {
            Log.w(TAG, "Database holds " + surahsSeen + " surahs / " + total + " ayahs");
            return false;
        }
        return true;
    }

    private SQLiteDatabase openReadOnly(File file) {
        return SQLiteDatabase.openDatabase(file.getPath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    private static void deleteQuietly(File file) {
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Could not delete " + file.getName());
        }
    }

    /**
     * Drops the formatted-surah cache and the verified-install flag, forcing the next read to
     * revalidate the file on disk. Only needed if the installed database is replaced at runtime.
     */
    public static void clearCache() {
        installVerified = false;
        cachedBasmallah = null;
        synchronized (CACHE_LOCK) {
            SURAH_TEXT_CACHE.evictAll();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Reading
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a whole surah as one display-ready Arabic string with every ayah terminated by its
     * number in ornate parentheses, for example {@code …ٱلرَّحِيمِ ﴿١﴾ ٱلْحَمْدُ… ﴿٢﴾}.
     *
     * <p>Runs the query on the calling thread, so <b>call it from a background thread</b>. Results
     * are cached, which is what makes swiping between neighbouring surahs feel instant.
     *
     * @param surahId 1-based surah number, 1 to 114
     * @return the formatted surah text, never {@code null} and never empty
     * @throws IllegalArgumentException if {@code surahId} is outside 1..114
     * @throws IOException              if the database is not installed or the surah is missing
     */
    public String getSurahText(int surahId) throws IOException {
        return getSurahText(surahId, true);
    }

    /**
     * Same as {@link #getSurahText(int)}, with explicit control over the surah's basmallah header.
     *
     * <p>The Tanzil text keeps the basmallah out of the numbered ayahs of every surah except 1, and
     * surah 9 has none at all. A Mushaf prints it above surahs 2-8 and 10-114, so it is emitted as an
     * un-numbered leading line rather than being spliced into ayah 1. It is read back out of the
     * database (surah 1, ayah 1) instead of being hard-coded, so the orthography can never diverge
     * from the text being displayed.
     *
     * @param surahId           1-based surah number, 1 to 114
     * @param includeBasmallah  whether to prepend the un-numbered basmallah header line
     */
    public String getSurahText(int surahId, boolean includeBasmallah) throws IOException {
        if (!SurahIndex.isValid(surahId)) {
            throw new IllegalArgumentException("Surah must be between 1 and "
                    + SurahIndex.TOTAL_SURAHS + ", got " + surahId);
        }
        synchronized (CACHE_LOCK) {
            String cached = SURAH_TEXT_CACHE.get(surahId);
            if (cached != null) {
                return cached;
            }
        }

        prepareDatabase();

        List<Ayah> ayahs = queryAyahs(surahId);
        StringBuilder text = new StringBuilder(estimateSize(ayahs.size()));
        if (includeBasmallah && SurahIndex.get(surahId).hasBasmallahHeader()) {
            text.append(getBasmallah()).append("\n\n");
        }
        for (int i = 0; i < ayahs.size(); i++) {
            if (i > 0) {
                text.append(' ');
            }
            text.append(ayahs.get(i).formattedText());
        }

        String result = text.toString();
        synchronized (CACHE_LOCK) {
            SURAH_TEXT_CACHE.put(surahId, result);
        }
        return result;
    }

    /**
     * Reads one surah's ayahs in ascending order.
     *
     * @param surahId 1-based surah number, 1 to 114
     * @return an unmodifiable list with exactly {@link QuranMetadata#ayahCount(int)} entries
     * @throws IOException if the database is unavailable or the surah is incomplete
     */
    public List<Ayah> getVersesBySurah(int surahId) throws IOException {
        if (!SurahIndex.isValid(surahId)) {
            throw new IllegalArgumentException("Surah must be between 1 and "
                    + SurahIndex.TOTAL_SURAHS + ", got " + surahId);
        }
        prepareDatabase();
        return queryAyahs(surahId);
    }

    /**
     * @return the basmallah exactly as the database stores it (surah 1, ayah 1)
     * @throws IOException if the database is unavailable
     */
    public String getBasmallah() throws IOException {
        String cached = cachedBasmallah;
        if (cached != null) {
            return cached;
        }
        prepareDatabase();
        File file = databaseFile();
        try (SQLiteDatabase database = openReadOnly(file);
             Cursor cursor = database.rawQuery(QUERY_AYAH, new String[]{"1", "1"})) {
            if (!cursor.moveToFirst()) {
                throw new IOException("Database does not contain surah 1 ayah 1");
            }
            String text = cursor.getString(0);
            if (text == null || text.trim().isEmpty()) {
                throw new IOException("Surah 1 ayah 1 is empty");
            }
            String basmallah = text.trim();
            cachedBasmallah = basmallah;
            return basmallah;
        } catch (RuntimeException error) {
            throw new IOException("Could not read the basmallah from " + DATABASE_NAME, error);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Searching
    // ---------------------------------------------------------------------------------------------

    /**
     * Searches the whole Mushaf for ayahs whose {@code text} contains {@code query}, using SQL
     * {@code LIKE} against the {@code text} column.
     *
     * <p>Runs on the calling thread, so <b>call it from a background thread</b>. The query is bound
     * as a parameter and the {@code %} / {@code _} wildcards it contains are escaped, so user input
     * can never alter the statement or match as a pattern. Matches come back in Mushaf order
     * (surah ascending, then ayah ascending) and are capped at 200 rows.
     *
     * @param query the word or phrase to look for, in the database's own Uthmani orthography
     * @return an unmodifiable list of matching ayahs, possibly empty, never {@code null}
     * @throws IOException if the database is not installed or the query fails
     */
    public List<Ayah> searchQuran(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        prepareDatabase();
        File file = databaseFile();
        if (!file.exists()) {
            throw new IOException(DATABASE_NAME + " is not installed");
        }

        String pattern = "%" + escapeLikePattern(query.trim()) + "%";
        List<Ayah> matches = new ArrayList<>();
        try (SQLiteDatabase database = openReadOnly(file);
             Cursor cursor = database.rawQuery(QUERY_SEARCH + SEARCH_LIMIT, new String[]{pattern})) {
            int surahColumn = cursor.getColumnIndexOrThrow("sura");
            int ayahColumn = cursor.getColumnIndexOrThrow("ayah");
            int textColumn = cursor.getColumnIndexOrThrow("text");
            while (cursor.moveToNext()) {
                String text = cursor.getString(textColumn);
                if (text == null || text.trim().isEmpty()) {
                    continue; // A blank row can never satisfy a search; skip instead of failing.
                }
                matches.add(new Ayah(cursor.getInt(surahColumn), cursor.getInt(ayahColumn),
                        text.trim()));
            }
        } catch (RuntimeException error) {
            throw new IOException("Could not search " + DATABASE_NAME
                    + ". Check that arabic_text(sura, ayah, text) exists.", error);
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Quotes LIKE wildcards in user input so they match literally, using the {@code \} escape
     * character declared in {@link #QUERY_SEARCH}.
     */
    private static String escapeLikePattern(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<Ayah> queryAyahs(int surahId) throws IOException {
        File file = databaseFile();
        if (!file.exists()) {
            throw new IOException(DATABASE_NAME + " is not installed");
        }
        List<Ayah> ayahs = new ArrayList<>();
        try (SQLiteDatabase database = openReadOnly(file);
             // Bound parameter, never string concatenation: an id cannot alter the query.
             Cursor cursor = database.rawQuery(QUERY_SURAH, new String[]{String.valueOf(surahId)})) {
            int surahColumn = cursor.getColumnIndexOrThrow("sura");
            int ayahColumn = cursor.getColumnIndexOrThrow("ayah");
            int textColumn = cursor.getColumnIndexOrThrow("text");
            while (cursor.moveToNext()) {
                String text = cursor.getString(textColumn);
                if (text == null || text.trim().isEmpty()) {
                    throw new IOException("Empty ayah text at " + surahId + ":"
                            + cursor.getInt(ayahColumn));
                }
                ayahs.add(new Ayah(cursor.getInt(surahColumn), cursor.getInt(ayahColumn), text.trim()));
            }
        } catch (RuntimeException error) {
            throw new IOException("Could not read surah " + surahId + " from " + DATABASE_NAME
                    + ". Check that arabic_text(sura, ayah, text) exists.", error);
        }

        int expected = QuranMetadata.ayahCount(surahId);
        if (ayahs.size() != expected) {
            throw new IOException("Surah " + surahId + " returned " + ayahs.size()
                    + " ayahs from " + DATABASE_NAME + ", expected " + expected);
        }
        for (int i = 0; i < ayahs.size(); i++) {
            if (ayahs.get(i).number != i + 1) {
                throw new IOException("Surah " + surahId + " ayah numbering is not contiguous at position " + i);
            }
        }
        return Collections.unmodifiableList(ayahs);
    }

    /**
     * The Mushaf marker for one ayah number: ornate parentheses around Arabic-Indic digits, padded
     * with non-breaking spaces so the number never wraps away from its ayah.
     *
     * <p>Public so a reader can locate a specific ayah inside a formatted surah without duplicating
     * the punctuation rules -- {@code QuranActivity} uses it to scroll to a requested ayah.
     *
     * @param ayahNumber 1-based ayah number
     * @return the marker, for example {@code ﴿٢٥٥﴾}
     */
    public static String ayahMarker(int ayahNumber) {
        // Leading "" forces string concatenation: two adjacent chars would otherwise add as ints.
        return "" + AYAH_MARK_OPEN + NO_BREAK_SPACE
                + QuranMetadata.arabicNumber(ayahNumber)
                + NO_BREAK_SPACE + AYAH_MARK_CLOSE;
    }

    private static int estimateSize(int ayahCount) {
        // About 160 characters per ayah stops Al-Baqarah from re-growing the builder repeatedly.
        return Math.max(256, ayahCount * 160);
    }

    /** One ayah of the Mushaf, in the database's own numbering and orthography. */
    public static final class Ayah {
        /** 1-based surah number. */
        public final int surah;
        /** 1-based ayah number within the surah. */
        public final int number;
        /** Ayah text, verbatim from the database. */
        public final String text;

        public Ayah(int surah, int number, String text) {
            this.surah = surah;
            this.number = number;
            this.text = text;
        }

        /** @return the ayah text followed by its number in ornate parentheses, e.g. {@code … ﴿٢٥٥﴾}. */
        public String formattedText() {
            return text + " " + ayahMarker(number);
        }

        @Override
        public String toString() {
            return surah + ":" + number;
        }
    }
}
