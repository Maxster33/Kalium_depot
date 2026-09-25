# Architecture cible du réseau KaLium (décidée avec LeKiwi06, 24/09/2026)

Brouillon local. À pousser avec la mise à jour de `REGLES.md` (règle 2.2) et de `REPRISE_PROJET.md`, après le push
de Maxster33 (réorganisation des serveurs en cours).

Principe : **un plugin = un rôle** ; les plugins se parlent au lieu d'être fusionnés.

## Serveurs (après la migration de Maxster33 du 24/09/2026, voir `REPRISE_PROJET.md`)

| Nom Velocity | Machine (onglet WinSCP) | Rôle | Plugins à nous (cible) |
|---|---|---|---|
| (proxy) | 7018 | Proxy Velocity | KLM_Relay |
| `lobby` | 7002 | Lobby | KLM_Menu, KLM_Hub |
| `kal-games` | 7001 (ex KalGames2) | Hub des mini-jeux | KG_Instances, KG_Menu, KG_ScoreBoards, KG_Bingo, KG_BoatRace, KG_Parkour, KG_PvpKit, KG_Rush, KLM_Menu, KLM_Hub |
| `serveur-jeux` | 7015 | Bingo + jeux gourmands (Manhunt...) | KG_BingoGame, KLM_Menu |
| `kixster` | 7003 | Kixster SMP (rendu au SMP) | KLM_Menu (futur KX_Menu) |
| `event` | 7021 (ancien kal-games) | Serveur Event : survie classique (depuis le 25/09/2026), préfixe `KS_` | KLM_Menu (sans boussole, `/menu`), KS_Dimensions |
| `kal-test-dev` | 5038 | Test survie (en pause) | KaliumCore, KLM_Menu |

Les anciens noms `Bingo` et `kalgames2` n'existent plus. Tout déploiement pour le hub se fait sur 7001, pour le
Bingo sur 7015.

## Couche réseau (tous les serveurs)

| Plugin | Où | Rôle |
|---|---|---|
| **KLM_Relay** (aujourd'hui KaliumRelay, renommage décidé, voir charte § 2) | proxy Velocity | « Boîte aux lettres » entre serveurs, indépendante des joueurs (affectations, reconnexion en partie, futures pages de menu à distance). Reste séparé : un plugin Velocity ne tourne pas sur Paper |
| **KLM_Menu** | chaque serveur Paper | Couche profonde des menus : navigation entre serveurs, boussole, boîte à outils des menus, catalogue « Interfaces », redirection de fin de partie |
| **KLM_Hub** (nouveau) | chaque serveur Paper | Point d'apparition, arrivée des joueurs, objets verrouillés du hub. Les **protections de zone** sont confiées à **WorldGuard** (déjà installé), pas codées |
| **Menus par serveur** : KG_Menu (mini-jeux), puis KX_Menu, SK_Menu, KV_Menu... | un par serveur | Menu propre au serveur ; alimenté par les plugins du serveur |

## Couche mini-jeux (serveur des mini-jeux)

| Plugin | Rôle |
|---|---|
| **KG_Instances** (nouveau, sorti de KalGames) | Moteur des parties : file d'attente, parties publiques / privées, compte à rebours, spectateurs, attribution des arènes ; monde des instances, capture et collage des arènes. Valide les fins de partie |
| **KG_Menu** | **Chargeur d'interfaces** : chaque plugin FOURNIT ses interfaces (joueur et admin), KG_Menu les DÉCOUVRE au démarrage et décide qui voit quoi, sans avoir ces écrans dans son code. Pages : Mini-jeux (plugins de jeu), Parties en cours (KG_Instances), Classements et journaux des scores (KG_ScoreBoards), Configuration (interface admin de chaque plugin) |
| **KG_ScoreBoards** | Stockage de tous les scores et données (temps par checkpoint...), classements, affichage en jeu, future liaison unique avec le bot Discord. Contient les **règles communes** à tous les jeux : bonus puis multiplicateurs (additifs), classement cumulé (ses points + ceux des joueurs en dessous), points décimaux, format K / M / Md. Contient aussi l'ancienne passerelle **ScoreBridge**, gardée comme **barème de secours** pour un jeu qui n'a pas encore le sien |
| **KG_BoatRace**, **KG_Parkour** | Jeux (en cours, voir leurs cahiers des charges). Chacun : son barème, son interface admin, son interface joueur |
| **KG_PvpKit**, **KG_Rush** | Jeux à sortir ensuite (d'abord les jeux les plus joués ; le Rush, jeu de niche, sera peaufiné plus tard) |
| **KG_Bingo** / **KG_BingoGame** | Déjà séparés |
| Hunger Games, Manhunt, Build Battle | En attente (aujourd'hui simples emplacements sans moteur dans KalGames) |

## Modération (plus tard, session dédiée)

| Plugin | Rôle |
|---|---|
| **KG_AntiCheat** (préfixe à confirmer : `KLM_` s'il couvre tout le réseau) | Donne des **indices de suspicion** (jamais de sanction automatique sans décision humaine) : gains de points anormaux ; proportion de minerais rares anormale (diamants...) ; analyse de toutes les statistiques de parties stockées dans KG_ScoreBoards ; inspection de l'inventaire d'un joueur (« invsee ») ; tenue de la liste noire et détection des comptes secondaires (alts) des joueurs de la liste noire ; **suspicion de revente contre de l'argent réel** (récompenses jamais ouvertes ni vendues au marché, objets de valeur toujours donnés ou lâchés à d'autres joueurs sans contrepartie visible, flux à sens unique répétés entre mêmes comptes) ; autres triches classiques |

**Répartition avec GrimAC** (décidé par LeKiwi06 le 24/09/2026) : **GrimAC** (déjà installé sur le hub) fait le
travail « dur » en temps réel (mouvements, combat, triches client classiques). Notre anti-triche ne le refait pas :
il **analyse les événements précis** (gains, minerais, échanges, statistiques de parties) pour repérer des
**schémas de triche** dans la durée.

**Traçabilité des objets de valeur** (décidé par LeKiwi06 le 24/09/2026) : chaque objet de valeur généré par le
serveur (récompense, lot...) reçoit un **identifiant aléatoire unique** caché dans l'objet (données persistantes de
l'item), avec son destinataire d'origine ; chaque passage de main en main (don, objet lâché puis ramassé, coffre,
vente au marché) est journalisé. Bonus : deux objets portant le même identifiant = duplication (glitch) détectée.
Limite à prévoir : deux objets avec des identifiants différents ne s'empilent plus entre eux ; réserver le marquage
aux objets de valeur (ou regrouper par lot).

Une fois tout sorti, **KalGames disparaît** : son contenu est réparti entre KG_Instances, KLM_Hub (+ WorldGuard),
KG_Menu et KG_ScoreBoards.

## Circuit d'une fin de partie

1. Le **jeu** calcule les points avec son barème (règles communes fournies par KG_ScoreBoards).
2. Il les remet à **KG_Instances**, qui valide la fin de partie.
3. **KG_ScoreBoards** enregistre scores et données.
4. KG_Instances clôt la partie ; **KLM_Menu / KLM_Hub** redirigent les joueurs (retour au hub).

# Charte du réseau KaLium

## 1. Nommage des plugins

| Préfixe | Portée | Exemples |
|---|---|---|
| `KLM_` | Réseau entier (proxy ou chaque serveur Paper) | KLM_Menu, KLM_Hub, KLM_Relay |
| `KG_` | Serveur des mini-jeux (Kal-Games) | KG_Instances, KG_Menu, KG_ScoreBoards, KG_BoatRace, KG_Parkour |
| `<XX>_` | Serveur précis, un préfixe de 2 lettres par serveur (`KS_` = serveur Event, choisi par Maxster33 le 25/09/2026) | KS_Dimensions ; KX_Menu, SK_Menu, KV_Menu (futurs) |

- Un plugin = un rôle. Le menu d'un serveur s'appelle toujours `<préfixe>_Menu`.
- Le dossier du dépôt, le jar (`<Nom>-<version>.jar`), le nom du plugin et son dossier de données portent le même nom.

## 2. Renommage KaliumRelay → KLM_Relay (décidé le 24/09/2026, à faire)

À faire dans une session dédiée, plugin réservé :
- dossier du dépôt, `pom.xml`, `build.sh`, `velocity-plugin.json`, identifiant `@Plugin(id = "klm_relay")`, classe
  principale, `README.md`, `REPRISE_PROJET.md` (partie permanente), textes et commentaires des plugins qui le citent
  (KG_Bingo, KG_BingoGame) à leur prochaine version ;
- les JOURNAL.md, `archive_reprise.md` et `historique-conversations/` gardent l'ancien nom (historique) ;
- **déploiement sur le proxy** : le dossier de données change (`plugins/kaliumrelay/` → `plugins/klm_relay/`) : y
  recopier `relay.properties` (jeton compris, fait par l'humain, jamais dans le dépôt) ; l'ancien jar et l'ancien
  dossier vont dans `_removed-kaliumrelay-<version>/`. L'adresse et le port du relais ne changent pas : rien à
  modifier sur les autres serveurs.

## 3. Menus inter-serveurs (validé le 24/09/2026 ; amélioration pour plus tard, pas prioritaire)

Contrainte : un menu en coffre n'existe que sur le serveur du joueur. Fonctionnement mixte :
- **Consultation et actions simples à distance** : le menu du serveur B fournit par KLM_Relay une DESCRIPTION de page ;
  KLM_Menu la dessine sur le serveur A avec sa boîte à outils (même style partout) ; un clic est renvoyé à B.
- **Pages détaillées** (inventaires, placement de points, arènes) : bouton « Ouvrir sur <serveur> » : le joueur est
  transféré et la page s'ouvre à son arrivée (même mécanisme que l'affectation Bingo). Pour aller à pleine vitesse,
  on va directement sur le serveur concerné.
- Le délai (quelques centaines de ms) est accepté.

**Format d'une description de page** (JSON, versionné) : `format` (numéro de version du format), `server` (serveur
d'origine), `page` (identifiant), `title`, `rows`, `buttons` (pour chacun : `slot`, `icon`, `name`, `lore`, `action`
facultative), `generatedAt`. Un serveur qui reçoit un `format` qu'il ne connaît pas affiche « page indisponible, mettez
le serveur à jour » au lieu d'une page cassée.

**Format d'une action** : `format`, `from` (serveur du joueur), `player` (UUID), `page`, `action`, `sentAt`.

## 4. Sécurité des échanges entre serveurs

1. Toute requête vers KLM_Relay porte le jeton du relais (`X-Kalium-Relay-Token`) ; le jeton n'existe que dans les
   fichiers de configuration des serveurs, jamais dans le dépôt ni dans une conversation.
2. Le serveur qui EXÉCUTE une action revérifie lui-même les permissions du joueur (opérateur, permission du menu) :
   il ne fait jamais confiance à ce qu'annonce le serveur d'origine.
3. Une action n'est acceptée que si elle correspond à un bouton de la page que CE serveur a lui-même décrite
   (pas d'action arbitraire), et une seule fois (identifiant unique, refus des doublons).
4. Une action trop ancienne (plus de 30 s après `sentAt`) est refusée.
5. Rien de sensible dans une description de page (jeton, adresse IP de joueur, mot de passe).
6. Si le relais est injoignable, chaque serveur garde ses menus locaux : seule la partie « à distance » est coupée.
