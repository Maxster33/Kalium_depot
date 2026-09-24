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
**Déploiement** : avec KalGames 1.17.0, sur Kal-Games (7001). **Statut : compilé, non déployé, non testé en jeu.**
