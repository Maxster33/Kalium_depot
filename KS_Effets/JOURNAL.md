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

**Compilé le 28/10/2026, non déployé. Statut : non testé en jeu.**
