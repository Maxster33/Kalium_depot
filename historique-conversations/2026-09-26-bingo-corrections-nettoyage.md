# 2026-09-26 — Corrections du Bingo, nettoyage des serveurs

- Plugin(s) concerné(s) : KG_BingoGame (Serveur Jeux) ; dossiers `_removed-…` de tous les serveurs.
- Versions avant / après : KG_BingoGame 0.8.0 → 0.8.2.
- Suite de `2026-09-26-kanvas-parkour-equilibrage.md` (même nuit).

## Demandé
- « Le bonus pour avoir un item en 1er est la moitié des points de base de l'item. »
- Partie à 4 équipes buggée : barre d'action trop chargée (« fais-en 2 lignes ») ; « des gens ne sont pas TP ».
- Icônes de la grille : piston collant / piston, bloc musical / juke-box, bois, lits (seulement l'oreiller), enclume,
  longue-vue méconnaissables ; « vérifie si d'autres exemples existent » ; objets du Nether « rangés difficiles pour
  rien ». Puis : blocs « compressés », « on ne peut pas les détailler un peu plus ? », items « un peu petits à côté ».
- Nouvelle règle : supprimer les `_removed-…` à 3 versions ou plus de la version en service ; puis les anciens
  plugins plus en service.
- « Stocke ça dans à faire plus tard, et clôture la session. »

## Fait
- KG_BingoGame 0.8.1 puis 0.8.2 (déployées sur Serveur Jeux ; 0.8.2 non testée) : bonus du 1er 1 / 3 / 5 / 10 ;
  reconnexion d'un joueur absent au lancement (envoyé sur sa map) ; joueurs sans équipe placés dans l'équipe la moins
  remplie ; barre d'action courte à partir de 3 équipes (« ⏱ 45m12s | A:42pts·1/3 | ... ») ; icônes : rendu 3D des
  blocs (`ModelRenderer`), longue-vue, lit entier, pomme enchantée, tout en 22 pixels ; 7 objets du Nether en Normal.
- 200 icônes vérifiées hors serveur (client officiel 26.2 téléchargé avec accord, empreinte vérifiée).
- `REGLES.md` : règle des 3 versions ; scripts WinSCP préparés, lancés par LeKiwi06 : 85 dossiers supprimés.

## Décisions
- La barre d'action n'a qu'une ligne : format court choisi par LeKiwi06 (à partir de 3 équipes).
- Joueurs sans équipe : placés dans une équipe existante (pas de nouvelle équipe : maps déjà préparées).
- Icônes : rendu 3D des blocs (le jeu n'a pas d'image plate pour eux), dessus un peu moins haut que la vue du jeu,
  tout en 22 pixels.
- Suppressions : faites par l'humain (Claude ne supprime jamais de fichier) ; gardés : plugins retirés de Kanvas le
  26/09, dossiers de l'Event (Maxster33), grosses sauvegardes datées.

## Ajout en fin de session
- « Des parties ne se sont pas supprimées de l'interface du Bingo sur Kal-Games » : KG_Bingo 1.5.1 (partie listée plus
  de 30 min retirée ; cause : redémarrages de Serveur Jeux pendant des salles d'attente), déployée à 6 h 56.

## Reste à faire
Voir « À faire plus tard » dans le compte rendu du 2026-09-26 de `REPRISE_PROJET.md`.
