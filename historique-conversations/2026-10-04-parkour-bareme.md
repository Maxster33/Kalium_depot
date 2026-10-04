# 2026-10-04 — Nouveau barème du Parcours

- Plugin(s) concerné(s) : KG_Parkour (serveur Kal-Games).
- Versions avant / après : KG_Parkour 1.1.1 → 1.2.0 (compilé, non déployé).

## Demandé
- « J'aimerais modifier kg_parkour : globalement les points gagnés pour avoir validé chaque checkpoint ce sera : or 1,
  fer 1, cuivre 3, améthyste 3, glace 3, netherite 5, diamant 5, émeraude 7, bloc invisible 7, eau 7, barrière 10. »
- Bonus : finir un checkpoint en 1er x1,5 ; sans tomber x1,5 ; sous le barème de temps x1,25 à x1,75 en fonction du
  temps fait ; terminer en 1er le parkour +25. « Tout ce qui concerne "fait en 1er" n'est pas obtenable en solo. »

## Fait
- KG_Parkour 1.2.0 : points de base par section selon sa position (réglable par point de contrôle), multiplicateurs
  additifs, +25 au 1er arrivé, points décimaux crédités à chaque section, détail dans le journal de KG_ScoreBoards.
  Détail : `KG_Parkour/JOURNAL.md`. Cahier des charges, `EQUILIBRAGE_POINTS.md` et `REPRISE_PROJET.md` mis à jour.

## Décisions
- Les blocs (or, fer...) ne sont que l'ordre des checkpoints dans la map (LeKiwi06) : pas de détection de bloc.
- Multiplicateurs additifs, comme la course de bateau (maximum x2,75) : choix de LeKiwi06.
- Bonus de temps avec un seul temps par checkpoint : x1,25 juste en dessous, x1,75 à la moitié ou moins (LeKiwi06).
- Défauts de Claude, annoncés à LeKiwi06 : l'ancien podium 3 / 2 / 1 disparaît ; « sans tomber » = aucun retour au
  point de contrôle (chute, mort, objet) ; solo = un seul coureur dans la partie ; pas de cumul en fin de partie ;
  l'arrivée compte comme la section qui suit le dernier point de contrôle ; valeurs réglables dans les réglages du jeu.

## Reste à faire
- Déployer sur Kal-Games, puis saisir le « Temps du bonus » de chaque point de contrôle.
- Tester : points de chaque checkpoint, bonus du 1er (2 joueurs) et absent en solo, sans chute, temps, +25, résultats,
  crédit au classement avec un non-opérateur.
- Mesurer le rythme de points (~135 / 30 min visés, `EQUILIBRAGE_POINTS.md`).
