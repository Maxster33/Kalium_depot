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

**À déployer avec KaliumRelay 1.6.0** (proxy) ; KLM_Menu 2.10.0 pour le bouton. **Compilé le 09/10/2026, non déployé.
Statut : non testé en jeu** (logique du proxy essayée hors jeu : demandes, blocage, réglages, relecture après
redémarrage).
