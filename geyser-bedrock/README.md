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
| Jeton de fly (KS_Jetons) | `knowledge_book` | `minecraft:iron_ingot` | `kalium:jeton_fly` | lingot de fer |
| Jeton de la mort (KS_Jetons) | `knowledge_book` | `minecraft:netherite_ingot` | `kalium:jeton_mort` | lingot de netherite |
| Jeton de téléportation (KS_Jetons) | `knowledge_book` | `minecraft:gold_ingot` | `kalium:jeton_tp` | lingot d'or |
| Jeton de localisation (KS_Jetons) | `knowledge_book` | `minecraft:copper_ingot` | `kalium:jeton_localisation` | lingot de cuivre |
| Jeton de claim (KS_Jetons) | `knowledge_book` | `minecraft:golden_shovel` | `kalium:jeton_claim` | pelle en or |
| Badge de fly (KS_Jetons) | `knowledge_book` | `minecraft:iron_block` | `kalium:badge_fly` | face du bloc de fer (image plate) |
| Badge de la mort (KS_Jetons) | `knowledge_book` | `minecraft:netherite_block` | `kalium:badge_mort` | face du bloc de netherite (image plate) |
| Badge de téléportation (KS_Jetons) | `knowledge_book` | `minecraft:gold_block` | `kalium:badge_tp` | face du bloc d'or (image plate) |
| Badge de localisation (KS_Jetons) | `knowledge_book` | `minecraft:copper_block` | `kalium:badge_localisation` | face du bloc de cuivre (image plate) |
| Cristal de purification (KS_Meule) | `knowledge_book` | `minecraft:amethyst_shard` | `kalium:cristal_purification` | éclat d'améthyste |
| Casse-croûte doré (KS_CasseCroute) | `pumpkin_pie` | `minecraft:suspicious_stew` | `kalium:casse_croute_dore` | ragoût suspect |

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
- **1.3.0 (08/10/2026, LeKiwi06)** : jetons (fly, mort, téléportation, localisation : lingots ; claim : pelle en or) et
  badges (fly, mort, téléportation, localisation : face du bloc de fer, de netherite, d'or, de cuivre, en image plate)
  de KS_Jetons 2.0.0. Demande de LeKiwi06 : « tous les objets customs doivent figurer dans le proxy pour que les joueurs
  Bedrock les voient » ; vérifié : tous les objets faits sur un livre de connaissances (9 plugins) ont maintenant leur
  ligne. Le niveau d'un badge est dans son nom (même image pour tous les niveaux). Envoyé sur le proxy le 08/10/2026 à
  19:40 (1.2.0 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.2.0/` ; supprimable par l'humain :
  `_removed-kalium-objets-1.0.0/`) ; actif après redémarrage du proxy. **Non testé.**
- **Correspondance seule, sans nouveau pack (11/10/2026, LeKiwi06)** : signalement de LeKiwi06, « les joueurs bedrock ne
  peuvent pas stacker leurs jetons ». Cause : un jeton est un livre de connaissances (pile de 1) dont le serveur porte la
  pile à 64 ; Geyser ne reprend pas ce réglage tout seul, donc l'objet Bedrock gardait une pile de 1. Correction :
  `"components": {"minecraft:max_stack_size": 64}` sur les 5 jetons de `kalium_objets.json` (format des
  correspondances de Geyser, version 2). Les images ne changent pas : pack 1.3.0 gardé, pas de nouveau `.mcpack`.
  Les badges restent à 1 (ils ne s'empilent pas non plus sur Java). **Même défaut, non corrigé, signalé** : Clé de
  l'End, Bedrock Breaker, Fragment de Spawner, Cœur de Spawner et Changeur de Biome s'empilent par 64 sur Java et pas
  sur Bedrock. Envoyée avec la 1.4.0 (ci-dessous).
- **1.4.0 (11/10/2026, LeKiwi06)** : Cristal de purification de KS_Meule 1.1.0 (éclat d'améthyste, pile de 64 déclarée).
  Contient la correction des jetons (pile de 64). Pack `KaLium-objets-1.4.0.mcpack` construit (image extraite du jeu
  26.2). **Envoyé sur le proxy le 11/10/2026 à 09:00 (LeKiwi06 ; accord de LeKiwi06 : « oui déploie tout » ; `kalium_objets.json` et `KaLium-objets-1.4.0.mcpack` ; 1.3.0 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.3.0/` ; supprimables par l'humain : `_removed-kalium-objets-1.0.0/`, `1.1.0/`), actif après redémarrage du proxy. Non testé.**
- **Correspondance seule (11/10/2026, LeKiwi06 : « oui fix le soucis pour tout les items »)** : pile de 64 déclarée aussi pour la
  Clé de l'End, le Bedrock Breaker, le Fragment et le Cœur de Spawner et le Changeur de Biome (11 objets empilables sur
  16 ; l'Estomac du gardien et les badges restent à 1, comme sur Java). Pack 1.4.0 inchangé. **Envoyé sur le proxy le 11/10/2026 à 10:47 (LeKiwi06 ; `kalium_objets.json` seul ; celui de 09:00 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.4.0-correspondance/`), actif après redémarrage du proxy. Non testé.**

- **Réglage de pile retiré, en test (11/10/2026, Maxster33)** : après le redémarrage du proxy de 15:05, un téléphone
  Android restait bloqué au chargement des packs (côté proxy : aucune erreur, Geyser charge les 17 objets ; le
  téléphone abandonne seul, « Bedrock client disconnected »). Le pack 1.4.0 a été vérifié (identique au 1.3.0 à part
  le Cristal de purification) ; seule autre nouveauté depuis le 10/10, où Bedrock fonctionnait : les 11
  `"minecraft:max_stack_size": 64`. Retirés de `kalium_objets.json` (pack 1.4.0 inchangé). **Envoyé sur le proxy le
  11/10/2026 à 15:25 (Maxster33 ; l'ancien dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.4.0-pile64/`),
  actif après redémarrage du proxy. En attente du test** : si le téléphone entre, le réglage de pile est la cause et
  les objets reviennent à une pile de 1 sur Bedrock.
  **Résultat (15:30, après redémarrage) : toujours bloqué, le réglage de pile n'est pas la cause.** MRMister7866 est
  entré sur mobile à 15:31 (le proxy envoie bien les packs). Téléphone de Maxster33 en Minecraft 26.52 ; Geyser du
  proxy en b1249 (prise en charge de 26.52 depuis le 06/10) ; la b1250 (09/10) corrige le téléchargement des packs.
  **Points ouverts (relais à LeKiwi06)** : passer Geyser-Velocity en b1251 sur le proxy et retester ; puis remettre
  le réglage de pile (fichier gardé dans `_removed-kalium-objets-1.4.0-pile64/` sur le proxy, et dans le commit
  e428b1e du dépôt).
- **Geyser 2.12.0 (23/10/2026, LeKiwi06)** : `Geyser-Velocity-2.12.0-b1269.jar` (Java 26.3) à la place de la
  2.11.3-b1249 (rangée avec une copie du `config.yml` dans `/plugins/_removed-geyser-2.11.3-b1249/`). Correspondances
  (format 2) et pack 1.4.0 inchangés ; Floodgate 2.2.5 b141 inchangé (déjà la dernière). Envoyé le 23/10/2026 à 15:47 ;
  actif après redémarrage du proxy. **Non testé.**
- **Réglage de pile remis (26/10/2026, LeKiwi06 : « remets le réglage de pile (max_stack_size) dans geyser-bedrock et
  envoie-le sur le proxy »)** : `kalium_objets.json` revient à la version d'avant le retrait du 11/10 (commit e428b1e) :
  `"minecraft:max_stack_size": 64` sur les 11 objets empilables (Clé de l'End, Bedrock Breaker, Fragment et Cœur de
  Spawner, Changeur de Biome, 5 jetons, Cristal de purification) ; Estomac du gardien et badges à 1, comme sur Java.
  Retiré le 11/10 pour un test (téléphone bloqué au chargement des packs), ce réglage n'en était pas la cause. Pack
  1.4.0 inchangé. Fichier du proxy vérifié identique au dépôt avant l'envoi. **Envoyé sur le proxy le 26/10/2026 à
  14:33 (l'ancien dans `/plugins/Geyser-Velocity/_removed-kalium-objets-sans-pile-2026-10-26/`), actif après
  redémarrage du proxy. Non testé** : sur Bedrock, vérifier qu'un joueur entre toujours (packs chargés) et que les
  jetons s'empilent.
- **1.5.0 (27/10/2026, LeKiwi06)** : Casse-croûte doré de KS_CasseCroute 1.0.0 (image du ragoût suspect). Premier objet
  qui n'est pas sur un livre de connaissances (un livre ne peut pas être mangé) : base Java `pumpkin_pie`, d'où une
  nouvelle liste `minecraft:pumpkin_pie` dans `kalium_objets.json`, avec `minecraft:food` (7, 5, mangeable rassasié)
  et `minecraft:max_stack_size` 16 déclarés (Geyser ne reprend pas les composants changés par le serveur ; animation
  et durée de consommation : celles de la tarte, inchangées). Pack `KaLium-objets-1.5.0.mcpack` construit (image
  extraite du jeu 26.2). **Non envoyé sur le proxy.**
