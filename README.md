# أذكاري - Dhikr companion

A premium, fully offline Arabic dhikr app for Android: adhkar of the morning and evening, a tasbeeh
counter, the user's own dhikr, and a floating dhikr card that appears gently over whatever app is
in front of you and disappears the moment you touch it.

No account, no backend, no ads, no analytics, no tracking, no network permission. Everything -
content, settings, counters - lives on the device.

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

## Stack

Kotlin 2.1.20 · Jetpack Compose (BOM 2025.04.01) · Material 3 · MVVM · Room 2.7.1 · DataStore ·
Hilt 2.56.2 · Navigation Compose · Coroutines/Flow · WindowManager · AlarmManager (inexact) ·
AppWidgetProvider · Java 17 · AGP 8.13.2 / Gradle 8.13 · compileSdk & targetSdk 36 · minSdk 23.

## Build

Requires **JDK 17** and the Android SDK (platform 36):

```bash
./gradlew clean assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease          # minified + resource-shrunk
```

Static audit suite (no JDK needed, runs anywhere Python 3 does):

```bash
python3 -m unittest discover -s tests -v
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

There is deliberately no `INTERNET` permission, no exact-alarm permission, no accessibility
service, no usage-stats access and no notification listener.
