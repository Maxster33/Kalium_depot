# SimpleClaimSystem sur Event (moteur de KS_Claim)

Plugin tiers : **SimpleClaimSystem 1.13.1** (Xyness, licence MIT, Modrinth). KS_Claim le pilote par son API ; ses menus
et ses commandes ne sont plus proposés aux joueurs. Cahier des charges : catégorie 3 « Claims » (LeKiwi06,
30/09/2026), copié dans `../CAHIER_DES_CHARGES.md` au déploiement.

## Fichiers à placer sur le serveur

| Fichier | Où |
|---|---|
| `SimpleClaimSystem-1.13.1.jar` (Modrinth, empreinte SHA-512 vérifiée) | `/plugins/` |
| `langs/fr_FR.yml` (ce dossier) | `/plugins/SimpleClaimSystem/langs/fr_FR.yml` |
| `config.yml` de SCS modifié (voir ci-dessous ; généré depuis celui du jar, jamais stocké dans le dépôt) | `/plugins/SimpleClaimSystem/config.yml`, **avant** le premier démarrage |

## Changements du `config.yml` de SCS (par rapport à celui du jar 1.13.1)

| Réglage | Valeur | Pourquoi (cahier) |
|---|---|---|
| `lang` | `fr_FR.yml` | Textes en français (SCS ajoute en anglais les clés non traduites, jamais vues par les joueurs) |
| `auto-purge` | `false` | Pas de suppression des claims inactifs |
| `enter-leave-messages`, `-title-`, `-chat-` | `false` | Entrée / sortie : barre de boss seulement (`bossbar: true` inchangé) |
| `check-for-updates`, `updates-notifications` | `false` | Pas de message de mise à jour aux opérateurs |
| `groups.default` | `max-claims: 100`, `max-chunks-per-claim: 1`, `max-chunks-total: 100`, `max-members: 6` (propriétaire + 5), `claim-cost: 0`, `claim-cost-multiplier: 0` | 100 claims d'un chunk, 5 membres ; le prix est calculé par KS_Claim |
| `status-settings` | `Fly: false`, `GuiTeleport: false` | Pas de vol dans les claims ; `/claim tp` remplacé plus tard par des homes |
| `default-values-settings` | `Fly: false` (membres), `GuiTeleport: false` (membres, visiteurs) | Idem |
| `blocked-interact-blocks` | coffres (dont coffres en cuivre), coffres piégés, tonneaux, 17 shulkers, entonnoirs, distributeurs, droppers, fours, fumoirs, hauts fourneaux, alambics, crafters, bibliothèques sculptées, pots décorés | Seuls les contenants sont protégés (tables, lits, panneaux, enclumes... libres) |
| `blocked-entities` | liste du jar sans `BOAT` ni `MINECART` | Bateaux et wagonnets libres |
| `claims-worlds-mode` | `world`, `world_nether`, `world_the_end` : `SURVIVAL` (inchangé) | Claims dans les 3 mondes |

Les réglages que les propriétaires peuvent changer (`status-settings`) et les valeurs de base des membres et visiteurs
(`default-values-settings`) se règlent ensuite dans ce fichier (LeKiwi06) ; KS_Claim n'affiche que les réglages
autorisés.

## Commandes à taper par l'humain (console d'Event)

Retirer les commandes de SCS aux joueurs (tout passe par `/ksclaims` et `/ksclaim`) :

```
lp group default permission set scs.command.claim false
lp group default permission set scs.command.claim.* false
lp group default permission set scs.command.claims false
lp group default permission set scs.command.unclaim false
```

Île du dragon (pas de claim à moins de 200 blocs de 0, 0 dans l'End ; SCS refuse les claims dans toute région
WorldGuard qui n'autorise pas `scs-claim`), en jeu dans l'End, avec WorldEdit :

```
//pos1 -200,0,-200
//pos2 199,255,199
/rg define ile_du_dragon
/rg flag ile_du_dragon scs-claim deny
```

À savoir : toute future région WorldGuard d'Event (dans n'importe quel monde) empêchera aussi les claims, sauf avec
`/rg flag <région> scs-claim allow`.
