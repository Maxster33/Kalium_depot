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
- Joueurs Bedrock : jetons et badges s'affichent comme des livres tant que `geyser-bedrock` n'a pas leurs
  correspondances (Maxster33) ; fusion à l'enclume sur Bedrock à vérifier (leur jeu calcule lui-même l'aperçu).
- `rachats.csv` de KS_Economy : `jeton_emplacement` n'existe plus ; nouveaux jetons et badges absents (valeurs à fixer
  avec LeKiwi06).

**Déployé sur Event le 08/10/2026 à 19:09 (LeKiwi06, avec KS_KaliumGive 1.9.0 ; 1.0.0 et son `config.yml` dans
`_removed-ks_jetons-1.0.0/`), actif après redémarrage d'Event. `config.yml` du serveur remplacé par celui de la 2.0.0
(`prix.claim: 20`) ; `jetons.yml` du serveur était vide (rien à reprendre). Statut : non testé en jeu.**
