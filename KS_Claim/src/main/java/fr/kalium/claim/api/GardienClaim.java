package fr.kalium.claim.api;

import org.bukkit.Chunk;

import java.util.UUID;

/**
 * KS_Claim 1.6.0 : un plugin qui interdit de supprimer, vendre ou acheter un claim (ex. KS_Courrier : le claim qui porte
 * la boîte aux lettres de son propriétaire). Service Bukkit, enregistré par ce plugin ; KS_Claim les consulte tous.
 */
public interface GardienClaim {

    /** Pourquoi ce claim (son propriétaire, son chunk) ne peut pas changer de main ou disparaître, ou null. */
    String refus(UUID proprio, Chunk chunk);
}
