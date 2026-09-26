# 2026-09-26 — Kanvas (plots en créatif), sortie du Parcours, équilibrage des barèmes

- Plugin(s) concerné(s) : KV_Plots, KV_Menu (nouveaux), KG_Parkour (nouveau), KalGames, KG_BingoGame, KG_Bingo ;
  serveurs Kanvas, Kal-Games, Serveur Jeux.
- Versions avant / après : KV_Plots — → 1.4.0 ; KV_Menu — → 1.3.0 ; KG_Parkour — → 1.0.0 ; KalGames 1.19.1 → 1.20.0 ;
  KG_BingoGame 0.7.8 → 0.8.0 ; KG_Bingo 1.4.0 → 1.5.0.

## Demandé
- « Reprendre là où on en était pour le cahier des charges de Kanvas », puis le coder : grille de plots, réservation,
  protection, puis KV_Menu (« une interface au lieu de juste avoir les commandes »), étoile du Nether en case 4.
- Générer une zone de 5 plots dans chaque direction à partir du plot de référence (-107 -2 -57).
- Reset / suppression de plot ; validation et votes ; étoile et boussole hors de la barre d'objets sur son plot en
  travaux ; titre / description ; menu des visites ; tableau sur le côté à la place du titre à l'écran ;
  signalements ; concours de build (participer, fin dans, participants, annuler sa participation, thème dans le
  tableau, interface admin).
- FAWE ne marchait pas ; « quand je passe d'opérateur à joueur ça me met en survie » ; tri des plugins de Kanvas.
- « Isoler KG_Parkour du reste en suivant le cahier des charges ».
- Équilibrer les barèmes : « 30 minutes à try hard le boat race = 30 minutes le parkour = le bingo… ».

## Fait
- Kanvas : cahier des charges complété ; KV_Plots / KV_Menu codés, déployés et testés par LeKiwi06 à chaque étape ;
  monde renommé `Kanvas` (par LeKiwi06) ; grille générée ; FAWE Paper à la place de la variante Bukkit, limites,
  permissions LuckPerms, baguette de navigation déplacée ; plugins inutiles rangés dans `_removed-…`.
- KG_Parkour 1.0.0 + KalGames 1.20.0 déployés sur Kal-Games (non testés).
- Mesures des rythmes de points (journal de KG_ScoreBoards, journaux de Serveur Jeux) : `EQUILIBRAGE_POINTS.md` ;
  KG_BingoGame 0.8.0 + KG_Bingo 1.5.0 déployés (non testés).

## Décisions
- Kanvas : réservation « là où l'on se tient, sinon au plus près du centre » ; grand plot libéré = redevient 4 moyens ;
  agrandissement : copie au centre, ancien plot remis à zéro ; plot validé : « dupliquer en version grande » ;
  signalements dans un fichier, consultés dans KV_Menu ; plots validés figés ; concours : votes des joueurs après la
  fin, plots gardés à part, taille choisie par le staff, plots visitables pendant le concours (défauts de Claude : un
  concours à la fois, hors places et déblocages, éditeurs autorisés, classement points puis moyenne).
- Créatif gardé sur Kanvas par KV_Plots (KLM_Menu, commun au réseau, non modifié).
- Parcours sorti tel quel (étape 0), comme la course de bateau.
- Équilibrage : retoucher chaque barème ; référence = course de bateau actuelle ; recalculer le passé. Bingo : barème
  doublé, points branchés sur les classements.
- Dossiers `_removed-kalgames-…` anciens : pas d'archive sur GitHub (configs possibles dans ces dossiers, dépôt
  public) ; suppression par LeKiwi06 (Claude ne supprime jamais).
- Sauvegardes des mondes : à voir plus tard.

## Reste à faire
- Tester KG_Parkour, KalGames 1.20.0, le Bingo (points doublés, classement « Bingo »).
- Équilibrage : Parcours (nouveau barème + mesures), PvP Kit, Rush, recalcul du passé.
- Kanvas : limites d'entités / mobs sans IA, agrandissement, KV_ScoreBoards, extension du monde.
- Sauvegardes des mondes ; suppression des `_removed-…` en trop (humain).
