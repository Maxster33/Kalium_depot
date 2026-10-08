#!/bin/sh
# Mise a jour 26.3 d'un serveur du reseau (Git Bash + WinSCP). Voir LISEZMOI.md.
#
#   sh envoyer.sh <serveur> sauvegarde   -> copie le serveur sur le PC (lecture seule, serveur allume ou arrete)
#   DRY_RUN=1 sh envoyer.sh <serveur>    -> verifie tout (fichiers presents, telechargements), n'envoie rien
#   sh envoyer.sh <serveur>              -> SERVEUR ARRETE : sauvegarde a jour, puis envoi
#   OPTIONS=1 sh envoyer.sh <serveur>    -> idem + changements facultatifs (voir LISEZMOI.md, a valider avant)
#
# <serveur> : proxy | lobby | kanvas | serveurjeux | kalgames
# Rien n'est supprime : les anciens jars vont dans /plugins/_removed-avant-26.3/ (proxy : _removed-avant-geyser-b1249/).
set -e
SRV="$1"; MODE="$2"
WINSCP="${WINSCP:-/c/Program Files (x86)/WinSCP/WinSCP.com}"
DIR="$(cd "$(dirname "$0")" && pwd)"
DL="$DIR/telechargements"
SAUVE="${SAUVE:-$HOME/Documents/Sauvegardes_KaLium}"
export MSYS_NO_PATHCONV=1

# Par serveur : session WinSCP, PAPER (1 = poser paper-26.3-159.jar a la racine),
# MAJ (ancien>nouveau), AJOUT (nouveaux), RETRAIT (a ranger), OPT (facultatifs : ancien>nouveau ou ancien seul).
PAPER=1; MAJ=""; AJOUT=""; RETRAIT=""; OPT=""; RANGE="/plugins/_removed-avant-26.3"
case "$SRV" in
  proxy)
    SESSION="ProxyVelocity@7018.mystrator.com"; PAPER=0; RANGE="/plugins/_removed-avant-geyser-b1249"
    OPT="Geyser-Velocity.jar>Geyser-Velocity-2.11.3-b1249.jar geyserupdater-spigot.jar" ;;
  lobby)
    SESSION="lobby@7002.mystrator.com"
    MAJ="worldedit-bukkit-7.4.5.jar>worldedit-bukkit-7.4.6-beta-02.jar worldguard-bukkit-7.0.18.jar>worldguard-bukkit-7.0.19.jar voicechat-bukkit-2.6.23.jar>voicechat-bukkit-2.6.24.jar"
    AJOUT="ViaVersion-5.12.1-SNAPSHOT.jar ViaBackwards-5.12.1-SNAPSHOT.jar"
    OPT="Geyser-Spigot.jar" ;;
  kanvas)
    SESSION="Kanvas@5038.mystrator.com" ;;   # deja prepare le 08/10/2026 vers 08:47 : seul le jar de demarrage change
  serveurjeux)
    SESSION="serveurjeux@7015.mystrator.com"
    MAJ="ViaVersion-5.12.0-SNAPSHOT.jar>ViaVersion-5.12.1-SNAPSHOT.jar worldedit-bukkit-7.4.6-beta-01.jar>worldedit-bukkit-7.4.6-beta-02.jar"
    AJOUT="ViaBackwards-5.12.1-SNAPSHOT.jar"
    RETRAIT="legacyfreecam-paper-2.0.0.jar"
    OPT="Skript-2.16.2.jar TradeShop-1.7.jar woodcutter-paper-1.0.2.jar Geyser-Spigot.jar" ;;
  kalgames)
    SESSION="KalGames@7001.mystrator.com"
    MAJ="ViaVersion-5.12.0.jar>ViaVersion-5.12.1-SNAPSHOT.jar ViaBackwards-5.12.0.jar>ViaBackwards-5.12.1-SNAPSHOT.jar worldedit-bukkit-7.4.5.jar>worldedit-bukkit-7.4.6-beta-02.jar voicechat-bukkit-2.6.23.jar>voicechat-bukkit-2.6.24.jar grimac-bukkit-2.3.74-8eb5f28.jar>grimac-bukkit-2.3.74-abb95b6.jar ConditionalEvents-4.79.2.jar>ConditionalEvents-4.80.3.jar PlayerKits2-1.23.3.jar>PlayerKits2-1.24.1.jar" ;;
  *) echo "Serveur inconnu : '$SRV' (proxy | lobby | kanvas | serveurjeux | kalgames)" >&2; exit 1 ;;
esac
[ "${OPTIONS:-0}" = 1 ] && MAJ="$MAJ $(for o in $OPT; do case "$o" in *">"*) echo "$o";; esac; done)" \
  && RETRAIT="$RETRAIT $(for o in $OPT; do case "$o" in *">"*) ;; *) echo "$o";; esac; done)"

[ -x "$WINSCP" ] || { echo "WinSCP.com introuvable ($WINSCP)" >&2; exit 1; }
T="$(mktemp -d)"
w() { cygpath -w "$1"; }
winscp() { # <script>
  "$WINSCP" /script="$(w "$1")" /log="$(w "$1").log" > "$1.out" 2>&1 || true
  if grep -i -E "erreur|error|échec|failure" "$1.out" | grep -v -E "changement de répertoire|Erreur système. Code : [23]|chemin d.accès|introuvable" | grep -q .; then
    cat "$1.out" >&2; echo "Erreur WinSCP (journal : $1.log)" >&2; exit 1
  fi
}
ouvrir() { printf 'option batch continue\noption confirm off\nopen "%s"\noption batch abort\n' "$SESSION"; }

sauvegarder() { # copie du serveur sur le PC (synchronisation : seuls les fichiers changes sont recopies)
  D="$SAUVE/$SRV-avant-26.3"; mkdir -p "$D"
  echo "Sauvegarde de $SRV dans $D (peut etre long la premiere fois)"
  { ouvrir
    echo "synchronize local -resumesupport=off -filemask=\"|libraries/; cache/; .cache/; versions/; .ai-backups/; logs/; crash-reports/; _removed-*/; *.log.gz\" \"$(w "$D")\" /"
    echo "exit"; } > "$T/sauve.txt"
  winscp "$T/sauve.txt"
  echo "Sauvegarde OK : $(du -sh "$D" | cut -f1)"
}

if [ "$MODE" = sauvegarde ]; then sauvegarder; rm -rf "$T"; exit 0; fi

sh "$DIR/telecharger.sh" > /dev/null
echo "== $SRV ($SESSION)"

# 1. Etat actuel du serveur
{ ouvrir; echo "ls /"; echo "ls /plugins/"; echo "exit"; } > "$T/ls.txt"
winscp "$T/ls.txt"
present() { grep -q -E " $(printf '%s' "$1" | sed 's/[][\.*^$]/\\&/g')$" "$T/ls.txt.out"; }
taille_loc() { wc -c < "$DL/$1" | tr -d ' '; }

# 2. Plan (et verification : chaque ancien jar doit etre la, sauf si le nouveau l'est deja)
ENVOIS=""; RANGES=""; PROBLEME=0
for m in $MAJ; do
  a="${m%%>*}"; n="${m##*>}"
  [ -f "$DL/$n" ] || { echo "  MANQUE sur le PC : $n" >&2; PROBLEME=1; }
  if present "$a"; then RANGES="$RANGES $a"; ENVOIS="$ENVOIS $n"; echo "  remplacer $a -> $n"
  elif present "$n"; then echo "  deja fait : $n"
  else echo "  ATTENTION : ni $a ni $n sur le serveur" >&2; PROBLEME=1; fi
done
for n in $AJOUT; do
  if present "$n"; then echo "  deja la : $n"; else ENVOIS="$ENVOIS $n"; echo "  ajouter $n"; fi
done
for a in $RETRAIT; do
  if present "$a"; then RANGES="$RANGES $a"; echo "  ranger $a (dans $RANGE/)"; else echo "  deja range : $a"; fi
done
if [ "$PAPER" = 1 ]; then
  if present paper-26.3-159.jar; then echo "  paper-26.3-159.jar deja a la racine"
  else echo "  poser paper-26.3-159.jar a la racine"; fi
fi
[ "${OPTIONS:-0}" = 1 ] || [ -z "$OPT" ] || echo "  (facultatifs NON compris, OPTIONS=1 pour les inclure : $OPT)"
[ "$PROBLEME" = 0 ] || { echo "Probleme : rien n'est envoye." >&2; exit 1; }

if [ "${DRY_RUN:-0}" = 1 ]; then echo "DRY_RUN : rien n'est envoye."; rm -rf "$T"; exit 0; fi

# 3. Sauvegarde a jour (le serveur doit etre ARRETE : mondes coherents)
sauvegarder

# 4. Envoi : nouveaux jars d'abord, puis anciens ranges (jamais de serveur sans le plugin)
{ ouvrir
  for n in $ENVOIS; do echo "put -resumesupport=off \"$(w "$DL/$n")\" /plugins/"; done
  if [ "$PAPER" = 1 ] && ! present paper-26.3-159.jar; then
    echo "put -resumesupport=off \"$(w "$DL/paper-26.3-159.jar")\" /"
  fi
  [ -z "$RANGES" ] || echo "mkdir $RANGE"
  for a in $RANGES; do echo "mv \"/plugins/$a\" $RANGE/"; done
  echo "ls /"; echo "ls /plugins/"; echo "exit"; } > "$T/envoi.txt"
grep -q "^mkdir" "$T/envoi.txt" && sed -i 's|^mkdir \(.*\)$|option batch continue\nmkdir \1\noption batch abort|' "$T/envoi.txt"
winscp "$T/envoi.txt"

# 5. Verification des tailles
ERR=0
for n in $ENVOIS; do
  t="$(grep -E " $(printf '%s' "$n" | sed 's/[][\.*^$]/\\&/g')$" "$T/envoi.txt.out" | tail -1 | awk '{print $5}')"
  [ "$t" = "$(taille_loc "$n")" ] && echo "  OK $n ($t octets)" || { echo "  TAILLE INCORRECTE : $n ($t au lieu de $(taille_loc "$n"))" >&2; ERR=1; }
done
for a in $RANGES; do
  if grep -q -E " $(printf '%s' "$a" | sed 's/[][\.*^$]/\\&/g')$" "$T/envoi.txt.out"; then
    echo "  ATTENTION : $a est encore dans /plugins/" >&2; ERR=1
  else echo "  range : $a"; fi
done
rm -rf "$T"
[ "$ERR" = 0 ] || exit 1
echo "Envoi termine pour $SRV. Anciens jars dans $RANGE/."
[ "$PAPER" = 1 ] && echo "A FAIRE dans le panneau Minestrator : Parametres > Hebergement > « Changer le parametre de demarrage » -> paper-26.3-159.jar, puis demarrer."
exit 0
