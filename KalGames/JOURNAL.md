# Journal de session — KalGames (Kal-Games)

Ce fichier sert de mémoire de reprise entre deux sessions : contexte du projet, règles à respecter, et
historique de ce qui a été fait. À lire en premier au début d'une nouvelle session si le fil de conversation
n'est pas disponible.

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

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

## Pour reprendre la prochaine fois (mise à jour, 1.10.4)

- Une fois kal-games et Kixster redémarrés : vérifier que le formulaire de création (menu "Bingo" du
  hub) propose bien un champ de durée pré-rempli à 60 min, réductible jusqu'à 5 min ; vérifier que
  Paramètres > Bingo (menu admin) permet de changer ce plafond (ex. 90 min) et que le formulaire de
  création reflète alors le nouveau plafond comme valeur pré-remplie/maximum.

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

## 1.26.0 - menus en coffre, accueil Créer / Rejoindre / Parties en cours, relance en salle d'attente (20/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (20/10/2026)**, sur maquette validée : « faire une alternative avec des interfaces coffres pour
tout », « masquer pour le moment les parties publiques des mini-jeux, on a pas assez de joueurs pour ça », « montrer
toutes les parties avec le titre du jeu et l'hôte », « au lieu d'un totem pour relancer dans le lobby, laisse nous dans
la salle d'attente du jeu avec un bouton quitter la partie, et un timer de 15 secondes qui relance pour les joueurs qui
n'ont pas quitté la partie » ; « il faut que les spec qui ont regardé la partie soient mis dans la salle d'attente entre
2 parties pour rejoindre la game avant la relance, avec un bouton "passer spectateur" ». Ajouts proposés et acceptés :
dernière partie, réglages retenus, hôte qui pilote la salle, objets de barre, résumé de la manche, messages en barre
d'action. Empilé sur la 1.25.0 non testée en jeu : accord explicite (« les joueurs les utilisent depuis un moment, et
aucun bug n'est à dénombrer »).

**Étape 1 : menus**
- KG_Menu 1.2.0 affiche l'accueil ; KalGames lui fournit ses jeux (`jeux`), ses parties (`parties` : privées listées,
  tous jeux confondus, nom du jeu + hôte + joueurs / places, en attente ou en jeu), la dernière partie du joueur
  (`derniere`) et les codes (`code`).
- **Parties publiques masquées** : `menus.parties-publiques: false` (par défaut). Le moteur public reste en place.
- **Réglages d'une nouvelle partie en coffre** (`MenusCoffre`) : une case par réglage, valeur dans le nom (arène : clic
  gauche suivante / droit précédente ; nombres : un clic ouvre une petite fenêtre avec une jauge, « Valider » ou
  « Retour » ramène au coffre (demande de LeKiwi06 du 20/10 : « ouvrir l'interface de jauge quand on clique sur
  l'item quand c'est nécessaire, par exemple le nombre de tours au bateau ») ; cases : clic = oui/non ;

  « Visible dans la liste »), « Créer la partie » au centre de la barre, « Retour » à côté. Le PvP Kit, le Cache-cache,
  les Piliers et le Pong fournissent leur propre formulaire : il s'ouvre encore en fenêtre depuis le coffre (étape 5).
  Rush : plus de « Joueurs maximum » (la taille vient des équipes de l'arène) ; l'arène se choisit dans les réglages.
- **Menu de la partie, menu du spectateur, choix de l'équipe du Rush en coffre** ; les fenêtres restent pour les joueurs
  qui ont choisi « fenêtres » (même organisation).
- **Réglages retenus** (`reglages.yml`, par joueur et par jeu) : la création reprend les derniers choix (coffre et
  fenêtre) ; **« Recréer ma dernière partie »** sur l'accueil (même jeu, même arène, mêmes réglages).
- Messages des menus en coffre au-dessus de la barre d'objets ; le code de la partie créée reste dans le tchat.
- Icône de chaque jeu : `menus.icones.<TYPE>` (sinon : bateau, bottes, épée, lit, citrouille, bloc d'or, boule de
  slime...).

**Étape 2 : fin de partie (parties privées)**
- Plus de retour au hub ni d'objet « Rejouer » : **tout le monde reste dans la salle d'attente**, barre en haut de
  l'écran « Nouvelle partie dans 15 s », puis la partie **relance avec ceux qui sont restés** (`relance.active`,
  `relance.secondes`). Pas assez de joueurs à 0 s : le compte à rebours s'arrête et reprend dès qu'un joueur rejoint la
  partie (l'hôte peut aussi « Reprendre le compte à rebours » ou « Lancer maintenant »).
- **Spectateurs** de la manche : mis dans la salle d'attente à la fin (s'il reste de la place) ; « Passer spectateur »
  (menu, ou œil de l'Ender dans la barre) pour seulement regarder la suivante ; un spectateur peut « Rejoindre la
  partie » depuis son menu.
- **Objets de la barre** dans la salle d'attente : « Quitter la partie » (lit rouge, case 6) et, entre deux manches,
  « Passer spectateur » (case 2) ; retirés au début de la manche.
- **L'hôte pilote** : « Changer les réglages » (le compte à rebours s'arrête ; « Créer la partie » prépare une nouvelle
  partie avec ces réglages, arène comprise, et y fait passer tout le monde), nommer un autre hôte (clic gauche sur une
  tête), exclure un joueur (Maj + clic droit ; en fenêtre : avec confirmation).
- **Résumé de la manche** (points gagnés par chacun, lus dans KG_ScoreBoards au début et à la fin de la manche) dans le
  menu de la partie, qui s'ouvre tout seul à la fin de la manche.
- Parties publiques (si on les réaffiche) : fonctionnement inchangé (retour au hub + « Rejouer »). KG_BuildBattle garde
  « Rejouer » (étape 6).

**Échap (LeKiwi06, 20/10/2026 : « fermer une interface avec échap ramène sur la page précédente, et que les jauges
soient enregistrées même sur un échap »)** :
- Coffres : fermer un coffre soi-même (Échap, touche de l'inventaire) ramène à la page précédente (réglages → choix du
  jeu, équipes → menu de la partie) ; sur une première page (menu de la partie, spectateur), Échap ferme. Mécanisme
  commun dans KG_Menu 1.2.0 (`echap`).
- Fenêtres des joueurs : le bouton « Retour » est devenu le bouton de sortie de la fenêtre ; le jeu le déclenche sur
  Échap (vérifié dans le code du client 26.3 : `DialogScreen.onClose` → `Dialog.onCancel` = action de sortie).
- Jauges : « Valider » est le bouton de sortie : Échap enregistre la valeur comme « Valider ».
- Les menus des modérateurs (Paramètres) ne changent pas.

**Limites** : un spectateur de l'ancienne partie
 n'est pas emmené quand l'hôte change les réglages (il revient au hub
quand l'ancienne partie se ferme). Le bouton « Classements » de l'ancien écran d'un jeu a disparu (les classements
restent dans « Informations > Classements »).

**À déployer avec KG_Menu 1.2.0 (obligatoire).** Nouvelles clés `menus`, `relance` : valeurs par défaut dans le code.
**Déployé sur Kal-Games le 20/10/2026 à 12:57 (LeKiwi06 ; ancien jar dans `_removed-…`), actif après redémarrage. Statut : non testé en jeu.**

## 1.27.0 - Kal-Games dans Kanvas : KalGames limité à ses mondes (21/10/2026, LeKiwi06)

**Demande de LeKiwi06 (21/10/2026)** : Kal-Games et Serveur Jeux expirent dans 3 jours ; « on va fusionner kal-games et kanvas en utilisant le même principe que pour les mondes simultanés dans serveur jeux » ; « les gens ne pourront pas passer de kanvas à kal-games sans le lobby, tout sera sur le même serveur mais de façon hermétique, on fera même des tchats séparés » ; inventaires séparés ; deux noms Velocity gardés (`kal-games` et `Kanvas`, même adresse). Plan : [Plan de migration](https://claude.ai/artifact/MyTJxpL8zq1XCy1sTZKdZh).

- Le monde du hub (`hub.world`, `Kal-Games`) est chargé au démarrage : il n'est plus forcément le monde principal du
  serveur. Correction du 22/10/2026 (même version) : plus de test du dossier `<racine>/<nom>` avant le chargement
  (sous Paper 26.3 un monde est une dimension, `<monde principal>/dimensions/<espace de noms>/<nom>`, le test aurait
  été faux) ; `WorldCreator(nom).createWorld()` est appelé dès que le monde n'est pas chargé, comme KV_Plots.
- Arrivée : le hub n'est imposé qu'à un joueur qui arrive dans le hub, le monde des parties ou un monde de
  `hub.teleport-from-worlds` (défaut dans le code : `buildbattle`). Un joueur de Kanvas garde sa position.
- Protection du hub (blocs, seaux, cadres, porte-armures, entités) et blocage de `/kit` : seulement dans le hub et le
  monde des parties (avant : tous les mondes, ce qui aurait bloqué la construction sur les plots).
- `/hub` et `/kalgames` refusés à un joueur d'un autre espace (métadonnée `klm_espace` de KLM_Espaces différente de
  `hub.space`, défaut `kal-games`), sauf admins : « Kal-Games se rejoint depuis le lobby. ».
- Nouvelles clés facultatives (défauts dans le code) : `hub.teleport-from-worlds`, `hub.space`.

**Compilé, non déployé, non testé en jeu.** À déployer avec KLM_Espaces 1.0.0 et KaliumRelay 1.9.0 (fusion de Kal-Games dans Kanvas).

## 1.28.0 - « Changer les réglages » modifie la partie au lieu d'en recréer une (22/10/2026, LeKiwi06)

**Bug signalé par LeKiwi06 (22/10/2026)** : « quand je modifie les règles de la game dans le lobby d'attente du
parkour, ça me recrée une partie quand je valide ».

**Cause** : en 1.26.0, « Changer les réglages » (hôte, entre deux manches) créait une nouvelle partie (autre copie de
l'arène, nouveau code, « Partie créée ») et y faisait passer tout le monde, parce que les jeux lisent leurs réglages
une seule fois, à la création de la partie.

**Correction** :
- Même arène : `InstanceManager.reconfigure` reconstruit la partie **sur la même copie de l'arène** avec les nouveaux
  réglages ; elle garde son code, son hôte, sa visibilité, ses joueurs (hôte en premier) et ses spectateurs. Aucun
  chargement, aucun message « Partie créée » : « L'hôte a changé les réglages de la partie. », puis la relance de
  15 s reprend. Un joueur qui n'a plus de place (joueurs maximum réduit) revient au hub avec le motif.
- Autre arène choisie : comme avant, une partie est préparée sur la nouvelle arène et tout le monde y passe.
- Si la manche a repris pendant que l'hôte réglait la partie : refus (« Les réglages ne se changent qu'entre deux
  manches, par l'hôte. ») au lieu de créer une partie à côté.
- Le bouton s'appelle « Appliquer les réglages » dans ce cas (coffre et fenêtre).

**Compilé, non déployé, non testé en jeu.**

**Déployé sur Kanvas (Kal-Games) le 23/10/2026 à 16:27 (déploiement groupé) ; 1.27.0 dans `_removed-kalgames-1.27.0/` ; actif après redémarrage, non testé en jeu.**

## 1.29.0 - événement « arène capturée » (31/10/2026, LeKiwi06)

Pour KG_Parkour 1.8.0 (demande de LeKiwi06 : les golems de la map « Ascension vers l'enfer » doivent se placer à la
capture). Ajout seulement : après une capture réussie (modèle enregistré, anciennes copies retirées),
`fr.kalium.games.event.ArenaCapturedEvent` (arène, monde de construction, zone, modérateur ou null) est lancé sur le
thread principal. Les plugins de jeu y relèvent ce que le modèle ne garde pas (le modèle ne contient que des blocs) et
enregistrent eux-mêmes l'arène. Aucun autre changement.

**Compilé, non déployé, non testé en jeu.** À déployer avec KG_Parkour 1.8.0 (qui en a besoin).

**Déployé sur Kanvas (Kal-Games) le 31/10/2026 à 12:14 avec KG_Parkour 1.8.0 ; 1.28.0 dans `_removed-kalgames-1.28.0/` ; actif après redémarrage, non testé en jeu.**
