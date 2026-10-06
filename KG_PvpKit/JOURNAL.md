# JOURNAL — KG_PvpKit

PvP Kit de Kal-Games, sorti de KalGames (règle 2.2 de `REGLES.md`), comme la course de bateau (KG_BoatRace) et le
Parcours (KG_Parkour). Serveur : `kal-games` (machine 7001). Dépend de KalGames (1.22.0 minimum) et KG_ScoreBoards.

## 1.0.0 — sortie de KalGames, kits, barème, déclassement (28/09/2026)

**Demande de LeKiwi06** : « créer le plugin de PvP Kit, en sortant le code de KalGames, en ajoutant le barème de points ».
Choix de LeKiwi06 : les kits partent avec le jeu (PlayerKits2 peut être retiré) ; barème = éliminations, victoire +
place, bonus d'infériorité, multiplicateur de série, et un **déclassement** du kit choisi par chaque joueur pendant le
vote ; parties à plusieurs manches : points à chaque manche, sans bonus de fin de match.

**Repris tel quel de KalGames 1.21.0** (`PvpInstance`, `Kit`, `KitLibrary`, menus du vote, des équipes et des kits) :
vote du kit, jusqu'à 4 équipes, parties publiques (file, chacun pour soi par défaut) et privées (équipes, Haste,
1 / 3 / 5 manches, kit voté ou aléatoire), arène restaurée entre les manches, spectateurs. Même type `PVP_KIT` et mêmes
points d'arène : mini-jeu, arènes, classements et liste des kits proposés (dans `minigames.yml` de KalGames) repris sans
rien reconfigurer. Textes : ceux du `lang.yml` de KalGames (clés `pvp.*`, `vote.*`, `teams.*`, `admin.kit…`).

**Kits** : `plugins/KG_PvpKit/kits.yml`. Au **premier démarrage**, les kits de `plugins/KalGames/kits.yml` sont
recopiés ; ceux qui venaient de PlayerKits2 sont convertis (fichier relu une dernière fois dans le dossier de
PlayerKits2), un kit vide ou introuvable est signalé dans la console. Ensuite PlayerKits2 ne sert plus. Plus de bouton
« Importer depuis PlayerKits2 ». Gestion : Informations > Paramètres > PvP Kit > Kits du mini-jeu > Gérer les kits.

**Barème (par manche, points décimaux)** — d'abord les points, puis les multiplicateurs **additifs** (règle commune,
comme la course de bateau) :
- élimination : `points-kill` **5** (au dernier joueur qui a frappé la victime dans les 10 s, même si elle tombe dans
  le vide ; compte aussi pour une équipe qui perd) ;
- victoire de la manche : `points-round-win` **10** ; 2e place (à 3-4 équipes, dernière équipe éliminée) :
  `points-second` **4** ; 3e place (à 4 équipes) : `points-third` **2** ;
- bonus d'infériorité (gagnants) : `points-inferiority` **5** par joueur d'écart avec la plus grande équipe adverse ;
- série de manches gagnées d'affilée (gagnants) : **x1,25** dès 3 (`streak-1-wins`, `streak-1-bonus-pct`), **x1,5**
  dès 5 (`streak-2-…`) ; remise à zéro à chaque manche perdue (une manche nulle ne change rien) ; gardée en mémoire,
  remise à zéro au redémarrage du serveur ;
- déclassement : **+50 %** par niveau (`downgrade-bonus-pct`), niveau 4 = x3.
Exemple : victoire avec 2 éliminations, série de 3, déclassement 2 : (10 + 10) × (1 + 0,25 + 1) = 45 pts.
Chaque joueur reçoit le détail (« +45 pts (2 élim. 10 + victoire 10 x2,25 : série de 3, déclassement 2) »).
Estimation : en 1 contre 1, ~15 manches en 30 min, 60 % de victoires → ~135 pts, la référence de
`EQUILIBRAGE_POINTS.md` ; à vérifier sur de vraies parties (événement « round » du journal).
Anciennes clés `points-win` / `points-bonus` : plus utilisées (nouvelles clés pour que les valeurs par défaut
s'appliquent même si l'ancien réglage a été enregistré).

**Déclassement** (`Downgrade`) : niveau 0 à 4 (`downgrade-max-level`) choisi par chaque joueur dans le menu du vote ou
par « Déclassement du kit » dans le menu de la partie ; modifiable tant que le joueur n'est pas en combat, gardé d'une
manche à l'autre. Chaque niveau :
- retire **un niveau à chaque enchantement** (un enchantement qui tombe à 0 disparaît ; livres enchantés compris) ;
- retire **20 %** (`downgrade-consumables-pct`) de chaque sorte de consommable (aliments, potions, perles, flèches,
  totems, charges de vent, fusées, boules de neige, œufs, fioles d'XP ; pas les blocs), compté sur tout le kit par
  objet identique, arrondi au plus proche ;
- objets **en un seul exemplaire** (précision de LeKiwi06) : totem retiré à partir du niveau 2 ; pomme de Notch
  retirée au niveau 4 ; potion jamais retirée, chaque effet perd un niveau par niveau de déclassement, sans descendre
  sous le niveau I (vitesse V au niveau 4 = vitesse I).

**Journal des parties** (KG_ScoreBoards) : événement « points » par joueur (comme KalGames : `/classements verifier`
retrouve les points non comptés) et événement « round » par manche (kit, équipes, durée du combat, éliminations, place,
déclassement, série, multiplicateur, points) pour caler le barème.

**Crochets de KalGames 1.22.0 utilisés** : `MinigameType.createForm` (formulaire de partie privée), `adminAction` /
`adminInfo` (page du jeu dans les paramètres), `GameInstance.onRespawned`, `openVote` (`/kalgames vote`),
`menuInfo` / `menuActions` (vote, déclassement, équipes dans le menu de la partie).

Limites : la passerelle vers l'ancien datapack (`scoring.bridge.win-commands` de KalGames) n'est plus appelée pour les
points du PvP Kit (comme la course de bateau) ; `combat-leave-commands` l'est toujours. Un joueur qui quitte avant la
fin de la manche perd ses points de la manche. Le vote montre le contenu du kit complet,
pas celui du kit déclassé.

**Déploiement** : **avec KalGames 1.22.0** (sinon plus de PvP Kit : son mini-jeu reste gardé de côté, sans perte).
Garder PlayerKits2 (ou au moins son dossier `kits`) jusqu'au premier démarrage de KG_PvpKit (conversion des kits), puis
le ranger dans `_removed-playerkits2-…`. Nouvelles clés : valeurs par défaut du code tant qu'elles ne sont pas changées
dans le panneau admin. **Statut : déployé sur Kal-Games (7001) le 28/09/2026 à 21 h 44 avec KalGames 1.22.0 (serveur éteint, par Claude de LeKiwi06 en ligne de commande WinSCP) ; non testé en jeu.**

## 1.0.1 - textes plus courts (02/10/2026, LeKiwi06)

Demande de LeKiwi06 (02/10/2026) : dans les réglages des jeux pour les opérateurs, les jauges étaient très longues
(nom et explication dans la jauge, élargie par KLM_Menu 2.5.0) ; Minecraft ne permettant du texte qu'en haut d'une
fenêtre, « écrit des textes plus courts dans les champs de jauge et boutons ». Mesure de tous les textes (police de
Minecraft) : chaque champ et bouton tient maintenant dans sa largeur normale (260 / 240 pixels).
- Réglages : « Série : 1er / 2e palier (manches) », « Déclassement : consommables (%) », « Attente avant lancement (s) »,
  « Arènes préchargées » (détail dans l'explication). Clés inchangées.
- Création d'une partie : « 3 manches (2 victoires) », « 5 manches (3 victoires) », « Kit aléatoire (même pour tous) »
  (nouvelles clés de langue `menu.rounds-3-court`, `menu.rounds-5-court`, `menu.kitmode-random-court`).

**Déployé sur Kal-Games le 02/10/2026 à 17:49 (LeKiwi06 ; 1.0.0 dans `_removed-kg_pvpkit-1.0.0/`), actif après redémarrage.
Statut : **testé et confirmé par LeKiwi06 le 02/10/2026**.**

## 1.1.0 - nombre de joueurs possibles au survol du jeu (06/10/2026, Maxster33)

**Demande de Maxster33** : afficher le nombre de joueurs possibles au survol de chaque jeu du menu de Kal-Games (voir
KalGames 1.23.0).

- Plage déclarée à KalGames : **2** (deux équipes d'un joueur) **à nombre d'équipes de l'arène x 4** (équipes de 4 au
  plus en partie privée ; le maximum public, équipes max x joueurs par équipe, est aussi pris en compte). Avec une
  arène à 4 départs : « Joueurs : 2 à 16 ».
- **Exige KalGames 1.23.0 : à déployer ensemble.**

**Déployé sur Kal-Games le 06/10/2026 à 13:55 (Maxster33 ; 1.0.1 dans `_removed-kg_pvpkit-1.0.1/`), actif après redémarrage. Statut : non testé en jeu.**
