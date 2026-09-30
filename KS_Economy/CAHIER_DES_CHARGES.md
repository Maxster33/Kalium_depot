# Cahier des charges - Catégorie 2 : Économie (KS_Economy, KS_Menu + Tradeshop, Vault) - LeKiwi06

Publié au déploiement de la 1re partie (01/10/2026) : score, blocs compressés, `/echange`, KS_Menu, Vault. Les
magasins (partie « Magasins ») seront codés après KS_Claim (catégorie 3).

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

## Magasins (codés après KS_Claim)

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
