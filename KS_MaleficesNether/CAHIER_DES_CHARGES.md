# Maléfices du Nether (demande du 29/10/2026, Kixster)

État : **validé par LeKiwi06 le 30/10/2026** (« Oui, je valide »). Code commencé le 30/10. Coûts en XP : `Malefices_Nether_cout_XP.xlsx`.

## 1. Demande (LeKiwi06, 29/10/2026)

Augmenter la difficulté du Nether. Écarté : les effets de potion sur les mobs (une cinquantaine de mobs avec effets
font lagger puis crasher les clients Bedrock).

> pourrait-on appliquer des maléfices aux joueurs en fonction du temps passé consécutivement dans le nether . par
> exemple au bout de 1 jours donc 20 minutes : le joueurs a périodiquement hunger , ensuite au bout de 2 jours :
> hunger + lenteur , jour 3 : faim + lenteur + faiblesse , jour 4 : faim + lenteur + faiblesse + poison , ce ne
> serait pas des effets permanent dans la dimension on parle de 1 minutes par jours le jours 1 , puis 2x1 minutes le
> jours 2 , etc : ce qui fait qu'au jour 20 ça deviens intenable et il faut sortir . on pourrait aussi faire un atout
> sur l'équipement qui permet de diminuer ces effets , ou une amulette mangeuse de mal , qui te protège tant qu'elle
> contient de l'xp .

Suite du même jour :

> l'atout servirait à reduire la longueur en effet , j'avais pensé à un plastron anti-radiation , avec des blocks
> d'or autour du plastron : chaque craft protège pour 1 jours passé dans le nether
>
> l'amulette peut etre stocké dans le même emplacement que les jetons et badges , il suffit d'une 3 eme catégorie :
> équipement spéciaux ( j'ai déjà pensé à un bague de relais dimensionnel , qui se décharge quand on tombe dans le vide
> de l'end , mais qui nous fait spawn en couche 500 de l'overworld afin d'éviter de mourir en plein millieu du vide
> avec un coffre de mort super dure à récupérer )
>
> elle ne stock pas d'xp , elle le consomme , nuance : elle va ponctionner l'xp que le joueur à sur lui , pour chaque
> effet qu'elle supprime , elle consomme 10 points d'xp par secondes et niveau retiré
>
> c'est clairement voulu que les farm afk soit déjà nerf , et comme les ferme afk du nether produisent en général de
> l'xp c'est une façon d'éviter que le joueur ressorte niveau 200 de la dimension en 1h d'afk
>
> mon exemple s'arretait au jour 4 , mais on va evidemment ajouter + de maléfices en fonction des jours , puis quand on
> aura épuisé le stock d'effet négatifs , on va s'amuser a les passer niveau 2 , puis 3 , etc

## 2. Réponses de LeKiwi06 (29/10/2026)

| Question | Réponse |
|---|---|
| Fermes AFK du Nether visées ? | Oui, voulu (elles produisent de l'XP). |
| Sortir du Nether remet le compteur à zéro ? | Non : le compteur **descend petit à petit** hors du Nether. |
| Le lait (et le miel pour le poison) ? | L'effet est **remis toutes les 5 secondes** pendant le maléfice. |
| Déconnexion ? | Validé : le compteur est **en pause** (ni montée ni descente). |
| Prévenir le joueur (barre de boss « Nether : jour N », message avant chaque maléfice) ? | Validé, à ajuster à l'usage. |
| Après le jour 4 ? | D'autres effets, puis passage au niveau II, III, etc. |
| Apparence de l'amulette | Texture faite par LeKiwi06. |
| Taux de l'amulette (10 au départ) | **Baissé à 5** points par seconde et par niveau. |

Réponses du 30/10/2026 :

> 2 : non sinon si le joueur passe 2h dedans et qu'il meurt , le maléfice ne s'enlève qu'après 1h , j'ai pas envie
> qu'il perde leur stuff si bêtement
>
> 3: il prend l'apparence d'un plastron en or ( le side effect est qu'ils ne sont pas aggro par les piglins , je le
> sais , c'est volontaire )
>
> 4 : forme de jambière avec des chaine en cuivre et une balise en bas dans la table de craft , il faut aussi pouvoir
> la drop dans les coffre de rewards ( quel est la valeur d'une balise dans l'économie ? )
>
> 5 : le maléfice est un effet du /effect , il peuvent le suivre via une boss bar
>
> d'ailleur j'aimerai que les effets du maléfice ne pop pas tous en meme temps , pour chaque jours entamé dans le
> nether on tire un timer random allant de 0 a 19 minutes pour activer la minute d'effet ( comme ça la boss bar
> apparait a chaque effet qui s'active , si il y a plusieurs effets en simultanés , on ecrit dans le nom " maléfice :
> effet 1+ effet 2 , etc et la barre de vie montre le 1er effet jusqu'a ce qu'il se termine , j'aimerai que la barre
> de vie soit rouge

Deuxièmes réponses du 30/10/2026 :

> 1 : les effets seront donc dans l'ordre : hunger , weakness , mining fatigue ,poison , slowness , darkness ,
> infestation , wither , levitation , nausée
> un fois arrivé au jour 11 , on passe les effets au niveau 2 dans l'ordre indiqué ci-desssus quand il y a un interet
> a le faire ( je pense notamment a darkness et nausée , d'ailleur ces effets empêchent le joueur de jouer , donc je
> propose qu'au début ils durent 10 secondes , et au lieu de passer au niveau supérieur , on rajoute 10 secondes )
>
> 2 : d'accord je valide
> 3 : le plastron divise le temps d'action des effets par 2 .
> 4 : passe sa valeur à 7500 , je sais c'est contre intuitif , mais l'amulette est un usage plus spécifique qu'une
> balise , donc elle vaudra moins ; je ne sais pas pour les pool et les fréquences , propose moi des choses
>
> 5 : si la barre de boss n'est pas de la bonne couleur sur bedrock ce n'est pas grave , c'est uniquement visuel
>
> oui c'est ça pour le tirage random de temps ( d'ailleur quand je dis jours c'est pas réellement les jours du jeu ,
> sinon si un joueur dors overworld ça peut faire bugguer , on compte un jour toute les 20 minutes réellement passé à
> l'intérieur , l'indicateur de jours est plus esthétique pour les joueurs que fonctionnel pour le plugin )

## 3. Choix d'interprétation

### 3.1 Compteur

- **Jour** = 20 minutes **réellement** passées dans le Nether (validé le 30/10), jamais les jours du jeu (un joueur
  qui dort dans l'Overworld ne change rien). Le numéro du jour sert d'affichage. Jour 0 (les 20 premières minutes) :
  rien.
- **Maléfice du jour N** (validé le 30/10) : au début de chaque jour, **chaque effet** tire au hasard N minutes
  différentes parmi les 20 du jour (départ de 0 à 19 min) et s'active à chacune (durée d'une activation en 3.2). Les
  effets ne démarrent donc pas ensemble ; au jour 20, un effet de 1 minute tient les 20 minutes : continu.
- Compte seulement le joueur **connecté, dans le Nether, en survie ou aventure**. Pause à la déconnexion.
- **Mort** (validé le 30/10) : compteur remis à zéro, pour que le joueur puisse retourner chercher son coffre de mort
  sans maléfice.
- **Hors du Nether sans mourir** (validé le 30/10) : 1 jour effacé toutes les 10 minutes dehors.
- Données par joueur via `CompteTest.de(uuid)` (règle de KS_MondeTest).

### 3.2 Effets (ordre de LeKiwi06, 30/10 ; réglable dans config.yml)

Jours 1 à 10 : un effet de plus par jour, dans l'ordre. À partir du jour 11 : une montée par jour, dans le même
ordre, seulement pour les effets où monter change quelque chose ; puis on recommence la liste.

- **Obscurité, lévitation et nausée** : 10 s par activation au lieu de 1 minute ; leur « montée » = **+ 10 s** (pas
  de niveau). Lévitation ajoutée le 30/10 par LeKiwi06.
- **Infestation** : pas de niveau dans le jeu (10 % de chance, 1 ou 2 poissons d'argent, lu dans le code) : sautée.
- **Fatigue de minage** : niveau III au plus (au-delà, plus aucune différence) : sautée ensuite.
- Les autres effets durent 1 minute par activation.

| Jour | Ajout | Jour | Montée | Jour | Montée |
|---|---|---|---|---|---|
| 1 | Faim | 11 | Faim II | 20 | Faim III |
| 2 | Faiblesse | 12 | Faiblesse II | 21 | Faiblesse III |
| 3 | Fatigue de minage | 13 | Fatigue de minage II | 22 | Fatigue de minage III |
| 4 | Poison | 14 | Poison II | 23 | Poison III |
| 5 | Lenteur | 15 | Lenteur II | 24 | Lenteur III |
| 6 | Obscurité (10 s) | 16 | Obscurité 20 s | 25 | Obscurité 30 s |
| 7 | Infestation | - | (sautée) | - | (sautée) |
| 8 | Wither | 17 | Wither II | 26 | Wither III |
| 9 | Lévitation (10 s) | 18 | Lévitation 20 s | 27 | Lévitation 30 s |
| 10 | Nausée (10 s) | 19 | Nausée 20 s | 28 | Nausée 30 s |

- **Lévitation** (10 s, validé le 30/10) : environ 1 bloc par seconde, soit environ 10 blocs et 3,5 cœurs de chute
  sans armure (1 minute aurait fait une soixantaine de blocs, chute mortelle). Parade voulue par LeKiwi06 : le
  **casque ventouse**. Lu dans KS_Atouts 1.1.0 : la ventouse ne colle pas pendant une autre lévitation, mais dès que
  celle du maléfice finit, la tête contre le plafond, elle colle au tick suivant ; déjà collé, une lévitation I plus
  courte que la sienne (15 s) ne la remplace pas (règle du jeu). **À respecter dans le code** : la lévitation du
  maléfice garde son **icône**, sinon KS_Atouts la prend pour la sienne et la retire. À tester en jeu.
- Effets **sur le joueur seulement**, sans particules (icône gardée) : un seul joueur, pas de souci Bedrock.
- Remis toutes les 5 secondes pendant le maléfice (durée 6 s), donc le lait ne soulage que quelques secondes.

### 3.3 Plastron anti-radiation (atout, KS_Atouts)

- Craft : 8 blocs d'or autour d'un plastron (tous les plastrons). Règles des atouts : un atout par objet, cumulable,
  apparence changée, stats gardées, ni posable ni utilisable dans un autre craft.
- **Effet** (30/10) : **durée de chaque activation divisée par 2** (1 minute → 30 s, 10 s → 5 s). Le compteur monte
  normalement. Le coût de l'amulette est donc divisé par 2 aussi.
- **Durée de l'atout** (validé le 30/10) : chaque craft = 1 jour (20 min), décompté seulement quand il est porté dans
  le Nether, comme les autres atouts qui ont un temps par craft.
- Apparence pendant l'atout (30/10) : **plastron en or** (vrai type, stats gardées). Les piglins ne l'attaquent donc
  pas : voulu par LeKiwi06.

### 3.4 Amulette mangeuse de mal

- **Équipement spécial** : rangée de plus dans l'inventaire spécial de KS_Jetons (/jetons), « Équipements spéciaux » ;
  elle agit tant qu'elle y est rangée. Même rangée plus tard pour la bague de relais dimensionnel.
- Ne stocke rien : pendant un maléfice, elle retire chaque effet et **prend 5 points d'XP par seconde et par niveau
  d'effet retiré** sur le joueur (10 au départ, LeKiwi06 le 29/10). Sans assez d'XP, l'effet s'applique.
- **Craft** (30/10), à l'établi, en forme de jambières :

  | | | |
  |---|---|---|
  | chaîne en cuivre | chaîne en cuivre | chaîne en cuivre |
  | chaîne en cuivre | (vide) | chaîne en cuivre |
  | chaîne en cuivre | **balise** | chaîne en cuivre |

  Toutes les chaînes en cuivre acceptées (oxydées ou cirées, choix de Claude).
- **Valeur : 7 500 émeraudes** (LeKiwi06, 30/10 : usage plus étroit qu'une balise, donc moins chère, même si la balise
  vaut 13 120 au barème). Prix à mettre dans KS_Economy ; rareté de KS_Textures entre 1 000 et 10 000 (pas la plus
  haute) ; recompiler KS_Textures après le changement de prix.
- **Coffres de récompense** (demande du 30/10) : propositions en 3.7.
- Objet custom : apparence Bedrock (geyser-bedrock), texture de LeKiwi06, rareté (KS_Textures).

### 3.4 bis Affichage (30/10)

- Le maléfice est un **effet de /effects** (KS_Effets) : « Maléfice du Nether », jour en cours et temps avant le
  prochain jour.
- **Barre de boss rouge** qui apparaît d'elle-même à chaque effet qui s'active : « Maléfice : Faim », plusieurs
  effets en même temps = « Maléfice : Faim + Lenteur + … ». La barre montre le temps du **premier effet actif**
  jusqu'à sa fin, puis passe au suivant ; elle disparaît quand plus aucun effet n'est actif.
- Couleur sur Bedrock : sans importance si elle n'est pas rouge (LeKiwi06, 30/10). Reste à voir la longueur du nom
  quand beaucoup d'effets sont actifs.

### 3.5 Estimation du coût de l'amulette (5 pts/s, détail dans le .xlsx)

Avec l'ordre de 3.2 (recalculé le 30/10) :

| Jour | Commence après | Coût du jour | Cumulé | Cumulé en niveaux depuis 0 | Avec plastron : cumulé en niveaux |
|---|---|---|---|---|---|
| 1 | 20 min | 300 | 300 | 14 | 9 |
| 2 | 40 min | 1 200 | 1 500 | 30 | 22 |
| 3 | 1 h | 2 700 | 4 200 | 45 | 35 |
| 4 | 1 h 20 | 4 800 | 9 000 | 60 | 46 |
| 6 | 2 h | 9 300 | 25 800 | 92 | 70 |
| 8 | 2 h 40 | 17 200 | 55 950 | 128 | 95 |
| 10 | 3 h 20 | 22 500 | 98 250 | 165 | 121 |
| 15 | 5 h | 56 250 | 306 000 | 278 | 201 |
| 20 | 6 h 40 | 90 000 | 681 000 | 406 | 292 |

Repères : niveau 30 = 1 395 points, niveau 50 = 5 345, niveau 100 = 30 970. Jours 1 à 5 inchangés (300 × N²) ;
ensuite un peu moins cher qu'avant, car l'obscurité, la lévitation et la nausée ne durent que 10 s.

### 3.7 Amulette dans les coffres de récompense (C : 2 %, choisi par LeKiwi06 le 30/10)

À coder dans `sortie/rewards/pools.py` : entrée « Amulette mangeuse de mal » du pool Légendaire, famille « Objets de
nos plugins », poids relatif **0,71** (2 % du pool ; la famille garde ses 13 %). Fait le 30/10 ; `butin.yml` et
`fusion.yml` régénérés avec les prix actuels (choix de LeKiwi06). Correction : les fichiers de butin n'ont pas de
filtre de dimension (la Clé de l'End non plus) ; l'amulette peut donc sortir même Nether fermé. Chances réelles
(pool recalculé) : coffre fusionné de 1 000 = 1 sur 125, 2 500 = 1 sur 50, 5 000 = 1 sur 25, 10 000 = 1 sur 9.

Elle irait dans le pool **Légendaire**, famille « Objets de nos plugins » (13 % du pool), à côté de la Clé de l'End
(5 071, 1,33 % des tirages Légendaires), avec la dimension « nether » (jamais tirée si le Nether est fermé). Les
fréquences des niveaux sont recalculées par `pools.py` pour garder la valeur de chaque récompense.

Chance d'avoir l'amulette dans une récompense, selon sa part des tirages Légendaires :

| Récompense | A : 0,5 % | B : 1 % (proposée) | C : 2 % |
|---|---|---|---|
| Kal-Games grosse (12 tirages) | 1 sur 16 700 | 1 sur 8 300 | 1 sur 4 200 |
| Kal-Games exceptionnelle (16 tirages) | 1 sur 450 | 1 sur 220 | 1 sur 110 |
| Kal-Games exceptionnelle permanente (24) | 1 sur 220 | 1 sur 110 | 1 sur 55 |
| Kanvas grosse (24 tirages) | 1 sur 110 | 1 sur 56 | 1 sur 28 |
| Kanvas exceptionnelle (40 tirages) | 1 sur 6 | 1 sur 3 | 1 sur 2 |

- **B** donne à l'amulette à peu près le même poids en valeur que la Clé de l'End (1 % × 7 500 = 75 par tirage
  Légendaire, contre 1,33 % × 5 071 = 67).

**Coffres fusionnés** (la plupart des récompenses, d'après LeKiwi06 le 30/10) : tirages et fréquences de
`generer_fusion.py` (même calcul que les niveaux). Valeur = valeur moyenne affichée sur le coffre, en émeraudes.

| Coffre fusionné | Tirages | Tirages Légendaires (moyenne) | A : 0,5 % | B : 1 % | C : 2 % |
|---|---|---|---|---|---|
| 500 | 15 | 0,16 | 1 sur 1 200 | 1 sur 600 | 1 sur 300 |
| 1 000 | 16 | 0,45 | 1 sur 450 | 1 sur 220 | 1 sur 110 |
| **2 500** | 24 | 1,13 | 1 sur 180 | **1 sur 89** | 1 sur 45 |
| 5 000 | 26 | 2,26 | 1 sur 89 | 1 sur 45 | 1 sur 23 |
| 10 000 | 29 | 6,67 | 1 sur 30 | 1 sur 15 | 1 sur 8 |

### 3.6 Effets négatifs du jeu (26.3), du moins au plus dangereux (liste demandée le 30/10)

Lue dans le code du jeu (`MobEffects`, catégories HARMFUL et NEUTRAL). Dégâts : 1 PV = ½ cœur.

| # | Effet | Ce qu'il fait | Niveaux utiles | Avis de Claude |
|---|---|---|---|---|
| 1 | Malchance | baisse la chance du butin (pêche, coffres) | non | inutile |
| 2 | Surbrillance | contour visible à travers les murs par les joueurs | non | inutile (sauf PvP) |
| 3 | Charge venteuse | à la mort : explosion de vent | non | inutile |
| 4 | Tissage | à la mort : toiles d'araignée autour | non | presque rien |
| 5 | Suintement | à la mort : 2 slimes | non | presque rien |
| 6 | Faim | vide la barre de faim (≈ 1,5 point de faim par minute au niveau I, saturation d'abord) | oui | gardé |
| 7 | Fatigue de minage | minage − 70 % (I), − 91 % (II), quasi bloqué (III) ; frappe plus lente | jusqu'à III | gardé |
| 8 | Faiblesse | − 4 dégâts au corps à corps par niveau | oui | gardé |
| 9 | Infestation | 10 % de chance, quand on est touché, de faire sortir des poissons d'argent | non | possible |
| 10 | Lenteur | − 15 % de vitesse par niveau | oui | gardé |
| 11 | Obscurité | la vue s'assombrit par vagues | non | gardé |
| 12 | Poison | 1 PV toutes les 1,25 s (I), 0,6 s (II), 0,3 s (III) ; s'arrête à ½ cœur | oui | gardé |
| 13 | Nausée | l'écran tangue | non | déconseillé (malaise réel) |
| 14 | Cécité | vue à quelques blocs, pas de sprint ni de coup critique | non | possible, très dur près de la lave |
| 15 | Wither | 1 PV toutes les 2 s (I), 1 s (II), 0,5 s (III), peut tuer, l'armure ne protège pas | oui | gardé, tard |
| 16 | Lévitation | fait monter ; chute à la fin | oui | déconseillé (mort quasi sûre) |
| 17 | Dégâts instantanés | 6 PV d'un coup par niveau (remis toutes les 5 s : mort en 20 s) | oui | écarté |

Neutres sans effet dans le Nether, écartés : mauvais présage, présage de raid, présage d'épreuve.

## 4. Questions ouvertes

Réglées : taux de l'amulette (5, 29/10), liste et ordre des effets, mort et descente, tirage, couleur sur Bedrock,
valeur de l'amulette (7 500) (30/10).

Réglés aussi le 30/10 : lévitation en 10 s + 10 s, durée du plastron, amulette au butin (**C : 2 %**).

Aucune question ouverte.

## 5. Plugins

- Nouveau : **KS_MaleficesNether** (compteur, maléfices, amulette).
- Touchés : KS_Jetons (rangée « Équipements spéciaux » + accès pour les autres plugins), KS_Atouts (plastron),
  KS_Effets (effet « Maléfice du Nether »), geyser-bedrock et KS_Textures (amulette), KS_Economy (prix de l'amulette),
  KS_KaliumGive (id pour le butin), tables de butin des récompenses, KS_Didacticiel.
