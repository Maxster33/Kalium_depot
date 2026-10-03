# JOURNAL — KG_BingoObservateur

Plugin Paper de **Serveur Jeux** (`serveur-jeux`) : les opérateurs observent n'importe quel joueur du serveur en mode
spectateur. Plugin séparé de KG_BingoGame (« un plugin = un rôle ») : il lit les parties et les équipes de
KG_BingoGame (`depend`) sans le modifier ; menus avec l'assistant `Gui` de KLM_Menu (`depend`). Compilation :
`build.sh` compile d'abord KG_BingoGame (qui compile KLM_Menu), sans jamais les embarquer dans le jar.

## 0.1.0 — création (03/10/2026, Maxster33)

**Demande de Maxster33** : « ajoute un plug in qui donne un objet aux opérateur permettant d'aller observer n'importe
quel joueur sur le serveur bingo en spectateur ».

**Choix de Maxster33** : objet donné par une commande ; les deux vues possibles (libre à côté du joueur, ou dans ses
yeux) ; fin par `/observer quitter` avec retour à la position d'avant ; liste de tous les joueurs, groupés par partie.

- **`/observer`** (permission `kgbingoobservateur.use`, opérateurs par défaut) : donne la longue-vue « Observer un
  joueur » (marque invisible, pas de nom à reconnaître). Clic droit avec elle (au lieu du zoom) : liste des joueurs
  connectés (sauf soi), triés par partie en cours (« Partie 1 · équipe A »), puis salles d'attente (« Salle d'attente de
  <hôte> »), puis le reste.
- Choix d'un joueur : **Vue libre** (mode spectateur, téléporté sur lui, même dans sa partie, son Nether ou son End ;
  clic gauche sur lui = vue par ses yeux, fonction normale du spectateur) ou **Dans ses yeux** (caméra attachée ; Maj
  pour se détacher ; tant qu'elle est attachée, elle suit le joueur quand il change de dimension, est téléporté ou
  réapparaît).
- En spectateur, Minecraft ne laisse pas utiliser d'objet : **`/observer`** ouvre alors directement la liste ;
  **`/observer <pseudo>`** : vue libre directe ; **`/observer quitter`** : retour à la position et au mode de jeu
  d'avant la première observation (salle d'attente si ce monde n'existe plus). L'inventaire n'est pas touché.
- **Retours automatiques** : partie observée terminée (vérifié chaque seconde : KG_BingoGame supprime les mondes d'une
  partie 5 s après sa fin et ne le peut pas avec quelqu'un dedans) ; déconnexion de l'opérateur (il se reconnecte à sa
  position d'avant) ; arrêt ou rechargement du plugin. Joueur observé déconnecté : message, l'opérateur reste sur place.
- Textes dans `plugins/KG_BingoObservateur/lang.yml` (créé au premier usage, modifiable).

Limites : un spectateur reste visible des autres spectateurs et apparaît en gris dans la liste Tab ; il compte dans le
nombre de joueurs en Bingo affiché à Kal-Games (KG_BingoGame 0.8.3).

**Déployé le 03/10/2026 à 10:18 sur Serveur Jeux (Maxster33 ; nouveau, rien à ranger), actif après redémarrage.
Statut : non testé en jeu.**
