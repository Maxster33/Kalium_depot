# KS_Enclume - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Cahier des charges : `KS_Event/CAHIER_DES_CHARGES.md`.

## 1.0.0 - enclume sans plafond de prix (25/09/2026)

Demande de Maxster33 : réparer ou fusionner sans « Trop cher ! », coût calculé comme en vanilla.
- Le plafond (`maximumRepairCost`, 40 niveaux en vanilla) est levé à l'ouverture de l'enclume et à chaque calcul
  (`InventoryOpenEvent`, `PrepareAnvilEvent`). Le coût et la pénalité des réparations successives restent vanilla ;
  le joueur doit toujours avoir assez de niveaux.
- Limite possible : le client Minecraft peut encore afficher « Trop cher ! » au-delà de 40 niveaux (affichage géré par
  le jeu du joueur) alors que le serveur accepte ; à vérifier en jeu.

**Déployé sur Event le 25/09/2026. Statut : non testé en jeu.**

## 1.1.0 - plus de « Trop cher ! » à l'écran (29/09/2026)

Demande de Maxster33 : au-delà de 40 niveaux, l'enclume affichait « Trop cher ! » (fioles de KS_FioleExp et
équipement), même si la prise était acceptée. Ce texte est affiché par le jeu du joueur dès 40 niveaux (le serveur ne
peut pas le changer). Choix de Maxster33 : 39 affiché + vrai coût écrit.
- Au-delà de 39 niveaux, le coût envoyé au joueur est plafonné à 39 (`PrepareAnvilEvent`, priorité HIGHEST, après
  les autres plugins) et le vrai coût s'écrit dans la barre d'action : « Coût réel : 55 niveaux » (vert si le joueur
  a assez de niveaux, rouge sinon).
- À la prise : refusée si le joueur n'a pas le vrai coût (message en barre d'action) ; sinon l'enclume vanilla retire
  les 39 niveaux et le plugin retire le reste au tick suivant, seulement si la prise a bien eu lieu (niveau du joueur
  baissé d'au moins 39). Les niveaux sont retirés comme en vanilla (progression dans le niveau gardée). En créatif :
  rien n'est retiré, comme en vanilla.
- Prises gérées par un autre plugin (clic annulé, ex. KS_FioleExp qui retire lui-même les points) : ignorées ; seul
  l'affichage est plafonné.

**Déployé sur Event le 29/09/2026 à 03:54 (1.0.0 dans `_removed-ks_enclume-1.0.0/`). Statut : non testé en jeu.**

## 1.1.1 - correctif : croix rouge au-delà de 39 niveaux (29/09/2026)

Signalé par Maxster33 (Java) : avec 1.1.0, au-delà de 39 niveaux l'enclume montrait une croix rouge (pas de résultat).
Cause : le jeu du joueur recalcule lui-même le résultat quand les cases d'entrée changent et le vide dès 40 niveaux
(valeur fixée dans le jeu) ; le résultat existait côté serveur (en 1.0.0 aussi : un clic sur la case vide le donnait).
Correctif : quand le coût est plafonné, le contenu de l'enclume est renvoyé au joueur au tick suivant
(`updateInventory`) : son jeu recalcule les cases d'entrée puis reçoit le résultat et le coût (39) du serveur.

**Non déployé. Statut : non testé en jeu.**
