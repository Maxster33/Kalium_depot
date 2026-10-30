# KS_Atouts - journal

Serveur Kixster. Atouts d'équipement (étape 4 du chantier « atouts, alcools, aliments », demande de LeKiwi06 du
28/10/2026 ; cahier local `Atouts_Alcools_Aliments.md`, partie 3.5, à copier ici au déploiement).
Dépend de KLM_Menu (textes) et de KS_Effets (/effects).

## 1.1.0 - casque ventouse (29/10/2026)

Demande de LeKiwi06 du 29/10 : « entourer un casque de block de miel fait nous coller au plafond si notre tête le
touche, pour retomber, il suffit de sneak ».

| Atout | Craft (à l'établi) | Apparence pendant l'atout | Effet |
|---|---|---|---|
| Casque ventouse | 8 blocs de miel autour d'un casque | bloc de miel (posé sur la tête) | collé au plafond quand la tête le touche, sneak pour retomber |

- **Collage** (vérifié chaque tick) : en l'air, la tête touche le dessous d'un bloc solide (dalle, escalier, verre,
  feuilles…). Pas dans l'eau ou la lave, pas en sneakant (on saute dans un tunnel de 2 blocs sans rester accroché),
  ni en véhicule, en élytre, en vol ou en spectateur. Pas non plus si le joueur lévite déjà (shulker).
- **Collé** : une lévitation I sans particules ni icône (15 s renouvelées chaque seconde) le plaque au plafond ; il
  glisse au plafond comme on avance en l'air. Le mouvement est calculé par le client : même rendu Java et Bedrock.
- **Décollage** : sneak, sortie de sous le plafond (plus de bloc à 0,15 bloc au-dessus de la tête), eau ou lave,
  casque retiré, lait ou mort, déconnexion, arrêt du serveur. Dégâts de chute normaux.
- Choix de LeKiwi06 (29/10) : glisse au plafond plutôt que figé (figer saccade, surtout sur Bedrock) ; apparence bloc
  de miel (« ça fonctionne sur l'emplacement de la tête » ; vérifié dans le client 26.3 : un objet sans apparence
  d'armure est dessiné sur la tête) ; sneak = pas collé ; hors de l'eau.
- **Protections ajoutées pour tous les atouts** : un objet sous atout ne se pose pas comme un bloc (la tête de gardien
  et le bloc de miel posés perdaient l'équipement), et ne sert dans aucun autre craft, à l'établi comme au fabricant
  (un casque ventouse donnait des fioles de miel ; teindre des bottes de ski n'est donc plus possible).

Limites :
- Bedrock : le bloc de miel porté sur la tête reste à vérifier en jeu (Bedrock dessine peut-être seulement les têtes
  et la citrouille).

Compilé le 29/10/2026 (`sortie/KS_Atouts-1.1.0.jar`). **Déployé sur Kixster le 29/10/2026 à 15:46** (LeKiwi06 : « Oui, déploie KS_Atouts 1.1.0 sur Kixster avec l'entrée du didacticiel »), avec `didacticiel.yml` (entrée « Casque ventouse » dans « Atouts d'équipement », règles complétées : pas de pose, pas d'autre craft) ; fichiers en place vérifiés identiques au dépôt avant l'envoi, et après. 1.0.0 dans `_removed-ks_atouts-1.0.0/`, ancien didacticiel dans `_removed-ks_didacticiel-yml-2026-10-29-ventouse/`. À redémarrer par LeKiwi06 (la recette `ks_atouts:ventouse` du didacticiel n'existe qu'après le redémarrage). Statut : non testé en jeu.

## 1.0.0 - bottes de ski, casque poiscaille, bottes coupe-feu (28/10/2026)

| Atout | Craft (à l'établi) | Apparence pendant l'atout | Effet |
|---|---|---|---|
| Bottes de ski | 8 pieds de lapin autour de bottes | bottes en cuir | marcher sur la poudreuse (le jeu le permet aux bottes en cuir) |
| Casque poiscaille | 8 poissons-globes autour d'un casque | tête de gardien | vision nocturne tant que la tête est sous l'eau |
| Bottes coupe-feu | 8 crèmes de magma autour de bottes | bottes en cuivre | aucun dégât des blocs de magma |

- Toutes les bottes et tous les casques du jeu (carapace de tortue comprise). Chaque craft ajoute **1 heure**,
  décomptée **seulement quand l'objet est porté**, sans plafond. Un autre atout remplace le précédent : son temps est
  perdu.
- **Apparence** : l'objet change vraiment de type et garde ses caractéristiques : modificateurs d'attributs (armure,
  robustesse, résistance au recul), durabilité et usure, réparation avec son matériau d'origine, résistance au feu
  (netherite), enchantabilité, pile de 1, nom de base (« Bottes en diamant »), réglages d'équipement (son, usure quand
  on est touché). Les enchantements, le nom donné et la garniture suivent d'eux-mêmes. Fin de l'atout : il reprend son
  type ; les valeurs recopiées qui venaient de son type sont retirées, l'usure reste.
- **Temps restant** : ligne « Atout : ... (59 min) » dans la description (mise à jour chaque minute portée et à la
  déconnexion) et dans `/effects` à la seconde près (KS_Effets, tant que l'objet est porté). Fin : message en barre
  d'action (`atout.fin` dans `lang.yml`).
- Vision nocturne du casque : 15 s renouvelées chaque seconde sous l'eau (pas de clignotement), retirée en sortant de
  l'eau.
- Dégâts du magma : reconnus par leur type du jeu (`hot_floor`), la « cause » de Bukkit étant dépréciée en 26.2.
- Le livre de recettes montre un aperçu ; le vrai résultat est calculé sur l'équipement posé au centre.

Limites :
- Pendant l'atout, l'objet ne se combine pas à l'enclume avec un objet de son type d'origine (types différents), et une
  tête de gardien n'accepte pas les livres d'enchantement de casque.
- La garniture d'un casque ne se voit pas sur la tête ; les bottes en cuir ont leur couleur par défaut.
- Bedrock : apparence par le vrai type (aucun réglage du pack) ; la tête de gardien portée reste à vérifier.
- Didacticiel et `/kaliumgive` : à faire au déploiement.

Compilé le 28/10/2026 (`sortie/KS_Atouts-1.0.0.jar`, après KLM_Menu et KS_Effets). **Déployé sur Kixster le 29/10/2026 à 11:17** (LeKiwi06 : « Oui, déploie KS_Cuisine 1.0.0, KS_Atouts 1.0.0, KS_Decapitator 1.4.0 et KS_Economy 1.12.2 sur Kixster, et geyser-bedrock 1.7.0 sur le proxy, avec les entrées du didacticiel »), avec `didacticiel.yml` ; fichiers en place vérifiés identiques au dépôt avant l'envoi. À redémarrer par LeKiwi06. Statut : non testé en jeu. Didacticiel : thème « Atouts d'équipement » (4 nouveautés).

## 1.2.0 - plastron anti-radiation (30/10/2026)

Chantier des maléfices du Nether (cahier local `Malefices_Nether.md`, partie 3.3, validé par LeKiwi06 le 30/10/2026).
LeKiwi06 : « un plastron anti-radiation, avec des blocs d'or autour du plastron : chaque craft protège pour 1 jour
passé dans le nether » ; « il prend l'apparence d'un plastron en or (le side effect est qu'ils ne sont pas aggro par
les piglins, je le sais, c'est volontaire) » ; « le plastron divise le temps d'action des effets par 2 ».

| Atout | Craft (à l'établi) | Apparence pendant l'atout | Effet |
|---|---|---|---|
| Plastron anti-radiation | 8 blocs d'or autour d'un plastron | plastron en or | durée des maléfices du Nether divisée par 2 (KS_MaleficesNether) |

- Tous les plastrons du jeu. **20 minutes par craft** (un jour du Nether), décomptées **seulement quand il est porté
  dans le Nether** ; cumulable, mêmes règles que les autres atouts (autre atout = temps perdu, ni posé ni dans un autre
  craft). Ligne de l'objet : « Atout : Plastron anti-radiation (20 min dans le Nether) ».
- Les atouts gèrent maintenant l'emplacement du plastron (temps porté, fin de l'atout, recette).
- API : `KSAtouts.porteAtout(joueur, id)` (KS_MaleficesNether : `anti_radiation`).
- Les piglins ne l'attaquent pas (vrai plastron en or) : voulu.

Empilé sur la 1.1.0, jamais testée en jeu (chantier validé par LeKiwi06). Compilé le 30/10/2026
(`sortie/KS_Atouts-1.2.0.jar`). **À déployer avec KS_MaleficesNether 1.0.0.** Statut : non déployé, non testé en jeu.
