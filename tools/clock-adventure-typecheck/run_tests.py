#!/usr/bin/env python3
"""
Runs the domain unit tests of Clock Adventure 3D on the JVM.

The project's tests are ordinary JUnit 4 tests (checked by `check.py`, run by `./gradlew test` in
Android Studio). This sandbox has no Maven, so the real JUnit jar cannot be fetched: instead the
tests are compiled against tools/clock-adventure-typecheck/testrt/junit-runtime.kt, which provides
the same assertions with real checking, and are then executed by a small reflective runner.

Usage:
    python3 run_tests.py
"""
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))
KOTLINC = os.environ.get("KOTLINC", "/tmp/kotlinc/package/bin/kotlinc")
JAVA_HOME = os.environ.get("JAVA_HOME", "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime")
OUT = "/tmp/clock_adventure_domain_tests"

DOMAIN = os.path.join(PROJECT, "domain", "src", "main", "kotlin")
TESTS = os.path.join(PROJECT, "domain", "src", "test", "kotlin")
RUNTIME = os.path.join(HERE, "testrt")

CLASS_RE = re.compile(r"^\s*(?:internal\s+|public\s+)?class\s+(\w+)", re.MULTILINE)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+)", re.MULTILINE)


def support_stubs():
    """The domain needs javax.inject and kotlinx.coroutines; the stub library provides both.

    The stub that mimics JUnit is skipped: testrt/ replaces it with real assertions.
    """
    files = []
    stub_dir = os.path.join(HERE, "stubs")
    for name in sorted(os.listdir(stub_dir)):
        if not name.endswith(".kt"):
            continue
        path = os.path.join(stub_dir, name)
        text = open(path, encoding="utf-8").read()
        if "package org.junit" in text:
            continue
        files.append(path)
    return files


def kotlin_files(*roots):
    files = []
    for root in roots:
        for dirpath, _, names in os.walk(root):
            for name in sorted(names):
                if name.endswith(".kt"):
                    files.append(os.path.join(dirpath, name))
    return files


def test_classes():
    classes = []
    for path in kotlin_files(TESTS):
        text = open(path, encoding="utf-8").read()
        package = PACKAGE_RE.search(text)
        for name in CLASS_RE.findall(text):
            fqcn = (package.group(1) + "." + name) if package else name
            classes.append(fqcn)
    return sorted(set(classes))


def write_runner(classes):
    lines = [
        "import kotlin.system.exitProcess",
        "",
        "fun main() {",
        "    val names = listOf(",
    ]
    for fqcn in classes:
        lines.append('        "%s",' % fqcn)
    lines += [
        "    )",
        "    var passed = 0",
        "    val failures = mutableListOf<String>()",
        "    for (name in names) {",
        "        val clazz = Class.forName(name)",
        "        val methods = clazz.declaredMethods.filter {",
        "            it.isAnnotationPresent(org.junit.Test::class.java)",
        "        }.sortedBy { it.name }",
        "        for (method in methods) {",
        "            val instance = clazz.getDeclaredConstructor().newInstance()",
        "            try {",
        "                method.isAccessible = true",
        "                method.invoke(instance)",
        "                passed += 1",
        "            } catch (error: Throwable) {",
        "                val cause = error.cause ?: error",
        "                failures.add(\"${clazz.simpleName}.${method.name}: $cause\")",
        "            }",
        "        }",
        "    }",
        "    println(\"tests passed: $passed, failed: ${failures.size}\")",
        "    failures.forEach { println(\"  FAIL $it\") }",
        "    exitProcess(if (failures.isEmpty()) 0 else 1)",
        "}",
        "",
    ]
    path = os.path.join(OUT, "TestRunner.kt")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    return path


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)

    classes = test_classes()
    if not classes:
        print("no test classes found")
        return 1
    runner = write_runner(classes)

    sources = kotlin_files(DOMAIN, TESTS, RUNTIME) + support_stubs() + [runner]
    env = dict(os.environ, JAVA_HOME=JAVA_HOME)
    compile_cmd = [KOTLINC, "-nowarn", "-d", OUT] + sources
    print("compiling %d sources (%d test classes)" % (len(sources), len(classes)))
    compiled = subprocess.run(compile_cmd, capture_output=True, text=True, env=env)
    if compiled.returncode != 0:
        print((compiled.stdout + compiled.stderr)[-4000:])
        print("COMPILATION FAILED")
        return 1

    kotlin_stdlib = None
    lib_dir = os.path.join(os.path.dirname(os.path.dirname(KOTLINC)), "lib")
    for name in ("kotlin-stdlib.jar", "kotlin-stdlib-2.4.20.jar"):
        candidate = os.path.join(lib_dir, name)
        if os.path.exists(candidate):
            kotlin_stdlib = candidate
            break
    classpath = OUT + ((":" + kotlin_stdlib) if kotlin_stdlib else "")
    result = subprocess.run(
        [os.path.join(JAVA_HOME, "bin", "java"), "-cp", classpath, "TestRunnerKt"],
        capture_output=True, text=True, env=env
    )
    print(result.stdout.strip())
    if result.stderr.strip():
        print(result.stderr.strip()[-2000:])
    return result.returncode


if __name__ == "__main__":
    sys.exit(main())
