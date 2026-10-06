#!/usr/bin/env bash
# Builds the fix jar with just a JDK; no Gradle/Loom needed.
# stubs/ holds compile-only stand-ins for the few Minecraft (intermediary) and Mixin types used;
# they are not packaged, the real classes come from the server at runtime.
#
# The build is reproducible: with the same JDK major version (21, which the release workflow
# uses) you get a byte-identical jar, so its SHA-256 can be compared with the published one.
set -euo pipefail
cd "$(dirname "$0")"

JDK_MAJOR=$(javac -version 2>&1 | sed -nE 's/^javac ([0-9]+).*/\1/p')
[ "$JDK_MAJOR" = 21 ] || echo "Note: building with JDK $JDK_MAJOR; release jars are built with JDK 21, so the checksum may differ." >&2

VERSION=$(sed -nE 's/.*"version": *"([^"]+)".*/\1/p' res/fabric.mod.json)
JAR=dist/testosterone-chunkfix-$VERSION.jar
rm -rf build && mkdir -p build/stubs build/classes dist
javac -nowarn --release 17 -d build/stubs $(find stubs -name '*.java' | LC_ALL=C sort)
javac --release 17 -cp build/stubs -d build/classes $(find src -name '*.java' | LC_ALL=C sort)
cp res/* build/classes/
rm -f "$JAR"
# No manifest (it would record the JDK vendor/version), fixed entry order and fixed timestamps.
(cd build/classes && jar --create --no-manifest --date=2026-01-01T00:00:00Z --file "../../$JAR" \
	fabric.mod.json testofix.mixins.json $(find dev -name '*.class' | LC_ALL=C sort))
(cd dist && sha256sum "$(basename "$JAR")" > "$(basename "$JAR").sha256")
echo "Built $JAR"
cat "$JAR.sha256"
