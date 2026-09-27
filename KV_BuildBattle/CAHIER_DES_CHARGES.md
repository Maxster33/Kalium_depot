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

## Repris de KV_Plots (principes, pas de dépendance au monde des plots)

- Créatif + vol ; FAWE autorisé **dans son terrain uniquement** (région WorldGuard par terrain, membres = l'équipe) ;
  une commande qui dépasse du terrain n'agit que sur la partie dedans ; limites anti-crash de FAWE déjà réglées.
- **Fin de la construction** : le terrain est figé (plus de membres dans sa région : ni blocs ni FAWE).
- **Vote** : 5 terracottas (rouge 1 … vert foncé 5) sur les cases 3 à 7 ; pas de vote sur le terrain de son équipe.
- Pas de TNT ni d'explosion, rien ne brûle, liquides limités au terrain, redstone coupée.

## Questions ouvertes

1. **Thème signalé** : que se passe-t-il ? (thème retiré du vote, message au staff, sanction ?)
2. **Vote des 5 thèmes** (partie publique) : même rythme (30 s, puis 3 s quand tout le monde a voté) ? Égalité = au
   hasard entre les ex æquo ?
3. **File publique** : nombre d'équipes pour lancer (2 minimum ?), compte à rebours, départ immédiat à 8 équipes ?
4. **Terrain vide** : une équipe dont personne n'a posé de bloc est-elle sautée au vote ?
5. **Points et classements** (KG_ScoreBoards, barème calé sur `EQUILIBRAGE_POINTS.md`) : maintenant, ou dans une
   étape suivante ?
6. **Fin de partie** : classement annoncé, puis retour automatique sur kal-games après combien de secondes ?
7. **Déconnexion** : un joueur qui se déconnecte peut-il revenir dans sa partie (reconnexion comme au Bingo) ?
8. **Salle d'attente** : une seule salle partagée par toutes les parties, ou une par partie ?
