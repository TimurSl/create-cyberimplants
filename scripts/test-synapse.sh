#!/usr/bin/env bash
# Compiles and tests the Minecraft-free Synapse packages with plain javac + JUnit.
# Needs only a JDK 21 and (first run) access to Maven Central; does not need NeoForge or Create.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/.tools"
OUT="$ROOT/build/synapse-check"
JUNIT_VERSION=1.10.2
JUNIT_JAR="$TOOLS/junit-platform-console-standalone-$JUNIT_VERSION.jar"

mkdir -p "$TOOLS" "$OUT"
if [ ! -f "$JUNIT_JAR" ]; then
  curl -fsSL --retry 6 --retry-delay 5 --retry-all-errors -o "$JUNIT_JAR" \
    "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"
fi

# Guard the architecture rule: the Synapse VM stays free of Minecraft/NeoForge/Create imports.
if grep -rnE '^import (net\.minecraft|net\.neoforged|com\.mojang|com\.simibubi)' "$ROOT/src/main/java/dev/cyberimplants/synapse"; then
  echo "ERROR: synapse packages must not import Minecraft, NeoForge, Mojang or Create classes" >&2
  exit 1
fi

rm -rf "$OUT" && mkdir -p "$OUT"
SOURCES=$(find "$ROOT/src/main/java/dev/cyberimplants/synapse" "$ROOT/src/test/java/dev/cyberimplants/synapse" -name '*.java')
javac --release 21 -Xlint:all -Werror -cp "$JUNIT_JAR" -d "$OUT" $SOURCES
java -jar "$JUNIT_JAR" execute --class-path "$OUT" --scan-class-path "$OUT" --disable-banner --details=tree --fail-if-no-tests
