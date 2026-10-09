package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.2.0 - ventes des boutiques (revue du 03/10/2026, validée par LeKiwi06) : statistiques (par boutique et par
 * magasin, total et 7 derniers jours), classement des magasins du catalogue, notification au propriétaire à chaque
 * vente (message s'il est connecté, résumé à sa connexion sinon). plugins/KS_Economy/ventes.yml (20 000 dernières
 * ventes, enregistré toutes les 30 secondes et à l'arrêt).
 */
final class Ventes implements Listener {

    static final long SEPT_JOURS = 7L * 24 * 3600_000;
    private static final int MAX = 20_000;
    private static final int MAX_NON_LUES = 50;

    record Vente(long date, String boutique, UUID proprio, UUID acheteur, String nomAcheteur, int lots, long points,
                 int objets) {
    }

    /** Statistiques d'une boutique ou d'un magasin. */
    record Stats(int ventes, int lots, int lots7j, long points, int objets) {
    }

    private final KSEconomy plugin;
    private final Lang lang;
    private final File fichier;
    private final List<Vente> liste = new ArrayList<>();
    /** Propriétaire hors ligne -> ventes à lui annoncer à sa connexion. */
    private final Map<UUID, List<String>> nonLues = new LinkedHashMap<>();
    private boolean modifie;

    Ventes(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.fichier = new File(plugin.getDataFolder(), "ventes.yml");
        charger();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::sauver, 20L * 30, 20L * 30);
    }

    /** Une vente vient d'avoir lieu : enregistrée, et annoncée au propriétaire. */
    void enregistrer(Boutique b, Player acheteur, int lots, Component prixTotal) {
        long points = b.enPoints() ? b.prixPoints * lots : 0;
        int objets = b.enPoints() ? 0 : b.prixQuantite * lots;
        liste.add(new Vente(System.currentTimeMillis(), b.id, b.proprio, acheteur.getUniqueId(), acheteur.getName(),
                lots, points, objets));
        if (liste.size() > MAX) {
            liste.subList(0, liste.size() - MAX).clear();
        }
        modifie = true;
        Component message = lang.c("vente.notification", "<green>Vente : <white><acheteur></white> a acheté <lots> "
                        + "lot(s) de « <boutique> » (<prix>).", "acheteur", acheteur.getName(), "lots", lots,
                "boutique", Boutiques.nomBoutique(b), "prix", prixTotal);
        Player proprio = Bukkit.getPlayer(b.proprio);
        if (proprio != null) {
            proprio.sendMessage(message);
        } else {
            List<String> l = nonLues.computeIfAbsent(b.proprio, u -> new ArrayList<>());
            l.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message));
            if (l.size() > MAX_NON_LUES) {
                l.subList(0, l.size() - MAX_NON_LUES).clear();
            }
        }
        lang.saveIfNeeded();
    }

    /** Connexion du propriétaire : résumé des ventes faites pendant son absence. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        List<String> l = nonLues.remove(p.getUniqueId());
        if (l == null || l.isEmpty()) {
            return;
        }
        modifie = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            p.sendMessage(lang.c("vente.absence", "<gold>Pendant ton absence : <n> vente(s) dans tes boutiques.",
                    "n", l.size()));
            int debut = Math.max(0, l.size() - 5);
            for (int i = debut; i < l.size(); i++) {
                p.sendMessage(Component.text(" - " + l.get(i)));
            }
            if (debut > 0) {
                p.sendMessage(lang.c("vente.absence-autres", "<gray>... et <n> autre(s) (statistiques : /magasin, "
                        + "Mes boutiques).", "n", debut));
            }
            lang.saveIfNeeded();
        }, 60L);
    }

    // ------------------------------------------------------------------ statistiques

    Stats boutique(String id) {
        return stats(v -> v.boutique().equals(id));
    }

    Stats magasin(UUID proprio) {
        return stats(v -> v.proprio().equals(proprio));
    }

    private Stats stats(java.util.function.Predicate<Vente> filtre) {
        long semaine = System.currentTimeMillis() - SEPT_JOURS;
        int ventes = 0;
        int lots = 0;
        int lots7j = 0;
        long points = 0;
        int objets = 0;
        for (Vente v : liste) {
            if (!filtre.test(v)) {
                continue;
            }
            ventes++;
            lots += v.lots();
            points += v.points();
            objets += v.objets();
            if (v.date() >= semaine) {
                lots7j += v.lots();
            }
        }
        return new Stats(ventes, lots, lots7j, points, objets);
    }

    /** 1.4.1 : les dernières ventes qui passent ce filtre, la plus récente d'abord. */
    List<Vente> dernieres(java.util.function.Predicate<Vente> filtre, int nombre) {
        List<Vente> r = new ArrayList<>();
        for (int i = liste.size() - 1; i >= 0 && r.size() < nombre; i--) {
            if (filtre.test(liste.get(i))) {
                r.add(liste.get(i));
            }
        }
        return r;
    }

    /** Lots vendus ces 7 derniers jours, par propriétaire (classement des magasins). */
    Map<UUID, Integer> lotsDeLaSemaine() {
        long semaine = System.currentTimeMillis() - SEPT_JOURS;
        Map<UUID, Integer> r = new HashMap<>();
        for (Vente v : liste) {
            if (v.date() >= semaine) {
                r.merge(v.proprio(), v.lots(), Integer::sum);
            }
        }
        return r;
    }

    /** Lots vendus ces 7 derniers jours, par boutique (tri « plus vendus » de la recherche). */
    Map<String, Integer> lotsDeLaSemaineParBoutique() {
        long semaine = System.currentTimeMillis() - SEPT_JOURS;
        Map<String, Integer> r = new HashMap<>();
        for (Vente v : liste) {
            if (v.date() >= semaine) {
                r.merge(v.boutique(), v.lots(), Integer::sum);
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (Map<?, ?> m : yaml.getMapList("ventes")) {
            try {
                liste.add(new Vente(((Number) m.get("date")).longValue(), String.valueOf(m.get("boutique")),
                        UUID.fromString(String.valueOf(m.get("proprio"))), UUID.fromString(String.valueOf(m.get("acheteur"))),
                        String.valueOf(m.get("nom-acheteur")), ((Number) m.get("lots")).intValue(),
                        ((Number) m.get("points")).longValue(), ((Number) m.get("objets")).intValue()));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Vente illisible ignorée : " + m);
            }
        }
        ConfigurationSection nl = yaml.getConfigurationSection("non-lues");
        if (nl != null) {
            for (String cle : nl.getKeys(false)) {
                try {
                    nonLues.put(UUID.fromString(cle), new ArrayList<>(nl.getStringList(cle)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ventes non lues ignorées : " + cle);
                }
            }
        }
    }

    void sauver() {
        if (!modifie) {
            return;
        }
        modifie = false;
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> sortie = new ArrayList<>();
        for (Vente v : liste) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", v.date());
            m.put("boutique", v.boutique());
            m.put("proprio", v.proprio().toString());
            m.put("acheteur", v.acheteur().toString());
            m.put("nom-acheteur", v.nomAcheteur());
            m.put("lots", v.lots());
            m.put("points", v.points());
            m.put("objets", v.objets());
            sortie.add(m);
        }
        yaml.set("ventes", sortie);
        nonLues.forEach((u, l) -> yaml.set("non-lues." + u, l));
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer ventes.yml : " + e.getMessage());
        }
    }
}
