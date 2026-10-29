package fr.kalium.textures;

import com.mojang.datafixers.util.Pair;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dans la connexion d'un joueur, juste avant le traitement des paquets : les objets envoyés au joueur reçoivent leur
 * marque de texture ({@link Regles#marquer}) ; un objet renvoyé en créatif la perd. Paquets couverts : inventaires et
 * menus (contenu, case, objet tenu à la souris, inventaire du joueur), équipement des entités (objet en main, armure),
 * données des entités (objet au sol, cadre, présentoir). Pas couverts : échanges des villageois, livre de recettes,
 * contenu d'un sac ou d'une shulker affiché dans l'infobulle (l'objet y garde son image d'aujourd'hui).
 *
 * Une erreur ne coupe jamais la connexion : le paquet part tel quel et l'erreur est écrite une fois dans le journal.
 */
final class Reecriture extends ChannelDuplexHandler {

    private static volatile boolean erreurSignalee;

    private final Logger journal;

    Reecriture(Logger journal) {
        this.journal = journal;
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
            if (msg instanceof ServerboundSetCreativeModeSlotPacket p) {
                ItemStack objet = Regles.demarquer(p.itemStack());
                if (objet != p.itemStack()) {
                    recu = new ServerboundSetCreativeModeSlotPacket(p.slotNum(), objet);
                }
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
            journal.log(Level.WARNING, "Erreur en ajoutant les textures (paquet envoye tel quel, signale une seule fois)", t);
        }
    }

    /** Le paquet avec les objets marqués, ou le paquet lui-même s'il n'y a rien à changer. */
    @SuppressWarnings("unchecked")
    private static Packet<?> paquet(Packet<?> paquet) {
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
        if (paquet instanceof ClientboundContainerSetContentPacket p) {
            List<ItemStack> objets = liste(p.items());
            ItemStack tenu = Regles.marquer(p.carriedItem());
            return objets == p.items() && tenu == p.carriedItem() ? p
                    : new ClientboundContainerSetContentPacket(p.containerId(), p.stateId(), objets, tenu);
        }
        if (paquet instanceof ClientboundContainerSetSlotPacket p) {
            ItemStack objet = Regles.marquer(p.getItem());
            return objet == p.getItem() ? p
                    : new ClientboundContainerSetSlotPacket(p.getContainerId(), p.getStateId(), p.getSlot(), objet);
        }
        if (paquet instanceof ClientboundSetCursorItemPacket p) {
            ItemStack objet = Regles.marquer(p.contents());
            return objet == p.contents() ? p : new ClientboundSetCursorItemPacket(objet);
        }
        if (paquet instanceof ClientboundSetPlayerInventoryPacket p) {
            ItemStack objet = Regles.marquer(p.contents());
            return objet == p.contents() ? p : new ClientboundSetPlayerInventoryPacket(p.slot(), objet);
        }
        if (paquet instanceof ClientboundSetEquipmentPacket p) {
            List<Pair<EquipmentSlot, ItemStack>> cases = new ArrayList<>();
            boolean change = false;
            for (Pair<EquipmentSlot, ItemStack> c : p.getSlots()) {
                ItemStack objet = Regles.marquer(c.getSecond());
                change |= objet != c.getSecond();
                cases.add(Pair.of(c.getFirst(), objet));
            }
            return change ? new ClientboundSetEquipmentPacket(p.getEntity(), cases) : p;
        }
        if (paquet instanceof ClientboundSetEntityDataPacket p) {
            List<SynchedEntityData.DataValue<?>> valeurs = new ArrayList<>();
            boolean change = false;
            for (SynchedEntityData.DataValue<?> v : p.packedItems()) {
                if (v.serializer() == EntityDataSerializers.ITEM_STACK) {
                    ItemStack objet = (ItemStack) v.value();
                    ItemStack marque = Regles.marquer(objet);
                    if (marque != objet) {
                        change = true;
                        valeurs.add(new SynchedEntityData.DataValue<>(v.id(), EntityDataSerializers.ITEM_STACK, marque));
                        continue;
                    }
                }
                valeurs.add(v);
            }
            return change ? new ClientboundSetEntityDataPacket(p.id(), valeurs) : p;
        }
        return paquet;
    }

    /** La liste avec les objets marqués, ou la liste elle-même si aucun ne change. */
    private static List<ItemStack> liste(List<ItemStack> objets) {
        List<ItemStack> resultat = null;
        for (int i = 0; i < objets.size(); i++) {
            ItemStack objet = objets.get(i);
            ItemStack marque = Regles.marquer(objet);
            if (marque != objet && resultat == null) {
                resultat = new ArrayList<>(objets);
            }
            if (resultat != null) {
                resultat.set(i, marque);
            }
        }
        return resultat == null ? objets : resultat;
    }
}
