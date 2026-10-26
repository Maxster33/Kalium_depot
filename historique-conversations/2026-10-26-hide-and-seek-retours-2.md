# 26/10/2026 — Hide and Seek : retours de test (composteur, orientation, évasions, points)

- Plugin concerné : KG_HideAndSeek (Kal-Games, hébergé dans Kanvas)
- Versions avant / après : 0.2.0 (en service) / 0.3.0 (compilée, non déployée)

## Demandé
Composteur impossible à imiter solide (troué sur le dessus) ; un objet en main pour changer l'état du bloc (statue du
golem de cuivre mal orientée, mauvaise posture) ; impossible de devenir solide sur une barrière, mais possible à
travers des fleurs, des trappes ; évasions cumulables tous les 5 points ; soundboard mieux valorisé (plus de délai de
10 s entre deux comptages) ; liste des blocs encore en vie à droite de l'écran pour aider les seekers ; points selon la
proximité des seekers, aucun point si aucun seeker n'est passé proche depuis 1 minute (proche = zone audible du
soundboard).

## Fait
- KG_HideAndSeek 0.3.0 : hider solide stable dans un bloc creux (il se tient sur le fond du composteur) ; solide debout
  sur une barrière, un muret, un portillon (bloc posé sur leur haut visible) ; case vide obligatoire (plus de
  remplacement des fleurs, herbes, trappes, tapis) ; objet « Orienter le bloc » (clic droit : orientation ; accroupi +
  clic droit : posture du golem de cuivre) ; réserve d'évasions (une tous les 5 points, plus de recharge de 60 s) ;
  chaque son du soundboard entendu par un seeker compte ; tableau de droite « Blocs en vie » ; points de survie de ×1 à
  ×3 selon la distance du seeker le plus proche (30 blocs), nuls sans seeker proche depuis 60 s.
- Compilé (`sortie/KG_HideAndSeek-0.3.0.jar`). Détail : `KG_HideAndSeek/JOURNAL.md`.

## Décisions (prises par Claude, à confirmer)
- « Proche » = portée des sons (30 blocs) ; ×3 au contact ; première minute de recherche comptée comme un passage.
- Posture au clic droit accroupi, sur le même objet que l'orientation.
- Pas de recharge entre deux évasions, seulement pas deux à la fois.

## Reste à faire
- Déployer KG_HideAndSeek 0.3.0 sur Kal-Games (Kanvas), seul, puis tester en jeu (liste dans le journal).
