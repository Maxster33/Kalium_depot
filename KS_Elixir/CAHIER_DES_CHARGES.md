# Cahier des charges - KS_Elixir - LeKiwi06

Partie du cahier des charges de la catégorie 1 « Contenu survie » (KS_Decapitator, KS_Elixir), validé par LeKiwi06
le 29/09/2026, publiée au déploiement (30/09/2026). Serveur : **Event** uniquement (bêta ; destination future : Kixster SMP).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_Elixir :
> craft des divers elixir custom cité dans la to do list de kixster

## To do list du Kixster SMP (fichier daté, `SOURCE_to_do_list_Kixster_SMP.txt`, sert pour les détails)

> ( un plugin pour les craft customs classique , un pour les elixirs )
>
>  8 gateaux , 1 potion de soin : 1 elixir de super gateau ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:saturation",amplifier:0,duration:36000,show_particles:false}],custom_color:16738740},minecraft:custom_name={text:"Super Gateau",color:"#FF69B4",italic:false}]  )
>  8 potions de night vision 8 minutes , 1 éclat d'écho : 1 Elixir de Chauve souris ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:night_vision",amplifier:0,duration:72000,show_particles:false}],custom_color:16738740},minecraft:custom_name={text:"Elixir de chauve souris",color:"dark_blue",italic:false}] )
>  8 potions de water breathing 8 minutes , 1 nautilus shell : 1 elixir d'anguille ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:water_breathing",amplifier:0,duration:72000,show_particles:false}],custom_color:16738740},minecraft:custom_name={text:"Elixir d'anguille",color:"#87CEEB",italic:false}]  )
>  8 potion de résistance au feu 8 minutes , 1 blue ice : 1 elixir d'ignifugation (  /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:fire_resistance",amplifier:0,duration:72000,show_particles:false}],custom_color:16738740},minecraft:custom_name={text:"Elixir d'ignifugation",color:"gold",italic:false}]  )
> ( faire les memes types délixir pour tout les effets obtenable sur le serveur )
> -> PAS d'élixir sur les effets négatifs ni sur le soin (effet instantané, une durée allongée n'a pas de sens dessus).
> -> PAS d'élixir de niveau 2, SAUF le saut amélioré (niveau 2 uniquement, sinon inutile), qui se craft quand meme avec des potions allongées comme les autres.
> -> chaque potion ingrédient utilisée est la version longue (8 minutes), niveau 1.
> -> ces élixirs servent juste à condenser plusieurs bouteilles en une seule, pour ne pas avoir à boire sans arrêt ; la contrainte de place perdue est compensée par un item rare/pénible à obtenir dans chaque recette. Les particules sont masquées sur toutes ces potions custom.
>
>  8 potions de slow falling 8 minutes , 1 shulker shell : 1 Elixir de Plume ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:slow_falling",amplifier:0,duration:72000,show_particles:false}],custom_color:11691775},minecraft:custom_name={text:"Elixir de Plume",color:"light_purple",italic:false}] )
>  8 potions de regeneration 8 minutes , 1 respawn anchor : 1 Elixir de Renaissance ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:regeneration",amplifier:0,duration:72000,show_particles:false}],custom_color:16731501},minecraft:custom_name={text:"Elixir de Renaissance",color:"red",italic:false}] )
>  8 potions d'invisibilité 8 minutes , 1 bouteille d'xp niveau 10 : 1 Elixir du Fantôme ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:invisibility",amplifier:0,duration:72000,show_particles:false}],custom_color:11119017},minecraft:custom_name={text:"Elixir du Fantôme",color:"gray",italic:false}] )
>  8 potions de force 8 minutes , 1 block d'or : 1 Elixir de Titan ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:strength",amplifier:0,duration:72000,show_particles:false}],custom_color:16766720},minecraft:custom_name={text:"Elixir de Titan",color:"gold",italic:false}] )
>  8 potions de vitesse 8 minutes , 1 harnais de happy ghast (peu importe la couleur) : 1 Elixir du Vent ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:speed",amplifier:0,duration:72000,show_particles:false}],custom_color:8379391},minecraft:custom_name={text:"Elixir du Vent",color:"aqua",italic:false}] )
>  8 potions de saut amélioré 8 minutes , 1 slime block : 1 Elixir de Rebond ( niveau 2, seule exception ) ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:jump_boost",amplifier:1,duration:72000,show_particles:false}],custom_color:8190976},minecraft:custom_name={text:"Elixir de Rebond",color:"green",italic:false}] )
>  8 potions de chance 8 minutes , 1 block d'émeraude compressé ( item NBT custom comme les bouteilles d'xp, obtenu auprès d'un pnj custom, coûte 100 blocs d'émeraude classiques ) : 1 Elixir de Fortune ( /give @s minecraft:potion[minecraft:potion_contents={custom_effects:[{id:"minecraft:luck",amplifier:0,duration:72000,show_particles:false}],custom_color:5294200},minecraft:custom_name={text:"Elixir de Fortune",color:"dark_green",italic:false}] )

# 2. Réponses de LeKiwi06

| Question | Réponse (29/09/2026) |
|---|---|
| Bloc d'émeraude compressé | Donné par **KS_Economy** quand on repasse du score en objet, prix **900 émeraudes** du score (catégorie 2) |
| Potion de chance 8 min (n'existe pas en vanilla) | Accepter les potions de chance **5 minutes** ; Élixir de Fortune = **Chance III** pendant **10 minutes** (avantage pour l'ouverture des coffres) |
| Chance III sans effet réel sur la plupart des coffres vanilla | Reporté en session 5 (KS_FairPlay) |
| Forme des recettes d'élixirs | 8 autour de l'ingrédient rare (anneau), Super Gâteau compris |
| Potions acceptées | Toutes les versions allongées quelle que soit leur durée ; **adapter la durée de l'élixir** en conséquence |
| Fiole de l'Élixir du Fantôme | Un seul système de fiole : celui de Maxster33 (KS_FioleExp) ; le mettre dans le tirage de l'endermite |
| Accents | Corriger les accents des noms |
| Liste des élixirs | Les 11 ; pas de Maître tortue (trop fort en élixir) ; Vent, Tissage, Suintement, Infestation = effets négatifs |
| Livre de recettes des élixirs | Oui |
| KS_KaliumGive (élixirs et têtes) | Oui |
| Durées adaptées | Élixir de Renaissance **renommé Élixir de Phénix**, durée **10 minutes** (timers ronds) ; Plume : 30 min |
| Potion de soin du Super Gâteau | Niveau I et II acceptés |
| Warden | Passe à la fiole de KS_FioleExp ; anciennes fioles encore utilisables mais plus créées ; Élixir du Fantôme : fiole KS_FioleExp de 10 niveaux seulement |
| Déblocage au livre de recettes | Dès l'obtention d'un des ingrédients, validé |
| Limite de 2 plugins (13 h - 23 h) | **Accord verbal de Maxster33** (29/09/2026) pour dépasser la limite sur ce chantier |
| Couleur des potions | Garder la couleur de base quand c'est possible ; bouteille à **aspect enchanté** pour les distinguer des potions classiques |
| « Élixir de Phénix » | Oui |
| Potions ingrédients | Seulement à boire |

Décision de LeKiwi06 au moment du code (30/09/2026) :

| Question | Réponse |
|---|---|
| Élixir + poudre à canon à l'alambic (élixir jetable / persistant) | Bloqué : un élixir ne peut pas être mis dans un alambic |

# 3. Choix d'interprétation (validés le 29/09/2026)

## KS_Elixir (nouveau)

Recettes en anneau : 8 potions autour de l'ingrédient rare au centre. Potions ingrédients : **à boire**, version **allongée**,
niveau I, quelle que soit sa durée (chance : la potion vanilla de 5 min ; soin : niveau I ou II). Élixir : potion à
boire, effet indiqué, **particules masquées**, **aspect enchanté**. Couleur : celle de la potion vanilla de
l'effet ; Super Gâteau (pas de potion vanilla de saturation) : son rose `16738740`. Recette débloquée au livre de recettes dès que le joueur obtient un de
ses ingrédients.

| Élixir | 8 x | Centre | Effet | Durée | Nom (couleur du nom) |
|---|---|---|---|---|---|
| Super Gâteau | gâteau | potion de soin (I ou II) | Saturation I | 30 min | Super Gâteau (`#FF69B4`) |
| Chauve-souris | vision nocturne allongée | éclat d'écho | Vision nocturne | 60 min | Élixir de chauve-souris (`dark_blue`) |
| Anguille | respiration aquatique allongée | carapace de nautile | Respiration aquatique | 60 min | Élixir d'anguille (`#87CEEB`) |
| Ignifugation | résistance au feu allongée | glace bleue | Résistance au feu | 60 min | Élixir d'ignifugation (`gold`) |
| Plume | chute lente allongée | carapace de shulker | Chute lente | 30 min | Élixir de Plume (`light_purple`) |
| Phénix | régénération allongée | ancre de réapparition | Régénération I | 10 min | Élixir de Phénix (`red`) |
| Fantôme | invisibilité allongée | fiole KS_FioleExp de 10 niveaux | Invisibilité | 60 min | Élixir du Fantôme (`gray`) |
| Titan | force allongée | bloc d'or | Force I | 60 min | Élixir de Titan (`gold`) |
| Vent | vitesse allongée | harnais de happy ghast (toute couleur) | Vitesse I | 60 min | Élixir du Vent (`aqua`) |
| Rebond | saut amélioré allongé | bloc de slime | Saut amélioré **II** | 60 min | Élixir de Rebond (`green`) |
| Fortune | chance (5 min) | bloc d'émeraude compressé **tier 3** | Chance **III** | 10 min | Élixir de Fortune (`dark_green`) |

- Élixir de Fortune : **modifié en session 2 (29/09/2026)** : bloc compressé **tier 3** (9 000 points, raccord avec
  Chance III), et non plus le bloc à 900 ; pas d'autre Élixir de Fortune. Le bloc vient de KS_Economy (catégorie 2, pas encore créé) ; tant que
  KS_Economy n'existe pas, cette recette est ignorée (avertissement dans la console).

## Plugins existants modifiés

| Plugin | Changement |
|---|---|
| KS_Crafts | Recettes des 8 spawners existants : têtes de KS_Decapitator acceptées (bébés et toutes variantes compris) ; zombie, squelette, creeper : tête vanilla **ou** custom. Aucun spawner ajouté |
| KS_ItemSimple | Retrait des 5 têtes « Steve » (et de `creerTete`) |
| KS_KaliumGive | Ajout des 11 élixirs et des têtes ; retrait des têtes « Steve » |
| KS_LootEntites | Warden : fiole de KS_FioleExp (plus de création d'ancienne fiole ; les anciennes restent utilisables) |
| geyser-bedrock | Si nécessaire, affichage des têtes sur Bedrock |

À déployer ensemble (règle 3.5) : KS_Decapitator, KS_Crafts, KS_ItemSimple, KS_KaliumGive (dépendances croisées).
Réservation au moment du code : accord verbal de Maxster33 pour dépasser la limite de 2 plugins (à noter dans la
réservation).
