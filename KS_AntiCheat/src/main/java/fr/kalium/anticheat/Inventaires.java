package fr.kalium.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
 * invsee / ecsee (cahier, catégorie 6) : inventaire et coffre de l'Ender d'un joueur, en ligne ou hors ligne ; le
 * staff voit, retire, ajoute (ex. ouvrir une shulker).
 *
 * - En ligne : vue reliée à l'inventaire du joueur (les changements des deux côtés sont recopiés) ; coffre de l'Ender
 *   ouvert tel quel.
 * - Hors ligne : l'état enregistré à sa dernière déconnexion (instantané pris à chaque déconnexion et toutes les 5
 *   minutes ; 1.0.1 : sans instantané, lu dans son fichier de sauvegarde, voir Sauvegardes) ; les changements du
 *   staff sont appliqués à sa prochaine connexion, avant qu'il puisse jouer.
 * - Journal (plugins/KS_AntiCheat/consultations.log) : chaque ouverture et ce qui a été retiré / ajouté (qui, quel
 *   joueur, quoi, quand), pour éviter les abus et pouvoir se dédouaner.
 *
 * Disposition de la vue d'inventaire (54 cases) : lignes 1 à 3 = inventaire, ligne 4 = barre, ligne 5 = casque,
 * plastron, jambières, bottes, seconde main ; le reste est bloqué.
 */
final class Inventaires implements Listener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Europe/Paris"));
    /** Case de la vue -> case de l'inventaire du joueur (PlayerInventory#getContents : 0-8 barre, 9-35, 36 bottes,
     *  37 jambières, 38 plastron, 39 casque, 40 seconde main). */
    private static final int[] VERS_JOUEUR = new int[54];

    static {
        java.util.Arrays.fill(VERS_JOUEUR, -1);
        for (int i = 0; i < 27; i++) {
            VERS_JOUEUR[i] = 9 + i;
        }
        for (int i = 0; i < 9; i++) {
            VERS_JOUEUR[27 + i] = i;
        }
        VERS_JOUEUR[36] = 39;
        VERS_JOUEUR[37] = 38;
        VERS_JOUEUR[38] = 37;
        VERS_JOUEUR[39] = 36;
        VERS_JOUEUR[40] = 40;
    }

    /** Vue ouverte par un membre du staff. */
    private static final class Vue implements InventoryHolder {
        UUID staff;
        UUID cible;
        String nomCible;
        boolean ender;
        boolean enLigne;
        Inventory inventaire;
        ItemStack[] avant;
        long dernierClic;

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final KSAntiCheat plugin;
    private final File dossierInstantanes;
    private final File dossierAttente;
    private final Map<UUID, Vue> vues = new HashMap<>();

    Inventaires(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.dossierInstantanes = new File(plugin.getDataFolder(), "instantanes");
        this.dossierAttente = new File(plugin.getDataFolder(), "en-attente");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::rafraichir, 10L, 10L);
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::instantane), 20L * 60 * 5, 20L * 60 * 5);
    }

    private Component t(String cle, String defaut, Object... paires) {
        return plugin.lang().c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ ouverture

    /** Ouvre l'inventaire (ender false) ou le coffre de l'Ender (ender true) de ce joueur. */
    void ouvrir(Player staff, OfflinePlayer cible, boolean ender) {
        fermerVue(staff);
        Vue v = new Vue();
        v.staff = staff.getUniqueId();
        v.cible = cible.getUniqueId();
        v.nomCible = cible.getName() == null ? "?" : cible.getName();
        v.ender = ender;
        Player enLigne = cible.getPlayer();
        v.enLigne = enLigne != null;
        String titre = (ender ? "Ender de " : "Inventaire de ") + v.nomCible + (v.enLigne ? "" : " (hors ligne)");
        if (enLigne != null && ender) {
            v.inventaire = enLigne.getEnderChest();
            v.avant = copie(enLigne.getEnderChest().getContents());
        } else {
            ItemStack[] contenu;
            if (enLigne != null) {
                contenu = enLigne.getInventory().getContents();
            } else {
                YamlConfiguration inst = lireInstantane(cible.getUniqueId());
                if (inst == null) {
                    // 1.0.1 : pas encore d'instantané : lecture de son fichier de sauvegarde (world/players/data).
                    inst = depuisSauvegarde(cible);
                }
                if (inst == null) {
                    staff.sendMessage(t("inv.aucun-2", "<red>Aucune sauvegarde lisible pour <nom>.", "nom", v.nomCible));
                    plugin.lang().saveIfNeeded();
                    return;
                }
                contenu = lireListe(inst, ender ? "ender" : "inventaire", ender ? 27 : 41);
            }
            v.inventaire = Bukkit.createInventory(v, ender ? 27 : 54, Component.text(titre));
            if (ender) {
                v.inventaire.setContents(copie(contenu));
            } else {
                remplirVue(v.inventaire, contenu);
            }
            v.avant = copie(contenu);
        }
        vues.put(staff.getUniqueId(), v);
        staff.openInventory(v.inventaire);
        journal(staff.getName(), v, "ouverture");
    }

    private void remplirVue(Inventory vue, ItemStack[] joueur) {
        for (int i = 0; i < 54; i++) {
            int j = VERS_JOUEUR[i];
            vue.setItem(i, j < 0 ? bloque() : j < joueur.length ? joueur[j] : null);
        }
    }

    private ItemStack[] depuisVue(Inventory vue) {
        ItemStack[] joueur = new ItemStack[41];
        for (int i = 0; i < 54; i++) {
            int j = VERS_JOUEUR[i];
            if (j >= 0) {
                ItemStack item = vue.getItem(i);
                joueur[j] = item == null ? null : item.clone();
            }
        }
        return joueur;
    }

    private static ItemStack bloque() {
        ItemStack vitre = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = vitre.getItemMeta();
        meta.displayName(Component.text(" "));
        vitre.setItemMeta(meta);
        return vitre;
    }

    private static ItemStack[] copie(ItemStack[] contenu) {
        ItemStack[] c = new ItemStack[contenu.length];
        for (int i = 0; i < contenu.length; i++) {
            c[i] = contenu[i] == null ? null : contenu[i].clone();
        }
        return c;
    }

    // ------------------------------------------------------------------ synchronisation (inventaire en ligne)

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Vue v) || v.ender) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory() && VERS_JOUEUR[event.getSlot()] < 0) {
            event.setCancelled(true);
            return;
        }
        v.dernierClic = System.currentTimeMillis();
        if (v.enLigne) {
            Bukkit.getScheduler().runTask(plugin, () -> versJoueur(v));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Vue v) || v.ender) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && VERS_JOUEUR[slot] < 0) {
                event.setCancelled(true);
                return;
            }
        }
        v.dernierClic = System.currentTimeMillis();
        if (v.enLigne) {
            Bukkit.getScheduler().runTask(plugin, () -> versJoueur(v));
        }
    }

    private void versJoueur(Vue v) {
        Player cible = Bukkit.getPlayer(v.cible);
        if (cible != null && vues.get(v.staff) == v) {
            cible.getInventory().setContents(depuisVue(v.inventaire));
        }
    }

    /** Toutes les 0,5 s : la vue suit l'inventaire du joueur (sauf juste après un clic du staff). */
    private void rafraichir() {
        long maintenant = System.currentTimeMillis();
        for (Vue v : vues.values()) {
            if (v.ender || !v.enLigne || maintenant - v.dernierClic < 1000) {
                continue;
            }
            Player cible = Bukkit.getPlayer(v.cible);
            Player staff = Bukkit.getPlayer(v.staff);
            if (cible == null || staff == null || staff.getItemOnCursor().getType() != Material.AIR) {
                continue;
            }
            ItemStack[] actuel = cible.getInventory().getContents();
            ItemStack[] affiche = depuisVue(v.inventaire);
            if (!java.util.Arrays.equals(actuel, affiche)) {
                remplirVue(v.inventaire, actuel);
            }
        }
    }

    // ------------------------------------------------------------------ fermeture

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Vue v = vues.get(event.getPlayer().getUniqueId());
        if (v != null && event.getInventory().equals(v.inventaire)) {
            terminer(event.getPlayer().getName(), v);
            vues.remove(event.getPlayer().getUniqueId());
        }
    }

    private void fermerVue(Player staff) {
        Vue v = vues.remove(staff.getUniqueId());
        if (v != null) {
            terminer(staff.getName(), v);
        }
    }

    /** Fin d'une consultation : journal des changements ; hors ligne : changements mis en attente. */
    private void terminer(String staff, Vue v) {
        ItemStack[] apres;
        if (v.ender) {
            apres = copie(v.inventaire.getContents());
        } else {
            apres = depuisVue(v.inventaire);
        }
        if (v.enLigne && !v.ender) {
            Player cible = Bukkit.getPlayer(v.cible);
            if (cible != null) {
                cible.getInventory().setContents(apres);
            }
        }
        String changements = difference(v.avant, apres);
        if (changements.isEmpty()) {
            return;
        }
        if (!v.enLigne) {
            mettreEnAttente(v.cible, v.ender, apres);
            YamlConfiguration inst = lireInstantane(v.cible);
            if (inst != null) {
                ecrireListe(inst, v.ender ? "ender" : "inventaire", apres);
                sauverInstantane(v.cible, inst);
            }
        }
        journal(staff, v, changements + (v.enLigne ? "" : " (appliqué à sa prochaine connexion)"));
    }

    /** Retirés / ajoutés, en comparant les objets (quantités additionnées). */
    static String difference(ItemStack[] avant, ItemStack[] apres) {
        Map<ItemStack, Integer> a = compter(avant);
        Map<ItemStack, Integer> b = compter(apres);
        List<String> retires = new ArrayList<>();
        List<String> ajoutes = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> e : a.entrySet()) {
            int d = e.getValue() - b.getOrDefault(e.getKey(), 0);
            if (d > 0) {
                retires.add(d + " x " + nom(e.getKey()));
            }
        }
        for (Map.Entry<ItemStack, Integer> e : b.entrySet()) {
            int d = e.getValue() - a.getOrDefault(e.getKey(), 0);
            if (d > 0) {
                ajoutes.add(d + " x " + nom(e.getKey()));
            }
        }
        List<String> parties = new ArrayList<>();
        if (!retires.isEmpty()) {
            parties.add("retirés : " + String.join(", ", retires));
        }
        if (!ajoutes.isEmpty()) {
            parties.add("ajoutés : " + String.join(", ", ajoutes));
        }
        return String.join(" ; ", parties);
    }

    private static Map<ItemStack, Integer> compter(ItemStack[] contenu) {
        Map<ItemStack, Integer> n = new LinkedHashMap<>();
        for (ItemStack item : contenu) {
            if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                n.merge(item.asOne(), item.getAmount(), Integer::sum);
            }
        }
        return n;
    }

    static String nom(ItemStack item) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return item.getType().getKey().getKey();
    }

    private void journal(String staff, Vue v, String action) {
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(plugin.getDataFolder(), "consultations.log"),
                StandardCharsets.UTF_8, true))) {
            out.println(DATE.format(Instant.now()) + " | " + staff + " | " + (v.ender ? "ecsee" : "invsee") + " | "
                    + v.nomCible + (v.enLigne ? " (en ligne)" : " (hors ligne)") + " | " + action);
        } catch (IOException e) {
            plugin.getLogger().warning("Journal des consultations : " + e.getMessage());
        }
    }

    /** Dernières lignes du journal des consultations (la plus récente d'abord). */
    List<String> dernieresConsultations(int nombre) {
        File f = new File(plugin.getDataFolder(), "consultations.log");
        if (!f.exists()) {
            return List.of();
        }
        try {
            List<String> lignes = java.nio.file.Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            List<String> r = new ArrayList<>(lignes.subList(Math.max(0, lignes.size() - nombre), lignes.size()));
            java.util.Collections.reverse(r);
            return r;
        } catch (IOException e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ instantanés et changements en attente

    private void instantane(Player joueur) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nom", joueur.getName());
        yaml.set("date", System.currentTimeMillis());
        ecrireListe(yaml, "inventaire", joueur.getInventory().getContents());
        ecrireListe(yaml, "ender", joueur.getEnderChest().getContents());
        sauverInstantane(joueur.getUniqueId(), yaml);
    }

    /**
     * 1.0.1 : instantané fait à partir du fichier de sauvegarde du joueur (Sauvegardes), enregistré pour la suite
     * (changements du staff, consultations suivantes) ; null si pas de fichier ou illisible.
     */
    private YamlConfiguration depuisSauvegarde(OfflinePlayer cible) {
        Sauvegardes.Contenu contenu;
        try {
            contenu = Sauvegardes.lire(cible.getUniqueId());
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("Sauvegarde de " + cible.getName() + " illisible : " + e);
            return null;
        }
        if (contenu == null) {
            return null;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nom", cible.getName());
        yaml.set("date", System.currentTimeMillis());
        yaml.set("source", "fichier de sauvegarde du joueur");
        ecrireListe(yaml, "inventaire", contenu.inventaire());
        ecrireListe(yaml, "ender", contenu.ender());
        sauverInstantane(cible.getUniqueId(), yaml);
        plugin.getLogger().info("Inventaire de " + cible.getName() + " lu dans son fichier de sauvegarde.");
        return yaml;
    }

    private static void ecrireListe(YamlConfiguration yaml, String cle, ItemStack[] contenu) {
        List<ItemStack> liste = new ArrayList<>();
        for (ItemStack item : contenu) {
            liste.add(item == null ? new ItemStack(Material.AIR) : item);
        }
        yaml.set(cle, liste);
    }

    private static ItemStack[] lireListe(YamlConfiguration yaml, String cle, int taille) {
        ItemStack[] contenu = new ItemStack[taille];
        List<?> liste = yaml.getList(cle, List.of());
        for (int i = 0; i < Math.min(taille, liste.size()); i++) {
            if (liste.get(i) instanceof ItemStack item && !item.getType().isAir()) {
                contenu[i] = item;
            }
        }
        return contenu;
    }

    private YamlConfiguration lireInstantane(UUID joueur) {
        File f = new File(dossierInstantanes, joueur + ".yml");
        return f.exists() ? YamlConfiguration.loadConfiguration(f) : null;
    }

    private void sauverInstantane(UUID joueur, YamlConfiguration yaml) {
        try {
            dossierInstantanes.mkdirs();
            yaml.save(new File(dossierInstantanes, joueur + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Instantané de " + joueur + " : " + e.getMessage());
        }
    }

    private void mettreEnAttente(UUID joueur, boolean ender, ItemStack[] contenu) {
        File f = new File(dossierAttente, joueur + ".yml");
        YamlConfiguration yaml = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        ecrireListe(yaml, ender ? "ender" : "inventaire", contenu);
        try {
            dossierAttente.mkdirs();
            yaml.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Changements en attente de " + joueur + " : " + e.getMessage());
        }
    }

    /** Connexion : les vues hors ligne de ce joueur se ferment (changements mis en attente), puis ils sont appliqués. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        for (Vue v : List.copyOf(vues.values())) {
            if (v.cible.equals(joueur.getUniqueId()) && !v.enLigne) {
                Player staff = Bukkit.getPlayer(v.staff);
                if (staff != null) {
                    staff.closeInventory();
                    staff.sendMessage(t("inv.connecte", "<yellow><nom> vient de se connecter : vue fermée, tes "
                            + "changements sont appliqués.", "nom", joueur.getName()));
                }
                fermerVueDe(v);
            }
        }
        File f = new File(dossierAttente, joueur.getUniqueId() + ".yml");
        if (!f.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        if (yaml.contains("inventaire")) {
            joueur.getInventory().setContents(lireListe(yaml, "inventaire", 41));
        }
        if (yaml.contains("ender")) {
            joueur.getEnderChest().setContents(lireListe(yaml, "ender", 27));
        }
        if (!f.delete()) {
            plugin.getLogger().warning("Impossible de supprimer " + f.getName());
        }
        plugin.getLogger().info("Changements du staff appliqués à l'inventaire de " + joueur.getName() + ".");
        plugin.lang().saveIfNeeded();
    }

    private void fermerVueDe(Vue v) {
        if (vues.get(v.staff) == v) {
            vues.remove(v.staff);
            Player staff = Bukkit.getPlayer(v.staff);
            terminer(staff == null ? "?" : staff.getName(), v);
        }
    }

    /** Déconnexion : instantané ; vues en ligne de ce joueur fermées ; vue du staff qui part fermée. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        fermerVue(joueur);
        for (Vue v : List.copyOf(vues.values())) {
            if (v.cible.equals(joueur.getUniqueId()) && v.enLigne) {
                Player staff = Bukkit.getPlayer(v.staff);
                fermerVueDe(v);
                if (staff != null) {
                    staff.closeInventory();
                }
            }
        }
        instantane(joueur);
    }

    /** Arrêt : vues fermées (changements enregistrés), instantanés des joueurs connectés. */
    void toutFermer() {
        for (Vue v : List.copyOf(vues.values())) {
            Player staff = Bukkit.getPlayer(v.staff);
            fermerVueDe(v);
            if (staff != null) {
                staff.closeInventory();
            }
        }
        Bukkit.getOnlinePlayers().forEach(this::instantane);
    }
}
