package fr.segame.armesia.mobs;

import fr.segame.armesia.Main;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;

public class MobSpawner {

    private final MobManager mobManager;

    public MobSpawner(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void spawnMob(Location loc, MobData data, String zoneId) {

        // ── Garde anti-eau : ne jamais spawner dans/sur de l'eau ─────────────
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

        // ── Villageois : vitesse réduite (défaut Minecraft = 0.5, trop rapide) ──
        if (mob instanceof Villager) {
            var speedAttr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(0.25);
        }

        mob.setRemoveWhenFarAway(false);   // pas de despawn naturel Minecraft pendant la session

        // Métadonnée légère (session uniquement)
        mob.setMetadata("customMob", new FixedMetadataValue(Main.getInstance(), true));


        mobManager.addInstance(new MobInstance(mob.getUniqueId(), data.getId(), zoneId));
    }
}