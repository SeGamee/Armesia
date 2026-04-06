package fr.segame.armesiaMobs.config;

import fr.segame.armesiaMobs.zones.SpawnCondition;
import fr.segame.armesiaMobs.zones.ZoneData;
import fr.segame.armesiaMobs.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ZoneConfig {

    private final JavaPlugin plugin;
    private final ZoneManager zoneManager;
    private final File file;
    private FileConfiguration config;

    public ZoneConfig(JavaPlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
        this.file = new File(plugin.getDataFolder(), "zones.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    // ─── Chargement ───────────────────────────────────────────────────────────

    public void load() {
        reload();
        ConfigurationSection root = config.getConfigurationSection("zones");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            String path = "zones." + id;

            String worldName = config.getString(path + ".world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[ZoneConfig] Monde introuvable '" + worldName + "' pour la zone '" + id + "'.");
                continue;
            }

            double p1x = config.getDouble(path + ".pos1.x");
            double p1y = config.getDouble(path + ".pos1.y");
            double p1z = config.getDouble(path + ".pos1.z");
            double p2x = config.getDouble(path + ".pos2.x");
            double p2y = config.getDouble(path + ".pos2.y");
            double p2z = config.getDouble(path + ".pos2.z");

            List<String> mobs = config.getStringList(path + ".mobs");
            int max            = config.getInt(path + ".max", 5);
            int priority       = config.getInt(path + ".priority", 0);
            boolean inherit    = config.getBoolean(path + ".inherit-mobs", false);
            boolean override   = config.getBoolean(path + ".override-mobs", false);

            double spawnMin   = config.getDouble(path + ".spawn-radius-min", 20);
            double spawnMax   = config.getDouble(path + ".spawn-radius-max", 40);
            int tMin          = config.getInt(path + ".target-min", 4);
            int tMax          = config.getInt(path + ".target-max", 6);
            int spawnInterval = config.getInt(path + ".spawn-interval", 5);
            double spawnChance = config.getDouble(path + ".spawn-chance", 1.0);
            SpawnCondition condition = SpawnCondition.fromString(
                    config.getString(path + ".spawn-condition", "ALWAYS"));

            double spawnBoostRatio1       = config.getDouble(path + ".spawn-boost-ratio1",       0.25);
            double spawnBoostRatio2       = config.getDouble(path + ".spawn-boost-ratio2",       0.50);
            double spawnBoostRatio3       = config.getDouble(path + ".spawn-boost-ratio3",       0.75);
            double spawnBoostMultiplier1  = config.getDouble(path + ".spawn-boost-multiplier1",  3.0);
            double spawnBoostMultiplier2  = config.getDouble(path + ".spawn-boost-multiplier2",  2.0);
            double spawnBoostMultiplier3  = config.getDouble(path + ".spawn-boost-multiplier3",  1.5);
            int    spawnBoostCount1       = config.getInt(   path + ".spawn-boost-count1",       3);
            int    spawnBoostCount2       = config.getInt(   path + ".spawn-boost-count2",       2);
            int    spawnBoostCount3       = config.getInt(   path + ".spawn-boost-count3",       1);

            double despawn              = config.getDouble(path + ".despawn-distance", 150);
            int    despawnCheckInterval = config.getInt(path + ".despawn-check-interval", 5);
            double despawnClose         = config.getDouble(path + ".despawn-chance-close",  0.0);
            double despawnMid           = config.getDouble(path + ".despawn-chance-mid",    0.2);
            double despawnFar           = config.getDouble(path + ".despawn-chance-far",    0.4);
            double despawnOuter         = config.getDouble(path + ".despawn-chance-outer",  0.7);
            double despawnRatio1        = config.getDouble(path + ".despawn-ratio1", 0.50);
            double despawnRatio2        = config.getDouble(path + ".despawn-ratio2", 0.75);
            double despawnRatio3        = config.getDouble(path + ".despawn-ratio3", 1.00);

            double boundaryTolerance    = config.getDouble(path + ".boundary-tolerance", 0.0);
            double bounceStrength       = config.getDouble(path + ".bounce-strength", 0.5);

            ZoneData zone = new ZoneData(id,
                    new Location(world, p1x, p1y, p1z),
                    new Location(world, p2x, p2y, p2z),
                    mobs, max);

            // ── Poids de spawn ──
            org.bukkit.configuration.ConfigurationSection weightsSection =
                    config.getConfigurationSection(path + ".mob-weights");
            if (weightsSection != null) {
                for (String mobId : weightsSection.getKeys(false)) {
                    double w = weightsSection.getDouble(mobId, 1.0);
                    if (w > 0) zone.setMobWeight(mobId, w);
                }
            }

            zone.setPriority(priority);
            zone.setInheritMobs(inherit);
            zone.setOverrideMobs(override);
            zone.setSpawnRadiusMin(spawnMin);
            zone.setSpawnRadiusMax(spawnMax);
            zone.setTargetMin(tMin);
            zone.setTargetMax(tMax);
            zone.setSpawnInterval(spawnInterval);
            zone.setSpawnChance(spawnChance);
            zone.setSpawnCondition(condition);
            zone.setSpawnBoostRatio1(spawnBoostRatio1);
            zone.setSpawnBoostRatio2(spawnBoostRatio2);
            zone.setSpawnBoostRatio3(spawnBoostRatio3);
            zone.setSpawnBoostMultiplier1(spawnBoostMultiplier1);
            zone.setSpawnBoostMultiplier2(spawnBoostMultiplier2);
            zone.setSpawnBoostMultiplier3(spawnBoostMultiplier3);
            zone.setSpawnBoostCount1(spawnBoostCount1);
            zone.setSpawnBoostCount2(spawnBoostCount2);
            zone.setSpawnBoostCount3(spawnBoostCount3);
            zone.setDespawnDistance(despawn);
            zone.setDespawnCheckInterval(despawnCheckInterval);
            zone.setDespawnChanceClose(despawnClose);
            zone.setDespawnChanceMid(despawnMid);
            zone.setDespawnChanceFar(despawnFar);
            zone.setDespawnChanceOuter(despawnOuter);
            zone.setDespawnRatio1(despawnRatio1);
            zone.setDespawnRatio2(despawnRatio2);
            zone.setDespawnRatio3(despawnRatio3);
            zone.setBoundaryTolerance(boundaryTolerance);
            zone.setBounceStrength(bounceStrength);

            zoneManager.registerZone(zone);
        }


    }

    // ─── Sauvegarde ───────────────────────────────────────────────────────────

    public void save() {
        config.set("zones", null);
        for (ZoneData zone : zoneManager.getAllZones()) writeZone(zone);
        persist();
    }

    public void saveZone(ZoneData zone) { writeZone(zone); persist(); }

    public void deleteZone(String id) { config.set("zones." + id, null); persist(); }

    // ─── Interne ──────────────────────────────────────────────────────────────

    private void writeZone(ZoneData zone) {
        String path = "zones." + zone.getId();
        Location p1 = zone.getPos1(), p2 = zone.getPos2();

        config.set(path + ".world",            p1 != null && p1.getWorld() != null ? p1.getWorld().getName() : "world");
        if (p1 != null) { config.set(path + ".pos1.x", p1.getX()); config.set(path + ".pos1.y", p1.getY()); config.set(path + ".pos1.z", p1.getZ()); }
        if (p2 != null) { config.set(path + ".pos2.x", p2.getX()); config.set(path + ".pos2.y", p2.getY()); config.set(path + ".pos2.z", p2.getZ()); }

        config.set(path + ".mobs",              zone.getMobs());
        config.set(path + ".max",               zone.getMax());
        config.set(path + ".priority",          zone.getPriority());
        config.set(path + ".inherit-mobs",      zone.isInheritMobs());
        config.set(path + ".override-mobs",     zone.isOverrideMobs());
        // ── Spawn ──
        config.set(path + ".spawn-radius-min",  zone.getSpawnRadiusMin());
        config.set(path + ".spawn-radius-max",  zone.getSpawnRadiusMax());
        config.set(path + ".target-min",        zone.getTargetMin());
        config.set(path + ".target-max",        zone.getTargetMax());
        config.set(path + ".spawn-interval",    zone.getSpawnInterval());
        config.set(path + ".spawn-chance",      zone.getSpawnChance());
        config.set(path + ".spawn-condition",   zone.getSpawnCondition().name());
        // ── Boost spawn ──
        config.set(path + ".spawn-boost-ratio1",      zone.getSpawnBoostRatio1());
        config.set(path + ".spawn-boost-ratio2",      zone.getSpawnBoostRatio2());
        config.set(path + ".spawn-boost-ratio3",      zone.getSpawnBoostRatio3());
        config.set(path + ".spawn-boost-multiplier1", zone.getSpawnBoostMultiplier1());
        config.set(path + ".spawn-boost-multiplier2", zone.getSpawnBoostMultiplier2());
        config.set(path + ".spawn-boost-multiplier3", zone.getSpawnBoostMultiplier3());
        config.set(path + ".spawn-boost-count1",      zone.getSpawnBoostCount1());
        config.set(path + ".spawn-boost-count2",      zone.getSpawnBoostCount2());
        config.set(path + ".spawn-boost-count3",      zone.getSpawnBoostCount3());
        // ── Despawn ──
        config.set(path + ".despawn-distance",        zone.getDespawnDistance());
        config.set(path + ".despawn-check-interval",  zone.getDespawnCheckInterval());
        config.set(path + ".despawn-chance-close",    zone.getDespawnChanceClose());
        config.set(path + ".despawn-chance-mid",      zone.getDespawnChanceMid());
        config.set(path + ".despawn-chance-far",      zone.getDespawnChanceFar());
        config.set(path + ".despawn-chance-outer",    zone.getDespawnChanceOuter());
        config.set(path + ".despawn-ratio1",          zone.getDespawnRatio1());
        config.set(path + ".despawn-ratio2",          zone.getDespawnRatio2());
        config.set(path + ".despawn-ratio3",          zone.getDespawnRatio3());
        // ── Frontière ──
        config.set(path + ".boundary-tolerance",      zone.getBoundaryTolerance());
        config.set(path + ".bounce-strength",         zone.getBounceStrength());
        // ── Poids de spawn ──
        config.set(path + ".mob-weights", null); // reset avant réécriture
        Map<String, Double> weights = zone.getMobWeights();
        if (!weights.isEmpty()) {
            weights.forEach((mobId, w) -> config.set(path + ".mob-weights." + mobId, w));
        }
    }

    private void persist() {
        try { file.getParentFile().mkdirs(); config.save(file); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "[ZoneConfig] Erreur sauvegarde.", e); }
    }
}
