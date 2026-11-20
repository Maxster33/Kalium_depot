# KS_SuperTool - journal

Plugin de Kixster : les machines, outils qui travaillent en 3 × 3. Cahiers des charges : cahiers locaux `Marteau.md`
et `Outils_3x3.md` (validés par LeKiwi06 le 20/11/2026), publiés dans `CAHIER_DES_CHARGES.md` au déploiement.
Ancien nom : KS_Marteau (jamais déployé, renommé le 20/11 à la demande de LeKiwi06).

## 1.0.0 - Foreuse, Abatteuse, Excavatrice, Laboureuse, Magnétisme (20/11/2026, LeKiwi06)

**Statut : compilé, non déployé, non testé en jeu.**

- **Plugin Paper** (`paper-plugin.yml`, pas `plugin.yml`) : l'enchantement Magnétisme est enregistré au démarrage par
  `Demarrage` (bootstrapper), le registre des enchantements se ferme ensuite. SimpleClaimSystem facultatif
  (`join-classpath` pour lire ses claims). Aucune dépendance à KLM_Menu : textes dans le code.
- **Cœur de machine** : noyau lourd entouré de 8 blocs de diamant (un noyau lourd ordinaire seulement). Objet : noyau
  lourd renommé, aspect enchanté, marqueur `ks_supertool:coeur_de_machine` ; il ne se pose pas.
- **Machines** (`Outil` : machine × niveau ; marqueur `ks_supertool:outil` = `foreuse`, `foreuse_netherite`,
  `abatteuse`... ; c'est aussi le nom de la texture et l'id de KS_KaliumGive). Crafts : le Cœur juste au-dessus des
  deux bâtons, des blocs de diamant complètent la forme de l'outil du jeu :
  - **Foreuse** (ex-marteau) : `bloc / Cœur / bloc` puis 2 bâtons. Une **vraie masse** (coup écrasé du jeu) avec le
    composant `tool` de la pioche du même niveau (vitesse 8, 9 en netherite), dégâts 6 / 7 ;
  - **Abatteuse** : `bloc Cœur / bloc bâton / _ bâton` ; vraie hache en diamant ;
  - **Excavatrice** : `Cœur / bâton / bâton` ; vraie pelle en diamant ;
  - **Laboureuse** : `bloc Cœur / _ bâton / _ bâton` ; vraie houe en diamant.
  Pour les trois dernières, vitesse, dégâts et temps de casse sont ceux du jeu (Bedrock compris).
- **Netherite** : table de forge, modèle d'amélioration + machine en diamant + **bloc** de netherite (recette sur
  n'importe quelle masse, hache, pelle ou houe en diamant ; résultat seulement pour une machine en diamant, usée et
  enchantée comprise ; le type change avec `withType`, enchantements, usure et nom donné gardés). La recette du jeu
  (lingot) et toute autre recette de forge sont refusées pour une machine.
- **Solidité** : 6 fois l'outil du jeu (9 366 en diamant, 12 186 en netherite), 1 point par bloc. `repairable` retiré :
  réparation à l'enclume par un **noyau lourd** ordinaire (remise à neuf, « renouveler le noyau », 1 niveau +
  pénalités du jeu), par la même machine, ou par Raccommodage ; jamais par matériau.
- **3 × 3 en cassant** (`Minage`) : un bloc que l'outil du jeu mine casse aussi les 8 autour, dans le plan de la face
  frappée (dernier clic gauche, sinon le regard), seulement ceux que l'outil mine ; **Foreuse** : pas un voisin plus
  dur que le bloc frappé ; jamais : contenants, spawners, spawners des épreuves, coffres-forts, blocs incassables ;
  accroupi : un seul bloc. Chaque voisin par `Player#breakBlock` (claims, protections, butin, XP, Toucher de soie,
  Solidité passent par le jeu et les autres plugins).
- **Laboureuse, récolte** : casser une culture mûre (blé, carottes, pommes de terre, betteraves, verrues du Nether)
  récolte les cultures mûres en 3 × 3 à la même hauteur et les replante au tick suivant, une semence reprise dans le
  butin de chacune ; accroupi : seulement celle-ci. Cultures de **KS_Cultures** (légumes, maïs, riz, kiwi) : avec
  KS_Cultures 1.1.0 (API `cultureMure` / `recolter`, dépendance facultative `join-classpath`, classe `Cultures`), la
  casse est annulée et KS_Cultures les récolte et les remet au stade « pousse » (kiwi : cueilli, l'arbuste reste) ;
  mélangées aux cultures du jeu dans la même zone 3 × 3.
- **3 × 3 au clic droit** (`Clic`, accroupi : seulement le bloc visé, transformé par le jeu) : Laboureuse = labour
  (terre, herbe, chemin ; terre grossière -> terre ; podzol et mycélium -> terre labourée directement, aussi le bloc
  visé) ; Excavatrice = chemins ; à l'horizontale, air au-dessus, jamais par-dessous ; Abatteuse = écorce, gratte le
  cuivre d'un cran, retire la cire, dans le plan de la face visée. 1 point de solidité par bloc transformé ; jamais un
  contenant ; pas dans le claim d'un autre.
- **Aura de feu** (Foreuse) : minerais de fer, d'or, de cuivre et débris antiques cuits (lingots, fragment), sans l'XP
  de la cuisson ; incompatible avec Toucher de soie.
- **Table d'enchantement** (`Table`) : tirage refait par le plugin pour une machine et pour un livre (algorithme du jeu,
  coûts du jeu). Foreuse : Efficacité, Toucher de soie, Solidité, Aura de feu, Magnétisme. Autres machines : ceux de
  la table du jeu pour l'outil, **sans Fortune**, plus Magnétisme. Livre : enchantements du jeu + Magnétisme.
- **Magnétisme** (`kalium:magnetisme`) : réglages de Fortune (poids 2, niveaux I à III, coût 15 + 9 par niveau à
  65 + 9 par niveau, enclume 4). Machine tenue en main (ou dans l'autre main) : objets au sol attirés dans un rayon de
  3 / 5 / 7 blocs (tous les 4 ticks), pas l'XP, pas les objets d'un claim sans droits, pas ceux qu'il ne peut pas
  encore ramasser. Hors de tout tag du jeu : seul ce plugin le propose.
- **Enclume** (`Enclume`, priorité LOW : avant la réduction de coût de KS_Enclume ; l'enclume en coffre de KS_Enclume
  passe aussi par ce calcul) : machine à gauche, résultat calculé par le plugin à la manière du jeu ; outil ordinaire :
  ni Magnétisme, ni machine sacrifiée. Établi et meule : une machine ne se combine avec rien.
- **Déploiement en deux temps** : nouveau plugin `KS_`, mis en test tout seul par KS_MondeTest (écouteurs et
  recettes). La tâche du Magnétisme demande `PhaseTest.ouvert("KS_SuperTool", monde)`.
- `KSSuperTool.creer(id)` et `ids()` : pour KS_KaliumGive, par réflexion.

Plugins communs (20/11, à déployer ensemble) : KS_Economy 1.13.7 (prix, une machine usée n'est pas rachetée),
KS_Textures 1.2.3 (rareté, textures de la Foreuse), KS_KaliumGive 1.14.0, KS_Cultures 1.1.0 (facultatif : récolte de
ses cultures), `didacticiel.yml` (crafts > machines,
machines-clic ; enclume > magnetisme ; `requiert: "KS_SuperTool"`), pack Java 1.5.0, geyser-bedrock 1.12.0.

Points à vérifier en premier, dans Kixster Test :
- au démarrage : pas d'erreur de KS_SuperTool, ni « Enchantement Magnétisme absent du registre » ;
- vitesse de minage de la Foreuse sur **Bedrock** (le client calcule lui-même le temps de casse) ;
- Magnétisme à la table et sur un livre, pour Java et Bedrock (Geyser ne connaît pas l'enchantement) ;
- recettes de forge sur Bedrock ;
- alertes de KS_AntiCheat (x-ray en alertes) quand 9 minerais tombent d'un coup.

Textures de l'Abatteuse (hache double en croissants), de l'Excavatrice et de la Laboureuse validées par LeKiwi06 le
20/11 : KS_Textures 1.2.4, pack Java 1.6.0, geyser-bedrock 1.13.0 (construits, non envoyés).
