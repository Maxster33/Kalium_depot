# JOURNAL — KG_BoatRace

Course de bateau de Kal-Games, sortie de KalGames (règle 2.2 de `REGLES.md`). Cahier des charges :
`CAHIER_DES_CHARGES.md` (même dossier). Serveur : `kal-games` (machine 7001).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.5.1 - solo : pas de tour en tête, bonus de tour propre réduit de moitié (05/10/2026, LeKiwi06)

**Demande de LeKiwi06 (05/10/2026)** : « pour le mode solo du boat race, il faut retirer le tour en tête, comme on est
forcément en tête, et réduire de moitié le bonus de tour propre en solo également, car c'est plus facile de ne pas
sortir de la piste en solo ».
- **Solo** = un seul coureur dans la course au moment où le tour se termine (un adversaire qui quitte la course la
  rend solo pour les tours suivants).
- **Tour en tête** (`lead-coef-x10`, x1,5) : plus jamais accordé en solo.
- **Tour sans hors-piste** (`points-clean-lap`, +1) : +0,5 en solo. Un tour propre solo vaut donc 1,5 point avant le
  chrono, au lieu de 2 puis x1,5.
- Inchangés : point du tour, bonus de série (+n toutes les 3 séries de tours propres), paliers de chrono, Grand Prix.
- Rétroactif (demandé ensuite par LeKiwi06 : « si, il faut que ce changement soit rétroactif ») : les 45 courses solo
  du journal recalculées tour par tour dans les classements de Kal-Games le 05/10/2026, voir `REPRISE_PROJET.md`.

Empilé sur la 1.5.0 non testée, à la demande explicite de LeKiwi06.

**Déployé sur Kal-Games le 05/10/2026 à 15:21 (LeKiwi06, serveur arrêté ; 1.5.0 dans `_removed-kg_boatrace-1.5.0/`).
Statut : non testé en jeu.**

## 1.6.0 - essai : bateau calculé par le serveur pour les joueurs Bedrock (09/10/2026, LeKiwi06)

**Demande de LeKiwi06 (08/10/2026)** : « les joueurs Bedrock ont un net avantage sur le boat race comme leur bateau ne
fonctionne pas pareil (pour avoir testé, on dirait qu'ils ne dérapent presque pas) ». Mesure faite d'abord (cahier des
charges, « Équité Java / Bedrock »), puis choix de LeKiwi06 parmi les pistes proposées : « bateau serveur en essai »,
empilé sur la 1.5.1 non testée avec son accord explicite.

**Mesure** (journal de KG_ScoreBoards, 24/09 au 08/10/2026, arène `non_alcoholic_pigeons`, 1 781 tours, tours lancés
de moins de 60 s) : meilleur tour 39,50 s en Java contre 32,90 s en Bedrock ; tour médian 45,0 s contre 39,8 s ; tous
les tronçons plus rapides en Bedrock (5 à 18 %), vitesse d'entrée aux checkpoints plus haute partout (+8 à +29 km/h,
190 contre 160 km/h en bout de ligne droite) ; 2,36 points par tour en Java contre 4,99 en Bedrock (paliers de chrono).
Réserve : un joueur a fait 949 des 1 300 tours Bedrock ; sans lui il reste 3,2 s sur le meilleur tour et 1,5 s sur le
médian. Détail dans le cahier des charges.

**Cause** : en bateau, c'est le jeu du joueur qui calcule le mouvement ; un joueur Bedrock roule avec la physique de
Bedrock, que Geyser transmet telle quelle.

**Changements** (tout est derrière le réglage, rien ne change tant qu'il est désactivé) :
- Réglage « Bedrock : bateau serveur (essai) » (`bedrock-server-boat`, **désactivé par défaut**). Activé : un joueur
  Bedrock ne pilote plus un vrai bateau. Il est assis sur un porte-armure invisible (`ServerBoats`), et le serveur
  calcule à chaque tick, à partir de ses touches (avancer, reculer, gauche, droite), le même mouvement que le jeu Java
  (frottement du sol selon les blocs, rotation, poussée, gravité, collisions avec les blocs). Un vrai bateau vide (la
  « coque ») suit cette position : c'est lui que l'on voit, et que regardent le hors-piste et l'anti-collision.
  Les joueurs Java gardent leur bateau habituel.
- Pourquoi un siège à part : un joueur passe toujours en première place d'un véhicule, donc pilote ; on ne peut pas le
  laisser dans un vrai bateau sans que son jeu le pilote.
- Réglage « Bedrock : hauteur du siège (cm) » (`bedrock-seat-height`, 19) : à ajuster si le joueur paraît trop haut ou
  trop bas dans son bateau.
- Sortie du siège interdite en course (`ServerBoatListener`), comme la sortie du bateau ; personne ne peut monter dans
  une coque.
- Position du bateau calculé envoyée aux joueurs à chaque tick (au lieu de tous les 3 ticks).
- Journal de KG_ScoreBoards : la ligne `lap` gagne `serverBoat` (vrai si le tour est fait avec un bateau calculé), pour
  mesurer l'effet.

**Vérifié hors jeu** (calcul lancé sur un faux monde) : vitesse maximale 144 km/h sur glace compacte et 262 km/h sur
glace bleue (valeurs du jeu Java), arrêt net contre un mur, glissade le long d'un mur pris en biais, chute et
atterrissage, dérapage en virage, frottement d'un bloc ordinaire.

**Limites connues** : la direction réagit avec le retard de la connexion du joueur (le jeu Java, lui, réagit tout de
suite) ; le bateau calculé ne heurte que les blocs, jamais une entité (avec l'anti-collision désactivé, deux joueurs
Java se heurtent encore, pas un joueur Bedrock) ; pas d'animation des rames ; eau simplifiée (flottaison seulement) ;
sable des âmes, miel et slime sans effet.

**À vérifier en jeu avec un joueur Bedrock (partie privée, réglage activé)** :
1. les touches arrivent bien : le bateau avance, recule et tourne ;
2. la vue du joueur tourne avec le bateau (sinon il doit tourner la caméra lui-même) ;
3. la hauteur du joueur dans la coque (réglage ci-dessus) ;
4. le retard de la direction est-il jouable ;
5. le joueur ne peut pas sortir du siège ni monter dans la coque ; retour au point de contrôle ; arrivée ;
6. hors-piste, anti-collision et classement inchangés ; un joueur Java dans la même course n'est pas touché.

**Déploiement** : seul (KalGames 1.23.0 et KG_ScoreBoards 1.9.0 en place). **Statut : compilé, non déployé, non testé
en jeu.**

## 1.6.1 - anti-collision Bedrock : tête des adversaires au-dessus de leur bateau (16/10/2026, LeKiwi06)

**Demande de LeKiwi06 (16/10/2026)** : « les joueurs bedrock voient une tête de joueur en boat race pour leurs
adversaires, mais ils collisionnent toujours dessus » ; pistes proposées : la mettre 1 ou 2 blocs plus haut, ou la
poser sur un porte-armure.

**Cause probable** : la copie vue par les joueurs Bedrock est déjà un porte-armure (invisible, « marqueur », tête +
pseudo, depuis la 1.4.0). Ses pieds étaient placés 0,5 bloc **sous** le bateau de l'adversaire : sa boîte de
collision (0,5 x 1,975 bloc) couvrait donc tout le bateau. Geyser 2.11.3 (lu dans le jar) met cette boîte à zéro pour
un porte-armure marqueur, mais d'après le retour le jeu Bedrock la heurte quand même. Non vérifié en jeu.

**Changements** :
- Le porte-armure est placé **au-dessus** du bateau de l'adversaire : pieds 1 bloc au-dessus du fond du bateau par
  défaut (1,5 bloc plus haut qu'avant). Sa boîte entière est au-dessus du bateau (haut du bateau : 0,56 bloc) ; la tête
  et le pseudo flottent environ 1,5 bloc plus haut que la vraie tête du joueur.
- Nouveau réglage « Bedrock : hauteur des têtes (cm) » (`bedrock-copy-height`, 100, de -50 à 300) : hauteur des pieds
  du porte-armure au-dessus du fond du bateau. -50 = ancienne hauteur. Valeur par défaut dans le code : rien à ajouter
  à la main sur le serveur.
- Joueurs Java inchangés (coque, tête et pseudo en entités d'affichage, sans collision).

**Limites** : si la collision venait d'autre chose que la boîte du porte-armure, la remonter ne suffira pas (à
signaler après le test). Un adversaire nettement plus bas (marche d'un bloc, pente) peut encore amener sa tête à
hauteur d'un bateau. Déplacement de la tête inchangé (téléportée à chaque passage de la boucle).

**À vérifier en jeu (au moins deux joueurs Bedrock)** : les bateaux se croisent et se doublent sans se heurter ; la
tête et le pseudo restent lisibles à cette hauteur (sinon ajuster le réglage).

**Déploiement** : empilé sur la 1.6.0 non testée (tout ce qu'elle ajoute reste derrière « Bedrock : bateau serveur
(essai) », désactivé par défaut) ; déployer la 1.6.1 met donc aussi la 1.6.0 en service. **Déployé sur Kal-Games le 16/10/2026 à 23:09 (LeKiwi06,
actif depuis le redémarrage de 23:12 fait par LeKiwi06, journal sans erreur ; 1.5.1 dans `_removed-kg_boatrace-1.5.1/` ; supprimables par l'humain :
`_removed-kg_boatrace-1.3.0/`, `1.4.0/`, `1.4.1/`, `1.4.2/`). Statut : non testé en jeu.**

## 1.6.2 - anti-collision : bateaux de course non solides côté serveur (17/10/2026, LeKiwi06)

**Retour de LeKiwi06 (17/10/2026)** sur la 1.6.1 : « apparemment ça n'a pas réglé le souci ».

**Cause trouvée** (journaux de Kal-Games du 10 au 17/10/2026 et `boatrace.jsonl` de KG_ScoreBoards, copies hors
dépôt) : ce n'est pas la tête. En bateau, le serveur rejoue chaque déplacement envoyé par le jeu du joueur, avec les
collisions entre entités. Pour le serveur, un bateau est toujours solide pour un autre bateau (lu dans le code de
Paper 26.3 : `AbstractBoat.canBeCollidedWith` renvoie toujours vrai, aucun réglage ni API ne le change), et le pilote
d'un bateau aussi (`isPushable`). Quand un coureur entre dans le bateau caché d'un adversaire, le serveur le ramène en
arrière (« moved wrongly » dans le journal). Chiffres : 101 retours en arrière pendant la course à 4 du 17/10 vers
minuit (93 pour les 3 joueurs Bedrock, 8 pour le joueur Java), **aucun en 277 minutes de courses solo** depuis le
10/10. Les joueurs Java sont donc touchés aussi. Cacher les bateaux aux joueurs (1.3.0, 1.4.0) ne pouvait pas suffire.

**Changements** (seulement quand l'anti-collision est actif, course à plusieurs) :
- `GhostBoats` : chaque bateau de course est une sous-classe du bateau du jeu (bateau, radeau, bateau ou radeau à
  coffre) qui n'est ni solide ni poussable. Le serveur l'ignore dans les collisions entre entités ; les blocs comptent
  toujours (murs, hors-piste inchangés). Les joueurs voient un bateau normal (même type d'entité, rien à changer pour
  Geyser). Aussi pour la coque du bateau calculé par le serveur (1.6.0, réglage désactivé).
- `CollisionShield` : les pilotes sont rendus non solides pendant la course (`Player#setCollidable(false)`), valeur
  d'avant rendue à l'arrivée, à l'abandon ou à la fin.
- La tête Bedrock reste à la hauteur de la 1.6.1 (réglage « Bedrock : hauteur des têtes (cm) »).

**Code interne du serveur** : `GhostBoats` utilise les classes de Paper 26.3 (noms du jeu en clair depuis la 26.x).
Compilation contre `outils-build/serveur/versions/26.3/paper-26.3.jar`, produit par `telecharger-outils.sh` à partir du
Paper 26.3 build 159 public (identique octet pour octet à celui de Kal-Games). **À chaque mise à jour de Paper** :
recompiler et vérifier ; si ces classes ont changé, la course utilise des bateaux normaux et le journal du serveur
affiche une fois « Bateaux non solides indisponibles (version de Paper ?) ».

**Vérifié hors jeu** : compilation ; code de Paper relu (contrôle des déplacements de véhicule, collisions entre
entités, création de l'objet Bukkit d'une entité). **Non vérifié en jeu.**

**À vérifier en jeu (au moins 2 joueurs, Bedrock et Java si possible)** : on traverse les adversaires sans être
ramené en arrière ; plus de « moved wrongly » dans le journal pendant les courses à plusieurs ; départ, chute et
retour au point de contrôle, arrivée normaux ; pas de bateau vide qui reste sur la piste.

**Déploiement** : empilé sur la 1.6.1 non testée (le correctif de la 1.6.1 n'a pas suffi, celui-ci le complète),
à la demande de LeKiwi06. **Déployé sur Kal-Games le 17/10/2026 à 03:00 (LeKiwi06, serveur allumé : actif après
redémarrage ; 1.6.1 dans `_removed-kg_boatrace-1.6.1/` ; supprimables par l'humain : `_removed-kg_boatrace-1.3.0/`,
`1.4.0/`, `1.4.1/`, `1.4.2/`, `1.5.0/`). Statut : non testé en jeu.**
