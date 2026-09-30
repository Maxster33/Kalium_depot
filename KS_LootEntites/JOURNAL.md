# KS_LootEntites - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - loots des mobs (25/09/2026)

Réductions et remplacements : à toutes les morts (fermes comprises). Ajouts : seulement si un joueur tue ; Butin sans
effet sur les pourcentages.
- **Golem de fer** : lingots → même nombre de pépites ; coquelicots divisés par 2 (arrondi au hasard), chacun
  remplacé par une fleur d'un bloc de haut au hasard : 1 % wither rose, 1 % pitcher plant, 1 % torchflower, sinon une
  des 12 fleurs vanilla (pissenlit, coquelicot, orchidée, allium, houstonie, 4 tulipes, marguerite, bleuet, muguet).
- **Zombifié (pigmen)** : tous les loots ÷ 2 (arrondi au hasard). **Sorcière** : tous les loots ÷ 5.
- **Pillard** : plus de fiole sinistre (seuls les capitaines en lâchaient).
- **Calmar, calmar lumineux** : drops ×2. **Wither squelette** : plus de crâne (le Wither ne peut plus être invoqué
  avec des crânes de ce serveur, confirmé par Maxster33), charbon ×2, 10 % de wither rose.
- **Chair putréfiée** (tous les mobs qui en lâchent : zombies, noyés, momifiés, zombies villageois, zoglins, chevaux
  zombies, zombifiés) : chaque chair a 50 % de chance de devenir un os.
- **Endermite** : 50 % de chance d'un de ces 6 objets (chances égales) : améthyste bourgeonnante, carapace de
  shulker, potion de régénération basique, fiole d'expérience niveau 10, fruit de chorus, 8 perles de l'Ender.
- **Warden** : 10 % d'une fiole d'expérience de niveau 10, 20, 30, 40 ou 50 (au hasard).
- **Fiole d'expérience (niveau N)** : fiole vanilla nommée et marquée ; lancée, elle donne l'XP pour passer du niveau
  0 au niveau N (160 / 550 / 1 395 / 2 920 / 5 345 points).
- Potions des autres mobs : dans KS_LootPotions.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - grand gardien : Estomac du gardien (28/09/2026)

Demande de Maxster33 : « Grand Gardien : ajouter 50 % de chance d'obtenir l'estomac du gardien (nouveau coffre) ».
- **Grand gardien** : 50 % d'un Estomac du gardien, seulement si un joueur le tue (comme les autres ajouts ; accord de
  Maxster33).
- L'objet vient de **KS_EstomacGardien** (`softdepend`) : sans ce plugin activé, rien n'est ajouté. `build.sh`
  compile contre les classes de KS_EstomacGardien : **compiler KS_EstomacGardien d'abord**.
- **À déployer avec KS_EstomacGardien 1.0.0.**

**Déployé sur Event le 28/09/2026 à 23:19. Statut : non testé en jeu.**

## 1.2.0 - Warden : tirage de fioles d'expérience (29/09/2026)

Demande de Maxster33 : retirer la fiole d'expérience du Warden (10 % d'une fiole de niveau 10, 20, 30, 40 ou 50) et la
remplacer par 1 tirage (niveaux revus à la baisse par Maxster33 avant déploiement : d'abord 20, 30, 40, 50, 60) :

| Fiole d'expérience | Quantité | Poids | Chance |
|---|---|---|---|
| niveau 10 (160 points) | 1 | 15 | 42,9 % |
| niveau 15 (315 points) | 1 | 10 | 28,6 % |
| niveau 20 (550 points) | 1 | 6 | 17,1 % |
| niveau 30 (1 395 points) | 1 | 3 | 8,6 % |
| niveau 40 (2 920 points) | 1 | 1 | 2,9 % |

- Chaque Warden tué par un joueur lâche **toujours** une de ces fioles (le tirage n'a pas d'entrée « rien »). Comme les
  autres ajouts du plugin : seulement s'il est tué par un joueur.
- Fioles « Fiole d'expérience (niveau N) » de KS_LootEntites (marqueur `xp_level`) ; elles n'ont pas la description en
  points des fioles de KS_FioleExp 1.5.0 et ne s'empilent pas avec elles.

**Endermite** (demande de Maxster33, même version) : dans son tirage, l'ancienne fiole de niveau 10 est remplacée par
la **nouvelle fiole de KS_FioleExp** (`KSFioleExp.creerFioleNiveaux(10)` : « Fiole d'expérience (niveau 10) »,
description « 160 points d'expérience », s'empile avec les fioles de 10 niveaux faites à l'enclume). `softdepend`
KS_FioleExp ; `build.sh` : compiler KS_FioleExp d'abord. Sans KS_FioleExp activé : l'ancienne fiole (le tirage garde
ses 6 objets).

Précision de Maxster33 : les loots ajoutés de l'endermite et du Warden ne tombent que s'ils sont tués par un joueur
(déjà le cas depuis 1.0.0, vérifié dans le code).

**Déployé sur Event le 29/09/2026 à 14:18 avec KS_FioleExp 1.5.0 (1.1.0 dans `_removed-ks_lootentites-1.1.0/`), actif après
redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.1 - Warden : 10 % de fiole (29/09/2026)

Demande de Maxster33 : 10 % de chance d'obtenir une fiole, par une ligne « rien » dans le tirage du Warden. Les fioles
pèsent 35 au total : « rien » pèse 315 (35 / 350 = 10 %).

| Résultat | Poids | Chance |
|---|---|---|
| rien | 315 | 90 % |
| fiole niveau 10 | 15 | 4,29 % |
| fiole niveau 15 | 10 | 2,86 % |
| fiole niveau 20 | 6 | 1,71 % |
| fiole niveau 30 | 3 | 0,86 % |
| fiole niveau 40 | 1 | 0,29 % |

Toujours seulement si le Warden est tué par un joueur. Endermite inchangée.

**Déployé sur Event le 29/09/2026 à 14:22 (1.2.0 dans `_removed-ks_lootentites-1.2.0/` ; supprimable par l'humain, 3 versions derrière : `_removed-ks_lootentites-1.0.0/`), actif après redémarrage d'Event. Statut : **testé et confirmé par LeKiwi06 le 30/09/2026**.**

## 1.3.0 - une seule fiole : celle de KS_FioleExp (30/09/2026)

Demande de LeKiwi06 (catégorie 1 « Contenu survie ») : un seul système de fiole, celui de Maxster33 (KS_FioleExp).
- **Warden** : même tirage (10, 15, 20, 30 ou 40 niveaux, 10 % au total), mais fiole de KS_FioleExp au lieu de
  l'ancienne fiole « niveau N » de ce plugin.
- **Endermite** : la fiole de 10 niveaux était déjà celle de KS_FioleExp (1.2.0) ; l'ancienne fiole de secours (sans
  KS_FioleExp) est retirée.
- Sans KS_FioleExp : pas de fiole du tout (avertissement au démarrage).
- Les anciennes fioles « niveau N » déjà en jeu restent utilisables (lecture gardée), mais ne sont plus créées.

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (1.2.1 dans `_removed-ks_lootentites-1.2.1/` ; supprimables par l'humain : `_removed-ks_lootentites-1.0.0/` et `1.1.0/`), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.4.0 - Warden : fioles plus communes, catalyseur remplacé (30/09/2026)

Demande de LeKiwi06 : « rendre les fioles d'xp dans le warden 50 % plus communes, et randomiser le loot du catalyst par
tous les blocs du biome deep dark » ; précisions : le catalyseur est celui que lâche le Warden (« ça manque de
diversité »), blocs du biome seulement, minerais extrêmement rares, 1 bloc, XP gardée.

- **Fioles** : 15 % au lieu de 10 % (poids multipliés par 3, mêmes proportions entre niveaux), toujours si un joueur
  tue le Warden.

| Tirage (sur 700) | Poids | Chance |
|---|---|---|
| rien | 595 | 85 % |
| fiole niveau 10 | 45 | 6,43 % |
| fiole niveau 15 | 30 | 4,29 % |
| fiole niveau 20 | 18 | 2,57 % |
| fiole niveau 30 | 9 | 1,29 % |
| fiole niveau 40 | 3 | 0,43 % |

- **Catalyseur de sculk** du Warden (à toutes ses morts, comme les autres remplacements) : remplacé par **un** bloc au
  hasard des blocs naturels du Deep Dark, sans la cité antique. XP du Warden inchangée.
  Tirage sur 10 002 :
  - Poids 765 chacun (7,65 %) : sculk, veine de sculk, capteur de sculk, hurleur de sculk, catalyseur de sculk,
    deepslate, deepslate pavée, tuf, gravier, pierre, granite, diorite, andésite.
  - Poids 8 chacun (0,08 %, 0,56 % pour un minerai quelconque) : minerais de deepslate de charbon, fer, cuivre, or,
    redstone, lapis, diamant.
  - Poids 1 (0,01 %, demande de LeKiwi06) : minerai d'émeraude de deepslate (absent du Deep Dark en vanilla).
- Un hurleur de sculk posé par un joueur n'appelle pas de Warden (comportement vanilla).

**Déployé sur Event le 30/09/2026 à 22:44 (LeKiwi06 ; 1.3.0 dans `_removed-ks_lootentites-1.3.0/` ; supprimables par l'humain, 3 versions derrière ou plus : `_removed-ks_lootentites-1.0.0/`, `1.1.0/`, `1.2.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

