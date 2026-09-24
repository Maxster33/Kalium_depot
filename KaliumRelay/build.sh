#!/bin/sh
# Compile KaliumRelay (ECJ, cible Java 21 - c'est un plugin VELOCITY, pas Paper, donc pas de
# dependance sur paper-api) et assemble le .jar. Meme convention que KalGames/KalBingo/
# KaliumCore/KaliumMenu, mais avec ses propres jars de dependances (libs/ local a ce projet :
# velocity-api + guice, absents des libs Paper) en plus de ceux deja disponibles dans les libs
# communes (guava, slf4j-api, gson, checker-qual, jspecify - communs a Paper et Velocity).
# Outils : <racine du depot>/outils-build si present (PC local, ignore par git), sinon /tmp/claude-0 (espace cloud).
# Sortie : <racine du depot>/sortie (PC local, ignore par git), sinon /mnt/user-data/outputs (espace cloud).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.1.0
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -d "$DIR/../outils-build" ]; then
  TOOLS="$DIR/../outils-build"; DEST="$DIR/../sortie"
else
  TOOLS=/tmp/claude-0; DEST=/mnt/user-data/outputs
fi
# Sous Windows (Git Bash), java attend des chemins Windows separes par ";".
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';'; win() { cygpath -w "$1"; } ;;
  *) SEP=':'; win() { printf '%s' "$1"; } ;;
esac
CP=""
for j in "$DIR"/libs/*.jar "$TOOLS"/libs/guava-*.jar "$TOOLS"/libs/slf4j-api-*.jar "$TOOLS"/libs/gson-*.jar \
    "$TOOLS"/libs/checker-qual-*.jar "$TOOLS"/libs/jspecify-*.jar "$TOOLS"/libs/failureaccess-*.jar; do
  CP="$CP$(win "$j")$SEP"
done
OUT="$TOOLS/classes/KaliumRelay"
rm -rf "$OUT" && mkdir -p "$OUT" "$DEST"
java -jar "$(win "$TOOLS/ecj.jar")" -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$CP" -d "$(win "$OUT")" "$(win "$DIR/src/main/java")"
sed "s/\${project.version}/$VERSION/" "$DIR/src/main/resources/velocity-plugin.json" > "$OUT/velocity-plugin.json"
jar cf "$DEST/KaliumRelay-$VERSION.jar" -C "$OUT" .
echo "OK -> $DEST/KaliumRelay-$VERSION.jar"
