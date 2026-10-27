# KS_CasseCroute - journal

Plugin autonome, serveur Kixster. Le Casse-croûte doré (demande de LeKiwi06, 27/10/2026).

## 1.0.0 - le Casse-croûte doré (27/10/2026)

- **Recette** (sans forme, à l'établi) : pomme d'or, carotte dorée, pissenlit doré, tranche de pastèque scintillante,
  tournesol, bol.
- **Aliment** : 7 points de nourriture, 5 de saturation ; mangeable même rassasié (choix de LeKiwi06) ; en 1,6 s
  comme un aliment vanilla. Donne pendant **5 minutes** : Absorption II (4 cœurs), Célérité II, Vitesse I (particules
  visibles, comme la pomme d'or). Le bol n'est pas rendu (choix de LeKiwi06). **Pile de 16.**
- **Objet** : nom « Casse-croûte doré » en or, non italique, aspect enchanté, image du ragoût suspect (`item_model`,
  choix de LeKiwi06). Base : **tarte à la citrouille** (un livre de connaissances, base habituelle des objets custom,
  ne peut pas être mangé ; la tarte n'a aucun autre usage vanilla). Marqueur `ks_cassecroute:casse_croute`, qui l'exclut
  aussi des rachats de KS_Economy.
- **Livre de recettes** : la recette est débloquée dès que le joueur obtient un des 6 ingrédients (comme KS_Elixir).
- **Apparence Bedrock** : `geyser-bedrock/` 1.5.0 (nourriture, saturation et pile de 16 déclarées).
- **Autres plugins** : `creerCasseCroute()`, `estCasseCroute(objet)`.

Limites :
- Pas encore dans `/kaliumgive` (non demandé).
- Pour les joueurs Bedrock, la consommation suit les composants déclarés dans `kalium_objets.json` : à garder
  identiques au plugin si les valeurs changent.

**Déployé sur Kixster le 27/10/2026 à 09:12 (LeKiwi06 : « Oui, déploie KS_CasseCroute sur Kixster… ») avec l'entrée
« Casse-croûte doré » du didacticiel (thème Crafts) ; apparence Bedrock (geyser-bedrock 1.5.0) envoyée sur le proxy à la
même heure. Actif après redémarrage de Kixster et du proxy. Statut : non testé en jeu.**
