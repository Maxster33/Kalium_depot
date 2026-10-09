# 2026-10-09 — Bouton « Rejouer » en fin de partie ; cahier de KLM_Contacts

- Plugin(s) concerné(s) : KalGames, KG_Bingo, KG_BingoGame, KG_BuildBattle, KV_BuildBattle ; KLM_Contacts (nouveau),
  KaliumRelay, KLM_Menu
- Versions avant / après : KalGames 1.23.0 / 1.24.0 ; KG_Bingo 1.8.0 / 1.9.0 ; KG_BingoGame 0.10.0 / 0.11.0 ;
  KG_BuildBattle 0.3.0 / 0.4.0 ; KV_BuildBattle 0.3.6 / 0.4.0 (déployées le 09/10/2026 à 5 h 46, non testées) ;
  KLM_Contacts 1.0.0 (nouveau), KaliumRelay 1.5.0 / 1.6.0, KLM_Menu 2.9.0 / 2.10.0 (compilées, non déployées, non
  testées)

## Demandé

LeKiwi06 (09/10/2026) : « pour tous les mini-jeux : faire un bouton à la fin de la partie pour relancer une partie avec
les mêmes paramètres » ; « j'aimerais aussi un KLM_Contacts pour ajouter nos amis, créer des groupes avec eux, voir dans
quels serveurs de KaLium ils sont, etc. »

## Fait

- Questions posées avant tout code, deux cahiers des charges écrits en local, puis validation de celui de « Rejouer ».
- **Rejouer** codé pour tous les jeux et déployé d'un coup (choix de LeKiwi06) :
  - KalGames 1.24.0 : objet « Rejouer » (30 s, case 7 du hub) à la fin d'un match, pour les joueurs et les spectateurs
    des 7 jeux du moteur (PvP Kit, Parcours, Course de bateau, Hide and Seek, Piliers de la Fortune, Pong, Rush) ;
  - KG_BingoGame 0.11.0 + KG_Bingo 1.9.0 : objet dans la salle d'attente d'après-partie du serveur Bingo ;
  - KV_BuildBattle 0.4.0 + KG_BuildBattle 0.4.0 : objet au retour sur kal-games.
- Cahier des charges publié : `KalGames/CAHIER_DES_CHARGES.md`. Détail technique : `JOURNAL.md` de chaque plugin.
- Déploiement par WinSCP (nouveaux jars envoyés, anciens déplacés dans `_removed-…`), jars vérifiés avant (nom et
  taille identiques à `jars-deployes/`). Aucun `config.yml` de serveur modifié. Serveurs non redémarrés.

- **KLM_Contacts, étape 1 « amis »** (après le « oui pour les deux, tu peux coder KLM_Contacts » de LeKiwi06) :
  - KLM_Contacts 1.0.0 (nouveau, serveurs Paper) : `/amis` et son menu (amis avec leur serveur, fiche d'un ami, ajout,
    demandes, bloqués, réglages), `/bloquer`, `/debloquer` ;
  - KaliumRelay 1.6.0 (proxy) : données des contacts, présence, « Rejoindre son serveur », notifications, messages
    privés `/mp` et `/r` écrits dans le journal du proxy ;
  - KLM_Menu 2.10.0 : bouton « Contacts » dans « Informations » (interfaces de joueur déclarées par les plugins).
  - Logique du proxy essayée hors jeu avec un faux proxy (32 cas : demandes, demandes croisées, blocage, réglages,
    relecture après redémarrage, changement de pseudo), tout passe. Compilés et poussés, **non déployés**.

## Décisions

- Forme : objet dans la barre pendant 30 s (plutôt qu'un menu qui s'ouvre seul ou une commande) ; proposé aussi aux
  spectateurs (demande de LeKiwi06).
- Partie privée : « le premier qui clique » recrée la partie et en devient l'hôte ; l'objet des autres devient
  « Rejoindre la partie de X ». Partie publique : retour dans la file.
- Spectateur : « il choisit » entre Jouer et Regarder.
- Périmètre : « tout maintenant » (moteur + Bingo + Build Battle), ordre « tout d'un coup » (déployés ensemble avant
  tout test), accord explicite pour empiler sur KalGames 1.23.0 non testé.
- Choix d'interprétation validés en bloc : mêmes réglages mais nouveau hasard (seed, thème, vote, rôles), même arène,
  case 7, pas d'objet si la partie est fermée autrement que par une fin de match.
- Bingo : les parties ne se créent que sur kal-games ; « Rejouer » fait donc un aller-retour par kal-games (deux écrans
  de chargement) et respecte la limite de 2 créations par heure et par joueur.
- KG_BuildBattle dépend désormais de KalGames (l'objet du hub est le sien).
- KLM_Contacts (réponses de LeKiwi06) : groupe = groupe de jeu (party) ; les membres suivent le chef d'office, réglable
  par membre ; invitations et messages privés ouverts à tous, réglables en « amis seulement » ; 1re version avec
  rejoindre / inviter, messages privés, notifications, mode invisible et blocage.

## Reste à faire

- Redémarrer Kal-Games, Serveur Jeux et Kanvas (par l'humain), puis tester sur Java et sur Bedrock : partie privée et
  partie publique d'un jeu du moteur (objet, recréation, « Rejoindre », spectateur), Bingo relancé à 2 joueurs, Build
  Battle privé relancé.
- Observateurs du Bingo (KG_BingoObservateur) : leur proposer aussi l'objet ? (question ouverte du cahier)
- KLM_Contacts (validé le 09/10/2026 : modération par le journal du proxy et LibertyBans, le groupe suit son chef
  quand il clique sur « Rejouer ») : **déployer l'étape 1** avec l'accord de LeKiwi06 : KaliumRelay 1.6.0 sur le proxy
  (redémarrage du proxy), KLM_Contacts 1.0.0 et KLM_Menu 2.10.0 sur les 6 serveurs Paper, `relay-token` à recopier à
  la main dans `plugins/KLM_Contacts/config.yml` de chaque serveur, `mp` et `r` à ajouter aux commandes bloquées de
  LibertyBans. Puis tester (deux comptes, dont un Bedrock, sur deux serveurs différents), puis étapes 2 (groupe de
  jeu) et 3 (parties).
- Décisions prises au code pour KLM_Contacts : `/mp` et `/r` sont des commandes du proxy (pour que LibertyBans puisse
  les bloquer) ; le bouton « Contacts » a demandé une petite modification de KLM_Menu (le cahier disait « sans le
  modifier ») et donne le comparateur à tous les joueurs ; « Rejoindre son serveur » est refusé pour Serveur Jeux
  seulement (réglable sur le proxy).
- Dossiers `_removed-…` supprimables par l'humain : liste dans `REPRISE_PROJET.md` (tableau des versions).
