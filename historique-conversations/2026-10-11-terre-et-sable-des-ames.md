# 2026-10-11 — Terre des âmes et sable des âmes : familles des rachats et butin des coffres

- Plugin(s) concerné(s) : KS_Economy, KG_Rewards, KV_Rewards (`butin.yml` seulement), KS_Teleport, KS_RewardsGUI, KS_Decapitator, KS_Crafts, KS_Jetons, KS_Meule (nouveau)
- Versions avant / après : KS_Economy 1.5.0 en service (1.5.1 jamais déployée) / 1.5.2 déployée sur Event et
  Kixster ; KG_Rewards 1.3.0 et KV_Rewards 1.3.0 inchangés (`butin.yml` remplacés)

## Demandé

LeKiwi06 : « est-ce que la soul sand et le soul soil sont dans les loot des rewards ? », puis « ajoute la terre des
ames aux terres , et le sables des ames aux sables ». À la question « rachats de la semaine, coffres de récompense ou
les deux ? » : « Les deux ».

## Fait

- Réponse à la première question : non, aucun des deux n'était dans le butin des coffres (le sable des âmes figurait
  seulement dans la liste des objets du Nether du script, la terre des âmes nulle part).
- KS_Economy 1.5.2 : dans `rachats.csv`, la terre des âmes passe dans la famille « terres » et le sable des âmes dans
  la famille « sable » ; la famille « sable des âmes » disparaît (178 familles). Seules ces 2 lignes changent, prix
  inchangés. Détail : `KS_Economy/JOURNAL.md`. Compilé (`sortie/KS_Economy-1.5.2.jar`), poussé. Non déployé.
- Coffres de récompense : les deux blocs ajoutés à la famille « Blocs » du script des pools (hors dépôt), à côté des
  sables et des terres. 4 entrées de plus : 4 x (pool Commun) et 24 x (pool Peu commun) de chacun. `butin.yml` de
  KG_Rewards et de KV_Rewards régénérés et vérifiés (aucune erreur) ; classeur des valeurs régénéré. Non envoyés.

## Décisions

- Choix de Claude signalés : version 1.5.2 plutôt qu'un changement glissé dans la 1.5.1 (une version par
  changement) ; quantités des piles calculées comme pour les autres blocs (prix du barème, 0,04322 émeraude l'unité).
- Les fréquences de quelques niveaux bougent de 1 sur 1 000 (la moyenne des pools change à peine) ; valeurs moyennes
  des niveaux inchangées.

## Déploiement (11/10/2026, 06:05)

- Accord de LeKiwi06 : « oui déploie », puis « je te donne la permission de le faire » (le premier envoi avait
  été refusé par le filtre de permissions de la session de Claude, rien n'était parti).
- KS_Economy 1.5.2 sur Event et Kixster (1.5.0 dans `_removed-ks_economy-1.5.0/`) ; `butin.yml` sur Kal-Games
  et Kanvas (anciens dans `_removed-kg_rewards-butin-2026-10-11/` et `_removed-kv_rewards-butin-2026-10-11/`).
- Les `butin.yml` des serveurs étaient identiques à ceux du 09/10 : rien d'écrasé.
- Actifs après redémarrage par LeKiwi06. Réservations libérées.

## Autre demande de la session

- « as tu des remarques quant au départ de kixster SMP ? » : journaux de Kixster lus en lecture seule (09/10
  19:52 au 11/10 05:30). Aucune erreur de nos plugins, aucun ralentissement. Signalé à LeKiwi06, rien de
  corrigé : nouveaux joueurs qui apparaissent entre Y 118 et Y 123 au-dessus du spawn (2 morts de chute à la
  première connexion) ; erreur de WorldGuard 7.0.19 à la pose d'un bateau ; alertes GrimAC sur les joueurs
  Java ; CoreProtect absent de Kixster ; région `ile_du_dragon` absente de l'End ; `/reward` tapé par des
  joueurs (seul `/rewards` existe).

## Corrections pour Kixster (« oui fais les deux »)

- KS_Teleport 1.1.0 : première connexion au point d'apparition exact du monde (-54 73 212 sur Kixster) au lieu du toit
  du spawn. La proposition parlait du « point de `/spawn` », qui n'existe pas (`/spawn` = maison de chaque joueur) :
  remplacé par le point d'apparition du monde, signalé à LeKiwi06.
- KS_RewardsGUI 1.4.2 : alias `/reward`.
- Compilés, poussés, non déployés ; réservations gardées jusqu'à l'accord de déploiement sur Kixster.
- Autre question : chances d'obtenir un livre Raccommodage sur Kixster (coffres de récompense : 10 % d'un tirage
  Légendaire ; livres des coffres de structures restés vanilla ; ni villageois, ni pêche, ni table d'enchantement).

## /tetes pour tous les joueurs

- Demande de LeKiwi06 : « j'aimerai aussi que les joueurs aient accès au /tetes mais que celui ci leur servent a
  savoir celle qu'ils ont déjà obtenus ( l'avoir eu dans son inventaire via un échange par exemple compte aussi ,
  pas la peine de l'avoir tué soit même ) ».
- KS_Decapitator 1.2.0 : compilé, poussé, non déployé. Détail et choix signalés : `KS_Decapitator/JOURNAL.md`.

## Fusion de deux coffres de récompense

- Demande de LeKiwi06 : « j'aimerai qu'on puisse fusionner 2 coffres de récompenses dans l'enclume ce qui
  additionnerait leurs valeurs , et couterait un nombre de point d'xp égale au nouveau montant en emeraude du
  coffres [...] le coffre s'appellera " coffre de récompense fusionné " , ça permettrait de stacker pleins de
  petits coffres pour essayer d'avoir une grosse récompense sur la longueur ».
- Ses réponses : « le clique droit sur l'enclume ouvre une interface de coffre avec un bouton valider sur la case
  du centre bas et le cout en xp affiché entre parenthèse , ne pas accepter les coffres déjà ouvert » ; contenu :
  « Nouveau tirage ».
- KS_RewardsGUI 1.5.0 : compilé, poussé, non déployé. `fusion.yml` généré hors dépôt et vérifié. Détail et choix
  signalés : `KS_RewardsGUI/JOURNAL.md`.

## /accepter pour les demandes d'échange

- Demande de LeKiwi06 : « il faut pouvoir faire /accepter pour accepter une demande d'échanger , c'est plus court ,
  si le joueur à reçu des demandes d'échange de plusieurs joueurs , ouvrir un menu cliquable pour choisir quelle
  demande accepter ».
- KS_Economy 1.6.0 : compilé, poussé, non déployé. Détail : `KS_Economy/JOURNAL.md`.
- Autre question de la session : blocs de la 26.3 remplacés par d'autres pour les joueurs Bedrock. Cause : Geyser
  (2.11.3) parle Java 26.2, ViaBackwards remplace ce qui n'existe pas en 26.2 ; le passage de Geyser en 26.3
  (GeyserMC/Geyser#6712) était encore ouvert le 11/10/2026. Rien à corriger de notre côté.

## Fonte des pièces d'armure

- Demande de LeKiwi06 : « j'aimerai également que le fait de faire fondre des pieces d'armure donne un lingot et pas
  une pépite , ça redonnerai de l'utilité a cette fonctionnalité ».
- KS_Crafts 1.12.0 : compilé, poussé, non déployé. Détail : `KS_Crafts/JOURNAL.md`.

## Jetons sur Bedrock, recette de la Clé de l’End, usage des badges

- « les joueurs bedrock ne peuvent pas stacker leurs jetons » : `geyser-bedrock/custom_mappings/kalium_objets.json`
  corrigé (pile de 64 déclarée pour les 5 jetons), poussé, non envoyé sur le proxy.
- « je n’ai pas débloqué le craft de la clé de l’end » : la recette est dans les recettes connues de son fichier de
  joueur sur Kixster ; à vérifier dans le livre de recettes d’un établi.
- Usage d’un badge et d’un jeton de localisation expliqué à LeKiwi06 (rien de modifié). Signalé : la description du
  jeton de localisation dit de le ranger dans ses jetons, alors qu’il s’utilise en main (clic droit).

## Description du jeton de localisation

- « oui corrige le texte » (LeKiwi06) : KS_Jetons 2.0.2, « Clic droit, jeton en main, pour l'utiliser ». Compilé,
  poussé, non déployé.

## Malédictions à la meule

- « est-ce qu'on a fait le plugin pour retirer les malédictions des items ? » : non. Puis : « il faut faire
  sneak+click sur une meule avec l'item concerné , cela coute 45 niveaux par malédiction ( interface de
  confirmation avant ) ».
- KS_Meule 1.0.0 (nouveau plugin) : compilé, poussé, non déployé. Détail et limites : `KS_Meule/JOURNAL.md`.
- Revu le même jour, avant déploiement : « fait en sorte que le menu soit uniquement a main nu et qu'on
  selectionne la pièce d'armure dans l'inventaire ( pas le stuff porté ) ». Main vide pour ouvrir le menu, objet
  choisi d'un clic dans l'inventaire.

## Déploiement sur Kixster (11/10/2026, 07:45)

- Accord de LeKiwi06 : « oui déploie tout sur kixster ».
- Envoyés : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0, et `plugins/KS_RewardsGUI/fusion.yml`. Anciens jars dans leurs dossiers `_removed-…`.
- Actifs après redémarrage de Kixster par LeKiwi06. Réservations libérées. Event n'a rien reçu.
- Non envoyé : `kalium_objets.json` sur le proxy (jetons empilables sur Bedrock), en attente de sa décision.

## Reste à faire

- Redémarrer Kixster (LeKiwi06), puis lire la console : « Fonte des armures : 6 recettes du jeu remplacées »,
  « fusion.yml : 516 entrées dans 5 pools, 213 réglages », KS_Meule activé, aucune erreur.
- Tests en jeu : arrivée d'un compte jamais connecté ; `/tetes` sans être opérateur ; fusion de deux coffres
  (Java et Bedrock) ; `/accepter` à trois joueurs ; fonte d'un casque et d'une pioche en fer ; malédiction à la
  meule (accroupi, main vide).
- À décider par LeKiwi06 : envoi de `kalium_objets.json` sur le proxy (et les 5 autres objets empilables) ;
  ces plugins sur Event ; WorldGuard (erreur à la pose d'un bateau), CoreProtect sur Kixster, région
  `ile_du_dragon`, réapparition sans lit sur le toit du spawn.
