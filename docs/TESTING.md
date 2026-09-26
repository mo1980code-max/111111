# Verification

```
python3 -m unittest discover -s tests -v      # 96 checks, ~12 s, no dependencies
```

## Why a static suite

The Android toolchain cannot run in this workspace: there is no JDK and no Android SDK, and the
Gradle distribution plus Google Maven / Maven Central are unreachable from here, so
`./gradlew assembleDebug` cannot be executed and **no build result may be claimed**. On a machine
with JDK 17 and the Android SDK, the normal build is still the real gate:

```
./gradlew clean assembleDebug
```

The suite below deliberately covers the classes of mistakes a compiler *would* have caught, plus the
product rules a compiler never checks. It is written in plain `unittest`, reads the sources as text
and has no third-party dependency.

| File | Checks | What it stands in for |
|---|---|---|
| `test_resource_references.py` | 15 | `aapt2 link`: every `R.*` and `@type/name` resolves, manifest components exist, format arguments match call sites, Arabic-only copy, no dead strings or drawables, no hard-coded Arabic in Kotlin |
| `test_kotlin_structure.py` | 14 | `kotlinc` hygiene: package == directory, balanced brackets, imports resolve, are unique, are all used and none is missing, every project symbol used in a file is imported, Hilt annotations paired, no TODO / placeholder / empty function bodies |
| `test_api_contracts.py` | 9 | The type checker on our own API: 72 indexed top-level signatures, named arguments exist, required parameters are supplied, every route has a destination, every stateful screen collects with lifecycle |
| `test_product_contracts.py` | 42 | The specification: permission budget, no network / SDK / exact alarms / WorkManager, overlay dismiss-and-open-nothing, quiet hours, boot restore, widget rules, RTL, splash, accessibility, Room safety |
| `test_content_integrity.py` | 16 | The religious content: azkar.json pinned by hash, counts 31/30/17, attributions present, verbatim seeding, preserved assets |

## Non-vacuity

Audits that always pass are worthless, so the heaviest ones were checked against injected faults;
each failed as intended and the file was restored immediately after:

* deleting the `StatusPill` import from `ReadingScreen.kt` → `SymbolVisibilityTest` reported
  `('…/ReadingScreen.kt', 'StatusPill')`;
* renaming `text =` to `caption =` in one `SecondaryButton(…)` call in `AboutScreen.kt` →
  `ComposableCallSiteTest` reported both the unknown argument and the missing required one;
* deleting the `LocalContext` import from `Dialogs.kt` → the import audit reported it as used
  without an import.

## What the suite cannot do

It does not type-check generics, resolve overload sets, expand kapt-generated Hilt/Room code or run
anything on a device. Those remain for `./gradlew assembleDebug` and a real install on Android 13+
(runtime notification permission, overlay grant, widget placement, TalkBack pass).
