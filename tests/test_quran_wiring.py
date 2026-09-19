"""Static wiring checks for the Quran feature.

There is no JDK or Android SDK in this workspace, so nothing here compiles the app. These checks
instead resolve every resource reference the Quran screens make -- R.id/R.string/R.color/... from
Java and @color/@string/@id/... from XML -- against the resources actually defined in res/, and
verify format-string arity and manifest wiring. That catches the class of error a missing compiler
would otherwise hide until Android Studio is opened.

Framework and AndroidX resources (@android:..., ?android:attr/..., library styles) are deliberately
out of scope: they are resolved by AAPT2 against the platform and dependency AARs.
"""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
JAVA = MAIN / "java/com/clock/livewallpaper"

# Files this feature owns, so the checks stay precise instead of scanning the legacy wallpaper UI.
QURAN_JAVA = [
    JAVA / "quran/QuranDatabaseHelper.java",
    JAVA / "quran/QuranMetadata.java",
    JAVA / "quran/SurahIndex.java",
    JAVA / "activity/QuranActivity.java",
    JAVA / "activity/SurahListActivity.java",
    JAVA / "adapter/SurahListAdapter.java",
    JAVA / "utils/EdgeToEdgeInsets.java",
    JAVA / "AppClass.java",
]
QURAN_XML = [
    MAIN / "AndroidManifest.xml",
    RES / "layout/activity_quran.xml",
    RES / "layout/activity_surah_list.xml",
    RES / "layout/item_surah.xml",
    RES / "values/quran.xml",
    RES / "values/colors.xml",
    RES / "color/quran_nav_button_text.xml",
    RES / "drawable/bg_quran_button.xml",
    RES / "drawable/bg_quran_nav_button.xml",
    RES / "drawable/bg_surah_badge.xml",
    RES / "drawable/bg_surah_row.xml",
]

VALUE_TYPES = {"color", "string", "string-array", "style", "dimen", "integer", "bool", "plurals",
               "attr", "item", "array", "declare-styleable", "eat-comment"}
FILE_TYPES = {"drawable", "layout", "anim", "animator", "font", "menu", "mipmap", "raw", "color",
              "xml", "transition", "interpolator"}


def call_arguments(source, method):
    """Yield the raw argument list of every `method(...)` call, using balanced-paren scanning.

    A regex that stops at the first `);` mis-reads nested calls such as
    `setText(getString(R.string.x, y));`, which is exactly the shape the Quran screens use.
    """
    for match in re.finditer(r"\b" + re.escape(method) + r"\s*\(", source):
        index = match.end()
        depth = 1
        in_string = False
        in_char = False
        while index < len(source) and depth > 0:
            char = source[index]
            if in_string:
                if char == "\\":
                    index += 1
                elif char == '"':
                    in_string = False
            elif in_char:
                if char == "\\":
                    index += 1
                elif char == "'":
                    in_char = False
            elif char == '"':
                in_string = True
            elif char == "'":
                in_char = True
            elif char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
            index += 1
        yield source[match.end():index - 1]


def split_top_level(argument_list):
    """Split a Java argument list on commas that are not nested in (), [], {}, strings or chars."""
    parts = []
    current = []
    depth = 0
    in_string = False
    in_char = False
    index = 0
    while index < len(argument_list):
        char = argument_list[index]
        if in_string:
            current.append(char)
            if char == "\\":
                index += 1
                if index < len(argument_list):
                    current.append(argument_list[index])
            elif char == '"':
                in_string = False
        elif in_char:
            current.append(char)
            if char == "\\":
                index += 1
                if index < len(argument_list):
                    current.append(argument_list[index])
            elif char == "'":
                in_char = False
        elif char == '"':
            in_string = True
            current.append(char)
        elif char == "'":
            in_char = True
            current.append(char)
        elif char in "([{":
            depth += 1
            current.append(char)
        elif char in ")]}":
            depth -= 1
            current.append(char)
        elif char == "," and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(char)
        index += 1
    parts.append("".join(current))
    if len(parts) == 1 and not parts[0].strip():
        return []
    return parts


def defined_values():
    """Every name defined in res/values/*.xml, keyed by resource type."""
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
    """Every file-based resource, keyed by directory type (res/color counts as both)."""
    found = {}
    for path in sorted(RES.rglob("*")):
        if not path.is_file() or "." not in path.name:
            continue
        kind = path.parent.name.split("-")[0]
        if kind in FILE_TYPES:
            found.setdefault(kind, set()).add(path.name.rsplit(".", 1)[0])
    return found


def defined_ids():
    """Every @+id declared in any layout, menu or drawable."""
    ids = set()
    for path in list(RES.glob("layout*/*.xml")) + list(RES.glob("menu/*.xml")) \
            + list(RES.glob("drawable*/*.xml")):
        ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)', path.read_text(encoding="utf-8")))
    for path in sorted(RES.glob("values*/*.xml")):
        for node in ET.parse(path).getroot():
            if node.tag == "item" and node.get("type") == "id":
                ids.add(node.get("name"))
    return ids


class ResourceWiringTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.values = defined_values()
        cls.files = defined_files()
        cls.ids = defined_ids()

    def assert_resolves(self, kind, name, where):
        if kind == "id":
            self.assertIn(name, self.ids, f"{where}: R.id.{name} is not declared by any @+id/")
            return
        if kind in self.values and name in self.values[kind]:
            return
        self.assertIn(name, self.files.get(kind, set()),
                      f"{where}: @{kind}/{name} is not defined in res/")

    def test_every_java_reference_resolves(self):
        pattern = re.compile(r"\bR\.([a-z]+)\.([A-Za-z0-9_]+)")
        for path in QURAN_JAVA:
            source = path.read_text(encoding="utf-8")
            # Strip comments so javadoc examples cannot be mistaken for live references.
            source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
            source = re.sub(r"//.*", "", source)
            for kind, name in pattern.findall(source):
                self.assert_resolves(kind, name, path.name)

    def test_every_xml_reference_resolves(self):
        pattern = re.compile(r'(?<![\w:])@(\w+)/([A-Za-z0-9_.]+)')
        for path in QURAN_XML:
            text = path.read_text(encoding="utf-8")
            text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
            for kind, name in pattern.findall(text):
                if kind in ("android", "app", "tools"):
                    continue  # framework, AndroidX or tools-only namespaces
                if kind == "style":
                    # Library parents such as Theme.Material.* are resolved by AAPT2.
                    self.assertTrue(
                        name in self.values.get("style", set()) or "." in name,
                        f"{path.name}: @style/{name} is neither local nor a qualified parent")
                    continue
                base = name.split(".")[0]
                self.assert_resolves(kind, base, path.name)

    def test_layout_ids_used_by_java_are_declared(self):
        """findViewById ids must exist in the layout each activity actually inflates."""
        pairs = {
            "activity/QuranActivity.java": "layout/activity_quran.xml",
            "activity/SurahListActivity.java": "layout/activity_surah_list.xml",
            "adapter/SurahListAdapter.java": "layout/item_surah.xml",
        }
        for java, layout in pairs.items():
            source = (JAVA / java).read_text(encoding="utf-8")
            source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
            wanted = set(re.findall(r"findViewById\(R\.id\.([A-Za-z0-9_]+)\)", source))
            declared = set(re.findall(r'@\+id/([A-Za-z0-9_]+)',
                                      (RES / layout).read_text(encoding="utf-8")))
            self.assertTrue(wanted, f"{java} declares no findViewById ids")
            self.assertEqual(wanted - declared, set(),
                             f"{java} binds ids missing from {layout}: {sorted(wanted - declared)}")

    def test_format_string_arity_matches_call_sites(self):
        """A %1$d with the wrong number of arguments is a runtime crash, not a build error."""
        strings = {}
        for path in sorted(RES.glob("values*/*.xml")):
            for node in ET.parse(path).getroot():
                if node.tag == "string" and node.get("name"):
                    raw = "".join(node.itertext())
                    strings[node.get("name")] = raw.replace("&#8230;", "…")

        placeholder = re.compile(r"%(?:(\d+)\$)?([-#+ 0,(]*)(\d*)(?:\.\d+)?([a-zA-Z])")

        def arity(template):
            matches = list(placeholder.finditer(template))
            if not matches:
                return 0
            positional = [int(m.group(1)) for m in matches if m.group(1)]
            if positional:
                return max(positional)
            return len(matches)

        checked = 0
        for path in QURAN_JAVA:
            source = re.sub(r"/\*.*?\*/", "", path.read_text(encoding="utf-8"), flags=re.S)
            source = re.sub(r"//[^\n]*", "", source)
            for inner in call_arguments(source, "getString"):
                parts = split_top_level(inner)
                if not parts:
                    continue
                # Cursor.getString(int columnIndex) shares the method name with
                # Context.getString(int resId, Object...); only the latter is checked here.
                match = re.fullmatch(r"R\.string\.([A-Za-z0-9_]+)", parts[0].strip())
                if match is None:
                    continue
                name = match.group(1)
                self.assertIn(name, strings, f"{path.name}: R.string.{name} is not defined")
                expected = arity(strings[name])
                supplied = len(parts) - 1
                self.assertEqual(supplied, expected,
                                 f"{path.name}: getString(R.string.{name}, ...) passes {supplied} "
                                 f"arguments but the string takes {expected}: {strings[name]!r}")
                checked += 1
        self.assertGreater(checked, 0, "no getString call sites were checked")

    def test_manifest_activities_have_sources(self):
        android = "{http://schemas.android.com/apk/res/android}"
        manifest = ET.parse(MAIN / "AndroidManifest.xml")
        names = [a.get(android + "name") for a in manifest.findall(".//activity")]
        for name in names:
            relative = name.replace("com.clock.livewallpaper.", "").replace(".", "/") + ".java"
            self.assertTrue((JAVA / relative).exists(), f"manifest declares {name} but {relative} is missing")
        for required in ("activity.SurahListActivity", "activity.QuranActivity"):
            self.assertIn("com.clock.livewallpaper." + required, names)

    def test_no_network_references_remain(self):
        """The reader is offline only: no HTTP, no URL, no sockets in the Quran feature."""
        banned = re.compile(r"https?://|HttpURLConnection|okhttp|Volley|URL\(|Socket\(|INTERNET")
        for path in QURAN_JAVA:
            source = path.read_text(encoding="utf-8")
            source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
            source = re.sub(r"//.*", "", source)
            self.assertIsNone(banned.search(source), f"{path.name} still references the network")

    def test_java_braces_and_packages_balance(self):
        for path in QURAN_JAVA:
            source = path.read_text(encoding="utf-8")
            self.assertEqual(source.count("{"), source.count("}"),
                             f"{path.name}: unbalanced braces")
            self.assertTrue(source.startswith("package com.clock.livewallpaper"),
                            f"{path.name}: unexpected package declaration")

    def test_all_xml_files_parse(self):
        for path in sorted(MAIN.rglob("*.xml")):
            with self.subTest(path=str(path.relative_to(MAIN))):
                ET.parse(path)


if __name__ == "__main__":
    unittest.main()
