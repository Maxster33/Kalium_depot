# Journal de session — KalGames (Kal-Games)

Ce fichier sert de mémoire de reprise entre deux sessions : contexte du projet, règles à respecter, et
historique de ce qui a été fait. À lire en premier au début d'une nouvelle session si le fil de conversation
n'est pas disponible.

## Contexte général du réseau

- Réseau Minecraft : proxy Velocity `Kalium.mine.fun` + serveurs **Lobby**, **Kixster**, **Kal-Games**.
- Serveurs Paper 26.2, hébergés sur Minestrator.
- Plugin développé ici : **KalGames** (hub + mini-jeux de Kal-Games), source dans `/home/claude/KalGames`.
- Build : `/home/claude/KalGames/build.sh` (ECJ, cible Java 21, écrit le jar dans `/mnt/user-data/outputs/`).
  Penser à synchroniser manuellement `VERSION=` dans `build.sh` **et** `<version>` dans `pom.xml` avant de
  builder (le `pom.xml` n'est pas lu par `build.sh`, c'est juste cosmétique/cohérence).

## Règles de déploiement à toujours respecter

- Le déploiement se fait via WinSCP (contrôle à distance de l'ordinateur de l'utilisateur), sites enregistrés :
  `kalgames@7021` (prod Kal-Games), `Kal-Test-Dev@5038` (test, volontairement en retard, ne pas toucher sauf
  demande explicite), `kixster@7003`, `lobby@7002`, `ProxyVelocity@7018`.
- Ne **jamais** redémarrer les serveurs prod soi-même : l'utilisateur s'en charge toujours lui-même après un
  déploiement.
- Remplacement d'un jar : déplacer (jamais supprimer) l'ancien jar dans un dossier dédié
  `/plugins/_removed-<nom-plugin>-<ancienne-version>/` (WinSCP : sélectionner l'ancien jar, Shift+F6
  "Déplacer vers...", corriger le chemin de destination proposé par défaut) **avant** d'envoyer le
  nouveau. Convention utilisée depuis la 1.9.5/1.10.0 (remplace l'ancienne convention ".bak" utilisée
  jusqu'à la 1.9.4, encore visible telle quelle sur le serveur pour ces vieux jars — ne pas y toucher).
- Toujours re-zoomer (`computer_zoom`) avant/après chaque sélection, renommage ou envoi pour vérifier qu'on
  agit sur le bon fichier (des clics mal ciblés sur la mauvaise ligne sont déjà arrivés par le passé).
- Ne **jamais** taper de caractères accentués dans l'éditeur intégré de WinSCP (mojibake garanti sur les
  fichiers `lang.yml` / `config.yml`) : si un texte accentué doit changer sur le serveur, le faire dans le
  code source Java (recompilé), pas en éditant le fichier YAML en place via WinSCP.
- Kixster n'a pas KalGames/KaliumMenu installés : ne jamais supposer qu'il en a besoin sauf demande explicite.
- `Kal-Test-Dev@5038` tourne une vieille version de KalGames, volontairement laissée telle quelle.
- Après chaque déploiement : envoyer un message récapitulatif en français à l'utilisateur (ce qui a changé,
  et rappeler qu'un redémarrage de Kal-Games est nécessaire).

## État au 22/09/2026 (historique - l'état actuel est dans `REPRISE_PROJET.md` et dans les dernières sections ci-dessous)

- Version déployée à cette date (`kalgames@7021`) : **KalGames 1.9.5**, jar en place dans `/plugins/`,
  ancien jar renommé en `.bak`.
- `build.sh` et `pom.xml` étaient alors sur `1.9.5`.
- **22/09/2026 : tous les serveurs ont été redémarrés par l'utilisateur, et il confirme que tout fonctionne
  très bien.** Les changements 1.9.0 → 1.9.5 sont donc vérifiés et validés en jeu (spectateur : vol libre,
  spawn au point de fin, sortie via /hub ; persistance des arènes ; suppression d'arène ; courses de bateau
  sans préchargement ; blocage de /kit et /kits).
- Aucune tâche en attente côté code à ce stade : tout ce qui a été demandé a été livré et confirmé
  fonctionnel. En attente de nouvelles demandes.

## Historique des versions (cette session)

### 1.9.0 — mode spectateur + persistance des arènes
- Bug spawn spectateur (chute, blocage) corrigé : spawn au point "gradins", téléportation cadrée dans
  l'arène (`clampSpectator`, depuis retiré en 1.9.3, voir plus bas).
- Nouveau système `matchSpectators` (participants ayant fini leur partie avant les autres) distinct des
  vrais spectateurs externes (`spectators`) : `GameInstance.becomeMatchSpectator(Player)`.
- Persistance des arènes générées entre redémarrages propres (marqueur `kalgames-clean-shutdown.txt` +
  signature de layout dans `InstanceWorld`, sauvegarde des cells de `ArenaPool` dans
  `instances-cells.yml`). Fallback sur l'ancien comportement (wipe complet) si arrêt sale ou config de
  grille changée.
- Scores des joueurs qui ne finissent pas à temps un mini-jeu : comptabilisés dans le classement
  (`RaceInstance.finishRace()`).
- Points par checkpoint/ligne d'arrivée : déjà configurables (aucun changement nécessaire, juste vérifié).

### 1.9.1 — fix suppression d'arène
- Bug découvert : la persistance de 1.9.0 a révélé que supprimer une arène ne supprimait pas réellement
  les blocs de ses copies générées (avant, le monde entier était wipé à chaque redémarrage donc ça ne se
  voyait pas). Ajout de `TemplateService.clearArea(...)` (nettoyage budgété par tick) utilisé uniquement
  pour les suppressions définitives (`ArenaPool.purgeArena` / `purgeMinigame`), pas pour le recyclage
  normal entre parties (coût perf).

### 1.9.2 — plus de préchargement pour les courses de bateau
- `BOAT_RACE` avait un défaut caché de 5 arènes préchargées (`prewarm-arenas`), unique parmi les
  mini-jeux (les autres sont à 0 par défaut). Réglage retiré du `MinigameType.BOAT_RACE` (disparaît du
  menu admin) + `ArenaPool.prewarm()` renvoie `0` en dur pour `BOAT_RACE`, quel que soit un éventuel
  réglage déjà enregistré côté serveur.

### 1.9.3 — spectateur : vol libre, spawn au point de fin, vol activé par défaut
- Suppression totale du cantonnement à la zone de l'arène pour les spectateurs (vrais spectateurs et
  participants ayant fini leur partie) : suppression de `GameInstance.clampSpectator(...)`, de
  `GameListener.onSpectatorMove`, et du filet de sécurité "chute sous l'arène" pour les spectateurs dans
  `GameInstance.tick()`.
- `becomeMatchSpectator` ne téléporte plus le joueur : son point d'apparition devient l'endroit exact où
  il a terminé sa partie (arrivée, élimination, timeout).
- Mode vol (`setAllowFlight(true)` + `setFlying(true)`) activé automatiquement dans
  `HubService.applySpectatorState`, pour tous les cas de mode spectateur.

### 1.9.4 — sortie du mode spectateur
- Constat : en mode spectateur vanilla, la barre d'objets est masquée par le client Minecraft — le menu
  (objet verrouillé cliquable) devient donc inatteignable, plus aucun moyen de "arrêter de regarder".
- `HubService.sendToHub` détecte maintenant si le joueur est un spectateur externe (`spectatorOf`) et
  délègue proprement à `leaveSpectator` (au lieu de laisser un état incohérent). Ça rend `/hub` et
  `/kalgames quitter` utilisables comme sortie de secours en toutes circonstances, y compris en spectateur.
- Message automatique envoyé dès l'entrée en mode spectateur (`HubService.applySpectatorState`) : rappelle
  de taper `/hub` pour arrêter de regarder.
- Textes de menus mis à jour (ne mentionnent plus l'ancien cantonnement à la zone, retiré en 1.9.3).

### 1.9.5 — blocage de /kit et /kits pour les joueurs
- PlayerKits2 (plugin tiers) ne sert dans KalGames que de bibliothèque de données pour les kits du vote
  PvP (`KitLibrary` relit ses fichiers `.yml`), mais sa propre commande `/kit` restait utilisable par tout
  le monde et permettait de s'équiper instantanément avec n'importe quel kit, hors vote et hors règles de
  partie.
- Nouveau listener `HubListener.onCommand` (`PlayerCommandPreprocessEvent`) : bloque `/kit` et `/kits`
  (avec ou sans préfixe `plugin:`) pour tout joueur n'ayant pas la permission `kalgames.bypass` (staff),
  avec message d'explication.

### 1.10.0 — intégration Bingo (côté kal-games)
- Nouveau mini-jeu Bingo, mais sur un **serveur séparé** (KalBingo, projet à part, voir son propre
  JOURNAL.md) : décision explicite de l'utilisateur (monde Overworld généré à la volée par seed, pas
  compatible avec le système de mondes-void/arènes-collées existant). KalGames ne fait que créer/rejoindre
  la partie et transférer les joueurs — toute la logique de jeu (équipes, mondes, validation...) vit dans
  KalBingo.
- Nouveau package `fr.kalium.games.bingo` :
  - `BingoParty`/`BingoPartyManager` : création (code à 4 caractères, seed aléatoire, durée lue depuis
    `config.yml`), jointure par code, transfert (`Connect` BungeeCord vers `bingo.server-name`).
  - `BingoNetworkListener` : répond aux demandes d'affectation envoyées par KalBingo (canal BungeeCord,
    sous-canaux `KalBingoAssignRequest`/`KalBingoAssignResponse`, `Forward`). Design **pull** (Bingo
    demande, kal-games répond) choisi avec l'utilisateur pour éviter un problème de fiabilité réseau : un
    message poussé vers un serveur sans aucun joueur connecté n'est pas garanti d'être livré.
  - `BingoCommand` (`/bingo create`, `/bingo join <code>`) : **commande de test minimale**, PAS encore
    intégrée au hub (pas d'entrée de menu dédiée) - à faire une fois le flux validé de bout en bout.
- Nouvelle section `bingo` dans `config.yml` : `server-name` (kixster pour les tests, facilement
  modifiable), `duration-seconds` (1h par défaut - **c'est ici, côté kal-games, que la durée de partie
  est modifiable par un admin**, transmise à KalBingo à chaque partie), `max-party-size`.
- Incidemment : `KalGames.java` reçoit maintenant aussi un canal BungeeCord **entrant**
  (`registerIncomingPluginChannel`), qui n'existait pas avant (seul le canal sortant était enregistré).
- Déployé sur kal-games (7021.mystrator.com) le 22/09/2026, ancien 1.9.5 déplacé (non détruit) dans
  `_removed-kalgames-1.9.5/`. **Redémarrage du serveur requis pour que le nouveau code prenne effet.**

### 1.10.1 — Bingo : vraie entrée de menu dans le hub (en plus des commandes)
- Demande explicite de l'utilisateur : "vraie intégration au menu, tout en laissant les commandes
  actuelles fonctionnelles" — `/bingo create` et `/bingo join <code>` restent disponibles telles quelles
  (`BingoCommand`, inchangé), le menu est un second point d'entrée vers exactement la même logique.
- `PlayerMenus.openGames()` (hub "Mini-jeux") : ajout d'un bouton "Bingo" manuel, à part de la boucle sur
  `plugin.repository().minigames()` — Bingo n'est PAS un `Minigame`/`GameInstance` classique (pas
  d'arène locale, la partie se déroule entièrement sur le serveur Bingo séparé une fois le joueur
  transféré), donc pas d'intégration dans le modèle `Arena`/`GameInstance`/`InstanceManager` existant
  (celui utilisé par PvP/Race) : ça aurait forcé Bingo dans un modèle pensé pour des mini-jeux locaux.
  Visible de tous les joueurs (pas admin-only), comme les commandes texte.
- Nouveau `PlayerMenus.openBingoMenu()` : bouton "Créer une partie Bingo" + formulaire "Rejoindre avec un
  code" (champ texte, même pattern que `openJoinPrivate`). `bingoCreate()`/`bingoJoin()` appellent
  directement `plugin.bingoParties().create()/join()/transferToBingo()` — **aucune logique dupliquée**
  avec `BingoCommand`, juste un autre appelant.

### 1.10.2 — Bingo : configuration du nombre d'équipes/joueurs à la création
- Demande explicite de l'utilisateur : pouvoir régler le nombre d'équipes et le nombre de joueurs par
  équipe **directement en créant la partie** depuis le hub, plutôt qu'avec des valeurs fixes.
- `PlayerMenus.openBingoCreate()` (remplace l'ancien bouton qui appelait `bingoCreate` directement) :
  formulaire avec deux champs numériques (`gui.number`, bornés par `bingo.max-team-count`/
  `max-team-size` dans `config.yml`, valeurs par défaut lues dans `bingo.default-team-count`/
  `default-team-size`) puis bouton "Créer la partie".
- `BingoParty`/`BingoPartyManager.create(host, teamCount, teamSize)` : la partie porte désormais son
  propre nombre d'équipes/joueurs par équipe (plus une constante globale), avec bornage défensif côté
  serveur (indépendant des bornes déjà imposées par le formulaire, au cas où l'appel viendrait d'ailleurs
  qu'un jour).
- Nouvelle section `config.yml` : `bingo.max-team-count`/`max-team-size` (4/4 par défaut — plafond
  confirmé dans le cahier des charges) et `bingo.default-team-count`/`default-team-size` (2/4).
- Protocole réseau étendu en conséquence (voir aussi JOURNAL.md de KalBingo) : `BingoNetworkListener`
  transmet maintenant aussi l'UUID de l'hôte et le `teamCount`/`teamSize` de la partie dans la réponse
  d'affectation (`KalBingoAssignResponse`), pour que KalBingo puisse les exploiter côté salle d'attente
  (menu joueur, validation des choix d'équipe).
- Déployé le 23/09/2026 sur kal-games (7021.mystrator.com) via WinSCP : ancien `KalGames-1.10.1.jar`
  déplacé (non détruit) dans `/plugins/_removed-kalgames-1.10.1/`, `KalGames-1.10.2.jar` envoyé dans
  `/plugins/`. **Redémarrage du serveur toujours requis.**

### 1.10.3 — dépôt de l'affectation sur le relais HTTP KaliumRelay
- Suite à un bug remonté par l'utilisateur : un second joueur qui rejoint une partie Bingo arrivait
  sur le modèle construit par l'admin au lieu de la salle d'attente, puis se faisait expulser
  quelques secondes après. Deux causes trouvées et corrigées côté KalBingo (voir son JOURNAL.md,
  section 0.1.9), plus une limite de fond identifiée : le canal de messagerie BungeeCord/Velocity
  utilisé par `BingoNetworkListener`/`AssignmentService` exige qu'AU MOINS UN joueur soit connecté
  sur kal-games pour livrer un message, dans les deux sens — en test à 2 comptes, si l'hôte ET le
  second joueur se retrouvent tous les deux sur Kixster, kal-games tombe à 0 joueur et la demande
  ne peut plus être livrée, quel que soit le délai ou le nombre de réessais.
- L'utilisateur a demandé si charger en continu une zone sur kal-games (sans joueur) résoudrait le
  problème — expliqué que non, la contrainte est au niveau du protocole (joueur connecté, pas chunk
  chargé). Proposé plusieurs options via AskUserQuestion ; l'utilisateur a choisi un plugin Velocity
  qui relaie les messages, confirmé pouvoir ouvrir un port sur l'hébergement Minestrator du proxy, et
  fourni le port **46199**.
- Nouveau projet **KaliumRelay** (plugin Velocity séparé, sur le proxy uniquement — voir son propre
  JOURNAL.md) : petit serveur HTTP autonome, complètement indépendant de toute connexion joueur.
  `BingoPartyManager.transferToBingo` dépose maintenant l'affectation du joueur sur ce relais
  (nouvelle méthode `pushAssignment`, appel HTTP asynchrone, non bloquant — échec loggé seulement)
  juste avant le transfert BungeeCord habituel, qui reste inchangé. KalBingo récupère cette
  affectation en interrogeant le relais en parallèle du canal BungeeCord existant (voir son
  JOURNAL.md, section 0.1.10) — le canal BungeeCord reste actif comme filet de sécurité.
- Nouvelles clés `bingo.relay-url` (`http://7018.mystrator.com:46199`) et `bingo.relay-token` dans
  `config.yml` — vides ou relais injoignable = fonctionnement inchangé par rapport à 1.10.2 (aucune
  casse).
- Déployé le 23/09/2026 sur kal-games (7021.mystrator.com) via WinSCP : ancien `KalGames-1.10.2.jar`
  déplacé (non détruit) dans `/plugins/_removed-kalgames-1.10.2/`, `KalGames-1.10.3.jar` envoyé dans
  `/plugins/`. `KaliumRelay-1.0.0.jar` déployé le même jour sur le proxy (`ProxyVelocity`, première
  installation) et `KalBingo-0.1.10.jar` sur Kixster — voir leurs JOURNAL.md respectifs. **Les TROIS
  serveurs (kal-games, Kixster, ET le proxy Velocity) doivent être redémarrés** — le proxy est un
  redémarrage supplémentaire par rapport aux déploiements précédents.

**Correctif du même jour, sans bump de version — `config.yml` déployé jamais mis à jour.** Après ce
déploiement, l'utilisateur a signalé que le bug persistait exactement à l'identique côté joueur qui
rejoint. Diagnostic par logs (téléchargés via WinSCP, pas de console) : `com.google.inject.CreationException`
dans `proxy-latest.log` montrait que KaliumRelay ne démarrait pas du tout sur le proxy (voir son
JOURNAL.md, section 1.0.1, pour cette partie) — MAIS même en supposant le relais réparé, `kalgames-latest.log`
ne montrait AUCUNE ligne `[Bingo]`, donc `pushAssignment` ne s'exécutait jamais. Cause : `saveDefaultConfig()`
(Paper/Bukkit) ne fusionne jamais de nouvelles clés dans un `config.yml` déjà présent sur le disque — le
fichier réellement déployé sur kal-games datait d'avant l'ajout de la section `bingo:` entière (créé à la
première installation, jamais régénéré depuis), donc `bingo.relay-url` résolvait au repli code (`""`),
qui désactive silencieusement le dépôt sur le relais. Une limite de fond, pas spécifique à ce plugin : à
surveiller pour CHAQUE future clé de config ajoutée ici ou ailleurs dans le workspace. **Corrigé** en
téléchargeant le `config.yml` réellement en place, en y insérant la section `bingo:` manquante (toutes
les autres clés déjà présentes/customisées par l'admin conservées telles quelles, notamment
`hub.use-world-spawn: false` et les coordonnées associées), puis en le ré-uploadant au même chemin via
WinSCP — pas de nouveau jar, juste le fichier de config corrigé en place.

## Repères techniques utiles (architecture KalGames)

- Monde des parties : `kalgames:instances` (void world, générateur custom `VoidGenerator`), géré par
  `InstanceWorld` (grille de slots/zones).
- `GameInstance` (classe de base) → `PvpInstance`, `RaceInstance` (parkour + courses de bateau).
- `InstanceManager` : registre central des parties, spectateurs externes (`spectating`), halls publics.
- `ArenaPool` : copies d'arènes pré-collées/recyclées (`Cell` : arenaId, minigameId, slot, template,
  pasted, owner, uses, discarded), persistées dans `instances-cells.yml`.
- `Minigame.getInt/getBool/getText(key, fallback)` : ordre de résolution = (1) réglage persisté par
  instance, (2) défaut déclaré dans `MinigameType` (`SettingSpec`), (3) fallback de l'appelant — un
  défaut de `SettingSpec` peut donc prendre le pas sur le fallback codé en dur si rien n'est persisté.
- `AdminMenus.openSettings()` génère automatiquement les champs du menu admin à partir de
  `minigame.type().settings()` : ajouter/retirer un `SettingSpec` suffit, pas besoin de toucher l'UI.
- `HubService` : état des joueurs (gradins, spectateur, hub), objets verrouillés, mode de jeu.
- `HubListener` : protection du hub, objets verrouillés, et désormais blocage de commandes.

## Pour reprendre la prochaine fois

- **Déployé sur kal-games (7021.mystrator.com) le 23/09/2026 via WinSCP** (voir section 1.10.3
  ci-dessus) : `KalGames-1.10.3.jar` en place dans `/plugins/`, ancien 1.10.2 archivé dans
  `_removed-kalgames-1.10.2/`, ET `config.yml` corrigé en place le même jour (section `bingo:`
  manquante ajoutée, voir "Correctif du même jour" ci-dessus). **Redémarrage des TROIS serveurs
  (kal-games, Kixster, ET le proxy Velocity) requis** pour activer le nouveau code (pas fait par
  Claude - pas d'accès console, juste SFTP) — le proxy est un ajout par rapport aux déploiements
  précédents (nouveau plugin KaliumRelay, lui aussi corrigé le même jour en 1.0.1, voir son
  JOURNAL.md).
- Prioritaire à tester une fois les trois serveurs redémarrés : le flux hôte + second joueur qui
  rejoint (le bug initial) — cette fois les DEUX causes trouvées (relais qui ne démarrait pas côté
  proxy + `config.yml` jamais mis à jour côté kal-games et Kixster) sont corrigées, contrairement au
  premier essai (1.10.3 seule) qui n'avait résolu ni l'une ni l'autre malgré un déploiement en
  apparence complet. Si le blocage se reproduit, vérifier que le port 46199 est bien accessible depuis
  kal-games ET Kixster vers le proxy (pas juste ouvert sur l'hébergement).
- Toujours en attente (demandé par l'utilisateur, pas encore implémenté) : lister les parties Bingo
  existantes sur kal-games pour pouvoir les rejoindre depuis le menu, en plus du code actuel.
- Une fois redémarré : tester la création d'une partie Bingo depuis le hub avec choix du nombre
  d'équipes/joueurs par équipe (nouveau formulaire, 1.10.2), puis le menu joueur côté salle d'attente
  (nether star, voir JOURNAL.md de KalBingo 0.1.8) pour vérifier que la configuration transmise
  correspond bien à ce qui a été choisi côté kal-games.
- Cinq retours utilisateur après le tout premier test du flux complet (23/09/2026), tous traités côté
  KalBingo (0.1.8) sauf la configuration équipes/joueurs (traitée ici, 1.10.2) : salle d'attente trop
  proche du modèle construit par l'admin, pas de configuration équipes/joueurs à la création (**cette
  version**), pas de moyen de configurer les équipes une fois dans la salle d'attente (nether star
  manquante), expulsion trop rapide de la salle d'attente, salle d'attente non protégée
  (casse/pose/interaction/monstres/PvP). Voir JOURNAL.md de KalBingo pour le détail des correctifs et les
  hypothèses/assomptions à confirmer par l'utilisateur.
- Le déclenchement du début de partie (compte à rebours/matchmaking) n'est pas encore construit côté
  KalBingo - actuellement déclenché manuellement par un admin, ou par l'hôte via le nouveau menu joueur
  (voir KalBingo 0.1.8).

## 1.10.4 — durée de partie choisie à la création, plafond admin dans Paramètres > Bingo

Demandes explicites de l'utilisateur (23/09/2026, en deux temps) :
> "il faut ajouter la possibilité de choisir la durée de la partie lorsqu'on la crée sur Kal"

puis, en précision :
> "Kalgames, le temps par defaut maximum doit être 1h, cette limite doit pouvoir etre changée via les
> parametres de kalgames. le createur de la partie doit pouvoir réduire le temps mais ne doit pas
> pouvoir dépasser le temps maximum défini par les opérateurs."

- **`BingoPartyManager.create(host, teamCount, teamSize, Duration duration)`** (nouvelle surcharge) :
  borne la durée demandée à `[bingo.min-duration-minutes, bingo.max-duration-minutes]` (5-60 min par
  défaut), exactement comme `teamCount`/`teamSize` sont déjà bornés à `max-team-count`/`max-team-size` —
  la validation cote SERVEUR (jamais de confiance au client) est ce qui garantit que l'hôte ne peut
  jamais dépasser le plafond admin, quel que soit ce que le formulaire aurait laissé passer. L'ancienne
  surcharge à 3 arguments délègue désormais à celle-ci avec `bingo.duration-seconds` comme repli (gardée
  pour `/bingo create`, la commande texte de test — voir `BingoCommand`, non modifiée).
- **`PlayerMenus.openBingoCreate()`** : nouveau champ `duration` (minutes) dans le formulaire de création
  de partie du hub, à côté de `teamCount`/`teamSize` — **pré-rempli avec `bingo.max-duration-minutes`**
  (le plafond admin), et borné par ce même plafond côté formulaire : l'hôte ne peut donc que RÉDUIRE la
  valeur affichée, jamais la dépasser (conforme au modèle "plafond unique" clarifié par l'utilisateur —
  pas de valeur par défaut séparée du plafond).
- **`AdminMenus.openBingoSettings()`** (nouvel écran admin) : un unique champ numérique
  `bingo.max-duration-minutes` (borné à `[bingo.min-duration-minutes, 24h]`), sauvegardé directement via
  `plugin.getConfig().set(...)` + `plugin.saveConfig()` (pas via le système `Minigame`/`SettingSpec`
  utilisé par les autres mini-jeux : Bingo n'est pas un `Minigame`, voir `BingoPartyManager`). Accessible
  depuis un nouveau bouton "Bingo" dans `AdminMenus.openHome()` (menu "Paramètres Kal-Games"), au même
  niveau que "Hub de Kal-Games" — répond directement à la demande "cette limite doit pouvoir etre changée
  via les parametres de kalgames".
- **`config.yml`** (section `bingo:`) : `max-duration-minutes` passé de `180` à `60` (1h, la nouvelle
  valeur par défaut demandée) ; commentaires réécrits pour clarifier que `duration-seconds` ne sert plus
  QUE de repli pour `/bingo create` (la commande texte), tandis que `max-duration-minutes` est
  désormais LE réglage admin-facing, modifiable en jeu via Paramètres > Bingo ; `min-duration-minutes`
  (5 min) reste un plancher technique non exposé (non demandé), pour éviter une partie dégénérément
  courte.
- Corrigé au passage : le repli Java codé en dur `plugin.getConfig().getInt("bingo.max-duration-minutes",
  180)` dans `BingoPartyManager.create()` était resté à `180`, incohérent avec le nouveau défaut `60` du
  `config.yml` — corrigé à `60` (n'affecte que le cas, très improbable après ce déploiement, où la clé
  serait totalement absente du fichier).

Côté KalBingo : aucun changement nécessaire pour cette moitié de la demande — la durée choisie ici est
simplement transmise telle quelle au lancement de la partie (`party.duration()`), comme c'était déjà le
cas. Voir en revanche le JOURNAL.md de KalBingo (0.1.12) pour la fin de partie automatique (temps écoulé
+ victoire), traitée dans la même session suite à une demande liée de l'utilisateur.

**Déployé le 23/09/2026 via WinSCP** : ancien `KalGames-1.10.3.jar` archivé (non détruit) dans
`/plugins/_removed-kalgames-1.10.3/`, `KalGames-1.10.4.jar` envoyé dans `/plugins/` sur kal-games.
`KalBingo-0.1.12.jar` déployé le même jour sur Kixster (voir son JOURNAL.md). **Redémarrage de kal-games
ET de Kixster requis** (le proxy/KaliumRelay n'a pas changé cette fois).

## Pour reprendre la prochaine fois (mise à jour, 1.10.4)

- Une fois kal-games et Kixster redémarrés : vérifier que le formulaire de création (menu "Bingo" du
  hub) propose bien un champ de durée pré-rempli à 60 min, réductible jusqu'à 5 min ; vérifier que
  Paramètres > Bingo (menu admin) permet de changer ce plafond (ex. 90 min) et que le formulaire de
  création reflète alors le nouveau plafond comme valeur pré-remplie/maximum.

## 1.10.5

**Liste des parties Bingo encore en salle d'attente, cliquables dans le menu** — demande explicite de
l'utilisateur : "il faut pouvoir voir les parties qui sont créées et encore en salle d'attente dans le
menu kalgames de manière à pouvoir la rejoindre facilement". Trois choix confirmés via
`AskUserQuestion` avant implémentation :
1. **Fiabilité de la liste** : ajout d'un signal réseau KalBingo → kal-games (recommandé et retenu)
   plutôt qu'une liste approximative côté kal-games seul.
2. **Parties pleines** : affichées mais non cliquables (montrent "(complet)"), pas masquées.
3. **Nombre max affiché** : 6 parties (les plus récemment créées), au-delà les plus anciennes ne
   s'affichent pas.

**Correctif de fond découvert en creusant le sujet** : `BingoPartyManager.remove(gameId)` existait déjà
mais n'était JAMAIS appelé — une partie restait indéfiniment dans `byCode`/`byGameId` côté kal-games
même après son démarrage réel sur Kixster (aucune conséquence visible avant cette fonctionnalité, la
liste n'existait pas encore, mais aurait rendu n'importe quelle liste de parties "ouvertes" fausse dès
le premier essai). Corrigé en branchant ce nettoyage sur le nouveau signal ci-dessous.

- **`fr.kalium.games.bingo.BingoPartyManager`** : nouvelle méthode `openParties(int limit)` — les
  parties encore présentes dans `byGameId` (donc ni démarrées ni annulées), triées de la plus récente
  à la plus ancienne, limitées à `limit`.
- **`fr.kalium.games.bingo.BingoNetworkListener`** : gère désormais un nouveau sous-canal
  `KalBingoPartyClosed` (en plus de `KalBingoAssignRequest` existant) — reçoit juste un `gameId` et
  appelle `parties.remove(gameId)` (donne enfin un appelant à cette méthode).
- **`fr.kalium.games.gui.PlayerMenus.openBingoMenu`** : sous les boutons "Créer"/"Rejoindre avec un
  code" existants, un bouton par partie encore en salle d'attente (nom de l'hôte, format équipes x
  taille, joueurs actuels/max) - clic direct = rejoint immédiatement (réutilise
  `BingoPartyManager.join` avec le code de la partie, aucune nouvelle logique de jointure) ; une partie
  pleine affiche "(complet)" et son clic se contente d'un message, sans inscrire le joueur.

**Côté KalBingo (voir son JOURNAL.md, 0.1.15)** : nouvelle classe `PartyStatusNotifier`, appelée par
`PartyStarter.start()` et `PartyCanceller.cancel()` juste après avoir retiré la partie de son propre
`PartyManager` - prévient kal-games via le même mécanisme "Forward" que l'affectation existante
(`KalBingoAssignRequest`/`Response`), donc pas de nouvelle dépendance réseau (pas besoin du relais HTTP
ici : le message part toujours d'un joueur réellement connecté à ce moment-là, l'hôte qui vient de
lancer/annuler).

**Aucune nouvelle clé `config.yml`** — réutilise `bingo.max-party-size` déjà existant (capacité max
d'une partie, déjà utilisé par `BingoPartyManager.join`).

**Déployé le 24/09/2026 via WinSCP** avec KalBingo 0.1.15 (les deux moitiés vont ensemble, l'une ne
fonctionne pas sans l'autre) - voir section déploiement plus bas / JOURNAL.md de KalBingo.

## Pour reprendre la prochaine fois (mise à jour, 1.10.5)

- Une fois kal-games ET Kixster redémarrés (les deux plugins doivent être à jour en même temps) :
  ouvrir le menu Bingo sur kal-games AVANT qu'aucune partie n'existe (liste vide, juste les deux
  boutons habituels), créer une partie sur un second compte, revenir dans le menu Bingo sur le premier
  et vérifier qu'elle apparaît et est cliquable (nom d'hôte, format équipes/taille, compteur de
  joueurs) ; cliquer dessus doit transférer directement vers Kixster sans taper de code.
- Vérifier la disparition de la liste : (1) une fois la partie LANCÉE sur Kixster (`/bingoadmin start`
  pour l'instant, en attendant l'étape interface/chronomètre de lancement automatique) - la partie ne
  doit plus apparaître dans le menu kal-games ; (2) une fois la partie ANNULÉE depuis le menu joueur
  Bingo (bouton "Annuler la partie", hôte uniquement) - même vérification.
- Vérifier l'affichage "(complet)" une fois le nombre max de joueurs atteint (`bingo.max-party-size`) :
  la partie doit rester visible dans la liste mais le clic ne doit rien faire d'autre qu'un message.
- Vérifier la limite d'affichage à 6 parties si plus de 6 tournent en même temps (créer 7+ parties de
  test) - seules les 6 plus récentes doivent apparaître.
- Toujours pas traité : le flux `PartyStatusNotifier` suppose qu'au moins un joueur de la partie
  fermée est encore connecté à Kixster au moment du démarrage/annulation pour acheminer le message
  (toujours le cas en pratique - c'est l'hôte qui déclenche l'action). Si ce cas limite s'avérait
  gênant en pratique (aucun joueur connecté au moment exact), la partie resterait visible côté
  kal-games jusqu'à ce qu'un mécanisme de nettoyage périodique soit ajouté - non fait pour l'instant,
  pas demandé.

## 1.10.6

**Deux correctifs rapportés par l'utilisateur après test de la 1.10.5/0.1.15** :

**1. Capacité affichée/appliquée fausse** : "il faut limiter le nombre de joueur possible dans une
partie à maximum = nombre d'équipe * nombre de joueurs par équipe. (on a testé avec 2 équipes de 1 et
la partie marquait 2/16 dans le menu KalGames)". Le code utilisait le plafond GLOBAL
`bingo.max-party-size` (16 par défaut) au lieu de la configuration choisie par l'hôte pour CETTE
partie précise.
- **`fr.kalium.games.bingo.BingoParty`** : nouvelle méthode `maxPlayers()` = `teamCount * teamSize`.
- **`fr.kalium.games.bingo.BingoPartyManager.join()`** : compare désormais `party.roster().size()` à
  `party.maxPlayers()` au lieu du plafond global.
- **`fr.kalium.games.gui.PlayerMenus.bingoPartyButton`** : idem pour l'affichage "actuel/max" et le
  statut "(complet)" dans la liste du menu Bingo.
- **`config.yml`** : `bingo.max-party-size` n'est plus lu nulle part - laissé en place (commentaire mis
  à jour) pour ne pas casser un fichier déjà déployé, peut être supprimé sans risque plus tard.

**2. Joueur qui abandonne puis rejoint se retrouve en salle d'attente fantôme** : "un joueur abandonne
la partie, ensuite il essaye de rejoindre la partie et ça le remet en salle d'attente. on dirait que
l'info du démarrage de la partie n'est pas arrivée sur KalGames". Correctif principal côté KalBingo
(voir son JOURNAL.md, 0.1.16) : un garde-fou refuse désormais de créer une salle d'attente fantôme pour
un `gameId` déjà actif, quelle que soit la cause exacte de la perte du signal `PartyStatusNotifier`.
Rien à changer ici côté KalGames pour ce point précis, mais **à surveiller** : si ce symptôme se
reproduit malgré le correctif KalBingo, vérifier en premier que les DEUX serveurs tournent bien avec
les dernières versions déployées (une désynchronisation de versions entre kal-games et Kixster
reproduirait exactement ce symptôme, puisque l'ancienne 1.10.4/0.1.14 ne savaient pas du tout gérer le
signal `KalBingoPartyClosed`).

**Déployé le 24/09/2026 via WinSCP**, en même temps que KalBingo 0.1.16 - **redémarrage des DEUX
serveurs requis, en même temps**, comme pour la 1.10.5/0.1.15.

## Pour reprendre la prochaine fois (mise à jour, 1.10.6)

- Refaire EXACTEMENT le test qui a révélé le bug : lancer une partie à 2 joueurs (2 équipes d'1), un
  des deux joueurs clique sur "Abandonner la partie" en cours de partie, puis essaie de rejoindre la
  même partie depuis kal-games (menu ou code) - il doit maintenant recevoir un message clair ("cette
  partie a déjà démarré...") et rester/revenir sur kal-games, PAS se retrouver dans une salle
  d'attente vide sur Kixster.
- Vérifier que "2 équipes de 1" affiche bien "x/2" (pas "x/16") dans la liste du menu Bingo, et que la
  partie passe bien en "(complet)" dès que 2 joueurs l'ont rejointe.
- Vérifier en conditions normales (sans abandon) que le flux habituel créer/rejoindre/lancer fonctionne
  toujours identique à avant ces deux correctifs.

## 1.10.7

**Bug rapporté par l'utilisateur** : "les parties qui sont terminées ne doivent plus apparaître dans
la liste des parties sur le menu de KalGames".

Cause (détail dans le JOURNAL de KalBingo 0.1.19) : le signal "partie fermée" de KalBingo passait
uniquement par le canal Forward, qui n'est livré ICI que si au moins un joueur est connecté sur
kal-games - au démarrage d'une partie, tout le monde est en général sur Kixster, donc il se perdait.

- **`bingo.BingoPartyManager.pollClosedParties()`** (nouveau) : toutes les 5 s (tâche lancée dans
  `KalGames.onEnable`), pour chaque partie encore listée, lit la clé `party-closed-<gameId>` sur le
  relais HTTP (même `bingo.relay-url` / `bingo.relay-token` que pour les affectations) ; si elle
  existe, la partie est retirée de la liste (`remove(gameId)`). Appels asynchrones, jamais sur le
  thread principal. Rien à configurer, aucun changement côté proxy (KaliumRelay).

À déployer avec KalBingo 0.1.19 (qui publie ce signal sur le relais).

**Déployé le 23/09/2026 via WinSCP** (1.10.6 archivée dans `_removed-kalgames-1.10.6/`), avec KalBingo 0.1.19.

## 1.11.0 — Rush, étape 1 (configuration)

**Demande explicite de l'utilisateur** : nouveau mini-jeu **Rush** (chaque équipe défend un lit ; ressources,
ponts, équipement, destruction du lit adverse, dernière équipe en vie). Cahier des charges complet donné en
deux étapes : étape 1 = modèle de mini-jeu configurable (arènes 2 ou 4 équipes, jusqu'à 4 joueurs par équipe,
toutes les règles dans les Paramètres, choix de l'arène par le joueur) ; étape 2 = objets, PNJ marchands et
moteur de jeu (voir le message d'origine, recopié dans la section "Rush — cahier des charges" plus bas).

Décisions confirmées via AskUserQuestion :
- **Une partie publique par arène** : Rush → choix de l'arène → jouer en public (la partie publique ouverte
  sur cette arène, créée au besoin ; dès qu'elle démarre, une nouvelle s'ouvre) / privée / regarder.
- **N'importe qui peut casser n'importe quel lit** (y compris le sien : "ramassé").
- **Monnaies : une zone par base** (2 coins), Bronze / Silver / Gold y apparaissent au hasard aux rythmes
  indiqués.
- **"Arènes en couche 300"** : rien à coder — l'utilisateur construit ses arènes vers Y=300 ; les copies sont
  recollées à la même hauteur, donc le plafond du monde (Y=319) limite déjà les constructions.

Ce qui est livré (1.11.0) :
- `MinigameType.RUSH` (non jouable pour l'instant : "configuration seule", invisible des joueurs) avec ses
  réglages : joueurs max par équipe (4), joueurs minimum en public (2), attente avant lancement (30 s), délai
  de réapparition (3 s), autodestruction des lits (15 min, 0 = jamais), intervalle Bronze/Silver/Gold en ticks
  (10 / 20 / 60 = 2 par s / 1 par s / 1 toutes les 3 s, par base), casser la carte (non), délai après victoire,
  points par victoire, arènes préchargées, parties privées max, spectateurs.
- `model.RushLayout` : équipes (bleue, rouge, jaune, verte), 7 PNJ (Trader, Trader #2, Rusher, Archer, Maçon,
  Armurier, Secret Trader) et points d'arène : salle d'attente + par base : apparition, lit (pied du lit,
  regard vers la tête), zone des monnaies (2 coins), un point par PNJ = 11 points par base. Bases bleue et
  rouge obligatoires ; jaune et verte : toutes les deux complètes (arène à 4 équipes) ou toutes les deux vides
  (arène à 2 équipes) — vérifié dans `Arena.missing`.
- `PointSpec.group` (nouveau, optionnel) + `AdminMenus` : les points d'un même groupe sont réunis dans un
  sous-menu ("Base bleue (x/11)"...) au lieu de ~45 boutons à la suite. Aucun effet sur les autres mini-jeux.

### Rush — cahier des charges (étape 2, à implémenter)

Monnaies : Bronze = brique renommée, Silver = lingot de fer renommé, Gold = lingot d'or renommé.
PNJ (mêmes échanges pour toutes les équipes, un exemplaire par base, positions dans les paramètres) :
- TRADER : Bronze x10 → Silver x1 ; Silver x3 → Gold x1 ; Gold x10 → Émeraude x1.
- TRADER #2 : Bronze x4 → Steak cuit x2 ; Silver x31 + Émeraude x1 → Enclume ; Silver x3 → Pomme dorée ;
  Émeraude x30 → Lit.
- RUSHER : Silver x5 → Épée en or Sharpness I ; Silver x10 → Sharpness II ; Silver x15 → Sharpness II +
  Knockback I ; Émeraude x1 → Sharpness IV ; Émeraude x3 → Sharpness IV + Knockback I ; Silver x10 → TNT ;
  Gold x2 → Briquet ; Bronze x32 → Coffre.
- ARCHER : Gold x3 → Arc Power I ; Gold x7 → Power I + Infinity I ; Gold x13 → Power II + Infinity I ;
  Émeraude x1 → Flèche ; Émeraude x3 → Livre Flame I ; Émeraude x3 → Livre Infinity I.
- MAÇON : Bronze x1 → Sable rouge x4 ; Bronze x4 → Glowstone x2 ; Bronze x4 → End Stone ; Bronze x4 → Pioche
  en bois Efficiency I ; Silver x2 → Pioche en pierre Efficiency II ; Gold x1 → Pioche en fer Efficiency III ;
  Bronze x4 → Verre teinté x2 (couleur de l'équipe).
- ARMURIER : Bronze x8 → Bottes / Pantalon / Casque en cuir Protection II (couleur de l'équipe) ; Silver x3 /
  x7 / x12 → Plastron en cotte de mailles Protection I / II / III.
- SECRET TRADER : Silver x1 → Fiole d'XP ; Émeraude x3 → Carroteur3000 (carotte sur un bâton Knockback II) ;
  Silver x8 → Boules de neige x16 ; Émeraude x2 → Coffre de l'End ; Gold x3 → Toile d'araignée.
Règles : parties simultanées publiques ou privées ; à l'arrivée, choix de l'équipe (bleu, rouge, jaune, vert),
équipe pleine = non rejoignable ; un lit posé dans chaque base ; tout joueur peut définir son point de spawn
sur n'importe quel lit ; à la mort : spectateur (vol), réapparition après 3 s (réglable, compte à rebours
visible) si son point de spawn est toujours sur un lit — sinon (lit cassé, ramassé, replacé) pas de
réapparition ; déconnexion = éliminé ; /hub ou /lobby à tout moment = quitte la partie ; lits autodétruits
après 15 min (réglable) ; monnaies générées dans la zone de chaque base : Bronze 2/s, Silver 1/s, Gold 1/3 s.

**Déployé le 23/09/2026** (1.10.7 archivée dans `_removed-kalgames-1.10.7/`).

## 1.12.0 — Rush, étape 2 (jeu jouable)

Suite du cahier des charges Rush (voir 1.11.0). Décision supplémentaire via AskUserQuestion : **à la mort,
tout l'inventaire tombe au sol** (objets, armure, monnaies), le joueur réapparaît les mains vides.

Nouveaux fichiers :
- `game.RushInstance` — moteur. Salle d'attente (point "stands") avec choix d'équipe (menu ouvert à
  l'arrivée, équipe pleine = refusée) ; public : lancement automatique dès `min-players` (compte à rebours
  `gather-seconds`) à condition d'avoir au moins 2 équipes différentes ; privé : lancé par l'hôte. Joueurs sans
  équipe au lancement → équipes les moins remplies. Au lancement : lit posé dans la base de chaque équipe
  présente (orientation = regard du point "lit"), 7 PNJ par base présente, inventaire + coffre de l'End vidés,
  téléportation au point d'apparition, point de réapparition = lit de sa base.
  Lits : chaque joueur est lié à UN lit précis ; clic droit sur n'importe quel lit = point de réapparition
  déplacé dessus (lits de décor de la carte compris). Un lit cassé / explosé / ramassé puis reposé = nouveau
  lit : les points de réapparition liés à l'ancien sont perdus. N'importe qui peut casser n'importe quel lit.
  Annonce + titre quand un lit de base est détruit. Autodestruction de tous les lits après
  `bed-destroy-minutes` (avertissement 1 min avant) ; ensuite, plus aucun lit ne peut être posé.
  Mort : dégâts mortels interceptés (pas d'écran de mort) → inventaire au sol (perdu si chute dans le vide),
  spectateur en vol à l'endroit de la mort ; réapparition sur son lit après `respawn-seconds` (titre avec
  compte à rebours) si le lit existe toujours (vérifié à la mort ET à la fin du compte à rebours), sinon
  éliminé. Mort attribuée au dernier agresseur (10 s) pour les chutes. Déconnexion, /hub, /lobby = éliminé
  (inventaire lâché). Équipe éliminée quand plus aucun joueur vivant ni en attente de réapparition ; la
  dernière équipe en jeu gagne (`points-win` à chacun de ses joueurs), puis retour au hub après
  `end-delay-seconds`.
  Monnaies : dans la zone de chaque base présente, au hasard entre les 2 coins (hauteur = le plus haut des
  deux coins), selon `bronze/silver/gold-interval-ticks`.
  Action bar : équipes (✔/✘ lit de base, joueurs encore en jeu) + temps avant autodestruction des lits.
- `game.RushItems` — monnaies (Bronze = brique, Silver = lingot de fer, Gold = lingot d'or, renommés) et les 45
  échanges. Menu d'échange vanilla construit pour chaque acheteur : verre teinté, armure en cuir et lit (Trader
  #2) à la couleur de SON équipe (vert = lime). Échanges illimités (echanges_PNJ_Rush.txt : "les utilisations
  déjà effectuées ne sont pas prises en compte").
- `listener.RushListener` — PNJ (clic → échanges), clic sur lit, pose/casse de lits, TNT (même règle que la
  casse : blocs posés + lits, ou toute la carte si `break-map`), dégâts mortels.

Modifié : `MinigameType.RUSH` jouable ; `InstanceManager` (création RushInstance, `joinPublicArena` = une
partie publique par arène, rouverte dès que la précédente démarre ou est pleine ; `spectatable` liste toutes
les parties publiques en cours) ; `ConnectionListener.onRespawn` (mort non interceptée, ex. /kill → retour en
spectateur) ; `PlayerMenus` (Rush → liste des arènes "(2/4 équipes)" → partie publique / privée ; menu
"Choisir mon équipe" ; pas de file d'attente pour Rush) ; `KalGames` (enregistre RushListener).

Décisions prises sans question (faciles à changer) : pas de tir ami entre coéquipiers ; les bases des équipes
sans joueur restent vides (ni lit, ni PNJ, ni monnaies) ; le coffre de l'End est vidé au lancement et au
départ (il est commun à tout le serveur) ; échanges non modifiables depuis les Paramètres (seuls les réglages
listés en 1.11.0 le sont).

À vérifier en jeu en priorité : que les PNJ acceptent bien les monnaies (le menu d'échange vanilla compare
l'objet exact, nom compris — les monnaies générées et demandées sont fabriquées de la même façon).

**Déployé le 23/09/2026 via WinSCP** (1.11.0 archivée dans `_removed-kalgames-1.11.0/`). Redémarrage de kal-games requis. Non testé en jeu au moment du déploiement.

## 1.12.1 — Rush : carte remise à l'identique, casse libre

**Demandes explicites de l'utilisateur** : "il faudrait que comme pour les autres jeux la map soit vidée et
réutilisée, donc il faut remplacer tous les blocs qui ne sont plus comme le modèle de la map, y compris les
blocs d'air" puis "l'option protection casse est inutile. les joueurs peuvent casser des blocs ou en poser mais
la map doit revenir à son état initial après chaque fin de partie".

- **Remise à l'identique complète** (`TemplateService.reconcile`, appelée par `ArenaPool.release` pour Rush) :
  jusqu'ici une copie réutilisée n'était remise en état que pour les blocs SUIVIS pendant la partie (posés,
  cassés, explosés...) ; ce qui échappait au suivi (réactions en chaîne, moitié d'un lit de décor soufflée par la
  TNT...) restait. Désormais, après chaque partie Rush, TOUTE la zone de l'arène est comparée au modèle — une
  position absente du modèle doit être de l'air — et chaque bloc différent est remplacé. Lecture par instantané
  de chunk, étalée sur plusieurs ticks (budget `instances.paste-blocks-per-tick`). La copie n'est proposée à une
  nouvelle partie qu'une fois la vérification terminée ; le nombre de blocs corrigés est écrit dans la console
  ("Arène X remise à l'identique du modèle : N bloc(s) corrigé(s)"). Réservé à Rush pour l'instant (les arènes
  de course peuvent être immenses) — extensible aux autres jeux si demandé. Limite : le contenu des coffres de
  la carte n'est pas comparé (le modèle ne stocke que les blocs).
- **Casse libre** : réglage "Casser les blocs de la carte" supprimé pour Rush ; tout bloc de l'arène est
  cassable à la main et par la TNT (`RushInstance.canBreak` / `explosionMayBreak` : dans les limites de
  l'arène), **sauf les zones d'apparition des monnaies** (demande explicite : "seule la zone d'apparition des
  monnaies doit être protégée contre la casse") : blocs entre les deux coins de chaque base, sol juste en
  dessous compris (`inCurrencyZone`). Le réglage `break-map` du PvP n'est pas concerné.
- **PNJ impossibles à tuer** (demande explicite) : déjà invulnérables ; en plus, gravité désactivée (ils ne
  tombent plus si on casse le bloc en dessous, maintenant que la casse est libre) et tout dégât sur un PNJ est
  annulé (`RushListener.onShopDamage`).

**Déployé le 23/09/2026 via WinSCP** (1.12.0 archivée dans `_removed-kalgames-1.12.0/`). Redémarrage de kal-games requis.

## 1.12.2 — Recapture d'arène : parties en cours préservées

**Demande explicite de l'utilisateur** : "lorsqu'on refait une capture d'arène suite à une modification il
faut que les arènes d'instances déjà générées se suppriment, sauf si une partie est en cours alors elle devra
être supprimée à la fin de la partie".

Avant : la capture FERMAIT toutes les parties de l'arène (joueurs renvoyés au hub) puis supprimait les copies
libres — et ce AVANT la capture elle-même, si bien qu'une pré-génération pouvait recoller l'ancien modèle
entre-temps.
- `AdminMenus.capture` : plus aucune partie fermée ; APRÈS une capture réussie, `InstanceManager.
  retireArenaCopies` supprime (blocs réellement effacés) les copies libres de l'ancien modèle et relance la
  pré-génération. Si la capture échoue, rien n'est supprimé (l'ancien modèle reste valable).
- `ArenaPool.release` : à la fin d'une partie dont la copie est d'un ancien modèle, la copie est supprimée avec
  effacement réel des blocs (auparavant simple déchargement). Idem pour une copie libre périmée trouvée par
  `acquire`.
- Une salle d'attente déjà ouverte sur l'ancienne copie (partie pas encore lancée) compte comme une partie en
  cours : elle se joue sur l'ancienne version, puis sa copie est supprimée.
- "Supprimer le modèle" et "Supprimer l'arène" ferment toujours les parties (inchangé, pas demandé).

**Déployé le 23/09/2026 via WinSCP** (1.12.1 archivée dans `_removed-kalgames-1.12.1/`), avec KaliumMenu 1.5.0.

## 1.12.3 — Correctif : serveur arrêté pendant une capture d'arène

**Demande explicite de l'utilisateur** : "le serveur crash quand on fais des capture de zone (on a tenté de
mettre à jour le parkour) fais en sorte que le processus soit plus lent si nécessaire afin d'éviter que le
serveur crash".

**Cause (journaux kal-games du 23/09, 18:33 et 18:43)** : arrêt par le watchdog de Paper (serveur bloqué plus de
10 s). La capture elle-même se terminait ; c'est l'étape suivante, ajoutée en 1.12.2
(`retireArenaCopies` → `ArenaPool.purgeArena` → `TemplateService.clearArea`), qui posait d'un coup un ticket sur
les 96 × 96 = 9 216 chunks de chaque emplacement à effacer. `addPluginChunkTicket` charge (et génère) chaque chunk
de façon SYNCHRONE : des dizaines de milliers de chargements dans un seul tick.

Changements (tous dans `TemplateService`) :
- **Effacement d'une ancienne copie** (`clearArea`) : file d'attente, une copie à la fois. Chaque chunk est
  demandé en arrière-plan SANS génération (`getChunkAtAsync(x, z, false)` : un chunk jamais généré n'a rien à
  effacer), 16 à la fois ; il est vidé puis relâché (sauvegardé vide). Budget : `instances.wipe-blocks-per-tick`
  (20 000 par défaut, la moitié du collage). Une ligne dans la console à la fin de chaque effacement.
- **Collage d'une copie** (`paste`) : les chunks de l'arène ne sont plus chargés de façon synchrone au départ ;
  ils sont chargés en arrière-plan (16 à la fois) avant le collage. Chaque chunk de l'arène est vidé avant le
  collage (quasi gratuit s'il est déjà vide) : un reste d'ancienne copie sur le disque ne laisse plus de blocs.
  `release` retire les tickets de tout l'emplacement (un collage interrompu peut en avoir posé hors de l'arène).
- **Capture** : chunks chargés en arrière-plan (8 à la fois, jamais générés), lus dans une photo du chunk
  (`ChunkSnapshot`), sections vides sautées, palette mise en cache par état de bloc. Le rythme est un nombre de
  blocs lus par tick, avec reprise au milieu d'un chunk : nouveau réglage `arenas.capture-blocks-per-tick`
  (30 000 par défaut), qui REMPLACE `arenas.capture-chunks-per-tick` (ignoré désormais). Progression dans la
  console toutes les 10 s.
- `clearChunk` : lecture dans la photo du chunk, écriture seulement des blocs non vides.

Les nouvelles clés ont une valeur par défaut dans le code : inutile de modifier le config.yml déployé.

## 1.12.4 — jeton du relais retiré du config.yml fourni (24/09/2026)

**Demande de LeKiwi06** : ne plus jamais publier le jeton du relais dans le dépôt public (voir
`KaliumRelay/JOURNAL.md`, 1.1.1, et `REGLES.md`, section 2).

- `config.yml` fourni avec le plugin : `bingo.relay-token: ""` (au lieu de l'ancien jeton fixe) et commentaire
  « à renseigner uniquement dans le config.yml du serveur ». Aucun changement de code.
- Le `config.yml` déployé sur kal-games n'est pas concerné (le plugin ne réécrit jamais un fichier existant) : il
  contient déjà le nouveau jeton, mis à la main le 24/09/2026.

Aussi dans cette version (même jour, demande de LeKiwi06) : commentaires obsolètes corrigés (la commande
`/bingo` n'est plus un « placeholder de test » : le menu Bingo du hub existe) et description de `/bingo` dans
`plugin.yml` ; titre « État actuel » en tête de ce journal renommé en « État au 22/09/2026 ». Aucun
changement de comportement.

À déployer avec KaliumRelay 1.1.1 et KalBingo 0.1.23 - pas urgent. **Statut : compilé, non déployé.**

## 1.13.0 — le Bingo sort de KalGames (plugin KG_Bingo), menus ouverts aux autres plugins (24/09/2026)

**Demande de LeKiwi06** : appliquer la règle « un plugin = un rôle » (`REGLES.md`, section 2) au Bingo : « il faut
donc isoler KG_bingo (tout ce que kal-games doit gérer pour le bingo) de kal_games ». Étape A d'un découpage en
deux étapes (étape B : renommer KalBingo en KG_BingoGame). Choix validé : une « prise » générique plutôt qu'un
bouton Bingo écrit en dur dans KalGames.

- **Retiré** (déplacé tel quel dans le nouveau plugin `KG_Bingo`, voir son JOURNAL) : package
  `fr.kalium.games.bingo` (`BingoCommand`, `BingoNetworkListener`, `BingoParty`, `BingoPartyManager`), menus Bingo de
  `PlayerMenus` (bouton du hub, création, liste des parties, jointure), écran Paramètres > Bingo d'`AdminMenus`,
  section `bingo:` de `config.yml`, commande `/bingo` de `plugin.yml`, canal BungeeCord entrant (ne servait qu'au
  Bingo), tâche `pollClosedParties`.
- **Ajouté** : `gui.MenuEntry` + `PlayerMenus.addGameEntry/removeGameEntry` (boutons du menu Mini-jeux, après les
  mini-jeux de KalGames) et `AdminMenus.addSettingsEntry/removeSettingsEntry` (accueil des Paramètres, après « Hub de
  Kal-Games », clic réservé aux modérateurs). Ces prises suivront le menu quand il sortira dans `KG_Menu` (prévu).
- Comportement pour les joueurs inchangé si KG_Bingo est installé (même bouton, même place, mêmes textes). Sans
  KG_Bingo, le bouton Bingo n'apparaît simplement plus.
- Vérifié : les autres classes compilées sont identiques à 1.12.3 (seules `KalGames`, `PlayerMenus`, `AdminMenus`
  changent, `MenuEntry` est nouvelle). Inclut les changements de 1.12.4 (jamais déployée).

**Déploiement** : obligatoirement **en même temps que KG_Bingo 1.0.0** (sinon le Bingo disparaît du hub). Le
`config.yml` déployé sur kal-games garde sa section `bingo:` : sans effet désormais, à recopier dans
`plugins/KG_Bingo/config.yml` (jeton compris) avant le redémarrage. **Statut : déployé sur kal-games le 24/09/2026 avec KG_Bingo 1.1.0, non testé en jeu.**

## 1.14.0 — classements sortis dans KG_ScoreBoards (24/09/2026)

**Demande de LeKiwi06** : découpage de KalGames, les classements deviennent le plugin autonome **KG_ScoreBoards**
(voir son JOURNAL). Aucun changement pour les joueurs.
- Retirés de KalGames : `StatsService`, `BoardService`, `RankingMenus` (déplacés). KalGames dépend désormais de
  KG_ScoreBoards (`depend:`) ; il lui fournit ses mini-jeux comme classements (nom, type parcours / course de bateau),
  sa règle « modérateur » (`kalgames.admin`) et le monde des parties (pas de panneau dedans).
- Inchangés : `ScoreBridge` (attribution des points, commandes du datapack, `stats.exclude-operators`), limite des
  parties privées classées (`stats.count-private-games`, `stats.private-daily-limit`), menus des mini-jeux (le bouton
  Classements ouvre les menus de KG_ScoreBoards, « Retour » revient au mini-jeu).
- Les clés `stats.timezone` et `boards.scale` sont désormais lues dans la config de KG_ScoreBoards.

**Déploiement** : obligatoirement avec KG_ScoreBoards 1.0.0 (migration des données dans son JOURNAL).
KG_Bingo 1.1.0 fonctionne sans changement. **Statut : déployé sur kal-games le 24/09/2026, démarrage vérifié.**

## 1.15.0 — interfaces dans le catalogue de KLM_Menu (24/09/2026)

- Déclare **« Hub Kal-Games »** (joueurs : menu du hub) et **« Paramètres Kal-Games »** (admins : mini-jeux,
  arènes, kits, hub) dans le catalogue de KLM_Menu ; fournit la liste de ses mini-jeux à KG_ScoreBoards (ils
  apparaissent dans « Classements »). Dépend de KLM_Menu. Rien d'autre ne change.
**Déploiement** : avec KLM_Menu 2.0.0 et KG_ScoreBoards 1.1.0. **Statut : déployé le 24/09/2026, non testé en jeu.**

## 1.15.1 — correctif : boussole du hub après le renommage de KaliumMenu (24/09/2026)

**Bug vu par LeKiwi06** : plus de boussole dans le hub de kal-games après le déploiement de KLM_Menu 2.0.0. Cause
(erreur de Claude : les mentions de l'ancien nom n'avaient pas été recherchées dans tout le dépôt avant le
renommage) : KalGames redonne la boussole au hub (après avoir vidé l'inventaire) seulement si un plugin nommé
« KaliumMenu » est actif. Corrigé : `KLM_Menu` (ou l'ancien nom) ; l'outil « créer un kit depuis mon inventaire »
écarte aussi les boussoles à la nouvelle étiquette (`klm_menu:menu_compass`). `softdepend` : KaliumMenu retiré
(KLM_Menu est déjà dans `depend`). **Statut : déployé sur kal-games le 24/09/2026 (redémarrage à faire).**

## 1.16.0 — menus du hub dans KG_Menu (24/09/2026)

**Demande de LeKiwi06** : menu du serveur kal-games séparé (KG_Menu), boussole gérée par KLM_Menu. Les protections
de zone du hub restent dans KalGames (« on s'en occupera plus tard »).
- Retirés : le cadre de l'accueil joueur (liste des jeux) et de l'accueil Paramètres (prises `MenuEntry` supprimées),
  l'objet « Mini-jeux » du hub (dans KG_Menu ; les anciens encore en inventaire ouvrent KG_Menu).
- KalGames fournit à KG_Menu : ses mini-jeux (même contenu qu'avant), « Mini-jeux Kal-Games » dans Paramètres (son
  ancien accueil admin : mini-jeux, arènes, kits, hub, parties en cours, recharger ; titre « Mini-jeux Kal-Games :
  paramètres » ; son bouton « Retour » ramène aux Paramètres de KG_Menu) et le menu de la partie en cours.
- Au hub : objet du hub donné par KG_Menu, boussole par KLM_Menu (`giveNavigation`) - plus de commande console.
- N'inscrit plus rien directement dans KLM_Menu (c'est KG_Menu qui y apparaît). `items.games` retiré de la config
  (voir `hub-item` dans celle de KG_Menu). Dépend de KG_Menu.
**Déploiement** : avec KG_Menu 1.0.0, KLM_Menu 2.1.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0. **Statut : déployé sur
Kal-Games (7001) le 24/09/2026 par LeKiwi06 (anciens jars et configs dans les `_removed-…` de chaque plugin), testé et
confirmé par LeKiwi06 le 24/09/2026** : menus du hub, course de bateau (grille, menu de la partie), Parkour,
classements, Java et Bedrock. Constaté : les joueurs Bedrock accélèrent plus vite et gardent mieux leur vitesse en
virage (voir `KG_BoatRace/CAHIER_DES_CHARGES.md`).
