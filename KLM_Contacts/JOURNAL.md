

**Actif sur Event depuis son redémarrage du 17/10/2026 à 03:57 (journal lu, heures en UTC : 01:57) : KLM_Contacts 1.3.0 puis KS_Trains 1.4.0 activés sans erreur ni avertissement (KLM_Contacts avant KS_Trains, comme voulu pour le partage entre amis), démarrage en 12 s. Statut : non testé en jeu.**

## 1.5.1 - « whitelist », anciens joueurs parrainables (23/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (23/10/2026, après le déploiement)** : « j'ai voulu parrainer un joueur, ça me dit que Kixster n'a pas encore envoyé la liste blanche (j'aimerais que les joueurs déjà whitelist puissent prendre un parrain, pour qu'on puisse rattraper les récompenses qu'on aurait dû avoir) ; utilise le terme whitelist au lieu de liste blanche d'ailleurs » ; « j'aimerais aussi que les heures AFK sur le serveur ne comptent pas (considérer un joueur comme AFK s'il n'effectue pas d'actions pendant plus de 5 minutes et enlever l'AFK quand une action est effectuée) ». Ses choix : rattrapage sur le temps vanilla (CoreProtect, prévu d'abord, ne tourne pas sur Kixster en 26.3), temps actif aussi dans Statistiques.

- « whitelist » au lieu de « liste blanche » dans tous les textes : **nouvelles clés de lang.yml** (`-2`, `-1b`), les anciens textes restant enregistrés dans le lang.yml des serveurs.
- Textes adaptés aux joueurs déjà whitelistés qui prennent un parrain (aide, saisie, refus « a déjà un parrain »), messages pour un parrainage croisé refusé.

**Compilé, non déployé, non testé en jeu.** À déployer ensemble : KaliumRelay 1.10.1 (proxy), KLM_Contacts 1.5.1 (tous les serveurs Paper), KS_Parrainage 1.1.0 + KLM_Statistiques 1.1.0 + `didacticiel.yml` (Kixster). Empilé sur les versions du 23/10 (déployées, pas encore testées), à la suite de la même demande.

## 1.5.0 - parrainage, demandes de liste blanche de Kixster (23/10/2026, LeKiwi06)

**Demande de LeKiwi06 (23/10/2026)**, à partir de son message d'annonce (« dans le menu contact cliquez sur
"parrainer un joueur" ; votre ami n'a plus qu'à accepter le parrainage, et il sera automatiquement whitelisté sur
Kixster » ; « une interface apparaît quand vous essayez de rejoindre Kixster sans être whitelisté, vous devez préciser
votre pseudo Discord ») ; maquette validée le même jour. Ses choix : menu admin en jeu (Discord plus tard) ; filleul
choisi parmi ses amis ou par un autre pseudo.

- Nouvelle classe `Parrainages`, menus en coffre (outil `Contenant` de KLM_Menu) :
  - bouton « Parrainer un joueur » du menu Contacts et `/parrainage` (`/parrainer`) : têtes des amis jamais venus sur
    Kixster (clic = proposition), « Autre pseudo » (petite fenêtre), « Mes filleuls » (temps de jeu actif, paliers
    atteints, prochain palier ; une proposition en attente s'annule d'un clic), « Propositions reçues », livre d'aide
    (avec l'avertissement sur les sanctions), « Demander aux admins » pour un joueur qui n'est pas sur Kixster ;
  - acceptation par le filleul : petit coffre Accepter / tête du parrain / Refuser ;
  - refusé à l'entrée de Kixster (message `whitelist` du proxy) : propositions reçues s'il en a, sinon fenêtre
    « Rejoindre Kixster » avec le champ « Pseudo Discord » (ou un rappel si une demande attend déjà) ;
  - Modération › « Demandes de whitelist » (permission `klmcontacts.whitelist`, opérateurs par défaut) : une feuille
    par demande (Discord, date, Java / Bedrock), clic = petit coffre Accepter / Refuser ; bouton « Parrainages » :
    qui a parrainé qui, date, temps de jeu actif du filleul.
- Choix de Claude : sur la maquette, les admins acceptaient d'un clic gauche et refusaient d'un clic droit ; l'outil
  `Contenant` ne transmet pas le type de clic, d'où le petit coffre de confirmation.

**Compilé, non déployé, non testé en jeu.** Empilé sur la 1.4.0 (testée en solo seulement) : LeKiwi06 a répondu « je valide » au message qui le signalait, le
23/10/2026. Exige KaliumRelay 1.10.0 ; à déployer sur tous les serveurs Paper avec lui.

**Déployé le 23/10/2026 (16:22-16:27) sur Kixster, le lobby, Serveur Jeux et Kanvas (déploiement groupé, accord de LeKiwi06) ; anciens jars dans `_removed-klm_contacts-<version>/` ; actif après redémarrage, non testé en jeu.**

## 1.4.0 - serveur à plusieurs espaces (KLM_Espaces) (21/10/2026, LeKiwi06)

**Demande de LeKiwi06 (21/10/2026)** : Kal-Games et Serveur Jeux expirent dans 3 jours ; « on va fusionner kal-games et kanvas en utilisant le même principe que pour les mondes simultanés dans serveur jeux » ; « les gens ne pourront pas passer de kanvas à kal-games sans le lobby, tout sera sur le même serveur mais de façon hermétique, on fera même des tchats séparés » ; inventaires séparés ; deux noms Velocity gardés (`kal-games` et `Kanvas`, même adresse). Plan : [Plan de migration](https://claude.ai/artifact/MyTJxpL8zq1XCy1sTZKdZh).

- Une partie annoncée au proxy (chef de groupe, « Inviter dans ma partie ») porte le nom Velocity de l'espace du joueur
  (métadonnée `klm_serveur`), au lieu du nom du serveur demandé une seule fois au premier joueur arrivé (sur Kanvas,
  il aurait pu valoir `Kanvas` pour une partie de Kal-Games).
- Contient la 1.3.0 (sur Event seulement, non testée).
- Remarque : ce `JOURNAL.md` a été tronqué par le commit 4f70287 du 17/10/2026 (les sections 1.0.0 à 1.3.0 sont
  dans l'historique git), signalé à LeKiwi06, non restauré.

**Compilé, non déployé, non testé en jeu.** À déployer avec KLM_Espaces 1.0.0 et KaliumRelay 1.9.0 (fusion de Kal-Games dans Kanvas).
