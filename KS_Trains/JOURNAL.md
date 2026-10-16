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

**Déployé sur Event le 16/10/2026 à 20:00 (LeKiwi06 : « envoie tout sur event je vais faire les tests là-bas » ; envoi groupé : KS_VerrouVehicule 1.0.0, KS_LaisseBateau 1.0.0, KS_Trains 1.0.0, KS_Crafts 1.16.0 ; KS_Crafts 1.14.1 en place vérifié identique à la référence avant l'envoi, dans `_removed-ks_crafts-1.14.1/` ; jars du serveur vérifiés identiques à ceux compilés après l'envoi), actif après redémarrage d'Event. Nouvelle physique des wagonnets pas encore activée sur Event (trains limités à 0,2 bloc/tick). Kixster n'est pas touché. Statut : non testé en jeu.**

## 1.1.0 - étape 2 : wagon contrôleur, niveaux de vitesse (16/10/2026, LeKiwi06)

LeKiwi06 : « envoie tout sur event je vais faire les tests là-bas, et continue l'étape suivante ». Règle 11 du cahier :
wagon contrôleur pour accélérer, freiner et faire marche arrière (1 bloc/s au plus), un par train, à moins de 5
wagons de la locomotive. Empilé sur la 1.0.0 non testée, à sa demande.

Revu avant tout déploiement (16/10/2026), demande de LeKiwi06 : « il faut que le train aille en marche arrière si on
recule alors qu'il est arrêté, et il faut que le train garde sa vitesse si on descend alors qu'il n'est pas arrêté,
rajoute également un système de vitesse de 1 à 8 : chaque niveau de vitesse est égal à 5 blocs par seconde, avec des
accélérations fluides entre ces vitesses stables ». Ses réponses : plafond de 20 blocs/s gardé (les niveaux 5 à 8 ne
le dépassent pas ; `max_minecart_speed` reste à 40), un appui = un niveau sans répétition, le bouton « Mettre en
marche » du menu lance le niveau 1. La 1.1.0 n'ayant été déployée nulle part, elle est modifiée sans nouvelle version.

- **Objet** : wagonnet vanilla brillant, « Wagon contrôleur », 4 lignes de description. Recette dans KS_Crafts 1.17.0.
  Clic droit sur un rail : un wagonnet marqué (comme un wagonnet du jeu). Cassé (dételé), il redonne un wagon
  contrôleur. Un distributeur ne le pose pas.
- **Attelage** : refusé s'il y aurait 2 wagons contrôleurs dans le train, ou si le contrôleur serait à 5 wagons ou
  plus du four de locomotive le plus proche (écart de rang de 4 au plus). Un train sans locomotive n'a pas d'écart à
  respecter.
- **Niveaux de vitesse** : 0 arrêt ; 1 à 8 en avant, **5 blocs/s par niveau** (`palier` 0,25 bloc/tick), plafonnés par
  la vitesse maximale du train (`vitesse-max` 1 bloc/tick = 20 blocs/s, moins 0,1 par wagon en trop) ; -1 **marche
  arrière** à 1 bloc/s (`marche-arriere-max`). Entre deux niveaux, le train accélère ou freine en douceur
  (`acceleration` 0,02 et `freinage` 0,05 bloc/tick par tick).
- **Conduite** : un joueur monte dans le contrôleur (clic droit). **Un appui sur avancer** : niveau +1 ; **un appui sur
  reculer** : niveau -1 ; **à l'arrêt, reculer passe en marche arrière** (le train repart vers l'arrière) ; depuis la
  marche arrière, avancer revient à l'arrêt. Tenir la touche ne répète pas. Au-dessus de la barre d'objets du
  conducteur : niveau, vitesse voulue et actuelle en blocs/s (« plafonné à … » si la traction ne suffit pas), à chaque
  appui et toutes les demi-secondes.
- **Conducteur qui descend** : le train **garde son niveau** et continue (arrêt par le menu, le bout des rails, un
  obstacle ou le carburant).
- **Marche arrière** : le train freine jusqu'à l'arrêt, puis le **dernier wagon mène** : il est poussé le long des
  rails et les autres wagons suivent son trajet, comme en marche avant. Mêmes arrêts qu'en marche avant.
- **Menu du train** : aussi par accroupi + clic droit sur le wagon contrôleur ; le contrôleur y apparaît (comparateur)
  avec son conducteur ; la fiche du bas donne le niveau et les vitesses en blocs/s. « Mettre en marche » : niveau 1
  (5 blocs/s, vers l'avant) ; « Arrêter le train » : niveau 0. Refusé pendant qu'un joueur conduit (message).
- `config.yml` : nouvelles clés `palier` (0,25) et `marche-arriere-max` (0,05) ; valeurs par défaut aussi dans le code
  (non ajoutées au `config.yml` d'un serveur qui en a déjà un).

Choix de Claude, signalés :

- La marche arrière demande aussi de la traction (carburant) ; sans traction, le train s'arrête et repasse au niveau 0.
- Un train qui se reforme (chunk rechargé, redémarrage) est au niveau 0.

Écrit sans serveur de test (touches reçues à bord d'un wagonnet sur Java et Bedrock à vérifier). Compilé le 16/10/2026
(`sortie/KS_Trains-1.1.0.jar`). **À déployer avec KS_Crafts 1.17.0. Statut : non déployé, non testé en jeu.**
