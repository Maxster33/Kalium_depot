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
