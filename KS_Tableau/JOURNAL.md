# KS_Tableau - journal

Plugin du serveur Event : tableau sur le côté de l'écran. Demande de LeKiwi06 (revue du 03/10/2026 : « fais en sorte
qu'on puisse l'activer via commande, il faut aussi afficher les coordonnées du joueur et le biome dans lequel il se
trouve »).

## 1.0.0 - tableau latéral (03/10/2026)

- `/tableau` (ou `/tableau on` | `off`) : affiche ou masque le tableau ; **masqué par défaut** ; le choix est gardé
  (`plugins/KS_Tableau/tableau.yml`).
- Titre « Event » ; lignes : score (KS_Economy), « Exploration journalière : n / 10 » (KS_FairPlay 1.0.3), nombre de
  claims (SimpleClaimSystem, relu toutes les 30 s), coordonnées X Y Z, biome (nom traduit par le jeu du joueur).
  Mis à jour chaque seconde ; une ligne dont le plugin manque n'est pas affichée.
- `depend` KLM_Menu ; `softdepend` KS_Economy, KS_FairPlay, SimpleClaimSystem.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.0.0 ; nouveau), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.0.1 - ligne « Exploration : n/10 » (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « essaie aussi de reduire tout les sidebar trop long [...] pour kixster " exploration journalière
: 10 / 10 " est bien trop long " exploration : 0/10 " suffit ».

- Ligne du tableau : « Exploration : 3/10 » (avant : « Exploration journalière : 3 / 10 »). Nouvelle clé de `lang.yml`
  (`ligne-exploration-courte`) : l'ancien texte reste dans le fichier des serveurs sous `ligne-exploration`, qui ne
  sert plus.
- Les autres lignes ne changent pas (score, claims, coordonnées, biome) : aucune n'est plus longue que celle-ci. Le
  message du tchat de KS_FairPlay (« Exploration journalière : n / 10 ») n'est pas un tableau : inchangé.

**Déployé sur Kixster le 11/10/2026 à 09:00 (LeKiwi06, envoi groupé : KS_Meule 1.1.0, KS_KaliumGive 1.10.0, KS_Economy 1.7.0, KS_Tableau 1.0.1 et `fusion.yml` de KS_RewardsGUI ; accord de LeKiwi06 : « oui déploie tout » ; 1.0.0 dans `_removed-ks_tableau-1.0.0/`), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 1.0.0). Statut : non testé en jeu.**

## 1.1.0 - monde de test : score et exploration du moi de test, sans claims (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- Score et exploration : lus dans KS_Economy 1.11.0 et KS_FairPlay 1.1.0, ils sont ceux du compte de test dans le monde de test. Ligne des claims masquée dans le monde de test (aucun claim n'y existe).

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
