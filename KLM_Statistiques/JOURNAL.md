# JOURNAL — KLM_Statistiques

Plugin Paper (peut aller sur chaque serveur ; Kixster d'abord) : statistiques vanilla du joueur sur ce serveur, dans le
comparateur « Informations ».

## 1.0.0 - création (23/10/2026, LeKiwi06)

**Demande de LeKiwi06 (23/10/2026)**, avec le parrainage : « stats de temps de jeu visibles pour les joueurs Bedrock »,
puis « toutes les stats vanilla, dans l'onglet Information, rajouter une catégorie Statistiques (peut-on récupérer les
stats de façon rétroactive pour les joueurs Bedrock ?), mettre aussi cette interface pour les joueurs Java ». Maquette
validée le même jour.

- Rétroactif : rien à reconstituer. Le serveur compte les statistiques de tous les joueurs, Bedrock compris
  (`world/stats/<uuid>.json`, depuis leur première venue) ; le menu les lit telles quelles. Ce sont celles du serveur
  où l'on est.
- Bouton « Statistiques » du comparateur « Informations » (KLM_Menu 2.10.0 ou plus) et `/statistiques` (`/stats`).
- Menu en coffre : rangée du haut = onglets de l'écran Statistiques de Java (Général ; Minés, Fabriqués, Utilisés,
  Cassés, Ramassés, Jetés ; Créatures tuées, Tué par) ; 36 statistiques non nulles par page ; « Trier » (du plus grand
  au plus petit, ou ordre alphabétique) ; Général commence par le temps de jeu.
- Noms des statistiques, des objets et des créatures : clés de traduction du jeu (affichées dans la langue du joueur ;
  Geyser les traduit pour Bedrock). Durées en min / h / j, distances en m / km, dégâts comme l'écran de Java (dixièmes).

**Compilé, non déployé, non testé en jeu.** Indépendant des autres plugins (seulement KLM_Menu). À vérifier au premier
test : noms traduits côté Bedrock.

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé) ; actif après redémarrage, non testé en jeu.**
