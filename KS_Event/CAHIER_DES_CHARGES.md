# Cahier des charges - plugins du serveur Event (survie) - Maxster33, 25/09/2026

Demande de Maxster33, reproduite telle quelle (partie 1), puis ses réponses aux questions de Claude (partie 2) et
les choix d'interprétation qui en découlent (partie 3). **Règle posée par Maxster33 : ne rien créer qui ne soit
écrit ci-dessous ; proposer avant de faire ; signaler ce qui est flou.**

# 1. Demande d'origine

> ( message pour claude : tu ne dois absolument rien créer qui n'ai été demandé dans la liste ci-dessous , le fait
> d'ajouter une chose non prévu est très dommageable , il ne faut le faire sous aucun prétextes , mais tu peux proposer
> tes idées avant de les faire ,si jamais tu créés quelques choses non écrit explicitement car j'ai donné une
> explication ou directive flou ou si tu as besoin que je te fournisse des documents que tu n'as pas , dit le moi )
>
> 0 - vocabulaire souvent utilisé : potion basique : potion de niveau 1 , non allongé en temps ( les joueurs pourront
> toujours les modifiés eux mêmes via l'alambic )
>
> **1 - mécaniques spéciales** ( je veux un plugin par mécaniques , pour les réutiliser séparément sur d'autres
> projets )
> - Prix d'enclume infinie : possibilité de réparer ou fusion sans contrainte de prix trop couteux ( prix calculé
>   selon les règles vanilla , juste sans plafond maximum )
> - NewVillagerTrade : les villageois n'ont plus de trade par défaut , aucun moyen de les lvl up etc , seuls les pnj
>   donnés a l'avenir en auront , on créera les loots table de trade dans un futur script
>
> **2 Craft custom** ( un plugin pour les craft customs classique , un pour les elixirs ) - afin de rendre certains
> items plus accessibles , certains craft ont été ajoutés :
> - 1 colorant orange , 8 sables : 8 sables rouges
> - 1 grès : 4 sables
> - 4 block de gold/iron/copper : 4 block de raw gold/iron/copper
> - 1 houe en bois , 8 tnt : 1 bedrock breaker
> - 5 diorite , 4 blocks de quartz : 9 calcite
> - 1 ghast tear , 8 obisidenne : 8 crying obsidian
> - 2 bone block , 2 deepslate : 1 reinforced deepslate
> - 2 pitcher plant : 4 graines de pitcher plant
> - 2 torchflower : 4 graines de torchflower
> - 4 coral : 1 coral block ( toutes les couleurs )
> - 4 verre , 4 glowstone block, 1 blaze rod : 4 light block niveau 15
> - tout les craft necessitant de la nether wart : remplacer la nether wart par des nether wart block ( par exemple
>   les red nether brick )
> - 8 batons , 1 membrane de phantom : 1 item frame invisible
> - 8 magma block , 1 colorant vert/orange/violet : 4 froglight ( couleur correspondante )
> - élixirs (Super Gateau, Elixir de chauve souris, Elixir d'anguille, Elixir d'ignifugation, puis « les memes types
>   délixir pour tout les effets obtenable sur le serveur ») - **reporté, voir partie 2**
>
> **3 - Changement de loots table** ( un plugin par type nécessaire : blocks , entités , peche , etc … faire un plugin
> spécial pour les potions lootables )
>
> Blocks - minerais : les minerais de fer et or droppent autant que ceux de cuivre ; les autres minerais ont 1 % de
> chance que le loot habituel soit 2 minerais du même minerais ( ne fonctionne pas avec silk touch) ; 10% de chance
> qu'un emeraude deepslate donne une potion de chance ( minerais le plus rare , récompense forte possible ).
> Feuillages : augmentation du taux de pousses par 2 sur chenes sombre , chene pale , savane ; faire un pool d'items
> pour les pommes : 1% de pomme d'or , 0,01% de pomme dorée enchantée ; le drop des pommes et leur pool spéciale
> associé est ajouté a toutes les feuilles. Chorus plant : ajouter 1% de drop potion basique de slow falling.
> Nether wart : drop habituel supprimé , elles 5% de chance drop une potion basique aléatoire ; toutes ses
> utilisations ont été remplacés par une alternative , les potions seront principalement des loots de mobs et des
> recompenses dans /rewards
>
> Entités ( le but de ces modifications est rééquilibrer le jeu : nerfer les farm afk , valoriser la découverte et le
> farm manuel ) :
> - iron golem : remplacer les lingots de fer par des pépites , réduire par 2 les fleurs , et randomiser le
>   coqueliquot par toutes les fleurs de 1 de haut ( propotion de 1% pour ces fleurs : wither rose , pitcher plant ,
>   torch flower ) ( nerf volontaire , car le minerai de fer est augmenté pour le minage manuel )
> - pigmen : reduire de moitié tout les loots , randomiser la chair putréfies avec 50% d'os et 50% de chair
> - endermite : ajouter 50% a la mort d'une endermite d'avoir un des items suivants : améthyste bourgeonnant , shulker
>   shell , potion de regeneration ( niveau basique ) , une bouteille d'xp équivalent niveau 10 , un chorus fruit , 8
>   enderpearl
> - vex : 10% potion basique d'infestation ; phantom : ajouter 10% potion basique slow falling ; sorcière : drop
>   réduit par 5 ; pillager capitaine : ne drop plus de ominious bottle, ajouter 1% potion basique random ( seulement
>   celles obtenable en survie de niveau basique ) ; strider : ajouter 1% potion fire resistance ; lapin : ajouter 1%
>   potion de jump boost ; gros slime : ajouter 10% potion infestation basique ; breeze : ajouter 10% potion basique
>   Wind charge ; allay : ajouter 10 % potions basique soin ; araignée : ajouter 1 % potion basique Weaving ; araigné
>   bleu : ajouter 1 % potion poison basique ; gardian : ajouter 10 % potion basique water breathing ; elder gardian :
>   ajouter 100% potion basique water breathing ; ghast : ajouter 10% de potion de régénération basique ; pillager :
>   ajouter 10 % de potion de faiblesse basique ; wandering trader : ajouter 100% de potion d'invisibilité basique ;
>   blaze : Ajouter 10% de potion de force basique ; cheval,ane,mule : ajouter 10% de potion de vitesse basique
> - squid,glowsquid : doubler la quantité de drop
> - wither squelette : supprimer drop de tête basique , doubler les drop de charbon , ajouter 10% de whiter rose
> - tout les mobs de types zombie : mélanger la chair putréfié avec 50 % d'os
> - warden : ajouter 10% de chance de drop un bouteille d'xp comme celles des !bottle-xp , d'un niveau aléatoires
>   entre : 10 , 20 , 30 , 40 , 50
>
> Peche : les livres enchantés sont retirés des loot table.

# 2. Réponses de Maxster33 (25/09/2026)

| Question | Réponse |
|---|---|
| Bedrock breaker | « doit pouvoir etre utilisé une seul fois pour un seul bloc » |
| Bouteille d'XP `!bottle-xp` | « regarde les fioles d'expérience deja existante dans le jeu » |
| Organisation en 8 plugins (tableau ci-dessous) | OK |
| Enclume : coût vanilla sans le « Trop cher ! », pénalité de réparations successive vanilla | oui |
| Villageois : gardent leur métier sans échange ; marchand ambulant aussi sans échange ; étiquette `ks_pnj` pour les futurs PNJ | oui, oui, oui |
| Crafts « 8 + 1 » en anneau autour de l'objet central, les autres sans forme | oui |
| Grès : variantes (taillé, poli, lisse) comprises ; grès rouge → sable rouge | oui et oui |
| Blocs → blocs bruts : cuivre | toutes les versions du cuivre |
| « 2 deepslate » | la pierre deepslate |
| Verrue : briques rouges du Nether avec bloc de verrue ; craft 9 verrues → bloc retiré ; potion étrange à l'alambic avec le bloc de verrue | oui, oui, oui (« il faut utiliser le bloc ») |
| Élixirs | « on laisse tomber le plug in elixir pour le moment on verra ça une autre fois » |
| Nouveaux drops de mobs seulement si un joueur tue le mob ; Butin (Looting) sans effet sur les pourcentages | ok |
| Liste des potions basiques aléatoires (voir partie 3) | ok |
| Or du Nether avec fer / or « comme le cuivre » | non |
| Feuilles : pousses ×2 chêne noir / pâle / acacia ; pommes sur toutes les feuilles au taux vanilla, chaque pomme 1 % dorée / 0,01 % enchantée ; aussi à la dégradation des feuilles | ok, oui et oui |
| Golem : autres fleurs (éclosions, pétales roses, fleurs sauvages) | ne pas les ajouter |
| Réductions (÷2 pigmen, ÷5 sorcière) avec arrondi au hasard | ok |
| Endermite : un des 6 objets, chances égales | ok |
| « Gros slime » | seulement les gros (taille 4) |
| Wither squelette : « tête basique » = son crâne | oui, le crâne |
| « Mobs de type zombie » | « tous les mob qui donnent normalement de la chair putréfiée en vanilla » |
| Pêche : livre enchanté remplacé | tirer au sort un autre trésor |
| Enregistrer sur GitHub | oui |

# 3. Plugins et interprétations retenues

| Plugin | Contenu |
|---|---|
| `KS_Enclume` | Enclume : coût vanilla, sans plafond |
| `KS_Villageois` | Villageois et marchand ambulant sans échange (sauf entités avec l'étiquette `ks_pnj`) |
| `KS_Crafts` | Crafts ci-dessus + bedrock breaker + verrue (briques, alambic) |
| `KS_LootBlocs` | Minerais, feuilles (pousses, pommes), verrue sans drop |
| `KS_LootEntites` | Golem, pigmen, sorcière, capitaine (fiole sinistre), calmars, wither squelette, chair putréfiée, endermite, warden, fioles d'expérience |
| `KS_LootPeche` | Livres enchantés remplacés par un autre trésor (arc, canne à pêche, étiquette, carapace de nautile, selle) |
| `KS_LootPotions` | Toutes les potions à ramasser (blocs et mobs), sauf celle du tirage de l'endermite |
| `KS_Elixirs` | Reporté |

- **Potion basique** : potion à boire, type de base non renforcé et non allongé (`PotionType` sans `LONG_` / `STRONG_`).
- **Potion basique aléatoire** : vitesse, lenteur, saut, force, soin, dégâts, poison, régénération, faiblesse,
  invisibilité, respiration aquatique, résistance au feu, vision nocturne, chute lente, maître tortue, vent, tissage,
  suintement, infestation (pas la chance).
- **Fiole d'expérience de niveau N** : fiole vanilla nommée « Fiole d'expérience (niveau N) », marquée ; lancée, elle
  donne l'XP pour passer du niveau 0 au niveau N (formule vanilla : 10 → 160 points, 20 → 550, 30 → 1 395,
  40 → 2 920, 50 → 5 345).
- **Bedrock breaker** : houe en bois nommée « Bedrock Breaker », marquée ; clic droit sur un bloc de bedrock : le bloc
  est retiré (aucun drop) et l'objet est consommé.
- **Endermite** : son tirage (6 objets dont la potion de régénération) est entièrement dans `KS_LootEntites`, pour
  garder un seul tirage.
- **Minerais « 1 % : 2 minerais »** : charbon, cuivre, lapis, redstone, diamant, émeraude (et leurs versions deepslate),
  quartz du Nether ; le drop est remplacé par 2 blocs du minerai cassé ; jamais avec Toucher de soie. Pas l'or du
  Nether ni les débris antiques (non cités).
- **Émeraude deepslate** : 10 % de potion de chance en plus du drop normal (cassée par un joueur).
- **Pillard** : 10 % de faiblesse pour tous les pillards (capitaines compris) ; le capitaine a en plus 1 % de potion
  aléatoire et ne lâche plus de fiole sinistre.
- **Mobs à chair putréfiée** : zombie, zombie noyé, zombie momifié, zombie villageois, zoglin, cheval zombie,
  zombifié (pigmen, après la réduction de moitié) : chaque chair a 50 % de chance de devenir un os.
- **Réductions et remplacements** (golem, pigmen, sorcière, calmars, wither squelette, chair) : s'appliquent à toutes
  les morts (fermes comprises) ; **ajouts** (potions, roses du wither, endermite, warden) : seulement si un joueur tue.
- **Corail** : les 5 coraux vivants (tube, cerveau, bulle, feu, corne) → bloc de la même couleur. Coraux morts non
  compris (non cités).
