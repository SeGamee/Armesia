package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final Main plugin;
    private File file;
    private FileConfiguration config;

    public KitManager(Main plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Kits ──────────────────────────────────────────────────────────────────

    public Set<String> getKitNames() {
        var s = config.getConfigurationSection("kits");
        return s == null ? Collections.emptySet() : s.getKeys(false);
    }

    public boolean kitExists(String name) {
        return config.contains("kits." + name.toLowerCase());
    }

    public String getKitPermission(String name) {
        return config.getString("kits." + name.toLowerCase() + ".permission", "");
    }

    public long getKitCooldown(String name) {
        return config.getLong("kits." + name.toLowerCase() + ".cooldown", 0L);
    }

    public String getKitDisplayName(String name) {
        return config.getString("kits." + name.toLowerCase() + ".display-name", name);
    }

    public ItemStack[] getKitItems(String name) {
        String path = "kits." + name.toLowerCase() + ".items";
        var section = config.getConfigurationSection(path);
        if (section == null) return new ItemStack[0];

        int max = 0;
        for (String k : section.getKeys(false)) {
            try { max = Math.max(max, Integer.parseInt(k)); } catch (NumberFormatException ignored) {}
        }

        ItemStack[] items = new ItemStack[max + 1];
        for (String k : section.getKeys(false)) {
            try { items[Integer.parseInt(k)] = config.getItemStack(path + "." + k); }
            catch (NumberFormatException ignored) {}
        }
        return items;
    }

    /**
     * Crée ou remplace un kit à partir des items d'un inventaire.
     *
     * @param name        Nom du kit (lowercase automatique)
     * @param permission  Permission requise (vide = ouvert à tous)
     * @param cooldown    Cooldown en secondes
     * @param displayName Nom affiché
     * @param inv         Inventaire source
     */
    public void createKit(String name, String permission, long cooldown, String displayName, Inventory inv) {
        String path = "kits." + name.toLowerCase();
        config.set(path + ".display-name", displayName);
        config.set(path + ".permission",   permission);
        config.set(path + ".cooldown",     cooldown);
        config.set(path + ".items",        null);   // reset
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                config.set(path + ".items." + i, item);
            }
        }
        save();
    }

    public void deleteKit(String name) {
        config.set("kits." + name.toLowerCase(), null);
        save();
    }

    // ── Cooldowns ─────────────────────────────────────────────────────────────

    public boolean isOnCooldown(UUID uuid, String kitName) {
        return getRemainingMs(uuid, kitName) > 0;
    }

    /** @return millisecondes restantes, 0 si pas de cooldown */
    public long getRemainingMs(UUID uuid, String kitName) {
        long expiry = config.getLong("cooldowns." + uuid + "." + kitName.toLowerCase(), 0L);
        long diff   = expiry - System.currentTimeMillis();
        return diff > 0 ? diff : 0;
    }

    public void setCooldown(UUID uuid, String kitName) {
        long seconds = getKitCooldown(kitName);
        if (seconds <= 0) return;
        config.set("cooldowns." + uuid + "." + kitName.toLowerCase(),
                System.currentTimeMillis() + seconds * 1000L);
        save();
    }

    public void resetCooldown(UUID uuid, String kitName) {
        config.set("cooldowns." + uuid + "." + kitName.toLowerCase(), null);
        save();
    }

    // ── Formatage du temps ────────────────────────────────────────────────────

    public static String formatDuration(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}

