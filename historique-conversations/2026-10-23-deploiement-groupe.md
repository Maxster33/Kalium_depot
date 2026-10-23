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
| Proxy (`Proxy velocity [new]`) | KaliumRelay 1.9.0 | **KaliumRelay 1.10.0** (+ Geyser 2.12.0 de l'autre session) |
| Kixster (`Kixster[new]`) | KS_Jetons 2.0.2, KS_Teleport 1.3.0 | KS_Jetons 2.1.0 + KS_Teleport 1.4.0 (ensemble) |
| | KS_Claim 1.4.0, KS_Economy 1.9.0 | KS_Claim 1.5.0, KS_Economy 1.10.0 |
| | KS_Crafts 1.15.0, KS_VerrouVehicule 1.0.0 | KS_Crafts 1.19.0, KS_VerrouVehicule 1.1.1 |
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

1. Envoyer partout (serveurs en marche : les jars ne sont lus qu'au redémarrage).
2. Redémarrages par LeKiwi06 : **proxy d'abord**, puis Kixster, lobby, Serveur Jeux, Kanvas.
3. Lire les journaux (aucune erreur attendue ; KS_Jetons convertit `jetons.yml` au format 2 et garde l'ancien en
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
- **KS_Crafts 1.19.0** en dehors des trains.
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

## Tests conseillés, dans l'ordre (après redémarrage)

1. Arrivée sur Kixster d'un joueur déjà inscrit : rien ne change ; `/didacticiel`, Informations › Statistiques.
2. `/jeton`, `/badge` : tes jetons et badges sont toujours là après la conversion ; une case de localisation.
3. `/claim`, `/claims`, `/magasins`.
4. Un train complet (déjà testé sur Event : vérifier seulement qu'il marche sur Kixster), un bateau à la laisse.
5. Parrainage et demande de liste blanche avec un compte non inscrit (voir `2026-10-23-parrainage.md`).
6. Kal-Games : « Changer les réglages » dans une salle d'attente ; tchats séparés à deux.
