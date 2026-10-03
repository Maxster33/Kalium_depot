# KLM_Hub - journal

Plugin réseau (préfixe `KLM_`, prévu dans `ARCHITECTURE_CIBLE.md`) : ce qui accueille les joueurs sur un serveur
d'arrivée. Installé sur le **lobby**. Dépend de **KLM_Menu 2.9.0** (service `QrCodes`, textes `Lang`).

## 1.0.0 — barre de boss du lobby et QR codes (03/10/2026, LeKiwi06)

**Demande de LeKiwi06** : « ajouter une boss bar KaLium SMC [...] (j'en avais déjà créé une tu peux peut-être la
retrouver et l'activer pour tout le monde) » ; « faire un bouton cliquable pour donner le QR code vers le serveur
discord en main gauche », pareil pour « ma chaîne » Twitch ; « les QR ne restent affichés que 30 secondes avec le boss
bar ajouté précédemment dans le lobby montre le temps restant via les hp ». Précisions de LeKiwi06 le même jour : une
seule barre, **violette**, dont « seul le texte est en vert pâle », « uniquement dans lobby » ; à droite de
« KaLium SMC », après un « | », les liens affichés « en alternance, en simulant un texte déroulant de 20 caractères
qui se stoppe quelques instants quand le code est affiché en entier puis redémarre pour montrer l'autre ».

- **Barre de boss** permanente pour tous les joueurs du serveur : violette, pleine, titre vert pâle (`#98fb98`)
  « KaLium SMC | » suivi du texte défilant. Une barre par joueur (même titre pour tous) : sa progression sert de
  minuteur au QR code de ce joueur.
- **Texte défilant** : `discord.gg/3PsEbZPpdW` puis `twitch.tv/lekiwi06`, un caractère toutes les 3 ticks, arrêt de
  3 s quand un lien est affiché en entier, 4 espaces entre deux liens. La fenêtre de 20 caractères est **agrandie à
  21** toute seule : le lien du Discord fait 21 caractères et doit pouvoir s'afficher en entier.
- **QR codes** (service `fr.kalium.menu.api.QrCodes` de KLM_Menu, appelé par le bouton « Recherche de joueurs » de la
  boussole) : carte en **main secondaire** pendant 30 s, nom du QR code, « Scanne-le avec ton téléphone. ». La barre
  du joueur se vide pendant ces 30 s puis se remplit. La carte ne peut être ni jetée, ni déplacée, ni changée de
  main, ni posée dans un cadre ; elle est retirée à la fin, à la déconnexion, à la mort (jamais lâchée) et au
  retour du joueur s'il en restait une (plantage). Un 2e clic remplace le QR code et relance les 30 s.
  Main secondaire déjà prise par autre chose : refus avec un message (aucun objet du joueur n'est déplacé).
- **Générateur de QR code écrit dans le plugin** (`QrCode`, aucune bibliothèque embarquée) : mode octets, versions 1 à
  10, correction L ou M. Les deux liens donnent un QR code de 25 x 25 modules, dessiné à 4 pixels par module sur la
  carte (128 x 128). Vérifié hors serveur avec le décodeur jsQR (5 textes, versions 1, 2, 5 et 8, à la résolution de
  la carte).
- Le numéro de carte de chaque lien est gardé dans `plugins/KLM_Hub/cartes.yml` : la même carte ressert après un
  redémarrage.
- `config.yml` (toutes les clés facultatives, valeurs par défaut dans le code) : `bossbar.enabled`, `color`,
  `format`, `texts`, `scroll.width | gap | ticks-per-character | pause-seconds`, `qr.seconds`. Avec
  `bossbar.enabled: false`, la barre n'apparaît que pendant un QR code. Textes des messages : `lang.yml`.

**Signalé, non fait (pas demandé)** : pas de commande de rechargement (une modification de `config.yml` s'applique au
redémarrage) ; l'ancienne barre créée à la main avec `/bossbar` sur le lobby n'a pas pu être retrouvée depuis le
dépôt : si elle existe encore, la retirer en jeu (`/bossbar list`, `/bossbar remove <id>`) pour ne pas en avoir deux.

**Limites connues** : le titre est centré par le jeu et sa largeur change pendant le défilement (léger tremblement) ;
sur Bedrock, la couleur du texte est la plus proche disponible et la barre garde la couleur de Bedrock.

**À déployer avec KLM_Menu 2.9.0** (sur un serveur resté à une version plus ancienne, KLM_Hub ne se charge pas).

**Déployé sur le lobby le 03/10/2026 à 22:46 (LeKiwi06), avec KLM_Menu 2.9.0, actif après redémarrage. Statut : non testé en jeu.**
