# KS_LootPotions - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - potions à ramasser (25/09/2026)

Potion basique : à boire, niveau 1, durée normale. Seulement quand un joueur casse le bloc ou tue le mob ; Butin sans
effet.
- **Blocs** : émeraude deepslate 10 % chance (en plus du drop, si le minerai a donné quelque chose) ; plante de
  chorus 1 % chute lente ; verrue du Nether 5 % potion aléatoire.
- **Mobs** : vex 10 % infestation ; phantom 10 % chute lente ; pillard 10 % faiblesse (capitaines compris) +
  capitaine 1 % potion aléatoire ; strider 1 % résistance au feu ; lapin 1 % saut ; gros slime (taille 4) 10 %
  infestation ; breeze 10 % vent ; allay 10 % soin ; araignée 1 % tissage ; araignée bleue 1 % poison ; gardien 10 %
  respiration aquatique ; gardien ancien 100 % respiration aquatique ; ghast 10 % régénération ; marchand ambulant
  100 % invisibilité ; blaze 10 % force ; cheval, âne, mule 10 % vitesse.
- **Potion aléatoire** : vitesse, lenteur, saut, force, soin, dégâts, poison, régénération, faiblesse, invisibilité,
  respiration aquatique, résistance au feu, vision nocturne, chute lente, maître tortue, vent, tissage, suintement,
  infestation (chances égales ; pas la chance).
- Potion de régénération de l'endermite : dans KS_LootEntites (tirage de 6 objets).

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**
