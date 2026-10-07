# KX_Monde : génération du monde de Kixster (26.3)

Datapacks de génération du futur monde de Kixster (chantier de Maxster33, 07/10/2026). Ils ne servent qu'à la
**création** d'un monde : ils doivent être dans `<monde>/datapacks/` avant le premier démarrage sur ce monde.

| Fichier | Rôle |
|---|---|
| `datapacks/tectonic-datapack-3.0.29.zip` | Tectonic (Modrinth, version datapack pour 26.3, inchangée) : relief |
| `datapacks/kalium-terralith-1.0.0.zip` | Construit par `BuildPack.java` à partir de Terralith 2.6.5+26.3 (datapack Modrinth) |

## kalium-terralith

Garde seulement 11 biomes de Terralith (choix de Maxster33) : forested_highlands, frozen_cliffs, glacial_chasm,
rocky_mountains, shield, siberian_taiga, volcanic_crater, volcanic_peaks, warm_river, wintry_forest, white_cliffs.

- **Aucune structure de Terralith** (pas de `structure`, `structure_set`, `template_pool`, `processor_list`).
  Les structures vanilla (villages, puits de mine, portails en ruine...) peuvent apparaître dans ces 11 biomes.
- Les autres biomes de Terralith sont remplacés dans `dimension/overworld.json` par le biome vanilla que
  Minecraft 26.3 placerait au même point climatique (rapport `biome_parameters` du générateur de données) ;
  les grottes de Terralith sont retirées (sous-sol vanilla). Détail : `rapport-construction.txt`.
- Pas le relief de Terralith (noise_settings, density_function) : c'est Tectonic qui fait le relief. Les règles
  de surface de Terralith (`material_rule`) sont gardées, nettoyées des biomes retirés.

Test local (Paper 26.3-159, graine `kalium`) : aucune erreur de génération ; les 11 biomes trouvés.
glacial_chasm et wintry_forest sont rares (un tous les 1 000 à 7 000 blocs selon l'endroit).

## Reconstruire

```
java -DbundlerMainClass=net.minecraft.data.Main -Djavax.net.ssl.trustStoreType=Windows-ROOT -jar paper-26.3-159.jar --reports --output out
java -cp libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar BuildPack.java <Terralith dézippé> out/reports/biome_parameters/minecraft/overworld.json pack forested_highlands,frozen_cliffs,...
```
Puis zipper le contenu de `pack/` (pack.mcmeta à la racine du zip).
