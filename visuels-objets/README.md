# visuels-objets - textures 16×16 des objets custom

Textures de **60 objets custom** de KaLium dans le style vanilla. Version du 28/10/2026, à vérifier et retoucher par
LeKiwi06.

Elles partent des textures du jeu Java installé sur le PC (bouteille de potion, wagonnet, bateau, radeau, boussole,
torche, bloc d'émeraude…), recolorées avec les teintes du jeu et complétées. Les objets sans équivalent vanilla
(jetons, badges, verrou) sont dessinés à la main avec les couleurs exactes des lingots et des
planches. Comme pour `geyser-bedrock`, les PNG contiennent des pixels de Mojang : **`png/` est hors du dépôt**
(`.gitignore`) ; seuls le script et l'aperçu sont suivis.

- `png/<id>.png` : une texture 16×16 par objet. **C'est le PNG qui fait foi** : on le retouche dans un éditeur de
  pixel art (Aseprite, Piskel, LibreSprite, Paint.NET…). Garder une copie des retouches : le dossier n'est pas dans git.
- `apercu.html` : planche de tous les objets (nom, plugin, apparence actuelle), à ouvrir dans un navigateur ; elle
  relit les PNG, il suffit de la recharger après une retouche.
- `dessiner.py` : `python visuels-objets/dessiner.py [--jar <jeu.jar>]` (par défaut : version 26.2 du launcher
  officiel). Il ne crée que les PNG manquants et refait `apercu.html` ; `--tout` redessine tout et **écrase les
  retouches**.

### Tout retoucher sur une seule image

```
python visuels-objets/dessiner.py --planche
```

Il assemble tous les PNG à leur taille réelle dans `planche.png` (160×96, 10 objets par ligne, sans marge ; l'ordre
est dans `planche.txt`). On l'ouvre dans l'éditeur en affichant une grille de 16×16 : chaque case est un objet. Une
fois les retouches enregistrées :

```
python visuels-objets/dessiner.py --decouper
```

Il redécoupe la planche et ne réécrit que les PNG modifiés (il affiche lesquels). Relancer `--planche` écrase la
planche : redécouper d'abord. Ne pas déplacer les cases ni redimensionner l'image. La planche est hors du dépôt, comme
`png/`.

## Objets couverts

| Plugin | Objets | Base vanilla |
|---|---|---|
| Déjà dans geyser-bedrock (18) | Estomac du gardien, Clé de l'End, Bedrock Breaker, Fragment et Cœur de Spawner, Changeur de Biome, 5 jetons, 4 badges, Cristal de purification, Casse-croûte doré, Bandage | sac, clé sinistre, pioche, fragment de disque, cœur de la mer, éclat d'améthyste, pain, papier ; jetons et badges dessinés |
| KS_Alcools (9) | 7 alcools (la version jetable garde la même image), Cocktail Molotov, Torche augmentée | potion (hydromel : bouteille de miel), potion jetable, torche |
| KS_Elixir (11) | les 11 élixirs | potion au bouchon d'or, couleur propre |
| KS_Trains, KS_VerrouVehicule (4) | Locomotive, Wagon contrôleur, Boussole du train, Verrou de véhicule | wagonnet à four, wagonnet, boussole ; verrou dessiné |
| KS_Cuisine (6) | Cookie à la baie sucrée, Cookie à la baie lumineuse, Sandwich jambon-beurre, Brunch, Blé soufflé, Bol de céréales | cookie (pépites en baies), pain (jambon et beurre), côtelette cuite (œuf au plat), blé, bol de ragoût (lait et céréales) |
| KS_Economy, KS_Crafts (7) | Bloc d'émeraude compressé tiers 1 à 6, Bloc de charbon de bois | bloc d'émeraude, bloc de charbon |
| KS_FioleExp, KS_CoffreMort (2) | les deux fioles d'expérience | fiole d'expérience (bleue, violette) |
| KS_JukeboxBoat, KS_Trains (3) | Bateau à jukebox, Radeau à jukebox, Wagon jukebox | dessinés par LeKiwi06 (bateau, radeau, wagonnet) |

Sans texture propre (choix de LeKiwi06, 29/10/2026) : le coffre renforcé, le bateau, le radeau et le minecart avec
coffre renforcé, et la pomme d'amour gardent l'image vanilla avec l'aspect enchanté (KS_CoffreRenforce 1.3.0 ;
KS_Cuisine l'avait déjà). Le bateau et le radeau à coffre et jukebox prendront l'image du bateau et du radeau à
jukebox, avec l'aspect enchanté (KS_JukeboxBoat 1.2.0). Leurs anciens PNG sont gardés dans `png/_retires/`. Pas de
texture pour les têtes (KS_Decapitator : skin du joueur ou du mob) ni pour les spawners (KS_Spawners : le mob
s'y voit déjà).

## Pas encore en jeu

Ces images ne sont branchées nulle part. Pour qu'elles s'affichent :

- **Java** : il faut un pack de ressources serveur (il n'y en a pas aujourd'hui) avec un modèle `kalium:<id>` par
  objet, et chaque plugin doit passer à `item_model` = `kalium:<id>` (au lieu de l'image vanilla actuelle).
- **Bedrock** : `geyser-bedrock/build.sh` prendrait ces PNG à la place des images extraites du jeu, et chaque objet
  doit avoir sa ligne dans `kalium_objets.json` (aujourd'hui, seuls les 18 premiers l'ont).
