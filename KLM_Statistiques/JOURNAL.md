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

## 1.1.0 - temps de jeu actif (23/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (23/10/2026, après le déploiement)** : « j'ai voulu parrainer un joueur, ça me dit que Kixster n'a pas encore envoyé la liste blanche (j'aimerais que les joueurs déjà whitelist puissent prendre un parrain, pour qu'on puisse rattraper les récompenses qu'on aurait dû avoir) ; utilise le terme whitelist au lieu de liste blanche d'ailleurs » ; « j'aimerais aussi que les heures AFK sur le serveur ne comptent pas (considérer un joueur comme AFK s'il n'effectue pas d'actions pendant plus de 5 minutes et enlever l'AFK quand une action est effectuée) ». Ses choix : rattrapage sur le temps vanilla (CoreProtect, prévu d'abord, ne tourne pas sur Kixster en 26.3), temps actif aussi dans Statistiques.

- « Temps de jeu actif (sans AFK), depuis le <date> » en tête de l'onglet Général : compté par ce plugin depuis son installation, minute par minute ; AFK après 5 minutes sans action (tourner la tête, parler, commande, casser, poser ou utiliser un bloc), fin de l'AFK à la première action. Le temps vanilla (AFK compris) reste juste après.
- Données : `plugins/KLM_Statistiques/temps-actif.yml` (date de début, secondes par joueur), sauvé toutes les 5 minutes et à l'arrêt.

**Compilé, non déployé, non testé en jeu.** À déployer ensemble : KaliumRelay 1.10.1 (proxy), KLM_Contacts 1.5.1 (tous les serveurs Paper), KS_Parrainage 1.1.0 + KLM_Statistiques 1.1.0 + `didacticiel.yml` (Kixster). Empilé sur les versions du 23/10 (déployées, pas encore testées), à la suite de la même demande.

**Déployé sur Kixster le 23/10/2026 à 18:50 (accord de LeKiwi06) ; 1.0.0 dans `_removed-klm_statistiques-1.0.0/` ; le temps actif est compté à partir de son premier démarrage ; non testé en jeu.**

## 1.1.1 - noms et valeurs des statistiques générales, icônes (23/10/2026, LeKiwi06)

**Signalé par LeKiwi06 (23/10/2026, capture d'écran)** : « l'onglet statistique présente plusieurs problèmes » : le
temps de jeu s'affichait « stat.minecraft.play_one_minute » avec « 3 576 443 », et la plupart des cases étaient des
feuilles de papier.

- Cause : le nom des statistiques donné par Bukkit (`Statistic.getKey()`) n'est pas celui du jeu pour une vingtaine
  d'entre elles (PLAY_ONE_MINUTE = `play_time`, CHEST_OPENED = `open_chest`, DROP_COUNT = `drop`...) : clé de
  traduction introuvable (nom brut affiché) et temps de jeu non reconnu (affiché en ticks).
- Table des 77 statistiques générales de Bukkit : nom dans le jeu, relevé dans le fichier de langue du client 26.3
  (`assets/minecraft/lang/en_us.json`, 78 clés `stat.minecraft.*` ; `sleep_in_straw_bed` n'existe pas dans Bukkit),
  et une icône pour chacune (plus de papier).
- Durées, distances et dégâts reconnus par le nom du jeu.

**Compilé, non déployé, non testé en jeu.**

**Déployé sur Kixster le 24/10/2026 (accord de LeKiwi06 : « envoie KLM_Statistiques 1.1.1 sur Kixster ») ; 1.1.0 dans `_removed-klm_statistiques-1.1.0/` ; actif après redémarrage, non testé en jeu.**

## 1.2.0 - temps de jeu du monde de test non compté (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- Le temps passé dans le monde de test de Kixster (métadonnée `ks_compte` de KS_MondeTest) ne compte pas dans le temps de jeu actif. Rien ne change sur les autres serveurs.

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
