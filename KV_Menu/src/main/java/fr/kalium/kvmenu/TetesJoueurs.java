package fr.kalium.kvmenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.kalium.kvplots.api.KanvasPlots;
import fr.kalium.kvplots.api.KanvasPlots.PlotInfo;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Visites « par joueur » : un coffre avec la tête de chaque joueur qui a au moins un plot (45 par page). Les menus de
 * KLM_Menu sont des dialogues, où l'on ne peut pas cliquer sur des têtes : cet écran est donc un coffre.
 */
final class TetesJoueurs implements Listener {

    private static final int PAR_PAGE = 45, PRECEDENT = 45, RETOUR = 49, SUIVANT = 53;

    /** Coffre ouvert : sa page, les joueurs affichés, et où revenir. */
    private static final class Ecran implements InventoryHolder {
        final int page;
        final List<UUID> joueurs;
        final Consumer<Player> retour;
        Inventory inventaire;

        Ecran(int page, List<UUID> joueurs, Consumer<Player> retour) {
            this.page = page;
            this.joueurs = joueurs;
            this.retour = retour;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final KanvasPlots plots;
    private final Lang lang;
    /** Ouvre la liste des plots d'un joueur (dialogue), avec l'écran où revenir. */
    interface OuvrirJoueur {
        void ouvrir(Player joueur, UUID createur, Consumer<Player> retour);
    }

    private final OuvrirJoueur ouvrirJoueur;

    TetesJoueurs(KanvasPlots plots, Lang lang, OuvrirJoueur ouvrirJoueur) {
        this.plots = plots;
        this.lang = lang;
        this.ouvrirJoueur = ouvrirJoueur;
    }

    private static Component sansItalique(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static String nom(UUID u) {
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    private ItemStack bouton(Material m, Component nom) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(sansItalique(nom));
        item.setItemMeta(meta);
        return item;
    }

    void ouvrir(Player joueur, int page, Consumer<Player> retour) {
        // Créateurs de plots, avec leurs plots, triés par pseudo.
        Map<UUID, List<PlotInfo>> parJoueur = new LinkedHashMap<>();
        for (PlotInfo p : plots.tousLesPlots()) parJoueur.computeIfAbsent(p.createur(), k -> new ArrayList<>()).add(p);
        List<UUID> joueurs = new ArrayList<>(parJoueur.keySet());
        joueurs.sort(Comparator.comparing(u -> nom(u).toLowerCase()));
        int pages = Math.max(1, (joueurs.size() + PAR_PAGE - 1) / PAR_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        Ecran ecran = new Ecran(page, joueurs, retour);
        Inventory inv = Bukkit.createInventory(ecran, 54,
                lang.c("heads.title", "Plots par joueur <gray>(<page>/<pages>)", "page", page + 1, "pages", pages));
        ecran.inventaire = inv;
        for (int i = 0; i < PAR_PAGE && page * PAR_PAGE + i < joueurs.size(); i++) {
            UUID u = joueurs.get(page * PAR_PAGE + i);
            List<PlotInfo> siens = parJoueur.get(u);
            long valides = siens.stream().filter(PlotInfo::valide).count();
            int points = siens.stream().mapToInt(PlotInfo::points).sum();
            ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) tete.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(u));
            meta.displayName(sansItalique(lang.c("heads.name", "<gold><name>", "name", nom(u))));
            meta.lore(List.of(
                    sansItalique(lang.c("heads.lore-plots", "<gray><n> plot(s), dont <v> validé(s)", "n", siens.size(), "v", valides)),
                    sansItalique(lang.c("heads.lore-points", "<gray><points> point(s)", "points", points)),
                    sansItalique(lang.c("heads.lore-click", "<yellow>Clic : voir ses plots"))));
            tete.setItemMeta(meta);
            inv.setItem(i, tete);
        }
        if (page > 0) inv.setItem(PRECEDENT, bouton(Material.ARROW, lang.c("heads.previous", "<yellow>Page précédente")));
        inv.setItem(RETOUR, bouton(Material.BARRIER, lang.c("menu.back", "<gray>Retour")));
        if (page < pages - 1) inv.setItem(SUIVANT, bouton(Material.ARROW, lang.c("heads.next", "<yellow>Page suivante")));
        joueur.openInventory(inv);
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Ecran ecran)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player joueur) || e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getSlot();
        if (slot < PAR_PAGE) {
            int index = ecran.page * PAR_PAGE + slot;
            if (index < ecran.joueurs.size()) {
                joueur.closeInventory();
                ouvrirJoueur.ouvrir(joueur, ecran.joueurs.get(index), q -> ouvrir(q, ecran.page, ecran.retour));
            }
        } else if (slot == PRECEDENT && ecran.page > 0) {
            ouvrir(joueur, ecran.page - 1, ecran.retour);
        } else if (slot == SUIVANT) {
            ouvrir(joueur, ecran.page + 1, ecran.retour);
        } else if (slot == RETOUR) {
            joueur.closeInventory();
            ecran.retour.accept(joueur);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Ecran) e.setCancelled(true);
    }
}
