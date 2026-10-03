# KLM_Chat - journal

Plugin réseau (préfixe `KLM_`) : tchat inter-serveur. Le même jar sur **tous les serveurs Paper**, sans réglage ni
dépendance.

## 1.0.0 — tchat global `/global` (03/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « proposer la commande /global send/true/false <message> pour utiliser le tchat
interserveur » ; « entre chaque message interne au serveur et tchat global mettre une séparation avec des
-----GLOBAL------ et ------<Nom du serveur>------- pour les séparer proprement sans prendre trop de place » ;
« indiquer sur quel serveur est la personne via un préfixe qui correspond à la couleur du portail du serveur en
question et afficher le contenu du message d'une autre couleur, gris clair par exemple, pour que le tchat interne au
serveur reste blanc et soit distinctif ». Choix de LeKiwi06 : `true` / `false` = **afficher / masquer** le tchat
global (affiché par défaut) ; couleurs : Lobby bleu KaLium `#09add3`, Kal-Games or, Kixster vert, Event rouge, Kanvas
rose, Bingo (serveur-jeux) jaune.

- `/global send <message>` : le message est montré aux joueurs de tous les serveurs qui n'ont pas masqué le tchat
  global : `[Kal-Games] Pseudo : message` (préfixe dans la couleur du serveur de l'auteur, reste en gris clair).
- `/global false` : masque le tchat global pour soi ; `/global true` : le réaffiche. Choix gardé dans
  `plugins/KLM_Chat/masques.yml`. Envoyer un message alors qu'on l'a masqué reste possible (un rappel est affiché).
- **Séparations** : `----- GLOBAL -----` avant un message global quand le dernier tchat vu par le joueur n'était pas
  global ; `----- <nom du serveur> -----` (nom dans sa couleur) avant un message du tchat du serveur quand le dernier
  vu était global. Jamais deux séparations de suite.
- Permission `klmchat.global` (tout le monde par défaut). Messages globaux notés dans la console de chaque serveur
  qui les affiche (`[GLOBAL] [Serveur] Pseudo : message`).
- Le texte d'un joueur n'est jamais interprété (ni balises de couleur, ni liens cliquables, ni « § ») ; longueur
  limitée à 256 caractères.
- **Transport** : canal BungeeCord « Forward » vers tous les serveurs (Velocity, comme la navigation de KLM_Menu).
  Rien à installer sur le proxy. Le nom de chaque serveur est demandé au proxy à l'arrivée du premier joueur
  (`GetServer`), ou fixé par `server-name`.
- `config.yml` (toutes les clés facultatives, valeurs par défaut dans le code) : `server-name`, `max-length`,
  `servers.<nom velocity>.name | color`, `messages.format | separator-global | separator-server`.

**Limites connues** :
- `/global true | false` est un choix **par serveur** (chaque serveur garde son `masques.yml`) : le masquer au lobby
  ne le masque pas sur Kal-Games.
- Les séparations suivent le tchat des joueurs ; les autres messages d'un serveur (arrivées, morts, messages des
  plugins) ne sont pas suivis et peuvent apparaître sous une ligne `GLOBAL`.
- Un serveur sans joueur ne reçoit rien (sans conséquence : personne pour lire).

**Signalé, non fait (pas demandé)** : aucun délai entre deux messages (anti-spam) ; un joueur rendu muet par
LibertyBans peut encore écrire avec `/global` tant que la commande n'est pas ajoutée aux commandes bloquées de
LibertyBans ; aucune modération ni historique en dehors de la console.

**Statut : compilé le 03/10/2026, non déployé, non testé en jeu.**
