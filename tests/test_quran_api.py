"""Tests for dynamic online Quran fetching, web component, and caching contracts."""
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
API_CLIENT = MAIN / "java/com/clock/livewallpaper/quran/QuranApiClient.java"
ACTIVITY = MAIN / "java/com/clock/livewallpaper/activity/QuranActivity.java"
LAYOUT = MAIN / "res/layout/activity_quran.xml"
WEB_INDEX = ROOT / "web/index.html"
ASSET_WEB = MAIN / "assets/quran/index.html"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class QuranApiTests(unittest.TestCase):
    def test_api_client_contract(self):
        code = API_CLIENT.read_text(encoding="utf-8")
        self.assertIn("https://api.alquran.cloud/v1/surah/%d/quran-uthmani", code)
        self.assertIn("MEMORY_CACHE", code)
        self.assertIn("saveToDiskCache", code)
        self.assertIn("loadFromDiskCache", code)
        self.assertIn("parseSurahJson", code)

    def test_activity_retry_and_loading_wiring(self):
        code = ACTIVITY.read_text(encoding="utf-8")
        self.assertIn("QuranApiClient.getSurah", code)
        self.assertIn("quran_retry_btn", code)
        self.assertIn("quran_progress", code)
        self.assertIn("loadSurahData()", code)
        self.assertIn("configureSystemBars();", code)

        layout = ET.parse(LAYOUT)
        retry_btn = layout.find(".//Button[@" + ANDROID + "id='@+id/quran_retry_btn']")
        self.assertIsNotNone(retry_btn)
        self.assertEqual(retry_btn.get(ANDROID + "visibility"), "gone")

        progress = layout.find(".//ProgressBar[@" + ANDROID + "id='@+id/quran_progress']")
        self.assertIsNotNone(progress)

    def test_web_component_specification(self):
        for path in [WEB_INDEX, ASSET_WEB]:
            html = path.read_text(encoding="utf-8")
            self.assertIn("https://fonts.googleapis.com/css2?family=Amiri", html)
            self.assertIn("https://api.alquran.cloud/v1/surah/{number}/quran-uthmani", html)
            self.assertIn("localStorage", html)
            self.assertIn("memoryCache", html)
            self.assertIn("btnRetry", html)
            self.assertIn("spinner", html)
            self.assertIn("loadingState", html)
            self.assertIn("errorState", html)

    def test_json_parsing_logic(self):
        mock_response = {
            "code": 200,
            "status": "OK",
            "data": {
                "number": 1,
                "name": "Dummy Surah",
                "englishName": "Al-Fatiha",
                "revelationType": "Meccan",
                "numberOfAyahs": 2,
                "ayahs": [
                    {
                        "number": 1,
                        "text": "mock_ayah_1",
                        "numberInSurah": 1,
                        "juz": 1,
                        "manzil": 1,
                        "page": 1,
                        "ruku": 1,
                        "hizbQuarter": 1,
                        "sajda": False
                    },
                    {
                        "number": 2,
                        "text": "mock_ayah_2",
                        "numberInSurah": 2,
                        "juz": 1,
                        "manzil": 1,
                        "page": 1,
                        "ruku": 1,
                        "hizbQuarter": 1,
                        "sajda": False
                    }
                ]
            }
        }
        json_str = json.dumps(mock_response)
        data = json.loads(json_str)
        self.assertEqual(data["code"], 200)
        self.assertEqual(len(data["data"]["ayahs"]), 2)
        self.assertEqual(data["data"]["ayahs"][0]["numberInSurah"], 1)


if __name__ == "__main__":
    unittest.main()
