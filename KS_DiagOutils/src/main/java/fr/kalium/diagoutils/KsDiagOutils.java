package fr.kalium.diagoutils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * KS_DiagOutils 1.0.0 (temporaire, demande de LeKiwi06, 24/10/2026) : « peux-tu régler le problème qu'ont les joueurs
 * bedrock pour modifier un bloc (farmland, stripped log, etc.) » ; .Cybonath2 puis zak (Bedrock) : houe, pelle, hache
 * sans effet sur Kixster, bloc et solidite remis ; un joueur Java y arrive au meme endroit.
 *
 * La cause n'a pas ete trouvee en lisant le code (Geyser 2.12.0, Paper 26.3, nos plugins, SimpleClaimSystem). Ce plugin
 * ne change rien au jeu : il ecrit au journal, pour un joueur qui tient une houe, une hache ou une pelle,
 * - chaque geste du bras en visant un bloc (le jeu du joueur a agi) ;
 * - chaque clic droit sur un bloc que le serveur recoit (PlayerInteractEvent), et a quelle priorite il est refuse ;
 * - chaque changement de bloc par ce joueur (EntityChangeBlockEvent : en 26.3, labourer, ecorcer, faire un chemin
 *   passent par l'element « block_transformers » de l'outil et cet evenement), et a quelle priorite il est annule ;
 * - le bloc obtenu au tick suivant.
 * Geste sans clic recu : Geyser (proxy) n'a rien envoye. Clic recu mais annule : la priorite donne les plugins en cause.
 */
public final class KsDiagOutils extends JavaPlugin implements Listener {

    /** Dernier geste note, par joueur (un geste par seconde au plus : on tient souvent le clic). */
    private final Map<UUID, Long> dernierGeste = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Diagnostic des outils actif (houe, hache, pelle) : lignes « [outil] » dans ce journal.");
    }

    static boolean outil(ItemStack item) {
        if (item == null) {
            return false;
        }
        String n = item.getType().name();
        return n.endsWith("_HOE") || n.endsWith("_SHOVEL") || (n.endsWith("_AXE") && !n.endsWith("_PICKAXE"));
    }

    static String qui(Player p) {
        return p.getName() + (p.getUniqueId().getMostSignificantBits() == 0 ? " (Bedrock)" : " (Java)")
                + (p.isOp() ? " op" : "") + " " + p.getGameMode().name().toLowerCase();
    }

    static String ou(Block b) {
        return b.getType() + " " + b.getX() + " " + b.getY() + " " + b.getZ();
    }

    // ------------------------------------------------------------------ geste du bras

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGeste(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        if (!outil(p.getInventory().getItemInMainHand())) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        Long dernier = dernierGeste.get(p.getUniqueId());
        if (dernier != null && maintenant - dernier < 1000) {
            return;
        }
        dernierGeste.put(p.getUniqueId(), maintenant);
        Block vise = p.getTargetBlockExact(6);
        getLogger().info("[outil] geste " + qui(p) + " avec " + p.getInventory().getItemInMainHand().getType()
                + " en visant " + (vise == null ? "rien" : ou(vise)) + ", accroupi=" + p.isSneaking()
                + ", main gauche=" + p.getInventory().getItemInOffHand().getType());
    }

    // ------------------------------------------------------------------ clic droit recu

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClic(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !outil(event.getItem())) {
            return;
        }
        Player p = event.getPlayer();
        getLogger().info("[outil] clic reçu " + qui(p) + " avec " + event.getItem().getType() + " sur "
                + ou(event.getClickedBlock()) + " face " + event.getBlockFace() + " main " + event.getHand()
                + ", accroupi=" + p.isSneaking() + ", main gauche=" + p.getInventory().getItemInOffHand().getType()
                + ", déjà refusé=" + refuse(event) + (refuse(event)
                ? " (plugins LOWEST : " + pluginsDe(PlayerInteractEvent.getHandlerList(), EventPriority.LOWEST) + ")" : ""));
        etatClic.put(p.getUniqueId(), refuse(event));
        Block bloc = event.getClickedBlock();
        Material avant = bloc.getType();
        Bukkit.getScheduler().runTask(this, () -> getLogger().info("[outil] résultat pour " + p.getName() + " : "
                + avant + " -> " + bloc.getType() + " (" + bloc.getX() + " " + bloc.getY() + " " + bloc.getZ() + ")"));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClicLow(PlayerInteractEvent event) {
        verifierClic(event, EventPriority.LOW);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClicNormal(PlayerInteractEvent event) {
        verifierClic(event, EventPriority.NORMAL);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClicHigh(PlayerInteractEvent event) {
        verifierClic(event, EventPriority.HIGH);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClicHighest(PlayerInteractEvent event) {
        verifierClic(event, EventPriority.HIGHEST);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClicMonitor(PlayerInteractEvent event) {
        if (verifierClic(event, EventPriority.MONITOR)) {
            getLogger().info("[outil] clic final pour " + event.getPlayer().getName() + " : bloc "
                    + event.useInteractedBlock() + ", objet " + event.useItemInHand());
        }
    }

    private static boolean refuse(PlayerInteractEvent e) {
        return e.useItemInHand() == Event.Result.DENY || e.useInteractedBlock() == Event.Result.DENY;
    }

    /** Clic d'outil sur un bloc : note les plugins des priorites precedentes s'il vient d'etre refuse. */
    private final Map<UUID, Boolean> etatClic = new HashMap<>();

    private boolean verifierClic(PlayerInteractEvent event, EventPriority priorite) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !outil(event.getItem())) {
            return false;
        }
        UUID id = event.getPlayer().getUniqueId();
        boolean avant = etatClic.getOrDefault(id, false);
        boolean maintenant = refuse(event);
        if (maintenant && !avant) {
            getLogger().info("[outil] clic refusé avant " + priorite + " pour " + event.getPlayer().getName()
                    + " (plugins à la priorité précédente : " + plugins(PlayerInteractEvent.getHandlerList(), priorite) + ")");
        }
        etatClic.put(id, maintenant);
        return true;
    }

    // ------------------------------------------------------------------ changement de bloc par le joueur

    private final Map<UUID, Boolean> etatChange = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChange(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player p) {
            getLogger().info("[outil] changement de bloc par " + qui(p) + " : " + ou(event.getBlock()) + " -> "
                    + event.getTo() + ", déjà annulé=" + event.isCancelled() + (event.isCancelled()
                    ? " (plugins LOWEST : " + pluginsDe(EntityChangeBlockEvent.getHandlerList(), EventPriority.LOWEST) + ")" : ""));
            etatChange.put(p.getUniqueId(), event.isCancelled());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChangeLow(EntityChangeBlockEvent event) {
        verifierChange(event, EventPriority.LOW);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChangeNormal(EntityChangeBlockEvent event) {
        verifierChange(event, EventPriority.NORMAL);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChangeHigh(EntityChangeBlockEvent event) {
        verifierChange(event, EventPriority.HIGH);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChangeHighest(EntityChangeBlockEvent event) {
        verifierChange(event, EventPriority.HIGHEST);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeMonitor(EntityChangeBlockEvent event) {
        if (verifierChange(event, EventPriority.MONITOR)) {
            getLogger().info("[outil] changement final pour " + event.getEntity().getName() + " : annulé="
                    + event.isCancelled());
        }
    }

    private boolean verifierChange(EntityChangeBlockEvent event, EventPriority priorite) {
        if (!(event.getEntity() instanceof Player p)) {
            return false;
        }
        boolean avant = etatChange.getOrDefault(p.getUniqueId(), false);
        if (event.isCancelled() != avant) {
            getLogger().info("[outil] changement " + (event.isCancelled() ? "annulé" : "rétabli") + " avant "
                    + priorite + " pour " + p.getName() + " (plugins à la priorité précédente : "
                    + plugins(EntityChangeBlockEvent.getHandlerList(), priorite) + ")");
        }
        etatChange.put(p.getUniqueId(), event.isCancelled());
        return true;
    }

    /** Plugins (autres que celui-ci) inscrits a la priorite juste avant celle donnee. */
    private String plugins(HandlerList liste, EventPriority priorite) {
        EventPriority precedente = EventPriority.values()[priorite.ordinal() - 1];
        return precedente + " : " + pluginsDe(liste, precedente);
    }

    private String pluginsDe(HandlerList liste, EventPriority priorite) {
        StringJoiner j = new StringJoiner(", ");
        for (RegisteredListener l : liste.getRegisteredListeners()) {
            if (l.getPriority() == priorite && l.getPlugin() != this) {
                j.add(l.getPlugin().getName());
            }
        }
        return j.length() == 0 ? "aucun" : j.toString();
    }
}
