package fr.kalium.relay;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.6.0 - donnees de KLM_Contacts (amis, demandes, blocages, reglages), gardees sur le proxy : c'est le seul endroit qui
 * voit tous les joueurs et tous les serveurs. Un fichier par joueur dans plugins/kaliumrelay/contacts/ (&lt;uuid&gt;.txt,
 * lignes « cle=valeur ») et un index des pseudos (noms.txt : « pseudo en minuscules TAB uuid »), mis a jour a chaque
 * connexion. Rien n'expire. Toutes les methodes sont appelees sous le verrou de {@link Contacts}.
 */
final class ContactsStore {

    /** Reglage des messages prives : tout le monde, amis seulement, personne. */
    static final String MP_ALL = "all";
    static final String MP_FRIENDS = "friends";
    static final String MP_NONE = "none";

    static final class Profile {
        final UUID id;
        String name = "";
        final Set<UUID> friends = new LinkedHashSet<>();
        /** Demandes d'ami recues, en attente de reponse. */
        final Set<UUID> incoming = new LinkedHashSet<>();
        /** Demandes d'ami envoyees, en attente de reponse. */
        final Set<UUID> outgoing = new LinkedHashSet<>();
        final Set<UUID> blocked = new LinkedHashSet<>();
        /** Mode invisible : les amis le voient hors ligne. */
        boolean invisible;
        /** Recevoir « X s'est connecte / deconnecte ». */
        boolean notify = true;
        String mp = MP_ALL;

        Profile(UUID id) {
            this.id = id;
        }
    }

    private final Path dossier;
    private final Logger logger;
    private final Map<UUID, Profile> profils = new HashMap<>();
    private final Map<String, UUID> noms = new HashMap<>();

    ContactsStore(Path dataDirectory, Logger logger) {
        this.dossier = dataDirectory.resolve("contacts");
        this.logger = logger;
        Path index = dossier.resolve("noms.txt");
        if (Files.exists(index)) {
            try {
                for (String ligne : Files.readAllLines(index, StandardCharsets.UTF_8)) {
                    int tab = ligne.indexOf('\t');
                    if (tab > 0) {
                        try {
                            noms.put(ligne.substring(0, tab), UUID.fromString(ligne.substring(tab + 1).trim()));
                        } catch (IllegalArgumentException ignored) {
                            // ligne abimee : ignoree
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn("[KaliumRelay] Contacts : index des pseudos illisible : " + e.getMessage());
            }
        }
    }

    /** Joueur deja venu sur KaLium depuis l'installation, d'apres son pseudo (majuscules ignorees), ou null. */
    UUID byName(String name) {
        return name == null ? null : noms.get(name.trim().toLowerCase(Locale.ROOT));
    }

    private Path fichier(UUID id) {
        return dossier.resolve(id + ".txt");
    }

    /** Profil du joueur (cree vide s'il n'existe pas encore ; ecrit seulement par {@link #save}). */
    Profile get(UUID id) {
        return profils.computeIfAbsent(id, this::load);
    }

    private Profile load(UUID id) {
        Profile profil = new Profile(id);
        Path fichier = fichier(id);
        if (!Files.exists(fichier)) {
            return profil;
        }
        try {
            for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
                int egal = ligne.indexOf('=');
                if (egal <= 0) {
                    continue;
                }
                String valeur = ligne.substring(egal + 1);
                switch (ligne.substring(0, egal)) {
                    case "name" -> profil.name = valeur;
                    case "friends" -> lire(valeur, profil.friends);
                    case "incoming" -> lire(valeur, profil.incoming);
                    case "outgoing" -> lire(valeur, profil.outgoing);
                    case "blocked" -> lire(valeur, profil.blocked);
                    case "invisible" -> profil.invisible = Boolean.parseBoolean(valeur);
                    case "notify" -> profil.notify = Boolean.parseBoolean(valeur);
                    case "mp" -> profil.mp = MP_FRIENDS.equals(valeur) || MP_NONE.equals(valeur) ? valeur : MP_ALL;
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("[KaliumRelay] Contacts : profil " + id + " illisible : " + e.getMessage());
        }
        return profil;
    }

    private static void lire(String valeur, Set<UUID> ensemble) {
        for (String partie : valeur.split(",")) {
            if (partie.isBlank()) {
                continue;
            }
            try {
                ensemble.add(UUID.fromString(partie.trim()));
            } catch (IllegalArgumentException ignored) {
                // identifiant abime : ignore
            }
        }
    }

    private static String ecrire(Set<UUID> ensemble) {
        StringBuilder sortie = new StringBuilder();
        for (UUID id : ensemble) {
            if (sortie.length() > 0) {
                sortie.append(',');
            }
            sortie.append(id);
        }
        return sortie.toString();
    }

    void save(Profile profil) {
        List<String> lignes = new ArrayList<>();
        lignes.add("name=" + profil.name);
        lignes.add("friends=" + ecrire(profil.friends));
        lignes.add("incoming=" + ecrire(profil.incoming));
        lignes.add("outgoing=" + ecrire(profil.outgoing));
        lignes.add("blocked=" + ecrire(profil.blocked));
        lignes.add("invisible=" + profil.invisible);
        lignes.add("notify=" + profil.notify);
        lignes.add("mp=" + profil.mp);
        remplacer(fichier(profil.id), lignes);
    }

    /** Connexion d'un joueur : son pseudo actuel est retenu (un changement de pseudo libere l'ancien). */
    void seen(UUID id, String name) {
        Profile profil = get(id);
        String cle = name.toLowerCase(Locale.ROOT);
        boolean nouveau = !Files.exists(fichier(id));
        if (!nouveau && name.equals(profil.name) && id.equals(noms.get(cle))) {
            return;
        }
        noms.values().removeIf(id::equals);
        noms.put(cle, id);
        profil.name = name;
        save(profil);
        List<String> lignes = new ArrayList<>();
        noms.forEach((nom, uuid) -> lignes.add(nom + "\t" + uuid));
        remplacer(dossier.resolve("noms.txt"), lignes);
    }

    private void remplacer(Path fichier, List<String> lignes) {
        try {
            Files.createDirectories(dossier);
            Path temporaire = fichier.resolveSibling(fichier.getFileName() + ".tmp");
            Files.write(temporaire, lignes, StandardCharsets.UTF_8);
            Files.move(temporaire, fichier, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warn("[KaliumRelay] Contacts : ecriture de " + fichier.getFileName() + " impossible : " + e.getMessage());
        }
    }
}
