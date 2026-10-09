# 2026-10-09 — Rachats de la semaine : objets custom affichés en étoile du Nether

- Plugin(s) concerné(s) : KS_Economy
- Versions avant / après : 1.5.0 / 1.5.1 (compilée, non déployée)

## Demandé

LeKiwi06 : « les items dans le rachat de la semaine sont buggué quand c'est des items custom on voit une nether
star ».

## Fait

- Cause : `Rachats.icone` (1.5.0, menus en coffres) mettait une étoile du Nether pour tout objet custom du barème
  (têtes, élixirs, spawners, fioles d'expérience, Estomac du gardien...).
- KS_Economy 1.5.1 : l'objet custom est créé par KS_KaliumGive (mêmes id que `/kaliumgive`), sans la marque de son
  plugin ; bloc de charbon de bois montré par un bloc de charbon. Détail : `KS_Economy/JOURNAL.md`.
- Compilé (`sortie/KS_Economy-1.5.1.jar`), poussé. Non déployé.

## Décisions

- Choix de Claude signalés : marque du plugin retirée de l'objet du menu ; appel par réflexion plutôt qu'un
  `softdepend` (KS_KaliumGive -> KS_Jetons / KS_Elixir -> KS_Economy : boucle de chargement sinon).

## Reste à faire

- Accord de LeKiwi06 pour déployer la 1.5.1 sur Event et Kixster (KS_Economy reste réservé en attendant).
- À tester en jeu : `/rachat` une semaine où un objet custom est tiré (tête, élixir, spawner, fiole), en Java et en
  Bedrock ; l'écran « Vendre au serveur » ; la vente d'un vrai objet custom.
