# Build Battle — cahier des charges (demande de LeKiwi06, 27/09/2026)

Brouillon, validé point par point avec LeKiwi06. Couvre les deux plugins du jeu :

| Plugin | Serveur | Rôle |
|---|---|---|
| `KG_BuildBattle` | kal-games (7001) | Entrée dans KG_Menu, file publique, création / liste des parties privées, envoi des joueurs vers Kanvas, points renvoyés à KG_ScoreBoards |
| `KV_BuildBattle` | Kanvas (5038) | Le jeu : salle d'attente, arène, terrains, thème, chrono, votes, résultats, retour sur kal-games |

Pourquoi sur Kanvas : le jeu se joue en créatif avec FastAsyncWorldEdit, avec les mêmes protections que les plots
(WorldGuard + FAWE limité aux régions dont on est membre). Accessible uniquement depuis kal-games. Même schéma que
le Bingo (KG_Bingo sur kal-games / KG_BingoGame sur Serveur Jeux, liaison par le relais KaliumRelay).

## Décisions (LeKiwi06, 27/09/2026)

- **Déroulement classique chronométré** : tout le monde construit en même temps sur le même thème, puis tout le monde
  est téléporté ensemble sur chaque terrain, l'un après l'autre, et vote avec les terracottas (comme sur les plots).
- **Tempos** : fast **3 min**, normal **5 min**, longue **10 min**, extra **30 min**.
- **Équipes** de 1 à 4 joueurs (solo, duo, trio, squad).
- **Lancement** :
  - **File publique** : tempo **normal** uniquement, en **solo, duo, trio ou squad** (une file par taille d'équipe).
  - **Parties privées** : créées par un joueur (l'hôte), qui règle le tempo et la taille des équipes ; les autres les
    rejoignent (comme le Bingo).
- **Thème** : les joueurs **votent parmi 5 thèmes** tirés au hasard. En partie privée, **mode spécial** où les joueurs
  **écrivent leur thème** (64 caractères au plus).
- **Arène** : déjà construite par LeKiwi06 (pour l'instant dans le même monde que les plots, elle ira dans un **monde à
  part**). **32 boîtes** en tout = **4 parties simultanées de 8 équipes au plus**. Outils de capture : l'arène entière,
  puis les zones constructibles.
- **Salle d'attente sur Kanvas** : les joueurs y passent en rejoignant une file ou une partie. LeKiwi06 la construira
  plus tard ; il faut l'option pour la capturer « comme pour le Bingo ».
- **Boîtes par partie** : de **2 à 8** (une par équipe), prises parmi les 32 boîtes libres de l'arène.
- **Capture de l'arène** : sert à la **recoller une fois dans le monde à part**. Après une partie, on ne recharge pas
  l'arène : on **vide seulement les zones constructibles** des boîtes utilisées.
- **Thème écrit (partie privée)** : chaque joueur propose un thème (64 caractères) pendant **1 min** (passe à **3 s**
  quand tout le monde a proposé), puis **vote de 30 s** parmi les propositions (passe à **3 s** quand tout le monde a
  voté). Pendant ce vote, la **poudre de blaze** sert à **signaler un thème inapproprié**.
- **Vote des terrains** : **30 s** par terrain par défaut, qui passent à **3 s** quand tout le monde a voté.
- **Thème signalé** : message au staff connecté + enregistrement dans un fichier (le thème n'est pas retiré du vote).
- **File publique** : compte à rebours de **30 s** dès qu'il y a de quoi faire **2 équipes** ; départ immédiat à **8
  équipes**.
- **Points et classements** (KG_ScoreBoards) : **étape suivante**, une fois le jeu jouable et testé.
- **Salle d'attente** : capturée une fois, puis **une copie par partie** (comme au Bingo).

## Choix par défaut (Claude, à ajuster si besoin)

- Vote des 5 thèmes (partie publique) : même rythme que le vote des thèmes écrits (30 s, puis 3 s quand tout le monde
  a voté) ; égalité = tirage au hasard entre les ex æquo.
- Terrain vide (aucun bloc posé) : présenté au vote comme les autres.
- Fin de partie : classement annoncé (titre + chat), 15 s pour regarder, puis retour sur kal-games.
- Déconnexion : pas de reconnexion à la partie dans la 1re version ; l'équipe continue sans lui (une équipe vide
  garde son terrain au vote).

## Étapes

1. **KV_BuildBattle : arène** (0.1.0, compilé le 27/09/2026) — monde à part, capture de l'arène entière et des 32 zones constructibles, collage dans
   le monde à part, capture de la salle d'attente (menu comme au Bingo).
2. **KV_BuildBattle : partie** (0.3.0, compilé le 27/09/2026) — thème (5 au choix / thèmes écrits), construction chronométrée, gel, votes, résultats,
   vidage des zones ; lançable par le staff sur Kanvas pour tester sans kal-games.
3. **KG_BuildBattle** (0.1.0 + accueil dans KV_BuildBattle 0.2.0, compilés le 27/09/2026, avant l'étape 2 à la demande
   de LeKiwi06 ; liaison « comme le Bingo », sans le moteur de KalGames) — file publique (solo / duo / trio / squad, tempo normal), parties privées (hôte : tempo, taille
   des équipes, thème écrit), envoi vers Kanvas par le relais, retour sur kal-games.
4. **Points et classements** (KG_ScoreBoards).

## Repris de KV_Plots (principes, pas de dépendance au monde des plots)

- Créatif + vol ; FAWE autorisé **dans son terrain uniquement** (région WorldGuard par terrain, membres = l'équipe) ;
  une commande qui dépasse du terrain n'agit que sur la partie dedans ; limites anti-crash de FAWE déjà réglées.
- **Fin de la construction** : le terrain est figé (plus de membres dans sa région : ni blocs ni FAWE).
- **Vote** : 5 terracottas (rouge 1 … vert foncé 5) sur les cases 3 à 7 ; pas de vote sur le terrain de son équipe.
- Pas de TNT ni d'explosion, rien ne brûle, liquides limités au terrain, redstone coupée.

- **Boîtes identiques** : on en capture **une** (boîte entière + zone constructible), le plugin la recopie en **4
  colonnes de 8** dans le monde à part, une colonne par partie, **espacées** pour qu'on ne voie pas les pseudos des
  autres parties (LeKiwi06, 27/09/2026).

## Questions ouvertes

1. **Points d'apparition** : choix par défaut (Claude) : un point posé par l'admin dans la boîte, qui sert à
   l'apparition de l'équipe et de point de vue pendant le vote de son terrain.
