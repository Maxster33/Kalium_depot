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

Actif depuis le redémarrage d'Event du 13/10/2026 à 07:44 (journal lu : chargé et activé sans erreur ; `config.yml` et `lang.yml` créés). Statut : non testé en jeu.

**Testé en jeu par LeKiwi06 le 13/10/2026 sur Event (Java)** : « je l'ai testé en jeu, ça fonctionne bien, si je le
casse alors qu'il joue une musique il rend le disque etc ». Défaut trouvé par lui le même jour (jukebox fantôme) :
corrigé en 1.1.0.

## 1.1.0 - bateau à coffre et jukebox, plus de jukebox fantôme (13/10/2026, LeKiwi06)

### Bateau à coffre et jukebox

Demande de LeKiwi06 : « est ce qu'on pourrait créer un bateau avec coffre et jukebox ? on ne peux pas monter dedans,
mais on peux le tracter avec une laisse, alors on pourrait l'accorcher a notre bateau ? comme ça le juke box lirait en
automatique les disque présent dans le coffre en boucle (on pourrait aussi mettre d'autre choses dans le coffres, pas
uniquement des disques) ».

Ses réponses : les deux bateaux existent ; « le sneak clique ouvre le coffre, et un clique normal permet de switch
entre play et pause » ; recette bateau à coffre + jukebox.

- **Objet** : un bateau à coffre ou un radeau à coffre vanilla, nommé « Bateau à coffre et jukebox » (« Radeau à
  coffre et jukebox »), 4 lignes de description. Recette dans KS_Crafts 1.14.0.
- **Bateau posé** : un vrai bateau à coffre (27 cases, tout objet ; les joueurs Bedrock le voient comme tel), le
  jukebox affiché devant le coffre, à la place du siège. **Personne n'y monte.**
- **Accroupi + clic droit** : le coffre s'ouvre (comportement du jeu). **Clic droit normal** : lecture / pause, avec
  un message au-dessus de la barre d'objets (« Lecture : morceau », « Pause », « Aucun disque dans le coffre »).
- **Lecture en boucle** : en lecture (état de départ), les disques du coffre sont joués l'un après l'autre, dans
  l'ordre des cases, puis on recommence ; 1 seconde de silence entre deux. Les disques restent dans le coffre. Le
  disque retiré pendant qu'il joue s'arrête et le suivant démarre. L'état lecture / pause et la case en cours sont
  gardés avec le bateau.
- **Laisse** : c'est le jeu qui s'en charge (lu dans le code du jeu 26.3). Laisse en main, clic droit sur le bateau :
  on le tient. Puis accroupi + clic droit sur son propre bateau : la laisse passe du joueur à ce bateau, qui le
  tracte. Clic droit sur le bateau tenu : on reprend la laisse. Cisailles : toutes les laisses sont coupées.
- **Cassé** : il lâche le bateau à coffre et jukebox du même bois et le contenu du coffre.
- `config.yml` : nouvelle clé `jukebox.position-coffre` (0.4), valeur par défaut dans le code. **Elle n'est pas
  ajoutée au `config.yml` déjà créé sur Event** : l'y écrire à la main s'il faut la régler.

Techniquement nécessaire, signalé :

- Le clic droit normal ne fait pas lecture / pause, et le jeu agit comme d'habitude, quand le joueur a une laisse ou
  des cisailles en main (attacher, couper) ou tient la laisse de ce bateau (la reprendre) : sans cela on ne pourrait
  plus l'attacher ni le détacher.
- Bateau à jukebox simple : accroupi + clic droit n'ouvre pas le menu des disques quand le joueur tient une entité en
  laisse, sinon on ne pourrait pas y accrocher le bateau à coffre.

Choix de Claude, signalés :

- Rien ne joue sans joueur à portée de la musique (64 blocs) : un joueur qui arrive en cours de morceau ne
  l'entendrait pas (limite du jeu). Dès qu'un joueur revient à portée, le disque interrompu reprend à son début. Avec
  plusieurs joueurs, celui qui arrive en cours de morceau attend le suivant.
- « Pause » arrête le morceau ; « lecture » le reprend à son début (le jeu ne sait pas reprendre un son au milieu).
- N'importe qui peut faire lecture / pause, sauf là où une protection interdit de toucher au bateau. Le coffre, lui,
  est protégé comme tout bateau à coffre (KS_Claim : il ne se casse pas chez les autres).

Limites connues : celles de la 1.0.0 (Bedrock : pas de jukebox visible, musique qui ne suit sans doute pas le
bateau, ce qui gêne davantage pour un bateau tracté). Position du jukebox devant le coffre écrite sans test en jeu.

### Jukebox fantôme (correction)

Signalé par LeKiwi06 le 13/10/2026, avec une capture : « le jeu refuse que je monte dans le bateau quand il est
accroché a un happy ghast, il m'en fait déscendre direct et laisse derrière un juke box fantome immobile et
invulnérable ».

- **Cause de la descente** : pas le plugin, ni le jeu (rien dans le code du jeu 26.3 n'éjecte les passagers d'un
  bateau tenu en laisse). Le journal de GrimAC sur Event porte 3 signalements « Simulation » (déplacement différent
  de celui prévu) pour LeKiwi06 à 08:00:13, 08:00:13 et 08:00:27, au moment des essais : l'anti-triche ne sait pas
  prévoir le mouvement d'un bateau tiré par un Happy Ghast, et renvoie le joueur en éjectant tous les passagers du
  bateau. **Non corrigé ici** (réglage de GrimAC, à décider) ; un bateau vanilla accroché à un Happy Ghast devrait
  faire descendre le joueur de la même façon (à vérifier par LeKiwi06).
- **Cause du fantôme** (défaut du plugin) : le jukebox était éjecté avec le joueur, restait sur place, et le bateau
  en recevait un nouveau.
- **Correction** : un jukebox descendu de son bateau y remonte au tick suivant ; sans bateau, ou si le bateau en a
  déjà un, il disparaît. Les fantômes déjà laissés sur Event disparaissent au chargement de leur chunk (donc au
  redémarrage).
- **Simplification** : le jukebox ne quitte plus le bateau quand un joueur monte. Le jeu place de lui-même un
  joueur en premier passager (celui qui conduit), lu dans le code du jeu 26.3 : le détour de la 1.0.0 était inutile.

**À déployer avec KS_Crafts 1.14.0.** Compilé le 13/10/2026 (`sortie/KS_JukeboxBoat-1.1.0.jar`).
**Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 13/10/2026 à 08:21 (LeKiwi06 : « déployer le nouveau jar » ; envoyé avec KS_Crafts 1.14.0 ; 1.0.0 et son `config.yml` dans `_removed-ks_jukeboxboat-1.0.0/` ; le `config.yml` du serveur, identique à celui de la 1.0.0, est remplacé par celui de la 1.1.0 : la clé `jukebox.position-coffre` y est donc ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage d'Event. Kixster n'est pas touché. Statut : non testé en jeu.**

Essai de LeKiwi06 (13/10/2026) avec un bateau vanilla accroché à un Happy Ghast : « le bateau normal m'ejecte aussi, mais les laisses n'ont pas buggué ». La descente vient donc de GrimAC, pas du plugin. Les laisses figées de sa capture ne se sont pas reproduites : pas d'enquête tant que cela ne revient pas (sa décision).

Actif depuis le redémarrage d'Event du 13/10/2026 (journal lu : 1.1.0 chargée et activée sans erreur ; `lang.yml` complété). Statut : non testé en jeu.

Laisses figées de la capture du 13/10 : LeKiwi06 a refait l'essai, « c'est juste un bug visuel coté client les
laisses ». Rien à corriger dans le plugin.

## 1.1.1 - jukebox 30 % plus gros (13/10/2026, LeKiwi06)

Demande de LeKiwi06 après son essai du bateau à coffre et jukebox : « il faudrait que le jukebox soit a peut près 30%
plus gros dans le bateau, je le trouve trop petit par rapport au coffre ». À la question « sur quel bateau ? » : « Les
deux bateaux ».

- `jukebox.taille` : 0.55 → **0.72** (0,55 x 1,3 = 0,715), pour les deux bateaux.
- Techniquement nécessaire, signalé : le jukebox est replacé pour sa nouvelle taille. Bateau à coffre :
  `jukebox.position-coffre` 0.4 → 0.49 (il ne rentre pas dans le coffre). Bateau simple : `jukebox.position`
  -0.4 → -0.36 (il dépasse à peine de la poupe et reste derrière le dos du conducteur). Valeurs calculées sur les
  dimensions du modèle du jeu, non vues en jeu.
- Seules ces trois valeurs par défaut changent (code et `config.yml` fourni). Sur un serveur qui a déjà son
  `config.yml`, il faut aussi le remplacer.

Compilé le 13/10/2026 (`sortie/KS_JukeboxBoat-1.1.1.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 13/10/2026 à 10:34 (LeKiwi06 : « Les deux bateaux », envoi annoncé avec KS_Crafts 1.14.1 ; 1.1.0 et son `config.yml` dans `_removed-ks_jukeboxboat-1.1.0/` ; `config.yml` du serveur, identique à celui de la 1.1.0, remplacé par celui de la 1.1.1 ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage d'Event. Kixster n'est pas touché. Statut : non testé en jeu.**

## 1.1.2 - jukebox de la taille exacte du coffre (13/10/2026, LeKiwi06)

Demande de LeKiwi06 : « si tu connais la taille du coffre, est-ce que tu peux faire en sorte que le jukebox fasse pile
la même taille ? ».

Mesures lues dans le modèle du jeu 26.3 (`BoatModel.createChestBoatModel`, rendu sans mise à l'échelle : 1 pixel =
1/16 de bloc) : le coffre est un cube de **12 pixels = 0,75 bloc** (fond de 8 pixels + couvercle de 4), posé sur le
fond du bateau, centré en largeur, de 0,125 à 0,875 bloc derrière le centre du bateau ; sa serrure dépasse d'un pixel
vers l'avant.

- `jukebox.taille` : 0.72 → **0.75**, pour les deux bateaux (choix « Les deux bateaux » de la 1.1.1). Hauteur
  inchangée : le jukebox repose déjà sur le fond du bateau, comme le coffre.
- Techniquement nécessaire, signalé : positions refaites sur ces mesures (celles de la 1.1.1 partaient d'un coffre
  mal situé). Bateau à coffre : `jukebox.position-coffre` 0.49 → **0.3125**, le jukebox touche la serrure du coffre
  sans y entrer. Bateau simple : `jukebox.position` -0.36 → **-0.5**, la place exacte du coffre d'un bateau à coffre.
- Radeau de bambou : son modèle est différent (`RaftModel`), les mêmes valeurs s'y appliquent sans avoir été
  recalculées.

Compilé le 13/10/2026 (`sortie/KS_JukeboxBoat-1.1.2.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 13/10/2026 à 10:39 (LeKiwi06 : « oui déploie la 1.1.2 sur event » ; 1.1.1 et son `config.yml` dans `_removed-ks_jukeboxboat-1.1.1/` ; `config.yml` du serveur, identique à celui de la 1.1.1, remplacé par celui de la 1.1.2 ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage d'Event. Kixster n'est pas touché. Statut : non testé en jeu.**

Actif depuis le redémarrage d'Event du 13/10/2026 à 10:46 (journal lu : 1.1.2 chargée et activée sans erreur). Statut : non testé en jeu.
