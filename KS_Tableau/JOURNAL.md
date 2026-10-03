# KS_Tableau - journal

Plugin du serveur Event : tableau sur le côté de l'écran. Demande de LeKiwi06 (revue du 03/10/2026 : « fais en sorte
qu'on puisse l'activer via commande, il faut aussi afficher les coordonnées du joueur et le biome dans lequel il se
trouve »).

## 1.0.0 - tableau latéral (03/10/2026)

- `/tableau` (ou `/tableau on` | `off`) : affiche ou masque le tableau ; **masqué par défaut** ; le choix est gardé
  (`plugins/KS_Tableau/tableau.yml`).
- Titre « Event » ; lignes : score (KS_Economy), « Exploration journalière : n / 10 » (KS_FairPlay 1.0.3), nombre de
  claims (SimpleClaimSystem, relu toutes les 30 s), coordonnées X Y Z, biome (nom traduit par le jeu du joueur).
  Mis à jour chaque seconde ; une ligne dont le plugin manque n'est pas affichée.
- `depend` KLM_Menu ; `softdepend` KS_Economy, KS_FairPlay, SimpleClaimSystem.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.0.0 ; nouveau), actif après redémarrage d'Event. Statut : non testé en jeu.**
