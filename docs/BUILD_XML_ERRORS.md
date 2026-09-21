# The StAX `NullPointerException` build failure

The build failed with:

```
Cannot invoke "javax.xml.stream.events.Attribute.getValue()" because the return value of
"javax.xml.stream.events.StartElement.getAttributeByName(javax.xml.namespace.QName)" is null
```

No file name, no line number, no task name worth reading. This note records what that message
actually means, why it is so unhelpful, and what was changed so it stays gone.

## It is not the app's code

`javax.xml.stream` (StAX) is a **desktop JVM** API. It is not part of Android, and nothing under
`app/src/main/java` imports it:

```bash
grep -rn "javax.xml.stream\|getAttributeByName" app/src/main/java   # no matches
```

So the exception cannot come from anything running on a phone. It comes from the **build tools**
on the machine doing the compiling, while they read the project's XML.

## What the message is really saying

StAX's `getAttributeByName(...)` returns `null` when the attribute is **absent** — it does not
throw. Several parsers in the Android toolchain look an attribute up and use it in one breath,
with no null check in between. From AGP's own `aaptcompiler`:

```kotlin
// TableExtractor.kt  — res/values
val nameAttribute = element.getAttributeByName(QName("name"))

// XmlProcessor.kt    — inline <aapt:attr>
val nameAttribute = attrElement.getAttributeByName(QName("name"))
```

When the attribute is missing, the lookup yields `null` and the `.getValue()` that follows throws
`NullPointerException`. Java's helpful-NPE text then reconstructs the expression, which is why the
message reads like an internal toolchain fault rather than "you forgot `name=` in `colors.xml`".

In short: **a required attribute is missing from an XML file the build tools parse**, and the
error names neither the attribute nor the file.

### The usual culprits

| Where | What is missing |
|---|---|
| `res/values/*.xml` | `name=` on `<string>`, `<color>`, `<dimen>`, `<style>`, `<attr>`, `<item>`, … |
| `res/values/attrs.xml` | `name=`/`value=` on `<attr>`, `<enum>`, `<flag>` |
| any layout/drawable | `name=` on an inline `<aapt:attr>` |
| `AndroidManifest.xml` | `android:name` on a component, or a `<meta-data>` with no value |
| **a third-party `.aar`** | XML inside someone else's artifact, re-parsed by Jetifier |
| **`res/layout/*`** | re-parsed by the data binding compiler when data binding is on |

The last two are the cruel ones: the offending file is either not yours or not obviously
implicated, so searching your own `res/` turns up nothing.

## What was wrong here

Every XML file in this repository is well-formed and every required attribute is present — the
checked-in `res/` was never the cause. What the project did carry was **two build passes that
re-parse XML with StAX while buying nothing**, each able to raise this exact exception:

1. **`dataBinding true` with no data binding in the project.** No layout had a `<layout>` root, a
   `<data>`/`<variable>` block or an `@{...}` expression, and no Java file referenced
   `DataBindingUtil`, `BindingAdapter` or `BaseObservable`. The feature still ran its layout
   parser over all 24 layouts on every build.
2. **`android.enableJetifier=true` with nothing to jetify.** Jetifier unzips every dependency and
   rewrites `android.support.*` to `androidx.*` **inside** the artifacts, re-parsing their XML as
   it goes. Every dependency here is already AndroidX (`com.github.QuadFlask:colorpicker` resolves
   to `androidx.appcompat:appcompat`), and the only `android.support` string in the project is the
   `FILE_PROVIDER_PATHS` meta-data **key**, which is a literal that must not be rewritten.

A third item was removed at the same time: the `jcenter()` repository. JFrog sunset it, so it
only added a doomed network round trip to dependency resolution.

## The changes

| File | Change |
|---|---|
| `app/build.gradle` | dropped `buildFeatures { dataBinding true }` |
| `gradle.properties` | `android.enableJetifier=true` → `false` |
| `build.gradle` | removed the dead `jcenter()` repository |
| `tests/test_resource_xml_contracts.py` | new — 22 checks that fail with a **file name** |

Each edit is commented in place with the reasoning and the condition for reverting it.

## The real fix: a readable error

Turning the passes off removes two ways to trigger the crash, but AAPT2 still parses `res/` and a
genuinely malformed resource would still produce the same opaque NPE. So the same
required-attribute rules are now asserted in Python, which needs no JDK or Android SDK and matches
the rest of `tests/`:

```bash
python3 -m unittest discover -s tests -v
```

Instead of a bare stack trace, a fault now reads:

```
app/src/main/res/values/colors.xml: <color> has no name= attribute. AAPT2 reads this with
getAttributeByName(QName("name")) and throws a bare NullPointerException when it is missing.
```

The suite covers well-formedness, BOM/encoding, `name=` on every values entry, style items,
styleable `<attr>`/`<enum>`/`<flag>`, plurals `quantity=`, duplicate resource names, inline
`<aapt:attr>`, `<include>`/`<fragment>`/`<view>`, undeclared namespace prefixes, and manifest
`android:name`/`<meta-data>` completeness.

The configuration guards are deliberately **two-way**: `test_data_binding_matches_actual_usage`
fails if data binding is switched on while unused *and* if a layout gains a `<layout>` root while
it is off. Re-enabling either pass on purpose is fine — the test tells you what to change.

### Verified by injecting real faults

| Injected fault | Result |
|---|---|
| `<color>` with no `name=` | caught, names `colors.xml` |
| `<attr>` with no `name=` | caught, names `attrs.xml` and `CustomAnalogClock` |
| undeclared `bogus:` prefix in a layout | caught, names `item_surah.xml` |
| `dataBinding true` restored while unused | caught, explains why |
| `<layout>` root added while data binding off | caught, says to enable it |

## If it comes back

1. **Run the tests first** — `python3 -m unittest discover -s tests -v`. If one fails, it names
   the file and the missing attribute; fix that and you are done.
2. **If the tests pass, the bad XML is not yours.** Get the real stack trace with
   `./gradlew assembleDebug --stacktrace --info` and read the frames above the NPE: the package
   (`com.android.aaptcompiler`, `…jetifier…`, `…databinding…`) tells you which pass is at fault.
3. **Suspect a dependency.** Clear its cached transform and re-resolve:
   `./gradlew --stop && rm -rf ~/.gradle/caches/transforms-* && ./gradlew assembleDebug`.
4. **Before re-enabling Jetifier**, run `./gradlew checkJetifier`. It walks the dependency graph
   and names anything still using the legacy support library. If it names nothing, leave it off.
