package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final Main plugin;
    private File file;
    private FileConfiguration config;

    public HomeManager(Main plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        file = new File(plugin.getDataFolder(), "homes.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Limite ────────────────────────────────────────────────────────────────

    public int getHomeLimit(Player player) {
        var limitsSection = plugin.getConfig().getConfigurationSection("homes.limits");
        int limit = plugin.getConfig().getInt("homes.default-limit", 1);
        if (limitsSection != null) {
            for (String perm : limitsSection.getKeys(false)) {
                if (Main.hasGroupPermission(player, perm)) {
                    int val = limitsSection.getInt(perm);
                    if (val < 0) return Integer.MAX_VALUE;
                    limit = Math.max(limit, val);
                }
            }
        }
        return limit;
    }

    public int getHomeCount(UUID uuid) {
        var section = config.getConfigurationSection("players." + uuid);
        return section == null ? 0 : section.getKeys(false).size();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean hasHome(UUID uuid, String name) {
        return config.contains("players." + uuid + "." + name.toLowerCase());
    }

    /** @return true si le home existait déjà (mise à jour), false si nouveau */
    public boolean setHome(UUID uuid, String name, Location loc) {
        boolean existed = hasHome(uuid, name.toLowerCase());
        String path = "players." + uuid + "." + name.toLowerCase();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x",     loc.getX());
        config.set(path + ".y",     loc.getY());
        config.set(path + ".z",     loc.getZ());
        config.set(path + ".yaw",   (double) loc.getYaw());
        config.set(path + ".pitch", (double) loc.getPitch());
        save();
        return existed;
    }

    public boolean deleteHome(UUID uuid, String name) {
        if (!hasHome(uuid, name.toLowerCase())) return false;
        config.set("players." + uuid + "." + name.toLowerCase(), null);
        save();
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        String path = "players." + uuid + "." + name.toLowerCase();
        if (!config.contains(path)) return null;
        String worldName = config.getString(path + ".world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch"));
    }

    public List<String> getHomes(UUID uuid) {
        var section = config.getConfigurationSection("players." + uuid);
        return section == null ? Collections.emptyList() : new ArrayList<>(section.getKeys(false));
    }
}

