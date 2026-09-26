# Équilibrage des barèmes de points (demande de LeKiwi06, 26/09/2026)

Objectif : **30 minutes « à fond » rapportent autant de points dans chaque jeu** (course de bateau = Parcours = Bingo
= …). Décisions de LeKiwi06 (26/09/2026) :
- **Retoucher le barème de chaque jeu** (pas de coefficient global) ;
- **référence = la course de bateau actuelle** (barème de KG_BoatRace 1.3.0 et suivants), qu'on ne change pas ;
- **recalculer le passé** (points déjà gagnés).

## Mesures (26/09/2026)

Sources : journal des parties de KG_ScoreBoards sur Kal-Games (`plugins/KG_ScoreBoards/journal/2026-09/*.jsonl`) et
journaux de Serveur Jeux (lignes « Partie … terminee … ; solo : » de KG_BingoGame). Temps = temps de jeu (course, ou
partie de Bingo préparation comprise), sans l'attente entre les parties.

| Jeu | Points crédités pour 30 min | Données |
|---|---|---|
| Course de bateau (référence) | ~135 en médiane (cumul crédité), ~160 pour les gagnants | 36 résultats, 19 courses solo |
| Bingo (barème 0.3.0 à 0.7.8) | ~70 pour les gagnants (55 à 135), ~20 à 40 pour les autres | 12 vraies parties de 27 à 63 min |
| Parcours (ancien barème : 1 pt / checkpoint + podium 3/2/1) | ~40 à 70 | 7 parties, une seule map |
| PvP Kit, Rush | pas de données | 3 événements |

## Plan

1. **Bingo** (fait le 26/09/2026, KG_BingoGame 0.8.0 + KG_Bingo 1.5.0) : barème **doublé** (objectifs 2 / 6 / 10 / 20,
   bonus du 1er 0 / 2 / 4 / 6 (1 / 3 / 5 / 10 depuis 0.8.1 : moitié des points de base), bonus de victoire 2 / 4 / 6 / 10 ; coefficients des bingos, multiplicateur de vitesse du
   blackout et bonus d'XP inchangés) ; **points du Bingo crédités dans les classements** (classement « bingo », général
   et du mois). Attendu : gagnant ~140 / 30 min. Déployés le 26/09/2026 à 5 h 23, testés et confirmés par LeKiwi06 le 26/09/2026 ; rythme réel à revérifier sur de vraies parties.
2. **Parcours** : coder le barème du cahier des charges (`KG_Parkour/CAHIER_DES_CHARGES.md` : chrono allongé à chaque
   checkpoint, 1 / 3 / 5 points selon la difficulté, bonus du 1er, first try ×2, paliers de temps), puis caler les
   valeurs sur ~135 / 30 min. **Il faut des parties « à fond » de LeKiwi06** pour mesurer (temps par checkpoint).
3. **PvP Kit, Rush** : retoucher par estimation, puis corriger avec les vraies parties.
4. **Recalcul du passé** : outil dans KG_ScoreBoards qui relit le journal. Le détail des anciens points n'étant pas
   gardé (Parcours), ce sera une **règle de trois par jeu** (anciens points × nouveau rythme ÷ ancien rythme). Le journal
   ne remonte qu'au 24-25/09/2026. Bingo : les parties passées n'ont jamais été créditées ; elles sont dans les journaux
   de Serveur Jeux (points solo avec l'ancien barème) et pourraient être créditées ×2.
5. Vérifier les rythmes régulièrement : même calcul sur le journal (à automatiser dans KG_ScoreBoards si utile).
