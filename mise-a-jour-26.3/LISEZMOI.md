# Passage en 26.3 : proxy, lobby, Kanvas, Serveur Jeux, Kal-Games

Préparé le 08/10/2026 par Maxster33 (avec Claude). **Envoyé le 08/10/2026 entre 16:20 et 17:23 sur les 5 serveurs, tous démarrés en 26.3 sans erreur nouvelle** (détail : `REPRISE_PROJET.md`). Texte d origine : à lancer serveur par serveur, après
le feu vert de Maxster33. Kixster et Event sont déjà en Paper 26.3-159 (07/10/2026) ; ce paquet reprend les mêmes
versions.

Pourquoi : les joueurs en Minecraft 26.3 sont refusés par les serveurs en 26.2 (vu dans le journal du proxy le
08/10/2026 à 14:19 : « Outdated server! I'm still on 26.2 » en allant au lobby).

## Ce qui a été vérifié

- **Nos plugins** : les 22 plugins de ces serveurs (KLM_Menu, KLM_Chat, KLM_Portal, KLM_Hub, KaliumCore, KalGames,
  KG_*, KV_*) compilent contre `paper-api 26.3.build.159-beta` **sans aucune erreur** (08/10/2026). Comme pour
  Event, **les jars en service ne changent pas**.
- **Paper** : 26.3-159 (06/10/2026) est toujours le dernier build ; même jar que Kixster et Event.
- **Plugins externes** : versions validées sur Event / Kixster en 26.3, ou dernière version marquée 26.3 sur
  Modrinth. `telecharger.sh` les télécharge dans `telechargements/` (hors du dépôt) et vérifie leur empreinte.
- **Geyser ne parle pas encore la 26.3** (dernier build : 2.11.3-b1249, Java 26.2) : **ViaVersion + ViaBackwards
  sur chaque serveur** (sinon joueurs Bedrock et clients 26.2 refusés), comme sur Kixster et Event.

## Changements par serveur

| Serveur | Remplacés (ancien → nouveau) | Ajoutés | Rangés | Facultatifs (`OPTIONS=1`) |
|---|---|---|---|---|
| **Proxy** | — (Velocity 4.2.0-30 en service depuis le 07/10 : gère déjà la 26.3) | — | — | Geyser-Velocity → 2.11.3-b1249 (06/10, gère Bedrock 26.52) ; ranger `geyserupdater-spigot.jar` (plugin Spigot, en erreur à chaque démarrage, sans effet) |
| **Lobby** | WorldEdit 7.4.5 → 7.4.6-beta-02 ; WorldGuard 7.0.18 → 7.0.19 ; voicechat 2.6.23 → 2.6.24 | ViaVersion, ViaBackwards 5.12.1-SNAPSHOT | — | ranger Geyser-Spigot (inutile derrière le proxy, pas encore en 26.3) |
| **Kanvas** | **déjà fait le 08/10/2026 vers 08:47** (FastAsyncWorldEdit 2.16.0, ViaVersion / ViaBackwards 5.12.1-SNAPSHOT, voicechat 2.6.24, `paper-26.3-159.jar` à la racine) | — | — | — |
| **Serveur Jeux** | ViaVersion 5.12.0-SNAPSHOT → 5.12.1-SNAPSHOT ; WorldEdit 7.4.6-beta-01 → beta-02 | ViaBackwards 5.12.1-SNAPSHOT | **LegacyFreecam 2.0.0** (plante en 26.3 : CommandAPI sans 26.3, vu sur Event) | ranger Skript 2.16.2 (aucun script, pas de 26.3), TradeShop 1.7 (pas de 26.3), WoodCutter 1.0.2, Geyser-Spigot : restes de la copie de Kixster du 24/09 |
| **Kal-Games** | ViaVersion et ViaBackwards 5.12.0 → 5.12.1-SNAPSHOT ; WorldEdit 7.4.5 → 7.4.6-beta-02 ; voicechat 2.6.23 → 2.6.24 ; **GrimAC 8eb5f28 → abb95b6** (8eb5f28 plante en 26.3, vu sur Event) ; ConditionalEvents 4.79.2 → 4.80.3 ; PlayerKits2 1.23.3 → 1.24.1 | — | — | — |

Gardés tels quels (déjà en 26.3 sur Event / Kixster, ou marqués 26.3) : LuckPerms 5.5.71, floodgate 2.2.5 (b141,
dernier build), PlaceholderAPI 2.12.3, WorldGuard 7.0.19, JEIRecipeFix 0.4.0, RideOnHead 1.1.3, FairMinimap,
SimpleClaimSystem 1.13.1, BedrockSkinRestorer (même fichier, marqué 26.3), PyxelRegions 1.3.0 (testé 26.3 selon
Spigot). **VelocityCommandForward-Paper 1.1.0** (lobby, Serveur Jeux, Kal-Games) : pas de version marquée 26.3
(dernière : 26.2) ; petit plugin, gardé ; à surveiller dans le journal.

Rien n'est supprimé : les anciens jars vont dans `/plugins/_removed-avant-26.3/` de chaque serveur.

## Décisions à prendre avant (Maxster33)

1. **Facultatifs** (colonne de droite) : les inclure (`OPTIONS=1`) ou non. Conseillé : oui partout.
2. **Kanvas** : préparé ce matin par quelqu'un d'autre (pas dans le dépôt) ; à confirmer avec LeKiwi06 avant de
   changer son jar de démarrage.

## Ordre conseillé et procédure

Ordre : **proxy, lobby, Kanvas, Serveur Jeux, Kal-Games** (un serveur à la fois ; le lobby tôt, car tout le monde y
arrive). Le redémarrage du proxy déconnecte tout le réseau.

Pour **chaque** serveur :

1. (déjà fait le 08/10/2026 : `sh envoyer.sh <serveur> sauvegarde`, copie sur le PC dans
   `Documents\Sauvegardes_KaLium\<serveur>-avant-26.3`, sans `libraries/`, `cache/`, `versions/`, `logs/`,
   `_removed-*/`.)
2. **Arrêter** le serveur (panneau Minestrator).
3. Envoi (Git Bash, dans ce dossier) :
   ```sh
   DRY_RUN=1 OPTIONS=1 sh envoyer.sh <serveur>   # à blanc : montre ce qui va changer
   OPTIONS=1 sh envoyer.sh <serveur>             # sauvegarde mise à jour (serveur arrêté), puis envoi
   ```
   `<serveur>` : `proxy`, `lobby`, `kanvas`, `serveurjeux`, `kalgames`. Sans `OPTIONS=1` : sans les facultatifs.
4. **Panneau Minestrator** (serveurs Paper) : Paramètres > Hébergement > « Changer le paramètre de démarrage » →
   `paper-26.3-159.jar` (Paper 26.3 n'est pas dans l'onglet « Versions »). Si le panneau pose un `spigot-26.3.jar`,
   ne pas le lancer.
5. **Démarrer** ; Claude lit le journal : « Loading Paper 26.3-159 », tous les plugins activés, aucune erreur.
6. Test rapide en jeu : connexion Java 26.3, Java 26.2 et Bedrock ; changer de serveur par la boussole.

**Proxy** : rien à envoyer sans `OPTIONS=1` ; un simple redémarrage active aussi **LibertyBans 1.2.0-M1** (posé le
07/10, pas encore vérifié : à lire dans le journal).

## Retour en arrière

Un monde ouvert en 26.3 **ne peut plus être ouvert en 26.2**. Si un serveur ne marche pas en 26.3 :
1. l'arrêter ; 2. remettre les anciens jars de `/plugins/_removed-avant-26.3/` (et ranger les nouveaux) ;
3. remettre les mondes depuis la sauvegarde du PC ; 4. choisir `paper-26.2-121.jar` (encore à la racine) dans le
panneau. Claude peut le faire par WinSCP.

## Correctif du 08/10/2026 : ViaVersion / ViaBackwards 5.12.1 stables

Les joueurs Bedrock (que Geyser fait entrer en 26.2) voyaient une interface de craft fausse : ViaBackwards
5.12.1-SNAPSHOT+634 traduit mal le livre de recettes 26.3 → 26.2 (corrigé dans la 5.12.1 stable du 08/10/2026).
`envoyer.sh` remplace maintenant les deux jars SNAPSHOT par les 5.12.1 stables sur chaque serveur Paper, Kixster et
Event compris (`sh envoyer.sh kixster`, `sh envoyer.sh event` : ces deux jars seulement, sans sauvegarde). Les
autres lignes du tableau ci-dessus sont déjà faites : le script les annonce « deja fait ».

```sh
DRY_RUN=1 sh envoyer.sh <serveur>             # à blanc
SANS_SAUVEGARDE=1 sh envoyer.sh <serveur>     # jars seulement, serveur allumé possible ; actif au redémarrage
```

Sur un autre PC que celui de Maxster33 : `WINSCP=<chemin de WinSCP.com>` et `SESSION_WINSCP="<nom de la session>"`.
État : Kixster envoyé le 08/10/2026 à 23:42, actif depuis son redémarrage du 09/10 à 00:12, craft Bedrock confirmé par
LeKiwi06 ; Event, lobby, Kanvas, Serveur Jeux et Kal-Games envoyés le 09/10/2026 vers 00:40 (actifs au prochain
redémarrage de chacun).

## Après la mise à jour

- Mettre à jour le tableau « Versions en service » de `REPRISE_PROJET.md` (Paper 26.3-159 partout).
- Puis les tests de Kixster : `KX_Monde/deploiement-kixster/TESTS_EN_JEU.md`.
