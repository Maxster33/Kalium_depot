# KV_Plots - journal

Serveur Kanvas (ex Kal-Test-Dev). Cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`. Dépend de WorldGuard (et donc
de FAWE, qui fournit l'API WorldEdit). Compilation : `sh telecharger-outils.sh` télécharge aussi les API WorldGuard
7.0.19 et WorldEdit 7.4.5 (compilation seulement ; en jeu, ce sont les plugins du serveur qui servent).

## 1.0.0 - grille, réservation, éditeurs, protection (26/09/2026)

Demande de LeKiwi06 : commencer par la grille de plots, la réservation et la protection.
- **Grille** (`config.yml`, section `grille`) : intérieur de 49 x 49, route de 9 (bordures en bedrock comprises), soit
  un pas de 58. `origine-x` / `origine-z` = coin nord-ouest de l'intérieur du plot (0, 0) ; `colonne-min/max`,
  `ligne-min/max` = plots déjà construits dans le monde. **À régler avant le premier démarrage** (valeurs par défaut
  provisoires).
- **Réservation** : `/plot reserver <moyen|grand>` : le plot libre où l'on se tient (grand : un groupe de 2 x 2 plots
  libres qui le contient, le plus proche du joueur), sinon le plus proche du plot (0, 0). 1 moyen + 1 grand au plus
  (`limites`). Téléportation sur la route, au milieu du bord nord du plot.
- **Grand plot** : les routes et bordures intérieures sont remplacées par le sol des plots, colonne par colonne, sur
  plusieurs ticks (`blocs-par-tick`, 20 000 par défaut ; environ 2 s). Le sol est relevé une fois au centre du plot
  (0, 0) au premier démarrage (`modele-sol.yml`) : **ce plot doit être vierge à ce moment-là**. Travaux repris au
  redémarrage s'ils ont été interrompus.
- **Éditeurs** : `/plot editeur <ajouter|retirer> <pseudo>` (créateur seulement, sur son plot) ; historique des
  éditeurs gardé (pour les votes). `/plot liste`, `/plot tp [numéro]`, `/plot info`.
- **Protection WorldGuard** : région `kv_plot_<n>` par plot (propriétaire = créateur, membres = éditeurs, priorité 10,
  toute la hauteur) ; région `__global__` du monde en `passthrough deny` (construction interdite hors de ses plots ;
  les opérateurs contournent WorldGuard).
- **Règles du monde** : créatif + vol à l'arrivée (un tick après, pour passer après KLM_Menu) ; pas de TNT (pose, wagon,
  allumage), aucune explosion ; rien ne brûle (propagation, lave, foudre) ; les liquides coulent mais ne sortent pas
  de leur plot ; redstone désactivée (courant forcé à 0, pistons et distributeurs bloqués).
- Données : `plots.yml` (plots, éditeurs, historique, état).
- Techniquement nécessaire, non demandé explicitement : le plugin charge lui-même le monde `kanvas` s'il n'est pas le
  monde principal du serveur.

Limites connues :
- Hors du monde `kanvas` configuré, rien n'est protégé par KV_Plots.
- La redstone est coupée au niveau du courant : certains blocs (observateurs, rails) peuvent encore réagir en partie.
- Pas encore : validation, votes, déblocages, agrandissement d'un plot moyen, remise à zéro, limites d'entités, mobs
  sans IA, titre / description, menus (KV_Menu), extension du monde.

À faire au déploiement (serveur Kanvas) :
- Renommer le dossier du monde `New World (2)` en `kanvas` (et `level-name` si c'est le monde principal).
- FAWE à la place de WorldEdit, avec `region-restrictions: true` et les limites anti-crash (config de FAWE).
- Désactiver le « mode survie forcé » de KLM_Menu sur Kanvas.

**Statut : non testé en jeu, non déployé.**
