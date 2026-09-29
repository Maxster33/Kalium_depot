# 2026-09-29 — Spawners et Changeur de Biome (serveur Event)

- Plugin(s) concerné(s) : KS_ItemSimple (nouveau), KS_Spawners (nouveau), KS_BiomeChanger (nouveau), KS_Crafts,
  KS_KaliumGive, KS_LootBlocs, geyser-bedrock
- Versions avant / après : — / 1.0.0 (x3) ; KS_Crafts 1.5.0 / 1.6.0 ; KS_KaliumGive 1.4.0 / 1.5.0 ; KS_LootBlocs 1.0.0 /
  1.1.0 ; geyser-bedrock 1.1.0 / 1.2.0

## Demandé
Maxster33 : KS_ItemSimple (objets qui ne servent qu'au craft : Fragment de Spawner, image des fragments de disque ; Coeur
de Spawner, image de l'ancre de réapparition, non posable) ; KS_BiomeChanger (Changeur de Biome, image du cristal de
l'End ; clic droit : menu des biomes de l'overworld, overworld seulement ; sphère de 32 blocs ou cube de même volume ;
aucun bloc ne bouge ; objet revérifié puis consommé, message « Vous avez changé le biome pour : <biome> » ; historique,
bouton « Historique », opérateurs : tout l'historique et téléportation d'un clic) ; crafts des 8 spawners (7 fragments +
cœur + tête) et du Changeur de Biome ; spawners avec créature, empilables par 64, règles vanilla, tombent quand on les
casse ; id KS_KaliumGive ; KS_LootBlocs : 1 fragment par spawner + 5 % d'un 2e. Ensuite : recettes des spawners et du
Changeur de Biome dans le livre de recettes dès l'obtention d'un Fragment de Spawner.

## Décisions (réponses de Maxster33)
- Têtes d'araignée, blaze, mouton, vache, poule (inexistantes en vanilla) : têtes « Steve » ; comme les 5 recettes
  auraient été identiques, 5 têtes « Steve » nommées dans KS_ItemSimple (données par /kaliumgive), en attendant un
  plugin dédié aux têtes.
- Fonctionnement des spawners dans un nouveau plugin KS_Spawners.
- Cassage : n'importe quel outil ; spawner posé : tombe tel quel, sans fragments ni XP ; spawner naturel : fragments et
  XP vanilla, pas le spawner.
- Réservations : plus de 2 plugins entre 13 h et 23 h, avec l'accord de LeKiwi06 (selon Maxster33).
- Choix de Claude, signalés : sphère (pas le cube) ; menus par la boîte à outils de KLM_Menu (charte) ; objets sur base
  livre de connaissances comme les autres objets custom ; tout empilable par 64 ; crafts sans forme.

## Reste à faire
- Déployer (Event : 6 jars ; proxy : correspondance Geyser et pack 1.2.0), redémarrer Event et le proxy (l'humain),
  tester.
- À décider : Changeur de Biome dans les zones WorldGuard ; loot des 5 têtes (plugin des têtes) ; accès des opérateurs à
  l'historique sans Changeur de Biome.

## Suite (17:19)
- Demandé : choix sphère ou cube pour le Changeur de Biome ; pas de changement dans les zones WorldGuard ; spawner
  fabriqué détruit par une explosion : il tombe au sol.
- Choix de Maxster33 : refus dès que la zone touche une région WorldGuard, même si le joueur en est membre.
- Fait (versions 1.0.0 non déployées complétées) : bouton « Forme » dans le menu (choix retenu par joueur jusqu'au
  redémarrage), vérification WorldGuard cellule par cellule avant de consommer l'objet (API WorldGuard 7.0.19 /
  WorldEdit 7.4.5, déjà dans `telecharger-outils.sh`), forme notée dans l'historique ; KS_Spawners : spawner posé
  détruit par une explosion lâché au sol.
- « oui installe » : déployé sur Event le 29/09/2026 à 17:22 (KS_ItemSimple, KS_Spawners, KS_BiomeChanger 1.0.0,
  KS_Crafts 1.6.0, KS_KaliumGive 1.5.0, KS_LootBlocs 1.1.0 ; anciens dans `_removed-ks_crafts-1.5.0/`,
  `_removed-ks_kaliumgive-1.4.0/`, `_removed-ks_lootblocs-1.0.0/`) et sur le proxy à 17:23 (geyser-bedrock 1.2.0 ;
  anciens dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.1.0/`). Réservations libérées. Redémarrer Event et le
  proxy (l'humain).
