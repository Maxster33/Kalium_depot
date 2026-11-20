#!/bin/sh
# Fabrique le pack Bedrock des objets custom (pour Geyser, sur le proxy) : <racine du depot>/sortie/KaLium-objets-<version>.mcpack
# Les images viennent de visuels-objets/png (hors du depot, le depot est public : aucune image de Mojang n'y est copiee).
# Usage : sh geyser-bedrock/build.sh
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.13.0
DIR="$(cd "$(dirname "$0")" && pwd)"
DEST="$DIR/../sortie"
WORK="$DIR/../sortie/geyser-pack-tmp"
rm -rf "$WORK" && mkdir -p "$WORK/textures/items" "$DEST"
cp "$DIR/pack/manifest.json" "$WORK/"
cp "$DIR/pack/textures/item_texture.json" "$WORK/textures/"
# 1.9.0 : sons du rhum de KS_Alcools (disques sans position, fichiers de Bedrock lui-meme : rien a copier).
mkdir -p "$WORK/sounds" && cp "$DIR/pack/sounds/sound_definitions.json" "$WORK/sounds/"
# 1.8.0 : les images sont les textures de visuels-objets (png/, hors du depot : faites a partir des images du jeu), les
# memes que le pack Java de KS_Textures ; une par icone de pack/textures/item_texture.json.
PNG="$DIR/../visuels-objets/png"
for id in $(grep -o '"kalium\.[a-z0-9_]*"' "$DIR/pack/textures/item_texture.json" | tr -d '"' | sed 's/^kalium\.//'); do
  [ -f "$PNG/$id.png" ] || { echo "Texture manquante : $PNG/$id.png (python visuels-objets/dessiner.py)"; exit 1; }
  cp "$PNG/$id.png" "$WORK/textures/items/kalium_$id.png"
done
# 1.11.0 : textures des blocs custom de KS_Blocs (visuels-objets/png/blocs), une par entree de terrain_texture.json
# (fichier ecrit par KS_Blocs/pack/blocs.py, avec custom_mappings/kalium_blocs.json).
mkdir -p "$WORK/textures/blocks" && cp "$DIR/pack/textures/terrain_texture.json" "$WORK/textures/"
for id in $(grep -o '"kalium_[a-z0-9_]*"' "$DIR/pack/textures/terrain_texture.json" | tr -d '"' | sed 's/^kalium_//'); do
  [ -f "$PNG/blocs/$id.png" ] || { echo "Texture manquante : $PNG/blocs/$id.png (python KS_Blocs/pack/blocs.py)"; exit 1; }
  cp "$PNG/blocs/$id.png" "$WORK/textures/blocks/kalium_$id.png"
done
for f in "$WORK"/textures/items/*.png "$WORK"/textures/blocks/*.png; do [ -s "$f" ] || { echo "Image manquante : $f"; exit 1; }; done
rm -f "$DEST/KaLium-objets-$VERSION.mcpack"
(cd "$WORK" && jar cfM "../KaLium-objets-$VERSION.mcpack" .)
rm -rf "$WORK"
echo "OK -> $DEST/KaLium-objets-$VERSION.mcpack"
