"""Azkar feature contract checks: data, sources, counters, persistence and wiring.

There is no JDK or Android SDK in this workspace, so nothing here compiles the app. These
checks instead verify the parts of the Azkar feature that are pure data and pure structure:

* assets/azkar.json holds the exact per-item repetition counts from the mandatory sources,
  in source order, with non-empty Arabic text;
* the three source URLs stay attached to the implementation (JSON, repository, values XML);
* the counter/persistence logic keeps its contract (clamped count-up, immediate save,
  per-item preferences keys, computed totals);
* every resource the Azkar screens reference resolves against res/ and the manifest.

Run with:
    python3 -m unittest discover -s tests -p 'test_azkar.py' -v
"""
import json
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
JAVA = MAIN / "java/com/clock/livewallpaper"
AZKAR_PKG = JAVA / "azkar"

ASSET = MAIN / "assets/azkar.json"

AZKAR_JAVA = [
    AZKAR_PKG / "AzkarCategory.java",
    AZKAR_PKG / "AzkarItem.java",
    AZKAR_PKG / "AzkarProgressStore.java",
    AZKAR_PKG / "AzkarRepository.java",
    AZKAR_PKG / "AzkarFonts.java",
    JAVA / "activity/AzkarHomeActivity.java",
    JAVA / "activity/AzkarListActivity.java",
    JAVA / "adapter/AzkarAdapter.java",
    JAVA / "activity/MainActivity.java",
]
AZKAR_XML = [
    MAIN / "AndroidManifest.xml",
    RES / "layout/activity_select_function.xml",
    RES / "layout/activity_azkar_home.xml",
    RES / "layout/activity_azkar_list.xml",
    RES / "layout/item_azkar.xml",
    RES / "values/azkar.xml",
    RES / "values/colors.xml",
    RES / "drawable/bg_azkar_button.xml",
    RES / "drawable/bg_azkar_home_card.xml",
    RES / "drawable/bg_azkar_card.xml",
    RES / "drawable/bg_azkar_card_completed.xml",
    RES / "drawable/bg_azkar_counter.xml",
    RES / "drawable/bg_azkar_counter_completed.xml",
    RES / "drawable/bg_azkar_badge.xml",
    RES / "drawable/bg_azkar_chip.xml",
    RES / "drawable/azkar_progress.xml",
    RES / "drawable/ic_azkar.xml",
    RES / "drawable/ic_azkar_morning.xml",
    RES / "drawable/ic_azkar_evening.xml",
    RES / "drawable/ic_azkar_tasbeeh.xml",
]

# Exact repetition counts transcribed from the mandatory sources, in source order.
EXPECTED_REPEATS = {
    "morning": [1, 3, 3, 3, 1, 1, 3, 4, 1, 7, 3, 1, 1, 3, 3, 3, 1, 3, 1, 1, 3, 10, 3, 3, 3, 3,
                1, 1, 100, 100, 100],
    "evening": [1, 1, 3, 3, 3, 1, 1, 3, 4, 1, 7, 3, 1, 1, 3, 3, 3, 1, 3, 1, 1, 3, 10, 3, 3, 3,
                3, 100, 1, 100],
    "tasbeeh": [100] * 17,
}

EXPECTED_SOURCES = {
    "morning": "https://www.islambook.com/azkar/1/أذكار-الصباح",
    "evening": "https://www.islambook.com/azkar/2/أذكار-المساء",
    "tasbeeh": "https://www.islamiokul.com/arabic/zikirler.html",
}

ARABIC_BLOCK = re.compile(r"[\u0600-\u06FF]")

ANDROID = "{http://schemas.android.com/apk/res/android}"


def defined_values():
    found = {}
    for path in sorted(RES.glob("values*/*.xml")):
        for node in ET.parse(path).getroot():
            if not isinstance(node.tag, str):
                continue
            name = node.get("name")
            if node.tag == "item" and node.get("type"):
                found.setdefault(node.get("type"), set()).add(name)
            elif name:
                found.setdefault(node.tag, set()).add(name)
    return found


def defined_files():
    found = {}
    for path in sorted(RES.rglob("*")):
        if not path.is_file() or "." not in path.name:
            continue
        kind = path.parent.name.split("-")[0]
        if kind in {"drawable", "layout", "font", "color", "xml", "values"}:
            found.setdefault(kind, set()).add(path.name.rsplit(".", 1)[0])
    return found


def defined_ids():
    ids = set()
    for path in RES.glob("layout*/*.xml"):
        ids.update(re.findall(r"@\+id/([A-Za-z0-9_]+)", path.read_text(encoding="utf-8")))
    return ids


class AzkarDataTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(ASSET.read_text(encoding="utf-8"))

    def test_source_urls_present_in_asset(self):
        self.assertEqual(self.data["sources"], EXPECTED_SOURCES)

    def test_item_counts(self):
        self.assertEqual(len(self.data["morning"]), 31)
        self.assertEqual(len(self.data["evening"]), 30)
        self.assertEqual(len(self.data["tasbeeh"]), 17)

    def test_schema_and_sequences(self):
        for category, items in self.data.items():
            if category == "sources":
                continue
            for index, item in enumerate(items, start=1):
                self.assertEqual(set(item), {"id", "order", "text", "repeat", "virtue"},
                                 f"{category}#{index}: schema drift")
                self.assertEqual(item["id"], index, f"{category}: ids must be 1..N in order")
                self.assertEqual(item["order"], index, f"{category}: order must follow the source")
                self.assertIsInstance(item["repeat"], int)
                self.assertGreaterEqual(item["repeat"], 1)
                self.assertIsInstance(item["virtue"], str)

    def test_exact_repeat_counts_per_item(self):
        """Every Azkar keeps its own count from the source; never a shared constant."""
        for category, expected in EXPECTED_REPEATS.items():
            actual = [item["repeat"] for item in self.data[category]]
            self.assertEqual(actual, expected, f"{category}: repeat counts drifted from source")

    def test_counts_are_not_uniform(self):
        """Guards against assigning one repetition number to every Azkar."""
        self.assertGreater(len(set(EXPECTED_REPEATS["morning"])), 1)
        self.assertGreater(len(set(EXPECTED_REPEATS["evening"])), 1)

    def test_evening_differs_from_morning(self):
        """Evening counts must be read from the Evening source, not copied from Morning."""
        morning = [i["repeat"] for i in self.data["morning"]]
        evening = [i["repeat"] for i in self.data["evening"]]
        self.assertNotEqual(morning, evening)
        # The Evening-only "last two verses of Al-Baqarah" item sits at #2 with count 1.
        self.assertIn("آمَنَ الرَّسُولُ", self.data["evening"][1]["text"])

    def test_arabic_text_present_and_rtl(self):
        for category, items in self.data.items():
            if category == "sources":
                continue
            for item in items:
                self.assertTrue(item["text"].strip(), f"{category}#{item['id']}: empty text")
                self.assertRegex(item["text"], ARABIC_BLOCK,
                                 f"{category}#{item['id']}: no Arabic script found")

    def test_multiline_texts_preserved(self):
        """Items with an istiadhah/basmalah header keep their paragraph break."""
        self.assertIn("\n\n", self.data["morning"][0]["text"])
        self.assertIn("آية الكرسى - البقرة 255", self.data["morning"][0]["text"])
        self.assertIn("البقرة 285 - 286", self.data["evening"][1]["text"])


class AzkarSourceAttachmentTests(unittest.TestCase):
    def test_repository_holds_all_source_urls(self):
        source = (AZKAR_PKG / "AzkarRepository.java").read_text(encoding="utf-8")
        for url in EXPECTED_SOURCES.values():
            self.assertIn(url, source, f"AzkarRepository lost source {url}")

    def test_values_xml_documents_sources(self):
        source = (RES / "values/azkar.xml").read_text(encoding="utf-8")
        for url in EXPECTED_SOURCES.values():
            self.assertIn(url, source, f"azkar.xml lost source {url}")

    def test_list_footer_shows_source_at_runtime(self):
        source = (JAVA / "activity/AzkarListActivity.java").read_text(encoding="utf-8")
        self.assertIn("category.sourceUrl()", source)
        self.assertIn("azkar_list_source", source)


class AzkarLogicTests(unittest.TestCase):
    def test_count_up_clamps_at_repeat_count(self):
        source = (AZKAR_PKG / "AzkarItem.java").read_text(encoding="utf-8")
        self.assertIn("currentCount >= repeatCount", source)
        self.assertIn("Math.min(value, repeatCount)", source)

    def test_completed_derived_not_stored(self):
        source = (AZKAR_PKG / "AzkarItem.java").read_text(encoding="utf-8")
        self.assertRegex(source, r"boolean isCompleted\(\) \{\s*return currentCount >= repeatCount;")

    def test_reset_returns_to_zero(self):
        source = (AZKAR_PKG / "AzkarItem.java").read_text(encoding="utf-8")
        self.assertRegex(source, r"void reset\(\) \{\s*currentCount = 0;")

    def test_tap_persists_immediately(self):
        source = (JAVA / "adapter/AzkarAdapter.java").read_text(encoding="utf-8")
        self.assertIn("item.countUp()", source)
        self.assertIn("repository.save(item)", source)
        self.assertIn("repository.reset(item)", source)

    def test_progress_store_keys_are_per_item(self):
        source = (AZKAR_PKG / "AzkarProgressStore.java").read_text(encoding="utf-8")
        self.assertIn('"azkar_prefs"', source)
        self.assertIn('"azkar_count_"', source)
        self.assertIn('KEY_PREFIX + category.key() + "_" + id', source)
        self.assertIn(".apply()", source)

    def test_totals_computed_never_hardcoded(self):
        home = (JAVA / "activity/AzkarHomeActivity.java").read_text(encoding="utf-8")
        listing = (JAVA / "activity/AzkarListActivity.java").read_text(encoding="utf-8")
        self.assertIn("repository.totalCount(category)", home)
        self.assertIn("items.size()", listing)
        for layout in ("activity_azkar_home.xml", "activity_azkar_list.xml"):
            xml = (RES / f"layout/{layout}").read_text(encoding="utf-8")
            self.assertNotIn("android:max=", xml, f"{layout}: totals must come from the data")

    def test_adapter_uses_recyclerview_1_1_api(self):
        """getBindingAdapterPosition() needs recyclerview 1.2+; the app ships 1.1.0."""
        source = (JAVA / "adapter/AzkarAdapter.java").read_text(encoding="utf-8")
        code = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
        code = re.sub(r"//.*", "", code)
        self.assertNotIn("getBindingAdapterPosition", code)
        self.assertIn("getAdapterPosition()", code)
        self.assertIn("RecyclerView.NO_POSITION", code)

    def test_no_daily_reset(self):
        """Progress persists until the user resets; grep for accidental schedulers."""
        for name in ("AzkarProgressStore.java", "AzkarRepository.java"):
            source = (AZKAR_PKG / name).read_text(encoding="utf-8")
            self.assertNotIn("AlarmManager", source)
            self.assertNotIn("WorkManager", source)


class AzkarWiringTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.values = defined_values()
        cls.files = defined_files()
        cls.ids = defined_ids()
        cls.sources = {p: p.read_text(encoding="utf-8") for p in AZKAR_JAVA}

    def assert_resolves(self, kind, name, where):
        if kind == "id":
            self.assertIn(name, self.ids, f"{where}: R.id.{name} has no @+id/")
        elif kind in {"drawable", "layout", "font", "xml", "mipmap"}:
            self.assertIn(name, self.files.get(kind, set()),
                           f"{where}: R.{kind}.{name} file is missing")
        elif kind == "color":
            # @color/ resolves to either a values <color> or a res/color/ file.
            known = self.values.get("color", set()) | self.files.get("color", set())
            self.assertIn(name, known, f"{where}: R.color.{name} is not defined")
        else:
            pool = set()
            for key in (kind, "item"):
                pool |= self.values.get(key, set())
            if kind == "style":
                pool |= self.values.get("style", set())
            self.assertIn(name, pool, f"{where}: R.{kind}.{name} is not defined")

    def test_java_references_resolve(self):
        for path, source in self.sources.items():
            # Only the references this feature's own files make; MainActivity predates it but
            # the Azkar button references must resolve too.
            for kind, name in re.findall(r"R\.(id|string|color|drawable|layout|font)\.(\w+)",
                                         source):
                if path.name == "MainActivity.java" and "zkar" not in name and "zkar" not in "":
                    # MainActivity also references legacy ids; those are out of scope here.
                    if name not in {"frameAzkar"} and "Azkar" not in source[max(0, source.find(name) - 200):source.find(name)]:
                        continue
                self.assert_resolves(kind, name, path.name)

    def test_xml_references_resolve(self):
        for path in AZKAR_XML:
            text = path.read_text(encoding="utf-8")
            # Strip XML comments: azkar.xml documents source URLs containing '/' and ':'.
            text = re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)
            for ref in re.findall(r"@([a-z]+)/([A-Za-z0-9_]+)", text):
                kind, name = ref
                if kind == "android":
                    continue
                where = f"{path.parent.name}/{path.name}"
                if kind == "null":
                    continue
                self.assert_resolves(kind, name, where)
            for theme in re.findall(r"@style/(\w+)", path.read_text(encoding="utf-8")):
                self.assert_resolves("style", theme, path.name)

    def test_manifest_registers_azkar_screens(self):
        manifest = (MAIN / "AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("activity.AzkarHomeActivity", manifest)
        self.assertIn("activity.AzkarListActivity", manifest)
        self.assertEqual(manifest.count("AzkarTheme"), 2)

    def test_home_icon_wiring(self):
        layout = (RES / "layout/activity_select_function.xml").read_text(encoding="utf-8")
        self.assertIn('@+id/frameAzkar', layout)
        self.assertIn('@drawable/bg_azkar_button', layout)
        self.assertIn('@drawable/ic_azkar', layout)
        self.assertIn('@string/azkar_title', layout)
        main = (JAVA / "activity/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("frameAzkar", main)
        self.assertIn("AzkarHomeActivity.class", main)

    def test_existing_quran_entry_untouched(self):
        """The Azkar button is additive: the Quran entry keeps working as before."""
        main = (JAVA / "activity/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("SurahListActivity.class", main)
        layout = (RES / "layout/activity_select_function.xml").read_text(encoding="utf-8")
        self.assertIn('@+id/frameQuran', layout)

    def test_home_has_three_matching_cards(self):
        root = ET.parse(RES / "layout/activity_azkar_home.xml").getroot()
        cards = [n for n in root.iter()
                 if (n.get(f"{ANDROID}id") or "").startswith("@+id/azkar_card_")]
        self.assertEqual(len(cards), 3)
        # Identical dimensions, spacing rhythm and elevation across the three cards.
        heights = {c.get(f"{ANDROID}minHeight") for c in cards}
        paddings = {c.get(f"{ANDROID}padding") for c in cards}
        elevations = {c.get(f"{ANDROID}elevation") for c in cards}
        backgrounds = {c.get(f"{ANDROID}background") for c in cards}
        self.assertEqual(heights, {"104dp"})
        self.assertEqual(paddings, {"16dp"})
        self.assertEqual(elevations, {"3dp"})
        self.assertEqual(backgrounds, {"@drawable/bg_azkar_home_card"})

    def test_card_layout_has_counter_and_reset(self):
        root = ET.parse(RES / "layout/item_azkar.xml").getroot()
        ids = {n.get(f"{ANDROID}id", "").replace("@+id/", "") for n in root.iter()}
        for required in ("azkar_card", "azkar_number", "azkar_repeat", "azkar_reset",
                         "azkar_text", "azkar_virtue", "azkar_counter",
                         "azkar_counter_text", "azkar_counter_hint"):
            self.assertIn(required, ids)
        counters = [n for n in root.iter()
                    if n.get(f"{ANDROID}id") == "@+id/azkar_counter"]
        self.assertEqual(len(counters), 1)
        self.assertEqual(counters[0].get(f"{ANDROID}minHeight"), "64dp")

    def test_arabic_text_is_rtl(self):
        root = ET.parse(RES / "layout/item_azkar.xml").getroot()
        cards = [n for n in root.iter() if n.get(f"{ANDROID}id") == "@+id/azkar_card"]
        self.assertEqual(cards[0].get(f"{ANDROID}layoutDirection"), "rtl")

    def test_format_string_arity(self):
        strings = {n.get("name"): (n.text or "")
                   for n in ET.parse(RES / "values/azkar.xml").getroot() if n.tag == "string"}
        self.assertEqual(len(re.findall(r"%1\$d", strings["azkar_card_number"])), 1)
        self.assertEqual(len(re.findall(r"%1\$d", strings["azkar_repeat_chip"])), 1)
        self.assertIn("%1$d", strings["azkar_progress"])
        self.assertIn("%2$d", strings["azkar_progress"])
        # Every getString(call) in the Azkar screens passes the matching argument count.
        adapter = (JAVA / "adapter/AzkarAdapter.java").read_text(encoding="utf-8")
        self.assertIn("R.string.azkar_card_number, item.id()", adapter)
        self.assertIn("R.string.azkar_repeat_chip, item.repeatCount()", adapter)

    def test_required_colours_defined(self):
        colours = {n.get("name"): (n.text or "").strip()
                   for n in ET.parse(RES / "values/colors.xml").getroot() if n.tag == "color"}
        for name in ("azkar_background", "azkar_card", "azkar_text", "azkar_muted",
                     "azkar_primary", "azkar_primary_dark", "azkar_deep", "azkar_gold",
                     "azkar_gold_soft", "azkar_on_primary", "azkar_completed_bg",
                     "azkar_divider", "azkar_track"):
            self.assertIn(name, colours)
            self.assertRegex(colours[name], r"^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

    def test_vectors_are_well_formed(self):
        for name in ("ic_azkar", "ic_azkar_morning", "ic_azkar_evening", "ic_azkar_tasbeeh"):
            root = ET.parse(RES / f"drawable/{name}.xml").getroot()
            self.assertTrue(root.tag.endswith("vector"), name)
            paths = [n for n in root.iter() if n.tag.endswith("path")]
            self.assertGreaterEqual(len(paths), 2, name)
            for path in paths:
                self.assertTrue(path.get(f"{ANDROID}pathData"), f"{name}: empty path")


if __name__ == "__main__":
    unittest.main()
