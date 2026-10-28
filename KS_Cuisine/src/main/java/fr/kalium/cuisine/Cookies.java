package fr.kalium.cuisine;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import fr.kalium.effets.EffetFinEvent;
import fr.kalium.effets.KSEffets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cookies (cahier, partie 3.4) :
 * - à la baie sucrée : « Apprivoiseur » 3 min : les animaux craintifs (ocelot, chat, renard, lapin, poissons, têtard)
 *   ne fuient plus ce joueur. Leur fuite est un chemin calculé par le jeu : quand le joueur le plus proche d'un de ces
 *   animaux est apprivoiseur et que le chemin l'éloignerait de lui, le chemin est refusé.
 * - à la baie lumineuse : « Lumière » 3 min : un bloc de lumière de niveau 14 suit le joueur (dans l'air ou une source
 *   d'eau, à la hauteur de sa tête, sinon de ses pieds). Le niveau 14 est réservé à ce cookie : au chargement d'un chunk
 *   (et au démarrage), tout bloc de lumière de niveau 14 sans joueur lumineux à côté est retiré.
 */
final class Cookies implements Listener {

    static final String EFFET_APPRIVOISEUR = "ks_cuisine:apprivoiseur";
    static final String EFFET_LUMIERE = "ks_cuisine:lumiere";

    private static final int DUREE = 3 * 60;
    private static final int NIVEAU = 14;
    /** Distance jusqu'à laquelle les animaux craintifs fuient un joueur (le renard : 16). */
    private static final double FUITE = 16;

    private static final Set<EntityType> CRAINTIFS = Set.of(EntityType.OCELOT, EntityType.CAT, EntityType.FOX,
            EntityType.RABBIT, EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH,
            EntityType.TADPOLE);

    private final KSCuisine plugin;

    /** Joueurs apprivoiseurs (mis à jour chaque seconde). */
    private final Set<UUID> apprivoiseurs = new HashSet<>();
    /** Bloc de lumière posé pour chaque joueur lumineux. */
    private final Map<UUID, Block> lumieres = new HashMap<>();

    Cookies(KSCuisine plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::seconde, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::suivre, 4L, 4L);
        for (World monde : Bukkit.getWorlds()) {
            for (Chunk chunk : monde.getLoadedChunks()) {
                nettoyer(chunk);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManger(PlayerItemConsumeEvent event) {
        String id = KSCuisine.idObjet(event.getItem());
        if (KSCuisine.COOKIE_BAIE.equals(id)) {
            KSEffets.donner(event.getPlayer(), EFFET_APPRIVOISEUR, DUREE);
            apprivoiseurs.add(event.getPlayer().getUniqueId());
        } else if (KSCuisine.COOKIE_LUMIERE.equals(id)) {
            KSEffets.donner(event.getPlayer(), EFFET_LUMIERE, DUREE);
        }
    }

    private void seconde() {
        apprivoiseurs.clear();
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (KSEffets.actif(joueur, EFFET_APPRIVOISEUR)) {
                apprivoiseurs.add(joueur.getUniqueId());
            }
        }
    }

    // ------------------------------------------------------------------ apprivoiseur

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChemin(EntityPathfindEvent event) {
        Entity animal = event.getEntity();
        if (apprivoiseurs.isEmpty() || !CRAINTIFS.contains(animal.getType()) || event.getTargetEntity() != null) {
            return;
        }
        Player proche = null;
        double distance = FUITE * FUITE;
        for (Player joueur : animal.getWorld().getPlayers()) {
            if (joueur.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double d = joueur.getLocation().distanceSquared(animal.getLocation());
            if (d < distance) {
                distance = d;
                proche = joueur;
            }
        }
        if (proche != null && apprivoiseurs.contains(proche.getUniqueId())
                && event.getLoc().distanceSquared(proche.getLocation()) > distance + 4) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ lumière

    /** Tous les 4 ticks : le bloc de lumière suit chaque joueur lumineux. */
    private void suivre() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Block ancien = lumieres.get(joueur.getUniqueId());
            Block nouveau = null;
            if (joueur.getGameMode() != GameMode.SPECTATOR && KSEffets.actif(joueur, EFFET_LUMIERE)) {
                Location tete = joueur.getEyeLocation();
                for (Block b : new Block[]{tete.getBlock(), joueur.getLocation().getBlock()}) {
                    if (b.equals(ancien) && estLumiere(b) || place(b)) {
                        nouveau = b;
                        break;
                    }
                }
            }
            if (ancien != null && !ancien.equals(nouveau)) {
                eteindre(ancien);
            }
            if (nouveau == null) {
                lumieres.remove(joueur.getUniqueId());
            } else {
                if (!nouveau.equals(ancien)) {
                    nouveau.setBlockData(lumiere(nouveau.getType() == Material.WATER), false);
                }
                lumieres.put(joueur.getUniqueId(), nouveau);
            }
        }
    }

    /** Air ou source d'eau. */
    private static boolean place(Block b) {
        Material type = b.getType();
        return type == Material.AIR || type == Material.CAVE_AIR
                || (type == Material.WATER && b.getBlockData() instanceof Levelled l && l.getLevel() == 0);
    }

    private static boolean estLumiere(Block b) {
        return b.getType() == Material.LIGHT && b.getBlockData() instanceof Light l && l.getLevel() == NIVEAU;
    }

    private static BlockData lumiere(boolean eau) {
        Light light = (Light) Material.LIGHT.createBlockData();
        light.setLevel(NIVEAU);
        light.setWaterlogged(eau);
        return light;
    }

    private static void eteindre(Block b) {
        if (b.getBlockData() instanceof Light l && l.getLevel() == NIVEAU) {
            b.setType(l.isWaterlogged() ? Material.WATER : Material.AIR, false);
        }
    }

    @EventHandler
    public void onFin(EffetFinEvent event) {
        if (event.getId().equals(EFFET_LUMIERE)) {
            Block b = lumieres.remove(event.getJoueur().getUniqueId());
            if (b != null) {
                eteindre(b);
            }
        } else if (event.getId().equals(EFFET_APPRIVOISEUR)) {
            apprivoiseurs.remove(event.getJoueur().getUniqueId());
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        Block b = lumieres.remove(event.getPlayer().getUniqueId());
        if (b != null) {
            eteindre(b);
        }
        apprivoiseurs.remove(event.getPlayer().getUniqueId());
    }

    /** Blocs de lumière de niveau 14 restés dans un chunk (arrêt du serveur, plantage) : retirés. */
    @EventHandler
    public void onChunk(ChunkLoadEvent event) {
        nettoyer(event.getChunk());
    }

    private void nettoyer(Chunk chunk) {
        if (!chunk.contains(lumiere(false)) && !chunk.contains(lumiere(true))) {
            return;
        }
        World monde = chunk.getWorld();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = monde.getMinHeight(); y < monde.getMaxHeight(); y++) {
                    Block b = chunk.getBlock(x, y, z);
                    if (estLumiere(b) && !lumieres.containsValue(b)) {
                        eteindre(b);
                    }
                }
            }
        }
    }
}
