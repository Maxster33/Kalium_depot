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

**Compilé, non déployé, non testé en jeu.**
