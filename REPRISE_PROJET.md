# Reprise du projet KaLium (KalBingo / KalGames) — sauvegarde du 23/09/2026 (mise à jour après Rush)

Document de reprise pour la prochaine session. **À donner à Claude en premier** (avec l'archive
entière) : il contient l'état exact du projet, ce qui est déployé, comment compiler et déployer, et
les points encore ouverts. L'historique détaillé de chaque plugin est dans son `JOURNAL.md`
(`sources/<plugin>/JOURNAL.md`), à lire ensuite pour le plugin concerné.

Autres documents utiles déjà présents dans le dossier Téléchargements de l'utilisateur :
`to_do_list_Kal_Games_Bingo.txt` (cahier des charges Bingo) et
`Procedure-Claude-controle-PC-deploiement.md` (procédure de contrôle du PC / déploiement).

## Règles du projet (toujours en vigueur)

- Ne jamais construire quelque chose qui n'a pas été explicitement demandé.
- En cas de doute technique ou d'ambiguïté : proposer / poser la question AVANT d'implémenter,
  jamais deviner.
- Déploiement non destructif : l'ancien jar est toujours déplacé (jamais supprimé) dans
  `/plugins/_removed-<plugin>-<ancienne version>/` avant d'envoyer le nouveau.
- Les messages et textes en jeu sont en français.

## Architecture du réseau

| Rôle | Nom Velocity | Hôte SFTP (onglet WinSCP) | Plugin(s) à nous |
|---|---|---|---|
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | KaliumRelay 1.1.0 (relais HTTP, port 46199) |
| Hub mini-jeux | `kal-games` | `kalgames@7021.mystrator.com` | KalGames 1.12.3, KaliumMenu 1.5.0 |
| Serveur Bingo (dédié) | `kixster` | `kixster@7003.mystrator.com` | KalBingo 0.1.22 |
| Serveur de test survie | `Kal-Test-Dev` | — | KaliumCore 1.4.0 (projet en pause, voir son JOURNAL) |

- **KalGames** : le hub. Menu "Bingo" pour créer une partie (équipes, taille, durée ≤ max défini par
  les opérateurs) ou rejoindre une partie listée (parties encore en salle d'attente, 6 max affichées).
  Transfère les joueurs vers Kixster.
- **KalBingo** : tout le jeu Bingo, sur Kixster. Salle d'attente (modèle capturé par l'admin via
  `/menu`, collé sur 4 emplacements), choix des équipes par l'hôte, une map (monde) par équipe avec la
  même seed, grille 5×5, validation, score, HUD, fin de partie, abandon, persistance des parties en
  cours (survivent à un redémarrage).
- **Communication kal-games ↔ Kixster** : deux chemins en parallèle.
  1. Canal BungeeCord "Forward" — ne livre un message QUE si au moins un joueur est connecté sur le
     serveur CIBLE (limite du protocole Minecraft, source de plusieurs bugs passés).
  2. Relais HTTP KaliumRelay (sur le proxy) — indépendant des joueurs. Endpoints :
     `/assignment/<clé>` (stockage clé → texte, POST dépose, GET lit et efface, expire en 2 min) et
     `/active-game/<uuid>` (joueur "en partie" sur un serveur, pour la reconnexion directe).
     Jeton : `relay-token` dans les config.yml de KalGames (`bingo.`) et KalBingo (`network.`).
- **Signal "partie fermée"** (démarrée/annulée → retirée de la liste kal-games) : Forward + clé relais
  `party-closed-<gameId>` republiée chaque minute pendant 6 h ; kal-games interroge le relais toutes
  les 5 s.

## Versions déployées au 23/09/2026 (toutes vérifiées dans WinSCP après actualisation forcée)

- Kixster `/plugins/KalBingo-0.1.22.jar` (Nether et End propres à chaque équipe, keepInventory en partie,
  suppression des maps corrigée ; salle d'attente jusqu'à 256 blocs par côté ; config.yml déployé modifié : `lobby.max-size: 256`, `lobby.blocks-per-tick: 30000`)
- kal-games `/plugins/KalGames-1.12.3.jar` (Rush jouable, NON encore testé en jeu ; 1.12.3 = correctif du crash
  pendant la capture d'arène, voir son JOURNAL)
- proxy `/plugins/KaliumRelay-1.1.0.jar`
- kal-games `/plugins/KaliumMenu-1.5.0.jar` (bouton Paramètres des téléportations). Lobby
  (`lobby@7002.mystrator.com`, onglet WinSCP ajouté par l'utilisateur) : KaliumMenu 1.5.0 présent.

Confirmé par l'utilisateur le 23/09/2026 (avant Rush) : "tout fonctionne très bien".

## Chantier en cours : mini-jeu Rush (KalGames 1.11.0 → 1.12.3)

Cahier des charges complet, décisions prises et fonctionnement : `sources/KalGames/JOURNAL.md`, sections
1.11.0 à 1.12.3. Code : `game/RushInstance.java` (moteur), `game/RushItems.java` (monnaies + 45 échanges),
`listener/RushListener.java`, `model/RushLayout.java` (équipes, PNJ, points d'arène). Prochaine étape : retours
de test de l'utilisateur (arène à configurer : salle d'attente + 11 points par base).

## Contenu de cette sauvegarde

- `sources/` : code source complet des 5 plugins (KalBingo, KalGames, KaliumCore, KaliumMenu,
  KaliumRelay), avec `build.sh`, `pom.xml` et `JOURNAL.md`.
- `outils-build/` : compilateur `ecj.jar` + toutes les bibliothèques (`libs/`, dont `paper-api`
  26.2.build.123) utilisés pour compiler — permet de recompiler à l'identique sans rien retélécharger.
- `jars-deployes/` : les jars actuellement en service (copie de secours).

## Compiler sur un PC (depuis le dépôt git) - mis en place le 24/09/2026

Prérequis : Java 21 ou plus (testé avec Temurin 25) et Git Bash sous Windows.

```sh
sh telecharger-outils.sh      # une seule fois par machine : compilateur ecj + bibliothèques dans outils-build/
sh KalBingo/build.sh          # -> sortie/KalBingo-<version>.jar (idem pour les autres plugins)
```

`outils-build/` et `sortie/` sont ignorés par git. Versions utilisées : ecj 3.46.100, paper-api
26.2.build.123 (et ses dépendances, voir le script). Vérifié le 24/09/2026 : les 5 jars recompilés ainsi sont
identiques octet pour octet à ceux de `jars-deployes/` (hors manifeste). Le fichier `.gitattributes` impose des
fins de ligne LF : sans lui, Git pour Windows convertit les config.yml en CRLF et les jars ne sont plus identiques.

## Compiler (dans l'espace de travail cloud de Claude)

Sans dossier `outils-build/` à la racine, les `build.sh` utilisent `/tmp/claude-0/` et écrivent dans
`/mnt/user-data/outputs/`. Pour restaurer :

```sh
unzip KalProjet-sauvegarde-2026-09-23.zip -d /home/claude/restore
mkdir -p /tmp/claude-0 && cp -r /home/claude/restore/outils-build/* /tmp/claude-0/
cp -r /home/claude/restore/sources/* /home/claude/
cd /home/claude/KalBingo && sh build.sh   # -> /mnt/user-data/outputs/KalBingo-<version>.jar
```

Chaque nouvelle version : incrémenter `VERSION=` dans `build.sh` ET `<version>` dans `pom.xml`, puis
ajouter une section au `JOURNAL.md` du plugin (ce qui a été demandé, la cause, ce qui a changé, les
limites, la date de déploiement).

## Déployer (WinSCP sur le PC de l'utilisateur)

1. Envoyer le jar dans la conversation, puis l'écrire dans `C:\Users\HP Zbook 17 G3\Downloads\`.
2. Dans WinSCP, onglet du bon serveur, panneau distant sur `/plugins/`, **Ctrl+R pour actualiser**
   (le panneau distant peut afficher un état périmé : ne jamais se fier à une liste non actualisée).
3. Sélectionner l'ancien jar (vérifier le nom et la taille dans la barre d'état), Maj+F6, chemin
   `/plugins/_removed-<plugin>-<ancienne version>/<ancien jar>`, Entrée.
4. Panneau local : Downloads (le dossier "Claude outputs" est un sous-dossier), sélectionner le
   nouveau jar, F5, OK. Actualiser et vérifier qu'il n'y a qu'un seul jar du plugin.
5. L'utilisateur redémarre le serveur (pas d'accès console/RCON, uniquement SFTP).

Pièges connus : ne pas double-cliquer sur un fichier (ouvre l'éditeur interne — refermer SANS
enregistrer) ; taper du texte quand la liste a le focus lance une recherche, pas une navigation ;
autoriser `Textinputhost` (clavier tactile Windows) si des clics échouent.

## Points ouverts / limites connues (rien de bloquant)

- KalBingo 0.1.22 (Nether / End par équipe, keepInventory) : à tester en jeu. Sur Paper 26.x les mondes ajoutés
  sont dans `Kixster SMP/dimensions/minecraft/` ; 18 anciens dossiers `bingo_<uuid>_<n>` de parties terminées
  avant 0.1.22 y restent (jamais effacés à cause de l'ancien bug) : à supprimer par l'utilisateur s'il le souhaite.

- KalGames 1.12.3 (capture / effacement / collage sans chargement synchrone de chunks) : à confirmer en jeu par une
  recapture du parkour. Réglages : `arenas.capture-blocks-per-tick` (30 000), `instances.wipe-blocks-per-tick`
  (20 000), `instances.paste-blocks-per-tick` (40 000). Journaux du crash du 23/09 : `Downloads\Claude outputs\kalgames-logs\`.

- Création d'une map : bloque le serveur Kixster ~8 s par map (génération synchrone du monde par le
  jeu). Proposé à l'utilisateur, pas encore traité.
- Un joueur déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris) ; la
  protection des blocs ne couvre plus que les salles collées.
- La pré-génération ne tient pas compte de `instances.max-simultaneous-games`.
- `reconnect-during-game.abandon-after-seconds` : détection seulement, aucune action.
- KalGames `bingo.max-party-size` n'est plus utilisé (capacité = équipes × taille).
- Proxy : `geyserupdater-spigot.jar` (plugin Spigot) dans les plugins Velocity → erreur de chargement
  au démarrage, sans conséquence. Non touché (pas demandé).
- Réglages utiles côté Kixster (`plugins/KalBingo/config.yml`) :
  `instances.pregeneration-radius-blocks` (200), `instances.pregeneration-stagger-seconds` (10),
  `game.post-game-lobby-timeout-seconds` (600), `game.no-players-abandon-after-seconds` (600).
  Toute nouvelle clé doit être AJOUTÉE à la main dans le fichier déployé (le plugin ne réécrit pas
  un config.yml existant) ; redémarrage requis.
