# 2026-10-06 — Les piliers de la Fortune (nouveau mini-jeu)

- Plugin(s) concerné(s) : KG_PiliersFortune (nouveau)
- Versions avant / après : — → KG_PiliersFortune 0.1.0 (déployée sur Kal-Games le 06/10/2026 à 13 h 12, non testée)

## Demandé
« Nous allons créer le jeu : Les piliers de la Fortune », de 4 à 8 joueurs, parties publiques et privées, map
pré-générée en 8 exemplaires, remise en état des blocs et des objets au sol à la fin, zone d'attente, départ sur des
piliers en pierre (vie, faim, saturation pleines, XP à 0, inventaire vide, immobiles pendant un décompte de 5 s au
milieu de l'écran), chronomètre et « joueurs en vie : 4/8 » en bas de l'écran, un objet aléatoire toutes les 5 s (au
sol si l'inventaire est plein), élimination sous la couche -64 puis mode spectateur, 10 min au plus, barème (+1 par
minute en vie, +5 par joueur tué ou tombé, x1 / x2 / x3... selon le rang d'élimination, gagnant seul x3, égalité :
scores inchangés). Plugin, intégration au menu Kal-Games et classements. Texte complet dans
`KG_PiliersFortune/JOURNAL.md`.

Ensuite (avant tout déploiement) : retirer les livres enchantés ; explosion (TNT, cristal de l'End) créditée au poseur
de l'explosif ; mort par feu ou lave, chute à cause d'une source d'eau créditée au poseur de la source ; débloquer le
feu et les pistons ; diviser les points par 3 ; interdire de poser un bloc contre une barrière. Puis : eau créditée
seulement 10 s après la pose de la source, et un vrai coup reçu ensuite l'emporte ; pas de division par 3 à la fin mais
des points de base recalés (~135 / 30 min, gagnant ~55 à 4 joueurs, ~70 à 8) ; ni eau ni lave contre une barrière ;
creeper crédité à l'utilisateur de l'œuf ; bloc cassé sous les pieds crédité au casseur ; objet d'un distributeur ou
d'un dropper crédité à celui qui l'a mis dedans.

## Fait
- Réservation de KG_PiliersFortune, puis plugin écrit et compilé (`KG_PiliersFortune-0.1.0.jar`).
- Demandes complémentaires codées dans la même version 0.1.0 (jamais déployée) : détail dans le journal du plugin.
  Le feu et les pistons sont débloqués par KG_PiliersFortune lui-même (dans ses parties seulement), sans toucher
  KalGames.
- Aucun changement dans KalGames, KG_Menu ni KG_ScoreBoards : le moteur de parties fournit déjà les arènes
  pré-générées (`prewarm-arenas`), la remise en état, les files publiques / parties privées, le bouton du menu et les
  classements par jeu.

## Décisions
- Questions posées avant de coder, réponses de Maxster33 : multiplicateurs exactement comme écrits (le 7e éliminé peut
  avoir x7 et le gagnant x3) ; objets : tous les objets de survie + œufs d'apparition ; pas de règle commune « moyenne
  des joueurs classés en dessous » ; la map existe déjà sur Kal-Games.
- Choix techniques signalés dans le journal : œufs du Wither et de l'Ender Dragon exclus, créatures frappables, mort et
  départ = élimination, chute créditée au dernier joueur qui a frappé dans les 10 s.

- Barème final, après plusieurs propositions de calcul : (0,25 point toutes les 30 s en vie + 7 par élimination) x rang
  de mort (survivants : rang suivant, partagé), x1,5 pour le gagnant s'il gagne avant la fin. Estimation : gagnant ~63 à
  4 joueurs, ~210 à 8 joueurs, ~128 / 30 min en moyenne ; à vérifier sur les vraies parties. Toutes les créatures des
  œufs créditées à l'utilisateur de l'œuf.
- Affichage ajouté : tableau à droite (joueurs en vie en vert en haut, éliminés en rouge en dessous, classés par
  points), message toutes les 30 s (+0,25), message à chaque élimination (par qui, comment, +7 points). Points du
  tableau en cours de partie = ceux qu'aurait le joueur si la partie s'arrêtait maintenant (choix de Claude, signalé).
- Leçon : présenter le barème étape par étape (temps, éliminations, rang, bonus), un exemple chiffré, sans tableaux de
  moyennes compliqués.

## Reste à faire
- Fait : déployé sur Kal-Games (« installe », 13 h 12). Reste : redémarrage par l'humain, créer l'arène, la capturer,
  poser la zone d'attente et les 8 piliers ; puis tester (liste dans le journal).
