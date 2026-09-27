# KV_BuildBattle - journal

Serveur Kanvas. Cahier des charges (commun avec KG_BuildBattle) : `KV_BuildBattle/CAHIER_DES_CHARGES.md`. Dépend de
KLM_Menu (menus, catalogue « Interfaces » ; API inchangée depuis 2.0.0, la version en place sur Kanvas) et de
WorldGuard (FAWE fournit l'API WorldEdit).

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

**Déployé sur Kanvas le 27/09/2026 (10:40). Statut : non testé en jeu.**
