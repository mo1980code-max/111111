"""SDK-independent contract checks, not a substitute for Android device tests."""
import bisect
from pathlib import Path
import re
import shutil
import sqlite3
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
METADATA = MAIN / "java/com/clock/livewallpaper/quran/QuranMetadata.java"
DATABASE = MAIN / "assets/databases/quran.ar.uthmani.db"
FONT = MAIN / "assets/fonts/quran_font.ttf"
ANDROID = "{http://schemas.android.com/apk/res/android}"


def numbers(name):
    body = re.search(r"int\[\] " + name + r" = \{(.*?)\};",
                     METADATA.read_text(), re.S).group(1)
    return [int(n) for n in re.findall(r"\d+", body)]


class OfflineQuranTests(unittest.TestCase):
    def test_complete_metadata(self):
        counts = numbers("AYAH_COUNTS")
        self.assertEqual(len(counts), 114)
        self.assertEqual(sum(counts), 6236)
        for name, size in (("PAGE_STARTS", 604), ("JUZ_STARTS", 30)):
            starts = numbers(name)
            self.assertEqual(len(starts), size)
            self.assertEqual(starts, sorted(set(starts)))
            self.assertEqual(starts[0], 1001)
            for key in starts:
                surah, ayah = divmod(key, 1000)
                self.assertTrue(1 <= surah <= 114)
                self.assertTrue(1 <= ayah <= counts[surah - 1])
            for surah, count in enumerate(counts, 1):
                for ayah in range(1, count + 1):
                    self.assertTrue(1 <= bisect.bisect_right(starts, surah * 1000 + ayah) <= size)

    def test_known_boundaries(self):
        pages = numbers("PAGE_STARTS")
        juz = numbers("JUZ_STARTS")
        for key, expected in ((1001, 1), (2001, 2), (2255, 42),
                              (5001, 106), (112001, 604), (114006, 604)):
            self.assertEqual(bisect.bisect_right(pages, key), expected)
        for key, expected in ((2141, 1), (2142, 2), (5081, 6), (5082, 7),
                              (9092, 10), (9093, 11), (114006, 30)):
            self.assertEqual(bisect.bisect_right(juz, key), expected)
        # Juz boundaries within a printed page must not be approximated from page number.
        self.assertEqual(bisect.bisect_right(pages, 5081), bisect.bisect_right(pages, 5082))
        self.assertEqual(bisect.bisect_right(pages, 9092), bisect.bisect_right(pages, 9093))

    def test_resources_and_manifest(self):
        for file in [MAIN / "AndroidManifest.xml", MAIN / "res/layout/activity_quran.xml",
                     MAIN / "res/values/quran.xml", MAIN / "res/values/quran_surah_names.xml"]:
            ET.parse(file)
        names = ET.parse(MAIN / "res/values/quran_surah_names.xml").findall(".//item")
        self.assertEqual(len(names), 114)
        self.assertTrue(all(item.text for item in names))
        layout = ET.parse(MAIN / "res/layout/activity_quran.xml")
        scroll = layout.find(".//ScrollView")
        self.assertEqual(len(scroll), 1)
        self.assertEqual(scroll[0].tag, "TextView")
        self.assertEqual(scroll[0].get(ANDROID + "textDirection"), "rtl")
        colors = {node.get("name"): node.text for node in
                  ET.parse(MAIN / "res/values/quran.xml").findall("color")}
        self.assertEqual(colors["quran_paper"], "#FBF9F0")
        self.assertEqual(colors["quran_muted"], "#7A7A7A")
        self.assertEqual(colors["quran_selected"], "#D0ECE4")
        self.assertEqual(colors["quran_ink"], "#1A1A1A")
        activity = ET.parse(MAIN / "AndroidManifest.xml").find(
            ".//activity[@" + ANDROID + "name='com.clock.livewallpaper.activity.QuranActivity']")
        self.assertIsNotNone(activity)
        self.assertEqual(activity.get(ANDROID + "exported"), "false")
        self.assertEqual(activity.get(ANDROID + "theme"), "@style/QuranTheme")

    def test_requested_build_configuration(self):
        app_build = (ROOT / "app/build.gradle").read_text()
        for declaration in ("compileSdk 36", "targetSdk 36", "minSdk 23",
                            "sourceCompatibility JavaVersion.VERSION_17",
                            "targetCompatibility JavaVersion.VERSION_17",
                            "languageVersion = JavaLanguageVersion.of(17)"):
            self.assertIn(declaration, app_build)
        self.assertIn("com.android.tools.build:gradle:8.13.2", (ROOT / "build.gradle").read_text())
        self.assertIn("gradle-8.13-bin.zip",
                      (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text())
        self.assertIn("android.nonFinalResIds=false", (ROOT / "gradle.properties").read_text())

    def test_startup_install_and_native_insets_wiring(self):
        helper = (MAIN / "java/com/clock/livewallpaper/quran/QuranDatabaseHelper.java").read_text()
        app = (MAIN / "java/com/clock/livewallpaper/AppClass.java").read_text()
        activity = (MAIN / "java/com/clock/livewallpaper/activity/QuranActivity.java").read_text()
        self.assertIn("public void prepareDatabase()", helper)
        self.assertIn("new QuranDatabaseHelper(this).prepareDatabase()", app)
        self.assertIn("installer.execute(", app)
        self.assertIn("installer.shutdown()", app)
        self.assertIn("configureSystemBars();", activity)
        self.assertIn("WindowInsets.Type.systemBars()", activity)
        self.assertIn("WindowInsets.Type.displayCutout()", activity)
        self.assertIn("WindowInsets.CONSUMED", activity)
        layout = ET.parse(MAIN / "res/layout/activity_quran.xml").getroot()
        self.assertEqual(layout.get(ANDROID + "id"), "@+id/quran_root")

    def test_sqlite_query_contract(self):
        # Fixture labels are intentionally NOT Quran text and are never packaged in assets.
        with sqlite3.connect(":memory:") as database:
            database.execute("CREATE TABLE arabic_text (sura INTEGER, ayah INTEGER, text TEXT)")
            database.executemany("INSERT INTO arabic_text VALUES (?, ?, ?)",
                                 [(2, 10, "fixture ten"), (1, 1, "other surah"),
                                  (2, 2, "fixture two"), (2, 1, "fixture one")])
            sql = "SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC"
            result = database.execute(sql, (2,)).fetchall()
            self.assertEqual([row[1] for row in result], [1, 2, 10])
            self.assertTrue(all(row[0] == 2 for row in result))
            self.assertEqual(database.execute(sql, ("2 OR 1=1",)).fetchall(), [])

    @unittest.skipUnless(DATABASE.exists(), "real Quran database not supplied")
    def test_bundled_database(self):
        with sqlite3.connect(DATABASE.as_uri() + "?mode=ro", uri=True) as database:
            self.assertEqual(database.execute("PRAGMA quick_check").fetchone()[0], "ok")
            rows = database.execute(
                "SELECT sura, ayah, text FROM arabic_text ORDER BY sura, ayah").fetchall()
            expected = [(surah, ayah) for surah, count in enumerate(numbers("AYAH_COUNTS"), 1)
                        for ayah in range(1, count + 1)]
            self.assertEqual([(s, a) for s, a, _ in rows], expected)
            self.assertTrue(all(text and text.strip() for _, _, text in rows))

    @unittest.skipUnless(FONT.exists(), "real Quran font not supplied")
    def test_bundled_font_header(self):
        # Only a file-format sanity check; glyph coverage needs device verification.
        self.assertIn(FONT.read_bytes()[:4], [b"\x00\x01\x00\x00", b"OTTO", b"ttcf", b"true"])

    @unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK not installed")
    def test_actual_java_metadata(self):
        java_test = '''
import com.clock.livewallpaper.quran.QuranMetadata;
public class MetadataCheck {
    private static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        equal(42, QuranMetadata.pageFor(2, 255));
        equal(604, QuranMetadata.pageFor(114, 6));
        equal(6, QuranMetadata.juzFor(5, 81));
        equal(7, QuranMetadata.juzFor(5, 82));
        equal(10, QuranMetadata.juzFor(9, 92));
        equal(11, QuranMetadata.juzFor(9, 93));
        if (!"٢٥٥".equals(QuranMetadata.arabicNumber(255))) throw new AssertionError();
        int previousPage = 0, previousJuz = 0, total = 0;
        for (int surah = 1; surah <= 114; surah++) {
            for (int ayah = 1; ayah <= QuranMetadata.ayahCount(surah); ayah++) {
                int page = QuranMetadata.pageFor(surah, ayah);
                int juz = QuranMetadata.juzFor(surah, ayah);
                if (page < previousPage || page < 1 || page > 604
                        || juz < previousJuz || juz < 1 || juz > 30) throw new AssertionError();
                previousPage = page;
                previousJuz = juz;
                total++;
            }
        }
        equal(6236, total);
        for (int[] invalid : new int[][]{{0, 1}, {115, 1}, {1, 0}, {1, 8}}) {
            try {
                QuranMetadata.pageFor(invalid[0], invalid[1]);
                throw new AssertionError("Invalid input accepted");
            } catch (IllegalArgumentException expected) { }
        }
    }
}
'''
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "MetadataCheck.java"
            source.write_text(java_test)
            subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-d", temporary,
                            str(METADATA), str(source)], check=True)
            subprocess.run(["java", "-cp", temporary, "MetadataCheck"], check=True)


if __name__ == "__main__":
    unittest.main()
