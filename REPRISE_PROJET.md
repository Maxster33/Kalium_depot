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
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | port 25370 | KaliumRelay (relais HTTP, port 46199) ; Geyser : apparence Bedrock des objets custom (`geyser-bedrock/`) |
| Lobby | `lobby` | `lobby@7002.mystrator.com` | 91.197.6.152:28618 | KLM_Menu, KLM_Portal |
| **Hub mini-jeux (Kal-Games)** | `kal-games` | `KalGames2@7001.mystrator.com` (onglet « KalGames2 ») | 91.197.6.24:22142 | KalGames, KG_PvpKit, KG_BoatRace, KG_Parkour, KG_BuildBattle, KG_Menu, KG_Bingo, KG_ScoreBoards, KLM_Menu |
| **Bingo + jeux gourmands** (Manhunt...) | `serveur-jeux` | `serveurjeux@7015.mystrator.com` | 91.197.6.65:22470 | KG_BingoGame, KLM_Menu (boussole désactivée) |
| Kixster SMP (rendu au SMP le 24/09/2026) | `kixster` | `kixster@7003.mystrator.com` | 91.197.6.212:29599 | KLM_Menu (boussole désactivée, menu par `/menu`) |
| Event : survie classique (depuis le 25/09/2026) | `event` | `Event@7021.mystrator.com` (session WinSCP enregistrée) | 51.254.174.133:21088 | KLM_Menu (boussole désactivée, `/menu`), KS_Dimensions, KS_Enclume, KS_Villageois, KS_Crafts, KS_LootBlocs, KS_LootEntites, KS_LootPeche, KS_LootPotions, KS_EstomacGardien, KS_LootCoffres, KS_KaliumGive, KS_EC_Extension (cahier des charges : `KS_Event/`) |
| Serveur de plots en créatif (Kanvas) | `Kanvas` (ex `kal-test-dev`, renommé par LeKiwi06 dans `velocity.toml` le 24-25/09/2026 ; destination de la boussole du lobby renommée `Kanvas` le 26/09/2026) | `Kal-Test-Dev@5038.mystrator.com` | 91.197.6.215:21621 | KV_Plots, KV_Menu, KLM_Menu (cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`) |

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
| **Serveur Jeux** (`serveur-jeux`, Bingo) | **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KG_BingoGame-0.8.3.jar` + `KLM_Menu-2.4.0.jar`** (0.8.2 et 2.0.0 dans les `_removed-…`). Avant : copie complète de Kixster du 24/09/2026 + **`KG_BingoGame-0.8.2.jar`** (26/09/2026 06:27 : icônes de la carte en 22 pixels, blocs en 3D ; icônes testées et confirmées par LeKiwi06 le 26/09/2026 ; 0.8.1 dans `_removed-kg_bingogame-0.8.1/` ; dossier `plugins/KG_BingoGame/icons/26.2-3d/` à supprimer par l'humain) ; 0.8.1 (06:15 : bonus du 1er = moitié des points, joueurs non téléportés corrigés, barre d'action courte à 3-4 équipes, icônes de la carte en 3D, 7 objets du Nether en Normal (`objectives.yml` du serveur remplacé) ; non testé ; 0.8.0 et l'ancien `objectives.yml` dans `_removed-kg_bingogame-0.8.0/`) ; 0.8.0 (barème doublé, résultats envoyés au hub, testé) ; 0.7.8 (25/09/2026 : succès limités à la partie, XP remise à zéro et convertie en points bonus (0,1/niveau), préparation des maps corrigée et 3 fois plus rapide, salle d'attente protégée avec entités recopiées et sans barrières, positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml` ; versions précédentes dans les `_removed-kg_bingogame-…`) + `KLM_Menu-2.0.0.jar` (boussole désactivée) + Floodgate, BedrockSkinRestorer, VelocityCommandForward. Config : `network.self-server-name: "serveur-jeux"`, `network.kal-games-server-name: "kal-games"`, `lobby.max-size: 256`, `lobby.blocks-per-tick: 10000`, `instances.pregeneration-radius-blocks: 150`, `pregeneration-stagger-seconds: 5`, `pregeneration-chunks-per-second: 40` (25/09/2026) | 0.6.0 testée et confirmée par LeKiwi06 le 24/09/2026 (préparation des maps fluide, partie complète, Bedrock) ; à confirmer : Nether / End, 2 parties simultanées |
| **Kal-Games** (`kal-games`, hub, machine 7001, ex KalGames2) | **28/09/2026 21:44 (LeKiwi06, serveur éteint, non testé) : `KalGames-1.22.0` + `KG_PvpKit-1.0.0`** (PvP Kit sorti de KalGames avec ses kits, nouveau barème et déclassement ; 1.21.0 et copies des configs dans `_removed-kalgames-1.21.0/` ; PlayerKits2 1.23.3 encore en place : à ranger dans `_removed-playerkits2-1.23.3/` après le premier démarrage, une fois les kits convertis ; `_removed-kalgames-1.19.0/` et `1.19.1/` supprimables). **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KalGames-1.21.0`, `KG_Menu-1.1.0`, `KG_ScoreBoards-1.6.0`, `KG_Bingo-1.6.0`, `KG_BuildBattle-0.2.0`, `KLM_Menu-2.4.0`** (anciennes versions dans les `_removed-…`). Avant : copie complète de kal-games du 24/09/2026, puis (LeKiwi06) : **`KalGames-1.20.0.jar`** (26/09/2026 04:41 : Parcours sorti dans KG_Parkour ; 1.19.1 et copies de config.yml, minigames.yml, arenas.yml dans `_removed-kalgames-1.19.1/`) + **`KG_BuildBattle-0.1.0.jar`** (27/09/2026 13:55 : bouton Build Battle dans KG_Menu, file publique, parties privées, envoi vers Kanvas ; nouveau plugin ; testé et validé par LeKiwi06 le 28/09/2026) + **`KG_Parkour-1.1.0.jar`** (26/09/2026 20:56 : chrono qui s'allonge à chaque checkpoint, anti-collision, bottes de proximité ; testé et confirmé par LeKiwi06 le 27/09/2026 ; 1.0.0 dans `_removed-kg_parkour-1.0.0/`, Parcours sorti de KalGames tel quel, testé et confirmé par LeKiwi06 le 26/09/2026) + **`KG_BoatRace-1.4.1.jar`** (course de bateau sortie de KalGames) + **`KG_ScoreBoards-1.5.0.jar`** (journal des parties, points décimaux, `/classements verifier` et `crediter`) + `KG_Menu-1.0.0.jar` + `KLM_Menu-2.1.0.jar` + `KG_Bingo-1.5.1.jar` (26/09/2026 06:56 : parties listées plus de 30 min retirées de la liste ; non testé ; 1.5.0 dans `_removed-kg_bingo-1.5.0/`) ; 1.5.0 (05:23 : points du Bingo dans les classements ; testé et confirmé par LeKiwi06 le 26/09/2026 ; 1.4.0 dans `_removed-kg_bingo-1.4.0/` ; `bingo.server-name: serveur-jeux` ; pseudo de l'hôte corrigé dans la liste) ; anciens jars et configs dans les `_removed-…` de chaque plugin ; + Floodgate, BedrockSkinRestorer, VelocityCommandForward, GrimAC | testé et confirmé par LeKiwi06 les 24 et 25/09/2026 (menus, boussole, course de bateau : checkpoints, km/h, classement en direct, écarts, barème, hors-piste, anti-collision Java et Bedrock ; Parkour, classements, Bingo) ; non testé : Rush, capture d'arène |
| lobby | **28/09/2026 12:03 : `KLM_Portal-1.3.1.jar`** (« Ajouter un portail » : seulement les régions `portal_...` ; 1.3.0 à 11:59 : garde-fou : pas de portail sur le lobby entier ni sur la zone d'arrivée ; 1.2.0 dans `_removed-klm_portal-1.2.0/` ; non testé ; le portail `lobby` → event créé par erreur à 11:49 est à retirer avec `/klmportal remove lobby`). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.3.0 dans `_removed-klm_menu-2.3.0/`, non testé). Avant : **`KLM_Menu-2.3.0.jar` + `KLM_Portal-1.1.0.jar`** (26/09/2026 19:37 : points de chute, arrivée au centre `7.5 67 39.5` testée et confirmée par LeKiwi06 ; `relay-token` de KLM_Portal encore vide ; **`KLM_Portal-1.2.0.jar`** déployé à 20:54 (effets de zone speed 6 / jump_boost 2 dans la région `lobby` ; testé et confirmé par LeKiwi06 le 27/09/2026 ; 1.1.0 dans `_removed-klm_portal-1.1.0/`) ; 2.2.0 / 1.0.0 dans `_removed-…`) ; avant : `KLM_Menu-2.2.0.jar` + `KLM_Portal-1.0.0.jar` (26/09/2026 18:25, LeKiwi06 : portails en régions WorldGuard reliés aux destinations du menu, interface « Ajouter un portail » ; ConditionalEvents 4.79.2 et PyxelRegions 1.2.2 rangés dans `_removed-…`, dossiers de données laissés ; 2.0.0 dans `_removed-klm_menu-2.0.0/` ; testés et confirmés par LeKiwi06 le 26/09/2026 ; 4 portails. KLM_Menu 2.3.0 + KLM_Portal 1.1.0 déployés ensuite) ; avant : `KLM_Menu-2.0.0.jar` (destinations au 24/09/2026 : `kixster` (désactivée), `kal-games`, `serveur-jeux`, `kal-test-dev`, `event` (désactivée) ; activer / désactiver en jeu : menu > Paramètres) | non testé (catalogue « Interfaces » ; nouvelles destinations) |
| proxy | icône de la liste Multijoueur : `server-icon.png` (64 x 64, logo KaLium, 26/09/2026) à la racine ; `KaliumRelay-1.2.0.jar` (26/09/2026 07:21 : `/server` réservé aux admins, liste `admins` de `plugins/kaliumrelay/relay.properties`, LeKiwi06 et Maaxster par défaut ; testé et confirmé par LeKiwi06 le 26/09/2026 ; 1.1.1 dans `_removed-kaliumrelay-1.1.1/`) ; 1.1.1 (déployé le 24/09/2026) | relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kanvas (ex Kal-Test-Dev) | **28/09/2026 12:03 : `KLM_Portal-1.3.1.jar`** (1.3.0 et 1.2.0 dans les `_removed-klm_portal-…/`, non testé). **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KLM_Menu-2.4.0`, `KV_BuildBattle-0.3.6`, `KV_Menu-1.3.1`** (anciennes versions dans les `_removed-…`). Avant : **`KV_BuildBattle-0.3.5.jar`** (28/09/2026 01:17, LeKiwi06 : Build Battle complet, voir son JOURNAL ; anciens jars dans `_removed-kv_buildbattle-…/`, supprimables par l'humain : 0.1.0, 0.2.0, 0.3.0, 0.3.1) + **`KV_Plots-1.4.1.jar`** + **`KLM_Menu-2.3.0.jar`** + **`KLM_Portal-1.2.0.jar`** (27/09/2026 19:59 : randomTickSpeed à 0 dans `Kanvas` et `buildbattle`, points de chute appliqués à l'arrivée ; anciens jars dans `_removed-kv_plots-1.4.0/`, `_removed-klm_menu-2.0.0/`) + `KV_Menu-1.3.0.jar` (26/09/2026 03:45, LeKiwi06 ; signalements, concours de build) ; FastAsyncWorldEdit **Paper** 2.15.4 (la variante Bukkit ne fonctionnait pas sur Paper 26.2 ; limites `limits.default` réduites, baguette de navigation = vide de structure ; originaux dans `_removed-fastasyncworldedit-config-2026-09-26/`) ; WorldGuard 7.0.19 ; LuckPerms (groupe `default` : permissions FAWE), PlaceholderAPI, ViaVersion / ViaBackwards, floodgate, voicechat 2.6.23 ; monde principal `Kanvas` (`level-name=Kanvas`). **Tri du 26/09/2026** : KaliumCore 1.4.0, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot, PyxelRegions rangés dans `plugins/_removed-<plugin>-<version>/` (dossiers de données laissés en place). Anciens dossiers `_removed-kv_plots-…` / `kv_menu-…` supprimés le 26/09/2026 par LeKiwi06 (règle des 3 versions) ; anciens `_removed-kaliumcore-…` supprimés aussi. `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` (extension PlaceholderAPI qui ne se chargeait pas, inutilisée) rangé dans `_removed-placeholderapi-scoreboard/` le 26/09/2026 (accord de LeKiwi06) | KV_Plots 1.0.0 à 1.2.0, KV_Menu 1.0.0 et 1.1.0, FAWE : testés et confirmés par LeKiwi06 le 26/09/2026 ; KV_Plots 1.3.0 / 1.3.1 (titre, description, visites, tableau sur le côté) et KV_Menu 1.2.0 testés et confirmés par LeKiwi06 le 26/09/2026 ; tri des plugins vérifié (plus d'erreur au démarrage) ; KV_Plots 1.4.0 et KV_Menu 1.3.0 (signalements, concours de build) testés et confirmés par LeKiwi06 le 26/09/2026 ; KV_BuildBattle 0.1.0 à 0.3.5, KV_Plots 1.4.1, KLM_Menu 2.3.0 et KLM_Portal 1.2.0 (point de chute, boussole, étoile) testés et validés par LeKiwi06 les 27 et 28/09/2026 |
| Kixster (`kixster`) | **28/09/2026 04:03 : `KLM_Menu-2.4.1.jar`** + config (`compass.enabled: true`, `items-by-default: false` : objets de menu seulement après `/menu on` ; non testé). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.0.0 dans `_removed-klm_menu-2.0.0/`) et alias `menu` retiré de `commands.yml` (non testé). Avant : remis dans l'état d'avant le Bingo le 24/09/2026 : monde d'origine `Kixster SMP` (ex `Kixster SMP_bak`, dernière sauvegarde 23/09 00:11), plugins SMP du 14/09 + WorldEdit 7.4.6-beta (gardé) + `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) ; tout le Bingo rangé dans `/_removed-bingo-2026-09-24/` et `/plugins/_removed-bingo-2026-09-24/` | non testé |
| Event (`event`, ancien kal-games) | **28/09/2026 23:19 (Maxster33) : `KS_EstomacGardien-1.3.0.jar` (1.1.0 à 23:25 avec `/estomac`, retirée en 1.2.0 à 23:35 ; 1.3.0 le 29/09 à 00:02 : casque 20 %), `KS_KaliumGive-1.1.0.jar` (23:35 : `/kaliumgive <pseudo> <id_custom> <nombre>`, opérateurs ; 1.1.0 à 23:47 : id `cle_de_l_end`), `KS_EC_Extension-1.0.0.jar` (23:47 : coffre de l'Ender à 6 lignes, 3 du bas débloquées case par case avec la Clé de l'End), `KS_Crafts-1.3.0.jar` (23:47 : craft de la Clé de l'End ; 1.3.0 le 29/09 à 00:27 : recette dans le livre de recettes dès qu'un ingrédient est obtenu) et `KS_LootCoffres-1.0.0.jar` (nouveaux), `KS_LootEntites-1.1.0.jar`, `KS_LootPotions-1.1.0.jar`** (anciens dans `_removed-ks_lootentites-1.0.0/` et `_removed-ks_lootpotions-1.0.0/` ; non testés ; demande : `KS_Event/CAHIER_DES_CHARGES.md`, partie 4). **28/09/2026 04:03 : `KLM_Menu-2.4.1.jar`** + config (`compass.enabled: true`, `items-by-default: false` : objets de menu seulement après `/menu on` ; non testé). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.0.0 dans `_removed-klm_menu-2.0.0/`) et alias `menu` retiré de `commands.yml` (non testé). Avant : reconverti en **survie classique** le 25/09/2026 : nouveau monde `world` (généré au premier démarrage, seed aléatoire, difficulté hard) ; ancien hub `Kal-Games` et vieux `world` dans `/_removed-survie-2026-09-25/` ; KalGames, KG_Bingo, KG_ScoreBoards (et tous leurs anciens jars) dans `/plugins/_removed-kalgames-event-2026-09-25/` ; `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) + **`KS_Dimensions-1.0.0.jar`** + `KS_Enclume`, `KS_Villageois`, `KS_Crafts`, `KS_LootBlocs`, `KS_LootEntites`, `KS_LootPeche`, `KS_LootPotions` 1.0.0 (25/09/2026, cahier des charges `KS_Event/CAHIER_DES_CHARGES.md`) ; + Floodgate (config et clé du proxy, 25/09/2026) ; PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`) ; extension PlaceholderAPI remise dans `plugins/PlaceholderAPI/expansions/` ; plugins tiers du hub gardés (ConditionalEvents, PyxelRegions, WorldGuard, GrimAC, JEIRecipeFix, PlaceholderAPI, ViaVersion / ViaBackwards, LuckPerms, voicechat, WorldEdit) | non testé |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

- **KG_ScoreBoards 1.7.0** (29/09/2026, LeKiwi06) : API HTTP en lecture pour le bot Discord (clés `api.port` / `api.token` à ajouter à la main, port 45347 attribué par Minestrator) ; non testé. Procédure dans son `JOURNAL.md`.
- **KLM_DiscordBot 0.1.0** (29/09/2026, LeKiwi06) : bot Discord Node.js, étape 1 (`/classement` en image) ; hébergement à choisir ; non testé sur Discord. Nécessite KG_ScoreBoards 1.7.0.

**Refonte des menus (déployée le 28/09/2026 à 3 h 40 par Claude de LeKiwi06, serveurs arrêtés, non testée en jeu)** : comparateur « Informations »
(case de gauche : Classements pour tous, Paramètres pour les admins, un bouton par jeu), boussole réduite à la
navigation et verrouillée pour tout le monde, couleurs des menus assombries pour les joueurs Bedrock, nombre de joueurs
sur chaque bouton de jeu de kal-games, boutons obsolètes retirés (Mini-jeux, Kits, Hub, Arènes mélangées, emplacement
Build Battle de KalGames), `/menu` (ouvre le menu du serveur, choix s'il y en a plusieurs ; `/menu on | off` remet / retire
les objets de menu de la barre, refusé en partie ou si les cases sont prises, sauf en créatif). Détail dans chaque
`JOURNAL.md`. Alias `menu` (vers `servers`) retiré du `commands.yml` de Kixster et d'Event (originaux dans
`/_removed-commands-2026-09-28/`).

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
- **Build Battle** (cahier des charges : `KV_BuildBattle/CAHIER_DES_CHARGES.md`, 27/09/2026) : KG_BuildBattle
  (kal-games : file publique, parties privées, envoi vers Kanvas) + KV_BuildBattle (Kanvas : le jeu). Étapes 1 (arène), 2
  (la partie) et 3 (KG_BuildBattle) faites, déployées et validées par LeKiwi06 le 28/09/2026 (KV_BuildBattle 0.3.5,
  KG_BuildBattle 0.1.0). Reste : 4 (points et classements, KG_ScoreBoards) ; limite de mobs par zone ; retirer
  l'emplacement « Build Battle » sans moteur de KalGames.
- **Architecture** : KalGames à répartir entre KG_Instances (moteur des parties : déjà ouvert aux plugins de jeu
  depuis KalGames 1.17.0), KLM_Hub + WorldGuard, KG_Menu et KG_ScoreBoards ; puis le Rush en plugin.
  PvP Kit sorti dans KG_PvpKit (1.0.0, déployé le 28/09/2026, non testé). Renommage KaliumRelay → KLM_Relay (voir la charte). Plus tard : menus inter-serveurs, KG_AntiCheat (avec GrimAC).
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

### 2026-09-28 — LeKiwi06

*(Session du 27/09/2026 à 9 h 47 au 28/09/2026 à 1 h 30. Fin de session : toutes les réservations libérées.)*

**Build Battle, nouveau mini-jeu (testé et validé par LeKiwi06 le 28/09/2026)** : cahier des charges
`KV_BuildBattle/CAHIER_DES_CHARGES.md`. Accessible seulement depuis kal-games, joué sur Kanvas (créatif + FAWE, mêmes
protections que les plots).
- **KG_BuildBattle 0.1.0** (kal-games, nouveau) : bouton dans KG_Menu ; file publique solo / duo / trio / squad (tempo
  normal 5 min) ; parties privées (tempo 3 / 5 / 10 / 30 min, équipes de 1 à 4, mode « thèmes écrits », code) ; envoi
  vers Kanvas par le relais (clé `buildbattle-<uuid>`). Liaison « comme le Bingo » (le moteur de KalGames n'est pas
  utilisé).
- **KV_BuildBattle 0.3.5** (Kanvas, nouveau) : monde `buildbattle` ; capture d'une boîte (entière + zone constructible
  + apparition) recopiée en 4 colonnes de 8 (une par partie, 600 blocs entre colonnes) ; salle d'attente capturée et
  recopiée par colonne ; partie complète : compte à rebours 30 s dès 2 équipes, 8 équipes max (16 en duo, 32 en
  squad), vote parmi 5 thèmes (liste de 200 validée) ou thèmes écrits (signalement à la poudre de blaze), construction
  chronométrée dans sa boîte, zones figées, vote terrain par terrain aux terracottas (30 s → 3 s), résultats, retour
  sur kal-games, boîtes remises à neuf ; salle en mode aventure, joueurs invincibles, infos en barre d'action ;
  mobs figés et retirables d'un clic ; rien ne pousse hors de la zone ; `randomTickSpeed` à 0.
- `relay-token` rempli à la main par LeKiwi06 dans les config.yml de KG_BuildBattle et KV_BuildBattle.

**Kanvas** : KV_Plots 1.4.1 (`randomTickSpeed` à 0 : la glace ne fond plus) ; KLM_Menu 2.0.0 → 2.3.0 et KLM_Portal 1.2.0
installés : le point de chute du lobby vers Kanvas fonctionne, boussole et étoile de KV_Menu vérifiées (validé).

**À faire plus tard**
- **Build Battle, étape 4** : points et classements (KG_ScoreBoards, barème calé sur `EQUILIBRAGE_POINTS.md`).
- Build Battle : limite du nombre de mobs par zone (non demandée pour l'instant) ; retirer l'emplacement « Build
  Battle » sans moteur de KalGames à la prochaine retouche de KalGames ; si le serveur s'arrête en pleine partie, les
  boîtes ne sont pas remises à neuf (« Générer l'arène »).
- Nettoyage (par l'humain, règle des 3 versions) : sur Kanvas, `_removed-kv_buildbattle-0.1.0/`, `0.2.0/`, `0.3.0/`,
  `0.3.1/`.
- Repris de la session du 26/09 (soir) : sur le lobby, VelocityCommandForward et dossiers `ConditionalEvents/`,
  `PyxelRegions/` non retirés ; `/l` et `/lobby` au lobby (« Tu es déjà au lobby », à trancher) ; visuel des portails
  désactivés ; KG_Parkour (réglages par checkpoint et par map, barème, contre-la-montre avec fantôme, Bedrock) ;
  tester KG_Bingo 1.5.1 et KG_BingoGame 0.8.2 ; équilibrage des barèmes ; Kanvas (limites d'entités, agrandissement
  moyen → grand, classements, extension du monde) ; sauvegardes des mondes ; dossiers gardés volontairement.

### 2026-09-28 — Maxster33

*(Session du 28/09/2026, de 21 h 40 à 23 h 30, Claude de Maxster33. Fin de session : réservations libérées.)*

**Serveur Event : estomac du gardien et loots des coffres de structures** (demande et réponses :
`KS_Event/CAHIER_DES_CHARGES.md`, partie 4). Déployé sur Event le 28/09/2026 à 23:19 (WinSCP en ligne de commande),
**non testé en jeu, redémarrage d'Event à faire par l'humain** :
- **KS_EstomacGardien 1.3.0** (nouveau ; 1.1.0 avait `/estomac`, retirée à la demande de Maxster33 ; 1.3.0 : casque 20 %) : objet « Estomac du gardien » (image du sac noir, pas un vrai sac) ; clic
  droit : consommé, donne son contenu (casque en diamant 20 %, puis 2 tirages : oeuf de tortue, bateau, corail,
  algue, trident enchanté, armure de nautile en diamant, coeur de la mer).
- **KS_LootCoffres 1.0.0** (nouveau) : cité antique, 4 bastions, trésor enfoui, cité de l'End, forteresse,
  avant-poste, manoir, coffres-forts des chambres d'épreuve. Le loot vanilla (tables 26.2 lues dans le jeu) est
  modifié après génération ; changements de poids exacts par remplacement (voir son JOURNAL). Seuls les coffres pas
  encore ouverts sont concernés.
- **KS_KaliumGive 1.0.0** (nouveau, 23:35) : `/kaliumgive <pseudo> <id_custom> <nombre>` (opérateurs) ; liste des
  id_custom dans le code : `estomac_gardien`, `cle_de_l_end` (1.1.0).
- **KS_EC_Extension 1.0.0** (nouveau, 23:47) : coffre de l'Ender à 6 lignes ; les 3 du bas sont bloquées (barrières) et se
  débloquent case par case en déposant une Clé de l'End (consommée). Clé : image de la clé des épreuves sinistre,
  empilable par 64, craft sans forme dans **KS_Crafts 1.1.0** (coeur de la mer, crâne de wither, totem, pomme de Notch,
  capteur sculk calibré, éponge, lingot de netherite, cloche, coeur de grinceur). Couvercle non animé (limite connue).
- **KS_LootEntites 1.1.0** : grand gardien 50 % d'estomac (tué par un joueur ; `softdepend` KS_EstomacGardien).
- **KS_LootPotions 1.1.0** : potion de faiblesse 10 % pour le capitaine pillard seulement.
- Anciens jars dans `/plugins/_removed-ks_lootentites-1.0.0/` et `/plugins/_removed-ks_lootpotions-1.0.0/`.
- À savoir : la session WinSCP enregistrée d'Event s'appelle `Event@7021.mystrator.com` (port 2022) ; le nom
  `kalgames@7021...` ne marche pas en ligne de commande (WinSCP essaie alors le port 22) ; tableau d'architecture corrigé.
- À tester : estomac (clic droit, contenu, inventaire plein), drop du grand gardien, coffres de structures neufs,
  coffres-forts normal et sinistre, capitaine / pillard, `/kaliumgive`, coffre de l'Ender (ouverture, déblocage,
  contenu gardé après reconnexion), craft de la Clé de l'End.
- **Apparence Bedrock (29/09/2026 00:18, non testée)** : nouveau dossier `geyser-bedrock/` (voir son README). Geyser
  n'affichait pas l'image (`item_model`) des objets custom : sur Bedrock, l'Estomac du gardien et la Clé de l'End
  étaient des livres. Correspondance Geyser `custom_mappings/kalium_objets.json` et pack `KaLium-objets-1.0.0.mcpack`
  envoyés sur le proxy (dossiers `custom_mappings/` et `packs/` de Geyser-Velocity, vides avant) ; **actif après
  redémarrage du proxy (l'humain)**. Aucun autre plugin n'utilise d'`item_model` (vérifié dans tout le dépôt).
