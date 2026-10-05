#!/usr/bin/env bash
# Verifies the fix with Fabric's real Mixin library against the real Testosterone class.
#   ./run.sh /path/to/testosterone-2.0.3.jar
# Runs three cases: without the fix (reproduces the leak), with the fix, and with a
# Testosterone build whose target method was renamed (the fix must fail loudly, not silently).
set -euo pipefail
cd "$(dirname "$0")"
TESTO="$(realpath "${1:?usage: ./run.sh /path/to/testosterone-2.0.3.jar}")"
FIX="$(realpath ../dist/testosterone-chunkfix-1.0.0.jar)"
MIXIN=0.17.4+mixin.0.8.7 ASM=9.8

mkdir -p lib
fetch() { [ -f "lib/$2" ] || curl -sSf --retry 3 -o "lib/$2" "https://repo1.maven.org/maven2/$1"; }
fetch "net/fabricmc/sponge-mixin/$MIXIN/sponge-mixin-$MIXIN.jar" sponge-mixin.jar
for a in asm asm-tree asm-commons asm-util asm-analysis; do fetch "org/ow2/asm/$a/$ASM/$a-$ASM.jar" "$a.jar"; done
fetch com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar guava.jar
fetch com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar failureaccess.jar
fetch com/google/code/gson/gson/2.11.0/gson-2.11.0.jar gson.jar

rm -rf build && mkdir -p build/harness build/game
javac --release 21 -nowarn -cp "lib/*" -d build/harness $(find harness-src -name '*.java') 2>&1 | grep -v '^Note:' || true
cp -r harness-res/* build/harness/
javac --release 17 -nowarn -d build/game $(find game-src -name '*.java')

run() { echo "===== $1"; java -cp "build/harness:lib/*" harness.Main "$2" 10000 build/game "$TESTO" "$FIX" "build/out/$2" 2>&1 | grep -vE '^\s+at |SpongePowered MIXIN|JAVA_TOOL_OPTIONS'; echo; }
run "WITHOUT the fix (Testosterone 2.0.3 as shipped)" nofix
run "WITH the fix" fix
run "WITH the fix, target method renamed (must fail loudly)" broken
