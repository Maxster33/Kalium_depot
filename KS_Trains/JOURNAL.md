

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
- **Vitesse minimale** (LeKiwi06 : « il faut qu'il refuse d'avancer s'il ne peut pas aller à au moins 3 blocs par
  seconde ») : en dessous, départ refusé (« Pas assez de carburant ou de fours pour ce train »), ou freinage jusqu'à
  l'arrêt s'il roulait (un four qui se vide). Soit au plus 2,58 wagons par point de traction : locomotive seule
  jusqu'à 38 wagons, 4 fours jusqu'à 64.
- **Niveau payé** (proposé par Claude, accord de LeKiwi06) : un train plafonné paie le coefficient du niveau qu'il peut
  vraiment atteindre, plus celui choisi (plafonné à 6 blocs/s au niveau 4 : payé au niveau 2, x1 au lieu de x1,5).
- La réaction en chaîne quand un four se vide avant les autres (traction en baisse, les autres consomment plus) est
  gardée (LeKiwi06 : « ça force à installer des minecart hopper »).
- Compris dans l'autonomie affichée au conducteur et dans le carburant brûlé du compteur.
- **Portée des sons** (LeKiwi06 : « le son ne s'entend pas d'assez loin ») : sifflet 128 blocs (avant 40), souffle de
  vapeur 64 blocs (avant 16, choix de Claude). Le jeu fait porter un son à 16 blocs x son volume ; au-delà de 1 le
  volume ne monte plus, mais le souffle était à 0,5 : il est maintenant deux fois plus fort de près.
- **Messages lisibles** (LeKiwi06 : « quand un texte vient remplacer celui qui indique la vitesse de la locomotive, il
  faut le laisser afficher 3 secondes avant de remettre l'ancien ») : après chaque message de KS_Trains au-dessus de la
  barre d'objets (refus, annonces, klaxon, boussole...), le tableau du conducteur attend 3 secondes avant de revenir
  (avant : remplacé dans la demi-seconde). Les messages d'autres plugins (KS_Claim...) ne sont pas concernés.
- **Klaxon par défaut** (LeKiwi06 : « un klaxon par défaut en note block qui ressemble à un sifflet de train ») : sans
  corne de chèvre choisie, la touche de saut (ou le bouton Klaxon du menu) joue un sifflet : accord de 3 notes de
  flûte graves (fa dièse mineur, choix de Claude), rejoué 6 fois en 1,2 seconde ; entendu à 256 blocs comme une corne.
  Remplace le message « Aucune corne choisie ». Bouton Klaxon : nouvelle ligne « Corne choisie, sinon sifflet du
  train » (clé `menu.klaxon-sifflet`). Recette du contrôleur : bloc musical au lieu de l'ampoule de cuivre
  (KS_Crafts 1.19.0, à déployer avec).
- `config.yml` : nouvelles clés `charge.plancher` (0,5), `charge.taux` (0,30), `vitesse-min` (0,15),
  `son.portee-sifflet` (128), `son.portee-souffle` (64) et `son.portee-klaxon` (256) ; clé `perte-par-wagon` retirée ;
  valeurs par défaut aussi dans le code.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.3.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 17/10/2026 à 10:20 (LeKiwi06 : « oui envoie sur event ») : envoi groupé KS_Trains 1.4.3 (+ `config.yml`) et KS_Crafts 1.19.0 ; jar en place (1.4.1) vérifié identique à la référence avant l'envoi, rangé dans `_removed-ks_trains-1.4.1/` ; jar du serveur vérifié identique à celui compilé. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 17/10/2026 à 23:42 (journal lu, heures en UTC : 21:42) : KS_Trains 1.4.3 activé sans erreur ni avertissement (après KS_Claim et KLM_Contacts), démarrage en 13 s. Statut : non testé en jeu.**

## 1.4.4 - console qui spamme, modèles de four, train arrêté en pente après une marche arrière (17/10/2026, LeKiwi06)

Signalements de LeKiwi06 sur Event (1.4.3) : « pourquoi la console de Event spamme autant ? », « les modèles de four sont
très buggués aussi » (capture : blocs à leur taille et orientation par défaut), « l'arrêt en pente en marche arrière
produit le même souci qu'il y avait avant en marche avant » (en reculant en montée : wagons entassés, blocs qui bougent),
« quand j'ai lancé le train après ce bug, il s'est retourné et a démarré dans l'autre sens en se dépliant du tassage ».

- **Cause de la console** (défaut de la 1.4.2) : `Quaternionf.dot(Quaternionf)` existe dans JOML 1.10.8 (bibliothèque
  de compilation) mais plus dans JOML 1.10.9 (celle du serveur Paper 26.3) : `NoSuchMethodError` à chaque tick
  (4 155 erreurs en 12 minutes, journal de 5,8 Mo). Les modèles agrandis n'étaient donc jamais orientés ni réduits.
  L'erreur coupait aussi la suite des tâches : chaque seconde, fours allumés, boussole et chunks gardés chargés après le
  premier wagon ; à chaque tick d'un train qui roule, collisions et wagons entonnoirs, et tous les trains suivants
  (plus déplacés ni retenus en pente). **Correction** : produit scalaire calculé dans le plugin. Tous les autres appels
  à JOML vérifiés présents dans la 1.10.9 du serveur.
- **Train arrêté** : chaque wagon est maintenant retenu à la place qu'il avait au moment de l'arrêt (remis en place dès
  qu'il s'en écarte de plus de 0,05 bloc), sans dépendre du trajet. En marche arrière, juste après le changement de
  sens, le trajet est refait sur la position des wagons et peut être trop court : des wagons n'étaient plus retenus.
  Cause exacte de l'entassement non prouvée (l'erreur ci-dessus a pu y contribuer) ; le retournement au redémarrage en
  découlait (trajet refait sur des wagons entassés).

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.4.jar`), `config.yml` inchangé. **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 00:18 (LeKiwi06 : « oui envoie sur event ») : jar en place (1.4.3) vérifié identique à la référence avant l'envoi, rangé dans `_removed-ks_trains-1.4.3/` ; jar du serveur vérifié identique à celui compilé ; `config.yml` inchangé. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

## 1.4.5 - sons du train : volume d'avant, portée lointaine (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « le bruit de train est trop fort maintenant, il faudrait le remettre au volume où il était
précédemment, mais le rendre audible de très loin ».

- **Cause** : le jeu fait porter un son à 16 blocs x son volume et ne joue pas plus fort au-delà de 1 ; pour porter à
  64 blocs, le souffle de vapeur de la 1.4.3 était donc joué au volume maximal (avant : 0,5).
- **Correction** : son joué pour chaque joueur à portée : à 4 blocs au plus de lui, dans la direction du train (on
  entend toujours d'où il vient), avec le volume qui donne à son oreille : volume de près x (1 - distance / portée).
  De près : souffle 0,5 et sifflet 1 comme avant la 1.4.3 ; jusqu'à 0 à la portée (souffle 64 blocs, sifflet 128).
- `config.yml` : nouvelles clés `son.volume-sifflet` (1,0) et `son.volume-souffle` (0,5).
- Klaxon (cornes et sifflet par défaut) inchangé.

Compilé le 18/10/2026 (`sortie/KS_Trains-1.4.5.jar`). **Statut : non déployé, non testé en jeu.**

## 1.4.6 - klaxon entendu de loin, sifflet en deux coups (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « on n'entend pas le klaxon d'assez loin, le but c'est de prévenir d'un danger comme le train
propulse en l'air, faut qu'on puisse l'entendre ; et ton klaxon par défaut est bien, mais il manque un temps de pause
pour vraiment avoir le tchou-tchou et pas tchouuuuu ».

- **Cause de la portée** : le klaxon était un son attaché au wagon contrôleur ; le jeu d'un joueur l'ignore quand ce
  wagon est trop loin pour lui être affiché, quelle que soit la portée du son.
- **Correction** : joué pour chaque joueur comme les sons de la 1.4.5 (depuis quelques blocs de lui, dans la direction
  du train) : **plein volume jusqu'à 128 blocs** (`son.plein-klaxon`), puis baissant jusqu'à 0 à 256 blocs
  (`son.portee-klaxon`). Cornes de chèvre et sifflet par défaut. Catégorie de son « créatures amicales » comme les
  autres sons du train (avant : « jukebox / blocs musicaux » ; choix de Claude : un avertissement ne doit pas
  disparaître avec la musique coupée).
- **Sifflet par défaut** : deux coups courts (accord joué 3 fois en 0,3 seconde chacun) séparés d'une pause, 0,45
  seconde entre leurs débuts (avant : un seul coup tenu de 1,2 seconde).
- `config.yml` : nouvelle clé `son.plein-klaxon` (128).

Compilé le 18/10/2026 (`sortie/KS_Trains-1.4.6.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 07:31 (LeKiwi06 : « oui envoie sur event ») : 1.4.6 (avec 1.4.4 et 1.4.5) à la place de la 1.4.4 ; jar en place vérifié identique à la référence et `config.yml` du serveur identique à celui de la 1.4.3, rangés dans `_removed-ks_trains-1.4.4/` ; jar et `config.yml` du serveur vérifiés identiques à ceux du dépôt. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

## 1.4.7 - modèles agrandis retirés, sauf celui du wagon jukebox (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « retire tous les modèles, sauf celui du jukebox, ils sont un peu buggués et n'apportent rien
d'essentiel ».

- Plus de modèle agrandi sur les fours, coffres et entonnoirs : on voit les wagonnets du jeu. Ceux déjà posés sont
  retirés à la seconde suivante (ils ne sont de toute façon pas enregistrés avec le monde).
- Le wagon jukebox garde son modèle (sans lui, c'est un wagonnet simple) ; `decor.actif`, `decor.taille` et
  `decor.hauteur` ne concernent plus que lui.
- Fumée du four du milieu de la locomotive inchangée.
- `config.yml` inchangé.

Compilé le 18/10/2026 (`sortie/KS_Trains-1.4.7.jar`). **Statut : non déployé, non testé en jeu.**
