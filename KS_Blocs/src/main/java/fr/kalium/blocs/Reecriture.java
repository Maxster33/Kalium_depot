package fr.kalium.blocs;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dans la connexion d'un joueur, juste avant le traitement des paquets : les blocs envoyés au joueur prennent l'état
 * donné par {@link Affichage} (chunks, changement d'un bloc, changements groupés d'une section, particules de casse).
 * Le monde du joueur est suivi par les paquets d'arrivée et de changement de monde, qui passent ici dans l'ordre.
 *
 * Bloc musical : le son d'un vrai bloc musical vient du serveur (bonne note), mais le client fait la particule (et
 * Geyser le son Bedrock) à partir de l'état qu'il voit ; le paquet d'événement est remplacé par une particule de la
 * bonne couleur, le son du serveur reste.
 *
 * Une erreur ne coupe jamais la connexion : le paquet part tel quel et l'erreur est écrite une fois dans le journal.
 */
final class Reecriture extends ChannelDuplexHandler {

    private static volatile boolean erreurSignalee;
    private static PalettedContainerFactory fabrique;
    private static Constructor<ClientboundLevelChunkPacketData> constructeur;
    private static Field champBlocsEntites;

    private final Affichage affichage;
    private final Logger journal;
    private volatile ServerLevel monde;
    private volatile UUID idMonde;

    Reecriture(Affichage affichage, Logger journal) {
        this.affichage = affichage;
        this.journal = journal;
    }

    /** Prépare l'accès au paquet de chunk (au démarrage, sur le fil principal). */
    static void preparer() throws ReflectiveOperationException {
        fabrique = PalettedContainerFactory.create(MinecraftServer.getServer().registryAccess());
        constructeur = ClientboundLevelChunkPacketData.class.getDeclaredConstructor(Map.class, byte[].class, List.class);
        constructeur.setAccessible(true);
        champBlocsEntites = ClientboundLevelChunkPacketData.class.getDeclaredField("blockEntitiesData");
        champBlocsEntites.setAccessible(true);
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
        if (envoye == null) {
            promise.setSuccess(); // paquet supprimé
            return;
        }
        super.write(ctx, envoye, promise);
    }

    private void signaler(Throwable t) {
        if (!erreurSignalee) {
            erreurSignalee = true;
            journal.log(Level.WARNING, "Erreur en reecrivant les blocs (paquet envoye tel quel, signale une seule fois)", t);
        }
    }

    /** Le paquet réécrit, le paquet lui-même s'il n'y a rien à changer, ou null pour ne pas l'envoyer. */
    @SuppressWarnings("unchecked")
    private Packet<?> paquet(Packet<?> paquet) throws ReflectiveOperationException {
        if (paquet instanceof ClientboundBundlePacket groupe) {
            List<Packet<? super ClientGamePacketListener>> sous = new ArrayList<>();
            boolean change = false;
            for (Packet<? super ClientGamePacketListener> p : groupe.subPackets()) {
                Packet<?> q = paquet(p);
                change |= q != p;
                if (q != null) {
                    sous.add((Packet<? super ClientGamePacketListener>) q);
                }
            }
            return change ? new ClientboundBundlePacket(sous) : groupe;
        }
        if (paquet instanceof ClientboundLoginPacket p) {
            changerDeMonde(MinecraftServer.getServer().getLevel(p.commonPlayerSpawnInfo().dimension()));
            return p;
        }
        if (paquet instanceof ClientboundRespawnPacket p) {
            changerDeMonde(MinecraftServer.getServer().getLevel(p.commonPlayerSpawnInfo().dimension()));
            return p;
        }
        UUID id = idMonde;
        if (id == null) {
            return paquet;
        }
        if (paquet instanceof ClientboundLevelChunkWithLightPacket p) {
            return chunk(p, id);
        }
        if (paquet instanceof ClientboundBlockUpdatePacket p) {
            BlockPos pos = p.getPos();
            BlockState etat = affichage.affiche(id, pos.getX(), pos.getY(), pos.getZ(), p.getBlockState());
            return etat == p.getBlockState() ? p : new ClientboundBlockUpdatePacket(pos, etat);
        }
        if (paquet instanceof ClientboundSectionBlocksUpdatePacket p) {
            return section(p, id);
        }
        if (paquet instanceof ClientboundLevelEventPacket p && p.getType() == 2001) {
            // particules et son de casse d'un bloc : data = identifiant de l'état cassé
            BlockPos pos = p.getPos();
            BlockState serveur = Block.stateById(p.getData());
            BlockState etat = affichage.particules(id, pos.getX(), pos.getY(), pos.getZ(), serveur);
            return etat == serveur ? p : new ClientboundLevelEventPacket(2001, pos, Block.getId(etat), p.isGlobalEvent());
        }
        if (paquet instanceof ClientboundBlockEventPacket p && p.getBlock() == Blocks.NOTE_BLOCK
                && affichage.apparences().bases().contains(Blocks.NOTE_BLOCK)) {
            BlockPos pos = p.getPos();
            Integer note = affichage.note(id, pos.getX(), pos.getY(), pos.getZ());
            if (note == null) {
                return p;
            }
            if (note < 0) {
                return null; // instrument sans note (tête) : pas de particule, comme en vanilla
            }
            return new ClientboundLevelParticlesPacket(ParticleTypes.NOTE, false, false, pos.getX() + 0.5,
                    pos.getY() + 1.2, pos.getZ() + 0.5, note / 24.0f, 0f, 0f, 1f, 0);
        }
        return paquet;
    }

    private void changerDeMonde(ServerLevel nouveau) {
        monde = nouveau;
        idMonde = nouveau == null ? null : nouveau.getWorld().getUID();
    }

    private Packet<?> section(ClientboundSectionBlocksUpdatePacket p, UUID id) {
        Short2ObjectMap<BlockState> etats = new Short2ObjectOpenHashMap<>();
        SectionPos[] section = new SectionPos[1];
        boolean[] change = new boolean[1];
        p.runUpdates((pos, serveur) -> {
            BlockState etat = affichage.affiche(id, pos.getX(), pos.getY(), pos.getZ(), serveur);
            change[0] |= etat != serveur;
            if (section[0] == null) {
                section[0] = SectionPos.of(pos);
            }
            etats.put(SectionPos.sectionRelativePos(pos), etat);
        });
        return change[0] ? new ClientboundSectionBlocksUpdatePacket(section[0], etats) : p;
    }

    private Packet<?> chunk(ClientboundLevelChunkWithLightPacket p, UUID id) throws ReflectiveOperationException {
        ServerLevel level = monde;
        Set<Block> bases = affichage.apparences().bases();
        Map<Integer, Registre.Entree> customs = affichage.registreChunk(id, p.x(), p.z());
        boolean aucunCustom = customs == null || customs.isEmpty();
        if (level == null || (aucunCustom && (bases.isEmpty() || !peutContenir(level, p.x(), p.z(), bases)))) {
            return p;
        }
        FriendlyByteBuf lu = p.chunkData().getReadBuffer();
        List<LevelChunkSection> sections = new ArrayList<>();
        while (lu.isReadable()) {
            LevelChunkSection s = new LevelChunkSection(fabrique);
            s.read(lu);
            sections.add(s);
        }
        boolean change = false;
        if (!bases.isEmpty()) {
            for (LevelChunkSection s : sections) {
                if (!s.maybeHas(etat -> bases.contains(etat.getBlock()))) {
                    continue;
                }
                for (int i = 0; i < 4096; i++) {
                    int x = i & 15, z = (i >> 4) & 15, y = i >> 8;
                    BlockState etat = s.getBlockState(x, y, z);
                    BlockState vrai = affichage.vrai(etat);
                    if (vrai != etat) {
                        s.setBlockState(x, y, z, vrai, false);
                        change = true;
                    }
                }
            }
        }
        if (!aucunCustom) {
            int minSection = level.getMinSectionY();
            for (Map.Entry<Integer, Registre.Entree> e : customs.entrySet()) {
                int cle = e.getKey();
                int y = Registre.y(cle);
                int indice = (y >> 4) - minSection;
                if (indice < 0 || indice >= sections.size()) {
                    continue;
                }
                LevelChunkSection s = sections.get(indice);
                int lx = Registre.x(cle), lz = Registre.z(cle);
                BlockState serveur = s.getBlockState(lx, y & 15, lz);
                BlockState etat = affichage.custom(id, (p.x() << 4) + lx, y, (p.z() << 4) + lz, e.getValue(), serveur);
                if (etat != null && etat != serveur) {
                    s.setBlockState(lx, y & 15, lz, etat, false);
                    change = true;
                }
            }
        }
        if (!change) {
            return p;
        }
        FriendlyByteBuf ecrit = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (LevelChunkSection s : sections) {
                s.write(ecrit);
            }
            byte[] octets = new byte[ecrit.readableBytes()];
            ecrit.readBytes(octets);
            ClientboundLevelChunkPacketData donnees = constructeur.newInstance(p.chunkData().getHeightmaps(), octets,
                    champBlocsEntites.get(p.chunkData()));
            return new ClientboundLevelChunkWithLightPacket(p.x(), p.z(), donnees, p.lightData());
        } finally {
            ecrit.release();
        }
    }

    /**
     * Faux seulement si le chunk du serveur est chargé et qu'aucune de ses sections ne peut contenir un bloc de base :
     * évite de décoder la plupart des chunks. Dans le doute, vrai.
     */
    private static boolean peutContenir(ServerLevel level, int cx, int cz, Set<Block> bases) {
        try {
            LevelChunk c = level.getChunkSource().getChunkNow(cx, cz);
            if (c == null) {
                return true;
            }
            for (LevelChunkSection s : c.getSections()) {
                if (s != null && !s.hasOnlyAir() && s.maybeHas(etat -> bases.contains(etat.getBlock()))) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }
}
