

**Actif sur Event depuis son redémarrage du 17/10/2026 à 04:18 (journal lu, heures en UTC : 02:18) : KS_Trains 1.4.1 activé sans erreur ni avertissement (après KLM_Contacts 1.3.0), démarrage en 12 s. Statut : non testé en jeu.**

## 1.4.2 - modèle agrandi orienté sur l'axe du train (17/10/2026, LeKiwi06)

Signalement de LeKiwi06 (vidéo sur Event) : « le bloc bouge dans tous les sens en pente ».

- **Cause** (défaut de la 1.3.7) : l'inclinaison venait du bloc de rail sous le wagon ; sur une pente, le wagonnet est à
  mi-hauteur entre deux blocs et le bloc trouvé change d'un tick à l'autre : le modèle basculait sans cesse entre
  penché et droit, et sa direction changeait en même temps (orientation du wagonnet du jeu, ou point cardinal du four).
- **Correction** : le modèle est orienté sur l'**axe du train** à ce wagon (du wagon de derrière au wagon de devant ;
  aux deux bouts, le wagon lui-même) : sa face avant pointe vers l'avant du train (fours : exactement, plus d'arrondi
  au point cardinal ; coffres aussi), et il penche exactement comme la voie, avec une transition douce en haut et en bas
  des pentes et dans les virages. Rotation autour de son centre, changement adouci sur 2 ticks, envoyé seulement s'il
  a tourné de plus d'un quart de degré.
- Vidéo non regardée (ni ffmpeg ni OpenCV sur le PC) : cause trouvée dans le code.

Compilé le 17/10/2026 (`sortie/KS_Trains-1.4.2.jar`). **Statut : non déployé, non testé en jeu.**

