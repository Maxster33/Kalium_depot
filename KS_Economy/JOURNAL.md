# KS_Economy - journal

Plugin du serveur Event : économie. Cahier des charges : catégorie 2 « Économie » de LeKiwi06 (validé le 29/09/2026 ;
copié dans `CAHIER_DES_CHARGES.md` au déploiement). Codé en 2 temps (réponse de LeKiwi06) : score, blocs, `/echange`
d'abord ; magasins (Tradeshop) après KS_Claim (catégorie 3).

## 1.0.0 - score, blocs compressés, /echange, Vault (30/09/2026)

- **Score** : 1 émeraude = 1 point. Taux réglables dans `config.yml` (valeurs par défaut dans le code) :
  `conversion.depot` (points reçus par émeraude déposée) et `conversion.retrait` (points payés par émeraude retirée),
  1 = 1:1. Dépôt arrondi à l'unité inférieure, coût du retrait à l'unité supérieure. Comptes dans
  `plugins/KS_Economy/comptes.yml` (enregistrés dans la minute qui suit un changement et à l'arrêt).
- **Menu « Économie »** (`/economie`, `/eco` ou bouton du menu d'Event, KS_Menu) : solde ;
  - **Tout déposer** : émeraudes, blocs d'émeraude et blocs compressés de l'inventaire (hors armure et main secondaire) ;
  - **Déposer des objets** : coffre où poser ce qu'on dépose ; à la fermeture, les émeraudes sont converties, le reste
    est rendu ;
  - **Retirer** : émeraude, bloc d'émeraude ou bloc compressé tier 1 à 6, et nombre ; refusé si le solde ou la place
    manquent ;
  - **Masquer / Révéler mon solde**.
- **Liste des joueurs (Tab)** : « Pseudo 1 234 » (solde en vert), visible par tous ; masqué : pseudo seul. Solde masqué :
  **1 %** prélevé chaque jour au premier passage après minuit (heure de Paris), arrondi à l'unité inférieure, rien si le
  solde est nul (`masquage.pourcent-par-jour`). Serveur éteint à minuit : prélèvement au démarrage suivant, une seule
  fois. Révéler est gratuit et immédiat.
- **Blocs d'émeraude compressés** : tier n = 10^n blocs d'émeraude = 9 x 10^n points (90 à 9 000 000). Bloc d'émeraude à
  aspect enchanté, nom « Bloc d'émeraude compressé (tier n) », valeur en description, marqueur `ks_economy:tier`.
  Obtenus seulement par le retrait. **Impossibles à poser** et **à décrafter** (aucune recette vanilla ne les accepte,
  établi et autocrafteur ; les recettes des plugins, oui : Élixir de Fortune).
- **/echange** `<joueur>` (puis `accepter` / `refuser`, message cliquable ; demande valable 60 s) : 2 joueurs à 5 blocs
  au plus. **Panneau d'échange** (coffre de 6 rangées) : à gauche sa part, à droite celle de l'autre (aperçu des objets,
  montant, validation). Boutons « Objets » (coffre de 16 cases où déposer ses objets), « Montant » (saisie, limitée au
  solde), « Valider », « Annuler ». L'échange a lieu quand les deux ont validé ; ouvrir « Objets » ou « Montant » et
  toute modification retirent les deux validations. Annulé, objets rendus, si un joueur s'éloigne à plus de 5 blocs,
  ferme le panneau, se déconnecte, meurt (objets au sol), reste plus de 2 minutes dans la saisie du montant, ou à
  l'arrêt du serveur. Refusé (validations retirées) si un solde ne suffit plus ou si un inventaire est trop plein pour
  recevoir les objets. Chaque échange est noté dans la console.
- **Vault** (nécessaire techniquement, signalé dans le cahier ; VaultUnlocked 2.20.2, téléchargé avec l'accord de
  LeKiwi06) : KS_Economy est déclaré comme économie Vault (points entiers, pas de banques), pour SimpleClaimSystem
  (catégorie 3). Sans Vault : avertissement, le reste fonctionne.
- **Autres plugins** : `solde(uuid)`, `crediter(uuid, points)`, `debiter(uuid, points)`, `creerBloc(tier)`,
  `tierBloc(objet)` (KS_Elixir 1.1.0).
- Textes : `plugins/KS_Economy/lang.yml` (créé au premier démarrage). `depend` KLM_Menu, `softdepend` KS_Menu, Vault ;
  `build.sh` : compiler KLM_Menu et KS_Menu d'abord ; `telecharger-outils.sh` télécharge VaultUnlocked.

Limites :
- Pas de commande d'administration des soldes (pas demandée).
- Un joueur peut toujours faire `/menu off` : le menu Économie reste ouvrable par `/economie`.

**Déployé sur Event le 01/10/2026 à 00:49 (LeKiwi06) avec KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 et
VaultUnlocked 2.20.2 (nouveau plugin tiers, `plugins/VaultUnlocked-2.20.2.jar`), actifs après redémarrage
d'Event. Statut : non testé en jeu.**

## 1.0.1 - textes des boutons du menu Économie (01/10/2026)

Bug vu par LeKiwi06 au premier test : les boutons du menu Économie affichaient « MemorySection[path='menu.tout-deposer',
... » au lieu de leur texte. Cause : dans `lang.yml`, une clé (`menu.tout-deposer`) et sa description
(`menu.tout-deposer.info`) : la 2e transforme la 1re en section YAML. 9 clés concernées (boutons du menu Économie et
du panneau d'échange). Correctif : nouveaux noms (`menu.bouton-tout-deposer`, `menu.info-tout-deposer`,
`echange.bouton-objets`...) ; les anciennes clés restent dans le `lang.yml` du serveur, sans effet.

**Déployé sur Event le 01/10/2026 à 01:33 (1.0.0 dans `_removed-ks_economy-1.0.0/`), actif après redémarrage d'Event.
Statut : non testé en jeu.**

## 1.0.2 - plus aucun texte qui défile (01/10/2026)

Demande de LeKiwi06 : des textes trop longs « défilent » dans les boutons et les champs ; « il ne faut jamais que ça
arrive, c'est illisible ». Largeur de chaque texte de bouton et de champ mesurée (police de Minecraft) : seule la liste
du retrait débordait (« Objet (valeur en émeraudes): Bloc compressé tier 3 (9 000) », ~304 pixels pour un champ de 260).
- Liste du retrait : « Objet » et « Émeraude », « Bloc d'émeraude », « Compressé tier 1 » à « Compressé tier 6 »
  (120 pixels au plus) ; les valeurs en points sont écrites dans le texte du menu (qui passe à la ligne).
- Titre du coffre de dépôt raccourci : « Déposer des émeraudes ».
- Nouvelles clés de `lang.yml` (`retrait.champ-objet`, `retrait.nom-…`, `retrait.valeurs`, `depot.titre-coffre`) : le
  `lang.yml` du serveur garde les anciens textes sous les anciennes clés, qui ne servent plus.
- Boutons vérifiés : tous à moins de 110 pixels pour 240 (« Déposer des objets » : 102).

**Déployé sur Event le 01/10/2026 à 18:54 (1.0.1 dans `_removed-ks_economy-1.0.1/`), actif après redémarrage d'Event.
Statut : non testé en jeu.**

## 1.0.3 - saisie du montant ajustée (01/10/2026)

Suite de 1.0.2 (voir KLM_Menu 2.5.0) : la fenêtre de saisie du montant de `/echange`, construite à la main, ajuste
aussi ses boutons avec `Lisible`. Le reste des menus passe par `Gui`, ajusté par KLM_Menu 2.5.0.

**Non déployé. À déployer ensemble (règle 3.5) : KLM_Menu 2.5.0 sur les 6 serveurs Paper (lobby, Kal-Games, Serveur
Jeux, Kixster, Event, Kanvas), KalGames 1.22.1 (Kal-Games), KG_BingoGame 0.8.4 (Serveur Jeux), KS_Economy 1.0.3
(Event) : ces trois-là ne démarrent pas sans KLM_Menu 2.5.0. Statut : non testé en jeu.**
