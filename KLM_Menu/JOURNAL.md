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
`/servers`. **Statut : compilé, non déployé.**
