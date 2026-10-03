# Cahier des charges - Catégorie 7 : Déplacements, jetons, coffres de mort - LeKiwi06

Publié au déploiement de KS_Jetons (03/10/2026) ; catégorie encore en rédaction.

Serveur : **Event** (bêta ; destination future : Kixster SMP). Né de la revue des plugins du 03/10/2026 (proposition
« Claims : se téléporter à mon claim »). État : **en cours de rédaction** (réponses de LeKiwi06 du 03/10/2026).

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
