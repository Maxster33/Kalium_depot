# 2026-10-08 — Joueur Bedrock figé après une suspension : correction au proxy

- Plugin(s) concerné(s) : KaliumRelay (proxy) ; KS_AntiCheat lu seulement, non modifié
- Versions avant / après : KaliumRelay 1.4.0 → 1.5.0

## Demandé
- LeKiwi06 : « l'antitriche a un souci, j'ai suspendu maxster pendant le test, et après avoir levé la suspension il
  ne peut pas bouger ».
- Après le diagnostic et deux corrections proposées : « ça a marché en se reconnectant, fais l'option 1 ».

## Fait
- Diagnostic en lecture seule (journaux de Kixster, du lobby et du proxy, sauvegarde du joueur ; copies hors dépôt) :
  la suspension et la levée de KS_AntiCheat ont fonctionné ; le joueur Bedrock, renvoyé au lobby par Velocity après
  l'expulsion, revenait figé en l'air sur Kixster et se faisait expulser pour « vol » à chaque retour ; même
  enchaînement le 02/10/2026 sur serveur-jeux après un redémarrage, sans KS_AntiCheat.
- KaliumRelay 1.5.0 : un joueur Bedrock que Velocity allait renvoyer vers un autre serveur après une expulsion est
  déconnecté de KaLium avec le message de l'expulsion. Déployé sur le proxy le 08/10/2026 à 20:54 (1.4.0 dans
  `_removed-kaliumrelay-1.4.0/`), actif après redémarrage du proxy.

## Décisions
- Correction au proxy (toutes les expulsions d'un joueur Bedrock) plutôt que dans KS_AntiCheat (suspension seule) :
  choix de LeKiwi06. Conséquence acceptée : un joueur Bedrock est déconnecté de KaLium au lieu d'arriver au lobby
  quand son serveur redémarre ou l'expulse (inactivité, « vol »).
- Réservation : KaliumRelay aurait été le 3e plugin réservé de LeKiwi06 avant 23 h ; LeKiwi06 a choisi de libérer
  KG_Pong.
- Le mécanisme exact côté Geyser n'a pas été trouvé : la correction évite le renvoi au lobby, elle ne répare pas
  Geyser.

## Reste à faire
- Redémarrer le proxy (humain), puis tester : suspendre et lever la suspension d'un joueur Bedrock (écran de
  déconnexion avec le message, puis retour normal) ; vérifier qu'un joueur Java expulsé arrive toujours au lobby.
- Signalé, non corrigé : CoreProtect CE 24.1 ne démarre plus sur Event depuis Paper 26.3.
- Supprimables par l'humain sur le proxy : `_removed-kaliumrelay-1.0.1/`, `1.1.0/`, `1.1.1/`, `1.2.0/`.
