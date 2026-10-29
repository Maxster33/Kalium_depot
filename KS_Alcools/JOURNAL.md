# KS_Alcools - journal

Serveur Kixster. Alcools à l'alambic, alcool neutre comme combustible, cocktail Molotov, bandage, torche augmentée
(étape 2 du chantier « atouts, alcools, aliments », demande de LeKiwi06 du 28/10/2026 ; cahier local
`Atouts_Alcools_Aliments.md`, parties 3.2 et 3.3, à copier ici au déploiement).
Dépend de KLM_Menu (textes) et de KS_Effets (effets custom) ; SimpleClaimSystem facultatif (claims).

## 1.1.0 - rhum (29/10/2026)

Étape 5 du chantier (cahier, partie 3.2.2, solution A). Choix de LeKiwi06 du 29/10 : **les 22 disques** du jeu plutôt
que les musiques de fond (sans pack Java, Java ne permet de jouer un morceau précis de musique de fond que pour 3
d'entre elles ; chaque disque, lui, a son propre son sur Java et sur Bedrock, donc tout le monde entend le même).

- **Rhum** : alcool neutre + canne à sucre ; 3 min, redstone 8 min, poudre à canon (jetable) ; pas de glowstone.
  Effet custom « Rhum » (KS_Effets). Nausée comme les autres alcools.
- **Musique** : un disque au hasard (jamais deux fois de suite le même). Au début d'un morceau, seuls les joueurs à
  **5 blocs** ou moins du buveur l'entendent, tous au même moment. Un joueur qui s'éloigne au-delà de 5 blocs (ou
  change de monde) ne l'entend plus ; celui qui arrive pendant un morceau attend le suivant. À la fin d'un morceau
  (durées des données du jeu, + 1 s), le suivant commence pour les joueurs proches.
- **Java** : le son du disque est attaché au buveur et le suit (catégorie « Jukebox / blocs musicaux »).
- **Bedrock** : son `kalium.rhum.<disque>` du pack geyser-bedrock 1.9.0, **sans position** (même volume où qu'on
  soit), envoyé à ce seul joueur ; joueur Bedrock reconnu par Floodgate (sans dépendance de compilation).
- **Fin** : effet terminé (temps, lait, mort), déconnexion du buveur, arrêt du serveur : la musique s'arrête pour tous
  ses auditeurs, même au milieu d'un morceau.

Limites :
- Deux buveurs proches : un joueur près des deux entend les deux musiques en même temps.
- Bedrock : à vérifier en jeu que le pack peut utiliser les fichiers de disques de Bedrock lui-même (rien n'est copié)
  et que le son est bien sans position. **À déployer avec geyser-bedrock 1.9.0** sur le proxy.
- Didacticiel (entrée « Les alcools » à compléter) : au déploiement.

Compilé le 29/10/2026 (`sortie/KS_Alcools-1.1.0.jar`). **Déployé le 29/10/2026 à 23:35** (LeKiwi06 : « Oui, déploie ») sur Kixster, avec `didacticiel.yml` (entrée « Les alcools » complétée du rhum ; ancien dans `_removed-ks_didacticiel-yml-2026-10-29-rhum/`), et geyser-bedrock 1.9.0 sur le proxy à la même heure ; fichiers en place vérifiés identiques au dépôt avant l'envoi. 1.0.0 dans `_removed-ks_alcools-1.0.0/`. À redémarrer par LeKiwi06 (Kixster et proxy). Statut : non testé en jeu.

## 1.0.0 - alcools, combustible, Molotov, bandage, torche augmentée (28/10/2026)

**Alcools** (potions sans type de base, marquées ; aucune recette d'alambic du jeu ne les reconnaît) :

| Alcool | À l'alambic | Effet (3 min ; redstone 8 min ; glowstone niveau II, 1 min 30) |
|---|---|---|
| Alcool neutre | fiole d'eau + patate empoisonnée | aucun (base des autres, combustible) |
| Tequila | alcool neutre + fleur de cactus | Résistance |
| Vodka | alcool neutre + patate | Lévitation, en pause quand le joueur est accroupi (effet custom) |
| Hydromel | alcool neutre + bouteille de miel | Célérité |
| Manzana | alcool neutre + pomme | Bonus de vie |
| Bière | alcool neutre + blé | Grâce du dauphin (pas de glowstone) |
| Tabascoolisé | alcool neutre + crème de magma | particules de feu, ni gel ni dégâts de la poudreuse (effet custom ; pas de glowstone) |

- Mélanges ajoutés par Paper (43) : redstone et glowstone sur l'alcool à boire et sur le jetable, poudre à canon sur
  chaque version. **Souffle de dragon** : aucun mélange (rien ne se passe), comme voulu. L'alcool neutre n'a ni durée,
  ni niveau II, ni version jetable (le Molotov tient ce rôle).
- **Nausée** : 10 % de chance, 10 secondes, à chaque alcool bu ou reçu (jetable), alcool neutre compris.
- **Vodka** : effet custom de KS_Effets (« Vodka », « Vodka II ») ; tant qu'il dure, Lévitation (II) avec la durée
  restante, retirée quand le joueur s'accroupit et remise quand il se relève ; le minuteur continue pendant la pause.
- **Tabascoolisé** : effet custom « Tabascoolisé » : flammes autour du joueur, gel effacé, dégâts de gel annulés.
- Jetables : la durée des effets custom baisse avec la distance, comme le jeu le fait pour ses effets.
- Infobulle : les effets du jeu s'affichent d'eux-mêmes ; pour l'alcool neutre, la vodka et le tabascoolisé, une
  description remplace « Aucun effet ».
- Les alcools ne sont ni des ingrédients d'élixir (KS_Elixir reconnaît les potions du jeu) ni des potions du butin.

**Alcool neutre comme combustible** (four, fumoir, haut fourneau) :
- En 26.3, un combustible est un **composant de l'objet** (`cooking_fuel`) : l'alcool neutre le porte, donc la case du
  combustible, le Maj+clic, le glisser et les entonnoirs l'acceptent comme du charbon, sans code. Valeurs : 10 000
  ticks au four, 5 000 au fumoir et au haut fourneau = **50 cuissons** partout (comme le charbon : 1 600, divisé par 2
  dans les fours rapides). Paper n'a pas de méthode pour ce composant : il est posé par le texte de l'objet ; si le
  jeu le refusait, un repli à durée fixe est essayé, puis un avertissement est écrit au démarrage.
- La **fiole vide** reste dans la case du combustible (comme le seau vide de la lave). Un **entonnoir sous le four**
  l'aspire : le jeu ne laisse sortir par le bas que les seaux, le plugin la déplace donc lui-même (tous les 8 ticks
  tant que l'entonnoir est plein ou bloqué par la redstone ; oublié au redémarrage : la fiole reste alors dans la case
  jusqu'à ce qu'un joueur la retire).
- Trains : KS_Trains 1.6.0.

**Cocktail Molotov** (alcool neutre + papier, sans forme) : potion jetable orange, sans effet. À l'impact : feu sur
3 × 3 colonnes (à la hauteur de l'impact, sinon un bloc plus bas ou plus haut, là où un feu tient ; feu des âmes sur
le sable et la terre des âmes), creepers à 1,5 bloc allumés. Rien dans le claim d'un autre joueur (message en barre
d'action ; lancé par un distributeur : rien dans aucun claim). **Ajout technique** : chaque feu passe par l'événement
d'allumage du jeu, comme un briquet du lanceur, pour que les autres protections (WorldGuard au spawn...) puissent le
refuser.

**Bandage** (8 papiers autour d'un alcool neutre) : se mange en 0,8 s, même rassasié, ne nourrit pas, rend 3 points de
vie (1,5 cœur), pile de 64. Base : **kelp séché** (même vitesse sur Java et Bedrock), image du papier ; apparence
Bedrock : `geyser-bedrock/` 1.6.0.

**Torche augmentée** (torche + papier + alcool neutre, sans forme → 4) : torche brillante. Posée, elle place des
blocs de lumière de niveau 15 sur une grille de 3 dans un rayon de 15 blocs (environ 520 blocs), seulement dans l'air
et les sources d'eau, jamais dans le claim d'un autre joueur que le poseur. Torches notées dans leur chunk (position,
poseur). Disparue (cassée, eau, support cassé, explosion, piston, commande) : vérifié au tick suivant d'une torche
tombée sur place et toutes les 5 secondes ; sa grille est recalculée et ses blocs de lumière de niveau 15 retirés,
puis les torches augmentées voisines (30 blocs) sont rallumées. Elle redonne une torche augmentée (la torche du jeu
qui tombe est remplacée).

**Autres** :
- Les objets du plugin ne servent pas dans les recettes du jeu (bandage → bloc de kelp, torche → lanterne...).
- Livre de recettes : une recette est débloquée quand le joueur obtient un de ses ingrédients.
- Pour les autres plugins : `alcool(id, variante, jetable)`, `ids()`, `idAlcool(objet)`, `estAlcoolNeutre(objet)`,
  `creerMolotov()`, `creerBandage()`, `creerTorche(nombre)`.
- Textes des messages dans `lang.yml` ; noms et descriptions des objets dans le code (un objet déjà fabriqué doit
  rester identique pour les recettes).

Limites et points à tester en premier :
- **Mélanges d'alambic** : en 26.x, l'alambic du jeu fonctionne avec des recettes ; les mélanges ajoutés par Paper
  (`PotionMix`) sont censés s'y ajouter : à vérifier (patate empoisonnée acceptée dans la case de l'ingrédient, eau →
  alcool neutre).
- **Bedrock** : l'écran du four est géré par le client Bedrock, qui pourrait refuser une potion dans la case du
  combustible (l'entonnoir marche dans tous les cas) ; les alcools s'affichent comme des potions avec leur nom.
- Vodka : la Lévitation du jeu apparaît aussi dans `/effects`, avec le même temps restant que l'effet « Vodka »
  (pendant la pause, seule « Vodka » reste).
- Torche augmentée : un bloc de lumière de niveau 15 posé à la main sur un point de sa grille est retiré avec elle ;
  un bloc posé dans un bloc de lumière le remplace (trou dans la grille jusqu'à la prochaine pose voisine).
- Rhum et Shéjiǔ : étapes 5 et 6 du cahier. `/kaliumgive` (KS_KaliumGive) et entrées du didacticiel : à faire.

Compilé le 28/10/2026 (`sortie/KS_Alcools-1.0.0.jar`, après KLM_Menu et KS_Effets). **Déployé sur Kixster le 28/10/2026 à 18:31** (LeKiwi06 : « Oui, déploie KS_Effets 1.0.0, KS_Alcools 1.0.0 et KS_Trains 1.6.0 sur Kixster, et geyser-bedrock 1.6.0 sur le proxy, avec les entrées du didacticiel »), avec `didacticiel.yml` ; fichiers en place vérifiés identiques au dépôt avant l'envoi. À redémarrer par LeKiwi06. Statut : non testé en jeu. Didacticiel : thème « Alcools et alambic » (5 nouveautés) ; apparence Bedrock du bandage (geyser-bedrock 1.6.0) envoyée sur le proxy à la même heure.
