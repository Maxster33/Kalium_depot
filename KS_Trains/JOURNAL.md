

**Actif sur Event depuis son redémarrage du 17/10/2026 à 03:57 (journal lu, heures en UTC : 01:57) : KLM_Contacts 1.3.0 puis KS_Trains 1.4.0 activés sans erreur ni avertissement (KLM_Contacts avant KS_Trains, comme voulu pour le partage entre amis), démarrage en 12 s. Statut : non testé en jeu.**

## 1.4.1 - percussion : projection vers le haut (17/10/2026, LeKiwi06)

Demande de LeKiwi06 (tests sur Event) : « le train ne repousse pas correctement, j'aimerais qu'il envoie très haut comme
le dragon ».

- **Cause** : le dragon pousse une entité **à chaque tick** tant qu'elle est sous ses ailes (lu dans le code du jeu :
  0,2 vers le haut à chaque fois, qui s'additionnent) ; le train ne la pousse qu'une fois (une fois par demi-seconde au
  plus) : il ne restait que 0,2 vers le haut, presque rien.
- **Correction** : montée de **3 blocs/tick** à pleine vitesse, soit **40 blocs de haut** (LeKiwi06 : « mets plutôt 40
  blocs de haut à pleine vitesse » ; calculé avec la physique du jeu : gravité 0,08 et freinage de l'air 2 % par tick ;
  `percussion.hauteur`), **0,4** au pas, en proportion entre les deux ; la vitesse verticale de l'entité n'est plus
  ajoutée si elle tombait. Poussée horizontale du dragon gardée, plafonnée à **3 blocs/tick** (`percussion.horizontal`)
  pour une entité collée au wagon (avant : jusqu'à 13 blocs/tick).
- `config.yml` : nouvelles clés `percussion.hauteur` et `percussion.horizontal` ; valeurs par défaut aussi dans le code.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.1.jar`). **Statut : non déployé, non testé en jeu.**

