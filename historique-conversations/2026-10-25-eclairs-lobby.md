# 2026-10-25 — Éclairs décoratifs au lobby

- Plugin concerné : KLM_Hub
- Versions avant / après : 1.0.0 / 1.1.0 (déployée sur le lobby le 25/10/2026 à 03:14, à redémarrer)

## Demandé
« j'ai mis le monde du lobby en mode orage et nuit pour faire une ambiance halloween, mais je ne vois aucun éclair
tomber durant l'orage, est-ce normal ? », puis « oui fais le » pour des éclairs décoratifs pendant l'orage.

## Fait
- Explication : rien ne bloque les éclairs dans nos plugins ; ils sont rares et, autour d'une île dans le vide, tombent
  presque tous au fond du monde.
- KLM_Hub 1.1.0 : éclair « effet seul » toutes les 6 à 20 s, à 16 à 48 blocs d'un joueur, quand il y a de l'orage.
  Section `lightning` du `config.yml`, active par défaut. Détail : `KLM_Hub/JOURNAL.md`.

## Décisions
- Mis dans KLM_Hub (le plugin qui accueille les joueurs au lobby) plutôt que dans un nouveau plugin.
- Éclairs autour des joueurs plutôt que d'un point fixe : au lobby les joueurs sont près du spawn, et l'éclair reste
  visible où qu'ils soient.

## Reste à faire
- Redémarrer le lobby (1.1.0 envoyée le 25/10/2026 à 03:14, 1.0.0 dans `_removed-klm_hub-1.0.0/`).
- Tester en jeu (Java et Bedrock) : `/weather thunder`, des éclairs visibles et audibles, pas de feu.
