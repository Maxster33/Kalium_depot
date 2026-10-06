# 2026-10-06 — Les piliers de la Fortune (nouveau mini-jeu)

- Plugin(s) concerné(s) : KG_PiliersFortune (nouveau)
- Versions avant / après : — → KG_PiliersFortune 0.1.0 (compilée, non déployée)

## Demandé
« Nous allons créer le jeu : Les piliers de la Fortune », de 4 à 8 joueurs, parties publiques et privées, map
pré-générée en 8 exemplaires, remise en état des blocs et des objets au sol à la fin, zone d'attente, départ sur des
piliers en pierre (vie, faim, saturation pleines, XP à 0, inventaire vide, immobiles pendant un décompte de 5 s au
milieu de l'écran), chronomètre et « joueurs en vie : 4/8 » en bas de l'écran, un objet aléatoire toutes les 5 s (au
sol si l'inventaire est plein), élimination sous la couche -64 puis mode spectateur, 10 min au plus, barème (+1 par
minute en vie, +5 par joueur tué ou tombé, x1 / x2 / x3... selon le rang d'élimination, gagnant seul x3, égalité :
scores inchangés). Plugin, intégration au menu Kal-Games et classements. Texte complet dans
`KG_PiliersFortune/JOURNAL.md`.

## Fait
- Réservation de KG_PiliersFortune, puis plugin écrit et compilé (`KG_PiliersFortune-0.1.0.jar`).
- Aucun changement dans KalGames, KG_Menu ni KG_ScoreBoards : le moteur de parties fournit déjà les arènes
  pré-générées (`prewarm-arenas`), la remise en état, les files publiques / parties privées, le bouton du menu et les
  classements par jeu.

## Décisions
- Questions posées avant de coder, réponses de Maxster33 : multiplicateurs exactement comme écrits (le 7e éliminé peut
  avoir x7 et le gagnant x3) ; objets : tous les objets de survie + œufs d'apparition ; pas de règle commune « moyenne
  des joueurs classés en dessous » ; la map existe déjà sur Kal-Games.
- Choix techniques signalés dans le journal : œufs du Wither et de l'Ender Dragon exclus, créatures frappables, mort et
  départ = élimination, chute créditée au dernier joueur qui a frappé dans les 10 s.

## Reste à faire
- Déployer sur Kal-Games (sur demande de Maxster33), redémarrer, créer l'arène, la capturer, poser la zone d'attente et
  les 8 piliers ; puis tester (liste dans le journal).
