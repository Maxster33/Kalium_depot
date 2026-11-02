# Wiki KaLium (salon Discord)

Publié par le bot avec `k!wiki setup <id du salon>`, puis mis à jour avec `k!wiki maj` après chaque modification de ce
dossier (le bot relit les fichiers, pas besoin de le redémarrer). Textes pour les joueurs : pas de détail technique.

- Un dossier par serveur, numéroté pour l'ordre (`01-kixster`, `02-kal-games`, `03-kanvas`) : un sommaire (embed)
  par serveur dans le salon.
- `_sommaire.md` : `# Titre`, puis `couleur: #rrggbb` (facultatif), puis le texte d'introduction du sommaire.
- Une fiche par fichier `NN-nom.md` = un fil verrouillé : `# Titre` (le fil s'appelle « Serveur · Titre »), puis
  `> résumé` (facultatif, affiché à côté du lien dans le sommaire), puis le contenu en Markdown Discord.
- Le contenu est coupé en messages de 2 000 caractères au plus, entre deux paragraphes ; une ligne `---` seule force
  un nouveau message.
- Le numéro devant un nom ne sert qu'à l'ordre : renuméroter garde le même fil. **Renommer** un fichier (`monde` →
  `le-monde`) crée un nouveau fil ; l'ancien est signalé, à supprimer à la main.
