package fr.segame.armesia.player;

import fr.segame.armesia.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Gère la persistance des données joueur (players.yml) ainsi que
 * le chargement / la sauvegarde en mémoire de leurs informations.
 */
public class PlayerDataManager {

    private final Main plugin;
    private File playersFile;
    private FileConfiguration playersConfig;

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        setupPlayersFile();
    }

    // ─── players.yml ────────────────────────────────────────────────────────────

    private void setupPlayersFile() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            playersFile.getParentFile().mkdirs();
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    public void savePlayers() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Chargement / Sauvegarde d'un joueur ────────────────────────────────────

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        String group = Main.groupManager.getValidGroupOrDefault(
                plugin.getConfig().getString("players." + uuid + ".group")
        );
        String job = plugin.getConfig().getString("players." + uuid + ".job", "Citoyen");

        Main.groups.put(uuid, group);
        Main.jobs.put(uuid, job);
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getConfig().set("players." + uuid + ".group",            Main.groups.get(uuid));
        plugin.getConfig().set("players." + uuid + ".job",              Main.jobs.get(uuid));
        plugin.saveConfig();
    }

    // ─── Permissions ────────────────────────────────────────────────────────────

    public boolean hasGroupPermission(Player player, String permission) {
        if (player.isOp()) return true;
        if (player.hasPermission(permission)) return true;

        String group = Main.groups.get(player.getUniqueId());
        group = Main.groupManager.getValidGroupOrDefault(group);

        List<String> permissions = plugin.getConfig()
                .getStringList("groups." + group + ".permissions");

        if (permissions.contains("*")) return true;
        return permissions.contains(permission);
    }
}

