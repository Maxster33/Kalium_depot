# KLM_Espaces - journal

Plugin réseau (préfixe `KLM_`), pour un serveur Paper qui porte plusieurs noms Velocity : chaque nom est un « espace »
hermétique (ses mondes, ses inventaires, ses joueurs visibles, son tchat). Aucune dépendance à nos autres plugins :
ils lisent l'espace d'un joueur dans ses métadonnées `klm_espace` (identifiant) et `klm_serveur` (nom Velocity).
Exige **KaliumRelay 1.9.0** sur le proxy (nom demandé à chaque connexion, passage direct entre espaces refusé).

## 1.0.0 - Kal-Games et Kanvas sur le même serveur (21/10/2026, LeKiwi06)

**Demande de LeKiwi06** : Kal-Games et Serveur Jeux expirent dans 3 jours ; « on va fusionner kal-games et kanvas en
utilisant le même principe que pour les mondes simultanés dans serveur jeux » ; « les gens ne pourront pas passer de
kanvas à kal-games sans le lobby, tout sera sur le même serveur mais de façon hermétique, on fera même des tchats
séparés » ; inventaires séparés. Plan : [Plan de migration](https://claude.ai/artifact/MyTJxpL8zq1XCy1sTZKdZh).

- **Espaces** (`config.yml`, `espaces`) : noms Velocity (le premier est l'officiel), mondes (nom ou clé, `*` accepté),
  monde d'arrivée. Pour Kanvas : `kal-games` (mondes `Kal-Games`, `kalgames:*`, `buildbattle`) et `kanvas` (`Kanvas`).
- **Arrivée** : à la pré-connexion (hors du fil principal), le nom Velocity demandé est lu sur le relais (clé
  `arrivee-<uuid>`) ; inconnu : dernier espace du joueur (`derniers.yml`), sinon `espace-par-defaut`. Position : la
  dernière position du joueur si elle est dans son espace, sinon sa position sauvée dans cet espace, sinon le point
  d'apparition du monde d'arrivée (KalGames met ensuite au hub).
- **Inventaires séparés** : à chaque départ, inventaire, coffre de l'Ender, expérience, vie, faim, effets, mode de jeu,
  vol et position sont sauvés dans `etats/<espace>/<uuid>.yml`. À l'arrivée dans un autre espace que celui des données
  du joueur (marque dans le joueur), ces données sont d'abord sauvées pour leur espace, puis l'état de l'espace
  d'arrivée est rendu (rien la première fois : inventaire vide). Données d'avant KLM_Espaces : appartiennent à
  `espace-des-donnees-existantes` (`kanvas` : les inventaires de Kanvas sont gardés).
- **Visibilité** : les joueurs des autres espaces sont cachés (liste Tab comprise).
- **Tchat** : le tchat du serveur, les messages d'arrivée, de départ et de mort ne vont qu'aux joueurs du même espace
  (la console voit tout). Le tchat global `/global` (KLM_Chat) reste commun à tout le réseau.
- `/espaces [liste | recharger]` (opérateurs) : joueurs de chaque espace.

**Limites** : `/msg` et `/tell` de Minecraft peuvent viser un joueur d'un autre espace s'il est nommé ; Velocity compte
les joueurs par nom, donc la boussole du lobby affiche bien le nombre de chaque espace. Un `/reload` remet chaque
joueur dans l'espace de son monde.

**Compilé, non déployé, non testé en jeu.** À déployer sur Kanvas avec KaliumRelay 1.9.0 (proxy), KalGames 1.27.0,
KLM_Menu 2.12.0, KLM_Chat 1.1.0, KLM_Contacts 1.4.0, KG_BuildBattle 0.7.0, KV_BuildBattle 0.5.0.
