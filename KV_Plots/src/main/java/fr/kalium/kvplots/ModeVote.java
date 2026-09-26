package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import fr.kalium.kvplots.api.KanvasPlots.Refus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Mode vote : en entrant dans un plot validé qu'il n'a pas encore noté (et dont il n'est ni créateur ni éditeur, même
 * ancien), le joueur voit son inventaire mis de côté et reçoit les 5 terracottas sur les cases 3 à 7 de la barre
 * d'objets (rouge 1 ... vert foncé 5), et une poudre de blaze en case 9 pour signaler le plot (menu de KV_Menu). Clic
 * avec une terracotta = vote. Son inventaire lui est rendu dès qu'il vote,
 * sort du plot, change de monde ou se déconnecte. L'inventaire mis de côté est aussi écrit sur le disque
 * (inventaires/<uuid>.yml) pour être rendu à la connexion suivante si le serveur s'arrête entre-temps.
 */
final class ModeVote implements Listener {

    private static final Material[] TERRACOTTAS = {Material.RED_TERRACOTTA, Material.ORANGE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA, Material.GREEN_TERRACOTTA};
    private static final NamedTextColor[] COULEURS = {NamedTextColor.RED, NamedTextColor.GOLD,
            NamedTextColor.YELLOW, NamedTextColor.GREEN, NamedTextColor.DARK_GREEN};
    /** Cases 3 à 7 de la barre d'objets (indices 2 à 6). */
    private static final int PREMIERE_CASE = 2;
    /** Case 9 : poudre de blaze pour signaler le plot (le menu de signalement est ouvert par KV_Menu). */
    private static final int CASE_SIGNALEMENT = 8;

    private final KVPlots plugin;
    private final NamespacedKey cleNote, cleSignaler;
    private final File dossier;
    /** Joueur en mode vote -> plot noté. */
    private final Map<UUID, Integer> enVote = new HashMap<>();

    ModeVote(KVPlots plugin) {
        this.plugin = plugin;
        this.cleNote = new NamespacedKey(plugin, "note");
        this.cleSignaler = new NamespacedKey(plugin, "signaler");
        this.dossier = new File(plugin.getDataFolder(), "inventaires");
    }

    boolean enVote(Player joueur) {
        return enVote.containsKey(joueur.getUniqueId());
    }

    private ItemStack terracotta(int note) {
        ItemStack item = new ItemStack(TERRACOTTAS[note - 1]);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Noter " + note + "/5", COULEURS[note - 1], TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Clic pour donner " + note + " point" + (note > 1 ? "s" : "") + " à ce plot.",
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(cleNote, PersistentDataType.INTEGER, note);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack poudreSignalement() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Signaler ce plot", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Clic : signaler un problème au staff.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(cleSignaler, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean estSignalement(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleSignaler, PersistentDataType.BYTE);
    }

    /** Plot en cours de notation par ce joueur (0 = pas en mode vote). */
    int plotEnVote(Player joueur) {
        Integer id = enVote.get(joueur.getUniqueId());
        return id == null ? 0 : id;
    }

    private int note(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer n = item.getItemMeta().getPersistentDataContainer().get(cleNote, PersistentDataType.INTEGER);
        return n == null ? 0 : n;
    }

    /** Plot que le joueur devrait être en train de noter là où il se trouve, ou null. */
    private Plot plotANoter(Player joueur) {
        if (!joueur.getWorld().equals(plugin.monde())) return null;
        Plot p = plugin.plotEn(joueur.getLocation().getBlockX(), joueur.getLocation().getBlockZ());
        if (p == null || !p.votable(joueur.getUniqueId()) || p.votes.containsKey(joueur.getUniqueId())) return null;
        return p;
    }

    /** Entre, change de plot ou sort du mode vote selon l'endroit où se trouve le joueur. */
    void verifier(Player joueur) {
        if (!joueur.isOnline()) return;
        Plot voulu = plotANoter(joueur);
        Integer actuel = enVote.get(joueur.getUniqueId());
        if (voulu == null) {
            if (actuel != null) sortir(joueur);
        } else if (actuel == null) {
            entrer(joueur, voulu);
        } else if (actuel != voulu.id) {
            enVote.put(joueur.getUniqueId(), voulu.id);
            annoncer(joueur, voulu);
        }
    }

    /** Après un changement de plot (validation, vote, travaux...) : tous les joueurs sont revérifiés. */
    void rafraichir() {
        for (Player joueur : plugin.getServer().getOnlinePlayers()) verifier(joueur);
    }

    private void annoncer(Player joueur, Plot p) {
        joueur.sendActionBar(Component.text("Plot n°" + p.id + " de " + KVPlots.nom(p.createur)
                + " : note-le avec une terracotta (1 à 5) !", NamedTextColor.GOLD));
    }

    private void entrer(Player joueur, Plot p) {
        if (!mettreDeCote(joueur)) return;
        PlayerInventory inv = joueur.getInventory();
        inv.clear();
        for (int note = 1; note <= 5; note++) inv.setItem(PREMIERE_CASE + note - 1, terracotta(note));
        inv.setItem(CASE_SIGNALEMENT, poudreSignalement());
        joueur.setItemOnCursor(null);
        enVote.put(joueur.getUniqueId(), p.id);
        annoncer(joueur, p);
    }

    private void sortir(Player joueur) {
        enVote.remove(joueur.getUniqueId());
        rendre(joueur);
    }

    /** Pour l'arrêt du plugin : chacun récupère son inventaire. */
    void toutRendre() {
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            if (enVote(joueur)) sortir(joueur);
        }
    }

    // --- Inventaire mis de côté (mémoire + disque) ---

    private File fichier(UUID joueur) {
        return new File(dossier, joueur + ".yml");
    }

    private boolean mettreDeCote(Player joueur) {
        File f = fichier(joueur.getUniqueId());
        if (f.exists()) {
            // Inventaire d'un mode vote précédent pas encore rendu : on le rend d'abord, jamais on ne l'écrase.
            rendre(joueur);
            if (f.exists()) return false;
        }
        List<String> contenu = new ArrayList<>();
        for (ItemStack item : joueur.getInventory().getContents()) {
            contenu.add(item == null || item.getType().isAir() ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("contenu", contenu);
        try {
            dossier.mkdirs();
            yml.save(f);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Inventaire de " + joueur.getName() + " non enregistré : pas de mode vote", e);
            return false;
        }
    }

    /** Rend l'inventaire mis de côté (s'il y en a un) et efface le fichier. */
    private void rendre(Player joueur) {
        File f = fichier(joueur.getUniqueId());
        PlayerInventory inv = joueur.getInventory();
        if (!f.exists()) {
            for (int i = 0; i < inv.getSize(); i++) {
                if (note(inv.getItem(i)) > 0 || estSignalement(inv.getItem(i))) inv.setItem(i, null);
            }
            return;
        }
        List<String> contenu = YamlConfiguration.loadConfiguration(f).getStringList("contenu");
        ItemStack[] items = new ItemStack[inv.getContents().length];
        for (int i = 0; i < items.length && i < contenu.size(); i++) {
            String s = contenu.get(i);
            items[i] = s.isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
        }
        inv.setContents(items);
        joueur.setItemOnCursor(null);
        if (!f.delete()) plugin.getLogger().warning("Impossible d'effacer " + f.getName());
    }

    // --- Événements ---

    private void verifierPlusTard(Player joueur) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> verifier(joueur), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            verifier(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        verifierPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        verifierPlusTard(e.getPlayer());
    }

    /** Un inventaire resté de côté (arrêt du serveur en plein vote) est rendu à la connexion. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if (fichier(e.getPlayer().getUniqueId()).exists()) rendre(e.getPlayer());
        verifierPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        if (enVote(e.getPlayer())) sortir(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player joueur = e.getPlayer();
        if (!enVote(joueur)) return;
        e.setCancelled(true); // ni pose de bloc, ni autre action avec les objets de vote
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) return;
        int note = note(e.getItem());
        if (note == 0) return;
        Plot p = plugin.plots().parId(enVote.get(joueur.getUniqueId()));
        if (p == null) {
            verifier(joueur);
            return;
        }
        try {
            plugin.voter(joueur, p, note);
            joueur.sendMessage("§aTu as donné " + note + "/5 au plot n°" + p.id + " de " + KVPlots.nom(p.createur)
                    + ". §7Pour changer ta note : bouton « Voter » du menu.");
        } catch (Refus r) {
            joueur.sendMessage("§c" + r.getMessage());
        }
        verifier(joueur);
    }

    /** En mode vote, l'inventaire est figé (créatif compris : InventoryCreativeEvent hérite de InventoryClickEvent). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player joueur && enVote(joueur)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player joueur && enVote(joueur)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (enVote(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (enVote(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player joueur && enVote(joueur)) e.setCancelled(true);
    }
}
