#!/bin/sh
# Compile KaliumMenu (ECJ, cible Java 21) et assemble le .jar
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.5.0
OUT=/tmp/claude-0/kmclasses
rm -rf "$OUT" && mkdir -p "$OUT"
java -jar /tmp/claude-0/ecj.jar -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$(ls /tmp/claude-0/libs/*.jar | tr '\n' ':')" -d "$OUT" src/main/java
cp src/main/resources/config.yml "$OUT/"
sed "s/\${project.version}/$VERSION/" src/main/resources/plugin.yml > "$OUT/plugin.yml"
mkdir -p /mnt/user-data/outputs
jar cf "/mnt/user-data/outputs/KaliumMenu-$VERSION.jar" -C "$OUT" .
echo "OK -> /mnt/user-data/outputs/KaliumMenu-$VERSION.jar"
