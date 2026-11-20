package fr.kalium.alcools;

import fr.kalium.effets.EffetFinEvent;
import fr.kalium.effets.KSEffets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.3.0 - Shéjiǔ (cahier « Atouts, alcools, aliments », 3.2.3 ; alcool neutre + œil d'araignée fermenté) : grimper aux
 * murs comme à une échelle.
 *
 * Le client grimpe quand le bloc à ses pieds est dans le tag « climbable » (LivingEntity.onClimbable, client 26.3), quel
 * que soit son état. Tous les 2 ticks, le plugin envoie au buveur SEUL une liane sans aucune face (état qui n'existe
 * jamais dans le monde) dans les blocs d'air au niveau de ses pieds, juste dessous et juste dessus, qui touchent un
 * bloc solide sur un côté. Le monde ne change pas, les autres joueurs ne voient rien. Le pack de KaLium (visuels-objets/
 * pack.py 1.4.0) donne à cet état un modèle vide : sans le pack, le jeu l'afficherait comme une liane côté nord.
 *
 * Côté serveur, le joueur est en l'air mais il touche le mur : pour le jeu et pour l'anti-vol de KS_AntiCheat (même
 * règle), le mur est un appui, il n'est ni expulsé ni signalé (allow-flight inutile). Le serveur ne voit pas la liane :
 * sa distance de chute est remise à zéro tant qu'il est sur une liane envoyée (sinon, en redescendant un mur, il
 * prendrait des dégâts de chute en touchant le sol).
 *
 * Pas dans l'eau (seulement les blocs d'air). Nouveauté « KS_Alcools:shejiu » (déploiement en deux temps, KS_MondeTest) :
 * tant qu'elle n'est pas validée, le Shéjiǔ ne se brasse et ne fait grimper que dans le monde de test.
 */
final class Shejiu implements Listener {

    static final String EFFET_SHEJIU = "ks_alcools:shejiu";
    static final String CLE = "KS_Alcools:shejiu";
    private static final BlockFace[] COTES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final KSAlcools plugin;
    private final BlockData liane = Bukkit.createBlockData("minecraft:vine[up=false,north=false,east=false,south=false,west=false]");
    /** Lianes envoyées à chaque joueur (positions de blocs du monde où il se trouve). */
    private final Map<UUID, Set<Location>> envoyees = new HashMap<>();
    private int compteur;

    Shejiu(KSAlcools plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    void toutRetirer() {
        for (UUID id : List.copyOf(envoyees.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                retirer(p);
            }
        }
        envoyees.clear();
    }

    private void tick() {
        // Toutes les secondes, les lianes sont renvoyées (un chunk renvoyé par le serveur les aurait effacées).
        boolean renvoyer = ++compteur % 10 == 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!KSEffets.actif(p, EFFET_SHEJIU) || !PhaseTest.ouvert(CLE, p)) {
                if (envoyees.containsKey(p.getUniqueId())) {
                    retirer(p);
                }
                continue;
            }
            Set<Location> voulues = voulues(p);
            Set<Location> avant = envoyees.getOrDefault(p.getUniqueId(), Set.of());
            for (Location l : avant) {
                if (!voulues.contains(l) && l.getWorld() == p.getWorld()) {
                    p.sendBlockChange(l, l.getBlock().getBlockData());
                }
            }
            for (Location l : voulues) {
                if (renvoyer || !avant.contains(l)) {
                    p.sendBlockChange(l, liane);
                }
            }
            envoyees.put(p.getUniqueId(), voulues);
            if (voulues.contains(p.getLocation().getBlock().getLocation()) && p.getFallDistance() > 0) {
                p.setFallDistance(0);
            }
        }
    }

    /** Blocs d'air aux pieds du joueur, dessous et dessus, qui touchent un bloc solide sur un côté. */
    private static Set<Location> voulues(Player p) {
        Set<Location> out = new HashSet<>();
        if (p.isInsideVehicle() || p.isGliding() || p.isDead()) {
            return out;
        }
        Block pieds = p.getLocation().getBlock();
        for (int dy = -1; dy <= 1; dy++) {
            Block b = pieds.getRelative(0, dy, 0);
            if (b.getType().isAir() && contreUnMur(b)) {
                out.add(b.getLocation());
            }
        }
        return out;
    }

    private static boolean contreUnMur(Block b) {
        World w = b.getWorld();
        for (BlockFace f : COTES) {
            Block voisin = b.getRelative(f);
            if (!w.isChunkLoaded(voisin.getX() >> 4, voisin.getZ() >> 4)) {
                continue;
            }
            if (voisin.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    /** Lianes retirées : le joueur revoit les vrais blocs. */
    private void retirer(Player p) {
        Set<Location> avant = envoyees.remove(p.getUniqueId());
        if (avant == null) {
            return;
        }
        for (Location l : avant) {
            if (l.getWorld() == p.getWorld()) {
                p.sendBlockChange(l, l.getBlock().getBlockData());
            }
        }
    }

    // ------------------------------------------------------------------ événements

    @EventHandler
    public void onFin(EffetFinEvent event) {
        if (event.getId().equals(EFFET_SHEJIU)) {
            retirer(event.getJoueur());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        retirer(event.getPlayer());
    }

    @EventHandler
    public void onMonde(PlayerChangedWorldEvent event) {
        envoyees.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        envoyees.remove(event.getPlayer().getUniqueId());
    }

    /** Tant que la nouveauté n'est pas validée : pas de Shéjiǔ brassé hors du monde de test. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrassage(BrewEvent event) {
        if (PhaseTest.ouvert(CLE, event.getBlock().getWorld())) {
            return;
        }
        for (ItemStack resultat : event.getResults()) {
            if (Alcools.SHEJIU.equals(Alcools.id(resultat))) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
