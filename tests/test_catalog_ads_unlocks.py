"""Static contracts for the offline catalogs, the ad layer and the permanent unlocks.

There is no JDK or Android SDK in this workspace, so nothing here compiles the app. The checks
instead resolve every resource reference the new code makes against what actually exists in ``res/``,
compare the Java catalogs with the assets and with the generator that produced them, and assert the
ad and unlock *rules* the feature is specified by -- the ones a compiler could never catch:

* 9 free clocks per section, 3 free wallpapers per section, everything else behind a rewarded ad;
* no interstitial ad anywhere in the source, and no ad at all on opening a section;
* the Quran section is ad free and the flag is raised at the screen, not at the call site;
* Azkar has no ad between the dhikr text and the counter, because it has no ad at all;
* native ads are inserted by the official SDK view, every ``NATIVE_AD_INTERVAL`` rows, only in long
  lists, and never inside the clock editor or next to the set / share buttons;
* every image the two catalogs name exists on disk, so both browsers work in airplane mode.

Framework and AndroidX resources (``@android:``, ``?android:attr/``, library styles, the ads SDK's own
attributes) are deliberately out of scope: AAPT2 resolves those against the platform and the AARs.
"""
import json
import re
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
JAVA = MAIN / "java/com/clock/livewallpaper"
ASSETS = MAIN / "assets"
ADS = JAVA / "ads"

sys.path.insert(0, str(ROOT / "tools"))

AD_JAVA = [
    ADS / "AdConfig.java",
    ADS / "AdPolicy.java",
    ADS / "AdsManager.java",
    ADS / "NativePlacement.java",
    ADS / "AdInsertingAdapter.java",
    ADS / "AppForegroundWatcher.java",
    JAVA / "unlock/UnlockStore.java",
    JAVA / "unlock/UnlockPrompt.java",
    JAVA / "catalog/Unlockable.java",
    JAVA / "catalog/ContentAccess.java",
    JAVA / "catalog/WallpaperCatalog.java",
    JAVA / "catalog/WallpaperEntry.java",
    JAVA / "catalog/WallpaperSection.java",
    JAVA / "image/LocalImage.java",
]
FEATURE_JAVA = AD_JAVA + [
    JAVA / "AppClass.java",
    JAVA / "model/Clocks.java",
    JAVA / "model/TextClocks.java",
    JAVA / "model/SmartClocks.java",
    JAVA / "utils/GetClocks.java",
    JAVA / "utils/ArabicDigits.java",
    JAVA / "activity/MainActivity.java",
    JAVA / "activity/ClockFuntionActivity.java",
    JAVA / "activity/ClockCardActivity.java",
    JAVA / "activity/WallpaperCategoryActivity.java",
    JAVA / "activity/WallpaperActivity.java",
    JAVA / "activity/SetWallpaperActivity.java",
    JAVA / "activity/QuranActivity.java",
    JAVA / "activity/SurahListActivity.java",
    JAVA / "adapter/CustomAdapter.java",
    JAVA / "adapter/TextAdapter.java",
    JAVA / "adapter/SmartTextAdapter.java",
    JAVA / "adapter/WallpaperAdapter.java",
    JAVA / "adapter/CategoryWallpaperAdapter.java",
    JAVA / "adapter/LockOverlay.java",
]
NEW_XML = [
    RES / "layout/ad_native.xml",
    RES / "layout/ad_native_slot.xml",
    RES / "layout/item_clocks.xml",
    RES / "layout/item_textclock.xml",
    RES / "layout/item_smarttextclock.xml",
    RES / "layout/item_wallpaper.xml",
    RES / "layout/item_cat_wallpaper.xml",
    RES / "layout/activity_clock_card.xml",
    RES / "layout/activity_wallpaper.xml",
    RES / "layout/activity_wallpaper_category.xml",
    RES / "layout/activity_clock_funtion.xml",
    RES / "layout/activity_select_function.xml",
    RES / "layout/activity_set_wallpaper.xml",
    RES / "values/ads.xml",
    RES / "drawable/bg_ad_card.xml",
    RES / "drawable/bg_ad_badge.xml",
    RES / "drawable/bg_ad_cta.xml",
    RES / "drawable/bg_lock_scrim.xml",
    RES / "drawable/bg_lock_badge.xml",
    RES / "drawable/ic_locked.xml",
    RES / "drawable/ic_lock_open.xml",
]

VALUE_TYPES = {"color", "string", "string-array", "style", "dimen", "integer", "bool", "plurals",
               "attr", "item", "array", "declare-styleable"}
FILE_TYPES = {"drawable", "layout", "anim", "animator", "font", "menu", "mipmap", "raw", "color",
              "xml", "transition", "interpolator"}

# Google's published test ad unit ids for this app id. Implementation must ship these, never real ids.
TEST_UNITS = {
    "APP_OPEN": "ca-app-pub-3940256099942544/9257395921",
    "REWARDED": "ca-app-pub-3940256099942544/5224354917",
    "NATIVE": "ca-app-pub-3940256099942544/2247696110",
    "BANNER": "ca-app-pub-3940256099942544/9214589741",
    "INTERSTITIAL": "ca-app-pub-3940256099942544/1033173712",
}


def read(path):
    return path.read_text(encoding="utf-8")


def strip_comments(source):
    # Comments may explain a decision; only code counts when asking "is this format used?".
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    return re.sub(r"(?m)^\s*//.*$", "", source)


def defined_values():
    found = {}
    for path in sorted(RES.glob("values*/*.xml")):
        for node in ET.parse(path).getroot():
            if not isinstance(node.tag, str):
                continue
            name = node.get("name")
            if node.tag == "item" and node.get("type"):
                found.setdefault(node.get("type"), set()).add(name)
            elif node.tag in VALUE_TYPES and name:
                found.setdefault(node.tag, set()).add(name)
    return found


def defined_files():
    found = {}
    for path in sorted(RES.rglob("*")):
        if not path.is_file() or "." not in path.name:
            continue
        kind = path.parent.name.split("-")[0]
        if kind in FILE_TYPES:
            found.setdefault(kind, set()).add(path.name.rsplit(".", 1)[0])
    return found


def defined_ids():
    ids = set()
    for path in list(RES.glob("layout*/*.xml")) + list(RES.glob("menu/*.xml")) \
            + list(RES.glob("drawable*/*.xml")):
        ids.update(re.findall(r"@\+id/([A-Za-z0-9_]+)", read(path)))
    for path in sorted(RES.glob("values*/*.xml")):
        for node in ET.parse(path).getroot():
            if node.tag == "item" and node.get("type") == "id":
                ids.add(node.get("name"))
    return ids


def sources(paths):
    return {str(path.relative_to(ROOT)): read(path) for path in paths if path.exists()}


def java_constant(name):
    """Value of an ``int``/``boolean`` constant declared in AdConfig, parsed from the source."""
    match = re.search(r"static final (?:int|long|boolean) " + name + r"\s*=\s*([^;]+);",
                      read(ADS / "AdConfig.java"))
    assert match, f"AdConfig does not declare {name}"
    return match.group(1).strip()


def java_number(name):
    """Same value as an int. AdConfig spells durations as simple products (5L * 60_000L), so those
    are multiplied out here instead of being parsed as a literal."""
    total = 1
    for part in java_constant(name).split("*"):
        total *= int(part.strip().replace("_", "").rstrip("Ll"))
    return total


def clock_table(table):
    """Rows of one ``Object[][]`` table in GetClocks, as raw text lines."""
    source = read(JAVA / "utils/GetClocks.java")
    start = source.index("Object[][] " + table + " = {")
    end = source.index("};", start)
    body = source[start:end]
    return [line.strip() for line in body.splitlines() if line.strip().startswith("{")]


class ResourceWiringTests(unittest.TestCase):
    """Every reference the new code makes has to exist -- the cheapest compile substitute."""

    @classmethod
    def setUpClass(cls):
        cls.values = defined_values()
        cls.files = defined_files()
        cls.ids = defined_ids()

    def resolves(self, kind, name):
        if kind == "id":
            return name in self.ids
        if name in self.values.get(kind, set()):
            return True
        return name in self.files.get(kind, set())

    def test_java_references_resolve(self):
        for rel, source in sources(FEATURE_JAVA).items():
            for kind, name in re.findall(r"\bR\.(id|string|layout|drawable|color|style|font|raw|"
                                         r"dimen|array|integer|bool|array)\.([A-Za-z0-9_]+)", source):
                self.assertTrue(self.resolves(kind, name), f"{rel}: R.{kind}.{name} is undefined")

    def test_xml_references_resolve(self):
        for path in NEW_XML:
            text = read(path)
            for kind, name in re.findall(r'"@(?:android\+)?(id|string|layout|drawable|color|style|'
                                         r'font|dimen|array|integer|bool)/([A-Za-z0-9_.]+)', text):
                if kind == "style":
                    continue
                self.assertTrue(self.resolves(kind, name), f"{path.name}: @{kind}/{name} is undefined")
            for name in re.findall(r'"@id/([A-Za-z0-9_]+)"', text):
                self.assertTrue(self.resolves("id", name), f"{path.name}: @id/{name} is never declared")

    def test_layout_files_parse_and_are_not_self_referential(self):
        for path in NEW_XML:
            if path.suffix == ".xml" and path.parent.name.startswith(("layout", "drawable")):
                ET.parse(path)  # raises on malformed XML

    def test_format_strings_match_their_call_sites(self):
        # The one parameterised string of this feature, checked by hand because there is no general
        # getString scanner here: two numbers, one for the total and one for the free count.
        ads = read(RES / "values/ads.xml")
        self.assertIn("%1$d", ads)
        self.assertIn("%2$d", ads)
        call = re.search(r"getString\(R\.string\.wallpaper_section_count([^)]*)\)",
                         read(JAVA / "adapter/CategoryWallpaperAdapter.java"))
        self.assertIsNotNone(call, "the section caption must be built from the string resource")
        self.assertEqual(2, len([part for part in call.group(1).split(",") if part.strip()]))

    def test_new_strings_are_arabic_or_namespaced_english(self):
        ads = read(RES / "values/ads.xml")
        names = re.findall(r'<string name="([a-z_0-9]+)"', ads)
        for required in ("ad_label", "unlock_clock_cta", "unlock_wallpaper_cta", "unlock_later",
                         "ad_offline_message", "ad_unavailable_message", "ad_busy_message",
                         "wallpaper_all_title", "clock_section_hint", "wallpaper_section_hint",
                         "content_locked"):
            self.assertIn(required, names, f"values/ads.xml must define {required}")
        self.assertEqual(len(names), len(set(names)), "duplicate string names in values/ads.xml")


class ClockCatalogTests(unittest.TestCase):
    """The three clock sections, the previews on disk and the generator that made them."""

    SECTIONS = {"analog": 14, "digital": 12, "smart": 12}
    FREE = 9

    def test_counts_and_free_tier(self):
        source = read(JAVA / "utils/GetClocks.java")
        for section, expected in self.SECTIONS.items():
            rows = clock_table(section.upper())
            self.assertEqual(expected, len(rows), f"{section} must hold {expected} clocks")
        self.assertIn("isFree(index)", source)
        self.assertIn("FREE_CLOCKS_PER_SECTION", source)
        self.assertEqual(self.FREE, java_number("FREE_CLOCKS_PER_SECTION"))

    def test_ids_match_the_preview_files(self):
        for section, expected in self.SECTIONS.items():
            directory = ASSETS / "previews/clock" / section
            files = sorted(p.stem for p in directory.glob("*.png"))
            self.assertEqual([f"{section}_{i:02d}" for i in range(1, expected + 1)], files,
                             f"{directory.relative_to(ROOT)} must hold exactly {expected} previews")

    def test_digital_and_smart_have_a_rewarded_tail(self):
        # The point of the extra variants: a section is not finished when only its last row is locked.
        for section in ("digital", "smart"):
            self.assertGreater(len(clock_table(section.upper())), self.FREE,
                               f"{section} needs more than the free tier so unlocking is meaningful")

    def test_style_indices_stay_inside_the_renderers(self):
        # textClockPosition is persisted by the editor, so a catalog row must never name a style the
        # renderers do not implement: digital has 10 layouts, smart 11.
        limits = {"DIGITAL": 10, "SMART": 11}
        for table, limit in limits.items():
            styles = []
            for row in clock_table(table):
                styles.append(int(re.findall(r",\s*(\d+)\s*}", row)[-1]))
            self.assertTrue(all(0 <= style < limit for style in styles),
                            f"{table} style index outside 0..{limit - 1}: {styles}")
            self.assertEqual(limit, len(set(styles)),
                             f"{table} must reach every layout the renderer implements")
        # The list index and the style index must be able to differ, otherwise the table is redundant.
        digital_styles = [int(re.findall(r",\s*(\d+)\s*}", row)[-1]) for row in clock_table("DIGITAL")]
        self.assertTrue(any(style != index for index, style in enumerate(digital_styles)),
                        "DIGITAL should not be a plain 0..n ladder of styles")

    def test_analog_colours_match_the_generator(self):
        build = __import__("build_local_previews")
        table_colors = re.findall(r'"(#[0-9A-Fa-f]{6})"', "\n".join(clock_table("ANALOG")))
        self.assertEqual(list(build.ANALOG_PALETTES), table_colors,
                         "GetClocks.ANALOG and the preview generator disagree on the dial colours")
        digital_colors = re.findall(r'"(#[0-9A-Fa-f]{6})"', "\n".join(clock_table("DIGITAL")))
        self.assertEqual(list(build.DIGITAL_PALETTES), digital_colors)

    def test_wallpaper_counts_match_the_generator(self):
        build = __import__("build_local_previews")
        index = json.loads(read(ASSETS / "wallpapers/index.json"))
        self.assertEqual(len(build.SECTIONS), len(index["sections"]))
        self.assertEqual(build.FREE_PER_SECTION, java_number("FREE_WALLPAPERS_PER_SECTION"))
        self.assertEqual(build.IMAGES_PER_SECTION, len(index["sections"][0]["items"]))

    def test_editor_still_receives_a_style_not_a_position(self):
        activity = read(JAVA / "activity/ClockCardActivity.java")
        self.assertIn('putInt("textClockPosition", textClocks.getStyle())', activity)
        self.assertIn('putInt("textClockPosition", smartClocks.getStyle())', activity)
        self.assertNotIn('putInt("textClockPosition", i)', activity)
        self.assertNotIn('putInt("textClockPosition", position)', activity)


class WallpaperAssetTests(unittest.TestCase):
    """Six sections, six images each, three free, all local."""

    EXPECTED = {
        "mecca": "مساجد مكة",
        "madinah": "مساجد المدينة المنورة",
        "aqsa": "المسجد الأقصى",
        "mosques": "مساجد أخرى",
        "minarets": "مآذن",
        "allah": 'صور مكتوب عليها "الله"',
    }

    def setUp(self):
        self.index = json.loads(read(ASSETS / "wallpapers/index.json"))

    def test_sections_and_free_tier(self):
        self.assertEqual(list(self.EXPECTED), [section["id"] for section in self.index["sections"]])
        for section in self.index["sections"]:
            self.assertEqual(self.EXPECTED[section["id"]], section["name"])
            items = section["items"]
            self.assertEqual(6, len(items), f"{section['id']} must hold six images")
            self.assertEqual([True, True, True, False, False, False],
                             [item["free"] for item in items],
                             f"{section['id']}: the first three are the free tier")
            for number, item in enumerate(items, start=1):
                self.assertEqual(f"{section['id']}_{number}", item["id"])
                self.assertEqual(f"wallpapers/{section['id']}/{section['id']}_{number}.jpg",
                                 item["file"])
                self.assertTrue((ASSETS / item["file"]).is_file(), f"missing {item['file']}")
                self.assertTrue(item["title"].strip(), "every wallpaper needs a title")

    def test_no_remote_catalog_survives(self):
        self.assertFalse((ASSETS / "wallpaper.json").exists())
        self.assertFalse((ASSETS / "wallpapernew.json").exists())
        for name in ("ResponseWallpaper.java", "ResponseWallpaperItem.java", "ImageUrlsItem.java"):
            self.assertFalse((JAVA / "model" / name).exists(), f"{name} belongs to the removed API")
        listing = "\n".join(read(path) for path in (JAVA / "catalog").glob("*.java"))
        self.assertNotIn("http", listing)

    def test_image_loading_libraries_stay_out_of_the_build(self):
        # Only the dependency lines matter: a comment may name a library that was removed.
        gradle = "\n".join(line for line in read(ROOT / "app/build.gradle").splitlines()
                           if line.strip().startswith("implementation"))
        for forbidden in ("com.github.bumptech.glide", "com.squareup.picasso", "com.android.volley",
                          "okdownload", "filedownloader"):
            self.assertNotIn(forbidden, gradle,
                             f"{forbidden} was removed with the remote wallpaper catalog")

    def test_no_third_party_image_loader_in_the_browser_paths(self):
        for rel, source in sources(FEATURE_JAVA).items():
            for forbidden in ("com.bumptech.glide", "com.squareup.picasso", "Volley",
                             "com.liulishuo.okdownload"):
                self.assertNotIn(forbidden, source, f"{rel} still uses {forbidden}")
            self.assertNotIn("ImageView.setImageURI(Uri.parse(\"http", source)

    def test_lock_overlay_is_bound_in_every_tile(self):
        for layout in ("item_clocks.xml", "item_textclock.xml", "item_smarttextclock.xml",
                       "item_wallpaper.xml"):
            text = read(RES / "layout" / layout)
            self.assertIn("@+id/lockScrim", text, f"{layout} cannot show a locked state")
            self.assertIn("@+id/lockBadge", text, f"{layout} has no padlock badge")
            self.assertIn("@drawable/ic_locked", text)


class AdRuleTests(unittest.TestCase):
    """The rules of the ad specification, asserted on the source."""

    def test_interstitials_are_gone(self):
        # A comment may explain why the format is absent; the code must not mention it at all.
        for path in JAVA.rglob("*.java"):
            source = strip_comments(read(path))
            self.assertNotIn("InterstitialAd", source, f"{path.name} still loads interstitials")
            self.assertNotIn("interstitial", source.lower(), f"{path.name} still loads interstitials")

    def test_legacy_admob_class_is_gone(self):
        self.assertFalse((JAVA / "AdAdmob.java").exists())
        for rel, source in sources(list(JAVA.rglob("*.java"))).items():
            self.assertNotIn("AdAdmob", source, f"{rel} still uses the removed AdAdmob helper")
            self.assertNotIn("/6499/example/banner", source, "the legacy sample banner id must not ship")

    def test_no_ad_when_a_section_is_opened(self):
        chooser = read(JAVA / "activity/ClockFuntionActivity.java")
        self.assertNotIn("AdsManager", chooser)
        self.assertNotIn("loadAd", chooser)
        for activity in ("ClockCardActivity.java", "WallpaperActivity.java",
                         "WallpaperCategoryActivity.java"):
            source = read(JAVA / "activity" / activity)
            self.assertNotIn("loadAd", source, f"{activity} must not request an ad on open")
            self.assertNotIn("AdView", source)
        # The rewarded ad may only be preloaded, never shown, while building a list.
        self.assertIn("preloadRewarded", read(JAVA / "activity/ClockCardActivity.java"))

    def test_no_banner_container_left_in_the_chrome(self):
        for layout in ("activity_clock_card.xml", "activity_wallpaper.xml",
                       "activity_wallpaper_category.xml", "activity_clock_funtion.xml",
                       "activity_set_wallpaper.xml", "activity_editor.xml"):
            text = read(RES / "layout" / layout)
            for forbidden in ("bannerAd", "adContainer", "AdView", "nativeAd"):
                self.assertNotIn(forbidden, text, f"{layout} still reserves space for {forbidden}")

    def test_native_ad_uses_the_official_view(self):
        card = read(RES / "layout/ad_native.xml")
        self.assertTrue(card.lstrip().splitlines()[1].startswith(
            "<com.google.android.gms.ads.nativead.NativeAdView"),
            "the native ad card must be a NativeAdView")
        for required in ("adHeadline", "adBody", "adAdvertiser", "adIcon", "adMedia", "adCallToAction",
                         "adAttribution", "adBadge"):
            self.assertIn("@+id/" + required, card)
        self.assertIn("@string/ad_label", card, "the ad badge must read 'إعلان' from resources")
        self.assertIn("nativead.MediaView", card, "video ads need a MediaView of at least 120dp")
        media = re.search(r'<com\.google\.android\.gms\.ads\.nativead\.MediaView.*?/>', card, re.S)
        self.assertIsNotNone(media)
        self.assertGreaterEqual(int(re.search(r'layout_height="(\d+)dp"', media.group(0)).group(1)), 120)

    def test_native_ad_slot_collapses_when_empty(self):
        slot = read(RES / "layout/ad_native_slot.xml")
        self.assertNotIn("layout_margin", slot, "a collapsed slot must leave no strip behind")
        placement = read(ADS / "NativePlacement.java")
        self.assertIn("View.GONE", placement)
        self.assertIn("removeAllViews", placement)

    def test_spacing_and_suppression(self):
        interval = java_number("NATIVE_AD_INTERVAL")
        self.assertTrue(6 <= interval <= 10, "NATIVE_AD_INTERVAL must stay inside the 6..10 range")
        self.assertGreaterEqual(java_number("NATIVE_AD_MIN_ITEMS"), interval + 2,
                                "a list of about one interval must not host an ad")
        policy = read(ADS / "AdPolicy.java")
        self.assertIn("NATIVE_AD_MIN_ITEMS", policy)
        self.assertIn("nativeAdsAllowed()", policy)
        # Refresh rules from the SDK guidance, encoded once.
        self.assertGreaterEqual(java_number("NATIVE_AD_REQUEST_COOLDOWN_MS"), 60_000)
        self.assertLessEqual(java_number("NATIVE_AD_TTL_MS"), 60 * 60 * 1000)

    def test_ad_unit_ids_are_the_test_ids_defined_once(self):
        config = read(ADS / "AdConfig.java")
        for key, unit in TEST_UNITS.items():
            if key in ("BANNER", "INTERSTITIAL"):
                continue  # these formats are not used at all
            self.assertIn(f'"{unit}"', config, f"{key}_AD_UNIT_ID must be the official test id")
        for rel, source in sources(list(JAVA.rglob("*.java"))).items():
            for unit in TEST_UNITS.values():
                if "AdConfig.java" in rel:
                    continue
                self.assertNotIn(unit, source, f"{rel} hard-codes an ad unit id; use AdConfig")
        self.assertIn("NATIVE_AD_UNIT_ID", read(ADS / "AdsManager.java"))
        manifest = read(MAIN / "AndroidManifest.xml")
        self.assertIn("ca-app-pub-3940256099942544~3347511713", manifest)

    def test_quran_section_blocks_every_ad_format(self):
        for activity in ("QuranActivity.java", "SurahListActivity.java"):
            source = read(JAVA / "activity" / activity)
            self.assertIn("AdPolicy.enterQuranScreen()", source)
            self.assertIn("AdPolicy.exitQuranScreen()", source)
        for path in list(JAVA.rglob("*.java")):
            name = path.name
            if not (name.startswith(("Quran", "Surah", "Azkar", "Ayah")) or "azkar" in str(path)):
                continue
            source = read(path)
            for forbidden in ("AdView", "AdRequest", "AdsManager", "NativeAd", "RewardedAd",
                              "AppOpenAd", "mobile_ads"):
                self.assertNotIn(forbidden, source, f"{name} must stay ad free, found {forbidden}")
        policy = read(ADS / "AdPolicy.java")
        self.assertIn("isQuranSectionActive()", policy)
        self.assertIn("lastLeftQuranAt", policy)
        # App open is refused for a moment after leaving the section, so the exit shows no ad.
        self.assertIn("APP_OPEN_AFTER_QURAN_MS", policy)
        self.assertGreater(java_number("APP_OPEN_AFTER_QURAN_MS"), 0)

    def test_quran_layouts_have_no_ad_container(self):
        for layout in sorted(RES.glob("layout/*quran*.xml")) + sorted(RES.glob("layout/*surah*.xml")) \
                + sorted(RES.glob("layout/*azkar*.xml")):
            text = read(layout)
            for forbidden in ("bannerAd", "adContainer", "AdView", "adSlot", "homeNativeAd"):
                self.assertNotIn(forbidden, text, f"{layout.name} reserves ad space inside a protected area")

    def test_app_open_rules(self):
        policy = read(ADS / "AdPolicy.java")
        self.assertIn("APP_OPEN_MIN_INTERVAL_MS", policy)
        self.assertIn("APP_OPEN_AFTER_REWARDED_MS", policy)
        self.assertIn("APP_OPEN_LAUNCHES_BEFORE_FIRST_AD", policy)
        self.assertIn("markSystemHandoff", policy)
        watcher = read(ADS / "AppForegroundWatcher.java")
        self.assertIn("hasBeenBackgrounded", watcher)
        self.assertIn("consumeSystemHandoff", watcher)
        manager = read(ADS / "AdsManager.java")
        self.assertIn("isShowingFullScreenAd", manager)
        app = read(JAVA / "AppClass.java")
        self.assertIn("AppForegroundWatcher.register(this)", app)
        self.assertIn("AdPolicy.registerLaunch(this)", app)

    def test_rewarded_is_the_only_unlock_path_and_is_optional(self):
        prompt = read(JAVA / "unlock/UnlockPrompt.java")
        self.assertIn("setNegativeButton", prompt, "the dialog must offer a way to say no")
        self.assertIn("R.string.unlock_later", prompt)
        self.assertIn("requestUnlock", prompt)
        self.assertIn("UnlockStore.get(activity).isUnlocked(unlockKey)", prompt,
                      "an already unlocked item must open without asking again")
        cta = read(RES / "values/ads.xml")
        self.assertIn("مشاهدة إعلان لفتح هذه الساعة", cta)
        self.assertIn("مشاهدة إعلان لفتح هذه الخلفية", cta)

    def test_unlock_is_permanent_and_local(self):
        store = read(JAVA / "unlock/UnlockStore.java")
        self.assertIn("getSharedPreferences", store)
        self.assertIn("MODE_PRIVATE", store)
        self.assertTrue(re.search(r"\.putBoolean\([^;]+,\s*true\)[^;]*\.apply\(\)", store)
                        or re.search(r"edit\(\)[^;]*putBoolean[^;]*apply\(\)", store, re.S),
                        "an earned reward must be written to disk immediately")
        manager = read(ADS / "AdsManager.java")
        self.assertIn("UnlockStore", manager)
        self.assertIn("unlock(", manager)

    def test_free_content_never_touches_the_ad_layer(self):
        access = read(JAVA / "catalog/ContentAccess.java")
        ready = access[access.index("public static void open"):access.index("isClockContent()")]
        self.assertNotIn("AdsManager", ready, "a free item must open without any ad call")
        self.assertIn("isAvailable(activity, item)", ready)

    def test_ad_free_build_has_no_locked_tiles(self):
        # Flipping the master switch off must unlock the *UI* as well as the ad calls, otherwise tiles
        # would carry a padlock that opens without an ad.
        store = read(JAVA / "unlock/UnlockStore.java")
        self.assertIn("freeByDefault || !AdConfig.ADS_ENABLED || isUnlocked(unlockKey)", store)

    def test_lists_gate_their_clicks_through_content_access(self):
        for activity in ("ClockCardActivity.java", "WallpaperActivity.java"):
            self.assertIn("ContentAccess.open(", read(JAVA / "activity" / activity))
        # The preview screen itself is reached only for usable content and shows no ad.
        self.assertNotIn("AdsManager", read(JAVA / "activity/SetWallpaperActivity.java"))
        self.assertNotIn("AdPolicy.markAdShowing", read(JAVA / "activity/SetWallpaperActivity.java"))

    def test_ad_label_is_arabic_and_separated_from_the_buttons(self):
        ads = read(RES / "values/ads.xml")
        self.assertIn('<string name="ad_label">إعلان</string>', ads)
        home = read(RES / "layout/activity_select_function.xml")
        slot_at = home.index("homeNativeAd")
        button_at = home.index("frameAzkar")
        self.assertGreater(slot_at, button_at,
                           "the home ad must be below the section buttons, not between them")
        self.assertIn("android:layout_marginTop", home[slot_at:slot_at + 600],
                      "the ad needs its own space under the buttons")

    def test_no_ad_inside_the_clock_screen(self):
        editor = read(RES / "layout/activity_editor.xml")
        for forbidden in ("adSlot", "AdView", "homeNativeAd", "bannerAd", "adContainer"):
            self.assertNotIn(forbidden, editor)
        self.assertNotIn("AdsManager", read(JAVA / "activity/EditorActivity.java"))


class SourceHygieneTests(unittest.TestCase):

    def test_braces_and_packages_balance(self):
        for rel, source in sources(FEATURE_JAVA).items():
            self.assertEqual(source.count("{"), source.count("}"), f"{rel} has unbalanced braces")
            tail = rel.split("com/clock/livewallpaper/", 1)[1]
            package = tail.rsplit("/", 1)[0].replace("/", ".") if "/" in tail else ""
            expected = "com.clock.livewallpaper." + package if package else "com.clock.livewallpaper"
            self.assertIn(f"package {expected};", source, f"{rel} declares the wrong package")

    def test_no_todo_left_in_the_new_code(self):
        for rel, source in sources(FEATURE_JAVA).items():
            for marker in ("TODO", "FIXME", "XXX", "placeholder impl"):
                self.assertNotIn(marker, source, f"{rel} still contains {marker}")

    def test_every_new_layout_is_referenced(self):
        java = "\n".join(read(path) for path in JAVA.rglob("*.java"))
        xml = "\n".join(read(path) for path in RES.glob("layout/*.xml"))
        for layout in ("ad_native", "ad_native_slot"):
            self.assertIn(f"R.layout.{layout}", java, f"{layout}.xml is not used by any screen")
        for id_name in ("lockScrim", "lockBadge", "previewImage"):
            self.assertIn(f"R.id.{id_name}", java, f"{id_name} is declared but never bound")

    def test_no_url_in_the_catalog_or_ad_layer(self):
        for rel, source in sources(FEATURE_JAVA).items():
            for match in re.findall(r'"(https?://[^"]+)"', source):
                self.fail(f"{rel} points at {match}; all content and assets must be local or from the SDK")


if __name__ == "__main__":
    unittest.main(verbosity=2)
