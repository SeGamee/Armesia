package fr.segame.armesia;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.api.StatsAPI;
import fr.segame.armesia.commands.*;
import fr.segame.armesia.listeners.BlockListener;
import fr.segame.armesia.listeners.KillListener;
import fr.segame.armesia.listeners.PlayersListeners;
import fr.segame.armesia.managers.EconomyManager;
import fr.segame.armesia.managers.GroupManager;
import fr.segame.armesia.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Main extends JavaPlugin {

    public static Main instance;
    public static GroupManager groupManager;
    public static StatsManager statsManager;
    public static EconomyManager economyManager;
    private EconomyAPI economyAPI;
    private StatsAPI statsAPI;

    // 🔥 players.yml
    private File playersFile;
    private FileConfiguration playersConfig;

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

    public static Map<UUID, Boolean> chatPrefixEnabled = new HashMap<>();
    public static Map<UUID, Boolean> tabPrefixEnabled = new HashMap<>();
    public static Map<UUID, String> groups = new HashMap<>();
    public static Map<UUID, String> jobs = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // 🔥 CONFIG PRINCIPAL
        saveDefaultConfig();

        // 🔥 PLAYERS FILE
        setupPlayersFile();

        // 🔥 MANAGERS
        groupManager = new GroupManager(this);
        groupManager.ensureDefaultGroupExists();

        economyManager = new EconomyManager(this);
        economyAPI = new EconomyAPI(economyManager);

        statsManager = new StatsManager(playersConfig);
        statsAPI = new StatsAPI(statsManager);

        getLogger().info("Armesia démarre !");

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new KillListener(statsManager, economyManager), this);
        pluginManager.registerEvents(new PlayersListeners(), this);
        pluginManager.registerEvents(new BlockListener(), this);

        // commandes
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
    }

    @Override
    public void onDisable() {
        savePlayers(); // 🔥 sécurité
    }

    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }

    public StatsAPI getStatsAPI() {
        return statsAPI;
    }

    public static Main getInstance() {
        return instance;
    }

    public static StatsManager getStatsManager() {
        return statsManager;
    }

    public static EconomyManager getEconomyManager() {
        return economyManager;
    }

    public static GroupManager getGroupManager() {
        return groupManager;
    }

    // ---------------- PLAYERS FILE ----------------
    private void setupPlayersFile() {
        playersFile = new File(getDataFolder(), "players.yml");

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

    // ---------------- PERMISSIONS ----------------
    public static boolean hasGroupPermission(Player player, String permission) {

        if (player.isOp()) return true;

        if (player.hasPermission(permission)) return true;

        String group = groups.get(player.getUniqueId());
        group = groupManager.getValidGroupOrDefault(group);

        List<String> permissions = getInstance().getConfig()
                .getStringList("groups." + group + ".permissions");

        if (permissions.contains("*")) return true;

        return permissions.contains(permission);
    }

    // ---------------- LOAD PLAYER ----------------
    public static void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        String group = groupManager.getValidGroupOrDefault(
                getInstance().getConfig().getString("players." + uuid + ".group")
        );

        String job = getInstance().getConfig()
                .getString("players." + uuid + ".job", "Citoyen");

        boolean showChatPrefix = getInstance().getConfig()
                .getBoolean("players." + uuid + ".show-chat-prefix", true);

        boolean showTabPrefix = getInstance().getConfig()
                .getBoolean("players." + uuid + ".show-tab-prefix", true);

        chatPrefixEnabled.put(uuid, showChatPrefix);
        tabPrefixEnabled.put(uuid, showTabPrefix);

        groups.put(uuid, group);
        jobs.put(uuid, job);
    }

    // ---------------- SAVE PLAYER ----------------
    public static void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();

        getInstance().getConfig().set("players." + uuid + ".group", groups.get(uuid));
        getInstance().getConfig().set("players." + uuid + ".job", jobs.get(uuid));
        getInstance().getConfig().set("players." + uuid + ".show-chat-prefix", chatPrefixEnabled.get(uuid));
        getInstance().getConfig().set("players." + uuid + ".show-tab-prefix", tabPrefixEnabled.get(uuid));

        getInstance().saveConfig();
    }

    // ---------------- PREFIX ----------------
    public static String getGroupChatPrefix(String group) {
        return getInstance().getConfig().getString("groups." + group + ".chat-prefix", "");
    }

    public static String getGroupTabPrefix(String group) {
        return getInstance().getConfig().getString("groups." + group + ".tab-prefix", "");
    }

    // ---------------- UPDATE TAB ----------------
    public static void updateTab(Player player) {

        String group = groups.get(player.getUniqueId());
        group = groupManager.getValidGroupOrDefault(group);

        boolean showTabPrefix = tabPrefixEnabled.getOrDefault(player.getUniqueId(), true);

        String prefix = "";
        if (showTabPrefix) {
            prefix = getGroupTabPrefix(group);
        }

        int priority = groupManager.getPriority(group);

        player.setPlayerListName(prefix + "§7" + player.getName());

        // hack simple pour priorité
        player.setLevel(priority);
    }
}