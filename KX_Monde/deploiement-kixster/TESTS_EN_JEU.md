# Kixster : tests en jeu après l'installation des plugins d'Event

Installation faite le 08/10/2026 à 15:06 ; démarrage de 15:20 : 43 plugins activés, aucune erreur. Cette liste vérifie
en jeu que **tout** fonctionne, dans l'ordre (chaque partie a besoin des précédentes).

## Comment lire cette liste

Chaque test a :
- **Faire** : les gestes, un par un ;
- **Attendu** : ce qu'on doit voir exactement (textes repris des plugins) ;
- **Si ça ne va pas** : quoi noter / à qui le dire.

Repères :
- 🖥️ **console** : à taper dans la console de Kixster sur Minestrator, **sans** `/` au début.
- 👑 **op** : en jeu, avec un compte opérateur (`op <pseudo>` dans la console pour le devenir).
- 🙂 **joueur** : en jeu, avec un compte **non opérateur**. Important : un opérateur passe à travers presque toutes
  les interdictions, il ne verrait pas les refus. Si vous n'avez qu'un compte : `deop <pseudo>` pour les tests
  🙂, puis `op <pseudo>` pour revenir.
- 👥 **2 joueurs** : il faut être deux en même temps.

Quand un test échoue : noter l'heure, ce qu'on a fait, ce qu'on a vu (capture d'écran si possible), et prévenir
Claude, qui lit le journal du serveur (`logs/latest.log`) à cette heure-là.

Matériel utile (👑, en créatif `gamemode creative` / retour `gamemode survival`) : la plupart des objets vanilla
se donnent par `/give <pseudo> <objet> <nombre>` ; les objets **custom** par `/kaliumgive <pseudo> <id> <nombre>`
(la touche Tab propose les id). **Les tests de butin et de limites se font en survie** (en créatif, plusieurs plugins
ne comptent rien ou ne lâchent rien, comme en vanilla).

---

## Partie 1 : préparer le serveur (≈ 5 min)

### 1.1 Fermer le serveur pendant les tests
- **Faire** (🖥️) :
  ```
  whitelist on
  whitelist add Maxster33
  whitelist add LeKiwi06
  ```
  (+ `whitelist add <pseudo>` pour chaque compte de test ; les joueurs Bedrock ont un pseudo qui commence par un
  point, ex. `.Pseudo`, à vérifier une fois connectés avec `list`.)
- **Attendu** : « Turned on the whitelist », puis « Added … to the whitelist ».

### 1.2 Permissions (les mêmes qu'Event)
Copiées de la base LuckPerms d'Event le 08/10/2026 : un groupe `admin` avec `kseconomy.staff` (gestion du staff
dans l'économie), et les 4 commandes de SimpleClaimSystem interdites aux joueurs (ils passent par `/ksclaim`).
- **Faire** (🖥️), une ligne à la fois :
  ```
  lp creategroup admin
  lp group admin permission set kseconomy.staff true
  lp group default permission set scs.command.claim false
  lp group default permission set scs.command.claim.* false
  lp group default permission set scs.command.claims false
  lp group default permission set scs.command.unclaim false
  lp user LeKiwi06 parent add admin
  ```
  Sur Event, seul LeKiwi06 est dans `admin`. Pour y être aussi : `lp user Maxster33 parent add admin`.
- **Vérifier** (🖥️) :
  ```
  lp group default permission info
  lp group admin permission info
  ```
- **Attendu** : `default` montre les 4 lignes `scs.command…` à **false** ; `admin` montre `kseconomy.staff` à
  **true**.
- **Si ça ne va pas** : `lp user LeKiwi06 parent add admin` dit « joueur introuvable » si LeKiwi06 n'est jamais venu
  sur Kixster : le refaire après sa première connexion.

---

## Partie 2 : connexion (≈ 5 min)

### 2.1 Java
- **Faire** : se connecter au réseau comme d'habitude, aller sur Kixster.
- **Attendu** : on arrive sur Kixster sans être expulsé ; **environ 2 s après**, message dans le tchat :
  « Mini-cartes : vue des grottes et radar d'entités désactivés (fair-play). »
- **Si ça ne va pas** : message d'expulsion → le recopier exactement.

### 2.2 Bedrock
- **Faire** : se connecter avec un compte Bedrock (téléphone, console ou Windows).
- **Attendu** : on arrive sur Kixster (Floodgate) ; même message de fair-play.
- **Si ça ne va pas** : « Unable to connect » ou expulsion → recopier le message (souvent la clé Floodgate).

### 2.3 Tableau sur le côté
- **Faire** : `/tableau on`.
- **Attendu** : tableau à droite, titre **« Kixster »**, avec : score (points), « Exploration journalière : 0 / 10 »,
  nombre de claims, coordonnées X Y Z, biome. Les coordonnées bougent quand on marche (mis à jour chaque seconde).
  Il est **masqué par défaut** : c'est normal qu'il n'apparaisse pas avant `/tableau on`.
- **Faire** : `/tableau off`, se déconnecter / reconnecter.
- **Attendu** : il reste masqué (le choix est gardé). Le remettre avec `/tableau on` pour la suite.

### 2.4 Tchat
- **Faire** : écrire un message.
- **Attendu** : il s'affiche normalement pour les autres joueurs (KLM_Chat). Chat vocal : si le mod Simple Voice
  Chat est installé côté client, on s'entend.

---

## Partie 3 : menus (≈ 5 min)

### 3.1 Objets de menu
- **Faire** : `/menu on`.
- **Attendu** : les objets de menu arrivent dans la barre, dont une **étoile du Nether en case 5**. Sur Kixster,
  ils ne sont **pas** donnés à la connexion (réglage de LeKiwi06) : il faut `/menu on`.
- **Faire** : essayer de jeter l'étoile (touche Q) ou de la déplacer.
- **Attendu** : impossible ; elle revient en case 5.
- **Faire** : `/menu off`.
- **Attendu** : les objets de menu disparaissent. Remettre `/menu on`.

### 3.2 Menu Kixster
- **Faire** : clic droit avec l'étoile du Nether ; puis fermer et taper `/kixster`.
- **Attendu** : les deux ouvrent le même menu, titre **« Kixster »**, avec un bouton par plugin : Économie, Claims,
  Jetons, Récompenses…
- **Faire** : `/event`.
- **Attendu** : **rien** ne s'ouvre (commande inconnue). C'est voulu : sur Kixster la commande est `/kixster`.
- **Faire** : cliquer chaque bouton du menu un par un.
- **Attendu** : chaque bouton ouvre son écran (on les teste en détail plus loin) ; aucun texte ne déborde ni ne
  défile.

### 3.3 Sur Bedrock
- **Faire** : mêmes gestes avec le compte Bedrock.
- **Attendu** : les menus s'affichent sous forme de formulaires Bedrock, avec les mêmes boutons.

---

## Partie 4 : dimensions (≈ 5 min)

### 4.1 Menu des portails
- **Faire** (👑) : `/dimensions`.
- **Attendu** : menu avec « Portail du Nether : activé » et « Portail de l'End : activé ».
- **Faire** : cliquer « Portail du Nether » ; essayer d'entrer dans un portail du Nether depuis le monde normal.
- **Attendu** : il passe à « désactivé » ; le portail ne nous emmène pas, message dans la barre d'action. Dans la
  console : « … désactivés par <pseudo> ».
- **Faire** : **le remettre sur « activé »** (sinon les joueurs ne pourront pas aller dans le Nether).

### 4.2 Voyager
- **Faire** : construire et allumer un portail du Nether, le prendre, revenir.
- **Attendu** : aller et retour OK.
- **Faire** (👑) : aller dans l'End (portail d'un fort, ou `execute in minecraft:the_end run tp @s 0 80 0`).
- **Attendu** : on arrive dans l'End. Rester dans l'End pour la partie 5.2.

---

## Partie 5 : régions WorldGuard (≈ 10 min) — avant les claims et les magasins

Deux régions à créer :
- `zone_shop` : la zone des magasins (monde normal). Tout le monde peut y construire et y claimer ; **sans elle,
  aucun magasin ne peut être créé**.
- `ile_du_dragon` : l'île principale de l'End, où **on ne peut pas claimer**.

### 5.1 Zone des shops (monde normal)
- **Faire** (👑), à l'endroit choisi pour les magasins :
  1. `//wand` : on reçoit une hache en bois.
  2. Clic **gauche** sur un bloc d'un coin de la zone, clic **droit** sur le coin opposé (en diagonale). Message
     « First position set » / « Second position set ».
  3. `//expand vert` : la sélection prend toute la hauteur du monde.
  4. Puis :
     ```
     /rg define zone_shop
     /rg flag zone_shop build allow
     /rg flag zone_shop scs-claim allow
     ```
- **Vérifier** : `/rg info zone_shop`.
- **Attendu** : la région existe, avec les flags `build: ALLOW` et `scs-claim: ALLOW`.
- **Si ça ne va pas** : « unknown flag scs-claim » → le noter (le flag vient de SimpleClaimSystem) et me prévenir.

### 5.2 Île du dragon (dans l'End)
- **Faire** (👑), **en étant dans l'End** :
  ```
  //pos1 -250,0,-250
  //pos2 250,255,250
  /rg define ile_du_dragon
  /rg flag ile_du_dragon scs-claim deny
  ```
  (Carré de 500 blocs centré sur l'île, toute la hauteur.)
- **Vérifier** : `/rg info ile_du_dragon` (toujours dans l'End).
- **Attendu** : la région existe avec `scs-claim: DENY`.
- **Revenir** au monde normal (`/kill`, ou portail de sortie si le dragon est mort, ou
  `execute in minecraft:overworld run tp @s 0 100 0` en op).

---

## Partie 6 : claims (≈ 15 min) — 🙂 compte non op

Règles : 1 claim = 1 chunk (16 × 16, toute la hauteur) ; les 10 premiers sont **gratuits**, ensuite payants avec les
points (11e : 237 points, 12e : 264…).

### 6.1 Créer un claim
- **Faire** (🙂) : dans un chunk libre, `/ksclaim`.
- **Attendu** : le prix s'affiche (gratuit), puis une confirmation ; après confirmation, le claim est créé.
  Le tableau affiche 1 claim (mis à jour en 30 s au plus).
- **Si ça ne va pas** : rien ne s'ouvre et aucun message → me le dire (c'est déjà arrivé une fois sur Event).

### 6.2 Menu des claims
- **Faire** (🙂) : `/ksclaims` (ou bouton « Claims » du menu Kixster).
- **Attendu** : la liste de ses claims ; dans un claim : nom (16 caractères), description (50), membres (5 au plus),
  réglages des membres et des visiteurs, groupe, vendre, « Expulser un joueur ». Changer le nom → il est gardé.

### 6.3 Commandes interdites
- **Faire** (🙂) : `/claim`, puis `/claims`, puis `/unclaim`.
- **Attendu** : les trois sont **refusées** (pas de permission). Si l'une marche, les commandes LuckPerms de la
  partie 1.2 n'ont pas pris : me le dire.

### 6.4 Protection et membres (👥)
- **Faire** : le 2e joueur (non membre) essaie de casser un bloc, poser un bloc, ouvrir un coffre dans le claim.
- **Attendu** : tout est refusé, avec un message de protection en français.
- **Faire** : le propriétaire ajoute le 2e joueur comme membre (`/ksclaims` → le claim → Membres → Ajouter) ;
  le 2e réessaie.
- **Attendu** : il peut maintenant (selon les réglages des membres). Retirer le membre : de nouveau refusé.

### 6.5 Nether et End : noms des mondes
- **Faire** (🙂) : un claim dans le **Nether**, puis un dans l'**End** hors de l'île (à plus de 250 blocs du
  centre).
- **Attendu** : les deux sont créés. Regarder le nom du monde affiché (barre en haut de l'écran en entrant dans le
  claim, et menu du claim).
- **Si ça ne va pas** : un nom brut ou anglais (ex. « Kixster SMP » pour le Nether, ou `minecraft:the_nether`) →
  me donner le texte exact : je corrige le réglage `world-aliases` de SimpleClaimSystem.

### 6.6 Île du dragon et zone des shops
- **Faire** (🙂) : `/ksclaim` sur l'île principale de l'End.
- **Attendu** : **refusé** (zone protégée).
- **Faire** (🙂) : `/ksclaim` dans la zone des shops.
- **Attendu** : **accepté**. Garder ce claim pour la partie 7.

### 6.7 Supprimer un claim
- **Faire** (🙂) : supprimer un des claims de test (menu du claim → supprimer → confirmer).
- **Attendu** : claim supprimé ; rien de remboursé (il était gratuit).

---

## Partie 7 : économie et magasins (≈ 20 min)

Règles : 1 émeraude = 1 point ; 1 bloc d'émeraude = 9 points. Le solde se voit dans la liste des joueurs (touche Tab),
en vert à côté du pseudo.

⚠️ Les points créés pendant les tests sont de **vrais points** (il n'y a pas de commande pour les retirer). Les
faire avec un compte du staff, et les **retirer en émeraudes puis les détruire** à la fin (7.7).

### 7.1 Déposer et retirer
- **Faire** (🙂) : prendre 64 émeraudes (👑 `/give <pseudo> emerald 64`) ; `/economie` → « Tout déposer ».
- **Attendu** : les émeraudes disparaissent ; solde **64** dans le menu, le tableau et la liste Tab.
- **Faire** : « Retirer » → Émeraude → 10.
- **Attendu** : 10 émeraudes dans l'inventaire ; solde 54.
- **Faire** : « Déposer des objets » → mettre 5 émeraudes **et** de la terre dans le coffre, fermer.
- **Attendu** : solde 59 ; la terre est rendue.

### 7.2 Masquer le solde
- **Faire** : « Masquer mon solde ».
- **Attendu** : dans la liste Tab, seul le pseudo s'affiche (plus le nombre). Le menu prévient que 1 % est prélevé
  chaque jour tant qu'il est masqué. « Révéler » : il réapparaît tout de suite.

### 7.3 Échange entre joueurs (👥)
- **Faire** : les deux joueurs à **moins de 5 blocs** ; le 1er tape `/echange <pseudo du 2e>` ; le 2e clique
  « accepter » dans le tchat (dans les 60 s).
- **Attendu** : un panneau s'ouvre chez les deux : à gauche sa part, à droite celle de l'autre.
- **Faire** : le 1er met un objet (bouton « Objets »), le 2e met 10 points (bouton « Montant ») ; les deux cliquent
  « Valider ».
- **Attendu** : l'objet passe chez le 2e, les 10 points chez le 1er ; soldes mis à jour.
- **Faire** : refaire un échange et s'éloigner à plus de 5 blocs avant de valider.
- **Attendu** : échange annulé, objets rendus.

### 7.4 Rachats de la semaine
- **Faire** (🙂) : `/rachat`.
- **Attendu** : la liste des 10 objets que le serveur rachète cette semaine (tirés chaque lundi), avec leur prix.
  Vendre un objet de la liste si on en a : les points arrivent sur le solde.

### 7.5 Créer un magasin
Coût : **45 000 points** (ou 5 blocs d'émeraude compressés tier 3). Pour avoir les points (👑) :
`/give <pseudo> emerald_block 2304` (2 304 blocs = 20 736 points), `/economie` → « Tout déposer », **3 fois**.
- **Faire** (avec ce compte) : d'abord dans un de ses claims **hors** de la zone des shops : `/magasin create`.
- **Attendu** : refusé ; aucun point retiré.
- **Faire** : **dans son claim de la zone des shops** (6.6) : `/magasin create`.
- **Attendu** : magasin créé, 45 000 points retirés, 10 boutiques possibles.
- **Faire** : `/magasin` : menu (mes boutiques, nom, description, position).

### 7.6 Créer une boutique et acheter (👥)
- **Faire** (propriétaire) : poser un coffre dans le claim du magasin, mettre 64 pierres dedans, poser un panneau
  contre le coffre.
- **Attendu** : au lieu de l'éditeur du panneau, un menu « Créer une boutique ».
- **Faire** : objet vendu « pierre » (« Écrire le nom »), quantité 16 ; prix « Monnaie (points) » 5 ; « Créer ».
- **Attendu** : le panneau affiche « [Troc] », 16 pierres, « contre », 5 points ; il n'est plus modifiable.
- **Faire** (2e joueur, avec au moins 5 points) : clic droit sur le panneau, acheter 1 lot.
- **Attendu** : il reçoit 16 pierres, perd 5 points ; le coffre a 48 pierres ; les 5 points sont **en attente dans
  la boutique** (le propriétaire les récupère en cliquant sur son panneau → « Récupérer les points »).

### 7.7 Nettoyer les points de test
- **Faire** : `/economie` → « Retirer » → Bloc d'émeraude (le maximum), puis `/clear` (👑) pour les détruire.
- **Attendu** : solde revenu à peu près à 0.

---

## Partie 8 : jetons et récompenses (≈ 5 min)

### 8.1 Jetons
- **Faire** (👑) : `/kaliumgive <pseudo> jeton_tp 3`, puis `jeton_emplacement`, `jeton_claim`, `jeton_mort` (1 chacun).
- **Attendu** : 4 sortes de jetons brillants (œil de l'Ender, carte, pelle en or, crâne de squelette).
- **Faire** : `/jetons` → « Déposer des jetons » → mettre les jetons et de la terre → fermer.
- **Attendu** : le menu compte 3 jetons de téléportation et 1 de chaque autre ; la terre est rendue.
- **Faire** : « Retirer » → 1 jeton de téléportation.
- **Attendu** : il revient dans l'inventaire ; il en reste 2.
- Normal : « Acheter » ne propose rien (les prix sont à 0 = pas encore en vente ; à fixer par LeKiwi06). Les jetons
  ne servent encore à rien d'autre (plugins de téléportation à venir).

### 8.2 Récompenses
- **Faire** (🙂) : `/rewards` (ou bouton « Récompenses » du menu).
- **Attendu** : le menu s'ouvre, **vide** pour l'instant. C'est normal : les récompenses de Kal-Games vont encore sur
  Event. Le vrai test est en partie 14.

---

## Partie 9 : objets custom (≈ 20 min) — 👑 pour donner, puis tester en **survie**

### 9.1 Bedrock Breaker
- **Faire** : `/kaliumgive <pseudo> bedrock_breaker 2` ; clic droit sur un bloc de bedrock (fond du monde, y = -64).
- **Attendu** : le bloc disparaît sans rien lâcher ; il reste 1 Bedrock Breaker. Un 2e clic **dans la seconde**
  ne fait rien (1 bloc par seconde au plus).

### 9.2 Changeur de Biome
- **Faire** : `/kaliumgive <pseudo> changeur_biome 2` ; dans le monde normal, **hors des claims des autres et de la
  zone des shops**, clic droit avec l'objet.
- **Attendu** : menu avec « Historique », « Forme : sphère (passer au cube) » et la liste des biomes.
- **Faire** : choisir un biome (ex. « Désert »).
- **Attendu** : « Vous avez changé le biome pour : Désert » ; la couleur de l'herbe et des feuilles change autour
  (32 blocs de rayon) ; un Changeur est consommé ; aucun bloc ne bouge.
- **Faire** : réessayer **à moins de 32 blocs de la zone des shops**.
- **Attendu** : « Impossible : la zone touche une zone protégée. Ton Changeur de Biome n'a pas été utilisé. »
- **Faire** : essayer dans le Nether.
- **Attendu** : « Le Changeur de Biome ne s'utilise que dans l'overworld. »

### 9.3 Estomac du gardien
- **Faire** : `/kaliumgive <pseudo> estomac_gardien 3` ; clic droit avec un estomac.
- **Attendu** : il est consommé et donne des objets marins (œuf de tortue, bateau, coraux, algues, trident
  enchanté, armure de nautile, cœur de la mer…) et parfois (20 %) un casque en diamant enchanté.

### 9.4 Coffre de l'Ender agrandi et Clé de l'End
- **Faire** : ouvrir un coffre de l'Ender.
- **Attendu** : **6 lignes** : les 3 du haut = le coffre de l'Ender habituel (contenu inchangé), les 3 du bas =
  barrières nommées « Case bloquée » (« Dépose une Clé de l'End ici pour la débloquer. »).
- **Faire** : `/kaliumgive <pseudo> cle_de_l_end 2` ; prendre une clé et cliquer sur une case bloquée.
- **Attendu** : la case devient vide et utilisable ; une clé consommée. Mettre un objet dedans, fermer, se
  déconnecter / reconnecter, rouvrir.
- **Attendu** : la case est toujours débloquée et l'objet est toujours là.

### 9.5 Fiole d'expérience (stocker de l'XP)
- **Faire** : avoir de l'XP (👑 `xp add <pseudo> 2000 points`) et une **fiole vide** (bouteille en verre) ; enclume :
  fiole vide en 1re case, 2e case vide, taper **100** dans le champ du nom ; prendre le résultat.
- **Attendu** : « Fiole d'expérience (100 XP) » ; exactement 100 points d'XP retirés.
- **Faire** : la lancer (clic droit).
- **Attendu** : elle se brise et lâche les 100 points en orbes.
- **Faire** : mettre une fiole d'expérience en 1re case de l'enclume pour la renommer.
- **Attendu** : aucun résultat (renommage interdit).
- **Bedrock** : accroupi + clic droit sur l'enclume avec une fiole vide en main → formulaire « Fiole d'expérience ».

### 9.6 Spawner
- **Faire** : `/kaliumgive <pseudo> spawner_zombi 1` ; le poser dans un endroit sombre ; attendre.
- **Attendu** : des zombies apparaissent (règles des spawners vanilla : joueur à moins de 16 blocs, obscurité).
- **Faire** : le casser avec n'importe quel outil (même à la main), en survie.
- **Attendu** : le spawner **tombe au sol** (sans XP) et peut être reposé.
- **Faire** : mettre une TNT à côté et l'allumer.
- **Attendu** : le spawner tombe aussi au sol (pas perdu).

### 9.7 Élixirs
- **Faire** : `/kaliumgive <pseudo> elixir_titan 1` ; le boire.
- **Attendu** : Force I pendant 60 minutes, sans particules.
- **Faire** : essayer de le mettre dans un alambic.
- **Attendu** : impossible.
- (Les 11 id : `elixir_super_gateau`, `elixir_chauve_souris`, `elixir_anguille`, `elixir_ignifugation`,
  `elixir_plume`, `elixir_phenix`, `elixir_fantome`, `elixir_titan`, `elixir_vent`, `elixir_rebond`, `elixir_fortune`.)
- **Faire** : boire `elixir_fortune`.
- **Attendu** : Chance III ; barre verte en haut de l'écran « Élixir de Fortune : 9:59 » qui se vide sur 10 minutes.

### 9.8 Têtes
- **Faire** (👑) : `/tetes`.
- **Attendu** : menu de toutes les têtes de mobs (45 par page) ; un clic en donne une. Poser une tête, la casser :
  elle redonne la même tête (même nom).

---

## Partie 10 : crafts, enclume, villageois (≈ 15 min)

### 10.1 Livre de recettes
- **Faire** : ramasser un ingrédient d'une recette custom (ex. un colorant orange), ouvrir le livre de recettes de
  l'établi.
- **Attendu** : la recette custom correspondante apparaît (ici : 8 sables rouges).

### 10.2 Quelques crafts (à l'établi)
| Mettre | Obtenir |
|---|---|
| 8 sables **autour** d'1 colorant orange (au centre) | 8 sables rouges |
| 1 grès (n'importe où) | 4 sables |
| 4 blocs de fer | 4 blocs de fer brut |
| 8 obsidiennes autour d'1 larme de ghast | 8 obsidiennes pleureuses |
| 8 bâtons autour d'1 membrane de phantom | 1 cadre invisible |
| 9 charbons de bois | 1 bloc de charbon de bois (qui ne se pose pas) |
| carapace de shulker / **coffre de l'Ender** / carapace de shulker (en colonne) | 1 boîte de shulker |
- **Attendu** : chaque craft donne le bon résultat. La boîte de shulker **ne se fait plus** avec un coffre normal.

### 10.3 Recettes des objets custom
- Bedrock Breaker : poudre de blaze / charge de feu / poudre de blaze ; wagonnet à TNT / cristal de l'End /
  wagonnet à TNT ; poudre de blaze / ancre de réapparition / poudre de blaze.
- Spawner à zombi : 7 Fragments de Spawner + 1 Cœur de Spawner + 1 tête de zombie (sans forme ;
  `/kaliumgive <pseudo> fragment_spawner 7` et `coeur_spawner 1`).
- **Attendu** : les deux crafts marchent.

### 10.4 Alambic
- **Faire** : essayer de mettre une verrue du Nether dans un alambic.
- **Attendu** : refusé, « La verrue du Nether ne se brasse plus. »

### 10.5 Enclume sans « Trop cher ! »
- **Faire** : avoir beaucoup de niveaux (👑 `xp add <pseudo> 100 levels`) ; fusionner deux objets très enchantés /
  très réparés jusqu'à dépasser 40 niveaux de coût.
- **Attendu** : jamais « Trop cher ! » ; au-delà de 39, aucune ligne de coût, et le **vrai coût** est écrit en
  dernière ligne de la description du résultat (« Coût réel : 55 niveaux », vert si on a assez de niveaux). La prise
  marche et retire bien ce nombre de niveaux.

### 10.6 Villageois
- **Faire** : clic droit sur un villageois qui a un métier, puis sur un marchand ambulant.
- **Attendu** : **aucun échange** proposé (voulu : seuls de futurs PNJ en auront).

---

## Partie 11 : butins (≈ 20 min) — en **survie**

### 11.1 Minerais
- **Faire** : miner du minerai de fer et d'or **sans** Fortune ni Toucher de soie.
- **Attendu** : **2** fers bruts / ors bruts par minerai (au lieu de 1 en vanilla).
- **Faire** : miner du minerai d'**émeraude de deepslate** (sans Toucher de soie).
- **Attendu** : l'émeraude **et une potion de chance** à chaque fois. Avec Toucher de soie : pas de potion.
- **Faire** : casser un **spawner naturel** (donjon, mineshaft).
- **Attendu** : 1 Fragment de Spawner (parfois 2) ; le spawner compte dans « Exploration journalière ».

### 11.2 Pêche
- **Faire** : pêcher un bon moment (une canne avec Chance de la mer aide).
- **Attendu** : jamais de livre enchanté (à la place : arc ou canne enchantés, étiquette, carapace de nautile,
  selle).

### 11.3 Mobs
- **Faire** : tuer des mobs (zombies, squelettes, golem de fer, sorcières, blazes…).
- **Attendu** : butins modifiés (ex. golem de fer : pépites au lieu de lingots ; zombies : la chair putréfiée devient
  parfois un os) ; **1 %** de chance d'une tête du mob. Les mobs nés d'un **spawner** ne donnent ni tête ni potion.

### 11.4 Coffres de structures et limite journalière
- **Faire** : ouvrir des coffres de structures **jamais ouverts** (village, donjon, mineshaft, ruines…).
- **Attendu** : à chaque coffre, dans le tchat **et** la barre d'action : « Exploration journalière : 1 / 10 »,
  puis 2 / 10… Le tableau suit.
- **Faire** : arriver à 10, puis essayer d'en ouvrir un 11e.
- **Attendu** : refusé, « Exploration journalière : limite atteinte, reviens demain. » ; le coffre ne peut pas non
  plus être cassé ni explosé. Remise à zéro à minuit.
- **Faire** : boire `elixir_fortune` (Chance III) et ouvrir un coffre neuf.
- **Attendu** : « Chance III : ouverture libre, elle ne compte pas. »

---

## Partie 12 : staff et anti-triche (≈ 10 min)

### 12.1 Interface anti-triche
- **Faire** (👑) : `/anticheat`.
- **Attendu** : interface avec alertes récentes, joueurs avec alertes, chercher un joueur, suspendus, morts
  d'entités importantes, journal invsee / ecsee.

### 12.2 Inventaires des joueurs (👥)
- **Faire** (👑) : `/invsee <pseudo du 2e joueur>` puis `/ecsee <pseudo>`.
- **Attendu** : on voit son inventaire / son coffre de l'Ender ; un objet retiré disparaît bien chez lui.

### 12.3 Suspension (👥)
- **Faire** (👑) : `/anticheat <pseudo>` → suspendre avec une raison « test ».
- **Attendu** : il est expulsé avec « Une erreur inhabituelle est survenue, contacte le staff. » et ne peut plus se
  connecter.
- **Faire** : **lever la suspension** dans le même menu.
- **Attendu** : « <pseudo> pourra de nouveau se connecter à Kixster. » ; il se reconnecte.

### 12.4 GrimAC
- **Faire** (🙂) : jouer normalement 5 minutes : courir, sauter, nager, monter en bateau et à cheval, combattre.
- **Attendu** : aucun « retour en arrière » gênant, aucune expulsion. Connu : avec une **ancienne version** de
  Minecraft (ViaBackwards), les véhicules peuvent avoir des soucis.

### 12.5 JourneyMap (si installé côté client)
- **Attendu** : la carte marche, mais **sans** radar d'entités ni vue des grottes.

---

## Partie 13 : réglages du serveur (≈ 5 min)

- **Pistons** (réglage de LeKiwi06) : une machine à dupliquer la TNT (ou les tapis, rails) **fonctionne**.
- **Anti-xray** : rien à voir sans mod de triche ; le réglage est vérifié dans les fichiers.
- **Fluidité** : avec 2 ou 3 joueurs qui explorent, le jeu ne doit pas ramer. **S'il rame** : me le dire, je
  remets `chunk-system.worker-threads` à `-1` (il est encore à 3, réglage de la prégénération), redémarrage
  nécessaire.

---

## Partie 14 : avant d'ouvrir aux joueurs

- [ ] Prévenir Claude : il passe **KG_Rewards** de Kal-Games en `boite: kixster` (sinon les récompenses gagnées sur
  Kal-Games continuent d'aller sur Event) ; redémarrage de Kal-Games nécessaire.
- [ ] Gagner une récompense sur Kal-Games, puis aller sur Kixster : message « Nouvelle récompense » (dans les 30 s)
  et la récompense dans `/rewards` → « Récupérer ».
- [ ] Retirer les points de test (7.7), supprimer les claims et le magasin de test si besoin.
- [ ] Remettre `/dimensions` : Nether et End **activés** (4.1).
- [ ] 🖥️ `whitelist off` (ou la garder, selon ce qui est prévu pour l'ouverture).
- [ ] Dire à Claude les tests qui ont échoué ; il note le résultat dans `REPRISE_PROJET.md`.

---

## Récapitulatif à cocher

- [ ] 1. Préparer (whitelist, permissions)
- [ ] 2. Connexion (Java, Bedrock, tableau, tchat)
- [ ] 3. Menus (`/menu on`, étoile, `/kixster`, boutons, Bedrock)
- [ ] 4. Dimensions (`/dimensions`, Nether, End)
- [ ] 5. Régions WorldGuard (`zone_shop`, `ile_du_dragon`)
- [ ] 6. Claims (créer, menu, commandes interdites, membres, Nether/End, île, zone des shops)
- [ ] 7. Économie (dépôt/retrait, masquer, échange, rachats, magasin, boutique)
- [ ] 8. Jetons et récompenses
- [ ] 9. Objets custom (Bedrock Breaker, Changeur de Biome, Estomac, Clé de l'End, fiole, spawner, élixirs, têtes)
- [ ] 10. Crafts, alambic, enclume, villageois
- [ ] 11. Butins (minerais, pêche, mobs, coffres et limite de 10)
- [ ] 12. Staff (anti-triche, invsee/ecsee, suspension, GrimAC, JourneyMap)
- [ ] 13. Réglages serveur (pistons, fluidité)
- [ ] 14. Avant l'ouverture
