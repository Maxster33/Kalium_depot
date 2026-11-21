# KS_Economy - journal

Plugin du serveur Event : économie. Cahier des charges : catégorie 2 « Économie » de LeKiwi06 (validé le 29/09/2026 ;
copié dans `CAHIER_DES_CHARGES.md` au déploiement). Codé en 2 temps (réponse de LeKiwi06) : score, blocs, `/echange`
d'abord ; magasins (Tradeshop) après KS_Claim (catégorie 3).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.13.6 - shulker améliorée (02/11/2026, LeKiwi06)

Chantier « Blocs, cultures et objets » (cahier local), section 4 : KS_ShulkerAmelioree 1.0.0. LeKiwi06 : « fait les 2 »
(ajouts aux plugins communs, puis l'encapsuleur).

- **Prix** : `shulker_amelioree` = boîte de shulker + 4 gabarits d'amélioration en netherite (17,45 + 4 × 28,01),
  **validé par LeKiwi06 le 02/11/2026** ; le barème donne 129,482, arrondi à 129,5 comme les autres prix (3 chiffres).
  Barème : `custom(...)` dans `sortie/bareme/bareme.py`, note dans `remplir.py` (avec les objets ajoutés après la
  liste de LeKiwi06), cible `pdc:ks_shulkeramelioree:shulker:b` dans `exporter_rachats.py`, ligne dans
  `ids-26.3-et-custom.csv` ; chaîne relancée : `rachats.csv` gagne exactement cette ligne (comparé avant / après,
  chaîne d'abord relancée sans changement : fichier identique). Dimensions : nether + end (gabarit, carapaces).
- **Prix de l'encapsuleur** (KS_Encapsuleur 1.0.0) : 8 shulkers améliorées + 1 crafter = 1 036,75, **validé par
  LeKiwi06 le 02/11/2026** (« Oui, valide ») ; arrondi à 1 037. Ajouté de la même façon (cible
  `pdc:ks_encapsuleur:objet:s:encapsuleur` : seul l'encapsuleur neuf, jamais une construction encapsulée) ; chaîne
  relancée : `rachats.csv` gagne exactement cette ligne.
- **Rachats** : une shulker améliorée n'est rachetée que vide (sinon son contenu serait perdu).
- **Boutiques** : une shulker améliorée posée n'est pas un contenant de boutique (la boutique ne verrait que les
  27 cases de son tampon). Une boîte de shulker ordinaire reste acceptée.

Compilé le 02/11/2026. **À déployer avec KS_ShulkerAmelioree 1.0.0 et KS_Encapsuleur 1.0.0** (sans eux, la ligne du barème ne correspond à
aucun objet ; rien ne casse). **Déployé sur Kixster le 02/11/2026 à 14:28** (LeKiwi06 : « Oui, envoie »), avec KS_ShulkerAmelioree 1.0.0, KS_Encapsuleur 1.0.0, KS_Economy 1.13.6, KS_CoffreRenforce 1.3.1, KS_Textures 1.2.2 et `didacticiel.yml` ; jars de la même taille que ceux du dépôt. À redémarrer par LeKiwi06. Statut : non testé en jeu.

## 1.13.7 - machines de KS_SuperTool (20/11/2026, LeKiwi06)

Chantiers du marteau et des outils 3 × 3 (cahiers locaux `Marteau.md` et `Outils_3x3.md`, validés le 20/11) :
KS_SuperTool 1.0.0 (ex-KS_Marteau, jamais déployé).

- **Prix** : somme des ingrédients, **validée par LeKiwi06 le 20/11/2026** (« le cumul des prix d'item me parait
  juste ») : `coeur_de_machine` 673,6 (noyau lourd 385,6 + 8 blocs de diamant) ; `foreuse` et `abatteuse` 745,6
  (Cœur + 2 blocs de diamant + 2 bâtons), `excavatrice` 673,6 (Cœur + 2 bâtons), `laboureuse` 709,6 (Cœur + 1 bloc
  + 2 bâtons) ; versions netherite + modèle de netherite 28,01 + bloc de netherite 1 454 (2 228, 2 228, 2 156,
  2 192 après l'arrondi de la chaîne). Cibles `pdc:ks_supertool:coeur_de_machine:b` et
  `pdc:ks_supertool:outil:s:<id>` (`foreuse`, `foreuse_netherite`, `abatteuse`...). Barème : `custom(...)` dans
  `sortie/bareme/bareme.py`, notes et liste des objets ajoutés dans `remplir.py`, cibles dans `exporter_rachats.py`,
  lignes dans `ids-26.3-et-custom.csv` ; chaîne relancée (d'abord sans changement : fichier identique) :
  `rachats.csv` gagne exactement ces 9 lignes. Le livre de Magnétisme reste un livre enchanté (6, jamais tiré), comme
  validé.
- **Rachats** : un objet custom usé n'est plus racheté (même règle que les objets du jeu depuis le 01/11) : un marteau
  doit avoir toute sa solidité (une machine aussi).

Statut : compilé, non déployé, non testé. À déployer avec KS_SuperTool 1.0.0 et KS_Textures 1.2.3 (rareté).

## 1.13.8 - boussole du voyageur (20/11/2026, LeKiwi06)

- **Prix** : `boussole_voyageur` = boussole + Changeur de Biome neuf (0,5 + 512,7 = 513,2), **validé par LeKiwi06 le
  02/11/2026** (« Oui, 513,2 ») et resté en attente depuis. Cible `pdc:ks_boussole:boussole_voyageur:b` (la boussole
  marquée par KS_Boussole, quel que soit le biome qu'elle suit). Barème : `custom(...)` dans `sortie/bareme/bareme.py`,
  note dans `remplir.py` (avec les objets ajoutés après la liste de LeKiwi06), cible dans `exporter_rachats.py`, ligne
  dans `ids-26.3-et-custom.csv` ; chaîne d'abord relancée sans changement (fichier identique), puis avec la boussole :
  `rachats.csv` gagne exactement cette ligne. Pas de dimension (comme le Changeur de Biome).
- Contient aussi les 9 prix de la 1.13.7 (machines de KS_SuperTool) : sans KS_SuperTool, ces lignes ne correspondent
  à aucun objet, rien ne casse.

Contient la 1.13.7. **Déployé sur Kixster le 20/11/2026 à 15:25** (LeKiwi06 : « déploie tout sur kixster et le proxy »), avec KS_SuperTool 1.0.0, KS_Economy 1.13.8, KS_Textures 1.2.4, KS_KaliumGive 1.14.0 et `didacticiel.yml` ; jars de la même taille que ceux du dépôt. À redémarrer par LeKiwi06. Statut : non testé en jeu.

## 1.13.9 - cultures de KS_Cultures (20/11/2026, LeKiwi06)

33 objets de KS_Cultures au barème, prix **validés par LeKiwi06 le 02/11/2026** (`sortie/cultures/
cultures-proposition.xlsx`, `KS_Cultures/AJOUTS_COMMUNS.md`). Chaque prix est la recette d'un objet du jeu
(`custom(..., ing=...)` dans `sortie/bareme/bareme.py`, liste `CULTURES_KS`) :

- tomate, poivron, aubergine, oignon, salade, riz = carotte (0,0293) ; kiwi = baies sucrées (0,0293) ; cerises = pomme
  (0,03984) ; épi de maïs = blé (0,04883) ;
- graines de légumes = graine de blé (0,01953) ; graines de fleurs = la moitié de la fleur (le composteur donne 2 graines
  en moyenne : 0,01019 pissenlit et coquelicot, 0,01992 petites fleurs, 0,02 oeilchidée) ; graines de grandes fleurs =
  1 colorant (grande fleur = 2 colorants : 0,0221 tournesol et rosier, 0,0332 lilas et pivoine) ;
- mélange de graines = 9 graines de légume (0,1758).

Cibles `pdc:ks_cultures:objet:s:<id>` (noms : `NOMS_CULTURES` dans `exporter_rachats.py`), **piles de 64** (sous
1 émeraude, un objet en pile de 1 n'est jamais tiré aux rachats) ; familles des rachats : graines de fleurs avec les
fleurs, le reste avec les cultures du jeu. Notes dans `remplir.py`, lignes dans `ids-26.3-et-custom.csv`. Chaîne
relancée : `rachats.csv` gagne exactement ces 33 lignes, rien d'autre ne change.

**Pas encore les grandes fleurs** (tournesol, rosier 0,0442 ; lilas, pivoine 0,0664) : à passer au barème au
déploiement de KS_Cultures, qui coupe leur duplication à la poudre d'os.

Statut : compilé, non déployé, non testé. À déployer avec KS_Textures 1.2.5. Sans KS_Cultures, ces lignes ne
correspondent à aucun objet : rien ne casse.
