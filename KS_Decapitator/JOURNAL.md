# KS_Decapitator - journal

Plugin autonome, serveur Event. Têtes de mobs. Cahier des charges : catégorie 1 « Contenu survie » de LeKiwi06
(validé le 29/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - têtes de mobs (30/09/2026)

Demande de LeKiwi06 : un plugin dans le style d'All Mob Heads, écrit par nous (plus simple à maintenir), qui donne
les têtes de tous les mobs ; mettre à jour les crafts de KS_Crafts qui ont besoin de têtes.

- **Chute** : 1 % (tous les mobs, réponse de LeKiwi06), seulement si un joueur tue le mob ; Butin sans effet. Aucune
  tête pour un mob né d'un spawner classique (vanilla ou KS_Spawners : raison d'apparition `SPAWNER`) ; les mobs des
  trial spawners en donnent. Ni joueur, ni Ender Dragon, ni géant, ni illusionniste, ni lapin tueur (absents de la table).
- **332 têtes** (table `src/main/resources/tetes.txt`) : une par variante visible (couleur, race, biome...), une par
  **état** (choix de LeKiwi06, 30/09/2026 : loup en colère, abeille pollinisée et / ou en colère, arpenteur frigorifié,
  chèvre hurleuse, vex en charge, creeper chargé) et une pour chaque **bébé** (« Tête de bébé ... »). Exceptions du
  cahier : poisson tropical = 1 tête ; villageois et zombie-villageois = 1 par métier (+ 1 bébé, qui n'a pas de métier) ;
  lapin « Toast » et mouton « jeb_ » (étiquette) ont leur tête. Le creeper chargé garde aussi son effet vanilla.
- **Tête custom pour tous** (zombie, squelette, creeper, piglin, wither squelette compris) ; têtes décoratives (le
  crâne custom de wither squelette n'invoque pas le Wither).
- **Objet** : tête de joueur à texture, nom « Tête de ... » blanc non italique, marqueur `ks_decapitator:tete` = id
  (ex. `bebe_mouton_rouge`, tiré du nom). Profil de la tête : UUID fixe propre à chaque id (les têtes identiques
  s'empilent).
- **Pose** : posables. Cassée (outil, explosion, piston, eau...), le jeu redonne une tête avec nom et texture mais sans
  le marqueur : toute tête du plugin qui apparaît au sol (`ItemSpawnEvent`) est reconstruite à l'identique, reconnue à
  l'UUID de son profil.
- **`/tetes`** (opérateurs, `ks.decapitator.tetes` ; ajouté à la demande de LeKiwi06 pour valider les textures) : menu
  de toutes les têtes, 45 par page ; clic = une tête dans l'inventaire.
- **Textures** : minecraft-heads.com bloque les robots (vérification anti-bot) ; accord de LeKiwi06 (30/09/2026) pour
  prendre celles de **MoreMobHeads** de JoelGodOfwar (licence MIT, mis à jour le 23/09/2026, textures issues de
  minecraft-heads.com). La table ne garde que l'empreinte de la texture (`textures.minecraft.net/texture/<empreinte>`).
  Pas repris : AllMobHeads (MLDEG), licence « tous droits réservés ».
- **Clé technique** calculée sur le mob tué : `<créature>[.<variante>][.<état>...][.baby]`, noms du jeu (ex.
  `sheep.red.baby`, `wolf.ashen.angry`, `villager.librarian`). Une clé absente de la table ne donne rien.
- **Autres plugins** : `creerTete(id)`, `idTete(objet)`, `ids()`, `tetesDe(créature)` (KS_Crafts 1.7.0,
  KS_KaliumGive 1.6.0).

Limites / à fournir par LeKiwi06 ou Maxster33 (valeur « Value » ou empreinte sur minecraft-heads.com, une ligne à
ajouter dans `tetes.txt`) :
- **Shulker** : seule la tête du shulker sans couleur existe ; les 16 shulkers colorés ne donnent rien pour l'instant
  (clés `shulker.white` ... `shulker.black`).
- **Cube de soufre** (`sulfur_cube`, bébé `sulfur_cube.baby`) et **bébé noyé** (`drowned.baby`) : pas de texture,
  rien pour l'instant.
- Cheval : une tête par robe (7), pas par marquage ; villageois : textures de la plaine pour tous les biomes.
- Les bébés viennent de MoreMobHeads (ajoutés dans sa version 1.0.45) : vérifier en jeu qu'ils correspondent bien aux
  nouvelles textures des bébés.
- Bedrock : les têtes posées s'affichent avec leur texture ; en inventaire, Geyser montre peut-être une tête de Steve
  (réglage `custom-skulls` de Geyser, à voir après le test).

**Non déployé. À déployer ensemble (règle 3.5) : KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0, KS_LootEntites 1.3.0. Statut : non testé en jeu.**
