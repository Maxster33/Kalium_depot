package fr.kalium.kvplots;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Protection par WorldGuard : une région par plot (propriétaire = créateur, membres = éditeurs), et la région globale
 * du monde en « passthrough deny » (personne ne construit hors des plots dont il est membre). FAWE, réglé pour
 * respecter WorldGuard, limite ainsi ses commandes aux plots du joueur.
 */
final class Regions {

    static final String PREFIXE = "kv_plot_";

    private final KVPlots plugin;

    Regions(KVPlots plugin) {
        this.plugin = plugin;
    }

    private RegionManager gestionnaire(World monde) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(monde));
    }

    /** Région globale : hors des plots, seuls les opérateurs (contournement WorldGuard) peuvent construire. */
    void protegerMonde(World monde) {
        RegionManager rm = gestionnaire(monde);
        if (rm == null) {
            plugin.getLogger().severe("WorldGuard ne gère pas le monde " + monde.getName() + " : aucune protection !");
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

    /** Crée ou met à jour la région du plot (limites et membres). */
    void appliquer(Plot p) {
        World monde = plugin.monde();
        RegionManager rm = gestionnaire(monde);
        if (rm == null) return;
        Grille g = plugin.grille();
        int minX = g.minX(p.colonne), minZ = g.minZ(p.ligne), cote = g.cote(p.taille);
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(PREFIXE + p.id,
                BlockVector3.at(minX, monde.getMinHeight(), minZ),
                BlockVector3.at(minX + cote - 1, monde.getMaxHeight() - 1, minZ + cote - 1));
        region.setPriority(10);
        // Plot validé = figé : ni propriétaire ni membre, personne n'y construit (FAWE compris), sauf les opérateurs.
        DefaultDomain proprietaires = new DefaultDomain();
        DefaultDomain membres = new DefaultDomain();
        if (p.etat != Plot.Etat.VALIDE) {
            proprietaires.addPlayer(p.createur);
            for (UUID u : p.editeurs) membres.addPlayer(u);
        }
        region.setOwners(proprietaires);
        region.setMembers(membres);
        rm.addRegion(region); // remplace la région de même nom
        enregistrer(rm);
    }

    void supprimer(Plot p) {
        RegionManager rm = gestionnaire(plugin.monde());
        if (rm == null) return;
        rm.removeRegion(PREFIXE + p.id);
        enregistrer(rm);
    }

    private void enregistrer(RegionManager rm) {
        try {
            rm.saveChanges();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "WorldGuard : enregistrement des régions différé", e);
        }
    }
}
