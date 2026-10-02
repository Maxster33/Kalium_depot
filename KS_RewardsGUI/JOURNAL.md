# KS_RewardsGUI - journal

Plugin du serveur Event : réception des récompenses des autres serveurs. Cahier des charges : catégorie 4
« Récompenses » de LeKiwi06 (validé le 30/09/2026 ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - réception, /rewards, journal (03/10/2026)

- **Transport** : boîte aux lettres `event` de KaliumRelay 1.3.0 (durable). Toutes les 30 secondes (`intervalle-secondes`),
  les messages sont lus, enregistrés dans `plugins/KS_RewardsGUI/recompenses.yml`, puis confirmés au relais ; un message
  déjà reçu n'est jamais compté deux fois (ids gardés). Joueur en ligne : message « Nouvelle récompense ». À la connexion :
  nombre de récompenses en attente.
- **Format d'un message** (YAML, documenté dans `Recompense.java`) : joueur (UUID), nom, origine, raison, date, contenu
  (`objet` : ItemStack sérialisé en base64 + nombre ; `custom` : id_custom de KS_KaliumGive + nombre ; `argent` : points
  du score).
- **`/rewards`** (alias `/recompenses`) et bouton « Récompenses » du menu d'Event : liste (origine, raison, contenu ; 8 par
  page), « Récupérer n », « Tout récupérer » (dans l'ordre, tant que l'inventaire a de la place). Refus si l'inventaire
  manque de place, si un objet custom n'existe pas sur Event (la récompense reste) ou si l'économie est absente. Pas
  d'expiration.
- **Journal** : `plugins/KS_RewardsGUI/recuperations.log` (date, joueur, origine, raison, contenu) pour l'anti-triche
  (catégorie 6).
- Configuration : `relay-url`, `relay-token` (**vide dans le dépôt** : à remplir sur le serveur avec le jeton du relais),
  `boite`, `intervalle-secondes`.
- `depend` KLM_Menu ; `softdepend` KS_Menu, KS_Economy, KS_KaliumGive ; `build.sh` : compiler ces plugins d'abord.

- **Bouton « Récompenses »** de KLM_Menu 2.6.0 : déclaré (`Recompenses`) ; ouvre `/rewards`. **KLM_Menu 2.6.0 obligatoire sur Event.**

**Non déployé (catégorie 4, étape 1 ; déploiement prévu avec les étapes suivantes). Statut : non testé.**
