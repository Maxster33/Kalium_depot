# Reprise du projet KaLium

Deux parties (voir `REGLES.md`, section 5) :
- **Partie permanente** : état actuel du projet, tenu à jour à chaque session, jamais archivé.
- **Comptes rendus signés** : le dernier compte rendu de chacun ; les plus anciens sont dans `archive_reprise.md`.

Le détail technique de chaque version est dans `<plugin>/JOURNAL.md`. Documents hors dépôt, chez l'utilisateur :
`to_do_list_Kal_Games_Bingo.txt` (cahier des charges Bingo), `Procedure-Claude-controle-PC-deploiement.md`.

# Partie permanente

## Architecture du réseau

| Rôle | Nom Velocity | Hôte SFTP (onglet WinSCP) | Plugin(s) à nous |
|---|---|---|---|
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | KaliumRelay (relais HTTP, port 46199) |
| Lobby | `lobby` | `lobby@7002.mystrator.com` | KaliumMenu |
| Hub mini-jeux | `kal-games` | `kalgames@7021.mystrator.com` | KalGames, KaliumMenu (KG_Bingo à venir) |
| Serveur Bingo (dédié) | `kixster` | `kixster@7003.mystrator.com` | KalBingo (KG_BingoGame à venir) |
| Serveur de test survie | `Kal-Test-Dev` | `Kal-Test-Dev@5038` | KaliumCore (projet en pause) |

- **KalGames** : le hub et les mini-jeux (PvP Kit, Parcours, Course de bateau, Rush ; Hunger Games, Manhunt et
  Build Battle configurables mais sans moteur). Chaque partie joue sur une copie de l'arène collée dans le monde
  vide `kalgames:instances` ; copies réutilisées et conservées aux arrêts propres. Classements général / mensuel /
  archives, panneaux dans le hub. Depuis 1.13.0 (non déployée), le Bingo n'y est plus : il est dans KG_Bingo.
- **KG_Bingo** (nouveau, non déployé, dépend de KalGames) : menu "Bingo" du hub : créer une partie (équipes,
  taille, durée ≤ maximum des opérateurs) ou rejoindre une partie listée ; transfère les joueurs vers Kixster.
  Tant qu'il n'est pas déployé, c'est KalGames 1.12.3 qui fait ce travail.
- **KalBingo** (renommé `KG_BingoGame` dans le dépôt, pas encore déployé sous ce nom) : tout le jeu Bingo, sur Kixster. Salle d'attente (modèle capturé via `/menu`, collé sur 4
  emplacements), équipes assignées par l'hôte, overworld + Nether + End par équipe (même seed), grille 5×5
  (`objectives.yml`, liste d'exemples encore provisoire), validation automatique par inventaire, score, HUD, fin
  de partie (victoire, temps, abandon d'équipe, plus personne connecté), parties en cours conservées au
  redémarrage.
- **Communication kal-games ↔ Kixster** : deux chemins en parallèle.
  1. Canal BungeeCord "Forward" — ne livre un message QUE si au moins un joueur est connecté sur le serveur
     CIBLE (limite du protocole Minecraft, source de plusieurs bugs passés).
  2. Relais HTTP KaliumRelay (sur le proxy) — indépendant des joueurs. `/assignment/<clé>` (POST dépose, GET lit
     et efface, expire en 2 min) et `/active-game/<uuid>` (joueur en partie, pour la reconnexion directe).
     Jeton : `relay-token` dans les config.yml de KalGames (`bingo.`, puis KG_Bingo après déploiement) et
     KalBingo (`network.`).
- **Signal "partie fermée"** (Bingo démarré/annulé → retiré de la liste kal-games) : Forward + clé relais
  `party-closed-<gameId>` republiée chaque minute pendant 6 h ; kal-games interroge le relais toutes les 5 s.

## Versions en service

Copie exacte de chaque jar dans `jars-deployes/`.

| Serveur | Jar | Test en jeu |
|---|---|---|
| Kixster | `KG_BingoGame-0.4.0.jar` (0.3.0 déployée le 24/09/2026, 0.4.0 le même jour ; KalBingo renommé, données dans `plugins/KG_BingoGame/`, `bukkit.yml` : `generator: KG_BingoGame` ; config.yml du serveur : `lobby.max-size: 256`, `lobby.blocks-per-tick: 30000`) | 0.3.0 testée le 24/09/2026 (annonces, coefficients, Nether/End par équipe, nulle en solo, invincibilité OK) ; 0.4.0 non testée (carte, têtes, résumé) |
| kal-games | `KalGames-1.13.0.jar` + `KG_Bingo-1.1.0.jar` (déployés le 24/09/2026 ; `plugins/KG_Bingo/config.yml` = copie de celui de KalGames, jeton compris) | non testé (Rush ; capture d'arène sans crash à confirmer en recapturant le parkour ; menu Bingo déplacé) |
| kal-games, lobby | `KaliumMenu-1.5.0.jar` | non confirmé (bouton Paramètres des téléportations) |
| proxy | `KaliumRelay-1.1.1.jar` (déployé le 24/09/2026) | relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kal-Test-Dev | `KaliumCore-1.4.0.jar` | non testé (projet en pause) |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

Aucune.

## Chantiers en cours

- **Rush (KalGames 1.11.0 → 1.12.3)** : jouable, jamais testé en jeu. Cahier des charges et décisions :
  `KalGames/JOURNAL.md`, sections 1.11.0 à 1.12.3. Code : `game/RushInstance.java`, `game/RushItems.java`,
  `listener/RushListener.java`, `model/RushLayout.java`. Prochaine étape : retours de test (arène à configurer :
  salle d'attente + 11 points par base).
- **Découpage du Bingo** (décidé le 24/09/2026, étape par étape) :
  - étape A, faite (non déployée) : `KG_Bingo` sorti de KalGames (KalGames 1.13.0 + KG_Bingo 1.0.0) ;
  - étape B, faite (non déployée) : KalBingo renommé `KG_BingoGame` 0.2.0 (dossier du dépôt `KG_BingoGame/`) ;
    migration de Kixster décrite dans `KG_BingoGame/JOURNAL.md` ;
  - nouveau Bingo, fait (non déployé, non testé) : KG_BingoGame 0.3.0 + KG_Bingo 1.1.0 (barème, modes, nulle,
    inactivité...) ; le « Nether commun » vu en test venait de la 0.1.20 (journaux de Kixster) : Nether / End par équipe à tester ;
  - puis déploiement de tout en une fois et test complet d'une partie.
- **KG_ScoreBoards** (annoncé par LeKiwi06 le 24/09/2026, après le Bingo) : scoreboards sortis de KalGames,
  alimentés entre autres par les points d'équipe et solo du Bingo. Affichage des scores décidé : **6 chiffres au
  plus** ; une décimale tant que c'est possible, plus de décimale à partir de 1 million ; puis à chaque puissance de
  1000 le préfixe adéquat (K, M, Md) avec 3 chiffres décimaux au plus.
- **Découpage de KalGames** en plusieurs plugins (`REGLES.md`, section 2) : décidé le 24/09/2026, pas commencé.
  À faire progressivement, en commençant par le Rush. Le menu du hub deviendra aussi son propre plugin (`KG_Menu`,
  annoncé par LeKiwi06 le 24/09/2026) : les prises `MenuEntry` de KalGames 1.13.0 le suivront. À traiter à cette
  étape (demande de LeKiwi06, vu en test le 24/09/2026) : dans la liste des parties Bingo, **le pseudo affiché
  n'est pas celui de l'hôte** - cause : le texte `bingo.party-entry` (KG_Bingo, `BingoMenus.bingoPartyButton`)
  contient le pseudo en dur, et KalGames écrit dans son `lang.yml` le texte d'une clé absente la 1re fois qu'il
  s'affiche (`Lang.java`, ligne 54) : le pseudo du 1er hôte affiché y est resté figé. Corriger avec un
  paramètre `<host>` ET une nouvelle clé (ou retirer la ligne du `lang.yml` du serveur). Rendre aussi plus clair
  le nombre de joueurs par équipe (« 2x4 équipes » est ambigu). Même principe prévu pour les modules de KaliumCore
  (annoncé par LeKiwi06 le 24/09/2026 : `KG_Reward`, `KG_Economy`, `KG_Claim`, `KG_Sethome`...).

## Points ouverts / limites connues (rien de bloquant)

- KalBingo : 18 anciens dossiers `bingo_<uuid>_<n>` de parties terminées avant 0.1.22 restent dans
  `Kixster SMP/dimensions/minecraft/` : à supprimer par l'humain s'il le souhaite.
- Création d'une map Bingo : bloque Kixster ~8 s par map (génération synchrone du monde). Proposé, pas traité.
- Un joueur Bingo déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle de salle d'attente Bingo : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente Bingo : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris).
- La pré-génération Bingo ne tient pas compte de `instances.max-simultaneous-games`.
- Éléments inutilisés, à supprimer lors d'une prochaine version (commentaires et journaux obsolètes déjà corrigés
  le 24/09/2026, dans les versions non déployées) : `bingo.max-party-size` dans KG_Bingo.
- **Jeton du relais** : l'ancien jeton, publié dans le dépôt public, a été remplacé le 24/09/2026 sur les 3
  serveurs (proxy `relay.properties`, kal-games et Kixster `config.yml`) ; le nouveau n'est écrit que sur les
  serveurs, jamais dans le dépôt. Code nettoyé le même jour (KaliumRelay 1.1.1, KalGames 1.12.4, KalBingo 0.1.23 :
  plus aucun jeton dans le code, le relais ne l'affiche plus) : voir « Versions compilées, non déployées ».
- Proxy : `geyserupdater-spigot.jar` (plugin Spigot) dans les plugins Velocity → erreur au démarrage, sans
  conséquence. Non touché (pas demandé).
- Réglages utiles côté Kixster (`plugins/KalBingo/config.yml`) : `instances.pregeneration-radius-blocks` (200),
  `instances.pregeneration-stagger-seconds` (10), `game.post-game-lobby-timeout-seconds` (600),
  `game.no-players-abandon-after-seconds` (600).
- KalGames, réglages de charge : `arenas.capture-blocks-per-tick` (30 000), `instances.wipe-blocks-per-tick`
  (20 000), `instances.paste-blocks-per-tick` (40 000).

## Compiler sur un PC (depuis le dépôt git)

Prérequis : Java 21 ou plus (testé avec Temurin 25) et Git Bash sous Windows.

```sh
sh telecharger-outils.sh      # une seule fois par machine : compilateur ecj + bibliothèques dans outils-build/
sh KG_BingoGame/build.sh      # -> sortie/KG_BingoGame-<version>.jar (idem pour les autres plugins)
```

`outils-build/` et `sortie/` sont ignorés par git. Versions : ecj 3.46.100, paper-api 26.2.build.123 (et ses
dépendances, voir le script). Vérifié le 24/09/2026 : les 5 jars recompilés sont identiques octet pour octet à
ceux de `jars-deployes/` (hors manifeste). `.gitattributes` impose des fins de ligne LF : sans lui, Git pour
Windows convertit les config.yml en CRLF et les jars ne sont plus identiques.

## Compiler (espace de travail cloud de Claude)

Sans dossier `outils-build/` à la racine, les `build.sh` utilisent `/tmp/claude-0/` et écrivent dans
`/mnt/user-data/outputs/`. Outils : archive `KalProjet-sauvegarde-2026-09-23.zip` si disponible
(`outils-build/*` à copier dans `/tmp/claude-0/`), sinon lancer `sh telecharger-outils.sh` dans le dépôt cloné.

## Déployer (WinSCP)

Règles : `REGLES.md`, section 4.

1. Mettre le jar à envoyer dans un dossier du PC (ex. Téléchargements).
2. Dans WinSCP, onglet du bon serveur, panneau distant sur `/plugins/`, **Ctrl+R pour actualiser** (le panneau
   distant peut afficher un état périmé) ; vérifier que le jar en place est celui de `jars-deployes/`.
3. Sélectionner l'ancien jar (vérifier nom et taille dans la barre d'état), Maj+F6, chemin
   `/plugins/_removed-<plugin en minuscules>-<ancienne version>/<ancien jar>`, Entrée.
4. Panneau local : sélectionner le nouveau jar, F5, OK. Actualiser et vérifier qu'il n'y a qu'un seul jar du plugin.
5. L'humain redémarre le serveur (pas d'accès console/RCON, uniquement SFTP).
6. Mettre à jour `jars-deployes/` et le tableau « Versions en service » ci-dessus, commit + push.

Pièges connus : ne pas double-cliquer sur un fichier (ouvre l'éditeur interne — refermer SANS enregistrer) ;
F6 déplace en téléchargeant puis supprimant, utiliser Maj+F6 ; taper du texte quand la liste a le focus lance
une recherche, pas une navigation ; autoriser `Textinputhost` (clavier tactile Windows) si des clics échouent.

# Comptes rendus signés

Format : `### <aaaa-mm-jj> — <pseudo>`. Un seul compte rendu par personne ici ; les précédents vont dans
`archive_reprise.md`.
