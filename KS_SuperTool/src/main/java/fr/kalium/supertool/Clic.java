package fr.kalium.supertool;

import fr.kalium.supertool.Outil.Machine;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Clic droit en 3 × 3 (cahier Outils_3x3.md § 6 et 8) : le jeu transforme le bloc visé, KS_SuperTool les 8 autour
 * (accroupi : seulement le bloc visé, comme le jeu) :
 * - Laboureuse : terre, herbe, chemin -> terre labourée ; terre grossière -> terre ; podzol et mycélium -> terre
 *   labourée directement (aussi le bloc visé : le jeu ne les laboure pas) ; à l'horizontale, air au-dessus ;
 * - Excavatrice : herbe, terre, podzol, terre grossière, mycélium, terre racinée -> chemin ; à l'horizontale, air
 *   au-dessus ;
 * - Abatteuse : écorce bûches et bois, gratte le cuivre d'un cran, retire la cire (dans cet ordre, comme la hache du
 *   jeu) ; dans le plan de la face visée (un mur de bûches, un tronc).
 * Un point de solidité par bloc transformé (Solidité compris) ; jamais un contenant ; pas dans le claim d'un autre.
 */
final class Clic implements Listener {

    private static final Set<Material> LABOURABLES = Set.of(Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH);
    private static final Set<Material> CHEMINS = Set.of(Material.GRASS_BLOCK, Material.DIRT, Material.PODZOL,
            Material.COARSE_DIRT, Material.MYCELIUM, Material.ROOTED_DIRT);

    private final boolean claims;

    Clic(boolean claims) {
        this.claims = claims;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClic(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        Player joueur = event.getPlayer();
        ItemStack objet = joueur.getInventory().getItemInMainHand();
        Outil outil = KSSuperTool.outil(objet);
        if (outil == null || outil.machine() == Machine.FOREUSE) {
            return;
        }
        Block centre = event.getClickedBlock();
        BlockFace face = event.getBlockFace();
        Machine m = outil.machine();
        // Rien autour si le bloc visé lui-même ne se transforme pas (pierre, bloc déjà labouré...).
        if (transformer(m, centre) == null) {
            return;
        }
        List<Block> cibles = new ArrayList<>();
        // Le jeu ne laboure ni le podzol ni le mycélium : la Laboureuse s'occupe aussi du bloc visé.
        if (m == Machine.LABOUREUSE && (centre.getType() == Material.PODZOL || centre.getType() == Material.MYCELIUM)) {
            cibles.add(centre);
        }
        if (!joueur.isSneaking()) {
            cibles.addAll(Minage.voisins(centre, m == Machine.ABATTEUSE ? face : BlockFace.UP));
        }
        if (face == BlockFace.DOWN && m != Machine.ABATTEUSE) {
            return; // comme le jeu : ni labour ni chemin par-dessous
        }
        int faits = 0;
        Sound son = null;
        for (Block b : cibles) {
            if (claims && ProtectionClaims.refuse(b.getChunk(), joueur.getUniqueId())) {
                continue;
            }
            Resultat r = transformer(m, b);
            if (r == null) {
                continue;
            }
            b.setBlockData(r.donnees());
            son = r.son();
            faits++;
        }
        if (faits == 0) {
            return;
        }
        joueur.getWorld().playSound(centre.getLocation().add(0.5, 0.5, 0.5), son, SoundCategory.BLOCKS, 1f, 1f);
        joueur.swingMainHand();
        objet.damage(faits, joueur);
    }

    private record Resultat(BlockData donnees, Sound son) {
    }

    private static Resultat transformer(Machine m, Block b) {
        Material type = b.getType();
        boolean airDessus = b.getRelative(BlockFace.UP).getType().isAir();
        switch (m) {
            case LABOUREUSE -> {
                if (!airDessus) {
                    return null;
                }
                if (LABOURABLES.contains(type) || type == Material.PODZOL || type == Material.MYCELIUM) {
                    return new Resultat(Material.FARMLAND.createBlockData(), Sound.ITEM_HOE_TILL);
                }
                if (type == Material.COARSE_DIRT) {
                    return new Resultat(Material.DIRT.createBlockData(), Sound.ITEM_HOE_TILL);
                }
                return null;
            }
            case EXCAVATRICE -> {
                return airDessus && CHEMINS.contains(type)
                        ? new Resultat(Material.DIRT_PATH.createBlockData(), Sound.ITEM_SHOVEL_FLATTEN) : null;
            }
            case ABATTEUSE -> {
                BlockState etat = b.getState(false);
                if (etat instanceof Container || etat instanceof InventoryHolder) {
                    return null;
                }
                String nom = type.name();
                Material ecorce = materiau("STRIPPED_" + nom);
                if (ecorce != null && !nom.startsWith("STRIPPED_")) {
                    return new Resultat(remplacer(b, type, ecorce), Sound.ITEM_AXE_STRIP);
                }
                Material gratte = gratter(nom);
                if (gratte != null) {
                    return new Resultat(remplacer(b, type, gratte), Sound.ITEM_AXE_SCRAPE);
                }
                if (nom.startsWith("WAXED_")) {
                    Material sansCire = materiau(nom.substring(6));
                    if (sansCire != null) {
                        return new Resultat(remplacer(b, type, sansCire), Sound.ITEM_AXE_WAX_OFF);
                    }
                }
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /** Cuivre d'un cran moins oxydé (oxydé -> altéré -> exposé -> neuf), jamais un objet ciré. */
    private static Material gratter(String nom) {
        if (nom.startsWith("WAXED_")) {
            return null;
        }
        for (String[] pas : new String[][] {{"OXIDIZED_", "WEATHERED_"}, {"WEATHERED_", "EXPOSED_"}, {"EXPOSED_", ""}}) {
            if (nom.startsWith(pas[0])) {
                String neuf = pas[1] + nom.substring(pas[0].length());
                Material m = materiau(neuf);
                if (m == null && neuf.equals("COPPER")) {
                    m = Material.COPPER_BLOCK;
                }
                return m;
            }
        }
        return null;
    }

    private static Material materiau(String nom) {
        Material m = Material.getMaterial(nom.toUpperCase(Locale.ROOT));
        return m != null && m.isBlock() ? m : null;
    }

    /** Même bloc sous un autre type, en gardant son orientation et ses propriétés (axe, demi-dalle, porte...). */
    private static BlockData remplacer(Block b, Material avant, Material apres) {
        String texte = b.getBlockData().getAsString();
        String ancien = avant.getKey().toString();
        String nouveau = apres.getKey().toString();
        try {
            return Bukkit.createBlockData(texte.replace(ancien, nouveau));
        } catch (IllegalArgumentException e) {
            return apres.createBlockData();
        }
    }
}
