# 2026-09-24 — Import du dépôt et raccordement des nouveaux serveurs au proxy (Maxster33)

- Plugin(s) concerné(s) : KLM_Menu (déploiement 2.0.0, config du lobby) ; configuration des serveurs (hors plugins).
- Versions avant / après : KLM_Menu 2.0.0 ajouté sur KalGames2, Serveur Jeux et Kal-Test-Dev ; aucune nouvelle version.

## Demandé
- Importer le bundle Git du projet dans le dépôt `Maxster33/Kalium_depot`, créer un dossier d'historique des
  conversations, donner la marche à suivre pour que le Claude de LeKiwi06 se connecte au même dépôt.
- « Pousser toutes les infos sur GitHub avant de commencer un travail et une fois fini. »
- Préparer la migration : KalGames → KalGames2, Bingo (Kixster) → Serveur Jeux, Kixster redevient SMP, l'ancien
  kal-games deviendra un serveur Event. Étape 1 : configurer KalGames2 et Serveur Jeux pour le crossplay avec les
  mêmes configs que les serveurs existants (plugins communs du lobby compris, avec leurs configurations), les
  raccorder au proxy, raccorder aussi Kal-Test-Dev.
- KLM_Menu partout, destinations ajoutées au KLM_Menu du lobby avec option pour les activer / désactiver.
- Ports voicechat fournis : proxy 44301, lobby 43841, Kixster 40046, KalGames 40002, KalTestDev 45595,
  Serveur Jeux 43131, KalGames2 43374.

## Fait
- Voir le compte rendu signé de Maxster33 du 2026-09-24 dans `REPRISE_PROJET.md`.

## Décisions
- Noms Velocity : `kalgames2`, `serveur-jeux`, `kal-test-dev`.
- Liste blanche désactivée sur les nouveaux serveurs ; configurations des plugins du lobby copiées telles quelles.
- L'option activer / désactiver des destinations existait déjà dans KLM_Menu (`disabled-destinations`, menu >
  Paramètres) : aucun changement de code ; les 3 nouvelles destinations sont ajoutées désactivées.
- Geyser-Spigot non copié (Geyser tourne sur le proxy).

## Reste à faire
- Redémarrages par l'humain : KalGames2, Serveur Jeux, Kal-Test-Dev, lobby, puis proxy ; vérifier les journaux.
- Activer les destinations dans le menu du lobby quand les serveurs sont prêts.
- voicechat : Kixster / kal-games / proxy non réglés ; décider de `voice_host` ou d'un voicechat sur le proxy.
- Étapes suivantes de la migration.

## Suite : migration complète (après-midi)
- Demandé : copier entièrement kal-games → KalGames2 et Kixster (Bingo) → Serveur Jeux « sans rien oublier » ; noms
  Velocity / KLM_Menu : KalGames2 devient kal-games, Serveur Jeux garde son nom, Bingo redevient kixster, l'ancien
  kal-games devient event ; destinations des plugins mises à jour sur les nouveaux serveurs uniquement (+ proxy).
- Décision : option B (transférer les noms dans le proxy) plutôt que renommer dans chaque plugin.
- Fait / reste à faire : voir le compte rendu de Maxster33 dans `REPRISE_PROJET.md`.

## Suite : correctifs et Kixster rendu au SMP (fin d'après-midi)
- Demandé : réparer les erreurs de démarrage (extension PlaceholderAPI mal placée sur le hub, voicechat en double et
  AnvilUnlocker sur le Bingo), puis remettre Kixster exactement comme avant le Bingo.
- Décisions de Maxster33 : AnvilUnlocker retiré du Bingo ; sur Kixster, WorldEdit gardé, voicechat 2.6.24 retiré,
  KLM_Menu gardé sans boussole avec `/menu` (alias de `/servers`), `generate-structures=false` laissé.
- Vérifié après redémarrage (24/09, 16 h 30) : KalGames2 et Serveur Jeux sans erreur ; Kixster : seule l'erreur
  AnvilUnlocker d'avant le Bingo (sans ProtocolLib).
- Reste à faire : reconversion de l'ancien kal-games en serveur Event ; tests en jeu ; KG_BoatRace (LeKiwi06) à
  déployer sur KalGames2.
