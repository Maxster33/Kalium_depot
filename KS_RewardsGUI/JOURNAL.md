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

**Journal de Kixster lu après le redémarrage de 18:36 (09/10/2026) : activé sans erreur, avec KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ; KS_AntiCheat démarre après eux. Personne n'avait encore ouvert la fiche d'un joueur : reste à tester en jeu.**

## 1.4.2 - alias /reward (11/10/2026, LeKiwi06)

Constat fait dans les journaux de Kixster : deux joueurs ont tapé `/reward` (deux fois chacun), qui n'existait pas.
Accord de LeKiwi06 : « oui fais les deux » (avec KS_Teleport 1.1.0).

- `/reward` ouvre les récompenses, comme `/rewards` et `/recompenses` (alias dans `plugin.yml`). Aucun changement de
  code, aucun nouveau texte.
- Empilé sur la 1.4.1 et la 1.4.0, jamais testées en jeu, à la demande de LeKiwi06.

**Compilé le 11/10/2026 (`sortie/KS_RewardsGUI-1.4.2.jar`). Jamais déployé seul : contenu dans la 1.5.0, déployée sur Kixster le 11/10/2026. Statut : non testé en jeu.**

## 1.5.0 - fusion de deux coffres de récompense à l'enclume (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai qu'on puisse fusionner 2 coffres de récompenses dans l'enclume ce qui additionnerait
leurs valeurs , et couterait un nombre de point d'xp égale au nouveau montant en emeraude du coffres , donc si le coffre
vaut 1000 points , ça demande 1000 points d'xp , le coffre s'appellera " coffre de récompense fusionné " , ça
permettrait de stacker pleins de petits coffres pour essayer d'avoir une grosse récompense sur la longueur ».
Ses réponses aux deux questions de Claude : interface : « le clique droit sur l'enclume ouvre une interface de coffre
avec un bouton valider sur la case du centre bas et le cout en xp affiché entre parenthèse , ne pas accepter les coffres
déjà ouvert » ; contenu : « Nouveau tirage » (les contenus des 2 coffres sont abandonnés, un nouveau contenu est tiré
comme une seule récompense de la valeur totale).

- **Ouverture** : clic droit sur une enclume (neuve ou abîmée), **un coffre de récompense en main** : le menu « Fusion
  de coffres » s'ouvre, à la place du coffre. Sans coffre en main, l'enclume s'ouvre comme d'habitude. La vraie fenêtre
  de l'enclume refuse toujours les coffres (le jeu Bedrock y calcule lui-même le résultat).
- **Menu** (coffre de 3 rangées) : un clic sur un coffre de récompense de son inventaire le choisit (2 au plus ; celui
  tenu en main l'est d'office), un clic sur un coffre choisi le retire. Les coffres **ne quittent pas l'inventaire** :
  le menu n'en montre que l'image, rien ne s'y prend ni ne s'y dépose (rien à rendre à la fermeture, rien à perdre si
  le serveur s'arrête).
- **Refusés** (message au-dessus de la barre d'objets) : coffre déjà ouvert, coffre sans valeur moyenne, coffre inconnu
  du serveur.
- **Bouton « Valider (N XP) »**, case du centre de la rangée du bas : N = valeur du coffre fusionné (somme des deux
  valeurs moyennes), arrondie à l'unité supérieure, en **points d'expérience** (pas en niveaux). Vert si le joueur les
  a, rouge sinon ; sa description donne le nom du coffre, sa valeur moyenne, le coût et les points du joueur. En
  créatif : gratuit.
- **Résultat** : les deux coffres disparaissent avec leur contenu ; le joueur reçoit, à la place du premier, un coffre
  **« Coffre de récompense fusionné »** (« Origine : Fusion », « Valeur moyenne : n émeraude(s) », « Non ouvert »), qui
  s'ouvre, s'échange et se fusionne à nouveau comme les autres. Il reste choisi dans le menu, pour lui ajouter aussitôt
  un autre coffre.
- **Nouveau tirage** : le contenu des coffres ordinaires est tiré par le serveur qui donne la récompense (KG_Rewards,
  KV_Rewards) ; pour un coffre fusionné, ce plugin tire lui-même, dans `plugins/KS_RewardsGUI/fusion.yml` (classe
  `Tables`) : les mêmes pools que `butin.yml`, et des « réglages » qui donnent, pour toute valeur, le nombre de tirages
  et la fréquence de chaque pool, de sorte que la moyenne du tirage soit la valeur du coffre (écart maximal 0,6 %).
  Fichier généré hors dépôt par `sortie/rewards/generer_fusion.py`, contrôlé par `verifier_fusion.py` (comme
  `butin.yml` : il donnerait les chances de chaque objet). **Sans ce fichier, la fusion est désactivée** (message).
- Journal `recuperations.log` : une ligne « FUSION » (qui, coût, les deux coffres avec leur contenu abandonné, le
  nouveau coffre avec sa valeur et son contenu).
- Nouveaux textes : clés `fusion.*` et `coffre.fusionne` de `lang.yml` (ajoutées au premier usage).
- Contient la 1.4.2 (alias `/reward`), jamais déployée.

Choix de Claude, signalés :
- un coffre de récompense en main est nécessaire pour ouvrir le menu (sinon l'enclume ne s'ouvrirait plus) ;
- nombre de tirages d'un coffre fusionné : celui des niveaux déjà en service, reliés entre eux (1 → 4 ; 5 → 6 ;
  10 → 8 ; 100 → 12 ; 1 000 → 16 ; 2 000 → 24 ; 8 000 → 27 ; 40 000 → 40 ; 80 000 → 80 ; au-delà, 1 tirage par
  1 000) ; fréquences : même calcul que les niveaux, corrigé dans deux plages qu'aucun niveau n'atteint (7 à 7,9 et
  224 à 250 émeraudes par tirage), où il s'écartait de la valeur de 5 à 10 % ;
- tous les coffres non ouverts qui ont une valeur moyenne se fusionnent, coffres de tops, de concours et de
  rattrapage compris (leur contenu fixe est abandonné comme le reste) ;
- le coffre fusionné reste choisi dans le menu après la fusion.

À savoir :
- Empiler coûte de plus en plus : chaque fusion coûte la valeur du **nouveau** coffre. Dix coffres de 10 émeraudes
  fusionnés un par un coûtent 20 + 30 + ... + 100 = 540 points ; fusionnés deux par deux (puis les fusionnés entre
  eux), moins.
- La fusion ne crée pas de valeur : la moyenne du coffre fusionné est la somme des deux ; elle change la répartition
  (moins de tirages au total, pools plus rares).
- Les tables de `fusion.yml` ne suivent pas une modification faite dans l'interface admin de KG_Rewards ou de
  KV_Rewards : régénérer et renvoyer les trois fichiers ensemble.
- Empilé sur les 1.4.0 à 1.4.2, jamais testées en jeu, à la demande de LeKiwi06. Écrit sans serveur de test.

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; 1.4.1 dans `_removed-ks_rewardsgui-1.4.1/` ; avec `plugins/KS_RewardsGUI/fusion.yml` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.4.0). Statut : non testé en jeu.**

**`fusion.yml` remplacé sur Kixster le 11/10/2026 à 09:00 (LeKiwi06 ; accord de LeKiwi06 : « oui déploie tout » ; Cristal de purification dans le pool Légendaire, comme dans les
`butin.yml` ; ancien fichier dans `_removed-ks_rewardsgui-fusion-2026-10-11/` ; jar inchangé), actif après redémarrage de
Kixster. Non testé.**

## 1.6.0 - coffres déposés par un plugin de ce serveur (parrainage) (23/10/2026, LeKiwi06)

**Demande de LeKiwi06 (23/10/2026)** : récompenses du parrainage (coffres pour le parrain et le filleul à chaque
palier de temps de jeu, barème validé le même jour).

- `KSRewardsGUI.deposerCoffre(joueur, nom, origine, raison, valeur)` : coffre « Non ouvert » d'une valeur moyenne
  donnée, contenu tiré avec les tables de `fusion.yml` (comme un coffre fusionné) ; il arrive dans /rewards comme une
  récompense venue d'un autre serveur. Faux si `fusion.yml` manque ou est vide (KS_Parrainage réessaie chaque minute).
- Rien d'autre ne change.

**Compilé, non déployé, non testé en jeu.** À déployer sur Kixster avec KS_Parrainage 1.0.0 ; `fusion.yml` doit être
présent dans `plugins/KS_RewardsGUI/` de Kixster.

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé) ; 1.5.0 dans `_removed-ks_rewardsgui-1.5.0/` ; actif après redémarrage, non testé en jeu.**

## 1.7.0 - fusion dans l'enclume en coffre de KS_Enclume (25/10/2026, LeKiwi06)

Demande de LeKiwi06 (voir `KS_Enclume/JOURNAL.md`, 1.4.0) : le menu de fusion s'ouvre accroupi + clic sur une enclume
et sert aussi d'enclume ordinaire (bugs de l'enclume pour les joueurs Bedrock) ; coffres posés dans les cases ; usure de
12 % à chaque validation, fusion comprise. Le menu est dans KS_Enclume 1.4.0 ; ce plugin garde les règles de la fusion.

- **Menu « Fusion de coffres » retiré** (il ne s'ouvre plus au clic droit sur une enclume, coffre en main). Les coffres
  se fusionnent dans l'enclume en coffre de KS_Enclume : deux coffres non ouverts posés dans ses cases.
- Clic droit sur une enclume, coffre de récompense en main : sans s'accroupir, le coffre ne s'ouvre pas et un message
  rappelle « Accroupis-toi et clique sur l'enclume pour fusionner des coffres. » (nouvelle clé `fusion.accroupi`) ;
  accroupi, le clic est laissé à KS_Enclume.
- Règles inchangées : coffres refusés (déjà ouvert, sans valeur moyenne, inconnu), coût = valeur du coffre fusionné en
  points d'expérience (retirés par KS_Enclume), nouveau tirage dans `fusion.yml`, ligne « FUSION » de
  `recuperations.log`. Nouveaux refus : un coffre avec un autre objet (`fusion.refus-autre`), deux fois le même coffre
  (`fusion.refus-meme`).
- API pour KS_Enclume : `estCoffre`, `refusFusion`, `apercuFusion` (image du coffre fusionné avant la fusion),
  `coutFusion`, `fusionner` (renvoie le coffre fusionné ; c'est KS_Enclume qui remplace les deux coffres de ses cases).
- Clés `fusion.titre`, `fusion.aide*`, `fusion.attente*`, `fusion.valider*`, `fusion.xp*`, `fusion.complet`,
  `fusion.absent`, `fusion.fait`, `fusion.manque`, `fusion.cout`, `fusion.resultat` de `lang.yml` : ne servent plus.

**Compilé le 25/10/2026, non déployé, non testé en jeu. À déployer sur Kixster avec KS_Enclume 1.4.0 (sans elle, les
coffres ne se fusionnent plus).**

## 1.8.0 - confirmation avant d'ouvrir un coffre de plus de 100 émeraudes (25/10/2026, LeKiwi06)

Demande de LeKiwi06 : « met une sécurité sur les coffres d'une valeur supérieur à 100 emeraude, ou on doit valider
l'ouverture pour que ça ouvre bien, j'ai eu plusieurs fois le soucis en voulant poser mon coffres fusionné dans mon
coffre, ça ouvrait mon coffre de récompense au lieu du coffre que je visais ».

- Clic droit avec un coffre de récompense **non ouvert** de **plus de 100 émeraudes** de valeur moyenne : au lieu de
  s'ouvrir, il montre un petit coffre « Ouvrir ce coffre ? » : « Ouvrir » (béton vert, à gauche ; rappelle qu'un coffre
  ouvert ne peut plus être fusionné ni revendu comme « Non ouvert »), l'image du coffre au centre, « Annuler » (béton
  rouge, à droite : le coffre reste fermé). Fermer le petit coffre revient à annuler.
- Coffres de 100 émeraudes ou moins, coffres déjà ouverts, coffres sans valeur connue : inchangés (ouverture directe).
- Nouvelles clés `confirmation.*` de `lang.yml`. Seuil fixé dans le code (`SEUIL_CONFIRMATION`).
- Contient la 1.7.0, jamais déployée.

Choix de Claude, signalés : seuil strict (101 émeraudes et plus) ; seulement à la première ouverture (un coffre déjà
ouvert n'a plus rien à protéger).

- **Conteneur visé** (accord de LeKiwi06 : « clic droit sur un coffre, tonneau ou autre conteneur avec un coffre de
  récompense en main, c'est le bloc visé qui s'ouvre ») : sans s'accroupir, un clic droit sur un bloc à inventaire
  (coffre, coffre piégé, tonneau, boîte de shulker, entonnoir, four, distributeur...) ou sur un coffre de l'Ender ouvre
  ce bloc, comme avec n'importe quel objet en main ; le coffre de récompense ne s'ouvre pas (et ne se pose toujours
  pas). Accroupi, le jeu n'ouvre pas le bloc : le coffre de récompense s'ouvre (avec la confirmation au-delà de 100
  émeraudes). Les protections (claims) s'appliquent comme pour tout clic sur un conteneur.

**Déployé sur Kixster le 25/10/2026 à 03:01 (LeKiwi06, avec KS_Enclume 1.4.0 et le `didacticiel.yml` ; accord de LeKiwi06 ; 1.6.0 dans
`_removed-ks_rewardsgui-1.6.0/` ; jar en place vérifié identique à la référence avant l'envoi ; `config.yml` et
`fusion.yml` non touchés), actif après redémarrage de Kixster. Contient la 1.7.0. Statut : non testé en jeu.**

## 1.9.0 - outils des opérateurs dans /rewards : don de coffres, historique (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026)** : « un bouton spécial dans le /rewards dédié aux opérateurs pour donner des
coffres de récompenses aux joueurs ? comme variable il faut : nom du joueur, nom du coffre, valeur du coffre (c'est
pour rétribuer les joueurs qui trouvent des bugs, [...] des récompenses d'événement etc.) ; il faut aussi y inclure un
historique de quelle personne a donné quoi à qui ». Maquette validée le même jour (« La maquette me va »).

- **Bouton « Outils des opérateurs »** (bloc de commande, case 52 de la barre de /rewards), visible avec la permission
  `ksrewardsgui.staff` (opérateurs par défaut). Petit coffre : « Donner un coffre », « Historique des dons »,
  « Coffres de mort actifs » (si KS_CoffreMort 1.1.0 est là : son menu `/coffres tous`, avec « Retour » vers ici),
  « Retour » vers /rewards.
- **Donner un coffre** : petite fenêtre avec trois champs : joueur, nom du coffre (40 caractères), valeur moyenne en
  émeraudes (virgule acceptée). Refus avec message, puis retour à la fenêtre avec ce qui a été tapé : joueur jamais
  connecté à ce serveur, nom vide, valeur hors de ]0 ; 10 000]. Puis confirmation verte / rouge (aperçu du coffre).
  Le joueur reçoit dans /rewards, en ligne ou non, un coffre « Non ouvert » nommé « Coffre : <nom> », origine
  « Staff », contenu tiré avec les tables de `fusion.yml` (`deposerCoffre`, comme le parrainage) ; message s'il est en
  ligne. Sans `fusion.yml` (ou vide) : refusé, rien n'est enregistré.
- **Historique des dons** : `plugins/KS_RewardsGUI/dons.yml` (date, opérateur, joueur et UUID, nom, valeur), affiché
  du plus récent au plus ancien (45 par page). Chaque don est aussi écrit dans `recuperations.log` (ligne « DON ») et
  dans la console.
- Plafond de 10 000 émeraudes (non demandé, techniquement nécessaire) : le nombre de tirages d'un coffre croît avec sa
  valeur, un coffre énorme ralentirait le serveur au moment du don.
- Menus en coffre de la classe commune `fr.kalium.menu.api.Contenant` (KLM_Menu 2.11.0 et plus) ; /rewards garde sa
  propre classe. Aucune nouvelle clé de `config.yml` ; nouveaux textes `staff.*` dans `lang.yml`.
- KS_CoffreMort est appelé sans dépendance à la compilation (il dépend déjà de KS_RewardsGUI). Avec un KS_CoffreMort
  plus ancien que 1.1.0, « Coffres de mort actifs » lance `/coffres tous`, qui n'ouvre alors que les coffres de
  l'opérateur lui-même.

À déployer avec KS_CoffreMort 1.1.0 (sinon pas de menu des coffres de mort). `fusion.yml` est déjà sur Kixster.

**Compilé, non déployé, non testé en jeu.**

**Déployé sur Kixster le 25/10/2026 à 03:09 (LeKiwi06, avec KS_CoffreMort 1.1.0 ; 1.8.0 dans `_removed-ks_rewardsgui-1.8.0/` ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage de Kixster. `config.yml` du serveur non touché. Statut : non testé en jeu.**

## 1.10.0 - moi de test : /rewards séparé (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- `/rewards` dans le monde de test : récompenses du compte de test (liste, récupération, message à la connexion).
- Les dépôts des autres plugins (coffres de mort, parrainage, autres serveurs) vont toujours au compte de Kixster.
- Outils des opérateurs depuis le monde de test : don de coffre seulement à un joueur qui s'y trouve (compte de test) ; coffres de mort refusés (survie).

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
