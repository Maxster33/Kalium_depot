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

## 1.1.0 - objets vanilla par leur id dans butin.yml (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « ajoute la lecture par id et génère les butin.yml ». Les tables de butin (5 pools, 96 entrées,
54 niveaux) sont préparées dans un tableau ; les saisir dans l'interface admin demandait de déposer chaque objet
vanilla dans un coffre, un par un.

Cause : un objet vanilla n'était enregistré que sous la forme `donnees` (l'objet sérialisé par le serveur, en base64),
qu'on ne peut pas écrire hors du serveur.

- **`butin.yml`** : un objet vanilla peut aussi être écrit par son id, `{type: objet, id: diamond, nombre: 2}`, avec au
  besoin `enchantements: {mending: 1}` (livre enchanté : enchantement stocké ; autre objet : enchantement posé). Au
  chargement, il est remplacé par l'objet créé par le serveur (`donnees`), exactement comme s'il avait été déposé dans
  l'interface admin : rien ne change pour le tirage, l'envoi ni KS_RewardsGUI.
- Id ou enchantement inconnu : l'élément est ignoré et signalé dans la console (`butin.yml : objet inconnu « ... »`).
- Le fichier garde les ids tant que personne ne modifie une table dans l'interface admin ; à la première modification,
  le plugin réécrit tout le fichier avec les `donnees` (comportement inchangé), commentaires compris.
- Aucun autre changement (interface admin, paliers, tops, envois).

Limites : écrit sans serveur de test ; la création des objets par id n'a été vérifiée qu'à la compilation. Les ids et le
format des fichiers générés ont été vérifiés hors serveur (lecteur YAML du plugin, liste des objets de l'API Paper
26.2). Empilé sur la 1.0.0 jamais testée en jeu (demande explicite de LeKiwi06).

Les `butin.yml` générés (Kal-Games et Kanvas) sont dans `sortie/rewards/butin/` (hors dépôt) ; valeurs validées par
LeKiwi06 le 09/10/2026.

**Commande d’essai** (demande de LeKiwi06 : « donne moi 10 récompenses de chaque dans mon reward sur kixster pour tester ») :
`/kgrewards test <joueur> <semaine | mois | permanent> <niveau | tout> [nombre] [boîte]` (admins `kgrewards.admin` et console ; nombre de 1 à 100).
Tire des récompenses dans les tables de butin et les envoie comme de vraies récompenses (raison « Essai n - <niveau>
(<période>) »), dans la boîte de `config.yml` ou dans celle indiquée (ex. `kixster`) ; aucun palier ni top n’est marqué
comme atteint. Pour cela, un message en attente peut porter sa propre boîte (première ligne `#boite:<nom>` dans
`envois.yml`, retirée à l’envoi). Les vraies récompenses partent toujours vers la boîte de `config.yml`.

**Déployé sur Kal-Games le 09/10/2026 à 07:05 (LeKiwi06 ; 1.0.0 dans `_removed-kg_rewards-1.0.0/`) avec `plugins/KG_Rewards/butin.yml` (aucun n'existait avant), actif depuis le redémarrage de 07:12 (journal lu : 1.1.0 activée, aucune erreur, aucun objet du `butin.yml` refusé). `config.yml` du serveur non touché. Statut : non testé en jeu.**
