# KV_Plots - journal

Serveur Kanvas (ex Kal-Test-Dev). Cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`. Dépend de WorldGuard (et donc
de FAWE, qui fournit l'API WorldEdit). Compilation : `sh telecharger-outils.sh` télécharge aussi les API WorldGuard
7.0.19 et WorldEdit 7.4.5 (compilation seulement ; en jeu, ce sont les plugins du serveur qui servent).

## 1.3.1 - informations du plot dans un tableau sur le côté (26/09/2026)

Demande de LeKiwi06 : « que le title soit remplacé par un affichage en sidebar, pour qu'on puisse voir les infos du
plot (créateur, éditeurs, note donnée et note globale, titre, etc.) ; seule la description doit être écrite dans le
tchat ».
- Dans un plot : tableau sur le côté de l'écran (sidebar, sans les chiffres à droite). En-tête = titre du plot (ou
  « Plot n°X ») ; lignes : « Plot n°X · taille », créateur, éditeurs (3 au plus, puis « + N autre(s) »), état
  (validé / en travaux / travaux en cours), note globale (moyenne /5), votes, points, « Ta note » (n/5, « pas
  encore » ou « ton plot »). Mis à jour toutes les 2 s et à chaque changement de plot.
- Sur les routes ou hors du monde des plots : le tableau disparaît (le tableau que le joueur avait avant est remis).
- La description reste écrite dans le chat en entrant dans le plot ; plus de titre au centre de l'écran.
- Limite : un tableau par joueur ; si un autre plugin affichait déjà une sidebar, elle est remplacée dans les plots.

**Statut : non testé en jeu, non déployé.**

## 1.3.0 - titre, description, visites (26/09/2026)

Cahier des charges : « chaque plot peut avoir un titre et une description (lore) » ; menu des visites (KV_Menu).
- **Titre** (32 caractères) et **description** (200 caractères), codes couleur `&` autorisés (non comptés), par le
  créateur, à tout moment (même plot validé) : `/plot titre <texte>`, `/plot description <texte>` (vide = retirer),
  ou le formulaire de KV_Menu. Enregistrés dans `plots.yml` (`titre`, `description`).
- **Entrée dans un plot** : titre (ou « Plot n°X ») et « par <créateur> » (« (en travaux) » si non validé) à l'écran,
  description dans le chat (remplacé en 1.3.1 par le tableau sur le côté). Rien sur les routes. `/plot info` les affiche aussi.
- **Visites** (API) : `tousLesPlots`, `plotsDuCreateur`, `hasardANoter` (plot validé au hasard, notable et pas encore
  noté par le joueur), `visiter` (téléportation au bord de n'importe quel plot), `definirLore` ; `PlotInfo` avec
  titre et description.

**Déployé sur Kanvas le 26/09/2026 (02:58) avec KV_Menu 1.2.0. Statut : non testé en jeu.**

## 1.2.0 - validation, votes, déblocage d'une 2e place (26/09/2026)

Demande de LeKiwi06 (cahier des charges, K4 à K7) ; « go » donné alors que 1.1.1 et KV_Menu 1.0.0 n'étaient pas
encore testés (empilement demandé explicitement).
- **Validation** : `/plot valider [confirmer]` (créateur ; bouton dans KV_Menu). Le plot est **figé** : sa région
  WorldGuard n'a plus ni propriétaire ni membre (personne n'y construit, FAWE compris, sauf les opérateurs). Un plot
  validé **libère sa place** : les places ne comptent que les plots en travaux.
- **Réouverture** : `/plot rouvrir [confirmer]` (créateur seulement, avec une place libre de la même taille). Les votes
  sont **gardés** ; le plot n'est pas notable pendant les travaux ; une fois revalidé, chacun peut revoter.
- **Votes** : en entrant dans un plot validé qu'il n'a pas encore noté (et dont il n'est ni créateur ni éditeur, même
  ancien), le joueur passe en **mode vote** : inventaire mis de côté (mémoire + `inventaires/<uuid>.yml`), 5
  terracottas sur les cases 3 à 7 (rouge 1, orange 2, jaune 3, vert clair 4, vert foncé 5 ; l'étoile de KV_Menu
  disparaît). Un clic avec une terracotta = vote ; l'inventaire est rendu dès qu'il vote, sort du plot, change de monde
  ou se déconnecte (et à la connexion suivante si le serveur s'est arrêté entre-temps). En mode vote, l'inventaire est
  figé (clics, créatif, jet, ramassage, échange de main annulés). Plot déjà noté : bouton « Voter » du menu ; un
  nouveau vote **remplace** l'ancien (K7). Votes enregistrés avec leur date (classement du mois).
- **Points** = somme des notes ; moyenne affichée (`/plot info`, fiche du menu).
- **Déblocage** : un plot qui atteint **100 points** (`limites.points-deblocage`) donne une place de plus de sa taille
  (`limites.maximum` : 2). Nouvelles clés de `config.yml` avec valeur par défaut dans le code (inutile de les ajouter
  au fichier du serveur).
- API : `valider`, `rouvrir`, `peutVoter`, `note`, `voter`, `enVote` ; `PlotInfo` avec points, votes, moyenne.
- Pas encore : poudre de blaze de signalement (avec les signalements), titre / description, classements.

**Déployé sur Kanvas le 26/09/2026 (02:37) avec KV_Menu 1.1.0. Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne »).**

## 1.1.1 - on reste en créatif sur Kanvas (26/09/2026)

Demande de LeKiwi06 : « quand je passe de opérateur à joueur sur Kanvas ça me met en survie, c'est pas censé le faire
sur ce serveur précis ». Cause : KLM_Menu remet toujours en survie au passage opérateur → joueur (écrit en dur dans
`toggleOperator`, sans réglage).
- Dans le monde des plots, un changement de mode de jeu vers autre chose que le créatif est annulé pour les joueurs
  non opérateurs (le vol est remis un tick après). Les opérateurs changent de mode librement. KLM_Menu n'est pas
  modifié (plugin commun au réseau).
- Limite : le message de KLM_Menu au passage en joueur reste celui de KLM_Menu.

**Déployé sur Kanvas le 26/09/2026 (02:10). Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne »).**

## 1.1.0 - remise à zéro et suppression d'un plot (26/09/2026)

Demande de LeKiwi06 : « il y a une commande pour reset ou supprimer son plot ? » ; proposition acceptée.
- `/plot reset` : le terrain du plot où l'on se tient redevient vierge (intérieur recopié depuis la tuile de référence ;
  routes intérieures d'un grand plot : sol des plots) ; le plot reste réservé, avec ses éditeurs.
- `/plot supprimer` : terrain vierge, puis le plot est libéré (sa place aussi). Un grand plot redevient **4 plots
  moyens** : routes et bordures intérieures reconstruites depuis la tuile. Sa région WorldGuard est retirée dès le
  début des travaux (plus personne n'y construit).
- Confirmation dans la minute (`/plot reset confirmer`, `/plot supprimer confirmer`). Droits : le créateur, **sauf plot
  validé** (réservé au staff, pour ne pas récupérer une place en gardant des points) ; le staff (`kvplots.admin`)
  toujours, aussi par numéro : `/kvadmin reset|supprimer <numéro> [confirmer]`.
- Entités du plot retirées (sauf joueurs), au début et à la fin des travaux. Travaux étalés sur plusieurs ticks et
  repris au redémarrage (`chantier` dans `plots.yml` ; l'ancienne clé `fusion-en-cours` de la 1.0.0 est relue).
- API : `remettreAZero`, `supprimer` (boutons de KV_Menu).

**Déployé sur Kanvas le 26/09/2026 (02:02) avec KV_Menu 1.0.0. Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne »).**

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
  anti-crash (config de FAWE) :
  `region-restrictions: true` / `mode: MEMBER` déjà en place ; limites `limits.default` réduites le 26/09/2026
  (max-changes 1 000 000, max-checks 2 000 000, max-radius 50, max-brush-radius 20, max-entities 25,
  max-blockstates 500, max-history-mb 50) ; FAWE Paper à la place de la variante Bukkit. Permissions des joueurs
  données dans LuckPerms (groupe `default` : `fawe.worldguard`, `worldedit.wand`, `worldedit.selection.*`,
  `worldedit.region.*`, `worldedit.clipboard.*`, `worldedit.history.*`, `worldedit.generation.*`, `worldedit.brush.*`).
  `worldedit-config.yml` : `navigation-wand.item` = `minecraft:structure_void` (la boussole de KLM_Menu déclenchait
  `/jumpto` / `/thru` pour les opérateurs). **Testé et confirmé par LeKiwi06 le 26/09/2026** : FAWE en joueur
  seulement dans son plot, sélection à cheval sur la bordure limitée au côté plot.
- Désactiver le « mode survie forcé » de KLM_Menu sur Kanvas.

**Déployé sur Kanvas le 26/09/2026 (01:43, sans KV_Menu), **
**Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne bien »).**
Réglage : `monde: Kanvas` (majuscule : le dossier du monde et `level-name` s'appellent `Kanvas`).
