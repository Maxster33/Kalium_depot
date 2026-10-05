# 2026-10-05 — Points des opérateurs, moyenne au lieu de la somme (suite de la session du 04/10)

- Plugin(s) concerné(s) : KG_BoatRace, KG_BingoGame (code) ; KalGames, KG_ScoreBoards (configuration et données de
  Kal-Games seulement).
- Versions avant / après : KG_BoatRace 1.4.2 → 1.5.0 (Kal-Games) ; KG_BingoGame 0.8.7 → 0.9.0 (Serveur Jeux).

## Demandé
- « Récupère tous les points non crédités en admin, crédite-les, et supprime cette règle. »
- « Nouvelle règle également pour tous les jeux : en multijoueur on ne gagne plus la somme des joueurs en dessous de
  nous, mais la moyenne ; fais en sorte que ce changement soit rétroactif. »
- « C'est bon le serveur est arrêté, vas-y déploie. »

## Fait
- Points d'opérateur jamais crédités (LeKiwi06 seul concerné) ajoutés dans `stats.yml` de Kal-Games : Bingo 373,4, PvP
  Kit 270,25, Parcours 103,5, course de bateau 37 ; lignes « credit » dans le journal d'octobre.
- Règle « les points d'un opérateur ne comptent pas » désactivée : `stats.exclude-operators: false` dans les
  `config.yml` de KalGames et de KG_ScoreBoards sur Kal-Games.
- KG_BoatRace 1.5.0 et KG_BingoGame 0.9.0 : ses points + la moyenne des points de ceux classés en dessous. Déployés le
  05/10/2026 à 12:33.
- Rétroactif dans `stats.yml` : 6 courses de bateau et 7 parties de Bingo à 3 joueurs ou plus. Écarts au général :
  .PatientLime2170 -699,25 ; LeKiwi06 -309,33 ; JujuEngineer -214,5 ; .TomHeroes57 -77,34 ; Maaxster -54.
- Originaux sur Kal-Games : `/plugins/_removed-kg_scoreboards-stats-2026-10-05/` (stats, journaux d'octobre, config de
  KG_ScoreBoards) et `/plugins/_removed-kalgames-config-2026-10-05/`.

## Décisions
- Formule retenue (annoncée à LeKiwi06, non contredite) : ses points + moyenne des points de ceux classés en dessous ;
  le dernier ne gagne que ses points ; à 2 joueurs rien ne change.
- Bingo : un joueur qui a abandonné compte pour 0 dans la moyenne (choix de Claude : même liste que pour la somme).
- Parcours, PvP Kit, Rush : jamais eu cette règle, rien changé.
- Règle des opérateurs : coupée par configuration, pas retirée du code (proposé à LeKiwi06, pas demandé).
- Parcours d'opérateur à l'ancien barème : revalorisés au barème 1.2.0, fourchette haute, comme les autres joueurs la
  veille ; ancien podium à 0.
- Bingo rétroactif : points propres reconstitués depuis les logs de Serveur Jeux (total = propres + somme de ceux
  classés derrière + XP + temps) ; multiplicateur de blackout x1 dans les 7 parties (un blackout gagné demande au moins
  50 points propres ; totaux d'équipe cohérents). Parties d'avant la 0.8.0 (créditées au double le 28/09) : écart doublé.
- Septembre : général seulement, archives du mois et des semaines non touchées. Aucun signal à KG_Rewards.
- Scripts de calcul et fichiers des serveurs gardés hors du dépôt.

## Reste à faire
- Redémarrer Kal-Games et Serveur Jeux (l'humain).
- Tester : partie en opérateur comptée ; course de bateau et Bingo à 3 joueurs ou plus.
- Supprimables par l'humain : Kal-Games `_removed-kg_boatrace-1.3.0/`, `1.4.0/`, `_removed-kg_parkour-1.0.0/` ; Serveur
  Jeux `_removed-kg_bingogame-0.8.0/` à `0.8.5/`.
- Si voulu : retirer l'option `stats.exclude-operators` du code (KalGames, KG_Bingo, KG_ScoreBoards).
