# KS_BedrockBreaker - journal

Plugin autonome, serveur Event. Le Bedrock Breaker, sorti de KS_Crafts (qui garde la recette) à la demande de
Maxster33 du 29/09/2026.

## 1.0.0 - Bedrock Breaker (29/09/2026)

Demande de Maxster33 : recette toujours dans KS_Crafts, avec une houe en diamant à la place de la houe en bois ;
apparence du bâton de blaze sur Java et Bedrock ; id `bedrock_breaker` dans KS_KaliumGive ; nouveau plugin pour le
faire fonctionner ; un seul bloc de bedrock par objet, consommé ; toute bedrock sauf la couche du fond (pas de chute
dans le vide) ; protections WorldGuard respectées ; recette dans le livre de recettes dès qu'on obtient une TNT ou une
houe en diamant (KS_Crafts 1.4.0) ; description « Utilisation unique ». Réponses de Maxster33 : toute houe en diamant
acceptée dans la recette (même renommée, enchantée ou abîmée) ; les anciens Bedrock Breaker marchent encore ;
empilable par 64 ; la couche du haut du plafond du Nether peut être cassée.

- **Objet** (`KSBedrockBreaker.creerBreaker()`, utilisé par KS_Crafts et KS_KaliumGive) : livre de connaissances
  (ni outil, ni bloc, ni ingrédient) nommé « Bedrock Breaker », description « Utilisation unique » (gris),
  `item_model` `minecraft:blaze_rod` (image du bâton de blaze ; sur Bedrock : `geyser-bedrock/` 1.1.0), empilable par
  64, marqué `ks_bedrockbreaker:bedrock_breaker`. Son clic droit vanilla (livre de connaissances consommé) est annulé.
- **Utilisation** : clic droit (main principale ou secondaire) sur un bloc de bedrock : le bloc disparaît, sans drop,
  et un Bedrock Breaker est retiré de la pile.
- **Refusé** :
  - couche du fond de la dimension (hauteur minimale du monde : y = -64 dans le monde normal, y = 0 dans le Nether) :
    message « La couche du fond ne peut pas être cassée. » en barre d'action ; les couches au-dessus peuvent être
    cassées (la couche du fond reste toujours pleine) ;
  - protections : un cassage de bloc est simulé (`BlockBreakEvent`) avant de retirer la bedrock ; WorldGuard (ou tout
    autre plugin de protection) l'annule dans une région où le joueur ne peut pas casser, et affiche son propre
    message ; le Bedrock Breaker n'est alors pas consommé ;
  - mode aventure (ajouté, nécessaire pour respecter les protections : ce mode interdit de casser des blocs).
- **Anciens Bedrock Breaker** (houe en bois marquée `ks_crafts:bedrock_breaker`, KS_Crafts 1.0.0 à 1.3.0) : marchent
  encore, avec les mêmes règles ; leur clic droit ne laboure plus la terre.

Limites :
- La bedrock des structures de l'End (portail de sortie, portails d'accès) peut être cassée (« toute bedrock »).
- Le cassage simulé est aussi vu par les autres plugins qui écoutent les cassages de blocs (aucun sur Event ne traite
  la bedrock).

**Déployé sur Event le 29/09/2026 à 12:55 (avec KS_Crafts 1.4.0 et KS_KaliumGive 1.3.0 ; apparence Bedrock : geyser-bedrock 1.1.0 sur le proxy à 12:56), actif après redémarrage d'Event et du proxy. Statut : testé par Maxster33 le 29/09/2026 : la bedrock est bien cassée, mais un clic répété cassait aussi le bloc juste derrière (corrigé en 1.0.1).**

## 1.0.1 - un bloc par seconde (29/09/2026)

Signalé par Maxster33 : en cassant un bloc de bedrock avec un autre juste derrière, les deux blocs étaient cassés et
deux Bedrock Breaker consommés. Cause probable : le clic droit maintenu est répété par le jeu Java (toutes les 0,2 s
environ) et un clic Bedrock peut arriver en double ; le 2e clic touchait le bloc qui venait d'apparaître derrière.
Demande : limiter l'utilisation à 1 par seconde.
- Après un bloc cassé, le même joueur ne peut plus en casser pendant 1 seconde : le clic est ignoré (aucun message,
  aucun Bedrock Breaker consommé). Seuls les cassages réussis déclenchent le délai.

**Déployé sur Event le 29/09/2026 à 13:30 (1.0.0 dans `_removed-ks_bedrockbreaker-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.0.2 - couche du fond cassable (29/09/2026)

Demande de Maxster33 : « enlève l'interdiction de casser la couche du fond ». Toute bedrock peut être cassée, y compris
la couche du fond de chaque dimension (y = -64 dans le monde normal, y = 0 dans le Nether) : un joueur peut donc ouvrir
un passage vers le vide. Message « La couche du fond ne peut pas être cassée. » retiré. Le reste est inchangé
(protections WorldGuard, mode aventure, un bloc par seconde).

**Déployé sur Event le 29/09/2026 à 13:59 (1.0.1 dans `_removed-ks_bedrockbreaker-1.0.1/`), actif après redémarrage d'Event. Statut : non testé en jeu.**
