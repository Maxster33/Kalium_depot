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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        OBJETS.put("cle_de_l_end", new ObjetCustom("KS_EC_Extension",
                () -> fr.kalium.ecextension.KSECExtension.creerCle()));
        OBJETS.put("bedrock_breaker", new ObjetCustom("KS_BedrockBreaker",
                () -> fr.kalium.bedrockbreaker.KSBedrockBreaker.creerBreaker()));
        // 1.5.0 : objets de KS_ItemSimple, KS_BiomeChanger et KS_Spawners (ids écrits ici : aucune classe de ces plugins
        // n'est chargée tant que l'objet n'est pas demandé).
        OBJETS.put("fragment_spawner", new ObjetCustom("KS_ItemSimple",
                () -> fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()));
        OBJETS.put("coeur_spawner", new ObjetCustom("KS_ItemSimple",
                () -> fr.kalium.itemsimple.KSItemSimple.creerCoeurSpawner()));
        OBJETS.put("changeur_biome", new ObjetCustom("KS_BiomeChanger",
                () -> fr.kalium.biomechanger.KSBiomeChanger.creerChangeur()));
        for (String creature : List.of("zombi", "squelette", "araignee", "creeper", "blaze", "mouton", "vache", "poule")) {
            OBJETS.put("spawner_" + creature, new ObjetCustom("KS_Spawners",
                    () -> fr.kalium.spawners.KSSpawners.creerSpawner(creature)));
        }
        // 1.6.0 (LeKiwi06) : les 5 têtes « Steve » de KS_ItemSimple sont retirées (têtes de KS_Decapitator, ajoutées
        // au démarrage : voir onEnable) ; élixirs de KS_Elixir.
        for (String elixir : List.of("super_gateau", "chauve_souris", "anguille", "ignifugation", "plume", "phenix",
                "fantome", "titan", "vent", "rebond", "fortune")) {
            OBJETS.put("elixir_" + elixir, new ObjetCustom("KS_Elixir",
                    () -> fr.kalium.elixir.KSElixir.creerElixir(elixir)));
        }
        // 1.8.0 (catégorie 7) : jetons de KS_Jetons ; 1.9.0 : jetons et badges ajoutés au démarrage (voir onEnable).
    }

    /** Fiole d'experience de KS_FioleExp, avec son nombre de niveaux (1.4.0 ; points avant) : fiole_exp(50). */
    private static final Pattern FIOLE_EXP = Pattern.compile("fiole_exp\\((\\d{1,9})\\)");

    /** id_custom -> objet, ou null si l'id est inconnu. */
    private static ObjetCustom trouver(String id) {
        Matcher fiole = FIOLE_EXP.matcher(id);
        if (fiole.matches()) {
            int niveaux = Integer.parseInt(fiole.group(1));
            return niveaux > 0 && niveaux <= fr.kalium.fioleexp.KSFioleExp.NIVEAUX_MAX ? new ObjetCustom("KS_FioleExp",
                    () -> fr.kalium.fioleexp.KSFioleExp.creerFioleNiveaux(niveaux)) : null;
        }
        return OBJETS.get(id);
    }

    /** 1.6.0 : têtes de KS_Decapitator (tete_<id>, ex. tete_bebe_mouton_rouge) ; liste lue dans le plugin des têtes. */
    private static final String TETE = "tete_";

    /** 1.9.0 : badges de KS_Jetons (badge_<type>_<niveau>, ex. badge_fly_3) ; liste lue dans KS_Jetons. */
    private static final String BADGE = "badge_";

    /**
     * 1.7.0 (catégorie 4 « Récompenses ») : un objet custom par son id_custom (comme /kaliumgive), ou null si l'id est
     * inconnu ou si son plugin n'est pas activé. Utilisé par KS_RewardsGUI.
     */
    public static ItemStack creer(String id) {
        ObjetCustom objet = id == null ? null : trouver(id.toLowerCase());
        if (objet == null || !Bukkit.getPluginManager().isPluginEnabled(objet.plugin())) {
            return null;
        }
        return objet.fabrique().get();
    }

    @Override
    public void onEnable() {
        getCommand("kaliumgive").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Decapitator")) {
            for (String id : fr.kalium.decapitator.KSDecapitator.ids()) {
                OBJETS.put(TETE + id, new ObjetCustom("KS_Decapitator",
                        () -> fr.kalium.decapitator.KSDecapitator.creerTete(id)));
            }
        }
        // 1.9.0 : jeton_fly, jeton_mort, jeton_tp, jeton_claim et badge_<type>_<niveau> (KS_Jetons 2.0.0).
        if (getServer().getPluginManager().isPluginEnabled("KS_Jetons")) {
            for (String id : fr.kalium.jetons.KSJetons.idsCustom()) {
                OBJETS.put(id, new ObjetCustom("KS_Jetons", () -> fr.kalium.jetons.KSJetons.creerCustom(id)));
            }
        }
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
        ObjetCustom objet = trouver(args[1].toLowerCase());
        if (objet == null) {
            List<String> liste = new ArrayList<>(OBJETS.keySet());
            liste.removeIf(id -> id.startsWith(TETE) || id.startsWith(BADGE));
            sender.sendMessage(Component.text("id_custom inconnu : " + args[1] + ". Liste : "
                    + String.join(", ", liste) + ", fiole_exp(<niveaux>), " + BADGE + "<type>_<niveau> (complétion), "
                    + TETE + "<tête> (liste : /tetes ou complétion)", NamedTextColor.RED));
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
            choix.add("fiole_exp(");
        } else {
            return List.of();
        }
        String debut = args[args.length - 1].toLowerCase();
        choix.removeIf(c -> !c.toLowerCase().startsWith(debut));
        return choix;
    }
}
