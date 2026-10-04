# 2026-10-04 — Barème des items, Raccommodage

- Plugin(s) concerné(s) : KS_LootCoffres, KS_Crafts, KS_LootBlocs, KS_LootPotions, KS_Economy (KS_EstomacGardien
  réservé puis libéré sans changement)
- Versions avant / après : KS_LootCoffres 1.0.0 / 1.1.0 ; KS_Crafts 1.9.0 / 1.10.0 ; KS_LootBlocs 1.2.0 / 1.3.0 ;
  KS_LootPotions 1.1.1 / 1.1.3 ; KS_Economy 1.2.0 / 1.3.0

## Demandé

LeKiwi06 :
- un barème de prix en émeraudes pour chaque item (26.3 + objets custom), avec rareté, difficulté d'obtention et
  utilité ; d'abord rempli par Claude, puis repris à la main par LeKiwi06, puis complété par Claude selon sa logique ;
- « je veux que les joueurs puissent avoir l'item avec mending en 10 à 20 tirages de loot table ».

## Fait

- Barème (hors dépôt, dossier `sortie/` du PC de LeKiwi06) : matières de base aux prix d'ancrage validés par LeKiwi06
  (terre 0,01 ; fer 0,15 ; diamant 4 ; débris antique 40...), objets fabriqués = somme des ingrédients, crafts custom
  compris ; 1 émeraude par minute de temps de transformation ; matières premières selon le temps pour un stack,
  l'automatisation et la demande.
- KS_LootCoffres 1.1.0 : jambières en diamant de la cité antique enchantées niveau 30 à 50 (au lieu de 10 à 32).
  Déployé sur Event le 04/10/2026 à 06:05, non testé, actif après redémarrage.
- KS_Crafts 1.10.0, KS_LootBlocs 1.3.0, KS_LootPotions 1.1.2 : plus de brassage à la verrue, verrue cultivable,
  potion sur verrue mûre seulement, boîte de shulker au coffre de l'Ender. Déployés sur Event le 04/10/2026 à
  06:52, non testés, actifs après redémarrage.
- KS_LootPotions 1.1.3 : potion de chance à 100 % sur le minerai d'émeraude de deepslate (10 % avant), jamais quand
  le bloc tombe tel quel (Toucher de soie : on pouvait reposer le minerai et recommencer). Compilé, non déployé
  (« pas maintenant », LeKiwi06).
- KS_Economy 1.3.0 : barème des prix dans le plugin (`rachats.csv`) et rachats de la semaine (10 objets tirés chaque
  lundi, 2 par gamme de prix, par familles ; lots de 1 / 10 / 64 / 320 émeraudes ; quota par objet et par jour
  au-dessus de 25 000 de cagnotte). Compilé, non déployé.

## Décisions

- LeKiwi06 : Raccommodage vaut 1 500 émeraudes ; il doit rester trouvable dans les structures (armure durable sans
  /rewards, livres en quantité pour les outils, pièces qui prennent de la valeur avec le temps).
- Une première demande (« bottes, plastron et casque suivent le niveau des jambières ») a été codée en local puis
  annulée avant commit : elle aurait divisé par 5 la chance de Raccommodage, à l'inverse de l'objectif. C'est le niveau
  des jambières qui a été relevé.
- Chances de Raccommodage (environ 40 % au niveau 30 à 50, 8 % au niveau 10 à 32) : simulation du tirage vanilla par
  Claude, non mesurées en jeu.
- LeKiwi06 : Fragment de Spawner 128 émeraudes ; Cœur de Spawner = récompense d'événement uniquement (prix à fixer).

- LeKiwi06 : le brassage au bloc de verrue était « une erreur » ; la verrue du Nether redevient cultivable mais ne se
  brasse plus ; boîte de shulker au coffre de l'Ender (casser la production automatique). Ces choix reviennent sur des
  demandes de Maxster33 du 25/09.
- LeKiwi06 (série 12 du barème, anomalies relevées par Claude) : potions et élixirs revus à la baisse, livre enchanté
  ordinaire au prix de la table d'enchantement, carte au trésor enfoui à 30, netherrack, briques, feuilles, graines,
  bourgeons d'améthyste, boîte de shulker au prix de sa recette, têtes vanilla au prix des têtes de KS_Decapitator.
- LeKiwi06 (rachats) : lot de 64 émeraudes pour la gamme 1 à 10 ; aucun quota sous 25 000 ; tirage par familles,
  objets custom compris. Choix de Claude signalés : jetons, monnaie et objets au contenu variable jamais tirés.

## Reste à faire

- Redémarrer Event (l'humain) ; tester un coffre de cité antique jamais ouvert.
- Déployer KS_LootPotions 1.1.3 et KS_Economy 1.3.0 (accord de LeKiwi06 à demander), puis les tester.
- Prix du Cœur de Spawner ; relecture des matières premières « estimées » du barème.
- Signalé, à décider : craft « 9 verrues → bloc de verrue » toujours retiré ; carapace de shulker seulement si un
  joueur tue ; éclat d'écho sur les chauves-souris (aucun plugin ne le fait).
