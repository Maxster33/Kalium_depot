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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KV_Plots (cahier des charges : KV_Plots/CAHIER_DES_CHARGES.md) - serveur Kanvas, demande de LeKiwi06 (25-26/09/2026).
 * 1.0.0 : génération de la grille, réservation (moyen / grand), éditeurs, protection WorldGuard, règles du monde.
 * 1.1.0 : remise à zéro et suppression d'un plot. 1.1.1 : les joueurs restent en créatif.
 * 1.2.0 : validation, votes (terracottas), déblocage d'une 2e place à 100 points.
 * 1.3.0 : titre et description, visites (API pour le menu des visites de KV_Menu). 1.3.1 : tableau sur le côté.
 * 1.4.0 : signalements (poudre de blaze, raisons, fichier, traitement par le staff dans KV_Menu).
 */
public final class KVPlots extends JavaPlugin {

    private World monde;
    private Grille grille;
    private Plots plots;
    private Signalements signalements;
    private Regions regions;
    private Chantier chantier;
    private ModeleTuile tuile;
    private boolean generation;
    private final Confirmations confirmations = new Confirmations();
    private ModeVote modeVote;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String nomMonde = getConfig().getString("monde", "Kanvas");
        monde = getServer().getWorld(nomMonde);
        if (monde == null) monde = new WorldCreator(nomMonde).createWorld(); // charge le dossier existant
        grille = new Grille(getConfig().getInt("grille.origine-x", 0), getConfig().getInt("grille.origine-z", 0),
                getConfig().getInt("grille.taille-plot", 49), getConfig().getInt("grille.largeur-route", 9),
                getConfig().getInt("grille.colonne-min", -5), getConfig().getInt("grille.colonne-max", 5),
                getConfig().getInt("grille.ligne-min", -5), getConfig().getInt("grille.ligne-max", 5));
        plots = new Plots(this);
        plots.charger();
        signalements = new Signalements(this);
        signalements.charger();
        regions = new Regions(this);
        chantier = new Chantier(this);
        chargerTuile();

        regions.protegerMonde(monde);
        for (Plot p : List.copyOf(plots.tous())) {
            if (p.chantier != Plot.Chantier.SUPPRESSION) regions.appliquer(p);
            if (tuile == null) continue;
            switch (p.chantier) { // reprise après un arrêt du serveur
                case FUSION -> fusionner(p, null);
                case REMISE_A_ZERO -> lancerRemise(p, null);
                case SUPPRESSION -> lancerSuppression(p, null);
                default -> { }
            }
        }

        getServer().getPluginManager().registerEvents(new ReglesMonde(this), this);
        getServer().getPluginManager().registerEvents(new EntreePlot(this), this);
        modeVote = new ModeVote(this);
        getServer().getPluginManager().registerEvents(modeVote, this);
        getServer().getServicesManager().register(KanvasPlots.class, new Api(this), this, ServicePriority.Normal);
        PluginCommand plot = getCommand("plot");
        CommandePlot commande = new CommandePlot(this);
        plot.setExecutor(commande);
        plot.setTabCompleter(commande);
        getCommand("kvadmin").setExecutor(new CommandeAdmin(this));
    }

    @Override
    public void onDisable() {
        if (modeVote != null) modeVote.toutRendre();
        if (plots != null) plots.sauver();
    }

    World monde() {
        return monde;
    }

    Grille grille() {
        return grille;
    }

    Confirmations confirmations() {
        return confirmations;
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

    /**
     * Plots de cette taille en travaux en même temps : 1 au départ, +1 dès qu'un plot de cette taille du joueur atteint
     * 100 points cumulés (au plus 2).
     */
    int places(UUID joueur, Taille t) {
        int base = t == Taille.GRAND ? getConfig().getInt("limites.grands", 1) : getConfig().getInt("limites.moyens", 1);
        int seuil = getConfig().getInt("limites.points-deblocage", 100);
        boolean debloque = false;
        for (Plot p : plots.duCreateur(joueur)) {
            if (p.taille == t && p.points() >= seuil) debloque = true;
        }
        return Math.min(getConfig().getInt("limites.maximum", 2), base + (debloque ? 1 : 0));
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
        if (taille == Taille.GRAND) p.chantier = Plot.Chantier.FUSION;
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
            p.chantier = Plot.Chantier.AUCUN;
            plots.sauver();
            getLogger().info("Grand plot n°" + p.id + " prêt (routes intérieures retirées).");
            if (joueur != null && joueur.isOnline()) {
                joueur.sendMessage("§aTon grand plot n°" + p.id + " est prêt !");
                teleporter(joueur, p);
            }
        }));
    }

    // --- Titre, description, visites ---

    static final int TITRE_MAX = 32, DESCRIPTION_MAX = 200;

    /** Vérifie la longueur (caractères visibles, codes couleur « & » non comptés). */
    private static String nettoyer(String texte, int max, String quoi) throws Refus {
        texte = texte == null ? "" : texte.strip();
        int visibles = PlainTextComponentSerializer.plainText().serialize(EntreePlot.texte(texte)).length();
        if (visibles > max || texte.length() > max * 3) {
            throw new Refus(quoi + " : " + visibles
                    + " caractères (maximum " + max + ").");
        }
        return texte;
    }

    /** Titre et description (vides = retirés), par le créateur, à tout moment (même plot validé). */
    void definirLore(Player joueur, Plot p, String titre, String description) throws Refus {
        if (!p.createur.equals(joueur.getUniqueId())) throw new Refus("Seul le créateur du plot choisit son titre et sa description.");
        String t = nettoyer(titre, TITRE_MAX, "Titre trop long");
        String d = nettoyer(description, DESCRIPTION_MAX, "Description trop longue");
        p.titre = t;
        p.description = d;
        plots.sauver();
    }

    /** Plot validé au hasard que le joueur peut noter et n'a pas encore noté, ou null. */
    Plot hasardANoter(UUID joueur) {
        List<Plot> choix = new ArrayList<>();
        for (Plot p : plots.tous()) {
            if (p.votable(joueur) && !p.votes.containsKey(joueur)) choix.add(p);
        }
        return choix.isEmpty() ? null : choix.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(choix.size()));
    }

    // --- Validation, votes ---

    /** Le créateur fige son plot : il devient votable et libère sa place. */
    void valider(Player joueur, Plot p) throws Refus {
        if (!p.createur.equals(joueur.getUniqueId())) throw new Refus("Seul le créateur du plot peut le valider.");
        if (p.etat == Plot.Etat.VALIDE) throw new Refus("Ce plot est déjà validé.");
        if (p.chantier != Plot.Chantier.AUCUN) throw new Refus("Des travaux sont en cours sur ce plot.");
        p.etat = Plot.Etat.VALIDE;
        plots.sauver();
        regions.appliquer(p);
        modeVote.rafraichir();
        getLogger().info("Plot n°" + p.id + " validé par " + joueur.getName() + ".");
    }

    /** Le créateur rouvre son plot validé : il lui faut une place libre de la même taille ; les votes sont gardés. */
    void rouvrir(Player joueur, Plot p) throws Refus {
        if (!p.createur.equals(joueur.getUniqueId())) throw new Refus("Seul le créateur du plot peut le rouvrir.");
        if (p.etat != Plot.Etat.VALIDE) throw new Refus("Ce plot n'est pas validé.");
        if (p.chantier != Plot.Chantier.AUCUN) throw new Refus("Des travaux sont en cours sur ce plot.");
        UUID u = joueur.getUniqueId();
        if (plots.compter(u, p.taille) >= places(u, p.taille)) {
            throw new Refus("Il te faut une place " + p.taille.nom + " libre pour rouvrir ce plot : valide ou supprime d'abord"
                    + " un de tes plots " + p.taille.nom + "s en travaux.");
        }
        p.etat = Plot.Etat.TRAVAUX;
        plots.sauver();
        regions.appliquer(p);
        modeVote.rafraichir();
        getLogger().info("Plot n°" + p.id + " rouvert par " + joueur.getName() + ".");
    }

    /** Vote de 1 à 5 ; un nouveau vote du même joueur remplace l'ancien. */
    void voter(Player joueur, Plot p, int note) throws Refus {
        if (note < 1 || note > 5) throw new Refus("La note va de 1 à 5.");
        UUID u = joueur.getUniqueId();
        if (!p.estExterieur(u)) throw new Refus("Tu ne peux pas noter un plot dont tu es (ou as été) créateur ou éditeur.");
        if (p.etat != Plot.Etat.VALIDE || p.chantier != Plot.Chantier.AUCUN) {
            throw new Refus("Ce plot n'est pas validé : on ne peut pas le noter pour le moment.");
        }
        p.votes.put(u, new Plot.Vote(note, System.currentTimeMillis()));
        plots.sauver();
    }

    ModeVote modeVote() {
        return modeVote;
    }

    // --- Signalements ---

    /** Raisons proposées (plus une case « Autre » avec texte libre). */
    static final List<String> RAISONS = List.of("Contenu inapproprié", "Copie d'un autre build", "Plot vide ou bâclé",
            "Triche aux votes");

    Signalements signalements() {
        return signalements;
    }

    /** Un joueur signale un plot dont il n'est ni créateur ni éditeur (un seul signalement non traité par plot). */
    Signalements.Signalement signaler(Player joueur, Plot p, List<String> raisons, String autre) throws Refus {
        UUID u = joueur.getUniqueId();
        if (!p.estExterieur(u)) throw new Refus("Tu ne peux pas signaler un plot dont tu es (ou as été) créateur ou éditeur.");
        List<String> choisies = new ArrayList<>();
        for (String r : raisons) {
            if (RAISONS.contains(r) && !choisies.contains(r)) choisies.add(r);
        }
        autre = autre == null ? "" : autre.strip();
        if (autre.length() > 200) autre = autre.substring(0, 200);
        if (choisies.isEmpty() && autre.isEmpty()) throw new Refus("Coche au moins une raison, ou écris-la dans « Autre ».");
        if (signalements.ouvert(u, p.id) != null) {
            throw new Refus("Tu as déjà signalé ce plot : le staff n'a pas encore traité ton signalement.");
        }
        Signalements.Signalement s = signalements.ajouter(p.id, u, choisies, autre);
        String resume = "Signalement n°" + s.id + " : plot n°" + p.id + " de " + nom(p.createur) + ", par " + joueur.getName()
                + " (" + String.join(", ", choisies) + (autre.isEmpty() ? "" : (choisies.isEmpty() ? "" : ", ") + "« " + autre + " »") + ")";
        getLogger().info(resume);
        for (Player staff : getServer().getOnlinePlayers()) {
            if (staff.hasPermission("kvplots.admin")) staff.sendMessage("§c[Kanvas] §f" + resume + " §7- menu Kanvas > Signalements");
        }
        return s;
    }

    /** Le staff classe un signalement (traité), en notant l'action faite. */
    void classer(Player staff, Signalements.Signalement s, String action) throws Refus {
        if (!staff.hasPermission("kvplots.admin")) throw new Refus("Réservé au staff.");
        if (s.classe) throw new Refus("Ce signalement est déjà classé.");
        s.classe = true;
        s.traitePar = staff.getUniqueId();
        s.action = action;
        signalements.sauver();
    }

    /** Le staff dévalide un plot : il repasse en travaux (même sans place libre) ; ses votes sont gardés. */
    void devalider(Player staff, Plot p) throws Refus {
        if (!staff.hasPermission("kvplots.admin")) throw new Refus("Réservé au staff.");
        if (p.etat != Plot.Etat.VALIDE) throw new Refus("Ce plot n'est pas validé.");
        if (p.chantier != Plot.Chantier.AUCUN) throw new Refus("Des travaux sont en cours sur ce plot.");
        p.etat = Plot.Etat.TRAVAUX;
        plots.sauver();
        regions.appliquer(p);
        modeVote.rafraichir();
        getLogger().info("Plot n°" + p.id + " dévalidé par " + staff.getName() + ".");
    }

    // --- Remise à zéro, suppression ---

    /** Colonnes de tout l'intérieur du plot (routes intérieures d'un grand plot comprises), chunk par chunk. */
    private Iterator<int[]> colonnesDe(Plot p) {
        int minX = grille.minX(p.colonne), minZ = grille.minZ(p.ligne), cote = grille.cote(p.taille);
        return parChunks(minX, minZ, minX + cote - 1, minZ + cote - 1);
    }

    /** Retire les entités du plot (sauf les joueurs), dans les chunks chargés. */
    private void retirerEntites(Plot p) {
        int minX = grille.minX(p.colonne), minZ = grille.minZ(p.ligne), cote = grille.cote(p.taille);
        BoundingBox zone = new BoundingBox(minX, monde.getMinHeight(), minZ, minX + cote, monde.getMaxHeight(), minZ + cote);
        for (Entity e : monde.getNearbyEntities(zone)) {
            if (!(e instanceof Player)) e.remove();
        }
    }

    /**
     * Qui peut remettre à zéro / supprimer ce plot : son créateur, sauf si le plot est validé ; le staff (kvplots.admin)
     * toujours.
     */
    void verifierTravaux(Player demandeur, Plot p, String action, String participe) throws Refus {
        if (tuile == null) throw new Refus("Impossible pour le moment (modèle de terrain absent).");
        if (p.chantier != Plot.Chantier.AUCUN) throw new Refus("Des travaux sont déjà en cours sur ce plot.");
        if (demandeur.hasPermission("kvplots.admin")) return;
        if (!p.createur.equals(demandeur.getUniqueId())) throw new Refus("Seul le créateur du plot peut le " + action + ".");
        if (p.etat == Plot.Etat.VALIDE) throw new Refus("Un plot validé ne peut être " + participe + " que par le staff.");
    }

    /** Remet le terrain du plot à l'état vierge ; le plot reste réservé, avec ses éditeurs. */
    void remettreAZero(Player demandeur, Plot p) throws Refus {
        verifierTravaux(demandeur, p, "remettre à zéro", "remis à zéro");
        lancerRemise(p, demandeur);
    }

    private void lancerRemise(Plot p, Player informe) {
        p.chantier = Plot.Chantier.REMISE_A_ZERO;
        plots.sauver();
        if (modeVote != null) modeVote.rafraichir();
        retirerEntites(p);
        chantier.ajouter(new Chantier.Tache(colonnesDe(p), (x, z) -> grille.caseEn(x, z) != null
                ? tuile.poserGrille(monde, x, z, grille.origineX, grille.origineZ)
                : tuile.poserSol(monde, x, z, grille.taille), () -> {
            retirerEntites(p);
            p.chantier = Plot.Chantier.AUCUN;
            plots.sauver();
            modeVote.rafraichir();
            getLogger().info("Plot n°" + p.id + " remis à zéro.");
            if (informe != null && informe.isOnline()) informe.sendMessage("§aPlot n°" + p.id + " remis à zéro.");
        }));
    }

    /** Remet le terrain à l'état vierge et libère le plot (un grand plot redevient 4 plots moyens). */
    void supprimer(Player demandeur, Plot p) throws Refus {
        verifierTravaux(demandeur, p, "supprimer", "supprimé");
        lancerSuppression(p, demandeur);
    }

    private void lancerSuppression(Plot p, Player informe) {
        p.chantier = Plot.Chantier.SUPPRESSION;
        plots.sauver();
        if (modeVote != null) modeVote.rafraichir();
        regions.supprimer(p); // plus personne n'y construit pendant les travaux
        retirerEntites(p);
        chantier.ajouter(new Chantier.Tache(colonnesDe(p),
                (x, z) -> tuile.poserGrille(monde, x, z, grille.origineX, grille.origineZ), () -> {
            retirerEntites(p);
            plots.retirer(p);
            plots.sauver();
            getLogger().info("Plot n°" + p.id + " supprimé et libéré.");
            if (informe != null && informe.isOnline()) informe.sendMessage("§aPlot n°" + p.id + " supprimé : la place est libre.");
        }));
    }

    void ajouterEditeur(Player createur, Plot p, UUID editeur) throws Refus {
        verifierCreateur(createur, p, editeur);
        if (!p.editeurs.add(editeur)) throw new Refus(nom(editeur) + " est déjà éditeur de ce plot.");
        p.historiqueEditeurs.add(editeur);
        plots.sauver();
        regions.appliquer(p);
        modeVote.rafraichir();
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
