package fr.segame.armesiaMobs.mobs;

import fr.segame.armesiaMobs.ArmesiaMobs;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

public class MobSpawner {

    private final MobManager mobManager;

    public MobSpawner(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void spawnMob(Location loc, MobData data, String zoneId) {

        // ── Garde anti-eau ─────────────────────────────────────────────────
        if (loc.getBlock().isLiquid()
                || loc.clone().subtract(0, 1, 0).getBlock().isLiquid()) {
            return;
        }

        Entity entity = loc.getWorld().spawnEntity(loc, data.getEntityType());

        if (!(entity instanceof LivingEntity mob)) {
            entity.remove();
            return;
        }

        mob.setCustomName(data.getName());
        mob.setCustomNameVisible(true);

        var maxHealthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(data.getHealth());
            mob.setHealth(data.getHealth());
        }

        // ── Villageois : vitesse réduite ────────────────────────────────────
        if (mob instanceof Villager) {
            var speedAttr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(0.25);
        }

        mob.setRemoveWhenFarAway(false);

        // ── Tags d'identification (session + persistant après redémarrage) ──
        mob.setMetadata("customMob", new FixedMetadataValue(ArmesiaMobs.getInstance(), true));

        // PDC — persiste dans les fichiers de monde (survit aux redémarrages)
        mob.getPersistentDataContainer().set(
                ArmesiaMobs.MOB_ID_KEY, PersistentDataType.STRING, data.getId());
        mob.getPersistentDataContainer().set(
                ArmesiaMobs.ZONE_ID_KEY, PersistentDataType.STRING, zoneId);

        mobManager.addInstance(new MobInstance(mob.getUniqueId(), data.getId(), zoneId));
    }
}