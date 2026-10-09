# KLM_Contacts — cahier des charges (demande de LeKiwi06, 09/10/2026)

**Validé par LeKiwi06 le 09/10/2026** (« oui pour les deux, tu peux coder KLM_Contacts ») : partie 4 validée, questions 5.3 et 5.4 tranchées (oui aux deux).
**Code : étape 1 « amis » - KLM_Contacts 1.0.0 et KLM_Menu 2.10.0 déployés sur les 6 serveurs Paper, KaliumRelay 1.6.0 sur le proxy, le 09/10/2026 à 6 h 06 ; non testés** (détail dans les `JOURNAL.md`). **Étape 2 « groupe de jeu » codée le 09/10/2026 sans attendre le test de l'étape 1 (demande de LeKiwi06) : KLM_Contacts 1.1.0 et KaliumRelay 1.7.0, déployés le 09/10/2026 à 6 h 29, non testés.** Étape 3 (parties) : à coder ensuite.

Nouveau plugin `KLM_Contacts` (préfixe `KLM_` : réseau entier), présent sur **chaque serveur Paper** (lobby,
kal-games, Serveur Jeux, Kixster, Event, Kanvas), comme KLM_Menu et KLM_Chat.

| Plugin | Où | Rôle |
|---|---|---|
| `KLM_Contacts` (nouveau) | chaque serveur Paper | Menus et commandes : amis, groupe, messages privés |
| `KaliumRelay` (modifié) | proxy Velocity | Garde les données (amis, demandes, blocages, groupes), sait sur quel serveur est chaque joueur, déplace les joueurs, livre messages et notifications |
| `KalGames` (modifié, étape 3) | kal-games | Entrée directe dans une partie privée sur invitation ; le groupe entre ensemble en partie |

## 1. Demande d'origine (recopiée telle quelle, 09/10/2026)

> j'aimerai aussi avec un KLM_contacts pour ajouter nos amis , créer des groupes avec eux ,  voir dans quel serveurs
> de kalium ils sont , etc .

## 2. Réponses de LeKiwi06 (09/10/2026)

1. **Groupe = groupe de jeu (party)** : un groupe temporaire avec un chef ; on s'invite, on rejoint un serveur ou une
   partie ensemble, tchat de groupe ; il disparaît quand tout le monde le quitte. Pas de listes permanentes d'amis.
2. **Dans la 1re version**, en plus des demandes d'ami (à accepter), des groupes et du serveur où se trouve chaque ami :
   - **Rejoindre / inviter** : bouton « Rejoindre son serveur » sur un ami, et « Inviter dans ma partie » (partie
     privée de Kal-Games) ;
   - **Messages privés** entre amis d'un serveur à l'autre, et tchat de groupe ;
   - **Notifications** : message quand un ami se connecte / se déconnecte ; demandes d'ami reçues même hors ligne
     (affichées à la connexion) ;
   - **Invisible et blocage** : mode invisible (les amis ne voient plus ton serveur) et liste de joueurs bloqués (plus
     de demandes ni de messages).

## 3. Règles

### Amis
1. **Demande d'ami** par pseudo (commande ou champ du menu) ; l'autre doit **accepter**. Elle peut être envoyée à un
   joueur hors ligne : il la voit à sa prochaine connexion.
2. **Liste d'amis** (menu) : pour chaque ami, **en ligne / hors ligne** et **le serveur de KaLium où il se trouve**
   (Lobby, Kal-Games, Serveur Jeux, Kixster, Event, Kanvas), amis en ligne en premier.
3. Fiche d'un ami : **Rejoindre son serveur**, **Envoyer un message**, **Inviter dans mon groupe**, **Inviter dans ma
   partie** (si je suis dans une partie privée), **Retirer des amis**, **Bloquer**.
4. **Notifications** : « X s'est connecté » / « X s'est déconnecté » pour ses amis ; désactivables.
5. **Mode invisible** : mes amis me voient « hors ligne » (ni serveur, ni notification de connexion). Je vois toujours
   les miens.
6. **Blocage** : un joueur bloqué ne peut plus m'envoyer de demande d'ami, de message privé ni d'invitation ; s'il
   était mon ami, il est retiré de mes amis. Il n'est pas prévenu.

### Groupe de jeu
7. **Créer un groupe** = inviter quelqu'un : celui qui invite devient le **chef**. L'invité accepte ou refuse
   (l'invitation expire au bout de 60 s).
8. Le chef peut **inviter**, **exclure**, **nommer un autre chef**, **dissoudre** le groupe. Chacun peut **quitter**.
   Si le chef part, le plus ancien membre devient chef. Le groupe disparaît quand il ne reste qu'une personne.
9. **Tchat de groupe** : visible des membres, quel que soit leur serveur.
10. **Se déplacer ensemble** : les membres sont déplacés **d'office** avec le chef, sauf ceux qui sont en pleine
    partie ; chaque membre peut passer en **« me demander avant »** dans ses réglages (il reçoit alors une
    proposition « Suivre ») (réponse du 09/10/2026 : « D'office, réglable ») :
    - le chef change de serveur : les membres le suivent ;
    - le chef crée ou rejoint une partie privée de Kal-Games : les membres y entrent avec lui ;
    - le chef entre dans une file publique : les membres y entrent avec lui.
11. Un groupe survit à un changement de serveur ; un membre déconnecté depuis plus de 5 minutes en sort.

### Messages privés
12. **Message privé** à un ami, d'un serveur à l'autre ; **répondre** au dernier message reçu d'une commande courte.
13. Désactivables (ne plus recevoir de messages privés).

### Accès
14. **Bouton « Contacts »** dans le comparateur « Informations » de KLM_Menu, sur tous les serveurs (déclaré par
    KLM_Contacts, sans modifier KLM_Menu).
15. **Commandes** : `/amis` (ouvre le menu ; `/amis ajouter <pseudo>`, `accepter`, `refuser`, `retirer`),
    `/groupe` (`inviter <pseudo>`, `accepter`, `quitter`, `exclure`, `chef`, `dissoudre`), `/gc <message>` (tchat de
    groupe), `/mp <pseudo> <message>` et `/r <message>` (messages privés), `/bloquer` / `/debloquer <pseudo>`.

## 4. Choix d'interprétation (à valider)

1. **Qui voit mon serveur** : mes amis acceptés seulement. Un joueur qui n'est pas mon ami ne voit rien.
2. **Pseudos** : on peut ajouter tout joueur déjà venu sur KaLium depuis l'installation (Java ou Bedrock ; le pseudo
   Bedrock s'écrit avec son préfixe habituel). Un joueur jamais vu : « joueur inconnu ».
3. **Plafonds** : 100 amis par joueur, 8 joueurs par groupe (réglables sur le proxy).
4. **Messages privés et invitations de groupe : ouverts à tous par défaut**, avec un réglage personnel **« amis
   seulement »** pour chacun des deux (réponse du 09/10/2026 : « on peut inviter n'importe qui (invitation réglable en
   "amis seulement"), pareil pour les MP »). Les joueurs bloqués restent refusés. Le `/msg` vanilla (même serveur)
   n'est pas touché.
5. (fusionné avec le point 4)
6. **« Rejoindre son serveur »** : refusé avec un message si l'ami est sur un serveur où l'on n'entre pas librement
   (Serveur Jeux : on n'y va que par une partie de Bingo ; Kanvas en Build Battle) ; l'ami en mode invisible n'est pas
   joignable.
7. **Modération** : les messages privés et le tchat de groupe sont écrits dans le journal du proxy (lisible par les
   admins), et un joueur banni / muet par LibertyBans ne peut pas en envoyer (voir question 5.3).
8. **Menus** : Dialogs de la boîte à outils de KLM_Menu ; aucun texte de bouton ne défile ; couleurs lisibles sur
   Bedrock.
9. **Technique (signalé, nécessaire)** :
   - les données sont gardées **sur le proxy** par KaliumRelay (seul endroit qui voit tous les joueurs et tous les
     serveurs), dans son dossier de données, jamais dans le dépôt ; KLM_Contacts interroge le relais avec le jeton
     habituel (`relay-token`, à remplir à la main dans le `config.yml` de chaque serveur) ;
   - messages, notifications et déplacements sont faits **directement par le proxy** (pas de délai, pas besoin qu'un
     joueur soit connecté sur le serveur d'arrivée) ;
   - si le relais est injoignable : le menu affiche « Contacts indisponibles », rien d'autre n'est gêné ;
   - KaliumRelay est partagé avec Maxster33 : réservation, et redémarrage du proxy au déploiement.
10. **Étapes** (une version testée avant la suivante) :
    1. **Amis** : demandes, liste avec serveur, rejoindre son serveur, notifications, invisible, blocage, messages
       privés. (KLM_Contacts 1.0.0 + KaliumRelay)
    2. **Groupe** : création, invitations, chef, tchat de groupe, suivre le chef d'un serveur à l'autre.
    3. **Parties** : « Inviter dans ma partie », le groupe entre ensemble dans une partie privée ou une file publique
       de Kal-Games (KalGames modifié) ; Bingo et Build Battle ensuite.

## 5. Questions ouvertes

1. **Réglé (09/10/2026)** : d'office, réglable par membre (règle 10).
2. **Réglé (09/10/2026)** : n'importe qui, réglable en « amis seulement », pour les invitations comme pour les MP
   (choix 4.4).
3. **Réglé (09/10/2026) : oui.** Messages privés et tchat de groupe écrits dans le journal du proxy ; sanctions
   LibertyBans respectées : `/mp` et `/r` sont des commandes du proxy, à ajouter aux commandes bloquées de LibertyBans.
4. **Réglé (09/10/2026) : oui.** Quand le chef du groupe clique sur « Rejouer », tout le groupe le suit dans la
   nouvelle partie (étape 3).

## 6. Écarts constatés au code de l'étape 1 (09/10/2026)

1. Règle 14 : le bouton « Contacts » a demandé une petite modification de KLM_Menu (2.10.0, `MenuSection.informations()`),
   contrairement à ce qui était écrit ; conséquence : tous les joueurs reçoivent le comparateur « Informations ».
2. Choix 4.6 : « Rejoindre son serveur » n'est refusé que pour Serveur Jeux (`contacts-no-join` sur le proxy) ; un ami
   en Build Battle sur Kanvas est joignable (on arrive sur les plots). Les destinations désactivées dans la boussole du
   lobby ne sont pas prises en compte.
3. Règle 15 : `/mp` et `/r` sont des commandes du proxy ; `/gc` viendra avec l'étape 2.

## 7. Choix faits au code de l'étape 2 (09/10/2026)

1. Règle 15 : `/groupe` et `/gc` sont des commandes du proxy (comme `/mp` et `/r`) ; `/groupe` seul ouvre le menu
   « Groupe de jeu ». Sous-commandes ajoutées : `refuser`, `suivre`, `info`.
2. Règle 10 : le chef change de serveur, les membres le suivent : fait. « Le chef crée ou rejoint une partie privée » et
   « le chef entre dans une file publique » : étape 3.
3. Règle 10, « sauf ceux qui sont en pleine partie » : le proxy demande au serveur du membre s'il est en partie ; un
   membre qui se trouve sur Serveur Jeux n'est jamais déplacé d'office (proposition), et personne n'est déplacé vers
   Serveur Jeux (on n'y entre que par une partie de Bingo).
4. Un membre qui accepte une invitation, ou qui se reconnecte, rejoint le chef selon la même règle (d'office ou sur
   proposition).
5. Règle 8 : quand le chef se déconnecte, le plus ancien membre connecté devient chef tout de suite (sans attendre les
   5 minutes de la règle 11) ; à son retour, l'ancien chef est un membre comme les autres.
6. Les groupes sont en mémoire sur le proxy : un redémarrage du proxy les efface.
7. Modération : le tchat de groupe est écrit dans le journal du proxy ; `gc` est à ajouter aux commandes bloquées de
   LibertyBans, avec `mp` et `r`.
