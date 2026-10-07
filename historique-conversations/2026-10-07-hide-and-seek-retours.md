# 2026-10-07 — Hide and Seek : retours du premier test

- Plugin(s) concerné(s) : KG_HideAndSeek
- Versions avant / après : 0.1.0 / 0.2.0 (déployé sur Kal-Games le 07/10/2026 à 22 h 10, non testé)

## Demandé

LeKiwi06, après un premier test avec des joueurs : « j'ai besoin de faire des modifications : quand les seekers
attendent, ils voient la map, il faut leur donner darkness ; désactiver la boussole de localisation, sinon les joueurs
non solides sont visibles ; améliorer la portée des sons, il faut qu'on les entende sur une trentaine de blocs ; faire
en sorte qu'on voie son propre bloc quand on est solide ; faire un seul item dans la hotbar pour le menu des
soundboard ; quand on est solide parfois on se fait pousser par son propre bloc et on redevient en mouvement, c'est
très gênant ».

## Fait

- KG_HideAndSeek 0.2.0 : les six points (détail dans `KG_HideAndSeek/JOURNAL.md`), compilé.
- Cahier des charges complété (partie 5 bis).
- Déployé sur Kal-Games avec l'accord de LeKiwi06 (0.1.0 rangée dans `_removed-kg_hideandseek-0.1.0/`), actif au
  prochain redémarrage. Réservation libérée.

## Décisions

- « Boussole de localisation » compris comme la barre de localisation du jeu (direction des joueurs en haut de
  l'écran) : coupée pour tous les joueurs de la partie.
- Voir son bloc et ne plus être repoussé : une seule correction pour les deux, le hider solide se tient debout sur son
  bloc au lieu d'être dedans. Contrepartie : il faut une case libre au-dessus du bloc.
- Sons favoris retirés avec les 6 objets de la barre.

## Reste à faire

- Redémarrer Kal-Games (humain), puis tester la 0.2.0.
- Toujours à confirmer : dernier seeker déconnecté = fin de partie ; hiders tous déconnectés = seekers gagnants ;
  familles de blocs refusées.
