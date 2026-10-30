# KS_Jetons - journal

Plugin du serveur Event : jetons. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort » de
LeKiwi06 (03/10/2026, en cours de rédaction ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - jetons et inventaire de jetons (03/10/2026)

- **Jetons** : de téléportation (apparence : œil de l'Ender), d'emplacement (carte), de claim (pelle en or), de la
  mort (crâne de squelette) ; livre de connaissances inutilisable, brillant, empilable par 64, marqueur
  `ks_jetons:jeton`.
- **Inventaire de jetons** (bouton « Jetons » du menu d'Event, `/jetons`) : nombre de chaque jeton ; « Déposer des
  jetons » (coffre : les jetons y entrent, le reste est rendu) ; « Retirer » (1 à 640, en objets) ; « Acheter » avec
  le score (prix `prix.<jeton>` du `config.yml`, **0 = pas encore en vente** : prix à fixer par LeKiwi06, repère
  « ± 15 minutes de farm pour une TP »). `jetons.yml`.
- API pour les autres plugins (KS_Teleport, KS_CoffreMort, KS_Claim, KS_KaliumGive) : `creer`, `type`, `nombre`,
  `consommer`, `ajouter`, `prix`.
- `depend` KLM_Menu ; `softdepend` KS_Menu, KS_Economy. Joueurs Bedrock : apparence à ajouter dans `geyser-bedrock`.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.0.0 ; nouveau), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 2.0.0 - lingots, badges, inventaire spécial en entonnoirs, fusion à l'enclume (08/10/2026, LeKiwi06)

Demande de LeKiwi06 (08/10/2026, « derniers ajouts et modifications pour jetons et badges ») et ses réponses du même
jour ; cahier des charges : catégorie 7, parties 6 à 11 (en local, publié au déploiement).

- **Jetons** : apparence d'un lingot. Fly (fer, **nouveau**), mort (netherite), téléportation (or), localisation
  (cuivre, **nouveau** : l'ancien « point de transport »), claim (pelle en or, inchangé). Le **jeton d'emplacement est
  supprimé** (remplacé par le badge de localisation) : ceux de la 1.0.0 deviennent des livres sans effet.
- **Badges** (nouveau) : fly (bloc de fer ; 1, 3, 5, 10, 15 minutes de vol par heure), mort (bloc de netherite ; une
  récupération gratuite tous les 7, 6, 5, 4, 3, 2, 1 jours), téléportation (bloc d'or ; 1 à 5 téléportations gratuites
  de plus en réserve), localisation (bloc de cuivre ; 1 à 5 emplacements de localisation en plus de `/home bed` et
  `/spawn` : niveau 1 = 1, niveau 2 = 2, etc. ; niveau maximal 5 comme les autres, non précisé par LeKiwi06).
  Livres de connaissances (on ne peut pas les poser), non empilables, niveau dans le nom. Valeurs réglables
  (`badges.<type>` du `config.yml` ; le nombre de valeurs est le niveau maximal).
- **Fusion** : 2 badges du même type et du même niveau dans une enclume donnent le niveau suivant. Coût en niveaux
  d'expérience : 30 (niveau 2), 50 (niveau 3), puis + 20 par niveau (70, 90, 110, 130 ; interprétation du « etc. » de
  LeKiwi06, réglable : `fusion-niveaux`). Aucun autre usage d'un jeton ou d'un badge à l'enclume (ni renommage).
  - Pour l'enclume vanilla, deux livres ne se combinent pas : elle vide le résultat et annule le coût après le calcul
    (vérifié dans le code de Paper). Le plugin pose donc l'aperçu (`PrepareAnvilEvent`, priorité HIGH), renvoie le coût
    au tick suivant et fait lui-même la prise (niveaux retirés, les deux badges consommés). L'enclume ne s'use pas.
  - Coût affiché : 39 au plus si le joueur a assez de niveaux (au-delà le jeu écrit « Trop cher ! ») ; le vrai coût est
    en dernière ligne de l'aperçu (vert ou rouge). KS_Enclume ne touche pas à ce résultat (coût laissé ≤ 39).
- **Inventaire spécial** (bouton « Inventaire spécial » du menu, `/jetons`, alias `/badges`) : fenêtre qui liste
  jetons et badges portés, puis deux contenants à l'interface d'entonnoir (5 cases) :
  - « Mes jetons » : seuls les jetons y entrent (5 piles de 64) ;
  - « Mes badges » : un badge de chaque type (4 cases, la 5e bloquée par une barrière non déplaçable). **Un badge est
    porté tant qu'il y est rangé.**
  - Les objets refusés ne peuvent pas y être posés (clic, Maj + clic, touches, glisser) ; à la fermeture, tout intrus
    est rendu. Enregistré à chaque changement (`jetons.yml`, objets entiers : un jeton de localisation garde sa
    magnétite).
  - Achat avec le score : seulement téléportation et claim (`prix.tp` : **0 = pas en vente**, à fixer ;
    `prix.claim` : **20** émeraudes, LeKiwi06, 08/10/2026). Jeton de
    fly et de la mort : /rewards seulement ; localisation : craft (KS_Teleport, à venir).
  - Contenant plein : les jetons achetés ou reçus attendent (ils comptent déjà) et y descendent dès qu'une place se
    libère.
- **Reprise de la 1.0.0** : les nombres de `jetons.yml` passent dans les contenants (le surplus en attente) ; le fichier
  d'origine est gardé (`jetons-1.0.0.yml`). Les jetons d'emplacement stockés ne sont pas repris (écrit dans la console).
- API (KS_Teleport, KS_CoffreMort, KS_Fly, KS_Claim, KS_KaliumGive) : `creer`, `creerBadge`, `creerCustom`,
  `idsCustom`, `type`, `badge`, `niveau`, `nombre`, `consommer`, `ajouter`, `prix`, `niveauBadge`, `valeurBadge`. Les
  délais des badges seront tenus par ces plugins, **par joueur** (changer de badge ne recharge rien).
- Textes du menu sous de nouvelles clés (`special.*`) : les anciennes clés du `lang.yml` ne servent plus.
- Nouvelles clés du `config.yml` (`badges`, `fusion-niveaux`) : valeurs par défaut dans le code ; à ajouter à la main
  dans le fichier du serveur seulement pour les changer. `prix.emplacement` et `prix.mort` ne sont plus lus.

Limites :
- **Aucun effet en jeu pour l'instant** : KS_Teleport, KS_CoffreMort et KS_Fly n'existent pas encore.
- Joueurs Bedrock : apparence des jetons et des badges dans `geyser-bedrock` 1.3.0 (proxy, 08/10/2026, actif après
  redémarrage du proxy) ; fusion à l'enclume sur Bedrock à vérifier (leur jeu calcule lui-même l'aperçu).
- `rachats.csv` de KS_Economy : `jeton_emplacement` n'existe plus ; nouveaux jetons et badges absents (valeurs à fixer
  avec LeKiwi06).

**Déployé sur Event le 08/10/2026 à 19:09 (LeKiwi06, avec KS_KaliumGive 1.9.0 ; 1.0.0 et son `config.yml` dans
`_removed-ks_jetons-1.0.0/`), actif après redémarrage d'Event. `config.yml` du serveur remplacé par celui de la 2.0.0
(`prix.claim: 20`) ; `jetons.yml` du serveur était vide (rien à reprendre). Statut : non testé en jeu.**

## 2.0.1 - prix : jeton de téléportation à 20, jeton de claim pas en vente (08/10/2026, LeKiwi06)

Correction de LeKiwi06 (08/10/2026) : « ce n'est pas le jeton de claim qui coûte 20 mais le jeton de téléportation »
(sa réponse « met 20 émeraudes » avait été rangée sous le jeton de claim). `config.yml` : `prix.tp: 20`,
`prix.claim: 0` (pas en vente, prix non fixé). Aucun changement de code ; le `config.yml` du serveur est à remplacer au
déploiement (la clé y existe déjà).

**Déployé sur Event le 08/10/2026 à 23:38 (LeKiwi06, déploiement groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; 2.0.0 et son `config.yml` dans
`_removed-ks_jetons-2.0.0/`), actif après redémarrage d'Event. `config.yml` du serveur remplacé (`prix.tp: 20`,
`prix.claim: 0`). Statut : non testé en jeu.**

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 2.0.1 ; 1.0.0 et son `config.yml` dans `_removed-ks_jetons-1.0.0/` ; `config.yml` du serveur remplacé par celui de la 2.0.1 (`prix.tp: 20`, `prix.claim: 0` ; l'ancien avait tous les prix à 0) ; `jetons.yml` contenait 2 jetons de téléportation de LeKiwi06, repris par la 2.0 au démarrage), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**

## 2.0.2 - description du jeton de localisation (11/10/2026, LeKiwi06)

Signalé par Claude en expliquant l'usage du jeton à LeKiwi06, qui a répondu « oui corrige le texte » : la description du
jeton de localisation disait « À ranger dans tes jetons : /jetons », comme les autres jetons, alors qu'il ne sert pas
depuis l'inventaire spécial : il s'utilise en main (clic droit : la magnétite est enregistrée dans un emplacement,
KS_Teleport).

- Jeton de localisation : dernière ligne de la description = « Clic droit, jeton en main, pour l'utiliser ». Les autres
  jetons gardent « À ranger dans tes jetons : /jetons ».
- Aucun autre changement. KS_Teleport n'est pas à recompiler (il prend la description de ce plugin).
- Les jetons de localisation déjà fabriqués gardent l'ancienne ligne (elle est écrite dans l'objet) ; un jeton neuf ne
  s'empile donc pas avec un ancien de la même magnétite.

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; 2.0.1 dans `_removed-ks_jetons-2.0.1/` ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. Event n'est pas touché (il reste en 2.0.1). Statut : non testé en jeu.**

## 2.1.0 - inventaire spécial en un seul coffre, cases de localisation, /jeton et /badge (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « /jeton et /badge (possibilité de l'écrire au singulier), j'aimerais aussi que cette interface
soit une seule interface de coffre, avec des slots dédiés pour les jetons de localisation si la personne a un badge de
localisation etc ». Maquette montrée avant le code ; ses réponses : un jeton posé dans une case de localisation = une
destination (« mais j'aimerais que chaque slot ait un cooldown de 24 h si on enlève un jeton, 15 minutes c'était bien
trop court ») ; une case par type de jeton.

- **Commandes** : /jetons, /badges, /jeton, /badge (alias) et le bouton « Inventaire spécial » du menu.
- **Un coffre de 4 rangées** (remplace la fenêtre et ses deux contenants d'entonnoir) :
  - rangée 1 : étiquette « Tes badges », puis une case par badge (fly, mort, téléportation, localisation) ; un badge y
    est porté ; poser un autre badge du même type échange les deux ;
  - rangée 2 : étiquette « Tes jetons », puis une case par jeton (fly, mort, téléportation, claim), **sans limite de
    nombre** (la case montre 64 au plus et « Tu en as n ») : plus de jetons « en attente de place » ;
  - rangée 3 : étiquette « Tes localisations », puis les cases de localisation (5 : la valeur du badge au niveau
    maximal, 8 au plus) : une case ouverte par niveau du badge de localisation porté ; bloquée (barrière, niveau
    demandé), en attente (horloge, temps restant) ou libre ;
  - rangée 4 : barre d'actions : « Comment ça marche », « Acheter : Jeton de téléportation » (si en vente), « Fermer ».
- **Gestes** (tous les clics qui touchent le coffre sont annulés puis faits par le plugin au tick suivant, pour que rien
  ne parte dans une mauvaise case ; les clics dans son propre inventaire restent libres) : Maj + clic sur un jeton ou un
  badge de son inventaire = il va dans sa case ; clic avec un objet sur sa case = le poser (clic droit : un jeton) ;
  clic sur une case = reprendre son contenu sur le curseur (clic droit : la moitié des jetons ; Maj : dans
  l'inventaire). Refus : message dans le tchat, au plus toutes les 2 secondes.
- **Cases de localisation** : un jeton de localisation par case. Posé : sa magnétite est une destination de /home
  (KS_Teleport 1.4.0 vérifie : jeton lié, magnétite toujours là, pas déjà dans une autre case). Repris : le jeton est
  rendu et la case n'en reprend un que **24 h** plus tard (`localisation-delai-heures`, nouvelle clé, 24 par défaut
  dans le code). Badge retiré ou plus bas : les jetons des cases en trop restent posés, mais ces destinations ne
  servent plus (comme avant).
- **API** inchangée pour les autres plugins (nombre, consommer, ajouter, prix, niveauBadge, valeurBadge, creer...) ;
  `nombre(LOCALISATION)` = jetons posés ; `consommer` et `ajouter` ne s'appliquent pas au jeton de localisation.
  Nouvelles : `localisations`, `toutesLocalisations`, `attenteLocalisation`, `delaiLocalisationMs`,
  `validateurLocalisation`, `ecouterLocalisations`, `poserLocalisation`, `rendre`, `ouvrirSpecial`. KS_Fly,
  KS_CoffreMort, KS_AntiCheat et KS_KaliumGive recompilés contre elle sans erreur (pas à redéployer).
- **Données** : `jetons.yml` passe au format 2 (`version: 2`, par joueur : niveaux des badges, nombres de jetons,
  jetons de localisation posés, cases en attente, objets à rendre). **Reprise automatique** du fichier de la 2.0 au
  premier démarrage (ancien fichier gardé : `jetons-2.0.yml`) : badges dans leur case, jetons dans leur case (jetons
  « en attente » compris) ; sont rendus au joueur (connexion ou ouverture du coffre) : les jetons de localisation non
  utilisés qui étaient rangés dans « Mes jetons », un deuxième badge du même type.
- Jeton de localisation : nouvelle dernière ligne « À poser dans tes localisations : /jetons » (les anciens gardent la
  leur).

Choix de Claude, signalés :
- pas de confirmation pour poser ou reprendre un jeton de localisation (le message du tchat dit le délai de 24 h) ;
- l'achat de jetons de téléportation reste une petite fenêtre de saisie, ouverte depuis la barre du coffre.

Limites : écrit sans serveur de test ; Bedrock (clics, Maj + clic tactile) à vérifier. Empilé sur la 2.0.2, jamais
testée en jeu (demande de LeKiwi06).

**À déployer avec KS_Teleport 1.4.0** (cases de localisation = emplacements). Compilé le 18/10/2026
(`sortie/KS_Jetons-2.1.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:44 (LeKiwi06 : « Envoie les cinq plugins sur Event pour que je teste, Kixster ensuite » ; envoi groupé : KS_Jetons 2.1.0, KS_Teleport 1.4.0, KS_Claim 1.5.0, KS_Economy 1.10.0, KS_Didacticiel 1.1.0 ; jars en place vérifiés de la même taille que `jars-deployes/` avant l'envoi, nouveaux jars de la même taille que ceux compilés ; ancien jar dans `_removed-ks_jetons-2.0.2/`), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.** `jetons.yml` du serveur vide : rien à reprendre.

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage.** `jetons.yml` copié avant sa conversion : `_removed-ks_jetons-2.0.2/jetons-avant-2.1.0.yml`.

## 2.2.0 - moi de test : inventaire de jetons séparé (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- Fiches des joueurs (`fiche`, `nombre`, `niveauBadge`, `localisations`, `attenteLocalisation`, inventaire spécial) par `CompteTest.de` : dans le monde de test, jetons, badges et jetons de localisation sont ceux du compte de test. Achat de jetons avec le solde de test (KS_Economy 1.11.0).
- `toutesLocalisations()` rend les jetons de chaque fiche (comptes de test compris) sans les recalculer par joueur.

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**

## 2.3.0 - équipements spéciaux (30/10/2026, LeKiwi06)

Chantier des maléfices du Nether (cahier local `Malefices_Nether.md`, validé le 30/10/2026). LeKiwi06 : « l'amulette
peut être stockée dans le même emplacement que les jetons et badges, il suffit d'une 3e catégorie : équipements
spéciaux » (plus tard aussi la bague de relais dimensionnel).

- **Coffre de 5 rangées** : nouvelle rangée « Tes équipements spéciaux » (étiquette violette) entre les localisations et
  la barre d'actions (aide, achat, fermer : une rangée plus bas).
- Un équipement est déclaré par son plugin (`declarerEquipement(id, nom, reconnaissance)`, 8 au plus) : une case par
  équipement, un seul de chaque, **porté tant qu'il y est rangé**. Mêmes gestes que les badges : clic avec l'objet
  sur sa case (échange si la case est prise), clic sur la case pour le reprendre, Maj + clic dans son inventaire.
- API : `declarerEquipement`, `porteEquipement`, `equipement`. Premier équipement : l'amulette mangeuse de mal
  (KS_MaleficesNether 1.0.0).
- `jetons.yml` : `joueurs.<compte>.equipements.<id>` (objet entier) ; un équipement dont le plugin manque est gardé
  (sans case). Moi de test : par `CompteTest.de` comme le reste de la fiche.
- Texte de refus changé (« Seuls les jetons, les badges et les équipements spéciaux... ») : nouvelle clé
  `coffre.refus-objet-2`.

Empilé sur la 2.2.0, jamais testée en jeu (demande de LeKiwi06 : chantier validé). Compilé le 30/10/2026
(`sortie/KS_Jetons-2.3.0.jar`). **À déployer avec KS_MaleficesNether 1.0.0.** Statut : non déployé, non testé en jeu.

**Déployé sur Kixster le 30/10/2026 à 18:56** (LeKiwi06 : « Oui, déploie » ; jars en place vérifiés identiques à `jars-deployes/` avant l'envoi), avec KS_MaleficesNether 1.0.0, KS_Jetons 2.3.0, KS_Atouts 1.2.0, KS_KaliumGive 1.13.0, KS_Textures 1.2.0 et le didacticiel ; geyser-bedrock 1.10.0 sur le proxy ; à redémarrer, non testé en jeu.
