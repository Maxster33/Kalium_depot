# Cahier des charges - Catégorie 3 : Claims (KS_Claim + SimpleClaimSystem) - LeKiwi06

Publié au déploiement (02/10/2026). Réglages de SimpleClaimSystem appliqués : `scs/README.md`.

Serveur : **Event** uniquement (bêta ; destination future : Kixster SMP). État : **cahier validé par LeKiwi06 le 30/09/2026** (session 3).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_Claim :
> plugin d'interface pour gérer ses claims , leurs membres , les permission , avec système de groupage des permission et des membres . prix du prochain claim , acheter des slot de claim via la money de KS_economy . utilisation de SimpleClaimSystem comme base  ( ajouter la vérifications de propriété de la zone dans le plugin du changeur de biome )

> ajouter les plugin : [...] SimpleClaimSystem [...]

Décisions de la session 2 liées : KS_Economy est une économie **Vault** (le score) ; les **magasins** de KS_Economy
sont déclarés dans un **groupe de claim** et regroupent les boutiques Tradeshop du groupe (10 maximum).

## To do list du Kixster SMP (fichier daté ; la demande du 29/09 fait foi en cas de différence)

> claim : système similaire à simple claim système ( configuration possible de toutes les mécaniques du jeu selon visiteur/membres , possibilité de grouper les claims pour editer les settings et les membres simultanément , faire un config .yml pour les prix de claim en emeraude dematérialisés , interaction bloqué : inclure seulement les contenant : donc ne pas bloquer les intéraction avec les bateau , table d'enchantement , table de craft , ender chest ... ) si l'option est jugé trop instable pour la sortis , je te fournirais un plugin de claim compatible , tu pourras le customiser pour inclures mes demandes .

## Documentation officielle relue (https://xyness.gitbook.io/simpleclaimsystem, demande de LeKiwi06)

- **Menus configurables** (objets, cases, titres, boutons personnalisés qui lancent une commande), fichiers de langue.
- **Listes réglables dans `config.yml`** : `blocked-interact-blocks` (blocs protégés par « InteractBlocks » : fours,
  enclumes, alambic, table d'enchantement, table de craft, coffre de l'Ender, lits, panneaux... par défaut),
  `blocked-items` (perles, arcs, seaux...), `blocked-entities` (villageois, bateaux, wagonnets, chevaux...),
  `special-blocks` (spawner).
- **Limites par groupe / permission / joueur** : `max-claims`, `max-chunks-per-claim`, `max-chunks-total`,
  `max-members`, `max-radius-claims`, `claim-distance`, `claim-cost` (+ `-multiplier`), `chunk-cost` (+
  `-multiplier`) ; permissions `scs.claim.<n>`, `scs.chunks.<n>`... Priorité : joueur dans la config > permission >
  groupe.
- Par défaut : **`auto-purge: true`** (claims des joueurs absents depuis **14 jours** supprimés), vol dans les claims,
  vente / achat de claims, `/claim tp`, messages d'entrée / sortie, barre de boss, claims possibles dans le Nether et
  l'End (`claims-worlds-mode`).
- **API** (`SimpleClaimSystemAPI`) : claim d'un chunk (`getClaimAtChunk`, `getClaimOwnerAt`), membre / banni,
  `setClaimPerm(claim, permission, valeur, rôle)`, `checkClaimPermission`, ajout / retrait de membres (un claim ou
  tous), `applySettingsToAllClaims`, fusion, chunks, nom, description, propriétaire. Rôles connus : membres,
  visiteurs, naturel.

## Ce qu'est SimpleClaimSystem (vérifié le 29/09/2026, v1.13.1, licence MIT, Paper / Folia, Minecraft 1.18+)

- Claims par **chunks** (plusieurs chunks par claim, fusion de claims, sous-zones, bannissements).
- **3 catégories de réglages** : Membres et Visiteurs (27 réglages chacune : construire, casser, boutons, objets,
  interagir avec les blocs, leviers, plaques, portes, trappes, portillons, fils, répéteurs / comparateurs, cloches,
  entités, semelles givrantes, téléportations, dégâts, vol, météo, téléportation par menu, portails, entrer, ramasser,
  jeter, blocs spéciaux, élytres, charges de vent), Naturel (explosions, liquides, redstone, feu, monstres, PvP).
- **Deux niveaux seulement** : membre ou visiteur. Pas de rôles, **pas de groupes de claims**.
- Économie **Vault** : `claim-cost`, `chunk-cost`, multiplicateurs (`claim-cost-multiplier` : chaque claim suivant
  coûte x fois plus), vente / achat de claims entre joueurs (`/claim sell`, `/claim buy`).
- Ses propres menus ; API (JitPack) : claims d'un joueur, ajout de membres... ; le reste (claim à une position,
  vérification d'une permission, modification des réglages) est à vérifier dans son code avant le développement.
- Stockage SQLite ou MySQL.

# 2. Réponses de LeKiwi06

| Question | Réponse (29-30/09/2026) |
|---|---|
| Interface | **Option A** : interface propre à KS_Claim pour tout ; SCS = moteur (API) |
| Groupes de claims | Un claim dans un seul groupe ; une modification du groupe s'applique à tous ses claims : validé |
| Rôles | Pas de rôles en plus : les permissions de base des propriétaires, membres et visiteurs se règlent dans le `config.yml` de SCS ; les propriétaires peuvent modifier certains réglages de leurs claims pour les membres et les visiteurs |
| Prix | **Prix au claim**, prochains prix affichés ; **10 claims gratuits**, puis **64n + 2n²** émeraudes (n = numéro du claim : 11e = 946, 12e = 1 056...) |
| Taille / membres | 1 claim = **2 x 2 chunks** ; **5 membres** maximum par claim |
| Unclaim | Rembourser le **dernier prix payé**, même si ce n'est pas ce claim-là qui l'a coûté |
| Contenants protégés | Liste proposée validée (coffres, coffres piégés, tonneaux, shulkers, entonnoirs, distributeurs, droppers, fours, fumoirs, hauts fourneaux, alambics, crafters, bibliothèques sculptées, pots décorés) ; bateaux et wagonnets retirés des entités protégées. LeKiwi06 corrigera lui-même le `config.yml` en cas d'oubli |
| Suppression auto des claims inactifs | Désactivée |
| Vol dans les claims | Désactivé |
| Vente de claims entre joueurs | **Gardée**, mais le prix de vente ne peut pas être inférieur au prix actuel d'un claim pour l'acheteur |
| `/claim tp` | Désactivé (un futur système de homes, en nombre limité par joueur, le remplacera) |
| Nether et End | Claims permis ; dans l'End, **pas sur l'île du dragon**, seulement le vide autour et la zone des End cities |
| Messages d'entrée / sortie | **Barre de boss uniquement** (pas de titre, chat ni barre d'action) |
| Changeur de Biome | Exception pour les **membres** du claim |
| Maximum | **100 claims** par joueur |
| Forme | **Carré de 2 x 2 chunks** créé d'un coup quand on claime |
| Calcul de n | Nombre de claims possédés au moment d'en créer un (achetés compris) ; exemple du remboursement validé |
| Vente entre joueurs | Achat refusé si le prix est inférieur au prochain prix de l'acheteur (message avec le minimum) ; tout prix accepté sous 10 claims ; le claim acheté compte chez l'acheteur : validé |
| Île du dragon | Région WorldGuard de 200 blocs autour de (0, 0) dans l'End, toute la hauteur, drapeau `scs-claim` : validé |
| Commandes | **`/ksclaim`** et **`/ksclaims`** |
| Placement / confirmation de suppression | Validés |
| Réglages modifiables par les propriétaires | LeKiwi06 s'en occupe lui-même dans la config |
| **Taille et prix (remplace les lignes Prix, Taille, Maximum, Forme ci-dessus)** | **1 claim = 1 chunk** ; pas d'ajout de chunk ni de fusion (plus simple) ; **option B** : 10 claims gratuits, 100 maximum, prix du claim n = **16n + n²/2** (arrondi à l'unité supérieure) |

# 3. Questions restantes

Aucune.

# 4. Choix d'interprétation (validés le 30/09/2026)

## Éléments

| Élément | Rôle |
|---|---|
| **KS_Claim** (nouveau) | Toute l'interface des claims (en français, couleurs Bedrock de KLM_Menu), groupes de claims, prix, remboursement, règle de vente ; pilote SCS par son API |
| **SimpleClaimSystem** (installé) | Moteur : claims, protections, membres, bannissements, vente ; ses menus ne sont plus proposés aux joueurs |
| KS_Economy (catégorie 2) | Paiements et remboursements en score (Vault) |
| KS_Menu (catégorie 2) | Bouton « Claims » |
| WorldGuard (déjà installé) | Région de l'île du dragon avec le drapeau `scs-claim` |
| **KS_BiomeChanger** (modifié) | Refus si la zone touche un claim dont le joueur n'est ni propriétaire ni membre |

## Ouverture

- **`/ksclaims`** (et bouton « Claims » de KS_Menu) : interface de **ses** claims uniquement (réponse de LeKiwi06,
  30/09/2026). **`/ksclaim`** : claime le chunk où se trouve le joueur, comme le bouton « Claimer ici » (prix affiché,
  confirmation). Pas de liste de tous les claims du serveur. Les commandes joueurs de SCS sont retirées aux joueurs
  (permissions), pour que tout passe par l'interface.

## Créer un claim

- Bouton « Claimer ici » : claime **le chunk où se trouve le joueur** (1 claim = 1 chunk = 16 x 16 blocs, toute la
  hauteur). Refusé si le chunk est déjà claimé, touche une région WorldGuard `scs-claim`, ou si le joueur a déjà
  **100 claims**.
- Un claim reste d'un seul chunk : pas d'ajout ni de retrait de chunk, **pas de fusion**, pas de claim par rayon ni
  de claim automatique. Pour une base plus grande : plusieurs claims voisins rangés dans un **groupe**.
- **Prix** : n = nombre de claims possédés + 1 (claims achetés compris). n <= 10 : gratuit ; au-delà :
  **16n + n²/2** émeraudes du score, arrondi à l'unité supérieure (11e : 237, 12e : 264, 50e : 2 050, 100e : 6 600).
  L'interface affiche le prix du prochain claim et des suivants. Paiement refusé si le solde est insuffisant.
- Mondes : overworld, Nether, End (sauf l'île du dragon).

## Supprimer un claim

- Rembourse le **dernier prix payé** par le joueur (chaque prix payé est retenu ; le remboursement retire le dernier
  de la liste), même si ce n'est pas ce claim-là qui l'a coûté. Aucun prix payé : rien à rembourser. Confirmation
  demandée avant la suppression.

## Gérer ses claims

- Liste de ses claims (nom, monde, coordonnées, groupe) ; pour chaque claim : nom et description, **membres** (5
  maximum : ajouter, retirer), bannissements (bannir, débannir, expulser), **réglages**.
- **Réglages** : valeurs de base dans le `config.yml` de SCS ; le propriétaire ne voit et ne modifie que les réglages
  (membres, visiteurs) que la configuration l'autorise à modifier. Rien n'est figé dans le code : LeKiwi06 règle
  lui-même cette liste.
- **Groupes de claims** : le joueur crée un groupe (nom), y range ses claims (un claim dans un seul groupe). Membres et
  réglages modifiés sur le groupe s'appliquent à tous ses claims (5 membres maximum par claim). Le **magasin** de
  KS_Economy se déclare sur un groupe.

## Vendre un claim

- Mise en vente par l'interface (prix au choix du vendeur, plafond `max-sell-price` de SCS). À l'achat : refusé si le
  prix est inférieur au prochain prix de claim de l'acheteur (message avec ce minimum ; sous 10 claims, tout prix
  passe) ou si l'acheteur a déjà 100 claims. Le claim acheté compte ensuite chez l'acheteur.

## Configuration de SCS

| Réglage | Valeur |
|---|---|
| `auto-purge` | `false` |
| Vol (`Fly`) | désactivé partout |
| `/claim tp` | retiré aux joueurs |
| `claims-worlds-mode` | overworld, Nether, End en survie |
| Entrée / sortie d'un claim | barre de boss seulement (pas de titre, chat, barre d'action) |
| `max-claims` / `max-chunks-per-claim` / `max-members` | 100 / 1 / 5 |
| `claim-cost` de SCS | désactivé (le prix 16n + n²/2 est calculé par KS_Claim) |
| `blocked-interact-blocks` | coffres, coffres piégés, tonneaux, shulkers, entonnoirs, distributeurs, droppers, fours, fumoirs, hauts fourneaux, alambics, crafters, bibliothèques sculptées, pots décorés |
| `blocked-entities` | liste par défaut sans bateaux ni wagonnets |

## Hors de ce chantier

- **Système de homes** (nombre limité par joueur), qui remplacera `/claim tp` : idée pour plus tard, noté dans l'index.
