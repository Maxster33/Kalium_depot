# KS_AntiCheat - journal

Plugin du serveur Event : anti-triche. Cahier des charges : catégorie 6 « Anti-triche » de LeKiwi06 (validé le
30/09/2026 ; interface dans la rubrique « Modération » de /menu, précision du 03/10/2026 ; copié dans
`CAHIER_DES_CHARGES.md` au déploiement). Codé en 5 étapes (versions 0.x), déployé à la fin.

## 0.1.0 - étape 1 : alertes, interface staff, suspensions, invsee / ecsee, morts d'entités (03/10/2026)

- **Alertes** : historique (`alertes.yml`, 5 000 dernières), console, message au staff connecté (permission
  `ksanticheat.staff`, opérateurs par défaut ; une annonce par minute au plus pour le même joueur et le même type
  d'alerte légère). Légère : le staff vérifie ; grave : suspension automatique (détections des étapes suivantes).
- **Suspensions** (`suspensions.yml`) : connexion à Event refusée avec « Une erreur inhabituelle est survenue,
  contacte le staff. » ; joueur connecté expulsé avec ce message ; annonce au staff ; levée par le staff.
- **Interface staff** : rubrique « Modération » de `/menu` (KLM_Menu 2.7.0) : « Anti-triche », « Invsee »,
  « EcSee » ; aussi `/anticheat [joueur]`, `/invsee <joueur>`, `/ecsee <joueur>`. Anti-triche : alertes récentes,
  joueurs avec alertes, chercher un joueur (état, alertes, inventaire, coffre de l'Ender, suspendre avec une raison /
  lever la suspension), suspendus, morts d'entités importantes, journal invsee / ecsee.
- **invsee / ecsee** : en ligne : vue reliée à l'inventaire du joueur (inventaire, barre, armure, seconde main ;
  changements recopiés dans les deux sens), coffre de l'Ender ouvert tel quel. Hors ligne : état enregistré à sa
  dernière déconnexion (instantané à chaque déconnexion et toutes les 5 minutes, `instantanes/`) ; les changements
  du staff sont appliqués à sa prochaine connexion (`en-attente/`), avant qu'il puisse jouer. Journal
  `consultations.log` : date, staff, invsee / ecsee, joueur (en ligne / hors ligne), ouverture, retirés / ajoutés.
  Limite : un joueur jamais venu depuis l'installation n'a pas d'instantané (message).
- **Morts d'entités importantes** (`morts.log`, 200 dernières dans l'interface) : villageois, golems, allays, boss
  (wither, dragon, gardien ancien, warden), mobs nommés, animaux apprivoisés (`morts.types`, `morts.nommes`,
  `morts.apprivoises`) : date, entité, nom, propriétaire, lieu, tué par (joueur, mob, cause).
- `depend` KLM_Menu ; `softdepend` GrimAC (étape 2).

**Non déployé (catégorie 6, étape 1). Statut : non testé en jeu.**
