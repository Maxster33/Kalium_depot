# KV_Menu - journal

Menus du serveur **Kanvas**. Hiérarchie des interfaces : **KLM_Menu** (catalogue, entrée « Kanvas ») → **KV_Menu** →
actions de **KV_Plots** (par son API `fr.kalium.kvplots.api.KanvasPlots`). Menus = fenêtres de dialogue de Minecraft
(boîte à outils `Gui` / `Lang` de KLM_Menu), textes modifiables dans `lang.yml`. Cahier des charges :
`KV_Plots/CAHIER_DES_CHARGES.md`.

## 1.0.0 - création (26/09/2026)

Demande de LeKiwi06 : « une interface au lieu de juste avoir les commandes ».
- Ouverture : **étoile du Nether** en emplacement 4 (`hub-item.slot` / `hub-item.material`), `/kanvas` (alias `/kv`,
  `/plots`) et le catalogue de KLM_Menu. L'étoile est donnée dans le monde des plots (arrivée, changement de monde,
  réapparition), verrouillée (ni jetée, ni déplacée, ni clonée), et remise à sa place toutes les 2 s (en créatif, le
  client peut modifier l'inventaire sans passer par les clics habituels) ; l'objet qui occupait l'emplacement est
  rangé ailleurs dans l'inventaire (jeté au sol si l'inventaire est plein). Retirée hors du monde des plots.
  Plus tard (votes) : remplacée par les terracottas dans les plots validés pas encore notés ; bouton « Voter » dans le
  menu pour un plot déjà noté.
- **Accueil** : « Réserver un plot moyen (n/max) », « Réserver un grand plot (n/max) » (avec confirmation : le plot
  libre où l'on se tient, sinon le plus proche du centre), « Mes plots » ; rappel du plot sur lequel on se trouve.
- **Mes plots** : un bouton par plot (créateur ou éditeur) → **fiche** : créateur, éditeurs, état ; « Se téléporter » ;
  « Éditeurs » (créateur seulement) ; « Remettre à zéro » et « Supprimer le plot » avec confirmation (créateur si le
  plot n'est pas validé, staff toujours).
- **Éditeurs** : champ « Pseudo du joueur » + « Ajouter l'éditeur saisi » ; « Retirer <pseudo> » avec confirmation.
- Les refus de KV_Plots (plus de place, pas le créateur...) s'affichent dans un petit message avec « OK ».
- Dépend de KLM_Menu et **KV_Plots 1.1.0** (à déployer ensemble).

**Déployé sur Kanvas le 26/09/2026 (02:02) avec KV_Plots 1.1.0. Statut : non testé en jeu.**
