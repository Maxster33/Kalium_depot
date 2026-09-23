# Journal de session — KaliumCore

Ce fichier sert de mémoire de reprise entre deux sessions pour le plugin **KaliumCore**. À lire en
premier au début d'une nouvelle session si le fil de conversation n'est pas disponible (voir aussi
`/home/claude/KalGames/JOURNAL.md` pour le contexte général du réseau et les règles de déploiement
communes à tous les plugins KaLium).

## ⏸ Projet en pause depuis le 22/09/2026

L'utilisateur a mis ce projet **en pause** le 22/09/2026 pour reprendre les modifications sur
**KalGames** (sur le serveur **Kal-Games**, à ne pas confondre avec Kal-Test-Dev) — pas d'abandon, juste
une priorité différente pour l'instant. **Aucune tâche ouverte/bloquante côté KaliumCore** : la refonte
1.4.0 du module commerce (voir "Révision 1.4.0" plus bas) est **entièrement terminée, compilée,
livrée et déployée** sur Kal-Test-Dev. État exact au moment de la pause :

- **Version active sur Kal-Test-Dev : `KaliumCore-1.4.0.jar`** (124 Ko), déployé le 22/09/2026 via
  WinSCP (téléchargement confirmé par l'utilisateur, upload direct). Ancien jar `KaliumCore-1.3.1.jar`
  déplacé — pas supprimé — dans `/plugins/_removed-kaliumcore-1.3.1/` (vérifié par capture d'écran
  WinSCP après coup).
- **Deux points restent ouverts, à relancer avec l'utilisateur dès la reprise de ce projet** (aucun des
  deux n'empêche de considérer le travail "fini" pour l'instant, ce sont juste des suivis) :
  1. **Confirmation des tests en jeu post-1.4.0** : l'utilisateur n'a pas encore confirmé avoir
     redémarré Kal-Test-Dev ni testé le nouveau flux Commerce en jeu — voir la checklist complète dans
     "Pour reprendre la prochaine fois" tout en bas de ce fichier (menu Dialog "Commerce", Mettre en
     vente avec durée 1-7j, Mes ventes/Marché/Recherche/Rewards paginés à 45/page avec nav en bas à
     droite, recherche FR/EN, historiques séparés en Dialog).
  2. **Hypothèse à faire confirmer/infirmer par l'utilisateur** : le bouton **"Vente flash"** a été
     conservé dans le menu Commerce alors qu'il n'était pas listé dans le dernier cahier des charges de
     l'utilisateur (redesign complet abandonnant les onglets/l'enclume) — décision prise pour ne pas
     supprimer silencieusement une fonctionnalité admin existante (programmation de ventes flash depuis
     Paramètres), mais **jamais explicitement validée par l'utilisateur**. À lui redemander : garder ou
     retirer ce bouton ?
- **Pour reprendre une session sur ce projet plus tard** : relire ce fichier en entier (surtout
  "Révision 1.4.0" et "Pour reprendre la prochaine fois" tout en bas), poser les 2 questions ci-dessus
  à l'utilisateur, puis reprendre selon sa réponse (rien à coder dans l'immédiat sauf s'il signale un
  bug en jeu ou demande le retrait de Vente flash).
- **Aucun fichier en cours d'édition, aucun build en attente** : le dépôt source
  (`/home/claude/KaliumCore`) est dans un état propre et compilable (dernier `sh build.sh` réussi,
  0 erreur), rien à nettoyer avant de passer à autre chose.

## Contexte

- Nouveau plugin de menu pour serveur de **survie longue durée**, en remplacement de KaliumMenu sur
  **Kal-Test-Dev** (le serveur de test, dont la map a été régénérée vierge le 22/09/2026).
- KalGames et KaliumMenu ont été retirés proprement de Kal-Test-Dev (déplacés, pas supprimés, dans
  `/plugins/_removed-kalgames-kaliummenu/` sur le serveur) avant de commencer ce plugin.
- Source : `/home/claude/KaliumCore`. Build : `/home/claude/KaliumCore/build.sh` (même principe que
  KalGames : ECJ, cible Java 21, jar écrit dans `/mnt/user-data/outputs/`). Penser à synchroniser
  `VERSION=` dans `build.sh` et `<version>` dans `pom.xml` avant de builder.

## Pourquoi ce plugin (demande initiale de l'utilisateur)

- Reprendre le modèle de KaliumMenu (Dialog natif Paper, aucun coffre) mais **sans objet verrouillé
  en barre d'action** : en survie, un emplacement de hotbar bloqué est trop gênant. Le menu s'ouvre
  uniquement par commande.
- Boutons du menu principal : **Retour au lobby** (tout le monde), **Statistiques** (tout le monde,
  ses propres stats), **Paramètres** (opérateurs uniquement).
- Le plugin doit être **extensible** : des modules optionnels seront ajoutés plus tard (système de
  commerce, système de claim), configurables/activables-désactivables depuis Paramètres. Quand un
  module est désactivé, son bouton de menu ET ses statistiques associées disparaissent.
- But affiché : un plugin **complet et réutilisable** tel quel sur d'autres serveurs, y compris
  temporaires → pas de base de données externe, tout est autonome dans le dossier du plugin
  (YAML par joueur).

## Architecture

- `fr.kalium.core.KaliumCore` (classe principale) : charge la config, enregistre les commandes
  `/kalium` (alias `/menu`, ouvre le menu) et `/kaliumcore` (alias `/kcore`, admin : `reload`), gère
  le canal BungeeCord (`Connect` vers `lobby-server`) pour le retour au lobby, expose les services
  (`dialogs()`, `modules()`, `stats()`, `menu()`, `claims()`, `claimsMenu()`, `claimsAdminMenu()`) aux
  autres classes, et le texte MiniMessage (`msg(clé)`, avec fallback FR codé en dur comme dans
  KaliumMenu).
- `fr.kalium.core.menu.DialogHelper` : utilitaire partagé (construit avec `KaliumCore`) pour éviter de
  dupliquer la construction de `Dialog`/`ActionButton`/`DialogAction.customClick` dans chaque écran.
  `button`/`rawButton` (boutons simples), `inputButton`/`rawButton` avec `BiConsumer<DialogResponseView,
  Player>` (boutons qui lisent des champs de saisie), `show(...)` (assemble et affiche le Dialog, avec
  ou sans `DialogInput`). Utilisé par `MenuService`, `ClaimsMenu`, `ClaimsAdminMenu`.
- `fr.kalium.core.module.ModuleRegistry` : **point d'extension** pour les modules optionnels.
  `ModuleInfo(id, displayName, menuLabel, menuDescription, defaultEnabled, menuAction, settingsAction)`
  — `menuAction` est appelé quand le joueur clique le bouton du module dans le menu principal ;
  `settingsAction`, s'il est non-null, fait apparaître dans Paramètres un bouton de **navigation** vers
  un sous-écran dédié au lieu d'un simple interrupteur on/off (utilisé par le module claims, qui a
  besoin de plusieurs réglages). État on/off lu/écrit dans `config.yml` (`modules.<id>.enabled`).
  Modules enregistrés actuellement : `stats` (pas de settingsAction, juste affiché/masqué) et `claims`
  (settingsAction → `ClaimsAdminMenu.openSettings`).
- `fr.kalium.core.stats.*` : suivi des statistiques.
  - `PlayerStats` : temps de jeu (secondes), niveaux XP dépensés, blocs cassés, blocs posés,
    monstres tués.
  - `StatsService` : cache mémoire + persistance **1 fichier YAML par joueur** dans
    `plugins/KaliumCore/playerdata/<uuid>.yml`. Temps de jeu accumulé via horodatage join/quit,
    "flush" périodique (`stats-autosave-seconds`, 300s par défaut) pour ne pas perdre la session en
    cours en cas de crash, sauvegarde systématique à la déconnexion et à l'arrêt du plugin.
  - `StatSection` (interface) : une section de l'écran Statistiques. `moduleId() == null` = section
    de base toujours affichée ; `moduleId() != null` = masquée dès que le module correspondant est
    désactivé (`ModuleRegistry.isEnabled`). **Important : désactiver le module ne fait que masquer
    l'écran** — `StatsListener` continue d'enregistrer en arrière-plan quoi qu'il arrive (demande
    explicite de l'utilisateur en 1.1.0).
  - `CoreStatsSection` : les 5 statistiques de base, `moduleId() = "stats"` (masquable désormais).
  - `StatsListener` : `PlayerJoinEvent`/`PlayerQuitEvent` (sessions), `BlockBreakEvent`/
    `BlockPlaceEvent`, `PlayerLevelChangeEvent` (diff négatif = niveaux dépensés : enchantement,
    enclume...), `EntityDeathEvent` (killer joueur + entité `instanceof Monster`).
- `fr.kalium.core.menu.MenuService` : écran principal (`openMain`) et Statistiques (`openStats`),
  Paramètres (`openSettings`). `openMain` : Retour au lobby, puis **Retour à la base** (si le module
  claims + l'option "home-chunk" sont actifs, téléporte vers `claims().homeLocation(...)`), puis un
  bouton par module actif (boucle sur `ModuleRegistry.all()`), puis Paramètres (admins). `openSettings`
  boucle sur les modules : bouton de navigation si `settingsAction` non-null, sinon interrupteur on/off
  classique.
- `fr.kalium.core.claims.*` (**nouveau en 1.1.0**) : système de claim de chunks (voir section dédiée
  ci-dessous).
- `fr.kalium.core.market.*` (**nouveau en 1.3.0**) : système de commerce entre joueurs (voir section
  dédiée ci-dessous).
- Accès au bouton Paramètres et à `/kaliumcore reload` : `KaliumCore.isAdmin(player)` = permission
  `kaliumcore.admin` (default: op) **ou** pseudo/UUID listé dans `operators` du config.yml (même
  mécanisme que KaliumMenu, pour rester cohérent). Nouvelle permission dédiée à la protection des
  claims : `kaliumcore.claims.bypass` (default: op) — ignore casse/pose/interaction/PvP protégés.

## Module claims (nouveau en 1.1.0)

Demande initiale complète de l'utilisateur : joueurs claim/unclaim le chunk où ils se trouvent depuis
le menu ; le propriétaire d'un chunk claim est protégé (PvP, casse, pose, interaction avec les blocs
tels que coffres/établis/enclumes) ; un chunk "principal" (base) définissable par le joueur, par défaut
le premier chunk claim ; un nombre d'emplacements ("claim slots") par joueur, 6 par défaut, réglable en
Paramètres, sans limite du nombre de chunks au-delà de ça (un admin peut toujours en donner plus,
notamment comme récompense d'event/quête) ; consultation et ajustement des emplacements de chaque
joueur (en ligne ou non) depuis Paramètres ; conversion emplacement ↔ objet transportable (pensé pour
un futur système de commerce). **Chaque mécanique est activable/désactivable** : un interrupteur
maître (module entier) dans "Options actives" (Paramètres), et des réglages plus fins dans
Paramètres > Claims > Options (voir ci-dessous ; la protection est scindée en 4 volets indépendants
depuis 1.2.0, suite à une demande explicite).

- `ClaimsService` : cœur logique. Clé de chunk `"world:x:z"` (le `:` est volontaire, car le `.` est
  déjà le séparateur de chemin des `ConfigurationSection` Bukkit — permet d'utiliser directement les
  clés de chunk comme chemins YAML). Stockage autonome **1 seul fichier** `plugins/KaliumCore/
  claims.yml` (pas de base de données externe, cohérent avec le choix déjà fait pour les stats) :
  `players.<uuid>.{slots, home}` et `claims.<chunkKey> = <uuid propriétaire>`. `claim`/`unclaim`
  retournent une enum de résultat (`OK`, `ALREADY_CLAIMED`/`NOT_CLAIMED`/`NOT_OWNER`, `NO_SLOTS`,
  `DISABLED`) pour que l'écran affiche le bon message. `claim()` : si `home-chunk` actif et que le
  joueur n'a pas encore de base, le premier chunk claim devient automatiquement sa base. `unclaim()` :
  si le chunk libéré était la base, elle redevient `null` (pas de réassignation automatique — le
  joueur redéfinit manuellement). `convertSlotToItem`/`convertItemToSlot` : gèrent la conversion
  emplacement ↔ `ClaimToken` (attention ItemStack : mutation via `getItem(slot)`/`setItem(slot, ...)`
  + `player.updateInventory()`, jamais en mutant en place l'ItemStack renvoyé par `getContents()`, qui
  ne se répercute pas toujours côté client). `adjustSlots(uuid, delta)` : utilisé par l'écran admin,
  et c'est le point d'entrée que d'autres systèmes (event, quête automatisée) pourront appeler pour
  donner des emplacements en récompense.
  Réglages lus depuis `config.yml` (`modules.claims.*`, tous par défaut `true` sauf `default-slots` =
  6) : `masterEnabled` (via `ModuleRegistry`, masque tout le module), `claimActionEnabled`,
  `unclaimActionEnabled`, `homeEnabled`, `itemConversionEnabled`, et depuis 1.2.0 la protection
  éclatée en 4 : `breakProtectionEnabled`, `placeProtectionEnabled`, `interactProtectionEnabled`,
  `pvpProtectionEnabled` (chacun avec getter + setter, clés config `protection-break/-place/
  -interact/-pvp`). `homeEnabled`/`itemConversionEnabled` : retirés de tout écran en 1.2.0 puis
  **remis en 1.2.1** (l'utilisateur les avait juste oubliés dans sa première liste, pas voulu les
  retirer) — voir `ClaimsAdminMenu.openOptions` ci-dessous, 8 cases à cocher au total désormais.
- `PlayerClaims` : simple porteur de données (`uuid`, `slotsTotal`, `home` nullable).
- `ClaimToken` : objet transportable représentant un emplacement (matériau/nom/lore configurables sous
  `modules.claims.token`, PAPER par défaut), identifié via `PersistentDataContainer` (clé
  `claim_token`) — même approche que le compas de KaliumMenu.
- `ClaimsListener` : applique la protection, chaque volet lisant son propre reglage (voir ci-dessus)
  et verifiant que le joueur n'a pas `kaliumcore.claims.bypass`. `BlockBreakEvent` (casse) et
  `BlockPlaceEvent` (depot) annulés separement si le chunk appartient à quelqu'un d'autre.
  `PlayerInteractEvent` (clic droit, "utilisation") annulé pour une liste de blocs interactifs
  (coffres, shulkers, fours, enclumes, établi, table d'enchantement, hopper/dropper/dispenser, four à
  brasser, lutrin, composteur, etc.). `EntityDamageByEntityEvent` (pvp) annulé si la **victime** est le
  propriétaire du chunk où elle se trouve (protège le propriétaire spécifiquement, pas un PvP-off
  général dans le chunk) et que l'attaquant (joueur direct ou tireur d'un projectile) n'a pas le
  bypass. Messages throttlés (2s par joueur) pour ne pas spammer sur casse/pose répétées.
- `ClaimsMenu` (écran joueur, ouvert depuis le bouton "Claims" du menu principal) : affiche le
  statut du chunk courant (libre/à moi/à quelqu'un d'autre), le nombre d'emplacements utilisés/total,
  puis les boutons pertinents selon l'état et les réglages actifs (Réclamer, Libérer, Définir comme
  base, Convertir emplacement→jeton, Convertir jeton→emplacement), retour au menu principal.
- `ClaimsAdminMenu` (sous-écran Paramètres, ouvert via `settingsAction` du module claims) :
  - `openSettings` : emplacements par défaut des nouveaux joueurs (champ numérique), bouton
    **"Options"** (→ `openOptions`, nouveau en 1.2.0) et bouton vers la gestion par joueur. Ne
    contient plus aucun interrupteur directement (voir ci-dessous).
  - `openOptions` (nouveau en 1.2.0) : les 6 réglages fins listés explicitement par l'utilisateur -
    réclamer un chunk, libérer un chunk, protection casse, protection dépôt, protection utilisation,
    protection pvp - sous forme de cases à cocher + bouton "Enregistrer" unique (même schéma que
    "Options actives"). `toggleInputs()`/`applyToggles()` (utilisés par `MenuService.
    openActiveOptions`) n'exposent plus, eux, que l'interrupteur maître du module ("Système de
    claim") - demande explicite de l'utilisateur ("dans option active laisse uniquement l'option
    système de claim").
  - `openPlayerAdmin` : deux champs (pseudo, ajustement +/-), bouton "Consulter" et bouton "Appliquer
    l'ajustement". Depuis 1.2.0, le résultat (infos consultées, ou confirmation d'ajustement)
    s'affiche **dans le corps du Dialog** (`consultInfo`/`applyInfo`, retournent un `Component` ajouté
    au `body` puis l'écran se rouvre avec) et non plus par message de chat - demande explicite de
    l'utilisateur. Le pseudo tapé est repropagé d'un clic à l'autre (`presetName`) pour ne pas avoir à
    le retaper.

## Module commerce (nouveau en 1.3.0, revu en 1.3.1, entièrement refondu en 1.4.0)

**Important pour toute reprise** : les paragraphes juste en dessous (jusqu'à "Révision 1.3.1" incluse)
décrivent l'architecture **1.3.0/1.3.1**, aujourd'hui obsolète — conservés comme historique de
raisonnement, pas comme description de l'état actuel du code. L'état actuel (depuis la 1.4.0) est
décrit dans "Révision 1.4.0 : refonte complète du flux" plus bas ; **lire cette section en priorité**
avant de toucher au module commerce.

Demande initiale complète de l'utilisateur : un système de commerce accessible depuis le menu,
activable/désactivable depuis Paramètres ; une page type "établi" (donc un vrai **inventaire**, pas un
Dialog, pour pouvoir afficher des objets) avec des onglets - **Vente flash** (programmée par les
opérateurs depuis Paramètres), **Mettre en vente** (les joueurs y mettent leurs objets en vente),
**Marché** (tout voir en vrac, avec recherche - "Ventes" jusqu'en 1.3.0) et **Historique** ; monnaie = **émeraude**
(objet physique, pas un solde virtuel) ; un bouton ouvrant un **coffre personnel** pour récupérer ses
gains/achats ; le survol d'un objet en vente affiche son prix et son prix de vente moyen ; un historique
d'achats/ventes consultable à tout moment. Toutes ces mécaniques sont dans un seul module (`market`)
avec un interrupteur maître dans "Options actives", comme les autres.

**Différence d'architecture importante avec claims/stats** : c'est le premier écran du plugin basé sur
un vrai `Inventory`/`InventoryHolder` (voir `MarketGuiHolder`, `MarketGuiListener`) plutôt qu'un
`Dialog` - nécessaire pour afficher des objets réels (icônes, survol = tooltip natif avec prix/prix
moyen dans la lore). La mise en vente a quand même besoin de saisir deux nombres (prix/quantité) :
elle ferme l'inventaire et ouvre un `Dialog` (comme le reste du plugin), puis rouvre l'inventaire une
fois la saisie validée. **Depuis la 1.3.1**, la recherche ne fonctionne plus ainsi (voir "Révision
1.3.1" ci-dessous) - elle se fait directement depuis la page via une enclume virtuelle, sans jamais
fermer complètement l'expérience de shopping.

- `MarketListing` / `FlashSale` / `TransactionRecord` : simples porteurs de données (voir leur javadoc).
  `FlashSale.status(now)` calcule à la volée UPCOMING/ACTIVE/SOLD_OUT/EXPIRED (rien à recalculer en
  tâche de fond pour le démarrage/l'arrêt d'une vente flash, seule l'expiration lointaine est purgée
  périodiquement).
- `MarketService` : cœur logique, même approche que `ClaimsService` (1 seul fichier
  `plugins/KaliumCore/market.yml`, sauvegarde à chaque mutation, `ConcurrentHashMap`). Les `ItemStack`
  (annonces, ventes flash, contenu du coffre de récompenses, historique) sont sérialisés nativement par
  `YamlConfiguration` (`ConfigurationSerializable`, pas de base64 maison).
  - **Mise en vente** (`createListing`) : reprend exactement la technique déjà validée dans
    `ClaimsService.convertItemToSlot` - lire l'objet **tenu en main**, écrire via `setItem` +
    `updateInventory()` plutôt que muter l'`ItemStack` en place. Pas d'écran glisser-déposer dans
    l'inventaire du commerce (voir plus bas pourquoi).
  - **Émeraudes** : pas de solde virtuel - `countEmeralds`/`removeEmeralds` comptent et retirent les
    `ItemStack` d'émeraude réels dans l'inventaire du joueur (36 emplacements principaux, hors
    armure/off-hand - simplification jugée raisonnable).
  - **Coffre de récompenses** (`rewardInventory`/`depositReward`/`openRewardChest`) : un vrai
    `Inventory` (54 cases) par joueur, en mémoire (`MarketRewardHolder`), où atterrissent les objets
    achetés, les émeraudes d'une vente conclue, et les objets d'une annonce annulée/expirée si le
    joueur n'a pas la place dans son inventaire (ou est hors-ligne). C'est le bouton demandé par
    l'utilisateur ("un bouton qui ouvre un coffre personnel"). `Inventory.addItem(...)` gère lui-même
    le merge de stacks - pas de logique de fusion à la main.
  - **Prix de vente moyen** (`averagePrice`) : moyenne cumulative par `Material`, alimentée
    uniquement par les ventes normales (`buyListing`), **pas** par les ventes flash (prix promotionnels
    ponctuels, pas représentatifs du "prix moyen" - simplification volontaire, à signaler si
    l'utilisateur veut les inclure).
  - **Vente flash** (`scheduleFlashSale`/`buyFlashSale`) : l'objet tenu en main par l'opérateur sert de
    **modèle réutilisable** (lot = sa quantité), **n'est jamais consommé/retiré de son inventaire** -
    le stock vendu est généré à la demande à chaque achat, jusqu'à épuisement du `stock` programmé.
    Différence à bien avoir en tête par rapport à la mise en vente joueur (qui, elle, retire l'objet
    réel de l'inventaire).
  - **Expiration** (`tick()`, appelé toutes les `stats-autosave-seconds`, 300s par défaut) : une
    annonce expirée (`listing-duration-days`, 7 jours par défaut) est retirée et son objet déposé dans
    le coffre de récompenses du vendeur ; une vente flash est purgée 24h après sa fin (grâce pour que
    l'écran admin garde une trace récente).
- `MarketGui` (**layout revu en 1.3.1**, voir "Révision 1.3.1" ci-dessous) : construit l'inventaire à
  onglets - colonne 0 = languettes (Vente flash/Mettre en vente/Marché/Historique/Recherche, une par
  rangée 0-4), colonnes 1-8 rangées 0-4 = contenu paginé (40 cases/page), rangée 5 = pagination + coffre
  de récompenses + fermer + retour menu. **Aucun objet du joueur n'y transite jamais** (voir
  `MarketGuiHolder`) : la mise en vente passe par l'objet tenu en main (pas de glisser-déposer dans
  l'écran), ce qui élimine tout risque de duplication/perte côté commerce. Onglet "Mettre en vente" :
  bouton épinglé (première case de contenu, aperçu = l'objet actuellement en main) + liste paginée des
  annonces du joueur (clic = annuler, objet récupéré). Onglet "Marché" : liste paginée de toutes les
  annonces (filtrées par la recherche active le cas échéant, avec un indicateur cliquable pour
  l'effacer), clic sur une annonce = achat immédiat (pas de confirmation supplémentaire - cohérent avec
  le reste du plugin qui privilégie des actions directes). Onglet "Vente flash" : même principe, sur les
  ventes flash actives/à venir. Onglet "Historique" : lecture seule. La recherche
  (`openSearchAnvil`/`finishSearchAnvil`) passe par une enclume virtuelle (voir "Révision 1.3.1"), la
  mise en vente (`openCreateListingDialog`) ferme l'inventaire, affiche un `Dialog`, puis rouvre
  l'inventaire.
- `MarketGuiListener` : câble les clics. Écran à onglets → **tout clic sur le haut, ou tout shift-clic
  depuis le bas, est annulé** (écran purement décoratif) puis interprété selon le slot. Coffre de
  récompenses → clics de **retrait** autorisés (clic/shift-clic/glisser vers le bas), tout ce qui
  déposerait un objet dedans (`PLACE_*`, `SWAP_WITH_CURSOR`, `HOTBAR_SWAP` sur le haut, ou shift-clic
  depuis le bas) est annulé - ce coffre ne sert qu'à récupérer, jamais de stockage libre. À la fermeture
  du coffre, sauvegarde immédiate. Enclume de recherche (1.3.1) → tout clic/shift-clic sur les 3 cases de
  l'enclume est annulé (`PrepareAnvilEvent` force en plus coût=0 et résultat=null en permanence) : aucun
  objet ni coût ne transite jamais, seule la saisie de texte (nom renommé) compte ; à la fermeture (clic
  ailleurs ou Échap), le texte tapé devient le filtre de recherche et l'onglet Marché se rouvre.
- `MarketAdminMenu` (sous-écran Paramètres, ouvert via `settingsAction` du module market) : limites
  (annonces actives max par joueur, durée de vente par défaut) sur l'écran principal, plus deux boutons
  - "Programmer une vente flash" (objet tenu en main + prix/stock/délai/durée) et "Ventes flash
  programmées" (liste avec annulation, jusqu'à 20 affichées). Comme pour claims, seul l'interrupteur
  maître ("Système de commerce") vit dans "Options actives" (`toggleInputs()`/`applyToggles()`).
- Toutes les clés de `DialogInput` du module (`market_enabled`, `market_max_listings`,
  `market_duration`, `market_flash_price/stock/delay/duration`, `market_price`, `market_qty`) suivent
  la règle `[A-Za-z0-9_]+` (voir le piège ci-dessous) - vérifiées explicitement avant livraison.
  `market_search` a disparu en 1.3.1 (la recherche n'utilise plus de `DialogInput`, voir plus bas).

### Révision 1.3.1 : correctif coffre de récompenses, renommage "Ventes" → "Marché", nouveau layout

Trois demandes de l'utilisateur après un premier test en jeu de 1.3.0 :

1. **Bug remonté** : "j'ai mis en place une vente flash, je l'ai ensuite acheté, mes émeraudes ont bien
   été utilisées mais je n'ai rien reçu dans le coffre de récompense." Cause : `MarketService`
   possédait une méthode `giveOrReward` qui donnait l'objet **directement dans l'inventaire du joueur**
   en priorité, et ne passait par le coffre de récompenses qu'en cas d'inventaire plein (overflow). Or le
   message affiché à l'achat ("Récupère ton objet dans le coffre de récompenses") promet explicitement
   le coffre. `buyListing`, `buyFlashSale` et `cancelListing` appelaient tous les trois `giveOrReward` -
   corrigé pour qu'ils appellent directement `depositReward(...)` (donc systématiquement le coffre,
   jamais l'inventaire courant), comme le faisait déjà `tick()` pour les annonces expirées.
   `giveOrReward` (devenue morte) a été supprimée. **L'achat de vente flash signalé par l'utilisateur
   n'était donc probablement pas perdu** : l'objet a très vraisemblablement atterri directement dans
   son inventaire normal (pas le coffre) puisque celui-ci avait de la place au moment de l'achat -
   à vérifier avec lui, et si l'objet est bien introuvable malgré tout, il faudra investiguer plus loin
   (aucun accès au serveur en direct depuis cet environnement pour le confirmer).
2. **Renommage** : l'onglet "Ventes" devient **"Marché"** (`market-tab-browse-name`,
   `market-title-browse` - mêmes clés, juste le texte). L'enum Java `MarketGuiHolder.Tab.VENTES` a
   été renommé `MARCHE` par cohérence (tous les usages mis à jour : `MarketGui`, `MarketGuiListener`).
3. **Recherche directement depuis la page + languettes sur le côté** :
   - La recherche ne passe plus par un `Dialog` séparé (qui fermait complètement l'écran de commerce) :
     elle se fait maintenant **directement depuis la page**, avec la même technique que la recherche de
     recette dans l'établi vanilla - une **enclume virtuelle** (`Player.openAnvil(Location, boolean
     force=true)`, jamais vue ailleurs dans le plugin jusqu'ici). Un objet symbolique (papier) est placé
     dans la case de gauche avec pour nom le filtre actuel ; taper un nouveau nom dans le champ de
     l'enclume puis la fermer (clic ailleurs ou Échap) applique ce texte comme filtre et rouvre
     directement l'onglet Marché filtré. Aucun objet ni coût ne transite jamais (`PrepareAnvilEvent`
     force coût=0/résultat=null en permanence, tous les clics sur l'enclume sont annulés) - risque de
     duplication nul, comme pour le reste du plugin. **Limite assumée** : l'enclume garde son titre
     vanilla ("Réparer & nommer") - `openAnvil` ne permet pas de titre personnalisé, contrairement à un
     `Dialog`.
   - Les languettes (Vente flash / Mettre en vente / Marché / Historique / Recherche) sont passées de la
     **rangée du haut** à la **colonne de gauche** (une par rangée, rangées 0-4), pour se rapprocher
     visuellement du livre de recettes de l'établi. **Limite assumée et annoncée à l'utilisateur** : un
     `Inventory` Bukkit standard ne peut pas faire déborder de vraies languettes hors du cadre de
     l'interface comme le fait le livre de recettes vanilla (rendu câblé en dur côté client, nécessite un
     resource pack) - la colonne de gauche est l'équivalent le plus proche réalisable avec les textures
     standard (`Material.CLOCK`/`ANVIL`/`EMERALD`/`BOOK`/`COMPASS`). Le contenu passe de 36 à 40 cases par
     page (colonnes 1-8 × rangées 0-4) grâce à la place récupérée sur l'ancienne rangée d'onglets.
   - Nouvelle correspondance slot ↔ case de contenu dans `MarketGui` (`slotForContentIndex`/
     `contentIndexForSlot`, index 0-based lecture gauche→droite puis haut→bas, colonne 0 et rangée 5
     exclues) - tous les calculs de pagination/clic (`renderFlash/Sell/Browse/History`,
     `flashIdAtSlot`, `listingIdAtSlot`, `ownListingIdAtSlot`, `isSellButtonSlot`) réécrits dessus.

### Révision 1.4.0 : refonte complète du flux (abandon des onglets et de l'enclume, un écran dédié par fonction)

Après le test en jeu de la 1.3.1, l'utilisateur a rejeté le principe même des onglets latéraux +
recherche par enclume ("ce n'est toujours pas ce que je veux"). Une piste de reprise du **vrai** livre
de recettes vanilla (tabs qui débordent du cadre + barre de recherche native) a été explorée puis
écartée et expliquée à l'utilisateur : les recettes vanilla plafonnent à 9 ingrédients (ne peuvent pas
représenter un prix jusqu'à 8640), les 4 catégories du livre sont fixes côté client (impossible de les
renommer "Marché"/"Mes ventes"/etc.), "Mettre en vente" et les historiques n'ont pas d'équivalent
recette, et il aurait fallu enregistrer de fausses recettes globales avec un risque de duplication/
déssynchronisation inacceptable. Un resource pack Java+Bedrock a aussi été évoqué (l'utilisateur a dit
oui) puis abandonné avec le reste de cette piste : un pack ne peut que reskinner des widgets client déjà
câblés en dur (établi/four), pas greffer des onglets protubérants ni un champ de texte persistant sur un
`Inventory` personnalisé.

L'utilisateur a ensuite envoyé un cahier des charges complet et explicite, entièrement différent :
plus d'onglets, plus d'enclume — **un menu `Dialog` "Commerce" avec un bouton par fonction**, chaque
fonction menant soit à un `Dialog` de saisie, soit à un écran type "page de coffre" dédié avec pagination
(45 objets/page) et une navigation regroupée **en bas à droite** (page précédente / page suivante /
retour à l'écran précédent), soit à une simple liste en `Dialog` pour les historiques. Reconstruit en
conséquence :

- **`MarketGui.openHub`** (nouveau point d'entrée du module, remplace l'ancien `MarketGui.open` à
  onglets) : un `Dialog` avec un bouton par fonction — Mettre en vente, Mes ventes, Recherche, Marché,
  Vente flash, Historique des ventes, Historique des achats, Rewards (avec le nombre d'objets en attente
  affiché sur le bouton), Retour au menu. `ModuleRegistry` (`KaliumCore`, enregistrement du module
  `market`) pointe désormais `menuAction` sur `marketGui::openHub`.
- **Mettre en vente** (`openCreateListingDialog`) : inchangé dans le principe (objet tenu en main,
  `Dialog` prix/quantité) mais avec un **3ᵉ champ** — la durée de la vente en jours, plafonnée à
  `MarketService.MAX_LISTING_DURATION_DAYS` (7, nouvelle constante), pré-remplie avec la durée par
  défaut réglée dans Paramètres. **Chaque annonce a donc désormais sa propre durée** (1-7 jours), au
  lieu d'une durée globale fixe unique (7 jours en dur) comme en 1.3.x — `MarketService.
  listingDurationDays()`/`(int)` renommées `defaultListingDurationDays()`/`(int)` (c'est maintenant une
  valeur *par défaut/pré-remplie*, pas une durée imposée) ; `MarketAdminMenu`/`settings-market-
  duration-label` mis à jour en conséquence pour ne plus laisser croire à une durée fixe.
- **Mes ventes**, **Marché**, **Vente flash**, **Recherche** : quatre écrans "page de coffre" quasi
  identiques (`MarketGui.openMesVentes/openMarche/openVenteFlash/openSearchResults`), tous construits
  sur le même patron (`MarketGuiHolder`, `Screen` enum remplaçant l'ancien `Tab`) : contenu = cases 0-44
  (45, denses, sans colonne réservée), navigation = cases 51 (page précédente) / 52 (page suivante) / 53
  (retour, avec l'info "page X/Y" en lore) — cases 45-50 volontairement vides. **Aucun remplissage
  décoratif** (verre gris) sur les cases inutilisées : rupture assumée avec le design 1.3.1, pour coller
  à la consigne "page type coffre" de l'utilisateur (un vrai coffre a des cases vides, pas du verre).
  Vente flash reste un écran à part entière (bouton "Vente flash" ajouté au menu Commerce) **alors que
  l'utilisateur ne l'a pas listé dans son nouveau cahier des charges** — hypothèse assumée pour ne pas
  supprimer silencieusement une fonctionnalité admin existante (programmation de ventes flash depuis
  Paramètres) ; **à confirmer avec l'utilisateur**, elle peut être retirée du menu si ce n'était pas
  voulu.
- **Recherche FR/EN** (`MarketService.searchListings`, `ItemNames`) : la recherche doit fonctionner
  aussi bien avec les noms français qu'anglais des objets ("Épée en diamant" ou "Diamond Sword"
  retrouvent tous les deux `DIAMOND_SWORD`). Comme Bukkit n'expose aucune table de traduction FR
  intégrée, les noms réels ont été récupérés depuis les fichiers de langue officiels de Minecraft
  (`en_us.json`/`fr_fr.json`, via un mirroir GitHub communautaire `InventivetalentDev/minecraft-assets`,
  snapshot `25w46a`), croisés avec l'énumération `Material` de l'API Paper (`javap`) : ~1951/2154
  `Material` mappés automatiquement (clé `item.minecraft.<id>` puis `block.minecraft.<id>`, avec gestion
  des `LEGACY_*` et du cas `MELON`/`MELON_SLICE`), complétés par ~32 traductions ajoutées à la main pour
  des blocs fictifs/pas encore sortis présents dans cette version de l'API (famille CINNABAR/SULFUR,
  GOLDEN_DANDELION, MUSIC_DISC_BOUNCE...). Les ~203 non mappés sont des `LEGACY_*` (non obtenables) et
  des `*_WALL_BANNER` (variantes d'état de bloc, jamais un objet en main) — sans impact réel. Résultat
  embarqué dans `src/main/resources/item_names.tsv` (copié dans le jar par `build.sh`, chargé par
  `ItemNames` au démarrage via `plugin.getResource(...)`). La recherche elle-même (`MarketService.
  searchText`) normalise nom d'objet + nom FR + nom EN (minuscule, accents retirés via `Normalizer`
  NFD) et fait un simple "contient" sur le mot-clé, normalisé pareil.
- **Historiques de ventes/d'achats** : deux écrans **séparés** (contrairement à l'unique onglet
  "Historique" mélangé de 1.3.x), tous les deux en `Dialog` (liste de texte, lecture seule, plafonnée à
  40 lignes affichées — même limite que la liste des ventes flash programmées en Paramètres, pour la
  même raison : un `Dialog` ne peut pas afficher des centaines de lignes proprement) plutôt qu'en page de
  coffre : le cahier des charges de l'utilisateur ne mentionne ni "page type coffre" ni pagination ni
  bouton de retour en bas à droite pour ces deux écrans (contrairement à tous les autres), lu comme un
  signal volontaire plutôt qu'un oubli. `MarketService.salesHistory`/`purchaseHistory` filtrent
  l'historique existant par type de transaction.
- **Rewards** (coffre personnel) : redesign de fond. Le cahier des charges demande la **même pagination
  à 45 objets/page** que Marché/Recherche pour ce coffre — incompatible avec l'ancien modèle "un seul
  `Inventory` de 54 cases EST le stockage". `MarketService` stocke donc désormais les récompenses en
  attente dans une **liste non bornée** (`List<ItemStack>` par joueur) ; `rewardPageItems(id, page)`
  découpe la page demandée à l'affichage, `reconcileRewardPage(id, page, survivors)` réintègre à la
  fermeture les objets restants de cette page précise dans la liste (le joueur peut retirer des objets
  librement, jamais en déposer — `MarketGuiListener` bloque tout `PLACE_*`/`SWAP_WITH_CURSOR`/
  `HOTBAR_SWAP` sur le haut de l'inventaire, comme avant). Plus de risque de débordement/plafond
  artificiel comme avec l'ancien coffre à 54 cases fixes. `depositReward` fusionne puis ajoute en fin de
  liste (mêmes garanties qu'avant sur le merge de stacks, réimplémentées à la main puisqu'il n'y a plus
  d'`Inventory.addItem` disponible sur une simple liste).
- **`MarketGuiHolder`** : l'ancien enum `Tab` (`VENTE_FLASH, METTRE_EN_VENTE, MARCHE, HISTORIQUE`)
  devient `Screen` (`MARCHE, VENTE_FLASH, MES_VENTES, RECHERCHE`) — remarque : `METTRE_EN_VENTE` et
  `HISTORIQUE` ne sont plus du tout des écrans "page de coffre" (mise en vente = `Dialog` uniquement,
  historique = `Dialog`-liste uniquement), ils ont donc disparu de cet enum. `MarketRewardHolder` porte
  désormais aussi `page()` (plus seulement `owner()`), nécessaire à `MarketGuiListener` pour savoir quelle
  tranche de la liste réconcilier à la fermeture.
- **`MarketGuiListener`** entièrement réécrit : dispatch par `MarketGui.isNavSlot`/`isContentSlot` (au
  lieu des anciennes constantes `TAB_*`), plus aucune trace d'enclume (`PrepareAnvilEvent`/
  `AnvilInventory` supprimés, la recherche est un `Dialog` comme le reste), fermeture du coffre de
  récompenses → `reconcileRewardPage` (au lieu d'un simple `save()` global).
- Clés de message : 27 ajoutées (`market-hub-*`, `market-title-mesventes/recherche`, `market-sell-
  dialog-duration-label`, `market-search-dialog-*`, `market-empty-search/rewards`, `market-history-
  ventes/achats-*`, `market-history-line-vente/achat/flash`), 25 supprimées (tout ce qui décrivait les
  onglets/l'enclume : `market-tab-*`, `market-search-anvil-hint`, `market-search-active`, `market-
  search-clear-hint`, `market-sell-button-*`, `market-sell-count*`, `market-lore-history-*`, `market-
  title-sell/history`, `market-empty-history`). Vérification 3 voies (appels Java ↔ `switch` de
  `KaliumCore.msg` ↔ `config.yml`) refaite après coup, en isolant d'abord les clés de persistance YAML
  de `MarketService` (`next-id`, `seller-name`, `listed-at`, `expires-at`, `flash-sales`, `start-at`,
  `end-at`, `created-by`, `price-stats` — des clés de sauvegarde, pas des clés de message, à ne pas
  confondre) : 92/92 clés commerce strictement identiques entre `switch` et `config.yml`, et tous les
  appels du code correspondent à une clé existante. Recompilation ECJ propre (`-nowarn` puis `-warn:all`
  : 0 erreur, aucun avertissement "unused"/"never used" dans les fichiers `market`).

## Piège important : cles des DialogInput (a lire avant d'ajouter un champ de saisie)

Paper valide le `key` de **tout** `DialogInput` (`bool`, `numberRange`, `text`, `singleOption`) avec
`net.minecraft.commands.functions.StringTemplate.isValidVariableName(String)` - la meme regle que les
variables de fonction Minecraft (`$(nom)`) : **uniquement lettres/chiffres/underscore `_`, aucun point
ni tiret**. Une cle invalide fait planter la construction du `Dialog` avec
`IllegalArgumentException: key must be a valid input name`, et comme cette construction se fait dans
une tache planifiee (`DialogHelper` l'appelle via `runTask`), l'exception part dans les logs serveur
**sans aucun message ni erreur visible cote joueur** - l'ecran ne s'ouvre juste pas, silencieusement.
C'est exactement ce qui s'est passe ici : `"default-slots"` (tiret) plantait depuis la toute premiere
version du systeme de claims (1.0.0), provoquant le bug "le bouton Claims ne fait rien" signale par
l'utilisateur ; en 1.1.1, les nouvelles cles `"module." + id` et `"claims.xxx"` (points) ont provoque le
meme crash sur le nouvel ecran "Options actives". Corrige en 1.1.2 en remplacant toutes les cles par des
formes `snake_case` sans point ni tiret (`default_slots`, `module_stats`, `claims_enabled`,
`claims_claim_action`, etc.) - verifie en desassemblant `BooleanDialogInputImpl$BuilderImpl` et
`StringTemplate.isValidVariableName` depuis les classes du serveur fournies par l'utilisateur
(`kaltest-server-classes.jar`), pas juste par relecture de code.
**A retenir pour tout futur DialogInput (nouveaux modules, commerce...) : la cle doit matcher
`[A-Za-z0-9_]+`, un point c'est une virgule.**

## Déploiement du 22/09/2026

- Nettoyage de Kal-Test-Dev : KalGames (dossier + jar + 5 `.bak`) et KaliumMenu (dossier + jar)
  déplacés (pas supprimés) dans `/plugins/_removed-kalgames-kaliummenu/` via WinSCP
  ("Déplacer vers...", **jamais F6** qui télécharge-puis-supprime).
- **KaliumCore 1.0.0** buildé et déployé dans `/plugins/` sur Kal-Test-Dev (menu de base : lobby,
  stats, paramètres — pas encore de claims).
- **KaliumCore 1.1.0** buildé et déployé le même jour, en remplacement du 1.0.0 : ajoute le toggle
  d'affichage des statistiques et le module claims complet (voir section dédiée ci-dessus). L'ancien
  jar `KaliumCore-1.0.0.jar` a été déplacé (pas supprimé) dans
  `/plugins/_removed-kaliumcore-1.0.0/` via WinSCP (même règle : jamais F6).
- **KaliumCore 1.1.1** buildé et déployé le même jour, suite à un retour utilisateur après un premier
  test en jeu : le bouton "Claims" dans Paramètres ne faisait rien au clic. Cause non reproduite avec
  certitude hors-jeu (pas de serveur de test disponible depuis cet environnement), mais corrigée en
  même temps qu'une refonte demandée par l'utilisateur : tous les interrupteurs
  (Statistiques + les 6 options du module claims - maître/claim/unclaim/protection/base/jeton) sont
  désormais regroupés sur un seul écran **"Options actives"** (accessible depuis Paramètres), sous
  forme de cases à cocher avec un unique bouton "Enregistrer" qui applique tout d'un coup - remplace
  l'ancien systeme de boutons-interrupteurs qui appliquaient et sauvegardaient immédiatement au clic.
  Voir `MenuService.openActiveOptions` (nouveau) et `ClaimsAdminMenu.toggleInputs()`/`applyToggles()`
  (le point d'entrée que le module claims expose à cet écran commun ; son propre écran
  `ClaimsAdminMenu.openSettings` ne contient plus que les emplacements par défaut et la gestion des
  joueurs, plus aucun interrupteur). L'ancien jar `KaliumCore-1.1.0.jar` a été déplacé (pas supprimé)
  dans `/plugins/_removed-kaliumcore-1.1.0/` via WinSCP. Seul `KaliumCore-1.1.1.jar` (53 Ko) reste actif
  dans `/plugins/`.
- **KaliumCore 1.1.2** buildé et déployé le même jour : correctif de la **vraie** cause du bug
  "Claims"/"Options actives" ne faisant rien au clic, identifiée grâce aux logs serveur fournis par
  l'utilisateur (`IllegalArgumentException: key must be a valid input name`, voir la section dédiée
  "Piège important" ci-dessus). Toutes les clés de `DialogInput` contenant un point ou un tiret ont
  été renommées en `snake_case`. Ancien jar `KaliumCore-1.1.1.jar` déplacé (pas supprimé) dans
  `/plugins/_removed-kaliumcore-1.1.1/`. **Confirmé fonctionnel par l'utilisateur** ("ça fonctionne") -
  Options actives et Claims s'ouvrent correctement.
- **KaliumCore 1.2.0** buildé suite à 3 demandes de l'utilisateur après validation du correctif
  1.1.2 : (1) le bouton "Consulter" de la gestion des emplacements affiche désormais le résultat
  dans l'écran du Dialog plutôt que dans le chat (idem pour "Appliquer l'ajustement", par
  cohérence) ; (2) l'écran "Options actives" ne montre plus, pour le module claims, que
  l'interrupteur maître "Système de claim" ; (3) nouveau bouton "Options" dans Paramètres > Claims
  ouvrant un sous-écran dédié aux 6 réglages fins (réclamer, libérer, protection casse/dépôt/
  utilisation/pvp) - la protection, qui était un seul interrupteur, est désormais scindée en 4 volets
  indépendants (demande explicite). Voir la section "Module claims" ci-dessus pour le détail
  technique. **Jamais déployé sur le serveur** : livré à l'utilisateur via téléchargement, mais
  celui-ci est passé directement à la demande suivante (1.2.1, ajout base/jeton) sans confirmer le
  téléchargement ni déclencher l'upload WinSCP - `KaliumCore-1.1.2.jar` est donc resté le jar actif
  sur Kal-Test-Dev jusqu'au déploiement de 1.2.1.
- **KaliumCore 1.2.1** buildé et déployé le même jour (upload WinSCP effectué directement, 1.2.0
  n'ayant jamais été mis en ligne) : l'utilisateur a confirmé vouloir aussi "chunk principal" (base)
  et "conversion en jeton" dans l'écran Options de Claims (j'avais bien fait de signaler leur
  absence plutôt que de supposer silencieusement que c'était voulu). L'écran
  `ClaimsAdminMenu.openOptions` a donc 8 cases à cocher au total. Le jar réellement actif jusque-là,
  `KaliumCore-1.1.2.jar`, a été déplacé (pas supprimé) dans `/plugins/_removed-kaliumcore-1.1.2/` -
  il n'existe pas de dossier `_removed-kaliumcore-1.2.0/` puisque cette version n'a jamais été
  déployée. Seul `KaliumCore-1.2.1.jar` reste actif dans `/plugins/`. **Confirmé déployé** (upload
  WinSCP + nettoyage vérifiés par capture d'écran), mais l'utilisateur n'a pas explicitement confirmé
  le résultat des tests en jeu (checklist de la section précédente) avant de passer à la demande
  suivante (système de commerce) - à garder à l'esprit si un bug de claims 1.2.1 remonte plus tard.
- **KaliumCore 1.3.0** buildé et déployé le même jour : ajoute le **système de commerce** complet
  (voir section dédiée ci-dessus - nouveau module `market`, premier écran du plugin basé sur un
  `Inventory` plutôt qu'un `Dialog`). Ancien jar `KaliumCore-1.2.1.jar` déplacé (pas supprimé) dans
  `/plugins/_removed-kaliumcore-1.2.1/` via WinSCP. Seul `KaliumCore-1.3.0.jar` (99 Ko) reste actif
  dans `/plugins/`. Redémarrage de Kal-Test-Dev et tests en jeu (checklist ci-dessous) pas encore
  confirmés par l'utilisateur au moment d'écrire ces lignes.
- **KaliumCore 1.3.1** buildé suite au retour utilisateur après un premier test en jeu de 1.3.0 :
  correctif du coffre de récompenses (voir "Révision 1.3.1" ci-dessus), renommage "Ventes" → "Marché",
  recherche déplacée dans une enclume directement sur la page (au lieu d'un `Dialog` séparé), et
  languettes déplacées de la rangée du haut vers la colonne de gauche (avec la limite du resource pack
  signalée à l'utilisateur). Vérification 3 voies clés Java/switch/config.yml refaite après les 5
  suppressions (`market-search-dialog-*`) et 3 ajouts (`market-tab-search-lore`, `market-search-anvil-
  hint`, `market-search-clear-hint`) de clés de message : 182/182. Recompilation ECJ propre (`-nowarn`
  puis `-warn:all`, aucun avertissement "unused"/"never used" dans les fichiers `market`). **Déployé le
  même jour** (upload WinSCP effectué directement après confirmation du téléchargement par
  l'utilisateur) : ancien jar `KaliumCore-1.3.0.jar` déplacé (pas supprimé, Shift+F6) dans
  `/plugins/_removed-kaliumcore-1.3.0/`. Seul `KaliumCore-1.3.1.jar` (100 Ko) reste actif dans
  `/plugins/` (vérifié par capture d'écran WinSCP après coup). Redémarrage de Kal-Test-Dev et tests en
  jeu (checklist ci-dessous) pas encore confirmés par l'utilisateur au moment d'écrire ces lignes.
- **KaliumCore 1.4.0** buildé suite au rejet par l'utilisateur du principe onglets+enclume de la 1.3.1
  ("ce n'est toujours pas ce que je veux") et à un cahier des charges complet de remplacement (voir
  "Révision 1.4.0" ci-dessus) : refonte totale du module commerce (menu `Dialog` "Commerce" + écrans
  dédiés par fonction, recherche FR/EN, durée de vente par annonce, historiques séparés, coffre de
  récompenses paginé). **Déployé le même jour** (téléchargement confirmé par l'utilisateur, upload
  WinSCP effectué directement) : ancien jar `KaliumCore-1.3.1.jar` déplacé (pas supprimé, Shift+F6
  "Déplacer vers...", jamais F6) dans `/plugins/_removed-kaliumcore-1.3.1/` (vérifié par capture d'écran
  WinSCP après coup - 100 Ko, présent). Seul `KaliumCore-1.4.0.jar` (124 Ko) reste actif dans
  `/plugins/`. Redémarrage de Kal-Test-Dev et tests en jeu (checklist ci-dessous) pas encore confirmés
  par l'utilisateur au moment d'écrire ces lignes.
- Chemin de livraison du jar (spécifique à cet environnement) : `SendUserFile` ne dépose pas
  systématiquement le fichier au même endroit — pour 1.0.0 il est réapparu directement dans
  `C:\Users\HP Zbook 17 G3\` (racine du profil) ; pour 1.1.0 il est réapparu dans
  `C:\Users\HP Zbook 17 G3\Downloads\Claude outputs\`. Bien vérifier plusieurs emplacements
  (racine du profil, `Downloads`, `Downloads\Claude outputs`) au prochain déploiement si le jar
  n'apparaît pas immédiatement dans le volet local de WinSCP.

## Pour reprendre la prochaine fois

- **Livraison 1.4.0** : déployée (voir bullet 1.4.0 dans "Déploiement du 22/09/2026" ci-dessus). Il
  reste à **signaler explicitement l'hypothèse "Vente flash conservée dans le menu Commerce alors
  qu'absente du nouveau cahier des charges"** (voir "Révision 1.4.0") si ce n'est pas déjà fait, pour
  que l'utilisateur confirme ou fasse retirer le bouton.
- Demander à l'utilisateur de redémarrer Kal-Test-Dev, puis vérifier en jeu (1.4.0) :
  - Paramètres → **Options actives** : doit toujours lister 3 cases (Statistiques, Système de claim,
    Système de commerce) — inchangé.
  - Menu principal → bouton **Commerce** : ouvre désormais un `Dialog` (plus un inventaire) avec un
    bouton par fonction : Mettre en vente, Mes ventes, Recherche, Marché, Vente flash, Historique des
    ventes, Historique des achats, Rewards (avec le compteur d'objets en attente), Retour au menu.
  - **Mettre en vente** : tenir un objet en main, cliquer le bouton → `Dialog` avec 3 champs (prix,
    quantité, **durée en jours, 1-7**, pré-remplie avec la durée par défaut de Paramètres) → confirmer →
    l'objet disparaît de l'inventaire et apparaît dans "Mes ventes" avec sa propre date d'expiration.
  - **Mes ventes** : page de coffre (45 cases, cases 45-50 vides, 51/52/53 = page précédente/suivante/
    retour en bas à droite) listant les annonces du joueur ; cliquer sur une annonce doit l'annuler et
    rendre l'objet (via le coffre de récompenses, jamais directement dans l'inventaire).
  - **Recherche** : `Dialog` à un seul champ texte → confirmer ouvre une page de coffre paginée des
    résultats. Tester avec un nom **français** ("épée", "planche de chêne") ET un nom **anglais**
    ("sword", "oak planks") — les deux doivent retrouver les mêmes objets. Tester aussi l'accentuation
    (ex. "epee" doit retrouver "Épée" grâce à la normalisation NFD).
  - **Marché** : page de coffre paginée de toutes les annonces actives ; survol = tooltip avec prix +
    prix de vente moyen ; clic = achat immédiat, objet déposé dans le coffre de récompenses.
  - **Vente flash** : inchangé dans le principe (Paramètres → Commerce → "Programmer une vente flash"),
    affichée dans son propre écran page-de-coffre paginé depuis le menu Commerce.
  - **Historique des ventes** / **Historique des achats** : deux écrans `Dialog` séparés (liste de
    texte, pas de pagination par page-de-coffre) — vérifier qu'un achat apparaît bien dans "Historique
    des achats" et qu'une vente conclue apparaît dans "Historique des ventes" (bon prix, bonne
    contrepartie, bonne date).
  - **Rewards** : désormais paginé (45 objets/page, navigation en bas à droite comme les autres écrans)
    au lieu d'un coffre fixe à 54 cases — vérifier qu'on peut retirer les objets normalement, qu'on ne
    peut PAS en déposer (glisser un objet dessus doit être bloqué), et que fermer puis rouvrir une page
    partiellement vidée garde bien les objets restants (test de `reconcileRewardPage`).
  - Paramètres → Commerce : limite d'annonces actives (tester qu'elle bloque bien une nouvelle mise en
    vente une fois atteinte) et durée de vente **par défaut** (vérifier qu'elle ne fait que pré-remplir
    le champ du `Dialog` de mise en vente, pas qu'elle impose une durée fixe).
- Points à garder à l'esprit (comportements volontairement simplifiés, à ajuster si l'utilisateur le
  demande) :
  - Les émeraudes comptées/retirées ne couvrent que les 36 emplacements principaux de l'inventaire (pas
    l'armure/off-hand).
  - Une vente flash ne consomme jamais l'objet tenu en main par l'opérateur (sert uniquement de modèle,
    contrairement à la mise en vente joueur qui retire l'objet réel).
  - Le prix de vente moyen n'inclut que les ventes normales (pas les ventes flash, prix promotionnels).
  - Achat = confirmation immédiate au clic, pas d'écran de confirmation supplémentaire.
