# KS_LaisseBateau - journal

Plugin de la survie (Kixster) : laisse courte entre deux bateaux. Aucune dépendance.

## 1.0.0 - laisse de 3 blocs entre deux bateaux (14/10/2026, LeKiwi06)

Demande de LeKiwi06 : « tu peux réduire la longueur de la laisse si deux bateaux sont accrochés l'un à l'autre
aussi ? »

Lu dans le code du jeu 26.3 (`Leashable`) : une laisse ne tire qu'au-delà de **6 blocs** (comme un élastique) et casse
à **12 blocs** ; rien de propre aux bateaux, et ces valeurs ne se règlent pas.

Ses réponses aux questions posées avant le code :

| Question | Réponse de LeKiwi06 |
|---|---|
| Longueur | 3 blocs de centre à centre (environ 1,5 bloc d'eau entre deux bateaux) |
| Bateaux concernés | Tout bateau tiré par un bateau (avec ou sans coffre, à jukebox ou non) ; laisse tenue par un joueur ou une barrière inchangée |
| Serveurs | Kixster seul |

- Chaque seconde, le plugin relève les bateaux et radeaux tenus en laisse par un autre bateau. À chaque tick, un tel
  bateau plus loin que `longueur` blocs (à l'horizontale) de celui qui le tracte prend la vitesse de ce dernier
  (mesurée d'un tick à l'autre), plus un rappel vers lui proportionnel à l'écart (`raideur`, 0,3 de l'écart par
  tick). En deçà de la longueur, le jeu fait comme d'habitude (le bateau glisse et ralentit). La corde s'affiche
  entre les deux bateaux : elle paraît plus courte.
- `config.yml` : `longueur` (3.0), `raideur` (0.3), valeurs par défaut aussi dans le code (réglables sans recompiler,
  rechargées au redémarrage).

Techniquement nécessaire, signalé :

- Un bateau remorqué **conduit par un joueur** n'est pas tiré : son mouvement est calculé par le jeu de ce joueur, le
  serveur ne peut pas le corriger. Le bateau à coffre et jukebox (personne n'y monte) et un bateau vide sont bien
  tirés.
- Vitesse donnée au bateau remorqué bornée à 1,5 bloc par tick (contre un saut de position du bateau qui tracte).
- Nouveau plugin plutôt qu'un ajout à KS_JukeboxBoat (« un plugin = un rôle » : la laisse vaut pour tous les bateaux) ;
  nom choisi par Claude.

Limites connues : un arrêt brusque du bateau qui tracte laisse le bateau remorqué glisser jusqu'à le cogner (comme
avec la laisse du jeu). Valeurs écrites sans test en jeu : `longueur` et `raideur` sont à ajuster dans `config.yml`
si le remorquage paraît trop mou ou trop sec.

Écrit sans serveur de test. Compilé le 14/10/2026 (`sortie/KS_LaisseBateau-1.0.0.jar`). **Statut : non déployé, non
testé en jeu.**

**Déployé sur Event le 16/10/2026 à 20:00 (LeKiwi06 : « envoie tout sur event je vais faire les tests là-bas » ; envoi groupé : KS_VerrouVehicule 1.0.0, KS_LaisseBateau 1.0.0, KS_Trains 1.0.0, KS_Crafts 1.16.0 ; KS_Crafts 1.14.1 en place vérifié identique à la référence avant l'envoi, dans `_removed-ks_crafts-1.14.1/` ; jars du serveur vérifiés identiques à ceux compilés après l'envoi), actif après redémarrage d'Event. Prévu pour Kixster : testé d'abord sur Event à la demande de LeKiwi06 ; Kixster n'est pas touché. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 16/10/2026 à 20:50 (journal lu, heures du journal en UTC : 18:50) : KS_Trains 1.2.0, KS_Crafts 1.17.0 (« 81 crafts ajoutés » : les 78 d'avant + verrou, locomotive et wagon contrôleur), KS_VerrouVehicule 1.1.0 et KS_LaisseBateau 1.0.0 chargés et activés sans erreur ni avertissement ; KS_Trains activé après KS_Claim (ordre voulu pour la pose de la locomotive) ; `config.yml` et `lang.yml` créés. Statut : non testé en jeu.**

## 1.0.1 - le bateau remorqué suit comme une remorque (18/10/2026, LeKiwi06)

Retour de LeKiwi06 sur Event : « elles font la bonne distance, mais le bateau de derrière ne change jamais
d'orientation ; d'habitude ça ressemble davantage à une remorque qui nous suit et pas à un boulet qu'on traîne ».

- **Cause** : le jeu ne tourne un bateau que s'il est conduit (lu dans le code du jeu 26.3 : `AbstractBoat.controlBoat`
  seul change le cap) ; le plugin ne faisait que le tirer.
- **Correction** : à chaque tick, l'avant du bateau remorqué est tourné vers le bateau qui le tracte, comme une
  remorque vers son attelage (aussi quand la laisse est détendue ; pas quand les deux bateaux se touchent presque).
- Bateau remorqué conduit par un joueur : toujours calculé par son jeu, inchangé.

Compilé le 18/10/2026 (`sortie/KS_LaisseBateau-1.0.1.jar`). **Statut : non déployé, non testé en jeu.**

**Déployé sur Event le 18/10/2026 à 10:33 (LeKiwi06 : corriger la laisse « avant de l'envoyer sur Event » ; envoi groupé KS_Trains 1.4.8 et KS_LaisseBateau 1.0.1) : jar en place (1.0.0) vérifié identique à la référence, rangé dans `_removed-ks_laissebateau-1.0.0/` ; jar du serveur vérifié identique à celui compilé. À redémarrer par LeKiwi06. Statut : non testé en jeu.**

**Actif sur Event depuis son redémarrage du 18/10/2026 à 13:34 (journal lu à 14:16, heures en UTC : 11:34) : KS_LaisseBateau 1.0.1 activé sans erreur ni avertissement ; aucune erreur jusqu'à la lecture (LeKiwi06 connecté depuis 13:47). Statut : non testé en jeu.**

## 1.0.2 - remorque à timon rigide (18/10/2026, LeKiwi06)

Retour de LeKiwi06 sur Event : « le bateau tracté ne suit pas correctement le bateau tracteur, mais je ne sais pas
comment décrire le problème, peux-tu regarder toi-même ? ».

- **Cause** (lue dans le code, Claude ne voit pas le jeu) : le bateau remorqué prenait la vitesse du tracteur, plus un
  rappel élastique au-delà de 3 blocs ; dans un virage il glissait de côté, parallèlement au tracteur, au lieu de suivre
  sa trace ; quand le tracteur ralentissait, il continuait sur son élan.
- Capture de LeKiwi06 (1.0.1 ; A tire B qui tire C) : « le bateau B semble être tracté à la perpendiculaire du A, et
  le bateau C suit le bateau B de façon rigide, comme si la laisse était solide » : B gardait la vitesse de A, donc
  restait à côté de lui après un virage.
- **Correction** : laisse tendue : à chaque tick, le bateau remorqué est placé sur la droite qui le relie à l'arrière
  du tracteur (l'attelage, à 0,7 bloc derrière son centre, là où il sera à la fin du tick), à `longueur` blocs de
  centre à centre quand ils sont alignés ; son avant pointe vers l'attelage. Il suit la trace et coupe les virages par
  l'intérieur comme une remorque. Laisse détendue (le tracteur ralentit, recule ou se rapproche) : ni poussé ni tourné,
  seulement freiné (x0,7 par tick) pour ne pas venir cogner le tracteur. Le frein du jeu sur l'eau (x0,9, appliqué
  avant le déplacement, lu dans `AbstractBoat.tick`) est compensé.
- Simulation de la capture (A, B, C ; B parti de travers ; A à 8 blocs/s, virage à 90° en 2 s, puis arrêt) : B passe
  derrière A en moins de 1,5 seconde ; 3,00 blocs entre chaque bateau en ligne droite, 2,98 dans le virage (B 17°
  derrière A, C 17° derrière B) ; à l'arrêt, B glisse jusqu'à 1,9 bloc de A (centre à centre) sans le toucher.
- `config.yml` : clé `raideur` retirée (elle ne sert plus) ; `longueur` inchangée.

Compilé le 18/10/2026 (`sortie/KS_LaisseBateau-1.0.2.jar`). **Statut : non déployé, non testé en jeu.**
