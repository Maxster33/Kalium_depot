# 2026-10-14 — Verrou de véhicule

- Plugin(s) concerné(s) : KS_VerrouVehicule (nouveau), KS_Crafts
- Versions avant / après : KS_VerrouVehicule — / 1.0.0 ; KS_Crafts 1.14.1 / 1.15.0 (Kixster seulement)

## Demandé
LeKiwi06 : « j'aimerai faire un item pour verrouiller le contenant d'un véhicule, histoire qu'on puisse le garer même
hors claims et qui ne soit pas cassable, déplaçable, vidable par hopper, etc. tant que le contenant n'est pas vide ».
Puis : « oui déploie sur kixster ».

## Fait
- Questions de cadrage posées avant le code (tableau des réponses dans `KS_VerrouVehicule/JOURNAL.md`).
- Code du jeu 26.3 lu pour les places des bateaux : un bateau à coffre n'a qu'une place, et un joueur qui monte passe
  toujours conducteur, d'où le siège invisible pour « monter sans conduire ».
- KS_VerrouVehicule 1.0.0 et KS_Crafts 1.15.0 (recette crochet + 4 lingots de fer) compilés, poussés, déployés sur
  Kixster le 14/10/2026 à 18:03 (jars vérifiés identiques avant et après l'envoi). Event non touché.

## Décisions
- Véhicules : bateaux et radeaux à coffre, wagonnets à coffre et à entonnoir, bateau à coffre et jukebox, âne / mule /
  lama à coffre.
- Ouverture par le propriétaire seul ; les autres montent sans conduire ; le propriétaire qui monte reprend la place
  du conducteur.
- Déplacement : le propriétaire, et le propriétaire et les membres du claim où se trouve le véhicule (dans ce claim).
- Verrou qui reste posé, ne protège que si le contenant n'est pas vide, retiré par un petit menu avant le coffre (le
  bouton dans le vrai coffre n'est pas faisable : proposé puis remplacé par ce menu, choix de LeKiwi06).
- Objet vanilla renommé (crochet), Kixster seulement.
- Choix de Claude signalés : animal verrouillé seulement par son maître, pas de pose dans le claim d'un autre,
  propriétaire qui ne casse pas non plus un véhicule protégé, opérateurs qui ouvrent et déplacent sans casser.

## Reste à faire
- Redémarrage de Kixster (humain), puis lecture du journal.
- Tests en jeu : craft, pose, refus (ouverture, casse, entonnoir), siège invisible (hauteur, Java et Bedrock),
  ancrage, menu du propriétaire, ouverture d'un âne / lama depuis le menu, déplacement par un membre de claim.
- Suppression par l'humain sur Kixster : `_removed-ks_crafts-1.10.0/`, `1.11.0/`, `1.12.0/`.
