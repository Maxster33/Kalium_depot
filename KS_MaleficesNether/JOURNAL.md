# KS_MaleficesNether - journal

Serveur Kixster. Maléfices du Nether : des effets qui montent avec le temps passé d'affilée dans le Nether, et
l'amulette mangeuse de mal. Cahier des charges local `Malefices_Nether.md` (LeKiwi06, validé le 30/10/2026 :
« Oui, je valide »), à copier ici dans `CAHIER_DES_CHARGES.md` au déploiement.
Dépend de KLM_Menu (textes), KS_Effets (/effects) et KS_Jetons 2.3.0 (équipements spéciaux) ; KS_Atouts 1.2.0
(plastron anti-radiation) facultatif.

## 1.0.0 - compteur, maléfices, barre de boss, amulette (30/10/2026)

Demande de LeKiwi06 (29/10/2026) : « augmenter la difficulté du nether » ; écarté : les effets de potion sur les mobs
(une cinquantaine de mobs avec effets font crasher les clients Bedrock). Retenu : « appliquer des maléfices aux joueurs
en fonction du temps passé consécutivement dans le nether », jusqu'à ce qu'« au jour 20 ça devienne intenable et il
faut sortir ». Fermes AFK du Nether visées exprès (elles produisent de l'XP).

- **Compteur** (par compte, `CompteTest.de` : le compte de test dans le monde de test) : secondes passées dans le
  Nether, connecté, en survie ou en aventure. Pause en créatif, en spectateur et à la déconnexion. **Un jour = 20
  minutes réelles** (`jour-secondes`), jamais les jours du jeu (LeKiwi06 : « si un joueur dort dans l'overworld ça
  peut faire bugguer »). Hors du Nether, sans mourir : 2 secondes effacées par seconde (`descente-par-seconde`, un jour
  en 10 minutes). **La mort remet le compteur à zéro** (LeKiwi06 : ne pas perdre son équipement « si bêtement »).
  `donnees.yml` (enregistré chaque minute et à l'arrêt).
- **Effets** (`effets` du `config.yml`, ordre de LeKiwi06) : faim, faiblesse, fatigue de minage, poison, lenteur,
  obscurité, infestation, wither, lévitation, nausée : un de plus par jour (jours 1 à 10). Ensuite une montée par jour
  dans le même ordre : un niveau de plus, ou + 10 s pour l'obscurité, la lévitation et la nausée (10 s par activation
  au départ : « ces effets empêchent le joueur de jouer ») ; infestation sautée (pas de niveau dans le jeu) ;
  fatigue de minage au plus III.
- **Tirage** : au début du jour N, chaque effet tire N minutes différentes parmi les 20 du jour (« un timer random
  allant de 0 à 19 minutes ») et s'active à chacune (début au hasard dans la minute pour un effet de moins d'une
  minute) ; au jour 20, un effet d'une minute est continu. Tirage refait à chaque changement de jour et au redémarrage.
- **Pose** : effet sur le joueur, sans particules, icône gardée, remis toutes les 5 s (`rafraichissement-secondes`),
  durée au plus jusqu'à la fin de l'activation : le lait ne soulage que quelques secondes. L'icône sert aussi à
  KS_Atouts : sa propre lévitation (casque ventouse) n'en a pas.
- **Barre de boss rouge**, d'elle-même à chaque effet actif : « Maléfice (jour N) : Faim + Lenteur » ; elle montre le
  temps du premier effet actif jusqu'à sa fin. Message à chaque nouveau jour (« Nether : jour N. » + ce qui arrive ou
  monte) et en revenant dans le Nether avec un jour en cours. Effet « Maléfice du Nether » dans /effects : « sans
  limite » dans le Nether, dehors le temps avant de repasser sous le jour 1.
- **Plastron anti-radiation** (atout de KS_Atouts 1.2.0) porté : durée de chaque activation divisée par 2
  (`plastron-diviseur`).
- **Amulette mangeuse de mal** : craft en jambières, 7 chaînes en cuivre (toutes, oxydées ou cirées, choix de Claude)
  et une balise en bas au milieu ; recette montrée dans le livre quand le joueur a une balise. Livre de connaissances
  inutilisable, non empilable, marque `ks_maleficesnether:amulette`. Rangée dans la rangée « Tes équipements spéciaux »
  de /jetons (KS_Jetons 2.3.0), elle retire chaque effet actif en prenant **5 points d'XP par seconde et par niveau**
  (`amulette-points`) ; choix de Claude : du plus dangereux (fin de la liste) au moins dangereux, tant que le joueur a
  assez d'XP ; sans assez d'XP, l'effet s'applique. Valeur : 7 500 émeraudes (LeKiwi06). Id `amulette_mangeuse_de_mal`
  (KS_KaliumGive 1.13.0).

Limites, à faire avant ou au déploiement :
- **Apparence provisoire** de l'amulette (cœur de la mer, Java) en attendant la texture de LeKiwi06 ; sur Bedrock, un
  livre tant que geyser-bedrock n'a pas son entrée. Texture, entrée Bedrock et rareté (prix 7 500 dans `Raretes.java`
  de KS_Textures) à faire ensemble : KS_Textures et geyser-bedrock sont réservés par un autre chantier.
- Butin des récompenses : amulette dans `sortie/rewards/pools.py` (2 % du Légendaire) ; `butin.yml` (KG_Rewards,
  KV_Rewards) et `fusion.yml` (KS_RewardsGUI) régénérés le 30/10 **avec les prix actuels** (LeKiwi06 : « Oui, prix
  actuels » ; ils dataient du 11/10), vérifiés sans erreur (`verifier_butin.py`, `verifier_fusion.py`). Hors git, à
  envoyer sur Kal-Games, Kanvas et Kixster **après** KS_MaleficesNether et KS_KaliumGive 1.13.0 (sinon une récompense
  avec l'amulette reste bloquée). Pas de filtre de dimension dans les fichiers de butin (comme la Clé de l'End).
- Didacticiel préparé le 30/10 (`didacticiel.yml`, pas encore envoyé) : thème « Maléfices du Nether » (requiert
  KS_MaleficesNether), entrée « Plastron anti-radiation » (requiert KS_Atouts 1.2.0), entrées /jetons complétées
  (équipements spéciaux). À envoyer avec les plugins.
- Écrit sans serveur de test. Pour tester sans attendre 20 minutes par jour : `jour-secondes: 60` dans le
  `config.yml` du serveur (un jour par minute ; chaque effet du jour tient alors toute la minute).

Compilé le 30/10/2026 (`sortie/KS_MaleficesNether-1.0.0.jar`). **À déployer avec KS_Jetons 2.3.0, KS_Atouts 1.2.0 et
KS_KaliumGive 1.13.0.** Statut : non déployé, non testé en jeu.
