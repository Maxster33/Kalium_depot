package fr.kalium.kvbuildbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;

/**
 * Build Battle, côté Kanvas (cahier des charges : KV_BuildBattle/CAHIER_DES_CHARGES.md). Étape 1 : monde à part,
 * capture de la boîte et de la salle d'attente, génération de l'arène (4 colonnes de 8 boîtes par défaut).
 */
public final class KVBuildBattle extends JavaPlugin implements TabExecutor {

    private World monde;
    private Travaux travaux;
    private Arene arene;
    private Lang lang;
    private MenuAdmin menu;
    private Jeu jeu;
    /** Confirmation de /bbadmin generer : joueur (ou console) -> heure de la demande. */
    private final java.util.Map<String, Long> confirmations = new java.util.HashMap<>();

    /** Monde vide : seules les constructions collées par le plugin y existent. */
    private static final class MondeVide extends ChunkGenerator {
        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5, 80, 0.5);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String nom = getConfig().getString("monde", "buildbattle");
        monde = getServer().getWorld(nom);
        if (monde == null) {
            monde = new WorldCreator(nom).generator(new MondeVide()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (monde == null) {
            getLogger().severe("Impossible de créer ou charger le monde " + nom + " : plugin désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        monde.setSpawnLocation(0, 80, 0);
        // 0.3.1 : pas de ticks aléatoires (LeKiwi06 : « la glace a fondu ») : glace, neige, feuilles, cultures restent telles
        // quelles dans l'arène et les constructions.
        monde.setGameRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, 0);
        travaux = new Travaux(this);
        arene = new Arene(this);
        arene.protegerMonde();
        getServer().getPluginManager().registerEvents(new Regles(this), this);

        lang = new Lang(this);
        Gui gui = new Gui(this, lang);
        menu = new MenuAdmin(this, lang, gui);
        // 0.2.0 : arrivée des joueurs envoyés par KG_BuildBattle (kal-games) ; 0.3.0 : les parties.
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        jeu = new Jeu(this, gui);
        getServer().getPluginManager().registerEvents(jeu, this);
        getServer().getServicesManager().register(MenuSection.class, MenuSection.of(this, "arene",
                MenuSection.Audience.ADMINS,
                lang.c("catalogue.titre", "<gold>Build Battle : arène"),
                lang.c("catalogue.description", "<gray>Capturer la boîte et la salle d'attente, générer l'arène."),
                menu::ouvrir), this, ServicePriority.Normal);
        lang.saveIfNeeded();
    }

    /** Téléporte au point d'apparition du monde vide, en vol (sinon on tombe dans le vide). */
    void allerAuMonde(Player p) {
        p.teleport(monde.getSpawnLocation().add(0.5, 0, 0.5));
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    @Override
    public void onDisable() {
        if (jeu != null) jeu.toutRendre();
    }

    World monde() {
        return monde;
    }

    Travaux travaux() {
        return travaux;
    }

    Arene arene() {
        return arene;
    }

    // --- /bbadmin ---

    @Override
    public boolean onCommand(CommandSender qui, Command commande, String label, String[] args) {
        String a0 = args.length > 0 ? args[0].toLowerCase() : "menu";
        switch (a0) {
            case "menu" -> {
                if (qui instanceof Player p) menu.ouvrir(p, null);
                else arene.info(qui);
            }
            case "info" -> {
                arene.info(qui);
                for (Partie p : jeu.parties()) {
                    qui.sendMessage("§7  Colonne " + (p.colonne + 1) + " : " + p.description() + ", " + p.phase + ", "
                            + p.joueurs.size() + "/" + p.places() + " joueurs");
                }
            }
            case "monde" -> {
                if (qui instanceof Player p) allerAuMonde(p);
            }
            case "boite", "salle" -> {
                if (args.length < 2) return false;
                String a1 = args[1].toLowerCase();
                if (a1.equals("capturer")) {
                    if (a0.equals("boite")) arene.capturerBoite(qui);
                    else arene.capturerSalle(qui);
                    return true;
                }
                if (!(qui instanceof Player p)) {
                    qui.sendMessage("§cÀ faire en jeu : la position est prise à l'endroit du joueur.");
                    return true;
                }
                String cle = a0 + "." + a1;
                if (!Arene.POSITIONS.contains(cle)) return false;
                arene.poser(p, cle);
                p.sendMessage("§a" + cle + " posé : §7" + Arene.texte(p.getLocation()));
            }
            case "generer" -> {
                Long demande = confirmations.remove(qui.getName());
                if (args.length > 1 && args[1].equalsIgnoreCase("confirmer") && demande != null
                        && System.currentTimeMillis() - demande < 60_000L) {
                    arene.generer(qui);
                } else {
                    confirmations.put(qui.getName(), System.currentTimeMillis());
                    qui.sendMessage("§eLes " + arene.colonnes() * arene.boitesParColonne() + " boîtes seront recopiées "
                            + "(boîtes déjà générées remplacées). Confirme dans la minute : §f/bbadmin generer confirmer");
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender qui, Command commande, String label, String[] args) {
        List<String> choix = switch (args.length) {
            case 1 -> List.of("menu", "info", "monde", "boite", "salle", "generer");
            case 2 -> switch (args[0].toLowerCase()) {
                case "boite" -> List.of("pos1", "pos2", "zone1", "zone2", "apparition", "capturer");
                case "salle" -> List.of("pos1", "pos2", "apparition", "capturer");
                case "generer" -> List.of("confirmer");
                default -> List.of();
            };
            default -> List.of();
        };
        List<String> r = new ArrayList<>();
        String debut = args[args.length - 1].toLowerCase();
        for (String c : choix) if (c.startsWith(debut)) r.add(c);
        return r;
    }
}
