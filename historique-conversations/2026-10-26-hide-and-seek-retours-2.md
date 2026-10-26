# 26/10/2026 — Hide and Seek : retours de test (composteur, orientation, évasions, points)

- Plugin concerné : KG_HideAndSeek (Kal-Games, hébergé dans Kanvas)
- Versions avant / après : 0.2.0 (en service) / 0.3.0 (déployée sur Kanvas le 26/10/2026 à 15:03, à redémarrer)

## Demandé
Composteur impossible à imiter solide (troué sur le dessus) ; un objet en main pour changer l'état du bloc (statue du
golem de cuivre mal orientée, mauvaise posture) ; impossible de devenir solide sur une barrière, mais possible à
travers des fleurs, des trappes ; évasions cumulables tous les 5 points ; soundboard mieux valorisé (plus de délai de
10 s entre deux comptages) ; liste des blocs encore en vie à droite de l'écran pour aider les seekers ; points selon la
proximité des seekers, aucun point si aucun seeker n'est passé proche depuis 1 minute (proche = zone audible du
soundboard). Puis : en partie de 4 joueurs ou moins, ne pas pouvoir sortir du bâtiment central, seekers au même
départ que les hiders ; plus un bloc est rare dans la map, plus il rapporte de points. Enfin : solide possible sous
une dalle ou une trappe du haut ; jamais sur un autre joueur (exploit d'escalade en s'empilant).

## Fait
- KG_HideAndSeek 0.3.0 : hider solide stable dans un bloc creux (il se tient sur le fond du composteur) ; solide debout
  sur une barrière, un muret, un portillon (bloc posé sur leur haut visible) ; case vide obligatoire (plus de
  remplacement des fleurs, herbes, trappes, tapis) ; objet « Orienter le bloc » (clic droit : orientation ; accroupi +
  clic droit : posture du golem de cuivre) ; réserve d'évasions (une tous les 5 points, plus de recharge de 60 s) ;
  chaque son du soundboard entendu par un seeker compte ; tableau de droite « Blocs en vie » ; points de survie de ×1 à
  ×3 selon la distance du seeker le plus proche (30 blocs), nuls sans seeker proche depuis 60 s ; points d'arène
  facultatifs « Bâtiment : coin 1 / 2 » : à 4 joueurs ou moins, personne ne sort de la zone et les seekers partent du
  départ des hiders ; points de survie ×1 à ×2 selon la rareté du bloc imité dans la map (affichée dans « Votre bloc »).
- Sous un plafond bas, le hider solide est rapetissé (attribut de taille) pour tenir sur son bloc ; refus si la case du
  dessous est le bloc d'un autre hider ou si un autre joueur occupe la case.
- Compilé, déployé sur Kanvas le 26/10/2026 à 15:03 (0.2.0 dans `_removed-kg_hideandseek-0.2.0/`). Détail : `KG_HideAndSeek/JOURNAL.md`.

## Décisions (prises par Claude, à confirmer)
- « Proche » = portée des sons (30 blocs) ; ×3 au contact ; première minute de recherche comptée comme un passage.
- Posture au clic droit accroupi, sur le même objet que l'orientation.
- Pas de recharge entre deux évasions, seulement pas deux à la fois.
- Bâtiment : zone entre deux coins plutôt que des blocs invisibles posés à la main (ils bloqueraient aussi les grandes
  parties) ; nombre de joueurs compté au lancement.
- Rareté : échelle logarithmique, ×2 pour le plus rare, sur les points de survie seulement.

## Reste à faire
- Poser les points « Bâtiment : coin 1 / 2 » de l'arène (Informations > Paramètres > Hide and Seek > Arènes).
- Redémarrer Kanvas (fait par l'humain), puis tester en jeu (liste dans le journal), en Bedrock aussi.
