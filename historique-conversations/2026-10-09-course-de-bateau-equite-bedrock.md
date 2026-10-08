# 2026-10-09 — Course de bateau : équité Java / Bedrock

- Plugin(s) concerné(s) : KG_BoatRace
- Versions avant / après : 1.5.1 (en service, non testée) / 1.6.0 (compilée, non déployée, non testée)

## Demandé

LeKiwi06 (08/10/2026, 23 h 40) : « les joueurs Bedrock ont un net avantage sur le boat race comme leur bateau ne
fonctionne pas pareil (pour avoir testé, on dirait qu'ils ne dérapent presque pas) ».

## Fait

- Mesure d'abord, comme le prévoit le cahier des charges : journal de KG_ScoreBoards de Kal-Games téléchargé en lecture
  seule (copie hors dépôt), 1 781 tours du 24/09 au 08/10. Bedrock : meilleur tour 32,90 s contre 39,50 s en Java, tous
  les tronçons plus rapides, vitesses d'entrée plus hautes partout, deux fois plus de points par tour. Tableau dans
  `KG_BoatRace/CAHIER_DES_CHARGES.md`.
- KG_BoatRace 1.6.0 : bateau calculé par le serveur pour les joueurs Bedrock (physique du jeu Java), derrière le réglage
  « Bedrock : bateau serveur (essai) », désactivé par défaut. Détail dans `KG_BoatRace/JOURNAL.md`.
- Calcul vérifié hors jeu sur un faux monde (vitesses maximales de Java, mur, chute, dérapage).

## Décisions

- Trois pistes proposées : bateau serveur en essai, compensation seule (records séparés, paliers de chrono décalés pour
  Bedrock), ou les deux. LeKiwi06 : « bateau serveur en essai ».
- Empilé sur la 1.5.1 non testée : accord explicite de LeKiwi06 (« oui, empile »).
- Le joueur Bedrock est assis sur un porte-armure invisible et non dans le bateau : un joueur passe toujours en
  première place d'un véhicule, donc pilote, et son jeu calculerait de nouveau le mouvement.
- Aucun réglage Geyser trouvé pour la physique des bateaux (recherche rapide, rien de fiable).

## Reste à faire

- Déployer la 1.6.0 sur Kal-Games (accord de LeKiwi06 à chaque déploiement), puis tester en partie privée avec un joueur
  Bedrock, réglage activé : les 6 points listés dans le `JOURNAL.md` (touches, vue qui tourne, hauteur du siège, retard
  de la direction, sortie du siège, hors-piste et anti-collision).
- Refaire la mesure à pilote égal : une dizaine de tours de LeKiwi06 en Bedrock, avec puis sans le bateau serveur (le
  journal note `serverBoat` à chaque tour).
- Si la conduite est mauvaise : repli sur les records séparés et les paliers de chrono décalés.
- Réservation de KG_BoatRace gardée jusqu'au test.
