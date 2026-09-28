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
