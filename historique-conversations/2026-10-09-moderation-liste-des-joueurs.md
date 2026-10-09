# 2026-10-09 — Modération : liste de tous les joueurs en têtes, claims d'un joueur

- Plugin(s) concerné(s) : KS_AntiCheat
- Versions avant / après : KS_AntiCheat 1.1.1 / 1.2.0 (compilé, non déployé, non testé)

## Demandé

LeKiwi06 : « il faudrait compléter la modération sur kixster, j'aimerai une interface de coffre avec les têtes de tout
les joueurs meme hors ligne, pour ne pas avoir a taper leurs pseudo, j'aimerai que cette option m'ouvre une interface
pour voir leur inventaire, ender chest, leurs liste de claim, etc »

## Fait

- KS_AntiCheat 1.2.0 : outil « Joueurs » dans la rubrique « Modération » de `/menu` et bouton « Tous les joueurs » de
  `/anticheat` : coffre de têtes (tous les joueurs connus du serveur, connectés d'abord, 45 par page) ; clic = fiche du
  joueur, qui gagne un bouton « Claims » (nom et position de ses claims, lus dans SimpleClaimSystem). Détail :
  `KS_AntiCheat/JOURNAL.md`.
- Vérifié dans le Paper 26.2 du PC : la tête d'un joueur hors ligne est posée avec son seul identifiant, c'est le jeu
  du staff qui va chercher l'apparence.

## Décisions

- Dans KS_AntiCheat (déjà sur Kixster, il porte déjà invsee, ecsee et la fiche d'un joueur) plutôt qu'un nouveau
  plugin : c'est le même rôle (outils du staff).
- La fiche reste un menu (Dialog) ; seule la liste des joueurs est un coffre, comme demandé.
- Claims lus directement dans SimpleClaimSystem : KS_Claim n'est pas modifié.
- « etc. » non interprété (règle : ne rien construire qui n'a pas été demandé) : question posée à LeKiwi06.

## Reste à faire

- Accord de LeKiwi06 pour déployer sur Kixster (et sur Event ?), puis test en jeu.
- Réponse de LeKiwi06 sur ce que le « etc. » doit couvrir (propositions : solde et magasin, maisons, jetons et badges,
  récompenses en attente, téléportation vers un claim, têtes aussi pour les outils « Invsee » et « EcSee »).
