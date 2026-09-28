"""The product rules of the specification, asserted against the source.

These are the constraints a compiler would never catch: which permissions exist, how the floating
card dismisses itself, that reminders use inexact alarms only, that the only network client in the
app is the advertising SDK - under the rules the ad layer promises - and that the deep links a
notification or a widget can fire are all reachable.

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


ADS = os.path.join(SRC, "ads")


def ad_layer_files():
    """Every Kotlin file of the advertising layer."""
    for root, _dirs, files in os.walk(ADS):
        for name in sorted(files):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def screen_files(folder):
    directory = os.path.join(SRC, "ui", "screens", folder)
    for name in sorted(os.listdir(directory)):
        if name.endswith(".kt"):
            yield os.path.join(directory, name)


def gradle_dependency_lines():
    """The dependency declarations of the app module, without the comments around them."""
    gradle = strip_comments(read(os.path.join(APP, "build.gradle")))
    return [
        line.strip() for line in gradle.splitlines()
        if re.match(r"\s*(implementation|api|kapt|compileOnly|runtimeOnly|debugImplementation)\b",
                    line)
    ]


def text_files_of_repository():
    """Every text file of the repository - used to prove no real ad unit id hides anywhere."""
    extensions = (".kt", ".java", ".xml", ".gradle", ".pro", ".properties", ".md", ".py",
                  ".json", ".txt", ".yml", ".yaml", ".kts")
    for root, dirs, files in os.walk(REPO):
        dirs[:] = [name for name in dirs if name not in {".git", ".gradle", "build", "node_modules"}]
        for name in sorted(files):
            if name.endswith(extensions):
                yield os.path.join(root, name)


class PermissionBudgetTest(unittest.TestCase):

    EXPECTED = {
        "android.permission.INTERNET",
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

    def test_internet_is_the_only_network_permission(self):
        """INTERNET exists for the ads SDK; nothing needs to inspect the network itself."""
        self.assertIn("android.permission.INTERNET", self.declared)
        for name in ("ACCESS_NETWORK_STATE", "ACCESS_WIFI_STATE", "ACCESS_FINE_LOCATION",
                     "ACCESS_COARSE_LOCATION"):
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
    """The content is local; the advertising SDK is the single exception, and it is fenced in."""

    FORBIDDEN_APIS = (
        "HttpURLConnection",
        "java.net.URL(",
        "OkHttp",
        "Retrofit",
        "WebView",
        "Firebase",
        "FirebaseAnalytics",
        "GoogleSignIn",
        "com.google.android.gms.analytics",
        "com.google.android.gms.measurement",
        "Supabase",
    )

    def test_no_networking_or_sdk_calls(self):
        source = all_kotlin_source()
        offenders = [api for api in self.FORBIDDEN_APIS if api in source]
        self.assertEqual([], offenders, f"network / SDK usage: {offenders}")

    def test_the_app_never_opens_a_connection_itself(self):
        """No socket, no URL, no download: whatever leaves the device leaves through the SDK."""
        offenders = []
        for path in kotlin_files():
            source = strip_comments(read(path))
            for api in ("java.net.", "URLConnection", "Socket(", "openStream(", "HttpClient"):
                if api in source:
                    offenders.append((os.path.relpath(path, REPO), api))
        self.assertEqual([], offenders, f"hand written networking: {offenders}")

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
        """Only the two Google advertising artifacts may talk to a server on the app's behalf."""
        dependencies = gradle_dependency_lines()
        for forbidden in ("firebase", "supabase", "retrofit", "okhttp", "volley", "ktor",
                          "appsflyer", "facebook", "unity-ads", "applovin", "ironsource",
                          "play-services-analytics", "play-services-measurement"):
            offenders = [line for line in dependencies if forbidden in line.lower()]
            self.assertEqual([], offenders, f"forbidden dependency: {offenders}")

    def test_the_only_google_dependencies_are_ads_and_consent(self):
        google = [
            line for line in gradle_dependency_lines()
            if "com.google.android" in line
        ]
        self.assertEqual(
            [
                "implementation 'com.google.android.gms:play-services-ads:25.4.0'",
                "implementation 'com.google.android.ump:user-messaging-platform:4.0.0'",
            ],
            google,
        )


class AdvertisingContractTest(unittest.TestCase):
    """The advertising layer: Google test ids only, consent first, and no surprises for the user."""

    TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    TEST_UNITS = {
        "banner": "ca-app-pub-3940256099942544/6300978111",
        "interstitial": "ca-app-pub-3940256099942544/1033173712",
        "rewarded": "ca-app-pub-3940256099942544/5224354917",
        "rewarded interstitial": "ca-app-pub-3940256099942544/5354046379",
        "native": "ca-app-pub-3940256099942544/2247696110",
        "app open": "ca-app-pub-3940256099942544/9257395921",
    }
    SENSITIVE_SCREENS = ("onboarding", "reading", "tasbeeh", "editor", "mydhikr", "overlay",
                         "privacy", "about")

    def setUp(self):
        self.config = strip_comments(read(os.path.join(ADS, "AdConfig.kt")))
        self.coordinator = strip_comments(read(os.path.join(ADS, "AdsCoordinator.kt")))
        self.consent = strip_comments(read(os.path.join(ADS, "ConsentManager.kt")))
        self.banner = strip_comments(read(os.path.join(ADS, "ui", "BannerAdSlot.kt")))
        self.native = strip_comments(read(os.path.join(ADS, "ui", "NativeAdCard.kt")))
        self.activity = strip_comments(read(os.path.join(SRC, "MainActivity.kt")))
        self.settings_screen = strip_comments(
            read(os.path.join(SRC, "ui", "screens", "settings", "SettingsScreen.kt"))
        )
        self.manifest = read(MANIFEST)

    # -------------------------------------------------------------- identifiers

    def test_every_ad_id_in_the_repository_is_a_google_test_id(self):
        """One real publisher id anywhere would be a live ad in a test build."""
        allowed = set(self.TEST_UNITS.values()) | {self.TEST_APP_ID}
        found = set()
        for path in text_files_of_repository():
            found.update(re.findall(r"ca-app-pub-\d+[/~]\d+", read(path)))
        self.assertEqual(allowed, found, f"unexpected ad ids: {sorted(found - allowed)}")

    def test_ad_config_declares_every_format_once(self):
        for name, unit in self.TEST_UNITS.items():
            self.assertEqual(1, self.config.count(unit), f"{name} unit id")

    def test_the_application_id_lives_only_in_the_manifest(self):
        self.assertIn("com.google.android.gms.ads.APPLICATION_ID", self.manifest)
        self.assertEqual(1, self.manifest.count(self.TEST_APP_ID))
        self.assertNotIn(self.TEST_APP_ID, all_kotlin_source())

    # ----------------------------------------------------------------- layering

    def test_the_sdk_is_confined_to_the_ads_package(self):
        offenders = []
        for path in kotlin_files():
            relative = os.path.relpath(path, SRC)
            if not re.search(r"^import\s+com\.google\.android\.(gms\.ads|ump)\.", read(path), re.M):
                continue
            if not relative.startswith("ads" + os.sep):
                offenders.append(relative)
        self.assertEqual([], offenders, f"ad SDK reached outside ads/: {offenders}")

    def test_no_singleton_keeps_an_activity(self):
        offenders = []
        for path in ad_layer_files():
            source = strip_comments(read(path))
            for pattern in (r"(?:val|var)\s+\w+\s*:\s*Activity\b", r"WeakReference<Activity>"):
                for match in re.finditer(pattern, source):
                    offenders.append((os.path.relpath(path, REPO), match.group(0)))
        self.assertEqual([], offenders, f"Activity retained by the ad layer: {offenders}")

    def test_the_activity_is_always_passed_in(self):
        for signature in (
            "fun gatherConsent(activity: Activity)",
            "fun showPrivacyOptions(activity: Activity)",
            "fun onForeground(activity: Activity)",
            "fun onContentSessionEnded(activity: Activity)",
        ):
            self.assertIn(signature, self.coordinator)

    # ------------------------------------------------------------------ consent

    def test_consent_runs_through_ump_with_a_foreground_activity(self):
        self.assertIn("UserMessagingPlatform.getConsentInformation(context)", self.consent)
        self.assertIn("requestConsentInfoUpdate(\n            activity,", self.consent)
        self.assertIn("loadAndShowConsentFormIfRequired(activity)", self.consent)
        self.assertIn("showPrivacyOptionsForm(activity)", self.consent)
        self.assertIn("PrivacyOptionsRequirementStatus.REQUIRED", self.consent)

    def test_two_consent_requests_or_forms_can_never_overlap(self):
        self.assertIn("updateInFlight.compareAndSet(false, true)", self.consent)
        self.assertIn("formInFlight.compareAndSet(false, true)", self.consent)

    def test_debug_builds_do_not_bypass_consent(self):
        source = "\n".join(strip_comments(read(path)) for path in ad_layer_files())
        for bypass in ("ConsentDebugSettings", "DEBUG_GEOGRAPHY", "addTestDeviceHashedId",
                       "consentInformation.reset()", "setTagForUnderAgeOfConsent"):
            self.assertNotIn(bypass, source)
        self.assertNotIn("BuildConfig.DEBUG", self.consent)

    def test_nothing_is_requested_before_consent_initialisation_and_onboarding(self):
        self.assertIn("consent.canRequestAds && initialized && onboarded", self.coordinator)
        self.assertIn("if (consent.canRequestAds) initializer.ensureInitialized()", self.coordinator)

    def test_the_sdk_is_initialised_in_exactly_one_idempotent_place(self):
        callers = [
            os.path.relpath(path, SRC) for path in kotlin_files()
            if "MobileAds.initialize(" in strip_comments(read(path))
        ]
        self.assertEqual([os.path.join("ads", "MobileAdsInitializer.kt")], callers)
        initializer = read(os.path.join(ADS, "MobileAdsInitializer.kt"))
        self.assertIn("started.compareAndSet(false, true)", initializer)
        # An initialised SDK is not a loaded ad, and the log says so.
        self.assertIn("no ad requested yet", initializer)

    # ------------------------------------------------------------------- banner

    def test_banner_is_created_once_sized_and_destroyed(self):
        self.assertIn("if (!state.adsReady) return", self.banner)
        self.assertIn("remember { AdView(context) }", self.banner)
        self.assertIn("getCurrentOrientationAnchoredAdaptiveBannerAdSize", self.banner)
        self.assertIn("Lifecycle.Event.ON_PAUSE -> adView.pause()", self.banner)
        self.assertIn("Lifecycle.Event.ON_RESUME -> adView.resume()", self.banner)
        self.assertIn("removeView(adView)", self.banner)
        self.assertIn("adView.destroy()", self.banner)

    def test_banner_reports_both_outcomes(self):
        self.assertIn("override fun onAdLoaded()", self.banner)
        self.assertIn('AdLog.loadFailed("banner", error)', self.banner)

    # ------------------------------------------------------------------- native

    def test_native_ad_is_labelled_bound_and_destroyed(self):
        self.assertIn("R.string.ad_label", self.native)
        self.assertIn("adView.setNativeAd(ad)", self.native)
        self.assertIn("onRelease = { view -> view.destroy() }", self.native)
        view_model = strip_comments(read(os.path.join(ADS, "AdsViewModel.kt")))
        cleared = view_model[view_model.index("override fun onCleared"):]
        self.assertIn("nativeRequest?.cancel()", cleared)
        self.assertIn("_nativeAd.value?.destroy()", cleared)
        manager = strip_comments(read(os.path.join(ADS, "NativeAdManager.kt")))
        self.assertIn("if (request.isCancelled)", manager)
        self.assertIn("nativeAd.destroy()", manager)

    def test_the_ad_label_is_arabic_copy(self):
        strings = read(os.path.join(RES, "values", "strings.xml"))
        self.assertIn('<string name="ad_label">إعلان</string>', strings)

    # ------------------------------------------------------------- full screen

    def test_full_screen_ads_are_capped_by_one_gate(self):
        for constant in ("SESSION_WARM_UP_MS", "FULLSCREEN_MIN_GAP_MS", "SENSITIVE_FLOW_QUIET_MS",
                         "INTERSTITIAL_MIN_SESSIONS", "APP_OPEN_MIN_BACKGROUND_MS"):
            self.assertIn(f"AdConfig.{constant}", self.coordinator)
        gate = self.coordinator[self.coordinator.index("private fun canShowFullscreen"):]
        gate = gate[:gate.index("private fun markFullscreenShown")]
        self.assertIn("!adsReady.value", gate)
        self.assertIn("consentManager.formVisible.value", gate)
        self.assertIn("now < quietUntilMs", gate)
        self.assertIn("AdConfig.SESSION_WARM_UP_MS", gate)
        self.assertIn("AdConfig.FULLSCREEN_MIN_GAP_MS", gate)

    def test_only_the_coordinator_can_show_a_full_screen_ad(self):
        owners = {
            os.path.join("ads", name) for name in
            ("AdsCoordinator.kt", "InterstitialAdManager.kt", "AppOpenAdManager.kt",
             "RewardedAdManager.kt")
        }
        offenders = []
        for path in kotlin_files():
            relative = os.path.relpath(path, SRC)
            if relative in owners:
                continue
            source = strip_comments(read(path))
            for symbol in ("InterstitialAdManager", "AppOpenAdManager", "RewardedAdManager"):
                if symbol in source:
                    offenders.append((relative, symbol))
        self.assertEqual([], offenders, f"full screen ads triggered outside the gate: {offenders}")

    def test_interstitial_waits_for_a_finished_reading_session(self):
        self.assertIn("finishedContentSessions < AdConfig.INTERSTITIAL_MIN_SESSIONS",
                      self.coordinator)
        shell = strip_comments(read(os.path.join(SRC, "ui", "DhikrApp.kt")))
        self.assertIn("Routes.isReading(previous)", shell)
        self.assertIn("!Routes.isReading(currentRoute)", shell)
        self.assertIn("adsCoordinator.onContentSessionEnded(", self.activity)

    def test_app_open_skips_the_cold_start_and_short_trips(self):
        block = self.coordinator[self.coordinator.index("fun onForeground"):]
        block = block[:block.index("fun onBackground")]
        self.assertIn("foregroundCount == 1", block)
        self.assertIn("awayMs < AdConfig.APP_OPEN_MIN_BACKGROUND_MS", block)
        self.assertIn('canShowFullscreen("app open")', block)

    def test_rewarded_is_user_initiated_and_rewards_from_the_callback_only(self):
        rewarded = strip_comments(read(os.path.join(ADS, "RewardedAdManager.kt")))
        self.assertIn("fun show(activity: Activity, onRewardEarned: (RewardItem) -> Unit)", rewarded)
        self.assertEqual(1, rewarded.count("onRewardEarned("))
        self.assertIn("ready.show(activity) { reward ->", rewarded)
        # No automatic placement exists: the coordinator never starts a rewarded ad.
        self.assertNotIn("RewardedAdManager", self.coordinator)

    def test_system_and_consent_screens_open_a_quiet_window(self):
        block = self.activity[self.activity.index("override fun startActivity("):]
        self.assertIn("adsCoordinator.onSystemUiShown()", block[:block.index("super.startActivity")])
        self.assertIn("markQuietPeriod()", self.coordinator)
        gather = self.coordinator[self.coordinator.index("fun gatherConsent"):]
        self.assertIn("markQuietPeriod()", gather[:gather.index("consentManager.gather")])

    # ----------------------------------------------------------------- placement

    def test_no_ads_in_focused_or_legal_screens(self):
        offenders = []
        for folder in self.SENSITIVE_SCREENS:
            for path in screen_files(folder):
                source = strip_comments(read(path))
                for symbol in ("BannerAdSlot", "NativeAdCard", "PrivacyOptionsRow", "AdsViewModel"):
                    if symbol in source:
                        offenders.append((folder, symbol))
        self.assertEqual([], offenders, f"ads inside a focused screen: {offenders}")

    def test_ads_are_placed_in_the_browsing_screens(self):
        home = strip_comments(read(os.path.join(SRC, "ui", "screens", "home", "HomeScreen.kt")))
        adhkar = strip_comments(read(os.path.join(SRC, "ui", "screens", "adhkar", "AdhkarScreen.kt")))
        self.assertIn("NativeAdCard()", home)
        self.assertIn("BannerAdSlot()", adhkar)

    def test_settings_has_no_debug_or_status_surface(self):
        for forbidden in ("AdMob", "adsReady", "canRequestAds", "BannerAdSlot", "NativeAdCard",
                          "InterstitialAd", "RewardedAd", "AppOpenAd", "AdView", "AdsUiState"):
            self.assertNotIn(forbidden, self.settings_screen)
        self.assertIn("PrivacyOptionsRow()", self.settings_screen)

    def test_the_only_ad_control_is_googles_privacy_options(self):
        row = strip_comments(read(os.path.join(ADS, "ui", "PrivacyOptionsRow.kt")))
        self.assertIn("if (!state.privacyOptionsRequired) return", row)
        self.assertIn("viewModel.showPrivacyOptions(activity)", row)
        strings = read(os.path.join(RES, "values", "strings.xml"))
        declared = set(re.findall(r'<string name="(settings_ads_\w+)"', strings))
        self.assertEqual(
            {"settings_ads_privacy_options", "settings_ads_privacy_options_subtitle"},
            declared,
        )

    # ------------------------------------------------------------------ logging

    def test_logging_is_debug_only_and_carries_one_tag(self):
        log = strip_comments(read(os.path.join(ADS, "AdLog.kt")))
        self.assertIn('TAG: String = "AdMobDebug"', log)
        calls = len(re.findall(r"(?<![\w.])Log\.", log))
        self.assertGreaterEqual(calls, 4)
        self.assertEqual(calls, log.count("BuildConfig.DEBUG"))
        offenders = [
            os.path.relpath(path, REPO) for path in ad_layer_files()
            if os.path.basename(path) != "AdLog.kt" and "android.util.Log" in read(path)
        ]
        self.assertEqual([], offenders, f"logging around AdLog: {offenders}")

    def test_every_ad_event_the_report_needs_is_logged(self):
        source = "\n".join(strip_comments(read(path)) for path in ad_layer_files())
        for event in ("onAdLoaded", "onAdFailedToLoad", "onAdImpression", "onAdClicked",
                      "onAdShowedFullScreenContent", "onAdDismissedFullScreenContent",
                      "onAdFailedToShowFullScreenContent", "requestConsentInfoUpdate",
                      "MobileAds.initialize"):
            self.assertIn(event, source)
        log = read(os.path.join(ADS, "AdLog.kt"))
        for field in ("error.code", "error.domain", "error.message", "error.responseInfo"):
            self.assertIn(field, log)

    def test_no_user_content_is_logged(self):
        """The dhikr text, the counter and the settings never reach Logcat through the ad layer."""
        source = "\n".join(strip_comments(read(path)) for path in ad_layer_files())
        for leak in ("dhikr", "Dhikr", "tasbeeh", "settings.", "counter"):
            offenders = [line for line in source.splitlines()
                         if "AdLog" in line and leak in line]
            self.assertEqual([], offenders, f"user data in a log line: {offenders}")


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
