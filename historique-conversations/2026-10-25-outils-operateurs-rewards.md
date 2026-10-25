# 2026-10-25 — Outils des opérateurs : don de coffres, historique, coffres de mort

- Plugin(s) concerné(s) : KS_CoffreMort, KS_RewardsGUI.
- Versions avant / après : KS_CoffreMort 1.0.3 → 1.1.0 ; KS_RewardsGUI 1.8.0 → 1.9.0.

## Demandé

« Peux-tu me créer un bouton spécial dans le /rewards dédié aux opérateurs pour donner des coffres de récompenses aux
joueurs ? comme variable il faut : nom du joueur, nom du coffre, valeur du coffre [...] il faut aussi y inclure un
historique de quelle personne a donné quoi à qui. Fais aussi une interface pour que les opérateurs voient les coffres
de morts actifs et puissent les transférer au rewards des joueurs concernés (si jamais le cas du bug de co de Lupo se
représente, on sera prêt). »

## Fait

- KS_CoffreMort 1.1.0 : `/coffres tous` (permission `kscoffremort.staff`) : tous les coffres de mort actifs, aperçu,
  transfert dans /rewards du mort (même hors ligne), `transferts.log`.
- KS_RewardsGUI 1.9.0 : bouton « Outils des opérateurs » dans /rewards (permission `ksrewardsgui.staff`) : donner un
  coffre (joueur, nom, valeur), historique des dons (`dons.yml`, lignes « DON » de `recuperations.log`), accès au menu
  des coffres de mort.
- Les deux compilés, puis déployés ensemble sur Kixster le 25/10/2026 à 03:09 (accord de LeKiwi06 : « Oui, déploie KS_CoffreMort 1.1.0 et KS_RewardsGUI 1.9.0 ensemble sur Kixster ») ; anciens jars dans `_removed-ks_coffremort-1.0.3/` et `_removed-ks_rewardsgui-1.8.0/`. Kixster à redémarrer.

## Décisions

- KS_RewardsGUI était en cours de modification par la session de l'enclume : LeKiwi06 a choisi d'attendre sa fin
  (plutôt qu'un nouveau plugin ou d'empiler en parallèle). KS_CoffreMort fait d'abord, KS_RewardsGUI après sa
  libération (03:03).
- Maquette des menus validée par LeKiwi06 (« La maquette me va »).
- Choix de Claude, à juger : origine « Staff » sur le coffre ; plafond de 10 000 émeraudes (tirages proportionnels à la
  valeur) ; joueur obligatoirement déjà venu sur le serveur ; transferts des coffres de mort notés dans leur propre
  journal (`transferts.log`), pas dans l'historique des dons ; didacticiel non touché (outils réservés aux opérateurs).

## Reste à faire

- Redémarrer Kixster (humain), lire le journal, puis tester en jeu : don à un joueur hors
  ligne, refus (joueur inconnu, valeur invalide), historique, transfert d'un coffre de mort d'un joueur hors ligne,
  « Retour » entre les menus, saisie depuis Bedrock.
