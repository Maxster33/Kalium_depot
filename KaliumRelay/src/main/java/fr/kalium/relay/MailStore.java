package fr.kalium.relay;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0 - boite aux lettres DURABLE entre serveurs (categorie 4 « Recompenses », LeKiwi06, 03/10/2026) : un serveur
 * depose un message pour une boite (ex. « event »), le serveur destinataire les lit quand il veut puis confirme chaque
 * message (DELETE) ; tant qu'il n'est pas confirme, un message reste, meme apres un redemarrage du proxy (un fichier par
 * boite dans plugins/kaliumrelay/mail/). Contrairement a /assignment (2 minutes en memoire), rien n'expire.
 */
final class MailStore {

    /** Taille maximale d'un message et nombre maximal de messages par boite (protection du proxy). */
    static final int MAX_BODY_BYTES = 256 * 1024;
    static final int MAX_MESSAGES = 20_000;

    private final Path dossier;
    private final Logger logger;
    private final Map<String, LinkedHashMap<String, String>> boites = new LinkedHashMap<>();

    MailStore(Path dataDirectory, Logger logger) {
        this.dossier = dataDirectory.resolve("mail");
        this.logger = logger;
    }

    /** Nom de boite accepte : lettres minuscules, chiffres, - et _ (32 au plus). */
    static boolean nomValide(String boite) {
        return boite != null && boite.matches("[a-z0-9_-]{1,32}");
    }

    private LinkedHashMap<String, String> boite(String nom) {
        return boites.computeIfAbsent(nom, n -> {
            LinkedHashMap<String, String> messages = new LinkedHashMap<>();
            Path fichier = dossier.resolve(n + ".txt");
            if (Files.exists(fichier)) {
                try {
                    for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
                        int tab = ligne.indexOf('\t');
                        if (tab > 0) {
                            messages.put(ligne.substring(0, tab), ligne.substring(tab + 1));
                        }
                    }
                } catch (IOException e) {
                    logger.warn("[KaliumRelay] Boite " + n + " illisible : " + e.getMessage());
                }
            }
            return messages;
        });
    }

    /** Depose un message ; renvoie son id, ou null si la boite est pleine. */
    synchronized String deposer(String nom, String corps) throws IOException {
        LinkedHashMap<String, String> messages = boite(nom);
        if (messages.size() >= MAX_MESSAGES) {
            return null;
        }
        String id = UUID.randomUUID().toString();
        messages.put(id, Base64.getEncoder().encodeToString(corps.getBytes(StandardCharsets.UTF_8)));
        ecrire(nom, messages);
        return id;
    }

    /** Messages en attente : lignes « id TAB corps en base64 ». */
    synchronized String lister(String nom) {
        StringBuilder sortie = new StringBuilder();
        boite(nom).forEach((id, corps) -> sortie.append(id).append('\t').append(corps).append('\n'));
        return sortie.toString();
    }

    /** Confirme (retire) un message ; true s'il existait. */
    synchronized boolean confirmer(String nom, String id) throws IOException {
        LinkedHashMap<String, String> messages = boite(nom);
        if (messages.remove(id) == null) {
            return false;
        }
        ecrire(nom, messages);
        return true;
    }

    private void ecrire(String nom, Map<String, String> messages) throws IOException {
        Files.createDirectories(dossier);
        List<String> lignes = new ArrayList<>();
        messages.forEach((id, corps) -> lignes.add(id + "\t" + corps));
        Path temporaire = dossier.resolve(nom + ".txt.tmp");
        Files.write(temporaire, lignes, StandardCharsets.UTF_8);
        Files.move(temporaire, dossier.resolve(nom + ".txt"), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }
}
