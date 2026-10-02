# KS_Claim - journal

Plugin du serveur Event : interface des claims, sur SimpleClaimSystem (SCS, moteur). Cahier des charges : catégorie 3
« Claims » de LeKiwi06 (validé le 30/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement). Réglages de SCS :
`scs/README.md`.

## 1.0.0 - claims, groupes, prix, vente (02/10/2026)

- **Ouverture** : `/ksclaims` ou bouton « Claims » du menu d'Event (KS_Menu) : ses claims seulement. `/ksclaim` :
  claime le chunk où se trouve le joueur (prix affiché, confirmation) ; sur son propre claim : ouvre le claim ; sur
  le claim à vendre d'un autre : propose l'achat.
- **1 claim = 1 chunk** (toute la hauteur) ; ni ajout de chunk ni fusion ; overworld, Nether, End (sauf l'île du
  dragon, région WorldGuard). Refusé si le chunk est pris, dans une zone protégée, monde désactivé, 100 claims, solde
  insuffisant ou trop près d'un autre claim (`claim-distance` de SCS).
- **Prix** : n = claims possédés + 1 (achetés compris) ; n <= 10 : gratuit ; sinon 16n + n²/2 points arrondi au-dessus
  (11e : 237, 12e : 264, 50e : 2 050, 100e : 6 600), payé avec le score (KS_Economy). « Prix des claims » : les 5
  prochains prix. Réglages : `claims-gratuits` (10), `claims-max` (100) dans `config.yml`.
- **Suppression** (confirmation) : rembourse le **dernier prix payé** (liste des prix payés de chaque joueur, le dernier
  est retiré), même si ce n'est pas ce claim qui l'a coûté ; aucun prix payé : rien. Un claim acheté à un joueur ne
  s'ajoute pas à cette liste (sinon le supprimer créerait des points).
- **Claim** : nom (16 caractères, lettres sans accents, chiffres, - et _) et description (50) ; **membres** (5 au plus,
  hors propriétaire : ajouter un joueur déjà venu sur le serveur, retirer) ; **bannis** (bannir, débannir, expulser) ;
  **réglages des membres** et **des visiteurs** (cases à cocher, seulement les réglages que `status-settings` de SCS
  autorise) ; **groupe** ; **vendre** (prix de 1 à `max-sell-price` de SCS) / retirer de la vente.
- **Groupes** (nom, un claim dans un seul groupe) : ajouter / retirer des claims, membres et réglages appliqués à tous
  ses claims (5 membres au plus par claim : un claim plein est signalé), renommer, supprimer (les claims restent).
- **Achat** : le joueur se tient dans le claim à vendre et fait `/ksclaim`. Refusé si l'acheteur a 100 claims, ou si le
  prix est inférieur au prix de son prochain claim alors qu'il a déjà 10 claims ou plus (message avec ce minimum).
  L'acheteur paie le vendeur (score) ; SCS renomme le claim (`claim-N`) et garde ses membres (sauf l'ancien
  propriétaire) ; le claim sort du groupe du vendeur.
- Données : `plugins/KS_Claim/donnees.yml` (prix payés, groupes). Textes : `plugins/KS_Claim/lang.yml`. Tous les textes
  de boutons et de champs mesurés : aucun ne défile.
- Opérations de SCS (base de données) hors du fil principal du serveur, puis retour au menu.
- **SCS en français** : `scs/langs/fr_FR.yml` (barre de boss, messages de protection, membres, bannissements ; les
  claims à vendre s'achètent avec `/ksclaim`).
- `depend` SimpleClaimSystem, KLM_Menu, KS_Economy ; `softdepend` KS_Menu ; `build.sh` : compiler KLM_Menu, KS_Menu et
  KS_Economy d'abord ; SCS dans `outils-build/libs` (`telecharger-outils.sh`).

Limites :
- Le premier joueur à créer un claim à côté d'un autre n'est limité que par `claim-distance` de SCS (0 par défaut :
  claims voisins permis).
- Le magasin de KS_Economy (déclaré sur un groupe) viendra avec la 2e partie de la catégorie 2.

**Déployé sur Event le 02/10/2026 à 22:26 (LeKiwi06) avec SimpleClaimSystem 1.13.1 (nouveau, `config.yml` et
`langs/fr_FR.yml` posés avant le premier démarrage), KS_Claim 1.0.0, KS_BiomeChanger 1.1.0 ; actifs après
redémarrage d'Event ; puis commandes LuckPerms et région de l'île du dragon (humain, voir `KS_Claim/scs/README.md`).
Statut : non testé en jeu.**
