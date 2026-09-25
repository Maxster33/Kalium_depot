package fr.kalium.kvplots;

import fr.kalium.kvplots.api.KanvasPlots;
import fr.kalium.kvplots.api.KanvasPlots.Refus;
import fr.kalium.kvplots.api.Taille;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KV_Plots (cahier des charges : KV_Plots/CAHIER_DES_CHARGES.md) - serveur Kanvas, demande de LeKiwi06 (25-26/09/2026).
 * Version 1.0.0 : génération de la grille, réservation (moyen / grand), éditeurs, protection WorldGuard, règles du
 * monde.
 */
public final class KVPlots extends JavaPlugin {

    private World monde;
    private Grille grille;
    private Plots plots;
    private Regions regions;
    private Chantier chantier;
    private ModeleTuile tuile;
    private boolean generation;

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
        chargerTuile();

        regions.protegerMonde(monde);
        for (Plot p : plots.tous()) {
            regions.appliquer(p);
            if (p.fusionEnCours && tuile != null) fusionner(p, null); // reprise après un arrêt du serveur
        }

        getServer().getPluginManager().registerEvents(new ReglesMonde(this), this);
        getServer().getServicesManager().register(KanvasPlots.class, new Api(this), this, ServicePriority.Normal);
        PluginCommand plot = getCommand("plot");
        CommandePlot commande = new CommandePlot(this);
        plot.setExecutor(commande);
        plot.setTabCompleter(commande);
        getCommand("kvadmin").setExecutor(new CommandeAdmin(this));
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

    ModeleTuile tuile() {
        return tuile;
    }

    private File fichierTuile() {
        return new File(getDataFolder(), "modele-tuile.yml");
    }

    /** Relevée autour du plot (0, 0) au premier démarrage, tant que ce plot n'est pas réservé (donc vierge). */
    private void chargerTuile() {
        tuile = ModeleTuile.charger(fichierTuile(), grille.pas);
        if (tuile != null) return;
        if (!plots.libre(new Grille.Case(0, 0))) {
            getLogger().severe("modele-tuile.yml absent et plot (0, 0) déjà réservé : génération et grands plots impossibles.");
            return;
        }
        tuile = ModeleTuile.relever(monde, grille.origineX, grille.origineZ, grille.pas);
        try {
            tuile.sauver(fichierTuile());
            getLogger().info("Tuile de référence relevée autour du plot (0, 0) et enregistrée dans modele-tuile.yml.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Impossible d'enregistrer modele-tuile.yml", e);
        }
    }

    // --- Génération de la grille ---

    /** Zone générée : les plots de la grille configurée et les routes tout autour. {minX, minZ, maxX, maxZ} */
    int[] zoneGrille() {
        return new int[] {grille.minX(grille.colonneMin) - grille.route, grille.minZ(grille.ligneMin) - grille.route,
                grille.minX(grille.colonneMax) + grille.pas - 1, grille.minZ(grille.ligneMax) + grille.pas - 1};
    }

    boolean generationEnCours() {
        return generation;
    }

    /**
     * Recopie la tuile de référence sur toute la zone de la grille (plots vierges, routes, bordures). Les plots
     * réservés ne sont pas touchés. Avance chunk par chunk.
     */
    void generer(Runnable fin) {
        int[] z0 = zoneGrille();
        generation = true;
        long total = (long) (z0[2] - z0[0] + 1) * (z0[3] - z0[1] + 1);
        long[] faites = {0};
        int[] palier = {0};
        Iterator<int[]> colonnes = parChunks(z0[0], z0[1], z0[2], z0[3]);
        chantier.ajouter(new Chantier.Tache(colonnes, (x, z) -> {
            faites[0]++;
            int pourcent = (int) (faites[0] * 100 / total);
            if (pourcent / 10 > palier[0]) {
                palier[0] = pourcent / 10;
                getLogger().info("Génération de la grille : " + pourcent + " %");
            }
            return plotEn(x, z) != null ? 1 : tuile.poserGrille(monde, x, z, grille.origineX, grille.origineZ);
        }, () -> {
            generation = false;
            getLogger().info("Génération de la grille terminée.");
            fin.run();
        }));
    }

    /** Colonnes d'un rectangle, chunk par chunk (moins de chunks chargés à la fois). */
    private static Iterator<int[]> parChunks(int minX, int minZ, int maxX, int maxZ) {
        return new Iterator<>() {
            int cx = minX >> 4, cz = minZ >> 4, i = 0;
            int[] suivant = chercher();

            private int[] chercher() {
                while (cx <= maxX >> 4) {
                    while (i < 256) {
                        int x = (cx << 4) + (i >> 4), z = (cz << 4) + (i & 15);
                        i++;
                        if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) return new int[] {x, z};
                    }
                    i = 0;
                    if (++cz > maxZ >> 4) {
                        cz = minZ >> 4;
                        cx++;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return suivant != null;
            }

            @Override
            public int[] next() {
                int[] r = suivant;
                suivant = chercher();
                return r;
            }
        };
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

    /** Places en même temps pour chaque taille (les déblocages à 100 points viendront plus tard). */
    int places(UUID joueur, Taille t) {
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
        if (deja >= places(joueur.getUniqueId(), taille)) {
            throw new Refus("Tu as déjà " + deja + " plot" + (deja > 1 ? "s " : " ") + taille.nom
                    + (deja > 1 ? "s" : "") + " (maximum : " + places(joueur.getUniqueId(), taille) + ").");
        }
        if (taille == Taille.GRAND && tuile == null) {
            throw new Refus("Les grands plots ne sont pas disponibles pour le moment (modèle de terrain absent).");
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
        chantier.ajouter(new Chantier.Tache(it, (x, z) -> tuile.poserSol(monde, x, z, grille.taille), () -> {
            p.fusionEnCours = false;
            plots.sauver();
            getLogger().info("Grand plot n°" + p.id + " prêt (routes intérieures retirées).");
            if (joueur != null && joueur.isOnline()) {
                joueur.sendMessage("§aTon grand plot n°" + p.id + " est prêt !");
                teleporter(joueur, p);
            }
        }));
    }

    void ajouterEditeur(Player createur, Plot p, UUID editeur) throws Refus {
        verifierCreateur(createur, p, editeur);
        if (!p.editeurs.add(editeur)) throw new Refus(nom(editeur) + " est déjà éditeur de ce plot.");
        p.historiqueEditeurs.add(editeur);
        plots.sauver();
        regions.appliquer(p);
    }

    void retirerEditeur(Player createur, Plot p, UUID editeur) throws Refus {
        verifierCreateur(createur, p, editeur);
        if (!p.editeurs.remove(editeur)) throw new Refus(nom(editeur) + " n'est pas éditeur de ce plot.");
        plots.sauver();
        regions.appliquer(p);
    }

    private static void verifierCreateur(Player createur, Plot p, UUID editeur) throws Refus {
        if (!p.createur.equals(createur.getUniqueId())) throw new Refus("Seul le créateur du plot gère ses éditeurs.");
        if (p.createur.equals(editeur)) throw new Refus("Tu es déjà le créateur de ce plot.");
    }

    static String nom(UUID u) {
        String n = org.bukkit.Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    /** Sur la route, au milieu du bord nord du plot, tourné vers le plot. */
    void teleporter(Player joueur, Plot p) {
        int cote = grille.cote(p.taille);
        int x = grille.minX(p.colonne) + cote / 2, z = grille.minZ(p.ligne) - 3;
        int y = monde.getHighestBlockYAt(x, z) + 1;
        joueur.teleport(new Location(monde, x + 0.5, y, z + 0.5, 0f, 0f));
    }
}
