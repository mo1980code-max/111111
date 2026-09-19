# Clocks: three sections, local previews, permanent unlocks

The clock area is split into the three sections the app is specified with, and every clock in them is a
*local* design: dial art from `res/drawable`, browser preview from `assets/previews/clock/`. No clock
screen, tile or preview ever touches a URL.

```
Home ──► ClockFuntionActivity (three sections, no ads here)
            ├─ Analog Clock  ──┐
            ├─ Digital Clock  ─┼──► ClockCardActivity (isWhich = 0 / 1 / 2)
            └─ Smart Clock   ──┘        2-column grid, native card every 8 rows
                    └─ tap a tile ──► ContentAccess
                                        ├─ available ─────────────► EditorActivity (no ads at all)
                                        └─ locked ──► dialog ──► rewarded ad ──► unlocked forever
```

## Sections, ids and the free tier

| Section | Rows | Free (`AdConfig.FREE_CLOCKS_PER_SECTION`) | Rewarded | Ids | Preview folder |
|---|---|---|---|---|---|
| Analog | 14 | 9 | 5 | `analog_01` … `analog_14` | `previews/clock/analog/` |
| Digital | 12 | 9 | 3 | `digital_01` … `digital_12` | `previews/clock/digital/` |
| Smart | 12 | 9 | 3 | `smart_01` … `smart_12` | `previews/clock/smart/` |

The tables are `utils/GetClocks.java` (`ANALOG`, `DIGITAL`, `SMART`), built through the `Unlockable`
interface (`catalog/Unlockable.java`): `getUnlockId()`, `getPreviewAsset()`, `isFreeByDefault()`,
`isClockContent()`. Digital and Smart were extended with extra rows on purpose — reusing an existing
renderer style with a different colourway — so a section is not "9 free and one extra": each has a
meaningful rewarded tail. No new rendering code was invented for them.

**Ids are the storage keys.** `clock:analog_07` is what `unlock/UnlockStore.java` writes, so appending
rows is safe and re-ordering them is not (`tests/test_catalog_ads_unlocks.py` fails if the ids drift
away from the preview files on disk).

**Style ≠ position.** The editor is driven by `textClockPosition`, which must stay the *renderer* style
index (`0..9` digital, `0..10` smart). `ClockCardActivity` therefore stores `item.getStyle()`, never the
list position; several catalog rows intentionally share a style. `res/drawable` covers
`digital_thumb_1..10` and `smart_thumb_1..11`, and the analog dials use
`clock_bg_1..14`/`clock_hour_1..14`/`clock_minte_1..14`/`clock_second_1..14`.

## Locked tiles

`adapter/LockOverlay.java` gives all four grids one behaviour:

* available → the tile is drawn at full brightness; an analog tile renders the live
  `viewUtils/AnalogClock` (and stops updating when recycled or locked);
* locked → the live view is switched off, the local preview is shown under `@drawable/bg_lock_scrim`
  with a padlock pill (`@drawable/ic_locked` + `@string/content_locked`);
* a locked clock cannot be opened, edited or applied — tapping it only ever shows the dialog of
  `docs/ADS.md`, and the list rebinds immediately after a reward so the padlock disappears.

The artwork of both states comes from the APK, so a locked tile renders identically in airplane mode.

## Previews

`tools/build_local_previews.py` renders every preview (480×480 PNG) from the same tables the app uses:

```bash
python3 tools/build_local_previews.py            # rewrite previews + wallpapers + index.json
python3 tools/build_local_previews.py --check    # verify every expected asset exists, write nothing
```

It draws real clock faces (hands at a fixed time, palette from the table) and, for the wallpaper
sections, its own artwork. Arabic is shaped with uharfbuzz + freetype-py; if those libraries are missing
the script degrades to text-free panels so a build never fails on tooling. Pillow is a *dev* dependency
only — nothing at runtime needs it.

## Offline behaviour

Free clocks and any clock already unlocked on the device stay fully usable without a network. Only the
*offer* of an additional clock needs the network, and its failure is reported with a message instead of a
silent no-op (`@string/ad_offline_message`, `@string/ad_unavailable_message`, `@string/ad_busy_message`).

## Verification

```bash
python3 -m unittest discover -s tests -v
python3 tools/build_local_previews.py --check
```

`ClockCatalogTests` in `tests/test_catalog_ads_unlocks.py` checks row counts, free tier, id ↔ preview
parity, style coverage, and that the colours in `GetClocks` are exactly the colours the previews were
rendered with.
