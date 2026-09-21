"""Guards against the build failing with a bare StAX NullPointerException.

The symptom this file exists for is an Android build that dies with::

    Cannot invoke "javax.xml.stream.events.Attribute.getValue()" because the return value of
    "javax.xml.stream.events.StartElement.getAttributeByName(javax.xml.namespace.QName)" is null

Nothing in ``src/main/java`` uses StAX -- ``javax.xml.stream`` is not even on Android. The reader
that crashes belongs to the *build tools*, which parse every file under ``res/`` with StAX before
AAPT2 ever sees it. Those parsers fetch a required attribute and use it without a null check, for
example in AGP's ``aaptcompiler``::

    val nameAttribute = element.getAttributeByName(QName("name"))   # TableExtractor.kt
    val nameAttribute = attrElement.getAttributeByName(QName("name"))  # XmlProcessor.kt

When the attribute is absent the call returns null and the ``.getValue()` that follows throws.
The exception carries no file name and no line number, so a single missing ``name=`` in one
resource looks like a broken toolchain and sends you hunting through Gradle caches.

These tests reproduce the same required-attribute rules in Python. A file that would make the
build tools throw fails here instead, naming the file, the element and the missing attribute.
They are static checks: no JDK or Android SDK is needed, matching the rest of ``tests/``.

The second half pins the two build settings that were removed while fixing this, because both
ran an XML pass that could raise the very same exception with nothing to show for it:

* ``dataBinding`` was enabled while no layout used data binding at all;
* ``android.enableJetifier`` was enabled while no dependency needed the support-library rewrite.
"""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
MANIFEST = MAIN / "AndroidManifest.xml"

ANDROID_NS = "http://schemas.android.com/apk/res/android"
AAPT_NS = "http://schemas.android.com/aapt"
A = "{%s}" % ANDROID_NS


def res_xml_files():
    """Every XML the build tools parse out of res/, sorted for a stable failure order."""
    return sorted(RES.rglob("*.xml"))


def relative(path):
    return str(path.relative_to(ROOT))


# Top-level children of <resources> that AAPT2 reads a name= off. Anything here without a name
# is what makes getAttributeByName("name") return null.
NEEDS_NAME = {
    "string", "color", "dimen", "bool", "integer", "fraction", "drawable", "id",
    "style", "array", "string-array", "integer-array", "plurals", "attr", "declare-styleable",
    "public", "item",
}


class WellFormedTests(unittest.TestCase):
    """A StAX reader gives up on malformed XML before any attribute lookup happens."""

    def test_every_resource_file_parses(self):
        files = res_xml_files()
        self.assertTrue(files, "no XML found under res/ -- the test is looking in the wrong place")
        for path in files:
            with self.subTest(file=relative(path)):
                try:
                    ET.parse(path)
                except ET.ParseError as error:
                    # Covers unbound prefixes, duplicate attributes and unclosed tags: each one
                    # aborts the build tools' parse with a message that never names the file.
                    self.fail(f"{relative(path)} is not well-formed XML: {error}")

    def test_manifest_parses(self):
        try:
            ET.parse(MANIFEST)
        except ET.ParseError as error:
            self.fail(f"AndroidManifest.xml is not well-formed XML: {error}")

    def test_no_byte_order_mark_and_no_leading_whitespace(self):
        # A BOM or a blank line before <?xml ...?> makes some readers fail on the declaration,
        # which surfaces as the same unhelpful stack trace.
        for path in res_xml_files() + [MANIFEST]:
            with self.subTest(file=relative(path)):
                head = path.read_bytes()[:4]
                self.assertFalse(head.startswith(b"\xef\xbb\xbf"),
                                 f"{relative(path)} starts with a UTF-8 BOM")
                self.assertFalse(head.startswith((b"\xff\xfe", b"\xfe\xff")),
                                 f"{relative(path)} is UTF-16; resources must be UTF-8")
                self.assertFalse(head[:1] in (b" ", b"\t", b"\n", b"\r"),
                                 f"{relative(path)} has whitespace before the XML declaration")


class ValuesRequireNameTests(unittest.TestCase):
    """res/values/*.xml is where the crashing name= lookup lives."""

    def values_files(self):
        return sorted(path for path in res_xml_files()
                      if path.parent.name.startswith("values"))

    def test_values_files_are_resources_documents(self):
        for path in self.values_files():
            with self.subTest(file=relative(path)):
                self.assertEqual(ET.parse(path).getroot().tag, "resources",
                                 f"{relative(path)} must have a <resources> root")

    def test_every_resource_entry_declares_a_name(self):
        for path in self.values_files():
            for entry in ET.parse(path).getroot():
                if not isinstance(entry.tag, str):
                    continue  # comment or processing instruction
                if entry.tag not in NEEDS_NAME:
                    continue
                with self.subTest(file=relative(path), element=entry.tag):
                    self.assertIn(
                        "name", entry.attrib,
                        f'{relative(path)}: <{entry.tag}> has no name= attribute. AAPT2 reads '
                        f'this with getAttributeByName(QName("name")) and throws a bare '
                        f"NullPointerException when it is missing.")

    def test_top_level_items_declare_a_type(self):
        for path in self.values_files():
            for entry in ET.parse(path).getroot():
                if isinstance(entry.tag, str) and entry.tag == "item":
                    with self.subTest(file=relative(path)):
                        self.assertIn("type", entry.attrib,
                                      f"{relative(path)}: a top-level <item> needs type=")

    def test_style_items_declare_a_name(self):
        for path in self.values_files():
            for style in ET.parse(path).getroot():
                if not (isinstance(style.tag, str) and style.tag == "style"):
                    continue
                for item in style:
                    if not isinstance(item.tag, str):
                        continue
                    with self.subTest(file=relative(path), style=style.get("name")):
                        self.assertEqual(item.tag, "item",
                                         f"{relative(path)}: <style> may only contain <item>")
                        self.assertIn("name", item.attrib,
                                      f'{relative(path)}: <item> in style "{style.get("name")}" '
                                      f"has no name=")

    def test_styleable_attributes_are_fully_declared(self):
        # attrs.xml is load-bearing here: AnalogClock reads index 0 of CustomAnalogClock, so an
        # <attr> that loses its name would both crash the parser and silently shift the indices.
        for path in self.values_files():
            for styleable in ET.parse(path).getroot():
                if not (isinstance(styleable.tag, str)
                        and styleable.tag in ("declare-styleable", "attr")):
                    continue
                for attr in styleable.iter():
                    if attr is styleable or not isinstance(attr.tag, str):
                        continue
                    label = styleable.get("name")
                    if attr.tag == "attr":
                        with self.subTest(file=relative(path), styleable=label):
                            self.assertIn("name", attr.attrib,
                                          f"{relative(path)}: <attr> in {label} has no name=")
                    elif attr.tag in ("enum", "flag"):
                        with self.subTest(file=relative(path), styleable=label):
                            self.assertIn("name", attr.attrib,
                                          f"{relative(path)}: <{attr.tag}> in {label} has no name=")
                            self.assertIn("value", attr.attrib,
                                          f"{relative(path)}: <{attr.tag}> in {label} has no value=")

    def test_plurals_items_declare_a_quantity(self):
        for path in self.values_files():
            for plural in ET.parse(path).getroot():
                if not (isinstance(plural.tag, str) and plural.tag == "plurals"):
                    continue
                for item in plural:
                    if not isinstance(item.tag, str):
                        continue
                    with self.subTest(file=relative(path), plurals=plural.get("name")):
                        self.assertIn("quantity", item.attrib,
                                      f"{relative(path)}: <item> in plurals "
                                      f'"{plural.get("name")}" has no quantity=')

    def test_no_duplicate_resource_names(self):
        seen = {}
        for path in self.values_files():
            for entry in ET.parse(path).getroot():
                if not isinstance(entry.tag, str) or not entry.get("name"):
                    continue
                key = (entry.tag, entry.get("name"))
                if key in seen:
                    self.fail(f'duplicate {entry.tag} name="{entry.get("name")}" in '
                              f"{seen[key]} and {relative(path)}")
                seen[key] = relative(path)


class InlineResourceTests(unittest.TestCase):
    """<aapt:attr> is the other place a missing name= reaches getAttributeByName."""

    def test_inline_aapt_attributes_declare_a_name(self):
        for path in res_xml_files():
            for element in ET.parse(path).getroot().iter():
                if element.tag == f"{{{AAPT_NS}}}attr":
                    with self.subTest(file=relative(path)):
                        self.assertIn("name", element.attrib,
                                      f"{relative(path)}: <aapt:attr> has no name=")


class LayoutTests(unittest.TestCase):
    """Attributes the layout inflater and the build tools both insist on."""

    def layouts(self):
        return sorted(path for path in res_xml_files() if path.parent.name.startswith("layout"))

    def test_structural_tags_are_complete(self):
        for path in self.layouts():
            for element in ET.parse(path).getroot().iter():
                if not isinstance(element.tag, str):
                    continue
                name = relative(path)
                if element.tag == "include":
                    with self.subTest(file=name, element="include"):
                        self.assertIn("layout", element.attrib,
                                      f"{name}: <include> has no layout=")
                elif element.tag == "fragment":
                    with self.subTest(file=name, element="fragment"):
                        self.assertTrue(f"{A}name" in element.attrib or "class" in element.attrib,
                                        f"{name}: <fragment> needs android:name or class")
                elif element.tag == "view":
                    with self.subTest(file=name, element="view"):
                        self.assertIn("class", element.attrib, f"{name}: <view> has no class=")

    def test_namespace_prefixes_are_declared(self):
        # An undeclared prefix is a hard parse error for a namespace-aware StAX reader. ET raises
        # "unbound prefix" for the same input, so a real miss cannot slip through.
        for path in res_xml_files():
            text = path.read_text(encoding="utf-8")
            declared = set(re.findall(r'xmlns:([\w.\-]+)\s*=', text)) | {"xml", "xmlns"}
            used = set(re.findall(r'[\s"\'<]([A-Za-z_][\w.\-]*):[\w.\-]+\s*=', text))
            for prefix in sorted(used - declared):
                with self.subTest(file=relative(path), prefix=prefix):
                    self.fail(f'{relative(path)}: prefix "{prefix}:" is used but xmlns:{prefix} '
                              f"is never declared")


class ManifestTests(unittest.TestCase):
    """The manifest merger runs its own StAX pass over this file."""

    def test_components_and_metadata_declare_android_name(self):
        root = ET.parse(MANIFEST).getroot()
        required = ("activity", "service", "provider", "receiver", "meta-data",
                    "action", "category", "uses-permission", "uses-library")
        for element in root.iter():
            if isinstance(element.tag, str) and element.tag in required:
                with self.subTest(element=element.tag):
                    self.assertIn(f"{A}name", element.attrib,
                                  f"<{element.tag}> in AndroidManifest.xml has no android:name")

    def test_metadata_entries_carry_a_value_or_resource(self):
        for element in ET.parse(MANIFEST).getroot().iter("meta-data"):
            key = element.get(f"{A}name")
            with self.subTest(meta_data=key):
                self.assertTrue(f"{A}value" in element.attrib or f"{A}resource" in element.attrib,
                                f'<meta-data android:name="{key}"> needs android:value '
                                f"or android:resource")

    def test_every_referenced_xml_resource_exists(self):
        for element in ET.parse(MANIFEST).getroot().iter("meta-data"):
            resource = element.get(f"{A}resource", "")
            if resource.startswith("@xml/"):
                target = RES / "xml" / f"{resource.split('/', 1)[1]}.xml"
                with self.subTest(resource=resource):
                    self.assertTrue(target.is_file(), f"{resource} is referenced but missing")


class BuildConfigurationTests(unittest.TestCase):
    """Keep the two XML passes that produced the NPE switched off while they buy nothing."""

    def app_gradle(self):
        return (ROOT / "app/build.gradle").read_text(encoding="utf-8")

    def layouts_using_data_binding(self):
        found = []
        for path in RES.glob("layout*/*.xml"):
            text = path.read_text(encoding="utf-8")
            if ET.parse(path).getroot().tag == "layout" or "<data>" in text or "<variable" in text:
                found.append(relative(path))
        return found

    def test_data_binding_matches_actual_usage(self):
        # Bidirectional on purpose: enabling data binding for nothing reintroduces the fragile
        # parser, and adding a <layout> root without enabling it fails to compile.
        enabled = re.search(r"^\s*dataBinding[\s=]+true", self.app_gradle(), re.MULTILINE)
        used = self.layouts_using_data_binding()
        if used:
            self.assertTrue(enabled,
                            f"these layouts use data binding: {used}. Enable it in "
                            f"app/build.gradle with buildFeatures {{ dataBinding true }}.")
        else:
            self.assertFalse(enabled,
                             "dataBinding is enabled but no layout has a <layout> root, a <data> "
                             "block or an @{...} expression. It only adds a StAX pass over "
                             "res/layout that reports faults as a bare NullPointerException.")

    def test_no_data_binding_helpers_in_java(self):
        java = "\n".join(path.read_text(encoding="utf-8")
                         for path in (MAIN / "java").rglob("*.java"))
        for symbol in ("DataBindingUtil", "androidx.databinding", "android.databinding"):
            self.assertNotIn(symbol, java,
                             f"{symbol} needs data binding enabled in app/build.gradle")

    def test_jetifier_stays_off_while_nothing_needs_it(self):
        properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
        self.assertIn("android.enableJetifier=false", properties,
                      "Jetifier re-parses third-party XML with StAX and reports a missing "
                      "attribute as a bare NullPointerException. Run ./gradlew checkJetifier "
                      "before turning it back on.")
        self.assertIn("android.useAndroidX=true", properties)

    def test_no_legacy_support_library_dependency(self):
        # The only reason to re-enable Jetifier would be a dependency that still ships
        # android.support.*; assert none crept into the build file.
        for line in self.app_gradle().splitlines():
            stripped = line.strip()
            if stripped.startswith(("implementation", "api ", "compile ")):
                self.assertNotIn("com.android.support", stripped,
                                 "a legacy support-library dependency would need Jetifier again")

    def test_only_the_file_provider_key_mentions_android_support(self):
        # android.support.FILE_PROVIDER_PATHS is a literal metadata key, not a class reference:
        # it must survive verbatim, which is exactly what Jetifier used to warn about.
        manifest = MANIFEST.read_text(encoding="utf-8")
        mentions = re.findall(r"android\.support\.[\w.]+", manifest)
        self.assertEqual(set(mentions), {"android.support.FILE_PROVIDER_PATHS"},
                         "unexpected android.support reference in the manifest")

    def test_dead_jcenter_repository_is_not_used(self):
        # JFrog sunset jcenter; keeping it made every resolution wait on a doomed request.
        build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        for line in build.splitlines():
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            self.assertNotIn("jcenter", stripped, "jcenter is shut down; use mavenCentral()")


if __name__ == "__main__":
    unittest.main()
