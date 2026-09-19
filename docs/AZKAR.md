# Azkar (standalone feature)

A completely independent main section of the app, alongside **Quran** and
**Islamic Wallpapers**. Nothing in the Quran or Wallpaper features was redesigned,
moved or removed to add it.

## Navigation

```
Home (Quran | Azkar | Islamic Wallpapers …)
  └─ tap Azkar ──► AzkarHomeActivity ("Azkar")
                      ├─ Morning Azkar ─┐
                      ├─ Evening Azkar ─┼──► AzkarListActivity (one per category)
                      └─ Tasbeeh ───────┘
```

The Azkar home icon (`frameAzkar` in `activity_select_function.xml`) is styled exactly
like the Quran button: same 52 dp pill, same 30 dp icon + 18 sp label rhythm, in the
Azkar emerald with the tasbeeh-bead `ic_azkar` icon.

## Mandatory sources

The Arabic text, order, numbers and **per-item repetition counts** were transcribed from:

| Category | Items | Source |
|---|---|---|
| Morning Azkar | 31 | https://www.islambook.com/azkar/1/أذكار-الصباح |
| Evening Azkar | 30 | https://www.islambook.com/azkar/2/أذكار-المساء |
| Tasbeeh | 17 | https://www.islamiokul.com/arabic/zikirler.html |

The URLs live in three places so they can never be silently dropped: `assets/azkar.json`
(`sources`), `AzkarRepository.SOURCE_*` and `res/values/azkar.xml` (header comment).
`tests/test_azkar.py` asserts all three agree.

Morning counts: `1,3,3,3,1,1,3,4,1,7,3,1,1,3,3,3,1,3,1,1,3,10,3,3,3,3,1,1,100,100,100`
Evening counts: `1,1,3,3,3,1,1,3,4,1,7,3,1,1,3,3,3,1,3,1,1,3,10,3,3,3,3,100,1,100`
Tasbeeh counts: `100 × 17`

Evening is deliberately **not** assumed to match Morning (extra "آمن الرسول" item, different
tail order).

## Data & offline behaviour

* `app/src/main/assets/azkar.json` — the whole feature content; the app works fully offline.
* `azkar/AzkarRepository.java` — parses the asset once, merges persisted counters into fresh
  `AzkarItem`s. Totals come from the parsed arrays; no total is hard-coded anywhere.
* `azkar/AzkarItem.java` — `id, category, order, arabicText, repeatCount, virtue` (imported,
  immutable) + `currentCount` (persisted). `isCompleted()` ⟺ `currentCount == repeatCount`.
* `azkar/AzkarProgressStore.java` — `SharedPreferences` file `azkar_prefs`, keys
  `azkar_count_<category>_<id>`. Every tap persists immediately (`apply()`), so progress
  survives closing the app or leaving the page. There is intentionally **no daily reset**:
  counts stay until the user taps **Reset** on the item.
* `azkar/AzkarFonts.java` — reuses the bundled Amiri Quran font for the dhikr text (OFL,
  already shipped for the Quran reader).

## Counter behaviour (per item)

* Tap the large counter button → `currentCount + 1`, clamped at that item's own
  `repeatCount` (never beyond). Further taps on a completed item are ignored.
* Reaching the target flips the card to the subtle green tint + emerald stroke and the
  counter to solid emerald with **✓ Completed** (one short button pulse, nothing more).
* **Reset** (visible whenever `currentCount > 0`) returns the item to `0 / N`.
* Only the counter button counts — the rest of the card is inert while scrolling.

## Progress

* Category header: `"X / N Completed"` + progress bar, computed from the live items.
* All complete → emerald **"All Azkar Completed ✓"** banner.
* Azkar home cards show the same per-category progress, refreshed in `onResume()`.

## Design

* Palette (`colors.xml`, `azkar_*`): deep emerald `#0B3D2E`, emerald `#0E7C5B`,
  soft gold `#C9A227`, ivory `#FAF7EE`, warm white `#FFFDF7`.
* Cards: 16–18 dp radius, hairline strokes, 2–3 dp elevation, generous spacing.
* Dhikr text: RTL, 20 sp Amiri, extra line spacing; virtue line 13 sp muted.
* Mobile-first: 64 dp counter buttons, 48 dp reset targets, one-handed taps.

## Files owned by this feature

```
app/src/main/assets/azkar.json
app/src/main/java/com/clock/livewallpaper/azkar/*.java (4 files)
app/src/main/java/com/clock/livewallpaper/activity/AzkarHomeActivity.java
app/src/main/java/com/clock/livewallpaper/activity/AzkarListActivity.java
app/src/main/java/com/clock/livewallpaper/adapter/AzkarAdapter.java
app/src/main/res/layout/activity_azkar_home.xml
app/src/main/res/layout/activity_azkar_list.xml
app/src/main/res/layout/item_azkar.xml
app/src/main/res/values/azkar.xml
app/src/main/res/drawable/ic_azkar*.xml, bg_azkar_*.xml, azkar_progress.xml
```

Touched, not owned: `AndroidManifest.xml` (2 activities), `MainActivity.java` + 
`activity_select_function.xml` (the `frameAzkar` button only), `colors.xml` (appended
palette). Verified with:

```
python3 -m unittest discover -s tests -v
```
