# KS_Crafts - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - crafts du serveur Event (25/09/2026)

Crafts « 8 + 1 » en anneau autour de l'objet central, les autres sans forme (réponses de Maxster33) :

| Ingrédients | Résultat |
|---|---|
| 8 sables autour d'1 colorant orange | 8 sables rouges |
| 1 grès (normal, sculpté, taillé, lisse) | 4 sables |
| 1 grès rouge (normal, sculpté, taillé, lisse) | 4 sables rouges |
| 4 blocs de fer / d'or | 4 blocs de fer / d'or brut |
| 4 blocs de cuivre (toutes les versions, oxydées et cirées) | 4 blocs de cuivre brut |
| 8 TNT autour d'1 houe en bois | 1 Bedrock Breaker |
| 5 diorites + 4 blocs de quartz | 9 calcites |
| 8 obsidiennes autour d'1 larme de ghast | 8 obsidiennes pleureuses |
| 2 blocs d'os + 2 deepslates (pierre) | 1 deepslate renforcée |
| 2 pitcher plants | 4 graines de pitcher plant |
| 2 torchflowers | 4 graines de torchflower |
| 4 coraux (tube, cerveau, bulle, feu, corne) | 1 bloc de corail de la même couleur |
| 4 verres + 4 blocs de glowstone + 1 bâton de blaze | 4 blocs de lumière niveau 15 |
| 8 bâtons autour d'1 membrane de phantom | 1 cadre invisible |
| 8 blocs de magma autour d'1 colorant vert / orange / violet | 4 froglights verdâtres / ocre / nacrées |
| 2 blocs de verrue + 2 briques du Nether (en carré, en diagonale) | 1 brique rouge du Nether (remplace la recette vanilla) |

- Craft vanilla « 9 verrues → 1 bloc de verrue » retiré ; alambic : bouteille d'eau + **bloc de verrue** → potion
  étrange (en plus de la verrue vanilla).
- **Bedrock Breaker** : houe en bois nommée « Bedrock Breaker » (marquée) ; clic droit sur un bloc de bedrock : le
  bloc disparaît (aucun drop) et l'objet est consommé (usage unique, réponse de Maxster33).
- Lumière niveau 15 et cadre invisible décrits au format des commandes de Minecraft : si le serveur refusait ce
  format, seule la recette concernée serait ignorée (avertissement dans le journal).
- **Limite connue** : 4 blocs de cuivre posés **en carré 2×2** correspondent aussi à la recette vanilla du cuivre
  taillé, que le jeu choisit probablement ; posés autrement (en ligne, en L...), ils donnent les blocs bruts. À
  vérifier en jeu ; si besoin, proposer une forme à Maxster33.
- Élixirs : reportés (décision de Maxster33).

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - craft de la Clé de l'End (28/09/2026)

Demande de Maxster33. **Clé de l'End** (objet de KS_EC_Extension, `softdepend`), sans forme : coeur de la mer, crâne
de wither squelette, totem d'immortalité, pomme dorée enchantée, capteur sculk calibré, éponge (sèche), lingot de
netherite, cloche, coeur de grinceur → 1 Clé de l'End. Sans KS_EC_Extension activé : craft ignoré (avertissement dans la
console). `build.sh` compile contre les classes de KS_EC_Extension : **compiler KS_EC_Extension d'abord**.

**Déployé sur Event le 28/09/2026 à 23:47 (1.0.0 dans `_removed-ks_crafts-1.0.0/`). Statut : non testé en jeu.**

## 1.2.0 - Clé de l'End dans le livre de recettes (29/09/2026)

Demande de Maxster33 : ajouter la recette de la Clé de l'End au livre de recettes. Une recette de plugin n'y apparaît
que si elle est débloquée pour le joueur : elle est débloquée pour chaque joueur à sa connexion (et pour les joueurs
en ligne au démarrage du plugin), sans condition. Les autres crafts de KS_Crafts ne sont pas concernés (non demandé).

**Déployé sur Event le 29/09/2026 à 00:25 (1.1.0 dans `_removed-ks_crafts-1.1.0/`). Statut : non testé en jeu.**

## 1.3.0 - recette de la Clé de l'End débloquée à l'obtention d'un ingrédient (29/09/2026)

Correction de Maxster33 : « non pas à la connexion des joueurs mais à l'obtention de l'un des objets de la recette ».
- La recette de la Clé de l'End est débloquée (livre de recettes) dès qu'un des 9 ingrédients arrive dans
  l'inventaire du joueur (`PlayerInventorySlotChangeEvent` de Paper : ramassage, coffre, craft, commande...), comme une
  recette vanilla. Une fois débloquée, elle le reste.
- Au démarrage du plugin et à la connexion : débloquée aussi si le joueur a déjà un ingrédient dans son inventaire
  (objets obtenus avant cette version).
- La 1.2.0 (débloquée à la connexion) n'est restée que 2 minutes sur le serveur (00:25 → 00:27), a priori jamais
  chargée.

**Déployé sur Event le 29/09/2026 à 00:27 (1.2.0 dans `_removed-ks_crafts-1.2.0/` ; `_removed-ks_crafts-1.0.0/`
supprimable par l'humain, 3 versions derrière). Statut : non testé en jeu.**
