package fr.kalium.fioleexp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * Menu de remplissage d'une fiole : accroupi + clic droit sur une enclume avec une fiole vide en main.
 * 1er menu : nombre de points ; 2e : « N points = X niveaux consommés (niveau A -> B) », Confirmer / Annuler.
 * Même effet qu'à l'enclume : points exacts retirés, une fiole vide consommée, même usure de l'enclume.
 *
 * 1.2.0 : joueurs Bedrock seulement (formulaire Floodgate, voir FormulaireBedrock).
 * 1.3.0 - demande de Maxster33, 29/09/2026 (« uniformiser ») : joueurs Java aussi, par un dialogue natif (Paper), avec
 * les mêmes textes que le formulaire Bedrock. Le remplissage par le champ du nom de l'enclume reste possible.
 */
final class MenuFiole implements Listener {

    static final String TITRE = "Fiole d'expérience";
    static final String CHAMP = "Nombre de points à stocker";

    /** Distance maximale entre le joueur et l'enclume à la confirmation. */
    private static final double DISTANCE_MAX = 6;

    /** Un bouton de dialogue Java ne sert qu'une fois, et pas au-delà de 10 minutes. */
    private static final ClickCallback.Options UNE_FOIS =
            ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build();

    private final KSFioleExp plugin;
    /** Formulaires Floodgate ; null si Floodgate est absent (tout le monde passe alors par le dialogue Java). */
    private final FormulaireBedrock bedrock;
    private final Map<UUID, Long> derniereOuverture = new HashMap<>();

    MenuFiole(KSFioleExp plugin, FormulaireBedrock bedrock) {
        this.plugin = plugin;
        this.bedrock = bedrock;
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
                || player.getInventory().getItemInMainHand().getType() != Material.GLASS_BOTTLE) {
            return;
        }
        event.setCancelled(true);
        // Un clic de Bedrock peut arriver en double : un seul menu par seconde.
        long maintenant = System.currentTimeMillis();
        Long dernier = derniereOuverture.put(player.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 1000) {
            return;
        }
        demanderPoints(player, block.getLocation(), null);
    }

    private boolean estBedrock(Player player) {
        return bedrock != null && bedrock.estBedrock(player);
    }

    /** Les réponses des menus arrivent hors du fil du serveur : la suite y est ramenée. */
    private void surServeur(Runnable suite) {
        plugin.getServer().getScheduler().runTask(plugin, suite);
    }

    // ------------------------------------------------------------------ étapes (communes Java / Bedrock)

    /** 1er menu : nombre de points à stocker (erreur éventuelle en tête). */
    private void demanderPoints(Player player, Location enclume, String erreur) {
        String texte = "Tu as " + KSFioleExp.groupDigits(player.calculateTotalExperiencePoints())
                + " points d'expérience (niveau " + player.getLevel() + ").\n"
                + "Une fiole vide de ton inventaire sera remplie.";
        Consumer<String> suite = saisie -> surServeur(() -> verifierPoints(player, enclume, saisie));
        if (estBedrock(player)) {
            bedrock.demanderPoints(player, erreur, texte, suite);
        } else {
            dialogueDemande(player, erreur, texte, suite);
        }
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
        int niveauApres = KSFioleExp.levelFor(player.calculateTotalExperiencePoints() - points);
        int consommes = player.getLevel() - niveauApres;
        String texte = "Stocker " + KSFioleExp.groupDigits(points) + " points consommera " + consommes
                + (consommes > 1 ? " niveaux" : " niveau") + " : tu passeras du niveau " + player.getLevel()
                + " au niveau " + niveauApres + ".";
        Runnable suite = () -> surServeur(() -> remplir(player, enclume, points));
        if (estBedrock(player)) {
            bedrock.confirmer(player, texte, suite);
        } else {
            dialogueConfirmation(player, texte, suite);
        }
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

    /** Confirmation : tout est revérifié (l'inventaire ou l'XP ont pu changer pendant le menu). */
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

    // ------------------------------------------------------------------ dialogues Java

    private static void dialogueDemande(Player player, String erreur, String texte, Consumer<String> suite) {
        List<DialogBody> corps = new ArrayList<>();
        if (erreur != null) {
            corps.add(DialogBody.plainMessage(Component.text(erreur, NamedTextColor.RED)));
        }
        corps.add(DialogBody.plainMessage(Component.text(texte)));
        DialogAction valider = DialogAction.customClick(
                (reponse, audience) -> suite.accept(reponse.getText("points")), UNE_FOIS);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(TITRE))
                        .body(corps)
                        .inputs(List.of(DialogInput.text("points", Component.text(CHAMP)).build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Valider")).action(valider).build(),
                        ActionButton.builder(Component.text("Annuler")).build())));
        player.showDialog(dialog);
    }

    private static void dialogueConfirmation(Player player, String texte, Runnable suite) {
        DialogAction confirmer = DialogAction.customClick((reponse, audience) -> suite.run(), UNE_FOIS);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(TITRE))
                        .body(List.of(DialogBody.plainMessage(Component.text(texte))))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirmer")).action(confirmer).build(),
                        ActionButton.builder(Component.text("Annuler")).build())));
        player.showDialog(dialog);
    }
}
