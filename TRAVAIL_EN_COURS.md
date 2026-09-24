# Travail en cours

Plugins réservés en ce moment (voir `REGLES.md`, sections 1 et 5). Un plugin listé ici ne doit pas être
modifié ni déployé par quelqu'un d'autre. Supprimer sa ligne à la fin du travail, même abandonné.

Deux catégories (voir `REGLES.md`, section 1) :
- **Utilisés actuellement** : modifiés en ce moment, personne d'autre n'y touche (seule catégorie comptée dans la
  limite de 2 plugins par personne de 13 h à 23 h).
- **Requis parfois** : utiles ponctuellement au chantier ; les autres ne les modifient qu'avec une demande acceptée.

Format d'une réservation : `- <Plugin> — <pseudo> — depuis le <aaaa-mm-jj hh:mm> — <objet du travail>`

Format d'une demande : `- <Plugin> — demandé par <pseudo> à <pseudo> — créneau le <aaaa-mm-jj> de <hh:mm> à <hh:mm>
— <objet> — réponse : en attente` (la réponse devient « accord (<pseudo ou Claude de pseudo>, <hh:mm>) » ou
« refus (...) » ; le demandeur retire la ligne à la fin du créneau).

## Utilisés actuellement

- KG_BoatRace — LeKiwi06 — depuis le 2026-09-24 13:58 — nouveau plugin : course de bateau sortie de KalGames (Grand Prix, fantôme, barème, temps par checkpoint)
- KG_Bingo — Maxster33 — depuis le 2026-09-24 14:04 — migration kal-games → KalGames2 : `bingo.server-name` = `serveur-jeux` (config sur KalGames2 uniquement)
- KG_BingoGame — Maxster33 — depuis le 2026-09-24 14:04 — migration Kixster → Serveur Jeux : `network.self-server-name` = `serveur-jeux`, `network.kal-games-server-name` = `kal-games` (config sur Serveur Jeux uniquement)

## Requis parfois

- KLM_Menu, KalGames, KG_ScoreBoards, KaliumRelay — Maxster33 — depuis le 2026-09-24 14:04 — migration complète kal-games → KalGames2 et Kixster → Serveur Jeux (copie de tous les fichiers, sans changement de code) ; renommages Velocity (kal-games, serveur-jeux, kixster, event) et destinations du KLM_Menu du lobby. Au-delà de 2 plugins entre 13 h et 23 h : accord de LeKiwi06 à confirmer

## Demandes
