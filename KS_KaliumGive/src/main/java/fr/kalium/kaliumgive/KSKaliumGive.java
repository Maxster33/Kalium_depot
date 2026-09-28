package fr.kalium.kaliumgive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * KS_KaliumGive (demande de Maxster33, 28/09/2026) : /kaliumgive <pseudo> <id_custom> <nombre> donne un objet custom
 * du serveur (opérateurs, permission ks.kaliumgive).
 *
 * Chaque objet est créé par le plugin qui le définit (softdepend) : l'objet donné est donc toujours identique à celui
 * du jeu. Un id dont le plugin n'est pas activé est refusé.
 */
public final class KSKaliumGive extends JavaPlugin {

    /** Plugin qui fournit l'objet, et sa fabrique. */
    private record ObjetCustom(String plugin, Supplier<ItemStack> fabrique) {
    }

    /** Liste des id_custom (ajouter ici les prochains objets, et leur plugin dans softdepend de plugin.yml). */
    private static final Map<String, ObjetCustom> OBJETS = new LinkedHashMap<>();

    static {
        OBJETS.put("estomac_gardien", new ObjetCustom("KS_EstomacGardien",
                () -> fr.kalium.estomacgardien.KSEstomacGardien.creerEstomac()));
    }

    @Override
    public void onEnable() {
        getCommand("kaliumgive").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 3) {
            return false;
        }
        Player cible = Bukkit.getPlayerExact(args[0]);
        if (cible == null) {
            sender.sendMessage(Component.text("Joueur introuvable : " + args[0], NamedTextColor.RED));
            return true;
        }
        ObjetCustom objet = OBJETS.get(args[1].toLowerCase());
        if (objet == null) {
            sender.sendMessage(Component.text("id_custom inconnu : " + args[1] + ". Liste : "
                    + String.join(", ", OBJETS.keySet()), NamedTextColor.RED));
            return true;
        }
        if (!getServer().getPluginManager().isPluginEnabled(objet.plugin())) {
            sender.sendMessage(Component.text("Le plugin " + objet.plugin() + " n'est pas activé.", NamedTextColor.RED));
            return true;
        }
        int nombre;
        try {
            nombre = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            nombre = 0;
        }
        if (nombre < 1 || nombre > 64) {
            sender.sendMessage(Component.text("Le nombre doit être entre 1 et 64.", NamedTextColor.RED));
            return true;
        }
        // Un objet à la fois (certains ne s'empilent pas) ; inventaire plein : le reste tombe au sol.
        for (int i = 0; i < nombre; i++) {
            for (ItemStack reste : cible.getInventory().addItem(objet.fabrique().get()).values()) {
                cible.getWorld().dropItemNaturally(cible.getLocation(), reste);
            }
        }
        sender.sendMessage(Component.text(nombre + " x " + args[1].toLowerCase() + " donné(s) à " + cible.getName()
                + ".", NamedTextColor.GREEN));
        return true;
    }

    /** Complétion : pseudos en ligne, puis id_custom. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> choix = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> choix.add(p.getName()));
        } else if (args.length == 2) {
            choix.addAll(OBJETS.keySet());
        } else {
            return List.of();
        }
        String debut = args[args.length - 1].toLowerCase();
        choix.removeIf(c -> !c.toLowerCase().startsWith(debut));
        return choix;
    }
}
