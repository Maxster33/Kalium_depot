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

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

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

## 0.10.0 - réserve de mondes par lots : plus jamais deux parties sur la même seed (05/10/2026, Maxster33)

**Constat de Maxster33 (05/10/2026)** : « hier soir j'ai lancé une partie et je me suis retrouvé avec exactement la même
seed que avant hier soir ».

**Cause** (0.8.5) : le monde ajouté à la réserve reprenait la seed du monde qui y restait (pour qu'une partie à 2 équipes
puisse prendre les 2 mondes). Une partie à 1 équipe créée pendant une autre partie prenait 1 monde, le monde restant
gardait la seed et le nouveau la reprenait : la même seed revenait à chaque partie de ce genre jusqu'au redémarrage (ou
jusqu'à ce qu'une partie prenne les 2 mondes d'un coup). La seed tirée par kal-games (`SecureRandom`) n'est pas en cause.

**Demande de Maxster33** : au redémarrage, supprimer toutes les seeds en réserve puis prégénérer 3 lots de seeds pour
4 équipes et 3 lots pour des parties en solo ; à la fin d'une partie, supprimer sa seed puis en générer une nouvelle
pour la réserve, lentement ; à 4 parties en cours, mettre en pause suppressions et générations.

**Choix de Maxster33** (questions posées) :
- suppression en arrière-plan (comme avant : monde déchargé puis fichiers effacés hors du fil du jeu, sans lag) plutôt
  qu'au rythme d'1 ou 6 chunks par seconde, qui n'apportait rien côté lag ;
- pendant une partie : seulement le terrain de la réserve, pas de création de monde (gel court du serveur) ;
- une partie à 2 ou 3 équipes prend un lot de 4 : les mondes en trop sont supprimés tout de suite ;
- aucun lot libre : mondes créés pour la partie, comme avant.

**Changements** (`InstanceWorldPreparer`, `InstanceWorldManager`, `BingoPlugin`) :
- La réserve est faite de **lots** : un lot = une seed neuve, jamais rejouée. `instances.reserve-lots-4-teams` lots de
  4 mondes (parties de 2 à 4 équipes) et `instances.reserve-lots-solo` lots de 1 monde (parties à 1 équipe), 3 et 3 par
  défaut. Chaque monde a son Nether et son End. Préparés dès le démarrage, après la suppression des mondes de réserve
  restés d'avant le redémarrage (lots solo et lots de 4 en alternance).
- **Une partie créée pendant qu'une autre est en cours prend un lot**, le plus avancé de sa taille ; ses mondes encore
  en file deviennent des tâches de la partie (servies en priorité). **Aucune partie en cours : mondes neufs créés pour
  elle avec la seed de kal-games, la réserve n'est pas entamée** (règle de la 0.8.5 conservée, demande de Maxster33).
  Mondes en trop du lot, équipes restées vides au lancement, partie annulée avant le lancement : mondes supprimés
  (avant : remis en réserve).
- **Remplacement** : le lot d'une partie est remplacé quand elle se termine (ou est annulée), une fois tous les
  effacements en cours terminés (nouveau compteur `InstanceWorldManager.pendingDeletions`). Une partie lancée sans lot
  (aucun libre) ne déclenche pas de remplacement.
- **Pendant une partie** : les mondes de réserve ne sont toujours pas créés (comme en 0.8.5) ; leur terrain avance à
  `instances.reserve-chunks-per-second-during-games` (5 par défaut), après celui des parties.
- **À `instances.max-simultaneous-games` parties en cours (4)** : réserve en pause (ni nouveau lot, ni terrain).
- Un lot dont un monde n'a pas pu être créé est abandonné en entier (mondes supprimés) et remplacé.

**Nouvelles clés** (absentes du `config.yml` déjà déployé : valeurs par défaut dans le code, à ajouter à la main pour
les modifier) : `instances.reserve-lots-4-teams: 3`, `instances.reserve-lots-solo: 3`,
`instances.reserve-chunks-per-second-during-games: 5`.

**Limites** :
- 15 mondes en réserve, soit 45 avec leur Nether et leur End, chargés et pré-générés : plus de mémoire et de disque
  qu'avec les 2 mondes de la 0.8.5. Avec les réglages du serveur (rayon 150, 40 chunks/s, un monde toutes les 5 s),
  environ 4 min de création de mondes et 8 min de terrain après le démarrage, sans partie en cours.
- Une partie à 1 équipe sans lot solo libre ne prend pas un lot de 4 : ses mondes sont créés pour elle.
- Les parties restaurées après un redémarrage n'ont pas de lot : à leur fin, rien n'est remplacé (la réserve est
  déjà complète).
- La pause à 4 parties ne concerne pas les effacements (en arrière-plan, sans lag).

### Contre la montre : nombre de bingos à choisir, 10 au plus en solo

**Demande de Maxster33 (05/10/2026)** : « il faut que l'on puisse choisir le nombre de bingo à réaliser pour remporter
la partie, et adapter le bonus de fin de partie en fonction du nombre de bingo ». Un bonus propre au contre la montre
(rang du bingo + difficulté de ses objectifs, divisé par 2) a d'abord été codé, puis abandonné par Maxster33 le même
jour : « pour les bonus de bingo finalement on va reprendre les règles de bonus de bingo des parties à plusieurs
équipes », précisé « mêmes règles qu'à plusieurs équipes sans le bonus en 1er » (c'est déjà la règle de toute partie à
une équipe depuis la 0.8.5 : seul le bonus des bingos uniquement difficiles / extrêmes, +0,5 au coefficient). Et :
« Limite le nombre de bingo à 10 pour les parties en solo ».

- Victoire : le nombre de bingos choisi à la création (réglage « Bingos à achever » déjà envoyé par KG_Bingo ; 3 par
  défaut) au lieu de toute la grille.
- **Partie à 1 équipe (tous modes) : 10 bingos au plus** (`BingoSettings.MAX_BINGOS_SOLO`, appliqué à l'arrivée des
  réglages de la partie, quelle que soit la version de KG_Bingo) ; 12 au plus à plusieurs équipes, comme avant.
- Chrono à zéro avant : défaite, « les N bingos n'ont pas été achevés à temps » ; les points des objectifs restent.
- Bonus de victoire inchangés : bonus habituel + 1 point par tranche de 20 s restantes.
- Barre d'action : bingos achevés / demandés (comme le mode Bingos) au lieu des objectifs remplis.

À déployer avec **KG_Bingo 1.7.3** (menu de création : 10 bingos au plus à 1 équipe, texte d'aide). Avec KG_Bingo
1.7.2, la règle s'applique quand même mais l'aide parle encore de la grille complète.

**Compilé le 05/10/2026. Déployé sur Serveur Jeux le 05/10/2026 à 13:34 (Maxster33 ; 0.9.0 dans `_removed-kg_bingogame-0.9.0/`), actif après redémarrage. Statut : non testé en jeu.** À tester : logs « Réserve : préparation d'un lot »
au démarrage (6 lots) ; une partie seule sur le serveur (pas de lot pris, mondes neufs) ; une partie créée pendant une
autre (« lot de réserve ... attribué », seed différente, pas de gel) ; fin de partie puis « son lot de réserve sera
remplacé » et nouveau lot. Contre la montre à 3 bingos : victoire au 3e bingo avec le bonus de temps ; défaite au
chrono ; menu de création limité à 10 bingos à 1 équipe.

## 0.11.0 - objet « Rejouer » dans la salle d'attente d'après-partie (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « pour tous les mini-jeux : faire un bouton à la fin de la partie pour relancer une partie
avec les mêmes paramètres » ; « il faut proposer ça aux joueurs pendant 30 secondes à la fin d'une partie ». Réponses du
09/10/2026 : objet dans la barre ; le premier qui clique recrée la partie et en devient l'hôte ; nouvelle seed.

- À la fin d'une partie (victoire ou temps écoulé), chaque joueur placé dans la salle d'attente d'après-partie reçoit
  l'objet **« Rejouer »** (totem, case 7) pendant **30 s**, en plus de l'étoile « Partie terminée ».
- Clic : les réglages de la partie terminée (équipes, joueurs par équipe, durée, mode, bingos, composition de la
  grille) sont déposés sur le relais (clé `bingo-rejouer-<uuid>`, gardée 2 min), puis le joueur est renvoyé sur
  kal-games, où **KG_Bingo 1.9.0** recrée la partie (nouvelle seed) et le renvoie ici, dans la nouvelle salle
  d'attente. Les parties ne se créent que sur kal-games (limites, liste, codes) : d'où cet aller-retour (deux écrans
  de chargement).
- Le premier qui clique devient l'hôte ; l'objet des autres devient **« Rejoindre la partie de X »** (message dans le
  tchat) : clic = même aller-retour, puis entrée dans la partie relancée.
- Si le relais ne répond pas, le joueur n'est pas renvoyé (message) et garde l'objet.
- L'objet est verrouillé comme l'étoile (ni déplacé, ni lâché) et retiré quand le joueur quitte la salle.
- Les réglages de l'hôte (équipes, taille, durée) sont gardés dans la partie (`BingoGame.setPartySettings`) ; pour une
  partie restaurée après un redémarrage, on prend les équipes jouées, la plus grande équipe et la durée actuelle.
- **Nouvelle clé de `config.yml`** (valeur par défaut dans le code) : `game.replay-seconds` (30).

Non fait : les **observateurs** (KG_BingoObservateur) ne reçoivent pas l'objet (question ouverte du cahier des charges).

**À déployer avec KG_Bingo 1.9.0.** **Compilé le 09/10/2026. Statut : non testé en jeu.**

## 0.12.0 - contre la montre : bonus de temps selon la grille (14/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « le bonus de temps n'est pas assez conséquent, le faire à cette vitesse devrait rapporter
bien plus, aux alentours de 1800 » (grille de 25 objectifs difficiles finie en 1 h 30 en solo). Repère : la même grille
en blackout finie en 1 h 30 rapporte 880 × 2 = 1 760 points. Formule choisie par LeKiwi06 parmi trois (taux unique de
1 point par 5 s, multiplicateur du blackout, valeur selon la grille).

- **Chaque minute restante au chrono vaut la valeur moyenne d'un objectif de la grille** (facile 2, normal 6,
  difficile 10, extrême 20), au prorata des secondes, arrondi au dixième inférieur (`BingoSettings.chronoTimeBonus`).
  Avant (0.8.5) : 1 point par tranche de 20 s, quelle que soit la grille (`CHRONO_SECONDS_PER_POINT`, supprimé).
- Inchangé : bonus seulement en cas de victoire, ajouté tel quel à l'équipe et à chacun de ses joueurs encore en jeu,
  sans multiplicateur ; message de fin « N min SS s restantes : +X pts ».

Exemples (grille de 25 difficiles, 10 bingos, dernière case sur une diagonale, soit 880 points d'objectifs) : finie en
1 h 30 → 82 min 30 restantes → +825 (avant +247), **1 705 points** ; à 5 min par objectif (2 h 05) → +475 (avant +142),
1 355 points. Grille par défaut (10 faciles, 10 normaux, 5 difficiles) : 5,2 points par minute restante (avant 3) ;
grille de faciles seulement : 2 points par minute.

Rappel : en solo, 10 bingos au plus ; la partie s'arrête au 10e bingo, la grille entière n'est validée que si la
dernière case est sur une diagonale (elle achève alors 3 bingos d'un coup).

Rien à changer sur Kal-Games (l'aide de KG_Bingo dit seulement « bonus selon le temps restant »).

**Compilé le 14/10/2026. Déployé sur Serveur Jeux le 14/10/2026 à 16:06 (LeKiwi06 ; 0.11.0 dans `_removed-kg_bingogame-0.11.0/`), actif après redémarrage. Statut : non testé en jeu** (la 0.11.0 non plus).

## 0.13.0 - ghast d'Halloween : un joker pour la grille (30/10/2026, LeKiwi06)

**Demande de LeKiwi06** (extrait de son annonce de l'événement d'Halloween) : « Un ghast est présent dans chaque
partie ; si vous le tuez en lui renvoyant sa boule, cela valide une case de votre grille. » Réponses du 30/10 :
joker au choix du joueur, avec le bonus « en 1er » ; apparition au hasard dans un rayon de 500 blocs autour du spawn,
sans pouvoir disparaître ; seul ce ghast compte (pas ceux du Nether) ; du 31/10 au 14/11 inclus.

- **Un ghast par équipe** (chaque équipe a son monde) : posé au lancement de la partie au hasard dans un rayon de
  500 blocs autour du point de départ, 20 blocs au-dessus du sol (chunk chargé sans bloquer le serveur), nom
  « Ghast d'Halloween » visible, persistant (ne disparaît jamais). Message à l'équipe à son apparition.
- **Ne meurt que de sa boule renvoyée** par un joueur de l'équipe : au coup ou avec un projectile (le jeu 26.3 donne
  la boule au joueur qui la frappe, `Projectile.deflect`, ou au tireur du projectile, `Projectile.onHit` + tag
  `redirectable_projectile`). Tout autre dégât est annulé (`/kill` reste possible). Une fois tué, il ne revient pas.
  **Pas de butin** (choix de Claude, à confirmer : la larme de ghast et la poudre à canon sont des objectifs de la
  grille, le ghast les aurait données en plus du joker dans l'Overworld).
- **Joker** : annonce à toute la partie (« Équipe X tue le ghast d'Halloween (pseudo) : un joker ! »), son de défi.
  Dans le papier Objectifs, l'objet « Joker du ghast » (boule de feu, en bas à gauche, seulement si l'équipe en a)
  ouvre la grille « Choisir une case » : un clic sur une case non validée la valide pour l'équipe exactement comme
  un objet trouvé (points, bonus « en 1er », bingos, temps gagné en contre la montre, victoire), annonce « valide …
  avec le joker ». La porte revient au papier Objectifs ; Échap = plus tard, le joker reste. Perdu à la fin de la
  partie.
- **Barre de boss** « Ghast d'Halloween » pour les joueurs à moins de 96 blocs du ghast vivant (pas de surbrillance,
  invisible sur Bedrock).
- État gardé dans les données du monde de l'équipe (ghast posé, jokers restants) : survit à un redémarrage.
- `ObjectiveValidationTask.validate` : validation d'une case sortie de `tick()` pour être partagée avec le joker.
- `config.yml` : `halloween.ghast.debut` / `fin` (aaaa-mm-jj, inclus, date de lancement de la partie, heure de Paris)
  et `rayon`. Valeurs par défaut dans le code (31/10, 14/11, 500) : rien à ajouter sur le serveur sauf pour les changer.

Limites : si le chunk tiré ne se charge pas, le ghast n'est pas reposé (message dans le journal du serveur) ; en
difficulté paisible le jeu retire les ghasts (avertissement dans le journal) ; le ghast peut s'éloigner de sa zone en
volant (rien ne le ramène).

**Compilé le 30/10/2026. Déployé sur Serveur Jeux (session « Event [new] ») le 30/10/2026 à 18:46 (LeKiwi06 ; 0.12.0 dans `_removed-kg_bingogame-0.12.0/`), actif après redémarrage. Statut : non testé en jeu.** Cahier publié dans `CAHIER_DES_CHARGES.md`.
