# KG_Rewards - journal

Plugin de kal-games : récompenses des mini-jeux. Cahier des charges : catégorie 4 « Récompenses » de LeKiwi06 (validé le
30/09/2026 ; décisions sur le butin du 03/10/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - paliers, tops, prestige, tables de butin (03/10/2026)

- **Grilles** : chaque mini-jeu de KG_ScoreBoards (Build Battle compris) + « général » (paliers : somme des points de
  tous les jeux ; tops : moyenne des 5 meilleurs scores de jeu, 0 pour les jeux manquants).
- **Paliers** (semaine, mois, permanent ; général et par jeu) : minimale tous les 100 jusqu'à 1 000, petite tous les 500
  jusqu'à 10 000, moyenne tous les 1 000 jusqu'à 50 000, grosse tous les 10 000 (aussi au-delà de 100 000),
  exceptionnelle tous les 100 000 ; à un seuil commun, seulement la plus grosse ; chaque seuil une fois par période.
  Vérifié à chaque ajout de points (`PointsAjoutesEvent` de KG_ScoreBoards 1.8.0).
- **Aucun palier rétroactif** : au premier démarrage, les scores actuels de chaque joueur servent de point de départ, et
  les tops permanents déjà occupés comptent comme atteints (sinon tous les anciens paliers seraient donnés d'un coup).
- **Prestige** (par grille, dès 100 000 points permanents, jamais obligatoire, bouton + confirmation) : le compteur des
  paliers permanents repart de 0 (classements de KG_ScoreBoards intacts) ; **+20 % de quantité par niveau sur les paliers
  permanents seulement** (objets arrondis au hasard, argent à l'unité inférieure) ; « Pseudo (prestige x) » dans les
  classements de ce jeu (KG_ScoreBoards 1.8.0).
- **Tops** : classiques 1, 2, 3, 5, 10, 25, 50, 100 (seul le meilleur) et 50, 25, 10, 5, 1 % (cumulés ; rang <= x % des
  joueurs qui ont au moins 1 point, arrondi au-dessus). Semaine et mois : classement final à la clôture de
  KG_ScoreBoards (`PeriodeClotureeEvent`, samedi 15 h et 1er vendredi 21 h). Permanent (vérifié toutes les 5 minutes) :
  à l'arrivée (une fois par top), puis chaque semaine passée dans le top depuis l'arrivée (le moins bon top occupé
  pendant la semaine compte ; le compte repart à zéro en sortant des tops).
- **Butin** (`plugins/KG_Rewards/butin.yml`, interface admin) : pools par rareté communs à tout (au départ : Commun, Rare,
  Épique, Légendaire, vides ; entrées : contenu + poids) ; pour chaque période (semaine, mois, permanent) et chaque niveau
  (5 paliers, 5 tops %, 8 tops) : nombre de tirages, fréquence (%) de chaque pool, récompenses fixes en plus. Contenu :
  objets déposés dans un coffre (copie exacte, rendus à l'admin), objets custom d'Event par id_custom, argent (points).
  Un niveau sans tirage ni fixe ne donne rien (noté dans la console).
- **Envoi** : message au format de KS_RewardsGUI, boîte `event` de KaliumRelay 1.3.0 ; file d'attente
  `plugins/KG_Rewards/envois.yml` (nouvel essai toutes les 15 secondes : rien n'est perdu si le relais est arrêté).
  Chaque récompense est notée dans la console.
- **Commandes** : `/kgrewards` (progression : compteurs et prochains paliers de chaque grille, prestige) ;
  `/kgrewards admin` (tables de butin, `kgrewards.admin`, opérateurs). Bouton « Récompenses » de KLM_Menu : étape 4.
- Données : `plugins/KG_Rewards/joueurs.yml`. Configuration : `relay-url`, `relay-token` (**vide dans le dépôt**),
  `boite`, `origine`. `depend` KG_ScoreBoards, KLM_Menu.

- **Bouton « Récompenses »** de KLM_Menu 2.6.0 : déclaré (`Recompenses`) ; ouvre la progression (bouton « Tables de butin » pour les admins). **KLM_Menu 2.6.0 obligatoire sur kal-games.**

**Déployé sur Kal-Games le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (nouveau), actif après redémarrage. Statut : non testé en jeu.**
