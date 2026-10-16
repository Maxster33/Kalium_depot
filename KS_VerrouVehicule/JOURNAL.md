# KS_VerrouVehicule - journal

Plugin de la survie (Kixster) : verrou de véhicule. Dépend de KLM_Menu (menu en coffre, textes, messages) ; utilise
SimpleClaimSystem s'il est là (claims) ; sa recette est dans KS_Crafts (1.15.0).

## 1.0.0 - verrou de véhicule (14/10/2026, LeKiwi06)

Demande de LeKiwi06 : « j'aimerai faire un item pour verrouiller le contenant d'un véhicule, histoire qu'on puisse le
garer même hors claims et qui ne soit pas cassable, déplaçable, vidable par hopper, etc. tant que le contenant n'est
pas vide ».

Ses réponses aux questions posées avant le code :

| Question | Réponse de LeKiwi06 |
|---|---|
| Véhicules protégés | Bateaux et radeaux à coffre, wagonnet à coffre, wagonnet à entonnoir, bateau à coffre et jukebox (KS_JukeboxBoat), lama / âne / mule à coffre |
| Qui ouvre le contenant | Le propriétaire seul (celui qui a posé le verrou) |
| Les autres et le déplacement | « Ils peuvent le déplacer uniquement si ce véhicule est dans leur claim à eux » ; le passager est autorisé ; le propriétaire est libre |
| Claim : qui peut le déplacer | Le propriétaire du claim et ses membres |
| Monter (une seule place dans un bateau à coffre) | « Ils peuvent monter mais pas conduire, et si le propriétaire essaie de monter il prend la place conducteur » |
| Le verrou une fois posé | Reste posé, retirable ; ne protège que tant que le contenant n'est pas vide ; tombe si le véhicule (vide) est cassé |
| Retrait | Par un petit menu avant le coffre (« Ouvrir le coffre » / « Retirer le verrou » / « Fermer ») |
| Obtention | Craft dans KS_Crafts : crochet au centre, 4 lingots de fer en croix |
| Apparence | Objet vanilla renommé (crochet), rien à ajouter sur le proxy |
| Serveur, nom | Kixster seulement, KS_VerrouVehicule |

- **Objet** : crochet vanilla brillant, « Verrou de véhicule », 5 lignes de description. Empilable (comme un crochet,
  aussi sur Bedrock : objet vanilla). Il ne se pose pas comme un crochet et ne sert pas d'ingrédient (coffre piégé,
  arbalète...) : il perdrait son nom.
- **Poser** : clic droit sur le véhicule, verrou en main (main principale). Refusé : véhicule sans coffre, déjà
  verrouillé (le message donne le pseudo), animal dont on n'est pas le maître, véhicule dans le claim d'un autre. Le
  véhicule garde l'UUID et le pseudo du propriétaire.
- **Protection** (verrou posé et contenant non vide) :
  - **Ouvrir** : le propriétaire seul. Un autre : « Verrouillé par <pseudo> » au-dessus de la barre d'objets. Bloqué
    aussi pour la touche d'inventaire à bord.
  - **Casser** : impossible pour tous, propriétaire compris (coups, explosions, lave, cactus...). Âne, mule, lama :
    aucun dégât, sauf le vide et `/kill`.
  - **Entonnoirs** : ni un entonnoir ni un wagonnet à entonnoir ne vident le contenant ; le remplir reste possible.
  - **Monter** : le propriétaire monte et conduit. Un autre joueur monte sur un **siège invisible** (porte-armure
    marqueur) posé dans le véhicule : il est assis mais ne conduit pas, et ne peut pas ouvrir le coffre. Si le
    propriétaire monte ensuite (clic droit normal), le passager descend et le propriétaire prend la place du
    conducteur. Les animaux et monstres ne montent pas.
  - **Déplacer** : seul le propriétaire, ou le propriétaire et les membres du claim où se trouve le véhicule, le
    conduit, le pousse, l'attache ou le détache en laisse. Un bateau ou un wagonnet est **ancré** : poussé par un
    autre joueur, une entité, un piston, un courant d'eau, des rails ou une explosion, il revient à sa place (écart
    toléré : 0,3 bloc ; une descente, chute ou eau, est acceptée). Libre pendant qu'il est conduit ou tenu en laisse
    par quelqu'un qui peut le déplacer, et après leur poussée tant qu'il roule ou glisse.
  - **Lama** : il ne suit une caravane que si elle est menée en laisse par quelqu'un qui peut le déplacer.
  - **Cisailles** : un autre joueur ne retire pas la selle ni le tapis d'un animal protégé.
- **Menu du propriétaire** (coffre d'une rangée de KLM_Menu), au clic qui ouvrirait le contenant (wagonnet : clic
  droit ; bateau, animal : accroupi + clic droit) : « Retirer le verrou » (dit si le véhicule est protégé ; le verrou
  revient dans l'inventaire, à ses pieds s'il est plein), « Ouvrir le coffre » au centre, « Fermer ». Le menu ne
  commande le véhicule qu'à 6 blocs au plus.
- **Contenant vide** : véhicule normal (n'importe qui l'ouvre, le casse, le déplace). Cassé (ou animal mort), il lâche
  son verrou.
- **Opérateurs** (permission `ksverrouvehicule.contourner`, par défaut les opérateurs) : ouvrent et déplacent un
  véhicule verrouillé ; ils ne le cassent pas non plus.

Techniquement nécessaire, signalé :

- **Siège invisible** : lu dans le code du jeu 26.3, un bateau à coffre n'a qu'une place, le conducteur est toujours
  le premier passager s'il est un joueur, et un joueur qui monte passe toujours en premier (sauf si un joueur y est
  déjà). Pour qu'un autre joueur monte sans conduire, il s'assoit sur un porte-armure invisible qui, lui, est le
  premier passager (même principe que le jukebox de KS_JukeboxBoat). Le siège n'est pas enregistré avec le monde ; il
  disparaît quand on en descend. Hauteur du joueur assis à vérifier en jeu (Java et Bedrock).
- **Ouverture d'un âne, d'une mule ou d'un lama depuis le menu** : l'API de Paper ne sait pas ouvrir leur inventaire ;
  le plugin appelle la méthode du jeu (`openCustomInventoryScreen`, comme accroupi + clic droit). Le jeu refuse si un
  autre joueur est dessus (message « Quelqu'un est monté dessus »).
- **Bateau à coffre et jukebox** : personne n'y monte déjà (KS_JukeboxBoat) ; le clic droit normal (lecture / pause)
  reste à tout le monde ; accroupi + clic droit ouvre le menu du verrou au lieu du coffre. KS_JukeboxBoat n'est pas
  modifié : ses écoutes ignorent les clics que ce plugin a déjà annulés.
- Un conducteur qui ne peut pas (ou plus) déplacer le véhicule est descendu : membre d'un claim qui en sort avec le
  véhicule (il reste dans le claim), joueur monté quand le contenant était vide et qui ne peut plus conduire une fois
  le contenant rempli.

Choix de Claude, signalés :

- Un animal se verrouille seulement par son maître (sinon n'importe qui verrouillerait l'âne d'un autre, qui ne
  pourrait plus l'ouvrir) ; on ne pose pas de verrou dans le claim d'un autre (même raison pour les véhicules).
- Le propriétaire ne casse pas non plus son véhicule protégé (coup accidentel) : il le vide ou retire le verrou d'abord.
- Les opérateurs ouvrent et déplacent, mais ne cassent pas.
- Un animal protégé n'est plus poussé par les autres entités, propriétaire compris (il n'est pas ancré : il marche de
  lui-même ; seuls la laisse, la monture et les caravanes sont contrôlées).
- Retrait du verrou sans confirmation (un clic dans le menu).

Limites connues :

- Un wagonnet verrouillé sur des rails ne part pas tout seul sur un rail de propulsion activé par redstone : il faut
  que le propriétaire le pousse (ensuite il roule librement tant qu'il ne s'arrête pas). Même chose pour un bateau mis
  dans un courant d'eau : le propriétaire doit le conduire.
- Un wagonnet garé dans une pente revient à sa place à chaque fois qu'il glisse (petits sauts visibles).
- Un membre de claim qui attache le véhicule en laisse dans son claim peut le tracter au-delà (la laisse reste
  permise une fois attachée) ; il ne peut toujours ni l'ouvrir ni le casser.
- Rien n'affiche le verrou sur le véhicule (un modèle 3D serait invisible pour les joueurs Bedrock) : on le découvre au
  clic.

Écrit sans serveur de test. Compilé le 14/10/2026 (`sortie/KS_VerrouVehicule-1.0.0.jar`). **À déployer avec
KS_Crafts 1.15.0** (recette). **Statut : non déployé, non testé en jeu.**

**Déployé sur Kixster le 14/10/2026 à 18:03 (LeKiwi06 : « oui déploie sur kixster » ; envoi groupé : KS_VerrouVehicule 1.0.0 (nouveau) et KS_Crafts 1.15.0 ; KS_Crafts 1.14.1 en place vérifié identique à la référence avant l'envoi, dans `_removed-ks_crafts-1.14.1/` ; jars du serveur vérifiés identiques à ceux compilés après l'envoi), actif après redémarrage de Kixster. Event n'est pas touché. Statut : non testé en jeu.**

Actif sur Kixster depuis son redémarrage du 14/10/2026 à 18:04 (journal lu : KS_VerrouVehicule 1.0.0 et KS_Crafts 1.15.0 chargés et activés sans erreur ni avertissement, « 79 crafts ajoutés » soit la recette du verrou en plus des 78, SimpleClaimSystem trouvé, `lang.yml` créé). Statut : non testé en jeu.

**Déployé sur Event le 16/10/2026 à 20:00 (LeKiwi06 : « envoie tout sur event je vais faire les tests là-bas » ; envoi groupé : KS_VerrouVehicule 1.0.0, KS_LaisseBateau 1.0.0, KS_Trains 1.0.0, KS_Crafts 1.16.0 ; KS_Crafts 1.14.1 en place vérifié identique à la référence avant l'envoi, dans `_removed-ks_crafts-1.14.1/` ; jars du serveur vérifiés identiques à ceux compilés après l'envoi), actif après redémarrage d'Event. Kixster reste en 1.0.0 (même version). Statut : non testé en jeu.**

## 1.1.0 - wagon verrouillé dans un train de KS_Trains (16/10/2026, LeKiwi06)

Étape 3 de KS_Trains (cahier des charges validé par LeKiwi06) : « un wagon à coffre verrouillé ne s'attelle que par
son propriétaire ; seul lui change ses réglages ; il n'est plus ancré quand il roule dans un train ».

- Un wagon qui porte l'identifiant d'un train de KS_Trains (`ks_trains:train`) **n'est pas ancré** : c'est le train qui
  le place. Toutes les autres protections restent (ouverture, casse, entonnoirs posés au sol, laisse...).
- L'attelage et les réglages du coffre (propriétaire seulement) sont dans KS_Trains 1.2.0.

Compilé le 16/10/2026 (`sortie/KS_VerrouVehicule-1.1.0.jar`). **À déployer avec KS_Trains 1.2.0. Statut : non
déployé, non testé en jeu.**

**Déployé sur Event le 16/10/2026 à 20:46 (LeKiwi06 : « oui envoie sur event » ; envoi groupé : KS_Trains 1.2.0, KS_VerrouVehicule 1.1.0 ; jars en place (KS_Trains 1.1.0, KS_VerrouVehicule 1.0.0) vérifiés identiques aux références avant l'envoi, rangés dans `_removed-ks_trains-1.1.0/` et `_removed-ks_verrouvehicule-1.0.0/` ; jars du serveur vérifiés identiques à ceux compilés ; Event n'avait toujours pas redémarré depuis 20:00 : aucune version précédente n'y a tourné), actif après redémarrage d'Event. Kixster reste en KS_VerrouVehicule 1.0.0 et n'a pas KS_Trains. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 16/10/2026 à 20:50 (journal lu, heures du journal en UTC : 18:50) : KS_Trains 1.2.0, KS_Crafts 1.17.0 (« 81 crafts ajoutés » : les 78 d'avant + verrou, locomotive et wagon contrôleur), KS_VerrouVehicule 1.1.0 et KS_LaisseBateau 1.0.0 chargés et activés sans erreur ni avertissement ; KS_Trains activé après KS_Claim (ordre voulu pour la pose de la locomotive) ; `config.yml` et `lang.yml` créés. Statut : non testé en jeu.**

## 1.1.1 - décor des wagons de KS_Trains accepté (16/10/2026, LeKiwi06)

KS_Trains 1.3.0 pose un modèle 3D de bloc (décor pour les joueurs Java) comme passager des wagons à coffre d'un train.
La 1.0.0 refuse tout passager qui n'est pas un joueur sur un véhicule verrouillé et protégé (animaux et monstres) : le
décor d'un coffre verrouillé ne se serait pas posé.

- Un modèle d'affichage (Display) peut monter dans un véhicule verrouillé ; animaux et monstres restent refusés.

**À déployer avec KS_Trains 1.3.0.** Compilé le 16/10/2026 (`sortie/KS_VerrouVehicule-1.1.1.jar`). **Statut : non
déployé, non testé en jeu.**

**Déployé sur Event le 16/10/2026 à 22:18 (LeKiwi06 : « j'ai arrêté event, envoie tout » ; Event arrêté proprement à 22:17:12, vérifié dans son journal ; envoi groupé : KS_Trains 1.3.0, KS_Crafts 1.18.0, KS_VerrouVehicule 1.1.1 ; jars en place (KS_Trains 1.2.0, KS_Crafts 1.17.0, KS_VerrouVehicule 1.1.0) vérifiés identiques aux références avant l'envoi, rangés dans `_removed-ks_trains-1.2.0/`, `_removed-ks_crafts-1.17.0/`, `_removed-ks_verrouvehicule-1.1.0/` ; jars du serveur vérifiés identiques à ceux compilés), actif après redémarrage d'Event. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 16/10/2026 à 22:20 (journal lu, heures du journal en UTC : 20:20) : KS_Trains 1.3.0, KS_Crafts 1.18.0 (« 82 crafts ajoutés » : + le wagon jukebox) et KS_VerrouVehicule 1.1.1 activés sans erreur ni avertissement. **Nouvelle physique des wagonnets active** : plus de refus du pack au démarrage, et le `level.dat` réécrit par le serveur à 22:20:16 garde `minecart_improvements` dans `enabled_features` et dans les packs activés. Reste à faire par LeKiwi06 : `/gamerule max_minecart_speed 40`. Statut : non testé en jeu.**
