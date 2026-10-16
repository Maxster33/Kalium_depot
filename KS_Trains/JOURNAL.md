# KS_Trains - journal

Plugin de la survie (Kixster) : trains de wagonnets. Dépend de KLM_Menu (menus, textes) ; recette de la locomotive
dans KS_Crafts (1.16.0). Cahier des charges validé par LeKiwi06 le 16/10/2026 (gardé en local, publié dans
`KS_Trains/CAHIER_DES_CHARGES.md` au déploiement) ; le plugin est fait en 3 étapes.

## 1.0.0 - étape 1 : locomotive, attelage, déplacement, carburant, menu (16/10/2026, LeKiwi06)

Demande de LeKiwi06 : « fait un système pour créer de longs trains en faisant en sorte qu'ils se suivent de façon
fluide » (demande complète et réponses dans le cahier des charges). LeKiwi06 : « je valide le cahier, commence l'étape
1 ».

- **Locomotive** : wagonnet à four vanilla brillant, « Locomotive ». Recette (KS_Crafts 1.16.0) : 3 wagonnets à four
  sur une même ligne. Clic droit sur un rail : **3 wagonnets à four attelés**, posés sur le rail visé et les 2 rails
  derrière lui, tournés vers l'axe le plus proche du regard. Refusé si ce ne sont pas 3 rails droits dans ce sens, ou
  s'il y a déjà un wagonnet. Un distributeur ne la pose pas.
- **Atteler** : chaîne (de fer ou de cuivre) en main, clic droit sur un wagon, puis sur un autre wagon à moins de
  2,5 blocs (15 s pour le faire). Les deux doivent être sur des rails, à l'arrêt, chacun au bout de son train (ou
  seul). Un wagon seul se met du côté où il est attelé ; deux trains sont joints bout à bout. La chaîne est consommée.
  Wagons acceptés : wagonnet simple, à coffre, à entonnoir, à four.
- **Dételer** : cisailles, clic droit sur un wagon : il est séparé du wagon de devant (celui de tête : du suivant),
  train arrêté seulement ; une chaîne de fer tombe. Ou « Dételer ici » dans la page du wagon (menu du train). Un
  morceau d'un seul wagon redevient un wagonnet ordinaire.
- **Protection** : un wagon attelé ne se casse pas (message : le dételer d'abord) ; les wagons d'un train ne sont ni
  poussés ni ne poussent.
- **Rouler** : la tête est poussée le long des rails par la physique du jeu ; **chaque autre wagon est placé à chaque
  tick sur le trajet exact de la tête**, à `espacement` (1,1) bloc du précédent : poussé vers sa place, ou replacé
  s'il en est à plus de 1,5 bloc. Vitesse maximale : `vitesse-max` (1 bloc/tick) tant que la traction suffit,
  `perte-par-wagon` (0,1) de moins par wagon en trop. **Traction** : 5 wagons par four alimenté de la locomotive
  (15 pour ses 3 fours), 10 par autre four alimenté. Accélération 0,02 et freinage 0,05 bloc/tick par tick.
- **Arrêts** : bout des rails (la tête revient sur le dernier rail), obstacle (1 s sans avancer), plus de carburant
  ou de traction ; message aux joueurs à moins de 32 blocs ou à bord. À l'arrêt, la tête ne bouge plus.
- **Carburant** (règles 6 et 7 du cahier) : chaque four a une réserve en blocs et une case de carburant. Clic droit
  sur un four du train : petit coffre, la case du milieu reçoit tout carburant (un autre objet n'y entre pas), la
  réserve s'affiche à côté. 1 carburant = sa durée de cuisson / 20 blocs (charbon : 80 ; seau de lave : 1 000, le seau
  vide reste dans la case et la bloque ; barre de blaze : 120 ; planche : 15). Chaque four alimenté consomme la
  distance parcourue par le train. Four alimenté : allumé (fumée), sans poussée propre.
- **Menu du train** (coffre de 6 rangées) : accroupi + clic droit sur un four de la locomotive. Un objet par wagon
  dans l'ordre (four : réserve ; wagonnet : passager), pages de 45 ; en bas : fiche « Traction : n / t wagons »
  (vitesse max, vitesse), **Mettre en marche / Arrêter le train** au centre (il roule vers l'avant), « Fermer ».
  Clic sur un wagon : sa page (« Dételer ici », « Retour »). Le menu ne commande le train qu'à 24 blocs au plus.
- **Chunks** : un train qui roule garde chargés les chunks de ses wagons et celui à 16 blocs devant la tête, jusqu'à
  une minute après son arrêt.
- **Enregistrement** : identifiant du train, rang et taille dans chaque wagon ; un train se reforme quand tous ses
  wagons sont chargés, **arrêté** (aussi après un redémarrage). Un wagon détruit (`/kill`...) coupe le train en deux à
  sa place.
- `config.yml` : `espacement`, `vitesse-max`, `perte-par-wagon`, `acceleration`, `freinage` (valeurs par défaut aussi
  dans le code).

Techniquement nécessaire, signalé :

- **1 bloc/tick demande la nouvelle physique** (règle 5 du cahier) : avec la physique actuelle de Kixster, un wagonnet
  à four ne dépasse pas 0,2 bloc/tick (lu dans le code du jeu 26.3 : moitié de la vitesse normale de 0,4). Le plugin
  marche avec les deux physiques ; activer l'expérience `minecart_improvements` et `max_minecart_speed` 40 se fait au
  déploiement, avec LeKiwi06 (serveur arrêté).
- Durées de cuisson : en 26.x elles ne sont pas lisibles par l'API de Paper ; le plugin a sa table des carburants du
  jeu (un carburant absent de la table vaut 300 ticks, la plupart sont des objets en bois).
- Pose de la locomotive après KS_Claim (`softdepend`), qui rend la pose des wagonnets possible partout : sans cela,
  KS_Claim laisserait le jeu poser un wagonnet à four en plus.
- **Wagon verrouillé** (KS_VerrouVehicule) : refusé à l'attelage pour l'instant (son ancrage le ramènerait à sa place) ;
  prévu à l'étape 3 avec KS_VerrouVehicule 1.1.0.

Choix de Claude, signalés :

- Le dételage rend toujours une chaîne de fer (même si l'attelage avait été fait avec une chaîne de cuivre).
- Un train qui se reforme (chunk rechargé, redémarrage) est arrêté : il faut le remettre en marche.
- Un wagon d'un train dont une partie est dans un chunk non chargé (train arrêté depuis plus d'une minute) redevient
  un wagonnet ordinaire le temps que le reste soit chargé.
- Rails de propulsion éteints : le train les traverse (la tête est poussée par le plugin) ; un wagon freiné est replacé.

Limites connues : pas encore de wagon contrôleur (étape 2 : accélérer, freiner, marche arrière), ni d'entonnoirs et de
réglages des coffres (étape 3). Rendu des trains à 20 blocs/s (Java et Bedrock) et seuils (1,5 bloc, 0,05 bloc) écrits
sans test en jeu.

Écrit sans serveur de test. Compilé le 16/10/2026 (`sortie/KS_Trains-1.0.0.jar`). **À déployer avec KS_Crafts
1.16.0**, et la nouvelle physique sur le monde de Kixster pour 1 bloc/tick. **Statut : non déployé, non testé en jeu.**
