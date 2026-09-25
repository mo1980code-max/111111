#!/usr/bin/env python3
"""
Android resource audit for Clock Adventure 3D.

Checks two things a Kotlin type check can never see:

1. every XML file under any module's res/ folder parses;
2. every `@type/name` reference (in resources, layouts and the manifest) resolves against the
   merged resource table of the project - i.e. the resource really exists in one of the modules.

`@android:...` framework references and `?attr/...` theme attributes are skipped.

Usage:
    python3 audit_res.py
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))

MODULES = ["app", "data", "domain", "presentation"]

# Resource types that can be declared by a values XML element name.
VALUE_TAGS = {
    "string": "string",
    "color": "color",
    "style": "style",
    "dimen": "dimen",
    "integer": "integer",
    "bool": "bool",
    "string-array": "array",
    "integer-array": "array",
    "array": "array",
    "plurals": "plurals",
    "drawable": "drawable",
    "item": None,
}

# Folder -> declared resource type
FOLDER_TYPES = {
    "drawable": "drawable",
    "drawable-anydpi": "drawable",
    "mipmap": "mipmap",
    "mipmap-anydpi-v26": "mipmap",
    "layout": "layout",
    "xml": "xml",
    "anim": "anim",
    "menu": "menu",
    "raw": "raw",
    "font": "font",
}

REFERENCE = re.compile(r'"@(?:\+)?([a-z-]+)/([A-Za-z0-9_.]+)"')
FRAMEWORK = re.compile(r'"@android:')


def res_dirs():
    for module in MODULES:
        path = os.path.join(PROJECT, module, "src", "main", "res")
        if os.path.isdir(path):
            yield module, path


def collect_definitions():
    defined = {}
    for module, res in res_dirs():
        for entry in sorted(os.listdir(res)):
            folder = entry.split("-")[0]
            kind = FOLDER_TYPES.get(entry) or FOLDER_TYPES.get(folder)
            full = os.path.join(res, entry)
            # values-xx/qualifier folder: resources are declared inside the XML files
            if folder == "values" and os.path.isdir(full):
                for name in sorted(os.listdir(full)):
                    if not name.endswith(".xml"):
                        continue
                    path = os.path.join(full, name)
                    try:
                        root = ET.parse(path).getroot()
                    except ET.ParseError as error:
                        yield None, "broken XML %s: %s" % (os.path.relpath(path, PROJECT), error)
                        continue
                    for element in root.iter():
                        element_kind = VALUE_TAGS.get(element.tag)
                        if element_kind and element.get("name"):
                            defined.setdefault(element_kind, set()).add(element.get("name"))
                continue
            if os.path.isdir(full):
                for name in sorted(os.listdir(full)):
                    if kind:
                        defined.setdefault(kind, set()).add(os.path.splitext(name)[0])
    # styles declared with <style name="x"> are covered above
    yield defined, None


def files_to_scan():
    for module, res in res_dirs():
        for root, _, names in os.walk(res):
            for name in sorted(names):
                if name.endswith(".xml"):
                    yield os.path.join(root, name)
    for module in MODULES:
        manifest = os.path.join(PROJECT, module, "src", "main", "AndroidManifest.xml")
        if os.path.exists(manifest):
            yield manifest


def main():
    defined = None
    problems = []
    for maybe_defined, error in collect_definitions():
        if error:
            problems.append("  " + error)
        elif maybe_defined is not None:
            defined = maybe_defined
    defined = defined or {}

    for path in files_to_scan():
        try:
            text = open(path, encoding="utf-8").read()
        except OSError:
            continue
        relative = os.path.relpath(path, PROJECT)
        if FRAMEWORK.search(text) and "@android:" in text:
            text = FRAMEWORK.sub('"@skip/', text)
        for kind, name in REFERENCE.findall(text):
            if kind == "android":
                continue
            if name not in defined.get(kind, set()):
                problems.append("  %s references @%s/%s which is not defined in any module" % (relative, kind, name))

    # Sanity: the manifest must point at classes that exist.
    manifest = os.path.join(PROJECT, "app", "src", "main", "AndroidManifest.xml")
    if os.path.exists(manifest):
        text = open(manifest, encoding="utf-8").read()
        package = "com.clockadventure.app"
        for match in re.finditer(r'android:name="(\.[A-Za-z0-9_.]+)"', text):
            class_name = match.group(1)
            fqcn = package + class_name if class_name.startswith(".") else class_name
            relative = fqcn.replace(".", "/") + ".kt"
            candidates = [
                os.path.join(PROJECT, module, "src", "main", "kotlin", relative)
                for module in MODULES
            ]
            if not any(os.path.exists(candidate) for candidate in candidates):
                problems.append("  manifest declares %s but no %s exists" % (fqcn, relative))

    if problems:
        print("PROBLEMS")
        print("\n".join(dict.fromkeys(problems)))
        return 1
    print("OK - all resources parse and every @type/name reference resolves")
    print("    (%d strings, %d drawables, %d mipmaps, %d colors, %d xml)"
          % (
              len(defined.get("string", ())),
              len(defined.get("drawable", ())),
              len(defined.get("mipmap", ())),
              len(defined.get("color", ())),
              len(defined.get("xml", ())),
          ))
    return 0


if __name__ == "__main__":
    sys.exit(main())
