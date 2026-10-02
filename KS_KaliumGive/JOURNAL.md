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

**Non déployé (catégorie 4, étape 1 ; déploiement prévu avec les étapes suivantes). Statut : non testé.**
