# KS_Encapsuleur — journal

## 1.0.0 — 2026-11-02 — non testé en jeu, non déployé

**Demande** (LeKiwi06, cahier local « Blocs, cultures et objets », section 3, validé le 02/11/2026 ; « fait les 2 ») :
un objet à usage unique qui emporte une construction (64 × 64 × 128 au plus) et la repose ailleurs, petit à petit.
Choix de LeKiwi06 le 02/11 : sélection par deux coins à l'objet ; l'objet devient la construction ; aperçu calé sur le
bloc visé, face avant vers le joueur.

**Fonctionnement**
- Craft (clé `ks_encapsuleur:encapsuleur`) : un crafter au centre, 8 shulkers améliorées **vides** autour
  (KS_ShulkerAmelioree) ; une boîte ordinaire ou une shulker améliorée pleine ne donne rien (crafter compris).
- Objet : bloc de structure (que seuls les opérateurs peuvent poser, et jamais posé : refusé par le plugin) à l'aspect
  enchanté, pile de 1. Bedrock : objet du jeu, rien à ajouter sur le proxy.
- Sélection : encapsuleur en main, clic gauche sur un bloc = coin 1, clic droit = coin 2 ; contour en particules (bleu,
  rouge si trop grand) vu du seul joueur ; accroupi + clic droit : menu Encapsuler / Effacer la sélection.
- Encapsuler : toute la zone dans des claims SimpleClaimSystem **dont le joueur est le propriétaire**. Restent sur
  place : contenants pleins (coffres, tonneaux, fours, entonnoirs, alambics, crafters, coffres renforcés et shulkers
  améliorées pleins...), objets posés dans un bloc (feu de camp, jukebox, pupitre, pot décoré, bibliothèque sculptée,
  étagère, bloc suspect), blocs incassables. S'il y en a : menu « Attention, certains blocs ne sont pas encapsulés »
  (Voir la liste par type avec nombre et positions, Valider, Annuler ; l'objet n'est consommé qu'à la validation, et la
  liste est refaite : un nouveau bloc restant redemande la validation). Emportés avec leurs données : panneaux,
  bannières, têtes, ruches et leurs abeilles, spawners de KS_Spawners (marque dans le bloc), blocs custom de KS_Blocs
  (registre déplacé), sources d'eau et de lave. Les entités restent.
- Stockage : structure du jeu dans `plugins/KS_Encapsuleur/constructions/<id>.nbt` (blocs et leurs données, sans
  entités) + `<id>.yml` (blocs restés sur place, blocs custom, face avant, propriétaire) ; l'objet porte l'id. Une
  construction posée est rangée dans `constructions/posees/` (jamais supprimée).
- Retrait de la zone : 4 000 blocs par tick, de haut en bas, sans mise à jour des voisins ; la zone est protégée pendant
  le retrait (casse, pose, clics, explosions, pistons, fluides, entonnoirs) pour qu'aucun bloc ni objet ne soit à la
  fois dans la construction et dans le monde ; à l'arrêt du serveur, le retrait est terminé d'un coup.
- Pose : clic droit sur un bloc avec la construction : aperçu en faux blocs (arêtes en vitre bleu clair, face avant en
  vitre verte, coins en lanternes de mer) ; menu : tourner à gauche / à droite, miroir, éloigner, rapprocher, gauche,
  droite, monter, descendre, Valider, Annuler. Valider : zone dans des claims où le joueur a les droits (propriétaire ou
  membre) ; chaque bloc posé ne remplace que l'air, l'eau, l'herbe (courte, haute, fougères) et la neige, sinon refus,
  blocs gênants en vitre rouge (400 au plus) et liste par type. L'air de la construction ne remplace rien. La
  construction est retirée de l'inventaire, puis posée à 1 500 blocs par tick, de bas en haut, eau et lave à la fin
  (elles recoulent), blocs custom retenus par KS_Blocs à la fin.
- Reprise : `chantiers.yml` (retraits et poses en cours, avancement noté toutes les 5 s) ; une pose reprend au
  redémarrage.
- Journal du serveur : chaque encapsulage et chaque pose (joueur, id, monde, position).

**Limites**
- Une grande zone (500 000 blocs) se lit et s'enregistre d'un coup à l'encapsulage : petit arrêt du serveur possible.
- Les blocs voisins de la zone ne sont pas mis à jour au retrait (une torche fixée sur un bloc emporté reste en l'air
  jusqu'à la prochaine mise à jour).
- Un bloc sur deux cases (porte, lit) coupé par le bord de la zone n'est emporté qu'à moitié.
- Créatif : un objet copié ne peut pas être posé deux fois (la construction posée n'existe plus).
- Tests dans « Kixster Test » : il faut un claim SimpleClaimSystem dans ce monde.
- Rotation et miroir : calculés comme le jeu (miroir puis rotation), à vérifier en jeu sur les escaliers et portes.
