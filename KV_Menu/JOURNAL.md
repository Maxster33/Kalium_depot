# KV_Menu - journal

Menus du serveur **Kanvas**. Hiérarchie des interfaces : **KLM_Menu** (catalogue, entrée « Kanvas ») → **KV_Menu** →
actions de **KV_Plots** (par son API `fr.kalium.kvplots.api.KanvasPlots`). Menus = fenêtres de dialogue de Minecraft
(boîte à outils `Gui` / `Lang` de KLM_Menu), textes modifiables dans `lang.yml`. Cahier des charges :
`KV_Plots/CAHIER_DES_CHARGES.md`.

## 1.2.0 - visites, titre et description (26/09/2026)

Cahier des charges : menu des visites (au hasard vers un plot pas encore noté ; liste des joueurs avec leurs têtes ;
liste à explorer soi-même) ; titre et description des plots.
- Accueil : **« Visiter les plots »** →
  - **« Au hasard : un plot à noter »** : téléportation vers un plot validé que le joueur n'a pas encore noté (message
    « Bravo ! » s'il n'en reste aucun) ;
  - **« Par joueur »** : **coffre** avec la tête de chaque joueur qui a au moins un plot (plots, validés, points dans
    l'info-bulle ; 45 par page, flèches, « Retour ») → clic = liste de ses plots. Coffre et non dialogue : on ne peut
    pas cliquer sur des têtes dans un dialogue ;
  - **« Tous les plots »** : liste par pages de 20 (titre ou « Plot n°X », taille, créateur, points ou « en
    travaux » ; description en info-bulle).
- **Fiche d'un plot** ouverte à tous : titre, description, créateur, éditeurs, état, points ; « Noter ce plot » (si
  notable) ; « Se téléporter » (n'importe quel plot) ; créateur : **« Titre et description »** (formulaire, codes `&`).
- « Mes plots » affiche le titre des plots.
- Nécessite **KV_Plots 1.3.0** (à déployer ensemble).

**Déployé sur Kanvas le 26/09/2026 (02:58) avec KV_Plots 1.3.0. Statut : non testé en jeu.**

## 1.1.0 - votes et validation (26/09/2026)

- Accueil : **« Voter pour ce plot »** (avec « ta note : n/5 » si déjà noté) quand on se trouve sur un plot qu'on
  peut noter → 5 boutons de 1/5 (rouge) à 5/5 (vert foncé) ; un nouveau vote remplace l'ancien.
- Fiche d'un plot : points, nombre de votes, moyenne ; **« Valider le plot »** / **« Rouvrir le plot »** avec
  confirmation (créateur).
- L'étoile du Nether n'est plus remise en place pendant un vote (inventaire remplacé par les terracottas de KV_Plots).
- Demande de LeKiwi06 : « la boussole et la nether star ne soient pas dans la hotbar quand on est dans un plot à nous
  qui n'est pas encore validé, pour pouvoir build plus facilement ». Sur un plot en travaux dont on est créateur ou
  éditeur, l'étoile est rangée en case 35 et la boussole de KLM_Menu en case 36 (dernière rangée de l'inventaire,
  hors barre d'objets ; ce qui s'y trouvait prend leur place dans la barre) ; en sortant du plot, elles reviennent
  à leur emplacement (étoile : `hub-item.slot` ; boussole : `compass.slot` de KLM_Menu), par échange. Rangées et non
  retirées : KLM_Menu 2.0.0 (Kanvas) ne redonne la boussole qu'à l'arrivée, et seulement si elle n'est nulle part.
  Vérifié à chaque changement de bloc (entrée / sortie de plot) et toutes les 2 s. Le menu reste accessible par
  `/kanvas`.
- Nécessite **KV_Plots 1.2.0** (à déployer ensemble).

**Déployé sur Kanvas le 26/09/2026 (02:37) avec KV_Plots 1.2.0. Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne »).**

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

**Déployé sur Kanvas le 26/09/2026 (02:02) avec KV_Plots 1.1.0. Statut : testé et confirmé par LeKiwi06 le 26/09/2026 (« tout fonctionne »).**
