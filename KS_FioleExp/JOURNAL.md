# KS_FioleExp - journal

Plugin autonome, serveur Event. Stocke des points d'expérience dans une fiole, utilisable ensuite comme une fiole
d'expérience vanilla.

## 1.0.0 - fiole remplie à l'enclume (29/09/2026)

Demande de Maxster33 : pouvoir stocker l'XP dans un objet, utilisable de la même manière que la fiole d'expérience
existante. Choix de Maxster33 : remplissage à l'enclume, nombre de **points** (pas de niveaux) tapé dans le champ du
nom ; nom « Fiole d'expérience (1 395 XP) » ; orbes au sol comme en vanilla ; une seule fiole vide remplie par
opération ; usure vanilla de l'enclume.

- **Remplissage** : fiole vide dans la 1re case, 2e case vide, nombre de points dans le champ du nom (espaces
  acceptés : « 1 395 »). Résultat : fiole d'expérience vanilla nommée et marquée (`ks_fioleexp:points`). Pas de
  résultat si le joueur n'a pas assez de points. Texte qui n'est pas un nombre : renommage vanilla de la fiole vide.
- **Coût affiché** : nombre de niveaux que le joueur va perdre (au moins 1). Le prélèvement réel est fait par le
  plugin (`InventoryClickEvent` sur la case résultat, annulé) : exactement les points demandés sont retirés (et non
  des niveaux entiers), une seule fiole vide est consommée (l'enclume vanilla prendrait toute la pile). Prise au clic
  gauche / droit (curseur) ou Maj+clic (inventaire) ; les autres clics (touches 1-9, etc.) ne font rien.
- **Usure** : 12 % de chance que l'enclume passe à l'état suivant (cassée à la 3e), sauf en créatif. En créatif, les
  points sont quand même retirés.
- **Utilisation** : lancée, la fiole se brise comme en vanilla (`ExpBottleEvent`) et lâche exactement les points
  stockés en orbes (ramassables par n'importe qui, absorbées par le Raccommodage comme en vanilla). Marche aussi
  lancée par un distributeur. Les fioles de même quantité s'empilent.
- **Autres plugins** : `KSFioleExp.creerFiole(points)` crée une fiole remplie (utilisée par KS_KaliumGive 1.2.0,
  id `fiole_exp(<points>)`, à déployer ensemble).
- Indépendant de KS_LootEntites (fioles « niveau N », autre marqueur) et compatible avec KS_Enclume (plafond levé).

Limites :
- Au-delà de 40 niveaux de coût, le jeu du joueur peut afficher « Trop cher ! » ; la prise reste acceptée par le
  serveur (même limite que KS_Enclume). À vérifier en jeu.
- Une fiole remplie peut être renommée à l'enclume : le nombre affiché ne correspond alors plus au contenu (le contenu
  réel reste celui du marqueur). Signalé, non traité.
- Bedrock (Geyser) : affichage du résultat et du coût de l'enclume à vérifier.

**Déployé sur Event le 29/09/2026 à 03:39 (avec KS_KaliumGive 1.2.0). Statut : testé et confirmé par Maxster33 le 29/09/2026.**

## 1.1.0 - fioles d'expérience impossibles à renommer (29/09/2026)

Demande de Maxster33 : rendre impossible le renommage des fioles d'expérience. Toute fiole d'expérience (vanilla,
remplie par KS_FioleExp ou « niveau N » de KS_LootEntites) en 1re case de l'enclume : aucun résultat. Le nom d'une
fiole remplie correspond donc toujours à son contenu.
Avec KS_Enclume 1.1.0 : au-delà de 39 niveaux, le coût affiché pour remplir une fiole est plafonné à 39 et le vrai
coût est écrit en barre d'action (le prélèvement reste fait en points par KS_FioleExp).

**Déployé sur Event le 29/09/2026 à 03:54 (1.0.0 dans `_removed-ks_fioleexp-1.0.0/`). Statut : testé et confirmé en Java par Maxster33 le 29/09/2026 ; Bedrock : problème de remplissage à l'enclume (voir REPRISE_PROJET.md).**

## 1.2.0 - formulaire Bedrock pour remplir une fiole (29/09/2026)

Test de Maxster33 (Bedrock) : à l'enclume, la fiole reçue est la bonne mais on ne voit pas le nombre de niveaux
consommés (l'aperçu Bedrock montre un simple renommage « 1395 »). Cause : sur Bedrock, le texte du champ du nom n'est
envoyé au serveur (par Geyser) qu'à la prise du résultat ; le serveur ne peut pas calculer le coût avant. Demande :
voir le nombre de niveaux consommés. Choix de Maxster33 : formulaire Floodgate.
- Joueurs Bedrock (Floodgate) : **accroupi + clic droit sur une enclume avec une fiole vide en main** → formulaire
  « Fiole d'expérience » : points et niveau actuels, champ « Nombre de points à stocker ». Puis confirmation :
  « Stocker 1 395 points consommera 12 niveaux : tu passeras du niveau 40 au niveau 28. » (Confirmer / Annuler).
- Confirmation : tout est revérifié (points suffisants, fiole vide dans l'inventaire, enclume toujours là et à 6 blocs
  au plus) ; une fiole vide consommée, points exacts retirés, fiole remplie donnée (au sol si inventaire plein), usure
  vanilla de l'enclume. Erreur (nombre invalide, pas assez de points, pas de fiole vide, enclume hors de portée) : le
  1er formulaire revient avec le message en rouge.
- Un seul formulaire par seconde (un clic Bedrock peut arriver en double).
- Joueurs Java : inchangés (champ du nom de l'enclume). L'enclume reste utilisable par les joueurs Bedrock (sans coût
  visible).
- Technique : classe `FormulaireBedrock` chargée seulement si Floodgate est activé (`softdepend: [floodgate]`) ;
  compilé contre l'API Floodgate 2.2.5 et Cumulus 1.1.2, ajoutés à `telecharger-outils.sh` (à relancer sur chaque PC).

**Déployé sur Event le 29/09/2026 à 05:58 (1.1.0 dans `_removed-ks_fioleexp-1.1.0/`). Statut : testé et confirmé en Bedrock par Maxster33 le 29/09/2026.**

## 1.3.0 - même menu pour les joueurs Java (29/09/2026)

Demande de Maxster33 : uniformiser ; les joueurs Java accèdent eux aussi à un menu par accroupi + clic droit avec une
fiole vide sur l'enclume, avec le même texte que sur Bedrock.
- Joueurs Java : **dialogue natif** de Minecraft (API Dialog de Paper) : 1er dialogue « Fiole d'expérience » (points et
  niveau actuels, champ « Nombre de points à stocker », boutons Valider / Annuler), puis confirmation « Stocker 1 395
  points consommera 12 niveaux : tu passeras du niveau 40 au niveau 28. » (Confirmer / Annuler). Erreurs : le 1er
  dialogue revient avec le message en rouge. Seule différence visible : pas de texte d'exemple grisé « ex. 1395 » dans
  le champ (les dialogues Java n'en ont pas), et le bouton du 1er menu s'appelle « Valider » (sur Bedrock, son nom est
  choisi par le jeu).
- Joueurs Bedrock : inchangés (formulaire Floodgate).
- Technique : nouvelle classe `MenuFiole` (déclencheur, textes, vérifications, remplissage, dialogues Java) commune aux
  deux ; `FormulaireBedrock` n'affiche plus que les formulaires Floodgate. Sans Floodgate, tout le monde reçoit le
  dialogue Java. Chaque bouton de dialogue ne sert qu'une fois (10 min au plus).
- Le remplissage par le champ du nom de l'enclume (1.0.0) reste possible pour tous.

Limites :
- Les dialogues existent depuis Minecraft Java 1.21.6 : un joueur connecté avec une version plus ancienne (ViaBackwards)
  ne verra probablement pas le menu (non vérifié).

**Déployé sur Event le 29/09/2026 à 10:24 (1.2.0 dans `_removed-ks_fioleexp-1.2.0/`), actif après redémarrage d'Event.
Statut : non testé en jeu.**

## 1.4.0 - usure de l'enclume réduite (29/09/2026)

Demande de Maxster33 : « la fabrication de fiole d'exp abîme trop l'enclume, il faut réduire ». Choix de Maxster33 :
6 % (au lieu de 12 %, valeur vanilla), réglable dans le `config.yml`.
- Chaque fiole remplie (champ du nom de l'enclume ou menu Java / Bedrock) a `usure-enclume-pourcent` % de chance
  d'abîmer l'enclume d'un cran (cassée au 3e cran) : 6 par défaut, soit environ 50 fioles en moyenne par enclume
  (25 avant). 0 = aucune usure. Toujours aucune usure en créatif. Les autres usages de l'enclume restent vanilla.
- Nouveau `config.yml` (le plugin n'en avait pas : il est créé au premier démarrage de la 1.4.0). La valeur par défaut
  6 est aussi dans le code. Valeur lue au démarrage : après modification, redémarrer le serveur.

**Déployé sur Event le 29/09/2026 à 10:57 (1.3.0 dans `_removed-ks_fioleexp-1.3.0/`), actif après redémarrage d'Event.
Statut : non testé en jeu (1.3.0, menu Java, pas encore testée non plus).**

## 1.5.0 - fiole remplie en niveaux (29/09/2026)

Demande de Maxster33 : écrire un nombre de **niveaux** au lieu d'un nombre de points (niveau 50 = les points pour passer
du niveau 0 au niveau 50), sur Java et Bedrock, par les deux méthodes (champ du nom de l'enclume et menu) ; renommer la
fiole en niveaux et mettre ses points en description ; dans le menu, détailler les points du joueur et le coût.
Choix de Maxster33 : nom « Fiole d'expérience (niveau 50) » (comme les fioles de KS_LootEntites, même contenu) ;
`fiole_exp(N)` de KS_KaliumGive en niveaux aussi.
- **Saisie** : nombre de niveaux de 1 à 20 000 (espaces acceptés), dans le champ du nom de l'enclume ou dans le menu.
  La fiole contient les points pour passer du niveau 0 à ce niveau (formule vanilla : 30 → 1 395, 50 → 5 345).
- **Fiole** : « Fiole d'expérience (niveau 50) », description (gris) « 5 345 points d'expérience ». Les fioles de même
  niveau s'empilent. Remplacement de `creerFiole(points)` par `creerFioleNiveaux(niveaux)` (KS_KaliumGive 1.4.0).
- **Menu** (Java et Bedrock, mêmes textes) :
  - 1er menu : « Tu as 2 920 points d'expérience (niveau 40). / Une fiole de N niveaux contient les points d'expérience
    pour passer du niveau 0 au niveau N. / Une fiole vide de ton inventaire sera remplie. », champ « Nombre de niveaux
    à stocker » (Bedrock : exemple « ex. 30 ») ;
  - confirmation : « Fiole de 30 niveaux : 1 395 points d'expérience. / Tu as 2 920 points d'expérience (niveau 40). /
    Coût : 1 395 points, soit 7 niveaux : tu passeras du niveau 40 au niveau 33 (1 525 points restants). » (si aucun
    niveau n'est perdu : « tu resteras au niveau 40 ») ;
  - erreurs : « Tape un nombre de niveaux (ex. 30). », « Tu n'as pas assez de points d'expérience : il en faut 5 345,
    tu en as 2 920. », fiole vide manquante, enclume hors de portée.
- **Enclume** : coût affiché = niveaux que le joueur va perdre (inchangé), prélèvement exact des points.
- Les fioles faites avant (« Fiole d'expérience (1 395 XP) ») gardent leur nom et leur contenu, et ne s'empilent pas
  avec les nouvelles.

Limites :
- Stocker N niveaux ne fait pas perdre N niveaux : un joueur de niveau 40 qui stocke 30 niveaux (1 395 points) perd 7
  niveaux (les niveaux hauts valent plus de points). La confirmation du menu le détaille ; à l'enclume, seul le nombre de
  niveaux perdus est affiché.

**À déployer avec KS_KaliumGive 1.4.0 (sinon `/kaliumgive ... fiole_exp(...)` échoue). Compilé le 29/09/2026, non
déployé. Statut : non testé en jeu.**
