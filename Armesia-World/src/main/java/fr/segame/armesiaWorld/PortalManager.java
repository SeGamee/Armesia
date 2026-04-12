package fr.segame.armesiaWorld;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gère le CRUD et la persistance des portails (config.yml → section "portals").
 * Lance également une tâche visuelle qui génère des particules dans chaque portail.
 */
public class PortalManager {

    private final MainWorld plugin;
    private final Map<String, Portal> portals = new HashMap<>();
    private BukkitTask particleTask;
    private final Random rng = new Random();

    public PortalManager(MainWorld plugin) {
        this.plugin = plugin;
        loadPortals();
        startParticleTask();
    }

    // ─── API ──────────────────────────────────────────────────────────────────

    public void addPortal(Portal portal) {
        portals.put(portal.getName().toLowerCase(), portal);
        savePortals();
    }

    public boolean removePortal(String name) {
        if (portals.remove(name.toLowerCase()) == null) return false;
        savePortals();
        return true;
    }

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public Collection<Portal> getAllPortals() { return portals.values(); }

    public List<String> getPortalNames() { return new ArrayList<>(portals.keySet()); }

    // ─── Particules visuelles ─────────────────────────────────────────────────

    private void startParticleTask() {
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Portal p : portals.values()) {
                World w = Bukkit.getWorld(p.getWorldName());
                if (w == null || w.getPlayers().isEmpty()) continue;

                int sizeX = p.getMaxX() - p.getMinX() + 1;
                int sizeY = p.getMaxY() - p.getMinY() + 1;
                int sizeZ = p.getMaxZ() - p.getMinZ() + 1;

                // Nombre de particules proportionnel à la taille du portail (min 10, max 40)
                int count = Math.max(10, Math.min(40, sizeX * sizeY * sizeZ / 2));

                for (int i = 0; i < count; i++) {
                    double rx = p.getMinX() + rng.nextDouble() * sizeX;
                    double ry = p.getMinY() + rng.nextDouble() * sizeY;
                    double rz = p.getMinZ() + rng.nextDouble() * sizeZ;
                    w.spawnParticle(Particle.PORTAL, rx, ry, rz, 1, 0, 0, 0, 0.05);
                }
            }
        }, 5L, 10L); // toutes les 0.5 secondes
    }

    public void shutdown() {
        if (particleTask != null) particleTask.cancel();
    }

    // ─── Persistance ─────────────────────────────────────────────────────────

    private void savePortals() {
        plugin.getConfig().set("portals", null);
        for (Portal p : portals.values()) {
            String path = "portals." + p.getName();
            plugin.getConfig().set(path + ".world",      p.getWorldName());
            plugin.getConfig().set(path + ".x1",         p.getMinX());
            plugin.getConfig().set(path + ".y1",         p.getMinY());
            plugin.getConfig().set(path + ".z1",         p.getMinZ());
            plugin.getConfig().set(path + ".x2",         p.getMaxX());
            plugin.getConfig().set(path + ".y2",         p.getMaxY());
            plugin.getConfig().set(path + ".z2",         p.getMaxZ());
            plugin.getConfig().set(path + ".targetZone", p.getTargetZone());
        }
        plugin.saveConfig();
    }

    private void loadPortals() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("portals");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path       = "portals." + key;
            String world      = plugin.getConfig().getString(path + ".world", "world");
            String targetZone = plugin.getConfig().getString(path + ".targetZone", "");
            int x1 = plugin.getConfig().getInt(path + ".x1");
            int y1 = plugin.getConfig().getInt(path + ".y1");
            int z1 = plugin.getConfig().getInt(path + ".z1");
            int x2 = plugin.getConfig().getInt(path + ".x2");
            int y2 = plugin.getConfig().getInt(path + ".y2");
            int z2 = plugin.getConfig().getInt(path + ".z2");
            portals.put(key.toLowerCase(),
                    new Portal(key, world, x1, y1, z1, x2, y2, z2, targetZone));
        }
        plugin.getLogger().info("[PortalManager] " + portals.size() + " portail(s) chargé(s).");
    }
}

