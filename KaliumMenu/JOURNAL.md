# Journal — KaliumMenu

Menu de navigation (Dialog natif, boussole) entre le lobby et les serveurs du réseau KaLium. Un seul jar,
installé sur chaque serveur Paper ; `role: lobby` sur le lobby (liste des serveurs), `role: backend` ailleurs
(retour au lobby + entrées locales, ex. "Hub de Kal-Games").

## 1.5.0 — Paramètres des téléportations

**Demande explicite de l'utilisateur** : "ajouter à kalium menu un bouton paramètre pour les op, pour pouvoir
activer ou désactiver des téléportations". Précisions via AskUserQuestion : un interrupteur **par destination** ;
une destination désactivée est **cachée pour tout le monde** (opérateurs compris).

- Bouton "Paramètres" dans le menu pour les joueurs ayant `kaliummenu.admin` (opérateurs par défaut). Il ouvre la
  liste des destinations de CE serveur (serveurs sur le lobby ; "Retour au lobby" + entrées locales ailleurs,
  quel que soit le monde), chacune "activée"/"désactivée" ; un clic bascule et enregistre.
- Enregistré dans `config.yml` : `disabled-destinations` (identifiants : clés de `servers`, nom du lobby pour
  "Retour au lobby", clés de `local-entries`). Réglage propre à chaque serveur.
- `/lobby` refusé si "Retour au lobby" est désactivé ("Cette téléportation est désactivée."). La commande /hub de
  KalGames n'est pas concernée (seul le bouton du menu est caché).
- Nouveaux textes avec valeurs par défaut intégrées (rien à ajouter dans les config.yml déjà déployés).

**Déployé le 23/09/2026 sur kal-games** (1.4.0 archivée dans `_removed-kaliummenu-1.4.0/`). Lobby : pas d accès SFTP depuis cette session, à déployer par l utilisateur.
