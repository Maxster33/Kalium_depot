# KS_Fly - journal

Plugin du serveur Event : vol dans ses claims. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de
mort » de LeKiwi06 (ajouts « jetons et badges » du 08/10/2026) : `KS_Jetons/CAHIER_DES_CHARGES.md`.

## 1.0.0 - vol dans ses claims avec un jeton ou un badge de fly (08/10/2026, LeKiwi06)

Demande de LeKiwi06 (08/10/2026) et ses réponses du même jour.

- **Où** : dans les claims dont on est **propriétaire ou membre** (SimpleClaimSystem), en survie ou aventure. On vole
  comme en créatif : double saut.
- **Jeton de fly** (KS_Jetons, inventaire spécial) : **10 minutes** de vol (`jeton-minutes`), **d'affilée** à partir de
  l'activation (le temps court aussi au sol, hors de ses claims, déconnecté). Il s'active par une **fenêtre** qui
  s'ouvre quand le joueur essaie de voler sans temps de jeton ni réserve de badge ; case « Ne plus m'afficher cette
  fenêtre » : un jeton est alors activé d'office à chaque essai.
- **Badge de fly** (porté = rangé dans l'inventaire spécial) : 1, 3, 5, 10, 15 minutes de vol par heure selon son
  niveau (valeurs dans le `config.yml` de KS_Jetons), **décomptées seulement en vol** ; la réserve se **remplit d'un
  coup 1 heure après le début de son utilisation** (`badge-recharge-minutes`) ; jamais au-delà du maximum du badge.
  Délai tenu **par joueur** : changer de badge ne recharge rien.
- **Ordre** : le badge sert avant un jeton ; un jeton déjà activé continue de servir (son temps court de toute façon,
  la réserve du badge est alors épargnée) et, quand il finit en plein vol, la réserve du badge prend la suite s'il en
  reste.
- **Compteur en barre de boss** : temps du jeton activé (bleue, tout le temps de son activation) ou réserve du badge
  (jaune, pendant le vol).
- **Fin** : temps écoulé, réserve vide ou sortie de ses claims en vol : le vol s'arrête avec un message et **les
  dégâts de chute s'appliquent**.
- **`/fly`** (bouton « Fly » du menu) : jetons, temps du jeton activé, réserve du badge et temps avant qu'elle soit
  pleine, réglage de la fenêtre (« Activer d'office » / « Remettre la fenêtre »). Nécessaire techniquement, non
  demandé : sans cela, « Ne plus m'afficher cette fenêtre » ne pouvait plus être annulé.
- Nécessaire techniquement : le serveur ne voit le double saut que si le vol est autorisé. Il l'est donc, dans ses
  claims, pour le joueur qui a de quoi voler (temps de jeton, réserve de badge, ou jeton en stock) et retiré dès que ce
  n'est plus le cas (4 vérifications par seconde). Un vol autorisé supprime d'ordinaire les dégâts de chute : ils sont
  remis pendant ce temps (`setFlyingFallDamage`). Marqueur sur le joueur : après un arrêt brutal du serveur, le vol
  resté autorisé est retiré à la connexion. Créatif et spectateur : rien n'est touché.
- `depend` KLM_Menu, KS_Jetons, SimpleClaimSystem ; `softdepend` KS_Menu. Données : `plugins/KS_Fly/fly.yml`. Aucun
  nouvel objet (rien à ajouter dans `geyser-bedrock`).

Limites, à vérifier en jeu :
- Double saut depuis un compte Bedrock (Geyser).
- SimpleClaimSystem a son propre vol de claim (`/claim fly`, réglage « Vol ») : non utilisé ; à vérifier qu'il ne retire
  pas le vol donné ici.
- Un joueur sans jeton ni réserve ne voit rien se passer quand il double-saute (aucune fenêtre : il n'a rien à activer).

**Déployé sur Event le 08/10/2026 à 23:38 (LeKiwi06, déploiement groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.3.1, KS_Claim 1.2.0 ; nouveau), actif après redémarrage d'Event.
Statut : non testé en jeu.**

**Envoyé sur Kixster le 09/10/2026 à 17:31 (LeKiwi06, envoi groupé de la catégorie 7 : KS_Jetons 2.0.1, KS_KaliumGive 1.9.0, KS_CoffreMort 1.0.1, KS_Fly 1.0.0, KS_Teleport 1.0.0, KS_Economy 1.4.0, KS_Claim 1.2.0 ; demande de LeKiwi06 : « envoie la catégorie 7 sur kixster » ; 1.0.0 ; nouveau sur Kixster), actif après redémarrage de Kixster. Jars en place vérifiés identiques aux références avant l'envoi. Statut : non testé en jeu.**
