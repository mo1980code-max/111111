#!/usr/bin/env python3
"""
Static Kotlin type-check for the Clock Adventure 3D sources.

The sandbox that generated this project has a real Kotlin compiler but no Android SDK and no
access to Google's Maven repository, so `./gradlew assembleDebug` cannot run here. Instead every
source set is compiled with kotlinc against a small stub library (stubs/) that declares the
third-party APIs the app uses. It catches everything a compiler can catch without the real
androidx artefacts: syntax, unresolved references to our own code, wrong argument counts,
nullability, generics and exhaustiveness.

Usage:
    python3 check.py                 # check every module
    python3 check.py domain          # check one module
"""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))
KOTLINC = os.environ.get("KOTLINC", "/tmp/kotlinc/package/bin/kotlinc")
JAVA_HOME = os.environ.get(
    "JAVA_HOME",
    "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime",
)

MODULES = {
    "domain": "domain/src/main/kotlin",
    "data": "data/src/main/kotlin",
    "presentation": "presentation/src/main/kotlin",
    "app": "app/src/main/kotlin",
    "tests": "domain/src/test/kotlin",
    "androidTest": "presentation/src/androidTest/kotlin",
}

# Gradle module dependencies, so each source set is compiled together with what it depends on.
DEPS = {
    "domain": ["domain"],
    "data": ["domain", "data"],
    "presentation": ["domain", "presentation"],
    "app": ["domain", "data", "presentation", "app"],
    "tests": ["domain", "tests"],
    "androidTest": ["domain", "presentation", "androidTest"],
}


def sources(*module_names):
    files = []
    stub_dir = os.path.join(HERE, "stubs")
    for root, _, names in os.walk(stub_dir):
        for name in sorted(names):
            if name.endswith(".kt"):
                files.append(os.path.join(root, name))
    for module in module_names:
        path = os.path.join(PROJECT, MODULES[module])
        if not os.path.isdir(path):
            continue
        for root, _, names in os.walk(path):
            for name in sorted(names):
                if name.endswith(".kt"):
                    files.append(os.path.join(root, name))
    return files


def check(module):
    files = sources(*DEPS[module])
    if not files:
        print(f"[{module}] no sources found - skipped")
        return True
    with tempfile.TemporaryDirectory() as out:
        cmd = [KOTLINC, "-nowarn", "-jvm-target", "17", "-d", out] + files
        env = dict(os.environ)
        env["JAVA_HOME"] = JAVA_HOME
        env["PATH"] = os.path.join(JAVA_HOME, "bin") + os.pathsep + env.get("PATH", "")
        proc = subprocess.run(cmd, capture_output=True, text=True, env=env)
        ok = proc.returncode == 0
        if ok:
            print(f"[{module}] OK  ({len(files)} files)")
        else:
            print(f"[{module}] FAILED ({len(files)} files)")
            lines = (proc.stdout + proc.stderr).splitlines()
            for line in lines[:400]:
                if line.strip():
                    print("    " + line.replace(PROJECT + "/", ""))
            if len(lines) > 400:
                print(f"    ... {len(lines) - 400} more lines")
        return ok


def main():
    targets = sys.argv[1:] or list(MODULES.keys())
    ok = True
    for module in targets:
        if module not in MODULES:
            print(f"unknown module {module!r}; expected one of {sorted(MODULES)}")
            return 2
        ok = check(module) and ok
    print("\nRESULT:", "ALL MODULES TYPE-CHECK CLEAN" if ok else "ERRORS FOUND")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
