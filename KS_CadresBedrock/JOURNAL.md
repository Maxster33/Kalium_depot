# KS_CadresBedrock - journal

Plugin de Kixster (préfixe `KS_`) : les cadres invisibles s'affichent comme des cadres normaux chez les joueurs Bedrock ;
ce plugin cache ces cadres aux joueurs Bedrock et leur montre l'objet tenu par un porte-armure invisible. Joueurs Java
jamais concernés. Aucune dépendance (joueur Bedrock reconnu à son UUID Floodgate, comme dans KLM_CollisionsBedrock).

## 0.1.0 - prototype dans « Kixster Test » (29/10/2026, LeKiwi06)

**Demande de LeKiwi06 (29/10/2026)** : « peut-on fix les cadres invisible pour les joueurs bedrock ? », puis, après
l'explication des pistes : « Fais un prototype du plugin (support invisible côté Bedrock) dans Kixster Test, sans
déployer ailleurs. »

**Cause** (jar de Geyser 2.12.0-b1269 du proxy lu le 29/10/2026, `ItemFrameEntity`) : pour un joueur Bedrock, Geyser
remplace chaque cadre par un bloc « cadre » (avec son objet) et ne lit jamais l'invisibilité ; Bedrock n'a pas de cadre
invisible. Les cadres invisibles (recette de KS_Crafts : 8 bâtons autour d'une membrane de phantom) apparaissent donc
comme des cadres normaux. Pistes écartées : texture transparente dans le pack Bedrock (tous les cadres deviendraient
invisibles, vides compris), fork de Geyser (rien côté Bedrock pour afficher un cadre invisible, à refaire à chaque mise
à jour).

**Changements** :
- Dans les mondes de `mondes` (« Kixster Test » seulement), chaque cadre invisible (cadre lumineux compris) qui contient
  un objet reçoit un porte-armure invisible qui tient une copie de l'objet dans la main droite. Le porte-armure n'est
  jamais envoyé aux joueurs Java (`setVisibleByDefault(false)`), jamais sauvegardé avec le chunk, ne tombe rien à sa mort.
- Pour les joueurs Bedrock : cadre caché (`hideEntity`), porte-armure montré (`showEntity`). Un cadre invisible **vide**
  reste visible (cadre normal), pour qu'ils puissent y poser un objet. Mise à jour toutes les 10 ticks (`intervalle`).
- Le joueur Bedrock vise le porte-armure : clic droit = tourner l'objet (un `PlayerInteractEntityEvent` sur le cadre est
  lancé d'abord : un claim ou un autre plugin peut refuser) ; coup = coup donné au cadre (`Player#attack` : l'objet
  tombe comme en vanilla, claims respectés).
- `config.yml` : `petit`, `main.cote`, `main.hauteur`, `ecart-mur`, `intervalle` (valeurs par défaut aussi sans fichier).
  `/cadresbedrock recharger` (op) relit le fichier et replace les porte-armures, sans redémarrage ; `/cadresbedrock info`.

**Limites connues** :
- Geyser ne rend les poses des bras qu'avec le pack optionnel de Geyser (GeyserOptionalPack, pas sur le proxy à ma
  connaissance) : l'objet est tenu comme dans une main (de biais, pointé vers l'avant), pas à plat contre le mur. La
  rotation de l'objet ne se voit pas côté Bedrock. Une carte s'affiche comme l'objet carte, pas comme son dessin.
- Les décalages sont estimés (modèle du porte-armure), à régler en jeu.
- Le porte-armure n'est pas un marqueur (sinon les joueurs Bedrock ne pourraient pas le viser) : il a une petite boîte
  côté serveur, qu'une flèche pourrait toucher (dégâts annulés).

**Statut** : non testé en jeu. Envoyé sur Kixster le 29/10/2026 à 01:25 (`/plugins/KS_CadresBedrock-0.1.0.jar`, nouveau
plugin, rien de remplacé) ; actif après le redémarrage de Kixster par l'humain. À tester dans « Kixster Test » avec un
joueur Bedrock et un joueur Java : rendu, position (régler `config.yml` puis `/cadresbedrock recharger`), clic droit,
coup, cadre vide.
