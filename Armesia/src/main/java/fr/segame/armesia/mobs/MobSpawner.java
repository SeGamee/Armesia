package fr.segame.armesia.mobs;

import fr.segame.armesia.Main;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

public class MobSpawner {

    /** Clés PDC — persistées dans le NBT de l'entité, survivent aux redémarrages */
    public static final String KEY_MOB_ID  = "armesia_mob_id";
    public static final String KEY_ZONE_ID = "armesia_zone_id";

    private final MobManager    mobManager;
    private final NamespacedKey keyMobId;
    private final NamespacedKey keyZoneId;

    public MobSpawner(MobManager mobManager) {
        this.mobManager = mobManager;
        this.keyMobId   = new NamespacedKey(Main.getInstance(), KEY_MOB_ID);
        this.keyZoneId  = new NamespacedKey(Main.getInstance(), KEY_ZONE_ID);
    }

    public void spawnMob(Location loc, MobData data, String zoneId) {

        Entity entity = loc.getWorld().spawnEntity(loc, data.getEntityType());

        if (!(entity instanceof LivingEntity mob)) {
            entity.remove();
            return;
        }

        mob.setCustomName(data.getName() + " §7[Niv." + data.getLevel() + "]");
        mob.setCustomNameVisible(true);

        var maxHealthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(data.getHealth());
            mob.setHealth(data.getHealth());
        }

        mob.setPersistent(true);           // sauvegardé dans le monde → survit aux déchargements de chunks
        mob.setRemoveWhenFarAway(false);   // pas de despawn naturel Minecraft

        // Métadonnée légère (session uniquement)
        mob.setMetadata("customMob", new FixedMetadataValue(Main.getInstance(), true));

        // Tags PDC : persistés dans le NBT → permettent la ré-identification après redémarrage
        mob.getPersistentDataContainer().set(keyMobId,  PersistentDataType.STRING, data.getId());
        mob.getPersistentDataContainer().set(keyZoneId, PersistentDataType.STRING, zoneId);

        mobManager.addInstance(new MobInstance(mob.getUniqueId(), data.getId(), zoneId));
    }
}