# 2026-10-13 — Bateau à jukebox

- Plugin(s) concerné(s) : KS_JukeboxBoat (nouveau), KS_Crafts
- Versions avant / après : KS_JukeboxBoat (n'existait pas) / 1.0.0 compilée ; KS_Crafts 1.12.0 en service / 1.13.0
  compilée. Les deux déployées sur Event le 13/10/2026 à 07:39 ; Kixster inchangé.

## Demandé

LeKiwi06 : « j'aimerai créer un plugin de bateau avec jukebox, un bateau très similaire au bateau avec coffres, mais
là il peut lire les disques de notre inventaire ».

## Fait

- Questions posées avant le code (apparence, écoute, disque, obtention), puis maquette du menu et questions sur
  l'ouverture à bord et le nom.
- KS_JukeboxBoat 1.0.0 : objet, bateau posé avec son jukebox affiché, menu des disques, musique qui suit le bateau,
  bateau cassé. Détail : `KS_JukeboxBoat/JOURNAL.md`.
- KS_Crafts 1.13.0 : recette bateau + jukebox, une par bois. Détail : `KS_Crafts/JOURNAL.md`.
- Les deux compilent. Sur « oui déploie sur event » : envoyés sur Event à 07:39 (jar de KS_Crafts en place vérifié
  identique à la référence avant l'envoi ; 1.12.0 dans `_removed-ks_crafts-1.12.0/`). Actifs après redémarrage, non
  testés. Réservations libérées.

## Décisions

Choix de LeKiwi06 :

- Jukebox visible à l'arrière du bateau sur Java (modèle 3D de bloc, 2e place prise), plutôt qu'un vrai bateau à
  coffre vu pareil sur Java et Bedrock : les joueurs Bedrock voient un bateau vide.
- Musique pour tous les joueurs proches, plutôt que pour les seuls passagers.
- Le disque va dans le bateau (rendu à l'arrêt, lâché si le bateau est cassé), plutôt que de rester dans
  l'inventaire.
- Craft bateau + jukebox, plutôt que bateau à coffre + jukebox ou pas de craft.
- Menu : disques de l'inventaire en haut du coffre, plutôt qu'un clic direct dans son propre inventaire.
- À bord : touche de saut seulement (ni clic droit avec un disque en main, ni coup dans le vide).
- Nom : KS_JukeboxBoat, plutôt que KS_BateauJukebox.

Choix de Claude, signalés : recette dans KS_Crafts (comme les autres objets) ; seul un joueur monte à bord ; le
bateau à jukebox ne sert pas d'ingrédient et ne sort pas d'un distributeur ; n'importe qui peut reprendre le disque
d'un bateau ; le disque reste dans le bateau à la fin du morceau ; réglages d'affichage du jukebox dans `config.yml`.

## Reste à faire

- Fait : Event redémarré à 07:44, journal lu (les deux plugins activés sans erreur, 78 crafts).
- Après les tests : déployer KS_JukeboxBoat 1.0.0 et KS_Crafts 1.13.0 ensemble sur Kixster, avec l'accord de LeKiwi06.
- Tests en jeu : recette et livre de recettes ; pose, jukebox bien placé à l'arrière (seul et avec un conducteur,
  bateau et radeau) et qui tourne avec le bateau ; conduite normale ; menu à pied et à bord ; musique qui suit le
  bateau ; reprise du disque ; bateau cassé (objet et disque rendus) ; déconnexion à bord puis retour ; passage par un
  portail ; côté Bedrock : menu, touche de saut, musique.
