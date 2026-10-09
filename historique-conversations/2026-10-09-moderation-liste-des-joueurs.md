# 2026-10-09 — Modération : liste de tous les joueurs en têtes, claims d'un joueur

- Plugin(s) concerné(s) : KS_AntiCheat ; lecture publique ajoutée à KS_Economy, KS_Teleport, KS_CoffreMort, KS_RewardsGUI
- Versions avant / après : KS_AntiCheat 1.1.1 / 1.2.0 (Kixster, 18:11) puis 1.3.0 (Kixster, 18:31, avec KS_Economy
  1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1) ; non testés ; Event n'est pas touché

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
- « etc. » non interprété dans la 1.2.0 (règle : ne rien construire qui n'a pas été demandé) : question posée à
  LeKiwi06, qui a choisi pour la version suivante : économie (solde, magasin, ventes et échanges), maisons et
  téléportation (ses /home, se téléporter à un claim ou à une maison), jetons et récompenses (inventaire spécial,
  récompenses en attente, coffres de mort).
- Déploiement : « Kixster seulement » (LeKiwi06) ; envoyé à 18:11, 1.1.1 rangée dans `_removed-ks_anticheat-1.1.1/`.

## Suite : le « etc. » (KS_AntiCheat 1.3.0)

- « j'ai redémarré kixster, tu peux lire le journal » (LeKiwi06) : 1.2.0 active sans erreur (redémarrage de 18:15).
- « vas-y pour la suite » (LeKiwi06) : KS_AntiCheat 1.3.0 (écrans « Économie », « Maisons », « Jetons et
  récompenses », téléportation du staff, journal des `/echange`) avec une lecture publique ajoutée à KS_Economy 1.4.1,
  KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 (réservés en « Requis parfois »). Compilés, non
  déployés.
- KS_RewardsGUI était réservé par une autre session de LeKiwi06 de 18:17 à 18:23 (1.4.0 déployée) ; une fois libéré,
  lecture publique ajoutée aussi (1.4.1 : liste des récompenses en attente).
- KS_Jetons : non modifié, il exposait déjà les nombres de jetons et les niveaux de badges.

## Reste à faire

- Test en jeu de la 1.2.0 (Kixster redémarré par LeKiwi06 à 18:15 ; journal lu : activée sans erreur, menu pas encore ouvert). À regarder : les têtes des joueurs hors ligne (comportement vérifié dans Paper 26.2 seulement, Kixster est en 26.3).
- Test en jeu des cinq plugins envoyés à 18:31 (accord : « Oui, sur Kixster » ; Kixster redémarré par LeKiwi06 à
  18:36, journal lu : les cinq activés sans erreur, fiche pas encore ouverte). À regarder : point d'arrivée d'un claim, arrivée sur un lit ou un coffre de mort.
- Contenu (objets) des récompenses en attente et vue de l'inventaire spécial : non montrés, à demander si utile.
