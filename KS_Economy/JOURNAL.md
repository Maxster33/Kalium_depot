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

**Statut : compilé le 04/10/2026, non déployé, non testé en jeu.**
