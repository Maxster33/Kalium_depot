# JOURNAL — KG_HideAndSeek

Hide and Seek de Kal-Games : un plugin par jeu (règle 2.2 de `REGLES.md`), comme KG_PvpKit, KG_BoatRace et KG_Parkour.
Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.2 en service) et KG_ScoreBoards. Aucun changement dans
KalGames : le jeu n'utilise que les crochets existants du moteur de parties.

## 0.1.0 — première version (06/10/2026)

**Statut : déployé sur Kal-Games le 06/10/2026 à 5 h 04 (LeKiwi06), actif après redémarrage, non testé en jeu.**
Nouveau plugin : aucun ancien jar à ranger. Cahier des charges : `CAHIER_DES_CHARGES.md`.

**Demande de LeKiwi06 (06/10/2026)** : « j'aimerais créer un mini-jeu de hide and seek ; les hiders doivent se cacher,
les seekers doivent trouver les hiders ; les hiders sont des blocs présents dans le décor de la map ; les hiders peuvent
faire du soundboard avec des sons de Minecraft ; un hider qui reste statique devient solide ; les hiders émettent un son
toutes les 30 secondes automatiquement ; un hider se fait one shot par un seeker s'il lui tape dessus ; un seeker n'a que
5 cœurs, et perd un demi-cœur chaque fois qu'il se trompe ; quand il tue un hider, il récupère toute sa vie, il peut
stocker jusqu'à 3 cœurs d'absorption s'il était à 3 cœurs ou plus au moment de tuer un hider ». Le reste a été réglé
par questions et réponses le même jour (`CAHIER_DES_CHARGES.md`).

### Règles codées

- **Partie** : 3 à 16 joueurs (`min-players`, `max-players`), publique ou privée. Seekers : 1 pour 5 joueurs, arrondi
  au-dessus (`players-per-seeker`), **volontaires d'abord** (bouton « Je veux être seeker » du menu de la partie pendant
  l'attente), complétés au hasard. Cachette 30 s (`hide-seconds`), seekers dans leur salle ; recherche 5 min
  (`seek-seconds`). Tous les hiders trouvés : les seekers gagnent ; au moins un hider en vie à la fin : les hiders gagnent.
- **Hider en mouvement** (décision de LeKiwi06 pour les joueurs Bedrock : pas de modèle 3D du bloc) : **joueur normal,
  visible, qui porte son bloc sur la tête**. Pseudo masqué au-dessus de la tête et pas de bousculade (équipe `kg_hns` du
  tableau des scores, comme l'anti-collision de KG_BoatRace et KG_Parkour).
- **Hider solide** : immobile 3 s (`solid-seconds`), il **devient un vrai bloc** posé dans la copie de l'arène, dans la
  case où il se tient ; il est recentré dans la case et caché à tous les autres joueurs. Il redevient un joueur dès
  qu'il sort de sa case ou saute ; tourner la tête ne compte pas. Refusé (« Impossible de devenir solide ici ») si la
  case est déjà prise par un bloc ou par un autre hider solide, ou si le hider se tient sur une dalle ou un escalier
  (le bloc flotterait).
- **Bloc** : tiré au hasard au début, menu ouvert aussitôt ; changement possible pendant la partie, 30 s au moins entre
  deux changements (`block-change-seconds`, compté à partir du premier choix).
- **Son automatique** : villageois (`auto-sound`) toutes les 30 s de recherche (`auto-sound-seconds`), les hiders l'un
  après l'autre dans un ordre tiré au hasard, à 0,75 s d'écart (`auto-sound-gap-ticks`) ; aucun point.
- **Soundboard** : liste commune (`sounds.yml`, 12 sons livrés), 3 s entre deux sons (`sound-cooldown-seconds`). Barre
  d'objets du hider : 6 sons favoris, « Tous les sons », « Changer de bloc », « Évasion ». Les favoris se choisissent
  dans « Tous les sons » > « Mes sons favoris » et sont gardés d'une partie à l'autre (`favorites.yml`).
- **Évasion** : Vitesse V pendant 2 s, dès 5 points gagnés dans la partie (`escape-points`), puis toutes les 60 s
  (`escape-cooldown-seconds`).
- **Seeker** : 5 cœurs, main nue. Un coup sur un hider (joueur ou bloc) l'élimine. Un coup sur un bloc du décor d'un
  type de la liste de la map : un demi-cœur perdu, pris d'abord sur l'absorption ; les autres blocs ne coûtent rien.
  Hider trouvé : 5 cœurs rendus, absorption gagnée selon la vie d'avant (3 cœurs : +1 ; 4 : +2 ; 5 : +3), 3 cœurs au
  plus. À 0 cœur : 15 s dans la salle des seekers (`seeker-respawn-seconds`), puis 5 cœurs sans absorption. La vie ne
  remonte pas toute seule ; aucun autre dégât pour personne.
- **Hider éliminé** : spectateur, ou seeker après 15 s dans la salle (`eliminated-becomes-seeker` : valeur des parties
  publiques et valeur par défaut du formulaire des parties privées).
- **Déconnexion** : le joueur garde son rôle et peut revenir jusqu'à la fin de la partie (il reprend là où il était) ;
  aucun point pendant l'absence. Plus aucun seeker connecté : fin immédiate, les hiders gagnent. Plus aucun hider
  connecté en jeu : les seekers gagnent. Un départ volontaire (`/hub`) est un abandon.
- **Points** (crédités à la fin, règle commune : ses points + la moyenne de ceux des joueurs classés en dessous) :
  hider 1 point toutes les 20 s de recherche survécues (`points-survive`, `survive-interval-seconds`), +8 en vie à la
  fin (`points-survivor`) ; seeker 6 par hider (`points-find`), +5 à chaque seeker si tous sont trouvés
  (`points-all-found`) ; soundboard +0,25 par son (`points-sound-hundredths`, en centièmes), un seul son compté toutes
  les 10 s (`sound-points-seconds`), si un seeker libre est à 24 blocs au plus (`sound-points-range`).

### Mise en place sur le serveur (modérateur)

1. Rien à créer à la main : au démarrage du serveur, KalGames crée lui-même le mini-jeu `hide-and-seek` (depuis
   KalGames 1.21.0, un mini-jeu par type de jeu enregistré) et déclare ses boutons à KG_Menu. Informations >
   Paramètres > **Hide and Seek** > « Arènes / maps » : créer l'arène, puis « Zone de l'arène (capture) ». Tant
   qu'aucune arène n'est complète, le jeu n'apparaît dans la liste des jeux que pour les modérateurs.
2. Poser les 4 points de l'arène : Tribune, Départ des hiders, Salle des seekers (pièce fermée), Départ des seekers.
3. Page du mini-jeu > **Blocs des maps** > la map : « + Ajouter le bloc regardé » (en regardant un bloc de la map) ou
   « + Ajouter le bloc en main ». **Une map sans bloc n'est pas jouable** (signalé en rouge sur la page du mini-jeu).
4. Facultatif : **Sons du soundboard** (ajouter ou retirer un son).

Blocs acceptés : tout bloc d'une seule case sur lequel on bute, plein ou non (enclume, composteur, table
d'enchantement, pot décoratif...), qui existe en objet. Refusés : dalles, escaliers, barrières, portillons, murets,
vitres, barreaux, portes, lits, blocs traversables (fleurs, herbes, torches).

### Technique

- `KGHideAndSeek` : type `HIDE_AND_SEEK`, écouteurs (coups, objets, retour d'un joueur), état posé sur les joueurs.
  `HideInstance` : la partie. `HideMenus`, `HideItems`, `MapBlocks` (`blocks.yml`), `SoundBoard` (`sounds.yml`,
  `favorites.yml`). Textes : `lang.yml` de KalGames, clés `hns.*`.
- **Coup du seeker** : sur un joueur, l'attaque elle-même (toujours sans dégâts). Sur un bloc : en mode aventure le jeu
  n'envoie que le mouvement de bras ; le plugin cherche alors le bloc visé (portée du joueur, 4,5 blocs). Un clic droit
  (porte, levier) fait aussi bouger le bras : ignoré pendant 0,2 s. 0,25 s au moins entre deux coups comptés.
- **Hider solide** : le vrai bloc est posé sans mise à jour des voisins ; le contenu d'origine de la case est remis dès
  que le hider bouge (et par le moteur en fin de partie, par sécurité). Le hider est caché avec `hidePlayer` : pendant
  ce temps il disparaît aussi de la liste Tab. Son propre jeu reçoit de l'air à l'emplacement du bloc (renvoyé chaque
  seconde), sinon il serait repoussé hors du bloc.
- **Cœurs du seeker** : modificateurs d'attribut (`kg_hideandseek:seeker_hearts`, `seeker_absorption`), retirés à la fin
  de la partie et, par sécurité, à chaque arrivée d'un joueur sur le serveur.
- **Retour d'un joueur** : à sa connexion, s'il est attendu dans une partie en cours, il y rentre par
  `InstanceManager.joinInstance` puis retrouve son rôle.
- **Journal des parties** (KG_ScoreBoards) : événement « points » par joueur et événement « match » (gagnant, durée de
  recherche, rôle de départ, hiders trouvés, points, crédité).

### Choix techniques à signaler (non demandés mot pour mot)

- Pseudo masqué et pas de bousculade pour les hiders (sinon le pseudo se voit à travers les murs).
- `min-players` réglable à 2 (pour tester à deux comptes) ; la valeur par défaut reste 3.
- Portes, leviers et boutons utilisables par les joueurs de la partie (remis en état à la fin).
- Tous les joueurs qui ont commencé la partie sont classés et crédités à la fin, même déconnectés ou partis, avec les
  points gagnés jusque-là.
- Le +5 « tous trouvés » va à tous les seekers connectés à la fin, hiders devenus seekers compris.

### À tester en jeu (rien n'a pu l'être)

- **Bedrock** : bloc porté sur la tête visible (selon le bloc), clic sur un bloc reconnu comme un coup (tactile et
  manette), menus du soundboard.
- Hider solide : pas de va-et-vient solide / mobile à l'arrivée du bloc, pas de repoussement ; sortie du bloc fluide.
- Coups : pas de demi-cœur perdu en ouvrant une porte ; pas de coup compté deux fois.
- Sons livrés : les 12 identifiants jouent bien un son en 26.2.
- Rythme des points (repère de `EQUILIBRAGE_POINTS.md` : environ 135 pour 30 minutes ; estimation d'un très bon hider :
  environ 165).
