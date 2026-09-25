package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KV_Plots (cahier des charges : KV_Plots/CAHIER_DES_CHARGES.md) - serveur Kanvas, demande de LeKiwi06 (25-26/09/2026).
 * Version 1.0.0 : grille de plots, réservation (moyen / grand), éditeurs, protection WorldGuard, règles du monde.
 */
public final class KVPlots extends JavaPlugin {

    private World monde;
    private Grille grille;
    private Plots plots;
    private Regions regions;
    private Chantier chantier;
    private ModeleSol modeleSol;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String nomMonde = getConfig().getString("monde", "kanvas");
        monde = getServer().getWorld(nomMonde);
        if (monde == null) monde = new WorldCreator(nomMonde).createWorld(); // charge le dossier existant
        grille = new Grille(getConfig().getInt("grille.origine-x", 0), getConfig().getInt("grille.origine-z", 0),
                getConfig().getInt("grille.taille-plot", 49), getConfig().getInt("grille.largeur-route", 9),
                getConfig().getInt("grille.colonne-min", -5), getConfig().getInt("grille.colonne-max", 5),
                getConfig().getInt("grille.ligne-min", -5), getConfig().getInt("grille.ligne-max", 5));
        plots = new Plots(this);
        plots.charger();
        regions = new Regions(this);
        chantier = new Chantier(this);
        chargerModeleSol();

        regions.protegerMonde(monde);
        for (Plot p : plots.tous()) {
            regions.appliquer(p);
            if (p.fusionEnCours && modeleSol != null) fusionner(p, null); // reprise après un arrêt du serveur
        }

        getServer().getPluginManager().registerEvents(new ReglesMonde(this), this);
        PluginCommand plot = getCommand("plot");
        CommandePlot commande = new CommandePlot(this);
        plot.setExecutor(commande);
        plot.setTabCompleter(commande);
    }

    @Override
    public void onDisable() {
        if (plots != null) plots.sauver();
    }

    World monde() {
        return monde;
    }

    Grille grille() {
        return grille;
    }

    Plots plots() {
        return plots;
    }

    ModeleSol modeleSol() {
        return modeleSol;
    }

    /** Relevé au centre du plot (0, 0) au premier démarrage, tant que ce plot n'est pas réservé (donc vierge). */
    private void chargerModeleSol() {
        File fichier = new File(getDataFolder(), "modele-sol.yml");
        modeleSol = ModeleSol.charger(fichier);
        if (modeleSol != null) return;
        Grille.Case origine = new Grille.Case(0, 0);
        if (!plots.libre(origine)) {
            getLogger().severe("modele-sol.yml absent et plot (0, 0) déjà réservé : grands plots impossibles.");
            return;
        }
        int centre = grille.taille / 2;
        modeleSol = ModeleSol.relever(monde, grille.origineX + centre, grille.origineZ + centre);
        try {
            modeleSol.sauver(fichier);
            getLogger().info("Modèle de sol relevé au centre du plot (0, 0) et enregistré dans modele-sol.yml.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Impossible d'enregistrer modele-sol.yml", e);
        }
    }

    boolean contient(Plot p, int x, int z) {
        int minX = grille.minX(p.colonne), minZ = grille.minZ(p.ligne), cote = grille.cote(p.taille);
        return x >= minX && x < minX + cote && z >= minZ && z < minZ + cote;
    }

    /** Plot qui contient (x, z) dans le monde des plots (routes intérieures d'un grand plot comprises), ou null. */
    Plot plotEn(int x, int z) {
        Plot p = plots.en(grille.caseProche(x, z));
        return p != null && contient(p, x, z) ? p : null;
    }

    // --- Réservation ---

    /** Place au plus en même temps pour chaque taille (les déblocages à 100 points viendront plus tard). */
    int places(Taille t) {
        return t == Taille.GRAND ? getConfig().getInt("limites.grands", 1) : getConfig().getInt("limites.moyens", 1);
    }

    private boolean groupeLibre(Grille.Case coin) {
        for (int dc = 0; dc < 2; dc++) {
            for (int dl = 0; dl < 2; dl++) {
                Grille.Case c = new Grille.Case(coin.colonne() + dc, coin.ligne() + dl);
                if (!grille.dansLeMonde(c) || !plots.libre(c)) return false;
            }
        }
        return true;
    }

    /**
     * Emplacement pour un plot de cette taille : la case libre où se tient le joueur (pour un grand plot, un groupe de
     * 2 x 2 cases libres qui la contient, le plus proche du joueur) ; sinon le plus proche du centre de la grille.
     * Renvoie la case du coin nord-ouest, ou null s'il n'y a plus de place.
     */
    Grille.Case emplacement(Player joueur, Taille taille) throws Refus {
        Location l = joueur.getLocation();
        Grille.Case ici = l.getWorld().equals(monde) ? grille.caseEn(l.getBlockX(), l.getBlockZ()) : null;
        if (ici != null && grille.dansLeMonde(ici) && plots.libre(ici)) {
            if (taille == Taille.MOYEN) return ici;
            // Position du joueur en cases, ramenée au coin d'un groupe dont il occuperait le milieu.
            double milieu = 1.0 - grille.route / (2.0 * grille.pas);
            double cx = (l.getX() - grille.origineX) / grille.pas - milieu;
            double cz = (l.getZ() - grille.origineZ) / grille.pas - milieu;
            Grille.Case meilleur = null;
            for (int dc = -1; dc <= 0; dc++) {
                for (int dl = -1; dl <= 0; dl++) {
                    Grille.Case coin = new Grille.Case(ici.colonne() + dc, ici.ligne() + dl);
                    if (groupeLibre(coin) && (meilleur == null
                            || coin.distance2(cx, cz) < meilleur.distance2(cx, cz))) {
                        meilleur = coin;
                    }
                }
            }
            if (meilleur == null) throw new Refus("Pas assez de plots libres autour de celui-ci pour un grand plot.");
            return meilleur;
        }
        Grille.Case meilleur = null;
        double centre = taille == Taille.GRAND ? 0.5 : 0; // un grand plot est jugé sur son milieu
        int fin = taille == Taille.GRAND ? 1 : 0;
        for (int c = grille.colonneMin; c <= grille.colonneMax - fin; c++) {
            for (int li = grille.ligneMin; li <= grille.ligneMax - fin; li++) {
                Grille.Case coin = new Grille.Case(c, li);
                boolean libre = taille == Taille.GRAND ? groupeLibre(coin) : plots.libre(coin);
                if (libre && (meilleur == null || coin.distance2(-centre, -centre) < meilleur.distance2(-centre, -centre))) {
                    meilleur = coin;
                }
            }
        }
        return meilleur;
    }

    /** Réserve un plot pour le joueur et l'y téléporte (après les travaux pour un grand plot). */
    Plot reserver(Player joueur, Taille taille) throws Refus {
        int deja = plots.compter(joueur.getUniqueId(), taille);
        if (deja >= places(taille)) {
            throw new Refus("Tu as déjà " + deja + " plot" + (deja > 1 ? "s " : " ") + taille.nom
                    + (deja > 1 ? "s" : "") + " (maximum : " + places(taille) + ").");
        }
        if (taille == Taille.GRAND && modeleSol == null) {
            throw new Refus("Les grands plots ne sont pas disponibles pour le moment (modèle de sol absent).");
        }
        Grille.Case coin = emplacement(joueur, taille);
        if (coin == null) throw new Refus("Il n'y a plus de plot " + taille.nom + " libre.");
        Plot p = plots.creer(taille, coin, joueur.getUniqueId());
        if (taille == Taille.GRAND) p.fusionEnCours = true;
        plots.sauver();
        regions.appliquer(p);
        if (taille == Taille.GRAND) {
            joueur.sendMessage("§eTon grand plot n°" + p.id + " est en préparation (retrait des routes)...");
            fusionner(p, joueur);
        } else {
            teleporter(joueur, p);
        }
        return p;
    }

    /** Remplace les routes et bordures intérieures d'un grand plot par le sol des plots. */
    private void fusionner(Plot p, Player joueur) {
        int minX = grille.minX(p.colonne), minZ = grille.minZ(p.ligne), cote = grille.cote(Taille.GRAND);
        List<int[]> colonnes = new ArrayList<>();
        for (int dx = 0; dx < cote; dx++) {
            for (int dz = 0; dz < cote; dz++) {
                if (dx >= grille.taille && dx < grille.pas || dz >= grille.taille && dz < grille.pas) {
                    colonnes.add(new int[] {minX + dx, minZ + dz});
                }
            }
        }
        Iterator<int[]> it = colonnes.iterator();
        chantier.ajouter(new Chantier.Tache(it, () -> {
            p.fusionEnCours = false;
            plots.sauver();
            getLogger().info("Grand plot n°" + p.id + " prêt (routes intérieures retirées).");
            if (joueur != null && joueur.isOnline()) {
                joueur.sendMessage("§aTon grand plot n°" + p.id + " est prêt !");
                teleporter(joueur, p);
            }
        }));
    }

    void majRegion(Plot p) {
        regions.appliquer(p);
    }

    /** Sur la route, au milieu du bord nord du plot, tourné vers le plot. */
    void teleporter(Player joueur, Plot p) {
        int cote = grille.cote(p.taille);
        int x = grille.minX(p.colonne) + cote / 2, z = grille.minZ(p.ligne) - 3;
        int y = monde.getHighestBlockYAt(x, z) + 1;
        joueur.teleport(new Location(monde, x + 0.5, y, z + 0.5, 0f, 0f));
    }

    /** Action refusée, avec le message pour le joueur. */
    static final class Refus extends Exception {
        Refus(String message) {
            super(message, null, false, false);
        }
    }
}
