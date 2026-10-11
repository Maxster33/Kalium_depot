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

**Déployé sur Kixster le 11/10/2026 à 07:45 (LeKiwi06, envoi groupé : KS_Teleport 1.1.0, KS_Decapitator 1.2.0, KS_RewardsGUI 1.5.0, KS_Economy 1.6.0, KS_Crafts 1.12.0, KS_Jetons 2.0.2, KS_Meule 1.0.0 ; accord de LeKiwi06 : « oui déploie tout sur kixster » ; nouveau sur Kixster ; jars en place vérifiés identiques aux références avant l'envoi), actif après redémarrage de Kixster. pas envoyé sur Event. Statut : non testé en jeu.**

## 1.1.0 - Cristal de purification à la place des niveaux (11/10/2026, LeKiwi06)

Demande de LeKiwi06 : « on va plutot faire un item pour enlever les malédictions au lieu d'une quantité d'xp , cet item
sera droppable dans les reward, il aura une valeur théorique de 600 emeraudes , met let dans les pool adaptés ( je ne
sais pas quelle apparence lui donner , mais il faut également le déclarer au proxy pour que les joueurs bedrock le
voient ) ».

- **Cristal de purification** (nouvel objet) : nom violet clair, « Retire une malédiction d'un objet », « Accroupi,
  main vide : clic sur une meule ». Livre de connaissances à l'apparence d'un **éclat d'améthyste** brillant
  (`item_model` `minecraft:amethyst_shard`), empilable par 64, marqué `ks_meule:cristal`. Son clic droit ne fait rien
  (il n'est pas consommé). Autres plugins : `KSMeule.creerCristal()`, `estCristal(objet)`.
- **Coût** : chaque malédiction retirée prend **1 cristal** dans l'inventaire du joueur (plus aucun niveau
  d'expérience). Boutons « Retirer (1 cristal) » et « Tout retirer (2 cristaux) », verts si le joueur a les cristaux,
  rouges sinon ; le menu d'accueil et chaque bouton disent combien il en a. Gratuit en créatif.
- Le geste et le menu ne changent pas : accroupi, clic droit sur une meule, main vide ; objet maudit choisi d'un clic
  dans l'inventaire.
- `config.yml` supprimé (il ne contenait que `niveaux-par-malediction`) ; un fichier déjà créé sur un serveur ne sert
  plus à rien.
- Textes changés sous de nouvelles clés de `lang.yml` (`retirer-cristal`, `tout-cristaux`, `cout-cristal`,
  `tes-cristaux`, `manque-cristal`...) ; les anciennes (`retirer`, `tout`, `tes-niveaux`, `manque`) ne servent plus.
- **Où il s'obtient** : coffres de récompense seulement (pool Légendaire, valeur 600 émeraudes : voir ci-dessous) et
  `/kaliumgive <joueur> cristal_purification` (KS_KaliumGive 1.10.0). Aucune recette.
- **Bedrock** : entrée `kalium:cristal_purification` dans `geyser-bedrock` 1.4.0 (image de l'éclat d'améthyste, pile
  de 64), à envoyer sur le proxy.

Choix de Claude, signalés : nom « Cristal de purification » et apparence d'éclat d'améthyste (LeKiwi06 : « je ne sais
pas quelle apparence lui donner ») ; 1 cristal par malédiction ; pas de recette.

À déployer ensemble : KS_Meule 1.1.0 et KS_KaliumGive 1.10.0 (sans lui, un coffre de récompense qui contient un cristal
reste fermé) ; `butin.yml` de KG_Rewards et de KV_Rewards, `fusion.yml` de KS_RewardsGUI ; `geyser-bedrock` 1.4.0 sur le
proxy.

**Compilé le 11/10/2026 (`sortie/KS_Meule-1.1.0.jar`). Non déployé. Statut : non testé en jeu.**
