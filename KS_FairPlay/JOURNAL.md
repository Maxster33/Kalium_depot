# KS_FairPlay - journal

Plugin du serveur Event : fair-play. Cahier des charges : catégorie 5 « FairPlay » de LeKiwi06 (validé le
30/09/2026 ; ajout du gardien ancien le 03/10/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - code Xaero, limite journalière, protections, gardien ancien, Chance III (03/10/2026)

- **Mini-cartes** : à la connexion (2 s après), message « Mini-cartes : vue des grottes et radar d'entités
  désactivés (fair-play). » suivi du code de Xaero (`§f§a§i§r§x§a§e§r§o`, invisible) ; `xaero-fair` dans
  `config.yml`. JourneyMap et Legacy Freecam : leurs plugins serveur (installés à part).
- **Limite globale de 10 par jour** (`limite-par-jour`, remise à zéro à minuit, heure de Paris) :
  - ouvrir (ou casser) un contenant au butin non généré : coffres, coffres piégés, tonneaux, distributeurs,
    coffres des wagonnets de mineshaft (compté quand le butin est généré pour le joueur ; un coffre double = 1) ;
    pots décorés et coffres forts exclus ;
  - casser un spawner naturel (pas ceux posés avec KS_Spawners, marqueur `ks_spawners:pose`) ;
  - brosser un bloc suspect (sable, gravier ; 1 fois par bloc et par jour, même si le brossage est interrompu) ;
  - tuer un **gardien ancien** (pour le tueur) ; limite atteinte : impossible de le frapper (corps à corps, flèche,
    trident, potion, nuage de potion, TNT allumée par le joueur). Gardiens normaux non concernés.
  - Limite atteinte : refus, bloc intact, « Limite atteinte, reviens demain » (au plus toutes les 3 s). Chaque
    ouverture comptée : « n / 10 » dans la barre d'action **et dans le tchat** (demande de LeKiwi06 : « pour ceux qui perdent le fil »).
- **Protection** tant que le butin n'est pas généré (spawner naturel : tant qu'il n'est pas cassé) : incassable sans
  autorisation du jour ; explosions, feu, pistons sans effet ; bloc suspect qui ne tombe pas ; entonnoirs (au-dessus
  ou à côté) qui ne le voient pas ; transferts automatiques refusés ; wagonnet de mineshaft indestructible sans
  autorisation (explosions comprises).
- **Chance III ou plus** (Élixir de Fortune) : sans limite, et rien ne compte (barre d'action « ouverture libre »).
  **Créatif** : ni limite ni compte.
- Données : `plugins/KS_FairPlay/compteurs.yml` (jour, compte par joueur, blocs suspects déjà comptés). `depend`
  KLM_Menu ; `softdepend` KS_Spawners.

Avec : configuration Paper d'Event (anti-xray en mode « cacher », suivi des entités réduit : voir le cahier),
plugins serveur JourneyMap et Legacy Freecam.

**Déployé sur Event le 03/10/2026 à 05:18 (LeKiwi06, nouveau) avec JourneyMap (`journeymap-paper-26.2-6.0.9.jar`, Hangar,
sha256 vérifié) et Legacy Freecam (`legacyfreecam-paper-2.0.0.jar`, Modrinth, sha1 vérifié), téléchargés avec l'accord
de LeKiwi06, et la configuration Paper (anti-xray, suivi des entités ; originaux dans `/_removed-config-2026-10-03/`) ;
actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.0.1 - compteur des coffres (03/10/2026, LeKiwi06)

Signalé par LeKiwi06 : « je ne vois pas le compteur de coffres » (coffres de test avec `container_loot` ;
`compteurs.yml` resté vide alors que le butin était sorti).
- Comptage revu : au clic sur un contenant au butin non généré (clic pas refusé), le plugin vérifie au tick suivant
  que le butin est sorti, puis compte (avant : à l'événement de génération du butin, qui n'a rien compté). Même
  chose pour le coffre d'un wagonnet ; un contenant au butin non généré cassé compte aussi.
- En créatif : « Créatif : non compté (passe en survie pour tester la limite) » dans le tchat et la barre d'action
  (avant : aucun message).

**Déployé sur Event le 03/10/2026 à 05:45 (LeKiwi06 ; 1.0.0 dans `_removed-ks_fairplay-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**
