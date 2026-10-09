# KG_BuildBattle - journal

Serveur kal-games. Cahier des charges (commun avec KV_BuildBattle) : `KV_BuildBattle/CAHIER_DES_CHARGES.md`. Dépend de
KLM_Menu (menus) et de KG_Menu (bouton dans le menu du serveur). Dépend de KalGames depuis la 0.4.0 (objet « Rejouer »).

## 0.1.0 - bouton du menu, file publique, parties privées, envoi vers Kanvas (27/09/2026)

Demande de LeKiwi06 : « il nous faut aussi la partie plugin à mettre sur kal-games comme pour le Bingo, histoire que
KG_Menu appelle l'interface » ; liaison choisie : « comme le Bingo » (KG_BuildBattle gère lui-même la file publique et
les parties privées, le moteur de KalGames / futur KG_Instances n'est pas utilisé : il ne gère que des arènes collées
sur kal-games).
- **Bouton « Build Battle »** dans le menu du serveur (KG_Menu, `MenuProvider`, ordre 30).
- **File publique** : Solo, Duo, Trio, Squad, tempo normal (5 min). Le joueur est envoyé tout de suite sur Kanvas,
  dans la salle d'attente (le compte à rebours y aura lieu : étape 2).
- **Partie privée** : l'hôte choisit le tempo (fast 3 min, normal 5, longue 10, extra 30), la taille des équipes (1 à
  4) et le mode « thèmes écrits » ; code de 4 caractères ; jusqu'à 8 équipes (`equipes-max`). Les autres la
  rejoignent dans la liste du menu (10 plus récentes) ou avec le code. L'hôte la lancera depuis la salle d'attente.
- **Envoi** : l'affectation (public + taille d'équipe, ou partie privée + réglages) est déposée sur le relais HTTP
  (clé `buildbattle-<uuid>`), **puis** le joueur est envoyé sur `serveur` (Kanvas) ; si le relais ne répond pas, le
  joueur n'est pas envoyé (message).
- **Liste des parties privées** : une partie est retirée quand Kanvas dépose `buildbattle-fermee-<id>` sur le relais
  (vérifié toutes les 5 s ; dépôt fait par KV_BuildBattle au lancement, étape 2) ou après 30 min
  (`expiration-liste-minutes`).
- `relay-token` : vide dans le dépôt, à recopier à la main dans `plugins/KG_BuildBattle/config.yml` du serveur (même
  valeur que `bingo.relay-token` de KG_Bingo).

Limites connues : le nombre de joueurs dans les files publiques n'est pas affiché sur kal-games (il n'est connu que de
Kanvas) ; KalGames garde son emplacement « Build Battle » sans moteur (à retirer en retouchant KalGames).

**Déployé sur kal-games le 27/09/2026 (13:55) avec KV_BuildBattle 0.2.0 (Kanvas). Statut : testé et validé par LeKiwi06 le 28/09/2026 (envoi en file publique confirmé le 27/09 ; « parfait, tu peux tout valider »).**

## 0.2.0 - nombre de joueurs sur le bouton du Build Battle (28/09/2026)

Demande de LeKiwi06 : afficher sur chaque bouton de jeu combien de joueurs y sont (le vrai nombre, par le relais).
- `RelayCounter` lit toutes les 5 s la clé `compteur-buildbattle` (publiée par KV_BuildBattle 0.3.6) ; le bouton
  « Build Battle » affiche « | n en jeu » (KG_Menu 1.1.0). Sans nouvelle valeur depuis 30 s : rien d'affiché.

**Déploiement** : avec KV_BuildBattle 0.3.6 (Kanvas), KG_Menu 1.1.0 et KLM_Menu 2.4.0. **Statut : déployé le 28/09/2026 à 3 h 40 (ancien jar dans `_removed-…`), non testé en jeu.**

## 0.3.0 - nombre de joueurs possibles au survol du Build Battle (06/10/2026, Maxster33)

**Demande de Maxster33** : afficher le nombre de joueurs possibles au survol de chaque jeu du menu de Kal-Games.

- Ligne « Joueurs : 2 à 32 » sous le texte du bouton : 2 (deux équipes d'un joueur, minimum de KV_BuildBattle pour
  lancer) à `equipes-max` x 4 (8 équipes de 4 par défaut). Texte : `menu.bouton-joueurs`.
- Indépendant de KalGames 1.23.0 et de KV_BuildBattle.

**Déployé sur Kal-Games le 06/10/2026 à 13:55 (Maxster33 ; 0.2.0 dans `_removed-kg_buildbattle-0.2.0/`), actif après redémarrage. Statut : non testé en jeu.**

## 0.4.0 - objet « Rejouer » au retour de Kanvas (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « pour tous les mini-jeux : faire un bouton à la fin de la partie pour relancer une partie
avec les mêmes paramètres » (objet proposé 30 s ; partie privée : le premier qui clique la recrée et en devient l'hôte).

- Nouveau `Rejouer` : à l'arrivée d'un joueur sur kal-games, la clé `buildbattle-rejouer-<uuid>` est lue sur le relais
  (1 s après l'arrivée). Elle est déposée par **KV_BuildBattle 0.4.0** à l'annonce des résultats, avec les réglages de
  la partie. Le joueur reçoit alors l'objet **« Rejouer »** de KalGames (30 s, case 7 de la barre du hub).
- **File publique** : clic = retour dans la file publique de la même taille d'équipes.
- **Partie privée** : le premier qui clique recrée la partie (même tempo, même taille d'équipes, même mode « thèmes
  écrits ») et part sur Kanvas ; l'objet des autres devient **« Rejoindre la partie de X »** tant que la partie est en
  salle d'attente.
- **Dépend désormais de KalGames** (`depend` de `plugin.yml`, `build.sh`) : l'objet, son délai et son verrouillage sont
  ceux de `KalGames.replay()` (1.24.0).
- Textes : `rejouer.creee`, `rejouer.complete`, `rejouer.pas-de-spectateur`.

Limite : un joueur qui quitte Kanvas pendant l'annonce des résultats et revient sur kal-games dans les 2 minutes reçoit
aussi l'objet.

**À déployer avec KalGames 1.24.0 et KV_BuildBattle 0.4.0.** **Compilé le 09/10/2026. Statut : non testé en jeu.**

## 0.5.0 - groupes de jeu de KLM_Contacts : le groupe suit son chef au Build Battle (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « code l'étape 3 sans attendre le test » (étape 3 du cahier de KLM_Contacts : le groupe de jeu
entre ensemble dans une partie, « Inviter dans ma partie », le groupe suit son chef quand il clique sur « Rejouer »).
Empilé sur la 0.4.0 non testée : accord explicite.

- Nouveau `LienContacts` (chargé seulement si KLM_Contacts est installé : `softdepend`) : le Build Battle se déclare à
  KLM_Contacts (`JeuDeGroupe`, identifiant `buildbattle`).
- Chaque joueur envoyé sur Kanvas est annoncé : `public:<taille des équipes>` (file publique) ou `code:<code>` (partie
  privée), « Rejouer » compris. Si c'est le chef d'un groupe, ses membres sont envoyés dans la même file ou la même
  partie, depuis kal-games.
- Partie privée disparue ou complète : message au membre, il reste sur kal-games.

Limite : en file publique, les membres rejoignent la file de la même taille d'équipes ; rien ne garantit qu'ils soient
dans la même équipe que leur chef (les équipes se forment sur Kanvas).

**À déployer après KLM_Contacts 1.2.0 et KaliumRelay 1.8.0.** **Déployé sur Kal-Games le 09/10/2026 à 06:50 (LeKiwi06 ; 0.4.0 dans `_removed-kg_buildbattle-0.4.0/`), actif après redémarrage. Statut : non testé en jeu.**
