package fr.kalium.meule;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
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
 * « il faut faire sneak+click sur une meule [...], cela coute 45 niveaux par malédiction (interface de confirmation
 * avant) », puis : « fait en sorte que le menu soit uniquement a main nu et qu'on selectionne la pièce d'armure dans
 * l'inventaire (pas le stuff porté) ».
 *
 * - Accroupi, clic droit sur une meule, la main vide : le menu s'ouvre (pas la meule). Avec un objet en main, rien ne
 *   change : le clic fait ce qu'il fait d'habitude.
 * - Dans le menu, un clic sur un objet maudit de son inventaire le choisit (l'armure portée et la main secondaire n'y
 *   sont pas : un coffre ne montre que l'inventaire). L'objet reste à sa place, le menu n'en montre que l'image.
 * - Un bouton par malédiction, « Tout retirer » s'il y en a plusieurs, « Annuler ». 45 niveaux d'expérience par
 *   malédiction retirée (config.yml : niveaux-par-malediction), gratuit en créatif. Les autres enchantements, le nom
 *   et l'usure de l'objet ne changent pas.
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
                || !event.getPlayer().isSneaking()
                || !event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }
        // Main vide : sans cela, la meule s'ouvrirait.
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline()) {
                ouvrir(joueur, -1);
            }
        });
    }

    /** Un clic sur un objet de son inventaire, menu ouvert : il est choisi s'il porte une malédiction. */
    @EventHandler
    public void onChoix(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur) || Contenant.ouvert(joueur, MARQUE) == null
                || !event.getView().getBottomInventory().equals(event.getClickedInventory())) {
            return;
        }
        event.setCancelled(true);
        ItemStack objet = event.getCurrentItem();
        if (objet == null || objet.getType().isAir()) {
            return;
        }
        if (maledictions(objet).isEmpty()) {
            Contenant.message(joueur, lang.c("sans-malediction", "<red>Cet objet n'a pas de malédiction."), true);
            lang.saveIfNeeded();
            return;
        }
        int place = event.getSlot();
        Contenant.apres(() -> ouvrir(joueur, place));
    }

    /**
     * Le menu : sans objet choisi (place -1, ou objet sans malédiction), une invitation à en choisir un ; sinon l'objet
     * et les boutons de confirmation.
     */
    private void ouvrir(Player joueur, int place) {
        ItemStack choisi = place < 0 ? null : joueur.getInventory().getItem(place);
        List<Enchantment> maledictions = maledictions(choisi);
        int prix = niveaux();
        Contenant menu = Contenant.pour(joueur, 2, lang.c("titre", "<dark_gray>Retirer une malédiction"), MARQUE);
        menu.barre();
        if (maledictions.isEmpty()) {
            menu.poser(4, Contenant.objet(Material.GRINDSTONE, lang.c("choisir", "<gold>Retirer une malédiction"), List.of(
                    lang.c("choisir-1", "<gray>Clique sur un objet maudit"),
                    lang.c("choisir-2", "<gray>de ton inventaire."),
                    lang.c("choisir-3", "<gray><cout> niveaux par malédiction.", "cout", prix))), null);
            menu.poser(menu.bas(4), Contenant.objet(Material.BARRIER, lang.c("fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
            return;
        }
        ItemStack objet = choisi.clone();
        menu.poser(4, objet, null);
        int colonne = maledictions.size() == 1 ? 3 : 1;
        for (Enchantment malediction : maledictions) {
            if (colonne > 4) {
                break;
            }
            boolean assez = assez(joueur, prix);
            menu.poser(menu.bas(colonne++), Contenant.objet(Material.ENCHANTED_BOOK, lang.c(assez ? "retirer" : "retirer-manque",
                    assez ? "<green>Retirer (<cout> niveaux)" : "<red>Retirer (<cout> niveaux)", "cout", prix), List.of(
                    malediction.description().colorIfAbsent(NamedTextColor.RED),
                    lang.c("tes-niveaux", "<gray>Tu as <n> niveaux.", "n", joueur.getLevel()))),
                    j -> retirer(j, place, objet, List.of(malediction)));
        }
        if (maledictions.size() > 1) {
            int total = prix * maledictions.size();
            boolean assez = assez(joueur, total);
            menu.poser(menu.bas(5), Contenant.objet(Material.GRINDSTONE, lang.c(assez ? "tout" : "tout-manque",
                    assez ? "<green>Tout retirer (<cout> niveaux)" : "<red>Tout retirer (<cout> niveaux)", "cout", total), List.of(
                    lang.c("tout-1", "<gray><n> malédictions", "n", maledictions.size()),
                    lang.c("tes-niveaux", "<gray>Tu as <n> niveaux.", "n", joueur.getLevel()))),
                    j -> retirer(j, place, objet, maledictions));
        }
        menu.poser(menu.bas(maledictions.size() == 1 ? 5 : 7), Contenant.objet(Material.BARRIER,
                lang.c("annuler", "<red>Annuler"), List.of()), Contenant::fermer);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Retire ces malédictions de l'objet choisi, s'il est toujours à sa place et si le joueur a les niveaux. */
    private void retirer(Player joueur, int place, ItemStack attendu, List<Enchantment> maledictions) {
        ItemStack objet = joueur.getInventory().getItem(place);
        if (!attendu.equals(objet)) {
            Contenant.message(joueur, lang.c("change", "<red>Cet objet n'est plus à sa place."), true);
            Contenant.apres(() -> ouvrir(joueur, -1));
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
        joueur.getInventory().setItem(place, objet);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1f);
        joueur.sendActionBar(maledictions.size() == 1 ? lang.c("fait", "<green>Malédiction retirée.")
                : lang.c("fait-plusieurs", "<green><n> malédictions retirées.", "n", maledictions.size()));
        getLogger().info(joueur.getName() + " retire " + maledictions.size() + " malédiction(s) de " + objet.getType()
                + " pour " + cout + " niveaux.");
        // S'il reste une malédiction sur l'objet, ses boutons sont remis ; sinon le menu attend un autre objet.
        Contenant.apres(() -> ouvrir(joueur, place));
        lang.saveIfNeeded();
    }
}
