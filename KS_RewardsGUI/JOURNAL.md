# KS_RewardsGUI - journal

Plugin du serveur Event : réception des récompenses des autres serveurs. Cahier des charges : catégorie 4
« Récompenses » de LeKiwi06 (validé le 30/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - réception, /rewards, journal (03/10/2026)

- **Transport** : boîte aux lettres `event` de KaliumRelay 1.3.0 (durable). Toutes les 30 secondes (`intervalle-secondes`),
  les messages sont lus, enregistrés dans `plugins/KS_RewardsGUI/recompenses.yml`, puis confirmés au relais ; un message
  déjà reçu n'est jamais compté deux fois (ids gardés). Joueur en ligne : message « Nouvelle récompense ». À la connexion :
  nombre de récompenses en attente.
- **Format d'un message** (YAML, documenté dans `Recompense.java`) : joueur (UUID), nom, origine, raison, date, contenu
  (`objet` : ItemStack sérialisé en base64 + nombre ; `custom` : id_custom de KS_KaliumGive + nombre ; `argent` : points
  du score).
- **`/rewards`** (alias `/recompenses`) et bouton « Récompenses » du menu d'Event : liste (origine, raison, contenu ; 8 par
  page), « Récupérer n », « Tout récupérer » (dans l'ordre, tant que l'inventaire a de la place). Refus si l'inventaire
  manque de place, si un objet custom n'existe pas sur Event (la récompense reste) ou si l'économie est absente. Pas
  d'expiration.
- **Journal** : `plugins/KS_RewardsGUI/recuperations.log` (date, joueur, origine, raison, contenu) pour l'anti-triche
  (catégorie 6).
- Configuration : `relay-url`, `relay-token` (**vide dans le dépôt** : à remplir sur le serveur avec le jeton du relais),
  `boite`, `intervalle-secondes`.
- `depend` KLM_Menu ; `softdepend` KS_Menu, KS_Economy, KS_KaliumGive ; `build.sh` : compiler ces plugins d'abord.

- **Bouton « Récompenses »** de KLM_Menu 2.6.0 : déclaré (`Recompenses`) ; ouvre `/rewards`. **KLM_Menu 2.6.0 obligatoire sur Event.**

**Déployé sur Event le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (nouveau), actif après redémarrage. Statut : non testé en jeu.**

## 1.0.1 - signal de récupération (03/10/2026, LeKiwi06)

Catégorie 6 (anti-triche, revente suspecte) : `fr.kalium.rewardsgui.api.RecompenseRecupereeEvent` (joueur, origine,
raison, objets, points, date d'envoi) quand une récompense est récupérée ; `plusAnciennesEnAttente()` : date de la
plus ancienne récompense en attente de chaque joueur. Aucun autre changement.

**Déployé sur Event le 03/10/2026 à 06:45 (LeKiwi06 ; 1.0.0 dans `_removed-ks_rewardsgui-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.0 - dépôt local d'une récompense (08/10/2026, LeKiwi06)

Catégorie 7 (coffres de mort) : `KSRewardsGUI.deposer(joueur, nom, origine, raison, objets)` dépose directement une
récompense en objets (36 piles au plus), sans passer par le relais ; elle apparaît dans `/rewards` comme les autres.
Utilisé par KS_CoffreMort 1.0.0 (mort dans un de ses claims ou en zone protégée, récupération par badge ou jeton de la
mort). Ces dépôts sont marqués `locale: true` : pas de signal `RecompenseRecupereeEvent` et pas de comptage dans
`plusAnciennesEnAttente()` (ce sont les objets du joueur lui-même : rien à surveiller pour l'anti-triche). Aucun autre
changement.

**Déployé sur Event le 08/10/2026 à 20:56 (LeKiwi06, avec KS_CoffreMort 1.0.0 ; 1.0.1 dans
`_removed-ks_rewardsgui-1.0.1/`), actif après redémarrage d'Event. `config.yml` du serveur non touché. Statut : non
testé en jeu.**

## 1.2.0 - récompenses remises dans des coffres « Non ouvert » (09/10/2026, LeKiwi06)

Demande de LeKiwi06, après son premier test des tables de butin : « il faut qu'on récupère les récompenses dans des
coffres renommés, avec un tag "non ouvert" pour s'assurer qu'ils n'ont pas été lootés (on pourra vendre nos loot box
comme ça) ». Ses réponses aux trois questions de Claude : ouverture au **clic droit, coffre en main** (fenêtre de
coffre, le tag saute dès l'ouverture) ; **contenu caché** jusqu'à l'ouverture ; coffres uniques, **en vente directe
seulement** pour le moment (pas de boutique).

- **Récupérer** une récompense dans `/rewards` donne maintenant **un seul objet** : un coffre nommé « Coffre :
  <raison> », avec les lignes « Origine : <serveur> » et « Non ouvert ». Une case libre suffit (avant : une case par
  pile). `/rewards` n'affiche plus le contenu, seulement l'origine et la raison.
- **Contenu caché** : il n'est pas dans l'objet, mais gardé par le plugin dans `plugins/KS_RewardsGUI/coffres.yml`,
  retrouvé par l'identifiant du coffre. Aucun joueur ne peut le lire avant l'ouverture, et un coffre copié ne s'ouvre
  qu'une fois.
- **Ouverture** : clic droit, coffre en main (dans le vide ou sur un bloc ; le coffre ne se pose jamais). À la première
  ouverture, le coffre devient « Ouvert (reste n pile(s)) », les points du score qu'il contient sont versés à celui
  qui l'ouvre (message), et une fenêtre montre les objets (54 au plus à la fois). Un clic sur un objet le prend ; on ne
  peut rien déposer dans la fenêtre. Fenêtre fermée : le coffre garde ce qui reste et se rouvre ; vidé, il disparaît.
- Les objets (objets custom compris) et les points ne sont créés qu'à l'ouverture : un coffre qui contient un objet
  inconnu du serveur se récupère, mais refuse de s'ouvrir (« il reste fermé ») tant que l'objet n'existe pas.
- **Protections** : le coffre ne se pose pas, ne se renomme pas à l'enclume (son nom dit ce qu'il contient), ne sert
  ni d'ingrédient (table de craft, fabricateur) ni de combustible.
- **Dépôts locaux** (coffres de mort de KS_CoffreMort, `locale: true`) : inchangés, rendus en objets avec leur contenu
  affiché (ce sont les objets du joueur lui-même).
- **Journal** `recuperations.log` : la récupération est notée comme avant (avec le contenu, pour le staff) ; une ligne
  « OUVERTURE » est ajoutée à la première ouverture (qui ouvre, quel coffre, gagné par qui, contenu).
- **Anti-triche** : `RecompenseRecupereeEvent` est toujours émis à la récupération, avec le coffre comme objet reçu et
  0 point (les points arrivent à l'ouverture). Le coffre porte un marqueur `ks_rewardsgui:coffre` : KS_AntiCheat le
  compte comme un objet de valeur (don sans contrepartie, récompense donnée presque aussitôt). Aucun changement dans
  KS_AntiCheat.

Choix de Claude, à corriger si besoin : objet = vrai coffre vanilla renommé (les joueurs Bedrock le voient sans rien
ajouter sur le proxy) ; dépôts locaux laissés en objets ; renommage à l'enclume refusé ; points versés à celui qui
ouvre, pas à celui qui a gagné la récompense.

Limites : écrit sans serveur de test. Le clic droit dans le vide avec un bloc en main n'a pas été vérifié sur Bedrock
(le clic sur un bloc ouvre aussi le coffre). Un coffre laissé au sol disparaît comme tout objet ; son contenu reste alors
dans `coffres.yml` (rien ne le nettoie). Les coffres ne s'empilent pas et ne se vendent pas en boutique (objets tous
différents).

**Compilé le 09/10/2026, non déployé. Statut : non testé en jeu.**
