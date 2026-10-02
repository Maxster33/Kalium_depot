# Cahier des charges - Catégorie 4 : Récompenses (KG_Rewards, KV_Rewards, KS_RewardsGUI, KLM_Menu) - LeKiwi06

Serveurs : kal-games (KG_Rewards), Kanvas (KV_Rewards), **Event** (KS_RewardsGUI ; destination future : Kixster SMP),
tous les serveurs Paper (KLM_Menu). État : **cahier validé par LeKiwi06 le 30/09/2026** (session 4).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_RewardsGUI :
> plugin de réception des récompenses envoyés par KG_Rewards , KV_Rewards , et dans le futur SK_rewards , etc
>
> KG_Rewards , KV_rewards ,etc  :
> plugins pour configurer le déblocage des récompenses de paliers  , et des tops permanents , hebdomadaire et mensuels
>
> KLM_Menu :
> Ajouter un bouton Récompenses dans le compateur "informations" pour les interfaces de configuration d'admin et les consultation pour les joueurs afin de voir leur progression dans les divers paliers et top  des divers plugins de Rewards .

## To do list du Kixster SMP (fichier daté ; la demande du 29/09 fait foi en cas de différence)

> /rewards : interface de collecte des récompenses envoyés par les autres serveur de jeux a chaques joueurs ( on créera les plugins pour générer et envoyer ses récompenses dans un autre script )

Autres mentions : potions « principalement des loots de mobs et des recompenses dans /rewards » ; anti-triche
(catégorie 6) : « récompenses jamais utilisé/ouverte ».

## Existant (dépôt, 30/09/2026)

- **KG_ScoreBoards** (kal-games) : points et meilleurs temps par mini-jeu, classements **général** et **du mois**,
  archives mensuelles, journal des parties, API HTTP (bot Discord). **Pas de classement hebdomadaire.**
- **KV_Plots** (Kanvas) : points des plots (somme des notes), votes datés (classement du mois prévu), pas encore de
  classements ; **KV_BuildBattle** : parties de Build Battle.
- **KaliumRelay** (proxy) : « boîte aux lettres » HTTP entre serveurs, qui marche même sans joueur connecté sur le
  serveur visé.
- **KLM_Menu** : comparateur « Informations » (classements pour tous, paramètres pour les admins) ; sur Event, donné
  seulement après `/menu on`.
- **KS_KaliumGive** (Event) : identifiants des objets custom du serveur Event (fioles, spawners, élixirs et têtes à
  venir...).

# 2. Réponses de LeKiwi06

| Question | Réponse (30/09/2026) |
|---|---|
| Contenu d'une récompense | **Tables de butin** (tirage) pour les paliers et les tops en % ; pour les vrais tops (1, 2, 3...) : récompense **fixe et précisée, sans hasard** |
| Destination | Tout arrive sur Event pour le moment (à faire évoluer plus tard) |
| Paliers | Pour les scores **permanents, hebdomadaires et mensuels** : tous les 100 points de 0 à 1 000 = récompense **minimale** ; tous les 500 de 0 à 10 000 = **petite** ; tous les 1 000 de 0 à 50 000 = **moyenne** ; tous les 10 000 jusqu'à 100 000 = **grosse** ; 100 000 = **exceptionnelle** |
| Prestige | Sur les paliers **permanents** : possibilité de passer au niveau de prestige supérieur ; le score permanent repart de 0 et les récompenses sont données en plus grosse quantité ; les scores hebdomadaire et mensuel ne sont pas remis à zéro |
| Kanvas | Les points de Build Battle sont déjà envoyés sur kal-games (donc récompensés par KG_Rewards) ; KV_Rewards : **notes reçues sur ses plots** (mensuel et permanent) et **classements des concours de build** |
| Tops | Tops en % : **50, 25, 10, 5, 1 %** (récompenses **cumulatives**) ; tops classiques : **100, 50, 25, 10, 5, 3, 2, 1** (**non cumulatifs** : seul le meilleur top du joueur compte). Un top **par jeu** + un top **global** (moyenne de tous les scores de jeu du joueur) |
| Interface de réception (Event) | `/rewards` + bouton KS_Menu ; liste des récompenses en attente (origine, raison, contenu) ; « Récupérer » / « Tout récupérer » ; refus si l'inventaire manque de place ; pas d'expiration : validé |
| Suivi des récupérations (anti-triche) | Oui : noter ce que chaque joueur récupère et quand |
| Bouton « Récompenses » de KLM_Menu (joueurs) | Seulement le plugin Rewards **du serveur où l'on est** |
| Interface admin | Créer / modifier / supprimer paliers et tops ; contenu : objets vanilla déposés, objets custom par identifiant, argent par montant : validé |
| Plugins partagés (KLM_Menu, KG_ScoreBoards, KaliumRelay) | Maxster33 travaille sur autre chose : LeKiwi06 a le champ libre (réservations quand même au moment du code) |
| Seuils communs à plusieurs grilles | Seulement la **plus grosse** récompense |
| Score des paliers | Paliers **général** et **par jeu** |
| Prestige | **Proposé, jamais obligatoire** ; sans prestige, le joueur peut toujours gagner les récompenses grosses et exceptionnelles au taux de son prestige actuel ; **+20 % de quantité par niveau** (adaptation à tous les gains à voir) ; compteur propre aux récompenses (classements intacts), mais le **niveau de prestige doit être affiché** pour ceux qui en ont |
| Tops hebdomadaires / mensuels | Classement final récompensé ; validation **le samedi à 15 h** (semaine) et **le 1er vendredi du mois à 21 h** (mois), pour tomber sur des pics de joueurs |
| Changement de top pendant la semaine | Top le moins bon occupé : d'accord |
| Récompense d'arrivée dans un top | **Une seule fois par top** |
| Tops en % | Rang arrondi au supérieur, parmi les joueurs qui ont au moins 1 point : d'accord |
| Top global | Moyenne des **5 meilleurs scores de jeu** du joueur (ne pas pénaliser les jeux qu'il n'aime pas) |
| Récompense des tops | Dès qu'on **atteint** un top, puis **chaque semaine passée** dans ce top (ex. : entré dans le top 5 un mardi = récompense immédiate ; toujours dans le top 5 le mardi suivant = récompense d'une semaine dans le top 5) |

| Au-delà de 100 000 sans prestige | Grosse tous les 10 000, exceptionnelle tous les 100 000 ; toujours la plus grosse seulement |
| Arrondi des +20 % | Arrondi au hasard (objets), argent arrondi à l'unité inférieure ; pour le moment, corrigeable plus tard |
| Affichage du prestige | « Pseudo (prestige x) » ; pas dans le chat ni la liste Tab |
| Périodes | **Alignées** sur les validations : semaine du samedi 15 h au samedi 15 h ; mois du 1er vendredi 21 h au 1er vendredi 21 h (clôture du mois de KG_ScoreBoards modifiée) |
| Top global | Toujours la moyenne des **5** meilleurs scores ; moins de 5 jeux joués = des 0 dans le calcul |
| Kanvas | Même système sur les notes reçues (mensuel et permanent) + récompense fixe par place aux concours de build : validé |

# 3. Questions restantes

Aucune. Le prestige **séparé par grille** (général, chaque jeu) était proposé sans réponse directe ; accepté avec la
validation de l'ensemble du cahier (30/09/2026).

# 4. Choix d'interprétation (validés le 30/09/2026)

## Éléments

| Élément | Serveur | Rôle |
|---|---|---|
| **KG_Rewards** (nouveau) | kal-games | Paliers et tops des mini-jeux (scores de KG_ScoreBoards, Build Battle compris) ; envoie les récompenses vers Event ; interface admin et progression des joueurs |
| **KV_Rewards** (nouveau) | Kanvas | Paliers et tops sur les notes reçues (KV_Plots) ; récompenses fixes des concours de build ; envoi vers Event ; interface admin et progression |
| **KS_RewardsGUI** (nouveau) | Event | Réception, `/rewards`, récupération, journal des récupérations |
| **KG_ScoreBoards** (modifié) | kal-games | Classement **hebdomadaire** (samedi 15 h -> samedi 15 h) ; mois aligné (1er vendredi 21 h -> 1er vendredi 21 h) ; affichage « Pseudo (prestige x) » dans les classements |
| **KLM_Menu** (modifié) | tous | Bouton « Récompenses » dans le comparateur « Informations » : ouvre l'interface du plugin Rewards **du serveur où l'on est** (progression pour les joueurs, configuration pour les admins) |
| **KaliumRelay** (modifié si nécessaire) | proxy | Transport des récompenses vers Event (boîte aux lettres, marche sans joueur connecté) |
| KS_Menu (catégorie 2) | Event | Bouton « Récompenses » (ouvre `/rewards`) |

## Récompenses

- **Tables de butin** (tirage pondéré) pour les paliers et les tops en % ; **récompense fixe** (sans hasard) pour les
  tops classiques (100, 50, 25, 10, 5, 3, 2, 1). Contenu : objets vanilla, objets custom d'Event (identifiants
  KS_KaliumGive), argent (score KS_Economy). Tout est livré sur Event.
- Configuration par l'interface admin : créer / modifier / supprimer ; objets vanilla déposés dans l'interface,
  objets custom par identifiant, argent par montant.

## Butin (décisions de LeKiwi06, 03/10/2026, au moment du code)

- **Tables de butin custom** : des **pools par rareté** (ex. Commun, Rare, Épique, Légendaire ; chaque pool : des
  contenus avec un poids), communs à tout. Chaque **niveau de récompense** (minimale, petite, moyenne, grosse,
  exceptionnelle, tops %, tops 1 à 100) a : un **nombre de tirages**, la **fréquence de chaque pool** (%), et des
  **récompenses fixes** données en plus du butin tiré.
- Niveaux réglés **séparément pour la semaine, le mois et le permanent** (les pools restent communs).
- Le **+20 % par niveau de prestige** ne s'applique qu'aux **paliers permanents** de la grille.

## Paliers (général et par jeu ; permanents, hebdomadaires, mensuels)

| Grille | Seuils | Récompense |
|---|---|---|
| Minimale | tous les 100, de 100 à 1 000 | table « minimale » |
| Petite | tous les 500, de 500 à 10 000 | table « petite » |
| Moyenne | tous les 1 000, de 1 000 à 50 000 | table « moyenne » |
| Grosse | tous les 10 000, de 10 000 à 100 000 (puis au-delà, sans prestige) | table « grosse » |
| Exceptionnelle | 100 000 (puis tous les 100 000, sans prestige) | table « exceptionnelle » |

- À un seuil commun à plusieurs grilles : **seulement la plus grosse**. Chaque seuil n'est récompensé qu'une fois par
  période (et par niveau de prestige pour les paliers permanents).
- Périodes : semaine (samedi 15 h -> samedi 15 h), mois (1er vendredi 21 h -> 1er vendredi 21 h), permanent.
- **Prestige** (paliers permanents) : proposé (bouton + confirmation) dès 100 000 points, jamais obligatoire.
  Prendre un prestige remet à 0 le **compteur de récompenses** (pas les classements de KG_ScoreBoards) et donne
  **+20 % de quantité par niveau** sur les récompenses. Quantités : objets arrondis au hasard (1 x 1,2 = 1, avec 20 %
  de chance d'avoir 2), argent arrondi à l'unité inférieure. Affiché « Pseudo (prestige x) » dans l'interface
  Récompenses et les classements. Un prestige **séparé par grille** (général, chaque jeu).

## Tops (par jeu + global ; permanents, hebdomadaires, mensuels)

- **Tops en %** : 50, 25, 10, 5, 1 % ; **cumulatifs** (un joueur du top 5 % reçoit aussi 50, 25 et 10 %). Top x % =
  rang <= x % du nombre de joueurs classés (au moins 1 point sur la période), arrondi au supérieur.
- **Tops classiques** : 100, 50, 25, 10, 5, 3, 2, 1 ; **non cumulatifs** (seul le meilleur top du joueur).
- **Top global** : moyenne des **5 meilleurs** scores de jeu du joueur (des 0 s'il a joué à moins de 5 jeux).
- **Hebdomadaires et mensuels** : classement final récompensé au moment de la validation (samedi 15 h ; 1er vendredi
  du mois 21 h).
- **Permanents** : récompense **dès l'arrivée** dans un top (une seule fois par top et par joueur, même en cas de
  sortie puis de retour), puis une récompense **pour chaque semaine** complète passée dans le top, comptée depuis le
  jour d'arrivée ; si le joueur a changé de top pendant la semaine, c'est le **moins bon** top occupé qui compte ; le
  compte des semaines repart à zéro s'il sort du top.

## Kanvas (KV_Rewards)

- Même système de paliers et de tops sur les **notes reçues** par ses plots, en **mensuel et permanent** seulement.
- **Concours de build** : récompense fixe par place au classement de chaque concours.

## Réception sur Event (KS_RewardsGUI)

- `/rewards` et bouton de KS_Menu : récompenses en attente (origine, raison, contenu) ; « Récupérer » /
  « Tout récupérer » ; refus si l'inventaire manque de place ; pas d'expiration.
- **Journal des récupérations** (joueur, récompense, contenu, date) pour l'anti-triche (catégorie 6).

## Coordination

- Maxster33 est sur autre chose : LeKiwi06 a le champ libre sur KLM_Menu, KG_ScoreBoards et KaliumRelay ; réservation
  dans `TRAVAIL_EN_COURS.md` quand même au moment du code.
