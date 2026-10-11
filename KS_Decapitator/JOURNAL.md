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

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (nouveau), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.1.0 - tête de wither squelette sale, endommagée, désactivée (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : la tête de wither squelette a la description « Sale », « Endommagée », « Désactivée » (et
« À réparer à l'établi pour invoquer le Wither ») ; chaque défaut se répare par un craft de KS_Crafts 1.8.0, dans
n'importe quel ordre ; réparée des 3, elle devient un **vrai crâne de wither squelette** (qui invoque le Wither).
- Abîmée, elle se pose comme décoration (sans invoquer) ; ses défauts restants sont gardés dans le bloc et rendus
  quand elle est cassée (aussi par une explosion ou un piston). Sans information, elle redevient abîmée de ses 3
  défauts (jamais réparée par erreur).
- API : `estTeteWitherSquelette`, `defautsTete`, `reparer(tête, défaut)`.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.0 ; ancienne version dans `_removed-ks_decapitator-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.0 - /tetes pour tous : têtes déjà obtenues (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai aussi que les joueurs aient accès au /tetes mais que celui ci leur servent a savoir
celle qu'ils ont déjà obtenus ( l'avoir eu dans son inventaire via un échange par exemple compte aussi , pas la peine
de l'avoir tué soit même ) ».

- **`/tetes` ouvert à tous les joueurs** (plus de permission sur la commande). Même menu, 45 têtes par page ; le titre
  donne le compte du joueur (« Têtes 12/332 - page 1/8 »).
  - Tête déjà obtenue : la tête elle-même, avec « Obtenue » en dernière ligne de sa description.
  - Tête jamais obtenue : une vitre grise à son nom, avec « Pas encore obtenue ».
  - Un clic ne donne rien à un joueur.
- **Opérateurs** (`ks.decapitator.tetes`, comme avant) : toutes les têtes sont montrées (avec leur état) et un clic en
  met une dans l'inventaire.
- **Une tête est « obtenue » dès qu'elle a été dans l'inventaire du joueur**, quelle que soit sa provenance (mob tué,
  échange, coffre, don, récompense) : notée quand il la ramasse au sol, et par un contrôle de son inventaire (armure et
  main secondaire comprises) toutes les 5 secondes, à la connexion, à la déconnexion et à l'ouverture de `/tetes`.
  Une tête obtenue le reste, même donnée, posée ou perdue ensuite.
- Données : `plugins/KS_Decapitator/collection.yml` (créé à la première tête notée), enregistré dans les 5 secondes
  qui suivent un changement et à l'arrêt.
- Textes écrits dans le code, comme le reste du plugin (pas de `lang.yml`).

Choix de Claude, signalés :
- une tête jamais obtenue montre son nom (la liste des 332 têtes est donc visible de tous), pas sa texture ;
- pas de message quand une nouvelle tête est notée ; pas de bouton dans le menu du serveur : `/tetes` seulement.

Limites :
- Rien n'est rattrapé pour le passé : une tête rangée dans un coffre ou posée avant cette version ne compte que
  lorsqu'elle repasse par l'inventaire du joueur. Celles qu'il porte sur lui sont notées à sa première connexion.
- Une tête prise puis reposée en moins de 5 secondes sans passer par le sol (d'un coffre à un autre, sans fermer)
  peut échapper au contrôle.
- Le coffre de l'Ender et les boîtes de shulker ne sont pas regardés.
- Bedrock : dans un inventaire, Geyser montre peut-être une tête de Steve (limite déjà notée en 1.0.0) ; le nom et
  « Obtenue » restent lisibles, et une tête manquante est une vitre grise sur les deux éditions.
- Le crâne de wither squelette entièrement réparé est un objet du jeu : il ne compte pas (la tête abîmée, oui).
- Empilé sur la 1.1.0, jamais testée en jeu, à la demande de LeKiwi06.

**Compilé le 11/10/2026 (`sortie/KS_Decapitator-1.2.0.jar`). Non déployé. Statut : non testé en jeu.**
