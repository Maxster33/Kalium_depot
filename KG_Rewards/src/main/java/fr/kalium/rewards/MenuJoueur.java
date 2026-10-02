package fr.kalium.rewards;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.scoreboards.KGScoreBoards;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Interface Récompenses du joueur (kal-games) : pour le général et chaque jeu joué, compteurs des paliers (semaine,
 * mois, permanent), prochain palier, niveau de prestige ; bouton de prestige quand il est possible (jamais obligatoire).
 * Les récompenses se récupèrent sur Event (/rewards).
 */
final class MenuJoueur {

    private final KGRewards plugin;
    private final Moteur moteur;
    private final Lang lang;
    private final Gui gui;

    MenuJoueur(KGRewards plugin, Moteur moteur) {
        this.plugin = plugin;
        this.moteur = moteur;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private static String pts(double valeur) {
        return fr.kalium.scoreboards.data.StatsService.formatPoints(valeur);
    }

    private List<String> grilles(UUID uuid) {
        List<String> liste = new ArrayList<>();
        liste.add(Moteur.GENERAL);
        KGScoreBoards sb = (KGScoreBoards) Bukkit.getPluginManager().getPlugin("KG_ScoreBoards");
        for (String jeu : sb.stats().minigamesWithPlayers()) {
            if (Moteur.score("general", jeu, uuid) > 0) {
                liste.add(jeu);
            }
        }
        return liste;
    }

    void ouvrir(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        List<Component> corps = new ArrayList<>();
        corps.add(lang.c("joueur.aide", "<gray>Chaque palier franchi et chaque top atteint donne une récompense, à récupérer "
                + "sur le serveur Event avec /rewards."));
        List<ActionButton> boutons = new ArrayList<>();
        for (String grille : grilles(uuid)) {
            int prestige = moteur.prestige(grille, uuid);
            double perm = moteur.compteur("permanent", grille, uuid);
            double semaine = moteur.compteur("semaine", grille, uuid);
            double mois = moteur.compteur("mois", grille, uuid);
            corps.add(lang.c("joueur.grille", "<gold><jeu><prestige></gold> <gray>- permanent <white><perm></white> "
                    + "(prochain palier <white><prochain-perm></white>), semaine <white><semaine></white> (<prochain-semaine>), "
                    + "mois <white><mois></white> (<prochain-mois>)",
                    "jeu", Moteur.nomGrille(grille), "prestige", prestige > 0 ? " (prestige " + prestige + ")" : "",
                    "perm", pts(perm), "prochain-perm", Moteur.prochainSeuil(perm),
                    "semaine", pts(semaine), "prochain-semaine", Moteur.prochainSeuil(semaine),
                    "mois", pts(mois), "prochain-mois", Moteur.prochainSeuil(mois)));
            if (moteur.prestigePossible(uuid, grille)) {
                boutons.add(gui.button(lang.c("joueur.bouton-prestige", "<light_purple>Prestige : <jeu>", "jeu",
                        Moteur.nomGrille(grille)), null, p -> confirmerPrestige(p, grille)));
            }
        }
        if (plugin.estAdmin(joueur)) {
            boutons.add(gui.button(lang.c("joueur.bouton-admin", "<red>Tables de butin"), null, plugin::ouvrirAdmin));
        }
        gui.open(joueur, lang.c("joueur.titre", "<light_purple><bold>Récompenses"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void confirmerPrestige(Player joueur, String grille) {
        int suivant = moteur.prestige(grille, joueur.getUniqueId()) + 1;
        gui.confirm(joueur, lang.c("prestige.titre", "<light_purple><bold>Prestige <niveau>", "niveau", suivant),
                lang.c("prestige.texte", "<white>Ton compteur des paliers permanents (<jeu>) repart de 0 ; les classements ne "
                        + "changent pas. Les paliers permanents rapporteront <light_purple>+<bonus> %</light_purple> de "
                        + "quantité. Ton nom s'affichera « <nom> (prestige <niveau>) ».", "jeu", Moteur.nomGrille(grille),
                        "bonus", suivant * 20, "nom", joueur.getName(), "niveau", suivant),
                p -> {
                    if (moteur.prestigePossible(p.getUniqueId(), grille)) {
                        moteur.prendrePrestige(p.getUniqueId(), grille);
                    }
                    ouvrir(p);
                }, this::ouvrir);
        lang.saveIfNeeded();
    }
}
