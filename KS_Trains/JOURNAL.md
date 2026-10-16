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

Puis (même jour, toujours avant déploiement), LeKiwi06 : « la vitesse de croisière est la vitesse 2, en vitesse 1 on
consomme légèrement moins de carburant, mais au niveau 3 et 4 c'est respectivement +25 % ou +50 % de consommation de
carburant pour une même distance ». Ses réponses : niveau 1 à -25 %, **4 niveaux seulement** (les niveaux 5 à 8 ne
dépassaient pas le plafond de 20 blocs/s), consommation selon le **niveau choisi** (pas la vitesse réelle), bouton
« Mettre en marche » toujours au niveau 1.

- **Objet** : wagonnet vanilla brillant, « Wagon contrôleur », 4 lignes de description. Recette dans KS_Crafts 1.17.0.
  Clic droit sur un rail : un wagonnet marqué (comme un wagonnet du jeu). Cassé (dételé), il redonne un wagon
  contrôleur. Un distributeur ne le pose pas.
- **Attelage** : refusé s'il y aurait 2 wagons contrôleurs dans le train, ou si le contrôleur serait à 5 wagons ou
  plus du four de locomotive le plus proche (écart de rang de 4 au plus). Un train sans locomotive n'a pas d'écart à
  respecter.
- **Niveaux de vitesse** : 0 arrêt ; **1 à 4** en avant, **5 blocs/s par niveau** (`palier` 0,25 bloc/tick : 5, 10,
  15, 20 blocs/s), plafonnés par la vitesse maximale du train (`vitesse-max` 1 bloc/tick = 20 blocs/s, moins 0,1 par
  wagon en trop) ; -1 **marche arrière** à 1 bloc/s (`marche-arriere-max`). Entre deux niveaux, le train accélère ou
  freine en douceur (`acceleration` 0,02 et `freinage` 0,05 bloc/tick par tick).
- **Consommation selon le niveau choisi** : le **niveau 2 est la vitesse de croisière** (1 charbon = 80 blocs) ; par
  bloc parcouru, chaque four brûle x0,75 au niveau 1 (1 charbon ≈ 107 blocs), x1,25 au niveau 3 (64 blocs), x1,5 au
  niveau 4 (≈ 53 blocs). Réglable : `consommation.niveau-1` à `niveau-4`.
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
- `config.yml` : nouvelles clés `palier` (0,25), `marche-arriere-max` (0,05) et `consommation.niveau-1` à `niveau-4` (0,75 / 1 / 1,25 / 1,5) ; valeurs par défaut aussi dans le code
  (non ajoutées au `config.yml` d'un serveur qui en a déjà un).

Choix de Claude, signalés :

- La marche arrière demande aussi de la traction (carburant) ; sans traction, le train s'arrête et repasse au niveau 0.
- Marche arrière, et freinage jusqu'à l'arrêt (niveau 0 demandé, train encore en mouvement) : consommation du
  niveau 1.
- Un train qui se reforme (chunk rechargé, redémarrage) est au niveau 0.

Écrit sans serveur de test (touches reçues à bord d'un wagonnet sur Java et Bedrock à vérifier). Compilé le 16/10/2026
(`sortie/KS_Trains-1.1.0.jar`). **À déployer avec KS_Crafts 1.17.0. Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 16/10/2026 à 20:40 (LeKiwi06 : « envoie sur event et continue l'étape 3 » ; envoi groupé : KS_Trains 1.1.0, KS_Crafts 1.17.0 ; jars en place (KS_Trains 1.0.0, KS_Crafts 1.16.0) vérifiés identiques aux références avant l'envoi, rangés dans `_removed-ks_trains-1.0.0/` et `_removed-ks_crafts-1.16.0/` ; jars du serveur vérifiés identiques à ceux compilés ; Event n'avait pas redémarré depuis l'envoi de 20:00 : la 1.0.0 n'y a jamais tourné et le `config.yml` de KS_Trains sera créé avec les clés de la 1.1.0), actif après redémarrage d'Event. Nouvelle physique des wagonnets pas encore activée. Statut : non testé en jeu.**

## 1.2.0 - étape 3 : wagons à entonnoir, réglages des coffres, wagons verrouillés (16/10/2026, LeKiwi06)

LeKiwi06 : « envoie sur event et continue l'étape 3 ». Règles 8 et 9 du cahier (wagon à entonnoir, entrée / sortie des
wagons à coffre) et wagons verrouillés de KS_VerrouVehicule. Empilé sur la 1.1.0 non testée, à sa demande.

- **Wagon à entonnoir** (dans un train) : tous les 8 ticks (rythme d'un entonnoir du jeu), il **prend un objet** dans
  les 3 wagons derrière lui, le plus proche d'abord (coffres dont la sortie est autorisée, autres entonnoirs), et en
  **donne un** aux 3 wagons devant lui, le plus proche d'abord (fours : carburant seulement, dans leur case, si elle est
  vide ou a le même carburant avec de la place ; coffres dont l'entrée est autorisée ; autres entonnoirs). Devant =
  côté locomotive, même en marche arrière.
- **Seau de lave** donné à un four : accepté seulement si sa réserve est vide ; elle prend 1 000 blocs et le **seau vide
  reste dans l'entonnoir**, qui le donne ensuite à un coffre devant lui dont l'entrée est autorisée.
- Dans un train, l'entonnoir **n'aspire plus** les objets au sol ni les contenants au-dessus de lui.
- **Wagon à coffre** : réglages **Entrée** et **Sortie** (autorisées par défaut), dans sa page du menu du train
  (colorant vert / rouge, clic pour changer). Seuls les entonnoirs du train les respectent ; un entonnoir posé au sol
  fait comme d'habitude.
- **Menu du train** : un coffre affiche son entrée, sa sortie et son verrou ; un entonnoir, le nombre d'objets qu'il
  contient et sa règle.
- **Wagon verrouillé** (KS_VerrouVehicule) : il s'attelle désormais, mais **seulement par le propriétaire du verrou**
  (ou un opérateur, permission `ksverrouvehicule.contourner`), qui est aussi le seul à régler son entrée et sa sortie.
  Il n'est plus ancré quand il est dans un train (KS_VerrouVehicule 1.1.0, à déployer avec).

Choix de Claude, signalés :

- Un entonnoir du train prend aussi dans un coffre verrouillé et protégé si sa sortie est autorisée : seul le
  propriétaire peut l'autoriser.
- Un objet par transfert et par sens (comme un entonnoir du jeu), même si l'inventaire voisin en a plusieurs.

Écrit sans serveur de test. Compilé le 16/10/2026 (`sortie/KS_Trains-1.2.0.jar`). **À déployer avec
KS_VerrouVehicule 1.1.0** (KS_Crafts inchangé, 1.17.0). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 16/10/2026 à 20:46 (LeKiwi06 : « oui envoie sur event » ; envoi groupé : KS_Trains 1.2.0, KS_VerrouVehicule 1.1.0 ; jars en place (KS_Trains 1.1.0, KS_VerrouVehicule 1.0.0) vérifiés identiques aux références avant l'envoi, rangés dans `_removed-ks_trains-1.1.0/` et `_removed-ks_verrouvehicule-1.0.0/` ; jars du serveur vérifiés identiques à ceux compilés ; Event n'avait toujours pas redémarré depuis 20:00 : aucune version précédente n'y a tourné), actif après redémarrage d'Event. Kixster reste en KS_VerrouVehicule 1.0.0 et n'a pas KS_Trains. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 16/10/2026 à 20:50 (journal lu, heures du journal en UTC : 18:50) : KS_Trains 1.2.0, KS_Crafts 1.17.0 (« 81 crafts ajoutés » : les 78 d'avant + verrou, locomotive et wagon contrôleur), KS_VerrouVehicule 1.1.0 et KS_LaisseBateau 1.0.0 chargés et activés sans erreur ni avertissement ; KS_Trains activé après KS_Claim (ordre voulu pour la pose de la locomotive) ; `config.yml` et `lang.yml` créés. Statut : non testé en jeu.**

**Nouvelle physique des wagonnets activée sur Event le 16/10/2026 à 20:56** (LeKiwi06 : « j'ai arrêté event, active la nouvelle physique » ; Event arrêté proprement à 20:53:59, vérifié dans son journal et sur `level.dat` juste avant l'envoi). Dans `world/level.dat` (26.3 : il ne contient plus de liste `enabled_features`), le pack intégré `minecart_improvements` est passé de `DataPacks.Disabled` à `DataPacks.Enabled` ; lu dans le code du jeu 26.3 (MinecraftServer.configurePackRepository) : les fonctions activées sont recalculées au démarrage à partir des packs sélectionnés. Rien d'autre n'a changé dans le fichier (vérifié par relecture). Original sauvegardé dans `/_removed-level-dat-2026-10-16/level.dat` (le remettre en place, serveur arrêté, pour revenir en arrière). Reste à faire par LeKiwi06 après le démarrage : `/gamerule max_minecart_speed 40`. Effet sur tous les wagonnets d'Event.

**Échec de l'activation (journal d'Event lu après le redémarrage du 16/10/2026 à 20:58)** : « Pack minecart_improvements requires features minecraft:minecart_improvements that are not enabled for this world, disabling pack ». Erreur de Claude en lisant le code du jeu : un pack n'est gardé que si ses fonctions sont dans la liste `enabled_features` du monde, qui vaut « vanilla » quand elle est absente de `level.dat` (WorldDataConfiguration, MinecraftServer.configurePackRepository). La nouvelle physique n'est donc pas active ; le `/gamerule max_minecart_speed 40` de LeKiwi06 (21:04) n'a pas pu servir. Correction prête : pack activé **et** `Data.enabled_features = [minecraft:vanilla, minecraft:minecart_improvements]`, à envoyer Event arrêté. Rien d'autre n'a été touché ; plugins chargés sans erreur.

## 1.2.1 - le train ne change plus de sens en joignant deux trains (16/10/2026, LeKiwi06)

Signalement de LeKiwi06 (tests sur Event) : « à un moment mon train a changé de sens en attachant plusieurs wagons ».

- **Cause** (défaut de la 1.0.0) : en joignant deux trains de plusieurs wagons, l'ordre du nouveau train suivait le
  morceau du **premier wagon cliqué**. Exemple : un groupe de wagons attelés entre eux, on clique d'abord son dernier
  wagon, puis le dernier wagon du train de la locomotive : le train de la locomotive était retourné, la locomotive se
  retrouvait à l'arrière et « avant » (le sens de la marche) pointait vers le groupe.
- **Correction** : après un attelage, le nouveau train garde le sens du morceau qui a une **locomotive** (sinon du plus
  long des deux). Un wagon seul se met toujours du côté où il est attelé.
- Un train déjà retourné sur Event le reste : le dételer derrière la locomotive (cisailles ou menu) puis le rattacher
  le remet dans le bon sens.

Compilé le 16/10/2026 (`sortie/KS_Trains-1.2.1.jar`). **Statut : non déployé, non testé en jeu.**
