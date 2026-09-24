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
**Déploiement** : avec KalGames 1.17.0, sur Kal-Games (7001). **Statut : déployé le 24/09/2026 à 19 h 47, testé et confirmé par LeKiwi06 le 24/09/2026 (course de bateau, journal de démarrage : mini-jeu boatrace rattaché).**

## 1.1.0 — étape 1 : vitesse, seuil des meilleurs temps, passages aux checkpoints (24/09/2026)

**Demandes de LeKiwi06** (cahier des charges, étape 1).
- **Vitesse en km/h** dans la barre d'action (`race.status-speed`), calculée à partir du déplacement horizontal
  (1 bloc = 1 m), lissée ; ignorée après une téléportation.
- **Seuil des meilleurs temps** : seuls les tours de 45 s ou moins entrent dans les meilleurs temps (nouveau réglage
  « Meilleurs temps : tours enregistrés jusqu'à (s) », `record-max-lap-seconds`, 0 = tous). Un tour plus long est
  annoncé « non enregistré ». Clé absente des configs existantes : 45 par défaut.
- **Passages aux checkpoints mesurés** : pour chaque checkpoint et la ligne d'arrivée, temps depuis le début du tour,
  place au passage et vitesse d'entrée ; le tour précédent est gardé. Pas encore affichés ni enregistrés : base du
  classement en direct et des écarts (étape 2) et des données envoyées à KG_ScoreBoards (à faire, KG_ScoreBoards à
  réserver).
- Placement des checkpoints : nouvel éditeur de KalGames 1.18.0.
**Déploiement** : avec KalGames 1.18.0. **Statut : déployé sur Kal-Games le 24/09/2026 à 20 h 07 (1.0.0 dans `_removed-kg_boatrace-1.0.0/`), testé et confirmé par LeKiwi06 le 24/09/2026 : vitesse cohérente, tour de plus de 45 s « non enregistré », checkpoints.**

## 1.2.0 — étape 2 : classement en direct, écarts, données dans KG_ScoreBoards (24/09/2026)

**Demandes de LeKiwi06** (cahier des charges, étape 2 et données de l'étape 1).
- **Classement en direct** : tableau latéral propre à la course (mis à jour chaque seconde) : position, pseudo (vert
  si arrivé), écart avec le premier au dernier point de passage commun, « +N t » pour les tours de retard, temps final
  des arrivés. Titre : tour du premier / nombre de tours. Réglage « Classement en direct (tableau latéral) »
  (`live-ranking`, activé par défaut). Le tableau que le joueur avait avant lui est rendu à la fin, à la sortie de la
  partie et à la fermeture.
- **Écarts à chaque checkpoint et à la ligne d'arrivée** (barre d'action, 2,5 s) : place au passage (P1 « en tête »),
  écart avec le coureur passé juste avant, et écart avec son propre passage au tour précédent (vert = plus rapide).
- **Données envoyées au journal de KG_ScoreBoards** (`journal/<aaaa-mm>/<mini-jeu>.jsonl`) :
  - `lap` à chaque tour : `match` (identifiant unique de la course), `arena`, `public`, `player`, `name`, `platform`
    (`java` / `bedrock`), `ranked`, `lap`, `laps`, `lapMillis`, `recorded` (sous le seuil des meilleurs temps),
    `checkpoints`, `splits` (pour chaque passage : `cp`, `ms` depuis le début du tour, `place`, `kmh` d'entrée ; le
    dernier = ligne d'arrivée) ;
  - `race` en fin de course : `match`, `arena`, `public`, `laps`, `checkpoints`, `results` (`player`, `name`,
    `platform`, `rank`, `finished`, `totalMillis`, `lapsDone`).
  Servira aussi à mesurer l'écart Java / Bedrock (voir cahier des charges).
**Déploiement** : **avec KG_ScoreBoards 1.3.0** (obligatoire) et KalGames 1.18.0. **Statut : compilé, non déployé, non
testé en jeu.**

## 1.3.0 — barème, hors-piste, anti-collision, tableau aéré (24/09/2026)

**Demandes de LeKiwi06** : « fais le barème de point et le hors piste, et surtout l'anticollision des bateaux » ;
tableau latéral « un peu trop compact à 2 joueurs » (proposition validée : lignes vides + meilleur tour).
- **Barème** (remplace les points du podium et par checkpoint, réglages `points-win` / `points-checkpoint` retirés) :
  par tour, d'abord les points (1 par tour, +1 sans hors-piste, +n à chaque série de 3 tours propres d'affilée,
  n = numéro de la série, remis à zéro par un hors-piste), puis les multiplicateurs **additifs** : chrono (moins de
  45 s x1,5, 40 s x2, 35 s x3, 30 s x5) et tour en tête (tous les checkpoints du tour passés 1er, x1,5). Grand Prix
  (course de 40 tours terminée) : x1,5 sur le total. Tout est réglable dans les réglages de la course (coefficients
  en dixièmes : 15 = x1,5). Détail affiché au joueur à chaque tour.
- **Fin de course** : classement au temps avec les points entre parenthèses, puis classement aux points avec le cumul
  (ses points + ceux de tous les joueurs en dessous) ; c'est le cumul qui est crédité (points décimaux,
  KG_ScoreBoards 1.4.0), pour les joueurs classés hors opérateurs.
- **Hors-piste** : le bateau touche, sous lui ou sur ses côtés (mur, bordure), un bloc autre qu'un bloc de piste
  (réglage `track-blocks`, « PACKED_ICE,BLUE_ICE » par défaut) ; en l'air, rien n'est compté. Une fois par tour :
  message, son, tour non « propre ». Attention : un marquage au sol d'un autre bloc (tapis, neige...) compte comme
  hors-piste s'il n'est pas ajouté à la liste.
- **Anti-collision** (`CollisionShield`, réglage `anti-collision`, activé) : pour chaque coureur Java, les vrais
  bateaux et pilotes adverses sont cachés et remplacés par une copie sans collision (coque en dalle du bois du bateau,
  tête du joueur, pseudo) qui suit leur position. Joueurs Bedrock : voient les vrais bateaux (à tester). Protection
  levée pour un coureur arrivé ou sorti, et pour tous en fin de course. Limite : un adversaire caché disparaît aussi
  de la liste Tab pendant la course.
- **Tableau latéral** : lignes vides autour des positions (10 au plus) et « Meilleur tour » de la course en cours.
- Journal : `lap` gagne `points`, `base`, `multiplier`, `clean`, `seriesBonus`, `lead`, `timeCoefficient` ; les
  résultats de `race` gagnent `points`, `cumulative`, `offTracks`.
**Déploiement** : **avec KG_ScoreBoards 1.4.0** (obligatoire). **Statut : compilé, non déployé, non testé en jeu.**
