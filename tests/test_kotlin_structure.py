"""Structural checks over the Kotlin sources.

No JDK is available in this workspace, so the compiler cannot run here. These checks cover the
mistakes that would otherwise only surface at compile time and that a careful reader can verify
mechanically: package/directory mismatches, unbalanced delimiters, imports that point at symbols
which do not exist, and project symbols used in a file that was never given the import.

Run with::

    python3 -m unittest discover -s tests -v
"""
import os
import re
import unittest

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(REPO, "app", "src", "main")
JAVA = os.path.join(MAIN, "java")
PACKAGE = "com.clock.livewallpaper"

DECLARATION = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*"
    r"(?:public\s+|internal\s+|private\s+|abstract\s+|open\s+|sealed\s+|data\s+|enum\s+|value\s+|inline\s+|expect\s+)*"
    r"(class|interface|object|fun|val|var|annotation class|typealias)\s+([A-Za-z_]\w*)",
    re.M,
)


def kotlin_files():
    for root, _dirs, files in os.walk(JAVA):
        for name in sorted(files):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def strip_comments(source):
    """Removes only comments, keeping code and string templates intact."""
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


def strip_code(source):
    """Removes comments, string literals and char literals so delimiters can be counted."""
    out = []
    index = 0
    length = len(source)
    while index < length:
        char = source[index]
        two = source[index:index + 2]
        three = source[index:index + 3]
        if three == '"""':
            end = source.find('"""', index + 3)
            index = length if end == -1 else end + 3
            continue
        if two == "//":
            end = source.find("\n", index)
            index = length if end == -1 else end
            continue
        if two == "/*":
            end = source.find("*/", index + 2)
            index = length if end == -1 else end + 2
            continue
        if char == '"':
            index += 1
            while index < length:
                if source[index] == "\\":
                    index += 2
                    continue
                if source[index] == '"':
                    index += 1
                    break
                index += 1
            continue
        if char == "'":
            index += 1
            while index < length:
                if source[index] == "\\":
                    index += 2
                    continue
                if source[index] == "'":
                    index += 1
                    break
                index += 1
            continue
        out.append(char)
        index += 1
    return "".join(out)


def package_of(path):
    source = read(path)
    match = re.search(r"^package\s+([\w.]+)", source, re.M)
    return match.group(1) if match else None


def top_level_declarations(source):
    """Names declared at the top level of a file (indent 0)."""
    names = set()
    for line in source.splitlines():
        if line.startswith((" ", "\t")):
            continue
        match = DECLARATION.match(line)
        if match:
            names.add(match.group(2))
        # enum entries and sealed members are not top level, on purpose
    return names


class ProjectIndex:
    """Where every project symbol lives."""

    def __init__(self):
        self.symbols = {}            # name -> set(packages)
        self.package_symbols = {}    # package -> set(names)
        self.file_package = {}       # path -> package
        self.sources = {}            # path -> source
        for path in kotlin_files():
            source = read(path)
            package = package_of(path)
            self.sources[path] = source
            self.file_package[path] = package
            names = top_level_declarations(strip_code(source))
            self.package_symbols.setdefault(package, set()).update(names)
            for name in names:
                self.symbols.setdefault(name, set()).add(package)


INDEX = ProjectIndex()


class PackageLayoutTest(unittest.TestCase):

    def test_package_matches_directory(self):
        mismatches = []
        for path, package in INDEX.file_package.items():
            expected = os.path.dirname(os.path.relpath(path, JAVA)).replace(os.sep, ".")
            if package != expected:
                mismatches.append((os.path.relpath(path, REPO), package, expected))
        self.assertEqual([], mismatches, f"package / directory mismatch: {mismatches}")

    def test_single_package_root(self):
        roots = {p.split(".")[0] + "." + p.split(".")[1] for p in INDEX.file_package.values()}
        self.assertEqual({"com.clock"}, roots)


class DelimiterBalanceTest(unittest.TestCase):

    def test_braces_and_parentheses_balance(self):
        offenders = []
        for path, source in INDEX.sources.items():
            code = strip_code(source)
            for opening, closing in (("{", "}"), ("(", ")"), ("[", "]")):
                if code.count(opening) != code.count(closing):
                    offenders.append(
                        (
                            os.path.relpath(path, REPO),
                            f"{opening}{closing}",
                            code.count(opening),
                            code.count(closing),
                        )
                    )
        self.assertEqual([], offenders, f"unbalanced delimiters: {offenders}")

    def test_no_tabs_and_no_trailing_whitespace(self):
        offenders = []
        for path, source in INDEX.sources.items():
            for number, line in enumerate(source.splitlines(), start=1):
                if "\t" in line or line != line.rstrip():
                    offenders.append((os.path.relpath(path, REPO), number))
        self.assertEqual([], offenders, f"whitespace problems: {offenders}")


class ImportResolutionTest(unittest.TestCase):

    def test_project_imports_resolve(self):
        """``import com.clock.livewallpaper.x.Y`` must point at a real declaration."""
        missing = []
        for path, source in INDEX.sources.items():
            for line in source.splitlines():
                match = re.match(rf"import ({re.escape(PACKAGE)}[\w.]*)", line)
                if not match:
                    continue
                target = match.group(1)
                package, _dot, name = target.rpartition(".")
                if name == "*":
                    if package not in INDEX.package_symbols:
                        missing.append((os.path.relpath(path, REPO), target))
                    continue
                if name in INDEX.package_symbols.get(package, set()):
                    continue
                # nested symbol: com.x.Class.Member
                outer_package, _dot2, outer = package.rpartition(".")
                if outer in INDEX.package_symbols.get(outer_package, set()):
                    continue
                # generated symbols (R, BuildConfig) live in the application package
                if package == PACKAGE and name in {"R", "BuildConfig"}:
                    continue
                missing.append((os.path.relpath(path, REPO), target))
        self.assertEqual([], missing, f"imports that resolve to nothing: {missing}")

    def test_library_symbols_are_imported(self):
        """A library type used without its import (the classic `LocalContext` slip) fails here.

        The index is built from the imports the project already uses, so it only knows about
        libraries this codebase actually depends on - which is exactly the risky set.
        """
        index = {}
        for path in kotlin_files():
            for match in re.finditer(r"^import\s+([\w.]+)$", read(path), re.M):
                fq = match.group(1)
                name = fq.rsplit(".", 1)[-1]
                if name[:1].isupper() and not fq.startswith(PACKAGE):
                    index.setdefault(name, set()).add(fq)

        offenders = []
        for path in kotlin_files():
            code = strip_comments(read(path))
            imported = {
                (m.group(2) or m.group(1).rsplit(".", 1)[-1])
                for m in re.finditer(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?$", code, re.M)
            }
            body = "\n".join(
                line for line in code.splitlines()
                if not line.startswith("import ") and not line.startswith("package ")
            )
            declared = set(re.findall(r"\b(?:class|object|interface|typealias)\s+(\w+)", body))
            declared |= set(re.findall(r"\bfun\s+(\w+)\(", body))
            declared |= set(re.findall(r"\bval\s+(\w+)\b", body))
            for name in sorted(index):
                if name in imported or name in declared:
                    continue
                # Not preceded by a dot: `Role.Button` is a member access, not a use of `Button`.
                if re.search(r"(?<![\w.])%s\b" % re.escape(name), body):
                    offenders.append((os.path.relpath(path, REPO), name))
        self.assertEqual([], offenders, f"used without an import: {offenders}")

    def test_no_unused_imports(self):
        """An import nobody uses is dead weight and hides a real dependency."""
        # `by remember { ... }` resolves getValue / setValue implicitly.
        implicit = {"getValue", "setValue", "provideDelegate"}
        offenders = []
        for path in kotlin_files():
            code = strip_comments(read(path))
            lines = code.splitlines()
            body = "\n".join(
                line for line in lines
                if not line.startswith("import ") and not line.startswith("package ")
            )
            for line in lines:
                match = re.match(r"import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$", line)
                if not match:
                    continue
                name = match.group(2) or match.group(1).rsplit(".", 1)[-1]
                if name == "*" or name in implicit:
                    continue
                if not re.search(r"\b%s\b" % re.escape(name), body):
                    offenders.append((os.path.relpath(path, REPO), match.group(1)))
        self.assertEqual([], offenders, f"unused imports: {offenders}")

    def test_no_duplicate_imports(self):
        offenders = []
        for path, source in INDEX.sources.items():
            imports = [line for line in source.splitlines() if line.startswith("import ")]
            duplicates = {line for line in imports if imports.count(line) > 1}
            if duplicates:
                offenders.append((os.path.relpath(path, REPO), sorted(duplicates)))
        self.assertEqual([], offenders, f"duplicate imports: {offenders}")


class SymbolVisibilityTest(unittest.TestCase):
    """A project symbol used in a file must be visible there: same package, or imported."""

    def test_used_project_symbols_are_imported(self):
        unresolved = []
        for path, source in INDEX.sources.items():
            package = INDEX.file_package[path]
            code = strip_code(source)
            body = code[code.index("\n", code.rfind("\nimport ")) + 1:] if "\nimport " in code else code

            imported = set()
            wildcard_packages = set()
            for line in code.splitlines():
                match = re.match(r"import ([\w.]+)(?:\s+as\s+(\w+))?$", line.strip())
                if not match:
                    continue
                target, alias = match.group(1), match.group(2)
                if target.endswith(".*"):
                    wildcard_packages.add(target[:-2])
                else:
                    imported.add(alias or target.rpartition(".")[2])

            local = INDEX.package_symbols.get(package, set())
            visible = set(local) | imported
            for wildcard in wildcard_packages:
                visible |= INDEX.package_symbols.get(wildcard, set())

            used = set(re.findall(r"\b([A-Za-z_]\w*)\b", body))
            for name, packages in INDEX.symbols.items():
                if name in visible or name not in used:
                    continue
                # fully qualified usage is fine
                if any(f"{pkg}.{name}" in code for pkg in packages):
                    continue
                unresolved.append((os.path.relpath(path, REPO), name))
        self.assertEqual([], unresolved, f"project symbols used without an import: {unresolved}")


class HiltContractTest(unittest.TestCase):

    def test_hilt_viewmodels_use_inject_constructor(self):
        offenders = []
        for path, source in INDEX.sources.items():
            if "@HiltViewModel" not in source:
                continue
            if "@Inject constructor" not in source:
                offenders.append(os.path.relpath(path, REPO))
        self.assertEqual([], offenders, f"@HiltViewModel without @Inject constructor: {offenders}")

    def test_injected_components_are_annotated(self):
        """A receiver that injects dependencies must carry @AndroidEntryPoint."""
        offenders = []
        for path, source in INDEX.sources.items():
            if re.search(r"class\s+\w+\s*:\s*(BroadcastReceiver|AppWidgetProvider)", source) and \
                    "@Inject" in source and "@AndroidEntryPoint" not in source:
                offenders.append(os.path.relpath(path, REPO))
        self.assertEqual([], offenders, f"injection without @AndroidEntryPoint: {offenders}")

    def test_qualified_dependencies_are_constructor_injected(self):
        """Kotlin puts a qualifier on the property, not the field: field injection would break."""
        offenders = []
        for path, source in INDEX.sources.items():
            for match in re.finditer(r"@Inject\s+(?:@\w+\s+)*lateinit\s+var\s+(\w+)", source):
                block = source[max(0, match.start() - 120):match.start()]
                if "@ApplicationScope" in match.group(0) or "@ApplicationScope" in block:
                    offenders.append((os.path.relpath(path, REPO), match.group(1)))
        self.assertEqual([], offenders, f"qualified field injection: {offenders}")


class PlaceholderTest(unittest.TestCase):

    FORBIDDEN = (
        "TODO(",
        "TODO:",
        "FIXME",
        "not implemented",
        "NotImplementedError",
        "placeholder for",
        "coming soon",
        "قريبًا",
    )

    def test_no_placeholders(self):
        offenders = []
        for path, source in INDEX.sources.items():
            for marker in self.FORBIDDEN:
                if marker in source:
                    offenders.append((os.path.relpath(path, REPO), marker))
        self.assertEqual([], offenders, f"placeholder markers: {offenders}")

    def test_no_empty_function_bodies(self):
        """An empty ``fun x() { }`` would be a stub; lambdas and no-op callbacks are fine."""
        offenders = []
        empty = re.compile(r"\bfun\s+\w+\s*\([^)]*\)\s*(?::\s*[\w<>?.]+\s*)?\{\s*\}", re.S)
        for path, source in INDEX.sources.items():
            for match in empty.finditer(strip_code(source)):
                offenders.append((os.path.relpath(path, REPO), match.group(0)[:60]))
        self.assertEqual([], offenders, f"empty function bodies: {offenders}")


if __name__ == "__main__":
    unittest.main()
