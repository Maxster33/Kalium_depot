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

## 1.3.0 - collisions, écartement, entonnoirs, décor, klaxons, wagon jukebox, boussole (16/10/2026, LeKiwi06)

Demandes de LeKiwi06 après ses premiers tests sur Event :

> j'aimerais que les wagonnets soient un peu plus gros, le train a l'air minuscule
> j'ai aussi remarqué qu'un wagon normal traverse le train, il faudrait des collisions
> j'aimerais que tu écartes légèrement les wagons aussi, car quand je les décroche ils glitchent et se mettent à
> rouler à l'infini
> le minecart hopper ne transmettrait pas bien le charbon dans les 3 minecarts de la locomotive également, il faut
> régler ce problème, il faudrait aussi un conteneur sur le minecart contrôleur pour y mettre les cornes de chèvre,
> histoire d'avoir des klaxons,
> il faut aussi créer le minecart jukebox, qui peut récupérer et reposer les disques dans les minecarts coffres devant
> lui (le plus proche de la loco, pas de l'arrière du train)
> il faut aussi faire en sorte que le minecart contrôleur marche comme une lodestone si on clique avec une boussole
> dessus, pour pouvoir retrouver le train facilement

Ses réponses : le jeu ne grossit pas un wagonnet ; « que le four central de la locomotive émette des particules de feu
de camp […] mais oui grandis-le un peu pour les Java » ; klaxons : 9 cases, corne choisie ; wagon jukebox : le coffre
juste devant lui uniquement, modes lecture, pause et aléatoire ; boussole : elle suit le train ; maquette des deux
interfaces validée ; recette du wagon jukebox : wagonnet + jukebox. Contient la 1.2.1 (jamais déployée).

- **Collisions** (la 1.0.0 supprimait toute collision avec un wagon de train) : un wagonnet libre, un joueur ou un
  animal touché par le train est **repoussé** (plus fort si le train roule vite) ; deux trains qui se heurtent :
  celui qui roule **s'arrête** (« un autre train est sur la voie »). Les wagons d'un même train, leurs passagers et le
  décor ne comptent pas.
- **Écartement** : `espacement` passe de 1,1 à **1,4** bloc (lu dans le code du jeu : deux wagonnets se poussent dès
  que leurs centres sont à moins de 1,38 bloc environ ; à 1,1 les wagons dételés se repoussaient sans fin). Les
  wagons dételés sont **arrêtés net**. La locomotive se pose maintenant sur **4 rails droits** (le message donne le
  nombre).
- **Entonnoirs** : le carburant va d'abord aux fours, **le moins rempli d'abord** (avant : le plus proche, jusqu'à 64) ;
  la **locomotive compte comme un ensemble** : si un de ses fours est à portée (3 wagons devant), ses 3 fours sont
  servis (avant : un contrôleur entre la locomotive et l'entonnoir mettait le premier four hors de portée). Puis un
  objet va au coffre ou à l'entonnoir le plus proche devant, comme avant.
- **Décor (Java seulement, choix de LeKiwi06)** : le chargement des wagons de train (four, allumé s'il est alimenté ;
  coffre ; entonnoir ; jukebox) est affiché par un modèle 3D de bloc plus gros posé dans le wagon (`decor.taille` 1,0
  contre 0,75 pour celui du jeu ; `decor.hauteur` -0,1, à régler en jeu). Les joueurs Bedrock voient les wagonnets du
  jeu. Modèle non enregistré avec le monde, remis chaque seconde tant que le wagon est dans un train. Le **four du milieu
  de la locomotive fume** (fumée de feu de camp) quand il est alimenté.
- **Klaxons** : page du wagon contrôleur dans le menu du train, bouton « Klaxons » : contenant de 2 rangées, 9 cases
  pour des cornes de chèvre (rien d'autre n'y entre), et sous chaque corne une vitre à cliquer pour la choisir (verte :
  choisie). À bord du contrôleur, **touche de saut** : la corne choisie sonne, avec son son, sa portée et son attente
  du jeu. Cornes enregistrées dans le wagon, lâchées s'il est cassé.
- **Wagon jukebox** : objet « Wagon jukebox » (wagonnet brillant ; recette dans KS_Crafts 1.18.0 : wagonnet + jukebox),
  posé comme un wagonnet avec un **jukebox affiché dedans** (bloc du wagonnet, vu aussi sur Bedrock) ; on n'y monte pas.
  Dans un train, il prend un disque dans le **wagon à coffre juste devant lui** (sortie autorisée), le joue (la musique
  sort du wagon et le suit, `jukebox.volume` 4 = 64 blocs) et le **rend au coffre** à la fin du morceau (à sa case si
  elle est libre ; entrée autorisée). Modes : **lecture** (ordre des cases, en boucle), **aléatoire**, **pause** (le
  disque revient) ; clic droit sur le wagon : lecture → aléatoire → pause ; ou dans sa page du menu (disque en cours,
  trois boutons). Rien ne joue sans joueur à portée. Disque qui ne peut pas revenir : gardé dans le wagon, lâché s'il
  est cassé. Cassé, il redonne un wagon jukebox.
- **Boussole du train** : clic droit avec une boussole sur le wagon contrôleur : elle devient « Boussole du train »
  (boussole de magnétite liée au contrôleur). Toutes les 2 s, les boussoles liées des joueurs connectés pointent vers
  la position actuelle du contrôleur (mise à jour s'il a bougé de plus de 8 blocs ; dernière position connue s'il est
  déchargé, gardée dans `controleurs.yml`).
- `config.yml` : `espacement` 1,4 (**à changer à la main sur un serveur qui a déjà 1,1**), nouvelles clés
  `decor.taille`, `decor.hauteur`, `jukebox.volume` (valeurs par défaut aussi dans le code).

Choix de Claude, signalés :

- Ce qui est repoussé : wagonnets libres, joueurs, animaux et monstres (pas seulement les wagonnets).
- Décor sur les wagons à four, coffre, entonnoir et jukebox ; pas sur les wagonnets simples ni le contrôleur (on y
  monte : un modèle prendrait la place du passager).
- Fumée : seulement quand le four du milieu est alimenté (à l'arrêt aussi).
- Wagon jukebox hors d'un train : silencieux. Le mode de départ est « lecture ».
- La boussole est mise à jour dans tout l'inventaire du joueur ; contrôleur cassé : elle garde sa dernière position.
- KS_VerrouVehicule 1.1.1 : un modèle d'affichage n'est plus refusé comme passager d'un véhicule verrouillé (sinon le
  décor d'un coffre verrouillé ne se poserait pas).

Écrit sans serveur de test. Compilé le 16/10/2026 (`sortie/KS_Trains-1.3.0.jar`). **À déployer avec KS_Crafts 1.18.0
et KS_VerrouVehicule 1.1.1. Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 16/10/2026 à 22:18 (LeKiwi06 : « j'ai arrêté event, envoie tout » ; Event arrêté proprement à 22:17:12, vérifié dans son journal ; envoi groupé : KS_Trains 1.3.0, KS_Crafts 1.18.0, KS_VerrouVehicule 1.1.1 ; jars en place (KS_Trains 1.2.0, KS_Crafts 1.17.0, KS_VerrouVehicule 1.1.0) vérifiés identiques aux références avant l'envoi, rangés dans `_removed-ks_trains-1.2.0/`, `_removed-ks_crafts-1.17.0/`, `_removed-ks_verrouvehicule-1.1.0/` ; jars du serveur vérifiés identiques à ceux compilés), actif après redémarrage d'Event. Statut : non testé en jeu.** `config.yml` de KS_Trains sur Event : identique au fichier fourni par la 1.2.0 (aucune retouche), copié dans `_removed-ks_trains-1.2.0/config.yml` puis remplacé par celui de la 1.3.0 (`espacement` 1,4, clés `decor` et `jukebox`) ; vérifié identique après l'envoi.

**Nouvelle physique, deuxième essai (même envoi, Event arrêté)** : `world/level.dat` du serveur (réécrit par le jeu : pack de nouveau dans Disabled) copié dans `/_removed-level-dat-2026-10-16/level-avant-correction-2217.dat`, puis remplacé par la version corrigée : pack `minecart_improvements` dans Enabled **et** `Data.enabled_features = [minecraft:vanilla, minecraft:minecart_improvements]` (seuls changements, vérifiés par relecture ; fichier du serveur identique après l'envoi). À confirmer dans le journal au démarrage, puis `/gamerule max_minecart_speed 40`.

**Actif sur Event depuis son redémarrage du 16/10/2026 à 22:20 (journal lu, heures du journal en UTC : 20:20) : KS_Trains 1.3.0, KS_Crafts 1.18.0 (« 82 crafts ajoutés » : + le wagon jukebox) et KS_VerrouVehicule 1.1.1 activés sans erreur ni avertissement. **Nouvelle physique des wagonnets active** : plus de refus du pack au démarrage, et le `level.dat` réécrit par le serveur à 22:20:16 garde `minecart_improvements` dans `enabled_features` et dans les packs activés. Reste à faire par LeKiwi06 : `/gamerule max_minecart_speed 40`. Statut : non testé en jeu.**

## 1.3.1 - wagons incassables, décor qui ne reste plus, accélération progressive (16/10/2026, LeKiwi06)

Signalements et demande de LeKiwi06 (tests sur Event) : « j'arrive à casser les wagons four et le wagon contrôleur
alors que ce n'est pas censé être possible, et avec les nouveaux modèles agrandis quand je casse un wagon le modèle
reste là où il était » ; « le train accélère et freine trop vite aussi, il faut que la vitesse soit plus progressive,
et quand le train est en surcharge de wagons il faut que ce soit encore plus lent, par exemple un train doit mettre une
centaine de blocs à atteindre sa vitesse maximum, et pour freiner une cinquantaine ».

- **Wagons cassables** (défaut de la 1.0.0, vu en jeu) : KS_Claim 1.3.0 lève, en priorité HIGHEST, tout refus de casser
  un wagonnet **sans inventaire** frappé par un joueur (demande du 11/10 : wagonnets cassables dans les claims). Les
  wagons à four, le contrôleur et le wagon jukebox n'ont pas d'inventaire : la protection de KS_Trains (priorité LOW)
  était défaite ; les coffres et entonnoirs, eux, restaient protégés. Les écoutes de KS_Trains passent maintenant en
  HIGHEST, après KS_Claim (KS_Trains est activé après lui, `softdepend`) : un wagon attelé ne se casse plus.
- **Décor resté sur place** (défaut de la 1.3.0) : le modèle descendait du wagon cassé et restait. Un décor qui descend
  de son wagon disparaît ; il est aussi retiré quand le wagon quitte le monde.
- **Accélération et freinage progressifs** : `acceleration` 0,02 → **0,005** et `freinage` 0,05 → **0,01** bloc/tick par
  tick : de l'arrêt à 1 bloc/tick (20 blocs/s) en environ **100 blocs** (10 s), de 1 bloc/tick à l'arrêt en environ
  **50 blocs** (5 s). **En surcharge** (plus de wagons que la traction), les deux sont multipliés par traction /
  wagons (ex. 25 wagons pour 15 de traction : x0,6, soit environ 167 blocs pour accélérer et 83 pour freiner). Les
  arrêts d'urgence (bout des rails, obstacle, autre train) restent immédiats.
- `config.yml` : nouvelles valeurs de `acceleration` et `freinage` (**à changer à la main sur Event**, qui a 0,02 et
  0,05).

Choix de Claude, signalés : la surcharge ralentit aussi le freinage (un train lourd freine plus longtemps) ; le
coefficient est traction / wagons.

Compilé le 16/10/2026 (`sortie/KS_Trains-1.3.1.jar`). **Statut : non déployé, non testé en jeu.**

## 1.3.2 - percussions et virages (16/10/2026, LeKiwi06)

Demandes de LeKiwi06 : « il faut aussi qu'un train qui collisionne une entité sur sa route la fasse voler en l'air
(comme un dragon quand il nous pousse, plus il va vite, plus il projette loin, à vitesse max il propulse comme le
dragon), à l'exception des wagons qui se brisent, et les autres trains déraillent (le déraillement : tous les wagons se
décrochent et les locomotives se cassent) ; par contre quand le train est arrêté, il faut que les entités le traversent
normalement » ; puis « il faut également que le train se fasse ralentir dans les virages » (réponses : -25 % de sa
vitesse, sauf en vitesse 1 ; il anticipe, mais en diagonale il ne faut compter qu'un virage tant qu'il reste en
diagonale). Remplace les collisions de la 1.3.0.

- **Train arrêté** : aucune collision, les entités le traversent (le jeu ne pousse plus rien).
- **Train qui roule** : chaque tick, ce qui touche un de ses wagons (et ce qui est devant le wagon qui mène, sur la
  distance du tick) est percuté :
  - **créature ou joueur** : projeté comme par le dragon (lu dans le code du jeu 26.3, EnderDragon.knockBack : 4 x écart
    / distance² à l'horizontale, 0,2 vers le haut, depuis le centre du wagon), multiplié par vitesse / vitesse maximale
    (à 20 blocs/s : exactement le dragon). Une même entité au plus une fois par demi-seconde. Pas de dégâts (le dragon en
    fait 5 : non repris, non demandé).
  - **wagonnet libre** : il **se brise** (son objet et son contenu tombent ; wagon contrôleur ou jukebox : leur objet,
    leurs cornes ou leur disque). Une protection peut l'empêcher (casse signalée aux autres plugins : un coffre
    verrouillé et non vide de KS_VerrouVehicule ne se brise pas). Wagonnets à bloc de commande ou à spawner : ignorés.
  - **wagon d'un autre train** : cet autre train **déraille** (« Déraillement ! ») : tous ses wagons sont dételés et
    arrêtés, ses fours de locomotive se brisent (objet et carburant au sol). Le train qui percute continue ; les wagons
    dételés restés sur sa route se brisent à leur tour.
  - Passagers, décor, sièges invisibles et spectateurs ne comptent pas.
- **Virages** : à partir du niveau 2 (pas au niveau 1 ni en marche arrière), dans un virage la vitesse voulue perd
  **25 %** (`virage.reduction`) tant qu'un wagon du train est sur un rail courbe. Le train **anticipe** : il lit les
  rails devant le wagon qui mène (bloc par bloc, virages et pentes compris) sur sa distance de freinage et freine pour
  arriver au virage à la bonne vitesse. Une **diagonale** (virages qui se suivent) reste un seul ralentissement de 25 %.

Choix de Claude, signalés : le train qui percute ne ralentit pas ; un wagonnet à TNT percuté se brise sans exploser ;
deux trains qui roulent l'un contre l'autre déraillent tous les deux ; la réduction de 25 % porte sur la vitesse voulue
(niveau ou vitesse permise par la traction).

Compilé le 16/10/2026 (`sortie/KS_Trains-1.3.2.jar`). **Statut : non déployé, non testé en jeu.**

## 1.3.3 - ralentissement aux percussions, wagons chargés projetés, TNT, jukebox au rechargement (16/10/2026, LeKiwi06)

Demandes de LeKiwi06 : « le train qui percute doit ralentir de 10 % de sa vitesse, sauf si ça le fait descendre sous la
vitesse 1 ; pour tous les wagons sur les rails avec un contenu, il faut les propulser en l'air comme si c'était des
entités au lieu de les briser ; les wagons à TNT ne peuvent pas être mis dans une configuration de train ; j'ai trouvé
un bug : quand le train se décharge puis qu'on le recharge, il passe à la musique suivante sur le jukebox ».

- **Ralentissement** : à chaque percussion (entité projetée, wagonnet brisé ou projeté, train déraillé), la vitesse du
  train perd **10 %** (`percussion.ralentissement`), sans descendre sous le niveau 1 (5 blocs/s, `palier`) ; s'il roule
  déjà moins vite, rien. Il réaccélère ensuite vers son niveau.
- **Wagonnet libre avec un contenu** : **projeté** comme une entité (poussée du dragon selon la vitesse) au lieu d'être
  brisé. Contenu : coffre ou entonnoir non vide, passager, cornes d'un wagon contrôleur, disque d'un wagon jukebox. Un
  wagonnet vide se brise comme en 1.3.2.
- **Wagons à TNT** : déjà refusés à l'attelage depuis la 1.0.0 (comme ceux à bloc de commande et à spawner), mais sans
  rien dire : un clic avec une chaîne affiche maintenant « Ce wagon ne s'attelle pas à un train ».
- **Jukebox au rechargement** (défaut de la 1.3.0) : au retour d'un wagon déchargé, la fin de morceau gardée en mémoire
  était dépassée : le disque était rendu au coffre et le suivant lancé. Le wagon oublie maintenant sa fin de morceau
  quand il quitte le monde, et **rejoue depuis le début le disque qu'il tient** (aussi après une interruption faute
  d'auditeur ou de coffre). Un disque n'est rendu qu'en fin de morceau ou en pause.
- `config.yml` : nouvelle clé `percussion.ralentissement` (0,10) ; valeur par défaut aussi dans le code.

Choix de Claude, signalés : « contenu » comprend les passagers, les cornes et le disque ; un wagonnet projeté plusieurs
fois de suite ne compte qu'une fois par demi-seconde (comme une entité).

Compilé le 16/10/2026 (`sortie/KS_Trains-1.3.3.jar`). **Statut : non déployé, non testé en jeu.**

## 1.3.4 - son de la locomotive, compteur kilométrique (16/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faudrait aussi mettre un son reconnaissable pour la locomotive, et un compteur kilométrique
dans l'interface de la locomotive pour voir la distance parcourue avec aussi » ; réponses : tchou-tchou à vapeur ;
avec le carburant consommé.

- **Son** (sons du jeu, entendus aussi sur Bedrock) : au démarrage (ou en repartant après un arrêt de plus de 2 s),
  **coup de sifflet** (deux notes de flûte ensemble) ; en roulant, **souffle de vapeur** (extinction de feu) rythmé, de
  plus en plus rapide avec la vitesse : toutes les 12 ticks au pas, toutes les 3 ticks à 20 blocs/s, en alternant deux
  hauteurs (tchou-tchou). Le son sort du premier four de locomotive ; un train sans locomotive ne fait pas de bruit.
- **Compteur** : chaque four de locomotive enregistre la distance parcourue et le carburant qu'il a brûlé. Dans le menu
  du train (bas, à gauche de la fiche de traction), une horloge : « Compteur : 850 blocs » ou « 12,3 km » (distance du
  four le plus avancé) et « Carburant brûlé : 154 charbons » (équivalent : réserve consommée par les fours de la
  locomotive / 80, consommation selon le niveau comprise). Les compteurs suivent les wagons (dételés puis rattachés,
  ils gardent leur total) ; ils partent de zéro pour les locomotives posées avant cette version.

Choix de Claude, signalés : sons dans la catégorie « Créatures neutres / Autres » des réglages du jeu ; distance comptée
dans les deux sens de marche ; carburant des autres fours du train non compté (« carburant de la locomotive »).

Compilé le 16/10/2026 (`sortie/KS_Trains-1.3.4.jar`). **Statut : non déployé, non testé en jeu.**
