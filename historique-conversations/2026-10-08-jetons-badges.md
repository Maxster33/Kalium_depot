# 2026-10-08 — Jetons, badges et coffres de mort (catégorie 7)

- Plugin(s) concerné(s) : KS_Jetons, KS_KaliumGive, geyser-bedrock, KS_CoffreMort, KS_RewardsGUI
- Versions avant / après : KS_Jetons 1.0.0 / 2.0.0 ; KS_KaliumGive 1.8.0 / 1.9.0 (déployés sur Event le 08/10/2026 à
  19:09, non testés) ; geyser-bedrock 1.2.0 / 1.3.0 (envoyé sur le proxy le 08/10/2026 à 19:40, non testé)

## Demandé

LeKiwi06 : « liste des derniers ajouts et modifications pour jetons et badges de Kixster SMP » : un inventaire spécial
pour les jetons et les badges ; jetons à l'apparence d'un lingot (fly : fer ; mort : netherite ; téléportation : or ;
localisation : cuivre) ; jeton de fly (10 minutes de vol dans ses claims, /rewards seulement) ; le jeton de la mort
récupère le coffre de mort au lieu d'en donner les coordonnées ; jeton de localisation crafté avec du cuivre ciré ;
badges (versions améliorées des jetons, /rewards et events seulement, fusion de 2 badges du même niveau à l'enclume, un
seul par type, apparence du bloc du lingot, capacité non cumulable) avec leurs niveaux. Texte complet et réponses :
`KS_Jetons/CAHIER_DES_CHARGES.md`, parties 6 à 10.

## Fait

- Cahier des charges de la catégorie 7 complété (parties 6 à 11), publié dans `KS_Jetons/CAHIER_DES_CHARGES.md`.
- KS_Jetons 2.0.0 : jetons en lingots, badges à niveaux, inventaire spécial en deux contenants à l'interface
  d'entonnoir, fusion à l'enclume, reprise des données de la 1.0.0. KS_KaliumGive 1.9.0 : ids des jetons et des badges.
- Déployés sur Event le 08/10/2026 à 19:09 (anciens jars et ancien `config.yml` dans `_removed-…`).
- Demande suivante de LeKiwi06 : « tous les objets customs doivent figurer dans le proxy pour que les joueurs Bedrock
  les voient » : `geyser-bedrock` 1.3.0 (5 jetons, 4 badges), envoyé sur le proxy à 19:40.

- Demande suivante : « commence KS_CoffreMort sans attendre le test », puis « oui déploie les deux sur Event » :
  KS_CoffreMort 1.0.0 (nouveau) et KS_RewardsGUI 1.0.1 / 1.1.0, déployés sur Event à 20:56, non testés.

- Corrections de LeKiwi06 (21 h) : l'étoile du menu jamais dans un coffre de mort (déjà fait par KS_Menu 1.2.0 de
  Maxster33) ; 20 émeraudes = jeton de téléportation (pas le jeton de claim) ; /rewards dans le claim d'un autre (pas
  dans les siens) : KS_CoffreMort 1.0.1, KS_Jetons 2.0.1.
- « ne déploie pas encore, on va s'occuper des autres plugins et tout déployer ensemble » : codés et compilés, **non
  déployés** : KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1 (région `zone_spawn`), KS_Claim 1.2.0 (jetons de
  claim), avec KS_CoffreMort 1.0.1 et KS_Jetons 2.0.1.

## Décisions

- Badge porté = rangé dans l'inventaire spécial ; délais attachés au joueur.
- Interface d'entonnoir (5 cases) pour les badges et pour les jetons ; cases en trop bloquées par une barrière.
- Fusion : 30, 50, 70, 90, 110, 130 niveaux.
- Jeton d'emplacement supprimé ; badge de localisation ajouté (niveau N = N emplacements en plus de `/home bed` et
  `/spawn`) ; « jeton de localisation » remplace « point de transport ».
- Jeton de la mort : /rewards seulement ; jeton de claim : 20 émeraudes ; jeton de téléportation : prix à fixer.
- Fly : claims dont on est propriétaire ou membre ; barre de boss ; dégâts de chute conservés ; badge avant jeton.
- Coffres de mort : coordonnées et temps restant visibles sans jeton ; jeton et badge envoient le contenu dans /rewards.
- `/spawn` : vers sa maison, après `/maison create` (500 émeraudes) dans un claim de la région `zone_spawn` (ex
  `zone_shop`) ; téléportation entre dimensions permise si la dimension est ouverte.
- Code et tests sur Event d'abord, puis Kixster.
- KS_CoffreMort (choix de Claude, signalés à LeKiwi06) : contenu gardé par le plugin (bloc vide, 54 cases) ; lots de
  27 piles dans /rewards ; 15 minutes en heure réelle ; fiole d'expérience propre au plugin ; objets d'un coffre de mort
  non suivis par l'anti-triche.
- Signalé par Claude, gardé tel que demandé : à 20 émeraudes, le jeton de claim coûte moins qu'un 11e claim (237).

## Reste à faire

- **Déploiement groupé sur Event** (accord de LeKiwi06 à demander) : KS_Jetons 2.0.1 (+ `config.yml` du serveur),
  KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; région WorldGuard `zone_spawn`
  à créer ou renommer par l'humain ; réservations à libérer ensuite.

- Redémarrer Event et le proxy (LeKiwi06), puis tester : `/jetons`, les deux contenants, `/kaliumgive <pseudo> badge_fly_1 2` et la
  fusion à l'enclume (Java et Bedrock).
- Tester KS_CoffreMort : mourir (coffre, coordonnées, fiole), dans un de ses claims (/rewards), `/coffres`,
  récupération avec `jeton_mort` et `badge_mort_1`, claim d'un autre (membre, visiteur).
- Coder KS_Fly, KS_Teleport, puis les jetons de claim dans KS_Claim.
- `zone_shop` → `zone_spawn` (WorldGuard et `magasins.region` de KS_Economy) ; `rachats.csv`.
- Niveau maximal du badge de localisation (5 par défaut) ; prix du jeton de téléportation.
