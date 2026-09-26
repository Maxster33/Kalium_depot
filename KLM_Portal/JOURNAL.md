# KLM_Portal - journal

Plugin réseau (préfixe `KLM_`), installable sur n'importe quel serveur Paper. Dépend de **KLM_Menu 2.2.0** (destinations
et leur activation) et de **WorldGuard** (régions des portails ; WorldEdit ou FAWE pour les dessiner).

## 1.0.0 — portails reliés aux destinations de KLM_Menu (26/09/2026)

**Demande de LeKiwi06** : remplacer ConditionalEvents et PyxelRegions (zones des portails du lobby, qui appelaient
VelocityCommandForward) par un KLM_Portal « relié au KLM_menu pour que les portails se déactivent quand on désactive
le bouton dans le KLM_menu ». Choix de LeKiwi06 : zones = **régions WorldGuard** (comme les autres plugins) ; portail
désactivé = **message et recul seulement** (le visuel viendra plus tard) ; utilisable **sur tous les serveurs**.

- Un portail = une région WorldGuard (dans un monde) + une destination de KLM_Menu, dans `config.yml` (`portals:`).
- Entrer dans la région envoie le joueur vers la destination, comme un clic sur son bouton (API de KLM_Menu 2.2.0 :
  changement de serveur par le canal BungeeCord, ou commande locale pour une entrée de `local-entries`). Déclenché
  seulement à l'**entrée** dans la région, puis délai de 3 s par joueur (`cooldown-seconds`) contre les envois en
  double.
- Destination désactivée dans KLM_Menu (menu > Paramètres) : l'entrée est annulée, le joueur est repoussé
  (du centre de la région vers lui, `push-strength`), message « Ce portail est désactivé. » dans la barre d'action
  (au plus toutes les 3 s). Aucun état propre à KLM_Portal : réactiver le bouton réactive le portail tout de suite.
- `/klmportal` (alias `/portail`, permission `klmportal.admin`, opérateurs) : `set <région> <destination>` (en jeu,
  dans le monde de la région ; autocomplétion des régions et des destinations ; avertit si la destination n'est pas
  un bouton de KLM_Menu), `remove <région>` (la région WorldGuard est gardée), `list` (état de chaque portail,
  région manquante signalée), `reload`.
- **Interface « Ajouter un portail »** (demande de LeKiwi06, même jour : « ajoute une interface "ajouter un portail"
  en tant qu'admin à la boussole ») : boussole > Interfaces > KLM_Portal (rubrique admin, opérateurs). Deux listes :
  régions WorldGuard du monde du joueur qui ne sont pas encore des portails, et destinations de KLM_Menu ; bouton
  « Créer le portail ». Sans région libre : rappel de la marche à suivre (baguette WorldEdit, `/rg define <nom>`).
  Création notée dans la console (« Portail ... relié à ... par <pseudo> »).
- Au démarrage : régions WorldGuard introuvables signalées dans la console (portail inactif).
- Textes dans `plugins/KLM_Portal/lang.yml` (créé à l'usage, modifiable).

Limites : un portail n'agit que sur les **déplacements à pied** (pas sur une téléportation, ex. perle, qui arriverait
dans la région) ; un joueur qui se connecte déjà dans la région n'est envoyé qu'après en être sorti et revenu.

**Déployé sur le lobby le 26/09/2026 18:25** (serveur éteint), avec KLM_Menu 2.2.0 (règle 3.5). LeKiwi06 confirme que
ConditionalEvents et PyxelRegions ne servaient qu'aux portails : jars rangés dans
`/plugins/_removed-conditionalevents-4.79.2/` et `/plugins/_removed-pyxelregions-1.2.2/` (dossiers de données
`ConditionalEvents/` et `PyxelRegions/` laissés en place). Les anciens portails ne sont pas repris : LeKiwi06 recrée
les régions dans WorldGuard. VelocityCommandForward reste installé (pas demandé). **Testé et confirmé par LeKiwi06 le 26/09/2026** (portails et
leur désactivation).

## 1.1.0 — points de chute (26/09/2026)

**Demande de LeKiwi06** : « conditional event s'occupait de remettre tout le monde au centre du lobby quand on se
connecte au serveur lobby, il faudrait intégrer ça au plugin en mettant un point de chute pour chaque téléportation
interserveur avec : coordonnées ou dernière position dans le serveur de destination ». Choix de LeKiwi06 : réglé
**côté départ**, dans KLM_Portal, avec une interface dans la boussole et une commande.

- **Côté départ** (`landings:` dans `config.yml`) : pour chaque serveur de destination, `derniere-position` ou
  `coordonnees` (x, y, z, yaw, pitch, monde ; monde vide = monde principal de l'arrivée). Avant CHAQUE changement de
  serveur fait par KLM_Menu (boussole, `/lobby`, portails : nouvelle API `setBeforeConnect` de KLM_Menu 2.3.0), la
  consigne est déposée dans **KaliumRelay** (entrée `landing-<uuid>` de `/assignment/`, sans modification du relais ;
  jamais confondue avec les affectations du Bingo, qui utilisent l'UUID seul), puis le joueur est envoyé (attente de
  3 s au plus). Nécessaire techniquement : le canal BungeeCord ne peut rien livrer à un serveur vide.
- **Côté arrivée** (KLM_Portal doit y être installé) : à la connexion, la consigne est lue et effacée, puis appliquée.
  Sans consigne (connexion directe au réseau, départ sans KLM_Portal, destination sans point de chute) :
  `default-arrival` de ce serveur (`derniere-position` ou `coordonnees`). Ajout nécessaire pour reprendre le
  comportement de ConditionalEvents (connexion directe au lobby → centre), signalé à LeKiwi06.
- `server-name` (nom de ce serveur dans `velocity.toml`) : une consigne destinée à un autre serveur (envoi raté puis
  connexion ailleurs dans les 2 minutes de vie d'une entrée du relais) est ignorée. Vide = pas de vérification.
- Interface : boussole > Interfaces > KLM_Portal > **Points de chute** : « Arrivée sur ce serveur » (Définir ici /
  Dernière position) puis un bouton par serveur de destination (liste : non défini / dernière position / coordonnées,
  champs X, Y, Z, orientation, monde).
- Commandes : `/klmportal chute <serveur> aucun | derniere | <x> <y> <z> [yaw pitch] [monde]`,
  `/klmportal arrivee ici | derniere | info`.
- Nouvelles clés (`relay-url`, `relay-token`, `server-name`, `landings`, `default-arrival`) : valeurs par défaut dans
  le code (règle 3.3). `relay-token` : le `token` de `relay.properties` du proxy, à écrire **uniquement** sur les
  serveurs. Sans jeton, seule `default-arrival` fonctionne.

Limites : les coordonnées d'un autre serveur se relèvent là-bas (F3) puis se tapent ici ; un point de chute vers un
serveur n'agit que si KLM_Portal 1.1.0 (et donc KLM_Menu 2.3.0) y est installé ; les envois faits par d'autres
plugins (Bingo, KalGames) ne passent pas par KLM_Menu et n'ont donc pas de consigne (`default-arrival` s'applique).

Config préparée pour le lobby (4 portails de LeKiwi06, `server-name: lobby`, `default-arrival` = centre
`7.5 67 39.5` du monde `Lobby Kalium`, repris de l'événement `center_lobby` de ConditionalEvents) : dans les
Téléchargements de LeKiwi06 (`klm_portal_lobby/`), `relay-token` à remplir. **Statut : compilé, non déployé (déploiement
refusé par le contrôle des permissions de Claude, le lobby étant probablement allumé), non testé.**
