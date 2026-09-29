# KLM_DiscordBot

Bot Discord du réseau KaLium : classements, statistiques et graphiques tirés de KG_ScoreBoards (kal-games).
Cahier des charges : `CAHIER_DES_CHARGES.md` · historique des versions : `JOURNAL.md`.

## Lancer le bot (PC ou hébergement)

1. Node.js 22 ou plus.
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
