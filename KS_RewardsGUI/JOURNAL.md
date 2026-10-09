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

**Déployé sur Kixster (1.0.1 dans `_removed-ks_rewardsgui-1.0.1/`) et sur Event (1.1.0 dans `_removed-ks_rewardsgui-1.1.0/`) le 09/10/2026 à 13:30 (LeKiwi06), actif depuis les redémarrages de 13:35 (journaux lus : 1.2.0 activée sur les deux serveurs, aucune erreur). `config.yml` des serveurs non touchés. Statut : testé et confirmé par LeKiwi06 le 09/10/2026 sur Event (« ça marche sur Event ») ; sur Kixster, les coffres qui contiennent un jeton de fly ou un badge restent fermés tant que ces objets n'y existent pas.**

## 1.3.0 - valeur moyenne sur le coffre, émeraudes dans le coffre (09/10/2026, LeKiwi06)

Demandes de LeKiwi06 après son test des coffres sur Event (« ça marche sur Event ») : « il faut afficher sur le coffre
de loot sa valeur moyenne » ; « il faut que les émeraudes soient données dans le coffre, pas via un message dans le
tchat ».

- **Valeur moyenne** : le message d'une récompense peut porter `valeur` (KG_Rewards et KV_Rewards 1.2.0 : la valeur
  moyenne de son niveau, pas celle de ce coffre-là, qui trahirait son contenu). Le coffre affiche alors « Valeur
  moyenne : n émeraude(s) » sous l'origine. Sans ce champ, la ligne n'apparaît pas.
- **Émeraudes dans le coffre** : les points d'une récompense (`type: argent`) ne sont plus versés sur le score avec un
  message ; ils sont mis dans le coffre en émeraudes (jusqu'à 64), ou en blocs d'émeraude et émeraudes au-delà. Le
  joueur les dépose lui-même sur son score (KS_Economy). Le coffre n'a donc plus besoin de KS_Economy pour s'ouvrir.
- Aucun autre changement (coffres existants compris : ils s'ouvrent comme avant, leurs points arrivent en émeraudes).

**Déployé sur Event et sur Kixster le 09/10/2026 à 15:12 (LeKiwi06 ; 1.2.0 dans `_removed-ks_rewardsgui-1.2.0/`), actif depuis les redémarrages de 16:03 (journaux lus : 1.3.0 activée sur les deux serveurs, aucune erreur). `config.yml` des serveurs non touchés. Statut : non testé en jeu.**

## 1.4.0 - /rewards en interface de type contenant (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage d'interfaces
de type contenant quand c'est possible au lieu des boutons ». Ses choix : commencer par la survie (`/rewards`, puis
l'économie) ; un écran qui demande de taper un texte ou un nombre garde une petite fenêtre de saisie ; maquette de
`/rewards` validée comme modèle (« oui, pars là-dessus »).

- **`/rewards`** n'est plus une fenêtre à boutons mais un **coffre de 6 rangées** : en haut, un objet par récompense
  en attente (45 par page) ; en bas, une barre d'actions sur fond de vitres grises.
  - Récompense d'un autre serveur : un **coffre** nommé par sa raison, avec « Origine », « Valeur moyenne » (si elle est
    connue) et « Clic : récupérer ce coffre ». Coffre de mort (dépôt local) : un **coffre de l'Ender**, avec le nombre
    de piles et « Clic : récupérer tes objets ».
  - Barre du bas : flèches « Page précédente » / « Page suivante » (seulement s'il y a une page), livre « Comment ça
    marche », entonnoir « Tout récupérer » (dès 2 récompenses), barrière « Fermer ».
  - Aucune récompense : un papier « Aucune récompense en attente » au milieu.
- Un clic récupère la récompense et **le menu se remplit à nouveau sans se refermer** (le curseur ne bouge pas).
- **Messages** (inventaire plein, tout récupéré...) : au-dessus de la barre d'objets du joueur, avec un son ; plus de
  fenêtre à fermer ni de message dans le tchat.
- Nouvelle classe `Contenant` (menu de type contenant : cases, actions, décor, écoute commune), écrite pour être
  reprise par les autres menus ; à déplacer dans la boîte à outils de KLM_Menu quand un deuxième plugin s'en servira.
- Textes sous de nouvelles clés `liste.*` de `lang.yml` (les anciennes clés `menu.*` ne servent plus).
- Inchangé : ce que fait une récupération (coffre « Non ouvert », coffre de mort rendu en objets), le journal, le
  signal pour l'anti-triche, la commande `/rewards` et les boutons qui l'ouvrent.

Limites : écrit sans serveur de test. À vérifier en jeu, surtout sur Bedrock (affichage des lignes de description,
clic sur les cases).

**Déployé sur Kixster et sur Event le 09/10/2026 à 18:20 (LeKiwi06 ; 1.3.0 dans `_removed-ks_rewardsgui-1.3.0/`), actif depuis les redémarrages de 18:23 (journaux lus : 1.4.0 activée sur les deux serveurs, aucune erreur). `config.yml` des serveurs non touchés. Statut : non testé en jeu.**

## 1.4.1 - lecture publique pour la modération (09/10/2026, LeKiwi06)

Pour la fiche d'un joueur de KS_AntiCheat 1.3.0 (demande de LeKiwi06 : récompenses en attente dans la fiche de
modération). Aucun changement pour les joueurs ; empilé sur la 1.4.0 non testée (« vas-y pour la suite »).
- `recompensesEnAttente(joueur)` : récompenses en attente d'un joueur (date d'envoi, origine, raison, nombre d'éléments,
  dépôt local ou non), en ligne ou hors ligne. Lecture seule : le contenu n'est ni montré ni touché.

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

**Déployé sur Kixster le 09/10/2026 à 18:31 (LeKiwi06, envoi groupé : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 ; accord de LeKiwi06 : « Oui, sur Kixster » ; 1.4.0 dans `_removed-ks_rewardsgui-1.4.0/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (KS_RewardsGUI 1.4.0). Statut : non testé en jeu.**
