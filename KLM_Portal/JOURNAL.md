# KLM_Portal - journal

Plugin réseau (préfixe `KLM_`), installable sur n'importe quel serveur Paper. Dépend de **KLM_Menu 2.2.0** (destinations
et leur activation) et de **WorldGuard** (régions des portails ; WorldEdit ou FAWE pour les dessiner).

## 1.0.0 — portails reliés aux destinations de KLM_Menu (26/09/2026)

**Demande de LeKiwi06** : remplacer ConditionalEvents et PyxelRegions (zones des portails du lobby, qui appelaient
VelocityCommandForward) par un KLM_Portal « relié au KLM_menu pour que les portails se déactivent quand on désactive
le bouton dans le KLM_menu ». Choix de LeKiwi06 : zones = **régions WorldGuard** (comme les autres plugins) ; portail
désactivé = **message et recul seulement** (le visuel viendra plus tard) ; utilisable **sur tous les serveurs**.

- Un portail = une région WorldGuard (dans un monde) + une destination de KLM_Menu, dans `config.yml` (`portals:`).
- Entrer dans la région envoie le joueur vers la destination, comme un clic sur son bouton (API de KLM_Menu 2.2.0 :
  changement de serveur par le canal BungeeCord, ou commande locale pour une entrée de `local-entries`). Déclenché
  seulement à l'**entrée** dans la région, puis délai de 3 s par joueur (`cooldown-seconds`) contre les envois en
  double.
- Destination désactivée dans KLM_Menu (menu > Paramètres) : l'entrée est annulée, le joueur est repoussé
  (du centre de la région vers lui, `push-strength`), message « Ce portail est désactivé. » dans la barre d'action
  (au plus toutes les 3 s). Aucun état propre à KLM_Portal : réactiver le bouton réactive le portail tout de suite.
- `/klmportal` (alias `/portail`, permission `klmportal.admin`, opérateurs) : `set <région> <destination>` (en jeu,
  dans le monde de la région ; autocomplétion des régions et des destinations ; avertit si la destination n'est pas
  un bouton de KLM_Menu), `remove <région>` (la région WorldGuard est gardée), `list` (état de chaque portail,
  région manquante signalée), `reload`.
- Au démarrage : régions WorldGuard introuvables signalées dans la console (portail inactif).
- Textes dans `plugins/KLM_Portal/lang.yml` (créé à l'usage, modifiable).

Limites : un portail n'agit que sur les **déplacements à pied** (pas sur une téléportation, ex. perle, qui arriverait
dans la région) ; un joueur qui se connecte déjà dans la région n'est envoyé qu'après en être sorti et revenu.

**Déploiement prévu** : lobby, avec KLM_Menu 2.2.0 (règle 3.5), puis retrait de ConditionalEvents et PyxelRegions
(dans `_removed-…`) une fois les portails testés. **Statut : compilé, non déployé, non testé en jeu.**
