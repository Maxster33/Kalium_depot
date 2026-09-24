# Kalium — plugins du réseau Minecraft KaLium

Code source des plugins du réseau KaLium (Paper 26.2 / Velocity), maintenu par LeKiwi06 et Maxster33 avec Claude.

**Avant toute chose, lire dans l'ordre : `REGLES.md`, `TRAVAIL_EN_COURS.md`, `REPRISE_PROJET.md`.**

| Dossier | Plugin | Serveur |
|---|---|---|
| `KalGames/` | Hub et mini-jeux (Rush, parkour, courses...) | kal-games |
| `KalBingo/` | Jeu Bingo complet | Kixster |
| `KaliumMenu/` | Menu de navigation entre serveurs | chaque serveur Paper |
| `KaliumRelay/` | Relais HTTP entre serveurs | proxy Velocity |
| `KaliumCore/` | Survie (projet en pause) | Kal-Test-Dev |

- Historique détaillé de chaque plugin : `<plugin>/JOURNAL.md`.
- `jars-deployes/` : copies des jars actuellement en service.
- Compiler : `sh telecharger-outils.sh` (une fois par machine), puis `sh <plugin>/build.sh` → jar dans `sortie/`
  (détails dans `REPRISE_PROJET.md`, section « Compiler »).
