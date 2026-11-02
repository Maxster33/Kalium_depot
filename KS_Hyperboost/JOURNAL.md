# KS_Hyperboost - journal

Plugin de Kixster : la fusée Hyperboost. Cahier des charges : partie 5 du cahier local `Blocs_Cultures_Objets.md`
(validé par LeKiwi06 les 01-02/11/2026 : montures seulement, vitesse plafonnée à 40 blocs/s, 10 s).

## 1.0.0 - fusée Hyperboost (02/11/2026, LeKiwi06)

- **Recette** (enregistrée par le plugin, livre de recettes débloqué dès qu'on a de la TNT ou une fusée) : TNT au
  centre, 8 fusées de feu d'artifice **de niveau 3** autour (le niveau est vérifié : avec une fusée de niveau 1 ou 2,
  pas de résultat). L'automate de craft ne peut pas la fabriquer (il ne passe pas par cette vérification).
- **Objet** : base étoile de feu d'artifice (rien ne se passe au clic, pas de distributeur, pas d'arbalète), apparence
  de fusée (`item_model`) avec l'aspect enchanté, pile de 16, marqueur `ks_hyperboost:hyperboost`. Ne sert
  d'ingrédient à aucune autre recette. `creerHyperboost()`, `estHyperboost(objet)`.
- **Utilisation** : clic droit en **conduisant** une monture sellée : cheval, âne, mule, chevaux squelette et zombie,
  chameau (et chameau momifié), happy ghast. Refusée à pied, en élytres, en passager, sur un lama, sans selle, ou si
  une poussée est déjà en cours. Une fusée consommée (pas en créatif).
- **Poussée** : 10 s, la monture va à 40 blocs/s au plus (`vitesse-max`, défaut dans le code, pas de `config.yml`).
  Calcul d'après le code du client 26.3 (la monture conduite se déplace chez le client, avec les attributs que le
  serveur lui envoie) :
  - chevaux : `movement_speed` porté à 0,8957 (vitesse limite au sol = 2,2026 × attribut par tick, 2,2327 sur la
    glace bleue, le pire cas : 39,5 blocs/s sur un bloc normal, 40 sur la glace bleue) ;
  - chameau : 0,7957 (le sprint ajoute 0,1) ; **saut et élan coupés** pendant la poussée (`jump_strength` à 0) :
    l'élan vaut 22,2 × saut × vitesse, il aurait dépassé 100 blocs/s ;
  - happy ghast : `flying_speed` porté à 0,1504 : 40 blocs/s à l'horizontale en diagonale (le pire cas), 32,7 blocs/s
    tout droit.
- **Retour exact** : modificateurs d'attributs **temporaires** (jamais enregistrés avec l'entité) ajoutés à la valeur
  finale et retirés à la fin, quand le joueur descend (ou change de monde, ou la monture meurt) ou se déconnecte, et à
  l'arrêt du plugin ; même après un arrêt brutal du serveur, la monture garde ses attributs d'avant.
- **Anti-triche** : métadonnée `ks_hyperboost` (date de fin + 5 s de marge) lue par KS_AntiCheat 1.5.0, qui annule et
  ignore les signalements de GrimAC pendant ce temps ; permissions `grim.nosetback` et `grim.nosetback.<vérification>`
  (GrimAC 2.3.74 coupe le renvoi en arrière de « Simulation » seulement avec la permission de la vérification, relue
  par `GrimUser.updatePermissions()`) pendant la même durée : sans elles, GrimAC renverrait le joueur en arrière et
  éjecterait les passagers.
- Retour au joueur : son de fusée, particules de feu d'artifice derrière la monture, « Hyperboost ! » puis
  « Hyperboost terminé » dans la barre d'action (textes dans `lang.yml`).
- `depend` KLM_Menu (textes) ; `softdepend` GrimAC.

Limites connues :
- **Saut d'un cheval** pendant la poussée : le saut ajoute 0,4 × force de saut bloc/tick vers l'avant, soit jusqu'à
  ~48 blocs/s pendant moins d'une seconde.
- **Happy ghast** : en montant (saut + regard vers le haut), la vitesse totale peut atteindre ~54 blocs/s, surtout à
  la verticale (comme en vanilla, l'entrée verticale s'ajoute).
- Le nautile (monture sous l'eau), le cochon et l'arpenteur ne sont pas pris : à décider.
- Bedrock (Geyser) : la monture est calculée par le client Bedrock, avec sa propre physique : vitesse réelle à
  mesurer ; le clic droit dans le vide avec l'objet en main est à vérifier.

À ajouter aux plugins communs (pas fait ici, plugins partagés) :
- **KS_Economy** (barème, à valider) : fusée de niveau 3 = (papier 0,015625 + 3 poudres 0,0635) / 3 = 0,068708 ;
  Hyperboost (1.1.0) = cristal de l'End 2,4375 + 2 × TNT 0,3575 + 6 × 0,068708 = **3,565** (1.0.0 : 8 fusées + TNT =
  0,9072, jugé trop bas). (Le barème ne connaît que la fusée de niveau 1 : 0,026375.)
- **KS_Didacticiel** : entrée « Fusée Hyperboost » (recette, clic droit en conduisant une monture sellée, 40 blocs/s
  pendant 10 s, chameau sans saut pendant la poussée).
- **Texture** (choix de LeKiwi06 le 02/11 : fusée jaune et noire au lieu de rouge et blanche) : PNG prêt dans
  `visuels-objets/png/fusee_hyperboost.png` (hors dépôt ; fusée du jeu 26.3 recolorée : rouge → jaune, blanc → noir).
  Reste : ligne dans `KS_Textures/.../Regles.java` (`fusee_hyperboost` → `ks_hyperboost:hyperboost`), base étoile de
  feu d'artifice dans `pack.py`, couleur de rareté d'après le prix (`Raretes.java`).
- **geyser-bedrock** : même PNG en apparence Bedrock (sinon les joueurs Bedrock voient une étoile de feu d'artifice).

**Compilé, non déployé (remplacé par la 1.1.0). Statut : non testé en jeu.**

## 1.1.0 - nautiles, arpenteur, pas de mort de chute (02/11/2026, LeKiwi06)

Retours de LeKiwi06 sur la 1.0.0 : « le cheval qui saute n'est pas gênant, tant qu'il ne meurt pas de chute en
retombant » ; happy ghast au-dessus de 40 blocs/s en montant : « ce n'est pas grave, je vais vérifier que ça fasse pas
bugguer » ; « j'aimerais que le nautile et l'arpenteur soient pris en compte ».

- **Chute** : la monture poussée et ses passagers (le conducteur compris, même s'il descend en l'air) ne prennent
  aucun dégât de chute pendant la poussée et 5 s après.
- **Nautile et nautile zombie** (sellés) : dans l'eau, vitesse = 0,0325 × `movement_speed`, entrée ramenée à 1,
  freinage 0,9 → `movement_speed` porté à 6,154 (40 blocs/s). Hors de l'eau : lent (0,02 × l'attribut).
  **Limite** : l'élan du nautile (touche de saut) vaut 1,2 × `movement_speed` dans la direction du regard, calculé par
  le client et lié à aucun autre attribut : pendant la poussée, un élan donne une pointe d'environ 7 blocs/tick
  (~150 blocs/s) qui retombe en une seconde (freinage 0,9 par tick, un élan toutes les 2 s). Pas pu être coupé.
- **Arpenteur** (sellé, champignon biscornu sur un bâton dans l'autre main) : vitesse = `movement_speed` × 0,55 (0,35
  hors de la lave) × accélération du champignon ; calcul au sol comme les chevaux → 1,6287 (40 blocs/s sur la lave).
  Refusé pendant une accélération du champignon ; le champignon est sans effet pendant la poussée (il la multiplierait
  jusqu'à × 2,15).

- **Recette plus chère** (LeKiwi06 : 0,91 « trop peu cher pour un tel pouvoir », prix visé autour de 5, pas de minerai
  sans raison, thème fusée ou explosifs ; choix : « 6 fusées, 2 TNT et un cristal ») : **cristal de l'End** au centre,
  TNT au-dessus et au-dessous, 6 fusées de niveau 3 sur les côtés. Prix **validé par LeKiwi06** le 02/11 (« ça fait très super fusée ») pour KS_Economy : cristal 2,4375 +
  2 × TNT 0,3575 + 6 × 0,068708 = **3,565**.

**Compilé, non déployé. Statut : non testé en jeu.**
