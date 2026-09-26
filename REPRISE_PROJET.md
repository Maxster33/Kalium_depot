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
| Event : survie classique (depuis le 25/09/2026) | `event` | `kalgames@7021.mystrator.com` | 51.254.174.133:21088 | KLM_Menu (boussole désactivée, `/menu`), KS_Dimensions, KS_Enclume, KS_Villageois, KS_Crafts, KS_LootBlocs, KS_LootEntites, KS_LootPeche, KS_LootPotions (cahier des charges : `KS_Event/`) |
| Serveur de plots en créatif (Kanvas) | `Kanvas` (ex `kal-test-dev`, renommé par LeKiwi06 dans `velocity.toml` le 24-25/09/2026 ; destination `kal-test-dev` de la boussole du lobby à mettre à jour) | `Kal-Test-Dev@5038.mystrator.com` | 91.197.6.215:21621 | KV_Plots, KV_Menu, KLM_Menu (cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`) |

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
| **Serveur Jeux** (`serveur-jeux`, Bingo) | copie complète de Kixster du 24/09/2026 + **`KG_BingoGame-0.8.2.jar`** (26/09/2026 06:27 : icônes de la carte en 22 pixels, blocs en 3D ; non testé ; 0.8.1 dans `_removed-kg_bingogame-0.8.1/` ; dossier `plugins/KG_BingoGame/icons/26.2-3d/` à supprimer par l'humain) ; 0.8.1 (06:15 : bonus du 1er = moitié des points, joueurs non téléportés corrigés, barre d'action courte à 3-4 équipes, icônes de la carte en 3D, 7 objets du Nether en Normal (`objectives.yml` du serveur remplacé) ; non testé ; 0.8.0 et l'ancien `objectives.yml` dans `_removed-kg_bingogame-0.8.0/`) ; 0.8.0 (barème doublé, résultats envoyés au hub, testé) ; 0.7.8 (25/09/2026 : succès limités à la partie, XP remise à zéro et convertie en points bonus (0,1/niveau), préparation des maps corrigée et 3 fois plus rapide, salle d'attente protégée avec entités recopiées et sans barrières, positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml` ; versions précédentes dans les `_removed-kg_bingogame-…`) + `KLM_Menu-2.0.0.jar` (boussole désactivée) + Floodgate, BedrockSkinRestorer, VelocityCommandForward. Config : `network.self-server-name: "serveur-jeux"`, `network.kal-games-server-name: "kal-games"`, `lobby.max-size: 256`, `lobby.blocks-per-tick: 10000`, `instances.pregeneration-radius-blocks: 150`, `pregeneration-stagger-seconds: 5`, `pregeneration-chunks-per-second: 40` (25/09/2026) | 0.6.0 testée et confirmée par LeKiwi06 le 24/09/2026 (préparation des maps fluide, partie complète, Bedrock) ; à confirmer : Nether / End, 2 parties simultanées |
| **Kal-Games** (`kal-games`, hub, machine 7001, ex KalGames2) | copie complète de kal-games du 24/09/2026, puis (LeKiwi06) : **`KalGames-1.20.0.jar`** (26/09/2026 04:41 : Parcours sorti dans KG_Parkour ; 1.19.1 et copies de config.yml, minigames.yml, arenas.yml dans `_removed-kalgames-1.19.1/`) + **`KG_Parkour-1.0.0.jar`** (Parcours sorti de KalGames, tel quel ; testés et confirmés par LeKiwi06 le 26/09/2026) + **`KG_BoatRace-1.4.1.jar`** (course de bateau sortie de KalGames) + **`KG_ScoreBoards-1.5.0.jar`** (journal des parties, points décimaux, `/classements verifier` et `crediter`) + `KG_Menu-1.0.0.jar` + `KLM_Menu-2.1.0.jar` + `KG_Bingo-1.5.0.jar` (26/09/2026 05:23 : points du Bingo dans les classements ; testé et confirmé par LeKiwi06 le 26/09/2026 ; 1.4.0 dans `_removed-kg_bingo-1.4.0/` ; `bingo.server-name: serveur-jeux` ; pseudo de l'hôte corrigé dans la liste) ; anciens jars et configs dans les `_removed-…` de chaque plugin ; + Floodgate, BedrockSkinRestorer, VelocityCommandForward, GrimAC | testé et confirmé par LeKiwi06 les 24 et 25/09/2026 (menus, boussole, course de bateau : checkpoints, km/h, classement en direct, écarts, barème, hors-piste, anti-collision Java et Bedrock ; Parkour, classements, Bingo) ; non testé : Rush, capture d'arène |
| lobby | `KLM_Menu-2.0.0.jar` (destinations au 24/09/2026 : `kixster` (désactivée), `kal-games`, `serveur-jeux`, `kal-test-dev`, `event` (désactivée) ; activer / désactiver en jeu : menu > Paramètres) | non testé (catalogue « Interfaces » ; nouvelles destinations) |
| proxy | `KaliumRelay-1.1.1.jar` (déployé le 24/09/2026) | relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kanvas (ex Kal-Test-Dev) | `KV_Plots-1.4.0.jar` + `KV_Menu-1.3.0.jar` (26/09/2026 03:45, LeKiwi06 ; signalements, concours de build) ; `KLM_Menu-2.0.0.jar` (24/09/2026) ; FastAsyncWorldEdit **Paper** 2.15.4 (la variante Bukkit ne fonctionnait pas sur Paper 26.2 ; limites `limits.default` réduites, baguette de navigation = vide de structure ; originaux dans `_removed-fastasyncworldedit-config-2026-09-26/`) ; WorldGuard 7.0.19 ; LuckPerms (groupe `default` : permissions FAWE), PlaceholderAPI, ViaVersion / ViaBackwards, floodgate, voicechat 2.6.23 ; monde principal `Kanvas` (`level-name=Kanvas`). **Tri du 26/09/2026** : KaliumCore 1.4.0, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot, PyxelRegions rangés dans `plugins/_removed-<plugin>-<version>/` (dossiers de données laissés en place). Dossiers `_removed-…` en trop (plus de 2 par plugin), à supprimer par l'humain : `_removed-kv_plots-1.0.0`, `-1.1.0`, `-1.1.1`, `-1.2.0`, `-1.3.0`, `_removed-kv_menu-1.0.0`, `_removed-kaliumcore-1.0.0`, `-1.1.0`, `-1.1.1`, `-1.1.2`, `-1.2.1`, `-1.3.0`. `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` (extension PlaceholderAPI qui ne se chargeait pas, inutilisée) rangé dans `_removed-placeholderapi-scoreboard/` le 26/09/2026 (accord de LeKiwi06) | KV_Plots 1.0.0 à 1.2.0, KV_Menu 1.0.0 et 1.1.0, FAWE : testés et confirmés par LeKiwi06 le 26/09/2026 ; KV_Plots 1.3.0 / 1.3.1 (titre, description, visites, tableau sur le côté) et KV_Menu 1.2.0 testés et confirmés par LeKiwi06 le 26/09/2026 ; tri des plugins vérifié (plus d'erreur au démarrage) ; KV_Plots 1.4.0 et KV_Menu 1.3.0 (signalements, concours de build) testés et confirmés par LeKiwi06 le 26/09/2026 |
| Kixster (`kixster`) | remis dans l'état d'avant le Bingo le 24/09/2026 : monde d'origine `Kixster SMP` (ex `Kixster SMP_bak`, dernière sauvegarde 23/09 00:11), plugins SMP du 14/09 + WorldEdit 7.4.6-beta (gardé) + `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) ; tout le Bingo rangé dans `/_removed-bingo-2026-09-24/` et `/plugins/_removed-bingo-2026-09-24/` | non testé |
| Event (`event`, ancien kal-games) | reconverti en **survie classique** le 25/09/2026 : nouveau monde `world` (généré au premier démarrage, seed aléatoire, difficulté hard) ; ancien hub `Kal-Games` et vieux `world` dans `/_removed-survie-2026-09-25/` ; KalGames, KG_Bingo, KG_ScoreBoards (et tous leurs anciens jars) dans `/plugins/_removed-kalgames-event-2026-09-25/` ; `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) + **`KS_Dimensions-1.0.0.jar`** + `KS_Enclume`, `KS_Villageois`, `KS_Crafts`, `KS_LootBlocs`, `KS_LootEntites`, `KS_LootPeche`, `KS_LootPotions` 1.0.0 (25/09/2026, cahier des charges `KS_Event/CAHIER_DES_CHARGES.md`) ; + Floodgate (config et clé du proxy, 25/09/2026) ; PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`) ; extension PlaceholderAPI remise dans `plugins/PlaceholderAPI/expansions/` ; plugins tiers du hub gardés (ConditionalEvents, PyxelRegions, WorldGuard, GrimAC, JEIRecipeFix, PlaceholderAPI, ViaVersion / ViaBackwards, LuckPerms, voicechat, WorldEdit) | non testé |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

Aucune (26/09/2026 : tout ce qui est compilé est déployé, voir le tableau ci-dessus).

## Chantiers en cours

Architecture visée et charte du réseau (nommage, menus inter-serveurs, sécurité) : `ARCHITECTURE_CIBLE.md`.

- **KG_BoatRace** (cahier des charges : `KG_BoatRace/CAHIER_DES_CHARGES.md`) : étapes 0 (sortie de KalGames), 1
  (km/h, éditeur de checkpoints, seuil 45 s), 2 (classement en direct, écarts, données), 3 (barème, hors-piste) et 7
  (anti-collision) **faites et testées** (1.4.1). Restent : **4** (abandon / reconnexion en 5 min avec position,
  angle et vitesse), **5** (mode Grand Prix mis en avant en public et en privé ; le bonus x1,5 des 40 tours existe
  déjà), **6** (fantôme du meilleur tour et contre-la-montre solo) ; mesurer l'écart Java / Bedrock avec les données
  du journal ; précision des temps au dixième (interpolation possible).
- **KG_Parkour** (cahier des charges : `KG_Parkour/CAHIER_DES_CHARGES.md`) : **étape 0 faite** le 26/09/2026
  (KG_Parkour 1.0.0 + KalGames 1.20.0 : le Parcours sort de KalGames tel quel ; déployés sur Kal-Games, testés et confirmés par LeKiwi06 le 26/09/2026).
  Restent : chrono rechargé à chaque checkpoint, barème (difficulté, 1er à valider, first try) calé sur la course de
  bateau (voir `EQUILIBRAGE_POINTS.md`), réglages par checkpoint (`PointSpec.withPointSettings`), contre-la-montre
  solo avec fantôme (Mannequin), anti-collision entre joueurs.
- **Équilibrage des barèmes** (`EQUILIBRAGE_POINTS.md`, 26/09/2026) : 30 min à fond = autant de points dans chaque
  jeu, référence = course de bateau (~135 / 30 min). Bingo fait (barème doublé + classements, testé et confirmé par LeKiwi06 le 26/09/2026). Restent :
  Parcours (nouveau barème, parties « à fond » de LeKiwi06 pour mesurer), PvP Kit, Rush, recalcul du passé.
- **Kanvas** (serveur de plots en créatif, cahiers : `KV_Plots/CAHIER_DES_CHARGES.md`) : KV_Plots 1.4.0 et KV_Menu
  1.3.0 en service et testés (grille, réservation, éditeurs, protection WorldGuard + FAWE, remise à zéro / suppression,
  validation, votes aux terracottas, déblocages, titre / description, tableau sur le côté, visites, signalements,
  concours de build). Restent : limites d'entités et mobs sans IA, agrandissement moyen → grand (et « dupliquer en
  version grande »), classements KV_ScoreBoards (solo / duo / équipe, général et du mois), extension automatique du
  monde.
- **Architecture** : KalGames à répartir entre KG_Instances (moteur des parties : déjà ouvert aux plugins de jeu
  depuis KalGames 1.17.0), KLM_Hub + WorldGuard, KG_Menu et KG_ScoreBoards ; puis PvP Kit et Rush en plugins.
  Renommage KaliumRelay → KLM_Relay (voir la charte). Plus tard : menus inter-serveurs, KG_AntiCheat (avec GrimAC).
- **KG_ScoreBoards** : journal des parties (1.3.0), points décimaux (1.4.0) et vérification / crédit des parties des
  derniers jours (1.5.0 : `/classements verifier [jours]`, `/classements crediter <id|tout>`) faits. Étape C (résultats du Bingo via
  le relais) faite le 26/09/2026 (KG_BingoGame 0.8.0 + KG_Bingo 1.5.0 : classement « bingo », testé et confirmé par LeKiwi06 le 26/09/2026). Restent
  l'étape B du Bingo (classements solo / duo / trio / squad + blackout, fiche de chaque partie), l'affichage en jeu
  des données du journal (graphiques).
- **Interfaces** : KG_Menu 1.0.0 et KLM_Menu 2.1.0 en service sur Kal-Games. Phase 2 : accès des opérateurs aux
  interfaces des autres serveurs (voir la charte).
- **Bingo** : pseudo de l'hôte dans la liste des parties corrigé (KG_Bingo 1.4.0) ; nouvelle salle d'attente capturée
  par LeKiwi06 le 25/09/2026 (67x44x83) ; succès limités à la partie et préparation des maps 3 fois plus rapide
  (KG_BingoGame 0.7.2), tout testé. Reste à tester : Nether / End de chaque équipe.
- **Rush (KalGames)** : jouable, jamais testé en jeu ; à peaufiner plus tard (jeu de niche, après les jeux les plus
  joués). Voir `KalGames/JOURNAL.md`, sections 1.11.0 à 1.12.3.
- **Nettoyage des serveurs** : ne garder que les 2 derniers `_removed-…` par plugin (+ anciens `.bak`, dossiers
  `-token`) : après le découpage des plugins, suppression par l'humain.

## Points ouverts / limites connues (rien de bloquant)

- **Sauvegardes des mondes : à voir plus tard (LeKiwi06, 26/09/2026)**. Aucune sauvegarde automatique connue (seulement
  des copies manuelles, ex. `Kixster SMP_bak`). Pistes : 1) vérifier dans le panneau web Minestrator si chaque serveur
  a des sauvegardes (non vérifié, Claude n'y a pas accès) ; 2) sauvegarde manuelle des mondes sur le PC par WinSCP
  (hors du dépôt, serveur arrêté ou après `save-all` ; relever d'abord la taille des mondes) ; 3) plugin de
  sauvegarde sur chaque serveur (place disque à vérifier). À savoir : la génération de la grille de Kanvas
  (26/09/2026) a remplacé la zone x -406 à 240 / z -356 à 290 sans sauvegarde préalable ; la zone Build Battle
  (« à sauvegarder ») n'a pas été sauvegardée avant, vérifier si elle y était.
- **Serveur Event (25/09/2026)** : ConditionalEvents, PyxelRegions et WorldGuard gardent leurs réglages du hub (événements,
  régions de l'ancien monde) : à vérifier pour la survie. Floodgate installé, extension PlaceholderAPI déplacée,
  PlayerKits2 retiré (demande de Maxster33, 25/09/2026).
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
- Création d'une map Bingo : une seule map créée à la fois pour tout le serveur, zone de spawn plus gardée en
  mémoire (KG_BingoGame 0.6.0, « plus aucun lag à la génération » selon LeKiwi06).
- Un joueur Bingo déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle de salle d'attente Bingo : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente Bingo : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris).
- La pré-génération Bingo ne tient pas compte de `instances.max-simultaneous-games` (0.6.0 : sans conséquence de
  charge, toutes les parties passent par une seule file de génération).
- Éléments inutilisés, à supprimer lors d'une prochaine version (commentaires et journaux obsolètes déjà corrigés
  le 24/09/2026, dans les versions non déployées) : `bingo.max-party-size` dans KG_Bingo.
- **Secret de transfert Velocity** (`forwarding.secret`) : affiché par erreur dans une conversation Claude le
  24/09/2026. Vérifié le 25/09/2026 : absent du dépôt et de tout son historique. Décision de LeKiwi06 : pas de
  changement tant qu'il n'est pas publié sur GitHub.
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

### 2026-09-26 — LeKiwi06

*(Session de la nuit du 26/09/2026, de 0 h à 5 h 30. Fin de session : réservations KV_Plots, KV_Menu, KG_Parkour,
KalGames, KG_BingoGame, KG_Bingo, KLM_Menu et KG_ScoreBoards libérées.)*

**Kanvas (serveur de plots en créatif) : tout testé et confirmé par LeKiwi06 (« tout fonctionne »)**
- Cahier des charges complété (`KV_Plots/CAHIER_DES_CHARGES.md`) ; monde `New World (2)` renommé `Kanvas` (monde
  principal, majuscule) ; grille de 121 plots générée (plot de référence : coin -107 / -57, plots 49 x 49, routes 9).
- KV_Plots 1.0.0 → 1.4.0 et KV_Menu 1.0.0 → 1.3.0 : réservation (moyen / grand), éditeurs, protection WorldGuard,
  remise à zéro / suppression, créatif gardé en repassant joueur (KLM_Menu remettait en survie), validation, votes
  aux terracottas (inventaire mis de côté), déblocage à 100 points, titre / description, tableau sur le côté, visites
  (hasard, têtes des joueurs, liste), signalements (poudre de blaze, écrans du staff), concours de build (phases
  construction / votes / terminé, gestion par le staff). Étoile du Nether en case 4 (menu), rangée avec la boussole
  sur son plot en travaux.
- FAWE : la variante Bukkit ne marchait pas sur Paper 26.2 → **variante Paper** ; limites réduites ; permissions du
  groupe `default` (LuckPerms) ; baguette de navigation de WorldEdit = vide de structure (la boussole de KLM_Menu la
  déclenchait). Testé : FAWE en joueur seulement dans son plot.
- Tri des plugins de Kanvas : KaliumCore, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot,
  PyxelRegions et l'extension PlaceholderAPI inutilisée rangés dans `_removed-…`.

**Kal-Games / Serveur Jeux : déployés, non testés**
- KG_Parkour 1.0.0 + KalGames 1.20.0 : le Parcours sort de KalGames tel quel (orientation des checkpoints gardée).
- Équilibrage des barèmes (`EQUILIBRAGE_POINTS.md`, mesures sur les vraies parties) : KG_BingoGame 0.8.0 (barème
  doublé, résultats envoyés au hub) + KG_Bingo 1.5.0 (points du Bingo crédités dans les classements, classement
  « bingo »).

**À faire / à savoir**
- Tester : Parcours (partie publique, entraînement), course de bateau, Bingo (points doublés, puis crédités dans le
  classement « Bingo » du hub dans la minute qui suit la fin de partie ; message « points crédités » dans la console
  du hub).
- Équilibrage : barème du Parcours (jouer quelques parties « à fond » pour mesurer), PvP Kit, Rush, recalcul du passé
  (dont les anciennes parties de Bingo, jamais créditées).
- Sauvegardes des mondes : à voir (voir « Points ouverts »).
- Supprimer les dossiers `_removed-…` en trop (listes dans ce fichier et dans `KalGames/JOURNAL.md`) : c'est à
  l'humain de le faire.

### 2026-09-25 (soir) — LeKiwi06

*(Session du 25/09/2026 après-midi et soir. Fin de session : réservations KG_BingoGame et KalGames libérées.)*

**Déployé et testé (« tout est bon »)**
- KalGames 1.19.0 + KG_ScoreBoards 1.5.0 : plus de limite de parties privées, chaque attribution de points journalisée,
  `/classements verifier [jours]` et `/classements crediter` (points de course non comptés recrédités).
- KalGames 1.19.1 : aux points de contrôle (Parcours et courses), le retour garde l'orientation de la caméra du joueur
  au moment du passage (correctif mis dans KalGames en attendant KG_Parkour).
- KG_BingoGame 0.7.2 → 0.7.8 : préparation des maps débloquée (plus de blocage à 0 %), succès limités à la partie ;
  salle d'attente invincible, nether star bloquée, plaques de pression, entités recopiées (invulnérables, avec leur
  nom), barrières non recopiées ; positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml`
  (`/bingoadmin lobby capture` et `info` marchent aussi depuis la console) ; XP remise à zéro au lancement et en fin de
  partie, convertie en bonus (0,1 point par niveau, solo et équipe, sans cumul ni multiplicateur).
- Salle d'attente du Bingo (monde `bingo_lobby`) : coin 1 `-69 63 -14`, coin 2 `-5 108 67`, apparition
  `-13.49 79 2.77`.

**À faire / à savoir**
- KG_Parkour à créer (cahier des charges dans le dépôt) ; suite de KG_BoatRace (étapes 4, 5, 6).
- Kanvas : cahier `KV_Plots/CAHIER_DES_CHARGES.md` en local chez LeKiwi06 (non poussé), questions restantes, tri des
  plugins, zone Build Battle à sauvegarder.
- Bingo : tester Nether / End et 2 parties simultanées.

### 2026-09-25 — Maxster33

**Serveur Event reconverti en survie classique** (demande de Maxster33, faite par son Claude, WinSCP en ligne de
commande) :
- Map : ancien hub `Kal-Games` et vieux dossier `world` rangés dans `/_removed-survie-2026-09-25/` ;
  `server.properties` : `level-name=world` (nouveau monde vierge généré au premier démarrage, seed aléatoire),
  `difficulty=hard` (décisions de Maxster33) ; anciens `server.properties`, `commands.yml` et config KLM_Menu dans ce
  même dossier.
- Plugins retirés (rangés dans `/plugins/_removed-kalgames-event-2026-09-25/`) : KalGames 1.15.1, KG_Bingo 1.2.0,
  KG_ScoreBoards 1.1.0, leurs dossiers de données, tous les anciens `_removed-kalgames/kg_*/kaliummenu`, `claudebak`
  et les `.jar.bak`. Plugins tiers gardés (pas demandé de les retirer).
- KLM_Menu 2.0.0 gardé : boussole désactivée, menu par `/menu` (alias de `/servers` dans `commands.yml`, même
  méthode que Kixster).
- **Nouveau plugin KS_Dimensions 1.0.0** (préfixe `KS_` = serveur Event) : `/dimensions` (opérateurs) ouvre un menu
  pour activer / désactiver les portails du Nether et de l'End ; un portail désactivé bloque l'aller depuis le monde
  normal, jamais le retour (voir son JOURNAL). Compilé, déployé, non testé.
- À faire / à savoir : démarrer Event (l'humain) et tester ; activer la destination `event` dans le menu du lobby ;
  voir « Points ouverts ». Ensuite (même jour) : Floodgate installé (config et clé du proxy), extension
  PlaceholderAPI déplacée dans `expansions/`, PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`).
  Restent à vérifier : réglages de hub de ConditionalEvents / PyxelRegions / WorldGuard.
- **Plugins du serveur Event (soir du 25/09/2026)** : cahier des charges de Maxster33 enregistré dans
  `KS_Event/CAHIER_DES_CHARGES.md` (demande, réponses, interprétations) ; 7 plugins autonomes créés, compilés et
  déployés sur Event, non testés : KS_Enclume, KS_Villageois, KS_Crafts, KS_LootBlocs, KS_LootEntites, KS_LootPeche,
  KS_LootPotions (détail dans leurs JOURNAL.md). KS_Elixirs reporté (décision de Maxster33). Limite connue :
  4 blocs de cuivre en carré 2×2 donnent probablement le cuivre taillé vanilla (voir KS_Crafts/JOURNAL.md).
