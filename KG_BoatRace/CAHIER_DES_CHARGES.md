# KG_BoatRace — cahier des charges (demande de LeKiwi06, 24/09/2026)

Brouillon local, validé point par point avec LeKiwi06. À relire avant chaque étape.

La course de bateau sort de KalGames dans ce plugin séparé (`REGLES.md`, règle 2.2). Point de départ : le code de
KalGames 1.16.0 (compilé, pas encore déployé ni testé ; accord de LeKiwi06).

**Déploiement** : sur le hub des mini-jeux `kal-games` = machine 7001 (ex KalGames2, migration de Maxster33 du
24/09/2026), qui tourne aujourd'hui KalGames 1.15.1 : la sortie de KG_BoatRace implique d'y déployer aussi KalGames
1.16.0+ et les plugins qui l'accompagnent (KG_Menu 1.0.0, KLM_Menu 2.1.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0+).

## Fonctionnalités

1. **Mode Grand Prix** : 40 tours, disponible en partie privée ET publique, mis en avant (« spécial ») dans
   l'interface du jeu. Terminé en entier : ×1,5 sur le total des points de la course.
2. **Fantôme** du meilleur tour de chaque joueur : il rejoue ce tour tant que le joueur ne l'a pas battu ; un
   nouveau meilleur tour remplace le fantôme dès le tour suivant, avec une annonce dans le tchat.
3. **Contre-la-montre solo** contre son propre fantôme. Le joueur ne part pas de la place qu'occupait le fantôme
   dans la partie enregistrée.
4. **Seuil d'enregistrement** : seuls les tours de 45 s ou moins sont enregistrés (meilleurs temps et fantôme).
   Réglable dans le menu admin.
5. **Collisions entre bateaux** : à empêcher. Piste retenue, à tester (vidéos Boatlabs étudiées le 24/09/2026) :
   pour chaque joueur, cacher les vrais bateaux des adversaires et afficher à la place une copie visuelle (entité
   d'affichage, sans collision, avec le pseudo) qui suit leur position à chaque tick. Même technique que le fantôme.
   Tests à prévoir : Java contre Java, Java contre Bedrock (Geyser), Bedrock contre Bedrock (ce que voit un joueur
   Bedrock, et s'il heurte quelque chose). Les entités d'affichage ne sont jamais comptées comme hors-piste (le
   hors-piste ne regarde que les blocs du monde).
6. **Checkpoints** : LeKiwi06 les pose lui-même ; il faut une interface admin pour les placer (plus nombreux).
7. **Classement en temps réel** pendant la course.
8. **Écarts de temps à chaque checkpoint** : les DEUX, écart avec le coureur juste devant à ce checkpoint (comme
   en F1) ET écart avec son propre passage au tour précédent. En mode fantôme : écart avec le fantôme.
9. **Temps de chaque checkpoint** enregistrés (pas seulement le tour), avec le classement au passage : envoyés à
   KG_ScoreBoards, un fichier par mois, une ligne par tour (données pour de futurs graphiques / bot Discord).
10. **Vitesse du bateau en km/h** affichée.

## Barème de points (tour par tour)

1. On additionne d'abord les points du tour :
   - 1 point par tour ;
   - +1 si le tour est sans hors-piste (**+0,5 en solo depuis la 1.5.1**, LeKiwi06, 05/10/2026) ;
   - série de 3 tours consécutifs sans hors-piste : +n (n = numéro de la série validée d'affilée). Après une
     validation, il faut 3 nouveaux tours complets sans hors-piste pour la suivante ; un hors-piste remet n à 1.
2. Puis on applique la somme des multiplicateurs obtenus (coefficients ADDITIFS : ×1,5 et ×1,5 = ×2) :
   - temps du tour : > 45 s aucun bonus ; 45-40 s ×1,5 ; 40-35 s ×2 ; 35-30 s ×3 ; < 30 s ×5 ;
   - tour en tête (tous les checkpoints du tour passés en étant 1er) : ×1,5 (**jamais en solo depuis la 1.5.1**).
   Paliers de temps et coefficients réglables dans le menu admin de la course.
3. Grand Prix de 40 tours terminé en entier : ×1,5 sur le total de la course.
4. Points décimaux autorisés ; affichage au-delà de 1 000 en K / M / Md (KG_ScoreBoards à mettre à jour).
5. Résultats de la course (même règle que le Parkour, choix laissé à Claude le 24/09/2026, modifiable) : deux
   classements, au TEMPS (ordre d'arrivée) et aux POINTS (trié par points individuels ; entre parenthèses le cumul
   = ses points + ceux de tous les joueurs en dessous, c'est le cumul qui est crédité).
   **Depuis la 1.5.0 (LeKiwi06, 04/10/2026)** : ses points + la MOYENNE des points des joueurs en dessous.
6. Limite « 5 parties privées comptées par jour » supprimée (obsolète).

**Hors-piste** : dès que le bateau touche un bloc autre que de la glace compacte ou de la glace bleue d'une façon
qui le freine ou le ralentit (sol, mur, bordure). Le bateau ne doit glisser que sur ces deux glaces pour valider
un tour sans hors-piste. Pas de retour au checkpoint : seulement la perte des bonus concernés.

## Équité Java / Bedrock (constaté par LeKiwi06 le 24/09/2026)

- Les joueurs Bedrock (Geyser) ont une grosse accélération au départ de la course. Précisé par LeKiwi06 : ils
  **accélèrent plus vite** et **conservent mieux leur vitesse dans les virages**, pour une vitesse maximale
  similaire. Un plafond de vitesse ne suffit donc pas.
- À MESURER d'abord (vitesse en km/h, temps par checkpoint et par tour, avec la plateforme du joueur : Java ou
  Bedrock via Floodgate), avant de corriger.
- **Mesuré le 08/10/2026** (journal de KG_ScoreBoards, 24/09 au 08/10, arène `non_alcoholic_pigeons`, 1 781 tours ;
  tours lancés de moins de 60 s) :

  | | Java (6 joueurs, 303 tours) | Bedrock (6 joueurs, 1 300 tours) |
  |---|---|---|
  | Meilleur tour | 39,50 s | 32,90 s |
  | Tour médian | 45,0 s | 39,8 s |
  | Tours propres | 47 % | 74 % |
  | Points par tour (moyenne) | 2,36 | 4,99 |
  | Tours sous 40 s (palier x2 ou mieux) | 1 % | 54 % |
  | Départ jusqu'au 1er checkpoint | 4,6 s | 3,9 s |

  Tous les tronçons sont plus rapides en Bedrock (5 à 18 % sur le meilleur quart des tours de chaque plateforme) et la
  vitesse d'entrée aux checkpoints est plus haute partout (+8 à +29 km/h ; 190 contre 160 km/h en bout de ligne
  droite) : la « vitesse maximale similaire » notée plus haut est fausse. Réserve : un joueur a fait 949 des 1 300 tours
  Bedrock ; sans lui il reste 3,2 s sur le meilleur tour et 1,5 s sur le médian. L'avantage réel est donc entre 3 et 6 s
  par tour d'environ 40 s. À refaire à pilote égal (une dizaine de tours de LeKiwi06 en Bedrock).
- **Cause** : en bateau, c'est le jeu du joueur qui calcule le mouvement ; un joueur Bedrock roule avec la physique de
  Bedrock, que Geyser transmet telle quelle.
- **Choix de LeKiwi06 (08/10/2026)** : bateau calculé par le serveur pour les joueurs Bedrock, en essai (1.6.0, réglage
  `bedrock-server-boat` désactivé par défaut). Repli si la conduite est mauvaise : records séparés Java / Bedrock et
  paliers de chrono décalés pour Bedrock (piste 3 ci-dessous).
- Pistes, à tester dans cet ordre : (1) bateau posé et bloqué pendant le compte à rebours, relâché pour tous en même
  temps ; (2) limiter côté serveur, pour les joueurs Bedrock seulement, le gain de vitesse par tick (accélération)
  et la vitesse gardée en virage - risque d'effet « élastique » à évaluer en jeu ; (3) en dernier recours,
  classements séparés Java / Bedrock, ou plateforme affichée dans les classements.

## Solo (ajout du 24/09/2026)

- Une partie solo (contre-la-montre) rapporte des points, comme en Parkour : l'ancienne règle « les parties solo /
  privées ne comptent pas » est obsolète avec les nouveaux barèmes. Anomalies : futur KG_AntiCheat.

## Abandon et déconnexion

- Abandon possible sans conséquence ; les autres finissent leur course.
- Déconnexion (≠ abandon) : le joueur peut revenir et continuer. Il repart du dernier checkpoint avec la
  position, l'angle et la vitesse enregistrés à son entrée dans ce checkpoint.
- Pas revenu au bout de 5 minutes : abandon définitif.

## Étapes prévues

0. Sortir la course de bateau de KalGames dans KG_BoatRace (+ nouvelle version de KalGames).
1. Base : vitesse en km/h, interface de placement des checkpoints, temps par checkpoint, seuil de 45 s.
2. Course en direct : classement en temps réel, écarts aux checkpoints.
3. Barème : détection du hors-piste, points (+ KG_ScoreBoards : décimales, K / M / Md, données par checkpoint).
4. Abandon et déconnexion (reprise dans les mêmes conditions, 5 minutes).
5. Mode Grand Prix.
6. Fantôme et contre-la-montre solo.
7. Collisions (selon la vidéo).
