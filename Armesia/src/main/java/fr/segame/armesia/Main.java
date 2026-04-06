package fr.segame.armesia;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.api.StatsAPI;
import fr.segame.armesia.commands.*;
import fr.segame.armesia.listeners.BlockListener;
import fr.segame.armesia.listeners.KillListener;
import fr.segame.armesia.listeners.PlayersListeners;
import fr.segame.armesia.managers.*;
import fr.segame.armesia.player.PlayerDataManager;
import fr.segame.armesiaLevel.ArmesiaLevel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class Main extends JavaPlugin {

    public static Main instance;

    // ─── Managers statiques (accès direct depuis toute la codebase) ─────────────
    public static GroupManager   groupManager;
    public static StatsManager   statsManager;
    public static EconomyManager economyManager;

    // ─── Caches joueur partagés (lus/écrits par PlayerDataManager & compagnie) ──
    public static Map<UUID, String>  groups            = new HashMap<>();
    public static Map<UUID, String>  jobs              = new HashMap<>();

    // ─── Managers d'instance ────────────────────────────────────────────────────
    private EconomyAPI        economyAPI;
    private StatsAPI          statsAPI;
    private PlayerDataManager playerDataManager;
    private TabManager        tabManager;

    // ============================================================================
    //  Cycle de vie du plugin
    // ============================================================================

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // ─── Initialisation des managers ────────────────────────────────────────
        groupManager      = new GroupManager(this);
        groupManager.ensureDefaultGroupExists();

        playerDataManager = new PlayerDataManager(this);
        tabManager        = new TabManager(this);

        economyManager    = new EconomyManager(this);
        economyAPI        = new EconomyAPI(economyManager);

        statsManager      = new StatsManager(playerDataManager.getPlayersConfig());
        statsAPI          = new StatsAPI(statsManager);

        // ─── Enregistrement du callback de changement de niveau ─────────────────
        // Armesia-Level notifie ici pour mettre à jour le tab et le scoreboard.
        Plugin levelPlugin = Bukkit.getPluginManager().getPlugin("Armesia-Level");
        if (levelPlugin instanceof ArmesiaLevel) {
            ArmesiaLevel.registerLevelChangeCallback(uuid -> {
                Main.updateAllTabs();
                Main.updatePlayerScoreboard(uuid);
            });
        }

        // ─── Enregistrement des événements et des commandes ─────────────────────
        registerEvents();
        registerCommands();
    }

    @Override
    public void onDisable() {
        playerDataManager.savePlayers();
    }

    // ============================================================================
    //  Enregistrement des événements
    // ============================================================================

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new KillListener(this, statsManager, economyManager), this);
        pm.registerEvents(new PlayersListeners(), this);
        pm.registerEvents(new BlockListener(), this);
    }

    // ============================================================================
    //  Enregistrement des commandes
    // ============================================================================

    private void registerCommands() {
        getCommand("heal").setExecutor(new HealCommand());
        getCommand("feed").setExecutor(new FeedCommand());
        getCommand("suicide").setExecutor(new SuicideCommand());
        getCommand("kill").setExecutor(new KillCommand());
        getCommand("cleareffect").setExecutor(new ClearEffectCommand());
        getCommand("broadcast").setExecutor(new BroadcastCommand());
        getCommand("group").setExecutor(new GroupCommand());
        getCommand("reloadconfig").setExecutor(new ReloadConfigCommand());
        getCommand("money").setExecutor(new EconomyCommand(this));
        getCommand("tokens").setExecutor(new EconomyCommand(this));
        getCommand("pay").setExecutor(new EconomyCommand(this));
    }

    // ============================================================================
    //  Getters d'instance
    // ============================================================================

    public static Main          getInstance()        { return instance; }
    public static GroupManager  getGroupManager()    { return groupManager; }
    public static StatsManager  getStatsManager()    { return statsManager; }
    public static EconomyManager getEconomyManager() { return economyManager; }

    public EconomyAPI        getEconomyAPI()         { return economyAPI; }
    public StatsAPI          getStatsAPI()           { return statsAPI; }
    public PlayerDataManager getPlayerDataManager()  { return playerDataManager; }
    public TabManager        getTabManager()         { return tabManager; }

    // ============================================================================
    //  Délégués → PlayerDataManager  (rétro-compatibilité avec les appelants)
    // ============================================================================

    /** Accès au fichier players.yml (utilisé par EconomyManager, LevelManager…) */
    public FileConfiguration getPlayersConfig() { return playerDataManager.getPlayersConfig(); }
    public void savePlayers()                   { playerDataManager.savePlayers(); }

    public static void loadPlayer(Player player)                        { instance.playerDataManager.loadPlayer(player); }
    public static void savePlayer(Player player)                        { instance.playerDataManager.savePlayer(player); }
    public static boolean hasGroupPermission(Player player, String perm){ return instance.playerDataManager.hasGroupPermission(player, perm); }

    // ============================================================================
    //  Délégués → TabManager  (rétro-compatibilité avec les appelants)
    // ============================================================================

    public static void   updateTab(Player player)              { instance.tabManager.updateTab(player); }
    public static void   updateAllTabs()                       { instance.tabManager.updateAllTabs(); }
    public static String getGroupChatPrefix(String group)      { return instance.tabManager.getGroupChatPrefix(group); }
    public static String getGroupTabPrefix(String group)       { return instance.tabManager.getGroupTabPrefix(group); }

    /** Enregistré par Armesia-Scoreboard au démarrage (pas de dépendance inverse). */
    public static void registerScoreboardUpdateCallback(Consumer<Player> callback) {
        TabManager.registerScoreboardUpdateCallback(callback);
    }

    public static void updatePlayerScoreboard(UUID uuid) {
        TabManager.updatePlayerScoreboard(uuid);
    }

}