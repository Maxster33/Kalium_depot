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

**Déployé seul sur Kal-Games (7001) le 26/09/2026 20:56** (serveur éteint, accord de LeKiwi06) ; 1.0.0 dans
`/plugins/_removed-kg_parkour-1.0.0/`. Nouveaux réglages : valeurs par défaut du code tant qu'ils ne sont pas changés
dans le panneau admin. **Testé et confirmé par LeKiwi06 le 27/09/2026** (« tout fonctionne bien »).

## 1.1.1 - noms de réglages plus courts (02/10/2026, LeKiwi06)

Demande de LeKiwi06 (02/10/2026) : dans les réglages des jeux pour les opérateurs, les jauges étaient très longues
(nom et explication dans la jauge, élargie par KLM_Menu 2.5.0) ; Minecraft ne permettant du texte qu'en haut d'une
fenêtre, « écrit des textes plus courts dans les champs de jauge et boutons ». Mesure de tous les textes (police de
Minecraft) : chaque champ et bouton tient maintenant dans sa largeur normale (260 / 240 pixels).
Noms raccourcis : « Arènes préchargées », « Temps par point de contrôle (s) », « Temps au 2e palier (s) ». Clés
inchangées.

**Déployé sur Kal-Games le 02/10/2026 à 17:49 (LeKiwi06 ; 1.1.0 dans `_removed-kg_parkour-1.1.0/`), actif après redémarrage.
Statut : **testé et confirmé par LeKiwi06 le 02/10/2026**.**

## 1.2.0 - nouveau barème (04/10/2026, LeKiwi06)

**Demande de LeKiwi06 (04/10/2026)** : points gagnés à chaque checkpoint validé, dans l'ordre de la map : or 1, fer 1,
cuivre 3, améthyste 3, glace 3, netherite 5, diamant 5, émeraude 7, bloc invisible 7, eau 7, barrière 10. Bonus :
finir un checkpoint en 1er x1,5 ; sans tomber x1,5 ; sous le barème de temps x1,25 à x1,75 selon le temps fait ;
terminer le parkour en 1er +25. « Tout ce qui concerne "fait en 1er" n'est pas obtenable en solo. » Choix de
LeKiwi06 : multiplicateurs **additifs** (comme la course de bateau) ; bonus de temps avec **un seul temps** par
checkpoint.

- **Points de base par section** (du point précédent au point de contrôle) selon sa position dans le parcours :
  1, 1, 3, 3, 3, 5, 5, 7, 7, 7, 10 (10 pour les points suivants). Réglage « Points de base » de chaque point de
  contrôle (arène > Points de contrôle > n° > Réglages de ce point) : 0 = selon la position.
- **L'arrivée** valide la section qui suit le dernier point de contrôle : avec 10 points de contrôle, c'est la 11e
  section (10 points) ; au-delà de la 11e position elle ne rapporte rien (reste le bonus du 1er arrivé).
- **Multiplicateurs additifs** (x1,5 et x1,5 = x2 ; maximum x2,75) sur les points de la section :
  - 1er à valider la section : x1,5 (`first-coef-x10`) ; jamais en solo (un seul coureur dans la partie) ;
  - sans chute : x1,5 (`clean-coef-x10`) ; aucune chute, mort ni retour avec l'objet depuis le point précédent ;
  - temps : réglage « Temps du bonus (s) » de chaque point de contrôle (`finish-time-seconds` pour l'arrivée) ; temps
    de la section juste sous ce temps : x1,25 (`time-coef-min-x100`), jusqu'à x1,75 (`time-coef-max-x100`) à la moitié
    de ce temps ou moins, proportionnel entre les deux. 0 (par défaut) = pas de bonus de temps.
- **1er arrivé** : +25 (`points-first-finish`), ajoutés tels quels ; jamais en solo.
- **Retirés** : 1 point par point de contrôle (`points-checkpoint`) et podium 3 / 2 / 1 (`points-win`, que recevaient
  aussi les coureurs non arrivés).
- Points **décimaux**, crédités à chaque section (comme avant : un joueur qui quitte garde ses points). Affichage :
  barre d'action « +5,25 pts (x1,75 : 1er, sans chute, temps x1,25) », message à l'arrivée avec le total, points de
  chacun dans les résultats.
- Journal de KG_ScoreBoards : l'événement « points » porte le détail (`section`, `sectionMillis` = temps de la
  section, `base`, `multiplier`, `first`, `clean`, `timeCoefficient`, `finishBonus`) : les temps par section servent
  à régler les temps du bonus.

Limites : `ScoreBridge.award` de KalGames ne prend que des entiers ; les points sont crédités directement dans
KG_ScoreBoards (comme la course de bateau), la passerelle vers le datapack n'est donc plus appelée pour le Parcours.
Le cumul du cahier des charges (ses points + ceux des joueurs en dessous) n'est pas fait. Le temps de l'arrivée est un
réglage du jeu, pas de l'arène. En entraînement, rien n'est affiché ni compté ; les points restent inscrits au journal
comme « non comptés » (comme avant). Nouveaux textes : `race.checkpoint-score`, `race.finish-score`,
`race.result-chrono-out-points` (`race.result-line-points` et `race.result-dnf-points` sont ceux de la course de bateau).

**Compilé le 04/10/2026, non déployé. Statut : non testé en jeu.**
