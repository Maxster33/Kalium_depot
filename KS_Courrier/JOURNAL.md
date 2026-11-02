# KS_Courrier - journal

Plugin de Kixster (survie) : boîte aux lettres, lettres et colis, livraison des demandes publiques de KS_Contrats.
Cahier : `Cahiers_KaLium/Blocs_Cultures_Objets.md`, partie 9 (local). Dépend de KLM_Menu, KS_Economy, KS_Blocs,
KS_Contrats (1.1.0) ; `softdepend` KS_Claim (1.6.0), SimpleClaimSystem, KS_MondeTest.

## 1.0.0 - boîte aux lettres, lettres, colis (02/11/2026, LeKiwi06)

**Demande** (liste du 01/11, partie 9 du cahier) puis, le 02/11, après un point complet demandé par LeKiwi06 (« l'idée
n'était pas de moi ») :

| Question | Choix de LeKiwi06 |
|---|---|
| Nombre de boîtes | Une seule par joueur, posée dans un de ses claims ; on ne peut ni supprimer ni vendre ce claim |
| Crafts | Boîte : 1 coffre + 5 lingots de fer en U ; enveloppe : 2 papiers ; colis : 1 coffre + 1 ficelle + 2 papiers, 27 cases |
| Frais | Selon la distance entre les deux boîtes : 1 point par tranche de 1 000 blocs (gratuit sous 1 000) |
| Destinataire sans boîte | « pas de boîte = pas d'envoi » |
| Boîte en forme de cube | Oui |
| Courrier non relevé | Ne revient pas à l'expéditeur ; historique des envois et des réceptions |
| Demandes publiques | Bouton dans /contrat (KS_Contrats 1.1.0), récompense en points ou en objets, 3 ouvertes au plus, expiration facultative |
| Père Noël | Gardé pour l'événement de Noël (décembre) : jusqu'à 3 objets demandés, moins de chances pour un objet cher. **Pas codé** (à préciser en décembre) |
| Maire | Abandonné (pas de maire sur le serveur) |
| Journal hebdomadaire | Pas pour le moment |

**Boîte aux lettres** (`Boites`) : bloc custom de KS_Blocs (`ks_courrier:boite_aux_lettres`, base : bloc musical,
apparence `boite_aux_lettres`, orientable). Pose refusée : si le joueur a déjà une boîte, hors de ses propres claims
(propriétaire du claim, pas membre), dans un claim en vente. Monde de test : n'importe où (pas de claims). Clic droit du
propriétaire : menu ; d'un autre : « Boîte aux lettres de X ». Accroupi avec un bloc en main : le bloc se pose contre
elle. Cassée (ou explosée) : rendue en objet par KS_Blocs, le joueur n'a plus de boîte, son courrier reste gardé. Pas
de piston. Remise en place par l'encapsuleur : sans propriétaire, adoptée par le premier joueur sans boîte qui la
clique dans son claim. Claim de la boîte : ni supprimé, ni mis en vente (service `GardienClaim` de KS_Claim 1.6.0).

**Menu de la boîte** (coffre) : « Courrier reçu (n) » (clic : l'envoi scellé va dans l'inventaire), « Envoyer une
lettre », « Envoyer un colis », « Demandes publiques » (KS_Contrats, livraison possible), « Historique ».

**Envoi** (`Poste`) : destinataire = tête d'un joueur en ligne qui a sa boîte (même monde : survie ou test), ou pseudo
écrit (joueur hors ligne). Frais affichés avant l'envoi, payés avec le score (`KSEconomy.debiterCompte`) ; Nether :
coordonnées x 8 ; distance horizontale.
- Lettre : clic sur un livre écrit (signé) de son inventaire, confirmation ; le livre et une enveloppe sont pris.
- Colis : coffre à 27 cases libres (Maj+clic depuis l'inventaire, glisser) + barre (Envoyer, Annuler, Aide) ; un colis
  vide est pris dans l'inventaire ; fermer ou annuler rend les objets (au sol si l'inventaire est plein).
- Livré dans la boîte du destinataire, même hors ligne ; message s'il est en ligne, et à sa connexion (« n envois
  t'attendent »).

**Aucune duplication** : le contenu reste au serveur (`courrier.yml`, enregistré à chaque changement) ; l'objet
scellé (« Lettre de X », « Colis de X », avec expéditeur, destinataire et date) ne porte que le numéro de l'envoi.
Ouvert une fois (clic droit) ; une copie s'ouvre vide, disparaît et la tentative est notée dans la console. Colis :
ouverture refusée sans assez de cases libres. Le contenu d'un colis ouvert est effacé (le résumé reste) ; le livre
d'une lettre est gardé (modération). N'importe quel joueur qui tient un envoi scellé peut l'ouvrir.

**Historique** : envois et réceptions (date, frais et distance, résumé du colis, état : dans la boîte / pris le ... /
ouvert le ... par ...). Admins (`kscourrier.admin`, op par défaut) : `/courrier historique <pseudo> [test]`, lecture des
lettres d'un clic.

**Demandes publiques** : KS_Contrats appelle le service `Livraison` (`LivraisonCourrier`) pour déposer, comme des colis
du serveur (27 piles par colis), les objets livrés au demandeur, la récompense en objets du livreur, la récompense
rendue (annulation, expiration).

**Objets** (marque `ks_courrier:objet` = `enveloppe`, `colis`, `lettre`, `colis_ferme` ; `ks_courrier:envoi` = numéro) :
base papier, images provisoires du jeu (`item_model` : papier, sac, sac marron ; boîte : tonneau). Ils ne servent dans
aucune recette de l'établi.

**Monde de test** : courrier du « moi de test » (CompteTest), séparé de la survie.

Limites :
- **L'apparence `boite_aux_lettres` manque dans `apparences.json` de KS_Blocs** (que seule la session des cultures
  modifie) : sans elle, la boîte est désactivée (message dans la console) ; lettres et colis restent fabricables mais
  ne peuvent pas être envoyés. Entrée proposée ci-dessous.
- Pas de « boîte du staff », pas de Père Noël, pas de journal hebdomadaire (choix ci-dessus).
- Recettes enregistrées par le plugin (pas KS_Crafts) ; prix KS_Economy, didacticiel, textures : à ajouter (liste dans
  le compte rendu de la session).
- Crafter (bloc fabricant) : la garde des recettes ne couvre que l'établi.

Entrée proposée pour `KS_Blocs/src/main/resources/apparences.json` (états libres à vérifier ; jack o'lanterns : harp
1-8, répéteur proposé : bit 1-3) :

```json
"boite_aux_lettres": {
  "bloc": "note_block",
  "modele": "orientable",
  "textures": {"front": "boite_aux_lettres_face", "side": "boite_aux_lettres_cote", "top": "boite_aux_lettres_dessus"},
  "variantes": {
    "north": "instrument=flute,note=1,powered=false",
    "east": "instrument=flute,note=2,powered=false",
    "south": "instrument=flute,note=3,powered=false",
    "west": "instrument=flute,note=4,powered=false"
  }
}
```

**Compilé le 02/11/2026, non déployé, non testé en jeu.** À déployer ensemble sur Kixster : KS_Courrier 1.0.0,
KS_Contrats 1.1.0, KS_Claim 1.6.0, KS_Blocs (avec l'apparence ci-dessus) et geyser-bedrock (correspondance régénérée).
