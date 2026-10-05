#!/usr/bin/env bash
# Builds the fix jar with just a JDK (17+); no Gradle/Loom needed.
# stubs/ holds compile-only stand-ins for the few Minecraft (intermediary) and Mixin types used;
# they are not packaged, the real classes come from the server at runtime.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build && mkdir -p build/stubs build/classes dist
javac -nowarn --release 17 -d build/stubs $(find stubs -name '*.java')
javac --release 17 -cp build/stubs -d build/classes $(find src -name '*.java')
cp res/* build/classes/
(cd build/classes && jar --create --file ../../dist/testosterone-chunkfix-1.0.0.jar fabric.mod.json testofix.mixins.json dev)
echo "Built dist/testosterone-chunkfix-1.0.0.jar"
