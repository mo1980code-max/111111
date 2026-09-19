# Offline Quran reader (native Android / Java)

The implementation is integrated into the existing `com.clock.livewallpaper` app. It uses native Android APIs, supports API 23 and newer, and adds no libraries or network calls.

## Build configuration (updated)

| Setting | Value |
| --- | --- |
| Gradle runtime / Java compiler | JDK 17 |
| Android Gradle Plugin | 8.13.2 |
| Gradle wrapper distribution | 8.13 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 23 |
| Java source / target | `JavaVersion.VERSION_17` |

`app/build.gradle` sets SDK levels, Java 17 compile options, and the Java 17 compiler toolchain. The **root** `build.gradle` declares AGP 8.13.2; `gradle/wrapper/gradle-wrapper.properties` selects Gradle 8.13. All three must be updated together. See the [official AGP compatibility table](https://developer.android.com/build/releases/agp-8-13-0-release-notes#compatibility).

Set Android Studio's **Gradle JDK** to JDK 17, or export `JAVA_HOME` to an installed JDK 17 before invoking the wrapper. The compiler toolchain does not change the JVM that launches Gradle. Install Android SDK Platform 36 and the build tools required by AGP; no machine-specific JDK/SDK path is committed.

Existing Clock/Wallpaper dependencies are retained (one duplicate Glide declaration was removed). Debug shrinking is disabled for easier debugging; release still uses R8 and resource shrinking with the optimized default ProGuard rules. `android.nonFinalResIds=false` in `gradle.properties` preserves the legacy `switch (R.id...)` code in `EditorActivity` under AGP 8. The root build prefers Google/Maven Central/JitPack, retaining a JCenter fallback for legacy artifacts.

**Scope:** the Quran reader handles Android 15/16 enforced edge-to-edge with native window insets and readable system-bar icons. This is not a full target-36 migration audit of the Clock/Wallpaper screens or their old third-party libraries. Test their permissions, media/storage access, back navigation, system-bar insets, and dependency/R8 compatibility separately before releasing the entire app.

## 1. Bundled assets and offline fallback

The required assets are checked into the app at these exact, case-sensitive paths:

```text
app/src/main/assets/
├── databases/
│   └── quran.ar.uthmani.db
├── fonts/
│   └── quran_font.ttf
└── quran_fallback.json
```

The database is a standalone SQLite file containing all 6,236 Hafs-numbered ayahs in
`arabic_text(sura, ayah, text)`, and the font is the bundled Arabic Quran typeface. The
small JSON file is deliberately an emergency fallback containing the seven ayahs of
Al-Fatiha. It lets the screen render useful offline content even when an old install,
partial APK update, or corrupt database prevents the full reader from opening. No
runtime download or network connection is required.

The database must remain an uncompressed, standalone SQLite file (not a ZIP or a database
that depends on a separate WAL file). Required schema contract:

```sql
-- Describes the required columns; do not create an empty database with this SQL.
CREATE TABLE arabic_text (
    sura INTEGER,
    ayah INTEGER,
    text TEXT
);

-- Actual query contract used by the Java helper:
SELECT sura, ayah, text
FROM arabic_text
WHERE sura = ?
ORDER BY ayah ASC;
```

Extra columns/tables are fine. Surahs and ayahs are one-based. The helper expects complete Hafs-numbered Surahs, checks for missing/duplicate/empty verses, and checks their expected verse counts. The font must support the database's actual text encoding and Quranic diacritics. Merely renaming an unrelated font is not sufficient. Verify text authenticity and rendering against a trusted Mushaf; structural validation cannot establish textual authenticity.

## 2. Open a Surah from an existing Java click handler

```java
import android.content.Intent;
import com.clock.livewallpaper.activity.QuranActivity;

// For example, inside your existing Activity:
Intent intent = new Intent(this, QuranActivity.class);
intent.putExtra(QuranActivity.EXTRA_SURAH, 2); // 1–114; defaults to Al-Fatihah
intent.putExtra(QuranActivity.EXTRA_AYAH, 255); // optional; defaults to ayah 1
startActivity(intent);
```

The reader is registered in `AndroidManifest.xml` with a native no-action-bar theme and `exported="false"`. Your existing launcher and navigation are unchanged; connect the Intent to your preferred Quran entry point. No storage permission is needed: the database lives in app-private storage. Existing Internet permissions used by other app features are untouched; this reader does not use them.

This app's built-in entry point is the home screen (`MainActivity` → `activity_select_function.xml`): a `#2D7D46` pill button with the `res/drawable/ic_quran.xml` icon that launches `QuranActivity` (Surah 1, ayah 1) via an `OnClickListener` + `Intent`.

## Included files

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/clock/livewallpaper/quran/QuranDatabaseHelper.java` | First-use asset copy, integrity/schema checks, read-only SQLite query, closed resources |
| `app/src/main/java/com/clock/livewallpaper/activity/QuranActivity.java` | Background loading, font, clickable verses, persistent selection, state restoration |
| `app/src/main/java/com/clock/livewallpaper/quran/QuranMetadata.java` | Ayah counts and official 604-page Madani page/Juz mapping |
| `app/src/main/res/layout/activity_quran.xml` | Paper-colored, RTL, centered, scrollable reader |
| `app/src/main/res/values/quran.xml` | Colors, labels, error messages and native theme |
| `app/src/main/assets/quran_fallback.json` | Validated seven-ayah Al-Fatiha fallback for damaged or missing full-data assets |
| `app/src/main/res/values/quran_surah_names.xml` | All 114 Arabic Surah names, in default resources so the existing English-only resource configuration retains them |

To reuse this in a different application, copy all six files, change package declarations and the `R` import, and register `QuranActivity` with `@style/QuranTheme` in that application's manifest. Also merge the background `prepareDatabase()` startup hook from `app/src/main/java/com/clock/livewallpaper/AppClass.java` into your existing `Application.onCreate()`; do not replace unrelated application initialization. Apply the build configuration listed above.

## Rendering and metadata behavior

- Paper: `#FBF9F0`; verse ink: `#1A1A1A`; Surah/Juz/page labels: `#7A7A7A`.
- Surah is physically right and Juz left, regardless of the device language; the footer is physically right.
- Text is centered and explicitly RTL. Each verse is followed by a marker such as `﴿ ٢٥٥ ﴾` with Arabic-Indic digits.
- Tap any verse to highlight its entire text and marker in `#D0ECE4`. The initially requested ayah is highlighted and scrolled into view. Selection and scroll offset survive Activity recreation.
- **Juz and page refer to the selected ayah**, initially ayah 1 unless another is requested. Tap a later verse to update both labels. Juz boundaries use exact Surah/ayah positions, including boundaries occurring within a printed page.
- This is a **whole-Surah, reflowable TextView**, not an exact 15-line printed page or an image-based Mushaf. A long Surah spans many printed pages; phone scroll position is not treated as a Mushaf page number. Metadata assumes the standard 604-page Madani Mushaf with Hafs verse numbering.
- Database text is preserved verbatim: no diacritic removal, normalization, or automatic basmallah insertion. Upstream text variants can differ in whether basmallah is included, so blindly prepending it can duplicate text.

## Database lifecycle

`AppClass.onCreate()` calls `prepareDatabase()` on a worker thread at initial app launch (and validates/reuses the installed copy on later starts). A failure is logged without crashing unrelated Clock/Wallpaper features. `QuranActivity` also prepares the database when querying, so it safely waits for an in-progress install or retries a failed startup install. If that retry still fails, the Activity first opens `quran_fallback.json` for Surah Al-Fatiha, and otherwise exposes both a Retry action and the same fallback action instead of leaving a blank reader.

The helper uses `context.getDatabasePath("quran.ar.uthmani.db")`, not a hard-coded `/data/data` path. On first use, it copies to a temporary file in the same private database directory, syncs and validates it, then renames it into place. A partial first-run copy is never opened under the final database name. A process-wide lock prevents competing Activity instances from installing simultaneously.

Subsequent opens reuse the installed file after validation. A structurally invalid installed copy is recovered from the packaged asset. Database copying and querying run on a worker thread; cursors and connections are closed in try-with-resources. Destroyed Activities ignore late results.

`QuranDatabaseHelper` intentionally does **not** extend `SQLiteOpenHelper`: there is no empty `onCreate()` schema and no forced change to the upstream database version. This sample is single-process; do not call its installer from multiple Android processes without an inter-process lock. If a later app version ships revised Quran data, add an explicit version/hash-based replacement policy; a valid installed database is intentionally not overwritten on each launch.

## Official repository references and licensing

Reference revision: [`1ec595ecced95ab215b1e9c542c910c4c5436166`](https://github.com/quran/quran_android/tree/1ec595ecced95ab215b1e9c542c910c4c5436166).

- [DatabaseHandler.kt](https://github.com/quran/quran_android/blob/1ec595ecced95ab215b1e9c542c910c4c5436166/app/src/main/java/com/quran/labs/androidquran/database/DatabaseHandler.kt): `arabic_text`, `sura`, `ayah`, `text`, and verse ordering. The implementation here is Java, not an import of the upstream Kotlin database layer.
- [ArabicDatabaseUtils.kt](https://github.com/quran/quran_android/blob/1ec595ecced95ab215b1e9c542c910c4c5436166/app/src/main/java/com/quran/labs/androidquran/model/translation/ArabicDatabaseUtils.kt): text retrieval and notes about basmallah differences.
- [MadaniDataSource.kt](https://github.com/quran/quran_android/blob/1ec595ecced95ab215b1e9c542c910c4c5436166/pages/data/madani/src/main/kotlin/com/quran/labs/androidquran/pages/data/madani/MadaniDataSource.kt): numeric metadata adapted into `QuranMetadata.java`. Page starts are encoded as `surah * 1000 + ayah`; Juz starts are taken from every eighth quarter boundary.
- [Arabic Surah names](https://github.com/quran/quran_android/blob/1ec595ecced95ab215b1e9c542c910c4c5436166/common/ui/core/src/main/res/values-ar/sura_names.xml): adapted with a feature-specific resource name.

Upstream is GPL-3.0. Its license is preserved in `docs/licenses/quran_android-GPL-3.0.txt`; the adapted metadata and names are attributed in their files. **Review GPL compatibility before distributing this within your existing app.** For an incompatible distribution model, replace those resources with appropriately licensed metadata. Separately verify the database and font redistribution licenses and include their required notices. This integration does not imply permission to redistribute arbitrary Quran databases/fonts.

## Validation

Run the SDK-independent checks:

```sh
python3 -m unittest discover -s tests -p 'test_offline_quran.py' -v
```

These check metadata coverage, page/Juz boundaries, layout/resource wiring, and the SQLite query contract using synthetic non-Quran fixture strings. They also validate the real bundled database/font if present, and compile/run the metadata Java class if a JDK is available.

Then, in an environment with JDK 17 and Android SDK 36 configured:

```sh
bash gradlew :app:assembleDebug :app:lintDebug
```

Android build/device tests could not be run in the provided workspace because it has no JDK or Android SDK. Before shipping, test:

1. Fresh install in airplane mode: open Surahs 1, 2, 9 and 114; verify counts and glyphs.
2. Tap ayahs, including 2:141 → 2:142, 5:81 → 5:82, and 9:92 → 9:93; verify Juz changes.
3. Rotate while loading and while scrolled into a long Surah; check state and errors.
4. Reopen offline; confirm the installed database is reused.
5. On a debug device, test an invalid installed database, missing assets, and interrupted first-run installation.
6. Check RTL layout, large system font sizes, TalkBack clickable verses, and scroll/tap behavior on API 23 and an Android 16/API 36 device.

7. On API 36, test portrait/landscape, gesture and three-button navigation, display cutouts, and large-screen resizing; ensure header/footer never overlap system bars.
