# KS_Marteau - journal

Plugin de Kixster : le marteau. Cahier des charges : cahier local `Marteau.md` (validé par LeKiwi06 le 20/11/2026),
publié dans `CAHIER_DES_CHARGES.md` au déploiement.

## 1.0.0 - marteau, Cœur de Marteau, Magnétisme (20/11/2026, LeKiwi06)

**Statut : compilé, non déployé, non testé en jeu.**

- **Plugin Paper** (`paper-plugin.yml`, pas `plugin.yml`) : l'enchantement Magnétisme est enregistré au démarrage par
  `Demarrage` (bootstrapper), le registre des enchantements se ferme ensuite. SimpleClaimSystem facultatif
  (`join-classpath` pour lire ses claims). Aucune dépendance à KLM_Menu : textes dans le code.
- **Cœur de Marteau** : noyau lourd entouré de 8 blocs de diamant (un noyau lourd ordinaire seulement). Objet : noyau
  lourd renommé, aspect enchanté, marqueur `ks_marteau:coeur_de_marteau` ; il ne se pose pas.
- **Marteau** : `bloc de diamant / Cœur / bloc de diamant` en haut, un bâton au milieu des deux rangées du bas.
  Objet : une **vraie masse** du jeu (coup écrasé du jeu), marqueur `ks_marteau:marteau` = `diamant` ou `netherite`,
  composants posés par le plugin :
  - `tool` : règles de la pioche du même niveau (blocs trop durs pour le niveau sans butin, puis `mineable/pickaxe` à
    la vitesse 8 en diamant, 9 en netherite ; 1 ailleurs) ; **1 point de solidité par bloc** (la masse en prend 2) ;
  - `max_damage` : 9 366 (diamant, 1 561 × 6) et 12 186 (netherite, 2 031 × 6) ;
  - dégâts : 6 (comme la masse) et 7 en netherite ; vitesse d'attaque de la masse ;
  - `repairable` retiré (la masse se répare à la baguette de breeze) : le marteau se répare à l'enclume avec un
    **noyau lourd** ordinaire, qui le remet à neuf (« renouveler le noyau », 1 niveau + pénalités du jeu), avec un
    autre marteau du même niveau, ou par Raccommodage ;
  - netherite : `damage_resistant` au feu (feu et lave, comme les objets en netherite).
- **Marteau en netherite** : table de forge, modèle d'amélioration en netherite + marteau en diamant + **bloc** de
  netherite. Recette sur n'importe quelle masse, le résultat n'est donné que pour un marteau en diamant (usé et
  enchanté compris) ; enchantements, usure et nom donné gardés.
- **Minage en 3 × 3** (`Minage`) : un bloc qu'une pioche du même niveau mine casse aussi les 8 blocs autour, dans le
  plan de la face frappée (dernier clic gauche, sinon le regard) ; seuls ceux qu'une pioche mine sont cassés. Jamais :
  contenants, spawners, spawners des épreuves, coffres-forts, blocs incassables. Accroupi : un seul bloc. Chaque
  voisin est cassé par `Player#breakBlock` : claims, protections des autres plugins, butin, XP, Toucher de soie,
  Solidité et 1 point de solidité par bloc passent par le jeu.
- **Aura de feu** : les minerais de fer, d'or, de cuivre et les débris antiques donnent le résultat du four (lingots,
  fragment de netherite), sans l'XP de la cuisson.
- **Table d'enchantement** (`Table`) : pour un marteau et pour un livre, tirage refait par le plugin avec
  l'algorithme du jeu (`EnchantmentHelper.selectEnchantment`), coûts du jeu. Marteau : Efficacité, Toucher de soie,
  Solidité, Aura de feu, Magnétisme (pas Fléau des arthropodes, réponse du 20/11). Livre : enchantements du jeu + Magnétisme.
- **Magnétisme** (`kalium:magnetisme`) : réglages de Fortune (poids 2, niveaux I à III, coût 15 + 9 par niveau à
  65 + 9 par niveau, enclume 4). Marteau tenu en main (ou dans l'autre main) : les objets au sol dans un rayon de
  3 / 5 / 7 blocs sont attirés vers le joueur (tous les 4 ticks), sauf les orbes d'XP, les objets d'un claim où il n'a
  pas les droits et les objets qu'il ne peut pas encore ramasser. Hors de tout tag du jeu : seul ce plugin le propose.
- **Enclume** (`Enclume`) : marteau à gauche, résultat calculé par le plugin à la manière du jeu : livre enchanté ou
  marteau du même niveau ou noyau lourd à droite ; enchantements permis = ceux de la table + Raccommodage + malédiction de
  disparition ; Toucher de soie et Aura de feu incompatibles ; coûts et pénalité de travaux du jeu. Masse ordinaire :
  ni Magnétisme, ni marteau sacrifié.
- **Déploiement en deux temps** : nouveau plugin `KS_`, mis en test tout seul par KS_MondeTest (écouteurs et
  recettes). La tâche du Magnétisme demande `PhaseTest.ouvert("KS_Marteau", monde)`.

Réponses de LeKiwi06 du 20/11 aux choix de Claude : pas de Fléau des arthropodes ; Magnétisme actif quand le
marteau est tenu (main ou autre main), pas rangé ; réparation par un noyau lourd (remis à neuf). En attente : les
voisins du 3 × 3 sont cassés quelle que soit leur dureté (obsidienne autour d'une pierre, par exemple).

Points à vérifier en premier, dans Kixster Test :
- au démarrage : pas d'erreur de KS_Marteau, ni « Enchantement Magnétisme absent du registre » ;
- vitesse de minage sur **Bedrock** (le client calcule lui-même le temps de casse ; diagnostic KS_DiagOutils) ;
- Magnétisme à la table et sur un livre, pour Java et Bedrock (Geyser ne connaît pas l'enchantement) ;
- recette de forge sur Bedrock ;
- alertes de KS_AntiCheat (x-ray en alertes) quand 9 minerais tombent d'un coup.

Reste à faire : prix dans KS_Economy (Cœur 673,6, Marteau 745,6, Marteau en netherite 2 227,6, livre de Magnétisme 6),
rareté KS_Textures, apparence Bedrock (geyser-bedrock) et texture Java, id KS_KaliumGive, entrée du didacticiel.
