# KV_Plots - journal

Serveur Kanvas (ex Kal-Test-Dev). Cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`. Dépend de WorldGuard (et donc
de FAWE, qui fournit l'API WorldEdit). Compilation : `sh telecharger-outils.sh` télécharge aussi les API WorldGuard
7.0.19 et WorldEdit 7.4.5 (compilation seulement ; en jeu, ce sont les plugins du serveur qui servent).

## 1.0.0 - génération de la grille, réservation, éditeurs, protection (26/09/2026)

Demande de LeKiwi06 : commencer par la grille de plots, la réservation et la protection.
- **Grille** (`config.yml`, section `grille`) : intérieur de 49 x 49, route de 9 (bordures en bedrock comprises), soit
  un pas de 58. `origine-x` / `origine-z` = coin nord-ouest de l'intérieur du plot (0, 0) : **-107 / -57** (relevé par LeKiwi06 ; sol
  à y = -2, le dessous est constructible aussi). `colonne-min/max`, `ligne-min/max` = plots déjà construits dans le
  monde : **à régler avant le premier démarrage** (-5 à 5 provisoire).
- **Réservation** : `/plot reserver <moyen|grand>` : le plot libre où l'on se tient (grand : un groupe de 2 x 2 plots
  libres qui le contient, le plus proche du joueur), sinon le plus proche du plot (0, 0). 1 moyen + 1 grand au plus
  (`limites`). Téléportation sur la route, au milieu du bord nord du plot.
- **Tuile de référence** (`modele-tuile.yml`) : relevée au premier démarrage, un carré de 58 x 58 colonnes à partir de
  (-107, -57) = intérieur du plot (0, 0), route à l'est, route au sud, croisement (bedrock et décor compris ; toute la
  hauteur jusqu'au plus haut bloc). **Le plot (0, 0) et ses routes est et sud doivent être vierges et complets à ce
  moment-là.** Si `modele-tuile.yml` est supprimé, il est relevé à nouveau au démarrage suivant.
- **Génération** (demande de LeKiwi06 : « une première génération d'une zone de 5 plots dans chaque direction ») :
  `/kvadmin generer`, puis `/kvadmin generer confirmer` dans la minute (opérateurs, `kvplots.admin`). La tuile est
  recopiée sur toute la grille configurée (colonnes et lignes -5 à 5 = 121 plots moyens) et sur les routes qui
  l'entourent : x de -406 à 240, z de -356 à 290. Tout ce qui s'y trouve est remplacé, sauf les plots réservés.
  Avancement dans la console tous les 10 %. Estimation : quelques minutes, sans bloquer le serveur.
- **Grand plot** : les routes et bordures intérieures sont remplacées par le sol des plots (colonne centrale de la
  tuile), colonne par colonne, sur plusieurs ticks (`blocs-par-tick`, 20 000 par défaut). Travaux repris au
  redémarrage s'ils ont été interrompus.
- **Éditeurs** : `/plot editeur <ajouter|retirer> <pseudo>` (créateur seulement, sur son plot) ; historique des
  éditeurs gardé (pour les votes). `/plot liste`, `/plot tp [numéro]`, `/plot info`.
- **Protection WorldGuard** : région `kv_plot_<n>` par plot (propriétaire = créateur, membres = éditeurs, priorité 10,
  toute la hauteur) ; région `__global__` du monde en `passthrough deny` (construction interdite hors de ses plots ;
  les opérateurs contournent WorldGuard).
- **Règles du monde** : créatif + vol à l'arrivée (un tick après, pour passer après KLM_Menu) ; pas de TNT (pose, wagon,
  allumage), aucune explosion ; rien ne brûle (propagation, lave, foudre) ; les liquides coulent mais ne sortent pas
  de leur plot ; redstone désactivée (courant forcé à 0, pistons et distributeurs bloqués).
- **API** pour KV_Menu : `fr.kalium.kvplots.api.KanvasPlots` (registre de services de Paper) : plots d'un joueur,
  plot à un endroit, monde des plots, places, réserver, téléporter, ajouter / retirer un éditeur.
- Données : `plots.yml` (plots, éditeurs, historique, état).
- Techniquement nécessaire, non demandé explicitement : le plugin charge lui-même le monde `kanvas` s'il n'est pas le
  monde principal du serveur.

Limites connues :
- Hors du monde `kanvas` configuré, rien n'est protégé par KV_Plots.
- Les routes reproduisent la tuile de référence : un décor qui ne se répète pas tous les 58 blocs n'est pas recopié.
- La redstone est coupée au niveau du courant : certains blocs (observateurs, rails) peuvent encore réagir en partie.
- Pas encore : validation, votes, déblocages, agrandissement d'un plot moyen, remise à zéro, limites d'entités, mobs
  sans IA, titre / description, menus (KV_Menu), extension du monde.

À faire au déploiement (serveur Kanvas) :
- ~~Renommer `New World (2)`~~ : fait par LeKiwi06 le 26/09/2026, en `Kanvas` (monde principal).
- ~~FAWE à la place de WorldEdit~~ (2.15.4, fait par LeKiwi06) ; reste à régler `region-restrictions: true` et les limites
  anti-crash (config de FAWE).
- Désactiver le « mode survie forcé » de KLM_Menu sur Kanvas.

**Déployé sur Kanvas le 26/09/2026 (01:43, sans KV_Menu), en attente de redémarrage. Statut : non testé en jeu.**
Réglage : `monde: Kanvas` (majuscule : le dossier du monde et `level-name` s'appellent `Kanvas`).
