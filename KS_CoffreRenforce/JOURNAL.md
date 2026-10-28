# KS_CoffreRenforce - journal

Plugin de Kixster (survie). Dépend de KLM_Menu (menus en coffre, textes). Recette dans KS_Crafts (1.21.0).

## 1.0.0 - coffre renforcé (25/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'aimerais créer ma propre version du drawer : le coffre renforcé. Il se craft avec un
coffre, 4 blocs de fer et 4 fragments de spawner, et permet de stocker jusqu'à 1 000 stacks du même item. J'aimerais
qu'on puisse le remplir et le vider par la redstone, on ne peut pas coller d'autres coffres à côté pour en faire un
double coffre et il peut servir de boutique. »

Choix de LeKiwi06 (questions du 25/10/2026, maquette du menu validée) :

| Question | Choix |
|---|---|
| Déposer / retirer à la main | Menu en coffre (pas de clic gauche façon drawer) |
| Casser un coffre plein | Refusé tant qu'il n'est pas vide |
| Recette | Coffre au centre, blocs de fer sur les côtés, fragments de spawner aux coins |
| Voir le contenu sans ouvrir | Texte au-dessus de la barre d'objets en le visant, avec le mot « stack » (pas « pile ») |
| Entonnoirs sur une boutique | Ni remplir ni vider ; une boutique de coffre renforcé se paie **seulement en points** (pas de troc) |
| Coffre vidé | Il se libère (accepte de nouveau n'importe quel objet) |
| Objets refusés | Shulkers et sacs qui contiennent quelque chose |
| Comparateur | Signal selon le remplissage (0 vide, 15 à 1 000 stacks) |
| Partie boutique (KS_Economy) | Après le coffre, une fois finie l'autre session qui modifiait KS_Economy |

Fonctionnement :

- **Objet** : un coffre nommé « Coffre renforcé » (marqueur `ks_coffrerenforce:coffre`). Vrai coffre vanilla, objet et
  bloc : visibles tels quels sur Bedrock, aucune entrée à ajouter dans `geyser-bedrock`.
- **Capacité** : 1 000 stacks de l'objet stocké (64 000 pour un objet par 64, 16 000 par 16, 1 000 non empilable),
  un seul objet exact (enchantements, nom compris).
- **Menu** (clic droit, 3 rangées) : l'objet stocké en haut avec sa quantité et ses stacks (vitre blanche « Vide ») ;
  « Retirer 1 », « Retirer 1 stack », « Remplir mon inventaire », « Tout déposer » (coffre vide : l'objet tenu en main
  choisit ce qu'il stocke) ; un clic sur un objet de son inventaire le dépose ; livre d'aide, « Fermer ». Messages
  au-dessus de la barre d'objets. Le menu ne s'ouvre que si le coffre aurait pu s'ouvrir (claims respectés), jamais
  pour un spectateur ; le vrai coffre ne s'ouvre jamais.
- **Texte visé** (toutes les demi-secondes, à 5 blocs, sans menu ouvert) : « Coffre renforcé : 12 345 x Diamant
  (193 / 1 000 stacks) » ou « vide (0 / 1 000 stacks) ».
- **Redstone** : les 27 cases du vrai coffre servent de tampon (une partie du stock). Après chaque passage d'entonnoir,
  le tampon est recalculé : le comparateur lit le remplissage du coffre entier (calcul du jeu 26.3 relu avec javap), et
  la place libre du tampon ne dépasse jamais la place restante (un entonnoir ne dépasse pas 1 000 stacks). Calcul
  vérifié pour chaque quantité de 0 à la capacité (objets par 64, par 16, non empilables) : aucune erreur. Un
  entonnoir n'y met que l'objet stocké (n'importe quel objet accepté si le coffre est vide) et jamais un objet refusé.
- **Jamais de coffre double** : un coffre posé contre un coffre renforcé (ou l'inverse) redevient simple au tick
  suivant ; on peut donc poser plusieurs coffres renforcés côte à côte (mon choix : ne pas refuser la pose, comme le
  fait KS_CoffreMort, pour permettre des murs de coffres renforcés).
- **Casser** : refusé s'il contient quelque chose (message) ; vide, il redonne un coffre renforcé (rien en créatif).
  Explosions, wither, dragon : il n'est pas détruit.
- **Stock** : gardé dans le bloc (données du coffre : objet + réserve), rien dans un fichier. KS_MondeTest n'a rien à
  couper : un coffre renforcé du monde de test reste dans ce monde (il ne se casse que vide).
- **API** pour les boutiques (KS_Economy, à faire) : `estCoffreRenforce`, `objet`, `nombre`, `retirer`, `ajouter`.

Limites :

- Un objet mis dans le tampon par autre chose qu'un entonnoir (golem de cuivre, autre plugin) et qui n'est pas l'objet
  stocké est rejeté au-dessus du coffre au prochain passage d'entonnoir ou à l'ouverture du menu (non vérifié en jeu).
- Le nom de l'objet vient du jeu, au singulier : « 12 345 x Diamant » (la maquette disait « Diamants »).
- **Pas encore de boutique** : la partie KS_Economy (panneau sur un coffre renforcé, paiement en points seulement,
  entonnoirs refusés) reste à faire.
- Didacticiel de Kixster à mettre à jour au déploiement.

**Déployé sur Kixster le 25/10/2026 à 03:51** avec KS_Crafts 1.21.0 et `didacticiel.yml` (LeKiwi06 : « Oui, déploie KS_CoffreRenforce 1.0.0 et KS_Crafts 1.21.0 sur Kixster, avec la mise à jour du didacticiel » ; nouveau jar de la même taille que celui compilé), actif après redémarrage de Kixster. Statut : non testé en jeu.

## 1.1.0 - bateau et minecart avec coffre renforcé (27/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « peux-tu faire le bateau et le minecart avec coffre renforcé ».

Choix de LeKiwi06 (questions du 27/10/2026) :

| Question | Choix |
|---|---|
| Recette | Comme le vanilla : un bateau (n'importe quel bois, radeau compris) + un coffre renforcé ; un minecart + un coffre renforcé |
| Casser un véhicule plein | « il est cassable s'il contient moins de 27 stacks » (le contenu tombe au sol) |
| Wagon de train (KS_Trains) | Oui, avec KS_Trains (1.5.0) |
| Bateau à jukebox (KS_JukeboxBoat) | Non |

Fonctionnement :

- **Objets** : « Bateau avec coffre renforcé » (bateau à coffre du bois choisi), « Radeau avec coffre renforcé »
  (bambou), « Minecart avec coffre renforcé » : objets vanilla nommés, même marqueur que le coffre renforcé. Vrais
  bateaux et minecarts à coffre une fois posés : visibles tels quels sur Bedrock, rien à ajouter dans `geyser-bedrock`.
- **Craft** : la recette du jeu (bateau à coffre de chaque bois, minecart à coffre) faite avec un coffre renforcé donne
  le véhicule avec coffre renforcé, du même bois ; aussi dans un crafter. Avant, elle donnait un véhicule à coffre
  ordinaire et le coffre renforcé était perdu. Un véhicule avec coffre renforcé ne sert d'ingrédient à aucune recette
  (bateau à jukebox compris). Un distributeur ne le pose pas (il poserait un véhicule ordinaire).
- **Aucun autre craft** (LeKiwi06 : « il ne faut pas que le coffre renforcé serve aux crafts normaux ») : une recette
  avec un coffre renforcé autre que celle du bateau ou du minecart à coffre ne donne rien, dans un établi comme dans
  un crafter (avant : entonnoir, coffre piégé, wagonnet à entonnoir... le consommaient comme un coffre ordinaire).
- **Aucun combustible** (LeKiwi06 : « bloque-le aussi ») : un coffre renforcé ou un véhicule avec coffre renforcé ne
  brûle pas dans un four, un fumoir ou un haut fourneau (il reste dans la case du combustible), ni dans les fours de
  la locomotive de KS_Trains 1.5.0 (`estObjetRenforce`).
- **Stock** : gardé dans le véhicule (données de l'entité), avec les 27 cases de son coffre comme tampon, exactement
  comme le bloc (capacité 1 000 stacks, entonnoirs, comparateur sur rail détecteur pour le minecart).
- **Menu** : le même que le bloc (clic droit sur le minecart, Maj + clic droit sur le bateau, ou inventaire à bord) ;
  l'aide donne la règle du véhicule. Texte au-dessus de la barre d'objets en le visant, comme le bloc.
- **Casser** : refusé (message) avec plus de 27 stacks dedans, coups et explosions compris ; avec 27 stacks ou moins,
  tout le stock tombe au sol et le véhicule redonne l'objet « … avec coffre renforcé » (avec son nom s'il a été
  renommé). Dans un train, KS_Trains refuse toujours de casser un wagon attelé.
- **API** : `estCoffreRenforce`, `objet`, `nombre`, `retirer`, `ajouter` existent aussi pour une entité (véhicule) ;
  `creerBateau(Material)`, `creerMinecart()`.
- Le code passe par un « support » (bloc ou véhicule) dans `Stockage` et `MenuCoffre` ; rien de changé pour le bloc.

Limites :

- « Moins de 27 stacks » compris comme **27 stacks ou moins** (ce que contient un coffre normal) : à confirmer par
  LeKiwi06 (`STACKS_CASSABLE`).
- Un véhicule qui disparaît sans être cassé (tombé dans le vide, `/kill`) perd son contenu, comme un véhicule vanilla.
- Le menu reste utilisable tant qu'il est ouvert, même si le véhicule s'éloigne (pas de distance vérifiée).
- Remplacement de l'objet lâché par le jeu fait comme KS_JukeboxBoat (`EntityDropItemEvent`), non vérifié en jeu.
- Le wagon jukebox de KS_Trains lit ses disques dans les 27 cases du wagon à coffre devant lui : derrière un minecart
  avec coffre renforcé, il ne voit que le tampon (non modifié, à décider).
- Didacticiel de Kixster (`crafts > coffre_renforce`) à compléter au déploiement ; KS_Didacticiel est réservé en ce
  moment par le chantier /contrat.

Compilé le 27/10/2026 (`sortie/KS_CoffreRenforce-1.1.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Kixster le 28/10/2026 à 09:34** (LeKiwi06 : « Oui, déploie KS_CoffreRenforce 1.1.0 et KS_Trains 1.5.0 sur Kixster »), avec KS_Trains 1.5.0 et `didacticiel.yml` (Véhicules › « Bateau et minecart avec coffre renforcé ») : jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, rangés dans `_removed-ks_coffrerenforce-1.0.0/` et `_removed-ks_trains-1.4.8/` ; fichiers du serveur vérifiés identiques à ceux compilés. À redémarrer par LeKiwi06. Statut : non testé en jeu.

## 1.2.0 - décraft des véhicules avec coffre renforcé (28/10/2026, LeKiwi06)

Décraft demandé par LeKiwi06 (fait dans KS_Crafts 1.23.0) : bateau, radeau ou minecart avec coffre renforcé seul dans
la grille -> coffre renforcé, et le bateau, le radeau ou le minecart reste dans la grille. Ici, seulement : la recette
`ks_crafts:decraft_vehicule` n'est plus refusée (le véhicule avec coffre renforcé reste refusé dans toutes les autres).
**À déployer avec KS_Crafts 1.23.0.**

Compilé le 28/10/2026 (`sortie/KS_CoffreRenforce-1.2.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Kixster le 28/10/2026 à 10:49** (LeKiwi06 : « Oui, déploie KS_Crafts 1.23.0 et KS_CoffreRenforce 1.2.0 sur Kixster avec le didacticiel »), avec `didacticiel.yml` : jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, rangés dans `_removed-ks_crafts-1.22.0/` et `_removed-ks_coffrerenforce-1.1.0/` ; fichiers du serveur vérifiés identiques à ceux compilés. À redémarrer par LeKiwi06. Statut : non testé en jeu.
