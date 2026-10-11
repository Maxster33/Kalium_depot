# 2026-10-12 — Barème corrigé après les premiers rachats

- Plugin(s) concerné(s) : KS_Economy
- Versions avant / après : 1.7.0 en service (Kixster et Event) / 1.8.0 déployée sur les deux le 12/10/2026 à 01:17, puis 1.8.1 à 01:32

## Demandé

LeKiwi06 : « la nouvelle économie vient de sortir et le cinabre est bien trop boosté, il vaut autant que du fer ».
Puis : trouver les autres anomalies du genre et les lister (« 18 œufs bruns pour une émeraude, c'est bien trop, c'est
un revenu passif d'avoir des poules sur un hopper »), appliquer ses décisions, et « refaire un tirage d'économie pour
les items qui ont été modifiés, on tire un nouvel objet à la place ».

## Fait

- Cause du cinabre : note provisoire « à vérifier en jeu » jamais vérifiée (0,0764 pour un fer à 0,10).
- Liste d'anomalies en 7 catégories remise à LeKiwi06 (erreurs franches, revenu passif, poudre d'os, blocs abondants,
  butin de monstres, objets jamais vérifiés, cuivre brut).
- KS_Economy 1.7.1 puis 1.8.0 : `rachats.csv` régénéré (678 prix changent sur 1 929) ; une offre de la semaine dont le
  prix du barème a changé est remplacée par un nouveau tirage. Détail : `KS_Economy/JOURNAL.md`.

## Décisions

- Un bloc abondant une fois son biome trouvé vaut la pierre taillée, 0,01 (« vu la taille de la carte ça aura le temps
  de ruiner l'économie avant qu'on épuise la ressource »).
- Un objet produit sans action du joueur est un revenu passif : prix très bas (œuf 0,01).
- Butin de monstres et poissons divisé par 2 ; disque Bounce au prix du Relic (mineshafts des sulfur caves) ; cartes de
  structure et carte au trésor à 10 ; cuivre brut au prix du lingot (lingots abondants dans les trial chambers) ;
  colorant blanc au prix de la poudre d'os ; coussin = 1,5 laine ; `straw_bed` = 0,75 botte de foin.
- Catégories 1 à 4 de la liste validées telles que proposées ; reste de la catégorie 6 validé tel quel.

## Déploiement (12/10/2026, 01:17)

Accord de LeKiwi06 : « oui déploie tout ». KS_Economy 1.8.0 sur Kixster et Event, 1.7.0 dans
`_removed-ks_economy-1.7.0/`, jars en place vérifiés identiques à `jars-deployes/` avant l'envoi. Active après
redémarrage ; non testée en jeu. Perle de l'Ender laissée à 0,5 (« ils ne savent pas pour l'endermite »).

## Points du cinabre retirés (12/10/2026, 01:24)

**Fait le 12/10/2026 à 01:24, Kixster arrêté (LeKiwi06 : « kixster est arrêté, retire tout »)** : 316 points retirés de `plugins/KS_Economy/comptes.yml` (3 soldes : - 250, - 57, - 9 ; aucun n'est passé sous zéro ; montants de `rachats.yml` recoupés avec le journal du serveur ; les murets vendus ne sont pas rendus) ; ancien fichier dans `/plugins/_removed-ks_economy-comptes-2026-10-12/` ; fichier relu identique après l'envoi. Kixster était arrêté depuis 00:33 : aucune vente de cinabre après le relevé.

## Journal de Kixster après redémarrage (12/10/2026, 01:25)

**Journal de Kixster lu le 12/10/2026 après le redémarrage de 01:25 (demande de LeKiwi06)** : KS_Economy 1.8.0 active, « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur. Ligne attendue présente : « Rachats de la semaine 2026-S42 : objets au prix modifié remplacés (waxed_oxidized_chiseled_copper, dripstone_block, brown_egg, cinnabar_brick_wall, diorite_wall) » ; les 5 autres offres sont gardées, les 5 nouvelles sont `golden_apple`, `potion(tissage)`, `ladder`, `mushroom_stem`, `deepslate_lapis_ore`. `rachats.yml` : les 10 offres portent leur prix du barème. `comptes.yml` : soldes corrigés conservés après le redémarrage. Statut : vérifié dans la console, non testé en jeu. Event : pas encore redémarré.

## Pied de champignon doublé (KS_Economy 1.8.1)

LeKiwi06 : « les troncs de champignons sont trop peu chers, ils sont pénibles à avoir (plus que les blocs marron et rouge), il faut donc doubler le prix des tiges de champignon ». `mushroom_stem` 0,03 au lieu de 0,015 ; compilé, non déployé.

Event : journal lu à 01:29, aucun redémarrage depuis le 11/10 à 17:36 (toujours en 1.7.0).

## Reste à faire

- KS_Economy 1.8.1 déployée sur Kixster et Event le 12/10/2026 à 01:32 (« oui déploie tout ») : active après redémarrage de chaque serveur ; lire la console (« objets au prix modifié remplacés »).

- Tester en jeu ; redémarrer Event et y lire la ligne « objets au prix modifié remplacés ».
- Silex (0,029) à 2,9 fois le gravier (0,01) ; fiole de miel à 0,05 au lieu de 0,06.
- Tables de butin des coffres de récompense (`butin.yml`) : calculées avec l'ancien barème, à régénérer ou non.
