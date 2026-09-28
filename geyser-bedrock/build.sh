#!/bin/sh
# Fabrique le pack Bedrock des objets custom (pour Geyser, sur le proxy) : <racine du depot>/sortie/KaLium-objets-<version>.mcpack
# Les images viennent du jeu Java installe sur le PC (le depot est public : aucune image de Mojang n'y est copiee).
# Usage : sh geyser-bedrock/build.sh [chemin du .jar du jeu] (par defaut : version 26.2 du launcher officiel).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.0.0
DIR="$(cd "$(dirname "$0")" && pwd)"
GAME_JAR="${1:-$APPDATA/.minecraft/versions/26.2/26.2.jar}"
DEST="$DIR/../sortie"
WORK="$DIR/../sortie/geyser-pack-tmp"
rm -rf "$WORK" && mkdir -p "$WORK/textures/items" "$DEST"
cp "$DIR/pack/manifest.json" "$WORK/"
cp "$DIR/pack/textures/item_texture.json" "$WORK/textures/"
# id Bedrock -> image du jeu Java
unzip -p "$GAME_JAR" assets/minecraft/textures/item/black_bundle.png > "$WORK/textures/items/kalium_estomac_gardien.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/ominous_trial_key.png > "$WORK/textures/items/kalium_cle_de_l_end.png"
for f in "$WORK"/textures/items/*.png; do [ -s "$f" ] || { echo "Image manquante : $f"; exit 1; }; done
rm -f "$DEST/KaLium-objets-$VERSION.mcpack"
(cd "$WORK" && jar cfM "../KaLium-objets-$VERSION.mcpack" .)
rm -rf "$WORK"
echo "OK -> $DEST/KaLium-objets-$VERSION.mcpack"
