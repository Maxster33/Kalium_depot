package fr.kalium.kvbuildbattle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Arène du Build Battle (demande de LeKiwi06, 27/09/2026) : l'admin capture UNE boîte (la boîte entière, sa zone
 * constructible et le point d'apparition de l'équipe), puis le plugin la recopie dans le monde à part en colonnes
 * (une colonne par partie, 4 x 8 par défaut), assez éloignées pour qu'on ne voie pas les pseudos des autres parties.
 * La salle d'attente est capturée de la même façon et recopiée au nord de chaque colonne (une copie par partie).
 *
 * Chaque zone constructible a sa région WorldGuard (kv_bb_<colonne>_<boîte>), sans membre pour l'instant : personne
 * n'y construit (FAWE compris) hors opérateurs. Le reste du monde est en « passthrough deny ».
 */
final class Arene {

    static final String PREFIXE_REGION = "kv_bb_";

    /** Positions posées par l'admin (clé -> lieu), gardées dans positions.yml. */
    static final List<String> POSITIONS = List.of("boite.pos1", "boite.pos2", "boite.zone1", "boite.zone2",
            "boite.apparition", "salle.pos1", "salle.pos2", "salle.apparition");

    private final KVBuildBattle plugin;
    private final File fichierPositions, fichierArene, fichierBoite, fichierSalle;
    private final YamlConfiguration positions;
    private Modele boite, salle;
    /** Zone constructible, relative au coin le plus bas de la boîte (0 = pas encore capturée). */
    private int zx, zy, zz, zsx, zsy, zsz;
    /** Taille des boîtes / salles collées dans le monde (0 = rien de collé). */
    private int gx, gy, gz, sgx, sgy, sgz;

    Arene(KVBuildBattle plugin) {
        this.plugin = plugin;
        File dossier = plugin.getDataFolder();
        fichierPositions = new File(dossier, "positions.yml");
        fichierArene = new File(dossier, "arene.yml");
        fichierBoite = new File(dossier, "boite.kvbb");
        fichierSalle = new File(dossier, "salle.kvbb");
        positions = YamlConfiguration.loadConfiguration(fichierPositions);
        YamlConfiguration a = YamlConfiguration.loadConfiguration(fichierArene);
        zx = a.getInt("zone.x");
        zy = a.getInt("zone.y");
        zz = a.getInt("zone.z");
        zsx = a.getInt("zone.sx");
        zsy = a.getInt("zone.sy");
        zsz = a.getInt("zone.sz");
        gx = a.getInt("boites-collees.sx");
        gy = a.getInt("boites-collees.sy");
        gz = a.getInt("boites-collees.sz");
        sgx = a.getInt("salles-collees.sx");
        sgy = a.getInt("salles-collees.sy");
        sgz = a.getInt("salles-collees.sz");
        boite = lire(fichierBoite);
        salle = lire(fichierSalle);
    }

    private Modele lire(File f) {
        if (!f.exists()) return null;
        try {
            return Modele.charger(f);
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, f.getName() + " illisible", e);
            return null;
        }
    }

    private void sauverArene() {
        YamlConfiguration a = new YamlConfiguration();
        a.set("zone.x", zx);
        a.set("zone.y", zy);
        a.set("zone.z", zz);
        a.set("zone.sx", zsx);
        a.set("zone.sy", zsy);
        a.set("zone.sz", zsz);
        a.set("boites-collees.sx", gx);
        a.set("boites-collees.sy", gy);
        a.set("boites-collees.sz", gz);
        a.set("salles-collees.sx", sgx);
        a.set("salles-collees.sy", sgy);
        a.set("salles-collees.sz", sgz);
        try {
            a.save(fichierArene);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer arene.yml", e);
        }
    }

    private void sauverModele(Modele m, File f) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                m.sauver(f);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer " + f.getName(), e);
            }
        });
    }

    // --- Disposition ---

    int colonnes() {
        return Math.max(1, plugin.getConfig().getInt("disposition.colonnes", 4));
    }

    int boitesParColonne() {
        return Math.max(1, plugin.getConfig().getInt("disposition.boites-par-colonne", 8));
    }

    private int ox() {
        return plugin.getConfig().getInt("disposition.origine-x", 10_000);
    }

    private int oy() {
        return plugin.getConfig().getInt("disposition.origine-y", 64);
    }

    private int oz() {
        return plugin.getConfig().getInt("disposition.origine-z", 0);
    }

    /** Distance entre le bord ouest de deux colonnes (fixe : ne dépend pas de la taille des constructions). */
    private int pasColonnes() {
        return Math.max(16, plugin.getConfig().getInt("disposition.pas-colonnes", 600));
    }

    /** La construction tient-elle dans la largeur d'une colonne, avec au moins 64 blocs vides jusqu'à la suivante ? */
    private boolean tientDansColonne(CommandSender qui, Modele m, String quoi) {
        if (m.sx + 64 <= pasColonnes()) return true;
        dire(qui, "§c" + quoi + " trop large (" + m.sx + " blocs) pour des colonnes espacées de " + pasColonnes()
                + " blocs : augmente disposition.pas-colonnes dans config.yml.");
        return false;
    }

    private int pasBoites(int profondeur) {
        return profondeur + Math.max(0, plugin.getConfig().getInt("disposition.ecart-boites", 8));
    }

    private int[] coinBoite(int colonne, int rang, int profondeur) {
        return new int[] {ox() + colonne * pasColonnes(), oy(), oz() + rang * pasBoites(profondeur)};
    }

    private int[] coinSalle(int colonne, int profondeur) {
        return new int[] {ox() + colonne * pasColonnes(), oy(),
                oz() - Math.max(0, plugin.getConfig().getInt("disposition.ecart-salle", 32)) - profondeur};
    }

    boolean boiteCapturee() {
        return boite != null && zsx > 0;
    }

    boolean salleCapturee() {
        return salle != null;
    }

    boolean boitesCollees() {
        return gx > 0;
    }

    boolean sallesCollees() {
        return sgx > 0;
    }

    /** Point d'apparition dans une boîte collée (colonne et rang à partir de 0), ou null. */
    Location apparitionBoite(int colonne, int rang) {
        if (boite == null || !boitesCollees()) return null;
        int[] c = coinBoite(colonne, rang, boite.sz);
        return boite.apparition(plugin.monde(), c[0], c[1], c[2]);
    }

    /** Point d'apparition dans la salle d'attente d'une colonne, ou null. */
    Location apparitionSalle(int colonne) {
        if (salle == null || !sallesCollees()) return null;
        int[] c = coinSalle(colonne, salle.sz);
        return salle.apparition(plugin.monde(), c[0], c[1], c[2]);
    }

    /** Numéro de la zone constructible (colonne * boîtes + rang) contenant ce bloc, ou -1. */
    int zoneEn(int x, int y, int z) {
        if (!boitesCollees() || zsx <= 0) return -1;
        for (int c = 0; c < colonnes(); c++) {
            for (int r = 0; r < boitesParColonne(); r++) {
                int[] o = coinBoite(c, r, gz);
                int x0 = o[0] + zx, y0 = o[1] + zy, z0 = o[2] + zz;
                if (x >= x0 && x < x0 + zsx && y >= y0 && y < y0 + zsy && z >= z0 && z < z0 + zsz) {
                    return c * boitesParColonne() + r;
                }
            }
        }
        return -1;
    }

    // --- Positions ---

    Location position(String cle) {
        String b = cle + ".";
        if (!positions.isConfigurationSection(cle)) return null;
        World monde = Bukkit.getWorld(positions.getString(b + "monde", ""));
        if (monde == null) return null;
        return new Location(monde, positions.getDouble(b + "x"), positions.getDouble(b + "y"), positions.getDouble(b + "z"),
                (float) positions.getDouble(b + "yaw"), (float) positions.getDouble(b + "pitch"));
    }

    void poser(Player joueur, String cle) {
        Location l = joueur.getLocation();
        String b = cle + ".";
        positions.set(b + "monde", l.getWorld().getName());
        positions.set(b + "x", l.getX());
        positions.set(b + "y", l.getY());
        positions.set(b + "z", l.getZ());
        positions.set(b + "yaw", (double) l.getYaw());
        positions.set(b + "pitch", (double) l.getPitch());
        try {
            positions.save(fichierPositions);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible d'enregistrer positions.yml", e);
        }
        plugin.getLogger().info("Position " + cle + " posée par " + joueur.getName() + " en " + texte(l));
    }

    static String texte(Location l) {
        if (l == null) return "(non définie)";
        return String.format(Locale.ROOT, "%s %.1f %.1f %.1f", l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
    }

    // --- Captures ---

    /** Capture la boîte (coins, zone constructible, apparition). Renvoie false si refusé (message déjà envoyé). */
    boolean capturerBoite(CommandSender qui) {
        Location p1 = position("boite.pos1"), p2 = position("boite.pos2");
        Location z1 = position("boite.zone1"), z2 = position("boite.zone2"), ap = position("boite.apparition");
        if (p1 == null || p2 == null || z1 == null || z2 == null || ap == null) {
            qui.sendMessage("§cPose d'abord les 2 coins de la boîte, les 2 coins de la zone constructible et le point d'apparition.");
            return false;
        }
        World monde = p1.getWorld();
        if (!monde.equals(p2.getWorld()) || !monde.equals(z1.getWorld()) || !monde.equals(z2.getWorld())
                || !monde.equals(ap.getWorld())) {
            qui.sendMessage("§cLes coins de la boîte, de la zone et le point d'apparition doivent être dans le même monde.");
            return false;
        }
        int minX = Math.min(p1.getBlockX(), p2.getBlockX()), maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.max(monde.getMinHeight(), Math.min(p1.getBlockY(), p2.getBlockY()));
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()), maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        int zminX = Math.min(z1.getBlockX(), z2.getBlockX()), zmaxX = Math.max(z1.getBlockX(), z2.getBlockX());
        int zminY = Math.min(z1.getBlockY(), z2.getBlockY()), zmaxY = Math.max(z1.getBlockY(), z2.getBlockY());
        int zminZ = Math.min(z1.getBlockZ(), z2.getBlockZ()), zmaxZ = Math.max(z1.getBlockZ(), z2.getBlockZ());
        if (zminX < minX || zmaxX > maxX || zminY < minY || zmaxY > maxY || zminZ < minZ || zmaxZ > maxZ) {
            qui.sendMessage("§cLa zone constructible doit être entièrement dans la boîte.");
            return false;
        }
        if (travaux().occupe()) {
            qui.sendMessage("§cDes travaux sont déjà en cours, patiente.");
            return false;
        }
        int nzx = zminX - minX, nzy = zminY - minY, nzz = zminZ - minZ;
        int nzsx = zmaxX - zminX + 1, nzsy = zmaxY - zminY + 1, nzsz = zmaxZ - zminZ + 1;
        try {
            travaux().capturer(p1, p2, ap, m -> {
                boite = m;
                zx = nzx;
                zy = nzy;
                zz = nzz;
                zsx = nzsx;
                zsy = nzsy;
                zsz = nzsz;
                sauverArene();
                sauverModele(m, fichierBoite);
                dire(qui, "§aBoîte capturée (" + m.sx + "x" + m.sy + "x" + m.sz + ", zone constructible " + zsx + "x"
                        + zsy + "x" + zsz + "). §7Utilise « Générer l'arène » pour la recopier "
                        + colonnes() * boitesParColonne() + " fois dans le monde du Build Battle.");
            }, erreur -> dire(qui, "§c" + erreur));
        } catch (IllegalArgumentException e) {
            qui.sendMessage("§c" + e.getMessage());
            return false;
        }
        qui.sendMessage("§eCapture de la boîte en cours (petit à petit, un message confirmera la fin)...");
        return true;
    }

    /** Capture la salle d'attente puis la recopie au nord de chaque colonne (les anciennes copies sont effacées). */
    boolean capturerSalle(CommandSender qui) {
        Location p1 = position("salle.pos1"), p2 = position("salle.pos2"), ap = position("salle.apparition");
        if (p1 == null || p2 == null || ap == null) {
            qui.sendMessage("§cPose d'abord les 2 coins de la salle d'attente et son point d'apparition.");
            return false;
        }
        if (!p1.getWorld().equals(p2.getWorld()) || !p1.getWorld().equals(ap.getWorld())) {
            qui.sendMessage("§cLes coins de la salle et le point d'apparition doivent être dans le même monde.");
            return false;
        }
        if (travaux().occupe()) {
            qui.sendMessage("§cDes travaux sont déjà en cours, patiente.");
            return false;
        }
        try {
            travaux().capturer(p1, p2, ap, m -> {
                if (!tientDansColonne(qui, m, "Salle d'attente")) return;
                effacerSalles();
                salle = m;
                sauverModele(m, fichierSalle);
                World monde = plugin.monde();
                for (int c = 0; c < colonnes(); c++) {
                    int[] o = coinSalle(c, m.sz);
                    travaux().coller(m, monde, o[0], o[1], o[2], null);
                }
                sgx = m.sx;
                sgy = m.sy;
                sgz = m.sz;
                sauverArene();
                travaux().apres(() -> dire(qui, "§aSalle d'attente capturée (" + m.sx + "x" + m.sy + "x" + m.sz
                        + ") et recopiée pour les " + colonnes() + " parties."));
            }, erreur -> dire(qui, "§c" + erreur));
        } catch (IllegalArgumentException e) {
            qui.sendMessage("§c" + e.getMessage());
            return false;
        }
        qui.sendMessage("§eCapture de la salle d'attente en cours (petit à petit, un message confirmera la fin)...");
        return true;
    }

    private void effacerSalles() {
        if (!sallesCollees()) return;
        for (int c = 0; c < colonnes(); c++) {
            int[] o = coinSalle(c, sgz);
            travaux().effacer(plugin.monde(), o[0], o[1], o[2], sgx, sgy, sgz, null);
        }
        sgx = sgy = sgz = 0;
    }

    /** Recopie la boîte capturée dans toutes les colonnes (les anciennes boîtes sont effacées) et crée les régions. */
    boolean generer(CommandSender qui) {
        if (!boiteCapturee()) {
            qui.sendMessage("§cCapture d'abord la boîte.");
            return false;
        }
        if (travaux().occupe()) {
            qui.sendMessage("§cDes travaux sont déjà en cours, patiente.");
            return false;
        }
        World monde = plugin.monde();
        Modele m = boite;
        if (!tientDansColonne(qui, m, "Boîte")) return false;
        boolean memeTaille = gx == m.sx && gy == m.sy && gz == m.sz;
        if (boitesCollees() && !memeTaille) {
            // Anciennes boîtes d'une autre taille : effacées avant de coller les nouvelles.
            for (int c = 0; c < colonnes(); c++) {
                for (int r = 0; r < boitesParColonne(); r++) {
                    int[] o = coinBoite(c, r, gz);
                    travaux().effacer(monde, o[0], o[1], o[2], gx, gy, gz, null);
                }
            }
        }
        gx = m.sx;
        gy = m.sy;
        gz = m.sz;
        sauverArene();
        int total = colonnes() * boitesParColonne();
        for (int c = 0; c < colonnes(); c++) {
            for (int r = 0; r < boitesParColonne(); r++) {
                int[] o = coinBoite(c, r, m.sz);
                travaux().coller(m, monde, o[0], o[1], o[2], null);
            }
        }
        travaux().apres(() -> {
            creerRegions();
            dire(qui, "§aArène générée : " + total + " boîtes (" + colonnes() + " colonnes de " + boitesParColonne()
                    + "), zones constructibles protégées.");
        });
        qui.sendMessage("§eGénération de " + total + " boîtes en cours (petit à petit, un message confirmera la fin)...");
        return true;
    }

    private RegionManager regions() {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(plugin.monde()));
    }

    /** Hors des zones, seuls les opérateurs construisent (région globale en passthrough deny). */
    void protegerMonde() {
        RegionManager rm = regions();
        if (rm == null) {
            plugin.getLogger().severe("WorldGuard ne gère pas le monde " + plugin.monde().getName() + " : aucune protection !");
            return;
        }
        ProtectedRegion global = rm.getRegion(ProtectedRegion.GLOBAL_REGION);
        if (global == null) {
            global = new GlobalProtectedRegion(ProtectedRegion.GLOBAL_REGION);
            rm.addRegion(global);
        }
        global.setFlag(Flags.PASSTHROUGH, StateFlag.State.DENY);
        enregistrer(rm);
    }

    /** Une région par zone constructible, sans membre (les équipes y seront ajoutées pendant les parties). */
    private void creerRegions() {
        RegionManager rm = regions();
        if (rm == null) return;
        for (String id : List.copyOf(rm.getRegions().keySet())) {
            if (id.startsWith(PREFIXE_REGION)) rm.removeRegion(id);
        }
        for (int c = 0; c < colonnes(); c++) {
            for (int r = 0; r < boitesParColonne(); r++) {
                int[] o = coinBoite(c, r, gz);
                int x0 = o[0] + zx, y0 = o[1] + zy, z0 = o[2] + zz;
                ProtectedCuboidRegion region = new ProtectedCuboidRegion(nomRegion(c, r),
                        BlockVector3.at(x0, y0, z0), BlockVector3.at(x0 + zsx - 1, y0 + zsy - 1, z0 + zsz - 1));
                region.setPriority(10);
                rm.addRegion(region);
            }
        }
        enregistrer(rm);
    }

    static String nomRegion(int colonne, int rang) {
        return PREFIXE_REGION + (colonne + 1) + "_" + (rang + 1);
    }

    private void enregistrer(RegionManager rm) {
        try {
            rm.saveChanges();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "WorldGuard : enregistrement des régions différé", e);
        }
    }

    private Travaux travaux() {
        return plugin.travaux();
    }

    /** Message au joueur s'il est encore connecté (ou à la console). */
    private static void dire(CommandSender qui, String message) {
        if (qui instanceof Player p) {
            Player enLigne = Bukkit.getPlayer(p.getUniqueId());
            if (enLigne != null) enLigne.sendMessage(message);
        } else {
            qui.sendMessage(message);
        }
    }

    void info(CommandSender qui) {
        qui.sendMessage("§6Build Battle §7- monde §f" + plugin.monde().getName());
        qui.sendMessage("§7Boîte : " + (boiteCapturee() ? "§acapturée §7(" + boite.sx + "x" + boite.sy + "x" + boite.sz
                + ", zone " + zsx + "x" + zsy + "x" + zsz + ")" : "§cnon capturée") + "§7 - arène : "
                + (boitesCollees() ? "§agénérée §7(" + colonnes() + " x " + boitesParColonne() + ")" : "§cnon générée"));
        qui.sendMessage("§7Salle d'attente : " + (salleCapturee() ? "§acapturée §7(" + salle.sx + "x" + salle.sy + "x"
                + salle.sz + ")" : "§cnon capturée"));
        for (String cle : POSITIONS) qui.sendMessage("§7  " + cle + " : §f" + texte(position(cle)));
    }
}
