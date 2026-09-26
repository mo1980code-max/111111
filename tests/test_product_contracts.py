"""The product rules of the specification, asserted against the source.

These are the constraints a compiler would never catch: which permissions exist, how the floating
card dismisses itself, that reminders use inexact alarms only, that nothing in the app can reach
the network, and that the deep links a notification or a widget can fire are all reachable.

Run with::

    python3 -m unittest discover -s tests -v
"""
import os
import re
import unittest
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(REPO, "app")
MAIN = os.path.join(APP, "src", "main")
RES = os.path.join(MAIN, "res")
JAVA = os.path.join(MAIN, "java")
SRC = os.path.join(JAVA, "com", "clock", "livewallpaper")
MANIFEST = os.path.join(MAIN, "AndroidManifest.xml")
ANDROID_NS = "http://schemas.android.com/apk/res/android"


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def strip_comments(source):
    """Removes // and /* */ comments so a rule can be asserted against code, not prose."""
    out = []
    index = 0
    length = len(source)
    while index < length:
        two = source[index:index + 2]
        if source[index:index + 3] == '"""':
            end = source.find('"""', index + 3)
            end = length if end == -1 else end + 3
            out.append(source[index:end])
            index = end
            continue
        if two == "//":
            end = source.find("\n", index)
            index = length if end == -1 else end
            continue
        if two == "/*":
            end = source.find("*/", index + 2)
            index = length if end == -1 else end + 2
            continue
        out.append(source[index])
        index += 1
    return "".join(out)


def kotlin_files():
    for root, _dirs, files in os.walk(JAVA):
        for name in sorted(files):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def all_kotlin_source():
    """Every Kotlin file, comments removed: rules are asserted against real code only."""
    return "\n".join(strip_comments(read(path)) for path in kotlin_files())


class PermissionBudgetTest(unittest.TestCase):

    EXPECTED = {
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.VIBRATE",
    }

    def setUp(self):
        self.root = ET.parse(MANIFEST).getroot()
        self.declared = {
            element.get(f"{{{ANDROID_NS}}}name")
            for element in self.root.findall("uses-permission")
        }

    def test_permission_set_is_exactly_the_budget(self):
        self.assertEqual(self.EXPECTED, self.declared)

    def test_no_exact_alarm_permission(self):
        for name in ("SCHEDULE_EXACT_ALARM", "USE_EXACT_ALARM"):
            self.assertNotIn(f"android.permission.{name}", self.declared)

    def test_no_internet_permission(self):
        for name in ("INTERNET", "ACCESS_NETWORK_STATE", "ACCESS_WIFI_STATE"):
            self.assertNotIn(f"android.permission.{name}", self.declared)

    def test_no_privacy_sensitive_services(self):
        declared = " ".join(sorted(name for name in self.declared if name))
        for forbidden in (
            "BIND_ACCESSIBILITY_SERVICE",
            "BIND_NOTIFICATION_LISTENER_SERVICE",
            "PACKAGE_USAGE_STATS",
            "QUERY_ALL_PACKAGES",
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
            "SET_WALLPAPER",
            "AD_ID",
        ):
            self.assertNotIn(forbidden, declared)

    def test_rtl_is_enabled(self):
        application = self.root.find("application")
        self.assertEqual("true", application.get(f"{{{ANDROID_NS}}}supportsRtl"))


class OfflineTest(unittest.TestCase):

    FORBIDDEN_APIS = (
        "HttpURLConnection",
        "java.net.URL(",
        "OkHttp",
        "Retrofit",
        "WebView",
        "Firebase",
        "FirebaseAnalytics",
        "MobileAds",
        "AdRequest",
        "AdView",
        "InterstitialAd",
        "GoogleSignIn",
        "com.google.android.gms",
        "Supabase",
    )

    def test_no_networking_or_sdk_calls(self):
        source = all_kotlin_source()
        offenders = [api for api in self.FORBIDDEN_APIS if api in source]
        self.assertEqual([], offenders, f"network / SDK usage: {offenders}")

    def test_no_remote_fonts_or_images(self):
        """Fonts are bundled files; no downloadable-font provider and no http(s) references."""
        offenders = []
        for root, _dirs, files in os.walk(RES):
            for name in files:
                path = os.path.join(root, name)
                if not name.endswith(".xml"):
                    continue
                source = read(path)
                urls = [
                    url for url in re.findall(r"https?://[^\"'\s]+", source)
                    if "schemas.android.com" not in url
                ]
                if "fontProviderAuthority" in source or urls:
                    offenders.append((os.path.relpath(path, REPO), urls))
        self.assertEqual([], offenders, f"remote resources: {offenders}")

    def test_bundled_fonts_exist(self):
        for font in ("cairo_regular.ttf", "tajawal_regular.ttf", "amiri_quran.ttf"):
            self.assertTrue(os.path.exists(os.path.join(RES, "font", font)), font)

    def test_no_backend_dependencies(self):
        gradle = read(os.path.join(APP, "build.gradle"))
        for forbidden in ("firebase", "play-services", "admob", "supabase", "retrofit", "okhttp"):
            self.assertNotIn(forbidden, gradle.lower())


class OverlayContractTest(unittest.TestCase):
    """The signature feature: a floating card that dismisses on touch and opens nothing."""

    def setUp(self):
        self.manager = read(os.path.join(SRC, "overlay", "OverlayManager.kt"))
        self.card = read(os.path.join(SRC, "overlay", "DhikrOverlayCard.kt"))
        self.permission = read(os.path.join(SRC, "overlay", "OverlayPermission.kt"))

    def test_uses_application_overlay_window_type(self):
        self.assertIn("TYPE_APPLICATION_OVERLAY", self.manager)
        self.assertIn("FLAG_NOT_FOCUSABLE", self.manager)

    def test_permission_is_checked_before_showing(self):
        self.assertIn("Settings.canDrawOverlays", self.permission)
        presenter = read(os.path.join(SRC, "reminder", "ReminderPresenter.kt"))
        self.assertIn("OverlayPermission.canDraw(context)", presenter)

    def test_touch_dismisses_and_starts_no_activity(self):
        self.assertIn("detectTapGestures", self.card)
        self.assertIn("onDismiss", self.card)
        for path in ("overlay/DhikrOverlayCard.kt", "overlay/OverlayManager.kt",
                     "overlay/OverlayViewOwner.kt"):
            source = read(os.path.join(SRC, *path.split("/")))
            self.assertNotIn("startActivity", source, f"{path} must not launch an Activity")

    def test_no_service_is_used_for_the_overlay(self):
        manifest = read(MANIFEST)
        self.assertNotIn("<service", manifest)
        self.assertNotIn(": Service(", all_kotlin_source())

    def test_single_window_instance(self):
        """Showing twice must replace the card, never stack two windows."""
        self.assertIn("removeImmediately()", self.manager)

    def test_notification_fallback_exists(self):
        presenter = read(os.path.join(SRC, "reminder", "ReminderPresenter.kt"))
        self.assertIn("notifier.showDhikr(text)", presenter)

    def test_preview_uses_the_same_composable(self):
        preview = read(os.path.join(SRC, "ui", "components", "OverlayPreviewCard.kt"))
        self.assertIn("DhikrOverlayCard(", preview)


class ReminderContractTest(unittest.TestCase):

    def setUp(self):
        self.scheduler = read(os.path.join(SRC, "reminder", "ReminderScheduler.kt"))
        self.presenter = strip_comments(read(os.path.join(SRC, "reminder", "ReminderPresenter.kt")))

    def test_only_inexact_alarms(self):
        code = strip_comments(self.scheduler)
        self.assertIn("setAndAllowWhileIdle", code)
        for forbidden in ("setExact(", "setExactAndAllowWhileIdle", "setAlarmClock", "setRepeating("):
            self.assertNotIn(forbidden, code)

    def test_no_workmanager_as_a_scheduler(self):
        source = all_kotlin_source()
        for forbidden in ("PeriodicWorkRequest", "WorkManager", "OneTimeWorkRequest"):
            self.assertNotIn(forbidden, source)

    def test_chain_is_rearmed_before_anything_is_shown(self):
        body = self.presenter[self.presenter.index("suspend fun onPeriodicAlarm"):]
        rearm = body.index("scheduler.schedulePeriodic")
        quiet = body.index("quietHours.isQuietAt")
        show = body.index("overlayManager.showDhikrAsync")
        self.assertLess(rearm, quiet)
        self.assertLess(quiet, show)

    def test_quiet_hours_cross_midnight(self):
        models = read(os.path.join(SRC, "data", "prefs", "SettingsModels.kt"))
        block = models[models.index("fun isQuietAt"):]
        self.assertIn("startMinute < endMinute", block)
        self.assertIn("minuteOfDay >= startMinute || minuteOfDay < endMinute", block)

    def test_recent_dhikr_avoidance(self):
        repository = read(os.path.join(SRC, "data", "DhikrRepository.kt"))
        self.assertIn("recentDhikrIds()", repository)
        self.assertIn("filterNot { recent.contains(it.id) }", repository)

    def test_daily_reminders_cover_morning_evening_friday(self):
        kinds = read(os.path.join(SRC, "reminder", "DailyReminderKind.kt"))
        for kind in ("MORNING", "EVENING", "FRIDAY"):
            self.assertIn(kind, kinds)
        self.assertIn("weeklyOnFriday", kinds)

    def test_boot_restores_schedules(self):
        boot = read(os.path.join(SRC, "reminder", "BootReceiver.kt"))
        self.assertIn("restoreSchedules", boot)
        manifest = read(MANIFEST)
        self.assertIn("android.intent.action.BOOT_COMPLETED", manifest)

    def test_receivers_finish_their_async_result(self):
        for name in ("BootReceiver", "DhikrReminderReceiver", "DailyReminderReceiver"):
            source = read(os.path.join(SRC, "reminder", f"{name}.kt"))
            self.assertIn("goAsync()", source, name)
            self.assertIn("pendingResult.finish()", source, name)


class WidgetContractTest(unittest.TestCase):

    def test_widgets_do_not_poll(self):
        for name in ("widget_dhikr_info.xml", "widget_quick_actions_info.xml"):
            source = read(os.path.join(RES, "xml", name))
            self.assertIn('updatePeriodMillis="0"', source)

    def test_pending_intents_are_immutable(self):
        for name in ("WidgetRefresh.kt", "DhikrWidgetProvider.kt"):
            source = read(os.path.join(SRC, "widget", name))
            for match in re.finditer(r"PendingIntent\.get\w+\(", source):
                tail = source[match.start():match.start() + 400]
                self.assertIn("FLAG_IMMUTABLE", tail, name)

    def test_quick_actions_open_the_three_routes(self):
        source = read(os.path.join(SRC, "widget", "QuickActionsWidgetProvider.kt"))
        for route in ("Routes.MORNING", "Routes.EVENING", "Routes.TASBEEH"):
            self.assertIn(route, source)

    def test_dhikr_widget_rotates(self):
        source = read(os.path.join(SRC, "widget", "DhikrWidgetProvider.kt"))
        self.assertIn("pickForReminder()", source)
        self.assertIn("WIDGET_NEXT_DHIKR", source)


class ThemeAndLayoutTest(unittest.TestCase):

    def test_layout_direction_is_forced_rtl(self):
        theme = read(os.path.join(SRC, "ui", "theme", "Theme.kt"))
        self.assertIn("LocalLayoutDirection provides LayoutDirection.Rtl", theme)

    def test_light_and_dark_schemes_are_independent(self):
        theme = read(os.path.join(SRC, "ui", "theme", "Theme.kt"))
        light = theme[theme.index("private val LightScheme"):theme.index("private val DarkScheme")]
        dark = theme[theme.index("private val DarkScheme"):]
        light_colors = set(re.findall(r"Color\(0x[0-9A-Fa-f]{8}\)", light))
        dark_colors = set(re.findall(r"Color\(0x[0-9A-Fa-f]{8}\)", dark))
        self.assertTrue(light_colors)
        self.assertTrue(dark_colors)
        # the dark theme is designed, not a tinted copy of the light one
        self.assertGreater(len(dark_colors - light_colors), 5)

    def test_three_theme_modes_exist(self):
        models = read(os.path.join(SRC, "data", "prefs", "SettingsModels.kt"))
        for mode in ("LIGHT", "DARK", "SYSTEM"):
            self.assertIn(f"{mode}(", models)

    def test_splash_theme_hands_over_to_the_app_theme(self):
        themes = read(os.path.join(RES, "values", "themes.xml"))
        self.assertIn("Theme.Dhikr.Starting", themes)
        self.assertIn("postSplashScreenTheme", themes)
        self.assertIn("installSplashScreen", read(os.path.join(SRC, "MainActivity.kt")))

    def test_edge_to_edge(self):
        activity = read(os.path.join(SRC, "MainActivity.kt"))
        self.assertIn("enableEdgeToEdge()", activity)
        self.assertIn("isAppearanceLightStatusBars", activity)


class AccessibilityTest(unittest.TestCase):

    def test_icon_buttons_require_a_description(self):
        controls = read(os.path.join(SRC, "ui", "components", "Controls.kt"))
        signature = controls[controls.index("fun IconActionButton("):controls.index("/** Text + icon action")]
        self.assertIn("contentDescription: String?", signature)
        self.assertIn(".size(48.dp)", signature)

    def test_touch_targets_are_at_least_48dp(self):
        controls = read(os.path.join(SRC, "ui", "components", "Controls.kt"))
        minimums = re.findall(r"defaultMinSize\(minHeight = (\d+)\.dp\)", controls)
        self.assertTrue(minimums)
        self.assertTrue(all(int(value) >= 48 for value in minimums), minimums)

    def test_counter_exposes_its_state(self):
        ring = read(os.path.join(SRC, "ui", "components", "TasbeehRing.kt"))
        self.assertIn("contentDescription = counterDescription", ring)
        self.assertIn("role = Role.Button", ring)

    def test_font_scaling_is_supported(self):
        """Text sizes are sp based, so the system font scale applies everywhere."""
        typography = read(os.path.join(SRC, "ui", "theme", "Type.kt"))
        self.assertNotIn(".dp,\n        lineHeight", typography)
        self.assertGreater(typography.count(".sp"), 20)


class DataContractTest(unittest.TestCase):

    def test_room_never_drops_user_data(self):
        module = read(os.path.join(SRC, "di", "AppModule.kt"))
        self.assertNotIn("fallbackToDestructiveMigration", module)
        database = read(os.path.join(SRC, "data", "local", "DhikrDatabase.kt"))
        self.assertIn("exportSchema = true", database)

    def test_seeded_rows_cannot_be_deleted(self):
        dao = read(os.path.join(SRC, "data", "local", "DhikrDao.kt"))
        self.assertIn("DELETE FROM dhikr WHERE id = :id AND isDefault = 0", dao)

    def test_seeded_text_is_never_rewritten(self):
        repository = read(os.path.join(SRC, "data", "DhikrRepository.kt"))
        block = repository[repository.index("suspend fun save("):]
        guarded = block[:block.index("val cleaned")]
        self.assertIn("existing.isDefault", guarded)
        self.assertIn("isEnabled = isEnabled, includeInOverlay = includeInOverlay", guarded)

    def test_settings_live_in_datastore(self):
        repository = read(os.path.join(SRC, "data", "prefs", "SettingsRepository.kt"))
        self.assertIn("DataStore<Preferences>", repository)
        self.assertIn("dhikr_settings", repository)

    def test_backup_rules_cover_the_local_data(self):
        rules = read(os.path.join(RES, "xml", "data_extraction_rules.xml"))
        self.assertIn("dhikr.db", rules)
        self.assertIn("datastore", rules)


class ReleaseHardeningTest(unittest.TestCase):
    """Regressions for the release audit: background delivery, clock changes, window context."""

    def setUp(self):
        self.scheduler = strip_comments(read(os.path.join(SRC, "reminder", "ReminderScheduler.kt")))
        self.boot = strip_comments(read(os.path.join(SRC, "reminder", "BootReceiver.kt")))
        self.overlay = strip_comments(read(os.path.join(SRC, "overlay", "OverlayManager.kt")))
        self.manifest = read(MANIFEST)
        # Declared actions only: the manifest comments explain what is deliberately absent.
        self.actions = {
            element.get(f"{{{ANDROID_NS}}}name")
            for element in ET.parse(MANIFEST).getroot().iter("action")
        }

    def test_interval_reminders_use_the_elapsed_realtime_clock(self):
        """A wall-clock edit must not skip or double-fire the periodic chain."""
        block = self.scheduler[self.scheduler.index("fun schedulePeriodic"):]
        block = block[:block.index("fun cancelPeriodic")]
        self.assertIn("SystemClock.elapsedRealtime()", block)
        self.assertNotIn("System.currentTimeMillis()", block)
        self.assertIn("ELAPSED_REALTIME_WAKEUP", self.scheduler)
        # Time-of-day reminders stay on the wall clock, which is what the user picked.
        self.assertIn("AlarmManager.RTC_WAKEUP", self.scheduler)

    def test_a_restore_never_postpones_an_armed_reminder(self):
        """Cold starts used to push the next dhikr one full interval into the future."""
        self.assertIn("FLAG_NO_CREATE", self.scheduler)
        self.assertIn("fun isPeriodicPending()", self.scheduler)
        block = self.scheduler[self.scheduler.index("fun apply("):]
        block = block[:block.index("fun applyDaily")]
        self.assertIn("keepPendingChain", block)
        self.assertIn("!keepPendingChain || !isPeriodicPending()", block)
        for path in (("core", "AppStartup.kt"), ("reminder", "ReminderPresenter.kt")):
            source = strip_comments(read(os.path.join(SRC, *path)))
            self.assertIn("keepPendingChain = true", source, path[-1])

    def test_cancelling_releases_the_pending_intent_record(self):
        """Otherwise isPeriodicPending would keep reporting a chain the user switched off."""
        for name in ("fun cancelPeriodic", "fun cancelDaily"):
            block = self.scheduler[self.scheduler.index(name):]
            end = block.find("\n    fun ", 1)
            self.assertIn("operation.cancel()", block[:end if end != -1 else len(block)], name)

    def test_clock_and_timezone_changes_rearm_the_daily_alarms(self):
        self.assertIn("Intent.ACTION_TIME_CHANGED", self.boot)
        self.assertIn("Intent.ACTION_TIMEZONE_CHANGED", self.boot)
        self.assertIn("android.intent.action.TIME_SET", self.actions)
        self.assertIn("android.intent.action.TIMEZONE_CHANGED", self.actions)

    def test_locked_boot_is_not_claimed(self):
        """The app is not directBootAware, so handling LOCKED_BOOT_COMPLETED would be a lie."""
        self.assertNotIn("android.intent.action.LOCKED_BOOT_COMPLETED", self.actions)
        self.assertNotIn("LOCKED_BOOT_COMPLETED", all_kotlin_source())
        self.assertNotIn('directBootAware="true"', self.manifest)

    def test_boot_receiver_ignores_everything_else(self):
        self.assertIn("if (action !in HANDLED_ACTIONS) return", self.boot)
        self.assertIn("Intent.ACTION_BOOT_COMPLETED", self.boot)
        self.assertIn("Intent.ACTION_MY_PACKAGE_REPLACED", self.boot)

    def test_overlay_window_uses_a_window_context(self):
        """API 30+ wants a window context for a non-activity window; older releases fall back."""
        block = self.overlay[self.overlay.index("fun overlayWindowContext"):]
        block = block[:block.index("private fun buildLayoutParams")]
        self.assertIn("Build.VERSION_CODES.R", block)
        self.assertIn("createDisplayContext", block)
        self.assertIn("createWindowContext", block)
        self.assertIn("TYPE_APPLICATION_OVERLAY", block)
        self.assertIn("getOrDefault(context)", block)
        self.assertIn("ComposeView(windowContext)", self.overlay)

    def test_overlay_measures_the_window_it_is_added_to(self):
        block = self.overlay[self.overlay.index("private fun buildLayoutParams"):]
        self.assertIn("currentWindowMetrics.bounds.width()", block)

    def test_overlay_is_removed_by_the_manager_that_added_it(self):
        self.assertIn("current.windowManager.removeViewImmediate", self.overlay)

    def test_no_foreground_service_was_added_for_the_overlay(self):
        source = all_kotlin_source()
        for forbidden in ("startForegroundService", "foregroundServiceType", "Service()"):
            self.assertNotIn(forbidden, source)
        self.assertNotIn("<service", self.manifest)

    def test_hilt_plugin_is_applied_so_receivers_are_injected(self):
        """@AndroidEntryPoint on a BroadcastReceiver only injects through the Hilt transform."""
        gradle = read(os.path.join(APP, "build.gradle"))
        self.assertIn("dagger.hilt.android.plugin", gradle)
        self.assertIn("hilt-android-compiler", gradle)
        for name in ("BootReceiver", "DhikrReminderReceiver", "DailyReminderReceiver"):
            source = read(os.path.join(SRC, "reminder", f"{name}.kt"))
            self.assertIn("@AndroidEntryPoint", source, name)
            self.assertIn("@Inject", source, name)
            # BroadcastReceiver.onReceive is abstract: calling super would not compile.
            self.assertNotIn("super.onReceive", source, name)

    def test_reminder_copy_admits_the_delay(self):
        strings = read(os.path.join(RES, "values", "strings.xml"))
        note = strings[strings.index('name="settings_reminder_inexact_note"'):]
        note = note[:note.index("</string>")]
        self.assertIn("غير دقيقة", note)
        self.assertIn("قد", note)
        self.assertIn("settings_reminder_inexact_note", read(
            os.path.join(SRC, "ui", "screens", "settings", "SettingsScreen.kt")
        ))


if __name__ == "__main__":
    unittest.main()
