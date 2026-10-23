# 2026-10-23 — Plan du déploiement groupé (préparé, rien d'envoyé)

Demande de LeKiwi06 (23/10/2026) : « prépare-toi à tout déployer, mais précise-moi ce que je n'ai pas encore testé ».
Il attend la fin des autres sessions (Geyser 2.12.0 sur le proxy) pour tout envoyer en même temps.

## Ce qui part, serveur par serveur

Tous les jars sont dans `sortie/` (compilés depuis le dépôt) ; ceux qui étaient sur Event sont identiques à
`jars-deployes/`. Vérifié le 23/10 à 16 h (lecture seule) : les jars en place sur Kixster sont ceux de
`jars-deployes/` (mêmes tailles), `fusion.yml` est présent dans `plugins/KS_RewardsGUI/`, KS_Decapitator n'a pas
encore de `config.yml`. Les autres serveurs sont à revérifier juste avant l'envoi (règle 4.2).

| Serveur (session WinSCP) | Remplace | Par |
|---|---|---|
| Proxy (`Proxy velocity [new]`) | KaliumRelay 1.9.0 | **KaliumRelay 1.10.0** |
| Proxy (déjà posé, rien à envoyer) | Geyser-Velocity 2.11.3-b1249 | **Geyser-Velocity 2.12.0-b1269**, envoyé le 23/10 à 15:47 par la session « Joueur Geyser blocs non modifiables » (ancien jar et copie de son `config.yml` dans `_removed-geyser-2.11.3-b1249/`) ; actif au redémarrage du proxy |
| Kixster (`Kixster[new]`) | KS_Jetons 2.0.2, KS_Teleport 1.3.0 | KS_Jetons 2.1.0 + KS_Teleport 1.4.0 (ensemble) |
| | KS_Claim 1.4.0, KS_Economy 1.9.0 | KS_Claim 1.5.0, KS_Economy 1.10.0 |
| | KS_Crafts 1.15.0, KS_VerrouVehicule 1.0.0 | KS_Crafts **1.20.0** (contient la 1.19.0 + 9 os → 3 blocs d'os), KS_VerrouVehicule 1.1.1 |
| | KS_Decapitator 1.2.0 | KS_Decapitator 1.3.0 |
| | — | KS_Trains 1.4.8, KS_LaisseBateau 1.0.2 (nouveaux sur Kixster) |
| | — | KS_Didacticiel 1.3.0 + `plugins/KS_Didacticiel/didacticiel.yml` (nouveau sur Kixster) |
| | KS_RewardsGUI 1.5.0 | KS_RewardsGUI 1.6.0 |
| | — | **KS_Parrainage 1.0.0**, **KLM_Statistiques 1.0.0** (nouveaux) |
| | KLM_Contacts 1.2.0 | KLM_Contacts 1.5.0 |
| Lobby (`Lobby [new]`) | KLM_Contacts 1.2.0 | KLM_Contacts 1.5.0 |
| Serveur Jeux (`Event [new]`, machine 7021) | KLM_Contacts 1.2.0 (d'après `REPRISE_PROJET.md`) | KLM_Contacts 1.5.0 |
| Kanvas + Kal-Games (`Kanvas[new]`) | KLM_Contacts 1.4.0, KalGames 1.27.0 | KLM_Contacts 1.5.0, KalGames 1.28.0 |

Chaque ancien jar va dans `/plugins/_removed-<plugin>-<ancienne version>/` (`put` du nouveau AVANT le `mv` de
l'ancien). KLM_Menu n'a pas à bouger : aucune fonction utilisée n'a changé depuis la 2.11.0 (en place partout).

## Ordre

1. Envoyer partout (serveurs en marche : les jars ne sont lus qu'au redémarrage). Sur le proxy, vérifier aussi
   qu'il n'y a qu'un seul jar de Geyser dans `/plugins/` (la 2.12.0-b1269).
2. Redémarrages par LeKiwi06 : **proxy d'abord**, puis Kixster, lobby, Serveur Jeux, Kanvas.
3. Lire les journaux (proxy : Geyser 2.12.0 démarre et passe son `config.yml` en version 9 ; aucune erreur attendue ; KS_Jetons convertit `jetons.yml` au format 2 et garde l'ancien en
   `jetons-2.0.yml`).

## Réglages à la main après le premier démarrage de Kixster

- `plugins/KS_Parrainage/config.yml` : `relay-token` = celui de `plugins/KS_RewardsGUI/config.yml` (jamais dans le
  dépôt), puis redémarrage ou `/ksparrainage recharger`.
- `plugins/KS_Decapitator/config.yml` (créé au démarrage, à `false`) : `tetes-de-joueurs: true` (prévu pour Kixster
  dans son journal), puis redémarrage.
- Liste blanche de Kixster active (`whitelist on`) : sans elle, le proxy laisse tout le monde entrer.
- `klmcontacts.whitelist` (opérateurs par défaut) : à donner dans LuckPerms au staff qui traite les demandes.

## Ce que LeKiwi06 n'a pas encore testé en jeu

**Testé** (sur Event, « j'ai testé tout marche bien », 22/10) : KS_Trains 1.4.8, KS_LaisseBateau 1.0.2 (et, à
travers les trains, sans doute la recette du wagon contrôleur de KS_Crafts 1.19.0, non noté).

**Jamais testé, parmi ce qui part :**
- **Geyser 2.12.0** (proxy) : connexion d'un joueur Bedrock, blocs de la 26.3 visibles (la raison de la mise à
  jour), objets custom KaLium (Clé de l'End, jetons, Cristal de purification...).
- **KS_Jetons 2.1.0 + KS_Teleport 1.4.0** : inventaire spécial en un coffre, cases de localisation (24 h d'attente),
  `/jeton`, `/badge`, `/bed`, et la conversion des données des joueurs (`jetons.yml`). Sur Event depuis le 18/10, aucun
  retour de test.
- **KS_Claim 1.5.0** : `/claim` et `/claims` (Java et Bedrock).
- **KS_Economy 1.10.0** : `/magasins`, `/shop`, `/market`, rachats de la semaine dans le catalogue.
- **KS_Didacticiel 1.3.0** (et 1.0.0 à 1.2.x) : tout le didacticiel, « Poser une question », réponses du staff,
  recettes cliquables.
- **KS_Decapitator 1.3.0** : têtes manquantes, shulkers teints, têtes de joueurs, barrières dans `/tetes`.
- **KS_VerrouVehicule 1.1.1** (wagons décorés de KS_Trains) ; sur Kixster, la 1.0.0 en place n'a jamais été testée
  non plus.
- **KS_Crafts 1.20.0** : la recette 9 os → 3 blocs d'os (ajoutée le 23/10), et le reste de la 1.19.0 en dehors des trains.
- **KalGames 1.28.0** : « Changer les réglages » modifie la partie sur place (même code, même salle d'attente).
- **Tout le parrainage** (KaliumRelay 1.10.0, KLM_Contacts 1.5.0, KS_Parrainage, KS_RewardsGUI 1.6.0) et
  **KLM_Statistiques** : tests détaillés dans `2026-10-23-parrainage.md`.

**Déjà en service, jamais testé (sous ce qui part) :**
- Kixster : KS_Economy 1.9.0, KS_Teleport 1.3.0, KS_Claim 1.4.0 (13/10), KS_VerrouVehicule 1.0.0 (14/10),
  KS_KaliumGive 1.11.0, KS_JukeboxBoat 1.1.2 (13/10).
- Kanvas / Kal-Games : fusion testée en solo seulement (tchats séparés : il faut deux joueurs) ; KalGames 1.26.0 et
  1.27.0, KG_Menu 1.2.0 (menus en coffre, salle d'attente, relance après 15 s), KG_Bingo 1.11.0, KG_BuildBattle
  0.6.0 / 0.7.0.
- Serveur Jeux : déménagement du 21/10 sur la machine d'Event ; KG_BingoGame 0.12.0 (contre la montre).
- KaliumRelay 1.9.0 et KLM_Contacts 1.4.0 (base du parrainage) : testés en solo seulement.

## Autres serveurs : rien d'autre à déployer, mais beaucoup de versions jamais notées « testées »

Seuls envois prévus hors Kixster : KaliumRelay 1.10.0 (proxy), KLM_Contacts 1.5.0 (lobby, Serveur Jeux, Kanvas),
KalGames 1.28.0 (Kanvas). Geyser 2.12.0 est déjà sur le proxy depuis le 23/10 à 15:47 (autre session) : actif au
prochain redémarrage du proxy, non testé (les joueurs Bedrock doivent entrer en 26.3).

Versions en service jamais confirmées en jeu (relevé du 23/10, `REPRISE_PROJET.md` et `JOURNAL.md`). Beaucoup ont
sans doute été jouées sans souci, mais personne ne l'a noté :
- **Proxy** : KaliumRelay 1.6.0 à 1.8.0 (amis, groupes, parties de groupe : jamais testés à plusieurs), 1.9.0 testé
  en solo ; Geyser 2.12.0.
- **Lobby** : KLM_Contacts 1.0.0 à 1.2.0, KLM_Menu 2.9.0 à 2.11.0 (Recherche de joueurs, bouton Contacts, menus en
  coffre), KLM_Hub 1.0.0 (barre de boss Discord / Twitch, QR codes), KLM_Chat 1.0.0. Testés : KLM_Portal 1.3.2,
  KLM_Menu 2.5.0.
- **Kanvas + Kal-Games** : fusion testée en solo (tchats séparés : à deux) ; KalGames 1.26.0 et 1.27.0, KG_Menu
  1.2.0 (menus en coffre, accueil Créer / Rejoindre, salle d'attente et relance après 15 s), KG_Bingo 1.11.0,
  KG_BuildBattle 0.6.0 / 0.7.0, KV_BuildBattle 0.5.0, KG_BoatRace 1.6.1 / 1.6.2 (anti-collision Bedrock, bateaux non
  solides), KG_Parkour 1.2.0 (barème), KG_PvpKit 1.1.0, KG_HideAndSeek 0.2.0, KG_PiliersFortune 0.2.0, KG_ScoreBoards
  1.9.0, KG_Rewards 1.3.0 et KV_Rewards 1.3.0 (paliers, tops ; les coffres eux-mêmes marchent), KV_Plots 1.7.0 +
  KV_Menu 1.4.0 (points de notes au carré, concours pendant les votes), KLM_Chat 1.1.0, KLM_Espaces 1.0.0.
- **Serveur Jeux** : déménagement du 21/10 (Bingo sur la nouvelle machine, mondes de réserve régénérés), KG_BingoGame
  0.9.0 à 0.12.0 (barème de victoire, réserve de mondes, « Rejouer », contre la montre), KG_BingoObservateur 0.1.0
  (`/observer`), KLM_Contacts 1.0.0 à 1.2.0.
- Le `rattrapage.yml` de KG_Rewards posé le 10/10 à 00:23 (5 coffres, dont les 16 récompenses parties vers la boîte
  d'Event) a bien été lancé (confirmé par LeKiwi06 le 23/10/2026).

## Tests conseillés, dans l'ordre (après redémarrage)

0. Un joueur Bedrock se connecte : il voit les blocs de la 26.3 et les objets custom. Faire réessayer **.Cybonath2**
   (houe, pelle, hache sans effet sur Kixster, cause non trouvée) : si c'est toujours le cas, passer `debug-mode: true`
   dans le `config.yml` de Geyser sur le proxy pour voir le motif du refus (piste de la session Geyser).
1. Arrivée sur Kixster d'un joueur déjà inscrit : rien ne change ; `/didacticiel`, Informations › Statistiques.
2. `/jeton`, `/badge` : tes jetons et badges sont toujours là après la conversion ; une case de localisation.
3. `/claim`, `/claims`, `/magasins`.
4. Un train complet (déjà testé sur Event : vérifier seulement qu'il marche sur Kixster), un bateau à la laisse.
5. Parrainage et demande de liste blanche avec un compte non inscrit (voir `2026-10-23-parrainage.md`).
6. Kal-Games : « Changer les réglages » dans une salle d'attente ; tchats séparés à deux.
