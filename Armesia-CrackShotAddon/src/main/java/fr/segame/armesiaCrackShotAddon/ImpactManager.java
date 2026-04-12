package fr.segame.armesiaCrackShotAddon;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

public class ImpactManager {

    public void trackProjectile(Projectile projectile, ConfigurationSection sec) {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (projectile.isDead() || !projectile.isValid()) {

                    Location loc = projectile.getLocation();
                    spawnImpact(loc, sec);

                    cancel();
                    return;
                }

            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void spawnImpact(Location loc, ConfigurationSection sec) {

        String shape = sec.getString("Shape", "SPHERE");

        double radius = sec.getDouble("Radius", 2);
        int points = sec.getInt("Points", 30);

        String[] rgb = sec.getString("Color", "255-100-0").split("-");
        Color color = Color.fromRGB(
                Integer.parseInt(rgb[0]),
                Integer.parseInt(rgb[1]),
                Integer.parseInt(rgb[2])
        );

        for (int i = 0; i < points; i++) {

            double theta = 2 * Math.PI * Math.random();
            double phi = Math.acos(2 * Math.random() - 1);

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            loc.getWorld().spawnParticle(
                    Particle.REDSTONE,
                    loc.clone().add(x, y, z),
                    1,
                    new Particle.DustOptions(color, 1.5f)
            );
        }
    }
}