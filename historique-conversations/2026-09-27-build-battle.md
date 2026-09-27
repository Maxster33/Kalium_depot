# 2026-09-27 — Build Battle (KG_BuildBattle + KV_BuildBattle), points de chute et glace sur Kanvas

- Plugin(s) concerné(s) : KG_BuildBattle et KV_BuildBattle (nouveaux), KV_Plots, KLM_Menu et KLM_Portal (déploiement
  seulement) ; serveurs Kanvas et Kal-Games. Session du 27/09/2026 (9 h 47) au 28/09/2026 (1 h 30), LeKiwi06.
- Versions avant / après : KG_BuildBattle — → 0.1.0 ; KV_BuildBattle — → 0.3.5 ; KV_Plots 1.4.0 → 1.4.1 ; sur Kanvas,
  KLM_Menu 2.0.0 → 2.3.0 et KLM_Portal — → 1.2.0.

## Demandé
- « Ajouter des nouveaux mini-jeux, commençons par le Build Battle. Il se trouvera sur Kanvas même s'il est accessible
  uniquement depuis Kal-Games », en créatif avec WorldEdit, avec des règles proches des plots ; réutiliser le vote et
  la « fin de concours » de KV_Plots.
- Réponses au cahier des charges : tempos fast 3 / normal 5 / longue 10 / extra 30 min ; équipes de 1 à 4 ; file
  publique (tempo normal, solo / duo / trio / squad) et parties privées (réglées par le créateur) ; vote parmi 5 thèmes,
  mode privé « thèmes écrits » (64 caractères, 1 min d'écriture, vote de 30 s, poudre de blaze pour signaler un thème
  inapproprié : message au staff + fichier) ; vote de 30 s par terrain qui passe à 3 s quand tout le monde a voté ;
  compte à rebours de 30 s dès 2 équipes ; 8 équipes max ; salle d'attente sur Kanvas, capturée « comme pour le
  Bingo », une copie par partie ; « les boîtes sont identiques, fais 4 colonnes de 8 espacées pour ne pas voir les
  pseudos des autres parties » ; « tu n'auras besoin que de vider la zone constructible » ; points et classements plus
  tard.
- « La partie plugin à mettre sur kal-games comme pour le Bingo, que KG_Menu appelle l'interface. »
- Retours de test : point de chute vers Kanvas ignoré ; la glace de l'arène fondait ; infos en barre du boss au lieu de
  la barre d'action ; créatif dans la salle d'attente (« rends-nous invincibles ») ; 200 thèmes diversifiés et ouverts
  (liste revue trois fois puis validée) ; mobs avec IA ; arbre qui dépasse de la zone ; mobs restés d'une partie à
  l'autre. « Parfait, tu peux tout valider et clôturer. »

## Fait
- Cahier des charges `KV_BuildBattle/CAHIER_DES_CHARGES.md` ; KV_BuildBattle 0.1.0 → 0.3.5 et KG_BuildBattle 0.1.0
  codés, déployés (WinSCP en ligne de commande, sessions enregistrées) et validés par LeKiwi06 ; KV_Plots 1.4.1 ;
  KLM_Menu 2.3.0 + KLM_Portal 1.2.0 installés sur Kanvas (point de chute, boussole, étoile validés).
- `relay-token` rempli à la main par LeKiwi06 (KG_BuildBattle, KV_BuildBattle, KLM_Portal de Kanvas et du lobby).

## Décisions
- Deux plugins (un rôle chacun) : KG_BuildBattle (kal-games) et KV_BuildBattle (Kanvas). Liaison « comme le Bingo » :
  file et parties gérées par KG_BuildBattle, moteur de KalGames (futur KG_Instances) non utilisé (il ne gère que des
  arènes collées sur kal-games).
- Monde à part `buildbattle` ; une boîte capturée recopiée en 4 colonnes de 8 ; une colonne = une partie de 2 à 8
  équipes.
- Choix par défaut de Claude acceptés : équipes tirées au hasard ; égalité du vote de thème = tirage au sort ; terrain
  vide présenté au vote ; note changeable pendant les 30 s ; 15 s de résultats ; pas de reconnexion à une partie ;
  zone remise « comme capturée » (puis boîte entière en 0.3.4, pour effacer ce qui dépasse) ; point d'apparition de
  l'équipe = point de vue au vote ; pas de limite de mobs pour l'instant.

## Reste à faire
- Étape 4 : points et classements du Build Battle (KG_ScoreBoards).
- Limite de mobs par zone ; retrait de l'emplacement « Build Battle » de KalGames ; boîtes non remises à neuf si le
  serveur s'arrête en pleine partie.
- Nettoyage des `_removed-kv_buildbattle-0.1.0/`, `0.2.0/`, `0.3.0/`, `0.3.1/` sur Kanvas (par l'humain).
