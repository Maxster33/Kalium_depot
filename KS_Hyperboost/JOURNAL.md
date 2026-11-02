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
  Hyperboost = 8 × 0,068708 + TNT 0,3575 = **0,9072**. (Le barème ne connaît que la fusée de niveau 1 : 0,026375.)
- **KS_Didacticiel** : entrée « Fusée Hyperboost » (recette, clic droit en conduisant une monture sellée, 40 blocs/s
  pendant 10 s, chameau sans saut pendant la poussée).
- **KS_Textures** : rien d'obligatoire (l'objet porte déjà `item_model` = fusée) ; couleur de rareté d'après le prix.
- **geyser-bedrock** : apparence Bedrock de l'objet (sinon les joueurs Bedrock voient une étoile de feu d'artifice).

**Compilé, non déployé. Statut : non testé en jeu.**
