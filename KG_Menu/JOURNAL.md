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
hub : reprendre `item.games.*` du `lang.yml` de KalGames s'ils ont été modifiés. **Statut : déployé sur Kal-Games (7001) le 24/09/2026 par LeKiwi06, testé et confirmé par LeKiwi06 le 24/09/2026 (hub : boussole et étoile, accueil, Paramètres, catalogue « Interfaces », Java et Bedrock).**

## 1.1.0 — nombre de joueurs sur les boutons des jeux, paramètres par jeu (28/09/2026)

**Demandes de LeKiwi06 (28/09/2026)** : « pour chaque bouton visant à rejoindre un jeu, le plugin doit afficher combien
il y a de joueurs dedans » ; le bouton « Paramètres » ouvrait « Mini-jeux Kal-Games, Bingo, Classements (modération) » :
« le Bingo est un mini-jeu aussi, "mini-jeux Kal-Games" est une répétition, classement modération surcharge pour
rien » ; classements et paramètres vont dans le comparateur « Informations » (KLM_Menu 2.4.0).
- **Accueil (étoile)** : chaque jeu affiche « | n en jeu » (nouveau champ `players` de `MenuProvider.Entry`, -1 =
  inconnu, rien d'affiché ; l'ancien constructeur à 4 champs existe toujours). Plus de bouton « Paramètres » ni
  « Classements » ici.
- **Paramètres** : les réglages fournis par les plugins (`MenuProvider.settings`) sont des boutons à plat de
  « Informations > Paramètres » (`MenuSection.expand`), un par jeu : course de bateau, parcours, PvP Kit, Rush, Bingo...
  et « Parties en cours ». Plus d'écran « Paramètres Kal-Games » intermédiaire.
- Étoile : en créatif, sa case ne peut plus être remplacée (comme la boussole) ; inventaire renvoyé après un refus.
- Étoile déclarée à KLM_Menu pour `/menu` (ouverture, `/menu on | off`) ; pas d'étoile donnée à un joueur qui a fait
  `/menu off`.

**Déploiement** : avec KLM_Menu 2.4.0 (obligatoire), KalGames 1.21.0, KG_ScoreBoards 1.6.0, KG_Bingo 1.6.0,
KG_BuildBattle 0.2.0. **Statut : déployé le 28/09/2026 à 3 h 40 (ancien jar dans `_removed-…`), non testé en jeu.**

## 1.2.0 - accueil Créer / Rejoindre / Parties en cours, menus en coffres ou en fenêtres (20/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (20/10/2026)**, sur maquette validée : « faire une option pour switch du /dialog au /coffre dans
l'interface, paramètre, affichage », « quand on ouvre l'interface on voit "créer une partie", "rejoindre une partie",
"voir les parties en cours" », « montrer toutes les parties avec le titre du jeu et l'hôte ».
- **Accueil** (étoile, `/kalgames menu`, « Kal-Games » du catalogue) : Créer une partie (choix du jeu, puis ses
  réglages fournis par le plugin du jeu), Rejoindre une partie (toutes les parties ouvertes, tous jeux confondus : nom
  du jeu, hôte, joueurs / places, en attente ou en jeu ; « Entrer un code »), Parties en cours (parties lancées, à
  regarder), « Recréer ma dernière partie », Paramètres.
- **Paramètres › Affichage** : menus en **coffres** (par défaut, `affichage.coffres-par-defaut`) ou en **fenêtres** ;
  choix de chaque joueur dans `affichage.yml`. Les deux modes ont la même organisation.
- API (`MenuProvider`, ajouts avec valeurs par défaut : les plugins compilés avant restent compatibles) : `Jeu`,
  `Partie`, `Derniere`, `jeux`, `parties`, `derniere`, `code`. Un plugin pas encore adapté (KG_Bingo, KG_BuildBattle)
  apparaît dans « Créer une partie » avec son ancien menu en fenêtre (icône : `icones.<id>`) ; ses parties ne sont pas
  encore dans les listes (étape 6).
- Pour les autres plugins de kal-games : `coffres(joueur)`, `clic(joueur)` (clic gauche / droit / Maj dans un menu en
  coffre), `message(...)` (barre d'action si un coffre est ouvert, sinon tchat), `openCreate`, `openJoin`,
  `openDisplay`.

- **Échap ramène à la page précédente** (LeKiwi06, 20/10/2026) : dans les coffres, `echap(joueur, menu, retour)` :
  fermé par le joueur lui-même (raison `PLAYER`), le menu rouvre la page précédente un tick plus tard ; pas en ouvrant
  un autre menu, une fenêtre de saisie ou en changeant de monde. Dans les fenêtres de l'accueil, « Retour » est le
  bouton de sortie (déclenché par Échap).

**À déployer avec KalGames 1.26.0.** Nouvelles clés `affichage`, `icones` : valeurs par défaut dans le code.

**Compilé, non déployé, non testé en jeu.**
