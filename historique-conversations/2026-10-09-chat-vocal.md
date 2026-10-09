# 2026-10-09 — Simple Voice Chat réglé sur tous les serveurs

- Plugin(s) concerné(s) : aucun des nôtres ; plugin externe voicechat (Simple Voice Chat) 2.6.24, inchangé
- Versions avant / après : sans objet (réglages seulement)

## Demandé

LeKiwi06 : « il faudrait configurer simple voice chat dans tout les serveurs ».

## Fait

- Lecture seule des 7 serveurs : voicechat 2.6.24 sur les 6 serveurs Paper, rien sur le proxy ; `voice_host` vide
  partout ; ports déjà bons sur lobby, Kal-Games, Kanvas et Serveur Jeux, restés à 24454 (défaut) sur Kixster et Event.
- Cause du chat vocal muet : les joueurs entrent par le proxy et chaque serveur a sa propre IP ; sans `voice_host`,
  le client cherche la voix sur l'IP du proxy.
- Envoyé à 18:32 (serveurs allumés) dans `plugins/voicechat/voicechat-server.properties` : `voice_host=<IP>:<port>`
  sur les 6 serveurs, `port=40046` sur Kixster et `port=40002` sur Event. Anciens fichiers dans
  `/plugins/_removed-voicechat-config-2026-10-09/`. Valeurs : `REPRISE_PROJET.md`, « Points ouverts ».
- `REGLES.md` : règle 4.6 (Kal-Test-Dev) supprimée.

## Décisions

- LeKiwi06 : « voice_host par serveur » plutôt que le plugin voicechat sur le proxy (seule version pour Velocity :
  alpha 2.6.18 du 28/05, jamais essayée avec Velocity 4.2.0 ; elle aurait aussi demandé un redémarrage du proxy).
- LeKiwi06 : « Oui, Kanvas aussi », puis « la règle pour kal-test-dev est obsolète, il n'est plus un serveur de
  testes depuis longtemps, tu peux donc supprimer la règle ».
- Choix de Claude signalé : IP de jeu de chaque serveur (celle de `velocity.toml`) plutôt qu'un nom d'hôte.

## Suite : journaux après redémarrage

- « j'ai redémarré les serveurs, tu peux lire les journaux » (LeKiwi06) : redémarrages de 18:36 - 18:37. Sur les 6
  serveurs, voicechat démarre sur le bon port avec le bon `voice_host`, aucune erreur, réglages non réécrits.
- Lobby : LeKiwi06 et Maaxster connectés au chat vocal à 18:37. Les 5 autres : personne n'était encore entré.

## Suite : « on n'a pas la permission de parler »

- Cause : le `plugin.yml` de voicechat déclare `voicechat.speak`, `voicechat.listen` et `voicechat.groups` sans
  valeur par défaut (« opérateurs seulement » pour Bukkit) ; LuckPerms les refuse donc aux non-opérateurs.
- Correction faite par LeKiwi06 en jeu, sur les 6 serveurs (18:55 à 19:02) : `/lp group default permission set
  voicechat.speak true`, puis `voicechat.listen` et `voicechat.groups`. Proposition non retenue : un `permissions.yml`
  à la racine de chaque serveur (redémarrage nécessaire, jamais essayé avec LuckPerms).
- LeKiwi06 : « c'est bon ». Journaux relus à 19:08 : permissions enregistrées et au moins une connexion vocale
  réussie sur chacun des 6 serveurs, aucune erreur.

## Reste à faire

- Rien. À savoir : les trois permissions sont à redonner sur tout nouveau serveur (une base LuckPerms par serveur).
