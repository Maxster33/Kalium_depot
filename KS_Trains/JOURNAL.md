

**Actif sur Event depuis son redémarrage du 17/10/2026 à 04:18 (journal lu, heures en UTC : 02:18) : KS_Trains 1.4.1 activé sans erreur ni avertissement (après KLM_Contacts 1.3.0), démarrage en 12 s. Statut : non testé en jeu.**

## 1.4.2 - modèle agrandi orienté sur l'axe du train (17/10/2026, LeKiwi06)

Signalement de LeKiwi06 (vidéo sur Event) : « le bloc bouge dans tous les sens en pente ».

- **Cause** (défaut de la 1.3.7) : l'inclinaison venait du bloc de rail sous le wagon ; sur une pente, le wagonnet est à
  mi-hauteur entre deux blocs et le bloc trouvé change d'un tick à l'autre : le modèle basculait sans cesse entre
  penché et droit, et sa direction changeait en même temps (orientation du wagonnet du jeu, ou point cardinal du four).
- **Correction** : le modèle est orienté sur l'**axe du train** à ce wagon (du wagon de derrière au wagon de devant ;
  aux deux bouts, le wagon lui-même) : sa face avant pointe vers l'avant du train (fours : exactement, plus d'arrondi
  au point cardinal ; coffres aussi), et il penche exactement comme la voie, avec une transition douce en haut et en bas
  des pentes et dans les virages. Rotation autour de son centre, changement adouci sur 2 ticks, envoyé seulement s'il
  a tourné de plus d'un quart de degré.
- Vidéo non regardée (ni ffmpeg ni OpenCV sur le PC) : cause trouvée dans le code.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.2.jar`). **Statut : non déployé, non testé en jeu.**

## 1.4.3 - consommation et vitesse selon la charge du train (17/10/2026, LeKiwi06)

Demande de LeKiwi06 : « à partir de 8 wagons (5 en plus de la locomotive), chaque nouveau wagon ajouté fait consommer
davantage tous les fours du train, mais je ne sais pas comment formuler mathématiquement cette augmentation pour pas
qu'elle soit inutile ou trop punitive ». Premier choix sur tableau comparatif : « D, avec 10 % » (charge partagée au-delà
de 7 wagons). Puis : « rendre le nombre de fours plus significatif », « si le train est moins chargé [...] les fours
consomment moins », « le train passe systématiquement de ne bouge pas à maximum de vitesse, il faudrait rendre ça plus
progressif » et « plus il y a de wagons, plus il faut de fours ». Choix final sur tableaux comparatifs : « proportionnel
au carré, M3, sous charge à 0,5, et surcharge à 30 % ». La formule D n'a jamais été déployée.

- **Charge** = wagons / traction (traction : 15 pour la locomotive alimentée, +10 par autre four alimenté).
- **Consommation de chaque four alimenté** (en plus du coefficient du niveau) : charge <= 1 : **racine(charge)**, au
  moins **0,5** ; charge > 1 : **racine(charge) x (1 + 30 % x wagons en trop / fours alimentés)**.
- **Vitesse maximale** : 1 bloc/tick x **(traction / wagons)²** quand le train a plus de wagons que sa traction (avant :
  -0,1 bloc/tick par wagon en trop, le train passait de l'arrêt à la pleine vitesse en ajoutant un four).
- Le nombre de fours le plus économe à pleine vitesse est toujours celui juste nécessaire (3 jusqu'à 15 wagons, puis un
  de plus tous les 10 wagons) : plus il y a de wagons, plus il faut de fours (vérifié par simulation de 1 à 64 wagons).
- Exemples (niveau 2, charbon brûlé par tout le train sur 1 000 blocs) : 8 wagons et 3 fours x0,73 = 27 (avant 38) ;
  15 wagons et 3 fours x1,00 = 38 ; 20 wagons et 3 fours x1,73 = 65 à 56 % de la vitesse, avec 4 fours x0,89 = 45 à
  pleine vitesse ; 45 wagons et 6 fours = 75 ; 64 wagons et 8 fours = 99 ; 64 wagons et 5 fours x3,71 = 232 à 30 % de
  la vitesse ; 10 wagons et 6 fours x0,50 = 38.
- Compris dans l'autonomie affichée au conducteur et dans le carburant brûlé du compteur.
- `config.yml` : nouvelles clés `charge.plancher` (0,5) et `charge.taux` (0,30) ; clé `perte-par-wagon` retirée ;
  valeurs par défaut aussi dans le code.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.3.jar`). **Statut : non déployé, non testé en jeu.**

