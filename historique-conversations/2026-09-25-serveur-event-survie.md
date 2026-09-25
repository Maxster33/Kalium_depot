# 2026-09-25 — Serveur Event reconverti en survie, plugin KS_Dimensions (Maxster33)

- Plugin(s) concerné(s) : KS_Dimensions (nouveau) ; KalGames, KG_Bingo, KG_ScoreBoards (retirés d'Event) ; KLM_Menu
  (config d'Event).
- Versions avant / après (Event) : KalGames 1.15.1, KG_Bingo 1.2.0, KG_ScoreBoards 1.1.0 → retirés ; KLM_Menu 2.0.0
  gardé ; KS_Dimensions 1.0.0 ajouté.

## Demandé
- Mettre à jour les informations depuis GitHub, puis préparer le serveur Event pour une survie : nettoyer la map
  pour une survie classique ; enlever les plugins faits par Claude liés à KalGames ; ne garder que KLM_Menu, ouvert
  par `/menu` et non par la boussole ; créer KS-Dimensions (`/dimensions`, opérateurs seulement) : menu pour
  activer / désactiver les portails du Nether et de l'End ; mettre à jour GitHub.
- Guide du Claude de LeKiwi06 appliqué (WinSCP en ligne de commande, pas de captures d'écran).

## Fait
- Voir le compte rendu signé de Maxster33 du 2026-09-25 dans `REPRISE_PROJET.md` et `KS_Dimensions/JOURNAL.md`.

## Décisions
- Nouveau monde vierge (seed aléatoire), difficulté hard (Maxster33).
- Nom `KS_Dimensions` (charte : `_`), préfixe `KS_` = serveur Event.
- Portail désactivé : l'aller est bloqué, jamais le retour (choix de Claude, pour ne pas bloquer un joueur).
- `/menu` par un alias de `commands.yml` (aucun changement de code de KLM_Menu), comme sur Kixster.

## Reste à faire
- Démarrer Event et tester (`/menu`, `/dimensions`, portails) ; activer la destination `event` du lobby.
- Points ouverts d'Event : Floodgate, extension PlaceholderAPI mal placée, configs de hub des plugins tiers.

## Suite
- Demandé : installer Floodgate, remettre l'extension PlaceholderAPI à sa place, enlever PlayerKits2 (Event).
- Fait : Floodgate (config et clé du proxy reprises du lobby), extension dans `plugins/PlaceholderAPI/expansions/`,
  PlayerKits2 rangé dans `/plugins/_removed-playerkits2-1.23.3/`. Non testé (Event pas encore démarré).
