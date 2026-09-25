#!/bin/sh
# Telecharge le compilateur (ecj) et les bibliotheques necessaires pour compiler les plugins KaLium
# dans outils-build/ (dossier ignore par git, ~32 Mo). A lancer une seule fois par machine, depuis
# la racine du depot : sh telecharger-outils.sh
# Versions verifiees le 24/09/2026 : les jars recompiles avec ces outils sont identiques a ceux en service.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$DIR/outils-build/libs"
C=https://repo1.maven.org/maven2
P=https://repo.papermc.io/repository/maven-public

get() {
  if [ -f "$2" ]; then
    echo "deja present : $(basename "$2")"
  else
    curl -sfL -o "$2" "$1" && echo "telecharge   : $(basename "$2")"
  fi
}

get "$C/org/eclipse/jdt/ecj/3.46.100/ecj-3.46.100.jar" "$DIR/outils-build/ecj.jar"

L="$DIR/outils-build/libs"
get "$P/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar" "$L/paper-api-26.2.build.123.jar"
for a in adventure-api adventure-key adventure-text-minimessage adventure-text-serializer-gson \
         adventure-text-serializer-legacy adventure-text-serializer-plain adventure-text-serializer-json \
         adventure-text-logger-slf4j; do
  get "$C/net/kyori/$a/5.2.0/$a-5.2.0.jar" "$L/$a-5.2.0.jar"
done
get "$P/net/md-5/bungeecord-chat/1.21-R0.2-deprecated%2Bbuild.21/bungeecord-chat-1.21-R0.2-deprecated%2Bbuild.21.jar" "$L/bungeecord-chat-1.21-R0.2-deprecated.jar"
get "$C/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar" "$L/guava-33.6.0-jre.jar"
get "$C/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar" "$L/failureaccess-1.0.3.jar"
get "$C/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar" "$L/gson-2.14.0.jar"
get "$C/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar" "$L/snakeyaml-2.2.jar"
get "$C/org/joml/joml/1.10.8/joml-1.10.8.jar" "$L/joml-1.10.8.jar"
get "$C/it/unimi/dsi/fastutil/8.5.18/fastutil-8.5.18.jar" "$L/fastutil-8.5.18.jar"
get "$C/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar" "$L/log4j-api-2.26.0.jar"
get "$C/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar" "$L/slf4j-api-2.0.17.jar"
get "https://libraries.minecraft.net/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar" "$L/brigadier-1.3.10.jar"
get "$C/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar" "$L/jspecify-1.0.0.jar"
get "$C/org/checkerframework/checker-qual/3.49.2/checker-qual-3.49.2.jar" "$L/checker-qual-3.49.2.jar"
get "$C/org/jetbrains/annotations/26.1.0/annotations-26.1.0.jar" "$L/annotations-26.1.0.jar"
# WorldGuard / WorldEdit (API seulement, pour KV_Plots ; sur le serveur, FAWE fournit l'API WorldEdit)
E=https://maven.enginehub.org/repo
get "$E/com/sk89q/worldguard/worldguard-core/7.0.19/worldguard-core-7.0.19.jar" "$L/worldguard-core-7.0.19.jar"
get "$E/com/sk89q/worldguard/worldguard-bukkit/7.0.19/worldguard-bukkit-7.0.19.jar" "$L/worldguard-bukkit-7.0.19.jar"
get "$E/com/sk89q/worldedit/worldedit-core/7.4.5/worldedit-core-7.4.5.jar" "$L/worldedit-core-7.4.5.jar"
get "$E/com/sk89q/worldedit/worldedit-bukkit/7.4.5/worldedit-bukkit-7.4.5.jar" "$L/worldedit-bukkit-7.4.5.jar"
echo "Outils prets dans $DIR/outils-build"
