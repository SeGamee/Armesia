package fr.segame.armesia.player;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;

    private int xp;
    private int level;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.xp = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}