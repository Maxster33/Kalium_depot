# JOURNAL — KaliumRelay

Plugin **Velocity** (pas Paper/Bukkit), tournant sur le proxy `ProxyVelocity` (7018.mystrator.com,
`velocity-4.1.2-SNAPSHOT-27.jar`) — pas sur KalGames ni sur KalBingo. Relais HTTP minimal entre ces
deux plugins, créé le 23/09/2026 pour résoudre une limite réseau rencontrée sur l'intégration Bingo
(voir contexte ci-dessous).

Instruction explicite du projet, à respecter en permanence : **ne jamais construire quelque chose de
non demandé** ; proposer les idées techniques avant de les implémenter ; signaler toute ambiguïté
plutôt que de deviner.

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

## 1.0.0 — première version

Voir section "Contexte" ci-dessus pour le détail complet. Résumé : `POST`/`GET
/assignment/{playerId}`, jeton partagé, purge automatique après 2 minutes.

**Déployé le 23/09/2026 sur le proxy `ProxyVelocity` (7018.mystrator.com) via WinSCP** :
`KaliumRelay-1.0.0.jar` envoyé dans `/plugins/` — première installation, aucun ancien jar à archiver.
**Redémarrage du proxy Velocity requis** pour que le plugin démarre (en plus des redémarrages de
kal-games et Kixster déjà nécessaires pour KalGames 1.10.3 / KalBingo 0.1.10 — voir leurs JOURNAL.md
respectifs) — c'est un TROISIÈME serveur à redémarrer, qui n'avait encore jamais eu besoin de l'être
pour l'intégration Bingo jusqu'ici.

## 1.0.1 — deux bugs trouvés au diagnostic par logs (23/09/2026)

Après le déploiement de la 1.0.0 et des mises à jour associées de KalGames 1.10.3 / KalBingo 0.1.10,
l'utilisateur a signalé que le bug persistait à l'identique : l'hôte crée/assigne bien la partie, mais
le joueur qui rejoint atterrit quand même dans une mauvaise salle d'attente et se fait expulser après
quelques secondes. N'ayant pas de console/SSH sur les trois serveurs, diagnostic mené en téléchargeant
`logs/latest.log` de `kal-games`, `Kixster` et `ProxyVelocity` via WinSCP (F5 vers le PC local), puis
lecture des fichiers. Deux causes INDÉPENDANTES trouvées, les deux nécessaires pour que le bug
disparaisse :

**Bug 1 — KaliumRelay ne démarrait jamais sur le proxy.** `proxy-latest.log` montrait
`com.google.inject.CreationException: [Guice/MissingConstructor]: No injectable constructor for type
KaliumRelay` au démarrage — le plugin échouait silencieusement au chargement, donc le serveur HTTP
relais n'existait tout simplement pas, quel que soit le reste. Cause : le constructeur était annoté
`javax.inject.Inject` (JSR-330), pas `com.google.inject.Inject` (Guice). Le jar livré ne contient QUE
nos propres classes compilées (même convention que velocity-api/paper-api : les jars de libs/ servent
uniquement à compiler, jamais embarqués) — rien ne garantissait que `javax.inject.Inject` soit résolu
correctement par l'injecteur au runtime. Le système de plugins de Velocity repose entièrement sur
Guice : son propre Injector garantit que `com.google.inject.Inject` est toujours présent et
correctement résolu, contrairement à l'annotation JSR-330 séparée. **Corrigé en 1.0.1** : import changé
vers `com.google.inject.Inject`, `guice-5.1.0.jar` récupéré depuis `repo.papermc.io` (scope `provided`
dans le pom.xml cosmétique, jamais embarqué dans le jar livré — confirmé par `unzip -l` : seulement nos
4 classes + `velocity-plugin.json` + `META-INF/`).

**Bug 2 — les `config.yml` déjà déployés sur kal-games et Kixster étaient obsolètes.**
`saveDefaultConfig()` (Paper/Bukkit) n'écrit le `config.yml` par défaut QUE si le fichier n'existe pas
encore sur le disque — il ne fusionne JAMAIS de nouvelles clés dans un `config.yml` déjà existant. Les
fichiers réellement déployés sur kal-games et Kixster dataient d'avant les modifs relais (créés à la
première installation, jamais régénérés depuis), donc malgré un code source à jour et un
`config.yml` bundlé correct dans les jars 1.10.3/0.1.10 : `bingo.relay-url` (KalGames) et
`network.relay-url` (KalBingo) résolvaient tous les deux à `""` (repli code), le relais n'était donc
JAMAIS interrogé ni alimenté même une fois le Bug 1 corrigé. `kalgames-latest.log` confirmait : zéro
ligne `[Bingo]` (donc `pushAssignment` ne s'exécutait jamais). Par ailleurs `assignment-wait-seconds`
était resté à l'ancienne valeur `15` sur Kixster (pas `30` comme prévu depuis la 0.1.10) : le log
`kixster-latest.log` montrait le second joueur connecté dans le mauvais monde (`overworld` au lieu de
`bingo_lobby`) puis expulsé exactement 15 secondes plus tard — signature exacte du timeout, pas d'un
kick ou d'une déconnexion volontaire. **Corrigé en patchant les `config.yml` EN PLACE sur les deux
serveurs** (téléchargés/édités/ré-uploadés via WinSCP, aucune valeur admin existante touchée - voir
JOURNAL.md de KalGames et de KalBingo pour le détail par projet). Cette limite de
`saveDefaultConfig()` peut resurgir à chaque future clé de config ajoutée à n'importe quel plugin de ce
workspace : à garder en tête, pas spécifique à Bingo/relais.

**Déployé le 23/09/2026** : `KaliumRelay-1.0.0.jar` archivé dans `/plugins/_removed-kaliumrelay-1.0.0/`
(Shift+F6, non destructif), `KaliumRelay-1.0.1.jar` envoyé dans `/plugins/`. **Proxy à redémarrer** (en
plus de kal-games et Kixster, déjà nécessaires — voir JOURNAL.md respectifs) avant de retester.

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

## 1.1.0 — routage initial pour la reconnexion en cours de partie (23/09/2026)

Suite au premier test complet réussi (voir JOURNAL.md de KalBingo, section 0.1.11) : demande explicite
de l'utilisateur, "si un joueur est déconnecté durant une partie et qu'il se reconnecte avant la fin de
la partie il faut que le proxy le renvoi directement sur la partie". KaliumRelay est le seul endroit du
projet qui tourne DANS le processus du proxy (KalGames et KalBingo ne peuvent pas, eux, décider vers
quel serveur router une connexion entrante) - extension naturelle du même plugin plutôt qu'un nouveau,
pas de dépendance supplémentaire.

**Nouveau `ActiveGameRegistry`** : `Map<UUID, String>` en mémoire (joueur -&gt; nom du serveur où il est
"en partie"), alimenté par KalBingo via deux nouvelles routes HTTP sur le serveur existant (voir
`RelayHttpServer`, même serveur/port/jeton que `/assignment/`) :
- `POST /active-game/{playerId}` (corps = nom du serveur, ex. "kixster") : appelé par
  `PartyStarter.start()` côté KalBingo dès qu'un joueur est téléporté dans son instance (voir JOURNAL.md
  de KalBingo, 0.1.11).
- `DELETE /active-game/{playerId}` : existe côté serveur (endpoint prêt) mais **n'est appelé nulle part
  pour l'instant** — aucune fin de partie n'existe encore côté KalBingo (`GameManager.cleanupGame`
  jamais invoqué). Purge de sécurité à 6h (`ActiveGameRegistry.sweep`, très large — pas une vraie
  expiration de partie, juste un garde-fou mémoire en cas de crash/oubli) en attendant qu'une vraie fin
  de partie existe et appelle ce DELETE.

**Nouvel abonnement `PlayerChooseInitialServerEvent`** (`KaliumRelay.onChooseInitialServer`) : à CHAQUE
nouvelle connexion au proxy (y compris une reconnexion après déconnexion complète - c'est le seul
moment où cet événement se déclenche), consulte `ActiveGameRegistry` AVANT que Velocity ne choisisse le
serveur initial habituel (try list de `velocity.toml`, normalement kal-games/lobby). Si le joueur y
figure et que le serveur enregistré est bien déclaré sur ce proxy (`server.getServer(name)`), redirige
directement vers lui (`event.setInitialServer(...)`) - sinon (joueur absent du registre, ou serveur non
déclaré : garde-fou anti config incohérente) le routage habituel s'applique sans aucun changement.
Lecture 100% en mémoire, aucun appel HTTP nécessaire ici (même JVM que le serveur HTTP qui alimente le
registre).

**Déployé le 23/09/2026 via WinSCP** : ancien `KaliumRelay-1.0.1.jar` archivé (non détruit) dans
`/plugins/_removed-kaliumrelay-1.0.1/`, `KaliumRelay-1.1.0.jar` envoyé dans `/plugins/` sur le proxy.
**Redémarrage du proxy requis** (en plus de Kixster, voir JOURNAL.md de KalBingo 0.1.11 — kal-games
n'a pas besoin d'être redémarré cette fois, aucune modification côté KalGames).

## Pour reprendre la prochaine fois (mise à jour, 1.1.0)

- Une fois le proxy et Kixster redémarrés : tester la reconnexion (déconnexion pendant une partie en
  cours, puis reconnexion - doit arriver directement sur Kixster plutôt que sur kal-games/lobby). Les
  logs du plugin (préfixe `[KaliumRelay]`) confirment le routage direct quand il a lieu.
  ("est enregistré en partie sur ... mais ce serveur n'est pas déclaré" indiquerait une incohérence de
  nom entre `network.self-server-name` côté KalBingo et le nom déclaré dans `velocity.toml`).
- `DELETE /active-game/{playerId}` existe mais n'est jamais appelé (voir ci-dessus) : à relier au jour
  où une vraie fin de partie existera côté KalBingo (section 9 du cahier des charges), pour que le
  registre ne garde pas un joueur "en partie" indéfiniment après la fin réelle de sa partie.

## 1.1.1 — plus de jeton écrit dans le code (24/09/2026)

**Demande de LeKiwi06** : « comment on fait en sorte que le token ne leak pas de nouveau ? [...] si on change pas
le système, ça va re leak au prochain dépôt ».

**Cause** : le jeton partagé était une valeur fixe écrite dans `RelayConfig.java` et dans les `config.yml` fournis
avec KalGames et KalBingo, donc publiée dans le dépôt GitHub public ; il était aussi affiché dans la console du
proxy à chaque démarrage. Le jeton a été changé le 24/09/2026 à la main sur les 3 serveurs (`relay.properties` du
proxy, `config.yml` de kal-games et de Kixster), test Bingo réussi, sans redéploiement.

- `RelayConfig` : plus de `DEFAULT_TOKEN`. Si `relay.properties` n'a pas de jeton (ou un jeton vide), un jeton
  aléatoire (`UUID.randomUUID()`) y est écrit, avec un avertissement dans la console indiquant de le recopier dans
  les `config.yml` de KalGames et KalBingo - **sans afficher sa valeur**.
- La ligne qui affichait le jeton à chaque démarrage est supprimée.
- Aucun changement de comportement pour le proxy en service : son `relay.properties` contient déjà un jeton.

Aussi dans cette version : commentaires de `ActiveGameRegistry` et `RelayHttpServer` corrigés (ils disaient
qu'aucune fin de partie Bingo n'existait et que DELETE n'était appelé nulle part : KalBingo l'appelle en fin de
partie et à l'abandon depuis 0.1.12 / 0.1.14). Les « Pour reprendre » plus haut qui disent le contraire sont
historiques.

À déployer en même temps que KalGames 1.12.4 et KalBingo 0.1.23 (même nettoyage, voir leurs JOURNAL) - pas urgent.
**Statut : déployé sur le proxy le 24/09/2026 (démarrage vérifié dans le journal).**

## 1.2.0 — /server réservé aux admins (26/09/2026)

**Demande de LeKiwi06** : « désactiver la commande /server pour les joueurs (pas les admins) sur tous les serveurs ».
- `/server` est une commande du proxy (Velocity), autorisée à tous par défaut. Le proxy n'a pas de gestionnaire de
  permissions (pas de LuckPerms) et ne connaît pas les opérateurs des serveurs : KaliumRelay bloque `/server` (et
  `/velocity:server`) pour tout joueur absent de la liste **`admins`** de `relay.properties` (pseudos ou UUID séparés
  par des virgules, insensible à la casse ; **LeKiwi06,Maaxster** par défaut, ajoutée au fichier au premier démarrage),
  avec un message (« réservée aux admins, utilise la boussole ») ; la commande est aussi retirée des suggestions (Tab).
- Pour ajouter un admin : modifier `admins=` dans `plugins/kaliumrelay/relay.properties` sur le proxy, puis
  redémarrer le proxy.
- Compilation : adventure-api / adventure-key / brigadier ajoutés au classpath (déjà dans `outils-build/libs`).
**Déployé sur le proxy le 26/09/2026 à 7 h 21** (1.1.1 dans `_removed-kaliumrelay-1.1.1/`). **Statut : non testé en jeu.**

