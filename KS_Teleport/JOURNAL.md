# KS_Teleport - journal

Plugin du serveur Event : téléportations. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort » de
LeKiwi06 (03/10/2026 et 08/10/2026) : `KS_Jetons/CAHIER_DES_CHARGES.md`.

## 1.0.0 - /home, /spawn, /maison, jetons de localisation (08/10/2026, LeKiwi06)

Demande de LeKiwi06 (03/10/2026, précisée le 08/10/2026).

- **Destinations** : son lit (`/home bed`, par défaut) ; sa **maison du spawn** (`/spawn`) ; les **emplacements** du
  badge de localisation (niveau N : N emplacements). `/home` (bouton « Téléportations » du menu) : téléportations
  gratuites disponibles, jetons, destinations.
- **Téléportation gratuite** : 1 par heure (`delai-minutes`), délai lancé par son utilisation. Le **badge de
  téléportation** en stocke 1 à 5 de plus, **chacune avec son propre délai d'une heure** (délais tenus par joueur).
- **Ensuite** : un **jeton de téléportation** de l'inventaire spécial ; sans jeton : proposition de **payer le prix
  d'un jeton** (20 points, `prix.tp` de KS_Jetons), débit puis départ. Sans badge, un jeton relance le délai de la
  téléportation gratuite à 1 heure ; avec un badge, les délais des téléportations stockées ne sont pas touchés.
  Rien n'est payé tant que le joueur n'est pas parti.
- **Avant de partir** : 5 secondes sans bouger (`attente-secondes`), annulées par un dégât.
- **Entre dimensions** : permis si la dimension est ouverte (portails du Nether / de l'End de KS_Dimensions) ; rester
  dans une dimension où l'on est déjà est toujours permis.
- **Maison du spawn** : `/maison create` dans **un de ses claims** (propriétaire) de la région WorldGuard
  **`zone_spawn`** (`maison.region`), **500 points** (`maison.prix`), confirmation ; elle donne `/spawn`, qui emmène
  là où l'on se tenait. `/maison position` la replace (gratuit, mêmes conditions), comme `/magasin position`. Si
  l'endroit n'est plus dans un de ses claims de la zone, `/spawn` est refusé jusqu'à `/maison position`.
- **Jeton de localisation** : recette en forme, boussole au centre et 8 blocs de cuivre ciré autour (les 4 stades
  d'oxydation, pas les variantes taillées). Le résultat n'apparaît que si la boussole est liée à une magnétite qui
  existe encore ; il porte ses coordonnées (dans sa description). Objet qu'on peut donner.
  - Clic droit : confirmation, puis la magnétite est enregistrée dans le premier emplacement libre et le jeton est
    utilisé. Refusé si la magnétite a été cassée, si elle est déjà dans ses emplacements, sans badge de localisation,
    ou sans emplacement libre.
  - Arrivée : au-dessus de la magnétite (refusé si la place est bouchée ou si la magnétite n'existe plus).
  - **Libérer un emplacement** (`/home`, confirmation) : le jeton n'est pas rendu ; l'emplacement ne resservira que
    **15 minutes** plus tard (`emplacement-libere-minutes`).
  - Badge retiré ou de niveau plus bas : les emplacements en trop restent enregistrés (magnétite toujours protégée)
    mais ne servent plus tant que le niveau ne revient pas.
- **Magnétite indestructible** tant qu'au moins un joueur l'a dans un emplacement : ni cassée (message), ni soufflée
  par une explosion, ni poussée par un piston, ni changée par une créature ; de nouveau cassable quand le dernier
  joueur la libère.
- `depend` KLM_Menu, KS_Jetons ; `softdepend` KS_Menu, KS_Economy, KS_Dimensions, WorldGuard, SimpleClaimSystem.
  Données : `plugins/KS_Teleport/teleport.yml`. Aucun nouvel objet : le jeton de localisation est déjà dans
  `geyser-bedrock` 1.3.0.

Choix de Claude, à confirmer par LeKiwi06 : le jeton de localisation est consommé à l'enregistrement et n'est pas
rendu quand on libère l'emplacement ; la maison est l'endroit exact où l'on se tient à `/maison create` ;
`/maison position` (non demandé) ; `/home` sans argument ouvre le menu.

Limites, à vérifier en jeu : la recette avec une boussole liée (Java et Bedrock) ; `/home bed` avec une ancre de
réapparition ; commandes `/home` et `/spawn` d'un autre plugin éventuel.

**Déployé sur Event le 08/10/2026 à 23:38 (LeKiwi06, déploiement groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; nouveau), actif après redémarrage d'Event.
**Région WorldGuard `zone_spawn` à créer ou renommer par l'humain** (sans elle : ni `/maison create` ni `/spawn`).
Statut : non testé en jeu.**

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 1.0.0 ; nouveau sur Kixster ; **région WorldGuard `zone_spawn` à créer par l'humain** (sans elle : ni `/maison create` ni `/spawn`)), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**

## 1.0.1 - lecture publique pour la modération (09/10/2026, LeKiwi06)

Pour la fiche d'un joueur de KS_AntiCheat 1.3.0 (demande de LeKiwi06 : ses /home, s'y téléporter). Aucun changement
pour les joueurs.
- `KSTeleport.destinations(joueur)` : maison du spawn et emplacements de localisation d'un joueur (libellé et position
  d'arrivée), en ligne ou hors ligne. Lecture seule ; le lit n'y est pas (il se lit dans Paper).

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

**Déployé sur Kixster le 09/10/2026 à 18:31 (LeKiwi06, envoi groupé : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 ; accord de LeKiwi06 : « Oui, sur Kixster » ; 1.0.0 dans `_removed-ks_teleport-1.0.0/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (KS_Teleport 1.0.0). Statut : non testé en jeu.**
