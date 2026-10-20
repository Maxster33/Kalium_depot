

**Actif sur Event depuis son redémarrage du 17/10/2026 à 03:57 (journal lu, heures en UTC : 01:57) : KLM_Contacts 1.3.0 puis KS_Trains 1.4.0 activés sans erreur ni avertissement (KLM_Contacts avant KS_Trains, comme voulu pour le partage entre amis), démarrage en 12 s. Statut : non testé en jeu.**

## 1.4.0 - serveur à plusieurs espaces (KLM_Espaces) (21/10/2026, LeKiwi06)

**Demande de LeKiwi06 (21/10/2026)** : Kal-Games et Serveur Jeux expirent dans 3 jours ; « on va fusionner kal-games et kanvas en utilisant le même principe que pour les mondes simultanés dans serveur jeux » ; « les gens ne pourront pas passer de kanvas à kal-games sans le lobby, tout sera sur le même serveur mais de façon hermétique, on fera même des tchats séparés » ; inventaires séparés ; deux noms Velocity gardés (`kal-games` et `Kanvas`, même adresse). Plan : [Plan de migration](https://claude.ai/artifact/MyTJxpL8zq1XCy1sTZKdZh).

- Une partie annoncée au proxy (chef de groupe, « Inviter dans ma partie ») porte le nom Velocity de l'espace du joueur
  (métadonnée `klm_serveur`), au lieu du nom du serveur demandé une seule fois au premier joueur arrivé (sur Kanvas,
  il aurait pu valoir `Kanvas` pour une partie de Kal-Games).
- Contient la 1.3.0 (sur Event seulement, non testée).
- Remarque : ce `JOURNAL.md` a été tronqué par le commit 4f70287 du 17/10/2026 (les sections 1.0.0 à 1.3.0 sont
  dans l'historique git), signalé à LeKiwi06, non restauré.

**Compilé, non déployé, non testé en jeu.** À déployer avec KLM_Espaces 1.0.0 et KaliumRelay 1.9.0 (fusion de Kal-Games dans Kanvas).
