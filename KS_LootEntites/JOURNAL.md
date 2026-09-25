# KS_LootEntites - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - loots des mobs (25/09/2026)

Réductions et remplacements : à toutes les morts (fermes comprises). Ajouts : seulement si un joueur tue ; Butin sans
effet sur les pourcentages.
- **Golem de fer** : lingots → même nombre de pépites ; coquelicots divisés par 2 (arrondi au hasard), chacun
  remplacé par une fleur d'un bloc de haut au hasard : 1 % wither rose, 1 % pitcher plant, 1 % torchflower, sinon une
  des 12 fleurs vanilla (pissenlit, coquelicot, orchidée, allium, houstonie, 4 tulipes, marguerite, bleuet, muguet).
- **Zombifié (pigmen)** : tous les loots ÷ 2 (arrondi au hasard). **Sorcière** : tous les loots ÷ 5.
- **Pillard** : plus de fiole sinistre (seuls les capitaines en lâchaient).
- **Calmar, calmar lumineux** : drops ×2. **Wither squelette** : plus de crâne (le Wither ne peut plus être invoqué
  avec des crânes de ce serveur, confirmé par Maxster33), charbon ×2, 10 % de wither rose.
- **Chair putréfiée** (tous les mobs qui en lâchent : zombies, noyés, momifiés, zombies villageois, zoglins, chevaux
  zombies, zombifiés) : chaque chair a 50 % de chance de devenir un os.
- **Endermite** : 50 % de chance d'un de ces 6 objets (chances égales) : améthyste bourgeonnante, carapace de
  shulker, potion de régénération basique, fiole d'expérience niveau 10, fruit de chorus, 8 perles de l'Ender.
- **Warden** : 10 % d'une fiole d'expérience de niveau 10, 20, 30, 40 ou 50 (au hasard).
- **Fiole d'expérience (niveau N)** : fiole vanilla nommée et marquée ; lancée, elle donne l'XP pour passer du niveau
  0 au niveau N (160 / 550 / 1 395 / 2 920 / 5 345 points).
- Potions des autres mobs : dans KS_LootPotions.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**
