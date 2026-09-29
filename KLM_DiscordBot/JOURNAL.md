# JOURNAL — KLM_DiscordBot

Bot Discord du réseau KaLium (Node.js, externe aux serveurs Minecraft) : classements, statistiques et graphiques
tirés de KG_ScoreBoards. Cahier des charges : `CAHIER_DES_CHARGES.md`. Mode d'emploi : `README.md`.

Version : champ `version` de `package.json` et constante `VERSION` de `src/index.js`.

## 0.1.0 — étape 1 : squelette et /classement (29/09/2026)

**Demande de LeKiwi06 (29/09/2026)** : étape 1 du cahier des charges.
- Node.js 22+ (ESM), dépendances : `discord.js` 14, `chart.js` 4, `@napi-rs/canvas` 1 (binaires précompilés, pas
  de compilation native). Configuration par `.env` (`node --env-file`), modèle vide `.env.example`.
- `src/api.js` : client de l'API de KG_ScoreBoards 1.7.0 ; garde en mémoire la dernière réponse de chaque requête et
  la rend si kal-games est éteint (« données du …, serveur de jeu hors ligne »), sauf pour le journal (`events`,
  jamais de cache : pas d'annonce en double) et sauf jeton refusé (erreur signalée).
- `src/charts.js` : classement en barres horizontales (PNG 900 px de large), fond sombre fixe, couleur fixe par
  jeu (palette catégorielle validée pour fond sombre, choisie par l'identifiant du jeu).
- `/classement <jeu> [période] [tri] [mois]` : top 10 général, du mois ou d'un mois archivé ; tri aux points ou au
  meilleur tour (écart avec le premier affiché). Image + le même classement en texte dans l'embed. Autocomplétion
  des jeux (`/status`) et des mois archivés.
- Commandes enregistrées sur le seul serveur Discord KaLium (`DISCORD_GUILD_ID`) : visibles immédiatement.
- Intention Discord : `Guilds` seulement (aucune intention privilégiée).
- Testé sans Discord contre une fausse API : classement aux tours, jeu inconnu, serveur éteint (cache), serveur
  injoignable ; rendu des images vérifié (`npm run apercu`).
- Limite : sur un hébergement Linux sans polices, les textes des images pourraient mal s'afficher (police à
  embarquer à l'étape 6 si besoin).

**Statut : non déployé (hébergement à choisir), non testé sur Discord.** Nécessite KG_ScoreBoards 1.7.0 sur kal-games.
