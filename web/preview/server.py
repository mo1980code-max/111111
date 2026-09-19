#!/usr/bin/env python3
"""Design preview server for the Android Quran screens.

This is a development aid, NOT part of the APK. Nothing under web/ is packaged into the app.

It exists because the Android app cannot be compiled in this workspace (no JDK, no Android SDK), so
the two new screens are mirrored here in HTML/CSS -- driven by the *same* data the app uses:

  * surah text comes from app/src/main/assets/databases/quran.ar.uthmani.db through the identical
    SQL and formatting rules implemented in QuranDatabaseHelper.java
  * the 114-row index comes from the arrays in SurahIndex.java, parsed at request time
  * the Arabic face is the real app/src/main/assets/fonts/quran_font.ttf

So what you see is what the Android layouts will render, and a data bug shows up here too.

Run:  python3 web/preview/server.py [--port 8000]
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
MAIN = ROOT / "app/src/main"
DATABASE = MAIN / "assets/databases/quran.ar.uthmani.db"
FONT = MAIN / "assets/fonts/quran_font.ttf"
SURAH_INDEX = MAIN / "java/com/clock/livewallpaper/quran/SurahIndex.java"
METADATA = MAIN / "java/com/clock/livewallpaper/quran/QuranMetadata.java"

# Mirrors QuranDatabaseHelper.java exactly.
QUERY_SURAH = "SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC"
QUERY_BASMALLAH = "SELECT text FROM arabic_text WHERE sura = ? AND ayah = ? LIMIT 1"
AYAH_MARK_OPEN = "\uFD3F"
AYAH_MARK_CLOSE = "\uFD3E"
NO_BREAK_SPACE = "\u00A0"
NO_BASMALLAH_HEADER = {1, 9}

ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"


def arabic_number(value: int) -> str:
    return "".join(ARABIC_DIGITS[int(c)] if c.isdigit() else c for c in str(value))


def ayah_marker(number: int) -> str:
    return f"{AYAH_MARK_OPEN}{NO_BREAK_SPACE}{arabic_number(number)}{NO_BREAK_SPACE}{AYAH_MARK_CLOSE}"


def java_string_array(name: str) -> list[str]:
    source = SURAH_INDEX.read_text(encoding="utf-8")
    body = re.search(r"String\[\] " + name + r" = \{(.*?)\n    \};", source, re.S).group(1)
    return re.findall(r'"((?:[^"\\]|\\.)*)"', body)


def java_boolean_array(name: str) -> list[bool]:
    source = SURAH_INDEX.read_text(encoding="utf-8")
    body = re.search(r"boolean\[\] " + name + r" = \{(.*?)\n    \};", source, re.S).group(1)
    return [token == "true" for token in re.findall(r"\b(true|false)\b", body)]


def java_int_array(name: str, path: Path) -> list[int]:
    source = path.read_text(encoding="utf-8")
    body = re.search(r"int\[\] " + name + r" = \{(.*?)\};", source, re.S).group(1)
    return [int(n) for n in re.findall(r"\d+", body)]


def section_for(starts: list[int], surah: int, ayah: int) -> int:
    """Port of QuranMetadata.sectionFor: upper bound of the last section starting at or before."""
    import bisect
    return bisect.bisect_right(starts, surah * 1000 + ayah)


def build_index() -> list[dict]:
    arabic = java_string_array("ARABIC_NAMES")
    english = java_string_array("ENGLISH_NAMES")
    meanings = java_string_array("ENGLISH_MEANINGS")
    medinan = java_boolean_array("MEDINAN_FLAGS")
    counts = java_int_array("AYAH_COUNTS", METADATA)
    pages = java_int_array("PAGE_STARTS", METADATA)
    juz = java_int_array("JUZ_STARTS", METADATA)

    rows = []
    for index in range(114):
        surah_id = index + 1
        count = counts[index]
        rows.append({
            "id": surah_id,
            "arabicName": arabic[index],
            "englishName": english[index],
            "englishMeaning": meanings[index],
            "medinan": medinan[index],
            "ayahCount": count,
            "firstJuz": section_for(juz, surah_id, 1),
            "lastJuz": section_for(juz, surah_id, count),
            "firstPage": section_for(pages, surah_id, 1),
            "lastPage": section_for(pages, surah_id, count),
            "hasBasmallahHeader": surah_id not in NO_BASMALLAH_HEADER,
        })
    return rows


def surah_payload(surah_id: int) -> dict:
    rows = build_index()
    if not 1 <= surah_id <= 114:
        raise KeyError(surah_id)
    meta = rows[surah_id - 1]

    connection = sqlite3.connect(f"file:{DATABASE}?mode=ro", uri=True)
    try:
        ayahs = connection.execute(QUERY_SURAH, (surah_id,)).fetchall()
        if len(ayahs) != meta["ayahCount"]:
            raise ValueError(f"surah {surah_id}: {len(ayahs)} ayahs, expected {meta['ayahCount']}")
        basmallah = None
        if meta["hasBasmallahHeader"]:
            found = connection.execute(QUERY_BASMALLAH, (1, 1)).fetchone()
            basmallah = found[0].strip() if found else None
    finally:
        connection.close()

    # Same assembly as QuranDatabaseHelper.getSurahText(int, boolean).
    body = " ".join(f"{text.strip()} {ayah_marker(number)}" for _, number, text in ayahs)
    text = f"{basmallah}\n\n{body}" if basmallah else body

    return {
        **meta,
        "text": text,
        "basmallah": basmallah,
        "arabicNumber": arabic_number(surah_id),
        "ayahs": [{"number": number, "text": text.strip(), "marker": ayah_marker(number)}
                  for _, number, text in ayahs],
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "QuranPreview/1.0"

    def log_message(self, fmt, *args):  # keep the console readable
        pass

    def send(self, payload: bytes, content_type: str, status: int = 200):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def send_json(self, obj, status: int = 200):
        self.send(json.dumps(obj, ensure_ascii=False).encode("utf-8"),
                  "application/json; charset=utf-8", status)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        route = parsed.path

        try:
            if route in ("/", "/index.html"):
                self.send((HERE / "index.html").read_bytes(), "text/html; charset=utf-8")
            elif route == "/api/index":
                self.send_json({"surahs": build_index(), "total": 114})
            elif route.startswith("/api/surah/"):
                self.send_json(surah_payload(int(route.rsplit("/", 1)[1])))
            elif route == "/quran_font.ttf":
                self.send(FONT.read_bytes(), "font/ttf")
            else:
                self.send_json({"error": "not found", "path": route}, 404)
        except (KeyError, ValueError):
            self.send_json({"error": "surah must be between 1 and 114"}, 400)
        except FileNotFoundError as error:
            self.send_json({"error": f"missing asset: {error.filename}. "
                                     "Run tools/build_quran_db.py first."}, 500)
        except Exception as error:  # surface server bugs in the UI instead of hanging
            self.send_json({"error": f"{type(error).__name__}: {error}"}, 500)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--host", default="0.0.0.0")
    args = parser.parse_args()

    for required in (DATABASE, FONT, SURAH_INDEX, HERE / "index.html"):
        if not required.exists():
            raise SystemExit(f"missing required file: {required}")

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"Quran design preview on http://{args.host}:{args.port}")
    print(f"  database : {DATABASE.relative_to(ROOT)}")
    print(f"  font     : {FONT.relative_to(ROOT)}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
