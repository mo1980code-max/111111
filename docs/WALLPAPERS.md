# Wallpapers: local gallery, six sections, permanent unlocks

Every wallpaper ships inside the APK. The remote catalog the old build read (`assets/wallpaper.json`,
`assets/wallpapernew.json`, `ResponseWallpaper*`/`ImageUrlsItem` models, Glide/okdownload loading and the
"Download Wallpaper" button state) is **deleted**: there is no image URL left in the wallpaper flow.

```
Home ──► WallpaperCategoryActivity (6 sections + "كل الخلفيات", no ads on open)
            └─ tap a section ──► WallpaperActivity (2-column grid of that section)
                                    ├─ tap a free tile ─────────────► SetWallpaperActivity
                                    └─ locked tile ──► dialog ──► rewarded ──► unlocked forever
                                                        └── Set as live wallpaper / share (offline)
```

## Sections

| Id | Section | Images | Free | Rewarded |
|---|---|---|---|---|
| `mecca` | مساجد مكة | 6 | 3 | 3 |
| `madinah` | مساجد المدينة المنورة | 6 | 3 | 3 |
| `aqsa` | المسجد الأقصى | 6 | 3 | 3 |
| `mosques` | مساجد أخرى | 6 | 3 | 3 |
| `minarets` | مآذن | 6 | 3 | 3 |
| `allah` | صور مكتوب عليها "الله" | 6 | 3 | 3 |
| `all` | كل الخلفيات (synthetic, catalog order) | 36 | 18 | 18 |

Files live in `app/src/main/assets/wallpapers/<section>/<section>_<n>.jpg` (36 files, about 3 MB in
total), ids are `mecca_1` … `allah_6`, and `wallpapers/index.json` is the manifest the Java catalog
parses. The free tier is `AdConfig.FREE_WALLPAPERS_PER_SECTION` = the first three of every section; the
`free` flag is stored per item in the manifest, so the offer and the gate cannot disagree — and an id is
only ever a key like `wallpaper:mecca_5`, never a position.

## Catalog

`catalog/WallpaperCatalog.java` parses `wallpapers/index.json` once per process into
`WallpaperSection` / `WallpaperEntry` (both implement `catalog/Unlockable.java`). It exposes
`sections()`, `sectionsWithAll()`, `items(sectionId)`, `title(sectionId)`, `byId(id)`,
`allEntries()` and `FREE_PER_SECTION`. A damaged manifest degrades to an empty list instead of crashing,
and `invalidate()` exists for tests.

## Tiles, locks and previews

`adapter/WallpaperAdapter.java` renders a tile from the asset itself through
`image/LocalImage.java` (async, two-pass `inSampleSize`, `RGB_565`, shared `LruCache`, view-tag race
check) at roughly 200 dp, never at full resolution. `adapter/LockOverlay.java` adds the locked look —
`@drawable/bg_lock_scrim` plus a padlock pill saying *"افتحها بالإعلان"* — and
`adapter/CategoryWallpaperAdapter.java` uses the section's own first free image as its cover, with a
caption stating the size of the section and its free tier (`٦ صور · ٣ مجانية`, Arabic-Indic digits via
`utils/ArabicDigits.java`).

Every section image doubles as its preview: there is no second, lower-resolution copy to keep in sync.

## Setting a wallpaper

`activity/SetWallpaperActivity.java` decodes the asset for the screen, copies it into the external cache
so `CustomWallpaper` and the share sheet get a real file, and stores that path in TinyDB under
`isWallpaper` — the mechanism the live wallpaper service already reads. Both buttons wait for the copy,
which is local I/O: no progress percentage, no "download" text, no failure state caused by the network,
and no ad next to the set / share controls (the old bottom container is gone from the layout).

## Ads in the wallpaper area

Only native cards inside a list, inserted by `ads/AdInsertingAdapter.java` every 8 rows and suppressed
below 10 rows (`docs/ADS.md`). Consequence worth knowing: a section holds 6 images, so the six sections
are ad free by the short-list rule, while "كل الخلفيات" (36 images) carries four cards. Opening the
section grid, opening a section and browsing images never triggers an ad, and nothing at all is shown on
the preview screen.

## Regenerating the artwork

```bash
python3 tools/build_local_previews.py                  # all sections + index.json
python3 tools/build_local_previews.py --section allah  # one section
python3 tools/build_local_previews.py --check          # verify every expected file exists
```

Makkah / Madinah / al-Aqsa images are AI-rendered base frames (kept outside the repository in
`~/staging/wallpapers/`) that the script grades twice — daylight and night — and retouches; the
"مساجد أخرى", "مآذن" and calligraphy sections are drawn procedurally, because an image model cannot be
trusted with Arabic text: `صور مكتوب عليها "الله"` is typeset with the Amiri Quran font, shaped through
uharfbuzz and rasterised through freetype-py, so the word الله is spelled correctly at every size.

## Verification

```bash
python3 -m unittest discover -s tests -v
```

`WallpaperAssetTests` asserts the six sections and their Arabic names, six images each with exactly the
first three free, id/file/title naming, that every file named in `index.json` exists in `assets/`, and
that the removed remote catalog, its models and its image libraries cannot come back.
