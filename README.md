# أذكاري - Dhikr companion

A premium, fully offline Arabic dhikr app for Android: adhkar of the morning and evening, a tasbeeh
counter, the user's own dhikr, and a floating dhikr card that appears gently over whatever app is
in front of you and disappears the moment you touch it.

No account, no backend, no analytics, no tracking. The content, the settings and the counters live
on the device and work with the network switched off; the only thing that goes online is the
advertising described below, and only after the user's consent choice allows it.

## Features

* **Floating dhikr card** over other apps (`SYSTEM_ALERT_WINDOW`): four themes (نور / زمرد / ليل /
  صفاء), three positions, font size, opacity, auto-dismiss (touch only, 5/10/15/30 s) and an
  optional haptic, with a live preview. Touching the card anywhere dismisses it and opens nothing.
* **Reminders** every 5/10/15/20/30/45 min, 1/2/3 h or a custom interval, with quiet hours (midnight
  crossing supported), recent-dhikr avoidance, and a notification fallback when the overlay
  permission is not granted.
* **Morning, evening and Friday** (سورة الكهف - "نورٌ بين الجمعتين ✦") daily reminders.
* **Adhkar reading** with per-item counters, progress and the original source under every text.
* **Tasbeeh** with presets, targets 33 / 100 / custom, a daily total that resets with the date, and
  haptics.
* **أذكاري**: create, edit and delete your own dhikr, with validation and a delete confirmation.
* **Two home-screen widgets**: a dhikr card with "ذكر آخر" rotation, and الصباح / المساء / المسبحة
  shortcuts.
* Arabic-first, true RTL, light and independently designed dark theme, Hijri + Gregorian date,
  TalkBack labels, ≥48 dp targets, font scaling, edge-to-edge.

## Advertising

The app carries Google AdMob advertising, integrated through one Hilt layer in
`com.clock.livewallpaper.ads` and governed by Google's User Messaging Platform (UMP).

* **Google test ad units only.** Every id in the repository is one of Google's published test ids -
  the application id `ca-app-pub-3940256099942544~3347511713` in the manifest, and the banner,
  interstitial, rewarded, rewarded-interstitial, native and app-open test units in `AdConfig.kt`.
  They must be replaced with real units before publishing.
* **Consent first.** UMP is asked on every launch from the foreground activity; a form is shown when
  it is required; the SDK is initialised and an ad is requested only while `canRequestAds()` is
  true. Onboarding must be finished first.
* **Where ads appear.** A native card among the cards of the home screen (labelled إعلان), an
  adaptive banner in the adhkar list, one interstitial at most when a reading session ends, and an
  app open ad on a real return to the app. The reading, tasbeeh, editor, onboarding, privacy and
  about screens carry none.
* **One gate.** `AdsCoordinator` owns the frequency caps and the quiet windows around consent forms
  and Android permission dialogs; screens never talk to the SDK.
* **Privacy options.** When UMP reports that a privacy options form is required, and only then,
  Settings shows a single row that opens Google's form. There is no debug or status surface.
* **Debug logging.** Debug builds log the init, the UMP result, each request, load, failure (code,
  domain, message, response info), impression, click and full-screen event under the Logcat tag
  `AdMobDebug`. Release builds log nothing, and no user content is ever logged.

## Stack

Kotlin 2.1.20 · Jetpack Compose (BOM 2025.04.01) · Material 3 · MVVM · Room 2.7.1 · DataStore ·
Hilt 2.56.2 · Navigation Compose · Coroutines/Flow · WindowManager · AlarmManager (inexact) ·
AppWidgetProvider · Google Mobile Ads 25.4.0 + UMP 4.0.0 · Java 17 · AGP 8.13.2 / Gradle 8.13 ·
compileSdk & targetSdk 36 · minSdk 23.

## Build

Requires **JDK 17** and the Android SDK (platform 36):

```bash
./gradlew clean assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease          # minified + resource-shrunk
```

Static audit suite (no Android SDK needed, runs anywhere Python 3 does):

```bash
python3 -m unittest discover -s tests -v      # 138 checks
python3 tools/dhikr-typecheck/check.py        # kotlinc type-check of the ad layer (needs kotlinc)
```

## Documentation

| Document | Content |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | module map, layers, startup, navigation, overlay, reminders, threading |
| [docs/AZKAR.md](docs/AZKAR.md) | the adhkar content, its sources and the rules that protect it |
| [docs/ASSETS.md](docs/ASSETS.md) | content, fonts, drawables, strings and their licences |
| [docs/TESTING.md](docs/TESTING.md) | what the audit suite verifies and what it cannot |

## Permissions

| Permission | Why | Optional |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | draw the floating dhikr card | yes - falls back to a notification |
| `POST_NOTIFICATIONS` | reminder notifications (Android 13+) | yes |
| `RECEIVE_BOOT_COMPLETED` | re-arm the enabled reminders after a reboot | - |
| `VIBRATE` | the short optional haptic | yes |
| `INTERNET` | required by the Google Mobile Ads SDK; nothing else uses it | - |

There is deliberately no exact-alarm permission, no accessibility service, no usage-stats access,
no notification listener, no location, no advertising-id permission and no backend of our own: the
app's own data never leaves the device.
