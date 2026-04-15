package fr.segame.armesiaCrackShotAddon;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

public class ImpactManager {

    public void trackProjectile(Projectile projectile, ConfigurationSection weaponSection, Player shooter) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isDead() || !projectile.isValid()) {
                    Location loc = projectile.getLocation();
                    onProjectileImpact(loc, weaponSection, shooter);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void onProjectileImpact(Location impactLoc, ConfigurationSection weaponSection, Player shooter) {

        // 🔥 IMPACT PARTICLES
        ConfigurationSection impactSec = weaponSection.getConfigurationSection("Impact");
        if (impactSec != null && impactSec.getBoolean("Enabled", false)) {
            Main.getInstance().getTrailManager().spawnImpact(impactLoc, impactSec);
        }

        // 🔥 ZONES
        ConfigurationSection zones = weaponSection.getConfigurationSection("Zones");
        if (zones == null) return;

        for (String key : zones.getKeys(false)) {

            ConfigurationSection zone = zones.getConfigurationSection(key);
            if (zone == null || !zone.getBoolean("Enabled", false)) continue;

            String trigger = zone.getString("Trigger", "IMPACT");
            if (!trigger.equalsIgnoreCase("IMPACT")) continue;

            Location loc = ZoneUtils.resolveLocation(zone, shooter, impactLoc);

            Sound sound = parseSound(zone.getString("Sound"));
            if (sound != null) {
                loc.getWorld().playSound(loc, sound, 1f, 1f);
            }

            Main.getInstance().getZoneManager().createZone(loc, zone);
        }
    }

    private Sound parseSound(String name) {
        if (name == null) return null;
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}