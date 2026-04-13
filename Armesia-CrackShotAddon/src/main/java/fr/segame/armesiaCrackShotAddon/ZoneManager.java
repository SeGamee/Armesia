package fr.segame.armesiaCrackShotAddon;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ZoneManager {

    public void createZone(Location center, ConfigurationSection sec) {

        double radius = sec.getDouble("Radius", 3);
        double heightUp = sec.getDouble("Height_Up", 1);
        double heightDown = sec.getDouble("Height_Down", 1);

        int duration = sec.getInt("Duration", 100);
        int tickRate = sec.getInt("Tick", 10);

        int surfacePoints = sec.getInt("Points_Surface", 30);
        int insidePoints = sec.getInt("Points_Inside", 0);

        Particle particle = Particle.valueOf(sec.getString("Particle", "REDSTONE"));

        String[] rgb = sec.getString("Color", "255-255-255").split("-");
        Color color = Color.fromRGB(
                Integer.parseInt(rgb[0]),
                Integer.parseInt(rgb[1]),
                Integer.parseInt(rgb[2])
        );

        List<String> effects = sec.getStringList("Effects");

        final Sound loopSound = parseSound(sec.getString("Sound_Loop"));
        String shape = sec.getString("Shape", "CYLINDER");

        World world = center.getWorld();

        new BukkitRunnable() {

            int time = 0;

            @Override
            public void run() {

                if (time >= duration) {
                    cancel();
                    return;
                }

                spawnParticles(center, radius, heightUp, heightDown,
                        surfacePoints, insidePoints,
                        particle, color, shape);

                for (Player p : world.getPlayers()) {

                    Location loc = p.getLocation().add(0, 1, 0);

                    double dx = loc.getX() - center.getX();
                    double dy = loc.getY() - center.getY();
                    double dz = loc.getZ() - center.getZ();

                    boolean inside = switch (shape.toUpperCase()) {

                        case "SPHERE" -> dx * dx + dy * dy + dz * dz <= radius * radius;

                        case "CUBE" -> Math.abs(dx) <= radius
                                && dy <= heightUp && dy >= -heightDown
                                && Math.abs(dz) <= radius;

                        default -> dx * dx + dz * dz <= radius * radius
                                && dy <= heightUp && dy >= -heightDown;
                    };

                    if (!inside) continue;

                    // 🔊 SOUND LOOP
                    if (loopSound != null) {
                        world.playSound(p.getLocation(), loopSound, 0.5f, 1f);
                    }

                    // 🧪 EFFECTS
                    for (String s : effects) {

                        try {
                            String[] parts = s.split(":");

                            PotionEffectType type = PotionEffectType.getByName(parts[0]);
                            int dur = Integer.parseInt(parts[1]);
                            int amp = Integer.parseInt(parts[2]);

                            if (type == null) continue;

                            applySmartEffect(p, type, dur, amp);

                        } catch (Exception ignored) {}
                    }
                }

                time += tickRate;
            }

        }.runTaskTimer(Main.getInstance(), 0L, tickRate);
    }

    // 🔥 PARTICULES (SURFACE + INSIDE)
    private void spawnParticles(Location center, double radius, double heightUp, double heightDown,
                                int surfacePoints, int insidePoints,
                                Particle particle, Color color, String shape) {

        World world = center.getWorld();

        // 🔹 SURFACE
        for (int i = 0; i < surfacePoints; i++) {

            double x, y, z;

            switch (shape.toUpperCase()) {

                case "SPHERE":
                    double theta = 2 * Math.PI * Math.random();
                    double phi = Math.acos(2 * Math.random() - 1);

                    x = radius * Math.sin(phi) * Math.cos(theta);
                    y = radius * Math.sin(phi) * Math.sin(theta);
                    z = radius * Math.cos(phi);
                    break;

                case "CUBE":
                    x = (Math.random() < 0.5 ? -radius : radius);
                    y = (Math.random() * (heightUp + heightDown)) - heightDown;
                    z = (Math.random() < 0.5 ? -radius : radius);
                    break;

                default: // CYLINDER
                    double angle = 2 * Math.PI * Math.random();

                    x = Math.cos(angle) * radius;
                    z = Math.sin(angle) * radius;
                    y = (Math.random() * (heightUp + heightDown)) - heightDown;
                    break;
            }

            spawnParticle(world, center.clone().add(x, y, z), particle, color);
        }

        // 🔸 INSIDE
        for (int i = 0; i < insidePoints; i++) {

            double x, y, z;

            switch (shape.toUpperCase()) {

                case "SPHERE":
                    double theta = 2 * Math.PI * Math.random();
                    double phi = Math.acos(2 * Math.random() - 1);

                    double r = Math.random() * radius;

                    x = r * Math.sin(phi) * Math.cos(theta);
                    y = r * Math.sin(phi) * Math.sin(theta);
                    z = r * Math.cos(phi);
                    break;

                case "CUBE":
                    x = (Math.random() * 2 - 1) * radius;
                    y = (Math.random() * (heightUp + heightDown)) - heightDown;
                    z = (Math.random() * 2 - 1) * radius;
                    break;

                default: // CYLINDER
                    double angle = 2 * Math.PI * Math.random();
                    double rCyl = Math.random() * radius;

                    x = Math.cos(angle) * rCyl;
                    z = Math.sin(angle) * rCyl;
                    y = (Math.random() * (heightUp + heightDown)) - heightDown;
                    break;
            }

            spawnParticle(world, center.clone().add(x, y, z), particle, color);
        }
    }

    // 🔥 SPAWN PARTICULE
    private void spawnParticle(World world, Location loc, Particle particle, Color color) {

        if (particle == Particle.REDSTONE) {
            world.spawnParticle(
                    Particle.REDSTONE,
                    loc,
                    1,
                    new Particle.DustOptions(color, 1.2f)
            );
        } else {
            world.spawnParticle(particle, loc, 1);
        }
    }

    // 🔥 EFFETS PROPRES
    private void applySmartEffect(Player p, PotionEffectType type, int duration, int amplifier) {

        PotionEffect current = p.getPotionEffect(type);

        boolean shouldApply = false;

        if (current == null) {
            shouldApply = true;
        } else if (current.getAmplifier() < amplifier) {
            shouldApply = true;
        } else if (current.getDuration() < 20) {
            shouldApply = true;
        }

        if (shouldApply) {
            p.addPotionEffect(new PotionEffect(type, duration, amplifier), true);
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