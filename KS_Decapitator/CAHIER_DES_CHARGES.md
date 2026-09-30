# Cahier des charges - KS_Decapitator - LeKiwi06

Partie du cahier des charges de la catégorie 1 « Contenu survie » (KS_Decapitator, KS_Elixir), validé par LeKiwi06
le 29/09/2026, publiée au déploiement (30/09/2026). Serveur : **Event** uniquement (bêta ; destination future : Kixster SMP).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_Décapitator :
> plugin dans le style de  all mobs head ( build notre propre version car c'est simple et plus maintenable sur le long terme )  mettre a jour les craft nécessitant des têtes custom dans KS_craft
>

## To do list du Kixster SMP (fichier daté, `SOURCE_to_do_list_Kixster_SMP.txt`, sert pour les détails)
> Décaptitator : tout les mobs droppent leurs têtes , en conservant la variété au sein d'un même mob les mobs passif droppent a 2% , le mobs hostiles à 1% ( les wither  et wither squelettes doivent avoir aussi des têtes custom selon le meme ratio que les autes , mais pas l'ender dragon , si tu n'arrives pas a trouver les fichiers de têtes par toi meme , demande les nous on les trouvera )

# 2. Réponses de LeKiwi06

| Question | Réponse (29/09/2026) |
|---|---|
| Mobs neutres | Loup, abeille, golem de fer = passifs (2 %) ; tous les autres = hostiles (1 %) |
| Chute seulement si tué par un joueur, Butin sans effet, pas de tête pour les mobs de spawner | ok |
| Variantes (une tête par variante visible ; poisson tropical : 1 tête ; villageois / zombie villageois : 1 par métier ; shulker : 17 couleurs) | ok |
| Bébés | **Tête différente** : les bébés ont maintenant leurs propres textures, il les faut |
| Têtes vanilla | Le creeper chargé donne la tête vanilla ; Décapitator donne une tête **custom** même pour les mobs qui ont une tête vanilla |
| Tête du wither squelette | Décorative, ne sert pas à invoquer le Wither (suit le point précédent) |
| Pose | Posables, gardent nom et identité une fois cassées |
| Noms | « Tête de mouton rouge », « Tête de villageois forgeron »..., blanc, non italique |
| Textures | Claude les cherche (minecraft-heads.com) ; validation en se les donnant en jeu |
| KS_Crafts / KS_ItemSimple | Les 5 recettes prennent les têtes de Décapitator ; aucune tête « Steve » en circulation (joueurs pas au courant) ; les retirer de KS_ItemSimple et KS_KaliumGive (obsolètes) |
| Accents | Corriger les accents des noms |
| KS_KaliumGive (élixirs et têtes) | Oui |
| Bedrock (Geyser `custom-skulls`) et coordination avec Maxster33 | D'accord |
| Taux (remplace la ligne « Mobs neutres ») | **Tous les mobs à 1 %**, pour faire simple |
| Nom des têtes de bébé | « Tête de bébé mouton rouge »... validé |
| Creeper chargé / crâne de wither squelette toujours retiré par KS_LootEntites | Laisser ainsi pour le moment |
| Déblocage au livre de recettes | Dès l'obtention d'un des ingrédients, validé |
| Limite de 2 plugins (13 h - 23 h) | **Accord verbal de Maxster33** (29/09/2026) pour dépasser la limite sur ce chantier |
| Mobs des trial spawners | Tous les mobs peuvent lâcher leur tête ; seuls les spawners déjà établis (zombie, squelette, creeper, araignée, blaze, mouton, vache, poule) ont une recette : aucun spawner ajouté |
| Spawners zombie / squelette / creeper | Accepter aussi la tête custom |
| Bébés et variantes dans les recettes de spawners | Acceptés |

Décisions de LeKiwi06 au moment du code (30/09/2026) :

| Question | Réponse |
|---|---|
| Source des textures (minecraft-heads.com bloque les robots) | MoreMobHeads (licence MIT) ; les textures manquantes (shulkers colorés, cube de soufre, bébé noyé) seront fournies |
| États temporaires (en colère, pollinisée, frigorifié, hurleuse, en charge, chargé) | Une tête par état |
| Menu pour valider les textures | Oui : `/tetes` pour les opérateurs |

# 3. Choix d'interprétation (validés le 29/09/2026)

## KS_Decapitator (nouveau)

- Tout mob tué par un joueur a **1 %** de chance de lâcher sa tête (passifs, neutres, hostiles, boss). Butin sans
  effet. Pas de tête pour les mobs nés d'un spawner classique (vanilla ou KS_Spawners, réponse du point 2) ; les mobs
  des trial spawners en donnent. Aucun joueur, pas d'Ender Dragon.
- Mobs couverts : ceux qui apparaissent en survie ; le Wither et le wither squelette compris.
- **Une tête par variante visible**, et une tête distincte pour chaque **bébé** (« Tête de bébé ... »). Exceptions :
  poisson tropical = 1 tête ; villageois et zombie villageois = 1 tête par métier ; shulker = 17 couleurs.
- Tête **custom** pour tous, même ceux qui ont une tête vanilla (zombie, squelette, creeper, piglin, wither
  squelette). Le creeper chargé garde son comportement vanilla (tête vanilla), sauf le crâne de wither squelette,
  toujours retiré par KS_LootEntites : le Wither reste impossible à invoquer. Les têtes custom sont décoratives.
- Nom « Tête de <mob> [variante] », blanc, non italique. Posables ; cassées, elles redonnent exactement le même objet
  (nom, texture, marqueur).
- Textures : têtes de joueur à texture, trouvées par Claude sur minecraft-heads.com ; liste des mobs couverts donnée à
  LeKiwi06 ; validation en jeu en se les donnant.
- Nécessaire techniquement (signalé) : fonctions pour les autres plugins (créer une tête, reconnaître une tête) ;
  réglage `custom-skulls` de Geyser si les têtes s'affichent en Steve sur Bedrock.

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
