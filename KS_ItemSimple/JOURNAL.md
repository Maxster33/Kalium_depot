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

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (apparence Bedrock : geyser-bedrock 1.2.0 sur le proxy à 17:23), actifs après redémarrage d'Event et du proxy. Statut : non testé en jeu.**

## 1.1.0 - têtes « Steve » retirées (30/09/2026)

Demande de LeKiwi06 (catégorie 1 « Contenu survie ») : les têtes de mobs viennent maintenant de KS_Decapitator ; les
5 têtes « Steve » (`tete_araignee`, `tete_blaze`, `tete_mouton`, `tete_vache`, `tete_poule`) sont retirées avec
`TETES` et `creerTete` (aucune n'est en circulation selon LeKiwi06). Le refus de pose (qui ne servait qu'aux têtes)
est retiré aussi : un livre de connaissances ne se pose pas. Fragment et Cœur de Spawner inchangés.

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (1.0.0 dans `_removed-ks_itemsimple-1.0.0/`), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**
