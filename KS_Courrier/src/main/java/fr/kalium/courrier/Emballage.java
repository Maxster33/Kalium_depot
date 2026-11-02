package fr.kalium.courrier;

import fr.kalium.menu.api.Contenant;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Coffre où l'on pose les objets d'un colis : 27 cases libres (rangées 1 à 3), barre d'actions en bas (Envoyer,
 * Annuler). Fermé sans envoyer : les objets reviennent dans l'inventaire (au sol s'il est plein). Envoyé : les objets
 * passent au serveur (Donnees) dans le même tick, le coffre est vidé.
 */
final class Emballage implements InventoryHolder {

    private static final int CASES = Poste.CASES_COLIS, AIDE = 30, ENVOYER = 31, ANNULER = 35;

    private final Menus menus;
    final UUID destinataire;
    final String nom;
    private Inventory inventaire;
    /** Objets envoyés ou rendus : plus rien à rendre. */
    private boolean fini;

    private Emballage(Menus menus, UUID destinataire, String nom) {
        this.menus = menus;
        this.destinataire = destinataire;
        this.nom = nom;
    }

    static void ouvrir(Menus menus, Player p, UUID destinataire, String nom) {
        Emballage e = new Emballage(menus, destinataire, nom);
        e.inventaire = Bukkit.createInventory(e, CASES + 9, menus.titreColis(nom));
        for (int i = CASES; i < CASES + 9; i++) {
            e.inventaire.setItem(i, Contenant.decor());
        }
        e.inventaire.setItem(AIDE, menus.aideColis());
        e.inventaire.setItem(ENVOYER, menus.boutonEnvoyer(p, destinataire));
        e.inventaire.setItem(ANNULER, menus.boutonAnnuler());
        p.openInventory(e.inventaire);
    }

    @Override
    public Inventory getInventory() {
        return inventaire;
    }

    List<ItemStack> contenu() {
        List<ItemStack> liste = new ArrayList<>();
        for (int i = 0; i < CASES; i++) {
            ItemStack o = inventaire.getItem(i);
            if (o != null && !o.getType().isAir()) {
                liste.add(o.clone());
            }
        }
        return liste;
    }

    /** Après l'envoi : les objets appartiennent au colis. */
    void vider() {
        for (int i = 0; i < CASES; i++) {
            inventaire.setItem(i, null);
        }
        fini = true;
    }

    /** Rend les objets au joueur (une seule fois). */
    void rendre(Player p) {
        if (fini) {
            return;
        }
        fini = true;
        List<ItemStack> objets = contenu();
        for (int i = 0; i < CASES; i++) {
            inventaire.setItem(i, null);
        }
        for (ItemStack reste : p.getInventory().addItem(objets.toArray(new ItemStack[0])).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), reste);
        }
    }

    void clic(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        int brut = event.getRawSlot();
        if (fini || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (brut >= CASES && brut < CASES + 9) {
            event.setCancelled(true);
            if (brut == ENVOYER) {
                menus.envoyerColis(p, this);
            } else if (brut == ANNULER) {
                rendre(p);
                Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(Emballage.class),
                        () -> p.closeInventory());
            }
            return;
        }
        if (brut >= CASES + 9 && event.isShiftClick()) {
            // Maj+clic depuis l'inventaire : seulement dans les 27 cases (jamais dans la barre).
            event.setCancelled(true);
            ItemStack o = event.getCurrentItem();
            if (o == null || o.getType().isAir()) {
                return;
            }
            ItemStack reste = o.clone();
            for (int i = 0; i < CASES && reste.getAmount() > 0; i++) {
                ItemStack c = inventaire.getItem(i);
                if (c != null && c.isSimilar(reste) && c.getAmount() < c.getMaxStackSize()) {
                    int n = Math.min(reste.getAmount(), c.getMaxStackSize() - c.getAmount());
                    c.setAmount(c.getAmount() + n);
                    inventaire.setItem(i, c);
                    reste.setAmount(reste.getAmount() - n);
                }
            }
            for (int i = 0; i < CASES && reste.getAmount() > 0; i++) {
                ItemStack c = inventaire.getItem(i);
                if (c == null || c.getType().isAir()) {
                    inventaire.setItem(i, reste.clone());
                    reste.setAmount(0);
                }
            }
            event.setCurrentItem(reste.getAmount() > 0 ? reste : null);
        }
    }

    void glisser(InventoryDragEvent event) {
        if (fini || event.getRawSlots().stream().anyMatch(s -> s >= CASES && s < CASES + 9)) {
            event.setCancelled(true);
        }
    }
}
