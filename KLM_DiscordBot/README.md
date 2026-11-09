# KLM_DiscordBot

Bot Discord du réseau KaLium : classements, statistiques et graphiques tirés de KG_ScoreBoards (kal-games), et wiki
du réseau dans un salon (`k!wiki`).
Cahier des charges : `CAHIER_DES_CHARGES.md` · historique des versions : `JOURNAL.md`.

## Lancer le bot (PC ou hébergement)

1. Node.js 20 ou plus.
2. Dans ce dossier : `npm install`
3. Copier `.env.example` en `.env` et le remplir (jeton du bot, identifiant du serveur Discord, jeton de l'API).
   **Le fichier `.env` ne doit jamais être commité** (il est dans `.gitignore`).
4. `npm start`

`npm run apercu` dessine des graphiques d'exemple (`apercu-*.png`) sans Discord ni serveur.

## Jeton de l'API

Même valeur dans `.env` (`API_TOKEN`) et dans `plugins/KG_ScoreBoards/config.yml` sur kal-games (`api.token`).
Pour en générer un (PowerShell) :

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```

## Wiki KaLium (`k!wiki`)

Un salon textuel : un sommaire par serveur (Kixster, Kal-Games, Kanvas) qui renvoie vers une fiche par fil verrouillé.
Les textes sont dans `wiki/` (format : `wiki/LISEZMOI.md`).

1. Portail développeur Discord > Bot : activer **Message Content Intent** (le bot lit les commandes `k!`).
2. Inviter le bot avec : voir les salons, envoyer des messages (et dans les fils), créer des fils publics, gérer les
   fils, gérer les messages, intégrer des liens, voir l'historique, **gérer les rôles** (pour fermer le salon aux joueurs).
3. Créer le salon, puis taper dans n'importe quel salon (permission « Gérer le serveur ») : `k!wiki setup <id du salon>`.
4. Après une modification de `wiki/` : `k!wiki maj` (les messages sont modifiés sur place, les liens restent bons).

Les identifiants Discord publiés sont gardés dans `wiki-etat.json` (jamais commité) : lancer le bot toujours depuis
la même machine, sinon le wiki est republié en double. `API_URL` / `API_TOKEN` vides : le wiki marche, sans `/classement`.
