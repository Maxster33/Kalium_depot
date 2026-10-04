# KS_LootCoffres - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Demande de Maxster33 du 28/09/2026 (voir
`KS_Event/CAHIER_DES_CHARGES.md`, partie 4).

## 1.0.0 - loots des coffres de structures (28/09/2026)

**Principe** : le loot vanilla (tables de la 26.2, lues dans le jeu) est généré normalement puis modifié juste avant
d'être mis dans le coffre (`LootGenerateEvent`) ou éjecté par un coffre-fort (`BlockDispenseLootEvent`). Tout ce qui
n'est pas cité reste vanilla. **Seuls les coffres pas encore ouverts** sont concernés (le loot d'un coffre de
structure est tiré à la première ouverture).

Les changements de poids sont faits par remplacement, avec des chances calculées pour donner exactement les nouveaux
poids (calcul en commentaire dans le code) :
- **Cité antique** (tirage principal, 5 à 10 tirages, poids total 84) : jambières en diamant enchantées niveau 10 à 32
  (au lieu de 30 à 50) ; pomme dorée enchantée : poids divisé par 2, totem d'immortalité ajouté au même poids (tous
  les autres poids x2 : pomme 1/168, totem 1/168, les autres gardent leur chance) : une pomme sur deux devient un
  totem. Par coffre : pomme environ 8,6 % → 4,4 %, totem 4,4 %.
- **Bastions** (pont, étable à hoglins, autres, salle au trésor) : plus de modèle de forge (amélioration en netherite).
- **Bastion, salle au trésor** : pièces en diamant **enchantées** : plastron niveau 30 à 50 ; épée, lance, casque,
  jambières, bottes niveau 1 à 29 (au lieu d'un enchantement au hasard). Les versions sans enchantement de la table
  vanilla ne changent pas.
- **Trésor enfoui** : totem d'immortalité, tirage à part, même chance par coffre que dans la cité antique
  (1 - moyenne de (167/168)^n pour n de 5 à 10, environ 4,4 %, affiché au démarrage).
- **Cité de l'End** : bottes en diamant niveau 30 à 50 ; épée, lance, plastron, casque, jambières en diamant niveau
  1 à 29 (au lieu de 20 à 39).
- **Forteresse du Nether** (tirage principal, poids total 78) : crâne de wither squelette x1 (poids 2) et débris
  antique x1 (poids 2) ajoutés (chaque objet du tirage remplacé avec une chance de 4/82) ; lingot d'or 3 à 5, lingot de
  fer 5 à 11, diamant 2 à 4.
- **Avant-poste** (tirage expérience / ficelle / flèches / crochet / fer / livre, poids total 22) : fiole sinistre x1
  (poids 3) ajoutée (remplacement avec une chance de 3/25).
- **Manoir** : livres enchantés niveau 30 à 50 (au lieu d'un enchantement au hasard).
- **Coffre-fort, tirage commun** : fiole sinistre poids 2 → 1 (gardée avec une chance de 25/48, sinon remplacée par
  un autre objet du tirage commun aux poids vanilla).
- **Coffre-fort sinistre, tirage commun** : tous les poids x2 sauf la fiole sinistre (gardée avec une chance de 15/29,
  sinon remplacée par un autre objet du tirage commun sinistre).
- **Coffre-fort sinistre, tirage rare** : livre Rafale de vent au niveau 1, 2 ou 3 (au lieu de 1).
- Enchantements « niveau X à Y » : comme la fonction vanilla `enchant_with_levels` (enchantements `#on_random_loot`) ;
  l'usure vanilla des objets est gardée.

**Déployé sur Event le 28/09/2026 à 23:19. Statut : non testé en jeu.**

## 1.1.0 - jambières de la cité antique : niveau 30 à 50 (04/10/2026, LeKiwi06)

Demande de LeKiwi06 : « je veux que les joueurs puissent avoir l'item avec mending en 10 à 20 tirages de loot
table ». Les trois autres pièces en diamant enchantées « niveau 30 à 50 » (plastron de la salle au trésor des bastions,
bottes des cités de l'End, casque de l'Estomac du gardien) sortent avec Raccommodage environ 4 fois sur 10, soit une
pièce avec Raccommodage tous les 12 à 19 coffres ou estomacs. Les jambières de la cité antique, au niveau 10 à 32
depuis la 1.0.0 (demande de Maxster33), n'en avaient qu'environ 8 fois sur 100, soit une tous les 75 coffres.
- **Cité antique** : jambières en diamant enchantées **niveau 30 à 50** (valeur vanilla ; 1.0.0 : 10 à 32). Le reste
  (pomme dorée enchantée, totem) est inchangé.
- Chances estimées par Claude (simulation du tirage d'enchantement vanilla et tables de butin de mémoire) : ordres de
  grandeur, pas mesurés en jeu.
- Seuls les coffres pas encore ouverts sont concernés.

La 1.0.0 n'a jamais été testée en jeu : fonctionnalité ajoutée à la demande explicite de LeKiwi06.

**Statut : compilé le 04/10/2026, non déployé, non testé en jeu.**
