# KS_Decapitator - journal

Plugin autonome, serveur Event. Têtes de mobs. Cahier des charges : catégorie 1 « Contenu survie » de LeKiwi06
(validé le 29/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - têtes de mobs (30/09/2026)

Demande de LeKiwi06 : un plugin dans le style d'All Mob Heads, écrit par nous (plus simple à maintenir), qui donne
les têtes de tous les mobs ; mettre à jour les crafts de KS_Crafts qui ont besoin de têtes.

- **Chute** : 1 % (tous les mobs, réponse de LeKiwi06), seulement si un joueur tue le mob ; Butin sans effet. Aucune
  tête pour un mob né d'un spawner classique (vanilla ou KS_Spawners : raison d'apparition `SPAWNER`) ; les mobs des
  trial spawners en donnent. Ni joueur, ni Ender Dragon, ni géant, ni illusionniste, ni lapin tueur (absents de la table).
- **332 têtes** (table `src/main/resources/tetes.txt`) : une par variante visible (couleur, race, biome...), une par
  **état** (choix de LeKiwi06, 30/09/2026 : loup en colère, abeille pollinisée et / ou en colère, arpenteur frigorifié,
  chèvre hurleuse, vex en charge, creeper chargé) et une pour chaque **bébé** (« Tête de bébé ... »). Exceptions du
  cahier : poisson tropical = 1 tête ; villageois et zombie-villageois = 1 par métier (+ 1 bébé, qui n'a pas de métier) ;
  lapin « Toast » et mouton « jeb_ » (étiquette) ont leur tête. Le creeper chargé garde aussi son effet vanilla.
- **Tête custom pour tous** (zombie, squelette, creeper, piglin, wither squelette compris) ; têtes décoratives (le
  crâne custom de wither squelette n'invoque pas le Wither).
- **Objet** : tête de joueur à texture, nom « Tête de ... » blanc non italique, marqueur `ks_decapitator:tete` = id
  (ex. `bebe_mouton_rouge`, tiré du nom). Profil de la tête : UUID fixe propre à chaque id (les têtes identiques
  s'empilent).
- **Pose** : posables. Cassée (outil, explosion, piston, eau...), le jeu redonne une tête avec nom et texture mais sans
  le marqueur : toute tête du plugin qui apparaît au sol (`ItemSpawnEvent`) est reconstruite à l'identique, reconnue à
  l'UUID de son profil.
- **`/tetes`** (opérateurs, `ks.decapitator.tetes` ; ajouté à la demande de LeKiwi06 pour valider les textures) : menu
  de toutes les têtes, 45 par page ; clic = une tête dans l'inventaire.
- **Textures** : minecraft-heads.com bloque les robots (vérification anti-bot) ; accord de LeKiwi06 (30/09/2026) pour
  prendre celles de **MoreMobHeads** de JoelGodOfwar (licence MIT, mis à jour le 23/09/2026, textures issues de
  minecraft-heads.com). La table ne garde que l'empreinte de la texture (`textures.minecraft.net/texture/<empreinte>`).
  Pas repris : AllMobHeads (MLDEG), licence « tous droits réservés ».
- **Clé technique** calculée sur le mob tué : `<créature>[.<variante>][.<état>...][.baby]`, noms du jeu (ex.
  `sheep.red.baby`, `wolf.ashen.angry`, `villager.librarian`). Une clé absente de la table ne donne rien.
- **Autres plugins** : `creerTete(id)`, `idTete(objet)`, `ids()`, `tetesDe(créature)` (KS_Crafts 1.7.0,
  KS_KaliumGive 1.6.0).

Limites / à fournir par LeKiwi06 ou Maxster33 (valeur « Value » ou empreinte sur minecraft-heads.com, une ligne à
ajouter dans `tetes.txt`) :
- **Shulker** : seule la tête du shulker sans couleur existe ; les 16 shulkers colorés ne donnent rien pour l'instant
  (clés `shulker.white` ... `shulker.black` ; ajoutés en 1.3.0).
- **Cube de soufre** (`sulfur_cube`, bébé `sulfur_cube.baby`) et **bébé noyé** (`drowned.baby`) : pas de texture,
  rien pour l'instant (ajoutés en 1.3.0).
- Cheval : une tête par robe (7), pas par marquage ; villageois : textures de la plaine pour tous les biomes.
- Les bébés viennent de MoreMobHeads (ajoutés dans sa version 1.0.45) : vérifier en jeu qu'ils correspondent bien aux
  nouvelles textures des bébés.
- Bedrock : les têtes posées s'affichent avec leur texture ; en inventaire, Geyser montre peut-être une tête de Steve
  (réglage `custom-skulls` de Geyser, à voir après le test).

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (nouveau), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.1.0 - tête de wither squelette sale, endommagée, désactivée (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : la tête de wither squelette a la description « Sale », « Endommagée », « Désactivée » (et
« À réparer à l'établi pour invoquer le Wither ») ; chaque défaut se répare par un craft de KS_Crafts 1.8.0, dans
n'importe quel ordre ; réparée des 3, elle devient un **vrai crâne de wither squelette** (qui invoque le Wither).
- Abîmée, elle se pose comme décoration (sans invoquer) ; ses défauts restants sont gardés dans le bloc et rendus
  quand elle est cassée (aussi par une explosion ou un piston). Sans information, elle redevient abîmée de ses 3
  défauts (jamais réparée par erreur).
- API : `estTeteWitherSquelette`, `defautsTete`, `reparer(tête, défaut)`.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.0 ; ancienne version dans `_removed-ks_decapitator-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.0 - /tetes pour tous : têtes déjà obtenues (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai aussi que les joueurs aient accès au /tetes mais que celui ci leur servent a savoir
celle qu'ils ont déjà obtenus ( l'avoir eu dans son inventaire via un échange par exemple compte aussi , pas la peine
de l'avoir tué soit même ) ».

- **`/tetes` ouvert à tous les joueurs** (plus de permission sur la commande). Même menu, 45 têtes par page ; le titre
  donne le compte du joueur (« Têtes 12/332 - page 1/8 »).
  - Tête déjà obtenue : la tête elle-même, avec « Obtenue » en dernière ligne de sa description.
  - Tête jamais obtenue : une vitre grise à son nom, avec « Pas encore obtenue ».
  - Un clic ne donne rien à un joueur.
- **Opérateurs** (`ks.decapitator.tetes`, comme avant) : toutes les têtes sont montrées (avec leur état) et un clic en
  met une dans l'inventaire.
- **Une tête est « obtenue » dès qu'elle a été dans l'inventaire du joueur**, quelle que soit sa provenance (mob tué,
  échange, coffre, don, récompense) : notée quand il la ramasse au sol, et par un contrôle de son inventaire (armure et
  main secondaire comprises) toutes les 5 secondes, à la connexion, à la déconnexion et à l'ouverture de `/tetes`.
  Une tête obtenue le reste, même donnée, posée ou perdue ensuite.
- Données : `plugins/KS_Decapitator/collection.yml` (créé à la première tête notée), enregistré dans les 5 secondes
  qui suivent un changement et à l'arrêt.
- Textes écrits dans le code, comme le reste du plugin (pas de `lang.yml`).

Choix de Claude, signalés :
- une tête jamais obtenue montre son nom (la liste des 332 têtes est donc visible de tous), pas sa texture ;
- pas de message quand une nouvelle tête est notée ; pas de bouton dans le menu du serveur : `/tetes` seulement.

Limites :
- Rien n'est rattrapé pour le passé : une tête rangée dans un coffre ou posée avant cette version ne compte que
  lorsqu'elle repasse par l'inventaire du joueur. Celles qu'il porte sur lui sont notées à sa première connexion.
- Une tête prise puis reposée en moins de 5 secondes sans passer par le sol (d'un coffre à un autre, sans fermer)
  peut échapper au contrôle.
- Le coffre de l'Ender et les boîtes de shulker ne sont pas regardés.
- Bedrock : dans un inventaire, Geyser montre peut-être une tête de Steve (limite déjà notée en 1.0.0) ; le nom et
  « Obtenue » restent lisibles, et une tête manquante est une vitre grise sur les deux éditions.
- Le crâne de wither squelette entièrement réparé est un objet du jeu : il ne compte pas (la tête abîmée, oui).
- Empilé sur la 1.1.0, jamais testée en jeu, à la demande de LeKiwi06.

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; 1.1.0 dans `_removed-ks_decapitator-1.1.0/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.1.0). Statut : non testé en jeu.**

## 1.3.0 - têtes manquantes, shulkers teints au colorant, têtes de joueurs, barrières dans /tetes (16/10/2026, LeKiwi06)

Demandes de LeKiwi06 : « il manque la tete de sulfur cube dans le plugin de tete », puis « est-ce qu'il manque
d'autres tetes ? » et « met des bloc barrier a la place des vitres grises dans le menu /tetes , c'est trop peux
visible comme c'est là », enfin « comment obtient ton les tete de shulker colorés ? ».

- **19 têtes ajoutées** dans `tetes.txt` (**351** au total), toutes celles qui restaient à fournir depuis la 1.0.0 :
  - « Tête de cube de soufre » (`sulfur_cube`) et « Tête de bébé cube de soufre » (`sulfur_cube.baby`) ;
  - les **16 shulkers colorés** (`shulker.white` ... `shulker.black`, « Tête de shulker rouge »..., mêmes noms de
    couleur que les moutons) ; le shulker sans couleur garde sa tête ;
  - « Tête de bébé noyé » (`drowned.baby`).
  Même chute que les autres (1 %) ; visibles dans `/tetes` ; KS_KaliumGive et KS_Crafts les voient par `ids()` sans
  changement de leur côté. Aucun changement du calcul des clés (elles étaient déjà calculées, sans tête associée).
- **Textures** : MoreMobHeads ne les a toujours pas (dernière version du 23/09/2026). Prises sur minecraft-heads.com
  par son API publique (le site lui-même bloque les robots). Choix de Claude, à valider en jeu :
  - cube de soufre : parmi les 3 têtes « Sulfur Cube », la plus proche de la texture du jeu
    (`entity/sulfur_cube/sulfur_cube_outer.png` du client 26.3 : coque jaune pâle à cercles, yeux bruns, petite
    bouche) ;
  - shulkers : le site a deux styles par couleur ; pris celui à spirale sur le couvercle (proche du jeu), qui existe
    pour 15 couleurs ; pour le blanc, seule la version unie existe ;
  - bébé noyé : la tête « Baby Drowned » (série des nouvelles textures de bébés).
  Source notée dans `LICENCE_TEXTURES.txt`.
- **`/tetes`** : une tête jamais obtenue est un **bloc de barrière** à son nom (au lieu d'une vitre grise), avec
  toujours « Pas encore obtenue ».
- **Teinture des shulkers** (choix de LeKiwi06 : « Teinture au colorant », entre retirer ces têtes, les rendre
  obtenables ou les garder) : un clic droit avec un colorant sur un shulker le teint, comme sur Bedrock. Raison : sur
  Java, un shulker n'a de couleur que par commande, ou en se dupliquant depuis un shulker déjà coloré (vérifié dans le
  code du jeu 26.3 : aucune interaction de teinture) ; les shulkers des cités de l'End n'en ont pas, et les 16 têtes
  colorées étaient donc impossibles à obtenir. Un colorant consommé (pas en créatif), son du colorant ; rien si le
  shulker a déjà cette couleur ; un clic annulé par un autre plugin (claim) ne teint pas. Un shulker teint se duplique
  ensuite dans sa couleur (règle du jeu). Classe `TeintureShulker`.
- **Têtes de joueurs** (Kixster SMP seulement ; demande de LeKiwi06 : « chaque joueur va avoir un mob du jeu choisis
  au hasard , si un joueur renomme ce mob avec le nom du joueur , il drop sa tête , ça ne fonctionne qu'avec les
  pseudo sur serveur kixster smp , et personne ne doit savoir qui a quel mob ») :
  - chaque joueur reçoit en secret un **type de mob** tiré au hasard parmi les créatures de `tetes.txt` (n'importe
    quel mouton compte, quelle que soit sa couleur ou son âge) ;
  - un mob de ce type, **renommé avec son pseudo** (majuscules indifférentes) puis **tué par un joueur**, lâche la
    **tête de ce joueur** (tête de joueur du jeu, avec son skin), **sans limite** (réponses de LeKiwi06 du 16/10) ;
  - seuls comptent les pseudos de joueurs déjà venus sur le serveur ;
  - tirage à la connexion (ou au premier mob renommé pour un joueur pas revenu depuis) ; vrai hasard, pas de formule
    (le dépôt est public) ; aucune commande, aucun message, rien dans le journal du serveur ;
  - skin noté à chaque connexion, pour que la tête ait la bonne texture quand le joueur est hors ligne ;
  - réglage `tetes-de-joueurs` dans le nouveau `config.yml` (`false` par défaut) : **à mettre à `true` sur Kixster**
    au déploiement ; Event reste à `false` (sauf pour un test). Données : `plugins/KS_Decapitator/tetes_de_joueurs.yml`.
  Choix de Claude, signalés : il faut que le mob soit tué par un joueur (comme les têtes de mobs ; une mort dans la
  lave ne donne rien) ; les mobs de spawner comptent ; tous les types sont tirables (Wither, warden et grand gardien
  compris) ; un joueur peut aussi trouver sa propre tête. Limites : qui a accès aux fichiers du serveur peut lire le
  tirage ; un joueur qui change de pseudo garde son mob, mais seul son nouveau pseudo marche ; joueur Bedrock : son
  pseudo commence par le préfixe de Floodgate, il faut l'écrire tel quel.
- **Pages « Joueurs » dans `/tetes`** (demande de LeKiwi06 : « ajoute une page supplémentaire au menu /tetes quand tu
  débloque une première tete de joueurs avec nether star pour les tetes manquante , il faut l'update a chaque nouveau
  joueurs sur le serveur ») :
  - dès sa **première tête de joueur obtenue**, un joueur a des pages de plus, après celles des mobs : **tous les
    joueurs déjà venus sur le serveur**, par ordre alphabétique, 45 par page, titre « Joueurs 3/120 - page 9/10 » ;
  - la liste est relue à chaque ouverture : un **nouveau joueur** y apparaît dès sa première connexion ;
  - tête obtenue : la tête du joueur (son skin) avec « Obtenue » ; manquante : une **étoile du Nether** « Tête de
    <pseudo> » avec « Pas encore obtenue » ;
  - « obtenue » comme pour les têtes de mobs : dès qu'elle passe dans l'inventaire (collection.yml, id
    `joueur:<uuid>`) ; reconnue au profil de la tête, donc toute tête de ce joueur compte, d'où qu'elle vienne ;
  - opérateurs : ces pages toujours visibles, clic = la tête du joueur dans l'inventaire ;
  - seulement là où les têtes de joueurs sont actives (`tetes-de-joueurs: true`, Kixster).
  Limite : un joueur se voit lui-même dans la liste (sa tête est obtenable comme les autres).

Vérification faite (16/10/2026) : tous les mobs vivants de l'API 26.2 et du client 26.3 (tables de butin
`entities/`) ont leur tête, sauf ceux exclus par le cahier (joueur, Ender Dragon, géant, illusionniste) et les
entités qui ne sont pas des mobs (support d'armure, mannequin). Toutes les variantes du jeu 26.3 (chat, loup, vache,
cochon, poule, grenouille, nautile zombie) sont couvertes.
Sans tête, le jeu ayant pourtant une texture à part (liste faite le 16/10/2026, réponse de LeKiwi06 : « Rien de
plus ») : loup apprivoisé (9 + bébés), golem de fer fissuré (3), ghast qui tire, renard endormi, villageois et
zombie-villageois par biome ; cheval par marquage et poisson tropical par motif (exclus par le cahier).

Limites :
- **Bébé cube de soufre** : le site n'a pas de tête de bébé (le jeu a pourtant une texture `_small` à part) ; il
  reprend la texture de l'adulte, avec son propre nom (choix de Claude, signalé).
- Pas de tête de bébé (le jeu n'en fait pas naître, l'API les accepte pourtant) : chameau momifié, perroquet, piglin
  brute, cheval-squelette, cheval-zombie, marchand ambulant, nautile zombie ; grenouille (son petit est le têtard).
  Un bébé forcé par commande ne donne rien.
- Bedrock : une barrière en inventaire s'affiche bien sur Bedrock (objet du jeu).
- Empilé sur la 1.2.0 (Kixster et Event : Event était en fait déjà en 1.2.0, envoyée le 11/10 à 10:47).

**Déployé sur Event le 16/10/2026 à 23:20 (LeKiwi06, tests ; 1.2.0 dans `_removed-ks_decapitator-1.2.0/`, jar en place vérifié identique à la référence avant l'envoi) avec un `config.yml` à `tetes-de-joueurs: true` le temps du test (à remettre à `false` sur Event ensuite). En attente du redémarrage d'Event. Statut : non testé en jeu. Kixster reste en 1.2.0.**

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage.** `tetes-de-joueurs: true` à mettre dans le `config.yml` créé au démarrage.
