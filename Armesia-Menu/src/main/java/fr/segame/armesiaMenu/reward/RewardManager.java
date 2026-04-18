package fr.segame.armesiaMenu.reward;

import org.bukkit.configuration.file.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardManager {

    private final JavaPlugin plugin;

    private final File file;
    private final FileConfiguration config;

    public RewardManager(JavaPlugin plugin) {
        this.plugin = plugin;

        file = new File(plugin.getDataFolder(), "rewards.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasClaimed(UUID uuid, String key) {
        return config.getBoolean("players." + uuid + "." + key, false);
    }

    public void claim(UUID uuid, String key) {
        config.set("players." + uuid + "." + key, true);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}