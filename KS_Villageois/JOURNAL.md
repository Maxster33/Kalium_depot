# KS_Villageois - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - villageois sans échanges (25/09/2026)

Demande de Maxster33 : « les villageois n'ont plus de trade par défaut , aucun moyen de les lvl up etc , seuls les pnj
donnés a l'avenir en auront ».
- `VillagerAcquireTradeEvent` annulé pour les villageois **et les marchands ambulants** (réponse de Maxster33) : ils
  gardent leur métier mais n'ont aucun échange, donc ni expérience ni niveau.
- Un échange déjà présent (entité antérieure au plugin) est retiré quand un joueur interagit avec l'entité.
- Futurs PNJ à échanges : entités portant l'étiquette `ks_pnj` (`/tag <entité> add ks_pnj`), jamais touchées
  (préparé à la demande de Maxster33 ; leurs échanges seront définis dans un futur script).

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**
