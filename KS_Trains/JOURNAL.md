

**Déployé sur Event le 17/10/2026 à 02:41 (LeKiwi06 : « tu peux y aller » ; KS_Trains 1.3.0 en place vérifié identique à la référence, rangé dans `_removed-ks_trains-1.3.0/` avec son `config.yml` (identique à celui envoyé le 16/10, sans retouche) ; `config.yml` remplacé par celui de la 1.3.8 (`acceleration` 0,005, `freinage` 0,01, `decor.taille` 0,875, clés `virage`, `percussion`, `propulseur`) ; jar et `config.yml` du serveur vérifiés identiques après l'envoi ; supprimables par l'humain : `_removed-ks_trains-1.0.0/`, `1.1.0/`), actif après redémarrage d'Event. La 1.3.8 contient les 1.3.1 à 1.3.7, jamais déployées. Statut : non testé en jeu.**

## 1.3.9 - train arrêté en pente : les wagons ne glissent plus (17/10/2026, LeKiwi06)

Signalement de LeKiwi06 (capture sur Event) : « mon train s'est arrêté en pente, et du coup il s'est entassé ».

- **Cause** (défaut de la 1.0.0, visible avec la nouvelle physique) : sur une pente, le jeu pousse chaque wagonnet vers
  le bas de 0,0078 bloc/tick à chaque tick (lu dans le code du jeu 26.3, NewMinecartBehavior.calculateSlopeSpeed ; sans
  lien avec la gravité de l'entité). Train arrêté, seule la tête était tenue en place ; les autres wagons n'étaient que
  poussés vers leur place et glissaient jusqu'à s'entasser.
- **Correction** : train arrêté, chaque wagon est remis à sa place exacte dès qu'il s'en écarte de plus de 0,05 bloc, et
  sa vitesse est remise à zéro. En marche, rien ne change.
- Un train déjà entassé ne se démêle pas seul (son trajet enregistré est tassé lui aussi) : dételer ses wagons aux
  cisailles, les remettre en ligne sur les rails, puis les rattacher.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.3.9.jar`). **Statut : non déployé, non testé en jeu.**

