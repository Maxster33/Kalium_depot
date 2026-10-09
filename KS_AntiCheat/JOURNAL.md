# KS_AntiCheat - journal

Plugin du serveur Event : anti-triche. Cahier des charges : catégorie 6 « Anti-triche » de LeKiwi06 (validé le
30/09/2026 ; interface dans la rubrique « Modération » de /menu, précision du 03/10/2026 ; copié dans
`CAHIER_DES_CHARGES.md` au déploiement). Codé en 5 étapes (versions 0.x), déployé à la fin.

## 0.1.0 - étape 1 : alertes, interface staff, suspensions, invsee / ecsee, morts d'entités (03/10/2026)

- **Alertes** : historique (`alertes.yml`, 5 000 dernières), console, message au staff connecté (permission
  `ksanticheat.staff`, opérateurs par défaut ; une annonce par minute au plus pour le même joueur et le même type
  d'alerte légère). Légère : le staff vérifie ; grave : suspension automatique (détections des étapes suivantes).
- **Suspensions** (`suspensions.yml`) : connexion à Event refusée avec « Une erreur inhabituelle est survenue,
  contacte le staff. » ; joueur connecté expulsé avec ce message ; annonce au staff ; levée par le staff.
- **Interface staff** : rubrique « Modération » de `/menu` (KLM_Menu 2.7.0) : « Anti-triche », « Invsee »,
  « EcSee » ; aussi `/anticheat [joueur]`, `/invsee <joueur>`, `/ecsee <joueur>`. Anti-triche : alertes récentes,
  joueurs avec alertes, chercher un joueur (état, alertes, inventaire, coffre de l'Ender, suspendre avec une raison /
  lever la suspension), suspendus, morts d'entités importantes, journal invsee / ecsee.
- **invsee / ecsee** : en ligne : vue reliée à l'inventaire du joueur (inventaire, barre, armure, seconde main ;
  changements recopiés dans les deux sens), coffre de l'Ender ouvert tel quel. Hors ligne : état enregistré à sa
  dernière déconnexion (instantané à chaque déconnexion et toutes les 5 minutes, `instantanes/`) ; les changements
  du staff sont appliqués à sa prochaine connexion (`en-attente/`), avant qu'il puisse jouer. Journal
  `consultations.log` : date, staff, invsee / ecsee, joueur (en ligne / hors ligne), ouverture, retirés / ajoutés.
  Limite : un joueur jamais venu depuis l'installation n'a pas d'instantané (message).
- **Morts d'entités importantes** (`morts.log`, 200 dernières dans l'interface) : villageois, golems, allays, boss
  (wither, dragon, gardien ancien, warden), mobs nommés, animaux apprivoisés (`morts.types`, `morts.nommes`,
  `morts.apprivoises`) : date, entité, nom, propriétaire, lieu, tué par (joueur, mob, cause).
- `depend` KLM_Menu ; `softdepend` GrimAC (étape 2).

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 0.2.0 - étape 2 : détections GrimAC, x-ray, macros, AFK (03/10/2026)

- **Infraction grave** : alerte grave + **suspension automatique** (« automatique » comme auteur).
- **GrimAC** (branché à son bus d'événements par réflexion : pas de dépendance à sa version) : une même vérification
  qui signale un joueur `grimac.alerte-signalements` (10) fois en 10 minutes = alerte légère ; vérification
  « lourde » (`grimac.lourdes` : Reach, Hitboxes, FastBreak, FarBreak, MultiBreak, Timer) au niveau de violation
  `grimac.seuil-suspension` (100) = infraction grave. Seuils à ajuster après les tests.
- **X-ray** : seuls les diamants, émeraudes et débris antiques **cachés** (au plus un bloc d'air autour, ni eau ni
  lave) ; un filon compte une fois (voisin à 3 blocs, moins de 30 s) ; taux pour 1 000 blocs de roche minés ; par
  heure : alerte (5 filons et 3 pour 1 000), heure extrême (10 filons et 8 pour 1 000), infraction grave si 3 heures
  extrêmes sur les 6 dernières (`xray.*`). `minage.yml` : 24 dernières heures. Onglet **« Minage (x-ray) »** de la
  rubrique Modération (et dans Anti-triche) : joueurs des dernières 24 h, du plus suspect au moins suspect.
- **Macros** : alerte légère après 20 actions (casser, pêcher, frapper, utiliser un objet) sans bouger ni tourner la
  caméra depuis 5 minutes (`macros.*`).
- **AFK** : expulsion après 60 minutes sans vrai mouvement ni mouvement de caméra (dans l'eau ou un véhicule : ne
  compte pas) ; « Expulsé après 60 minutes d'inactivité. » ; `ksanticheat.afk-libre` : jamais expulsé.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## Étape 3 (duplication) : reportée (03/10/2026)

Décision de LeKiwi06 : l'identifiant caché dans les objets de valeur empêcherait nos recettes et boutiques (KS_Crafts,
KS_Elixir, magasins) de reconnaître les objets marqués ; la détection de duplication est reportée. En attendant : les
réglages anti-duplication de Paper (étape 5).

## 0.4.0 - étape 4 : revente suspecte (03/10/2026)

Alertes légères (seuils `revente.*`) :
- **Récompense jamais récupérée** : en attente sur /rewards depuis 14 jours (vérifié toutes les heures, une alerte
  par semaine au plus) ; **récupérée puis donnée presque aussitôt** : dans les 30 minutes, donnée sans contrepartie.
- **Dons répétés** : 5 dons d'objets de valeur sans contrepartie en 7 jours (puis toutes les 5).
- **Sens unique** : 3 dons du même donneur au même receveur en 7 jours (puis tous les 3).
- Don sans contrepartie : `/echange` où l'un donne un objet de valeur et ne reçoit ni objet de valeur ni au moins
  1 000 points ; ou objet de valeur jeté au sol et ramassé par un autre joueur (dans les 10 minutes).
- Objet de valeur : `revente.objets` (netherite, élytres, étoiles du Nether, balises, totems, pommes dorées
  enchantées, cœur lourd, masse, œuf de dragon, blocs de diamant et d'émeraude, spawners, tridents) ou objet custom
  de nos plugins (marqueur d'un espace `ks_...`, sauf les objets de menu).
- Signaux : `EchangeTermineEvent` (KS_Economy 1.1.5) et `RecompenseRecupereeEvent` + `plusAnciennesEnAttente()`
  (KS_RewardsGUI 1.0.1) ; sans eux (absents ou trop anciens), ces indices ne sont simplement pas suivis.
  `revente.yml` : dons des 7 derniers jours. `softdepend` KS_Economy, KS_RewardsGUI.
- Non suivi : ventes en magasin.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.0.0 - étape 5 : bannissement de KaLium, TNT ; première version à déployer (03/10/2026)

- **« Bannir de KaLium »** (fiche d'un joueur ; raison + confirmation) : POST `/ban` de KaliumRelay 1.4.0, qui lance
  `libertybans ban <pseudo> <raison (staff)>` sur la console du proxy (LibertyBans). Message d'échec si le relais est
  injoignable, `relay-token` vide ou LibertyBans absent. `relay-url`, `relay-token` (**vide dans le dépôt**).
- **Règle `tntExplosionDropDecay`** activée au démarrage dans les mondes d'Event (`tnt-drop-decay`) : fermes à TNT
  environ 4 fois moins efficaces (choix de LeKiwi06 : la TNT sert à casser, pas à récupérer).
- Avec, à installer à part (voir le cahier) : CoreProtect (journaux du monde), LibertyBans (proxy), CauldronInteract,
  Woodcutter (datapack) ; configuration de Paper : `allow-piston-duplication: true` (TNT, tapis, rails ; les autres
  réglages « non supportés » restent désactivés), `lava-obscures: true` (overworld et Nether).
- Versions 0.1.0 à 0.5.0 : étapes de codage, jamais déployées.

**Déployé sur Event le 03/10/2026 à 06:45 (LeKiwi06, nouveau ; `config.yml` posé, `relay-token` à remplir) avec CoreProtect CE 24.1, CauldronInteract 1.4.0, Woodcutter 7.2 (datapack, `world/datapacks/`), téléchargés avec l'accord de LeKiwi06 (sha1 vérifiés), et la configuration Paper (`allow-piston-duplication: true`, `lava-obscures: true` ; originaux dans `/_removed-config-2026-10-03/cat6/`) ; LibertyBans 1.1.4 et KaliumRelay 1.4.0 sur le proxy. Actif après redémarrage du proxy et d'Event. Statut : non testé en jeu.**

## 1.0.1 - invsee / ecsee hors ligne sans attendre une connexion (03/10/2026, LeKiwi06)

Question de LeKiwi06 : « pourquoi je dois attendre que les joueurs se soient connectés pour consulter leur
inventaire ? » (Paper ne donne aucun accès à l'inventaire d'un joueur déconnecté.)
- Sans instantané (joueur pas revenu depuis l'installation), l'inventaire, l'armure, la seconde main et le coffre de
  l'Ender sont lus dans son **fichier de sauvegarde** (`world/players/data/<uuid>.dat`, NBT compressé ; objets
  reconstruits par Paper avec la version des données du fichier), puis gardés comme instantané.
- Lecture seule du fichier : les changements du staff restent appliqués à la prochaine connexion (écrire dans la
  sauvegarde d'un joueur risquerait de l'abîmer). Message si aucune sauvegarde lisible.

**Déployé sur Event le 03/10/2026 à 07:46 (LeKiwi06 ; 1.0.0 dans `_removed-ks_anticheat-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.0.2 - GrimAC Simulation : écarts sous 0,05 ignorés (03/10/2026, LeKiwi06)

Demande de LeKiwi06 (alertes « GrimAC Simulation » sur lui-même, écarts de 0,003 et 0,025 bloc) : « que cette
alerte n'arrive sur grim que si l'erreur de simulation est de 0,05 ou plus ».
- Un signalement de GrimAC dont l'écart (premier nombre de son détail) est sous le minimum de sa vérification n'est
  plus compté (ni alerte, ni suspension) : `grimac.ecart-minimum` (`Simulation: 0.05` ; aussi la valeur par défaut du
  code si la clé manque, comme dans le `config.yml` déjà sur Event). Autres vérifications inchangées.
- **Historique des alertes** (LeKiwi06 : « fait aussi un historique des alertes sur l'interface des joueurs ») :
  bouton « Historique des alertes » sur la fiche d'un joueur dès sa première entrée (avant : « Toutes les alertes »
  au-delà de 8) ; 10 par page, la plus récente d'abord, numéro de page ; en rouge les alertes graves, en bleu les
  actions du staff. Les actions du staff y sont maintenant notées (sans annonce) : suspension (par qui, raison), levée,
  bannissement de KaLium ; elles ne comptent pas dans le nombre d'alertes.
- **Revente : têtes de mobs** (LeKiwi06 : « laisse quand même les têtes des mobs rares, genre l'axolotl bleu, le panda
  brun ») : une tête de KS_Decapitator ne compte comme objet de valeur que si elle est rare (`revente.tetes-rares` :
  axolotl bleu, panda brun, mouton rose, mooshroom brune, lapin Toast, mouton jeb_, creeper chargé, chèvre hurleuse,
  cheval-squelette, cheval-zombie, wither, gardien ancien, warden ; bébés compris). Les autres objets custom
  restent de valeur.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.0 - minage : tous les minerais, compte par minerai (03/10/2026, LeKiwi06)

Retours des tests de Maxster33 (LeKiwi06 : « inclure tous les minerais avec une catégorie par minerai », « le compte
n'a pas l'air fiable », « un score par minerai pas par filon », « compter ceux cassés avec la fortune et la
délicatesse, et même sans enchantement »). Vérifié avec CoreProtect (03/10, 17:13 à 17:27) : roche bien comptée
(810 pour 836), mais 4 « filons » seulement pour 23 diamants, 6 or, 4 redstone et 1 fer cassés. Causes : filon
regroupé sur 30 s (un filon miné en plusieurs fois compté plusieurs fois ou pas), minerai touchant 2 faces d'air
(souvent à cause du tunnel du joueur) exclu, 7 diamants posés par lui comptés.
- **Chaque minerai compte**, par catégorie : charbon, cuivre, fer, or, or du Nether, redstone, lapis, diamant,
  émeraude, quartz, débris antiques ; quel que soit l'outil (Fortune, Délicatesse, sans enchantement) ; comparé à la
  roche minée (pourcentage).
- **Caché** : aucun voisin d'air, d'eau ou de lave d'origine ; les blocs cassés par le joueur lui-même dans les
  15 dernières minutes ne comptent pas comme ouverture (tunnel, filon miné bloc par bloc).
- **Minerais posés par un joueur ignorés** (liste gardée dans `minage.yml`).
- Onglet Minage : par joueur (24 h), roche, puis chaque minerai : total, cachés, pourcentage ; classé par minerais
  rares cachés pour 1 000 blocs de roche.
- Alertes et suspension sur les minerais rares cachés (diamant, émeraude, débris), comptés par minerai : nouveaux
  seuils (nouveaux noms, les anciens du `config.yml` d'Event ne servent plus) `xray.alerte-minerais` 15,
  `alerte-taux-minerais` 12 pour 1 000, `extreme-minerais` 30, `extreme-taux-minerais` 30 ; `extreme-heures` 3,
  `fenetre-heures` 6 inchangés. À ajuster après les tests.
- `minage.yml` : nouveau format (l'ancien, en filons, repart de zéro).
- Comprend aussi les changements de 1.0.2 (jamais déployée).

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.1.1 - correctif : duplication par invsee / ecsee (03/10/2026, LeKiwi06)

Signalé par LeKiwi06 (tests avec Maxster33) : « quand je récupère un item, l'action n'est pas synchronisée sur celle du
joueur dans son ec, donc on peut dupliquer des items si je le prends puis qu'il le prend à son tour ».
- Cause (ecsee) : KS_EC_Extension fait travailler le joueur sur une **copie** de son coffre (pour l'extension), réécrite
  à la fermeture ; l'ecsee modifiait le vrai coffre en même temps. Cause (invsee) : la vue entière était recopiée
  dans l'inventaire du joueur à chaque clic du staff, ce qui pouvait faire réapparaître un objet jeté ou déplacé.
- **Ecsee en ligne** : le coffre du joueur est fermé (copie enregistrée) ; tant que le staff le regarde, le joueur ne
  peut pas l'ouvrir (« Ton coffre de l'Ender est indisponible un instant. ») ; la vue montre les 6 lignes (extension de
  KS_EC_Extension 1.1.0 comprise, cases bloquées non modifiables) ; réécrite à la fermeture (aussi à la déconnexion du
  joueur).
- **Invsee en ligne** : synchronisé case par case à chaque tick ; même case changée des deux côtés : la version du
  joueur gagne, et ce que le staff y avait pris ou posé lui est repris ou rendu (noté dans la console).
- **Une seule vue à la fois** par joueur et par type (inventaire / coffre de l'Ender).
- **Hors ligne** : l'extension est lue dans le fichier de sauvegarde (`BukkitValues`) et dans les instantanés, et
  réappliquée à la connexion avec le reste.
- Double-clic (ramasser tout) désactivé dans les vues. `softdepend` KS_EC_Extension.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.1 ; ancienne version dans `_removed-ks_anticheat-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.0 - tous les joueurs en têtes, claims d'un joueur (09/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faudrait compléter la modération sur kixster, j'aimerai une interface de coffre avec les
têtes de tout les joueurs meme hors ligne, pour ne pas avoir a taper leurs pseudo, j'aimerai que cette option m'ouvre
une interface pour voir leur inventaire, ender chest, leurs liste de claim, etc ».
- **« Joueurs »** : nouvel outil de la rubrique « Modération » de `/menu` (en premier), et bouton « Tous les joueurs »
  de l'accueil de l'anti-triche (`/anticheat`). Coffre de 6 lignes : une tête par joueur connu du serveur (connecté,
  ou qui y a une sauvegarde), 45 par page, les connectés d'abord puis par ordre alphabétique ; flèches de page, porte
  « Retour ». Sous le pseudo (vert : en ligne, blanc : hors ligne, rouge : suspendu) : « En ligne » ou « Hors ligne,
  vu le <date> », nombre d'alertes, « Suspendu ». Rien ne peut être pris ni déposé.
- **Clic sur une tête** : la fiche du joueur (celle de « Chercher un joueur » : inventaire, coffre de l'Ender,
  suspendre / lever, bannir de KaLium, historique des alertes) ; son « Retour » ramène à la même page de la liste.
- **« Claims (n) »**, nouveau bouton de la fiche : claims possédés par le joueur, en ligne ou hors ligne, lus dans
  SimpleClaimSystem : nom et position (« Overworld, x 120 z -40 », centre du chunk), 10 par page. Lecture seule. Sans
  SimpleClaimSystem : pas de bouton. `softdepend` SimpleClaimSystem.
- Pseudo et dernière connexion d'un joueur hors ligne : lus une fois dans sa sauvegarde, puis gardés en mémoire
  jusqu'au redémarrage (mis à jour à chaque déconnexion).

Choix de Claude (à confirmer) : le « etc. » de la demande n'est pas interprété : la fiche garde ce qu'elle proposait
déjà, plus les claims. Les outils « Invsee » et « EcSee » de la rubrique demandent toujours un pseudo.

Limites :
- Tête d'un joueur hors ligne : l'objet ne porte que son identifiant, c'est le jeu du staff qui va chercher
  l'apparence ; un joueur Bedrock garde la tête par défaut, et un membre du staff en Bedrock voit des têtes par défaut
  (le pseudo reste affiché).
- Un joueur jamais venu sur ce serveur (ou pas revenu depuis le changement de monde) n'est pas dans la liste.
- Les claims dont le joueur est seulement membre ne sont pas listés ; pas de téléportation vers un claim.

**Déployé sur Kixster le 09/10/2026 à 18:11 (LeKiwi06 : « Kixster seulement » ; 1.1.1 dans `_removed-ks_anticheat-1.1.1/` ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage de Kixster. Event reste en 1.1.1. Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage de 18:15 (09/10/2026) : 1.2.0 chargée et activée sans erreur, après SimpleClaimSystem ; KLM_Menu trouve les interfaces de KS_AntiCheat. Personne n'avait encore ouvert le menu : la liste en têtes et les claims restent à tester en jeu.**

## 1.3.0 - fiche d'un joueur : économie, maisons, jetons et récompenses, téléportation (09/10/2026, LeKiwi06)

Suite de la 1.2.0 : le « etc. » de la demande, précisé par LeKiwi06 (choix dans une liste, puis « vas-y pour la
suite ») : économie (solde, magasin, dernières ventes et échanges), maisons et téléportation (ses /home, se téléporter à
un de ses claims ou à une maison), jetons et récompenses (inventaire spécial, récompenses en attente, coffres de mort).
Empilé sur la 1.2.0 non testée, à sa demande.

Nouveaux boutons de la fiche d'un joueur (en ligne ou hors ligne), tous en lecture seule :
- **« Économie »** : solde (et s'il est masqué), magasin (nom, position, nombre de boutiques, points en attente dans
  les boutiques), ventes de ses boutiques (nombre, lots des 7 derniers jours, points gagnés), ses 5 dernières ventes,
  ses 5 derniers achats en boutique, ses 5 derniers `/echange`.
- **« Maisons »** : lit (point de réapparition enregistré), maison du spawn, emplacements de localisation : position
  de chacun, et un bouton par destination pour s'y téléporter.
- **« Jetons et récompenses »** : jetons de l'inventaire spécial (un nombre par type), badges portés (niveau),
  récompenses en attente dans `/rewards` (nombre ; pour les 10 plus récentes : date, origine, raison, nombre
  d'éléments), coffres de mort actifs (position, piles d'objets, minutes restantes) avec un bouton par coffre pour s'y
  téléporter.
- **« Claims »** : un bouton par claim pour s'y téléporter (au point enregistré par SimpleClaimSystem pour ce claim).
- **Téléportation du staff** : immédiate, sans jeton ni délai ; chaque téléportation est notée dans la console (qui, où).
- **Journal des échanges** (nécessaire, non demandé tel quel) : KS_Economy ne garde aucune trace des `/echange` ;
  KS_AntiCheat les note maintenant dans `plugins/KS_AntiCheat/echanges.log` (date, les deux joueurs, ce que chacun a
  donné). Les échanges d'avant cette version ne sont donc pas affichés.
- Lecture des autres plugins : `Infos` ; `softdepend` KS_Teleport, KS_CoffreMort, KS_Jetons en plus. Un plugin absent :
  pas de bouton ou pas de ligne ; trop ancien : un message le dit.

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

Limites :
- Récompenses en attente : leur contenu (les objets) n'est pas montré.
- Inventaire spécial : des nombres et des niveaux, pas la vue du contenant.
- Arrivée d'une téléportation non vérifiée (le staff peut arriver dans un bloc ou dans le vide si le lieu a changé) ;
  lit : position enregistrée, même si le lit a été cassé.
- À vérifier en jeu : le point d'arrivée d'un claim (celui que SimpleClaimSystem enregistre à sa création).

**Compilé le 09/10/2026, non déployé. Statut : non testé en jeu.**
