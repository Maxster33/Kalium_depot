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
