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

    // ──────────────────────────────────────────────────────
    //  ENTRY POINTS
    // ──────────────────────────────────────────────────────

    /** Démarre le trail d'un projectile (section CustomParticles du weapon config) */
    public void startProjectileTrail(Projectile projectile, ConfigurationSection sec) {
        if (sec == null || !sec.getBoolean("Enabled", false)) return;

        boolean followProjectile = sec.getBoolean("Follow_Projectile", false);
        boolean animated        = sec.getBoolean("Animated", false);

        if (followProjectile) {
            runFollowProjectileTrail(projectile, sec);
        } else if (animated) {
            Location start = projectile.getLocation();
            Vector   dir   = safeDirection(projectile);
            animateTrail(start, dir, sec);
        } else {
            Location start = projectile.getLocation();
            Vector   dir   = safeDirection(projectile);
            spawnFullTrail(start, dir, sec);
        }
    }

    /** Trail manuel depuis une position (Follow_Player, etc.) */
    public void startEnergyTrail(Player player, Location start, Vector direction, ConfigurationSection sec) {
        if (sec == null || !sec.getBoolean("Enabled", false)) return;

        if (sec.getBoolean("Follow_Player", false)) {
            runFollowPlayerTrail(player, sec);
        } else if (sec.getBoolean("Animated", false)) {
            animateTrail(start, direction, sec);
        } else {
            spawnFullTrail(start, direction, sec);
        }
    }

    /** Spawn les particules d'impact (section Impact: du weapon config) */
    public void spawnImpact(Location loc, ConfigurationSection sec) {
        if (sec == null || !sec.getBoolean("Enabled", false)) return;

        Particle particle        = parseParticle(sec.getString("Trail", "REDSTONE"));
        Color    color           = parseColor(sec.getString("Color", "255-255-255"));
        double   radius          = sec.getDouble("Radius", 2.0);
        int      points          = sec.getInt("Points", 30);
        String   shape           = sec.getString("Shape", "SPHERE");
        boolean  passThroughWalls = sec.getBoolean("PassThrough_Walls", true);

        // Direction fictive (non utilisée par SPHERE/LINE)
        Vector fakeDir = new Vector(1, 0, 0);
        spawnShapeAt(loc, fakeDir, shape, particle, color, radius, points, 0, passThroughWalls);
    }

    // ──────────────────────────────────────────────────────
    //  FOLLOW PROJECTILE — tick par tick
    // ──────────────────────────────────────────────────────

    private void runFollowProjectileTrail(Projectile projectile, ConfigurationSection sec) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isDead() || !projectile.isValid()) {
                    cancel();
                    return;
                }
                Vector vel = projectile.getVelocity();
                if (vel.lengthSquared() < 0.0001) return;

                Location loc = projectile.getLocation();
                // Direction inverse = trail va DERRIÈRE le projectile
                Vector dir = vel.normalize().multiply(-1);
                spawnFullTrail(loc, dir, sec);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // ──────────────────────────────────────────────────────
    //  FOLLOW PLAYER — tick par tick
    // ──────────────────────────────────────────────────────

    private void runFollowPlayerTrail(Player player, ConfigurationSection sec) {
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
                Vector   dir = loc.getDirection().normalize();
                spawnFullTrail(loc, dir, sec);
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // ──────────────────────────────────────────────────────
    //  TRAIL STATIQUE COMPLET
    // ──────────────────────────────────────────────────────

    private void spawnFullTrail(Location start, Vector direction, ConfigurationSection sec) {
        Particle particle        = parseParticle(sec.getString("Trail", "REDSTONE"));
        Color    color           = parseColor(sec.getString("Trail_Color", "255-255-255"));
        double   spacing         = sec.getDouble("Space_Between_Trails", 0.2);
        double   maxDistance     = sec.getDouble("Distance", 30);
        double   radius          = sec.getDouble("Radius", 1.5);
        int      points          = sec.getInt("Points", 20);
        String   shape           = sec.getString("Shape", "LINE");
        boolean  passThroughWalls = sec.getBoolean("PassThrough_Walls", true);

        for (double d = 0; d <= maxDistance; d += spacing) {
            Location loc = start.clone().add(direction.clone().multiply(d));
            // Mur détecté sur l'axe central → on stoppe le trail ici
            if (!passThroughWalls && !loc.getBlock().isPassable()) break;
            spawnShapeAt(loc, direction, shape, particle, color, radius, points, d, passThroughWalls);
        }
    }

    // ──────────────────────────────────────────────────────
    //  TRAIL ANIMÉ — avance progressivement
    // ──────────────────────────────────────────────────────

    private void animateTrail(Location start, Vector direction, ConfigurationSection sec) {
        Particle particle        = parseParticle(sec.getString("Trail", "REDSTONE"));
        Color    color           = parseColor(sec.getString("Trail_Color", "255-255-255"));
        double   spacing         = sec.getDouble("Space_Between_Trails", 0.5);
        double   maxDistance     = sec.getDouble("Distance", 40);
        double   radius          = sec.getDouble("Radius", 1.5);
        int      points          = sec.getInt("Points", 20);
        String   shape           = sec.getString("Shape", "LINE");
        boolean  passThroughWalls = sec.getBoolean("PassThrough_Walls", true);

        new BukkitRunnable() {
            double d = 0;
            @Override
            public void run() {
                if (d > maxDistance) {
                    cancel();
                    return;
                }
                Location loc = start.clone().add(direction.clone().multiply(d));
                // Mur détecté → on stoppe l'animation
                if (!passThroughWalls && !loc.getBlock().isPassable()) {
                    cancel();
                    return;
                }
                spawnShapeAt(loc, direction, shape, particle, color, radius, points, d, passThroughWalls);
                d += spacing;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // ──────────────────────────────────────────────────────
    //  SPAWN D'UNE FORME À UN POINT
    // ──────────────────────────────────────────────────────

    private void spawnShapeAt(Location loc, Vector direction, String shape,
                               Particle particle, Color color,
                               double radius, int points, double d,
                               boolean passThroughWalls) {

        // Axes perpendiculaires à la direction (pour CYLINDER / SPIRAL)
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = direction.clone().crossProduct(right).normalize();

        switch (shape.toUpperCase()) {

            case "LINE":
                spawnParticle(loc, particle, color, passThroughWalls);
                break;

            case "CYLINDER":
                for (int i = 0; i < points; i++) {
                    double angle  = 2 * Math.PI * i / points;
                    Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                                         .add(up.clone().multiply(Math.sin(angle) * radius));
                    spawnParticle(loc.clone().add(offset), particle, color, passThroughWalls);
                }
                break;

            case "SPHERE":
                for (int i = 0; i < points; i++) {
                    double theta = 2 * Math.PI * Math.random();
                    double phi   = Math.acos(2 * Math.random() - 1);
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);
                    spawnParticle(loc.clone().add(x, y, z), particle, color, passThroughWalls);
                }
                break;

            case "SPIRAL":
                double angle  = d * 10;
                Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                                     .add(up.clone().multiply(Math.sin(angle) * radius));
                spawnParticle(loc.clone().add(offset), particle, color, passThroughWalls);
                break;

            default:
                spawnParticle(loc, particle, color, passThroughWalls);
                break;
        }
    }

    // ──────────────────────────────────────────────────────
    //  UTILITAIRES
    // ──────────────────────────────────────────────────────

    private void spawnParticle(Location loc, Particle particle, Color color, boolean passThroughWalls) {
        if (!passThroughWalls && !loc.getBlock().isPassable()) return;
        if (particle == Particle.REDSTONE) {
            Color c = (color != null) ? color : Color.WHITE;
            loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1,
                    new Particle.DustOptions(c, 1.2f));
        } else {
            loc.getWorld().spawnParticle(particle, loc, 1);
        }
    }

    private Particle parseParticle(String name) {
        if (name == null) return Particle.REDSTONE;
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Particle.REDSTONE;
        }
    }

    private Color parseColor(String raw) {
        if (raw == null) return Color.WHITE;
        try {
            String[] rgb = raw.split("-");
            return Color.fromRGB(
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim())
            );
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private Vector safeDirection(Projectile projectile) {
        Vector vel = projectile.getVelocity();
        if (vel.lengthSquared() > 0.0001) return vel.normalize();
        return projectile.getLocation().getDirection().normalize();
    }
}