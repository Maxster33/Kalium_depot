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
| Lobby | `lobby` | `lobby@7002.mystrator.com` | 91.197.6.152:28618 | KLM_Menu, KLM_Portal, KLM_Hub, KLM_Chat, KLM_Contacts |
| **Hub mini-jeux (Kal-Games)** | `kal-games` | **depuis le 22/10/2026 : sur Kanvas** (`Kal-Test-Dev@5038.mystrator.com`, session WinSCP « Kanvas[new] »), espace étanche géré par KLM_Espaces ; l'ancienne machine `KalGames@7001` est arrêtée, contenu intact | 91.197.6.215:21621 (avant le 22/10/2026 : 91.197.6.24:22142) | KalGames, KG_PvpKit, KG_BoatRace, KG_Parkour, KG_BuildBattle, KG_HideAndSeek, KG_PiliersFortune, KG_Menu, KG_Bingo, KG_ScoreBoards, KLM_Menu, KLM_Chat, KLM_Contacts |
| **Bingo + jeux gourmands** (Manhunt...) | `serveur-jeux` | `Event@7021.mystrator.com` (session WinSCP « Event [new] » ; sur la machine d'Event depuis le 21/10/2026, l'ancienne machine `serveurjeux@7015` est arrêtée, contenu intact) | 51.254.174.133:21088 (avant le 21/10/2026 : 91.197.6.65:22470) | KG_BingoGame, KG_BingoObservateur, KLM_Menu (boussole désactivée), KLM_Chat, KLM_Contacts |
| Kixster SMP (rendu au SMP le 24/09/2026) | `kixster` | `kixster@7003.mystrator.com` | 91.197.6.212:29599 | KLM_Menu (boussole désactivée, menu par `/menu`), KLM_Chat, KLM_Contacts |
| ~~Event : survie classique~~ (25/09/2026 → **retiré le 21/10/2026** : sa machine accueille maintenant Serveur Jeux ; ancien contenu dans `/_removed-event-avant-migration-2026-10-21/`) | ~~`event`~~ (retiré de `velocity.toml` le 21/10/2026) | `Event@7021.mystrator.com` | — | KLM_Menu (boussole désactivée, `/menu`), KLM_Chat, KLM_Contacts, KS_Dimensions, KS_Enclume, KS_Villageois, KS_Crafts, KS_BedrockBreaker, KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_LootBlocs, KS_LootEntites, KS_LootPeche, KS_LootPotions, KS_EstomacGardien, KS_LootCoffres, KS_KaliumGive, KS_EC_Extension, KS_FioleExp, KS_Decapitator, KS_Elixir, KS_Menu, KS_Economy, KS_Claim, KS_JukeboxBoat, SimpleClaimSystem (cahiers des charges : `KS_Event/`, `KS_Decapitator/`, `KS_Elixir/`, `KS_Economy/`, `KS_Claim/`) |
| Serveur de plots en créatif (Kanvas) + **Kal-Games** (depuis le 22/10/2026 : deux espaces étanches, KLM_Espaces 1.0.0 ; hub dans `/Kanvas/dimensions/minecraft/kal-games/`, parties dans `/Kanvas/dimensions/kalgames/instances/`) | `Kanvas` et `kal-games` (même adresse) (ex `kal-test-dev`, renommé par LeKiwi06 dans `velocity.toml` le 24-25/09/2026 ; destination de la boussole du lobby renommée `Kanvas` le 26/09/2026) | `Kal-Test-Dev@5038.mystrator.com` | 91.197.6.215:21621 | KV_Plots, KV_Menu, KLM_Menu, KLM_Chat, KLM_Contacts (cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`) |

Machines Minestrator : **machine 1** = proxy, lobby, Event (ex kal-games), Kixster ; **machine 2** = Kal-Test-Dev (Kanvas, et Kal-Games depuis le 22/10/2026) ;
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

**Ports voicechat** (UDP, attribués par Minestrator) : proxy 44301 (inutilisé), lobby 43841, Kixster 40046,
Serveur Jeux 40002 (machine d'Event depuis le 21/10/2026, `voice_host=51.254.174.133:40002`), Kanvas 45595 (ex Kal-Test-Dev), ancienne machine de Serveur Jeux 43131, Kal-Games 43374 (ex KalGames2).
**Chat vocal derrière le proxy** (décision de LeKiwi06, 09/10/2026) : pas de plugin voicechat sur le proxy ; dans
`plugins/voicechat/voicechat-server.properties` de chaque serveur Paper, `voice_host=<IP de jeu du serveur>:<son port
voicechat>` (sans lui, le client vise l'IP du proxy avec le port du serveur et ne se connecte à rien). Si l'IP d'un
serveur change, son `voice_host` est à changer aussi.

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

Relevé par `ls /plugins/` sur chaque serveur le 21/11/2026 (Claude de LeKiwi06). Copie de chaque jar dans
`jars-deployes/`. **Une ligne par serveur, versions actuelles seulement** : à chaque déploiement, remplacer la version
dans la ligne (ne rien empiler) ; le détail (date, raison, test en jeu) va dans le `JOURNAL.md` du plugin.
Historique complet des déploiements jusqu'au 21/11/2026 : `archive_versions.md` (à chercher, pas à lire en entier).

| Serveur (session WinSCP) | Plugins KaLium | Autres plugins |
|---|---|---|
| **Lobby** (`Lobby [new]`) | KLM_Chat 1.0.0, KLM_CollisionsBedrock 1.0.0, KLM_Contacts 1.5.2, KLM_Hub 1.1.0, KLM_Menu 2.11.0, KLM_Portal 1.3.2, KS_Blocs 0.3.0 | BedrockSkinRestorer[1.0.5], LuckPerms-Bukkit-5.5.71, VelocityCommandForward-Paper-1.1.0, ViaBackwards-5.12.1, ViaVersion-5.12.1, floodgate-spigot, voicechat-bukkit-2.6.24, worldedit-bukkit-7.4.6-beta-02, worldguard-bukkit-7.0.19 |
| **Kanvas** + Kal-Games (`Kanvas[new]`) | KG_Bingo 1.11.0, KG_BoatRace 1.6.2, KG_BuildBattle 0.7.0, KG_HideAndSeek 0.3.0, KG_Menu 1.2.0, KG_Parkour 1.8.2, KG_PiliersFortune 0.2.0, KG_PvpKit 1.2.0, KG_Rewards 1.5.0, KG_ScoreBoards 1.9.0, KLM_Chat 1.1.0, KLM_CollisionsBedrock 1.0.0, KLM_Contacts 1.5.2, KLM_Espaces 1.0.0, KLM_Menu 2.12.0, KLM_Portal 1.3.2, KS_Blocs 0.3.0, KV_BuildBattle 0.5.0, KV_Menu 1.4.0, KV_Plots 1.8.2, KV_Rewards 1.3.0, KalGames 1.29.0 | BedrockSkinRestorer[1.0.5], FastAsyncWorldEdit-Paper-2.16.0, JEIRecipeFix-0.4.0, LuckPerms-Bukkit-5.5.71, PlaceholderAPI-2.12.3, VelocityCommandForward-Paper-1.1.0, ViaBackwards-5.12.1, ViaVersion-5.12.1, floodgate-spigot, grimac-bukkit-2.3.74-abb95b6, voicechat-bukkit-2.6.24, worldguard-bukkit-7.0.19 |
| **Kixster** (`Kixster[new]`) | KLM_Chat 1.0.0, KLM_CollisionsBedrock 1.0.0, KLM_Contacts 1.5.2, KLM_Menu 2.12.0, KLM_Statistiques 1.2.0, KS_Alcools 1.2.0, KS_AntiCheat 1.6.0, KS_Atouts 1.4.0, KS_BedrockBreaker 1.1.0, KS_BiomeChanger 1.2.0, KS_Blocs 0.3.0, KS_Boussole 1.1.0, KS_CadresBedrock 0.1.0, KS_CasseCroute 1.0.0, KS_Claim 1.5.0, KS_CoffreMort 1.1.0, KS_CoffreRenforce 1.3.1, KS_Contrats 1.0.0, KS_Crafts 1.23.0, KS_Cuisine 1.0.0, KS_Decapitator 1.4.0, KS_Didacticiel 1.5.0, KS_Dimensions 1.1.0, KS_DisquesBundle 1.0.0, KS_EC_Extension 1.2.0, KS_Economy 1.13.8, KS_Effets 1.0.0, KS_Elixir 1.1.0, KS_Encapsuleur 1.0.0, KS_Enclume 1.4.0, KS_EstomacGardien 2.0.0, KS_FairPlay 1.1.1, KS_FioleExp 1.5.0, KS_Fly 1.1.0, KS_ItemSimple 1.1.0, KS_Jetons 2.3.0, KS_JukeboxBoat 1.2.0, KS_KaliumGive 1.14.0, KS_LaisseBateau 1.0.2, KS_LootBlocs 1.3.0, KS_LootCoffres 1.1.0, KS_LootEntites 1.5.0, KS_LootPeche 1.2.0, KS_LootPotions 1.2.1, KS_MaleficesNether 1.0.0, KS_Menu 1.3.0, KS_Meule 1.1.0, KS_MondeTest 1.2.0, KS_Parrainage 1.2.0, KS_RewardsGUI 1.10.0, KS_ShulkerAmelioree 1.0.2, KS_Spawners 1.1.0, KS_SuperTool 1.0.0, KS_Tableau 1.2.0, KS_Teleport 1.5.0, KS_Textures 1.2.4, KS_Trains 1.6.0, KS_VerrouVehicule 1.1.1, KS_Villageois 1.0.0, KS_Votes 1.0.0 | Chunky-Bukkit-1.5.3, CoreProtect-CE-24.1, FairMinimap-1.0.0+1.21.7, LuckPerms-Bukkit-5.5.71, RideOnHead-1.1.3, SimpleClaimSystem-1.13.1, VaultUnlocked-2.20.3, ViaBackwards-5.12.1, ViaVersion-5.12.1, floodgate-spigot, grimac-bukkit-2.3.74-abb95b6, journeymap-paper-26.3-6.0.10, nuvotifier-bukkit-3.3.6, voicechat-bukkit-2.6.24, worldedit-bukkit-7.4.6-beta-01, worldguard-bukkit-7.0.19 |
| **Serveur Jeux** (Bingo, sur la machine d'Event : `Event [new]`) | KG_BingoGame 0.13.0, KG_BingoObservateur 0.1.0, KLM_Chat 1.0.0, KLM_CollisionsBedrock 1.0.0, KLM_Contacts 1.5.2, KLM_Menu 2.11.0, KS_Blocs 0.3.0 | BedrockSkinRestorer[1.0.5], FairMinimap-1.0.0+1.21.7, LuckPerms-Bukkit-5.5.71, RideOnHead-1.1.3, SimpleClaimSystem-1.13.1, VelocityCommandForward-Paper-1.1.0, ViaBackwards-5.12.1, ViaVersion-5.12.1, floodgate-spigot, voicechat-bukkit-2.6.24, worldedit-bukkit-7.4.6-beta-02 |
| **Proxy** (`Proxy velocity [new]`) | KaliumRelay 1.12.0, KaLium-objets 1.13.0 (mcpack dans `Geyser-Velocity/packs/`) | Geyser-Velocity, LibertyBans_Release-1.2.0-M1, VelocityCommandForward-Velocity-1.1.0, floodgate-velocity, nuvotifier-velocity-3.3.6 |

Kal-Games (7001) et l'ancien Serveur Jeux (7015) n'existent plus (session `Serveur Jeux [new]` : accès refusé le
21/11/2026). Bot Discord : KLM_DiscordBot 0.8.1 chez Dishost (`Kal-Site`).

**Déployé le 20/11/2026, non testé en jeu** : KS_SuperTool 1.0.0, KS_Blocs 0.3.0 (tous les serveurs Paper),
KS_AntiCheat 1.6.0, KS_Boussole 1.1.0, KS_ShulkerAmelioree 1.0.2, KaliumRelay 1.12.0, KaLium-objets 1.13.0.
Les autres états « non testé » sont dans les `JOURNAL.md` et `archive_versions.md`.

### Compilé, plus récent que le serveur (non déployé)

Relevé le 21/11/2026 en comparant le `JOURNAL.md` de chaque plugin aux serveurs :

- **Kixster** : KS_Claim 1.6.0 + KS_Contrats 1.1.0 + KS_Courrier 1.0.0 (nouveau, boîte aux lettres), KS_Crafts 1.24.0,
  KS_Economy 1.13.9, KS_Effets 1.1.0, KS_LootBlocs 1.4.0, KS_LootPeche 1.3.0, KS_Textures 1.2.6 ; nouveaux jamais
  déployés : KS_Cultures 1.1.0, KS_Hyperboost 1.1.0, KS_RepeteurWifi 1.0.0, KS_DiagOutils 1.0.0.
- **Kanvas** : KG_ScoreBoards 1.10.0.
- **Lobby, Kixster, Serveur Jeux** : KLM_Chat 1.1.0 (1.0.0 en place) ; **Lobby, Serveur Jeux** : KLM_Menu 2.12.0
  (2.11.0 en place).
- Jamais déployés : KG_Pong 0.1.0 (prototype), KaliumCore 1.4.0 (bibliothèque).
- Pack Java 1.7.0 + geyser-bedrock 1.14.0 (cultures) : construits, non envoyés.

## Chantiers en cours

- **Migration des serveurs (21-23/10/2026, LeKiwi06)** : Kal-Games (7001) et Serveur Jeux (7015) expirent. Serveur Jeux copié sur la machine d'Event (`serveur-jeux` pointe vers 51.254.174.133:21088, `event` retiré) ; Kal-Games fusionné dans Kanvas avec KLM_Espaces (deux noms Velocity, même machine, on passe par le lobby). Maxster33 ne travaille plus sur le projet (21/10/2026) : LeKiwi06 seul.
Architecture visée et charte du réseau (nommage, menus inter-serveurs, sécurité) : `ARCHITECTURE_CIBLE.md`.

- **KG_BoatRace** (cahier des charges : `KG_BoatRace/CAHIER_DES_CHARGES.md`) : étapes 0 (sortie de KalGames), 1
  (km/h, éditeur de checkpoints, seuil 45 s), 2 (classement en direct, écarts, données), 3 (barème, hors-piste) et 7
  (anti-collision) **faites et testées** (1.4.1). Restent : **4** (abandon / reconnexion en 5 min avec position,
  angle et vitesse), **5** (mode Grand Prix mis en avant en public et en privé ; le bonus x1,5 des 40 tours existe
  déjà), **6** (fantôme du meilleur tour et contre-la-montre solo) ; précision des temps au dixième (interpolation
  possible). **Écart Java / Bedrock mesuré le 08/10/2026** (Bedrock 3 à 6 s plus rapide par tour d'environ 40 s, chiffres
  dans le cahier des charges) ; **1.6.0 compilée, non déployée** : bateau calculé par le serveur pour les joueurs
  Bedrock, en essai (réglage désactivé par défaut) ; repli prévu : records séparés et paliers de chrono décalés.
- **KG_Parkour** (cahier des charges : `KG_Parkour/CAHIER_DES_CHARGES.md`) : **étape 0 faite** le 26/09/2026
  (KG_Parkour 1.0.0 + KalGames 1.20.0 : le Parcours sort de KalGames tel quel ; déployés sur Kal-Games, testés et confirmés par LeKiwi06 le 26/09/2026).
  1.1.0 : chrono par checkpoint, anti-collision. **1.2.0 (déployé le 04/10/2026 à 22:16, non testé) : nouveau barème**
  (voir son `JOURNAL.md`). **1.5.0 (27/10/2026, compilée, non déployée, non testée en jeu) : lave qui monte** pour la map de jump vertical
  (point facultatif « Départ de la lave » de l'arène ; 30 s puis 1 bloc toutes les 3 s ; contient la 1.4.0 déployée le 26/10). Restent : cumul crédité en fin de partie, chrono réglable par checkpoint, barème calé sur la course de
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

- **URGENT (relevé le 23/10/2026 dans le journal du proxy, Geyser 2.12.0)** : « WARNING! NetherNet support is NOT enabled. Players will not be able to connect to your server, starting October 27th, when NetherNet is scheduled to release » (https://geysermc.org/blog/preparing-for-nethernet). Sans réglage avant le 27/10/2026, les joueurs Bedrock ne pourront plus entrer sur KaLium. **Réglé le 23/10/2026 à 17:10 (accord de LeKiwi06), actif depuis le redémarrage du proxy du 24/10 à 01:35 (signaling et NetherNet démarrés, sans avertissement), pas encore testé avec un client Bedrock** : `plugins/Geyser-Velocity/config.yml` : `transport: both` (RakNet et NetherNet), `webrtc-port: 41518` (déjà là, port attribué au proxy), `advertise-addresses: [91.197.6.68]` ; signaling `builtin` sur 40312 (TCP) ; ancien fichier dans `plugins/Geyser-Velocity/_removed-geyser-config-avant-nethernet-2026-10-23/`. Ports du proxy (panneau Minestrator, TCP et UDP) : 25370 (Java), 40312 (Bedrock : RakNet UDP + signaling TCP), 41518 (NetherNet UDP), 44270 (NuVotifier en TCP depuis le 03/11/2026 14:54 ; jamais 40312, pris par la signalisation de Geyser), 44301 (voicechat, inutilisé), 46199 (relais). À tester : un client Bedrock à jour (NetherNet) et un plus ancien (RakNet) ; sans certificat HTTPS, demande de confiance à la première connexion.
- **CoreProtect ne tourne pas sur Kixster** (relevé le 23/10/2026 dans le journal : « Minecraft 26.3 is not supported », le plugin s'arrête au démarrage ; pas de dossier `plugins/CoreProtect/`) : aucune trace des blocs posés / cassés pour les enquêtes et les rollbacks. Signalé à LeKiwi06, rien de fait.


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

Format : `### 2026-10-23 — LeKiwi06

*(Session du 23/10/2026, de 15 h 10 à 16 h. Réservations gardées : KS_Parrainage, KLM_Statistiques, KS_RewardsGUI, KaliumRelay, KLM_Contacts, KS_Didacticiel, jusqu'au déploiement.)*

**Ajout (25/10/2026, 03:10, autre session de LeKiwi06) : KS_MondeTest 1.0.0 compilé, poussé et déployé sur Kixster
à 03:10 (accord de LeKiwi06), à redémarrer, non testé en jeu** : monde de test en créatif sur Kixster, étanche par rapport à la survie (voir « Versions compilées, non
déployées »). Déployé avec KLM_Menu 2.12.0 (clé `espaces-interfaces`) et le monde de test sans claims dans
SimpleClaimSystem ; réservation libérée. Après le redémarrage : `mondetest ajouter LeKiwi06 op` dans la console, puis
le plan de test du `JOURNAL.md` de KS_MondeTest.

**Parrainage, liste blanche automatique, demande de liste blanche, statistiques : compilés et poussés, non déployés,
non testés en jeu.** Demande faite à partir de son message d'annonce. Ses choix : demandes traitées dans un menu admin en
jeu (bot Discord plus tard) ; filleul choisi parmi ses amis ou par un autre pseudo ; barème proposé en .xlsx puis validé
(8 paliers de 1 h à 200 h de jeu actif du filleul, 5 à 600 émeraudes de valeur moyenne pour chacun, environ 6 émeraudes
par heure jouée, AFK non compté, pas de coffre à l'arrivée, pas de limite de filleuls) ; toutes les stats vanilla dans
« Informations », pour Java et Bedrock (rétroactif : le serveur garde déjà les stats de chaque joueur).

- À déployer ensemble (voir « Versions compilées, non déployées ») : proxy, tous les serveurs Paper (KLM_Contacts),
  Kixster. Réglages à la main : `relay-token` de KS_Parrainage, `whitelist on` sur Kixster, `fusion.yml` présent.
- Le proxy ne refuse l'entrée de Kixster qu'une fois que KS_Parrainage lui a envoyé la liste blanche (5 s après le
  démarrage de Kixster).
- Détail des tests à faire : `historique-conversations/2026-10-23-parrainage.md`.

**Ajout (23/10/2026, 15:50, autre session de LeKiwi06) : Geyser 2.12.0 (b1269) envoyé sur le proxy ; actif après
redémarrage du proxy, non testé.** Demande de LeKiwi06 : les joueurs Bedrock ne voient pas les blocs de la 26.3 (Geyser
2.11.3 parlait la 26.2, ViaBackwards traduisait). Floodgate du proxy déjà en 2.2.5 b141 (dernière, avec la 26.3) :
inchangé. Correspondances des objets custom (format 2) et protocole Bedrock des joueurs (2193) toujours pris en charge.
À tester après le redémarrage : connexion Bedrock, blocs 26.3, objets custom KaLium.
**Outils sans effet pour un joueur Bedrock (.Cybonath2, Kixster), non résolu** : houe, pelle, hache annulées (bloc et
solidité remis), même à ses pieds hors claims ; il pose des blocs et ouvre des coffres ; LeKiwi06 réussit au même endroit.
Écartés : nos plugins, SimpleClaimSystem (ne contrôle pas labour / chemin / écorçage), WorldGuard, GrimAC (exempte les
joueurs Bedrock), bordure du monde, portée. Piste restante : Geyser refuse l'action quand il croit que l'objet en main ne
correspond pas, ou cause liée à son appareil. À retester après la mise à jour de Geyser ; sinon `debug-mode: true` dans
le `config.yml` de Geyser (proxy) pour voir le motif.

### <aaaa-mm-jj> — <pseudo>`. Un seul compte rendu par personne ici ; les précédents vont dans
`archive_reprise.md`.

### 2026-10-30 — LeKiwi06 (maléfices du Nether)

Cahier local `Malefices_Nether.md` validé ; **déployé le 30/10/2026 à 18:56, à redémarrer, non testé en jeu** :
KS_MaleficesNether 1.0.0 (nouveau : compteur du temps passé dans le Nether, maléfices tirés au hasard, barre de boss
rouge, amulette mangeuse de mal), KS_Jetons 2.3.0, KS_Atouts 1.2.0, KS_KaliumGive 1.13.0, KS_Textures 1.2.0,
didacticiel, geyser-bedrock 1.10.0 (proxy), butin des récompenses régénéré (Kixster, Kanvas). Le Nether est encore
fermé sur Kixster. Restes : pack Java 1.1.0 sur minepack.fr (LeKiwi06), ranger le pack Bedrock 1.9.0 pendant l'arrêt
du proxy, retouche de la texture de l'amulette (premier jet de Claude). Détail :
`historique-conversations/2026-10-30-malefices-nether.md`.

### 2026-10-11 — Maxster33

*(Session du 11/10/2026, de 15 h 05 à 15 h 45. Fin de session : réservation `geyser-bedrock` libérée. **Relais à
LeKiwi06.**)*

**Bedrock bloqué au chargement des packs — non résolu, à reprendre par LeKiwi06.**
1. Jusqu'au redémarrage de 15:05, toutes les connexions Bedrock tombaient en « An internal error occurred! »
   (`NoSuchFileException … KaLium-objets-1.3.0.mcpack`) : le pack 1.3.0 avait été retiré de `packs/` à 09:00 alors que
   le proxy tournait (même piège que le 08/10 : redémarrer le proxy juste après un envoi de pack). Réglé par le
   redémarrage.
2. Depuis, le téléphone Android de Maxster33 (Minecraft **26.52**) reste bloqué au chargement des packs, puis
   abandonne (« Bedrock client disconnected ») ; aucune erreur côté proxy. Caches vidés, autre connexion : pareil.
   **MRMister7866 est entré sur mobile à 15:31** : le proxy envoie bien les packs.
3. Vérifié : pack 1.4.0 du proxy sain (identique au 1.3.0 hormis le Cristal de purification) ; `kalium_objets.json`
   valide ; Geyser charge 17 objets sans avertissement.
4. **Essai sans effet, envoyé sur le proxy à 15:25 (Maxster33)** : les 11 `"minecraft:max_stack_size": 64` retirés de
   `custom_mappings/kalium_objets.json` (pack 1.4.0 inchangé ; fichier de 10:47 dans
   `/plugins/Geyser-Velocity/_removed-kalium-objets-1.4.0-pile64/`). Toujours bloqué après redémarrage (15:26).
   **Donc : les objets custom ne s'empilent plus par 64 sur Bedrock pour l'instant ; à remettre** (fichier du commit
   e428b1e ou celui du dossier `_removed-…`) une fois le problème réglé.
5. **Piste suivante, non faite** : Geyser-Velocity du proxy en **2.11.3-b1249** (prise en charge de 26.52 depuis le
   06/10) ; la **b1250 (09/10)** corrige « slow resource pack downloading ». Passer en **b1251** (dernière, 10/10),
   b1249 rangé dans `/plugins/_removed-geyser-b1249/`, redémarrer, faire retester Maxster33. Demander aussi la version
   de Minecraft de MRMister7866 (s'il est aussi en 26.52, chercher plutôt côté téléphone de Maxster33).

Détail : `geyser-bedrock/README.md` (Historique).
