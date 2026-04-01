package fr.segame.armesia.config;

import fr.segame.armesia.mobs.MobData;
import fr.segame.armesia.mobs.MobManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MobConfig {

    private final JavaPlugin plugin;
    private final MobManager mobManager;
    private final File file;
    private FileConfiguration config;

    public MobConfig(JavaPlugin plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.file = new File(plugin.getDataFolder(), "mobs.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    // ─── Chargement ───────────────────────────────────────────────────────────

    public void load() {
        reload();
        if (!config.isConfigurationSection("mobs")) return;

        for (String id : config.getConfigurationSection("mobs").getKeys(false)) {
            String path = "mobs." + id;

            String name        = config.getString(path + ".name", id);
            int level          = config.getInt(path + ".level", 1);
            double health      = config.getDouble(path + ".health", 20.0);
            int money          = config.getInt(path + ".money", 0);
            String lootTable   = config.getString(path + ".loot-table", "");
            String typeStr     = config.getString(path + ".entity-type", "ZOMBIE");

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MobConfig] Type invalide '" + typeStr + "' pour le mob '" + id + "' → ZOMBIE utilisé.");
                entityType = EntityType.ZOMBIE;
            }

            mobManager.registerMob(new MobData(id, name, entityType, level, health, money, lootTable));
        }

        plugin.getLogger().info("[MobConfig] " + config.getConfigurationSection("mobs").getKeys(false).size() + " mob(s) chargé(s).");
    }

    // ─── Sauvegarde ───────────────────────────────────────────────────────────

    public void save() {
        // Réécrit tout le fichier depuis l'état en mémoire
        config.set("mobs", null);

        for (MobData mob : mobManager.getAllMobs()) {
            writeMob(mob);
        }

        persist();
    }

    public void saveMob(MobData mob) {
        writeMob(mob);
        persist();
    }

    public void deleteMob(String id) {
        config.set("mobs." + id, null);
        persist();
    }

    // ─── Interne ──────────────────────────────────────────────────────────────

    private void writeMob(MobData mob) {
        String path = "mobs." + mob.getId();
        config.set(path + ".name",        mob.getName());
        config.set(path + ".entity-type", mob.getEntityType().name());
        config.set(path + ".level",       mob.getLevel());
        config.set(path + ".health",      mob.getHealth());
        config.set(path + ".money",       mob.getMoney());
        config.set(path + ".loot-table",  mob.getLootTable());
    }

    private void persist() {
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[MobConfig] Erreur lors de la sauvegarde.", e);
        }
    }
}




