# 2026-09-25 — Allègement du Bingo, déploiements, course de bateau (KG_BoatRace)

- Plugins concernés : KG_BingoGame, KalGames, KG_BoatRace (nouveau), KG_ScoreBoards, (KG_Menu, KLM_Menu, KG_Bingo :
  déploiement seulement).
- Versions avant / après : KG_BingoGame 0.5.0 → 0.6.0 ; KalGames 1.15.1 → 1.18.0 ; KG_BoatRace 1.0.0 → 1.4.1 ;
  KG_ScoreBoards 1.1.0 → 1.4.0 ; KG_Menu 1.0.0, KLM_Menu 2.1.0 (Kal-Games), KG_Bingo 1.3.0 déployés.

## Demandé
- « Les serveurs ont du mal à tenir » : alléger la génération des maps du Bingo, pouvoir faire plusieurs parties
  simultanées sans crash (« pas grave si la game met 5 minutes à se lancer, on veut que ce soit fluide »).
- Déployer ce qui était fini, vérifier la migration de Maxster33 (Velocity, secrets de transfert).
- Annonce promo et patch note de la bêta ouverte (ouverture officielle le 25/09 à 21 h).
- Course de bateau : Grand Prix 40 tours, fantôme, seuil 45 s, anti-collision, plus de checkpoints, placement en
  temps réel et écarts, temps par checkpoint pour de futurs graphiques / bot Discord, vitesse en km/h ; barème de
  points ; abandon et reconnexion. Parkour : chrono rechargé, barème, contre-la-montre, anti-collision.
- Architecture : KG_Instances, KLM_Hub + WorldGuard, KG_Menu chargeur d'interfaces, KG_ScoreBoards central, charte,
  KLM_Relay, menus inter-serveurs, KG_AntiCheat.
- Guide pour que le Claude de Maxster33 travaille plus vite (commandes plutôt que captures d'écran).

## Fait
- Voir le compte rendu « 2026-09-25 — LeKiwi06 » de `REPRISE_PROJET.md` et les JOURNAL.md des plugins.

## Décisions
- Voir `KG_BoatRace/CAHIER_DES_CHARGES.md`, `KG_Parkour/CAHIER_DES_CHARGES.md` et `ARCHITECTURE_CIBLE.md`.
- Hors-piste : contrôle sous le joueur seulement (demande de LeKiwi06 après test), contact latéral seulement s'il
  ralentit. Anti-collision : copies visuelles (Java : coque + tête + pseudo ; Bedrock : porte-armure tête + pseudo).
- Points décimaux : affichage sans « K » sous un million (6 chiffres), « M » / « Md » ensuite.

## Reste à faire
- Changer le secret de transfert Velocity ; capturer la nouvelle salle d'attente du Bingo.
- KG_BoatRace étapes 4, 5, 6 ; KG_Parkour ; architecture cible.
