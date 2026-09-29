# KLM_DiscordBot — cahier des charges (demande de LeKiwi06, 29/09/2026)

Brouillon, à valider point par point avec LeKiwi06. À relire avant chaque étape.

**Demande** : « créer un bot Discord pour connecter les scoreboards à notre serveur Discord, pour y afficher des
graphiques et des stats en fonction des joueurs, des jeux, des accomplissements (genre plus longue win streak, etc.) ».

**Décisions de LeKiwi06 (29/09/2026)** :
- **Bot externe** (pas un plugin Minecraft), écrit en **Node.js** ; hébergement décidé plus tard, avant le déploiement.
- Affichage : **commandes slash**, **salon mis à jour automatiquement**, **annonces**, **récap périodique**.
- Contenu de la première version : **profil joueur**, **classements par jeu**, **accomplissements**, **activité du serveur**.
- **Liaison des comptes** Discord ↔ Minecraft **avec un code**.
- On commence par ce cahier des charges, puis on code étape par étape.

**Réponses de LeKiwi06 aux questions ouvertes (29/09/2026)** : voir § 11.

Nom du dossier : `KLM_DiscordBot` (préfixe réseau : le bot pourra plus tard couvrir d'autres serveurs que
kal-games), validé par LeKiwi06.

## 1. Architecture

```
 Discord  <──(passerelle Discord)──>  Bot Node.js  ──(HTTPS/HTTP + jeton)──>  KG_ScoreBoards (kal-games)
                                         │                                        │
                                   bot.sqlite                         stats.yml, archives/, journal/*.jsonl
 (comptes liés, curseur des annonces,                                 (données déjà existantes)
  messages à rafraîchir, historique)
```

- **Seul interlocuteur côté Minecraft : KG_ScoreBoards** (`ARCHITECTURE_CIBLE.md`). Les jeux ne parlent jamais au bot.
- **Le bot interroge, le serveur répond** : kal-games n'a jamais besoin de joindre le bot. Le bot peut donc tourner
  n'importe où (Minestrator, PC, VPS) sans rien changer côté serveur.
- **Kal-games éteint** : le bot reste en ligne, répond avec les dernières données en cache (en le signalant :
  « données du 29/09 à 21 h 40, serveur hors ligne ») ; les annonces reprennent au redémarrage sans rien perdre
  (curseur, voir § 3).

### Bibliothèques envisagées (bot)

| Besoin | Choix | Remarque |
|---|---|---|
| Discord | `discord.js` v14 | commandes slash, embeds, boutons, envoi d'images |
| Graphiques | `chart.js` + `chartjs-node-canvas` (rendu en PNG) | dépend d'un module natif (`canvas`) : à vérifier sur l'hébergeur retenu ; repli : `@napi-rs/canvas` (binaires précompilés) |
| Base locale | `node:sqlite` (intégré à Node 22+) ou `better-sqlite3` | un seul fichier `bot.sqlite` |
| Tâches planifiées | minuteries du bot (pas de cron système) | récaps, rafraîchissements |

Node.js **22 LTS** minimum.

## 2. API de KG_ScoreBoards (nouvelle, lecture seule sauf la liaison)

Petit serveur HTTP intégré à KG_ScoreBoards (`com.sun.net.httpserver`, déjà dans Java : aucune dépendance),
exécuté hors du fil principal du serveur Minecraft.

- **Port** : un port supplémentaire ouvert sur kal-games (à demander dans le panneau Minestrator, comme le 46199
  du relais). Clé `api.port` (0 = API désactivée, valeur par défaut).
- **Jeton** : clé `api.token` du `config.yml` **sur le serveur uniquement**, vide par défaut (`REGLES.md` § 2.4) ;
  API refusée tant que le jeton est vide. Le bot l'envoie dans l'en-tête `Authorization: Bearer …`. Jamais
  affiché dans la console.
- **Lecture des fichiers** : l'API lit les données en mémoire de `StatsService` et les fichiers du journal ; elle
  n'écrit rien dans `stats.yml`.

| Route | Rôle |
|---|---|
| `GET /api/v1/status` | version, heure du serveur, joueurs en ligne, jeux connus (identifiant, nom, type POINTS / TIME / LAP) |
| `GET /api/v1/rankings/<jeu>?period=general\|month&limit=10` | classement d'un jeu |
| `GET /api/v1/archives` · `/api/v1/archives/<aaaa-mm>/<jeu>` | mois archivés, classement complet d'un mois |
| `GET /api/v1/players/<uuid ou pseudo>` | points et meilleurs temps par jeu (général et mois), rang dans chaque jeu |
| `GET /api/v1/events?after=<curseur>&limit=500` | événements du journal (toutes les lignes JSON, tous jeux) après un curseur ; renvoie le curseur suivant |
| `POST /api/v1/link/verify` | vérifie un code de liaison (§ 4) et renvoie l'UUID et le pseudo du joueur |

**Curseur des événements** : `<aaaa-mm>/<jeu>/<numéro de ligne>` (le journal est en ajout seul, un fichier par mois
et par jeu), encodé en une chaîne opaque pour le bot.

Les statistiques calculées (accomplissements, séries, activité) sont faites **par le bot** à partir des événements :
KG_ScoreBoards reste un stockage et une porte d'accès, sans logique propre à Discord.

## 3. Données et base locale du bot

Le bot récupère **tout le journal** au premier démarrage, puis les nouveaux événements toutes les **30 s**
(réglable), et les range dans `bot.sqlite` :

- `events` : copie des lignes du journal (jeu, type, date, match, champs JSON) ; source de tous les calculs ;
- `links` : compte Discord ↔ UUID Minecraft ;
- `messages` : messages du salon auto-rafraîchi (identifiants Discord) ;
- `records` : records et accomplissements actuels, pour détecter qu'un record vient d'être battu (annonces) ;
- `snapshots` : points de chaque joueur par jour (courbes d'évolution ; le journal seul suffit à les reconstruire).

`bot.sqlite` n'est jamais dans le dépôt (`.gitignore`).

## 4. Liaison des comptes

1. En jeu, sur kal-games : **`/lier`** → KG_ScoreBoards donne un code à 6 caractères, valable **10 minutes**,
   usage unique (« Tape /lier ABC123 sur le Discord KaLium »).
2. Sur Discord : **`/lier ABC123`** → le bot appelle `POST /api/v1/link/verify` et enregistre la liaison.
3. **`/delier`** (Discord) retire la liaison. Un compte Minecraft = un compte Discord (une nouvelle liaison
   remplace l'ancienne).
4. Les joueurs Bedrock (UUID Floodgate) se lient de la même façon.

Effets de la liaison : `/stats` sans argument montre ses propres stats ; dans les messages des **classements
mensuels uniquement**, un joueur lié est affiché avec ses **deux noms** : pseudo Minecraft + mention Discord (ping).
Partout ailleurs, pseudo Minecraft seul, sans ping. Rôles Discord selon le rang : **pas dans la v1** (possible plus tard).

## 5. Commandes slash

Toutes les réponses sont en français, avec accents. Un graphique = une image PNG dans un embed.

| Commande | Réponse |
|---|---|
| `/stats [joueur]` | Profil : points et rang par jeu (général et mois), parties jouées, victoires, taux de victoire, record personnel de chaque jeu ; graphique de l'évolution des points sur 30 jours. Sans argument : son compte lié |
| `/classement <jeu> [mois]` | Top 10 en image (barres), général ou du mois ; mois archivé en option |
| `/records [jeu]` | Accomplissements (§ 7) : détenteur et valeur de chaque record |
| `/activite [période]` | Parties par jour et par jeu, joueurs actifs, part Java / Bedrock |
| `/comparer <joueur1> <joueur2>` | Deux profils côte à côte (validé par LeKiwi06) : pour **chaque statistique**, l'écart en **+ / − %** du joueur 1 par rapport au joueur 2 (ex. « Points PvP Kit : 1 250 vs 980 (+27,6 %) ») ; « — » si la valeur du joueur 2 est nulle. Pour un temps, plus court = mieux : le signe suit l'avantage (un meilleur tour plus rapide s'affiche en +) |
| `/lier <code>`, `/delier` | Liaison (§ 4) |

Choix du jeu et du joueur : **autocomplétion** Discord (liste des jeux de `/status`, pseudos connus du journal).

## 6. Messages automatiques

Salons choisis par les administrateurs Discord avec `/config salon <type> <#salon>` (permission « Gérer le
serveur »), gardés dans `bot.sqlite`.

- **Salon des classements** : un message par jeu (image du top 10 général + du mois), **modifié** (pas reposté)
  toutes les **10 minutes** s'il a changé.
- **Annonces** :
  - nouveau record d'un accomplissement (§ 7) ;
  - nouveau n° 1 d'un classement (général ou du mois).
- **Salon « résultats de parties »** : **toutes les parties de tous les jeux**, avec **un fil (thread) par mode de
  jeu** (PvP Kit, Course de bateau, Parcours, Bingo, Rush...). Le bot crée le fil d'un jeu à sa première partie et
  retient son identifiant. Chaque fin de partie : résumé court (vainqueur, top 3, points ; pour les jeux qui n'ont
  que des points : top 3 aux points). Si Discord archive un fil inactif, le bot le rouvre en y écrivant.
- **Récaps** : par défaut chaque **lundi à 10 h** (semaine écoulée) et le **1er du mois à 10 h** (mois écoulé, avec
  l'archive de KG_ScoreBoards) : top 3 par jeu, jeu le plus joué, joueur le plus actif, records battus, graphique
  d'activité. Fuseau : `Europe/Paris`. **Jour et heure réglables** par les administrateurs Discord
  (`/config recap hebdo <jour> <hh:mm>`, `/config recap mensuel <jour du mois> <hh:mm>`, `/config recap off`),
  gardés dans `bot.sqlite` ; valeurs par défaut dans le `.env`.

## 7. Accomplissements (calculés à partir du journal)

Ce que le journal contient aujourd'hui, par jeu :

| Jeu | Événements | Détail disponible |
|---|---|---|
| PvP Kit (KG_PvpKit 1.0.0) | `round`, `points` | kit, équipes, équipe gagnante, durée du combat ; par joueur : équipe, éliminations, place, série, déclassement, points |
| Course de bateau (KG_BoatRace) | `race`, `lap`, `points` | ordre d'arrivée, arrivé ou non, tours, temps de chaque tour et de chaque checkpoint |
| Bingo (KG_Bingo) | `points` | points de chaque joueur de la partie |
| Parcours (KG_Parkour), Rush (KalGames) | `points` (ScoreBridge) | points seulement : **pas de vainqueur ni de détail** |

Accomplissements proposés pour la v1 :

| Accomplissement | Jeux | Source |
|---|---|---|
| **Plus longue série de victoires** (en cours et record) | PvP Kit, Course de bateau | PvP : **deux séries distinctes**, manches gagnées (équipe du joueur = équipe gagnante) et matchs gagnés ; bateau : 1er arrivé |
| Plus d'éliminations en une manche / au total | PvP Kit | `round.results[].kills` |
| Meilleur tour, meilleure course | Course de bateau | `lap`, `race` |
| Plus de points en une partie | tous | `points` |
| Plus de parties jouées (total, du mois) | tous | nombre de `match` distincts par joueur |
| Plus de victoires | PvP Kit, Course de bateau | comme la série |
| Premier à atteindre 100 / 1 000 / 10 000 points | tous | cumul des `points` comptés |

Règles communes : seuls les événements **comptés** (`counted: true`) entrent dans les records ; les opérateurs
sont écartés comme dans les classements ; une partie **non classée** est ignorée : elle **n'interrompt pas** une
série (décision de LeKiwi06).

Match gagné au PvP Kit : le journal ne contient que les manches (`round`) ; le vainqueur du match est déduit des
manches d'un même `match` (équipe qui en a gagné le plus). À vérifier sur le journal réel à l'étape 3 ; si c'est
ambigu (égalité), un événement de fin de match serait à ajouter dans KG_PvpKit (à demander à ce moment-là).

Pour avoir séries et victoires au **Parcours**, au **Rush** et au **Bingo**, il faudra plus tard enrichir leur journal
(événement de fin de partie avec vainqueur) : **hors de ce chantier**, à faire quand ces jeux seront retouchés.

## 8. Graphiques (v1)

- Barres horizontales : top 10 d'un classement (points ou temps).
- Courbe : évolution des points d'un joueur (30 jours), un trait par jeu.
- Barres empilées : parties par jour et par jeu (activité).
- Anneau : part Java / Bedrock, part de chaque jeu.
- Couleurs fixes par jeu (même couleur partout), lisibles sur le thème sombre et clair de Discord.

## 9. Sécurité

- Le dépôt est public : **jeton Discord, jeton de l'API, identifiants de salons privés** seulement dans le `.env`
  du bot sur sa machine d'hébergement (`.env.example` vide dans le dépôt) et dans le `config.yml` de kal-games.
- API : jeton obligatoire, lecture seule sauf la vérification des codes ; limite de requêtes ; aucune donnée
  personnelle autre que pseudo, UUID et statistiques.
- Bot Discord : permissions minimales (voir les salons, envoyer des messages, envoyer des messages dans les fils,
  créer des fils publics, joindre des fichiers, intégrer des liens, lire l'historique des messages) ; aucune
  intention privilégiée (pas de lecture du contenu des messages ni de la liste des membres).

## 10. Étapes

| Étape | Contenu | Plugins / dossiers touchés |
|---|---|---|
| **1** (faite le 29/09/2026 : KG_ScoreBoards 1.7.0 + bot 0.1.0, non testée) | API en lecture dans KG_ScoreBoards (`status`, `rankings`, `players`, `events`) + squelette du bot (`/classement` en image). Valide toute la chaîne | KG_ScoreBoards, KLM_DiscordBot |
| **2** | Base locale, synchronisation du journal, `/stats`, `/activite` et leurs graphiques | KLM_DiscordBot |
| **3** | Accomplissements, `/records`, annonces de records | KLM_DiscordBot |
| **4** | Liaison (`/lier` en jeu et sur Discord) | KG_ScoreBoards, KLM_DiscordBot |
| **5** | Salon auto-rafraîchi, annonces de fin de partie, récaps | KLM_DiscordBot |
| **6** | Choix de l'hébergement et déploiement | — |

Chaque étape est testée avant la suivante (`REGLES.md` § 3.2). Pendant le développement, le bot peut tourner sur
un PC contre l'API de kal-games (ou un faux jeu de données tiré d'un journal téléchargé **hors du dépôt**).

**Préalables côté humain** (Claude ne crée pas de compte) : créer l'application et le bot dans le portail
développeur Discord, l'inviter sur le serveur Discord, copier le jeton dans le `.env` ; demander un port
supplémentaire pour kal-games à Minestrator.

## 11. Questions tranchées (LeKiwi06, 29/09/2026)

1. Nom du dossier : **`KLM_DiscordBot`**, validé.
2. Port de l'API : **Minestrator peut ouvrir un port de plus** sur kal-games (7001).
3. Partie non classée : **ne coupe pas** la série. PvP Kit : on compte **les manches et les matchs** (deux séries).
4. Résultats de parties : **tous les jeux**, dans un salon « résultats de parties » avec **un fil par mode de jeu** (§ 6).
5. Ping : **uniquement dans les classements mensuels**, avec les deux noms (pseudo Minecraft + mention Discord) (§ 4).
6. `/comparer` : **oui**, avec un écart en **+ / − %** pour chaque statistique (§ 5).
7. Récaps : lundi 10 h et 1er du mois 10 h par défaut, **jour et heure réglables** (§ 6).
