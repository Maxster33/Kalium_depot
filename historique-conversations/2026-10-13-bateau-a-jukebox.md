# 2026-10-13 — Bateau à jukebox

- Plugin(s) concerné(s) : KS_JukeboxBoat (nouveau), KS_Crafts ; GrimAC (constat seulement)
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

- Journal d'Event lu après le redémarrage de 07:44 : les deux plugins activés sans erreur.
- LeKiwi06 a testé la 1.0.0 : « ça fonctionne bien, si je le casse alors qu'il joue une musique il rend le disque ».
- Deuxième demande : « un bateau avec coffre et jukebox ? on ne peux pas monter dedans, mais on peux le tracter avec
  une laisse [...] le juke box lirait en automatique les disque présent dans le coffre en boucle ». KS_JukeboxBoat
  1.1.0 et KS_Crafts 1.14.0 compilés, non déployés.
- Défaut signalé par LeKiwi06 (capture à 08:01) : bateau à jukebox tiré par un Happy Ghast, joueur descendu aussitôt,
  jukebox fantôme laissé en l'air. Cause de la descente : GrimAC (3 signalements « Simulation » à 08:00 dans sa base,
  lue hors dépôt) ; cause du fantôme : le plugin. Fantôme corrigé en 1.1.0.
- Question de LeKiwi06 : « est ce qu'il y a possibilité de render un mob avec une tête de jukebox sur bedrock ? ».
  Réponse : probablement (porte-armure invisible coiffé d'un jukebox), commande d'essai donnée ; pas encore essayé.

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

Choix de LeKiwi06 pour le bateau à coffre : les deux bateaux existent ; accroupi + clic droit ouvre le coffre, clic
droit normal alterne lecture et pause ; recette bateau à coffre + jukebox.

Choix de Claude, signalés : recette dans KS_Crafts (comme les autres objets) ; seul un joueur monte à bord ; le
bateau à jukebox ne sert pas d'ingrédient et ne sort pas d'un distributeur ; n'importe qui peut reprendre le disque
d'un bateau ; le disque reste dans le bateau à la fin du morceau ; réglages d'affichage du jukebox dans `config.yml`.

## Reste à faire

- KS_JukeboxBoat 1.1.2 (jukebox de la taille exacte du coffre, 0,75 bloc, demande de LeKiwi06) : déployée sur Event le
  13/10 à 10:39. Reste : redémarrer Event, regarder en jeu les deux bateaux et le radeau de bambou.
- Fait le 13/10 à 10:34 : KS_Crafts 1.14.1 (recette en double corrigée) et KS_JukeboxBoat 1.1.1 (jukebox 30 % plus
  gros sur les deux bateaux, demande de LeKiwi06) sur Event. Reste : redémarrer, regarder la taille et la place du
  jukebox sur les deux bateaux.
- Fait le 13/10 à 08:21 sur « tu peux configurer grimAC, et déployer le nouveau jar » : KS_JukeboxBoat 1.1.0 et
  KS_Crafts 1.14.0 sur Event. Reste : redémarrer Event, tester, puis Kixster.
- GrimAC : fait le 13/10 à 08:25 sur Event, réglage global choisi par LeKiwi06 (plutôt qu'une exemption ciblée dans
  KS_AntiCheat, recommandée par Claude) : `Simulation.immediate-setback-threshold` et `Simulation.max-advantage` à -1.
  GrimAC d'Event ne corrige plus la vitesse ni le vol anormaux, il les signale seulement. À tester : monter dans un
  bateau tiré par un Happy Ghast. Kixster : GrimAC inchangé, à décider.
- Laisses figées de la capture : LeKiwi06 a refait l'essai, « c'est juste un bug visuel coté client ». Sans suite.
- Essai Bedrock du porte-armure invisible coiffé d'un jukebox (LeKiwi06) ; s'il se voit, remplacer le modèle 3D.
- Tests du bateau à coffre : recette, jukebox bien placé devant le coffre, personne ne monte, lecture / pause,
  enchaînement et boucle des disques, disque retiré en cours, laisse (tenir, accrocher à son bateau, reprendre),
  cassé (objet et contenu rendus).

- Fait : Event redémarré à 07:44, journal lu (les deux plugins activés sans erreur, 78 crafts).
- Après les tests : déployer KS_JukeboxBoat 1.0.0 et KS_Crafts 1.13.0 ensemble sur Kixster, avec l'accord de LeKiwi06.
- Tests en jeu : recette et livre de recettes ; pose, jukebox bien placé à l'arrière (seul et avec un conducteur,
  bateau et radeau) et qui tourne avec le bateau ; conduite normale ; menu à pied et à bord ; musique qui suit le
  bateau ; reprise du disque ; bateau cassé (objet et disque rendus) ; déconnexion à bord puis retour ; passage par un
  portail ; côté Bedrock : menu, touche de saut, musique.
