# Historique des conversations avec Claude

Ce dossier garde une trace des sessions de travail entre l'utilisateur et Claude sur le projet KaLium.

**Attention : le dépôt est public.** Ne jamais y mettre de mot de passe, jeton (`relay-token`...),
adresse e-mail, ni d'hôte ou identifiant SFTP qui ne figure pas déjà dans le dépôt.

## Un fichier par session

- Nom : `AAAA-MM-JJ-sujet-court.md` (exemple : `2026-09-24-import-depot.md`).
- Plusieurs sessions le même jour : ajouter un suffixe (`2026-09-24-rush-2.md`).

## Contenu attendu

```markdown
# <Date> — <Sujet>

- Plugin(s) concerné(s) : ...
- Versions avant / après : ...

## Demandé
Ce que l'utilisateur a demandé, avec ses mots.

## Fait
Ce qui a été réalisé (commits, jars déployés, fichiers modifiés).

## Décisions
Choix faits et pourquoi (y compris les propositions refusées).

## Reste à faire
Points ouverts, tests à mener en jeu.
```

Le détail technique de chaque version reste dans `<plugin>/JOURNAL.md` ; ce dossier sert à retrouver
le fil des échanges.
