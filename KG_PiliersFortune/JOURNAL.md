# JOURNAL — KG_PiliersFortune

« Les piliers de la Fortune » de Kal-Games : un plugin par jeu (règle 2.2 de `REGLES.md`), comme KG_Pong et
KG_HideAndSeek. Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.2 en service) et KG_ScoreBoards.
**Aucun changement dans KalGames, KG_Menu ni KG_ScoreBoards** : le jeu n'utilise que les crochets existants du moteur de
parties ; son bouton dans le menu de Kal-Games et ses classements (général, du mois, de la semaine) sont créés tout
seuls, comme pour les autres jeux.

## 0.1.0 — première version (06/10/2026)

**Statut : déployé sur Kal-Games le 06/10/2026 à 13 h 12 (Maxster33), actif après redémarrage, non testé en jeu.**
Nouveau plugin : aucun ancien jar à ranger. À déployer seul (KalGames 1.22.2, KG_ScoreBoards 1.9.0 en service).

**Demande de Maxster33 (06/10/2026)** : « Les piliers de la Fortune. De 4 à 8 joueurs, parties publiques et privées. La
map doit être pré-générée en 8 exemplaires instanciés. À la fin de chaque partie il faut remettre les blocs qui auraient
été ajoutés ou enlevés ainsi que les objets tombés au sol. Les joueurs sont d'abord téléportés dans la zone d'attente. Au
début de la partie ils apparaissent chacun sur un pilier en pierre, avec toute leur vie, la barre de nourriture pleine et
saturation pleine, exp à 0, et inventaire totalement vide. Ils sont bloqués sur leur position le temps d'un décompte de
5 secondes qui doit être affiché au milieu de l'écran. En bas de l'écran, un chronomètre doit être affiché pour informer
du temps restant, mais aussi le nombre de joueurs restants en vie, exemple : joueurs en vie : 4/8. Toutes les 5 secondes
les joueurs reçoivent un objet aléatoire (pas tous le même). Si l'inventaire du joueur est plein alors l'objet tombe au
sol. Lorsqu'un joueur tombe en dessous de la couche -64 il est éliminé et passe en mode spectateur jusqu'à la fin de la
partie. La partie dure 10 minutes maximum. +1 point à chaque minute passée en vie, +5 points par joueur que l'on tue ou
fait tomber ; en fin de partie : x1 pour le premier joueur éliminé, x2 pour le 2e, x3 pour le 3e et ainsi de suite
jusqu'à 8. S'il reste un seul joueur en vie à la fin de la partie alors il gagne, son score est multiplié par 3. S'il
reste plusieurs joueurs en vie à la fin, ils sont à égalité, leurs scores ne changent pas. Créer le plug-in, intégrer le
jeu au menu Kal-Games et créer ses classements. »

**Réponses de Maxster33 aux questions (même jour)** :
- Multiplicateurs **exactement comme écrits** : éliminés x1, x2, x3... ; gagnant seul x3 ; survivants à égalité x1 (même
  si un éliminé a un plus gros multiplicateur que le gagnant, ex. 7e éliminé x7 à 8 joueurs).
- Objets : **tous les objets de survie + les œufs d'apparition**.
- **Pas** de règle commune « points + moyenne des joueurs classés en dessous » : le barème seul.
- La map existe déjà **sur Kal-Games**.

**Demandes complémentaires de Maxster33 (même jour, avant tout déploiement ; la version reste 0.1.0)** :
« Enlève les livres enchantés de la liste d'objets. Si un joueur est expulsé par une explosion (TNT ou cristal de l'End
par exemple), c'est le joueur qui a posé l'explosif qui est le tueur. Si un joueur meurt de feu, de lave, ou tombe à
cause d'une source d'eau, c'est le joueur qui a posé la source qui est le tueur. Il faut débloquer le feu et les
pistons. Divise par 3 les points. » Puis : « Il faut rendre impossible le fait de poser un bloc contre un bloc
barrière. »

**Suite (même jour, toujours avant déploiement)** : « Pour une source d'eau on ne compte l'élimination pour le poseur que
dans les 10 secondes après avoir posé la source. Si un joueur utilise cette source ensuite pour rattraper sa chute et se
fait tirer dessus à l'arc par exemple ou se fait expulser par une explosion, on ne prend pas en compte le fait qu'il
était dans une source d'eau. La division par 3 ne doit pas se faire à la fin de la partie, et 3 n'est pas bon : change
les points de base avant multiplicateur pour arriver à environ 135 points pour 30 minutes, en moyenne ; le premier sur
une partie à 4 devrait gagner environ 55 points et le premier sur une partie à 8 environ 70 points. » Puis : « Interdis
les sources d'eau et de lave contre les blocs barrière. Mort par creeper : revient à celui qui a utilisé l'œuf. Mort par
bloc retiré sous les pieds d'un joueur : revient à celui qui a cassé le bloc. Objet envoyé par un distributeur ou un
dropper : revient à celui qui a mis l'objet dedans. »

**Barème final (même jour)** : « Pour toutes les créatures sorties d'un œuf, l'élimination revient au joueur qui a utilisé
l'œuf. » Puis, après plusieurs propositions de calcul : « 1. +0,25 toutes les 30 secondes ; 2. + nombre fixe de points
pour chaque élimination ; 3. x rang ; 4. x1,5 pour le gagnant s'il gagne avant la fin », avec **7 points par
élimination** (le même nombre quel que soit le nombre de joueurs). Les joueurs encore en vie à la fin ont aussi un rang
de mort : **le rang qui suit le dernier éliminé, partagé** (choix de Maxster33).

### Règles codées

- **Partie** : 4 à 8 joueurs (`min-players`, `max-players`, limité au nombre de piliers), publique ou privée. Attente
  dans la zone d'attente (point « Zone d'attente » = tribune du moteur) ; partie publique lancée 20 s après le minimum
  atteint (`public-gather-seconds`, 5 s si complète).
- **Départ** : chaque joueur sur un pilier tiré au hasard ; vie, faim et saturation pleines, expérience à 0, inventaire
  vide, effets retirés, **mode survie**. Immobile (il peut tourner la tête) pendant le décompte de 5 s
  (`countdown-seconds`), chiffre au milieu de l'écran, puis « C'est parti ! ».
- **Barre du bas** (chaque seconde) : « Temps restant : 9:41 | Joueurs en vie : 4/8 » (« /8 » = joueurs au départ).
- **Tableau à droite de l'écran** (demande de Maxster33, actualisé chaque seconde et à chaque élimination) : en haut, en
  **vert**, les joueurs en vie ; en dessous, en **rouge**, les éliminés ; chaque groupe classé par points, avec le
  classement de la partie (« 1. Pseudo 12,5 »). Points affichés : pendant la partie, ceux que le joueur aurait si elle
  s'arrêtait maintenant (base x rang de mort, sans le bonus du gagnant ; les joueurs en vie ont le rang qui suit le
  dernier éliminé, leurs points montent donc à chaque élimination) ; à la fin, les points définitifs. Montré à tous les
  membres de la partie (spectateurs compris) ; le tableau d'avant leur est rendu à la fin ou à leur départ.
- **Tchat** : toutes les 30 s, « +0,25 point pour chaque joueur encore en vie (n) » ; à chaque élimination, qui, comment
  et les points : « Pseudo est tombé dans le vide : éliminé par Autre (Flèche). Autre gagne +7 points. (5 en vie) ».
  Comment : « coup », nom du projectile ou de l'entité traduit par le jeu (Flèche, TNT amorcée, Cristal de l'End,
  Creeper, Zombie...), « Squelette : Flèche » pour une créature d'un œuf, « (distributeur) », « explosion », « feu »,
  « lave », « eau », « bloc cassé sous ses pieds ». Ce qui est arrivé : « est tombé dans le vide », « est mort », « a
  quitté la partie ». Sans joueur crédité : « Pseudo est tombé dans le vide. (5 en vie) ». Les +7 sont avant le
  multiplicateur de rang.
- **Objets** : toutes les 5 s (`item-interval-seconds`), premier objet 5 s après le départ, un objet tiré au hasard
  **pour chaque joueur** (un exemplaire) ; inventaire plein : l'objet tombe à ses pieds. Voir « Objets » plus bas.
- **Élimination** : sous la couche -64 (`void-y`), à la mort (coups, créatures, lave...), ou au départ du joueur
  (déconnexion, `/hub`). L'éliminé passe en **mode spectateur** au-dessus de son pilier, jusqu'à la fin.
- **Fin** : plus qu'un joueur en vie (il gagne) ; plus personne (pas de gagnant) ; ou 10 min écoulées (`duration-seconds`,
  égalité des survivants). Classement aux points dans le tchat, puis retour au hub après 8 s (`end-delay-seconds`).
- **Points** : **(0,25 point par tranche de 30 s en vie + 7 points par élimination) x rang de mort**, puis **x1,5 pour
  le gagnant s'il gagne avant la fin du temps**.
  - Temps : +0,25 toutes les 30 s passées en vie pendant la partie (`time-interval-seconds` 30,
    `points-time-hundredths` 25), soit 5 points au plus en 10 minutes.
  - Élimination : 7 points (`points-kill`), le même nombre quel que soit le nombre de joueurs.
  - Rang de mort : x1 pour le premier éliminé, x2 pour le deuxième... ; les joueurs encore en vie à la fin partagent le
    rang qui suit le dernier éliminé (gagnant seul : rang = nombre de joueurs).
  - Bonus x1,5 (`winner-multiplier-tenths` 15) : seulement si le dernier joueur en vie gagne avant les 10 minutes ; un
    joueur seul encore en vie au bout des 10 minutes a son rang, sans le bonus.
  - Affiché dans le tchat : « 63 pts (3,5 temps + 1 élim. = 10,5 x4 x1,5) ». Crédité au classement du jeu
    (`piliers-fortune`) ; tous les joueurs du départ sont crédités, même déconnectés.
- **Qui a éliminé qui** (+7) : le tueur direct, sinon le joueur crédité du dernier « coup » reçu dans les 10 s
  (`kill-credit-seconds`) avant l'élimination (chute, mort ou départ). Un « coup » :
  - un coup au corps à corps ou un projectile (flèche, boule de neige, œuf, charge de vent...) ;
  - un **bloc cassé sous ses pieds** (n'importe quelle case sous le joueur, il peut être à cheval) : celui qui l'a cassé ;
  - une **créature sortie d'un œuf d'apparition** (toutes : creeper, zombie, squelette et ses flèches...) : celui qui a
    utilisé l'œuf ;
  - un objet envoyé par un **distributeur ou un dropper** (flèche, potion jetable, boule de feu, TNT, briquet, eau ou
    lave) : celui qui a **mis l'objet dedans** (contenu comparé à l'ouverture et à la fermeture ; s'ils sont plusieurs
    pour la même sorte d'objet, le dernier ; un dropper qui remplit un distributeur transmet le joueur) ;
  - une **explosion** : TNT (le joueur qui a **posé le bloc de TNT**, quel que soit celui qui l'a allumée, réactions
    en chaîne comprises), **cristal de l'End** et wagonnet de TNT (celui qui les a posés), lit et ancre de
    réapparition qui explosent (celui qui les a posés) ;
  - une **brûlure** par un feu (briquet, boule de feu, ou propagation d'un feu posé) ou par une **lave** posée au seau
    (et la lave qui en coule) : le poseur de la source ; il reste crédité tant que le joueur continue de brûler ;
  - un passage dans l'**eau** posée au seau par un autre joueur (et l'eau qui en coule) : le poseur de la source,
    **seulement dans les 10 s qui suivent la pose de la source** (`water-credit-seconds`) ; plus tard, l'eau ne compte
    pour personne. Un vrai coup reçu ensuite (flèche, explosion, coup...) l'emporte : l'eau ne le remplace pas tant que
    ce coup compte. Vérifié tous les 2 ticks tant que le joueur est dans l'eau.
  On ne se crédite jamais soi-même (sa propre TNT, sa propre lave...).
- **Feu et pistons débloqués** (KalGames les bloque dans tout le monde des parties) : pendant une partie de ce jeu,
  dans son arène seulement, le feu peut être allumé (briquet, boule de feu, lave), se propage, brûle les blocs et
  s'éteint ; les pistons poussent et tirent (refusés si une case touchée sort de l'arène). Chaque bloc touché est suivi
  et remis en état à la fin. La règle du monde des parties `fireSpreadRadiusAroundPlayer`, mise à 0 par KalGames, est
  remise à sa valeur normale au lancement de chaque partie (sans effet sur les autres jeux : KalGames y annule toujours
  tout feu).
- **Barrières** : un joueur de la partie ne peut ni poser de bloc ni verser d'eau ou de lave (seaux de poissons
  compris) **contre une barrière**, dans toutes les phases.
- **Remise en état** (moteur de KalGames) : blocs posés, cassés, détruits par explosion, eau et lave remis à l'identique,
  objets au sol, flèches et créatures supprimés à la fin de chaque partie. **8 copies de l'arène pré-générées** au
  démarrage du serveur (`prewarm-arenas` = 8), gardées de côté et réutilisées.

### Objets

Tous les objets du jeu (dans les fonctionnalités activées du monde), chacun avec la même chance, **œufs d'apparition
compris**. Exclus : objets de commande, de structure, de test et de débogage, barrière, lumière, livre des connaissances,
bedrock, cadre de portail de l'End, générateurs (spawner, générateur d'épreuve, coffre-fort), terre labourée, chemin,
plante de chorus, frai de grenouille, dalle de chêne pétrifiée, deepslate renforcée, améthyste bourgeonnante, sable et
gravier suspects, blocs infestés (aucun ne s'obtient en survie), et le **livre enchanté** (demande de Maxster33).
Potions (normales, jetables, persistantes) et flèches à effet : effet tiré au hasard.

### Mise en place sur le serveur (modérateur)

1. Déployer le jar et redémarrer : KalGames crée lui-même le mini-jeu `piliers-fortune` et son bouton dans le menu. Tant
   qu'aucune arène n'est complète, le jeu n'apparaît dans la liste des jeux que pour les modérateurs.
2. Informations > Paramètres > **Les piliers de la Fortune** > « Arènes / maps » : créer l'arène, puis « Zone de l'arène
   (capture) » autour de la map. **La zone capturée limite la construction** : prévoir assez de vide autour et
   au-dessus des piliers (aucun bloc ne peut être posé hors de la zone). Rien en dessous des piliers : la chute doit
   mener sous la couche -64.
3. Poser les points : **Zone d'attente**, puis **Piliers** : un point par pilier, debout au sommet (8 points pour
   8 joueurs ; la direction regardée en posant le point est celle du joueur au départ).
4. Les 8 copies sont collées une à une après le démarrage (et après chaque nouvelle capture).

### Choix techniques à signaler (non demandés mot pour mot)

- **Œufs du Wither et de l'Ender Dragon exclus** (boss qui détruisent l'arène et la font durer) ; tous les autres œufs
  sont dans la liste, Warden compris.
- Les joueurs en vie peuvent **frapper les créatures** de leur partie (KalGames l'interdit partout ailleurs).
- Une **mort** (coups, créatures, lave, feu, chute) élimine aussi, comme la chute sous -64 ; un départ (déconnexion,
  `/hub`) aussi, crédité comme une chute. Les objets d'un joueur mort ne tombent pas (règle commune de KalGames).
- Pas de faim ni de dégâts pendant le décompte ; mode survie pendant la partie.
- `min-players` réglable à 2 (pour tester à deux comptes) ; la valeur par défaut reste 4.
- Explosif, feu, lave, eau : en plus de ce qui était demandé, lits, ancres de réapparition, wagonnets de TNT et feux de
  camp sont crédités à leur poseur de la même façon ; une TNT revient à celui qui l'a posée, pas à celui qui l'a
  allumée.
- Non crédités : objets mis dans un distributeur par un entonnoir sans
  joueur, distributeur ou dropper déjà rempli dans la map.

### Limites connues

- La propagation du feu dépend de la règle `fireSpreadRadiusAroundPlayer` du monde des parties, remise à sa valeur
  normale par ce plugin à chaque lancement de partie ; KalGames la remet à 0 à chacun de ses démarrages.
- Les explosions (TNT, cristal, creeper) cassent des blocs pendant la partie ; ils sont remis en état à la fin.
- Les œufs d'apparition envoyés par un distributeur ne font rien apparaître : KalGames bloque ce type d'apparition
  dans le monde des parties (non modifié).
- Distributeur : le joueur crédité est le dernier à avoir ajouté cette sorte d'objet (pas de suivi objet par objet).

### À tester en jeu (rien n'a pu l'être)

- Décompte, barre du bas, objets toutes les 5 s, inventaire plein (objet au sol).
- Élimination sous -64, mode spectateur au-dessus du pilier, fin à un joueur, fin au temps (égalité), barème affiché.
- Remise en état entre deux parties (blocs, eau, lave, objets au sol, créatures) et 8 copies pré-générées au démarrage.
- Œufs d'apparition (créatures qui apparaissent, coups des joueurs sur elles), joueurs Bedrock.
- Crédits : TNT posée par A et allumée par B (crédit à A), cristal de l'End, feu au briquet, lave et eau au seau
  (chute après être passé dedans), brûlure qui continue après la sortie du feu.
- Feu : il se propage, brûle les blocs, s'éteint ; tout est remis en état. Pistons qui poussent et tirent.
- Bloc, eau et lave contre une barrière refusés (Java et Bedrock).
- Tableau de droite (couleurs, ordre, points qui montent à chaque élimination, points définitifs à la fin, tableau
  rendu après la partie), y compris sur Bedrock ; messages du tchat (30 s, éliminations, noms traduits).
- Bloc cassé sous les pieds, créatures d'un œuf (creeper, squelette...), flèche / TNT / eau d'un distributeur rempli par un joueur ; eau posée
  il y a plus de 10 s (pas de crédit) ; flèche reçue dans l'eau d'un autre (crédit à l'archer).
- Rythme des points : relever la durée réelle des parties et les éliminations créditées dans le journal des parties
  (événements `match`) pour vérifier le calage (voir « Calage des points »).

### Calage des points

Repère : environ 135 points pour 30 minutes en moyenne (`EQUILIBRAGE_POINTS.md`). Avec le rang de mort, un gagnant de
partie à 8 a toujours un multiplicateur deux fois plus grand qu'à 4 (x12 contre x6) : viser à la fois ~55 pour le
gagnant à 4 et ~70 pour le gagnant à 8 (première demande) n'est pas possible ; Maxster33 a choisi 7 points par
élimination. Hypothèses (rien n'a été mesuré) : une partie dure environ 7 minutes, plus environ 1 min 30 entre deux
parties (3,5 parties en 30 min) ; 70 % des éliminations sont créditées à un joueur ; le gagnant en fait 1 à 4 joueurs et
2 à 8 joueurs.

| Points / élimination | Gagnant à 4 | Gagnant à 8 | Moyenne d'un joueur / 30 min |
|---|---|---|---|
| 5 | 51 | 162 | ~102 |
| **7 (retenu)** | **63** | **210** | **~128** |

Gagnant en 7 minutes, 7 points par élimination : à 4 joueurs (3,5 + 1 x 7) x 4 x 1,5 = 63 ; à 8 joueurs
(3,5 + 2 x 7) x 8 x 1,5 = 210 (avec 1 seule élimination : 126). Tout reste réglable en jeu.

## 0.1.1 - à partir de 3 joueurs (06/10/2026, Maxster33)

**Demande de Maxster33** : « pouvoir jouer à partir de 3 joueurs (3 à 8 joueurs) ».

- Valeur par défaut du réglage « Joueurs minimum » (`min-players`) : **3** au lieu de 4 (publique et privée). Au survol du
  jeu dans le menu de Kal-Games : « Joueurs : 3 à 8 ».
- Rien d'autre ne change. Attention : si « Joueurs minimum » a déjà été modifié en jeu (Paramètres du mini-jeu), la valeur
  enregistrée reste en place ; le régler à 3 dans les Paramètres a le même effet tout de suite, sans redémarrage.

**Statut : compilé, non déployé, non testé en jeu.**

## 0.2.0 - bûche, créatures des œufs, objets enchantés, inventaire récupéré, nouveau barème (06/10/2026, Maxster33)

Comprend la 0.1.1 (jamais déployée) : 3 à 8 joueurs.

**Demande de Maxster33** : « premier objet reçu doit être une bûche ; il faut que les monstres que l'on fait apparaître
n'attaquent pas le joueur qui l'a posé et que le golem de fer et le bonhomme de neige attaquent les autres joueurs
également ; les équipements et outils doivent être enchantés (random) ; lorsqu'on tue un joueur ou le fait tomber on doit
récupérer tout ce qu'il y a dans son inventaire. On va revoir le barème : à la place des points d'élimination on met
+25 % (réglable) au score actuel (temps) et tous les joueurs en vie gagnent 3 points (réglable) ; on enlève le
multiplicateur de rang, et le gagnant de la partie (s'il y en a un) remporte ses points + la moyenne des points des
autres joueurs. »

### Changements

- **Premier objet** : une bûche de chêne pour chaque joueur, 5 s après le départ ; ensuite les objets au hasard.
- **Créatures des œufs** : elles ne visent jamais le joueur qui a utilisé l'œuf (elles se tournent vers le joueur en vie
  le plus proche, à 32 blocs au plus, sinon personne) et ne lui font aucun dégât, projectiles et explosion de creeper
  compris. **Golem de fer et golem de neige** : chaque seconde, sans cible, ils prennent le joueur en vie le plus proche
  (autre que leur propriétaire) ; les boules de neige poussent et l'élimination qui suit est créditée au propriétaire.
- **Objets enchantés** : tout objet enchantable reçu (armures, armes, outils, arcs, arbalètes, tridents, cannes à
  pêche...) est enchanté comme à une table d'enchantement de niveau 5 à 30, enchantements compatibles entre eux, sans
  enchantements « trésor ». Les livres restent des livres.
- **Inventaire récupéré** : le joueur crédité d'une élimination (tué, tombé, parti) reçoit tout l'inventaire de
  l'éliminé (armure et seconde main comprises) ; ce qui ne rentre pas tombe à ses pieds. Seulement s'il est lui-même
  encore en vie et connecté (sinon l'inventaire disparaît, comme avant).
- **Barème** :
  1. +0,25 point toutes les 30 s en vie (inchangé) ;
  2. élimination : le joueur crédité gagne **+25 % de son score actuel** (`kill-bonus-percent`), calculé avant le
     point 3 ;
  3. à chaque élimination, **tous les joueurs encore en vie gagnent +3 points** (`points-alive-on-elimination`) ;
  4. **plus de multiplicateur de rang ni de x1,5** ;
  5. le **gagnant** (seul joueur encore en vie, avant ou au bout des 10 minutes) gagne son score **+ la moyenne des
     scores de tous les autres joueurs** de la partie ; égalité (plusieurs en vie) : scores inchangés.
  Réglages retirés : `points-kill`, `winner-multiplier-tenths`.
- Tableau de droite : score actuel pendant la partie, points définitifs à la fin. Tchat : « Autre gagne +25 % (4 → 5
  pts) et récupère son inventaire », puis « +3 points pour chaque joueur encore en vie (n) ».

- **Piliers espacés** (demande de Maxster33 : « les positions des piliers ont été configurées en cercle, donc le 1 est en
  face du 5 ; il faut que les joueurs soient à un écart équivalent en début de partie en fonction du nombre de
  joueurs ») : avec n joueurs, n piliers aussi espacés que possible autour du cercle, à partir d'un pilier tiré au
  hasard ; les joueurs y sont placés au hasard. Avec 8 piliers : 4 joueurs = un pilier sur deux ; 3 joueurs = écarts de
  3, 3 et 2 ; 5 joueurs = écarts de 2, 2, 2, 1, 1 (dans un ordre fixe) ; 8 joueurs = tous. **Suppose que les points
  « Piliers » sont posés dans l'ordre du cercle** (1, 2, 3... en tournant).

### Choix techniques à signaler

- La bûche est une bûche de chêne.
- Le +25 % s'applique au score avant le +3 de survie du même moment.
- Un joueur seul en vie au bout des 10 minutes est aussi le gagnant (le bonus « avant la fin » n'existe plus).

**Statut : compilé, non déployé, non testé en jeu.**
