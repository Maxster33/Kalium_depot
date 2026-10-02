# KS_BiomeChanger - journal

Serveur Event. Le Changeur de Biome (demande de Maxster33, 29/09/2026). Dépend de KLM_Menu (boîte à outils des menus).

## 1.0.0 - Changeur de Biome, menu, historique (29/09/2026)

Demande de Maxster33 :
- objet « Changeur de Biome » avec l'apparence du cristal de l'End ;
- clic droit avec l'objet en main : menu pour choisir le nouveau biome (biomes de l'overworld seulement, utilisable
  seulement dans l'overworld) ;
- biome changé dans une sphère de 32 blocs de rayon (ou un cube de même volume) autour du joueur, aucun bloc ne bouge ;
- à la validation : revérifier que le joueur a encore un Changeur de Biome, le consommer, message « Vous avez changé
  le biome pour : <nom du biome> » ;
- historique de chaque changement ; bouton « Historique » dans le menu : un joueur voit ses derniers changements, un
  opérateur voit tous les changements et s'y téléporte d'un clic.

Fait :
- **Objet** : livre de connaissances (ni bloc, ni ingrédient vanilla), `item_model` `minecraft:end_crystal`, nommé
  « Changeur de Biome », empilable par 64, marqué ; clic droit vanilla annulé. `creerChangeur()` (KS_Crafts 1.6.0,
  KS_KaliumGive 1.5.0). Apparence Bedrock : `geyser-bedrock/` 1.2.0.
- **Menu** : menus natifs de Minecraft par la boîte à outils de KLM_Menu (affichés aussi sur Bedrock par Geyser, couleurs
  adaptées). Bouton « Historique », puis un bouton par biome du tag `#minecraft:is_overworld` (noms dans la langue du
  joueur, triés par identifiant), sur 2 colonnes. Hors de l'overworld : message « Le Changeur de Biome ne s'utilise que
  dans l'overworld. ». Un seul menu par seconde (clics en double).
- **Forme** (demande de Maxster33 : au choix du joueur) : bouton « Forme : sphère (passer au cube) » en haut du menu ;
  **sphère** de 32 blocs de rayon ou **cube** de même volume (52 blocs de côté : 32 x racine cubique de 4 pi / 3 = 51,6),
  centrés sur le joueur. Choix retenu pour chaque joueur jusqu'au redémarrage (sphère par défaut).
- **Zones protégées** (demande de Maxster33) : si WorldGuard est activé (`softdepend`), refus dès que la zone modifiée
  touche une région WorldGuard (hors région globale), **même si le joueur en est membre ou propriétaire** (choix de
  Maxster33) : « Impossible : la zone touche une zone protégée. Ton Changeur de Biome n'a pas été utilisé. » Vérifié
  cellule par cellule (une sphère ne touche pas les régions proches des coins de sa boîte). Compilé contre l'API
  WorldGuard 7.0.19 / WorldEdit 7.4.5 (`telecharger-outils.sh`, versions d'Event).
- **Changement** (au clic sur un biome) : revérifie joueur en ligne, overworld, Changeur de Biome dans l'inventaire
  (celui de la main en priorité, sinon le premier trouvé), zones protégées, puis le consomme ; zone centrée sur la
  position du joueur au moment de la validation ; seul le biome change, par cellules de 4 x 4 x 4 blocs (résolution
  des biomes dans le jeu : bord en escalier), dans la hauteur du monde ; les chunks touchés sont renvoyés aux joueurs
  (couleurs de l'herbe, du feuillage, de l'eau). Message vert « Vous avez changé le biome pour : » + nom du biome (doré).
- **Historique** (`plugins/KS_BiomeChanger/historique.yml`, enregistré à chaque changement) : date, joueur, monde,
  position, biome, forme. Menu « Historique des changements de biome », 10 par page, du plus récent au plus ancien :
  joueur = ses changements (texte) ; opérateur = tous, un bouton par changement (« 29/09 14:30 · pseudo · biome ») qui le
  téléporte à la position enregistrée (opérateur revérifié au clic). Bouton « Retour » vers le choix du biome.

Limites :
- Un opérateur n'accède à l'historique que par le menu, donc avec un Changeur de Biome en main (ouvrir le menu ne le
  consomme pas).
- Les biomes des grottes (grottes luxuriantes, deep dark...) font partie des biomes de l'overworld proposés.

**Déployé sur Event le 29/09/2026 à 17:22 avec KS_ItemSimple, KS_Spawners, KS_BiomeChanger, KS_Crafts 1.6.0, KS_KaliumGive 1.5.0 et KS_LootBlocs 1.1.0 (apparence Bedrock : geyser-bedrock 1.2.0 sur le proxy à 17:23), actifs après redémarrage d'Event et du proxy. Statut : non testé en jeu.**

## 1.1.0 - claims : propriétaire et membres seulement (02/10/2026, LeKiwi06)

Cahier des charges catégorie 3 « Claims » : le Changeur de Biome est refusé si la zone modifiée touche un claim de
SimpleClaimSystem dont le joueur n'est ni le propriétaire ni un membre (message : « la zone touche le claim d'un autre
joueur », objet non consommé). Vérification chunk par chunk de la zone (classe `ProtectionClaims`, chargée seulement si
SimpleClaimSystem est activé ; `softdepend` SimpleClaimSystem). Les régions WorldGuard restent refusées comme avant.

**Déployé sur Event le 02/10/2026 à 22:26 (LeKiwi06) avec SimpleClaimSystem 1.13.1 (nouveau, `config.yml` et
`langs/fr_FR.yml` posés avant le premier démarrage), KS_Claim 1.0.0, KS_BiomeChanger 1.1.0 (1.0.0 dans `_removed-ks_biomechanger-1.0.0/`) ; actifs après
redémarrage d'Event ; puis commandes LuckPerms et région de l'île du dragon (humain, voir `KS_Claim/scs/README.md`).
Statut : non testé en jeu.**

## 1.1.1 - API de SimpleClaimSystem initialisée (03/10/2026, LeKiwi06)

Même défaut que celui signalé par LeKiwi06 sur KS_Claim : le Changeur de Biome plantait en vérifiant les claims (console : « API not initialized. Call initialize() first. »).
SimpleClaimSystem 1.13.1 ne crée pas son API lui-même : le plugin l'initialise maintenant avant chaque utilisation
(`SimpleClaimSystemAPI_Provider.initialize`, sans effet si c'est déjà fait). Aucun autre changement.

**Non déployé. Statut : non testé en jeu.**
