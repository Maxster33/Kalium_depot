

**Déployé sur Event le 17/10/2026 à 02:41 (LeKiwi06 : « tu peux y aller » ; KS_Trains 1.3.0 en place vérifié identique à la référence, rangé dans `_removed-ks_trains-1.3.0/` avec son `config.yml` (identique à celui envoyé le 16/10, sans retouche) ; `config.yml` remplacé par celui de la 1.3.8 (`acceleration` 0,005, `freinage` 0,01, `decor.taille` 0,875, clés `virage`, `percussion`, `propulseur`) ; jar et `config.yml` du serveur vérifiés identiques après l'envoi ; supprimables par l'humain : `_removed-ks_trains-1.0.0/`, `1.1.0/`), actif après redémarrage d'Event. La 1.3.8 contient les 1.3.1 à 1.3.7, jamais déployées. Statut : non testé en jeu.**

## 1.3.9 - train arrêté en pente : les wagons ne glissent plus (17/10/2026, LeKiwi06)

Signalement de LeKiwi06 (capture sur Event) : « mon train s'est arrêté en pente, et du coup il s'est entassé ».

- **Cause** (défaut de la 1.0.0, visible avec la nouvelle physique) : sur une pente, le jeu pousse chaque wagonnet vers
  le bas de 0,0078 bloc/tick à chaque tick (lu dans le code du jeu 26.3, NewMinecartBehavior.calculateSlopeSpeed ; sans
  lien avec la gravité de l'entité). Train arrêté, seule la tête était tenue en place ; les autres wagons n'étaient que
  poussés vers leur place et glissaient jusqu'à s'entasser.
- **Correction** : train arrêté, chaque wagon est remis à sa place exacte dès qu'il s'en écarte de plus de 0,05 bloc, et
  sa vitesse est remise à zéro. En marche, rien ne change.
- Un train déjà entassé ne se démêle pas seul (son trajet enregistré est tassé lui aussi) : dételer ses wagons aux
  cisailles, les remettre en ligne sur les rails, puis les rattacher.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.3.9.jar`). **Statut : non déployé, non testé en jeu.**

## 1.4.0 - propriétaire et amis, limites, menu Bedrock, gares, autonomie, nom, démêlage (17/10/2026, LeKiwi06)

Propositions de Claude (« des ajouts utiles pour les trains ? ») acceptées par LeKiwi06 : « 1 : oui, il faut protéger
tous les contenants du train ; 2 : il faut une limite, 64 wagons c'est bien, par contre j'aimerais que le train tourne
tant qu'il y a du carburant, mais les joueurs ne peuvent avoir qu'un train qui charge simultanément, et il faut que la
zone chargée soit la plus petite possible ; 3 à 8 : d'accord ». Réponses : partage avec les amis de KLM_Contacts ; les
autres montent comme passagers ; un train qui percute celui d'un autre le fait dérailler quand même ; le dernier train
mis en route reste chargé, tant qu'il roule ; trains déjà posés : le premier qui ouvre le menu en devient propriétaire.
Contient la 1.3.9 (jamais déployée).

- **Propriétaire** : celui qui pose la locomotive (enregistré dans ses fours, avec son pseudo). Lui, ses **amis**
  (KLM_Contacts 1.3.0, liste rafraîchie chaque minute) et les opérateurs ont tous les droits : menu, conduite,
  carburant, coffres et entonnoirs, klaxons, wagon jukebox, dételage, attelage, boussole, nom. Les **autres** montent
  seulement dans les wagonnets simples (voyageurs) ; sinon « Ce train appartient à <pseudo> ». Les coffres et
  entonnoirs d'un train ne s'ouvrent pas non plus par une autre voie, et un entonnoir posé au sol ou un wagonnet à
  entonnoir hors du train ne les vide plus. Train sans propriétaire (posé avant la 1.4.0) : libre jusqu'à ce que
  quelqu'un ouvre son menu, qui en devient propriétaire. La fiche du menu donne le propriétaire.
- **64 wagons** au plus (`wagons-max`) : attelage refusé au-delà.
- **Chargement** : un train roule tant qu'il a du carburant ; seul le **dernier train mis en route** par son
  propriétaire garde des chunks chargés, et **seulement tant qu'il roule** (plus la minute après l'arrêt) ; zone : les
  chunks de ses wagons et celui où sera le wagon qui mène dans une seconde (avant : 16 blocs devant). Train sans
  propriétaire : aucun chunk forcé.
- **Menu** (secours pour Bedrock) : boutons **Niveau -** et **Niveau +** (mêmes règles que le contrôleur ; refusés si un
  autre joueur conduit) et **Klaxon** (corne choisie du contrôleur). À bord du contrôleur, **clic droit main vide**
  ouvre le menu du train (sur Bedrock, pas d'accroupi + clic depuis le wagon).
- **Départ en gare** : un train arrêté par des rails propulseurs éteints repart seul, à son niveau d'avant, quand plus
  aucun de ses wagons n'est sur un rail éteint (rail alimenté) ; « Le train repart ». Un arrêt ou un niveau changé à
  la main annule ce départ.
- **Autonomie** au-dessus de la barre d'objets du conducteur : « autonomie N blocs » au niveau actuel (le four alimenté
  le moins rempli, carburant de sa case compris), ou « sans carburant ».
- **Nom du train** : étiquette renommée (enclume), clic droit sur un four de la locomotive : le nom s'affiche au-dessus
  de la tête de la locomotive (vu aussi sur Bedrock), dans le titre du menu (« Nom — N wagons »), devant les messages du
  train et sur les boussoles liées ensuite (« Boussole : Nom »). L'étiquette est consommée.
- **Démêlage** : quand le trajet enregistré est trop court pour placer tous les wagons (wagons tassés, trop écartés,
  rechargés), il est prolongé en suivant les rails derrière le train, dans le sens opposé à la dernière marche.
- **`decor.actif`** (true) : false = plus de modèles agrandis.
- `config.yml` : nouvelles clés `decor.actif` et `wagons-max` ; valeurs par défaut aussi dans le code.

Choix de Claude, signalés : opérateurs = tous les droits ; train sans locomotive (wagons attelés entre eux) : sans
propriétaire, libre ; clic droit main vide à bord pour le menu ; démêlage d'un train tassé juste après un redémarrage :
sans dernière marche connue, le sens est deviné d'après le trajet (à vérifier).

`softdepend` KLM_Contacts ; `build.sh` compile contre KLM_Contacts (à compiler avant). Sans KLM_Contacts : propriétaire
seul. **À déployer avec KLM_Contacts 1.3.0** pour le partage entre amis.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.0.jar`). **Statut : non déployé, non testé en jeu.**
