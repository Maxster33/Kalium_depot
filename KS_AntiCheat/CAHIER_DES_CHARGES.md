# Cahier des charges - Catégorie 6 : Anti-triche (KS_AntiCheat + GrimAC) - LeKiwi06

Publié au déploiement (03/10/2026).

Serveur : **Event** (bêta ; destination future : Kixster SMP ; portée réseau à décider). État : **cahier validé par LeKiwi06
le 30/09/2026** (session 6).

# 1. Demande d'origine

## Demande du 29/09/2026 (fait foi)

> KS_AntiCheat :
>
> plugin d'interface et communiquant avec GrimAC pour la structure principale et paper global .yml pour gérer :
>  invsee
> EcSee
>  les logs du monde  ( blocks , kill d'entités importantes , etc )
> anti clear lava
> anti x-ray
> anti-macro
> anti-duplication
> détection d'afk prolongés
> détection d'utilisation suspectes (récompenses jamais utilisé/ouverte ou vendu au market par exemple ce qui indiquerai de la revente parralèle )

> ajouter les plugin : woodCutter , CauldronInteract , SimpleVoiceChat , Tradeshop , SimpleClaimSystem , GrimAC

## Architecture cible (`ARCHITECTURE_CIBLE.md`, section Modération, décisions de LeKiwi06 du 24/09/2026)

- `KG_AntiCheat` (préfixe `KLM_` s'il couvre tout le réseau) : **indices de suspicion, jamais de sanction automatique
  sans décision humaine** ; gains de points anormaux ; proportion de minerais rares anormale ; statistiques des parties
  (KG_ScoreBoards) ; invsee ; **liste noire et détection des comptes secondaires** ; suspicion de revente contre de
  l'argent réel (récompenses jamais ouvertes ni vendues, objets de valeur donnés sans contrepartie, flux à sens unique
  répétés entre mêmes comptes).
- **GrimAC** fait le travail « dur » en temps réel (mouvements, combat, triches client) ; notre anti-triche ne le refait
  pas et analyse les événements précis dans la durée.
- **Traçabilité des objets de valeur** : identifiant unique caché dans chaque objet de valeur généré par le serveur,
  passages de main en main journalisés ; deux objets au même identifiant = duplication détectée ; limite : objets
  d'identifiants différents non empilables (marquage réservé aux objets de valeur, ou par lot).

## Existant (30/09/2026)

- **GrimAC** déjà installé sur Event (et kal-games) ; API : événement `FlagEvent` (joueur, vérification déclenchée),
  vérifications dont `AutoclickerA`.
- **SimpleVoiceChat** (`voicechat`) déjà installé sur Event et Kanvas.
- Catégorie 5 : anti-xray de Paper en mode « cacher » (minerais, coffres, tonneaux, spawners) déjà décidé.
- Catégories 2 et 4 : `/echange`, magasins, journal des récupérations de `/rewards` (données pour la revente).

> **Précision de LeKiwi06 (03/10/2026)** : « il faut que les onglets de modération soit visible depuis le /menu dans le rubrique modération , c'est ici qu'on verra les invsee , ECsee , les proba de minage , les indices de suspicions etc ». Fait dans KLM_Menu 2.7.0 (rubrique « Modération » de `/menu`, outils déclarés par chaque plugin avec sa permission) ; premier outil : signalements des magasins (KS_Economy 1.1.4). KS_AntiCheat y déclarera invsee, ecsee, proportions de minerais, indices de suspicion.

# 2. Réponses de LeKiwi06

| Question | Réponse (30/09/2026) |
|---|---|
| Portée | KS_AntiCheat propre à Event pour l'instant ; ce qui couvre le réseau (statistiques des mini-jeux, liste noire, comptes secondaires) plus tard dans un `KLM_AntiCheat` |
| Sanctions | Pas de sanction automatique, mais on peut **rendre le serveur inaccessible aux joueurs suspects** le temps que le staff statue |
| Interface staff | `/anticheat` + ~~bouton dans les Paramètres de KLM_Menu~~ **rubrique « Modération » de `/menu` (LeKiwi06, 03/10/2026)** ; joueurs, alertes, historique, outils : validé |
| Alertes | En jeu au staff (permission) + historique ; Discord plus tard : validé |
| invsee / ecsee | Voir, **retirer et ajouter** (ex. vérifier une shulker) ; **joueurs hors ligne** aussi (ex. joueur suspendu) ; **journal** de chaque consultation et modification (éviter les abus, pouvoir se dédouaner) |
| Journaux du monde | **CoreProtect** installé ; KS_AntiCheat ajoute les morts d'entités importantes : d'accord |
| Clear lava | Surtout empêcher le **minage de la netherite via les lacs de lave du Nether** |
| X-ray | Anti-xray de Paper + détection de proportion anormale de minerais rares : validé |
| Macros | **Toute action exécutée sans intervention du joueur** (ex. ferme à bois, ferme à raid, pêche automatique) |
| Duplication | Traçabilité par identifiant unique des objets de valeur : validé ; **voir aussi si on peut régler les bugs de duplication directement dans Paper** |
| AFK prolongé | **Expulsion au bout d'une heure** ; le but est de limiter l'AFK, pas de l'interdire |
| Revente suspecte | Les 3 indices proposés, seuils réglables : validé |
| Configuration de Paper | Réglée à la main ; l'interface ne la modifie pas : validé |
| Suspension | **Automatique**, réservée aux **infractions graves** (limiter les dégâts) ; joueur clean : suspension levée ; sinon : **banni de tout KaLium**. Infraction légère : simple alerte, le staff vérifie lui-même. Message au joueur suspendu : « Une erreur inhabituelle est survenue, contacte le staff. » (évite qu'il efface des preuves, il vient directement au staff) |
| Macros | Alerte seulement quand des actions se répètent sans mouvement du joueur ni de sa caméra depuis X minutes : d'accord |
| Duplication dans Paper | Tout désactiver **sauf le duplicateur de TNT** (à la place : baisser le taux de drop des TNT pour limiter l'efficacité des fermes, sans les rendre impossibles) |
| Infractions graves | Duplication détectée, alerte GrimAC de triche lourde au-delà d'un seuil, proportion de minerais rares extrême : validé, **attention à ne pas suspendre ceux qui minent simplement en grotte** |
| Bannissement réseau | Aucun outil aujourd'hui (aucun joueur banni jusqu'ici) |
| Duplication par pistons | **Laisser les 3 activées** (TNT, tapis, rails) |
| Fermes à TNT | Oui, les blocs cassés par la TNT ; **diviser par 10** l'efficacité des fermes concernées |
| Installations seules | WoodCutter (ou datapack) et CauldronInteract cherchés pour Paper 26.2 au moment du code ; SimpleVoiceChat déjà installé : d'accord |
| X-ray : ne compter que les minerais cachés, suspension seulement si extrême sur plusieurs heures | D'accord |
| Fermes à TNT | Règle vanilla **`tntExplosionDropDecay`** (environ divisé par 4) pour le moment, à ajuster après les tests ; **pas de plugin**. Les TNT non dupliquées sont aussi touchées : **choix de game design volontaire** (la TNT sert à casser, pas à récupérer) |
| Bannissement réseau | Installer **LibertyBans** sur le proxy ; bouton « Bannir de KaLium » dans KS_AntiCheat : d'accord |

# 3. Questions restantes

Aucune.

# 4. Choix d'interprétation (validés le 30/09/2026)

## Éléments

| Élément | Rôle |
|---|---|
| **KS_AntiCheat** (nouveau, Event) | Interface staff, alertes, suspension automatique, invsee / ecsee, morts d'entités importantes, détections (x-ray, macros, AFK, duplication, revente), traçabilité des objets de valeur |
| **GrimAC** (déjà installé) | Triches « dures » en temps réel ; ses alertes (`FlagEvent`) sont reprises par KS_AntiCheat |
| **CoreProtect** (installé) | Journaux des blocs, des coffres, retours en arrière |
| **LibertyBans** (installé sur le proxy) | Bannissement de tout KaLium |
| **Paper** (configuration d'Event, à la main) | Anti-xray (catégorie 5) + **minerais cachés derrière la lave** (`lava-obscures`), débris antiques compris ; réglages « non supportés » de duplication : **TNT, tapis, rails autorisés**, le reste désactivé |
| **Règle du monde** (Event : overworld, Nether, End) | `tntExplosionDropDecay` activée |
| Plus tard : `KLM_AntiCheat` | Réseau : statistiques des mini-jeux, liste noire, comptes secondaires |

## Interface staff et alertes

- `/anticheat` et rubrique « Modération » de `/menu` (KLM_Menu 2.7.0, permission staff) : liste des joueurs avec leurs alertes
  (GrimAC + nos détections), historique par joueur, accès aux outils.
- Alertes en jeu aux membres du staff (permission), gardées dans un historique. Discord plus tard (bot).

## Sanctions

- **Infraction légère** (macro, AFK, revente suspecte, x-ray modéré) : **alerte seulement**, le staff vérifie.
- **Infraction grave** -> **suspension automatique** : duplication détectée (deux objets de valeur au même
  identifiant) ; alerte GrimAC de triche lourde au-delà d'un seuil réglable ; proportion de minerais rares
  **cachés** extrême **sur plusieurs heures**.
- **Suspendu** : ne peut plus se connecter à Event ; message « Une erreur inhabituelle est survenue, contacte le
  staff. » ; le staff lève la suspension (joueur clean) ou **bannit de tout KaLium** (bouton qui lance LibertyBans).

## Outils

- **invsee / ecsee** : inventaire et coffre de l'Ender d'un joueur, **en ligne ou hors ligne** (ex. suspendu) ; voir,
  **retirer, ajouter** (ex. ouvrir une shulker). **Journal** de chaque consultation et modification (qui, quel
  joueur, quoi, quand), pour éviter les abus et pouvoir se dédouaner.
- **Journaux du monde** : CoreProtect ; KS_AntiCheat journalise en plus les **morts d'entités importantes**
  (villageois, animaux apprivoisés, mobs nommés, golems, boss ; liste réglable).

## Détections

- **X-ray** : ne comptent que les minerais rares (diamant, émeraude, débris antiques...) **cachés** au moment où le
  joueur les atteint (ne touchant ni air, ni eau, ni lave) ; un mineur de grotte n'est pas compté. Alerte au-delà d'un
  seuil par heure ; suspension seulement si le ratio reste extrême sur plusieurs heures (seuils réglables).
- **Clear lava** : parade par l'anti-xray (minerais et débris antiques derrière la lave cachés) ; le pack de
  ressources lui-même est indétectable.
- **Macros** : alerte si des actions se répètent (casser, pêcher, frapper, utiliser un objet) alors que le joueur ne
  bouge ni lui ni sa caméra depuis X minutes (réglable) ; alerte seulement.
- **AFK prolongé** : **expulsion au bout d'une heure** sans vrai mouvement ni mouvement de caméra (un courant d'eau
  ou un wagonnet ne compte pas comme mouvement).
- **Duplication** : identifiant unique caché dans chaque **objet de valeur** (liste réglable : blocs compressés,
  élixirs, spawners, objets de `/rewards`, netherite...) ; deux objets au même identifiant = infraction grave.
  Limite : deux objets d'identifiants différents ne s'empilent plus entre eux.
- **Revente suspecte** (seuils réglables) : récompense jamais récupérée, ou récupérée puis donnée / vendue presque
  aussitôt ; objets de valeur souvent donnés (`/echange`, jetés au sol) sans contrepartie ; échanges à sens unique
  répétés entre les mêmes comptes.

## Installations seules

- **WoodCutter** (tailleur de pierre qui coupe le bois ; datapack si plus simple) et **CauldronInteract**
  (distributeur + seau / chaudron) : versions pour Paper 26.2 cherchées au moment du code. **SimpleVoiceChat** :
  déjà installé sur Event.

# 5. Au moment du code (03/10/2026, KS_AntiCheat 1.0.0)

- **Duplication (identifiant caché)** : **reportée** (LeKiwi06, 03/10/2026) : le marqueur empêcherait nos recettes et
  boutiques (KS_Crafts, KS_Elixir, magasins) de reconnaître les objets marqués. En attendant : réglages de Paper.
- **Vérifié** : CoreProtect et LibertyBans n'étaient **pas installés** (le cahier les croyait en place). Versions
  trouvées : CoreProtect CE 24.1 (Paper 26.2), LibertyBans 1.1.4 (Velocity), CauldronInteract 1.4.0 (Paper 26.2),
  Woodcutter 7.2 (datapack, 26.2).
- **Bannir de KaLium** : par KaliumRelay 1.4.0 (`POST /ban`, jeton) plutôt que VelocityCommandForward (il faudrait
  un plugin de plus sur Event et il ouvrirait toutes les commandes du proxy aux serveurs).
- **Paper** : les réglages « non supportés » de duplication étaient tous désactivés ; seul `allow-piston-duplication`
  passe à `true` (il couvre TNT, tapis et rails) ; `lava-obscures: true` (overworld et Nether).
- **Choix de Claude (seuils réglables, à ajuster après les tests)** : GrimAC : alerte à 10 signalements d'une même
  vérification en 10 min, suspension si Reach, Hitboxes, FastBreak, FarBreak, MultiBreak ou Timer atteint le niveau
  100 ; x-ray : alerte à 5 filons cachés et 3 pour 1 000 blocs de roche dans l'heure, heure extrême à 10 filons et
  8 pour 1 000, suspension à 3 heures extrêmes sur 6 ; macros : 20 actions sans bouger ni tourner la caméra depuis
  5 min ; revente : 5 dons en 7 jours, 3 au même joueur, récompense en attente 14 jours, donnée dans les 30 min ;
  contrepartie : objet de valeur ou 1 000 points. invsee / ecsee hors ligne : état à la dernière déconnexion,
  changements appliqués à la reconnexion.
