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
| Lobby | `lobby` | `lobby@7002.mystrator.com` | KLM_Menu |
| Hub mini-jeux | `kal-games` | `kalgames@7021.mystrator.com` | KalGames, KG_Bingo, KG_ScoreBoards, KLM_Menu (KG_Menu à venir) |
| Serveur Bingo (dédié) | `kixster` | `kixster@7003.mystrator.com` | KG_BingoGame, KLM_Menu (boussole désactivée) |
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
| Kixster | `KG_BingoGame-0.5.0.jar` + `KLM_Menu-2.0.0.jar` (boussole désactivée : objectif du Bingo) (KG_BingoGame 0.3.0 à 0.5.0 déployées le 24/09/2026 ; KalBingo renommé, données dans `plugins/KG_BingoGame/`, `bukkit.yml` : `generator: KG_BingoGame` ; config.yml du serveur : `lobby.max-size: 256`, `lobby.blocks-per-tick: 30000`) | 0.3.0 testée le 24/09/2026 (annonces, coefficients, Nether/End par équipe, nulle en solo, invincibilité OK) ; 0.4.0 testée (carte, têtes, résumé : « propre et sans bugs ») ; 0.4.2 / 0.5.0 non testées (contour des objets blancs, multiplicateur du blackout, interface dans le catalogue) |
| kal-games | `KalGames-1.15.1.jar` + `KG_ScoreBoards-1.1.0.jar` + `KG_Bingo-1.2.0.jar` + `KLM_Menu-2.0.0.jar` (déployés le 24/09/2026, classements migrés dans `plugins/KG_ScoreBoards/` ; `plugins/KG_Bingo/config.yml` = copie de celui de KalGames, jeton compris) | non testé (Rush ; capture d'arène sans crash à confirmer en recapturant le parkour ; catalogue « Interfaces ») |
| lobby | `KLM_Menu-2.0.0.jar` (KaliumMenu renommé le 24/09/2026, dossier `plugins/KLM_Menu/`) | non testé (catalogue « Interfaces » ; Paramètres des téléportations non confirmés depuis la 1.5.0) |
| proxy | `KaliumRelay-1.1.1.jar` (déployé le 24/09/2026) | relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kal-Test-Dev | `KaliumCore-1.4.0.jar` | non testé (projet en pause) |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

À installer **ensemble sur kal-games** (serveur arrêté), dans le dépôt depuis le 24/09/2026 :

| Jar | Contenu |
|---|---|
| `KG_Menu-1.0.0.jar` | nouveau : menu du serveur kal-games, découverte des interfaces des jeux au démarrage, objet « Mini-jeux » du hub |
| `KLM_Menu-2.1.0.jar` | boussole entièrement gérée par KLM_Menu (`giveNavigation`) ; lobby et Kixster peuvent rester en 2.0.0 |
| `KalGames-1.16.0.jar` | menus du hub sortis vers KG_Menu, boussole demandée à KLM_Menu |
| `KG_Bingo-1.3.0.jar` | boutons fournis à KG_Menu |
| `KG_ScoreBoards-1.2.0.jar` | boutons fournis à KG_Menu |

Procédure : ranger chaque ancien jar dans `_removed-<plugin>-<version>/`, envoyer les nouveaux ; relire les textes
`item.games.*` du `lang.yml` de KalGames et les reprendre dans `plugins/KG_Menu/lang.yml` s'ils ont été modifiés ;
après redémarrage, vérifier dans le journal « N fournisseur(s) d'interface trouvé(s) » (KG_Menu). Détails dans les
JOURNAL. Les jars se recréent avec `sh <plugin>/build.sh`.

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

### 2026-09-24 — LeKiwi06

*(Fin de session : toutes les réservations libérées ; KG_Menu et les versions qui l'accompagnent restent à déployer.)*

**Mise en route et règles**
- Dépôt cloné sur le PC de LeKiwi06, compilation locale (`telecharger-outils.sh`, `build.sh` PC + cloud,
  `.gitattributes` LF) : jars recompilés identiques à ceux en service.
- `REGLES.md` créé et complété : réservations en **deux catégories** (utilisés actuellement / requis parfois) avec
  **demandes de créneau** auxquelles le Claude de l'autre peut répondre ; 2 plugins « utilisés » max par personne de
  13 h à 23 h (heure de Paris, `date` simple sous Git Bash) ; un plugin = un rôle ; secrets seulement sur les
  serveurs ; non destructif (`_removed-…`, **2 derniers gardés par plugin**, suppression par l'humain).
  `REPRISE_PROJET.md` restructuré, ancienne version dans `archive_reprise.md`.
- Sécurité : jeton du relais remplacé sur les 3 serveurs par LeKiwi06 (jamais écrit dans le dépôt ni la
  conversation) ; KaliumRelay 1.1.1 sans jeton dans le code.
- WinSCP : tous les serveurs enregistrés avec mot de passe (Kal-games, Kal-test-dev, Kixster, Lobby, Proxy Velocity) ;
  Claude déploie par ces sites sans jamais taper de mot de passe ; l'humain arrête / redémarre.

**Bingo (terminé et déployé)**
- Découpage : `KG_Bingo` (hub) + KalBingo renommé `KG_BingoGame` (Kixster migré).
- Nouveau Bingo (KG_BingoGame 0.3.0 → 0.5.0) : 200 objectifs validés ; modes Bingos (3 à 12, chrono) et Blackout ;
  composition de la grille au choix ; barème en temps réel (1/3/5/10, 1re équipe, coefficients additifs, victoire,
  classement cumulé équipe et solo) ; blackout gagné en moins de 2 h : ×2 / ×1,5 (difficile + extrême : ×5 / ×2) ;
  nulle par vote, « Égalité » au score, abandon (10 min), inactivité (5 min), invincibilité de fin, keepInventory,
  papier verrouillé ; carte de la grille en main secondaire (icônes du jeu officiel, contour des objets blancs),
  couleurs d'équipe A rouge / B bleu / C jaune / D verte, têtes des coéquipiers, résumé de fin de partie ; mode solo
  gardé pour le speedrun.
- Testé par LeKiwi06 : « propre et sans bugs ». Limite acceptée : têtes invisibles sur Bedrock. Le « Nether commun »
  vu en test venait de l'ancienne 0.1.20.

**Classements : KG_ScoreBoards**
- Plugin autonome (étape A faite et déployée) : classements sortis de KalGames, données migrées sur kal-games
  (`stats.yml`, `boards.yml`), `StatsService` compilé identique. Panneaux du hub : Paramètres > Mini-jeux Kal-Games >
  mini-jeu > Classements > Panneaux dans le hub.
- Décisions pour la suite (étape B, non faite) : toutes les parties enregistrées, par joueur et par catégorie (solo /
  duo / trio / squad, blackout) : points, objectifs en 1er, bingos, victoire / défaite / nulle ; parties sans
  adversaire comptées ; seuls les classements individuels affichés au début ; archives détaillées (futur bot
  Discord) ; résultats du Bingo transmis par KaliumRelay ; format des scores : 6 chiffres max, puis K / M / Md.

**Interfaces : KLM_Menu et KG_Menu**
- Hiérarchie décidée : **KLM_Menu** (tous les serveurs : navigation, boussole, boîte à outils des menus, catalogue
  « Interfaces ») → **menu de chaque serveur** (KG_Menu pour kal-games ; plus tard créa, skyblock, survie) →
  interfaces des jeux, **découvertes au démarrage**. Les mini-jeux gardent leurs propres écrans. Protections de zone
  du hub : restent dans KalGames pour l'instant.
- KLM_Menu 2.0.0 (KaliumMenu renommé) déployé sur kal-games, lobby et Kixster (boussole désactivée sur Kixster :
  c'est un objectif du Bingo). Bug corrigé : plus de boussole au hub (KalGames 1.15.1).
- Compilés, **non déployés** : KG_Menu 1.0.0, KLM_Menu 2.1.0, KalGames 1.16.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0
  (à installer ensemble sur kal-games, voir leurs JOURNAL).
- Prévu : phase 2 (accès des admins aux interfaces des autres serveurs, via KaliumRelay ; limite : menus en coffre et
  actions sur le joueur impossibles à distance).

**À faire / à savoir**
- Liste des parties Bingo : pseudo de l'hôte figé dans `lang.yml` et affichage des équipes ambigu → à corriger
  (KG_Bingo / KG_Menu).
- À tester : pseudos dans les annonces à 3 comptes ou plus, nulle en groupe, multiplicateur du blackout, catalogue
  « Interfaces », KG_Menu.
- Tri des anciens dossiers `_removed-…` et `.bak` : après le découpage des plugins.
- Vus dans les journaux, hors de notre travail : config de ConditionalEvents invalide (kal-games), AnvilUnlocker sans
  ProtocolLib, 2 voicechat et Geyser sur Kixster, geyserupdater-spigot sur le proxy.
- Points 2 et 3 de la première lecture : à rappeler quand LeKiwi06 le demande.
- Erreurs de Claude, corrigées : commits incomplets (étape B du Bingo, journaux de KG_Menu), horodatages en UTC,
  journaux de serveur posés un moment dans le dossier du dépôt / dans Documents, deux dossiers de Kal-test-dev
  listés par erreur, ancien nom KaliumMenu non recherché avant le renommage (boussole du hub).
