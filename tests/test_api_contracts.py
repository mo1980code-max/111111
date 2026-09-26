"""Call-site contracts between the app's own functions.

Without a compiler, the second large class of errors after missing imports is calling one of the
project's own composables with a parameter name that does not exist, or forgetting a parameter
that has no default. These checks parse every top level function of the project and every call to
it, and verify both directions.

Run with::

    python3 -m unittest discover -s tests -v
"""
import os
import re
import unittest

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(REPO, "app", "src", "main")
JAVA = os.path.join(MAIN, "java")
PACKAGE_DIR = os.path.join(JAVA, "com", "clock", "livewallpaper")


def kotlin_files():
    for root, _dirs, files in os.walk(JAVA):
        for name in sorted(files):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def strip_comments(source):
    out = []
    index = 0
    length = len(source)
    while index < length:
        two = source[index:index + 2]
        if source[index:index + 3] == '"""':
            end = source.find('"""', index + 3)
            end = length if end == -1 else end + 3
            out.append(source[index:end])
            index = end
            continue
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


def split_arguments(source, open_index):
    """Splits an argument list whose ``(`` is at ``open_index``; returns (args, close_index)."""
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


class Signature:
    def __init__(self, name, path, params):
        self.name = name
        self.path = path
        self.params = params  # list of (name, has_default)

    @property
    def names(self):
        return [name for name, _default in self.params]

    @property
    def required(self):
        return [name for name, default in self.params if not default]


def parse_signatures():
    """All top level ``fun`` declarations of the project with their parameter lists."""
    signatures = {}
    duplicates = set()
    for path in kotlin_files():
        source = strip_comments(read(path))
        for match in re.finditer(r"^(?:@\w+(?:\([^)]*\))?\s*)*(?:private\s+|internal\s+)?fun\s+(?:<[^>]+>\s+)?([A-Za-z_]\w*)\s*\(", source, re.M):
            name = match.group(1)
            open_index = source.index("(", match.end() - 1)
            args, close = split_arguments(source, open_index)
            if close is None:
                continue
            params = []
            for arg in args:
                param = re.match(r"(?:vararg\s+)?(\w+)\s*:", arg.strip())
                if not param:
                    continue
                params.append((param.group(1), "=" in arg.split(":", 1)[1]))
            if name in signatures:
                duplicates.add(name)
            signatures[name] = Signature(name, path, params)
    # overloads cannot be checked by name alone
    for name in duplicates:
        signatures.pop(name, None)
    return signatures


SIGNATURES = parse_signatures()


class ComposableCallSiteTest(unittest.TestCase):

    def _call_sites(self, name):
        pattern = re.compile(rf"(?<![\w.]){re.escape(name)}\s*\(")
        for path in kotlin_files():
            source = strip_comments(read(path))
            for match in pattern.finditer(source):
                # skip the declaration itself
                prefix = source[max(0, match.start() - 40):match.start()]
                if re.search(r"\bfun\s+$", prefix) or re.search(r"\bfun\s+<[^>]+>\s+$", prefix):
                    continue
                open_index = source.index("(", match.end() - 1)
                args, close = split_arguments(source, open_index)
                if close is None:
                    continue
                trailing_lambda = bool(re.match(r"\s*\{", source[close + 1:close + 4]))
                yield path, args, trailing_lambda

    def test_named_arguments_exist(self):
        offenders = []
        for name, signature in SIGNATURES.items():
            for path, args, _lambda in self._call_sites(name):
                for arg in args:
                    named = re.match(r"^(\w+)\s*=(?!=)", arg)
                    if named and named.group(1) not in signature.names:
                        offenders.append(
                            (os.path.relpath(path, REPO), name, named.group(1))
                        )
        self.assertEqual([], offenders, f"unknown named arguments: {offenders}")

    def test_required_parameters_are_supplied(self):
        offenders = []
        for name, signature in SIGNATURES.items():
            required = signature.required
            if not required:
                continue
            for path, args, trailing_lambda in self._call_sites(name):
                supplied = set()
                positional = 0
                for arg in args:
                    named = re.match(r"^(\w+)\s*=(?!=)", arg)
                    if named:
                        supplied.add(named.group(1))
                    else:
                        if positional < len(signature.params):
                            supplied.add(signature.params[positional][0])
                        positional += 1
                if trailing_lambda and signature.params:
                    supplied.add(signature.params[-1][0])
                missing = [param for param in required if param not in supplied]
                if missing:
                    offenders.append((os.path.relpath(path, REPO), name, missing))
        self.assertEqual([], offenders, f"missing required arguments: {offenders}")


class NavigationGraphTest(unittest.TestCase):

    def setUp(self):
        self.routes = read(os.path.join(PACKAGE_DIR, "ui", "navigation", "Routes.kt"))
        self.host = read(os.path.join(PACKAGE_DIR, "ui", "navigation", "DhikrNavHost.kt"))

    def test_every_route_constant_has_a_destination(self):
        constants = re.findall(r'const val (\w+) = "([\w/{}?=]+)"', self.routes)
        patterns = {
            name for name in re.findall(r"composable\(\s*(?:route = )?Routes\.(\w+)", self.host)
        }
        skipped = {"ARG_CATEGORY", "ARG_DHIKR_ID"}
        missing = [
            name for name, _value in constants
            if name not in patterns and name not in skipped
        ]
        self.assertEqual([], missing, f"routes without a destination: {missing}")

    def test_external_routes_are_reachable(self):
        """Notification / widget deep links must all exist in the graph."""
        external = re.search(r"EXTERNAL_ROUTES[^}]+}", self.routes, re.S).group(0)
        names = set(re.findall(r"add\((\w+)\)", external))
        destinations = set(re.findall(r"composable\(\s*(?:route = )?Routes\.(\w+)", self.host))
        self.assertTrue(names)
        self.assertTrue(
            names.issubset(destinations),
            f"deep link routes missing from the graph: {sorted(names - destinations)}",
        )

    def test_reading_and_editor_declare_their_arguments(self):
        for pattern, argument in (
            ("READING_PATTERN", "ARG_CATEGORY"),
            ("EDITOR_PATTERN", "ARG_DHIKR_ID"),
        ):
            block = re.search(
                rf"composable\(\s*route = Routes\.{pattern}.*?\)\s*\{{", self.host, re.S
            )
            self.assertIsNotNone(block, f"{pattern} has no destination")
            self.assertIn(f"navArgument(Routes.{argument})", block.group(0))

    def test_bottom_bar_matches_bottom_routes(self):
        bar = read(os.path.join(PACKAGE_DIR, "ui", "navigation", "BottomBar.kt"))
        tabs = set(re.findall(r"Routes\.(\w+),\s*R\.string", bar))
        declared = set(
            re.findall(r"\w+", re.search(r"BOTTOM_ROUTES[^\n]+listOf\(([^)]*)\)", self.routes).group(1))
        )
        self.assertEqual({tab.lower() for tab in tabs}, {name.lower() for name in declared})


class ScreenWiringTest(unittest.TestCase):
    """Each screen observes its ViewModel with a lifecycle aware collector."""

    SCREENS = [
        ("home", "HomeScreen"),
        ("adhkar", "AdhkarScreen"),
        ("reading", "ReadingScreen"),
        ("tasbeeh", "TasbeehScreen"),
        ("mydhikr", "MyDhikrScreen"),
        ("editor", "DhikrEditorScreen"),
        ("settings", "SettingsScreen"),
        ("overlay", "OverlaySettingsScreen"),
    ]

    def test_screens_collect_state_with_lifecycle(self):
        offenders = []
        for folder, screen in self.SCREENS:
            path = os.path.join(PACKAGE_DIR, "ui", "screens", folder, f"{screen}.kt")
            source = read(path)
            if "collectAsStateWithLifecycle()" not in source:
                offenders.append(screen)
        self.assertEqual([], offenders, f"screens not using collectAsStateWithLifecycle: {offenders}")

    def test_screens_take_their_viewmodel_by_hilt(self):
        offenders = []
        for folder, screen in self.SCREENS:
            path = os.path.join(PACKAGE_DIR, "ui", "screens", folder, f"{screen}.kt")
            if "hiltViewModel()" not in read(path):
                offenders.append(screen)
        self.assertEqual([], offenders, f"screens without hiltViewModel(): {offenders}")

    def test_every_screen_is_composable_and_arabic_only(self):
        """No hard coded user facing latin text inside the screens."""
        offenders = []
        for folder, screen in self.SCREENS + [("onboarding", "OnboardingScreen"),
                                              ("privacy", "PrivacyScreen"),
                                              ("about", "AboutScreen")]:
            path = os.path.join(PACKAGE_DIR, "ui", "screens", folder, f"{screen}.kt")
            source = read(path)
            self.assertIn("@Composable", source)
            for literal in re.findall(r'text\s*=\s*"([^"]+)"', source):
                offenders.append((screen, literal))
        self.assertEqual([], offenders, f"hard coded text= literals: {offenders}")


if __name__ == "__main__":
    unittest.main()
