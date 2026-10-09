# 2026-10-09 — Coffre de mort gardé 1 heure, nouveau coffre pour Maaxster, canne à sucre

- Plugin(s) concerné(s) : KS_CoffreMort ; KG_Rewards (fichier de données seulement, aucun code)
- Versions avant / après : KS_CoffreMort 1.0.2 → 1.0.3 (Kixster) ; KG_Rewards 1.3.0 inchangé

## Demandé

LeKiwi06 (23 h) : « on dirait que la canne à sucre [...] elle ne pousse pas sur kixster c'est très bizarre, les autres
cultures n'ont pas de soucis de ce que j'ai testé pourtant ».

LeKiwi06 (23 h 15) : « maxster a ragequit à cause du système de coffre de mort et a tout perdu, on va donc lui
redonner un coffre de récompense comme celui de tout à l'heure du même montant que ce qu'il avait gagné, on va aussi
étendre la longueur des coffres de morts à 1h au lieu de 15 minutes ».

## Fait

- **Canne à sucre** (lecture seule de Kixster : `spigot.yml`, `config/paper-world-defaults.yml`, journal) : aucun
  réglage ni plugin en cause (`cane-modifier: 100`, hauteur maximale 3, aucun de nos plugins n'écoute la pousse,
  WorldGuard `disable-crop-growth: false`). Le journal montre `/gamerule random_tick_speed 1` lancé par LeKiwi06 à
  20:11:30 (3 par défaut) : tout pousse trois fois plus lentement, et la canne n'a aucun stade visible entre deux
  blocs (environ 55 minutes par bloc au lieu de 18). Conseil : `/gamerule random_tick_speed 3`. Réponse de
  LeKiwi06 : « c'est bon merci ».
- **Ce qui est arrivé à Maaxster** (journal de Kixster) : mort à 21:37:24 en X 13510, Z -13466 (36 piles),
  déconnexion à 21:38:25, coffre disparu à 21:52:25.
- **KS_CoffreMort 1.0.3** : `duree-minutes` à 60 par défaut (code et `config.yml` fourni). Envoyé sur Kixster à
  23:25 avec le `config.yml` du serveur passé à 60 ; 1.0.2 et l'ancien `config.yml` dans
  `_removed-ks_coffremort-1.0.2/`. Jar en place vérifié identique à la référence avant l'envoi.
- **Coffre de Maaxster** : `plugins/KG_Rewards/rattrapage.yml` posé sur Kal-Games à 23:26, avec Maaxster seul et le
  réglage de son coffre du rattrapage de 20:21 (valeur moyenne 745,5 émeraudes, 24 piles, mêmes fréquences). Le
  `rattrapage-envoye.yml` du premier envoi est rangé dans `_removed-kg_rewards-rattrapage-2026-10-09/`.

## Décisions

- LeKiwi06 : « Oui, les deux » (envoi sur Kixster et sur Kal-Games) ; Event : « Non, Kixster seulement » (il reste en
  KS_CoffreMort 1.0.1, à 15 minutes).
- Choix de Claude signalés : pas de nouvelle commande dans KG_Rewards, la commande de rattrapage existante est
  réutilisée avec un fichier d'un seul joueur ; le contenu est tiré à nouveau au hasard (même valeur moyenne, pas
  les mêmes objets) ; le motif affiché reste « Rattrapage : 131 récompenses gagnées depuis le début des scores ».

## Reste à faire

- LeKiwi06 : sur Kal-Games, `/kgrewards rattrapage kixster` (relecture), puis `/kgrewards rattrapage kixster confirmer`.
- LeKiwi06 : redémarrer Kixster pour activer la 1.0.3 ; un coffre posé avant le redémarrage garde ses 15 minutes.
- À tester en jeu : message de mort (« il disparaît dans 60 minutes »), temps restant dans `/coffres`.
- Signalé, non traité : 23 erreurs WorldGuard 7.0.19 dans le journal de Kixster depuis 20:09 (`SpawnEntityEvent`,
  NullPointerException) quand un joueur pose un bateau.

## Suite (23 h 30) : récompenses automatiques vers Kixster

- Question de LeKiwi06 : « les futures récompenses de palier etc seront données automatiquement maintenant ? ».
  Réponse : oui, mais `boite: event` était resté dans le `config.yml` de KG_Rewards (Kal-Games) et de KV_Rewards
  (Kanvas) ; quatre récompenses automatiques du 09/10 sont parties vers la boîte d'Event.
- LeKiwi06 : « oui passe les deux en boite kixster ». Fait à 23:32 sur les deux serveurs (fichiers relus à
  l'identique, anciens fichiers dans `_removed-kg_rewards-config-2026-10-09/` et
  `_removed-kv_rewards-config-2026-10-09/`), aucun code modifié.
- Reste à faire par LeKiwi06 : redémarrer Kal-Games et Kanvas (le réglage n'est lu qu'au démarrage) ; lancer la
  commande du coffre de Maaxster (pas encore lancée à 23:32) ; dire s'il veut redonner sur Kixster les quatre
  récompenses parties vers Event.
