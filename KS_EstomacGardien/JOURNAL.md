# KS_EstomacGardien - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Demande de Maxster33 du 28/09/2026 (voir
`KS_Event/CAHIER_DES_CHARGES.md`, partie 4).

## 1.0.0 - objet « Estomac du gardien » (28/09/2026)

- **Objet** : « Estomac du gardien », avec l'image du sac noir vanilla (`item_model` = `minecraft:black_bundle`) ; ce
  n'est pas un vrai sac (on ne peut rien y ranger). Objet de base : livre de connaissances (pas un bloc, aucun
  craft, se range par 1 comme un sac), dont l'effet vanilla est annulé. Marqué `ks_estomacgardien:estomac`.
- **Clic droit** (en l'air ou sur un bloc, main principale ou secondaire) : l'objet est consommé et donne son contenu ;
  inventaire plein : le reste tombe au sol.
- **Contenu** :
  - tirage 1 (1 tirage) : 10 % casque en diamant enchanté niveau 30 à 50 (sinon rien) ;
  - tirage 2 (2 tirages, poids sur 31) : oeuf de tortue x1 (3), bateau au hasard x1 (3 ; tous les bateaux et
    radeaux, avec ou sans coffre), corail au hasard x2 à 3 (10 ; plante, éventail ou bloc des 5 coraux vivants),
    algue au hasard x2 à 3 (10 ; varech ou herbe marine), trident enchanté niveau 10 à 29 x1 (3), armure de nautile
    en diamant x1 (1), coeur de la mer x1 (1).
  - Enchantements « niveau X à Y » : comme la fonction vanilla `enchant_with_levels` (enchantements
    `#on_random_loot`).
- Pour les autres plugins : `KSEstomacGardien.creerEstomac()` (utilisé par KS_LootEntites 1.1.0 : grand gardien).

**Déployé sur Event le 28/09/2026 à 23:19. Statut : non testé en jeu.** Déployé avec KS_LootEntites 1.1.0.

## 1.1.0 - commande /estomac (28/09/2026)

Demande de Maxster33 : pouvoir se donner l'objet (option « commande du plugin » choisie plutôt qu'un datapack).
- **`/estomac [joueur] [nombre]`** : opérateurs seulement (permission `ks.estomac.give`, `default: op`). Sans joueur :
  soi-même (depuis la console, le joueur est obligatoire). Nombre de 1 à 64 (1 par défaut ; limite ajoutée pour éviter
  une faute de frappe géante). Un estomac par case ; inventaire plein : le reste tombe au sol.

**Déployé sur Event le 28/09/2026 à 23:25 (1.0.0 dans `_removed-ks_estomacgardien-1.0.0/`). Statut : non testé en jeu.**

## 1.2.0 - commande /estomac retirée (28/09/2026)

Demande de Maxster33 : « supprime la commande » ; l'objet se donne maintenant avec
`/kaliumgive <pseudo> estomac_gardien <nombre>` (nouveau plugin KS_KaliumGive). Code identique à la 1.0.0.

**Déployé sur Event le 28/09/2026 à 23:35 (1.1.0 dans `_removed-ks_estomacgardien-1.1.0/`). Statut : non testé en jeu.**

## 1.3.0 - casque en diamant : 10 % → 20 % (29/09/2026)

Demande de Maxster33 : « multiplier par deux les chances d'obtenir le casque en diamant ». Tirage 1 : 20 % casque en
diamant enchanté niveau 30 à 50 (sinon rien) ; le reste est inchangé.

**Déployé sur Event le 29/09/2026 à 00:02 (1.2.0 dans `_removed-ks_estomacgardien-1.2.0/` ;
`_removed-ks_estomacgardien-1.0.0/` supprimable par l'humain, 3 versions derrière). Statut : non testé en jeu.**

## 2.0.0 - l'estomac s'ouvre comme un coffre de récompense (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « on va aussi rework l'estomac de gardien : il marchera comme un coffre de récompense avec une
interface de coffre malgré que ce soit un bundle , avec une mention non ouvert ou ouvert ( mais on ne peux pas le
fusionner dans l'enclume ) il faut qu'il contiennent + d'items ».
- **Objet** : même image (sac noir), ligne verte « Non ouvert ». Les estomacs déjà en jeu reçoivent cette ligne quand
  ils arrivent dans un inventaire de joueur, et s'ouvrent de la même façon.
- **Ouverture** : clic droit (dans le vide ou sur un bloc) : fenêtre de coffre « Estomac du gardien » (3 rangées). À la
  première ouverture, le contenu est tiré et écrit dans l'objet, qui devient « Ouvert (reste n piles) » (gris). Un clic
  sur un objet de la fenêtre le prend (inventaire plein : il reste dans la fenêtre) ; rien ne peut y être déposé et
  aucun objet ne bouge par les clics du jeu pendant que la fenêtre est ouverte. Fenêtre fermée : l'estomac garde ce qui
  reste et se rouvre ; vidé, il disparaît.
- **Rachats** : un estomac ouvert perd la marque `ks_estomacgardien:estomac` (il porte `estomac_ouvert`) : les rachats
  de KS_Economy ne reconnaissent que les estomacs non ouverts (aucun changement dans KS_Economy).
- **Enclume** : refusé, ouvert ou non (enclume du jeu et enclume en coffre de KS_Enclume, qui applique ce refus) : ni
  renommage, ni fusion. Les coffres de récompense de KS_RewardsGUI ne se fusionnent qu'entre eux.
- **Contenu** (proposition `sortie/peche/proposition-peche-et-estomac.xlsx`, validée par LeKiwi06 : « code le nouveau
  contenu de l'Estomac tel que proposé ») : 7 à 8 piles au lieu de 2 à 3.
  - tirage 1 inchangé : 20 % casque en diamant enchanté niveau 30 à 50 ;
  - tirage 2 (même table) fait **4 fois** au lieu de 2 ;
  - **nouveau tirage 3** (butin marin), fait 3 fois, poids sur 54 : éclats de prismarine x4-8 (10), cristaux de
    prismarine x2-5 (10), poisson cru au hasard parmi morue, saumon, poisson tropical, poisson-globe x2-5 (10),
    concombres de mer x2-4 (6), sacs d'encre luisante x2-4 (6), lanternes aquatiques x2-4 (5), carapace de nautile (3),
    écaille de tortue x1-2 (3), éponge (1).
  - Valeur moyenne ajoutée au barème : environ 10,9 émeraudes par estomac (2 tirages 2 à 4,8 + 3 tirages 3 à 0,4). Le
    prix de l'estomac dans `rachats.csv` de KS_Economy (155,4) n'est pas changé : une autre session travaille sur
    KS_Economy en ce moment ; à recalculer au prochain passage du barème.
- Didacticiel : nouvelle entrée « Estomac du gardien » (thème Butin), marquée Nouveau le 26/10.

Choix de Claude, signalés : contenu gardé dans l'objet (il n'est tiré qu'à la première ouverture, rien n'est donc
caché avant) ; ouvert = plus revendable aux rachats ; pas de confirmation avant d'ouvrir (KS_RewardsGUI en demande une
au-delà de 100 émeraudes).

**Compilé, non déployé, non testé en jeu.**
