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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * invsee / ecsee (cahier, catégorie 6) : inventaire et coffre de l'Ender d'un joueur, en ligne ou hors ligne ; le
 * staff voit, retire, ajoute (ex. ouvrir une shulker).
 *
 * 1.1.1 (correctif de duplication signalé par LeKiwi06 : « quand je récupère un item, l'action n'est pas synchronisée
 * sur celle du joueur dans son ec, donc on peut dupliquer ») :
 * - Inventaire en ligne : synchronisé **case par case à chaque tick** (une case changée d'un seul côté est recopiée de
 *   l'autre ; changée des deux côtés : la version du joueur gagne, et ce que le staff y avait pris ou posé lui est repris
 *   ou rendu). Avant : la vue entière était recopiée, ce qui pouvait faire réapparaître un objet.
 * - Coffre de l'Ender en ligne : le coffre du joueur (copie de KS_EC_Extension) est fermé et enregistré, puis le joueur ne
 *   peut plus l'ouvrir tant que le staff le regarde ; la vue montre les 6 lignes (extension de KS_EC_Extension comprise,
 *   cases bloquées non modifiables) et est réécrite à la fermeture. Avant : le joueur travaillait sur sa copie en même
 *   temps que le staff sur le vrai coffre.
 * - Hors ligne : l'état enregistré (instantané à chaque déconnexion et toutes les 5 minutes ; sans instantané, son
 *   fichier de sauvegarde, extension comprise) ; changements appliqués à sa prochaine connexion.
 * - Journal (consultations.log) : chaque ouverture et ce qui a été retiré / ajouté.
 *
 * Disposition de la vue d'inventaire (54 cases) : lignes 1 à 3 = inventaire, ligne 4 = barre, ligne 5 = casque,
 * plastron, jambières, bottes, seconde main ; le reste est bloqué.
 */
final class Inventaires implements Listener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Europe/Paris"));
    private static final int EC = 27;
    /** Case de la vue -> case de l'inventaire du joueur (0-8 barre, 9-35, 36 bottes, 37 jambières, 38 plastron, 39 casque, 40 seconde main). */
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

    /** Vue ouverte par un membre du staff (toujours un coffre du plugin, jamais l'inventaire réel du joueur). */
    private static final class Vue implements InventoryHolder {
        UUID staff;
        UUID cible;
        String nomCible;
        boolean ender;
        boolean enLigne;
        /** Coffre de l'Ender : extension de KS_EC_Extension affichée (54 cases) et ses cases débloquées. */
        boolean extension;
        int masque;
        Inventory inventaire;
        ItemStack[] avant;
        /** Inventaire en ligne : dernier état commun (base de la synchronisation case par case). */
        ItemStack[] base;

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final KSAntiCheat plugin;
    private final File dossierInstantanes;
    private final File dossierAttente;
    private final Map<UUID, Vue> vues = new HashMap<>();
    /** Joueurs dont le coffre de l'Ender est regardé par le staff : ils ne peuvent pas l'ouvrir. */
    private final Set<UUID> ecVerrouilles = new HashSet<>();

    Inventaires(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.dossierInstantanes = new File(plugin.getDataFolder(), "instantanes");
        this.dossierAttente = new File(plugin.getDataFolder(), "en-attente");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::synchroniserTout, 1L, 1L);
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
        // Une seule vue à la fois par joueur et par type (deux copies modifiées en même temps dupliqueraient).
        for (Vue autre : vues.values()) {
            if (autre.cible.equals(cible.getUniqueId()) && autre.ender == ender) {
                Player qui = Bukkit.getPlayer(autre.staff);
                staff.sendMessage(t("inv.deja-ouvert", "<red><qui> regarde déjà ce <quoi> : réessaie plus tard.", "qui",
                        qui == null ? "?" : qui.getName(), "quoi", ender ? "coffre de l'Ender" : "inventaire"));
                plugin.lang().saveIfNeeded();
                return;
            }
        }
        Vue v = new Vue();
        v.staff = staff.getUniqueId();
        v.cible = cible.getUniqueId();
        v.nomCible = cible.getName() == null ? "?" : cible.getName();
        v.ender = ender;
        Player enLigne = cible.getPlayer();
        v.enLigne = enLigne != null;
        String titre = (ender ? "Ender de " : "Inventaire de ") + v.nomCible + (v.enLigne ? "" : " (hors ligne)");
        if (ender) {
            ItemStack[] haut;
            ItemStack[] bas;
            if (enLigne != null) {
                // Le joueur ne travaille plus sur une copie de son coffre pendant que le staff le regarde.
                Extension.fermer(enLigne);
                if (enLigne.getOpenInventory().getTopInventory().equals(enLigne.getEnderChest())) {
                    enLigne.closeInventory();
                }
                ecVerrouilles.add(enLigne.getUniqueId());
                haut = copie(enLigne.getEnderChest().getContents());
                v.extension = Extension.active();
                bas = v.extension ? Extension.lire(enLigne) : new ItemStack[EC];
                v.masque = v.extension ? Extension.masque(enLigne) : 0;
            } else {
                YamlConfiguration inst = instantaneOuSauvegarde(cible);
                if (inst == null) {
                    staff.sendMessage(t("inv.aucun-2", "<red>Aucune sauvegarde lisible pour <nom>.", "nom", v.nomCible));
                    plugin.lang().saveIfNeeded();
                    return;
                }
                haut = lireListe(inst, "ender", EC);
                v.extension = Extension.active() && inst.contains("masque");
                bas = lireListe(inst, "extension", EC);
                v.masque = inst.getInt("masque", 0);
            }
            v.inventaire = Bukkit.createInventory(v, v.extension ? 2 * EC : EC, Component.text(titre));
            for (int i = 0; i < EC; i++) {
                v.inventaire.setItem(i, haut[i]);
                if (v.extension) {
                    v.inventaire.setItem(EC + i, debloquee(v, i) ? bas[i] : Extension.image());
                }
            }
            v.avant = enderDepuisVue(v);
        } else {
            ItemStack[] contenu;
            if (enLigne != null) {
                contenu = copie(enLigne.getInventory().getContents());
            } else {
                YamlConfiguration inst = instantaneOuSauvegarde(cible);
                if (inst == null) {
                    staff.sendMessage(t("inv.aucun-2", "<red>Aucune sauvegarde lisible pour <nom>.", "nom", v.nomCible));
                    plugin.lang().saveIfNeeded();
                    return;
                }
                contenu = lireListe(inst, "inventaire", 41);
            }
            v.inventaire = Bukkit.createInventory(v, 54, Component.text(titre));
            remplirVue(v.inventaire, contenu);
            v.avant = copie(contenu);
            v.base = copie(contenu);
        }
        vues.put(staff.getUniqueId(), v);
        staff.openInventory(v.inventaire);
        journal(staff.getName(), v, "ouverture");
    }

    private YamlConfiguration instantaneOuSauvegarde(OfflinePlayer cible) {
        YamlConfiguration inst = lireInstantane(cible.getUniqueId());
        return inst != null ? inst : depuisSauvegarde(cible);
    }

    private static boolean debloquee(Vue v, int i) {
        return (v.masque & (1 << i)) != 0;
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

    /** Coffre de l'Ender de la vue : 27 cases (haut), puis 27 (extension ; cases bloquées : vides). */
    private ItemStack[] enderDepuisVue(Vue v) {
        ItemStack[] r = new ItemStack[v.extension ? 2 * EC : EC];
        for (int i = 0; i < r.length; i++) {
            ItemStack item = v.inventaire.getItem(i);
            r[i] = item == null || (i >= EC && !debloquee(v, i - EC)) ? null : item.clone();
        }
        return r;
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
            c[i] = contenu[i] == null || contenu[i].getType().isAir() ? null : contenu[i].clone();
        }
        return c;
    }

    // ------------------------------------------------------------------ clics dans les vues

    /** Case de la vue qui ne contient rien de réel (vitre de l'inventaire, case bloquée de l'extension). */
    private boolean caseFigee(Vue v, int slot) {
        if (slot < 0 || slot >= v.inventaire.getSize()) {
            return false;
        }
        if (!v.ender) {
            return VERS_JOUEUR[slot] < 0;
        }
        return slot >= EC && !debloquee(v, slot - EC);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Vue v)) {
            return;
        }
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            // Double-clic : ne jamais ramasser des images (vitres, cases bloquées).
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory() && caseFigee(v, event.getSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Vue v)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < v.inventaire.getSize() && caseFigee(v, slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Joueur dont le coffre de l'Ender est regardé par le staff : il ne peut pas l'ouvrir. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpenEnder(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST && event.getPlayer() instanceof Player p
                && ecVerrouilles.contains(p.getUniqueId()) && event.getInventory().equals(p.getEnderChest())) {
            event.setCancelled(true);
            p.sendMessage(t("inv.ender-indisponible", "<red>Ton coffre de l'Ender est indisponible un instant."));
            plugin.lang().saveIfNeeded();
        }
    }

    // ------------------------------------------------------------------ synchronisation de l'inventaire en ligne

    private static boolean pareil(ItemStack a, ItemStack b) {
        boolean videA = a == null || a.getType().isAir();
        boolean videB = b == null || b.getType().isAir();
        if (videA || videB) {
            return videA && videB;
        }
        return a.equals(b);
    }

    /** Chaque tick : inventaires en ligne regardés par le staff, case par case. */
    private void synchroniserTout() {
        for (Vue v : List.copyOf(vues.values())) {
            if (!v.ender && v.enLigne) {
                synchroniser(v);
            }
        }
    }

    private void synchroniser(Vue v) {
        Player cible = Bukkit.getPlayer(v.cible);
        Player staff = Bukkit.getPlayer(v.staff);
        if (cible == null || staff == null || vues.get(v.staff) != v) {
            return;
        }
        ItemStack[] joueur = cible.getInventory().getContents();
        ItemStack[] vue = depuisVue(v.inventaire);
        for (int j = 0; j < 41; j++) {
            boolean parStaff = !pareil(vue[j], v.base[j]);
            boolean parJoueur = !pareil(joueur[j], v.base[j]);
            if (parStaff && !parJoueur) {
                cible.getInventory().setItem(j, vue[j] == null ? null : vue[j].clone());
                v.base[j] = vue[j] == null ? null : vue[j].clone();
            } else if (parJoueur) {
                if (parStaff && !pareil(vue[j], joueur[j])) {
                    // Même case changée des deux côtés : le joueur gagne ; le staff reprend / rend son changement.
                    rejeter(staff, v.base[j], vue[j]);
                    plugin.getLogger().info("Invsee de " + v.nomCible + " : conflit sur une case, changement du staff "
                            + staff.getName() + " annulé.");
                }
                ItemStack reel = joueur[j] == null || joueur[j].getType().isAir() ? null : joueur[j].clone();
                v.inventaire.setItem(versVue(j), reel);
                v.base[j] = reel == null ? null : reel.clone();
            }
        }
    }

    private static int versVue(int caseJoueur) {
        for (int i = 0; i < 54; i++) {
            if (VERS_JOUEUR[i] == caseJoueur) {
                return i;
            }
        }
        return -1;
    }

    /** Annule le changement du staff sur une case : ce qu'il y a pris lui est repris, ce qu'il y a posé lui est rendu. */
    private void rejeter(Player staff, ItemStack base, ItemStack vue) {
        boolean videBase = base == null || base.getType().isAir();
        boolean videVue = vue == null || vue.getType().isAir();
        if (!videBase && !videVue && base.isSimilar(vue)) {
            int d = vue.getAmount() - base.getAmount();
            if (d > 0) {
                rendre(staff, vue.asQuantity(d));
            } else if (d < 0) {
                reprendre(staff, base.asQuantity(-d));
            }
            return;
        }
        if (!videBase) {
            reprendre(staff, base.clone());
        }
        if (!videVue) {
            rendre(staff, vue.clone());
        }
    }

    private void rendre(Player staff, ItemStack objet) {
        for (ItemStack reste : staff.getInventory().addItem(objet).values()) {
            staff.getWorld().dropItemNaturally(staff.getLocation(), reste);
        }
    }

    private void reprendre(Player staff, ItemStack objet) {
        int reste = objet.getAmount();
        ItemStack curseur = staff.getItemOnCursor();
        if (!curseur.getType().isAir() && curseur.isSimilar(objet)) {
            int pris = Math.min(reste, curseur.getAmount());
            curseur.setAmount(curseur.getAmount() - pris);
            staff.setItemOnCursor(curseur.getAmount() > 0 ? curseur : null);
            reste -= pris;
        }
        if (reste > 0) {
            Map<Integer, ItemStack> manque = staff.getInventory().removeItem(objet.asQuantity(reste));
            if (!manque.isEmpty()) {
                plugin.getLogger().warning("Invsee : impossible de reprendre " + manque.values().iterator().next().getAmount()
                        + " x " + nom(objet) + " à " + staff.getName() + " (déjà utilisés).");
            }
        }
    }

    // ------------------------------------------------------------------ fermeture

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Vue v = vues.get(event.getPlayer().getUniqueId());
        if (v != null && event.getInventory().equals(v.inventaire)) {
            vues.remove(event.getPlayer().getUniqueId());
            terminer(event.getPlayer().getName(), v);
        }
    }

    private void fermerVue(Player staff) {
        Vue v = vues.remove(staff.getUniqueId());
        if (v != null) {
            terminer(staff.getName(), v);
        }
    }

    /** Fin d'une consultation : changements écrits (ou mis en attente hors ligne), journal. */
    private void terminer(String staff, Vue v) {
        ItemStack[] apres;
        Player cible = Bukkit.getPlayer(v.cible);
        if (v.ender) {
            apres = enderDepuisVue(v);
            if (v.enLigne) {
                ecVerrouilles.remove(v.cible);
                if (cible != null) {
                    ItemStack[] haut = java.util.Arrays.copyOf(apres, EC);
                    cible.getEnderChest().setContents(haut);
                    if (v.extension) {
                        Extension.ecrire(cible, java.util.Arrays.copyOfRange(apres, EC, 2 * EC));
                    }
                }
            }
        } else {
            if (v.enLigne && cible != null) {
                vues.put(v.staff, v);
                synchroniser(v);
                vues.remove(v.staff);
            }
            apres = depuisVue(v.inventaire);
        }
        String changements = difference(v.avant, apres);
        if (changements.isEmpty()) {
            return;
        }
        if (!v.enLigne) {
            mettreEnAttente(v, apres);
            YamlConfiguration inst = lireInstantane(v.cible);
            if (inst != null) {
                if (v.ender) {
                    ecrireListe(inst, "ender", java.util.Arrays.copyOf(apres, EC));
                    if (v.extension) {
                        ecrireListe(inst, "extension", java.util.Arrays.copyOfRange(apres, EC, 2 * EC));
                    }
                } else {
                    ecrireListe(inst, "inventaire", apres);
                }
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
            if (item != null && !item.getType().isAir() && item.getType() != Material.GRAY_STAINED_GLASS_PANE
                    && item.getType() != Material.BARRIER) {
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
        if (ecVerrouilles.contains(joueur.getUniqueId())) {
            return; // son coffre est en cours de modification par le staff
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nom", joueur.getName());
        yaml.set("date", System.currentTimeMillis());
        ecrireListe(yaml, "inventaire", joueur.getInventory().getContents());
        ecrireListe(yaml, "ender", joueur.getEnderChest().getContents());
        if (Extension.active() && !Extension.ouvert(joueur)) {
            ecrireListe(yaml, "extension", Extension.lire(joueur));
            yaml.set("masque", Extension.masque(joueur));
        }
        sauverInstantane(joueur.getUniqueId(), yaml);
    }

    /**
     * 1.0.1 : instantané fait à partir du fichier de sauvegarde du joueur (Sauvegardes), enregistré pour la suite ;
     * null si pas de fichier ou illisible. 1.1.1 : extension de KS_EC_Extension comprise.
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
        if (contenu.extension() != null) {
            ecrireListe(yaml, "extension", contenu.extension());
            yaml.set("masque", contenu.masque());
        }
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

    private void mettreEnAttente(Vue v, ItemStack[] contenu) {
        File f = new File(dossierAttente, v.cible + ".yml");
        YamlConfiguration yaml = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        if (v.ender) {
            ecrireListe(yaml, "ender", java.util.Arrays.copyOf(contenu, EC));
            if (v.extension) {
                ecrireListe(yaml, "extension", java.util.Arrays.copyOfRange(contenu, EC, 2 * EC));
            }
        } else {
            ecrireListe(yaml, "inventaire", contenu);
        }
        try {
            dossierAttente.mkdirs();
            yaml.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Changements en attente de " + v.cible + " : " + e.getMessage());
        }
    }

    /** Connexion : les vues hors ligne de ce joueur se ferment (changements mis en attente), puis ils sont appliqués. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        for (Vue v : List.copyOf(vues.values())) {
            if (v.cible.equals(joueur.getUniqueId()) && !v.enLigne) {
                Player staff = Bukkit.getPlayer(v.staff);
                fermerVueDe(v);
                if (staff != null) {
                    staff.closeInventory();
                    staff.sendMessage(t("inv.connecte", "<yellow><nom> vient de se connecter : vue fermée, tes "
                            + "changements sont appliqués.", "nom", joueur.getName()));
                }
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
            joueur.getEnderChest().setContents(lireListe(yaml, "ender", EC));
        }
        if (yaml.contains("extension") && Extension.active()) {
            Extension.ecrire(joueur, lireListe(yaml, "extension", EC));
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

    /** Déconnexion : vues en ligne de ce joueur fermées (changements écrits), vue du staff qui part fermée, instantané. */
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
        ecVerrouilles.remove(joueur.getUniqueId());
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
