# KS_MondeTest - journal

Plugin de Kixster (préfixe `KS_`) : un monde de test en créatif, étanche par rapport à la survie. Dépend de KLM_Menu
(boutons de la rubrique Modération, textes). Les autres plugins lisent le côté d'un joueur dans sa métadonnée
`klm_espace` (`kixster` ou `test`), comme avec KLM_Espaces.

## 1.0.0 - monde de test séparé de la survie (25/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « j'utilisais Event comme serveur test avant, mais je ne l'ai plus. Peux-tu me créer une
dimension séparée dans Kixster où je peux tester tout ça en créa, sans risquer de me give par accident des choses sur la
survie ? ». Choix validés le 25/10/2026 : nouveau plugin (KLM_Espaces sépare à la connexion, pas au changement de monde) ;
« un terrain plat conviendra bien » ; « dans la boussole des admins sur Kixster ajouter un bouton "Monde de test" et quand
on est dedans "Retourner sur Kixster" » ; « fais comme pour Kal-Games et Kanvas en séparant tout, y compris le mode
opérateur et le gamemode » ; opérateurs du monde de test : ceux qu'il désigne ; pas de garde-fou sur `/give` en survie
(« je ne suis presque jamais en opérateur sur Kixster »).

- **Monde** `Kixster Test` (`monde`), plat, sans structures, créé au premier démarrage ; bordure de 500 blocs (`bordure`)
  centrée sur son point d'apparition ; règle `keepInventory`.
- **Entrée / sortie** : boutons « Monde de test » et « Retourner sur Kixster » de la rubrique **Modération** du menu
  (`/menu` et boussole ; « Monde de test » visible des testeurs seulement, hors du monde de test) ; aussi `/mondetest`
  (entrer, ou sortir si l'on y est). Sortie : retour à l'endroit quitté en survie ; entrée : dernière position dans le
  monde de test. **On n'entre que par là** : toute autre téléportation vers le monde de test est refusée (`/tp`, `/home`,
  spectateur...). On **sort par n'importe quel moyen** (`/spawn`, `/home`, réapparition...) : l'échange a lieu à chaque
  changement de monde.
- **Tout est séparé** (classe `Etats`, reprise de KLM_Espaces) : inventaire, coffre de l'Ender **et son extension**
  (KS_EC_Extension garde ses 3 lignes du bas et ses cases débloquées dans les données du joueur, échangées aussi),
  expérience, vie, faim, effets, mode de jeu (créatif à la première entrée, `mode-a-l-arrivee`), vol, **statut
  d'opérateur**. L'état du côté quitté va dans `etats/<côté>/<uuid>.yml` ; celui du côté où se trouve le joueur reste
  dans ses données de joueur ; le côté des données en cours est marqué dans le joueur (`ks_mondetest:cote`), donc un
  arrêt imprévu ne mélange jamais les deux. Inventaire ouvert refermé avant l'échange (objet tenu au curseur compris).
  Données du joueur sauvées juste après l'échange.
- **Opérateur** : dans le monde de test, opérateur seulement si LeKiwi06 l'a désigné (`/mondetest ajouter <joueur> op`) ;
  en revenant en survie, le statut d'avant l'entrée est rendu. Arrêt du serveur juste après une entrée : statut de survie
  rétabli à la connexion suivante (`registre.yml`, `op-a-rendre`).
- **Joueurs de l'autre côté cachés** (liste Tab comprise) ; **tchat**, messages d'arrivée, de départ, de mort et de
  progrès limités au même côté (la console voit tout). `/global` (KLM_Chat) reste commun à tout le réseau.
- **Dans le monde de test** :
  - rien ne tombe à la mort, pas de coffre de mort, rien envoyé dans `/rewards` ; réapparition dans le monde de test ;
  - portails refusés (joueurs et entités : ils mèneraient au Nether et à l'End de la survie) ; lit, ancre, `/spawnpoint`
    refusés (une mort en survie y ramènerait) ;
  - commandes et coffres des plugins de `plugins-coupes` refusés : KS_Economy (dépôt, rachats, `/echange`), KS_Jetons,
    KS_RewardsGUI, KS_CoffreMort, KS_Claim, KS_Fly, KS_Parrainage, KS_AntiCheat (invsee), KS_Menu, SimpleClaimSystem,
    WorldGuard, GrimAC ; les alias sont reconnus (`/eco`...) ;
  - étoile de KS_Menu : son menu est refermé aussitôt (KS_Menu l'ouvre même quand le clic est annulé) ;
  - `commandes-coupees` : `/kixster`, `/setspawn` et les commandes qui agissent sur tout le serveur (`op`, `deop`,
    `whitelist`, `ban`, `stop`, `reload`, LuckPerms, `difficulty`, `save-off`, `tick`, `datapack`, `function`...) ;
  - sélecteurs qui atteignent la survie refusés (`@a`, `@e`, `@p`, `@r`, `@n` sans `distance=` ni `dx/dy/dz=`), et
    `execute in`.
- **Des deux côtés** : une commande qui nomme un joueur de l'autre côté (connecté ou non) est refusée (`/give`, `/tp`,
  `/msg`, `/invsee`, `/kaliumgive`...), sauf `commandes-libres` (`/mondetest`, `/global`).
- **Joueur sans accès** déconnecté dans le monde de test : il arrive en survie, avec ses données de survie.
- **Testeurs** : `/mondetest ajouter <joueur> [op]`, `retirer <joueur>`, `liste` (console ou permission
  `ksmondetest.admin`, opérateurs par défaut ; un opérateur du monde de test peut donc les lancer depuis le monde de
  test). Retirer un joueur qui est dans le monde de test le renvoie en survie. Liste dans `acces.yml`.
- **Journal** `plugins/KS_MondeTest/journal.log` : entrées, sorties et par quel moyen, refus, accès donnés ou retirés.
- Textes dans `plugins/KS_MondeTest/lang.yml`.

**À régler au déploiement (Kixster)** :
1. **KLM_Menu 2.12.0** (déjà sur Kanvas, `jars-deployes/KLM_Menu-2.12.0.jar`) à la place de la 2.11.0, avec dans son
   `config.yml` (clé ajoutée à la main) :
   ```yaml
   espaces-interfaces:
     kixster: ["KS_Menu", "KS_Economy", "KS_Jetons", "KS_RewardsGUI", "KS_CoffreMort", "KS_Claim", "KS_Fly", "KS_Parrainage", "KS_AntiCheat"]
   ```
   Sans elle, le menu de Kixster (Économie, Jetons...) reste ouvrable par `/menu` depuis le monde de test (avertissement
   au démarrage de KS_MondeTest).
2. SimpleClaimSystem : monde `Kixster Test` sans claims (`claims-worlds-mode`).
3. Première fois, dans la console : `mondetest ajouter LeKiwi06 op`.

**Limites** :
- Bouton « Invsee » de la rubrique Modération ouvert **depuis la survie** sur un joueur du monde de test : non intercepté
  (seule la commande l'est) ; correctif propre : petite retouche de KS_AntiCheat.
- Une commande qui vise un joueur par son UUID, une fonction de datapack ou un bloc de commande échappent au filtre.
- Un menu déjà ouvert au moment d'entrer reste utilisable jusqu'à sa fermeture.
- Statistiques vanilla, temps de jeu (KLM_Statistiques, parrainage) et alertes de minage de KS_AntiCheat comptent aussi
  dans le monde de test.
- Avant de retirer le plugin du serveur : faire sortir tout le monde du monde de test (sinon inventaire et op du monde de
  test restent).
- `/op` lancé depuis la console sur un joueur qui est dans le monde de test est annulé à sa sortie (statut de survie
  rendu).

**Déployé sur Kixster le 25/10/2026 à 03:10 (accord de LeKiwi06 : « oui, déploie KS_MondeTest 1.0.0 et KLM_Menu 2.12.0
(avec espaces-interfaces) sur Kixster, et règle SimpleClaimSystem pour le monde de test »)**, avec KLM_Menu 2.12.0
(2.11.0 et son `config.yml` dans `_removed-klm_menu-2.11.0/`, clé `espaces-interfaces` ci-dessus ajoutée) et
`plugins/SimpleClaimSystem/config.yml` (`Kixster Test: DISABLED` dans `claims-worlds-mode`, alias « Monde de test » ;
ancien fichier dans `_removed-simpleclaimsystem-config-2026-10-25/`). **Actif après redémarrage (fait par LeKiwi06),
non testé en jeu.**

**Plan de test** (après `mondetest ajouter LeKiwi06 op` dans la console) :
1. En survie : noter inventaire, XP, statut d'op ; poser un objet dans le bas du coffre de l'Ender.
2. Menu › Modération › « Monde de test » : arrivée en créatif, op, inventaire et coffre de l'Ender vides, joueurs de la
   survie invisibles, tchat séparé.
3. Se donner des objets (diamants, `/kaliumgive`), remplir le coffre de l'Ender, mourir : objets gardés, pas de coffre
   de mort.
4. `/economie`, `/jetons`, `/rewards`, étoile du menu de Kixster, portail, `/give <joueur en survie> diamond`, `/op` :
   refusés.
5. « Retourner sur Kixster » : même endroit, inventaire, coffre de l'Ender, XP et statut d'op d'avant, aucun objet du
   monde de test.
6. Revenir dans le monde de test (objets de test retrouvés), puis en sortir par `/spawn` : inventaire de survie rendu.
7. Depuis la survie, `/tp` vers le monde de test refusé ; vérifier `plugins/KS_MondeTest/journal.log`.

## 1.1.0 - « moi de test » (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai)** : « je vois encore mon score de monnaie etc. dans le tableau
à droite, donc ce n'est pas bien séparé ; et l'heure du jour et de la nuit est synchro entre mondetest et kixster [...]
il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la
distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java : « ton option
moi de test nécessite que j'ai 2 comptes Java ? » ; non, identifiant de test automatique) ; progrès, recettes et
statistiques : « rien ne compte en test ».

- **Moi de test** : dans le monde de test, métadonnée `ks_compte` = identifiant du compte de test du joueur
  (`UUID.nameUUIDFromBytes("ks_mondetest:" + uuid)`, version 3, toujours le même) ; retirée en survie. KS_Economy
  1.11.0, KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0 et KS_Fly 1.1.0 y rangent les
  données du joueur (solde, jetons, maisons, récompenses, exploration, vol) ; KS_Tableau 1.1.0 et la liste Tab montrent
  donc le moi de test ; KS_Menu 1.3.0 y masque les boutons Claims et Coffres de mort ; KLM_Statistiques 1.2.0 et
  KS_Parrainage 1.2.0 n'y comptent pas le temps de jeu. Métadonnées retirées un tick après la déconnexion (les autres
  plugins sauvent encore le compte de test pendant le départ).
- **Progrès, recettes et statistiques vanilla** : n'avancent pas dans le monde de test (critères de progrès, recettes
  découvertes et statistiques annulés) ; ceux de Kixster restent intacts.
- `plugins-coupes` (nouveau `config.yml`) : KS_CoffreMort, KS_Claim, KS_Parrainage, KS_AntiCheat, SimpleClaimSystem,
  WorldGuard, GrimAC. KS_Economy, KS_Jetons, KS_RewardsGUI, KS_Fly et KS_Menu n'y sont plus (moi de test) ; `kixster`
  retiré de `commandes-coupees`. L'étoile de KS_Menu n'est refermée que si KS_Menu est dans `plugins-coupes`.
- **Heure** : déjà séparée côté serveur, rien à changer. Paper 26.3 donne une horloge à chaque monde
  (`time.affects-all-worlds: false` dans `paper-global.yml` de Kixster ; `world_clocks.dat` propre à
  `dimensions/minecraft/kixster_test/`) ; `/time` ne change que l'horloge du monde où on le tape. Vérifié le 25/10 à
  11:08 : monde de test à 4 429 ticks, Kixster à 12 205 (sauvegardés au même instant). Le 25/10 à 04:35:52,
  `/time set night` dans le monde de test puis retour sur Kixster à 04:35:54 : il y faisait déjà nuit (vers 19 400).

**Au déploiement** :
1. Nouveau `plugins/KS_MondeTest/config.yml` (l'ancien dans `_removed-ks_mondetest-1.0.0/`) : les listes de la 1.0.0
   couperaient encore l'économie, les jetons et les récompenses.
2. `config.yml` de KLM_Menu : `espaces-interfaces: kixster: ["KS_CoffreMort", "KS_Claim", "KS_Parrainage", "KS_AntiCheat"]`.
3. Tous les plugins de la liste ci-dessus ensemble (et KS_BiomeChanger 1.2.0, exigé par KS_Economy depuis la 1.10.1).

**Limites** : les comptes de test apparaissent dans les fichiers des plugins (`comptes.yml`, `jetons.yml`...) sous leur
identifiant de test ; les magasins de Kixster ne sont pas testables depuis le monde de test (refusés : stock et gains
réels) ; l'écran des statistiques montre celles de Kixster.

**Compilé, non déployé, non testé en jeu.**
