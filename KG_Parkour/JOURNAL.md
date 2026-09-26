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

**Déploiement** : avec KalGames 1.20.0, sur Kal-Games (7001), le 26/09/2026 à 4 h 41. **Statut : testé et confirmé par LeKiwi06 le 26/09/2026.**

## 1.1.0 — chrono par checkpoint, anti-collision, bottes de proximité (26/09/2026)

**Demande de LeKiwi06** : « update le parkour, les timers synchro : on n'est pas éliminé au bon moment en fonction du
CP ; il faudrait aussi enlever les collisions entre joueurs et nous rendre invisible avec des bottes en cuir colorées
si on est trop proche d'un autre adversaire (3 blocs de distance) ». Choix de LeKiwi06 : le chrono du cahier des
charges avec des réglages par jeu (le panneau par checkpoint et par map viendra ensuite) ; invisible **seulement pour
l'adversaire proche**.

- **Chrono** (cahier des charges, points 1 et 2) : remplace le temps maximum fixe entre deux points de contrôle
  (`checkpoint-timeout-seconds`, 300 s, qui repartait de zéro à chaque point, n'est plus utilisé). Chrono de départ
  `chrono-start-seconds` (30 s) ; chaque point de contrôle ajoute `chrono-add-seconds` (30 s), puis
  `chrono-add-late-seconds` (60 s) à partir du point `chrono-late-from-checkpoint` (7). Barre d'action :
  « Chrono m:ss » (rouge sous 10 s), « +30 s » au passage d'un point. À zéro, le joueur **tombe au temps** : titre
  « Temps écoulé », il passe spectateur ; ce n'est pas un abandon. Résultats : « tombé au temps ». Réglages dans le
  panneau admin du jeu ; `chrono-start-seconds: 0` = pas de chrono. Mode entraînement : toujours sans chrono.
- **Anti-collision** : les coureurs sont dans une équipe « sans collision » du tableau principal (`kgparkour_nocol`),
  comme les bateaux de KG_BoatRace, pendant le compte à rebours et la course.
- **Bottes de proximité** (`ProximityGhosts`, réglage `ghost-radius`, 3 blocs, 0 = désactivé) : quand deux coureurs
  sont à moins de 3 blocs, chacun cache l'autre et voit à sa place un porte-armure invisible (marqueur : ni collision,
  ni prise possible) qui ne porte que des bottes en cuir à la couleur de ce coureur (16 couleurs, une par coureur) et
  suit sa position à chaque tick. Les spectateurs et les autres coureurs voient les joueurs normalement. Retour à la
  normale au-delà de 3 blocs, à l'arrivée, en tombant au temps, en quittant et en fin de course.

Limites : Paper ne sait pas rendre un joueur invisible pour une seule personne, d'où la copie en bottes ; un joueur
caché disparaît aussi de la liste des joueurs (Tab) de celui qui le cache, le temps d'être proche. Bottes et
anti-collision à tester sur Bedrock. Le délai de grâce de 30 s après le 1er et le temps limite global restent inchangés.

**Déploiement** : seul (aucun changement de KalGames), sur Kal-Games (7001). **Statut : compilé, non déployé, non testé.**
