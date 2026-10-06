# 2026-10-06 — Hide and Seek (nouveau mini-jeu)

- Plugin(s) concerné(s) : KG_HideAndSeek (nouveau)
- Versions avant / après : aucune / 0.1.0 (déployé sur Kal-Games le 06/10/2026 à 5 h 04, non testé)

## Demandé

LeKiwi06 : « j'aimerais créer un mini-jeu de hide and seek » : les hiders sont des blocs du décor de la map, ils ont un
soundboard, deviennent solides quand ils restent statiques et émettent un son toutes les 30 secondes ; un seeker les
élimine d'un coup, n'a que 5 cœurs, perd un demi-cœur par erreur, récupère sa vie et de l'absorption quand il trouve un
hider.

Deux sessions le même jour : la première (questions et réponses, cahier des charges) s'est arrêtée sur une erreur
d'accès ; la seconde a repris le cahier, fait valider les derniers points et écrit le code.

Précisions données pendant la seconde session :
- « certains blocs décoratifs comme les enclumes, composteur, table d'enchantement, pot décoratif ne sont pas des blocs
  pleins, mais devraient être utilisables » ;
- « il faut qu'on soit un joueur normal qui porte le bloc sur sa tête tant qu'on n'est pas solide, sinon si on utilise
  un modèle 3D du bloc, les joueurs Bedrock ne le verront pas ».

## Fait

- Cahier des charges validé, publié au déploiement : `KG_HideAndSeek/CAHIER_DES_CHARGES.md`.
- KG_HideAndSeek 0.1.0 écrit et compilé (`sh KG_HideAndSeek/build.sh`) : type `HIDE_AND_SEEK` enregistré auprès de
  KalGames, sans aucun changement dans KalGames. Détail : `KG_HideAndSeek/JOURNAL.md`.
- Déployé sur Kal-Games à la demande de LeKiwi06 (jar envoyé par WinSCP, copie dans `jars-deployes/`) ; actif au
  prochain redémarrage du serveur. Réservation libérée.

- Correction de la procédure : il n'y a plus de bouton « créer un mini-jeu » (KalGames 1.21.0) ; KalGames crée le
  mini-jeu `hide-and-seek` au démarrage et déclare ses boutons à KG_Menu.
- Map construite sur Kanvas : importée sur Kal-Games par un schéma WorldEdit (`hideandseek.schem`, 207 x 121 x 167,
  environ 850 000 blocs ; une première sauvegarde ne contenait qu'une partie de la map). Fichier copié par WinSCP
  dans `plugins/WorldEdit/schematics/` de Kal-Games ; collage, feuilles de cerisier rendues persistantes, capture,
  points et blocs de la map faits en jeu par LeKiwi06 (« normalement tout est configuré »).

## Décisions

- 3 à 16 joueurs, 1 seeker pour 5 joueurs, volontaires d'abord ; cachette 30 s, recherche 5 min.
- Hider en mouvement : joueur normal avec son bloc sur la tête (Bedrock) ; hider immobile 3 s : vrai bloc, joueur caché.
  Proposition écartée : bloc en modèle 3D qui suit un joueur invisible (non vu par les joueurs Bedrock).
- Blocs d'une map : réglés par un modérateur ; acceptés s'ils tiennent dans une case et qu'on bute dessus, pleins ou
  non ; refusés : dalles, escaliers, barrières, murets, vitres, portes, lits, blocs traversables.
- Hider éliminé : réglage de la partie (spectateur par défaut, ou devient seeker).
- Déconnexion : retour possible jusqu'à la fin, aucun point pendant l'absence ; plus aucun seeker connecté : fin
  immédiate, les hiders gagnent.
- Points : hider 1 / 20 s survécues, +8 en vie à la fin ; seeker 6 par hider, +5 si tous trouvés ; soundboard +0,25 par
  son (un compté toutes les 10 s, seeker à 24 blocs au plus) ; classement de fin : règle commune (ses points + la
  moyenne de ceux classés en dessous).

## Reste à faire

- Tester, en Java et en Bedrock (liste dans `KG_HideAndSeek/JOURNAL.md`), puis mesurer le rythme des points.
- À confirmer par LeKiwi06 : le dernier seeker qui se déconnecte termine la partie ; s'il ne reste que des hiders
  déconnectés, les seekers gagnent ; liste exacte des familles de blocs refusées.
