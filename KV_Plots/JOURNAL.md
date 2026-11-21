# KV_Plots - journal

Serveur Kanvas (ex Kal-Test-Dev). Cahier des charges : `KV_Plots/CAHIER_DES_CHARGES.md`. Dépend de WorldGuard (et donc
de FAWE, qui fournit l'API WorldEdit). Compilation : `sh telecharger-outils.sh` télécharge aussi les API WorldGuard
7.0.19 et WorldEdit 7.4.5 (compilation seulement ; en jeu, ce sont les plugins du serveur qui servent).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.8.2 - Wind Burst de la masse rétabli pour de bon (26/10/2026, LeKiwi06)

Retour de LeKiwi06 après la 1.8.1 : « ça ne fonctionne toujours pas ... » ; masse enchantée Wind Burst sur un golem de
fer (Résistance 255), vers x 200000, z 100000 du monde Kanvas ; « j'arrive à le frapper , mais c'est comme si ma mace
n'était pas enchanté ».

- **Cause** (vérifiée dans Paper 26.3, version de Kanvas) : l'explosion du Wind Burst n'a **pas d'entité source**
  (`wind_burst.json` ne met pas `attribute_to_user`, donc `ExplodeEffect` passe `null`). Sans source, Paper déclenche
  `BlockExplodeEvent` et non `EntityExplodeEvent` : `onBlockExplode` l'annulait toujours, d'où aucun envoi de la
  poussée. L'analyse de la 1.8.1 sur ce point était fausse.
- Écartés en lisant leur code : WorldGuard 7.0.19 (le drapeau `wind-charge-burst` non réglé laisse passer la poussée
  qu'on se donne à soi-même ; `other-explosion` autorisé par défaut ; les blocs protégés sont retirés, l'explosion
  n'est pas annulée), KalGames, KLM_*, GrimAC (aucune alerte), la Résistance 255 (un coup à 0 dégât déclenche quand
  même les enchantements).
- **Correctif** : `onBlockExplode` traite le souffle de vent comme `onEntityExplode` (`TRIGGER_BLOCK` : liste de blocs
  vidée, explosion gardée) ; les autres explosions de blocs (lits, ancres de réapparition) restent annulées.

**Déployé sur Kanvas le 26/10/2026 à 22:29 (LeKiwi06), actif après redémarrage, non testé en jeu.** 1.8.1 rangée
dans `_removed-kv_plots-1.8.1/`.

## 1.8.1 - charges de vent et Wind Burst rétablis (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « je suis entrain de build un parkour sur kanvas , mais le wind burst ne fonctionne pas ».

- **Cause** (vérifiée dans le code de Paper 26.2) : `ReglesMonde` annulait toutes les explosions du monde des plots.
  La charge de vent passe par `ExplosionPrimeEvent` (annulé : elle n'explose pas du tout) ; le Wind Burst de la masse
  passe par `EntityExplodeEvent` (annulé : `ServerLevel.explode` s'arrête avant d'envoyer au joueur le paquet qui porte
  sa poussée, son mouvement étant géré par son client).
- **Correctif** : `ExplosionPrimeEvent` laissé passer pour les charges de vent (`AbstractWindCharge`) ;
  `EntityExplodeEvent` d'un souffle de vent (`ExplosionResult.TRIGGER_BLOCK`) non annulé, mais sa liste de blocs vidée :
  la poussée fonctionne, aucune porte, trappe, cloche ou bouton n'est actionné (pas d'effet sur le plot d'un voisin).
  Les autres explosions restent annulées.
- Empilée sur la 1.8.0 (non déployée) : déployer la 1.8.1 déploie aussi la 1.8.0.

**Déployé sur Kanvas le 26/10/2026 à 21:39 (LeKiwi06), avec la 1.8.0, actif après redémarrage, non testé en jeu.**
1.7.0 rangée dans `_removed-kv_plots-1.7.0/`.

## 1.8.0 - plots des concours terminés notables, classement figé (25/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai qu'on puisse voter les builds de concours après la fin de la phase de vote ».
Choix de LeKiwi06 (questions posées avant le code) : classement **figé** à la clôture, **tous** les anciens concours
concernés, notes tardives comptées pour les points des bâtisseurs **comme sur un plot normal**.

- **Votes** : à la fin des votes d'un concours, ses plots restent notables avec les terracottas (mode vote, « Au hasard :
  un plot à noter »), aux mêmes conditions qu'un plot normal (ni créateur ni éditeur, même ancien). Seul un concours
  **annulé** ferme encore les votes (ses plots sont supprimés). Les plots des concours déjà terminés redeviennent
  notables au démarrage.
- **Classement figé** : à la clôture, le classement (plot, points, nombre de votes) est enregistré dans `concours.yml`
  (clé `classement`, liste « plot:points:votes »). `plotsDuConcours` d'un concours terminé renvoie ce classement, avec
  les points, votes et moyenne **de la clôture** : le menu « Anciens concours » (vainqueur, « 1. », « 2. »..., points
  affichés) ne bouge plus. La fiche du plot et le tableau sur le côté montrent les notes à jour.
- **Anciens concours** (terminés avant la 1.8.0) : au premier démarrage, leur classement est figé d'après les notes
  actuelles, qui sont celles de la clôture (votes fermés jusque-là), et enregistré.
- **Points des bâtisseurs** (KV_Rewards) : une note donnée après la clôture compte comme sur un plot normal (paliers,
  tops) ; les récompenses des places du concours, données à la clôture, ne changent pas. Aucun changement dans
  KV_Rewards ni KV_Menu.
- Annonce de fin des votes : « Le classement est définitif, mais les plots restent notables. » ajouté (choix de Claude,
  signalé : « Votes clos » seul aurait laissé croire qu'on ne peut plus noter).
- Empilée sur la 1.7.0, jamais testée en jeu (demande explicite de LeKiwi06).

**Compilé le 25/10/2026 (`sortie/KV_Plots-1.8.0.jar`) ; déployé avec la 1.8.1 le 26/10/2026 à 21:39. Statut : non testé
en jeu.**

## 1.7.0 - un concours en construction pendant les votes du précédent, tableau plus court (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « fait en sorte qu'on puisse avoir un concours de build en cours pendant que le précédent est en phase de votes , essaie aussi de reduire tout les sidebar trop long , sur kanvas , mais aussi pour kixster " exploration journalière : 10 / 10 " est bien trop long " exploration : 0/10 " suffit ».

- **Concours** : un seul concours **en construction** à la fois, comme avant, mais un nouveau concours peut être lancé
  pendant que le précédent est **en votes** (avant : « Un concours est déjà en cours. » tant que les votes n'étaient
  pas clos). Si la construction du nouveau se termine avant la fin des votes de l'ancien, les deux sont en votes en même
  temps ; chacun se clôt à sa date.
  - Participer et annuler sa participation : concours en construction. Les plots d'un concours en votes restent figés
    et notables.
  - Passages de phase automatiques : chaque concours actif est suivi (toutes les 20 s).
  - API : `concoursActifs()` (celui en construction d'abord, puis ceux en votes), `participation(joueur, concours)`,
    et pour le staff `modifierConcours`, `cloreVotes`, `annulerConcours` avec le numéro du concours. Les anciennes
    méthodes restent (`concoursActuel()` : le concours en construction, sinon le plus ancien en votes).
  - `concours.yml` : format inchangé.
- **Tableau sur le côté** (dans un plot) : lignes raccourcies.
  - « Concours de build : thème » → « Concours : thème » ; « Fin dans : 2 j 3 h 10 min » → « Fin : 2 j 3 h » ;
    « Votes : fin dans ... » → « Fin des votes : 2 j 3 h ». Durée courte : jours et heures, ou heures et minutes, ou
    minutes. Un concours en construction et un concours en votes s'affichent ensemble (3 lignes).
  - « Ancien concours : thème » → « Ancien concours » ; « État : travaux en cours » → « État : travaux » ;
    « Note globale : » → « Note : ».
  - **Texte qui défile** (demande suivante de LeKiwi06, le même jour, avant tout déploiement : « pour les lignes trop
    longues comme un thème de build , rend le déroulant de la meme façon que la boss bar dans le lobby ») : le thème du
    concours et le titre du plot, écrits par le staff et les joueurs, défilent quand ils dépassent leur place (une ligne
    de 26 caractères, libellé compris). Comme le texte défilant de KLM_Hub : un caractère tous les 3 ticks, un arrêt
    de 3 secondes quand le début du texte est affiché, 4 espaces avant son retour ; les couleurs « & » sont gardées.
    Un texte qui tient dans sa place ne bouge pas.
  - Pour cela, le tableau n'est plus recréé toutes les 2 secondes : seulement quand son nombre de lignes change ;
    sinon ses lignes sont réécrites en place, et les lignes qui défilent toutes les 3 ticks.
  - Non raccourcis : les pseudos (créateur, éditeurs).
- Contient la 1.6.0 (points de notes au carré), jamais déployée.

À déployer avec KV_Menu 1.4.0 (son menu des concours utilise la nouvelle API).

**Déployé sur Kanvas le 11/10/2026 à 09:00 (LeKiwi06, envoi groupé : KV_Plots 1.7.0, KV_Menu 1.4.0 et `butin.yml` de KV_Rewards ; accord de LeKiwi06 : « oui déploie tout » ; 1.5.0 dans `_removed-kv_plots-1.5.0/`), actif après redémarrage de Kanvas. Statut : non testé en jeu.**
