# Archive des comptes rendus de REPRISE_PROJET.md

Anciens comptes rendus, déplacés ici selon `REGLES.md` (section 5). À consulter uniquement en cas de doute ou
d'oubli : l'état actuel du projet est dans `REPRISE_PROJET.md`.

---

### 2026-10-02 — LeKiwi06

**Ajout (18/10/2026, 15:55) : KS_LaisseBateau 1.0.2 envoyé sur Event (tests), à redémarrer.** Jar en place (1.0.1) vérifié identique à `jars-deployes/`, rangé dans `_removed-ks_laissebateau-1.0.1/` ; jar du serveur vérifié identique à celui compilé ; `config.yml` du serveur non touché.

**Ajout (18/10/2026, 15:51) : KS_LaisseBateau 1.0.2 compilé, non déployé.** Le bateau remorqué suivait mal (vitesse du tracteur recopiée : glisse de côté dans les virages, élan au ralentissement) : laisse tendue : suit la trace de l'arrière du tracteur comme une remorque ; détendue : ni poussé ni tourné, seulement freiné.

**Ajout (18/10/2026, 10:33) : KS_Trains 1.4.8 et KS_LaisseBateau 1.0.1 envoyés sur Event (tests) ; actifs depuis le redémarrage du 18/10 à 13:34 (journal lu : aucune erreur), non testés en jeu.** Jars en place (KS_Trains 1.4.7, KS_LaisseBateau 1.0.0) vérifiés identiques à `jars-deployes/` avant l'envoi, rangés dans `_removed-ks_trains-1.4.7/` et `_removed-ks_laissebateau-1.0.0/` ; jars du serveur vérifiés identiques à ceux compilés.

**Ajout (18/10/2026, 10:32) : KS_LaisseBateau 1.0.1 compilé, non déployé.** Le bateau remorqué est tourné vers celui qui le tracte (remorque) au lieu de garder son orientation.

**Ajout (18/10/2026, 09:18) : KS_Trains 1.4.8 compilé, non déployé.** Corne posée prise en compte même sans cliquer sa vitre ; dans le menu du train, page de chaque wagon : « Renommer » (nom affiché dans le menu) et « Ouvrir le contenu » (coffre, entonnoir, four ; verrou respecté).

**Ajout (18/10/2026, 08:20) : KS_Trains 1.4.7 envoyé sur Event (tests) ; actif depuis le redémarrage du 18/10 à 08:30 (journal lu : aucune erreur au démarrage, ni après 35 minutes de jeu avec un train à 09:07), non testé en jeu.** Remplace la 1.4.6 (envoyée à 07:31, en attente de redémarrage) : jar en place vérifié identique à `jars-deployes/`, rangé dans `_removed-ks_trains-1.4.6/` ; jar du serveur vérifié identique à celui compilé, `config.yml` du serveur identique à celui du dépôt.

**Ajout (18/10/2026, 07:43) : KS_Trains 1.4.7 compilé, non déployé.** Modèles agrandis retirés (fours, coffres, entonnoirs), sauf celui du wagon jukebox. Sifflet et klaxon en catégorie de son « jukebox / blocs musicaux », seul le souffle de vapeur en « créatures amicales ».

**Ajout (18/10/2026, 07:31) : KS_Trains 1.4.6 envoyé sur Event (tests), à redémarrer.** Remplace la 1.4.4 (envoyée à 00:18, en attente de redémarrage) : jar en place vérifié identique à `jars-deployes/` et `config.yml` du serveur identique à celui envoyé avec la 1.4.3, rangés dans `_removed-ks_trains-1.4.4/` ; jar et `config.yml` du serveur vérifiés identiques après l'envoi.

**Ajout (18/10/2026, 07:29) : KS_Trains 1.4.6 compilé, non déployé.** Klaxon entendu de loin (plein volume jusqu'à 128 blocs, jusqu'à 256 ; avant, son attaché au wagon, ignoré par le jeu des joueurs trop loin) ; sifflet par défaut en deux coups (« tchou-tchou »).

**Ajout (18/10/2026, 00:20) : KS_Trains 1.4.5 compilé, non déployé.** Sons du train au volume d'avant la 1.4.3 de près (souffle 0,5, sifflet 1) mais entendus de loin (64 et 128 blocs) : joués pour chaque joueur, depuis quelques blocs de lui dans la direction du train.

**Ajout (18/10/2026, 00:18) : KS_Trains 1.4.4 envoyé sur Event (tests), à redémarrer.** Jar en place (1.4.3) vérifié identique à `jars-deployes/` avant l'envoi, rangé dans `_removed-ks_trains-1.4.3/` ; jar du serveur vérifié identique à celui compilé.

**Ajout (17/10/2026, 23:57) : KS_Trains 1.4.4 compilé, non déployé.** Corrige la 1.4.3 sur Event : erreur à chaque tick dans la console (`Quaternionf.dot` absent de JOML 1.10.9 du serveur), modèles agrandis jamais orientés, et suite des tâches coupée (collisions, entonnoirs, chunks, autres trains) ; train arrêté : chaque wagon retenu à sa place d'arrêt (entassement en pente après une marche arrière en montée).

**Ajout (17/10/2026, 10:20) : KS_Trains 1.4.3 et KS_Crafts 1.19.0 envoyés sur Event (tests) ; actifs depuis le redémarrage du 17/10 à 23:42 (journal lu : aucune erreur), non testés en jeu.** Jars en place (KS_Trains 1.4.1, KS_Crafts 1.18.0) vérifiés identiques à `jars-deployes/` avant l'envoi et rangés dans `_removed-ks_trains-1.4.1/` (avec l'ancien `config.yml`, identique à celui par défaut de la 1.4.1) et `_removed-ks_crafts-1.18.0/` ; jars et `config.yml` du serveur vérifiés identiques après l'envoi.

**Ajout (17/10/2026, 05:58) : KS_Trains 1.4.3 complété et KS_Crafts 1.19.0 compilés, non déployés.** Départ refusé sous 3 blocs/s ; un train plafonné paie le niveau qu'il atteint ; portée des sons (sifflet 128 blocs, souffle 64) ; messages gardés 3 s avant le retour du tableau du conducteur ; klaxon par défaut (sifflet en bloc musical) ; recette du contrôleur : bloc musical au lieu de l'ampoule de cuivre (KS_Crafts 1.19.0).

**Ajout (17/10/2026, 04:37) : KS_Trains 1.4.3 compilé, non déployé.** Surcharge des longs trains (formule choisie par LeKiwi06 sur tableau comparatif : charge partagée, 10 % par wagon au-delà de 7, divisé par le nombre de fours alimentés).

**Ajout (17/10/2026, 05:31) : KS_Trains 1.4.3 refait, compilé, non déployé.** La formule de surcharge ci-dessus est remplacée (choix de LeKiwi06 sur tableaux comparatifs : « proportionnel au carré, M3, sous charge à 0,5, et surcharge à 30 % ») : chaque four consomme selon la charge du train (wagons / traction), 0,5 au moins quand il est peu chargé, +30 % par wagon en trop partagé entre les fours ; la vitesse maximale baisse progressivement (traction / wagons au carré) au lieu de passer de l'arrêt à la pleine vitesse.

**Ajout (17/10/2026, 04:27) : KS_Trains 1.4.2 compilé, non déployé.** Le modèle agrandi bougeait dans tous les sens en pente (vidéo de LeKiwi06) : il est maintenant orienté sur l'axe du train (wagons voisins) au lieu du bloc de rail sous lui.

**Ajout (17/10/2026, 04:17) : KS_Trains 1.4.1 déployé sur Event (tests) ; actif après redémarrage.** Projection à 40 blocs de haut à pleine vitesse ; jar et `config.yml` vérifiés identiques après l'envoi.

**Ajout (17/10/2026, 04:13) : KS_Trains 1.4.1 compilé, non déployé.** Percussion : les entités sont envoyées très haut comme par le dragon (signalé par LeKiwi06 : elles ne l'étaient presque pas).

**Ajout (17/10/2026, 04:00) : journal d'Event lu après le redémarrage de 03:57 : KLM_Contacts 1.3.0 et KS_Trains 1.4.0 activés sans erreur.** Tests de LeKiwi06 à venir (partage entre amis, menu à bord, départ en gare).

**Ajout (17/10/2026, 03:55) : KS_Trains 1.4.0 et KLM_Contacts 1.3.0 déployés sur Event (tests) ; actifs après redémarrage.** Jars et `config.yml` de KS_Trains vérifiés identiques après l'envoi ; KLM_Contacts 1.3.0 seulement sur Event.

**Ajout (17/10/2026, 03:47) : KS_Trains 1.4.0 et KLM_Contacts 1.3.0 compilés, non déployés.** Les 8 propositions de Claude acceptées par LeKiwi06 : propriétaire et amis, 64 wagons, un train chargé par joueur, menu de secours Bedrock, départ en gare, autonomie, nom du train, démêlage, décor désactivable. KLM_Contacts expose la liste d'amis aux autres plugins.

**Ajout (17/10/2026, 03:00) : KS_Trains 1.3.9 compilé, non déployé.** Train arrêté en pente : les wagons glissaient et s'entassaient (capture de LeKiwi06) ; ils sont maintenant tenus à leur place.

**Ajout (17/10/2026, 02:45) : KS_Trains 1.3.8 déployé sur Event (tests) ; actif après redémarrage.** Jar et `config.yml` vérifiés identiques après l'envoi ; ancien `config.yml` (sans retouche) dans `_removed-ks_trains-1.3.0/`.

**Ajout (17/10/2026, 02:34) : KS_Trains 1.3.8 compilé, non déployé.** Modèle agrandi ramené à 0,875 bloc (il dépassait du wagonnet), face avant des fours tournée vers l'avant du train (signalé par LeKiwi06).

**Ajout (17/10/2026, 01:49) : KS_Trains 1.3.7 compilé, non déployé.** Le modèle agrandi (Java) restait droit sur les pentes (capture de LeKiwi06) : il penche maintenant de 45° avec le wagon, mis à jour chaque tick quand le train roule.

**Ajout (16/10/2026, 23:09) : KS_Trains 1.3.6 compilé, non déployé.** Un train à court de carburant freine comme un train qu'on arrête au lieu de s'arrêter net (demande de LeKiwi06).

**Ajout (16/10/2026, 23:08) : KS_Trains 1.3.5 compilé, non déployé.** Rails propulseurs (demande de LeKiwi06) : pas de carburant sur un rail allumé, -1 bloc/s par rail éteint franchi (20 rails arrêtent un train à pleine vitesse), départ refusé tant qu'un wagon est sur un rail éteint.

**Ajout (16/10/2026, 23:04) : KS_Trains 1.3.4 compilé, non déployé.** Son de la locomotive (sifflet au démarrage, souffle de vapeur rythmé selon la vitesse) et compteur kilométrique avec le carburant brûlé dans le menu du train (demandes de LeKiwi06).

**Ajout (16/10/2026, 23:00) : KS_Trains 1.3.3 compilé, non déployé.** Demandes et défaut signalés par LeKiwi06 : -10 % de vitesse par percussion (pas sous le niveau 1), wagonnets avec contenu projetés au lieu d'être brisés, message pour les wagons à TNT (déjà refusés), jukebox qui passait au morceau suivant après un rechargement (corrigé : il rejoue son disque).

**Ajout (16/10/2026, 22:55) : KS_Trains 1.3.2 compilé, non déployé.** Demandes de LeKiwi06 : train qui roule = entités projetées comme par le dragon (selon la vitesse), wagonnets libres brisés, autres trains déraillés (wagons dételés, locomotives cassées) ; train arrêté traversable ; -25 % dans les virages sauf niveau 1, anticipé, une diagonale = un seul virage.

**Ajout (16/10/2026, 22:45) : KS_Trains 1.3.1 compilé, non déployé.** Signalé par LeKiwi06 en test : wagons à four et contrôleur cassables (KS_Claim 1.3.0 lève en HIGHEST le refus de casser un wagonnet sans inventaire ; KS_Trains passe après lui), modèle agrandi resté en place après la casse d'un wagon ; accélération et freinage rendus progressifs (100 / 50 blocs, plus lents en surcharge).

**Ajout (16/10/2026, 22:25) : journal d'Event lu après le redémarrage de 22:20 ; nouvelle physique des wagonnets active.** Pack `minecart_improvements` accepté (gardé dans `level.dat` réécrit par le serveur), KS_Trains 1.3.0, KS_Crafts 1.18.0 (82 crafts) et KS_VerrouVehicule 1.1.1 activés sans erreur. Reste : `/gamerule max_minecart_speed 40` par LeKiwi06, puis ses tests.

**Ajout (16/10/2026, 22:20) : KS_Trains 1.3.0, KS_Crafts 1.18.0, KS_VerrouVehicule 1.1.1 et `level.dat` corrigé envoyés sur Event (arrêté par LeKiwi06) ; actifs après redémarrage.** Tous les fichiers vérifiés identiques après l'envoi ; anciennes versions dans leurs dossiers `_removed-…`. `config.yml` de KS_Trains remplacé (espacement 1,4). À vérifier au démarrage : pack `minecart_improvements` accepté.

**Ajout (16/10/2026, 22:10) : KS_Trains 1.3.0, KS_Crafts 1.18.0 et KS_VerrouVehicule 1.1.1 compilés, non déployés.** Demandes de LeKiwi06 après ses tests sur Event : collisions, wagons plus écartés, carburant mieux réparti par les entonnoirs, wagons plus gros pour Java (le jeu ne grossit pas un wagonnet : chargement agrandi) et fumée de la locomotive, klaxons (cornes de chèvre) dans le contrôleur, wagon jukebox (coffre juste devant, lecture / aléatoire / pause), boussole qui suit le train. Maquette validée. En attente : arrêt d'Event pour envoyer ces jars, `level.dat` corrigé et le nouvel `espacement`.

**Ajout (16/10/2026, 21:20) : KS_Trains 1.2.1 compilé, non déployé : train retourné en joignant deux trains (signalé par LeKiwi06).** L'ordre suivait le premier wagon cliqué ; il suit maintenant le morceau qui a la locomotive. À envoyer sur Event avec la correction de `level.dat` (Event arrêté).

**Ajout (16/10/2026, 21:10) : nouvelle physique refusée par Event au démarrage de 20:58.** Le pack `minecart_improvements` a été désactivé par le jeu (fonction absente de `enabled_features`, liste à ajouter aussi dans `level.dat` : erreur de lecture de Claude). Correction prête, en attente d'un nouvel arrêt d'Event par LeKiwi06.

**Ajout (16/10/2026, 20:58) : nouvelle physique des wagonnets activée sur Event (serveur arrêté par LeKiwi06).** Pack intégré `minecart_improvements` passé de Disabled à Enabled dans `world/level.dat` (seul changement, vérifié) ; original dans `/_removed-level-dat-2026-10-16/level.dat`. À faire après le démarrage : `/gamerule max_minecart_speed 40` (trains à 20 blocs/s ; tous les wagonnets d'Event concernés). Détail : `KS_Trains/JOURNAL.md`.

**Ajout (16/10/2026, 20:55) : journal d'Event lu après le redémarrage de 20:50.** KS_Trains 1.2.0, KS_Crafts 1.17.0 (81 crafts), KS_VerrouVehicule 1.1.0 et KS_LaisseBateau 1.0.0 chargés et activés sans erreur ni avertissement, démarrage en 14 s. Seul avertissement, déjà connu : GrimAC et ViaBackwards (véhicules des anciens clients). Tests en jeu de LeKiwi06 à venir ; nouvelle physique pas activée.

**Ajout (16/10/2026, 20:50) : KS_Trains 1.2.0 et KS_VerrouVehicule 1.1.0 déployés sur Event (tests de LeKiwi06) ; actifs après redémarrage.** Event n'avait pas redémarré depuis 20:00 : il démarrera directement avec KS_Trains 1.2.0, KS_Crafts 1.17.0, KS_VerrouVehicule 1.1.0 et KS_LaisseBateau 1.0.0. KS_Trains 1.1.0 retiré de `jars-deployes/` (jamais démarré). Nouvelle physique pas activée.

**Ajout (16/10/2026, 20:45) : KS_Trains 1.2.0 (étape 3, dernière du cahier) et KS_VerrouVehicule 1.1.0 compilés, non déployés, non testés.** Wagons à entonnoir (prennent dans les 3 wagons derrière, donnent aux 3 devant, carburant des fours, seau de lave et seau vide), réglages entrée / sortie des wagons à coffre dans le menu du train, wagons verrouillés attelés et réglés par leur propriétaire, plus ancrés dans un train. Les 3 étapes du cahier de KS_Trains sont codées.

**Ajout (16/10/2026, 20:45) : KS_Trains 1.1.0 et KS_Crafts 1.17.0 déployés sur Event (tests de LeKiwi06) ; actifs après redémarrage.** Event n'avait pas redémarré depuis l'envoi de 20:00 : KS_Trains 1.0.0 et KS_Crafts 1.16.0 n'y ont jamais tourné, retirés de `jars-deployes/`. Nouvelle physique pas activée. Étape 3 de KS_Trains en cours.

**Ajout (16/10/2026, 20:40) : KS_Trains 1.1.0 (étape 2 : wagon contrôleur) et KS_Crafts 1.17.0 compilés, non déployés, non testés.** Wagon contrôleur (recette en casque autour d'un wagonnet, chaque pièce une fois), un par train, à moins de 5 wagons de la locomotive ; à bord, avancer accélère, reculer freine puis passe en marche arrière (1 bloc/s), le dernier wagon menant alors le train ; conducteur descendu : arrêt. Empilé sur la 1.0.0 en test sur Event (demande de LeKiwi06 : « continue l'étape suivante »).

**Ajout (16/10/2026, 20:05) : KS_Trains 1.0.0, KS_LaisseBateau 1.0.0, KS_VerrouVehicule 1.0.0 et KS_Crafts 1.16.0 déployés sur Event pour les tests de LeKiwi06 ; actifs après redémarrage.** LeKiwi06 : « envoie tout sur event je vais faire les tests là-bas, et continue l'étape suivante ». Avant l'envoi : KS_Crafts 1.14.1 en place identique à la référence ; après : les 4 jars du serveur identiques à ceux compilés. KS_Crafts 1.14.1 n'est plus en service nulle part (Event 1.16.0, Kixster 1.15.0) : retiré de `jars-deployes/`. Nouvelle physique des wagonnets pas activée sur Event (il faut Event arrêté). Kixster n'est pas touché.

**Ajout (16/10/2026, 19:45) : KS_Trains 1.0.0 (nouveau plugin, étape 1 sur 3) et KS_Crafts 1.16.0 compilés, non déployés, non testés : trains de wagonnets.** Demande de LeKiwi06 : « un système pour créer de longs trains en faisant en sorte qu'ils se suivent de façon fluide » ; cahier des charges écrit avec ses réponses (gardé en local, `Cahiers_KaLium/KS_Trains.md`), validé : « je valide le cahier, commence l'étape 1 ». Étape 1 : locomotive (3 wagonnets à four, recette dans KS_Crafts), attelage à la chaîne et dételage aux cisailles ou au menu, wagons placés à chaque tick sur le trajet de la tête, traction (5 par four de la locomotive, 10 par autre four) et vitesse (1 bloc/tick, −0,1 par wagon en trop), carburant par four (1 charbon = 80 blocs, case unique, seau de lave), menu du train (marche / arrêt, composition, dételer), chunks gardés chargés. Lu dans le code du jeu 26.3 : les wagonnets ne se tiennent pas en laisse ; un wagonnet à four roule à 0,2 bloc/tick avec la physique actuelle ; la nouvelle physique est une expérience à activer sur le monde (règle `max_minecart_speed`). Étapes 2 (wagon contrôleur) et 3 (entonnoirs, réglages des coffres, KS_VerrouVehicule 1.1.0) à suivre après les tests. Réservations de KS_Trains et KS_Crafts gardées.

**Ajout (14/10/2026, 20:40) : KS_LaisseBateau 1.0.0 (nouveau plugin) compilé, non déployé, non testé : laisse courte entre deux bateaux.** Demande de LeKiwi06 : « tu peux réduire la longueur de la laisse si deux bateaux sont accrochés l'un à l'autre aussi ? ». Lu dans le code du jeu 26.3 : la laisse tire au-delà de 6 blocs et casse à 12, sans réglage. Ses choix : 3 blocs de centre à centre, tout bateau tiré par un bateau, Kixster seulement. Le plugin tire lui-même le bateau remorqué (vitesse du bateau qui tracte + rappel) ; un bateau remorqué conduit par un joueur n'est pas tiré (calculé par son jeu). `longueur` et `raideur` réglables dans `config.yml`. Réservation gardée jusqu'au déploiement.

**Ajout (14/10/2026, 18:10) : KS_VerrouVehicule 1.0.0 et KS_Crafts 1.15.0 actifs sur Kixster, journal lu.** Redémarrage du 14/10 à 18:04 : les deux plugins chargés et activés sans erreur ni avertissement, « 79 crafts ajoutés » (78 + la recette du verrou), démarrage en 15,5 s. Avertissement déjà présent, sans lien avec ce déploiement : GrimAC signale que ViaBackwards sur un serveur 1.21.2+ pose des problèmes de véhicules aux anciens clients. Reste à tester en jeu.

**Ajout (14/10/2026, 18:03) : KS_VerrouVehicule 1.0.0 et KS_Crafts 1.15.0 déployés sur Kixster ; non testés, actifs après redémarrage ; réservations libérées.** LeKiwi06 : « oui déploie sur kixster ». Avant l'envoi : KS_Crafts 1.14.1 en place identique à la référence ; après : jars du serveur identiques à ceux compilés, 1.14.1 dans `_removed-ks_crafts-1.14.1/`. Event n'est pas touché : `jars-deployes/` garde KS_Crafts 1.14.1 (Event) et 1.15.0 (Kixster). À tester en jeu sur Kixster : craft du verrou, pose sur chaque véhicule, refus d'ouverture / de casse / d'entonnoir, passager sur le siège invisible (hauteur, Java et Bedrock), ancrage (piston, eau, poussée), menu du propriétaire (ouvrir, retirer), ouverture d'un âne / lama depuis le menu, déplacement par un membre de claim.

**Ajout (14/10/2026, 16:12) : KS_VerrouVehicule 1.0.0 (nouveau plugin) et KS_Crafts 1.15.0 compilés, non déployés, non testés : verrou de véhicule.** Demande de LeKiwi06 : « un item pour verrouiller le contenant d'un véhicule, histoire qu'on puisse le garer même hors claims et qui ne soit pas cassable, déplaçable, vidable par hopper, etc. tant que le contenant n'est pas vide ». Ses choix (questions posées avant le code) : bateaux et radeaux à coffre, wagonnets à coffre et à entonnoir, bateau à coffre et jukebox, âne / mule / lama à coffre ; ouverture par le propriétaire seul ; les autres montent sans conduire, le propriétaire qui monte prend la place du conducteur ; déplacement permis seulement au propriétaire, et au propriétaire et aux membres du claim où le véhicule se trouve ; verrou qui reste posé, retiré par un petit menu avant le coffre ; crochet vanilla renommé ; recette crochet + 4 lingots de fer (KS_Crafts 1.15.0) ; Kixster seulement. Techniquement nécessaire : siège invisible (porte-armure) pour monter sans conduire (lu dans le code du jeu 26.3), véhicule ancré (revient à sa place s'il est poussé). Écrit sans serveur de test : hauteur du passager assis, ouverture d'un âne depuis le menu et comportement sur Bedrock à vérifier en jeu. Choix et limites : `KS_VerrouVehicule/JOURNAL.md`. Réservations de KS_VerrouVehicule et KS_Crafts gardées jusqu'au déploiement.

**Ajout (14/10/2026, 16:10) : KG_BingoGame 0.12.0 déployé sur Serveur Jeux, bonus de temps du contre la montre ; non testé, actif après redémarrage ; réservation libérée.** Demande de LeKiwi06 : une grille de 25 objectifs difficiles finie en 1 h 30 en solo devrait rapporter « aux alentours de 1800 » (avant : 1 127 ; la même grille en blackout donne 1 760). Formule choisie par LeKiwi06 : chaque minute restante au chrono vaut la valeur moyenne d'un objectif de la grille (difficiles seulement : 10 par minute → 1 705 ; grille par défaut : 5,2 par minute ; faciles seulement : 2). Réservé en 3e plugin de 13 h à 23 h avec l'accord de Maxster33 (selon LeKiwi06). La 0.11.0 (bouton « Rejouer ») n'était pas testée non plus. Détail : `KG_BingoGame/JOURNAL.md`, section 0.12.0. Dossiers supprimables sur Serveur Jeux (3 versions ou plus derrière la 0.12.0) : `_removed-kg_bingogame-0.8.0` à `0.8.7` et `0.9.0` (garder `0.10.0` et `0.11.0`) ; suppression par l'humain.

**Ajout (13/10/2026, 16:40) : KS_Economy 1.9.0, KS_Teleport 1.3.0 et KS_Claim 1.4.0 actifs sur Kixster, journal lu.** Kixster a redémarré le 13/10 à 16:36 : les trois plugins chargés et activés sans erreur ni avertissement (KS_Teleport avant KS_Claim), 49 plugins, démarrage en 15 s ; anciennes versions arrêtées proprement à 16:36:32. `teleport.yml` inchangé (maison de Maaxster). Aucun joueur connecté après le redémarrage au moment de la lecture : rien n'a encore servi, reste à tester en jeu.

**Ajout (13/10/2026, 16:30) : KS_Economy 1.9.0, KS_Teleport 1.3.0 et KS_Claim 1.4.0 déployés sur Kixster ; non testés, actifs après redémarrage ; réservations libérées.** Accord de LeKiwi06 : « Kixster seulement ». Avant l'envoi : les trois jars en place identiques aux références ; après : jars du serveur identiques à ceux compilés, anciens jars dans leurs dossiers `_removed-…`. Aucun `config.yml` modifié. **Event reste en KS_Economy 1.8.2, KS_Teleport 1.2.0 et KS_Claim 1.3.0** : `jars-deployes/` garde donc les deux séries. À tester en jeu sur Kixster : `/magasin create` à 450, les trois agrandissements, `/setspawn` (Java et Bedrock), magnétite incassable, claim gratuit rendu dans `/ksclaims` (Maaxster).

**Ajout (13/10/2026, 16:15) : magasin moins cher et à tiers, `/setspawn`, claim de la maison rendu ; KS_Economy 1.9.0, KS_Teleport 1.3.0 et KS_Claim 1.4.0 compilés, non déployés, non testés.** Demande de LeKiwi06 (plusieurs problèmes, source Maaxster) : « maison create ne rend pas le claim », magasin à 450 au lieu de 45 000, agrandissement à 10x²+450x-50 avec 3 agrandissements au plus (+2, +3, +5 boutiques, tier 4 = 20), une boutique et une maison par joueur, `/setspawn` avec la boussole de la magnétite en main. Journal de Kixster lu (lecture seule, hors dépôt) : Maaxster a déclaré sa maison le 13/10 à 15:48 pour 500 points, sans erreur ; rien n'était prévu pour le claim. Quatre questions posées avant le code, réponses de LeKiwi06 : le claim de la maison ne compte plus (maison seulement) ; `/setspawn` remplace `/maison position` (`/maison create` garde l'endroit où l'on se tient) ; magnétite indestructible ; création payable aussi avec 5 blocs tier 1. Déjà en place, relu dans le code : un magasin et une maison par joueur. Choix de Claude signalés : tier affiché dans le menu du magasin ; magnétite de nouveau cassable si son claim n'est plus au joueur ; supprimer le claim de la maison ne rembourse rien. Non changé : un agrandissement demande toujours un autre claim de la zone. Les `config.yml` de Kixster et d'Event n'ont pas de section `magasins` : rien à modifier à la main. KS_Teleport en « Utilisés actuellement », KS_Economy et KS_Claim en « Requis parfois » (16 h : limite de 2 plugins « utilisés » atteinte avec KG_BoatRace). Détail : `JOURNAL.md` des trois plugins, `historique-conversations/2026-10-13-magasin-maison-setspawn.md`.

**Ajout (13/10/2026, 14:35) : bateaux à jukebox actifs sur Kixster, journal lu ; erreur de WorldGuard avec les bateaux en peuplier.** Kixster a redémarré le 13/10 à 14:11 : KS_JukeboxBoat 1.1.2, KS_Crafts 1.14.1 (« 78 crafts ajoutés ») et KS_KaliumGive 1.11.0 chargés et activés sans erreur, 49 plugins, démarrage en 15 s. **Point ouvert (Kixster et sans doute Event) : WorldGuard 7.0.19 ne connaît pas le bois de la 26.3 (peuplier).** À 14:13, une erreur « Could not pass event PlayerInteractEvent to WorldGuard » (`NullPointerException` dans `SpawnEntityEvent`, appelé depuis `BoatItem.use`) : sa table des bateaux (lue dans son jar) s'arrête au chêne pâle, alors que la liste des bateaux du jeu contient `poplar_boat` et `poplar_chest_boat`. Chaque pose d'un bateau en peuplier écrit cette erreur et n'est pas contrôlée par WorldGuard (sans conséquence pratique : KS_Claim 1.3.0 rend déjà les bateaux posables partout). Non corrigé : à régler par une version de WorldGuard qui gère la 26.3. Statut des trois plugins : vérifiés dans la console, non testés en jeu sur Kixster.

**Ajout (13/10/2026, 14:07) : bateaux à jukebox déployés sur Kixster ; non testés sur Kixster, actifs après redémarrage ; réservations libérées.** Accord de LeKiwi06 : « tu peux déployer sur kixster ». Envoi groupé : KS_JukeboxBoat 1.1.2 (nouveau sur Kixster), KS_Crafts 1.14.1 (à la place de la 1.12.0), KS_KaliumGive 1.11.0 (à la place de la 1.10.0). Avant l'envoi : les deux jars en place identiques aux références, KLM_Menu 2.11.0 présent, aucun KS_JukeboxBoat. Réservés en « Requis parfois » pour ce déploiement groupé (14 h : la limite de 2 plugins « utilisés » était atteinte). **GrimAC de Kixster n'est pas modifié** : on y est éjecté d'un bateau tiré par un Happy Ghast (seul Event a le réglage global). **KS_KaliumGive d'Event reste en 1.10.0** : `jars-deployes/` garde donc les deux jars. Statut sur Event : bateaux vus en jeu par LeKiwi06 (1.0.0 à 1.1.0 testées ; taille de la 1.1.2 et KS_KaliumGive 1.11.0 non confirmées en toutes lettres).

**Ajout (13/10/2026, 11:05) : KS_KaliumGive 1.11.0 compilée, non déployée, non testée : bateaux à jukebox dans `/kaliumgive`.** Demande de LeKiwi06 : « met les dans le kaliumgive aussi ». Un id par bateau du jeu, `jukebox_<id du bateau>` (ex. `jukebox_oak_boat`, `jukebox_oak_chest_boat`), objets créés par KS_JukeboxBoat. Réservation gardée jusqu'au déploiement.

**Ajout (13/10/2026, 10:50) : KS_JukeboxBoat 1.1.2 et KS_Crafts 1.14.1 actifs sur Event, journal lu.** Event a redémarré le 13/10 à 10:46 (08:46 UTC dans son journal ; une première lecture à 10:42 montrait qu'il n'avait pas encore redémarré) : KS_JukeboxBoat 1.1.2 et KS_Crafts 1.14.1 chargés et activés sans erreur, « 78 crafts ajoutés » (89 en 1.14.0 : les 11 recettes en double ont disparu), GrimAC activé, 49 plugins, démarrage en 12 s, aucune exception. La 1.1.1 n'a jamais tourné. Statut : vérifié dans la console, non testé en jeu.

**Ajout (13/10/2026, 10:39) : KS_JukeboxBoat 1.1.2 déployée sur Event ; non testée, active après redémarrage ; réservation libérée.** Accord de LeKiwi06 : « oui déploie la 1.1.2 sur event ». Avant l'envoi : jar en place identique à la référence, `config.yml` du plugin identique à celui de la 1.1.1 ; il est remplacé par celui de la 1.1.2 (jukebox de 0,75 bloc, comme le coffre).

**Ajout (13/10/2026, 10:50) : KS_JukeboxBoat 1.1.2 compilée, non déployée, non testée : jukebox de la taille exacte du coffre.** Demande de LeKiwi06 : « si tu connais la taille du coffre, est-ce que tu peux faire en sorte que le jukebox fasse pile la même taille ? ». Le coffre d'un bateau à coffre mesure 0,75 bloc de côté (modèle du jeu 26.3 lu sur le PC) : `jukebox.taille` 0.75, positions refaites (bateau à coffre : contre la serrure du coffre ; bateau simple : à la place qu'aurait le coffre). Réservation gardée jusqu'au déploiement.

**Ajout (13/10/2026, 10:34) : KS_JukeboxBoat 1.1.1 et KS_Crafts 1.14.1 déployés sur Event ; non testés, actifs après redémarrage ; réservations libérées.** LeKiwi06 : « oui déploie la 1.14.1 sur event, j'ai testé c'est juste un bug visuel coté client les laisses. il faudrait que le jukebox soit a peut près 30% plus gros dans le bateau, je le trouve trop petit par rapport au coffre » ; à la question « sur quel bateau ? » (envoi annoncé avec la 1.14.1) : « Les deux bateaux ». KS_JukeboxBoat 1.1.1 : jukebox de 0,72 bloc au lieu de 0,55, replacé en conséquence (valeurs calculées, non vues en jeu). Avant l'envoi : les deux jars en place identiques aux références, `config.yml` du plugin identique à celui de la 1.1.0. Laisses figées : sans suite, défaut d'affichage du client (constat de LeKiwi06).

**Ajout (13/10/2026, 10:15) : journal d'Event lu après redémarrage ; KS_Crafts 1.14.1 compilée, non déployée.** Dernier démarrage d'Event le 13/10 à 10:07 (08:07 UTC dans son journal) : KS_JukeboxBoat 1.1.0 et KS_Crafts 1.14.0 chargés et activés sans erreur, GrimAC activé sans erreur avec son nouveau `config.yml`, 49 plugins, démarrage en 12 s, aucune exception. Défaut trouvé par Claude : « 89 crafts ajoutés » au lieu de 88, car la liste des bateaux du jeu contient aussi les bateaux à coffre (et la 26.3 a un 11e bois) : la recette du bateau à coffre et jukebox est en double dans le livre de recettes (sans autre effet). Corrigé dans KS_Crafts 1.14.1 (compilée, non déployée). Détail : `KS_Crafts/JOURNAL.md`.

**Ajout (13/10/2026, 08:30) : GrimAC d'Event ne renvoie plus les joueurs pour un déplacement anormal (réglage global choisi par LeKiwi06) ; réservations libérées.** But : pouvoir monter dans un bateau tiré par un Happy Ghast. Constat (jar de GrimAC 2.3.74-abb95b6 lu hors dépôt) : à chaque renvoi d'un joueur assis dans un véhicule, GrimAC envoie au client une fausse position du véhicule et éjecte tous ses passagers ; son `config.yml` n'a aucun réglage propre aux véhicules ou aux laisses. Trois voies proposées à LeKiwi06 : exemption ciblée dans KS_AntiCheat (permission `grim.nosetback` donnée pendant qu'un joueur est dans un bateau tenu en laisse, recommandée), réglage global, ne rien changer. **Son choix : « Réglage global ».** Fait à 08:25 sur Event : dans le bloc `Simulation` du `config.yml` de GrimAC, `immediate-setback-threshold: -1` et `max-advantage: -1` (lu dans le code : -1 = jamais de renvoi par cette vérification). **Conséquence : sur Event, la vitesse et le vol anormaux ne sont plus corrigés par GrimAC pour aucun joueur ; ils restent signalés** (KS_AntiCheat : alerte légère à 10 signalements « Simulation » d'écart 0,05 ou plus en 10 minutes ; pas de suspension automatique, « Simulation » n'est pas dans `grimac.lourdes`). Les autres vérifications de GrimAC (Reach, Timer, Phase...) sont inchangées. Un joueur dans un bateau tiré par un Happy Ghast déclenchera donc des alertes légères. Actif après redémarrage d'Event ou `/grim reload`. Non testé. Kixster : GrimAC inchangé (à décider au moment d'y déployer le bateau à jukebox ; l'exemption ciblée reste possible).

**Ajout (13/10/2026, 08:25) : KS_JukeboxBoat 1.1.0 et KS_Crafts 1.14.0 déployés sur Event ; non testés, actifs après redémarrage.** Accord de LeKiwi06 : « tu peux configurer grimAC, et déployer le nouveau jar ». Envoi à 08:21 ; avant l'envoi, les deux jars en place vérifiés identiques aux références (téléchargés et comparés) et `plugins/KS_JukeboxBoat/config.yml` identique à celui fourni avec la 1.0.0 (aucun réglage fait à la main) : remplacé par celui de la 1.1.0. Essai de LeKiwi06 avec un bateau vanilla accroché à un Happy Ghast : « le bateau normal m'ejecte aussi, mais les laisses n'ont pas buggué » : la descente vient donc bien de GrimAC et non du plugin ; les laisses figées de sa capture ne se sont pas reproduites, LeKiwi06 ne veut pas qu'on s'y attarde tant que cela ne revient pas. Réglage de GrimAC : voir l'ajout suivant ou le point ouvert.

**Ajout (13/10/2026, 08:15) : KS_JukeboxBoat 1.1.0 et KS_Crafts 1.14.0 compilés, non déployés, non testés : bateau à coffre et jukebox, jukebox fantôme corrigé.** LeKiwi06 a testé la 1.0.0 sur Event : « je l'ai testé en jeu, ça fonctionne bien, si je le casse alors qu'il joue une musique il rend le disque etc ». Nouvelle demande : « un bateau avec coffre et jukebox ? on ne peux pas monter dedans, mais on peux le tracter avec une laisse [...] le juke box lirait en automatique les disque présent dans le coffre en boucle » ; ses choix : les deux bateaux existent, accroupi + clic ouvre le coffre, clic normal alterne lecture et pause, recette bateau à coffre + jukebox. Défaut signalé par lui pendant le code : en montant dans un bateau à jukebox tiré par un Happy Ghast, il est aussitôt descendu et un jukebox fantôme reste en l'air. Enquête (journal d'Event, base de GrimAC, code du jeu 26.3 lu sur le PC) : la descente vient de **GrimAC** (3 signalements « Simulation » à 08:00, il éjecte tous les passagers du bateau), pas du plugin ni du jeu ; le fantôme était un défaut du plugin (jukebox éjecté jamais repris), corrigé en 1.1.0. **Point ouvert, non traité : GrimAC fait descendre un joueur d'un bateau tiré par un Happy Ghast** (à vérifier avec un bateau vanilla ; réglage de GrimAC à décider avec Maxster33). Question de LeKiwi06 restée ouverte : afficher le jukebox aux joueurs Bedrock avec un porte-armure invisible coiffé d'un jukebox (essai en jeu à faire par lui). Réservations de KS_JukeboxBoat et KS_Crafts gardées jusqu'au déploiement.

**Ajout (13/10/2026, 07:50) : KS_JukeboxBoat 1.0.0 et KS_Crafts 1.13.0 actifs sur Event, journal lu.** Event a redémarré le 13/10 à 07:44 (05:44 UTC dans son journal) : KS_JukeboxBoat 1.0.0 et KS_Crafts 1.13.0 chargés et activés sans erreur, « 78 crafts ajoutés », pas d'avertissement « KS_JukeboxBoat absent » (la recette du bateau à jukebox est donc enregistrée), `config.yml` et `lang.yml` de KS_JukeboxBoat créés ; 49 plugins, démarrage en 12 s, aucune erreur ni exception dans le journal. Statut : vérifié dans la console, non testé en jeu.

**Ajout (13/10/2026, 07:45) : KS_JukeboxBoat 1.0.0 et KS_Crafts 1.13.0 déployés sur Event ; non testés, actifs après redémarrage ; réservations libérées.** Accord de LeKiwi06 : « oui déploie sur event ». Envoi à 07:39 ; avant l'envoi, `KS_Crafts-1.12.0.jar` en place vérifié identique à la référence (téléchargé et comparé), KLM_Menu 2.11.0 présent, aucun KS_JukeboxBoat ; après l'envoi, un seul jar de KS_Crafts dans `/plugins/` (1.13.0), tailles identiques à celles de `sortie/`. Kixster n'est pas touché (KS_Crafts 1.12.0, pas de bateau à jukebox) : `jars-deployes/` garde donc les deux jars de KS_Crafts. À tester sur Event après redémarrage (liste : `historique-conversations/2026-10-13-bateau-a-jukebox.md`), puis déploiement sur Kixster.

**Ajout (13/10/2026, 06:50) : KS_JukeboxBoat 1.0.0 (nouveau plugin) et KS_Crafts 1.13.0 compilés, non déployés, non testés : bateau à jukebox.** Demande de LeKiwi06 : « j'aimerai créer un plugin de bateau avec jukebox, un bateau très similaire au bateau avec coffres, mais là il peut lire les disques de notre inventaire ». Ses choix (questions posées avant le code, menu jugé sur maquette) : bateau simple avec un jukebox visible à l'arrière sur Java (modèle 3D de bloc, 2e place prise ; les joueurs Bedrock voient un bateau vide), musique pour tous les joueurs proches et qui suit le bateau, disque rangé dans le bateau (rendu à l'arrêt, lâché si le bateau est cassé), craft bateau + jukebox pour tous les bois, menu en coffre (disques de l'inventaire en haut, disque du bateau et « Fermer » en bas), ouverture à pied par accroupi + clic droit et à bord par la touche de saut, nom KS_JukeboxBoat. La recette est dans KS_Crafts 1.13.0 (empilée sur la 1.12.0 jamais testée, car nécessaire) : les deux jars sont à déployer ensemble. Écrit sans serveur de test : position du jukebox (`config.yml`), touche de saut et musique sur Bedrock à vérifier en jeu. Limites et choix signalés : `KS_JukeboxBoat/JOURNAL.md`. Réservations de KS_JukeboxBoat et KS_Crafts gardées jusqu'au déploiement.

**Ajout (13/10/2026, 05:40) : KS_Economy 1.8.2 active sur Kixster, journaux et `comptes.yml` lus.** KS_Economy 1.8.2 y est active depuis le redémarrage du 12/10 à 05:15 (4 démarrages depuis, le dernier le 13/10 à 05:32), « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur. `comptes.yml` : les 8 comptes portent la ligne `gagne`. Compteur vérifié sur les ventes du journal : une vente de 312 points aux rachats de la semaine (12/10, 13:40) a monté le solde et le compteur du vendeur de 745 à 1 057 ; des ventes au marchand ambulant (13/10, 03:29) sont comptées aussi ; un compte dont le solde a monté sans vente a un compteur resté plus bas (421 pour 455). Personne ne dépasse 2 500 gagnés : le quota lui-même n'a encore servi à personne. Statut : vérifié dans la console et les fichiers, non testé en jeu. Event : à lire après son redémarrage.

**Ajout (12/10/2026, 05:15) : KS_Economy 1.8.2 déployée sur Kixster et Event ; non testée, active après redémarrage ; réservation libérée.** Accord de LeKiwi06 : « oui déploie tout ». Envoi à 05:13, jars en place vérifiés identiques aux références avant l'envoi. Au redémarrage, chaque compte reçoit son compteur `gagne` (égal à son solde) dans `comptes.yml`.

**Ajout (12/10/2026, 05:12) : KS_Economy 1.8.2 recompilée avec un compteur des points gagnés ; non déployée, non testée.** LeKiwi06 : « oui il faut mettre un compteur d'argent gagné, sinon les joueurs vont juste retirer les émeraudes en items pour outrepasser le quota ». Ses choix : seules les ventes au serveur comptent (rachats de la semaine, marchand ambulant) ; le compteur des comptes existants part de leur solde. Le quota (seuil 2 500, 0,5 %, 0,75 % masqué) porte sur ce compteur, plus sur le solde. Détail : `KS_Economy/JOURNAL.md`.

**Ajout (12/10/2026, 05:08) : KS_Economy 1.8.2 compilée, non déployée, non testée : quota des rachats.** Demande de LeKiwi06 : « que le quota débute à 2500 gagné, pas 25000, et que le quota par item soit de 0,5 % de notre monnaie gagnée, et 0,75 % avec le solde masqué ». Valeurs par défaut changées (les `config.yml` des serveurs n'ont pas de section `rachats`). Le quota reste calculé sur le solde du joueur (signalé à LeKiwi06 : « monnaie gagnée » compris comme la cagnotte).

**Ajout (12/10/2026, 02:45) : KS_Economy 1.8.1 active sur Kixster (redémarrage de 01:33) et sur Event (redémarrage de 01:42), journaux lus, aucune erreur.** Offres au prix modifié remplacées : `mushroom_stem` sur Kixster ; `straw_bed`, `string`, `raw_copper` sur Event. Vérifié dans la console, non testé en jeu. À savoir : jusqu'à l'arrêt du 12/10 à 00:33, Kixster tournait encore avec les versions d'avant le 11/10 (KS_Economy 1.5.0, KS_RewardsGUI 1.4.1...) : les déploiements du 11/10 n'y sont actifs que depuis le redémarrage de 01:25.

**Ajout (12/10/2026, 01:35) : KS_Economy 1.8.1 déployée sur Kixster et Event ; non testée, active après redémarrage ; réservation libérée.** Accord de LeKiwi06 : « oui déploie tout ». Envoi à 01:32, jars en place vérifiés identiques aux références avant l'envoi. Au redémarrage : sur Kixster l'offre du pied de champignon est remplacée ; sur Event (encore en 1.7.0) `straw_bed`, ficelle et cuivre brut sont remplacés.

**Ajout (12/10/2026, 01:31) : KS_Economy 1.8.1 compilée, non déployée, non testée : pied de champignon doublé (0,03).** Demande de LeKiwi06 : « il faut donc doubler le prix des tiges de champignon » (plus pénibles à obtenir que les blocs marron et rouge). Event n'a pas redémarré depuis le 11/10 à 17:36 (journal lu à 01:29) : il tourne encore en 1.7.0, le jar 1.8.0 en place attend le redémarrage.

**Ajout (12/10/2026, 01:30) : KS_Economy 1.8.0 active sur Kixster, journal lu.** Redémarrage de 01:25 : KS_Economy 1.8.0 active, « Barème : 1929 objets (0 inconnus de cette version) », aucune erreur. Ligne attendue présente : « Rachats de la semaine 2026-S42 : objets au prix modifié remplacés (waxed_oxidized_chiseled_copper, dripstone_block, brown_egg, cinnabar_brick_wall, diorite_wall) » ; les 5 autres offres sont gardées, les 5 nouvelles sont `golden_apple`, `potion(tissage)`, `ladder`, `mushroom_stem`, `deepslate_lapis_ore`. `rachats.yml` : les 10 offres portent leur prix du barème. `comptes.yml` : soldes corrigés conservés après le redémarrage. Statut : vérifié dans la console, non testé en jeu. Event : pas encore redémarré.

**Ajout (12/10/2026, 01:25) : points du cinabre retirés sur Kixster.** Demande de LeKiwi06 : « retire aussi les points obtenus via le cinabre », puis « kixster est arrêté, retire tout ». 316 points retirés de `plugins/KS_Economy/comptes.yml` (3 soldes : - 250, - 57, - 9 ; aucun n'est passé sous zéro ; montants de `rachats.yml` recoupés avec le journal du serveur ; les murets vendus ne sont pas rendus) ; ancien fichier dans `/plugins/_removed-ks_economy-comptes-2026-10-12/` ; fichier relu identique après l'envoi. Kixster était arrêté depuis 00:33 : aucune vente de cinabre après le relevé. Rien à retirer sur Event (aucune vente de cinabre). KS_Economy 1.8.0 s'active au redémarrage de Kixster.

**Ajout (12/10/2026, 01:20) : KS_Economy 1.8.0 déployée sur Kixster et Event ; non testée, active après redémarrage ; réservation libérée.** Accord de LeKiwi06 : « oui déploie tout ». Envoi à 01:17, jars en place vérifiés identiques aux références avant l'envoi. Au redémarrage, les offres de la semaine dont le prix a changé sont remplacées (console : « objets au prix modifié remplacés »). La perle de l'Ender reste à 0,5 (décision de LeKiwi06). **Reste à faire** : retirer les points gagnés avec le cinabre sur Kixster (demande de LeKiwi06 ; 316 points, 3 joueurs, relevés dans le journal du serveur) : `comptes.yml` à modifier serveur éteint ; `butin.yml` des coffres de récompense non régénéré ; silex à 2,9 fois le gravier.

**Ajout (12/10/2026, 01:15) : KS_Economy 1.8.0 compilée, non déployée, non testée : suite du barème et remplacement des offres au prix modifié.** Réponses de LeKiwi06 : « catégorie 1 à 4 et le reste de la 6 validé » (liste d'anomalies du 12/10) ; coussin = 1,5 laine, `straw_bed` = 0,75 botte de foin ; carte au trésor à 10 ; colorant blanc au prix de la poudre d'os ; « il faut refaire un tirage d'économie pour les items qui ont été modifiés, on tire un nouvel objet à la place ». 678 prix changent par rapport au barème en service. Une offre de la semaine au prix modifié est remplacée au démarrage (jamais par le même objet). Restent ouverts : perle de l'Ender / endermite (améthyste bourgeonnante à 250), silex 2,9 fois le gravier, `butin.yml` des coffres de récompense à régénérer ou non. Détail : `KS_Economy/JOURNAL.md`.

**Ajout (12/10/2026, 01:30) : KS_Economy 1.7.1 compilée, non déployée, non testée : barème corrigé après les premiers rachats.** Signalement de LeKiwi06 : « le cinabre est bien trop boosté, il vaut autant que du fer ». Cause : note provisoire jamais vérifiée (0,0764 pour un fer à 0,10). Ses décisions : cinabre et soufre 0,01 ; butin de monstres et poissons divisé par 2 ; disque Bounce au prix du Relic ; cartes de structure à 10 ; cuivre brut au prix du lingot ; coussins et `straw_bed` au prix de leur recette. 113 prix changent, aucun code. Les rachats déjà tirés gardent leur prix (`rachats.yml`). En attente de sa décision : le reste de la liste d'anomalies du 12/10 (œufs, poudre d'os, blocs abondants, sculk). `butin.yml` des coffres de récompense non régénéré. Détail : `KS_Economy/JOURNAL.md`. Réservation gardée jusqu'au déploiement.

**Ajout (11/10/2026, 10:50) : Event mis au niveau de Kixster ; sur Kixster : réapparition sans lit, bateaux et wagonnets, chauve-souris, CoreProtect ; proxy : objets empilables sur Bedrock ; non testés, actifs après redémarrage ; réservations libérées.** Réponses de LeKiwi06 du 11/10 : pas de plafond pour l'exceptionnelle de Kanvas (« le niveau d'investissement requis [...] la mérite ») ; « oui fix le soucis pour tout les items , et pense a le faire a l'avenir » (pile sur Bedrock) ; « oui met event a jour par rapport a kixster » ; « rend les bateaux , minecraft , etc posable et cassable dans les zones protégés et claims , rajoute core protect » ; île du dragon : il la fera à l'ouverture de l'End ; toit du spawn : « je ne sais pas comment fix le soucis » ; « la night vision doit se drop a 25% de chance sur les chauve souris » ; éclat d'écho des chauves-souris attendu à 10 % (jamais codé jusque-là). Fait et envoyé à 10:47 : KS_Teleport 1.2.0, KS_Claim 1.3.0, KS_LootPotions 1.2.0, KS_LootEntites 1.5.0 (Kixster et Event), CoreProtect CE 24.1 copié d'Event vers Kixster, Event aligné sur Kixster (14 jars + `fusion.yml`), correspondance Geyser sur le proxy. Choix de Claude signalés : véhicules à inventaire (bateau à coffre, wagonnet à coffre ou à entonnoir) non cassables chez les autres ; wagonnet à TNT non posable en zone protégée. Proposition d'équilibrage des potions des mobs présentée à LeKiwi06, en attente de sa validation (seule la chauve-souris est codée). **À faire par LeKiwi06 : redémarrer Event, Kixster, Kanvas, Kal-Games et le proxy.**

**Ajout (11/10/2026, 09:05) : tout ce qui attendait est déployé (Kixster, Kanvas, Kal-Games, proxy) ; non testé, actif après redémarrage de chaque serveur ; réservations libérées.** Accord de LeKiwi06 : « oui déploie tout ». Envoi à 09:00. Kixster : KS_Meule 1.1.0, KS_KaliumGive 1.10.0, KS_Economy 1.7.0, KS_Tableau 1.0.1, `fusion.yml`. Kanvas : KV_Plots 1.7.0, KV_Menu 1.4.0, `butin.yml`. Kal-Games : `butin.yml`. Proxy : `kalium_objets.json` et `KaLium-objets-1.4.0.mcpack`. Vérifié avant : jars en place identiques à `jars-deployes/` ; `butin.yml` et `fusion.yml` des serveurs identiques à ceux envoyés le matin ; correspondance du proxy identique à la 1.3.0 du dépôt. Vérifié après : un seul jar par plugin, bonnes tailles ; un seul pack sur le proxy. Event n'a rien reçu depuis le matin (non demandé). **À faire par LeKiwi06 : redémarrer Kixster, Kanvas, Kal-Games et le proxy** (le proxy déconnecte tout le réseau). À lire ensuite dans les consoles : Kixster : « Fonte des armures : 6 recettes du jeu remplacées » (KS_Crafts), « fusion.yml : 517 entrées dans 5 pools, 213 réglages » (KS_RewardsGUI), KS_Meule activé ; Kanvas et Kal-Games : aucune ligne « objet inconnu » de KV_Rewards / KG_Rewards ; proxy : Geyser charge 16 objets custom sans erreur. Restent à décider par LeKiwi06 : équilibrage du drop des potions des mobs (demande interrompue) ; plafond de l'exceptionnelle de Kanvas ; les 5 autres objets empilables sur Bedrock ; ces plugins sur Event ; points de Kixster signalés (WorldGuard et bateaux, CoreProtect, `ile_du_dragon`, réapparition sans lit).

**Ajout (11/10/2026, 08:49) : KV_Plots 1.7.0 revu avant déploiement : thème et titre trop longs défilent dans le tableau sur le côté.** Demande de LeKiwi06 : « pour les lignes trop longues comme un thème de build , rend le déroulant de la meme façon que la boss bar dans le lobby ». Même réglage que KLM_Hub (un caractère tous les 3 ticks, arrêt de 3 secondes au début du texte) ; le tableau n'est plus recréé toutes les 2 secondes. Même numéro 1.7.0 (jamais déployé). Compilé, non déployé, non testé.

**Ajout (11/10/2026, 08:35) : concours de build simultanés (KV_Plots 1.7.0, KV_Menu 1.4.0) et tableaux latéraux plus courts (KV_Plots 1.7.0, KS_Tableau 1.0.1) ; compilés, non déployés, non testés.** Demande de LeKiwi06 : « fait en sorte qu'on puisse avoir un concours de build en cours pendant que le précédent est en phase de votes , essaie aussi de reduire tout les sidebar trop long , sur kanvas , mais aussi pour kixster " exploration journalière : 10 / 10 " est bien trop long " exploration : 0/10 " suffit ». Un seul concours en construction à la fois, mais il peut être lancé pendant les votes du précédent ; menus et gestion du staff par concours ; `concours.yml` inchangé. Tableaux : Kanvas (« Concours : », « Fin : 2 j 3 h », « Fin des votes : », « Note : », « État : travaux ») ; Kixster (« Exploration : n/10 », nouvelle clé de `lang.yml`). LeKiwi06 avait dit « oui déploie tout » puis « att stop » juste avant : rien n'a été envoyé. Réservations gardées.

**Ajout (11/10/2026, 08:21) : Kanvas : points de notes au carré (KV_Plots 1.6.0) et paliers recalés ; prêts, non déployés, non testés.** Remarque de LeKiwi06 : « les récompenses exceptionnelles permanente sont surpuissante » (Kanvas : 80 000 émeraudes, sur une hypothèse de Claude jamais validée). Sa règle : chaque note rapporte son carré, x2 sur un grand plot ; plot moyen en 4 à 5 h, grand plot en 10 à 15 h ; 10 à 20 votes par build. Calcul : 55 à 60 points par heure de build pour un bon bâtisseur, donc 0,12 émeraude par point au mois et 0,24 en permanent (avant 0,4 et 0,8) : paliers du mois 12 / 60 / 120 / 1 200 / 12 000, permanents 24 / 120 / 240 / 2 400 / 24 000. `butin.yml` de KV_Rewards régénéré hors dépôt et vérifié ; celui de KG_Rewards inchangé. Données de Kanvas lues en lecture seule : 27 votes, 3 ou 4 par plot ; avec la nouvelle règle le meilleur bâtisseur passe de 36 à 128 points (un palier « minimale »). Choix de Claude signalé : le score affiché d’un plot, le déblocage des tailles et les concours restent sur la somme des notes. Réservations gardées (KV_Plots, KV_Rewards).

**Ajout (11/10/2026, 08:07) : Cristal de purification : KS_Meule 1.1.0, KS_KaliumGive 1.10.0, tables de butin et geyser-bedrock 1.4.0 prêts, non déployés, non testés.** Demande de LeKiwi06 : « on va plutot faire un item pour enlever les malédictions au lieu d'une quantité d'xp , cet item sera droppable dans les reward, il aura une valeur théorique de 600 emeraudes , met let dans les pool adaptés ( [...] il faut également le déclarer au proxy pour que les joueurs bedrock le voient ) ». Fait : objet « Cristal de purification » (éclat d'améthyste brillant, pile de 64 ; nom et apparence choisis par Claude) ; à la meule, 1 cristal par malédiction ; id `cristal_purification` dans KS_KaliumGive ; pools : valeur 600, donc pool Légendaire (3,3 % d'un tirage Légendaire ; jeton de claim 9,0 → 6,7 %, Changeur de Biome 2,2 → 1,7 %, Clé de l'End 1,8 → 1,3 % ; moyenne du pool 1 138 → 1 109) ; `butin.yml` ×2 et `fusion.yml` régénérés hors dépôt et vérifiés (aucune erreur) ; classeur des valeurs régénéré ; `geyser-bedrock` 1.4.0 (correspondance + pack construit). Demande interrompue juste avant par LeKiwi06, non commencée : « équilibre mieux le drop des potions sur les différents mobs ». Réservations gardées jusqu'au déploiement.

**Ajout (11/10/2026, 07:51) : KS_Economy 1.7.0 compilée, non déployée, non testée : rachats du marchand ambulant.** Demande de LeKiwi06 : « j'aimerai que le wandering trader propose aussi 10 échanges , les moins cher ont echangeable jusqu'a 10 fois [...] diamant [...] 5 fois [...] totem [...] 1 exemplaire : les règles de trades sont les mêmes que pour les rachats de la semaine ». Ses réponses : le marchand achète ; menu en coffre et score ; un tirage par marchand, stock commun. Rachats gardés dans les données du marchand (aucun fichier). Choix de Claude signalés : paliers 10 / 10 / 5 / 2 / 1 par gamme de prix, un clic = un lot. Réservation gardée jusqu'au déploiement.

**Ajout (11/10/2026, 07:50) : sept plugins déployés sur Kixster ; non testés, actifs après redémarrage ; réservations libérées.** Accord de LeKiwi06 : « oui déploie tout sur kixster ». Envoi à 07:45 : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0, et `plugins/KS_RewardsGUI/fusion.yml`. Vérifié avant : les six jars remplacés étaient ceux de `jars-deployes/` (mêmes tailles), aucun dossier `_removed-…` du même nom, pas de `fusion.yml` sur le serveur. Vérifié après : un seul jar par plugin, tailles identiques aux jars compilés. Event n'a rien reçu (non demandé). **Non envoyé : `geyser-bedrock/custom_mappings/kalium_objets.json` sur le proxy** (jetons empilables sur Bedrock ; redémarrage du proxy nécessaire), en attente de la décision de LeKiwi06. À lire après le redémarrage de Kixster : console (KS_Crafts : « Fonte des armures : 6 recettes du jeu remplacées » ; KS_RewardsGUI : « fusion.yml : 516 entrées dans 5 pools, 213 réglages » ; KS_Meule activé). À tester en jeu : compte jamais connecté (arrivée au sol), `/tetes` avec un compte non opérateur, fusion de deux coffres (Java et Bedrock), `/accepter` à trois joueurs, fonte d'un casque et d'une pioche en fer, malédiction à la meule (accroupi, main vide).

**Ajout (11/10/2026, 07:27) : KS_Meule 1.0.0 revu avant déploiement : main vide, objet choisi dans l'inventaire.** Demande de LeKiwi06 : « fait en sorte que le menu soit uniquement a main nu et qu'on selectionne la pièce d'armure dans l'inventaire ( pas le stuff porté ) ». Le menu s'ouvre accroupi, main vide, sur une meule ; un clic sur un objet maudit de l'inventaire le choisit, puis les boutons de confirmation apparaissent. La version « objet en main » poussée plus tôt le même jour est remplacée (jamais déployée ; même numéro 1.0.0). Compilé, non déployé, non testé ; réservation gardée.

**Ajout (11/10/2026, 07:20) : KS_Meule 1.0.0 (nouveau plugin) compilé, non déployé, non testé : retirer les malédictions à la meule.** Question de LeKiwi06 : « est-ce qu'on a fait le plugin pour retirer les malédictions des items ? » : non, aucune trace (dépôt, cahiers des charges, liste d'origine). Sa demande : « il faut faire sneak+click sur une meule avec l'item concerné , cela coute 45 niveaux par malédiction ( interface de confirmation avant ) ». Fait : menu en coffre (objet au centre, un bouton par malédiction, « Tout retirer », « Annuler »), 45 niveaux par malédiction (`niveaux-par-malediction`), livres enchantés compris. Signalé, non fait : une armure de Liaison déjà portée ne peut pas être prise en main, donc pas traitée. Réservation gardée jusqu'au déploiement.

**Ajout (11/10/2026, 07:14) : KS_Jetons 2.0.2 compilée, non déployée, non testée : description du jeton de localisation.** Accord de LeKiwi06 (« oui corrige le texte ») : la description disait de ranger le jeton dans l'inventaire spécial, alors qu'il s'utilise en main. Les jetons déjà fabriqués gardent l'ancienne ligne. Réservation gardée jusqu'au déploiement.

**Ajout (11/10/2026, 07:09) : jetons non empilables sur Bedrock : correspondance Geyser corrigée dans le dépôt, non envoyée sur le proxy ; recette de la Clé de l’End : débloquée côté serveur.** Signalements de LeKiwi06 : « les joueurs bedrock ne peuvent pas stacker leurs jetons » ; « je n’ai pas débloqué le craft de la clé de l’end alors que j’ai eu plusieurs items du craft ». (1) Jetons : livres de connaissances dont le serveur porte la pile à 64 ; la correspondance Geyser ne le disait pas, donc pile de 1 sur Bedrock ; `"components": {"minecraft:max_stack_size": 64}` ajouté aux 5 jetons de `geyser-bedrock/custom_mappings/kalium_objets.json` (pack inchangé, 1.3.0). À envoyer sur le proxy (redémarrage du proxy). Même défaut signalé, non corrigé : Clé de l’End, Bedrock Breaker, Fragment et Cœur de Spawner, Changeur de Biome. (2) Clé de l’End : le fichier du joueur sur Kixster (lu en lecture seule) contient `ks_crafts:cle_de_l_end` parmi 28 recettes de KS_Crafts connues : le déblocage a bien eu lieu, c’est l’affichage qui est en cause (recette à 9 ingrédients : visible dans le livre de l’établi, pas dans celui de l’inventaire ; à confirmer par LeKiwi06).

**Ajout (11/10/2026, 07:03) : KS_Crafts 1.12.0 compilée, non déployée, non testée : une pièce d'armure fondue donne un lingot.** Demande de LeKiwi06 : « j'aimerai également que le fait de faire fondre des pieces d'armure donne un lingot et pas une pépite , ça redonnerai de l'utilité a cette fonctionnalité ». Les recettes de fonte du jeu (four et haut fourneau, fer, or, cuivre) sont lues au démarrage et remplacées chacune par deux recettes : pièces d'armure → lingot, le reste (outils, armes, armure de cheval) → pépite. Choix de Claude signalé : armure de cheval et outils non compris. À vérifier dans la console au démarrage : « Fonte des armures : 6 recettes du jeu remplacées ». Réservation gardée jusqu'au déploiement.

**Ajout (11/10/2026, 06:59) : KS_Economy 1.6.0 compilée, non déployée, non testée : `/accepter` pour les demandes d'échange.** Demande de LeKiwi06 : « il faut pouvoir faire /accepter pour accepter une demande d'échanger , c'est plus court , si le joueur à reçu des demandes d'échange de plusieurs joueurs , ouvrir un menu cliquable pour choisir quelle demande accepter ». Avant, une seule demande était gardée par joueur (la nouvelle effaçait l'ancienne) : une demande par demandeur maintenant ; menu « Demandes d'échange » (une tête par demandeur) quand il y en a plusieurs ; les boutons du message répondent à la demande de leur demandeur. `/echange refuser` sans nom refuse toutes les demandes (signalé). Réservation gardée jusqu'au déploiement sur Kixster et Event.

**Ajout (11/10/2026, 06:50) : KS_RewardsGUI 1.5.0 compilée, non déployée, non testée : fusion de deux coffres de récompense à l'enclume.** Demande de LeKiwi06 : « j'aimerai qu'on puisse fusionner 2 coffres de récompenses dans l'enclume ce qui additionnerait leurs valeurs , et couterait un nombre de point d'xp égale au nouveau montant en emeraude du coffres [...] le coffre s'appellera " coffre de récompense fusionné " ». Ses réponses : menu en coffre au clic droit sur l'enclume, bouton valider au centre de la rangée du bas avec le coût entre parenthèses, coffres déjà ouverts refusés ; nouveau tirage à la valeur totale. Le tirage se fait sur le serveur du joueur : nouveau fichier `plugins/KS_RewardsGUI/fusion.yml` (mêmes pools que `butin.yml` + réglages par valeur), généré hors dépôt par `sortie/rewards/generer_fusion.py` et vérifié (aucune erreur, moyenne du tirage = valeur du coffre à 0,6 % près). Sans ce fichier, la fusion est désactivée. Choix de Claude signalés dans `KS_RewardsGUI/JOURNAL.md` (coffre en main pour ouvrir le menu, nombre de tirages, coffres de tops et de rattrapage acceptés). À déployer : jar + `fusion.yml` sur Kixster (et Event), après la présentation à LeKiwi06.

**Ajout (11/10/2026, 06:27) : KS_Decapitator 1.2.0 compilée, non déployée, non testée : `/tetes` pour tous les joueurs.** Demande de LeKiwi06 : « j'aimerai aussi que les joueurs aient accès au /tetes mais que celui ci leur servent a savoir celle qu'ils ont déjà obtenus ( l'avoir eu dans son inventaire via un échange par exemple compte aussi ) ». Chaque joueur voit ses têtes obtenues (la tête, « Obtenue ») et les autres en vitre grise à leur nom (« Pas encore obtenue ») ; compte dans le titre ; une tête est notée dès qu'elle est dans son inventaire (ramassage, contrôle toutes les 5 secondes, connexion, déconnexion, ouverture du menu). Opérateurs : inchangé (toutes les têtes, clic = une tête). Choix de Claude signalés : nom des têtes manquantes visible, pas de message à l'obtention, pas de bouton de menu. Réservation gardée jusqu'au déploiement (Kixster, et Event si LeKiwi06 le veut).

**Ajout (11/10/2026, 06:20) : départ de Kixster SMP, journaux lus ; KS_Teleport 1.1.0 et KS_RewardsGUI 1.4.2 compilés, non déployés, non testés.** Question de LeKiwi06 : « as tu des remarques quant au départ de kixster SMP ? bugs , éléments manquants, points a améliorer ». Journaux de Kixster lus en lecture seule (09/10 19:52 au 11/10 05:30, 6 joueurs) : aucune erreur de nos plugins, aucun ralentissement. Constats : (1) les 6 nouveaux comptes apparaissent entre Y 118 et Y 123 au-dessus du spawn protégé (point d'apparition du monde : -54 73 212), 2 morts de chute à la première connexion ; (2) WorldGuard 7.0.19 plante à la pose d'un bateau sur Paper 26.3 (35 erreurs, sa protection ne s'applique pas à ce clic) ; (3) alertes GrimAC sur les joueurs Java seulement (alpha, montage avec ViaBackwards non pris en charge, retours en arrière actifs) ; (4) CoreProtect absent de Kixster ; (5) région `ile_du_dragon` absente de l'End (`zone_spawn` et `protected_spawn` existent) ; (6) `/reward` tapé par des joueurs ; (7) à chaque connexion d'un joueur Bedrock, 24 à 119 avertissements « moved too quickly » dans la même seconde, sans effet visible. Sur accord de LeKiwi06 (« oui fais les deux ») : KS_Teleport 1.1.0 pour (1) et KS_RewardsGUI 1.4.2 pour (6), réservations gardées jusqu'au déploiement sur Kixster. Les autres points attendent sa décision. Non fait, signalé : la réapparition après une mort sans lit suit la même règle du jeu que la première connexion.

**Ajout (11/10/2026, 06:10) : terre des âmes et sable des âmes déployés : KS_Economy 1.5.2 sur Event et Kixster, `butin.yml` sur Kal-Games et Kanvas ; non testés, actifs après redémarrage ; réservations libérées.** Accord de LeKiwi06 : « oui déploie », puis « je te donne la permission de le faire ». Envoi à 06:05. Vérifié avant : KS_Economy 1.5.0 était bien le jar en place sur les deux serveurs (240 796 octets, celui de `jars-deployes/`) ; les `butin.yml` des serveurs étaient identiques à ceux envoyés le 09/10. Vérifié après : un seul jar KS_Economy par serveur (241 318 octets), `butin.yml` de 66 516 octets (Kal-Games) et 65 670 octets (Kanvas). KS_Economy 1.5.2 contient la 1.5.1 (objets custom des rachats), jamais déployée seule. À tester en jeu : `/rachat` (objets custom montrés par leur vrai objet ; familles « terres » et « sable ») et `/kgrewards test` pour voir sortir les deux blocs. Journaux des redémarrages non lus.

**Ajout (11/10/2026, 01:55) : terre des âmes et sable des âmes : familles des rachats (KS_Economy 1.5.2) et butin des coffres de récompense ; compilé, prêt, non déployé, non testé.** Demande de LeKiwi06 : « ajoute la terre des ames aux terres , et le sables des ames aux sables », puis « Les deux » (rachats de la semaine et coffres de récompense). KS_Economy 1.5.2 : dans `rachats.csv`, `soul_soil` passe dans la famille « terres » et `soul_sand` dans la famille « sable » (la famille « sable des âmes » disparaît, 178 familles) ; prix inchangés, aucun changement de code ; contient la 1.5.1 jamais déployée. Coffres : `butin.yml` de KG_Rewards et de KV_Rewards régénérés hors dépôt (`sortie/rewards/butin/`) avec 4 entrées de plus dans la famille « Blocs » (4 x et 24 x de chacun des deux blocs, pools Commun et Peu commun) ; les fréquences de quelques niveaux bougent de 1 sur 1 000, valeurs moyennes inchangées ; vérifiés (aucune erreur). Avant l'envoi des `butin.yml` : comparer avec ceux des serveurs (le plugin réécrit le fichier quand une table est modifiée dans l'interface admin). Réservations gardées (KS_Economy, KG_Rewards, KV_Rewards) jusqu'au déploiement, en attente de l'accord de LeKiwi06.

**Ajout (10/10/2026, 00:25) : récompenses automatiques du 09/10 au soir à redonner sur Kixster ; fichier posé, commande à lancer par LeKiwi06.** Demande de LeKiwi06 : « donne les récompenses de ce soir sur kixster ». Journaux de Kal-Games lus : 16 récompenses automatiques sont parties vers la boîte d'Event entre 19:42 et 00:14 (Kal-Games n'a redémarré qu'à 00:19 avec `boite: kixster`) : LeKiwi06 : paliers de 900 points aux Piliers de la Fortune (mois et permanent, 19:42) et de 6 000 points au général (permanent, 00:10), 23 émeraudes ; valoux20 : top 50 % général (19:43), paliers de 100 points aux Piliers et au général (mois et permanent, 23:58), top 5 aux Piliers (00:03) et à Hide and Seek (00:13), paliers de 200 points au général (mois et permanent, 00:14), 19,5 émeraudes ; .MRMister7866 : palier de 5 000 points au Bingo (mois, 21:32), 10 émeraudes ; .PatientLime2170 : paliers de 500 points à Hide and Seek (mois et permanent) et de 8 000 points au général (mois), 00:10, 25 émeraudes. Aucune sur Kanvas. Pas de console : un `rattrapage.yml` posé sur Kal-Games à 00:23 contient un coffre par joueur (valeur moyenne de ses récompenses, 9 piles) plus le coffre de Maaxster, pas encore lancé à ce moment-là ; une seule commande envoie les 5 : `/kgrewards rattrapage kixster` puis `confirmer`. Choix de Claude signalés : un coffre par joueur au lieu d'un coffre par récompense (comme le rattrapage du 09/10) ; contenu tiré à nouveau ; les 16 récompenses restent aussi dans la boîte d'Event. Redémarrages lus (« j'ai restart les serveurs ») : Kixster 23:53 (KS_CoffreMort 1.0.3), Kanvas 23:53, Kal-Games 00:19, aucune erreur. Réservation libérée.

**Ajout (09/10/2026, 23:35) : récompenses automatiques dirigées vers Kixster ; envoyé, non testé, actif après redémarrage de Kal-Games et de Kanvas.** Question de LeKiwi06 : « les futures récompenses de palier etc seront données automatiquement maintenant ? ». Oui (paliers au franchissement, tops à la clôture, tops permanents toutes les 5 minutes), mais `boite: event` était resté dans le `config.yml` de KG_Rewards (Kal-Games) et de KV_Rewards (Kanvas) : quatre récompenses automatiques du 09/10 sont parties vers la boîte d'Event (LeKiwi06, deux paliers de 900 points aux Piliers de la Fortune, 19:42 ; valoux20, top 50 % général, 19:43 ; .MRMister7866, palier de 5 000 points au Bingo du mois, 21:32). Accord de LeKiwi06 (« oui passe les deux en boite kixster ») : les deux fichiers passés à `boite: kixster` à 23:32, relus à l'identique ; anciens fichiers dans `_removed-kg_rewards-config-2026-10-09/` et `_removed-kv_rewards-config-2026-10-09/`. Les quatre récompenses déjà parties restent sur Event (rien n'a été redonné). À 23:32, `rattrapage.yml` de Maaxster était toujours en place sur Kal-Games : commande pas encore lancée. Réservations libérées.

**Ajout (09/10/2026, 23:30) : KS_CoffreMort 1.0.3 sur Kixster (coffre de mort gardé 1 heure) et nouveau coffre de rattrapage pour Maaxster seul sur Kal-Games ; envoyés, non testés.** Demande de LeKiwi06 : « maxster a ragequit à cause du système de coffre de mort et a tout perdu, on va donc lui redonner un coffre de récompense comme celui de tout à l'heure du même montant que ce qu'il avait gagné, on va aussi étendre la longueur des coffres de morts à 1h au lieu de 15 minutes ». Journal de Kixster : Maaxster mort à 21:37:24 en X 13510 Z -13466 (36 piles), déconnecté à 21:38:25, coffre disparu à 21:52:25. KS_CoffreMort 1.0.3 + `config.yml` à 60 envoyés sur Kixster à 23:25 (actifs au prochain redémarrage ; un coffre déjà posé garde ses 15 minutes) ; Event non touché (« Non, Kixster seulement »). `rattrapage.yml` de Maaxster seul posé sur Kal-Games à 23:26, sans changement de code : LeKiwi06 lance `/kgrewards rattrapage kixster` puis `confirmer` ; le contenu est tiré à nouveau (même valeur moyenne, pas les mêmes objets). Réservations libérées. **Aussi le 09/10 (23 h) : « la canne à sucre ne pousse pas sur Kixster »** : aucun réglage ni plugin en cause (`cane-modifier` 100, hauteur 3) ; la règle `random_tick_speed` a été mise à 1 par LeKiwi06 à 20:11:30 (3 par défaut), donc tout pousse trois fois plus lentement (canne : environ 55 minutes par bloc) ; conseil donné : `/gamerule random_tick_speed 3`, réponse « c'est bon merci ». Vu dans le même journal, non traité : 23 erreurs WorldGuard 7.0.19 (`SpawnEntityEvent`, NullPointerException) quand un joueur pose un bateau. Détail : `historique-conversations/2026-10-09-coffre-de-mort-1-heure.md`.

**Ajout (09/10/2026, 20:25) : KS_Economy 1.5.1 compilée, non déployée, non testée : dans les rachats de la semaine, un objet custom montre son vrai objet au lieu d'une étoile du Nether.** Signalement de LeKiwi06 : « les items dans le rachat de la semaine sont buggué quand c'est des items custom on voit une nether star ». Cause : écrit ainsi en 1.5.0 (étoile pour les 361 objets custom du barème). Correction : objet créé par KS_KaliumGive (mêmes id que `/kaliumgive`), marque du plugin retirée, appel par réflexion (pas de `softdepend` : boucle KS_KaliumGive -> KS_Jetons / KS_Elixir -> KS_Economy) ; bloc de charbon de bois montré par un bloc de charbon. Jar : `sortie/KS_Economy-1.5.1.jar`. **En service : toujours la 1.5.0 sur Event et Kixster.** Déploiement reporté par LeKiwi06 (« il y a des joueurs, on fera ça après le stream de ce soir ») : KS_Economy reste réservé jusque-là. Détail : `KS_Economy/JOURNAL.md`, `historique-conversations/2026-10-09-rachats-objets-custom.md`.

**Ajout (09/10/2026, 19:48) : données des joueurs de Kixster remises à zéro (rangées, pas supprimées) ; redémarrage de 19:52 lu : aucune erreur, non testé en jeu au-delà de la connexion de LeKiwi06.** Demande de LeKiwi06 : « il faut supprimer les données de joueurs de tout les plugins sur kixster, pour qu'on commence tous de 0 », puis « Oui, tout » pour les données Minecraft (inventaire, coffre de l'Ender, XP, position, succès, statistiques). Constaté avant : 4 comptes seulement (LeKiwi06, Maaxster et leurs 2 comptes Bedrock), uniquement des données d'essai (80 coffres « Essai N » de KS_RewardsGUI, 1 claim, deux soldes de 384 et 450 dans KS_Economy). Kixster éteint par LeKiwi06 à 19:46:35 (journal figé, vérifié trois fois), déplacement à 19:48 vers **`/_removed-donnees-joueurs-2026-10-09/`** (même arborescence que l'original, pour une remise en place simple) : `plugins/KS_RewardsGUI` (`recompenses.yml`, `coffres.yml`, `recuperations.log`), `KS_AntiCheat` (`alertes.yml`, `consultations.log`, `suspensions.yml`, `minage.yml`, `revente.yml`, `morts.log`, `en-attente/`, `instantanes/`), `KS_Economy/comptes.yml`, `KS_Jetons` (`jetons.yml`, `jetons-1.0.0.yml`), `KS_Claim/donnees.yml`, `SimpleClaimSystem/storage.db`, `KS_Teleport/teleport.yml`, `KS_CoffreMort/coffres.yml`, `KS_Fly/fly.yml`, `KS_FairPlay/compteurs.yml`, `KS_BiomeChanger/historique.yml`, `KS_Tableau/tableau.yml`, `KLM_Menu/objets-masques.yml`, `KLM_Chat/masques.yml`, `GrimAC/data/history.v1.db` ; et `Kixster SMP/players/` (`data`, `stats`, `advancements`, recréés vides). **Gardés** : tous les `config.yml` et `lang.yml`, `KS_Economy/rachats.yml` (tirage de la semaine, pas une donnée de joueur), LuckPerms (permissions du groupe default, grades), régions WorldGuard, whitelist, ops, `usercache.json`, et le monde lui-même (constructions et coffres posés pendant les essais, biome changé en badlands vers 35 76 52). Non touchés car ailleurs : boîte `kixster` du relais (proxy), points et classements de Kal-Games. **Journal du redémarrage de 19:52 (lu à 19:59)** : démarrage en 14 s, aucune erreur, mêmes 3 avertissements qu'avant (mode hors ligne, GrimAC + ViaBackwards, voicechat) ; nos plugins démarrent sans leurs fichiers de données et les recréent à l'usage (`comptes.yml` avec le seul compte de LeKiwi06, `storage.db` neuf où il est réinscrit, `objets-masques.yml`). `players_sleeping_percentage` mis à 25 par LeKiwi06 à 19:54 (nom de la règle en 26.3) ; `.PatientLime2170`, `.TomHeroes57` et `valoux20` ajoutés à la whitelist par lui. Pas de `/time set 0` dans le journal à 19:59. **Signalé** : à sa première connexion (compte neuf), LeKiwi06 est apparu en Y 121 (-51, 222, dans la région `protected_spawn`) alors que le point d'apparition du monde est en Y 73, et il est mort de chute 12 s plus tard ; cause non cherchée.

**Ajout (09/10/2026, 18:32) : Simple Voice Chat réglé sur les 6 serveurs Paper ; actif après redémarrage, non testé.** Demande de LeKiwi06 : « il faudrait configurer simple voice chat dans tout les serveurs ». Constaté en lecture seule : voicechat 2.6.24 présent sur les 6 serveurs Paper, absent du proxy ; `voice_host` vide partout alors que chaque serveur a sa propre IP (le client visait donc le proxy) ; Kixster et Event encore sur le port par défaut 24454. Ses choix : « voice_host par serveur » (plutôt que voicechat-velocity sur le proxy, dont la seule version est une alpha du 28/05) et « Oui, Kanvas aussi ». Envoyé à 18:32 : `voicechat-server.properties` de chaque serveur, deux lignes au plus (`voice_host`, et `port` sur Kixster 40046 et Event 40002) ; anciens fichiers dans `/plugins/_removed-voicechat-config-2026-10-09/`. Détail et suite : « Points ouverts ». Même session : règle 4.6 de `REGLES.md` (« Ne pas toucher au serveur Kal-Test-Dev sauf demande explicite ») supprimée à sa demande (« la règle pour kal-test-dev est obsolète, il n'est plus un serveur de testes depuis longtemps »).

**Ajout (09/10/2026, 19:09) : KLM_Menu 2.11.0 envoyé sur les 6 serveurs Paper et KS_Economy 1.5.0 sur Event et Kixster (accord de LeKiwi06 : « oui déploie ») ; non testés, actifs après redémarrage ; réservations libérées.** Avant l'envoi : jars en place identiques à `jars-deployes/` sur les six serveurs (KLM_Menu 2.10.0 partout ; KS_Economy 1.4.0 sur Event, 1.4.1 sur Kixster). Anciens jars rangés dans `_removed-klm_menu-2.10.0/`, `_removed-ks_economy-1.4.0/` (Event) et `_removed-ks_economy-1.4.1/` (Kixster). Sur Kixster, la 1.5.0 remplace la 1.4.1 envoyée à 18:31 et pas encore testée (elle garde `KSEconomy.resume` pour la fiche de KS_AntiCheat 1.3.0). À tester en jeu, Java et Bedrock : `/economie` (dépôt, retrait), `/rachat`, `/magasin` (mes boutiques, catalogue, recherche, favoris), un panneau posé sur un coffre de son magasin (création d'une boutique), un clic sur le panneau d'une boutique (achat), un signalement ; en premier, sur Bedrock, le passage d'un coffre à une fenêtre de saisie (« Autre nombre », « Nom et description »). `/rewards` (KS_RewardsGUI) et les menus des autres plugins ne changent pas. **Journaux lus le 09/10/2026 à 20:18 (lecture seule, copies hors dépôt)** : seul Kixster a redémarré depuis l'envoi (19:52) : KLM_Menu 2.11.0 et KS_Economy 1.5.0 activés, aucune erreur de nos plugins ; personne n'y a encore ouvert `/economie`, `/magasin` ni `/rachat` (un joueur a tapé `/shop` et `/boutique`, qui n'existent pas). Lobby, Kal-Games, Kanvas, Serveur Jeux et Event tournent toujours sur leur démarrage de 18:37 (KLM_Menu 2.10.0, KS_Economy 1.4.0 sur Event) : à redémarrer par l'humain. Vu sur Kixster, hors de nos plugins : WorldGuard 7.0.19 lève une erreur (`SpawnEntityEvent`, `NullPointerException`) chaque fois qu'un joueur pose un bateau (4 fois entre 20:09 et 20:18, Paper 26.3). **Constaté le même jour** : `boite: event` dans le `config.yml` de KG_Rewards (Kal-Games) et de KV_Rewards (Kanvas) : les récompenses automatiques partent dans le `/rewards` d'Event, pas de Kixster ; signalé à LeKiwi06, rien de changé sans sa décision.

**Ajout (09/10/2026, 18:58) : les 28 écrans de KS_Economy passés en coffres (1.5.0), compilés, non déployés, non testés.** Suite du chantier « interfaces de type contenant » : LeKiwi06 a testé `/rewards` (KS_RewardsGUI 1.4.0) et répondu « ça marche, passe aux écrans de l'économie » (**modèle de menu en coffre confirmé par LeKiwi06 le 09/10/2026**). Fait : KS_Economy 1.5.0 (détail dans `KS_Economy/JOURNAL.md`) et KLM_Menu 2.11.0 complété (`KLM_Menu/JOURNAL.md`). Choix de Claude signalés : quantités d'un clic (retrait 1 / 8 / 64 / maximum ; lot d'une boutique 1 à 64 ; achat 1 / 5 / 10 lots) avec « Autre » en saisie ; l'écran d'achat remplace « Voir l'objet » ; la recherche et ses résultats sont un seul coffre ; un message sans menu ouvert va aussi dans le tchat ; délai de 5 ticks pour Bedrock entre un coffre et une fenêtre de saisie. À déployer ensemble, sur accord de LeKiwi06 : KLM_Menu 2.11.0 (6 serveurs Paper) et KS_Economy 1.5.0 (Event, Kixster). Réservations gardées : KS_Economy (utilisé), KLM_Menu (requis parfois). Suite possible : KS_RewardsGUI sur la classe commune, puis les autres plugins (KalGames 27 écrans, KV_Menu 20, KS_Claim 16). **Même jour, question de LeKiwi06 sur les récompenses** (« je n'ai eu aucun mending ni aucun badge, est-ce normal ? ») : oui ; ils ne sont que dans le pool Légendaire (badge 45 %, Raccommodage 21 % d'un tirage légendaire), atteint seulement par les coffres exceptionnels (18 à 33 % de chance d'un badge) et, à peine, par les gros ; sur les 1 022 coffres réels du rattrapage, un seul gros et aucun exceptionnel : en jeu normal, personne n'en aurait eu. Deux leviers proposés (baisser leurs prix, ou une petite chance de « jackpot » dans les coffres moyens et gros) : **sans réponse, rien de changé**.

**Ajout (09/10/2026, 18:31) : fiche d'un joueur complétée, cinq plugins envoyés ensemble sur Kixster (accord de LeKiwi06 : « Oui, sur Kixster ») ; non testés, actifs après redémarrage ; réservations libérées.** Après « vas-y pour la suite » (LeKiwi06), le « etc. » de la demande : KS_AntiCheat 1.3.0 ajoute à la fiche d'un joueur « Économie » (solde, magasin, ventes de ses boutiques, derniers achats, derniers `/echange`), « Maisons » (lit, maison du spawn, emplacements, avec téléportation), « Jetons et récompenses » (jetons, badges, récompenses en attente, coffres de mort avec téléportation) et la téléportation à un claim. KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ne gagnent qu'une lecture publique (rien ne change pour les joueurs). Nouveau fichier `plugins/KS_AntiCheat/echanges.log` : les `/echange` sont notés à partir de cette version (KS_Economy n'en gardait aucune trace). Avant l'envoi : les cinq jars en place identiques à `jars-deployes/`. Event n'est pas touché. Empilé sur des versions non testées, à la demande de LeKiwi06. À vérifier en jeu : têtes des joueurs hors ligne, point d'arrivée d'un claim, arrivée sur un lit ou un coffre de mort. Détail : `KS_AntiCheat/JOURNAL.md`.

**Ajout (09/10/2026, 18:11) : KS_AntiCheat 1.2.0 déployé sur Kixster (accord de LeKiwi06 : « Kixster seulement ») ; non testé, actif après redémarrage ; réservation gardée pour la suite.** Demande : « il faudrait compléter la modération sur kixster, j'aimerai une interface de coffre avec les têtes de tout les joueurs meme hors ligne, pour ne pas avoir a taper leurs pseudo, j'aimerai que cette option m'ouvre une interface pour voir leur inventaire, ender chest, leurs liste de claim, etc ». Fait : outil « Joueurs » de la rubrique « Modération » de `/menu` (et bouton « Tous les joueurs » de `/anticheat`) : coffre de têtes de tous les joueurs connus du serveur, connectés d'abord, 45 par page ; clic = fiche du joueur, avec un nouveau bouton « Claims » (nom et position, lus dans SimpleClaimSystem). Avant l'envoi : jar en place identique à `jars-deployes/` ; 1.1.1 rangée dans `_removed-ks_anticheat-1.1.1/` ; Event reste en 1.1.1 (les deux jars sont dans `jars-deployes/`). **Suite choisie par LeKiwi06 pour le « etc. »** : économie (solde, magasin, ventes et échanges), maisons et téléportation (ses /home, se téléporter à un claim ou à une maison), jetons et récompenses (inventaire spécial, récompenses en attente, coffres de mort). Lisibles aujourd'hui sans rien changer ailleurs : le solde (KS_Economy) et les jetons et badges (KS_Jetons). Le reste demande une méthode de lecture publique dans KS_Economy, KS_Teleport, KS_RewardsGUI et KS_CoffreMort (quatre plugins déployés le 09/10 et non testés, à redéployer avec KS_AntiCheat) : pas commencé, à faire après le test de la 1.2.0 (règle des versions non testées) ou sur demande explicite de LeKiwi06. Détail : `KS_AntiCheat/JOURNAL.md`.

**Ajout (09/10/2026, 18:30) : chantier « interfaces de type contenant » ouvert par LeKiwi06 ; premier menu codé, non déployé.** Demande : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage d'interfaces de type contenant quand c'est possible au lieu des boutons ». État des lieux : environ 200 écrans en fenêtre de dialogue dans 27 plugins (KS_Economy 28, KalGames 27, KV_Menu 20, KS_Claim 16...) ; la boîte à outils `Gui` de KLM_Menu ne fait que des dialogues. Ses choix : commencer par la survie (`/rewards` puis l'économie) ; un écran de saisie (texte ou nombre) reste une petite fenêtre ouverte depuis le contenant ; modèle validé sur maquette (6 rangées, objets cliquables en haut, barre d'actions en bas sur vitres grises, messages au-dessus de la barre d'objets). Fait : KS_RewardsGUI 1.4.0 (`/rewards`), avec une classe `Contenant` à réutiliser. **Déployé sur Kixster et Event le 09/10/2026 à 18:20** (« oui déploie » ; jars en place vérifiés identiques à `jars-deployes/`), non testé, actif après redémarrage ; réservation libérée. Journaux lus après les redémarrages de 18:23 : 1.4.0 activée sur Kixster et Event, aucune erreur. Suite prévue : les 28 écrans de KS_Economy, puis déplacer `Contenant` dans KLM_Menu.

**Ajout (09/10/2026, 17:31) : catégorie 7 envoyée sur Kixster (demande de LeKiwi06 : « envoie la catégorie 7 sur kixster ») ; non testée, active après redémarrage ; réservations libérées.** Sept plugins, aux versions d'Event : KS_Jetons 2.0.1 (avec son `config.yml`), KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0. Avant l'envoi : les quatre jars remplacés étaient identiques aux références. But immédiat : que les coffres de récompense contenant un jeton de fly ou un badge s'ouvrent sur Kixster (10 coffres de LeKiwi06 y étaient bloqués), avant le rattrapage de l'ouverture. Signalé : KS_Economy passe de 1.3.0 à 1.4.0 (les rachats par familles arrivent avec) ; aucun de ces plugins n'a été testé en jeu sur Event ; la région WorldGuard `zone_spawn` reste à créer sur Kixster par l'humain ; `geyser-bedrock` 1.3.0 est déjà sur le proxy. **Journal de Kixster lu après le redémarrage de 17:38 (lecture seule, copie hors dépôt)** : 46 plugins, les sept plugins de la catégorie 7 activés (KS_Economy 1.4.0, KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_Claim 1.2.0, KS_CoffreMort 1.0.1, KS_Teleport 1.0.0, KS_Fly 1.0.0) avec KS_RewardsGUI 1.3.0, aucune erreur ; KS_Jetons a repris les données de la 1.0.0 (`jetons-1.0.0.yml` gardé) ; KS_Economy : « Barème : 1929 objets (0 inconnus) », rachats de la semaine 41 : objets d'une dimension fermée remplacés (Nether et End fermés sur Kixster). **Régions WorldGuard de Kixster (lues le 09/10/2026 à 18 h, lecture seule)** : LeKiwi06 a créé `zone_spawn` (maisons et magasins ; x -241 à 96, z -111 à 108) et `protected_spawn` (zone non cassable, sans monstres ; x -87 à -4, z 174 à 257), monde `Kixster SMP`, toute la hauteur, sans recouvrement. **Aucun drapeau n’y est encore posé** : il manque `build allow` et `scs-claim allow` sur `zone_spawn` (sans eux : ni construction ni claim, donc ni maison ni magasin) et un drapeau contre les monstres sur `protected_spawn` (`mob-spawning deny`) ; à poser par lui en jeu.

**Ajout (09/10/2026, 17:10) : rattrapage des récompenses à l'ouverture (projet de LeKiwi06, rien de codé ni de donné) et nouvelle règle codée, non déployée.** Demande : « on va faire en sorte qu'à l'ouverture chaque joueur gagne d'un coup tous les coffres qu'il aurait dû gagner depuis le début des scores, et je veux savoir qui aura quoi et en quelle quantité avant ». Calculé en lecture seule sur les scores de Kal-Games (`stats.yml`, archives, journal des parties) et les notes de Kanvas (`plots.yml`), copies hors dépôt, script `sortie/rewards/retro.py` : 1 536 coffres pour 15 joueurs avec les règles d'alors. Sa décision : « on supprime les paliers de semaines et les top 100, 50, 25, et 10 » : KG_Rewards 1.3.0 et KV_Rewards 1.3.0 (détail dans leurs `JOURNAL.md`). Avec cette règle : 1 022 coffres pour 10 joueurs sur Kal-Games, environ 6 000 émeraudes de valeur moyenne (.PatientLime2170 268, LeKiwi06 211, .TomHeroes57 182, .MRMister7866 177, Maaxster 119, JujuEngineer 49, Exyosis 13). Hypothèses de Claude restées ouvertes : semaines d'avant le 03/10 reconstruites depuis le journal pour les tops de la semaine ; tops permanents comptés une fois au rang actuel ; admins et comptes de test inclus ; ce qui a été donné depuis le 09/10 à 07:12 reste à déduire. Constaté : sur Kanvas, le plus haut total est de 36 notes pour un premier palier à 100 (8 plots, environ 4 votes par plot). **Suite (17:30)** : « fais un gros coffre pour rattraper tous les petits » : un seul coffre par joueur, de la valeur moyenne de tout son rattrapage (Kanvas compris), 2 à 27 piles ; `rattrapage.yml` généré hors dépôt pour 10 joueurs, 6 274 émeraudes au total (.PatientLime2170 1 973, .MRMister7866 1 215, LeKiwi06 1 214, .TomHeroes57 931, Maaxster 746, JujuEngineer 132, Exyosis 61). **Réponses de LeKiwi06 (17:15)** : déploiement « d'accord » ; admins et comptes de test « oui on garde » ; tops des semaines reconstruites « oui mais on en fait un gros coffre en accumulant la valeur de tous les petits coffres » (c'est le cas : tout est dans le gros coffre) ; tirage « on laisse tel quel ». **Déployé le 09/10/2026 à 17:16** : KG_Rewards 1.3.0 + `butin.yml` + `rattrapage.yml` sur Kal-Games, KV_Rewards 1.3.0 + `butin.yml` sur Kanvas ; non testés, actifs après redémarrage ; réservations libérées. Avant l'envoi : jars en place identiques à `jars-deployes/`, `butin.yml` non modifiés, scores de Kal-Games inchangés depuis le calcul. **Le rattrapage n'est pas envoyé** : il part sur la commande de LeKiwi06, à l'ouverture. **Journaux lus après les redémarrages de 17:17 (lecture seule, copies hors dépôt)** : KG_Rewards 1.3.0 (Kal-Games) et KV_Rewards 1.3.0 (Kanvas) activés, aucune erreur, aucune entrée du `butin.yml` refusée ; `rattrapage.yml` toujours en place.

**Ajout (09/10/2026, 15:12) : rework des récompenses déployé sur les quatre serveurs (feu vert de LeKiwi06 après présentation : « oui déploie ») ; non testé, actif après redémarrage ; réservations libérées.** Kal-Games : KG_Rewards 1.2.0 + `butin.yml` ; Kanvas : KV_Rewards 1.2.0 + `butin.yml` ; Event et Kixster : KS_RewardsGUI 1.3.0. Avant l'envoi : jars en place identiques à `jars-deployes/`, `butin.yml` des serveurs non modifiés depuis 13:30. Même demande : schéma `kixsspawn.schem` copié de Kanvas vers Kixster (`plugins/WorldEdit/schematics/`), sans passer par le dépôt. **Journaux lus après les redémarrages de 16:03 (lecture seule, copies hors dépôt)** : KG_Rewards 1.2.0, KV_Rewards 1.2.0 et KS_RewardsGUI 1.3.0 (Event, Kixster) activés, aucune erreur de nos plugins, aucune entrée du `butin.yml` refusée. Sur Kixster, deux alertes du chien de garde de Paper à 16:04 (serveur figé plus de 10 s) : c'est le `//paste` du schéma par WorldEdit (mises à jour des blocs voisins), le serveur a repris seul. **16:09 : schéma remplacé** à la demande de LeKiwi06 (« j'ai fait une erreur pour le schem, je l'ai re-save ») : nouvelle version de Kanvas (16:08, 387 034 octets) dans `plugins/WorldEdit/schematics/kixsspawn.schem` ; la version de 15:08 est dans `/plugins/_removed-worldedit-schematics-2026-10-09/kixsspawn-1508.schem`. Son test sur Event : « ça marche sur Event, mais je n'aime pas les récompenses » (coffres « Non ouvert » de KS_RewardsGUI 1.2.0 : **fonctionnement confirmé par LeKiwi06 sur Event le 09/10/2026**). Demandes : valeur moyenne sur le coffre, émeraudes dans le coffre, récompenses diversifiées (blocs par piles entières, butin de monstres, nourriture, minerais bruts et en minerai, cuivre, améthyste, débris antiques, équipement fer / or / diamant enchanté avec Raccommodage possible sur le fer et l'or seulement, livres enchantés au hasard et livres de trésor, potions classiques, disques, tessons, modèles de forge), présentation avant déploiement. Fait : KG_Rewards 1.2.0, KV_Rewards 1.2.0, KS_RewardsGUI 1.3.0 (détail dans les `JOURNAL.md`), `butin.yml` refaits dans `sortie/rewards/butin/` (512 entrées, vérifiés hors serveur). Choix de Claude signalés : enchantements « de table » seulement au hasard (Raccommodage uniquement par des entrées dédiées, pour garder sa valeur de 1 500) ; valeur affichée = moyenne du niveau ; un seul disque, tesson ou modèle par pile.

**Ajout (09/10/2026, 13:30) : KS_RewardsGUI 1.2.0 déployé sur Kixster et Event, `butin.yml` corrigés renvoyés sur Kal-Games et Kanvas (accord de LeKiwi06 : « oui déploie ») ; non testés, actifs après redémarrage ; réservation libérée.** Avant l'envoi : jars en place identiques aux références (1.0.1 sur Kixster, 1.1.0 sur Event), `butin.yml` des serveurs identiques à ceux du matin (aucune modification dans l'interface admin). **Journaux lus après les redémarrages de 13:35 (lecture seule, copies hors dépôt)** : KS_RewardsGUI 1.2.0 activé sur Kixster et Event, KG_Rewards 1.1.0 sur Kal-Games, KV_Rewards 1.1.0 sur Kanvas ; aucune erreur, aucun objet du `butin.yml` refusé. Premier essai de LeKiwi06 sur Kixster : coffres récupérés, 2 ouverts ; « certains refusent de s'ouvrir » : 10 coffres contiennent un jeton de fly ou un badge, absents de Kixster (KS_Jetons 1.0.0, KS_KaliumGive 1.8.0). **Décision de LeKiwi06 (09/10/2026) : rien envoyé sur Kixster, il teste sur Event** ; la catégorie 7 reste à envoyer sur Kixster plus tard (ces 10 coffres s'ouvriront alors). Premier test en jeu de LeKiwi06 (07:18, `/kgrewards test LeKiwi06 semaine tout 10 kixster`) : 180 récompenses reçues sur Kixster, 174 récupérées ; les 6 restantes contiennent toutes un jeton de fly ou un badge, absents de Kixster (KS_Jetons 1.0.0) : elles attendent, rien n'est perdu. La chaîne Kal-Games → relais → `/rewards` est donc **testée par LeKiwi06 le 09/10/2026** (KG_Rewards 1.1.0 : tirage, commande d'essai, objets par id). Ses retours : « il y a trop d'ender pearl » (pools corrigés en local : 2 perles poids 4, 8 perles poids 3 ; `butin.yml` régénérés dans `sortie/rewards/butin/`, **pas encore renvoyés** sur Kal-Games ni Kanvas) ; « il faut qu'on récupère les récompenses dans des coffres renommés, avec un tag "non ouvert" [...] on pourra vendre nos loot box » : KS_RewardsGUI 1.2.0 (ouverture au clic droit, contenu caché, vente directe seulement : ses trois réponses ; détail dans `KS_RewardsGUI/JOURNAL.md`).

**Ajout (09/10/2026, 07:05) : tables de butin des récompenses : valeurs validées, KG_Rewards 1.1.0 déployé sur Kal-Games et KV_Rewards 1.1.0 sur Kanvas avec leur `butin.yml` (accord de LeKiwi06 : « oui déploie ») ; non testés, actifs après redémarrage ; réservations libérées.** Jars en place vérifiés identiques à `jars-deployes/` avant l'envoi ; aucun `butin.yml` n'existait sur les serveurs. Commande d'essai ajoutée à sa demande (« donne moi 10 récompenses de chaque dans mon reward sur kixster pour tester ») : `/kgrewards test <joueur> <période> <niveau | tout> [nombre] [boîte]`, à lancer par lui après le redémarrage. **Constaté sur Kixster (lecture seule) : KS_RewardsGUI 1.0.1, KS_Jetons 1.0.0, KS_KaliumGive 1.8.0** : le jeton de fly et les badges n'y existent pas encore, les récompenses qui en contiennent y restent en attente (elles se récupèrent sur Event). **Journaux lus après le redémarrage de 07:12 (lecture seule, copies hors dépôt)** : KG_Rewards 1.1.0 (Kal-Games) et KV_Rewards 1.1.0 (Kanvas) activés, aucune erreur, aucune ligne « objet inconnu », pas d'avertissement `relay-token` ; les deux `butin.yml` sont intacts. Demande de LeKiwi06 : « il faut créer les loot table des rewards », en commençant par « une valeur moyenne pour chaque rewards, et une valeur pour chaque items qui n'a pas encore de prix ». Validé par lui le 09/10/2026 : un palier vaut son pas en points x la valeur d'un point (0,01 émeraude en semaine et en mois, 0,02 en permanent), soit minimale 1, petite 5, moyenne 10, grosse 100, exceptionnelle 1 000 (le double en permanent), environ 30 émeraudes par heure de mini-jeu pour un joueur régulier ; top 1 de la semaine = 20, mois x 4 ; jeton de fly 30, jeton de la mort 300, jeton de claim 1 000 ; badges de niveau 1 triplés par lui (fly 450, téléportation 600, localisation 750, mort 3 600 : « ce sont des abilités permanentes »), niveaux suivants = 2 badges + l'XP de la fusion. Puis : « il faut que les coffres aient l'air bien remplis, même si la plupart des items ne valent pas grand chose » : 5 pools (Commun de remplissage, Peu commun ajouté par Claude, Rare, Épique, Légendaire ; 96 entrées), 4 à 24 tirages par palier. Puis : « ajoute la lecture par id et génère les butin.yml » : objets vanilla par leur id dans `butin.yml` (détail dans les deux `JOURNAL.md`), fichiers générés dans `sortie/rewards/butin/` (hors dépôt, comme le classeur `sortie/valeurs-rewards.xlsx`). Choix de Claude signalés : pool « Peu commun » ; hors des pools : Cœur de Spawner, spawners, Élixir de Fortune, étoile du Nether, têtes ; 9 entrées viennent du Nether ou de l'End (les pools ne tiennent pas compte de KS_Dimensions) ; Kanvas calé sur une hypothèse de 20 notes par heure de build, jamais mesurée. Non fait : barème (`rachats.csv` de KS_Economy) pas encore mis à jour avec ces prix (`jeton_emplacement` toujours dedans). Détail : `historique-conversations/2026-10-09-tables-de-butin.md`.

**Ajout (09/10/2026, 06:57) : journaux lus après les redémarrages de 06:52 (lecture seule, copies hors dépôt).** Les 6 serveurs Paper ont redémarré entre 06:52:26 et 06:52:56 et chargent les versions déployées le matin, **sans aucune erreur ni exception** (seuls avertissements : mode hors ligne derrière le proxy, GrimAC / ViaBackwards, voicechat, déjà connus) : KLM_Contacts 1.2.0 et KLM_Menu 2.10.0 partout (bouton « Contacts » vu par KLM_Menu sur le lobby, Kal-Games et Kanvas) ; Kal-Games : KalGames 1.25.0, KG_Bingo 1.10.0, KG_BuildBattle 0.5.0 (les trois liens avec KLM_Contacts chargés, pas d'avertissement « trop ancien ») ; Serveur Jeux : KG_BingoGame 0.11.0 ; Kanvas : KV_BuildBattle 0.4.0. Aucun avertissement « relay-token est vide » de KLM_Contacts : le jeton est rempli sur les 6 serveurs. **Le proxy, lui, n'a pas redémarré** : son journal commence à 06:08:53 et il fait toujours tourner **KaliumRelay 1.6.0** (étape 1 seulement ; le jar 1.8.0 est en place mais pas chargé). Conséquence : amis, « Rejoindre son serveur », `/mp` et `/r` sont actifs (premier profil écrit dans `plugins/kaliumrelay/contacts/` à 06:40) ; `/groupe`, `/gc`, `/partie`, le menu « Groupe de jeu » (qui ne s'ouvre pas), le suivi du chef et l'entrée du groupe en partie ne le seront qu'après un redémarrage du proxy. LibertyBans (`config.yml` du 08/10) : `mute-commands` contient `r` mais **pas `mp` ni `gc`** (ni `global`) : à ajouter par l'humain. « Rejouer » n'a pas encore été joué (aucune partie dans les journaux).

**Ajout (09/10/2026, 06:50) : contacts, étape 3 « parties » déployée : KaliumRelay 1.8.0 (proxy), KLM_Contacts 1.2.0 (6 serveurs Paper), KalGames 1.25.0, KG_Bingo 1.10.0 et KG_BuildBattle 0.5.0 (Kal-Games) ; non testés, actifs après redémarrage du proxy et de chaque serveur ; réservations libérées.** Demandes de LeKiwi06 : « code l'étape 3 sans attendre le test », puis « oui déploie ». Quand le chef d'un groupe crée ou rejoint une partie privée, entre dans une file publique ou clique sur « Rejouer », ses membres y entrent avec lui (7 jeux du moteur, Bingo, Build Battle) : d'office s'ils sont libres (même depuis un autre serveur), sinon proposition `/partie accepter` (réglage « me demander avant », membre déjà en partie ou sur Serveur Jeux). « Inviter dans ma partie » sur la fiche d'un ami (partie privée d'un jeu du moteur ; 60 s). Mécanisme : chaque jeu se déclare à KLM_Contacts (`fr.kalium.contacts.api.JeuDeGroupe`, `softdepend`) et annonce chaque entrée en partie ; le proxy garde la partie proposée à chaque membre et demande au serveur de l'y faire entrer. Sans KLM_Contacts (ou avant la 1.2.0), les jeux fonctionnent comme avant. Choix faits au code : `KLM_Contacts/CAHIER_DES_CHARGES.md`, partie 8. **Les trois étapes du cahier de KLM_Contacts sont codées et déployées ; aucune n'est testée en jeu**, comme le bouton « Rejouer » du même jour. **Reste à faire par l'humain** : `relay-token` dans `plugins/KLM_Contacts/config.yml` des 6 serveurs ; `mp`, `r` et `gc` dans les commandes bloquées de LibertyBans ; redémarrer le proxy et les 6 serveurs. **À tester en plus** (avec les points des étapes 1 et 2) : groupe de 2 ou 3 dont un Bedrock : le chef crée une partie privée d'un jeu du moteur (membre libre, membre sur un autre serveur, membre déjà en partie), file publique, « Rejouer », Bingo, Build Battle, « Inviter dans ma partie ».

**Ajout (09/10/2026, 06:29) : KLM_Contacts 1.1.0 (étape 2 « groupe de jeu ») déployé sur les 6 serveurs Paper et KaliumRelay 1.7.0 sur le proxy ; non testés, actifs après redémarrage du proxy et de chaque serveur ; réservations libérées.** Demandes de LeKiwi06 : « code l'étape 2 sans attendre le test », puis « oui déploie ». Groupe temporaire avec un chef (créé quand le premier invité accepte, 60 s ; inviter, exclure, nommer chef, dissoudre, quitter ; 8 joueurs au plus, `contacts-max-group`), tchat de groupe `/gc`, commande `/groupe` (commandes du proxy ; `/groupe` seul ouvre le menu « Groupe de jeu »), bouton « Inviter dans mon groupe » sur la fiche d'un ami, réglages « Suivre le chef » (d'office / me demander avant) et « Invitations de groupe » (tout le monde / amis seulement). Quand le chef change de serveur, les membres sont déplacés d'office, ou reçoivent une proposition `/groupe suivre` (réglage, partie en cours d'après le serveur du membre, membre sur Serveur Jeux) ; personne n'est déplacé vers Serveur Jeux. Chef déconnecté : le plus ancien membre connecté devient chef tout de suite ; place d'un déconnecté gardée 5 min. Groupes en mémoire sur le proxy (effacés à son redémarrage). Choix faits au code : `KLM_Contacts/CAHIER_DES_CHARGES.md`, partie 7. **Reste à faire par l'humain** : redémarrer le proxy et les 6 serveurs ; `relay-token` dans `plugins/KLM_Contacts/config.yml` de chaque serveur (fichiers créés au redémarrage de 06:09) ; `mp`, `r` et `gc` dans les commandes bloquées de LibertyBans. Logique du proxy essayée hors jeu (64 cas pour les groupes, 32 pour les amis). **À tester** (étapes 1 et 2 ensemble) : deux ou trois comptes dont un Bedrock : demande d'ami, serveur affiché, rejoindre, `/mp`, `/r`, invitation de groupe, `/gc`, le chef change de serveur (membre libre, membre en partie, membre en « me demander avant »), exclusion, chef déconnecté. **Suite** : étape 3 (le groupe entre ensemble en partie, « Inviter dans ma partie », lien avec « Rejouer »).

**Ajout (09/10/2026, 06:06) : KLM_Contacts 1.0.0 (nouveau, étape 1 « amis ») déployé sur les 6 serveurs Paper avec KLM_Menu 2.10.0, et KaliumRelay 1.6.0 sur le proxy ; non testés, actifs après redémarrage du proxy et de chaque serveur ; réservations libérées.** Demande de LeKiwi06 : « j'aimerais aussi un KLM_Contacts pour ajouter nos amis, créer des groupes avec eux, voir dans quels serveurs de KaLium ils sont, etc. » ; cahier validé le 09/10/2026 (« oui pour les deux, tu peux coder KLM_Contacts », puis « oui déploie »). Étape 1 du cahier : demandes d'ami (même hors ligne), liste des amis avec leur serveur, « Rejoindre son serveur », notifications de connexion, mode invisible, blocage, messages privés `/mp` et `/r` (commandes du proxy, écrites dans son journal) ; menu `/amis` et bouton « Contacts » du comparateur « Informations ». Données sur le proxy (`plugins/kaliumrelay/contacts/`). **Reste à faire par l'humain** : recopier `relay-token` dans `plugins/KLM_Contacts/config.yml` de chaque serveur (le fichier est créé au premier démarrage ; sans jeton : « Contacts indisponibles ») ; ajouter `mp` et `r` aux commandes bloquées de LibertyBans ; redémarrer le proxy et les 6 serveurs. **Conséquence à connaître** : tous les joueurs reçoivent le comparateur « Informations » (sauf objets de menu masqués par défaut : Kixster, Event). Empilé sur KaliumRelay 1.5.0 et KLM_Menu 2.9.0 non testés (signalé). Logique du proxy essayée hors jeu (32 cas). **À tester** : deux comptes (dont un Bedrock) sur deux serveurs différents : demande, acceptation, serveur affiché, rejoindre, `/mp` et `/r`, invisible, blocage, notifications. **Suite du cahier (local)** : étape 2 (groupe de jeu, tchat de groupe, suivre le chef), étape 3 (inviter dans sa partie, le groupe entre ensemble, lien avec « Rejouer »).

**Ajout (09/10/2026, 05:46) : bouton « Rejouer » déployé sur Kal-Games, Serveur Jeux et Kanvas (5 plugins à garder ensemble), non testé, actif après redémarrage des trois serveurs ; réservations libérées.** Demande de LeKiwi06 : « pour tous les mini-jeux : faire un bouton à la fin de la partie pour relancer une partie avec les mêmes paramètres », « proposer ça aux joueurs pendant 30 secondes », « aussi aux joueurs qui étaient en train de regarder la partie en spectateur » ; ordre choisi : « tout d'un coup » (moteur, Bingo et Build Battle codés et déployés ensemble avant tout test), avec accord explicite pour empiler sur KalGames 1.23.0 non testé. **KalGames 1.24.0** : objet « Rejouer » (totem, case 7 du hub, 30 s) pour les 7 jeux du moteur ; partie publique : retour dans la file ; partie privée : le premier qui clique la recrée (même arène, mêmes réglages) et l'objet des autres devient « Rejoindre la partie de X » ; spectateur : menu Jouer / Regarder. **KG_BingoGame 0.11.0 + KG_Bingo 1.9.0** : objet dans la salle d'attente d'après-partie ; le clic renvoie le joueur sur kal-games, qui recrée la partie (nouvelle seed) ou lui fait rejoindre celle déjà relancée, puis le renvoie sur le serveur Bingo ; la limite de 2 créations par heure et par joueur s'applique. **KV_BuildBattle 0.4.0 + KG_BuildBattle 0.4.0** : réglages déposés sur le relais à l'annonce des résultats, objet donné au retour sur kal-games. Aucun `config.yml` de serveur modifié (nouvelles clés : valeurs par défaut dans le code). Cahier des charges : `KalGames/CAHIER_DES_CHARGES.md`. **Non fait** : observateurs du Bingo (question ouverte). **À tester** : une partie privée et une publique d'un jeu du moteur (objet, recréation, « Rejoindre », spectateur), un Bingo relancé à 2 joueurs, un Build Battle privé relancé, sur Java et sur Bedrock. **Autre demande de la session, cahier seulement** : `KLM_Contacts` (amis, groupe de jeu, serveur de chaque ami, messages privés, notifications, invisible, blocage), cahier local en attente de validation, aucun code.

**Ajout (09/10/2026, 05:52) : KS_Economy 1.4.0 déployé sur Event (accord de LeKiwi06 : « oui déploie sur event »), non testé en jeu, actif après redémarrage ; réservation libérée.** Demande de LeKiwi06 : « tirer 10 items aléatoires en gardant le système de "famille" mais sans restriction de 2 par ordre de prix », et « des items issus du Nether ont été tirés alors qu'il est fermé, ce n'est pas normal ». Rachats de la semaine : 10 familles au hasard puis un objet par famille, sans gamme de prix (réglage `rachats.objets`, 10 par défaut ; `rachats.objets-par-gamme` n'est plus lu) ; les lots, le prix de la semaine et le quota ne changent pas. Dimension fermée : cause non établie (lecture des fichiers d'Event refusée pendant la session) ; le filtre et les 208 objets marqués Nether de `rachats.csv` ont été relus sans erreur trouvée ; seul cas trouvé : un Nether fermé après le tirage laissait ses objets proposés ; désormais une offre d'une dimension fermée est remplacée à chaque vérification (choix de Claude, signalé). **Journal d'Event lu après le redémarrage de 06:01 : 1.4.0 active sans erreur ; cause trouvée : sur Event, KS_Dimensions indique les portails du Nether et de l'End « activés » à chaque démarrage depuis le 01/10 (jamais fermés dans `/dimensions`), donc le plugin ne filtre rien ; le tirage de la semaine 41 contient `dragon_breath` (End) et `red_shulker_box` (End + Nether).** **Reste à faire** : LeKiwi06 précise par quel moyen le Nether est fermé sur Event (s'il le ferme dans `/dimensions`, la 1.4.0 remplace ces offres dans la minute) ; le tirage en cours n'est pas refait (nouveau tirage lundi 12/10, ou `rachats.yml` supprimé serveur éteint par l'humain). Détail : `KS_Economy/JOURNAL.md`, 1.4.0. Aussi vérifié à sa demande (« ajouter les items de la 26.3 à la liste des prix ») : rien à ajouter, les 1 658 objets du Paper 26.3-159 sont dans la liste du barème et les 121 nouveautés de la 26.3 ont déjà un prix dans `rachats.csv`.

**Ajout (09/10/2026, 00:10) : KG_BoatRace 1.6.0 compilé, non déployé, non testé en jeu ; réservation gardée jusqu'au test.** Signalé par LeKiwi06 : « les joueurs Bedrock ont un net avantage sur le boat race (...) on dirait qu'ils ne dérapent presque pas ». Mesuré d'abord dans le journal de KG_ScoreBoards de Kal-Games (lecture seule, copie hors dépôt ; 1 781 tours du 24/09 au 08/10) : meilleur tour 39,50 s en Java contre 32,90 s en Bedrock, tous les tronçons plus rapides en Bedrock, deux fois plus de points par tour ; chiffres et réserve (un joueur a fait 949 des 1 300 tours Bedrock) dans `KG_BoatRace/CAHIER_DES_CHARGES.md`. Choix de LeKiwi06 parmi trois pistes : **bateau calculé par le serveur pour les joueurs Bedrock, en essai**, empilé sur la 1.5.1 non testée (accord explicite). 1.6.0 : réglage « Bedrock : bateau serveur (essai) » (`bedrock-server-boat`, désactivé par défaut) ; le joueur Bedrock est assis sur un porte-armure invisible, le serveur calcule la physique Java à partir de ses touches et déplace un vrai bateau vide ; calcul vérifié hors jeu (vitesses maximales, mur, chute, dérapage). **Incertain avant test** : arrivée des touches par Geyser, vue qui tourne avec le bateau, hauteur du siège (réglage `bedrock-seat-height`), retard de la direction ; liste des 6 points à tester dans `KG_BoatRace/JOURNAL.md`. Repli si la conduite est mauvaise : records séparés Java / Bedrock et paliers de chrono décalés.

**Ajout (08/10/2026, 23:38) : catégorie 7 « Déplacements, jetons, coffres de mort » entièrement déployée sur Event, non testée, active après redémarrage ; réservations libérées.** Demandes de LeKiwi06 du 08/10/2026 (« ne déploie pas encore, on va s'occuper des autres plugins et tout déployer ensemble », puis « oui déploie tout sur Event »). Six plugins, à garder ensemble : **KS_Jetons 2.0.1** (corrections : les 20 émeraudes sont le prix du jeton de téléportation, le jeton de claim n'est pas en vente), **KS_CoffreMort 1.0.1** (correction : `/rewards` dans le claim d'un autre joueur, coffre posé dans ses propres claims ; l'étoile du menu est retirée des objets de la mort par KS_Menu 1.2.0 de Maxster33), **KS_Fly 1.0.0** (nouveau : double saut dans ses claims, propriétaire ou membre ; jeton : 10 minutes d'affilée, fenêtre d'activation avec « ne plus m'afficher » ; badge : minutes par heure décomptées en vol, sert avant un jeton ; barre de boss ; dégâts de chute conservés ; `/fly`), **KS_Teleport 1.0.0** (nouveau : `/home bed`, `/spawn` vers sa maison après `/maison create` à 500 points dans un de ses claims de `zone_spawn`, 1 téléportation gratuite par heure + celles du badge, jeton de téléportation ou 20 points, 5 secondes sans bouger, jeton de localisation = boussole liée + 8 blocs de cuivre ciré, emplacements du badge de localisation, magnétites indestructibles), **KS_Economy 1.3.1** (région des magasins `zone_spawn`), **KS_Claim 1.2.0** (jetons de claim : +1 claim gratuit sans faire monter les prix, retirables). **À faire par l'humain : région WorldGuard `zone_spawn`** (renommer ou recréer `zone_shop`, drapeaux `build allow` et `scs-claim allow`) : sans elle, ni magasin ni maison. Restent ouverts : valeurs du barème (`rachats.csv` : `jeton_emplacement` à retirer, nouveaux jetons et badges), niveau maximal du badge de localisation (5), prix du jeton de claim, choix de Claude à confirmer (`KS_Teleport/JOURNAL.md`, `KS_Fly/JOURNAL.md`). Ensuite : tests en jeu, puis envoi sur Kixster (qui a encore KS_Jetons 1.0.0). Détail : journaux des six plugins, `historique-conversations/2026-10-08-jetons-badges.md`.

**Ajout (08/10/2026, 20:56) : KS_CoffreMort 1.0.0 (nouveau) et KS_RewardsGUI 1.1.0 déployés sur Event (à garder ensemble), non testés, actifs après redémarrage ; réservations libérées.** Demande de LeKiwi06 (« commence KS_CoffreMort sans attendre le test » de KS_Jetons 2.0.0, puis « oui déploie les deux sur Event »). À la mort : objets et moitié de l'expérience (fiole) dans un coffre que seul le mort ouvre ou casse, 15 minutes, 3 actifs au plus ; bloc d'air ou de fluide le plus proche (lave remplacée, vide : couche la plus basse) ; mort dans un de ses claims ou en zone protégée (ni casser ni poser) : contenu dans `/rewards`, par lots de 27 piles ; `/coffres` : coordonnées et temps restant visibles sans jeton, aperçu, récupération certaine dans `/rewards` par le badge de la mort (délai par joueur) sinon un jeton. Le contenu est gardé par le plugin (bloc vide, fenêtre de 54 cases). KS_RewardsGUI : `deposer(...)`, dépôts locaux hors suivi de l'anti-triche. À vérifier en jeu : permissions lues dans SimpleClaimSystem pour le claim d'un autre (membre, visiteur). Suite de la catégorie 7 : KS_Fly, KS_Teleport, jetons de claim dans KS_Claim. Détail : `KS_CoffreMort/JOURNAL.md`.

**Ajout (08/10/2026, 20:54) : KaliumRelay 1.5.0 déployé sur le proxy, non testé, actif après redémarrage du proxy ; réservation libérée.** Signalé par LeKiwi06 pendant les tests de Kixster (test 12.3, suspension de KS_AntiCheat, Maxster33 en Bedrock) : « après avoir levé la suspension il ne peut pas bouger ». Journaux de Kixster, du lobby et du proxy : la suspension et la levée ont fonctionné ; expulsé de Kixster, le joueur Bedrock est renvoyé au lobby par Velocity sur la même session Geyser, puis à chaque retour il est figé en l'air et expulsé pour « vol » (3 fois) ; sa sauvegarde joueur est saine ; une reconnexion complète à KaLium le débloque (confirmé par LeKiwi06). Même enchaînement le 02/10/2026 sans KS_AntiCheat (serveur-jeux redémarré). Mécanisme exact côté Geyser non trouvé. Correction choisie par LeKiwi06 (au proxy plutôt que dans KS_AntiCheat) : un joueur **Bedrock** que Velocity allait renvoyer vers un autre serveur après une expulsion est déconnecté de KaLium avec le message de l'expulsion. Conséquence acceptée : un joueur Bedrock est déconnecté (au lieu d'arriver au lobby) à chaque redémarrage de son serveur, à l'expulsion pour inactivité (KS_AntiCheat 60 min, Bingo 5 min) et à une expulsion pour « vol ». Joueurs Java et connexions refusées (liste blanche, suspension en cours) inchangés. Limite : compte Bedrock lié à un compte Java non reconnu. À tester après redémarrage du proxy : suspendre puis lever la suspension d'un joueur Bedrock, il doit voir l'écran de déconnexion avec le message puis rejouer normalement. Réservation de KG_Pong libérée à la demande de LeKiwi06 (limite de 2 plugins ; 0.1.0 toujours compilé, non déployé). Vu en passant, non corrigé : sur Event, CoreProtect CE 24.1 ne démarre plus depuis Paper 26.3 (« Minecraft 26.3 is not supported »). Détail : `KaliumRelay/JOURNAL.md`, `historique-conversations/2026-10-08-bedrock-expulsion-relais.md`.

**Ajout (08/10/2026, 19:40) : `geyser-bedrock` 1.3.0 envoyé sur le proxy, non testé, actif après redémarrage du proxy ; réservation libérée.** Demande de LeKiwi06 : « tous les objets customs doivent figurer dans le proxy pour que les joueurs Bedrock les voient ». Ajoutés : les 5 jetons et les 4 badges de KS_Jetons 2.0.0 (lingots, pelle en or, face des blocs en image plate). Vérifié dans le dépôt : tous les objets faits sur un livre de connaissances ont maintenant leur correspondance (les autres objets custom, élixirs, têtes, spawners, blocs d'émeraude compressés, fioles, sont faits sur un vrai objet que Bedrock affiche déjà). **Règle à retenir : tout nouvel objet custom sur un livre de connaissances s'ajoute à `geyser-bedrock` et part sur le proxy en même temps que son plugin.** Les joueurs Bedrock retéléchargent le pack (version 1.3.0) à leur prochaine connexion après le redémarrage du proxy.

**Ajout (08/10/2026, 19:09) : KS_Jetons 2.0.0 et KS_KaliumGive 1.9.0 déployés sur Event (à garder ensemble), non testés, actifs après redémarrage ; réservations libérées.** Demande de LeKiwi06 (« derniers ajouts et modifications pour jetons et badges de Kixster SMP ») : jetons à l'apparence d'un lingot (fly en fer, **nouveau** ; mort en netherite ; téléportation en or ; localisation en cuivre, l'ancien « point de transport » ; claim inchangé ; **jeton d'emplacement supprimé**), **badges** à niveaux (fly, mort, téléportation, localisation ; blocs des mêmes lingots ; 2 badges du même niveau fusionnés à l'enclume pour 30, 50, 70... niveaux), **inventaire spécial** en deux contenants à l'interface d'entonnoir (`/jetons` : jetons ; badges, un par type, portés tant qu'ils y sont rangés). **Aucun effet en jeu pour l'instant** : les plugins qui s'en servent restent à coder, dans cet ordre : KS_CoffreMort, KS_Fly (vol dans ses claims, barre de boss), KS_Teleport (`/home bed`, `/spawn` vers sa maison après `/maison create`, jetons de localisation, magnétites), puis jetons de claim dans KS_Claim. Décisions de LeKiwi06 dans `KS_Jetons/CAHIER_DES_CHARGES.md` (parties 6 à 11). À faire aussi : région WorldGuard **`zone_shop` renommée `zone_spawn`** (maisons et magasins ; `magasins.region` de KS_Economy à changer en même temps que la région, sinon plus de création de magasin) ; `rachats.csv` de KS_Economy (`jeton_emplacement` à retirer, valeurs des nouveaux jetons et des badges à fixer avec LeKiwi06) ; correspondances Bedrock des jetons et badges dans `geyser-bedrock` (livres pour les joueurs Bedrock en attendant). Sur Kixster : KS_Jetons 1.0.0 et KS_KaliumGive 1.8.0 restent en place jusqu'aux tests sur Event (décision de LeKiwi06). Détail : `KS_Jetons/JOURNAL.md`, `historique-conversations/2026-10-08-jetons-badges.md`.

**Ajout (07/10/2026, 22 h 10) : KG_HideAndSeek 0.2.0 déployé sur Kal-Games (actif après redémarrage), non testé ; réservation libérée.** Premier test de la 0.1.0 fait par LeKiwi06 avec des joueurs ; six demandes : obscurité (`darkness`) pour les seekers qui attendent dans leur salle ; joueurs de la partie retirés de la barre de localisation (les hiders en mouvement y étaient visibles) ; sons audibles à 30 blocs (réglage `sound-range`) ; hider solide placé debout sur son bloc (il le voit, et son jeu ne le repousse plus : en 0.1.0 il était dans le bloc et redevenait parfois mobile) ; barre du hider réduite à 3 objets (soundboard, changer de bloc, évasion), sons favoris retirés. Détail et limites : `KG_HideAndSeek/JOURNAL.md`.

**Ajout (06/10/2026, 10 h) : KG_Pong 0.1.0, prototype d'un nouveau mini-jeu Pong pour Kal-Games ; compilé, non déployé, non testé en jeu ; réservation gardée jusqu'au test.** Demande de LeKiwi06 du 06/10/2026 : Pong vu de dessus à 2 joueurs, premier à 5 points ; raquettes en vrais blocs qui butent contre les murs ; balle = Sulfur Cube qui a avalé un bloc de glace ; clic gauche pour changer le sens de la raquette, clic droit pour la déplacer (maintenu : en continu). Choix de Claude à confirmer : la trajectoire de la balle est calculée par le plugin, qui place le cube à chaque tick (le bloc de glace ne sert donc qu'à l'apparence) ; les joueurs volent, immobiles, à leur point de vue ; barème 1 point par but et +3 au vainqueur. Rien n'a pu être essayé en jeu : commandes (surtout en Bedrock), rendu de la balle et vue de dessus sont à juger sur le serveur (liste dans `KG_Pong/JOURNAL.md`).

**Ajout (06/10/2026, 5 h 04) : KG_HideAndSeek 0.1.0, nouveau mini-jeu Hide and Seek, déployé sur Kal-Games (actif après redémarrage), non testé en jeu ; réservation libérée.** Demande de LeKiwi06 du 06/10/2026, réglée par questions et réponses (cahier des charges : `KG_HideAndSeek/CAHIER_DES_CHARGES.md`). Les hiders imitent un bloc du décor : en mouvement, joueur normal qui porte son bloc sur la tête (décision de LeKiwi06 pour les joueurs Bedrock, qui ne verraient pas un modèle 3D) ; immobile 3 s, vrai bloc posé dans la copie de l'arène et joueur caché. Seekers à 5 cœurs, un demi-cœur perdu par erreur, absorption en réserve ; soundboard, son automatique, évasion, retour possible après une déconnexion. Aucun changement dans KalGames. Détail, mise en place par un modérateur et liste des points à tester : `KG_HideAndSeek/JOURNAL.md`. Serveur redémarré et arène configurée par LeKiwi06 le 06/10/2026 (map construite sur Kanvas, importée par un schéma WorldEdit, voir le JOURNAL). **Reste à faire : tester avec des joueurs (Java et Bedrock).** Trois lectures de Claude à confirmer par LeKiwi06 : le dernier seeker qui se déconnecte termine la partie (il ne peut donc pas revenir) ; s'il ne reste que des hiders déconnectés, les seekers gagnent ; liste exacte des familles de blocs refusées.

**Ajout (05/10/2026, 15:21) : KG_BoatRace 1.5.1 déployé sur Kal-Games (serveur arrêté par LeKiwi06), non testé, et courses solo recalculées.** Demande de LeKiwi06 : en solo, retirer le tour en tête (« on est forcément en tête ») et réduire de moitié le bonus de tour propre (+0,5 au lieu de +1) ; « il faut que ce changement soit rétroactif ». Solo = un seul coureur dans la course à la fin du tour. Bonus de série, chrono et Grand Prix inchangés. Rétroactif dans `stats.yml` : les 45 courses solo du journal (812 tours) recalculées tour par tour (5 622 → 3 693,38 points, dont -1 282 pour le tour en tête et -646,62 pour le tour propre) ; général, octobre et semaine pour les courses concernées ; archives non touchées ; pas de ligne au journal. Écarts au général : .PatientLime2170 -1 493,24 (18 courses ; 5 992,42 → 4 499,18), .TomHeroes57 -214,87, Maaxster -137,12, .MRMister7866 -67,63, LeKiwi06 -7,75, Exyosis -3,5, JujuEngineer -3, LE_TIGRE32 -1,5. Original dans `/plugins/_removed-kg_scoreboards-stats-2026-10-05-solo/`. À tester : course solo (tour propre 1,5 point avant chrono, pas de « en tête »), course à plusieurs inchangée. Réservation libérée.

**Ajout (05/10/2026, 12:33) : trois demandes de LeKiwi06, appliquées sur Kal-Games (serveur arrêté par LeKiwi06) et Serveur Jeux ; non testé.**
1. **« Récupère tous les points non crédités en admin, crédite-les »** : seul LeKiwi06 en avait (journal de KG_ScoreBoards, raison « operateur », hors rattrapages du 28/09). Crédités dans `stats.yml` : Bingo 373,4 (29/09 : 338,8 avec la moyenne, au lieu de 432,8 ; 01/10 : 34,6), PvP Kit 270,25, Parcours 103,5 (parties à l'ancien barème revalorisées au barème 1.2.0, fourchette haute, comme tout le monde ; podium à 0), course de bateau 37. Une ligne « credit » par événement ajoutée au journal d'octobre (`by` : « Claude (demande de LeKiwi06) » ; mêmes références que `/classements verifier`, qui ne les reproposera pas). Non récupérable : course de bateau du 24/09 (aucun point dans le journal). Temps et records d'opérateur non repris (les meilleurs tours des derniers jours restent proposés par `/classements verifier`).
2. **« Supprime cette règle »** (les points d'un opérateur ne comptent pas) : désactivée par configuration, `stats.exclude-operators: false` dans `KalGames/config.yml` (lu aussi par KG_Bingo) et `KG_ScoreBoards/config.yml`. **L'option existe toujours dans le code, activée par défaut** (KalGames `ScoreBridge.excluded`, KG_Bingo, KG_ScoreBoards) : un serveur installé avec une config neuve l'appliquerait de nouveau.
3. **« En multijoueur on ne gagne plus la somme des joueurs en dessous de nous, mais la moyenne ; rétroactif »** : KG_BoatRace 1.5.0 (Kal-Games) et KG_BingoGame 0.9.0 (Serveur Jeux), les deux seuls jeux qui avaient cette règle : ses points + la moyenne des points de ceux classés en dessous (à 2 joueurs rien ne change). Rétroactif dans `stats.yml` (général ; octobre et semaine pour les parties concernées ; archives non touchées) : 6 courses de bateau à 3 joueurs ou plus (journal) et 7 parties de Bingo (points propres reconstitués depuis les lignes « terminee » des logs de Serveur Jeux, multiplicateur de blackout x1 vérifié ; parties d'avant la 0.8.0 créditées au double : écart doublé). Écarts au général : .PatientLime2170 -699,25 (Bingo -507,17, bateau -192,08), JujuEngineer -214,5 (Bingo), LeKiwi06 -309,33 (Bingo -287,83, bateau -21,5), .TomHeroes57 -77,34 (Bingo -53, bateau -24,34), Maaxster -54 (Bingo). Pas de ligne au journal pour ces écarts.
Originaux (stats, journaux d'octobre, configs) dans `/plugins/_removed-kg_scoreboards-stats-2026-10-05/` et `/plugins/_removed-kalgames-config-2026-10-05/`. Détail : `historique-conversations/2026-10-05-operateurs-moyenne.md`. À tester : une partie en opérateur compte (plus de message « Mode opérateur ») ; course de bateau et Bingo à 3 joueurs ou plus (total crédité = points + moyenne). Réservations libérées.

**Ajout (04/10/2026, 22:26) : points du Parcours recalculés avec le barème de KG_Parkour 1.2.0, dans `stats.yml` de KG_ScoreBoards sur Kal-Games (serveur arrêté par LeKiwi06).** Demande de LeKiwi06 : « prends la fourchette haute pour tout le monde et crédite les points ». Méthode : les 67 parties du journal (depuis le 25/09/2026) rejouées point de contrôle par point de contrôle (points de base selon la position, 1er à valider dans les parties à plusieurs, « sans chute » supposé partout = fourchette haute ; ni bonus de temps ni +25, personne n'ayant fini le parcours) ; points d'avant le journal par règle de trois (rapport du joueur s'il a au moins 20 points dans le journal, sinon rapport global x2,90). Général : .TomHeroes57 334 → 1 108,43 ; LeKiwi06 107 → 291,04 ; .MRMister7866 110 → 250,16 ; Exyosis 73 → 222,61 ; .PatientLime2170 106 → 210,68 ; JujuEngineer 16 → 46,75 ; Ronoxsyb 15 → 43,43 ; Maaxster 25 → 30,66 ; NeirdaMC 7 → 20,27 ; LE_TIGRE32 5 → 16,5 ; .Lyfty4013 4 → 7,5. Octobre : .TomHeroes57 64 → 202,5 ; .MRMister7866 24 → 48 ; LeKiwi06 9 → 32 ; .PatientLime2170 17 → 31,5 ; JujuEngineer 4 → 12 ; Maaxster inchangé à 17 (le recalcul donnait 7,5 : aucun retrait). Semaine : .TomHeroes57 54 → 171. Original dans `/plugins/_removed-kg_scoreboards-stats-2026-10-04/stats.yml`. Non touchés : `archives/2026-09.yml`, archives des semaines, journal des parties ; aucun signal envoyé à KG_Rewards (paliers non déclenchés par ce crédit). Détail : `historique-conversations/2026-10-04-parkour-bareme.md`.

**Ajout (04/10/2026, 22:16) : KG_Parkour 1.2.0 déployé sur Kal-Games, non testé, actif après redémarrage (l'humain)** (demande de LeKiwi06 : nouveau barème du Parcours). Points de base selon la position du checkpoint dans la map (1, 1, 3, 3, 3, 5, 5, 7, 7, 7, 10 ; réglable par point de contrôle), multiplicateurs additifs (1er à valider x1,5, sans chute x1,5, temps x1,25 à x1,75 avec un seul temps par point de contrôle), +25 au 1er arrivé, rien « en 1er » en solo ; l'ancien point par checkpoint et le podium 3 / 2 / 1 sont retirés ; points décimaux crédités à chaque section. À faire après le redémarrage : saisir le « Temps du bonus » de chaque point de contrôle (les temps par section sont dans le journal de KG_ScoreBoards, événement « points », `sectionMillis`). À tester : points de chaque checkpoint, bonus du 1er à 2 joueurs et absent en solo, sans chute, temps, +25, résultats avec les points, crédit au classement (non-opérateur). Réservation libérée.

**Ajout (04/10/2026, 22:01) : KS_Economy 1.3.0 et KS_LootPotions 1.1.3 déployés sur Event, non testés.** KS_Economy : le barème des prix (1 929 objets, `rachats.csv`, produit par un script hors dépôt) est dans le plugin ; « Rachats de la semaine » (`/rachat`, bouton du menu Économie) : 2 objets tirés par gamme de prix chaque lundi, par familles, jamais issus d'une dimension fermée dans KS_Dimensions ; lots de 1 / 10 / 64 / 320 émeraudes ; quota par objet et par jour au-dessus de 25 000 de cagnotte. À tester : le tirage au démarrage (console), vendre un lot et « Tout vendre », un objet enchanté ou abîmé refusé, le quota avec une cagnotte au-dessus de 25 000 (visible puis masquée), une tête et un élixir. Pour refaire un tirage : supprimer `plugins/KS_Economy/rachats.yml` serveur éteint. À décider par LeKiwi06 : prix du Cœur de Spawner, œuf de dragon et Cœur de Spawner dans le tirage ou non. KS_LootPotions : casser un minerai d'émeraude de deepslate (potion de chance à chaque fois, aucune avec Toucher de soie).

**Ajout (04/10/2026, 06:52) : 3 plugins déployés sur Event, non testés** (à garder ensemble) : KS_Crafts 1.10.0, KS_LootBlocs 1.3.0, KS_LootPotions 1.1.2. Décisions de LeKiwi06 : le brassage au bloc de verrue était « une erreur » (retiré) ; la verrue du Nether redevient cultivable mais ne se brasse plus ; la boîte de shulker se fabrique avec un coffre de l'Ender. **Ces choix reviennent sur des demandes de Maxster33 (25/09 : verrue sans drop, alambic au bloc de verrue).** À tester : cultiver de la verrue (drop, potion à 5 % seulement sur une verrue mûre) ; alambic (verrue refusée au clic, au glisser, par entonnoir ; bloc de verrue sans effet ; redstone et poudre à canon toujours possibles sur une potion trouvée) ; boîte de shulker (ancienne recette refusée, nouvelle avec coffre de l'Ender, livre de recettes). Non fait, à décider : craft « 9 verrues → bloc de verrue » toujours retiré.

**Ajout (04/10/2026, 06:05) : KS_LootCoffres 1.1.0 déployé sur Event, non testé** (jambières de la cité antique niveau 30 à 50 : objectif de LeKiwi06, une pièce en diamant avec Raccommodage en 10 à 20 coffres ou estomacs ; les trois autres pièces y étaient déjà). Redémarrage d'Event nécessaire ; seuls les coffres pas encore ouverts sont concernés. **Barème des prix des items (en cours, hors dépôt)** : classeurs dans `sortie/` sur le PC de LeKiwi06 (prix en émeraudes : matières de base, puis somme des ingrédients ; 1 émeraude par minute de temps). Signalé à LeKiwi06, non codé : la verrue du Nether à rendre de nouveau cultivable et à bloquer à l'alambic ; « carapace de shulker seulement si un joueur tue » et « éclat d'écho sur les chauves-souris » ne sont dans aucun plugin.

**Ajout (03/10/2026, 22:47) : déployés, non testés, actifs après redémarrage de chaque serveur (l'humain)** : KLM_Chat 1.0.0 et
KLM_Menu 2.9.0 sur les 6 serveurs Paper ; KLM_Hub 1.0.0 et la ligne de vision nocturne (config de KLM_Portal) sur le lobby.
Rien sur le proxy. Réservations libérées. Sur le lobby, sans redémarrage, `/klmportal reload` suffit pour la vision nocturne.

**Ajout (03/10/2026, 21:27) : tchat global, barre de boss du lobby, recherche de joueurs.**
KLM_Chat 1.0.0 (nouveau : `/global send <message> | true | false`, préfixe du serveur dans la couleur de son portail, contenu
gris clair, séparations `----- GLOBAL -----` / `----- <serveur> -----`), KLM_Hub 1.0.0 (nouveau, lobby : barre violette,
titre vert pâle « KaLium SMC | » + lien défilant Discord / Twitch avec arrêt ; QR codes 30 s en main secondaire, la barre
se vide pendant ce temps), KLM_Menu 2.9.0 (bouton « Recherche de joueurs » de la boussole sous 4 joueurs). Détail :
chaque `JOURNAL.md` et `historique-conversations/2026-10-03-tchat-global-lobby.md`.
Ancienne barre faite avec `/bossbar` sur le lobby : à retirer en jeu si elle existe encore (`/bossbar list`, `/bossbar remove <id>`).
À tester : barre du lobby (couleurs, défilement, arrêt sur chaque lien, Bedrock) ; boussole avec moins de 4 joueurs puis
4 ou plus ; les deux QR codes (scan au téléphone, 30 s, barre qui se vide, impossible à jeter / déplacer, main gauche
déjà prise, déconnexion pendant les 30 s) ; `/global send` entre deux serveurs (préfixe, couleurs, séparations dans les
deux sens), `/global false` puis `true`, joueur Bedrock ; vision nocturne sans clignotement en Java et en Bedrock.
Signalé à LeKiwi06 : pas d'anti-spam ni de lien avec les joueurs muets pour `/global` ; choix `true | false` par serveur.

**Ajout (03/10/2026, 19:06) : KS_Crafts 1.9.0 déployé sur Event, non testé** (toutes ses recettes dans le livre de recettes, débloquées dès qu'on obtient un ingrédient).

**Ajout (03/10/2026, 18:42) : 11 plugins déployés sur Event, non testés** : KS_AntiCheat 1.1.1 (minage par
minerai et par catégorie, minerais posés ignorés ; **correctif de duplication** invsee / ecsee signalé pendant les tests
de Maxster33), KS_EC_Extension 1.1.0, KLM_Menu 2.8.0 (Modération aussi dans la navigation de la boussole),
KS_LootPotions 1.1.1, KS_Economy 1.2.0, KS_FairPlay 1.0.3, KS_KaliumGive 1.8.0, KS_Decapitator 1.1.0 + KS_Crafts 1.8.0
(tête de wither squelette sale / endommagée / désactivée et ses 5 crafts de réparation, bloc de charbon de bois),
KS_Tableau 1.0.0 (/tableau), KS_Jetons 1.0.0 (/jetons ; **prix à 0 = rien en vente**, à fixer). KLM_Menu par
serveur : Event 2.8.0, Kal-Games / Kanvas 2.6.0, autres 2.5.0. Redémarrage d'Event nécessaire.

**Ajout (03/10/2026, 16:41) : revue des plugins d'Event (Kixster) et suites validées par LeKiwi06.**
- Vérifié : aucune erreur de nos plugins dans les 10 démarrages du 03/10 ; les 26 jars maison en service = `jars-deployes/`.
- Compilés, **en attente du déploiement groupé** (voir « Requis parfois » de TRAVAIL_EN_COURS) : KS_AntiCheat 1.0.2
  (écarts Simulation sous 0,05 ignorés, historique des alertes, têtes rares seulement), KS_LootPotions 1.1.1 (pas de
  potion sur les mobs de spawner), KS_Economy 1.2.0 (score enregistré à chaque transaction, boutique à un seul
  acheteur, ventes : notifications, stats, classement ; favoris ; recherche avec filtres), KS_FairPlay 1.0.3,
  KS_Tableau 1.0.0 (nouveau, /tableau).
- Retirés d'Event : ConditionalEvents et PyxelRegions (accord de LeKiwi06).
- Gardé tel quel (LeKiwi06) : le Bedrock Breaker casse aussi la bedrock du fond et du plafond (voulu pour le build).
- Régions : `ile_du_dragon` (End) en place ; **pas de région `spawn`** dans l'overworld.
- LuckPerms : groupe `admin` créé par LeKiwi06 (pas encore de modérateurs).
- **Archives en trop sur Event** (règle des 2 derniers `_removed-…` par plugin ; suppression par l'humain) : klm_menu
  2.0.0, 2.4.0, 2.4.1 ; ks_claim 1.0.0 ; ks_crafts 1.0.0 à 1.4.0 (5) ; ks_economy 1.0.0, 1.0.1, 1.0.2, 1.0.3, 1.1.0,
  1.1.1, 1.1.2 ; ks_enclume 1.0.0, 1.1.0, 1.1.1, 1.1.2 ; ks_estomacgardien 1.0.0 ; ks_fioleexp 1.0.0, 1.1.0, 1.2.0 ;
  ks_kaliumgive 1.0.0 à 1.4.0 (5) ; ks_lootentites 1.0.0, 1.1.0, 1.2.0 (32 dossiers).
- À venir (questions posées à LeKiwi06) : téléportations (/home bed, /spawn, jetons de TP et de localisation), jetons
  de claim, jetons de la mort, coffres de mort. Plus tard : bot Discord (alertes), défi qui génère la monnaie.

**Ajout (03/10/2026, 07:46) : KS_AntiCheat 1.0.1 déployé sur Event, non testé** (invsee / ecsee d'un joueur hors ligne sans attendre sa connexion : lecture de `world/players/data/<uuid>.dat` ; changements toujours appliqués à la reconnexion).

**Ajout (03/10/2026, 06:45) : catégorie 6 « Anti-triche » déployée, non testée.** Event : KS_AntiCheat 1.0.0
(alertes, rubrique Modération : Anti-triche, Invsee, EcSee, Minage ; suspensions ; invsee / ecsee en ligne et hors
ligne avec journal ; morts d'entités ; GrimAC, x-ray, macros, AFK ; revente suspecte ; « Bannir de KaLium » ; règle
tntExplosionDropDecay), KS_Economy 1.1.5, KS_RewardsGUI 1.0.1, CoreProtect CE 24.1, CauldronInteract 1.4.0,
Woodcutter 7.2 (datapack), Paper (`allow-piston-duplication: true`, `lava-obscures: true`). Proxy : KaliumRelay
1.4.0, LibertyBans 1.1.4. Duplication par identifiant : reportée (LeKiwi06). Cahier : `KS_AntiCheat/CAHIER_DES_CHARGES.md`.
**À faire par l'humain** : `relay-token` dans `plugins/KS_AntiCheat/config.yml` (Event) ; redémarrer le proxy puis
Event ; donner `ksanticheat.staff` aux modérateurs (opérateurs par défaut). À tester : alertes, invsee / ecsee (en
ligne, hors ligne), suspension et levée, Bannir de KaLium, Minage, AFK (1 h), CoreProtect (`/co i`), Woodcutter,
CauldronInteract, duplication de TNT.

**Ajout (03/10/2026) : tests de LeKiwi06** : Xaero (fair-play), limite des coffres de KS_FairPlay 1.0.2 (« Exploration journalière : n / 10 ») et barre de l'Élixir de Fortune **validés**. Reste à tester : JourneyMap, spawners naturels, blocs suspects, gardien ancien, entonnoirs, anti-xray.

**Ajout (03/10/2026, 06:13)** : JourneyMap réglé le 03/10/2026 à 06:13 (`/journeymap/server/6.0/journeymap.server.global.config`) : `caveMapping`, `radarEnabled`, `worldPlayerRadar`, `seeUndergroundPlayers` à NONE, radars de joueurs / villageois / animaux / monstres coupés ; surface, relief, biomes, points de repère gardés. `plugins/journeymap/journeymap-server.json` : UUID de l'équipe JourneyMap (codé en dur dans le plugin) retiré des admins, opérateurs gardés. Originaux dans `/_removed-config-2026-10-03/journeymap/`. **Vérifié après le redémarrage d'Event (03/10/2026, 06:14) : réglages en place, JourneyMap et KS_FairPlay 1.0.2 chargés sans erreur.**

**Ajout (03/10/2026, 06:07) : KS_FairPlay 1.0.2 déployé sur Event, non testé** (compteur « Exploration journalière : n / 10 », message de limite sans détails, barre « Élixir de Fortune : m:ss »).

**Ajout (03/10/2026, 05:45) : KS_FairPlay 1.0.1 déployé sur Event, non testé** (« je ne vois pas le compteur de coffres » : comptage au tick suivant si le butin est sorti, message en créatif).

**Ajout (03/10/2026, 05:18) : catégorie 5 « FairPlay » déployée sur Event, non testée.** KS_FairPlay 1.0.0 (code
Xaero à la connexion ; limite globale de 10 par jour : contenants au butin non généré, spawners naturels, blocs
suspects, gardiens anciens tués ; protections ; Chance III libre ; compteur dans la barre d'action et le tchat),
plugins serveur JourneyMap 26.2-6.0.9 et Legacy Freecam 2.0.0, configuration Paper (anti-xray, suivi des entités).
Cahier : `KS_FairPlay/CAHIER_DES_CHARGES.md`. **À faire** : redémarrer Event ; ensuite Claude règle JourneyMap
(radar et grottes coupés, sa configuration n'existe qu'après le premier démarrage) ; règlement « Legacy Freecam
seulement » (humain). À tester (speedrun) : limite et message à 10, Chance III, spawner posé (libre) / naturel,
bloc suspect, gardien ancien, entonnoir sous un coffre de structure, anti-xray (minerais cachés), Xaero.

**Ajout (03/10/2026, 03:22) : KLM_Menu 2.7.0 et KS_Economy 1.1.4 déployés sur Event, non testés.** Rubrique
« Modération » dans `/menu` (staff : outils déclarés par les plugins avec leur permission) ; premier outil :
signalements des magasins. Cahier 6 mis à jour : l'interface staff de KS_AntiCheat (invsee, ecsee, proportions de
minerais, indices de suspicion) ira dans cette rubrique. **KLM_Menu par serveur** : Event 2.7.0 ; Kal-Games et
Kanvas 2.6.0 ; lobby, Serveur Jeux, Kixster 2.5.0 (les trois versions sont dans `jars-deployes/` ; 2.5.0 remis,
retiré par erreur le 03/10 à 01:05).

**Ajout (03/10/2026, 02:35) : KS_Economy 1.1.3 et KS_Claim 1.1.2 déployés sur Event, non testés.** KS_Economy :
catalogue sans son propre magasin, pas d'achat dans sa propre boutique, « Voir l'objet » (coffre en lecture seule),
« Signaler la boutique / le magasin » (raisons + Autre ; staff : `/magasin signalements`, permission
`kseconomy.staff`, opérateurs par défaut : **à donner aux modérateurs par LuckPerms**). KS_Claim : bannissement
retiré (expulser gardé), bannissements existants levés au démarrage. Actifs après redémarrage d'Event.

**Ajout (03/10/2026, 01:49) : KS_Economy 1.1.2 déployé sur Event, non testé** (retours de LeKiwi06 sur les
boutiques, voir son JOURNAL et le cahier « Retours de LeKiwi06 (03/10/2026) ») : recherche d'objet corrigée (noms
français absents du jar), noms français sur les panneaux, panneau en 4 lignes avec l'état (stock, rupture, coffre
plein, fermée), nom de boutique et « Renommer », « Fermer temporairement », suppression depuis le menu : panneau
retiré et rendu, place d'une boutique supprimée prise 3 h, coffres en cuivre. Actif après redémarrage d'Event.

**Ajout (03/10/2026, 01:05) : catégorie 4 « Récompenses » déployée, et correctif des claims ; non testés.**
Proxy : KaliumRelay 1.3.0. Event : KS_RewardsGUI 1.0.0 (nouveau, `/rewards`), KS_KaliumGive 1.7.0, KLM_Menu 2.6.0,
KS_Claim 1.1.1, KS_Economy 1.1.1, KS_BiomeChanger 1.1.1. Kal-Games : KG_ScoreBoards 1.8.0, KG_Rewards 1.0.0 (nouveau),
KLM_Menu 2.6.0. Kanvas : KV_Plots 1.5.0, KV_Rewards 1.0.0 (nouveau), KLM_Menu 2.6.0. Cahier :
`KG_Rewards/CAHIER_DES_CHARGES.md` (aussi dans KS_RewardsGUI et KV_Rewards).
- Correctif signalé par LeKiwi06 (`/ksclaim`, `/ksclaims`, `/magasin` sans réponse) : SimpleClaimSystem 1.13.1 ne crée
  pas son API lui-même (« API not initialized ») ; KS_Claim, KS_Economy et KS_BiomeChanger l'initialisent.
- **À faire par l'humain** : remplir `relay-token` (même valeur que le proxy) dans `plugins/KS_RewardsGUI/config.yml`
  (Event), `plugins/KG_Rewards/config.yml` (Kal-Games), `plugins/KV_Rewards/config.yml` (Kanvas) ; puis redémarrer
  le proxy, Event, Kal-Games et Kanvas. Sans jeton, les récompenses attendent dans `envois.yml` (rien n'est perdu).
- À tester (speedrun) : claims et magasins ; `/kgrewards`, `/kvrewards` (progression, prestige, `admin` : tables de
  butin à remplir) ; une récompense arrive sur Event (`/rewards`, bouton Récompenses) ; classement de la semaine.
- Signalé, non touché : ConditionalEvents sur Event ne lit pas son `config.yml` (erreur de format vers la ligne 10).

**Ajout (03/10/2026, 00:02) : magasins déployés sur Event, non testés** : KS_Economy 1.1.0 (nos propres boutiques,
Tradeshop abandonné ; région WorldGuard `zone_shop` où l'on claime et construit librement ; `/magasin create` 45 000 points
ou 5 blocs tier 3, `/magasin agrandir` 10 000 points +1 boutique ; boutique = panneau sur coffre / tonneau / shulker d'un
claim du magasin ; prix en objet ou en points en attente) + KS_Claim 1.1.0. Cahier mis à jour :
`KS_Economy/CAHIER_DES_CHARGES.md`. À tester (speedrun) : création du magasin, d'une boutique (nom écrit et objet de
l'inventaire, prix en points et en objet), achat sur place et à distance, points en attente, gestion à distance
(verrou), protections (casse, explosion, entonnoir), refus de supprimer un claim avec boutiques.

*Tests d'Event regroupés plus tard (LeKiwi06 : un « speedrun » du serveur pour tout tester) : catégorie 2, 1re partie, et
catégorie 3 restent non testées d'ici là.*

**Ajout (22:26) : catégorie 3 « Claims » déployée sur Event, non testée** : SimpleClaimSystem 1.13.1 (téléchargé avec
l'accord de LeKiwi06 ; configuration et traduction posées avant le premier démarrage), KS_Claim 1.0.0, KS_BiomeChanger
1.1.0. Cahier : `KS_Claim/CAHIER_DES_CHARGES.md` ; réglages de SCS : `KS_Claim/scs/README.md`. **Après le redémarrage
d'Event (l'humain)** : 4 commandes LuckPerms (`lp group default permission set scs.command.claim false`, `.claim.*`,
`.claims`, `.unclaim`) et région de l'île du dragon dans l'End (`//pos1 -200,0,-200`, `//pos2 199,255,199`,
`/rg define ile_du_dragon`, `/rg flag ile_du_dragon scs-claim deny`).
À tester : console (SimpleClaimSystem et KS_Claim chargés, langue fr_FR) ; `/ksclaim` (gratuit jusqu'au 10e, puis
237...) ; bouton Claims du menu d'Event ; membres, bannis, réglages, groupes ; vente et achat entre 2 joueurs ;
suppression et remboursement ; un visiteur ne peut pas ouvrir un coffre mais peut utiliser une table de craft ;
`/claim` refusé aux joueurs ; pas de claim sur l'île du dragon ; Changeur de Biome refusé dans le claim d'un autre ;
barre de boss en français ; Bedrock.

*(Session du 02/10/2026, de 17 h 30 à 17 h 55. Fin de session : réservations libérées.)*

**Textes plus courts dans les menus** (LeKiwi06 : dans les réglages des jeux, les jauges élargies par KLM_Menu 2.5.0
restaient trop longues ; Minecraft n'accepte du texte qu'en haut d'une fenêtre, d'où « écrit des textes plus courts »).
Déployé le 02/10/2026 à 17:49, **non testé**, actif après redémarrage de Kal-Games, du lobby et de Kanvas (l'humain) :
KalGames 1.22.2 (réglages : nom seul dans la jauge, explications en haut du menu), KG_BoatRace 1.4.2, KG_Parkour 1.1.1,
KG_PvpKit 1.0.1 (20 noms de réglages raccourcis, détails dans l'explication ; listes de création du PvP Kit), KG_Bingo
1.6.1 (création d'une partie), KLM_Portal 1.3.2 (lobby, Kanvas : point de chute). Réservation de 6 plugins avec
l'accord de Maxster33 (transmis par LeKiwi06).
À tester : réglages de chaque jeu (Paramètres > jeu > Réglages) et d'un point, création d'un Bingo et d'un PvP Kit,
points de chute (`/klmportal`) : jauges et listes de largeur normale, rien ne défile, explications en haut.

### 2026-10-01 — LeKiwi06

**Ajout (23:42) : aucun texte qui défile dans les menus** (demande de LeKiwi06 : « il ne faut jamais que ça arrive,
c'est illisible », sur tous les plugins). KLM_Menu 2.5.0 (outil `Lisible` : boutons et champs élargis à leur texte,
pour tous les plugins qui utilisent `Gui`) sur les 6 serveurs Paper + KalGames 1.22.1, KG_BingoGame 0.8.4, KS_Economy
1.0.3. **Actifs après redémarrage de chaque serveur (l'humain). Non testé.** À tester : menus de chaque serveur
(boussole, Informations, Classements, Paramètres, création d'un Bingo, menus de Kanvas, Économie) : plus aucun texte
qui défile ; console : message « Texte trop long » éventuel à signaler. Supprimables par l'humain (règle des 3 versions) :
`_removed-klm_menu-2.3.0/` et plus anciens sur chaque serveur.

*(Suite de la session du 30/09/2026, jusqu'à 1 h. Fin de session : réservations libérées.)*

- **KS_LootEntites 1.4.0** (Warden : fioles à 15 %, catalyseur remplacé par un bloc du Deep Dark, émeraude de
  deepslate à 0,01 %) : déployé le 30/09 à 22:44, testé et confirmé par LeKiwi06. Catégorie 1 testée et confirmée.
- **Catégorie 2 « Économie », 1re partie**, déployée sur Event le 01/10/2026 à 00:49, **non testée**, active après
  redémarrage d'Event (l'humain) : KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 + VaultUnlocked 2.20.2 (téléchargé
  avec l'accord de LeKiwi06, ajouté à `telecharger-outils.sh`). Cahier : `KS_Economy/CAHIER_DES_CHARGES.md`.
  Magasins (Tradeshop) : après KS_Claim (catégorie 3).

**Tests à faire (catégorie 2, 1re partie)** :
1. Console : KS_Menu, KS_Economy (« Déclaré comme économie Vault »), KS_Elixir (11 recettes, plus d'avertissement pour
   Fortune), Vault chargés sans erreur.
2. `/menu on` : étoile du Nether en case 5 ; clic droit → menu d'Event avec le bouton « Économie » ; impossible de la
   déplacer ou de la jeter ; `/menu off` la retire ; `/event` ouvre le menu.
3. Menu Économie (`/economie`) : « Tout déposer » avec émeraudes + blocs d'émeraude ; « Déposer des objets » : poser des
   émeraudes et un autre objet, fermer → émeraudes créditées, objet rendu.
4. Liste Tab : ton solde à côté de ton pseudo, vu par un 2e joueur ; « Masquer » → pseudo seul ; « Révéler » → solde.
5. Retrait : émeraudes, bloc, bloc compressé tier 1 et tier 3 ; refus avec solde insuffisant et inventaire plein.
6. Bloc compressé : aspect enchanté, nom, valeur ; impossible à poser, à décrafter (établi et autocrafteur) ; se range
   dans un coffre ; se redépose (tout déposer).
7. Élixir de Fortune : 8 potions de chance + 1 bloc tier 3 → Chance III 10 min.
8. `/echange <joueur>` (2 joueurs à moins de 5 blocs) : accepter par le clic ; objets et montant de chaque côté, aperçu
   chez l'autre ; validation retirée quand on modifie ; échange des deux côtés ; s'éloigner / fermer le panneau /
   se déconnecter → annulé et objets rendus ; montant supérieur au solde refusé ; demande à plus de 5 blocs refusée.
9. Le lendemain après minuit : solde masqué diminué de 1 %.
10. Bedrock : étoile, menu Économie (fenêtres), panneau d'échange et saisie du montant.

### 2026-09-30 — LeKiwi06

*(Session du 30/09/2026, de 17 h 35 à 18 h 15. Fin de session : réservations libérées.)*

**Catégorie 1 « Contenu survie » codée et déployée sur Event le 30/09/2026 à 18:07, testée et confirmée par LeKiwi06 le 30/09/2026** (cahiers :
`KS_Decapitator/CAHIER_DES_CHARGES.md`, `KS_Elixir/CAHIER_DES_CHARGES.md` ; détail dans chaque `JOURNAL.md`) :
KS_Decapitator 1.0.0, KS_Elixir 1.0.0, KS_Crafts 1.7.0, KS_ItemSimple 1.1.0, KS_KaliumGive 1.6.0, KS_LootEntites 1.3.0.
**Actifs après redémarrage d'Event (l'humain).** Textures des têtes : MoreMobHeads (MIT) ; manquent 16 shulkers
colorés, cube de soufre, bébé noyé (à fournir : une ligne dans `KS_Decapitator/src/main/resources/tetes.txt`).
À savoir : la session WinSCP enregistrée d'Event s'appelle `Event [new]` sur le PC de LeKiwi06 (WinSCP.com dans
`%LOCALAPPDATA%\Programs\WinSCP\`, sous Git Bash : `MSYS_NO_PATHCONV=1` et chemin Windows pour `/script=`).

**Tests à faire** :
1. Console au démarrage : « 332 têtes chargées », « 10 recettes d'élixirs ajoutées », avertissement attendu pour la
   recette de Fortune (KS_Economy absent), aucune erreur des 6 plugins.
2. `/tetes` : parcourir les 8 pages, vérifier textures et noms (surtout les bébés) ; noter les têtes fausses.
3. Pose d'une tête puis casse (main, pioche, TNT, piston) : même nom, même texture, s'empile avec une tête neuve.
4. Chute : tuer des mobs (1 %, ex. `/kaliumgive` ne sert pas ici) ; aucune tête pour un mob d'un spawner.
5. Élixirs : `/kaliumgive <pseudo> elixir_titan 1` (et les autres) : nom, couleur, aspect enchanté, effet et durée
   (Rebond II, Fortune III 10 min, Plume 30 min, Phénix 10 min) ; craft en anneau avec de vraies potions allongées ;
   Super Gâteau avec soin I puis II ; Fantôme avec une fiole de 10 niveaux ; Vent avec deux couleurs de harnais.
6. Livre de recettes : ramasser un éclat d'écho → recette de l'élixir de chauve-souris débloquée.
7. Alambic : impossible d'y mettre un élixir (clic, Maj+clic, touche 1-9, entonnoir).
8. Spawners : recette avec une tête de mouton de couleur, une tête de bébé vache, une tête vanilla de zombie et une
   tête custom de zombie.
9. `/kaliumgive` : complétion `tete_…`, `elixir_…` ; `tete_mouton` n'existe plus (`tete_mouton_blanc`).
10. Warden tué : fiole de KS_FioleExp (« Fiole d'expérience (niveau N) » avec les points en description).
11. Bedrock : têtes en main et posées (texture ou Steve ?).

### 2026-09-29 — LeKiwi06

*(Session du 29/09/2026, de 0 h 45 à 17 h 50, mise en pause par LeKiwi06. Fin de session : réservations libérées.)*

**Bot Discord : KLM_DiscordBot (nouveau), cahier des charges `KLM_DiscordBot/CAHIER_DES_CHARGES.md`.** Bot externe en
Node.js qui lit les données de kal-games par une API de KG_ScoreBoards (seul interlocuteur du bot). Décisions de
LeKiwi06 : commandes slash, salon des classements rafraîchi, annonces, récaps (jour et heure réglables) ; profil joueur,
classements, accomplissements, activité ; liaison des comptes par code (`/lier`) ; résultats de toutes les parties dans
un salon avec un fil par jeu ; mention Discord seulement dans les classements mensuels ; `/comparer` avec écarts en % ;
séries de victoires : parties non classées ignorées, PvP Kit en manches ET en matchs. 6 étapes.
- **Étape 1 faite, non testée** :
  - **KG_ScoreBoards 1.7.0** : API HTTP en lecture (`/api/v1/status`, `rankings`, `archives`, `players`, `events` avec
    curseur), jeton obligatoire. **Déployé sur Kal-Games le 29/09/2026 à 2 h 36, serveur allumé** : actif au prochain
    redémarrage. Clés `api.port: 45347` (port attribué par Minestrator) et `api.token` **à ajouter à la main** par
    LeKiwi06 dans `plugins/KG_ScoreBoards/config.yml` (jeton jamais dans le dépôt).
  - **KLM_DiscordBot 0.1.0** : `/classement` (top 10 en image : général, du mois, mois archivé ; points ou meilleur
    tour). Lancement : `npm install` puis `npm start` dans `KLM_DiscordBot/` (mode d'emploi : `README.md`).
- Bot Discord créé par LeKiwi06 dans le portail développeur et invité sur le serveur Discord (7 permissions, lien
  d'invitation avec `permissions=309237763072`). Node.js 24 installé sur le PC de LeKiwi06.
- `KLM_DiscordBot/.env` créé vide sur le PC de LeKiwi06 (ignoré par git) : à remplir (`DISCORD_TOKEN`,
  `DISCORD_GUILD_ID`, `API_TOKEN`).
- À savoir : en **mode automatique**, Claude Code bloque les envois sur les serveurs (WinSCP), même avec l'accord écrit de
  l'humain ; repasser dans le mode où l'on approuve chaque action avant un déploiement. Le serveur SFTP de Minestrator ne
  gère pas la copie distante (`cp` de WinSCP) : passer par un téléchargement puis un envoi.

**Reste à faire (reprise)** : ajouter les clés `api` au `config.yml` de KG_ScoreBoards et redémarrer kal-games (console :
« API du bot Discord à l'écoute sur le port 45347 ») ; remplir `.env` ; lancer le bot et tester `/classement` ; puis
étape 2 (`/stats`, `/activite`, graphiques d'évolution). Supprimables par l'humain sur kal-games :
`_removed-kg_scoreboards-1.3.0/` et `1.4.0/`.

### 2026-10-05 — Maxster33

*(Session du 05/10/2026, de 12 h 54 à 13 h 40. Fin de session : réservations libérées.)*

**KG_BingoGame 0.10.0 (Serveur Jeux) et KG_Bingo 1.7.3 (Kal-Games), déployés à 13:34, non testés, actifs après redémarrage des deux serveurs (à garder ensemble)** : réserve de mondes par lots. Constat de Maxster33 : deux
parties à deux jours d'écart sur la même seed. Cause : en 0.8.5, le monde ajouté à la réserve reprenait la seed du
monde restant, qui pouvait donc revenir indéfiniment jusqu'au redémarrage (parties créées pendant une autre partie).
Désormais : au démarrage, mondes de réserve supprimés puis 3 lots de 4 mondes et 3 lots solo préparés (une seed neuve
par lot) ; une partie créée pendant une autre prend un lot (mondes en trop, équipes vides, partie annulée : supprimés ;
sans autre partie en cours : mondes neufs, réserve non entamée) ; le lot est remplacé
à la fin de la partie, une fois ses mondes effacés du disque ; pendant une partie, seul le terrain de la réserve avance
(5 chunks/s) ; à 4 parties en cours, réserve en pause ; aucun lot libre : mondes créés pour la partie. Nouvelles clés
(valeurs par défaut dans le code) : `instances.reserve-lots-4-teams`, `reserve-lots-solo`,
`reserve-chunks-per-second-during-games`. Détail : `KG_BingoGame/JOURNAL.md`,
`historique-conversations/2026-10-05-bingo-reserve-lots.md`.

**Contre la montre en bingos (KG_BingoGame 0.10.0 + KG_Bingo 1.7.3, mêmes jars, déployés
ensemble)** : victoire avec le nombre de bingos choisi (3 par défaut) au lieu de toute la grille ; 10 bingos au plus
à 1 équipe (tous modes), 12 à plusieurs ; bonus inchangés (temps restant ; bingos comme dans toute partie à 1 équipe,
sans bonus « en 1er »). Réserve de mondes : une partie créée sans autre partie en cours
garde des mondes neufs (la réserve n'est entamée que pendant une autre partie).


### 2026-10-03 — Maxster33

*(Session du 03/10/2026, de 8 h 40 à 9 h 50. Fin de session : réservations libérées.)*

**Bingo, déployé à 09:41 (Serveur Jeux) et 09:44 (Kal-Games), non testé, actif après redémarrage des deux
serveurs** : KG_BingoGame 0.8.5 (+ nouveau `objectives.yml`), KG_Bingo 1.7.0, KG_ScoreBoards 1.9.0 (à garder ensemble).
- 2 lancements ratés ce matin : grille à 25 difficiles alors qu'il n'en existait que 18 -> **7 difficiles ajoutés** (25) ;
  la partie restait ensuite bloquée (« existe deja ») -> corrigé, l'hôte peut relancer et voit la raison.
- **2 parties par heure** et par joueur (60 dernières minutes, opérateurs non limités).
- **Abandon** (bouton ou déconnexion > 10 min) : 0 point de la partie et **-15 au classement** (total négatif possible).
- **Une seule équipe** : aucun bonus « en 1er ».
- **Contre la montre** (1 équipe) : 10 min, +5 min par objectif, grille complète obligatoire ; victoire = bonus habituel
  + 1 pt par 20 s restantes ; temps écoulé = défaite, points des objectifs gardés.
- **Gel de 5 à 6 s à la création d'une partie** (création de l'overworld de chaque équipe sur le thread principal) :
  point d'apparition fourni par un générateur (terrain normal) puis placé sur la terre ferme ; **réserve de 2 mondes**
  créée seulement sans partie en cours, utilisée par une partie créée pendant une autre.
- Erreurs « Impossible de publier ... : null » cette nuit (00:55 à 01:13) : relais injoignable (proxy redémarré
  probablement), reparti seul ; pas un bug. Crédit des points sur Kal-Games non vérifié.

**Correction des classements Bingo (17:04, Kal-Games arrêté)** : 27 parties à une seule équipe jouées avant la 0.8.5
avaient les bonus « en 1er » (objectif, bingo). Détail impossible à recalculer (seuls les totaux sont gardés) : **40 %
des points hors XP retirés** (choix de Maxster33), dans `stats.yml` (général, octobre), `archives/2026-09.yml` et
`archives/semaines/2026-09-26.yml` : .MRMister7866 -2 426,6 (général), Maaxster -452,2, .PatientLime2170 -89,8,
.TomHeroes57 -59,4. Originaux dans `/plugins/_removed-kg_scoreboards-stats-2026-10-03/`. Pas d'écriture dans le
journal des parties ; paliers de récompenses déjà donnés non repris. Détail : `historique-conversations/2026-10-03-bingo.md`.

**KG_BingoGame 0.8.6** (Serveur Jeux, déployé à 12:51, non testé) : en 0.8.5 le départ pouvait tomber au milieu de
l'océan (terre cherchée seulement à 160 blocs du centre) ; désormais biome terrestre le plus proche, jusqu'à 3000 blocs.
Le gel à la création a bien disparu (« Prepared spawn area in 0 ms »).

**KG_BingoObservateur 0.1.0** (nouveau, Serveur Jeux, déployé à 10:18, non testé, actif après redémarrage) :
`/observer` donne aux opérateurs une longue-vue « Observer un joueur » (liste groupée par partie, vue libre ou dans ses
yeux, `/observer quitter`, retour automatique en fin de partie).

À tester (liste complète dans `historique-conversations/2026-10-03-bingo.md`) : observateur (longue-vue, deux vues,
Nether / mort du joueur suivi, `/observer quitter`, retour en fin de partie et mondes bien supprimés, refus pour un
non-opérateur) ; grille à 25 difficiles, limite de
création, -15 à l'abandon (bouton, déconnexion, joueur seul), pas de « en 1er » à une équipe, contre la montre (chrono,
victoire, temps écoulé, redémarrage), plus de « Prepared spawn area in 5000 ms », départ sur la terre ferme, logs
« Réserve : ... », 2e partie pendant une autre sans gel.

### 2026-10-01 — Maxster33

*(Session du 01/10/2026, de 19 h 30 à 20 h 25. Fin de session : réservations libérées.)*

- **Chat Bedrock (réglé)** : « Chat désactivé à cause de l'absence de la clé publique du profil » sur tous les
  serveurs depuis toujours. Cause : `enforce-secure-profile=true` (valeur par défaut) dans `server.properties` des
  serveurs Paper ; les joueurs Bedrock (Floodgate) n'ont pas de clé de profil. Réglé par Maxster33 (passé à `false`) ;
  confirmé par Maxster33 le 01/10/2026. Pour tout nouveau serveur Paper : `enforce-secure-profile=false`.
- **Paramètres inaccessibles sur PS5** (signalé par Maxster33, pas testé sur les autres plateformes) : non étudié ;
  aucune cause trouvée dans notre code. À préciser : bouton grisé ou écran qui se referme, sur quel serveur.
- **KS_Enclume 1.2.0** (Event, déployé à 19:50, non testé, actif après redémarrage d'Event) : coût vanilla jusqu'à
  50 niveaux, partie au-dessus comptée pour moitié (70 → 60, 100 → 75) ; fioles de KS_FioleExp inchangées.
  À tester : réparation / fusion à plus de 50 niveaux en vanilla (Java et Bedrock, « Coût réel » et niveaux retirés),
  fiole remplie à l'enclume (coût inchangé).
- **KS_LootBlocs 1.2.0** (Event, déployé à 20:24, non testé, actif après redémarrage d'Event) : fer et or bruts =
  vanilla x2 (2, puis Fortune vanilla : jusqu'à 8 en Fortune III) au lieu de 2 à 5 « comme le cuivre » (trop). À tester :
  minage sans Fortune (toujours 2), avec Fortune III (2 à 8), Toucher de soie (bloc de minerai).

---


## Version complète de REPRISE_PROJET.md du 23/09/2026 (avant la restructuration du 24/09/2026)

Auteur non précisé. Rédigée pour la sauvegarde `KalProjet-sauvegarde-2026-09-23.zip`, avant la création du dépôt git.

## Reprise du projet KaLium (KalBingo / KalGames) — sauvegarde du 23/09/2026 (mise à jour après Rush)

Document de reprise pour la prochaine session. **À donner à Claude en premier** (avec l'archive
entière) : il contient l'état exact du projet, ce qui est déployé, comment compiler et déployer, et
les points encore ouverts. L'historique détaillé de chaque plugin est dans son `JOURNAL.md`
(`sources/<plugin>/JOURNAL.md`), à lire ensuite pour le plugin concerné.

Autres documents utiles déjà présents dans le dossier Téléchargements de l'utilisateur :
`to_do_list_Kal_Games_Bingo.txt` (cahier des charges Bingo) et
`Procedure-Claude-controle-PC-deploiement.md` (procédure de contrôle du PC / déploiement).

### Règles du projet (toujours en vigueur)

- Ne jamais construire quelque chose qui n'a pas été explicitement demandé.
- En cas de doute technique ou d'ambiguïté : proposer / poser la question AVANT d'implémenter,
  jamais deviner.
- Déploiement non destructif : l'ancien jar est toujours déplacé (jamais supprimé) dans
  `/plugins/_removed-<plugin>-<ancienne version>/` avant d'envoyer le nouveau.
- Les messages et textes en jeu sont en français.

### Architecture du réseau

| Rôle | Nom Velocity | Hôte SFTP (onglet WinSCP) | Plugin(s) à nous |
|---|---|---|---|
| Proxy Velocity | — | `ProxyVelocity@7018.mystrator.com` | KaliumRelay 1.1.0 (relais HTTP, port 46199) |
| Hub mini-jeux | `kal-games` | `kalgames@7021.mystrator.com` | KalGames 1.12.3, KaliumMenu 1.5.0 |
| Serveur Bingo (dédié) | `kixster` | `kixster@7003.mystrator.com` | KalBingo 0.1.22 |
| Serveur de test survie | `Kal-Test-Dev` | — | KaliumCore 1.4.0 (projet en pause, voir son JOURNAL) |

- **KalGames** : le hub. Menu "Bingo" pour créer une partie (équipes, taille, durée ≤ max défini par
  les opérateurs) ou rejoindre une partie listée (parties encore en salle d'attente, 6 max affichées).
  Transfère les joueurs vers Kixster.
- **KalBingo** : tout le jeu Bingo, sur Kixster. Salle d'attente (modèle capturé par l'admin via
  `/menu`, collé sur 4 emplacements), choix des équipes par l'hôte, une map (monde) par équipe avec la
  même seed, grille 5×5, validation, score, HUD, fin de partie, abandon, persistance des parties en
  cours (survivent à un redémarrage).
- **Communication kal-games ↔ Kixster** : deux chemins en parallèle.
  1. Canal BungeeCord "Forward" — ne livre un message QUE si au moins un joueur est connecté sur le
     serveur CIBLE (limite du protocole Minecraft, source de plusieurs bugs passés).
  2. Relais HTTP KaliumRelay (sur le proxy) — indépendant des joueurs. Endpoints :
     `/assignment/<clé>` (stockage clé → texte, POST dépose, GET lit et efface, expire en 2 min) et
     `/active-game/<uuid>` (joueur "en partie" sur un serveur, pour la reconnexion directe).
     Jeton : `relay-token` dans les config.yml de KalGames (`bingo.`) et KalBingo (`network.`).
- **Signal "partie fermée"** (démarrée/annulée → retirée de la liste kal-games) : Forward + clé relais
  `party-closed-<gameId>` republiée chaque minute pendant 6 h ; kal-games interroge le relais toutes
  les 5 s.

### Versions déployées au 23/09/2026 (toutes vérifiées dans WinSCP après actualisation forcée)

- Kixster `/plugins/KalBingo-0.1.22.jar` (Nether et End propres à chaque équipe, keepInventory en partie,
  suppression des maps corrigée ; salle d'attente jusqu'à 256 blocs par côté ; config.yml déployé modifié : `lobby.max-size: 256`, `lobby.blocks-per-tick: 30000`)
- kal-games `/plugins/KalGames-1.12.3.jar` (Rush jouable, NON encore testé en jeu ; 1.12.3 = correctif du crash
  pendant la capture d'arène, voir son JOURNAL)
- proxy `/plugins/KaliumRelay-1.1.0.jar`
- kal-games `/plugins/KaliumMenu-1.5.0.jar` (bouton Paramètres des téléportations). Lobby
  (`lobby@7002.mystrator.com`, onglet WinSCP ajouté par l'utilisateur) : KaliumMenu 1.5.0 présent.

Confirmé par l'utilisateur le 23/09/2026 (avant Rush) : "tout fonctionne très bien".

### Chantier en cours : mini-jeu Rush (KalGames 1.11.0 → 1.12.3)

Cahier des charges complet, décisions prises et fonctionnement : `sources/KalGames/JOURNAL.md`, sections
1.11.0 à 1.12.3. Code : `game/RushInstance.java` (moteur), `game/RushItems.java` (monnaies + 45 échanges),
`listener/RushListener.java`, `model/RushLayout.java` (équipes, PNJ, points d'arène). Prochaine étape : retours
de test de l'utilisateur (arène à configurer : salle d'attente + 11 points par base).

### Contenu de cette sauvegarde

- `sources/` : code source complet des 5 plugins (KalBingo, KalGames, KaliumCore, KaliumMenu,
  KaliumRelay), avec `build.sh`, `pom.xml` et `JOURNAL.md`.
- `outils-build/` : compilateur `ecj.jar` + toutes les bibliothèques (`libs/`, dont `paper-api`
  26.2.build.123) utilisés pour compiler — permet de recompiler à l'identique sans rien retélécharger.
- `jars-deployes/` : les jars actuellement en service (copie de secours).

### Compiler sur un PC (depuis le dépôt git) - mis en place le 24/09/2026

Prérequis : Java 21 ou plus (testé avec Temurin 25) et Git Bash sous Windows.

```sh
sh telecharger-outils.sh      # une seule fois par machine : compilateur ecj + bibliothèques dans outils-build/
sh KalBingo/build.sh          # -> sortie/KalBingo-<version>.jar (idem pour les autres plugins)
```

`outils-build/` et `sortie/` sont ignorés par git. Versions utilisées : ecj 3.46.100, paper-api
26.2.build.123 (et ses dépendances, voir le script). Vérifié le 24/09/2026 : les 5 jars recompilés ainsi sont
identiques octet pour octet à ceux de `jars-deployes/` (hors manifeste). Le fichier `.gitattributes` impose des
fins de ligne LF : sans lui, Git pour Windows convertit les config.yml en CRLF et les jars ne sont plus identiques.

### Compiler (dans l'espace de travail cloud de Claude)

Sans dossier `outils-build/` à la racine, les `build.sh` utilisent `/tmp/claude-0/` et écrivent dans
`/mnt/user-data/outputs/`. Pour restaurer :

```sh
unzip KalProjet-sauvegarde-2026-09-23.zip -d /home/claude/restore
mkdir -p /tmp/claude-0 && cp -r /home/claude/restore/outils-build/* /tmp/claude-0/
cp -r /home/claude/restore/sources/* /home/claude/
cd /home/claude/KalBingo && sh build.sh   # -> /mnt/user-data/outputs/KalBingo-<version>.jar
```

Chaque nouvelle version : incrémenter `VERSION=` dans `build.sh` ET `<version>` dans `pom.xml`, puis
ajouter une section au `JOURNAL.md` du plugin (ce qui a été demandé, la cause, ce qui a changé, les
limites, la date de déploiement).

### Déployer (WinSCP sur le PC de l'utilisateur)

1. Envoyer le jar dans la conversation, puis l'écrire dans `C:\Users\HP Zbook 17 G3\Downloads\`.
2. Dans WinSCP, onglet du bon serveur, panneau distant sur `/plugins/`, **Ctrl+R pour actualiser**
   (le panneau distant peut afficher un état périmé : ne jamais se fier à une liste non actualisée).
3. Sélectionner l'ancien jar (vérifier le nom et la taille dans la barre d'état), Maj+F6, chemin
   `/plugins/_removed-<plugin>-<ancienne version>/<ancien jar>`, Entrée.
4. Panneau local : Downloads (le dossier "Claude outputs" est un sous-dossier), sélectionner le
   nouveau jar, F5, OK. Actualiser et vérifier qu'il n'y a qu'un seul jar du plugin.
5. L'utilisateur redémarre le serveur (pas d'accès console/RCON, uniquement SFTP).

Pièges connus : ne pas double-cliquer sur un fichier (ouvre l'éditeur interne — refermer SANS
enregistrer) ; taper du texte quand la liste a le focus lance une recherche, pas une navigation ;
autoriser `Textinputhost` (clavier tactile Windows) si des clics échouent.

### Points ouverts / limites connues (rien de bloquant)

- KalBingo 0.1.22 (Nether / End par équipe, keepInventory) : à tester en jeu. Sur Paper 26.x les mondes ajoutés
  sont dans `Kixster SMP/dimensions/minecraft/` ; 18 anciens dossiers `bingo_<uuid>_<n>` de parties terminées
  avant 0.1.22 y restent (jamais effacés à cause de l'ancien bug) : à supprimer par l'utilisateur s'il le souhaite.

- KalGames 1.12.3 (capture / effacement / collage sans chargement synchrone de chunks) : à confirmer en jeu par une
  recapture du parkour. Réglages : `arenas.capture-blocks-per-tick` (30 000), `instances.wipe-blocks-per-tick`
  (20 000), `instances.paste-blocks-per-tick` (40 000). Journaux du crash du 23/09 : `Downloads\Claude outputs\kalgames-logs\`.

- Création d'une map : bloque le serveur Kixster ~8 s par map (génération synchrone du monde par le
  jeu). Proposé à l'utilisateur, pas encore traité.
- Un joueur déconnecté au moment exact du lancement ne reçoit ni kit de départ ni soin à son retour.
- Recapture du modèle : seuls les emplacements actuellement configurés sont effacés.
- Salle d'attente : monstres et PvP bloqués dans tout le monde `bingo_lobby` (modèle compris) ; la
  protection des blocs ne couvre plus que les salles collées.
- La pré-génération ne tient pas compte de `instances.max-simultaneous-games`.
- `reconnect-during-game.abandon-after-seconds` : détection seulement, aucune action.
- KalGames `bingo.max-party-size` n'est plus utilisé (capacité = équipes × taille).
- Proxy : `geyserupdater-spigot.jar` (plugin Spigot) dans les plugins Velocity → erreur de chargement
  au démarrage, sans conséquence. Non touché (pas demandé).
- Réglages utiles côté Kixster (`plugins/KalBingo/config.yml`) :
  `instances.pregeneration-radius-blocks` (200), `instances.pregeneration-stagger-seconds` (10),
  `game.post-game-lobby-timeout-seconds` (600), `game.no-players-abandon-after-seconds` (600).
  Toute nouvelle clé doit être AJOUTÉE à la main dans le fichier déployé (le plugin ne réécrit pas
  un config.yml existant) ; redémarrage requis.

### 2026-09-24 — LeKiwi06

*(Fin de session : toutes les réservations libérées ; KG_Menu et les versions qui l'accompagnent restent à déployer.)*

**Mise en route et règles**
- Dépôt cloné sur le PC de LeKiwi06, compilation locale (`telecharger-outils.sh`, `build.sh` PC + cloud,
  `.gitattributes` LF) : jars recompilés identiques à ceux en service.
- `REGLES.md` créé et complété : réservations en **deux catégories** (utilisés actuellement / requis parfois) avec
  **demandes de créneau** auxquelles le Claude de l'autre peut répondre ; 2 plugins « utilisés » max par personne de
  13 h à 23 h (heure de Paris, `date` simple sous Git Bash) ; un plugin = un rôle ; secrets seulement sur les
  serveurs ; non destructif (`_removed-…`, **2 derniers gardés par plugin**, suppression par l'humain).
  `REPRISE_PROJET.md` restructuré, ancienne version dans `archive_reprise.md`.
- Sécurité : jeton du relais remplacé sur les 3 serveurs par LeKiwi06 (jamais écrit dans le dépôt ni la
  conversation) ; KaliumRelay 1.1.1 sans jeton dans le code.
- WinSCP : tous les serveurs enregistrés avec mot de passe (Kal-games, Kal-test-dev, Kixster, Lobby, Proxy Velocity) ;
  Claude déploie par ces sites sans jamais taper de mot de passe ; l'humain arrête / redémarre.

**Bingo (terminé et déployé)**
- Découpage : `KG_Bingo` (hub) + KalBingo renommé `KG_BingoGame` (Kixster migré).
- Nouveau Bingo (KG_BingoGame 0.3.0 → 0.5.0) : 200 objectifs validés ; modes Bingos (3 à 12, chrono) et Blackout ;
  composition de la grille au choix ; barème en temps réel (1/3/5/10, 1re équipe, coefficients additifs, victoire,
  classement cumulé équipe et solo) ; blackout gagné en moins de 2 h : ×2 / ×1,5 (difficile + extrême : ×5 / ×2) ;
  nulle par vote, « Égalité » au score, abandon (10 min), inactivité (5 min), invincibilité de fin, keepInventory,
  papier verrouillé ; carte de la grille en main secondaire (icônes du jeu officiel, contour des objets blancs),
  couleurs d'équipe A rouge / B bleu / C jaune / D verte, têtes des coéquipiers, résumé de fin de partie ; mode solo
  gardé pour le speedrun.
- Testé par LeKiwi06 : « propre et sans bugs ». Limite acceptée : têtes invisibles sur Bedrock. Le « Nether commun »
  vu en test venait de l'ancienne 0.1.20.

**Classements : KG_ScoreBoards**
- Plugin autonome (étape A faite et déployée) : classements sortis de KalGames, données migrées sur kal-games
  (`stats.yml`, `boards.yml`), `StatsService` compilé identique. Panneaux du hub : Paramètres > Mini-jeux Kal-Games >
  mini-jeu > Classements > Panneaux dans le hub.
- Décisions pour la suite (étape B, non faite) : toutes les parties enregistrées, par joueur et par catégorie (solo /
  duo / trio / squad, blackout) : points, objectifs en 1er, bingos, victoire / défaite / nulle ; parties sans
  adversaire comptées ; seuls les classements individuels affichés au début ; archives détaillées (futur bot
  Discord) ; résultats du Bingo transmis par KaliumRelay ; format des scores : 6 chiffres max, puis K / M / Md.

**Interfaces : KLM_Menu et KG_Menu**
- Hiérarchie décidée : **KLM_Menu** (tous les serveurs : navigation, boussole, boîte à outils des menus, catalogue
  « Interfaces ») → **menu de chaque serveur** (KG_Menu pour kal-games ; plus tard créa, skyblock, survie) →
  interfaces des jeux, **découvertes au démarrage**. Les mini-jeux gardent leurs propres écrans. Protections de zone
  du hub : restent dans KalGames pour l'instant.
- KLM_Menu 2.0.0 (KaliumMenu renommé) déployé sur kal-games, lobby et Kixster (boussole désactivée sur Kixster :
  c'est un objectif du Bingo). Bug corrigé : plus de boussole au hub (KalGames 1.15.1).
- Compilés, **non déployés** : KG_Menu 1.0.0, KLM_Menu 2.1.0, KalGames 1.16.0, KG_Bingo 1.3.0, KG_ScoreBoards 1.2.0
  (à installer ensemble sur kal-games, voir leurs JOURNAL).
- Prévu : phase 2 (accès des admins aux interfaces des autres serveurs, via KaliumRelay ; limite : menus en coffre et
  actions sur le joueur impossibles à distance).

**À faire / à savoir**
- Liste des parties Bingo : pseudo de l'hôte figé dans `lang.yml` et affichage des équipes ambigu → à corriger
  (KG_Bingo / KG_Menu).
- À tester : pseudos dans les annonces à 3 comptes ou plus, nulle en groupe, multiplicateur du blackout, catalogue
  « Interfaces », KG_Menu.
- Tri des anciens dossiers `_removed-…` et `.bak` : après le découpage des plugins.
- Vus dans les journaux, hors de notre travail : config de ConditionalEvents invalide (kal-games), AnvilUnlocker sans
  ProtocolLib, 2 voicechat et Geyser sur Kixster, geyserupdater-spigot sur le proxy.
- Points 2 et 3 de la première lecture : à rappeler quand LeKiwi06 le demande.
- Erreurs de Claude, corrigées : commits incomplets (étape B du Bingo, journaux de KG_Menu), horodatages en UTC,
  journaux de serveur posés un moment dans le dossier du dépôt / dans Documents, deux dossiers de Kal-test-dev
  listés par erreur, ancien nom KaliumMenu non recherché avant le renommage (boussole du hub).

### 2026-09-24 — Maxster33

**Dépôt** : import du bundle `Kalium-depot.bundle` sur `Maxster33/Kalium_depot` (main + 5 étiquettes), dossier
`historique-conversations/` créé.

**Raccordement de nouveaux serveurs au proxy (étape 1 de la migration)** - fait par Claude via WinSCP, sauvegardes
dans `_removed-config-2026-09-24/` (racine de chaque serveur) et `plugins/_removed-klm_menu-2.0.0/`,
`plugins/_removed-voicechat-2.6.23/` (lobby) :
- Proxy : `kalgames2`, `serveur-jeux`, `kal-test-dev` ajoutés dans `velocity.toml` (le renommage `Kixster` →
  `Bingo` fait entre-temps par LeKiwi06 est conservé).
- KalGames2 et Serveur Jeux (Paper neufs) : transfert Velocity activé (`paper-global.yml`), `online-mode=false`,
  liste blanche désactivée ; plugins communs du lobby **avec leurs configurations** (Floodgate + clé du proxy,
  BedrockSkinRestorer, VelocityCommandForward, LuckPerms (base H2 du lobby copiée), voicechat, WorldEdit, WorldGuard,
  ConditionalEvents, PyxelRegions) + KLM_Menu 2.0.0 ; voicechat : ports 43374 / 43131.
- Kal-Test-Dev : transfert Velocity activé (il était en `online-mode=false` SANS proxy : tout le monde pouvait s'y
  connecter sous n'importe quel pseudo), clé Floodgate remplacée par celle du proxy, KLM_Menu 2.0.0, voicechat 45595.
- Lobby : KLM_Menu, 3 nouvelles destinations (désactivées, à activer en jeu) ; voicechat : port 24454 → 43841.
- Non fait : voicechat de Kixster / kal-games / proxy ; redémarrages (humain).
- Erreurs de Claude : deux valeurs secrètes (secret Velocity, `management-server-secret`) affichées dans la
  conversation pendant une comparaison de fichiers (jamais publiées) ; un jar ouvert par erreur dans l'éditeur de
  WinSCP, refermé sans enregistrer.

**Migration complète (après-midi du 24/09/2026)** - LeKiwi06 avait libéré KG_Bingo / KG_BingoGame :
- kal-games et Kixster arrêtés par Maxster33, **copiés en entier** (912 Mo et 791 Mo, dossiers cachés `.paper`,
  `.cache`, `.ai-backups` compris) sur KalGames2 et Serveur Jeux ; ancien contenu des nouveaux serveurs dans
  `_removed-avant-migration-2026-09-24/`. Vérifié par comparaison fichier par fichier (seules différences : les
  réglages ci-dessous).
- Réglages : ports de jeu et voicechat des nouveaux serveurs, KG_Bingo `bingo.server-name: serveur-jeux`,
  KG_BingoGame `network.self-server-name: "serveur-jeux"` (clé ajoutée). **Aucun changement de code** : KaliumRelay
  ne connaît aucun nom de serveur (il relaie ceux que les plugins lui donnent), KLM_Menu des serveurs de jeu renvoie
  seulement au lobby, et `kal-games` reste le nom du hub pour KG_BingoGame.
- Proxy : `kal-games` → KalGames2, `event` → ancien kal-games, `kixster` → Kixster, `serveur-jeux` ; `Bingo` et
  `kalgames2` supprimés. Lobby : destinations KLM_Menu `kixster` (désactivée), `kal-games`, `serveur-jeux`,
  `kal-test-dev`, `event` (désactivée).
- KG_BingoGame n'était plus actif sur Kixster (0.5.0 rangée à 11:57) : 0.5.0 réinstallée sur Serveur Jeux.
- Ajout de Floodgate, BedrockSkinRestorer, VelocityCommandForward sur les 2 nouveaux serveurs (absents des originaux).
- PC de Maxster33 : Java 27 installé, `telecharger-outils.sh` fait, compilation vérifiée (KaliumRelay). Sous Git Bash,
  Java 27 n'est pas dans le PATH tant que Git Bash n'a pas été relancé : `export PATH="/c/Program Files/Java/jdk-27/bin:$PATH"`.
- Travail désormais piloté par WinSCP.com + scripts (guide du Claude de LeKiwi06), plus par captures d'écran.
- **Correctifs et nettoyage (fin d'après-midi du 24/09/2026)** : KalGames2 : `PlaceholderAPIScoreboardObjectivesPlaceholder.jar`
  (extension PlaceholderAPI, pas un plugin) déplacé dans `plugins/PlaceholderAPI/expansions/`. Serveur Jeux : voicechat
  2.6.23 rangé (2.6.24 gardé), AnvilUnlocker retiré (sans ProtocolLib, inutile au Bingo). Kixster rendu au SMP : monde
  d'origine restauré, `bukkit.yml` sans générateur Bingo, KG_BingoGame + anciens `_removed-kalbingo/kg_bingogame` +
  mondes ratés (`_failed_v0.1.3`, `_crashed_0.1.4`) + monde vide du Bingo (avec `bingo_lobby`) + fichier `Depot`
  (journal envoyé par erreur à la place d'un jar le 24/09 à 12:05) rangés dans `_removed-bingo-2026-09-24/` ;
  voicechat 2.6.24 rangé ; WorldEdit gardé ; `generate-structures=false` laissé (décisions de Maxster33).
- **Migration confirmée par Maxster33 le 24/09/2026** (« tout a l'air de fonctionner correctement ») après redémarrage ; journaux de démarrage de KalGames2 et Serveur Jeux : Paper 26.2-122, seules erreurs = celles déjà présentes sur les originaux (jar `PlaceholderAPIScoreboardObjectivesPlaceholder.jar` illisible sur le hub ; 2 voicechat et AnvilUnlocker sans ProtocolLib sur le Bingo), non corrigées (pas demandé).
- À faire : nettoyage de Kixster (SMP) et
  reconversion d'Event ; KG_BoatRace (LeKiwi06) à déployer sur KalGames2.

---

### 2026-09-25 — LeKiwi06

*(Session du 24/09/2026 fin d'après-midi au 25/09/2026 midi. Fin de session : réservations libérées.)*

**Déploiements et vérifications**
- KG_BingoGame 0.6.0 (génération des maps étalée) déployée sur Serveur Jeux, testée : « plus aucun lag à la
  génération » ; lot KG_Menu 1.0.0 / KLM_Menu 2.1.0 / KalGames 1.16.0 / KG_Bingo 1.3.0 / KG_ScoreBoards 1.2.0 déployé
  sur Kal-Games et testé (Java et Bedrock). Vérifié après la migration de Maxster33 : `velocity.toml`, transfert
  Velocity actif et secret identique sur les 6 serveurs, noms `serveur-jeux` dans les configs du Bingo.
- `kal-test-dev` renommé `Kanvas` dans Velocity par LeKiwi06 (destination de la boussole du lobby à mettre à jour).

**Course de bateau : KG_BoatRace (nouveau plugin, 1.0.0 → 1.4.1)**
- KalGames 1.17.0 : types de mini-jeux ouverts aux plugins de jeu (moteur, réglages, classement, options) ; la course
  sort dans KG_BoatRace sans rien reconfigurer. KalGames 1.18.0 : éditeur de listes de points (checkpoints : insérer,
  remplacer, supprimer, pages) et réglages par point (pour le Parkour).
- KG_BoatRace : vitesse en km/h, seuil de 45 s pour les meilleurs temps, classement en direct (tableau latéral
  personnel avec meilleur tour et « Ton record »), écarts à chaque checkpoint, barème de points (tours, tours propres,
  séries, chrono, tour en tête, Grand Prix, cumul crédité), hors-piste (sous le joueur + contact qui ralentit),
  anti-collision Java et Bedrock (copies sans collision). Tout testé en jeu, avec des joueurs Bedrock.
- KG_ScoreBoards 1.3.0 / 1.4.0 : journal des parties (`journal/<mois>/<mini-jeu>.jsonl`, pour le bot Discord) et
  points décimaux.

**Décisions**
- Cahiers des charges KG_BoatRace et KG_Parkour, architecture cible et charte du réseau : voir les fichiers du dépôt.
- Ordre : d'abord les jeux les plus joués (course, Parkour, PvP Kit), le Rush plus tard.
- GrimAC fait l'anti-triche « dur » ; le futur KG_AntiCheat analysera les données (gains, minerais, échanges,
  revente contre de l'argent réel, traçabilité des objets de valeur).

**À faire / à savoir**
- Capturer la nouvelle salle d'attente du Bingo ; tester Nether / End de la 0.6.0 et 2 parties simultanées.
- Suite de KG_BoatRace (étapes 4, 5, 6) puis KG_Parkour.
- Erreurs de Claude, corrigées : un journal envoyé à la place d'un jar (chemin avec espaces, contourné par des
  scripts WinSCP) ; secret Velocity affiché pendant une vérification ; méthode supprimée par erreur lors d'une
  modification (vue à la compilation).

---

## Comptes rendus de LeKiwi06 retirés de REPRISE_PROJET.md le 27/09/2026

### 2026-09-26 — LeKiwi06

*(Session de la nuit du 26/09/2026, de 0 h à 6 h 45. Fin de session : toutes les réservations libérées.)*

**Kanvas (serveur de plots en créatif) : testé et confirmé par LeKiwi06**
- Cahier des charges complété (`KV_Plots/CAHIER_DES_CHARGES.md`) ; monde `New World (2)` renommé `Kanvas` ; grille de
  121 plots générée (plot de référence : coin -107 / -57, plots 49 x 49, routes 9).
- KV_Plots 1.4.0 et KV_Menu 1.3.0 : réservation (moyen / grand), éditeurs, protection WorldGuard, remise à zéro /
  suppression, créatif gardé, validation, votes aux terracottas, déblocage à 100 points, titre / description, tableau
  sur le côté, visites, signalements, concours de build ; étoile du Nether (menu) en case 4.
- FAWE Paper (la variante Bukkit ne marchait pas), limites, permissions LuckPerms du groupe `default`, baguette de
  navigation = vide de structure ; tri des plugins de Kanvas.

**Kal-Games / Serveur Jeux**
- Testés : KG_Parkour 1.0.0 + KalGames 1.20.0 (le Parcours sort de KalGames) ; KG_BingoGame 0.8.0 + KG_Bingo 1.5.0
  (barème du Bingo doublé, points du Bingo dans les classements).
- **KG_BingoGame 0.8.2 déployé, non testé** : bonus du 1er = moitié des points de base ; partie à 4 équipes (joueurs
  non téléportés : reconnexion et joueurs sans équipe ; barre d'action courte à partir de 3 équipes) ; icônes de la
  carte (blocs en 3D comme dans l'inventaire, tout en 22 pixels) ; 7 objets courants du Nether passés en Normal
  (`objectives.yml` du serveur remplacé).
- Nettoyage : nouvelle règle (`REGLES.md` 4.3 : `_removed-…` supprimables à 3 versions ou plus de la version en
  service, suppression par l'humain) ; 85 dossiers supprimés par LeKiwi06 avec des scripts WinSCP préparés par Claude.

**À faire plus tard** (demande de LeKiwi06, 26/09/2026)
- **Tester KG_Bingo 1.5.1** (Kal-Games) : les parties qui ne démarrent pas quittent la liste au bout de 30 min (message
  dans la console) ; les parties bloquées de la nuit ont disparu au redémarrage.
- **Tester KG_BingoGame 0.8.2** (icônes de la carte : confirmées) : bonus du 1er,
  Nether en Normal, partie à 3-4 équipes (barre d'action, joueur sans équipe placé, reconnexion pendant la
  préparation).
- **Équilibrage des barèmes** (`EQUILIBRAGE_POINTS.md`) : barème du Parcours (jouer quelques parties « à fond » pour
  mesurer), PvP Kit, Rush, recalcul du passé (dont les anciennes parties de Bingo, jamais créditées).
- **Kanvas** : limites d'entités et mobs sans IA ; agrandissement moyen → grand (et « dupliquer en version grande ») ;
  classements KV_ScoreBoards (solo / duo / équipe, général et du mois) ; extension automatique du monde.
- **KG_Parkour** (cahier des charges) : chrono allongé à chaque checkpoint, barème, contre-la-montre avec fantôme,
  anti-collision.
- **Sauvegardes des mondes** (voir « Points ouverts »).
- Dossiers gardés volontairement : plugins retirés de Kanvas le 26/09 (à supprimer dans quelques jours), dossiers de
  l'Event (voir avec Maxster33), grosses sauvegardes datées (tant que les sauvegardes des mondes ne sont pas réglées) ;
  dossier `plugins/KG_BingoGame/icons/26.2-3d/` sur Serveur Jeux (plus lu depuis 0.8.2).

### 2026-09-25 (soir) — LeKiwi06

*(Session du 25/09/2026 après-midi et soir. Fin de session : réservations KG_BingoGame et KalGames libérées.)*

**Déployé et testé (« tout est bon »)**
- KalGames 1.19.0 + KG_ScoreBoards 1.5.0 : plus de limite de parties privées, chaque attribution de points journalisée,
  `/classements verifier [jours]` et `/classements crediter` (points de course non comptés recrédités).
- KalGames 1.19.1 : aux points de contrôle (Parcours et courses), le retour garde l'orientation de la caméra du joueur
  au moment du passage (correctif mis dans KalGames en attendant KG_Parkour).
- KG_BingoGame 0.7.2 → 0.7.8 : préparation des maps débloquée (plus de blocage à 0 %), succès limités à la partie ;
  salle d'attente invincible, nether star bloquée, plaques de pression, entités recopiées (invulnérables, avec leur
  nom), barrières non recopiées ; positions de capture gardées dans `plugins/KG_BingoGame/lobby_capture.yml`
  (`/bingoadmin lobby capture` et `info` marchent aussi depuis la console) ; XP remise à zéro au lancement et en fin de
  partie, convertie en bonus (0,1 point par niveau, solo et équipe, sans cumul ni multiplicateur).
- Salle d'attente du Bingo (monde `bingo_lobby`) : coin 1 `-69 63 -14`, coin 2 `-5 108 67`, apparition
  `-13.49 79 2.77`.

**À faire / à savoir**
- KG_Parkour à créer (cahier des charges dans le dépôt) ; suite de KG_BoatRace (étapes 4, 5, 6).
- Kanvas : cahier `KV_Plots/CAHIER_DES_CHARGES.md` en local chez LeKiwi06 (non poussé), questions restantes, tri des
  plugins, zone Build Battle à sauvegarder.
- Bingo : tester Nether / End et 2 parties simultanées.

---

## Compte rendu de LeKiwi06 retiré de REPRISE_PROJET.md le 28/09/2026

### 2026-09-26 (soir) — LeKiwi06

*(Session du 26/09/2026, de 18 h à 21 h, testée le 27/09/2026. Fin de session : toutes les réservations libérées.)*

**Lobby : ConditionalEvents et PyxelRegions remplacés par KLM_Portal (testé et confirmé par LeKiwi06)**
- **KLM_Portal** (nouveau plugin réseau, `KLM_Portal/JOURNAL.md`) : un portail = une région WorldGuard reliée à une
  destination de KLM_Menu ; désactiver le bouton dans menu > Paramètres désactive le portail (message + recul).
  Interfaces dans la boussole (Interfaces > KLM_Portal) : « Ajouter un portail » et « Points de chute ». 4 portails
  au lobby (`portal_event`, `portal_kal_games`, `portal_kixster`, `portal_kanvas`, monde `Lobby Kalium`).
- Arrivée au lobby : centre `7.5 67 39.5` (reprend l'événement `center_lobby` de ConditionalEvents).
- Effets de zone : speed 6 et jump_boost 2 dans la région WorldGuard `lobby` (reprend `lobby_effects`, sans la
  vision nocturne, non demandée).
- KLM_Menu 2.2.0 / 2.3.0 : API des destinations et attente avant changement de serveur (points de chute).
- ConditionalEvents 4.79.2 et PyxelRegions 1.2.2 rangés dans `_removed-…` (dossiers de données laissés).

**Kal-Games : KG_Parkour 1.1.0 (testé et confirmé par LeKiwi06)** : chrono de 30 s qui s'allonge à chaque checkpoint
(+30 s, +60 s à partir du 7e ; à zéro le joueur tombe au temps), plus de collision entre coureurs, adversaires à
moins de 3 blocs vus sous forme de bottes en cuir colorées.

**À faire plus tard**
- **`relay-token`** à remplir dans `plugins/KLM_Portal/config.yml` du lobby (le `token` de `relay.properties` du
  proxy) pour que les points de chute vers d'autres serveurs soient transmis ; installer KLM_Menu 2.3.0 +
  KLM_Portal 1.2.0 sur un serveur pour qu'il applique les points de chute qu'on lui envoie. (27/09/2026 : KLM_Menu 2.3.0 + KLM_Portal 1.2.0 installés sur Kanvas)
- Sur le lobby, non retirés (pas demandé) : VelocityCommandForward (plus utilisé par les portails), dossiers de
  données `ConditionalEvents/` et `PyxelRegions/`. Avant ConditionalEvents, `/l` et `/lobby` au lobby ramenaient au
  centre : KLM_Menu répond maintenant « Tu es déjà au lobby » (à trancher).
- Visuel des portails désactivés (prévu plus tard par LeKiwi06).
- **KG_Parkour** (cahier des charges) : réglages du chrono par checkpoint et par map (panneau admin), barème,
  contre-la-montre avec fantôme ; bottes et anti-collision à revérifier avec des joueurs Bedrock.
- Repris de la session précédente : tester KG_Bingo 1.5.1 et KG_BingoGame 0.8.2 (bonus du 1er, Nether en Normal,
  3-4 équipes) ; équilibrage des barèmes (`EQUILIBRAGE_POINTS.md`) ; Kanvas (limites d'entités, agrandissement moyen →
  grand, classements, extension du monde) ; sauvegardes des mondes ; dossiers gardés volontairement (plugins retirés de
  Kanvas le 26/09, dossiers de l'Event, grosses sauvegardes, `plugins/KG_BingoGame/icons/26.2-3d/` sur Serveur Jeux).


---

## Compte rendu de LeKiwi06 retiré de REPRISE_PROJET.md le 29/09/2026

### 2026-09-28 — LeKiwi06

*(Session du 27/09/2026 à 9 h 47 au 28/09/2026 à 1 h 30. Fin de session : toutes les réservations libérées.)*

**Build Battle, nouveau mini-jeu (testé et validé par LeKiwi06 le 28/09/2026)** : cahier des charges
`KV_BuildBattle/CAHIER_DES_CHARGES.md`. Accessible seulement depuis kal-games, joué sur Kanvas (créatif + FAWE, mêmes
protections que les plots).
- **KG_BuildBattle 0.1.0** (kal-games, nouveau) : bouton dans KG_Menu ; file publique solo / duo / trio / squad (tempo
  normal 5 min) ; parties privées (tempo 3 / 5 / 10 / 30 min, équipes de 1 à 4, mode « thèmes écrits », code) ; envoi
  vers Kanvas par le relais (clé `buildbattle-<uuid>`). Liaison « comme le Bingo » (le moteur de KalGames n'est pas
  utilisé).
- **KV_BuildBattle 0.3.5** (Kanvas, nouveau) : monde `buildbattle` ; capture d'une boîte (entière + zone constructible
  + apparition) recopiée en 4 colonnes de 8 (une par partie, 600 blocs entre colonnes) ; salle d'attente capturée et
  recopiée par colonne ; partie complète : compte à rebours 30 s dès 2 équipes, 8 équipes max (16 en duo, 32 en
  squad), vote parmi 5 thèmes (liste de 200 validée) ou thèmes écrits (signalement à la poudre de blaze), construction
  chronométrée dans sa boîte, zones figées, vote terrain par terrain aux terracottas (30 s → 3 s), résultats, retour
  sur kal-games, boîtes remises à neuf ; salle en mode aventure, joueurs invincibles, infos en barre d'action ;
  mobs figés et retirables d'un clic ; rien ne pousse hors de la zone ; `randomTickSpeed` à 0.
- `relay-token` rempli à la main par LeKiwi06 dans les config.yml de KG_BuildBattle et KV_BuildBattle.

**Kanvas** : KV_Plots 1.4.1 (`randomTickSpeed` à 0 : la glace ne fond plus) ; KLM_Menu 2.0.0 → 2.3.0 et KLM_Portal 1.2.0
installés : le point de chute du lobby vers Kanvas fonctionne, boussole et étoile de KV_Menu vérifiées (validé).

**À faire plus tard**
- **Build Battle, étape 4** : points et classements (KG_ScoreBoards, barème calé sur `EQUILIBRAGE_POINTS.md`).
- Build Battle : limite du nombre de mobs par zone (non demandée pour l'instant) ; retirer l'emplacement « Build
  Battle » sans moteur de KalGames à la prochaine retouche de KalGames ; si le serveur s'arrête en pleine partie, les
  boîtes ne sont pas remises à neuf (« Générer l'arène »).
- Nettoyage (par l'humain, règle des 3 versions) : sur Kanvas, `_removed-kv_buildbattle-0.1.0/`, `0.2.0/`, `0.3.0/`,
  `0.3.1/`.
- Repris de la session du 26/09 (soir) : sur le lobby, VelocityCommandForward et dossiers `ConditionalEvents/`,
  `PyxelRegions/` non retirés ; `/l` et `/lobby` au lobby (« Tu es déjà au lobby », à trancher) ; visuel des portails
  désactivés ; KG_Parkour (réglages par checkpoint et par map, barème, contre-la-montre avec fantôme, Bedrock) ;
  tester KG_Bingo 1.5.1 et KG_BingoGame 0.8.2 ; équilibrage des barèmes ; Kanvas (limites d'entités, agrandissement
  moyen → grand, classements, extension du monde) ; sauvegardes des mondes ; dossiers gardés volontairement.


---

### 2026-09-25 — Maxster33

**Serveur Event reconverti en survie classique** (demande de Maxster33, faite par son Claude, WinSCP en ligne de
commande) :
- Map : ancien hub `Kal-Games` et vieux dossier `world` rangés dans `/_removed-survie-2026-09-25/` ;
  `server.properties` : `level-name=world` (nouveau monde vierge généré au premier démarrage, seed aléatoire),
  `difficulty=hard` (décisions de Maxster33) ; anciens `server.properties`, `commands.yml` et config KLM_Menu dans ce
  même dossier.
- Plugins retirés (rangés dans `/plugins/_removed-kalgames-event-2026-09-25/`) : KalGames 1.15.1, KG_Bingo 1.2.0,
  KG_ScoreBoards 1.1.0, leurs dossiers de données, tous les anciens `_removed-kalgames/kg_*/kaliummenu`, `claudebak`
  et les `.jar.bak`. Plugins tiers gardés (pas demandé de les retirer).
- KLM_Menu 2.0.0 gardé : boussole désactivée, menu par `/menu` (alias de `/servers` dans `commands.yml`, même
  méthode que Kixster).
- **Nouveau plugin KS_Dimensions 1.0.0** (préfixe `KS_` = serveur Event) : `/dimensions` (opérateurs) ouvre un menu
  pour activer / désactiver les portails du Nether et de l'End ; un portail désactivé bloque l'aller depuis le monde
  normal, jamais le retour (voir son JOURNAL). Compilé, déployé, non testé.
- À faire / à savoir : démarrer Event (l'humain) et tester ; activer la destination `event` dans le menu du lobby ;
  voir « Points ouverts ». Ensuite (même jour) : Floodgate installé (config et clé du proxy), extension
  PlaceholderAPI déplacée dans `expansions/`, PlayerKits2 retiré (`/plugins/_removed-playerkits2-1.23.3/`).
  Restent à vérifier : réglages de hub de ConditionalEvents / PyxelRegions / WorldGuard.
- **Plugins du serveur Event (soir du 25/09/2026)** : cahier des charges de Maxster33 enregistré dans
  `KS_Event/CAHIER_DES_CHARGES.md` (demande, réponses, interprétations) ; 7 plugins autonomes créés, compilés et
  déployés sur Event, non testés : KS_Enclume, KS_Villageois, KS_Crafts, KS_LootBlocs, KS_LootEntites, KS_LootPeche,
  KS_LootPotions (détail dans leurs JOURNAL.md). KS_Elixirs reporté (décision de Maxster33). Limite connue :
  4 blocs de cuivre en carré 2×2 donnent probablement le cuivre taillé vanilla (voir KS_Crafts/JOURNAL.md).

---

### 2026-09-28 — Maxster33

*(Session du 28/09/2026, de 21 h 40 à 23 h 30, Claude de Maxster33. Fin de session : réservations libérées.)*

**Serveur Event : estomac du gardien et loots des coffres de structures** (demande et réponses :
`KS_Event/CAHIER_DES_CHARGES.md`, partie 4). Déployé sur Event le 28/09/2026 à 23:19 (WinSCP en ligne de commande),
**non testé en jeu, redémarrage d'Event à faire par l'humain** :
- **KS_EstomacGardien 1.3.0** (nouveau ; 1.1.0 avait `/estomac`, retirée à la demande de Maxster33 ; 1.3.0 : casque 20 %) : objet « Estomac du gardien » (image du sac noir, pas un vrai sac) ; clic
  droit : consommé, donne son contenu (casque en diamant 20 %, puis 2 tirages : oeuf de tortue, bateau, corail,
  algue, trident enchanté, armure de nautile en diamant, coeur de la mer).
- **KS_LootCoffres 1.0.0** (nouveau) : cité antique, 4 bastions, trésor enfoui, cité de l'End, forteresse,
  avant-poste, manoir, coffres-forts des chambres d'épreuve. Le loot vanilla (tables 26.2 lues dans le jeu) est
  modifié après génération ; changements de poids exacts par remplacement (voir son JOURNAL). Seuls les coffres pas
  encore ouverts sont concernés.
- **KS_KaliumGive 1.0.0** (nouveau, 23:35) : `/kaliumgive <pseudo> <id_custom> <nombre>` (opérateurs) ; liste des
  id_custom dans le code : `estomac_gardien`, `cle_de_l_end` (1.1.0).
- **KS_EC_Extension 1.0.0** (nouveau, 23:47) : coffre de l'Ender à 6 lignes ; les 3 du bas sont bloquées (barrières) et se
  débloquent case par case en déposant une Clé de l'End (consommée). Clé : image de la clé des épreuves sinistre,
  empilable par 64, craft sans forme dans **KS_Crafts 1.1.0** (coeur de la mer, crâne de wither, totem, pomme de Notch,
  capteur sculk calibré, éponge, lingot de netherite, cloche, coeur de grinceur). Couvercle non animé (limite connue).
- **KS_LootEntites 1.1.0** : grand gardien 50 % d'estomac (tué par un joueur ; `softdepend` KS_EstomacGardien).
- **KS_LootPotions 1.1.0** : potion de faiblesse 10 % pour le capitaine pillard seulement.
- Anciens jars dans `/plugins/_removed-ks_lootentites-1.0.0/` et `/plugins/_removed-ks_lootpotions-1.0.0/`.
- À savoir : la session WinSCP enregistrée d'Event s'appelle `Event@7021.mystrator.com` (port 2022) ; le nom
  `kalgames@7021...` ne marche pas en ligne de commande (WinSCP essaie alors le port 22) ; tableau d'architecture corrigé.
- À tester : estomac (clic droit, contenu, inventaire plein), drop du grand gardien, coffres de structures neufs,
  coffres-forts normal et sinistre, capitaine / pillard, `/kaliumgive`, coffre de l'Ender (ouverture, déblocage,
  contenu gardé après reconnexion), craft de la Clé de l'End.
- **Apparence Bedrock (29/09/2026 00:18 ; testée et confirmée par Maxster33 le 29/09/2026 après redémarrage du proxy)** : nouveau dossier `geyser-bedrock/` (voir son README). Geyser
  n'affichait pas l'image (`item_model`) des objets custom : sur Bedrock, l'Estomac du gardien et la Clé de l'End
  étaient des livres. Correspondance Geyser `custom_mappings/kalium_objets.json` et pack `KaLium-objets-1.0.0.mcpack`
  envoyés sur le proxy (dossiers `custom_mappings/` et `packs/` de Geyser-Velocity, vides avant) ; **actif après
  redémarrage du proxy (l'humain)**. Aucun autre plugin n'utilise d'`item_model` (vérifié dans tout le dépôt).
- **Fiole d'expérience remplie (29/09/2026 03:39, testée et confirmée par Maxster33 le 29/09/2026)** : **KS_FioleExp 1.0.0** (nouveau) : à l'enclume, fiole
  vide en 1re case, 2e case vide, nombre de **points** d'XP tapé dans le champ du nom → « Fiole d'expérience (1 395 XP) »
  (exactement ces points retirés, une fiole vide par opération, usure vanilla de l'enclume) ; lancée, elle lâche
  exactement ces points en orbes. **KS_KaliumGive 1.2.0** : id `fiole_exp(<points>)` (ex. `fiole_exp(1395)`) ; 1.1.0
  dans `_removed-ks_kaliumgive-1.1.0/`. Tests OK (remplissage, lancer, /kaliumgive).
- **29/09/2026 03:54, non testés** : **KS_FioleExp 1.1.0** (aucune fiole d'expérience ne peut plus être renommée à
  l'enclume) et **KS_Enclume 1.1.0** (le jeu du joueur affiche « Trop cher ! » dès 40 niveaux : au-delà de 39, 39 est
  affiché, le vrai coût est écrit en barre d'action et le reste est retiré à la prise). À tester : renommer une fiole
  (impossible), réparation / fusion à plus de 40 niveaux (niveaux retirés, refus si pas assez), fiole de plus de 40
  niveaux.
- **29/09/2026 04:17, non testé** : **KS_Enclume 1.1.1**. En 1.1.0, au-delà de 39 niveaux, la case résultat montrait une
  croix rouge (le jeu Java recalcule lui-même le résultat et le vide dès 40 niveaux) : le serveur renvoie maintenant le
  résultat au joueur. Le vrai coût est écrit en dernière ligne de la description du résultat (la barre d'action était
  cachée par l'enclume ; retirée de l'objet à la prise). « + » à la place de 39 : impossible (nombre écrit par le jeu).
  À tester : réparation / fusion et fiole à plus de 40 niveaux (résultat visible, ligne de coût, niveaux retirés, objet
  sans la ligne), prise refusée sans assez de niveaux. Nettoyage possible par l'humain : rien.
- **29/09/2026 04:31, non testé** : **KS_Enclume 1.1.2** : au-delà de 39 niveaux, l'enclume n'affiche plus aucun coût
  (« + » ou « Coût : » seul impossibles : ligne écrite par le jeu) ; vrai coût en dernière ligne de la description du
  résultat ; à la prise, le vrai coût est rendu à l'enclume, qui retire elle-même les niveaux. À tester : prise (bref
  clignotement possible), niveaux retirés, refus sans assez de niveaux, prise ratée (curseur occupé). **Nettoyage
  possible par l'humain** (règle des 3 versions) : sur Event, `/plugins/_removed-ks_enclume-1.0.0/`.
- **Tests du 29/09/2026 (Maxster33)** : KS_Enclume 1.1.2 et KS_FioleExp 1.1.0 **OK en Java**. **Bedrock** : au-delà de 39
  niveaux l'amélioration marche mais le vrai coût n'apparaît pas dans la description ; le remplissage d'une fiole
  (nombre tapé dans le nom) renomme simplement la fiole, coût affiché 1. Cause probable : sur Bedrock, l'enclume calcule
  et affiche son propre résultat (Geyser ne fait que corriger le coût) ; le résultat préparé par le serveur n'est pas
  montré (non corrigeable côté serveur ni par un pack ; la fiole reçue est pourtant la bonne).
- **29/09/2026 05:15, non testé** : **KS_Enclume 1.1.3** : sur Bedrock, la 1.1.2 (coût 0 envoyé) empêchait de prendre le
  résultat. Joueurs Bedrock (Floodgate) : 39 affiché s'ils ont assez de niveaux, sinon le vrai coût (« Trop cher ! ») ;
  Java inchangé. « 40+ » et relever la limite de 40 : impossibles (fixés dans le jeu du joueur). À tester sur Bedrock :
  prise avec assez de niveaux (vrai coût retiré), « Trop cher ! » sans assez. **Nettoyage possible par l'humain** (règle
  des 3 versions) : voir la ligne 1.1.4 ci-dessous.
- **29/09/2026 05:43, non testé** : **KS_Enclume 1.1.4** : Bedrock, au-delà de 39 niveaux, « Coût réel : N niveaux »
  (vert / rouge) ajouté en dernière ligne de l'objet de la 1re case (marqueur invisible), visible au survol et dans
  l'aperçu du résultat ; retiré à la prise, à tout clic / glisser, à la fermeture (déconnexion comprise), et par sécurité
  à la connexion et à l'ouverture d'une enclume. Sur Bedrock, taper le nom après avoir posé les deux objets. À tester
  (Bedrock) : ligne visible, couleur, objet obtenu et objet repris / rendu **sans** la ligne. **Nettoyage possible par
  l'humain** (règle des 3 versions) : sur Event, `/plugins/_removed-ks_enclume-1.0.0/`, `_removed-ks_enclume-1.1.0/`,
  `_removed-ks_enclume-1.1.1/`.
- **29/09/2026 05:58, non testé** : **KS_FioleExp 1.2.0** : sur Bedrock, le nombre tapé dans le champ du nom n'arrive au
  serveur qu'à la prise (coût impossible à afficher avant). Joueurs Bedrock : **accroupi + clic droit sur une enclume
  avec une fiole vide en main** → formulaire (nombre de points), puis confirmation « N points consommera X niveaux
  (niveau A → B) ». Java inchangé. **`telecharger-outils.sh` modifié** (API Floodgate 2.2.5 + Cumulus 1.1.2, pour
  compiler KS_FioleExp) : le relancer sur chaque PC. **Testé et confirmé en Bedrock par Maxster33 le 29/09/2026.**
- **29/09/2026 matin (Maxster33)** : sur Bedrock, l'Estomac du gardien et la Clé de l'End avaient toujours l'apparence
  d'un livre : le proxy n'avait pas été redémarré depuis l'envoi de `geyser-bedrock/`. Après redémarrage : **apparence
  Bedrock testée et confirmée par Maxster33 le 29/09/2026**.
- **29/09/2026 10:24, déployé sur Event, non testé** : **KS_FioleExp 1.3.0** : joueurs Java aussi, accroupi + clic
  droit sur une enclume avec une fiole vide → dialogue natif (Paper) avec les mêmes textes que le formulaire Bedrock
  (points, confirmation du coût en niveaux). Le champ du nom de l'enclume reste utilisable (choix de Maxster33).
  **Actif après redémarrage d'Event (l'humain).** À tester (Java) : ouverture, textes, confirmation (points, fiole
  vide consommée, fiole reçue), erreurs ; Bedrock inchangé. **Nettoyage possible par l'humain** (règle des 3
  versions) : sur Event, `/plugins/_removed-ks_fioleexp-1.0.0/`.
- **29/09/2026 10:55, déployé sur Event à 10:57, non testé** (actif après redémarrage d'Event, l'humain ; 1.3.0 dans
  `_removed-ks_fioleexp-1.3.0/` ; **supprimables par l'humain** (règle des 3 versions) : `_removed-ks_fioleexp-1.0.0/`
  et `_removed-ks_fioleexp-1.1.0/`) : **KS_FioleExp 1.4.0** : « la fabrication de fiole d'exp abîme
  trop l'enclume » : 6 % de chance d'usure par fiole au lieu de 12 % (environ 50 fioles par enclume au lieu de 25),
  réglable dans le nouveau `config.yml` (`usure-enclume-pourcent`, créé au premier démarrage ; 0 = aucune usure).
- **29/09/2026 12:40, déployé à 12:55 (Event) et 12:56 (proxy), non testé** : **Bedrock Breaker** (détail : `KS_BedrockBreaker/JOURNAL.md`).
  Nouveau plugin **KS_BedrockBreaker 1.0.0** : objet « Bedrock Breaker » (image du bâton de blaze, description
  « Utilisation unique », empilable par 64), un bloc de bedrock par objet, sauf la couche du fond ; protections
  WorldGuard respectées (cassage simulé) ; anciens Bedrock Breaker (houe en bois) encore utilisables. **KS_Crafts 1.4.0** :
  8 TNT autour d'une houe en diamant (n'importe laquelle) ; recette débloquée dans le livre dès qu'on obtient une TNT
  ou une houe en diamant. **KS_KaliumGive 1.3.0** : id `bedrock_breaker`. **geyser-bedrock 1.1.0** : apparence Bedrock.
  Anciens jars dans `_removed-ks_crafts-1.3.0/` et `_removed-ks_kaliumgive-1.2.0/` ; sur le proxy, ancien
  `kalium_objets.json` et pack 1.0.0 dans `/plugins/Geyser-Velocity/_removed-kalium-objets-1.0.0/` (hors de `packs/` et
  `custom_mappings/`). **Actif après redémarrage d'Event et du proxy (l'humain).** À tester : craft (8 TNT + houe en
  diamant), recette dans le livre à l'obtention d'une TNT ou d'une houe en diamant, apparence (Java et Bedrock),
  description, pile de 64, bedrock cassée (1 objet consommé), couche du fond refusée, région WorldGuard refusée (objet
  gardé), ancien Bedrock Breaker, `/kaliumgive <pseudo> bedrock_breaker 1`. **Nettoyage possible par l'humain** (règle
  des 3 versions) : sur Event, `_removed-ks_crafts-1.0.0/`, `_removed-ks_crafts-1.1.0/`, `_removed-ks_kaliumgive-1.0.0/`.
- **29/09/2026 13:20, déployé sur Event à 13:30, non testé (actif après redémarrage d'Event)** : **KS_BedrockBreaker 1.0.1** : un bloc par seconde au plus
  par joueur (signalé par Maxster33 : un bloc de bedrock juste derrière était cassé aussi, avec un 2e Bedrock Breaker,
  à cause du clic répété).
- **29/09/2026 13:35, déployé sur Event à 13:59, non testé** : **KS_BedrockBreaker 1.0.2** : la couche du fond peut de nouveau
  être cassée (demande de Maxster33 ; passage vers le vide possible).
- **29/09/2026 13:55, déployé sur Event à 13:59, non testé** : **KS_Crafts 1.5.0** : nouvelle recette du Bedrock Breaker
  (poudre de blaze aux 4 coins, charge de feu en haut, wagonnets à TNT à gauche et à droite, cristal de l'End au
  centre, ancre de réapparition en bas) ; débloquée dans le livre de recettes à l'obtention d'un de ces ingrédients.
- **29/09/2026 14:08, déployé sur Event à 14:18, non testé** : **KS_FioleExp 1.5.0** + **KS_KaliumGive 1.4.0** (à déployer
  ensemble) : fiole remplie en **niveaux** (niveau 50 = points du niveau 0 au niveau 50), à l'enclume et dans le menu
  (Java et Bedrock) ; nom « Fiole d'expérience (niveau 50) », points en description ; confirmation du menu détaillée
  (points de la fiole, points du joueur, coût en points et en niveaux perdus) ; `fiole_exp(N)` en niveaux.
- **29/09/2026 14:10, déployé sur Event à 14:18, non testé** : **KS_LootEntites 1.2.0** : Warden (tué par un joueur) : une
  fiole d'expérience tirée au sort à chaque fois, niveau 10 (poids 15), 15 (10), 20 (6), 30 (3) ou 40 (1) ; l'ancienne
  fiole (10 % d'une fiole de niveau 10 à 50) est retirée. Endermite : la fiole de niveau 10 de son tirage devient la
  nouvelle fiole de KS_FioleExp (points en description) ; à déployer avec KS_FioleExp 1.5.0.
  **Actif après redémarrage d'Event (l'humain).** À tester : fiole à l'enclume et au menu (Java, Bedrock) en niveaux, nom
  et description, confirmation détaillée, `/kaliumgive <pseudo> fiole_exp(50) 1`, fiole du Warden et de l'endermite
  (tués par un joueur). **Nettoyage possible par l'humain** (règle des 3 versions) : sur Event,
  `_removed-ks_fioleexp-1.0.0/`, `1.1.0/`, `1.2.0/`, `_removed-ks_kaliumgive-1.0.0/`, `1.1.0/`.
- **29/09/2026 14:21, déployé sur Event à 14:22, non testé (actif après redémarrage d'Event ; supprimable par l'humain : `_removed-ks_lootentites-1.0.0/`)** : **KS_LootEntites 1.2.1** : Warden, ligne « rien » de poids 315
  dans le tirage : 10 % de chance d'une fiole (niveau 10 : 4,29 %, 15 : 2,86 %, 20 : 1,71 %, 30 : 0,86 %, 40 : 0,29 %).
- **29/09/2026 17:10, déployé sur Event à 17:22 et sur le proxy à 17:23, non testé** : **spawners et Changeur de Biome** (à déployer ensemble ;
  réservations au-delà de 2 plugins avec l'accord de LeKiwi06, selon Maxster33). Nouveaux plugins **KS_ItemSimple 1.0.0**
  (Fragment de Spawner, Cœur de Spawner, 5 têtes « Steve » nommées en attendant un plugin des têtes), **KS_Spawners
  1.0.0** (8 spawners avec leur créature, empilables par 64 ; posés par un joueur, ils tombent quand on les casse, sans
  XP), **KS_BiomeChanger 1.0.0** (Changeur de Biome : menu des biomes de l'overworld, sphère de 32 blocs, historique,
  téléportation pour les opérateurs ; dépend de KLM_Menu) ; **KS_Crafts 1.6.0** (8 spawners : 7 fragments + cœur + tête ;
  Changeur de Biome ; recettes débloquées dans le livre à l'obtention d'un Fragment de Spawner) ; **KS_KaliumGive 1.5.0**
  (16 nouveaux id) ; **KS_LootBlocs 1.1.0** (spawner naturel : 1 fragment + 5 % d'un 2e) ; **geyser-bedrock 1.2.0**
  (apparence Bedrock, proxy). Détail dans chaque `JOURNAL.md`.
  Compléments (demandes de Maxster33, avant déploiement) : Changeur de Biome au choix **sphère** (32 blocs de rayon) ou
  **cube** (52 blocs de côté, même volume) ; **refusé si la zone touche une région WorldGuard** (même celles du joueur) ;
  un spawner fabriqué détruit par une **explosion** tombe au sol. `outils-build` : lancer `sh telecharger-outils.sh` si
  l'API WorldGuard / WorldEdit manque (déjà dans le script, pour KV_Plots).
  **Actifs après redémarrage d'Event et du proxy (l'humain).** À tester : crafts (spawners, Changeur de Biome), livre de
  recettes au 1er fragment, pose / cassage / explosion d'un spawner fabriqué, fragments d'un spawner naturel, menu du
  Changeur de Biome (sphère / cube, biome, région WorldGuard refusée, historique, téléportation opérateur), apparence
  Bedrock, `/kaliumgive`. **Nettoyage possible par l'humain** (règle des 3 versions) : sur Event,
  `_removed-ks_crafts-1.0.0/` à `1.3.0/`, `_removed-ks_kaliumgive-1.0.0/` à `1.2.0/`.
