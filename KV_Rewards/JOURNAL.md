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

## 1.1.0 - objets vanilla par leur id dans butin.yml (09/10/2026, LeKiwi06)

Même changement que KG_Rewards 1.1.0 (demande de LeKiwi06 : « ajoute la lecture par id et génère les butin.yml ») :
dans `butin.yml`, un objet vanilla peut être écrit par son id, `{type: objet, id: diamond, nombre: 2}`, avec au besoin
`enchantements: {mending: 1}` ; il est remplacé au chargement par l'objet créé par le serveur (`donnees`), comme s'il
avait été déposé dans l'interface admin. Id ou enchantement inconnu : élément ignoré, signalé dans la console. Le
fichier garde les ids jusqu'à la première modification dans l'interface admin. Aucun autre changement.

Limites : comme KG_Rewards 1.1.0 (écrit sans serveur de test ; empilé sur la 1.0.0 jamais testée, demande explicite de
LeKiwi06). Le `butin.yml` généré pour Kanvas (mois, permanent, concours) est dans `sortie/rewards/butin/` (hors
dépôt) ; ses paliers reposent sur une hypothèse de 20 notes reçues par heure de build, jamais mesurée.

**Commande d’essai** (demande de LeKiwi06 : « donne moi 10 récompenses de chaque dans mon reward sur kixster pour tester ») :
`/kvrewards test <joueur> <mois | permanent | concours> <niveau | tout> [nombre] [boîte]` (admins `kvrewards.admin` et console ; nombre de 1 à 100).
Tire des récompenses dans les tables de butin et les envoie comme de vraies récompenses (raison « Essai n - <niveau>
(<période>) »), dans la boîte de `config.yml` ou dans celle indiquée (ex. `kixster`) ; aucun palier ni top n’est marqué
comme atteint. Pour cela, un message en attente peut porter sa propre boîte (première ligne `#boite:<nom>` dans
`envois.yml`, retirée à l’envoi). Les vraies récompenses partent toujours vers la boîte de `config.yml`.

**Déployé sur Kanvas le 09/10/2026 à 07:05 (LeKiwi06 ; 1.0.0 dans `_removed-kv_rewards-1.0.0/`) avec `plugins/KV_Rewards/butin.yml` (aucun n'existait avant), actif depuis le redémarrage de 07:12 (journal lu : 1.1.0 activée, aucune erreur, aucun objet du `butin.yml` refusé). `config.yml` du serveur non touché. Statut : non testé en jeu.**

## 1.2.0 - objets enchantés au tirage, potions, valeur moyenne du niveau (09/10/2026, LeKiwi06)

Même changement que KG_Rewards 1.2.0 (voir son `JOURNAL.md` pour la demande de LeKiwi06 et le détail) : `enchanter:
[min, max]` (objet créé et enchanté à chaque tirage, sans trésor), `enchantements` ajoutés ensuite, `potion: <type>`,
`valeur` d'un niveau envoyée avec la récompense et affichée sur le coffre par KS_RewardsGUI 1.3.0. Mêmes pools que
Kal-Games ; niveaux du mois, du permanent et des concours.

**Déployé sur Kanvas le 09/10/2026 à 15:12 (LeKiwi06, feu vert après présentation ; 1.1.0 dans `_removed-kv_rewards-1.1.0/`, avec l'ancien `butin.yml`) avec le nouveau `butin.yml`, actif depuis le redémarrage de 16:03 (journal lu : 1.2.0 activée, aucune erreur, aucune des 512 entrées refusée). `config.yml` du serveur non touché. Statut : non testé en jeu.**
