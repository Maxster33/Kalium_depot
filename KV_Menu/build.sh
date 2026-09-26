#!/bin/sh
# Compile KV_Menu (menus du serveur Kanvas) (ECJ, cible Java 21 : tourne sur Java 21+ / 25) et assemble le .jar
# Outils : <racine du depot>/outils-build si present (PC local, ignore par git), sinon /tmp/claude-0 (espace cloud).
# Sortie : <racine du depot>/sortie (PC local, ignore par git), sinon /mnt/user-data/outputs (espace cloud).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.3.0
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
# Depend de KLM_Menu (boite a outils des menus, catalogue) et de KV_Plots (API des plots) : compiles d'abord, jamais embarques.
sh "$DIR/../KLM_Menu/build.sh" > /dev/null
sh "$DIR/../KV_Plots/build.sh" > /dev/null
CP="$(win "$TOOLS/classes/KLM_Menu")$SEP$(win "$TOOLS/classes/KV_Plots")$SEP"
for j in "$TOOLS"/libs/*.jar; do CP="$CP$(win "$j")$SEP"; done
OUT="$TOOLS/classes/KV_Menu"
rm -rf "$OUT" && mkdir -p "$OUT" "$DEST"
java -jar "$(win "$TOOLS/ecj.jar")" -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$CP" -d "$(win "$OUT")" src/main/java
cp src/main/resources/config.yml "$OUT/"
sed "s/\${project.version}/$VERSION/" src/main/resources/plugin.yml > "$OUT/plugin.yml"
jar cf "$DEST/KV_Menu-$VERSION.jar" -C "$OUT" .
echo "OK -> $DEST/KV_Menu-$VERSION.jar"
