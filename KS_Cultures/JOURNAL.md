# KS_Cultures - journal

Plugin de Kixster (survie). Cultures de fleurs, nouveaux végétaux, graines par biome, mélange de graines (cahier local
`Blocs_Cultures_Objets.md`, parties 10 à 14 ; prix dans `sortie/cultures/cultures-proposition.xlsx`, hors git).
Dépend de KLM_Menu (textes, menus en coffre) et de KS_Blocs (stades de pousse en blocs custom, 0.2.0 ou plus).

## 1.0.0 - cultures de Kixster (02/11/2026, LeKiwi06)

**Demande de LeKiwi06** : liste de 13 ajouts du 01/11/2026, validée le 02/11 (« Oui, valide » pour les prix et les
récoltes).

### Végétaux (`Vegetal`)

| Végétal | Où | Stades | Récolte (validée le 02/11) |
|---|---|---|---|
| Tomate, poivron, aubergine, oignon, salade | terre labourée | pousse, jeune, mûr | 1 à 3 fruits + 1 à 2 graines |
| Maïs | terre labourée | pousse, jeune, mûr sur 2 blocs (s'il y a de la place) | 1 à 2 épis + 1 à 2 graines |
| Riz | sous l'eau (source), sur terre, terre labourée, boue, argile, sable, gravier | pousse, jeune, mûr | 1 à 4 grains (le grain se replante) |
| Kiwi | terre ou terre labourée (le fruit se replante) | pousse, jeune, arbuste, arbuste avec fruits | clic droit : 1 à 2 fruits, l'arbuste repousse ses fruits |

- Semis : clic droit sur le dessus du sol avec la graine (le grain de riz, le kiwi). Les protections
  (claims) sont respectées : un essai de pose leur demande si le joueur peut construire là.
- Casse d'une culture pas mûre : sa graine revient (un arbuste rend son fruit, plus ses fruits s'il en porte). Les
  deux blocs du maïs partent ensemble, une seule récolte.
- **Récolte à la main seulement** : ni l'eau (la canne à sucre et le varech ne sont pas cassés par l'eau) ni les
  villageois fermiers (ils ne récoltent que les cultures vanilla) ; un piston ne bouge pas s'il devait en casser une.
- Pousse : chaque minute, chaque culture des chunks chargés avance d'un stade avec une chance de 1 / durée moyenne
  (`config.yml`, `pousse.minutes-par-stade` : 15 min, maïs 20 min, « un peu plus lent » d'après le tableau validé) ;
  lumière d'au moins 9 ; deux fois plus lent sur terre labourée sèche. Un champ de 2 stades se fait donc en ~30 min en
  moyenne (le blé vanilla : ~30 min dans de bonnes conditions).
- Appui : la culture casse (son butin tombe) si la terre labourée disparaît (piétinement compris : comme en vanilla) ;
  la terre labourée sèche sous une culture ne redevient pas de la terre (comme sous le blé).
- **Poudre d'os** (comportement des cultures vanilla, accepté par LeKiwi06 le 02/11) : un stade de plus ; la fleur
  en croissance fleurit. Distributeur de poudre d'os : rien sur ces cultures.
- Végétaux découverts : récolter un végétal mûr (casse ou cueillette) l'ajoute à la liste du joueur (données du
  joueur) ; message la première fois pour ceux qui ont des graines au composteur.

### Fleurs (`Fleurs`)

- Au composteur : 1 fleur -> 1 à 3 graines. Graines custom : pissenlit, coquelicot, orchidée bleue, allium, houstonie
  bleue, 4 tulipes, marguerite, bleuet, muguet, oeilchidée (ouverte ou fermée : une graine, qui donne l'oeilchidée
  fermée), tournesol, lilas, rosier, pivoine. Torchiflore et plante carnivore : leurs graines vanilla (KS_Crafts 1.24.0
  retire leurs crafts).
- Graine sur la terre labourée : stade « en croissance » (variante `fleur`), puis la vraie fleur est posée (grande
  fleur : s'il y a de la place au-dessus). Elle se récolte ensuite comme une fleur ordinaire.
- **Grandes fleurs** (tournesol, lilas, rosier, pivoine) : plus de duplication à la poudre d'os (clic droit et
  distributeur).
- Pas de graine pour la rose de wither (2,53 au barème) ni pour le pissenlit doré (0,38) : exclusion validée par
  LeKiwi06 le 02/11.

### Graines par biome

Une herbe courte ou haute cassée par un joueur (hors créatif, sans cisailles) dans le bon biome donne, une fois sur 8
(`herbes.chance`), la graine d'un végétal du biome (au hasard s'il y en a plusieurs) ; la graine de blé vanilla reste
possible en plus. **Table des biomes validée par LeKiwi06 le 02/11** (`herbes.biomes` du `config.yml`, mots cherchés
dans le nom du biome, Terralith compris) : tomate savanes ; poivron badlands, shrublands, déserts ; aubergine jungles (pas de
bambous) ; oignon taïgas (pas enneigées) ; salade forêts (ni sombres, ni enneigées) ; maïs plaines et prairies ; riz
marais et rivières ; kiwi jungles de bambous. Le butin des herbes est fait ici :
KS_LootBlocs n'est pas modifié.

### Composteur (`Composteur`)

- Clic droit main vide sur un composteur (sauf plein : sa poudre d'os se prend comme en vanilla) : menu en coffre.
- En haut, ce que le joueur porte : fleurs (clic : toutes en graines), fruits des végétaux découverts (clic : 1 fruit
  -> 1 graine ; pas découvert : refusé, avec la raison), graines (clic : stockées dans le composteur, 64 par sorte au
  plus). Graines acceptées : toutes celles de KS_Cultures (riz compris) et les graines vanilla (blé, betterave,
  pastèque, citrouille, torchiflore, plante carnivore).
- En bas : aide, graines stockées (menu pour les reprendre), **mélange de graines** (dès 9 sortes stockées : 1 graine de
  chacune des 9 sortes les plus nombreuses -> 1 mélange), fermer.
- Stockage propre à chaque composteur (données du chunk) ; il tombe au sol si le composteur est cassé, explosé ou
  brûlé ; un piston ne pousse pas un composteur qui stocke des graines.
- Mélange de graines : sur la terre labourée, un végétal au hasard parmi tomate, poivron, aubergine, oignon, salade,
  maïs, kiwi (pas le riz, qui pousse sous l'eau).

### Cerises

Choix de LeKiwi06 du 02/11 : « les cerises doivent drop des cerisiers, ce n'est pas une plante cultivable, seulement un
item équivalent aux pommes ». Pas d'arbuste cerisier : les feuilles de cerisier donnent des cerises à la place des
pommes, au taux des pommes (KS_LootBlocs 1.4.0, qui appelle `KSCultures.cerises(n)`), sans pomme dorée. Elles se
mangent comme une pomme et valent son prix au barème.

### Objets

Graines et mélange : livre de connaissances (aucun usage vanilla, son clic droit est bloqué). Fruits : tarte à la
citrouille (aliment sans autre usage, comme le casse-croûte doré), **ils se mangent** (choix de LeKiwi06 du 02/11 ;
valeurs de l'aliment vanilla au même prix : légumes et épi de maïs 3 de nourriture et 3,6 de saturation comme la
carotte ; riz, kiwi 2 et 0,4 comme les baies sucrées ; cerises 4 et 2,4 comme la pomme) ; un clic droit qui ne plante pas les mange. 64 par
pile, à l'image d'un objet du jeu (item_model provisoire), marqués `ks_cultures:objet = <id>`. Ids : fruits `tomate`, `poivron`, `aubergine`, `oignon`,
`salade`, `mais`, `riz`, `cerises`, `kiwi` ; graines `graine_tomate`, `graine_poivron`, `graine_aubergine`,
`graine_oignon`, `graine_salade`, `graine_mais` ; graines de fleurs `graine_dandelion`, `graine_poppy`,
`graine_blue_orchid`, `graine_allium`, `graine_azure_bluet`, `graine_red_tulip`, `graine_orange_tulip`,
`graine_white_tulip`, `graine_pink_tulip`, `graine_oxeye_daisy`, `graine_cornflower`, `graine_lily_of_the_valley`,
`graine_eyeblossom`, `graine_sunflower`, `graine_lilac`, `graine_rose_bush`, `graine_peony` ; `melange`. Noms dans
`lang.yml` (`objet.<id>`). Un fruit tenu en main sur un composteur se composte comme en vanilla (le menu s'ouvre main
vide).

### Limites

- Rien testé en jeu. À essayer d'abord dans Kixster Test, surtout sur Bedrock : couche d'eau autour du riz, clic droit
  sur une plante custom (cueillette), clic droit main vide sur le composteur.
- Une culture ne pousse que dans un chunk chargé (comme les cultures vanilla, à peu près).
- L'essai de pose (protections) passe par un faux `BlockPlaceEvent` : un plugin qui journalise les poses le verrait.
- Créatif : la pioche (clic molette) sur une culture donne de la canne à sucre ou du varech.

**Compilé le 02/11/2026, non déployé, non testé en jeu.** À déployer avec KS_Blocs 0.2.0, KS_Crafts 1.24.0, KS_LootBlocs 1.4.0, geyser-bedrock
(correspondance régénérée par `python KS_Blocs/pack/blocs.py`) et les ajouts aux plugins communs (`AJOUTS_COMMUNS.md`).

## 1.1.0 - récolte par la Laboureuse (20/11/2026, LeKiwi06)

Cahier local `Outils_3x3.md` (validé le 20/11) : la Laboureuse de KS_SuperTool récolte et replante aussi les cultures
de KS_Cultures (réponse de LeKiwi06 : « oui pour les 3 questions »).

- API publique : `KSCultures.cultureMure(bloc)` (légume, maïs bas ou haut, riz mûrs ; kiwi avec fruits) et
  `KSCultures.recolter(joueur, bloc)` : même essai de claims que le semis (faux événement de pose), butin de la
  culture mûre (découverte comprise), une semence reprise (graine, ou grain de riz), la culture repart au stade
  « pousse » ; le maïs perd son haut et son bas est reposé ; un kiwi est cueilli comme au clic droit (l'arbuste reste).
- Rien d'autre ne change.

Statut : compilé, non déployé, non testé. À déployer avec KS_Blocs 0.2.0, KS_Crafts 1.24.0, KS_LootBlocs 1.4.0
(la 1.0.0 n'a jamais été déployée) et KS_SuperTool 1.0.0.

