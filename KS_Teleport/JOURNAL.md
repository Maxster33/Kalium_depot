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

**Journal de Kixster lu après le redémarrage de 18:36 (09/10/2026) : activé sans erreur, avec KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ; KS_AntiCheat démarre après eux. Personne n'avait encore ouvert la fiche d'un joueur : reste à tester en jeu.**

## 1.1.0 - un nouveau joueur arrive au point d'apparition du monde (11/10/2026, LeKiwi06)

Constat fait dans les journaux de Kixster (09/10 au 11/10/2026) : les 6 nouveaux comptes sont apparus entre Y 118 et
Y 123 au-dessus du spawn protégé, alors que le point d'apparition du monde est en -54 73 212 ; deux sont morts de
chute dans la minute (LeKiwi06 en 12 s le 09/10, .TomHeroes57 en 52 s le 10/10). Accord de LeKiwi06 : « oui fais les
deux » (avec l'alias `/reward` de KS_RewardsGUI 1.4.2).

- **Cause** : pour un nouveau joueur, le jeu ne garde du point d'apparition du monde que la colonne (et ses voisines,
  dans le rayon d'apparition) et pose le joueur sur le bloc le plus haut : ici le toit du spawn. La règle de jeu du
  rayon ne change pas la hauteur (`/gamerule spawnRadius 0`, tapée le 09/10, a d'ailleurs été refusée : le nom n'existe
  plus en 26.3).
- **Correction** : à sa première connexion (`hasPlayedBefore()` faux), le joueur est posé au point d'apparition exact du
  monde principal (centre du bloc, orientation du point), s'il y a la place de s'y tenir (2 blocs libres). Sinon il
  reste là où le jeu l'a posé et une ligne le dit dans la console.
- Le point se règle avec la commande du jeu `/setworldspawn` (position et orientation). Aucun nouveau réglage, aucune
  nouvelle commande, aucun nouveau texte.
- Écart avec la proposition faite à LeKiwi06 (« envoyer les nouveaux joueurs au point de `/spawn` ») : ce point
  n'existe pas, `/spawn` mène à la maison du spawn de chaque joueur. Le point d'apparition du monde le remplace
  (choix de Claude, signalé).
- Non fait, signalé : un joueur qui meurt **sans lit** réapparaît par la même règle du jeu, donc sans doute sur le
  toit lui aussi (non vérifiable dans les journaux). Les connexions suivantes ne sont pas concernées (le joueur revient
  où il s'est déconnecté).
- Empilé sur la 1.0.1 et la 1.0.0, jamais testées en jeu, à la demande de LeKiwi06.

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; 1.0.1 dans `_removed-ks_teleport-1.0.1/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.0.0). Statut : non testé en jeu.**

## 1.2.0 - réapparition sans lit au point d'apparition du monde (11/10/2026, LeKiwi06)

Signalé avec la 1.1.0 ; LeKiwi06 : « pour le toit du spawn je ne sais pas comment fix le soucis ». Un joueur qui meurt
sans lit (ou revient de l'End sans lit) est posé par le jeu comme un nouveau joueur : sur le bloc le plus haut de la
colonne, donc sur le toit du spawn de Kixster.
- Réapparition sans lit ni ancre de réapparition, dans le monde principal : le joueur réapparaît au point d'apparition
  exact du monde (`/setworldspawn`), comme un nouveau joueur depuis la 1.1.0. Avec un lit ou une ancre : inchangé.
- Si le point est bouché (moins de 2 blocs libres) : le joueur reste où le jeu le pose, une ligne le dit dans la console.
- Aucun nouveau réglage, aucun nouveau texte.

**Déployé sur Kixster et sur Event le 11/10/2026 à 10:47 (LeKiwi06 ; demandes du 11/10/2026 ; 1.1.0 dans `_removed-ks_teleport-1.1.0/` sur Kixster, 1.0.0 dans `_removed-ks_teleport-1.0.0/` sur Event), actif après redémarrage. Statut : non testé en jeu.**

## 1.3.0 - /setspawn avec une boussole liée à une magnétite ; le claim de la maison est rendu (13/10/2026, LeKiwi06)

Demande de LeKiwi06 : « on ne peut créer qu'une seule maison ; on doit tenir en main la boussole magnétisée de la
lodestone pour notre tp de maison et faire /setspawn, ce qui consomme la boussole et met notre point de /spawn au-dessus
de la lodestone » et « le fait de faire maison create ne rend pas le claim (source : Maaxster, vérifier les logs) ».

Journal de Kixster lu (13/10/2026) : Maaxster a fait `/maison create` à 15:47:56, « Maaxster déclare sa maison du spawn
pour 500 points » à 15:48:02, sans erreur ; puis `/magasin create` (aucun magasin créé), `/maison` et
`/maison position`. Rien n'était prévu pour le claim : c'est ajouté ici.

- **`/setspawn`** (nouvelle commande) : une **boussole liée à une magnétite** dans la main. La boussole est
  **consommée** et le point de `/spawn` passe **au-dessus de la magnétite**. Conditions : avoir déjà sa maison
  (`/maison create`) ; la magnétite existe encore et se trouve dans **un de ses claims de la zone du spawn**
  (`maison.region`). Refus, sans consommer la boussole : pas de maison, pas de boussole liée en main, magnétite hors de
  ses claims de la zone, magnétite cassée, point déjà sur cette magnétite. Pas de confirmation.
- **`/maison position` n'existe plus** (choix de LeKiwi06 : « Remplace /maison position ») : `/setspawn` est la seule
  façon de déplacer le point. **`/maison create` ne change pas** : 500 points, enregistre l'endroit où l'on se tient,
  qui reste le point de `/spawn` jusqu'au premier `/setspawn` (la maison de Maaxster garde donc son point).
- **Une seule maison par joueur** : déjà le cas depuis la 1.0.0, relu dans le code ; le texte le dit maintenant
  (« Tu as déjà une maison au spawn (une seule par joueur). »).
- **Magnétite indestructible** (choix de LeKiwi06) tant qu'elle sert de point de `/spawn` : comme celles des
  emplacements (ni cassée, ni soufflée, ni poussée par un piston, ni changée par une créature). Elle redevient cassable
  quand le joueur refait `/setspawn` ailleurs. Choix de Claude, signalé : elle redevient cassable aussi si son claim
  n'est plus à ce joueur (supprimé ou vendu), pour qu'un terrain ne garde pas un bloc incassable ; `/spawn` est alors
  refusé de toute façon.
- **`/spawn`** avec un point placé par `/setspawn` : arrivée au-dessus de la magnétite, refusée si la magnétite n'existe
  plus ou si la place est bouchée (2 blocs libres), comme pour un emplacement.
- **Le claim de la maison est rendu** (choix de LeKiwi06 : « Ne compte plus (maison) ») : le claim qui porte la maison
  ne compte plus dans les claims du joueur. Fait dans **KS_Claim 1.4.0**, qui lit `KSTeleport.maisonDansSonClaim(joueur)`
  (la maison, si elle est dans un de ses claims de la zone du spawn ; sinon rien). Le claim du magasin n'est pas
  concerné.
- Données : `joueurs.<uuid>.maison-magnetite` (monde, x, y, z) dans `teleport.yml`. `plugin.yml` : commande `setspawn`.
- **Textes** (nouvelles clés de `lang.yml`) : `maison.perdue-setspawn`, `maison.aide-setspawn`, `maison.deja-setspawn`,
  `maison.creee-setspawn`, `setspawn.boussole`, `setspawn.lieu`, `setspawn.cassee`, `setspawn.deja`, `setspawn.fait`.
- Limites, à vérifier en jeu : écrit sans serveur de test ; boussole liée tenue par un joueur Bedrock ; aucun autre
  plugin de Kixster ne déclare `/setspawn` (liste des plugins lue dans le journal), non vérifié sur Event.
- Empilé sur les versions 1.0.0 à 1.2.0, jamais testées en jeu (demande de LeKiwi06).

À déployer ensemble : KS_Teleport 1.3.0 et KS_Claim 1.4.0 (KS_Claim 1.4.0 appelle une méthode absente de KS_Teleport
1.2.0 : erreur à l'ouverture du menu des claims).

**Déployé sur Kixster le 13/10/2026 à 16:30 (LeKiwi06, envoi groupé : KS_Economy 1.9.0, KS_Teleport 1.3.0, KS_Claim 1.4.0 ; accord de LeKiwi06 : « Kixster seulement » ; jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, jars envoyés vérifiés identiques à ceux compilés après l'envoi ; 1.2.0 dans `_removed-ks_teleport-1.2.0/` ; supprimables par l'humain : `_removed-ks_teleport-1.0.0/`, `1.0.1/` ; `config.yml` du serveur non modifié), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.2.0). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage du 13/10/2026 à 16:36 (demande de LeKiwi06) : KS_Economy 1.9.0, KS_Teleport 1.3.0 et KS_Claim 1.4.0 chargés et activés sans erreur ni avertissement (KS_Teleport avant KS_Claim), 49 plugins, démarrage en 15 s. Aucun joueur ne s'était encore connecté : aucune commande (`/magasin`, `/setspawn`, `/ksclaims`) n'a tourné, reste à tester en jeu.**

## 1.4.0 - emplacements dans l'inventaire spécial, délai de 24 h, /bed (18/10/2026, LeKiwi06)

Demandes de LeKiwi06 : « /bed (alias de /home bed) » ; l'inventaire spécial en un seul coffre avec des cases de
localisation (KS_Jetons 2.1.0), un jeton posé = une destination, 24 h d'attente pour une case vidée.

- **/bed** : comme /home bed.
- **Emplacements** = les cases de localisation de l'inventaire spécial (KS_Jetons 2.1.0) : un jeton de localisation
  posé dans la case N est l'emplacement N. Plus d'enregistrement par clic droit : un clic droit avec le jeton en main
  ouvre l'inventaire spécial, avec un message. Plus de « Libérer l'emplacement » dans /home (le jeton n'était pas
  rendu) : on reprend le jeton dans sa case (il est rendu, la case attend 24 h).
- **Vérification** au moment de poser (demandée par KS_Jetons) : jeton lié à une magnétite, magnétite toujours là,
  pas déjà dans une autre case du joueur.
- **/home** : liste des emplacements tirée des cases (libre, en attente avec le temps restant, inactif si le badge est
  trop bas), texte « pour ajouter ou libérer un emplacement... » et bouton « Mes localisations » (ouvre l'inventaire
  spécial ; choix de Claude, signalé).
- **Magnétites indestructibles** : celles des jetons posés dans les cases de tous les joueurs (et toujours les points de
  /spawn), recalculées à chaque changement.
- **Reprise** au premier démarrage : chaque emplacement enregistré avant (teleport.yml) devient un jeton de localisation
  de la même magnétite, posé dans la même case ; case déjà prise : jeton rendu au joueur. teleport.yml n'écrit plus
  d'emplacements ensuite (garder l'ancien fichier au déploiement). Les délais de 15 minutes en cours sont oubliés.
- `emplacement-libere-minutes` n'est plus lue (le délai est `localisation-delai-heures` de KS_Jetons).

Limites : écrit sans serveur de test. Empilé sur la 1.3.0, jamais testée en jeu (demande de LeKiwi06).

**À déployer avec KS_Jetons 2.1.0.** Compilé le 18/10/2026 (`sortie/KS_Teleport-1.4.0.jar`). **Statut : non déployé,
non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:44 (LeKiwi06 : « Envoie les cinq plugins sur Event pour que je teste, Kixster ensuite » ; envoi groupé : KS_Jetons 2.1.0, KS_Teleport 1.4.0, KS_Claim 1.5.0, KS_Economy 1.10.0, KS_Didacticiel 1.1.0 ; jars en place vérifiés de la même taille que `jars-deployes/` avant l'envoi, nouveaux jars de la même taille que ceux compilés ; ancien jar dans `_removed-ks_teleport-1.2.0/`), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.** `teleport.yml` du serveur vide : aucun emplacement à reprendre. Event reçoit aussi la 1.3.0 (/setspawn).

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage.**
