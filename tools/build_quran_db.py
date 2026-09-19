#!/usr/bin/env python3
"""Build app/src/main/assets/databases/quran.ar.uthmani.db from the Tanzil Uthmani text.

The Android app is fully offline: it never downloads Quran data at runtime. This script
is the one-time, reproducible generator for the binary asset that ships inside the APK.

Source text
    Tanzil Quran Text (Uthmani, Version 1.1)
    Copyright (C) 2007-2026 Tanzil Project -- License: Creative Commons Attribution 3.0
    http://tanzil.net

Resulting schema (the contract QuranDatabaseHelper.java queries against):
    CREATE TABLE arabic_text (sura INTEGER, ayah INTEGER, text TEXT);
    CREATE TABLE properties  (property TEXT, value TEXT);

Usage
    # Fresh download (needs network):
    python3 tools/build_quran_db.py

    # Offline rebuild from a local copy of the Tanzil XML:
    python3 tools/build_quran_db.py --xml /path/to/quran-uthmani.xml

The script refuses to emit a database unless it verifies all 114 surahs, all 6236
ayahs, the per-surah ayah counts, and a clean "PRAGMA quick_check".
"""

from __future__ import annotations

import argparse
import io
import sqlite3
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app/src/main/assets/databases/quran.ar.uthmani.db"

SOURCE_URL = "https://raw.githubusercontent.com/WWGTX/Quran/main/quran-uthmani.xml"
SOURCE_NAME = "Tanzil Quran Text (Uthmani, Version 1.1)"
SOURCE_LICENSE = "Creative Commons Attribution 3.0 (Tanzil Project, http://tanzil.net)"
SCHEMA_VERSION = "1"

# Per-surah ayah counts for the Hafs numbering, used as an independent cross-check
# against the downloaded text so a truncated or wrong-script download cannot ship.
EXPECTED_AYAH_COUNTS = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
    89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
    12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
    30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
]


def fetch_xml(explicit_path: Path | None) -> bytes:
    """Return the Tanzil Uthmani XML bytes, from a local file or the network."""
    if explicit_path is not None:
        data = explicit_path.read_bytes()
        print(f"read local source: {explicit_path} ({len(data):,} bytes)")
        return data

    print(f"downloading {SOURCE_URL}")
    request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "quran-db-builder/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    print(f"downloaded {len(data):,} bytes")
    return data


def parse_rows(xml_bytes: bytes) -> list[tuple[int, int, str]]:
    """Parse (sura, ayah, text) triples and verify the copyright block and structure."""
    text = xml_bytes.decode("utf-8")
    if "Tanzil Quran Text" not in text or "Creative Commons Attribution 3.0" not in text:
        raise SystemExit(
            "refusing to build: the source XML is missing the Tanzil copyright block.\n"
            "The CC-BY-3.0 notice must travel with the text."
        )

    root = ET.fromstring(text)
    surahs = root.findall("sura")
    if len(surahs) != 114:
        raise SystemExit(f"refusing to build: expected 114 surahs, found {len(surahs)}")

    rows: list[tuple[int, int, str]] = []
    for position, surah in enumerate(surahs, start=1):
        index = int(surah.get("index"))
        if index != position:
            raise SystemExit(f"refusing to build: surah order broke at position {position}")
        ayahs = surah.findall("aya")
        if len(ayahs) != EXPECTED_AYAH_COUNTS[position - 1]:
            raise SystemExit(
                f"refusing to build: surah {index} has {len(ayahs)} ayahs, "
                f"expected {EXPECTED_AYAH_COUNTS[position - 1]}"
            )
        for number, ayah in enumerate(ayahs, start=1):
            if int(ayah.get("index")) != number:
                raise SystemExit(f"refusing to build: ayah numbering broke at {index}:{number}")
            body = (ayah.get("text") or "").strip()
            if not body:
                raise SystemExit(f"refusing to build: empty text at {index}:{number}")
            rows.append((index, number, body))

    if len(rows) != sum(EXPECTED_AYAH_COUNTS):
        raise SystemExit(f"refusing to build: expected {sum(EXPECTED_AYAH_COUNTS)} ayahs")
    print(f"parsed {len(surahs)} surahs / {len(rows)} ayahs")
    return rows


def write_database(rows: list[tuple[int, int, str]], destination: Path) -> None:
    """Create the SQLite asset, then verify it independently before leaving it in place."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        destination.unlink()

    connection = sqlite3.connect(destination)
    try:
        connection.execute("PRAGMA journal_mode = OFF")
        connection.execute("PRAGMA synchronous = OFF")
        connection.execute(
            "CREATE TABLE arabic_text ("
            "sura INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL)"
        )
        connection.execute(
            "CREATE TABLE properties (property TEXT NOT NULL, value TEXT NOT NULL)"
        )
        connection.executemany("INSERT INTO arabic_text VALUES (?, ?, ?)", rows)
        connection.executemany(
            "INSERT INTO properties VALUES (?, ?)",
            [
                ("schema_version", SCHEMA_VERSION),
                ("text_version", "uthmani-1.1"),
                ("source", SOURCE_NAME),
                ("license", SOURCE_LICENSE),
                ("qira", "hafs"),
            ],
        )
        # Covers "WHERE sura = ? ORDER BY ayah ASC" so a surah loads without a scan or a sort.
        connection.execute("CREATE INDEX arabic_text_sura_ayah ON arabic_text (sura, ayah)")
        connection.commit()
        connection.execute("VACUUM")
        connection.commit()
    finally:
        connection.close()

    verify(destination)


def verify(destination: Path) -> None:
    connection = sqlite3.connect(f"file:{destination}?mode=ro", uri=True)
    try:
        integrity = connection.execute("PRAGMA quick_check").fetchone()[0]
        if integrity != "ok":
            raise SystemExit(f"refusing to ship: PRAGMA quick_check returned {integrity!r}")

        fetched = connection.execute(
            "SELECT sura, ayah, text FROM arabic_text ORDER BY sura, ayah"
        ).fetchall()
        if len(fetched) != 6236:
            raise SystemExit(f"refusing to ship: {len(fetched)} ayahs in the built database")
        if [row[:2] for row in fetched] != [
            (surah, ayah)
            for surah, count in enumerate(EXPECTED_AYAH_COUNTS, start=1)
            for ayah in range(1, count + 1)
        ]:
            raise SystemExit("refusing to ship: surah/ayah keys are not complete and in order")
        if any(not row[2].strip() for row in fetched):
            raise SystemExit("refusing to ship: blank ayah text found")

        contract = connection.execute(
            "SELECT sura, ayah, text FROM arabic_text WHERE sura = ? ORDER BY ayah ASC", (1,)
        ).fetchall()
        if len(contract) != 7:
            raise SystemExit("refusing to ship: the query contract did not return surah 1")

        properties = dict(connection.execute("SELECT property, value FROM properties").fetchall())
        if properties.get("source") != SOURCE_NAME:
            raise SystemExit("refusing to ship: provenance is not recorded in properties")
    finally:
        connection.close()

    size = destination.stat().st_size
    print(f"verified {destination.relative_to(ROOT)} ({size:,} bytes)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--xml", type=Path, help="local Tanzil quran-uthmani.xml instead of downloading")
    parser.add_argument("--out", type=Path, default=OUTPUT, help="destination .db path")
    arguments = parser.parse_args()

    rows = parse_rows(fetch_xml(arguments.xml))
    write_database(rows, arguments.out)
    print("done")
    return 0


if __name__ == "__main__":
    sys.exit(main())
