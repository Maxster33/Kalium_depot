# KS_Spawners - journal

Plugin autonome, serveur Event. Spawners fabriqués avec leur créature (demande de Maxster33, 29/09/2026).

## 1.0.0 - spawners avec créature (29/09/2026)

Demande de Maxster33 : spawners craftables (KS_Crafts 1.6.0) avec la créature déjà dedans, empilables par 64 ; une fois
posés, mêmes règles que les spawners vanilla, sauf qu'ils tombent au sol quand on les casse (pour être déplacés).
Choix de Maxster33 : nouveau plugin (le fonctionnement n'est pas qu'un craft) ; n'importe quel outil.

| Id | Objet | Créature |
|---|---|---|
| `zombi` | Spawner à zombi | zombie |
| `squelette` | Spawner à squelette | squelette |
| `araignee` | Spawner à araignée | araignée |
| `creeper` | Spawner à creeper | creeper |
| `blaze` | Spawner à blaze | blaze |
| `mouton` | Spawner à mouton | mouton |
| `vache` | Spawner à vache | vache |
| `poule` | Spawner à poule | poule |

- **Objet** : spawner nommé, marqué (`ks_spawners:creature`), empilable par 64. `creerSpawner(id)` (KS_Crafts,
  KS_KaliumGive 1.5.0).
- **Pose** : le jeu n'applique la créature d'un objet spawner que pour un opérateur ; le plugin la règle lui-même et
  marque le bloc (`ks_spawners:pose`). Ensuite : spawner vanilla (lumière, distance du joueur, etc.).
- **Cassage** (joueur, n'importe quel outil, protections respectées : seulement si le cassage n'est pas annulé) : le
  spawner posé tombe tel quel, **sans XP** (sinon poser / casser produirait de l'XP à l'infini) ; en créatif, rien ne
  tombe (comme en vanilla). Si un opérateur a changé la créature avec un oeuf, c'est la nouvelle créature qui tombe
  (si elle fait partie des 8).
- **Explosion** (demande de Maxster33) : un spawner posé détruit par une explosion (TNT, creeper, boule de feu...) n'est
  pas perdu, il tombe au sol (toujours, sans XP). Un spawner naturel reste vanilla (détruit sans rien lâcher).
- Spawners naturels : comportement vanilla, aucun objet ; KS_LootBlocs 1.1.0 y ajoute les Fragments de Spawner.

Limites :
- Un spawner posé cassé par le corps du Wither (pas une explosion) est perdu, comme un spawner vanilla.

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (apparence Bedrock : geyser-bedrock 1.2.0 sur le proxy à 17:23), actifs après redémarrage d'Event et du proxy. Statut : non testé en jeu.**
