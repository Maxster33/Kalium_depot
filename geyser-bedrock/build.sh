#!/bin/sh
# Fabrique le pack Bedrock des objets custom (pour Geyser, sur le proxy) : <racine du depot>/sortie/KaLium-objets-<version>.mcpack
# Les images viennent du jeu Java installe sur le PC (le depot est public : aucune image de Mojang n'y est copiee).
# Usage : sh geyser-bedrock/build.sh [chemin du .jar du jeu] (par defaut : version 26.2 du launcher officiel).
set -e
export JAVA_TOOL_OPTIONS=
VERSION=1.3.0
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
unzip -p "$GAME_JAR" assets/minecraft/textures/item/blaze_rod.png > "$WORK/textures/items/kalium_bedrock_breaker.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/disc_fragment_5.png > "$WORK/textures/items/kalium_fragment_spawner.png"
# Ancre de reapparition : bloc en 3D sur Java ; sur Bedrock, image plate de son cote (icone d'objet).
unzip -p "$GAME_JAR" assets/minecraft/textures/block/respawn_anchor_side0.png > "$WORK/textures/items/kalium_coeur_spawner.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/end_crystal.png > "$WORK/textures/items/kalium_changeur_biome.png"
# 1.3.0 : jetons (lingots, pelle en or) et badges (blocs : image plate d'une face) de KS_Jetons.
unzip -p "$GAME_JAR" assets/minecraft/textures/item/iron_ingot.png > "$WORK/textures/items/kalium_jeton_fly.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/netherite_ingot.png > "$WORK/textures/items/kalium_jeton_mort.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/gold_ingot.png > "$WORK/textures/items/kalium_jeton_tp.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/copper_ingot.png > "$WORK/textures/items/kalium_jeton_localisation.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/item/golden_shovel.png > "$WORK/textures/items/kalium_jeton_claim.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/block/iron_block.png > "$WORK/textures/items/kalium_badge_fly.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/block/netherite_block.png > "$WORK/textures/items/kalium_badge_mort.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/block/gold_block.png > "$WORK/textures/items/kalium_badge_tp.png"
unzip -p "$GAME_JAR" assets/minecraft/textures/block/copper_block.png > "$WORK/textures/items/kalium_badge_localisation.png"
for f in "$WORK"/textures/items/*.png; do [ -s "$f" ] || { echo "Image manquante : $f"; exit 1; }; done
rm -f "$DEST/KaLium-objets-$VERSION.mcpack"
(cd "$WORK" && jar cfM "../KaLium-objets-$VERSION.mcpack" .)
rm -rf "$WORK"
echo "OK -> $DEST/KaLium-objets-$VERSION.mcpack"
