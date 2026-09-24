# 2026-09-24 — Bingo, classements, interfaces (KLM_Menu / KG_Menu)

- Plugin(s) concerné(s) : KaliumRelay, KalGames, KG_Bingo, KG_BingoGame (ex-KalBingo), KG_ScoreBoards (nouveau),
  KLM_Menu (ex-KaliumMenu), KG_Menu (nouveau).
- Versions avant / après (en service) : KaliumRelay 1.1.0 → 1.1.1 ; KalGames 1.12.3 → 1.15.1 ; KalBingo 0.1.22 →
  KG_BingoGame 0.5.0 ; KG_Bingo 1.2.0 ; KG_ScoreBoards 1.1.0 ; KaliumMenu 1.5.0 → KLM_Menu 2.0.0. Compilés non
  déployés : KG_Menu 1.0.0, KLM_Menu 2.1.0, KalGames 1.16.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0.

## Demandé
- Mise en route sur le PC de LeKiwi06, lecture complète, compilation locale, règles de travail (réservations,
  un plugin = un rôle, comptes rendus signés, non destructif, secrets).
- Remplacer le jeton du relais publié par erreur, et empêcher que ça se reproduise.
- Découper le Bingo (KG_Bingo côté hub, KG_BingoGame côté jeu) puis le refaire : liste d'objectifs, barème, modes,
  nulle, inactivité, keepInventory, invincibilité, carte en main secondaire, têtes, résumé, multiplicateur blackout.
- Sortir les classements de KalGames (KG_ScoreBoards) avec des archives détaillées pour un futur bot Discord.
- Une interface globale sur tous les serveurs (KLM_Menu) et un menu par serveur (KG_Menu) qui découvrent les
  interfaces des plugins au démarrage ; « une interface claire pour trouver les interfaces de chaque chose ».

## Fait
- Voir le compte rendu signé du 2026-09-24 dans `REPRISE_PROJET.md` et les `JOURNAL.md` des plugins (versions,
  déploiements, tests). Déploiements faits par Claude via WinSCP, redémarrages par LeKiwi06.

## Décisions
- Multiplicateurs additifs ; victoire à la 1re équipe qui atteint ses bingos (chrono : meilleur score) ; « Égalité »
  distincte de la nulle ; égalité de vote dans une équipe = oui ; mode solo gardé (speedrun).
- KG_ScoreBoards autonome (KalGames en dépend) ; résultats Bingo via KaliumRelay ; tout enregistrer, n'afficher que
  les classements individuels au début.
- Hiérarchie des interfaces : KLM_Menu → menu de serveur (KG_Menu...) → interfaces des jeux ; boussole gérée par
  KLM_Menu seul ; protections de zone restent dans KalGames pour l'instant.
- Proposition refusée : sauter la création de KG_Menu au profit d'une section « Jeux » dans KLM_Menu.

## Reste à faire
- Déployer KG_Menu 1.0.0 + KLM_Menu 2.1.0 + KalGames 1.16.0 + KG_Bingo 1.3.0 + KG_ScoreBoards 1.2.0 sur kal-games.
- Étape B de KG_ScoreBoards (classements Bingo, journal des gains, fiches de partie), étape C (envoi par KaliumRelay).
- Phase 2 de KLM_Menu (interfaces des autres serveurs).
- Tests en jeu listés dans le compte rendu ; bug du pseudo dans la liste des parties Bingo.
