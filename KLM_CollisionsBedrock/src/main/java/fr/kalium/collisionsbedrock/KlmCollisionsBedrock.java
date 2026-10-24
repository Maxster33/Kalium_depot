package fr.kalium.collisionsbedrock;

import io.papermc.paper.event.player.PlayerFailMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * KLM_CollisionsBedrock 1.0.0 (demande de LeKiwi06, 24/10/2026) : « les joueurs geyser de facon general, le bamboo,
 * dripstone, etc. leur donne des rollback ».
 *
 * Cause (code de Paper 26.3 et jar de Geyser 2.12.0 lus le 24/10/2026) : en Java, le bambou et la stalactite sont
 * decales au hasard selon leur position, boite de collision comprise. Geyser corrige la position des joueurs Bedrock
 * avec une forme fixe par bloc, sans ce decalage. Le joueur Bedrock se retrouve donc, pour le serveur, dans le bloc ;
 * Paper le renvoie a sa position precedente : sans rien ecrire au journal quand il entre dans un bloc qu'il ne
 * touchait pas (« CLIPPED_INTO_BLOCK »), avec « moved wrongly! » quand le serveur, bloque par le bloc, ne retrouve pas
 * la position du joueur (« MOVED_WRONGLY »).
 *
 * Paper annonce chacun de ces renvois par PlayerFailMoveEvent, qui permet d'accepter le deplacement. Pour un joueur
 * Bedrock seulement (Floodgate : UUID dont la moitie haute vaut 0), on l'accepte si le seul bloc en cause est dans la
 * liste « blocs » du config.yml. Les joueurs Java ne sont jamais touches. GrimAC exempte deja les joueurs Bedrock :
 * ce plugin ne contourne aucun anti-triche.
 *
 * Detail du controle de Paper (ServerGamePacketListenerImpl.handleMovePlayer, 26.3) : MOVED_WRONGLY d'abord (ecart
 * entre la position calculee par le serveur et celle du joueur) ; puis, une fois le joueur place a sa nouvelle position,
 * CLIPPED_INTO_BLOCK, annonce aussi quand MOVED_WRONGLY a ete refuse. On ne l'accepte donc pas dans ce cas-la.
 */
public final class KlmCollisionsBedrock extends JavaPlugin implements Listener {

    /** Bloc accepte par defaut (aussi sans config.yml, REGLES.md section 3). */
    private static final List<String> BLOCS_PAR_DEFAUT = List.of("BAMBOO", "POINTED_DRIPSTONE");
    /** Marge de vanilla : une boite qui touche juste la surface d'un bloc ne le heurte pas. */
    private static final double EPSILON = 1.0E-5;

    private final Set<Material> blocs = EnumSet.noneOf(Material.class);
    private double deplacementMax = 1.0;
    private boolean journal = true;
    private long intervalleJournal = 5000;

    /** Tick du dernier MOVED_WRONGLY refuse (par nous ou un autre plugin), par joueur. */
    private final Map<UUID, Integer> refusDuTick = new HashMap<>();
    /** Derniere ligne de journal, par joueur (une ligne toutes les « intervalle-journal » millisecondes au plus). */
    private final Map<UUID, Long> dernierJournal = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        List<String> noms = getConfig().contains("blocs") ? getConfig().getStringList("blocs") : BLOCS_PAR_DEFAUT;
        for (String nom : noms) {
            Material m = Material.matchMaterial(nom);
            if (m == null || !m.isBlock()) {
                getLogger().warning("Bloc inconnu dans « blocs » : " + nom);
            } else {
                blocs.add(m);
            }
        }
        deplacementMax = getConfig().getDouble("deplacement-max", 1.0);
        journal = getConfig().getBoolean("journal-des-renvois", true);
        intervalleJournal = getConfig().getLong("intervalle-journal", 5000);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Blocs acceptes pour les joueurs Bedrock : " + blocs);
    }

    static boolean bedrock(Player player) {
        return player.getUniqueId().getMostSignificantBits() == 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFailMove(PlayerFailMoveEvent event) {
        Player player = event.getPlayer();
        if (!bedrock(player) || event.isAllowed()) {
            return;
        }
        PlayerFailMoveEvent.FailReason raison = event.getFailReason();
        if (raison != PlayerFailMoveEvent.FailReason.MOVED_WRONGLY
                && raison != PlayerFailMoveEvent.FailReason.CLIPPED_INTO_BLOCK) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.distance(to) > deplacementMax) {
            return;
        }
        BoundingBox avant = boite(player, from);
        BoundingBox apres = boite(player, to);
        if (raison == PlayerFailMoveEvent.FailReason.MOVED_WRONGLY) {
            // Le serveur, bloque par le bloc, n'a pas retrouve la position du joueur : on accepte si un bloc de la
            // liste est sur le trajet (deplacement d'un joueur qui marche, court ou saute : voir deplacementMax).
            if (!blocsTouches(from.getWorld(), avant.clone().union(apres), true).isEmpty()) {
                event.setAllowed(true);
                event.setLogWarning(false);
            }
            return;
        }
        // CLIPPED_INTO_BLOCK : annonce aussi apres un MOVED_WRONGLY refuse, qu'on ne contourne pas.
        Integer tick = refusDuTick.get(player.getUniqueId());
        if (tick != null && tick == Bukkit.getCurrentTick()) {
            return;
        }
        // Blocs que le joueur heurte a sa nouvelle position : tous doivent etre de la liste. (Paper ne regarde que les
        // blocs nouvellement heurtes, mais « from » est la derniere position annoncee aux plugins, pas forcement celle
        // du debut de ce deplacement : on ne s'en sert pas pour trier.)
        Set<Material> touches = blocsTouches(from.getWorld(), apres, false);
        if (!touches.isEmpty() && blocs.containsAll(touches)) {
            event.setAllowed(true);
        } else if (!touches.isEmpty()) {
            noter(player, "entre dans " + noms(touches), to);
        }
    }

    /** Apres tous les plugins : retient un MOVED_WRONGLY refuse (voir onFailMove) et le note au journal. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFailMoveFinal(PlayerFailMoveEvent event) {
        Player player = event.getPlayer();
        if (!bedrock(player) || event.isAllowed()
                || event.getFailReason() != PlayerFailMoveEvent.FailReason.MOVED_WRONGLY) {
            return;
        }
        refusDuTick.put(player.getUniqueId(), Bukkit.getCurrentTick());
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != null && from.getWorld().equals(to.getWorld())) {
            BoundingBox trajet = boite(player, from).union(boite(player, to));
            noter(player, "mouvement refusé près de " + noms(blocsTouches(from.getWorld(), trajet, false)), to);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        refusDuTick.remove(event.getPlayer().getUniqueId());
        dernierJournal.remove(event.getPlayer().getUniqueId());
    }

    /** Boite du joueur (taille actuelle : debout, accroupi, a la nage) placee a la position donnee. */
    private static BoundingBox boite(Player player, Location ou) {
        Location ici = player.getLocation();
        return player.getBoundingBox().clone().shift(ou.getX() - ici.getX(), ou.getY() - ici.getY(), ou.getZ() - ici.getZ());
    }

    /** Types des blocs dont la forme de collision heurte la boite ; avec listeSeule, seulement ceux de « blocs ». */
    private Set<Material> blocsTouches(World world, BoundingBox boite, boolean listeSeule) {
        Set<Material> types = EnumSet.noneOf(Material.class);
        BoundingBox b = boite.clone().expand(-EPSILON);
        for (int x = (int) Math.floor(b.getMinX()); x <= (int) Math.floor(b.getMaxX()); x++) {
            for (int y = (int) Math.floor(b.getMinY()) - 1; y <= (int) Math.floor(b.getMaxY()); y++) {
                for (int z = (int) Math.floor(b.getMinZ()); z <= (int) Math.floor(b.getMaxZ()); z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isPassable() || (listeSeule && !blocs.contains(block.getType()))) {
                        continue;
                    }
                    for (BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
                        BoundingBox monde = part.clone().shift(x, y, z);
                        if (monde.overlaps(b)) {
                            types.add(block.getType());
                            break;
                        }
                    }
                }
            }
        }
        return types;
    }

    /** Une ligne au journal par joueur toutes les « intervalle-journal » ms : sert a trouver d'autres blocs en cause. */
    private void noter(Player player, String quoi, Location ou) {
        if (!journal) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        Long dernier = dernierJournal.get(player.getUniqueId());
        if (dernier != null && maintenant - dernier < intervalleJournal) {
            return;
        }
        dernierJournal.put(player.getUniqueId(), maintenant);
        getLogger().info("Renvoi Bedrock : " + player.getName() + " " + quoi + " (" + ou.getWorld().getName() + " "
                + ou.getBlockX() + " " + ou.getBlockY() + " " + ou.getBlockZ() + ")");
    }

    private static String noms(Set<Material> types) {
        if (types.isEmpty()) {
            return "aucun bloc";
        }
        StringJoiner j = new StringJoiner(", ");
        for (Material m : types) {
            j.add(m.name());
        }
        return j.toString();
    }
}
