# 2026-10-02 — Textes plus courts dans les menus

- Plugin(s) concerné(s) : KalGames, KG_BoatRace, KG_Parkour, KG_PvpKit, KG_Bingo (Kal-Games), KLM_Portal (lobby, Kanvas)
- Versions avant / après : KalGames 1.22.1 → 1.22.2, KG_BoatRace 1.4.1 → 1.4.2, KG_Parkour 1.1.0 → 1.1.1, KG_PvpKit
  1.0.0 → 1.0.1, KG_Bingo 1.6.0 → 1.6.1, KLM_Portal 1.3.1 → 1.3.2

## Demandé
LeKiwi06 : « dans les réglages des jeux pour les opérateurs c'est toujours pas ça, écrit en texte au-dessus des boutons
ce que c'est et la jauge ne sert qu'à régler ça, pas besoin de les rendre super longues » ; puis (texte entre les
jauges impossible dans une fenêtre Minecraft) « écrit des textes plus courts dans les champs de jauge et boutons
alors » ; « tu peux déployer ».

## Fait
- Réglages des jeux : nom seul dans chaque jauge, explications en texte en haut du menu ; 20 noms raccourcis.
- Listes de création du Bingo et du PvP Kit, point de chute de KLM_Portal : options courtes, explications en haut.
- Tous les textes de boutons et de champs mesurés : chacun tient dans sa largeur normale (260 / 240 pixels).
- Déployé le 02/10/2026 à 17:49.

## Décisions
- 6 plugins réservés en même temps : accord de Maxster33 (transmis par LeKiwi06).
- Nouvelles clés de langue pour les textes déjà déployés (les `lang.yml` des serveurs gardent les anciens).

## Suite : catégorie 3 « Claims »
- SimpleClaimSystem 1.13.1 téléchargé (accord de LeKiwi06), API et code source lus ; KS_Claim 1.0.0 (interface des
  claims), KS_BiomeChanger 1.1.0, traduction française et configuration de SCS ; déployés sur Event à 22:26.
- Choix de Claude signalés : achat en se tenant dans le claim avec /ksclaim ; l'acheteur garde les membres (SCS) ; un
  claim acheté n'entre pas dans les prix remboursables (sinon création de points).

## Suite : catégorie 2, magasins
- TradeShop original sans mise à jour depuis 2023 : LeKiwi06 choisit nos propres boutiques. Région `zone_shop` (claims et
  construction libres), `/magasin create` (45 000 points ou 5 blocs tier 3), `/magasin agrandir` (10 000, +1 boutique),
  création au panneau, prix en objet ou en points en attente, achat avec nombre de lots. Partie « Magasins » du cahier
  réécrite et validée par LeKiwi06.
- KS_Economy 1.1.0 et KS_Claim 1.1.0 déployés sur Event le 03/10/2026 à 00:02.

## Reste à faire
- Région `zone_shop` (humain) ; tests des magasins avec le speedrun.
- Claims : redémarrer Event, commandes LuckPerms, région de l'île du dragon, tests (compte rendu du 02/10/2026).
- Redémarrer Kal-Games, le lobby et Kanvas ; tester les menus (liste dans le compte rendu du 02/10/2026).
