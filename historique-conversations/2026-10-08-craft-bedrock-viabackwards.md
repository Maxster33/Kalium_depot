# 2026-10-08 — Interface de craft des joueurs Bedrock (ViaBackwards)

- Plugin(s) concerné(s) : aucun des nôtres ; plugins externes ViaVersion et ViaBackwards (`mise-a-jour-26.3/`).
- Versions avant / après : ViaVersion 5.12.1-SNAPSHOT+1069 → 5.12.1 ; ViaBackwards 5.12.1-SNAPSHOT+634 → 5.12.1
  (Kixster seulement).

## Demandé
LeKiwi06 : « geyser pose des soucis avec la nouvelle mise à jour pour l'interface de craft des joueurs bedrock, y a-t-il
un fix pour ce problème ? », puis « oui mets à jour les scripts et envoie sur kixster ».

## Fait
- Recherche : Geyser (2.11.3-b1249) ne parle que la 26.2 ; les joueurs Bedrock passent par ViaBackwards sur nos
  serveurs 26.3. Le build du 22/09 en service traduit mal le livre de recettes 26.3 → 26.2 (ViaVersion/ViaVersion#5072,
  corrigé par ViaVersion/ViaBackwards#1336 le 29/09 ; même symptôme chez Geyser : GeyserMC/Geyser#6755).
- `mise-a-jour-26.3/telecharger.sh` : ViaVersion et ViaBackwards 5.12.1 stables (empreintes sha512 de Modrinth).
- `mise-a-jour-26.3/envoyer.sh` : SNAPSHOT → 5.12.1 sur tous les serveurs Paper ; serveurs `kixster` et `event`
  ajoutés (deux jars seulement, sans sauvegarde) ; `SESSION_WINSCP=` et `SANS_SAUVEGARDE=1`.
- Kixster : envoi du 08/10/2026 à 23:42 (essai à blanc avant), tailles vérifiées, anciens jars dans
  `/plugins/_removed-via-5.12.1-snapshot/`.

## Décisions
- Pas de build non officiel de ViaBackwards, ni de build de test de Geyser 26.3 (déconseillé en production).
- Kixster d'abord, les autres serveurs après le test.

## Reste à faire
- Redémarrer Kixster (l'humain), puis tester le craft avec un joueur Bedrock.
- Si c'est bon : Event, lobby, Kanvas, Serveur Jeux, Kal-Games (`sh envoyer.sh <serveur>`).
- Lenteur éventuelle du craft : voir GeyserMC/Geyser#6744 (ouvert).

## Suite (09/10/2026)
- Test de LeKiwi06 à 00:09 (« je vois des planches de bois dans les crafts des outils ») : Kixster n'avait pas été
  redémarré depuis l'envoi (dernier démarrage 08/10 à 21:05, Via 5.12.1-SNAPSHOT dans le journal). À refaire après
  redémarrage.
- « Le cadre a une membrane de phantom ? » : c'est la recette du cadre invisible de KS_Crafts.
- Demande : réparations de la tête de wither squelette mal indiquées dans l'interface de craft → **KS_Crafts 1.11.0**
  (vrais objets affichés ; voir `KS_Crafts/JOURNAL.md`), compilé, non déployé, non testé ; KS_Crafts réservé.
- Reste à faire : accord de LeKiwi06 pour déployer KS_Crafts 1.11.0 (Event, Kixster), puis test en jeu.
