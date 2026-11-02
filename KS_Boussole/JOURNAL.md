# KS_Boussole - journal

Plugin de Kixster (préfixe `KS_`). Dépend de KLM_Menu (menus en coffre) et de KS_BiomeChanger (Changeur de Biome du
craft).

## 1.0.0 - boussole du voyageur (02/11/2026)

**Demande (LeKiwi06, 01-02/11/2026, cahier local `Blocs_Cultures_Objets.md`, section 2)** : une boussole craftée avec
une boussole et un Changeur de Biome, qui cherche un biome choisi dans un menu en coffre (Terralith compris) dans
2 000 blocs autour du joueur, sans générer de chunks, puis pointe vers lui comme une boussole magnétisée.
Choix de LeKiwi06 du 02/11 : recherches illimitées avec 15 minutes de recharge, Changeur de Biome neuf seulement,
utilisable dans l'overworld, le Nether et l'End.

- **Craft sans forme** : une boussole ordinaire + un Changeur de Biome neuf (10 utilisations sur 10, exactement l'objet
  de `KSBiomeChanger.creerChangeur()`). Recette enregistrée par le plugin (`ks_boussole:boussole_voyageur`), aussi dans
  un crafter. Un Changeur entamé, ou d'avant la 1.2.0 de KS_BiomeChanger (sans compteur), n'est pas accepté.
- **Objet** : boussole nommée « Boussole du voyageur », une par case (régler une boussole d'une pile les réglerait
  toutes), marquée `ks_boussole:boussole_voyageur` ; description « Pointe vers : <biome> » une fois réglée.
- **Clic droit** (en l'air ou sur un bloc) : menu en coffre de 6 rangées, un objet par biome que la génération du
  monde du joueur peut produire (fournisseur de biomes du monde : Terralith compris, biomes du Nether dans le
  Nether...), trié par nom français (`biomes.yml`, copie de celui de KS_Tableau 1.2.0 ; biome absent : nom tiré de
  son identifiant), 45 par page ; le biome visé brille. Barre du bas : pages, état (biome visé, recharge), Fermer.
  Un clic sur un bloc qui s'utilise (coffre, porte...) garde son effet normal, sauf accroupi ; un clic sur une
  magnétite ouvre le menu (la boussole ne se règle pas sur la magnétite). Un seul menu par seconde (clics doublés de
  Bedrock).
- **Recherche** : `World#locateNearestBiome(origine, rayon, 32, 64, biome)`, la recherche de `/locate biome`
  (vérifié dans le code 26.3 : `BiomeSource.findClosestBiome3d` ne lit que le bruit de génération, aucun chunk chargé
  ni généré ; pas de contrôle du fil d'exécution dans `CraftWorld`), lancée hors du fil principal ; une à la fois par
  joueur. Trouvé : la boussole tenue (sinon l'autre main, sinon la première de l'inventaire) devient magnétisée vers
  le point trouvé (`CompassMeta#setLodestone`, sans magnétite suivie), message au-dessus de la barre d'objets.
  Pas trouvé, changement de monde pendant la recherche, plus de boussole : message, rien ne change.
- **Recharge** : 15 minutes par joueur après une recherche réussie (une recherche sans résultat ne compte pas),
  gardée dans les données du joueur (survit au redémarrage, vaut pour toutes ses boussoles).
- `config.yml` : `rayon` (2000), `recharge-minutes` (15) ; valeurs par défaut dans le code. Textes dans `lang.yml`.
- **Bedrock** : vérifié dans Geyser 2.11.3 (`CompassItem`, `LodestoneCache`,
  `BedrockPositionTrackingDBClientRequestTranslator`) : une boussole avec `lodestone_tracker` devient une
  `minecraft:lodestone_compass` de Bedrock, dont le jeu demande la position à Geyser : l'aiguille devrait pointer aussi
  sur Bedrock. **À confirmer en jeu.** Repli si ce n'est pas le cas : direction et distance du biome dans la
  description de la boussole ou au-dessus de la barre d'objets quand elle est tenue.

Limites : un joueur dans une autre dimension que celle du point visé voit l'aiguille tourner (comme en vanilla). Le
point trouvé est une cellule de biome à la hauteur où la recherche l'a trouvé (biomes de grottes : sous terre).

Compilé ; **déployé sur Kixster le 02/11/2026 à 13:19** (jar de 14 511 octets, même taille que celui du dépôt ; nouveau plugin, rien rangé), **à redémarrer, non testé en jeu**. Prix validé par LeKiwi06 le 02/11 : **513,2** (boussole 0,5 + Changeur de Biome 512,7), à ajouter dans `rachats.csv` de KS_Economy (ligne `boussole_voyageur;boussole_voyageur;513.2;pdc:ks_boussole:boussole_voyageur:b;Boussole du voyageur;1;`). Restent à ajouter : entrée du didacticiel (KS_Didacticiel).
