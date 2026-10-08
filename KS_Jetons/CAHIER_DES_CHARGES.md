# Cahier des charges - Catégorie 7 : Déplacements, jetons, coffres de mort - LeKiwi06

Publié au déploiement de KS_Jetons (03/10/2026 ; mis à jour le 08/10/2026 : KS_Jetons 2.0.0 et 2.0.1, KS_CoffreMort) ;
restent à coder : KS_Fly, KS_Teleport, jetons de claim.

Serveur : **Event** (bêta ; destination future : Kixster SMP). Né de la revue des plugins du 03/10/2026 (proposition
« Claims : se téléporter à mon claim »). État : **en cours de rédaction** (réponses de LeKiwi06 du 03/10/2026 ;
ajouts « jetons et badges » du 08/10/2026 en parties 6 à 11 ; toutes les réponses reçues le 08/10/2026 ;
KS_Jetons 2.0.0 déployé sur Event le 08/10/2026 à 19:09, non testé).

# 1. Demande d'origine (03/10/2026, fait foi)

> il va falloir un tp gratuite (style /home ) par heure et les suivantes sont payantes en achetant des jetons de tp . par défaut : /home bed et / spawn uniquement , on pourra débloquer des emplacements de téléportation avec des jetons de localisations
>
> ( qui se craft avec une boussole de localisation pointant vers la destinations + 8 blocks de verre autour , le block de magnétite deviens indestructible si une personne l'utilise comme point de tp , si la dernière personne à l'utiliser comme point de tp retire cette destinations de ses emplacements , le bloc est de nouveau cassable )
>
> dans la meme logique on va créer des jetons de claim gagnable et qui offre une utilisation de claim gratuite ( ça ne fait pas monter le prix des claims , donc si on revend tout claims, on arrive a 11 claims gratuits dispos + 1 par jeton utilisé , ça stock " utilisation gratuites " dans l'interface pour claim histoire qu'on voit) et des jeton de la mort ( jeton utilisable après être mort et qui donne la localisation exact d'un de ses coffre de mort actifs)
>
> coffre de mort : quand un joueur meurt , son loot est stocké dans un coffre ouvrable et cassable seulement par lui pendant 15 minutes, après ce délai là il disparait et le stuff aussi , une personne ne peux pas avoir plus de 3 coffres de mort actif en simultané , si un 4 eme doit spawn , on supprime le plus ancien , si il meurt dans une zone protégé ou il ne peux ni casser ni poser ou dans un de ses claims , alors son coffre de mort attéris dans le /rewards . ( notifier le joueur quand c'est le cas , cette règle sert a ce que nos claims soit une zone safe et éviter de rendre la récupération impossible si on meurt dans une zone protégé a laquelle on a pas la permissions )

# 2. Réponses de LeKiwi06 (03/10/2026)

> jeton de tp acheté avec le score ,( ou gagné via le reward ) si la personne en a d'avance dans son inventaire virtuel ça les consomme (faire un onglet inventaire spécial pour les jetons , histoire de les y stocker et les en retirer quand on veux , fait un onglet dans les menus pour y accéder ) si la personne veut se tp avec la tp gratuite ça lui met le cooldown si la personne n'a pas de jeton et qu'elle veut se tp hors cooldown : lui proposer de payer x points pour un jeton de tp , si elle valide et elle est débitée et téléportée

> on va fixer les prix plus tard , mais retiens " + ou - 15 minutes de farm pour s'offrir une tp " / je valide / par défaut seulement /home bed et le /spawn , et on pourra avoir jusqu'a 20 emplacements , mais pour les débloquer il faut des jeton d'emplacements ( concernant les jeton de localisation , changeons le nom pour " point de transport " histoire de pouvoir les distribuer aux gens qui auraient des slot de libre ) / dans la même idée que pour éviter les abus avec les slots de tp , fait en sorte qu'un slot libérer ne soit remodifiable que 15 minutes plus tard / on fera des achats pour ça en points , on trouvera des ordre de grandeur adapté pour les prix plus tard / c'est bien ça pour les jetons de claim , un jeton utilisé augmente le stock de claim gratuit de 1 de façon permanente , il est possible de retirer les jetons utilisé pour les revendre , mais il faut soit avoir un slot de claim dispo pour le faire , soit payer la somme du claim suivant ( ce qui aurait du être le cas si la personne n'avait pas eu ce jeton ) / le jeton de la mort permet de choisir quel coffre en montrant le contenue ( comme pour le pvp kit ) afficher aussi les délai restant avant despawn . / l'xp est stocké dans le coffre , on en perd que la moitié à chaque mort ( comme les items et les farms sont rendu difficile , perdre tout son grind d'xp ferait trop ragequit , donne une bouteille d'xp custom avec la moitié des points d'xp que la personne avait)

# 3. Décisions

| Sujet | Décision |
|---|---|
| Jetons | De téléportation, d'emplacement, de claim, de la mort : achetés avec le score ou gagnés via /rewards ; **inventaire de jetons** (onglet « Jetons » des menus : déposer, retirer quand on veut) |
| Prix | Plus tard ; repère : **± 15 minutes de farm pour une TP** ; réglables (valeurs provisoires dans la config) |
| TP gratuite | 1 par heure (délai lancé par la TP gratuite) ; ensuite un jeton de l'inventaire de jetons ; sans jeton : proposer de payer le prix d'un jeton, débit puis TP |
| Délai avant de partir | 5 s sans bouger, annulé par un dégât (validé) |
| Destinations | Par défaut `/home` (lit) et `/spawn` seulement ; jusqu'à **20 emplacements**, chacun débloqué par un **jeton d'emplacement** |
| Point de transport (ex « jeton de localisation ») | Craft : boussole de localisation (liée à une magnétite) + 8 blocs de verre autour ; objet qu'on peut donner ; l'utiliser enregistre sa magnétite dans un emplacement libre |
| Magnétite | Indestructible tant qu'au moins un joueur l'a dans ses emplacements ; de nouveau cassable quand le dernier la retire |
| Emplacement libéré | Remodifiable seulement **15 minutes** plus tard (anti-abus) |
| Jeton de claim | Utilisé : +1 claim gratuit **permanent** (stock affiché dans l'interface des claims) ; ne fait pas monter le prix ; retirable (pour revendre) si un claim gratuit est disponible, sinon en payant le prix du claim suivant |
| Jeton de la mort | Choisir un de ses coffres de mort actifs en voyant son contenu (comme le PvP Kit) et le temps restant ; donne ses coordonnées exactes |
| Coffre de mort | Objets + XP ; ouvrable et cassable par le mort seulement pendant 15 min, puis disparaît avec son contenu ; 3 actifs au plus (le 4e supprime le plus ancien) ; mort dans une zone où il ne peut ni casser ni poser, ou dans un de ses claims : contenu envoyé dans /rewards, avec un message |
| XP à la mort | La moitié est perdue ; l'autre moitié en **fiole d'expérience custom** (points) dans le coffre |

# 4. À trancher (proposé par Claude)

- `/spawn` placé par un admin (`/setspawn`).
- TP vers une magnétite d'une autre dimension : permise ?
- Verre du craft : n'importe quel bloc de verre (teinté compris, pas les vitres) ?
- Mort dans le vide ou la lave : coffre sur le bloc sûr le plus proche ; aucun : /rewards.
- Pas de coordonnées à la mort (c'est le rôle du jeton de la mort).
- Objets des jetons : livre de connaissances (comme nos autres objets custom) ; joueurs Bedrock : correspondances à
  ajouter dans `geyser-bedrock` (Maxster33).

# 5. Plugins prévus

- **KS_Jetons** (nouveau) : objets jetons, inventaire de jetons (onglet du menu d'Event), achat avec le score ;
  id_custom pour /rewards (KS_KaliumGive).
- **KS_Teleport** (nouveau) : /home, /spawn, emplacements, points de transport, magnétites, TP gratuite et payante.
- **KS_CoffreMort** (nouveau) : coffres de mort, fiole d'XP, jeton de la mort, envoi dans /rewards.
- Modifiés : KS_Claim (jetons de claim), KS_RewardsGUI (dépôt local d'une récompense), KS_KaliumGive (ids des jetons).

# 6. Ajouts et modifications du 08/10/2026 : jetons et badges (fait foi sur les parties 1 à 5 quand elles diffèrent)

> liste des derniers ajouts et modifications pour jetons et badges de  kixster SMP
>
> - faire un inventaire spécial pour les jetons et badges via un bouton inventaire spécial
> - les jetons : utilise le visuel d'un lingot
> jeton de Fly : lingot de fer
> ( pour rappel : ce jeton n'est obtenable que par rewards , et permet de fly pendant 10 minutes dans ses claims , il s'active via un fenetre qui s'ouvre quand une personne essaie de fly sans jeton ou badge de fly actif  option " ne plus m'afficher cette fenetre " si la personne veux activer son jeton automatique chaque fois qu'elle essaie de fly sans jeton ou badge activés )
>
> jeton de la mort : lingot de netherite
> ( changement : ce jeton ne donne plus la localisation de son point de mort , il permet de récupérer directement le coffre de mort choisis dans le reward )
>
> Jeton de téléportation : lingot d'or
> ( pour rappel : ce jeton est achetable a tout moment contre une quantité d'emeraudes , il permet d'outrepasser le cooldown de téléportation d'une heure , ce qui le remet donc a 1h )
>
> jeton de localisation : lingot de cuivre
> ( pour rappel : ce jeton se craft avec une boussole qui mointe vers une magnétite entouré de block de cuivre ciré dans une table de craft , il permet d'enregistrer une localisation de téléportation , si la magnétite est enregistrer comme point de tp pour quelqu'un elle est indestructible , et si elle a été cassé avant qu'un jeton de localisation soit utilisé pour elle , alors refuser l'utilisation du jeton )
>
> les badges sont des version améliorer des jetons et ne s'obtiennent que via les rewards et events  , ils s'améliorent en fusionnant 2 badges du même niveau dans une enclume
> on ne peux pas porter simultanément 2 badges du même type
> les badges ont l'apparence du block créés a partir des lingots de leur jetons correspondant
> contrairement aux jetons que l'on peux accumuler et utiliser quand on le souhaite , les abilités des badges sont non stackable ( en gros on ne peux pas dépasser la capacité maximum d'abilité stocké par le badge , meme en attendant le double cooldown)
>
> badge de fly : 1 minutes de fly , cooldown de 1h
> chaque niveau supérieur : niveau 2 : 3 minutes par heures , niveau 3 : 5 minutes par heures , niveau 4 : 10 minutes par heures , niveau 5 : 15 minutes par heures
>
> badge de la mort : 1 récupération de stuff gratuite par semaine IRL , non stackable
> chaque niveau supérieur :  niveau 2 : tout les 6 jours  , niveau 3 : tout les 5 jours  , niveau 4 : tout les 4 jours  , niveau 5 :tout  les 3 jours , niveau 6 : tout les 2 jours , niveau 7 : tout les jours
>
> badges de téléportation : 1 téléportation gratuite supplémentaire stocké ( chaque téléportations a son propre cooldown d'une heure )
>
> chaque niveau supérieur :  niveau 2  : 2 tp supplémentaire stockés , niveau 3  : 3 tp supplémentaire stockés , niveau 4  : 4 tp supplémentaire stockés, niveau 5  : 5 tp supplémentaire stockés

# 7. Décisions du 08/10/2026 (ce qui est clair dans la partie 6)

| Sujet | Décision |
|---|---|
| Inventaire spécial | Contient les jetons **et les badges** ; ouvert par un bouton « inventaire spécial » (existant en 1.0.0 : bouton « Jetons » du menu et `/jetons`, jetons seulement) |
| Apparence des jetons | Un lingot : fly = fer, mort = netherite, téléportation = or, localisation = cuivre (remplace œil de l'Ender et crâne de la 1.0.0) |
| Jeton de fly (nouveau) | Obtenu **seulement** via /rewards ; 10 minutes de vol **dans ses claims** ; s'active par une fenêtre qui s'ouvre quand le joueur essaie de voler sans jeton ni badge de fly actif ; option « ne plus m'afficher cette fenêtre » = un jeton est activé d'office à chaque essai |
| Jeton de la mort (changement) | Ne donne **plus** les coordonnées ; récupère directement le coffre de mort choisi (remplace la ligne « Jeton de la mort » de la partie 3) |
| Jeton de téléportation | Achetable à tout moment contre des émeraudes ; passe outre le délai d'une heure ; le délai repart à 1 h |
| Jeton de localisation | Craft : boussole liée à une magnétite, entourée de **blocs de cuivre ciré** (remplace les 8 blocs de verre) ; enregistre une destination ; magnétite indestructible tant qu'elle est enregistrée chez quelqu'un ; magnétite cassée avant l'utilisation du jeton : utilisation refusée |
| Badges | Versions améliorées des jetons ; obtenus **seulement** via /rewards et events ; 2 badges du même niveau fusionnés dans une enclume = niveau supérieur ; jamais 2 badges du même type portés à la fois ; apparence : bloc du lingot du jeton (fer, netherite, or) ; capacité **non cumulable** (jamais au-delà du maximum du badge, même après deux délais) |
| Badge de fly | Par heure : niveau 1 = 1 min, 2 = 3 min, 3 = 5 min, 4 = 10 min, 5 = 15 min |
| Badge de la mort | 1 récupération gratuite tous les : niveau 1 = 7 jours réels, 2 = 6, 3 = 5, 4 = 4, 5 = 3, 6 = 2, 7 = 1 |
| Badge de téléportation | TP gratuites supplémentaires stockées : niveau 1 = 1, 2 = 2, 3 = 3, 4 = 4, 5 = 5 ; chaque TP a son propre délai d'une heure |

Nécessaire techniquement (à signaler) :
- « Essayer de voler » en survie = double saut : le serveur ne le voit que si le vol est autorisé pour le joueur. Le
  plugin l'autorise donc dans ses claims et intercepte l'essai (fenêtre, jeton ou badge), sinon l'annule.
- Les jetons et badges restent des livres de connaissances avec l'apparence du lingot ou du bloc : un badge ne peut
  donc pas être posé comme un bloc. Joueurs Bedrock : correspondances dans `geyser-bedrock` (Maxster33).
- Magnétite « indestructible » : aussi contre les explosions et les pistons.
- Fusion à l'enclume : à accorder avec KS_Enclume (Maxster33), qui agit sur les mêmes événements.

# 8. Questions du 08/10/2026 et réponses de LeKiwi06 (08/10/2026, font foi)

Questions de Claude (résumé) : 1 « porter » un badge = rangé dans l'inventaire spécial, 1 case par type, délais
attachés au joueur ; 2 forme de l'inventaire spécial ; 3 coût de la fusion ; 4 jetons d'emplacement et de claim, nom du
jeton de localisation, badge de localisation ; 5 « ses claims » ; 6 décompte du fly (jeton : 10 min d'affilée dès
l'activation ; badge : minutes décomptées en vol, réserve remplie d'un coup 1 h après le début de son utilisation) ;
7 fin du temps ou sortie du claim en vol ; 8 ordre badge puis jeton ; 9 récupération du coffre de mort (choix du coffre,
contenu envoyé dans /rewards) ; 10 obtention du jeton de la mort ; 11 jeton de TP et délai ; 12 cuivre ciré (8 blocs,
4 stades d'oxydation, pas les variantes taillées) ; 13 points de la partie 4 ; 14 Event d'abord puis Kixster.

> 1 : oui c'est ça je valide
> 2 : plutot l'interface des hopper , il y a 5 emplacements c'est bien si on veux rajouter des badges a l'avenir , et il faut aussi un contenant style hopper pour les jetons  , on peux bloquer les cases excédentes avec des barrier non déplaçable
> 3 :  non il faut des prix super cher , par exemple 30 niveaux pour passer au niveau 2 , 50 pour le lvl 3 , etc
> 4 :  oui j'ai oublié le badge de localisation , qui permet d'avoir des slots supplémentaires de localisation  il faut le mettre
> le jeton de claim aussi doit etre fait comme expliqué la dernière fois
> et oui le jeton de localisation c'est bien le point de transport , je préfère " jeton de localisation " c'est plus cohérent avec le reste
> le jeton d'emplacement n'a pas lieu d'etre , comme on a le badge de localisation qui sert a enregistrer ses jeton de localisations .
>
> 5 : propriétaire + membres
> 6 :  oui ça me va
> 7 :  il faut afficher le compteur de fly via une bossbar , si la personne chute , c'est de sa faute , il faut conserver les dégats de chute
> 8 : oui prioriser l'utilisation d'un badge avant un jeton
> 9 :  oui c'est bien ça   , par défaut on peux tout de même voir les coordonnées et le temps de nos coffres de morts , le jeton et le badge servent a récupérer de façon certaine son stuff , mais coutera super cher ducoup car très recherché
> 10 : seulement via rewards
> 11 : oui c'est ça
> 12 :  je valide
>
> 13 :
>
> le /spawn ne fonctionnera qu'une fois que le  joueur aura déclaré  sa maison au spawn ( même fonctionnement que pour un magasin  avec un /maison create ( prix 500 emeraudes )  dans un claim de la zone du spawn )
>
> oui la téléportation interdimension est possible  si la dimension est ouverte
>
> sur le block d'air ou de fluide le plus proche pour le coffre de mort , si une personne meurt dans la lave : remplacer le block de lave par le coffre , dans le vide de l'end sur la couche constructible la plus basse du monde aux coordonnées de la mort
>
> si les coordonnées et le cooldown de chaque coffres est affichés , le jeton et le badge sert surtout a récupérer de façon sur son stuff .
>
> 14 : d'accord on va faire ça

# 9. Décisions du 08/10/2026 (suite ; complètent la partie 7)

| Sujet | Décision |
|---|---|
| Badge « porté » | Actif quand il est rangé dans l'inventaire spécial ; 1 par type ; retirable (don, fusion) ; délais attachés au joueur, pas au badge |
| Inventaire spécial | Deux contenants à l'interface d'entonnoir (5 cases) : un pour les badges, un pour les jetons ; cases en trop bloquées par une barrière non déplaçable |
| Fusion à l'enclume | Très chère : 30 niveaux pour le niveau 2, 50 pour le niveau 3, « etc. » (interprété : + 20 par niveau, réglable) |
| Badge de localisation (ajout) | Donne des emplacements de localisation supplémentaires, où enregistrer ses jetons de localisation |
| Jeton d'emplacement | **Supprimé** (remplacé par le badge de localisation) |
| Jeton de claim | Gardé, comme décidé le 03/10 |
| Nom | « Jeton de localisation » (plus « point de transport ») |
| Fly : claims | Ceux dont le joueur est propriétaire **ou membre** |
| Fly : décompte | Jeton : 10 min d'affilée dès l'activation. Badge : minutes décomptées en vol ; réserve remplie d'un coup 1 h après le début de son utilisation |
| Fly : affichage et fin | Compteur en **barre de boss** ; dégâts de chute **conservés** |
| Fly : ordre | Badge d'abord, puis jeton |
| Coffres de mort : informations | Coordonnées et temps restant de ses coffres visibles **sans jeton** (annule « pas de coordonnées à la mort » de la partie 4) |
| Jeton et badge de la mort | Choix du coffre (contenu, temps restant), contenu (objets + fiole d'XP) envoyé dans /rewards, coffre posé retiré : récupération certaine |
| Jeton de la mort : obtention | /rewards seulement (plus d'achat avec le score) ; objet de grande valeur |
| Jeton de TP | Relance le délai de la TP gratuite à 1 h ; avec un badge, ne sert que quand toutes les TP stockées sont vides, sans toucher à leurs délais |
| Craft du jeton de localisation | Boussole liée + 8 blocs de cuivre ciré (4 stades d'oxydation, pas les variantes taillées) |
| `/spawn` | Ne marche qu'après avoir déclaré sa maison au spawn : `/maison create` (500 émeraudes) dans un claim de la zone du spawn, même fonctionnement qu'un magasin |
| TP entre dimensions | Permise si la dimension est ouverte |
| Coffre de mort : emplacement | Bloc d'air ou de fluide le plus proche ; mort dans la lave : le coffre remplace le bloc de lave ; vide de l'End : couche constructible la plus basse du monde, aux coordonnées de la mort |
| Serveur | Code et tests sur Event d'abord, puis envoi sur Kixster |

# 10. Dernières questions et réponses de LeKiwi06 (08/10/2026, font foi)

Questions de Claude : 1 badge de localisation (emplacements par niveau, niveau maximal, emplacements sans badge) ;
2 `/spawn` (maison du spawn ou point de spawn du serveur ; quelle région) ; 3 coût de fusion après 30 et 50 (+ 20 par
niveau : 70, 90, 110, 130) ; 4 jeton de claim toujours achetable ; 5 déploiement de KS_Jetons 2.0.0 sur Event.

> 1 :  les joueurs on /home bed par défaut , une fois qu'ils auront fait /maison create dans un claim présent dans la region world guard du spawn , il auront le /spawn , ensuite le badge permet d'avoir plus d'emplacement que ces 2 là , le niveau 1 donne un slot , le niveau 2 un 2eme , etc
>
> 2 : oui il tp chaque joueur à sa maison  , la region zone_shop est maintenant zone_spawn  , qui permet de créer maisons et magasins
>
> 3 : ça me conviens
> 4 : met 20 emeraudes
> 5 : oui vas y

| Sujet | Décision |
|---|---|
| Destinations | `/home bed` par défaut ; `/spawn` (TP à **sa maison** du spawn) une fois `/maison create` fait dans un de ses claims de la région du spawn ; les emplacements en plus viennent **seulement** du badge de localisation |
| Badge de localisation | Niveau 1 = 1 emplacement, niveau 2 = 2, etc. (niveau maximal non précisé : 5 dans KS_Jetons 2.0.0, réglable) |
| Région WorldGuard | `zone_shop` devient **`zone_spawn`** : maisons et magasins (à changer dans KS_Economy : `magasins.region`, et dans WorldGuard par l'humain) |
| Coût de fusion | 30, 50, 70, 90, 110, 130 niveaux (validé) |
| Jeton de claim | Achetable avec le score : **20 émeraudes** |

Reste ouvert : niveau maximal du badge de localisation (le cahier du 03/10 parlait de 20 emplacements au plus).

# 10 bis. Corrections de LeKiwi06 (08/10/2026, 21 h ; font foi sur tout ce qui précède)

> la nether star ne doit pas etre dans un coffre de mort  , ce n'est pas le jeton de claim qui coute 20 mais le jeton de téléportation  .  et ce n'est pas quand un meurt dans notre claim que le coffre arrive dans reward , c'est dans les claims des autres

| Sujet | Décision |
|---|---|
| Étoile du menu | Jamais dans un coffre de mort (KS_Menu 1.2.0 de Maxster33 la retire des objets de la mort) |
| Jeton de téléportation | **20 émeraudes** |
| Jeton de claim | Prix non fixé (pas en vente) : les « 20 émeraudes » ne le concernaient pas |
| Coffre de mort et claims | Dans **ses** claims : coffre posé. Dans le **claim d'un autre** : contenu dans /rewards (corrige « ou dans un de ses claims » des parties 1, 3 et 9) |

# 11. Plugins (ordre de code)

1. **KS_Jetons 2.0.0** (jetons en lingots, badges, inventaire spécial en entonnoirs, fusion) + KS_KaliumGive 1.9.0 (ids) :
   **déployés sur Event le 08/10/2026 à 19:09, non testés**.
2. **KS_CoffreMort** 1.0.0 (nouveau) + KS_RewardsGUI 1.1.0 (dépôt local d'une récompense) : **déployés sur Event le
   08/10/2026 à 20:56, non testés**.
3. **KS_Fly** (nouveau : vol dans ses claims, barre de boss).
4. **KS_Teleport** (nouveau : /home, /spawn et /maison, jetons de localisation, magnétites).
5. KS_Claim (jetons de claim), KS_Economy (`rachats.csv` : `jeton_emplacement` à retirer, valeurs des nouveaux objets
   à fixer avec LeKiwi06).
