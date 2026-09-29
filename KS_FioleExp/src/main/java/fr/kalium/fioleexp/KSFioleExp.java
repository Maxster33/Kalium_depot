package fr.kalium.fioleexp;

import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KS_FioleExp (serveur Event) - demande de Maxster33, 29/09/2026 : stocker de l'XP dans un objet utilisable comme la
 * fiole d'expérience existante.
 *
 * Remplissage à l'enclume : fiole vide dans la 1re case, 2e case vide, nombre de POINTS d'XP tapé dans le champ du nom.
 * Le résultat est une fiole d'expérience vanilla marquée, nommée « Fiole d'expérience (1 395 XP) ». Une seule fiole
 * vide est remplie par opération. La prise du résultat est faite par le plugin (et non par l'enclume vanilla, qui
 * retirerait des niveaux entiers et toute la pile de fioles vides) : exactement les points demandés sont retirés,
 * l'enclume s'use comme en vanilla (12 %). Le coût affiché est le nombre de niveaux que le joueur va perdre
 * (plafonné à 39 à l'écran par KS_Enclume au-delà, vrai coût en barre d'action).
 *
 * 1.1.0 : les fioles d'expérience (vanilla ou remplies) ne peuvent plus être renommées à l'enclume.
 * 1.2.0 / 1.3.0 : menu de remplissage (accroupi + clic droit sur une enclume avec une fiole vide), voir MenuFiole.
 *
 * Lancée, la fiole se brise comme une fiole vanilla et lâche exactement les points stockés en orbes.
 */
public final class KSFioleExp extends JavaPlugin implements Listener {

    private static final int RESULT_SLOT = 2;

    private static NamespacedKey pointsKey;

    @Override
    public void onEnable() {
        pointsKey = new NamespacedKey(this, "points");
        getServer().getPluginManager().registerEvents(this, this);
        // 1.2.0 : formulaire Bedrock, seulement si Floodgate est present (ses classes ne sont chargees qu'ici).
        // 1.3.0 : menu aussi pour les joueurs Java (dialogue), voir MenuFiole.
        FormulaireBedrock bedrock = getServer().getPluginManager().isPluginEnabled("floodgate")
                ? new FormulaireBedrock() : null;
        getServer().getPluginManager().registerEvents(new MenuFiole(this, bedrock), this);
    }

    // ------------------------------------------------------------------ fiole remplie

    /** Fiole d'experience vanilla marquee avec le nombre de points stockes. Necessite que le plugin soit active
     * (utilisee aussi par KS_KaliumGive). */
    public static ItemStack creerFiole(int points) {
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();
        meta.displayName(Component.text("Fiole d'expérience (" + groupDigits(points) + " XP)")
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(pointsKey, PersistentDataType.INTEGER, points);
        bottle.setItemMeta(meta);
        return bottle;
    }

    private static Integer storedPoints(ItemStack item) {
        if (item == null || item.getType() != Material.EXPERIENCE_BOTTLE || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(pointsKey, PersistentDataType.INTEGER);
    }

    /** 1395 -> "1 395". */
    static String groupDigits(int value) {
        String digits = String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(' ');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    /** Nombre de points tape dans le champ du nom (espaces toleres), ou null si ce n'est pas un nombre positif. */
    static Integer parsePoints(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[\\s\\u00A0\\u202F]", "");
        if (!digits.matches("\\d{1,9}")) {
            return null;
        }
        int points = Integer.parseInt(digits);
        return points > 0 ? points : null;
    }

    /** Points d'experience pour aller du niveau 0 au niveau donne (formule vanilla). */
    private static long pointsForLevel(int level) {
        if (level <= 16) {
            return (long) level * level + 6L * level;
        }
        if (level <= 31) {
            return (long) (2.5 * level * level - 40.5 * level + 360);
        }
        return (long) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Niveau atteint avec ce total de points. */
    static int levelFor(int points) {
        int level = 0;
        while (pointsForLevel(level + 1) <= points) {
            level++;
        }
        return level;
    }

    // ------------------------------------------------------------------ enclume

    /** Fiole vide + 2e case vide + nombre valide ; null sinon (l'enclume garde alors son comportement vanilla). */
    private Integer requestedPoints(AnvilInventory inventory, AnvilView view) {
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        if (first == null || first.getType() != Material.GLASS_BOTTLE || (second != null && !second.isEmpty())) {
            return null;
        }
        return parsePoints(view.getRenameText());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        // 1.1.0 : aucune fiole d'experience (vanilla ou remplie) ne peut etre renommee.
        ItemStack first = event.getInventory().getFirstItem();
        if (first != null && first.getType() == Material.EXPERIENCE_BOTTLE) {
            event.setResult(null);
            return;
        }
        Integer points = requestedPoints(event.getInventory(), view);
        if (points == null || !(view.getPlayer() instanceof Player player)) {
            return;
        }
        int current = player.calculateTotalExperiencePoints();
        if (points > current) {
            event.setResult(null); // pas assez d'XP
            return;
        }
        event.setResult(creerFiole(points));
        view.setRepairCost(Math.max(1, player.getLevel() - levelFor(current - points)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTakeResult(InventoryClickEvent event) {
        if (!(event.getView() instanceof AnvilView view) || event.getRawSlot() != RESULT_SLOT
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        Integer points = storedPoints(result);
        if (points == null) {
            return;
        }
        event.setCancelled(true);

        // Verification au moment de la prise (l'XP ou les cases ont pu changer depuis le calcul).
        AnvilInventory inventory = (AnvilInventory) view.getTopInventory();
        if (!points.equals(requestedPoints(inventory, view))) {
            return;
        }
        int current = player.calculateTotalExperiencePoints();
        if (points > current) {
            return;
        }
        ItemStack given = creerFiole(points);
        ClickType click = event.getClick();
        if (click.isShiftClick()) {
            if (!canFit(player.getInventory(), given)) {
                return;
            }
            player.getInventory().addItem(given);
        } else if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            ItemStack cursor = view.getCursor();
            if (cursor == null || cursor.isEmpty()) {
                view.setCursor(given);
            } else if (cursor.isSimilar(given) && cursor.getAmount() < cursor.getMaxStackSize()) {
                cursor.setAmount(cursor.getAmount() + 1);
                view.setCursor(cursor);
            } else {
                return;
            }
        } else {
            return;
        }

        player.setExperienceLevelAndProgress(current - points);
        ItemStack first = inventory.getFirstItem();
        first.setAmount(first.getAmount() - 1);
        inventory.setFirstItem(first.getAmount() > 0 ? first : null);
        damageAnvil(inventory.getLocation(), player);
        getServer().getScheduler().runTask(this, player::updateInventory);
    }

    private static boolean canFit(PlayerInventory inventory, ItemStack item) {
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.isEmpty()
                    || (slot.isSimilar(item) && slot.getAmount() < slot.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    /** Usure vanilla : 12 % de chance que l'enclume passe a l'etat suivant (hors mode creatif). */
    void damageAnvil(Location location, Player player) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || ThreadLocalRandom.current().nextFloat() >= 0.12f) {
            location.getWorld().playEffect(location, Effect.ANVIL_USE, 0);
            return;
        }
        Block block = location.getBlock();
        // Pas de switch sur Material : ECJ genererait une table de toutes les valeurs de l'enum (classe de 130 Ko).
        Material type = block.getType();
        Material next = type == Material.ANVIL ? Material.CHIPPED_ANVIL
                : type == Material.CHIPPED_ANVIL ? Material.DAMAGED_ANVIL : null;
        if (next == null) {
            block.setType(Material.AIR);
            location.getWorld().playEffect(location, Effect.ANVIL_BREAK, 0);
            getServer().getScheduler().runTask(this, () -> player.closeInventory());
            return;
        }
        BlockData old = block.getBlockData();
        BlockData data = next.createBlockData();
        if (old instanceof Directional oldDir && data instanceof Directional newDir) {
            newDir.setFacing(oldDir.getFacing());
        }
        block.setBlockData(data);
        location.getWorld().playEffect(location, Effect.ANVIL_USE, 0);
    }

    // ------------------------------------------------------------------ utilisation

    @EventHandler(priority = EventPriority.HIGH)
    public void onBottle(ExpBottleEvent event) {
        Integer points = storedPoints(event.getEntity().getItem());
        if (points != null) {
            event.setExperience(points);
        }
    }
}
