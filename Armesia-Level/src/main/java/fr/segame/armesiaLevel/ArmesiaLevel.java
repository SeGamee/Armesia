package fr.segame.armesiaLevel;

import fr.segame.armesiaLevel.api.LevelAPI;
import fr.segame.armesiaLevel.commands.LevelCommand;
import fr.segame.armesiaLevel.commands.ReloadLevelCommand;
import fr.segame.armesiaLevel.config.LevelConfig;
import fr.segame.armesiaLevel.listeners.PlayerLevelListener;
import fr.segame.armesiaLevel.managers.LevelManager;
import fr.segame.armesiaLevel.player.PlayerDataManager;
import fr.segame.armesiaLevel.player.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Consumer;

public final class ArmesiaLevel extends JavaPlugin {

    private static ArmesiaLevel instance;

    private PlayerManager     playerManager;
    private PlayerDataManager playerDataManager;
    private LevelManager      levelManager;
    private LevelConfig       levelConfig;

    /** Callback enregistré par Armesia pour mettre à jour le tab/scoreboard. */
    private static Consumer<UUID> levelChangeCallback;

    // ============================================================================
    //  Cycle de vie
    // ============================================================================

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        levelConfig = new LevelConfig(this);

        playerManager     = new PlayerManager();
        playerDataManager = new PlayerDataManager(this, playerManager);
        levelManager      = new LevelManager(playerManager, this);

        LevelAPI.init(levelManager);

        getServer().getPluginManager().registerEvents(
                new PlayerLevelListener(playerDataManager), this);

        getCommand("level").setExecutor(new LevelCommand(levelManager));
        getCommand("reloadlevel").setExecutor(new ReloadLevelCommand(this));

        getLogger().info("[Armesia-Level] Système de niveaux activé.");
    }

    @Override
    public void onDisable() {
        // Sauvegarder les joueurs encore connectés (ex : reload)
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            playerDataManager.savePlayer(player);
        }
        levelChangeCallback = null;
        getLogger().info("[Armesia-Level] Données sauvegardées.");
    }

    // ============================================================================
    //  Rechargement de la config
    // ============================================================================

    public void reloadLevelConfig() {
        reloadConfig();
        levelConfig = new LevelConfig(this);
    }

    // ============================================================================
    //  Callback de changement de niveau (hook pour Armesia / Scoreboard)
    // ============================================================================

    /**
     * Permet à un plugin tiers (Armesia) d'être notifié à chaque changement
     * de niveau / XP pour mettre à jour son tab et son scoreboard.
     */
    public static void registerLevelChangeCallback(Consumer<UUID> callback) {
        levelChangeCallback = callback;
    }

    public static Consumer<UUID> getLevelChangeCallback() {
        return levelChangeCallback;
    }

    // ============================================================================
    //  Getters
    // ============================================================================

    public static ArmesiaLevel getInstance()         { return instance; }
    public PlayerManager     getPlayerManager()      { return playerManager; }
    public PlayerDataManager getPlayerDataManager()  { return playerDataManager; }
    public LevelManager      getLevelManager()       { return levelManager; }
    public LevelConfig       getLevelConfig()        { return levelConfig; }
}
