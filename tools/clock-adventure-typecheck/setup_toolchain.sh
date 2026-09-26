#!/usr/bin/env bash
# Restores the Kotlin compiler and the JDK this harness needs.
#
# The sandbox starts empty every session (and /tmp is wiped between turns), so both are fetched on
# demand: the JDK from PyPI (jdk4py) and the Kotlin compiler from npm (the JetBrains release
# binaries on GitHub are not reachable from here).
#
# Usage:  bash tools/clock-adventure-typecheck/setup_toolchain.sh
set -euo pipefail

KOTLINC_VERSION="${KOTLINC_VERSION:-2.4.20}"
JDK4PY_VERSION="${JDK4PY_VERSION:-17.0.9.2}"

echo "==> JDK (jdk4py ${JDK4PY_VERSION})"
python3 -c "import jdk4py" 2>/dev/null || pip install --break-system-packages --quiet "jdk4py==${JDK4PY_VERSION}"
python3 - <<'PY'
import jdk4py, pathlib
print("    java:", pathlib.Path(jdk4py.JAVA_HOME) / "bin" / "java")
PY

echo "==> Kotlin compiler (${KOTLINC_VERSION})"
if [ ! -x /tmp/kotlinc/package/bin/kotlinc ]; then
  rm -rf /tmp/kotlinc
  mkdir -p /tmp/kotlinc
  ( cd /tmp/kotlinc && npm pack "kotlin-compiler@${KOTLINC_VERSION}" >/dev/null && tar -xzf kotlin-compiler-*.tgz )
  chmod +x /tmp/kotlinc/package/bin/kotlinc
fi
/tmp/kotlinc/package/bin/kotlinc -version 2>/dev/null | head -1

echo
echo "Ready. Now run:"
echo "  python3 tools/clock-adventure-typecheck/check.py"
echo "  python3 tools/clock-adventure-typecheck/run_tests.py"
echo "  python3 tools/clock-adventure-typecheck/audit.py"
