# KLM_CollisionsBedrock - journal

Plugin réseau (préfixe `KLM_`), à mettre sur chaque serveur Paper où jouent des joueurs Bedrock : quand Paper veut
renvoyer un joueur Bedrock en arrière à cause d'un bloc dont la collision diffère entre Java et Bedrock (liste
`blocs` du `config.yml`), le déplacement est accepté. Joueurs Java jamais concernés. Aucune dépendance (joueur Bedrock
reconnu à son UUID Floodgate, comme dans KG_BoatRace).

## 1.0.0 - plus de retour en arrière des joueurs Bedrock dans les bambous et les stalactites (24/10/2026, LeKiwi06)

**Demande de LeKiwi06 (24/10/2026)** : « je parle des joueurs geyser de façon général, le bamboo, dripstone, etc. leur
donne des rollback » ; choix : « prépare les deux : le petit plugin réseau pour les joueurs Bedrock et le réglage de
GrimAC pour les joueurs Java » (réglage de GrimAC : voir `REPRISE_PROJET.md`, il ne fait pas partie de ce plugin).

**Cause** (code lu le 24/10/2026, non vérifié en jeu) :
- GrimAC n'y est pour rien : il exempte les joueurs Geyser (aucune violation Bedrock dans ses bases de Kanvas et
  de Kixster).
- En Java, le bambou et la stalactite sont décalés au hasard selon leur position, boîte de collision comprise. Geyser
  2.12.0 (jar du proxy) corrige la position des joueurs Bedrock avec une forme fixe par bloc (`collisions.nbt`), sans
  ce décalage ; il a des corrections pour d'autres blocs (vitres, barreaux, lanternes, portes, échafaudages...), pas
  pour ceux-là.
- Paper 26.3 (`ServerGamePacketListenerImpl.handleMovePlayer`) renvoie alors le joueur à sa position précédente :
  sans rien écrire au journal quand il entre dans un bloc qu'il ne touchait pas (`CLIPPED_INTO_BLOCK`), avec
  « moved wrongly! » quand le serveur, bloqué par le bloc, ne retrouve pas la position du joueur (`MOVED_WRONGLY`).
  D'où beaucoup de rollbacks et peu de lignes dans le journal.

**Changements** :
- `PlayerFailMoveEvent` de Paper, joueurs Bedrock seulement, déplacement d'au plus `deplacement-max` bloc (1 par
  défaut) :
  - `MOVED_WRONGLY` accepté (et plus noté au journal) si un bloc de la liste est sur le trajet ;
  - `CLIPPED_INTO_BLOCK` accepté si tous les blocs que le joueur touche à sa nouvelle position sont de la liste.
    Paper l'annonce aussi après un `MOVED_WRONGLY` refusé : dans ce cas il n'est jamais accepté.
- `config.yml` : `blocs` (`BAMBOO`, `POINTED_DRIPSTONE` par défaut, aussi sans fichier), `deplacement-max`,
  `journal-des-renvois` et `intervalle-journal`. Changement pris en compte au redémarrage.
- **Journal des renvois** (nécessaire pour les « etc. » de la demande) : quand un joueur Bedrock est quand même
  renvoyé, une ligne `Renvoi Bedrock : <joueur> entre dans <blocs> (monde x y z)` ou `... mouvement refusé près de
  <blocs> ...`, au plus une toutes les 5 s par joueur. Les blocs qui reviennent souvent sont à ajouter à `blocs`.

**Limites** : un joueur Bedrock peut entrer un peu dans les blocs de la liste, et dans aucun autre (comme son jeu le
lui permet déjà). Les bateaux et autres véhicules ne sont pas concernés (contrôle séparé de Paper). Un rollback venu
d'autre chose qu'un bloc (lag, GrimAC pour les Java) n'est pas touché.

**Compilé, non déployé, non testé en jeu.** À déployer sur Kixster, Kanvas (Kal-Games), le lobby et Serveur Jeux.
À vérifier en jeu (joueur Bedrock) : marcher, courir et sauter dans une bambouseraie et contre des stalactites
sans être ramené en arrière ; lire les lignes « Renvoi Bedrock » du journal après quelques jours.
