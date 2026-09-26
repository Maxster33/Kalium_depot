# Journal — KLM_Menu (anciennement KaliumMenu)

Le plugin s'appelait **KaliumMenu** jusqu'à la 1.5.0 (renommé en 2.0.0, voir la dernière section).

Menu de navigation (Dialog natif, boussole) entre le lobby et les serveurs du réseau KaLium. Un seul jar,
installé sur chaque serveur Paper ; `role: lobby` sur le lobby (liste des serveurs), `role: backend` ailleurs
(retour au lobby + entrées locales, ex. "Hub de Kal-Games").

## 1.5.0 — Paramètres des téléportations

**Demande explicite de l'utilisateur** : "ajouter à kalium menu un bouton paramètre pour les op, pour pouvoir
activer ou désactiver des téléportations". Précisions via AskUserQuestion : un interrupteur **par destination** ;
une destination désactivée est **cachée pour tout le monde** (opérateurs compris).

- Bouton "Paramètres" dans le menu pour les joueurs ayant `kaliummenu.admin` (opérateurs par défaut). Il ouvre la
  liste des destinations de CE serveur (serveurs sur le lobby ; "Retour au lobby" + entrées locales ailleurs,
  quel que soit le monde), chacune "activée"/"désactivée" ; un clic bascule et enregistre.
- Enregistré dans `config.yml` : `disabled-destinations` (identifiants : clés de `servers`, nom du lobby pour
  "Retour au lobby", clés de `local-entries`). Réglage propre à chaque serveur.
- `/lobby` refusé si "Retour au lobby" est désactivé ("Cette téléportation est désactivée."). La commande /hub de
  KalGames n'est pas concernée (seul le bouton du menu est caché).
- Nouveaux textes avec valeurs par défaut intégrées (rien à ajouter dans les config.yml déjà déployés).

**Déployé le 23/09/2026 sur kal-games** (1.4.0 archivée dans `_removed-kaliummenu-1.4.0/`). Lobby : pas d accès SFTP depuis cette session, à déployer par l utilisateur.

## 2.0.0 — KLM_Menu : interface globale du réseau (24/09/2026)

**Demande de LeKiwi06** : « KLM_Menu, c'est la partie navigation inter-serveur [...] la structure principale ; une fois
arrivé sur un serveur, le menu de ce serveur prend le relais ; KLM_Menu doit être présent sur tous les serveurs, pour
que les admins aient accès à tous les GUI depuis tous les serveurs [...] la couche profonde » ; « que KLM_Menu
interroge les plugins de chez nous pour savoir s'ils n'ont pas une interface à rajouter [...] une interface claire
pour trouver les interfaces de chaque chose ». Les mini-jeux gardent leurs propres interfaces.

- **Renommé** KaliumMenu → KLM_Menu (préfixe KLM_ = plugins communs au réseau). Classe principale `KlmMenu`, paquet
  `fr.kalium.menu` conservé. Commandes et permissions inchangées (`/servers`, `/lobby`, `/kaliummenu` + alias
  `/klm`, `kaliummenu.admin`, `kaliummenu.bypass`) pour ne rien casser. Les boussoles déjà données (étiquette
  `kaliummenu:menu_compass`) restent reconnues.
- **Boîte à outils des menus** (`fr.kalium.menu.api`) : `Gui` (menus Dialog) et `Lang` (textes, `lang.yml` de chaque
  plugin), repris de KalGames. KG_ScoreBoards l'utilise déjà ; les autres plugins y passeront au fil des découpages.
- **Catalogue des interfaces** : chaque plugin déclare ses interfaces (`MenuSection`) par le registre de services de
  Paper ; KLM_Menu les découvre au démarrage (journal : « N interface(s) trouvée(s) : ... ») et à chaque ajout ou
  retrait. Nouveau bouton **« Interfaces »** dans le menu de la boussole : un bouton par plugin, puis ses interfaces
  (celles réservées aux admins marquées « (admin) », visibles selon les droits de chacun).
- `load: STARTUP` (sans effet visible) : pour que KG_BingoGame, qui démarre avant les mondes, puisse en dépendre.
- Phase 2 prévue (non faite) : accès des opérateurs aux interfaces des AUTRES serveurs, via KaliumRelay. Limite
  connue : il faudra que les menus soient faits avec la boîte à outils ; les menus en coffre et les actions sur le
  joueur lui-même (téléportation, inventaire) ne pourront pas se faire à distance.

**Migration** (serveur arrêté, sur chaque serveur Paper qui avait KaliumMenu : kal-games, lobby) : déplacer
`KaliumMenu-1.5.0.jar` dans `/plugins/_removed-kaliummenu-1.5.0/` (+ copie de `plugins/KaliumMenu/config.yml`),
renommer `plugins/KaliumMenu/` en `plugins/KLM_Menu/`, envoyer `KLM_Menu-2.0.0.jar`. Nouveau sur Kixster : config par
défaut avec **`compass.enabled: false`** (la boussole est un objectif du Bingo : elle le validerait) ; menu par
`/servers`. **Statut : déployé le 24/09/2026 sur kal-games, lobby et Kixster, non testé en jeu.**

## 2.1.0 — la boussole entièrement gérée par KLM_Menu (24/09/2026)

**Demande de LeKiwi06** : « la boussole fait partie de l'interface [...] gérée sur tous les serveurs par le même
plugin ». Nouvelle méthode `giveNavigation(joueur)` : un plugin qui vide l'inventaire (KalGames au hub) demande à
KLM_Menu de remettre la boussole, au lieu de la commande console `/kaliummenu give`. En-tête de `config.yml` mis à
jour. **Statut : déployé sur Kal-Games (7001) le 24/09/2026 (avec KG_Menu 1.0.0), testé et confirmé par LeKiwi06 le 24/09/2026** (boussole verrouillée en joueur ; déplaçable par les opérateurs, permission `kaliummenu.bypass`, voulu) ; lobby, Kixster, Serveur Jeux et Kanvas restent en 2.0.0.

## Configuration du lobby — destination Kanvas (26/09/2026, pas de nouvelle version)

**Demande de LeKiwi06** : mettre à jour les noms de KLM_Menu. Dans `plugins/KLM_Menu/config.yml` du lobby, la
destination `kal-test-dev` (« Kal-Test-Dev », serveur de test) n'existait plus sur le proxy (renommé `Kanvas` dans
`velocity.toml`) : remplacée par **`Kanvas`** (« Kanvas » en orange `#FF8800`, « Serveur de plots en créatif :
construis, fais noter tes builds. »). Autres noms vérifiés, à jour. Original dans
`plugins/_removed-klm_menu-config-2026-09-26/config.yml` du lobby. S'applique avec `/kaliummenu reload` (opérateur).
La configuration par défaut du dépôt ne contenait pas `kal-test-dev`.
**Erreur de Claude, corrigée** : la première version de la description contenait « créatif : construis » sans
guillemets ; en YAML, « : » suivi d'un espace est invalide hors guillemets : après `/kaliummenu reload`, le fichier n'a
pas pu être lu et le menu ne proposait plus que le lobby. Description mise entre guillemets, fichier vérifié avec
snakeyaml avant envoi (5 destinations), renvoyé. À retenir : toujours vérifier un config.yml modifié avant de l'envoyer.
**Testé et confirmé par LeKiwi06 le 26/09/2026** (boussole du lobby).

## 2.2.0 — API des destinations pour KLM_Portal (26/09/2026)

**Demande de LeKiwi06** : remplacer ConditionalEvents et PyxelRegions (portails du lobby) par un plugin KLM_Portal
« relié au KLM_menu pour que les portails se désactivent quand on désactive le bouton dans le KLM_menu ».
Ajout nécessaire côté KLM_Menu (aucun changement visible en jeu) : trois méthodes publiques.
- `isDestinationEnabled(id)` : faux si la destination est dans `disabled-destinations` (menu > Paramètres).
- `destinationIds()` : identifiants des destinations de ce serveur (autocomplétion de KLM_Portal).
- `sendToDestination(joueur, id)` : même effet qu'un clic sur le bouton (entrée locale = sa commande, sinon
  changement de serveur par le proxy, avec l'orthographe exacte de la clé de `servers`, ex. `Kanvas`). Ne fait rien
  et renvoie `false` si la destination est désactivée.

Limite : un identifiant absent du menu de ce serveur est toujours considéré comme actif (il ne peut pas être désactivé).
À déployer **avec KLM_Portal 1.0.0** sur chaque serveur qui reçoit des portails (règle 3.5). Sur le lobby, remplace
la 2.0.0 (la 2.1.0 n'est que sur Kal-Games). **Déployé sur le lobby le 26/09/2026 18:25** (avec KLM_Portal 1.0.0 ;
2.0.0 dans `/plugins/_removed-klm_menu-2.0.0/`). **Testé et confirmé par LeKiwi06 le 26/09/2026** (portails).

## 2.3.0 — attente avant changement de serveur, pour les points de chute (26/09/2026)

**Demande de LeKiwi06** : points de chute de KLM_Portal 1.1.0 (voir son journal). Ajouts nécessaires :
- `setBeforeConnect(fonction)` : appelée avant CHAQUE changement de serveur fait par KLM_Menu (boussole, `/lobby`,
  `sendToDestination`) ; le message « Connexion à ... » s'affiche tout de suite, l'envoi attend la fin de la fonction
  (3 s au plus). Sans fonction : comme avant.
- `serverIds()` : noms des serveurs du menu de ce serveur (sans les entrées locales).

**Déployé sur le lobby le 26/09/2026 19:37** avec KLM_Portal 1.1.0 (2.2.0 dans `/plugins/_removed-klm_menu-2.2.0/`).
**Statut : non testé en jeu.**
