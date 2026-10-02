# 2026-10-03 — Catégorie 4 « Récompenses » et correctif des claims

- Plugin(s) concerné(s) : KaliumRelay (proxy) ; KS_RewardsGUI, KS_KaliumGive, KS_Claim, KS_Economy, KS_BiomeChanger
  (Event) ; KG_ScoreBoards, KG_Rewards (Kal-Games) ; KV_Plots, KV_Rewards (Kanvas) ; KLM_Menu (Event, Kal-Games, Kanvas)
- Versions avant / après : KaliumRelay 1.2.0 → 1.3.0, KS_RewardsGUI → 1.0.0 (nouveau), KS_KaliumGive 1.6.0 → 1.7.0,
  KS_Claim / KS_Economy / KS_BiomeChanger 1.1.0 → 1.1.1, KG_ScoreBoards 1.7.0 → 1.8.0, KG_Rewards → 1.0.0 (nouveau),
  KV_Plots 1.4.1 → 1.5.0, KV_Rewards → 1.0.0 (nouveau), KLM_Menu 2.5.0 → 2.6.0

## Demandé
LeKiwi06 : « oui go la 4 » (catégorie 4 du cahier) ; décisions sur le butin (tables de butin par rareté, fréquences par
pool selon le niveau, récompenses fixes en plus), prestige sur les paliers permanents seulement, périodes séparées ;
Kanvas : tous les bâtisseurs, concours au total des notes avec 3 places, « Finir Kanvas d'abord ». Puis : « tout ce
qui a trait au claim ne marche pas » ; « vas y déploie ».

## Fait
- Étapes 1 à 5 de la catégorie 4 (voir les JOURNAL.md) : boîte aux lettres durable du relais, réception et `/rewards`
  sur Event, classement de la semaine et mois aligné, KG_Rewards, bouton Récompenses de KLM_Menu, KV_Rewards.
- Claims : console d'Event lue (« API not initialized ») ; SimpleClaimSystem 1.13.1 ne crée pas son API : KS_Claim,
  KS_Economy et KS_BiomeChanger l'initialisent (1.1.1).
- Jars en place vérifiés identiques à `jars-deployes/`, puis déployés le 03/10/2026 à 01:05 (anciens jars dans les
  `_removed-…`) ; `config.yml` des trois nouveaux plugins posés avec `relay-token` vide.

## Reste à faire (humain)
- Remplir `relay-token` dans KS_RewardsGUI (Event), KG_Rewards (Kal-Games), KV_Rewards (Kanvas) ; redémarrer le
  proxy, Event, Kal-Games, Kanvas ; remplir les tables de butin (`/kgrewards admin`, `/kvrewards admin`) ; tests au
  speedrun.
- ConditionalEvents (Event) : `config.yml` illisible (signalé, non touché).
