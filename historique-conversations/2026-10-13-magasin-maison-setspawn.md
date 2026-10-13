# 2026-10-13 — Magasin à tiers, /setspawn, claim de la maison

- Plugin(s) concerné(s) : KS_Economy, KS_Teleport, KS_Claim
- Versions avant / après : KS_Economy 1.8.2 / 1.9.0 ; KS_Teleport 1.2.0 / 1.3.0 ; KS_Claim 1.3.0 / 1.4.0. Les trois
  compilées ; déployées sur Kixster le 13/10/2026 à 16:30 (Event inchangé) ; non testées en jeu.

## Demandé

LeKiwi06 : « plusieurs problèmes : le fait de faire maison create ne rend pas le claim (source : maaxster, vérifier les
logs) ; le prix du magasin create est de 45000, il doit passer à 450 ; le prix du magasin agrandir est calculé tel que
x est le nombre d'agrandissement : 10x²+450x-50 ; le nombre maximum d'agrandissement est de 3 ; le premier accorde
2 slots de boutique supplémentaires, le deuxième 3, le troisième 5 ; on doit normalement arriver à un magasin tier 4
avec 20 slots de boutique ; on ne peut créer qu'une boutique par joueur ; on ne peut créer qu'une seule maison ; on doit
tenir en main la boussole magnétisée de la lodestone pour notre tp de maison et faire /setspawn, ce qui consomme la
boussole et met notre point de /spawn au-dessus de la lodestone ».

## Fait

- Journal de Kixster lu en lecture seule (copie hors dépôt) : Maaxster, `/maison create` le 13/10 à 15:47:56, maison
  déclarée pour 500 points à 15:48:02, sans erreur ; puis `/magasin create` (aucun magasin créé), `/maison`,
  `/maison position`. Le plugin ne faisait rien du claim.
- Quatre questions posées avant le code (voir Décisions).
- KS_Economy 1.9.0 : création à 450 points ou 5 blocs tier 1 ; 3 agrandissements au plus (410, 890, 1 390 points ;
  +2, +3, +5 boutiques) ; tier affiché. Détail : `KS_Economy/JOURNAL.md`.
- KS_Teleport 1.3.0 : `/setspawn` ; plus de `/maison position` ; magnétite du point de `/spawn` indestructible.
  Détail : `KS_Teleport/JOURNAL.md`.
- KS_Claim 1.4.0 : le claim qui porte la maison ne compte plus. Détail : `KS_Claim/JOURNAL.md`.
- Données et réglages des deux serveurs lus (hors dépôt) : pas de section `magasins` dans les `config.yml` (rien à
  modifier à la main) ; Kixster : aucun magasin, une maison (Maaxster), aucun claim payé ; Event : un magasin sans
  agrandissement, aucune maison.

- Sur « Kixster seulement » (LeKiwi06) : les trois jars envoyés sur Kixster à 16:30 (jars en place vérifiés identiques aux
  références avant, jars envoyés vérifiés après ; anciens jars dans `_removed-…`). Actifs après redémarrage.
  Réservations libérées.

## Décisions

Choix de LeKiwi06 (réponses aux questions) :

- « Rendre le claim » : le claim qui porte la maison ne compte plus dans les claims du joueur, maison seulement
  (proposés aussi : maison et magasin ; rembourser le prix du claim).
- `/setspawn` remplace `/maison position` ; `/maison create` garde l'endroit où l'on se tient (proposés aussi :
  `/setspawn` obligatoire après la création ; garder les deux commandes).
- La magnétite du point de `/spawn` est indestructible (proposé aussi : cassable, `/spawn` refusé ensuite).
- Création du magasin payable aussi avec 5 blocs tier 1 (proposés aussi : points seulement ; laisser 5 blocs tier 3).

Choix de Claude, signalés :

- « une boutique par joueur » lu comme un magasin par joueur (déjà le cas, comme une maison par joueur).
- Tier du magasin affiché dans son menu.
- Un agrandissement demande toujours un autre claim de la zone (non demandé, donc non changé).
- Magnétite de nouveau cassable si son claim n'est plus au joueur.
- Supprimer le claim de la maison ne rembourse rien.
- `/setspawn` sans confirmation.

## Reste à faire

- Redémarrer Kixster (par l'humain) pour activer les trois plugins.
- Event : resté aux anciennes versions, à décider par LeKiwi06 (KS_Teleport 1.3.0 et KS_Claim 1.4.0 ensemble).
- Tester en jeu : `/magasin create` à 450, les trois agrandissements, `/setspawn` (Java et Bedrock), magnétite
  incassable, claim gratuit rendu dans `/ksclaims`.
