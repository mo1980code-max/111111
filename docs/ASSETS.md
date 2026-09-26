# Assets

Everything the app displays ships inside the APK. There is no download step, no remote font, no
remote image and no network permission, so every asset below is also the offline story.

## Religious content

| File | Content | Rule |
|---|---|---|
| `app/src/main/assets/azkar.json` | 31 morning, 30 evening, 17 tasbeeh items with `text`, `repeat`, `order`, `virtue` and the section `sources` | **Never edited.** Copied verbatim into Room. See [AZKAR.md](AZKAR.md) |
| `app/src/main/assets/databases/quran.ar.uthmani.db` | Tanzil Uthmani text (1.5 MB) | Verified content inherited by the project; kept even though the current UI has no Quran reader |
| `app/src/main/assets/wallpapers/` | 36 images + `index.json` | Verified imagery inherited by the project; kept |

`tests/test_content_integrity.py` pins `azkar.json` by SHA-256 (`978c4aa0…3063f`) and also compares
it against the committed blob, so any edit - deliberate or accidental - fails the suite.

## Fonts

| File | Face | Licence |
|---|---|---|
| `res/font/cairo_regular.ttf` | Cairo - UI text | `docs/licenses/Cairo-OFL-1.1.txt` |
| `res/font/tajawal_regular.ttf` | Tajawal - numerals, secondary UI | `docs/licenses/Tajawal-OFL-1.1.txt` |
| `res/font/amiri_quran.ttf` | Amiri Quran - dhikr and Quranic text | `docs/licenses/AmiriQuran-OFL-1.1.txt` |

All three are bundled font resources used through `FontFamily` in `ui/theme/Type.kt`. There is no
`fontProviderAuthority` anywhere: downloadable fonts would be a network dependency.

`docs/licenses/` also keeps `Tanzil-Uthmani-CC-BY-3.0.txt` and `quran_android-GPL-3.0.txt` for the
inherited Quran database.

## Drawables and icons

41 vector drawables, all original geometry, no third-party icon pack:

* `tools/build_icon_set.py` generates the 32 UI icons plus the brand mark from one stroke
  specification (2 px stroke, round caps, 24 dp grid), so the set stays visually consistent. Icons
  that carry direction (`ic_chevron`, `ic_arrow_back`, `ic_share`, `ic_reset`) are marked
  `android:autoMirrored="true"` for RTL.
* `tools/build_launcher_icons.py` renders the adaptive-icon PNG fallbacks for
  `mipmap-{m,h,xh,xxh,xxx}dpi`.
* Hand-written: the splash icon, the launcher foreground/monochrome vectors, the widget backgrounds
  and `ic_notification`.

Re-running either script is safe: the output is deterministic and matches what is committed.
`tests/test_resource_references.py` fails if a drawable is referenced but missing - or declared and
never used.

## Strings

`res/values/strings.xml` holds **236 strings + 1 string-array**, all Arabic; it is the only place
where user-facing copy lives. The audit suite enforces three rules:

1. no Latin letters in the copy (except the licence names `SIL` / `OFL`),
2. no Arabic string literal anywhere in Kotlin - dates, durations and the Hijri month names are
   formatted from resources too,
3. no dead strings and no dangling `stringResource` argument count.

Colours are split deliberately: `res/values/colors.xml` only carries the platform-level colours a
theme or a widget needs (window background, splash, launcher, widget surfaces) with a
`values-night` variant; the in-app palette lives in `ui/theme/Color.kt` where Compose can use it.
