# Halloween : le ghast du Bingo (demande du 30/10/2026)

Cahier LOCAL (hors dépôt). État : **validé et codé le 30/10/2026** (KG_BingoGame 0.13.0, compilé, non déployé, non testé). Choix ajouté au
code, à confirmer : pas de butin (larme et poudre sont des objectifs de la grille).

## 1. Demande (LeKiwi06, 30/10/2026)

Extrait de son annonce de l'événement d'Halloween (Kanvas, Kal-Games) :

> « Un ghast est présent dans chaque partie ; si vous le tuez en lui renvoyant sa boule, cela valide une case de
> votre grille. »

Plugin désigné : **KG_BingoGame** (`objectives.yml`, validation des objectifs). Points à cadrer qu'il a listés : où et
quand le ghast apparaît, un seul pour tous ou un par joueur, comment on sait qui l'a tué avec une boule renvoyée, case
dédiée ou n'importe quelle case, affichage sur Bedrock.

## 2. Ce que fait le code aujourd'hui (lu le 30/10, KG_BingoGame 0.12.0, serveur-jeux 7015)

- **Un monde par équipe** (`BingoInstance` : un monde = une équipe, même seed pour toutes ; Nether et End propres à
  chaque équipe). Les équipes ne se croisent jamais : « un seul ghast pour tous » est impossible sans changer tout le
  jeu. Un ghast par monde d'équipe est la seule lecture possible de « un ghast dans chaque partie ».
- **Grille commune** à toutes les équipes, progression par équipe (`BingoGame.teamProgress`).
- **Validation = objets seulement** : `Objective(material, quantity, difficulty, condition)` ; `ObjectiveValidationTask`
  regarde toutes les secondes si UN membre de l'équipe a l'objet dans son inventaire. Le champ `condition` n'est
  qu'un texte affiché, jamais vérifié. Aucune case ne se valide aujourd'hui par un événement (mob tué…).
- **Points** (`ScoreEngine`) : valeur de la difficulté + bonus « en 1er » si l'équipe est la première à valider la
  case, bingos (lignes, colonnes, diagonales), coefficients ; en contre la montre, temps gagné selon la difficulté.
- Affichage de la grille : carte (icônes tirées des textures du jeu, `IconResolver`) et menu « Objectifs ».
  Joueurs Bedrock : fenêtres déjà adaptées (couleurs, `BedrockColors`).

**Qui a tué le ghast avec une boule renvoyée** (lu dans le jeu 26.3, `javap`) : quand un joueur frappe une boule de
feu, `Projectile.deflect` la renvoie et fait de ce joueur son **propriétaire** (`setOwner`) ; la boule qui touche le
ghast le blesse avec la source « fireball » (boule = entité directe, propriétaire = cause). Côté plugin :
`EntityDeathEvent` du ghast, `getDamageSource()` : entité directe = boule de feu (`LargeFireball`), cause = un
joueur de l'équipe. Fiable, et le ghast meurt en un coup (le jeu inflige 1 000 dégâts au ghast touché par sa boule
renvoyée par un joueur). Un ghast tué à l'arc ou à l'épée ne compte pas.

## 3. Choix d'interprétation proposés (à valider)

1. **Un ghast par équipe**, dans l'Overworld de son monde, marqué comme « ghast d'Halloween » (donnée cachée sur
   l'entité). Celui qui le tue valide pour son équipe ; tuer le ghast ne change rien pour les autres équipes (elles
   ont chacune le leur).
2. **Apparition** (réponse du 30/10) : au lancement de la partie, à un endroit tiré au hasard dans un rayon de
   **500 blocs** autour du point d'apparition de la partie (en l'air, au-dessus du sol). Il **ne peut pas
   disparaître** (persistant, jamais retiré quand on s'éloigne ; il reste dans son chunk quand personne n'est
   autour). À vérifier au code : la difficulté du monde (en paisible, le jeu retire les ghasts).
3. **Invulnérable à tout le reste** (flèches, épée, lave, chute…) : seule sa boule renvoyée par un joueur le tue. Il
   ne peut donc pas être « gâché ». Un seul par partie : une fois tué, il ne revient pas.
4. **Repérage** : barre de boss « Ghast d'Halloween » visible à ~64 blocs, nom au-dessus de lui ; pas de
   surbrillance (glowing), que les joueurs Bedrock ne voient pas.
5. **Annonce** dans le tchat de la partie, comme une case : « Équipe X tue le ghast d'Halloween (pseudo) : … ».
6. **Période** (réponse du 30/10 : « pendant 15 jours, durée de l'event ») : dates de début et de fin dans
   `config.yml` (du 31/10 au 14/11 inclus, confirmé le 30/10), lues au lancement de chaque partie ; une partie lancée avant la
   fin garde son ghast jusqu'au bout. Clés à ajouter à la main sur le serveur (le fichier n'est copié qu'à la
   première installation).
7. Tous les modes (bingos, blackout, contre la montre).

### Case validée : deux lectures possibles (question 1)

| | A. Case « joker » | B. Case dédiée « Ghast » |
|---|---|---|
| Ce que dit l'annonce | « valide une case de votre grille » : n'importe laquelle | une case de la grille est le ghast |
| Grille | inchangée | une case de chaque grille (au hasard) devient « Tuer le ghast d'Halloween » |
| Le joueur | choisit dans un menu en coffre la case non validée qu'il veut (Échap = plus tard, le joker reste) | rien à choisir |
| Points | ceux de la case choisie (bonus « en 1er » si personne ne l'a encore) | une difficulté à fixer (ex. difficile, 10 pts) |
| `objectives.yml` | rien à ajouter | une entrée d'un nouveau genre (`type: ghast`) hors composition |
| Fin de partie | joker non utilisé = perdu | - |

Ma préférence : **A**, plus proche du texte de l'annonce et plus intéressant (on peut s'en servir pour finir une
ligne). Variante plus simple de A : la case validée est tirée au hasard parmi les cases non validées.

## 4. Questions ouvertes

1. ~~**Case** : joker au choix du joueur (A), joker au hasard (variante), ou case dédiée (B) ?~~ **Réglé le 30/10 : joker au choix (A)**, voir partie 5.
2. ~~**Où et quand**~~ **Réglé le 30/10** : au hasard dans un rayon de 500 blocs, ne peut pas disparaître. Ghasts
   naturels du Nether : **réglé le 30/10, seul le ghast d'Halloween donne le joker**.
3. ~~**Points**~~ **Réglé le 30/10** : points de la case choisie, avec le bonus « en 1er ».
4. ~~**Dates**~~ **Réglé le 30/10** : 15 jours, durée de l'événement. **Confirmé le 30/10 : du 31/10 au 14/11 inclus.**
5. ~~**Bedrock**~~ **Réglé le 30/10** : renvoi testé par LeKiwi06, difficile avec le petit délai mais possible.
6. ~~**Arc**~~ **Réglé le 30/10** (LeKiwi06 : « un projectile renvoie la boule normalement », exact) : relu dans le
   jeu 26.3, `Projectile.onHit` : quand un projectile (flèche, boule de neige, trident…) touche une boule du tag
   `redirectable_projectile` (boule de feu du ghast, charges de vent), il la renvoie (`onRedirectProjectile`,
   `ProjectileDeflection.AIM_DEFLECT`) et le propriétaire du projectile (le tireur) devient celui de la boule. Le
   ghast tué par elle compte donc comme tué par le tireur : rien à ajouter au plugin, la détection prévue couvre le
   coup et l'arc. (Ma première lecture, « la flèche rebondit », était fausse : je n'avais regardé que les dégâts
   reçus par la boule.)

## 5. Réponses de LeKiwi06

- **30/10/2026, question 1** : « Joker au choix ». Tuer le ghast donne un joker : le joueur choisit dans un menu
  en coffre une case pas encore validée de la grille de son équipe, qui est validée pour l'équipe (lecture A de la
  partie 3 ; pas de case dédiée, rien à ajouter dans `objectives.yml`).
- **30/10/2026, questions 2 à 5** : apparition « rng dans un rayon de 500 blocks du spawn, et ne peut pas
  despawn » ; « oui elle donne le bonus de 1er » ; « pendant 15 jours, durée de l'event » ; Bedrock : « avec le
  petit délai c'est difficile de renvoyer la boule, mais pas impossible, et ils peuvent utiliser un arc pour tirer
  sur la boule » ; « c'est déjà le cas, un projectile renvoie la boule normalement » (vérifié, question 6).
- **30/10/2026, ghasts du Nether** : « Oui, lui seul » : seul le ghast d'Halloween donne le joker, pas les ghasts
  naturels du Nether.
- **30/10/2026, dates** : « Oui, ces dates » : du 31/10 au 14/11 inclus.
