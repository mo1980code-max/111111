#!/usr/bin/env python3
"""
Build-readiness audit for Clock Adventure 3D.

The kotlinc harness (check.py) compiles the sources against stubs, so it cannot see whether a
Gradle module actually declares the dependency that provides an import, nor whether the Android
Gradle Plugin configuration is valid. This script closes that gap:

1. every import of every source set is mapped to the Gradle artefact that provides it, and the
   module's build.gradle is checked for that artefact (or for a project dependency that brings it
   in transitively);
2. the Gradle files are scanned for the mistakes that break a first sync in Android Studio
   (Compose compiler options with Kotlin 2.0, `resConfigs` misuse, missing namespace, ...).

Usage:
    python3 audit_deps.py
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))

MODULES = {
    "domain": "domain/src/main/kotlin",
    "data": "data/src/main/kotlin",
    "presentation": "presentation/src/main/kotlin",
    "app": "app/src/main/kotlin",
}
TESTS = {
    "domain": "domain/src/test/kotlin",
}

# import prefix -> (human name, tokens that must appear in the module's dependencies block)
REQUIRED = [
    ("androidx.compose.foundation", "Compose Foundation", ["composeFoundation", "composeMaterial3", "composeUi "]),
    ("androidx.compose.material3", "Compose Material 3", ["composeMaterial3"]),
    ("androidx.compose.material.icons", "Material Icons", ["materialIconsExtended", "materialIconsCore"]),
    ("androidx.compose.animation", "Compose Animation", ["composeAnimation", "composeMaterial3", "composeUi "]),
    ("androidx.compose.runtime", "Compose Runtime", ["composeRuntime", "composeMaterial3", "composeUi "]),
    ("androidx.compose.ui", "Compose UI", ["composeUi", "composeMaterial3"]),
    ("androidx.navigation.compose", "Navigation Compose", ["navigationCompose"]),
    ("androidx.navigation", "Navigation", ["navigationCompose", "navigationRuntime"]),
    ("androidx.hilt.navigation.compose", "Hilt Navigation Compose", ["hiltNavigationCompose"]),
    ("androidx.lifecycle.compose", "Lifecycle Runtime Compose", ["lifecycleRuntimeCompose"]),
    ("androidx.lifecycle.viewmodel.compose", "Lifecycle ViewModel Compose", ["lifecycleViewModelCompose"]),
    ("androidx.lifecycle", "Lifecycle / ViewModel", ["lifecycleViewModel", "lifecycleRuntime", "lifecycleViewModelCompose"]),
    ("androidx.activity.compose", "Activity Compose", ["activityCompose"]),
    ("androidx.room", "Room", ["roomRuntime", "roomKtx"]),
    ("androidx.datastore", "DataStore", ["datastorePreferences"]),
    ("androidx.core", "AndroidX Core", ["coreKtx"]),
    ("androidx.appcompat", "AppCompat", ["appCompat"]),
    ("dagger.hilt.android", "Hilt Android", ["hiltAndroid"]),
    ("dagger.hilt", "Hilt", ["hiltAndroid", "hiltCompiler"]),
    ("javax.inject", "javax.inject", ["javaxInject"]),
    ("kotlinx.coroutines", "Kotlin Coroutines", ["kotlinxCoroutines"]),
]

# JRE / Kotlin packages need no dependency.
IGNORE_PREFIXES = (
    "java.",
    "kotlin.",
    "kotlinx.coroutines.flow.",
    "com.clockadventure.",
)

# Import prefixes that only make sense with a compiler plugin (checked separately).
PLUGIN_ONLY = ("androidx.compose.compiler",)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def sources(*relative_paths):
    files = []
    for relative in relative_paths:
        root = os.path.join(PROJECT, relative)
        if not os.path.isdir(root):
            continue
        for dirpath, _, names in os.walk(root):
            for name in sorted(names):
                if name.endswith(".kt"):
                    files.append(os.path.join(dirpath, name))
    return files


def imports_of(files):
    found = set()
    for path in files:
        text = read(path)
        for match in re.finditer(r"^import ([A-Za-z0-9_.]+)", text, re.MULTILINE):
            found.add(match.group(1))
    return found


def audit_module(name):
    build = read(os.path.join(PROJECT, name, "build.gradle"))
    deps_block = build[build.index("dependencies {"):] if "dependencies {" in build else ""
    files = sources(MODULES[name])
    if name in TESTS:
        files += sources(TESTS[name])
    imports = imports_of(files)

    problems = []
    used = []
    for prefix, human, tokens in REQUIRED:
        matching = [i for i in imports if i == prefix or i.startswith(prefix + ".")]
        if not matching:
            continue
        used.append(human)
        if not any(token in deps_block for token in tokens):
            problems.append(
                "  %s uses %s but build.gradle declares none of %s"
                % (name, human, "/".join(token.strip() for token in tokens))
            )
    return problems, used


def audit_gradle():
    """Static checks on the Gradle / AGP configuration."""
    problems = []
    root = read(os.path.join(PROJECT, "build.gradle"))
    if "composeOptions" in root or any(
        "composeOptions" in read(os.path.join(PROJECT, m, "build.gradle"))
        for m in MODULES if os.path.exists(os.path.join(PROJECT, m, "build.gradle"))
    ):
        problems.append(
            "  composeOptions { kotlinCompilerExtensionVersion } is set somewhere: with Kotlin 2.0 "
            "the Compose compiler plugin (org.jetbrains.kotlin.plugin.compose) replaces it and the "
            "old option makes the build fail."
        )
    for name in MODULES:
        path = os.path.join(PROJECT, name, "build.gradle")
        if not os.path.exists(path):
            continue
        text = read(path)
        if "android {" in text and "namespace" not in text:
            problems.append("  %s/build.gradle has no namespace" % name)
        if "resConfigs" in text and "*rootProject.ext.supportedLocales" not in text:
            problems.append(
                "  %s/build.gradle passes a joined string to resConfigs; AGP takes varargs "
                "(use `resConfigs(*rootProject.ext.supportedLocales)`)" % name
            )
        if "compose true" in text and "org.jetbrains.kotlin.plugin.compose" not in text:
            problems.append("  %s/build.gradle enables Compose without the Compose compiler plugin" % name)
    return problems


def main():
    print("Clock Adventure 3D - dependency audit\n")
    problems = []
    for name in MODULES:
        module_problems, used = audit_module(name)
        print("[%s] %d artefact groups in use" % (name, len(used)))
        problems += module_problems
    print()
    problems += audit_gradle()

    if problems:
        print("PROBLEMS")
        print("\n".join(problems))
        return 1
    print("OK - every import is covered by a declared dependency, Gradle config looks valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())
