# 2026-10-21 — Déploiement de KG_Bingo et KG_BuildBattle (menus en coffre)

- Plugin(s) concerné(s) : KG_Bingo, KG_BuildBattle (Kal-Games)
- Versions avant / après : KG_Bingo 1.10.0 / 1.11.0 ; KG_BuildBattle 0.5.0 / 0.6.0

## Demandé
LeKiwi06 (par le projet KaLium) : compiler KG_Bingo 1.11.0 et KG_BuildBattle 0.6.0 et les déployer sur Kal-Games,
sans redémarrer le serveur. Puis « je te l'autorise » (après le refus du filtre de permissions), et « je ne peux pas
tester ce soir, on va partir du principe que ça fonctionne, si tu as repris la logique utilisée sur les autres jeux
c'est censé être bon ».

## Fait
- Compilation OK des deux plugins.
- Vérification avant envoi : `/plugins/` de Kal-Games contenait bien les jars de `jars-deployes/` (1.10.0 et 0.5.0).
- Déploiement le 21/10/2026 à 01:29 (session WinSCP « Kalgames [new] ») : nouveaux jars envoyés d'abord, anciens
  déplacés dans `_removed-kg_bingo-1.10.0/` et `_removed-kg_buildbattle-0.5.0/`. Pas de nouvelle clé de config.
- `jars-deployes/`, les deux `JOURNAL.md` et `REPRISE_PROJET.md` mis à jour (commit db50ab1).
- Code vérifié : Bingo et Build Battle utilisent la même classe `Contenant` (KLM_Menu) que KalGames 1.26.0, et les
  jauges du Bingo le même mécanisme (fenêtre, « Valider », Échap enregistre).
- Réservations de KG_Bingo et KG_BuildBattle libérées.

## Décisions
- Statut laissé « non testé en jeu » (la règle demande un test confirmé), même si LeKiwi06 part du principe que ça
  fonctionne.

## Reste à faire
- Redémarrage de Kal-Games (humain).
- Tests en jeu, Bingo : coffre de réglages unique ; jauges (Échap enregistre et ramène au coffre) ; composition de la
  grille (4 jauges, total rouge si différent de 25) ; mode (contre la montre seulement à 1 équipe, proposé par défaut) ;
  Échap = Retour ; partie visible dans « Rejoindre une partie » et par « Entrer un code », transfert vers Serveur Jeux ;
  « Recréer ma dernière partie » et réglages retenus après déconnexion ; mode fenêtres ; affichage Bedrock.
- Tests en jeu, Build Battle : coffre (tempo et équipes au clic gauche / droit, thèmes écrits), Échap = Retour ; partie
  privée dans « Rejoindre une partie » et par code, départ vers Kanvas ; « Recréer ma dernière partie » ; mode
  fenêtres ; objet « Rejouer » au retour de Kanvas et du Bingo.
- Suppression par l'humain sur Kal-Games : `_removed-kg_bingo-1.8.0` et plus anciens (1.7.3 à 1.3.0),
  `_removed-kg_buildbattle-0.3.0`, `0.2.0`, `0.1.0`.
