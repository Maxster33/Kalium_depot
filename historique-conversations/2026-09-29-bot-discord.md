# 2026-09-29 — Bot Discord (KLM_DiscordBot), étape 1

- Plugin(s) concerné(s) : KLM_DiscordBot (nouveau), KG_ScoreBoards
- Versions avant / après : KG_ScoreBoards 1.6.0 → 1.7.0 (déployé sur Kal-Games) ; KLM_DiscordBot — → 0.1.0 (non déployé)

## Demandé
« créer un bot discord pour connecter les scoreboard a notre serveur discord, pour y afficher des graphiques et des
stats en fonctions des joueurs, des jeux, des accomplissement (genre plus longue win streak, etc) ».
Puis : libérer KG_PvpKit et KalGames, expliquer la création du jeton du bot, coder l'étape 1, déployer KG_ScoreBoards.

## Fait
- Cahier des charges `KLM_DiscordBot/CAHIER_DES_CHARGES.md` (architecture, API, liaison, commandes, messages
  automatiques, accomplissements, graphiques, sécurité, 6 étapes) avec les réponses de LeKiwi06.
- KG_ScoreBoards 1.7.0 : API HTTP en lecture pour le bot ; déployé sur Kal-Games le 29/09/2026 à 2 h 36.
- KLM_DiscordBot 0.1.0 : bot Node.js, `/classement` en image.
- Explications : création de l'application et du bot Discord, lien d'invitation, fichier `.env`.

## Décisions
- Bot externe en Node.js (plutôt qu'un plugin Java) ; hébergement décidé plus tard.
- Les 7 réponses de LeKiwi06 : voir la section 11 du cahier des charges.
- Proposition « bot en plugin KG_Discord » refusée au profit du bot externe.

## Reste à faire
- Clés `api` dans le `config.yml` de KG_ScoreBoards sur kal-games, redémarrage ; remplir `.env` ; tester `/classement`.
- Étapes 2 à 6 du cahier des charges.
