package fr.segame.armesiaCrackShotAddon;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TrailManager {

    // 🔥 ENTRY POINT
    public void startEnergyTrail(Player player, Location start, Vector direction, ConfigurationSection sec) {

        if (sec == null) return;

        boolean follow = sec.getBoolean("Follow_Player", false);

        if (follow) {
            startFollowingTrail(player, sec);
        } else {
            startStaticTrail(start, direction, sec);
        }
    }

    // 🔥 TRAIL QUI SUIT LE PROJECTILE
    public void startProjectileTrail(Projectile projectile, ConfigurationSection sec) {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (projectile.isDead() || !projectile.isValid()) {
                    cancel();
                    return;
                }

                Location loc = projectile.getLocation();
                Vector dir = projectile.getVelocity().normalize();

                startStaticTrail(loc, dir, sec);

            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // 🔥 TRAIL QUI SUIT LE JOUEUR
    private void startFollowingTrail(Player player, ConfigurationSection sec) {

        int duration = sec.getInt("Duration", 10);

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                if (ticks > duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                Vector dir = loc.getDirection().normalize();

                startStaticTrail(loc, dir, sec);

                ticks++;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // 🔥 TRAIL PRINCIPAL
    private void startStaticTrail(Location start, Vector direction, ConfigurationSection sec) {

        boolean animated = sec.getBoolean("Animated", false);

        if (animated) {
            animateTrail(start, direction, sec);
            return;
        }

        spawnFullTrail(start, direction, sec);
    }

    // 🔥 VERSION STATIQUE (TON SYSTÈME ACTUEL)
    private void spawnFullTrail(Location start, Vector direction, ConfigurationSection sec) {

        Particle particle = Particle.valueOf(sec.getString("Trail", "REDSTONE"));

        String[] rgb = sec.getString("Trail_Color", "255-255-255").split("-");
        Color color = Color.fromRGB(
                Integer.parseInt(rgb[0]),
                Integer.parseInt(rgb[1]),
                Integer.parseInt(rgb[2])
        );

        double spacing = sec.getDouble("Space_Between_Trails", 0.2);
        double maxDistance = sec.getDouble("Distance", 30);
        double radius = sec.getDouble("Radius", 1.5);

        int points = sec.getInt("Points", 20);
        String shape = sec.getString("Shape", "CYLINDER");

        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() == 0) right = new Vector(1, 0, 0);
        right.normalize();

        Vector up = direction.clone().crossProduct(right).normalize();

        for (double d = 0; d <= maxDistance; d += spacing) {

            Location loc = start.clone().add(direction.clone().multiply(d));

            switch (shape.toUpperCase()) {

                case "CYLINDER":

                    for (int i = 0; i < points; i++) {

                        double angle = 2 * Math.PI * i / points;

                        double x = Math.cos(angle) * radius;
                        double y = Math.sin(angle) * radius;

                        Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));

                        spawnParticle(loc.clone().add(offset), particle, color);
                    }
                    break;

                case "SPHERE":

                    for (int i = 0; i < points; i++) {

                        double theta = 2 * Math.PI * Math.random();
                        double phi = Math.acos(2 * Math.random() - 1);

                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.sin(phi) * Math.sin(theta);
                        double z = radius * Math.cos(phi);

                        spawnParticle(loc.clone().add(x, y, z), particle, color);
                    }
                    break;

                case "SPIRAL":

                    double angle = d * 10;

                    double x = Math.cos(angle) * radius;
                    double y = Math.sin(angle) * radius;

                    Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));

                    spawnParticle(loc.clone().add(offset), particle, color);
                    break;

                case "LINE":

                    spawnParticle(loc, particle, color);
                    break;
            }
        }
    }

    // 🔥 VERSION ANIMÉE
    private void animateTrail(Location start, Vector direction, ConfigurationSection sec) {

        Particle particle = Particle.valueOf(sec.getString("Trail", "REDSTONE"));

        String[] rgb = sec.getString("Trail_Color", "255-255-255").split("-");
        Color color = Color.fromRGB(
                Integer.parseInt(rgb[0]),
                Integer.parseInt(rgb[1]),
                Integer.parseInt(rgb[2])
        );

        double spacing = sec.getDouble("Space_Between_Trails", 0.2);
        double maxDistance = sec.getDouble("Distance", 30);

        new BukkitRunnable() {

            double d = 0;

            @Override
            public void run() {

                if (d > maxDistance) {
                    cancel();
                    return;
                }

                Location loc = start.clone().add(direction.clone().multiply(d));

                spawnParticle(loc, particle, color);

                d += spacing;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // 🔥 SPAWN PARTICLE
    private void spawnParticle(Location loc, Particle particle, Color color) {

        if (particle == Particle.REDSTONE && color != null) {
            loc.getWorld().spawnParticle(
                    Particle.REDSTONE,
                    loc,
                    1,
                    new Particle.DustOptions(color, 1.2f)
            );
        } else {
            loc.getWorld().spawnParticle(particle, loc, 1);
        }
    }
}