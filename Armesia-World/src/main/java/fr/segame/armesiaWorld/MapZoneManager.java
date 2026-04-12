package fr.segame.armesiaWorld;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gère le CRUD et la persistance (config.yml → section "mapzones") des zones de map.
 */
public class MapZoneManager {

    private final MainWorld plugin;
    private final Map<String, MapZone> zones = new HashMap<>();

    public MapZoneManager(MainWorld plugin) {
        this.plugin = plugin;
        loadZones();
    }

    // ─── API ──────────────────────────────────────────────────────────────────

    /** Ajoute (ou remplace) une zone et sauvegarde. */
    public void addZone(MapZone zone) {
        zones.put(zone.getName().toLowerCase(), zone);
        saveZones();
    }

    /** Supprime une zone. Retourne false si elle n'existait pas. */
    public boolean removeZone(String name) {
        if (zones.remove(name.toLowerCase()) == null) return false;
        saveZones();
        return true;
    }

    /** Récupère une zone par son nom (insensible à la casse). */
    public MapZone getZone(String name) {
        return zones.get(name.toLowerCase());
    }

    public Collection<MapZone> getAllZones() { return zones.values(); }

    public List<String> getZoneNames() { return new ArrayList<>(zones.keySet()); }

    // ─── Persistance ─────────────────────────────────────────────────────────

    private void saveZones() {
        plugin.getConfig().set("mapzones", null); // efface l'ancienne section
        for (MapZone z : zones.values()) {
            String p = "mapzones." + z.getName();
            plugin.getConfig().set(p + ".world", z.getWorldName());
            plugin.getConfig().set(p + ".x1",    z.getMinX());
            plugin.getConfig().set(p + ".y1",    z.getMinY());
            plugin.getConfig().set(p + ".z1",    z.getMinZ());
            plugin.getConfig().set(p + ".x2",    z.getMaxX());
            plugin.getConfig().set(p + ".y2",    z.getMaxY());
            plugin.getConfig().set(p + ".z2",    z.getMaxZ());
        }
        plugin.saveConfig();
    }

    private void loadZones() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mapzones");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String p     = "mapzones." + key;
            String world = plugin.getConfig().getString(p + ".world", "world");
            int x1 = plugin.getConfig().getInt(p + ".x1");
            int y1 = plugin.getConfig().getInt(p + ".y1");
            int z1 = plugin.getConfig().getInt(p + ".z1");
            int x2 = plugin.getConfig().getInt(p + ".x2");
            int y2 = plugin.getConfig().getInt(p + ".y2");
            int z2 = plugin.getConfig().getInt(p + ".z2");
            zones.put(key.toLowerCase(), new MapZone(key, world, x1, y1, z1, x2, y2, z2));
        }
        plugin.getLogger().info("[MapZoneManager] " + zones.size() + " zone(s) chargée(s).");
    }
}

