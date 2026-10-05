# Archive des comptes rendus de REPRISE_PROJET.md

Anciens comptes rendus, déplacés ici selon `REGLES.md` (section 5). À consulter uniquement en cas de doute ou
d'oubli : l'état actuel du projet est dans `REPRISE_PROJET.md`.

---

### 2026-10-03 — Maxster33

*(Session du 03/10/2026, de 8 h 40 à 9 h 50. Fin de session : réservations libérées.)*

**Bingo, déployé à 09:41 (Serveur Jeux) et 09:44 (Kal-Games), non testé, actif après redémarrage des deux
serveurs** : KG_BingoGame 0.8.5 (+ nouveau `objectives.yml`), KG_Bingo 1.7.0, KG_ScoreBoards 1.9.0 (à garder ensemble).
- 2 lancements ratés ce matin : grille à 25 difficiles alors qu'il n'en existait que 18 -> **7 difficiles ajoutés** (25) ;
  la partie restait ensuite bloquée (« existe deja ») -> corrigé, l'hôte peut relancer et voit la raison.
- **2 parties par heure** et par joueur (60 dernières minutes, opérateurs non limités).
- **Abandon** (bouton ou déconnexion > 10 min) : 0 point de la partie et **-15 au classement** (total négatif possible).
- **Une seule équipe** : aucun bonus « en 1er ».
- **Contre la montre** (1 équipe) : 10 min, +5 min par objectif, grille complète obligatoire ; victoire = bonus habituel
  + 1 pt par 20 s restantes ; temps écoulé = défaite, points des objectifs gardés.
- **Gel de 5 à 6 s à la création d'une partie** (création de l'overworld de chaque équipe sur le thread principal) :
  point d'apparition fourni par un générateur (terrain normal) puis placé sur la terre ferme ; **réserve de 2 mondes**
  créée seulement sans partie en cours, utilisée par une partie créée pendant une autre.
- Erreurs « Impossible de publier ... : null » cette nuit (00:55 à 01:13) : relais injoignable (proxy redémarré
  probablement), reparti seul ; pas un bug. Crédit des points sur Kal-Games non vérifié.

**Correction des classements Bingo (17:04, Kal-Games arrêté)** : 27 parties à une seule équipe jouées avant la 0.8.5
avaient les bonus « en 1er » (objectif, bingo). Détail impossible à recalculer (seuls les totaux sont gardés) : **40 %
des points hors XP retirés** (choix de Maxster33), dans `stats.yml` (général, octobre), `archives/2026-09.yml` et
`archives/semaines/2026-09-26.yml` : .MRMister7866 -2 426,6 (général), Maaxster -452,2, .PatientLime2170 -89,8,
.TomHeroes57 -59,4. Originaux dans `/plugins/_removed-kg_scoreboards-stats-2026-10-03/`. Pas d'écriture dans le
journal des parties ; paliers de récompenses déjà donnés non repris. Détail : `historique-conversations/2026-10-03-bingo.md`.

**KG_BingoGame 0.8.6** (Serveur Jeux, déployé à 12:51, non testé) : en 0.8.5 le départ pouvait tomber au milieu de
l'océan (terre cherchée seulement à 160 blocs du centre) ; désormais biome terrestre le plus proche, jusqu'à 3000 blocs.
Le gel à la création a bien disparu (« Prepared spawn area in 0 ms »).

**KG_BingoObservateur 0.1.0** (nouveau, Serveur Jeux, déployé à 10:18, non testé, actif après redémarrage) :
`/observer` donne aux opérateurs une longue-vue « Observer un joueur » (liste groupée par partie, vue libre ou dans ses
yeux, `/observer quitter`, retour automatique en fin de partie).

À tester (liste complète dans `historique-conversations/2026-10-03-bingo.md`) : observateur (longue-vue, deux vues,
Nether / mort du joueur suivi, `/observer quitter`, retour en fin de partie et mondes bien supprimés, refus pour un
non-opérateur) ; grille à 25 difficiles, limite de
création, -15 à l'abandon (bouton, déconnexion, joueur seul), pas de « en 1er » à une équipe, contre la montre (chrono,
victoire, temps écoulé, redémarrage), plus de « Prepared spawn area in 5000 ms », départ sur la terre ferme, logs
« Réserve : ... », 2e partie pendant une autre sans gel.

### 2026-10-01 — Maxster33

*(Session du 01/10/2026, de 19 h 30 à 20 h 25. Fin de session : réservations libérées.)*

- **Chat Bedrock (réglé)** : « Chat désactivé à cause de l'absence de la clé publique du profil » sur tous les
  serveurs depuis toujours. Cause : `enforce-secure-profile=true` (valeur par défaut) dans `server.properties` des
  serveurs Paper ; les joueurs Bedrock (Floodgate) n'ont pas de clé de profil. Réglé par Maxster33 (passé à `false`) ;
  confirmé par Maxster33 le 01/10/2026. Pour tout nouveau serveur Paper : `enforce-secure-profile=false`.
- **Paramètres inaccessibles sur PS5** (signalé par Maxster33, pas testé sur les autres plateformes) : non étudié ;
  aucune cause trouvée dans notre code. À préciser : bouton grisé ou écran qui se referme, sur quel serveur.
- **KS_Enclume 1.2.0** (Event, déployé à 19:50, non testé, actif après redémarrage d'Event) : coût vanilla jusqu'à
  50 niveaux, partie au-dessus comptée pour moitié (70 → 60, 100 → 75) ; fioles de KS_FioleExp inchangées.
  À tester : réparation / fusion à plus de 50 niveaux en vanilla (Java et Bedrock, « Coût réel » et niveaux retirés),
  fiole remplie à l'enclume (coût inchangé).
- **KS_LootBlocs 1.2.0** (Event, déployé à 20:24, non testé, actif après redémarrage d'Event) : fer et or bruts =
  vanilla x2 (2, puis Fortune vanilla : jusqu'à 8 en Fortune III) au lieu de 2 à 5 « comme le cuivre » (trop). À tester :
  minage sans Fortune (toujours 2), avec Fortune III (2 à 8), Toucher de soie (bloc de minerai).

---


## Version complète de REPRISE_PROJET.md du 23/09/2026 (avant la restructuration du 24/09/2026)

Auteur non précisé. Rédigée pour la sauvegarde `KalProjet-sauvegarde-2026-09-23.zip`, avant la création du dépôt git.

## Reprise du projet KaLium (KalBingo / KalGames) — sauvegarde du 23/09/2026 (mise à jour après Rush)

Document de reprise pour la prochaine session. **À donner à Claude en premier** (avec l'archive
entière) : il contient l'état exact du projet, ce qui est déployé, comment compiler et déployer, et
les points encore ouverts. L'historique détaillé de chaque plugin est dans son `JOURNAL.md`
(`sources/<plugin>/JOURNAL.md`), à lire ensuite pour le plugin concerné.

Autres documents utiles déjà présents dans le dossier Téléchargements de l'utilisateur :
`to_do_list_Kal_Games_Bingo.txt` (cahier des charges Bingo) et
`Procedure-Claude-controle-PC-deploiement.md` (procédure de contrôle du PC / déploiement).

### Règles du projet (toujours en vigueur)

- Ne jamais construire quelque chose qui n'a pas été explicitement demandé.
- En cas de doute technique ou d'ambiguïté : proposer / poser la question AVANT d'implémenter,
  jamais deviner.
- Déploiement non destructif : l'ancien jar est toujours déplacé (jamais supprimé) dans
  `/plugins/_removed-<plugin>-<ancienne version>/` avant d'envoyer le nouveau.
- Les messages et textes en jeu sont en français.

### Architecture du réseau

| Rôle | Nom Velocity | Hôte SFTP (onglet WinSCP) | Plugin(s) à nous |
|---|---|---|---|
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | KaliumRelay 1.1.0 (relais HTTP, port 46199) |
| Hub mini-jeux | `kal-games` | `kalgames@7021.mystrator.com` | KalGames 1.12.3, KaliumMenu 1.5.0 |
| Serveur Bingo (dédié) | `kixster` | `kixster@7003.mystrator.com` | KalBingo 0.1.22 |
| Serveur de test survie | `Kal-Test-Dev` | — | KaliumCore 1.4.0 (projet en pause, voir son JOURNAL) |

- **KalGames** : le hub. Menu "Bingo" pour créer une partie (équipes, taille, durée ≤ max défini par
  les opérateurs) ou rejoindre une partie listée (parties encore en salle d'attente, 6 max affichées).
  Transfère les joueurs vers Kixster.
- **KalBingo** : tout le jeu Bingo, sur Kixster. Salle d'attente (modèle capturé par l'admin via
  `/menu`, collé sur 4 emplacements), choix des équipes par l'hôte, une map (monde) par équipe avec la
  même seed, grille 5×5, validation, score, HUD, fin de partie, abandon, persistance des parties en
  cours (survivent à un redémarrage).
- **Communication kal-games ↔ Kixster** : deux chemins en parallèle.
  1. Canal BungeeCord "Forward" — ne livre un message QUE si au moins un joueur est connecté sur le
     serveur CIBLE (limite du protocole Minecraft, source de plusieurs bugs passés).
  2. Relais HTTP KaliumRelay (sur le proxy) — indépendant des joueurs. Endpoints :
     `/assignment/<clé>` (stockage clé → texte, POST dépose, GET lit et efface, expire en 2 min) et
     `/active-game/<uuid>` (joueur "en partie" sur un serveur, pour la reconnexion directe).
     Jeton : `relay-token` dans les config.yml de KalGames (`bingo.`) et KalBingo (`network.`).
- **Signal "partie fermée"** (démarrée/annulée → retirée de la liste kal-games) : Forward + clé relais
  `party-closed-<gameId>` republiée chaque minute pendant 6 h ; kal-games interroge le relais toutes
  les 5 s.

### Versions déployées au 23/09/2026 (toutes vérifiées dans WinSCP après actualisation forcée)

- Kixster `/plugins/KalBingo-0.1.22.jar` (Nether et End propres à chaque équipe, keepInventory en partie,
  suppression des maps corrigée ; salle d'attente jusqu'à 256 blocs par côté ; config.yml déployé modifié : `lobby.max-size: 256`, `lobby.blocks-per-tick: 30000`)
- kal-games `/plugins/KalGames-1.12.3.jar` (Rush jouable, NON encore testé en jeu ; 1.12.3 = correctif du crash
  pendant la capture d'arène, voir son JOURNAL)
- proxy `/plugins/KaliumRelay-1.1.0.jar`
- kal-games `/plugins/KaliumMenu-1.5.0.jar` (bouton Paramètres des téléportations). Lobby
  (`lobby@7002.mystrator.com`, onglet WinSCP ajouté par l'utilisateur) : KaliumMenu 1.5.0 présent.

Confirmé par l'utilisateur le 23/09/2026 (avant Rush) : "tout fonctionne très bien".

### Chantier en cours : mini-jeu Rush (KalGames 1.11.0 → 1.12.3)

Cahier des charges complet, décisions prises et fonctionnement : `sources/KalGames/JOURNAL.md`, sections
1.11.0 à 1.12.3. Code : `game/RushInstance.java` (moteur), `game/RushItems.java` (monnaies + 45 échanges),
`listener/RushListener.java`, `model/RushLayout.java` (équipes, PNJ, points d'arène). Prochaine étape : retours
de test de l'utilisateur (arène à configurer : salle d'attente + 11 points par base).

### Contenu de cette sauvegarde

- `sources/` : code source complet des 5 plugins (KalBingo, KalGames, KaliumCore, KaliumMenu,
  KaliumRelay), avec `build.sh`, `pom.xml` et `JOURNAL.md`.
- `outils-build/` : compilateur `ecj.jar` + toutes les bibliothèques (`libs/`, dont `paper-api`
  26.2.build.123) utilisés pour compiler — permet de recompiler à l'identique sans rien retélécharger.
- `jars-deployes/` : les jars actuellement en service (copie de secours).

### Compiler sur un PC (depuis le dépôt git) - mis en place le 24/09/2026

Prérequis : Java 21 ou plus (testé avec Temurin 25) et Git Bash sous Windows.

```sh
sh telecharger-outils.sh      # une seule fois par machine : compilateur ecj + bibliothèques dans outils-build/
sh KalBingo/build.sh          # -> sortie/KalBingo-<version>.jar (idem pour les autres plugins)
```

`outils-build/` et `sortie/` sont ignorés par git. Versions utilisées : ecj 3.46.100, paper-api
26.2.build.123 (et ses dépendances, voir le script). Vérifié le 24/09/2026 : les 5 jars recompilés ainsi sont
identiques octet pour octet à ceux de `jars-deployes/` (hors manifeste). Le fichier `.gitattributes` impose des
fins de ligne LF : sans lui, Git pour Windows convertit les config.yml en CRLF et les jars ne sont plus identiques.

### Compiler (dans l'espace de travail cloud de Claude)

Sans dossier `outils-build/` à la racine, les `build.sh` utilisent `/tmp/claude-0/` et écrivent dans
`/mnt/user-data/outputs/`. Pour restaurer :

```sh
unzip KalProjet-sauvegarde-2026-09-23.zip -d /home/claude/restore
mkdir -p /tmp/claude-0 && cp -r /home/claude/restore/outils-build/* /tmp/claude-0/
cp -r /home/claude/restore/sources/* /home/claude/
cd /home/claude/KalBingo && sh build.sh   # -> /mnt/user-data/outputs/KalBingo-<version>.jar
```

Chaque nouvelle version : incrémenter `VERSION=` dans `build.sh` ET `<version>` dans `pom.xml`, puis
ajouter une section au `JOURNAL.md` du plugin (ce qui a été demandé, la cause, ce qui a changé, les
limites, la date de déploiement).

### Déployer (WinSCP sur le PC de l'utilisateur)

1. Envoyer le jar dans la conversation, puis l'écrire dans `C:\Users\HP Zbook 17 G3\Downloads\`.
2. Dans WinSCP, onglet du bon serveur, panneau distant sur `/plugins/`, **Ctrl+R pour actualiser**
   (le panneau distant peut afficher un état périmé : ne jamais se fier à une liste non actualisée).
3. Sélectionner l'ancien jar (vérifier le nom et la taille dans la barre d'état), Maj+F6, chemin
   `/plugins/_removed-<plugin>-<ancienne version>/<ancien jar>`, Entrée.
4. Panneau local : Downloads (le dossier "Claude outputs" est un sous-dossier), sélectionner le
   nouveau jar, F5, OK. Actualiser et vérifier qu'il n'y a qu'un seul jar du plugin.
5. L'utilisateur redémarre le serveur (pas d'accès console/RCON, uniquement SFTP).

Pièges connus : ne pas double-cliquer sur un fichier (ouvre l'éditeur interne — refermer SANS
enregistrer) ; taper du texte quand la liste a le focus lance une recherche, pas une navigation ;
autoriser `Textinputhost` (clavier tactile Windows) si des clics échouent.

### Points ouverts / limites connues (rien de bloquant)

- KalBingo 0.1.22 (Nether / End par équipe, keepInventory) : à tester en jeu. Sur Paper 26.x les mondes ajoutés
  sont dans `Kixster SMP/dimensions/minecraft/` ; 18 anciens dossiers `bingo_<uuid>_<n>` de parties terminées
  avant 0.1.22 y restent (jamais effacés à cause de l'ancien bug) : à supprimer par l'utilisateur s'il le souhaite.

- KalGames 1.12.3 (capture / effacement / collage sans chargement synchrone de chunks) : à confirmer en jeu par une
  recapture du parkour. Réglages : `arenas.capture-blocks-per-tick` (30 000), `instances.wipe-blocks-per-tick`
  (20 000), `instances.paste-blocks-per-tick` (40 000). Journaux du crash du 23/09 : `Downloads\Claude outputs\kalgames-logs\`.

- Création d'une map : bloque le serveur Kixster ~8 s par map (génération synchrone du monde par le
  jeu). Proposé à l'utilisateur, pas encore traité.
- Un joueur déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris) ; la
  protection des blocs ne couvre plus que les salles collées.
- La pré-génération ne tient pas compte de `instances.max-simultaneous-games`.
- `reconnect-during-game.abandon-after-seconds` : détection seulement, aucune action.
- KalGames `bingo.max-party-size` n'est plus utilisé (capacité = équipes × taille).
- Proxy : `geyserupdater-spigot.jar` (plugin Spigot) dans les plugins Velocity → erreur de chargement
  au démarrage, sans conséquence. Non touché (pas demandé).
- Réglages utiles côté Kixster (`plugins/KalBingo/config.yml`) :
  `instances.pregeneration-radius-blocks` (200), `instances.pregeneration-stagger-seconds` (10),
  `game.post-game-lobby-timeout-seconds` (600), `game.no-players-abandon-after-seconds` (600).
  Toute nouvelle clé doit être AJOUTÉE à la main dans le fichier déployé (le plugin ne réécrit pas
  un config.yml existant) ; redémarrage requis.

### 2026-09-24 — LeKiwi06

*(Fin de session : toutes les réservations libérées ; KG_Menu et les versions qui l'accompagnent restent à déployer.)*

**Mise en route et règles**
- Dépôt cloné sur le PC de LeKiwi06, compilation locale (`telecharger-outils.sh`, `build.sh` PC + cloud,
  `.gitattributes` LF) : jars recompilés identiques à ceux en service.
- `REGLES.md` créé et complété : réservations en **deux catégories** (utilisés actuellement / requis parfois) avec
  **demandes de créneau** auxquelles le Claude de l'autre peut répondre ; 2 plugins « utilisés » max par personne de
  13 h à 23 h (heure de Paris, `date` simple sous Git Bash) ; un plugin = un rôle ; secrets seulement sur les
  serveurs ; non destructif (`_removed-…`, **2 derniers gardés par plugin**, suppression par l'humain).
  `REPRISE_PROJET.md` restructuré, ancienne version dans `archive_reprise.md`.
- Sécurité : jeton du relais remplacé sur les 3 serveurs par LeKiwi06 (jamais écrit dans le dépôt ni la
  conversation) ; KaliumRelay 1.1.1 sans jeton dans le code.
- WinSCP : tous les serveurs enregistrés avec mot de passe (Kal-games, Kal-test-dev, Kixster, Lobby, Proxy Velocity) ;
  Claude déploie par ces sites sans jamais taper de mot de passe ; l'humain arrête / redémarre.

**Bingo (terminé et déployé)**
- Découpage : `KG_Bingo` (hub) + KalBingo renommé `KG_BingoGame` (Kixster migré).
- Nouveau Bingo (KG_BingoGame 0.3.0 → 0.5.0) : 200 objectifs validés ; modes Bingos (3 à 12, chrono) et Blackout ;
  composition de la grille au choix ; barème en temps réel (1/3/5/10, 1re équipe, coefficients additifs, victoire,
  classement cumulé équipe et solo) ; blackout gagné en moins de 2 h : ×2 / ×1,5 (difficile + extrême : ×5 / ×2) ;
  nulle par vote, « Égalité » au score, abandon (10 min), inactivité (5 min), invincibilité de fin, keepInventory,
  papier verrouillé ; carte de la grille en main secondaire (icônes du jeu officiel, contour des objets blancs),
  couleurs d'équipe A rouge / B bleu / C jaune / D verte, têtes des coéquipiers, résumé de fin de partie ; mode solo
  gardé pour le speedrun.
- Testé par LeKiwi06 : « propre et sans bugs ». Limite acceptée : têtes invisibles sur Bedrock. Le « Nether commun »
  vu en test venait de l'ancienne 0.1.20.

**Classements : KG_ScoreBoards**
- Plugin autonome (étape A faite et déployée) : classements sortis de KalGames, données migrées sur kal-games
  (`stats.yml`, `boards.yml`), `StatsService` compilé identique. Panneaux du hub : Paramètres > Mini-jeux Kal-Games >
  mini-jeu > Classements > Panneaux dans le hub.
- Décisions pour la suite (étape B, non faite) : toutes les parties enregistrées, par joueur et par catégorie (solo /
  duo / trio / squad, blackout) : points, objectifs en 1er, bingos, victoire / défaite / nulle ; parties sans
  adversaire comptées ; seuls les classements individuels affichés au début ; archives détaillées (futur bot
  Discord) ; résultats du Bingo transmis par KaliumRelay ; format des scores : 6 chiffres max, puis K / M / Md.

**Interfaces : KLM_Menu et KG_Menu**
- Hiérarchie décidée : **KLM_Menu** (tous les serveurs : navigation, boussole, boîte à outils des menus, catalogue
  « Interfaces ») → **menu de chaque serveur** (KG_Menu pour kal-games ; plus tard créa, skyblock, survie) →
  interfaces des jeux, **découvertes au démarrage**. Les mini-jeux gardent leurs propres écrans. Protections de zone
  du hub : restent dans KalGames pour l'instant.
- KLM_Menu 2.0.0 (KaliumMenu renommé) déployé sur kal-games, lobby et Kixster (boussole désactivée sur Kixster :
  c'est un objectif du Bingo). Bug corrigé : plus de boussole au hub (KalGames 1.15.1).
- Compilés, **non déployés** : KG_Menu 1.0.0, KLM_Menu 2.1.0, KalGames 1.16.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0
  (à installer ensemble sur kal-games, voir leurs JOURNAL).
- Prévu : phase 2 (accès des admins aux interfaces des autres serveurs, via KaliumRelay ; limite : menus en coffre et
  actions sur le joueur impossibles à distance).

**À faire / à savoir**
- Liste des parties Bingo : pseudo de l'hôte figé dans `lang.yml` et affichage des équipes ambigu → à corriger
  (KG_Bingo / KG_Menu).
- À tester : pseudos dans les annonces à 3 comptes ou plus, nulle en groupe, multiplicateur du blackout, catalogue
  « Interfaces », KG_Menu.
- Tri des anciens dossiers `_removed-…` et `.bak` : après le découpage des plugins.
- Vus dans les journaux, hors de notre travail : config de ConditionalEvents invalide (kal-games), AnvilUnlocker sans
  ProtocolLib, 2 voicechat et Geyser sur Kixster, geyserupdater-spigot sur le proxy.
- Points 2 et 3 de la première lecture : à rappeler quand LeKiwi06 le demande.
- Erreurs de Claude, corrigées : commits incomplets (étape B du Bingo, journaux de KG_Menu), horodatages en UTC,
  journaux de serveur posés un moment dans le dossier du dépôt / dans Documents, deux dossiers de Kal-test-dev
  listés par erreur, ancien nom KaliumMenu non recherché avant le renommage (boussole du hub).

### 2026-09-24 — Maxster33

**Dépôt** : import du bundle `Kalium-depot.bundle` sur `Maxster33/Kalium_depot` (main + 5 étiquettes), dossier
`historique-conversations/` créé.

**Raccordement de nouveaux serveurs au proxy (étape 1 de la migration)** - fait par Claude via WinSCP, sauvegardes
dans `_removed-config-2026-09-24/` (racine de chaque serveur) et `plugins/_removed-klm_menu-2.0.0/`,
`plugins/_removed-voicechat-2.6.23/` (lobby) :
- Proxy : `kalgames2`, `serveur-jeux`, `kal-test-dev` ajoutés dans `velocity.toml` (le renommage `Kixster` →
  `Bingo` fait entre-temps par LeKiwi06 est conservé).
- KalGames2 et Serveur Jeux (Paper neufs) : transfert Velocity activé (`paper-global.yml`), `online-mode=false`,
  liste blanche désactivée ; plugins communs du lobby **avec leurs configurations** (Floodgate + clé du proxy,
  BedrockSkinRestorer, VelocityCommandForward, LuckPerms (base H2 du lobby copiée), voicechat, WorldEdit, WorldGuard,
  ConditionalEvents, PyxelRegions) + KLM_Menu 2.0.0 ; voicechat : ports 43374 / 43131.
- Kal-Test-Dev : transfert Velocity activé (il était en `online-mode=false` SANS proxy : tout le monde pouvait s'y
  connecter sous n'importe quel pseudo), clé Floodgate remplacée par celle du proxy, KLM_Menu 2.0.0, voicechat 45595.
- Lobby : KLM_Menu, 3 nouvelles destinations (désactivées, à activer en jeu) ; voicechat : port 24454 → 43841.
- Non fait : voicechat de Kixster / kal-games / proxy ; redémarrages (humain).
- Erreurs de Claude : deux valeurs secrètes (secret Velocity, `management-server-secret`) affichées dans la
  conversation pendant une comparaison de fichiers (jamais publiées) ; un jar ouvert par erreur dans l'éditeur de
  WinSCP, refermé sans enregistrer.

**Migration complète (après-midi du 24/09/2026)** - LeKiwi06 avait libéré KG_Bingo / KG_BingoGame :
- kal-games et Kixster arrêtés par Maxster33, **copiés en entier** (912 Mo et 791 Mo, dossiers cachés `.paper`,
  `.cache`, `.ai-backups` compris) sur KalGames2 et Serveur Jeux ; ancien contenu des nouveaux serveurs dans
  `_removed-avant-migration-2026-09-24/`. Vérifié par comparaison fichier par fichier (seules différences : les
  réglages ci-dessous).
- Réglages : ports de jeu et voicechat des nouveaux serveurs, KG_Bingo `bingo.server-name: serveur-jeux`,
  KG_BingoGame `network.self-server-name: "serveur-jeux"` (clé ajoutée). **Aucun changement de code** : KaliumRelay
  ne connaît aucun nom de serveur (il relaie ceux que les plugins lui donnent), KLM_Menu des serveurs de jeu renvoie
  seulement au lobby, et `kal-games` reste le nom du hub pour KG_BingoGame.
- Proxy : `kal-games` → KalGames2, `event` → ancien kal-games, `kixster` → Kixster, `serveur-jeux` ; `Bingo` et
  `kalgames2` supprimés. Lobby : destinations KLM_Menu `kixster` (désactivée), `kal-games`, `serveur-jeux`,
  `kal-test-dev`, `event` (désactivée).
- KG_BingoGame n'était plus actif sur Kixster (0.5.0 rangée à 11:57) : 0.5.0 réinstallée sur Serveur Jeux.
- Ajout de Floodgate, BedrockSkinRestorer, VelocityCommandForward sur les 2 nouveaux serveurs (absents des originaux).
- PC de Maxster33 : Java 27 installé, `telecharger-outils.sh` fait, compilation vérifiée (KaliumRelay). Sous Git Bash,
  Java 27 n'est pas dans le PATH tant que Git Bash n'a pas été relancé : `export PATH="/c/Program Files/Java/jdk-27/bin:$PATH"`.
- Travail désormais piloté par WinSCP.com + scripts (guide du Claude de LeKiwi06), plus par captures d'écran.
- **Correctifs et nettoyage (fin d'après-midi du 24/09/2026)** : KalGames2 : `PlaceholderAPIScoreboardObjectivesPlaceholder.jar`
  (extension PlaceholderAPI, pas un plugin) déplacé dans `plugins/PlaceholderAPI/expansions/`. Serveur Jeux : voicechat
  2.6.23 rangé (2.6.24 gardé), AnvilUnlocker retiré (sans ProtocolLib, inutile au Bingo). Kixster rendu au SMP : monde
  d'origine restauré, `bukkit.yml` sans générateur Bingo, KG_BingoGame + anciens `_removed-kalbingo/kg_bingogame` +
  mondes ratés (`_failed_v0.1.3`, `_crashed_0.1.4`) + monde vide du Bingo (avec `bingo_lobby`) + fichier `Depot`
  (journal envoyé par erreur à la place d'un jar le 24/09 à 12:05) rangés dans `_removed-bingo-2026-09-24/` ;
  voicechat 2.6.24 rangé ; WorldEdit gardé ; `generate-structures=false` laissé (décisions de Maxster33).
- **Migration confirmée par Maxster33 le 24/09/2026** (« tout a l'air de fonctionner correctement ») après redémarrage ; journaux de démarrage de KalGames2 et Serveur Jeux : Paper 26.2-122, seules erreurs = celles déjà présentes sur les originaux (jar `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` illisible sur le hub ; 2 voicechat et AnvilUnlocker sans ProtocolLib sur le Bingo), non corrigées (pas demandé).
- À faire : nettoyage de Kixster (SMP) et
  reconversion d'Event ; KG_BoatRace (LeKiwi06) à déployer sur KalGames2.

---

### 2026-09-25 — LeKiwi06

*(Session du 24/09/2026 fin d'après-midi au 25/09/2026 midi. Fin de session : réservations libérées.)*

**Déploiements et vérifications**
- KG_BingoGame 0.6.0 (génération des maps étalée) déployée sur Serveur Jeux, testée : « plus aucun lag à la
  génération » ; lot KG_Menu 1.0.0 / KLM_Menu 2.1.0 / KalGames 1.16.0 / KG_Bingo 1.3.0 / KG_ScoreBoards 1.2.0 déployé
  sur Kal-Games et testé (Java et Bedrock). Vérifié après la migration de Maxster33 : `velocity.toml`, transfert
  Velocity actif et secret identique sur les 6 serveurs, noms `serveur-jeux` dans les configs du Bingo.
- `kal-test-dev` renommé `Kanvas` dans Velocity par LeKiwi06 (destination de la boussole du lobby à mettre à jour).

**Course de bateau : KG_BoatRace (nouveau plugin, 1.0.0 → 1.4.1)**
- KalGames 1.17.0 : types de mini-jeux ouverts aux plugins de jeu (moteur, réglages, classement, options) ; la course
  sort dans KG_BoatRace sans rien reconfigurer. KalGames 1.18.0 : éditeur de listes de points (checkpoints : insérer,
  remplacer, supprimer, pages) et réglages par point (pour le Parkour).
- KG_BoatRace : vitesse en km/h, seuil de 45 s pour les meilleurs temps, classement en direct (tableau latéral
  personnel avec meilleur tour et « Ton record »), écarts à chaque checkpoint, barème de points (tours, tours propres,
  séries, chrono, tour en tête, Grand Prix, cumul crédité), hors-piste (sous le joueur + contact qui ralentit),
  anti-collision Java et Bedrock (copies sans collision). Tout testé en jeu, avec des joueurs Bedrock.
- KG_ScoreBoards 1.3.0 / 1.4.0 : journal des parties (`journal/<mois>/<mini-jeu>.jsonl`, pour le bot Discord) et
  points décimaux.

**Décisions**
- Cahiers des charges KG_BoatRace et KG_Parkour, architecture cible et charte du réseau : voir les fichiers du dépôt.
- Ordre : d'abord les jeux les plus joués (course, Parkour, PvP Kit), le Rush plus tard.
- GrimAC fait l'anti-triche « dur » ; le futur KG_AntiCheat analysera les données (gains, minerais, échanges,
  revente contre de l'argent réel, traçabilité des objets de valeur).

**À faire / à savoir**
- Capturer la nouvelle salle d'attente du Bingo ; tester Nether / End de la 0.6.0 et 2 parties simultanées.
- Suite de KG_BoatRace (étapes 4, 5, 6) puis KG_Parkour.
- Erreurs de Claude, corrigées : un journal envoyé à la place d'un jar (chemin avec espaces, contourné par des
  scripts WinSCP) ; secret Velocity affiché pendant une vérification ; méthode supprimée par erreur lors d'une
  modification (vue à la compilation).

---

## Comptes rendus de LeKiwi06 retirés de REPRISE_PROJET.md le 27/09/2026

### 2026-09-26 — LeKiwi06

*(Session de la nuit du 26/09/2026, de 0 h à 6 h 45. Fin de session : toutes les réservations libérées.)*

**Kanvas (serveur de plots en créatif) : testé et confirmé par LeKiwi06**
- Cahier des charges complété (`KV_Plots/CAHIER_DES_CHARGES.md`) ; monde `New World (2)` renommé `Kanvas` ; grille de
  121 plots générée (plot de référence : coin -107 / -57, plots 49 x 49, routes 9).
- KV_Plots 1.4.0 et KV_Menu 1.3.0 : réservation (moyen / grand), éditeurs, protection WorldGuard, remise à zéro /
  suppression, créatif gardé, validation, votes aux terracottas, déblocage à 100 points, titre / description, tableau
  sur le côté, visites, signalements, concours de build ; étoile du Nether (menu) en case 4.
- FAWE Paper (la variante Bukkit ne marchait pas), limites, permissions LuckPerms du groupe `default`, baguette de
  navigation = vide de structure ; tri des plugins de Kanvas.

**Kal-Games / Serveur Jeux**
- Testés : KG_Parkour 1.0.0 + KalGames 1.20.0 (le Parcours sort de KalGames) ; KG_BingoGame 0.8.0 + KG_Bingo 1.5.0
  (barème du Bingo doublé, points du Bingo dans les classements).
- **KG_BingoGame 0.8.2 déployé, non testé** : bonus du 1er = moitié des points de base ; partie à 4 équipes (joueurs
  non téléportés : reconnexion et joueurs sans équipe ; barre d'action courte à partir de 3 équipes) ; icônes de la
  carte (blocs en 3D comme dans l'inventaire, tout en 22 pixels) ; 7 objets courants du Nether passés en Normal
  (`objectives.yml` du serveur remplacé).
- Nettoyage : nouvelle règle (`REGLES.md` 4.3 : `_removed-…` supprimables à 3 versions ou plus de la version en
  service, suppression par l'humain) ; 85 dossiers supprimés par LeKiwi06 avec des scripts WinSCP préparés par Claude.

**À faire plus tard** (demande de LeKiwi06, 26/09/2026)
- **Tester KG_Bingo 1.5.1** (Kal-Games) : les parties qui ne démarrent pas quittent la liste au bout de 30 min (message
  dans la console) ; les parties bloquées de la nuit ont disparu au redémarrage.
- **Tester KG_BingoGame 0.8.2** (icônes de la carte : confirmées) : bonus du 1er,
  Nether en Normal, partie à 3-4 équipes (barre d'action, joueur sans équipe placé, reconnexion pendant la
  préparation).
- **Équilibrage des barèmes** (`EQUILIBRAGE_POINTS.md`) : barème du Parcours (jouer quelques parties « à fond » pour
  mesurer), PvP Kit, Rush, recalcul du passé (dont les anciennes parties de Bingo, jamais créditées).
- **Kanvas** : limites d'entités et mobs sans IA ; agrandissement moyen → grand (et « dupliquer en version grande ») ;
  classements KV_ScoreBoards (solo / duo / équipe, général et du mois) ; extension automatique du monde.
- **KG_Parkour** (cahier des charges) : chrono allongé à chaque checkpoint, barème, contre-la-montre avec fantôme,
  anti-collision.
- **Sauvegardes des mondes** (voir « Points ouverts »).
- Dossiers gardés volontairement : plugins retirés de Kanvas le 26/09 (à supprimer dans quelques jours), dossiers de
  l'Event (voir avec Maxster33), grosses sauvegardes datées (tant que les sauvegardes des mondes ne sont pas réglées) ;
  dossier `plugins/KG_BingoGame/icons/26.2-3d/` sur Serveur Jeux (plus lu depuis 0.8.2).

### 2026-09-25 (soir) — LeKiwi06

*(Session du 25/09/2026 après-midi et soir. Fin de session : réservations KG_BingoGame et KalGames libérées.)*

**Déployé et testé (« tout est bon »)**
- KalGames 1.19.0 + KG_ScoreBoards 1.5.0 : plus de limite de parties privées, chaque attribution de points journalisée,
  `/classements verifier [jours]` et `/classements crediter` (points de course non comptés recrédités).
- KalGames 1.19.1 : aux points de contrôle (Parcours et courses), le retour garde l'orientation de la caméra du joueur
  au moment du passage (correctif mis dans KalGames en attendant KG_Parkour).
- KG_BingoGame 0.7.2 → 0.7.8 : préparation des maps débloquée (plus de blocage à 0 %), succès limités à la partie ;
  salle d'attente invincible, nether star bloquée, plaques de pression, entités recopiées (invulnérables, avec leur
  nom), barrières non recopiées ; positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml`
  (`/bingoadmin lobby capture` et `info` marchent aussi depuis la console) ; XP remise à zéro au lancement et en fin de
  partie, convertie en bonus (0,1 point par niveau, solo et équipe, sans cumul ni multiplicateur).
- Salle d'attente du Bingo (monde `bingo_lobby`) : coin 1 `-69 63 -14`, coin 2 `-5 108 67`, apparition
  `-13.49 79 2.77`.

**À faire / à savoir**
- KG_Parkour à créer (cahier des charges dans le dépôt) ; suite de KG_BoatRace (étapes 4, 5, 6).
- Kanvas : cahier `KV_Plots/CAHIER_DES_CHARGES.md` en local chez LeKiwi06 (non poussé), questions restantes, tri des
  plugins, zone Build Battle à sauvegarder.
- Bingo : tester Nether / End et 2 parties simultanées.

---

## Compte rendu de LeKiwi06 retiré de REPRISE_PROJET.md le 28/09/2026

### 2026-09-26 (soir) — LeKiwi06

*(Session du 26/09/2026, de 18 h à 21 h, testée le 27/09/2026. Fin de session : toutes les réservations libérées.)*

**Lobby : ConditionalEvents et PyxelRegions remplacés par KLM_Portal (testé et confirmé par LeKiwi06)**
- **KLM_Portal** (nouveau plugin réseau, `KLM_Portal/JOURNAL.md`) : un portail = une région WorldGuard reliée à une
  destination de KLM_Menu ; désactiver le bouton dans menu > Paramètres désactive le portail (message + recul).
  Interfaces dans la boussole (Interfaces > KLM_Portal) : « Ajouter un portail » et « Points de chute ». 4 portails
  au lobby (`portal_event`, `portal_kal_games`, `portal_kixster`, `portal_kanvas`, monde `Lobby Kalium`).
- Arrivée au lobby : centre `7.5 67 39.5` (reprend l'événement `center_lobby` de ConditionalEvents).
- Effets de zone : speed 6 et jump_boost 2 dans la région WorldGuard `lobby` (reprend `lobby_effects`, sans la
  vision nocturne, non demandée).
- KLM_Menu 2.2.0 / 2.3.0 : API des destinations et attente avant changement de serveur (points de chute).
- ConditionalEvents 4.79.2 et PyxelRegions 1.2.2 rangés dans `_removed-…` (dossiers de données laissés).

**Kal-Games : KG_Parkour 1.1.0 (testé et confirmé par LeKiwi06)** : chrono de 30 s qui s'allonge à chaque checkpoint
(+30 s, +60 s à partir du 7e ; à zéro le joueur tombe au temps), plus de collision entre coureurs, adversaires à
moins de 3 blocs vus sous forme de bottes en cuir colorées.

**À faire plus tard**
- **`relay-token`** à remplir dans `plugins/KLM_Portal/config.yml` du lobby (le `token` de `relay.properties` du
  proxy) pour que les points de chute vers d'autres serveurs soient transmis ; installer KLM_Menu 2.3.0 +
  KLM_Portal 1.2.0 sur un serveur pour qu'il applique les points de chute qu'on lui envoie. (27/09/2026 : KLM_Menu 2.3.0 + KLM_Portal 1.2.0 installés sur Kanvas)
- Sur le lobby, non retirés (pas demandé) : VelocityCommandForward (plus utilisé par les portails), dossiers de
  données `ConditionalEvents/` et `PyxelRegions/`. Avant ConditionalEvents, `/l` et `/lobby` au lobby ramenaient au
  centre : KLM_Menu répond maintenant « Tu es déjà au lobby » (à trancher).
- Visuel des portails désactivés (prévu plus tard par LeKiwi06).
- **KG_Parkour** (cahier des charges) : réglages du chrono par checkpoint et par map (panneau admin), barème,
  contre-la-montre avec fantôme ; bottes et anti-collision à revérifier avec des joueurs Bedrock.
- Repris de la session précédente : tester KG_Bingo 1.5.1 et KG_BingoGame 0.8.2 (bonus du 1er, Nether en Normal,
  3-4 équipes) ; équilibrage des barèmes (`EQUILIBRAGE_POINTS.md`) ; Kanvas (limites d'entités, agrandissement moyen →
  grand, classements, extension du monde) ; sauvegardes des mondes ; dossiers gardés volontairement (plugins retirés de
  Kanvas le 26/09, dossiers de l'Event, grosses sauvegardes, `plugins/KG_BingoGame/icons/26.2-3d/` sur Serveur Jeux).


---

## Compte rendu de LeKiwi06 retiré de REPRISE_PROJET.md le 29/09/2026

### 2026-09-28 — LeKiwi06

*(Session du 27/09/2026 à 9 h 47 au 28/09/2026 à 1 h 30. Fin de session : toutes les réservations libérées.)*

**Build Battle, nouveau mini-jeu (testé et validé par LeKiwi06 le 28/09/2026)** : cahier des charges
`KV_BuildBattle/CAHIER_DES_CHARGES.md`. Accessible seulement depuis kal-games, joué sur Kanvas (créatif + FAWE, mêmes
protections que les plots).
- **KG_BuildBattle 0.1.0** (kal-games, nouveau) : bouton dans KG_Menu ; file publique solo / duo / trio / squad (tempo
  normal 5 min) ; parties privées (tempo 3 / 5 / 10 / 30 min, équipes de 1 à 4, mode « thèmes écrits », code) ; envoi
  vers Kanvas par le relais (clé `buildbattle-<uuid>`). Liaison « comme le Bingo » (le moteur de KalGames n'est pas
  utilisé).
- **KV_BuildBattle 0.3.5** (Kanvas, nouveau) : monde `buildbattle` ; capture d'une boîte (entière + zone constructible
  + apparition) recopiée en 4 colonnes de 8 (une par partie, 600 blocs entre colonnes) ; salle d'attente capturée et
  recopiée par colonne ; partie complète : compte à rebours 30 s dès 2 équipes, 8 équipes max (16 en duo, 32 en
  squad), vote parmi 5 thèmes (liste de 200 validée) ou thèmes écrits (signalement à la poudre de blaze), construction
  chronométrée dans sa boîte, zones figées, vote terrain par terrain aux terracottas (30 s → 3 s), résultats, retour
  sur kal-games, boîtes remises à neuf ; salle en mode aventure, joueurs invincibles, infos en barre d'action ;
  mobs figés et retirables d'un clic ; rien ne pousse hors de la zone ; `randomTickSpeed` à 0.
- `relay-token` rempli à la main par LeKiwi06 dans les config.yml de KG_BuildBattle et KV_BuildBattle.

**Kanvas** : KV_Plots 1.4.1 (`randomTickSpeed` à 0 : la glace ne fond plus) ; KLM_Menu 2.0.0 → 2.3.0 et KLM_Portal 1.2.0
installés : le point de chute du lobby vers Kanvas fonctionne, boussole et étoile de KV_Menu vérifiées (validé).

**À faire plus tard**
- **Build Battle, étape 4** : points et classements (KG_ScoreBoards, barème calé sur `EQUILIBRAGE_POINTS.md`).
- Build Battle : limite du nombre de mobs par zone (non demandée pour l'instant) ; retirer l'emplacement « Build
  Battle » sans moteur de KalGames à la prochaine retouche de KalGames ; si le serveur s'arrête en pleine partie, les
  boîtes ne sont pas remises à neuf (« Générer l'arène »).
- Nettoyage (par l'humain, règle des 3 versions) : sur Kanvas, `_removed-kv_buildbattle-0.1.0/`, `0.2.0/`, `0.3.0/`,
  `0.3.1/`.
- Repris de la session du 26/09 (soir) : sur le lobby, VelocityCommandForward et dossiers `ConditionalEvents/`,
  `PyxelRegions/` non retirés ; `/l` et `/lobby` au lobby (« Tu es déjà au lobby », à trancher) ; visuel des portails
  désactivés ; KG_Parkour (réglages par checkpoint et par map, barème, contre-la-montre avec fantôme, Bedrock) ;
  tester KG_Bingo 1.5.1 et KG_BingoGame 0.8.2 ; équilibrage des barèmes ; Kanvas (limites d'entités, agrandissement
  moyen → grand, classements, extension du monde) ; sauvegardes des mondes ; dossiers gardés volontairement.


---

### 2026-09-25 — Maxster33

**Serveur Event reconverti en survie classique** (demande de Maxster33, faite par son Claude, WinSCP en ligne de
commande) :
- Map : ancien hub `Kal-Games` et vieux dossier `world` rangés dans `/_removed-survie-2026-09-25/` ;
  `server.properties` : `level-name=world` (nouveau monde vierge généré au premier démarrage, seed aléatoire),
  `difficulty=hard` (décisions de Maxster33) ; anciens `server.properties`, `commands.yml` et config KLM_Menu dans ce
  même dossier.
- Plugins retirés (rangés dans `/plugins/_removed-kalgames-event-2026-09-25/`) : KalGames 1.15.1, KG_Bingo 1.2.0,
  KG_ScoreBoards 1.1.0, leurs dossiers de données, tous les anciens `_removed-kalgames/kg_*/kaliummenu`, `claudebak`
  et les `.jar.bak`. Plugins tiers gardés (pas demandé de les retirer).
- KLM_Menu 2.0.0 gardé : boussole désactivée, menu par `/menu` (alias de `/servers` dans `commands.yml`, même
  méthode que Kixster).
- **Nouveau plugin KS_Dimensions 1.0.0** (préfixe `KS_` = serveur Event) : `/dimensions` (opérateurs) ouvre un menu
  pour activer / désactiver les portails du Nether et de l'End ; un portail désactivé bloque l'aller depuis le monde
  normal, jamais le retour (voir son JOURNAL). Compilé, déployé, non testé.
- À faire / à savoir : démarrer Event (l'humain) et tester ; activer la destination `event` dans le menu du lobby ;
  voir « Points ouverts ». Ensuite (même jour) : Floodgate installé (config et clé du proxy), extension
  PlaceholderAPI déplacée dans `expansions/`, PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`).
  Restent à vérifier : réglages de hub de ConditionalEvents / PyxelRegions / WorldGuard.
- **Plugins du serveur Event (soir du 25/09/2026)** : cahier des charges de Maxster33 enregistré dans
  `KS_Event/CAHIER_DES_CHARGES.md` (demande, réponses, interprétations) ; 7 plugins autonomes créés, compilés et
  déployés sur Event, non testés : KS_Enclume, KS_Villageois, KS_Crafts, KS_LootBlocs, KS_LootEntites, KS_LootPeche,
  KS_LootPotions (détail dans leurs JOURNAL.md). KS_Elixirs reporté (décision de Maxster33). Limite connue :
  4 blocs de cuivre en carré 2×2 donnent probablement le cuivre taillé vanilla (voir KS_Crafts/JOURNAL.md).

---

### 2026-09-28 — Maxster33

*(Session du 28/09/2026, de 21 h 40 à 23 h 30, Claude de Maxster33. Fin de session : réservations libérées.)*

**Serveur Event : estomac du gardien et loots des coffres de structures** (demande et réponses :
`KS_Event/CAHIER_DES_CHARGES.md`, partie 4). Déployé sur Event le 28/09/2026 à 23:19 (WinSCP en ligne de commande),
**non testé en jeu, redémarrage d'Event à faire par l'humain** :
- **KS_EstomacGardien 1.3.0** (nouveau ; 1.1.0 avait `/estomac`, retirée à la demande de Maxster33 ; 1.3.0 : casque 20 %) : objet « Estomac du gardien » (image du sac noir, pas un vrai sac) ; clic
  droit : consommé, donne son contenu (casque en diamant 20 %, puis 2 tirages : oeuf de tortue, bateau, corail,
  algue, trident enchanté, armure de nautile en diamant, coeur de la mer).
- **KS_LootCoffres 1.0.0** (nouveau) : cité antique, 4 bastions, trésor enfoui, cité de l'End, forteresse,
  avant-poste, manoir, coffres-forts des chambres d'épreuve. Le loot vanilla (tables 26.2 lues dans le jeu) est
  modifié après génération ; changements de poids exacts par remplacement (voir son JOURNAL). Seuls les coffres pas
  encore ouverts sont concernés.
- **KS_KaliumGive 1.0.0** (nouveau, 23:35) : `/kaliumgive <pseudo> <id_custom> <nombre>` (opérateurs) ; liste des
  id_custom dans le code : `estomac_gardien`, `cle_de_l_end` (1.1.0).
- **KS_EC_Extension 1.0.0** (nouveau, 23:47) : coffre de l'Ender à 6 lignes ; les 3 du bas sont bloquées (barrières) et se
  débloquent case par case en déposant une Clé de l'End (consommée). Clé : image de la clé des épreuves sinistre,
  empilable par 64, craft sans forme dans **KS_Crafts 1.1.0** (coeur de la mer, crâne de wither, totem, pomme de Notch,
  capteur sculk calibré, éponge, lingot de netherite, cloche, coeur de grinceur). Couvercle non animé (limite connue).
- **KS_LootEntites 1.1.0** : grand gardien 50 % d'estomac (tué par un joueur ; `softdepend` KS_EstomacGardien).
- **KS_LootPotions 1.1.0** : potion de faiblesse 10 % pour le capitaine pillard seulement.
- Anciens jars dans `/plugins/_removed-ks_lootentites-1.0.0/` et `/plugins/_removed-ks_lootpotions-1.0.0/`.
- À savoir : la session WinSCP enregistrée d'Event s'appelle `Event@7021.mystrator.com` (port 2022) ; le nom
  `kalgames@7021...` ne marche pas en ligne de commande (WinSCP essaie alors le port 22) ; tableau d'architecture corrigé.
- À tester : estomac (clic droit, contenu, inventaire plein), drop du grand gardien, coffres de structures neufs,
  coffres-forts normal et sinistre, capitaine / pillard, `/kaliumgive`, coffre de l'Ender (ouverture, déblocage,
  contenu gardé après reconnexion), craft de la Clé de l'End.
- **Apparence Bedrock (29/09/2026 00:18 ; testée et confirmée par Maxster33 le 29/09/2026 après redémarrage du proxy)** : nouveau dossier `geyser-bedrock/` (voir son README). Geyser
  n'affichait pas l'image (`item_model`) des objets custom : sur Bedrock, l'Estomac du gardien et la Clé de l'End
  étaient des livres. Correspondance Geyser `custom_mappings/kalium_objets.json` et pack `KaLium-objets-1.0.0.mcpack`
  envoyés sur le proxy (dossiers `custom_mappings/` et `packs/` de Geyser-Velocity, vides avant) ; **actif après
  redémarrage du proxy (l'humain)**. Aucun autre plugin n'utilise d'`item_model` (vérifié dans tout le dépôt).
- **Fiole d'expérience remplie (29/09/2026 03:39, testée et confirmée par Maxster33 le 29/09/2026)** : **KS_FioleExp 1.0.0** (nouveau) : à l'enclume, fiole
  vide en 1re case, 2e case vide, nombre de **points** d'XP tapé dans le champ du nom → « Fiole d'expérience (1 395 XP) »
  (exactement ces points retirés, une fiole vide par opération, usure vanilla de l'enclume) ; lancée, elle lâche
  exactement ces points en orbes. **KS_KaliumGive 1.2.0** : id `fiole_exp(<points>)` (ex. `fiole_exp(1395)`) ; 1.1.0
  dans `_removed-ks_kaliumgive-1.1.0/`. Tests OK (remplissage, lancer, /kaliumgive).
- **29/09/2026 03:54, non testés** : **KS_FioleExp 1.1.0** (aucune fiole d'expérience ne peut plus être renommée à
  l'enclume) et **KS_Enclume 1.1.0** (le jeu du joueur affiche « Trop cher ! » dès 40 niveaux : au-delà de 39, 39 est
  affiché, le vrai coût est écrit en barre d'action et le reste est retiré à la prise). À tester : renommer une fiole
  (impossible), réparation / fusion à plus de 40 niveaux (niveaux retirés, refus si pas assez), fiole de plus de 40
  niveaux.
- **29/09/2026 04:17, non testé** : **KS_Enclume 1.1.1**. En 1.1.0, au-delà de 39 niveaux, la case résultat montrait une
  croix rouge (le jeu Java recalcule lui-même le résultat et le vide dès 40 niveaux) : le serveur renvoie maintenant le
  résultat au joueur. Le vrai coût est écrit en dernière ligne de la description du résultat (la barre d'action était
  cachée par l'enclume ; retirée de l'objet à la prise). « + » à la place de 39 : impossible (nombre écrit par le jeu).
  À tester : réparation / fusion et fiole à plus de 40 niveaux (résultat visible, ligne de coût, niveaux retirés, objet
  sans la ligne), prise refusée sans assez de niveaux. Nettoyage possible par l'humain : rien.
- **29/09/2026 04:31, non testé** : **KS_Enclume 1.1.2** : au-delà de 39 niveaux, l'enclume n'affiche plus aucun coût
  (« + » ou « Coût : » seul impossibles : ligne écrite par le jeu) ; vrai coût en dernière ligne de la description du
  résultat ; à la prise, le vrai coût est rendu à l'enclume, qui retire elle-même les niveaux. À tester : prise (bref
  clignotement possible), niveaux retirés, refus sans assez de niveaux, prise ratée (curseur occupé). **Nettoyage
  possible par l'humain** (règle des 3 versions) : sur Event, `/plugins/_removed-ks_enclume-1.0.0/`.
- **Tests du 29/09/2026 (Maxster33)** : KS_Enclume 1.1.2 et KS_FioleExp 1.1.0 **OK en Java**. **Bedrock** : au-delà de 39
  niveaux l'amélioration marche mais le vrai coût n'apparaît pas dans la description ; le remplissage d'une fiole
  (nombre tapé dans le nom) renomme simplement la fiole, coût affiché 1. Cause probable : sur Bedrock, l'enclume calcule
  et affiche son propre résultat (Geyser ne fait que corriger le coût) ; le résultat préparé par le serveur n'est pas
  montré (non corrigeable côté serveur ni par un pack ; la fiole reçue est pourtant la bonne).
- **29/09/2026 05:15, non testé** : **KS_Enclume 1.1.3** : sur Bedrock, la 1.1.2 (coût 0 envoyé) empêchait de prendre le
  résultat. Joueurs Bedrock (Floodgate) : 39 affiché s'ils ont assez de niveaux, sinon le vrai coût (« Trop cher ! ») ;
  Java inchangé. « 40+ » et relever la limite de 40 : impossibles (fixés dans le jeu du joueur). À tester sur Bedrock :
  prise avec assez de niveaux (vrai coût retiré), « Trop cher ! » sans assez. **Nettoyage possible par l'humain** (règle
  des 3 versions) : voir la ligne 1.1.4 ci-dessous.
- **29/09/2026 05:43, non testé** : **KS_Enclume 1.1.4** : Bedrock, au-delà de 39 niveaux, « Coût réel : N niveaux »
  (vert / rouge) ajouté en dernière ligne de l'objet de la 1re case (marqueur invisible), visible au survol et dans
  l'aperçu du résultat ; retiré à la prise, à tout clic / glisser, à la fermeture (déconnexion comprise), et par sécurité
  à la connexion et à l'ouverture d'une enclume. Sur Bedrock, taper le nom après avoir posé les deux objets. À tester
  (Bedrock) : ligne visible, couleur, objet obtenu et objet repris / rendu **sans** la ligne. **Nettoyage possible par
  l'humain** (règle des 3 versions) : sur Event, `/plugins/_removed-ks_enclume-1.0.0/`, `_removed-ks_enclume-1.1.0/`,
  `_removed-ks_enclume-1.1.1/`.
- **29/09/2026 05:58, non testé** : **KS_FioleExp 1.2.0** : sur Bedrock, le nombre tapé dans le champ du nom n'arrive au
  serveur qu'à la prise (coût impossible à afficher avant). Joueurs Bedrock : **accroupi + clic droit sur une enclume
  avec une fiole vide en main** → formulaire (nombre de points), puis confirmation « N points consommera X niveaux
  (niveau A → B) ». Java inchangé. **`telecharger-outils.sh` modifié** (API Floodgate 2.2.5 + Cumulus 1.1.2, pour
  compiler KS_FioleExp) : le relancer sur chaque PC. **Testé et confirmé en Bedrock par Maxster33 le 29/09/2026.**
- **29/09/2026 matin (Maxster33)** : sur Bedrock, l'Estomac du gardien et la Clé de l'End avaient toujours l'apparence
  d'un livre : le proxy n'avait pas été redémarré depuis l'envoi de `geyser-bedrock/`. Après redémarrage : **apparence
  Bedrock testée et confirmée par Maxster33 le 29/09/2026**.
- **29/09/2026 10:24, déployé sur Event, non testé** : **KS_FioleExp 1.3.0** : joueurs Java aussi, accroupi + clic
  droit sur une enclume avec une fiole vide → dialogue natif (Paper) avec les mêmes textes que le formulaire Bedrock
  (points, confirmation du coût en niveaux). Le champ du nom de l'enclume reste utilisable (choix de Maxster33).
  **Actif après redémarrage d'Event (l'humain).** À tester (Java) : ouverture, textes, confirmation (points, fiole
  vide consommée, fiole reçue), erreurs ; Bedrock inchangé. **Nettoyage possible par l'humain** (règle des 3
  versions) : sur Event, `/plugins/_removed-ks_fioleexp-1.0.0/`.
- **29/09/2026 10:55, déployé sur Event à 10:57, non testé** (actif après redémarrage d'Event, l'humain ; 1.3.0 dans
  `_removed-ks_fioleexp-1.3.0/` ; **supprimables par l'humain** (règle des 3 versions) : `_removed-ks_fioleexp-1.0.0/`
  et `_removed-ks_fioleexp-1.1.0/`) : **KS_FioleExp 1.4.0** : « la fabrication de fiole d'exp abîme
  trop l'enclume » : 6 % de chance d'usure par fiole au lieu de 12 % (environ 50 fioles par enclume au lieu de 25),
  réglable dans le nouveau `config.yml` (`usure-enclume-pourcent`, créé au premier démarrage ; 0 = aucune usure).
- **29/09/2026 12:40, déployé à 12:55 (Event) et 12:56 (proxy), non testé** : **Bedrock Breaker** (détail : `KS_BedrockBreaker/JOURNAL.md`).
  Nouveau plugin **KS_BedrockBreaker 1.0.0** : objet « Bedrock Breaker » (image du bâton de blaze, description
  « Utilisation unique », empilable par 64), un bloc de bedrock par objet, sauf la couche du fond ; protections
  WorldGuard respectées (cassage simulé) ; anciens Bedrock Breaker (houe en bois) encore utilisables. **KS_Crafts 1.4.0** :
  8 TNT autour d'une houe en diamant (n'importe laquelle) ; recette débloquée dans le livre dès qu'on obtient une TNT
  ou une houe en diamant. **KS_KaliumGive 1.3.0** : id `bedrock_breaker`. **geyser-bedrock 1.1.0** : apparence Bedrock.
  Anciens jars dans `_removed-ks_crafts-1.3.0/` et `_removed-ks_kaliumgive-1.2.0/` ; sur le proxy, ancien
  `kalium_objets.json` et pack 1.0.0 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.0.0/` (hors de `packs/` et
  `custom_mappings/`). **Actif après redémarrage d'Event et du proxy (l'humain).** À tester : craft (8 TNT + houe en
  diamant), recette dans le livre à l'obtention d'une TNT ou d'une houe en diamant, apparence (Java et Bedrock),
  description, pile de 64, bedrock cassée (1 objet consommé), couche du fond refusée, région WorldGuard refusée (objet
  gardé), ancien Bedrock Breaker, `/kaliumgive <pseudo> bedrock_breaker 1`. **Nettoyage possible par l'humain** (règle
  des 3 versions) : sur Event, `_removed-ks_crafts-1.0.0/`, `_removed-ks_crafts-1.1.0/`, `_removed-ks_kaliumgive-1.0.0/`.
- **29/09/2026 13:20, déployé sur Event à 13:30, non testé (actif après redémarrage d'Event)** : **KS_BedrockBreaker 1.0.1** : un bloc par seconde au plus
  par joueur (signalé par Maxster33 : un bloc de bedrock juste derrière était cassé aussi, avec un 2e Bedrock Breaker,
  à cause du clic répété).
- **29/09/2026 13:35, déployé sur Event à 13:59, non testé** : **KS_BedrockBreaker 1.0.2** : la couche du fond peut de nouveau
  être cassée (demande de Maxster33 ; passage vers le vide possible).
- **29/09/2026 13:55, déployé sur Event à 13:59, non testé** : **KS_Crafts 1.5.0** : nouvelle recette du Bedrock Breaker
  (poudre de blaze aux 4 coins, charge de feu en haut, wagonnets à TNT à gauche et à droite, cristal de l'End au
  centre, ancre de réapparition en bas) ; débloquée dans le livre de recettes à l'obtention d'un de ces ingrédients.
- **29/09/2026 14:08, déployé sur Event à 14:18, non testé** : **KS_FioleExp 1.5.0** + **KS_KaliumGive 1.4.0** (à déployer
  ensemble) : fiole remplie en **niveaux** (niveau 50 = points du niveau 0 au niveau 50), à l'enclume et dans le menu
  (Java et Bedrock) ; nom « Fiole d'expérience (niveau 50) », points en description ; confirmation du menu détaillée
  (points de la fiole, points du joueur, coût en points et en niveaux perdus) ; `fiole_exp(N)` en niveaux.
- **29/09/2026 14:10, déployé sur Event à 14:18, non testé** : **KS_LootEntites 1.2.0** : Warden (tué par un joueur) : une
  fiole d'expérience tirée au sort à chaque fois, niveau 10 (poids 15), 15 (10), 20 (6), 30 (3) ou 40 (1) ; l'ancienne
  fiole (10 % d'une fiole de niveau 10 à 50) est retirée. Endermite : la fiole de niveau 10 de son tirage devient la
  nouvelle fiole de KS_FioleExp (points en description) ; à déployer avec KS_FioleExp 1.5.0.
  **Actif après redémarrage d'Event (l'humain).** À tester : fiole à l'enclume et au menu (Java, Bedrock) en niveaux, nom
  et description, confirmation détaillée, `/kaliumgive <pseudo> fiole_exp(50) 1`, fiole du Warden et de l'endermite
  (tués par un joueur). **Nettoyage possible par l'humain** (règle des 3 versions) : sur Event,
  `_removed-ks_fioleexp-1.0.0/`, `1.1.0/`, `1.2.0/`, `_removed-ks_kaliumgive-1.0.0/`, `1.1.0/`.
- **29/09/2026 14:21, déployé sur Event à 14:22, non testé (actif après redémarrage d'Event ; supprimable par l'humain : `_removed-ks_lootentites-1.0.0/`)** : **KS_LootEntites 1.2.1** : Warden, ligne « rien » de poids 315
  dans le tirage : 10 % de chance d'une fiole (niveau 10 : 4,29 %, 15 : 2,86 %, 20 : 1,71 %, 30 : 0,86 %, 40 : 0,29 %).
- **29/09/2026 17:10, déployé sur Event à 17:22 et sur le proxy à 17:23, non testé** : **spawners et Changeur de Biome** (à déployer ensemble ;
  réservations au-delà de 2 plugins avec l'accord de LeKiwi06, selon Maxster33). Nouveaux plugins **KS_ItemSimple 1.0.0**
  (Fragment de Spawner, Cœur de Spawner, 5 têtes « Steve » nommées en attendant un plugin des têtes), **KS_Spawners
  1.0.0** (8 spawners avec leur créature, empilables par 64 ; posés par un joueur, ils tombent quand on les casse, sans
  XP), **KS_BiomeChanger 1.0.0** (Changeur de Biome : menu des biomes de l'overworld, sphère de 32 blocs, historique,
  téléportation pour les opérateurs ; dépend de KLM_Menu) ; **KS_Crafts 1.6.0** (8 spawners : 7 fragments + cœur + tête ;
  Changeur de Biome ; recettes débloquées dans le livre à l'obtention d'un Fragment de Spawner) ; **KS_KaliumGive 1.5.0**
  (16 nouveaux id) ; **KS_LootBlocs 1.1.0** (spawner naturel : 1 fragment + 5 % d'un 2e) ; **geyser-bedrock 1.2.0**
  (apparence Bedrock, proxy). Détail dans chaque `JOURNAL.md`.
  Compléments (demandes de Maxster33, avant déploiement) : Changeur de Biome au choix **sphère** (32 blocs de rayon) ou
  **cube** (52 blocs de côté, même volume) ; **refusé si la zone touche une région WorldGuard** (même celles du joueur) ;
  un spawner fabriqué détruit par une **explosion** tombe au sol. `outils-build` : lancer `sh telecharger-outils.sh` si
  l'API WorldGuard / WorldEdit manque (déjà dans le script, pour KV_Plots).
  **Actifs après redémarrage d'Event et du proxy (l'humain).** À tester : crafts (spawners, Changeur de Biome), livre de
  recettes au 1er fragment, pose / cassage / explosion d'un spawner fabriqué, fragments d'un spawner naturel, menu du
  Changeur de Biome (sphère / cube, biome, région WorldGuard refusée, historique, téléportation opérateur), apparence
  Bedrock, `/kaliumgive`. **Nettoyage possible par l'humain** (règle des 3 versions) : sur Event,
  `_removed-ks_crafts-1.0.0/` à `1.3.0/`, `_removed-ks_kaliumgive-1.0.0/` à `1.2.0/`.
