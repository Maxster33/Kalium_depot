# KS_Enclume - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - enclume sans plafond de prix (25/09/2026)

Demande de Maxster33 : réparer ou fusionner sans « Trop cher ! », coût calculé comme en vanilla.
- Le plafond (`maximumRepairCost`, 40 niveaux en vanilla) est levé à l'ouverture de l'enclume et à chaque calcul
  (`InventoryOpenEvent`, `PrepareAnvilEvent`). Le coût et la pénalité des réparations successives restent vanilla ;
  le joueur doit toujours avoir assez de niveaux.
- Limite possible : le client Minecraft peut encore afficher « Trop cher ! » au-delà de 40 niveaux (affichage géré par
  le jeu du joueur) alors que le serveur accepte ; à vérifier en jeu.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**
