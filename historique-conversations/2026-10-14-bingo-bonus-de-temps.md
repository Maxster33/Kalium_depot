# 2026-10-14 — Bingo : bonus de temps du contre la montre

- Plugin(s) concerné(s) : KG_BingoGame
- Versions avant / après : 0.11.0 → 0.12.0

## Demandé
LeKiwi06 : « combien de points rapporte une game de chrono en solo du bingo avec 25 objectifs difficiles si je les
fais tous en 5 minutes par item ? », puis « et si je finis toute la grille en 1h30 ? », puis « le bonus de temps n'est
pas assez conséquent, le faire à cette vitesse devrait rapporter bien plus je pense, aux alentours de 1800 ».

## Fait
- Calcul (barème 0.11.0) : 880 points d'objectifs (16 par objectif, coefficients des 12 bingos difficiles) + bonus de
  temps : 142 à 5 min par objectif (1 022), 247 en 1 h 30 (1 127). Correction en cours de route : en solo, 10 bingos au
  plus ; la grille entière n'est faite que si la dernière case est sur une diagonale.
- KG_BingoGame 0.12.0 : chaque minute restante vaut la valeur moyenne d'un objectif de la grille
  (`BingoSettings.chronoTimeBonus`) ; 1 h 30 → 1 705, 2 h 05 → 1 355. Compilé, déployé sur Serveur Jeux le 14/10/2026
  à 16:06 (0.11.0 dans `_removed-kg_bingogame-0.11.0/`), non testé, actif après redémarrage.

## Décisions
- Trois formules proposées : 1 point par 5 s pour toute grille (1 870, mais le temps l'emporte sur les objectifs dans
  les petites parties faciles), valeur selon la grille (1 705), multiplicateur du blackout (2 007). LeKiwi06 a choisi
  la valeur selon la grille.
- Repère : la même grille en blackout finie en 1 h 30 rapporte 1 760.
- Réservation en 3e plugin de 13 h à 23 h : accord de Maxster33 (selon LeKiwi06).

## Reste à faire
- Redémarrer Serveur Jeux, puis gagner une partie en contre la montre et vérifier le message « N min SS s restantes :
  +X pts ».
- Si 1 705 paraît encore bas : monter la valeur de chaque minute (×1,1 → environ 1 787).
- Dossiers supprimables sur Serveur Jeux : `_removed-kg_bingogame-0.8.0` à `0.8.7` et `0.9.0` (suppression par
  l'humain).
