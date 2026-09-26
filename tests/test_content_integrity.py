"""Guards the religious content.

The bundled adhkar are verified text with recorded attributions. Nothing in this project may
rewrite, normalise, reflow or "improve" them, and no verified asset may quietly disappear. These
tests pin the content file by hash and check that the seeding path copies it verbatim.

Run with::

    python3 -m unittest discover -s tests -v
"""
import hashlib
import json
import os
import re
import subprocess
import unittest

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(REPO, "app", "src", "main")
ASSETS = os.path.join(MAIN, "assets")
RES = os.path.join(MAIN, "res")
SRC = os.path.join(MAIN, "java", "com", "clock", "livewallpaper")
AZKAR = os.path.join(ASSETS, "azkar.json")

# Hash of the verified content as it was inherited from the repository. It is a tripwire: if a
# future change edits a single Arabic character of the bundled adhkar, this test fails.
AZKAR_SHA256 = "978c4aa06f3a5df8778418b222f14cc21e2e7e3c434e726d268ed4bb91d3063f"

EXPECTED_COUNTS = {"morning": 31, "evening": 30, "tasbeeh": 17}


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


class AzkarAssetTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        with open(AZKAR, "rb") as handle:
            cls.raw = handle.read()
        cls.data = json.loads(cls.raw.decode("utf-8"))

    def test_file_is_byte_identical_to_the_verified_copy(self):
        digest = hashlib.sha256(self.raw).hexdigest()
        self.assertEqual(AZKAR_SHA256, digest, "the bundled adhkar must never be edited")

    def test_matches_the_committed_copy(self):
        """Second, independent check: the working copy equals the inherited git blob."""
        try:
            committed = subprocess.run(
                ["git", "show", "HEAD:app/src/main/assets/azkar.json"],
                cwd=REPO, capture_output=True, check=True
            ).stdout
        except (OSError, subprocess.CalledProcessError):
            self.skipTest("git history is not available here")
        self.assertEqual(committed, self.raw)

    def test_section_counts(self):
        for section, count in EXPECTED_COUNTS.items():
            self.assertEqual(count, len(self.data[section]), section)

    def test_every_item_is_complete(self):
        for section in EXPECTED_COUNTS:
            for index, item in enumerate(self.data[section]):
                where = f"{section}[{index}]"
                self.assertTrue(item["text"].strip(), where)
                self.assertGreaterEqual(item["repeat"], 1, where)
                self.assertIsInstance(item["order"], int, where)
                self.assertIsInstance(item["id"], int, where)

    def test_ids_and_order_are_unique(self):
        for section in EXPECTED_COUNTS:
            ids = [item["id"] for item in self.data[section]]
            orders = [item["order"] for item in self.data[section]]
            self.assertEqual(len(ids), len(set(ids)), section)
            self.assertEqual(len(orders), len(set(orders)), section)

    def test_every_section_carries_its_attribution(self):
        sources = self.data["sources"]
        for section in EXPECTED_COUNTS:
            self.assertIn(section, sources)
            self.assertTrue(sources[section].strip(), section)

    def test_text_is_arabic(self):
        arabic = re.compile(r"[\u0600-\u06FF]")
        for section in EXPECTED_COUNTS:
            for item in self.data[section]:
                self.assertTrue(arabic.search(item["text"]))


class SeedingTest(unittest.TestCase):
    """The seed path is the only code that touches verified text: it must copy, never edit."""

    def setUp(self):
        self.source = read(os.path.join(SRC, "data", "seed", "AzkarSeedSource.kt"))
        body = self.source[self.source.index("fun load()"):self.source.index("private fun sourceLabel")]
        self.body = body

    def test_text_is_copied_verbatim(self):
        self.assertIn("val text = item.optString(\"text\")", self.body)
        self.assertIn("arabicText = text,", self.body)

    def test_no_text_transformation(self):
        for forbidden in (
            "text.trim()",
            "text.replace",
            "text.normalize",
            "Normalizer",
            "text.lowercase",
            "text.uppercase",
            "text.substring",
            "text.take(",
            "text.filter",
        ):
            self.assertNotIn(forbidden, self.body, f"seeding must not transform text: {forbidden}")

    def test_empty_items_are_skipped_not_invented(self):
        self.assertIn("if (text.isBlank()) continue", self.body)

    def test_virtue_and_repeat_travel_with_the_text(self):
        self.assertIn("virtue = virtue,", self.body)
        self.assertIn("repeatCount = item.optInt(\"repeat\", 1)", self.body)
        self.assertIn("sourceReference = reference,", self.body)

    def test_reseeding_is_idempotent(self):
        """A second seed run must not duplicate rows: the seed key is the identity."""
        dao = read(os.path.join(SRC, "data", "local", "DhikrDao.kt"))
        self.assertIn("OnConflictStrategy.IGNORE", dao)
        entity = read(os.path.join(SRC, "data", "local", "DhikrEntity.kt"))
        self.assertIn('Index(value = ["seedKey"], unique = true)', entity)

    def test_overlay_limit_is_a_display_choice_only(self):
        self.assertIn("includeInOverlay = text.length <= OVERLAY_TEXT_LIMIT", self.body)


class PreservedAssetTest(unittest.TestCase):
    """Verified assets inherited from the project stay in the tree, even when UI was retired."""

    def test_quran_database_is_kept(self):
        path = os.path.join(ASSETS, "databases", "quran.ar.uthmani.db")
        self.assertTrue(os.path.exists(path))
        self.assertGreater(os.path.getsize(path), 1_000_000)

    def test_wallpaper_library_is_kept(self):
        index = os.path.join(ASSETS, "wallpapers", "index.json")
        self.assertTrue(os.path.exists(index))
        json.loads(read(index))

    def test_fonts_and_their_licences_are_kept_together(self):
        for font in ("cairo_regular.ttf", "tajawal_regular.ttf", "amiri_quran.ttf"):
            path = os.path.join(RES, "font", font)
            self.assertTrue(os.path.exists(path), font)
            self.assertGreater(os.path.getsize(path), 10_000, font)
        licences = os.listdir(os.path.join(REPO, "docs", "licenses"))
        self.assertTrue(licences, "font licences must ship with the fonts")


if __name__ == "__main__":
    unittest.main()
