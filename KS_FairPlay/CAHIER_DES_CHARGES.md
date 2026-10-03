# Cahier des charges - Catégorie 5 : FairPlay (KS_FairPlay) - LeKiwi06

Publié au déploiement (03/10/2026).

Serveur : **Event** uniquement (bêta ; destination future : Kixster SMP). État : **cahier validé par LeKiwi06 le 30/09/2026** (session 5).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_FairPlay :
> plugin visant a limité l'abus des fonctionnalité jugé comme déloyales :
> fair mini-maps ( journey , xaeros , etc )
> limitation de la free-cam pour pas qu'elle puisse passer a travers les blocks ou s'eloigner a plus de 50 blocks du joueur ( legacy freecam comme exemple )
> Debug ( F3 ) limités quant aux informations d'entity et de tiles entity
> limite journalière de coffre de structure ouvrable ( inclure les spawners  et le suspicious gravel + sand) les rendre invincible tant qu'ils n'ont pas été généré a l'ouverture et non lootable de façon détournés (via hopper etc )

## Point reporté de la session 1

Élixir de Fortune = Chance III « pour donner un avantage au fait de craft cette popo pour l'ouverture des coffres ».

# Faisabilité (vérifiée le 30/09/2026)

Principe : un mod client ne peut être bridé par le serveur **que s'il accepte de l'être** (il écoute un message du
serveur). Un mod qui ne communique pas avec le serveur est invisible pour lui.

| Demande | Ce que le serveur peut faire | Limite |
|---|---|---|
| Mini-cartes « fair » | **Xaero** (Minimap / World Map) : code dans un message (`§f§a§i§r§x§a§e§r§o`) ou effets `xaerominimap:no_entity_radar`, `no_cave_maps`... : coupe la **vue des grottes** (et donc la carte du Nether) et le **radar d'entités**. **JourneyMap** : plugin serveur officiel pour Paper (Hangar) : radar et cartographie des grottes désactivables | Les autres mini-cartes ne sont bridées que si elles lisent un code serveur (à vérifier une par une) |
| Freecam | **Legacy Freecam** (« Fair Play ») ne traverse déjà pas les blocs et accepte des règles du serveur (commandes côté Paper). Le Freecam « classique » n'a pas encore de règles serveur pour Paper (proposition en cours chez ses auteurs, non intégrée) | **Aucune limite de distance à 50 blocs connue** ; impossible d'**obliger** un joueur à utiliser Legacy Freecam plutôt qu'un autre mod. Parade serveur : anti-xray de Paper (les minerais et blocs choisis cachés derrière la pierre ne sont pas envoyés au client, freecam ou non) |
| F3 : entités et blocs à données | Le F3 affiche ce que le serveur envoie. Le serveur peut **ne pas envoyer** : anti-xray de Paper (blocs cachés : minerais, et au besoin coffres, spawners...), portée de suivi des entités réduite | Le F3 ne peut pas être modifié par morceaux ; l'option `reducedDebugInfo` cache aussi les coordonnées (gênant en survie) |
| Coffres de structure, spawners, gravier / sable suspects | **Entièrement faisable** côté serveur (Paper : ouverture, casse, explosions, entonnoirs, pistons, génération du butin) | - |
| Chance III sur les coffres | En vanilla, la Chance **n'agit que sur la pêche** (les tables des coffres n'ont ni `bonus_rolls` ni `quality`). Faisable en modifiant le butin à sa génération (Paper) | La règle de bonus est à définir |

Sources : Xaero's Minimap / Map Server Utils (Modrinth), JourneyMap pour Paper (Hangar), Legacy Freecam (Modrinth),
Freecam PR #633 (GitHub), AntiFreecam (Modrinth), wiki Minecraft « Luck » / « Loot table ».

# 2. Réponses de LeKiwi06

| Question | Réponse (30/09/2026) |
|---|---|
| N'autoriser que Legacy Freecam | Souhaité si possible (voir partie 4 : impossible à imposer techniquement, règle + modération) |
| Distance de la freecam | Pas de limite ; le joueur ne voit que les chunks chargés |
| « Camembert » (graphique de profilage du F3) qui révèle coffres, spawners... | Question posée (voir partie 4) |
| Chance III | À coder |
| Mini-cartes | Vue des grottes (carte du Nether comprise) et radar d'entités coupés pour tous ; carte de surface et points de repère gardés ; plugin serveur JourneyMap installé : validé |
| Freecam / F3 | Règle « Legacy Freecam seulement » + parades serveur (anti-xray de Paper en mode « cacher », portée de suivi des entités réduite) : validé |
| Ce qui compte | Contenants au butin non généré (coffres, tonneaux, wagonnets de mineshaft, distributeurs des temples), casse des spawners naturels, brossage des blocs suspects ; coffres forts (vaults) exclus : validé |
| Limite | **10 par jour**, **limite globale** pour toute la liste ; remise à zéro à minuit (Paris) |
| Limite atteinte | Refus avec message, bloc intact : validé |
| Protection | Incassable (joueurs, explosions, pistons, feu), pas d'entonnoir tant que le butin n'est pas généré : validé |
| Chance III | Permet d'**ouvrir des coffres même limite atteinte**, autant qu'on veut pendant les 10 minutes de l'élixir |
| Butin par joueur (Paper) | **Non** : détruirait la rareté (les joueurs se donneraient les coordonnées) |

# 3. Questions restantes

Aucune.

# 4. Choix d'interprétation (validés le 30/09/2026)

## Éléments

| Élément | Rôle |
|---|---|
| **KS_FairPlay** (nouveau) | Code « fair » de Xaero à la connexion ; règles de Legacy Freecam (si elle les accepte d'un serveur Paper) ; limite journalière et protection des coffres de structure, spawners naturels, blocs suspects ; effet de Chance III |
| **JourneyMap** (plugin serveur, installé) | Radar et cartographie des grottes désactivés pour tous |
| **Paper** (configuration d'Event) | Anti-xray en mode « cacher » : minerais **+ coffres, tonneaux, spawners** ; portée de suivi des entités réduite (valeurs à régler au moment du code) |
| Règlement du serveur | « Legacy Freecam seulement » (contrôlé par la modération) |

## Mini-cartes

- Xaero : vue des grottes (donc carte du Nether) et radar d'entités coupés pour tous (code envoyé à la connexion) ;
  carte de surface et points de repère gardés.
- JourneyMap : même chose par son plugin serveur.
- Autres mini-cartes : bridées seulement si elles lisent un code serveur (liste vérifiée au moment du code).

## Freecam, F3 et « camembert »

- **Impossible d'imposer Legacy Freecam** : le Freecam classique ne se signale pas au serveur, qui ne peut donc ni le
  voir ni le bloquer. Règle du serveur + modération ; si Legacy Freecam accepte des règles d'un serveur Paper,
  KS_FairPlay les envoie (pas de traversée des blocs).
- **Distance** : pas de limite ; une freecam ne voit que les chunks envoyés par le serveur (distance d'affichage).
- **« Camembert »** (graphique de profilage du F3) : le serveur ne peut pas le bloquer, il est calculé par le jeu du
  joueur. Mais il ne révèle que les blocs que le client connaît : avec l'anti-xray qui cache coffres, tonneaux et
  spawners enfermés dans la pierre, ces blocs ne sont **pas envoyés** au client et n'apparaissent donc pas dans le
  camembert. Limite : un coffre à l'air libre (village, épave visible) reste visible, comme en vanilla.

## Coffres de structure, spawners, blocs suspects

- **Comptent** (limite **globale de 10 par jour**, remise à zéro à minuit, heure de Paris) : ouvrir un contenant dont
  le butin n'est pas encore généré (coffres, tonneaux, coffres des wagonnets de mineshaft, distributeurs des
  temples) ; casser un spawner naturel (pas ceux de KS_Spawners) ; brosser du gravier ou du sable suspect. Coffres
  forts (vaults) exclus.
- **Limite atteinte** : refus avec le message « Limite atteinte, reviens demain » ; le bloc reste intact.
- **Protection** tant que le butin n'est pas généré (spawner naturel : tant qu'il n'est pas cassé par un joueur
  autorisé) : incassable par les joueurs sans autorisation du jour, les explosions, les pistons, le feu ; impossible
  à vider par un entonnoir ou un wagonnet à entonnoir. Une fois ouvert, c'est un contenant normal.
- **Chance III** (Élixir de Fortune) : tant que le joueur a l'effet **Chance de niveau III ou plus**, il ouvre sans
  limite, et ces ouvertures **ne comptent pas** dans ses 10 du jour. La potion de chance vanilla (niveau I) ne donne
  rien de plus.
- Pas de butin par joueur (refusé).

# 5. Au moment du code (03/10/2026, KS_FairPlay 1.0.0)

## Demande ajoutée par LeKiwi06

> il faut aussi inclure dans la limite de 10 les dégats fait un elder guardian , ils ont un loot rare en eux qui ne drop que si ils sont tué par un joueur , il doit etre impossible pour un joueur ayant atteint sa limite de frapper un guardian .

Choix (Claude, à valider aux tests) : tuer un **gardien ancien** compte 1 pour le tueur ; limite atteinte : impossible
de le frapper (corps à corps, flèche, trident, potion, nuage de potion, TNT allumée par le joueur). Gardiens normaux
non concernés (fermes). Chance III et créatif : libres.

## Vérifications (03/10/2026)

- **Legacy Freecam** a maintenant un **plugin serveur** pour Paper 1.20.6–26.2 (2.0.0, Modrinth `legacyfreecam`) :
  tout ce qui est « déloyal » est interdit par défaut (traverser les blocs, modifier ou interagir avec le monde, figer
  le joueur, luminosité, chunks non chargés, écoute du chat vocal). Il remplace l'envoi des règles par KS_FairPlay.
- **JourneyMap** : plugin serveur `journeymap-paper-26.2-6.0.9` (Hangar, TeamJM) ; radar et cartographie des grottes à
  couper dans sa configuration après le premier démarrage.
- **Xaero** : code `§f§a§i§r§x§a§e§r§o` dans un message à la connexion (vue des grottes, carte du Nether comprise, et
  radar d'entités coupés).
- **Paper (Event)** : anti-xray en mode 1 (« cacher ») ; overworld : liste de Paper + coffres piégés, tonneaux,
  spawners, jusqu'à la hauteur 128 ; Nether : débris antiques, minerais d'or et de quartz, roche noire dorée, coffres,
  coffres piégés, tonneaux, spawners (remplacés par netherrack, basalte, roche noire) ; End : désactivé (pas de
  minerais). Suivi des entités (`spigot.yml`) : joueurs 64, animaux 48, monstres 48, divers 32, autres 32
  (affichages 128 inchangés). `lava-obscures` : catégorie 6.

> il faut aussi avoir le compteur de coffres dans le tchat pour ceux qui perdent le fil

Fait : à chaque ouverture comptée, « Limite du jour : n / 10 » dans la barre d'action et dans le tchat.

> la limite doit juste afficher " Exploration journalière : n / 10" pour ne pas trop spoil les changements

Fait (KS_FairPlay 1.0.2) : compteur « Exploration journalière : n / 10 » ; limite atteinte : « Exploration journalière : limite atteinte, reviens demain. » (sans la liste de ce qui compte).

> il faut afficher clairement le timer de la potion sur l'écran du joueur aussi pour qu'il puisse suivre le temps qui lui reste plus facilement

Fait (KS_FairPlay 1.0.2) : barre verte en haut de l'écran « Élixir de Fortune : m:ss » tant que le joueur a Chance III ou plus (jauge sur 10 minutes, mise à jour chaque seconde).

> c'est redémarré, règle journeymap

Fait : JourneyMap réglé le 03/10/2026 à 06:13 (`/journeymap/server/6.0/journeymap.server.global.config`) : `caveMapping`, `radarEnabled`, `worldPlayerRadar`, `seeUndergroundPlayers` à NONE, radars de joueurs / villageois / animaux / monstres coupés ; surface, relief, biomes, points de repère gardés. `plugins/journeymap/journeymap-server.json` : UUID de l'équipe JourneyMap (codé en dur dans le plugin) retiré des admins, opérateurs gardés. Originaux dans `/_removed-config-2026-10-03/journeymap/`. **Actif au prochain redémarrage d'Event ; vérifier ensuite que JourneyMap n'a pas réécrit ces fichiers.**
