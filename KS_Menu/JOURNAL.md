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

**Statut : compilé, non déployé, non testé en jeu.**

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
