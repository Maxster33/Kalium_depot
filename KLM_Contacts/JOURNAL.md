# KLM_Contacts - journal

Tous les serveurs Paper (lobby, kal-games, Serveur Jeux, Kixster, Event, Kanvas). Cahier des charges :
`KLM_Contacts/CAHIER_DES_CHARGES.md` (publié au déploiement). Dépend de KLM_Menu (boîte à outils des menus, bouton
« Contacts » de « Informations ») ; les données sont sur le proxy, dans KaliumRelay (1.6.0 ou plus).

## 1.0.0 - étape 1 : amis (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'aimerais aussi un KLM_Contacts pour ajouter nos amis, créer des groupes avec eux, voir
dans quels serveurs de KaLium ils sont, etc. ». Réponses du 09/10/2026 : groupe = groupe de jeu (étape 2) ; 1re version
avec rejoindre / inviter, messages privés, notifications, mode invisible et blocage ; messages privés ouverts à tous,
réglables en « amis seulement » ; messages privés écrits dans le journal du proxy et soumis aux sanctions LibertyBans.
Le cahier prévoit trois étapes (amis, groupe, parties), une version testée avant la suivante : **cette version est
l'étape 1**.

- **`/amis`** (alias `/ami`, `/contacts`) ouvre le menu « Contacts » ; `/amis ajouter | accepter | refuser | retirer |
  rejoindre <pseudo>` ; **`/bloquer <pseudo>`**, **`/debloquer <pseudo>`**.
- **Bouton « Contacts »** dans le comparateur « Informations » (KLM_Menu 2.10.0). Avec un KLM_Menu plus ancien, le
  plugin fonctionne par ses commandes, sans le bouton.
- **Menu « Contacts »** : amis en ligne d'abord, avec le serveur où ils se trouvent (« Kal-Games », « Event »...), 12 par
  page ; « Ajouter un ami » (pseudo), « Demandes (n) », « Joueurs bloqués », « Réglages ».
- **Fiche d'un ami** : « Rejoindre son serveur » (s'il est en ligne sur un autre serveur), « Envoyer un message »,
  « Retirer des amis », « Bloquer » (confirmations).
- **Demandes** : envoyées par pseudo à tout joueur déjà venu sur KaLium, même hors ligne (il la voit à sa connexion) ;
  accepter / refuser ; annuler une demande envoyée ; deux demandes croisées valent acceptation.
- **Réglages** : mode invisible (les amis voient « hors ligne »), notifications de connexion des amis, messages
  privés (tout le monde / amis seulement / personne).
- **Messages privés** : commandes **du proxy** `/mp <pseudo> <message>` et `/r <message>` (KaliumRelay 1.6.0), d'un
  serveur à l'autre.
- Noms des serveurs affichés : `lang.yml`, clés `serveur.<nom Velocity en minuscules>`.
- Chaque ouverture de menu interroge le relais (`relay-url`, `relay-token`) hors du fil principal. Relais injoignable ou
  jeton vide : « Contacts indisponibles pour le moment », rien d'autre n'est gêné.
- **`relay-token`** : vide dans le dépôt, à recopier à la main dans `plugins/KLM_Contacts/config.yml` de chaque
  serveur (même valeur que les autres plugins reliés au relais).

Pas dans cette version (étapes suivantes du cahier) : groupes de jeu et tchat de groupe (étape 2), « Inviter dans ma
partie » et entrée du groupe en partie (étape 3).

Limites connues :
- « Rejoindre son serveur » ne tient pas compte des destinations désactivées dans la boussole du lobby ; les serveurs
  fermés à cette fonction se règlent sur le proxy (`contacts-no-join` de `relay.properties`, `serveur-jeux` par
  défaut). Un ami en Build Battle sur Kanvas est joignable : on arrive sur les plots, pas dans sa partie.
- L'autocomplétion des pseudos ne propose que les joueurs du même serveur.

**À déployer avec KaliumRelay 1.6.0** (proxy) ; KLM_Menu 2.10.0 pour le bouton. **Déployé sur les 6 serveurs Paper le 09/10/2026 à 06:06 (LeKiwi06), actif après redémarrage ; `relay-token` à remplir sur chaque serveur.
Statut : non testé en jeu** (logique du proxy essayée hors jeu : demandes, blocage, réglages, relecture après
redémarrage).

## 1.1.0 - étape 2 : groupe de jeu (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « code l'étape 2 sans attendre le test ». Empilé sur la 1.0.0 non testée : accord explicite.

- Les groupes et les commandes **`/groupe`** et **`/gc`** sont sur le proxy (KaliumRelay 1.7.0, voir son journal).
- **Menu « Groupe de jeu »** (bouton du menu « Contacts », ou `/groupe` seul) : membres avec leur serveur et le chef ;
  « Inviter un joueur » (pseudo, 60 s pour accepter) ; invitation reçue : « Rejoindre le groupe » / « Refuser » ;
  « Rejoindre le chef » ; « Quitter le groupe » ; pour le chef : « Gérer les membres » (exclure, nommer chef) et
  « Dissoudre le groupe ».
- **Fiche d'un ami** : « Inviter dans mon groupe » (s'il est en ligne).
- **Réglages** : « Suivre le chef » (d'office / me demander avant) et « Invitations de groupe » (tout le monde / amis
  seulement).
- **Suivre le chef** : quand le proxy demande si un joueur est en partie (message de plugin `kalium:contacts`,
  « follow »), le plugin interroge les jeux de ce serveur (`PlayerActivity` de KLM_Menu : KalGames, KG_BingoGame,
  KV_BuildBattle) et répond au relais ; en partie, le joueur n'est pas déplacé, il reçoit une proposition.
- Les résultats des actions de groupe sont dits dans le tchat par le proxy (pas de texte dans `lang.yml` pour eux).

Pas dans cette version (étape 3 du cahier) : « Inviter dans ma partie », entrée du groupe en partie, lien avec
« Rejouer ».

**À déployer avec KaliumRelay 1.7.0.** **Déployé sur les 6 serveurs Paper le 09/10/2026 à 06:29 (LeKiwi06 ; 1.0.0 dans `_removed-klm_contacts-1.0.0/`), actif après redémarrage de chaque serveur. Statut : non testé en jeu.**

## 1.2.0 - étape 3 : parties (09/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « code l'étape 3 sans attendre le test » (étape 3 du cahier de KLM_Contacts : le groupe de jeu
entre ensemble dans une partie, « Inviter dans ma partie », le groupe suit son chef quand il clique sur « Rejouer »).
Empilé sur la 1.1.0 non testée : accord explicite.

- **Nouvelle API `fr.kalium.contacts.api.JeuDeGroupe`** : un plugin de jeu s'y déclare (registre de services) avec un
  identifiant (`kalgames`, `bingo`, `buildbattle`), sait faire entrer un joueur dans une partie à partir d'une
  référence (`rejoindre`), et peut dire dans quelle partie privée se trouve un joueur (`partieDe`).
- **`KlmContacts.annoncer(joueur, jeu, référence, nom affiché)`** : appelé par les jeux à chaque entrée en partie ; le
  proxy (KaliumRelay 1.8.0) fait suivre les membres si ce joueur est chef d'un groupe.
- Quand le proxy le demande (message « game »), le plugin demande la partie au relais en disant si le joueur est déjà
  en partie, puis le plugin du jeu le fait entrer. Juste après une arrivée sur le serveur, il attend 1,5 s (remise à
  zéro du joueur par le hub).
- **Fiche d'un ami : « Inviter dans ma partie »**, quand je suis dans une partie privée d'un jeu déclaré (jeux du moteur
  de KalGames).
- **Menu « Groupe de jeu »** : partie proposée (celle du chef, ou une invitation) avec « Rejoindre la partie » /
  « Refuser la partie ».
- Le plugin demande une fois au proxy le nom Velocity de son serveur (au premier joueur connecté).

**À déployer avec KaliumRelay 1.8.0** ; les jeux suivent avec KalGames 1.25.0, KG_Bingo 1.10.0 et KG_BuildBattle 0.5.0. **Déployé sur les 6 serveurs Paper le 09/10/2026 à 06:50 (LeKiwi06 ; 1.1.0 dans `_removed-klm_contacts-1.1.0/`), actif après redémarrage. Statut : non testé en jeu.**
