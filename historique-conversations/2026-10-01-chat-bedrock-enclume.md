# 2026-10-01 — Chat Bedrock, enclume au-delà de 50 niveaux

- Plugin(s) concerné(s) : KS_Enclume ; réglage `server.properties` de tous les serveurs Paper
- Versions avant / après : KS_Enclume 1.1.4 → 1.2.0

## Demandé
- « Aucun joueur bedrock ne peut utiliser le chat en jeu. Les joueurs PS5 ne peuvent pas accéder aux paramètres en
  jeu. » Message d'erreur : « chat désactivé à cause de l'absence de la clé publique du profile ».
- « l'augmentation du prix en expérience pour la réparation et l'amélioration d'objet sur l'enclume, soit réduite de
  moitié à partir du niveau 50 », puis « installe ».

## Fait
- Chat : cause identifiée (`enforce-secure-profile=true`) ; Maxster33 l'a passé à `false` lui-même ; réglé.
- KS_Enclume 1.2.0 codé, compilé, déployé sur Event à 19:50 (1.1.4 dans `_removed-ks_enclume-1.1.4/`).

## Décisions
- Enclume : coût vanilla jusqu'à 50, partie au-dessus comptée pour moitié (choisi parmi : coût total / pénalité
  ralentie / les deux). Arrondi à l'unité inférieure. Fioles de KS_FioleExp exclues.

## Reste à faire
- Tester KS_Enclume 1.2.0 en jeu (Java, Bedrock) après redémarrage d'Event.
- Paramètres inaccessibles sur PS5 : à préciser (symptôme exact, serveur).
- Supprimables par l'humain sur Event : `_removed-ks_enclume-1.0.0/`, `1.1.0/`, `1.1.1/`, `1.1.2/`.
