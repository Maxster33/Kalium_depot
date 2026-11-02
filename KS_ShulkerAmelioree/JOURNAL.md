# KS_ShulkerAmelioree — journal

## 1.0.0 — 2026-11-02 — non testé en jeu, non déployé

**Demande** (LeKiwi06, 01-02/11/2026, cahier local « Blocs, cultures et objets », section 4) : une shulker améliorée
qui contient autant qu'un coffre double, gérée comme le coffre renforcé (contenu rangé par le plugin, entonnoirs et
comparateurs) ; aucune shulker à l'intérieur, même vide ; décraft de la boîte de shulker normale vide (2 carapaces +
le coffre de l'Ender de la recette KS_Crafts 1.10.0) ; la shulker améliorée ne se décrafte pas. Elle servira
d'ingrédient à l'encapsuleur.

**Fonctionnement**
- Craft (enregistré par le plugin, clé `ks_shulkeramelioree:shulker_amelioree`) : une boîte de shulker **vide** au
  centre, 4 gabarits d'amélioration en netherite en + ; la couleur est gardée. Une boîte avec du contenu, ou déjà
  améliorée, ne donne rien.
- Objet : boîte de shulker nommée « Shulker améliorée », aspect enchanté, marque dans ses données ; son contenu
  (54 cases) est gardé dans ses données (jamais dans le contenu de boîte du jeu) ; la description donne le nombre de
  cases occupées et les 5 premiers objets.
- Bloc : vraie boîte de shulker. Contenu (54 cases) gardé dans le bloc ; les 27 cases du jeu servent de tampon pour
  les entonnoirs et le comparateur (comme le coffre renforcé, voir `Contenu.java`). Le tampon est choisi pour que le
  comparateur donne le signal d'un coffre double avec ces 54 cases (vérifié sur 200 000 contenus aléatoires, calcul en
  float comme le jeu).
- Clic droit : menu de 6 rangées (54 cases), partagé entre joueurs comme un coffre ; le couvercle s'ouvre. Écrit dans
  le bloc au tick qui suit chaque clic et à la fermeture (déconnexion, mort). Les claims s'appliquent (le menu ne
  s'ouvre que si la boîte aurait pu s'ouvrir, et une boîte bloquée par un bloc au-dessus ne s'ouvre pas).
- Entonnoirs / droppers : n'y mettent que ce qui tient dans les 54 cases, jamais une boîte de shulker ; ils attendent
  pendant qu'un menu est ouvert.
- Cassée : menus fermés, contenu retiré du bloc, l'objet tombe avec son contenu et son nom (créatif : rien si vide).
  Explosions, wither, dragon : pas détruite. Pistons : ne la poussent ni ne la tirent (le jeu la casserait). Distributeur :
  ne la pose pas. Objet détruit au sol (lave, feu, cactus, explosion) : son contenu tombe, comme une boîte du jeu.
- Décraft (clé `ks_shulkeramelioree:decraft_shulker`) : une boîte de shulker ordinaire vide, seule dans la grille →
  2 carapaces ; le coffre de l'Ender reste dans la grille (crafter : dans sa case). Teinture de la shulker améliorée :
  permise (recette du jeu, contenu gardé).
- API : `estShulkerAmelioree(ItemStack|Block)`, `creerShulker()`, `creerShulker(Material)`, `estVide(ItemStack)`,
  `contenu(ItemStack|Block)`.

**Validé par LeKiwi06 le 02/11/2026** : prix de **129,49** (boîte de shulker 17,45 + 4 gabarits d'amélioration en
netherite à 28,01 ; à ajouter dans KS_Economy) et craft avec une boîte de shulker **vide** seulement.

**Limites**
- Shulker presque pleine (53 cases occupées sur 54) : un entonnoir peut rester bloqué pour un objet nouveau (le tampon
  n'a plus de case vide), il passe dès qu'un objet sort.
- Créatif : poser la shulker améliorée ne consomme pas l'objet, son contenu est donc copié (comme une boîte du jeu).
- Un plugin qui casse le bloc sans événement (`breakNaturally`) ne lâcherait que le tampon.
- Le coffre renforcé (KS_CoffreRenforce) accepte une shulker améliorée pleine (il ne voit que le contenu de boîte du
  jeu) : à lui faire refuser via `estVide`.

**À déployer ensemble** (02/11/2026) : KS_ShulkerAmelioree 1.0.0, KS_Economy 1.13.6 (prix 129,5 ; rachetée seulement
vide ; jamais contenant de boutique), KS_CoffreRenforce 1.3.1 (refuse une shulker améliorée pleine), KS_Textures 1.2.2
(rareté Rare) et `didacticiel.yml` (2 entrées dans « Crafts »).
