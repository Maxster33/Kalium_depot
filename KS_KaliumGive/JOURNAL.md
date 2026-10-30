# KS_KaliumGive - journal

Plugin autonome, serveur Event. Demande de Maxster33 du 28/09/2026.

## 1.0.0 - commande /kaliumgive (28/09/2026)

- **`/kaliumgive <pseudo> <id_custom> <nombre>`** : donne un objet custom du serveur. Opérateurs seulement
  (permission `ks.kaliumgive`, `default: op`). Les 3 arguments sont obligatoires ; nombre de 1 à 64 (limite ajoutée
  contre les fautes de frappe) ; un objet à la fois, inventaire plein : le reste tombe au sol. Complétion : pseudos en
  ligne, puis id_custom. id inconnu : le message donne la liste.
- **Liste des id_custom** (dans le code, `OBJETS`) :
  - `estomac_gardien` : Estomac du gardien (créé par KS_EstomacGardien, `creerEstomac()`).
- Chaque objet est créé par le plugin qui le définit (`softdepend`) : toujours identique à celui du jeu ; si ce plugin
  n'est pas activé, l'id est refusé. `build.sh` compile contre les classes de KS_EstomacGardien : **compiler
  KS_EstomacGardien d'abord**.
- Ajouter un objet : une ligne dans `OBJETS`, son plugin dans `softdepend` (`plugin.yml`) et dans le classpath de
  `build.sh`.

**Déployé sur Event le 28/09/2026 à 23:35. Statut : non testé en jeu.**

## 1.1.0 - id cle_de_l_end (28/09/2026)

Demande de Maxster33 : **`cle_de_l_end`** ajouté à la liste (Clé de l'End, créée par KS_EC_Extension,
`KSECExtension.creerCle()` ; `softdepend` et `build.sh` : compiler KS_EC_Extension d'abord).

**Déployé sur Event le 28/09/2026 à 23:47 (1.0.0 dans `_removed-ks_kaliumgive-1.0.0/`). Statut : non testé en jeu.**

## 1.2.0 - id fiole_exp(<points>) (29/09/2026)

Demande de Maxster33 : **`fiole_exp(<points>)`**, ex. `/kaliumgive Maxster33 fiole_exp(1395) 1` donne une
« Fiole d'expérience (1 395 XP) » (créée par KS_FioleExp, `KSFioleExp.creerFiole(points)` ; 1 à 999 999 999 points).
Complétion : `fiole_exp(` ; le message d'id inconnu cite `fiole_exp(<points>)`. `softdepend` et `build.sh` :
compiler KS_FioleExp d'abord. Déployé avec KS_FioleExp 1.0.0 (nécessaire pour cet id).

**Déployé sur Event le 29/09/2026 à 03:39 (1.1.0 dans `_removed-ks_kaliumgive-1.1.0/`). Statut : testé et confirmé par Maxster33 le 29/09/2026.**

## 1.3.0 - id bedrock_breaker (29/09/2026)

Demande de Maxster33 : id **`bedrock_breaker`** (objet créé par KS_BedrockBreaker, `KSBedrockBreaker.creerBreaker()`),
ex. `/kaliumgive Maxster33 bedrock_breaker 5`. `softdepend` et `build.sh` : compiler KS_BedrockBreaker d'abord.
À déployer avec KS_BedrockBreaker 1.0.0.

**Déployé sur Event le 29/09/2026 à 12:55 avec KS_BedrockBreaker 1.0.0 (1.2.0 dans `_removed-ks_kaliumgive-1.2.0/` ; supprimable par l'humain, 3 versions derrière : `_removed-ks_kaliumgive-1.0.0/`). Statut : non testé en jeu.**

## 1.4.0 - fiole_exp(N) en niveaux (29/09/2026)

Choix de Maxster33 (avec KS_FioleExp 1.5.0) : `fiole_exp(N)` donne une fiole de **N niveaux** (points pour passer du
niveau 0 au niveau N, 1 à 20 000), ex. `/kaliumgive Maxster33 fiole_exp(50) 1` → « Fiole d'expérience (niveau 50) »
(5 345 points). Message d'id inconnu : `fiole_exp(<niveaux>)`. Utilise `KSFioleExp.creerFioleNiveaux(niveaux)`.

**Déployé sur Event le 29/09/2026 à 14:18 avec KS_FioleExp 1.5.0 (1.3.0 dans `_removed-ks_kaliumgive-1.3.0/` ; supprimables par l'humain, 3 versions derrière : `_removed-ks_kaliumgive-1.0.0/`, `1.1.0/`). Statut : non testé en jeu.**

## 1.5.0 - spawners, fragments, cœur, têtes, Changeur de Biome (29/09/2026)

Demande de Maxster33 : nouveaux id `fragment_spawner`, `coeur_spawner` (KS_ItemSimple), `changeur_biome`
(KS_BiomeChanger), `spawner_zombi`, `spawner_squelette`, `spawner_araignee`, `spawner_creeper`, `spawner_blaze`,
`spawner_mouton`, `spawner_vache`, `spawner_poule` (KS_Spawners) ; plus `tete_araignee`, `tete_blaze`, `tete_mouton`,
`tete_vache`, `tete_poule` (têtes « Steve » de KS_ItemSimple, seul moyen de les obtenir pour l'instant). « spawner
araignée » de la demande écrit `spawner_araignee` (pas d'espace ni d'accent dans un id). `softdepend` et `build.sh` :
compiler KS_ItemSimple, KS_BiomeChanger et KS_Spawners d'abord.

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (1.4.0 dans `_removed-ks_kaliumgive-1.4.0/` ; supprimables par l'humain : `_removed-ks_kaliumgive-1.0.0/` à `1.2.0/`). Statut : non testé en jeu.**

## 1.6.0 - élixirs et têtes de mobs (30/09/2026)

Demande de LeKiwi06 (catégorie 1 « Contenu survie ») :
- **Élixirs** (KS_Elixir) : `elixir_super_gateau`, `elixir_chauve_souris`, `elixir_anguille`, `elixir_ignifugation`,
  `elixir_plume`, `elixir_phenix`, `elixir_fantome`, `elixir_titan`, `elixir_vent`, `elixir_rebond`,
  `elixir_fortune`.
- **Têtes** (KS_Decapitator) : `tete_<id>` pour les 332 têtes (ex. `tete_mouton_rouge`, `tete_bebe_loup_cendre`),
  liste lue dans KS_Decapitator au démarrage (complétion de la commande, ou `/tetes`).
- Retirés : les 5 têtes « Steve » de KS_ItemSimple (même ids `tete_araignee`, `tete_blaze`... repris par les têtes de
  KS_Decapitator ; `tete_mouton`, `tete_vache`, `tete_poule` n'existent plus : `tete_mouton_blanc`, `tete_vache_temperee`,
  `tete_poule_temperee`...).
- Message d'id inconnu : la liste n'affiche plus chaque tête (`tete_<tête>`).
- `softdepend` et `build.sh` : KS_Decapitator, KS_Elixir (compiler d'abord).

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (1.5.0 dans `_removed-ks_kaliumgive-1.5.0/` ; supprimables par l'humain : `_removed-ks_kaliumgive-1.0.0/` à `1.3.0/`), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.7.0 - fonction publique de création (03/10/2026, LeKiwi06)

Catégorie 4 « Récompenses » : `KSKaliumGive.creer(id)` crée un objet custom d'Event par son id_custom (comme
`/kaliumgive`, fioles `fiole_exp(N)` comprises), ou null si l'id est inconnu ou son plugin désactivé. Utilisé par
KS_RewardsGUI pour les récompenses en objets custom. Aucun autre changement.

**Déployé sur Event le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (ancienne version dans `_removed-ks_kaliumgive-1.6.0/`), actif après redémarrage. Statut : non testé en jeu.**

## 1.8.0 - jetons (03/10/2026, LeKiwi06)

Catégorie 7 : id_custom `jeton_tp`, `jeton_emplacement`, `jeton_claim`, `jeton_mort` (objets de KS_Jetons), pour
`/kaliumgive` et les tables de butin des récompenses. `softdepend` KS_Jetons.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.8.0 ; ancienne version dans `_removed-ks_kaliumgive-1.7.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.9.0 - jetons en lingots et badges (08/10/2026, LeKiwi06)

KS_Jetons 2.0.0 : id_custom lus dans KS_Jetons au démarrage (comme les têtes) : `jeton_fly`, `jeton_mort`, `jeton_tp`,
`jeton_claim` et `badge_<type>_<niveau>` (`badge_fly_1` à `5`, `badge_mort_1` à `7`, `badge_tp_1` à `5`,
`badge_localisation_1` à `5`). Retiré : `jeton_emplacement` (jeton supprimé). Pas de `jeton_localisation` : il ne vient
que du craft, lié à une magnétite. Le message d'id inconnu ne liste pas chaque badge (`badge_<type>_<niveau>`).

**Déployé sur Event le 08/10/2026 à 19:09 (LeKiwi06, avec KS_Jetons 2.0.0 ; 1.8.0 dans `_removed-ks_kaliumgive-1.8.0/` ;
supprimables par l'humain : `_removed-ks_kaliumgive-1.0.0/` à `1.6.0/`), actif après redémarrage d'Event. Statut : non
testé en jeu.**

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 1.9.0 ; 1.8.0 dans `_removed-ks_kaliumgive-1.8.0/`), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**

## 1.10.0 - Cristal de purification (11/10/2026, LeKiwi06)

KS_Meule 1.1.0 (demande de LeKiwi06 : un objet qui retire les malédictions, donné par les coffres de récompense) :
nouvel id_custom `cristal_purification`, créé par KS_Meule (`softdepend`). C'est par cet id que KS_RewardsGUI crée
l'objet à l'ouverture d'un coffre.

À déployer avec KS_Meule 1.1.0.

**Déployé sur Kixster le 11/10/2026 à 09:00 (LeKiwi06, envoi groupé : KS_Meule 1.1.0, KS_KaliumGive 1.10.0, KS_Economy 1.7.0, KS_Tableau 1.0.1 et `fusion.yml` de KS_RewardsGUI ; accord de LeKiwi06 : « oui déploie tout » ; 1.9.0 dans `_removed-ks_kaliumgive-1.9.0/`), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.9.0). Statut : non testé en jeu.**

## 1.11.0 - bateaux à jukebox (13/10/2026, LeKiwi06)

Demande de LeKiwi06, après ses essais des deux bateaux de KS_JukeboxBoat sur Event : « met les dans le kaliumgive
aussi ».

- Nouveaux id_custom, lus au démarrage dans la liste des bateaux du jeu (comme les têtes et les jetons) : un par
  bateau, **`jukebox_<id du bateau dans le jeu>`**. Exemples : `jukebox_oak_boat` (bateau à jukebox en chêne),
  `jukebox_oak_chest_boat` (bateau à coffre et jukebox en chêne), `jukebox_bamboo_raft`, `jukebox_bamboo_chest_raft`.
  22 ids en 26.3 (11 bois) ; un bois ajouté par une version future suivra.
- L'objet est créé par KS_JukeboxBoat (`softdepend`) : identique à celui de la recette.
- Le message d'id inconnu ne liste pas chaque bateau (`jukebox_<bateau> (complétion)`), comme pour les badges.

Choix de Claude, signalé : ids formés sur les noms du jeu (en anglais) plutôt que traduits, pour suivre d'eux-mêmes
les bois du jeu.

Exige KS_JukeboxBoat 1.1.0 ou plus (les bateaux à coffre).

Compilé le 13/10/2026 (`sortie/KS_KaliumGive-1.11.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Kixster le 13/10/2026 à 14:07 (LeKiwi06 : « tu peux déployer sur kixster » ; envoi groupé avec KS_JukeboxBoat 1.1.2 et KS_Crafts 1.14.1 ; 1.10.0 dans `_removed-ks_kaliumgive-1.10.0/` ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.10.0). Statut : non testé en jeu.**

Actif sur Kixster depuis son redémarrage du 13/10/2026 à 14:11 (journal lu : 1.11.0 chargée et activée sans erreur). Statut : non testé en jeu sur Kixster.

## 1.12.0 - kit de test (30/10/2026, LeKiwi06)

Demande de LeKiwi06 : « créer moi des kits de test avec la commande /kalium give, il faut que ça me donne un coffre dans
lequel il y a des shulker, ces shulker doivent être renommés par le nom du plugin (trains, alcool, etc.) pour que je
puisse tout tester efficacement : il me faut les items finis, et de quoi faire leurs crafts ».

- **`/kaliumgive <pseudo> kit_test <nombre>`** (nouvelle classe `KitTest`) : un coffre « Kit de test » plein de boîtes
  de shulker nommées par le plugin sans « KS_ » (Trains, Alcools, Cuisine, Atouts, Crafts...). Dans chaque boîte : les
  objets finis du plugin, puis les ingrédients d'une fois chacune de ses recettes. Coffre et boîtes gardent leur contenu
  quand on les pose. Plus de 27 piles : « Alcools (1/2) », « Alcools (2/2) » ; plus de 27 boîtes : un 2e coffre.
- Contenu lu au moment de la commande, pas recopié (une recette ajoutée plus tard suit d'elle-même) :
  - objets finis : les id_custom de `/kaliumgive` par plugin (sans les 332 têtes, toujours par `tete_<tête>`), une fiole
    d'expérience de 30 niveaux, et le résultat de chaque recette qui porte une marque d'un plugin KS_ ; l'objet va dans
    la boîte du plugin de sa marque (la locomotive, recette de KS_Crafts, dans « Trains ») ;
  - ingrédients : chaque recette du serveur (établi, four, fumoir, haut fourneau, tailleuse, forge) dont le résultat
    porte la marque d'un plugin KS_ ou dont la clé est celle d'un plugin KS_. Une case qui accepte plusieurs objets
    donne le premier (un seul bois, une seule pièce d'armure pour un atout, une seule tête de la créature) ; les
    quantités de plusieurs recettes s'additionnent ;
  - résultat sans marque (objet du jeu, aperçu d'un atout) : seulement ses ingrédients (les atouts se posent au craft).
- **Alambic de KS_Alcools** : l'API de Paper ne liste pas les mélanges d'alambic, donc écrit dans `KitTest.alcools()` :
  chaque alcool dans chaque variante (normal, long, fort ; à boire et jetable), 3 fioles d'eau, l'ingrédient de chaque
  alcool, redstone, glowstone, poudre à canon, poudre de blaze. **À tenir à jour avec `Alcools.java` de KS_Alcools**
  (nouvel alcool, Shéjiǔ de l'étape 6). `softdepend` et `build.sh` : KS_Alcools (et KS_Effets, KLM_Menu pour compiler).
- `kit_test` n'est pas un id_custom : `creer()` (récompenses de KS_RewardsGUI) ne le connaît pas.

Limites : un seul exemplaire des ingrédients par recette ; aucun établi, four ni alambic dans le kit ; le coffre est un
gros objet (centaines d'objets décrits) : non essayé avec un joueur Bedrock.

Compilé le 30/10/2026 (`sortie/KS_KaliumGive-1.12.0.jar`).

**Déployé sur Kixster le 30/10/2026 à 11:39 (LeKiwi06 : « Oui, déploie » ; jar en place vérifié identique à la référence avant l'envoi ; 1.11.0 dans `_removed-ks_kaliumgive-1.11.0/` ; supprimables par l'humain, 3 versions derrière ou plus : `_removed-ks_kaliumgive-1.8.0/`, `1.9.0/`), actif après redémarrage de Kixster. Statut : non testé en jeu.**

## 1.13.0 - amulette mangeuse de mal (30/10/2026)

Chantier des maléfices du Nether (cahier local `Malefices_Nether.md`, validé par LeKiwi06 le 30/10/2026) : id
`amulette_mangeuse_de_mal` (KS_MaleficesNether, softdepend), pour `/kalium give` et le butin des récompenses (2 % des
tirages Légendaires, choix de LeKiwi06).

Compilé le 30/10/2026 (`sortie/KS_KaliumGive-1.13.0.jar`). **À déployer avec KS_MaleficesNether 1.0.0.** Statut : non
déployé, non testé en jeu.
