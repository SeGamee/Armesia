package fr.segame.armesia.zones;

import fr.segame.armesia.mobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class ZoneManager {

    private final Map<String, ZoneData> zones = new HashMap<>();
    private final MobSpawner spawner;
    private final MobManager mobManager;
    private final Random random = new Random();
    private final JavaPlugin plugin;

    public ZoneManager(JavaPlugin plugin, MobSpawner spawner, MobManager mobManager) {
        this.plugin = plugin;
        this.spawner = spawner;
        this.mobManager = mobManager;

        startTasks();
    }

    public void registerZone(ZoneData zone) {
        zones.put(zone.getId(), zone);
    }

    public ZoneData getZone(String id) {
        return zones.get(id);
    }

    private int countMobsAroundPlayer(Player player, ZoneData zone, double radius) {

        int count = 0;

        for (MobInstance instance : mobManager.getAllInstances()) {

            Entity entity = Bukkit.getEntity(instance.getUuid());
            if (entity == null) continue;

            if (!instance.getZoneId().equals(zone.getId())) continue;

            if (entity.getLocation().distance(player.getLocation()) <= radius) {
                count++;
            }
        }

        return count;
    }

    private Location getSmartSpawnLocation(Player player, ZoneData zone) {

        for (int i = 0; i < 10; i++) {

            double radius = 20 + random.nextDouble() * 20; // 20-40 blocs
            double angle = random.nextDouble() * Math.PI * 2;

            double x = player.getLocation().getX() + Math.cos(angle) * radius;
            double z = player.getLocation().getZ() + Math.sin(angle) * radius;

            Location loc = new Location(player.getWorld(), x, player.getLocation().getY(), z);

            // 🔥 ajuste au sol
            loc = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);

            // ❌ hors zone
            if (!isInZone(loc, zone)) continue;

            // ❌ eau
            if (loc.getBlock().isLiquid()) continue;

            // ❌ dans le champ de vision
            if (isLookingAt(player, loc)) continue;

            return loc;
        }

        return null;
    }

    private boolean isLookingAt(Player player, Location loc) {

        Vector direction = player.getLocation().getDirection().normalize();
        Vector toLoc = loc.toVector().subtract(player.getLocation().toVector()).normalize();

        double dot = direction.dot(toLoc);

        return dot > 0.85;
    }

    // 🔁 TICK GLOBAL
    private void startTasks() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            Iterator<MobInstance> iterator = mobManager.getAllInstances().iterator();

            while (iterator.hasNext()) {

                MobInstance instance = iterator.next();

                Entity entity = Bukkit.getEntity(instance.getUuid());

                if (!(entity instanceof Mob mob)) {
                    iterator.remove();
                    continue;
                }

                double closestDistance = Double.MAX_VALUE;

                for (var player : Bukkit.getOnlinePlayers()) {

                    double distance = player.getLocation().distance(mob.getLocation());

                    if (distance < closestDistance) {
                        closestDistance = distance;
                    }
                }

                double chance;

                if (closestDistance < 80) {
                    chance = 0.0;
                } else if (closestDistance < 120) {
                    chance = 0.2;
                } else if (closestDistance < 150) {
                    chance = 0.4;
                } else {
                    chance = 0.7;
                }

                // 🔥 roll random
                if (Math.random() < chance) {
                    mob.remove();
                    iterator.remove();
                }

            }

        }, 0L, 300L); // 🔥 300 ticks = 15 secondes

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (ZoneData zone : zones.values()) {

                for (var player : Bukkit.getOnlinePlayers()) {

                    if (!isInZone(player.getLocation(), zone)) continue;

                    int nearby = countMobsAroundPlayer(player, zone, 40);

                    int target = 3 + random.nextInt(3); // 🔥 3 à 5 mobs autour

                    if (nearby >= target) continue;

                    // 🔥 timing aléatoire
                    if (random.nextDouble() > 0.6) continue;

                    Location loc = getSmartSpawnLocation(player, zone);
                    if (loc == null) continue;

                    String mobId = zone.getMobs().get(random.nextInt(zone.getMobs().size()));
                    MobData data = mobManager.getMob(mobId);
                    if (data == null) continue;

                    spawner.spawnMob(loc, data, zone.getId());
                }

            }

        }, 0L, 300L); // check toutes les 15 secondes
    }

    // 🎯 SPAWN RANDOM
    private Location getRandomLocation(ZoneData zone) {

        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());

        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());

        double x = minX + random.nextDouble() * (maxX - minX);
        double z = minZ + random.nextDouble() * (maxZ - minZ);

        return new Location(zone.getPos1().getWorld(), x, zone.getPos1().getY(), z);
    }

    // 📦 CHECK ZONE
    public boolean isInZone(Location loc, ZoneData zone) {

        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());

        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    // 📍 CENTRE ZONE
    public Location getCenter(ZoneData zone) {

        double x = (zone.getPos1().getX() + zone.getPos2().getX()) / 2;
        double z = (zone.getPos1().getZ() + zone.getPos2().getZ()) / 2;

        return new Location(zone.getPos1().getWorld(), x, zone.getPos1().getY(), z);
    }
}