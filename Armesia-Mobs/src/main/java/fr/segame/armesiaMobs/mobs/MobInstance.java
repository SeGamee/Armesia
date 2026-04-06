package fr.segame.armesiaMobs.mobs;

import java.util.UUID;

public class MobInstance {

    private final UUID uuid;
    private final String mobId;
    private final String zoneId;

    public MobInstance(UUID uuid, String mobId, String zoneId) {
        this.uuid = uuid;
        this.mobId = mobId;
        this.zoneId = zoneId;
    }

    public UUID getUuid() { return uuid; }
    public String getMobId() { return mobId; }
    public String getZoneId() { return zoneId; }
}