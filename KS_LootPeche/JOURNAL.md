# KS_LootPeche - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - pêche sans livres enchantés (25/09/2026)

Demande de Maxster33 : « les livres enchantés sont retirés des loot table » ; réponse : tirer au sort un autre trésor.
- Un livre enchanté pêché est remplacé par un des autres trésors vanilla, à chances égales : arc, canne à pêche
  (tous deux enchantés niveau 30 avec usure aléatoire jusqu'à 25 %, comme en vanilla), étiquette, carapace de
  nautile, selle. Correction de la proposition du 25/09 : le nénuphar n'est pas un trésor (il est dans les déchets).
- Les chances d'obtenir un trésor ne changent pas.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - livres enchantés de retour, sans Raccommodage (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « remet les livre enchantés dans la pêche , mais retire le fait que le mending puisse spawn ».
- Les livres enchantés sont de nouveau pêchés, avec la chance vanilla (plus de remplacement par un autre trésor).
- Raccommodage : un livre, un arc ou une canne à pêche pêché qui l'a est réenchanté comme le fait la pêche vanilla
  (niveau 30, enchantements de trésor compris) jusqu'à ne plus l'avoir ; l'usure tirée par le jeu est gardée. Au bout
  de 50 tirages (jamais atteint en pratique), Raccommodage est simplement retiré.
- Les autres enchantements de trésor (Gel, Malédictions, Vitesse des âmes...) restent possibles, comme en vanilla.
- Didacticiel : texte de « Pêche » mis à jour, marqué Nouveau le 26/10.
- Têtes de mobs marins, tête de wither squelette, fiole sinistre et pomme de Notch : proposition de chances envoyée
  (`sortie/peche/proposition-peche-et-estomac.xlsx`), à coder après validation.

**Compilé, non déployé, non testé en jeu.**
