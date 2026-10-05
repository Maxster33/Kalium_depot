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
| Lobby | `lobby` | `lobby@7002.mystrator.com` | 91.197.6.152:28618 | KLM_Menu, KLM_Portal, KLM_Hub, KLM_Chat |
| **Hub mini-jeux (Kal-Games)** | `kal-games` | `KalGames@7001.mystrator.com` (session WinSCP de Maxster33 ; onglet « KalGames2 » chez LeKiwi06) | 91.197.6.24:22142 | KalGames, KG_PvpKit, KG_BoatRace, KG_Parkour, KG_BuildBattle, KG_Menu, KG_Bingo, KG_ScoreBoards, KLM_Menu, KLM_Chat |
| **Bingo + jeux gourmands** (Manhunt...) | `serveur-jeux` | `serveurjeux@7015.mystrator.com` | 91.197.6.65:22470 | KG_BingoGame, KG_BingoObservateur, KLM_Menu (boussole désactivée), KLM_Chat |
| Kixster SMP (rendu au SMP le 24/09/2026) | `kixster` | `kixster@7003.mystrator.com` | 91.197.6.212:29599 | KLM_Menu (boussole désactivée, menu par `/menu`), KLM_Chat |
| Event : survie classique (depuis le 25/09/2026) | `event` | `Event@7021.mystrator.com` (session WinSCP enregistrée) | 51.254.174.133:21088 | KLM_Menu (boussole désactivée, `/menu`), KLM_Chat, KS_Dimensions, KS_Enclume, KS_Villageois, KS_Crafts, KS_BedrockBreaker, KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_LootBlocs, KS_LootEntites, KS_LootPeche, KS_LootPotions, KS_EstomacGardien, KS_LootCoffres, KS_KaliumGive, KS_EC_Extension, KS_FioleExp, KS_Decapitator, KS_Elixir, KS_Menu, KS_Economy, KS_Claim, SimpleClaimSystem (cahiers des charges : `KS_Event/`, `KS_Decapitator/`, `KS_Elixir/`, `KS_Economy/`, `KS_Claim/`) |
| Serveur de plots en créatif (Kanvas) | `Kanvas` (ex `kal-test-dev`, renommé par LeKiwi06 dans `velocity.toml` le 24-25/09/2026 ; destination de la boussole du lobby renommée `Kanvas` le 26/09/2026) | `Kal-Test-Dev@5038.mystrator.com` | 91.197.6.215:21621 | KV_Plots, KV_Menu, KLM_Menu, KLM_Chat (cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`) |

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
`online-mode=false` et `enforce-secure-profile=false` (sinon chat refusé aux joueurs Bedrock : « absence de la clé publique du profil » ; réglé sur tous les serveurs le 01/10/2026) ; Floodgate avec la **même `key.pem`** que le proxy (le proxy envoie les données Bedrock :
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
| **Serveur Jeux** (`serveur-jeux`, Bingo) | **05/10/2026 13:34 (Maxster33, non testé, actif après redémarrage) : `KG_BingoGame-0.10.0.jar`** (réserve de mondes par lots : 3 lots de 4 et 3 lots solo, une seed neuve par lot, entamée seulement pendant une autre partie ; contre la montre gagné en N bingos, 10 au plus à 1 équipe ; à garder avec KG_Bingo 1.7.3 ; 0.9.0 dans `_removed-kg_bingogame-0.9.0/` ; supprimables par l'humain : `_removed-kg_bingogame-0.8.0/` à `0.8.6/` (7)). **05/10/2026 12:33 (LeKiwi06, non testé, actif après redémarrage) : `KG_BingoGame-0.9.0.jar`** (victoire : points + moyenne des équipes / joueurs classés derrière, au lieu de leur somme ; 0.8.7 dans `_removed-kg_bingogame-0.8.7/` ; supprimables par l'humain : `_removed-kg_bingogame-0.8.0/` à `0.8.5/` (6)). **03/10/2026 22:47 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » de la boussole sous 4 joueurs ; 2.5.0 dans `_removed-klm_menu-2.5.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global send <message> | true | false`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`. **03/10/2026 17:05 (Maxster33, non testé, actif après redémarrage) : `KG_BingoGame-0.8.7.jar`** (contre la montre : temps gagné selon la difficulté, 2 min 30 / 4 min / 6 min 30 / 10 min ; 0.8.6 dans `_removed-kg_bingogame-0.8.6/` ; supprimable par l'humain en plus : `_removed-kg_bingogame-0.8.4/`). **03/10/2026 12:51 (Maxster33, non testé, actif après redémarrage) : `KG_BingoGame-0.8.6.jar`** (point de départ dans un biome terrestre : en 0.8.5 il pouvait tomber au milieu de l'océan ; 0.8.5 dans `_removed-kg_bingogame-0.8.5/` ; supprimable par l'humain en plus : `_removed-kg_bingogame-0.8.3/`). **03/10/2026 10:18 (Maxster33, non testé, actif après redémarrage) : `KG_BingoObservateur-0.1.0.jar`** (nouveau : `/observer` donne aux opérateurs une longue-vue pour observer n'importe quel joueur en spectateur, vue libre ou dans ses yeux, `/observer quitter`). **03/10/2026 09:41 (Maxster33, non testé, actif après redémarrage) : `KG_BingoGame-0.8.5.jar` + nouveau `plugins/KG_BingoGame/objectives.yml`** (25 difficiles ; partie bloquée après un échec du lancement corrigée ; abandon : -15 au classement ; pas de bonus « en 1er » à une équipe ; mode contre la montre ; plus de gel à la création d'une partie : point d'apparition fourni + réserve de 2 mondes `bingo_reserve-…` ; 0.8.4 et l'ancien `objectives.yml` dans `_removed-kg_bingogame-0.8.4/` ; supprimables par l'humain : `_removed-kg_bingogame-0.8.0/`, `0.8.1/`, `0.8.2/`). **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`) + **`KG_BingoGame-0.8.4.jar`** (même correctif ; 0.8.3 dans `_removed-kg_bingogame-0.8.3/`). **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KG_BingoGame-0.8.3.jar` + `KLM_Menu-2.4.0.jar`** (0.8.2 et 2.0.0 dans les `_removed-…`). Avant : copie complète de Kixster du 24/09/2026 + **`KG_BingoGame-0.8.2.jar`** (26/09/2026 06:27 : icônes de la carte en 22 pixels, blocs en 3D ; icônes testées et confirmées par LeKiwi06 le 26/09/2026 ; 0.8.1 dans `_removed-kg_bingogame-0.8.1/` ; dossier `plugins/KG_BingoGame/icons/26.2-3d/` à supprimer par l'humain) ; 0.8.1 (06:15 : bonus du 1er = moitié des points, joueurs non téléportés corrigés, barre d'action courte à 3-4 équipes, icônes de la carte en 3D, 7 objets du Nether en Normal (`objectives.yml` du serveur remplacé) ; non testé ; 0.8.0 et l'ancien `objectives.yml` dans `_removed-kg_bingogame-0.8.0/`) ; 0.8.0 (barème doublé, résultats envoyés au hub, testé) ; 0.7.8 (25/09/2026 : succès limités à la partie, XP remise à zéro et convertie en points bonus (0,1/niveau), préparation des maps corrigée et 3 fois plus rapide, salle d'attente protégée avec entités recopiées et sans barrières, positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml` ; versions précédentes dans les `_removed-kg_bingogame-…`) + `KLM_Menu-2.0.0.jar` (boussole désactivée) + Floodgate, BedrockSkinRestorer, VelocityCommandForward. Config : `network.self-server-name: "serveur-jeux"`, `network.kal-games-server-name: "kal-games"`, `lobby.max-size: 256`, `lobby.blocks-per-tick: 10000`, `instances.pregeneration-radius-blocks: 150`, `pregeneration-stagger-seconds: 5`, `pregeneration-chunks-per-second: 40` (25/09/2026) | 0.6.0 testée et confirmée par LeKiwi06 le 24/09/2026 (préparation des maps fluide, partie complète, Bedrock) ; à confirmer : Nether / End, 2 parties simultanées |
| **Kal-Games** (`kal-games`, hub, machine 7001, ex KalGames2) | **05/10/2026 13:34 (Maxster33, non testé, actif après redémarrage) : `KG_Bingo-1.7.3.jar`** (contre la montre : nombre de bingos à achever, 3 à 10 à 1 équipe ; à garder avec KG_BingoGame 0.10.0 ; 1.7.2 dans `_removed-kg_bingo-1.7.2/` ; supprimables par l'humain : `_removed-kg_bingo-1.3.0/`, `1.4.0/`, `1.5.0/`, `1.5.1/`, `1.6.0/`, `1.6.1/`, `1.7.0/` (7)). **05/10/2026 12:33 (LeKiwi06, serveur arrêté, non testés) : `KG_BoatRace-1.5.0.jar`** (points + moyenne des joueurs classés en dessous, au lieu de leur somme ; 1.4.2 dans `_removed-kg_boatrace-1.4.2/` ; supprimables par l'humain : `_removed-kg_boatrace-1.3.0/`, `1.4.0/`) **+ config : `stats.exclude-operators: false`** dans `KalGames/config.yml` et `KG_ScoreBoards/config.yml` (les points et temps des opérateurs comptent ; originaux dans `_removed-kalgames-config-2026-10-05/` et `_removed-kg_scoreboards-stats-2026-10-05/`) **+ `stats.yml` et journal d'octobre de KG_ScoreBoards modifiés** (points d'opérateur crédités, rétroactif de la moyenne ; originaux dans `_removed-kg_scoreboards-stats-2026-10-05/`). **04/10/2026 22:16 (LeKiwi06, non testé, actif après redémarrage) : `KG_Parkour-1.2.0.jar`** (nouveau barème : 1 à 10 points selon la position du checkpoint, 1er à valider x1,5, sans chute x1,5, temps x1,25 à x1,75, +25 au 1er arrivé ; 1.1.1 dans `_removed-kg_parkour-1.1.1/` ; supprimable par l'humain : `_removed-kg_parkour-1.0.0/`). **03/10/2026 22:47 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » de la boussole sous 4 joueurs ; 2.6.0 dans `_removed-klm_menu-2.6.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global send <message> | true | false`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`, `2.1.0/`, `2.4.0/`. **03/10/2026 18:12 (Maxster33, non testé, actif après redémarrage) : `KG_Bingo-1.7.2`** (création d'une partie en deux écrans : équipes puis règles ; à 1 équipe, contre la montre présélectionné et expliqué, absent à 2 équipes ou plus ; 1.7.1 dans `_removed-kg_bingo-1.7.1/`). **03/10/2026 17:06 (Maxster33, non testé, actif après redémarrage) : `KG_Bingo-1.7.1`** (texte d'aide du contre la montre ; 1.7.0 dans `_removed-kg_bingo-1.7.0/` ; supprimable par l'humain en plus : `_removed-kg_bingo-1.6.0/`). **03/10/2026 09:44 (Maxster33, non testés, actifs après redémarrage) : `KG_Bingo-1.7.0` + `KG_ScoreBoards-1.9.0`** (2 parties Bingo par heure, mode contre la montre, pénalité d'abandon retirée du classement, totaux négatifs possibles ; à déployer avec KG_BingoGame 0.8.5 ; 1.6.1 et 1.8.0 dans `_removed-kg_bingo-1.6.1/` et `_removed-kg_scoreboards-1.8.0/` ; supprimables par l'humain : `_removed-kg_bingo-1.3.0/`, `1.4.0/`, `1.5.0/`, `1.5.1/`, `_removed-kg_scoreboards-1.3.0/` à `1.6.0/`). **03/10/2026 01:05 (LeKiwi06, non testés) : `KG_ScoreBoards-1.8.0`** (classement de la semaine, mois aligné, prestige affiché) + **`KG_Rewards-1.0.0`** (nouveau : paliers, tops, prestige, butin ; `config.yml` posé, `relay-token` à remplir) + `KLM_Menu-2.6.0` ; anciens jars dans les `_removed-…`. **02/10/2026 17:49 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KalGames-1.22.2`, `KG_BoatRace-1.4.2`, `KG_Parkour-1.1.1`, `KG_PvpKit-1.0.1`, `KG_Bingo-1.6.1`** (textes plus courts dans les menus ; réglages des jeux : nom seul dans la jauge, explication en haut du menu ; anciens jars dans les `_removed-…`). **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`) + **`KalGames-1.22.1.jar`** (même correctif, menus de KalGames, KG_Bingo, KG_PvpKit ; 1.22.0 dans `_removed-kalgames-1.22.0/`). **29/09/2026 02:36 (LeKiwi06, serveur allumé, jar actif au prochain redémarrage, non testé) : `KG_ScoreBoards-1.7.0`** (API HTTP pour le bot Discord ; 1.6.0 et copie du `config.yml` dans `_removed-kg_scoreboards-1.6.0/` ; clés `api.port: 45347` et `api.token` à ajouter à la main par l'humain ; `_removed-kg_scoreboards-1.3.0/` et `1.4.0/` supprimables). **28/09/2026 21:44 (LeKiwi06, serveur éteint, non testé) : `KalGames-1.22.0` + `KG_PvpKit-1.0.0`** (PvP Kit sorti de KalGames avec ses kits, nouveau barème et déclassement ; 1.21.0 et copies des configs dans `_removed-kalgames-1.21.0/` ; PlayerKits2 1.23.3 encore en place : à ranger dans `_removed-playerkits2-1.23.3/` après le premier démarrage, une fois les kits convertis ; `_removed-kalgames-1.19.0/` et `1.19.1/` supprimables). **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KalGames-1.21.0`, `KG_Menu-1.1.0`, `KG_ScoreBoards-1.6.0`, `KG_Bingo-1.6.0`, `KG_BuildBattle-0.2.0`, `KLM_Menu-2.4.0`** (anciennes versions dans les `_removed-…`). Avant : copie complète de kal-games du 24/09/2026, puis (LeKiwi06) : **`KalGames-1.20.0.jar`** (26/09/2026 04:41 : Parcours sorti dans KG_Parkour ; 1.19.1 et copies de config.yml, minigames.yml, arenas.yml dans `_removed-kalgames-1.19.1/`) + **`KG_BuildBattle-0.1.0.jar`** (27/09/2026 13:55 : bouton Build Battle dans KG_Menu, file publique, parties privées, envoi vers Kanvas ; nouveau plugin ; testé et validé par LeKiwi06 le 28/09/2026) + **`KG_Parkour-1.1.0.jar`** (26/09/2026 20:56 : chrono qui s'allonge à chaque checkpoint, anti-collision, bottes de proximité ; testé et confirmé par LeKiwi06 le 27/09/2026 ; 1.0.0 dans `_removed-kg_parkour-1.0.0/`, Parcours sorti de KalGames tel quel, testé et confirmé par LeKiwi06 le 26/09/2026) + **`KG_BoatRace-1.4.1.jar`** (course de bateau sortie de KalGames) + **`KG_ScoreBoards-1.5.0.jar`** (journal des parties, points décimaux, `/classements verifier` et `crediter`) + `KG_Menu-1.0.0.jar` + `KLM_Menu-2.1.0.jar` + `KG_Bingo-1.5.1.jar` (26/09/2026 06:56 : parties listées plus de 30 min retirées de la liste ; non testé ; 1.5.0 dans `_removed-kg_bingo-1.5.0/`) ; 1.5.0 (05:23 : points du Bingo dans les classements ; testé et confirmé par LeKiwi06 le 26/09/2026 ; 1.4.0 dans `_removed-kg_bingo-1.4.0/` ; `bingo.server-name: serveur-jeux` ; pseudo de l'hôte corrigé dans la liste) ; anciens jars et configs dans les `_removed-…` de chaque plugin ; + Floodgate, BedrockSkinRestorer, VelocityCommandForward, GrimAC | testé et confirmé par LeKiwi06 les 24 et 25/09/2026 (menus, boussole, course de bateau : checkpoints, km/h, classement en direct, écarts, barème, hors-piste, anti-collision Java et Bedrock ; Parkour, classements, Bingo) ; non testé : Rush, capture d'arène |
| lobby | **03/10/2026 22:46 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Hub-1.0.0.jar`** (nouveau : barre de boss violette « KaLium SMC | lien défilant Discord / Twitch », QR codes 30 s en main secondaire) **+ `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » ; 2.5.0 dans `_removed-klm_menu-2.5.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global`) **+ vision nocturne** (`night_vision: 0` ajouté à `region-effects.lobby.effects` de `plugins/KLM_Portal/config.yml` ; ancien fichier dans `_removed-klm_portal-config-2026-10-03/`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`, `2.2.0/`, `2.3.0/`, `_removed-klm_portal-1.0.0/`, `1.1.0/`, `1.2.0/`. **02/10/2026 17:49 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Portal-1.3.2.jar`** (point de chute : option courte ; 1.3.1 dans `_removed-klm_portal-1.3.1/`). **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`). **28/09/2026 12:03 : `KLM_Portal-1.3.1.jar`** (« Ajouter un portail » : seulement les régions `portal_...` ; 1.3.0 à 11:59 : garde-fou : pas de portail sur le lobby entier ni sur la zone d'arrivée ; 1.2.0 dans `_removed-klm_portal-1.2.0/` ; non testé ; le portail `lobby` → event créé par erreur à 11:49 est à retirer avec `/klmportal remove lobby`). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.3.0 dans `_removed-klm_menu-2.3.0/`, non testé). Avant : **`KLM_Menu-2.3.0.jar` + `KLM_Portal-1.1.0.jar`** (26/09/2026 19:37 : points de chute, arrivée au centre `7.5 67 39.5` testée et confirmée par LeKiwi06 ; `relay-token` de KLM_Portal encore vide ; **`KLM_Portal-1.2.0.jar`** déployé à 20:54 (effets de zone speed 6 / jump_boost 2 dans la région `lobby` ; testé et confirmé par LeKiwi06 le 27/09/2026 ; 1.1.0 dans `_removed-klm_portal-1.1.0/`) ; 2.2.0 / 1.0.0 dans `_removed-…`) ; avant : `KLM_Menu-2.2.0.jar` + `KLM_Portal-1.0.0.jar` (26/09/2026 18:25, LeKiwi06 : portails en régions WorldGuard reliés aux destinations du menu, interface « Ajouter un portail » ; ConditionalEvents 4.79.2 et PyxelRegions 1.2.2 rangés dans `_removed-…`, dossiers de données laissés ; 2.0.0 dans `_removed-klm_menu-2.0.0/` ; testés et confirmés par LeKiwi06 le 26/09/2026 ; 4 portails. KLM_Menu 2.3.0 + KLM_Portal 1.1.0 déployés ensuite) ; avant : `KLM_Menu-2.0.0.jar` (destinations au 24/09/2026 : `kixster` (désactivée), `kal-games`, `serveur-jeux`, `kal-test-dev`, `event` (désactivée) ; activer / désactiver en jeu : menu > Paramètres) | non testé (catalogue « Interfaces » ; nouvelles destinations) |
| proxy | **29/09/2026 17:23 (Maxster33) : `geyser-bedrock` 1.2.0** (+ Fragment et Cœur de Spawner, Changeur de Biome ; 1.1.0 dans `plugins/Geyser-Velocity/_removed-kalium-objets-1.1.0/` ; non testé). **29/09/2026 12:56 (Maxster33) : `geyser-bedrock` 1.1.0** (`plugins/Geyser-Velocity/custom_mappings/kalium_objets.json` + `packs/KaLium-objets-1.1.0.mcpack` : Estomac du gardien, Clé de l'End, Bedrock Breaker ; 1.0.0 dans `plugins/Geyser-Velocity/_removed-kalium-objets-1.0.0/` ; actif après redémarrage du proxy, non testé ; 1.0.0 testée et confirmée par Maxster33 le 29/09/2026) ; icône de la liste Multijoueur : `server-icon.png` (64 x 64, logo KaLium, 26/09/2026) à la racine ; `KaliumRelay-1.2.0.jar` (26/09/2026 07:21 : `/server` réservé aux admins, liste `admins` de `plugins/kaliumrelay/relay.properties`, LeKiwi06 et Maaxster par défaut ; testé et confirmé par LeKiwi06 le 26/09/2026 ; 1.1.1 dans `_removed-kaliumrelay-1.1.1/`) ; 1.1.1 (déployé le 24/09/2026) | **03/10/2026 06:45 (LeKiwi06, non testés) : `KaliumRelay-1.4.0.jar`** (`POST /ban` vers LibertyBans ; 1.3.0 dans `_removed-kaliumrelay-1.3.0/`) **+ `LibertyBans_Release-1.1.4.jar`** (nouveau, bannissement de tout KaLium). **03/10/2026 01:05 (LeKiwi06, non testé) : `KaliumRelay-1.3.0.jar`** (boîte aux lettres durable des récompenses vers Event ; 1.2.0 dans `_removed-kaliumrelay-1.2.0/`). relais confirmé le 24/09/2026 en 1.1.0 ; démarrage 1.1.1 vérifié dans le journal ; reconnexion directe non confirmée |
| Kanvas (ex Kal-Test-Dev) | **03/10/2026 22:47 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » de la boussole sous 4 joueurs ; 2.6.0 dans `_removed-klm_menu-2.6.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global send <message> | true | false`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`, `2.3.0/`, `2.4.0/`. **03/10/2026 01:05 (LeKiwi06, non testés) : `KV_Plots-1.5.0`** (notes reçues, signaux) + **`KV_Rewards-1.0.0`** (nouveau : récompenses des notes et des concours ; `config.yml` posé, `relay-token` à remplir) + `KLM_Menu-2.6.0` ; anciens jars dans les `_removed-…`. **02/10/2026 17:49 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Portal-1.3.2.jar`** (point de chute : option courte ; 1.3.1 dans `_removed-klm_portal-1.3.1/`). **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`). **28/09/2026 12:03 : `KLM_Portal-1.3.1.jar`** (1.3.0 et 1.2.0 dans les `_removed-klm_portal-…/`, non testé). **28/09/2026 03:40 (LeKiwi06, refonte des menus, non testé) : `KLM_Menu-2.4.0`, `KV_BuildBattle-0.3.6`, `KV_Menu-1.3.1`** (anciennes versions dans les `_removed-…`). Avant : **`KV_BuildBattle-0.3.5.jar`** (28/09/2026 01:17, LeKiwi06 : Build Battle complet, voir son JOURNAL ; anciens jars dans `_removed-kv_buildbattle-…/`, supprimables par l'humain : 0.1.0, 0.2.0, 0.3.0, 0.3.1) + **`KV_Plots-1.4.1.jar`** + **`KLM_Menu-2.3.0.jar`** + **`KLM_Portal-1.2.0.jar`** (27/09/2026 19:59 : randomTickSpeed à 0 dans `Kanvas` et `buildbattle`, points de chute appliqués à l'arrivée ; anciens jars dans `_removed-kv_plots-1.4.0/`, `_removed-klm_menu-2.0.0/`) + `KV_Menu-1.3.0.jar` (26/09/2026 03:45, LeKiwi06 ; signalements, concours de build) ; FastAsyncWorldEdit **Paper** 2.15.4 (la variante Bukkit ne fonctionnait pas sur Paper 26.2 ; limites `limits.default` réduites, baguette de navigation = vide de structure ; originaux dans `_removed-fastasyncworldedit-config-2026-09-26/`) ; WorldGuard 7.0.19 ; LuckPerms (groupe `default` : permissions FAWE), PlaceholderAPI, ViaVersion / ViaBackwards, floodgate, voicechat 2.6.23 ; monde principal `Kanvas` (`level-name=Kanvas`). **Tri du 26/09/2026** : KaliumCore 1.4.0, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot, PyxelRegions rangés dans `plugins/_removed-<plugin>-<version>/` (dossiers de données laissés en place). Anciens dossiers `_removed-kv_plots-…` / `kv_menu-…` supprimés le 26/09/2026 par LeKiwi06 (règle des 3 versions) ; anciens `_removed-kaliumcore-…` supprimés aussi. `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` (extension PlaceholderAPI qui ne se chargeait pas, inutilisée) rangé dans `_removed-placeholderapi-scoreboard/` le 26/09/2026 (accord de LeKiwi06) | KV_Plots 1.0.0 à 1.2.0, KV_Menu 1.0.0 et 1.1.0, FAWE : testés et confirmés par LeKiwi06 le 26/09/2026 ; KV_Plots 1.3.0 / 1.3.1 (titre, description, visites, tableau sur le côté) et KV_Menu 1.2.0 testés et confirmés par LeKiwi06 le 26/09/2026 ; tri des plugins vérifié (plus d'erreur au démarrage) ; KV_Plots 1.4.0 et KV_Menu 1.3.0 (signalements, concours de build) testés et confirmés par LeKiwi06 le 26/09/2026 ; KV_BuildBattle 0.1.0 à 0.3.5, KV_Plots 1.4.1, KLM_Menu 2.3.0 et KLM_Portal 1.2.0 (point de chute, boussole, étoile) testés et validés par LeKiwi06 les 27 et 28/09/2026 |
| Kixster (`kixster`) | **03/10/2026 22:47 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » de la boussole sous 4 joueurs ; 2.5.0 dans `_removed-klm_menu-2.5.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global send <message> | true | false`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`, `2.4.0/`. **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`). **28/09/2026 04:03 : `KLM_Menu-2.4.1.jar`** + config (`compass.enabled: true`, `items-by-default: false` : objets de menu seulement après `/menu on` ; non testé). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.0.0 dans `_removed-klm_menu-2.0.0/`) et alias `menu` retiré de `commands.yml` (non testé). Avant : remis dans l'état d'avant le Bingo le 24/09/2026 : monde d'origine `Kixster SMP` (ex `Kixster SMP_bak`, dernière sauvegarde 23/09 00:11), plugins SMP du 14/09 + WorldEdit 7.4.6-beta (gardé) + `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) ; tout le Bingo rangé dans `/_removed-bingo-2026-09-24/` et `/plugins/_removed-bingo-2026-09-24/` | non testé |
| Event (`event`, ancien kal-games) | **04/10/2026 22:01 (LeKiwi06, non testés, actifs après redémarrage) : `KS_Economy-1.3.0.jar`** (barème des prix dans le plugin, rachats de la semaine : `/rachat`, 10 objets tirés chaque lundi) **+ `KS_LootPotions-1.1.3.jar`** (potion de chance à 100 % sur le minerai d'émeraude de deepslate, jamais avec Toucher de soie) ; anciens jars dans `_removed-ks_economy-1.2.0/` et `_removed-ks_lootpotions-1.1.2/` ; supprimables par l'humain : `_removed-ks_economy-1.0.0/` à `1.1.5/` (10), `_removed-ks_lootpotions-1.0.0/` à `1.1.1/` (3). **04/10/2026 06:52 (LeKiwi06, non testés, actifs après redémarrage) : `KS_Crafts-1.10.0.jar`** (plus de brassage au bloc de verrue, verrue du Nether refusée à l'alambic : les potions ne viennent plus que du butin et de /rewards ; boîte de shulker fabriquée avec un coffre de l'Ender) **+ `KS_LootBlocs-1.3.0.jar`** (la verrue du Nether redevient cultivable) **+ `KS_LootPotions-1.1.2.jar`** (potion de la verrue : verrue mûre seulement) ; anciens jars dans `_removed-ks_crafts-1.9.0/`, `_removed-ks_lootblocs-1.2.0/`, `_removed-ks_lootpotions-1.1.1/` ; supprimables par l'humain : `_removed-ks_crafts-1.0.0/` à `1.7.0/` (8), `_removed-ks_lootblocs-1.0.0/`, `_removed-ks_lootpotions-1.0.0/`. **04/10/2026 06:05 (LeKiwi06, non testé, actif après redémarrage) : `KS_LootCoffres-1.1.0.jar`** (jambières en diamant de la cité antique enchantées niveau 30 à 50 au lieu de 10 à 32, pour que Raccommodage y sorte aussi souvent que sur le plastron des bastions, les bottes des cités de l'End et le casque de l'Estomac ; 1.0.0 dans `_removed-ks_lootcoffres-1.0.0/`). **03/10/2026 22:47 (LeKiwi06, non testés, actifs après redémarrage) : `KLM_Menu-2.9.0.jar`** (bouton « Recherche de joueurs » de la boussole sous 4 joueurs ; 2.8.0 dans `_removed-klm_menu-2.8.0/`) **+ `KLM_Chat-1.0.0.jar`** (nouveau : tchat inter-serveur `/global send <message> | true | false`) ; supprimables par l'humain : `_removed-klm_menu-2.0.0/`, `2.4.0/`, `2.4.1/`, `2.5.0/`, `2.6.0/`. **03/10/2026 19:06 (LeKiwi06, non testé) : `KS_Crafts-1.9.0.jar`** (toutes les recettes dans le livre de recettes, débloquées à l'obtention d'un ingrédient ; 1.8.0 dans `_removed-ks_crafts-1.8.0/`). **03/10/2026 18:42 (LeKiwi06, non testés) : KS_AntiCheat 1.1.1** (minage par minerai ; correctif de duplication invsee / ecsee) **+ KS_EC_Extension 1.1.0 + KLM_Menu 2.8.0** (Modération dans la navigation) **+ KS_LootPotions 1.1.1 + KS_Economy 1.2.0** (ventes, classement, favoris, recherche ; une boutique, un acheteur) **+ KS_FairPlay 1.0.3 + KS_KaliumGive 1.8.0 + KS_Decapitator 1.1.0 + KS_Crafts 1.8.0** (tête de wither squelette à réparer, bloc de charbon de bois) **+ KS_Tableau 1.0.0 et KS_Jetons 1.0.0** (nouveaux) ; anciens jars dans les `_removed-…`. **03/10/2026 16:41 (LeKiwi06) : ConditionalEvents 4.79.2 et PyxelRegions 1.3.0 retirés** (restes du hub ; jars dans `_removed-conditionalevents-4.79.2/` et `_removed-pyxelregions-1.3.0/`, dossiers de données laissés ; effectif au redémarrage). **03/10/2026 07:46 (LeKiwi06, non testé) : `KS_AntiCheat-1.0.1.jar`** (invsee / ecsee hors ligne lus dans le fichier de sauvegarde du joueur ; 1.0.0 dans `_removed-ks_anticheat-1.0.0/`). **03/10/2026 06:45 (LeKiwi06, non testés) : catégorie 6 Anti-triche : `KS_AntiCheat-1.0.0.jar`** (nouveau ; `relay-token` à remplir) **+ `KS_Economy-1.1.5` + `KS_RewardsGUI-1.0.1`** (signaux pour l'anti-triche) **+ `CoreProtect-CE-24.1.jar`, `CauldronInteract-1.4.0-RELEASE.jar`, datapack `Woodcutter-7.2.zip`** (téléchargés avec l'accord de LeKiwi06) **+ configuration Paper** (`allow-piston-duplication: true`, `lava-obscures: true` ; originaux dans `/_removed-config-2026-10-03/cat6/`). **03/10/2026 06:07 (LeKiwi06, non testé) : `KS_FairPlay-1.0.2.jar`** (« Exploration journalière : n / 10 », barre du temps de l'Élixir de Fortune ; 1.0.1 dans `_removed-ks_fairplay-1.0.1/`). **03/10/2026 05:45 (LeKiwi06, non testé) : `KS_FairPlay-1.0.1.jar`** (comptage des ouvertures revu, message en créatif ; 1.0.0 dans `_removed-ks_fairplay-1.0.0/`). **03/10/2026 05:18 (LeKiwi06, non testés) : catégorie 5 FairPlay : `KS_FairPlay-1.0.0.jar`** (nouveau) **+ `journeymap-paper-26.2-6.0.9.jar` + `legacyfreecam-paper-2.0.0.jar`** (plugins serveur téléchargés avec l'accord de LeKiwi06) **+ configuration Paper** (anti-xray mode 1 overworld et Nether, coupé dans l'End ; suivi des entités réduit dans `spigot.yml` ; originaux dans `/_removed-config-2026-10-03/`). **03/10/2026 03:22 (LeKiwi06, non testés) : `KLM_Menu-2.7.0.jar`** (rubrique « Modération » dans `/menu` ; 2.6.0 dans `_removed-klm_menu-2.6.0/`) **+ `KS_Economy-1.1.4.jar`** (signalements des magasins dans cette rubrique ; 1.1.3 dans `_removed-ks_economy-1.1.3/`). **03/10/2026 02:35 (LeKiwi06, non testés) : `KS_Economy-1.1.3.jar`** (catalogue sans son magasin, pas d'achat chez soi, « Voir l'objet », signalements, `/magasin signalements`, permission `kseconomy.staff` ; 1.1.2 dans `_removed-ks_economy-1.1.2/`) **+ `KS_Claim-1.1.2.jar`** (plus de bannissement, bannissements existants levés au démarrage ; 1.1.1 dans `_removed-ks_claim-1.1.1/`). **03/10/2026 01:49 (LeKiwi06, non testé) : `KS_Economy-1.1.2.jar`** (boutiques : noms français, recherche corrigée, panneau avec état, nom de boutique, fermeture temporaire, panneau retiré à la suppression, place prise 3 h, coffres en cuivre ; 1.1.1 dans `_removed-ks_economy-1.1.1/`). **03/10/2026 01:05 (LeKiwi06, non testés) : `KS_RewardsGUI-1.0.0.jar`** (nouveau : `/rewards`, récompenses des autres serveurs ; `config.yml` posé, `relay-token` à remplir par l'humain) + `KS_KaliumGive-1.7.0` + `KLM_Menu-2.6.0` (bouton Récompenses) + **`KS_Claim-1.1.1`, `KS_Economy-1.1.1`, `KS_BiomeChanger-1.1.1`** (API de SimpleClaimSystem initialisée : claims et magasins ne répondaient pas) ; anciens jars dans les `_removed-…`. **03/10/2026 00:02 (LeKiwi06, non testés) : `KS_Economy-1.1.0.jar`** (magasins et boutiques maison dans la région `zone_shop`, `/magasin`, catalogue, achat et gestion à distance ; 1.0.3 dans `_removed-ks_economy-1.0.3/`) **+ `KS_Claim-1.1.0.jar`** (claim d'un magasin avec boutiques : ni supprimé ni vendu ; 1.0.0 dans `_removed-ks_claim-1.0.0/`). **À faire par l'humain : région WorldGuard `zone_shop` (`/rg define zone_shop`, `/rg flag zone_shop build allow`, `/rg flag zone_shop scs-claim allow`).** **02/10/2026 22:26 (LeKiwi06, catégorie 3 « Claims », non testés) : `SimpleClaimSystem-1.13.1.jar` (plugin tiers, nouveau ; `config.yml` et `langs/fr_FR.yml` posés avant le premier démarrage, voir `KS_Claim/scs/README.md`), `KS_Claim-1.0.0.jar` (nouveau : `/ksclaims`, `/ksclaim`, groupes, prix, vente), `KS_BiomeChanger-1.1.0.jar` (refus dans le claim d'un autre ; 1.0.0 dans `_removed-ks_biomechanger-1.0.0/`). Restent à faire par l'humain : commandes LuckPerms (retirer `scs.command.*` au groupe default) et région WorldGuard `ile_du_dragon` (`scs-claim deny`) dans l'End.** **01/10/2026 23:42 (LeKiwi06, testé et confirmé par LeKiwi06 le 02/10/2026) : `KLM_Menu-2.5.0.jar`** (aucun texte qui défile dans les menus : outil `Lisible` ; ancien jar dans `_removed-klm_menu-2.4.x/`) + **`KS_Economy-1.0.3.jar`** (saisie du montant ajustée ; 1.0.2 dans `_removed-ks_economy-1.0.2/`). **01/10/2026 20:24 (Maxster33, non testé) : `KS_LootBlocs-1.2.0.jar`** (fer et or bruts : vanilla x2, soit 2 puis Fortune vanilla, au lieu de 2 à 5 ; 1.1.0 dans `_removed-ks_lootblocs-1.1.0/`). **01/10/2026 19:50 (Maxster33, non testé) : `KS_Enclume-1.2.0.jar`** (coût vanilla jusqu'à 50 niveaux, partie au-dessus comptée pour moitié ; fioles de KS_FioleExp inchangées ; 1.1.4 dans `_removed-ks_enclume-1.1.4/` ; supprimables par l'humain : `_removed-ks_enclume-1.0.0/`, `1.1.0/`, `1.1.1/`, `1.1.2/`). **01/10/2026 18:54 (LeKiwi06, non testé) : `KS_Economy-1.0.2.jar`** (plus aucun texte qui défile dans les champs : liste du retrait raccourcie ; 1.0.1 dans `_removed-ks_economy-1.0.1/`). **01/10/2026 01:33 (LeKiwi06, non testé) : `KS_Economy-1.0.1.jar`** (textes des boutons du menu Économie et du panneau d'échange corrigés ; 1.0.0 dans `_removed-ks_economy-1.0.0/`). **01/10/2026 00:49 (LeKiwi06, catégorie 2 « Économie », 1re partie, non testés) : `KS_Menu-1.0.0.jar` (nouveau : étoile du Nether après `/menu on`, menu d'Event, `/event`), `KS_Economy-1.0.0.jar` (nouveau : score, blocs compressés, menu Économie `/economie`, solde dans la liste Tab, masquage à 1 %/jour, `/echange`, économie Vault), `KS_Elixir-1.1.0.jar` (recette de l'Élixir de Fortune ; 1.0.0 dans `_removed-ks_elixir-1.0.0/`) + **VaultUnlocked 2.20.2** (plugin tiers, nouveau, `VaultUnlocked-2.20.2.jar`, Modrinth).** **30/09/2026 22:44 (LeKiwi06, testé et confirmé par LeKiwi06 le 30/09/2026) : `KS_LootEntites-1.4.0.jar`** (Warden : fioles à 15 % au lieu de 10 %, catalyseur remplacé par un bloc du Deep Dark : 13 blocs à 7,65 %, minerais de deepslate à 0,08 % chacun, émeraude de deepslate à 0,01 % ; 1.3.0 dans `_removed-ks_lootentites-1.3.0/` ; supprimables par l'humain : `_removed-ks_lootentites-1.0.0/`, `1.1.0/`, `1.2.0/`). **30/09/2026 18:07 (LeKiwi06, catégorie 1 « Contenu survie », testés et confirmés par LeKiwi06 le 30/09/2026) : `KS_Decapitator-1.0.0.jar` (nouveau : 332 têtes de mobs, 1 % quand un joueur tue le mob, une par variante, état et bébé, `/tetes` pour les ops ; textures manquantes : 16 shulkers colorés, cube de soufre, bébé noyé), `KS_Elixir-1.0.0.jar` (nouveau : 11 élixirs en anneau, bloqués à l'alambic ; recette de Fortune en attente de KS_Economy), `KS_Crafts-1.7.0.jar` (spawners avec les têtes de KS_Decapitator), `KS_ItemSimple-1.1.0.jar` (têtes « Steve » retirées), `KS_KaliumGive-1.6.0.jar` (`elixir_…`, `tete_…`), `KS_LootEntites-1.3.0.jar` (Warden : fiole de KS_FioleExp) ; anciens dans `_removed-ks_crafts-1.6.0/`, `_removed-ks_itemsimple-1.0.0/`, `_removed-ks_kaliumgive-1.5.0/`, `_removed-ks_lootentites-1.2.1/` ; supprimables par l'humain : `_removed-ks_crafts-1.0.0/` à `1.4.0/`, `_removed-ks_kaliumgive-1.0.0/` à `1.3.0/`, `_removed-ks_lootentites-1.0.0/` et `1.1.0/`.** **29/09/2026 17:22 (Maxster33) : `KS_ItemSimple-1.0.0.jar`, `KS_Spawners-1.0.0.jar`, `KS_BiomeChanger-1.0.0.jar` (nouveaux : Fragment et Cœur de Spawner, têtes, 8 spawners avec créature, Changeur de Biome sphère ou cube avec historique, refusé dans les régions WorldGuard), `KS_Crafts-1.6.0.jar` (recettes des spawners et du Changeur de Biome, livre de recettes au 1er fragment), `KS_KaliumGive-1.5.0.jar` (16 id), `KS_LootBlocs-1.1.0.jar` (fragments sur les spawners naturels) ; anciens dans `_removed-ks_crafts-1.5.0/`, `_removed-ks_kaliumgive-1.4.0/`, `_removed-ks_lootblocs-1.0.0/`** (non testés). **29/09/2026 14:22 (Maxster33) : `KS_LootEntites-1.2.1.jar` (Warden : 10 % de fiole, ligne « rien » de poids 315 ; 1.2.0 dans `_removed-ks_lootentites-1.2.0/`)** (non testé). **29/09/2026 14:18 (Maxster33) : `KS_FioleExp-1.5.0.jar` (fiole remplie en niveaux, points en description, menu détaillé), `KS_KaliumGive-1.4.0.jar` (`fiole_exp(N)` en niveaux) et `KS_LootEntites-1.2.0.jar` (Warden : une fiole de niveau 10, 15, 20, 30 ou 40 ; endermite : nouvelle fiole de 10 niveaux ; tués par un joueur) ; anciens dans `_removed-ks_fioleexp-1.4.0/`, `_removed-ks_kaliumgive-1.3.0/`, `_removed-ks_lootentites-1.1.0/`** (non testés). **29/09/2026 13:59 (Maxster33) : `KS_BedrockBreaker-1.0.2.jar` (couche du fond de nouveau cassable ; 1.0.1 dans `_removed-ks_bedrockbreaker-1.0.1/`) et `KS_Crafts-1.5.0.jar` (nouvelle recette du Bedrock Breaker : poudre de blaze, charge de feu, wagonnets à TNT, cristal de l'End, ancre de réapparition ; 1.4.0 dans `_removed-ks_crafts-1.4.0/`)** (non testés). **29/09/2026 13:30 (Maxster33) : `KS_BedrockBreaker-1.0.1.jar` (un bloc par seconde au plus ; 1.0.0 dans `_removed-ks_bedrockbreaker-1.0.0/`)** (non testé). **29/09/2026 12:55 (Maxster33) : `KS_BedrockBreaker-1.0.0.jar` (nouveau : Bedrock Breaker, image du bâton de blaze, usage unique, pas la couche du fond, protections WorldGuard respectées), `KS_Crafts-1.4.0.jar` (recette : 8 TNT + houe en diamant ; livre de recettes à l'obtention d'une TNT ou d'une houe en diamant ; 1.3.0 dans `_removed-ks_crafts-1.3.0/`) et `KS_KaliumGive-1.3.0.jar` (id `bedrock_breaker` ; 1.2.0 dans `_removed-ks_kaliumgive-1.2.0/`)** (non testés). **29/09/2026 10:57 (Maxster33) : `KS_FioleExp-1.4.0.jar` (usure de l'enclume à 6 % par fiole remplie au lieu de 12 %, réglable : `usure-enclume-pourcent` dans `plugins/KS_FioleExp/config.yml`, créé au premier démarrage ; 1.3.0 dans `_removed-ks_fioleexp-1.3.0/`)** (non testé). **29/09/2026 10:24 (Maxster33) : `KS_FioleExp-1.3.0.jar` (joueurs Java aussi : accroupi + clic droit sur une enclume avec une fiole vide → dialogue natif, mêmes textes que le formulaire Bedrock ; 1.2.0 dans `_removed-ks_fioleexp-1.2.0/`)** (non testé). **29/09/2026 05:58 (Maxster33) : `KS_FioleExp-1.2.0.jar` (Bedrock : accroupi + clic droit sur une enclume avec une fiole vide → formulaire, coût en niveaux affiché avant confirmation ; 1.1.0 dans `_removed-ks_fioleexp-1.1.0/`)** (testé et confirmé en Bedrock par Maxster33 le 29/09/2026). **29/09/2026 05:43 (Maxster33) : `KS_Enclume-1.1.4.jar` (Bedrock : vrai coût en dernière ligne de l'objet de la 1re case, retiré à la sortie de l'enclume ; 1.1.3 dans `_removed-ks_enclume-1.1.3/`)** (non testé). **29/09/2026 05:15 (Maxster33) : `KS_Enclume-1.1.3.jar` (joueurs Bedrock au-delà de 39 niveaux : 39 affiché s'ils ont assez de niveaux, sinon « Trop cher ! » ; 1.1.2 dans `_removed-ks_enclume-1.1.2/`)** (non testé). **29/09/2026 04:31 (Maxster33) : `KS_Enclume-1.1.2.jar` (au-delà de 39 niveaux : aucun coût affiché par l'enclume, vrai coût dans la description du résultat, retiré par l'enclume à la prise ; 1.1.1 dans `_removed-ks_enclume-1.1.1/`)** (non testé). **29/09/2026 04:17 (Maxster33) : `KS_Enclume-1.1.1.jar` (correctif : résultat masqué par une croix rouge au-delà de 39 niveaux ; vrai coût en dernière ligne de la description du résultat ; 1.1.0 dans `_removed-ks_enclume-1.1.0/`)** (non testé). **29/09/2026 03:54 (Maxster33) : `KS_FioleExp-1.1.0.jar` (fioles d'expérience non renommables) et `KS_Enclume-1.1.0.jar` (remplacé à 04:17 par 1.1.1) ; anciens dans `_removed-ks_fioleexp-1.0.0/` et `_removed-ks_enclume-1.0.0/`** (non testés). **29/09/2026 03:39 (Maxster33) : `KS_FioleExp-1.0.0.jar` (nouveau, remplacé à 03:54 : XP stocké dans une fiole à l'enclume) et `KS_KaliumGive-1.2.0.jar` (id `fiole_exp(<points>)` ; 1.1.0 dans `_removed-ks_kaliumgive-1.1.0/`)** (testés et confirmés par Maxster33 le 29/09/2026). **28/09/2026 23:19 (Maxster33) : `KS_EstomacGardien-1.3.0.jar` (1.1.0 à 23:25 avec `/estomac`, retirée en 1.2.0 à 23:35 ; 1.3.0 le 29/09 à 00:02 : casque 20 %), `KS_KaliumGive-1.1.0.jar` (23:35 : `/kaliumgive <pseudo> <id_custom> <nombre>`, opérateurs ; 1.1.0 à 23:47 : id `cle_de_l_end`), `KS_EC_Extension-1.0.0.jar` (23:47 : coffre de l'Ender à 6 lignes, 3 du bas débloquées case par case avec la Clé de l'End), `KS_Crafts-1.3.0.jar` (23:47 : craft de la Clé de l'End ; 1.3.0 le 29/09 à 00:27 : recette dans le livre de recettes dès qu'un ingrédient est obtenu) et `KS_LootCoffres-1.0.0.jar` (nouveaux), `KS_LootEntites-1.1.0.jar`, `KS_LootPotions-1.1.0.jar`** (anciens dans `_removed-ks_lootentites-1.0.0/` et `_removed-ks_lootpotions-1.0.0/` ; non testés ; demande : `KS_Event/CAHIER_DES_CHARGES.md`, partie 4). **28/09/2026 04:03 : `KLM_Menu-2.4.1.jar`** + config (`compass.enabled: true`, `items-by-default: false` : objets de menu seulement après `/menu on` ; non testé). **28/09/2026 03:40 : `KLM_Menu-2.4.0.jar`** (2.0.0 dans `_removed-klm_menu-2.0.0/`) et alias `menu` retiré de `commands.yml` (non testé). Avant : reconverti en **survie classique** le 25/09/2026 : nouveau monde `world` (généré au premier démarrage, seed aléatoire, difficulté hard) ; ancien hub `Kal-Games` et vieux `world` dans `/_removed-survie-2026-09-25/` ; KalGames, KG_Bingo, KG_ScoreBoards (et tous leurs anciens jars) dans `/plugins/_removed-kalgames-event-2026-09-25/` ; `KLM_Menu-2.0.0.jar` (boussole désactivée, `/menu` = alias de `/servers` dans `commands.yml`) + **`KS_Dimensions-1.0.0.jar`** + `KS_Enclume`, `KS_Villageois`, `KS_Crafts`, `KS_LootBlocs`, `KS_LootEntites`, `KS_LootPeche`, `KS_LootPotions` 1.0.0 (25/09/2026, cahier des charges `KS_Event/CAHIER_DES_CHARGES.md`) ; + Floodgate (config et clé du proxy, 25/09/2026) ; PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`) ; extension PlaceholderAPI remise dans `plugins/PlaceholderAPI/expansions/` ; plugins tiers du hub gardés (ConditionalEvents, PyxelRegions, WorldGuard, GrimAC, JEIRecipeFix, PlaceholderAPI, ViaVersion / ViaBackwards, LuckPerms, voicechat, WorldEdit) | non testé |

Confirmé par l'utilisateur le 23/09/2026, avant le Rush : « tout fonctionne très bien ».

### Versions compilées, non déployées

- **KG_ScoreBoards 1.7.0** : déployé sur Kal-Games le 29/09/2026 à 2 h 36 (voir le tableau ci-dessus).
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
  1.1.0 : chrono par checkpoint, anti-collision. **1.2.0 (déployé le 04/10/2026 à 22:16, non testé) : nouveau barème**
  (voir son `JOURNAL.md`). Restent : cumul crédité en fin de partie, chrono réglable par checkpoint, barème calé sur la course de
  bateau (voir `EQUILIBRAGE_POINTS.md`), contre-la-montre solo avec fantôme (Mannequin).
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

### 2026-10-05 — Maxster33

*(Session du 05/10/2026, de 12 h 54 à 13 h 40. Fin de session : réservations libérées.)*

**KG_BingoGame 0.10.0 (Serveur Jeux) et KG_Bingo 1.7.3 (Kal-Games), déployés à 13:34, non testés, actifs après redémarrage des deux serveurs (à garder ensemble)** : réserve de mondes par lots. Constat de Maxster33 : deux
parties à deux jours d'écart sur la même seed. Cause : en 0.8.5, le monde ajouté à la réserve reprenait la seed du
monde restant, qui pouvait donc revenir indéfiniment jusqu'au redémarrage (parties créées pendant une autre partie).
Désormais : au démarrage, mondes de réserve supprimés puis 3 lots de 4 mondes et 3 lots solo préparés (une seed neuve
par lot) ; une partie créée pendant une autre prend un lot (mondes en trop, équipes vides, partie annulée : supprimés ;
sans autre partie en cours : mondes neufs, réserve non entamée) ; le lot est remplacé
à la fin de la partie, une fois ses mondes effacés du disque ; pendant une partie, seul le terrain de la réserve avance
(5 chunks/s) ; à 4 parties en cours, réserve en pause ; aucun lot libre : mondes créés pour la partie. Nouvelles clés
(valeurs par défaut dans le code) : `instances.reserve-lots-4-teams`, `reserve-lots-solo`,
`reserve-chunks-per-second-during-games`. Détail : `KG_BingoGame/JOURNAL.md`,
`historique-conversations/2026-10-05-bingo-reserve-lots.md`.

**Contre la montre en bingos (KG_BingoGame 0.10.0 + KG_Bingo 1.7.3, mêmes jars, déployés
ensemble)** : victoire avec le nombre de bingos choisi (3 par défaut) au lieu de toute la grille ; 10 bingos au plus
à 1 équipe (tous modes), 12 à plusieurs ; bonus inchangés (temps restant ; bingos comme dans toute partie à 1 équipe,
sans bonus « en 1er »). Réserve de mondes : une partie créée sans autre partie en cours
garde des mondes neufs (la réserve n'est entamée que pendant une autre partie).

### 2026-10-02 — LeKiwi06

**Ajout (05/10/2026, 12:33) : trois demandes de LeKiwi06, appliquées sur Kal-Games (serveur arrêté par LeKiwi06) et Serveur Jeux ; non testé.**
1. **« Récupère tous les points non crédités en admin, crédite-les »** : seul LeKiwi06 en avait (journal de KG_ScoreBoards, raison « operateur », hors rattrapages du 28/09). Crédités dans `stats.yml` : Bingo 373,4 (29/09 : 338,8 avec la moyenne, au lieu de 432,8 ; 01/10 : 34,6), PvP Kit 270,25, Parcours 103,5 (parties à l'ancien barème revalorisées au barème 1.2.0, fourchette haute, comme tout le monde ; podium à 0), course de bateau 37. Une ligne « credit » par événement ajoutée au journal d'octobre (`by` : « Claude (demande de LeKiwi06) » ; mêmes références que `/classements verifier`, qui ne les reproposera pas). Non récupérable : course de bateau du 24/09 (aucun point dans le journal). Temps et records d'opérateur non repris (les meilleurs tours des derniers jours restent proposés par `/classements verifier`).
2. **« Supprime cette règle »** (les points d'un opérateur ne comptent pas) : désactivée par configuration, `stats.exclude-operators: false` dans `KalGames/config.yml` (lu aussi par KG_Bingo) et `KG_ScoreBoards/config.yml`. **L'option existe toujours dans le code, activée par défaut** (KalGames `ScoreBridge.excluded`, KG_Bingo, KG_ScoreBoards) : un serveur installé avec une config neuve l'appliquerait de nouveau.
3. **« En multijoueur on ne gagne plus la somme des joueurs en dessous de nous, mais la moyenne ; rétroactif »** : KG_BoatRace 1.5.0 (Kal-Games) et KG_BingoGame 0.9.0 (Serveur Jeux), les deux seuls jeux qui avaient cette règle : ses points + la moyenne des points de ceux classés en dessous (à 2 joueurs rien ne change). Rétroactif dans `stats.yml` (général ; octobre et semaine pour les parties concernées ; archives non touchées) : 6 courses de bateau à 3 joueurs ou plus (journal) et 7 parties de Bingo (points propres reconstitués depuis les lignes « terminee » des logs de Serveur Jeux, multiplicateur de blackout x1 vérifié ; parties d'avant la 0.8.0 créditées au double : écart doublé). Écarts au général : .PatientLime2170 -699,25 (Bingo -507,17, bateau -192,08), JujuEngineer -214,5 (Bingo), LeKiwi06 -309,33 (Bingo -287,83, bateau -21,5), .TomHeroes57 -77,34 (Bingo -53, bateau -24,34), Maaxster -54 (Bingo). Pas de ligne au journal pour ces écarts.
Originaux (stats, journaux d'octobre, configs) dans `/plugins/_removed-kg_scoreboards-stats-2026-10-05/` et `/plugins/_removed-kalgames-config-2026-10-05/`. Détail : `historique-conversations/2026-10-05-operateurs-moyenne.md`. À tester : une partie en opérateur compte (plus de message « Mode opérateur ») ; course de bateau et Bingo à 3 joueurs ou plus (total crédité = points + moyenne). Réservations libérées.

**Ajout (04/10/2026, 22:26) : points du Parcours recalculés avec le barème de KG_Parkour 1.2.0, dans `stats.yml` de KG_ScoreBoards sur Kal-Games (serveur arrêté par LeKiwi06).** Demande de LeKiwi06 : « prends la fourchette haute pour tout le monde et crédite les points ». Méthode : les 67 parties du journal (depuis le 25/09/2026) rejouées point de contrôle par point de contrôle (points de base selon la position, 1er à valider dans les parties à plusieurs, « sans chute » supposé partout = fourchette haute ; ni bonus de temps ni +25, personne n'ayant fini le parcours) ; points d'avant le journal par règle de trois (rapport du joueur s'il a au moins 20 points dans le journal, sinon rapport global x2,90). Général : .TomHeroes57 334 → 1 108,43 ; LeKiwi06 107 → 291,04 ; .MRMister7866 110 → 250,16 ; Exyosis 73 → 222,61 ; .PatientLime2170 106 → 210,68 ; JujuEngineer 16 → 46,75 ; Ronoxsyb 15 → 43,43 ; Maaxster 25 → 30,66 ; NeirdaMC 7 → 20,27 ; LE_TIGRE32 5 → 16,5 ; .Lyfty4013 4 → 7,5. Octobre : .TomHeroes57 64 → 202,5 ; .MRMister7866 24 → 48 ; LeKiwi06 9 → 32 ; .PatientLime2170 17 → 31,5 ; JujuEngineer 4 → 12 ; Maaxster inchangé à 17 (le recalcul donnait 7,5 : aucun retrait). Semaine : .TomHeroes57 54 → 171. Original dans `/plugins/_removed-kg_scoreboards-stats-2026-10-04/stats.yml`. Non touchés : `archives/2026-09.yml`, archives des semaines, journal des parties ; aucun signal envoyé à KG_Rewards (paliers non déclenchés par ce crédit). Détail : `historique-conversations/2026-10-04-parkour-bareme.md`.

**Ajout (04/10/2026, 22:16) : KG_Parkour 1.2.0 déployé sur Kal-Games, non testé, actif après redémarrage (l'humain)** (demande de LeKiwi06 : nouveau barème du Parcours). Points de base selon la position du checkpoint dans la map (1, 1, 3, 3, 3, 5, 5, 7, 7, 7, 10 ; réglable par point de contrôle), multiplicateurs additifs (1er à valider x1,5, sans chute x1,5, temps x1,25 à x1,75 avec un seul temps par point de contrôle), +25 au 1er arrivé, rien « en 1er » en solo ; l'ancien point par checkpoint et le podium 3 / 2 / 1 sont retirés ; points décimaux crédités à chaque section. À faire après le redémarrage : saisir le « Temps du bonus » de chaque point de contrôle (les temps par section sont dans le journal de KG_ScoreBoards, événement « points », `sectionMillis`). À tester : points de chaque checkpoint, bonus du 1er à 2 joueurs et absent en solo, sans chute, temps, +25, résultats avec les points, crédit au classement (non-opérateur). Réservation libérée.

**Ajout (04/10/2026, 22:01) : KS_Economy 1.3.0 et KS_LootPotions 1.1.3 déployés sur Event, non testés.** KS_Economy : le barème des prix (1 929 objets, `rachats.csv`, produit par un script hors dépôt) est dans le plugin ; « Rachats de la semaine » (`/rachat`, bouton du menu Économie) : 2 objets tirés par gamme de prix chaque lundi, par familles, jamais issus d'une dimension fermée dans KS_Dimensions ; lots de 1 / 10 / 64 / 320 émeraudes ; quota par objet et par jour au-dessus de 25 000 de cagnotte. À tester : le tirage au démarrage (console), vendre un lot et « Tout vendre », un objet enchanté ou abîmé refusé, le quota avec une cagnotte au-dessus de 25 000 (visible puis masquée), une tête et un élixir. Pour refaire un tirage : supprimer `plugins/KS_Economy/rachats.yml` serveur éteint. À décider par LeKiwi06 : prix du Cœur de Spawner, œuf de dragon et Cœur de Spawner dans le tirage ou non. KS_LootPotions : casser un minerai d'émeraude de deepslate (potion de chance à chaque fois, aucune avec Toucher de soie).

**Ajout (04/10/2026, 06:52) : 3 plugins déployés sur Event, non testés** (à garder ensemble) : KS_Crafts 1.10.0, KS_LootBlocs 1.3.0, KS_LootPotions 1.1.2. Décisions de LeKiwi06 : le brassage au bloc de verrue était « une erreur » (retiré) ; la verrue du Nether redevient cultivable mais ne se brasse plus ; la boîte de shulker se fabrique avec un coffre de l'Ender. **Ces choix reviennent sur des demandes de Maxster33 (25/09 : verrue sans drop, alambic au bloc de verrue).** À tester : cultiver de la verrue (drop, potion à 5 % seulement sur une verrue mûre) ; alambic (verrue refusée au clic, au glisser, par entonnoir ; bloc de verrue sans effet ; redstone et poudre à canon toujours possibles sur une potion trouvée) ; boîte de shulker (ancienne recette refusée, nouvelle avec coffre de l'Ender, livre de recettes). Non fait, à décider : craft « 9 verrues → bloc de verrue » toujours retiré.

**Ajout (04/10/2026, 06:05) : KS_LootCoffres 1.1.0 déployé sur Event, non testé** (jambières de la cité antique niveau 30 à 50 : objectif de LeKiwi06, une pièce en diamant avec Raccommodage en 10 à 20 coffres ou estomacs ; les trois autres pièces y étaient déjà). Redémarrage d'Event nécessaire ; seuls les coffres pas encore ouverts sont concernés. **Barème des prix des items (en cours, hors dépôt)** : classeurs dans `sortie/` sur le PC de LeKiwi06 (prix en émeraudes : matières de base, puis somme des ingrédients ; 1 émeraude par minute de temps). Signalé à LeKiwi06, non codé : la verrue du Nether à rendre de nouveau cultivable et à bloquer à l'alambic ; « carapace de shulker seulement si un joueur tue » et « éclat d'écho sur les chauves-souris » ne sont dans aucun plugin.

**Ajout (03/10/2026, 22:47) : déployés, non testés, actifs après redémarrage de chaque serveur (l'humain)** : KLM_Chat 1.0.0 et
KLM_Menu 2.9.0 sur les 6 serveurs Paper ; KLM_Hub 1.0.0 et la ligne de vision nocturne (config de KLM_Portal) sur le lobby.
Rien sur le proxy. Réservations libérées. Sur le lobby, sans redémarrage, `/klmportal reload` suffit pour la vision nocturne.

**Ajout (03/10/2026, 21:27) : tchat global, barre de boss du lobby, recherche de joueurs.**
KLM_Chat 1.0.0 (nouveau : `/global send <message> | true | false`, préfixe du serveur dans la couleur de son portail, contenu
gris clair, séparations `----- GLOBAL -----` / `----- <serveur> -----`), KLM_Hub 1.0.0 (nouveau, lobby : barre violette,
titre vert pâle « KaLium SMC | » + lien défilant Discord / Twitch avec arrêt ; QR codes 30 s en main secondaire, la barre
se vide pendant ce temps), KLM_Menu 2.9.0 (bouton « Recherche de joueurs » de la boussole sous 4 joueurs). Détail :
chaque `JOURNAL.md` et `historique-conversations/2026-10-03-tchat-global-lobby.md`.
Ancienne barre faite avec `/bossbar` sur le lobby : à retirer en jeu si elle existe encore (`/bossbar list`, `/bossbar remove <id>`).
À tester : barre du lobby (couleurs, défilement, arrêt sur chaque lien, Bedrock) ; boussole avec moins de 4 joueurs puis
4 ou plus ; les deux QR codes (scan au téléphone, 30 s, barre qui se vide, impossible à jeter / déplacer, main gauche
déjà prise, déconnexion pendant les 30 s) ; `/global send` entre deux serveurs (préfixe, couleurs, séparations dans les
deux sens), `/global false` puis `true`, joueur Bedrock ; vision nocturne sans clignotement en Java et en Bedrock.
Signalé à LeKiwi06 : pas d'anti-spam ni de lien avec les joueurs muets pour `/global` ; choix `true | false` par serveur.

**Ajout (03/10/2026, 19:06) : KS_Crafts 1.9.0 déployé sur Event, non testé** (toutes ses recettes dans le livre de recettes, débloquées dès qu'on obtient un ingrédient).

**Ajout (03/10/2026, 18:42) : 11 plugins déployés sur Event, non testés** : KS_AntiCheat 1.1.1 (minage par
minerai et par catégorie, minerais posés ignorés ; **correctif de duplication** invsee / ecsee signalé pendant les tests
de Maxster33), KS_EC_Extension 1.1.0, KLM_Menu 2.8.0 (Modération aussi dans la navigation de la boussole),
KS_LootPotions 1.1.1, KS_Economy 1.2.0, KS_FairPlay 1.0.3, KS_KaliumGive 1.8.0, KS_Decapitator 1.1.0 + KS_Crafts 1.8.0
(tête de wither squelette sale / endommagée / désactivée et ses 5 crafts de réparation, bloc de charbon de bois),
KS_Tableau 1.0.0 (/tableau), KS_Jetons 1.0.0 (/jetons ; **prix à 0 = rien en vente**, à fixer). KLM_Menu par
serveur : Event 2.8.0, Kal-Games / Kanvas 2.6.0, autres 2.5.0. Redémarrage d'Event nécessaire.

**Ajout (03/10/2026, 16:41) : revue des plugins d'Event (Kixster) et suites validées par LeKiwi06.**
- Vérifié : aucune erreur de nos plugins dans les 10 démarrages du 03/10 ; les 26 jars maison en service = `jars-deployes/`.
- Compilés, **en attente du déploiement groupé** (voir « Requis parfois » de TRAVAIL_EN_COURS) : KS_AntiCheat 1.0.2
  (écarts Simulation sous 0,05 ignorés, historique des alertes, têtes rares seulement), KS_LootPotions 1.1.1 (pas de
  potion sur les mobs de spawner), KS_Economy 1.2.0 (score enregistré à chaque transaction, boutique à un seul
  acheteur, ventes : notifications, stats, classement ; favoris ; recherche avec filtres), KS_FairPlay 1.0.3,
  KS_Tableau 1.0.0 (nouveau, /tableau).
- Retirés d'Event : ConditionalEvents et PyxelRegions (accord de LeKiwi06).
- Gardé tel quel (LeKiwi06) : le Bedrock Breaker casse aussi la bedrock du fond et du plafond (voulu pour le build).
- Régions : `ile_du_dragon` (End) en place ; **pas de région `spawn`** dans l'overworld.
- LuckPerms : groupe `admin` créé par LeKiwi06 (pas encore de modérateurs).
- **Archives en trop sur Event** (règle des 2 derniers `_removed-…` par plugin ; suppression par l'humain) : klm_menu
  2.0.0, 2.4.0, 2.4.1 ; ks_claim 1.0.0 ; ks_crafts 1.0.0 à 1.4.0 (5) ; ks_economy 1.0.0, 1.0.1, 1.0.2, 1.0.3, 1.1.0,
  1.1.1, 1.1.2 ; ks_enclume 1.0.0, 1.1.0, 1.1.1, 1.1.2 ; ks_estomacgardien 1.0.0 ; ks_fioleexp 1.0.0, 1.1.0, 1.2.0 ;
  ks_kaliumgive 1.0.0 à 1.4.0 (5) ; ks_lootentites 1.0.0, 1.1.0, 1.2.0 (32 dossiers).
- À venir (questions posées à LeKiwi06) : téléportations (/home bed, /spawn, jetons de TP et de localisation), jetons
  de claim, jetons de la mort, coffres de mort. Plus tard : bot Discord (alertes), défi qui génère la monnaie.

**Ajout (03/10/2026, 07:46) : KS_AntiCheat 1.0.1 déployé sur Event, non testé** (invsee / ecsee d'un joueur hors ligne sans attendre sa connexion : lecture de `world/players/data/<uuid>.dat` ; changements toujours appliqués à la reconnexion).

**Ajout (03/10/2026, 06:45) : catégorie 6 « Anti-triche » déployée, non testée.** Event : KS_AntiCheat 1.0.0
(alertes, rubrique Modération : Anti-triche, Invsee, EcSee, Minage ; suspensions ; invsee / ecsee en ligne et hors
ligne avec journal ; morts d'entités ; GrimAC, x-ray, macros, AFK ; revente suspecte ; « Bannir de KaLium » ; règle
tntExplosionDropDecay), KS_Economy 1.1.5, KS_RewardsGUI 1.0.1, CoreProtect CE 24.1, CauldronInteract 1.4.0,
Woodcutter 7.2 (datapack), Paper (`allow-piston-duplication: true`, `lava-obscures: true`). Proxy : KaliumRelay
1.4.0, LibertyBans 1.1.4. Duplication par identifiant : reportée (LeKiwi06). Cahier : `KS_AntiCheat/CAHIER_DES_CHARGES.md`.
**À faire par l'humain** : `relay-token` dans `plugins/KS_AntiCheat/config.yml` (Event) ; redémarrer le proxy puis
Event ; donner `ksanticheat.staff` aux modérateurs (opérateurs par défaut). À tester : alertes, invsee / ecsee (en
ligne, hors ligne), suspension et levée, Bannir de KaLium, Minage, AFK (1 h), CoreProtect (`/co i`), Woodcutter,
CauldronInteract, duplication de TNT.

**Ajout (03/10/2026) : tests de LeKiwi06** : Xaero (fair-play), limite des coffres de KS_FairPlay 1.0.2 (« Exploration journalière : n / 10 ») et barre de l'Élixir de Fortune **validés**. Reste à tester : JourneyMap, spawners naturels, blocs suspects, gardien ancien, entonnoirs, anti-xray.

**Ajout (03/10/2026, 06:13)** : JourneyMap réglé le 03/10/2026 à 06:13 (`/journeymap/server/6.0/journeymap.server.global.config`) : `caveMapping`, `radarEnabled`, `worldPlayerRadar`, `seeUndergroundPlayers` à NONE, radars de joueurs / villageois / animaux / monstres coupés ; surface, relief, biomes, points de repère gardés. `plugins/journeymap/journeymap-server.json` : UUID de l'équipe JourneyMap (codé en dur dans le plugin) retiré des admins, opérateurs gardés. Originaux dans `/_removed-config-2026-10-03/journeymap/`. **Vérifié après le redémarrage d'Event (03/10/2026, 06:14) : réglages en place, JourneyMap et KS_FairPlay 1.0.2 chargés sans erreur.**

**Ajout (03/10/2026, 06:07) : KS_FairPlay 1.0.2 déployé sur Event, non testé** (compteur « Exploration journalière : n / 10 », message de limite sans détails, barre « Élixir de Fortune : m:ss »).

**Ajout (03/10/2026, 05:45) : KS_FairPlay 1.0.1 déployé sur Event, non testé** (« je ne vois pas le compteur de coffres » : comptage au tick suivant si le butin est sorti, message en créatif).

**Ajout (03/10/2026, 05:18) : catégorie 5 « FairPlay » déployée sur Event, non testée.** KS_FairPlay 1.0.0 (code
Xaero à la connexion ; limite globale de 10 par jour : contenants au butin non généré, spawners naturels, blocs
suspects, gardiens anciens tués ; protections ; Chance III libre ; compteur dans la barre d'action et le tchat),
plugins serveur JourneyMap 26.2-6.0.9 et Legacy Freecam 2.0.0, configuration Paper (anti-xray, suivi des entités).
Cahier : `KS_FairPlay/CAHIER_DES_CHARGES.md`. **À faire** : redémarrer Event ; ensuite Claude règle JourneyMap
(radar et grottes coupés, sa configuration n'existe qu'après le premier démarrage) ; règlement « Legacy Freecam
seulement » (humain). À tester (speedrun) : limite et message à 10, Chance III, spawner posé (libre) / naturel,
bloc suspect, gardien ancien, entonnoir sous un coffre de structure, anti-xray (minerais cachés), Xaero.

**Ajout (03/10/2026, 03:22) : KLM_Menu 2.7.0 et KS_Economy 1.1.4 déployés sur Event, non testés.** Rubrique
« Modération » dans `/menu` (staff : outils déclarés par les plugins avec leur permission) ; premier outil :
signalements des magasins. Cahier 6 mis à jour : l'interface staff de KS_AntiCheat (invsee, ecsee, proportions de
minerais, indices de suspicion) ira dans cette rubrique. **KLM_Menu par serveur** : Event 2.7.0 ; Kal-Games et
Kanvas 2.6.0 ; lobby, Serveur Jeux, Kixster 2.5.0 (les trois versions sont dans `jars-deployes/` ; 2.5.0 remis,
retiré par erreur le 03/10 à 01:05).

**Ajout (03/10/2026, 02:35) : KS_Economy 1.1.3 et KS_Claim 1.1.2 déployés sur Event, non testés.** KS_Economy :
catalogue sans son propre magasin, pas d'achat dans sa propre boutique, « Voir l'objet » (coffre en lecture seule),
« Signaler la boutique / le magasin » (raisons + Autre ; staff : `/magasin signalements`, permission
`kseconomy.staff`, opérateurs par défaut : **à donner aux modérateurs par LuckPerms**). KS_Claim : bannissement
retiré (expulser gardé), bannissements existants levés au démarrage. Actifs après redémarrage d'Event.

**Ajout (03/10/2026, 01:49) : KS_Economy 1.1.2 déployé sur Event, non testé** (retours de LeKiwi06 sur les
boutiques, voir son JOURNAL et le cahier « Retours de LeKiwi06 (03/10/2026) ») : recherche d'objet corrigée (noms
français absents du jar), noms français sur les panneaux, panneau en 4 lignes avec l'état (stock, rupture, coffre
plein, fermée), nom de boutique et « Renommer », « Fermer temporairement », suppression depuis le menu : panneau
retiré et rendu, place d'une boutique supprimée prise 3 h, coffres en cuivre. Actif après redémarrage d'Event.

**Ajout (03/10/2026, 01:05) : catégorie 4 « Récompenses » déployée, et correctif des claims ; non testés.**
Proxy : KaliumRelay 1.3.0. Event : KS_RewardsGUI 1.0.0 (nouveau, `/rewards`), KS_KaliumGive 1.7.0, KLM_Menu 2.6.0,
KS_Claim 1.1.1, KS_Economy 1.1.1, KS_BiomeChanger 1.1.1. Kal-Games : KG_ScoreBoards 1.8.0, KG_Rewards 1.0.0 (nouveau),
KLM_Menu 2.6.0. Kanvas : KV_Plots 1.5.0, KV_Rewards 1.0.0 (nouveau), KLM_Menu 2.6.0. Cahier :
`KG_Rewards/CAHIER_DES_CHARGES.md` (aussi dans KS_RewardsGUI et KV_Rewards).
- Correctif signalé par LeKiwi06 (`/ksclaim`, `/ksclaims`, `/magasin` sans réponse) : SimpleClaimSystem 1.13.1 ne crée
  pas son API lui-même (« API not initialized ») ; KS_Claim, KS_Economy et KS_BiomeChanger l'initialisent.
- **À faire par l'humain** : remplir `relay-token` (même valeur que le proxy) dans `plugins/KS_RewardsGUI/config.yml`
  (Event), `plugins/KG_Rewards/config.yml` (Kal-Games), `plugins/KV_Rewards/config.yml` (Kanvas) ; puis redémarrer
  le proxy, Event, Kal-Games et Kanvas. Sans jeton, les récompenses attendent dans `envois.yml` (rien n'est perdu).
- À tester (speedrun) : claims et magasins ; `/kgrewards`, `/kvrewards` (progression, prestige, `admin` : tables de
  butin à remplir) ; une récompense arrive sur Event (`/rewards`, bouton Récompenses) ; classement de la semaine.
- Signalé, non touché : ConditionalEvents sur Event ne lit pas son `config.yml` (erreur de format vers la ligne 10).

**Ajout (03/10/2026, 00:02) : magasins déployés sur Event, non testés** : KS_Economy 1.1.0 (nos propres boutiques,
Tradeshop abandonné ; région WorldGuard `zone_shop` où l'on claime et construit librement ; `/magasin create` 45 000 points
ou 5 blocs tier 3, `/magasin agrandir` 10 000 points +1 boutique ; boutique = panneau sur coffre / tonneau / shulker d'un
claim du magasin ; prix en objet ou en points en attente) + KS_Claim 1.1.0. Cahier mis à jour :
`KS_Economy/CAHIER_DES_CHARGES.md`. À tester (speedrun) : création du magasin, d'une boutique (nom écrit et objet de
l'inventaire, prix en points et en objet), achat sur place et à distance, points en attente, gestion à distance
(verrou), protections (casse, explosion, entonnoir), refus de supprimer un claim avec boutiques.

*Tests d'Event regroupés plus tard (LeKiwi06 : un « speedrun » du serveur pour tout tester) : catégorie 2, 1re partie, et
catégorie 3 restent non testées d'ici là.*

**Ajout (22:26) : catégorie 3 « Claims » déployée sur Event, non testée** : SimpleClaimSystem 1.13.1 (téléchargé avec
l'accord de LeKiwi06 ; configuration et traduction posées avant le premier démarrage), KS_Claim 1.0.0, KS_BiomeChanger
1.1.0. Cahier : `KS_Claim/CAHIER_DES_CHARGES.md` ; réglages de SCS : `KS_Claim/scs/README.md`. **Après le redémarrage
d'Event (l'humain)** : 4 commandes LuckPerms (`lp group default permission set scs.command.claim false`, `.claim.*`,
`.claims`, `.unclaim`) et région de l'île du dragon dans l'End (`//pos1 -200,0,-200`, `//pos2 199,255,199`,
`/rg define ile_du_dragon`, `/rg flag ile_du_dragon scs-claim deny`).
À tester : console (SimpleClaimSystem et KS_Claim chargés, langue fr_FR) ; `/ksclaim` (gratuit jusqu'au 10e, puis
237...) ; bouton Claims du menu d'Event ; membres, bannis, réglages, groupes ; vente et achat entre 2 joueurs ;
suppression et remboursement ; un visiteur ne peut pas ouvrir un coffre mais peut utiliser une table de craft ;
`/claim` refusé aux joueurs ; pas de claim sur l'île du dragon ; Changeur de Biome refusé dans le claim d'un autre ;
barre de boss en français ; Bedrock.

*(Session du 02/10/2026, de 17 h 30 à 17 h 55. Fin de session : réservations libérées.)*

**Textes plus courts dans les menus** (LeKiwi06 : dans les réglages des jeux, les jauges élargies par KLM_Menu 2.5.0
restaient trop longues ; Minecraft n'accepte du texte qu'en haut d'une fenêtre, d'où « écrit des textes plus courts »).
Déployé le 02/10/2026 à 17:49, **non testé**, actif après redémarrage de Kal-Games, du lobby et de Kanvas (l'humain) :
KalGames 1.22.2 (réglages : nom seul dans la jauge, explications en haut du menu), KG_BoatRace 1.4.2, KG_Parkour 1.1.1,
KG_PvpKit 1.0.1 (20 noms de réglages raccourcis, détails dans l'explication ; listes de création du PvP Kit), KG_Bingo
1.6.1 (création d'une partie), KLM_Portal 1.3.2 (lobby, Kanvas : point de chute). Réservation de 6 plugins avec
l'accord de Maxster33 (transmis par LeKiwi06).
À tester : réglages de chaque jeu (Paramètres > jeu > Réglages) et d'un point, création d'un Bingo et d'un PvP Kit,
points de chute (`/klmportal`) : jauges et listes de largeur normale, rien ne défile, explications en haut.

### 2026-10-01 — LeKiwi06

**Ajout (23:42) : aucun texte qui défile dans les menus** (demande de LeKiwi06 : « il ne faut jamais que ça arrive,
c'est illisible », sur tous les plugins). KLM_Menu 2.5.0 (outil `Lisible` : boutons et champs élargis à leur texte,
pour tous les plugins qui utilisent `Gui`) sur les 6 serveurs Paper + KalGames 1.22.1, KG_BingoGame 0.8.4, KS_Economy
1.0.3. **Actifs après redémarrage de chaque serveur (l'humain). Non testé.** À tester : menus de chaque serveur
(boussole, Informations, Classements, Paramètres, création d'un Bingo, menus de Kanvas, Économie) : plus aucun texte
qui défile ; console : message « Texte trop long » éventuel à signaler. Supprimables par l'humain (règle des 3 versions) :
`_removed-klm_menu-2.3.0/` et plus anciens sur chaque serveur.

*(Suite de la session du 30/09/2026, jusqu'à 1 h. Fin de session : réservations libérées.)*

- **KS_LootEntites 1.4.0** (Warden : fioles à 15 %, catalyseur remplacé par un bloc du Deep Dark, émeraude de
  deepslate à 0,01 %) : déployé le 30/09 à 22:44, testé et confirmé par LeKiwi06. Catégorie 1 testée et confirmée.
- **Catégorie 2 « Économie », 1re partie**, déployée sur Event le 01/10/2026 à 00:49, **non testée**, active après
  redémarrage d'Event (l'humain) : KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 + VaultUnlocked 2.20.2 (téléchargé
  avec l'accord de LeKiwi06, ajouté à `telecharger-outils.sh`). Cahier : `KS_Economy/CAHIER_DES_CHARGES.md`.
  Magasins (Tradeshop) : après KS_Claim (catégorie 3).

**Tests à faire (catégorie 2, 1re partie)** :
1. Console : KS_Menu, KS_Economy (« Déclaré comme économie Vault »), KS_Elixir (11 recettes, plus d'avertissement pour
   Fortune), Vault chargés sans erreur.
2. `/menu on` : étoile du Nether en case 5 ; clic droit → menu d'Event avec le bouton « Économie » ; impossible de la
   déplacer ou de la jeter ; `/menu off` la retire ; `/event` ouvre le menu.
3. Menu Économie (`/economie`) : « Tout déposer » avec émeraudes + blocs d'émeraude ; « Déposer des objets » : poser des
   émeraudes et un autre objet, fermer → émeraudes créditées, objet rendu.
4. Liste Tab : ton solde à côté de ton pseudo, vu par un 2e joueur ; « Masquer » → pseudo seul ; « Révéler » → solde.
5. Retrait : émeraudes, bloc, bloc compressé tier 1 et tier 3 ; refus avec solde insuffisant et inventaire plein.
6. Bloc compressé : aspect enchanté, nom, valeur ; impossible à poser, à décrafter (établi et autocrafteur) ; se range
   dans un coffre ; se redépose (tout déposer).
7. Élixir de Fortune : 8 potions de chance + 1 bloc tier 3 → Chance III 10 min.
8. `/echange <joueur>` (2 joueurs à moins de 5 blocs) : accepter par le clic ; objets et montant de chaque côté, aperçu
   chez l'autre ; validation retirée quand on modifie ; échange des deux côtés ; s'éloigner / fermer le panneau /
   se déconnecter → annulé et objets rendus ; montant supérieur au solde refusé ; demande à plus de 5 blocs refusée.
9. Le lendemain après minuit : solde masqué diminué de 1 %.
10. Bedrock : étoile, menu Économie (fenêtres), panneau d'échange et saisie du montant.

### 2026-09-30 — LeKiwi06

*(Session du 30/09/2026, de 17 h 35 à 18 h 15. Fin de session : réservations libérées.)*

**Catégorie 1 « Contenu survie » codée et déployée sur Event le 30/09/2026 à 18:07, testée et confirmée par LeKiwi06 le 30/09/2026** (cahiers :
`KS_Decapitator/CAHIER_DES_CHARGES.md`, `KS_Elixir/CAHIER_DES_CHARGES.md` ; détail dans chaque `JOURNAL.md`) :
KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0, KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0, KS_LootEntites 1.3.0.
**Actifs après redémarrage d'Event (l'humain).** Textures des têtes : MoreMobHeads (MIT) ; manquent 16 shulkers
colorés, cube de soufre, bébé noyé (à fournir : une ligne dans `KS_Decapitator/src/main/resources/tetes.txt`).
À savoir : la session WinSCP enregistrée d'Event s'appelle `Event [new]` sur le PC de LeKiwi06 (WinSCP.com dans
`%LOCALAPPDATA%\Programs\WinSCP\`, sous Git Bash : `MSYS_NO_PATHCONV=1` et chemin Windows pour `/script=`).

**Tests à faire** :
1. Console au démarrage : « 332 têtes chargées », « 10 recettes d'élixirs ajoutées », avertissement attendu pour la
   recette de Fortune (KS_Economy absent), aucune erreur des 6 plugins.
2. `/tetes` : parcourir les 8 pages, vérifier textures et noms (surtout les bébés) ; noter les têtes fausses.
3. Pose d'une tête puis casse (main, pioche, TNT, piston) : même nom, même texture, s'empile avec une tête neuve.
4. Chute : tuer des mobs (1 %, ex. `/kaliumgive` ne sert pas ici) ; aucune tête pour un mob d'un spawner.
5. Élixirs : `/kaliumgive <pseudo> elixir_titan 1` (et les autres) : nom, couleur, aspect enchanté, effet et durée
   (Rebond II, Fortune III 10 min, Plume 30 min, Phénix 10 min) ; craft en anneau avec de vraies potions allongées ;
   Super Gâteau avec soin I puis II ; Fantôme avec une fiole de 10 niveaux ; Vent avec deux couleurs de harnais.
6. Livre de recettes : ramasser un éclat d'écho → recette de l'élixir de chauve-souris débloquée.
7. Alambic : impossible d'y mettre un élixir (clic, Maj+clic, touche 1-9, entonnoir).
8. Spawners : recette avec une tête de mouton de couleur, une tête de bébé vache, une tête vanilla de zombie et une
   tête custom de zombie.
9. `/kaliumgive` : complétion `tete_…`, `elixir_…` ; `tete_mouton` n'existe plus (`tete_mouton_blanc`).
10. Warden tué : fiole de KS_FioleExp (« Fiole d'expérience (niveau N) » avec les points en description).
11. Bedrock : têtes en main et posées (texture ou Steve ?).

### 2026-09-29 — LeKiwi06

*(Session du 29/09/2026, de 0 h 45 à 17 h 50, mise en pause par LeKiwi06. Fin de session : réservations libérées.)*

**Bot Discord : KLM_DiscordBot (nouveau), cahier des charges `KLM_DiscordBot/CAHIER_DES_CHARGES.md`.** Bot externe en
Node.js qui lit les données de kal-games par une API de KG_ScoreBoards (seul interlocuteur du bot). Décisions de
LeKiwi06 : commandes slash, salon des classements rafraîchi, annonces, récaps (jour et heure réglables) ; profil joueur,
classements, accomplissements, activité ; liaison des comptes par code (`/lier`) ; résultats de toutes les parties dans
un salon avec un fil par jeu ; mention Discord seulement dans les classements mensuels ; `/comparer` avec écarts en % ;
séries de victoires : parties non classées ignorées, PvP Kit en manches ET en matchs. 6 étapes.
- **Étape 1 faite, non testée** :
  - **KG_ScoreBoards 1.7.0** : API HTTP en lecture (`/api/v1/status`, `rankings`, `archives`, `players`, `events` avec
    curseur), jeton obligatoire. **Déployé sur Kal-Games le 29/09/2026 à 2 h 36, serveur allumé** : actif au prochain
    redémarrage. Clés `api.port: 45347` (port attribué par Minestrator) et `api.token` **à ajouter à la main** par
    LeKiwi06 dans `plugins/KG_ScoreBoards/config.yml` (jeton jamais dans le dépôt).
  - **KLM_DiscordBot 0.1.0** : `/classement` (top 10 en image : général, du mois, mois archivé ; points ou meilleur
    tour). Lancement : `npm install` puis `npm start` dans `KLM_DiscordBot/` (mode d'emploi : `README.md`).
- Bot Discord créé par LeKiwi06 dans le portail développeur et invité sur le serveur Discord (7 permissions, lien
  d'invitation avec `permissions=309237763072`). Node.js 24 installé sur le PC de LeKiwi06.
- `KLM_DiscordBot/.env` créé vide sur le PC de LeKiwi06 (ignoré par git) : à remplir (`DISCORD_TOKEN`,
  `DISCORD_GUILD_ID`, `API_TOKEN`).
- À savoir : en **mode automatique**, Claude Code bloque les envois sur les serveurs (WinSCP), même avec l'accord écrit de
  l'humain ; repasser dans le mode où l'on approuve chaque action avant un déploiement. Le serveur SFTP de Minestrator ne
  gère pas la copie distante (`cp` de WinSCP) : passer par un téléchargement puis un envoi.

**Reste à faire (reprise)** : ajouter les clés `api` au `config.yml` de KG_ScoreBoards et redémarrer kal-games (console :
« API du bot Discord à l'écoute sur le port 45347 ») ; remplir `.env` ; lancer le bot et tester `/classement` ; puis
étape 2 (`/stats`, `/activite`, graphiques d'évolution). Supprimables par l'humain sur kal-games :
`_removed-kg_scoreboards-1.3.0/` et `1.4.0/`.
