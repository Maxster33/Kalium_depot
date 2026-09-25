# KV_Menu - journal

Menus du serveur **Kanvas**. Hiérarchie des interfaces : **KLM_Menu** (catalogue, entrée « Kanvas ») → **KV_Menu** →
actions de **KV_Plots** (par son API `fr.kalium.kvplots.api.KanvasPlots`). Menus = fenêtres de dialogue de Minecraft
(boîte à outils `Gui` / `Lang` de KLM_Menu), textes modifiables dans `lang.yml`. Cahier des charges :
`KV_Plots/CAHIER_DES_CHARGES.md`.

## 1.0.0 - création (26/09/2026)

Demande de LeKiwi06 : « une interface au lieu de juste avoir les commandes ».
- Ouverture : `/kanvas` (alias `/kv`, `/plots`) et le catalogue de KLM_Menu.
- **Accueil** : « Réserver un plot moyen (n/max) », « Réserver un grand plot (n/max) » (avec confirmation : le plot
  libre où l'on se tient, sinon le plus proche du centre), « Mes plots » ; rappel du plot sur lequel on se trouve.
- **Mes plots** : un bouton par plot (créateur ou éditeur) → **fiche** : créateur, éditeurs, état ; « Se téléporter » ;
  « Éditeurs » (créateur seulement).
- **Éditeurs** : champ « Pseudo du joueur » + « Ajouter l'éditeur saisi » ; « Retirer <pseudo> » avec confirmation.
- Les refus de KV_Plots (plus de place, pas le créateur...) s'affichent dans un petit message avec « OK ».
- Dépend de KLM_Menu et KV_Plots (à déployer ensemble).

**Statut : non testé en jeu, non déployé.**
