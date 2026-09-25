# KV_Plots — cahier des charges (demande de LeKiwi06, 25/09/2026)

Brouillon local, complété au fil des réponses de LeKiwi06. Serveur : **Kanvas** (ex Kal-Test-Dev, machine 5038), serveur
de plots en créatif. Préfixe `KV_` = Kanvas (voir la charte dans `ARCHITECTURE_CIBLE.md`).

## Demandes

| # | Demande |
|---|---|
| K1 | Créatif + vol pour tous, sans être opérateur |
| K2 | Ne modifier que ses plots, ou ceux où l'on est éditeur |
| K3 | Au plus 1 plot moyen + 1 plot grand réservés en même temps |
| K4 | Valider un plot fini libère la place pour en reprendre un de la même taille |
| K5 | Noter les plots validés de 1/5 à 5/5 avec des terracottas (rouge, orange, jaune, vert clair, vert foncé) |
| K6 | Un plot qui atteint 100 points cumulés débloque +1 plot moyen simultané |
| K7 | Revoter sur un même plot remplace l'ancien vote (pas de vote en plus) |
| K8 | Classements des meilleurs plots, général et du mois, avec le pseudo du créateur, en solo et en groupe |
| K9 | Sécurité avec des plugins existants : FastAsyncWorldEdit et WorldGuard |

## Décisions

- **Plot validé** : figé (plus modifiable). Il ne peut être **rouvert que par son créateur**, et seulement s'il a une place
  libre de la même taille (le plot rouvert reprend cette place). Les éditeurs ne peuvent pas le rouvrir.
- **Votes d'un plot rouvert** : gardés (points et classements conservés) ; pendant les travaux, le plot n'est plus
  votable ; une fois revalidé, chaque votant peut revoter, ce qui remplace son ancien vote (règle K7).
- **Tailles** : plots **moyens** et **grands** seulement (pas de petit plot pour le moment).
- **Monde** : monde existant de Kanvas, avec les motifs de délimitation déjà construits. Un **grand plot = 2 x 2 plots
  moyens** : on retire le croisement central et les 4 routes adjacentes.
- **Votes** : interdits au créateur et aux éditeurs. **Historique des éditeurs** gardé pour chaque plot : un ancien
  éditeur retiré ne peut pas voter non plus (anti-abus).
- **Limites** : au plus **2 plots de chaque taille**. Récompenses suivantes (plus tard) : via un futur `KLM_Rewards`,
  pour obtenir des choses dans la survie.
- **Classements** : au **total de points**, avec la **note moyenne entre parenthèses**. Trois classements : **solo**
  (1 constructeur), **duo** (2), **équipe** (3 et plus).
- **Affichage des classements** : un plugin **`KV_ScoreBoards`** (même logique que KG_ScoreBoards : données, menus,
  panneaux), et KLM_Menu pourra appeler les classements et les menus de chaque serveur (menus inter-serveurs, voir la
  charte).
- **Mobs** : sans IA, statiques (décor) ; les constructeurs du plot les retirent en un clic. **Limite d'entités : 10
  par plot moyen, 25 par plot grand.**
- **Pas de TNT.** La **lave et l'eau coulent**, mais **rien ne brûle** (pas de feu propagé ni allumé par la lave).
- **FAWE** : accessible aux créateurs et éditeurs, **dans leurs plots uniquement**. Une commande qui dépasse du plot
  n'agit que sur la partie dans le plot (construire au bord ne doit pas bugger).
- **Anti-crash** : limites strictes sur les sélections et commandes FAWE (taille, nombre de blocs modifiés, temps),
  pour qu'aucun joueur ne puisse faire tomber le serveur.
- **Extension du monde** : quand il n'y a plus de plot libre, le plugin génère de nouveaux plots (routes et
  délimitations comprises) autour des plots existants.
- **Monde** : `New World (2)`, à renommer **`kanvas`**.
- **Dimensions** : plot moyen = **49 x 49** constructibles, entouré d'une bordure en bedrock (non comprise). Route =
  **9 de large bordures en bedrock comprises** (bedrock + 7 de route + bedrock), soit un pas de grille de **58** blocs.
  Grand plot = 2 x 2 moyens + la route centrale = **107 x 107**.
- **Déblocages** : au départ 1 moyen + 1 grand. Le **2e moyen** et le **2e grand** se débloquent chacun à **100 points**
  cumulés sur un plot.
- **Votes** : en entrant dans un plot validé (qu'on a le droit de noter), le joueur **reçoit les 5 terracottas** ; il
  clique avec l'une d'elles pour voter. 1 terracotta = 1 à 5 points (rouge 1 … vert foncé 5).
- **Inventaire en visite** : à l'entrée d'un plot validé qu'on peut noter, l'inventaire est **sauvegardé** puis rendu
  à la sortie. Les 5 terracottas sont sur les **5 cases centrales de la hotbar** (cases 3 à 7), plus une **poudre de
  blaze pour signaler le plot** : menu de raisons à cocher, avec une case « Autre » où l'on écrit sa raison.
- **Déblocages précisés** : un plot **moyen** à 100 points débloque le 2e moyen ; un plot **grand** à 100 points
  débloque le 2e grand.
- **Agrandir un plot (moyen → grand)** : si les 3 plots voisins qui forment le 2 x 2 sont libres, on retire simplement
  les routes entre eux. Sinon, le plugin choisit une zone de 2 x 2 plots libres ailleurs : la construction y est
  **collée au centre**, l'ancien plot est **remis à zéro et libéré**. L'agrandissement **change le type de place
  occupée** (moyen → grand) et n'est possible que si le joueur a une **place grand libre**.
- **Plot validé** : pas d'agrandissement direct ; option **« Dupliquer en version grande »** : copie collée au centre
  d'un nouveau grand plot (en travaux, place grand requise) ; l'original reste validé avec ses votes.
- **Signalements** : raisons à cocher : contenu inapproprié, copie d'un autre build, plot vide ou bâclé, triche aux
  votes, Autre (texte libre). Message en jeu au staff connecté + **sauvegarde dans un fichier**, consultables dans
  l'**interface admin de `KV_Menu`**. Actions du staff : classer le signalement, dévalider le plot, le remettre à zéro.
- **Réserver un plot** : sur un plot libre, le joueur prend celui où il se tient (pour un grand : un groupe de 2 x 2
  plots libres qui le contient) ; ailleurs, le plot libre le plus proche du centre de la grille.
- **Menu** : `KV_Menu`, ouvert par une **étoile du Nether en emplacement 4**. Dans un plot validé pas encore noté,
  les terracottas (cases 3 à 7) la remplacent automatiquement ; dans un plot déjà noté, on revote par le bouton
  **« Voter »** du menu.
- **Génération** : première zone de **5 plots dans chaque direction** autour du plot de référence (-107, -2, -57),
  routes recopiées depuis celles qui existent.
- **Remise à zéro / suppression** (26/09/2026) : `/plot reset` (terrain vierge, plot gardé) et `/plot supprimer`
  (terrain vierge, place libérée), avec confirmation ; par le créateur sauf plot validé ; le staff toujours.
- **Grand plot libéré** (remis à zéro) : il **redevient 4 plots moyens** (routes et bordures reconstruites).
- **Titre / description** : titre de 32 caractères, description de 200, codes couleur autorisés, modifiables à tout
  moment par le créateur.
- **Classement du mois** : total des **votes reçus pendant le mois**.
- **Entités** : la limite **10 (moyen) / 25 (grand)** compte **toutes les entités sauf les peintures** (porte-armures,
  cadres, véhicules, mobs…). Peintures limitées à part : **20 (moyen) / 30 (grand)**.
- **Redstone désactivée** pour le moment.
- **Œufs d'apparition autorisés** : les mobs apparaissent sans IA, immobiles, invulnérables, silencieux, retirables en
  un clic par les constructeurs du plot (et comptés dans la limite d'entités).
- **Menu des visites** : (1) téléportation **aléatoire vers un plot validé qu'on n'a pas encore noté** ; (2) **liste
  des joueurs** (têtes) : un clic ouvre les plots du joueur ; (3) **liste de tous les plots** à explorer soi-même.
- **Lore** : chaque plot peut avoir un **titre** et une **description**, définis par le créateur (affichés à l'entrée du
  plot, dans les menus de visite et les classements).

## Architecture proposée (à valider)

- Plugin `KV_Plots` : monde de plots, réservation, éditeurs, validation, votes, déblocages, agrandissement,
  signalements, titre / description, données des classements.
- Plugin `KV_Menu` : menus joueurs (mes plots, visites, réglages du plot) et interface admin (signalements).
- Plugin `KV_ScoreBoards` : affichage des classements (menus, panneaux), appelable depuis KLM_Menu.
- Protection : une région WorldGuard par plot (membres = créateur + éditeurs), construction interdite ailleurs.
- FAWE à la place de WorldEdit (limité aux régions WorldGuard dont on est membre).
- Créatif + vol forcés à l'arrivée ; désactiver le « mode survie forcé » de KLM_Menu sur Kanvas.

## Questions ouvertes

Aucune pour les règles du jeu. Reste à valider : l'architecture et le tri des plugins ci-dessous.

## Plugins de Kanvas (tri proposé, à valider)

- Garder : KLM_Menu, LuckPerms, Floodgate, WorldGuard, FAWE (remplace WorldEdit), PlaceholderAPI (si utile), voicechat,
  ViaVersion / ViaBackwards (si le proxy ne les a pas).
- Retirer (dans `_removed-…`) : KaliumCore, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot,
  PyxelRegions, WorldEdit.
