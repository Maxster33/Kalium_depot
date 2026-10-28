# KS_Cuisine - journal

Serveur Kixster. Nouveaux aliments et pomme d'amour (étape 3 du chantier « atouts, alcools, aliments », demande de
LeKiwi06 du 28/10/2026 ; cahier local `Atouts_Alcools_Aliments.md`, partie 3.4, à copier ici au déploiement).
Dépend de KLM_Menu (textes) et de KS_Effets (effets custom).

## 1.0.0 - aliments, pomme d'amour, cookies (28/10/2026)

Valeurs de LeKiwi06 en cuisses, converties en points du jeu (1 cuisse = 2 points) :

| Aliment | Recette | Nourriture / saturation | Base (image) | Autre |
|---|---|---|---|---|
| Pomme d'amour | pomme + 8 sucres (anneau) | 4 / 2,4 (une pomme) | pomme, brillante | mobs et joueurs amoureux |
| Cookie à la baie sucrée | cookie + 3 baies sucrées | 6 / 1,2 | cookie | Apprivoiseur 3 min |
| Cookie à la baie lumineuse | cookie + 3 baies lumineuses | 2 / 0,4 | cookie | Lumière 3 min |
| Sandwich jambon-beurre | pain + côtelette cuite + seau de lait (le seau revient) | 15 / 20,8 | pain | - |
| Brunch | côtelette crue + œuf (les 3 couleurs) | 10 / 7 | côtelette cuite | - |
| Blé soufflé | blé au four (10 s) ou au fumoir (5 s) | 1 / 1 | kelp séché (image du blé) | se mange en 0,8 s |
| Bol de céréales | seau de lait + 8 blés soufflés (le seau revient) | 10 / 10 | tarte à la citrouille (image du ragoût de champignons) | remet à zéro le temps sans dormir ; n'enlève pas les effets |

Les ingrédients sont les objets du jeu sans marque (un cookie déjà parfumé ne sert pas de base). Les aliments du plugin
ne servent dans aucune autre recette (pomme d'amour en pomme d'or...) et sont refusés à l'alambic (sinon la pomme
d'amour ferait de la manzana). Le jeu plafonne la saturation au niveau de faim (sandwich).

**Pomme d'amour sur les mobs** : clic droit sur un mob qui ne se reproduit pas : hostiles compris ; ni les 4 boss
(Wither, Ender Dragon, Warden, grand gardien), ni les animaux qui ont déjà une nourriture de reproduction, ni les
villageois, ni un bébé ; ni (LeKiwi06, 28/10) le shulker (immobile), le perroquet, le marchand ambulant (pas de
marchands à l'infini) et le têtard (déjà un bébé). Il est amoureux 30 s (cœurs) ; deux amoureux de la même espèce à 8 blocs ou moins marchent
l'un vers l'autre, un bébé naît à leur contact, et les parents attendent 5 min (comme les animaux du jeu).
- Espèce qui a un bébé dans le jeu (zombie, piglin, cheval-squelette...) : le vrai bébé.
- Slime : un **petit slime** (le jeu en a déjà de plusieurs tailles ; LeKiwi06, 28/10). Le cube de magma reste un
  mini-mob.
- Sinon : **mini-mob** (taille 0,5, marqué `ks_cuisine:bebe`), qui grandit au bout de 20 min passées dans un chunk
  chargé. Sa tête : KS_Decapitator 1.4.0 (« Tête de bébé ... », même texture que l'adulte).
- Les bébés ne disparaissent pas loin des joueurs (comme un animal né d'un élevage).

**Pomme d'amour entre joueurs** : la manger rend « Amoureux » 30 s (effet de KS_Effets, dans /effects). Deux joueurs
amoureux qui se touchent : son de glissade sur un bloc de miel, un bébé villageois apparaît, l'effet s'arrête pour les
deux, et chacun est fatigué 5 min. Une pomme mangée pendant ce temps ne rend pas amoureux et la barre d'action le dit
(`amour.fatigue` dans `lang.yml`). Délais gardés en mémoire : remis à zéro au redémarrage.

**Apprivoiseur** (cookie à la baie sucrée, 3 min) : ocelot, chat, renard, lapin, poissons et têtard ne fuient plus ce
joueur. Leur fuite est un chemin calculé par le jeu : quand le joueur le plus proche (16 blocs) d'un de ces animaux est
apprivoiseur et que le chemin l'éloignerait de lui, le chemin est refusé.

**Lumière** (cookie à la baie lumineuse, 3 min) : un bloc de lumière de niveau 14 suit le joueur (à la hauteur de sa
tête, sinon de ses pieds ; seulement dans l'air et les sources d'eau). Le niveau 14 est réservé à ce cookie : au
chargement de chaque chunk et au démarrage, tout bloc de lumière de niveau 14 sans joueur lumineux est retiré (le jeu
dit tout de suite si un chunk en contient : seuls ceux-là sont parcourus).

**Apparence Bedrock** : geyser-bedrock 1.7.0 (blé soufflé, bol de céréales) ; les autres gardent l'image de leur base.

Pour les autres plugins : `objet(id)`, `ids()`, `idObjet(objet)`, `estBebe(mob)`.

Limites :
- Apprivoiseur : un animal dont le joueur le plus proche est apprivoiseur ne s'éloigne pas non plus de lui en se
  promenant ; s'il est plus près d'un autre joueur, il fuit celui-ci comme d'habitude.
- Mobs amoureux : ils vont l'un vers l'autre seulement si leur propre comportement le permet (un mob qui attaque un
  joueur le poursuit d'abord).
- Prix des nouvelles têtes : proposition en attente de validation (`sortie/bareme/tetes-bebes-proposition.xlsx`).
- `/kaliumgive` et didacticiel : à faire au déploiement.

Compilé le 28/10/2026 (`sortie/KS_Cuisine-1.0.0.jar`, après KLM_Menu et KS_Effets). **Statut : non déployé, non testé
en jeu.**
