# JOURNAL — KaliumRelay

Plugin **Velocity** (pas Paper/Bukkit), tournant sur le proxy `ProxyVelocity` (7018.mystrator.com,
`velocity-4.1.2-SNAPSHOT-27.jar`) — pas sur KalGames ni sur KalBingo. Relais HTTP minimal entre ces
deux plugins, créé le 23/09/2026 pour résoudre une limite réseau rencontrée sur l'intégration Bingo
(voir contexte ci-dessous).

Instruction explicite du projet, à respecter en permanence : **ne jamais construire quelque chose de
non demandé** ; proposer les idées techniques avant de les implémenter ; signaler toute ambiguïté
plutôt que de deviner.

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## Contexte : pourquoi ce plugin existe

KalGames (hub, serveur `kal-games`) et KalBingo (mini-jeu Bingo, serveur `Kixster`) sont deux plugins
Paper séparés, reliés par le proxy Velocity. Pour transférer un joueur vers une partie Bingo, KalGames
avait besoin de transmettre à KalBingo l'affectation de ce joueur (gameId, seed, hôte, équipes,
roster) via le canal de messagerie plugin BungeeCord/Velocity (`Forward`/`Connect`).

**Problème identifié (bug remonté par l'utilisateur, 23/09/2026)** : ce canal de messagerie exige
qu'AU MOINS UN joueur soit connecté sur le serveur CIBLE pour qu'un message puisse être livré — c'est
une contrainte du protocole Minecraft lui-même (le message est routé via la connexion d'un joueur),
pas quelque chose de corrigeable uniquement côté code. En test à 2 comptes, si l'hôte et un second
joueur qui rejoint se retrouvent tous les deux sur Kixster en même temps, kal-games tombe à 0 joueur
connecté et ne peut plus recevoir la demande d'affectation envoyée par KalBingo (design "pull" —
Bingo interroge kal-games à l'arrivée du joueur) : le second joueur reste bloqué en salle d'attente
puis est expulsé au bout du délai configuré.

L'utilisateur a demandé si charger en continu une zone sur kal-games (sans joueur présent)
résoudrait le problème. Expliqué que non : la contrainte est le nombre de joueurs connectés, pas les
chunks chargés — un chunk chargé ne crée pas de connexion joueur. Plusieurs options ont alors été
proposées (AskUserQuestion) ; l'utilisateur a choisi **un plugin Velocity qui relaie les messages**,
confirmé pouvoir ouvrir un port sur l'hébergement Minestrator du proxy, et fourni le port **46199**.

## Ce que fait KaliumRelay

Un serveur HTTP minimal (`com.sun.net.httpserver.HttpServer`, aucune dépendance externe — pas de
framework HTTP tiers) tournant dans le processus du proxy Velocity, sur le port choisi par
l'utilisateur (46199). Il ne fait qu'une chose : stocker temporairement l'affectation d'un joueur,
déposée par KalGames, jusqu'à ce que KalBingo vienne la récupérer.

- `POST /assignment/{playerId}` : KalGames dépose l'affectation (corps texte simple, lignes
  `clé=valeur` : `gameId`, `seed`, `duration`, `host`, `teamCount`, `teamSize`, `roster` en UUID
  séparés par des virgules) — appelé par `BingoPartyManager.transferToBingo` juste avant le transfert
  du joueur.
- `GET /assignment/{playerId}` : KalBingo récupère l'affectation — **consomme et supprime** l'entrée
  à la lecture (pas de double livraison). Appelé en boucle par `AssignmentService.pollRelay` dès
  qu'un joueur sans affectation connue arrive sur Kixster, EN PARALLÈLE du canal BungeeCord existant
  (gardé comme filet de sécurité) — la première voie à résoudre gagne.
- Authentification : en-tête `X-Kalium-Relay-Token`, jeton partagé identique dans les trois projets
  (`KalGames/config.yml` → `bingo.relay-token`, `KalBingo/config.yml` → `network.relay-token`,
  `KaliumRelay/relay.properties` → `token`) — valeur fixe choisie au départ pour que les trois
  fonctionnent ensemble sans étape de copier-coller manuelle. Réponse 401 si absent/incorrect.
- Purge automatique (toutes les minutes) des entrées non récupérées depuis plus de 2 minutes — évite
  une fuite mémoire si un joueur ne se connecte jamais à KalBingo après son transfert.

**Ce plugin est un COMPLÉMENT au canal BungeeCord existant, jamais un remplacement.** Si
`relay-url`/`relay-token` sont vides côté KalGames ou KalBingo (config non renseignée), ou si le
relais est injoignable, tout continue de fonctionner comme avant (silencieux, aucune casse) : le
canal BungeeCord reste le chemin par défaut, juste moins fiable dans le cas "0 joueur" décrit
ci-dessus.

## Architecture technique

- `KaliumRelay.java` : classe principale (`@Plugin(id = "kaliumrelay", ...)`), injection
  `com.google.inject.Inject` (Guice — voir section "1.0.1" ci-dessous pour pourquoi ce n'est PAS
  `javax.inject.Inject` malgré ce qui avait été tenté en 1.0.0). S'abonne à `ProxyInitializeEvent`
  (démarre `RelayHttpServer`) et `ProxyShutdownEvent` (l'arrête proprement).
- `RelayConfig.java` : charge/crée `relay.properties` (port + jeton) dans le dossier de données du
  plugin au premier démarrage, avec des valeurs par défaut (port 46199, jeton fixe partagé avec
  KalGames/KalBingo) — pas de dépendance YAML supplémentaire pour un fichier aussi simple.
- `RelayHttpServer.java` : le serveur HTTP lui-même — `ConcurrentHashMap<String, Entry>` en mémoire
  (`record Entry(String body, long storedAt)`), POST stocke, GET consomme-et-supprime, tâche de fond
  (1x/minute) qui purge les entrées de plus de 2 minutes.

## Build

Comme tous les autres projets de ce workspace (KalGames, KalBingo, KaliumCore, KaliumMenu) : Maven
Central est bloqué dans cet environnement (403 Forbidden sur `repo.maven.apache.org`, confirmé par
un essai de `mvn package` qui échoue systématiquement, pas seulement pour ce projet). `pom.xml` est
donc **cosmétique uniquement** — le vrai build passe par `build.sh` : compilateur ECJ (Eclipse
Compiler for Java) + jars de dépendances récupérés manuellement via `curl` dans `libs/` (ici :
`velocity-api-4.1.2-SNAPSHOT` et `javax.inject`, tous deux récupérés depuis
`repo.papermc.io/repository/maven-public/`, seul dépôt Maven accessible dans cet environnement).

Version de `velocity-api` choisie en fonction du jar **réellement déployé** sur le proxy
(`velocity-4.1.2-SNAPSHOT-27.jar`, vérifié par inspection directe via WinSCP), pas la dernière version
stable (4.2.0) qui aurait pu introduire un risque d'incompatibilité ABI/API avec le proxy en place.

## Pour reprendre la prochaine fois

- Une fois les trois serveurs (kal-games, Kixster, proxy Velocity) redémarrés avec 1.0.1 + les
  `config.yml` patchés : retester le flux hôte + second joueur qui rejoint. Cette fois les DEUX causes
  trouvées (relais qui ne démarrait pas + config jamais renseignée) sont corrigées, contrairement au
  premier essai (1.0.0) qui n'avait résolu ni l'une ni l'autre malgré un déploiement en apparence
  complet.
- Si le blocage se reproduit malgré tout : vérifier que le port 46199 est bien accessible en TCP
  depuis les machines de kal-games ET Kixster vers celle du proxy (pas seulement "ouvert" côté
  hébergement Minestrator — un pare-feu intermédiaire pourrait encore bloquer), et consulter les logs
  du plugin KaliumRelay sur le proxy (préfixe `[KaliumRelay]`) — cette fois ils devraient au moins
  exister, ce qui n'était pas le cas avec la 1.0.0.
- Le jeton partagé est actuellement une valeur fixe choisie
  pour éviter une étape de configuration manuelle. Si la sécurité de ce canal devient sensible (il ne
  transite que des UUID de partie et des noms de serveur, rien de plus sensible à ce stade), un vrai
  secret aléatoire devrait être généré et copié dans les trois `config.yml`/`relay.properties` à la
  fois.
- Pas de HTTPS (HTTP simple) : acceptable pour l'instant vu que les trois serveurs sont sur le même
  hébergement Minestrator et que les données transmises ne sont pas sensibles — à reconsidérer si ça
  change.

## Pour reprendre la prochaine fois (mise à jour, 1.1.0)

- Une fois le proxy et Kixster redémarrés : tester la reconnexion (déconnexion pendant une partie en
  cours, puis reconnexion - doit arriver directement sur Kixster plutôt que sur kal-games/lobby). Les
  logs du plugin (préfixe `[KaliumRelay]`) confirment le routage direct quand il a lieu.
  ("est enregistré en partie sur ... mais ce serveur n'est pas déclaré" indiquerait une incohérence de
  nom entre `network.self-server-name` côté KalBingo et le nom déclaré dans `velocity.toml`).
- `DELETE /active-game/{playerId}` existe mais n'est jamais appelé (voir ci-dessus) : à relier au jour
  où une vraie fin de partie existera côté KalBingo (section 9 du cahier des charges), pour que le
  registre ne garde pas un joueur "en partie" indéfiniment après la fin réelle de sa partie.

## 1.10.0 - parrainage et liste blanche de Kixster (23/10/2026, LeKiwi06)

**Demande de LeKiwi06 (23/10/2026)**, à partir de son message d'annonce : système de parrainage, récompenses selon
l'activité de l'invité, liste blanche automatique par parrainage, demande de liste blanche par une interface quand un
joueur non inscrit essaie de rejoindre Kixster (pseudo Discord, traitée par un admin). Maquette et barème validés le
même jour (« je valide »).

- Nouvelle classe `Parrainage` (sous le verrou de `Contacts`) : propositions de parrainage, parrainages acceptés (un
  parrain par filleul, gardés pour toujours : historique des admins), demandes de liste blanche, joueurs « autorisés »
  (acceptés, pas encore ajoutés à la liste blanche de Kixster), copie de la liste blanche et des opérateurs de Kixster
  envoyée par KS_Parrainage, copie du temps de jeu des filleuls et des paliers (affichage).
  Fichiers : `plugins/kaliumrelay/parrainage.txt`, `whitelist-kixster.txt`.
- Actions `/contacts/` : `plist`, `pparrainer`, `pannuler`, `paccepter`, `prefuser`, `wdemande`, `wliste`, `whisto`,
  `waccepter`, `wrefuser` (KLM_Contacts 1.5.0) ; `wsync`, `wcheck`, `wapplique`, `pkixster`, `pjeu` (KS_Parrainage).
- Entrée sur Kixster (`onServerPreConnect`) : un joueur absent de la copie de la liste blanche, non autorisé et non
  admin est refusé ; son serveur reçoit le message `whitelist` (canal `kalium:contacts`) et KLM_Contacts lui ouvre la
  demande (ou l'acceptation d'un parrainage reçu). Tant que Kixster n'a jamais envoyé sa liste, ou si sa liste blanche
  est désactivée, le proxy laisse passer.
- Conditions : le parrain doit être sur la liste blanche de Kixster ; le filleul doit être déjà venu sur KaLium, ne
  jamais avoir été sur Kixster ni avoir été parrainé ; 10 propositions en attente au plus par parrain ; un joueur
  bloqué par le filleul ne lui envoie rien (sans le savoir).
- Prévenus dans le tchat : le filleul (proposition, cliquable `/parrainage`), le parrain (acceptation, refus), les
  admins de `relay.properties` (nouvelle demande, et à leur arrivée s'il y en a en attente), le joueur (demande
  acceptée ou refusée).
- `relay.properties` : nouvelle clé `parrainage-serveur` (défaut `kixster`, ajoutée d'office au fichier).
- Corps des requêtes `/contacts/` : 64 Ko au plus (4 Ko avant), pour la liste blanche de Kixster.

Limites : la liste des admins prévenus est celle de `relay.properties` (le proxy ne connaît pas les permissions des
serveurs) ; un joueur retiré plus tard de la liste blanche de Kixster ne peut plus être parrainé (il a déjà eu un
parrain, ou il a été sur Kixster) : il passe par une demande aux admins.

**Compilé, non déployé, non testé en jeu.** Empilé sur la 1.9.0 testée en solo seulement (LeKiwi06 a répondu « je valide » au message qui le signalait, le
23/10/2026). À déployer avec KLM_Contacts 1.5.0 (tous les serveurs Paper) et, sur Kixster, KS_Parrainage 1.0.0 +
KS_RewardsGUI 1.6.0 (+ KLM_Statistiques 1.0.0 et `didacticiel.yml`).

**Déployé sur le proxy le 23/10/2026 à 16:18 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; 1.9.0 dans `_removed-kaliumrelay-1.9.0/` ; actif après redémarrage du proxy, non testé en jeu.**

## 1.10.1 - anciens joueurs parrainables, « whitelist » (23/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (23/10/2026, après le déploiement)** : « j'ai voulu parrainer un joueur, ça me dit que Kixster n'a pas encore envoyé la liste blanche (j'aimerais que les joueurs déjà whitelist puissent prendre un parrain, pour qu'on puisse rattraper les récompenses qu'on aurait dû avoir) ; utilise le terme whitelist au lieu de liste blanche d'ailleurs » ; « j'aimerais aussi que les heures AFK sur le serveur ne comptent pas (considérer un joueur comme AFK s'il n'effectue pas d'actions pendant plus de 5 minutes et enlever l'AFK quand une action est effectuée) ». Ses choix : rattrapage sur le temps vanilla (CoreProtect, prévu d'abord, ne tourne pas sur Kixster en 26.3), temps actif aussi dans Statistiques.

- Un joueur déjà whitelisté sur Kixster peut prendre un parrain (avant : seulement un joueur jamais venu) ; il n'est pas remis dans les « autorisés » (déjà sur la whitelist). Toujours un seul parrain par joueur.
- Pas de parrainage croisé : on ne parraine pas son parrain, ni le parrain de son parrain (`err:cycle`), à la proposition comme à l'acceptation ; ces joueurs n'apparaissent pas dans les amis à parrainer.
- « whitelist » au lieu de « liste blanche » dans les messages du tchat.
- `pkixster` envoie aussi la date d'acceptation.

**Compilé, non déployé, non testé en jeu.** À déployer ensemble : KaliumRelay 1.10.1 (proxy), KLM_Contacts 1.5.1 (tous les serveurs Paper), KS_Parrainage 1.1.0 + KLM_Statistiques 1.1.0 + `didacticiel.yml` (Kixster). Empilé sur les versions du 23/10 (déployées, pas encore testées), à la suite de la même demande.

**Déployé sur le proxy le 23/10/2026 à 18:50 (accord de LeKiwi06) ; 1.10.0 dans `_removed-kaliumrelay-1.10.0/` ; actif après redémarrage, non testé en jeu.**

## 1.10.2 - historique des demandes de whitelist traitées (26/10/2026, LeKiwi06)

**Demande de LeKiwi06 (26/10/2026)** : « j'aimerai que tu me créer un historique pour les demandes de whitelist traités ».

- Chaque demande traitée (acceptée ou refusée par un admin, ou acceptée par un parrainage) est copiée dans l'historique avec la **date du traitement** (lignes `H` de `parrainage.txt`, gardées pour toujours). Avant, une nouvelle demande d'un joueur refusé remplaçait l'ancienne : l'historique les garde toutes.
- Au premier démarrage en 1.10.2, les demandes déjà traitées (acceptées ou refusées avant) passent dans l'historique, sans date de traitement (inconnue).
- Nouvelle requête `whdemandes` (KLM_Contacts 1.5.2) : l'historique, le plus récent d'abord.
- **Rappel au lobby** (deuxième demande du 26/10/2026 : « met moi un message quand je rejoins le lobby comme pour les rewards pour me dire combien de demande de whitelist sont en attente, histoire que je n'en laisse pas trainer trop longtemps ») : à **chaque** arrivée sur le lobby (premier serveur de la liste `try` de Velocity), 3 s après, comme le rappel des rewards de Kixster, un admin de `relay.properties` (`admins`) reçoit « Tu as N demande(s) de whitelist en attente : menu > Modération > Demandes de whitelist » (rien s'il n'y en a aucune). Remplace le message de la 1.10.0, envoyé seulement à la première connexion et au moment même de l'arrivée (sans doute jamais reçu).
- Aucun réglage nouveau.

Limite (inchangée) : une demande en attente d'un joueur ajouté à la main à la whitelist de Kixster reste « en attente ».

**Compilé, non déployé, non testé en jeu.** Empilé sur la 1.10.1 (parrainage testé par LeKiwi06 le 24/10/2026, demandes de whitelist jamais testées), à sa demande. À déployer avec KLM_Contacts 1.5.2 (tous les serveurs Paper) ; sans lui, le bouton « Demandes traitées » ne reçoit rien.

**Déployé le 26/10/2026 à 01:11 sur le proxy (accord de LeKiwi06) ; 1.10.1 dans `_removed-kaliumrelay-1.10.1/` ; actif après redémarrage, non testé en jeu.**

## 1.11.0 - jeton du bot Discord (09/11/2026, LeKiwi06)

**Demande de LeKiwi06 (09/11/2026)** : étape 1 du cahier complet du bot Discord (KLM_DiscordBot 0.3.0) : joindre
KG_ScoreBoards sur Kanvas par la boîte aux lettres du relais (le proxy ne peut pas joindre Kanvas en direct, et le
bot sur Dishost non plus).
- Nouvelle clé `bot-token` dans `relay.properties`, générée au premier démarrage si absente, à recopier dans le `.env`
  du bot (`RELAIS_TOKEN`). Différente du jeton des serveurs (`token`).
- Avec ce jeton, `/mail/` seulement : déposer et retirer dans `scoreboards`, lire et confirmer dans `bot` ; toute autre
  boîte : 403 ; `/contacts`, `/ban`, `/assignment`, `/active-game` : 401. Le jeton des serveurs garde tous ses droits.
- Testé en local (serveur HTTP du relais lancé seul) : chaque droit et chaque refus ci-dessus vérifiés.

**Compilé, non déployé.** À déployer avec KG_ScoreBoards 1.10.0 (Kanvas) et KLM_DiscordBot 0.3.0 ; redémarrage du
proxy nécessaire.
