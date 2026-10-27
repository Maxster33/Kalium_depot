# KS_DisquesBundle - journal

Plugin de la survie (Kixster) : disques de musique empilables par 64 dans les bundles. Aucune dépendance.

## 1.0.0 - disques à 1/64 du bundle (27/10/2026, LeKiwi06) - compilé, non déployé, non testé en jeu

Demande de LeKiwi06 : « fait en sorte que les disques agissent comme un item stackable par 64 dans les bundle ».

Lu dans le code du jeu 26.3 (`BundleContents.getWeight`) : le poids d'un objet dans un bundle vaut 1 / sa taille de
pile (rien d'autre, sauf bundles imbriqués et ruches) ; un disque (pile de 1) remplit donc le bundle à lui seul.

Question posée avant le code :

| Question | Réponse de LeKiwi06 |
|---|---|
| Disques empilables par 64 partout, ou seulement dans le bundle ? | Seulement dans le bundle |

- **Entrée** : juste avant un clic gauche qui met un disque dans un bundle (disque tenu sur le bundle, ou bundle tenu
  sur le disque), le disque reçoit une taille de pile de 64 (composant `max_stack_size`) ; l'insertion reste celle du
  jeu : poids 1/64, bundle plein à 64 disques, disques identiques regroupés en une pile, barre de remplissage juste.
- **Sortie** : le jeu sort une pile entière (clic droit sur le bundle, bundle tenu en clic droit sur une case vide,
  bundle vidé à la main, bundle détruit). Le tick suivant un clic qui touche un bundle ou un disque, tout disque à 64
  trouvé hors d'un bundle (curseur, inventaire du joueur, contenant ouvert) reprend sa taille de pile du jeu et est
  séparé en disques à l'unité : un reste à sa place, les autres vont dans l'inventaire du joueur (à ses pieds s'il est
  plein). Un disque à 64 qui apparaît au sol est séparé aussitôt en objets d'un disque.
- Vérification aussi à la connexion (filet de sécurité si le serveur s'arrête entre la sortie et la vérification).

Techniquement nécessaire, signalé :

- Sortir d'un bundle une pile de plusieurs disques en met donc un sur le curseur (ou dans la case visée) et range les
  autres dans l'inventaire.
- Nouveau plugin (« un plugin = un rôle ») ; nom choisi par Claude.

Limites connues, à vérifier au test :

- **Joueurs Bedrock** : leur jeu calcule peut-être lui-même le poids (disque = bundle plein) et refuse l'insertion
  avant même de l'envoyer au serveur ; dans ce cas, rien à faire côté serveur.
- **Mode créatif** : l'inventaire créatif est géré par le jeu du joueur, l'insertion d'un disque n'y est pas
  modifiée (les disques sortis sont tout de même remis à l'unité).
- Un joueur Java voit un instant le refus prévu par son jeu, puis l'état réel envoyé par le serveur.
