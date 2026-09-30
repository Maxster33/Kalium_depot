# KS_Menu - journal

Plugin du serveur Event : son menu. Cahier des charges : catégorie 2 « Économie » de LeKiwi06 (validé le 29/09/2026 ;
copié dans `CAHIER_DES_CHARGES.md` de KS_Economy au déploiement).

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

**Non déployé. À déployer ensemble (règle 3.5) : KS_Menu 1.0.0, KS_Economy 1.0.0, KS_Elixir 1.1.0 et l'installation de
VaultUnlocked 2.20.2 sur Event. Statut : non testé en jeu.**
