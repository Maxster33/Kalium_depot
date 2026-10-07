#!/bin/sh
# Envoi du paquet Kixster (plugins d'Event adaptés + réglages de serveur de LeKiwi06). Git Bash + WinSCP.
# Kixster doit être ARRÊTÉ. Rien n'est supprimé sur le serveur : les fichiers remplacés vont dans un _removed-…
#
#   sh envoyer.sh [session WinSCP de Kixster] [session WinSCP d'Event]
#   (par défaut : kixster@7003.mystrator.com et Event@7021.mystrator.com ; noms des onglets enregistrés)
#   DRY_RUN=1 sh envoyer.sh ...   -> prépare et vérifie tout, mais n'envoie rien (dossier gardé pour relecture)
#
# Les secrets ne passent jamais par le dépôt : jeton du relais (KS_RewardsGUI) et clé Floodgate sont lus sur Event,
# le secret Velocity reste dans le paper-global.yml de Kixster (modifié sur place, ligne par ligne).
set -e
KX="${1:-kixster@7003.mystrator.com}"
EV="${2:-Event@7021.mystrator.com}"
WINSCP="${WINSCP:-/c/Program Files (x86)/WinSCP/WinSCP.com}"
DIR="$(cd "$(dirname "$0")" && pwd)"
REMOVED="/_removed-config-avant-plugins-event-$(date +%Y%m%d-%H%M)"
export MSYS_NO_PATHCONV=1

[ -x "$WINSCP" ] || { echo "WinSCP.com introuvable ($WINSCP) : variable WINSCP=..." >&2; exit 1; }
sh "$DIR/telecharger-externes.sh"

T="$(mktemp -d)"   # dossier temporaire hors du dépôt (contiendra les secrets)
w() { cygpath -w "$1"; }
winscp() { # <script> : exécute un script WinSCP ; signale toute erreur autre que le dossier local enregistré absent
  "$WINSCP" /script="$(w "$1")" /log="$(w "$1").log" > "$1.out" 2>&1 || true
  if grep -i -E "erreur|error|échec|failure" "$1.out" | grep -v -E "changement de répertoire|Erreur système. Code : [23]|chemin d.accès|introuvable" | grep -q .; then
    cat "$1.out" >&2; echo "Erreur WinSCP (journal : $1.log)" >&2; exit 1
  fi
}
ouvrir() { # session : l'option continue évite l'arrêt sur un dossier local enregistré absent
  printf 'option batch continue\noption confirm off\nopen "%s"\noption batch abort\n' "$1"
}
echo "Dossier de travail : $T"
cp -r "$DIR/plugins" "$DIR/racine" "$T/"
cp "$DIR/externes/"*.jar "$T/plugins/"
mkdir -p "$T/serveur/config" "$T/serveur/the_nether" "$T/serveur/the_end" "$T/event"
MONDE="/Kixster SMP/dimensions/minecraft"

# 1. Fichiers actuels de Kixster et secrets d'Event
{ ouvrir "$KX"
  echo "get /spigot.yml \"$(w "$T/serveur")\\\\\""
  echo "get /config/paper-global.yml /config/paper-world-defaults.yml \"$(w "$T/serveur/config")\\\\\""
  echo "get \"$MONDE/the_nether/paper-world.yml\" \"$(w "$T/serveur/the_nether")\\\\\""
  echo "get \"$MONDE/the_end/paper-world.yml\" \"$(w "$T/serveur/the_end")\\\\\""
  echo "close"
  ouvrir "$EV"
  echo "get /plugins/floodgate/key.pem /plugins/KS_RewardsGUI/config.yml \"$(w "$T/event")\\\\\""
  echo "exit"; } > "$T/lire.txt"
winscp "$T/lire.txt"

# 2. Secrets
JETON="$(grep '^relay-token:' "$T/event/config.yml")"
[ -n "$JETON" ] && [ "$JETON" != 'relay-token: ""' ] || { echo "Jeton du relais introuvable sur Event" >&2; exit 1; }
sed -i "s|^relay-token: \"\"$|$JETON|" "$T/plugins/KS_RewardsGUI/config.yml"
[ -s "$T/event/key.pem" ] || { echo "Clé Floodgate introuvable sur Event" >&2; exit 1; }
cp "$T/event/key.pem" "$T/plugins/floodgate/key.pem"
for f in spigot.yml config/paper-global.yml config/paper-world-defaults.yml the_nether/paper-world.yml the_end/paper-world.yml; do
  [ -s "$T/serveur/$f" ] || { echo "Fichier de Kixster non récupéré : $f" >&2; exit 1; }
done

# 3. Réglages de LeKiwi06 (mêmes changements que sur Event le 03/10/2026), appliqués aux fichiers de Kixster
S="$T/serveur"; R="$T/racine"; mkdir -p "$R/config" "$R/Kixster SMP/dimensions/minecraft/the_nether" "$R/Kixster SMP/dimensions/minecraft/the_end"
sed -e '/entity-tracking-range:/,/other:/{s/^\(      players:\) 128$/\1 64/;s/^\(      animals:\) 96$/\1 48/;s/^\(      monsters:\) 96$/\1 48/;s/^\(      misc:\) 96$/\1 32/;s/^\(      other:\) 64$/\1 32/}' \
  "$S/spigot.yml" > "$R/spigot.yml"
sed -e 's/^\(  allow-piston-duplication:\) false$/\1 true/' "$S/config/paper-global.yml" > "$R/config/paper-global.yml"
sed -e '/^  anti-xray:/,/^    replacement-blocks:/{s/^\(    enabled:\) false$/\1 true/;s/^\(    lava-obscures:\) false$/\1 true/;s/^\(    max-block-height:\) 64$/\1 128/;s/^\(    - ender_chest\)$/\1\n    - trapped_chest\n    - barrel\n    - spawner/}' \
  "$S/config/paper-world-defaults.yml" > "$R/config/paper-world-defaults.yml"
for d in the_nether the_end; do
  if grep -q '^anticheat:' "$S/$d/paper-world.yml"; then
    cp "$S/$d/paper-world.yml" "$R/Kixster SMP/dimensions/minecraft/$d/paper-world.yml"   # déjà réglé : inchangé
  else
    cat "$S/$d/paper-world.yml" "$DIR/reglages-serveur/$d-anti-xray.yml" > "$R/Kixster SMP/dimensions/minecraft/$d/paper-world.yml"
  fi
done
verifier() { grep -q -- "$2" "$1" || { echo "ATTENTION : « $2 » absent de $1 (réglage déjà différent sur Kixster ?)" >&2; }; }
verifier "$R/spigot.yml" '      players: 64'
verifier "$R/config/paper-global.yml" 'allow-piston-duplication: true'
verifier "$R/config/paper-world-defaults.yml" '    - spawner'
verifier "$R/config/paper-world-defaults.yml" 'max-block-height: 128'
echo "Réglages modifiés (diff) :"
diff -r "$S/config" "$R/config" | grep '^[<>]' || true

if [ -n "$DRY_RUN" ]; then echo "DRY_RUN : rien n'est envoyé. Contenu préparé dans $T"; exit 0; fi

# 4. Envoi : originaux rangés dans $REMOVED, puis réglages et plugins
{ ouvrir "$KX"
  for d in "" /config /the_nether /the_end; do echo "mkdir $REMOVED$d"; done
  echo "mv /spigot.yml $REMOVED/"
  echo "mv /config/paper-global.yml $REMOVED/config/"
  echo "mv /config/paper-world-defaults.yml $REMOVED/config/"
  echo "mv \"$MONDE/the_nether/paper-world.yml\" $REMOVED/the_nether/"
  echo "mv \"$MONDE/the_end/paper-world.yml\" $REMOVED/the_end/"
  echo "put -resumesupport=off \"$(w "$T/racine")\\\\*\" /"
  echo "put -resumesupport=off \"$(w "$T/plugins")\\\\*\" /plugins/"
  echo "ls /plugins/"
  echo "exit"; } > "$T/envoyer.txt"
winscp "$T/envoyer.txt"
grep '\.jar' "$T/envoyer.txt.out" | grep -v '100%' | awk '{print $5, $NF}' || true
rm -rf "$T"
echo "Envoi terminé. Originaux dans $REMOVED. Redémarrer Kixster puis vérifier le journal (voir LISEZMOI.md)."
