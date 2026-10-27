# KS_Contrats — cahier des charges (demande de LeKiwi06, 27/10/2026)

**État : validé le 27/10/2026 (partie 2) ; la partie 3 reste valable sauf ce que la partie 2 remplace (garantie,
expiration, engagements, durée, `/echange`). Codé le 27/10/2026 : KS_Contrats 1.0.0 + KS_Economy 1.12.0 + thème
« Contrats » du didacticiel (commit 66d7a96), compilés, non déployés, non testés. Copier ce cahier dans
`KS_Contrats/CAHIER_DES_CHARGES.md` au déploiement.** (maquette des menus montrée dans la session ; questions en partie 4).

Nouveau plugin `KS_Contrats` (Kixster). Rôle : garder une trace des engagements entre joueurs, pour qu'un admin
puisse trancher en cas de non-respect.

| Plugin | Rôle |
|---|---|
| `KS_Contrats` (nouveau) | `/contrat`, menus en coffre, garanties en séquestre, litiges tranchés par un admin |
| `KS_Economy` (lu seulement) | `KSEconomy.debiter` / `crediter` / `solde` pour les garanties (aucun changement) |
| `KLM_Menu` (lu seulement) | `Contenant` (coffres), `Gui` (saisies de texte et de nombres) |
| `KS_Menu` (à confirmer) | bouton « Contrats » dans le menu de Kixster |
| `KS_Didacticiel` | nouvelle entrée `didacticiel.yml` au déploiement |

## 1. Demande d'origine (recopiée telle quelle, 27/10/2026)

> peut tu créer le /contrat sur kixster ( il sert a avoir une trace et  qu'un admin puisse trancher en cas de non
> respect de l'engagement )  :
>
> la commande ouvre une interface coffre dans laquelle on voit nos contrats actifs
>
> on peux modifier ou annuler ou valider de façon anticipée un contrat actif , le changement est effectif si la/les
> personnes en face accepte .
>
> pour un contrat de groupe , toute les personnes du groupes doivent accepter avant que l'action soit effectué
>
> quand on a un contrat qui demande notre attention , on reçoit une message en se connectant (proposition ,
> modification , etc )
>
> item pour créer un contrat :
> 1 on selectionne la/les personnes de notre équipe qui participent au contrat ( facultatif )
> 2 on sélectionne la/les personne avec qui passer le contrat
> 3 on écrit a quoi on s'engage
> 4 on écrit a quoi s'engage l'autre / les autres personne
> 5 on met une date d'expiration ( facultatif )
> 6 on met une garantis ( somme de points en séquestre a donner par chaque parties le temps du contrat ) (facultatif)
> 6 on valide
>
> un item pour voir les propositions de contrat :
> quand on clique sur un contrat on peux l'accepter , le refuser , ou proposer une modification
>
> quand on modifie un contrat , c'est à la/aux personne d'en face de l'accepter ou de le modifier à leurs tour
>
> gestion de nos contrats actifs :
> on peux leur choisir un nom , un logo ( item du jeu ) , et les mettre en favoris ( apparait au début dans la liste )
> ces customisations sont personnelles chaque joueurs peut avoir son propre nom et logo pour un même contrat
>
> exemple de contrat :
> titre : [item A] contre [item B]
> logo : [item B]
>
> [pseudo de l'équipe A] et moi s'engageons à :  fournir X [item A] chaque semaines
>
> [pseudo de l'équipe B ] s'engagent à  : fournir X [item B] chaque semaines
>
> Date d'expiration : dans 36 j 14h 21m
>
> Garantis : 5000 points stocké jusqu'à la fin du contrat
>
> note : un contrat n'est pas forcement un item contre un autre , ça peut aussi être un service ( creusage d'une
> zone , faire un build , ramener un mob , etc ) c'est aux joueurs de le préciser dans le contrat

## 2. Réponses de LeKiwi06 (27/10/2026)

1. Garantie : « par chaque partie ; en cas de groupe, la somme est divisée équitablement entre tous les joueurs (pas de
   chiffre à virgule ; si ça doit arriver, certains joueurs paieront 1 point de plus que les autres, ce n'est pas
   gênant) ».
2. Expiration : « quand un contrat expire, on envoie une notification disant qu'il faut soit le valider, soit le
   contester ».
3. Décision d'admin : « montant libre possible, si le contrat avait été partiellement rempli par exemple ».
4. Bouton dans le menu de Kixster : oui. 5. Permission `kscontrats.admin` : oui.

Modifications demandées :
> on choisis le type de contrat dans avant d'écrire un engagement ( bien ou service ) si l'engagement est un bien ,
> alors on peux selectionner l'item dans l'inventaire ou bien écrire son nom , et sa quantité
>
> on peux choisir soit une date d'expiration pour un contrat en " one shot" , ou une récurence genre " toutes les
> semaines " pour les contrat qui se répètent
>
> on va aussi modifier le /echange pour pouvoir sélectionner un contrat quand on échange , comme ça si le contrat
> contient un item , il est marqué comme validé dans le contrat par la suite ( ou bien avec une mention 10/64 , si on
> l'a partiellement remplis par exemple )
>
> quand on sélectionne un contrat de bien dans le /echange , on ne peux mettre que les items ou les points du contrat ,
> pas d'autres biens et on ne peux pas mettre + que le maximum du contrat . ( pour eviter les petits malins qui
> profiteraient d'un echange banale pour discrètement valider une close de contrat )

### Proposition mise à jour (27/10/2026, à valider)

**Engagements** : avant d'écrire un engagement, on choisit son type :
- **Bien** : jusqu'à 5 lignes, chacune = un objet et sa quantité, ou des points. L'objet se choisit en cliquant sur un
  objet de son inventaire (il faudra livrer exactement cet objet : mêmes enchantements, même objet custom, usure non
  comptée) ou en écrivant son nom en français (table `noms_objets.txt` de KS_Economy ; n'importe quel objet de ce type
  compte). Quantité : fenêtre de saisie. Une précision écrite facultative (« livré au coffre de ma base »).
- **Service** : texte libre (400 caractères), comme avant.

**Durée** : « Une fois » (date d'expiration facultative) ou « Récurrent » (chaque jour, chaque semaine, toutes les 2
semaines, chaque mois, à compter de l'acceptation ; date de fin facultative, sinon jusqu'à une annulation ou une
validation anticipée acceptée). Dans un contrat récurrent, les quantités sont dues à chaque période et le compteur
repart à 0 au début de la suivante.

**Suivi des biens** : chaque ligne affiche sa progression (« 10/64 diamants », « cette semaine » si récurrent), pour la
partie entière (les livraisons de chaque coéquipier s'additionnent).

**Fin d'une période ou expiration** : si toutes les lignes de bien sont livrées et qu'il n'y a pas de service, la
période (ou le contrat) est validée d'office. Sinon, chaque participant reçoit « Valider ou contester » (message tout de
suite s'il est en ligne, et à chaque connexion tant qu'il n'a pas répondu). Contester = signaler un non-respect (litige).
Contrat « une fois » : garanties rendues quand tous ont validé. Contrat récurrent : la période est rangée dans
l'historique, le contrat continue.

**Garantie** : un montant par partie, divisé entre ses joueurs (5 000 pour 3 joueurs : 1 667, 1 667 et 1 666 ; le point
de plus va à ceux listés en premier). Chaque joueur paie sa part quand il accepte.

**Décision d'admin** : il répartit librement le total en séquestre entre les participants (un montant par joueur,
le total doit tomber juste), avec des raccourcis « Tout rendre », « Tout à la partie A », « Tout à la partie B ».

**`/echange` (KS_Economy)** : bouton « Contrat » dans le panneau d'échange → liste des contrats actifs que les deux
joueurs ont ensemble, chacun d'un côté différent. Une fois un contrat choisi (affiché aux deux) :
- chacun ne peut poser que les objets et les points que **sa partie doit**, sans dépasser **ce qui reste à livrer**
  (période en cours pour un récurrent) ; tout autre objet est refusé avec un message ;
- un seul des deux peut poser quelque chose (livraison sans contrepartie dans le même échange) ;
- à la conclusion, les quantités livrées s'ajoutent au contrat (« 10/64 ») et la livraison entre dans l'historique ;
- « Retirer le contrat » repasse en échange normal (les objets déjà posés qui ne sont pas du contrat restent refusés
  tant qu'un contrat est choisi).
Technique : KS_Economy ne dépend pas de KS_Contrats (service Bukkit déclaré par KS_Contrats ; sans lui, pas de bouton).
Une autre session a préparé KS_Economy 1.11.1 (prix du cuivre, non commité) : l'échange viendra en 1.12.0 par-dessus,
sans toucher à ses fichiers.

### Réponses aux questions restantes (27/10/2026, 03:20) — cahier validé, code commencé

1. 7 jours sans réponse = validé : « ça me va ».
2. « on peut mettre plusieurs biens par contrat (maximum 5 items différents) » : jusqu'à 5 objets différents par
   engagement, plus des points (une ligne à part, ne compte pas dans les 5).
3. Périodes à compter de l'acceptation : « oui ».

### Questions restantes (27/10/2026, posées)

1. **Pas de réponse à « Valider ou contester »** : si un joueur ne répond jamais (il a arrêté de jouer), les garanties
   restent bloquées. Proposé : sans réponse au bout de **7 jours**, il est compté comme ayant validé.
2. **Plusieurs lignes par bien** (jusqu'à 5, objets ou points mélangés) : d'accord, ou une seule ligne ?
3. **Récurrent** : les périodes sont-elles bien « à compter de l'acceptation » (un contrat accepté jeudi à 18 h : chaque
   jeudi à 18 h), ou alignées sur le calendrier (chaque lundi à 0 h, le 1er du mois) ?

## 3. Proposition (choix d'interprétation de Claude, à valider)

### Vocabulaire
- **Partie A** : celui qui propose + les joueurs qu'il met de son côté (« mon équipe »). **Partie B** : les joueurs
  avec qui il passe le contrat. Il n'existe pas de système d'équipe sur Kixster : l'équipe, c'est seulement le côté
  du contrat. Chaque côté peut compter plusieurs joueurs.
- **Participants** : tous les joueurs des deux parties.

### Menus (tous en coffre, modèle de `/rewards` et de KS_Economy)
1. **`/contrat` → « Mes contrats »** (6 rangées) : les contrats actifs, **favoris en premier** puis du plus récent au
   plus ancien ; chaque contrat = son logo et son nom **personnels** (par défaut : livre et « Contrat avec <pseudo> ») ;
   description : les deux parties, l'expiration restante, la garantie, et « Demande ta réponse » s'il y a une demande
   en attente. Barre du bas : pages (colonnes 1 et 9), « Créer un contrat » (livre et plume), « Propositions (n) »
   (papier, avec le nombre en attente), aide (livre), « Fermer » (barrière), « Terminés » (historique).
2. **« Créer un contrat »** : un coffre-brouillon avec une case par étape, chacune montrant sa valeur actuelle :
   1. Mon équipe (facultatif) — 2. L'autre partie (obligatoire) — 3. Mon engagement — 4. Leur engagement —
   5. Expiration (facultatif) — 6. Garantie (facultatif) — « Envoyer la proposition » (bloc vert, refusé avec un
   message tant qu'une étape obligatoire manque). Le brouillon est gardé si on ferme le menu.
   - **Choix des joueurs** : coffre de têtes des joueurs en ligne sur Kixster (clic = ajouté / retiré, ceux choisis
     brillent) + « Autre pseudo » (saisie ; joueur déjà venu sur Kixster, même hors ligne). Un joueur ne peut pas
     être des deux côtés.
   - **Engagements** : fenêtre de saisie de texte sur plusieurs lignes (limite proposée : 400 caractères par partie).
   - **Expiration** : fenêtre avec jours / heures / minutes, ou « Aucune ».
   - **Garantie** : fenêtre avec un nombre de points (0 = aucune).
3. **« Propositions »** : nouveaux contrats, modifications, validations anticipées et annulations qui attendent ma
   réponse. Clic → fiche : lire le contrat, **Accepter** (vert), **Refuser** (rouge), **Proposer une modification**
   (ouvre le brouillon pré-rempli ; une modification remplace la proposition et repart chez tous les autres).
4. **Fiche d'un contrat actif** : lire le contrat (fenêtre de texte, plus lisible que la description d'un objet) ;
   **Renommer** (étiquette), **Changer le logo** (clic sur un objet de son inventaire), **Favori** (étoile) : réglages
   personnels, invisibles des autres ; **Modifier**, **Valider en avance**, **Annuler** (demandes à faire accepter) ;
   **Signaler un non-respect** ; **Versions** (toutes les versions acceptées, avec dates et signataires).

### Règles
1. **Accord de tous** : une proposition, une modification, une validation anticipée ou une annulation n'est effective
   que quand **tous les autres participants** l'ont acceptée (y compris les coéquipiers de celui qui la fait).
   Un seul refus annule la demande pour tout le monde (le contrat actif reste tel qu'il était).
2. Une seule demande en attente par contrat ; celui qui l'a faite peut la retirer.
3. Pendant une demande de modification, l'ancienne version reste en vigueur.
4. **Message à la connexion** : « Contrats : n demandent ta réponse (/contrat) » ; message aussi tout de suite aux
   participants en ligne. Message quand une demande est acceptée, refusée, ou quand un contrat expire.
5. **Garantie (séquestre)** : le montant est retiré du score de chaque participant au moment où il accepte (celui qui
   propose : à l'envoi) ; refus ou retrait de la proposition : chacun récupère sa part. Une modification qui change la
   garantie retire ou rend la différence à l'acceptation. On ne peut pas accepter sans le solde nécessaire.
6. **Fin du contrat** : validation anticipée acceptée, annulation acceptée, ou expiration → garanties rendues à
   chacun, contrat rangé dans « Terminés ».
7. **Litige** : « Signaler un non-respect » (avec un texte d'explication) met le contrat **en litige** : garanties
   bloquées (plus rendues à l'expiration), plus de demandes possibles, admins prévenus (en ligne : message ; sinon à
   la connexion). Un admin (permission `kscontrats.admin`) ouvre `/contrat admin` : contrats en litige, recherche des
   contrats d'un joueur, lecture de toutes les versions et des signalements ; il tranche : rendre les garanties à
   tous, donner toutes les garanties à la partie A ou à la partie B (partagées entre ses membres), puis clore.
8. **Trace** : rien n'est jamais effacé ; chaque version, demande, acceptation, refus, signalement et décision d'admin
   est gardé avec sa date (fichier `plugins/KS_Contrats/contrats.yml`).
9. **Monde de test** : comptes du « moi de test » par `CompteTest.de` (contrats et garanties séparés de la survie).
10. Limites proposées : 10 joueurs par partie, 30 contrats actifs + propositions par joueur.

### Points techniques
- Textes dans `lang.yml` ; libellés mesurés (aucun ne défile).
- Aucun objet custom : icônes vanilla seulement (rien à envoyer sur le proxy).
- `depend` KLM_Menu, KS_Economy ; `softdepend` KS_Menu, KS_MondeTest.

## 4. Questions à LeKiwi06

1. **Garantie** : le montant est-il payé par **chaque joueur** du contrat (proposé), ou **une fois par partie**
   (et alors qui paie dans une équipe : celui qui propose ? partagé ?) ?
2. **Litige à l'expiration** : délai avant de rendre les garanties à l'expiration, pour laisser le temps de signaler
   (proposé : 3 jours), ou rendues tout de suite ?
3. **Décision d'admin** : les 3 choix proposés (rendre à tous / tout à A / tout à B) suffisent-ils, ou faut-il pouvoir
   donner un montant libre à chaque joueur (amende partielle) ?
4. **Bouton dans le menu de Kixster** (KS_Menu) en plus de `/contrat` ?
5. **Qui est admin** pour les litiges : permission `kscontrats.admin` donnée par LuckPerms (proposé) ?
