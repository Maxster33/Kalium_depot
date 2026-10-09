# 2026-10-09 — Bouton « Rejouer » en fin de partie ; cahier de KLM_Contacts

- Plugin(s) concerné(s) : KalGames, KG_Bingo, KG_BingoGame, KG_BuildBattle, KV_BuildBattle ; KLM_Contacts (cahier
  seulement, aucun code)
- Versions avant / après : KalGames 1.23.0 / 1.24.0 ; KG_Bingo 1.8.0 / 1.9.0 ; KG_BingoGame 0.10.0 / 0.11.0 ;
  KG_BuildBattle 0.3.0 / 0.4.0 ; KV_BuildBattle 0.3.6 / 0.4.0 (déployées le 09/10/2026 à 5 h 46, non testées)

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
- KLM_Contacts : valider les choix d'interprétation du cahier local et les deux questions restantes (modération des
  messages privés, lien entre le groupe et « Rejouer »), puis coder par étapes (amis, groupe, parties). Il touchera
  KaliumRelay (données sur le proxy), partagé avec Maxster33.
- Dossiers `_removed-…` supprimables par l'humain : liste dans `REPRISE_PROJET.md` (tableau des versions).
