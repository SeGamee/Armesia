package fr.segame.armesiaWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SpawnManager {

    private final MainWorld plugin;
    private Location spawnLocation;

    public SpawnManager(MainWorld plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    // ─── API publique ─────────────────────────────────────────────────────────

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    public Location getSpawn() {
        return spawnLocation;
    }

    /**
     * Définit le spawn à partir de la position exacte du joueur.
     * Le X et Z sont centrés sur le bloc (+ 0.5) pour ne pas coller à un coin,
     * et le yaw/pitch sont conservés pour que la vue soit correcte.
     */
    public void setSpawn(Location loc) {
        this.spawnLocation = new Location(
                loc.getWorld(),
                Math.floor(loc.getX()) + 0.5,
                loc.getY(),
                Math.floor(loc.getZ()) + 0.5,
                loc.getYaw(),
                loc.getPitch()
        );
        saveSpawn();
    }

    /**
     * Téléporte le joueur au spawn enregistré.
     * Retourne false si aucun spawn n'a été défini.
     */
    public boolean teleportToSpawn(Player player) {
        if (spawnLocation == null) return false;
        player.teleport(spawnLocation);
        return true;
    }

    // ─── Persistance ─────────────────────────────────────────────────────────

    private void saveSpawn() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) return;
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("spawn.world",  spawnLocation.getWorld().getName());
        cfg.set("spawn.x",      spawnLocation.getX());
        cfg.set("spawn.y",      spawnLocation.getY());
        cfg.set("spawn.z",      spawnLocation.getZ());
        cfg.set("spawn.yaw",   (double) spawnLocation.getYaw());
        cfg.set("spawn.pitch", (double) spawnLocation.getPitch());
        plugin.saveConfig();
    }

    private void loadSpawn() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("spawn.world")) return;

        String worldName = cfg.getString("spawn.world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Spawn configuré dans le monde '" + worldName
                    + "' mais ce monde n'est pas chargé.");
            return;
        }

        double x     = cfg.getDouble("spawn.x");
        double y     = cfg.getDouble("spawn.y");
        double z     = cfg.getDouble("spawn.z");
        float  yaw   = (float) cfg.getDouble("spawn.yaw");
        float  pitch = (float) cfg.getDouble("spawn.pitch");
        spawnLocation = new Location(world, x, y, z, yaw, pitch);
        plugin.getLogger().info("Spawn chargé : " + worldName
                + " (" + String.format("%.1f", x) + ", " + String.format("%.1f", y)
                + ", " + String.format("%.1f", z) + ")");
    }
}

