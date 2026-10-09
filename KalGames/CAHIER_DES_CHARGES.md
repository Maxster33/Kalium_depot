# Bouton « Rejouer » — cahier des charges (demande de LeKiwi06, 09/10/2026)

**Validé par LeKiwi06 le 09/10/2026** : partie 4 validée en bloc (« Validé tel quel ») ; ordre du travail tranché :
« tout d'un coup » (moteur + Bingo + Build Battle codés et déployés ensemble, avant tout test), avec accord explicite
pour empiler sur KalGames 1.23.0 non testé. Reste ouvert : question 5.3 (observateurs du Bingo).
**Code : KalGames 1.24.0, KG_Bingo 1.9.0, KG_BingoGame 0.11.0, KG_BuildBattle 0.4.0, KV_BuildBattle 0.4.0, déployés le
09/10/2026 à 5 h 46 (Kal-Games, Serveur Jeux, Kanvas), non testés** (détail dans le `JOURNAL.md` de chaque plugin).

Pas un nouveau plugin : une fonction ajoutée aux jeux existants.

| Plugin touché | Serveur | Pourquoi |
|---|---|---|
| `KalGames` (moteur des parties) | kal-games (7001) | Couvre d'un coup les 7 jeux du moteur : PvP Kit, Parcours, Course de bateau, Hide and Seek, Piliers de la Fortune, Pong, Rush. Aucun changement prévu dans les plugins de ces jeux |
| `KG_Bingo` + `KG_BingoGame` | kal-games + Serveur Jeux (7015) | Le Bingo se termine sur Serveur Jeux (salle d'attente d'après-partie) |
| `KG_BuildBattle` + `KV_BuildBattle` | kal-games + Kanvas (5038) | Le Build Battle se termine sur Kanvas, puis renvoie sur kal-games |
| `KG_BingoObservateur` | Serveur Jeux | Seulement si les observateurs du Bingo doivent aussi avoir le bouton (question 5.3) |

## 1. Demande d'origine (recopiée telle quelle, 09/10/2026)

> pour tout les mini jeux :  faire un bouton a la fin de la partie pour relancer une partie avec les mêmes paramètres

## 2. Réponses de LeKiwi06 (09/10/2026)

1. **Durée** : « il faut proposer ça aux joueurs pendant 30 secondes à la fin d'une partie ».
2. **Spectateurs** : « il faut aussi le proposer aux joueurs qui étaient en train de regarder la partie en
   spectateur ».
3. **Forme** : un **objet « Rejouer » dans la barre d'objets**, pendant 30 s, puis il disparaît (Java et Bedrock ; ne
   cache pas le résumé des points).
4. **Partie privée, qui relance** : **le premier qui clique** recrée la partie (même jeu, même arène, mêmes réglages)
   et en devient l'hôte ; chez les autres, le bouton devient « Rejoindre la partie de X ».
5. **Partie publique** : le bouton remet le joueur dans la file du même jeu.
6. **Un spectateur qui clique** : **il choisit** entre « Jouer » (il entre comme joueur s'il reste de la place) et
   « Regarder » (il revient en spectateur).
7. **Périmètre** : **tout maintenant** : les 7 jeux du moteur + Bingo + Build Battle, dans le même chantier.

## 3. Règles

### Jeux du moteur de Kal-Games (KalGames)
1. À la fin d'un match, quand les joueurs et les spectateurs sont renvoyés au hub, chacun reçoit l'objet
   **« Rejouer »** dans sa barre. Il disparaît au bout de **30 s** (durée réglable).
2. **Partie publique** : clic = le joueur rejoint la file publique du même jeu (comme par le menu).
3. **Partie privée** :
   - le premier qui clique crée une nouvelle partie privée : même mini-jeu, même arène, mêmes réglages, même choix
     « listée / non listée » ; il en est l'hôte ; nouveau code de partie ;
   - dès qu'elle existe, l'objet des autres devient **« Rejoindre la partie de X »** : clic = entrée dans cette partie
     (comme avec le code) ;
   - la partie démarre comme d'habitude : c'est l'hôte qui la lance depuis le menu de la partie.
4. **Spectateur** de la partie précédente : clic = petit menu à 2 boutons, **« Jouer »** et **« Regarder »**.
   - partie privée pas encore recréée : « Jouer » la crée (il devient l'hôte) ; « Regarder » est grisé tant que
     personne ne l'a recréée ;
   - partie publique : « Jouer » = file publique ; « Regarder » = spectateur de la partie publique du jeu.
5. L'objet disparaît aussi dès que le joueur l'a utilisé, entre dans une autre partie, ou quitte le serveur.

### Bingo
6. À la fin d'une partie de Bingo, les joueurs sont dans la **salle d'attente d'après-partie de Serveur Jeux** : c'est
   là que l'objet « Rejouer » apparaît, pendant 30 s.
7. Le premier qui clique recrée une partie de Bingo avec les mêmes réglages (nombre d'équipes, taille des équipes,
   durée, mode, nombre de bingos, composition de la grille) et en devient l'hôte ; les autres ont « Rejoindre la partie
   de X ». La nouvelle partie se déroule comme une partie créée depuis kal-games (salle d'attente, choix des équipes,
   lancement par l'hôte) et apparaît dans la liste des parties de kal-games.

### Build Battle
8. Le Build Battle renvoie les joueurs sur kal-games 15 s après les résultats : l'objet « Rejouer » apparaît **à leur
   arrivée dans le hub de kal-games**, pendant 30 s.
9. Partie publique : clic = file publique du Build Battle. Partie privée : le premier qui clique recrée la partie
   (même rythme, même taille d'équipes, même nombre d'équipes, même mode « thèmes écrits ») et en devient l'hôte ; les
   autres ont « Rejoindre la partie de X ».

## 4. Choix d'interprétation (validés en bloc le 09/10/2026)

1. **« Mêmes paramètres » = les réglages choisis par l'hôte**, pas le hasard de la partie : nouvelle seed (nouvelles
   maps) et nouvelle grille au Bingo, nouveau thème tiré au Build Battle, nouveau vote de kit au PvP Kit, nouveaux
   rôles au Hide and Seek.
2. **Même arène** : la nouvelle partie privée reprend l'arène sur laquelle on vient de jouer (même si l'hôte avait
   choisi « au hasard »). Si elle n'est plus utilisable (retirée entre-temps), une autre est prise au hasard.
3. **Case de la barre : la 6** (7e case), réglable (`replay.slot`). Cases déjà prises dans le hub : 0 (comparateur),
   4 (étoile des mini-jeux), 8 (boussole).
4. **L'objet n'apparaît qu'après un match réellement terminé** : pas quand la partie est fermée par un admin, quand
   l'hôte ferme la salle avant le lancement, ni pour un joueur qui a quitté ou abandonné avant la fin.
5. **Limites habituelles respectées** : nombre maximum de parties privées du jeu, places de la partie, mini-jeu
   désactivé entre-temps : le clic affiche alors le motif du refus (le même qu'au menu).
6. **Les 30 s ne sont pas prolongées** quand quelqu'un a recréé la partie : passé ce délai, on la rejoint par les moyens
   habituels (liste des parties, code).
7. **`/menu off`** (objets de menu masqués) : l'objet « Rejouer » est quand même donné (il est temporaire).
8. **Nouvelles clés de configuration** (valeurs par défaut dans le code, à ajouter à la main sur le serveur pour les
   changer) : `replay.enabled` (true), `replay.seconds` (30), `replay.slot` (6).
9. **Technique (signalé, nécessaire)** : pour le Bingo et le Build Battle, la recréation passe par le relais
   (KaliumRelay) entre Serveur Jeux / Kanvas et kal-games. À déployer ensemble : KG_Bingo + KG_BingoGame, et
   KalGames + KG_BuildBattle + KV_BuildBattle.
   - **Choix fait au code (09/10/2026)** : au Bingo, les parties ne se créent que sur kal-games (limites, liste des
     parties, codes). Le clic sur « Rejouer » renvoie donc le joueur sur kal-games, qui recrée la partie (ou lui fait
     rejoindre celle déjà relancée) et le renvoie aussitôt sur Serveur Jeux : **deux écrans de chargement** au lieu
     d'aucun. La limite de **2 parties créées par heure et par joueur** (KG_Bingo 1.7.0, opérateurs exclus) s'applique
     à « Rejouer ».
   - KG_BuildBattle dépend maintenant de KalGames (l'objet « Rejouer » du hub est celui de KalGames).

## 5. Questions ouvertes

1. Réglé (voir 4.1) si validé : nouvelle seed au Bingo.
2. Réglé (voir 4.2) si validé : même arène.
3. **Observateurs du Bingo** (KG_BingoObservateur) : leur proposer aussi le bouton (« Regarder la nouvelle partie »
   seulement, ou choix Jouer / Regarder comme sur kal-games) ?
4. **Réglé (09/10/2026)** : « Rejouer : tout d'un coup » - moteur + Bingo + Build Battle codés et déployés ensemble ;
   accord explicite de LeKiwi06 pour empiler sur KalGames 1.23.0 (non testé en jeu).
