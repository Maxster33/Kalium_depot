# JOURNAL — KG_BingoGame (anciennement KalBingo)

Le plugin s'appelait **KalBingo** jusqu'à la 0.1.23 (renommé en 0.2.0, voir la dernière section). Les sections
ci-dessous gardent l'ancien nom : c'est de l'historique.

Mini-jeu Bingo Minecraft pour Kal Games : plugin **séparé** de KalGames, tournant sur son propre
serveur Paper dédié (Kixster pour les tests actuellement, facilement modifiable). Décision confirmée
par l'utilisateur : un monde Overworld généré à la volée par seed n'est pas compatible avec le système
de mondes-void/arènes-collées de KalGames, d'où un serveur séparé plutôt qu'une fusion.

Cahier des charges intégral : `to_do_list_Kal_Games_Bingo.txt` (fourni par l'utilisateur, source de
vérité pour toutes les règles de jeu). Instruction explicite de l'utilisateur, à respecter en
permanence : **ne jamais construire quelque chose de non demandé** ; proposer les idées techniques
avant de les implémenter ; signaler toute ambiguïté ou tout document manquant plutôt que de deviner.

## État au 22/09/2026 (historique - l'état actuel est dans `REPRISE_PROJET.md` et dans les dernières sections ci-dessous)

Étapes 1 à 3 du cahier des charges (section 16, ordre de priorité) :
1. **Architecture parties/instances** — fait, par équipe (pas par joueur, voir plus bas).
2. **Création/réinitialisation des mondes à seed partagée** — fait.
3. **Gestion des joueurs** (salle d'attente, choix d'équipe, réception de l'affectation réseau) — fait.

Pas encore fait : grille (étape 4), validation des objectifs (5), interface/carte custom (6),
chronomètre/compte à rebours automatique (7), détection de ligne (8), fin de partie/classement (9),
administration complète (10), intégration récompenses (11).

Déployé sur Kixster (7003.mystrator.com) le 22/09/2026, première installation (pas de version
antérieure à déplacer). **Redémarrage du serveur requis pour que le plugin s'active.**

## Décisions confirmées par l'utilisateur (à ne pas reconsidérer sans nouvelle demande)

- Serveur séparé de KalGames (pas de fusion dans le même codebase/serveur).
- Monde de chaque instance : Overworld généré à la volée par seed (`WorldCreator`), pas un monde
  modèle/copié.
- Coop dès le départ : jusqu'à 4 équipes par partie, 1 à 4 joueurs par équipe (le solo est simplement
  une équipe à 1 joueur — même modèle).
- Salle d'attente : **sur le serveur Bingo lui-même**, pas sur kal-games. Un joueur qui crée ou rejoint
  une partie sur kal-games est transféré immédiatement (pas de salle d'attente côté kal-games).
- Choix d'équipe dans la salle d'attente : ~~manuel (numéro d'équipe) OU "équipe aléatoire", par
  chaque joueur pour lui-même~~ **REMPLACÉ en 0.1.9** : c'est désormais l'hôte qui assigne l'équipe
  de chaque joueur (demande explicite de l'utilisateur, voir section 0.1.9 plus bas) - un joueur
  normal ne choisit plus sa propre équipe.
- Salle d'attente : un seul modèle, construit et capturé par l'utilisateur lui-même (sélection par
  position du joueur, comme pour les arènes sur kal-games — PAS de coordonnées à taper à la main),
  recapturable à tout moment. Dupliqué automatiquement sur plusieurs emplacements (un par partie
  simultanée possible).
- Durée de partie : modifiable par un admin **depuis les paramètres de kal-games** (pas depuis KalBingo),
  transmise à chaque partie créée. KalBingo n'impose pas de plafond local.
- Serveur de test : Kixster, nom facilement modifiable (`network.kal-games-server-name` côté KalBingo,
  `bingo.server-name` côté KalGames).

## Architecture technique

### Équipes/instances/mondes (`fr.kalium.bingo.game`)
- `BingoTeam` : 1 à 4 joueurs, numéro d'équipe.
- `BingoInstance` : UN monde = UNE équipe (pas un joueur) ; suivi de connexion par joueur au sein de
  l'équipe.
- `BingoGame` : liste d'instances (une par équipe), seed partagée, état (`GameState`), durée.
- `GameManager.createGame(gameId, seed, duration, teams)` : vérifie tout depuis `config.yml` (max
  équipes/joueurs par équipe, parties simultanées) — aucune valeur codée en dur. Pas de plafond de durée
  imposé ici (voir plus haut).
- `InstanceWorldManager` : génération/suppression des mondes à la volée par seed (`WorldCreator` +
  `VoidGenerator` réutilisé par ailleurs pour `bingo_lobby`).

### Salle d'attente (`fr.kalium.bingo.world`)
- `LobbySlots` : monde permanent `bingo_lobby` (jamais supprimé, contrairement aux mondes
  `bingo_<gameId>_<n>` qui sont éphémères), jusqu'à `instances.max-simultaneous-games` emplacements
  espacés le long de l'axe X (`lobby.spacing`).
- `LobbyTemplateService`/`LobbyTemplate` : capture/collage d'UN modèle de salle (cuboïde de BlockData +
  point d'apparition avec orientation), système maison sans dépendance externe (même esprit que le
  `TemplateService` de KalGames pour les arènes, mais volontairement plus simple : pas d'entités, pas de
  contenus de conteneurs — juste des blocs). Recapturable à tout moment : chaque capture republie
  automatiquement le modèle sur tous les emplacements.
- Sélection par position du joueur (`BingoAdminCommand` : `lobby pos1`/`pos2`/`spawn`/`capture`/`info`),
  pas de coordonnées à taper à la main — demande explicite de l'utilisateur, "comme pour les arènes sur
  kal-games".

### Réseau (`fr.kalium.bingo.network`)
- Design **pull** (pas push) pour l'affectation partie/équipe : dès qu'un joueur arrive sur Bingo sans
  affectation connue (`PlayerConnectListener`), `AssignmentService` envoie une demande à kal-games (canal
  BungeeCord `Forward`, sous-canal `KalBingoAssignRequest`) ; kal-games répond
  (`KalBingoAssignResponse`, via `BingoNetworkListener` côté KalGames). Choix motivé : un message
  "poussé" vers un serveur sans aucun joueur connecté n'est pas garanti d'être livré (le canal
  BungeeCord/Velocity route via la connexion d'un joueur) — en interrogeant plutôt que de recevoir un
  envoi non sollicité, les deux trajets (Bingo→kal-games, puis kal-games→Bingo) ont toujours un chemin
  garanti, puisqu'à chaque fois le serveur ciblé a déjà un joueur connecté.
- Délai d'attente configurable (`reconnect-during-game.assignment-wait-seconds`, 15s par défaut) avant
  renvoi vers kal-games si aucune réponse n'arrive.

### Équipes dans la salle d'attente (`fr.kalium.bingo.game.BingoParty`/`PartyManager`, `command.TeamCommand`)
- Une fois l'affectation connue, le joueur est téléporté dans la salle d'attente et choisit son équipe
  via `/bingoteam <1-4>` ou `/bingoteam random`.
- `PartyStarter` + `/bingoadmin start <gameId>` : fait passer une partie en attente aux vraies instances
  de jeu (mondes créés, joueurs téléportés). **Déclenchement actuellement MANUEL par un admin** —
  placeholder de test en attendant l'étape interface/chronomètre (compte à rebours/matchmaking) du
  cahier des charges, pas encore abordée. Ne pas prendre ce choix pour une décision finale.

## Repères techniques utiles

- Le squelette initial (uploadé par l'utilisateur, écrit par une autre session Claude sans accès réseau)
  supposait 1 instance = 1 joueur et un `ServerTransferService` placeholder (`"velocitysend"` en
  commande console). Les deux ont été abandonnés : le vrai mécanisme de transfert est le canal
  BungeeCord natif (`Connect`), déjà utilisé par `KalGames.connectLobby()`/`KaliumCore` — confirmé par
  inspection directe du code source de ces deux projets.
- `plugin.yml` : commandes `bingoadmin` (`lobby ...`, `start <gameId>`) et `bingoteam`.
- Build : `build.sh` (ECJ, même convention que KalGames/KaliumCore), sortie
  `/mnt/user-data/outputs/KalBingo-<version>.jar`.

## 0.1.2 — generateur de monde vide pour la map par defaut de Kixster

Demande utilisateur : sauvegarder la map actuelle de Kixster en renommant son dossier, puis la
remplacer par un monde vide. `BingoPlugin` surcharge desormais `getDefaultWorldGenerator(worldName, id)`
et retourne `VoidGenerator` (deja utilise pour `bingo_lobby`) — ce mecanisme standard Bukkit permet a
un plugin de fournir le generateur d'un monde arbitraire, y compris le monde par defaut du serveur, via
le mapping `worlds: <nom>: generator: KalBingo` dans `bukkit.yml` (pas besoin de Multiverse ou autre
plugin tiers). Le monde par defaut de Kixster s'appelle `Kixster SMP` (level-name dans
`server.properties`) — c'est le dossier `/Kixster SMP/` a la racine du serveur qui doit etre renomme
(le dossier `/world/` a la racine est un ancien monde inutilise, different, a ne pas toucher).

Important : renommer le dossier d'un monde activement charge pendant que le serveur tourne est risque
(fichiers ouverts, `session.lock`). Le serveur doit etre arrete avant le renommage, puis redemarre pour
qu'il recree `Kixster SMP` a partir de zero avec le generateur vide. Aucun outil console/RCON
disponible dans cette session — l'arret/redemarrage doit etre fait par l'utilisateur.

## 0.1.3 — correctif : le monde genere n'etait pas vide (load: STARTUP)

Apres le renommage + redemarrage (fait par l'utilisateur), le nouveau monde `Kixster SMP` n'etait PAS
vide. Diagnostic via `logs/latest.log` sur Kixster (pas de suppositions) :
```
[Server thread/ERROR]: Could not set generator for default world 'Kixster SMP': Plugin 'KalBingo v0.1.2' is not enabled yet (is it load:STARTUP?)
```
Cause : le monde principal du serveur est cree AVANT l'activation des plugins charges normalement
(`load: POSTWORLD`, valeur par defaut) — a ce moment-la, CraftBukkit cherche le plugin "KalBingo" pour
recuperer son generateur, ne le trouve pas encore active, et abandonne silencieusement en generant un
monde vanilla normal a la place (aucune exception cote plugin, juste ce warning serveur, d'ou
l'importance d'avoir verifie le log plutot que de deviner). Corrige en ajoutant `load: STARTUP` dans
`plugin.yml` de KalBingo : le plugin est alors active avant la creation des mondes, comme le font les
plugins de generation de monde (void/skyblock) en general.

Sequence a refaire par l'utilisateur avec la 0.1.3 : arreter Kixster, renommer/deplacer le dossier
`Kixster SMP` actuel (celui-la n'est PAS le monde original — c'est la tentative ratee avec du terrain
normal, generee apres le premier redemarrage ; le vrai monde original est `Kixster SMP_bak`, deja en
lieu sur), redemarrer. Verifier `logs/latest.log` : la ligne d'erreur "Could not set generator" ne doit
plus apparaitre.

## 0.1.4 — correctif : load: STARTUP cassait la creation du monde bingo_lobby

L'utilisateur a refait la sequence (arret, dossier `Kixster SMP` supprime lui-meme, redemarrage) avec
la 0.1.3 : toujours pas vide. Nouveau diagnostic via `logs/latest.log` (encore une fois, pas de
suppositions) :
```
[Server thread/ERROR]: Error occurred while enabling KalBingo v0.1.3 (Is it up to date?)
java.lang.IllegalStateException: Cannot create additional worlds on STARTUP
    at org.bukkit.craftbukkit.CraftServer.createWorld(CraftServer.java:1167)
    at KalBingo-0.1.3.jar//fr.kalium.bingo.world.LobbySlots.init(LobbySlots.java:49)
    at KalBingo-0.1.3.jar//fr.kalium.bingo.BingoPlugin.onEnable(BingoPlugin.java:61)
[KalBingo] Disabling KalBingo v0.1.3
[Server thread/ERROR]: Could not set generator for default world 'Kixster SMP': Plugin 'KalBingo v0.1.3' is not enabled yet (is it load:STARTUP?)
```
Cause : `load: STARTUP` (ajoute en 0.1.3) fait tourner `onEnable()` — qui appelait directement
`lobbySlots.init()`, donc `Bukkit.createWorld()` pour `bingo_lobby` — pendant la phase STARTUP, qui
n'autorise QUE la creation du monde principal, pas de mondes supplementaires. `onEnable()` plantait
avec une exception, Bukkit desactivait alors le plugin automatiquement, et le generateur du monde par
defaut redevenait indisponible (meme symptome qu'en 0.1.2, cause differente).

Corrige en reportant `lobbySlots.init()` (creation de `bingo_lobby`) a `ServerLoadEvent`
(`BingoPlugin` enregistre un `Listener` inline dans `onEnable()`) : cet evenement se declenche une
seule fois, une fois le serveur completement demarre (tous les mondes crees y compris ceux des
plugins, tous les plugins actives) — c'est le hook standard pour ce genre de creation de monde
differee quand un plugin est en `load: STARTUP`. Le reste de `onEnable()` (managers, commandes,
canaux reseau) ne cree aucun monde et continue de s'executer immediatement, sans changement.

Sequence a refaire par l'utilisateur avec la 0.1.4 : arreter Kixster, verifier qu'il ne reste qu'un
dossier `Kixster SMP_bak` (le vrai original) a la racine — sinon supprimer/deplacer tout ce qui
s'appelle `Kixster SMP`, redemarrer, verifier `logs/latest.log` (ni "Could not set generator" ni
"Cannot create additional worlds").

## Incident : plantage au demarrage sans rapport avec KalBingo

Au redemarrage suivant (toujours avec un dossier `Kixster SMP` absent), le serveur a carrement refuse
de demarrer, avec une erreur qui se produit AVANT l'activation des plugins, dans le bootstrap interne
de Paper :
```
[WorldFolderMigration] World storage migration is required during startup.
[VanillaWorldMigration] Starting Vanilla import for world 'Kixster SMP' (minecraft:overworld)
[ERROR]: Unable to read or access the world gen settings file! ...
[WARN]: Failed to load datapacks, can't proceed with server load.
java.lang.IllegalStateException: Overworld settings missing
```
Ce comportement n'etait apparu a AUCUNE des tentatives precedentes, qui partaient pourtant du meme
etat (dossier `Kixster SMP` totalement absent) — rien dans nos changements (jar KalBingo, bukkit.yml)
n'explique le declenchement de cette logique de "migration". A traiter comme un bug/instabilite
ponctuelle de ce build Paper (26.2-121) plutot qu'un defaut du plugin. Le dossier a moitie cree par ce
plantage (`players/dimensions/datapacks/data/session.lock/level.dat` sans `level.dat_old`) a ete range
dans `Kixster SMP_crashed_0.1.4` (rien supprime) pour repartir d'un etat propre. Si ca se reproduit, il
faudra regarder du cote de `config/` (paper-world-defaults.yml etc.) et `cache/` a la racine du
serveur, ou envisager `--safeMode`.

## 0.1.5 — les operateurs ne sont plus renvoyes vers kal-games sans partie en cours

Demande utilisateur : pouvoir construire ET modifier la salle d'attente a l'avenir sans etre coupe.
Probleme : `PlayerConnectListener` appelle `AssignmentService.requestAssignment()` pour CHAQUE joueur
qui rejoint Bingo (y compris un admin qui vient juste pour `/bingoadmin lobby tp`) ; si kal-games ne
connait pas de partie pour lui (ce qui est le cas normal pour un admin hors contexte de partie), il
etait renvoye vers kal-games au bout de `reconnect-during-game.assignment-wait-seconds` (15s par
defaut) - bien trop court pour construire/modifier la salle d'attente tranquillement.

Corrige dans `AssignmentService` (nouvelle methode privee `handleNoGameFound`, appelee a la fois par le
timeout et par une reponse explicite "found=false" de kal-games) : un joueur avec la permission
`bingo.admin` (la meme qui protege `/bingoadmin`, "les operateurs" demandes par l'utilisateur) reste
sur le serveur au lieu d'etre renvoye - message l'invitant a utiliser `/bingoadmin lobby tp`. Un joueur
normal sans partie garde l'ancien comportement (renvoi vers kal-games). Rien ne change quand une partie
EST trouvee (le flux salle d'attente/equipe normal continue de s'appliquer a tout le monde, y compris
aux admins qui rejoignent une vraie partie).

## 0.1.6 — menu graphique /menu pour la salle d'attente (opérateurs)

Demande utilisateur : "je ne comprends pas l'utilisation de la commande" pour capturer la salle
d'attente → un menu graphique plutot que des sous-commandes a taper, reserve aux operateurs.

Refactor prealable : la logique de `/bingoadmin lobby tp/pos1/pos2/spawn/capture/info` (jusque-la
inline dans `BingoAdminCommand`) a ete extraite dans une nouvelle classe partagee
`world.LobbyCaptureService` (etat pos1/pos2/spawn par joueur + actions), pour que les commandes texte
(gardees, pour ceux qui les preferent) ET le nouveau menu appellent exactement le meme code - aucune
logique dupliquee, aucun comportement nouveau, juste une presentation en plus.

Nouveau : `gui.LobbyMenu` (inventaire Bukkit 1 ligne, identifie de maniere fiable via un
`InventoryHolder` marqueur dedie plutot que le titre, pour eviter tout faux-positif avec un autre menu
d'un autre plugin) et `command.MenuCommand` (`/menu`, alias `/bingomenu`). Boutons : "Aller dans la
salle d'attente" (teleport + ferme le menu), "Coin 1"/"Coin 2"/"Point d'apparition" (laine verte/grise
selon si deja defini, re-cliquable a tout moment pour redefinir), "Capturer" (bloc emeraude/redstone
selon si les 3 sont prets), "Infos" (definie ou non, nombre d'emplacements). Le menu se rafraichit apres
chaque clic pour montrer l'etat a jour.

Acces reserve aux operateurs : commande protegee par la permission `bingo.admin` (meme permission que
`/bingoadmin`, `default: op` dans plugin.yml) a la fois au niveau de plugin.yml (Bukkit refuse
l'execution avant meme d'appeler l'executor) et re-verifiee dans `MenuCommand` pour un message en
francais coherent avec le reste du plugin plutot que le message par defaut de Bukkit.

## État confirmé (2026-09-23)

- **KalBingo 0.1.6 tourne sur Kixster, serveur démarré avec succès.**
- **Monde par défaut de Kixster CONFIRMÉ vide (void).** Le correctif en trois étapes (0.1.2 → 0.1.4,
  voir historique plus haut dans ce journal) a fonctionné ; le plantage `WorldFolderMigration` isolé
  rencontré en cours de route ne s'est pas reproduit et est bien resté un incident transitoire.
- **Salle d'attente construite et capturée** sur Kixster (monde `bingo_lobby`). Statut non précisé :
  capture faite via le nouveau menu `/menu` ou via les anciennes commandes texte `/bingoadmin lobby
  ...` — à confirmer si pertinent, mais dans les deux cas la capture partage la même logique
  (`LobbyCaptureService`), donc le résultat est équivalent.

## Pour reprendre la prochaine fois

- Tester le flux complet de bout en bout : `/bingo create` (kal-games) → `/bingo join <code>` →
  transfert vers Kixster → salle d'attente → `/bingoteam` (choix d'équipe) → `/bingoadmin start
  <gameId>` (admin, sur Bingo) → vérifier la création effective de l'instance de monde de partie.
- Prochaine étape du cahier des charges une fois ce flux validé : la grille Bingo (section 2/étape 4),
  puis validation des objectifs (5), interface/carte custom (6), chronomètre/compte à rebours automatique
  (7), détection de ligne (8), fin de partie/classement (9), outillage admin complet (10), intégration
  récompenses (11).
- `/bingo create`/`join` (kal-games) et `/bingoadmin start` (Bingo) restent des interfaces de TEST
  minimales, pas des décisions finales — à revoir une fois le flux confirmé fonctionnel (menu hub côté
  kal-games, vrai déclenchement de partie côté Bingo).

## 0.1.7 — grille Bingo configurable (section 2 du cahier des charges, étape 4)

Étape suivante de l'ordre de priorité (section 16), demandée par l'utilisateur en même temps que
l'intégration du menu Bingo côté kal-games (1.10.1, voir JOURNAL.md de KalGames).

Nouveau package `fr.kalium.bingo.grid` :
- `Objective` (record) : item (`Material`), quantité, `Difficulty` (EASY/MEDIUM/HARD/EXTREME — mêmes
  paliers que "bleu/jaune/orange/rouge" section 1 étape 11), `condition` (texte libre, **pas encore
  interprétée** : la validation automatique est la section 3, pas encore traitée — ce champ ne fait que
  capturer la donnée en attendant).
- `ObjectiveLibrary` : charge la liste depuis **`objectives.yml`, dans le dossier de données du plugin,
  PAS dans le `.jar` ni `config.yml`** — modifiable librement sans recompiler ni remplacer le plugin
  (exigence explicite du cahier des charges, section 2 : "configurable sans devoir modifier le plugin
  principal" + "système permettant de modifier facilement les listes d'items"). `load()` copie le fichier
  d'exemples par défaut au premier démarrage si absent ; `reload()` recharge à chaud.
- `BingoGrid`/`GridCell` : grille carrée `size x size`, une case = un objectif (+ un `boolean validated`
  présent mais **totalement inerte** pour l'instant — rien ne le fait passer à `true`, la détection
  automatique n'existe pas encore).
- `GridGenerator` : tirage aléatoire **sans répétition** dans le pool d'objectifs. Erreur explicite si le
  pool est trop petit pour la taille demandée. Aucun mode "grille fixe" construit — non demandé
  explicitement, et section 14 du cahier des charges liste "objectifs fixes ou aléatoires" comme "à
  définir avant l'ouverture", donc pas anticipé.

Intégration : `BingoGame` porte maintenant un champ `BingoGrid grid` (une seule grille par partie,
**partagée par toutes les équipes** — hypothèse de travail, pas une décision finale : la section 14 liste
"génération identique ou différente entre les joueurs" comme non tranché). `GameManager.assignGrid(game)`
génère et attache la grille ; `PartyStarter.start()` l'appelle juste après `prepareInstances()`, donc
`/bingoadmin start <gameId>` génère maintenant aussi la grille de la partie.

Nouveau fichier `objectives.yml` (ressource par défaut, copiée au premier démarrage) : **26 objectifs
d'exemple clairement marqués PLACEHOLDER** dans l'en-tête du fichier (pas une liste validée — section 13
du cahier des charges interdit explicitement d'inventer la liste finale). Assez pour tester une grille
5x5 par défaut (`grid.size: 5` dans `config.yml`, nouvelle section, valeur de départ raisonnable mais pas
définitive — section 14 : "taille exacte" non tranchée).

Nouvelles sous-commandes admin (`BingoAdminCommand`, ajoutées uniquement pour pouvoir tester/vérifier
cette étape, section 11 : "ne créer que les commandes réellement nécessaires") :
- `/bingoadmin grid reload` : recharge `objectives.yml` depuis le disque sans redémarrer le serveur.
- `/bingoadmin grid show <gameId>` : affiche en chat la grille générée pour une partie en cours (liste
  numérotée item/quantité/difficulté/condition) — en attendant l'interface/carte custom (étape suivante
  de l'ordre de priorité, pas encore traitée).

**Assomption prise sans confirmation explicite de l'utilisateur** (question posée mais seule la question
sur l'intégration menu a reçu une réponse) : liste d'objectifs livrée avec des exemples placeholder
clairement marqués plutôt qu'un fichier vide ou une vraie liste fournie par l'utilisateur — à corriger
facilement si ce n'est pas ce qui était voulu, le fichier n'est qu'un point de départ éditable.

## 0.1.8 — premier test de bout en bout : 5 correctifs remontés par l'utilisateur

Après déploiement + redémarrage des deux serveurs (0.1.7 / KalGames 1.10.1), l'utilisateur a pu pour la
première fois créer une partie depuis kal-games et arriver dans la salle d'attente sur Kixster. Retour
détaillé avec 5 problèmes, traités un par un ci-dessous.

### 1. Salle d'attente trop proche du modèle construit par l'admin
`LobbySlots.slotOrigin(slot)` plaçait l'emplacement 0 directement à l'origine du monde
(`0, 64, 0` + `slot * spacing`) — or c'est justement là, près du spawn du monde void, que l'admin
construit naturellement son modèle avec `/menu`/`/bingoadmin lobby capture`. Les deux se retrouvaient
visibles en même temps. Corrigé en ajoutant un décalage de départ configurable,
`lobby.slots-start-offset` dans `config.yml` (2000 blocs par défaut, le long de l'axe X, avant le premier
emplacement) : `slotOrigin` devient `(startOffset + slot * spacing, 64, 0)`. À augmenter encore si 2000
blocs restent insuffisants (dépend de la taille du modèle capturé).

### 2. Configuration du nombre d'équipes/joueurs à la création
Traité côté KalGames (1.10.2, voir son JOURNAL.md) : nouveau formulaire à la création avec deux champs
numériques. Côté KalBingo, le protocole réseau (`AssignmentNetworkListener`/`AssignmentService`) et
`BingoParty`/`PartyManager.getOrCreate(...)` ont été étendus pour recevoir et porter ces valeurs
(`host`, `teamCount`, `teamSize`) par partie, à la place des anciennes constantes globales — nécessaire
aussi pour les points 3 et 4 ci-dessous (l'hôte doit être identifié pour le menu, et `trySetTeam` doit
connaître les bornes réelles de la partie).

### 3. Aucun moyen de configurer les équipes une fois dans la salle d'attente
Demande : redonner la nether star bloquée (menu de partie) une fois dans la salle d'attente. Nouveau
`gui.LobbyItems` (même pattern PDC que `ItemService`/`HubListener` côté KalGames) : donne une nether
star "Menu de la partie" au joueur dès qu'il est téléporté dans la salle d'attente
(`AssignmentService`, juste après le téléport de succès). Nouveau `gui.PartyMenu` (inventaire Bukkit,
même pattern que `LobbyMenu`) : boutons d'équipe (1 par équipe, laine/teinture selon complet/occupé/
libre, liste des membres en lore), case info (hôte + équipe du joueur), et — **uniquement si le joueur
est l'hôte de la partie** (`party.isHost`) — "Lancer la partie"/"Annuler la partie". Le clic sur une
équipe appelle `BingoParty.trySetTeam(...)` (même méthode que `/bingoteam`, aucune logique dupliquée,
refactor de `TeamCommand` pour l'utiliser aussi).

**Assomption non confirmée par l'utilisateur** : "lancer"/"annuler" réservés à l'hôte uniquement, par
analogie avec le fonctionnement des menus de partie côté KalGames (`PlayerMenus.openGameMenu`). À
confirmer/corriger si un autre fonctionnement était voulu (ex. n'importe quel joueur peut annuler, ou
un admin doit pouvoir le faire aussi).

### 4. Expulsion rapide de la salle d'attente (aucune partie en cours)
Cause exacte non confirmée avec certitude (pas de log d'erreur trouvé permettant de trancher) : le seul
chemin de code menant à cette expulsion est `AssignmentService.handleNoGameFound` (déclenché soit par une
réponse explicite "aucune partie trouvée" de kal-games, soit par l'absence de réponse dans le délai de
`reconnect-during-game.assignment-wait-seconds`, 15s par défaut). Hypothèse : en test solo, kal-games peut
se retrouver momentanément sans aucun joueur connecté juste après le transfert de l'hôte vers Kixster, ce
qui casserait la livraison du message `Forward` de la demande d'affectation (Bingo → kal-games) — le même
type de problème de fiabilité que celui déjà identifié et contourné, dans l'autre sens, par le design
"pull" original (voir plus haut dans ce journal). **Correctif défensif appliqué sans confirmation du
diagnostic** : `AssignmentService` renvoie maintenant la demande d'affectation toutes les 2 secondes
(`scheduleRetries`) tant que le délai d'attente n'est pas écoulé, au lieu d'un unique envoi initial — si
le premier essai échoue à être livré, un des suivants a de bonnes chances de passer une fois kal-games de
nouveau peuplé. **À signaler à l'utilisateur : si le problème se reproduit malgré ce correctif, il faudra
creuser plus profondément (logs serveur des deux côtés au moment précis de l'expulsion).**

### 5. Salle d'attente non protégée
Nouveau `world.LobbyProtectionListener` (enregistré uniquement pour le monde `bingo_lobby`, jamais pour
les mondes de partie) : bloque casse de bloc, pose de bloc, interaction avec les blocs (clic droit,
plaques de pression), interaction avec un seau (vider/remplir), empêche le spawn de monstres
(`CreatureSpawnEvent` sur toute entité `Monster`), désactive le PvP (dégâts joueur→joueur annulés). La
nether star du menu de partie (point 3) reste utilisable : le listener laisse passer les interactions
sur l'item du menu avant d'appliquer les restrictions générales.

## 0.1.9 — menu en Dialog natif, assignation des équipes par l'hôte, correctifs

Après le premier test de bout en bout (0.1.8), retours complémentaires de l'utilisateur :

### Nether star trop proche du modèle / expulsion : confirmé fonctionnel pour l'hôte
Contrairement à ce qui avait été supposé un temps pendant le diagnostic, la nether star et la
distance de la salle d'attente fonctionnaient déjà correctement pour l'hôte (0.1.8 était bon sur
ces deux points) - fausse piste écartée en cours de route.

### Bug réel trouvé : un joueur qui REJOINT (pas l'hôte) arrive sur le modèle puis se fait expulser
Deux causes identifiées, corrigées toutes les deux :
1. **`PartyManager.getOrCreate` utilisait `computeIfAbsent`** : quand l'hôte se connecte le
   premier (créant l'objet `BingoParty` côté KalBingo avec le roster attendu tel qu'il était à CE
   moment-là), puis qu'un second joueur rejoint la partie et se connecte à son tour, le second
   appel à `getOrCreate` pour le MÊME `gameId` était ignoré par `computeIfAbsent` (l'objet existe
   déjà) - le roster plus à jour envoyé par kal-games (incluant le nouveau joueur) était donc
   silencieusement perdu. Corrigé : `getOrCreate` fusionne désormais le roster reçu dans la partie
   existante (`BingoParty.mergeExpectedRoster`) au lieu de l'ignorer.
2. **Le spawn du monde `bingo_lobby` restait fixé à `(0, 80, 0)`**, jamais décalé par
   `lobby.slots-start-offset` (contrairement aux emplacements collés, eux bien corrigés en 0.1.8) :
   tant que l'affectation réseau d'un joueur n'est pas encore résolue (le temps d'un aller-retour
   avec kal-games), Bukkit le place à ce spawn par défaut - c'est-à-dire pile sur le modèle
   original de l'admin. Corrigé : le spawn du monde suit désormais le même décalage
   (`slotOrigin`-compatible : `(startOffset, 80, 0)`).

**Limite connue, PAS résolue** (voir commentaire ajouté dans `config.yml`,
`reconnect-during-game.assignment-wait-seconds`) : le message d'affectation (Bingo → kal-games)
passe par un canal réseau qui exige qu'au moins un joueur soit connecté sur kal-games pour être
livré. En test à 2 comptes, si l'hôte ET le second joueur se retrouvent TOUS LES DEUX sur Kixster
en même temps, kal-games tombe à 0 joueur et la demande ne peut tout simplement pas être livrée,
quel que soit le délai configuré ou le nombre de réessais - le joueur reste bloqué au spawn (donc
plus près du modèle que prévu, mais plus autant qu'avant grâce au correctif du point 2) puis est
expulsé au bout du délai (`assignment-wait-seconds`, remonté de 15 à 30s par prudence, mais ça ne
résout pas le cas décrit). **Si ça se reproduit** : la vraie solution serait que kal-games transmette
l'affectation de manière proactive au moment du transfert (`transferToBingo`, où le joueur
transféré est justement encore présent pour porter le message), plutôt que Bingo l'interroge après
coup - je ne l'ai pas implémenté sans votre confirmation, c'est un changement de protocole plus
large que je préfère proposer avant de faire.

### Menu en Dialog natif (remplace l'inventaire Bukkit)
Demande explicite : "je préfère que la nether star ouvre un menu plutôt qu'une page type
inventaire". Nouveau `gui.DialogGui` (même principe que `Gui.java` côté KalGames : Dialog natif
Minecraft, boutons avec callback, MiniMessage pour le texte - volontairement plus simple, pas de
système de traduction multi-langue puisque KalBingo n'en a pas). `PartyMenu` entièrement réécrit
pour l'utiliser : n'implémente plus `Listener` (plus de clic d'inventaire à intercepter, seulement
des boutons de Dialog).

### Assignation des équipes réservée à l'hôte (remplace le choix libre par chaque joueur)
Demande explicite : "le créateur de la partie doit assigner les équipes aux joueurs lui même" -
remplace la décision confirmée précédemment ("choix manuel OU aléatoire, par chaque joueur pour
lui-même", encore listée plus haut dans ce journal sous "Décisions confirmées" - **cette section
n'est donc plus à jour sur ce point précis**). Dans le nouveau `PartyMenu` : un joueur normal voit
la composition des équipes en LECTURE SEULE ; seul l'hôte a des boutons, un par joueur connecté,
qui le font passer à l'équipe suivante non complète en cliquant dessus (même principe de cycle que
`PlayerMenus.openTeams` côté KalGames). `/bingoteam` a été adapté en conséquence : réservé à
l'hôte, nouvelle syntaxe `/bingoteam <joueur> <équipe>` (assignation directe d'un joueur précis, en
complément du clic-pour-cycler du menu). Validation toujours partagée via `BingoParty.trySetTeam`
entre les deux points d'entrée, aucune logique dupliquée.

## 0.1.10 — relais HTTP KaliumRelay (résout la limite réseau connue en 0.1.9)

Suite à la question de l'utilisateur ("est-il possible de charger en continue une zone sur
kal-games sans joueur pour que les informations puissent s'envoyer sur le serveur bingo ?") : j'ai
expliqué que le chargement de chunks ne changerait rien (la contrainte est au niveau du protocole
Minecraft - le canal de messagerie plugin BungeeCord/Velocity a besoin d'un JOUEUR connecté des
deux côtés pour livrer un message, pas d'un chunk chargé), puis proposé plusieurs options via
AskUserQuestion. L'utilisateur a choisi "Plugin Velocity qui relaie les messages", confirmé pouvoir
ouvrir un port sur l'hébergement Minestrator du proxy, et fourni le port **46199**.

**Nouveau projet `KaliumRelay`** (plugin Velocity, sur le proxy `ProxyVelocity` uniquement - pas un
plugin des serveurs kal-games/Kixster) : petit serveur HTTP autonome (`com.sun.net.httpserver`,
sans dépendance externe) qui permet à KalGames de DÉPOSER (POST) l'affectation d'un joueur au
moment de `transferToBingo`, et à KalBingo de la RÉCUPÉRER (GET, consomme/supprime) dès que ce
joueur se connecte chez lui - complètement indépendant de toute connexion joueur sur l'un ou
l'autre serveur, contrairement au canal BungeeCord existant. Jeton partagé fixe (même valeur dans
les 3 projets, pas de copier-coller manuel nécessaire), purge automatique des entrées non
récupérées après 2 minutes. Voir `KaliumRelay/JOURNAL.md` (nouveau) pour le détail.

Ce relais est un COMPLÉMENT au canal BungeeCord existant, pas un remplacement : `AssignmentService`
interroge maintenant les deux chemins EN PARALLÈLE (nouveau `RelayClient` + méthode `pollRelay`),
la première réponse gagne (`handleResponse` ignore désormais toute résolution tardive grâce à
`pending.remove()` vérifié). Si `network.relay-url` est vide ou le relais injoignable, tout continue
de fonctionner comme en 0.1.9 (silencieux, aucune casse).

Côté KalGames : `BingoPartyManager.transferToBingo` dépose l'affectation sur le relais juste avant
le transfert du joueur (nouvelle méthode `pushAssignment`, appel HTTP asynchrone, échec loggé mais
jamais bloquant). Nouvelles clés `bingo.relay-url` / `bingo.relay-token` dans `config.yml`.

**Déployé le 23/09/2026 via WinSCP** : `KalBingo-0.1.10.jar` sur Kixster (ancien `0.1.9.jar` archivé
dans `/plugins/_removed-kalbingo-0.1.9/`), `KalGames-1.10.3.jar` sur kal-games (ancien `1.10.2.jar`
archivé dans `/plugins/_removed-kalgames-1.10.2/`, voir son JOURNAL.md), et `KaliumRelay-1.0.0.jar`
sur le proxy `ProxyVelocity` (première installation, rien à archiver). **Les TROIS serveurs
(kal-games, Kixster, ET le proxy Velocity) doivent être redémarrés** pour ce déploiement - pas fait
par Claude (pas d'accès console, juste SFTP). C'est un redémarrage supplémentaire par rapport aux
déploiements précédents (le proxy n'avait encore jamais eu besoin d'être redémarré pour KalBingo).

## 0.1.10 (suite) — le relais ne fonctionnait toujours pas : deux bugs trouvés par diagnostic de logs

Après le déploiement ci-dessus, l'utilisateur a signalé que le bug persistait exactement à
l'identique : l'hôte crée/assigne bien la partie, mais le joueur qui rejoint arrive quand même dans
une autre salle d'attente et se fait expulser après quelques secondes. Diagnostic mené en
téléchargeant `logs/latest.log` de `kal-games`, `Kixster` et `ProxyVelocity` via WinSCP (pas
d'accès console/SSH) plutôt qu'en devinant. Deux causes INDÉPENDANTES trouvées :

1. **KaliumRelay ne démarrait pas du tout sur le proxy** (`com.google.inject.CreationException: No
   injectable constructor for type KaliumRelay` dans `proxy-latest.log`) - annotation `@Inject`
   JSR-330 au lieu de Guice, corrigé en 1.0.1. Voir `KaliumRelay/JOURNAL.md` pour le détail complet.
2. **Le `config.yml` réellement déployé sur Kixster (et sur kal-games) datait d'avant TOUS les ajouts
   liés au relais** - `saveDefaultConfig()` (Paper/Bukkit) ne fusionne jamais de nouvelles clés dans
   un `config.yml` déjà présent sur le disque, il ne l'écrit que si le fichier n'existe pas encore.
   Résultat : `network.relay-url` résolvait au repli code (`""`, désactivé) malgré un code et un
   `config.yml` bundlé corrects depuis la 0.1.10, ET `assignment-wait-seconds` était resté à
   l'ancienne valeur `15` au lieu de `30`. `kixster-latest.log` le confirmait précisément : le second
   joueur connecté dans le mauvais monde (`overworld` au lieu de `bingo_lobby`, preuve que
   l'affectation n'avait jamais été résolue) puis expulsé exactement 15 secondes plus tard - la durée
   exacte de l'ancienne valeur, signature du timeout de `handleNoGameFound()`, pas d'un kick ou d'une
   déconnexion volontaire.

**Corrigé le 23/09/2026, sans nouveau bump de version côté KalBingo** (pas de changement de code, le
0.1.10 déployé était déjà correct - seul le fichier de config sur le disque du serveur était en
retard) : `config.yml` de Kixster téléchargé, `assignment-wait-seconds` remis à `30`,
`network.relay-url`/`network.relay-token` et `lobby.slots-start-offset` ajoutés (absents jusque-là),
aucune autre clé touchée, puis ré-uploadé au même chemin via WinSCP. Le `config.yml` de kal-games a
reçu le même traitement pour la section `bingo:` manquante (voir JOURNAL.md de KalGames). `KaliumRelay`
redéployé en 1.0.1 sur le proxy (ancien 1.0.0 archivé dans `/plugins/_removed-kaliumrelay-1.0.0/`).
Cette limite de `saveDefaultConfig()` n'est pas spécifique à Bingo/relais : à garder en tête pour
CHAQUE future clé de config ajoutée à n'importe quel plugin de ce workspace, puisqu'elle a affecté
silencieusement KalGames ET KalBingo sur plusieurs versions sans qu'aucune erreur ne soit levée.

## Pour reprendre la prochaine fois (mise à jour)

- **Déployé sur Kixster (7003.mystrator.com) le 23/09/2026 via WinSCP** : `KalBingo-0.1.10.jar` (voir
  section 0.1.10 ci-dessus) ET `config.yml` corrigé en place le même jour (voir section juste
  au-dessus). **Redémarrage des TROIS serveurs (kal-games, Kixster, proxy Velocity) requis** - le
  proxy est un ajout par rapport aux déploiements précédents, et doit tourner en 1.0.1 (pas 1.0.0).
- Prioritaire à tester : le flux complet hôte + un second joueur qui rejoint (le bug initial) - cette
  fois les DEUX causes trouvées (relais qui ne démarrait pas + config jamais mise à jour) sont
  corrigées, contrairement au premier essai (déploiement 0.1.10 seul) qui n'avait résolu ni l'une ni
  l'autre malgré un déploiement en apparence complet. Si le blocage/expulsion se reproduit malgré
  tout, vérifier d'abord que le port 46199 est bien accessible depuis kal-games ET Kixster vers le
  proxy (pas juste ouvert sur l'hébergement), et regarder les logs du plugin KaliumRelay sur le proxy
  (préfixe `[KaliumRelay]`) - cette fois ils devraient au moins exister.
- Nouveau menu (Dialog) et assignation des équipes par l'hôte : à tester et confirmer que c'est
  bien le fonctionnement voulu (host clique sur un joueur pour le faire changer d'équipe - pas de
  liste déroulante pour choisir une équipe précise depuis le menu, seulement via `/bingoteam
  <joueur> <équipe>` en commande - à signaler si un choix direct dans le menu est préférable).
- Toujours en attente (demandé, pas encore implémenté - interrompu par les bugs plus urgents
  ci-dessus) : lister les parties existantes sur KalGames pour pouvoir les rejoindre depuis un menu,
  plutôt que seulement par code.
- Ancien `KalBingo-0.1.9.jar` archivé (non détruit) dans `/plugins/_removed-kalbingo-0.1.9/`.
  KalGames 1.10.3 déployé le même jour sur kal-games (7021.mystrator.com), voir son JOURNAL.md.
- À tester une fois les deux serveurs redémarrés, dans l'ordre des 5 points ci-dessus : distance de la
  salle d'attente au modèle (ajuster `lobby.slots-start-offset` si encore insuffisant), formulaire de
  création avec nombre d'équipes/joueurs, menu joueur (nether star) avec configuration des équipes +
  lancer/annuler pour l'hôte, absence d'expulsion prématurée (signaler si ça se reproduit malgré le
  correctif défensif), protection effective de la salle d'attente.
- Deux points à confirmer/corriger auprès de l'utilisateur (assomptions prises faute de précision) : le
  comportement exact d'"annuler la partie" (renvoie tous les joueurs connectés vers kal-games + libère
  l'emplacement + oublie la partie) et la restriction lancer/annuler aux seuls hôtes.
- Vérifier si la liste d'objectifs placeholder d'`objectives.yml` convient ou doit être remplacée par une
  vraie liste fournie par l'utilisateur (section 13 du cahier des charges) — toujours en attente.
- Prochaine étape du cahier des charges une fois ce test validé : validation des objectifs (section 3,
  étape 5 de l'ordre de priorité), puis interface Bingo/carte custom (6), chronomètre/déclenchement
  automatique de partie (7, pourrait remplacer le déclenchement manuel actuel), détection de ligne (8),
  fin de partie/classement (9), administration complète (10), intégration récompenses (11).

## 0.1.11 — retrait de la Nether Star sur la carte, objectifs (papier + validation auto), reconnexion en partie

Suite au premier test complet reussi (creation/assignation d'equipes/generation des maps OK, voir
JOURNAL.md de KaliumRelay 1.0.1 pour le correctif qui l'a permis), l'utilisateur a remonte trois
demandes explicites :

1. **La Nether Star (menu de salle d'attente) restait dans la hotbar sur la carte de jeu**, inutilisable
   ("il n'y a pas de partie en attente" au clic - `PartyMenu.open` echoue car `partyManager` ne
   connait plus cette partie une fois lancee). `LobbyItems.remove(Player)` (nouveau) retire l'objet de
   TOUTE la hotbar/l'inventaire, appele dans `PartyStarter.start()` au moment de la teleportation dans
   l'instance.
2. **Pas de liste d'objectifs a recolter** : nouvel objet papier "Objectifs" (`GameItems`, slot 8,
   donne au meme moment que ci-dessus) - MOUVABLE dans l'inventaire (contrairement a la Nether Star)
   mais pas jetable (`GameItemListener.onDrop`), demande explicite de l'utilisateur. Clic droit ouvre
   `GameMenu` (Dialog, lecture seule) : temps restant (`BingoGame.getRemaining()`, nouveau), nombre de
   cases validees par CHAQUE equipe (`BingoGame.countValidated`), et la liste complete des objectifs de
   la grille avec coche pour ceux deja valides par l'equipe du joueur qui consulte.
   - **Validation des objectifs elle-meme (section 3 du cahier des charges) : jamais implementee avant
     cette version** (`GridCell.validated` existait mais restait totalement inerte, et etait de toute
     facon un booleen unique PARTAGE par case - incompatible avec un suivi par equipe). Mecanique
     confirmee par l'utilisateur via AskUserQuestion le 23/09/2026 (deux choix distincts) : (a)
     detection AUTOMATIQUE - une case se valide des qu'UN membre de l'equipe possede, a lui seul, la
     quantite requise dans son inventaire (pas de somme entre plusieurs joueurs, pas de depot manuel
     dans un coffre) ; (b) une fois validee, la case reste acquise DEFINITIVEMENT pour le reste de la
     partie, meme si l'objet est ensuite perdu/depense (pas de reverification). Nouvelle classe
     `ObjectiveValidationTask`, verifiee chaque seconde (`runTaskTimer`, 20 ticks) pour toutes les
     parties `IN_PROGRESS`. Etat de progression stocke dans `BingoGame.teamProgress` (nouveau,
     `Map<Integer, boolean[]>` par numero d'equipe - `GridCell.validated` reste inerte, non reutilise
     pour ne pas melanger un etat partage et un etat par equipe).
3. **Reconnexion en cours de partie** : "si un joueur est deconnecte durant une partie et qu'il se
   reconnecte avant la fin de la partie il faut que le proxy le renvoi directement sur la partie".
   Aucun mecanisme de reconnexion en jeu n'existait avant cette version (`GameManager.onPlayerDisconnect/
   onPlayerReconnect` existaient deja mais n'etaient JAMAIS appeles - code mort depuis leur creation).
   Deux volets necessaires, aucun ne suffisant seul :
   - **Cote proxy (routage initial)** : nouveau registre `ActiveGameRegistry` dans KaliumRelay (voir son
     JOURNAL.md, version 1.1.0) - KalBingo y enregistre chaque joueur comme "en partie sur kixster" des
     que `PartyStarter.start()` le teleporte dans son instance (`RelayClient.registerActiveGame`,
     nouveau, POST `/active-game/{playerId}`). Le proxy consulte ce registre a CHAQUE nouvelle connexion
     (`PlayerChooseInitialServerEvent`) et route directement vers Kixster si le joueur y figure, au lieu
     du routage habituel (kal-games/lobby).
   - **Cote KalBingo (etat du joueur une fois sur Kixster)** : `PlayerConnectListener` reecrit pour
     distinguer un NOUVEL arrivant (comportement inchange : `AssignmentService.requestAssignment`) d'une
     RECONNEXION en cours de partie (`GameManager.findGameOf`, nouveau, cherche dans TOUTES les parties
     actives) - dans ce second cas, `AssignmentService` n'est PAS sollicite du tout (la BingoParty de ce
     joueur a deja ete consommee/supprimee au lancement, voir `PartyStarter` : l'interroger aurait
     provoque un "aucune partie trouvee" et un renvoi a tort vers kal-games, exactement le bug qu'on
     corrige). Bukkit replace de lui-meme le joueur a sa derniere position dans le monde de son instance
     (jamais supprime tant que la partie est active) - aucune teleportation manuelle necessaire cote
     Kixster, seulement la mise a jour de `BingoInstance.setPlayerConnected`/`GameManager.onPlayerReconnect`.
   - **Limite connue, assumee (hors sujet de cette demande)** : aucune fin de partie automatique n'existe
     encore cote KalBingo (`GameManager.cleanupGame` n'est appele nulle part, `BingoGame.isTimeUp()` n'est
     verifie par personne) - une partie ne se termine donc jamais d'elle-meme, et l'enregistrement
     "en partie" cote KaliumRelay n'est jamais retire automatiquement non plus (voir son JOURNAL.md). Pas
     un probleme concret tant que la fin de partie n'existe pas ailleurs non plus - a revisiter ensemble
     le jour ou section 3 (validation) et section 9 (fin de partie/classement) seront traitees.

Nouvelle cle `network.self-server-name: "kixster"` dans `config.yml` (nom de CE serveur cote proxy,
transmis au relais pour le routage de reconnexion) - valeur par defaut deja correcte dans le code
(`"kixster"`) si jamais absente du fichier deploye, donc pas de nouveau risque lie a la staleness de
`saveDefaultConfig()` documentee le 23/09/2026 (voir plus haut) meme si le `config.yml` deploye n'est
pas repatché.

**Deploye le 23/09/2026 via WinSCP** : ancien `KalBingo-0.1.10.jar` archive (non detruit) dans
`/plugins/_removed-kalbingo-0.1.10/`, `KalBingo-0.1.11.jar` envoye dans `/plugins/` sur Kixster.
`KaliumRelay-1.1.0.jar` deploye le meme jour sur le proxy (voir son JOURNAL.md). **Redemarrage de
Kixster ET du proxy Velocity requis** - kal-games N'A PAS besoin d'etre redemarre cette fois (aucune
modification cote KalGames).

## Pour reprendre la prochaine fois (mise à jour, 0.1.11)

- Une fois Kixster et le proxy redemarres : verifier que la Nether Star disparait bien en arrivant sur
  la carte, que le papier "Objectifs" fonctionne (liste, temps restant, progression par equipe,
  validation automatique en ramassant les items, non jetable), et tester la reconnexion (se deconnecter
  pendant une partie en cours puis se reconnecter - doit arriver directement sur Kixster, dans son
  monde d'instance, pas sur kal-games).
- La mecanique de validation (detection automatique par inventaire, verrouillage definitif) a ete
  confirmee par l'utilisateur mais reste la toute premiere version de cette brique (section 3) : pas de
  message specifique de "Bingo !"/ligne completee (section 8, toujours a faire), pas de points par
  difficulte (section 1 etape 11, mentionnee mais jamais implementee), `Objective.condition` toujours
  pas interprete (texte libre, ignore).
- Fin de partie (section 9) toujours pas implementee : `GameManager.cleanupGame`/`BingoGame.isTimeUp()`
  restent inutilises pour l'instant - a traiter ensemble avant que ça devienne genant (mondes d'instance
  qui s'accumulent indefiniment, joueurs enregistres "en partie" pour toujours cote KaliumRelay).

## 0.1.12 — fin de partie (temps écoulé + victoire), salle d'attente post-victoire, durée choisie côté Kal

Demandes explicites de l'utilisateur (23/09/2026, en deux temps) :
> "la partie doit se terminée à la fin du temps règlementaire (1h par défaut)" / "il faut ajouter la
> possibilité de choisir la durée de la partie lorsqu'on la crée sur Kal"

puis, en précision/extension :
> "Kalgames, le temps par defaut maximum doit être 1h, cette limite doit pouvoir etre changée via les
> parametres de kalgames. le createur de la partie doit pouvoir réduire le temps mais ne doit pas
> pouvoir dépasser le temps maximum défini par les opérateurs." / "Si une équipe rempli tous les
> objectifs alors tous les joueurs sont renvoyés en salle d'attente, ils reçoivent la nether star, et
> peuvent partir quand ils le souhaitent. si ils reste trop longtemps dans la salle d'attente apres la
> partie alors ils sont expulsés (apres 10 minutes)"

Comble la limite documentée depuis la 0.1.11 : `GameManager.cleanupGame`/`BingoGame.isTimeUp()` étaient
définis mais jamais appelés par personne — aucune partie ne se terminait jamais d'elle-même.

- **`GameEndService` (nouvelle classe)** : centralise les DEUX déclencheurs de fin de partie, jamais
  fusionnés (l'utilisateur les a décrits différemment) :
  - **Temps écoulé** (`tick()`, appelé chaque seconde par `BingoPlugin`, comme `ObjectiveValidationTask`) :
    dès que `BingoGame.isTimeUp()` pour une partie `IN_PROGRESS`, TOUS les joueurs (en ligne) sont
    renvoyés directement vers kal-games (`AssignmentService.sendBackToKalGames`), le papier "Objectifs"
    retiré (`GameItems.remove`).
  - **Victoire** (`checkWin(game, teamNumber)`, appelé par `ObjectiveValidationTask` juste après CHAQUE
    nouvelle case validée, avant qu'un `tick()` de timeout ne puisse s'exécuter sur la même partie) : dès
    qu'une équipe atteint `grid.getSize() * grid.getSize()` cases validées, TOUS les joueurs (équipe
    gagnante ET perdantes) sont téléportés dans la salle d'attente Bingo (`LobbySlots.reserve(gameId)` —
    le même mécanisme que la salle d'attente pré-partie, ré-invoqué avec le même gameId puisque
    `PartyStarter.start()` l'avait déjà libéré au lancement), reçoivent la Nether Star
    (`LobbyItems.give`), et sont suivis dans une file d'attente interne (`lingeringDeadlines`) avec une
    échéance de `game.post-game-lobby-timeout-seconds` (10 min par défaut).
  - Dans les deux cas : `RelayClient.clearActiveGame(playerId)` est appelé pour CHAQUE membre de la
    partie (en ligne ou non, async) — une future reconnexion doit suivre le routage normal (kal-games),
    pas être renvoyée indéfiniment vers Kixster (voir 0.1.11/1.1.0) — puis `GameManager.cleanupGame` est
    différé de `game.end-cleanup-delay-seconds` (5s par défaut) pour laisser le temps au transfert
    (Connect ou téléportation) de sortir effectivement les joueurs du monde avant que
    `InstanceWorldManager.deleteInstanceWorld()` ne tente de le décharger (échoue/log un avertissement
    si des joueurs y sont encore présents).
  - **Départ volontaire** (`leaveVoluntarily`) : un joueur "lingering" peut repartir à tout moment (clic
    sur la Nether Star, voir `LobbyProtectionListener` ci-dessous) — renvoyé immédiatement vers
    kal-games, sans attendre les 10 minutes.
  - **Expulsion automatique** (`sweepLingering`, appelée depuis le même `tick()`) : au-delà de
    l'échéance, un joueur encore présent est renvoyé vers kal-games de la même façon.
  - L'emplacement de salle d'attente réservé pour la partie (`LobbySlots`) est libéré
    (`releaseSlotIfEmpty`) dès qu'il ne reste plus personne en attente (parti volontairement ou expulsé),
    pour qu'il puisse resservir à une AUTRE partie qui se terminerait ensuite par victoire.
- **`ObjectiveValidationTask`** : appelle désormais `GameEndService.checkWin(game, team)` juste après
  chaque `markValidated` réussi ; si la partie n'est plus `IN_PROGRESS` juste après (victoire), arrête
  immédiatement de traiter les cases restantes de cette partie pour ce tick (`break outer`) — inutile de
  continuer à valider des cases pour une partie déjà terminée.
- **`LobbyProtectionListener`** : le clic sur la Nether Star a désormais DEUX comportements selon le
  contexte — salle d'attente AVANT une partie (cas normal) : ouvre `PartyMenu` comme avant ; salle
  d'attente APRÈS une victoire (`GameEndService.isLingering`) : `PartyMenu` échouerait avec "aucune
  partie en attente" (la `BingoParty` a déjà été consommée au lancement) — déclenche donc directement
  `GameEndService.leaveVoluntarily` à la place.
- **`PlayerConnectListener`** : `findGameOf` est désormais filtré sur `GameState.IN_PROGRESS` — une
  partie qui vient de se terminer reste quelques secondes dans `GameManager.getActiveGames()` le temps
  du nettoyage différé (voir `GameEndService.scheduleCleanup`) ; sans ce filtre, un joueur qui se
  reconnecterait pendant cette fenêtre aurait été à tort traité comme "encore en partie" (papier
  Objectifs redonné, message de reconnexion) alors que la partie est déjà finie.
- **Nouvelles clés `config.yml`** (section `game:`) : `end-cleanup-delay-seconds: 5` et
  `post-game-lobby-timeout-seconds: 600` — **à ajouter manuellement dans le `config.yml` déployé sur
  Kixster** (staleness de `saveDefaultConfig()` documentée le 23/09/2026, voir plus haut : ces deux clés
  seraient sinon absentes, mais le code utilise les mêmes valeurs par défaut en repli, donc pas de casse
  si l'ajout manuel est oublié dans l'immédiat — juste pas modifiable sans redeploiement tant que ce
  n'est pas fait).

**Durée de partie choisie côté KalGames** (répond à la demande initiale, hors KalBingo — voir aussi le
JOURNAL.md de KalGames pour le détail) : KalBingo ne change pas ici, il continue de recevoir la durée
telle que décidée côté kal-games (`BingoPartyManager.create`, transmise via `party.duration()` au
lancement) — aucun plafond n'est imposé localement par ce plugin, comme documenté depuis l'origine.

**Déployé le 23/09/2026 via WinSCP** : ancien `KalBingo-0.1.11.jar` archivé (non détruit) dans
`/plugins/_removed-kalbingo-0.1.11/`, `KalBingo-0.1.12.jar` envoyé dans `/plugins/` sur Kixster.
`KalGames-1.10.4.jar` déployé le même jour sur kal-games (voir son JOURNAL.md). **Redémarrage de
Kixster ET de kal-games requis** (le proxy/KaliumRelay n'a pas changé cette fois, pas besoin de le
redémarrer).

## Pour reprendre la prochaine fois (mise à jour, 0.1.12)

- Une fois Kixster et kal-games redémarrés : tester les DEUX fins de partie séparément — (1) laisser le
  temps s'écouler (ou réduire `game.default-duration-seconds`/la durée choisie à la création pour un
  test rapide) et vérifier le renvoi direct vers kal-games ; (2) remplir la grille d'une équipe et
  vérifier le renvoi vers la salle d'attente Bingo avec la Nether Star, le départ volontaire par clic, et
  (avec un délai raccourci pour tester, voir `post-game-lobby-timeout-seconds`) l'expulsion automatique.
- Penser à ajouter manuellement `game.end-cleanup-delay-seconds: 5` et
  `game.post-game-lobby-timeout-seconds: 600` dans le `config.yml` déployé sur Kixster si l'utilisateur
  veut pouvoir les modifier sans redéploiement (voir ci-dessus, valeurs de repli déjà correctes sinon).
- Toujours pas traité (hors sujet de cette demande) : message spécifique "Bingo !"/détection de ligne
  (section 8), points par difficulté (section 1 étape 11), `Objective.condition` toujours pas interprété,
  action à prendre en cas d'abandon prolongé (`GameManager.hasAbandoned`, toujours du code mort — distinct
  de la fin de partie normale traitée ici), liste des parties Bingo existantes à rejoindre.

## 0.1.13 — abandon de partie, persistance (redémarrage/crash), fin de partie unifiée, pré-génération en cascade des maps

Quatre demandes explicites de l'utilisateur, 23/09/2026, une fois le bug de déploiement 1.10.3/1.10.4
côté kal-games corrigé (voir son JOURNAL.md) :

> « il faudrait que la partie se termine si personne n'est connecté dessus depuis 10 minutes. la partie
> doit continuer meme si le serveur est redémarré ou si il crash. à la fin d'une partie on clear les
> inventaires des joueurs, on leur redonne une netherstar pour qu'ils puissent retourner au hub kalgames
> (via un menu) et on efface les maps. […] »

> « a chaque partie il faut générer de nouvelles maps donc il serait préférable de générer les maps
> directement après la création de la partie et en cascade pour éviter les lags »

Quatre décisions ont été confirmées via `AskUserQuestion` avant l'implémentation (voir choix ci-dessous),
conformément à la règle du projet ("proposer avant d'implémenter en cas de doute").

**1. Fin de partie par abandon (aucun joueur connecté depuis 10 min)** — `GameManager.hasAnyConnectedPlayer`
+ `GameEndService` : un TROISIÈME déclencheur de fin de partie (en plus de timeout et victoire), suivi via
une nouvelle map `emptySince` (gameId → instant depuis lequel plus aucun joueur n'est connecté à aucune
instance de la partie), vérifiée à chaque `tick()`. Au-delà de `game.no-players-abandon-after-seconds`
(600s par défaut), `endByNoPlayers()` termine la partie SILENCIEUSEMENT (aucun joueur en ligne pour
recevoir un message ou une salle d'attente) : juste le nettoyage habituel (relais, mondes différés,
fichier de sauvegarde). Distinct de `GameManager.hasAbandoned` (suivi PAR JOUEUR, resté du code mort,
action jamais définie) : celui-ci suit la PARTIE entière et la termine réellement.

**2. Persistance (survie à un redémarrage/crash)** — nouveau package `fr.kalium.bingo.persistence`,
classe `GamePersistence` : sauvegarde un fichier YAML par partie EN COURS (`games/<gameId>.yml`, dossier
de données du plugin) — seed, durée, TEMPS RESTANT (pas l'instant de départ absolu), instances (équipe,
monde, joueurs), grille complète (chaque case : item/quantité/difficulté/condition) et progression par
équipe (indices de cases validées). Sauvegarde synchrone (fichier minuscule, même convention que
`plugin.saveConfig()` ailleurs dans ce projet) déclenchée : au lancement de la partie (`PartyStarter`),
à chaque nouvelle case validée (`ObjectiveValidationTask`), et périodiquement toutes les 60s en filet de
sécurité (`BingoPlugin`). Rechargée au démarrage (`GamePersistence.loadAll`, appelé depuis
`ServerLoadEvent` — même contrainte que `LobbySlots.init()`, création de monde interdite en phase
STARTUP) : les mondes d'instance sont RECHARGÉS depuis le disque (pas régénérés — `WorldCreator` charge
le dossier existant), la grille et la progression sont reconstruites telles quelles, et le fichier est
supprimé dès la fin réelle de la partie (`GameEndService.scheduleCleanup`) pour ne jamais restaurer une
partie déjà terminée.
- **Chronomètre : mis en PAUSE pendant l'arrêt** (choix confirmé via `AskUserQuestion`) — le temps
  restant sauvegardé est restauré tel quel (`BingoGame.restoreInProgress`, qui recalcule un `startedAt`
  fictif à partir de maintenant), quelle que soit la durée réelle de l'interruption. Une partie dont le
  temps était déjà écoulé AVANT l'arrêt ne se termine donc PAS immédiatement au redémarrage — elle
  reprend avec le temps qu'il lui restait à la dernière sauvegarde, et se terminera normalement au
  prochain `tick()` si ce temps est nul/négatif.
- **Portée volontairement limitée aux parties DÉJÀ LANCÉES** (choix confirmé via `AskUserQuestion`) : une
  salle d'attente pas encore lancée (choix des équipes en cours, `PartyManager`/`BingoParty`) N'EST PAS
  persistée — elle repart de zéro si le serveur redémarre avant le lancement de la partie.
- `GameManager.restoreGame` (+ record `RestoredInstance`) : reconstruit une `BingoGame` directement à
  l'état `IN_PROGRESS` sans repasser par `createGame` (pas de nouvelle seed, pas de vérification de
  `max-simultaneous-games` — la partie existait déjà avant l'arrêt).

**3. Fin de partie unifiée (timeout ET victoire)** — choix confirmé via `AskUserQuestion` : le timeout
utilise désormais EXACTEMENT le même traitement que la victoire (auparavant : renvoi direct vers
kal-games, sans salle d'attente). `GameEndService.endByTimeout`/`endByWin` délèguent tous les deux à une
nouvelle méthode commune `finishAndSendToLobby` (seul le message affiché diffère). Nouveautés dans ce
traitement commun :
- **Inventaire ENTIÈREMENT vidé** (`clearInventory` : contenu principal, armure, main secondaire) —
  demande explicite ("on clear les inventaires des joueurs"), remplace l'ancien retrait ciblé du seul
  papier "Objectifs" (`GameItems.remove`), qu'un vidage complet couvre déjà.
- **La nether star ouvre désormais un MENU** (`fr.kalium.bingo.gui.PostGameMenu`, un seul bouton "Retour
  à kal-games") au lieu de déclencher un départ immédiat sans confirmation — demande explicite : "une
  netherstar pour qu'ils puissent retourner au hub kalgames (via un menu)". `LobbyProtectionListener`
  ouvre ce menu si `GameEndService.isLingering`, `PartyMenu` sinon. `LobbyItems.givePostGame` donne le
  même objet verrouillé qu'en salle d'attente AVANT partie mais avec un nom/lore différent ("Partie
  terminée" / "Clic droit : retourner à kal-games").
- "Efface les maps" : déjà couvert par le nettoyage différé existant (`GameManager.cleanupGame` via
  `InstanceWorldManager.deleteInstanceWorld`), aucun changement nécessaire ici — confirmé que "maps"
  désigne bien les mondes d'instance (cohérent avec la demande suivante).

**4. Pré-génération en cascade des mondes d'instance** — nouvelle classe `fr.kalium.bingo.world.
InstanceWorldPreparer` : génère les mondes d'instance UN PAR UN, espacés de
`instances.pregeneration-stagger-seconds` (10s par défaut, choix confirmé via `AskUserQuestion`), DÈS LA
CRÉATION de la salle d'attente (`PartyManager.getOrCreate`, nouveau callback `onCreated` invoqué
uniquement à la création d'une NOUVELLE `BingoParty`, pas en cas de fusion) — le nom du monde et la seed
sont déjà connus à ce moment (`BingoParty.getTeamCount()`), sans dépendre du choix des équipes. Les
mondes prêts sont mis en cache (`ready`, monde → `World`) et RÉCLAMÉS par `GameManager.prepareInstances`
au lancement réel de la partie, avec repli sur la génération synchrone habituelle si l'hôte lance avant
la fin de la pré-génération (jamais bloquant). `PartyCanceller.cancel` annule/supprime les mondes déjà
pré-générés pour une partie annulée avant son lancement (jamais réclamés, sinon orphelins sur le disque).
Limite connue, non traitée (même catégorie que `InstanceWorldManager`) : ne tient pas compte de
`instances.max-simultaneous-games`, qui ne s'applique qu'au lancement réel.

**Nouvelles clés `config.yml`** : `game.no-players-abandon-after-seconds: 600` (section `game:`) et
`instances.pregeneration-stagger-seconds: 10` (section `instances:`) — **à ajouter manuellement dans le
`config.yml` déployé sur Kixster** (staleness de `saveDefaultConfig()`, voir plus haut), sinon valeurs de
repli déjà correctes dans le code, juste pas modifiables sans redéploiement en attendant.

**Nouveau dossier de données** : `plugins/KalBingo/games/` (créé automatiquement à la première
sauvegarde) — contient un `.yml` par partie EN COURS. Ne pas le supprimer manuellement pendant qu'une
partie tourne (perte de la persistance pour cette partie en cas de crash ultérieur) ; les fichiers de
parties déjà terminées sont normalement déjà nettoyés automatiquement.

## Pour reprendre la prochaine fois (mise à jour, 0.1.13)

- Une fois Kixster redémarré : tester les TROIS fins de partie — (1) timeout et (2) victoire, en
  vérifiant dans les deux cas l'inventaire vidé, la salle d'attente, le clic sur la nether star qui ouvre
  bien le nouveau menu (pas de départ immédiat), et l'expulsion après le délai ; (3) abandon — quitter le
  serveur avec tous les joueurs d'une partie en cours et vérifier (avec `no-players-abandon-after-seconds`
  raccourci pour tester) que la partie se termine bien après le délai.
- Tester la persistance : lancer une partie, valider quelques objectifs, PUIS redémarrer Kixster
  (`/stop` + relance, pas juste `/reload`) et vérifier que la partie reprend avec la même grille, la même
  progression, et le même temps restant qu'avant l'arrêt ; vérifier aussi qu'une reconnexion pendant
  cette fenêtre renvoie bien le joueur dans son instance.
- Tester la pré-génération en cascade : créer une partie à plusieurs équipes, observer dans les logs que
  les mondes se génèrent un par un (espacés du délai configuré) PENDANT le choix des équipes, avant même
  que l'hôte ne clique sur "Lancer la partie".
- Penser à ajouter manuellement `game.no-players-abandon-after-seconds: 600` et
  `instances.pregeneration-stagger-seconds: 10` dans le `config.yml` déployé sur Kixster (voir ci-dessus).
- Toujours pas traité : message spécifique "Bingo !"/détection de ligne (section 8), points par
  difficulté (section 1 étape 11), `Objective.condition` toujours pas interprété, liste des parties Bingo
  existantes à rejoindre.

## 0.1.14

**1. Seed déjà aléatoire — aucun correctif nécessaire.** Vérification demandée par l'utilisateur ("la
seed de génération de map doit changer à chaque fois de manière aléatoire") : `BingoPartyManager`
(côté KalGames) utilise déjà `SecureRandom.nextLong()` à CHAQUE création de partie (pas de cache, pas de
valeur codée en dur), et cette seed est propagée telle quelle jusqu'à `InstanceWorldManager.
createInstanceWorld` sans jamais être réutilisée entre deux parties. Si des maps identiques ont été
observées, la cause la plus probable était le bug de jar ambigu déjà corrigé précédemment (plugin non
redéployé) — pas un problème de génération de seed.

**2. Menu Objectifs réécrit en INVENTAIRE** (`fr.kalium.bingo.gui.GameMenu`, remplace l'ancien Dialog
texte) — demande explicite : "il serait préférable de visualiser la liste dans une page type
inventaire". Grille 5×5 centrée dans un inventaire 6 lignes/54 cases (`GRID_COL_OFFSET`), chaque case =
un objet représentant l'objectif (matériau + quantité = taille de pile). Menu strictement en LECTURE
SEULE (`fr.kalium.bingo.gui.GameMenuListener` annule tout clic/glisser-déposer) à l'exception de deux
objets verrouillés en dernière rangée :
- **Fermer** (Barrière, case en bas à droite, `GameMenu.CLOSE_SLOT`) — demande explicite : "un objet
  bloqué en bas à droite pour fermer la liste".
- **Abandonner la partie** (Porte en chêne, juste au-dessus, `GameMenu.ABANDON_SLOT`) — voir point 5.

**3. Infobulle par équipe au survol** — demande explicite : "quand on survole un objet on doit voir...
équipe A : complété (vert) / non complété (rouge), équipe B : ...". Implémenté via le lore natif de
l'`ItemStack` (aucun code de survol personnalisé nécessaire, le tooltip Minecraft s'en charge) : une
ligne par équipe, générée dynamiquement selon le nombre d'équipes réel de la partie.

**4. Effet brillant sur objectif validé (grille uniquement)** — demande explicite : "quand un objectif
de la grille a été rempli, alors il doit être brillant... comme pour les livres enchantés". Seule la
case validée PAR L'ÉQUIPE DU JOUEUR QUI REGARDE brille (logique : chaque joueur ne voit sa propre
progression briller, pas celle des autres équipes). Technique : `ItemStack.addUnsafeEnchantment(...)`
(contourne les vérifications "cet enchantement s'applique-t-il à cet objet") + `ItemFlag.HIDE_ENCHANTS`
sur l'`ItemMeta` → effet visuel brillant SANS aucun texte d'enchantement dans le tooltip, exactement le
rendu d'un livre enchanté.

**5. Système de score** — confirmé via `AskUserQuestion` (lignes uniquement, pas de colonnes/diagonales) :
`BingoGame.score(teamNumber)` = 1 point par objectif validé + 1 point par ligne entièrement validée
(5 lignes possibles max) + 5 points bonus si la grille entière est complétée. Purement informatif/
compétitif (affiché dans le HUD et l'infobulle) — NE CHANGE PAS la condition de victoire, qui reste
"première équipe à valider toute la grille" (`GameEndService.checkWin`, inchangé).

**6. Chronomètre + scores permanents au-dessus de la barre de vie** (nouvelle classe `fr.kalium.bingo.
game.GameHudService`, rafraîchie chaque seconde via `runTaskTimer`) — demande explicite : "le chrono de
la partie doit être affiché sur l'écran au dessus de la barre de vie, avec les scores actuels de chaque
équipe." Choix technique : action bar (`Player.sendActionBar`, PAS une boss bar) — se situe visuellement
juste au-dessus de la barre de vie/faim (contrairement à une boss bar, tout en haut de l'écran), et
réutilise le même mécanisme qu'un affichage persistant déjà utilisé côté KalGames (`RaceInstance`/
`GameInstance`).

**7. Abandon définitif d'une partie** — demande explicite : "un objet qui permet d'abandonner la
partie... êtes-vous sûr de vouloir abandonner ? (attention vous ne pourrez pas revenir)... oui/non".
- `fr.kalium.bingo.gui.AbandonConfirmMenu` : fenêtre de confirmation (Dialog, même mécanisme que les
  autres menus de confirmation du plugin) avec les deux boutons demandés — "Oui, abandonner" déclenche
  `AbandonService.abandon`, "Non, revenir en jeu" ne fait que fermer la fenêtre (le joueur reste en jeu,
  aucun état modifié).
- `fr.kalium.bingo.game.AbandonService` : marque le joueur comme abandonné sur SON instance
  (`BingoInstance.markAbandoned`, nouvel ensemble `abandoned` distinct de `playerConnected` — une simple
  déconnexion reste réversible, un abandon ne l'est JAMAIS, même en cas de reconnexion), retire l'objet
  "Objectifs", le renvoie sur kal-games (`AssignmentService.sendBackToKalGames`), prévient le relais
  (`RelayClient.clearActiveGame`, async) et vérifie si son équipe/la partie doit se terminer
  immédiatement (`GameEndService.checkImmediateAbandonment` — si plus AUCUN joueur actif sur la partie
  après cet abandon, inutile d'attendre les 10 minutes de `no-players-abandon-after-seconds`).
- Un joueur abandonné est exclu de `BingoInstance.hasAnyActivePlayer()`, `GameManager.findGameOf()` et
  `ObjectiveValidationTask.teamHasObjective()` — il n'est plus jamais traité comme "en jeu" même s'il se
  reconnecte au serveur, mais reste listé dans le roster de son équipe (informatif uniquement). Le reste
  de son équipe et les autres équipes continuent normalement.

**Aucune nouvelle clé `config.yml`** dans cette version — pas de patch manuel du `config.yml` déployé sur
Kixster nécessaire pour cette mise à jour.

## Pour reprendre la prochaine fois (mise à jour, 0.1.14)

- Une fois Kixster redémarré : ouvrir le menu Objectifs en jeu (objet papier) et vérifier l'affichage en
  inventaire (grille centrée, survol affichant le statut par équipe, case brillante après validation),
  puis cliquer sur "Fermer" (ferme sans effet) et vérifier qu'aucun objet ne peut être déplacé/retiré du
  menu.
- Vérifier l'affichage permanent du chronomètre + scores au-dessus de la barre de vie pendant une partie
  IN_PROGRESS.
- Tester l'abandon : cliquer sur "Abandonner la partie", répondre "Non" (doit rester en jeu sans rien
  changer), puis répondre "Oui" (doit renvoyer vers kal-games, l'objectif ne doit plus compter pour son
  équipe, et une reconnexion ne doit PAS le remettre dans la partie). Tester aussi le cas où le dernier
  joueur actif d'une partie abandonne : la partie doit se terminer immédiatement (pas attendre 10 min).
- Vérifier le calcul des scores (1 pt/objectif + 1 pt/ligne + 5 pts bonus grille complète) dans le HUD et
  l'infobulle de survol.
- Confirmé (pas de correctif nécessaire) : la seed de génération change bien aléatoirement à chaque
  partie — voir point 1 ci-dessus.
- Toujours pas traité : message spécifique "Bingo !"/détection de ligne à l'écran (au-delà du score,
  section 8), points par difficulté (section 1 étape 11), `Objective.condition` toujours pas interprété.
- **Nouvelle demande reçue, pas encore investiguée** : "il faut pouvoir voir les parties qui sont créées
  et encore en salle d'attente dans le menu kalgames de manière à pouvoir la rejoindre facilement" — côté
  KalGames cette fois (pas KalBingo). À investiguer : `PlayerMenus.openBingoCreate`,
  `BingoPartyManager`, `BingoNetworkListener` pour voir si KalGames garde déjà trace des parties encore
  en formation, puis proposer une approche (visibilité : toutes les parties ou seulement certaines ?
  affectation d'équipe automatique ou manuelle en rejoignant ? limite du nombre de parties affichées ?)
  avant d'implémenter, conformément à la règle du projet.

## 0.1.15

**Moitié KalBingo de la demande ci-dessus** (voir JOURNAL.md de KalGames, 1.10.5, pour la moitié
kal-games — les deux vont ensemble, l'une ne fonctionne pas sans l'autre). Investigation : côté
kal-games, `BingoPartyManager` gardait déjà TOUTES les infos nécessaires pour lister une partie (hôte,
code, équipes/taille, joueurs inscrits) — la seule chose manquante était de savoir QUAND une partie
encore listée là-bas avait en réalité déjà démarré ou été annulée sur Kixster, pour la retirer de la
liste au bon moment. Confirmé via `AskUserQuestion` : ajouter un signal réseau pour ça plutôt qu'une
liste approximative (recommandé, retenu).

- **`fr.kalium.bingo.network.PartyStatusNotifier`** (NOUVEAU) : prévient kal-games qu'une partie en
  salle d'attente (pas encore lancée) vient de disparaître, via un `Forward` BungeeCord/Velocity —
  même mécanisme que `AssignmentService`/`BingoNetworkListener` déjà en place pour l'affectation des
  joueurs (nouveau sous-canal `KalBingoPartyClosed`, transporte juste le `gameId`). Pas besoin du
  relais HTTP ici (contrairement à l'affectation) : ce signal part TOUJOURS d'un joueur réellement
  connecté à Bingo au moment de l'appel (l'hôte qui vient de cliquer sur "Lancer"/"Annuler"), donc
  toujours livrable sans filet de sécurité supplémentaire.
- **`fr.kalium.bingo.game.PartyStarter.start()`** : capture un joueur en ligne pendant la téléportation
  des équipes (`carrier`), puis appelle `partyStatusNotifier.notifyClosed(gameId, carrier)` juste après
  `partyManager.remove(gameId)`.
- **`fr.kalium.bingo.game.PartyCanceller.cancel()`** : même principe — cherche un joueur encore connecté
  dans le roster AVANT de les renvoyer vers kal-games, appelle `notifyClosed` une fois.
- **`BingoPlugin.java`** : construit `PartyStatusNotifier` (réutilise `kalGamesServerName` déjà
  disponible dans `onEnable`), l'injecte dans `PartyStarter` et `PartyCanceller`.

**Aucune nouvelle clé `config.yml`** — `kalGamesServerName` (network.kal-games-server-name) était déjà
configuré et suffit.

**Déployé le 24/09/2026 via WinSCP**, en même temps que KalGames 1.10.5 (voir son JOURNAL.md pour le
plan de test complet des deux côtés — la fonctionnalité ne peut être testée qu'avec les deux plugins à
jour et les deux serveurs (kal-games ET Kixster) redémarrés).

## 0.1.16

**Correctif rapporté par l'utilisateur** : "lorsque une partie est en cours, un joueur abandonne la
partie, ensuite il essaye de rejoindre la partie et ça le remet en salle d'attente. on dirait que
l'info du démarrage de la partie n'est pas arrivée sur KalGames".

Cause probable : le signal `PartyStatusNotifier` (0.1.15) prévient bien kal-games qu'une partie a
démarré, mais rien ne garantissait qu'il arrive TOUJOURS (message réseau perdu, ou l'un des deux
serveurs pas encore redémarré avec la bonne version au moment du test). Si kal-games pense encore
qu'une partie est "en salle d'attente" alors qu'elle a réellement démarré ici, un joueur qui la
rejoint (bouton de la liste, ou `/bingo join <code>`) est renvoyé vers Bingo avec ce `gameId` déjà
actif - et `PartyManager.getOrCreate` (voir 0.1.13/InstanceWorldPreparer) ne sait pas faire la
différence entre "partie jamais vue" et "partie déjà démarrée" : il créait une salle d'attente
FANTÔME, vide, jamais réclamée par personne (exactement le symptôme observé - "ça le remet en salle
d'attente").

- **`fr.kalium.bingo.network.AssignmentService`** : constructeur reçoit désormais `GameManager` en
  plus de `PartyManager`. Dans `handleResponse`, AVANT d'appeler `partyManager.getOrCreate(...)`, un
  garde-fou vérifie `gameManager.getGame(gameId) != null` — si cette partie est déjà une partie ACTIVE
  connue ici, on refuse de créer une salle d'attente fantôme : le joueur reçoit un message ("cette
  partie a déjà démarré, vous ne pouvez plus la rejoindre depuis kal-games") et est renvoyé vers
  kal-games au lieu d'être laissé dans une salle vide qui ne deviendra jamais une vraie partie. Ce
  correctif protège contre ce symptôme précis QUEL QUE SOIT la cause exacte de la perte du signal de
  fermeture (fiabilité réseau ponctuelle, ou serveurs pas encore synchronisés) - défense en profondeur
  plutôt qu'une dépendance à 100% sur `PartyStatusNotifier`.
- **`BingoPlugin.java`** : passe `gameManager` (déjà construit plus tôt dans `onEnable`) au
  constructeur d'`AssignmentService`.

**Limite connue, non traitée** : ce garde-fou ne couvre que le cas "partie encore active ici". Une
partie déjà TERMINÉE ET NETTOYÉE (`GameManager.cleanupGame`, plusieurs minutes après la fin) n'est
plus non plus connue de `GameManager` - dans ce cas (très improbable en pratique : il faudrait que
kal-games propose encore le même vieux code ET que le joueur clique dessus après la fin complète de
la partie), le même symptôme fantôme pourrait théoriquement se reproduire. Pas traité ici (pas
demandé, cas marginal) ; si ça se reproduit en pratique, la vraie source à vérifier en premier reste
si les DEUX serveurs (kal-games ET Kixster) tournent bien avec les dernières versions déployées.

**Voir aussi JOURNAL.md de KalGames (1.10.6)** pour le correctif lié : la capacité affichée/appliquée
d'une partie utilisait le plafond global au lieu de la config choisie par l'hôte.

**Déployé le 24/09/2026 via WinSCP**, en même temps que KalGames 1.10.6.

## 0.1.17

**Demande explicite de l'utilisateur** : "il faut ajouter un délai de: [10 secondes*le nombre
d'équipe] entre l'arrivée dans la salle d'attente et le début de la partie. si le createur de la
partie lance la partie avant, alors un chrono s'affiche : début de la partie dans : [temps
restant]".

But : laisser le temps à la pré-génération en cascade des mondes (voir `InstanceWorldPreparer`,
`instances.pregeneration-stagger-seconds`) de finir avant que la partie ne démarre réellement,
plutôt que de risquer un démarrage sur des mondes pas encore prêts.

Deux questions posées à l'utilisateur avant implémentation (règle du projet : proposer avant de
faire en cas d'ambiguïté) :
- Un opérateur qui force le démarrage via `/bingoadmin start` doit-il pouvoir contourner ce délai ?
  → Réponse : non, il respecte le même délai que l'hôte.
- Affichage du chronomètre : action bar (au-dessus de la barre de vie, même mécanisme que
  `GameHudService`) ou boss bar ? → Réponse : action bar.

- **`fr.kalium.bingo.game.BingoParty`** : nouveau champ `createdAt` (`Instant.now()` à la
  construction) + getter `getCreatedAt()` — sert de référence pour calculer le délai écoulé.
- **`fr.kalium.bingo.game.PartyCountdownService`** (nouveau) : calcule le délai minimum
  (`10s * party.getTeamCount()`), expose `isReady(party)` / `remaining(party)`, et
  `scheduleStart(party, onReady)` qui programme un rappel automatique de `onReady` une fois le délai
  écoulé tout en diffusant un chronomètre en action bar toutes les secondes ("Début de la partie
  dans : Xs") à tous les joueurs connectés de la partie. `cancel(gameId)` arrête un compte à rebours
  en cours (partie annulée entre-temps). Un seul compte à rebours actif par partie — un second appel
  à `scheduleStart` pendant qu'un compte à rebours est déjà en cours ne fait rien de plus (évite de
  le relancer à chaque clic sur "Lancer la partie").
- **`fr.kalium.bingo.game.PartyStarter`** : `Result` devient `(game, failure, message, scheduled)`
  avec des fabriques statiques `immediate()` / `scheduled()` / `failure()` — plus clair qu'un simple
  booléen `ok()` pour distinguer "partie lancée", "compte à rebours programmé (pas un échec)" et
  "échec réel". `start()` vérifie désormais `countdownService.isReady(party)` avant de créer la
  partie : si le délai n'est pas écoulé, programme le démarrage automatique (`scheduleStart` rappelle
  `start(gameId)` lui-même une fois prêt) et renvoie `Result.scheduled(...)` avec le temps restant au
  lieu d'échouer. Nettoyage défensif de `countdownService.cancel(gameId)` juste avant le retour
  réussi final.
- **`fr.kalium.bingo.game.PartyCanceller`** : appelle désormais `countdownService.cancel(gameId)` —
  sinon un compte à rebours en cours déclencherait `start()` sur une partie qui vient d'être annulée.
- **`fr.kalium.bingo.gui.PartyMenu.launch()`** et **`command.BingoAdminCommand`** (`/bingoadmin
  start`) : gèrent désormais le cas `result.scheduled()` AVANT `result.ok()` (sinon `result.game()`
  serait `null` et provoquerait une erreur) — affichent simplement le message du compte à rebours.
- **`BingoPlugin.java`** : construit `PartyCountdownService` (ne dépend que du plugin lui-même) juste
  avant `PartyStarter`/`PartyCanceller`, l'injecte dans les deux.

**Aucune nouvelle clé `config.yml`** — formule fixe (10s * nombre d'équipes) telle que demandée
littéralement par l'utilisateur, cohérente avec le principe déjà suivi pour `PartyStatusNotifier` et
`BingoParty.maxPlayers()` (pas de configuration ajoutée sans demande explicite).

**KalBingo uniquement** — fonctionnalité entièrement contenue dans la salle d'attente du serveur
Bingo, aucun changement côté KalGames nécessaire.

**Déployé le 23/09/2026 via WinSCP sur Kixster.**

## 0.1.18

**Demandes explicites de l'utilisateur** (retour de test de la 0.1.17) :
- "si tous les membres d'une équipe ont abandonné, fin de la partie" ;
- "les maps ne sont toujours pas générées à l'avance, il faut que la génération des maps commence
  dès l'arrivée dans la salle d'attente" ;
- "l'endroit où les joueurs apparaissent sur la map doit devenir leur point de spawn (actuellement si
  on meurt on est renvoyé sur le modèle de la salle d'attente). à la fin d'une partie ou si un joueur
  abandonne il faut clear son inventaire et supprimer son point de spawn".

Précisions obtenues via AskUserQuestion : la partie ne s'arrête que si l'abandon complet d'une équipe
ne laisse plus qu'**une seule équipe en jeu** (4 équipes dont 1 abandonne : les 3 autres continuent),
message **neutre** (pas de gagnant déclaré) ; rayon de pré-génération **~200 blocs**.

"Pas de délai avant le début de la partie" (0.1.17) : vérifié dans le log de Kixster - 0.1.17 bien
chargée, partie à 2 équipes créée à 18:00:46, les 2 mondes prêts à 18:01:13, partie ensuite jouée.
Délai de 20 s (10 s x 2 équipes) compté depuis l'arrivée : très probablement appliqué mais court et
peu visible. Aucun bug trouvé, rien changé.

- **Terrain pré-généré** (`world.InstanceWorldPreparer`) : créer le monde ne génère que la petite zone
  de spawn, le reste était généré à la volée pendant l'exploration (le lag signalé). Dès qu'un monde
  est créé (donc pendant la salle d'attente), les chunks dans un rayon de
  `instances.pregeneration-radius-blocks` (200 par défaut, nouvelle clé, 0 = désactivé) autour de son
  spawn sont générés du centre vers l'extérieur, en asynchrone (`getChunkAtAsync` de Paper), 4 à la
  fois maximum. Log au début et à la fin ("Terrain de '...' pré-généré (N chunks en X s)") pour pouvoir
  mesurer. S'arrête si la partie est annulée. Un monde créé au lancement faute d'être prêt à temps
  (`GameManager.prepareInstances`) est pré-généré de la même façon, en arrière-plan.
- **Point de spawn** (`game.PlayerResetService`, nouveau) : au lancement (`PartyStarter`), le point
  d'apparition sur la map devient le point de spawn du joueur (`setRespawnLocation(..., force=true)`,
  sauvegardé avec les données du joueur, survit à une déconnexion/redémarrage). Posé aussi à la
  reconnexion en cours de partie si besoin (joueur hors ligne au lancement), sans écraser un lit posé
  sur sa propre map. Filet de sécurité `game.GameRespawnListener` : un joueur en partie qui
  réapparaîtrait ailleurs que sur SA map (ex : lit détruit -> le jeu le renverrait sur le monde par
  défaut, c'est-à-dire le modèle de salle d'attente) est renvoyé sur le point d'apparition de sa map.
- **Remise à zéro** (inventaire ENTIÈREMENT vidé + point de spawn supprimé) : à l'abandon
  (`AbandonService`, remplace l'ancien retrait du seul papier "Objectifs") et à toute fin de partie
  (`GameEndService` : victoire, temps écoulé, abandon d'équipe, plus personne connecté). Un joueur
  **hors ligne** au moment de la fin est noté dans `plugins/KalBingo/pending-resets.yml` et remis à
  zéro à sa prochaine connexion à Kixster (`PlayerConnectListener`, avant tout le reste) - sinon il
  retrouverait son ancien inventaire à la partie suivante.
- **Fin si une équipe abandonne** (`GameEndService.checkTeamAbandonment`, appelé par
  `AbandonService` après chaque abandon) : si une équipe est entièrement abandonnée
  (`BingoInstance.isFullyAbandoned`, une simple déconnexion ne compte pas) et qu'il ne reste plus
  qu'une équipe en jeu -> fin normale (salle d'attente post-partie, nether star de retour) avec
  "L'équipe X a entièrement abandonné. La partie est terminée." Partie à une seule équipe : inchangé
  (c'est `checkImmediateAbandonment` qui la termine, personne à prévenir).
- `GameEndService.finishAndSendToLobby` ignore désormais les joueurs qui ont abandonné (déjà remis à
  zéro et renvoyés vers kal-games : ne pas les rapatrier dans la salle d'attente), et libère
  l'emplacement de salle d'attente si personne n'y a été placé (sinon il restait réservé pour rien -
  défaut latent, qui aurait été déclenché plus souvent par la nouvelle fin sur abandon d'équipe).

**KalBingo uniquement**, aucun changement côté KalGames.

**Déployé le 23/09/2026 via WinSCP sur Kixster** (0.1.17 archivée dans `_removed-kalbingo-0.1.17/`). Redémarrage de Kixster requis.

## 0.1.19

**Demandes explicites de l'utilisateur** :
- "les parties qui sont terminées ne doivent plus apparaître dans la liste des parties sur le menu de
  KalGames" ;
- "au début de la partie la vie et la nourriture des joueurs doit être remontée à fond" ;
- "il faut ajouter un effet de regen dans la salle d'attente" ;
- "il faut ajouter un kit de départ aux joueurs quand la partie commence : 8 steak cuits, pioche en
  pierre, hache en pierre, houe en pierre, pelle en pierre, épée en pierre".

- **Parties terminées encore listées sur kal-games - cause** : `PartyStatusNotifier` (0.1.15)
  n'envoyait le signal "partie fermée" que par le canal Forward, en supposant qu'il était toujours
  livrable parce qu'il part d'un joueur connecté sur Kixster. Faux : un Forward n'est livré que si au
  moins un joueur est connecté sur le serveur CIBLE (kal-games). Au démarrage d'une partie, tout le
  monde est en général sur Kixster : kal-games est vide, le signal était perdu, la partie restait
  listée pour toujours.
  **Correctif** : le signal est aussi publié sur le relais HTTP (`RelayClient.postPartyClosed`, clé
  `party-closed-<gameId>` sur l'endpoint générique `/assignment/` de KaliumRelay - aucun changement
  côté proxy), et republié toutes les minutes pendant 6 h (une entrée du relais expire en 2 min : si
  kal-games ne l'a pas lue entre-temps, par ex. serveur vide mis en pause, elle revient). kal-games
  l'interroge toutes les 5 s (KalGames 1.10.7). Le Forward est gardé (instantané quand kal-games a du
  monde). `PartyCanceller` publie désormais même sans aucun joueur connecté (le relais suffit).
- **Début de partie** (`game.StarterKit`, appelé par `PartyStarter` pour chaque joueur téléporté) :
  inventaire vidé, vie au maximum, faim et saturation au maximum (+ feu éteint), puis kit en barre
  rapide : épée (0), pioche (1), hache (2), pelle (3), houe (4), 8 steaks cuits (5). Emplacement 8
  inchangé (papier "Objectifs"). Inventaire vidé AVANT le kit pour que tout le monde parte avec la
  même chose.
- **Régénération en salle d'attente** (`world.LobbyRegenTask`, chaque seconde) : Régénération I sans
  limite de durée, sans particules, pour tout joueur dans le monde des salles d'attente
  (`lobby.world-name`, avant ET après une partie) ; retirée dès qu'il en sort.

**Voir aussi KalGames 1.10.7** (lecture du signal sur le relais). Les deux plugins doivent être à
jour ; redémarrage de Kixster ET de kal-games requis.

**Déployé le 23/09/2026 via WinSCP** (0.1.18 archivée dans `_removed-kalbingo-0.1.18/`), avec KalGames 1.10.7.

## 0.1.20

**Demandes explicites de l'utilisateur** : "quand tu as protégé la salle d'attente contre la casse de
bloc, le dépôt de bloc etc... il fallait bloquer que pour les salles d'attente, pas le modèle. il faut
débloquer le modèle sinon je ne peux pas le modifier. assure-toi que lorsqu'un modèle est recapturé,
les salles d'attente déjà générées doivent se supprimer".

- **Protection limitée aux salles d'attente** (`world.LobbyProtectionListener`) : la protection
  s'appliquait à TOUT le monde `bingo_lobby`, donc aussi au modèle de l'admin, impossible à modifier.
  La casse/pose de blocs, les seaux et l'interaction avec les blocs ne sont désormais bloqués que dans
  la zone des salles collées (`LobbySlots.isInSlotArea` : tous les emplacements + une marge de
  `lobby.spacing / 2` autour, sur toute la hauteur - avec la config actuelle, X de ~1850 à ~3100). Le
  modèle, construit près de l'origine du monde (loin des emplacements grâce à
  `lobby.slots-start-offset`), est de nouveau modifiable. Le blocage des monstres et du PvP reste
  valable dans tout le monde (n'empêche pas de modifier le modèle). La nether star reste verrouillée.
- **Recapture = anciennes salles supprimées** (`LobbyCaptureService.capture`,
  `LobbySlots.clearAllSlots`, `LobbyTemplateService.clear`) : avant, le nouveau modèle était collé
  PAR-DESSUS les anciennes salles, et tout ce qui dépassait de l'ancien modèle restait en place.
  Désormais, la zone de l'ancien modèle est vidée (air) sur chaque emplacement, puis le nouveau est
  collé. Limite : seuls les emplacements actuellement configurés (`instances.max-simultaneous-games`)
  sont nettoyés - si ce nombre a été réduit depuis le dernier collage, les emplacements en trop ne
  sont pas effacés.

**KalBingo uniquement.**

**Déployé le 23/09/2026 via WinSCP sur Kixster** (0.1.19 archivée dans `_removed-kalbingo-0.1.19/`).

## 0.1.21 — Salle d'attente : capture jusqu'à 256 blocs par côté

**Demande explicite de l'utilisateur** : "augmente la taille maximale de capture de la salle d'attente du
serveur Bingo. actuellement 64 bloc par coté, je souhaiterais 256 bloc par coté".

- `lobby.max-size` : 256 par défaut (le code plafonne à 256). Le config.yml DÉPLOYÉ contenait `max-size: 64` :
  modifié à la main sur Kixster (256).
- Une salle de 256 × 256 × 256 = 16,7 millions de blocs, collée sur 4 emplacements : faite d'un seul coup comme
  avant, la capture / l'effacement / le collage auraient bloqué le serveur (arrêt par le watchdog, comme sur
  kal-games le 23/09). Donc, dans `LobbyTemplateService` :
  - capture, effacement des anciennes salles et collage se font petit à petit, chunk par chunk (chunks chargés
    en arrière-plan), avec un budget par tick : nouveau réglage `lobby.blocks-per-tick` (30 000 par défaut,
    1 par bloc lu, 4 par bloc modifié). Une opération à la fois (file d'attente) ;
  - collage : un bloc n'est modifié que s'il diffère du modèle ;
  - capture refusée si une capture / mise à jour des salles est déjà en cours ; messages au joueur au début,
    à la fin de la capture puis à la fin du collage sur tous les emplacements ;
  - au démarrage du serveur, le recollage des salles est lui aussi progressif.
- `LobbyTemplate` : blocs stockés en palette + indice 16 bits (2 octets par bloc au lieu d'une chaîne), fichier
  `lobby_template.kgt` binaire compressé. L'ancien format texte reste lisible (converti à la prochaine capture).
- Limite : `lobby.spacing` (300) doit rester supérieur à la taille de la salle.

**Déployé le 23/09/2026 via WinSCP sur Kixster** (0.1.20 et l'ancien config.yml archivés dans `_removed-kalbingo-0.1.20/`).

## 0.1.22 — Nether et End propres à chaque équipe, keepInventory

**Demandes explicites de l'utilisateur** : "pour bingo, le nether et l'end doivent être généré comme
l'overworld (chacun le sien)" ; "il faut activer le keep inventory pour les joueurs du Bingo lorsqu'ils sont
dans une partie et dans les 3 dimensions".

Avant : un portail pris dans une map de partie menait au Nether / à l'End PAR DÉFAUT du serveur (`the_nether`,
`the_end`), partagés par toutes les équipes et toutes les parties.

- **Mondes** (`InstanceWorldManager`) : chaque équipe a `bingo_<partie>_<n>_nether` et `bingo_<partie>_<n>_the_end`,
  même seed que son overworld. `deleteInstanceWorld` supprime les trois ; une partie restaurée après
  redémarrage recharge aussi son Nether / End s'ils existent.
- **Pré-génération** (`InstanceWorldPreparer`) : la cascade crée d'abord les overworlds de toutes les équipes
  (indispensables au lancement), puis les Nether, puis les End, un monde toutes les
  `instances.pregeneration-stagger-seconds`. Terrain pré-généré avec le même rayon
  (`instances.pregeneration-radius-blocks`) : Nether autour de l'équivalent (÷ 8) du spawn, End autour de
  l'île principale. La cascade continue après le lancement et s'arrête à la fin de la partie
  (`GameManager.cleanupGame` → `cancel`). Si un joueur prend un portail avant que son Nether / End soit prêt,
  il est généré à ce moment-là (quelques secondes de blocage, comme la génération de repli d'un overworld).
- **Portails** (nouveau `DimensionPortalListener`) : Nether ↔ overworld de l'équipe (coordonnées ÷ 8 / × 8, le
  serveur retrouve ou construit le portail) ; portail de l'End → plateforme d'obsidienne (100, 49, 0) de l'End
  de l'équipe ; sortie de l'End → point de réapparition du joueur s'il est sur les mondes de son équipe, sinon
  le point d'apparition de la partie. Objets et créatures suivent les mêmes règles.
- **Réapparition** (`GameRespawnListener`, `PlayerConnectListener`) : un point de réapparition dans le Nether ou
  l'End de l'équipe (ancre de réapparition) est respecté.
- **keepInventory** activé sur les 3 mondes de chaque équipe (règle de jeu du monde : seuls les joueurs en partie
  y sont ; la salle d'attente n'est pas concernée).

**Bug corrigé au passage (constaté pendant ce travail)** : sur Paper 26.x les mondes ajoutés sont rangés dans
`Kixster SMP/dimensions/minecraft/<nom>` et non à la racine du serveur ; la suppression des maps en fin de partie
cherchait à la racine et n'effaçait donc rien. Elle cherche maintenant aux deux endroits (garde-fou : seuls les
noms de mondes de partie `..._<uuid>_<n>[_nether|_the_end]` peuvent être supprimés). Les 18 anciens dossiers
`bingo_...` déjà présents (parties terminées avant cette version) n'ont PAS été touchés : à supprimer par
l'utilisateur s'il le souhaite.

**Déployé le 23/09/2026 via WinSCP sur Kixster** (0.1.21 archivée dans `_removed-kalbingo-0.1.21/`).

## 0.1.23 — jeton du relais retiré du config.yml fourni (24/09/2026)

**Demande de LeKiwi06** : ne plus jamais publier le jeton du relais dans le dépôt public (voir
`KaliumRelay/JOURNAL.md`, 1.1.1, et `REGLES.md`, section 2).

- `config.yml` fourni avec le plugin : `network.relay-token: ""` (au lieu de l'ancien jeton fixe) et commentaire
  « à renseigner uniquement dans le config.yml du serveur ». Aucun changement de code.
- Le `config.yml` déployé sur Kixster n'est pas concerné (le plugin ne réécrit jamais un fichier existant) : il
  contient déjà le nouveau jeton, mis à la main le 24/09/2026.

À déployer avec KaliumRelay 1.1.1 et KalGames 1.12.4 - pas urgent. Aussi dans cette version (même jour, demande de LeKiwi06) : commentaires du code et de `config.yml` /
`objectives.yml` qui décrivaient des fonctions « pas encore implémentées » alors qu'elles existent (validation,
fin de partie...) corrigés ; titre « État actuel » en tête de ce journal renommé en « État au 22/09/2026 ».
Aucun changement de comportement.

**Statut : compilé, non déployé.**

## 0.2.0 — renommé KG_BingoGame (24/09/2026)

**Demande de LeKiwi06** : « renommer Kalbingo (tout ce que le serveur exécute pour concrètement jouer la partie) en
KG_BingoGame, pour coller au nouveau format de noms ». Étape B du découpage du Bingo (étape A : KG_Bingo, voir son
JOURNAL). Décision : noms internes du code conservés (`fr.kalium.bingo`, classe principale `BingoPlugin`).

- `plugin.yml` : `name: KG_BingoGame` ; dossier du dépôt `KalBingo/` → `KG_BingoGame/` ; jar `KG_BingoGame-0.2.0.jar`.
- Messages de la console : préfixe `[KalBingo]` → `[KG_BingoGame]` ; commentaires mis à jour.
- Vérifié : code compilé identique à 0.1.22 hormis le nom du plugin. Inclut 0.1.23 (jamais déployée).

**Ce que le changement de nom implique sur Kixster** (à faire serveur ARRÊTÉ, sans partie en cours) :
- le dossier de données devient `plugins/KG_BingoGame/` : y déplacer tout le contenu de `plugins/KalBingo/`
  (`config.yml`, `objectives.yml`, `lobby_template.kgt`, `games/`, `pending-resets.yml`), sinon la salle d'attente,
  les objectifs et les réglages (dont le jeton du relais) sont perdus ;
- `bukkit.yml` (racine du serveur) : `generator: KalBingo` → `generator: KG_BingoGame`, sinon le monde principal
  vide (`Kixster SMP`) n'a plus son générateur et du terrain normal peut y apparaître ;
- les objets marqués par le plugin (Nether Star de la salle d'attente, papier « Objectifs ») changent d'étiquette
  interne : un objet de l'ancienne version encore dans un inventaire ne serait plus reconnu. D'où : aucune partie en
  cours au moment du déploiement.

**Procédure de déploiement** (Kixster arrêté) :
1. Déplacer `/plugins/KalBingo-0.1.22.jar` dans `/plugins/_removed-kalbingo-0.1.22/`, et y copier aussi le dossier
   `/plugins/KalBingo/` (copie de sauvegarde) et `/bukkit.yml`.
2. Renommer `/plugins/KalBingo/` en `/plugins/KG_BingoGame/`.
3. Envoyer `KG_BingoGame-0.2.0.jar` dans `/plugins/`.
4. Dans `/bukkit.yml`, remplacer `generator: KalBingo` par `generator: KG_BingoGame`.
5. Démarrer Kixster ; dans `logs/latest.log`, vérifier l'absence de « Could not set generator » et la ligne
   « [KG_BingoGame] Plugin active ».

À déployer avec KaliumRelay 1.1.1, KalGames 1.13.0 et KG_Bingo 1.0.0. **Statut : compilé, non déployé.**

## 0.3.0 — nouveau Bingo : barème de points, modes, nulle, inactivité (24/09/2026)

**Demande de LeKiwi06** (cahier des charges du 24/09, précisé au fil de la conversation). Décisions : multiplicateurs
**additifs** ; points en **temps réel** avec annonces ; la victoire va à la 1re équipe qui atteint son objectif.

- **Liste d'objectifs validée** (`objectives.yml`) : 100 faciles, 50 normaux, 25 difficiles, 25 extrêmes.
- **Modes** (`BingoSettings`, choisis dans le menu de création de KG_Bingo 1.1.0) :
  - *Bingos* : lignes, colonnes et diagonales au choix, 3 à 12 à achever ; la 1re équipe qui y parvient gagne ;
    chrono : à la fin du temps, le meilleur score gagne, meilleurs scores égaux = **« Égalité »** (compte comme une
    nulle pour les points) ;
  - *Blackout* : sans chrono, la 1re équipe qui remplit la grille gagne ; nulle proposée automatiquement à chaque
    heure de jeu ;
  - composition de la grille par difficulté (25 cases) ; défaut 10 F / 10 N / 5 D / 0 X.
- **Barème** (`score/ScoreEngine`, valeurs dans `Difficulty`) : 1/3/5/10 ; 1re équipe sur un objectif +0/+1/+2/+3 ;
  1re équipe à achever un bingo +0,5 au coefficient de ses cases, bingo uniquement difficile/extrême +0,5 pour toute
  équipe ; victoire +1/+2/+3/+5 par objectif ; bonus ajoutés avant les coefficients ; classement cumulé en cas de
  victoire (points + ceux des équipes derrière). Points solo : objectifs validés par le joueur + gain complet des
  bingos auxquels il a participé (+ bonus de victoire sur ses objectifs) ; classement solo avec les MÊMES règles que
  les équipes (victoire : joueurs gagnants en tête, abandons en dernier, points + ceux des joueurs derrière ; nulle
  ou égalité : points propres). Joueur seul dans son équipe : pas de pseudo dans les annonces ni l'info-bulle.
  Testé hors serveur (11 vérifications).
- **Annonces** à tous les joueurs de la partie : objectif validé (en 1er, pseudo, points), bingo (participants, points).
  Classements d'équipe et solo affichés en fin de partie.
- **Info-bulle** des objectifs : nom français, difficulté et valeur ; par équipe : pseudo, bonus de 1re, coefficient,
  points ; bingos passant par la case affichés seulement s'ils ont été achevés.
- **Nulle** (`DrawVoteService`, bouton du menu Objectifs, `/bingonulle proposer|oui|non`) : vote de l'équipe du
  proposant puis de toutes les autres (majorité interne, **égalité de votes = oui**), 3 min, délai global de 30 min ;
  nulle = chaque équipe garde ses propres points. Proposée aussi quand une équipe abandonne.
- **Abandon** : déconnecté 10 min (`game.disconnect-abandon-seconds`) = abandon définitif ; équipe entièrement
  abandonnée = perd, classée dernière ; s'il ne reste qu'une équipe, la partie s'arrête et elle vote la nulle (sinon
  elle gagne).
- **Inactivité** (`InactivityService`) : 5 min sans action (`game.inactivity-kick-seconds`) = expulsion, puis compte
  à rebours de déconnexion.
- **Invincibilité** entre la fin de partie et le départ de la salle d'attente (dégâts et faim annulés, feu / chute /
  effets retirés avant la téléportation).
- **keepInventory** garanti à la mort d'un joueur en partie (plus seulement par la règle du monde, qui ne s'était pas
  appliquée en test) ; nouvelle API `GameRules.KEEP_INVENTORY`.
- **Papier Objectifs** : ne peut plus quitter l'inventaire du joueur (artisanat, coffres, villageois, cadres...),
  jamais lâché à la mort ; il ne compte plus pour l'objectif PAPER (bug).
- Supprimée : clé `reconnect-during-game.abandon-after-seconds` (remplacée par `game.disconnect-abandon-seconds`).
- **Nether commun vu en test : pas un bug de la 0.1.22.** Les journaux de Kixster montrent que cette partie (23/09,
  21 h 50 – 22 h 52) tournait encore avec la **0.1.20**, d'avant les Nether / End par équipe et le keepInventory ; la
  0.1.22 a été déposée à 23 h 15. La seule partie jouée en 0.1.22 (24/09, 3 h 35, test du jeton) a bien généré un
  Nether et un End par équipe (`..._1_nether`, `..._2_the_end`...), mais s'est terminée au bout d'une minute : les
  portails n'ont jamais été essayés. À tester : Nether et End de chaque équipe, et keepInventory.

**Déploiement** : avec KG_Bingo 1.1.0 (et les versions en attente : KaliumRelay 1.1.1, KalGames 1.13.0). Suivre la
procédure de migration de la 0.2.0 ci-dessus (jamais déployée), avec en plus : déplacer l'ancien
`plugins/KG_BingoGame/objectives.yml` dans le dossier `_removed-...` pour que la nouvelle liste soit installée.
Aucune partie en cours. **Statut : déployé sur Kixster le 24/09/2026 (migration faite, journal de démarrage OK : 200 objectifs), non testé en jeu.**

## 0.3.1 — correctifs après le 1er test (24/09/2026)

Test de LeKiwi06 avec 2 comptes : annonces, coefficients, Nether / End par équipe, nulle en solo et invincibilité
de fin de partie OK.
- **Points solo** : le bonus de victoire n'était pas multiplié par les coefficients des bingos dans les points
  solo (un joueur seul avait 91 en solo pour 101 en équipe). Corrigé : un joueur seul a exactement les points de son
  équipe (vérifié hors serveur, 13 vérifications).
- **Modes de jeu** (demande de LeKiwi06) : aventure dans la salle d'attente (avant et après la partie), survie sur la
  map (lancement et reconnexion).

À tester plus tard : pseudos dans les annonces à plus de 2 comptes, nulle en groupe.
**Statut : jamais déployée seule, incluse dans la 0.4.0.**

## 0.4.0 — carte de la grille, têtes, couleurs d'équipe, résumé de fin de partie (24/09/2026)

**Demande de LeKiwi06** (inclut les correctifs de la 0.3.1).
- **Couleurs d'équipe** (`TeamStyle`) : A rouge, B bleu, C jaune, D verte (numéro interne 1 à 4 inchangé), dans le
  tchat, la barre d'action, le menu Objectifs, les classements et la carte. La salle d'attente (avant la partie)
  garde les numéros d'équipe.
- **Carte de la grille en main secondaire** (`map/`) : une seule carte par partie, la même pour tous. Fond blanc,
  séparations noires ; case validée à la couleur de l'équipe, partagée en 2, 3 ou 4 bandes verticales de même
  largeur si plusieurs équipes l'ont validée ; icône de l'objet centrée dans sa case (16x16, jamais sur les lignes) ;
  quantité en bas à droite de l'icône, en blanc entouré de noir. Redessinée à chaque validation. Verrouillée comme le
  papier (pas de jet, d'artisanat, de coffre, de cadre ; gardée à la mort, redonnée à la réapparition et à la
  reconnexion ; remplacée si elle date d'avant un redémarrage). Le papier Objectifs est conservé.
- **Icônes des objets** : prises dans le jeu officiel (demande de LeKiwi06 : « prends les textures vanilla du jeu »).
  Au 1er démarrage d'une version du jeu, le plugin télécharge le jeu chez Mojang (~40 Mo), en extrait une icône par
  objet dans `plugins/KG_BingoGame/icons/<version>/`, puis supprime le téléchargement ; aucune image dans le dépôt
  (public). Objets sans image à plat (coffre, coffre de l'Ender, bouclier, têtes) : face avant découpée dans la
  texture du modèle 3D. Testé hors serveur sur les 200 objectifs : 200 icônes trouvées. Tant que les icônes ne sont
  pas prêtes (ou si le téléchargement échoue), la carte affiche « ? ».
- **Têtes des coéquipiers** à gauche du menu Objectifs (équipe seulement) : points solo, objectifs validés (dont en
  1er) et leur liste, bingos.
- **Résumé de fin de partie** (`SummaryMenu`), bouton « Résumé de la partie » du menu de la nether star en salle
  d'attente post-partie : une rangée par équipe dans l'ordre du classement (laine de sa couleur avec ses points, puis
  les têtes de ses joueurs : points solo finaux, objectifs, bingos).

**Déploiement** : remplacer le jar sur Kixster (aucune autre modification). Au 1er démarrage, attendre dans le
journal « icône(s) d'objets chargée(s) pour la carte ». **Statut : déployé sur Kixster le 24/09/2026, non testé en jeu.**

**Partie à une seule équipe (mode « solo », pour s'entraîner ou faire du speedrun) : comportement VOULU, à garder**
(LeKiwi06, 24/09/2026, testé : « je veux que ça reste si un joueur veut try hard du speedrun »). Victoire aux bingos
demandés ou à la grille complète, pas de nulle possible (message « Une nulle n'a de sens qu'à plusieurs équipes »,
à garder aussi), pas de nulle automatique en blackout.
Démarrage 0.4.0 sur Kixster : icônes préparées en 3 s (1508 objets, 28 sans icône, aucun parmi les objectifs).
Test de LeKiwi06 (24/09/2026) : carte, têtes, résumé « propres et sans bugs ». Limite connue, acceptée : les têtes
des joueurs s'affichent sur Java mais pas sur Bedrock (Geyser).

## 0.4.1 — contour des objets blancs sur la carte (24/09/2026)

**Demande de LeKiwi06** : « mets un contour gris foncé pour les items de couleur blanche pour qu'ils ressortent
mieux ». Une icône dont au moins la moitié des pixels visibles sont blancs ou gris très clair (clairs et peu
colorés) reçoit un contour gris foncé d'un pixel autour de sa silhouette, sans toucher les lignes de la grille.
Parmi les 200 objectifs, 18 sont concernés (bûche de bouleau, diorite, sable, verre, vitre, fer, ficelle, os, plume,
laine et lit blancs, papier, sucre, carte, gâteau...) ; l'or et le diamant n'en ont pas. Vérifié sur un aperçu
dessiné hors serveur. **Statut : jamais déployée seule, incluse dans la 0.4.2.**

## 0.4.2 — multiplicateur de vitesse du blackout (24/09/2026)

**Demande de LeKiwi06** : en blackout, si l'équipe gagnante remplit la grille en moins de 2 h, ses points finaux sont
multipliés, APRÈS l'ajout des points des équipes classées derrière :
- grille normale : **×2 en moins d'1 h**, **×1,5 entre 1 h et 2 h** ;
- grille uniquement difficile / extrême (0 facile, 0 normal) : **×5 en moins d'1 h**, **×2 entre 1 h et 2 h**
  (« c'est une prouesse de faire un blackout comme ça en moins d'une heure, même en équipe »).
Temps = temps de jeu de la partie. Appliqué aussi aux points solo finaux des joueurs de l'équipe gagnante et aux
parties sans adversaire (supposition de Claude, non contredite - à confirmer). Affiché dans la raison de la victoire
(« blackout en 0 h 52 : ×2 »), dans le classement du tchat et dans le résumé. Inclut la 0.4.1.
**Statut : compilé, non déployé.**
