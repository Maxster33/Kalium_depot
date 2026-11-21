# KS_AntiCheat - journal

Plugin du serveur Event : anti-triche. Cahier des charges : catégorie 6 « Anti-triche » de LeKiwi06 (validé le
30/09/2026 ; interface dans la rubrique « Modération » de /menu, précision du 03/10/2026 ; copié dans
`CAHIER_DES_CHARGES.md` au déploiement). Codé en 5 étapes (versions 0.x), déployé à la fin.

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## Étape 3 (duplication) : reportée (03/10/2026)

Décision de LeKiwi06 : l'identifiant caché dans les objets de valeur empêcherait nos recettes et boutiques (KS_Crafts,
KS_Elixir, magasins) de reconnaître les objets marqués ; la détection de duplication est reportée. En attendant : les
réglages anti-duplication de Paper (étape 5).

## 1.3.0 - fiche d'un joueur : économie, maisons, jetons et récompenses, téléportation (09/10/2026, LeKiwi06)

Suite de la 1.2.0 : le « etc. » de la demande, précisé par LeKiwi06 (choix dans une liste, puis « vas-y pour la
suite ») : économie (solde, magasin, dernières ventes et échanges), maisons et téléportation (ses /home, se téléporter à
un de ses claims ou à une maison), jetons et récompenses (inventaire spécial, récompenses en attente, coffres de mort).
Empilé sur la 1.2.0 non testée, à sa demande.

Nouveaux boutons de la fiche d'un joueur (en ligne ou hors ligne), tous en lecture seule :
- **« Économie »** : solde (et s'il est masqué), magasin (nom, position, nombre de boutiques, points en attente dans
  les boutiques), ventes de ses boutiques (nombre, lots des 7 derniers jours, points gagnés), ses 5 dernières ventes,
  ses 5 derniers achats en boutique, ses 5 derniers `/echange`.
- **« Maisons »** : lit (point de réapparition enregistré), maison du spawn, emplacements de localisation : position
  de chacun, et un bouton par destination pour s'y téléporter.
- **« Jetons et récompenses »** : jetons de l'inventaire spécial (un nombre par type), badges portés (niveau),
  récompenses en attente dans `/rewards` (nombre ; pour les 10 plus récentes : date, origine, raison, nombre
  d'éléments), coffres de mort actifs (position, piles d'objets, minutes restantes) avec un bouton par coffre pour s'y
  téléporter.
- **« Claims »** : un bouton par claim pour s'y téléporter (au point enregistré par SimpleClaimSystem pour ce claim).
- **Téléportation du staff** : immédiate, sans jeton ni délai ; chaque téléportation est notée dans la console (qui, où).
- **Journal des échanges** (nécessaire, non demandé tel quel) : KS_Economy ne garde aucune trace des `/echange` ;
  KS_AntiCheat les note maintenant dans `plugins/KS_AntiCheat/echanges.log` (date, les deux joueurs, ce que chacun a
  donné). Les échanges d'avant cette version ne sont donc pas affichés.
- Lecture des autres plugins : `Infos` ; `softdepend` KS_Teleport, KS_CoffreMort, KS_Jetons en plus. Un plugin absent :
  pas de bouton ou pas de ligne ; trop ancien : un message le dit.

À déployer ensemble : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 (sans les quatre derniers, la fiche de KS_AntiCheat 1.3.0 s'ouvre mais en montre moins).

Limites :
- Récompenses en attente : leur contenu (les objets) n'est pas montré.
- Inventaire spécial : des nombres et des niveaux, pas la vue du contenant.
- Arrivée d'une téléportation non vérifiée (le staff peut arriver dans un bloc ou dans le vide si le lieu a changé) ;
  lit : position enregistrée, même si le lit a été cassé.
- À vérifier en jeu : le point d'arrivée d'un claim (celui que SimpleClaimSystem enregistre à sa création).

**Déployé sur Kixster le 09/10/2026 à 18:31 (LeKiwi06, envoi groupé : KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 ; accord de LeKiwi06 : « Oui, sur Kixster » ; 1.2.0 dans `_removed-ks_anticheat-1.2.0/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (KS_AntiCheat 1.1.1). Statut : non testé en jeu.**

**Journal de Kixster lu après le redémarrage de 18:36 (09/10/2026) : activé sans erreur, avec KS_AntiCheat 1.3.0, KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2 et KS_RewardsGUI 1.4.1 ; KS_AntiCheat démarre après eux. Personne n'avait encore ouvert la fiche d'un joueur : reste à tester en jeu.**

## 1.4.0 - monde de test exclu, Timer et TimerLimit plus « lourdes » (30/10/2026, LeKiwi06)

LeKiwi06 a été suspendu automatiquement le 30/10 à 22:14 pendant un test : « GrimAC TimerLimit : niveau 100 (seuil
100) ». Le niveau est passé de 62 à 100 en une fraction de seconde (rafale après un pic de lag). « Timer » dans
`grimac.lourdes` était comparé au début du nom et prenait donc TimerLimit. LeKiwi06 avait déjà 16 alertes légères
Timer / TimerLimit depuis le 09/10 : ces vérifications réagissent au lag. Autre constat : `plugins-coupes` de
KS_MondeTest coupe les commandes de KS_AntiCheat, pas ses détections ; le soir même, chaque kit_test jeté à un testeur
a donné une alerte « Revente ? » (une quarantaine).

- **Monde de test** (`enTest` : métadonnée `ks_compte` de KS_MondeTest, ou monde `monde-test`, « Kixster Test » par
  défaut) : aucun signalement GrimAC, aucune macro, aucun x-ray, aucun don ni échange ni récompense compté (et
  `/echange` pas noté dans `echanges.log`), aucune mort d'entité notée dans `morts.log`. L'expulsion AFK reste.
- **Vérifications lourdes** comparées sur le **nom exact** ; `Timer` retirée de la liste par défaut et du `config.yml`
  de Kixster (Timer et TimerLimit restent des alertes légères au bout de 10 signalements en 10 minutes).

**Déployé sur Kixster le 30/10/2026 à 22:20 (LeKiwi06 : « Oui, les deux ») : jar 1.4.0, `config.yml` du serveur
(ligne `lourdes` seulement) et `suspensions.yml` vidé (suspension de LeKiwi06 levée) ; 1.3.0, ancien `config.yml` et
ancien `suspensions.yml` dans `_removed-ks_anticheat-1.3.0/`. Actif au redémarrage de Kixster : d'ici là, la
suspension reste en mémoire. Statut : non testé.**

**Journal de Kixster lu après le redémarrage du 31/10 à 10:26 : 1.4.0 chargée, signalements de GrimAC repris, `suspensions.yml` vide ; LeKiwi06 reconnecté à 10:27 (suspension levée). Les alertes « récompense jamais récupérée » de joueurs jamais revenus portent le nom « ? ».**

## 1.4.1 - x-ray en alertes seulement (31/10/2026, LeKiwi06)

LeKiwi06 : « pour les suspicions de revente etc, ne jamais suspendre la personne, simplement alerter les admins ». La
revente et les macros n'ont jamais suspendu. Le x-ray suspendait à 3 heures extrêmes sur 6 ; sur Kixster, les heures
extrêmes viennent surtout de joueurs Bedrock (.PatientLime2170 à 2 sur 3). LeKiwi06 a choisi « alertes seulement ».

- **X-ray** : 3 heures extrêmes sur 6 donnent une **alerte grave** (rouge, au staff connecté et dans l'historique),
  plus de suspension automatique.
- Seule suspension automatique restante : GrimAC lourd (Reach, Hitboxes, FastBreak, FarBreak, MultiBreak, niveau
  100). GrimAC ne vérifie pas les joueurs Bedrock (base de Kixster : 8 joueurs Bedrock, aucune violation).

**Déployée sur Kixster le 31/10/2026 à 10:39 (LeKiwi06 : « Oui, envoie-la ») ; 1.4.0 dans `_removed-ks_anticheat-1.4.0/` ; `config.yml` et `suspensions.yml` inchangés. Active au redémarrage de Kixster. Statut : non testée.**

## 1.5.0 - fusée Hyperboost : GrimAC ignoré pendant la poussée (02/11/2026, LeKiwi06)

Demande (chantier Kixster du 01-02/11, fusée Hyperboost) : ne pas sanctionner un joueur dont la monture est poussée à
40 blocs/s par KS_Hyperboost. Empilée sur la 1.4.1 non testée à la demande du chantier.

- Pendant une poussée et 5 s après (métadonnée `ks_hyperboost` du joueur : date de fin en ms, posée par
  KS_Hyperboost), chaque signalement de GrimAC est **annulé** (`setCancelled(true)` sur son événement : pas d'alerte de
  GrimAC, pas de violation comptée, donc pas de renvoi en arrière qui éjecterait les passagers) et **non compté** par
  KS_AntiCheat (ni alerte, ni suspension).
- Sans KS_Hyperboost : rien ne change.

**Compilé, non déployé. Statut : non testé en jeu. À déployer avec KS_Hyperboost 1.0.0.**
