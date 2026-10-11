# 2026-10-12 — Barème corrigé après les premiers rachats

- Plugin(s) concerné(s) : KS_Economy
- Versions avant / après : 1.7.0 en service (Kixster et Event) / 1.8.0 déployée sur les deux le 12/10/2026 à 01:17 (contient la 1.7.1)

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

## Reste à faire

- Tester en jeu ; lire la ligne « objets au prix modifié remplacés » de la console après le redémarrage.
- Retirer les points gagnés avec le cinabre sur Kixster (demande de LeKiwi06 : « retire aussi les points obtenus via
  le cinabre ») : 316 points, 3 joueurs (journal du serveur du 12/10, 00:09 à 00:30) ; `comptes.yml` à modifier serveur
  éteint.
- Silex (0,029) à 2,9 fois le gravier (0,01) ; fiole de miel à 0,05 au lieu de 0,06.
- Tables de butin des coffres de récompense (`butin.yml`) : calculées avec l'ancien barème, à régénérer ou non.
