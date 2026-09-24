# JOURNAL — KG_Menu

Menu du serveur **kal-games** : accueil (liste des jeux et autres boutons) et accueil « Paramètres », alimentés par
les plugins de kal-games. Place dans la hiérarchie des interfaces (décidée par LeKiwi06 le 24/09/2026) :
**KLM_Menu** (tous les serveurs : navigation, boussole, catalogue) → **menu de chaque serveur** (ici KG_Menu ; plus
tard créa, skyblock, survie...) → interfaces des jeux et fonctions, gérées par leurs propres plugins.

## 1.0.0 — création (24/09/2026)

**Demandes de LeKiwi06** : « les interfaces des différents mini-jeux sont appelées par KG_Menu dans les plugins
respectifs » ; « quand on crée un jeu et qu'il a donc son interface de prévue, il la transmet / soit détectée au
restart par KG_Menu ».
- **Découverte au démarrage** : chaque plugin de kal-games déclare un fournisseur (`fr.kalium.kgmenu.api.MenuProvider`,
  registre de services de Paper) avec ses jeux, ses autres boutons, ses réglages et, s'il veut, le menu de la partie
  en cours du joueur. KG_Menu les trouve au démarrage (journal : « N fournisseur(s) d'interface trouvé(s) ») et à
  chaque ajout / retrait. Les listes sont demandées à chaque ouverture (un mini-jeu créé en jeu apparaît aussitôt).
- **Accueil** (repris de KalGames, même titre « Mini-jeux Kal-Games ») : jeux de KalGames, Bingo, puis Classements,
  puis « Paramètres » pour ceux qui ont des réglages. **Paramètres** : réglages de chaque plugin (mini-jeux
  Kal-Games, Bingo, classements (modération)).
- **Objet du hub** (étoile « Mini-jeux », repris de KalGames ; `hub-item.slot` / `hub-item.material`, textes
  `item.games.*`) : menu de la partie en cours si un jeu s'en charge, sinon l'accueil. Verrouillé (pas de permission
  de contournement par défaut, comme avant). KalGames le donne quand il prépare le hub (`giveHubItem`).
- Dans le catalogue de KLM_Menu : une seule entrée **« Kal-Games »** (+ « Kal-Games : paramètres » pour les admins).
- Dépend de KLM_Menu (boîte à outils des menus).

**Déploiement** : avec KLM_Menu 2.1.0, KalGames 1.16.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0. Textes de l'objet du
hub : reprendre `item.games.*` du `lang.yml` de KalGames s'ils ont été modifiés. **Statut : compilé, non déployé.**
