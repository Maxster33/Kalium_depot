package fr.kalium.core.claims;

import java.util.UUID;

/** Donnees de claim d'un joueur : nombre total d'emplacements possedes et chunk principal ("base"). */
public final class PlayerClaims {

    private final UUID uuid;
    private int slotsTotal;
    /** Cle du chunk principal ("world:x:z"), ou null si aucun n'est defini. */
    private String home;

    public PlayerClaims(UUID uuid, int slotsTotal) {
        this.uuid = uuid;
        this.slotsTotal = Math.max(0, slotsTotal);
    }

    public UUID uuid() {
        return uuid;
    }

    public int slotsTotal() {
        return slotsTotal;
    }

    public void slotsTotal(int value) {
        slotsTotal = Math.max(0, value);
    }

    public String home() {
        return home;
    }

    public void home(String chunkKey) {
        home = chunkKey;
    }
}
