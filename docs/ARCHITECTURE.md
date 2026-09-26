# Architecture

Offline Arabic dhikr companion: Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, Hilt.
One Activity, one Compose navigation graph, no backend, no account, no analytics, no network code
of any kind.

```
app/src/main/java/com/clock/livewallpaper/
├── DhikrApplication.kt        @HiltAndroidApp, delegates to AppStartup
├── MainActivity.kt            the single Activity: splash, edge-to-edge, deep-link routing
├── core/                      Arabic formatting, Hijri date, day part, haptics, startup
├── data/
│   ├── local/                 Room entity, DAO, database, category enum
│   ├── prefs/                 DataStore models + SettingsRepository
│   ├── seed/                  AzkarSeedSource: assets/azkar.json -> rows, verbatim
│   └── DhikrRepository.kt     the only door to dhikr data
├── di/AppModule.kt            DataStore, Room, application CoroutineScope
├── notification/              channels + the two reminder notifications
├── overlay/                   the floating card: manager, view owner, composable, permission
├── reminder/                  inexact alarm scheduler, receivers, presenter
├── ui/
│   ├── theme/                 colours, type, shapes, DhikrTheme, overlay palettes
│   ├── components/            the shared Material 3 building blocks
│   ├── navigation/            Routes, NavHost, bottom bar
│   ├── util/                  clipboard / share, notification permission
│   └── screens/               one package per screen: Screen + ViewModel
└── widget/                    two app widgets and their refresh helper
```

70 Kotlin files. No `Service`, no `AccessibilityService`, no `UsageStatsManager`, no
`NotificationListenerService`, no `WorkManager`.

## Layers

```
Compose screen  ──collects──►  StateFlow<UiState>  ──►  ViewModel  ──►  Repository  ──►  Room / DataStore
     ▲                                                       │
     └──────────────── events (SharedFlow) ──────────────────┘
```

* **Screens** are stateless about persistence: they read one `UiState` via
  `collectAsStateWithLifecycle()` and call `viewModel::something`. No screen touches Room,
  DataStore, `AlarmManager` or `WindowManager` directly.
* **ViewModels** (`@HiltViewModel`) expose `StateFlow<UiState>` built with `stateIn(viewModelScope,
  WhileSubscribed(5_000), initial)`, plus a `SharedFlow` for one-shot events (a deletion, a finished
  tasbeeh round, a validation error).
* **Repositories** are the only place that knows about storage. `DhikrRepository` wraps the DAO and
  owns the product rules (seeding, reminder pick, validation, share/copy text). `SettingsRepository`
  wraps DataStore and exposes one flow per settings group plus a `snapshot()` for non-UI callers
  (alarms, widgets, receivers).
* **Hilt** provides everything from `SingletonComponent`: the `DataStore`, the Room database and a
  `@ApplicationScope CoroutineScope` (`SupervisorJob + Dispatchers.Default`) for work that must
  outlive a screen.

## Startup

`DhikrApplication.onCreate()` calls `AppStartup.start()` exactly once (guarded by an
`AtomicBoolean`). It registers the notification channels synchronously - cheap - and then, on the
application scope:

1. `DhikrRepository.ensureSeeded()` - idempotent, runs only when the stored seed version is older
   than the bundled one.
2. `ReminderScheduler.apply(settings.snapshot())` - re-arms exactly the schedules the user enabled
   (also after an app update or a settings restore).
3. `WidgetRefresh.request(context)` - one broadcast so existing widgets show fresh content.

Nothing blocks the main thread; the first frame does not wait for the database.

## Navigation and deep links

`ui/navigation/Routes.kt` is the single source of truth:

| Route constant | Path | Reached from |
|---|---|---|
| `ONBOARDING` | `onboarding` | first launch only |
| `HOME` | `home` | launcher, dhikr notification, dhikr widget |
| `ADHKAR` | `adhkar` | bottom bar |
| `READING_PATTERN` | `reading/{category}` | Adhkar, Home quick actions, widgets, daily reminders |
| `TASBEEH` | `tasbeeh` | bottom bar, quick-actions widget |
| `MY_DHIKR` | `my_dhikr` | bottom bar |
| `EDITOR_PATTERN` | `dhikr_editor?dhikrId={dhikrId}` | أذكاري (add / edit) |
| `SETTINGS` | `settings` | Home header |
| `OVERLAY_SETTINGS` | `overlay_settings` | Settings, Home permission card |
| `PRIVACY` | `privacy` | Settings |
| `ABOUT` | `about` | Settings |

Outside the app, a destination is opened with an **explicit** intent:
`MainActivity.routeIntent(context, route)` puts the route in `MainActivity.EXTRA_ROUTE`; the
Activity is `singleTop`, so `onNewIntent` re-delivers it and the graph navigates with
`launchSingleTop` + `popUpTo(HOME)`. `Routes.EXTERNAL_ROUTES` lists what may arrive that way and an
unknown value is ignored. No exported intent filter beyond the launcher one, and no implicit deep
link scheme - nothing external can drive the app.

## The floating card (signature feature)

```
alarm fires ──► ReminderPresenter.onPeriodicAlarm()
                 1. re-arm the next alarm FIRST (the chain can never die)
                 2. quiet hours?          -> stop
                 3. reminders disabled?   -> stop
                 4. pick a dhikr (recent ones excluded)
                 5. canDrawOverlays ? OverlayManager.showDhikrAsync(text)
                                    : DhikrNotifier.showDhikr(text)   (fallback)
                 6. remember the id, refresh the widgets
```

`OverlayManager` is a `@Singleton`, **not** a Service. It adds one `ComposeView` to the
`WindowManager` with `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_HARDWARE_ACCELERATED`,
width `0.90 × current window bounds` capped at 520 dp, gravity from the chosen position.
`OverlayViewOwner` gives that view the `Lifecycle`, `ViewModelStore` and `SavedStateRegistry`
owners Compose requires outside an Activity: `attach()` moves it to CREATED *before* `addView`,
`resume()` follows, and `destroy()` plus `disposeComposition()` run on removal.

Why this is legal from the background on Android 15/16 and needs no service: the window type
itself is the mechanism. The platform raises the importance of a process that owns a
`TYPE_APPLICATION_OVERLAY` window, the reminder receiver is already alive while `onReceive` /
`goAsync()` runs, and the restrictions added in recent releases target *foreground-service starts*
and *background activity launches* - the app starts neither. From API 30 the window is created
through `createDisplayContext(...).createWindowContext(TYPE_APPLICATION_OVERLAY, null)` so it
carries the configuration and metrics of the area it is drawn in; anything unexpected falls back
to the application context, and every `WindowManager` call is wrapped, so a revoked permission
degrades to the notification instead of crashing.

Dismissal is the product rule that shapes the whole file: `DhikrOverlayCard` reacts to
`detectTapGestures(onPress = ...)`, so the card disappears on **touch down anywhere** - no button to
find, no ripple to wait for, and **no `startActivity` call in the entire `overlay` package**.
Touching the card never opens the app. A per-user auto-dismiss (touch only, 5/10/15/30 s) removes it
on its own, and showing a second card replaces the first instead of stacking.

The same composable draws the live preview in the appearance screen, so what the user sets is
exactly what appears over other apps.

## Reminders

`ReminderScheduler` uses **inexact** alarms only: `AlarmManagerCompat.setAndAllowWhileIdle`, one
alarm at a time, re-armed after each delivery. The app therefore never requests
`SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`, and never pretends a 15-minute-minimum
`PeriodicWorkRequest` can honour a 5-minute interval.

* Periodic: 5/10/15/20/30/45 min, 1/2/3 h or a custom 5-720 min value.
* Daily: morning, evening and a Friday reminder (`weeklyOnFriday`, سورة الكهف), each with its own
  time, request code and notification id.
* Quiet hours: a single `isQuietAt` that handles the midnight crossing (`22:00 -> 06:00`).
* Clocks: the periodic chain is armed on `ELAPSED_REALTIME_WAKEUP`, so editing the system time or
  crossing a timezone can neither skip it nor fire it early; the daily reminders are wall-clock
  (`RTC_WAKEUP`) alarms and are recomputed when the system reports the change.
* Restore: `BootReceiver` (BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED - all
  four exempt from the implicit-broadcast ban) re-arms only what is enabled. LOCKED_BOOT_COMPLETED
  is deliberately not handled: the app is not `directBootAware` and its settings live in
  credential-encrypted storage. Every receiver uses `goAsync()` and finishes its `PendingResult`.
* No double booking: a restore passes `keepPendingChain = true`, so an already armed reminder is
  left alone and opening the app cannot keep postponing the next dhikr; a real settings change
  re-arms immediately. `FLAG_NO_CREATE` answers "is one already armed?", and cancelling also
  cancels the `PendingIntent` record.
* Honesty: inexact alarms are throttled in Doze (roughly one wake-up per app every nine minutes),
  so the settings screen states that short intervals may not arrive every time. Nothing in the UI
  claims an exact delivery.

## Widgets

Both providers declare `updatePeriodMillis="0"`: the system never polls them. They refresh only on a
real event - a reminder fired, "ذكر آخر" was tapped, a reboot, or `WidgetRefresh.request`. Every
`PendingIntent` is explicit and `FLAG_IMMUTABLE`.

## Theme

`DhikrTheme(themeMode)` installs the Material 3 scheme (light ivory/emerald/gold, independently
designed charcoal-emerald dark), the Arabic typography (Cairo for UI, Amiri Quran for dhikr text),
the extra brand colours through `LocalDhikrColors`, and forces `LayoutDirection.Rtl` for the whole
tree so the layout is true RTL regardless of the device locale. `dhikrBodyStyle(fontScale)` scales
only the dhikr text; nothing is measured in hard-coded pixels, so the system font scale works
everywhere.

## Threading

| Work | Where |
|---|---|
| UI state | `viewModelScope`, `StateFlow` |
| Database, DataStore | Room/DataStore coroutines, called from repositories |
| Seeding, alarm bookkeeping | `@ApplicationScope` (`Dispatchers.Default`) |
| Broadcast receivers | `goAsync()` + a short-lived scope, always finished |
| Overlay window | main thread (`showDhikrAsync` hops to `Dispatchers.Main`) |
