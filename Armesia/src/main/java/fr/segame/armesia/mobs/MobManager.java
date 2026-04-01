package fr.segame.armesia.mobs;

import java.util.*;

public class MobManager {

    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<UUID, MobInstance> activeMobs = new HashMap<>();

    public void registerMob(MobData mob) {
        mobs.put(mob.getId(), mob);
    }

    public void removeMob(String id) {
        mobs.remove(id);
    }

    public MobData getMob(String id) {
        return mobs.get(id);
    }

    public Collection<MobData> getAllMobs() {
        return mobs.values();
    }

    public Set<String> getMobIds() {
        return mobs.keySet();
    }

    public void addInstance(MobInstance instance) {
        activeMobs.put(instance.getUuid(), instance);
    }

    public void removeInstance(UUID uuid) {
        activeMobs.remove(uuid);
    }

    public MobInstance getInstance(UUID uuid) {
        return activeMobs.get(uuid);
    }

    public Collection<MobInstance> getAllInstances() {
        return activeMobs.values();
    }
}