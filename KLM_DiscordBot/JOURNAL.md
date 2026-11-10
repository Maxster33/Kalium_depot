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

## 0.2.0 — wiki KaLium dans un salon (02/11/2026)

**Demande de LeKiwi06 (02/11/2026)** : « un wiki kalium sous forme de channel discord, un channel principal avec un
sommaire (un message de sommaire par serveur : kixster, kal-games, kanvas), des fiches sous forme de fils dans ce
salon, le sommaire renvoie aux fils ». Choix : commande `k!wiki setup <id du salon>`, fils verrouillés (staff seul),
premier jet des fiches rédigé par Claude, liste des fiches à valider avant le texte.
- `src/wiki-fiches.js` : lecture de `wiki/<NN-serveur>/` (`_sommaire.md` + une fiche par fichier), découpage en
  messages de 2 000 caractères (entre paragraphes, `---` force une coupure), contrôles (titre, fiche vide, nom de fil
  ≤ 100, sommaire ≤ 4 096).
- `src/wiki.js` : `k!wiki setup <id>` / `k!wiki maj` (permission « Gérer le serveur ») ; un fil public par fiche
  (archivage 7 jours, verrouillé), messages « a commencé un fil » effacés, un embed de sommaire par serveur avec les
  liens des fils ; mise à jour sur place (fil rouvert s'il est archivé, renommé, messages modifiés / ajoutés /
  retirés ; tout republié si un message a disparu) ; salon fermé aux joueurs (@everyone : pas de message, pas de fil),
  dérogation au nom du bot ; fils sans fiche signalés, jamais supprimés. État dans `wiki-etat.json` (ignoré par git).
- **Écart au cahier des charges § 9** (validé par la demande `k!`) : intentions `GuildMessages` + `MessageContent`
  (privilégiée, à activer dans le portail) ; le bot ne traite que les messages commençant par `k!` sur le serveur KaLium.
- `API_URL` / `API_TOKEN` facultatifs : sans eux, `/classement` n'est pas enregistré.
- Testé sans Discord : lecture et découpage des fiches (ordre numérique, BOM, CRLF, coupure à 2 000, `---`, erreurs).

**Statut : non testé sur Discord ; fiches pas encore rédigées (liste à valider).**

## 0.3.0 — étape 1 du cahier complet : API par le relais, base locale, /config (09/11/2026)

**Demande de LeKiwi06 (09/11/2026)** : étape 1 du cahier complet (validé le 09/11, gardé en local). Le bot tourne
chez Dishost (Node 20.20) depuis le 09/11.
- `/classement` passe par KaliumRelay 1.11.0 (`RELAIS_URL` / `RELAIS_TOKEN`, jeton `bot-token` du relais) :
  demande déposée dans la boîte `scoreboards`, relevée par KG_ScoreBoards 1.10.0 sur Kanvas, réponse lue dans la
  boîte `bot` (toutes les 0,4 s, 10 s au plus, demande retirée si abandonnée). `API_URL` / `API_TOKEN` restent possibles
  en direct. Autocomplétion : jeux et mois archivés gardés 60 s (Discord n'attend que 3 s), chargés au démarrage.
- Base locale `bot.sqlite` (`better-sqlite3` 12.11.1, dernière version pour Node 20 ; script d'installation autorisé
  dans `package.json`). Si elle ne se charge pas, le bot démarre quand même, sans `/config`.
- `/config salon <type> [salon]` (vide = retirer) et `/config salons` : permission « Gérer le serveur », 13 types du
  cahier (§ 3). Les fonctions de ces salons arrivent aux étapes suivantes.
- Testé en local : transport par le relais contre KaliumRelay 1.11.0 et KG_ScoreBoards 1.10.0 réels (Paper 26.3) ;
  base (choisir, remplacer, retirer). Non testé sur Discord.

**Statut : non déployé.** À déployer avec KaliumRelay 1.11.0 et KG_ScoreBoards 1.10.0 ; `.env` à compléter.

## 0.4.0 — règlement en fils (`k!rules set`) (09/11/2026)

**Demande de LeKiwi06 (09/11/2026)** : le règlement du salon d'informations publié par le bot, « chaque partie du
règlement dans un fil isolé », un lien « Revenir au sommaire » à la fin de chaque fil, commande `k!rules set <channel
id>` réservée au rôle « Le fameux fruit » (deux comptes).
- `src/reglement.js` : présentation + sommaire dans un embed du salon, une partie par fil public verrouillé (embed),
  terminée par `↩️ [Revenir au sommaire](lien du message du sommaire)`. Sommaire publié d'abord (les fils ont besoin
  de son lien), complété avec les liens des fils à la fin. Relancer la commande met tout à jour sur place.
- Textes dans `reglement/` (`_sommaire.md` + une partie par fichier, ordre alphabétique), relus à chaque commande.
- Autorisation : rôle dont le nom contient « le fameux fruit » (majuscules ignorées) ; les autres membres n'ont
  aucune réponse. Salon fermé aux membres comme le wiki (fonctions communes exportées de `src/wiki.js`).
- État dans `reglement-etat.json` (ignoré par git).
- Testé sans Discord : lecture des textes, longueurs (sommaire 1 011 caractères, parties ≤ 754, limite 4 096).

**Statut : non déployé, non testé sur Discord.**

## 0.5.0 — `k!rules edit`, better-sqlite3 retiré (10/11/2026)

**Demandes de LeKiwi06 (10/11/2026)** : « un k!rules edit qui me permet de facilement modifier l'embed » ; « désactive
la partie du code qui cause le problème de démarrage pour le moment ».
- `k!rules edit` (rôle « Le fameux fruit ») : menu déroulant des encadrés (présentation et sommaire, puis chaque
  partie) ; le choix ouvre un formulaire Discord prérempli (titre, couleur, texte, et pour la présentation le texte
  sous le sommaire). À l'envoi : fichier de `reglement/` réécrit, longueurs vérifiées (ancien fichier remis sinon),
  tous les salons où le règlement est publié mis à jour ; un salon supprimé (salon de test) est oublié.
- **Les textes modifiés par `k!rules edit` vivent sur l'hébergement** : récupérer `reglement/` de Dishost avant d'y
  renvoyer celui du dépôt, et le recopier dans le dépôt.
- `better-sqlite3` retiré de `package.json` : chez Dishost, pas de binaire tout prêt et la compilation est tuée faute de
  mémoire (`Killed signal terminated program cc1`), ce qui ajoutait ~3 min à chaque démarrage. `src/db.js` gardé :
  sans le module, le bot démarre sans `/config` (comme avant). À remplacer par une base sans compilation (sql.js).
- Testé hors Discord : menu et formulaires (limites de Discord respectées), aller-retour des fichiers identique,
  couleur / titre vides / ligne « --- » refusés.

**Statut : non déployé, non testé sur Discord.**
