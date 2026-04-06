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

            String name      = config.getString(path + ".name", id);
            double health    = config.getDouble(path + ".health", 20.0);
            String lootTable = config.getString(path + ".loot-table", "");
            String typeStr   = config.getString(path + ".entity-type", "ZOMBIE");

            // Compat rétrograde : ancienne clé "money" fixe
            int legacyMoney = config.getInt(path + ".money", -1);
            int moneyMin = config.getInt(path + ".money-min", legacyMoney >= 0 ? legacyMoney : 0);
            int moneyMax = config.getInt(path + ".money-max", legacyMoney >= 0 ? legacyMoney : 0);
            int xpMin    = config.getInt(path + ".xp-min", 0);
            int xpMax    = config.getInt(path + ".xp-max", 0);

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MobConfig] Type invalide '" + typeStr + "' pour le mob '" + id + "' → ZOMBIE utilisé.");
                entityType = EntityType.ZOMBIE;
            }

            MobData mobData = new MobData(id, name, entityType, health, moneyMin, moneyMax, xpMin, xpMax, lootTable);
            mobManager.registerMob(mobData);
        }


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
        config.set(path + ".health",      mob.getHealth());
        config.set(path + ".money",       null);          // supprime l'ancienne clé fixe
        config.set(path + ".money-min",   mob.getMoneyMin());
        config.set(path + ".money-max",   mob.getMoneyMax());
        config.set(path + ".xp-min",      mob.getXpMin());
        config.set(path + ".xp-max",      mob.getXpMax());
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




