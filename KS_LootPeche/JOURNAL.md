# KS_LootPeche - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - pêche sans livres enchantés (25/09/2026)

Demande de Maxster33 : « les livres enchantés sont retirés des loot table » ; réponse : tirer au sort un autre trésor.
- Un livre enchanté pêché est remplacé par un des autres trésors vanilla, à chances égales : arc, canne à pêche
  (tous deux enchantés niveau 30 avec usure aléatoire jusqu'à 25 %, comme en vanilla), étiquette, carapace de
  nautile, selle. Correction de la proposition du 25/09 : le nénuphar n'est pas un trésor (il est dans les déchets).
- Les chances d'obtenir un trésor ne changent pas.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - livres enchantés de retour, sans Raccommodage (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « remet les livre enchantés dans la pêche , mais retire le fait que le mending puisse spawn ».
- Les livres enchantés sont de nouveau pêchés, avec la chance vanilla (plus de remplacement par un autre trésor).
- Raccommodage : un livre, un arc ou une canne à pêche pêché qui l'a est réenchanté comme le fait la pêche vanilla
  (niveau 30, enchantements de trésor compris) jusqu'à ne plus l'avoir ; l'usure tirée par le jeu est gardée. Au bout
  de 50 tirages (jamais atteint en pratique), Raccommodage est simplement retiré.
- Les autres enchantements de trésor (Gel, Malédictions, Vitesse des âmes...) restent possibles, comme en vanilla.
- Didacticiel : texte de « Pêche » mis à jour, marqué Nouveau le 26/10.
- Têtes de mobs marins, tête de wither squelette, fiole sinistre et pomme de Notch : proposition de chances envoyée
  (`sortie/peche/proposition-peche-et-estomac.xlsx`), à coder après validation.

**Compilé, non déployé, non testé en jeu.**

## 1.2.0 - prises rares : têtes de mobs marins, tête de wither squelette, fiole sinistre, pomme de Notch (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « ajoute aussi qu'on puisse pécher les têtes de mobs marins ( y compris les bébés et les axolotl ,
plus la tête est rare , moins elle a de chance d'être péchée ) , et ajoute aussi de façon extrêment rare de pécher une
tête de wither ( elle aura forcement tout les défauts ) , la potion de bad omen , et la pomme de notch ». Chances
proposées (`sortie/peche/proposition-peche-et-estomac.xlsx`), puis « c'est un peux trop rare rend tout 2 fois plus
courant » : appliqué.

- Nouveau `config.yml` (`prises-rares`, une ligne par objet : 1 chance sur N par prise ; modifiable, redémarrage
  nécessaire). Un seul tirage par prise sur toute la table ; si un objet sort, il **remplace** la prise (poisson,
  déchet ou trésor). Chance de la mer et Appât n'y changent rien (Appât fait seulement pêcher plus vite).

| Objet | 1 chance sur | Temps moyen sans Appât / Appât III |
|---|---|---|
| Têtes du palier B (23 : morue, saumon, poisson-globe, poisson tropical, poulpes et bébés, poulpes luisants et bébés, tortue et bébé, nautile et bébé, têtard, gardien, noyé, axolotls rose, brun, doré, cyan et leurs bébés), chacune | 1 000 | 5,8 h / 2,2 h |
| Têtes du palier C (dauphin, bébé dauphin, nautile-zombie, nautile-zombie des coraux), chacune | 5 000 | 29 h / 11 h |
| Têtes du palier D (axolotl bleu, bébé axolotl bleu, bébé noyé), chacune | 25 000 | 146 h / 56 h |
| Tête de grand gardien | 100 000 | 583 h / 222 h |
| Tête de wither squelette (sale, endommagée, désactivée : `creerTete` de KS_Decapitator) | 50 000 | 292 h / 111 h |
| Fiole sinistre (Mauvais présage, niveau I à V au hasard) | 50 000 | 292 h / 111 h |
| Pomme dorée enchantée | 75 000 | 438 h / 167 h |

  Un de ces objets : environ 1 prise sur 42 (toutes les 15 min sans Appât). Valeur moyenne au barème : environ 57
  émeraudes par heure de pêche sans Appât, 149 avec Appât III.
- Pas de grenouille (pas marine). Fermes de pêche AFK : pas de règle ajoutée, KS_AntiCheat expulse déjà un joueur AFK
  au bout de 60 minutes.
- `softdepend: [KS_Decapitator]` (`build.sh` : compiler KS_Decapitator d'abord) ; sans lui, les têtes sont ignorées
  (avertissement au démarrage), le reste marche. Le journal du serveur donne « N prises rares. » au démarrage (35
  attendues).
- Didacticiel : texte de « Pêche » complété (sans dévoiler les objets), date Nouveau du 26/10 inchangée.

**Compilé, non déployé, non testé en jeu.** Contient la 1.1.0, jamais déployée.

**Déployé sur Kixster le 26/10/2026 à 19:48 (LeKiwi06 : « ok vas y déploie ça me conviens »), actif après redémarrage de Kixster. Statut : non testé en jeu.**
