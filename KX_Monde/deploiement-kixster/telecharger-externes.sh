#!/bin/sh
# Telecharge les plugins externes du paquet Kixster (versions validees sur Event en Paper 26.3, 07/10/2026)
# dans externes/ (ignore par git : ces jars ne sont pas a nous) et verifie leur empreinte SHA-512.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$DIR/externes"
cd "$DIR/externes"

prendre() { # <fichier> <url> <sha512>
  if [ ! -f "$1" ] || [ "$(sha512sum "$1" | cut -d' ' -f1)" != "$3" ]; then
    echo "Telechargement de $1"
    curl -s -L -f -o "$1" "$2"
  fi
  if [ "$(sha512sum "$1" | cut -d' ' -f1)" != "$3" ]; then
    echo "ERREUR : empreinte inattendue pour $1" >&2
    exit 1
  fi
  echo "OK $1"
}

prendre SimpleClaimSystem-1.13.1.jar \
  https://cdn.modrinth.com/data/80Ke0mYG/versions/DRqwNblx/SimpleClaimSystem-1.13.1.jar \
  fd31cb64bc4bf156c0f957a1856f91ed9e68cb35d6bcfb94e888e2c22e76f04539ac8bbfe5bfcbfe83df416f60323bb8eb09d65e2bc4406c32f6311fb4b03e14
prendre VaultUnlocked-2.20.3.jar \
  https://cdn.modrinth.com/data/ayRaM8J7/versions/qZgRzoYs/VaultUnlocked-2.20.3.jar \
  0eedea1591459e7e327315b43afa834a173c8c2ae31b3b235586d31963df29a4a6c0ef4b8f3fdc746e15afd47ee50c1ff93584543b5c2ef7c2a80135dba1136a
prendre grimac-bukkit-2.3.74-abb95b6.jar \
  https://cdn.modrinth.com/data/LJNGWSvH/versions/YJEwvStg/grimac-bukkit-2.3.74-abb95b6.jar \
  12ca9f9a0e19bfee78309768788f141c432ec48b9a3180dbde430159ba09bbab43d25949c903a0a122e916229dfa938325d49aab1a793bc8c40100be6a2130b7
prendre worldguard-bukkit-7.0.19.jar \
  https://cdn.modrinth.com/data/DKY9btbd/versions/TtfwTyi6/worldguard-bukkit-7.0.19.jar \
  e9ad7ad53c93a07a7d5c3af3d844a677abbe894de4d7ff69fc03397daae5e8532217d57de23a3cddbe0d94ae81ee7acde6c78384b9596aa1eb3001bb37087d41
prendre journeymap-paper-26.3-6.0.10.jar \
  https://cdn.modrinth.com/data/lfHFW1mp/versions/nWFxhjT4/journeymap-paper-26.3-6.0.10.jar \
  e221758b768c231344c473a6d1adff84e566520636618221ed518d7104e8d3e66003df7c8e052ed149f2d8429fe0d256118bd25fbdf7859a8dfd083010f89306
prendre floodgate-spigot.jar \
  https://download.geysermc.org/v2/projects/floodgate/versions/2.2.5/builds/141/downloads/spigot \
  f3ee694abaddbbaad3ce1e64b49ee4f2323971e97b9d8a9ff7a38c281f83c95e91dc996223ed3ba04dc3783144ccd70fa7307721e532e62eb368af0fb9e98233
