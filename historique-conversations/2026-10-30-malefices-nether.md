# 30/10/2026 — Maléfices du Nether

- Plugin(s) concerné(s) : KS_MaleficesNether (nouveau), KS_Jetons, KS_Atouts, KS_KaliumGive, KS_Textures,
  KS_Didacticiel (didacticiel.yml), geyser-bedrock, butin des récompenses (KG_Rewards, KV_Rewards, KS_RewardsGUI)
- Versions avant / après : KS_MaleficesNether — / 1.0.0 ; KS_Jetons 2.2.0 / 2.3.0 ; KS_Atouts 1.1.0 / 1.2.0 ;
  KS_KaliumGive 1.12.0 / 1.13.0 ; KS_Textures 1.1.0 / 1.2.0 ; geyser-bedrock 1.9.0 / 1.10.0 ; pack Java 1.0.0 / 1.1.0

## Demandé

« Augmenter la difficulté du Nether, en appliquant soit des maléfices aux joueurs, soit en boostant les mobs. » Les
effets de potion sur les mobs sont écartés (une cinquantaine de mobs avec effets font crasher Bedrock). Retenu :
des maléfices selon le temps passé d'affilée dans le Nether, « au jour 20 ça devient intenable et il faut sortir »,
un atout (plastron anti-radiation) et une « amulette mangeuse de mal » qui consomme l'XP du joueur.

## Fait

- Cahier local `Malefices_Nether.md` (validé le 30/10), copié dans `KS_MaleficesNether/CAHIER_DES_CHARGES.md`.
- KS_MaleficesNether 1.0.0 : compteur (jour = 20 min réelles, descente 1 jour / 10 min dehors, mort = zéro),
  10 effets puis montées, N minutes au hasard par effet et par jour, barre de boss rouge, /effects, amulette.
- KS_Jetons 2.3.0 (rangée « équipements spéciaux »), KS_Atouts 1.2.0 (plastron anti-radiation), KS_KaliumGive 1.13.0
  (id de l'amulette), KS_Textures 1.2.0 (texture et rareté), geyser-bedrock 1.10.0, pack Java 1.1.0, texture de
  l'amulette (premier jet de Claude), didacticiel, butin régénéré avec les prix actuels.
- Commits f5073b6, be38474, 8f20258, b225a3e, puis le commit de déploiement. Déployé le 30/10 à 18:56 (Kixster),
  18:57 (proxy, Kanvas).

## Décisions

- Maléfices sur le joueur plutôt que mobs renforcés (contrainte Bedrock) ; fermes AFK du Nether visées exprès.
- Ordre des effets (LeKiwi06) : faim, faiblesse, fatigue de minage, poison, lenteur, obscurité, infestation, wither,
  lévitation, nausée ; obscurité, lévitation et nausée en 10 s + 10 s ; montées à partir du jour 11.
- Amulette : 5 points d'XP par seconde et par niveau (10 au départ) ; valeur 7 500 (moins qu'une balise, voulu) ;
  2 % des tirages Légendaires. Plastron : durées ÷ 2, 20 min par craft dans le Nether, apparence plastron en or.
- Mort = remise à zéro (ne pas perdre son équipement « si bêtement »).
- KS_Economy non touché (prix de l'amulette dans KS_Textures et pools.py).

## Reste à faire

- Redémarrer Kixster, Kanvas et le proxy (ranger `KaLium-objets-1.9.0.mcpack` pendant l'arrêt du proxy).
- Retoucher la texture de l'amulette si besoin (puis refaire les deux packs).
- Le Nether est fermé sur Kixster : les maléfices n'agiront qu'à son ouverture ; mettre alors à jour l'entrée
  « Nether et End fermés » du didacticiel.
- Tests en jeu (astuce : `jour-secondes: 60` dans le config.yml de KS_MaleficesNether pour aller vite).
