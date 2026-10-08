# Kixster : tests en jeu après l'installation des plugins d'Event (08/10/2026)

À faire dans l'ordre. Cocher au fur et à mesure (`[x]`). Un test qui échoue : noter ce qui s'est passé (capture si
possible) et prévenir Claude, qui lira le journal du serveur.

- **(console)** = à taper dans la console de Kixster (Minestrator), sans `/`.
- **(op)** = en jeu, avec un compte opérateur.
- **(joueur)** = en jeu, avec un compte **qui n'est pas opérateur** (sinon les interdictions ne se voient pas).
- **(2 joueurs)** = il faut être deux.

---

## 1. Préparer

- [ ] (console) Fermer le serveur aux joueurs pendant les tests : `whitelist on`, puis `whitelist add <pseudo>` pour
  chaque testeur (Maxster33, LeKiwi06, compte de test…).
- [ ] (console) **Permissions** (les mêmes qu'Event, lues dans sa base LuckPerms le 08/10/2026) :
  ```
  lp creategroup admin
  lp group admin permission set kseconomy.staff true
  lp group default permission set scs.command.claim false
  lp group default permission set scs.command.claim.* false
  lp group default permission set scs.command.claims false
  lp group default permission set scs.command.unclaim false
  lp user LeKiwi06 parent add admin
  ```
  Sur Event, seul LeKiwi06 est dans le groupe admin. Ajouter Maxster33 si besoin : `lp user Maxster33 parent add admin`.
- [ ] (console) Vérifier : `lp group default permission info` (4 lignes `scs.command…` à `false`) et
  `lp group admin permission info` (`kseconomy.staff`).

## 2. Connexion

- [ ] Connexion **Java** par le proxy : on arrive sur Kixster, pas de déconnexion, pas d'erreur dans la console.
- [ ] Connexion **Bedrock** (Floodgate) : on arrive sur Kixster.
- [ ] Message de **KS_FairPlay** environ 2 s après la connexion (mini-cartes : vue des grottes et radar coupés).
- [ ] **Tableau** à droite de l'écran, titre « Kixster ». `/tableau off` le masque, `/tableau on` le remet.
- [ ] Discuter dans le chat (KLM_Chat) ; chat vocal si le mod est installé.

## 3. Menus

- [ ] **Étoile du Nether** en case 5 de la barre ; clic droit : menu « Kixster ».
- [ ] `/kixster` ouvre le même menu. (`/event` ne doit **pas** l'ouvrir.)
- [ ] Chaque bouton du menu ouvre bien son écran : Économie, Claims, Jetons, Récompenses…
- [ ] `/menu` (KLM_Menu) s'ouvre ; `/menu on` / `/menu off` donnent / retirent les objets de menu.
- [ ] Sur **Bedrock** : les menus s'affichent aussi.

## 4. Dimensions

- [ ] (op) `/dimensions` : le menu montre les portails du Nether et de l'End **activés**.
- [ ] Passer un portail du Nether : on arrive dans le Nether, retour possible.
- [ ] Aller dans l'End (portail de l'End ; `/tp` en op si besoin).

## 5. Régions WorldGuard (à faire avant les claims et les magasins)

**Zone des shops** (dans le monde normal, à l'endroit choisi pour les shops) :
- [ ] (op) `//wand`, puis clic gauche sur un coin de la zone et clic droit sur le coin opposé. Prévoir toute la
  hauteur : `//expand vert`.
- [ ] (op)
  ```
  /rg define zone_shop
  /rg flag zone_shop build allow
  /rg flag zone_shop scs-claim allow
  ```
**Île du dragon** (en étant **dans l'End**) :
- [ ] (op)
  ```
  //pos1 -250,0,-250
  //pos2 250,255,250
  /rg define ile_du_dragon
  /rg flag ile_du_dragon scs-claim deny
  ```
- [ ] (op) `/rg info zone_shop` et `/rg info ile_du_dragon` (dans l'End) : les flags sont bien là.

## 6. Claims

- [ ] (joueur) `/ksclaim` dans un chunk libre : claim créé (gratuit jusqu'à 10 claims).
- [ ] (joueur) `/ksclaims` : menu de mes claims (nom, description, membres, réglages).
- [ ] (joueur) `/claim`, `/claims` et `/unclaim` (commandes de SimpleClaimSystem) : **refusées**.
- [ ] (2 joueurs) Le 2e joueur ne peut ni casser ni poser dans le claim du 1er ; après l'avoir ajouté comme membre, il peut.
- [ ] (joueur) Claim dans le **Nether** puis dans l'**End** (hors de l'île) : OK, **noms des mondes** affichés
  correctement (pas de nom bizarre du genre `minecraft:the_nether`). Si un nom est bizarre, me le dire, je corrige.
- [ ] (joueur) Claim sur l'**île du dragon** : **refusé**.
- [ ] (joueur) Claim dans la **zone des shops** : autorisé.

## 7. Économie et magasins

- [ ] (joueur) `/economie` : solde ; déposer des émeraudes (1 émeraude = 1 point), en retirer.
- [ ] (2 joueurs, à moins de 5 blocs) `/echange <joueur>`, l'autre clique sur « accepter » ; échanger des objets et
  des points ; vérifier que chacun a bien reçu sa part.
- [ ] (joueur) `/rachat` : les 10 objets rachetés de la semaine.
- [ ] (joueur avec assez de points : 45 000, ou 5 blocs compressés tier 3) Dans un de ses claims de la zone des shops :
  `/magasin create`, puis poser un panneau contre un coffre : menu « Créer une boutique ». Créer une boutique, la
  faire acheter par un 2e joueur (clic droit sur le panneau).
- [ ] Hors de la zone des shops, `/magasin create` est refusé.

## 8. Jetons et récompenses

- [ ] (op) `/kaliumgive <pseudo> jeton_tp 1` (aussi `jeton_emplacement`, `jeton_claim`, `jeton_mort`).
- [ ] (joueur) `/jetons` : déposer les jetons, les voir comptés, les retirer.
- [ ] (joueur) `/rewards` s'ouvre (vide pour l'instant, c'est normal : les récompenses de Kal-Games vont encore sur
  Event, voir la partie 13).

## 9. Objets custom (`/kaliumgive <pseudo> <id> <nombre>`, op ; Tab propose les id)

- [ ] `bedrock_breaker` : clic droit sur une bedrock, le bloc disparaît et l'objet est consommé.
- [ ] `changeur_biome` : menu des biomes, changer un biome autour de soi ; dans la zone des shops : **refusé**.
- [ ] `estomac_gardien` : clic droit, l'objet est consommé et donne son contenu.
- [ ] `cle_de_l_end` : ouvrir son coffre de l'Ender (6 lignes, 3 du bas bloquées), poser la clé sur une case
  bloquée : la case se débloque ; se déconnecter / reconnecter : elle reste débloquée.
- [ ] `fiole_exp(100)` (ou une fiole vide de KS_FioleExp) : stocker de l'XP, la boire, l'XP revient.
- [ ] `spawner_zombi` : le poser (zombies qui apparaissent), le casser avec n'importe quel outil : il tombe au sol.
- [ ] `elixir_titan` (ou un autre) : à boire, effet appliqué ; impossible de le mettre dans un alambic.
- [ ] `tete_mouton_rouge` : la tête s'affiche bien.

## 10. Crafts, enclume, villageois

- [ ] Livre de recettes : les recettes custom apparaissent après avoir ramassé un ingrédient.
- [ ] Quelques crafts : 8 sables autour d'1 colorant orange → 8 sables rouges ; 1 grès → 4 sables ; 8 obsidiennes
  autour d'1 larme de ghast → 8 obsidiennes pleureuses ; 8 bâtons autour d'1 membrane de phantom → cadre invisible.
- [ ] Enclume : réparer un objet très réparé, plus de « Trop cher ! » (le coût en niveaux reste celui du vanilla).
- [ ] Villageois : aucun échange proposé, impossible de les faire monter de niveau.

## 11. Butins

- [ ] Miner du minerai de **fer** ou d'**or** (sans Toucher de soie) : 2 à 5 minerais bruts.
- [ ] **Pêche** : jamais de livre enchanté (remplacé par un autre trésor).
- [ ] Tuer des mobs : butins modifiés, parfois une **tête** (KS_Decapitator, `/tetes` pour voir toutes les têtes).
- [ ] Minerai d'**émeraude de deepslate** : une potion de chance tombe à chaque fois (pas avec Toucher de soie).
- [ ] Coffres de structures (village, donjon, cité antique…) : le butin est modifié ; **limite de 10 coffres par jour**
  (KS_FairPlay) affichée dans la barre d'action.

## 12. Staff et anti-triche

- [ ] (op) `/anticheat` : l'interface s'ouvre (alertes, joueurs, suspensions).
- [ ] (op) `/invsee <joueur>` et `/ecsee <joueur>` : inventaire et coffre de l'Ender d'un joueur connecté.
- [ ] GrimAC : pas de déconnexion ni de retour en arrière gênant en jouant normalement (courir, nager, bateau, cheval).
  Connu : les vieilles versions de Minecraft (ViaBackwards) peuvent avoir des soucis avec les véhicules.
- [ ] JourneyMap (si installé côté client) : radar et vue des grottes coupés.

## 13. Réglages du serveur

- [ ] Duplication par pistons : une machine à dupliquer la TNT, par exemple, fonctionne.
- [ ] Anti-xray : rien à voir à l'œil nu sans mod ; réglage déjà vérifié dans les fichiers.
- [ ] Le jeu ne rame pas. **S'il rame** : Claude remet `chunk-system.worker-threads` à `-1` (il est encore à 3 depuis
  la prégénération).

## 14. Avant d'ouvrir aux joueurs

- [ ] Claude passe **KG_Rewards** de Kal-Games en `boite: kixster` (sinon les récompenses vont sur Event).
- [ ] Faire un essai de récompense depuis Kal-Games : elle arrive dans `/rewards` sur Kixster.
- [ ] (console) `whitelist off` (ou garder la whitelist, selon ce qui est prévu pour l'ouverture).
