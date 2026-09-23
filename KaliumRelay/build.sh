#!/bin/sh
# Compile KaliumRelay (ECJ, cible Java 21 - c'est un plugin VELOCITY, pas Paper, donc pas de
# dependance sur paper-api) et assemble le .jar. Meme convention que KalGames/KalBingo/
# KaliumCore/KaliumMenu, mais avec ses propres jars de dependances (libs/ local a ce projet :
# velocity-api + javax.inject, introuvables dans /tmp/claude-0/libs qui ne contient que des
# dependances Paper) en plus de ceux deja disponibles globalement (guava, slf4j-api, gson,
# checker-qual, jspecify - communs a Paper et Velocity, memes jars reutilises).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.1.0
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT=/tmp/claude-0/krclasses
rm -rf "$OUT" && mkdir -p "$OUT"
CP="$(ls "$DIR"/libs/*.jar /tmp/claude-0/libs/guava-*.jar /tmp/claude-0/libs/slf4j-api-*.jar \
  /tmp/claude-0/libs/gson-*.jar /tmp/claude-0/libs/checker-qual-*.jar /tmp/claude-0/libs/jspecify-*.jar \
  /tmp/claude-0/libs/failureaccess-*.jar | tr '\n' ':')"
java -jar /tmp/claude-0/ecj.jar -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$CP" -d "$OUT" "$DIR/src/main/java"
sed "s/\${project.version}/$VERSION/" "$DIR/src/main/resources/velocity-plugin.json" > "$OUT/velocity-plugin.json"
mkdir -p /mnt/user-data/outputs
jar cf "/mnt/user-data/outputs/KaliumRelay-$VERSION.jar" -C "$OUT" .
echo "OK -> /mnt/user-data/outputs/KaliumRelay-$VERSION.jar"
