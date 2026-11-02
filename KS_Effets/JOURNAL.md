# KS_Effets - journal

Serveur Kixster. Moteur des effets custom et `/effects` (étape 1 du chantier « atouts, alcools, aliments », demande de
LeKiwi06 du 28/10/2026 ; cahier local `Atouts_Alcools_Aliments.md`, partie 3.1, à copier ici au déploiement).
Dépend de KLM_Menu (menus en coffre, textes).

## 1.0.0 - moteur des effets custom et /effects (28/10/2026)

- **Pour les autres plugins** (KS_Alcools, KS_Cuisine, KS_Atouts, à venir) :
  - `KSEffets.declarer(id, nom, icone)` : un effet custom, id « plugin:nom » (minuscules, sans point) ;
  - `donner(joueur, id, secondes)` : s'il est déjà actif, le plus long des deux temps est gardé (comme un effet du
    jeu) ; `retirer`, `actif`, `restant`, `actifs` ;
  - `fournisseur(id, fonction)` : effet dont le temps est tenu ailleurs (atout sur l'objet porté) ; la fonction rend
    les secondes restantes, -1 sans limite, 0 inactif ;
  - `EffetFinEvent` (cause : temps écoulé, retiré, mort, lait) : le plugin arrête ce que faisait l'effet.
  Ce que fait l'effet (musique, lumière...) reste dans le plugin qui l'a déclaré.
- **Minuteurs** : en secondes, ne s'écoulent qu'en jeu ; gardés à la déconnexion et au redémarrage (`effets.yml`,
  enregistré chaque minute s'il a changé, à la déconnexion et à l'arrêt).
- **Mort et lait** : un effet custom s'efface, comme un effet du jeu (`effacer-a-la-mort`, `effacer-au-lait` dans
  `config.yml`, vrais par défaut). Choix de Claude, non écrit dans le cahier, **confirmé par LeKiwi06 le 28/10/2026**
  (« Oui, garde l'effacement à la mort et au lait »).
- **`/effects`** (alias `/effets`) : menu en coffre des effets en cours, ceux du jeu (nom traduit, niveau, couleur
  bleue / rouge / grise selon le type, fiole à la couleur de l'effet) puis les custom, avec le temps restant, rafraîchi
  chaque seconde. Clic sur un effet : petit menu « Barre de boss », « Barre d'action », « Ne plus suivre », « Retour ».
  Un seul effet par barre : en choisir un autre remplace le précédent. L'effet suivi brille dans le menu. Quand il se
  termine, son suivi s'arrête. Choix gardés par compte.
- **Barre de boss** : nom de l'effet et temps restant ; la jauge descend (durée totale connue pour un effet custom ;
  pour un effet du jeu, le plus grand temps vu depuis le début du suivi). **Barre d'action** : réécrite chaque seconde.
- **Monde de test** (KS_MondeTest) : effets et choix rangés sous le compte de test du joueur (`CompteTest`).
- Textes dans `lang.yml`.

Limites :
- La barre d'action est partagée avec les messages courts des autres plugins : un tel message est remplacé par le
  suivi à la seconde suivante.
- KS_Fly a sa propre barre de boss (temps de vol) : les deux peuvent s'afficher en même temps.
- Aucun effet custom n'existe encore : ils arriveront avec KS_Alcools, KS_Cuisine et KS_Atouts.
- Entrée du didacticiel (`/effects`) : à ajouter dans `didacticiel.yml` au déploiement (KS_Didacticiel).

Compilé le 28/10/2026. **Déployé sur Kixster le 28/10/2026 à 18:31** (LeKiwi06 : « Oui, déploie KS_Effets 1.0.0, KS_Alcools 1.0.0 et KS_Trains 1.6.0 sur Kixster, et geyser-bedrock 1.6.0 sur le proxy, avec les entrées du didacticiel »), avec `didacticiel.yml` ; fichiers en place vérifiés identiques au dépôt avant l'envoi. À redémarrer par LeKiwi06. Statut : non testé en jeu. Entrées du didacticiel : Menus › « Tes effets », Commandes › « /effects (/effets) ».

## 1.1.0 - cumul des effets nourriture + boisson (02/11/2026)

Demande de LeKiwi06 (chantier « blocs, cultures et objets », cahier local `Blocs_Cultures_Objets.md`, partie 6) : une
nourriture et une boisson qui donnent le même effet le font passer au niveau supérieur ; deux nourritures ou deux
boissons ne se cumulent pas. Décidé le 02/11 : potions vanilla = boissons ; +1 au meilleur niveau, plafonné à III
(balise II + cumul = III) ; le niveau cumulé dure tant que les deux effets sont actifs, puis on revient au niveau d'avant.
Réponses de LeKiwi06 du 02/11 : **tous les effets** se cumulent, nocifs compris (« Oui, tous les effets ») ; les potions
**jetables et persistantes reçues** comptent comme boissons (« Oui, aussi reçues »).

- Nouvelle classe `Cumul`. La source d'un effet est la cause que Paper donne à l'effet (`EntityPotionEffectEvent`) :
  nourriture = `FOOD` (aliments du jeu, ragoût suspect, Casse-croûte doré) ; boisson = `POTION_DRINK`, `POTION_SPLASH`,
  `AREA_EFFECT_CLOUD` (potions, alcools, élixirs, bues ou reçues) ; autre = tout le reste (balise, conduit, plugins,
  commandes...), qui compte seulement pour le « meilleur niveau ».
- Chaque seconde : si une nourriture et une boisson donnent le même effet, niveau = meilleur des trois sources + 1
  (plafond III), pour la plus courte des deux durées (et celle de la source « autre » si c'est elle qui donne le
  meilleur niveau, sinon on pourrait passer 2 s sous une balise et garder III). Le cumul est posé par-dessus l'effet en
  cours : le jeu garde l'effet plus faible « caché » et le remet à la fin (vérifié dans `MobEffectInstance.update`
  du client 26.3). Remangé ou rebu : le cumul est prolongé.
- Pas de cumul pour les effets instantanés ni pour ceux dont le niveau ne change rien : vision nocturne, résistance au
  feu, respiration aquatique, invisibilité, chute lente, grâce du dauphin, cécité, nausée, surbrillance, obscurité.
- Un effet retiré (lait, mort, totem, miel, plugin) oublie ses sources. Temps des sources gardés par compte (compte de
  test à part) dans `cumul.yml`, décomptés seulement en jeu, comme les effets du jeu.
- Aucun texte nouveau (le niveau s'affiche dans /effects et l'inventaire comme un effet du jeu).

Limites :
- Repose sur les causes de Paper : à vérifier en jeu que le Casse-croûte doré (composant `consumable` custom) et le
  ragoût suspect arrivent bien en `FOOD`, et les alcools en `POTION_DRINK`.
- KS_MondeTest retire et remet les effets en changeant de monde : les sources sont alors oubliées (le cumul en cours
  reste jusqu'à sa fin, sans revenir ensuite au niveau d'avant si KS_MondeTest ne l'a pas gardé).
- Construit sur la 1.0.0 jamais testée en jeu, à la demande de LeKiwi06.

Compilé le 02/11/2026. Non déployé. Statut : non testé en jeu.
