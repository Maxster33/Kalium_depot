# KS_LootBlocs - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - minerais, feuilles, verrue du Nether (25/09/2026)

- **Fer et or** (normaux et deepslate, pas l'or du Nether) : 2 à 5 minerais bruts comme le cuivre, puis bonus de
  Fortune vanilla des minerais. Toucher de soie : inchangé.
- **Autres minerais** (charbon, cuivre, lapis, redstone, diamant, émeraude et leurs versions deepslate, quartz du
  Nether) : 1 % de chance que le drop soit remplacé par 2 blocs du minerai cassé ; jamais avec Toucher de soie.
- **Feuilles** (cassées par un joueur **ou** dégradées) : chêne noir, chêne pâle et acacia : une pousse de plus avec
  la chance vanilla (≈ ×2) ; toutes les feuilles : pommes au taux vanilla (chêne et chêne noir les donnaient déjà) ;
  chaque pomme : 0,01 % pomme dorée enchantée, 1 % pomme dorée, sinon pomme. Cisailles / Toucher de soie : rien
  d'ajouté (comme en vanilla).
- **Verrue du Nether** : plus aucun drop de verrue, qu'elle soit cassée par un joueur, par l'eau, par un piston ou
  par une explosion.
- Potions (émeraude deepslate, chorus, verrue) : dans KS_LootPotions.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - Fragments de Spawner (29/09/2026)

Demande de Maxster33 : les spawners lâchent des Fragments de Spawner : 1 à chaque spawner + 5 % de chance d'en avoir un
2e.
- Spawner **naturel** cassé par un joueur (hors créatif, n'importe quel outil, cassage non annulé par une protection) :
  1 Fragment de Spawner (KS_ItemSimple), 2 avec 5 % de chance ; l'XP reste vanilla.
- Les spawners **posés par un joueur** (marqueur `ks_spawners:pose` de KS_Spawners) n'en lâchent pas : ils tombent
  eux-mêmes (sinon, fragments -> spawner -> fragments à l'infini).
- `softdepend` KS_ItemSimple ; `build.sh` : compiler KS_ItemSimple d'abord. Sans KS_ItemSimple activé : rien.

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (1.0.0 dans `_removed-ks_lootblocs-1.0.0/`). Statut : non testé en jeu.**

## 1.2.0 - fer et or bruts : vanilla x2 (01/10/2026)

Demande de Maxster33 : « Nous avions réglé de manière à ce qu'on en obtienne autant que du cuivre, mais il s'avère que
c'est trop. Nous allons alors reprendre les valeurs vanilla et faire multiplié par 2 ».
- Fer et or (normaux et deepslate, pas l'or du Nether) : **2** minerais bruts au lieu de 2 à 5 (vanilla : 1), puis
  bonus de Fortune vanilla (multiplicateur) : Fortune I : 2 ou 4 ; Fortune II : 2, 4 ou 6 ; Fortune III : 2, 4, 6 ou 8.
  Moyennes : 2 / 2,67 / 4 / 5,5 (avant : 3,5 / 4,67 / 7 / 9,6). Toucher de soie : inchangé.
- Le reste (autres minerais, feuilles, verrue, fragments de spawner) : inchangé.

**Compilé, non déployé. Statut : non testé en jeu.**
