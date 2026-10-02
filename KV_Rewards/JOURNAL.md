# KV_Rewards - journal

Plugin de Kanvas : récompenses des notes reçues et des concours de build. Cahier des charges : catégorie 4
« Récompenses » de LeKiwi06 (validé le 30/09/2026 ; décisions du 03/10/2026 : tous les bâtisseurs d'un plot, concours
au total des notes avec 3 places ; copié dans `CAHIER_DES_CHARGES.md` au déploiement). Mêmes règles que KG_Rewards.

## 1.0.0 - paliers, tops, prestige, concours, tables de butin (03/10/2026)

- **Compteur** : somme des notes reçues par les plots dont le joueur est créateur ou éditeur (KV_Plots 1.5.0).
  Périodes : **mois** (aligné comme KG_ScoreBoards : du 1er vendredi 21 h au suivant, heure de Paris) et **permanent**.
- **Paliers** : mêmes seuils que KG_Rewards (minimale / petite / moyenne / grosse / exceptionnelle), vérifiés à chaque
  note reçue (`NoteRecueEvent`), pour chaque bâtisseur du plot. **Aucun palier rétroactif** (au premier démarrage, les
  notes déjà reçues et les tops permanents occupés servent de point de départ).
- **Prestige** (dès 100 000 notes permanentes, jamais obligatoire, bouton + confirmation) : le compteur des paliers
  permanents repart de 0 ; +20 % de quantité par niveau sur les paliers permanents seulement.
- **Tops** : classiques 1, 2, 3, 5, 10, 25, 50, 100 (seul le meilleur) et 50, 25, 10, 5, 1 % (cumulés). Mois :
  classement final au changement de mois (vérifié chaque minute). Permanent (toutes les 5 minutes) : à l'arrivée, puis
  chaque semaine passée dans le top (comme KG_Rewards).
- **Concours de build** (`ConcoursTermineEvent`) : 1re, 2e et 3e places (total des notes, puis moyenne ; au moins
  1 point) ; chaque bâtisseur du plot reçoit le butin de sa place.
- **Butin** (`plugins/KV_Rewards/butin.yml`, interface admin, comme KG_Rewards) : pools par rareté ; périodes mois,
  permanent (paliers et tops) et concours (3 places) : tirages, fréquences par pool, fixes.
- **Envoi** : boîte `event` de KaliumRelay 1.3.0 (lue par KS_RewardsGUI) ; file `plugins/KV_Rewards/envois.yml`
  (nouvel essai toutes les 15 secondes).
- **Commandes** : `/kvrewards` (progression, prestige) ; `/kvrewards admin` (tables de butin, `kvrewards.admin`,
  opérateurs). Bouton « Récompenses » de KLM_Menu 2.6.0 (**obligatoire sur Kanvas**).
- Données : `plugins/KV_Rewards/joueurs.yml`. Configuration : `relay-url`, `relay-token` (**vide dans le dépôt**),
  `boite`, `origine` (Kanvas). `depend` KV_Plots (1.5.0), KLM_Menu.

**Déployé sur Kanvas le 03/10/2026 à 01:05 (LeKiwi06, catégorie 4 « Récompenses » et correctif des claims) (nouveau), actif après redémarrage. Statut : non testé en jeu.**
