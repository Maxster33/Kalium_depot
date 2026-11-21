# Journal — KLM_Menu (anciennement KaliumMenu)

Le plugin s'appelait **KaliumMenu** jusqu'à la 1.5.0 (renommé en 2.0.0, voir la dernière section).

Menu de navigation (Dialog natif, boussole) entre le lobby et les serveurs du réseau KaLium. Un seul jar,
installé sur chaque serveur Paper ; `role: lobby` sur le lobby (liste des serveurs), `role: backend` ailleurs
(retour au lobby + entrées locales, ex. "Hub de Kal-Games").

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## Configuration du lobby — destination Kanvas (26/09/2026, pas de nouvelle version)

**Demande de LeKiwi06** : mettre à jour les noms de KLM_Menu. Dans `plugins/KLM_Menu/config.yml` du lobby, la
destination `kal-test-dev` (« Kal-Test-Dev », serveur de test) n'existait plus sur le proxy (renommé `Kanvas` dans
`velocity.toml`) : remplacée par **`Kanvas`** (« Kanvas » en orange `#FF8800`, « Serveur de plots en créatif :
construis, fais noter tes builds. »). Autres noms vérifiés, à jour. Original dans
`plugins/_removed-klm_menu-config-2026-09-26/config.yml` du lobby. S'applique avec `/kaliummenu reload` (opérateur).
La configuration par défaut du dépôt ne contenait pas `kal-test-dev`.
**Erreur de Claude, corrigée** : la première version de la description contenait « créatif : construis » sans
guillemets ; en YAML, « : » suivi d'un espace est invalide hors guillemets : après `/kaliummenu reload`, le fichier n'a
pas pu être lu et le menu ne proposait plus que le lobby. Description mise entre guillemets, fichier vérifié avec
snakeyaml avant envoi (5 destinations), renvoyé. À retenir : toujours vérifier un config.yml modifié avant de l'envoyer.
**Testé et confirmé par LeKiwi06 le 26/09/2026** (boussole du lobby).

## Déploiement sur Kanvas (27/09/2026, pas de nouvelle version)

Demande de LeKiwi06 : le point de chute réglé au lobby pour Kanvas ne s'appliquait pas (Kanvas n'avait ni KLM_Portal ni
KLM_Menu 2.3.0, qui lisent la consigne à l'arrivée). **KLM_Menu 2.3.0 + KLM_Portal 1.2.0** installés sur Kanvas le 27/09/2026 à
19:59 (KLM_Menu 2.0.0 dans `_removed-klm_menu-2.0.0/`) ; `relay-token` de `plugins/KLM_Portal/config.yml` à remplir à
la main (Kanvas et lobby). Statut : testé et confirmé par LeKiwi06 le 27/09/2026 (point de chute vers Kanvas, boussole et étoile de
KV_Menu).

## 2.9.0 - bouton « Recherche de joueurs » dans la boussole (03/10/2026, LeKiwi06)

Demande de LeKiwi06 : « un bouton supplémentaire dans la boussole de KLM_menu "recherche de joueurs" : s'il y a moins
de 4 joueurs dans le serveur où se trouve la personne », qui propose la commande `/global send/true/false <message>`,
de « rejoindre le discord » (lien + bouton qui donne le QR code en main gauche) et de « repasser quand kiwi est en
live » (lien du Twitch + bouton QR code).
- Menu de navigation (boussole, `/servers`) : bouton « Recherche de joueurs » après les destinations, tant qu'il y a
  **moins de 4 joueurs sur ce serveur** (`player-search.below`, 0 = jamais).
- La fenêtre rappelle `/global send <message>`, `/global false` et `/global true` (plugin KLM_Chat), affiche le lien
  du Discord et celui du Twitch (`player-search.discord`, `player-search.twitch`), puis « Retour ».
- Nouvelle API `fr.kalium.menu.api.QrCodes` (`owner()`, `donner(joueur, lien, nom)`) : là où un plugin la fournit
  (KLM_Hub, lobby), deux boutons « QR code du Discord » et « QR code du Twitch ». Sans ce plugin : les liens seulement.
- Nouvelles clés facultatives (valeurs par défaut dans le code, rien à ajouter aux `config.yml` déjà en place).

Limite : le bouton n'apparaît que là où la boussole ou `/servers` sont utilisés (pas de boussole sur Serveur Jeux ;
sur Kixster et Event, après `/menu on`). À déployer avec KLM_Hub 1.0.0 sur le lobby.

**Déployé sur les 6 serveurs Paper le 03/10/2026 à 22:47 (LeKiwi06 ; anciennes versions dans `_removed-klm_menu-2.5.0/` (lobby, Kixster, Serveur Jeux), `2.6.0/` (Kal-Games, Kanvas), `2.8.0/` (Event)), actif après redémarrage. Statut : non testé en jeu.** Les versions 2.6.0 à 2.8.0 n'ont pas encore été
testées en jeu (signalé à LeKiwi06).

## 2.10.0 - interfaces de joueur dans « Informations » (bouton « Contacts ») (09/10/2026, LeKiwi06)

Demande de LeKiwi06 (KLM_Contacts, cahier validé le 09/10/2026) : un bouton « Contacts » dans le comparateur
« Informations », sur tous les serveurs. Jusqu'ici une interface de joueur qui n'était pas un classement n'y était pas
affichée. Empilé sur la 2.9.0, non testée en jeu : nécessaire pour la demande, signalé à LeKiwi06.
- `MenuSection.informations()` (faux par défaut) : une interface qui répond vrai a son bouton directement dans
  « Informations », après « Classements » et « Récompenses », avant « Paramètres ».
- Un joueur qui voit une telle interface reçoit le comparateur, même sans classement sur ce serveur. **Conséquence :
  avec KLM_Contacts installé, tous les joueurs ont le comparateur sur tous les serveurs** (sauf là où les objets de
  menu sont masqués par défaut, Kixster et Event : `/menu on`, ou `/menu`).
- Ajout seulement : les plugins compilés contre la 2.9.0 fonctionnent sans changement.

**Déployé sur les 6 serveurs Paper le 09/10/2026 à 06:06 (LeKiwi06 ; 2.9.0 dans `_removed-klm_menu-2.9.0/`), actif après redémarrage de chaque serveur. Statut : non testé en jeu.**

## 2.11.0 - menus de type contenant dans la boîte à outils (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage d'interfaces
de type contenant quand c'est possible au lieu des boutons ». Le premier menu converti (`/rewards`, KS_RewardsGUI 1.4.0)
a été validé en jeu par lui le 09/10/2026 (« ça marche, passe aux écrans de l'économie ») : sa brique passe dans la
boîte à outils pour servir à tous les plugins.

- **`fr.kalium.menu.api.Contenant`** : un coffre de 1 à 6 rangées dont chaque case porte un objet et, au besoin, une
  action. `poser(case, objet, action)`, `barre()` (dernière rangée en vitres grises), `objet(...)` (objet de menu :
  nom et lignes sans italique, attributs masqués ; à partir d'un matériau ou d'un objet existant), `decor()`,
  `lignes(texte, couleur)` (coupe une description à 35 caractères), `message(joueur, texte, refus)` (message au-dessus
  de la barre d'objets, avec un son), `pour(joueur, titre, marque)` (reprend le menu déjà ouvert de la même marque et le
  vide, pour le remplir à nouveau sans le refermer).
- **KLM_Menu enregistre l'écoute commune** (`Contenant.Ecoute`) : tout clic et tout glisser dans un tel menu est annulé
  (rien ne se prend ni ne se dépose), le clic sur une case à action la déclenche avec un son de bouton.
- Modèle à suivre (dans le commentaire de la classe) : 6 rangées, objets cliquables en haut (45 par page), barre
  d'actions en bas ; une saisie de texte ou de nombre reste une fenêtre de `Gui`, ouverte depuis une case.
- Ajout seulement : `Gui` et les plugins compilés contre la 2.10.0 fonctionnent sans changement. KS_RewardsGUI 1.4.0
  garde pour l'instant sa propre copie de la classe.
- **Complété le même jour pour KS_Economy 1.5.0** (premier plugin à s'en servir, 28 écrans) :
  `pour(joueur, rangées, titre, marque)` (petit menu, confirmation) et `bas(colonne)` (case de la barre d'actions) ;
  `lignes(texte mis en forme)` (coupe un texte de `lang.yml` ; un texte court est gardé tel quel) ;
  `saisie(joueur, fenêtre)` : ouvre une fenêtre de `Gui` depuis un menu (le coffre est refermé d'abord ; un joueur
  Bedrock attend 5 ticks, délai choisi par Claude, à vérifier en jeu) ; `fermer(joueur)` et `apres(suite)` : pendant
  un clic, le jeu interdit d'ouvrir ou de fermer une fenêtre, donc `ouvrir`, `fermer` et `saisie` attendent la fin du
  clic (un tick) quand ils sont appelés depuis l'action d'une case.

**Déployé le 09/10/2026 à 19:09 sur les 6 serveurs Paper (lobby, Kal-Games, Kanvas, Serveur Jeux, Event, Kixster ; accord de LeKiwi06 : « oui déploie » ; jars en place vérifiés identiques à `jars-deployes/` ; 2.10.0 dans `_removed-klm_menu-2.10.0/`). Actif après redémarrage. Statut : non testé en jeu.**

## 2.12.0 - interfaces par espace (KLM_Espaces) (21/10/2026, LeKiwi06)

**Demande de LeKiwi06 (21/10/2026)** : Kal-Games et Serveur Jeux expirent dans 3 jours ; « on va fusionner kal-games et kanvas en utilisant le même principe que pour les mondes simultanés dans serveur jeux » ; « les gens ne pourront pas passer de kanvas à kal-games sans le lobby, tout sera sur le même serveur mais de façon hermétique, on fera même des tchats séparés » ; inventaires séparés ; deux noms Velocity gardés (`kal-games` et `Kanvas`, même adresse). Plan : [Plan de migration](https://claude.ai/artifact/MyTJxpL8zq1XCy1sTZKdZh).

- Nouvelle clé facultative `espaces-interfaces` (espace -> noms de plugins, `*` accepté) : sur un serveur à plusieurs
  espaces, les interfaces (Informations, Paramètres, `/menu`) et les objets de menu d'un plugin rattaché à un autre
  espace que celui du joueur lui sont cachés. Absente, ou sans KLM_Espaces : rien ne change.
- Pour Kanvas : `kal-games: ["KalGames", "KG_*"]`, `kanvas: ["KV_*"]`.
- À déployer seulement sur Kanvas (les autres serveurs n'en ont pas besoin).

**Compilé, non déployé, non testé en jeu.** À déployer avec KLM_Espaces 1.0.0 et KaliumRelay 1.9.0 (fusion de Kal-Games dans Kanvas).

**Déployé aussi sur Kixster le 25/10/2026 à 03:10** (même jar que Kanvas, avec KS_MondeTest 1.0.0 qui pose la métadonnée
`klm_espace` : `kixster` ou `test`) ; `config.yml` de Kixster : `espaces-interfaces: kixster: [KS_Menu, KS_Economy,
KS_Jetons, KS_RewardsGUI, KS_CoffreMort, KS_Claim, KS_Fly, KS_Parrainage, KS_AntiCheat]` (cachés dans le monde de test) ;
2.11.0 dans `_removed-klm_menu-2.11.0/`. Actif après redémarrage, non testé en jeu.
