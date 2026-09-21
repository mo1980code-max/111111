# Ads, unlocks and the free tier

Monetisation for `com.clock.livewallpaper`, written so that the religious content of the app is never
interrupted. Every rule below is enforced in code and asserted by `tests/test_catalog_ads_unlocks.py`.

## Formats in use

| Format | Used? | Where | Guarded by |
|---|---|---|---|
| Native (in-feed) | yes | Home screen (one card), clock grids, "all wallpapers" grid | `AdPolicy.nativeAdsAllowed()`, `AdPolicy.nativeSlotPositions()` |
| Rewarded | yes, only as a reward | the unlock dialog of a locked clock or wallpaper | `UnlockPrompt`, `AdsManager.requestUnlock()` |
| App open | yes, restricted | warm background → foreground return only | `AdPolicy.canShowAppOpen()`, `AppForegroundWatcher` |
| Banner | **no** | removed from every screen | layouts contain no `AdView` |
| Interstitial | **no** | removed from the source entirely | `test_interstitials_are_gone` |

```
Home (MainActivity)
  ├─ one native card, below the four section buttons, in its own paper card
  ├─ Clocks  ──► ClockFuntionActivity (no ads) ──► ClockCardActivity grid ── native every 8 rows
  ├─ Wallpapers ─► WallpaperCategoryActivity (7 rows → no ad) ─► WallpaperActivity
  │                                                        └─ "كل الخلفيات" (36 rows) ── native every 8
  ├─ Quran   ──► 100 % ad free, every screen                    (AdPolicy.enterQuranScreen)
  └─ Azkar   ──► 100 % ad free, no interstitial/rewarded near the counter
```

## Configuration

All numbers live in `ads/AdConfig.java` — nothing else declares an ad unit id (asserted by
`test_ad_unit_ids_are_the_test_ids_defined_once`).

| Constant | Value | Meaning |
|---|---|---|
| `ADS_ENABLED` | `true` | master switch; `false` ships a completely ad-free build |
| `APP_OPEN_AD_UNIT_ID` | `ca-app-pub-3940256099942544/9257395921` | Google **demo** id |
| `REWARDED_AD_UNIT_ID` | `ca-app-pub-3940256099942544/5224354917` | Google **demo** id |
| `NATIVE_AD_UNIT_ID` | `ca-app-pub-3940256099942544/2247696110` | Google **demo** id |
| `NATIVE_AD_INTERVAL` | `8` | content rows between two native ads (spec allows 6–10) |
| `NATIVE_AD_MIN_ITEMS` | `10` | shorter lists get no ad at all |
| `NATIVE_AD_REQUEST_COOLDOWN_MS` | `60 000` | never request the same placement more often |
| `NATIVE_AD_TTL_MS` | `60 min` | a cached native ad is reloaded after this age |
| `APP_OPEN_MIN_INTERVAL_MS` | `5 min` | two app open ads are never closer than this |
| `APP_OPEN_AFTER_REWARDED_MS` | `60 s` | no app open ad stacked right after a rewarded one |
| `APP_OPEN_AFTER_QURAN_MS` | `20 s` | leaving the Quran section shows nothing |
| `APP_OPEN_EXPIRY_MS` | `4 h` | an app open ad older than this is discarded (SDK guidance) |
| `APP_OPEN_LAUNCHES_BEFORE_FIRST_AD` | `3` | a fresh install is not greeted by an ad |
| `FREE_CLOCKS_PER_SECTION` | `9` | free clocks in Analog / Digital / Smart |
| `FREE_WALLPAPERS_PER_SECTION` | `3` | free images in every wallpaper section |

Debug builds always use the three official demo unit ids above. `AdConfig` is the single switch point:
`appOpenAdUnitId()`, `rewardedAdUnitId()` and `nativeAdUnitId()` return those ids when
`BuildConfig.DEBUG` is true, and the centrally colocated `RELEASE_*` placeholders in the same file for
a release build. Replace the placeholders together with the production units before publishing; no ad
unit id is scattered through an Activity or adapter. Keep `com.google.android.gms.ads.APPLICATION_ID`
in `AndroidManifest.xml` equal to the app id in `AdConfig` and register development test devices.

## Native ads

`ads/NativePlacement.java` owns exactly one `NativeAd` per placement and `ads/AdInsertingAdapter.java`
turns a content adapter into "content + ad rows":

* the card is `res/layout/ad_native.xml`, whose root **is** `com.google.android.gms.ads.nativead.NativeAdView`;
  headline, body, advertiser, icon, `MediaView` and call-to-action are all registered with the SDK, so
  impressions, clicks and the AdChoices icon are handled by Google, not by us;
* the card always shows the label `إعلان` (`@string/ad_label`) on a dark pill, sits on a light paper
  background (`@drawable/bg_ad_card`) with its own stroke, and app content never overlaps it;
* in a `GridLayoutManager` an ad row spans the full width (`SpanSizeLookup` in the wrapper);
* a row that has no ad renders **nothing**: the slot has zero height and no margins of its own, so a
  failed request, no fill or offline leaves no white strip behind;
* the wrapper asks for an ad only when its plan actually contains an ad row and re-plans on every data
  change (so an unlock that changes the row count re-computes the slots). Content rows are unconditional:
  while `AdPolicy.nativeAdsAllowed()` is false the wrapper returns the content rows with no ad row at all,
  so the grid always renders and an ad can never replace or hide content;
* the click never overlaps an app action: ad rows can only be inserted between list tiles, and no ad is
  placed inside the clock editor, the wallpaper preview or the app's dialogs.

Because of the short-list rule, the six individual wallpaper sections (6 images each) host **no** ad;
in-feed wallpaper ads appear in "كل الخلفيات" (36 images). The three clock sections (14/12/12) get one
card each. This is the intended reading of "every 8 items, never in a short list".

## Rewarded unlock: the only way anything is paid for

`catalog/ContentAccess.open()` is the single gate every clock tile and every wallpaper tile goes
through:

1. free tier, or already unlocked on this device → opens immediately, no ad layer involved at all;
2. otherwise `unlock/UnlockPrompt.java` shows a dialog — *"مشاهدة إعلان لفتح هذه الساعة"* /
   *"...هذه الخلفية"* with a decline button ("لاحقًا"). Tapping the tile itself never loads an ad;
3. the rewarded ad is shown only after that confirmation; on `onUserEarnedReward` the key
   (`clock:analog_07`, `wallpaper:mecca_5`, …) is written to `SharedPreferences` (`content_unlocks_v1`)
   by `unlock/UnlockStore.java` and the list rebinds, so the padlock disappears permanently;
4. no network, no fill, or a dismissed ad → the tile stays locked and a short Arabic message explains
   that free and already-unlocked content keeps working.

Opening a clock section, opening a wallpaper section and tapping any Azkar counter element show
**nothing**.

## Quran and Azkar

The Quran screens (`SurahListActivity`, `QuranActivity`, `QuranFragment`, search, bookmarks, audio)
call `AdPolicy.enterQuranScreen()` in `onCreate` and `AdPolicy.exitQuranScreen()` in `onDestroy`. While
the counter is above zero, `AdPolicy` refuses *every* format at the source: no load, no show, no app
open on a foreground return. The counter only reaches zero when the last Quran screen is gone, so a
rotation inside the reader cannot open a gap. After leaving, `APP_OPEN_AFTER_QURAN_MS` keeps the ad layer
quiet for a moment — the exit itself shows no ad, and the next one waits for a natural foreground return.

Azkar has no ad of any kind — not even the "isolated banner" the spec allowed. The dhikr text, the
counter buttons and the tasbeeh screen therefore contain no `AdView`, no request and no slot. The test
suite fails if any `Quran*`, `Surah*`, `Azkar*`, `Ayah*` file ever mentions the ads SDK again.

## App open

`ads/AppForegroundWatcher.java` counts started activities. Falling to zero marks the app backgrounded
and preloads an app open ad; rising *from* zero shows it — never on a cold start, never while the Quran
section is active, never while another ad is in flight, never within 60 s of a rewarded ad, never within
5 min of the previous app open ad, never before the third launch, and never on the return from a screen
the user asked for (live wallpaper picker, share sheet, gallery) — those call
`AdPolicy.markSystemHandoff()` so exactly that one opportunity is skipped.

## Verification

```bash
python3 -m unittest discover -s tests -v       # includes the ad/unlock contracts
```
