# 2026-10-23 — Parrainage, liste blanche de Kixster, statistiques

- Plugin(s) concerné(s) : KaliumRelay, KLM_Contacts, KS_RewardsGUI, KS_Parrainage (nouveau), KLM_Statistiques (nouveau),
  KS_Didacticiel (`didacticiel.yml` seulement).
- Versions avant / après : KaliumRelay 1.9.0 → 1.10.0 ; KLM_Contacts 1.4.0 → 1.5.0 ; KS_RewardsGUI 1.5.0 → 1.6.0 ;
  KS_Parrainage 1.0.0 ; KLM_Statistiques 1.0.0.

## Demandé

« Peux-tu créer cet ajout à partir du message d'annonce ? (système de parrainage, stats de temps de jeu visibles pour
les joueurs Bedrock, système de récompenses pour l'activité de l'invité, système de whitelist automatique via
parrainage, système de demande de whitelist via interface à la tentative de join pour les non whitelistés) », avec le
message d'annonce (menu Contacts > « parrainer un joueur », l'ami accepte et il est whitelisté sur Kixster, récompenses
pour les deux qui grossissent avec le temps de jeu de l'invité, sanction possible du parrain, demande avec le pseudo
Discord traitée par un admin).

## Fait

- Questions posées puis maquette des 6 menus (en coffre) et barème en .xlsx (`sortie/parrainage/bareme-parrainage.xlsx`,
  script `faire_xlsx.py`, hors dépôt) ; validés : « je valide ».
- KaliumRelay 1.10.0, KLM_Contacts 1.5.0, KS_RewardsGUI 1.6.0, KS_Parrainage 1.0.0, KLM_Statistiques 1.0.0 compilés ;
  `didacticiel.yml` complété. Rien de déployé.

## Décisions

- Demandes de liste blanche : menu admin en jeu (Modération › Demandes de whitelist), Discord plus tard (LeKiwi06).
- Filleul : un ami jamais venu sur Kixster, ou un autre pseudo (LeKiwi06).
- Barème : proposé par Claude (environ 10 % de ce que le filleul gagnerait en farmant), validé tel quel ; AFK non
  compté, pas de coffre à l'arrivée, pas de limite de filleuls (propositions de la feuille « Questions », validées).
- Statistiques : toutes les stats vanilla, dans « Informations », pour Java et Bedrock (LeKiwi06).
- Choix de Claude : traitement d'une demande par un petit coffre Accepter / Refuser (au lieu du clic gauche / droit de
  la maquette, que l'outil Contenant ne distingue pas) ; « actif » = tourner la tête, parler, commande, casser / poser /
  utiliser un bloc dans les 5 dernières minutes ; admins prévenus = liste `admins` de `relay.properties`.

## Reste à faire

- Déployer ensemble : KaliumRelay 1.10.0 (proxy), KLM_Contacts 1.5.0 (lobby, Kanvas / Kal-Games, Serveur Jeux,
  Kixster), et sur Kixster KS_Parrainage 1.0.0, KS_RewardsGUI 1.6.0, KLM_Statistiques 1.0.0, `didacticiel.yml`
  (`/didacticiel recharger`). Puis : `relay-token` dans `plugins/KS_Parrainage/config.yml`, `whitelist on`, vérifier
  `fusion.yml` de KS_RewardsGUI.
- Tests en jeu (il faut un compte qui n'est pas sur la liste blanche de Kixster) :
  1. Le compte non inscrit essaie d'aller sur Kixster : refusé, la fenêtre « Rejoindre Kixster » s'ouvre ; demande
     envoyée ; un admin la voit dans Modération › Demandes de whitelist, l'accepte ; le joueur entre sur Kixster.
  2. Parrainage : un joueur de Kixster ouvre Contacts › « Parrainer un joueur », propose à son ami ; l'ami accepte
     (message cliquable ou `/parrainage`) et entre sur Kixster ; « Mes filleuls » montre son temps de jeu.
  3. Après 1 h de jeu actif du filleul : un coffre « Non ouvert » de 5 pour chacun dans /rewards (`/ksparrainage` pour
     suivre). Joueur AFK : le temps ne monte pas.
  4. Joueur Bedrock : les mêmes menus, la tête des joueurs, la fenêtre de demande ; Informations › Statistiques, noms
     traduits.
