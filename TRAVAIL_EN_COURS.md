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

- KG_BingoGame — LeKiwi06 — depuis le 2026-09-24 11:36 — alléger la génération des maps et la salle d'attente (plusieurs parties simultanées sans lag) ; serveur Kixster renommé « bingo » dans Velocity : `network.self-server-name` à mettre à jour (config du serveur et code)
- KG_Bingo — LeKiwi06 — depuis le 2026-09-24 12:23 — serveur Kixster renommé « bingo » : `bingo.server-name` à mettre à jour (config sur kal-games et code)
- KLM_Menu — Maxster33 — depuis le 2026-09-24 12:10 — déploiement (2.0.0) sur KalGames2, Serveur Jeux et Kal-Test-Dev ; nouvelles destinations dans la config du lobby (aucun changement de code)
- Serveurs (hors plugins) — Maxster33 — depuis le 2026-09-24 12:10 — raccordement de KalGames2, Serveur Jeux et Kal-Test-Dev au proxy : velocity.toml, paper-global.yml, server.properties, plugins communs du lobby (Floodgate, LuckPerms, voicechat...)

## Requis parfois

## Demandes
