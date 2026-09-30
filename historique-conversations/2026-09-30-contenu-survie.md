# 2026-09-30 — Catégorie 1 « Contenu survie » : têtes de mobs et élixirs

- Plugin(s) concerné(s) : KS_Decapitator (nouveau), KS_Elixir (nouveau), KS_Crafts, KS_ItemSimple, KS_KaliumGive,
  KS_LootEntites (serveur Event)
- Versions avant / après : KS_Crafts 1.6.0 → 1.7.0, KS_ItemSimple 1.0.0 → 1.1.0, KS_KaliumGive 1.5.0 → 1.6.0,
  KS_LootEntites 1.2.1 → 1.3.0 ; KS_Decapitator 1.0.0, KS_Elixir 1.0.0

## Demandé
LeKiwi06 : « on attaque le code de la catégorie 1 (Contenu survie) », d'après le cahier des charges validé le
29/09/2026 ; puis « tu peux déployer et m'annoncer les tests à faire ».

## Fait
- KS_Decapitator 1.0.0 : 332 têtes (1 % quand un joueur tue le mob, pas pour les mobs de spawner), une par variante,
  état et bébé ; posées puis cassées, elles redonnent le même objet ; `/tetes` (ops) pour les vérifier.
- KS_Elixir 1.0.0 : 11 élixirs en anneau, livre de recettes, élixirs bloqués à l'alambic ; Fortune en attente de
  KS_Economy.
- KS_Crafts 1.7.0 (spawners avec les têtes de KS_Decapitator), KS_ItemSimple 1.1.0 (têtes « Steve » retirées),
  KS_KaliumGive 1.6.0 (`elixir_…`, `tete_…`), KS_LootEntites 1.3.0 (Warden : fiole de KS_FioleExp).
- Déployés sur Event le 30/09/2026 à 18:07 (WinSCP en ligne de commande, session enregistrée « Event [new] ») ; actifs
  après redémarrage d'Event par l'humain. Cahiers publiés dans `KS_Decapitator/` et `KS_Elixir/`.
- `outils-build/` mis à jour (`telecharger-outils.sh` : Floodgate et Cumulus manquaient pour compiler KS_FioleExp).

## Décisions
- Textures : minecraft-heads.com bloque les robots ; MoreMobHeads (licence MIT) retenu ; AllMobHeads écarté (licence
  « tous droits réservés »). Textures manquantes (16 shulkers colorés, cube de soufre, bébé noyé) à fournir.
- Une tête par état temporaire aussi (loup / abeille en colère, abeille pollinisée, arpenteur frigorifié, chèvre
  hurleuse, vex en charge, creeper chargé) : choix de LeKiwi06.
- Menu `/tetes` pour les opérateurs : accepté.
- Élixirs interdits dans l'alambic (sinon élixirs jetables / persistants) : choix de LeKiwi06.
- Choix de Claude signalés : lapin « Toast » et mouton « jeb_ » ont leur tête ; cheval : une tête par robe (pas par
  marquage) ; villageois : textures de la plaine ; poisson tropical : tête générique.

## Reste à faire
- Redémarrer Event (l'humain), puis les tests en jeu (liste dans le compte rendu de `REPRISE_PROJET.md`).
- Fournir les textures manquantes ; voir l'affichage des têtes sur Bedrock (Geyser `custom-skulls` si besoin).
- Recette de l'Élixir de Fortune quand KS_Economy existera (catégorie 2).
- Supprimables par l'humain sur Event : `_removed-ks_crafts-1.0.0/` à `1.4.0/`, `_removed-ks_kaliumgive-1.0.0/` à
  `1.3.0/`, `_removed-ks_lootentites-1.0.0/` et `1.1.0/`.
