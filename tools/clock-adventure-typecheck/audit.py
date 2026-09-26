#!/usr/bin/env python3
"""
Runs every static audit of the Clock Adventure 3D project in one go:

    python3 audit.py

    audit_deps.py - does every Kotlin import have a matching Gradle dependency, and is the
                    Android Gradle Plugin configuration valid?
    audit_res.py  - do all resource XML files parse, and does every @type/name reference resolve?
    audit_room.py - do the tables and columns used in Room @Query strings exist on the entities?

Together with `check.py` (kotlinc type check) these cover the failure modes that would otherwise
only show up when Gradle and the Android SDK are available.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPTS = ["audit_deps.py", "audit_res.py", "audit_room.py"]


def main():
    failed = []
    for script in SCRIPTS:
        print("=" * 72)
        print(script)
        print("=" * 72)
        result = subprocess.run([sys.executable, os.path.join(HERE, script)])
        if result.returncode != 0:
            failed.append(script)
        print()
    if failed:
        print("FAILED: " + ", ".join(failed))
        return 1
    print("ALL AUDITS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
