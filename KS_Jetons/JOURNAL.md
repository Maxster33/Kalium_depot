# KS_Jetons - journal

Plugin du serveur Event : jetons. Cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort » de
LeKiwi06 (03/10/2026, en cours de rédaction ; copié dans `CAHIER_DES_CHARGES.md` au déploiement).

## 1.0.0 - jetons et inventaire de jetons (03/10/2026)

- **Jetons** : de téléportation (apparence : œil de l'Ender), d'emplacement (carte), de claim (pelle en or), de la
  mort (crâne de squelette) ; livre de connaissances inutilisable, brillant, empilable par 64, marqueur
  `ks_jetons:jeton`.
- **Inventaire de jetons** (bouton « Jetons » du menu d'Event, `/jetons`) : nombre de chaque jeton ; « Déposer des
  jetons » (coffre : les jetons y entrent, le reste est rendu) ; « Retirer » (1 à 640, en objets) ; « Acheter » avec
  le score (prix `prix.<jeton>` du `config.yml`, **0 = pas encore en vente** : prix à fixer par LeKiwi06, repère
  « ± 15 minutes de farm pour une TP »). `jetons.yml`.
- API pour les autres plugins (KS_Teleport, KS_CoffreMort, KS_Claim, KS_KaliumGive) : `creer`, `type`, `nombre`,
  `consommer`, `ajouter`, `prix`.
- `depend` KLM_Menu ; `softdepend` KS_Menu, KS_Economy. Joueurs Bedrock : apparence à ajouter dans `geyser-bedrock`.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.0.0 ; nouveau), actif après redémarrage d'Event. Statut : non testé en jeu.**
