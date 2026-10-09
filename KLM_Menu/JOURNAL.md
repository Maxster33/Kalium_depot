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
**Testé et confirmé par LeKiwi06 le 26-27/09/2026** (boussole, `/lobby`, portails, arrivée au centre du lobby) ;
points de chute vers d'autres serveurs non testés (`relay-token` vide, KLM_Portal absent des autres serveurs).

## Déploiement sur Kanvas (27/09/2026, pas de nouvelle version)

Demande de LeKiwi06 : le point de chute réglé au lobby pour Kanvas ne s'appliquait pas (Kanvas n'avait ni KLM_Portal ni
KLM_Menu 2.3.0, qui lisent la consigne à l'arrivée). **KLM_Menu 2.3.0 + KLM_Portal 1.2.0** installés sur Kanvas le 27/09/2026 à
19:59 (KLM_Menu 2.0.0 dans `_removed-klm_menu-2.0.0/`) ; `relay-token` de `plugins/KLM_Portal/config.yml` à remplir à
la main (Kanvas et lobby). Statut : testé et confirmé par LeKiwi06 le 27/09/2026 (point de chute vers Kanvas, boussole et étoile de
KV_Menu).

## 2.4.0 — comparateur « Informations », couleurs lisibles sur Bedrock, verrou pour tous (28/09/2026)

**Demandes de LeKiwi06 (28/09/2026)** : « certaines couleurs se voient très mal sur Bedrock, notamment le gris clair,
vert et vert clair, jaune pâle (toutes les couleurs trop claires) » ; « la boussole n'est pas lock in slot, on peut la
drop et la bouger » ; « sur lobby j'ai un bouton paramètres et un bouton interface, sauf que le bouton interface contient
uniquement des paramètres » ; « pour tout ce qui est classements et paramètres, tu vas faire ça dans un comparateur avec
texture enchantée en slot 1 de la hotbar qui portera le nom "Informations" ; pour les joueurs il contiendra que le
classement pour le moment, et pour les admins il doit aussi contenir les paramètres retravaillés ». Choix de LeKiwi06 :
comparateur sur tous les serveurs, case de gauche, verrou pour tout le monde (opérateurs compris).

- **Comparateur « Informations »** (texture enchantée, case de gauche = `informations.slot: 0`) : clic droit →
  **Classements** (interfaces déclarées comme classements, `MenuSection.ranking()`) pour tous, et **Paramètres** pour
  les admins (`kaliummenu.admin`). Donné aux admins partout, aux joueurs seulement s'il y a un classement sur le serveur
  (kal-games pour l'instant) ; retiré sinon (ex. un admin qui repasse joueur). Mêmes règles que la boussole (arrivée,
  réapparition, `giveNavigation`), jamais par-dessus un objet du joueur, retiré des objets lâchés à la mort.
  `informations.enabled` absent = comme `compass.enabled` : pas de comparateur sur Serveur Jeux, Kixster, Event (le
  Bingo a des objectifs, la survie a besoin de la barre). Là, **`/informations`** (alias `/infos`) ou `/menu` y mènent.
- **`/menu`** (demande de LeKiwi06, même jour : « /menu ouvre l'interface ; si le serveur ne possède qu'un item
  d'interface : ouvrir directement cette interface, s'il en possède plusieurs : menu de sélection ; /menu on/off sert à
  donner / retirer les divers items d'interface de la hotbar ; si des items sont dans les slots correspondants : refuser,
  sauf sur Kanvas comme on est en créatif ; durant un mini-jeu : refuser ») :
  - `/menu` : les objets d'interface proposés au joueur là où il est (étoile de kal-games ou de Kanvas, boussole =
    navigation, comparateur = informations) ; un seul → il s'ouvre, plusieurs → menu de choix. Marche aussi avec les
    objets retirés, et sur les serveurs sans boussole (la navigation y est toujours proposée).
  - `/menu off` : retire tous ces objets (inventaire entier) et n'en redonne plus, même après une mort ou une
    reconnexion ; `/menu on` : les remet à leurs cases. Mémorisé par serveur dans `plugins/KLM_Menu/objets-masques.yml`.
  - `/menu on` refusé si une de ces cases contient autre chose (message avec les numéros de case, 1 à 9), **sauf en mode
    créatif** : l'objet du joueur est déplacé dans l'inventaire (remplacé s'il n'y a plus de place, sans perte en
    créatif). Règle liée au mode de jeu, pas au serveur : Kanvas est concerné car on y est en créatif.
  - `/menu on` et `/menu off` refusés pendant une partie (KalGames : partie, gradins, spectateur ; Build Battle ; mondes
    du Bingo). `/menu` seul reste permis en partie (il ouvre par exemple le menu de la partie sur kal-games).
  - Nouvelles API : `InterfaceItem` (objet d'interface d'un plugin : nom, case, donner, ouvrir) et `PlayerActivity`
    (le joueur est-il en partie ?), par le registre de services ; `KlmMenu.itemsHidden(Player)`.
  - Sur Kixster et Event, `/menu` était un alias de `/servers` dans `commands.yml` : **retirer cet alias au
    déploiement**, sinon il masque la nouvelle commande. Sur Serveur Jeux, l'ancien `/menu` de KG_BingoGame (menu de la
    salle d'attente) devient `/bingomenu` (0.8.3).
- **Paramètres** : les réglages de chaque plugin **à plat**, un bouton chacun (interfaces ADMINS ; une interface peut en
  fournir plusieurs avec `MenuSection.expand`, ex. KG_Menu : un par jeu), puis « Téléportations » (l'ancien
  « Paramètres » de la boussole : activer / désactiver les destinations et les portails reliés).
- **Boussole = navigation seulement** : boutons « Interfaces » (catalogue par plugin, supprimé) et « Paramètres » retirés.
  Restent les destinations, « Passer opérateur / joueur » et « Mode : ... ».
- **Verrou pour tout le monde** : la permission `kaliummenu.bypass` n'a plus d'effet (la boussole était volontairement
  libre pour les opérateurs depuis la 2.1.0). Boussole et comparateur ne se jettent, ne se déplacent ni ne s'échangent
  avec la main secondaire. En créatif (le jeu du joueur décide seul du contenu des cases), une case qui contient un de
  ces objets ne peut plus être remplacée, et l'inventaire est renvoyé au joueur après chaque tentative refusée.
- **Bedrock** (`fr.kalium.menu.api.BedrockColors`) : Geyser affiche les menus sur fond clair ; pour un joueur Bedrock
  (identifiant Floodgate), toute couleur trop claire (luminance > 0,15 : gris clair, blanc, vert, jaune, aqua, or,
  rose, le bleu KaLium...) est assombrie en gardant sa teinte, dans le titre, les textes, les boutons, les info-bulles
  et les champs. Les joueurs Java ne voient aucun changement. Appliqué par `Gui.open` (tous les plugins qui utilisent la
  boîte à outils) et par les menus propres de KLM_Menu ; KalGames 1.21.0 et KG_BingoGame 0.8.3 l'utilisent aussi.
- API (compatible avec les plugins compilés avant) : `MenuSection.ranking()`, `order()`, `expand(Player)`,
  `MenuSection.of(..., ranking, order, ...)` ; `KlmMenu.openInformations`, `openParametres`, `isInformations`.

Limites : le seuil de 0,15 est calculé pour le gris clair des boutons Bedrock (contraste d'au moins 3 contre 1) ; à
revoir en jeu si un titre devient trop foncé. Les interfaces « joueurs » qui ne sont pas des classements (accueil
Kal-Games, Kanvas) ne sont plus listées par KLM_Menu : elles ont leur objet (étoile). Textes du comparateur : `lang.yml`
de KLM_Menu (`info.*`).

**Déploiement** : sur **tous les serveurs Paper** (les plugins ci-dessous en dépendent), avec KG_Menu 1.1.0, KalGames
1.21.0, KG_ScoreBoards 1.6.0, KG_Bingo 1.6.0, KG_BuildBattle 0.2.0 (kal-games), KG_BingoGame 0.8.3 (Serveur Jeux),
KV_BuildBattle 0.3.6 et KV_Menu 1.3.1 (Kanvas). Aucune clé de config obligatoire. **Statut : déployé le 28/09/2026 à 3 h 40 (ancien jar dans `_removed-…`), non testé en jeu.**

## 2.4.1 — objets de menu retirés par défaut sur Kixster et Event, /menu on pour les avoir (28/09/2026)

**Retour de LeKiwi06 (28/09/2026)** : « le /menu fonctionne bien dans Kixster et Event, mais il ne donne pas la boussole
quand je fais /menu on (par défaut il doit être off sur ces serveurs pour pas déranger les joueurs, mais il faut laisser
la possibilité pour ceux qui préfèrent avoir la boussole, les items) ». Cause : `compass.enabled: false` sur ces deux
serveurs interdisait la boussole, même après `/menu on`.
- Nouvelle clé **`items-by-default`** (absente = `true`) : à `false`, un joueur n'a aucun objet de menu tant qu'il n'a pas
  fait `/menu on` ; son choix est gardé (`objets-masques.yml` : `joueurs` = `/menu off`, `affiches` = `/menu on`).
- `compass.enabled: false` garde son sens : jamais de boussole, même avec `/menu on` (Serveur Jeux : la boussole est un
  objectif du Bingo).
- Configs modifiées le 28/09/2026 (accord de LeKiwi06) : **Kixster et Event** : `compass.enabled: true` +
  `items-by-default: false` (anciennes configs dans `/plugins/_removed-klm_menu-2.4.0/`).

**Déployé sur Kixster et Event le 28/09/2026 à 4 h 03** (serveurs allumés, pris en compte au redémarrage ; 2.4.0 dans
`/plugins/_removed-klm_menu-2.4.0/`). Lobby, kal-games, Serveur Jeux, Kanvas restent en 2.4.0 (même comportement sans la
clé). **Statut : non testé en jeu.**

## 2.5.0 - aucun texte qui défile dans les menus (01/10/2026, LeKiwi06)

Demande de LeKiwi06 : des textes trop longs « défilent » dans les boutons et les jauges ; « il ne faut jamais que ça
arrive, c'est illisible » ; « il faut régler ce problème sur tous les plugins ».

- **Nouvelle classe `fr.kalium.menu.api.Lisible`** : mesure la largeur d'un texte avec la police de Minecraft (avance
  de chaque caractère, gras +1, accents comme la lettre de base) et ajuste un menu avant de l'afficher :
  - boutons d'un même menu élargis à la largeur du plus long texte (au moins la largeur prévue, 400 pixels au plus) ;
    moins de colonnes si elles ne tiennent plus à l'écran (500 pixels) ;
  - listes déroulantes (« libellé: option ») et curseurs (« libellé: valeur ») élargis à leur plus long texte ;
  - un texte qui dépasserait encore 400 pixels est signalé une fois dans la console (« Texte trop long [...] à
    raccourcir »).
- Appliqué dans `Gui.open` (donc à **tous les plugins qui utilisent la boîte à outils** : KG_Menu, KG_ScoreBoards,
  KG_BuildBattle, KLM_Portal, KV_Menu, KV_BuildBattle, KS_Menu, KS_Economy, KS_BiomeChanger, KS_Dimensions, sans les
  recompiler : la classe est chargée depuis KLM_Menu) et dans le menu de la boussole (destinations).
- Utilisable par les menus construits à la main : `Lisible.ajuster(champs, boutons, sortie, colonnes)` (KalGames
  1.22.1, KG_BingoGame 0.8.4, KS_Economy 1.0.3).
- Les textes déjà enregistrés dans les `lang.yml` des serveurs sont pris en compte (mesure à l'affichage).
- Vérifié dans tout le dépôt : aucun libellé de bouton ou de champ ne dépasse 400 pixels (les plus longs textes sont
  des info-bulles, qui passent à la ligne). KS_FioleExp : boutons courts, rien à changer. KaliumCore (hors service) :
  non traité.

**Déployé le 01/10/2026 à 23:42 (LeKiwi06) : KLM_Menu 2.5.0 sur les 6 serveurs Paper (2.4.0 dans
`_removed-klm_menu-2.4.0/` sur lobby, Kal-Games, Serveur Jeux, Kanvas ; 2.4.1 dans `_removed-klm_menu-2.4.1/` sur
Kixster et Event), KalGames 1.22.1 (`_removed-kalgames-1.22.0/`), KG_BingoGame 0.8.4 (`_removed-kg_bingogame-0.8.3/`),
KS_Economy 1.0.3 (`_removed-ks_economy-1.0.2/`) ; actifs après redémarrage de chaque serveur. Statut : **testé et confirmé par LeKiwi06 le 02/10/2026** (menus : plus aucun texte qui défile).**

## 2.6.0 - bouton Récompenses (03/10/2026, LeKiwi06)

Catégorie 4 « Récompenses » : bouton « Récompenses » dans le comparateur « Informations » ; il ouvre l'interface
Récompenses **du serveur où l'on est** (progression des joueurs ; configuration pour les admins). Nouvelle API
`fr.kalium.menu.api.Recompenses` (`owner()`, `ouvrir(joueur)`), déclarée par le plugin de récompenses du serveur dans le
ServicesManager : KG_Rewards (kal-games), KS_RewardsGUI (Event), KV_Rewards (Kanvas, plus tard). Sans plugin de
récompenses, rien ne change. Le comparateur est donné dès qu'un serveur a un plugin de récompenses.

**Déployé sur Event, Kal-Games et Kanvas le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (ancienne version dans `_removed-klm_menu-2.5.0/`), actif après redémarrage. Statut : non testé en jeu.**

## 2.7.0 - rubrique « Modération » dans /menu (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faut que les onglets de modération soit visible depuis le /menu dans le rubrique
modération , c'est ici qu'on verra les invsee , ECsee , les proba de minage , les indices de suspicions etc ».
- API : `MenuSection.moderation()` (défaut : non) et le raccourci `MenuSection.moderation(plugin, id, permission,
  ordre, titre, description, ouverture)` : outil de modération visible avec la permission du plugin (et les
  opérateurs). Ces outils ne vont jamais dans « Paramètres ».
- `/menu` : bouton « Modération » (après les menus du serveur) dès qu'un outil est visible pour le joueur ; la
  rubrique liste les outils, avec « Retour ». Sans outil : `/menu` inchangé.
- Compatible avec les plugins compilés pour 2.6.0 (méthode ajoutée avec une valeur par défaut). Cahier 6 mis à jour
  (l'interface staff de KS_AntiCheat ira dans cette rubrique, plus dans les Paramètres).

**Déployé sur Event seulement le 03/10/2026 à 03:22 (LeKiwi06 ; 2.6.0 dans `_removed-klm_menu-2.6.0/`) avec KS_Economy 1.1.4, actif après redémarrage d'Event ; Kal-Games et Kanvas restent en 2.6.0, lobby, Serveur Jeux et Kixster en 2.5.0. Statut : non testé en jeu.**

## 2.8.0 - Modération dans la navigation de la boussole (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : « inclus le bouton de modération dans navigation sur la boussole, histoire qu'il ne soit plus
seulement accessible par le /menu ». Le menu de navigation (boussole, /servers) affiche « Modération » (après les
destinations) dès qu'un outil de modération est visible pour le joueur ; « Retour » ramène là d'où la rubrique a été
ouverte (navigation ou /menu). Aucun autre changement.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 2.8.0 ; ancienne version dans `_removed-klm_menu-2.7.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 2.9.0 - bouton « Recherche de joueurs » dans la boussole (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : « un bouton supplémentaire dans la boussole de KLM_menu "recherche de joueurs" : s'il y a moins
de 4 joueurs dans le serveur où se trouve la personne », qui propose la commande `/global send/true/false <message>`,
de « rejoindre le discord » (lien + bouton qui donne le QR code en main gauche) et de « repasser quand kiwi est en
live » (lien du Twitch + bouton QR code).
- Menu de navigation (boussole, `/servers`) : bouton « Recherche de joueurs » après les destinations, tant qu'il y a
  **moins de 4 joueurs sur ce serveur** (`player-search.below`, 0 = jamais).
- La fenêtre rappelle `/global send <message>`, `/global false` et `/global true` (plugin KLM_Chat), affiche le lien
  du Discord et celui du Twitch (`player-search.discord`, `player-search.twitch`), puis « Retour ».
- Nouvelle API `fr.kalium.menu.api.QrCodes` (`owner()`, `donner(joueur, lien, nom)`) : là où un plugin la fournit
  (KLM_Hub, lobby), deux boutons « QR code du Discord » et « QR code du Twitch ». Sans ce plugin : les liens seulement.
- Nouvelles clés facultatives (valeurs par défaut dans le code, rien à ajouter aux `config.yml` déjà en place).

Limite : le bouton n'apparaît que là où la boussole ou `/servers` sont utilisés (pas de boussole sur Serveur Jeux ;
sur Kixster et Event, après `/menu on`). À déployer avec KLM_Hub 1.0.0 sur le lobby.

**Déployé sur les 6 serveurs Paper le 03/10/2026 à 22:47 (LeKiwi06 ; anciennes versions dans `_removed-klm_menu-2.5.0/` (lobby, Kixster, Serveur Jeux), `2.6.0/` (Kal-Games, Kanvas), `2.8.0/` (Event)), actif après redémarrage. Statut : non testé en jeu.** Les versions 2.6.0 à 2.8.0 n'ont pas encore été
testées en jeu (signalé à LeKiwi06).

## 2.10.0 - interfaces de joueur dans « Informations » (bouton « Contacts ») (09/10/2026, LeKiwi06)

Demande de LeKiwi06 (KLM_Contacts, cahier validé le 09/10/2026) : un bouton « Contacts » dans le comparateur
« Informations », sur tous les serveurs. Jusqu'ici une interface de joueur qui n'était pas un classement n'y était pas
affichée. Empilé sur la 2.9.0, non testée en jeu : nécessaire pour la demande, signalé à LeKiwi06.
- `MenuSection.informations()` (faux par défaut) : une interface qui répond vrai a son bouton directement dans
  « Informations », après « Classements » et « Récompenses », avant « Paramètres ».
- Un joueur qui voit une telle interface reçoit le comparateur, même sans classement sur ce serveur. **Conséquence :
  avec KLM_Contacts installé, tous les joueurs ont le comparateur sur tous les serveurs** (sauf là où les objets de
  menu sont masqués par défaut, Kixster et Event : `/menu on`, ou `/menu`).
- Ajout seulement : les plugins compilés contre la 2.9.0 fonctionnent sans changement.

**Déployé sur les 6 serveurs Paper le 09/10/2026 à 06:06 (LeKiwi06 ; 2.9.0 dans `_removed-klm_menu-2.9.0/`), actif après redémarrage de chaque serveur. Statut : non testé en jeu.**

## 2.11.0 - menus de type contenant dans la boîte à outils (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage d'interfaces
de type contenant quand c'est possible au lieu des boutons ». Le premier menu converti (`/rewards`, KS_RewardsGUI 1.4.0)
a été validé en jeu par lui le 09/10/2026 (« ça marche, passe aux écrans de l'économie ») : sa brique passe dans la
boîte à outils pour servir à tous les plugins.

- **`fr.kalium.menu.api.Contenant`** : un coffre de 1 à 6 rangées dont chaque case porte un objet et, au besoin, une
  action. `poser(case, objet, action)`, `barre()` (dernière rangée en vitres grises), `objet(...)` (objet de menu :
  nom et lignes sans italique, attributs masqués ; à partir d'un matériau ou d'un objet existant), `decor()`,
  `lignes(texte, couleur)` (coupe une description à 35 caractères), `message(joueur, texte, refus)` (message au-dessus
  de la barre d'objets, avec un son), `pour(joueur, titre, marque)` (reprend le menu déjà ouvert de la même marque et le
  vide, pour le remplir à nouveau sans le refermer).
- **KLM_Menu enregistre l'écoute commune** (`Contenant.Ecoute`) : tout clic et tout glisser dans un tel menu est annulé
  (rien ne se prend ni ne se dépose), le clic sur une case à action la déclenche avec un son de bouton.
- Modèle à suivre (dans le commentaire de la classe) : 6 rangées, objets cliquables en haut (45 par page), barre
  d'actions en bas ; une saisie de texte ou de nombre reste une fenêtre de `Gui`, ouverte depuis une case.
- Ajout seulement : `Gui` et les plugins compilés contre la 2.10.0 fonctionnent sans changement. KS_RewardsGUI 1.4.0
  garde pour l'instant sa propre copie de la classe.
- **Complété le même jour pour KS_Economy 1.5.0** (premier plugin à s'en servir, 28 écrans) :
  `pour(joueur, rangées, titre, marque)` (petit menu, confirmation) et `bas(colonne)` (case de la barre d'actions) ;
  `lignes(texte mis en forme)` (coupe un texte de `lang.yml` ; un texte court est gardé tel quel) ;
  `saisie(joueur, fenêtre)` : ouvre une fenêtre de `Gui` depuis un menu (le coffre est refermé d'abord ; un joueur
  Bedrock attend 5 ticks, délai choisi par Claude, à vérifier en jeu) ; `fermer(joueur)` et `apres(suite)` : pendant
  un clic, le jeu interdit d'ouvrir ou de fermer une fenêtre, donc `ouvrir`, `fermer` et `saisie` attendent la fin du
  clic (un tick) quand ils sont appelés depuis l'action d'une case.

**Compilé le 09/10/2026, non déployé (à envoyer sur les serveurs avec le premier plugin qui s'en sert). Statut : non testé en jeu.**

