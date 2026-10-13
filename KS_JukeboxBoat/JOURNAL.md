# KS_JukeboxBoat - journal

Plugin de la survie (Event, Kixster) : bateau à jukebox. Dépend de KLM_Menu (menu en coffre, textes) ; sa recette est
dans KS_Crafts (1.13.0).

## 1.0.0 - bateau à jukebox (13/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai créer un plugin de bateau avec jukebox, un bateau très similaire au bateau avec
coffres, mais là il peut lire les disques de notre inventaire ».

Ses réponses aux questions posées avant le code :

| Question | Réponse de LeKiwi06 |
|---|---|
| Apparence du bateau posé | Bateau simple avec un jukebox visible à l'arrière sur Java ; les joueurs Bedrock voient un bateau vide et la 2e place est prise (préféré au vrai bateau à coffre, vu partout) |
| Qui entend la musique | Tous les joueurs proches, comme un vrai jukebox ; elle suit le bateau |
| Le disque pendant la lecture | Il va dans le bateau ; rendu à l'arrêt, lâché si le bateau est cassé ; la musique peut continuer sans joueur à bord |
| Obtention | Craft bateau + jukebox, sans forme, tous les bois et le radeau de bambou ; cassé, il redonne le bateau à jukebox |
| Menu (maquette) | Coffre : les disques de l'inventaire en haut, le disque du bateau et « Fermer » dans la barre du bas |
| Ouverture à bord | Touche de saut (à pied : accroupi + clic droit) |
| Nom du plugin | KS_JukeboxBoat |

- **Objet** : un bateau ou un radeau vanilla (son bois se voit), nommé « Bateau à jukebox » (« Radeau à jukebox »),
  3 lignes de description. Aucune apparence custom : rien à ajouter dans `geyser-bedrock`.
- **Bateau posé** : bateau simple marqué, qui porte le nom de l'objet (il s'affiche quand on le vise), avec un
  jukebox de 0,55 bloc à l'arrière. Ce jukebox est un modèle 3D de bloc (BlockDisplay) passager du bateau : il prend
  la 2e place, un seul joueur monte à bord.
- **Menu** (`Contenant` de KLM_Menu) : à pied, accroupi + clic droit sur le bateau ; à bord, touche de saut. Hauteur
  du coffre selon le nombre de disques portés (rangement, barre d'objets, main secondaire). Un clic sur un disque : il
  quitte l'inventaire, va dans le bateau, la musique démarre (le disque déjà dans le bateau est rendu d'abord). Un
  clic sur le disque du bateau, au centre de la barre du bas : la musique s'arrête, le disque revient dans
  l'inventaire (à ses pieds s'il est plein). Chaque disque porte le nom de son morceau. Messages au-dessus de la
  barre d'objets.
- **Musique** : son du disque, émis par le bateau et qui le suit, catégorie « Jukebox / Blocs musicaux », volume 4
  (64 blocs, comme un jukebox). Tout disque jouable dans un jukebox marche, y compris ceux d'une version future.
- **Bateau cassé** : il lâche le bateau à jukebox du même bois (avec son nom s'il a été renommé) et son disque.
- **Recette** : dans KS_Crafts 1.13.0 (à déployer ensemble). Sans KS_Crafts, l'objet n'a aucune recette.
- `config.yml` : `volume`, `jukebox.taille`, `jukebox.hauteur`, `jukebox.position` (pour régler l'affichage du
  jukebox sans recompiler ; valeurs par défaut aussi dans le code).

Techniquement nécessaire, signalé :

- Seul un joueur monte dans un bateau à jukebox : un animal ou un monstre y prendrait la place du jukebox ou du
  conducteur.
- Le bateau à jukebox ne sert pas d'ingrédient (bateau à coffre, etc.) : le jukebox serait perdu. Un distributeur ne
  le pose pas (il poserait un bateau simple) : l'objet y reste.
- Le jukebox quitte le bateau pendant un tick quand un joueur monte, pour que le joueur soit le premier passager
  (celui qui conduit).

Choix de Claude, signalés :

- N'importe qui peut ouvrir le menu d'un bateau et reprendre son disque, comme sur un jukebox posé, sauf là où une
  protection interdit de toucher aux bateaux. Avec KS_Claim 1.3.0, un bateau sans inventaire se casse partout : le
  disque d'un bateau à jukebox laissé quelque part peut donc être pris par un autre joueur.
- Le disque n'est pas rendu à la fin du morceau : il reste dans le bateau (comme dans un jukebox). Pour le réécouter,
  le reprendre puis le remettre.
- Le menu ne commande plus le bateau à plus de 8 blocs.

Limites connues :

- **Bedrock** : pas de jukebox visible (choix de LeKiwi06) ; la musique ne suit sans doute pas le bateau (Geyser joue
  un son à l'endroit où il a démarré) : en s'éloignant, un joueur Bedrock l'entend de moins en moins. La touche de
  saut à bord est à vérifier sur écran tactile ; à pied, accroupi + clic droit marche dans tous les cas.
- La musique s'arrête pour un joueur qui s'éloigne au point de ne plus voir le bateau, ou quand son chunk est
  déchargé ; elle ne reprend pas toute seule. Un joueur qui arrive pendant le morceau ne l'entend pas (comme un
  jukebox).
- Reprendre le disque coupe aussi, pour les joueurs proches, un vrai jukebox qui jouerait le même morceau.
- Position et taille du jukebox écrites sans serveur de test : à régler en jeu (`config.yml`).

Compilé le 13/10/2026.

**Déployé sur Event le 13/10/2026 à 07:39 (LeKiwi06 : « oui déploie sur event » ; envoyé avec KS_Crafts 1.13.0), actif après redémarrage d'Event. Kixster n'est pas touché. Statut : non testé en jeu.**
