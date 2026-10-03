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

**Non déployé (catégorie 6, étape 1). Statut : non testé en jeu.**

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

**Non déployé (catégorie 6, étape 2). Statut : non testé en jeu.**

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

**Non déployé (catégorie 6, étape 4). Statut : non testé en jeu.**

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

**Non déployé. Statut : non testé en jeu.**
