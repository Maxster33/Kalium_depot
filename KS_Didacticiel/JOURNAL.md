# KS_Didacticiel - journal

Plugin de Kixster : didacticiel des changements apportés au jeu, dans le comparateur « Informations ». Dépend de
KLM_Menu (2.11.0 : `Contenant`, `MenuSection.informations()`).

## 1.0.0 - didacticiel en coffre, contenu dans didacticiel.yml, marque « Nouveau » (18/10/2026, LeKiwi06)

Demande de LeKiwi06 : « il faut que tu me crées un didacticiel qui sera dans le menu information sur kixster, ce
didacticiel permet d'expliquer tous les changements. il faudra donc le mettre à jour régulièrement ». Maquette montrée
avant le code ; ses réponses :

| Question | Réponse de LeKiwi06 |
|---|---|
| Forme | Menu en coffre (un objet par thème, puis un objet par nouveauté, l'explication dans sa description) |
| Mise à jour | Fichier sur le serveur (`didacticiel.yml`), rechargé sans recompiler ni redémarrer |
| Nouveautés récentes | « marque nouveau, jusqu'à ce que la personne l'ait ouvert pour la première fois » |
| Outils du staff | Pas dans le didacticiel |

- **Ouverture** : bouton « Didacticiel » du comparateur « Informations » (`/infos` sur Kixster, où le comparateur
  n'est donné qu'après `/menu on`), premier bouton de joueur (ordre 10, avant « Contacts ») ; aussi `/didacticiel`.
- **Écran des thèmes** (coffre, une rangée par 9 thèmes + barre) : un objet par thème (nom, résumé, nombre de
  nouveautés, « Nouveau : n à découvrir », « Clic : voir le détail ») ; barre : livre « Comment ça marche », « Retour »
  vers « Informations » (porte) ou « Fermer » (barrière, ouvert par `/didacticiel`).
- **Écran d'un thème** : un objet par nouveauté ; sa description = le texte (coupé à environ 35 caractères) et, s'il y
  en a une, « Commande : /... ». « Retour » vers les thèmes.
- **« Nouveau »** : par joueur et par nouveauté. Une nouveauté dont la valeur `nouveau` n'a pas encore été vue par le
  joueur porte « Nouveau » en tête de sa description et brille ; son thème brille et dit combien il en reste. Ouvrir
  le thème enregistre ce que le joueur a vu (`plugins/KS_Didacticiel/vus.yml`, uuid > thème > nouveauté = valeur
  vue) : la marque est encore affichée pendant cette ouverture, puis disparaît. Changer la valeur `nouveau` d'une
  nouveauté (la date du jour) la remet en « Nouveau » pour tous.
- **Contenu** : `plugins/KS_Didacticiel/didacticiel.yml`, créé depuis le jar au premier démarrage ; référence dans le
  dépôt : `src/main/resources/didacticiel.yml` (mode d'emploi en tête du fichier). Titre, thèmes (nom, icône, résumé)
  et nouveautés (nom, icône, texte, commande facultative, nouveau). Icône inconnue : livre et avertissement dans la
  console ; 45 thèmes et 45 nouveautés par thème au plus.
- **`/didacticiel recharger`** (permission `ksdidacticiel.admin`, opérateurs ; aussi depuis la console) : relit le
  fichier et répond « 10 thèmes, 58 nouveautés » (et le nombre d'avertissements).
- **Contenu de la 1.0.0** : tout ce qui est en service sur Kixster au 18/10/2026 (inventaire du même jour, tiré des
  `JOURNAL.md` et de `REPRISE_PROJET.md`), en 10 thèmes et 58 nouveautés : le monde, le butin, crafts et objets,
  enclume / meule / expérience, économie, claims, se déplacer, mort et récompenses, véhicules, menus et fair-play. Pas
  ce qui n'est que sur Event (trains, laisse courte, têtes de la 1.3.0 de KS_Decapitator).
- Textes des menus : `lang.yml` du plugin (créé au premier démarrage).

Choix de Claude, signalés :
- `/didacticiel` (non demandé) : le comparateur n'est pas dans la barre par défaut sur Kixster ; la commande sert aussi
  à `recharger`.
- Au premier déploiement, toutes les nouveautés portent `nouveau: "2026-10-18"` : chaque joueur voit tout en
  « Nouveau » jusqu'à ce qu'il ouvre chaque thème.
- Ordre 10 dans « Informations » ; « Nouveau » marqué à l'ouverture du thème (une nouveauté ne s'ouvre pas : on la
  lit en passant la souris dessus).

Limites :
- Écrit sans serveur de test. Bedrock : description des objets et brillance à vérifier.
- Le texte est coupé aux espaces à environ 35 caractères : une ligne peut être un peu plus longue avec un mot long.
- Une mise à jour du fichier ne fait rien tant que `/didacticiel recharger` n'a pas été tapé (ou le serveur redémarré).

**Mise à jour du contenu** (à chaque déploiement qui change le jeu sur Kixster) : modifier
`src/main/resources/didacticiel.yml` (nouvelle nouveauté ou texte changé, avec la date du jour dans `nouveau`), le
commiter, l'envoyer dans `plugins/KS_Didacticiel/` sur Kixster, puis `/didacticiel recharger`. Le jar n'est à
recompiler que si le code change (le fichier du jar ne sert qu'au premier démarrage).

Compilé le 18/10/2026 (`sortie/KS_Didacticiel-1.0.0.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 08:01 (LeKiwi06 : « Envoie d'abord KS_Didacticiel 1.0.0 sur Event pour que je teste le menu, Kixster ensuite » ; nouveau ; KLM_Menu 2.11.0 en place ; jar du serveur vérifié de la même taille que celui compilé), actif après redémarrage d'Event. Kixster pas encore. Statut : non testé en jeu.**
