# KS_Cultures 1.0.0 - ajouts à faire dans les plugins et fichiers communs

KS_Cultures enregistre lui-même tout ce qui lui est propre ; ces ajouts reviennent aux sessions qui tiennent les
plugins communs (02/11/2026). Prix validés le 02/11 (`sortie/cultures/cultures-proposition.xlsx`), en émeraudes à
l'unité.

## 1. KS_Economy (barème, `sortie/bareme`)

Objets marqués `ks_cultures:objet = <id>` (base : livre de connaissances).

| Id | Objet | Prix | Décomposition |
|---|---|---|---|
| tomate, poivron, aubergine, oignon, salade | Tomate... | 0,029296875 | = carotte (légume d'une culture simple) |
| riz | Riz | 0,029296875 | = carotte (le grain est aussi la semence) |
| cerises, kiwi | Cerises, Kiwi | 0,029296875 | = baies sucrées (arbuste, le fruit se replante) |
| mais | Épi de maïs | 0,048828125 | = blé (2 blocs de haut, plus lent) |
| graine_tomate ... graine_salade, graine_mais | Graines | 0,01953125 | = graines de blé (graine <= fruit ; composteur : 1 fruit -> 1 graine) |
| graine_dandelion, graine_poppy | Graines de pissenlit / coquelicot | 0,010185943 | fleur (0,020371885) / 2 : 1 fleur donne 2 graines en moyenne |
| graine_blue_orchid, graine_allium, graine_azure_bluet, graine_red_tulip, graine_orange_tulip, graine_white_tulip, graine_pink_tulip, graine_oxeye_daisy, graine_cornflower, graine_lily_of_the_valley | Graines de petite fleur | 0,019920658 | fleur (0,039841317) / 2 |
| graine_eyeblossom | Graines d'oeilchidée | 0,02 | oeilchidée (0,04) / 2 (même règle, validé par LeKiwi06 le 02/11) |
| graine_sunflower, graine_rose_bush | Graines de tournesol / rosier | 0,022099618 | grande fleur à 2 colorants (2 x 0,022099618) / 2 |
| graine_lilac, graine_peony | Graines de lilas / pivoine | 0,033201097 | grande fleur à 2 colorants (2 x 0,033201097) / 2 |
| melange | Mélange de graines | 0,17578125 | validé : 9 graines au prix d'une graine de légume (voir l'alerte ci-dessous) |

Grandes fleurs (**maintenant que la duplication à la poudre d'os est coupée par KS_Cultures**, à mettre au barème au
déploiement de KS_Cultures, pas avant) : tournesol, rosier 0,044199235 (2 colorants jaunes / rouges) ; lilas, pivoine
0,066402195 (2 colorants magenta / roses).

Torchiflore et plante carnivore : leurs graines vanilla restent à 0,02203125 (fleur / 2, comme le composteur donne 2
graines en moyenne) : rien à changer.

**Alerte, contrôle des rapports** : un mélange se fait avec n'importe quelles 9 sortes de graines. Les 9 sortes les
moins chères (citrouille 0,0031 ; pissenlit, coquelicot 0,0102 ; pastèque 0,0125 ; 5 petites fleurs 0,0199) coûtent
environ 0,136, moins que le prix du mélange (0,176) : s'il se vend au prix du barème, il rapporte ~0,04 par mélange.
LeKiwi06 (02/11) : « pas gênant comme ça », prix gardé à 0,17578125.

Rareté (KS_Textures, seuils 10 / 100 / 1 000 / 10 000) : tout est sous 10, rareté commune ; recompiler KS_Textures après
le changement de prix.

## 2. KS_Crafts

Fait dans cette session (accord du brief) : KS_Crafts 1.24.0 retire `ks_crafts:pitcher_pod` et
`ks_crafts:torchflower_seeds`. À déployer avec KS_Cultures.

## 3. KS_Didacticiel (`didacticiel.yml`)

- Fiche « Crafts pratiques » (section `crafts`) : retirer `ks_crafts:pitcher_pod` et `ks_crafts:torchflower_seeds` de
  la liste des recettes et « graines » du texte.
- Nouvelle fiche (section `monde` ou une section « cultures ») :
  - « Composteur » : clic droit main vide sur un composteur : les fleurs donnent 1 à 3 graines, les fruits des
    végétaux déjà récoltés donnent leurs graines, les graines se stockent ; 9 sortes stockées donnent un mélange de
    graines (un végétal au hasard).
  - « Fleurs » : graine sur la terre labourée, la fleur pousse ; les grandes fleurs ne se dupliquent plus à la poudre
    d'os.
  - « Nouveaux végétaux » : tomate, poivron, aubergine, oignon, salade, maïs (sur la terre labourée), riz (sous l'eau),
    cerises et kiwi (arbustes, clic droit pour cueillir) ; graines trouvées en cassant les herbes de certains biomes
    (tomate : savanes ; poivron : badlands, déserts ; aubergine : jungles ; oignon : taïgas ; salade : forêts ; maïs :
    plaines, prairies ; riz : marais, rivières ; cerises : bosquets de cerisiers ; kiwi : jungles de bambous) ; récolte
    à la main seulement ; les fruits se mangent ; poudre d'os : un stade de plus.

## 4. Textures (LeKiwi06) - textures provisoires en place

### Blocs (stades de pousse) : `visuels-objets/png/blocs/<nom>.png`, 16 x 16, plante en croix

Une même image sert à Java (modèle `cross`) et à Bedrock (`minecraft:geometry.cross`). 17 fichiers, provisoires créés
par `python KS_Blocs/pack/blocs.py` (images du jeu) :

`culture_pousse`, `culture_jeune` (stades communs à tous les végétaux de terre), `culture_fleur` (graine de fleur en
croissance), `culture_tomate`, `culture_poivron`, `culture_aubergine`, `culture_oignon`, `culture_salade`,
`culture_mais_bas`, `culture_mais_haut`, `culture_cerisier`, `culture_cerisier_fruits`, `culture_kiwi`,
`culture_kiwi_fruits`, `riz_pousse`, `riz_jeune`, `riz_mur`.

Ensuite : `python KS_Blocs/pack/blocs.py` (pack Java `sortie/KaLium-blocs-0.2.0.zip` + `geyser-bedrock/custom_mappings/
kalium_blocs.json` + `geyser-bedrock/pack/textures/terrain_texture.json`), puis `sh geyser-bedrock/build.sh` (version
du pack à monter). Le pack Java des blocs est encore à fusionner dans celui de KS_Textures.

### Objets : `visuels-objets/png/<nom>.png`, 16 x 16, Java et Bedrock

33 icônes, règle KS_Textures `r("<nom>", "ks_cultures:objet", "<id>")` et correspondance Bedrock de geyser-bedrock pour
chacune (nom proposé = id) :

- fruits : `tomate`, `poivron`, `aubergine`, `oignon`, `salade`, `mais`, `riz`, `cerises`, `kiwi` ;
- graines : `graine_tomate`, `graine_poivron`, `graine_aubergine`, `graine_oignon`, `graine_salade`, `graine_mais` ;
- graines de fleurs : `graine_dandelion`, `graine_poppy`, `graine_blue_orchid`, `graine_allium`, `graine_azure_bluet`,
  `graine_red_tulip`, `graine_orange_tulip`, `graine_white_tulip`, `graine_pink_tulip`, `graine_oxeye_daisy`,
  `graine_cornflower`, `graine_lily_of_the_valley`, `graine_eyeblossom`, `graine_sunflower`, `graine_lilac`,
  `graine_rose_bush`, `graine_peony` ;
- `melange`.

Les fruits sont des tartes à la citrouille (ils se mangent), les graines et le mélange des livres de connaissances.
En attendant : image d'un objet du jeu (item_model : betterave, carotte, fruit de chorus, pomme de terre, algue séchée,
blé, graines de citrouille, baies sucrées, baies lumineuses ; graines de blé, de pastèque, de betterave ; cosse de
plante carnivore pour le mélange). Bedrock voit une tarte à la citrouille ou un livre de connaissances tant que la correspondance n'existe pas.

## 5. geyser-bedrock

- Blocs : correspondance régénérée par `blocs.py` (ci-dessus) ; essai fait dans un dossier à part le 02/11 (14 états
  de canne à sucre, 3 de varech, croix sans collision).
- Objets : les 33 correspondances d'objets ci-dessus (même marque que KS_Textures).
