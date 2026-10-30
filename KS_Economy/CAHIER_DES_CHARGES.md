# Cahier des charges - Catégorie 2 : Économie (KS_Economy, KS_Menu, Vault ; magasins maison) - LeKiwi06

Publié au déploiement (1re partie le 01/10/2026, magasins le 03/10/2026). Tradeshop abandonné le 02/10/2026 (voir « Magasins »).

Serveur : **Event** uniquement (bêta ; destination future : Kixster SMP). État : **cahier validé par LeKiwi06 le 29/09/2026** (session 2).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_Economy :
>  plugin d'interface pour les transition emeraude  : item < - > score , avec interface pour utiliser tradeshop ( possibilité de présenter son magasin avec prix affichés dans l'interface , possibilité de prélever son paiement et restock a distance  , possibilité d'acheter a distance également )

> ajouter les plugin : [...] Tradeshop [...]

Décision de la session 1 : le **bloc d'émeraude compressé** (ingrédient de l'Élixir de Fortune) est donné par
KS_Economy quand on repasse du score en objet, au prix de **900 émeraudes** du score.

## To do list du Kixster SMP (fichier daté ; la demande du 29/09 fait foi en cas de différence)

> économie : système comme le plugin tradeshop mais connecté a placeholderAPI pour faire des graphiques de ventes hebdo a mettre sur discord ( le bot discord sera un prochain script détaillé ) paiement possible " emeraude dématérialisés" ou troc contre un autre item , possibilité de prix et vente multiple , peut se faire dans tout les contenant autorisé .
> émeraude dématérialisés : possibilités de transformer ses emeraudes en scoreboard pour les paiement dans le système d'économie , impossible de retirer ses émeraudes du scoreboard , transformer tout les émeraudes . met en place une variable pour une taxe de convertion de l'emeraude setup sur 1 : 1 pour le moment  ( si on a besoin de contrer l'inflation a l'avenir on utilisera cette variable par exemple ) . le shop se créer via un panneau sur le contenant , contenant acceptés : tonneaux , coffres , shulker ( double coffres acceptés , possibilité de plusieurs panneaux sur le meme contenant )  si l'option est jugé trop instable pour la sortis , je te fournirais un plugin d'économie compatible , tu pourras le customiser pour inclures mes demandes .

## Ce qu'est Tradeshop (vérifié le 29/09/2026)

- Boutiques par panneau sur un contenant (coffres, tonneaux, shulkers...), compatible Paper 1.21.x / 26.1 / 26.2.
- **Troc uniquement** : « it does not use soft currency [...] instead, it trades one item for another ». Pas de
  monnaie virtuelle, pas de Vault.
- Possède un « Addon Framework » (API) ; ce qu'il permet (lecture des boutiques, achat / réassort à distance) reste
  à vérifier dans son code avant le développement.

# 2. Réponses de LeKiwi06

| Question | Réponse (29/09/2026) |
|---|---|
| Score et Tradeshop | **Option A** : les boutiques Tradeshop restent en objets ; KS_Economy fait le lien avec le score |
| Taux et taxe | 1 émeraude = 1 point ; taxe de conversion réglable (config), 1:1 au départ, dans les deux sens |
| Dépôt | Émeraudes, blocs d'émeraude, et **tous les tiers de blocs compressés** (puissances de 10, jusqu'à ×1M) ; bouton « tout déposer » |
| Retrait | Oui (émeraudes, blocs, blocs compressés du tier 1 (×10) au tier 6 (×1M)) |
| Affichage du score | Visible par défaut ; un joueur peut le masquer contre **1 % de son argent par jour** |
| /pay | Non : à la place **`/echange`** : joueurs à 5 blocs maximum l'un de l'autre, échange d'objets et de monnaie, panneau d'échange inspiré de Dofus |
| Ouverture | Bouton dans l'étoile du Nether (`/menu on`) + commande |
| Magasins | Chaque joueur déclare un magasin dans un **groupe de claim** ; il contient tous les coffres de trade du claim (**10 maximum**) ; 1er magasin : **5 blocs compressés tier 3 (45 000 points)** ; **1 magasin par personne** pour l'instant. Fiche : nom, propriétaire, coordonnées, courte description. Thème obligatoire, contrôlé par les modérateurs en jeu (**rien dans le code**) |
| Réassort / paiement à distance | Ouvrir l'interface du coffre à distance pour prélever les paiements et remettre des objets de l'inventaire ; **pendant ce temps, aucun échange possible avec ce coffre** (sur place ni à distance), contre la duplication |
| Achat à distance | **Troc uniquement** (le joueur peut retirer son score en objets par KS_Economy) |
| Graphiques Discord | Pas pour le moment |
| Chargement des chunks, vérification de l'API Tradeshop | D'accord |
| Valeur des tiers | Tier n = 10^n blocs d'émeraude = 9 x 10^n points (tier 3 = 9 000), validé |
| Élixir de Fortune | Demande un bloc compressé **tier 3** (raccord avec Chance III) ; aucun autre Élixir de Fortune |
| Obtention des blocs compressés | Uniquement par KS_Economy ; **ni posables ni décraftables** ; stockables dans des contenants |
| Score visible | **Par les autres joueurs** ; le masquer sert à cacher sa fortune, le révéler quand on veut |
| Prélèvement de 1 % | Chaque jour à minuit (Paris), arrondi à l'unité inférieure, réaffichage gratuit, rien si solde nul : validé |
| /echange | **2 joueurs** ; une interface pour saisir le montant, une interface de coffre pour déposer les objets ; le reste validé |
| Étoile du Nether sur Event | Créer **KS_Menu** (menu du serveur Event, avec son étoile) |
| Ordre de codage | KS_Economy en 2 temps : score, blocs, /echange d'abord ; magasins après KS_Claim |
| Paiement de la création du magasin | Au choix : score ou 5 blocs tier 3 |
| Coordonnées | Point défini par le propriétaire (`/magasin position`) |
| 11e boutique dans le groupe de claim | Refusée avec un message |
| Textes | Nom 32 caractères, unique ; description 100 caractères |
| Accès à distance | Par le **propriétaire** ; coffre verrouillé pour tous, déverrouillé à la fermeture ou à la déconnexion |
| Partie 4 | **Validée le 29/09/2026** |

# 3. Questions restantes

Aucune.

# 4. Choix d'interprétation (validés le 29/09/2026)

## Plugins et installations

| Élément | Rôle |
|---|---|
| **KS_Economy** (nouveau) | Score (émeraudes dématérialisées), blocs compressés, conversion, `/echange`, magasins |
| **KS_Menu** (nouveau) | Menu du serveur Event : une étoile du Nether donnée par `/menu on` (objet d'interface de KLM_Menu, comme KG_Menu / KV_Menu) ; les plugins d'Event y ajoutent leurs boutons. Au départ : un seul bouton, « Économie » |
| **Tradeshop** (installé) | Boutiques de troc par panneau, inchangé |
| **Vault** (ou VaultUnlocked, installé) | Nécessaire techniquement (signalé) : KS_Economy s'y déclare comme économie, pour que SimpleClaimSystem (session 3) puisse faire payer en score |

## Le score

- 1 émeraude = 1 point. Taxe de conversion dans `config.yml` (valeur par défaut dans le code : 1:1), appliquée au
  dépôt et au retrait.
- Interface « Économie » (`/economie` ou bouton de KS_Menu) : solde ; **déposer** (objet par objet ou « tout
  déposer » : émeraudes, blocs d'émeraude, blocs compressés de tous les tiers, pris dans l'inventaire) ; **retirer**
  (le joueur choisit l'objet et la quantité ; refusé si l'inventaire est plein ou le solde insuffisant).
- **Visibilité** : le solde d'un joueur est visible des autres joueurs dans la **liste des joueurs (Tab)**, à côté de
  son pseudo. Bouton « Masquer mon solde » : prélève 1 % du solde chaque jour à minuit (heure de Paris), arrondi à
  l'unité inférieure (rien si le solde est nul) ; « Révéler mon solde » est gratuit et immédiat.

## Blocs d'émeraude compressés

| Tier | Valeur | Nom |
|---|---|---|
| 1 à 6 | 10^n blocs d'émeraude = 9 x 10^n points (90 ; 900 ; 9 000 ; 90 000 ; 900 000 ; 9 000 000) | « Bloc d'émeraude compressé (tier n) » |

- Apparence : bloc d'émeraude à aspect enchanté ; description indiquant sa valeur en points. Empilables par 64.
- Obtenus seulement par le retrait de KS_Economy. **Impossibles à poser et à décrafter** ; utilisables dans les
  contenants, les boutiques Tradeshop, `/echange`, la création de magasin (tier 3) et la recette de l'Élixir de
  Fortune (tier 3).

## /echange (2 joueurs)

- `/echange <joueur>` envoie une demande ; l'autre accepte (message cliquable ou `/echange accepter`). Les deux
  joueurs doivent être à **5 blocs maximum**.
- Panneau d'échange : chaque joueur voit sa part et celle de l'autre (objets et montant). Bouton « Objets » : interface
  de coffre où il dépose ses objets ; bouton « Montant » : interface de saisie du montant (limité à son solde).
- L'échange a lieu quand les **deux** ont cliqué « Valider » ; toute modification d'une des parts annule les
  validations. Annulé (objets rendus) si un joueur s'éloigne à plus de 5 blocs, ferme le panneau ou se déconnecte.

## Magasins (révisés le 02/10/2026, remplacent la version du 29/09 ci-dessous)

Décisions de LeKiwi06 (02/10/2026) : Tradeshop abandonné (l'original n'est plus mis à jour depuis 2023) : **nos propres
boutiques** dans KS_Economy ; boutiques dans une **zone dédiée** ; prix en objet **ou en monnaie**.

| Question | Réponse (02/10/2026) |
|---|---|
| Plugin de boutiques | Nos propres boutiques (KS_Economy), pas de Tradeshop |
| Création d'une boutique | Poser un panneau sur un contenant ouvre une interface « Créer une boutique » / « Fermer » ; puis l'objet à vendre (écrire son nom ou le choisir dans l'inventaire), sa quantité, puis le prix : **monnaie** ou **objet** (mêmes choix que l'objet à vendre) |
| Lieu | Une région WorldGuard **`zone_shop`** pour tous les shops ; on peut y claimer ; **tout le monde construit** dans la zone |
| Magasin | `/magasin create` dans un de ses claims de la zone_shop : 5 blocs compressés tier 3 **ou** 45 000 points prélevés, magasin créé ; 10 boutiques au plus ; **1 magasin par joueur** |
| Agrandir | `/magasin agrandir` dans un autre de ses claims de la zone_shop : **10 000 points**, le claim rejoint le magasin et le magasin gagne **1 boutique** de plus |
| Boutique hors magasin | Impossible de poser une boutique dans un claim qui n'est pas dans un magasin |
| Nom de l'objet écrit | Nom français ou id du jeu, sans tenir compte des majuscules ni des accents |
| Achat sur place | Clic sur le panneau : menu avec le nombre de lots |
| Prix en monnaie | Les points payés **attendent dans la boutique** ; le propriétaire les récupère en la gérant |

### Choix d'interprétation (validés par LeKiwi06 le 02/10/2026)

- **Zone** : région WorldGuard `zone_shop` créée par LeKiwi06 avec les drapeaux `build allow` et `scs-claim allow`
  (SimpleClaimSystem refuse sinon les claims dans une région). Un claim est « dans la zone » si le centre de son chunk
  est dans la région. Nom de la région réglable (`magasins.region`).
- **Magasin** (1 par joueur) : `/magasin create` dans un de ses claims de la zone, au choix 5 blocs tier 3 de
  l'inventaire ou 45 000 points ; fiche : nom (32 caractères, unique), description (100), position (`/magasin position`,
  dans un claim du magasin). `/magasin agrandir` : 10 000 points, claim ajouté, +1 boutique. `/magasin` : menu du
  magasin. Prix réglables dans `config.yml`.
- **Claims du magasin** : un claim qui porte des boutiques ne peut être ni supprimé ni vendu (KS_Claim refuse, avec un
  message) ; sans boutique, le supprimer ou le vendre le retire du magasin (agrandissement non remboursé).
- **Boutique** : panneau posé contre (ou sur) un coffre, coffre piégé, tonneau ou shulker (coffre double compris) ;
  plusieurs panneaux sur le même contenant = plusieurs boutiques qui partagent le stock. Seul le propriétaire du
  magasin crée une boutique, dans un claim de son magasin, dans la limite du magasin.
  - Interface : « Créer une boutique » / « Fermer » (fermer : panneau ordinaire) ; objet vendu : « Écrire le nom »
    (plusieurs résultats : liste à choisir) ou « Choisir dans l'inventaire » (objets custom compris : têtes, élixirs,
    blocs compressés...) ; quantité (1 à 2 304) ; prix : « Monnaie » (points) ou « Objet » (mêmes choix + quantité) ;
    récapitulatif puis « Créer ». Le panneau affiche l'offre (« [Troc] », objet et quantité, « contre », prix).
  - Stock : le contenu du contenant ; les objets payés arrivent dans le contenant ; les points payés attendent dans la
    boutique.
  - Protection : le contenant d'une boutique ne peut pas être vidé par entonnoir ; le casser (ou casser le panneau)
    supprime la boutique, seulement pour le propriétaire (les claims protègent déjà des visiteurs).
- **Achat sur place** : clic droit sur le panneau : offre, stock (lots disponibles), nombre de lots à acheter ; refusé
  si le stock, le paiement (objets de l'inventaire ou solde), la place dans l'inventaire de l'acheteur ou la place du
  contenant pour le paiement manquent.
- **Catalogue** (menu Économie, bouton « Magasins ») : tous les magasins (nom, propriétaire), puis les boutiques d'un
  magasin (objet, prix, stock) ; **achat à distance** : même menu que sur place.
- **Gestion à distance** (propriétaire, menu du magasin) : ouvrir le contenu d'une boutique (prélever, réassortir) et
  récupérer ses points ; pendant ce temps la boutique est **verrouillée** (ni achat ni ouverture, sur place ou à
  distance) jusqu'à la fermeture ou la déconnexion ; le chunk est chargé le temps de l'opération.

### Retours de LeKiwi06 (03/10/2026, KS_Economy 1.1.2)

> pour le plugin de magasin : le nom ne s'affiche pas en français sur les coffres , si il est composé de plusieurs mots un seul s'affiche , j'ai aussi essayé de chercher un item avec bloc de diamant , et diamond_block , aucun des 2 n'a marché , on ne peux pas renommer nos boutiques dans notre magasin elles sont numérotés c'est nul , aucun indicateur n'est présent visuellement pour voir des rupture de stock etc dans l'interface ou sur le coffre , et on ne peux pas utiliser les divers coffres de cuivre comme shop c'est dommage .
>
> il faut aussi que le panneau sur le coffre soit détruit si on supprime un shop a distance , ajouter aussi une option pour le fermer temporairement et ajouter un cooldown de 3h avant de pouvoir reposer un shop qu'on a supprimé ( pour eviter les switch abuse )

Choix d'interprétation (Claude, à valider aux tests) :
- **Noms français** écrits par le plugin (panneaux et menus), quelle que soit la langue du client ; la recherche par
  nom français ou id refonctionne (fichier des noms oublié dans le jar jusqu'en 1.1.1).
- **Panneau** : 4 lignes mesurées (le jeu n'affiche d'une ligne trop longue que les premiers mots ; sinon « … ») :
  nom de la boutique (gras), lot vendu, « pour » + prix, **état** : « Stock : N lots » (vert), « Rupture de stock »
  (rouge), « Coffre plein » (paiement en objet impossible, orange), « Fermée » (rouge). Tenu à jour après un achat,
  une gestion à distance, la fermeture du coffre sur place et au chargement du chunk.
- **Nom de boutique** (20 caractères) : choisi à la création (le nom de l'objet vendu par défaut), « Renommer » dans
  la gestion ; les listes (mes boutiques, catalogue) montrent les noms, en couleur selon l'état, avec l'état écrit.
- **Fermer temporairement / Rouvrir** (gestion de la boutique) : plus aucun achat, le panneau affiche « Fermée ».
- **Suppression depuis le menu** (à distance ou non) : le panneau est retiré et rendu au propriétaire.
- **Délai de 3 h** (`magasins.delai-suppression-heures`) : après la suppression d'une boutique (menu ou panneau /
  contenant cassé), sa place dans le magasin reste prise 3 h ; message avec le temps restant.
- **Coffres en cuivre** (tous les états d'oxydation, cirés ou non) acceptés comme contenants.

### Demande de LeKiwi06 (03/10/2026, KS_Economy 1.1.3)

> il ne faut pas que les joueurs voient leurs propre boutique dans l'onglet magasins de économie , et il ne peuvent pas acheter eux même pour eviter de fausser les stats de magasins . aussi , je veux une possibilité " voir l'item" qui nous montre clairement ce qu'on va acheter dans une contenant . pour éviter touts risques d'arnaques , il faut aussi un bouton pour signaler une boutique ou un magasin au complet , pour arnaque , contenue inaproprié , etc

Choix d'interprétation (Claude, à valider aux tests) :
- **Catalogue** (Économie, « Magasins ») : son propre magasin n'y est pas (il reste dans « Mon magasin »).
- **Pas d'achat chez soi** : clic sur son panneau ou ouverture depuis un menu : la gestion de la boutique ; achat
  refusé s'il passe quand même.
- **« Voir l'objet »** (menu d'achat) : coffre en lecture seule : l'objet vendu tel quel (survol : nom,
  enchantements, description ; l'achat donne exactement des objets identiques), le résumé de l'offre, l'objet demandé
  (ou une émeraude avec le prix en points). Refermé : retour au menu d'achat.
- **Signaler** : « Signaler la boutique » (menu d'achat) et « Signaler le magasin » (fiche du magasin dans le
  catalogue) ; raisons à cocher : arnaque (objet ou prix trompeur), contenu inapproprié (nom, texte), thème du magasin
  non respecté, et « Autre » (200 caractères) ; un seul signalement non traité par joueur et par cible ; pas sur les
  siens. Message au staff connecté (`kseconomy.staff`, opérateurs par défaut) et dans la console ; enregistré dans
  `plugins/KS_Economy/signalements.yml`.
- **Staff** : `/magasin signalements` : non traités / classés ; détail (cible, auteur, date, raisons, offre) ; se
  téléporter ; supprimer la boutique signalée (sans délai de 3 h pour le propriétaire) ; classer avec l'action faite.

## Rachats de la semaine (LeKiwi06, 04/10/2026, KS_Economy 1.3.0)

> ok les prixs me paraissent bon , enregistrer cette base de donnée dans le plugin d'économie , et fait une interface ou on tire 10 items random par semaine et on les achètes aux joueurs

| Point | Règle |
|---|---|
| Base | Le barème des prix (émeraudes à l'unité) est dans KS_Economy |
| Tirage | 10 objets par semaine : on tire 10 familles au hasard, puis un objet dans chaque famille (les variantes d'un objet comptent pour un seul). Modifié le 09/10/2026 : avant, 2 objets par gamme de prix |
| Prix de rachat | Au hasard à plus ou moins 25 % du prix du barème, arrondi à l'unité |
| Lot (objet empilable) | Moins de 0,1 : 1 émeraude ; 0,1 à 1 : 10 ; 1 à 10 : 64 ; 10 à 100 : 320 ; plus de 100 : à l'unité. Quantité arrondie à l'unité inférieure, payée à l'entier le plus proche |
| Quota | Par objet, par joueur et par jour : aucun sous 25 000 de cagnotte ; au-dessus, 5 % de la cagnotte (7,5 % si elle est masquée) ; la vente qui fait dépasser passe, on bloque ensuite |

Précisions de LeKiwi06 (réponses du 04/10/2026) : gamme 1 à 10 = 64 émeraudes (le tableau, pas l'exemple du
diamant) ; « chaque famille d'item a son sous-tirage comme les têtes, il tire dans les items de sa famille qui
correspondent à la gamme de prix qu'on lui demande » (familles : diorite, cuivre, fleur, terres, spruce, potion,
améthyste, boue, terres cuites) ; ne pas compter les variantes non obtenables en survie dans l'inventaire.

Modification de LeKiwi06 (09/10/2026) :

> j'aimerai modifier le tirage des items pour les rachats de la semaine , il faudrait tirer 10 items aléatoire  en gardant le systeme de " famille " mais sans restriction de 2 par ordre de prix
>
> aussi tout a l'heure , des items issues du nether ont été tirés alors qu'il est fermé , ce n'est pas normal

Les gammes de prix ne servent plus qu'à la taille des lots. Un objet issu d'une dimension fermée dans KS_Dimensions
n'est jamais proposé, même si la dimension est fermée après le tirage (il est alors remplacé).

## Magasins (version du 29/09/2026, remplacée)

- Un joueur déclare **un** magasin dans un de ses **groupes de claim** (KS_Claim, session 3). Prix du premier
  magasin : 45 000 points, payés au choix avec le score ou 5 blocs compressés tier 3 de l'inventaire.
- Le magasin regroupe toutes les boutiques Tradeshop du groupe de claim, **10 maximum** (la 11e création est refusée
  avec un message).
- Fiche : nom (32 caractères, unique), propriétaire, coordonnées (point fixé par `/magasin position`), description
  (100 caractères). Le thème est contrôlé par les modérateurs en jeu, pas par le plugin.
- **Catalogue** (interface Économie) : tous les magasins, puis les boutiques d'un magasin avec l'objet proposé, le
  prix et le stock.
- **Achat à distance** : troc seulement, avec les objets de l'inventaire de l'acheteur ; refusé si le stock ou le
  paiement manque, ou si l'inventaire est plein.
- **Gestion à distance** (propriétaire) : ouvrir le contenu d'une de ses boutiques pour prélever les paiements et
  remettre des objets ; pendant ce temps, la boutique est **verrouillée** pour tous (sur place et à distance), jusqu'à
  la fermeture de l'interface ou la déconnexion. Le chunk est chargé le temps de l'opération.

## Autres cahiers modifiés

- Catégorie 1 (`1_Contenu_survie.md`) : l'Élixir de Fortune demande un bloc compressé **tier 3** (au lieu du bloc à 900).

# Admin shop d'Halloween et rachats à 100 objets (KS_Economy 1.13.0, LeKiwi06, 30/10/2026)

Publié au déploiement (30/10/2026). Cahier local `Halloween_AdminShop.md`, recopié tel quel.

Demande du 30/10/2026. État : **en cours de rédaction** (questions de la partie 4 à trancher avant tout code ; cahier **validé le 30/10** (« Je valide, code ») ; codé : KS_Economy 1.13.0, commit 386df7e, non déployé).
Dépend du cahier des maléfices (`Malefices_Nether.md`) pour l'amulette mangeuse de mal.

## 1. Demande (LeKiwi06, 30/10/2026), recopiée telle quelle

> Chantier Halloween (Kixster) : admin shop et boutiques étendues, dans KS_Economy. Extrait de mon annonce :
> « L'admin Shop ouvre pendant la durée de l'événement : livres mending, badges, jetons, coffres de récompenses,
> l'amulette mangeuse de mal, ... Ce shop s'ouvre très rarement. Le shop classique s'étend également à 100 items au
> lieu de 10 pendant cet événement. »
> À cadrer avant de coder (cahier local) : ouverture et fermeture par le staff, liste des objets, prix tirés du barème
> avec des ancrages (jamais par formule seule, livré en .xlsx), ce que deviennent les boutiques en trop à la fin de
> l'événement (réglage boutiques: 10 du config.yml), et la durée de l'événement (semaine ou 15 jours).
> Dépend de la discussion des maléfices pour l'amulette.

## 2. Ce qui existe aujourd'hui (KS_Economy 1.12.2, lu le 30/10)

- **Aucun shop où le joueur achète au serveur.** Le serveur ne fait que racheter : rachats de la semaine (`/rachat`,
  10 objets tirés le lundi, `rachats.objets: 10`) et marchand ambulant (10 objets). Le reste, ce sont les boutiques
  des joueurs.
- `/shop` est un alias de `/magasins` : le catalogue des magasins des joueurs **et** les rachats de la semaine.
- Magasins : `magasins.boutiques: 10` = boutiques d'un magasin à sa création (tier 1) ; agrandissements +2, +3, +5
  (tier 4 = 20 boutiques).
- Le menu des rachats a 10 cases fixes (2 rangées de 5) : 100 objets demandent des pages.
- Valeurs déjà validées des objets cités (rewards du 09/10 et barème) :

  | Objet | Valeur validée | Remarque |
  |---|---|---|
  | Livre Raccommodage | 1 500 | barème du 04/10 (« prix de départ, à réévaluer ») |
  | Badges niveau 1 | fly 450, mort 3 600, TP 600, localisation 750 | niveau N = 2 badges N-1 + XP de fusion |
  | Jetons | fly 30, mort 300, claim 1 000, TP 20 | **`rachats.csv` a encore mort 150, claim 5 000, emplacement 250** (avant le 09/10) |
  | Coffres de récompense | valeur moyenne du niveau : 1 / 5 / 10 / 100 / 1 000 (× 2 en permanent) | contenu tiré à l'ouverture |
  | Amulette mangeuse de mal | 7 500 | KS_MaleficesNether **pas encore codé** |

## 3. Réponses de LeKiwi06

| Question | Réponse (30/10/2026) |
|---|---|
| 1. « Le shop classique » à 100 items | **Les rachats de la semaine** (lecture A) : le serveur rachète 100 objets au lieu de 10 pendant l'événement. Les boutiques des magasins ne changent pas, donc la question 6 tombe |
| 2. Durée | **15 jours**, à partir du 31/10 (donc jusqu'au 14/11, heure exacte de début et de fin à fixer à l'ouverture) |
| 3. Ouverture | **`/adminshop ouvrir <durée en jours>`** (au lieu d'une date de fin) ; le reste de la proposition n'a pas été contesté |
| 4. Liste des objets | **Les objets custom qui n'ont pas de craft** (« cristal de purification, etc. »), plus ce que cite l'annonce |
| 4. Coffres de récompense | Valeurs **10, 100 et 1 000**, à **1,5 × leur valeur théorique** : **15, 150 et 1 500 points** |
| 4. Amulette mangeuse de mal | **Vendue par exception** (elle a un craft) : 7 500 × 1,5 = **11 250** ; seulement si KS_MaleficesNether est déployé |
| 4. Cœur de Spawner | **Vendu à 15 353** (10 235 × 1,5), en sachant qu'avec 7 fragments et une tête il donne un spawner |
| 4. Jeton de téléportation | **Retiré de l'admin shop** (déjà vendu 20 dans /jetons) |
| 5. Limites d'achat | **Aucune limite à l'admin shop** ; les limites restent seulement sur les rachats de la semaine (quotas actuels par objet et par jour, inchangés) |
| 7. Prix | **× 1,5 partout** : chaque objet vaut 1,5 × sa valeur validée (colonne « Prix à × 1,5 » de 5.1) |

## 4. Questions

1. **[Réglé : lecture A, rachats de la semaine]** **« Le shop classique » à 100 items**, deux lectures possibles :
   - **A. Les rachats de la semaine** (le serveur rachète 100 objets au lieu de 10) : c'est le seul « shop » du
     serveur qui compte en « items » et qui en a 10, et `/shop` l'ouvre. Fin de l'événement : le tirage suivant
     revient à 10, rien à ranger. Risque : 10 fois plus de façons de gagner des points (les quotas restent).
   - **B. Les boutiques d'un magasin** (`magasins.boutiques: 10` passe à 100) : lecture de ta note. Il faut alors
     décider du sort des boutiques en trop (question 6).
2. **[Réglé : 15 jours]** **Dates** : Halloween est demain (31/10). Début (le 31/10 ?) et durée : **une semaine** (jusqu'au 07/11) ou
   **15 jours** (jusqu'au 14/11) ? Proposition : l'admin shop dure ce que dure l'événement, et la fin est automatique.
3. **[Réglé : durée en jours]** **Ouverture et fermeture** (proposition) : `/adminshop ouvrir <date de fin>` et `/adminshop fermer` (permission
   `kseconomy.staff`, comme les signalements) ; fermeture automatique à la date de fin ; message à tous à l'ouverture,
   à la veille de la fin et à la fermeture ; bouton « Admin shop » dans le menu Économie, visible seulement quand il
   est ouvert ; les deux réglages (admin shop + shop étendu) s'ouvrent et se ferment ensemble. D'accord, ou le shop
   étendu doit-il avoir ses propres dates ?
4. **[Réglé : objets custom sans craft + Raccommodage + amulette, coffres 10 / 100 / 1 000 ; détail en 5.1]** **Liste des objets** : que met-on derrière « ... » ? Candidats (à cocher) :
   - livre Raccommodage ; badges niveau 1 (les 4) ; jetons fly, mort, TP, claim ;
   - coffres de récompense : lesquels ? (proposition : moyenne 10, grosse 100, exceptionnelle 1 000) ;
   - amulette mangeuse de mal **seulement si KS_MaleficesNether est déployé à temps** (sinon retirée de la liste) ;
   - autres possibles : livres de trésor (Raccommodage mis à part), Élixir de Fortune, fragment / Cœur de Spawner,
     spawners, têtes de boss, badges de niveau 2+ ?
5. **[Réglé : aucune limite à l'admin shop, quotas des rachats inchangés]** **Limites d'achat** : sans limite, un riche achète 50 Raccommodage d'un coup. Proposition : un nombre maximal par
   joueur et par événement pour chaque objet (ex. Raccommodage 2, badges 1 de chaque, coffre exceptionnel 1), pas de
   stock commun. Paiement en points seulement (ou aussi en blocs compressés ?).
6. **[Sans objet : lecture A retenue]** **Fin de l'événement, boutiques en trop** (lecture B seulement) :
   - **A. Fermées** : les boutiques au-delà de la limite passent « Fermée » (plus d'achat), le propriétaire garde
     leur gestion à distance pour vider le stock et récupérer ses points ; elles rouvrent au prochain événement ou
     quand il libère des places. **Recommandée** : rien ne se perd.
   - **B. Gardées** : elles restent ouvertes, mais aucune création tant que le magasin est au-dessus de sa limite.
   - **C. Supprimées** : panneau rendu, le contenu reste dans le contenant (plus de boutique).
   Et : 100 pour tous les magasins, ou + 90 par rapport à leur tier ?
7. **[Réglé : × 1,5 partout]** **Prix** : proposition d'ancrage par famille, à corriger avant le .xlsx :
   - objets à valeur fixe (Raccommodage, badges, jetons, amulette) : **valeur validée × 1,5** (rare, mais l'admin
     shop crée des objets à partir de points : il doit coûter plus cher que de les obtenir en jeu) ;
   - coffres de récompense : **valeur moyenne × 1,5** (le contenu est revendable en partie aux rachats) ;
   - le jeton de claim et le jeton de la mort : prendre les valeurs du 09/10 (1 000 et 300), pas celles de
     `rachats.csv` (à corriger dans le barème au passage ?).
   Le .xlsx (objet, valeur validée, source, coefficient, prix, limite) est livré une fois la liste fixée.

## 5. Choix d'interprétation (proposés, à valider)

- **Admin shop** : menu en coffre (objets, pas de boutons ; textes sans défilement, nouvelles clés de texte) :
  une case par objet, survol : prix, date de fermeture ; clic : quantité puis confirmation (paiement en
  points) ; refus si solde insuffisant ou inventaire plein ; aucune limite d'achat. Les objets sont créés par les
  plugins qui les possèdent (KS_Jetons, KS_RewardsGUI, KS_MaleficesNether), comme KS_KaliumGive.
- Liste et prix dans un fichier `adminshop.yml` (modifiable sans recompiler) ; chaque achat écrit dans la
  console (joueur, objet, prix), comme les rachats.
- Monde de test « Kixster Test » : l'admin shop y marche avec le solde de test (`CompteTest.de`), jamais avec le
  vrai solde.
- **Rachats à 100 objets** (question 1) : le menu des rachats passe à plusieurs pages (10 objets par page, mêmes
  cases qu'aujourd'hui). À l'ouverture de l'événement, le tirage de la semaine en cours est complété jusqu'à 100 (les
  10 offres déjà là gardent leur prix) ; à la fermeture, on revient à 10 en gardant les 10 premières offres, sans
  attendre lundi. Mêmes règles qu'en temps normal : familles (une famille ne revient que s'il n'en reste pas d'autre),
  ± 25 %, lots, dimensions fermées exclues, quotas par objet et par jour inchangés. Le marchand ambulant reste à 10.
- Risque signalé : 100 objets, c'est environ 10 fois plus de points gagnables en vendant au serveur. Le quota par
  objet (0,5 % du total gagné au-dessus de 2 500) limite chaque objet, pas le total.
- Didacticiel (`didacticiel.yml`) : une entrée « Admin shop » (visible seulement pendant un événement ?).

### 5.1 Objets custom sans craft (relevé du 30/10 dans le dépôt)

Liste et prix validés : `C:\Users\xelin\lk6_remi\Depot git kalium\sortie\halloween\adminshop-prix-proposition.xlsx`
(hors git). Coefficient 1,5 partout (validé par LeKiwi06 le 30/10).

| Objet | Plugin | Valeur validée | Source de la valeur | Prix à × 1,5 |
|---|---|---|---|---|
| Cristal de purification | KS_Meule | 600 | LeKiwi06, 11/10 (pools des rewards) | 900 |
| Estomac du gardien | KS_EstomacGardien | 155,4 | barème (contenu moyen) | 233 |
| Fragment de Spawner | KS_ItemSimple | 128 | LeKiwi06 (32 diamants) | 192 |
| Cœur de Spawner | KS_ItemSimple | 10 235 | **provisoire**, jamais fixé ; « récompense d'événement uniquement » | 15 353 |
| Jeton de fly | KS_Jetons | 30 | rewards, 09/10 | 45 |
| Jeton de la mort | KS_Jetons | 300 | rewards, 09/10 | 450 |
| Jeton de claim | KS_Jetons | 1 000 | rewards, 09/10 | 1 500 |
| Badge de fly niveau 1 | KS_Jetons | 450 | rewards, 09/10 | 675 |
| Badge de téléportation niveau 1 | KS_Jetons | 600 | rewards, 09/10 | 900 |
| Badge de localisation niveau 1 | KS_Jetons | 750 | rewards, 09/10 | 1 125 |
| Badge de la mort niveau 1 | KS_Jetons | 3 600 | rewards, 09/10 | 5 400 |
| Coffres de récompense 10 / 100 / 1 000 | KS_RewardsGUI | valeur moyenne | **validé : 15 / 150 / 1 500** | 15 / 150 / 1 500 |
| Livre Raccommodage (vanilla, cité dans l'annonce) | - | 1 500 | barème, 04/10 | 2 250 |

Écartés (choix de Claude, à confirmer) :
- **Jeton de téléportation** : retiré par LeKiwi06 le 30/10 (déjà vendu 20 dans /jetons).
- **Avec un craft** : Clé de l'End, Bedrock Breaker, spawners, Changeur de Biome, élixirs, jeton de localisation,
  verrou de véhicule, aliments, alcools, trains, torche augmentée, bandage, Casse-croûte doré.
- **Amulette mangeuse de mal** : elle a un craft (jambières de chaînes en cuivre + balise), mais l'annonce la cite :
  **gardée par exception** (LeKiwi06, 30/10), 11 250, seulement si KS_MaleficesNether est déployé à temps (sinon
  absente de l'admin shop ; elle s'ajoute à `adminshop.yml` dès que le plugin est là).
- **Sans craft mais pas des objets à vendre** : blocs d'émeraude compressés (c'est de la monnaie), fioles
  d'expérience (butin des mobs, valeur = l'XP contenue), fiole du coffre de mort.
- **Cœur de Spawner** : **vendu à 15 353** (LeKiwi06, 30/10), alors qu'il était prévu comme récompense d'événement
  uniquement : un spawner s'achète donc environ 15 353 + 7 × 192 = 16 697 points (+ la tête) pendant l'événement.
  Sa valeur de 10 235 reste provisoire dans le barème.

## 6. Plugins

- Modifié : **KS_Economy** (admin shop, shop étendu, commandes staff).
- Touchés : KS_Jetons et KS_RewardsGUI (création des objets vendus), KS_MaleficesNether (amulette, si prêt),
  KS_Didacticiel (`didacticiel.yml`), éventuellement `rachats.csv` (jetons).
