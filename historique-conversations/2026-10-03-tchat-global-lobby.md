# 2026-10-03 — Tchat global, barre de boss du lobby, recherche de joueurs

- Plugin(s) concerné(s) : KLM_Chat (nouveau), KLM_Hub (nouveau), KLM_Menu ; KLM_Portal (configuration du lobby seulement)
- Versions avant / après : KLM_Chat — / 1.0.0 ; KLM_Hub — / 1.0.0 ; KLM_Menu 2.8.0 / 2.9.0 ; KLM_Portal 1.3.2 inchangé

## Demandé

LeKiwi06, pour « le serveur dans son ensemble » :
- « ajouter une boss bar KaLium SMC en jaune, avec une boss bar violette dessous (j'en avais déjà créé une tu peux
  peut-être la retrouver et l'activer pour tout le monde) » ;
- « ajouter l'effet night vision permanent à tous dans le lobby (met un temps assez long genre 1 minute et pas 2
  secondes en permanence sinon côté bedrock l'effet clignote) » ;
- « un bouton supplémentaire dans la boussole de KLM_menu "recherche de joueurs" : s'il y a moins de 4 joueurs dans le
  serveur où se trouve la personne » : proposer `/global send/true/false <message>`, « rejoindre le discord » (lien,
  code 3PsEbZPpdW, bouton qui donne le QR code en main gauche), « repasser quand kiwi est en live »
  (twitch.tv/lekiwi06, bouton QR code) ;
- « les QR ne restent affichés que 30 secondes », la barre de boss du lobby « montre le temps restant via les hp » ;
- tchat inter-serveur : séparations `-----GLOBAL------` / `------<Nom du serveur>-------`, préfixe du serveur dans la
  couleur de son portail, contenu en gris clair pour que le tchat du serveur reste blanc.

## Fait

- **KLM_Chat 1.0.0** (nouveau, tous les serveurs Paper) : `/global send <message> | true | false`, préfixes colorés,
  séparations, choix gardé par serveur. Transport par le canal BungeeCord « Forward » (rien sur le proxy).
- **KLM_Hub 1.0.0** (nouveau, lobby) : barre de boss violette, titre vert pâle « KaLium SMC | » + texte défilant
  Discord / Twitch avec arrêt sur chaque lien ; QR codes en main secondaire pendant 30 s, la barre se vide pendant ce
  temps. Générateur de QR code écrit dans le plugin, vérifié avec un décodeur (jsQR) hors serveur.
- **KLM_Menu 2.9.0** : bouton « Recherche de joueurs » dans la boussole (moins de 4 joueurs sur le serveur), nouvelle
  API `QrCodes`.
- Compilés (`sortie/`), **non déployés, non testés en jeu**.

## Décisions

- LeKiwi06 : `/global true | false` = afficher / masquer le tchat global (affiché par défaut).
- LeKiwi06 : une seule barre, violette, texte en vert pâle, « uniquement dans lobby » (et non jaune + violette) ;
  liens en alternance dans un texte défilant de 20 caractères qui s'arrête sur chaque lien. La fenêtre passe à 21
  caractères (longueur du lien du Discord) pour qu'il s'affiche en entier.
- LeKiwi06 : deux nouveaux plugins (KLM_Chat, KLM_Hub) plutôt que tout dans KLM_Menu (« un plugin = un rôle ») ;
  KLM_Menu et KLM_Portal réservés en « requis parfois ».
- LeKiwi06 : couleurs des préfixes : Lobby `#09add3`, Kal-Games or, Kixster vert, Event rouge, Kanvas rose, Bingo
  (serveur-jeux) jaune.
- Vision nocturne : par les **effets de zone de KLM_Portal** (déjà utilisés pour speed et jump boost dans la région
  `lobby`, durée infinie, donc sans clignotement) : une ligne de configuration sur le lobby, pas de code.
- L'ancienne barre faite à la main n'est pas dans le dépôt ; l'accès aux sessions WinSCP a été refusé à Claude dans
  cette session : la barre est recréée par KLM_Hub.
- QR codes proposés seulement là où KLM_Hub est installé (lobby) ; ailleurs, le bouton montre les liens.

## Reste à faire

- Déployer : KLM_Chat 1.0.0 et KLM_Menu 2.9.0 sur les 6 serveurs Paper, KLM_Hub 1.0.0 sur le lobby (avec KLM_Menu
  2.9.0), puis redémarrages par l'humain.
- Lobby : ajouter `night_vision: 0` sous `region-effects.lobby.effects` de `plugins/KLM_Portal/config.yml` (copie de
  l'ancien fichier dans `_removed-…` avant), puis `/klmportal reload`.
- Retirer l'ancienne barre du lobby si elle existe encore (`/bossbar list`, `/bossbar remove <id>`).
- Tests en jeu : voir le compte rendu de `REPRISE_PROJET.md`.
- Signalé, à décider : anti-spam et joueurs muets pour `/global` ; `/global true | false` commun à tous les serveurs.
