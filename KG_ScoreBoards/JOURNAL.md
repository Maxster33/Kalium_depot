# JOURNAL — KG_ScoreBoards

Classements des mini-jeux : points cumulés et meilleurs temps (général depuis la création du serveur + mois en
cours), archives mensuelles, panneaux flottants du hub et menus de classement. Sorti de KalGames le 24/09/2026
(demande de LeKiwi06 : un plugin = un rôle). Tourne sur **kal-games**.

## 1.0.0 — création : classements sortis de KalGames 1.13.0 (24/09/2026)

**Décisions de LeKiwi06** : plugin **autonome** (option 1) ; KalGames dépend de KG_ScoreBoards (et non l'inverse) ;
les résultats du Bingo arriveront plus tard par KaliumRelay ; archives détaillées (futur bot Discord).

- Déplacés de KalGames (`git mv`) : `StatsService` (données : `stats.yml`, `archives/`), `BoardService` (panneaux :
  `boards.yml`), `RankingMenus` (menus joueurs et modérateurs). **`StatsService` compilé identique à celui de
  KalGames 1.13.0** (seul le nom du paquet change) : mêmes fichiers, même format, mêmes règles.
- Copiés de KalGames (à réunir plus tard dans le socle commun KG_Core) : `Gui` (menus Dialog) et `Lang` (textes
  MiniMessage, `lang.yml` propre à KG_ScoreBoards, mêmes clés et mêmes textes par défaut : `rank.*`, `board.*`,
  `menu.back`, `admin.denied`, `gui.*`, `prefix`).
- KG_ScoreBoards ne connaît pas les mini-jeux : chaque plugin lui fournit ses **classements** (`Category` :
  identifiant, nom affiché, type POINTS / TIME / LAP) par `addCategories`, puis enregistre points et temps par
  `stats()` et ouvre les menus par `openPlayer` / `openAdmin` en donnant l'action du bouton « Retour ». Règle
  « modérateur » et monde interdit aux panneaux fournis par KalGames (`setAdminCheck`, `setForbiddenWorld`).
- Correctif en passant : le retour du menu modérateur est mémorisé par modérateur.
- Panneaux déjà placés : même étiquette interne (`kalgames_board:`), ils sont repris tels quels.
- `config.yml` : `stats.timezone`, `boards.scale`, `gui.button-width` (valeurs reprises de KalGames).

**Migration sur kal-games** (serveur ARRÊTÉ, à faire avec KalGames 1.14.0) :
1. Créer `/plugins/_removed-kalgames-1.13.0/` : y déplacer `KalGames-1.13.0.jar` et y COPIER `plugins/KalGames/`
   `stats.yml`, `boards.yml`, `lang.yml`, `config.yml` et le dossier `archives/`.
2. Créer `/plugins/KG_ScoreBoards/` et y DÉPLACER `stats.yml`, `boards.yml` et `archives/` depuis `plugins/KalGames/`.
3. Si `stats.timezone` ou `boards.scale` ont été changés dans la config de KalGames, écrire les mêmes valeurs dans
   `plugins/KG_ScoreBoards/config.yml` (sinon laisser le fichier par défaut se créer). Idem pour les textes
   `rank.*` / `board.*` personnalisés dans le `lang.yml` de KalGames.
4. Envoyer `KG_ScoreBoards-1.0.0.jar` et `KalGames-1.14.0.jar`.
5. Démarrer ; vérifier « KG_ScoreBoards actif », puis les classements et un panneau du hub.

**Statut : déployé sur kal-games le 24/09/2026** (migration faite par Claude : `stats.yml` et `boards.yml` déplacés, pas
encore d'`archives/` ; textes des classements repris du `lang.yml` de KalGames ; démarrage vérifié dans le journal).

## 1.1.0 — boîte à outils et catalogue de KLM_Menu (24/09/2026)

- Menus et textes : boîte à outils de KLM_Menu (`fr.kalium.menu.api`) ; copies locales de `Gui` et `Lang`
  supprimées. Dépend de KLM_Menu.
- Interfaces déclarées dans le catalogue de KLM_Menu : **« Classements »** (joueurs : liste des jeux → Top 10) et
  **« Classements (modération) »** (admins : liste des jeux → menu modérateur : complets, archives, panneaux, clôture).
- API : `addCategories(source, identifiants)` pour que les classements d'un plugin puissent être listés.
**Déploiement** : avec KLM_Menu 2.0.0, KalGames 1.15.0. **Statut : déployé le 24/09/2026, non testé en jeu.**

## 1.2.0 — boutons fournis à KG_Menu (24/09/2026)

- « Classements » (accueil de kal-games) et « Classements (modération) » (Paramètres, admins) fournis à KG_Menu au
  lieu d'être inscrits directement dans KLM_Menu. Dépend de KG_Menu.
**Déploiement** : avec KG_Menu 1.0.0, KalGames 1.16.0. **Statut : déployé sur Kal-Games (7001) le 24/09/2026, non testé en jeu.**
