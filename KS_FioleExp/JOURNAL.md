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

**Non déployé. Statut : non testé en jeu.**
