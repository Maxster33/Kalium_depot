# Règles du projet KaLium

Valables pour chaque session de travail, quel que soit le Claude ou l'humain (LeKiwi06, Maxster33).
Chaque règle est suivie de sa raison (« Pourquoi ») : en cas de situation non prévue, c'est la raison qui guide.

## 1. Avant de commencer

1. `git pull`, puis lire dans l'ordre : ce fichier, `TRAVAIL_EN_COURS.md`, `REPRISE_PROJET.md`, puis le
   `JOURNAL.md` du ou des plugins concernés.
2. **Réserver le ou les plugins** dans `TRAVAIL_EN_COURS.md` avant de toucher au code :
   - vérifier qu'aucun des plugins voulus n'y figure déjà ; s'il y figure, **ne pas commencer** et avertir son humain ;
   - ajouter une ligne `- <Plugin> — <pseudo> — depuis le <aaaa-mm-jj hh:mm> — <objet du travail>` ;
   - commit + `git push` **immédiatement**. Si le push est refusé : `git pull`, revérifier ; si l'autre a réservé
     le même plugin entre-temps, retirer sa ligne et avertir son humain.
   - Une demande qui touche plusieurs plugins : les réserver **tous** en une fois, ou aucun.
   - **Deux catégories** (décision de LeKiwi06, 24/09/2026) :
     - **Utilisés actuellement** : plugins modifiés en ce moment. Personne d'autre n'y touche.
     - **Requis parfois** : plugins dont le chantier aura besoin ponctuellement (petite retouche, compilation contre
       eux, déploiement groupé). Personne d'autre ne les modifie sans demande acceptée (voir ci-dessous).
     La limite de 2 plugins (ci-dessous) ne compte que les « utilisés actuellement ».
   - **Demandes de créneau** : pour toucher un plugin réservé par l'autre, ajouter une ligne dans la section
     « Demandes » de `TRAVAIL_EN_COURS.md` (plugin, qui demande, créneau précis, ex. « de 13 h 50 à 14 h 00 »,
     objet) et pousser, puis attendre la réponse. Le Claude de l'autre personne vérifie cette section à chaque
     `git pull` : il peut répondre « accord » lui-même si le créneau ne gêne pas son travail en cours (sinon il
     demande à son humain), note sa réponse avec l'heure et pousse ; pendant un créneau accordé il ne touche pas au
     plugin. Pas de réponse = pas d'accord. Le demandeur retire sa ligne à la fin du créneau.
   - **De 13 h à 23 h (heure de Paris), 2 plugins réservés au maximum par personne.** Au-delà, il faut l'accord
     de l'autre personne, noté dans la réservation (« accord de <pseudo> »). En dehors de ces heures, pas de
     limite. Attention à l'heure utilisée (réservations comprises) : l'horloge d'un espace cloud est souvent en UTC,
     à convertir en heure de Paris ; sous Git Bash (Windows), `TZ=Europe/Paris date` renvoie en réalité l'heure UTC
     (fuseaux horaires absents) : utiliser `date` tout court, le PC étant réglé à l'heure de Paris.

     Pourquoi : de 13 h à 23 h, LeKiwi06 et Maxster33 sont actifs en même temps ; la limite évite qu'une personne
     bloque tout le projet. Le découpage en petits plugins (section 2) la rend peu contraignante.
   - Ne **jamais** supprimer la réservation de quelqu'un d'autre, même ancienne : avertir son humain, qui voit
     avec l'autre personne.

   Pourquoi : deux sessions qui modifient le même plugin en parallèle produisent des versions contradictoires
   (et des conflits git). Une réservation n'est visible de l'autre qu'une fois poussée sur GitHub. Les catégories
   et les créneaux permettent de partager un plugin sans double écriture.

## 2. Pendant le travail

1. **Ne rien construire qui n'a pas été explicitement demandé.** En cas de doute ou d'ambiguïté : poser la
   question avant d'implémenter, jamais deviner. Ce qui est techniquement nécessaire pour réaliser la demande
   est permis mais doit être signalé. Un bug ou une amélioration repérés en passant sont **signalés, jamais
   corrigés d'office**.

   Pourquoi : chaque ajout non demandé est du code à relire, tester et maintenir, et peut contredire un choix
   que l'utilisateur n'a pas encore fait.

2. **Un plugin = un rôle.** Tout nouveau jeu ou nouvelle fonction indépendante devient un plugin séparé ; les
   plugins interagissent entre eux plutôt que d'être fusionnés. Pour les jeux de Kal-Games : un socle commun
   (`KG_Core` : monde des instances, arènes, hub, menus, textes, classements) dont dépendent les jeux
   (`KG_PvpKit`, `KG_Rush`...). KalGames actuel est découpé **progressivement**, quand on retouche une de ses
   parties (en commençant par le Rush), jamais d'un bloc.

   Pourquoi : un plugin surchargé fait qu'une mise à jour ou un bug d'un jeu touche tous les autres jeux et
   menus. Limite : tous les plugins partagent le même serveur ; un blocage du serveur (watchdog) les arrête
   tous, quel que soit le découpage.

3. **Textes vus par les joueurs : en français, avec accents.** Les commentaires du code sont en français
   (accents facultatifs).

4. **Le dépôt est public** : jamais de mot de passe, jeton, adresse e-mail ni identifiant de connexion dans un
   fichier, un commit ou un compte rendu.
   - Les secrets (ex. `relay-token`) n'existent que dans les fichiers de configuration **sur les serveurs**. Dans
     le code et les `config.yml` fournis avec les plugins, leur valeur par défaut est **vide** (`""`) ou générée
     aléatoirement par le plugin ; un plugin n'affiche jamais un secret dans la console.
   - Un fichier récupéré sur un serveur (`config.yml`, `relay.properties`, logs...) ne passe **jamais** par le
     dossier du dépôt : le télécharger ailleurs (ex. Téléchargements).

   Pourquoi : le 24/09/2026, le jeton du relais a dû être changé en urgence : une valeur fixe avait été écrite
   dans le code et les `config.yml` du dépôt « pour éviter une étape de copier-coller ».

## 3. Versions et configuration

1. Chaque nouvelle version d'un plugin : `VERSION=` dans `build.sh`, `<version>` dans `pom.xml`, et une section
   dans son `JOURNAL.md` (demande, cause, changements, limites, date de déploiement).
2. Chaque version porte un **statut de test** dans son `JOURNAL.md` et dans `REPRISE_PROJET.md` :
   « non testé en jeu » ou « testé et confirmé par <pseudo> le <date> ». On n'empile pas de nouvelle
   fonctionnalité sur une version jamais testée, sauf demande explicite.

   Pourquoi : un bug découvert après plusieurs versions non testées est beaucoup plus difficile à situer.

3. **Nouvelle clé de `config.yml`** : `saveDefaultConfig()` n'ajoute jamais une clé à un `config.yml` déjà
   présent sur le serveur. Toute nouvelle clé doit avoir une valeur par défaut dans le code, et être ajoutée à
   la main dans le fichier déployé si elle doit être modifiable.

   Pourquoi : c'est déjà arrivé (KalGames 1.10.3, KalBingo 0.1.10) : le code était correct mais la clé absente
   du serveur, et la fonction restait silencieusement désactivée.

4. **Ne jamais pousser du code qui ne compile pas** : compiler le plugin (`sh <plugin>/build.sh`) avant chaque
   commit qui touche à son code. Procédure : `REPRISE_PROJET.md`, section « Compiler ».

   Pourquoi : sinon l'autre personne récupère un dépôt cassé sans savoir d'où vient l'erreur.

5. **Plugins à déployer ensemble** : si un changement exige de mettre à jour plusieurs plugins en même temps
   (ex. KalGames 1.10.5 + KalBingo 0.1.15), le noter dans chacun des `JOURNAL.md` concernés et les déployer
   dans la même opération.

   Pourquoi : des serveurs à des versions incompatibles ont déjà produit des bugs difficiles à comprendre
   (salle d'attente fantôme du Bingo).

## 4. Déploiement

LeKiwi06 et Maxster33 peuvent tous deux déployer (WinSCP). Claude peut piloter WinSCP mais ne saisit jamais de
mot de passe : sessions enregistrées, ou mot de passe tapé par l'humain.

1. On ne déploie qu'un plugin **qu'on a réservé** dans `TRAVAIL_EN_COURS.md`.
2. Avant de déployer : actualiser le panneau distant (**Ctrl+R**) et vérifier que le jar en place est bien
   celui de `jars-deployes/`. Sinon, s'arrêter et demander (l'autre a peut-être déployé autre chose).
3. **Non destructif** : sur un serveur, on ne supprime jamais rien (jars, config, mondes, données, logs) : on
   déplace dans un dossier `_removed-…`. Pour un plugin : `/plugins/_removed-<plugin en minuscules>-<ancienne
   version>/` (ex. `_removed-kalgames-1.12.2`), où l'on copie aussi le `config.yml` avant de le modifier. Une
   suppression définitive n'est faite que par l'humain.

   Pourquoi : il n'existe aucune sauvegarde des serveurs accessible par SFTP ; un fichier supprimé est perdu.

   **On ne garde que les 2 derniers dossiers `_removed-…` de chaque plugin** sur chaque serveur Minestrator
   (décision de LeKiwi06, 24/09/2026). À chaque déploiement, celui qui déploie liste les dossiers en trop (les plus
   anciens) ; leur suppression définitive reste faite par l'humain. Attention : le dépôt n'existe que depuis le
   23/09/2026, les versions plus anciennes n'existent QUE dans ces dossiers ; si on veut les garder, les télécharger
   sur un PC avant de les supprimer.
4. Ne jamais taper d'accents dans l'éditeur intégré de WinSCP (texte corrompu) : un texte accentué se change
   dans le code source, puis on recompile.
5. Claude ne redémarre jamais un serveur : c'est l'humain qui le fait.
6. Ne pas toucher au serveur Kal-Test-Dev sauf demande explicite.
7. Dans le même push, juste après le déploiement : copie du nouveau jar dans `jars-deployes/` (ancien jar
   retiré de ce dossier) et mise à jour du tableau des versions de `REPRISE_PROJET.md`.

   Pourquoi : `jars-deployes/` et `REPRISE_PROJET.md` sont la seule façon pour l'autre équipe de savoir ce qui
   tourne réellement sur les serveurs.

## 5. Fin de session

1. `git pull`, puis commit avec un message clair en français, puis `git push`.
2. **Libérer ses réservations** dans `TRAVAIL_EN_COURS.md` (même si le travail est abandonné), et pousser.
3. Mettre à jour `REPRISE_PROJET.md` :
   - partie **permanente** (état actuel, versions en service, points ouverts, procédures) : toujours à jour,
     jamais archivée ;
   - ajouter son **compte rendu signé** : `### <aaaa-mm-jj> — <pseudo>`.
4. **Archivage** : si le compte rendu le plus récent de l'autre personne est postérieur à ses propres comptes
   rendus, on déplace **ses propres** anciens comptes rendus dans `archive_reprise.md` (jamais ceux de l'autre).

   Pourquoi : l'autre a forcément lu ces comptes rendus avant d'écrire le sien ; les archiver évite aux
   sessions suivantes de lire des informations dépassées. Les archives ne se consultent qu'en cas de doute.

5. Ajouter un fichier dans `historique-conversations/` (voir son README).

## Rôle de chaque fichier

| Fichier | Contenu |
|---|---|
| `REGLES.md` | Ces règles |
| `TRAVAIL_EN_COURS.md` | Plugins réservés en ce moment |
| `REPRISE_PROJET.md` | État actuel du projet + derniers comptes rendus signés |
| `archive_reprise.md` | Anciens comptes rendus (consultation en cas de doute) |
| `<plugin>/JOURNAL.md` | Détail technique de chaque version d'un plugin |
| `historique-conversations/` | Un fichier par session : demandes, décisions, reste à faire |
| `jars-deployes/` | Copie exacte des jars en service |
