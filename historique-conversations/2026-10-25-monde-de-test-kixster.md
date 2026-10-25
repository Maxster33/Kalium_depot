# 2026-10-25 — Monde de test en créatif sur Kixster

- Plugin(s) concerné(s) : KS_MondeTest (nouveau), KLM_Menu (déploiement seulement), config de SimpleClaimSystem
- Versions avant / après : KS_MondeTest — / 1.0.0 ; KLM_Menu sur Kixster 2.11.0 / 2.12.0

## Demandé
« j'utilisais Event comme serveur test avant, mais je ne l'ai plus. Peux-tu me créer une dimension séparée dans Kixster
où je peux tester tout ça en créa, sans risquer de me give par accident des choses sur la survie ? »

## Fait
- Proposition faite d'abord (fuites trouvées en lisant les plugins : extension du coffre de l'Ender, coffres de mort vers
  /rewards, dépôts de KS_Economy et KS_Jetons, lit et portails du monde de test), choix validés, puis réservation.
- KS_MondeTest 1.0.0 codé, compilé, poussé (commit da9b68a) : voir son `JOURNAL.md`.
- Déployé sur Kixster le 25/10/2026 à 03:10 avec KLM_Menu 2.12.0 (`espaces-interfaces`) et le monde de test sans claims
  dans SimpleClaimSystem (commit 89445a0). Réservation libérée.

## Décisions
- Nouveau plugin plutôt que KLM_Espaces (qui sépare à la connexion, pas au changement de monde, et tourne sur Kanvas).
- Terrain plat, bordure 500.
- Boutons « Monde de test » / « Retourner sur Kixster » dans la rubrique Modération du menu.
- Tout séparé comme Kal-Games / Kanvas, y compris le statut d'opérateur et le mode de jeu ; joueurs et tchat séparés.
- Opérateurs du monde de test : ceux que LeKiwi06 désigne (`/mondetest ajouter <joueur> op`). Pas de `/test mode`.
- Pas de garde-fou sur `/give` en survie (« je ne suis presque jamais en opérateur sur Kixster »).
- Commande `/mondetest` et non `/test` (la commande `/test` existe déjà dans Minecraft).

## Reste à faire
- Redémarrage de Kixster (LeKiwi06), puis dans la console : `mondetest ajouter LeKiwi06 op`.
- Plan de test : `KS_MondeTest/JOURNAL.md`.
- Supprimables par l'humain sur Kixster : `_removed-klm_menu-2.9.0/`, `2.4.1/`, `2.0.0/`.
- Plus tard si besoin : retouche de KS_AntiCheat (bouton Invsee de la rubrique Modération depuis la survie).
