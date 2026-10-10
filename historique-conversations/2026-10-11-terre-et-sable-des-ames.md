# 2026-10-11 — Terre des âmes et sable des âmes : familles des rachats et butin des coffres

- Plugin(s) concerné(s) : KS_Economy, KG_Rewards, KV_Rewards (`butin.yml` seulement)
- Versions avant / après : KS_Economy 1.5.1 (non déployée) / 1.5.2 (compilée, non déployée) ; KG_Rewards 1.3.0 et
  KV_Rewards 1.3.0 inchangés

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

## Reste à faire

- Déploiement, sur accord de LeKiwi06 : KS_Economy 1.5.2 sur Event et Kixster ; `butin.yml` sur Kal-Games et Kanvas.
  Avant d'envoyer les `butin.yml`, les comparer à ceux des serveurs : le plugin réécrit le fichier dès qu'une table
  est modifiée dans l'interface admin.
- Réservations gardées jusque-là (KS_Economy, KG_Rewards, KV_Rewards).
- À tester en jeu : `/rachat` (aucune erreur au démarrage ; une semaine où « terres » ou « sable » sort) ;
  `/kgrewards test` pour voir sortir les deux blocs dans un coffre.
