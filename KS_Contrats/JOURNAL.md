# KS_Contrats - journal

Plugin de Kixster : `/contrat`, contrats entre joueurs. Cahier des charges : demande de LeKiwi06 du 27/10/2026,
gardé en local (`Cahiers_KaLium/KS_Contrats.md`), copié dans `CAHIER_DES_CHARGES.md` au déploiement.

## 1.0.0 - contrats, garanties, litiges, livraisons par /echange (27/10/2026, LeKiwi06)

Demande : « peux-tu créer le /contrat sur Kixster (il sert à avoir une trace et qu'un admin puisse trancher en cas de
non-respect de l'engagement) ». Maquette des menus et choix validés le 27/10/2026 (garantie par partie divisée entre
ses joueurs, « valider ou contester » à l'expiration, montant libre pour l'admin, bouton du menu de Kixster, permission
`kscontrats.admin`, bien ou service, une fois ou récurrent, /echange limité au contrat, 7 jours sans réponse = validé,
5 objets différents au plus par bien, périodes à compter de l'acceptation).

**Menus (coffres)** :
- `/contrat` (ou bouton « Contrats » du menu de Kixster, KS_Menu, ordre 15) : « Mes contrats » (actifs, arrivés à leur
  terme, en litige ; favoris en tête), « Créer un contrat », « Propositions (n) », « Terminés », « Litiges » (admins).
- Créer : une case par étape : 1. mon équipe (facultatif), 2. l'autre partie (têtes des joueurs en ligne du même monde,
  ou « Autre pseudo » d'un joueur déjà venu sur Kixster), 3. mon engagement, 4. leur engagement (bien ou service),
  5. durée (une fois avec expiration facultative, ou chaque jour / semaine / 2 semaines / mois avec fin facultative ;
  comptée à l'acceptation), 6. garantie (par partie), « Envoyer ». Brouillon gardé en mémoire (perdu au redémarrage).
- Bien : jusqu'à 5 objets (clic sur un objet de son inventaire = cet objet précis, usure non comptée ; ou nom écrit en
  français = n'importe quel objet de ce type, recherche de KS_Economy) avec leur quantité, des points, une précision.
  Service : texte libre (400 caractères).
- Propositions : ce qui attend ma réponse (papier brillant), mes demandes en attente des autres (carte), les périodes
  à valider (horloge). Fiche d'une demande : lire (nouvelle version et version en vigueur), Accepter, Refuser,
  « Proposer une modification » (contre-proposition, remplace la demande), « Retirer ma demande » pour son auteur.
- Fiche d'un contrat : lire, Renommer, Changer le logo (clic sur un objet de son inventaire), Favori (réglages
  personnels) ; Modifier, Valider en avance, Annuler (demandes) ; Signaler un non-respect ; période à valider ;
  versions et historique.
- Admin (`/contrat admin`) : contrats en litige, contrats d'un joueur (et de son moi de test), lecture, historique,
  « Trancher » : montant libre par joueur, raccourcis « Tout rendre », « Tout à la partie A / B », total qui doit
  tomber juste, confirmation.

**Règles** :
- Toute demande (nouveau contrat, modification, validation anticipée, annulation) prend effet quand tous les autres
  participants l'ont acceptée (coéquipiers compris) ; un refus la fait tomber pour tout le monde ; une demande à la
  fois par contrat ; les joueurs d'un contrat ne changent pas (modification = termes seulement).
- Garantie : montant par partie divisé entre ses joueurs (le point de plus aux premiers listés) ; chacun paie sa part
  en acceptant (l'auteur à l'envoi) ; rendue au refus, au retrait, à la validation, à l'annulation, à la fin validée.
  Modification qui augmente la garantie : la différence est retenue à l'acceptation (rendue si la demande tombe) ;
  qui la baisse : rendue quand elle prend effet.
- Fin d'une période (récurrent) ou expiration : validée d'office si tous les biens sont livrés par /echange et sans
  service ; sinon chacun reçoit « valider ou contester » (7 jours sans réponse = validé). Contrat arrivé à son terme :
  garanties rendues quand la fin est validée. Contester ou signaler : litige (garanties bloquées, demandes coupées,
  périodes en pause, admins prévenus) jusqu'à la décision d'un admin.
- Messages : tout de suite aux joueurs en ligne, sinon à la connexion ; à la connexion, « Contrats : n demandent ta
  réponse » (cliquable), et le nombre de litiges pour les admins.
- Limites : 10 joueurs par partie, 30 contrats ouverts (actifs et proposés) par joueur.
- `/echange` (KS_Economy 1.12.0) : service `ContratsEchange` ; quantités livrées par partie et par ligne
  (« 10/64 cette semaine »), remises à 0 à chaque période, livraisons dans l'historique.
- Monde de test : les comptes du « moi de test » (`CompteTest`) ; on ne choisit que des joueurs du même monde ; les
  garanties passent par `KSEconomy.debiterCompte` / `crediterCompte` (compte exact, même si le joueur a changé de monde).
- Données : `plugins/KS_Contrats/contrats.yml` (contrats, réglages personnels, messages en attente), enregistré à
  chaque changement (au plus toutes les 30 s) et à l'arrêt. Rien n'est effacé. Textes : `lang.yml`.
- `depend` KLM_Menu, KS_Economy ; `softdepend` KS_Menu, KS_MondeTest. `build.sh` compile KS_Menu et KS_Economy d'abord.

Limites :
- Un contrat « une fois » sans date d'expiration ne se termine que par une validation anticipée ou une annulation
  acceptée (ou un litige tranché), même si tout a été livré.
- Le brouillon n'est pas enregistré sur disque.
- Les livraisons ne passent que par /echange (deux joueurs à 5 blocs) ; un service se valide ou se conteste.
- Les objets custom choisis par leur nom ne sont pas trouvables (seulement depuis l'inventaire).

**À déployer ensemble sur Kixster** : KS_Contrats 1.0.0 (nouveau) et KS_Economy 1.12.0 (contient la 1.11.1 : prix du
cuivre taillé), puis `didacticiel.yml` (thème « Contrats », `requiert: KS_Contrats`). Commande LuckPerms à faire par
l'humain pour les admins non op : `kscontrats.admin`.

**Déployé sur Kixster le 27/10/2026 à 03:49 (accord de LeKiwi06 : « Oui, déploie KS_Contrats 1.0.0 et KS_Economy 1.12.0 sur Kixster avec le didacticiel.yml ») avec KS_Economy 1.12.0 et `didacticiel.yml` (ancien dans `_removed-ks_didacticiel-didacticiel-2026-10-27/`) ; actif après redémarrage, non testé en jeu.**
