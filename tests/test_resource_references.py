"""Every resource the app references must exist in ``res/`` - and the reverse where it matters.

This workspace has no JDK and no Android SDK (Gradle cannot run here), so these checks stand in
for the part of ``aapt2 link`` that catches dangling ``R.*`` references. They resolve every
resource name used by the Kotlin sources, the XML resources and the manifest against what is
actually declared under ``app/src/main/res``.

Framework resources (``@android:``, ``?attr/``, ``?android:attr/``) and AndroidX library styles
are deliberately out of scope: those are resolved by the SDK and the AARs, not by this repository.

Run with::

    python3 -m unittest discover -s tests -v
"""
import os
import re
import unittest
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(REPO, "app", "src", "main")
RES = os.path.join(MAIN, "res")
JAVA = os.path.join(MAIN, "java")
MANIFEST = os.path.join(MAIN, "AndroidManifest.xml")

VALUE_TAGS = {
    "string": "string",
    "string-array": "array",
    "integer-array": "array",
    "color": "color",
    "dimen": "dimen",
    "style": "style",
    "bool": "bool",
    "integer": "integer",
    "plurals": "plurals",
}

FILE_RES_DIRS = {
    "drawable": "drawable",
    "layout": "layout",
    "xml": "xml",
    "font": "font",
    "mipmap": "mipmap",
    "anim": "anim",
    "raw": "raw",
}


def kotlin_files():
    for root, _dirs, files in os.walk(JAVA):
        for name in sorted(files):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def xml_files():
    for root, _dirs, files in os.walk(RES):
        for name in sorted(files):
            if name.endswith(".xml"):
                yield os.path.join(root, name)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def split_arguments(source, open_index):
    """Splits the argument list of a call whose ``(`` sits at ``open_index``.

    Returns ``(arguments, index_of_closing_paren)`` and handles nested calls, strings and
    lambdas, which a plain regular expression cannot.
    """
    depth = 0
    args = []
    current = []
    index = open_index
    in_string = False
    while index < len(source):
        char = source[index]
        if in_string:
            if char == "\\":
                current.append(source[index:index + 2])
                index += 2
                continue
            if char == '"':
                in_string = False
            current.append(char)
            index += 1
            continue
        if char == '"':
            in_string = True
            current.append(char)
            index += 1
            continue
        if char in "([{":
            depth += 1
            if depth == 1 and char == "(":
                index += 1
                continue
            current.append(char)
        elif char in ")]}":
            depth -= 1
            if depth == 0:
                tail = "".join(current).strip()
                if tail:
                    args.append(tail)
                return args, index
            current.append(char)
        elif char == "," and depth == 1:
            args.append("".join(current).strip())
            current = []
        else:
            current.append(char)
        index += 1
    return args, None


def strip_kotlin_comments(source):
    """Drops // and /* */ comments so prose in KDoc is not mistaken for code."""
    out = []
    index = 0
    length = len(source)
    while index < length:
        two = source[index:index + 2]
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


def referenced_resource_names(*kinds):
    """Every resource name referenced from Kotlin, res/ XML or the manifest."""
    names = set()
    for path in kotlin_files():
        source = read(path)
        for kind in kinds:
            names |= set(re.findall(r"R\.%s\.(\w+)" % kind, source))
    for path in list(xml_files()) + [MANIFEST]:
        source = read(path)
        for kind in kinds:
            names |= set(re.findall(r"@%s/(\w+)" % kind, source))
    return names


def declared_resources():
    """Maps resource type -> set of declared names."""
    declared = {kind: set() for kind in set(VALUE_TAGS.values())}
    for kind in FILE_RES_DIRS.values():
        declared.setdefault(kind, set())
    declared.setdefault("id", set())

    # values*/ declarations
    for entry in sorted(os.listdir(RES)):
        if not entry.startswith("values"):
            continue
        folder = os.path.join(RES, entry)
        if not os.path.isdir(folder):
            continue
        for name in sorted(os.listdir(folder)):
            if not name.endswith(".xml"):
                continue
            root = ET.parse(os.path.join(folder, name)).getroot()
            for child in root:
                if child.tag in VALUE_TAGS and child.get("name"):
                    declared[VALUE_TAGS[child.tag]].add(child.get("name"))

    # file based resources
    for entry in sorted(os.listdir(RES)):
        folder = os.path.join(RES, entry)
        if not os.path.isdir(folder):
            continue
        base = entry.split("-")[0]
        if base not in FILE_RES_DIRS:
            continue
        for name in sorted(os.listdir(folder)):
            stem = name.split(".")[0]
            declared[FILE_RES_DIRS[base]].add(stem)

    # ids declared inside layouts
    for path in xml_files():
        for match in re.finditer(r'android:id="@\+id/([A-Za-z0-9_]+)"', read(path)):
            declared["id"].add(match.group(1))

    return declared


DECLARED = declared_resources()


class KotlinResourceReferencesTest(unittest.TestCase):
    """``R.<type>.<name>`` in Kotlin must resolve."""

    def test_every_r_reference_exists(self):
        missing = []
        pattern = re.compile(r"\bR\.(\w+)\.(\w+)\b")
        for path in kotlin_files():
            for kind, name in pattern.findall(read(path)):
                if kind not in DECLARED:
                    missing.append((os.path.relpath(path, REPO), kind, name, "unknown type"))
                elif name not in DECLARED[kind]:
                    missing.append((os.path.relpath(path, REPO), kind, name, "not declared"))
        self.assertEqual([], missing, f"unresolved R references: {missing}")

    def test_no_android_r_shortcut(self):
        """``android.R`` layouts/strings are not used; the product ships its own."""
        offenders = []
        for path in kotlin_files():
            if re.search(r"\bandroid\.R\.(layout|string|drawable)\b", read(path)):
                offenders.append(os.path.relpath(path, REPO))
        self.assertEqual([], offenders)


class XmlResourceReferencesTest(unittest.TestCase):
    """``@type/name`` inside res/ and the manifest must resolve."""

    REFERENCE = re.compile(r'"@(?!\+)(?!android:)(\w+)/([A-Za-z0-9_.]+)"')

    def _check(self, path):
        missing = []
        for kind, name in self.REFERENCE.findall(read(path)):
            if kind in ("attr", "aapt"):
                continue
            bucket = DECLARED.get(kind)
            if bucket is None:
                missing.append((os.path.relpath(path, REPO), kind, name, "unknown type"))
            elif name not in bucket:
                missing.append((os.path.relpath(path, REPO), kind, name, "not declared"))
        return missing

    def test_res_xml_references(self):
        missing = []
        for path in xml_files():
            missing.extend(self._check(path))
        self.assertEqual([], missing, f"unresolved XML references: {missing}")

    def test_manifest_references(self):
        self.assertEqual([], self._check(MANIFEST))


class ManifestComponentsTest(unittest.TestCase):
    """Every component declared in the manifest exists as a Kotlin class, and vice versa."""

    def setUp(self):
        self.manifest = read(MANIFEST)
        self.package = "com.clock.livewallpaper"

    def _class_path(self, relative):
        return os.path.join(JAVA, *self.package.split("."), *relative.lstrip(".").split("."))

    def test_declared_components_exist(self):
        missing = []
        for match in re.finditer(r'android:name="(\.[\w.]+)"', self.manifest):
            path = self._class_path(match.group(1)) + ".kt"
            if not os.path.exists(path):
                missing.append(match.group(1))
        self.assertEqual([], missing, f"manifest points at missing classes: {missing}")

    def test_every_receiver_and_activity_is_declared(self):
        """A BroadcastReceiver / Activity that is not in the manifest would never run."""
        declared = set(re.findall(r'android:name="\.([\w.]+)"', self.manifest))
        undeclared = []
        for path in kotlin_files():
            source = read(path)
            if re.search(r"class\s+\w+\s*:\s*(BroadcastReceiver|AppWidgetProvider)\b", source) or \
               re.search(r":\s*ComponentActivity\b", source):
                relative = os.path.relpath(path, os.path.join(JAVA, *self.package.split(".")))
                name = relative[:-3].replace(os.sep, ".")
                if name not in declared:
                    undeclared.append(name)
        self.assertEqual([], undeclared, f"components missing from the manifest: {undeclared}")

    def test_application_class_declared(self):
        self.assertIn('android:name=".DhikrApplication"', self.manifest)


class UnusedFileResourceTest(unittest.TestCase):
    """A drawable or layout nobody references only makes the APK bigger."""

    def test_no_unused_file_resources(self):
        unused = {}
        for kind in ("drawable", "layout", "xml", "font", "mipmap"):
            dead = sorted(DECLARED[kind] - referenced_resource_names(kind))
            if dead:
                unused[kind] = dead
        self.assertEqual({}, unused, f"unused resources: {unused}")


class StringResourceTest(unittest.TestCase):
    """The Arabic copy itself has to be well formed."""

    def setUp(self):
        self.path = os.path.join(RES, "values", "strings.xml")
        self.root = ET.parse(self.path).getroot()
        self.strings = {
            child.get("name"): (child.text or "")
            for child in self.root
            if child.tag == "string"
        }

    def test_no_empty_strings(self):
        empty = [name for name, value in self.strings.items() if not value.strip()]
        self.assertEqual([], empty)

    def test_positional_format_arguments_only(self):
        """Counters are formatted as Arabic-Indic text, so every argument must be ``%1$s``."""
        offenders = []
        for name, value in self.strings.items():
            if re.search(r"%(?!\d+\$s)", value):
                offenders.append((name, value))
        self.assertEqual([], offenders, f"non positional / non string formats: {offenders}")

    def test_format_arguments_match_call_sites(self):
        """``stringResource(R.string.x, a, b)`` must pass exactly as many arguments as ``x`` needs."""
        arity = {
            name: len(set(re.findall(r"%(\d+)\$s", value)))
            for name, value in self.strings.items()
        }
        mismatches = []
        for path in kotlin_files():
            source = read(path)
            for start in (m.start() for m in re.finditer(r"\bstringResource\(", source)):
                open_index = source.index("(", start)
                args, end = split_arguments(source, open_index)
                if end is None or not args:
                    continue
                first = args[0].strip()
                match = re.fullmatch(r"R\.string\.(\w+)", first)
                if not match:
                    continue
                name = match.group(1)
                expected = arity.get(name, 0)
                passed = len(args) - 1
                if expected != passed:
                    mismatches.append(
                        (os.path.relpath(path, REPO), name, f"expected {expected}, passed {passed}")
                    )
        self.assertEqual([], mismatches, f"format argument mismatches: {mismatches}")

    def test_arabic_copy(self):
        """Every user facing string is Arabic; only licence names may carry Latin letters."""
        allowed_latin = {"SIL", "OFL"}
        latin = []
        for name, value in self.strings.items():
            stripped = re.sub(r"%\d+\$s", "", value)
            for token in re.findall(r"[A-Za-z]+", stripped):
                if token not in allowed_latin:
                    latin.append((name, token))
        self.assertEqual([], latin, f"non Arabic UI copy: {latin}")

    def test_no_dead_strings(self):
        """Dead copy is a maintenance trap: every declared string must be referenced."""
        declared = set(self.strings) | {
            child.get("name") for child in self.root if child.tag == "string-array"
        }
        self.assertEqual([], sorted(declared - referenced_resource_names("string", "array")))

    def test_no_arabic_literals_in_kotlin(self):
        """All user facing copy comes from resources: no Arabic literal survives in Kotlin."""
        arabic = re.compile(r"[\u0600-\u06FF]")
        offenders = []
        for path in kotlin_files():
            code = strip_kotlin_comments(read(path))
            for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', code):
                if arabic.search(literal):
                    offenders.append((os.path.relpath(path, REPO), literal))
        self.assertEqual([], offenders, f"hard coded Arabic copy: {offenders}")

    def test_night_and_v28_overrides_are_known_names(self):
        """A qualified override that misspells its name silently does nothing at runtime."""
        base_names = {
            kind: set(DECLARED[kind]) for kind in ("string", "color", "style", "dimen", "bool")
        }
        unknown = []
        for entry in sorted(os.listdir(RES)):
            if not entry.startswith("values-"):
                continue
            folder = os.path.join(RES, entry)
            for name in sorted(os.listdir(folder)):
                root = ET.parse(os.path.join(folder, name)).getroot()
                for child in root:
                    kind = VALUE_TAGS.get(child.tag)
                    if kind in base_names and child.get("name") not in base_names[kind]:
                        unknown.append((entry, name, child.get("name")))
        self.assertEqual([], unknown, f"qualified resources with no base declaration: {unknown}")


if __name__ == "__main__":
    unittest.main()
