package fr.kalium.kgbuildbattle;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Une partie privée côté kal-games : juste de quoi la lister, la rejoindre et envoyer ses joueurs sur Kanvas, où
 * elle se joue (salle d'attente, lancement par l'hôte). Même principe que les parties du Bingo (KG_Bingo).
 */
final class Partie {

    final String id;
    final String code;
    final UUID hote;
    final String nomHote;
    final Tempo tempo;
    final int tailleEquipes;
    final int equipesMax;
    /** Mode spécial : chaque joueur écrit son thème, puis vote parmi les propositions. */
    final boolean themesEcrits;
    final Instant creee = Instant.now();
    final Set<UUID> joueurs = new LinkedHashSet<>();

    Partie(String id, String code, UUID hote, String nomHote, Tempo tempo, int tailleEquipes, int equipesMax,
           boolean themesEcrits) {
        this.id = id;
        this.code = code;
        this.hote = hote;
        this.nomHote = nomHote;
        this.tempo = tempo;
        this.tailleEquipes = tailleEquipes;
        this.equipesMax = equipesMax;
        this.themesEcrits = themesEcrits;
        joueurs.add(hote);
    }

    int places() {
        return tailleEquipes * equipesMax;
    }
}
