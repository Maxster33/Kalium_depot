# 2026-10-09 — Rachats de la semaine : tirage par familles, dimension fermée

- Plugin(s) concerné(s) : KS_Economy
- Versions avant / après : KS_Economy 1.3.1 / 1.4.0 (déployé sur Event le 09/10/2026 à 05:52, non testé)

## Demandé

LeKiwi06 :
1. « tu peux ajouter les items de la 26.3 à la liste des prix »
2. « j'aimerai modifier le tirage des items pour les rachats de la semaine, il faudrait tirer 10 items aléatoire en
   gardant le système de "famille" mais sans restriction de 2 par ordre de prix »
3. « aussi tout à l'heure, des items issus du nether ont été tirés alors qu'il est fermé, ce n'est pas normal »

## Fait

- Objets de la 26.3 : rien à ajouter. La liste du barème a été comparée au registre du Paper 26.3-159
  (`mise-a-jour-26.3/`) : 1 658 objets des deux côtés, aucun écart ; les 121 nouveautés de la 26.3 ont un prix dans le
  tableau du barème et dans `rachats.csv` (105 tirables, 16 cartes de structure jamais tirées). Restent sans prix,
  comme avant, 122 objets non obtenables (œufs d'apparition, blocs de commande, pierres infestées...).
- KS_Economy 1.4.0 : tirage de 10 familles au hasard puis un objet par famille, sans gamme de prix ; une offre issue
  d'une dimension fermée dans KS_Dimensions est remplacée à chaque vérification. Détail : `KS_Economy/JOURNAL.md`.

- « oui déploie sur event » (LeKiwi06) : KS_Economy 1.4.0 envoyé sur Event à 05:52, 1.3.1 rangé dans
  `_removed-ks_economy-1.3.1/` ; actif après redémarrage.

- « j'ai redémarré event, tu peux lire le journal » (LeKiwi06) : 1.4.0 active sans erreur (redémarrage de 06:01).
  Cause des objets du Nether trouvée : sur Event, KS_Dimensions indique les portails du Nether et de l'End « activés »
  à chaque démarrage depuis le 01/10, jamais fermés dans `/dimensions` ; le plugin ne filtre donc rien. Tirage de la
  semaine 41 : `dragon_breath` (End) et `red_shulker_box` (End + Nether), aucun autre objet du Nether ou de l'End.

## Décisions

- Les gammes de prix ne servent plus qu'à la taille des lots (inchangée, non demandée).
- Dimension fermée : la lecture des fichiers d'Event a été refusée pendant la session, la cause n'est donc pas
  établie. Le remplacement d'une offre quand sa dimension est fermée après le tirage est un choix de Claude, signalé à
  LeKiwi06 : c'est le seul cas trouvé dans le code où un objet du Nether reste proposé Nether fermé.

## Reste à faire

- LeKiwi06 : préciser par quel moyen le Nether est fermé sur Event ; les rachats ne connaissent que `/dimensions`.
- Tester en jeu : `/rachat`, fermer le Nether
  dans `/dimensions` et vérifier que les objets du Nether sont remplacés dans la minute.
- Le tirage de la semaine 41 n'est pas refait par la mise à jour : nouveau tirage lundi 12/10, ou `rachats.yml`
  supprimé serveur éteint par l'humain.
- Toujours ouvert dans le barème : prix de l'arbuste rouge et du champignon étagère (« à vérifier »).
