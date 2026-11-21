# KS_Didacticiel - journal

Plugin de Kixster : didacticiel des changements apportés au jeu, dans le comparateur « Informations ». Dépend de
KLM_Menu (2.11.0 : `Contenant`, `MenuSection.informations()`).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

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

- Retour de LeKiwi06 : « les équipements au four n'ont pas besoin de montrer leur craft, tout est déjà expliqué dans le titre du sujet » : « Armure fondue » n'a plus de recettes (15 nouveautés cliquables).

Contient la 1.2.2 (jamais déployée). Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.3.0.jar`). **Statut : non déployé,
non testé en jeu.** À envoyer avec son `didacticiel.yml`.

**Déployé sur Event le 18/10/2026 à 17:16 (LeKiwi06 : « Oui, envoie KS_Didacticiel 1.3.0 et son didacticiel.yml sur Event » ; jar en place (1.2.1) vérifié de la même taille que `jars-deployes/`, rangé avec l'ancien `didacticiel.yml` dans `_removed-ks_didacticiel-1.2.1/` ; nouveau jar et nouveau fichier de la même taille que ceux du dépôt), actif après redémarrage d'Event. Contient la 1.2.2. Kixster pas encore. Statut : non testé en jeu.**

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage.** Avec `plugins/KS_Didacticiel/didacticiel.yml` du dépôt (parrainage, statistiques, blocs d'os).

## Contenu du 25/10/2026 (LeKiwi06) : enclume en coffre, fusion et ouverture des coffres de récompense

Pour KS_Enclume 1.4.0 et KS_RewardsGUI 1.8.0 (`didacticiel.yml` seul, jar inchangé) :
- thème « Enclume, meule, expérience » : nouvelle entrée `en-coffre` « Enclume en coffre » (accroupi + clic droit, deux
  objets posés, calcul par le serveur sans limite, usure, renommage par le clic simple) ;
- thème des récompenses : `fusion` (accroupi + clic droit sur une enclume, deux coffres posés) et `rewards`
  (confirmation au-delà de 100 émeraudes ; clic sur un coffre ou un tonneau : c'est lui qui s'ouvre) réécrites.
Les trois portent `nouveau: "2026-10-25"`.

**Envoyé sur Kixster le 25/10/2026 à 03:01** : le fichier du serveur avec ces trois changements seulement. Le texte du
Changeur de biome (« 10 utilisations », KS_BiomeChanger 1.2.0, compilé non déployé, commit adcd52a) est dans le dépôt
mais **pas encore sur Kixster** : il partira avec KS_BiomeChanger 1.2.0 (envoyer alors le fichier du dépôt). Ancien
fichier dans `_removed-ks_didacticiel-contenu-2026-10-25/`. Relu au redémarrage (ou `/didacticiel recharger`). Statut : non
testé en jeu.

## 1.4.0 - rappel à la connexion des nouveautés non lues (25/10/2026, LeKiwi06)

Demande de LeKiwi06 : « que les joueurs soit avertis des parties du didacticiel qu'ils n'ont pas lu par un message comme
celui des rewards en attente quand ils se connectent a kixster, un truc du genre "<nombre> nouveautés disponibles dans
le /didacticiel !" ; ça s'afficherait a chaque connexion dans le tchat tant qu'ils n'ont pas fait /didacticiel à
nouveau ».

- **Message** (4 secondes après la connexion, juste après celui des récompenses en attente de KS_RewardsGUI, même
  couleur) : « n nouveautés disponibles dans le /didacticiel ! » (« 1 nouveauté disponible... » au singulier). n = toutes
  les nouveautés encore marquées « Nouveau » pour ce joueur (celles montrées sur ce serveur, comme dans le menu).
- **Quand il s'arrête** : dès que le joueur ouvre la liste des thèmes (/didacticiel, /help ou le bouton du comparateur
  « Informations »), même sans ouvrir les thèmes : les nouveautés non lues de ce moment sont « annoncées » (`vus.yml`,
  section `annonces`, uuid > thème > nouveauté = valeur annoncée). La marque « Nouveau » du menu, elle, reste jusqu'à
  l'ouverture du thème, comme avant.
- **Quand il revient** : quand une nouveauté est ajoutée ou que sa valeur `nouveau` change (nouvelle date) ; le nombre
  affiché compte alors toutes les non lues, anciennes comprises.
- Textes : nouvelles clés `rappel.une` et `rappel.plusieurs` du `lang.yml`.

Choix de Claude, signalés : le bouton « Didacticiel » du comparateur arrête aussi le rappel (pas seulement la commande) ;
au premier démarrage de la 1.4.0, tout joueur qui a encore des « Nouveau » reçoit le message à sa connexion, y compris
pour des nouveautés plus anciennes que la 1.4.0.

Limites : écrit sans serveur de test. Jar seul (`didacticiel.yml` inchangé).

Compilé le 25/10/2026 (`sortie/KS_Didacticiel-1.4.0.jar`).

**Déployé sur Kixster le 25/10/2026 à 03:47 (LeKiwi06 : « Oui, envoie KS_Didacticiel 1.4.0 sur Kixster » ; jar en place (1.3.0) vérifié de la même taille que `jars-deployes/`, rangé dans `_removed-ks_didacticiel-1.3.0/` ; nouveau jar de la même taille que celui compilé ; `didacticiel.yml` du serveur inchangé), actif après redémarrage de Kixster. Statut : non testé en jeu.**

## Contenu du 25/10/2026, 03:51 : coffre renforcé (LeKiwi06)

Nouveauté `crafts > coffre_renforce` (« Coffre renforcé », recette `ks_crafts:coffre_renforce`, `requiert:
"KS_CoffreRenforce"`, `nouveau: "2026-10-25"`), avec le déploiement de KS_CoffreRenforce 1.0.0 et KS_Crafts 1.21.0.

**Envoyé sur Kixster le 25/10/2026 à 03:51** : le fichier du serveur avec cette seule entrée en plus (le texte du
Changeur de biome 1.2.0 n'y est toujours pas, il partira avec KS_BiomeChanger 1.2.0). Ancien fichier dans
`_removed-ks_didacticiel-contenu-2026-10-25b/`. Relu au redémarrage. Statut : non testé en jeu.

**26/10/2026 19:48 : `didacticiel.yml` envoyé sur Kixster (LeKiwi06) : pêche, Estomac du gardien, spawners, Bedrock Breaker, cuivre brut retiré, texte du Changeur de Biome du 25/10 ; ancien fichier dans `_removed-ks_didacticiel-yml-2026-10-26/`. Relu au redémarrage (ou `/didacticiel recharger`).**

**27/10/2026 09:12 : `didacticiel.yml` envoyé sur Kixster (LeKiwi06) : nouveauté « Casse-croûte doré » dans le thème Crafts (`casse_croute`, recette cliquable, `requiert: "KS_CasseCroute"`, nouveau `2026-10-27`). Fichier du serveur vérifié identique au dépôt avant l'envoi ; ancien dans `_removed-ks_didacticiel-yml-2026-10-27-casse-croute/`. Visible après redémarrage de Kixster (le plugin requis doit être chargé).**

## 1.5.0 - nouveautés en test cachées en survie (03/11/2026, LeKiwi06)

Déploiement en deux temps (KS_MondeTest 1.2.0) : un `requiert` dont le plugin ou la clé (`"KS_Contrats:demandes"`,
nouveau format accepté) est encore en test n'est montré que dans le monde de test (pour relire l'entrée). En survie,
l'entrée apparaît d'elle-même à `/mondetest valider`, avec « Nouveau » et l'annonce à la connexion. Classe `PhaseTest`
(copiée), `softdepend: [KS_MondeTest]` ; sans KS_MondeTest, rien ne change.

**Statut : compilé, non déployé, non testé.**

**Déployé sur Kixster le 03/11/2026 à 14:31 (LeKiwi06 : « Oui, déploie ») avec KS_MondeTest 1.2.0 ; jar en place (1.4.0) vérifié de la même taille que `jars-deployes/`, rangé dans `_removed-ks_didacticiel-1.4.0/` ; actif après redémarrage. Statut : non testé en jeu.**
