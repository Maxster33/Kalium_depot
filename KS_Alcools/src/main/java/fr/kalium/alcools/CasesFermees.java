package fr.kalium.alcools;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 1.2.0 (LeKiwi06, 30/10/2026 : « pour que ce soit plus visuel ») : dans l'écran d'un alambic, les cases vides de
 * gauche et de droite montrent une barrière « Case fermée ». Affichage seul : la barrière n'existe que dans ce qui est
 * envoyé au joueur (gestionnaire placé dans sa connexion, comme KS_Textures), l'alambic ne contient rien ; rien ne
 * peut donc être volé, tomber à la casse ni sortir par un entonnoir. Une fiole posée avant 1.2.0 sur un côté s'affiche
 * telle quelle (on peut la retirer).
 *
 * Un clic sur la barrière ne fait rien : le serveur refuse le clic ({@link Alambic}) et renvoie l'écran, la barrière
 * revient. En créatif, une barrière copiée depuis cet écran est effacée avant d'arriver au serveur.
 *
 * Compilé contre le serveur Paper 26.3 (paquets réseau) : à revérifier à chaque mise à jour de Minecraft. Une erreur
 * ne coupe jamais la connexion : le paquet part tel quel et l'erreur est écrite une fois dans le journal.
 */
final class CasesFermees extends ChannelDuplexHandler {

    private static final Key ECOUTEUR = Key.key("ks_alcools", "cases_fermees");
    private static final String NOM = "ks_alcools_cases_fermees";

    private static ItemStack barriere = ItemStack.EMPTY;
    private static volatile boolean erreurSignalee;

    private final Logger journal;
    /** Identifiant du menu d'alambic ouvert chez ce joueur, -1 si son menu ouvert n'est pas un alambic. */
    private volatile int alambic = -1;

    private CasesFermees(Logger journal) {
        this.journal = journal;
    }

    /** Place le gestionnaire dans la connexion de chaque joueur qui se connecte (redémarrage nécessaire). */
    static void installer(KSAlcools plugin) {
        org.bukkit.inventory.ItemStack objet = new org.bukkit.inventory.ItemStack(Material.BARRIER);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(Component.text("Case fermée", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Une fiole à la fois, dans la case du milieu", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "case_fermee"), PersistentDataType.BYTE,
                (byte) 1);
        objet.setItemMeta(meta);
        barriere = CraftItemStack.asNMSCopy(objet);

        Logger journal = plugin.getLogger();
        ChannelInitializeListenerHolder.addListener(ECOUTEUR, canal -> {
            if (canal.pipeline().get("packet_handler") != null) {
                canal.pipeline().addBefore("packet_handler", NOM, new CasesFermees(journal));
            } else {
                canal.pipeline().addLast(NOM, new CasesFermees(journal));
            }
        });
    }

    static void desinstaller() {
        ChannelInitializeListenerHolder.removeListener(ECOUTEUR);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Object envoye = msg;
        try {
            if (msg instanceof Packet<?> paquet) {
                envoye = paquet(paquet);
            }
        } catch (Throwable t) {
            signaler(t);
            envoye = msg;
        }
        super.write(ctx, envoye, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object recu = msg;
        try {
            if (msg instanceof ServerboundSetCreativeModeSlotPacket p
                    && ItemStack.isSameItemSameComponents(p.itemStack(), barriere)) {
                recu = new ServerboundSetCreativeModeSlotPacket(p.slotNum(), ItemStack.EMPTY);
            }
        } catch (Throwable t) {
            signaler(t);
            recu = msg;
        }
        super.channelRead(ctx, recu);
    }

    private void signaler(Throwable t) {
        if (!erreurSignalee) {
            erreurSignalee = true;
            journal.log(Level.WARNING, "Erreur en affichant les cases fermees de l'alambic (paquet envoye tel quel, "
                    + "signale une seule fois)", t);
        }
    }

    private static boolean cote(int caseMenu) {
        return caseMenu == 0 || caseMenu == 2;
    }

    /** Le paquet avec les barrières, ou le paquet lui-même s'il n'y a rien à changer. */
    @SuppressWarnings("unchecked")
    private Packet<?> paquet(Packet<?> paquet) {
        if (paquet instanceof ClientboundBundlePacket groupe) {
            List<Packet<? super ClientGamePacketListener>> sous = new ArrayList<>();
            boolean change = false;
            for (Packet<? super ClientGamePacketListener> p : groupe.subPackets()) {
                Packet<?> q = paquet(p);
                change |= q != p;
                sous.add((Packet<? super ClientGamePacketListener>) q);
            }
            return change ? new ClientboundBundlePacket(sous) : groupe;
        }
        if (paquet instanceof ClientboundOpenScreenPacket p) {
            alambic = p.getType() == MenuType.BREWING_STAND ? p.getContainerId() : -1;
            return p;
        }
        if (paquet instanceof ClientboundContainerSetContentPacket p && p.containerId() == alambic) {
            List<ItemStack> objets = null;
            for (int c = 0; c <= 2 && c < p.items().size(); c += 2) {
                if (p.items().get(c).isEmpty()) {
                    if (objets == null) {
                        objets = new ArrayList<>(p.items());
                    }
                    objets.set(c, barriere.copy());
                }
            }
            return objets == null ? p
                    : new ClientboundContainerSetContentPacket(p.containerId(), p.stateId(), objets, p.carriedItem());
        }
        if (paquet instanceof ClientboundContainerSetSlotPacket p && p.getContainerId() == alambic
                && cote(p.getSlot()) && p.getItem().isEmpty()) {
            return new ClientboundContainerSetSlotPacket(p.getContainerId(), p.getStateId(), p.getSlot(),
                    barriere.copy());
        }
        return paquet;
    }
}
