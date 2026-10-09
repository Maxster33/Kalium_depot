# KS_CoffreMort - journal

Plugin du serveur Event : coffres de mort. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort » de
LeKiwi06 (03/10/2026 et 08/10/2026) : `KS_Jetons/CAHIER_DES_CHARGES.md`.

## 1.0.0 - coffres de mort (08/10/2026, LeKiwi06)

Demande de LeKiwi06 (03/10/2026, précisée le 08/10/2026 ; « commence KS_CoffreMort sans attendre le test » de
KS_Jetons 2.0.0).

- **À la mort** : les objets du joueur vont dans un **coffre de mort** au lieu de tomber. La moitié de son expérience
  est gardée, dans une **fiole d'expérience** rangée dans le coffre (« Fiole d'expérience (N points) » : lancée, elle
  rend exactement ces points) ; l'autre moitié est perdue (aucune orbe). Message avec les coordonnées et la durée.
- **Emplacement** : le bloc d'air ou de fluide le plus proche de la mort (6 blocs autour, `rayon-recherche`) ; dans la
  lave, le coffre remplace le bloc de lave ; dans l'eau, le coffre est inondé et la source revient quand il disparaît ;
  hors du monde (vide de l'End...) : la couche constructible la plus basse, aux coordonnées de la mort.
- **Le coffre** : ouvrable et cassable **par le mort seulement**, pendant **15 minutes** (`duree-minutes`, heure
  réelle : le temps passe aussi serveur éteint ou joueur déconnecté) ; ensuite il disparaît avec son contenu. **3 coffres
  actifs** au plus par joueur (`maximum`) : le 4e supprime le plus ancien, avec un message.
  - Le contenu est gardé par le plugin (`coffres.yml`), le bloc n'est qu'un repère vide : fenêtre de 54 cases où l'on
    ne peut que prendre ; coffre vidé = coffre retiré ; cassé par son propriétaire : le contenu tombe.
  - Protégé des explosions, des créatures, des entonnoirs ; on ne peut pas poser un coffre contre lui (coffre double).
- **Envoi dans /rewards** (KS_RewardsGUI 1.1.0), avec un message, quand la mort a lieu **dans un de ses claims**
  (propriétaire) ou dans une **zone protégée où il ne peut ni casser ni poser** (région WorldGuard ; claim d'un autre
  où il n'a ni « Construire » ni « Casser »), ou quand aucun bloc n'est libre autour. Par lots de 27 piles au plus
  (« Mort du 08/10 20:15 (1/2) ») pour qu'un lot tienne dans un inventaire. Sans KS_RewardsGUI : coffre posé quand même.
- **`/coffres`** (alias `/coffremort`, bouton « Coffres de mort » du menu) : pour chaque coffre actif, monde,
  coordonnées, temps restant, nombre de piles (visibles **sans jeton**, décision du 08/10/2026) ; « Voir le coffre »
  (aperçu, rien ne se prend) ; **« Récupérer le coffre »** : le contenu part dans /rewards et le coffre disparaît,
  après confirmation. Moyen utilisé : le **badge de la mort** s'il est disponible (gratuit, une fois tous les 7 à 1
  jours réels selon son niveau ; délai tenu **par joueur**, changer de badge ne recharge rien), sinon **1 jeton de la
  mort** de l'inventaire spécial (KS_Jetons 2.0.0).
- `depend` KLM_Menu ; `softdepend` KS_Menu, KS_Jetons, KS_RewardsGUI, WorldGuard, SimpleClaimSystem. Aucun objet sur
  livre de connaissances : rien à ajouter dans `geyser-bedrock` (la fiole est une fiole d'expérience vanilla).

Limites, à vérifier en jeu :
- Permissions des claims lues dans SimpleClaimSystem (`getPermissionForPlayer` « Build » et « Destroy ») : à vérifier
  pour un membre et pour un visiteur.
- Si le bloc du coffre est retiré par une commande (WorldEdit, `/setblock`), le coffre reste listé dans `/coffres`
  mais ne peut plus être ouvert : seule la récupération par badge ou jeton reste possible.
- Un opérateur ne peut ni ouvrir ni casser le coffre d'un autre.

**Déployé sur Event le 08/10/2026 à 20:56 (LeKiwi06, avec KS_RewardsGUI 1.1.0 ; nouveau), actif après redémarrage
d'Event. Statut : non testé en jeu.**

## 1.0.1 - /rewards dans les claims des autres, pas dans les siens (08/10/2026, LeKiwi06)

Correction de LeKiwi06 (08/10/2026) : « ce n'est pas quand on meurt dans notre claim que le coffre arrive dans reward,
c'est dans les claims des autres ». La 1.0.0 suivait le texte du 03/10 (« ou dans un de ses claims »).
- Mort **dans un de ses claims** : coffre de mort posé, comme ailleurs.
- Mort **dans le claim d'un autre joueur** (même si l'on en est membre) : contenu envoyé dans `/rewards`, message « Tu
  es mort dans le claim d'un autre joueur : tes objets t'attendent dans /rewards. » (nouvelle clé `mort.claim-autre`).
  Les permissions du claim ne sont plus lues (plus de point à vérifier pour membre et visiteur).
- Inchangé : zone protégée par WorldGuard où il ne peut ni casser ni poser, et aucun bloc libre : `/rewards`.
- « La nether star ne doit pas être dans un coffre de mort » : c'est l'étoile du menu, déjà retirée des objets de la
  mort par KS_Menu 1.2.0 (Maxster33, 08/10/2026, priorité LOW, avant KS_CoffreMort) ; rien à changer ici. Une vraie
  étoile du Nether (butin du Wither) va toujours dans le coffre.

**Déployé sur Event le 08/10/2026 à 23:38 (LeKiwi06, déploiement groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; 1.0.0 dans
`_removed-ks_coffremort-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 1.0.1 ; nouveau sur Kixster), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**

## 1.0.2 - lecture publique pour la modération (09/10/2026, LeKiwi06)

Pour la fiche d'un joueur de KS_AntiCheat 1.3.0 (demande de LeKiwi06 : coffres de mort dans la fiche de modération).
Aucun changement pour les joueurs.
- `KSCoffreMort.coffresActifs(joueur)` : coffres de mort actifs d'un joueur (position, heure de disparition, nombre de
  piles d'objets), en ligne ou hors ligne. Lecture seule : le contenu n'est ni montré ni touché.

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

**Déployé sur Kixster le 09/10/2026 à 18:31 (LeKiwi06, envoi groupé : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 ; accord de LeKiwi06 : « Oui, sur Kixster » ; 1.0.1 dans `_removed-ks_coffremort-1.0.1/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (KS_CoffreMort 1.0.1). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage de 18:36 (09/10/2026) : activé sans erreur, avec KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ; KS_AntiCheat démarre après eux. Personne n'avait encore ouvert la fiche d'un joueur : reste à tester en jeu.**

## 1.0.3 - coffre de mort gardé 1 heure (09/10/2026, LeKiwi06)

Demande de LeKiwi06 (09/10/2026, 23 h 15) : « on va aussi étendre la longueur des coffres de morts à 1h au lieu de 15
minutes ». Cause : sur Kixster, Maaxster est mort à 21:37 loin du spawn (36 piles), s'est déconnecté à 21:38, et son
coffre a disparu à 21:52 avec tout son contenu.
- `duree-minutes` vaut **60** par défaut (code et `config.yml` fourni) au lieu de 15. Aucun autre changement ; les
  messages affichent la durée lue dans le réglage (« il disparaît dans 60 minutes »).
- Le `config.yml` déjà présent sur un serveur garde sa valeur : il faut y mettre `duree-minutes: 60` à la main.
- Un coffre déjà posé garde l'heure de disparition calculée à la mort (15 minutes) ; seuls les coffres posés après
  le redémarrage durent 1 heure.

**Déployé sur Kixster le 09/10/2026 à 23:25 (LeKiwi06 ; accord : « Oui, les deux » ; 1.0.2 et l'ancien `config.yml` dans `_removed-ks_coffremort-1.0.2/` ; `config.yml` du serveur passé à `duree-minutes: 60` ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (1.0.1, 15 minutes ; décision de LeKiwi06 : « Non, Kixster seulement »). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage du 09/10/2026 à 23:53 : 1.0.3 activée sans erreur ; `duree-minutes: 60` relu dans le `config.yml` du serveur. Reste à tester en jeu.**
