# JOURNAL — KG_PiliersFortune

« Les piliers de la Fortune » de Kal-Games : un plugin par jeu (règle 2.2 de `REGLES.md`), comme KG_Pong et
KG_HideAndSeek. Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.2 en service) et KG_ScoreBoards.
**Aucun changement dans KalGames, KG_Menu ni KG_ScoreBoards** : le jeu n'utilise que les crochets existants du moteur de
parties ; son bouton dans le menu de Kal-Games et ses classements (général, du mois, de la semaine) sont créés tout
seuls, comme pour les autres jeux.

## 0.1.0 — première version (06/10/2026)

**Statut : compilé, non déployé, non testé en jeu.** Nouveau plugin : aucun ancien jar à ranger.

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

### Règles codées

- **Partie** : 4 à 8 joueurs (`min-players`, `max-players`, limité au nombre de piliers), publique ou privée. Attente
  dans la zone d'attente (point « Zone d'attente » = tribune du moteur) ; partie publique lancée 20 s après le minimum
  atteint (`public-gather-seconds`, 5 s si complète).
- **Départ** : chaque joueur sur un pilier tiré au hasard ; vie, faim et saturation pleines, expérience à 0, inventaire
  vide, effets retirés, **mode survie**. Immobile (il peut tourner la tête) pendant le décompte de 5 s
  (`countdown-seconds`), chiffre au milieu de l'écran, puis « C'est parti ! ».
- **Barre du bas** (chaque seconde) : « Temps restant : 9:41 | Joueurs en vie : 4/8 » (« /8 » = joueurs au départ).
- **Objets** : toutes les 5 s (`item-interval-seconds`), premier objet 5 s après le départ, un objet tiré au hasard
  **pour chaque joueur** (un exemplaire) ; inventaire plein : l'objet tombe à ses pieds. Voir « Objets » plus bas.
- **Élimination** : sous la couche -64 (`void-y`), à la mort (coups, créatures, lave...), ou au départ du joueur
  (déconnexion, `/hub`). L'éliminé passe en **mode spectateur** au-dessus de son pilier, jusqu'à la fin.
- **Fin** : plus qu'un joueur en vie (il gagne) ; plus personne (pas de gagnant) ; ou 10 min écoulées (`duration-seconds`,
  égalité des survivants). Classement aux points dans le tchat, puis retour au hub après 8 s (`end-delay-seconds`).
- **Points** : +1 par minute complète en vie (`points-minute`, la 10e minute comprise) ; +5 par joueur éliminé
  (`points-kill`). En fin de partie, le total est multiplié : x(rang d'élimination) pour les éliminés, x3 pour le
  gagnant seul (`winner-multiplier`), x1 pour les survivants à égalité ; **puis divisé par 3** (`points-divisor`,
  arrondi au centième ; affiché « 12 pts (12 x3 / 3) »). Crédité au classement du jeu (`piliers-fortune`) ; tous les
  joueurs du départ sont crédités, même déconnectés.
- **Qui a éliminé qui** (+5) : le tueur direct, sinon le joueur crédité du dernier « coup » reçu dans les 10 s
  (`kill-credit-seconds`) avant l'élimination (chute, mort ou départ). Un « coup » :
  - un coup au corps à corps ou un projectile (flèche, boule de neige, œuf, charge de vent...) ;
  - une **explosion** : TNT (le joueur qui a **posé le bloc de TNT**, quel que soit celui qui l'a allumée, réactions
    en chaîne comprises), **cristal de l'End** et wagonnet de TNT (celui qui les a posés), lit et ancre de
    réapparition qui explosent (celui qui les a posés) ;
  - une **brûlure** par un feu (briquet, boule de feu, ou propagation d'un feu posé) ou par une **lave** posée au seau
    (et la lave qui en coule) : le poseur de la source ; il reste crédité tant que le joueur continue de brûler ;
  - un passage dans l'**eau** posée au seau par un autre joueur (et l'eau qui en coule) : le poseur de la source,
    vérifié tous les 2 ticks tant que le joueur est dans l'eau ; s'il tombe dans les 10 s, l'élimination est pour lui.
  On ne se crédite jamais soi-même (sa propre TNT, sa propre lave...).
- **Feu et pistons débloqués** (KalGames les bloque dans tout le monde des parties) : pendant une partie de ce jeu,
  dans son arène seulement, le feu peut être allumé (briquet, boule de feu, lave), se propage, brûle les blocs et
  s'éteint ; les pistons poussent et tirent (refusés si une case touchée sort de l'arène). Chaque bloc touché est suivi
  et remis en état à la fin. La règle du monde des parties `fireSpreadRadiusAroundPlayer`, mise à 0 par KalGames, est
  remise à sa valeur normale au lancement de chaque partie (sans effet sur les autres jeux : KalGames y annule toujours
  tout feu).
- **Barrières** : un joueur de la partie ne peut pas poser de bloc **contre une barrière** (le clic sur une barrière
  ne pose rien), dans toutes les phases.
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
- Le diviseur des points est un réglage (`points-divisor`, 3) appliqué au score final, plutôt que de changer les
  barèmes de base (+1 / +5 ne se divisent pas en nombres entiers).
- Non crédités : créatures sorties d'un œuf (creeper compris), bloc retiré sous les pieds, TNT allumée par un
  distributeur, eau ou lave versée par un distributeur.

### Limites connues

- La propagation du feu dépend de la règle `fireSpreadRadiusAroundPlayer` du monde des parties, remise à sa valeur
  normale par ce plugin à chaque lancement de partie ; KalGames la remet à 0 à chacun de ses démarrages.
- Les explosions (TNT, cristal, creeper) cassent des blocs pendant la partie ; ils sont remis en état à la fin.
- Poser un bloc contre une barrière est refusé, mais pas verser de l'eau ou de la lave contre une barrière (non
  demandé).

### À tester en jeu (rien n'a pu l'être)

- Décompte, barre du bas, objets toutes les 5 s, inventaire plein (objet au sol).
- Élimination sous -64, mode spectateur au-dessus du pilier, fin à un joueur, fin au temps (égalité), barème affiché.
- Remise en état entre deux parties (blocs, eau, lave, objets au sol, créatures) et 8 copies pré-générées au démarrage.
- Œufs d'apparition (créatures qui apparaissent, coups des joueurs sur elles), joueurs Bedrock.
- Crédits : TNT posée par A et allumée par B (crédit à A), cristal de l'End, feu au briquet, lave et eau au seau
  (chute après être passé dedans), brûlure qui continue après la sortie du feu.
- Feu : il se propage, brûle les blocs, s'éteint ; tout est remis en état. Pistons qui poussent et tirent.
- Bloc posé contre une barrière refusé (Java et Bedrock).
- Rythme des points (repère de `EQUILIBRAGE_POINTS.md` : environ 135 pour 30 minutes ; après la division par 3, un
  gagnant à 8 joueurs avec 3 ou 4 éliminations fait environ 25 à 30 points en 10 minutes).
