# 2026-10-04 — Nouveau barème du Parcours

- Plugin(s) concerné(s) : KG_Parkour (serveur Kal-Games).
- Versions avant / après : KG_Parkour 1.1.1 → 1.2.0 (déployé sur Kal-Games le 04/10/2026 à 22:16, non testé).

## Demandé
- « J'aimerais modifier kg_parkour : globalement les points gagnés pour avoir validé chaque checkpoint ce sera : or 1,
  fer 1, cuivre 3, améthyste 3, glace 3, netherite 5, diamant 5, émeraude 7, bloc invisible 7, eau 7, barrière 10. »
- Bonus : finir un checkpoint en 1er x1,5 ; sans tomber x1,5 ; sous le barème de temps x1,25 à x1,75 en fonction du
  temps fait ; terminer en 1er le parkour +25. « Tout ce qui concerne "fait en 1er" n'est pas obtenable en solo. »

- « Tu peux calculer une estimation rétroactive des points actuels du parkour ? », puis « prends la fourchette haute
  pour tout le monde et crédite les points ».

## Fait
- KG_Parkour 1.2.0 : points de base par section selon sa position (réglable par point de contrôle), multiplicateurs
  additifs, +25 au 1er arrivé, points décimaux crédités à chaque section, détail dans le journal de KG_ScoreBoards.
  Déployé sur Kal-Games (« vas-y déploie », LeKiwi06). Détail : `KG_Parkour/JOURNAL.md`. Cahier des charges, `EQUILIBRAGE_POINTS.md` et `REPRISE_PROJET.md` mis à jour.

- Estimation rétroactive : les 67 parties du journal de KG_ScoreBoards (25/09 au 04/10/2026) rejouées avec le nouveau
  barème ; fourchette selon le bonus « sans chute » (inconnu du journal) : 802 points actuels → 1 523 à 2 248
  (central 1 779) ; points d'avant le journal par règle de trois.
- Crédit de la fourchette haute dans `stats.yml` de Kal-Games (serveur arrêté par LeKiwi06, 22:26) : général, octobre,
  semaine en cours. Général : .TomHeroes57 334 → 1 108,43 ; LeKiwi06 107 → 291,04 ; .MRMister7866 110 → 250,16 ; Exyosis 73 → 222,61 ; .PatientLime2170 106 → 210,68 ; JujuEngineer 16 → 46,75 ; Ronoxsyb 15 → 43,43 ; Maaxster 25 → 30,66 ; NeirdaMC 7 → 20,27 ; LE_TIGRE32 5 → 16,5 ; .Lyfty4013 4 → 7,5.
  Original dans `/plugins/_removed-kg_scoreboards-stats-2026-10-04/`. Scripts et fichiers du serveur gardés hors du
  dépôt.

## Décisions
- Les blocs (or, fer...) ne sont que l'ordre des checkpoints dans la map (LeKiwi06) : pas de détection de bloc.
- Multiplicateurs additifs, comme la course de bateau (maximum x2,75) : choix de LeKiwi06.
- Bonus de temps avec un seul temps par checkpoint : x1,25 juste en dessous, x1,75 à la moitié ou moins (LeKiwi06).
- Défauts de Claude, annoncés à LeKiwi06 : l'ancien podium 3 / 2 / 1 disparaît ; « sans tomber » = aucun retour au
  point de contrôle (chute, mort, objet) ; solo = un seul coureur dans la partie ; pas de cumul en fin de partie ;
  l'arrivée compte comme la section qui suit le dernier point de contrôle ; valeurs réglables dans les réglages du jeu.

- Recalcul : c'est l'écart qui est ajouté ; personne ne perd de points (Maaxster garde ses 17 points d'octobre alors que
  le recalcul donnait 7,5, le podium ayant disparu) ; archives de septembre et des semaines non touchées.

## Reste à faire
- Redémarrer Kal-Games (l'humain), puis saisir le « Temps du bonus » de chaque point de contrôle.
- Supprimable par l'humain sur Kal-Games : `/plugins/_removed-kg_parkour-1.0.0/`.
- Tester : points de chaque checkpoint, bonus du 1er (2 joueurs) et absent en solo, sans chute, temps, +25, résultats,
  crédit au classement avec un non-opérateur.
- Mesurer le rythme de points (~135 / 30 min visés, `EQUILIBRAGE_POINTS.md`).
