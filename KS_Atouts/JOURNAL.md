# KS_Atouts - journal

Serveur Kixster. Atouts d'équipement (étape 4 du chantier « atouts, alcools, aliments », demande de LeKiwi06 du
28/10/2026 ; cahier local `Atouts_Alcools_Aliments.md`, partie 3.5, à copier ici au déploiement).
Dépend de KLM_Menu (textes) et de KS_Effets (/effects).

## 1.0.0 - bottes de ski, casque poiscaille, bottes coupe-feu (28/10/2026)

| Atout | Craft (à l'établi) | Apparence pendant l'atout | Effet |
|---|---|---|---|
| Bottes de ski | 8 pieds de lapin autour de bottes | bottes en cuir | marcher sur la poudreuse (le jeu le permet aux bottes en cuir) |
| Casque poiscaille | 8 poissons-globes autour d'un casque | tête de gardien | vision nocturne tant que la tête est sous l'eau |
| Bottes coupe-feu | 8 crèmes de magma autour de bottes | bottes en cuivre | aucun dégât des blocs de magma |

- Toutes les bottes et tous les casques du jeu (carapace de tortue comprise). Chaque craft ajoute **1 heure**,
  décomptée **seulement quand l'objet est porté**, sans plafond. Un autre atout remplace le précédent : son temps est
  perdu.
- **Apparence** : l'objet change vraiment de type et garde ses caractéristiques : modificateurs d'attributs (armure,
  robustesse, résistance au recul), durabilité et usure, réparation avec son matériau d'origine, résistance au feu
  (netherite), enchantabilité, pile de 1, nom de base (« Bottes en diamant »), réglages d'équipement (son, usure quand
  on est touché). Les enchantements, le nom donné et la garniture suivent d'eux-mêmes. Fin de l'atout : il reprend son
  type ; les valeurs recopiées qui venaient de son type sont retirées, l'usure reste.
- **Temps restant** : ligne « Atout : ... (59 min) » dans la description (mise à jour chaque minute portée et à la
  déconnexion) et dans `/effects` à la seconde près (KS_Effets, tant que l'objet est porté). Fin : message en barre
  d'action (`atout.fin` dans `lang.yml`).
- Vision nocturne du casque : 15 s renouvelées chaque seconde sous l'eau (pas de clignotement), retirée en sortant de
  l'eau.
- Dégâts du magma : reconnus par leur type du jeu (`hot_floor`), la « cause » de Bukkit étant dépréciée en 26.2.
- Le livre de recettes montre un aperçu ; le vrai résultat est calculé sur l'équipement posé au centre.

Limites :
- Pendant l'atout, l'objet ne se combine pas à l'enclume avec un objet de son type d'origine (types différents), et une
  tête de gardien n'accepte pas les livres d'enchantement de casque.
- La garniture d'un casque ne se voit pas sur la tête ; les bottes en cuir ont leur couleur par défaut.
- Bedrock : apparence par le vrai type (aucun réglage du pack) ; la tête de gardien portée reste à vérifier.
- Didacticiel et `/kaliumgive` : à faire au déploiement.

Compilé le 28/10/2026 (`sortie/KS_Atouts-1.0.0.jar`, après KLM_Menu et KS_Effets). **Statut : non déployé, non testé
en jeu.**
