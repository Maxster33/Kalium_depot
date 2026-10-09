package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Gravite;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Revente suspecte (cahier, catégorie 6, étape 4 ; les 3 indices, seuils réglables ; alertes légères seulement) :
 *
 * 1. Récompense jamais récupérée (en attente sur /rewards depuis revente.jours-attente jours), ou récupérée puis
 *    donnée presque aussitôt (dans les revente.minutes-recompense minutes : /echange sans contrepartie, ou jetée au sol
 *    et ramassée par un autre joueur).
 * 2. Objets de valeur souvent donnés sans contrepartie : revente.dons dons en 7 jours (/echange ou au sol).
 * 3. Échanges à sens unique répétés entre les mêmes comptes : revente.sens-unique dons en 7 jours du même donneur au
 *    même receveur.
 *
 * Objet de valeur : type de revente.objets (netherite, élytres, étoiles du Nether...) ou objet custom de nos plugins
 * (marqueur d'un espace « ks_... », sauf les objets de menu). Contrepartie : un objet de valeur, ou au moins
 * revente.points-contrepartie points. Données : revente.yml (7 jours).
 */
final class Revente implements Listener {

    static final List<String> OBJETS_PAR_DEFAUT = List.of("NETHERITE_INGOT", "NETHERITE_BLOCK", "NETHERITE_SCRAP",
            "ANCIENT_DEBRIS", "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL", "NETHERITE_HOE",
            "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS", "ELYTRA", "NETHER_STAR",
            "BEACON", "TOTEM_OF_UNDYING", "ENCHANTED_GOLDEN_APPLE", "HEAVY_CORE", "MACE", "DRAGON_EGG", "DIAMOND_BLOCK",
            "EMERALD_BLOCK", "SPAWNER", "TRIDENT");
    private static final long SEPT_JOURS = 7L * 24 * 3600_000;
    /** 1.0.2 (LeKiwi06) : têtes de KS_Decapitator qui restent des objets de valeur (variantes rares, boss). */
    static final List<String> TETES_RARES_PAR_DEFAUT = List.of("axolotl.blue", "panda.brown", "sheep.pink",
            "mooshroom.brown", "rabbit.toast", "sheep.jeb", "creeper.charged", "goat.screaming", "skeleton_horse",
            "zombie_horse", "wither", "elder_guardian", "warden");
    private static final NamespacedKey TETE = new NamespacedKey("ks_decapitator", "tete");

    private record Don(UUID donneur, String nomDonneur, UUID receveur, String nomReceveur, long date) {
    }

    private record Recu(ItemStack objet, long date) {
    }

    private record Jete(UUID joueur, String nom, long date) {
    }

    private final KSAntiCheat plugin;
    private final File fichier;
    private final List<Don> dons = new ArrayList<>();
    /** Objets récupérés récemment sur /rewards, par joueur. */
    private final Map<UUID, List<Recu>> recus = new HashMap<>();
    /** Objets de valeur jetés au sol : entité -> qui l'a jeté. */
    private final Map<UUID, Jete> jetes = new HashMap<>();
    /** Récompenses en attente déjà signalées (joueur -> date de l'alerte). */
    private final Map<UUID, Long> attentesSignalees = new HashMap<>();

    Revente(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "revente.yml");
        charger();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::verifierAttentes, 20L * 120, 20L * 3600);
    }

    // ------------------------------------------------------------------ valeur

    boolean deValeur(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        List<String> types = plugin.getConfig().getStringList("revente.objets");
        if (types.isEmpty()) {
            types = OBJETS_PAR_DEFAUT;
        }
        if (types.contains(item.getType().name())) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        // 1.0.2 : têtes de mobs : seulement les rares (sinon trop d'alertes de dons pour des têtes ordinaires).
        String tete = item.getItemMeta().getPersistentDataContainer().get(TETE,
                org.bukkit.persistence.PersistentDataType.STRING);
        if (tete != null) {
            List<String> rares = plugin.getConfig().getStringList("revente.tetes-rares");
            if (rares.isEmpty()) {
                rares = TETES_RARES_PAR_DEFAUT;
            }
            for (String r : rares) {
                if (tete.equals(r) || tete.startsWith(r + ".")) {
                    return true;
                }
            }
            return false;
        }
        for (NamespacedKey cle : item.getItemMeta().getPersistentDataContainer().getKeys()) {
            if (cle.getNamespace().startsWith("ks_") && !cle.getNamespace().equals("ks_menu")) {
                return true;
            }
        }
        return false;
    }

    private boolean contientDeLaValeur(List<ItemStack> objets) {
        return objets.stream().anyMatch(this::deValeur);
    }

    // ------------------------------------------------------------------ signaux

    /** /echange abouti (KS_Economy 1.1.5). */
    void echange(Player a, Player b, List<ItemStack> objetsA, List<ItemStack> objetsB, long pointsA, long pointsB) {
        long seuil = plugin.getConfig().getLong("revente.points-contrepartie", 1000);
        if (contientDeLaValeur(objetsA) && !contientDeLaValeur(objetsB) && pointsB < seuil) {
            don(a, b, objetsA, "/echange");
        }
        if (contientDeLaValeur(objetsB) && !contientDeLaValeur(objetsA) && pointsA < seuil) {
            don(b, a, objetsB, "/echange");
        }
    }

    /** Récompense récupérée sur /rewards (KS_RewardsGUI 1.0.1). */
    void recompense(Player joueur, List<ItemStack> objets) {
        List<Recu> liste = recus.computeIfAbsent(joueur.getUniqueId(), u -> new ArrayList<>());
        long maintenant = System.currentTimeMillis();
        for (ItemStack o : objets) {
            liste.add(new Recu(o.asOne(), maintenant));
        }
        nettoyerRecus(liste, maintenant);
    }

    private void nettoyerRecus(List<Recu> liste, long maintenant) {
        long fenetre = Math.max(1, plugin.getConfig().getLong("revente.minutes-recompense", 30)) * 60_000L;
        liste.removeIf(r -> maintenant - r.date() > fenetre);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item entite = event.getItemDrop();
        if (deValeur(entite.getItemStack())) {
            jetes.put(entite.getUniqueId(), new Jete(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
                    System.currentTimeMillis()));
        }
        long maintenant = System.currentTimeMillis();
        jetes.values().removeIf(j -> maintenant - j.date() > 10 * 60_000L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player receveur)) {
            return;
        }
        Jete j = jetes.remove(event.getItem().getUniqueId());
        if (j == null || j.joueur().equals(receveur.getUniqueId())) {
            return;
        }
        Player donneur = Bukkit.getPlayer(j.joueur());
        don(donneur, j.joueur(), j.nom(), receveur, List.of(event.getItem().getItemStack()), "au sol");
    }

    // ------------------------------------------------------------------ dons et indices

    private void don(Player donneur, Player receveur, List<ItemStack> objets, String comment) {
        don(donneur, donneur.getUniqueId(), donneur.getName(), receveur, objets, comment);
    }

    private void don(Player donneurEnLigne, UUID donneur, String nomDonneur, Player receveur, List<ItemStack> objets,
                     String comment) {
        long maintenant = System.currentTimeMillis();
        dons.add(new Don(donneur, nomDonneur, receveur.getUniqueId(), receveur.getName(), maintenant));
        dons.removeIf(d -> maintenant - d.date() > SEPT_JOURS);
        sauver();
        String quoi = resume(objets);
        // 1. Récompense donnée presque aussitôt.
        List<Recu> liste = recus.get(donneur);
        if (liste != null) {
            nettoyerRecus(liste, maintenant);
            for (ItemStack o : objets) {
                if (liste.stream().anyMatch(r -> r.objet().isSimilar(o))) {
                    plugin.alertes().ajouter(donneur, nomDonneur, "Revente ? (récompense donnée)", Gravite.LEGERE,
                            "récompense de /rewards donnée à " + receveur.getName() + " (" + comment + ") peu après "
                                    + "l'avoir récupérée : " + quoi);
                    break;
                }
            }
        }
        // 2. Dons répétés.
        long parDonneur = dons.stream().filter(d -> d.donneur().equals(donneur)).count();
        int seuilDons = Math.max(1, plugin.getConfig().getInt("revente.dons", 5));
        if (parDonneur >= seuilDons && parDonneur % seuilDons == 0) {
            plugin.alertes().ajouter(donneur, nomDonneur, "Revente ? (dons répétés)", Gravite.LEGERE,
                    parDonneur + " dons d'objets de valeur sans contrepartie en 7 jours (dernier : " + quoi + " à "
                            + receveur.getName() + ", " + comment + ")");
        }
        // 3. Sens unique répété entre les mêmes comptes.
        long paire = dons.stream().filter(d -> d.donneur().equals(donneur) && d.receveur().equals(receveur.getUniqueId()))
                .count();
        int seuilPaire = Math.max(1, plugin.getConfig().getInt("revente.sens-unique", 3));
        if (paire >= seuilPaire && paire % seuilPaire == 0) {
            plugin.alertes().ajouter(donneur, nomDonneur, "Revente ? (sens unique)", Gravite.LEGERE,
                    paire + " dons sans contrepartie à " + receveur.getName() + " en 7 jours, jamais l'inverse ("
                            + comment + ")");
        }
    }

    private static String resume(List<ItemStack> objets) {
        List<String> parties = new ArrayList<>();
        for (ItemStack o : objets) {
            if (parties.size() == 4) {
                parties.add("...");
                break;
            }
            parties.add(o.getAmount() + " x " + Inventaires.nom(o));
        }
        return String.join(", ", parties);
    }

    /** Toutes les heures : récompenses en attente depuis trop longtemps (KS_RewardsGUI 1.0.1). */
    private void verifierAttentes() {
        if (Bukkit.getPluginManager().getPlugin("KS_RewardsGUI") == null) {
            return;
        }
        if (!(Bukkit.getPluginManager().getPlugin("KS_RewardsGUI") instanceof fr.kalium.rewardsgui.KSRewardsGUI r)
                || !r.isEnabled()) {
            return;
        }
        long jours = Math.max(1, plugin.getConfig().getLong("revente.jours-attente", 14));
        long maintenant = System.currentTimeMillis();
        Map<UUID, Long> attentes;
        try {
            attentes = r.plusAnciennesEnAttente();
        } catch (LinkageError e) {
            return;
        }
        attentes.forEach((joueur, date) -> {
            if (maintenant - date < jours * 24 * 3600_000L) {
                return;
            }
            Long deja = attentesSignalees.get(joueur);
            if (deja != null && maintenant - deja < SEPT_JOURS) {
                return;
            }
            attentesSignalees.put(joueur, maintenant);
            String nom = Bukkit.getOfflinePlayer(joueur).getName();
            plugin.alertes().ajouter(joueur, nom == null ? "?" : nom, "Revente ? (récompense jamais récupérée)",
                    Gravite.LEGERE, "récompense en attente sur /rewards depuis plus de " + jours + " jours");
        });
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        long maintenant = System.currentTimeMillis();
        for (Map<?, ?> m : yaml.getMapList("dons")) {
            try {
                Don d = new Don(UUID.fromString(String.valueOf(m.get("donneur"))), String.valueOf(m.get("nom-donneur")),
                        UUID.fromString(String.valueOf(m.get("receveur"))), String.valueOf(m.get("nom-receveur")),
                        ((Number) m.get("date")).longValue());
                if (maintenant - d.date() <= SEPT_JOURS) {
                    dons.add(d);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Don illisible ignoré : " + m);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> sortie = new ArrayList<>();
        for (Don d : dons) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("donneur", d.donneur().toString());
            m.put("nom-donneur", d.nomDonneur());
            m.put("receveur", d.receveur().toString());
            m.put("nom-receveur", d.nomReceveur());
            m.put("date", d.date());
            sortie.add(m);
        }
        yaml.set("dons", sortie);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer revente.yml : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ écouteurs des autres plugins

    /** Enregistré seulement si KS_Economy est là (sinon sa classe d'événement n'existe pas). */
    static final class EcouteEchanges implements Listener {
        private final Revente revente;

        EcouteEchanges(Revente revente) {
            this.revente = revente;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEchange(fr.kalium.economy.api.EchangeTermineEvent e) {
            revente.echange(e.a(), e.b(), e.objetsA(), e.objetsB(), e.pointsA(), e.pointsB());
            // 1.3.0 : journal des échanges (fiche d'un joueur, « Économie »).
            revente.plugin.echanges().noter(e.a(), e.b(), e.objetsA(), e.objetsB(), e.pointsA(), e.pointsB());
        }
    }

    /** Enregistré seulement si KS_RewardsGUI est là. */
    static final class EcouteRecompenses implements Listener {
        private final Revente revente;

        EcouteRecompenses(Revente revente) {
            this.revente = revente;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onRecompense(fr.kalium.rewardsgui.api.RecompenseRecupereeEvent e) {
            revente.recompense(e.joueur(), e.objets());
        }
    }
}
