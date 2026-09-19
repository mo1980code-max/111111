"""Offline Quran contract checks: metadata, the bundled database, the font and the layouts.

These are SDK-independent and never substitute for device testing, but they do verify the parts of
this feature that are pure data and pure structure -- which is most of what can silently break when
someone swaps the database asset, edits the metadata tables or renames a view id.

Run with:
    python3 -m unittest discover -s tests -p 'test_offline_quran.py' -v
"""
import bisect
import re
import shutil
import unicodedata
import sqlite3
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
JAVA = MAIN / "java/com/clock/livewallpaper"
METADATA = JAVA / "quran/QuranMetadata.java"
SURAH_INDEX = JAVA / "quran/SurahIndex.java"
DB_HELPER = JAVA / "quran/QuranDatabaseHelper.java"
DATABASE = MAIN / "assets/databases/quran.ar.uthmani.db"
FONT = MAIN / "assets/fonts/quran_font.ttf"
ANDROID = "{http://schemas.android.com/apk/res/android}"
TOOLS = "{http://schemas.android.com/tools}"

TOTAL_SURAHS = 114
TOTAL_AYAHS = 6236

REQUIRED_COLOURS = {
    "quran_background": "#FBF9F0",
    "quran_text": "#1A1A1A",
    "quran_highlight": "#D0ECE4",
    "quran_header": "#7A7A7A",
    "quran_button": "#2D7D46",
}

# Uthmani marks that must survive verbatim from the source text into the shipped database.
# Each entry was confirmed present in the bundled asset; a stripped or normalised copy fails here.
UTHMANI_MARKS = (
    ("\u0640", "tatweel"),
    ("\u0653", "maddah above"),
    ("\u0670", "superscript alef"),
    ("\u06D6", "sallallahou alayhe wasallam"),
    ("\u06DB", "three dots"),
    ("\u06E5", "small waw"),
)


def skeleton(text):
    """Reduce Arabic text to its consonantal skeleton.

    Drops every combining mark and the tatweel, so two spellings that differ only in diacritic
    ORDER compare equal. The bundled Tanzil text writes some marks in an order that differs from
    what typing the same word produces, and comparing against a typed literal would be a false
    failure -- or worse, would tempt someone to "fix" the authentic text.
    """
    return "".join(char for char in text
                   if unicodedata.category(char) != "Mn" and char != "\u0640")


# Consonantal skeleton of the basmallah, spelled out as codepoints so this file stays
# encoding-proof and no reviewer has to eyeball Arabic to check the expectation.
BASMALLAH_SKELETON = [
    "\u0628\u0633\u0645",                       # b-s-m
    "\u0671\u0644\u0644\u0647",                # (a)l-l-h
    "\u0671\u0644\u0631\u062d\u0645\u0646",  # (a)l-r-h-m-n
    "\u0671\u0644\u0631\u062d\u064a\u0645",  # (a)l-r-h-y-m
]


def has_fonttools():
    try:
        import fontTools  # noqa: F401
        return True
    except ImportError:
        return False


def numbers(name):
    """Extract an int[] literal from QuranMetadata.java."""
    text = METADATA.read_text(encoding="utf-8")
    body = re.search(r"int\[\] " + name + r" = \{(.*?)\};", text, re.S).group(1)
    return [int(n) for n in re.findall(r"\d+", body)]


def java_string_array(name):
    """Extract a String[] literal from SurahIndex.java."""
    text = SURAH_INDEX.read_text(encoding="utf-8")
    body = re.search(r"String\[\] " + name + r" = \{(.*?)\n    \};", text, re.S).group(1)
    return re.findall(r'"((?:[^"\\]|\\.)*)"', body)


def java_boolean_array(name):
    """Extract a boolean[] literal from SurahIndex.java."""
    text = SURAH_INDEX.read_text(encoding="utf-8")
    body = re.search(r"boolean\[\] " + name + r" = \{(.*?)\n    \};", text, re.S).group(1)
    return [token == "true" for token in re.findall(r"\b(true|false)\b", body)]


def all_ayah_texts():
    with sqlite3.connect(f"file:{DATABASE}?mode=ro", uri=True) as database:
        return [row[0] for row in database.execute("SELECT text FROM arabic_text ORDER BY sura, ayah")]


class MetadataTests(unittest.TestCase):
    """QuranMetadata drives the page and Juz labels, so its tables must be complete and monotonic."""

    def test_ayah_counts_are_complete(self):
        counts = numbers("AYAH_COUNTS")
        self.assertEqual(len(counts), TOTAL_SURAHS)
        self.assertEqual(sum(counts), TOTAL_AYAHS)
        self.assertTrue(all(count > 0 for count in counts))
        self.assertEqual(counts[0], 7, "Al-Fatihah has 7 ayahs")
        self.assertEqual(counts[1], 286, "Al-Baqarah has 286 ayahs")
        self.assertEqual(counts[113], 6, "An-Nas has 6 ayahs")

    def test_page_and_juz_tables_are_sorted_and_in_range(self):
        counts = numbers("AYAH_COUNTS")
        for name, size in (("PAGE_STARTS", 604), ("JUZ_STARTS", 30)):
            starts = numbers(name)
            self.assertEqual(len(starts), size)
            self.assertEqual(starts, sorted(set(starts)), f"{name} must be strictly increasing")
            self.assertEqual(starts[0], 1001, "the Mushaf starts at 1:1")
            for key in starts:
                surah, ayah = divmod(key, 1000)
                self.assertTrue(1 <= surah <= TOTAL_SURAHS)
                self.assertTrue(1 <= ayah <= counts[surah - 1],
                                f"{name} key {key} is not a real ayah")
            for surah, count in enumerate(counts, 1):
                for ayah in range(1, count + 1):
                    self.assertTrue(1 <= bisect.bisect_right(starts, surah * 1000 + ayah) <= size,
                                    f"{surah}:{ayah} maps outside 1..{size}")

    def test_known_page_and_juz_boundaries(self):
        pages = numbers("PAGE_STARTS")
        juz = numbers("JUZ_STARTS")
        for key, expected in ((1001, 1), (2001, 2), (2255, 42),
                              (5001, 106), (112001, 604), (114006, 604)):
            self.assertEqual(bisect.bisect_right(pages, key), expected, f"page for {key}")
        for key, expected in ((2141, 1), (2142, 2), (5081, 6), (5082, 7),
                              (9092, 10), (9093, 11), (114006, 30)):
            self.assertEqual(bisect.bisect_right(juz, key), expected, f"juz for {key}")
        # Juz boundaries inside a printed page must not be derived from the page number.
        self.assertEqual(bisect.bisect_right(pages, 5081), bisect.bisect_right(pages, 5082))
        self.assertEqual(bisect.bisect_right(pages, 9092), bisect.bisect_right(pages, 9093))


class SurahIndexTests(unittest.TestCase):
    """The 114 index rows come from Java arrays, so those arrays are the data source under test."""

    def test_every_array_has_114_entries(self):
        for name in ("ARABIC_NAMES", "ENGLISH_NAMES", "ENGLISH_MEANINGS"):
            values = java_string_array(name)
            self.assertEqual(len(values), TOTAL_SURAHS, f"{name} length")
            self.assertTrue(all(value.strip() for value in values), f"{name} has a blank entry")
        self.assertEqual(len(java_boolean_array("MEDINAN_FLAGS")), TOTAL_SURAHS)

    def test_first_and_last_surah_names(self):
        arabic = java_string_array("ARABIC_NAMES")
        english = java_string_array("ENGLISH_NAMES")
        self.assertEqual(arabic[0], "الفاتحة")
        self.assertEqual(english[0], "Al-Fatihah")
        self.assertEqual(arabic[1], "البقرة")
        self.assertEqual(english[1], "Al-Baqarah")
        self.assertEqual(arabic[113], "الناس")
        self.assertEqual(english[113], "An-Nas")

    def test_arabic_names_are_actually_arabic(self):
        for index, name in enumerate(java_string_array("ARABIC_NAMES"), start=1):
            letters = [c for c in name if c.isalpha()]
            self.assertTrue(letters, f"surah {index} name has no letters")
            self.assertTrue(all("\u0600" <= c <= "\u06FF" for c in letters),
                            f"surah {index} name {name!r} contains non-Arabic letters")

    def test_english_names_are_unique_and_latin(self):
        english = java_string_array("ENGLISH_NAMES")
        self.assertEqual(len(set(english)), TOTAL_SURAHS, "transliterations must not repeat")
        for name in english:
            self.assertTrue(all(ord(c) < 0x0600 for c in name), f"{name!r} is not Latin")

    def test_medinan_classification_is_the_conventional_28(self):
        flags = java_boolean_array("MEDINAN_FLAGS")
        medinan = {index for index, flag in enumerate(flags, start=1) if flag}
        self.assertEqual(len(medinan), 28, "the Egyptian classification has 28 Madinan surahs")
        for surah in (2, 3, 4, 5, 8, 9, 13, 22, 24, 33, 47, 48, 49, 55, 57, 58, 59, 60,
                      61, 62, 63, 64, 65, 66, 76, 98, 99, 110):
            self.assertIn(surah, medinan, f"surah {surah} should be Madinan")
        for surah in (1, 6, 7, 10, 36, 112, 114):
            self.assertNotIn(surah, medinan, f"surah {surah} should be Makkan")

    @unittest.skipUnless(DATABASE.exists(), "database not bundled")
    def test_index_ayah_counts_match_the_database(self):
        with sqlite3.connect(f"file:{DATABASE}?mode=ro", uri=True) as database:
            rows = database.execute("SELECT sura, COUNT(*) FROM arabic_text GROUP BY sura").fetchall()
        self.assertEqual(len(rows), TOTAL_SURAHS)
        counts = numbers("AYAH_COUNTS")
        for surah, count in rows:
            self.assertEqual(count, counts[surah - 1], f"surah {surah} ayah count")


@unittest.skipUnless(DATABASE.exists(), "quran.ar.uthmani.db is not bundled")
class BundledDatabaseTests(unittest.TestCase):
    """The shipped asset must be a complete, readable, correctly shaped Mushaf database."""

    def setUp(self):
        self.connection = sqlite3.connect(f"file:{DATABASE}?mode=ro", uri=True)

    def tearDown(self):
        self.connection.close()

    def test_integrity_and_schema(self):
        self.assertEqual(self.connection.execute("PRAGMA quick_check").fetchone()[0], "ok")
        definition = self.connection.execute(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='arabic_text'").fetchone()
        self.assertIsNotNone(definition, "arabic_text table is missing")
        columns = {row[1] for row in self.connection.execute("PRAGMA table_info(arabic_text)")}
        self.assertLessEqual({"sura", "ayah", "text"}, columns)

    def test_complete_and_ordered(self):
        rows = self.connection.execute(
            "SELECT sura, ayah, text FROM arabic_text ORDER BY sura, ayah").fetchall()
        self.assertEqual(len(rows), TOTAL_AYAHS)
        expected = [(surah, ayah)
                    for surah, count in enumerate(numbers("AYAH_COUNTS"), start=1)
                    for ayah in range(1, count + 1)]
        self.assertEqual([(s, a) for s, a, _ in rows], expected)
        self.assertTrue(all(text and text.strip() for _, _, text in rows), "blank ayah text found")

    def test_query_contract_used_by_the_helper(self):
        source = DB_HELPER.read_text(encoding="utf-8")
        self.assertIn("SELECT sura, ayah, text FROM", source)
        self.assertIn("WHERE sura = ? ORDER BY ayah ASC", source)
        rows = self.connection.execute(
            "SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC", (2,)
        ).fetchall()
        self.assertEqual(len(rows), 286)
        self.assertEqual([row[1] for row in rows], list(range(1, 287)))
        self.assertTrue(all(row[0] == 2 for row in rows))

    def test_query_uses_bound_parameters(self):
        """A bound parameter cannot be talked into returning another surah."""
        rows = self.connection.execute(
            "SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC",
            ("2 OR 1=1",)).fetchall()
        self.assertEqual(rows, [])

    def test_basmallah_semantics(self):
        """Surah 1 carries the basmallah as ayah 1; no other surah carries it as ayah 1."""
        basmallah = self.connection.execute(
            "SELECT text FROM arabic_text WHERE sura=1 AND ayah=1").fetchone()[0].strip()
        self.assertEqual(skeleton(basmallah).split(), BASMALLAH_SKELETON,
                         "surah 1 ayah 1 is not the basmallah")
        self.assertEqual(len(basmallah.split()), 4)
        for surah in range(2, TOTAL_SURAHS + 1):
            first = self.connection.execute(
                "SELECT text FROM arabic_text WHERE sura=? AND ayah=1", (surah,)).fetchone()[0].strip()
            self.assertNotEqual(first, basmallah,
                                f"surah {surah} ayah 1 must not duplicate the basmallah")

    def test_provenance_is_recorded(self):
        properties = dict(self.connection.execute("SELECT property, value FROM properties").fetchall())
        self.assertIn("Tanzil", properties.get("source", ""))
        self.assertIn("Creative Commons", properties.get("license", ""))
        self.assertEqual(properties.get("text_version"), "uthmani-1.1")

    def test_text_is_verbatim_uthmani(self):
        blob = "".join(all_ayah_texts())
        for codepoint, label in UTHMANI_MARKS:
            self.assertIn(codepoint, blob, f"missing {label} (U+{ord(codepoint):04X})")
        # Ayah markers are added at display time, never stored in the text column.
        self.assertNotIn("\uFD3F", blob)
        self.assertNotIn("\uFD3E", blob)


@unittest.skipUnless(FONT.exists() and DATABASE.exists(), "font or database is not bundled")
class BundledFontTests(unittest.TestCase):
    def test_font_file_header(self):
        self.assertIn(FONT.read_bytes()[:4],
                      [b"\x00\x01\x00\x00", b"OTTO", b"ttcf", b"true"],
                      "quran_font.ttf is not a TrueType/OpenType font")

    @unittest.skipUnless(has_fonttools(), "fontTools is not installed")
    def test_font_covers_every_codepoint_in_the_database(self):
        """A font missing a mark renders tofu; this is the check that catches a swapped font."""
        from fontTools.ttLib import TTFont
        covered = set()
        for table in TTFont(str(FONT))["cmap"].tables:
            covered |= set(table.cmap.keys())
        needed = set()
        for text in all_ayah_texts():
            needed |= {ord(char) for char in text}
        missing = sorted(needed - covered)
        self.assertEqual(missing, [],
                         "no glyph for " + ", ".join(f"U+{c:04X}" for c in missing))

    @unittest.skipUnless(has_fonttools(), "fontTools is not installed")
    def test_font_covers_the_arabic_chrome_it_is_applied_to(self):
        """QuranActivity applies the Quran face to the header labels, so they must be covered too.

        Covers all 114 surah names, the Arabic header/footer templates from quran.xml, the
        Arabic-Indic digits, both ornate parentheses and the non-breaking space that pads them.
        """
        from fontTools.ttLib import TTFont
        covered = set()
        for table in TTFont(str(FONT))["cmap"].tables:
            covered |= set(table.cmap.keys())

        needed = {ord(char) for char in "\u00A0\u2013 \uFD3E\uFD3F"}
        needed |= {ord(c) for c in "٠١٢٣٤٥٦٧٨٩"}
        for name in java_string_array("ARABIC_NAMES"):
            needed |= {ord(char) for char in name}

        quran_strings = ET.parse(MAIN / "res/values/quran.xml").getroot()
        for node in quran_strings.findall("string"):
            value = "".join(node.itertext())
            if any("\u0600" <= char <= "\u06FF" for char in value):
                needed |= {ord(char) for char in value if char != "%"}

        missing = sorted(needed - covered)
        self.assertEqual(missing, [],
                         "the header would render tofu for "
                         + ", ".join(f"U+{c:04X}" for c in missing))


class LayoutTests(unittest.TestCase):
    @staticmethod
    def parse(relative):
        return ET.parse(MAIN / relative)

    def test_required_colours_are_defined_in_colors_xml(self):
        colors = {node.get("name"): node.text.strip()
                  for node in self.parse("res/values/colors.xml").findall("color")}
        for name, value in REQUIRED_COLOURS.items():
            self.assertEqual(colors.get(name), value, f"{name} must be {value} in colors.xml")

    @staticmethod
    def find_by_id(root, resource_id):
        return root.find(f".//*[@{ANDROID}id='{resource_id}']")

    def test_reader_layout_structure(self):
        root = self.parse("res/layout/activity_quran.xml").getroot()
        self.assertEqual(root.get(ANDROID + "id"), "@+id/quran_root")
        self.assertEqual(root.get(ANDROID + "background"), "@color/quran_background")

        # Swipe navigation replaced the Next/Previous buttons: the body is a ViewPager2, one
        # QuranFragment per surah, and the old buttons must stay gone.
        pager = root.find(".//androidx.viewpager2.widget.ViewPager2")
        self.assertIsNotNone(pager, "activity_quran.xml must host a ViewPager2")
        self.assertEqual(pager.get(ANDROID + "id"), "@+id/quran_pager")
        self.assertEqual(pager.get(ANDROID + "layout_height"), "0dp")
        self.assertEqual(pager.get(ANDROID + "layout_weight"), "1")
        self.assertEqual(pager.get(ANDROID + "layoutDirection"), "ltr",
                         "a left swipe must always mean 'next surah'")
        for removed in ("@+id/quran_prev_btn", "@+id/quran_next_btn"):
            self.assertIsNone(self.find_by_id(root, removed),
                              f"{removed} must be gone: swiping replaces the buttons")

        # Toolbar controls: font size, bookmark (last read position) and the theme toggle.
        for required in ("@+id/quran_back", "@+id/quran_position", "@+id/quran_font_decrease",
                         "@+id/quran_font_increase", "@+id/quran_bookmark",
                         "@+id/quran_theme_toggle", "@+id/quran_surah_name", "@+id/quran_juz",
                         "@+id/quran_page"):
            self.assertIsNotNone(self.find_by_id(root, required),
                                 f"{required} missing from activity_quran.xml")

        # Rows that pin a direction must pin it to ltr, so left/right stay physical.
        for row in root.findall("LinearLayout"):
            direction = row.get(ANDROID + "layoutDirection")
            if direction is not None:
                self.assertEqual(direction, "ltr", "header/footer rows must be LTR-anchored")

        header = self.header_row(root)
        self.assertEqual([view.get(ANDROID + "id") for view in header][:2],
                         ["@+id/quran_surah_name", "@+id/quran_juz"],
                         "Surah Name must be the left header view and Juz the right one")
        for view in header:
            self.assertEqual(view.get(ANDROID + "textColor"), "@color/quran_header",
                             "header labels use the #7A7A7A header colour")
        self.assertIsNotNone(self.find_by_id(root, "@+id/quran_page"))

    def test_reader_page_fragment_layout(self):
        root = self.parse("res/layout/fragment_quran_page.xml").getroot()
        self.assertEqual(root.get(ANDROID + "id"), "@+id/quran_page_root")
        self.assertEqual(root.get(ANDROID + "background"), "@color/quran_background")

        for required in ("@+id/quran_page_scroll", "@+id/quran_page_text",
                         "@+id/quran_page_progress", "@+id/quran_page_status",
                         "@+id/quran_page_retry"):
            self.assertIsNotNone(self.find_by_id(root, required),
                                 f"{required} missing from fragment_quran_page.xml")

        scroll = root.find(".//ScrollView")
        self.assertIsNotNone(scroll)
        self.assertEqual(len(scroll), 1)
        body = scroll[0]
        self.assertEqual(body.tag, "TextView", "the body must be one TextView inside a ScrollView")
        self.assertEqual(body.get(ANDROID + "textDirection"), "rtl")
        self.assertEqual(body.get(ANDROID + "gravity"), "center")
        self.assertEqual(body.get(ANDROID + "textColor"), "@color/quran_text")

        retry = self.find_by_id(root, "@+id/quran_page_retry")
        self.assertEqual(retry.tag, "Button")
        self.assertEqual(retry.get(ANDROID + "background"), "@drawable/bg_quran_nav_button")
        self.assertEqual(retry.get(ANDROID + "textColor"), "@color/quran_nav_button_text")

    @staticmethod
    def header_row(root):
        for row in root.findall("LinearLayout"):
            ids = [child.get(ANDROID + "id") for child in row]
            if "@+id/quran_surah_name" in ids and "@+id/quran_juz" in ids:
                return [child for child in row if child.get(ANDROID + "id")]
        raise AssertionError("no header row containing both quran_surah_name and quran_juz")

    def test_index_layout_hosts_a_recyclerview(self):
        root = self.parse("res/layout/activity_surah_list.xml").getroot()
        self.assertEqual(root.get(ANDROID + "id"), "@+id/surah_list_root")
        self.assertEqual(root.get(ANDROID + "background"), "@color/quran_background")
        recycler = root.find(".//androidx.recyclerview.widget.RecyclerView")
        self.assertIsNotNone(recycler, "activity_surah_list.xml must host a RecyclerView")
        self.assertEqual(recycler.get(ANDROID + "id"), "@+id/surah_recycler")
        # The list container (a FrameLayout hosting the list plus the floating shortcut) carries
        # weight=1 with height=0dp, so the list region, not the header, absorbs remaining space.
        container = root.find(".//FrameLayout")
        self.assertIsNotNone(container, "the list must sit in a FrameLayout overlay container")
        self.assertEqual(container.get(ANDROID + "layout_height"), "0dp")
        self.assertEqual(container.get(ANDROID + "layout_weight"), "1")
        self.assertEqual(recycler.get(TOOLS + "listitem"), "@layout/item_surah")
        self.assertIsNotNone(LayoutTests.find_by_id(root, "@+id/surah_list_back"))

        # Search: a SearchView filters surah names while typing and searches the text on submit.
        search = root.find(".//androidx.appcompat.widget.SearchView")
        self.assertIsNotNone(search, "activity_surah_list.xml must host a SearchView")
        self.assertEqual(search.get(ANDROID + "id"), "@+id/surah_list_search")

        # Continue Reading: the bookmark shortcut floats over the list, hidden until a bookmark
        # exists (the activity decides visibility at runtime).
        continue_reading = LayoutTests.find_by_id(root, "@+id/surah_list_continue_btn")
        self.assertIsNotNone(continue_reading,
                             "the Continue Reading bookmark shortcut is missing")
        self.assertEqual(continue_reading.tag, "Button")
        self.assertEqual(continue_reading.get(ANDROID + "text"), "@string/quran_continue_reading")
        self.assertEqual(continue_reading.get(ANDROID + "visibility"), "gone")

        # Theme toggle shares the reader's moon/sun affordance.
        self.assertIsNotNone(LayoutTests.find_by_id(root, "@+id/surah_list_theme_toggle"))
        self.assertIsNotNone(LayoutTests.find_by_id(root, "@+id/surah_list_empty"))

    def test_search_result_row_layout(self):
        root = self.parse("res/layout/item_search_result.xml").getroot()
        for required in ("@+id/search_result_ref", "@+id/search_result_text"):
            self.assertIsNotNone(LayoutTests.find_by_id(root, required),
                                 f"{required} missing from item_search_result.xml")
        self.assertEqual(root.get(ANDROID + "clickable"), "true")
        self.assertEqual(root.get(ANDROID + "focusable"), "true")
        excerpt = LayoutTests.find_by_id(root, "@+id/search_result_text")
        self.assertEqual(excerpt.get(ANDROID + "textDirection"), "rtl")
        self.assertEqual(excerpt.get(ANDROID + "textColor"), "@color/quran_text")

    def test_row_layout_exposes_every_bound_view(self):
        root = self.parse("res/layout/item_surah.xml").getroot()
        for required in ("@+id/surah_number", "@+id/surah_english_name",
                         "@+id/surah_english_meaning", "@+id/surah_meta",
                         "@+id/surah_arabic_name"):
            self.assertIsNotNone(LayoutTests.find_by_id(root, required),
                                 f"{required} missing from item_surah.xml")
        self.assertEqual(root.get(ANDROID + "clickable"), "true")
        self.assertEqual(root.get(ANDROID + "focusable"), "true")
        arabic = LayoutTests.find_by_id(root, "@+id/surah_arabic_name")
        self.assertEqual(arabic.get(ANDROID + "textDirection"), "rtl")

    def test_navigation_buttons_use_the_button_colour(self):
        text = (MAIN / "res/drawable/bg_quran_nav_button.xml").read_text(encoding="utf-8")
        self.assertIn("@color/quran_button", text)
        self.assertIn("@color/quran_button_pressed", text)
        self.assertIn('android:state_enabled="false"', text,
                      "the ends of the Mushaf need a visible disabled state")

    def test_manifest_registration(self):
        manifest = self.parse("AndroidManifest.xml")
        for name in ("SurahListActivity", "QuranActivity"):
            activity = manifest.find(
                f".//activity[@{ANDROID}name='com.clock.livewallpaper.activity.{name}']")
            self.assertIsNotNone(activity, f"{name} is not registered")
            self.assertEqual(activity.get(ANDROID + "exported"), "false")
            self.assertEqual(activity.get(ANDROID + "theme"), "@style/QuranTheme")

    def test_theme_uses_the_paper_background(self):
        text = (MAIN / "res/values/quran.xml").read_text(encoding="utf-8")
        self.assertIn('<item name="android:windowBackground">@color/quran_background</item>', text)


class OfflineGuaranteeTests(unittest.TestCase):
    OFFLINE_SOURCES = (
        "quran/QuranDatabaseHelper.java",
        "quran/SurahIndex.java",
        "quran/QuranMetadata.java",
        "quran/QuranSettings.java",
        "quran/QuranThemeColors.java",
        "activity/QuranActivity.java",
        "activity/QuranFragment.java",
        "activity/SurahListActivity.java",
        "adapter/SurahListAdapter.java",
        "adapter/AyahSearchAdapter.java",
    )

    @staticmethod
    def code_only(relative):
        source = (JAVA / relative).read_text(encoding="utf-8")
        source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
        return re.sub(r"//[^\n]*", "", source)

    def test_reader_code_has_no_network_path(self):
        for relative in self.OFFLINE_SOURCES:
            self.assertNotRegex(
                self.code_only(relative),
                r"https?://|HttpURLConnection|okhttp3|Socket\(|URLConnection",
                f"{relative} must not reach the network")

    def test_online_client_was_removed(self):
        self.assertFalse((JAVA / "quran/QuranApiClient.java").exists(),
                         "the network fallback must stay deleted for a pure-offline reader")

    def test_database_is_copied_from_assets_on_first_run(self):
        source = DB_HELPER.read_text(encoding="utf-8")
        self.assertIn('ASSET_PATH = "databases/" + DATABASE_NAME', source)
        self.assertIn('DATABASE_NAME = "quran.ar.uthmani.db"', source)
        self.assertIn("public void prepareDatabase()", source)
        self.assertIn("getDatabasePath(DATABASE_NAME)", source,
                      "must not hard-code a /data/data path")
        self.assertIn("renameTo", source, "copy into place only after validation succeeds")
        self.assertIn("public String getSurahText(int surahId)", source)
        self.assertIn("\\uFD3F", source, "the ayah marker must use the ornate parenthesis codepoint")
        self.assertIn("\\uFD3E", source)

    def test_application_prepares_the_database_off_the_main_thread(self):
        source = (JAVA / "AppClass.java").read_text(encoding="utf-8")
        self.assertIn("new QuranDatabaseHelper(this).prepareDatabase()", source)
        self.assertIn("installer.execute(", source)
        self.assertIn("installer.shutdown()", source)

    def test_activity_receives_the_intent_contract(self):
        source = (JAVA / "activity/QuranActivity.java").read_text(encoding="utf-8")
        self.assertIn('EXTRA_SURAH_ID = "surah_id"', source)
        self.assertIn('EXTRA_SURAH_NAME = "surah_name"', source)
        self.assertIn("getIntExtra(EXTRA_SURAH_ID", source)
        self.assertIn("getStringExtra(EXTRA_SURAH_NAME)", source)

    def test_swipe_navigation_uses_viewpager2(self):
        source = (JAVA / "activity/QuranActivity.java").read_text(encoding="utf-8")
        # Comments are stripped so the javadoc entry-contract example (which shows a caller using
        # startActivity) cannot be mistaken for live navigation code.
        code = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
        code = re.sub(r"//[^\n]*", "", code)
        self.assertIn("ViewPager2", source, "the reader must host a ViewPager2")
        self.assertIn("FragmentStateAdapter", source,
                      "each surah must be a fragment page in a FragmentStateAdapter")
        self.assertIn("extends FragmentStateAdapter", source)
        self.assertIn("SurahIndex.TOTAL_SURAHS", source,
                      "the adapter must expose all 114 surahs as pages")
        self.assertIn("createFragment", source)
        self.assertNotIn("startActivity", code,
                         "the reader must never launch another activity to change surahs")
        fragment = (JAVA / "activity/QuranFragment.java").read_text(encoding="utf-8")
        self.assertIn("extends Fragment", fragment,
                      "each page must be an AndroidX fragment")
        self.assertIn("newInstance", fragment)

    def test_font_is_loaded_from_assets(self):
        source = (JAVA / "activity/QuranFragment.java").read_text(encoding="utf-8")
        self.assertIn('"fonts/quran_font.ttf"', source)
        self.assertIn("Typeface.createFromAsset", source)
        self.assertIn("setTypeface", source)

    def test_bookmark_contract_uses_shared_preferences(self):
        source = (JAVA / "quran/QuranSettings.java").read_text(encoding="utf-8")
        self.assertIn("getSharedPreferences", source)
        self.assertIn('"last_read_surah_id"', source,
                      "the bookmark must persist the surah under last_read_surah_id")
        self.assertIn("scrollY", source.replace("scroll_y", "scrollY"),
                      "the bookmark must persist the scroll position")
        self.assertIn("saveBookmark", source)
        reader = (JAVA / "activity/QuranActivity.java").read_text(encoding="utf-8")
        self.assertIn("saveBookmark", reader, "the toolbar bookmark icon must save the position")
        index = (JAVA / "activity/SurahListActivity.java").read_text(encoding="utf-8")
        self.assertIn("getBookmarkSurah", index)
        self.assertIn("EXTRA_SCROLL_Y", reader,
                      "the reader must accept a one-shot scroll offset for Continue Reading")

    def test_search_contract_uses_sql_like(self):
        source = DB_HELPER.read_text(encoding="utf-8")
        self.assertIn("public List<Ayah> searchQuran(String query)", source)
        self.assertIn("LIKE ?", source, "search must use the SQL LIKE operator")
        self.assertIn("ESCAPE", source, "wildcards in user input must be escaped")
        self.assertIn("text LIKE", source.replace("text LIKE ?", "text LIKE"),
                      "LIKE must match against the text column")
        index = (JAVA / "activity/SurahListActivity.java").read_text(encoding="utf-8")
        self.assertIn("searchQuran", index, "the index screen must call the helper's search")
        self.assertIn("SearchView", index, "the index screen must host a SearchView")

    def test_font_size_and_theme_are_persisted(self):
        settings = (JAVA / "quran/QuranSettings.java").read_text(encoding="utf-8")
        self.assertIn("putFloat", settings, "the font size must persist as a float")
        self.assertIn("getFontSize", settings)
        self.assertIn("adjustFontSize", settings)
        self.assertIn("toggleDarkMode", settings)
        reader = (JAVA / "activity/QuranActivity.java").read_text(encoding="utf-8")
        self.assertIn("adjustFontSize", reader, "the +/- icons must change the persisted size")
        self.assertIn("toggleDarkMode", reader, "the moon/sun icon must flip the persisted theme")

    def test_dark_palette_matches_the_spec(self):
        source = (JAVA / "quran/QuranThemeColors.java").read_text(encoding="utf-8")
        for spec in ("0xFF121212", "0xFFE0E0E0", "0xFFA0A0A0",  # dark bg / text / header+footer
                     "0xFFFBF9F0", "0xFF1A1A1A"):                 # light Madani bg / text
            self.assertIn(spec, source, f"palette constant {spec} missing from QuranThemeColors")

    def test_home_screen_opens_the_index(self):
        source = (JAVA / "activity/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("SurahListActivity.class", source)
        self.assertNotIn("new Intent(MainActivity.this, QuranActivity.class)", source,
                         "the home tile must open the index, not a hard-coded surah")

    def test_licence_files_ship_with_the_binaries(self):
        for relative in ("docs/licenses/Tanzil-Uthmani-CC-BY-3.0.txt",
                         "docs/licenses/AmiriQuran-OFL-1.1.txt"):
            path = ROOT / relative
            self.assertTrue(path.exists(), f"{relative} is required next to the bundled assets")
            self.assertGreater(path.stat().st_size, 500)
        tanzil = (ROOT / "docs/licenses/Tanzil-Uthmani-CC-BY-3.0.txt").read_text(encoding="utf-8")
        self.assertIn("Creative Commons Attribution 3.0", tanzil)
        self.assertIn("tanzil.net", tanzil)

    def test_attribution_is_visible_in_the_app(self):
        strings = (MAIN / "res/values/quran.xml").read_text(encoding="utf-8")
        self.assertIn("quran_attribution", strings)
        layout = (MAIN / "res/layout/activity_surah_list.xml").read_text(encoding="utf-8")
        self.assertIn("@string/quran_attribution", layout,
                      "CC-BY-3.0 requires the source to be indicated in the application")


class BuildConfigurationTests(unittest.TestCase):
    def test_requested_toolchain(self):
        app_build = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        for declaration in ("compileSdk 36", "targetSdk 36", "minSdk 23",
                            "sourceCompatibility JavaVersion.VERSION_17",
                            "targetCompatibility JavaVersion.VERSION_17",
                            "languageVersion = JavaLanguageVersion.of(17)"):
            self.assertIn(declaration, app_build)
        self.assertIn("com.android.tools.build:gradle:8.13.2",
                      (ROOT / "build.gradle").read_text(encoding="utf-8"))
        self.assertIn("gradle-8.13-bin.zip",
                      (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8"))
        self.assertIn("android.nonFinalResIds=false",
                      (ROOT / "gradle.properties").read_text(encoding="utf-8"))

    def test_recyclerview_dependency_is_declared(self):
        self.assertIn("androidx.recyclerview:recyclerview",
                      (ROOT / "app/build.gradle").read_text(encoding="utf-8"))

    def test_viewpager2_dependency_is_declared(self):
        self.assertIn("androidx.viewpager2:viewpager2",
                      (ROOT / "app/build.gradle").read_text(encoding="utf-8"),
                      "swipe navigation needs the ViewPager2 artifact")


@unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK not installed")
class CompiledBehaviourTests(unittest.TestCase):
    """Compile and execute the pure-Java classes; skipped when no JDK is available."""

    def test_metadata_and_index_agree_at_runtime(self):
        program = '''
import com.clock.livewallpaper.quran.QuranMetadata;
import com.clock.livewallpaper.quran.SurahIndex;

public class MetadataCheck {
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        equal(42, QuranMetadata.pageFor(2, 255));
        equal(604, QuranMetadata.pageFor(114, 6));
        equal(6, QuranMetadata.juzFor(5, 81));
        equal(7, QuranMetadata.juzFor(5, 82));
        equal(10, QuranMetadata.juzFor(9, 92));
        equal(11, QuranMetadata.juzFor(9, 93));
        equal("\\u0662\\u0665\\u0665", QuranMetadata.arabicNumber(255));

        equal(114, SurahIndex.all().size());
        equal(114, SurahIndex.TOTAL_SURAHS);
        equal("Al-Fatihah", SurahIndex.get(1).englishName);
        equal("Al-Baqarah", SurahIndex.get(2).englishName);
        equal("An-Nas", SurahIndex.get(114).englishName);
        equal(7, SurahIndex.get(1).ayahCount);
        equal(286, SurahIndex.get(2).ayahCount);
        equal(6, SurahIndex.get(114).ayahCount);

        check(!SurahIndex.get(1).hasBasmallahHeader(), "surah 1 carries the basmallah as ayah 1");
        check(SurahIndex.get(2).hasBasmallahHeader(), "surah 2 needs a basmallah header");
        check(!SurahIndex.get(9).hasBasmallahHeader(), "surah 9 has no basmallah");

        equal(1, SurahIndex.get(2).firstJuz);
        equal(3, SurahIndex.get(2).lastJuz);
        check(SurahIndex.get(2).spansMultipleJuz(), "Al-Baqarah spans juz 1-3");
        check(SurahIndex.get(2).spansMultiplePages(), "Al-Baqarah spans many pages");
        check(!SurahIndex.get(114).spansMultiplePages(), "An-Nas fits on page 604");
        equal(604, SurahIndex.get(114).firstPage);

        int total = 0;
        int previousPage = 0;
        int previousJuz = 0;
        for (SurahIndex.Surah surah : SurahIndex.all()) {
            equal(QuranMetadata.ayahCount(surah.id), surah.ayahCount);
            check(!surah.arabicName.isEmpty(), "blank Arabic name at " + surah.id);
            check(!surah.englishName.isEmpty(), "blank English name at " + surah.id);
            check(!surah.englishMeaning.isEmpty(), "blank meaning at " + surah.id);
            check(surah.firstPage >= previousPage, "page went backwards at " + surah.id);
            check(surah.firstJuz >= previousJuz, "juz went backwards at " + surah.id);
            check(surah.firstPage <= surah.lastPage, "page range inverted at " + surah.id);
            check(surah.firstJuz <= surah.lastJuz, "juz range inverted at " + surah.id);
            previousPage = surah.firstPage;
            previousJuz = surah.firstJuz;
            total += surah.ayahCount;
        }
        equal(6236, total);
        check(previousPage <= 604, "surah start pages exceeded the Mushaf");

        for (int invalid : new int[]{0, 115, -1}) {
            check(!SurahIndex.isValid(invalid), "isValid accepted " + invalid);
            try {
                SurahIndex.get(invalid);
                throw new AssertionError("get() accepted " + invalid);
            } catch (IllegalArgumentException expected) {
                // correct
            }
        }
        equal("", SurahIndex.arabicName(0));
        equal("", SurahIndex.englishName(999));
    }
}
'''
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "MetadataCheck.java"
            source.write_text(program, encoding="utf-8")
            subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-d", temporary,
                            str(METADATA), str(SURAH_INDEX), str(source)], check=True)
            subprocess.run(["java", "-cp", temporary, "MetadataCheck"], check=True)


if __name__ == "__main__":
    unittest.main()
