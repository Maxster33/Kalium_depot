# Déploiement des plugins d'Event sur Kixster (étape 4)

Préparé le 07/10/2026 par Maxster33 (avec Claude) ; à lancer **quand la prégénération de Kixster est terminée**
(Chunky, carte de 40 000 blocs de côté). Contexte complet : `REPRISE_PROJET.md`, « Passage de Kixster en Paper 26.3 ».

## Ce qui est envoyé

- **27 plugins KS_** (`plugins/*.jar`) : identiques à ceux d'Event (`jars-deployes/`), sauf **KS_Menu 1.1.0**
  (commande du menu réglable). KLM_Menu 2.9.0 et KLM_Chat 1.0.0 sont déjà sur Kixster.
- **Plugins externes**, téléchargés par `telecharger-externes.sh` depuis Modrinth / GeyserMC (empreintes vérifiées ;
  jamais dans le dépôt) : SimpleClaimSystem 1.13.1, VaultUnlocked 2.20.3, Floodgate 2.2.5 (b141),
  GrimAC 2.3.74-abb95b6 (alpha avec « support 26.3 »), WorldGuard 7.0.19, JourneyMap 26.3-6.0.10.
  Tous validés sur Event en Paper 26.3 le 07/10/2026.
- **Réglages des plugins** (`plugins/<plugin>/`) repris d'Event, **sans les données de jeu** (ni comptes, magasins,
  claims, jetons, alertes, historiques, `storage.db`, bases de GrimAC, régions WorldGuard), adaptés à Kixster :
  - `KS_Menu/config.yml` : `commande: kixster` (→ `/kixster`) ; `KS_Menu/lang.yml` : « Kixster ».
  - `KS_Tableau/lang.yml` : titre « Kixster » ; `KS_AntiCheat/lang.yml` : « se connecter à Kixster ».
  - `KS_RewardsGUI/config.yml` : `boite: kixster`.
  - `SimpleClaimSystem/config.yml` : mondes `Kixster SMP`, `Kixster SMP_nether`, `Kixster SMP_the_end` (noms
    affichés seulement ; noms exacts du Nether et de l'End à vérifier, voir plus bas).
- **Réglages de serveur de LeKiwi06** (ceux d'Event du 03/10/2026), appliqués par le script aux fichiers **actuels**
  de Kixster (rien n'est écrasé d'un bloc) :
  - `spigot.yml` : suivi des entités réduit (joueurs 64, animaux 48, monstres 48, divers 32, autres 32).
  - `config/paper-global.yml` : `allow-piston-duplication: true`.
  - `config/paper-world-defaults.yml` : anti-xray (mode 1, + coffres piégés, tonneaux, spawners), `lava-obscures`,
    hauteur 128.
  - Nether : anti-xray du Nether (`reglages-serveur/the_nether-anti-xray.yml`) ; End : anti-xray coupé.
  - `journeymap/server/6.0/` (dans `racine/`) : radar, carte des grottes, joueurs sous terre coupés.

## Secrets (jamais dans le dépôt, il est public)

`envoyer.sh` les lit au moment de l'envoi, dans un dossier temporaire supprimé à la fin :
- jeton du relais : `relay-token` de `plugins/KS_RewardsGUI/config.yml` **d'Event** ;
- clé Floodgate : `plugins/floodgate/key.pem` **d'Event** (la même que celle du proxy) ;
- secret Velocity : reste dans le `config/paper-global.yml` de Kixster (seule la ligne des pistons est changée).

## Envoi

Prérequis : Git Bash, WinSCP (sessions enregistrées pour Kixster et Event), Kixster **arrêté**, Event accessible.

```sh
cd KX_Monde/deploiement-kixster
DRY_RUN=1 sh envoyer.sh <session Kixster> <session Event>   # à blanc : prépare et vérifie, n'envoie rien
sh envoyer.sh <session Kixster> <session Event>             # envoi
```
Sessions par défaut : `kixster@7003.mystrator.com` et `Event@7021.mystrator.com` (noms des onglets WinSCP de
Maxster33 ; donner les siens en arguments). Les fichiers remplacés sont rangés dans
`/_removed-config-avant-plugins-event-<date>/` sur Kixster. Testé à blanc le 07/10/2026 (tout OK).

## Après l'envoi

1. Redémarrer Kixster ; dans le journal : les 33 plugins activés, aucune erreur (sur Event : 44 plugins sans erreur).
2. Noms des mondes : si SimpleClaimSystem affiche un nom brut pour le Nether ou l'End, corriger les clés
   `world-aliases` / `claims-worlds-mode` de `plugins/SimpleClaimSystem/config.yml` (le mode par défaut, SURVIVAL,
   est déjà le bon).
3. **KG_Rewards (Kal-Games)** : `boite: kixster` dans son `config.yml`, sinon les récompenses continuent d'aller
   sur Event (boîte `event`).
4. **LuckPerms de Kixster** : reprendre les permissions d'Event (ex. retirer `scs.command.*` au groupe default,
   `kseconomy.staff` pour le staff).
5. **WorldGuard** : régions `zone_shop` (`/rg define zone_shop`, `/rg flag zone_shop build allow`,
   `/rg flag zone_shop scs-claim allow` ; sans elle, KS_Economy ne crée aucun magasin) et `ile_du_dragon` dans
   l'End (`scs-claim deny`).
6. `chunk-system.worker-threads: 3` (mis pour la prégénération) dans `config/paper-global.yml` : remettre `-1`
   (automatique) si le jeu rame.
7. Pas dans ce paquet : le datapack `Woodcutter-7.2.zip` d'Event (le plugin WoodCutter a été retiré de Kixster).
