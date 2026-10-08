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

**À déployer avec KS_RewardsGUI 1.1.0 (et KS_Jetons 2.0.0, déjà sur Event).** Compilé le 08/10/2026 ; **non déployé**.
