# 2026-10-09 — Kixster : données des joueurs remises à zéro

- Plugin(s) concerné(s) : aucun code modifié ; fichiers de données de KS_RewardsGUI, KS_AntiCheat, KS_Economy,
  KS_Jetons, KS_Claim, KS_Teleport, KS_CoffreMort, KS_Fly, KS_FairPlay, KS_BiomeChanger, KS_Tableau, KLM_Menu,
  KLM_Chat, SimpleClaimSystem, GrimAC, et données Minecraft des joueurs
- Versions avant / après : inchangées

## Demandé

LeKiwi06 : « il faut supprimer les données de joueurs de tout les plugins sur kixster , pour qu'on commence tous
de 0 ». Puis, pendant le travail : « tu peux aussi en profiter pour time set 0 , mettre la
playersleepingpercentage a 25 % etc ».

## Fait

- Lecture seule d'abord : 4 comptes (LeKiwi06, Maaxster, leurs 2 comptes Bedrock), uniquement des données d'essai.
- Kixster éteint par LeKiwi06 (19:46:35). À 19:48, tout a été **déplacé** dans
  `/_removed-donnees-joueurs-2026-10-09/` (même arborescence que l'original), rien n'est supprimé. Liste complète :
  `REPRISE_PROJET.md`, compte rendu de LeKiwi06, ajout du 09/10 à 19:48.
- Vérifié après coup : les dossiers des plugins ne contiennent plus que `config.yml` et `lang.yml` (plus
  `rachats.yml` pour KS_Economy), les dossiers `players/data`, `stats` et `advancements` sont vides.

## Décisions

- LeKiwi06 : « Oui, tout » : les données Minecraft (inventaire, coffre de l'Ender, XP, position, succès,
  statistiques) sont remises à zéro aussi. L'extension du coffre de l'Ender (KS_EC_Extension) est dans ces
  données, donc remise à zéro avec.
- Choix de Claude signalés : `rachats.yml` gardé (tirage de la semaine du serveur) ; LuckPerms, régions
  WorldGuard, whitelist, ops et `usercache.json` gardés ; préférences `objets-masques.yml` (KLM_Menu) et
  `masques.yml` (KLM_Chat) rangées avec le reste (« tout les plugins »).
- Heure et règle de sommeil : pas faites. Ce sont des commandes de console, le serveur était éteint et Claude ne
  le démarre pas ; les règles de jeu ne sont pas dans `level.dat` en 26.3 (lu en lecture seule), pas de
  modification du monde à la main.

## Reste à faire

- LeKiwi06 : redémarrer Kixster, puis `/time set 0` et la règle de sommeil à 25 % ; préciser ce que couvre « etc ».
- Lire le journal du redémarrage (plugins qui recréent leurs fichiers sans erreur).
- Le monde n'a pas été touché : constructions et coffres des essais toujours là.
- Non touchés car hors de Kixster : boîte `kixster` du relais (récompenses pas encore livrées), points et
  classements de Kal-Games.
- Suppression définitive du dossier `_removed-donnees-joueurs-2026-10-09` : par l'humain, quand il le voudra.

## Suite : journal après redémarrage

- « j'ai redémarré kixster, tu peux lire le journal » (LeKiwi06) : redémarrage de 19:52, journal lu à 19:59. Aucune
  erreur ; nos plugins démarrent sans leurs fichiers de données et les recréent à l'usage.
- Fait par LeKiwi06 : `players_sleeping_percentage` à 25 (19:54), trois joueurs ajoutés à la whitelist. Pas de
  `/time set 0` dans le journal à 19:59.
- Signalé, cause non cherchée : compte neuf apparu en Y 121 près du spawn (point d'apparition du monde en Y 73),
  mort de chute 12 s après la connexion.
