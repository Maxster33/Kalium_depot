package fr.kalium.meule;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * KS_Meule (LeKiwi06, 11/10/2026) : retirer les malédictions d'un objet à la meule.
 *
 * « il faut faire sneak+click sur une meule avec l'item concerné, cela coute 45 niveaux par malédiction (interface de
 * confirmation avant) ».
 *
 * - Accroupi, clic droit sur une meule, l'objet maudit en main : un menu de confirmation s'ouvre (l'objet au centre,
 *   un bouton par malédiction, « Tout retirer » s'il y en a plusieurs, « Annuler »). Sans malédiction sur l'objet en
 *   main, rien ne change : le clic fait ce qu'il fait d'habitude.
 * - 45 niveaux d'expérience par malédiction retirée (config.yml : niveaux-par-malediction), gratuit en créatif. Les
 *   autres enchantements, le nom et l'usure de l'objet ne changent pas.
 * - Un livre enchanté qui porte une malédiction compte aussi ; s'il ne lui reste aucun enchantement, il redevient un
 *   livre.
 */
public final class KSMeule extends JavaPlugin implements Listener {

    private static final String MARQUE = "meule";

    private Lang lang;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        getServer().getPluginManager().registerEvents(this, this);
        lang.saveIfNeeded();
    }

    private int niveaux() {
        return Math.max(0, getConfig().getInt("niveaux-par-malediction", 45));
    }

    /** Malédictions de l'objet (posées sur lui, ou gardées dans un livre enchanté). */
    private static List<Enchantment> maledictions(ItemStack objet) {
        List<Enchantment> liste = new ArrayList<>();
        if (objet == null || objet.getType().isAir() || !objet.hasItemMeta()) {
            return liste;
        }
        ItemMeta meta = objet.getItemMeta();
        meta.getEnchants().keySet().stream().filter(Enchantment::isCursed).forEach(liste::add);
        if (meta instanceof EnchantmentStorageMeta livre) {
            livre.getStoredEnchants().keySet().stream().filter(e -> e.isCursed() && !liste.contains(e)).forEach(liste::add);
        }
        return liste;
    }

    private static boolean assez(Player joueur, int cout) {
        return joueur.getGameMode() == GameMode.CREATIVE || joueur.getLevel() >= cout;
    }

    // Le clic est traité même s'il a été refusé par une protection (zone du spawn, claim d'un autre) : la meule ne
    // s'ouvre pas, aucun bloc ne change, et sans cela une meule publique ne servirait à rien.
    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.GRINDSTONE
                || !event.getPlayer().isSneaking() || maledictions(event.getItem()).isEmpty()) {
            return;
        }
        // L'objet en main ne doit pas servir (poser une citrouille maudite, par exemple).
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline()) {
                ouvrir(joueur);
            }
        });
    }

    /** Menu de confirmation pour l'objet tenu en main. */
    private void ouvrir(Player joueur) {
        ItemStack objet = joueur.getInventory().getItemInMainHand().clone();
        List<Enchantment> maledictions = maledictions(objet);
        if (maledictions.isEmpty()) {
            Contenant.fermer(joueur);
            return;
        }
        int prix = niveaux();
        Contenant menu = Contenant.pour(joueur, 2, lang.c("titre", "<dark_gray>Retirer une malédiction"), MARQUE);
        menu.barre();
        menu.poser(4, objet, null);
        int place = maledictions.size() == 1 ? 3 : 1;
        for (Enchantment malediction : maledictions) {
            if (place > 4) {
                break;
            }
            boolean assez = assez(joueur, prix);
            menu.poser(menu.bas(place++), Contenant.objet(Material.ENCHANTED_BOOK, lang.c(assez ? "retirer" : "retirer-manque",
                    assez ? "<green>Retirer (<cout> niveaux)" : "<red>Retirer (<cout> niveaux)", "cout", prix), List.of(
                    malediction.description().colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.RED),
                    lang.c("tes-niveaux", "<gray>Tu as <n> niveaux.", "n", joueur.getLevel()))),
                    j -> retirer(j, objet, List.of(malediction)));
        }
        if (maledictions.size() > 1) {
            int total = prix * maledictions.size();
            boolean assez = assez(joueur, total);
            menu.poser(menu.bas(5), Contenant.objet(Material.GRINDSTONE, lang.c(assez ? "tout" : "tout-manque",
                    assez ? "<green>Tout retirer (<cout> niveaux)" : "<red>Tout retirer (<cout> niveaux)", "cout", total), List.of(
                    lang.c("tout-1", "<gray><n> malédictions", "n", maledictions.size()),
                    lang.c("tes-niveaux", "<gray>Tu as <n> niveaux.", "n", joueur.getLevel()))),
                    j -> retirer(j, objet, maledictions));
        }
        menu.poser(menu.bas(maledictions.size() == 1 ? 5 : 7), Contenant.objet(Material.BARRIER,
                lang.c("annuler", "<red>Annuler"), List.of()), Contenant::fermer);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Retire ces malédictions de l'objet en main, s'il est toujours celui du menu et si le joueur a les niveaux. */
    private void retirer(Player joueur, ItemStack attendu, List<Enchantment> maledictions) {
        ItemStack objet = joueur.getInventory().getItemInMainHand();
        if (!attendu.equals(objet)) {
            Contenant.message(joueur, lang.c("change", "<red>L'objet en main a changé."), true);
            Contenant.fermer(joueur);
            lang.saveIfNeeded();
            return;
        }
        int cout = niveaux() * maledictions.size();
        if (!assez(joueur, cout)) {
            Contenant.message(joueur, lang.c("manque", "<red>Il te faut <cout> niveaux d'expérience.", "cout", cout), true);
            lang.saveIfNeeded();
            return;
        }
        ItemMeta meta = objet.getItemMeta();
        for (Enchantment malediction : maledictions) {
            meta.removeEnchant(malediction);
            if (meta instanceof EnchantmentStorageMeta livre) {
                livre.removeStoredEnchant(malediction);
            }
        }
        objet.setItemMeta(meta);
        // Un livre enchanté sans enchantement n'existe pas dans le jeu : il redevient un livre.
        if (meta instanceof EnchantmentStorageMeta livre && !livre.hasStoredEnchants()) {
            objet = new ItemStack(Material.BOOK, objet.getAmount());
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            joueur.giveExpLevels(-cout);
        }
        joueur.getInventory().setItemInMainHand(objet);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1f);
        joueur.sendActionBar(maledictions.size() == 1 ? lang.c("fait", "<green>Malédiction retirée.")
                : lang.c("fait-plusieurs", "<green><n> malédictions retirées.", "n", maledictions.size()));
        getLogger().info(joueur.getName() + " retire " + maledictions.size() + " malédiction(s) de " + objet.getType()
                + " pour " + cout + " niveaux.");
        // S'il reste une malédiction, le menu est rempli à nouveau ; sinon il se ferme.
        Contenant.apres(() -> ouvrir(joueur));
        lang.saveIfNeeded();
    }
}
