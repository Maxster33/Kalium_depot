# 28/09/2026 — Serveur Event : estomac du gardien et loots des coffres de structures

- Plugin(s) concerné(s) : KS_EstomacGardien (nouveau), KS_LootCoffres (nouveau), KS_LootEntites, KS_LootPotions
- Versions avant / après : — → 1.0.0, — → 1.0.0, 1.0.0 → 1.1.0, 1.0.0 → 1.1.0

## Demandé
Maxster33 : modifier le plugin de loot existant, créer un plugin pour un nouveau coffre (« Estomac du gardien », image
du sac noir) et un plugin pour les nouveaux loots des coffres de structures (cité antique, bastions, trésor enfoui,
cité de l'End, forteresse du Nether, avant-poste, manoir, chambres d'épreuve) ; grand gardien 50 % d'estomac ; potion
de faiblesse du pillard déplacée vers le capitaine. Puis : « met tout sur le serveur event ».

## Fait
- Tables de loot vanilla de la 26.2 lues dans le jeu installé pour partir des vrais poids.
- 4 plugins écrits, compilés, déployés sur Event le 28/09/2026 à 23:19 ; `jars-deployes/`, journaux,
  `REPRISE_PROJET.md` et `KS_Event/CAHIER_DES_CHARGES.md` (partie 4) à jour.

## Décisions
Questions posées avant de coder, toutes les réponses dans `KS_Event/CAHIER_DES_CHARGES.md` (partie 4). Choix
technique : modifier le loot vanilla après génération plutôt que réécrire les tables (tout ce qui n'est pas cité
reste vanilla) ; changements de poids par remplacement à probabilité exacte.

## Reste à faire
- Redémarrer Event (l'humain) et tester (voir le compte rendu dans `REPRISE_PROJET.md`).
- Ajouté ensuite : KS_EstomacGardien 1.1.0 (`/estomac`, 23:25), puis à la demande de Maxster33 commande retirée
  (1.2.0) et nouveau plugin KS_KaliumGive 1.0.0 (`/kaliumgive <pseudo> <id_custom> <nombre>`, id `estomac_gardien`),
  déployés à 23:35.
- Ajouté ensuite (23:47) : KS_EC_Extension 1.0.0 (coffre de l'Ender à 6 lignes, 3 du bas débloquées case par case
  avec la Clé de l'End ; réponses : 1 clé = 1 case, barrière, craft sans forme, clé par 64), KS_Crafts 1.1.0 (craft de
  la clé), KS_KaliumGive 1.1.0 (id `cle_de_l_end`). À tester.
