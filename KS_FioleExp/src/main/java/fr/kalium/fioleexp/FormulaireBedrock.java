package fr.kalium.fioleexp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * KS_FioleExp 1.2.0 - demande de Maxster33, 29/09/2026 : sur Bedrock, le nombre tapé dans le champ du nom de l'enclume
 * n'est envoyé au serveur qu'à la prise du résultat, le coût ne peut donc pas être affiché avant. Les joueurs Bedrock
 * remplissent donc une fiole par un formulaire : accroupi + clic droit sur une enclume avec une fiole vide en main.
 * 1er formulaire : nombre de points ; 2e : « N points = X niveaux consommés (niveau A -> B) », Confirmer / Annuler.
 * Même effet qu'à l'enclume : points exacts retirés, une fiole vide consommée, usure vanilla de l'enclume.
 *
 * Classe chargée seulement si Floodgate est activé (elle utilise ses classes et celles de Cumulus).
 */
final class FormulaireBedrock implements Listener {

    /** Distance maximale entre le joueur et l'enclume à la confirmation. */
    private static final double DISTANCE_MAX = 6;

    private final KSFioleExp plugin;
    private final Map<UUID, Long> derniereOuverture = new HashMap<>();

    FormulaireBedrock(KSFioleExp plugin) {
        this.plugin = plugin;
    }

    private static boolean estEnclume(Block block) {
        Material type = block.getType();
        return type == Material.ANVIL || type == Material.CHIPPED_ANVIL || type == Material.DAMAGED_ANVIL;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null
                || !estEnclume(block) || !player.isSneaking()
                || player.getInventory().getItemInMainHand().getType() != Material.GLASS_BOTTLE
                || !FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        // Un clic de Bedrock peut arriver en double : un seul formulaire par seconde.
        long maintenant = System.currentTimeMillis();
        Long dernier = derniereOuverture.put(player.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 1000) {
            return;
        }
        demanderPoints(player, block.getLocation(), null);
    }

    /** 1er formulaire : nombre de points à stocker (erreur éventuelle en tête). */
    private void demanderPoints(Player player, Location enclume, String erreur) {
        int current = player.calculateTotalExperiencePoints();
        String texte = (erreur != null ? "§c" + erreur + "§r\n\n" : "")
                + "Tu as " + KSFioleExp.groupDigits(current) + " points d'expérience (niveau " + player.getLevel() + ").\n"
                + "Une fiole vide de ton inventaire sera remplie.";
        CustomForm form = CustomForm.builder()
                .title("Fiole d'expérience")
                .label(texte)
                .input("Nombre de points à stocker", "ex. 1395")
                .validResultHandler(response -> {
                    String saisie = response.asInput(1);
                    plugin.getServer().getScheduler().runTask(plugin, () -> verifierPoints(player, enclume, saisie));
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void verifierPoints(Player player, Location enclume, String saisie) {
        if (!player.isOnline()) {
            return;
        }
        Integer points = KSFioleExp.parsePoints(saisie);
        if (points == null) {
            demanderPoints(player, enclume, "Tape un nombre de points (ex. 1395).");
            return;
        }
        String erreur = erreurPrise(player, enclume, points);
        if (erreur != null) {
            demanderPoints(player, enclume, erreur);
            return;
        }
        int current = player.calculateTotalExperiencePoints();
        int niveauApres = KSFioleExp.levelFor(current - points);
        int consommes = player.getLevel() - niveauApres;
        ModalForm form = ModalForm.builder()
                .title("Fiole d'expérience")
                .content("Stocker " + KSFioleExp.groupDigits(points) + " points consommera " + consommes
                        + (consommes > 1 ? " niveaux" : " niveau") + " : tu passeras du niveau " + player.getLevel()
                        + " au niveau " + niveauApres + ".")
                .button1("Confirmer")
                .button2("Annuler")
                .validResultHandler(response -> {
                    if (response.clickedFirst()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> remplir(player, enclume, points));
                    }
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** Raison pour laquelle la fiole ne peut pas être remplie, ou null. */
    private static String erreurPrise(Player player, Location enclume, int points) {
        if (points > player.calculateTotalExperiencePoints()) {
            return "Tu n'as pas assez de points d'expérience.";
        }
        if (player.getInventory().first(Material.GLASS_BOTTLE) < 0) {
            return "Il te faut une fiole vide dans ton inventaire.";
        }
        if (!estEnclume(enclume.getBlock()) || !player.getWorld().equals(enclume.getWorld())
                || player.getLocation().distance(enclume.clone().add(0.5, 0.5, 0.5)) > DISTANCE_MAX) {
            return "L'enclume n'est plus à portée.";
        }
        return null;
    }

    /** Confirmation : tout est revérifié (l'inventaire ou l'XP ont pu changer pendant le formulaire). */
    private void remplir(Player player, Location enclume, int points) {
        if (!player.isOnline()) {
            return;
        }
        String erreur = erreurPrise(player, enclume, points);
        if (erreur != null) {
            demanderPoints(player, enclume, erreur);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int slot = inventory.first(Material.GLASS_BOTTLE);
        ItemStack vide = inventory.getItem(slot);
        vide.setAmount(vide.getAmount() - 1);
        inventory.setItem(slot, vide.getAmount() > 0 ? vide : null);
        player.setExperienceLevelAndProgress(player.calculateTotalExperiencePoints() - points);
        for (ItemStack reste : inventory.addItem(KSFioleExp.creerFiole(points)).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), reste);
        }
        plugin.damageAnvil(enclume, player);
    }
}
