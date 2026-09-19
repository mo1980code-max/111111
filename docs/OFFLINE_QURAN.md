# Offline Quran — Surah Index + Reader

A fully offline, fully navigable Quran reader inside the existing `com.clock.livewallpaper` app.
All 114 surahs are browsable from an index and readable from a single reader screen that pages
between surahs in place. **No network access, no runtime downloads, no API keys.**

Native Android APIs only, `minSdk 23`, `targetSdk`/`compileSdk` 36, Java 17. The v2 upgrade added
one dependency: `androidx.viewpager2:viewpager2:1.0.0` for swipe navigation.

---

## v2 upgrade at a glance

The five standard reader features layered on top of the offline core:

| Feature | Where | Persistence |
| --- | --- | --- |
| Swipe navigation — `ViewPager2` + `FragmentStateAdapter`, one `QuranFragment` per surah; Next/Previous buttons removed | `QuranActivity`, `QuranFragment`, `activity_quran.xml`, `fragment_quran_page.xml` | — |
| Bookmark — toolbar icon saves `last_read_surah_id` + `scrollY`; **Continue Reading** button on the index jumps back | `QuranSettings`, `QuranActivity`, `SurahListActivity` | `SharedPreferences("quran_prefs")` |
| Search — `QuranDatabaseHelper.searchQuran(String)` (SQL `LIKE` on `text`, escaped wildcards, capped at 200); `SearchView` filters surah names while typing and full-text-searches on submit | `QuranDatabaseHelper`, `SurahListActivity`, `AyahSearchAdapter`, `item_search_result.xml` | — |
| Adjustable font size — `+`/`−` toolbar icons, 16–44 sp in 1 sp steps | `QuranActivity`, `QuranFragment`, `QuranSettings` | `SharedPreferences` float |
| Dark mode — moon/sun toggle on both screens; light Madani `#FBF9F0`/`#1A1A1A`, dark `#121212`/`#E0E0E0` with `#A0A0A0` chrome, switched programmatically by `QuranThemeColors` | `QuranThemeColors`, `QuranSettings`, both activities, list adapters | `SharedPreferences` boolean |

The two Quran activities now extend `AppCompatActivity` (a `FragmentStateAdapter` requires a
`FragmentActivity` host), and `QuranTheme` was moved to a `Theme.AppCompat` parent accordingly.

---

## 1. What this replaces

An earlier iteration fetched surahs at runtime from `api.alquran.cloud` and fell back to a
hard-coded 7-ayah array for Al-Fatihah, so in practice only Al-Fatihah was reliably readable.
That online path is **deleted**: `QuranApiClient.java`, `tests/test_quran_api.py`,
`docs/DYNAMIC_QURAN.md` and `docs/OFFLINE_QURAN_COMPLETE.md` no longer exist, and the
`res/values/quran_surah_names.xml` array was superseded by Java arrays.

The reader now queries a real bundled SQLite database, so every surah is available on a device in
airplane mode.

---

## 2. Screen flow

```
MainActivity (home)
   └── Quran tile  ──►  SurahListActivity          RecyclerView, 114 rows, offline, instant
                            │  · SearchView: filters names while typing,
                            │    full-text search on submit (tap a result → that ayah)
                            │  · Continue Reading: jumps to the saved bookmark
                            │  tap a row / result
                            │  Intent extras: surah_id, surah_name, [ayah | scroll_y]
                            ▼
                         QuranActivity             ViewPager2, one QuranFragment per surah
                            ▲  │
                            └──┘  swipe left/right between surahs — no new Activity,
                                  no back-stack growth; toolbar carries font +/-,
                                  bookmark and the moon/sun theme toggle
```

`SurahListActivity` also pre-warms the database on a worker thread, so the first surah a user opens
usually needs no spinner. The pre-warm is idempotent and shares a process-wide lock with the copy
started from `AppClass.onCreate()`.

---

## 3. Bundled assets

Both binaries are committed, because the feature is offline by definition.

| Path | Size | What it is |
| --- | --- | --- |
| `app/src/main/assets/databases/quran.ar.uthmani.db` | 1.6 MB | 114 surahs / 6236 ayahs, Uthmani script, Hafs numbering |
| `app/src/main/assets/fonts/quran_font.ttf` | 164 KB | Amiri Quran Regular — the face designed for this text |

### Provenance

* **Text** — Tanzil Quran Text (Uthmani, Version 1.1), Copyright (C) 2007-2026 Tanzil Project,
  Creative Commons Attribution 3.0, http://tanzil.net. Stored **verbatim**: no diacritic stripping,
  no normalisation, no re-ordering.
* **Font** — Amiri Quran Regular, Amiri Project, SIL Open Font License 1.1. Verified to cover every
  codepoint in the database, plus all 114 surah names, the Arabic header labels, Arabic-Indic digits
  and both ornate parentheses.
* **Licence texts** — `docs/licenses/Tanzil-Uthmani-CC-BY-3.0.txt`,
  `docs/licenses/AmiriQuran-OFL-1.1.txt`. CC-BY-3.0 requires the source to be indicated in the
  application, so the index screen carries a visible attribution line and the database records
  `source`/`license` in its `properties` table.

### Regenerating the database

The `.db` is generated, never hand-edited:

```sh
python3 tools/build_quran_db.py                 # downloads the Tanzil XML and rebuilds
python3 tools/build_quran_db.py --xml path.xml  # rebuild offline from a local copy
```

It refuses to emit a file unless the copyright block is present, all 114 surahs and 6236 ayahs are
there, every per-surah ayah count matches, and `PRAGMA quick_check` is clean.

---

## 4. Database contract

```sql
CREATE TABLE arabic_text (sura INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL);
CREATE TABLE properties  (property TEXT NOT NULL, value TEXT NOT NULL);
CREATE INDEX arabic_text_sura_ayah ON arabic_text (sura, ayah);

-- the only query the reader needs:
SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC;
```

Surahs and ayahs are one-based. Extra columns and tables are ignored, so a richer database can be
dropped in unchanged. The index makes `WHERE sura = ? ORDER BY ayah ASC` a range scan with no sort.

`QuranDatabaseHelper` deliberately does **not** extend `SQLiteOpenHelper`: the shipped database is a
read-only external artifact with its own version, and extending the helper would mean writing an
`onCreate()` that must never run and accepting that Android may rewrite the upstream schema version.
Copying the file and opening it read-only keeps the bytes on disk identical to the bytes in the APK.

### Install safety

1. Copy the asset to `quran.ar.uthmani.db.installing` in the same private database directory.
2. `flush()` + `FileDescriptor.sync()`, so a crash cannot leave a hollow file.
3. Validate: `PRAGMA quick_check`, the required schema, and a `GROUP BY sura` count compared against
   `QuranMetadata` for all 114 surahs.
4. Only then `renameTo()` into place — atomic within one directory, so a reader never opens a partial
   database.

A process-wide lock stops two activities installing at once. A structurally invalid installed copy is
recovered from the packaged asset. Validation is memoised per process, because re-reading 6236 rows on
every swipe between surahs would be the most expensive part of navigation. `getDatabasePath()` is used,
never a hard-coded `/data/data` path. No storage permission is needed.

---

## 5. Java API

### `QuranDatabaseHelper`

```java
QuranDatabaseHelper db = new QuranDatabaseHelper(context);

db.prepareDatabase();              // first-run asset copy; worker thread only
db.isDatabaseReady();              // never throws; safe for UI decisions

String text = db.getSurahText(2);  // whole surah, display-ready, ﴿n﴾ markers
String text = db.getSurahText(2, false);          // without the basmallah header
List<QuranDatabaseHelper.Ayah> ayahs = db.getVersesBySurah(2);
String basmallah = db.getBasmallah();             // read from 1:1, never hard-coded
QuranDatabaseHelper.ayahMarker(255);              // "﴿٢٥٥﴾"
QuranDatabaseHelper.clearCache();                 // after replacing the database

// v2: full-text search, worker thread only
List<QuranDatabaseHelper.Ayah> hits = db.searchQuran("ٱلرحيم");  // SQL LIKE, Mushaf order, ≤200 rows
```

`getSurahText(int)` returns one string: every ayah terminated by its number in ornate parentheses
with non-breaking-space padding so a number never wraps away from its ayah. Results are held in an
8-entry `LruCache`, which is what makes swiping between neighbouring surahs feel instant.

`searchQuran(String)` matches with `text LIKE ?` using a bound pattern; `%` and `_` in user input
are escaped with an `ESCAPE '\'` clause so they match literally, and results are capped at 200 rows
in Mushaf order (surah ascending, then ayah ascending).

**Basmallah handling.** The Tanzil text keeps the basmallah out of the numbered ayahs of every surah
except 1, and surah 9 has none. A mushaf prints it above surahs 2-8 and 10-114, so it is emitted as an
un-numbered leading line rather than spliced into ayah 1 — splicing it would have mis-numbered every
ayah or duplicated text in surah 1. It is read back out of the database (1:1) so its orthography can
never drift from the text beside it.

### `SurahIndex`

```java
SurahIndex.TOTAL_SURAHS;                    // 114
SurahIndex.TOTAL_AYAHS;                     // 6236
SurahIndex.ARABIC_NAMES / ENGLISH_NAMES / ENGLISH_MEANINGS;   // String[114]
SurahIndex.isValid(2); SurahIndex.get(2); SurahIndex.all();
SurahIndex.arabicName(2); SurahIndex.englishName(2);

SurahIndex.Surah s = SurahIndex.get(2);
s.id; s.arabicName; s.englishName; s.englishMeaning; s.medinan; s.ayahCount;
s.firstJuz; s.lastJuz; s.firstPage; s.lastPage;
s.hasBasmallahHeader(); s.spansMultipleJuz(); s.spansMultiplePages(); s.arabicNumber();
```

Names live in Java as requested. Ayah counts and the page/Juz mapping are delegated to
`QuranMetadata` rather than duplicated, so the list screen, the header and the database cannot
disagree.

### Opening a surah

```java
Intent intent = new Intent(this, QuranActivity.class);
intent.putExtra(QuranActivity.EXTRA_SURAH_ID, 2);            // "surah_id", 1-114, defaults to 1
intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, "البقرة");    // "surah_name"
intent.putExtra(QuranActivity.EXTRA_AYAH, 255);              // optional one-shot ayah target
intent.putExtra(QuranActivity.EXTRA_SCROLL_Y, 4321);         // optional one-shot px offset
startActivity(intent);
```

`scroll_y` wins over `ayah` when both are passed, and both are consumed once by the target page —
`SurahListActivity` uses `EXTRA_SCROLL_Y` for **Continue Reading** and `EXTRA_AYAH` for search
results. `surah_id` is the single source of truth: it selects the starting page and drives the header.
`surah_name` sets the window title; the on-screen header is rendered from `SurahIndex` instead,
because it has to stay correct after the user pages to a surah no caller passed in. A mismatch is
logged, not displayed. The legacy `quran.surah` / `quran.ayah` keys are still accepted.

---

## 6. UI

| Spec | Where | Value |
| --- | --- | --- |
| Background | `colors.xml` → `quran_background` | `#FBF9F0` |
| Text | `colors.xml` → `quran_text` | `#1A1A1A` |
| Highlight | `colors.xml` → `quran_highlight` | `#D0ECE4` |
| Header | `colors.xml` → `quran_header` | `#7A7A7A` |
| Buttons | `colors.xml` → `quran_button` | `#2D7D46` |

`colors.xml` is the single definition of the **light** palette; `res/values/quran.xml` holds only
strings and the theme. Supporting shades (`quran_button_pressed`, `quran_on_button`,
`quran_divider`, `quran_badge`, `quran_error`) are derived from those five. The **dark** palette
lives in `QuranThemeColors` (background `#121212`, text `#E0E0E0`, chrome `#A0A0A0`, derived
divider/badge/highlight shades) and is applied programmatically because the user toggles it at
runtime; the XML colours are the light-mode defaults.

**Reader** (`activity_quran.xml` + `fragment_quran_page.xml`), top to bottom:

1. Toolbar — back arrow, `2 of 114`, then the four reading controls: **font −**, **font +**
   (16–44 sp in 1 sp steps, persisted), **bookmark** (saves `last_read_surah_id` + the page's live
   `scrollY`) and the **moon/sun theme toggle**.
2. Header — **Surah Name left**, **Juz Number right**, both in the chrome colour. A surah spanning
   several juz shows a range (`الجزء ١–٣`).
3. Body — a `ViewPager2` pinned LTR; each of the 114 pages is a `QuranFragment` with one
   `ScrollView` → `TextView`, centred, explicitly RTL, `quran_font.ttf` applied from Java (an asset
   path cannot be referenced from XML), selectable so an ayah can be copied. Swiping left/right
   changes surahs; `FragmentStateAdapter` keeps memory flat by destroying distant pages.
4. Footer — **Page Number** centred. Multi-page surahs show a page range.

Header, toolbar and footer rows pin `android:layoutDirection="ltr"` so "left" and "right" stay
physical in every device locale, while the Arabic body pins RTL independently. The pinned header and
footer follow the selected page through `onPageSelected`, so swiping never leaves stale labels.

**Index** (`activity_surah_list.xml` + `item_surah.xml` + `item_search_result.xml`) — number badge,
transliteration, English meaning, `286 Ayahs · Madinah · Juz 1`, and the Arabic name on the right.
Whole row is the click target (`clickable` + `focusable`) for TalkBack and d-pad. Above the list:
the **SearchView** (filters surah names while typing; submitting runs `searchQuran` and swaps the
list to matching ayahs — tapping a result opens the reader at that ayah) and the theme toggle.
Floating over the list's bottom corner: the green **Continue Reading** button, visible only while a
bookmark exists. Attribution line at the foot.

Language policy: mushaf labels are Arabic, navigation chrome is English. Both live in
`res/values/quran.xml` and can be flipped without touching a layout.

---

## 7. Threading, state and error handling

* The first-run copy runs on a worker thread; `prepareDatabase()` must never be called on the main
  thread. Each `QuranFragment` owns a single-thread executor for its surah read, so neighbouring
  pages load in parallel; the index screen's full-text search runs on its own worker.
* Results posted to a destroyed/detached fragment or finishing activity are dropped, so rotating or
  swiping away mid-load cannot crash.
* Rotation: `ViewPager2` restores its selected page and each `QuranFragment` restores its exact
  scroll offset through `onSaveInstanceState`. One-shot targets (bookmark `scroll_y`, search
  `ayah`) are consumed after the first render so they never re-jump.
* Body views set `android:saveEnabled="false"` so the fragment's explicit scroll restoration is the
  only authority (a selectable `TextView` otherwise restores its own state and fights it).
* Both screens handle Android 15/16 enforced edge-to-edge through `utils/EdgeToEdgeInsets`, which
  applies system-bar, cutout and IME insets on top of each root's design padding and keeps the bar
  icons dark on the paper background.
* Errors are always actionable: a missing/corrupt database offers Retry; a missing font says exactly
  which asset path to check. Neither silently substitutes a system face nor shows a blank reader.

---

## 8. Licensing — read before distributing

* **Tanzil text** — CC-BY-3.0. Verbatim redistribution is permitted; changing it is not. Attribution
  is required and is already surfaced in-app.
* **Amiri Quran font** — SIL OFL 1.1. Embeddable and redistributable; the licence text must ship with
  it, and it does.
* **`QuranMetadata.java`** — the 604-page and 30-juz boundary tables are adapted from the **GPL-3.0**
  project `quran/quran_android` (`MadaniDataSource.kt`), with its licence preserved at
  `docs/licenses/quran_android-GPL-3.0.txt`. This predates the current change set and is the one
  GPL-derived component in the feature. **Review GPL compatibility against your distribution model.**
  If it is incompatible, replace those two integer tables with data you have the rights to use — they
  are the only GPL-derived values, and nothing else in the feature imports them except
  `SurahIndex`'s derived juz/page fields.

The surah names are now the Tanzil names (CC-BY-3.0), no longer the GPL-adapted ones.

---

## 9. Validation

```sh
python3 -m unittest discover -s tests -v
```

54 SDK-independent checks across two files:

* `tests/test_offline_quran.py` — metadata completeness and monotonicity, known page/juz boundaries,
  the 114-entry Java arrays, the conventional 28-Madinan classification, the real bundled database
  (integrity, schema, all 6236 keys in order, the exact query contract, bound parameters, basmallah
  semantics, recorded provenance, verbatim Uthmani marks), font glyph coverage of both the database
  text and the Arabic chrome, layout structure and required colours, manifest registration, the
  offline guarantee, licence files, and in-app attribution.
* `tests/test_quran_wiring.py` — resolves every `R.id`/`R.string`/`R.color`/`R.drawable` reference in
  Java and every `@color`/`@string`/`@id` reference in XML against the resources actually defined in
  `res/`, checks `findViewById` ids against the layout each activity inflates, verifies
  `getString(...)` format arity at every call site, and confirms no network API survives.

With a JDK present, `CompiledBehaviourTests` additionally compiles and executes `QuranMetadata` and
`SurahIndex` and asserts 114 surahs, 6236 ayahs, the juz/page ranges and the rejection of invalid ids.

**What these cannot prove:** that the app compiles against the Android SDK, or that it renders
correctly. All 41 Java files were parsed for syntax and every cross-class call was checked against the
declared signatures, but no `javac`/`android.jar` was available in this workspace.

Then, with JDK 17 and Android SDK 36:

```sh
bash gradlew :app:assembleDebug :app:lintDebug
```

### Device checklist

1. Fresh install in airplane mode: open surahs 1, 2, 9, 55 and 114; check ayah counts, the basmallah
   header (present for 2, absent for 1 and 9) and glyph rendering against a trusted mushaf.
2. Swipe from surah 1 towards 114 and back; confirm the header, juz, page and position follow the
   selected page and that no new activity is stacked (Back exits the reader in one press).
3. Tap the bookmark icon deep inside surah 2, return to the index and tap **Continue Reading**;
   confirm the reader reopens surah 2 at the saved scroll offset. Kill and relaunch the app first —
   the bookmark must survive.
4. Font: tap +/− and confirm the size changes live on the current page and survives relaunch.
   Theme: toggle the moon/sun icon on both screens; confirm both palettes (`#FBF9F0`/`#1A1A1A` vs
   `#121212`/`#E0E0E0` with `#A0A0A0` chrome) and that the choice survives relaunch.
5. Search: type "kaw" and confirm the surah list filters; submit a Quranic word and confirm ayah
   results appear with `Surah 2:255`-style references; tap one and confirm the reader opens there.
6. Rotate while loading and while deep-scrolled in surah 2; confirm the surah and scroll offset survive.
7. Kill the process mid first-run copy; relaunch and confirm the `.installing` temp file is discarded
   and the copy completes.
8. On a debug build, corrupt the installed `.db` and confirm it is recovered from the asset.
9. Check TalkBack on both screens, large system font sizes, and RTL/physical left-right placement with
   the device language set to Arabic.
10. On API 36, check portrait/landscape, gesture and three-button navigation, cutouts and large-screen
   resizing — header and footer must never slide under a system bar.

---

## 10. File inventory

| File | Role |
| --- | --- |
| `quran/QuranDatabaseHelper.java` | asset copy + validation, `getSurahText(int)`, `searchQuran(String)`, caching |
| `quran/SurahIndex.java` | 114 surahs in Java: names, meanings, counts, juz/page, Makkah/Madinah |
| `quran/QuranMetadata.java` | ayah counts and the 604-page / 30-juz mapping |
| `quran/QuranSettings.java` | `SharedPreferences` wrapper: font size, dark mode, bookmark (`last_read_surah_id` + `scrollY`) |
| `quran/QuranThemeColors.java` | light/dark palettes applied programmatically on every Quran view |
| `activity/SurahListActivity.java` | index screen: list, SearchView, full-text results, Continue Reading, theme toggle |
| `adapter/SurahListAdapter.java` | row binding, name filtering, theme-aware colours |
| `adapter/AyahSearchAdapter.java` | full-text search result rows |
| `activity/QuranActivity.java` | reader: `ViewPager2` host, pinned header/footer, font/bookmark/theme toolbar |
| `activity/QuranFragment.java` | one surah page: load, scroll, font size, theme |
| `utils/EdgeToEdgeInsets.java` | shared target-36 edge-to-edge inset handling |
| `AppClass.java` | background first-run install at app launch |
| `activity/MainActivity.java` | home Quran tile now opens the index |
| `res/layout/activity_surah_list.xml`, `item_surah.xml`, `item_search_result.xml`, `activity_quran.xml`, `fragment_quran_page.xml` | layouts |
| `res/values/colors.xml`, `quran.xml` | light palette, strings, `QuranTheme` |
| `res/drawable/bg_quran_nav_button.xml`, `bg_surah_row.xml`, `bg_surah_badge.xml`, `bg_quran_button.xml` | backgrounds |
| `res/drawable/ic_bookmark.xml`, `ic_font_increase.xml`, `ic_font_decrease.xml`, `ic_moon.xml`, `ic_sun.xml`, `ic_search.xml` | toolbar icons |
| `res/color/quran_nav_button_text.xml` | button text colour state list |
| `tools/build_quran_db.py` | regenerates the database asset from the Tanzil source |

To reuse in another app, copy the `quran/` package, both activities, the fragment, the adapters, `EdgeToEdgeInsets`,
the layouts, the Quran colour/string/theme resources and the two assets; register both activities with
`@style/QuranTheme`; and merge (not replace) the `prepareDatabase()` startup hook into your own
`Application.onCreate()`.

`assets/quran/index.html` is an unrelated earlier web prototype that loads fonts from a CDN. It is not
used by this feature and can be deleted.
