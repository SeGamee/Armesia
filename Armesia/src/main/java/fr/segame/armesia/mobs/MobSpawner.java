package fr.segame.armesia.mobs;

import fr.segame.armesia.Main;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;

public class MobSpawner {

    private final MobManager mobManager;

    public MobSpawner(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void spawnMob(Location loc, MobData data, String zoneId) {

        Zombie mob = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        mob.setCustomName(data.getName() + " §7[Niv." + data.getLevel() + "]");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(data.getHealth());
        mob.setHealth(data.getHealth());

        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setFireTicks(0);
        mob.setVisualFire(false);

        mob.setMetadata("customMob", new FixedMetadataValue(Main.getInstance(), true));

        mobManager.addInstance(new MobInstance(
                mob.getUniqueId(),
                data.getId(),
                zoneId
        ));
    }
}