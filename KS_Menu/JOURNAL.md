# KS_Menu - journal

Plugin du serveur Event : son menu. Cahier des charges : catégorie 2 « Économie » de LeKiwi06 (validé le 29/09/2026 ;
copié dans `CAHIER_DES_CHARGES.md` de KS_Economy au déploiement).

## 1.2.0 - étoile gardée à la mort (08/10/2026)

Signalé par Maxster33 (tests de Kixster) : « la nether star du /menu on tombe au sol quand on meurt. il ne faut pas
qu'elle tombe au sol ».
- Cause : KS_Menu verrouillait l'étoile (ni jetée, ni déplacée) mais ne la retirait pas des objets lâchés à la mort
  (KLM_Menu ne le fait que pour sa boussole et son comparateur).
- À la mort, l'étoile est retirée des objets lâchés (priorité LOW : avant KS_CoffreMort, qui range les objets dans
  le coffre de mort en HIGHEST) ; elle est rendue à sa case à la réapparition, comme avant.

**Déployé sur Event et Kixster le 08/10/2026 à 21:12 (Maxster33 ; 1.1.0 dans `_removed-ks_menu-1.1.0/`), actif après
redémarrage. Statut : non testé en jeu.**

## 1.1.0 - commande du menu réglable (07/10/2026)

Demande de Maxster33 : préparer les plugins d'Event pour Kixster (nouveau serveur de survie en 26.3) ; sur Kixster, le
menu s'ouvre par `/kixster` (plus `/menu`, l'étoile du Nether et `/menu on | off`, inchangés).

- **Commande** : plus déclarée dans `plugin.yml` ; enregistrée au démarrage (API Paper `LifecycleEvents.COMMANDS`,
  `BasicCommand`) avec le nom lu dans `config.yml` : `commande: event` par défaut (Event inchangé), `kixster` sur
  Kixster. Nom invalide → `/event` et un avertissement dans la console. Redémarrage nécessaire après un changement.
- **`config.yml`** (nouveau, créé au premier démarrage) : `commande`, `hub-item.slot` (4 par défaut, comme avant).
- Textes « Event » (nom de l'étoile, titre du menu) : toujours dans `plugins/KS_Menu/lang.yml`, à changer par serveur.
- Compilé contre paper-api 26.2.build.123 ; vérifié aussi contre 26.3.build.159-beta (0 erreur).

**Déployé sur Event le 07/10/2026 à 11:33 (Maxster33 ; 1.0.0 dans `_removed-ks_menu-1.0.0/`), actif après
redémarrage d'Event (Paper 26.3). Statut : non testé.**

## 1.0.0 - menu d'Event (30/09/2026)

Demande de LeKiwi06 : ouvrir l'Économie par un bouton dans l'étoile du Nether (`/menu on`) ; créer KS_Menu, le menu du
serveur Event (comme KG_Menu sur kal-games, KV_Menu sur Kanvas).

- **Étoile du Nether** (case 5 de la barre, réglable : `hub-item.slot`, 4 par défaut) : clic droit = menu d'Event.
  Déclarée à KLM_Menu (`InterfaceItem`, id `event`) : sur Event (`items-by-default: false`), elle n'est donnée qu'après
  `/menu on` et retirée par `/menu off`, comme les autres objets de menu. Verrouillée : ni déplacée, ni jetée, ni
  échangée de main ; remise à sa case toutes les 2 secondes.
- **Menu** (Dialog de KLM_Menu) : un bouton par plugin d'Event déclaré. Au départ : « Économie » (KS_Economy).
  Aussi ouvert par `/menu` et `/event`.
- **Autres plugins** : `KSMenu.ajouterBouton(plugin, id, nom, description, ordre, ouvrir)` dans leur `onEnable`
  (`softdepend` KS_Menu).
- Textes : `plugins/KS_Menu/lang.yml` (créé au premier démarrage).
- `depend` KLM_Menu ; `build.sh` : compiler KLM_Menu d'abord.

**Déployé sur Event le 01/10/2026 à 00:49 (LeKiwi06) avec KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 et
VaultUnlocked 2.20.2 (nouveau plugin tiers, `plugins/VaultUnlocked-2.20.2.jar`), actifs après redémarrage
d'Event. Statut : non testé en jeu.**

## 1.3.0 - monde de test : boutons de la survie masqués (25/10/2026, LeKiwi06)

**Demande de LeKiwi06 (25/10/2026, après le premier essai du monde de test)** : « je vois encore mon score de monnaie etc. dans le tableau à droite, donc ce n'est pas bien séparé [...] il faut vraiment scinder les 2, mis à part pour les plugins qui doivent marcher sur les 2 mondes, mais en faisant la distinction du moi de Kixster et le moi du mondetest ». Choix validés : « moi de test » (même compte Java, identifiant de test automatique) pour l'économie, les jetons, les maisons, le tableau, les récompenses, le fly et l'exploration ; progrès, recettes et statistiques : « rien ne compte en test ».

Principe (KS_MondeTest 1.1.0) : dans le monde de test, le joueur porte la métadonnée `ks_compte`, identifiant de son compte de test (UUID de version 3 tiré du sien, toujours le même). Classe `CompteTest` (copiée dans chaque plugin concerné, sans dépendance) : `de(uuid)` rend ce compte de test si le joueur est dans le monde de test, sinon son UUID. Les données sont rangées sous cet identifiant : son compte de Kixster n'est jamais touché. Sans KS_MondeTest : rien ne change.

- Dans le monde de test (métadonnée `ks_compte`), les boutons des plugins de `monde-test-masques` (config.yml ; valeur par défaut dans le code : KS_Claim, KS_CoffreMort) ne sont pas affichés. Les autres (Économie, Jetons, Récompenses, Téléportations, Fly) y marchent avec le compte de test.

**À déployer ensemble sur Kixster** : KS_MondeTest 1.1.0, KS_Economy 1.11.0 (avec KS_BiomeChanger 1.2.0, exigé par KS_Economy 1.10.1), KS_Jetons 2.2.0, KS_Teleport 1.5.0, KS_RewardsGUI 1.10.0, KS_FairPlay 1.1.0, KS_Fly 1.1.0, KS_Tableau 1.1.0, KS_Menu 1.3.0, KS_Parrainage 1.2.0, KLM_Statistiques 1.2.0 ; nouveau `config.yml` de KS_MondeTest et clé `espaces-interfaces` de KLM_Menu à changer (voir le JOURNAL de KS_MondeTest). **Déployé sur Kixster le 25/10/2026 à 12:10 (accord de LeKiwi06 : « oui, déploie les 11 plugins du moi de test et KS_BiomeChanger 1.2.0 sur Kixster, avec le nouveau config.yml de KS_MondeTest et espaces-interfaces de KLM_Menu ») ; ancien jar dans `_removed-<plugin>-<version>/` ; actif après redémarrage (fait par LeKiwi06), non testé en jeu.**
