package fr.segame.armesiaMobs.mobs;

import fr.segame.armesiaMobs.ArmesiaMobs;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        mob.setCanPickupItems(false);

        // ── Nom + vie ───────────────────────────────────────────────────────
        applyMobName(mob, data);

        // ── Tags d'identification (session + persistant après redémarrage) ──
        mob.setMetadata("customMob", new FixedMetadataValue(ArmesiaMobs.getInstance(), true));

        // PDC — persiste dans les fichiers de monde (survit aux redémarrages)
        mob.getPersistentDataContainer().set(
                ArmesiaMobs.MOB_ID_KEY, PersistentDataType.STRING, data.getId());
        mob.getPersistentDataContainer().set(
                ArmesiaMobs.ZONE_ID_KEY, PersistentDataType.STRING, zoneId);

        mobManager.addInstance(new MobInstance(mob.getUniqueId(), data.getId(), zoneId));
    }

    // ── Applique le nom + indicateur de vie (utilisé au spawn et après dégâts) ──
    public static void applyMobName(LivingEntity mob, MobData data) {
        applyMobName(mob, data, (int) Math.ceil(mob.getHealth()));
    }

    public static void applyMobName(LivingEntity mob, MobData data, int currentHealth) {
        String translatedName = org.bukkit.ChatColor.translateAlternateColorCodes('&', data.getName());
        String fullName = translatedName + " §f| " + currentHealth + " §c❤";
        mob.customName(LegacyComponentSerializer.legacySection().deserialize(fullName));
        mob.setCustomNameVisible(true);
    }
}