# JOURNAL — KS_Parrainage

Plugin Paper du serveur **Kixster** : partie « Kixster » du parrainage. Les parrainages et les demandes de liste
blanche sont gardés par le proxy (KaliumRelay 1.10.0) ; les menus sont dans KLM_Contacts 1.5.0 (tous les serveurs).

## 1.0.0 - création (23/10/2026, LeKiwi06)

**Demande de LeKiwi06 (23/10/2026)**, à partir de son message d'annonce : « pour chaque joueur qui vient grâce à vous,
vous recevez tous les 2 des récompenses sur Kixster SMP, et le plus beau ? plus vos invités jouent, plus vous recevrez
de grosses récompenses ! » ; filleul « automatiquement whitelisté sur Kixster ». Barème proposé par Claude en .xlsx
(`sortie/parrainage/bareme-parrainage.xlsx`, hors dépôt) et validé (« je valide ») avec ses propositions : temps AFK
non compté, pas de coffre à l'arrivée, pas de limite de filleuls.

- **Liste blanche** : à la connexion d'un joueur absent de la liste blanche (`AsyncPlayerPreLoginEvent`), le proxy est
  interrogé (`wcheck`) ; s'il a été accepté (parrainage, ou demande acceptée par un admin), il entre
  (`PlayerConnectionValidateLoginEvent` ; `PlayerLoginEvent` est à retirer dans Paper 26), il est ajouté à la liste
  blanche à son arrivée (avec son vrai pseudo, Bedrock compris) et le proxy est prévenu (`wapplique`). Un joueur banni
  de ce serveur reste refusé.
- La liste blanche de Kixster et ses opérateurs sont envoyés au proxy 5 s après le démarrage, toutes les 5 minutes et
  après chaque ajout (`wsync`) : c'est elle qui fait refuser l'entrée par le proxy et ouvrir la demande.
- **Temps de jeu actif** des filleuls, compté minute par minute : actif = a tourné la tête, parlé, lancé une commande,
  cassé, posé ou utilisé un bloc dans les `inactivite-minutes` (5) dernières minutes. Un bassin AFK déplace le joueur
  sans lui faire tourner la tête.
- **Paliers** (`config.yml`, `paliers` : `heures`, `parrain`, `filleul`) : 1 h 5, 3 h 10, 6 h 20, 12 h 40, 24 h 80,
  48 h 150, 100 h 300, 200 h 600 émeraudes de valeur moyenne, pour chacun. À chaque palier, un coffre « Non ouvert »
  pour le filleul et un pour le parrain, dans /rewards (KS_RewardsGUI 1.6.0, `deposerCoffre`, tables de `fusion.yml`).
  Un coffre refusé (fusion.yml absent) est gardé dans `filleuls.yml` et réessayé chaque minute.
- Filleuls relus sur le proxy chaque minute (`pkixster`) ; temps de jeu et paliers renvoyés chaque minute (`pjeu`) pour
  le menu « Mes filleuls ».
- `/ksparrainage` (opérateurs) : état (filleuls, temps, paliers, coffres en attente) ; `/ksparrainage recharger` relit
  `config.yml`.
- Données : `plugins/KS_Parrainage/filleuls.yml`.

Réglages à faire au déploiement : `relay-token` dans `plugins/KS_Parrainage/config.yml` (après le premier démarrage) ;
la liste blanche de Kixster doit être active (`whitelist on`) ; `fusion.yml` présent dans `plugins/KS_RewardsGUI/`.

**Compilé, non déployé, non testé en jeu.** À déployer avec KaliumRelay 1.10.0 (proxy), KLM_Contacts 1.5.0 (tous les
serveurs Paper) et KS_RewardsGUI 1.6.0 (Kixster).

**Déployé sur Kixster le 23/10/2026 à 16:22 (déploiement groupé) ; actif après redémarrage ; `relay-token` à recopier ; non testé en jeu.**

**23/10/2026 : premier démarrage à 16:36 sans erreur (jeton vide signalé, comme prévu) ; `relay-token` posé à 16:45 (original dans `_removed-ks_parrainage-config-2026-10-23/`) ; actif après le prochain redémarrage de Kixster.**

## 1.1.0 - rattrapage des joueurs déjà whitelistés, « whitelist » (23/10/2026, LeKiwi06)

**Demandes de LeKiwi06 (23/10/2026, après le déploiement)** : « j'ai voulu parrainer un joueur, ça me dit que Kixster n'a pas encore envoyé la liste blanche (j'aimerais que les joueurs déjà whitelist puissent prendre un parrain, pour qu'on puisse rattraper les récompenses qu'on aurait dû avoir) ; utilise le terme whitelist au lieu de liste blanche d'ailleurs » ; « j'aimerais aussi que les heures AFK sur le serveur ne comptent pas (considérer un joueur comme AFK s'il n'effectue pas d'actions pendant plus de 5 minutes et enlever l'AFK quand une action est effectuée) ». Ses choix : rattrapage sur le temps vanilla (CoreProtect, prévu d'abord, ne tourne pas sur Kixster en 26.3), temps actif aussi dans Statistiques.

- Un filleul qui jouait déjà sur Kixster reçoit une fois son temps de jeu vanilla passé (`PLAY_ONE_MINUTE`, AFK compris : l'activité n'était pas suivie avant), quand ce serveur apprend le parrainage : les coffres des paliers déjà dépassés arrivent d'un coup, pour lui et son parrain. Ensuite seul le temps actif compte. Gardé dans `filleuls.yml` (`rattrapage`, `rattrape`) ; les filleuls d'avant la 1.1.0 sont considérés comme déjà rattrapés.
- `/ksparrainage recharger` envoie aussi la whitelist au proxy et relit les filleuls (le jeton est pris en compte sans redémarrage) ; `/ksparrainage` montre les heures reprises.
- « whitelist » dans les textes (message de bienvenue : nouvelle clé `bienvenue-2`).
- Choix de Claude : CoreProtect, prévu d'abord pour estimer le temps actif passé, ne démarre pas sur Kixster (« Minecraft 26.3 is not supported ») ; LeKiwi06 a choisi le temps vanilla.

**Compilé, non déployé, non testé en jeu.** À déployer ensemble : KaliumRelay 1.10.1 (proxy), KLM_Contacts 1.5.1 (tous les serveurs Paper), KS_Parrainage 1.1.0 + KLM_Statistiques 1.1.0 + `didacticiel.yml` (Kixster). Empilé sur les versions du 23/10 (déployées, pas encore testées), à la suite de la même demande.
