# KS_RewardsGUI - journal

Plugin du serveur Event : réception des récompenses des autres serveurs. Cahier des charges : catégorie 4
« Récompenses » de LeKiwi06 (validé le 30/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.7.0 - fusion dans l'enclume en coffre de KS_Enclume (25/10/2026, LeKiwi06)

Demande de LeKiwi06 (voir `KS_Enclume/JOURNAL.md`, 1.4.0) : le menu de fusion s'ouvre accroupi + clic sur une enclume
et sert aussi d'enclume ordinaire (bugs de l'enclume pour les joueurs Bedrock) ; coffres posés dans les cases ; usure de
12 % à chaque validation, fusion comprise. Le menu est dans KS_Enclume 1.4.0 ; ce plugin garde les règles de la fusion.

- **Menu « Fusion de coffres » retiré** (il ne s'ouvre plus au clic droit sur une enclume, coffre en main). Les coffres
  se fusionnent dans l'enclume en coffre de KS_Enclume : deux coffres non ouverts posés dans ses cases.
- Clic droit sur une enclume, coffre de récompense en main : sans s'accroupir, le coffre ne s'ouvre pas et un message
  rappelle « Accroupis-toi et clique sur l'enclume pour fusionner des coffres. » (nouvelle clé `fusion.accroupi`) ;
  accroupi, le clic est laissé à KS_Enclume.
- Règles inchangées : coffres refusés (déjà ouvert, sans valeur moyenne, inconnu), coût = valeur du coffre fusionné en
  points d'expérience (retirés par KS_Enclume), nouveau tirage dans `fusion.yml`, ligne « FUSION » de
  `recuperations.log`. Nouveaux refus : un coffre avec un autre objet (`fusion.refus-autre`), deux fois le même coffre
  (`fusion.refus-meme`).
- API pour KS_Enclume : `estCoffre`, `refusFusion`, `apercuFusion` (image du coffre fusionné avant la fusion),
  `coutFusion`, `fusionner` (renvoie le coffre fusionné ; c'est KS_Enclume qui remplace les deux coffres de ses cases).
- Clés `fusion.titre`, `fusion.aide*`, `fusion.attente*`, `fusion.valider*`, `fusion.xp*`, `fusion.complet`,
  `fusion.absent`, `fusion.fait`, `fusion.manque`, `fusion.cout`, `fusion.resultat` de `lang.yml` : ne servent plus.

**Compilé le 25/10/2026, non déployé, non testé en jeu. À déployer sur Kixster avec KS_Enclume 1.4.0 (sans elle, les
coffres ne se fusionnent plus).**

## 1.8.0 - confirmation avant d'ouvrir un coffre de plus de 100 émeraudes (25/10/2026, LeKiwi06)

Demande de LeKiwi06 : « met une sécurité sur les coffres d'une valeur supérieur à 100 emeraude, ou on doit valider
l'ouverture pour que ça ouvre bien, j'ai eu plusieurs fois le soucis en voulant poser mon coffres fusionné dans mon
coffre, ça ouvrait mon coffre de récompense au lieu du coffre que je visais ».

- Clic droit avec un coffre de récompense **non ouvert** de **plus de 100 émeraudes** de valeur moyenne : au lieu de
  s'ouvrir, il montre un petit coffre « Ouvrir ce coffre ? » : « Ouvrir » (béton vert, à gauche ; rappelle qu'un coffre
  ouvert ne peut plus être fusionné ni revendu comme « Non ouvert »), l'image du coffre au centre, « Annuler » (béton
  rouge, à droite : le coffre reste fermé). Fermer le petit coffre revient à annuler.
- Coffres de 100 émeraudes ou moins, coffres déjà ouverts, coffres sans valeur connue : inchangés (ouverture directe).
- Nouvelles clés `confirmation.*` de `lang.yml`. Seuil fixé dans le code (`SEUIL_CONFIRMATION`).
- Contient la 1.7.0, jamais déployée.

Choix de Claude, signalés : seuil strict (101 émeraudes et plus) ; seulement à la première ouverture (un coffre déjà
ouvert n'a plus rien à protéger).

- **Conteneur visé** (accord de LeKiwi06 : « clic droit sur un coffre, tonneau ou autre conteneur avec un coffre de
  récompense en main, c'est le bloc visé qui s'ouvre ») : sans s'accroupir, un clic droit sur un bloc à inventaire
  (coffre, coffre piégé, tonneau, boîte de shulker, entonnoir, four, distributeur...) ou sur un coffre de l'Ender ouvre
  ce bloc, comme avec n'importe quel objet en main ; le coffre de récompense ne s'ouvre pas (et ne se pose toujours
  pas). Accroupi, le jeu n'ouvre pas le bloc : le coffre de récompense s'ouvre (avec la confirmation au-delà de 100
  émeraudes). Les protections (claims) s'appliquent comme pour tout clic sur un conteneur.

**Déployé sur Kixster le 25/10/2026 à 03:01 (LeKiwi06, avec KS_Enclume 1.4.0 et le `didacticiel.yml` ; accord de LeKiwi06 ; 1.6.0 dans
`_removed-ks_rewardsgui-1.6.0/` ; jar en place vérifié identique à la référence avant l'envoi ; `config.yml` et
`fusion.yml` non touchés), actif après redémarrage de Kixster. Contient la 1.7.0. Statut : non testé en jeu.**

## 1.9.0 - outils des opérateurs dans /rewards : don de coffres, historique (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026)** : « un bouton spécial dans le /rewards dédié aux opérateurs pour donner des
coffres de récompenses aux joueurs ? comme variable il faut : nom du joueur, nom du coffre, valeur du coffre (c'est
pour rétribuer les joueurs qui trouvent des bugs, [...] des récompenses d'événement etc.) ; il faut aussi y inclure un
historique de quelle personne a donné quoi à qui ». Maquette validée le même jour (« La maquette me va »).

- **Bouton « Outils des opérateurs »** (bloc de commande, case 52 de la barre de /rewards), visible avec la permission
  `ksrewardsgui.staff` (opérateurs par défaut). Petit coffre : « Donner un coffre », « Historique des dons »,
  « Coffres de mort actifs » (si KS_CoffreMort 1.1.0 est là : son menu `/coffres tous`, avec « Retour » vers ici),
  « Retour » vers /rewards.
- **Donner un coffre** : petite fenêtre avec trois champs : joueur, nom du coffre (40 caractères), valeur moyenne en
  émeraudes (virgule acceptée). Refus avec message, puis retour à la fenêtre avec ce qui a été tapé : joueur jamais
  connecté à ce serveur, nom vide, valeur hors de ]0 ; 10 000]. Puis confirmation verte / rouge (aperçu du coffre).
  Le joueur reçoit dans /rewards, en ligne ou non, un coffre « Non ouvert » nommé « Coffre : <nom> », origine
  « Staff », contenu tiré avec les tables de `fusion.yml` (`deposerCoffre`, comme le parrainage) ; message s'il est en
  ligne. Sans `fusion.yml` (ou vide) : refusé, rien n'est enregistré.
- **Historique des dons** : `plugins/KS_RewardsGUI/dons.yml` (date, opérateur, joueur et UUID, nom, valeur), affiché
  du plus récent au plus ancien (45 par page). Chaque don est aussi écrit dans `recuperations.log` (ligne « DON ») et
  dans la console.
- Plafond de 10 000 émeraudes (non demandé, techniquement nécessaire) : le nombre de tirages d'un coffre croît avec sa
  valeur, un coffre énorme ralentirait le serveur au moment du don.
- Menus en coffre de la classe commune `fr.kalium.menu.api.Contenant` (KLM_Menu 2.11.0 et plus) ; /rewards garde sa
  propre classe. Aucune nouvelle clé de `config.yml` ; nouveaux textes `staff.*` dans `lang.yml`.
- KS_CoffreMort est appelé sans dépendance à la compilation (il dépend déjà de KS_RewardsGUI). Avec un KS_CoffreMort
  plus ancien que 1.1.0, « Coffres de mort actifs » lance `/coffres tous`, qui n'ouvre alors que les coffres de
  l'opérateur lui-même.

À déployer avec KS_CoffreMort 1.1.0 (sinon pas de menu des coffres de mort). `fusion.yml` est déjà sur Kixster.

**Compilé, non déployé, non testé en jeu.**

**Déployé sur Kixster le 25/10/2026 à 03:09 (LeKiwi06, avec KS_CoffreMort 1.1.0 ; 1.8.0 dans `_removed-ks_rewardsgui-1.8.0/` ; jar en place vérifié identique à la référence avant l'envoi), actif après redémarrage de Kixster. `config.yml` du serveur non touché. Statut : non testé en jeu.**

## 1.10.0 - moi de test : /rewards séparé (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- `/rewards` dans le monde de test : récompenses du compte de test (liste, récupération, message à la connexion).
- Les dépôts des autres plugins (coffres de mort, parrainage, autres serveurs) vont toujours au compte de Kixster.
- Outils des opérateurs depuis le monde de test : don de coffre seulement à un joueur qui s'y trouve (compte de test) ; coffres de mort refusés (survie).

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
