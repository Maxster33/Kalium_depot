# 2026-10-05 — Bingo : même seed deux soirs de suite, réserve par lots

- Plugin(s) concerné(s) : KG_BingoGame (et lecture de KG_BoatRace)
- Versions avant / après : KG_BingoGame 0.9.0 → 0.10.0 (compilée, non déployée)

## Demandé
1. « Peux-tu me dire en détail comment sont comptés les points dans le BoatRace sur KalGames » : expliqué (barème par
   tour, multiplicateurs additifs, Grand Prix, cumul), puis « est-ce que tous les détails des points distribués sont
   sauvegardés » : oui, journal de KG_ScoreBoards (lignes `lap` et `race`), seul le total va dans `stats.yml`.
2. « Il faudrait enlever le bonus tour en tête pour les courses en solo », rétroactif : KG_BoatRace était réservé par
   LeKiwi06 ; abandonné par Maxster33 (« non laisse tomber »).
3. « Hier soir j'ai lancé une partie et je me suis retrouvé avec exactement la même seed que avant hier soir » : cause
   trouvée dans la réserve de mondes de la 0.8.5 (voir `KG_BingoGame/JOURNAL.md`, 0.10.0).
4. Réserve demandée : au redémarrage, réserve vidée puis 3 lots de seeds pour 4 équipes et 3 lots solo ; suppression
   de la seed à la fin de chaque partie (1 chunk/s pendant une partie, 6 sinon), puis génération lente d'une nouvelle ;
   pause à 4 parties en cours.

## Fait
- KG_BingoGame 0.10.0 : réserve par lots (détail dans `KG_BingoGame/JOURNAL.md`), compilée.
- Réservation de KG_BingoGame après sa libération par LeKiwi06.

## Décisions
- Suppression en arrière-plan plutôt que chunk par chunk (aucun gain côté lag) ; pendant une partie, seulement le
  terrain de la réserve ; mondes en trop d'un lot supprimés tout de suite ; sans lot libre, mondes créés pour la
  partie. (Choix de Maxster33 sur les recommandations de Claude.)

## Reste à faire
- Déployer KG_BingoGame 0.10.0 sur Serveur Jeux (sur demande de Maxster33), puis tester (voir le journal).
- Surveiller la mémoire et le disque de Serveur Jeux avec 45 mondes en réserve.
