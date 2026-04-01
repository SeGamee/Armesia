package fr.segame.armesia.mobs;

import org.bukkit.Location;

import java.util.UUID;

public class MobInstance {

    private final UUID uuid;
    private final String mobId;
    private final String zoneId;
    /** Dernière position connue — sert à détecter si l'entité est dans un chunk déchargé */
    private Location lastLoc;

    public MobInstance(UUID uuid, String mobId, String zoneId) {
        this.uuid = uuid;
        this.mobId = mobId;
        this.zoneId = zoneId;
    }

    public UUID getUuid() { return uuid; }
    public String getMobId() { return mobId; }
    public String getZoneId() { return zoneId; }

    public Location getLastLoc() { return lastLoc; }
    public void updateLoc(Location loc) { this.lastLoc = loc.clone(); }
}