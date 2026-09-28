#!/usr/bin/env python3
"""Kotlin type-check for the advertising layer of the dhikr app.

Why this exists
---------------
`./gradlew assembleDebug` cannot run in this workspace: there is no Android SDK and neither
Google's Maven repository nor Maven Central is reachable, so the real `play-services-ads`,
`user-messaging-platform` and AndroidX artifacts cannot be downloaded. A real Kotlin compiler is
available though, so the next best thing is done here: `app/src/main/java/com/clock/livewallpaper/
ads/**` is compiled with kotlinc against API stubs that declare - with the documented signatures -
the Mobile Ads SDK, the UMP SDK, the AndroidX pieces the ad layer uses and the five app
symbols it imports. `R` and `BuildConfig` are generated from `app/src/main/res` by generate_r.py.

What it proves and what it does not
-----------------------------------
It proves the ad layer is syntactically valid Kotlin, that every reference resolves, that argument
counts, nullability, generics and `override` signatures line up with the documented SDK API, and
that Compose/Hilt annotations are used on shapes the compiler accepts. It cannot prove that the
real artifacts behave as documented, and it is emphatically not a build: "type-check clean" is not
"BUILD SUCCESSFUL", and neither is evidence that an ad was ever loaded.

Usage::

    python3 tools/dhikr-typecheck/check.py

The compiler and JDK are found through the ``KOTLINC`` and ``JAVA_HOME`` environment variables;
the defaults below match the toolchain that ``tools/clock-adventure-typecheck/setup_toolchain.sh``
installs (kotlinc 2.1.20 and the jdk4py Java 17 runtime).
"""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SHARED_STUBS = os.path.join(REPO, "tools", "clock-adventure-typecheck", "stubs")
LOCAL_STUBS = os.path.join(HERE, "stubs_extra")
SOURCES = os.path.join(REPO, "app", "src", "main", "java", "com", "clock", "livewallpaper", "ads")

KOTLINC = os.environ.get("KOTLINC", "/tmp/kotlinc/package/bin/kotlinc")
JAVA_HOME = os.environ.get(
    "JAVA_HOME",
    "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime",
)

# Stubs of the sibling harness that this app replaces with a richer version of its own.
SKIP_SHARED = {"generated-r-app.kt", "generated-r-presentation.kt"}


def kotlin_files(directory):
    found = []
    for root, _dirs, names in os.walk(directory):
        for name in sorted(names):
            if name.endswith(".kt"):
                found.append(os.path.join(root, name))
    return found


def stub_files():
    local = kotlin_files(LOCAL_STUBS)
    overridden = {os.path.basename(path) for path in local} | SKIP_SHARED
    shared = [
        path for path in kotlin_files(SHARED_STUBS)
        if os.path.basename(path) not in overridden
    ]
    return shared + local


def main():
    stubs = stub_files()
    sources = kotlin_files(SOURCES)
    if not sources:
        print("no ad sources found")
        return 2

    with tempfile.TemporaryDirectory() as out:
        command = [KOTLINC, "-nowarn", "-jvm-target", "17", "-d", out] + stubs + sources
        env = dict(os.environ)
        env["JAVA_HOME"] = JAVA_HOME
        env["PATH"] = os.path.join(JAVA_HOME, "bin") + os.pathsep + env.get("PATH", "")
        result = subprocess.run(command, capture_output=True, text=True, env=env)

    output = (result.stdout + result.stderr).strip()
    errors = [line for line in output.splitlines() if "error:" in line]
    print(f"stubs: {len(stubs)} files, ad layer: {len(sources)} files")
    if errors:
        print(f"TYPE-CHECK FAILED ({len(errors)} errors)")
        for line in output.splitlines()[:200]:
            if line.strip():
                print("    " + line.replace(REPO + "/", ""))
        return 1
    print("TYPE-CHECK CLEAN (this is not a Gradle build and not a loaded ad)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
