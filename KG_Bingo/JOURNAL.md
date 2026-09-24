# JOURNAL — KG_Bingo

Plugin Paper du serveur **kal-games** : tout ce que le hub gère pour le Bingo — créer une partie (équipes, taille,
durée ≤ maximum des opérateurs), la rejoindre par code ou depuis la liste des parties encore en salle d'attente,
transférer le joueur vers le serveur Bingo (Kixster), déposer son affectation sur le relais HTTP (KaliumRelay),
répondre aux demandes d'affectation du serveur Bingo, retirer de la liste les parties démarrées/annulées, commande
`/bingo`, Paramètres > Bingo. Le jeu lui-même tourne sur Kixster (plugin KalBingo, futur `KG_BingoGame`).

**Dépend de KalGames** (`depend` dans `plugin.yml`) : utilise ses menus (assistant `Gui`, textes de son `lang.yml`)
et y ajoute ses boutons via `MenuEntry` (voir `KalGames/JOURNAL.md`, 1.13.0). Compilation : `build.sh` compile
d'abord KalGames pour s'en servir, sans jamais l'embarquer dans le jar.

Historique antérieur (quand ce code était dans KalGames) : `KalGames/JOURNAL.md`, sections 1.10.0 à 1.10.7.

## 1.0.0 — création : partie Bingo sortie de KalGames (24/09/2026)

**Demande de LeKiwi06** : « il faut donc isoler KG_bingo (tout ce que kal-games doit gérer pour le bingo) de
kal_games » (règle « un plugin = un rôle »). Étape A du découpage du Bingo.

- Code déplacé de KalGames 1.12.4 **sans changement de comportement** : package `fr.kalium.games.bingo` (nom
  conservé), menus Bingo réunis dans `BingoMenus`. Nouvelle classe principale `KGBingo`. Vérifié : les classes
  déplacées compilent à l'identique (seul le type du plugin hôte change).
- `config.yml` : même section `bingo:` qu'avant dans KalGames (mêmes clés, `relay-token: ""` à remplir sur le
  serveur).
- Textes : mêmes clés qu'avant (`bingo.*`, `admin.bingo-*`, `admin.home-bingo*`), lus dans le `lang.yml` de KalGames.

**Déploiement** (en même temps que KalGames 1.13.0) : envoyer le jar dans `/plugins/` de kal-games, démarrer une
fois pour créer `plugins/KG_Bingo/config.yml`, y recopier la section `bingo:` de `plugins/KalGames/config.yml`
(**jeton compris**, recopié par l'humain), redémarrer. **Statut : compilé, non déployé.**

## 1.1.0 — réglages du nouveau Bingo dans le menu de création (24/09/2026)

**Demande de LeKiwi06** : mode et composition de la grille choisis à la création.
- Menu « Nouvelle partie Bingo » : mode (*Bingos* avec chrono, ou *Blackout* sans chrono), bingos à achever (3 à 12,
  défaut 3), nombre d'objectifs faciles / normaux / difficiles / extrêmes (total obligatoirement 25 ; défaut
  10 / 10 / 5 / 0). La durée ne sert pas en blackout.
- Réglages transmis à KG_BingoGame (texte `rules`, en fin de message BungeeCord et dans le relais) ; sans réglages
  (`/bingo create`), KG_BingoGame applique ses valeurs par défaut.

**Déploiement** : avec KG_BingoGame 0.3.0 et KalGames 1.13.0 (remplace la 1.0.0, jamais déployée : même procédure).
**Statut : compilé, non déployé.**
