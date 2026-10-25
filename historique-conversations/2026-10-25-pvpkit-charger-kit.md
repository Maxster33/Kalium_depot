# 2026-10-25 — PvP Kit : charger un kit dans son inventaire

- Plugin(s) concerné(s) : KG_PvpKit.
- Versions avant / après : KG_PvpKit 1.1.0 → 1.2.0.

## Demandé

« peux tu rajouter une option dans la gestion des kits du pvp kits pour importer le kit dans mon inventaire histoire
que je puisse le modifier facilement »

## Fait

- KG_PvpKit 1.2.0 : bouton « Charger dans mon inventaire » dans la fiche d'un kit (Gérer les kits › un kit). Le kit
  complet remplace l'inventaire ; on le modifie puis « Remplacer par mon inventaire actuel » l'enregistre.
- Compilé (`sortie/KG_PvpKit-1.2.0.jar`), non déployé.

## Décisions

- Choix de Claude, à juger : page de confirmation seulement si l'inventaire contient autre chose que les objets
  verrouillés ; objets verrouillés (hub de KalGames, boussole de KLM_Menu) gardés ; kit chargé sans déclassement.

## Reste à faire

- Déployer sur Kanvas (espace Kal-Games) et redémarrer (humain), puis tester : chargement inventaire vide, confirmation
  inventaire plein, objets du hub gardés, modification puis « Remplacer par mon inventaire actuel ».
