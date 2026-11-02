package fr.kalium.courrier;

import fr.kalium.blocs.EcouteurBloc;
import fr.kalium.blocs.KSBlocs;
import fr.kalium.blocs.TypeBloc;
import fr.kalium.courrier.Donnees.Boite;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * La boîte aux lettres, bloc custom de KS_Blocs (choix de LeKiwi06 du 02/11/2026) :
 * - une seule par joueur, posée dans un de ses claims (claim qui n'est pas en vente) ; ce claim ne peut alors être ni
 *   supprimé ni vendu (KS_Claim 1.6.0, {@link GardeClaim}) ; dans le monde de test (pas de claims) : n'importe où ;
 * - clic droit du propriétaire : menu du courrier ; d'un autre joueur : le nom du propriétaire ;
 * - cassée : rendue en objet, le courrier reste gardé par le serveur ; pas de piston ;
 * - remise en place par l'encapsuleur : sans propriétaire, adoptée par le premier joueur sans boîte qui la clique
 *   (dans son claim).
 */
final class Boites implements Listener, EcouteurBloc {

    private final KSCourrier plugin;
    private final Lang lang;
    private final Donnees donnees;
    private TypeBloc type;

    Boites(KSCourrier plugin, Donnees donnees) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.donnees = donnees;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    /** Déclare le bloc à KS_Blocs ; faux si son apparence manque dans apparences.json (KS_Blocs). */
    boolean declarer(Objets objets) {
        try {
            type = new TypeBloc(Objets.ID_BOITE, Objets.APPARENCE_BOITE, org.bukkit.Material.NOTE_BLOCK)
                    .objet(objets.modeleBoite()).ecouteur(this);
            KSBlocs.declarer(type);
            return true;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Boite aux lettres desactivee : " + e.getMessage());
            type = null;
            return false;
        }
    }

    private static boolean estBoite(Block b) {
        TypeBloc t = KSBlocs.type(b);
        return t != null && Objets.ID_BOITE.equals(t.id());
    }

    // ------------------------------------------------------------------ claims

    /** Propriétaire du claim de ce chunk, ou null (pas de claim, SimpleClaimSystem absent). */
    private static UUID proprioClaim(Chunk chunk) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SimpleClaimSystem")) {
            return null;
        }
        fr.xyness.SCS.Types.Claim claim = scs().getClaimAtChunk(chunk);
        return claim == null ? null : claim.getUUID();
    }

    private static boolean enVente(Chunk chunk) {
        fr.xyness.SCS.Types.Claim claim = scs().getClaimAtChunk(chunk);
        return claim != null && claim.getSale();
    }

    private static fr.xyness.SCS.API.SimpleClaimSystemAPI scs() {
        fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.initialize(
                (fr.xyness.SCS.SimpleClaimSystem) Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        return fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.getAPI();
    }

    /** Pourquoi ce joueur ne peut pas avoir sa boîte ici, ou null. */
    private Component refusPose(Player p, Block b) {
        UUID compte = Poste.compte(p);
        Boite deja = donnees.boite(compte);
        if (deja != null && !deja.a(b)) {
            return t("pose.deja", "<red>Tu as déjà une boîte aux lettres (<x> <y> <z>) : une seule par joueur.", "x",
                    deja.x(), "y", deja.y(), "z", deja.z());
        }
        if (CompteTest.enTest(p)) {
            return null; // monde de test : aucun claim
        }
        if (!p.getUniqueId().equals(proprioClaim(b.getChunk()))) {
            return t("pose.claim", "<red>Pose ta boîte aux lettres dans un de tes claims.");
        }
        if (enVente(b.getChunk())) {
            return t("pose.vente", "<red>Ce claim est en vente : retire-le de la vente avant d'y poser ta boîte.");
        }
        return null;
    }

    // ------------------------------------------------------------------ pose, casse

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPose(BlockPlaceEvent event) {
        if (!Objets.ID_BOITE.equals(KSBlocs.id(event.getItemInHand()))) {
            return;
        }
        if (type == null) {
            event.setCancelled(true);
            return;
        }
        Component refus = refusPose(event.getPlayer(), event.getBlockPlaced());
        if (refus != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(refus);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPosee(BlockPlaceEvent event) {
        if (type == null || !Objets.ID_BOITE.equals(KSBlocs.id(event.getItemInHand()))) {
            return;
        }
        Player p = event.getPlayer();
        Block b = event.getBlockPlaced();
        donnees.boites.put(Poste.compte(p), new Boite(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ(), p.getName()));
        donnees.sauver();
        p.sendMessage(t("pose.ok", "<green>Boîte aux lettres posée : clic droit pour ton courrier."));
    }

    /** Cassée, explosée, retirée par l'encapsuleur... : le propriétaire n'a plus de boîte (son courrier reste). */
    @Override
    public void retire(Block bloc, String variante) {
        UUID proprio = donnees.proprio(bloc);
        if (proprio != null) {
            donnees.boites.remove(proprio);
            donnees.sauver();
            Player p = Poste.enLigne(proprio);
            if (p != null) {
                p.sendMessage(t("casse", "<yellow>Ta boîte aux lettres a été retirée : ton courrier t'attend jusqu'à ce "
                        + "que tu en poses une autre."));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(Boites::estBoite)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(Boites::estBoite)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ clic

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClic(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || b == null || type == null || !estBoite(b)) {
            return;
        }
        Player p = event.getPlayer();
        if (p.isSneaking() && event.getItem() != null && event.getItem().getType().isBlock()) {
            return; // accroupi avec un bloc : il se pose contre la boîte
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        UUID compte = Poste.compte(p);
        UUID proprio = donnees.proprio(b);
        if (proprio == null) {
            Component refus = refusPose(p, b);
            if (refus != null) {
                p.sendMessage(t("clic.sans-proprio", "<gray>Boîte aux lettres sans propriétaire."));
                return;
            }
            donnees.boites.put(compte, new Boite(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ(), p.getName()));
            donnees.sauver();
            p.sendMessage(t("clic.adoptee", "<green>Cette boîte aux lettres est maintenant la tienne."));
            proprio = compte;
        }
        if (!proprio.equals(compte)) {
            p.sendMessage(t("clic.autre", "<gray>Boîte aux lettres de <joueur>.", "joueur", donnees.boites.get(proprio).nom()));
            return;
        }
        plugin.menus().boite(p);
    }
}
