# JOURNAL — KG_Parkour

Parcours de Kal-Games, sorti de KalGames (règle 2.2 de `REGLES.md`), comme la course de bateau (KG_BoatRace).
Cahier des charges : `CAHIER_DES_CHARGES.md` (même dossier). Serveur : `kal-games` (machine 7001).

Versions plus anciennes et historique : `JOURNAL_ARCHIVE.md` (à chercher, pas à lire en entier).

## 1.7.0 — masse, bonus, arrivée par bouton, vision nocturne (31/10/2026)

**Demande de LeKiwi06**, pour la map « Ascension vers l'enfer » (caverne) : une section se fait avec une masse,
prêtée le temps de ce point de contrôle, sans que les joueurs puissent se taper ; des bonus or +1, diamant +3,
émeraude +5, chacun à prendre seulement à son point de contrôle ; l'arrivée est un bouton à cliquer ; vision nocturne.
Ses réponses : on frappe des mobs pour rebondir ; Wind Burst I ; bonus = plateformes de la taille d'un point de
contrôle, rareté choisie dans l'interface (certaines en blocs invisibles) ; points des bonus multipliés comme ceux de
la section.

Tout se règle dans l'éditeur d'arène, rien ne change sur les maps qui n'ont pas ces réglages :
- **Masse** : réglage « Masse jusqu'au point suivant » de chaque point de contrôle. Après ce point, masse Wind Burst I
  incassable en case 3, mise en main ; reprise au point suivant (et en partant en spectateur). Aussi en entraînement.
- **Cibles de la masse** (liste de points, réglage « Mob », nom anglais, `husk` par défaut) : les mobs posés dans la
  map de construction ne sont PAS copiés dans les arènes (les modèles ne gardent que les blocs), le jeu fait donc
  apparaître un mob immobile par point au départ (sans IA, silencieux, sans gravité, sans collision, sans
  équipement, 1 024 PV, soigné et remis en place à chaque passage de la boucle, refait s'il disparaît ; retiré à la
  fin). KalGames annule tout coup dans une arène et Wind Burst ne part que si le coup touche (lu dans `Player.attack`
  du jeu 26.3) : `Masse` rétablit (HIGHEST) le coup d'un coureur, masse en main, sur une cible, dans une section à la
  masse. Entre joueurs rien ne change (`canFight` faux). Le souffle d'un Wind Burst ne projette que celui qui vient de
  frapper une cible (1 s) : les autres coureurs ne peuvent pas être poussés (`EntityKnockbackEvent`, cause EXPLOSION).
- **Bonus** (liste de points) : réglages « Rareté » (1 or +1, 2 diamant +3, 3 émeraude +5) et « Point de contrôle
  du bonus » (0 = avant le premier). Pris en passant dans le rayon des points de contrôle, une fois par tour,
  seulement entre son point et le suivant (sinon message gris). Points ajoutés à la base de la section en cours,
  donc multipliés à sa validation ; détail « bonus +N » dans le message et `bonus` dans le journal des points. En
  entraînement : message seulement.
- **Arrivée par bouton** : si un bouton est sur le point « Arrivée » ou à 1 bloc, la zone d'arrivée ne compte plus :
  clic droit sur le bouton après tous les points de contrôle (sinon « Passe d'abord tous les points de contrôle »).
- **Vision nocturne** (point facultatif, posé n'importe où) : vision nocturne sans particules pour tous les joueurs
  de la partie (gradins, course, spectateurs).

Nouvelles clés (textes par défaut dans le code) : `race.mace-item-name`, `race.mace-item-lore`, `race.bonus`,
`race.bonus-training`, `race.bonus-wrong`, `race.bonus-wrong-start`, `race.button-early`.

Limites : une autre protection du serveur (WorldGuard…) qui annulerait aussi les coups sur les mobs empêcherait le
rebond ; que le souffle de Wind Burst sur celui qui frappe passe bien par `EntityKnockbackEvent` reste à voir en jeu
(sinon il ne serait pas bloqué non plus, rien ne casse).

**Déployé sur Kanvas (espace Kal-Games) le 31/10/2026 à 11:54, serveur en marche (1.6.0 dans
`_removed-kg_parkour-1.6.0/`), actif au redémarrage. Statut : non testé en jeu.** À faire dans l'éditeur d'arène de la map : cocher la masse sur
le point de contrôle qui précède la section, poser les cibles, les bonus (rareté, point de contrôle), le point
« Vision nocturne », et le point « Arrivée » sur le bouton.

## 1.8.0 — cibles de la masse relevées à la capture (31/10/2026)

**Demande de LeKiwi06** : les golems de fer de la map de construction (invoqués avec `NoAI:1b`) doivent se placer
d'eux-mêmes quand il capture la map à nouveau.

- À chaque capture d'une arène du Parcours (KalGames 1.29.0, `ArenaCapturedEvent`), tous les mobs **sans IA** de la
  zone deviennent les « Cibles de la masse » de l'arène : même place, même orientation, réglage « Mob » = leur
  espèce (`iron_golem`…), triés de bas en haut. La liste des cibles est remplacée ; si la zone n'a aucun mob sans
  IA, les cibles déjà posées restent. Message au modérateur « Cibles de la masse relevées : N mob(s) sans IA de la
  zone. » (nouvelle clé `race.capture-targets`) et ligne dans la console.
- En course, ce sont toujours des copies faites par le jeu (sans IA, 1 024 PV soignés, sans effet de potion : les
  effets sur les mobs font planter Bedrock) ; la Résistance de la commande d'origine n'est pas reprise.
- Les chunks de la zone sont chargés pour lire leurs entités (déjà chargés par la capture, en principe).

**Compilé, non déployé. Statut : non testé en jeu.** Exige KalGames 1.29.0 (sinon KG_Parkour ne démarre pas).

**Déployé sur Kanvas (espace Kal-Games) le 31/10/2026 à 12:14 avec KalGames 1.29.0, serveur en marche ; la 1.7.0 (envoyée à 11:54, jamais active) dans `_removed-kg_parkour-1.7.0/`. Actif au redémarrage. Statut : non testé en jeu.**

## 1.8.1 — pas de chrono sur les maps avec lave (31/10/2026)

**Demande de LeKiwi06** : sur « Ascension vers l'enfer », on ne doit pas tomber au temps ; on perd seulement en
mourant quand la lave a recouvert son dernier point de contrôle.

- Sur une map qui a le point « Départ de la lave », le chrono par joueur (« Chrono de départ » et temps ajouté aux
  points de contrôle) n'est plus appliqué : plus de « Temps écoulé », plus de « Chrono » dans l'action bar. La lave
  reste la seule élimination. Les autres maps gardent le chrono.
- Le « Temps limite de la course » (global, 0 par défaut) s'applique toujours s'il est réglé.

**Compilé, non déployé. Statut : non testé en jeu.**

**Déployé sur Kanvas (espace Kal-Games) le 31/10/2026 à 18:00, serveur en marche ; jar de la même taille que celui du dépôt (50 363 octets), la 1.8.0 dans `_removed-kg_parkour-1.8.0/`. Actif au redémarrage. Statut : non testé en jeu.**

## 1.8.2 — lave posée petit à petit (31/10/2026)

**Demande de LeKiwi06** : la lave fait des pics de lag quand une couche se pose.

- Avant, toute la couche (toutes les cases d'air de la hauteur, dans toute l'arène) était posée d'un coup, dans un
  seul tick. Elle est maintenant posée petit à petit : au plus « Lave : blocs posés par tick » blocs par tick du
  serveur (nouveau réglage `lava-blocks-per-tick`, 150 par défaut, soit 3 000 blocs par seconde), et le nombre de
  cases parcourues par tick est limité aussi. Moins de travail d'un coup pour le serveur, les clients Java et
  Geyser (un bloc changé = un paquet pour Bedrock).
- La hauteur de la lave qui compte pour les joueurs (retour au point de contrôle, élimination, action bar) monte dès
  le début de la couche, comme avant. La couche suivante attend que la précédente soit finie.

**Compilé, non déployé. Statut : non testé en jeu.**

**Déployé sur Kanvas (espace Kal-Games) le 31/10/2026 à 20:11, serveur en marche ; jar de la même taille que celui du dépôt (50 700 octets), la 1.8.1 dans `_removed-kg_parkour-1.8.1/`. Actif au redémarrage. Statut : non testé en jeu.**
