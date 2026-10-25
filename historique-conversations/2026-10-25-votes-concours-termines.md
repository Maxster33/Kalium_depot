# 25/10/2026 — Votes sur les plots des concours terminés

- Plugin concerné : KV_Plots (Kanvas)
- Versions avant / après : 1.7.0 (en service) / 1.8.0 (compilée, non déployée)

## Demandé
« j'aimerai qu'on puisse voter les builds de concours après la fin de la phase de vote »

## Fait
- KV_Plots 1.8.0 : à la fin des votes, les plots du concours restent notables (seul un concours annulé ferme les votes) ;
  classement enregistré dans `concours.yml` à la clôture et renvoyé tel quel par `plotsDuConcours` (points, votes et
  moyenne de la clôture) ; anciens concours : classement figé au premier démarrage d'après les notes actuelles.
- Annonce de fin des votes complétée : « Le classement est définitif, mais les plots restent notables. »
- Compilé (`sortie/KV_Plots-1.8.0.jar`).

## Décisions
- Classement figé à la clôture (choix de LeKiwi06), pour que le vainqueur affiché reste celui qui a été annoncé et
  récompensé.
- Anciens concours concernés aussi (choix de LeKiwi06).
- Notes tardives comptées pour les points des bâtisseurs comme sur un plot normal (choix de LeKiwi06) : rien à changer
  dans KV_Rewards.

## Reste à faire
- Déployer KV_Plots 1.8.0 sur Kanvas (seul), puis tester : noter un plot d'un ancien concours, vérifier que « Anciens
  concours » garde son classement et ses points, et que `concours.yml` contient les clés `classement`.
