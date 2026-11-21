# KS_Crafts - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.21.1 - plus de blocs de cuivre brut par craft (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il y a un craft buggué avec le cuivre oxydé [...] 4 blocks oxydés qui donnent 4 cuivre brut ,
je ne t'ai jamais demandé ce craft , retire le ». Recette `ks_crafts:raw_copper_block` retirée (4 blocs de cuivre, de
n'importe quelle oxydation, cirés ou non -> 4 blocs de cuivre brut ; venue du cahier des charges d'Event, 1.0.0). Les
recettes des blocs de fer et d'or bruts restent. Didacticiel : la recette n'est plus dans « Crafts pratiques » (simple
correction, date `nouveau` inchangée).

**Compilé, non déployé, non testé en jeu.** À envoyer sur Kixster avec `didacticiel.yml`.

## 1.22.0 - spawners en forme de jambières (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « les spawner : le craft mal fait [...] la tête du mob doit être placé sous le spawner avec une
forme de jambière en fragments de spawners ». Les 11 recettes de spawners (mêmes ids) ont maintenant une forme :

| | gauche | milieu | droite |
|---|---|---|---|
| haut | Fragment | Fragment | Fragment |
| milieu | Fragment | Cœur de Spawner | Fragment |
| bas | Fragment | tête du mob | Fragment |

Mêmes ingrédients qu'avant (7 fragments, 1 cœur, 1 tête vanilla ou de KS_Decapitator) ; seule la place compte
maintenant. Livre de recettes inchangé (débloquées par le premier Fragment de Spawner). Le nom « Spawner à zombie »
vient de KS_Spawners 1.1.0. Didacticiel : texte de « Spawners » mis à jour, marqué Nouveau le 26/10.

**Compilé, non déployé, non testé en jeu.** Contient la 1.21.1 (cuivre brut retiré), pas encore déployée. À déployer
avec KS_Spawners 1.1.0 et `didacticiel.yml`.

**Déployé sur Kixster le 26/10/2026 à 19:48 (LeKiwi06 : « ok vas y déploie ça me conviens »), actif après redémarrage de Kixster. Statut : non testé en jeu.**

## 1.23.0 - décraft des bateaux et minecarts spéciaux (28/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'aimerais qu'on puisse décrafter les bateaux et minecarts spéciaux (jukebox et
renforcé) : si un craft ne peut pas donner 2 résultats différents, alors supprimer le véhicule et conserver le bloc
spécial ». Choix (questions du 28/10/2026) : les deux rendus (le bloc spécial en résultat, le véhicule simple reste
dans la grille, comme le seau vide d'un gâteau) ; le wagon jukebox de KS_Trains aussi.

- Un véhicule spécial **seul** dans la grille (établi ou inventaire) :
  - bateau à jukebox -> jukebox, le bateau reste ; bateau à coffre et jukebox -> jukebox, le bateau à coffre reste ;
  - bateau, radeau ou minecart avec coffre renforcé -> coffre renforcé, le bateau, le radeau ou le minecart reste ;
  - wagon jukebox -> jukebox, le minecart reste.
- Le véhicule simple est posé dans la case du véhicule spécial au tick suivant (le jeu retire l'objet après le craft) ;
  inventaire fermé entre-temps : il va dans l'inventaire (au sol s'il est plein). Rien n'est rendu si le craft n'a pas
  eu lieu (Maj + clic avec l'inventaire plein).
- **Crafter** : même résultat ; le véhicule simple reste dans sa case (comme les restes d'un craft vanilla), ou tombe
  au-dessus du crafter si la case est prise.
- Une seule recette (`ks_crafts:decraft_vehicule` : n'importe quel bateau ou minecart seul), cachée du livre de
  recettes ; le résultat est choisi dans `Decrafts`, et refusé pour un véhicule ordinaire. Passe après le refus de
  KS_JukeboxBoat (priorité normale) ; KS_CoffreRenforce 1.2.0 laisse passer cette recette (**à déployer ensemble**).

Limite : sur Bedrock, le livre de recettes peut montrer « bateau -> jukebox » pour n'importe quel bateau (Geyser
traduit les recettes) ; un bateau ordinaire ne donne rien. Non vérifié en jeu.

Compilé le 28/10/2026 (`sortie/KS_Crafts-1.23.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Kixster le 28/10/2026 à 10:49** (LeKiwi06 : « Oui, déploie KS_Crafts 1.23.0 et KS_CoffreRenforce 1.2.0 sur Kixster avec le didacticiel »), avec `didacticiel.yml` : jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, rangés dans `_removed-ks_crafts-1.22.0/` et `_removed-ks_coffrerenforce-1.1.0/` ; fichiers du serveur vérifiés identiques à ceux compilés. À redémarrer par LeKiwi06. Statut : non testé en jeu.

## 1.24.0 - plus de graines de torchiflore ni de cosses de plante carnivore par craft (02/11/2026, LeKiwi06)

**Demande de LeKiwi06** (cultures de Kixster, cahier local `Blocs_Cultures_Objets.md`, partie 14, validée le 02/11) :
le composteur de KS_Cultures donne 1 à 3 graines par fleur, torchiflore et plante carnivore comprises ; leurs crafts
sont retirés.

- Recettes retirées : `ks_crafts:pitcher_pod` (2 plantes carnivores -> 4 cosses) et `ks_crafts:torchflower_seeds`
  (2 torchiflores -> 4 graines).
- **À déployer avec KS_Cultures 1.0.0** (sinon plus aucun moyen de tirer des graines de ces fleurs) et avec un
  `didacticiel.yml` sans ces deux recettes (fiche « Crafts pratiques » de KS_Didacticiel).

Compilé le 02/11/2026 (`sortie/KS_Crafts-1.24.0.jar`). **Statut : non déployé, non testé en jeu.**

**21/11/2026 :** à déployer seulement à la **validation** de KS_Cultures en survie (`/mondetest valider KS_Cultures`),
pas à son premier déploiement : KS_Cultures arrive en test, et ce retrait s'appliquerait tout de suite en survie.
