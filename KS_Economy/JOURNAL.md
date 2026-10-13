# KS_Economy - journal

Plugin du serveur Event : économie. Cahier des charges : catégorie 2 « Économie » de LeKiwi06 (validé le 29/09/2026 ;
copié dans `CAHIER_DES_CHARGES.md` au déploiement). Codé en 2 temps (réponse de LeKiwi06) : score, blocs, `/echange`
d'abord ; magasins (Tradeshop) après KS_Claim (catégorie 3).

## 1.0.0 - score, blocs compressés, /echange, Vault (30/09/2026)

- **Score** : 1 émeraude = 1 point. Taux réglables dans `config.yml` (valeurs par défaut dans le code) :
  `conversion.depot` (points reçus par émeraude déposée) et `conversion.retrait` (points payés par émeraude retirée),
  1 = 1:1. Dépôt arrondi à l'unité inférieure, coût du retrait à l'unité supérieure. Comptes dans
  `plugins/KS_Economy/comptes.yml` (enregistrés dans la minute qui suit un changement et à l'arrêt).
- **Menu « Économie »** (`/economie`, `/eco` ou bouton du menu d'Event, KS_Menu) : solde ;
  - **Tout déposer** : émeraudes, blocs d'émeraude et blocs compressés de l'inventaire (hors armure et main secondaire) ;
  - **Déposer des objets** : coffre où poser ce qu'on dépose ; à la fermeture, les émeraudes sont converties, le reste
    est rendu ;
  - **Retirer** : émeraude, bloc d'émeraude ou bloc compressé tier 1 à 6, et nombre ; refusé si le solde ou la place
    manquent ;
  - **Masquer / Révéler mon solde**.
- **Liste des joueurs (Tab)** : « Pseudo 1 234 » (solde en vert), visible par tous ; masqué : pseudo seul. Solde masqué :
  **1 %** prélevé chaque jour au premier passage après minuit (heure de Paris), arrondi à l'unité inférieure, rien si le
  solde est nul (`masquage.pourcent-par-jour`). Serveur éteint à minuit : prélèvement au démarrage suivant, une seule
  fois. Révéler est gratuit et immédiat.
- **Blocs d'émeraude compressés** : tier n = 10^n blocs d'émeraude = 9 x 10^n points (90 à 9 000 000). Bloc d'émeraude à
  aspect enchanté, nom « Bloc d'émeraude compressé (tier n) », valeur en description, marqueur `ks_economy:tier`.
  Obtenus seulement par le retrait. **Impossibles à poser** et **à décrafter** (aucune recette vanilla ne les accepte,
  établi et autocrafteur ; les recettes des plugins, oui : Élixir de Fortune).
- **/echange** `<joueur>` (puis `accepter` / `refuser`, message cliquable ; demande valable 60 s) : 2 joueurs à 5 blocs
  au plus. **Panneau d'échange** (coffre de 6 rangées) : à gauche sa part, à droite celle de l'autre (aperçu des objets,
  montant, validation). Boutons « Objets » (coffre de 16 cases où déposer ses objets), « Montant » (saisie, limitée au
  solde), « Valider », « Annuler ». L'échange a lieu quand les deux ont validé ; ouvrir « Objets » ou « Montant » et
  toute modification retirent les deux validations. Annulé, objets rendus, si un joueur s'éloigne à plus de 5 blocs,
  ferme le panneau, se déconnecte, meurt (objets au sol), reste plus de 2 minutes dans la saisie du montant, ou à
  l'arrêt du serveur. Refusé (validations retirées) si un solde ne suffit plus ou si un inventaire est trop plein pour
  recevoir les objets. Chaque échange est noté dans la console.
- **Vault** (nécessaire techniquement, signalé dans le cahier ; VaultUnlocked 2.20.2, téléchargé avec l'accord de
  LeKiwi06) : KS_Economy est déclaré comme économie Vault (points entiers, pas de banques), pour SimpleClaimSystem
  (catégorie 3). Sans Vault : avertissement, le reste fonctionne.
- **Autres plugins** : `solde(uuid)`, `crediter(uuid, points)`, `debiter(uuid, points)`, `creerBloc(tier)`,
  `tierBloc(objet)` (KS_Elixir 1.1.0).
- Textes : `plugins/KS_Economy/lang.yml` (créé au premier démarrage). `depend` KLM_Menu, `softdepend` KS_Menu, Vault ;
  `build.sh` : compiler KLM_Menu et KS_Menu d'abord ; `telecharger-outils.sh` télécharge VaultUnlocked.

Limites :
- Pas de commande d'administration des soldes (pas demandée).
- Un joueur peut toujours faire `/menu off` : le menu Économie reste ouvrable par `/economie`.

**Déployé sur Event le 01/10/2026 à 00:49 (LeKiwi06) avec KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 et
VaultUnlocked 2.20.2 (nouveau plugin tiers, `plugins/VaultUnlocked-2.20.2.jar`), actifs après redémarrage
d'Event. Statut : non testé en jeu.**

## 1.0.1 - textes des boutons du menu Économie (01/10/2026)

Bug vu par LeKiwi06 au premier test : les boutons du menu Économie affichaient « MemorySection[path='menu.tout-deposer',
... » au lieu de leur texte. Cause : dans `lang.yml`, une clé (`menu.tout-deposer`) et sa description
(`menu.tout-deposer.info`) : la 2e transforme la 1re en section YAML. 9 clés concernées (boutons du menu Économie et
du panneau d'échange). Correctif : nouveaux noms (`menu.bouton-tout-deposer`, `menu.info-tout-deposer`,
`echange.bouton-objets`...) ; les anciennes clés restent dans le `lang.yml` du serveur, sans effet.

**Déployé sur Event le 01/10/2026 à 01:33 (1.0.0 dans `_removed-ks_economy-1.0.0/`), actif après redémarrage d'Event.
Statut : non testé en jeu.**

## 1.0.2 - plus aucun texte qui défile (01/10/2026)

Demande de LeKiwi06 : des textes trop longs « défilent » dans les boutons et les champs ; « il ne faut jamais que ça
arrive, c'est illisible ». Largeur de chaque texte de bouton et de champ mesurée (police de Minecraft) : seule la liste
du retrait débordait (« Objet (valeur en émeraudes): Bloc compressé tier 3 (9 000) », ~304 pixels pour un champ de 260).
- Liste du retrait : « Objet » et « Émeraude », « Bloc d'émeraude », « Compressé tier 1 » à « Compressé tier 6 »
  (120 pixels au plus) ; les valeurs en points sont écrites dans le texte du menu (qui passe à la ligne).
- Titre du coffre de dépôt raccourci : « Déposer des émeraudes ».
- Nouvelles clés de `lang.yml` (`retrait.champ-objet`, `retrait.nom-…`, `retrait.valeurs`, `depot.titre-coffre`) : le
  `lang.yml` du serveur garde les anciens textes sous les anciennes clés, qui ne servent plus.
- Boutons vérifiés : tous à moins de 110 pixels pour 240 (« Déposer des objets » : 102).

**Déployé sur Event le 01/10/2026 à 18:54 (1.0.1 dans `_removed-ks_economy-1.0.1/`), actif après redémarrage d'Event.
Statut : non testé en jeu.**

## 1.0.3 - saisie du montant ajustée (01/10/2026)

Suite de 1.0.2 (voir KLM_Menu 2.5.0) : la fenêtre de saisie du montant de `/echange`, construite à la main, ajuste
aussi ses boutons avec `Lisible`. Le reste des menus passe par `Gui`, ajusté par KLM_Menu 2.5.0.

**Déployé le 01/10/2026 à 23:42 (LeKiwi06) : KLM_Menu 2.5.0 sur les 6 serveurs Paper (2.4.0 dans
`_removed-klm_menu-2.4.0/` sur lobby, Kal-Games, Serveur Jeux, Kanvas ; 2.4.1 dans `_removed-klm_menu-2.4.1/` sur
Kixster et Event), KalGames 1.22.1 (`_removed-kalgames-1.22.0/`), KG_BingoGame 0.8.4 (`_removed-kg_bingogame-0.8.3/`),
KS_Economy 1.0.3 (`_removed-ks_economy-1.0.2/`) ; actifs après redémarrage de chaque serveur. Statut : **testé et confirmé par LeKiwi06 le 02/10/2026** (menus : plus aucun texte qui défile).**

## 1.1.0 - magasins et boutiques (02/10/2026, LeKiwi06)

2e partie de la catégorie 2. Décisions de LeKiwi06 (02/10/2026, partie « Magasins » du cahier révisée) : Tradeshop
abandonné (l'original n'est plus mis à jour depuis 2023), nos propres boutiques ; une région WorldGuard `zone_shop` pour
tous les shops, où l'on peut claimer et où tout le monde construit ; prix en objet ou en monnaie.

- **Magasin** (1 par joueur) : `/magasin create` (ou le menu) dans un de ses claims de la zone (centre du chunk dans la
  région) : **45 000 points** ou **5 blocs compressés tier 3** ; 10 boutiques. `/magasin agrandir` dans un autre de ses
  claims de la zone : **10 000 points**, le claim rejoint le magasin, +1 boutique. `/magasin position` (dans un claim du
  magasin). `/magasin` : menu (mes boutiques, nom et description (32 / 100 caractères, nom unique), position, agrandir).
- **Boutique** : poser un panneau contre (ou sur) un coffre, coffre piégé, tonneau ou shulker d'un claim de son magasin
  ouvre « Créer une boutique » / « Fermer » (au lieu de l'éditeur du panneau). Objet vendu : « Écrire le nom » (nom
  français ou id du jeu, sans majuscules ni accents ; plusieurs résultats : liste, 20 au plus ; noms tirés du fichier
  de langue fr_fr de Minecraft 26.2, `noms_objets.txt`) ou « Choisir dans l'inventaire » (objets custom compris) ;
  quantité (1 à 2 304) ; prix : « Monnaie (points) » ou « Objet » (mêmes choix + quantité) ; récapitulatif, « Créer ».
  Panneau : « [Troc] », le lot, « contre », le prix ; ciré (non modifiable). Plusieurs panneaux sur un même contenant :
  plusieurs boutiques, même stock (coffre double compris).
- **Achat** : clic droit sur le panneau (ou depuis le catalogue, à distance) : offre, stock en lots, nombre de lots.
  Refusé si le stock, le paiement (objets de l'inventaire ou solde), la place de l'acheteur ou la place du contenant
  manquent. Objets payés : dans le contenant ; points payés : **en attente dans la boutique**. Chaque achat est noté dans
  la console.
- **Propriétaire** : clic sur son panneau ou « Mes boutiques » : offre et stock, points en attente (« Récupérer les
  points »), « Gérer le stock » (à distance : copie du contenu, recopiée à la fermeture ; boutique **verrouillée** pendant
  ce temps (ni achat, ni ouverture sur place) ; chunk chargé), « Supprimer la boutique » (le panneau redevient ordinaire,
  points en attente rendus).
- **Catalogue** : bouton « Magasins » du menu Économie : tous les magasins, puis leurs boutiques (offre, prix, stock) et
  l'achat à distance ; « Mon magasin ».
- **Protections** : panneau et contenant d'une boutique : cassés seulement par le propriétaire (la boutique est alors
  supprimée), ni explosions, ni pistons, ni entonnoirs.
- **KS_Claim 1.1.0** (à déployer ensemble) : un claim qui porte des boutiques ne peut être ni supprimé, ni vendu, ni
  acheté ; supprimé ou vendu sans boutique, il quitte le magasin. `refusClaim(proprio, chunk)`, `claimRetire(proprio,
  chunk)`.
- Données : `plugins/KS_Economy/magasins.yml`. Réglages : section `magasins` de `config.yml` (région, prix, nombre de
  boutiques ; valeurs par défaut dans le code : le `config.yml` déjà présent sur Event n'a pas ces clés, à ajouter à la
  main seulement pour les modifier). `softdepend` WorldGuard, SimpleClaimSystem (sans eux : pas de magasin).

Limites :
- Un claim de la zone qui n'est pas dans un magasin reste un claim ordinaire (on peut y poser des panneaux normaux).
- Les points en attente d'une boutique sont rendus au propriétaire si elle est supprimée.

**Déployé sur Event le 03/10/2026 à 00:02 (LeKiwi06) avec KS_Economy 1.1.0 et KS_Claim 1.1.0 (1.0.3 dans
`_removed-ks_economy-1.0.3/`), actifs après redémarrage d'Event ; région `zone_shop` à créer par l'humain. Statut : non
testé en jeu.**

## 1.1.1 - API de SimpleClaimSystem initialisée (03/10/2026, LeKiwi06)

Signalé par LeKiwi06 : `/magasin` et la création de boutiques dans un claim : rien ne s'ouvrait, sans message (console : « API not initialized. Call initialize() first. »).
SimpleClaimSystem 1.13.1 ne crée pas son API lui-même : le plugin l'initialise maintenant avant chaque utilisation
(`SimpleClaimSystemAPI_Provider.initialize`, sans effet si c'est déjà fait). Aucun autre changement.

**Déployé sur Event le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (ancienne version dans `_removed-ks_economy-1.1.0/`), actif après redémarrage. Statut : non testé en jeu.**

## 1.1.2 - boutiques : noms français, état, nom, fermeture, délai de 3 h, coffres en cuivre (03/10/2026, LeKiwi06)

Retours de LeKiwi06 après les premiers essais (cahier mis à jour : « Retours de LeKiwi06 (03/10/2026) ») :
- **Recherche d'objet** (« bloc de diamant », « diamond_block ») : ne trouvait rien, `noms_objets.txt` n'était pas
  copié dans le jar par `build.sh` ; corrigé.
- **Noms français** écrits par le plugin sur les panneaux et dans les menus (avant : nom traduit par le client, en
  anglais sur les panneaux).
- **Panneau** en 4 lignes mesurées (le jeu ne gardait que les premiers mots d'une ligne trop longue ; sinon coupée
  avec « … », 60 px pour un panneau suspendu) : nom de la boutique, lot, « pour » + prix, état (« Stock : N lots »,
  « Rupture de stock », « Coffre plein », « Fermée », « Hors service »). Mis à jour après un achat, une gestion à
  distance, la fermeture du contenant sur place, au chargement du chunk et au démarrage (anciens panneaux compris).
- **Nom de boutique** (20 caractères) : à la création (nom de l'objet vendu par défaut), « Renommer » ; listes (mes
  boutiques, catalogue) par nom, coloré selon l'état, avec l'état écrit et les points à récupérer.
- **Fermer temporairement / Rouvrir** : aucun achat, panneau « Fermée ».
- **Suppression depuis le menu** : le panneau est retiré et rendu au propriétaire (avant : panneau ordinaire laissé).
- **Délai de 3 h** (`magasins.delai-suppression-heures`, nouveau, 3 par défaut) : la place d'une boutique supprimée
  (menu, panneau ou contenant cassé) reste prise ; message avec le temps restant ; affiché dans le menu du magasin.
  Enregistré dans `magasins.yml` (`suppressions`).
- **Coffres en cuivre** (tous états, cirés ou non) acceptés comme contenants.
- Recherche des boutiques d'un contenant limitée aux panneaux voisins : ne charge plus le chunk de toutes les boutiques.
- Nouvelles clés de langue pour les textes changés (`magasin.aide-2`, `magasin.supprimer-texte-2`,
  `boutique.supprimee-2`) ; les anciennes restent dans les `lang.yml` des serveurs sans servir.

Limite : un golem de cuivre du propriétaire, près d'une boutique en coffre de cuivre, peut en déplacer le stock (la
protection des claims de SimpleClaimSystem s'applique aux golems des autres).

**Déployé sur Event le 03/10/2026 à 01:49 (LeKiwi06 ; 1.1.1 dans `_removed-ks_economy-1.1.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.3 - catalogue sans son magasin, pas d'achat chez soi, « Voir l'objet », signalements (03/10/2026, LeKiwi06)

Demande de LeKiwi06 (cahier : « Demande de LeKiwi06 (03/10/2026, KS_Economy 1.1.3) ») :
- **Catalogue** : son propre magasin n'apparaît plus. **Pas d'achat dans sa propre boutique** (stats des magasins) :
  le propriétaire arrive sur la gestion ; achat refusé sinon.
- **« Voir l'objet »** (menu d'achat) : coffre en lecture seule : objet vendu tel quel, résumé, objet demandé (ou
  émeraude « N points ») ; refermé : retour au menu d'achat.
- **Signalements** : « Signaler la boutique » (menu d'achat), « Signaler le magasin » (fiche dans le catalogue) ;
  raisons à cocher (arnaque, contenu inapproprié, thème non respecté) + « Autre » ; 1 non traité par joueur et par
  cible ; message au staff connecté (`kseconomy.staff`, nouvelle permission, opérateurs par défaut) et console ;
  `plugins/KS_Economy/signalements.yml`. `/magasin signalements` (staff) : non traités / classés, détail, se
  téléporter, supprimer la boutique (sans délai de 3 h), classer avec l'action faite.

**Déployé sur Event le 03/10/2026 à 02:35 (LeKiwi06 ; ancienne version dans `_removed-ks_economy-1.1.2/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.4 - signalements dans la rubrique « Modération » de /menu (03/10/2026, LeKiwi06)

« Signalements des magasins » déclaré comme outil de modération (KLM_Menu 2.7.0, permission `kseconomy.staff`) :
ouvert depuis `/menu` → « Modération », avec « Retour » vers la rubrique. `/magasin signalements` reste.
**Nécessite KLM_Menu 2.7.0** (avec 2.6.0, le bouton irait dans « Paramètres »).

**Déployé sur Event le 03/10/2026 à 03:22 (LeKiwi06 ; 1.1.3 dans `_removed-ks_economy-1.1.3/`) avec KLM_Menu 2.7.0, actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.5 - signal de fin d'échange (03/10/2026, LeKiwi06)

Catégorie 6 (anti-triche, revente suspecte) : `fr.kalium.economy.api.EchangeTermineEvent` (joueurs A et B, objets et
points donnés par chacun) quand un `/echange` aboutit. Aucun autre changement.

**Déployé sur Event le 03/10/2026 à 06:45 (LeKiwi06 ; 1.1.4 dans `_removed-ks_economy-1.1.4/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.0 - ventes, boutique à un seul acheteur, classement, favoris, recherche (03/10/2026, LeKiwi06)

Revue du 03/10/2026, points validés par LeKiwi06 :
- **Score enregistré à chaque transaction** (au tick suivant le crédit ou le débit, regroupé ; avant : une fois par
  minute, un crash pouvait défaire un paiement alors que les objets étaient livrés).
- **Une boutique, un acheteur à la fois** (« si un joueur est en train d'utiliser une boutique, personne ne puisse le
  faire en même temps ») : menu d'achat, « Voir l'objet » et achat réservent la boutique (son contenant, donc toutes
  ses boutiques) au joueur ; libérée après l'achat, à la déconnexion ou après 30 s sans action ; sinon « Quelqu'un
  utilise cette boutique en ce moment : réessaie dans un instant. ».
- **Notification de vente** au propriétaire : message s'il est connecté ; sinon résumé à sa connexion (5 dernières,
  50 gardées).
- **Statistiques** : lots vendus cette semaine et au total, nombre de ventes, points et objets reçus : dans le menu
  du magasin et dans la gestion de chaque boutique ; lots de la semaine sur la fiche publique d'un magasin.
  `ventes.yml` (20 000 dernières ventes).
- **Catalogue** : magasins classés par lots vendus ces 7 derniers jours (rang et ventes de la semaine) ; boutons
  « Rechercher un objet » et « Favoris (n) ».
- **Favoris** : « Ajouter aux favoris » / « Retirer des favoris » sur la fiche d'un magasin et dans le menu d'achat ;
  page Favoris (magasins et boutiques ; ceux qui n'existent plus sont retirés). `favoris.yml`.
- **Recherche** : nom français ou id (facultatif), catégorie (objets spéciaux, potions et élixirs, armures, outils et
  armes, nourriture, minerais et ressources, redstone, blocs, autres), prix (tous, en points, en objet), en stock
  seulement, tri (plus vendus, moins cher, plus cher, nom ; prix par objet, en points) ; boutiques des autres
  joueurs ; 10 par page, « Modifier la recherche ».

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.2.0 ; ancienne version dans `_removed-ks_economy-1.1.5/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.3.0 - barème des prix et rachats de la semaine (04/10/2026, LeKiwi06)

Demande de LeKiwi06 : « enregistrer cette base de données dans le plugin d'économie, et fait une interface où on tire
10 items random par semaine et on les achète aux joueurs ».

- **Barème** : `rachats.csv` (dans le jar), 1 929 objets (vanilla 26.3 et objets custom) : id, famille, prix en
  émeraudes à l'unité, façon de reconnaître l'objet, nom. Autres plugins : `KSEconomy.prixBareme(id)`. Le fichier est
  produit par le script du barème (hors dépôt) : pour changer un prix, régénérer le fichier puis recompiler.
- **Tirage** chaque lundi (heure de Paris), ou au premier démarrage qui suit : 2 objets dans chacune des 5 gammes de
  prix (moins de 0,1 ; 0,1 à 1 ; 1 à 10 ; 10 à 100 ; 100 et plus). On tire d'abord une **famille** (179 : diorite,
  cuivre, fleur, terres, sapin, potion, améthyste, boue, terres cuites, tête...), puis un objet de cette famille dans
  la gamme demandée : les variantes d'un objet comptent pour un seul. Une famille ne sort deux fois dans la semaine
  que s'il n'en reste pas d'autre.
- **Dimensions fermées** (LeKiwi06 : « il ne faut pas tirer d'item issu de dimension qui sont fermées dans KS dimension ») :
  quand KS_Dimensions désactive les portails du Nether ou de l'End, aucun objet issu de cette dimension n'est tiré
  (état lu au moment du tirage ; un tirage déjà fait ne change pas). Chaque objet du barème porte sa dimension
  d'origine : matières premières marquées à la main (netherrack, quartz, bâton de blaze, pierre de l'End,
  carapace de shulker, élytres...), têtes selon la créature, objets fabriqués selon leurs ingrédients (comparateur
  = quartz = Nether ; boîte de shulker = End et Nether, par l'œil de l'Ender du coffre de l'Ender).
- **Prix de la semaine** : prix du barème à plus ou moins 25 %, arrondi à l'unité dès 1 émeraude.
- **Lot** : selon la gamme du prix de la semaine, la quantité (arrondie à l'unité inférieure) qui approche 1 / 10 /
  64 / 320 émeraudes, payée au nombre entier le plus proche (un diamant tiré à 3 : 21 pour 63 ; à 4 : 16 pour 64 ;
  à 5 : 12 pour 60). À l'unité à partir de 100 émeraudes et pour un objet non empilable.
- **Quota** par objet, par joueur et par jour (minuit, heure de Paris) : aucun tant que la cagnotte ne dépasse pas
  25 000 ; au-dessus, un objet ne rapporte pas plus de 5 % de la cagnotte (7,5 % si elle est masquée). La vente qui
  fait dépasser passe, les suivantes sont refusées jusqu'au lendemain. Le quota suit la cagnotte pendant la vente.
- **Menu** : `/rachat` (ou `/rachats`) et bouton « Rachats de la semaine » du menu Économie : les 10 lots (quantité,
  objet, points, ce que le joueur possède) ; par objet : « Vendre 1 lot », « Tout vendre ». Les points sont crédités
  sur le score (créés par le serveur).
- Objets acceptés : objet vanilla sans marque de plugin, sans enchantement, sans usure, conteneur vide (un nom donné
  à l'enclume est accepté) ; potion de base de l'effet demandé ; objet custom reconnu par la marque de son plugin
  (têtes, élixirs, spawners, fioles d'expérience de 10 / 15 / 20 / 30 / 40 niveaux, Estomac du gardien...). Seul
  l'inventaire compte (ni armure, ni main secondaire).
- **Dans la base mais jamais tirés** (choix de Claude, signalés à LeKiwi06) : émeraude et blocs d'émeraude
  (monnaie) ; jetons (ils s'achètent en points : les racheter jusqu'à 25 % plus cher créerait des points à volonté) ;
  potion / potion jetable / persistante / flèche à effet sans effet précisé, livres enchantés, cartes au trésor, livre
  écrit, soupe suspecte, tête de wither squelette abîmée (prix variable selon le contenu) ; objet non empilable de
  moins de 1 émeraude (il serait payé 1 émeraude pièce, bien au-dessus de son prix) ; objets de la 26.3 que le serveur
  ne connaît pas encore.
- `rachats.yml` : tirage de la semaine et gains du jour. Réglages (`config.yml`, section `rachats`, à ajouter à la
  main sur le serveur pour les modifier) : objets par gamme, écart du prix, seuil et parts du quota.
- Limites connues : pas de commande pour refaire le tirage (supprimer `rachats.yml` serveur éteint) ; les lots de
  têtes demandent des têtes identiques ; l'œuf de dragon peut sortir (une seule vente possible, environ 150 000).

**Déployé sur Event le 04/10/2026 à 22:01 (LeKiwi06 ; 1.2.0 dans `_removed-ks_economy-1.2.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.3.1 - région du spawn : `zone_spawn` (08/10/2026, LeKiwi06)

Décision de LeKiwi06 (08/10/2026) : « la région zone_shop est maintenant zone_spawn, qui permet de créer maisons et
magasins ». Valeur par défaut de `magasins.region` : `zone_spawn` (code et `config.yml` du jar). Aucun autre changement.
- **Au déploiement** : la région WorldGuard doit s'appeler `zone_spawn` (à renommer ou recréer par l'humain, avec
  `build allow` et `scs-claim allow`) ; si le `config.yml` du serveur contient `magasins.region: zone_shop`, le changer.
  Tant que la région et le réglage ne portent pas le même nom, aucun magasin ne peut être créé ni agrandi.
- Non changé : `rachats.csv` (`jeton_emplacement` à retirer, valeurs des nouveaux jetons et des badges à fixer).

**Déployé sur Event le 08/10/2026 à 23:38 (LeKiwi06, déploiement groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; 1.3.0 dans
`_removed-ks_economy-1.3.0/` ; supprimables par l'humain : `_removed-ks_economy-1.0.0/` à `1.1.5/`, 10 dossiers), actif
après redémarrage d'Event. Le `config.yml` du serveur n'a pas de section `magasins` : la région lue est donc
`zone_spawn` (valeur du code) ; **région WorldGuard à renommer ou recréer par l'humain**. Statut : non testé en jeu.**

## 1.4.0 - rachats : 10 objets au hasard par familles, plus de gamme de prix (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « modifier le tirage des items pour les rachats de la semaine, il faudrait tirer 10 items
aléatoires en gardant le système de "famille" mais sans restriction de 2 par ordre de prix », et « des items issus du
Nether ont été tirés alors qu'il est fermé, ce n'est pas normal ».

- **Tirage** : 10 familles au hasard parmi toutes (174 ; 155 Nether fermé), puis un objet au hasard dans chaque
  famille, quel que soit son prix. Une famille ne sort deux fois que s'il n'en reste pas d'autre. Le réglage
  `rachats.objets-par-gamme` n'est plus lu ; nouveau réglage `rachats.objets` (10 par défaut dans le code, à ajouter à
  la main dans le `config.yml` du serveur pour le changer).
- Conséquence, mesurée sur 20 000 tirages simulés avec le barème actuel : en moyenne par semaine 4,7 objets à moins
  de 0,1 émeraude, 3 entre 0,1 et 1, 1 entre 1 et 10, 0,6 entre 10 et 100, 0,7 à 100 et plus (avant : 2 de chaque).
- Inchangés : prix de la semaine (barème à plus ou moins 25 %), taille des lots (1 / 10 / 64 / 320 émeraudes selon le
  prix de la semaine), quota, objets jamais tirés.
- **Dimension fermée** : cause non établie (les fichiers d'Event n'ont pas pu être lus pendant la session). Relu :
  le filtre du tirage et la colonne « dimension » de `rachats.csv` (208 objets marqués Nether, matières premières
  vérifiées une à une) ; aucune erreur trouvée. Seul cas trouvé dans le code : le tirage ne regardait KS_Dimensions
  qu'au moment du tirage (lundi, ou premier démarrage), donc un Nether fermé ensuite laissait ses objets proposés
  jusqu'au dimanche. Corrigé (choix de Claude, signalé) : à chaque vérification (toutes les minutes, au démarrage et à
  l'ouverture du menu), une offre dont la dimension est fermée est retirée et remplacée par un nouveau tirage (ligne
  « objets d'une dimension fermée remplacés » dans la console). Rouvrir la dimension ne fait pas revenir l'offre.
- Le tirage en cours (semaine 41) n'est pas refait par la mise à jour : seuls les objets d'une dimension fermée sont
  remplacés. Le nouveau tirage s'applique lundi 12/10, ou en supprimant `rachats.yml` serveur éteint (l'humain).
- Limite connue : si les objets vus par LeKiwi06 ne sont pas marqués « nether » dans `rachats.csv`, ou si KS_Dimensions
  n'est pas le plugin qui ferme le Nether sur ce serveur, cette version ne corrige pas le problème : relever les objets
  concernés et la ligne « Rachats de la semaine ... » de la console.

**Déployé sur Event le 09/10/2026 à 05:52 (LeKiwi06 ; 1.3.1 dans `_removed-ks_economy-1.3.1/`, jar en place vérifié identique à `jars-deployes/` avant l'envoi ; `config.yml` du serveur non modifié ; supprimables par l'humain : `_removed-ks_economy-1.0.0/` à `1.2.0/`, 11 dossiers), actif après redémarrage d'Event. Statut : non testé en jeu.**

**Journal d'Event lu le 09/10/2026 après le redémarrage de 06:01 (accord de LeKiwi06)** : KS_Economy 1.4.0 actif,
« Barème : 1929 objets (0 inconnus de cette version) » (les objets de la 26.3 sont reconnus), aucune erreur.
**Cause des objets du Nether** : sur Event, KS_Dimensions indique « Portails du Nether : activés ; portails de l'End :
activés » à chaque démarrage depuis le 01/10 (43 journaux, aucune ligne « désactivés par ... », `config.yml` de
KS_Dimensions inchangé depuis le 30/09). Pour le plugin, aucune dimension n'est fermée : le tirage de la semaine 41
(05/10 à 12:38) contient donc `dragon_breath` (End) et `red_shulker_box` (End + Nether). Le filtre n'est pas en cause.
Tant que le Nether n'est pas fermé dans `/dimensions`, ses objets peuvent sortir ; dès qu'il l'est, la 1.4.0 remplace
ces offres dans la minute. À préciser par LeKiwi06 : par quel moyen le Nether est fermé sur Event.

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 1.4.0, la version d'Event (1.3.1 pour la région `zone_spawn` + 1.4.0 pour les rachats par familles) ; 1.3.0 dans `_removed-ks_economy-1.3.0/` ; le `config.yml` du serveur n'a pas de section `magasins` : la région lue devient `zone_spawn` (à créer par l'humain)), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**

## 1.4.1 - lecture publique pour la modération (09/10/2026, LeKiwi06)

Pour la fiche d'un joueur de KS_AntiCheat 1.3.0 (demande de LeKiwi06 : solde, magasin, dernières ventes dans la fiche
de modération). Aucun changement pour les joueurs.
- `KSEconomy.resume(joueur, lignes)` renvoie un `fr.kalium.economy.api.ResumeJoueur` : solde, solde masqué ou non,
  magasin (nom, position), nombre de boutiques, points en attente dans les boutiques, ventes de ses boutiques (nombre,
  lots des 7 derniers jours, points gagnés), dernières ventes et derniers achats. Lecture seule, en ligne ou hors ligne.

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

**Déployé sur Kixster le 09/10/2026 à 18:31 (LeKiwi06, envoi groupé : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 ; accord de LeKiwi06 : « Oui, sur Kixster » ; 1.4.0 dans `_removed-ks_economy-1.4.0/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (KS_Economy 1.4.0). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage de 18:36 (09/10/2026) : activé sans erreur, avec KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ; KS_AntiCheat démarre après eux. Personne n'avait encore ouvert la fiche d'un joueur : reste à tester en jeu.**

## 1.5.0 - menus en coffres (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage d'interfaces
de type contenant quand c'est possible au lieu des boutons », puis, après son test de `/rewards` : « ça marche, passe
aux écrans de l'économie ». Les 28 écrans du plugin étaient des fenêtres de dialogue ; ils deviennent des coffres
(classe `Contenant` de **KLM_Menu 2.11.0, à déployer avec**). Aucune règle du jeu ne change : mêmes prix, mêmes
contrôles, mêmes fichiers.

Modèle commun (nouvelle classe `Menus`) : objets cliquables en haut, barre d'actions en bas sur vitres grises (flèches
de page aux deux bouts, livre d'aide, action principale au centre, porte « Retour » ou barrière « Fermer » à sa
droite). Les messages courts (« Solde insuffisant », « Acheté : 2 lots »...) s'affichent au-dessus de la barre d'objets
du joueur, avec un son, et l'écran reste ouvert ; sans menu ouvert (commande tapée, clic sur un panneau) ils sont aussi
écrits dans le tchat. Les confirmations sont un petit coffre : béton vert « Confirmer », béton rouge « Annuler ».

- **Économie** (`/economie`) : solde sur un bloc d'émeraude ; tout déposer, déposer des objets, retirer, magasins,
  rachats, masquer / révéler.
- **Retirer** : les 8 objets (émeraude, bloc, compressés tier 1 à 6) avec leur coût, puis le nombre d'un clic : 1, 8,
  64, « le maximum » (selon le solde et la place), ou « Autre nombre » (saisie).
- **Rachats de la semaine** : un objet par rachat (la pile montre le lot), avec le prix et ce qu'on possède ; un clic
  ouvre la vente : « Vendre 1 lot », « Tout vendre ».
- **Mon magasin** (`/magasin`) : fiche sur la tête du joueur, mes boutiques, nom et description, position, agrandir ;
  bouton « Magasins » vers le catalogue. Création : payer en points ou en blocs tier 3, d'un clic.
- **Mes boutiques** : une boutique par case (l'objet vendu, nom coloré selon l'état, offre, état, points à récupérer),
  45 par page. **Ma boutique** : stock, renommer, fermer / rouvrir, récupérer les points, supprimer.
- **Catalogue** : un magasin par case (tête du propriétaire, rang, ventes de la semaine, description), 45 par page ;
  dans la barre : rechercher, favoris, mon magasin. **Magasin d'un joueur** : ses boutiques ; dans la barre : sa fiche,
  favori (étoile), signaler. **Favoris** : magasins et boutiques dans le même coffre.
- **Créer une boutique** (panneau posé sur un contenant) : « Créer une boutique » ou « Non, un panneau ordinaire » ;
  l'objet se choisit en cliquant dans une copie de son inventaire (ou « Écrire le nom » ; plusieurs objets trouvés :
  un coffre pour choisir) ; quantité par lot : 1 à 64 d'un clic ou « Autre quantité » ; prix : monnaie ou objet ;
  récapitulatif avec l'objet vendu, le prix, le nom de la boutique (modifiable) et « Créer la boutique ».
- **Acheter dans une boutique** : à gauche l'objet exact vendu (nom, enchantements, description), au centre l'offre et
  l'état, à droite ce qu'on paie ; « Acheter 1 / 5 / 10 lots » d'un clic, ou « Autre nombre de lots » ; favori et
  signalement dans la barre. Le coffre « Voir l'objet » de la 1.1.3 disparaît : l'écran d'achat le remplace. Ouvert
  depuis une liste, « Retour » y ramène ; ouvert depuis le panneau, « Fermer ».
- **Rechercher un objet** : un seul coffre, résultats en haut (36 par page), filtres dessous : nom de l'objet (saisie),
  catégorie (un coffre de 10 objets), prix, en stock seulement, tri ; un clic change le filtre et la liste suit.
- **Signaler** : une teinture par raison (verte : cochée), « Autre » (saisie), « Envoyer ». **Signalements du staff** :
  un signalement par case, fiche avec la boutique signalée, se téléporter, supprimer la boutique, classer (d'un clic,
  ou avec une note saisie).

Restent des fenêtres de dialogue, ouvertes depuis une case (un coffre ne sait pas faire de saisie) : nombre à retirer,
nom et description du magasin, nom d'une boutique, nom d'un objet, quantité ou nombre de lots libre, prix en points,
texte « Autre » d'un signalement, note de classement, et leurs messages d'erreur de saisie. Le coffre de dépôt et le
stock d'une boutique restent de vrais coffres.

À savoir :
- Les textes déjà dans `lang.yml` sont repris tels quels ; les nouveaux ont de nouvelles clés (`eco.*`, `contenant.*`,
  `*-coffre`, `*-info`...). Les anciennes clés des fenêtres supprimées restent dans `lang.yml`, sans effet.
- Une description longue est coupée en lignes d'environ 35 caractères : seule sa première couleur est gardée.
- Tête d'un magasin : celle du propriétaire ; un joueur Bedrock ou jamais vu par le serveur a une tête ordinaire.
- Catalogue, mes boutiques et signalements passent de 10 à 45 par page ; la recherche de 10 à 36.
- Bedrock : entre un coffre et une fenêtre de saisie, le coffre est refermé et la fenêtre s'ouvre un quart de seconde
  plus tard (délai choisi par Claude, à vérifier en jeu).

**Déployé le 09/10/2026 à 19:09 sur Event (à la place de la 1.4.0) et Kixster (à la place de la 1.4.1), avec KLM_Menu 2.11.0 (accord de LeKiwi06 : « oui déploie » ; jars en place vérifiés identiques à `jars-deployes/` ; anciens jars dans `_removed-ks_economy-1.4.0/` et `_removed-ks_economy-1.4.1/`). Actif après redémarrage. Statut : non testé en jeu
(aucun des 28 écrans n'a été ouvert ; seule la coupe des descriptions a été essayée hors serveur).** La 1.5.0 part de
la 1.4.1 (Kixster, non testée) : elle garde `KSEconomy.resume` pour KS_AntiCheat 1.3.0. Event est en 1.4.0.

## 1.5.1 - rachats : les objets custom montrent leur vrai objet (09/10/2026, LeKiwi06)

Signalement de LeKiwi06 : « les items dans le rachat de la semaine sont buggué quand c'est des items custom on voit
une nether star ».

- **Cause** : depuis la 1.5.0 (menus en coffres), l'objet d'un rachat était l'objet du jeu pour un objet vanilla, la
  potion pour une potion, et une étoile du Nether pour **tout** objet custom du barème (361 lignes : 330 têtes,
  11 élixirs, 8 spawners, 5 fioles d'expérience, Estomac du gardien, Clé de l'End, Bedrock Breaker, Changeur de
  Biome, Fragment et Cœur de Spawner, bloc de charbon de bois). Écrit ainsi en 1.5.0, pas une panne.
- **Correction** : l'objet custom est créé par KS_KaliumGive (`KSKaliumGive.creer(id)`, les id du barème sont ceux de
  `/kaliumgive` : relu ligne par ligne contre la liste de KS_KaliumGive), puis habillé comme les autres (nom
  « N x objet », prix, « Tu en as »).
  Liste des rachats et écran « Vendre au serveur ».
- Le bloc de charbon de bois (KS_Crafts) n'est pas dans KS_KaliumGive : il est montré par un bloc de charbon (c'est
  son apparence en jeu).
- Choix de Claude, signalés :
  - l'objet du menu ne porte **pas** la marque de son plugin (retirée) : c'est une image, pas un vrai objet custom ;
  - appel par réflexion, sans `softdepend` : KS_KaliumGive dépend de KS_Jetons et KS_Elixir, qui dépendent de
    KS_Economy ; une dépendance dans l'autre sens ferait une boucle au chargement des plugins.
- Limite : sans KS_KaliumGive (ou si le plugin de l'objet est désactivé), l'étoile du Nether reste. KS_KaliumGive est
  sur Event et Kixster (1.9.0).
- Aucun autre changement : reconnaissance des objets à la vente, prix, tirage, quota inchangés. Pas de nouveau texte.

**Compilé le 09/10/2026 (`sortie/KS_Economy-1.5.1.jar`). Jamais déployé seul : contenu dans la 1.5.2, déployée le 11/10/2026. Statut : non testé en jeu.**

## 1.5.2 - barème : familles de la terre des âmes et du sable des âmes (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « ajoute la terre des ames aux terres , et le sables des ames aux sables » (rachats de la semaine
et coffres de récompense : « Les deux »).

- `rachats.csv` : la terre des âmes (`soul_soil`) passe dans la famille « terres » (terre, terre stérile, terre
  racinaire, podzol, bloc d'herbe, mycélium) et le sable des âmes (`soul_sand`) dans la famille « sable » (sable, sable
  rouge). La famille « sable des âmes » n'existe plus : 178 familles au lieu de 179. Seules ces 2 lignes changent.
- Effet au tirage des rachats : ces deux blocs ne forment plus une famille à eux seuls ; ils sont tirés quand la
  famille « terres » ou « sable » sort, parmi ses objets. Ils restent marqués « nether » : jamais tirés quand
  KS_Dimensions ferme le Nether (le filtre porte sur l'objet, avant le choix de la famille).
- Prix (0,04322 émeraude l'unité), reconnaissance à la vente, lots, quota : inchangés. Aucun changement de code.
- Les rachats déjà tirés pour la semaine en cours ne sont pas refaits.
- Contient la 1.5.1 (objets custom des rachats), jamais déployée : empilé sur une version non testée, à la demande de
  LeKiwi06.

**Déployé sur Event et Kixster le 11/10/2026 à 06:05 (LeKiwi06 ; 1.5.0 dans `_removed-ks_economy-1.5.0/`), actif après redémarrage. Statut : non testé en jeu.**

## 1.6.0 - /accepter, plusieurs demandes d'échange, menu de choix (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faut pouvoir faire /accepter pour accepter une demande d'échanger , c'est plus court , si le
joueur à reçu des demandes d'échange de plusieurs joueurs , ouvrir un menu cliquable pour choisir quelle demande
accepter ».

- **`/accepter`** (nouvelle commande) : accepte la demande d'échange reçue, comme `/echange accepter`.
- **Plusieurs demandes** : avant, une nouvelle demande effaçait la précédente (une seule gardée par joueur).
  Maintenant un joueur garde une demande par demandeur, chacune valable 60 secondes.
  - Une seule demande valable : `/accepter` l'accepte.
  - Des demandes de plusieurs joueurs : `/accepter` ouvre le menu « Demandes d'échange » (coffre), une tête par
    demandeur à son nom ; un clic accepte sa demande. Une demande expirée entre-temps : « Cette demande d'échange n'est
    plus valable. »
- Les boutons « [Accepter] » et « [Refuser] » du message nomment maintenant le demandeur : ils répondent à **sa**
  demande, même s'il y en a d'autres.
- Techniquement nécessaire, signalé : `/accepter <joueur>`, `/echange accepter <joueur>` et `/echange refuser <joueur>`
  (ce que lancent les boutons) ; `/echange refuser` sans nom refuse **toutes** les demandes reçues (avant : la seule).
- Le reste est inchangé : distance de 5 blocs, échange déjà en cours, panneau d'échange.
- Nouveaux textes : `echange.choix-titre`, `echange.choix-nom`, `echange.choix-clic`, `echange.expiree` (ajoutés à
  `lang.yml` au premier usage).

Limites : sur Bedrock, la tête d'un joueur peut s'afficher en tête de Steve dans le menu (son nom reste lisible).
`/accepter` ne sert qu'aux échanges : les demandes d'ami et les invitations de partie gardent leurs commandes
(`/amis accepter`, `/partie accepter`). Contient les 1.5.1 et 1.5.2, déployées le 11/10/2026 et non testées en jeu.

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; 1.5.2 dans `_removed-ks_economy-1.5.2/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.5.2). Statut : non testé en jeu.**

## 1.7.0 - rachats du marchand ambulant (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai que le wandering trader propose aussi 10 échanges , les moins cher ont echangeable
jusqu'a 10 fois , puis plus ça vaut moins on peux échanger , un item comme le diamant serait échangeable 5 fois par
exemple , un item comme le totem ne le serait qu'en 1 exemplaire : les règles de trades sont les mêmes que pour les
rachats de la semaine concernant les items tirés , leurs quantité , et leurs prix ». Ses réponses aux trois questions
de Claude : **le marchand achète** (le joueur donne l'objet et reçoit des émeraudes) ; **menu en coffre, score** ;
**un tirage par marchand, stock commun** à tous les joueurs.

- **Clic droit sur un marchand ambulant** : menu « Marchand ambulant » (coffre), ses 10 rachats en deux rangées de 5,
  comme les rachats de la semaine : la pile montre le lot, la description donne le prix du lot, « Encore n fois » (ou
  « Épuisé »), ce que le joueur possède. Un clic vend **1 lot** : objets retirés de l'inventaire, points versés sur le
  score. La fenêtre d'échange du jeu ne s'ouvre pas (le marchand n'a aucun échange : KS_Villageois).
- **Tirage** : celui des rachats de la semaine, refait pour chaque marchand à son premier clic : 10 familles au
  hasard, un objet par famille, jamais un objet d'une dimension fermée, prix du barème à plus ou moins 25 %, lot de la
  gamme du prix (1, 10, 64 ou 320 émeraudes ; à l'unité à partir de 100 ou pour un objet non empilable). Les objets
  custom du barème (têtes, élixirs...) peuvent sortir, comme pour les rachats.
- **Nombre de fois** par rachat, pour tous les joueurs ensemble, selon le prix à l'unité : moins de 0,1 : **10** ;
  0,1 à 1 : **10** ; 1 à 10 : **5** (le diamant) ; 10 à 100 : **2** ; 100 et plus : **1** (le totem). Réglable :
  `marchand.echanges` du `config.yml` (et `marchand.objets`, 10) ; valeurs par défaut dans le code.
- **Gardé sur le marchand lui-même** (ses données) : les rachats et ce qu'il en reste disparaissent avec lui et
  survivent à un redémarrage tant qu'il est là. Aucun fichier.
- Le joueur doit rester à 8 blocs du marchand ; s'il est parti (ou mort) : « Le marchand ambulant n'est plus là. »
- **Pas de quota du jour** ici (le quota des rachats de la semaine ne s'applique pas) : le stock du marchand fait la
  limite. Le tirage d'un marchand est écrit dans la console, comme chaque vente (« marchand ambulant »).
- Un marchand qui porte l'étiquette `ks_pnj` (PNJ à échanges prévu par KS_Villageois) n'est pas concerné.
- Technique : le tirage des rachats de la semaine sert aux deux (même code) ; rien ne change pour `/rachat`.
- Nouveaux textes : clés `marchand.*` de `lang.yml` (ajoutées au premier usage).

Choix de Claude, signalés :
- les paliers 0,1 à 1 (10 fois) et 10 à 100 (2 fois) : LeKiwi06 n'a donné que 10 pour les moins chers, 5 pour le
  diamant et 1 pour le totem ;
- un clic = 1 lot (pas de « Tout vendre ») ;
- le tirage se fait au premier clic sur le marchand, pas à son apparition (même résultat pour les joueurs).

À savoir : un marchand peut verser beaucoup si les joueurs ont les objets. Par rachat, au plus : 10 émeraudes (moins
de 0,1), 100 (0,1 à 1), 320 (1 à 10), 640 (10 à 100), le prix de l'objet au-delà. Les marchands ambulants sont rares
(le jeu en fait apparaître un de temps en temps près d'un joueur, pour 40 minutes).

Contient la 1.6.0 (déployée sur Kixster le 11/10/2026, non testée en jeu). Empilé à la demande de LeKiwi06. Écrit sans
serveur de test.

**Déployé sur Kixster le 11/10/2026 à 09:00 (LeKiwi06, envoi groupé : KS_Meule 1.1.0, KS_KaliumGive 1.10.0, KS_Economy 1.7.0, KS_Tableau 1.0.1 et `fusion.yml` de KS_RewardsGUI ; accord de LeKiwi06 : « oui déploie tout » ; 1.6.0 dans `_removed-ks_economy-1.6.0/`), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.5.2). Statut : non testé en jeu.**

## 1.7.1 - barème : prix corrigés après les premiers rachats (12/10/2026, LeKiwi06)

Signalement de LeKiwi06 : « le cinabre est bien trop boosté, il vaut autant que du fer ». Cause : le cinabre et le
soufre n'avaient jamais reçu de prix validé ; leur note provisoire (« bloc récent : note à vérifier en jeu »), jamais
vérifiée, donnait 0,0764 émeraude par la formule (0,051, puis × 1,5 « non automatisable »), contre 0,10 pour le lingot
de fer ; avec le prix de la semaine à plus ou moins 25 %, les deux se croisaient.

Seul `rachats.csv` change (113 prix sur 1 929 ; aucun changement de code, familles inchangées). Décisions de LeKiwi06 :
- **Cinabre et soufre : 0,01**, le prix de la pierre taillée (« une fois dans le biome, on en a en abondance, et vu la
  taille de la carte ça aura le temps de ruiner l'économie avant qu'on épuise la ressource ») ; leurs variantes suivent
  par leurs recettes (dalles 0,005). Pointe de soufre, soufre concentré et seau de cube de soufre : inchangés.
- **Butin de monstres et poissons divisé par 2** : crème de magma 0,0845 ; boule de slime 0,0685 ; poudre à canon
  0,0635 ; œil d'araignée 0,049 ; poisson tropical et poisson-globe 0,045 ; ficelle 0,034 ; os 0,0295. Les objets
  fabriqués suivent (TNT 0,39 ; bloc de slime 0,62 ; poudre d'os 0,0098 ; arc 0,11 ; laisse 0,085...).
- **Disque Bounce : 83,9**, comme le disque Relic (« dans les mineshafts des sulfur caves, aussi rare que le disque
  relic je pense ») ; avant : 22,8.
- **Cartes de structure (15) : 10** (« pas de valeur réelle, mais mets-les à 10 pour la valeur estimée ») ; avant : 2.
  Elles restent jamais tirées. Carte au trésor enfoui : 30, inchangée.
- **Cuivre brut : 0,015**, le prix du lingot (« le cuivre en lingot se trouve en grande quantité dans les trial
  chambers, donc le prix de cuisson est négligeable ») ; avant : 0,005. Bloc de cuivre brut 0,135 ; minerai de cuivre
  0,06 (avant 0,02). Lingot et objets en cuivre : inchangés.
- Coussins et `straw_bed` (26.3) : prix de leur recette, inchangés (recettes comptées : 2 laines ; 3 bottes de foin +
  3 planches).

À savoir :
- Les rachats déjà tirés (semaine en cours, marchands ambulants déjà ouverts) gardent leur prix et leur lot : ils sont
  enregistrés dans `rachats.yml` et dans les données du marchand. Les nouveaux prix s'appliquent au tirage suivant, ou
  en supprimant `rachats.yml` serveur éteint (l'humain), ce qui refait tout le tirage de la semaine.
- Effet de bord signalé, non corrigé : la poudre d'os (0,0098) vaut maintenant moins que le colorant blanc (0,0221)
  qu'elle fabrique à 1 pour 1.
- Les tables de butin des coffres de récompense (`butin.yml`, hors dépôt) sont calculées avec ces prix : non
  régénérées.
- Autres anomalies listées à LeKiwi06 le 12/10 (œufs, objets qui se multiplient à la poudre d'os, blocs abondants,
  capteur sculk...) : prix en attente de sa décision, inchangés ici.
- Contient la 1.7.0 (déployée le 11/10/2026, non testée en jeu) : seul le barème change.

**Compilé le 12/10/2026, jamais déployé : remplacé par la 1.8.0, qui le contient.**

## 1.8.0 - barème : suite des prix corrigés ; une offre au prix modifié est remplacée (12/10/2026, LeKiwi06)

**Barème** (`rachats.csv` ; par rapport au barème en service, celui des versions 1.3.0 à 1.7.0 : 678 prix changent sur
1 929, dont 659 objets tirables ; aucun ne monte depuis la 1.7.1). Réponse de LeKiwi06 à la liste d'anomalies du 12/10 :
« catégorie 1 à 4 et le reste de la 6 validé », plus ses corrections :
- **Erreurs franches** : œuf brun et œuf bleu au prix de l'œuf ; pousses, propagule et champignons du Nether à la
  moitié de leur bûche ou tige (règle « graine ≤ fruit », comme les feuilles) ; capteur sculk 0,10 (avant 1,32),
  hurleur 0,30 (3,80), catalyseur 1 (2,53) ; mousse pâle au prix de la mousse.
- **Revenu passif** : œuf 0,01 (avant 0,031 ; brun et bleu 0,043) ; écaille de tatou et rayon de miel 0,03 ; cactus,
  citrouille, laine blanche 0,0125 (graines de citrouille : le quart, 0,0031).
- **Se multiplie seul ou à la poudre d'os** : mycélium et podzol 0,02 ; nyliums, racines, germes et lianes du Nether,
  blocs de verrues, herbes marines 0,015 ; champilampe, cornichon de mer, grande foliogoutte, buisson à lucioles, baies
  lumineuses, lichen lumineux 0,03 ; œilchidées 0,04 ; coraux et éventails vivants 0,03 ; blocs de corail vivants 0,05.
- **Blocs abondants une fois leur biome trouvé : 0,01** : coraux morts, mousse, boue, sable, sable rouge, gravier,
  spéléothème, tuf, basalte, bloc d'herbe, pierre de l'End, ardoise taillée, terre stérile, granite, diorite, andésite,
  sable et terre des âmes, bloc de magma, terre cuite, pierre noire. Calcite, terre racineuse, blocs et pied de
  champignon 0,015. Poudre de glowstone 0,02 (bloc 0,08). Les objets fabriqués suivent (verre 0,02 ; béton blanc
  0,036 ; grès 0,04...).
- **Colorant blanc** au prix de la poudre d'os (0,0098 ; « mets le colorant blanc au même prix que la poudre d'os »).
- **Coussin** : 1,5 laine (0,019 en blanc) ; **`straw_bed`** : 0,75 botte de foin (0,33) ; **carte au trésor enfoui :
  10** (avant 30).
- Validés tels quels (reste de la catégorie 6) : pointe de soufre 0,135 ; soufre concentré 0,50 ; seau de cube de soufre
  0,56 ; `red_shrub` et `shelf_mushroom` 0,060.

Écarts par rapport aux nombres de la liste, signalés à LeKiwi06 :
- fiole de miel : 0,05 et non 0,06 (somme de ses ingrédients : la fiole en verre a baissé avec le sable) ;
- bloc de corail vivant : 0,05 comme validé, donc moins que les 4 coraux de son craft KS_Crafts (0,12) ;
- perle de l'Ender et endermite (une perle lancée rapporte en moyenne plus que son prix, par l'améthyste bourgeonnante
  à 250) : rien de changé, la proposition n'était pas un prix ;
- le silex (0,029) vaut maintenant 2,9 fois le gravier (0,01) qui le donne : non corrigé.

**Offre au prix modifié** (demande de LeKiwi06 : « il faut refaire un tirage d'économie pour les items qui ont été
modifiés, on tire un nouvel objet à la place ») :
- chaque offre des rachats de la semaine enregistre le prix du barème au moment du tirage (`offres.N.bareme` dans
  `rachats.yml`) ; à chaque vérification (démarrage, toutes les minutes, ouverture du menu), une offre dont le prix du
  barème n'est plus le même est retirée et remplacée par un nouveau tirage (mêmes règles : familles, dimensions
  ouvertes), jamais par l'objet retiré. Console : « Rachats de la semaine ... : objets au prix modifié remplacés
  (ids) ». Les autres offres ne bougent pas ; les gains du jour (quota) non plus.
- Offres tirées avant la 1.8.0 (pas de prix enregistré) : remplacées si l'objet est dans `rachats-modifies.txt` (dans le
  jar : les 678 objets dont le prix a changé par rapport au barème des versions 1.3.0 à 1.7.0), ou si le prix de la
  semaine sort de l'écart permis autour du prix actuel ; les autres reçoivent le prix actuel.
- `build.sh` copie `rachats-modifies.txt` dans le jar.

Limites :
- Les rachats d'un marchand ambulant déjà ouvert gardent leur prix (ses données ne portent pas le prix du barème ; un
  marchand reste 40 minutes).
- Les tables de butin des coffres de récompense (`butin.yml`, hors dépôt) sont calculées avec ces prix : non régénérées.
- Remplacement écrit sans serveur de test : la règle de détection a été simulée sur les deux barèmes, pas en jeu.
- Contient la 1.7.1 (jamais déployée) et la 1.7.0 (déployée le 11/10/2026, non testée en jeu) : empilé à la demande de
  LeKiwi06.

**Déployé sur Kixster et Event le 12/10/2026 à 01:17 (LeKiwi06 ; accord : « oui déploie tout » ; jar en place vérifié identique à `jars-deployes/` sur les deux serveurs avant l'envoi ; 1.7.0 dans `_removed-ks_economy-1.7.0/` ; supprimables par l'humain : sur Kixster `_removed-ks_economy-1.3.0/`, `1.4.0/`, `1.4.1/`, `1.5.0/`, `1.5.2/` ; sur Event `_removed-ks_economy-1.0.0/` à `1.5.0/`, 15 dossiers), actif après redémarrage de chaque serveur. Statut : non testé en jeu.**

Offres de la semaine 42 qui seront remplacées au redémarrage (objets au prix modifié) : sur Kixster `waxed_oxidized_chiseled_copper`, `dripstone_block`, `brown_egg`, `cinnabar_brick_wall`, `diorite_wall` ; sur Event `straw_bed`, `string`, `raw_copper` (ligne « objets au prix modifié remplacés » de la console).

Décision de LeKiwi06 le même jour : la perle de l'Ender reste à 0,5 (« ils ne savent pas pour l'endermite, donc aucune raison que la valeur de la perle grimpe »).

Ventes de cinabre avant la correction (Kixster, 12/10 de 00:09 à 00:30, `cinnabar_brick_wall` à 10 pour 1 point ; journal du serveur et `rachats.yml`) : 316 points pour 3 160 murets, 3 joueurs. Demande de LeKiwi06 : « retire aussi les points obtenus via le cinabre » (pas de commande d'administration des soldes : `comptes.yml` modifié serveur éteint).

**Fait le 12/10/2026 à 01:24, Kixster arrêté (LeKiwi06 : « kixster est arrêté, retire tout »)** : 316 points retirés de `plugins/KS_Economy/comptes.yml` (3 soldes : - 250, - 57, - 9 ; aucun n'est passé sous zéro ; montants de `rachats.yml` recoupés avec le journal du serveur ; les murets vendus ne sont pas rendus) ; ancien fichier dans `/plugins/_removed-ks_economy-comptes-2026-10-12/` ; fichier relu identique après l'envoi. Kixster était arrêté depuis 00:33 : aucune vente de cinabre après le relevé.

**Journal de Kixster lu le 12/10/2026 après le redémarrage de 01:25 (demande de LeKiwi06)** : KS_Economy 1.8.0 active, « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur. Ligne attendue présente : « Rachats de la semaine 2026-S42 : objets au prix modifié remplacés (waxed_oxidized_chiseled_copper, dripstone_block, brown_egg, cinnabar_brick_wall, diorite_wall) » ; les 5 autres offres sont gardées, les 5 nouvelles sont `golden_apple`, `potion(tissage)`, `ladder`, `mushroom_stem`, `deepslate_lapis_ore`. `rachats.yml` : les 10 offres portent leur prix du barème. `comptes.yml` : soldes corrigés conservés après le redémarrage. Statut : vérifié dans la console, non testé en jeu. Event : pas encore redémarré.

## 1.8.1 - barème : pied de champignon doublé (12/10/2026, LeKiwi06)

Demande de LeKiwi06 : « les troncs de champignons sont trop peu chers, ils sont pénibles à avoir (plus que les blocs
marron et rouge), il faut donc doubler le prix des tiges de champignon ».

- `rachats.csv` : pied de champignon (`mushroom_stem`) 0,03 au lieu de 0,015. Seule cette ligne change ; blocs de
  champignon marron et rouge inchangés (0,015). Aucun changement de code.
- Effet au déploiement : une offre de la semaine sur le pied de champignon (tirée sur Kixster le 12/10 à 01:25, 64 pour
  1 point) est remplacée par un nouveau tirage au redémarrage, comme tout objet au prix modifié (1.8.0).
- `rachats-modifies.txt` inchangé (il ne sert qu'aux offres tirées avant la 1.8.0).
- Contient la 1.8.0 (déployée le 12/10/2026, vérifiée dans la console de Kixster, non testée en jeu).

**Déployé sur Kixster et Event le 12/10/2026 à 01:32 (LeKiwi06 ; accord : « oui déploie tout » ; jar en place vérifié identique à `jars-deployes/` sur les deux serveurs avant l'envoi ; 1.8.0 dans `_removed-ks_economy-1.8.0/` ; supprimables par l'humain : sur Kixster `_removed-ks_economy-1.3.0/` à `1.6.0/`, 6 dossiers ; sur Event `_removed-ks_economy-1.0.0/` à `1.5.2/`, 16 dossiers), actif après redémarrage de chaque serveur. Event n'avait pas redémarré depuis le 11/10 à 17:36 : il passera directement de la 1.7.0 à la 1.8.1. Statut : non testé en jeu.**

**Journal de Kixster lu le 12/10/2026 après le redémarrage de 01:33** : KS_Economy 1.8.1 active, « Barème : 1929
objets (0 inconnus de cette version) », aucune erreur ; « Rachats de la semaine 2026-S42 : objets au prix modifié
remplacés (mushroom_stem) ». Statut : vérifié dans la console, non testé en jeu. Event : pas encore redémarré.

**Journal d'Event lu le 12/10/2026 à 02:42, après le redémarrage de 01:42 (heure de Paris ; 23:42 dans le journal, en UTC)** : KS_Economy 1.8.1 active (passage direct depuis la 1.7.0), « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur ; « Rachats de la semaine 2026-S42 : objets au prix modifié remplacés (straw_bed, string, raw_copper) » ; les 7 autres offres sont gardées, les 3 nouvelles sont `cinnabar_brick_stairs` (121 pour 1 point, au nouveau prix), `verdant_froglight`, `crimson_fence_gate`. `rachats.yml` : les 10 offres portent leur prix du barème. Statut : vérifié dans la console des deux serveurs, non testé en jeu.

## 1.8.2 - quota des rachats : compteur des points gagnés, dès 2 500, 0,5 % par objet (12/10/2026, LeKiwi06)

Demande de LeKiwi06 : « fais en sorte que le quota débute à 2500 gagné, pas 25000, et que le quota par item soit de
0,5 % de notre monnaie gagnée, et 0,75 % avec le solde masqué », puis : « oui il faut mettre un compteur d'argent
gagné, sinon les joueurs vont juste retirer les émeraudes en items pour outrepasser le quota ».

- **Compteur « gagné »** par compte (`gagne` dans `comptes.yml`) : il ne baisse jamais (ni retrait d'émeraudes, ni
  achat, ni échange, ni prélèvement du solde masqué). Il monte seulement avec les **ventes au serveur** : rachats de la
  semaine et rachats du marchand ambulant (choix de LeKiwi06 : « Ventes au serveur » ; les dépôts d'émeraudes, les
  échanges et les ventes en boutique ne comptent pas, pour qu'il ne puisse pas être gonflé entre joueurs).
- **Départ** (choix de LeKiwi06 : « Du solde actuel ») : au premier démarrage en 1.8.2, le compteur de chaque compte
  existant prend la valeur de son solde ; un compte créé ensuite part de 0.
- **Quota** par objet, par joueur et par jour : aucun tant que le compteur ne dépasse pas `rachats.quota.seuil`,
  2 500 (avant : 25 000 de solde) ; au-dessus, un objet ne rapporte pas plus de `rachats.quota.pourcent`, 0,5 % du
  compteur (avant : 5 % du solde), ou `rachats.quota.pourcent-masque`, 0,75 %, si le solde est masqué (avant 7,5 %).
  La vente qui fait dépasser passe, les suivantes sont refusées jusqu'à minuit ; le quota suit le compteur pendant la
  vente. Valeurs par défaut du code et du `config.yml` du jar ; le `config.yml` de Kixster et celui d'Event n'ont pas
  de section `rachats` (lus le 12/10) : ce sont ces valeurs qui s'appliquent.
- Exemples (points par objet et par jour ; solde masqué entre parenthèses) : 2 500 gagnés : aucun quota ; 2 600 :
  13 (19) ; 5 000 : 25 (37) ; 10 000 : 50 (75) ; 25 000 : 125 (187).
- **Textes** (nouvelles clés de `lang.yml`, les anciennes restent sur le serveur sans effet) : `rachats.sans-quota-gains`
  « Aucun quota avant 2 500 points gagnés en rachats (toi : 745). » et `rachats.quota-gains` « Quota du jour : 13 points
  par objet (0,5 % de tes 2 600 points gagnés en rachats). ».
- Autres plugins : `KSEconomy.gagne(uuid)`.
- Les rachats du marchand ambulant n'ont toujours pas de quota (son stock fait la limite), mais ils alimentent le
  compteur.
- Limite : au départ, le compteur vaut le solde, donc le texte « gagnés en rachats » n'est pas exact pour les comptes
  existants tant qu'ils n'ont rien vendu.
- Contient la 1.8.1 (déployée le 12/10/2026, vérifiée dans la console, non testée en jeu). Aucun changement du barème.
  Écrit sans serveur de test.

**Déployé sur Kixster et Event le 12/10/2026 à 05:13 (LeKiwi06 ; accord : « oui déploie tout » ; jar en place vérifié identique à `jars-deployes/` sur les deux serveurs avant l'envoi ; 1.8.1 dans `_removed-ks_economy-1.8.1/` ; supprimables par l'humain : sur Kixster `_removed-ks_economy-1.3.0/` à `1.7.0/`, 7 dossiers ; sur Event `_removed-ks_economy-1.0.0/` à `1.7.0/`, 17 dossiers), actif après redémarrage de chaque serveur. Au premier démarrage : `comptes.yml` reçoit la ligne `gagne` de chaque compte (égale au solde), écrite dans la minute. Statut : non testé en jeu.**

**Journaux de Kixster lus le 13/10/2026 à 05:32 (demande de LeKiwi06)** : KS_Economy 1.8.2 y est active depuis le redémarrage du 12/10 à 05:15 (4 démarrages depuis, le dernier le 13/10 à 05:32), « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur. `comptes.yml` : les 8 comptes portent la ligne `gagne`. Compteur vérifié sur les ventes du journal : une vente de 312 points aux rachats de la semaine (12/10, 13:40) a monté le solde et le compteur du vendeur de 745 à 1 057 ; des ventes au marchand ambulant (13/10, 03:29) sont comptées aussi ; un compte dont le solde a monté sans vente a un compteur resté plus bas (421 pour 455). Personne ne dépasse 2 500 gagnés : le quota lui-même n'a encore servi à personne. Statut : vérifié dans la console et les fichiers, non testé en jeu. Event : à lire après son redémarrage.

## 1.9.0 - magasin : création à 450 points, 3 agrandissements, tiers (13/10/2026, LeKiwi06)

Demande de LeKiwi06 : « le prix du magasin create est de 45000, il doit passer à 450 ; le prix du magasin agrandir est
calculé tel que x est le nombre d'agrandissement : 10x²+450x-50 ; le nombre maximum d'agrandissement est de 3 ; le
premier accorde 2 slots de boutique supplémentaires, le deuxième 3, le troisième 5 ; on doit normalement arriver à un
magasin tier 4 avec 20 slots de boutique ; on ne peut créer qu'une boutique par joueur ».

- **Création** : **450 points** (avant : 45 000), `magasins.prix-creation`. Paiement en blocs (choix de LeKiwi06 :
  « 5 blocs tier 1 ») : **5 blocs d'émeraude compressés tier 1** (= 450 points ; avant : 5 blocs tier 3),
  `magasins.blocs-tier1-creation` (l'ancienne clé `blocs-tier3-creation` n'est plus lue).
- **Agrandissements** : **3 au plus**. Le x-ième coûte **10x² + 450x - 50 points** (formule dans le code ; l'ancienne
  clé `prix-agrandissement` n'est plus lue) et ajoute les boutiques de `magasins.boutiques-par-agrandissement`
  (`[2, 3, 5]` ; le nombre de valeurs est le maximum d'agrandissements) :

  | Tier | Agrandissement | Prix | Boutiques |
  |---|---|---|---|
  | 1 | création | 450 | 10 |
  | 2 | 1er | 410 | 12 |
  | 3 | 2e | 890 | 15 |
  | 4 | 3e | 1 390 | 20 |

  Au tier 4, « Agrandir ici » et `/magasin agrandir` répondent que le magasin est au tier maximal.
- **Non changé** : un agrandissement se fait toujours dans un autre de ses claims de la zone, qui rejoint le magasin
  (la demande ne parle que du prix, du nombre et des boutiques). **Un magasin par joueur** : déjà le cas depuis la
  1.1.0 (« Tu as déjà un magasin (un seul par joueur). »), relu dans le code, rien à changer.
- Choix de Claude, signalé : le **tier** est affiché (fiche du magasin « Tier 1 / 4 », bouton et confirmation de
  l'agrandissement « tier 2, 410 points, +2 boutiques »), puisque les textes de l'agrandissement devaient être réécrits.
- **Textes** (nouvelles clés de `lang.yml`, les anciennes restent sur le serveur sans effet) : `magasin.aide-creer-tier1`,
  `magasin.payer-blocs-tier1`, `magasin.pas-assez-blocs-tier1`, `magasin.fiche-tier`, `magasin.agrandir-info-tier`,
  `magasin.agrandir-max`, `magasin.aide-boutique`, `magasin.aide-agrandir`, `magasin.agrandir-complet`,
  `magasin.agrandir-texte-tier`, `magasin.agrandi-tier`, `boutique.limite-max`.
- **Serveurs** (fichiers lus le 13/10/2026, hors dépôt) : le `config.yml` de Kixster et celui d'Event n'ont pas de
  section `magasins` : les nouvelles valeurs du code s'appliquent sans rien modifier à la main. Kixster : aucun
  magasin. Event : un magasin, sans agrandissement (il reste à 10 boutiques, tier 1).
- Un magasin qui aurait plus de 3 agrandissements (ancienne règle, aucun cas sur les serveurs) est compté au tier 4.
- Écrit sans serveur de test. Empilé sur la 1.8.2, non testée en jeu (demande de LeKiwi06).

**Déployé sur Kixster le 13/10/2026 à 16:30 (LeKiwi06, envoi groupé : KS_Economy 1.9.0, KS_Teleport 1.3.0, KS_Claim 1.4.0 ; accord de LeKiwi06 : « Kixster seulement » ; jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, jars envoyés vérifiés identiques à ceux compilés après l'envoi ; 1.8.2 dans `_removed-ks_economy-1.8.2/` ; supprimables par l'humain : `_removed-ks_economy-1.3.0/`, `1.4.0/`, `1.4.1/`, `1.5.0/`, `1.5.2/`, `1.6.0/`, `1.7.0/`, `1.8.0/`, 8 dossiers ; `config.yml` du serveur non modifié (pas de section `magasins`)), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.8.2). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage du 13/10/2026 à 16:36 (demande de LeKiwi06) : KS_Economy 1.9.0, KS_Teleport 1.3.0 et KS_Claim 1.4.0 chargés et activés sans erreur ni avertissement (KS_Teleport avant KS_Claim), 49 plugins, démarrage en 15 s. Aucun joueur ne s'était encore connecté : aucune commande (`/magasin`, `/setspawn`, `/ksclaims`) n'a tourné, reste à tester en jeu.**
