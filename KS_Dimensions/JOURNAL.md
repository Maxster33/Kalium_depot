# KS_Dimensions - journal

Plugin du serveur Event (`event`, machine 7021, préfixe `KS_`). Dépend de KLM_Menu (boîte à outils des menus).

## 1.1.0 - entrée du Nether payante (31/10/2026)

**Demande (LeKiwi06, 31/10/2026, Kixster)** : faire payer 20 points pour entrer dans le Nether, « comme une
téléportation » ; les perles de l'Ender ne doivent plus passer par les portails, etc. Choix de LeKiwi06 : 20 points à
chaque fois (pas de téléportation gratuite ni de jeton), retour gratuit, menu de confirmation, perles et véhicules
avec joueur bloqués (mobs et objets seuls passent).

- Portail du Nether pris depuis le monde normal : menu « Entrer dans le Nether coûte 20 points » (Confirmer /
  Annuler), rouvert au plus toutes les 10 s tant que le joueur reste dans le portail. Confirmé : 20 points débités
  (KS_Economy, `debiter`), le passage est gardé (`plugins/KS_Dimensions/passes.yml`, survit au redémarrage) ; le
  joueur part dès que le portail se redéclenche (il y reste, ou y revient plus tard). Pas assez de points : message,
  rien n'est débité. KS_Economy absent : entrée refusée.
- Prix dans `config.yml` : `nether-entree-prix` (20 par défaut, 0 = gratuit) ; absent du config du serveur = 20.
- Retour du Nether vers le monde normal : gratuit, comme avant.
- Perles de l'Ender : une perle qui passe un portail du Nether depuis le monde normal est arrêtée, et une
  téléportation par perle du monde normal vers le Nether (perle restée là-bas) est annulée.
- Véhicule ou monture qui porte un joueur (même en passager de passager) : ne passe plus le portail vers le Nether ;
  message « Descends pour passer le portail du Nether ».
- Console : « <pseudo> paie 20 points pour entrer dans le Nether. », « <pseudo> entre dans le Nether (passage payé). »
- Restent ouverts (non demandés) : téléportations de KS_Teleport vers un point dans le Nether (payées comme une
  téléportation) et réapparition à une ancre de réapparition du Nether.

Compilé ; **déployé sur Kixster le 31/10/2026 à 18:21** (jar de 9 170 octets, même taille que celui du dépôt ; 1.0.0 dans `/plugins/_removed-ks_dimensions-1.0.0/`), **à redémarrer, non testé**. Le Nether est encore fermé sur Kixster (`nether-portals-enabled: false`).

## 1.0.0 - menu des portails du Nether et de l'End (25/09/2026)

**Demande (Maxster33, 25/09/2026)** : « créer un plug in KS-Dimensions qui s'ouvrira en jeu par un /dimensions
(utilisable que par les opérateurs) et qui sera un menu permettant d'activer / désactiver les portails du nether et
portail de l'end ». Nom écrit `KS_Dimensions` pour suivre la charte (`ARCHITECTURE_CIBLE.md`, § 1).

- `/dimensions` (permission `ksdimensions.admin`, par défaut : opérateurs) ouvre un menu (Dialog natif, boîte à
  outils de KLM_Menu) avec deux boutons : « Portail du Nether : activé / désactivé » et « Portail de l'End : ... ».
  Un clic inverse l'état, l'enregistre dans `config.yml` (`nether-portals-enabled`, `end-portals-enabled`), l'écrit
  dans la console (« ... désactivés par <pseudo> ») et rouvre le menu. Depuis la console : affiche l'état.
- Portail désactivé : on ne peut plus **aller** dans sa dimension **depuis le monde normal** (joueurs :
  `PlayerPortalEvent`, entités : `EntityPortalEvent`, annulés) ; message dans la barre d'action, au plus toutes les
  3 s. **Le retour reste possible** (portail du Nether pris depuis le Nether, portail de sortie de l'End) : un joueur
  déjà dans la dimension n'est jamais bloqué. Choix de Claude, signalé à Maxster33.
- Allumer un portail du Nether reste possible (seul le voyage est bloqué).
- Interface déclarée dans le catalogue « Interfaces » de KLM_Menu (rubrique administrateurs), comme les autres
  plugins du réseau.
- Textes dans `plugins/KS_Dimensions/lang.yml` (créé au démarrage, modifiable).

Compilé contre l'API de KLM_Menu 2.0.0 (inchangée en 2.1.0). **Déployé sur Event le 25/09/2026** (avec KLM_Menu
2.0.0 déjà en place). **Statut : non testé en jeu.**
