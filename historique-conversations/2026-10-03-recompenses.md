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

## Suite : retours sur les boutiques (KS_Economy 1.1.1 → 1.1.2)
- LeKiwi06 : noms anglais et coupés sur les panneaux, recherche « bloc de diamant » / « diamond_block » sans
  résultat, boutiques numérotées, aucun indicateur de rupture, coffres en cuivre refusés ; puis : panneau à retirer
  quand on supprime à distance, fermeture temporaire, délai de 3 h avant de reposer une boutique supprimée.
- Cause de la recherche : `noms_objets.txt` non copié dans le jar par `build.sh`.
- Choix de Claude signalés : délai de 3 h = la place de la boutique supprimée reste prise (les autres places libres
  restent utilisables) ; panneau : nom, lot, prix, état ; panneau rendu au propriétaire.
- Déployé sur Event le 03/10/2026 à 01:49.

## Suite : claims sans bannissement, magasins (KS_Claim 1.1.2, KS_Economy 1.1.3)
- LeKiwi06 : « supprime l'option de bannissement du claim » ; puis : son magasin hors du catalogue, pas d'achat
  chez soi, « voir l'item », signaler une boutique ou un magasin.
- Choix de Claude signalés : « Expulser » gardé ; bannissements existants levés au démarrage ; raisons de
  signalement (arnaque, contenu inapproprié, thème non respecté, Autre) ; staff : `/magasin signalements`
  (se téléporter, supprimer la boutique, classer), permission `kseconomy.staff`.
- Déployés sur Event le 03/10/2026 à 02:35.

## Suite : rubrique « Modération » (KLM_Menu 2.7.0, KS_Economy 1.1.4)
- LeKiwi06 : « il faut que les onglets de modération soit visible depuis le /menu dans le rubrique modération ,
  c'est ici qu'on verra les invsee , ECsee , les proba de minage , les indices de suspicions etc ».
- Fait : bouton « Modération » dans `/menu` (staff), outils déclarés par les plugins ; signalements des magasins
  dedans. Le reste (invsee, ecsee...) viendra avec la catégorie 6. Déployés sur Event le 03/10/2026 à 03:22.
- Correction : `jars-deployes/KLM_Menu-2.5.0.jar` remis (encore en service sur lobby, Serveur Jeux, Kixster).

## Suite : catégorie 5 « FairPlay » (KS_FairPlay 1.0.0)
- LeKiwi06 : « on attaque la catégorie 5 » ; ajouts : gardien ancien dans la limite (impossible de le frapper limite
  atteinte), compteur dans le tchat.
- Vérifié : Legacy Freecam a un plugin serveur Paper 26.2 (règles strictes par défaut) ; JourneyMap 26.2-6.0.9 ;
  code Xaero. Téléchargements acceptés par LeKiwi06.
- Choix de Claude signalés : gardiens normaux non concernés ; suivi des entités 64 / 48 / 48 / 32 / 32 ; anti-xray
  jusqu'à 128 ; `lava-obscures` laissé pour la catégorie 6.
- Déployé sur Event le 03/10/2026 à 05:18.
- Suite : « je ne vois pas le compteur de coffres » (compteurs.yml vide) : KS_FairPlay 1.0.1 (comptage au tick suivant, message en créatif), déployé à 05:52.
