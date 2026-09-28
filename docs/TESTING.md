# Verification

```
python3 -m unittest discover -s tests -v      # 138 checks, ~12 s, no dependencies
python3 tools/dhikr-typecheck/check.py        # kotlinc type-check of the ad layer, ~13 s
```

## Why a static suite

The Android toolchain cannot run in this workspace: there is no Android SDK, and the Gradle
distribution, Google Maven and Maven Central are all unreachable from here, so
`./gradlew assembleDebug` cannot be executed and **no build result may be claimed**. On a machine
with JDK 17 and the Android SDK, the normal build is still the real gate:

```
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
```

The suite below deliberately covers the classes of mistakes a compiler *would* have caught, plus the
product rules a compiler never checks. It is written in plain `unittest`, reads the sources as text
and has no third-party dependency.

| File | Checks | What it stands in for |
|---|---|---|
| `test_resource_references.py` | 15 | `aapt2 link`: every `R.*` and `@type/name` resolves, manifest components exist, format arguments match call sites, Arabic-only copy, no dead strings or drawables, no hard-coded Arabic in Kotlin |
| `test_kotlin_structure.py` | 14 | `kotlinc` hygiene: package == directory, balanced brackets, imports resolve, are unique, are all used and none is missing, every project symbol used in a file is imported, Hilt annotations paired, no TODO / placeholder / empty function bodies |
| `test_api_contracts.py` | 9 | The type checker on our own API: 78 indexed top-level signatures, named arguments exist, required parameters are supplied, every route has a destination, every stateful screen collects with lifecycle |
| `test_product_contracts.py` | 84 | The specification: permission budget, no backend / analytics SDK / exact alarms / WorkManager / service, the whole advertising contract (28 checks, below), overlay dismiss-and-open-nothing, window context, quiet hours, boot + time-change restore, widget rules, RTL, splash, accessibility, Room safety |
| `test_content_integrity.py` | 16 | The religious content: azkar.json pinned by hash, counts 31/30/17, attributions present, verbatim seeding, preserved assets |

`test_product_contracts.py` breaks down as: `PermissionBudgetTest` 5, `OfflineTest` 6,
`AdvertisingContractTest` 28, `OverlayContractTest` 7, `ReminderContractTest` 8,
`WidgetContractTest` 4, `ThemeAndLayoutTest` 5, `AccessibilityTest` 4, `DataContractTest` 5,
`ReleaseHardeningTest` 12.

## The advertising contract

`AdvertisingContractTest` is the written form of the ad rules, so a future edit that breaks one of
them fails here instead of in production: only Google test ad ids exist anywhere in the repository
and the application id lives only in the manifest; the SDK is confined to `ads/`; no `@Singleton`
holds an `Activity`; consent runs through UMP with a foreground activity and can never run twice at
once; debug builds do not bypass consent; nothing is requested before `canRequestAds` **and**
initialisation **and** finished onboarding; the SDK is initialised in exactly one idempotent place;
the banner is created once, sized, destroyed, and reports both outcomes; the native ad is labelled,
bound and destroyed; every full-screen ad passes through one gate in the coordinator; the
interstitial waits for a finished reading session; app open skips the cold start and short trips;
the rewarded ad is user-initiated and rewards only from the earned-reward callback; system and
consent screens open a quiet window; the focused and legal screens carry no ads; Settings has no
debug or status surface and its only ad control is Google's privacy options; logging is debug-only
under a single tag, covers every event the report needs, and never carries user content.

## Type-checking the ad layer

`tools/dhikr-typecheck/check.py` compiles the 13 files of
`app/src/main/java/com/clock/livewallpaper/ads/` with `kotlinc` against hand-written stubs of the
libraries they use (`tools/dhikr-typecheck/stubs_extra/`, plus the shared stubs of the sibling
harness where they are identical). The stub signatures for the Google Mobile Ads and UMP APIs were
transcribed from the official reference — `AdView`, `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`,
`InterstitialAd.load`, `RewardedAd.load/show(activity, listener)`, `AppOpenAd.load`,
`AdLoader.Builder.forNativeAd/withAdListener/withNativeAdOptions`, `NativeAdView` and its asset
setters, `UserMessagingPlatform.requestConsentInfoUpdate/loadAndShowConsentFormIfRequired/showPrivacyOptionsForm`,
`ConsentInformation.canRequestAds/privacyOptionsRequirementStatus`.

What it proves: the ad layer is internally consistent, and its calls match the *documented* shape of
the SDK — misspelled members, wrong argument types, missing arguments and bad overrides are caught.

What it does **not** prove: it is not a Gradle build. It does not run kapt, so Hilt graph errors
(missing bindings, scope mismatches) are invisible to it; it does not resolve the real `.aar`
artifacts, so a stub that drifts from the shipped SDK would hide a real error; it does not compile
the other 70 files of the app (their Compose and platform surface is far larger than the stubs
cover); and a clean type-check says nothing whatsoever about whether an ad ever loads.

## Non-vacuity

Audits that always pass are worthless, so the heaviest ones were checked against injected faults;
each failed as intended and the file was restored immediately after.

Older release audits:

* deleting the `StatusPill` import from `ReadingScreen.kt` → `SymbolVisibilityTest` reported
  `('…/ReadingScreen.kt', 'StatusPill')`;
* renaming `text =` to `caption =` in one `SecondaryButton(…)` call in `AboutScreen.kt` →
  `ComposableCallSiteTest` reported both the unknown argument and the missing required one;
* deleting the `LocalContext` import from `Dialogs.kt` → the import audit reported it as used
  without an import;
* the three release-audit fixes, reverted one by one - the periodic alarm back to
  `System.currentTimeMillis()`, `keepPendingChain = true` dropped from `AppStartup`, and the
  overlay removed through a freshly resolved `WindowManager` - each failed exactly the
  `ReleaseHardeningTest` case written for it (and the first also tripped the unused-import audit).

The advertising contract, tested the same way:

| Injected fault | Test that failed |
|---|---|
| banner id changed to a publisher-looking `ca-app-pub-…` | `test_every_ad_id_in_the_repository_is_a_google_test_id`, `test_ad_config_declares_every_format_once` |
| `adsReady` no longer depends on `canRequestAds` | `test_nothing_is_requested_before_consent_initialisation_and_onboarding` |
| a `canRequestAds / adsReady` status row added to Settings | `test_settings_has_no_debug_or_status_surface` |
| `AdLog.d` logging outside the `BuildConfig.DEBUG` guard | `test_logging_is_debug_only_and_carries_one_tag` |
| `AndroidView(onRelease = { })` — native view not destroyed | `test_native_ad_is_labelled_bound_and_destroyed` |

And the type-check harness, so that "TYPE-CHECK CLEAN" means something:

| Injected fault | Reported as |
|---|---|
| `initializer.ensureInitialised()` (typo) | `unresolved reference 'ensureInitialised'` |
| `RewardedAd.show()` without the activity | `argument type mismatch` |
| `loadAd(AdRequest.Builder())` instead of `.build()` | `argument type mismatch: … but 'AdRequest' was expected` |
| `consentInformation.canRequestAds` as a property | `function invocation 'canRequestAds()' expected` |

## What the suite cannot do

It does not expand kapt-generated Hilt/Room code, does not link against the real SDK artifacts and
does not run anything on a device. Those remain for `./gradlew assembleDebug`, a real install on
Android 13+ (runtime notification permission, overlay grant, widget placement, TalkBack pass) and a
Logcat session filtered on `AdMobDebug` to see whether an ad actually loads.
