# Kalium — plugins du réseau Minecraft KaLium

Code source des plugins du réseau KaLium (Paper 26.2 / Velocity), maintenu avec Claude.

| Dossier | Plugin | Serveur |
|---|---|---|
| `KalGames/` | Hub et mini-jeux (Rush, parkour, courses...) | kal-games |
| `KalBingo/` | Jeu Bingo complet | Kixster |
| `KaliumMenu/` | Menu de navigation entre serveurs | chaque serveur Paper |
| `KaliumRelay/` | Relais HTTP entre serveurs | proxy Velocity |
| `KaliumCore/` | Survie (projet en pause) | Kal-Test-Dev |

- **À lire en premier** : `REPRISE_PROJET.md` (état du projet, versions déployées, compilation, déploiement, règles).
- Historique détaillé de chaque plugin : `<plugin>/JOURNAL.md`.
- `jars-deployes/` : copies des jars actuellement en service.

## Règles de travail (valables pour chaque session Claude)

- Ne rien construire qui n'ait pas été explicitement demandé ; en cas de doute, demander avant.
- Déploiement non destructif : l'ancien jar va dans `/plugins/_removed-<plugin>-<version>/`.
- Textes en jeu en français.
- Chaque nouvelle version : `VERSION=` dans `build.sh`, `<version>` dans `pom.xml`, section dans `JOURNAL.md`.
- Plusieurs Claude en parallèle : un plugin par session, et récupérer (`git pull`) avant de commencer.

Le compilateur et les bibliothèques (`outils-build/`, ~32 Mo) ne sont pas dans le dépôt : `sh telecharger-outils.sh`
les récupère (une fois par machine), puis `sh <plugin>/build.sh` produit le jar dans `sortie/`
(voir `REPRISE_PROJET.md`, section « Compiler »).
