# JOURNAL — KS_Votes

Plugin Paper de **Kixster** : récompenser les votes sur les sites de référencement.
Cahiers (locaux, hors dépôt) : `KS_Votes.md` (sites, réception, Bedrock) et `Blocs_Cultures_Objets.md`, section 1
« /vote », qui remplace les montants et les paliers de `KS_Votes.md`.

## 1.0.0 — 02/11/2026 (Claude de LeKiwi06) — déployée sur Kixster le 03/11/2026 à 14:37, à redémarrer, non testée en jeu

**Demande** (LeKiwi06, 31/10 et 02/11/2026) : un coffre de butin par vote, par site et par jour ; valeur selon la
série ; menu `/vote` en coffre.

**Réponses de LeKiwi06 du 02/11/2026** aux questions ouvertes de `KS_Votes.md` :
- les 5 sites proposés (serveur-prive.net, serveurs-minecraft.org, top-serveurs.net, serveur-minecraft-vote.fr,
  minecraft-mp.com) ;
- top du mois : classement + coffres de **250 / 150 / 75** aux trois premiers ;
- vote d'un pseudo jamais venu : gardé jusqu'à sa première venue (30 jours, `attente-jours`) ;
- pseudo sans point : d'abord un joueur Java de ce nom, sinon le joueur Bedrock « .pseudo » (l'oubli du point par un
  joueur Bedrock est bien plus probable qu'un même pseudo sur les deux versions).

**Fonctionnement :**
- **Coffres** : `KSRewardsGUI.deposerCoffre` (comme KS_Parrainage et les dons du staff), origine « Votes », raison
  « Vote sur <site> » ; un coffre refusé (KS_RewardsGUI désactivé, fusion.yml vide) est réessayé chaque minute.
- **Un vote récompensé par site et par jour** (minuit, heure de Paris, `fuseau`), même si le site permet de voter
  plus souvent.
- **Série** : jours de suite avec au moins un vote ; un seul jour manqué la remet à zéro. Valeur du coffre (`paliers`) :
  5 (1-2 jours), 10 (dès 3), 15 (dès 7), 20 (dès 14, plafond). Le coffre d'un vote prend la série du jour de ce vote.
- **Top du mois** (votes récompensés, départage : le premier arrivé à ce nombre) : coffres `top-coffres` donnés dans la
  première minute du mois suivant (au premier démarrage après, si le serveur était arrêté).
- **Réception** :
  - NuVotifier : l'événement `VotifierEvent` du plugin « Votifier » installé sur Kixster, lu par réflexion (aucun jar
    de NuVotifier pour compiler). Le site est reconnu par son nom de service (`votifier` de chaque site, contenu dans
    le nom, sans tenir compte des majuscules) ; un service inconnu est écrit dans la console avec son nom exact.
  - API (sites sans Votifier) : adresse `api.url` avec `{ip}`, `{pseudo}`, `{cle}`, interrogée hors du fil principal
    pour chaque joueur en ligne pas encore récompensé ce jour-là sur ce site, toutes les `api-intervalle-secondes`
    (300) et à l'ouverture de `/vote` (au plus une fois par 30 s) ; vote accepté si l'expression régulière `api.ok`
    est trouvée dans la réponse (code HTTP 2xx).
- **Pseudos** : chaque joueur venu est retenu (UUID + pseudo) dans `votes.yml` ; un vote d'un joueur connu hors ligne
  est déposé tout de suite dans son /rewards.
- **Menu `/vote`** (coffre de 3 rangées, `KLM_Menu` Contenant) : « Ma série » (série, prochain coffre, prochain palier,
  votes du mois, rappel de voter aujourd'hui, pseudo sans point pour Bedrock), « Top du mois » (10 premiers + coffres
  de fin de mois), un objet par site ayant un `lien` (« Disponible » + valeur du coffre, ou « Déjà voté aujourd'hui »
  + temps avant minuit ; pseudo à taper ; clic = lien dans le tchat, cliquable en Java, lisible en Bedrock), « Fermer ».
  Sans aucun lien réglé : « Bientôt disponible ».
- **Annonce** dans le tchat à chaque vote récompensé (`annonce`), cliquable vers `/vote`.
- **Admin** (`ksvotes.admin`, op) : `/votes test <joueur> <site>` (vote simulé : donne un vrai coffre),
  `/votes serie <joueur> [jours]` (voir, ou corriger : la série corrigée compte aujourd'hui), `/votes recharger`.

**Données** : `plugins/KS_Votes/votes.yml` (joueurs, comptes par mois, votes gardés, coffres à réessayer, dernier top
donné). Textes : `lang.yml`.

**Limites :**
- Sites à API vérifiés par IP : deux joueurs derrière la même box seraient tous deux récompensés par un seul vote ; la
  vérification par pseudo (`{pseudo}`) ne dépend que de ce que le site propose.
- Aucune adresse d'API n'est fournie par défaut : la documentation de chaque site n'a pas pu être vérifiée ; elles se
  recopient depuis la page « API / Votifier » de chaque site à l'inscription.
- Un vote NuVotifier transmis par messages de plugin n'arrive sur Kixster que lorsqu'un joueur y est connecté (NuVotifier
  les garde en attente sur le proxy jusque-là).

**À ajouter ailleurs (fichiers communs, non modifiés ici)** : étape `/vote` dans `didacticiel.yml` (KS_Didacticiel).
Aucun objet custom, aucune recette, aucun prix.

**Déploiement du 03/11/2026** (accord de LeKiwi06) :
- 14:34 : NuVotifier (AzuVotifier 3.3.6, Modrinth, empreintes vérifiées) sur le proxy (`/plugins/nuvotifier/`, port 40312
  en TCP, même numéro que Geyser en UDP ; `pluginMessaging`, cache fichier, `onlySendToJoinedServer = false`) et sur
  Kixster (`/plugins/Votifier/`, `port: -1`, `pluginMessaging`). Clés RSA générées en local puis la privée effacée du PC ;
  la clé publique à donner aux sites est dans `sortie/votes/public.key` (hors dépôt).
- 14:37 : `KS_Votes-1.0.0.jar` sur Kixster (nouveau plugin, rien à ranger). Proxy et Kixster à redémarrer.
- Les liens et noms de service des sites (`config.yml`) restent vides : à remplir à l'inscription sur chaque site.
