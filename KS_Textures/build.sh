#!/bin/sh
# Compile KS_Textures (ECJ, cible Java 21 : tourne sur Java 21+ / 25) et assemble le .jar
# Outils : <racine du depot>/outils-build si present (PC local, ignore par git), sinon /tmp/claude-0 (espace cloud).
# Sortie : <racine du depot>/sortie (PC local, ignore par git), sinon /mnt/user-data/outputs (espace cloud).
# Compile contre le serveur Paper 26.3 (paquets, composants des objets) : <outils>/serveur/versions/26.3/paper-26.3.jar
# et <outils>/serveur/libraries, produits par telecharger-outils.sh (jamais embarques).
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
CP=""
for j in "$TOOLS"/libs/*.jar; do CP="$CP$(win "$j")$SEP"; done
SERVER="$TOOLS/serveur/versions/26.3/paper-26.3.jar"
if [ ! -f "$SERVER" ]; then
  echo "Serveur Paper 26.3 absent ($SERVER) : lancer sh telecharger-outils.sh a la racine du depot" >&2
  exit 1
fi
CP="$CP$(win "$SERVER")$SEP"
# (boucle ligne par ligne : le chemin du depot contient des espaces)
LIBS="$(find "$TOOLS/serveur/libraries" -name '*.jar' ! -name 'paper-api-*')"
while IFS= read -r j; do CP="$CP$(win "$j")$SEP"; done <<EOF_LIBS
$LIBS
EOF_LIBS
OUT="$TOOLS/classes/KS_Textures"
rm -rf "$OUT" && mkdir -p "$OUT" "$DEST"
java -jar "$(win "$TOOLS/ecj.jar")" -21 -proc:none -nowarn -encoding UTF-8 \
  -cp "$CP" -d "$(win "$OUT")" src/main/java
sed "s/\${project.version}/$VERSION/" src/main/resources/plugin.yml > "$OUT/plugin.yml"
cp src/main/resources/config.yml "$OUT/config.yml"
# 1.1.0 : bareme de KS_Economy (prix des objets custom, pour la couleur de rarete) : recompiler apres un changement de prix
cp ../KS_Economy/src/main/resources/rachats.csv "$OUT/rachats.csv"
jar cf "$DEST/KS_Textures-$VERSION.jar" -C "$OUT" .
echo "OK -> $DEST/KS_Textures-$VERSION.jar"
