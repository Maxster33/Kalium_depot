package fr.kalium.anticheat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Alertes (cahier, catégorie 6) : chaque détection (GrimAC, x-ray, macros, duplication, revente...) devient une alerte
 * gardée dans l'historique (plugins/KS_AntiCheat/alertes.yml, 5 000 dernières) et annoncée au staff connecté
 * (permission ksanticheat.staff ; au plus une annonce par minute pour le même joueur et le même type).
 * Légère : le staff vérifie. Grave : suspension automatique (Suspensions).
 */
final class Alertes {

    enum Gravite { LEGERE, GRAVE }

    static final class Alerte {
        long date;
        UUID joueur;
        String nom;
        String type;
        Gravite gravite;
        String detail;
    }

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Europe/Paris"));
    private static final int MAX = 5000;
    private static final long PAUSE_ANNONCE_MS = 60_000;

    private final KSAntiCheat plugin;
    private final File fichier;
    private final List<Alerte> liste = new ArrayList<>();
    private final Map<String, Long> derniereAnnonce = new HashMap<>();
    private boolean modifie;

    Alertes(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "alertes.yml");
        charger();
    }

    /** Nouvelle alerte : historique, console, staff connecté. */
    void ajouter(UUID joueur, String nom, String type, Gravite gravite, String detail) {
        Alerte a = new Alerte();
        a.date = System.currentTimeMillis();
        a.joueur = joueur;
        a.nom = nom;
        a.type = type;
        a.gravite = gravite;
        a.detail = detail;
        liste.add(a);
        if (liste.size() > MAX) {
            liste.subList(0, liste.size() - MAX).clear();
        }
        modifie = true;
        plugin.getLogger().info("Alerte " + (gravite == Gravite.GRAVE ? "GRAVE" : "légère") + " : " + nom + " - " + type
                + " - " + detail);
        String cle = joueur + "|" + type;
        Long avant = derniereAnnonce.get(cle);
        if (gravite == Gravite.LEGERE && avant != null && a.date - avant < PAUSE_ANNONCE_MS) {
            return;
        }
        derniereAnnonce.put(cle, a.date);
        Component message = plugin.lang().c(gravite == Gravite.GRAVE ? "alerte.grave" : "alerte.legere",
                gravite == Gravite.GRAVE ? "<dark_red><bold>[Anti-triche]</bold> <red><nom> : <type> <gray>- <detail>"
                        : "<gold>[Anti-triche] <yellow><nom> : <type> <gray>- <detail>",
                "nom", nom, "type", type, "detail", detail);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (KSAntiCheat.staff(p)) {
                p.sendMessage(message);
            }
        }
        plugin.lang().saveIfNeeded();
    }

    List<Alerte> recentes(int nombre) {
        List<Alerte> r = new ArrayList<>(liste.subList(Math.max(0, liste.size() - nombre), liste.size()));
        java.util.Collections.reverse(r);
        return r;
    }

    List<Alerte> duJoueur(UUID joueur) {
        List<Alerte> r = new ArrayList<>();
        for (int i = liste.size() - 1; i >= 0; i--) {
            if (liste.get(i).joueur.equals(joueur)) {
                r.add(liste.get(i));
            }
        }
        return r;
    }

    /** Joueurs ayant des alertes, le plus récent d'abord : UUID -> nom. */
    Map<UUID, String> joueurs() {
        Map<UUID, String> r = new LinkedHashMap<>();
        for (int i = liste.size() - 1; i >= 0; i--) {
            r.putIfAbsent(liste.get(i).joueur, liste.get(i).nom);
        }
        return r;
    }

    static String ligne(Alerte a) {
        return DATE.format(Instant.ofEpochMilli(a.date)) + " " + (a.gravite == Gravite.GRAVE ? "[GRAVE] " : "") + a.type
                + " : " + a.detail;
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (Map<?, ?> m : yaml.getMapList("alertes")) {
            try {
                Alerte a = new Alerte();
                a.date = ((Number) m.get("date")).longValue();
                a.joueur = UUID.fromString(String.valueOf(m.get("joueur")));
                a.nom = String.valueOf(m.get("nom"));
                a.type = String.valueOf(m.get("type"));
                a.gravite = Gravite.valueOf(String.valueOf(m.get("gravite")));
                a.detail = String.valueOf(m.get("detail"));
                liste.add(a);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Alerte illisible ignorée : " + m);
            }
        }
    }

    /** Enregistre si besoin (toutes les 30 secondes et à l'arrêt). */
    void sauver() {
        if (!modifie) {
            return;
        }
        modifie = false;
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> sortie = new ArrayList<>();
        for (Alerte a : liste) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", a.date);
            m.put("joueur", a.joueur.toString());
            m.put("nom", a.nom);
            m.put("type", a.type);
            m.put("gravite", a.gravite.name());
            m.put("detail", a.detail);
            sortie.add(m);
        }
        yaml.set("alertes", sortie);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer alertes.yml : " + e.getMessage());
        }
    }
}
