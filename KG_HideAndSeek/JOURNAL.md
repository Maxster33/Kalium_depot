# JOURNAL — KG_HideAndSeek

Hide and Seek de Kal-Games : un plugin par jeu (règle 2.2 de `REGLES.md`), comme KG_PvpKit, KG_BoatRace et KG_Parkour.
Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.2 en service) et KG_ScoreBoards. Aucun changement dans
KalGames : le jeu n'utilise que les crochets existants du moteur de parties.

## 0.3.0 — retours de test du 26/10/2026

**Statut : déployé sur Kanvas (Kal-Games) le 26/10/2026 à 15:03 (LeKiwi06), actif après redémarrage, non testé en
jeu.** 0.2.0 rangée dans `_removed-kg_hideandseek-0.2.0/`. Seul (aucun changement dans KalGames). Serveur : Kal-Games, hébergé
dans Kanvas depuis le 22/10/2026.

**Demande de LeKiwi06 (26/10/2026)** : « le composteur en hide and seek ne fonctionne pas, comme il est troué sur le
dessus on ne peut jamais se solidifier ; il faudrait aussi un item en main pour changer d'état notre block, car le
copper golem n'est pas orienté correctement ni dans la bonne posture ; on ne peut pas se solidifier sur une barrière
aussi, ce n'est pas normal ; par contre on peut se solidifier à travers des blocks comme les fleurs, les trappes, etc ;
il faut aussi que les évasions s'accumulent tous les 5 points [...] et le fait de soundboard n'est pas assez valorisé,
retire le cooldown de 10 secondes entre chaque comptage ; j'aimerais aussi qu'on voie à droite la liste des blocks
encore en vie pour aider les seekers ; et plus un seeker est proche de nous, plus on gagne de points (si aucun seeker
n'est passé proche de nous pendant 1 minute, alors ne pas gagner de points : proche = zone audible des soundboard) ».

- **Composteur** (et tout bloc creux : chaudron, entonnoir...). Cause : le hider solide était placé sur le point le
  plus haut du bloc (le bord du composteur) au centre de la case, donc au-dessus du trou ; il tombait au fond, cet
  écart de hauteur le faisait redevenir mobile, et ainsi de suite. Correction : la hauteur où il se tient ne compte que
  les parties du bloc sous son emprise (0,6 bloc de large, centrée) : dans un composteur, il se tient sur le fond.
- **Debout sur une barrière, un muret ou un portillon** : on s'y tient à 1,5 bloc de hauteur, ce qui était refusé
  (« pieds à mi-hauteur, le bloc flotterait »). Le bloc est maintenant posé dans la case juste au-dessus, donc sur le
  haut visible de la barrière (sa forme de collision dépasse d'une demi-case au-dessus de son dessin).
- **Plus de solidification à travers une fleur, une herbe, une trappe, un tapis...** : la case du hider doit être vide
  (air). Avant, ces blocs étaient remplacés par le bloc du hider (et disparaissaient). Nouveau message « Impossible de
  devenir solide ici : la case doit être vide (pas de fleur, d'herbe, de trappe, de tapis...) ». Conséquence : on ne
  devient plus solide debout sur un tapis ou une trappe fermée posée au sol.
- **Objet « Orienter le bloc »** (bâton de débogage, case 4 de la barre, donné seulement si le bloc imité peut tourner
  ou changer de posture) : clic droit, orientation suivante (nord, est, sud, ouest, puis haut et bas si le bloc le
  permet ; 8 directions pour les têtes, bannières... ; axe pour les bûches) ; **accroupi + clic droit, posture
  suivante** de la statue du golem de cuivre (debout, assis, en course, en étoile). Le choix est gardé pour les
  prochaines fois où le hider devient solide, appliqué tout de suite s'il l'est déjà ; remis à zéro quand il change de
  bloc. Le bloc porté sur la tête ne montre pas l'état choisi.
- **Évasions cumulables** : une de plus tous les `escape-points` points gagnés dans la partie (5 par défaut, survie +
  soundboard) ; le nombre d'objets « Évasion » montre la réserve. Plus de recharge de 60 s : le réglage
  `escape-cooldown-seconds` est retiré ; une évasion est seulement refusée pendant les 2 s de celle en cours (un double
  clic ne gaspille pas la réserve).
- **Soundboard** : le réglage `sound-points-seconds` (un seul son compté toutes les 10 s) est retiré : chaque son joué
  (toujours 3 s entre deux sons) avec un seeker libre à 24 blocs au plus (`sound-points-range`) rapporte 0,25 point.
- **Tableau de droite « Blocs en vie (N) »** : les blocs imités par les hiders en vie et connectés, avec leur nombre
  (« 2× Composteur »), montré à tous les joueurs de la partie pendant la cachette et la recherche, retiré à la fin. Le
  tableau est propre à chaque joueur (comme la course de bateau) ; l'équipe des hiders y est recopiée chaque seconde,
  sinon leur pseudo se verrait.
- **Points de survie selon la proximité des seekers** : « proche » = à portée des sons (`sound-range`, 30 blocs), seeker
  libre (pas dans sa salle). Chaque seconde de recherche : seeker proche, ×1 à la limite de la portée jusqu'à ×3 au
  contact (`near-bonus-percent`, 200 %) ; plus aucun seeker proche depuis 60 s (`near-window-seconds`), aucun point ;
  entre les deux, ×1. La première minute de recherche compte comme un passage. Les points restent crédités toutes les
  20 s (`points-survive` : 1 point par intervalle à ×1).
  Nouveaux réglages (valeur par défaut dans le code, absents du `config.yml` en place, modifiables dans la page du
  jeu) : `near-bonus-percent`, `near-window-seconds`.

**Complément demandé par LeKiwi06 le même jour** : « la map de hide and seek est constituée d'un bâtiment central et d'un
décor autour, peux-tu faire en sorte que si on est 4 ou moins, on ne puisse pas sortir du bâtiment ? [...] il faut
déplacer le spawnpoint des seekers, car ils spawn en extérieur (il faudrait qu'ils aient le même spawnpoint que les
hiders dans cette configuration) ; j'aimerais aussi que plus un bloc est rare dans la map, plus il rapporte de points,
pour valoriser le fait de l'utiliser ».

- **Petite partie enfermée dans le bâtiment** : deux nouveaux points d'arène facultatifs, « Bâtiment : coin 1 » (un
  coin, au sol) et « Bâtiment : coin 2 » (le coin opposé, au niveau du toit). Avec `building-max-players` joueurs ou
  moins au lancement (4 par défaut, 0 = jamais) et les deux coins posés : hiders et seekers libres qui sortent de la
  zone entre les deux coins sont ramenés à leur dernière position dedans (« Petite partie : restez dans le bâtiment »),
  et les seekers partent du **départ des hiders** (à la sortie de leur salle et à chaque retour). La salle des seekers ne
  change pas. Annonce au lancement : « Petite partie : tout le monde reste dans le bâtiment ». Sécurité : si le départ
  des hiders n'est pas dans la zone, pas d'enfermement (avertissement dans la console). Aucun bloc à poser dans la map.
- **Blocs rares mieux payés** : au premier lancement sur une map, chaque bloc est compté dans la copie de l'arène (une
  fois par capture, gardé en mémoire jusqu'au redémarrage). Les points de survie sont multipliés par la rareté du bloc
  imité : ×1 pour le bloc de la liste le plus courant dans la map, jusqu'à ×2 pour le plus rare
  (`rarity-bonus-percent`, 100 %), sur une échelle logarithmique (un bloc présent 10 fois gagne autant sur un bloc
  présent 100 fois que celui-ci sur un bloc présent 1 000 fois). Un bloc absent de la map compte comme présent une fois.
  Se cumule avec la proximité des seekers (jusqu'à ×6 au total). Menu « Votre bloc » : le multiplicateur et le nombre
  dans la map s'affichent au survol de chaque bloc, et dans le message du choix.
  Nouveaux réglages : `building-max-players`, `rarity-bonus-percent`.

**Deuxième complément (LeKiwi06, même jour)** : « j'aimerais aussi qu'un joueur puisse se solidifier même s'il y a une
dalle haute ou une trappe haute au-dessus de lui, et il ne faut pas qu'un joueur puisse se solidifier sur un autre
joueur (on a trouvé un exploit où on pouvait escalader une énorme hauteur en s'empilant à tour de rôle l'un sur
l'autre) ».

- **Plafond bas** : avant, il fallait une case entièrement libre au-dessus du bloc (le hider solide se tient debout
  dessus). Maintenant on mesure la place libre au-dessus de l'endroit où il se tient (plafond cherché sur 3 cases, sous
  son emprise) ; s'il manque de la place pour un joueur debout (1,8 bloc), il est **rapetissé** (attribut de taille,
  modificateur `kg_hideandseek:solid_scale`) juste assez pour tenir. Exemple : sous une dalle du haut posée juste
  au-dessus de son bloc, il reste 0,5 bloc, taille ×0,25. Caché aux autres joueurs, seul lui le voit (caméra plus
  basse). Quand il redevient mobile, il est ramené à sa place d'avant puis reprend sa taille (sinon il grandirait dans
  le plafond). Refus « Pas assez de place au-dessus de vous » seulement sous 0,2 bloc de place. Remplace la limite de la
  0.2.0 (plus de hider couché sous un plafond à 2 blocs : il est rapetissé). Modificateur retiré aussi par sécurité à
  chaque nettoyage du joueur (fin de partie, arrivée sur le serveur).
- **Pas de solide sur un autre joueur** : refus « Impossible de devenir solide sur un autre joueur » si la case juste
  en dessous est le bloc d'un autre hider solide, ou si un autre joueur (caché ou non, sauf spectateur) occupe la case.
  Cause de l'escalade : B montait sur le bloc de A et devenait solide au-dessus de lui ; A, alors dans le bloc de B,
  en ressortait par le dessus et recommençait.

Choix à signaler (non demandés mot pour mot) : la distance « proche » des points de survie est la portée des sons
(30 blocs), alors que les points du soundboard gardent leur distance de 24 blocs ; la proximité ne change pas les
points du soundboard ; ×3 au contact est une valeur proposée (à régler) ; la première minute de recherche donne des
points sans seeker proche ; posture au clic droit accroupi (un seul objet) ; un hider absent n'apparaît pas dans le
tableau. Bâtiment : zone rectangulaire entre deux coins plutôt que des blocs invisibles (ils bloqueraient aussi les
grandes parties) ; le nombre de joueurs compte au lancement (pas de changement en cours de partie) ; le toit compte
comme dehors s'il est au-dessus du coin 2. Rareté : seulement les points de survie (pas le soundboard ni le +8 de fin) ;
×2 pour le plus rare est une valeur proposée.

Rythme : à ×1 en continu, le même qu'avant (3 points / min). Un hider qui reste près des seekers gagne jusqu'à 9 points
/ min ; un hider isolé ne gagne plus rien après la première minute. Le soundboard peut rapporter jusqu'à 5 points / min
(un son toutes les 3 s, seeker à 24 blocs au plus). Repère `EQUILIBRAGE_POINTS.md` : ~135 points pour 30 min.

À tester : composteur, chaudron, entonnoir (hider solide stable) ; debout sur une barrière, un muret ; refus sur une
fleur, une trappe ; statue du golem de cuivre (orientation, 4 postures, en Bedrock aussi) ; réserve d'évasions ;
tableau de droite (Java et Bedrock), pseudo des hiders toujours masqué avec ce tableau ; rythme des points ; petite
partie à 4 joueurs (poser d'abord les deux coins du bâtiment) : sortie par une porte et par le toit, seekers lâchés au
départ des hiders ; multiplicateurs des blocs dans le menu « Votre bloc » ; solide sous une dalle du haut et une trappe
du haut (taille réduite, retour à la taille normale sans rester coincé, **en Bedrock surtout** : Geyser doit appliquer
la taille à la boîte de collision du joueur) ; refus sur le bloc d'un autre hider.

## 0.2.0 — retours du premier test (07/10/2026)

**Statut : déployé sur Kal-Games le 07/10/2026 à 22 h 10 (LeKiwi06), actif après redémarrage, non testé en jeu.**
0.1.0 rangée dans `_removed-kg_hideandseek-0.1.0/`. KalGames en service : 1.23.0.

**Demande de LeKiwi06 (07/10/2026), après le premier test de la 0.1.0** : « quand les seekers attendent, ils voient la
map, il faut leur donner darkness ; désactiver la boussole de localisation, sinon les joueurs non solides sont
visibles ; améliorer la portée des sons, il faut qu'on les entende sur une trentaine de blocs ; faire en sorte qu'on
voie son propre bloc quand on est solide ; faire un seul item dans la hotbar pour le menu des soundboard ; quand on est
solide parfois on se fait pousser par son propre bloc et on redevient en mouvement, c'est très gênant ».

- **Obscurité des seekers en attente** : effet Obscurité (`darkness`) tant qu'un seeker est dans la salle des seekers
  (cachette, et retour à 0 cœur ou hider devenu seeker), retiré au moment où il est lâché.
- **Barre de localisation** (la barre du haut de l'écran qui montre la direction des joueurs) : les joueurs de la
  partie, hiders et seekers, n'y apparaissent plus (attribut `waypoint_transmit_range` ramené à 0 par un modificateur
  `kg_hideandseek:no_locator_bar`, retiré en fin de partie et à chaque arrivée sur le serveur).
- **Portée des sons** : nouveau réglage `sound-range` (« Portée des sons (blocs) », 30 par défaut, 16 à 96), pour le son
  automatique et le soundboard. La distance qui fait gagner des points au soundboard reste à part
  (`sound-points-range`, 24).
- **Hider solide : debout sur son bloc**. Cause des deux derniers points : en 0.1.0 le hider restait DANS son bloc, et
  son propre jeu recevait de l'air à cet endroit pour ne pas être repoussé ; il ne voyait donc pas son bloc, et quand
  l'air arrivait trop tard son jeu le repoussait hors du bloc (il redevenait mobile). En 0.2.0 le hider est placé
  **debout sur son bloc**, au centre, à la hauteur réelle du dessus du bloc : il le voit sous ses pieds, et il n'y a
  plus d'air envoyé ni de repoussement. Toujours caché à tous les autres joueurs. Il redevient un joueur dès qu'il
  quitte le dessus du bloc ou saute (il retombe alors au sol, le bloc disparaît).
  **Limites** : il faut une case libre au-dessus du bloc, sinon « Pas assez de place au-dessus de vous pour devenir
  solide » (on ne peut plus devenir solide sous un plafond à 1 bloc et demi, par exemple sous une dalle haute) ; sous
  un plafond à 2 blocs le hider se retrouve couché sur son bloc (comportement du jeu), sans autre effet.
- **Barre d'objets du hider** : 3 objets au lieu de 9 (cases 1 à 3) : « Soundboard » (ouvre le menu des sons),
  « Changer de bloc », « Évasion ». Les 6 sons favoris et le menu « Mes sons favoris » sont retirés ; le fichier
  `favorites.yml` de la 0.1.0 n'est plus lu (laissé en place sur le serveur).

Choix à signaler : l'obscurité s'applique aussi pendant les 15 s de retour à 0 cœur (la demande parlait des seekers
« qui attendent ») ; la barre de localisation est coupée pour les seekers aussi (les hiders ne les y voient plus).

À tester : obscurité suffisante dans la salle (sinon ajouter la cécité) ; hider solide sous un plafond à 2 blocs ;
blocs plus bas qu'une case (table d'enchantement, lanterne) ; barre de localisation en Bedrock.

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

**Map construite sur Kanvas** (cas de la première map, 06/10/2026) : la capture ne se fait que sur Kal-Games. Sur
Kanvas : `//wea`, `//pos1`, `//pos2`, `//size` pour contrôler, `//copy`, `//schem save <nom> -f` ; copier le fichier
de `plugins/FastAsyncWorldEdit/schematics/` (Kanvas) vers `plugins/WorldEdit/schematics/` (Kal-Games) ; sur
Kal-Games : `//schem load <nom>`, `//paste -a -s`, puis la capture. Les feuilles posées avec WorldEdit ne sont pas
persistantes et tombent hors de Kanvas (ticks aléatoires à 0 là-bas) : `//replace cherry_leaves
cherry_leaves[persistent=true]` avant la capture. Dans les parties, KalGames empêche les feuilles de tomber.

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
