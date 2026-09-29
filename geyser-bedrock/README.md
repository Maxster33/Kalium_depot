# geyser-bedrock - apparence Bedrock des objets custom

Geyser (sur le proxy) ne transmet pas le composant `item_model` aux joueurs Bedrock : sans ce dossier, un objet custom
fait sur un livre de connaissances s'affiche comme un livre. Ici, Geyser reconnaît ces objets par leur `item_model` et
les affiche avec une image Bedrock. **Apparence seulement** : les objets restent des livres de connaissances neutres
côté serveur (aucun effet du vrai sac ou de la vraie clé).

| Objet (plugin) | Objet Java de base | `item_model` | Objet Bedrock | Image |
|---|---|---|---|---|
| Estomac du gardien (KS_EstomacGardien) | `knowledge_book` | `minecraft:black_bundle` | `kalium:estomac_gardien` | sac noir |
| Clé de l'End (KS_EC_Extension) | `knowledge_book` | `minecraft:ominous_trial_key` | `kalium:cle_de_l_end` | clé des épreuves sinistre |
| Bedrock Breaker (KS_BedrockBreaker) | `knowledge_book` | `minecraft:blaze_rod` | `kalium:bedrock_breaker` | bâton de blaze |
| Fragment de Spawner (KS_ItemSimple) | `knowledge_book` | `minecraft:disc_fragment_5` | `kalium:fragment_spawner` | fragment de disque |
| Cœur de Spawner (KS_ItemSimple) | `knowledge_book` | `minecraft:respawn_anchor` | `kalium:coeur_spawner` | côté de l'ancre de réapparition (image plate) |
| Changeur de Biome (KS_BiomeChanger) | `knowledge_book` | `minecraft:end_crystal` | `kalium:changeur_biome` | cristal de l'End |

Attention : **tout** livre de connaissances qui a l'un de ces `item_model` prend cette apparence ; un futur objet
custom doit avoir son propre `item_model` (ou une autre base) et sa propre ligne ici.

## Fichiers

- `custom_mappings/kalium_objets.json` : correspondance Geyser (format 2, `type: definition`), à mettre dans
  `/plugins/Geyser-Velocity/custom_mappings/` du proxy.
- `pack/` : sources du pack de ressources Bedrock (`manifest.json`, `textures/item_texture.json`). Les images ne sont
  **pas** dans le dépôt (public ; images de Mojang) : `build.sh` les extrait du jeu Java installé.
- `build.sh` : `export PATH="/c/Program Files/Java/jdk-27/bin:$PATH"; sh geyser-bedrock/build.sh` →
  `sortie/KaLium-objets-<version>.mcpack`, à mettre dans `/plugins/Geyser-Velocity/packs/` du proxy.

## Ajouter un objet

1. Dans le plugin : `meta.setItemModel(...)` avec un `item_model` qui n'est utilisé par aucun autre objet custom.
2. Une entrée dans `custom_mappings/kalium_objets.json` (`model` = cet `item_model`) et dans
   `pack/textures/item_texture.json` ; l'image dans `build.sh`.
3. Changer la version du pack (`manifest.json`, `header.version`, et `VERSION=` dans `build.sh`) pour que les
   joueurs Bedrock retéléchargent le pack ; retirer l'ancien `.mcpack` de `packs/` (le ranger dans un `_removed-…`).
4. Redémarrer le proxy (l'humain).

## Historique

- **1.0.0 (29/09/2026, Maxster33)** : Estomac du gardien et Clé de l'End. Geyser 2.11.3-b1245 sur le proxy,
  `enable-custom-content: true`, `force-resource-packs: true`. Envoyé sur le proxy le 29/09/2026 à 00:18 ; actif
  après redémarrage du proxy. **Testé et confirmé par Maxster33 le 29/09/2026** (le matin, les objets étaient encore
  des livres : le proxy n'avait pas été redémarré).
- **1.1.0 (29/09/2026, Maxster33)** : Bedrock Breaker (image du bâton de blaze). Pack `KaLium-objets-1.1.0.mcpack` (1.0.0
  retiré de `packs/`) et `kalium_objets.json` mis à jour sur le proxy ; anciens fichiers rangés hors de `packs/` et de
  `custom_mappings/` (Geyser lit peut-être les sous-dossiers). Envoyé sur le proxy le 29/09/2026 à 12:56 (anciens fichiers dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.0.0/`) ; actif après redémarrage du proxy. **Non testé.**
- **1.2.0 (29/09/2026, Maxster33)** : Fragment de Spawner (fragment de disque), Cœur de Spawner (côté de l'ancre de
  réapparition : l'ancre est un bloc en 3D sur Java, Bedrock n'a qu'une image plate pour un objet custom) et Changeur
  de Biome (cristal de l'End). Envoyé sur le proxy le 29/09/2026 à 17:23 (1.1.0 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.1.0/`) ; actif après redémarrage du proxy. **Non testé.**
