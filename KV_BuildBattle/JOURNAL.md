# KV_BuildBattle - journal

Serveur Kanvas. Cahier des charges (commun avec KG_BuildBattle) : `KV_BuildBattle/CAHIER_DES_CHARGES.md`. Dépend de
KLM_Menu (menus, catalogue « Interfaces » ; API inchangée depuis 2.0.0, la version en place sur Kanvas) et de
WorldGuard (FAWE fournit l'API WorldEdit).

## 0.3.5 - les mobs sont retirés d'une partie à l'autre (28/09/2026)

Signalé par LeKiwi06 : « les mobs ne sont pas supprimés d'une partie à une autre ». Cause : en recollant la boîte, les
entités présentes n'étaient retirées que si la boîte capturée contenait elle-même des entités (sinon la fonction
s'arrêtait avant). Désormais, toutes les entités de la boîte (hors joueurs) sont retirées à chaque collage (fin de
partie, génération de l'arène, copies de la salle d'attente), puis celles de la capture sont recréées.
- Les mobs restés dans une boîte depuis une partie jouée en 0.3.4 disparaissent à la fin de la prochaine partie sur
  cette boîte, ou tout de suite avec « Générer l'arène ».

**Compilé, non déployé. Statut : non testé en jeu.**

## 0.3.4 - mobs figés, rien ne dépasse de la zone (27/09/2026)

Retours de LeKiwi06 : « les mobs ont toujours leur IA dans le build battle, on ne peut donc pas poser les entités » ;
« quand j'ai fait pousser un arbre au bord de la zone, il a poussé en partie à l'extérieur ; il faut vérifier que les
blocs qui ont pu dépasser soient bien supprimés également ».
- **Mobs figés** dans le monde `buildbattle` : ceux qui apparaissent par un oeuf, un seau, un golem construit, une
  commande ou la copie de l'arène sont **sans IA** (immobiles), silencieux, invincibles et jamais retirés par le jeu.
  Toutes les autres apparitions (naturelles, générateurs, Wither construit...) sont annulées.
- **Retirer un mob** : pendant la construction, un clic gauche d'un joueur de l'équipe sur un mob de sa zone le retire.
- **Pousse et engrais** : un arbre, un champignon géant ou de l'engrais (herbe, fleurs...) ne posent plus aucun bloc
  hors de la zone constructible d'où ils partent.
- **Fin de partie** : c'est la **boîte entière** qui est remise comme capturée, et plus seulement la zone : tout ce
  qui aurait pu dépasser de la zone est effacé, les entités de la boîte sont recréées.

Choix par défaut (Claude, à ajuster si besoin) : pas de limite du nombre de mobs par zone (KV_Plots prévoit 10 / 25
par plot, pas encore fait) ; les oeufs lancés (poulets) sont permis et figés comme les autres.

**Déployé sur Kanvas le 27/09/2026 (21:32, 0.3.3 dans `_removed-kv_buildbattle-0.3.3/`). Statut : non testé en jeu.**

## 0.3.3 - 200 thèmes (27/09/2026)

Demande de LeKiwi06 : « il me faut 200 thèmes diversifiés, qui ne doivent pas être trop restrictifs dans leur énoncé,
et pas trop sur le même thème (le Nether et l'End ça va dans une liste de 200, mais pas tous les biomes du jeu) ».
- Liste intégrée (`Jeu.THEMES`) portée de 40 à **200 thèmes** courts et ouverts, répartis entre lieux et bâtiments,
  époques, créatures et fantastique, animaux, nourriture, fêtes, métiers, objets, nature et météo, idées abstraites
  (quelques-uns par famille). 5 sont tirés au hasard à chaque partie.
- Deuxième version, même jour (LeKiwi06 : « trop de propositions sur le thème de la ville ; cabane dans les arbres est trop
  précis, cabane suffit ») : 25 bâtiments de ville ou thèmes redondants retirés (gratte-ciel, métro, hôpital, école,
  banque, restaurant...), énoncés raccourcis (Cabane, Forêt, Océan, Île, Voiture, Planète, Égypte...), 25 thèmes
  d'autres familles ajoutés (Automne, Couleurs, Géométrie, Chaos, Mythologie, Steampunk, Miroir...).
- Troisième version (LeKiwi06) : 14 retirés (Royaume, Soleil, Lune, Étoiles, Nuage, Ninja, Détective, Espion, Vampire,
  Nouvel an, Carnaval, Emoji, Paix, Amitié), 14 ajoutés (Nid, Coquillage, Plume, Tortue, Pingouin, Totem, Potion,
  Tapis volant, Boussole, Épouvantail, Squelette, Pique-nique, Bijou, Arène). **Liste validée par LeKiwi06 le 27/09/2026.**
- `config.yml` fourni : `themes: []` (vide = liste intégrée) ; une autre liste peut y être écrite sur le serveur.

**Déployé sur Kanvas le 27/09/2026 (21:08, avec les changements de 0.3.2 ; 0.3.1 dans `_removed-kv_buildbattle-0.3.1/`). Statut : non testé en jeu.**

## 0.3.2 - barre d'action, salle d'attente en mode aventure (27/09/2026)

Retours de LeKiwi06 : « les informations sont marquées en boss bar au lieu de l'action bar comme les autres jeux » ;
« on est en créatif dans le lobby d'attente pour le jeu, on devrait pas (rend-nous invincibles quand même) ».
- Les informations de la partie (salle d'attente, thème, temps restant, terrain noté, résultats) sont dans la **barre
  d'action**, renvoyée chaque seconde ; plus de barre du boss.
- **Salle d'attente et choix du thème** (qui s'y déroule) : **mode aventure**, sans vol. Les règles du monde laissent
  ce mode aux joueurs d'une partie à ces phases (sinon créatif forcé comme avant). Créatif à partir de la construction.
- Joueurs d'une partie **invincibles** et **sans faim**, à toutes les phases ; une chute dans le vide ramène au point
  d'apparition de la salle (ou de la boîte où le joueur doit être).

**Pas déployée seule : comprise dans 0.3.3.**

## 0.3.1 - la glace ne fond plus (27/09/2026)

Signalé par LeKiwi06 : « j'avais posé des blocs de glace dans l'arène de build battle, elle a fondu car le random tick
speed n'est pas à 0 ». Au démarrage, la règle `randomTickSpeed` du monde `buildbattle` est mise à **0** : glace, neige,
feuilles, cultures, herbe ne changent plus d'elles-mêmes (arène et constructions des joueurs).
- Dans les 5 s qui suivent l'arrivée en salle d'attente, une téléportation hors du monde `buildbattle` est annulée :
  un point de chute de KLM_Portal (installé sur Kanvas le même jour, « default-arrival » en coordonnées) sortirait sinon
  le joueur de sa partie. Par défaut (« dernière position »), KLM_Portal ne téléporte personne à l'arrivée.
- Limite : la glace déjà fondue dans les boîtes générées ne revient pas : la reposer dans la boîte d'origine, la
  recapturer puis regénérer l'arène. Le monde `Kanvas` est réglé par KV_Plots 1.4.1.

**Déployé sur Kanvas le 27/09/2026 (19:59, 0.3.0 dans `_removed-kv_buildbattle-0.3.0/`). Statut : non testé en jeu.**

## 0.3.0 - étape 2 : la partie (27/09/2026)

Cahier des charges (décisions de LeKiwi06 du 27/09/2026). Classes : `Jeu` (remplace `Accueil` : arrivée, parties par
colonne, horloge, objets), `Partie` (phases), `Objets`, `Inventaires`.
- **Salle d'attente** : l'inventaire Kanvas du joueur est **mis de côté** (fichier `inventaires/<uuid>.yml`) et rendu
  quand il quitte la partie (fin, déconnexion, changement de monde ; à la connexion suivante si le serveur s'est
  arrêté). Barre en haut de l'écran : mode, joueurs, départ.
  - **Public** : compte à rebours de **30 s** dès qu'il y a de quoi faire 2 équipes (plus de joueurs que la taille
    d'une équipe), annulé si l'on repasse en dessous ; **départ immédiat** quand les 8 équipes sont pleines.
  - **Privé** : l'hôte lance avec l'**émeraude** (au moins 2 équipes). S'il part, le joueur suivant devient l'hôte. Au
    lancement, `buildbattle-fermee-<id>` est déposé sur le relais : KG_BuildBattle retire la partie de sa liste.
- **Équipes** tirées au hasard, remplies dans l'ordre (ex. 3 joueurs en duo : une équipe de 2 et une de 1), 8 au plus.
- **Thème** :
  - normal : **5 thèmes** tirés au hasard dans `themes` (liste intégrée de 40 thèmes si absente), vote dans un menu
    (papier pour le rouvrir), **30 s**, puis 3 s quand tout le monde a voté ; égalité = tirage au sort ;
  - « thèmes écrits » (partie privée) : chacun écrit un thème (64 caractères, livre pour rouvrir), **1 min**, puis 3 s
    quand tout le monde a proposé ; puis vote de **30 s** parmi les propositions (doublons fusionnés). Aucune
    proposition : 5 thèmes au hasard ; une seule : elle est prise directement. Pendant ce vote, la **poudre de blaze**
    ouvre « Signaler un thème » : message au staff connecté (`kvbuildbattle.admin`) et dans la console, enregistré dans
    `signalements-themes.yml` (date, thème, auteur, signalé par, partie) ; le thème reste dans le vote.
- **Construction** : chaque équipe est téléportée dans sa boîte (rang 0, 1... de la colonne), devient membre de la
  région de sa zone (FAWE compris, dans la zone seulement), inventaire vide, créatif. Titre « Thème : … », barre avec
  le temps restant (3, 5, 10 ou 30 min selon le tempo). **On ne sort pas de sa boîte** (retour au point d'apparition).
- **Fin de la construction** : toutes les zones sont **figées** (plus de membres).
- **Votes** : tout le monde est téléporté sur chaque terrain, l'un après l'autre, **30 s** par terrain, puis 3 s quand
  tous ceux qui peuvent voter ont voté ; 5 terracottas (cases 3 à 7), note modifiable tant qu'on est sur le terrain ;
  l'équipe du terrain ne vote pas. On reste dans la boîte montrée. Hors construction, l'inventaire est figé.
- **Résultats** : classement au total des points puis à la moyenne (chat), titre du gagnant, tout le monde dans la boîte
  gagnante pendant **15 s**, puis **retour sur kal-games** ; les zones utilisées sont **remises comme dans la boîte
  capturée** (vides), puis la colonne est libérée.
- Une partie dont tous les joueurs sont partis s'arrête (zones vidées, colonne libérée).
- Durées réglables : section `durees` (`depart`, `ecriture-theme`, `vote-theme`, `vote-terrain`, `resultats`), valeurs
  par défaut dans le code. `/bbadmin info` affiche la phase de chaque partie.

Choix par défaut (Claude, à ajuster si besoin) : équipes tirées au hasard ; la note d'un terrain peut être changée
pendant ses 30 s ; un joueur déconnecté ne revient pas dans la partie (son équipe continue, un terrain dont toute
l'équipe est partie reste présenté au vote).

Limites connues : arrêt du serveur en pleine partie : les joueurs récupèrent leur inventaire, mais les zones ne sont pas
vidées ni figées (les vider en regénérant l'arène) ; points et classements : étape 4.

**Déployé sur Kanvas le 27/09/2026 (14:12, 0.2.0 dans `_removed-kv_buildbattle-0.2.0/`). Statut : non testé en jeu.**

## 0.2.0 - accueil des joueurs envoyés par KG_BuildBattle (27/09/2026)

Demande de LeKiwi06 : la partie kal-games « comme pour le Bingo » (KG_BuildBattle 0.1.0). Empilée sur 0.1.0 pas
encore testée, à sa demande.
- À la connexion, l'affectation est lue sur le relais (`buildbattle-<uuid>`, 3 essais à 1 s d'intervalle). Sans
  affectation (joueur venu pour les plots), rien ne change.
- Le joueur rejoint la **salle d'attente** de sa partie, une par colonne de l'arène : partie privée = même identifiant ;
  file publique = partie publique de la même taille d'équipe qui a encore de la place ; sinon une nouvelle partie sur
  une colonne libre. Plus de colonne libre : message et renvoi sur kal-games (`serveur-retour`).
- Salle pas encore capturée : le joueur arrive au point d'apparition du monde `buildbattle` (message).
- Un joueur qui se déconnecte ou quitte le monde `buildbattle` quitte sa salle ; une salle vide libère sa colonne.
- `/bbadmin info` liste les salles d'attente occupées.
- Nouvelles clés (valeurs par défaut dans le code) : `relay-url`, `relay-token` (vide : **à remplir à la main** sur le
  serveur, même valeur que `token` de `relay.properties` du proxy), `serveur-retour` (`kal-games`).
- Pas encore (étape 2) : compte à rebours, lancement par l'hôte, thème, construction, votes ; le joueur garde son
  inventaire et les objets de Kanvas (étoile de KV_Menu, boussole).

**Déployé sur Kanvas le 27/09/2026 (13:55) avec KG_BuildBattle 0.1.0 (0.1.0 dans `_removed-kv_buildbattle-0.1.0/`). Statut : testé et confirmé par LeKiwi06 le 27/09/2026 (2 joueurs en file publique solo, retrouvés ensemble dans la salle d'attente).**

## 0.1.0 - étape 1 : arène (27/09/2026)

Demande de LeKiwi06 : « j'ai déjà construit l'arène du build battle, ce sera dans un monde à part [...] il faudra me
faire capturer l'arène dans sa globalité, puis la zone buildable » ; « toutes les boîtes sont identiques, tu peux faire
4 colonnes de 8 boîtes espacées dans le monde pour pas qu'on voie les nametags des autres parties » ; salle d'attente
sur Kanvas, « il me faudra l'option pour la save comme pour le bingo ».
- **Monde à part** `buildbattle` (réglage `monde`), vide, créé au premier démarrage ; point d'apparition (0, 80, 0),
  pour construire la salle d'attente.
- **Capture de la boîte** : l'admin pose, à sa position, les 2 coins de la boîte entière, les 2 coins de la zone
  constructible (entièrement dans la boîte) et le point d'apparition de l'équipe (position + orientation), dans
  n'importe quel monde (la boîte actuelle est dans le monde `Kanvas`), puis « Capturer la boîte ». Blocs, entités
  (porte-armures, cadres...) et zone enregistrés dans `boite.kvbb` et `arene.yml`.
- **Générer l'arène** (confirmation) : la boîte est recopiée en **4 colonnes de 8** (`disposition`), une colonne par
  partie. Colonnes espacées de 600 blocs de bord à bord (`pas-colonnes`), boîtes d'une colonne séparées de 8 blocs
  (`ecart-boites`), à partir de (10000, 64, 0). Regénérer remplace les boîtes (constructions comprises).
- **Protection** : une région WorldGuard par zone constructible (`kv_bb_<colonne>_<boîte>`, priorité 10), **sans
  membre** pour l'instant (les équipes y seront ajoutées pendant les parties, étape 2) ; le reste du monde en
  `passthrough deny`. Hors opérateurs, personne n'y construit, FAWE compris.
- **Salle d'attente** : 2 coins + point d'apparition, puis « Capturer la salle d'attente » : enregistrée
  (`salle.kvbb`) et **recopiée au nord de chaque colonne** (une copie par partie, à 32 blocs de la 1re boîte,
  `ecart-salle`). Une nouvelle capture remplace les anciennes copies.
- **Règles du monde** (reprises de KV_Plots) : créatif + vol, pas de TNT ni d'explosion, rien ne brûle, les liquides ne
  sortent pas de leur zone constructible, redstone coupée.
- **Interface** : catalogue « Interfaces » de la boussole (rubrique admin) > « Build Battle : arène », ou `/bbadmin`.
  Commandes texte : `/bbadmin monde | boite <pos1|pos2|zone1|zone2|apparition|capturer> | salle
  <pos1|pos2|apparition|capturer> | generer [confirmer] | info`. Positions gardées dans `positions.yml` (survivent au
  redémarrage).
- Captures et collages faits petit à petit (`blocs-par-tick`, 10 000 ; `taille-max`, 256 blocs par côté), comme la
  salle d'attente du Bingo : le serveur n'est pas bloqué.

Choix par défaut (Claude, à ajuster si besoin) : le point d'apparition de l'équipe sert aussi de point de vue pendant
le vote de son terrain (étape 2) ; les barrières de la boîte sont recopiées (murs invisibles possibles).

Limites connues :
- Changer `disposition` après la génération : les boîtes déjà collées ne sont plus retrouvées (les effacer d'abord
  à la main, ou regénérer avec les anciens réglages).
- Une nouvelle capture de la boîte ne touche pas l'arène déjà générée : il faut « Générer l'arène » à nouveau.
- La boîte d'origine (monde `Kanvas`) n'est pas touchée.

**Déployé sur Kanvas le 27/09/2026 (10:40), remplacé par 0.2.0 à 13:55. Statut : non testé en jeu.**
