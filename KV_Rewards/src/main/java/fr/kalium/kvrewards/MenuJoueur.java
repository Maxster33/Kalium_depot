package fr.kalium.kvrewards;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Interface Récompenses du joueur (Kanvas) : notes reçues ce mois-ci et au total (prochain palier), niveau de prestige ;
 * bouton de prestige quand il est possible (jamais obligatoire). Les récompenses se récupèrent sur Event (/rewards).
 */
final class MenuJoueur {

    private final KVRewards plugin;
    private final Moteur moteur;
    private final Lang lang;
    private final Gui gui;

    MenuJoueur(KVRewards plugin, Moteur moteur) {
        this.plugin = plugin;
        this.moteur = moteur;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private static String pts(double valeur) {
        return String.valueOf((long) Math.floor(valeur));
    }

    void ouvrir(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        List<Component> corps = new ArrayList<>();
        corps.add(lang.c("joueur.aide", "<gray>Les notes reçues par tes plots (et ceux où tu es éditeur) font franchir "
                + "des paliers et entrer dans les tops ; les 3 premières places des concours de build sont aussi "
                + "récompensées. Tout se récupère sur le serveur Event avec /rewards."));
        int prestige = moteur.prestige(uuid);
        double perm = moteur.compteur("permanent", uuid);
        double mois = moteur.compteur("mois", uuid);
        corps.add(lang.c("joueur.notes", "<gold>Notes reçues<prestige></gold> <gray>- permanent <white><perm></white> "
                        + "(prochain palier <white><prochain-perm></white>), mois <white><mois></white> (<prochain-mois>)",
                "prestige", prestige > 0 ? " (prestige " + prestige + ")" : "",
                "perm", pts(perm), "prochain-perm", Moteur.prochainSeuil(perm),
                "mois", pts(mois), "prochain-mois", Moteur.prochainSeuil(mois)));
        List<ActionButton> boutons = new ArrayList<>();
        if (moteur.prestigePossible(uuid)) {
            boutons.add(gui.button(lang.c("joueur.bouton-prestige", "<light_purple>Passer au prestige"), null,
                    this::confirmerPrestige));
        }
        if (plugin.estAdmin(joueur)) {
            boutons.add(gui.button(lang.c("joueur.bouton-admin", "<red>Tables de butin"), null, plugin::ouvrirAdmin));
        }
        gui.open(joueur, lang.c("joueur.titre", "<light_purple><bold>Récompenses"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void confirmerPrestige(Player joueur) {
        int suivant = moteur.prestige(joueur.getUniqueId()) + 1;
        gui.confirm(joueur, lang.c("prestige.titre", "<light_purple><bold>Prestige <niveau>", "niveau", suivant),
                lang.c("prestige.texte", "<white>Ton compteur des paliers permanents repart de 0 ; les classements ne "
                                + "changent pas. Les paliers permanents rapporteront <light_purple>+<bonus> %</light_purple> de "
                                + "quantité.", "bonus", suivant * 20),
                p -> {
                    if (moteur.prestigePossible(p.getUniqueId())) {
                        moteur.prendrePrestige(p.getUniqueId());
                    }
                    ouvrir(p);
                }, this::ouvrir);
        lang.saveIfNeeded();
    }
}
