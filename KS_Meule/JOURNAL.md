# KS_Meule - journal

Plugin des serveurs de survie (Kixster, Event) : la meule. Dépend de KLM_Menu (menu en coffre, textes).

## 1.0.0 - retirer les malédictions à la meule (11/10/2026, LeKiwi06)

Question de LeKiwi06 : « est-ce qu'on a fait le plugin pour retirer les malédictions des items ? » (non : aucune trace
dans le dépôt ni dans les cahiers des charges). Sa demande : « il faut faire sneak+click sur une meule avec l'item
concerné , cela coute 45 niveaux par malédiction ( interface de confirmation avant ) ».

- **Geste** : accroupi, clic droit sur une meule, l'objet maudit **en main**. Un menu de confirmation s'ouvre ; la
  meule elle-même ne s'ouvre pas et l'objet en main ne sert pas (une citrouille maudite n'est pas posée).
  Si l'objet en main n'a aucune malédiction, rien ne change : le clic fait ce qu'il fait d'habitude.
- **Menu « Retirer une malédiction »** (coffre de 2 rangées) : l'objet au centre ; en bas, un bouton
  « Retirer (45 niveaux) » par malédiction (livre enchanté, le nom de la malédiction dans sa description), un bouton
  « Tout retirer (90 niveaux) » s'il y en a plusieurs, et « Annuler ». Boutons verts si le joueur a les niveaux,
  rouges sinon. L'objet reste dans la main du joueur : le menu n'en montre que l'image.
- **Coût** : 45 niveaux d'expérience par malédiction retirée (`niveaux-par-malediction` du `config.yml`, valeur par
  défaut dans le code), retirés comme à l'enclume (la progression dans le niveau est gardée). Gratuit en créatif.
- **Effet** : seule la malédiction choisie part. Les autres enchantements, le nom, l'usure et le reste de l'objet ne
  changent pas. S'il reste une malédiction, le menu se remplit à nouveau ; sinon il se ferme.
- Vérifié au clic : l'objet en main est toujours celui du menu (sinon « L'objet en main a changé. »).
- Toutes les malédictions du jeu sont reconnues (celles que le jeu marque comme telles : Liaison, Disparition).
- Une ligne dans la console à chaque retrait (qui, combien, quel objet, quel coût).
- Textes : `lang.yml` du plugin (créé au premier démarrage).

Choix de Claude, signalés :
- un **livre enchanté** qui porte une malédiction compte aussi ; s'il ne lui reste aucun enchantement, il redevient un
  livre ;
- le clic est traité même dans une zone protégée (spawn, claim d'un autre) : rien n'y est modifié, et une meule
  publique doit pouvoir servir ;
- le prix est réglable dans `config.yml`.

Limites :
- **Une armure maudite de Liaison déjà portée ne peut pas être prise en main**, donc pas traitée ici : c'est pourtant
  le cas le plus gênant. Non fait, à décider par LeKiwi06 (par exemple : main vide, le menu propose les pièces portées).
- Bedrock : le geste est « accroupi + toucher la meule » ; non testé.
- Écrit sans serveur de test.

**Compilé le 11/10/2026 (`sortie/KS_Meule-1.0.0.jar`). Non déployé. Statut : non testé en jeu.**
