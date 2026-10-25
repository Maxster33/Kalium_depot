# KS_Enclume - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - enclume sans plafond de prix (25/09/2026)

Demande de Maxster33 : réparer ou fusionner sans « Trop cher ! », coût calculé comme en vanilla.
- Le plafond (`maximumRepairCost`, 40 niveaux en vanilla) est levé à l'ouverture de l'enclume et à chaque calcul
  (`InventoryOpenEvent`, `PrepareAnvilEvent`). Le coût et la pénalité des réparations successives restent vanilla ;
  le joueur doit toujours avoir assez de niveaux.
- Limite possible : le client Minecraft peut encore afficher « Trop cher ! » au-delà de 40 niveaux (affichage géré par
  le jeu du joueur) alors que le serveur accepte ; à vérifier en jeu.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - plus de « Trop cher ! » à l'écran (29/09/2026)

Demande de Maxster33 : au-delà de 40 niveaux, l'enclume affichait « Trop cher ! » (fioles de KS_FioleExp et
équipement), même si la prise était acceptée. Ce texte est affiché par le jeu du joueur dès 40 niveaux (le serveur ne
peut pas le changer). Choix de Maxster33 : 39 affiché + vrai coût écrit.
- Au-delà de 39 niveaux, le coût envoyé au joueur est plafonné à 39 (`PrepareAnvilEvent`, priorité HIGHEST, après
  les autres plugins) et le vrai coût s'écrit dans la barre d'action : « Coût réel : 55 niveaux » (vert si le joueur
  a assez de niveaux, rouge sinon).
- À la prise : refusée si le joueur n'a pas le vrai coût (message en barre d'action) ; sinon l'enclume vanilla retire
  les 39 niveaux et le plugin retire le reste au tick suivant, seulement si la prise a bien eu lieu (niveau du joueur
  baissé d'au moins 39). Les niveaux sont retirés comme en vanilla (progression dans le niveau gardée). En créatif :
  rien n'est retiré, comme en vanilla.
- Prises gérées par un autre plugin (clic annulé, ex. KS_FioleExp qui retire lui-même les points) : ignorées ; seul
  l'affichage est plafonné.

**Déployé sur Event le 29/09/2026 à 03:54 (1.0.0 dans `_removed-ks_enclume-1.0.0/`). Statut : non testé en jeu.**

## 1.1.1 - correctif : croix rouge au-delà de 39 niveaux (29/09/2026)

Signalé par Maxster33 (Java) : avec 1.1.0, au-delà de 39 niveaux l'enclume montrait une croix rouge (pas de résultat).
Cause : le jeu du joueur recalcule lui-même le résultat quand les cases d'entrée changent et le vide dès 40 niveaux
(valeur fixée dans le jeu) ; le résultat existait côté serveur (en 1.0.0 aussi : un clic sur la case vide le donnait).
Correctif : quand le coût est plafonné, le contenu de l'enclume est renvoyé au joueur au tick suivant
(`updateInventory`) : son jeu recalcule les cases d'entrée puis reçoit le résultat et le coût (39) du serveur.
- Vrai coût : la barre d'action était cachée par l'interface de l'enclume (signalé par Maxster33 ; chat refusé). Choix de
  Maxster33 : dernière ligne de la description du résultat, « Coût réel : 55 niveaux » (vert si assez de niveaux, rouge
  sinon). À la prise, le résultat sans cette ligne est remis dans la case juste avant (`setCurrentItem`) : l'objet obtenu
  ne la garde pas. Pas assez de niveaux : prise refusée sans message (la ligne est rouge).
- Demandé aussi : afficher « + » au lieu de 39. Impossible : le jeu du joueur écrit lui-même « Coût d'enchantement : N »
  avec un nombre entier envoyé par le serveur (un pack de ressources changerait le texte pour tous les coûts).

**Déployé sur Event le 29/09/2026 à 04:17 (1.1.0 dans `_removed-ks_enclume-1.1.0/`). Statut : non testé en jeu.**

## 1.1.2 - aucun coût affiché au-delà de 39 niveaux (29/09/2026)

Demande de Maxster33 : ne rien écrire à la place de 39 (« laisser juste coût : »). Le jeu du joueur écrit la ligne de
coût d'un bloc : impossible de garder « Coût : » sans nombre ; à 0, il n'écrit aucune ligne. Choix de Maxster33 : aucune
ligne.
- Au-delà de 39 niveaux, le coût envoyé au joueur est 0 : aucune ligne de coût dans l'enclume ; le vrai coût reste en
  dernière ligne de la description du résultat.
- À la prise : le vrai coût est rendu à l'enclume juste avant (`setRepairCost`) ; l'enclume vanilla vérifie et retire
  elle-même les niveaux (plus de retrait « 39 + reste » au tick suivant). Si la prise n'a pas eu lieu (curseur occupé...),
  l'affichage (coût 0, ligne ajoutée) est remis au tick suivant.
- Limite possible : avec un coût de 0, le jeu Java croit ne pas pouvoir prendre l'objet ; le serveur accepte la prise et
  corrige l'affichage (bref clignotement possible). À vérifier en jeu.

**Déployé sur Event le 29/09/2026 à 04:31 (1.1.1 dans `_removed-ks_enclume-1.1.1/`). Statut : testé et confirmé en Java par Maxster33 le 29/09/2026 ; Bedrock : vrai coût absent de la description (voir REPRISE_PROJET.md).**

## 1.1.3 - affichage pour les joueurs Bedrock (29/09/2026)

Test de Maxster33 (Bedrock) avec 1.1.2 : au-delà de 39 niveaux, résultat visible mais impossible à prendre, aucun coût
affiché. Cause : le jeu Bedrock (via Geyser, qui aligne son coût sur celui du serveur) refuse la prise à coût 0. Avec
1.1.1 (39 envoyé), la prise marchait. Demande : « Trop cher » sans assez de niveaux, « 40+ » sinon (« 40+ » impossible :
le jeu écrit un nombre ; 39 à la place).
- Joueurs Bedrock seulement : 39 envoyé s'ils ont assez de niveaux (ou en créatif), sinon le vrai coût (≥ 40 : leur jeu
  affiche « Trop cher ! »). Joueurs Java : inchangé (0, aucune ligne de coût).
- Détection : API Floodgate lue par réflexion (`softdepend: [floodgate]`, pas de dépendance de compilation) ; à défaut,
  UUID Floodgate (64 premiers bits à 0).
- Limites Bedrock (non corrigeables côté serveur) : le jeu Bedrock affiche son propre aperçu du résultat (pas la ligne
  « Coût réel », et pour une fiole remplie à l'enclume il montre un simple renommage « 1395 » ; la fiole reçue est
  pourtant la bonne). Un pack de ressources ne change pas ce calcul.

**Déployé sur Event le 29/09/2026 à 05:15 (1.1.2 dans `_removed-ks_enclume-1.1.2/`). Statut : non testé en jeu.**

## 1.1.4 - vrai coût visible sur Bedrock (29/09/2026)

Test de Maxster33 (Bedrock) avec 1.1.3 : ça marche, mais au-delà de 40 niveaux on ne voit ni le coût ni si on a assez
de niveaux. Demande : afficher le nombre de niveaux nécessaire. Choix de Maxster33 : ligne sur l'objet de la 1re case.
- Le jeu Bedrock calcule lui-même l'aperçu du résultat (la ligne ajoutée au résultat par le serveur n'y apparaît pas),
  mais affiche la description des objets d'entrée envoyée par le serveur, et son aperçu copie l'objet de la 1re case.
- Joueurs Bedrock, coût > 39 : « Coût réel : N niveaux » (vert / rouge) ajouté en dernière ligne de l'objet de la
  1re case, avec un marqueur invisible (`ks_enclume:ligne_cout`), au tick suivant le calcul ; retiré si le coût repasse
  sous 40 ou si le résultat disparaît. Visible au survol de l'objet d'entrée et normalement dans l'aperçu du résultat.
- Retrait de la ligne : résultat pris (le résultat donné est calculé sans elle) ; tout clic ou glisser dans une enclume
  (inventaire et curseur du joueur nettoyés au tick suivant) ; fermeture de l'enclume, déconnexion comprise (cases
  d'entrée nettoyées avant que le jeu ne rende ou fasse tomber les objets) ; par sécurité, à la connexion et à
  l'ouverture d'une enclume. Joueurs Java : inchangés (jamais de ligne sur leurs objets).
- Limites : poser l'objet marqué remet à zéro le champ du nom sur Bedrock (Geyser) : taper le nom après avoir posé les
  deux objets. Le coût affiché ne tient pas compte d'un nom tapé sur Bedrock (envoyé au serveur seulement à la prise ;
  le vrai coût est revérifié à ce moment).

**Déployé sur Event le 29/09/2026 à 05:43 (1.1.3 dans `_removed-ks_enclume-1.1.3/`). Statut : non testé en jeu.**

## 1.2.0 - coût réduit au-delà de 50 niveaux (01/10/2026)

Demande de Maxster33 : « l'augmentation du prix en expérience pour la réparation et l'amélioration d'objet sur
l'enclume, soit réduite de moitié à partir du niveau 50 ». Choix de Maxster33 : coût vanilla jusqu'à 50 niveaux, la
partie au-dessus de 50 compte pour moitié (arrondi à l'unité inférieure) : 51 → 50, 70 → 60, 100 → 75, 150 → 100,
250 → 150.
- Appliqué à tout calcul de l'enclume (réparation, fusion, renommage), à la priorité NORMAL du `PrepareAnvilEvent` ;
  la pénalité des réparations successives enregistrée sur l'objet reste vanilla (seul le prix change).
- Fioles de KS_FioleExp : inchangées (KS_FioleExp, priorité HIGH, impose ensuite son propre coût).
- Affichage (Java : ligne « Coût réel », Bedrock : 39 ou « Trop cher ! » + ligne sur l'objet de la 1re case) et
  vérification à la prise : inchangés, ils utilisent le coût réduit (toujours supérieur à 39).

**Déployé sur Event le 01/10/2026 à 19:50 (Maxster33 ; 1.1.4 dans `_removed-ks_enclume-1.1.4/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.3.0 - coût de réparation dans la description des équipements (08/10/2026)

Demande de Maxster33 (après les tests de Kixster) : « ajouter le coût en niveaux nécessaire pour la réparation
directement dans les descriptions des équipements ». Choix de Maxster33 : coût d'une **réparation complète avec le
matériau** de l'objet, sur **tout objet réparable**.

- Dernière ligne (grise) de la description de chaque objet réparable de l'inventaire du joueur (barre, armure,
  seconde main) : « Réparation : 10 niveaux ».
- Calcul (règles vanilla de l'enclume) : pénalité de l'objet (0, 1, 3, 7, 15... selon les passages à l'enclume)
  + 1 niveau par unité de matériau, chaque unité réparant un quart de la solidité maximale, jusqu'à la réparation
  complète ; puis la réduction de la 1.2.0 au-delà de 50 niveaux. Exemple : épée en diamant (1 561 de solidité,
  1 diamant répare 390), pénalité 7, abîmée de 800 : 3 diamants (800 → 410 → 20 → 0), 7 + 3 = **10 niveaux**.
- Objet sans matériau de réparation (arc, trident, canne à pêche, briquet, cisailles...) : « Réparation : au moins
  7 niveaux (par fusion) » (seule la fusion le répare ; la pénalité est le minimum, le reste dépend des enchantements
  de l'autre objet). Objet intact : « Réparation : objet intact ». Objet incassable : pas de ligne.
- Mise à jour chaque seconde (l'objet n'est réécrit que si le texte change), à la connexion et à la fermeture d'un
  inventaire. Rien n'est changé tant qu'un autre inventaire est ouvert (enclume, coffre...), pour ne pas gêner les
  clics ni la ligne « Coût réel » des joueurs Bedrock. La ligne est retrouvée par son texte, gardé dans le marqueur
  `ks_enclume:ligne_reparation` : les autres lignes de la description ne sont jamais touchées.

Choix signalé : l'objet intact affiche « objet intact » (l'enclume refuse de réparer un objet intact) plutôt que sa
pénalité.

Limites :
- Seul l'inventaire des joueurs est mis à jour : un objet rangé dans un coffre garde la ligne de sa dernière mise à
  jour jusqu'à ce qu'un joueur le reprenne.
- Le coût suppose assez de matériau en 2e case (un seul passage à l'enclume) ; renommer en même temps ajoute 1 niveau.
- Une ligne de plus rend l'objet différent pour les plugins qui comparent les objets exactement (ex. boutiques de
  KS_Economy : un équipement mis en vente doit être choisi dans l'inventaire, comme avant).

**Déployé sur Event et Kixster le 08/10/2026 à 21:12 (Maxster33 ; 1.2.0 dans `_removed-ks_enclume-1.2.0/`), actif après
redémarrage. Statut : non testé en jeu.**

## 1.4.0 - enclume en coffre (accroupi + clic) (25/10/2026, LeKiwi06)

Demande de LeKiwi06 : « les joueurs bedrock m'ont dit que l'enclume n'arrive pas à dépasser la limite vanilla de prix
d'xp, ça ne propose pas l'item dans la case de résultat » ; « un joueur bedrock ne peux pas mettre de livre affilage sur
son épée, car leur jeu ne reconnais pas ce livre comme un livre enchanté valide ». Proposition : l'interface de fusion des
coffres de récompense « s'ouvre quand on sneak+click, si on essaie de mettre 2 coffres de récompenses elle fonctionne
comme d'habitude, mais elle peut également marcher comme une enclume normale » ; « il faut par contre veiller a ce que
l'enclume s'use parfois ». Ses réponses aux trois questions de Claude : usure de 12 % à chaque validation, **fusion de
coffres comprise** ; objets **posés dans les cases**, coffres de récompense compris ; menu dans **KS_Enclume**.

Cause des deux bugs : le jeu Bedrock calcule lui-même l'aperçu du résultat de l'enclume (voir 1.1.3 et 1.1.4) ; au-delà de
sa limite, ou pour un livre qu'il ne reconnaît pas, il n'affiche rien et le joueur ne peut rien prendre, même si le
serveur a accepté. Dans un menu en coffre, c'est le serveur seul qui calcule et donne le résultat.

- **Ouverture** : accroupi + clic droit sur une enclume (neuve, ébréchée ou endommagée) : menu « Enclume » (coffre de 3
  rangées). Sans s'accroupir, l'enclume du jeu s'ouvre comme avant (renommage, coûts affichés des 1.1.x). Accroupi avec
  une fiole vide en main : toujours le menu de KS_FioleExp (il passe avant). Un clic refusé par une protection (claim)
  n'ouvre rien.
- **Cases** : un clic sur un objet de son inventaire le pose (toute la pile) dans la 1re case libre (rangée du milieu,
  3e puis 5e case) ; un clic sur un objet posé le reprend. Rien ne passe par le curseur (tous les clics sont annulés, le
  plugin déplace lui-même les objets). À la fermeture (déconnexion et arrêt du plugin compris), les objets posés sont
  rendus ; ce qui ne tient plus dans l'inventaire tombe aux pieds du joueur.
- **Calcul** : par l'enclume du jeu, côté serveur, dans une fenêtre d'enclume jamais ouverte (`MenuType.ANVIL`) : règles
  vanilla (réparation au matériau, fusion de deux objets, livres, enchantements incompatibles, pénalité des passages
  successifs), réduction de la 1.2.0 au-delà de 50 niveaux, **aucun plafond**. Les refus des autres plugins s'appliquent
  (`PrepareAnvilEvent` : coffres de KS_RewardsGUI, fioles de KS_FioleExp). Le nom d'un objet renommé est gardé (sans
  champ de nom, le jeu l'aurait effacé et compté 1 niveau : il est retiré avant le calcul et remis sur le résultat).
- **Aperçu** (7e case du milieu) : l'objet obtenu ; sinon « Aucun résultat » avec la raison. **Valider** (centre de la
  rangée du bas) : « Valider (N niveaux) », vert si le joueur a les niveaux, rouge sinon. Au clic : niveaux retirés
  (gratuit en créatif), le résultat prend la place du 1er objet, la 2e case perd les unités de matériau utilisées (ou est
  vidée), comme dans le jeu. Le résultat reste dans le menu, pour enchaîner.
- **Deux coffres de récompense non ouverts** : fusion de KS_RewardsGUI 1.7.0, comme avant (coût en points
  d'expérience, « Valider (N XP) », nouveau tirage) ; l'aperçu montre le coffre fusionné et sa valeur moyenne. Un coffre
  avec un autre objet : refusé.
- **Jetons et badges de KS_Jetons** : refusés (« Badges : enclume normale (clic) ») : la fusion de deux badges est faite
  par KS_Jetons à la prise du résultat dans l'enclume du jeu ; ici, elle aurait été faite comme une fusion vanilla, avec
  un coût faux.
- **Usure** : à chaque validation (réparation, enchantement ou fusion de coffres), hors créatif, 12 % de chances que
  l'enclume s'abîme, comme dans le jeu (`AnvilMenu.onTake`, lu dans le code du jeu 26.3) : neuve → ébréchée →
  endommagée → détruite (son de l'enclume qui casse ; le menu se ferme, les objets sont rendus). Sens de l'enclume gardé.
- Validation refusée si l'enclume n'existe plus ou si le joueur s'en est éloigné de plus de 8 blocs (le menu se ferme).
- `softdepend: [floodgate, KS_RewardsGUI]` ; `build.sh` compile KS_RewardsGUI d'abord (jamais embarqué). Sans
  KS_RewardsGUI, l'enclume en coffre marche, sans fusion de coffres.

Choix de Claude, signalés :
- les textes du menu sont écrits dans le code (comme le reste de KS_Enclume, qui n'a pas de `lang.yml`) ;
- accroupi + clic sur une enclume avec un bloc en main ouvre le menu au lieu de poser le bloc contre l'enclume ;
- un clic pose toute la pile (le matériau en trop reste dans la case et revient à la fermeture) ;
- pas de renommage dans ce menu (un coffre ne permet pas de taper un texte) : l'aide renvoie à l'enclume normale.

Limites :
- Écrit sans serveur de test : le calcul dans une fenêtre d'enclume jamais ouverte n'a pas été essayé en jeu (en cas
  d'échec, le menu affiche « L'enclume en coffre est indisponible » et l'erreur est écrite une fois dans la console).
- Si le serveur plante (arrêt brutal, pas un arrêt normal) pendant qu'un objet est posé dans le menu, cet objet est perdu
  (choix « posés dans les cases »).
- Un livre au-delà du niveau maximal du jeu (ex. Tranchant VI) est ramené au maximum, comme dans l'enclume du jeu.

**Compilé le 25/10/2026, non déployé, non testé en jeu. À déployer sur Kixster avec KS_RewardsGUI 1.7.0.**
