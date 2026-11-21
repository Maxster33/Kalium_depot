

**Actif sur Event depuis son redémarrage du 17/10/2026 à 04:18 (journal lu, heures en UTC : 02:18) : KS_Trains 1.4.1 activé sans erreur ni avertissement (après KLM_Contacts 1.3.0), démarrage en 12 s. Statut : non testé en jeu.**

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.4.7 - modèles agrandis retirés, sauf celui du wagon jukebox (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « retire tous les modèles, sauf celui du jukebox, ils sont un peu buggués et n'apportent rien
d'essentiel ».

- Plus de modèle agrandi sur les fours, coffres et entonnoirs : on voit les wagonnets du jeu. Ceux déjà posés sont
  retirés à la seconde suivante (ils ne sont de toute façon pas enregistrés avec le monde).
- Le wagon jukebox garde son modèle (sans lui, c'est un wagonnet simple) ; `decor.actif`, `decor.taille` et
  `decor.hauteur` ne concernent plus que lui.
- Fumée du four du milieu de la locomotive inchangée.
- **Catégories de son** (LeKiwi06 : « seul le son du train doit être dans créatures amicales : sifflet, cornes, etc.
  doivent être joués dans jukebox et blocs musicaux ») : souffle de vapeur en « créatures amicales » ; sifflet de départ,
  klaxon (cornes de chèvre et sifflet par défaut) en « jukebox / blocs musicaux » (la musique du wagon jukebox y était
  déjà). En 1.4.6, tout était en « créatures amicales ».
- `config.yml` inchangé.

Compilé le 18/10/2026 (`sortie/KS_Trains-1.4.7.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:20 (LeKiwi06 : « oui envoie sur event ») : à la place de la 1.4.6 (pas encore redémarrée) ; jar en place vérifié identique à la référence, rangé dans `_removed-ks_trains-1.4.6/` ; jar du serveur vérifié identique à celui compilé ; `config.yml` inchangé. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 18/10/2026 à 08:30 (journal lu à 08:32, heures en UTC : 06:30) : KS_Trains 1.4.7 activé sans erreur ni avertissement. Aucun joueur connecté à la lecture ; relu à 09:07 après 35 minutes de jeu de LeKiwi06 avec un train : aucune erreur (plus de spam). Statut : en test.**

## 1.4.8 - corne prise en compte, wagons renommés et ouverts depuis le menu (18/10/2026, LeKiwi06)

Retour de LeKiwi06 sur Event (1.4.7) : « quand on met une corne elle n'est plus prise en compte, ça garde le klaxon de
base (il fait bien tchou-tchou en deux coups, c'est bon) » ; demande : « quand on clique dans l'interface du minecart
contrôleur on voit tous les minecarts du train : j'aimerais qu'on puisse les renommer, leur donner des noms dans cette
interface, et qu'on puisse accéder à leurs inventaires aussi ».

- **Corne** : le klaxon jouait la corne de la case choisie (la première par défaut) ; une corne posée dans une autre
  case sans cliquer sa vitre laissait le sifflet (avant la 1.4.3 : message « Aucune corne choisie »). Maintenant, si
  la case choisie est vide, la première corne posée sonne, et sa vitre est verte. L'instrument est lu dans le
  composant du jeu de la corne (sinon dans ses métadonnées). Journal d'Event : aucune erreur.
- **Nom d'un wagon** : bouton « Renommer » (étiquette) dans la page de chaque wagon : fenêtre de saisie (32 caractères
  au plus ; vide : plus de nom). Le nom est enregistré dans le wagon ; dans le menu du train, il remplace le titre du
  wagon, son numéro et son type passent en première ligne. Seulement dans le menu (pas au-dessus du wagon, choix de
  Claude ; le nom du train reste celui de l'étiquette sur la locomotive).
- **Contenu d'un wagon** : bouton « Ouvrir le contenu » (coffre) dans la page des wagons à coffre, entonnoir et four
  (case du carburant) : comme un clic droit dessus, depuis le menu. Wagon verrouillé (KS_VerrouVehicule) : son
  propriétaire seulement. Contrôleur : ses klaxons (déjà dans sa page) ; jukebox : ses disques sont dans le coffre
  devant lui.
- Ligne du clic dans le menu du train : « contenu, nom, dételer », « réglages, nom, dételer » ou « nom, dételer »
  (nouvelles clés de `lang.yml`).
- Appels à KLM_Menu vérifiés présents dans KLM_Menu 2.11.0 (celui d'Event). `config.yml` inchangé.

Compilé le 18/10/2026 (`sortie/KS_Trains-1.4.8.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 10:33 (LeKiwi06 : corriger la laisse « avant de l'envoyer sur Event » ; envoi groupé KS_Trains 1.4.8 et KS_LaisseBateau 1.0.1) : jar en place (1.4.7) vérifié identique à la référence, rangé dans `_removed-ks_trains-1.4.7/` ; jar du serveur vérifié identique à celui compilé. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 18/10/2026 à 13:34 (journal lu à 14:16, heures en UTC : 11:34) : KS_Trains 1.4.8 activé sans erreur ni avertissement ; aucune erreur jusqu'à la lecture (LeKiwi06 connecté depuis 13:47). Statut : non testé en jeu.**

**Testé en jeu par LeKiwi06 sur Event (signalé le 22/10/2026 : « j'ai testé tout marche bien »). Event retiré depuis le 21/10 ; pas encore déployé sur Kixster.**

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé, accord de LeKiwi06 : « allez on peut tout déployer ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage.**

## 1.5.0 - minecart avec coffre renforcé dans un train (27/10/2026, LeKiwi06)

Demande de LeKiwi06 : bateau et minecart avec coffre renforcé (KS_CoffreRenforce 1.1.0) ; choix « Oui, avec
KS_Trains » pour que le minecart serve de wagon.

- Le minecart avec coffre renforcé s'attelle comme un wagon à coffre (rien de changé là). Son stock (jusqu'à 1 000
  stacks d'un seul objet) n'est qu'en partie dans ses 27 cases : les **wagons à entonnoir du train** prennent et
  donnent un objet à la fois par l'API de KS_CoffreRenforce (`CoffresRenforces`), en respectant ses réglages entrée /
  sortie ; un objet qu'il refuse (autre objet que celui stocké, plein) reste dans l'entonnoir, qui essaie le wagon
  suivant.
- **Menu du train** : « Coffre renforcé » au lieu de « Coffre », avec son contenu (« 12 345 x Diamant » ou « Vide ») ;
  « Ouvrir le contenu » ouvre le menu du coffre renforcé.
- Wagon libre percuté et vide : il se brise et redonne le minecart avec coffre renforcé (avant : un minecart à coffre
  ordinaire). Avec un contenu, il est projeté comme les autres.
- Un coffre renforcé ou un véhicule avec coffre renforcé n'est plus un carburant des fours du train (case du four,
  wagons à entonnoir) : il aurait brûlé comme un coffre ou un bateau ordinaire (LeKiwi06 : « bloque-le aussi »).
- `softdepend` sur KS_CoffreRenforce : sans lui, rien ne change (ses classes ne sont jamais chargées).

Limite : le wagon jukebox derrière un minecart avec coffre renforcé ne voit que ses 27 cases (non modifié).

Compilé le 27/10/2026 (`sortie/KS_Trains-1.5.0.jar`, avec `KS_CoffreRenforce` compilé d'abord). **Statut : non
déployé, non testé en jeu.**

**Déployé sur Kixster le 28/10/2026 à 10:34** (LeKiwi06 : « Oui, déploie KS_CoffreRenforce 1.1.0 et KS_Trains 1.5.0 sur Kixster »), avec KS_Trains 1.5.0 et `didacticiel.yml` (Véhicules › « Bateau et minecart avec coffre renforcé ») : jars en place vérifiés identiques à `jars-deployes/` avant l'envoi, rangés dans `_removed-ks_coffrerenforce-1.0.0/` et `_removed-ks_trains-1.4.8/` ; fichiers du serveur vérifiés identiques à ceux compilés. À redémarrer par LeKiwi06. Statut : non testé en jeu.

## 1.6.0 - alcool neutre comme carburant (28/10/2026, LeKiwi06)

Étape 2 du chantier « atouts, alcools, aliments » (cahier local `Atouts_Alcools_Aliments.md`, partie 3.2.1) :
l'alcool neutre de KS_Alcools est un carburant des fours du train.

- **500 blocs** par alcool neutre (10 000 ticks de cuisson, comme 50 cuissons dans un four ; 1 charbon = 80 blocs).
- **Case du four** : l'alcool neutre s'y pose comme un autre carburant ; brûlé, il laisse la **fiole vide** dans la
  case (comme le seau vide de la lave), qui la bloque jusqu'à ce qu'un joueur la retire (clic droit sur le four ou menu
  du train, puis la case).
- **Wagons à entonnoir** : comme le seau de lave, l'alcool neutre est versé seulement dans un four dont la réserve
  est vide (+500 blocs) et **la fiole vide reste dans le wagon à entonnoir**.
- Reconnu par sa marque (`ks_alcools:alcool` = `neutre`) : pas de dépendance à KS_Alcools.

Compilé le 28/10/2026 (`sortie/KS_Trains-1.6.0.jar`). **Déployé sur Kixster le 28/10/2026 à 18:31** (LeKiwi06 : « Oui, déploie KS_Effets 1.0.0, KS_Alcools 1.0.0 et KS_Trains 1.6.0 sur Kixster, et geyser-bedrock 1.6.0 sur le proxy, avec les entrées du didacticiel »), avec `didacticiel.yml` ; fichiers en place vérifiés identiques au dépôt avant l'envoi. À redémarrer par LeKiwi06. Statut : non testé en jeu. 1.5.0 dans `_removed-ks_trains-1.5.0/`. Didacticiel : « Train : carburant » complété.
