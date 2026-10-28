package fr.kalium.alcools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Cocktail Molotov (cahier, partie 3.3) : potion jetable sans effet. À l'impact : feu sur une zone de 3 × 3 (sur les
 * blocs qui peuvent en porter), creepers de la zone allumés. Rien dans le claim d'un autre joueur (message au lanceur ;
 * lancé par un distributeur : rien dans aucun claim). Chaque feu passe aussi par l'événement d'allumage du jeu, comme un
 * briquet du lanceur : les autres protections (WorldGuard...) peuvent le refuser.
 */
final class Molotov implements Listener {

    private final KSAlcools plugin;

    Molotov(KSAlcools plugin) {
        this.plugin = plugin;
    }

    static ItemStack creer(NamespacedKey marque) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(null);
        meta.setColor(Color.fromRGB(0xFF7A1A));
        meta.displayName(Component.text("Cocktail Molotov", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Met le feu sur 3 × 3 blocs à l'impact", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Allume les creepers", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(marque, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.POTION_CONTENTS).build());
        return item;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onImpact(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!plugin.estMolotov(potion.getItem())) {
            return;
        }
        Player lanceur = potion.getShooter() instanceof Player p ? p : null;
        UUID proprietaire = lanceur == null ? null : lanceur.getUniqueId();
        Block centre = potion.getLocation().getBlock();
        boolean refus = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block bloc = cible(centre.getRelative(dx, 0, dz));
                if (bloc == null) {
                    continue;
                }
                if (plugin.claimRefuse(bloc.getChunk(), proprietaire)) {
                    refus = true;
                    continue;
                }
                allumer(bloc, potion, lanceur);
            }
        }
        for (Entity entite : potion.getWorld().getNearbyEntities(centre.getLocation().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
            if (entite instanceof Creeper creeper && !creeper.isIgnited()) {
                if (plugin.claimRefuse(creeper.getLocation().getChunk(), proprietaire)) {
                    refus = true;
                } else if (lanceur != null) {
                    creeper.ignite(lanceur);
                } else {
                    creeper.ignite();
                }
            }
        }
        if (refus && lanceur != null) {
            lanceur.sendActionBar(plugin.t("molotov.claim", "<red>Le feu ne prend pas dans le claim d'un autre joueur."));
        }
    }

    /** Case de feu de cette colonne : à la hauteur de l'impact, sinon juste en dessous, sinon juste au-dessus. */
    private static Block cible(Block colonne) {
        for (int dy : new int[]{0, -1, 1}) {
            Block bloc = colonne.getRelative(0, dy, 0);
            if (bloc.isEmpty() && bloc.canPlace(feu(bloc))) {
                return bloc;
            }
        }
        return null;
    }

    /** Feu des âmes sur le sable et la terre des âmes, comme le jeu. */
    private static BlockData feu(Block bloc) {
        Material dessous = bloc.getRelative(BlockFace.DOWN).getType();
        return (dessous == Material.SOUL_SAND || dessous == Material.SOUL_SOIL ? Material.SOUL_FIRE : Material.FIRE)
                .createBlockData();
    }

    private static void allumer(Block bloc, ThrownPotion potion, Player lanceur) {
        BlockIgniteEvent allumage = new BlockIgniteEvent(bloc, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL,
                lanceur != null ? lanceur : potion);
        if (allumage.callEvent()) {
            bloc.setBlockData(feu(bloc));
        }
    }
}
