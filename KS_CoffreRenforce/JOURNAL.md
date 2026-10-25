# KS_CoffreRenforce - journal

Plugin de Kixster (survie). Dépend de KLM_Menu (menus en coffre, textes). Recette dans KS_Crafts (1.21.0).

## 1.0.0 - coffre renforcé (25/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'aimerais créer ma propre version du drawer : le coffre renforcé. Il se craft avec un
coffre, 4 blocs de fer et 4 fragments de spawner, et permet de stocker jusqu'à 1 000 stacks du même item. J'aimerais
qu'on puisse le remplir et le vider par la redstone, on ne peut pas coller d'autres coffres à côté pour en faire un
double coffre et il peut servir de boutique. »

Choix de LeKiwi06 (questions du 25/10/2026, maquette du menu validée) :

| Question | Choix |
|---|---|
| Déposer / retirer à la main | Menu en coffre (pas de clic gauche façon drawer) |
| Casser un coffre plein | Refusé tant qu'il n'est pas vide |
| Recette | Coffre au centre, blocs de fer sur les côtés, fragments de spawner aux coins |
| Voir le contenu sans ouvrir | Texte au-dessus de la barre d'objets en le visant, avec le mot « stack » (pas « pile ») |
| Entonnoirs sur une boutique | Ni remplir ni vider ; une boutique de coffre renforcé se paie **seulement en points** (pas de troc) |
| Coffre vidé | Il se libère (accepte de nouveau n'importe quel objet) |
| Objets refusés | Shulkers et sacs qui contiennent quelque chose |
| Comparateur | Signal selon le remplissage (0 vide, 15 à 1 000 stacks) |
| Partie boutique (KS_Economy) | Après le coffre, une fois finie l'autre session qui modifiait KS_Economy |

Fonctionnement :

- **Objet** : un coffre nommé « Coffre renforcé » (marqueur `ks_coffrerenforce:coffre`). Vrai coffre vanilla, objet et
  bloc : visibles tels quels sur Bedrock, aucune entrée à ajouter dans `geyser-bedrock`.
- **Capacité** : 1 000 stacks de l'objet stocké (64 000 pour un objet par 64, 16 000 par 16, 1 000 non empilable),
  un seul objet exact (enchantements, nom compris).
- **Menu** (clic droit, 3 rangées) : l'objet stocké en haut avec sa quantité et ses stacks (vitre blanche « Vide ») ;
  « Retirer 1 », « Retirer 1 stack », « Remplir mon inventaire », « Tout déposer » (coffre vide : l'objet tenu en main
  choisit ce qu'il stocke) ; un clic sur un objet de son inventaire le dépose ; livre d'aide, « Fermer ». Messages
  au-dessus de la barre d'objets. Le menu ne s'ouvre que si le coffre aurait pu s'ouvrir (claims respectés), jamais
  pour un spectateur ; le vrai coffre ne s'ouvre jamais.
- **Texte visé** (toutes les demi-secondes, à 5 blocs, sans menu ouvert) : « Coffre renforcé : 12 345 x Diamant
  (193 / 1 000 stacks) » ou « vide (0 / 1 000 stacks) ».
- **Redstone** : les 27 cases du vrai coffre servent de tampon (une partie du stock). Après chaque passage d'entonnoir,
  le tampon est recalculé : le comparateur lit le remplissage du coffre entier (calcul du jeu 26.3 relu avec javap), et
  la place libre du tampon ne dépasse jamais la place restante (un entonnoir ne dépasse pas 1 000 stacks). Calcul
  vérifié pour chaque quantité de 0 à la capacité (objets par 64, par 16, non empilables) : aucune erreur. Un
  entonnoir n'y met que l'objet stocké (n'importe quel objet accepté si le coffre est vide) et jamais un objet refusé.
- **Jamais de coffre double** : un coffre posé contre un coffre renforcé (ou l'inverse) redevient simple au tick
  suivant ; on peut donc poser plusieurs coffres renforcés côte à côte (mon choix : ne pas refuser la pose, comme le
  fait KS_CoffreMort, pour permettre des murs de coffres renforcés).
- **Casser** : refusé s'il contient quelque chose (message) ; vide, il redonne un coffre renforcé (rien en créatif).
  Explosions, wither, dragon : il n'est pas détruit.
- **Stock** : gardé dans le bloc (données du coffre : objet + réserve), rien dans un fichier. KS_MondeTest n'a rien à
  couper : un coffre renforcé du monde de test reste dans ce monde (il ne se casse que vide).
- **API** pour les boutiques (KS_Economy, à faire) : `estCoffreRenforce`, `objet`, `nombre`, `retirer`, `ajouter`.

Limites :

- Un objet mis dans le tampon par autre chose qu'un entonnoir (golem de cuivre, autre plugin) et qui n'est pas l'objet
  stocké est rejeté au-dessus du coffre au prochain passage d'entonnoir ou à l'ouverture du menu (non vérifié en jeu).
- Le nom de l'objet vient du jeu, au singulier : « 12 345 x Diamant » (la maquette disait « Diamants »).
- **Pas encore de boutique** : la partie KS_Economy (panneau sur un coffre renforcé, paiement en points seulement,
  entonnoirs refusés) reste à faire.
- Didacticiel de Kixster à mettre à jour au déploiement.

**Déployé sur Kixster le 25/10/2026 à 03:51** avec KS_Crafts 1.21.0 et `didacticiel.yml` (LeKiwi06 : « Oui, déploie KS_CoffreRenforce 1.0.0 et KS_Crafts 1.21.0 sur Kixster, avec la mise à jour du didacticiel » ; nouveau jar de la même taille que celui compilé), actif après redémarrage de Kixster. Statut : non testé en jeu.
