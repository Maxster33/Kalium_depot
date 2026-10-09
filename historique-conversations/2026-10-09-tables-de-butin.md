# 2026-10-09 — Tables de butin des récompenses

- Plugin(s) concerné(s) : KG_Rewards (Kal-Games), KV_Rewards (Kanvas)
- Versions avant / après : KG_Rewards 1.0.0 / 1.1.0 (Kal-Games) ; KV_Rewards 1.0.0 / 1.1.0 (Kanvas) ; déployés le 09/10/2026 à 07:05 avec leur `butin.yml`, non testés

## Demandé

LeKiwi06 : « il faut créer les loot table des rewards, pour commencer il faut déterminer une valeur moyenne pour chaque
rewards, et une valeur pour chaque items qui n'a pas encore de prix ». Puis : « triple les prix des badges, ils sont
bien trop peu chers, ce sont des abilités permanentes même si peu puissantes au niveau 1 » ; « je valide le reste, on
passe aux pools » ; « tu peux me montrer des exemples de tirages pour chaque niveau de loot ? » ; « il faut que les
coffres aient l'air bien remplis, même si la plupart des items ne valent pas grand chose » ; « ajoute la lecture par id
et génère les butin.yml » ; « oui déploie, et donne moi 10 récompenses de chaque dans mon reward sur kixster pour tester ça ».

## Fait

- Simulation des paliers gagnés par un joueur type, avec les règles de KG_Rewards (grille générale + une grille par
  jeu, semaine, mois et permanent : les mêmes points paient dans six grilles).
- Classeur `sortie/valeurs-rewards.xlsx` (hors dépôt) : ancres, valeur de chaque récompense, joueurs types, tops,
  objets sans prix, pools, réglage de chaque niveau.
- Exemples de tirages simulés (`sortie/rewards/tirages.py`).
- KG_Rewards 1.1.0 et KV_Rewards 1.1.0 : objet vanilla écrit par son id dans `butin.yml`.
- `butin.yml` de Kal-Games et de Kanvas générés dans `sortie/rewards/butin/` (hors dépôt), relus avec le lecteur YAML
  du plugin ; ids vanilla comparés à la liste des objets de l'API Paper 26.2 ; valeur de chaque niveau recalculée
  depuis les fichiers (écart avec la cible : moins de 1 %).

- Commande d'essai `/kgrewards test` et `/kvrewards test` (admins et console) : récompenses tirées dans les tables et envoyées
  dans la boîte choisie ; un message en attente peut porter sa propre boîte.
- Déployé le 09/10/2026 à 07:05 : jars en place identiques à `jars-deployes/`, aucun `butin.yml` existant ; anciens jars
  dans `_removed-kg_rewards-1.0.0/` et `_removed-kv_rewards-1.0.0/` ; réservations libérées.
- Constaté sur Kixster : KS_RewardsGUI 1.0.1, KS_Jetons 1.0.0, KS_KaliumGive 1.8.0 (pas de jeton de fly ni de badges).

## Décisions

Validé par LeKiwi06 :
- Paliers : pas en points x valeur d'un point (0,01 émeraude en semaine et en mois, 0,02 en permanent) : minimale 1,
  petite 5, moyenne 10, grosse 100, exceptionnelle 1 000 ; le double en permanent. Un joueur régulier (5 h par semaine)
  touche environ 30 émeraudes par heure de mini-jeu, la moitié d'un farm efficace.
- Tops : top 1 de la semaine = 20 (un jeton de téléportation), 2 = 12, 3 = 8, 5 = 5, 10 = 3, 25 = 2, 50 = 1,5, 100 = 1 ;
  tops en % : 0,5 à 5 ; mois x 4 ; permanent = semaine.
- Objets : jeton de fly 30, jeton de la mort 300 (150 avant), jeton de claim 1 000 (5 000 avant), jeton de
  téléportation 20 ; `jeton_emplacement` à retirer du barème.
- Badges de niveau 1 : fly 450, téléportation 600, localisation 750, mort 3 600 (le triple de la proposition de
  Claude) ; niveau suivant = 2 badges du niveau en dessous + l'XP de la fusion.
- Récompenses bien remplies : 4 à 24 tirages par palier, pool Commun de petites piles sans valeur.

Choix de Claude, signalés :
- Un 5e pool « Peu commun » entre le remplissage et le Rare.
- Hors des pools : Cœur de Spawner (events seulement), spawners, Élixir de Fortune, étoile du Nether, têtes.
- 9 entrées viennent du Nether ou de l'End : les pools ne tiennent pas compte des dimensions fermées.
- Tops 1 à 100 et concours : récompense fixe courte, sans tirage.
- Kanvas : paliers calés sur une hypothèse de 20 notes reçues par heure de build, jamais mesurée.
- Les `butin.yml` et le classeur restent hors du dépôt (ils donneraient les chances de chaque objet aux joueurs).

Proposé, sans réponse : garantir la moitié de chaque récompense en récompense fixe (moins d'effet loterie).

## Reste à faire

- Redémarrer Kal-Games et Kanvas (l'humain), lire la console (aucune ligne « objet inconnu »), ouvrir
  `/kgrewards admin` et `/kvrewards admin`.
- Sur Kal-Games : `/kgrewards test LeKiwi06 semaine tout 10 kixster` (et `mois`, `permanent`) ; les récompenses avec un
  jeton de fly ou un badge ne se récupèrent pas sur Kixster tant que KS_Jetons 2.0.1 et KS_KaliumGive 1.9.0 n'y sont pas.
- Tester en jeu : franchir un palier, récupérer la récompense sur Event avec `/rewards` (il faut autant de cases libres
  que de tirages).
- Reporter les nouveaux prix dans le barème (`sortie/bareme`, puis `rachats.csv` de KS_Economy).
- Mesurer le rythme réel des notes sur Kanvas et recaler ses paliers.

## Suite (13 h) : premier test et coffres « Non ouvert » (KS_RewardsGUI 1.1.0 → 1.2.0)

- Plugin concerné : KS_RewardsGUI (Event, Kixster) ; déployé le 09/10/2026 à 13:30 (« oui déploie »), non testé.
- Test de LeKiwi06 à 07:18 : 180 récompenses d'essai envoyées sur Kixster, 174 récupérées. « il y a même 6 récompenses
  que je ne peux pas claim, même avec les 36 slots de libre » : lues sur Kixster, elles contiennent toutes un jeton de
  fly ou un badge, qui n'y existent pas encore (KS_Jetons 1.0.0).
- « il y a trop d'ender pearl dans les loot box que j'ai ouvert » : son journal de récupération le confirme (6 grosses
  sur 8, 5 exceptionnelles sur 6). Pools corrigés (2 perles : poids 10 → 4 ; 16 perles : 8 perles, poids 10 → 3) ; une
  grosse en contient maintenant 14 % du temps au lieu de 36 %. Fichiers régénérés, pas encore renvoyés.
- « il faut qu'on récupère les récompenses dans des coffres renommés, avec un tag "non ouvert" pour s'assurer qu'ils
  n'ont pas été lootés (on pourra vendre nos loot box comme ça) ». Réponses aux questions de Claude : « 1 : option 1 »
  (clic droit, coffre en main), « 2 : contenu caché », « 3 : uniquement en vente directe pour le moment ».
- Fait : KS_RewardsGUI 1.2.0 (détail dans son `JOURNAL.md`).
- Choix de Claude signalés : vrai coffre vanilla renommé (visible tel quel sur Bedrock, rien à ajouter sur le proxy) ;
  contenu gardé par le plugin et non dans l'objet ; coffres de mort inchangés ; pas de renommage à l'enclume ; points
  versés à celui qui ouvre.
- Déployé à 13:30 : KS_RewardsGUI 1.2.0 sur Kixster et Event ; `butin.yml` corrigés sur Kal-Games et Kanvas (anciens
  fichiers dans `_removed-kg_rewards-butin-2026-10-09/` et `_removed-kv_rewards-butin-2026-10-09/`) ; réservation libérée.
- Reste à faire : redémarrer les quatre serveurs (l'humain), tester (récupérer un
  coffre, l'ouvrir en Java et en Bedrock, prendre une partie, le rouvrir, l'échanger) ; envoyer la catégorie 7 sur Kixster.

## Suite (15 h) : rework des récompenses (KG_Rewards 1.2.0, KV_Rewards 1.2.0, KS_RewardsGUI 1.3.0)

- Présentation faite, feu vert de LeKiwi06 (« oui déploie, et transfère aussi le schem save "kixsspawn" vers kixster depuis kanvas ») : déployés le 09/10/2026 à 15:12 sur Kal-Games, Kanvas, Event et Kixster ; non testés ; réservations libérées. Schéma `kixsspawn.schem` copié de Kanvas vers `plugins/WorldEdit/schematics/` de Kixster.
- LeKiwi06 : « ça marche sur Event, mais je n'aime pas les récompenses » ; valeur moyenne affichée sur le coffre ;
  émeraudes « données dans le coffre, pas via un message dans le tchat » ; « il faut diversifier les récompenses : je
  n'ai eu que 3 blocs différents », « beaucoup de flèches mais aucune chair putréfiée, pas d'œil d'araignée »,
  nourriture pas assez variée, « des lingots de netherite mais pas de débris antiques », « les versions lingots mais pas
  les versions ore », « pas eu de cuivre ni d'améthyste » ; équipement en fer, or et diamant enchanté parfois (« mending
  sur le fer et l'or, mais pas sur le diamant ») ; livres enchantés au hasard et livres plus rares dans les meilleures
  récompenses ; potions classiques ; blocs de construction par stacks entiers pour les blocs non précieux ; disques,
  éclats de poterie, décorations d'armures ; « fais-moi une présentation du rework avant de le déployer sur mon feu
  vert ».
- Fait : pools refaits par familles d'objets (512 entrées au lieu de 96), émeraudes en vrais objets ; plugins : objets
  enchantés à chaque tirage, potions par type, valeur du niveau transmise et affichée sur le coffre, points mis en
  émeraudes dans le coffre.
- Choix de Claude signalés : enchantements au hasard sans trésor (Raccommodage seulement par des entrées dédiées, en
  fer et en or, dans le pool Légendaire, pour garder sa valeur de 1 500) ; valeur affichée = moyenne du niveau (pas
  celle du coffre) ; un seul objet inhabituel par pile ; valeurs moyennes des niveaux inchangées.
- Reste à faire : redémarrer les quatre serveurs (l'humain), lire les consoles, tester en jeu (équipement et livres enchantés, potions, valeur sur le coffre, émeraudes dans le coffre) ; charger le schéma sur Kixster.
