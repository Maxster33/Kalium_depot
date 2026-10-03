package fr.kalium.tableau;

import fr.kalium.menu.api.Lang;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Tableau (revue du 03/10/2026, validée par LeKiwi06 : « fais en sorte qu'on puisse l'activer via commande, il faut
 * aussi afficher les coordonnées du joueur et le biome dans lequel il se trouve ») : tableau sur le côté de l'écran,
 * serveur Event. /tableau (ou /tableau on | off) l'affiche ou le masque ; masqué par défaut ; choix gardé
 * (tableau.yml).
 *
 * Lignes : score (KS_Economy), exploration journalière (KS_FairPlay), claims (SimpleClaimSystem), coordonnées, biome
 * (nom traduit par le jeu du joueur). Mis à jour chaque seconde ; une ligne dont le plugin manque n'est pas affichée.
 */
public final class KSTableau extends JavaPlugin implements Listener {

    private Lang lang;
    private File fichier;
    private final Set<UUID> actifs = new LinkedHashSet<>();
    private final Map<UUID, Scoreboard> tableaux = new HashMap<>();
    /** Nombre de claims par joueur, relu toutes les 30 s (la base de SimpleClaimSystem n'est pas interrogée chaque seconde). */
    private final Map<UUID, Integer> claims = new HashMap<>();
    private int tick;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        fichier = new File(getDataFolder(), "tableau.yml");
        for (String s : YamlConfiguration.loadConfiguration(fichier).getStringList("actifs")) {
            try {
                actifs.add(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Joueur ignoré : " + s);
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tableau").setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, this::actualiser, 20L, 20L);
        getServer().getOnlinePlayers().forEach(this::afficherSiActif);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (UUID u : List.copyOf(tableaux.keySet())) {
            Player p = getServer().getPlayer(u);
            if (p != null) {
                p.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
            }
        }
        tableaux.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        boolean actif = actifs.contains(joueur.getUniqueId());
        boolean voulu = args.length > 0 ? args[0].equalsIgnoreCase("on") : !actif;
        if (voulu) {
            actifs.add(joueur.getUniqueId());
            afficher(joueur);
            joueur.sendMessage(lang.c("affiche", "<green>Tableau affiché. <gray>/tableau pour le masquer."));
        } else {
            actifs.remove(joueur.getUniqueId());
            masquer(joueur);
            joueur.sendMessage(lang.c("masque", "<yellow>Tableau masqué. <gray>/tableau pour l'afficher."));
        }
        sauver();
        lang.saveIfNeeded();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> r = new ArrayList<>(List.of("on", "off"));
        r.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
        return r;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        afficherSiActif(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tableaux.remove(event.getPlayer().getUniqueId());
        claims.remove(event.getPlayer().getUniqueId());
    }

    private void afficherSiActif(Player joueur) {
        if (actifs.contains(joueur.getUniqueId())) {
            afficher(joueur);
        }
    }

    private void afficher(Player joueur) {
        Scoreboard sb = getServer().getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("ks_tableau", Criteria.DUMMY,
                lang.c("titre", "<gold><bold>Event"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        tableaux.put(joueur.getUniqueId(), sb);
        joueur.setScoreboard(sb);
        lireClaims(joueur);
        remplir(joueur, sb);
    }

    private void masquer(Player joueur) {
        if (tableaux.remove(joueur.getUniqueId()) != null) {
            joueur.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
        }
    }

    /** Chaque seconde : lignes à jour ; toutes les 30 s : nombre de claims relu. */
    private void actualiser() {
        tick++;
        for (Map.Entry<UUID, Scoreboard> e : tableaux.entrySet()) {
            Player p = getServer().getPlayer(e.getKey());
            if (p == null) {
                continue;
            }
            if (tick % 30 == 0) {
                lireClaims(p);
            }
            remplir(p, e.getValue());
        }
    }

    private void remplir(Player joueur, Scoreboard sb) {
        Objective obj = sb.getObjective("ks_tableau");
        if (obj == null) {
            return;
        }
        List<Component> lignes = new ArrayList<>();
        Long score = Sources.score(joueur.getUniqueId());
        if (score != null) {
            lignes.add(lang.c("ligne-score", "<gray>Score : <white><score>", "score", points(score)));
        }
        int[] exploration = Sources.exploration(joueur.getUniqueId());
        if (exploration != null) {
            lignes.add(lang.c("ligne-exploration", "<gray>Exploration journalière : <white><n> / <limite>", "n",
                    exploration[0], "limite", exploration[1]));
        }
        Integer n = claims.get(joueur.getUniqueId());
        if (n != null) {
            lignes.add(lang.c("ligne-claims", "<gray>Claims : <white><n>", "n", n));
        }
        Location l = joueur.getLocation();
        lignes.add(lang.c("ligne-position", "<gray>X <white><x></white> Y <white><y></white> Z <white><z>", "x",
                l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ()));
        lignes.add(lang.c("ligne-biome", "<gray>Biome : ").append(Component.translatable(
                l.getBlock().getBiome()).color(net.kyori.adventure.text.format.NamedTextColor.WHITE)));
        // Lignes « l0 », « l1 »... : score décroissant pour garder l'ordre ; texte affiché à la place du nom.
        for (int i = 0; i < lignes.size(); i++) {
            var s = obj.getScore("l" + i);
            s.setScore(lignes.size() - i);
            s.customName(lignes.get(i));
        }
        for (int i = lignes.size(); i < 8; i++) {
            sb.resetScores("l" + i);
        }
    }

    /** « 12 345 points » (comme KS_Economy). */
    private static String points(long n) {
        String brut = Long.toString(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < brut.length(); i++) {
            if (i > 0 && (brut.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(brut.charAt(i));
        }
        return (n < 0 ? "-" : "") + sb + (Math.abs(n) > 1 ? " points" : " point");
    }

    private void lireClaims(Player joueur) {
        Integer n = Sources.claims(joueur.getUniqueId());
        if (n == null) {
            claims.remove(joueur.getUniqueId());
        } else {
            claims.put(joueur.getUniqueId(), n);
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("actifs", actifs.stream().map(UUID::toString).toList());
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer tableau.yml : " + e.getMessage());
        }
    }
}
