# KS_Teleport - journal

Plugin du serveur Event : téléportations. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort » de
LeKiwi06 (03/10/2026 et 08/10/2026) : `KS_Jetons/CAHIER_DES_CHARGES.md`.

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

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

## 1.5.0 - moi de test : maison et téléportations gratuites séparées (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- Fiche du joueur (maison du spawn, téléportations gratuites) par `CompteTest.de` ; jetons et emplacements lus dans KS_Jetons 2.2.0 (compte de test). `/home` dans le monde de test : emplacements du compte de test seulement.
- Inchangé : `/spawn` et `/bed` mènent en survie (sortie du monde de test, inventaires échangés par KS_MondeTest).

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
