# KS_Textures - journal

Plugin de Kixster (survie). Aucune dépendance. Compilé contre le serveur Paper 26.3 (paquets réseau, composants des
objets) : à revérifier à chaque mise à jour de Minecraft.

## 1.0.0 - textures des objets custom (29/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « tu peux faire le texture pack » (après les textures 16×16 de `visuels-objets`).

Choix de LeKiwi06 (questions du 29/10/2026) :

| Question | Choix |
|---|---|
| Hébergement du pack | mc-packs.net |
| Joueur Java qui refuse le pack | Pack facultatif : l'objet garde son image d'aujourd'hui |
| Bedrock | Mêmes textures (geyser-bedrock) |
| Méthode | Un seul plugin d'affichage, aucun autre plugin modifié (plutôt qu'une marque dans chaque plugin, qui aurait empêché les anciens objets de s'empiler avec les nouveaux) |

Fonctionnement :

- **Les objets ne changent pas sur le serveur.** Un gestionnaire placé dans la connexion de chaque joueur (juste avant
  `packet_handler`, par `ChannelInitializeListenerHolder` de Paper) ajoute à chaque objet custom reconnu sa texture en
  1re chaîne de `custom_model_data`, seulement dans ce qui est envoyé au joueur : inventaires et menus, objet tenu à la
  souris, équipement des entités (main, armure), objets au sol, cadres et présentoirs. Les anciens objets sont donc
  couverts aussi.
- **Reconnaissance** (`Regles.java`) : par la marque que chaque plugin écrit déjà dans l'objet (ex.
  `ks_jetons:jeton = fly`, `ks_economy:tier = 4`, `ks_jukeboxboat:bateau` sur un radeau). 60 textures. Pas de texture
  (choix du 29/10) : coffre renforcé et ses véhicules, pomme d'amour (image vanilla à l'aspect enchanté) ; le bateau
  (radeau) à coffre et jukebox prend l'image du bateau (radeau) à jukebox.
- **Créatif** : un objet renvoyé par un joueur en créatif perd la marque d'affichage avant d'arriver au serveur.
- **Pack** : fabriqué par `visuels-objets/pack.py` (`sortie/KaLium-textures-<version>.zip`, hors du dépôt : pixels de
  Mojang). Il remplace la définition vanilla de chaque objet de base (potion, bateaux de tous les bois, lingots...) par
  un choix sur la marque, la définition vanilla restant en dernier recours : sans pack, ou pour un objet ordinaire,
  rien ne change. Format des ressources 26.3 (97).
- **Envoi du pack** : à l'arrivée sur Kixster, à chaque joueur Java, facultatif, si `config.yml` donne `pack.url` et
  `pack.sha1` (vides par défaut : pack non proposé). Pas aux joueurs Bedrock (identifiant Floodgate).

Limites :

- Pas couverts (image d'aujourd'hui) : échanges des villageois, livre de recettes, objets montrés dans l'infobulle
  d'un sac ou d'une shulker.
- Après un clic dans un inventaire, le serveur renvoie les cases cliquées (l'objet du joueur porte la marque, pas
  celui du serveur) : quelques paquets de plus, rien de visible attendu.
- Ne pas recharger à chaud (`/reload`).
- Test hors jeu impossible (le jeu ne lie les composants des objets qu'au chargement de ses données) : rien n'a été
  vérifié en jeu.

À déployer avec : le pack sur mc-packs.net (adresse et SHA-1 dans `config.yml`), et geyser-bedrock 1.8.0 sur le
proxy pour Bedrock (mêmes textures, reconnues par la même marque).

Compilé le 29/10/2026 (`sortie/KS_Textures-1.0.0.jar`). **Statut : non déployé, non testé en jeu.**
