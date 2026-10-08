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

## 1.4.0 - Bedrock Breaker : houe en diamant, objet dans KS_BedrockBreaker, livre de recettes (29/09/2026)

Demande de Maxster33 (détail dans `KS_BedrockBreaker/JOURNAL.md`) :
- Recette : **8 TNT autour d'une houe en diamant** (n'importe laquelle, même renommée, enchantée ou abîmée : choix de
  Maxster33) → 1 Bedrock Breaker, objet créé par KS_BedrockBreaker (`softdepend` ; `build.sh` : compiler
  KS_BedrockBreaker d'abord). Sans KS_BedrockBreaker activé : craft ignoré (avertissement dans la console).
- L'objet et son utilisation (ancienne houe en bois nommée, clic droit sur la bedrock) sont retirés de KS_Crafts :
  c'est KS_BedrockBreaker qui les gère (les anciens Bedrock Breaker y marchent encore).
- Livre de recettes : la recette du Bedrock Breaker est débloquée dès qu'une **TNT ou une houe en diamant** arrive dans
  l'inventaire du joueur, comme la Clé de l'End (même mécanisme, généralisé : liste `LIVRE`).

**Déployé sur Event le 29/09/2026 à 12:55 avec KS_BedrockBreaker 1.0.0 (1.3.0 dans `_removed-ks_crafts-1.3.0/` ; supprimables par l'humain, 3 versions derrière : `_removed-ks_crafts-1.0.0/` et `1.1.0/`). Statut : non testé en jeu.**

## 1.5.0 - nouvelle recette du Bedrock Breaker (29/09/2026)

Demande de Maxster33 : nouvelle recette du Bedrock Breaker (avec forme) :

| | gauche | milieu | droite |
|---|---|---|---|
| haut | poudre de blaze | charge de feu | poudre de blaze |
| milieu | wagonnet à TNT | cristal de l'End | wagonnet à TNT |
| bas | poudre de blaze | ancre de réapparition | poudre de blaze |

- L'ancienne recette (8 TNT autour d'une houe en diamant) n'existe plus (même id `ks_crafts:bedrock_breaker`).
- Livre de recettes (choix de Maxster33) : la recette est débloquée dès que le joueur obtient l'un des nouveaux
  ingrédients (poudre de blaze, charge de feu, wagonnet à TNT, cristal de l'End, ancre de réapparition) ; la TNT et la
  houe en diamant ne la débloquent plus. Un joueur qui l'avait déjà débloquée la garde.

**Déployé sur Event le 29/09/2026 à 13:59 (1.4.0 dans `_removed-ks_crafts-1.4.0/` ; supprimables par l'humain, 3 versions derrière : `_removed-ks_crafts-1.0.0/`, `1.1.0/`, `1.2.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.6.0 - spawners et Changeur de Biome (29/09/2026)

Demande de Maxster33 (détail des objets : `KS_ItemSimple/`, `KS_Spawners/`, `KS_BiomeChanger/`). Crafts **sans forme**
(seuls les crafts « 8 + 1 » sont en anneau) :

| Ingrédients | Résultat |
|---|---|
| 7 Fragments de Spawner + 1 Cœur de Spawner + 1 tête de zombie | Spawner à zombi |
| ... + 1 crâne de squelette | Spawner à squelette |
| ... + 1 tête de creeper | Spawner à creeper |
| ... + 1 Tête d'araignée (tête « Steve » de KS_ItemSimple) | Spawner à araignée |
| ... + 1 Tête de blaze (idem) | Spawner à blaze |
| ... + 1 Tête de mouton (idem) | Spawner à mouton |
| ... + 1 Tête de vache (idem) | Spawner à vache |
| ... + 1 Tête de poule (idem) | Spawner à poule |
| 4 Fragments de Spawner + bloc de mousse + seau de neige poudreuse + oeilchidée ouverte + oeuf de sniffer + champignon brun | Changeur de Biome |

- Fragments, cœur et têtes « Steve » : objets exacts de KS_ItemSimple (une tête de joueur ordinaire ne marche pas) ;
  têtes de zombie, squelette et creeper : les têtes vanilla. Les têtes d'araignée, de blaze, de mouton, de vache et de
  poule n'existent pas en vanilla : têtes « Steve » nommées en attendant un plugin dédié aux têtes (choix de Maxster33 ;
  avec de simples têtes de Steve, les 5 recettes auraient été identiques).
- `softdepend` KS_ItemSimple, KS_Spawners, KS_BiomeChanger ; `build.sh` : les compiler d'abord. Sans eux : crafts
  ignorés (avertissement dans la console).
- **Livre de recettes** (demande de Maxster33) : dès qu'un joueur obtient un **Fragment de Spawner**, les recettes des
  8 spawners et du Changeur de Biome sont débloquées (fragment reconnu à son marqueur, pas seulement au livre de
  connaissances qui sert de base à d'autres objets custom).

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (1.5.0 dans `_removed-ks_crafts-1.5.0/` ; supprimables par l'humain, 3 versions derrière ou plus : `_removed-ks_crafts-1.0.0/` à `1.3.0/`). Statut : non testé en jeu.**

## 1.7.0 - spawners avec les têtes de KS_Decapitator (30/09/2026)

Demande de LeKiwi06 (catégorie 1 « Contenu survie ») : les recettes des 8 spawners prennent les têtes de
KS_Decapitator, **toutes variantes, états et bébés** du mob (ex. n'importe quelle tête de mouton, bébé ou non, de
n'importe quelle couleur). Aucun spawner ajouté.

- Araignée, blaze, mouton, vache, poule : tête de KS_Decapitator (les têtes « Steve » de KS_ItemSimple n'existent
  plus).
- Zombie, squelette, creeper : tête vanilla (recette d'avant, inchangée) **ou** tête de KS_Decapitator (nouvelles
  recettes `spawner_zombi_tete`, `spawner_squelette_tete`, `spawner_creeper_tete`).
- Vache : pas la mooshroom (autre créature).
- `softdepend` et `build.sh` : KS_Decapitator (compiler d'abord). Sans lui : seules les 3 recettes à tête vanilla
  restent (avertissement).
- Livre de recettes : inchangé (le premier Fragment de Spawner débloque toutes les recettes de spawners).

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (1.6.0 dans `_removed-ks_crafts-1.6.0/` ; supprimables par l'humain, 3 versions derrière ou plus : `_removed-ks_crafts-1.0.0/` à `1.4.0/`), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.8.0 - réparation de la tête de wither squelette, bloc de charbon de bois (03/10/2026, LeKiwi06)

Demande de LeKiwi06 (tête de KS_Decapitator 1.1.0 ; un défaut par craft, dans n'importe quel ordre) :
- **Réactiver** (désactivée) : tête au centre, autour en alternance 4 blocs d'émeraude compressés **tier 2** et 4 fioles
  d'expérience de **15 niveaux exactement** ; les deux façons (blocs aux coins ou aux côtés).
- **Nettoyer** (sale) : 8 pinceaux **non endommagés** autour.
- **Réparer** (endommagée) : en alternance 4 lingots de netherite et 4 blocs de charbon de bois ; les deux façons.
- Réparée des 3 défauts : vrai crâne de wither squelette.
- **Bloc de charbon de bois** : 9 charbons de bois ; apparence d'un bloc de charbon ; ingrédient et combustible
  seulement (ne se pose pas ; jamais pris pour un bloc de charbon dans une autre recette) ; se redéfait en 9 charbons
  de bois.
- Recettes en forme (livre de recettes) ; ingrédients exacts et résultat vérifiés à chaque craft. `softdepend`
  KS_Economy, KS_FioleExp ; « réactiver » ignoré sans eux.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.8.0 ; ancienne version dans `_removed-ks_crafts-1.7.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.9.0 - toutes les recettes dans le livre de recettes (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : « ajoute les recettes de tous les crafts custom qui n'en ont pas encore, elles se débloquent quand
on récupère au moins un des éléments du craft ». Chaque recette du plugin (sable rouge, sables, blocs bruts, calcite,
obsidienne pleureuse, deepslate renforcée, graines, coraux, lumière, cadre invisible, grenouillères, briques du Nether
rouges, Clé de l'End, Bedrock Breaker, spawners, Changeur de Biome, réparations de la tête de wither squelette, bloc de
charbon de bois et son inverse) apparaît dans le livre dès que le joueur obtient l'un de ses ingrédients (objets custom
reconnus exactement) ; une fois débloquée, elle le reste. Avant : seulement la Clé de l'End, le Bedrock Breaker, les
spawners et le Changeur de Biome. Les élixirs (KS_Elixir) le faisaient déjà. Le mélange de l'alambic (bloc de verrue)
n'est pas une recette d'établi : pas de livre.

**Déployé sur Event le 03/10/2026 à 19:06 (LeKiwi06 ; 1.8.0 dans `_removed-ks_crafts-1.8.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.10.0 - plus de brassage à la verrue, boîte de shulker au coffre de l'Ender (04/10/2026, LeKiwi06)

Demandes de LeKiwi06 (pendant le travail sur le barème des prix) : « il faut supprimer le brassage par bloc de verrue
c'est une erreur » ; « on va simplement bloquer le fait que la verrue aille dans l'alambic, donc elle sera farmable
comme une patate ou carotte » ; « on va remplacer le coffre dans le craft de la shulker box par un enderchest pour
casser la chaîne de production automatique des shulker ».

- **Alambic** : le mélange « bouteille d'eau + bloc de verrue → potion étrange » (1.0.0) est retiré. La **verrue du
  Nether** ne peut plus être posée dans un alambic (clic, raccourci clavier, glisser, entonnoir : refusé, message
  « La verrue du Nether ne se brasse plus. ») ; par sécurité, un brassage à la verrue est annulé. Conséquence voulue :
  plus aucune potion étrange, donc plus aucune potion de base à l'alambic ; les potions viennent du butin
  (KS_LootPotions, KS_LootEntites) et de /rewards. Les potions trouvées se modifient toujours (redstone, glowstone,
  poudre à canon, souffle de dragon, œil d'araignée fermenté).
- **Boîte de shulker** : la recette vanilla est retirée ; nouvelle recette de même forme (carapace, **coffre de
  l'Ender**, carapace), dans le livre de recettes dès qu'on obtient un ingrédient. La teinture des boîtes est inchangée.
- Inchangé : briques rouges du Nether au bloc de verrue, craft « 9 verrues → bloc de verrue » toujours retiré (non
  demandé ; à voir maintenant que la verrue redevient cultivable).
- **À déployer avec KS_LootBlocs 1.3.0 et KS_LootPotions 1.1.2.**

Limite : une verrue déjà posée dans un alambic avant cette version y reste, sans effet (brassage annulé).

**Déployé sur Event le 04/10/2026 à 06:52 (LeKiwi06 ; 1.9.0 dans `_removed-ks_crafts-1.9.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.11.0 - vrais objets dans les recettes de réparation de la tête de wither squelette (09/10/2026, LeKiwi06)

Demande de LeKiwi06 (vu dans l'interface de craft) : « le processus de réparation des têtes de wither était plutôt mal
indiqué : les items demandés ne sont pas les bons (on ne voit pas le tier du bloc compressé, on ne voit pas une tête de
wither marquée comme "désactivé" (avec alternative contenant les autres états selon l'ordre dans lequel on souhaite le
faire) et on ne voit pas le niveau de la fiole d'xp ».

Cause : les 5 recettes étaient déclarées avec les objets vanilla de même apparence (bloc d'émeraude, fiole
d'expérience, bloc de charbon, tête de joueur) ; les vrais objets n'étaient vérifiés qu'au moment du craft, et le livre
de recettes n'affiche que ce que la recette déclare.

- **Autour** : le livre montre le **bloc d'émeraude compressé tier 2**, la **fiole de 15 niveaux** et le **bloc de
  charbon de bois** (`RecipeChoice.predicateChoice` de Paper : objet d'exemple affiché, même vérification qu'avant).
  Pinceau et lingot de netherite : inchangés (objets vanilla).
- **Au centre** : la tête de wither squelette marquée du défaut à réparer (`ExactChoice`) ; d'abord celle qui n'a plus
  que ce défaut (le crâne de wither squelette affiché en résultat est alors le bon), puis, en alternance, celles qui
  ont aussi un autre défaut, puis celle qui a les trois.
- Aucun changement des ingrédients acceptés ni du résultat (toujours vérifiés à chaque craft).
- **Conséquence** : ces recettes se débloquent dans le livre en obtenant un **vrai** ingrédient (tête à réparer, bloc
  tier 2, fiole de 15 niveaux, bloc de charbon de bois, pinceau, lingot de netherite), plus avec n'importe quelle tête de
  joueur, bloc d'émeraude, fiole d'expérience ou bloc de charbon. Une recette déjà débloquée le reste.
- Sans la tête de wither squelette dans KS_Decapitator : crafts de réparation ignorés (avertissement).

Limite : le résultat affiché reste le crâne de wither squelette, même pour une tête qui garde d'autres défauts (le vrai
résultat apparaît en posant les objets). Affichage chez les joueurs Bedrock : non vérifié.

**Compilé le 09/10/2026, non déployé. Statut : non testé en jeu.**
