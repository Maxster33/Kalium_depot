package fr.kalium.core.stats;

import java.util.UUID;

/** Statistiques persistees d'un joueur sur ce serveur. */
public final class PlayerStats {

    private final UUID uuid;
    private long playtimeSeconds;
    private int levelsSpent;
    private long blocksBroken;
    private long blocksPlaced;
    private long monstersKilled;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public long playtimeSeconds() {
        return playtimeSeconds;
    }

    public void playtimeSeconds(long value) {
        playtimeSeconds = Math.max(0, value);
    }

    public void addPlaytime(long seconds) {
        if (seconds > 0) {
            playtimeSeconds += seconds;
        }
    }

    public int levelsSpent() {
        return levelsSpent;
    }

    public void levelsSpent(int value) {
        levelsSpent = Math.max(0, value);
    }

    public void addLevelsSpent(int amount) {
        if (amount > 0) {
            levelsSpent += amount;
        }
    }

    public long blocksBroken() {
        return blocksBroken;
    }

    public void blocksBroken(long value) {
        blocksBroken = Math.max(0, value);
    }

    public void incrementBlocksBroken() {
        blocksBroken++;
    }

    public long blocksPlaced() {
        return blocksPlaced;
    }

    public void blocksPlaced(long value) {
        blocksPlaced = Math.max(0, value);
    }

    public void incrementBlocksPlaced() {
        blocksPlaced++;
    }

    public long monstersKilled() {
        return monstersKilled;
    }

    public void monstersKilled(long value) {
        monstersKilled = Math.max(0, value);
    }

    public void incrementMonstersKilled() {
        monstersKilled++;
    }
}
