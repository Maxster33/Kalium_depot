# KS_Elixir - journal

Plugin autonome, serveur Event. Élixirs. Cahier des charges : catégorie 1 « Contenu survie » de LeKiwi06 (validé le
29/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement) ; recettes d'origine dans la to do list du Kixster SMP.

## 1.0.0 - les 11 élixirs (30/09/2026)

Demande de LeKiwi06 : les élixirs custom de la to do list du Kixster SMP. Un élixir condense 8 potions en une seule
bouteille (pour ne pas boire sans arrêt) ; l'ingrédient rare du centre compense la place gagnée.

Recette en anneau : 8 potions **à boire**, version **allongée**, niveau I, autour de l'ingrédient du centre.

| Élixir (id) | 8 x | Centre | Effet | Durée |
|---|---|---|---|---|
| Super Gâteau (`super_gateau`) | gâteau | potion de soin I ou II | Saturation I | 30 min |
| Élixir de chauve-souris (`chauve_souris`) | vision nocturne allongée | éclat d'écho | Vision nocturne | 60 min |
| Élixir d'anguille (`anguille`) | respiration aquatique allongée | carapace de nautile | Respiration aquatique | 60 min |
| Élixir d'ignifugation (`ignifugation`) | résistance au feu allongée | glace bleue | Résistance au feu | 60 min |
| Élixir de Plume (`plume`) | chute lente allongée | carapace de shulker | Chute lente | 30 min |
| Élixir de Phénix (`phenix`) | régénération allongée | ancre de réapparition | Régénération I | 10 min |
| Élixir du Fantôme (`fantome`) | invisibilité allongée | fiole KS_FioleExp de 10 niveaux | Invisibilité | 60 min |
| Élixir de Titan (`titan`) | force allongée | bloc d'or | Force I | 60 min |
| Élixir du Vent (`vent`) | vitesse allongée | harnais de happy ghast (16 couleurs) | Vitesse I | 60 min |
| Élixir de Rebond (`rebond`) | saut amélioré allongé | bloc de slime | Saut amélioré **II** | 60 min |
| Élixir de Fortune (`fortune`) | chance (5 min) | bloc d'émeraude compressé tier 3 (KS_Economy) | Chance **III** | 10 min |

- **Élixir** : potion à boire, particules masquées, **aspect enchanté**, couleur de la potion vanilla de l'effet
  (Super Gâteau : rose `16738740`), nom coloré non italique (couleurs du cahier), marqueur `ks_elixir:elixir` = id.
- Potions ingrédients : potions vanilla exactes (une potion renommée ne marche pas) ; ni jetables, ni persistantes.
- **Livre de recettes** : une recette est débloquée dès que le joueur obtient un de ses ingrédients (potion acceptée
  ou objet du centre), comme dans KS_Crafts.
- **Alambic** (choix de LeKiwi06, 30/09/2026) : un élixir ne peut pas y être mis (clic, Maj+clic, touches 1-9, main
  secondaire, glisser, entonnoirs) ; filet de sécurité : un brassage avec un élixir est annulé. Sinon la poudre à
  canon en ferait un élixir jetable (et persistant avec le souffle de dragon).
- **Élixir du Fantôme** : fiole de KS_FioleExp de 10 niveaux exactement (`softdepend`, `build.sh` : compiler
  KS_FioleExp d'abord ; sans lui, recette ignorée avec un avertissement). L'endermite la donne déjà (KS_LootEntites).
- **Élixir de Fortune** : le bloc d'émeraude compressé tier 3 viendra de KS_Economy (catégorie 2, pas encore créé) ;
  **recette ignorée** pour l'instant (avertissement dans la console). L'élixir existe déjà (`/kaliumgive`). En vanilla,
  la Chance n'agit que sur les tables de butin qui en tiennent compte (pêche surtout) : sujet reporté à KS_FairPlay
  (catégorie 5).
- **Autres plugins** : `creerElixir(id)`, `idElixir(objet)`, `ids()` (KS_KaliumGive 1.6.0).

**Déployé sur Event le 30/09/2026 à 18:07 (LeKiwi06) avec KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0,
KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0 et KS_LootEntites 1.3.0 (nouveau), actifs après redémarrage d'Event. Statut :
**testé et confirmé par LeKiwi06 le 30/09/2026** (« tout est bon »).**

## 1.1.0 - recette de l'Élixir de Fortune (30/09/2026)

Suite de la catégorie 2 (KS_Economy existe) : 8 potions de chance autour d'un **bloc d'émeraude compressé tier 3**
(9 000 points) de KS_Economy. `softdepend` et `build.sh` : KS_Economy (compiler d'abord) ; sans lui, recette ignorée
(avertissement). Livre de recettes : débloquée dès l'obtention d'une potion de chance ou d'un bloc tier 3.

**Non déployé. À déployer ensemble (règle 3.5) : KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 et l'installation de
VaultUnlocked 2.20.2 sur Event. Statut : non testé en jeu.**
