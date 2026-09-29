# KS_KaliumGive - journal

Plugin autonome, serveur Event. Demande de Maxster33 du 28/09/2026.

## 1.0.0 - commande /kaliumgive (28/09/2026)

- **`/kaliumgive <pseudo> <id_custom> <nombre>`** : donne un objet custom du serveur. Opérateurs seulement
  (permission `ks.kaliumgive`, `default: op`). Les 3 arguments sont obligatoires ; nombre de 1 à 64 (limite ajoutée
  contre les fautes de frappe) ; un objet à la fois, inventaire plein : le reste tombe au sol. Complétion : pseudos en
  ligne, puis id_custom. id inconnu : le message donne la liste.
- **Liste des id_custom** (dans le code, `OBJETS`) :
  - `estomac_gardien` : Estomac du gardien (créé par KS_EstomacGardien, `creerEstomac()`).
- Chaque objet est créé par le plugin qui le définit (`softdepend`) : toujours identique à celui du jeu ; si ce plugin
  n'est pas activé, l'id est refusé. `build.sh` compile contre les classes de KS_EstomacGardien : **compiler
  KS_EstomacGardien d'abord**.
- Ajouter un objet : une ligne dans `OBJETS`, son plugin dans `softdepend` (`plugin.yml`) et dans le classpath de
  `build.sh`.

**Déployé sur Event le 28/09/2026 à 23:35. Statut : non testé en jeu.**

## 1.1.0 - id cle_de_l_end (28/09/2026)

Demande de Maxster33 : **`cle_de_l_end`** ajouté à la liste (Clé de l'End, créée par KS_EC_Extension,
`KSECExtension.creerCle()` ; `softdepend` et `build.sh` : compiler KS_EC_Extension d'abord).

**Déployé sur Event le 28/09/2026 à 23:47 (1.0.0 dans `_removed-ks_kaliumgive-1.0.0/`). Statut : non testé en jeu.**

## 1.2.0 - id fiole_exp(<points>) (29/09/2026)

Demande de Maxster33 : **`fiole_exp(<points>)`**, ex. `/kaliumgive Maxster33 fiole_exp(1395) 1` donne une
« Fiole d'expérience (1 395 XP) » (créée par KS_FioleExp, `KSFioleExp.creerFiole(points)` ; 1 à 999 999 999 points).
Complétion : `fiole_exp(` ; le message d'id inconnu cite `fiole_exp(<points>)`. `softdepend` et `build.sh` :
compiler KS_FioleExp d'abord. Déployé avec KS_FioleExp 1.0.0 (nécessaire pour cet id).

**Déployé sur Event le 29/09/2026 à 03:39 (1.1.0 dans `_removed-ks_kaliumgive-1.1.0/`). Statut : testé et confirmé par Maxster33 le 29/09/2026.**
