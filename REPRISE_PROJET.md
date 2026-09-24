# Reprise du projet KaLium

Deux parties (voir `REGLES.md`, section 5) :
- **Partie permanente** : état actuel du projet, tenu à jour à chaque session, jamais archivé.
- **Comptes rendus signés** : le dernier compte rendu de chacun ; les plus anciens sont dans `archive_reprise.md`.

Le détail technique de chaque version est dans `<plugin>/JOURNAL.md`. Documents hors dépôt, chez l'utilisateur :
`to_do_list_Kal_Games_Bingo.txt` (cahier des charges Bingo), `Procedure-Claude-controle-PC-deploiement.md`.

# Partie permanente

## Architecture du réseau

| Rôle | Nom Velocity | Hôte SFTP (onglet WinSCP) | Adresse de jeu | Plugin(s) à nous |
|---|---|---|---|---|
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | port 25370 | KaliumRelay (relais HTTP, port 46199) |
| Lobby | `lobby` | `lobby@7002.mystrator.com` | 91.197.6.152:28618 | KLM_Menu |
| **Hub mini-jeux (Kal-Games)** | `kal-games` | `KalGames2@7001.mystrator.com` (onglet « KalGames2 ») | 91.197.6.24:22142 | KalGames, KG_Menu, KG_Bingo, KG_ScoreBoards, KLM_Menu |
| **Bingo + jeux gourmands** (Manhunt...) | `serveur-jeux` | `serveurjeux@7015.mystrator.com` | 91.197.6.65:22470 | KG_BingoGame, KLM_Menu (boussole désactivée) |
| Kixster SMP (rendu au SMP le 24/09/2026) | `kixster` | `kixster@7003.mystrator.com` | 91.197.6.212:29599 | KLM_Menu (boussole désactivée, menu par `/menu`) |
| Event (ancien hub kal-games, à reconvertir) | `event` | `kalgames@7021.mystrator.com` | 51.254.174.133:21088 | ancien hub : KalGames 1.15.1, KG_Bingo, KG_ScoreBoards, KLM_Menu (à nettoyer) |
| Serveur de test survie | `Kanvas` (nom Velocity vu le 24/09/2026 à 17 h ; ex `kal-test-dev`, destination de la boussole du lobby à mettre à jour) | `Kal-Test-Dev@5038.mystrator.com` | 91.197.6.215:21621 | KaliumCore (projet en pause), KLM_Menu |

Machines Minestrator : **machine 1** = proxy, lobby, Event (ex kal-games), Kixster ; **machine 2** = Kal-Test-Dev ;
**machine 3** = Kal-Games (ex KalGames2), Serveur Jeux (serveurs créés le 24/09/2026).

**Migration du 24/09/2026** (décidée par Maxster33, faite par son Claude) : le hub kal-games a été **copié en entier**
sur KalGames2 et Kixster (Bingo) **en entier** sur Serveur Jeux (mondes, données des joueurs, classements, arènes,
configs, plugins) ; les originaux sont intacts (arrêtés). Noms Velocity : `kal-games` → KalGames2, `event` → ancien
kal-games, `kixster` → Kixster, `serveur-jeux` inchangé (l'ancien nom `Bingo` et `kalgames2` n'existent plus).
**Tout déploiement pour le hub ou le Bingo se fait désormais sur KalGames2 (onglet 7001) et Serveur Jeux (7015)**,
plus sur kalgames@7021 ni kixster@7003. Réglages propres aux nouveaux serveurs : ports de jeu, ports voicechat,
KG_Bingo `bingo.server-name: serveur-jeux`, KG_BingoGame `network.self-server-name: "serveur-jeux"` (clé ajoutée à la
main) et `network.kal-games-server-name: "kal-games"`. Copies de départ sur le PC de Maxster33 :
`Téléchargements\migration-2026-09-24\` ; ancien contenu des nouveaux serveurs : `_removed-avant-migration-2026-09-24/`.

**Raccordement d'un serveur Paper au proxy** : `config/paper-global.yml` → `proxies.velocity` : `enabled: true`,
`online-mode: true`, `secret` = contenu de `forwarding.secret` du proxy (jamais dans le dépôt) ; `server.properties` :
`online-mode=false` ; Floodgate avec la **même `key.pem`** que le proxy (le proxy envoie les données Bedrock :
`send-floodgate-data: true`, sans elle les joueurs Bedrock sont expulsés). Une fois raccordé, le serveur refuse
les connexions directes (passage obligatoire par le proxy).
⚠️ **Le panneau Minestrator réécrit la section `proxies.velocity` de `paper-global.yml` à chaque démarrage** : le
transfert Velocity et son secret se règlent **dans le panneau** de chaque serveur (fait par Maxster33 le 24/09/2026
pour KalGames2, Serveur Jeux et Kal-Test-Dev), une modification du fichier seul est annulée au redémarrage
(symptôme en jeu : « Votre serveur n'a pas envoyé de requête de transfert vers le proxy »). Raccordement
confirmé par Maxster33 le 24/09/2026.

**Ports voicechat** (UDP, attribués par Minestrator) : proxy 44301, lobby 43841, Kixster 40046, kal-games 40002,
Kal-Test-Dev 45595, Serveur Jeux 43131, KalGames2 43374.

- **KLM_Menu** (tous les serveurs Paper, anciennement KaliumMenu) : couche profonde des interfaces - navigation
  entre serveurs et boussole, boîte à outils des menus (`fr.kalium.menu.api`), catalogue « Interfaces » où les
  plugins déclarent leurs interfaces (découvertes au démarrage). Boussole désactivée sur Kixster (objectif du Bingo).
- **KG_Menu** (kal-games, déployé le 24/09/2026) : menu du serveur kal-games (liste des jeux, Paramètres,
  objet « Mini-jeux » du hub), alimenté par les plugins de kal-games qui s'y déclarent. Hiérarchie : KLM_Menu → menu
  de chaque serveur → interfaces des jeux.
- **KalGames** : le hub (arrivée, protections de zone) et les mini-jeux (PvP Kit, Parcours, Course de bateau, Rush ;
  Hunger Games, Manhunt et Build Battle configurables mais sans moteur). Chaque partie joue sur une copie de l'arène
  collée dans le monde vide `kalgames:instances`. Dépend de KG_ScoreBoards et KLM_Menu (et de KG_Menu à partir de 1.16.0).
- **KG_ScoreBoards** : classements (général, du mois, archives, panneaux flottants du hub), sortis de KalGames ;
  chaque plugin y inscrit ses classements. Classements Bingo et archives détaillées : à venir (étape B).
- **KG_Bingo** : partie Bingo du hub : créer une partie (équipes, taille, durée, mode, composition de la grille) ou
  rejoindre une partie listée ; transfère les joueurs vers Kixster.
- **KG_BingoGame** (anciennement KalBingo) : tout le jeu Bingo sur Kixster - salle d'attente, un overworld + Nether +
  End par équipe (même seed), grille 5×5 (200 objectifs), barème en temps réel, modes Bingos / Blackout, nulle,
  carte de la grille en main secondaire, résumé de fin de partie, parties conservées au redémarrage.
- **Communication kal-games ↔ Kixster** : deux chemins en parallèle.
  1. Canal BungeeCord "Forward" — ne livre un message QUE si au moins un joueur est connecté sur le serveur
     CIBLE (limite du protocole Minecraft, source de plusieurs bugs passés).
  2. Relais HTTP KaliumRelay (sur le proxy) — indépendant des joueurs. `/assignment/<clé>` (POST dépose, GET lit
     et efface, expire en 2 min) et `/active-game/<uuid>` (joueur en partie, pour la reconnexion directe).
     Jeton : `relay-token` dans les config.yml de KG_Bingo (`bingo.`, kal-games) et KG_BingoGame (`network.`,
     Kixster) ; `relay.properties` sur le proxy. Jamais dans le dépôt.
- **Signal "partie fermée"** (Bingo démarré/annulé → retiré de la liste kal-games) : Forward + clé relais
  `party-closed-<gameId>` republiée chaque minute pendant 6 h ; kal-games interroge le relais toutes les 5 s.

## Versions en service

Copie exacte de chaque jar dans `jars-deployes/`.

| Serveur | Jar | Test en jeu |
|---|---|---|
| **Serveur Jeux** (`serveur-jeux`, Bingo) | copie complète de Kixster du 24/09/2026 + **`KG_BingoGame-0.6.0.jar`** (déployée le 24/09/2026 à 17 h 15 par LeKiwi06 ; 0.5.0 rangée dans `_removed-kg_bingogame-0.5.0-b/`) + `KLM_Menu-2.0.0.jar` (boussole désactivée) + Floodgate, BedrockSkinRestorer, VelocityCommandForward. Config : `network.self-server-name: "serveur-jeux"`, `network.kal-games-server-name: "kal-games"`, `lobby.max-size: 256`, `lobby.blocks-per-tick: 5000` (vérifié le 24/09/2026), `instances.pregeneration-radius-blocks: 150` | 0.6.0 testée et confirmée par LeKiwi06 le 24/09/2026 (préparation des maps fluide, partie complète, Bedrock) ; à confirmer : Nether / End, 2 parties simultanées |
| **Kal-Games** (`kal-games`, hub, machine 7001, ex KalGames2) | copie complète de kal-games du 24/09/2026, puis le 24/09/2026 à 17 h 13 (LeKiwi06) : `KalGames-1.16.0.jar` (remplacé à 19 h 47) + **`KG_Menu-1.0.0.jar` + `KLM_Menu-2.1.0.jar` + `KG_Bingo-1.3.0.jar` (`bingo.server-name: serveur-jeux`) + `KG_ScoreBoards-1.2.0.jar`** (anciens jars et configs dans les `_removed-…` de chaque plugin) + Floodgate, BedrockSkinRestorer, VelocityCommandForward, GrimAC ; **24/09/2026 à 19 h 47 : `KalGames-1.17.0.jar` + `KG_BoatRace-1.0.0.jar`** (la course de bateau sort de KalGames) | testé et confirmé par LeKiwi06 le 24/09/2026 (menus, boussole, course de bateau, Parkour, classements, Bingo, Java et Bedrock) ; non testé : Rush, capture d'arène |
| lobby | `KLM_Menu-2.0.0.jar` (destinations au 24/09/2026 : `kixster` (désactivée), `kal-games`, `serveur-jeux`, `kal-test-dev`, `event` (désactivée) ; activer / désactiver en jeu : menu > Paramètres) | non testé (catalogue « Interfaces » ; nouvelles destinations) |
| proxy | `KaliumRelay-1.1.1.jar` (déployé le 24/09/2026) | relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kal-Test-Dev | `KaliumCore-1.4.0.jar` + `KLM_Menu-2.0.0.jar` (24/09/2026, rôle `backend` par défaut) | non testé (projet en pause ; raccordement au proxy du 24/09 non testé) |
| Kixster (`kixster`) | remis dans l'état d'avant le Bingo le 24/09/2026 : monde d'origine `Kixster SMP` (ex `Kixster SMP_bak`, dernière sauvegarde 23/09 00:11), plugins SMP du 14/09 + WorldEdit 7.4.6-beta (gardé) + `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) ; tout le Bingo rangé dans `/_removed-bingo-2026-09-24/` et `/plugins/_removed-bingo-2026-09-24/` | non testé |
| Event (`event`, ancien kal-games) | inchangé depuis la migration (arrêté le 24/09/2026) ; reconversion à faire | - |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

Aucune (le 24/09/2026 à 17 h 15, tout ce qui était compilé a été déployé par LeKiwi06, voir le tableau ci-dessus).

## Chantiers en cours

- **Interfaces** (décidé par LeKiwi06 le 24/09/2026) : KLM_Menu 2.0.0 déployé partout. À déployer sur kal-games :
  KG_Menu 1.0.0 + KLM_Menu 2.1.0 + KalGames 1.16.0 + KG_Bingo 1.3.0 + KG_ScoreBoards 1.2.0 (voir « Versions
  compilées, non déployées »). Ensuite **phase 2** : accès des opérateurs aux interfaces des autres serveurs via
  KaliumRelay (menus faits avec la boîte à outils ; menus en coffre et actions sur le joueur impossibles à distance).
  Plus tard : un menu par serveur pour la créa, le skyblock, la survie.
- **KG_ScoreBoards, étape B** : classements Bingo individuels solo / duo / trio / squad + blackout (tout enregistrer :
  points solo et d'équipe, taille d'équipe, mode, objectifs en 1er, bingos, victoire / défaite / nulle ; parties sans
  adversaire comptées), points à décimales (format : 6 chiffres max, décimale jusqu'à 1 million, puis K / M / Md
  avec 3 décimales max), **journal des gains** (JSON Lines par mois, pour un futur bot Discord) et **fiche de chaque
  partie** (avec l'heure de chaque objectif). **Étape C** : KG_BingoGame envoie les résultats via KaliumRelay.
- **Bingo, bug à corriger** : dans la liste des parties (KG_Bingo), **le pseudo affiché n'est pas celui de
  l'hôte** - le texte `bingo.party-entry` contient le pseudo en dur et le `lang.yml` de KalGames l'a figé à la 1re
  apparition. Corriger avec un paramètre `<host>` ET une nouvelle clé ; rendre aussi plus clair le nombre de joueurs
  par équipe (« 2x4 équipes » est ambigu).
- **Rush (KalGames)** : jouable, jamais testé en jeu (arène à configurer : salle d'attente + 11 points par base).
  Voir `KalGames/JOURNAL.md`, sections 1.11.0 à 1.12.3.
- **Découpage de KalGames** en plugins par jeu (`REGLES.md`, section 2), progressivement, en commençant par le Rush.
  Même principe prévu pour KaliumCore (`KG_Reward`, `KG_Economy`, `KG_Claim`, `KG_Sethome`...). Protections de
  zone du hub : restent dans KalGames pour l'instant.
- **Nettoyage des serveurs** : ne garder que les 2 derniers `_removed-…` par plugin (+ anciens `.bak`, dossiers
  `-token`) : après le découpage des plugins, suppression par l'humain.

## Points ouverts / limites connues (rien de bloquant)

- **KalGames2 et Serveur Jeux repassés en Paper 26.2-121** (24/09/2026, 13 h) : créés en Paper 26.3 alpha, ils ne
  démarraient pas (WorldEdit 7.4.5 incompatible → crash au démarrage). `server.jar` remplacé par le
  `paper-26.2-121.jar` du lobby ; jar 26.3 et monde généré en 26.3 rangés dans `_removed-paper-26.3/`. Si le panneau
  Minestrator réinstalle la 26.3 au démarrage, choisir la 26.2 dans le panneau.
- **Raccordement du 24/09/2026 (Maxster33)** : actif seulement après redémarrage de KalGames2, Serveur Jeux,
  Kal-Test-Dev, du lobby (voicechat, destinations KLM_Menu) puis du proxy. À vérifier dans les journaux : Floodgate
  sans erreur de clé, KLM_Menu chargé, connexion par le proxy (`/server kalgames2`...).
- voicechat : ports réglés sur lobby, KalGames2, Serveur Jeux et Kal-Test-Dev ; **pas encore** sur Kixster (40046)
  ni kal-games (40002) ; rien sur le proxy (44301 : aucun plugin voicechat sur Velocity pour l'instant). `voice_host`
  laissé vide partout : derrière le proxy, les clients risquent de viser l'IP du proxy → à décider (voice_host
  `<ip>:<port>` par serveur, ou voicechat sur le proxy).
- Kal-Test-Dev garde son propre Geyser-Spigot (crossplay direct) : devenu inutile derrière le proxy (Geyser tourne
  sur le proxy) ; non retiré (pas demandé). Le lobby a aussi un `Geyser-Spigot.jar` qui ne se charge pas (aucun dossier).
- Lobby : la destination KLM_Menu `Kixster` (désactivée) vise un nom qui n'existe plus dans Velocity depuis le
  renommage en `Bingo`.

- KG_BingoGame : 18 anciens dossiers `bingo_<uuid>_<n>` de parties terminées avant 0.1.22 restent dans
  `Kixster SMP/dimensions/minecraft/` : à supprimer par l'humain s'il le souhaite.
- Création d'une map Bingo : bloque Kixster ~8 s par map (génération synchrone du monde). KG_BingoGame 0.6.0 (non
  déployée) : zone de spawn plus préparée, une seule map créée à la fois pour tout le serveur ; blocage restant à
  mesurer.
- Un joueur Bingo déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle de salle d'attente Bingo : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente Bingo : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris).
- La pré-génération Bingo ne tient pas compte de `instances.max-simultaneous-games` (0.6.0 : sans conséquence de
  charge, toutes les parties passent par une seule file de génération).
- Éléments inutilisés, à supprimer lors d'une prochaine version (commentaires et journaux obsolètes déjà corrigés
  le 24/09/2026, dans les versions non déployées) : `bingo.max-party-size` dans KG_Bingo.
- **Jeton du relais** : l'ancien jeton, publié dans le dépôt public, a été remplacé le 24/09/2026 sur les 3
  serveurs ; le nouveau n'est écrit que sur les serveurs. Plus aucun jeton dans le code (KaliumRelay 1.1.1).
- Vus dans les journaux, non touchés (pas demandé) : proxy `geyserupdater-spigot.jar` (plugin Spigot sur
  Velocity) ; kal-games : config de ConditionalEvents invalide (ligne 10) ; Kixster : AnvilUnlocker sans
  ProtocolLib, deux voicechat (2.6.23 et 2.6.24), Geyser sur le serveur au lieu du proxy.
- Têtes des joueurs (menus du Bingo) : invisibles sur Bedrock (Geyser) - accepté.
- Réglages utiles côté Kixster (`plugins/KG_BingoGame/config.yml`) : `instances.pregeneration-radius-blocks` (200),
  `instances.pregeneration-stagger-seconds` (10), `game.post-game-lobby-timeout-seconds` (600),
  `game.no-players-abandon-after-seconds` (600) ; à partir de 0.6.0 : `instances.pregeneration-chunks-per-second`
  (20) et `instances.pregeneration-max-chunks-in-flight` (2), à ajouter à la main pour accélérer / ralentir.
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

### 2026-09-24 — Maxster33

**Dépôt** : import du bundle `Kalium-depot.bundle` sur `Maxster33/Kalium_depot` (main + 5 étiquettes), dossier
`historique-conversations/` créé.

**Raccordement de nouveaux serveurs au proxy (étape 1 de la migration)** - fait par Claude via WinSCP, sauvegardes
dans `_removed-config-2026-09-24/` (racine de chaque serveur) et `plugins/_removed-klm_menu-2.0.0/`,
`plugins/_removed-voicechat-2.6.23/` (lobby) :
- Proxy : `kalgames2`, `serveur-jeux`, `kal-test-dev` ajoutés dans `velocity.toml` (le renommage `Kixster` →
  `Bingo` fait entre-temps par LeKiwi06 est conservé).
- KalGames2 et Serveur Jeux (Paper neufs) : transfert Velocity activé (`paper-global.yml`), `online-mode=false`,
  liste blanche désactivée ; plugins communs du lobby **avec leurs configurations** (Floodgate + clé du proxy,
  BedrockSkinRestorer, VelocityCommandForward, LuckPerms (base H2 du lobby copiée), voicechat, WorldEdit, WorldGuard,
  ConditionalEvents, PyxelRegions) + KLM_Menu 2.0.0 ; voicechat : ports 43374 / 43131.
- Kal-Test-Dev : transfert Velocity activé (il était en `online-mode=false` SANS proxy : tout le monde pouvait s'y
  connecter sous n'importe quel pseudo), clé Floodgate remplacée par celle du proxy, KLM_Menu 2.0.0, voicechat 45595.
- Lobby : KLM_Menu, 3 nouvelles destinations (désactivées, à activer en jeu) ; voicechat : port 24454 → 43841.
- Non fait : voicechat de Kixster / kal-games / proxy ; redémarrages (humain).
- Erreurs de Claude : deux valeurs secrètes (secret Velocity, `management-server-secret`) affichées dans la
  conversation pendant une comparaison de fichiers (jamais publiées) ; un jar ouvert par erreur dans l'éditeur de
  WinSCP, refermé sans enregistrer.

**Migration complète (après-midi du 24/09/2026)** - LeKiwi06 avait libéré KG_Bingo / KG_BingoGame :
- kal-games et Kixster arrêtés par Maxster33, **copiés en entier** (912 Mo et 791 Mo, dossiers cachés `.paper`,
  `.cache`, `.ai-backups` compris) sur KalGames2 et Serveur Jeux ; ancien contenu des nouveaux serveurs dans
  `_removed-avant-migration-2026-09-24/`. Vérifié par comparaison fichier par fichier (seules différences : les
  réglages ci-dessous).
- Réglages : ports de jeu et voicechat des nouveaux serveurs, KG_Bingo `bingo.server-name: serveur-jeux`,
  KG_BingoGame `network.self-server-name: "serveur-jeux"` (clé ajoutée). **Aucun changement de code** : KaliumRelay
  ne connaît aucun nom de serveur (il relaie ceux que les plugins lui donnent), KLM_Menu des serveurs de jeu renvoie
  seulement au lobby, et `kal-games` reste le nom du hub pour KG_BingoGame.
- Proxy : `kal-games` → KalGames2, `event` → ancien kal-games, `kixster` → Kixster, `serveur-jeux` ; `Bingo` et
  `kalgames2` supprimés. Lobby : destinations KLM_Menu `kixster` (désactivée), `kal-games`, `serveur-jeux`,
  `kal-test-dev`, `event` (désactivée).
- KG_BingoGame n'était plus actif sur Kixster (0.5.0 rangée à 11:57) : 0.5.0 réinstallée sur Serveur Jeux.
- Ajout de Floodgate, BedrockSkinRestorer, VelocityCommandForward sur les 2 nouveaux serveurs (absents des originaux).
- PC de Maxster33 : Java 27 installé, `telecharger-outils.sh` fait, compilation vérifiée (KaliumRelay). Sous Git Bash,
  Java 27 n'est pas dans le PATH tant que Git Bash n'a pas été relancé : `export PATH="/c/Program Files/Java/jdk-27/bin:$PATH"`.
- Travail désormais piloté par WinSCP.com + scripts (guide du Claude de LeKiwi06), plus par captures d'écran.
- **Correctifs et nettoyage (fin d'après-midi du 24/09/2026)** : KalGames2 : `PlaceholderAPIScoreboardObjectivesPlaceholder.jar`
  (extension PlaceholderAPI, pas un plugin) déplacé dans `plugins/PlaceholderAPI/expansions/`. Serveur Jeux : voicechat
  2.6.23 rangé (2.6.24 gardé), AnvilUnlocker retiré (sans ProtocolLib, inutile au Bingo). Kixster rendu au SMP : monde
  d'origine restauré, `bukkit.yml` sans générateur Bingo, KG_BingoGame + anciens `_removed-kalbingo/kg_bingogame` +
  mondes ratés (`_failed_v0.1.3`, `_crashed_0.1.4`) + monde vide du Bingo (avec `bingo_lobby`) + fichier `Depot`
  (journal envoyé par erreur à la place d'un jar le 24/09 à 12:05) rangés dans `_removed-bingo-2026-09-24/` ;
  voicechat 2.6.24 rangé ; WorldEdit gardé ; `generate-structures=false` laissé (décisions de Maxster33).- **Migration confirmée par Maxster33 le 24/09/2026** (« tout a l'air de fonctionner correctement ») après redémarrage ; journaux de démarrage de KalGames2 et Serveur Jeux : Paper 26.2-122, seules erreurs = celles déjà présentes sur les originaux (jar `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` illisible sur le hub ; 2 voicechat et AnvilUnlocker sans ProtocolLib sur le Bingo), non corrigées (pas demandé).
- À faire : nettoyage de Kixster (SMP) et
  reconversion d'Event ; KG_BoatRace (LeKiwi06) à déployer sur KalGames2.