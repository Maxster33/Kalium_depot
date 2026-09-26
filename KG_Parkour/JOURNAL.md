# JOURNAL — KG_Parkour

Parcours de Kal-Games, sorti de KalGames (règle 2.2 de `REGLES.md`), comme la course de bateau (KG_BoatRace).
Cahier des charges : `CAHIER_DES_CHARGES.md` (même dossier). Serveur : `kal-games` (machine 7001).

## 1.0.0 — sortie de KalGames (26/09/2026)

**Demande de LeKiwi06** : « isoler KG_Parkour du reste en suivant le cahier des charges » (le Parcours sort de KalGames
dans ce plugin séparé, comme KG_BoatRace ; étape 0, sans changement de règles).
- `ParkourInstance` : le Parcours repris **tel quel** de `RaceInstance` (KalGames 1.19.1), sans la partie « bateau »
  (inactive depuis KalGames 1.17.0) : départ commun, compte à rebours, points de contrôle (rayon, retour au dernier
  point en cas de chute ou de mort, orientation de la caméra gardée depuis 1.19.1), temps maximum entre deux points,
  points par point de contrôle et du podium, 30 s de grâce après le 1er, temps limite global, mode entraînement en
  partie privée (sans limite ni points, « Recommencer depuis le départ »), record personnel. Aucun changement de
  comportement. Textes : ceux du `lang.yml` de KalGames (`race.*`, `game.body-training`, `game.training-restart`).
- `KGParkour` : enregistre le type `PARKOUR` auprès de KalGames 1.20.0 (mêmes réglages et points d'arène qu'avant,
  moteur, classement au temps, case « Mode entraînement » à la création d'une partie privée, préchargement des arènes
  autorisé). Même nom de type : mini-jeux, arènes et classements existants repris sans rien reconfigurer.
- À l'arrêt du serveur, ferme lui-même ses parcours en cours (Paper le désactive avant KalGames).
- Dépend de KalGames (1.20.0 minimum) et KG_ScoreBoards.
- Les nouvelles règles du cahier des charges (chrono qui s'allonge à chaque point de contrôle, barème, contre-la-montre,
  fantôme...) viendront dans les versions suivantes, étape par étape.

**Déploiement** : avec KalGames 1.20.0, sur Kal-Games (7001). **Statut : non testé en jeu, non déployé.**
