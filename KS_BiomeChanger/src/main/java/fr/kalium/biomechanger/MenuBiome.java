package fr.kalium.biomechanger;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Menus du Changeur de Biome, avec la boîte à outils des menus de KLM_Menu (menus natifs de Minecraft, affichés aussi
 * sur Bedrock par Geyser) :
 * - choix du biome : bouton « Historique » puis un bouton par biome de l'overworld (noms dans la langue du joueur) ;
 * - historique : un joueur voit ses derniers changements ; un opérateur voit ceux de tout le monde, un bouton par
 *   changement pour s'y téléporter (opérateur revérifié au clic). 10 changements par page, du plus récent au plus ancien.
 */
final class MenuBiome {

    private static final int PAR_PAGE = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));
    private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    private final KSBiomeChanger plugin;
    private final Historique historique;
    private final Lang lang;
    private final Gui gui;

    MenuBiome(KSBiomeChanger plugin, Historique historique) {
        this.plugin = plugin;
        this.historique = historique;
        this.lang = new Lang(plugin);
        this.gui = new Gui(plugin, lang);
    }

    /** Textes par défaut de la boîte à outils (bouton « Fermer »...) dans lang.yml. */
    void enregistrerTextes() {
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ choix du biome

    /** Zone d'une forme, en clair. */
    private static String zone(KSBiomeChanger.Forme forme) {
        return forme == KSBiomeChanger.Forme.SPHERE
                ? "une sphère de " + KSBiomeChanger.RAYON + " blocs de rayon"
                : "un cube de " + Math.round(KSBiomeChanger.DEMI_COTE * 2) + " blocs de côté (même volume que la sphère)";
    }

    void ouvrir(Player player) {
        KSBiomeChanger.Forme forme = plugin.forme(player);
        KSBiomeChanger.Forme autre = forme == KSBiomeChanger.Forme.SPHERE ? KSBiomeChanger.Forme.CUBE
                : KSBiomeChanger.Forme.SPHERE;
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(Component.text("Historique", NamedTextColor.GOLD),
                Component.text(player.isOp() ? "Tous les changements de biome" : "Tes derniers changements de biome"),
                p -> historique(p, 0)));
        // Choix de la forme (demande de Maxster33) : le bouton passe à l'autre forme et rouvre le menu.
        boutons.add(gui.button(Component.text("Forme : " + forme.nom + " (passer au " + autre.nom + ")", NamedTextColor.AQUA),
                Component.text("Changer pour " + zone(autre)), p -> {
                    plugin.changerForme(p);
                    ouvrir(p);
                }));
        for (Biome biome : plugin.biomes()) {
            boutons.add(gui.button(Component.translatable(biome), null, p -> plugin.changer(p, biome)));
        }
        List<Component> corps = List.of(Component.text("Choisis le nouveau biome : il sera appliqué dans " + zone(forme)
                + " autour de toi. Aucun bloc ne bouge.\n"
                + "Impossible si la zone touche une zone protégée. Un Changeur de Biome sera consommé."));
        gui.open(player, Component.text("Changeur de Biome"), corps, List.of(), boutons, null, 2);
    }

    // ------------------------------------------------------------------ historique

    private static Component nomBiome(String cle) {
        int deuxPoints = cle.indexOf(':');
        return Component.translatable("biome." + cle.substring(0, deuxPoints) + "." + cle.substring(deuxPoints + 1));
    }

    void historique(Player player, int page) {
        boolean op = player.isOp();
        List<Historique.Changement> liste = op ? historique.tous() : historique.de(player.getUniqueId());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int numero = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        if (liste.isEmpty()) {
            corps.add(Component.text("Aucun changement de biome enregistré."));
        } else {
            corps.add(Component.text((op ? "Tous les changements de biome" : "Tes derniers changements de biome")
                    + " (page " + (numero + 1) + "/" + pages + ")"
                    + (op ? " : clique sur un changement pour t'y téléporter." : " :")));
            for (Historique.Changement c : liste.subList(numero * PAR_PAGE, Math.min(liste.size(), (numero + 1) * PAR_PAGE))) {
                String lieu = c.forme() + " · " + c.monde() + " " + c.x() + " " + c.y() + " " + c.z();
                if (op) {
                    boutons.add(gui.button(Component.text(DATE_COURTE.format(Instant.ofEpochMilli(c.date())) + " · "
                                    + c.nom() + " · ").append(nomBiome(c.biome())),
                            Component.text(lieu + " : se téléporter"), p -> teleporter(p, c)));
                } else {
                    corps.add(Component.text(DATE.format(Instant.ofEpochMilli(c.date())) + " — ")
                            .append(nomBiome(c.biome())).append(Component.text(" — " + lieu)));
                }
            }
        }
        if (numero > 0) {
            boutons.add(gui.button(Component.text("« Page précédente"), null, p -> historique(p, numero - 1)));
        }
        if (numero < pages - 1) {
            boutons.add(gui.button(Component.text("Page suivante »"), null, p -> historique(p, numero + 1)));
        }
        boutons.add(gui.button(Component.text("Retour"), null, this::ouvrir));
        gui.open(player, Component.text("Historique des changements de biome"), corps, List.of(), boutons, null, 1);
    }

    private static void teleporter(Player player, Historique.Changement c) {
        if (!player.isOp()) {
            return;
        }
        World monde = Bukkit.getWorld(c.monde());
        if (monde == null) {
            player.sendMessage(Component.text("Le monde " + c.monde() + " n'existe plus.", NamedTextColor.RED));
            return;
        }
        player.teleport(new Location(monde, c.x() + 0.5, c.y(), c.z() + 0.5));
    }
}
