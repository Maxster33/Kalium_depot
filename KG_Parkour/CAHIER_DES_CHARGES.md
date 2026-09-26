# KG_Parkour — cahier des charges (demande de LeKiwi06, 24/09/2026)

Brouillon local, validé point par point avec LeKiwi06. À relire avant chaque étape.

Le Parkour sort de KalGames dans ce plugin séparé (`REGLES.md`, règle 2.2), comme la course de bateau (KG_BoatRace).
Point de départ : KalGames 1.16.0 + la migration de Maxster33 du 24/09/2026 (aucun changement de code). Déploiement
sur le hub des mini-jeux `kal-games` = machine 7001 (ex KalGames2), comme KG_BoatRace.

**Étape 0 (faite le 26/09/2026, KG_Parkour 1.0.0 + KalGames 1.20.0)** : le Parcours sort de KalGames tel quel ; les
règles ci-dessous restent à faire.

**KG_Parkour 1.1.0 (26/09/2026)** : chrono points 1 et 2 (réglages par jeu, pas encore par checkpoint ni par map),
anti-collision, et en plus (demande de LeKiwi06) adversaires vus sous forme de bottes en cuir colorées à moins de 3 blocs.

## Chrono et fin de partie

1. Chrono de départ : 30 s. Chaque checkpoint atteint AJOUTE du temps : CP 1 à 6 : +30 s ; CP 7 et suivants : +60 s
   (valeurs par défaut, réglables checkpoint par checkpoint dans un panneau admin, comme pour la course de bateau).
   **Une configuration par map** : chaque parcours est unique (nombre de checkpoints, longueurs...) ; chrono de
   départ, temps ajouté, difficulté et palier de temps se règlent pour chaque checkpoint de chaque map. Les valeurs
   ci-dessus ne sont que les valeurs par défaut d'un nouveau checkpoint (difficulté selon sa position).
2. Chrono à zéro = le joueur « tombe au temps » : il passe spectateur et regarde les survivants. Il est considéré
   comme ayant fini sa partie (ce n'est PAS un abandon, aucun effet négatif). Il peut relancer une autre partie.
3. La partie se termine quand tous les joueurs ont fini le parcours ou sont tombés au temps.
4. Les points de la partie sont attribués à TOUS à la fin de la partie (le classement cumulé en dépend).

## Barème (même principe pour tous les jeux : d'abord les bonus, puis les multiplicateurs)

| Checkpoints | Difficulté | Points de base | Bonus « 1er à valider » |
|---|---|---|---|
| 1 à 3 | facile | 1 | +1 |
| 4 à 6 | normal | 3 | +2 |
| 7 à la fin | difficile | 5 | +5 |

- **Paliers de temps** : temps entre deux validations de checkpoint ; paliers et valeur du bonus configurables par
  checkpoint dans le panneau admin. LeKiwi06 mesurera d'abord les temps réels (first try, temps moyen par
  checkpoint) pour fixer les valeurs : le code prévoit la saisie ; tant qu'aucun palier n'est saisi, pas de bonus de
  temps. Les temps mesurés par KG_ScoreBoards (moyennes par checkpoint) serviront à régler ces paliers.
- **First try** : passer d'un checkpoint au suivant sans respawn : ×2 sur ce checkpoint.
- Points décimaux, affichage K / M / Md (comme partout).

## Classements de fin de partie

- Classement au **chrono global** (ordre d'arrivée).
- Classement aux **points** : trié par points individuels ; entre parenthèses le cumul (ses points + ceux de tous
  les joueurs en dessous dans CE classement aux points). On ne récompense pas seulement le plus rapide, mais celui
  qui a le mieux réussi selon les critères du barème (bonus et coefficients). C'est le cumul qui est crédité.

## Solo (contre-la-montre)

- Une partie solo rapporte des points (moins intéressant qu'avec des adversaires, mais sans attendre le matchmaking).
  Même chose pour la course de bateau. L'ancienne règle « les parties solo / privées ne comptent pas » est obsolète
  avec les nouveaux barèmes.
- La limite « 5 parties privées comptées par jour » (`stats.private-daily-limit`, socle KalGames) est supprimée pour
  TOUS les jeux : obsolète (décision de LeKiwi06 ; PvP Kit et Rush seront retravaillés bientôt).
- Le mode entraînement actuel (partie privée sans chrono ni points) est REMPLACÉ par le contre-la-montre avec points.
- **Fantôme** en contre-la-montre, comme pour la course de bateau : piste retenue, l'entité « Mannequin » de Paper
  (silhouette de joueur avec son skin, disponible dans notre version) ; à défaut un porte-armure avec sa tête.
  Sans collision. À tester sur Bedrock.
- Plus tard (autre session) : KG_AntiCheat, qui inspectera les classements à la recherche d'anomalies et les triches
  classiques.

## Données

- Temps de chaque checkpoint et toutes les données de partie enregistrés dans **KG_ScoreBoards**, comme tous les
  jeux : affichage en jeu, et plus tard une seule communication KG_ScoreBoards ↔ bot Discord, avec des données
  centralisées, claires et structurées.

## Anti-collision

- Pas de collision entre joueurs (règle de collision par équipe de Minecraft). À tester avec Bedrock, puis aviser.

## Interface admin

- Placement des checkpoints et panneau de réglage par checkpoint (temps ajouté, difficulté, palier de temps) : partie
  commune avec KG_BoatRace.
