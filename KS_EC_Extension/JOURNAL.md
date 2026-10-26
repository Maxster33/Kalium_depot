# KS_EC_Extension - journal

Plugin autonome (réutilisable ailleurs), serveur Event. Demande de Maxster33 du 28/09/2026.

## 1.0.0 - coffre de l'Ender doublé, Clé de l'End (28/09/2026)

Demande : « Changer l'interface de l'enderchest pour doubler sa capacité (comme les doubles coffres) ; bloquer les 3
lignes du bas avec des images de bloc invisible ; pouvoir débloquer les cases grâce à un objet, idéalement en
déposant l'objet dessus. L'objet : Clé de l'End (image de la clé des épreuves funeste). » Réponses : 1 clé = 1 case,
image de la barrière, clé empilable par 64, craft sans forme (dans KS_Crafts 1.1.0).

- **Interface** : à l'ouverture de son propre coffre de l'Ender, le joueur voit 6 lignes (titre vanilla « Coffre de
  l'Ender »). Les 3 lignes du haut sont le coffre de l'Ender vanilla (contenu inchangé) ; les 3 du bas sont
  l'extension.
- **Cases bloquées** : image de barrière nommée « Case bloquée » (texte : « Dépose une Clé de l'End ici pour la
  débloquer. »), impossibles à prendre ou remplir.
- **Déblocage** : clic gauche ou droit avec une Clé de l'End sur une case bloquée (ou petit glisser sur une seule
  case) : la case est débloquée pour toujours, une clé est consommée.
- **Stockage** : l'extension et les cases débloquées sont dans les données du joueur (PersistentDataContainer, fichier
  playerdata) ; le haut reste le coffre de l'Ender vanilla. Contenu copié à l'ouverture, réécrit à la fermeture (et à
  l'arrêt du plugin).
- **Clé de l'End** : livre de connaissances (aucun craft, pas un bloc) avec l'image de la clé des épreuves sinistre
  (`item_model` = `minecraft:ominous_trial_key`), donc sans effet sur les coffres-forts ; empilable par 64 ; clic droit
  vanilla annulé. Marquée `ks_ec_extension:cle_de_l_end`. Pour les autres plugins : `KSECExtension.creerCle()`.
- Limites : le couvercle du coffre ne s'anime pas à l'ouverture (l'interface vanilla est remplacée) ; un plugin qui
  ouvre le coffre de l'Ender d'un autre joueur ne montre que les 3 lignes vanilla ; en cas de plantage du serveur
  pendant que le coffre est ouvert, les changements faits depuis l'ouverture sont perdus (pas de duplication).

**Déployé sur Event le 28/09/2026 à 23:47. Statut : non testé en jeu.**

## 1.1.0 - fonctions pour l'ecsee de l'anti-triche (03/10/2026, LeKiwi06)

Correctif de duplication par ecsee (KS_AntiCheat 1.1.1) : `coffreOuvert`, `fermerCoffre` (copie enregistrée puis
fermée), `extension`, `casesDebloquees`, `ecrireExtension`, `imageCaseBloquee`. Aucun changement pour les joueurs.

**Déployé sur Event le 03/10/2026 à 18:42 (LeKiwi06, en 1.1.0 ; ancienne version dans `_removed-ks_ec_extension-1.0.0/`), actif après redémarrage d'Event. Statut : non testé en jeu.**

## 1.2.0 - coffre de l'Ender animé, interface fermée si le bloc disparaît (26/10/2026, LeKiwi06)

Demande de LeKiwi06 : « quand on ouvre le coffre de l'end il ne s'ouvre pas visuellement , et même si un joueur casse
l'ender chest qui a servis a ouvrir l'interface , elle ne se ferme pas , règle ça ».
- Cause : l'ouverture vanilla est annulée pour montrer l'interface à 6 lignes du plugin, donc le jeu n'animait plus le
  bloc et ne surveillait plus sa présence.
- **Animation** : le couvercle s'ouvre avec le son vanilla tant qu'au moins un joueur a l'interface ouverte depuis ce
  bloc, et se referme (avec son) quand le dernier la ferme.
- **Fermeture** : l'interface se ferme (contenu enregistré comme à une fermeture normale) si le bloc n'est plus un
  coffre de l'Ender (cassé, remplacé) ou si le joueur s'en éloigne de plus de 9 blocs. Vérifié toutes les 0,5 s, et
  aussitôt après qu'un joueur casse un coffre de l'Ender ouvert.
- Bloc reconnu par le jeu (coffre ouvert depuis un bloc), sinon par le dernier coffre de l'Ender cliqué. Un coffre
  ouvert sans bloc (commande d'un autre plugin) n'est ni animé ni surveillé, comme avant.

**Compilé, non déployé, non testé en jeu.**
