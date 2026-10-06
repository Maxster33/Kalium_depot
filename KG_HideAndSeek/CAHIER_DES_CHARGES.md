# KG_HideAndSeek — cahier des charges (demande de LeKiwi06, 06/10/2026)

Publié dans le dépôt au déploiement de KG_HideAndSeek 0.1.0 (06/10/2026). **Validé par LeKiwi06 le 06/10/2026** :
réponses intégrées, partie 4 validée en bloc, partie 5 tranchée. Reste à confirmer : les conséquences de la partie 5
marquées « À CONFIRMER ». Ce qui est codé, version par version : `JOURNAL.md`.

Plugin `KG_HideAndSeek`, jeu affiché « Hide and Seek », serveur `kal-games` (7001), branché sur le moteur de parties
de KalGames comme KG_PvpKit (dépendances : KalGames, KG_ScoreBoards).

## 1. Demande d'origine (recopiée telle quelle, 06/10/2026)

> j'aimerai créer un mini-jeux de hide ans seek
>
> les hiders doivent se cacher  les seekers doivent trouver les hiders
> les hiders sont des blocks présents dans le décor de la maps
> les hiders  peuvent faire de soundboard avec des sons de minecraft
> un hider qui reste statique deviens solide
> les hider emettent un son toutes les 30 secondes automatiquement
> un hider se fait one shot par un seeker si il lui tape dessus
> un seeker n'a que 5 coeurs ,et perd un demi coeur chaque fois qu'il se trompe , quand il tue un hider , il
> récupère toute sa vie , il peut stocker jusqu'a 3 coeurs d'absorbtion si il était a 3 coeurs ou plus au moment de
> tuer un hider
>
> au début

(Le message s'arrêtait à « au début » ; le début de partie a été réglé par les réponses ci-dessous.)

## 2. Règles (demande + réponses de LeKiwi06 du 06/10/2026)

### Partie
1. **3 à 16 joueurs**, parties **publiques et privées** comme les autres jeux.
2. **Seekers au départ : 1 pour 5 joueurs** (proportion réglable).
3. **Choix des seekers : volontaires d'abord**, complétés au hasard.
4. **Temps de cachette : 30 secondes.** Pendant ce temps les seekers sont **enfermés dans une salle** de la map.
5. **Recherche : 5 minutes.**
6. **Fin de partie** : tous les hiders éliminés avant la fin du chrono = les seekers gagnent ; au moins un hider en
   vie à la fin = les hiders survivants gagnent.

### Hiders
7. Un hider a l'apparence d'un **bloc du décor**. Liste des blocs possibles **réglée par un modérateur pour chaque
   map**.
8. Le hider **choisit son bloc dans un menu** et peut **en changer pendant la partie**, **30 secondes** au moins
   entre deux changements.
9. Immobile pendant **3 secondes**, le hider **devient solide** ; il redevient mobile dès qu'il se déplace.
10. Un coup d'un seeker = **élimination immédiate**.
11. **Hider éliminé : réglage de la partie** (spectateur ou devient seeker). **Par défaut : spectateur.**
12. **Son automatique** : bruit de **villageois**, toutes les **30 secondes**. Les hiders ne sonnent pas tous en même
    temps : **l'un après l'autre, à 0,75 seconde d'intervalle**.
13. **Soundboard** : liste de sons commune, **réglable par un modérateur** (livrée avec une douzaine de sons : chat,
    cochon, poule, villageois, creeper, enderman, ghast, cloche, rot, porte, feu d'artifice, enclume).
    **3 secondes** au moins entre deux sons. Accès : **6 sons favoris dans la barre d'objets** + **un objet qui
    ouvre le menu complet**.
14. **Objet d'évasion** : **Vitesse V pendant 2 secondes**. Obtenu après **5 points** gagnés dans la partie, puis
    réutilisable toutes les **60 secondes**.
15. Un hider ne peut ni frapper ni pousser.
16. Les hiders **ne se reconnaissent pas entre eux** (ils se voient comme des blocs, comme les seekers les voient).

### Seekers
17. **5 cœurs**, **main nue**.
18. **Erreur** = frapper un bloc du décor **d'un type de la liste de la map** : **un demi-cœur perdu**. Les autres
    blocs ne coûtent rien.
19. **Aucun autre dégât** (chute, feu, noyade, faim), pour personne. La vie ne se régénère pas toute seule.
20. Hider éliminé : le seeker **récupère ses 5 cœurs**. S'il avait **3 cœurs ou plus** : absorption gagnée **selon la
    vie restante** (3 cœurs : +1 ; 4 cœurs : +2 ; 5 cœurs : +3), **3 cœurs d'absorption au plus** en réserve.
21. **Seeker à 0 cœur** : **15 secondes** dans la salle des seekers, puis il repart avec 5 cœurs, sans absorption.

### Points (toutes les valeurs réglables)
22. **Hider** : **1 point toutes les 20 secondes survécues** ; **+8** s'il est encore en vie à la fin.
23. **Seeker** : **6 points par hider éliminé** ; **+5 à chaque seeker** si tous les hiders sont trouvés avant la fin.
24. **Soundboard** : **+0,25 point** par son, **sans plafond**, mais **un seul son compté toutes les 10 secondes**, et
    seulement si **un seeker est à moins de 24 blocs**.

### Map
25. Première map : **déjà construite** par LeKiwi06 (à enregistrer comme arène, points à poser).

## 3. Réglages prévus (menu admin du mini-jeu)

Temps de cachette, durée de recherche, proportion de seekers, délai de solidité, délai entre deux changements de
bloc, délai du soundboard, intervalle du son automatique et écart entre deux hiders, son automatique, seuil et
recharge de l'évasion, délai de retour d'un seeker, valeur par défaut « hider éliminé », toutes les valeurs du
barème, distance d'écoute du soundboard. Par map : liste des blocs. Commun : liste des sons du soundboard.

Points d'arène : départ des hiders, salle des seekers, départ des seekers (à la sortie de la salle), tribune.

## 4. Choix d'interprétation de Claude (validés en bloc par LeKiwi06 le 06/10/2026)

1. **Nombre de seekers** : arrondi au-dessus. 3 à 5 joueurs : 1 ; 6 à 10 : 2 ; 11 à 15 : 3 ; 16 : 4.
2. **Volontaires** : bouton « Je veux être seeker » pendant l'attente. Plus de volontaires que de places : tirage
   parmi eux. Pas assez : complété au hasard parmi les autres.
3. **Premier bloc** : le menu s'ouvre au début de la cachette ; sans choix, un bloc de la liste est tiré au hasard
   (le hider peut le changer tout de suite, le délai de 30 s ne commence qu'après son premier choix).
4. **Son automatique** : première salve 30 s après le lâcher des seekers ; ordre des hiders tiré au hasard à chaque
   salve ; un hider solide sonne aussi ; ce son ne rapporte aucun point.
5. **Points de survie** : comptés seulement pendant les 5 minutes de recherche (pas pendant la cachette), soit 15
   points au plus, + 8 de survie.
6. **Évasion** : le seuil de 5 points compte tous les points du hider dans la partie (survie + soundboard) : obtenue
   au plus tôt vers 1 min 20, au plus tard à 1 min 40.
7. **Absorption** : la vie regardée est celle d'avant le soin. L'absorption gagnée s'ajoute à celle déjà en réserve,
   dans la limite de 3 cœurs. Une erreur retire d'abord l'absorption, puis les cœurs.
8. **Hider devenu seeker** (si le réglage est activé) : même délai de 15 s dans la salle, puis 5 cœurs ; il garde
   ses points de hider et en gagne ensuite comme seeker.
9. **Barre d'objets du hider** : 6 sons favoris, menu des sons, changer de bloc, évasion (9 cases).
10. **Classement de fin de partie** : règle commune de KG_ScoreBoards (ses points + la moyenne des points des joueurs
    classés en dessous).
11. **Rythme** : un très bon hider (survie complète, soundboard à fond) fait ~30 points en 5 min 30, soit ~165 pour
    30 minutes, au-dessus du repère de ~135 (`EQUILIBRAGE_POINTS.md`). À mesurer sur de vraies parties.

## 5. Points tranchés par LeKiwi06 le 06/10/2026

1. **Déconnexion** : le joueur peut **revenir jusqu'à la fin de la partie** ; **aucun point gagné pendant
   l'absence**.
2. **Plus aucun seeker connecté** : **fin de partie immédiate, les hiders gagnent**.
3. **Blocs acceptés dans la liste d'une map** (précisé par LeKiwi06 le 06/10/2026 : « certains blocs décoratifs
   comme les enclumes, composteur, table d'enchantement, pot décoratif ne sont pas des blocs pleins, mais devraient
   être utilisables ») : **tout bloc d'une seule case sur lequel on bute**, plein ou non (enclume, composteur, table
   d'enchantement, pot décoratif, chaudron, lanterne...). **Refusés** : dalles, escaliers, barrières / murets /
   vitres (ils se raccordent aux voisins), blocs de deux cases (portes, lits, plantes hautes) et blocs traversables
   (fleurs, herbes, torches). La liste exacte des familles refusées est une lecture de Claude : **À CONFIRMER**.
   Le coup du seeker est jugé sur la **forme réelle** du bloc (on peut rater une enclume en visant le vide de sa
   case).

Conséquences déduites par Claude (**À CONFIRMER**) :

- a. Le point 2 passe avant le point 1 : si le dernier seeker se déconnecte, la partie se termine tout de suite, il
  ne peut plus revenir.
- b. **Hider absent** : invisible et intouchable, pas de point de survie, pas de son automatique ; il revient à
  l'endroit où il était. Absent à la fin du chrono : pas de bonus de survie.
- c. **Tous les hiders présents éliminés, il ne reste que des absents** : les seekers gagnent (sinon une
  déconnexion empêcherait la victoire des seekers).

## 6. Notes techniques (à confirmer au moment du code)

- **Hider en mouvement** (décision de LeKiwi06, 06/10/2026, pour les joueurs Bedrock) : **joueur normal, visible,
  qui porte son bloc sur la tête**. Pas de modèle 3D du bloc (`BlockDisplay`), que les joueurs Bedrock ne verraient
  pas. Conséquence : la règle 16 ne vaut plus que pour les hiders solides. Le bloc doit exister en objet (pour être
  porté sur la tête). Pseudo au-dessus de la tête masqué et pas de bousculade : par une équipe du tableau des scores.
- **Hider solide** : **vrai bloc posé dans la copie de l'arène** (aligné sur la grille), retiré dès que le hider
  bouge ; le hider est alors caché à tous les autres joueurs. Son propre jeu reçoit de l'air à cet endroit pour ne
  pas être repoussé hors du bloc. L'arène est remise en état par le moteur à la fin de la partie.
- **Coup du seeker** : sur un hider en mouvement, coup direct sur le joueur ; sur un bloc, reconnu par la position
  visée (clic gauche) ; la map n'est pas cassable.
- **Menus** : aucun texte de bouton ou de champ ne doit défiler (mesurer chaque libellé).
- Toute nouvelle clé de réglage a une valeur par défaut dans le code (`REGLES.md`, 3.3).
