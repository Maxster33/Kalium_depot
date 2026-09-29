# KS_ItemSimple - journal

Plugin autonome, serveur Event. Objets qui n'ont pas d'autre fonction que le craft (demande de Maxster33, 29/09/2026).

## 1.0.0 - Fragment et Cœur de Spawner, têtes « Steve » (29/09/2026)

Demande de Maxster33 : nouveau plugin pour les objets de craft ; « Fragment de Spawner » avec l'apparence des fragments
de disque ; « Coeur de Spawner » avec l'apparence de l'ancre de réapparition, qui ne peut pas être posé. Puis (choix de
Maxster33) : les têtes d'araignée, de blaze, de mouton, de vache et de poule n'existant pas en vanilla, 5 têtes « Steve »
nommées en attendant un plugin dédié aux têtes.

| Objet | Base | Image (`item_model`) | Id (marqueur `ks_itemsimple:objet`) |
|---|---|---|---|
| Fragment de Spawner | livre de connaissances | `minecraft:disc_fragment_5` | `fragment_spawner` |
| Cœur de Spawner | livre de connaissances | `minecraft:respawn_anchor` | `coeur_spawner` |
| Tête d'araignée, de blaze, de mouton, de vache, de poule | tête de joueur (Steve) | - | `tete_araignee`, `tete_blaze`, `tete_mouton`, `tete_vache`, `tete_poule` |

- Tous empilables par 64, nom blanc non italique.
- Livres de connaissances : clic droit vanilla annulé (il les consommerait). Têtes : ne peuvent pas être posées (pose
  annulée) ; elles peuvent être portées sur la tête comme une tête de joueur.
- Autres plugins : `creerFragmentSpawner()`, `creerCoeurSpawner()`, `creerTete(animal)`, `idObjet(objet)` (KS_Crafts
  1.6.0, KS_LootBlocs 1.1.0, KS_KaliumGive 1.5.0).
- Apparence Bedrock : `geyser-bedrock/` 1.2.0 (fragment et cœur ; sur Bedrock, le cœur a l'image plate du côté de
  l'ancre). Les têtes sont de vraies têtes de joueur (Steve) sur Java et Bedrock.

Limites :
- Un distributeur peut encore poser une tête (elle devient alors une tête de joueur ordinaire).

**Compilé le 29/09/2026, non déployé. Statut : non testé en jeu.**
