# KS_Blocs - journal

Plugin de Kixster (survie). Socle des blocs custom (cahier local `Blocs_Cultures_Objets.md`, partie 0) et premier
bloc : les jack o'lanterns bleue et cuivre (partie 8). Dépend de KLM_Menu (textes). Compilé contre le serveur Paper 26.3
(paquets réseau, sections de chunk) : à revérifier à chaque mise à jour de Minecraft. Ne pas recharger à chaud.

## 0.2.0 - plantes custom : canne à sucre et varech (02/11/2026, pour KS_Cultures)

**Demande de LeKiwi06** (cultures de Kixster, cahier local, parties 10 à 14, validées le 02/11) : les stades de pousse
des cultures sont des blocs custom sur la canne à sucre (cultures sur terre) et le varech (riz). L'API reste compatible
(rien d'enlevé ni de changé pour les jack o'lanterns, le répéteur wifi ou la boîte aux lettres).

- `apparences.json` : `ks_cultures_terre` (canne à sucre, âges 1 à 12 : pousse, jeune, fleur, tomate, poivron,
  aubergine, oignon, salade, mais_bas, mais_haut, kiwi, kiwi_fruits ; âges 13 à 15 libres) et
  `ks_cultures_eau` (varech, âges 1 à 3 : pousse, jeune, riz ; âges 4 à 25 libres). Plusieurs types peuvent partager une
  apparence (chaque végétal de KS_Cultures est un type, tous sur ks_cultures_terre).
- Nouveau : `textures_variantes` (facultatif) dans une apparence, pour une texture par variante (un modèle par stade).
- `Plantes` (nouveau) : un bloc custom à base de canne à sucre ou de varech ne vit plus selon les règles vanilla :
  - règle de survie de la base (canne à sucre sans eau à côté, appui retiré...) : `BlockDestroyEvent` de Paper annulé,
    l'écouteur est prévenu (`verifie`) au tick suivant, au plugin du type de décider ;
  - pas de pousse vanilla (canne à sucre au-dessus, varech qui monte), pas de poudre d'os du jeu (distributeur) ;
  - pas de vraie canne à sucre ni de vrai varech posés dessus ;
  - pistons : un piston qui casserait une de ces plantes ne bouge pas (sinon butin vanilla : de la canne à sucre) ;
  - éponge : elle ne retire pas un varech custom.
  - L'eau ne casse ni la canne à sucre ni le varech (code du jeu : FlowingFluid) : pas de récolte par l'eau.
- `KSBlocs.pourChaque(id, action)` devient public (pousse des cultures).
- `pack/blocs.py` 0.2.0 : textures par variante, croix Bedrock (`minecraft:geometry.cross`, sans collision, cadre de
  sélection de plante, cassée d'un coup), textures provisoires des cultures (images du jeu), option `--geyser <dossier>`
  pour un essai sans toucher à geyser-bedrock. **Pas lancé sur geyser-bedrock** (dossier commun) : à lancer avec la
  mise à jour de geyser-bedrock (essai fait dans un dossier à part : 25 états réservés, sortie correcte).

Limites : rien testé en jeu. Bedrock : la couche d'eau autour du riz (varech custom) est à vérifier en premier dans
Kixster Test, ainsi que le clic droit sur une plante custom (cueillette des arbustes).

**Compilé le 02/11/2026, non déployé, non testé en jeu.** À déployer avec KS_Cultures 1.0.0 et geyser-bedrock (nouvelle
version, correspondance régénérée par `blocs.py`).

## 0.1.1 - lumière de la jack o'lantern cuivre à 13 (02/11/2026)

**Question de LeKiwi06** : « si tu as mis des light 14 autour de la jack o lantern de cuivre ça ne risque pas de poser
des problème pour le cookie glowberrie ? » Oui : KS_Cuisine reconnaît ses lumières à leur seul niveau (14). Au
chargement d'un chunk, il efface toutes les lumières 14 qu'aucun joueur ne porte (et fouille tout le chunk, environ
100 000 blocs, dès qu'il en contient une) ; un joueur au cookie qui passe devant laissait la jack o'lantern éteinte ; et
casser une jack o'lantern retirait n'importe quelle lumière devant elle, celle d'un joueur au cookie comprise.

Choix de LeKiwi06 : « Oui, passe à 13 ». Changements :

- jack o'lantern cuivre : lumière 13 au lieu de 14 (bleue inchangée, 10) ; KS_Cuisine n'est pas modifié ;
- une jack o'lantern ne retire plus que la lumière de son propre niveau ;
- devant une lumière d'un autre niveau (cookie), elle n'en pose pas ; les jack o'lanterns des chunks chargés revérifient
  leur lumière toutes les 5 s (place libérée par un cookie, un piston...).

**Compilé le 02/11/2026. Déployé sur Kixster le 02/11/2026 à 13:44 (LeKiwi06 : « Oui, déploie »), avec geyser-bedrock 1.11.0 sur le proxy ; à redémarrer, non testé en jeu.** Remplace la 0.1.0 (jamais déployée).

## 0.1.0 - socle et jack o'lanterns (02/11/2026, prototype)

**Demande de LeKiwi06** (liste du 01/11, validée le 02/11) : des blocs custom pour Kixster, **sans détourner les blocs
musicaux** (les joueurs doivent pouvoir s'en servir), visibles sur Java et sur Bedrock ; base de quatre ajouts à venir
(cultures et fleurs, répéteur wifi, boîte aux lettres, encapsuleur). Premier bloc : jack o'lanterns bleue et cuivre.

Choix de LeKiwi06 (02/11) : recettes comme la vraie (citrouille sculptée + torche des âmes / torche de cuivre) ; lumière
de leur torche (bleue 10, cuivre 14) ; textures faites par lui (provisoires en attendant).

### Méthode : remplacement visuel

Vérifié dans le code du jeu 26.3 : le bloc musical, la canne à sucre et le varech ont un seul modèle pour tous leurs
états (`blockstates/*.json` à une seule variante).

- **Sur le serveur, de vrais blocs** : un bloc custom plein est un vrai bloc musical (le client prédit donc la bonne
  durée de casse, le bon outil et la bonne collision). Sa position est retenue dans les données du chunk
  (`ks_blocs:blocs`, `Registre`).
- **Dans ce qui est envoyé aux joueurs** (`Reecriture`, gestionnaire dans la connexion, comme KS_Textures) : chunks,
  changement d'un bloc, changements groupés, particules de casse. Les vrais blocs de ces types sont montrés dans un seul
  état (`note_block[instrument=harp,note=0,powered=false]`, `sugar_cane[age=0]`, `kelp[age=0]`), les autres états sont
  réservés aux blocs custom (`src/main/resources/apparences.json`, le registre des états). Un bloc de base qu'aucune
  apparence n'utilise n'est pas touché : aujourd'hui seul le bloc musical l'est.
- **Pas de PacketEvents** : la réécriture passe par le même accrochage que KS_Textures (`ChannelInitializeListenerHolder`
  de Paper) et lit / réécrit les sections de chunk avec le code du jeu lui-même (`LevelChunkSection.read/write`) : aucun
  plugin à installer, et le format des chunks suit le jeu. GrimAC (qui embarque PacketEvents) voit les états réécrits :
  même collision (bloc plein), pas de faux signalement attendu.
- **États réservés du bloc musical : note 1 à 24 seulement.** Un joueur Java qui pose un bloc musical le prédit en note
  0 : un état réservé en note 0 ferait clignoter un bloc custom à sa place.
- **Vrais blocs musicaux** : le son vient du serveur (`playSeededSound`, bonne note, vérifié en 26.3) ; seule la
  particule (et le son de Geyser, qui lit la note dans l'état qu'il voit) suivait l'état montré. Le paquet d'événement
  du bloc est remplacé par une particule de note de la bonne couleur (instrument sans note, tête : rien, comme en
  vanilla) ; Bedrock garde le son du serveur.
- **Prédiction du client** : quand un joueur pose ou casse un bloc au-dessus ou en dessous d'un bloc musical, son jeu
  recalcule l'instrument et change l'état montré du bloc custom voisin ; KS_Blocs lui renvoie les blocs custom voisins
  au tick suivant.

### Vie des blocs (`Evenements`)

- Pose par l'objet (bloc musical marqué `ks_blocs:type`) ; variante : face vers le joueur pour un bloc orientable.
- Casse : butin du type (par défaut son objet ; l'API reçoit le joueur et l'outil, pour le toucher de soie), rien en
  créatif. Explosions : butin avec la chance de l'explosion. Wither, ravageur : butin du bloc custom. Feu : rien.
- Pistons : la position suit le bloc (file de blocs comprise), écouteurs prévenus à l'ancienne place puis, après le
  mouvement, à la nouvelle.
- Le bloc musical d'un bloc custom ne joue jamais (clic gauche, redstone) et ne se règle pas (clic droit) ; un clic droit
  avec un bloc en main le pose contre lui. L'objet ne brûle pas dans un four.
- Remplacé sans passer par KS_Blocs (WorldEdit, /setblock) : la position est oubliée dès qu'un joueur reçoit ce bloc.

### API (autres plugins, `depend: [KS_Blocs]`)

`TypeBloc` (id, apparence, base, objet, butin, variante posée, écouteur) ; `KSBlocs.declarer`, `objet(id, n)`,
`id(objet)`, `type(...)`, `variante(bloc)`, `poser`, `enregistrer`, `changerVariante` (stades de pousse), `retirer`,
`deplacer` (encapsuleur), `renvoyer`, `verifierAutour` ; `EcouteurBloc` (`pose`, `retire`, `verifie`). Une nouvelle
apparence = une entrée dans `apparences.json` (états libres), puis `python KS_Blocs/pack/blocs.py`.

### Jack o'lanterns (`JackOLanterns`)

- Bloc musical à l'image de la jack o'lantern (orientable, 4 variantes) : bleue = harpe notes 1 à 4, cuivre = notes 5 à
  8 (nord, est, sud, ouest).
- Lumière : un bloc de lumière (10 ou 14, 13 depuis la 0.1.1) devant la face, seulement dans l'air ou une source d'eau, retiré avec la jack
  o'lantern, remis dès que la place se libère (voisin cassé, chunk chargé).
- Objet : bloc musical à l'image (`item_model`) de la jack o'lantern vanilla, en attendant sa règle KS_Textures.

### Textures

`KS_Blocs/pack/blocs.py` fabrique, à partir de `apparences.json` : le pack Java `sortie/KaLium-blocs-0.1.0.zip` (définition
complète du bloc musical, 1 350 états), la correspondance Bedrock `geyser-bedrock/custom_mappings/kalium_blocs.json` et
`geyser-bedrock/pack/textures/terrain_texture.json` ; textures provisoires dans `visuels-objets/png/blocs/` (hors du
dépôt) si elles manquent : jack o'lantern vanilla dont la lueur est teintée en bleu ou en vert, côté et dessus de la
citrouille.

### Limites

- La lumière dépend du bloc de lumière devant : face bouchée, pas de lumière.
- La poudre de redstone ne se tourne pas vers un bloc custom ; un observateur voit le bloc musical changer d'état.
- Créatif : la pioche (clic molette) donne un bloc musical.
- Bedrock : l'objet en main garde l'image du bloc musical (pas de correspondance d'objet tant que KS_Textures n'a pas sa
  règle) ; durée de casse Bedrock fixée à 1 s dans la correspondance, à vérifier.

**Compilé le 02/11/2026, non déployé, non testé en jeu.**
