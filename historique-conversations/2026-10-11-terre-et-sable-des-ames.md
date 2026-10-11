# 2026-10-11 — Terre des âmes et sable des âmes : familles des rachats et butin des coffres

- Plugin(s) concerné(s) : KS_Economy, KG_Rewards, KV_Rewards (`butin.yml` seulement), KS_Teleport, KS_RewardsGUI, KS_Decapitator
- Versions avant / après : KS_Economy 1.5.0 en service (1.5.1 jamais déployée) / 1.5.2 déployée sur Event et
  Kixster ; KG_Rewards 1.3.0 et KV_Rewards 1.3.0 inchangés (`butin.yml` remplacés)

## Demandé

LeKiwi06 : « est-ce que la soul sand et le soul soil sont dans les loot des rewards ? », puis « ajoute la terre des
ames aux terres , et le sables des ames aux sables ». À la question « rachats de la semaine, coffres de récompense ou
les deux ? » : « Les deux ».

## Fait

- Réponse à la première question : non, aucun des deux n'était dans le butin des coffres (le sable des âmes figurait
  seulement dans la liste des objets du Nether du script, la terre des âmes nulle part).
- KS_Economy 1.5.2 : dans `rachats.csv`, la terre des âmes passe dans la famille « terres » et le sable des âmes dans
  la famille « sable » ; la famille « sable des âmes » disparaît (178 familles). Seules ces 2 lignes changent, prix
  inchangés. Détail : `KS_Economy/JOURNAL.md`. Compilé (`sortie/KS_Economy-1.5.2.jar`), poussé. Non déployé.
- Coffres de récompense : les deux blocs ajoutés à la famille « Blocs » du script des pools (hors dépôt), à côté des
  sables et des terres. 4 entrées de plus : 4 x (pool Commun) et 24 x (pool Peu commun) de chacun. `butin.yml` de
  KG_Rewards et de KV_Rewards régénérés et vérifiés (aucune erreur) ; classeur des valeurs régénéré. Non envoyés.

## Décisions

- Choix de Claude signalés : version 1.5.2 plutôt qu'un changement glissé dans la 1.5.1 (une version par
  changement) ; quantités des piles calculées comme pour les autres blocs (prix du barème, 0,04322 émeraude l'unité).
- Les fréquences de quelques niveaux bougent de 1 sur 1 000 (la moyenne des pools change à peine) ; valeurs moyennes
  des niveaux inchangées.

## Déploiement (11/10/2026, 06:05)

- Accord de LeKiwi06 : « oui déploie », puis « je te donne la permission de le faire » (le premier envoi avait
  été refusé par le filtre de permissions de la session de Claude, rien n'était parti).
- KS_Economy 1.5.2 sur Event et Kixster (1.5.0 dans `_removed-ks_economy-1.5.0/`) ; `butin.yml` sur Kal-Games
  et Kanvas (anciens dans `_removed-kg_rewards-butin-2026-10-11/` et `_removed-kv_rewards-butin-2026-10-11/`).
- Les `butin.yml` des serveurs étaient identiques à ceux du 09/10 : rien d'écrasé.
- Actifs après redémarrage par LeKiwi06. Réservations libérées.

## Autre demande de la session

- « as tu des remarques quant au départ de kixster SMP ? » : journaux de Kixster lus en lecture seule (09/10
  19:52 au 11/10 05:30). Aucune erreur de nos plugins, aucun ralentissement. Signalé à LeKiwi06, rien de
  corrigé : nouveaux joueurs qui apparaissent entre Y 118 et Y 123 au-dessus du spawn (2 morts de chute à la
  première connexion) ; erreur de WorldGuard 7.0.19 à la pose d'un bateau ; alertes GrimAC sur les joueurs
  Java ; CoreProtect absent de Kixster ; région `ile_du_dragon` absente de l'End ; `/reward` tapé par des
  joueurs (seul `/rewards` existe).

## Corrections pour Kixster (« oui fais les deux »)

- KS_Teleport 1.1.0 : première connexion au point d'apparition exact du monde (-54 73 212 sur Kixster) au lieu du toit
  du spawn. La proposition parlait du « point de `/spawn` », qui n'existe pas (`/spawn` = maison de chaque joueur) :
  remplacé par le point d'apparition du monde, signalé à LeKiwi06.
- KS_RewardsGUI 1.4.2 : alias `/reward`.
- Compilés, poussés, non déployés ; réservations gardées jusqu'à l'accord de déploiement sur Kixster.
- Autre question : chances d'obtenir un livre Raccommodage sur Kixster (coffres de récompense : 10 % d'un tirage
  Légendaire ; livres des coffres de structures restés vanilla ; ni villageois, ni pêche, ni table d'enchantement).

## /tetes pour tous les joueurs

- Demande de LeKiwi06 : « j'aimerai aussi que les joueurs aient accès au /tetes mais que celui ci leur servent a
  savoir celle qu'ils ont déjà obtenus ( l'avoir eu dans son inventaire via un échange par exemple compte aussi ,
  pas la peine de l'avoir tué soit même ) ».
- KS_Decapitator 1.2.0 : compilé, poussé, non déployé. Détail et choix signalés : `KS_Decapitator/JOURNAL.md`.

## Reste à faire

- Déploiement de KS_Decapitator 1.2.0, sur accord de LeKiwi06 ; test avec un compte non opérateur.
- Déploiement de KS_Teleport 1.1.0 et KS_RewardsGUI 1.4.2 sur Kixster, sur accord de LeKiwi06 ; test avec un compte
  jamais connecté.
- Redémarrage d'Event, Kixster, Kal-Games et Kanvas par LeKiwi06, puis lecture des journaux.
- À tester en jeu : `/rachat` (objets custom montrés par leur vrai objet ; une semaine où « terres » ou
  « sable » sort) ; `/kgrewards test` pour voir sortir les deux blocs dans un coffre.
- Points de Kixster ci-dessus : en attente de la décision de LeKiwi06.
