# Kalium — plugins du réseau Minecraft KaLium

Code source des plugins du réseau KaLium (Paper 26.2 / Velocity), maintenu par LeKiwi06 et Maxster33 avec Claude.

**Avant toute chose, lire dans l'ordre : `REGLES.md`, `TRAVAIL_EN_COURS.md`, `REPRISE_PROJET.md`.**

| Dossier | Plugin | Serveur |
|---|---|---|
| `KalGames/` | Hub, moteur des parties et mini-jeux restants (Rush) - voir `ARCHITECTURE_CIBLE.md` | kal-games |
| `KG_Menu/` | Menu du serveur kal-games (jeux, paramètres), alimenté par les plugins de kal-games | kal-games |
| `KG_BoatRace/` | Course de bateau (sortie de KalGames ; cahier des charges dans le dossier) | kal-games |
| `KG_Parkour/` | Parcours (sorti de KalGames) | kal-games |
| `KG_PvpKit/` | PvP Kit (sorti de KalGames) : vote du kit, équipes, kits, barème, déclassement | kal-games |
| `KG_ScoreBoards/` | Classements (points, temps, archives, panneaux du hub) - KalGames en dépend | kal-games |
| `KG_Bingo/` | Partie Bingo du hub : créer / rejoindre, transfert vers Kixster (dépend de KalGames) | kal-games |
| `KG_BingoGame/` | Le jeu Bingo lui-même (anciennement KalBingo) | Kixster |
| `KG_BuildBattle/` | Build Battle du hub : bouton du menu, file publique, parties privées, envoi vers Kanvas | kal-games |
| `KV_BuildBattle/` | Build Battle : arène, terrains, votes (cahier des charges commun avec KG_BuildBattle dans le dossier) | Kanvas |
| `KLM_Menu/` | Interface globale : navigation entre serveurs, catalogue des interfaces des plugins, boîte à outils des menus (anciennement KaliumMenu) | chaque serveur Paper |
| `KLM_Portal/` | Portails : une région WorldGuard reliée à une destination de KLM_Menu (désactivée avec son bouton) | chaque serveur Paper (lobby d'abord) |
| `KLM_Chat/` | Tchat inter-serveur : `/global send <message>`, `/global true` / `false`, préfixe coloré par serveur, séparations avec le tchat du serveur | chaque serveur Paper |
| `KLM_Hub/` | Lobby : barre de boss (texte défilant Discord / Twitch), QR codes en main secondaire pour « Recherche de joueurs » (dépend de KLM_Menu) | lobby |
| `KaliumRelay/` | Relais HTTP entre serveurs | proxy Velocity |
| `KLM_DiscordBot/` | Bot Discord (Node.js, hors Minecraft) : classements, stats et graphiques tirés de l'API de KG_ScoreBoards (cahier des charges dans le dossier) | hébergement à choisir |
| `KaliumCore/` | Survie (projet en pause) | Kal-Test-Dev |
| `KS_Dimensions/` | Menu des opérateurs (`/dimensions`) pour activer / désactiver les portails du Nether et de l'End | event |
| `KS_Enclume/` | Enclume sans plafond de prix (coût vanilla) | event |
| `KS_Villageois/` | Villageois et marchands ambulants sans échanges (sauf étiquette `ks_pnj`) | event |
| `KS_Crafts/` | Crafts du serveur Event (sable rouge, blocs bruts, calcite, froglights, recette du Bedrock Breaker, Clé de l'End, bloc de verrue...) | event |
| `KS_BedrockBreaker/` | Bedrock Breaker : objet à usage unique qui retire un bloc de bedrock (protections WorldGuard respectées) | event |
| `KS_ItemSimple/` | Objets qui ne servent qu'au craft : Fragment et Cœur de Spawner, têtes en attente du plugin des têtes | event |
| `KS_Spawners/` | Spawners fabriqués avec leur créature ; posés par un joueur, ils tombent quand on les casse | event |
| `KS_BiomeChanger/` | Changeur de Biome : change le biome autour du joueur (overworld), menu et historique (dépend de KLM_Menu) | event |
| `KS_LootBlocs/` | Loots des blocs : minerais, feuilles, verrue du Nether | event |
| `KS_LootEntites/` | Loots des mobs (nerf des fermes AFK) et fioles d'expérience à niveau | event |
| `KS_LootPeche/` | Pêche sans livres enchantés | event |
| `KS_LootPotions/` | Potions basiques à ramasser sur les blocs et les mobs | event |
| `KS_Event/` | Cahier des charges des plugins du serveur Event (pas un plugin) | - |

- Historique détaillé de chaque plugin : `<plugin>/JOURNAL.md`.
- `jars-deployes/` : copies des jars actuellement en service.
- Compiler : `sh telecharger-outils.sh` (une fois par machine), puis `sh <plugin>/build.sh` → jar dans `sortie/`
  (détails dans `REPRISE_PROJET.md`, section « Compiler »).
