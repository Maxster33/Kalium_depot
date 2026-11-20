#!/bin/sh
# Compile KS_Boussole (ECJ, cible Java 21 : tourne sur Java 21+ / 25) et assemble le .jar
# Outils : <racine du depot>/outils-build si present (PC local, ignore par git), sinon /tmp/claude-0 (espace cloud).
# Sortie : <racine du depot>/sortie (PC local, ignore par git), sinon /mnt/user-data/outputs (espace cloud).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.1.0
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
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
# KLM_Menu (menus en coffre) et KS_BiomeChanger (Changeur de Biome du craft) : classes deja compilees par leur
# build.sh, servent seulement a compiler, jamais embarquees.
[ -d "$TOOLS/classes/KLM_Menu" ] || sh "$DIR/../KLM_Menu/build.sh" > /dev/null
[ -d "$TOOLS/classes/KS_BiomeChanger" ] || sh "$DIR/../KS_BiomeChanger/build.sh" > /dev/null
CP="$(win "$TOOLS/classes/KLM_Menu")$SEP$(win "$TOOLS/classes/KS_BiomeChanger")$SEP"
for j in "$TOOLS"/libs/*.jar; do CP="$CP$(win "$j")$SEP"; done
OUT="$TOOLS/classes/KS_Boussole"
rm -rf "$OUT" && mkdir -p "$OUT" "$DEST"
java -jar "$(win "$TOOLS/ecj.jar")" -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$CP" -d "$(win "$OUT")" src/main/java
cp src/main/resources/config.yml src/main/resources/biomes.yml src/main/resources/familles.yml "$OUT/"
sed "s/\${project.version}/$VERSION/" src/main/resources/plugin.yml > "$OUT/plugin.yml"
jar cf "$DEST/KS_Boussole-$VERSION.jar" -C "$OUT" .
echo "OK -> $DEST/KS_Boussole-$VERSION.jar"
