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
**Statut : déployé sur kal-games le 24/09/2026 (config = copie de celle de KalGames, jeton compris, faite
sur le serveur sans être lue), non testé en jeu.**

## 1.2.0 — interfaces dans le catalogue de KLM_Menu (24/09/2026)

- Déclare **« Bingo »** (joueurs : créer / rejoindre une partie) et **« Bingo : réglages »** (admins) dans le
  catalogue de KLM_Menu. Dépend de KLM_Menu. Les classements Bingo viendront avec KG_ScoreBoards (étape B).
**Déploiement** : avec KLM_Menu 2.0.0, KalGames 1.15.0. **Statut : déployé le 24/09/2026, non testé en jeu.**

## 1.3.0 — boutons fournis à KG_Menu (24/09/2026)

- « Bingo » (accueil) et « Bingo » (Paramètres, admins) fournis à KG_Menu (découverte au démarrage), à la place des
  prises MenuEntry de KalGames et de l'inscription directe dans KLM_Menu (1.2.0). Dépend de KG_Menu.
**Déploiement** : avec KG_Menu 1.0.0, KalGames 1.16.0. **Statut : déployé sur Kal-Games (7001) le 24/09/2026, testé et confirmé par LeKiwi06 le 24/09/2026 (création, liste, transfert vers Serveur Jeux).**

## 1.4.0 — pseudo de l'hôte dans la liste des parties (25/09/2026)

**Signalé par LeKiwi06** : « le bouton dans la liste des games de bingo est buggué, il ne met pas le pseudo du joueur
qui l'a créé, il met toujours Maxster » (bug déjà noté dans `REPRISE_PROJET.md`).
- Cause : le texte par défaut de `bingo.party-entry` contenait le pseudo écrit en dur ; le `lang.yml` de KalGames l'a
  enregistré au premier affichage (« <yellow>Maaxster</yellow>... ») et l'a réutilisé pour toutes les parties.
- Correctif : nouvelle clé `bingo.party-entry-2` avec le paramètre `<host>` ; « (complet) » dans sa propre clé
  (`bingo.party-entry-full`). Nombre d'équipes plus clair, comme prévu dans REPRISE : « 2 équipe(s) de 4 · 3/8
  joueurs » au lieu de « 2x4 équipes · 3/8 ». L'ancienne clé reste dans le `lang.yml` du serveur, inutilisée.
**Déploiement** : seul, sur Kal-Games (7001). **Statut : déployé le 25/09/2026 à 15 h 16, testé et confirmé par LeKiwi06 le 25/09/2026 (pseudo du créateur, équipes).**

## 1.5.0 — points du Bingo dans les classements (26/09/2026)

**Demande de LeKiwi06** : brancher les points du Bingo sur les classements (KG_ScoreBoards), dans le cadre de
l'équilibrage des barèmes (`EQUILIBRAGE_POINTS.md`).
- Nouveau classement **« Bingo »** (id `bingo`, points seulement), général et du mois, fourni à KG_ScoreBoards.
- `BingoResults` : chaque partie qui quitte la liste d'attente (démarrée ou annulée) est notée « résultats attendus »
  (`bingo-awaiting.yml`, 24 h) ; le relais HTTP est interrogé toutes les 30 s (clé `bingo-results-<gameId>`, publiée
  par KG_BingoGame 0.8.0) ; les points solo finaux sont **crédités une seule fois**, puis la partie sort de la liste.
- Comme les autres jeux : points des opérateurs non comptés (`stats.exclude-operators` de KalGames), chaque
  attribution écrite dans le journal des parties (`journal/<mois>/bingo.jsonl`, événement « points ») :
  `/classements verifier` et `crediter` fonctionnent aussi pour le Bingo.
- Dépend aussi de KG_ScoreBoards (plugin.yml).
- Limite : une partie démarrée pendant que le hub était arrêté n'est pas notée (pas de résultats crédités) ; ses
  points restent dans le journal de Serveur Jeux.
**Déploiement** : avec **KG_BingoGame 0.8.0** (Serveur Jeux), sur Kal-Games le 26/09/2026 à 5 h 23 (`_removed-kg_bingo-1.4.0/`). **Statut : non testé en jeu.**

