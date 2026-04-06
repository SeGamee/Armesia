package fr.segame.armesiaLevel.player;

import fr.segame.armesiaLevel.ArmesiaLevel;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Gère la persistance des données xp/level dans players.yml
 * (dossier propre à Armesia-Level).
 */
public class PlayerDataManager {

    private final ArmesiaLevel plugin;
    private final PlayerManager playerManager;

    private File playersFile;
    private FileConfiguration playersConfig;

    public PlayerDataManager(ArmesiaLevel plugin, PlayerManager playerManager) {
        this.plugin        = plugin;
        this.playerManager = playerManager;
        setupFile();
    }

    private void setupFile() {
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

    public void save() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Charge xp/level depuis le fichier et alimente le PlayerManager en mémoire. */
    public void loadPlayer(Player player) {
        UUID uuid  = player.getUniqueId();
        int xp     = playersConfig.getInt("players." + uuid + ".xp",    0);
        int level  = playersConfig.getInt("players." + uuid + ".level", 1);
        GamePlayer gp = playerManager.getPlayer(uuid);
        gp.setXp(xp);
        gp.setLevel(level);
    }

    /** Persiste xp/level et libère la mémoire. */
    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        GamePlayer gp = playerManager.getPlayer(uuid);
        playersConfig.set("players." + uuid + ".xp",    gp.getXp());
        playersConfig.set("players." + uuid + ".level", gp.getLevel());
        save();
        playerManager.removePlayer(uuid);
    }
}

