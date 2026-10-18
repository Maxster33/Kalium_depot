# KS_Didacticiel - journal

Plugin de Kixster : didacticiel des changements apportés au jeu, dans le comparateur « Informations ». Dépend de
KLM_Menu (2.11.0 : `Contenant`, `MenuSection.informations()`).

## 1.0.0 - didacticiel en coffre, contenu dans didacticiel.yml, marque « Nouveau » (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faut que tu me crées un didacticiel qui sera dans le menu information sur kixster, ce
didacticiel permet d'expliquer tous les changements. il faudra donc le mettre à jour régulièrement ». Maquette montrée
avant le code ; ses réponses :

| Question | Réponse de LeKiwi06 |
|---|---|
| Forme | Menu en coffre (un objet par thème, puis un objet par nouveauté, l'explication dans sa description) |
| Mise à jour | Fichier sur le serveur (`didacticiel.yml`), rechargé sans recompiler ni redémarrer |
| Nouveautés récentes | « marque nouveau, jusqu'à ce que la personne l'ait ouvert pour la première fois » |
| Outils du staff | Pas dans le didacticiel |

- **Ouverture** : bouton « Didacticiel » du comparateur « Informations » (`/infos` sur Kixster, où le comparateur
  n'est donné qu'après `/menu on`), premier bouton de joueur (ordre 10, avant « Contacts ») ; aussi `/didacticiel`.
- **Écran des thèmes** (coffre, une rangée par 9 thèmes + barre) : un objet par thème (nom, résumé, nombre de
  nouveautés, « Nouveau : n à découvrir », « Clic : voir le détail ») ; barre : livre « Comment ça marche », « Retour »
  vers « Informations » (porte) ou « Fermer » (barrière, ouvert par `/didacticiel`).
- **Écran d'un thème** : un objet par nouveauté ; sa description = le texte (coupé à environ 35 caractères) et, s'il y
  en a une, « Commande : /... ». « Retour » vers les thèmes.
- **« Nouveau »** : par joueur et par nouveauté. Une nouveauté dont la valeur `nouveau` n'a pas encore été vue par le
  joueur porte « Nouveau » en tête de sa description et brille ; son thème brille et dit combien il en reste. Ouvrir
  le thème enregistre ce que le joueur a vu (`plugins/KS_Didacticiel/vus.yml`, uuid > thème > nouveauté = valeur
  vue) : la marque est encore affichée pendant cette ouverture, puis disparaît. Changer la valeur `nouveau` d'une
  nouveauté (la date du jour) la remet en « Nouveau » pour tous.
- **Contenu** : `plugins/KS_Didacticiel/didacticiel.yml`, créé depuis le jar au premier démarrage ; référence dans le
  dépôt : `src/main/resources/didacticiel.yml` (mode d'emploi en tête du fichier). Titre, thèmes (nom, icône, résumé)
  et nouveautés (nom, icône, texte, commande facultative, nouveau). Icône inconnue : livre et avertissement dans la
  console ; 45 thèmes et 45 nouveautés par thème au plus.
- **`/didacticiel recharger`** (permission `ksdidacticiel.admin`, opérateurs ; aussi depuis la console) : relit le
  fichier et répond « 10 thèmes, 58 nouveautés » (et le nombre d'avertissements).
- **Contenu de la 1.0.0** : tout ce qui est en service sur Kixster au 18/10/2026 (inventaire du même jour, tiré des
  `JOURNAL.md` et de `REPRISE_PROJET.md`), en 10 thèmes et 58 nouveautés : le monde, le butin, crafts et objets,
  enclume / meule / expérience, économie, claims, se déplacer, mort et récompenses, véhicules, menus et fair-play. Pas
  ce qui n'est que sur Event (trains, laisse courte, têtes de la 1.3.0 de KS_Decapitator).
- Textes des menus : `lang.yml` du plugin (créé au premier démarrage).

Choix de Claude, signalés :
- `/didacticiel` (non demandé) : le comparateur n'est pas dans la barre par défaut sur Kixster ; la commande sert aussi
  à `recharger`.
- Au premier déploiement, toutes les nouveautés portent `nouveau: "2026-10-18"` : chaque joueur voit tout en
  « Nouveau » jusqu'à ce qu'il ouvre chaque thème.
- Ordre 10 dans « Informations » ; « Nouveau » marqué à l'ouverture du thème (une nouveauté ne s'ouvre pas : on la
  lit en passant la souris dessus).

Limites :
- Écrit sans serveur de test. Bedrock : description des objets et brillance à vérifier.
- Le texte est coupé aux espaces à environ 35 caractères : une ligne peut être un peu plus longue avec un mot long.
- Une mise à jour du fichier ne fait rien tant que `/didacticiel recharger` n'a pas été tapé (ou le serveur redémarré).

**Mise à jour du contenu** (à chaque déploiement qui change le jeu sur Kixster) : modifier
`src/main/resources/didacticiel.yml` (nouvelle nouveauté ou texte changé, avec la date du jour dans `nouveau`), le
commiter, l'envoyer dans `plugins/KS_Didacticiel/` sur Kixster, puis `/didacticiel recharger`. Le jar n'est à
recompiler que si le code change (le fichier du jar ne sert qu'au premier démarrage).

Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.0.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:01 (LeKiwi06 : « Envoie d'abord KS_Didacticiel 1.0.0 sur Event pour que je teste le menu, Kixster ensuite » ; nouveau ; KLM_Menu 2.11.0 en place ; jar du serveur vérifié de la même taille que celui compilé), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.**

## 1.1.0 - /help, contenu mis à jour (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « /help et /didacticiel (ouvre le didacticiel) ».

- **/help** ouvre le didacticiel, comme /didacticiel (et `/help recharger` pour le staff). Techniquement nécessaire,
  signalé : /help existe déjà (aide du serveur), un alias de plugin.yml perdrait contre elle ; le nom est repris dans la
  table des commandes au démarrage puis au premier tick. L'aide d'origine reste sous `bukkit:help` et `minecraft:help`.
- **Contenu** (`didacticiel.yml`, 60 nouveautés) : /claim et /claims, catalogue des magasins (/magasins, /shop,
  /market, lingot des rachats), /bed, jetons de localisation posés dans l'inventaire spécial (24 h), inventaire
  spécial en un coffre (/jeton, /badge), ce didacticiel (/help) ; ces 7 nouveautés ont `nouveau: "2026-10-18b"` et
  redeviennent « Nouveau » pour qui les avait lues.
- Sur un serveur où la 1.0.0 a déjà créé `plugins/KS_Didacticiel/didacticiel.yml`, **envoyer le nouveau fichier** puis
  `/didacticiel recharger` (le jar ne le remplace pas).

Limites : écrit sans serveur de test. Empilé sur la 1.0.0, pas encore testée en jeu (demande de LeKiwi06).

Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.1.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:44 (LeKiwi06 : « Envoie les cinq plugins sur Event pour que je teste, Kixster ensuite » ; envoi groupé : KS_Jetons 2.1.0, KS_Teleport 1.4.0, KS_Claim 1.5.0, KS_Economy 1.10.0, KS_Didacticiel 1.1.0 ; jars en place vérifiés de la même taille que `jars-deployes/` avant l'envoi, nouveaux jars de la même taille que ceux compilés ; ancien jar dans `_removed-ks_didacticiel-1.0.0/`), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.** `didacticiel.yml` du serveur remplacé par celui de la 1.1.0 (ancien dans le même dossier).

## 1.2.0 - recettes cliquables, nouveautés selon les plugins, thème « Commandes », contenu revu (18/10/2026, LeKiwi06)

Retours de LeKiwi06 sur la 1.1.0 : « être plus évasif sur certains sujets, comme butin rare ou coffre de structure [...]
c'est aux joueurs de découvrir » ; « les têtes de mobs doivent être dans butin » ; « pouvoir cliquer sur crafts
pratiques pour afficher via un menu avec système de pages les crafts en question » ; « dans véhicules parler des trains,
du fonctionnement de chaque type de wagonnets » ; « parler du drop de tête de joueurs en renommant les mobs » ; « dans
fair-play : préciser que le easy place mode de Litematica est interdit, etc » ; « dans monde, inutile de préciser qu'un
joueur sans spawn point arrive au spawn [...] c'est le comportement de base » ; « une catégorie commande [...] une
version liste qu'on puisse consulter, avec une courte description de ce qu'elle fait ».

- **Recettes cliquables** (code) : une nouveauté peut porter `recettes` (clés de recettes du serveur, ou un début suivi
  de `*`). Sa description finit par « Clic : voir les recettes (n) » ; le clic ouvre un coffre, **une recette par
  page** : grille d'établi 3 x 3 (ou l'entrée du four), poste (établi, four, haut fourneau), résultat ; un ingrédient au
  choix porte « Ou une de ses n variantes » ; flèches de page, « Recette n sur total », « Retour » vers le thème.
  « Crafts pratiques » : les 22 recettes de confort de KS_Crafts.
- **Nouveautés selon les plugins** (code) : `requiert` sur un thème ou une nouveauté (`"KS_Trains"`, ou
  `"KS_Decapitator 1.3.0"` pour une version minimale) : sans ce plugin activé (ou dans une version plus ancienne), rien
  n'est montré ni compté. Un thème sans nouveauté montrée disparaît. Permet d'écrire maintenant ce qui n'est que sur
  Event (trains, laisse courte, têtes de joueurs) : ce sera montré sur Kixster quand le plugin y arrivera.
- **Contenu** (`didacticiel.yml`, 11 thèmes, 101 nouveautés) :
  - « Le butin » : textes évasifs (minerais, émeraude, feuilles, fermes, potions, butin rare, coffres de structures,
    pêche) ; **Têtes de mobs** déplacée ici ; **Têtes de joueurs** (« mob secret », sans dire lequel) et **Shulkers de
    couleur** (KS_Decapitator 1.3.0).
  - « Le monde » : « Arrivée et réapparition » retirée.
  - « Crafts et objets » : « Crafts pratiques » cliquable.
  - « Véhicules » : laisse courte (KS_LaisseBateau) et 10 nouveautés sur les trains (KS_Trains) : locomotive,
    attelage, menu, wagon contrôleur, carburant, wagons à entonnoir, à coffre, jukebox, voyageurs, boussole et nom.
  - « Menus et fair-play » : **Mods et astuces interdits** (x-ray, Litematica en « easy place » ou « printer »,
    macros et clics automatiques, mini-cartes qui contournent le radar ou la vue des grottes) ; « Exploration du jour »
    plus évasive.
  - **« Commandes »** (nouveau thème) : 28 commandes, chacune avec une phrase.
  - Les nouveautés ajoutées, déplacées ou changées ont `nouveau: "2026-10-18c"` ; les textes seulement rendus évasifs
    gardent leur valeur (pas de « Nouveau »).
- Choix de Claude, signalés : « Ou une de ses n variantes » pour un ingrédient au choix (la première variante est
  montrée) ; freecam non citée dans les interdits (règle à confirmer par LeKiwi06) ; descriptions des commandes
  /groupe et /bloquer tirées des journaux de KLM_Contacts.
- Textes du code : nouvelles clés `nouveaute.recettes`, `recettes.*`, `retour-theme`.

Limites : écrit sans serveur de test. Empilé sur la 1.1.0, pas encore testée en jeu (demande de LeKiwi06). Les têtes de
joueurs ne marchent que si `tetes-de-joueurs: true` dans le config.yml de KS_Decapitator (false sur Event).

**À envoyer avec son `didacticiel.yml`** (le jar ne remplace pas celui du serveur). Compilé le 18/10/2026
(`sortie/KS_Didacticiel-1.2.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 09:07 (LeKiwi06 : « Oui, envoie KS_Didacticiel 1.2.0 et son didacticiel.yml sur Event » ; jar en place (1.1.0) vérifié de la même taille que `jars-deployes/`, rangé avec l'ancien `didacticiel.yml` dans `_removed-ks_didacticiel-1.1.0/` ; nouveau jar et nouveau fichier de la même taille que ceux du dépôt), actif après redémarrage d'Event (le jar ; le fichier seul serait relu par /didacticiel recharger, mais il demande la 1.2.0 pour recettes et requiert). Kixster pas encore. Statut : non testé en jeu.**

## 1.2.1 - recettes de tous les objets fabriqués (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faut que tous les items ayant un craft possèdent la même interface que celle pour les crafts
pratiques (élixirs, minecart contrôleur, locomotive, spawner, etc) ».

- **Contenu** (`didacticiel.yml`) : 16 nouveautés s'ouvrent sur leurs recettes : crafts pratiques, boîte de shulker,
  armure fondue (6 recettes de four et de haut fourneau), spawners (11), Changeur de Biome, Bedrock Breaker, Clé de
  l'End, tête de wither squelette (5 réparations et le bloc de charbon de bois, dans les deux sens), élixirs (11, de
  KS_Elixir), jeton de localisation (KS_Teleport), bateau à jukebox et bateau à coffre et jukebox (un par bois),
  verrou de véhicule, locomotive, wagon contrôleur, wagon jukebox. Valeurs `nouveau` inchangées (pas de « Nouveau »).
- **Code**, techniquement nécessaire :
  - `*` accepté n'importe où dans une clé (`ks_crafts:*_armure`) ; avant : seulement à la fin ;
  - une lettre de la grille répétée autant de fois que son choix a d'objets montre un objet différent par case : le
    wagon contrôleur affiche ses 5 pièces (comparateur, capteur sculk calibré, lampe à redstone, bloc musical, levier)
    au lieu de 5 comparateurs « ou une de ses 5 variantes ».
- Limite : le jeton de localisation, la tête de wither réparée et les objets dont le résultat est calculé au moment du
  craft montrent le résultat déclaré par la recette (jeton sans magnétite, crâne de wither squelette).

Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.2.1.jar`). **Statut : non déployé, non testé en jeu** (empilé sur la
1.2.0, pas encore testée, demande de LeKiwi06). À envoyer avec son `didacticiel.yml`.

**Déployé sur Event le 18/10/2026 à 15:57 (LeKiwi06 : « Oui, envoie KS_Didacticiel 1.2.1 et son didacticiel.yml sur Event » ; jar en place (1.2.0) vérifié de la même taille que `jars-deployes/`, rangé avec l'ancien `didacticiel.yml` dans `_removed-ks_didacticiel-1.2.0/` ; nouveau jar et nouveau fichier de la même taille que ceux du dépôt), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.**

## Contenu du 18/10/2026 (après la 1.2.1, sans changement de code)

Demande de LeKiwi06 : « oui ajoute les modifications au didacticiel » (KS_Trains 1.4.8, KS_LaisseBateau 1.0.1 et 1.0.2,
faits dans une autre session).

- « Train : le menu » : chaque wagon peut y être renommé et son contenu ouvert depuis le menu (KS_Trains 1.4.8).
- « Train : wagon contrôleur » : le klaxon est un sifflet, ou une corne de chèvre posée dans le contrôleur.
- « Laisse courte » : le bateau remorqué suit comme une remorque et prend les virages derrière le tracteur
  (KS_LaisseBateau 1.0.1 et 1.0.2).
- Ces trois nouveautés ont `nouveau: "2026-10-18d"`. Jar inchangé (1.2.1) : envoyer seulement `didacticiel.yml`, puis
  `/didacticiel recharger` (ou au prochain redémarrage).
- « Mods et astuces interdits » : + « toute freecam autre que Legacy Freecam » (LeKiwi06 : « Seule Legacy Freecam est autorisée »), `nouveau: "2026-10-18d"`.

**Envoyé sur Event le 18/10/2026 à 16:34** (`didacticiel.yml` seul, même taille que celui du dépôt ; l'ancien, celui du 15:57, dans `_removed-ks_didacticiel-contenu-2026-10-18/` ; jar 1.2.1 inchangé, déjà actif d'après le `lang.yml` du serveur réécrit à 16:30). À relire par `/didacticiel recharger`. Kixster pas encore. Statut : non testé en jeu.

## 1.2.2 - pièces du wagon contrôleur et variantes enfin montrées (18/10/2026, LeKiwi06)

Signalé par LeKiwi06 sur Event (1.2.1) : « le minecart contrôleur affiche toujours 5 comparateurs ».

- **Cause** (lue dans le serveur 26.3, `CraftRecipe.toBukkit`) : une recette relue par `recipeIterator()` ne rend plus
  ses ingrédients en `MaterialChoice` mais en `ItemTypeChoice`. La 1.2.1 ne reconnaissait que `MaterialChoice` : elle
  montrait le premier objet partout, sans la ligne « Ou une de ses n variantes » (défaut aussi de la 1.2.0, pour toutes
  les recettes à ingrédient au choix).
- **Correction** : les objets d'un choix sont lus pour `ItemTypeChoice` (clé de chaque type d'objet), `MaterialChoice`
  et `ExactChoice`. Le wagon contrôleur montre ses 5 pièces, une par case ; les ingrédients au choix (cuivres ciré,
  grès, bateaux...) portent de nouveau « Ou une de ses n variantes ».
- Méthodes vérifiées présentes dans l'API du serveur 26.3 (`ItemTypeChoice.itemTypes`, `RegistryKeySet.values`,
  `TypedKey.key`).

Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.2.2.jar`). **Statut : non déployé, non testé en jeu.** Jar seul
(`didacticiel.yml` inchangé).

## 1.3.0 - « Poser une question », réponses du staff (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « ajoute une touche "poser une question" dans le didacticiel, précise que le staff répondra aux
questions posées et modifiera le didacticiel si besoin ». Ses choix : outil staff dans « Modération » et réponse en jeu ;
pas de limite de questions.

- **Bouton « Poser une question »** (livre et plume, au centre de la barre de l'écran des thèmes) : « Une question sur
  Kixster ? Le staff y répondra en jeu et modifiera le didacticiel si besoin. » Clic : fenêtre de saisie (256
  caractères), « Envoyer » ou « Retour » ; après l'envoi : « Question envoyée. Le staff te répondra en jeu (dans le
  tchat, ou à ta prochaine connexion) et modifiera le didacticiel si besoin. », puis retour au didacticiel.
- La question est enregistrée (`plugins/KS_Didacticiel/questions.yml`, numérotée), écrite dans la console et annoncée
  dans le tchat au staff connecté (« [Didacticiel] pseudo pose une question : ... »).
- **Outil « Questions du didacticiel »** (rubrique « Modération » de /menu ; permission `ksdidacticiel.staff`, nouvelle,
  opérateurs par défaut) : coffre des questions en attente (les plus anciennes d'abord : n°, pseudo, date, question) ;
  clic : fenêtre de réponse (la question, un rappel « si la réponse manque au didacticiel, pense à le mettre à jour »,
  champ de 256 caractères). Horloge : questions répondues (réponse, qui, quand, lue ou pas encore). Pages de 45.
- **Réponse** : dans le tchat du joueur (« [Didacticiel] X a répondu à ta question : », la question, la réponse) s'il
  est connecté, sinon 2 secondes après sa prochaine connexion. Deux membres du staff ne peuvent pas répondre à la même
  question (le second est prévenu).
- Textes saisis insérés tels quels (sans mise en forme : un joueur ne peut pas glisser de lien ou de couleur dans les
  messages du staff).
- `didacticiel.yml` : « Ce didacticiel » cite le bouton (`nouveau: "2026-10-18e"`).

Choix de Claude, signalés : pas de commande en plus (l'outil passe par /menu) ; pas de suppression ni de classement sans
réponse ; le staff est prévenu dans le tchat seulement à l'envoi (pas de rappel à la connexion).

Contient la 1.2.2 (jamais déployée). Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.3.0.jar`). **Statut : non déployé,
non testé en jeu.** À envoyer avec son `didacticiel.yml`.

