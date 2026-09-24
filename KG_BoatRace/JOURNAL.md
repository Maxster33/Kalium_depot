# JOURNAL — KG_BoatRace

Course de bateau de Kal-Games, sortie de KalGames (règle 2.2 de `REGLES.md`). Cahier des charges :
`CAHIER_DES_CHARGES.md` (même dossier). Serveur : `kal-games` (machine 7001).

## 1.0.0 — sortie de KalGames (24/09/2026)

**Demande de LeKiwi06** : étape 0 du cahier des charges, sortir la course de bateau de KalGames.
- `BoatRaceInstance` : la course de bateau reprise **telle quelle** de `RaceInstance` (KalGames 1.16.0), partie
  « bateau » seulement : grille de départ, compte à rebours, bateau remis sous le joueur, checkpoints, ligne d'arrivée
  à deux points, tours, meilleur temps sur 1 tour, points du podium, temps limite proportionnel au nombre de tours,
  30 s de grâce après le 1er. Aucun changement de comportement. Textes : ceux du `lang.yml` de KalGames (`race.*`).
- `KGBoatRace` : enregistre le type `BOAT_RACE` auprès de KalGames 1.17.0 (mêmes réglages et points d'arène
  qu'avant, moteur, classement au meilleur tour, option « nombre de tours » en partie privée, pas de préchargement).
  Même nom de type : mini-jeux, arènes et classements existants repris sans rien reconfigurer.
- À l'arrêt du serveur, ferme lui-même ses courses en cours (Paper le désactive avant KalGames).
- Dépend de KalGames (1.17.0 minimum) et KG_ScoreBoards.
**Déploiement** : avec KalGames 1.17.0, sur Kal-Games (7001). **Statut : déployé le 24/09/2026 à 19 h 47, testé et confirmé par LeKiwi06 le 24/09/2026 (course de bateau, journal de démarrage : mini-jeu boatrace rattaché).**

## 1.1.0 — étape 1 : vitesse, seuil des meilleurs temps, passages aux checkpoints (24/09/2026)

**Demandes de LeKiwi06** (cahier des charges, étape 1).
- **Vitesse en km/h** dans la barre d'action (`race.status-speed`), calculée à partir du déplacement horizontal
  (1 bloc = 1 m), lissée ; ignorée après une téléportation.
- **Seuil des meilleurs temps** : seuls les tours de 45 s ou moins entrent dans les meilleurs temps (nouveau réglage
  « Meilleurs temps : tours enregistrés jusqu'à (s) », `record-max-lap-seconds`, 0 = tous). Un tour plus long est
  annoncé « non enregistré ». Clé absente des configs existantes : 45 par défaut.
- **Passages aux checkpoints mesurés** : pour chaque checkpoint et la ligne d'arrivée, temps depuis le début du tour,
  place au passage et vitesse d'entrée ; le tour précédent est gardé. Pas encore affichés ni enregistrés : base du
  classement en direct et des écarts (étape 2) et des données envoyées à KG_ScoreBoards (à faire, KG_ScoreBoards à
  réserver).
- Placement des checkpoints : nouvel éditeur de KalGames 1.18.0.
**Déploiement** : avec KalGames 1.18.0. **Statut : compilé, non déployé, non testé en jeu.**
