# JOURNAL — KG_Pong

Pong de Kal-Games : un plugin par jeu (règle 2.2 de `REGLES.md`), comme KG_HideAndSeek et KG_PvpKit.
Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.2 en service) et KG_ScoreBoards. Aucun changement dans
KalGames : le jeu n'utilise que les crochets existants du moteur de parties.

## 0.1.0 — prototype (06/10/2026)

**Statut : compilé, non déployé, non testé en jeu.** C'est un prototype : son but est de juger en jeu les commandes,
le rendu de la balle et la vue de dessus, en Java et en Bedrock, avant d'aller plus loin.

**Demande de LeKiwi06 (06/10/2026)**, au fil de la discussion : « un jeu d'arcade comme pong » ; « et si la balle
était un sulfur cube avec un bloc de glace dedans pour être glissant, c'est une entité, elle peut prendre de la vitesse
de façon fluide » ; « on verra la map de dessus, chacun contrôlant une barre en blocs solides [...] ayant une butée
contre des murs ; après le jeu s'occupe de la vélocité du sulfur cube en fonction de son déplacement et son point
d'impact ; la balle passe derrière la barre adverse, on marque un point ; le premier à 5 points gagne » ; commandes :
« clic gauche : changer entre monter et descendre, clic droit : activer l'action ».

### Règles codées

- **Partie** : 2 joueurs, publique ou privée. Le premier à 5 points gagne (`points-to-win`). Compte à rebours de 3 s
  (les raquettes bougent déjà), puis service.
- **Vue** : chaque joueur flotte (vol) à son point de vue, au-dessus du terrain, regard à la verticale vers le bas ;
  il ne peut pas se déplacer, seulement tourner la tête.
- **Raquette** : barre de vrais blocs, 5 de long (`bar-length`), posée à 2 cases du fond (`bar-offset`), béton bleu
  pour le joueur 1 et rouge pour le joueur 2 (`bar-block-1`, `bar-block-2`). Elle bute contre les murs.
- **Commandes** : clic gauche = inverser le sens (monter / descendre) ; clic droit = avancer d'une case ; clic droit
  maintenu = en continu à 10 blocs/s (`bar-speed`) à partir du 3e clic répété par le jeu (0,4 s). L'objet tenu en main
  montre le sens (teinture verte « Sens : monter », rouge « Sens : descendre »), rappelé au-dessus de la barre
  d'objets avec le score. « Monter » = vers le haut de l'écran du joueur, d'après la direction regardée quand le
  modérateur a posé le point de vue.
- **Balle** : un Sulfur Cube adulte qui a avalé un bloc de glace compactée (`ball-block`). Vitesse de départ 8 blocs/s
  (`ball-speed`), +0,5 bloc/s à chaque renvoi (`ball-speed-gain`, en dixièmes), 18 blocs/s au plus (`ball-speed-max`).
- **Renvoi** : par la face avant de la raquette. Au centre la balle repart droit, au bord à 55° ; une raquette qui a
  bougé dans les 4 derniers ticks ajoute 15° dans son sens ; 65° au plus. Touchée par le bout de la raquette, la
  balle est poussée de côté et le point n'est pas sauvé.
- **Murs** du haut et du bas : rebond simple.
- **Point** : la balle est entièrement passée derrière la raquette adverse. Elle revient au centre, attend 2 s
  (`serve-delay-seconds`), puis part vers celui qui vient d'encaisser (premier service au hasard), à ± 25°.
- **Abandon** : un joueur qui quitte ou se déconnecte en cours de partie perd, l'autre gagne.
- **Points de classement** (crédités à la fin, règle commune : ses points + la moyenne de ceux des joueurs classés en
  dessous) : 1 par but marqué (`points-goal`), +3 au vainqueur (`points-win`, pas en cas d'abandon adverse). Gagnant
  d'un 5-3 : 8 + 3 = 11 crédités ; perdant : 3.

### Mise en place sur le serveur (modérateur)

1. Construire un terrain plat entouré de murs : 9 x 3 cases au moins à l'intérieur (conseillé : environ 31 x 17), murs
   de 2 blocs de haut au moins. Rien d'autre à construire : les raquettes et la balle sont posées par le plugin.
2. Rien à créer à la main : au démarrage, KalGames crée le mini-jeu `pong`. Informations > Paramètres > **Pong** >
   « Arènes / maps » : créer l'arène, puis « Zone de l'arène (capture) ».
3. Poser les 5 points :
   - **Tribune** ;
   - **Terrain : coin 1** et **coin 2** : debout sur le sol du terrain, dans deux coins opposés, à l'intérieur des
     murs. La longueur du terrain est son plus grand côté ; la raquette du joueur 1 est du côté du coin 1 ;
   - **Vue du joueur 1** et **Vue du joueur 2** : en volant au-dessus du terrain (une vingtaine de blocs pour un
     terrain de 31 x 17), en regardant dans la direction qui doit être le haut de l'écran. Pour que « monter » fasse
     monter la raquette à l'écran, regarder le long des raquettes (le petit côté du terrain), pas vers l'adversaire.

### Technique

- `KGPong` : type `PONG`, écouteurs (clics, vol, protection de la balle). `PongInstance` : la partie. Textes :
  `lang.yml` de KalGames, clés `pong.*`.
- **La trajectoire est calculée par le plugin**, pas par la physique de Minecraft : à chaque tick, il avance la balle
  par pas de 0,2 bloc au plus (murs, raquettes, buts), puis place le cube à cet endroit. Le cube est sans intelligence
  ni gravité (`setAI(false)`), invulnérable, sans bousculade. Raison : un Pong demande des rebonds exacts et une
  vitesse constante, alors que la glissade du jeu ralentit et que son comportement contre des blocs qui bougent n'a
  pas pu être essayé. **Conséquence : le bloc de glace ne sert qu'à l'apparence**, il ne rend pas la balle plus
  glissante.
- **Bloc avalé** : posé dans l'emplacement d'équipement « corps » du cube (`EquipmentSlot.BODY`), seule façon de
  faire en Paper 26.2.
- **Clic gauche** : il arrive par deux évènements (mouvement de bras et interaction) ; un seul compte (2 ticks).
  **Clic droit maintenu** : le jeu renvoie le clic toutes les 4 ticks ; la raquette continue 5 ticks après le dernier
  clic reçu, donc s'arrête au plus un quart de seconde après le relâchement. Les 9 cases de la barre d'objets portent
  le même objet : un clic droit dans le vide n'est envoyé que si le joueur tient un objet.
- **Raquettes** : le contenu d'origine de chaque case est gardé et remis quand la raquette la quitte (et par le
  moteur en fin de partie).
- **Journal des parties** (KG_ScoreBoards) : évènement « points » par joueur et évènement « match » (gagnant, abandon,
  nombre de renvois, durée en ticks, buts, points, crédité).

### Choix techniques à signaler (non demandés mot pour mot)

- Trajectoire calculée par le plugin plutôt que par la glissade du Sulfur Cube (voir ci-dessus).
- Glace compactée plutôt que glace simple : c'est elle (avec la glace bleue) que le jeu range parmi les blocs
  « glissants rapides » ; réglable.
- Clic droit : une case par clic, continu seulement à partir du 3e clic répété (sinon un double clic ferait avancer
  de 4 cases).
- Les joueurs volent à leur point de vue (pas de plateforme à construire) et y sont immobilisés.
- Angles de renvoi, accélération, service vers celui qui a encaissé, abandon = défaite, barème des points.
- Partie privée limitée à 2 joueurs ; les autres regardent en mode spectateur.

### À tester en jeu (rien n'a pu l'être)

- **Balle** : le cube apparaît avec son bloc de glace ; déplacement fluide ; pas de décalage visible aux rebonds à
  grande vitesse (la position n'est envoyée aux joueurs que toutes les quelques ticks).
- **Commandes Java** : un clic droit ne change pas le sens ; le maintien donne un mouvement continu ; l'arrêt au
  relâchement est acceptable.
- **Bedrock** (clavier, manette, tactile) : clic gauche et clic droit dans le vide reconnus, maintien du clic droit,
  vol immobile, cube et bloc visibles.
- **Vue** : hauteur confortable, sens « monter » conforme à l'écran pour les deux joueurs.
- Rythme des points (repère de `EQUILIBRAGE_POINTS.md` : environ 135 pour 30 minutes).
