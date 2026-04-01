package fr.segame.armesia.config;

import fr.segame.armesia.loot.LootData;
import fr.segame.armesia.loot.LootManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class LootConfig {

    private final JavaPlugin plugin;
    private final LootManager lootManager;
    private final File file;
    private FileConfiguration config;

    public LootConfig(JavaPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
        this.file = new File(plugin.getDataFolder(), "loots.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    // ─── Chargement ───────────────────────────────────────────────────────────

    public void load() {
        reload();
        ConfigurationSection root = config.getConfigurationSection("tables");
        if (root == null) return;

        for (String tableId : root.getKeys(false)) {
            List<LootData> entries = new ArrayList<>();
            ConfigurationSection tableSection = root.getConfigurationSection(tableId);
            if (tableSection == null) continue;

            for (String key : tableSection.getKeys(false)) {
                String path = "tables." + tableId + "." + key;

                int min    = config.getInt(path + ".min", 1);
                int max    = config.getInt(path + ".max", 1);
                double pct = config.getDouble(path + ".chance", 1.0);

                // Item personnalisé (main hand) ?
                if (config.isItemStack(path + ".item")) {
                    ItemStack item = config.getItemStack(path + ".item");
                    if (item != null && item.getType() != Material.AIR) {
                        entries.add(new LootData(item, min, max, pct));
                        continue;
                    }
                }

                // Material simple
                String matStr = config.getString(path + ".material", "");
                try {
                    Material mat = Material.valueOf(matStr.toUpperCase());
                    entries.add(new LootData(mat, min, max, pct));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[LootConfig] Material invalide '" + matStr + "' ignoré.");
                }
            }

            lootManager.register(tableId, entries);
        }


    }

    // ─── Sauvegarde ───────────────────────────────────────────────────────────

    public void save() {
        config.set("tables", null);

        for (var entry : lootManager.getAllTables().entrySet()) {
            String tableId = entry.getKey();
            List<LootData> loots = entry.getValue();
            for (int i = 0; i < loots.size(); i++) {
                writeEntry(tableId, i, loots.get(i));
            }
        }

        persist();
    }

    public void saveTable(String tableId) {
        // Efface les anciennes entrées de cette table
        config.set("tables." + tableId, null);

        List<LootData> loots = lootManager.getTable(tableId);
        if (loots != null) {
            for (int i = 0; i < loots.size(); i++) {
                writeEntry(tableId, i, loots.get(i));
            }
        }

        persist();
    }

    public void deleteTable(String tableId) {
        config.set("tables." + tableId, null);
        persist();
    }

    // ─── Interne ──────────────────────────────────────────────────────────────

    private void writeEntry(String tableId, int index, LootData loot) {
        String path = "tables." + tableId + "." + index;
        config.set(path + ".min",    loot.getMin());
        config.set(path + ".max",    loot.getMax());
        config.set(path + ".chance", loot.getChance());

        if (loot.isCustomItem()) {
            config.set(path + ".item",     loot.getCustomItem());
            config.set(path + ".material", null);
        } else {
            config.set(path + ".material", loot.getMaterial().name());
            config.set(path + ".item",     null);
        }
    }

    private void persist() {
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[LootConfig] Erreur lors de la sauvegarde.", e);
        }
    }
}

