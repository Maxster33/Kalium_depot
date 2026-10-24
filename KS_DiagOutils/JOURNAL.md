# KS_DiagOutils - journal

Plugin **temporaire** de diagnostic pour Kixster : ne change rien au jeu, écrit au journal du serveur (lignes
`[outil]`) chaque usage d'une houe, d'une hache ou d'une pelle sur un bloc. À retirer une fois la cause trouvée.

## 1.0.0 - outils sans effet pour les joueurs Bedrock (24/10/2026, LeKiwi06)

**Demande de LeKiwi06 (24/10/2026)** : « peux-tu régler le problème qu'ont les joueurs bedrock pour modifier un bloc
(farmland, stripped log, etc.) » ; « zak a eu le même souci en arrivant, c'est donc peu probable que le postulat
"la config de Cybonath est en cause" soit valable ».

**Cherché sans succès en lisant le code** (24/10/2026) :
- Geyser 2.12.0 (proxy) : il annule une action sur un bloc (bloc remis) si l'objet en main ne correspond pas, hors de
  la bordure ou trop loin ; mais la pose de blocs passe par les mêmes contrôles et marche. Les objets custom de
  `geyser-bedrock` sont tous sur le livre de connaissances, pas sur les outils.
- Paper 26.3 : labourer, écorcer, faire un chemin ne sont plus du code de la houe, de la hache et de la pelle, mais
  l'élément `block_transformers` de l'outil (`BlockTransformer.transformBlock`), qui passe par
  `EntityChangeBlockEvent` (joueur). Refusé aussi si l'outil est en main droite avec un bouclier en main gauche, sans
  être accroupi.
- Nos plugins de Kixster qui écoutent `EntityChangeBlockEvent` (KS_FairPlay, KS_CoffreMort, KS_Teleport) n'annulent que
  des blocs précis ; SimpleClaimSystem 1.13.1 n'y annule que les withers.
- Le test du 23/10 « LeKiwi06 y arrive au même endroit » a été fait par un opérateur : il ne prouve pas qu'un joueur
  Java non opérateur y arrive.

**Ce qu'il écrit** (tout joueur qui tient un de ces outils, Bedrock ou Java) :
- `geste` : le bras bouge en visant un bloc (au plus une ligne par seconde) ;
- `clic reçu` : le serveur a reçu le clic droit sur le bloc (face, main, accroupi, objet en main gauche), puis la
  priorité à laquelle il est refusé et les plugins inscrits à cette priorité ;
- `changement de bloc` : le serveur a tenté le changement, puis la priorité à laquelle il est annulé et les plugins ;
- `résultat` : le bloc au tick suivant.

**Lecture** : geste sans clic reçu = Geyser n'a rien envoyé au serveur (piste suivante : `debug-mode: true` dans le
`config.yml` de Geyser du proxy) ; clic reçu mais refusé ou changement annulé = les plugins nommés.

**Compilé, non déployé, non testé en jeu.** À déployer sur Kixster seulement. Test : un joueur Bedrock, puis un joueur
Java **non opérateur**, labourent, écorcent et font un chemin au même endroit ; lire les lignes `[outil]`.
