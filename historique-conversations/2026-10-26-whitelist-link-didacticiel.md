# 2026-10-26 — Historique de la whitelist, link Java / Bedrock, didacticiel du lobby

- Plugin(s) concerné(s) : KaliumRelay, KLM_Contacts ; cadrage seulement pour KS_Liaison (nouveau) et KS_Didacticiel
  (lobby).
- Versions avant / après : KaliumRelay 1.10.1 → 1.10.2 ; KLM_Contacts 1.5.1 → 1.5.2.

## Demandé

« j'aimerai que tu me créer un historique pour les demandes de whitelist traités » ; puis « met moi un message quand je
rejoins le lobby comme pour les rewards pour me dire combien de demande de whitelist sont en attente, histoire que je
n'en laisse pas trainer trop longtemps ».

Dans le même message : une option sur Kixster pour lier (« link ») un compte Java et un compte Bedrock (même
inventaire, mêmes coordonnées ; demande et acceptation ; inventaire du compte qui perd sa progression rendu en rewards ;
jamais connectés en même temps ; délink : le joueur choisit le compte qui garde la progression, l'autre recommence de
zéro) ; un didacticiel dans le lobby (quel serveur sert à quoi, le concept de KaLium).

## Fait

- KaliumRelay 1.10.2 : chaque demande traitée (admin ou parrainage) gardée pour toujours avec sa date de traitement
  (lignes `H` de `parrainage.txt`) ; demandes déjà traitées reprises sans date ; requête `whdemandes` ; rappel « Tu as N
  demande(s) de whitelist en attente » aux admins de `relay.properties`, 3 s après chaque arrivée au lobby (premier
  serveur de `try`), à la place du message de la 1.10.0 (première connexion seulement, sans doute perdu).
- KLM_Contacts 1.5.2 : Modération › Demandes de whitelist › « Demandes traitées » (colorant vert / rouge, par qui,
  quand, Discord, Java / Bedrock ; 45 par page, la plus récente d'abord).
- Déployés le 26/10/2026 à 01:11 (accord de LeKiwi06 : « Oui, déploie ») : proxy, lobby, Kixster, Kanvas, Serveur Jeux ;
  anciens jars dans `_removed-kaliumrelay-1.10.1/` et `_removed-klm_contacts-1.5.1/`.
- Link et didacticiel : questions posées, maquette des menus du link montrée ; cahier des charges et brouillon du
  didacticiel écrits chez LeKiwi06, hors dépôt. Rien de codé.

## Décisions

- Link (LeKiwi06) : progression gardée au choix du joueur ; l'autre compte « redonne tout » (coffres de rewards :
  inventaire avec l'XP en fioles, coffre de l'Ender, inventaires spéciaux) ; demande et acceptation dans le menu
  Contacts.
- Didacticiel du lobby (LeKiwi06) : ouvert depuis le menu Informations.
- Choix de Claude, à juger : colorants plutôt que têtes dans « Demandes traitées » ; pas de recherche par pseudo.

## Reste à faire

- Redémarrer le proxy et les 4 serveurs Paper (humain), puis tester : traiter une demande, la voir dans « Demandes
  traitées » ; arriver au lobby avec une demande en attente (message 3 s après).
- Dossiers supprimables (3 versions ou plus derrière, suppression par l'humain) : proxy `_removed-kaliumrelay-` 1.0.1,
  1.1.0, 1.1.1, 1.2.0, 1.3.0, 1.4.0, 1.5.0, 1.6.0, 1.7.0, 1.8.0, 1.9.0 ; `_removed-klm_contacts-` lobby 1.0.0, 1.1.0,
  1.2.0 ; Kixster 1.2.0 ; Kanvas 1.0.0, 1.1.0, 1.2.0, 1.4.0 ; Serveur Jeux 1.0.0, 1.1.0, 1.2.0.
- Link : validation du cahier (claims, homes, grade, coffres de mort...), puis prototype pour tester Floodgate.
- Didacticiel du lobby : validation du brouillon (concept de KaLium, récompenses envoyées sur Kixster à confirmer).
