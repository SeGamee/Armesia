package fr.segame.armesia;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.api.StatsAPI;
import fr.segame.armesia.commands.*;
import fr.segame.armesia.config.LootConfig;
import fr.segame.armesia.config.MobConfig;
import fr.segame.armesia.config.ZoneConfig;
import fr.segame.armesia.listeners.BlockListener;
import fr.segame.armesia.listeners.DamageListener;
import fr.segame.armesia.listeners.KillListener;
import fr.segame.armesia.listeners.PlayersListeners;
import fr.segame.armesia.loot.LootManager;
import fr.segame.armesia.managers.*;
import fr.segame.armesia.mobs.MobListener;
import fr.segame.armesia.mobs.MobManager;
import fr.segame.armesia.mobs.MobSpawner;
import fr.segame.armesia.player.PlayerDataManager;
import fr.segame.armesia.zones.ZoneListener;
import fr.segame.armesia.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
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
    public static Map<UUID, Boolean> chatPrefixEnabled = new HashMap<>();
    public static Map<UUID, Boolean> tabPrefixEnabled  = new HashMap<>();
    public static Map<UUID, String>  groups            = new HashMap<>();
    public static Map<UUID, String>  jobs              = new HashMap<>();

    // ─── Managers d'instance ────────────────────────────────────────────────────
    private EconomyAPI        economyAPI;
    private StatsAPI          statsAPI;
    private PlayerManager     playerManager;
    private LevelManager      levelManager;
    private PlayerDataManager playerDataManager;
    private TabManager        tabManager;

    // ─── Système mobs / zones / loots ───────────────────────────────────────────
    private MobManager   mobManager;
    private MobSpawner   mobSpawner;
    private LootManager  lootManager;
    private ZoneManager  zoneManager;
    private DebugManager debugManager;
    private MobConfig    mobConfig;
    private LootConfig   lootConfig;
    private ZoneConfig   zoneConfig;

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

        playerManager     = new PlayerManager();
        levelManager      = new LevelManager(playerManager);

        mobManager        = new MobManager();
        mobSpawner        = new MobSpawner(mobManager);
        lootManager       = new LootManager();
        debugManager      = new DebugManager();
        zoneManager       = new ZoneManager(this, mobSpawner, mobManager, debugManager);

        // ─── Chargement des configs YAML ────────────────────────────────────────
        mobConfig  = new MobConfig(this, mobManager);
        lootConfig = new LootConfig(this, lootManager);
        zoneConfig = new ZoneConfig(this, zoneManager);

        mobConfig.load();
        lootConfig.load();
        zoneConfig.load(); // après mobs car les zones référencent des mobs

        // ─── Nettoyage des mobs vanilla résiduels ───────────────────────────────
        // Les mobs non-customs (sans metadata "customMob") sont des vestiges du monde
        // qui ont échappé au filtre onSpawn (spawné avant l'activation du plugin).
        // On les supprime au prochain tick, une fois les mondes entièrement chargés.
        Bukkit.getScheduler().runTask(this, () -> {
            int removed = 0;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity entity : world.getEntities()) {
                    if (!(entity instanceof Mob)) continue;
                    if (entity.hasMetadata("customMob")) continue;
                    entity.remove();
                    removed++;
                }
            }
            if (removed > 0)
                getLogger().info("[Armesia] Nettoyage démarrage : " + removed + " mob(s) vanilla supprimé(s).");
        });

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
        pm.registerEvents(new DamageListener(this), this);
        pm.registerEvents(new MobListener(mobManager, lootManager, zoneManager, debugManager, economyAPI, levelManager), this);
        pm.registerEvents(new ZoneListener(zoneManager, debugManager), this);
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
        getCommand("start").setExecutor(new StartCommand());
        getCommand("reloadconfig").setExecutor(new ReloadConfigCommand());
        getCommand("menu").setExecutor(new MenuCommand());
        getCommand("money").setExecutor(new EconomyCommand(this));
        getCommand("tokens").setExecutor(new EconomyCommand(this));
        getCommand("pay").setExecutor(new EconomyCommand(this));
        getCommand("level").setExecutor(new LevelCommand(this));

        MobCommand  mobCmd  = new MobCommand(mobManager, mobConfig, mobSpawner);
        LootCommand lootCmd = new LootCommand(lootManager, lootConfig);
        ZoneCommand zoneCmd = new ZoneCommand(zoneManager, mobManager, zoneConfig, debugManager);

        getCommand("mob").setExecutor(mobCmd);
        getCommand("mob").setTabCompleter(mobCmd);
        getCommand("loot").setExecutor(lootCmd);
        getCommand("loot").setTabCompleter(lootCmd);
        getCommand("zone").setExecutor(zoneCmd);
        getCommand("zone").setTabCompleter(zoneCmd);
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
    public PlayerManager     getPlayerManager()      { return playerManager; }
    public LevelManager      getLevelManager()       { return levelManager; }
    public PlayerDataManager getPlayerDataManager()  { return playerDataManager; }
    public TabManager        getTabManager()         { return tabManager; }

    public MobManager  getMobManager()  { return mobManager; }
    public LootManager getLootManager() { return lootManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public MobConfig   getMobConfig()   { return mobConfig; }
    public LootConfig  getLootConfig()  { return lootConfig; }
    public ZoneConfig  getZoneConfig()  { return zoneConfig; }

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