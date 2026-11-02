# KS_RepeteurWifi - journal

Plugin de Kixster (survie). Répéteur wifi (cahier local `Blocs_Cultures_Objets.md`, partie 7) : bloc custom du socle
KS_Blocs. Dépend de KLM_Menu (textes, menu en coffre) et de KS_Blocs ; SimpleClaimSystem facultatif.

## 1.0.0 - répéteur wifi (02/11/2026)

**Demande de LeKiwi06** (liste du 01/11, choix validés le 02/11) : craft 1 capteur sculk calibré, 3 répéteurs,
3 comparateurs, 2 lingots d'or ; portée 64 blocs, en envoi ou en réception, code à 6 caractères ; bloc custom ; côté
serveur, le récepteur est un vrai bloc qui émet du courant ; limiter le rythme (horloges). Un clic ouvre un menu en
coffre (émission / réception) et un bouton « Code » ouvre une enclume pour taper les 6 caractères.

### Méthode : un bloc cible sur le serveur

Vérifié dans le code du jeu 26.3 (`net.minecraft.world.level.block.TargetBlock`) : le bloc cible est un bloc plein,
source de redstone, avec une propriété `power` (0 à 15) émise vers les 6 faces (`ownSignal`). Elle ne redescend que
par un tick programmé quand un projectile le touche, et `onPlace` ne la remet à 0 que si le bloc précédent n'était pas
déjà un bloc cible. Le plugin règle donc lui-même la puissance (`AnaloguePowerable.setPower`) : 15 pour un récepteur
allumé (comme un bloc de redstone), 0 sinon ; les projectiles sont sans effet (`TargetHitEvent` annulé).

KS_Blocs montre aux joueurs, à la place du bloc cible, l'état du bloc musical réservé à l'apparence du répéteur (sa
réécriture compare le bloc du serveur à la base du type, ici `TARGET`). Les vrais blocs cibles du monde ne sont pas
touchés. Trois types KS_Blocs, une apparence chacun : émetteur, récepteur éteint, récepteur allumé (un type n'a qu'une
apparence, et les textures d'une apparence sont communes à ses variantes).

### Fonctionnement

- Objet : bloc cible marqué par KS_Blocs, image provisoire du capteur sculk calibré (`item_model`). Posé, c'est un
  émetteur sans code. Cassé (ou explosé), il redonne un répéteur neuf, sans code.
- Clic droit : menu en coffre de 3 rangées (Émission, Code, Réception ; Aide, Fermer). Accroupi avec un objet en main,
  le clic pose l'objet contre le répéteur. Dans un claim SimpleClaimSystem : propriétaire, membres et
  `ks_repeteurwifi.admin` seulement.
- Code : enclume sans bloc, papier au nom du code actuel (« Code » s'il n'y en a pas). Le résultat montre le code tapé ;
  validation par un clic sur le résultat ou en fermant l'enclume avec un code valable (le jeu Bedrock refuse de prendre
  un résultat à 0 niveau ; Geyser envoie le texte au serveur pendant la frappe). 6 lettres ou chiffres, mis en
  majuscules (« abc123 » = « ABC123 »). Le code est écrit dans les données du chunk (`ks_repeteurwifi:codes`).
- Transmission, chaque tick : un émetteur avec un code est actif s'il reçoit du courant d'un voisin (comme une lampe) ;
  un récepteur veut être allumé si un émetteur actif du même code est à 64 blocs ou moins (ligne droite, même monde).
- Rythme : un récepteur change d'état au plus une fois toutes les 10 ticks (`intervalle-minimum-ticks`, 0,5 s). Une
  horloge plus rapide est ralentie ; une impulsion plus courte n'est pas perdue (récepteur allumé 0,5 s).
- Pistons : le code suit le bloc. Un récepteur allumé peut alimenter un émetteur voisin (relais vers un autre code).

### Limites

- **Ne démarre pas tant que `apparences.json` de KS_Blocs n'a pas les trois apparences** (voir « À ajouter » plus
  bas) : le plugin écrit « Types de blocs refuses par KS_Blocs » et se désactive.
- Durée de casse : le jeu du joueur prédit celle d'un bloc musical (0,8, hache), le serveur casse un bloc cible (0,5,
  houe). À la hache, le bloc peut réapparaître un instant puis casser un peu plus tard (le serveur finit la casse).
- Changement d'aspect allumé / éteint : KS_Blocs n'a pas de « changer de type » ; le plugin retire puis réenregistre
  le bloc dans le même tick (deux renvois du bloc) : vérifier qu'il ne clignote pas.
- Un émetteur et un récepteur du même code côte à côte s'entretiennent (comme une boucle de redstone).
- Un émetteur dans un chunk déchargé ne transmet plus. L'encapsuleur ne garde pas le code (à retaper).
- Poudre de redstone : le serveur la tourne vers le bloc cible (source de redstone), mais le jeu du joueur voit un bloc
  musical et peut la prédire autrement au moment de la poser ; le courant passe dans tous les cas. À regarder en jeu.

### À ajouter ailleurs (plugins communs)

- **KS_Blocs, `src/main/resources/apparences.json`** (réservé par la session des cultures), puis
  `python KS_Blocs/pack/blocs.py` et `sh geyser-bedrock/build.sh` :

  ```json
  "repeteur_wifi_emetteur": {"bloc": "note_block", "modele": "cube",
    "textures": {"all": "repeteur_wifi_emetteur"}, "variantes": {"defaut": "instrument=bit,note=1,powered=false"}},
  "repeteur_wifi_recepteur": {"bloc": "note_block", "modele": "cube",
    "textures": {"all": "repeteur_wifi_recepteur"}, "variantes": {"defaut": "instrument=bit,note=2,powered=false"}},
  "repeteur_wifi_recepteur_allume": {"bloc": "note_block", "modele": "cube",
    "textures": {"all": "repeteur_wifi_recepteur_allume"}, "variantes": {"defaut": "instrument=bit,note=3,powered=false"}}
  ```
- Prix (KS_Economy) : somme des ingrédients du barème, 0,25 + 3 × 0,3425 + 3 × 0,365 + 2 × 0,4 = **3,1725** (proposé,
  à valider). Objet reconnu par sa marque KS_Blocs `ks_blocs:type = ks_repeteurwifi:emetteur`.
- Didacticiel (KS_Didacticiel) : une entrée « Répéteur wifi » (craft, clic droit, code, portée, rythme).
- Textures : 3 faces de bloc 16 × 16 dans `visuels-objets/png/blocs/` (`repeteur_wifi_emetteur.png`,
  `repeteur_wifi_recepteur.png`, `repeteur_wifi_recepteur_allume.png`, provisoires créées par `blocs.py` si elles
  manquent) ; icône de l'objet `visuels-objets/png/repeteur_wifi.png` (16 × 16), avec sa règle dans KS_Textures
  (`Regles.java`, objet `minecraft:target` marqué `ks_repeteurwifi:emetteur`, modèle `repeteur_wifi`) et sa
  correspondance Bedrock (`geyser-bedrock/custom_mappings/kalium_objets.json`, section `minecraft:target`, icône
  `kalium.repeteur_wifi` dans `pack/textures/item_texture.json`).

**Compilé le 02/11/2026, non déployé, non testé en jeu.**
