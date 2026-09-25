# KS_Dimensions - journal

Plugin du serveur Event (`event`, machine 7021, préfixe `KS_`). Dépend de KLM_Menu (boîte à outils des menus).

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
