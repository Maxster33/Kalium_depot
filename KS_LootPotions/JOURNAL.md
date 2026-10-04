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

## 1.1.0 - potion de faiblesse : capitaine seulement (28/09/2026)

Demande de Maxster33 : « Pillard : enlève les 10 % de chance d'obtenir une potion de faiblesse basique et ajoute-les
au capitaine pillard ».
- **Pillard ordinaire** : plus aucune potion.
- **Capitaine** (patrouille ou raid) : 10 % faiblesse basique + 1 % potion basique aléatoire (inchangé).

**Déployé sur Event le 28/09/2026 à 23:19. Statut : non testé en jeu.**

## 1.1.1 - pas de potion sur les mobs de spawner (03/10/2026, LeKiwi06)

Revue du 03/10/2026 (validé par LeKiwi06) : une ferme à spawner (blazes : potion de Force à 10 %, araignées...),
dont les spawners fabriqués avec KS_Spawners, donnait des potions à l'infini. Les mobs nés d'un spawner ne donnent
plus de potion (comme KS_Decapitator pour les têtes). Spawners d'épreuve (trial spawners) non concernés.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_lootpotions-1.1.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.2 - potion de la verrue : verrue mûre seulement (04/10/2026, LeKiwi06)

Techniquement nécessaire avec KS_LootBlocs 1.3.0 (signalé à LeKiwi06) : la verrue du Nether redevient cultivable, donc
on peut la replanter. Sans condition, poser puis casser aussitôt une verrue aurait donné 5 % de potion à chaque fois,
à volonté.
- Verrue du Nether : la potion aléatoire à 5 % ne tombe que si la verrue cassée par le joueur est **mûre** (dernier
  stade de croissance). Le reste est inchangé.
- **À déployer avec KS_LootBlocs 1.3.0 et KS_Crafts 1.10.0.**

**Déployé sur Event le 04/10/2026 à 06:52 (LeKiwi06 ; 1.1.1 dans `_removed-ks_lootpotions-1.1.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**
