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

## Architecture proposée (à valider)

- Plugin `KV_Plots` : monde de plots, réservation, éditeurs, validation, votes, déblocages, classements.
- Protection : une région WorldGuard par plot (membres = créateur + éditeurs), construction interdite ailleurs.
- FAWE à la place de WorldEdit (limité aux régions WorldGuard dont on est membre).
- Créatif + vol forcés à l'arrivée ; désactiver le « mode survie forcé » de KLM_Menu sur Kanvas.

## Questions ouvertes

1. Quel monde (`world` ou `New World (2)`) ? Dimensions d'un plot moyen, largeur des routes (à relever dans le monde).
   Un grand plot peut-il être formé de N'IMPORTE QUELS 2 x 2 moyens voisins, ou de blocs 2 x 2 fixés à l'avance ?
2. Limite de 2 par taille : au départ 1 moyen + 1 grand, le 2e moyen débloqué à 100 points ; et le 2e grand ?
3. Votes : menu des 5 terracottas sur le plot ? 3/5 = 3 points ?
4. Classement « du mois » : votes reçus ce mois-ci, ou plots validés ce mois-ci ?
5. Entités limitées (10 / 25) : seulement les mobs, ou aussi porte-armures, cadres, peintures, véhicules ?
6. Redstone : libre, ou limitée (horloges qui font laguer) ? Œufs d'apparition / autres objets à bloquer ?
7. Visites : téléportation sur n'importe quel plot (liste, `/plot visit <pseudo>`) ?

## Plugins de Kanvas (tri proposé, à valider)

- Garder : KLM_Menu, LuckPerms, Floodgate, WorldGuard, FAWE (remplace WorldEdit), PlaceholderAPI (si utile), voicechat,
  ViaVersion / ViaBackwards (si le proxy ne les a pas).
- Retirer (dans `_removed-…`) : KaliumCore, PlayerKits2, GrimAC, ConditionalEvents, JEIRecipeFix, Geyser-Spigot,
  PyxelRegions, WorldEdit.
