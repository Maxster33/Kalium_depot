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
