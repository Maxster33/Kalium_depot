# KS_Textures - journal

Plugin de Kixster (survie). Aucune dépendance. Compilé contre le serveur Paper 26.3 (paquets réseau, composants des
objets) : à revérifier à chaque mise à jour de Minecraft.

## 1.0.0 - textures des objets custom (29/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « tu peux faire le texture pack » (après les textures 16×16 de `visuels-objets`).

Choix de LeKiwi06 (questions du 29/10/2026) :

| Question | Choix |
|---|---|
| Hébergement du pack | mc-packs.net, puis minepack.fr (introuvable de son côté ; hébergeur du serveur, « on limite les risques ») |
| Joueur Java qui refuse le pack | Pack facultatif : l'objet garde son image d'aujourd'hui |
| Bedrock | Mêmes textures (geyser-bedrock) |
| Méthode | Un seul plugin d'affichage, aucun autre plugin modifié (plutôt qu'une marque dans chaque plugin, qui aurait empêché les anciens objets de s'empiler avec les nouveaux) |

Fonctionnement :

- **Les objets ne changent pas sur le serveur.** Un gestionnaire placé dans la connexion de chaque joueur (juste avant
  `packet_handler`, par `ChannelInitializeListenerHolder` de Paper) ajoute à chaque objet custom reconnu sa texture en
  1re chaîne de `custom_model_data`, seulement dans ce qui est envoyé au joueur : inventaires et menus, objet tenu à la
  souris, équipement des entités (main, armure), objets au sol, cadres et présentoirs. Les anciens objets sont donc
  couverts aussi.
- **Reconnaissance** (`Regles.java`) : par la marque que chaque plugin écrit déjà dans l'objet (ex.
  `ks_jetons:jeton = fly`, `ks_economy:tier = 4`, `ks_jukeboxboat:bateau` sur un radeau). 60 textures. Pas de texture
  (choix du 29/10) : coffre renforcé et ses véhicules, pomme d'amour (image vanilla à l'aspect enchanté) ; le bateau
  (radeau) à coffre et jukebox prend l'image du bateau (radeau) à jukebox.
- **Créatif** : un objet renvoyé par un joueur en créatif perd la marque d'affichage avant d'arriver au serveur.
- **Pack** : fabriqué par `visuels-objets/pack.py` (`sortie/KaLium-textures-<version>.zip`, hors du dépôt : pixels de
  Mojang). Il remplace la définition vanilla de chaque objet de base (potion, bateaux de tous les bois, lingots...) par
  un choix sur la marque, la définition vanilla restant en dernier recours : sans pack, ou pour un objet ordinaire,
  rien ne change. Format des ressources 26.3 (97).
- **Envoi du pack** : à l'arrivée sur Kixster, à chaque joueur Java, facultatif, si `config.yml` donne `pack.url` et
  `pack.sha1` (vides par défaut : pack non proposé). Pas aux joueurs Bedrock (identifiant Floodgate).

Limites :

- Pas couverts (image d'aujourd'hui) : échanges des villageois, livre de recettes, objets montrés dans l'infobulle
  d'un sac ou d'une shulker.
- Après un clic dans un inventaire, le serveur renvoie les cases cliquées (l'objet du joueur porte la marque, pas
  celui du serveur) : quelques paquets de plus, rien de visible attendu.
- Ne pas recharger à chaud (`/reload`).
- Test hors jeu impossible (le jeu ne lie les composants des objets qu'au chargement de ses données) : rien n'a été
  vérifié en jeu.

Pack 1.0.0 déposé par LeKiwi06 sur minepack.fr (adresse et SHA-1 dans le `config.yml` par défaut ; SHA-1 du fichier
hébergé vérifié identique le 29/10). À déployer avec geyser-bedrock 1.8.0 sur le proxy pour Bedrock (mêmes
textures, reconnues par la même marque).

Compilé le 29/10/2026 (`sortie/KS_Textures-1.0.0.jar`).

**Déployé sur Kixster le 29/10/2026 à 10:43 (Claude de LeKiwi06 ; accord : « Oui, déploie KS_Textures, KS_CoffreRenforce et KS_JukeboxBoat sur Kixster et geyser-bedrock 1.8.0 sur le proxy » ; nouveau sur ce serveur, son `config.yml` y sera créé au premier démarrage avec l'adresse du pack), actif après redémarrage de Kixster. Statut : non testé en jeu.**

Actif sur Kixster depuis son redémarrage du 29/10/2026 à 10:47 (journal lu : chargé et activé sans erreur ; « 60 textures d'objets ; pack propose aux joueurs Java »). Statut : non testé en jeu.
Sur le proxy, geyser-bedrock 1.8.0 au même moment (voir `geyser-bedrock/README.md`).

## 1.1.0 - couleur de rareté dans le nom des objets custom (29/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'aimerai que nos items ait une couleur dans leurs noms pour signifier la rareté comme les
items de base du jeu ».

Choix de LeKiwi06 (questions du 29/10/2026) :

| Question | Choix |
|---|---|
| Méthode | Affichage seul, dans ce plugin (comme les textures) : anciens objets couverts, toujours empilables avec les nouveaux, aucun autre plugin modifié |
| Niveaux | Les 4 du jeu (Commun blanc, Peu commun jaune, Rare aqua, Épique violet clair) + Légendaire doré |
| Couleurs propres (alcools, élixirs, aliments) | Remplacées par la rareté |
| Choix de la rareté | Seuils de prix : 10 / 100 / 1 000 / 10 000 émeraudes, avec plancher vanilla (« Oui, code avec ces seuils et le plancher vanilla ») |

Tableau validé : `sortie/rarete/raretes-proposition.xlsx` (hors du dépôt ; 107 objets, onglet « Seuils » modifiable).

Fonctionnement (`Raretes.java`) :

- Dans ce qui est envoyé aux joueurs (mêmes paquets que les textures), le nom (`custom_name`) d'un objet custom reconnu
  prend la couleur de sa rareté ; les parties du nom perdent leur propre couleur (gras, etc. gardés).
- **Prix** : barème de KS_Economy (`rachats.csv`, copié dans le jar par `build.sh` : **recompiler KS_Textures après un
  changement de prix**) pour les objets marqués `pdc:` (têtes, spawners, élixirs...) et les jetons / blocs d'émeraude
  compressés par leur identifiant. Absents du barème, valeurs dans le code : jeton de fly 30, cristal 600, badges de
  niveau 1 (fly 450, téléportation 600, localisation 750, mort 3 600 ; chaque niveau × 2) = valeurs des récompenses ;
  jeton de localisation, coffre renforcé et ses véhicules, alcools, aliments, trains, verrou, bateaux à jukebox =
  recette estimée. Fioles d'expérience (KS_FioleExp, KS_CoffreMort) : selon les points contenus (prix de la fiole de
  15 niveaux ÷ 315 par point).
- **Plancher** : jamais moins rare que la rareté que le jeu donne à l'objet (fiole d'expérience et têtes : jaune au
  moins ; un objet enchanté monte d'un cran, comme en vanilla).
- **Créatif** : le nom d'origine est rangé dans l'objet envoyé (`custom_data`, clé `ks_textures:nom`) et lui est
  rendu quand un joueur en créatif le renvoie.
- **Pas recolorés** : atouts (équipement du joueur, son nom est celui du joueur), objets sans nom, têtes de joueurs.

Limites : comme les textures (échanges des villageois, livre de recettes, infobulle d'un sac ou d'une shulker) ; les
messages du chat qui citent un objet gardent la couleur d'aujourd'hui.

Vérifié hors jeu (classe d'essai lancée contre le serveur 26.3, hors du dépôt) : 426 objets custom lus dans le barème,
prix et rareté conformes au tableau pour 22 marques (tête d'allay Peu commun, bébé zombie Rare, Wither Épique, spawner
et élixir de Fortune Légendaire, fiole de mort de 7 000 points Rare, badge de la mort niveau 3 Légendaire...), nom
recoloré, nom d'origine rangé puis rendu à l'identique. Le passage par un vrai objet (rareté du jeu) n'a pas pu être
essayé hors jeu.

Empilée sur la 1.0.0, déployée le 29/10 mais jamais testée en jeu (demande explicite de LeKiwi06).

Compilé le 29/10/2026 (`sortie/KS_Textures-1.1.0.jar`).

**Déployé sur Kixster le 29/10/2026 à 11:04 (Claude de LeKiwi06 ; accord : « Oui, déploie KS_Textures 1.1.0 sur Kixster » ; jar en place vérifié identique à `jars-deployes/KS_Textures-1.0.0.jar` avant l'envoi, 1.0.0 rangée dans `_removed-ks_textures-1.0.0/` ; `config.yml` inchangé, aucune nouvelle clé), actif après redémarrage de Kixster. Statut : non testé en jeu.**
