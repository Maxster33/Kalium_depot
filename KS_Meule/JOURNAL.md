# KS_Meule - journal

Plugin des serveurs de survie (Kixster, Event) : la meule. Dépend de KLM_Menu (menu en coffre, textes).

## 1.0.0 - retirer les malédictions à la meule (11/10/2026, LeKiwi06)

Question de LeKiwi06 : « est-ce qu'on a fait le plugin pour retirer les malédictions des items ? » (non : aucune trace
dans le dépôt ni dans les cahiers des charges). Sa demande : « il faut faire sneak+click sur une meule avec l'item
concerné , cela coute 45 niveaux par malédiction ( interface de confirmation avant ) ». Puis, après la première
écriture (objet tenu en main), le même jour et avant tout déploiement : « fait en sorte que le menu soit uniquement a
main nu et qu'on selectionne la pièce d'armure dans l'inventaire ( pas le stuff porté ) ». Ce qui suit décrit la
version revue ; la première écriture n'a jamais été déployée.

- **Geste** : accroupi, clic droit sur une meule, **la main vide**. Le menu s'ouvre ; la meule elle-même ne s'ouvre
  pas. Avec un objet en main, rien ne change : le clic fait ce qu'il fait d'habitude.
- **Menu « Retirer une malédiction »** (coffre de 2 rangées) :
  - d'abord une invitation : « Clique sur un objet maudit de ton inventaire », le prix par malédiction, « Fermer » ;
  - un clic sur un objet de son inventaire le choisit s'il porte une malédiction (sinon : « Cet objet n'a pas de
    malédiction. ») ; un autre clic en choisit un autre ;
  - l'objet choisi s'affiche au centre ; en bas, un bouton « Retirer (45 niveaux) » par malédiction (livre enchanté,
    le nom de la malédiction dans sa description), « Tout retirer (90 niveaux) » s'il y en a plusieurs, « Annuler ».
    Boutons verts si le joueur a les niveaux, rouges sinon.
  - L'objet reste à sa place dans l'inventaire : le menu n'en montre que l'image, rien ne s'y prend ni ne s'y dépose.
- **Pas l'équipement porté** : le menu ne montre que l'inventaire (barre d'objets comprise) ; l'armure portée et la
  main secondaire n'y sont pas, donc ne peuvent pas être choisies.
- **Coût** : 45 niveaux d'expérience par malédiction retirée (`niveaux-par-malediction` du `config.yml`, valeur par
  défaut dans le code), retirés comme à l'enclume (la progression dans le niveau est gardée). Gratuit en créatif.
- **Effet** : seule la malédiction choisie part. Les autres enchantements, le nom, l'usure et le reste de l'objet ne
  changent pas. S'il reste une malédiction sur l'objet, ses boutons sont remis ; sinon le menu attend un autre objet.
- Vérifié au clic : l'objet est toujours à sa place (sinon « Cet objet n'est plus à sa place. »).
- Toutes les malédictions du jeu sont reconnues (celles que le jeu marque comme telles : Liaison, Disparition).
- Une ligne dans la console à chaque retrait (qui, combien, quel objet, quel coût).
- Textes : `lang.yml` du plugin (créé au premier démarrage).

Choix de Claude, signalés :
- tout objet maudit de l'inventaire se choisit, pas seulement une pièce d'armure (outil, arme, élytres...) ; un
  **livre enchanté** qui porte une malédiction compte aussi et, s'il ne lui reste aucun enchantement, redevient un
  livre ;
- le clic est traité même dans une zone protégée (spawn, claim d'un autre) : rien n'y est modifié, et une meule
  publique doit pouvoir servir ;
- le prix est réglable dans `config.yml`.

Limites :
- Une armure de Liaison **déjà portée** ne se retire pas, donc ne revient jamais dans l'inventaire : elle ne peut pas
  être traitée (voulu : « pas le stuff porté »). Elle se perd à la mort, comme en vanilla.
- Bedrock : le geste est « accroupi + toucher la meule », main vide ; non testé.
- Écrit sans serveur de test.

**Compilé le 11/10/2026 (`sortie/KS_Meule-1.0.0.jar`). Non déployé. Statut : non testé en jeu.**
