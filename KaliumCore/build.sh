#!/bin/sh
# Compile KaliumCore (ECJ, cible Java 21 : tourne sur Java 21+ / 25) et assemble le .jar
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.4.0
OUT=/tmp/claude-0/kcclasses
rm -rf "$OUT" && mkdir -p "$OUT"
java -jar /tmp/claude-0/ecj.jar -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$(ls /tmp/claude-0/libs/*.jar | tr '\n' ':')" -d "$OUT" src/main/java
cp src/main/resources/config.yml "$OUT/"
cp src/main/resources/item_names.tsv "$OUT/"
sed "s/\${project.version}/$VERSION/" src/main/resources/plugin.yml > "$OUT/plugin.yml"
mkdir -p /mnt/user-data/outputs
jar cf "/mnt/user-data/outputs/KaliumCore-$VERSION.jar" -C "$OUT" .
echo "OK -> /mnt/user-data/outputs/KaliumCore-$VERSION.jar"
