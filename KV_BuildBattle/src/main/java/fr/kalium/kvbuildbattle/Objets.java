package fr.kalium.kvbuildbattle;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Objets du Build Battle, reconnus par une étiquette cachée (action). */
final class Objets {

    static final String LANCER = "lancer", THEME = "theme", SIGNALER = "signaler", NOTE = "note-";

    private static final Material[] TERRACOTTAS = {Material.RED_TERRACOTTA, Material.ORANGE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA, Material.GREEN_TERRACOTTA};
    private static final NamedTextColor[] COULEURS = {NamedTextColor.RED, NamedTextColor.GOLD,
            NamedTextColor.YELLOW, NamedTextColor.GREEN, NamedTextColor.DARK_GREEN};

    private final NamespacedKey cle;

    Objets(KVBuildBattle plugin) {
        this.cle = new NamespacedKey(plugin, "action");
    }

    private ItemStack objet(Material m, String action, Component nom, String info) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(info, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(cle, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack lancer() {
        return objet(Material.EMERALD, LANCER, Component.text("Lancer la partie", NamedTextColor.GREEN, TextDecoration.BOLD),
                "Clic : lancer la partie (au moins 2 équipes).");
    }

    ItemStack theme(boolean ecriture) {
        return objet(ecriture ? Material.WRITABLE_BOOK : Material.PAPER, THEME,
                Component.text(ecriture ? "Proposer un thème" : "Voter pour le thème", NamedTextColor.GOLD, TextDecoration.BOLD),
                "Clic : rouvrir le choix du thème.");
    }

    ItemStack signaler() {
        return objet(Material.BLAZE_POWDER, SIGNALER, Component.text("Signaler un thème", NamedTextColor.RED, TextDecoration.BOLD),
                "Clic : signaler un thème inapproprié au staff.");
    }

    ItemStack note(int n) {
        return objet(TERRACOTTAS[n - 1], NOTE + n,
                Component.text("Noter " + n + "/5", COULEURS[n - 1], TextDecoration.BOLD),
                "Clic : donner " + n + " point" + (n > 1 ? "s" : "") + " à cette construction.");
    }

    /** Action de l'objet, ou null si ce n'est pas un objet du Build Battle. */
    String action(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(cle, PersistentDataType.STRING);
    }
}
