#!/bin/sh
# Telecharge Paper 26.3 et les plugins externes de la mise a jour 26.3 (proxy, lobby, Kanvas, Serveur Jeux, Kal-Games)
# dans telechargements/ (ignore par git : ces jars ne sont pas a nous) et verifie leur empreinte.
# Versions : celles validees sur Event et Kixster en Paper 26.3 (07-08/10/2026), sinon la derniere version
# marquee 26.3 sur Modrinth (ConditionalEvents, PlayerKits2) ou le dernier build de GeyserMC (Geyser-Velocity).
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$DIR/telechargements"
cd "$DIR/telechargements"

prendre() { # <fichier> <url> <sha256|sha512> <empreinte>
  somme() { "$3sum" "$1" | cut -d' ' -f1; }
  if [ ! -f "$1" ] || [ "$(somme "$1" "" "$3")" != "$4" ]; then
    echo "Telechargement de $1"
    curl -s -L -f -o "$1" "$2"
  fi
  if [ "$(somme "$1" "" "$3")" != "$4" ]; then
    echo "ERREUR : empreinte inattendue pour $1" >&2
    exit 1
  fi
  echo "OK $1"
}

# Serveur
prendre paper-26.3-159.jar \
  https://fill-data.papermc.io/v1/objects/2224a0b2b6b096ff4c429ad926e97977213e4f633e90cb3a49b5eeb82f94bab0/paper-26.3-159.jar \
  sha256 2224a0b2b6b096ff4c429ad926e97977213e4f633e90cb3a49b5eeb82f94bab0

# Plugins Paper (Modrinth)
prendre ViaVersion-5.12.1-SNAPSHOT.jar \
  https://cdn.modrinth.com/data/P1OZGk5p/versions/TEgYlalY/ViaVersion-5.12.1-SNAPSHOT.jar \
  sha512 9f23879f392a53098e1cc1a70a942bd8558ae39ab725185cf79a5dda396e748af0896416ea0357473e9c8e6b8efb330a8f4afd7ca6bd3f80861419cd67ed69e1
prendre ViaBackwards-5.12.1-SNAPSHOT.jar \
  https://cdn.modrinth.com/data/NpvuJQoq/versions/w6P38zDf/ViaBackwards-5.12.1-SNAPSHOT.jar \
  sha512 5062a409e20d267047a8d8820d28e0b341b7a827f6aff94af365175f1e84506a501f28f013bed89a51b3a2f5dc5afb177207d2ac26105e9afaf228de31def685
prendre worldedit-bukkit-7.4.6-beta-02.jar \
  https://cdn.modrinth.com/data/1u6JkXh5/versions/J1eeOh6C/worldedit-bukkit-7.4.6-beta-02.jar \
  sha512 138ea8f412bf4a17104d2e090a9171e1b263c6b28a060dacda5bc78c4629e9bac1c3834cccaaa50ac87bf148a269cf9dfd46967dddedd64dc512fdf7a5db17f9
prendre worldguard-bukkit-7.0.19.jar \
  https://cdn.modrinth.com/data/DKY9btbd/versions/TtfwTyi6/worldguard-bukkit-7.0.19.jar \
  sha512 e9ad7ad53c93a07a7d5c3af3d844a677abbe894de4d7ff69fc03397daae5e8532217d57de23a3cddbe0d94ae81ee7acde6c78384b9596aa1eb3001bb37087d41
prendre voicechat-bukkit-2.6.24.jar \
  https://cdn.modrinth.com/data/9eGKb6K1/versions/EJth3OAr/voicechat-bukkit-2.6.24.jar \
  sha512 7f1d5765e79cd42616f14f40322d1171a8505e1116dfff71c7bd0e59af9d255c06f70a68a5b622015c0407d40c80e70298c172992007ff339cedcef0116fec42
prendre grimac-bukkit-2.3.74-abb95b6.jar \
  https://cdn.modrinth.com/data/LJNGWSvH/versions/YJEwvStg/grimac-bukkit-2.3.74-abb95b6.jar \
  sha512 12ca9f9a0e19bfee78309768788f141c432ec48b9a3180dbde430159ba09bbab43d25949c903a0a122e916229dfa938325d49aab1a793bc8c40100be6a2130b7
prendre ConditionalEvents-4.80.3.jar \
  https://cdn.modrinth.com/data/4EEcEnDG/versions/5UbBem3Z/ConditionalEvents-4.80.3.jar \
  sha512 6b48666035b89aa56b8dc63655f6008104a3529fa81f30888d5288ddcd9e753c71874c02b8f87780636a0f8bb1ab5d01787138ea1d8cadd2f6f77e48bd073400
prendre PlayerKits2-1.24.1.jar \
  https://cdn.modrinth.com/data/VuKoQN3Q/versions/vHyJzEqp/PlayerKits2-1.24.1.jar \
  sha512 0d813deb06e7ea377c35256b4c7e3546cb2021ee3ce78da37710beda1caa5eebf312844dd0a2ef3df9010f6c5d018ad4a857182de02999d781fc8bdeef2c8826

# Proxy (facultatif) : Geyser 2.11.3 build 1249 du 06/10/2026 (gere Bedrock 26.52)
prendre Geyser-Velocity-2.11.3-b1249.jar \
  https://download.geysermc.org/v2/projects/geyser/versions/2.11.3/builds/1249/downloads/velocity \
  sha256 0ea113ce061273c52f489bc40a3b287b98875b91b881cebde4d6be342f7a540d
