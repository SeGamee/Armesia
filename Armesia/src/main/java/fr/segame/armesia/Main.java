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
import fr.segame.armesia.player.GamePlayer;import fr.segame.armesia.mobs.MobListener;
import fr.segame.armesia.mobs.MobManager;
import fr.segame.armesia.mobs.MobSpawner;
import fr.segame.armesia.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public final class Main extends JavaPlugin {

    public static Main instance;
    public static GroupManager groupManager;
    public static StatsManager statsManager;
    public static EconomyManager economyManager;
    private EconomyAPI economyAPI;
    private StatsAPI statsAPI;
    private PlayerManager playerManager;
    private LevelManager levelManager;

    // Système de mobs/zones/loots
    private MobManager  mobManager;
    private MobSpawner  mobSpawner;
    private LootManager lootManager;
    private ZoneManager zoneManager;
    private DebugManager debugManager;
    private MobConfig   mobConfig;
    private LootConfig  lootConfig;
    private ZoneConfig  zoneConfig;

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

        playerManager = new PlayerManager();
        levelManager = new LevelManager(playerManager);

        mobManager   = new MobManager();
        mobSpawner   = new MobSpawner(mobManager);
        lootManager  = new LootManager();
        debugManager = new DebugManager();
        zoneManager  = new ZoneManager(this, mobSpawner, mobManager, debugManager);

        // ─── Chargement depuis les fichiers YAML ───────────────────────────
        mobConfig  = new MobConfig(this, mobManager);
        lootConfig = new LootConfig(this, lootManager);
        zoneConfig = new ZoneConfig(this, zoneManager);

        mobConfig.load();
        lootConfig.load();
        // Les zones peuvent référencer des mobs → on charge après
        zoneConfig.load();



        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new KillListener(this, statsManager, economyManager), this);
        pluginManager.registerEvents(new PlayersListeners(), this);
        pluginManager.registerEvents(new BlockListener(), this);
        pluginManager.registerEvents(new DamageListener(this), this);
        pluginManager.registerEvents(new MobListener(mobManager, lootManager, zoneManager, debugManager, economyAPI, levelManager), this);
        pluginManager.registerEvents(new fr.segame.armesia.zones.ZoneListener(zoneManager, debugManager), this);

        // ─── Commandes existantes ─────────────────────────────────────────
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

        // ─── Nouvelles commandes mob/loot/zone ────────────────────────────
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

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MobManager getMobManager()   { return mobManager; }
    public LootManager getLootManager() { return lootManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public MobConfig getMobConfig()     { return mobConfig; }
    public LootConfig getLootConfig()   { return lootConfig; }
    public ZoneConfig getZoneConfig()   { return zoneConfig; }

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

        // 🔥 XP + LEVEL
        int xp = getInstance().getPlayersConfig()
                .getInt("players." + uuid + ".xp", 0);

        int level = getInstance().getPlayersConfig()
                .getInt("players." + uuid + ".level", 1);

        GamePlayer gp = getInstance().getPlayerManager().getPlayer(uuid);

        gp.setXp(xp);
        gp.setLevel(level);

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

        String prefix = "§7";
        if (showTabPrefix) {
            prefix = getGroupTabPrefix(group);
        }

        // Récupérer le vrai niveau du joueur via GamePlayer
        GamePlayer gp = getInstance().getPlayerManager().getPlayer(player.getUniqueId());
        int level = gp != null ? gp.getLevel() : 1;

        // Affichage dans le TAB
        player.setPlayerListName("§7[" + level + "✫] " + prefix + player.getName());

        // ---------- TRI PAR NIVEAU VIA TEAMS DE SCOREBOARD ----------
        // On met à jour l'entrée de CE joueur dans le scoreboard de CHAQUE joueur en ligne.
        // Chaque joueur a son propre scoreboard (créé par Armesia-Scoreboard),
        // donc on doit propager le changement sur tous.
        String teamName = String.format("lvl_%04d", 9999 - level);
        String nametagPrefix = "§7[" + level + "✫] " + prefix;
        ChatColor nameColor = getLastChatColor(nametagPrefix);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();

            // Retirer ce joueur de toute autre team lvl_ sur ce scoreboard
            for (Team t : board.getTeams()) {
                if (t.getName().startsWith("lvl_") && t.hasEntry(player.getName())) {
                    t.removeEntry(player.getName());
                }
            }

            // Créer la team si elle n'existe pas encore, puis y ajouter le joueur
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            // Appliquer le préfixe du nametag (au-dessus de la tête) identique au TAB
            team.setPrefix(nametagPrefix);
            // Forcer la couleur du nom du joueur pour qu'elle corresponde au TAB
            team.setColor(nameColor);
            team.addEntry(player.getName());
        }
    }

    public static void updateAllTabs() {
        // Met à jour le TAB de tous les joueurs actuellement connectés
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTab(player);
        }
    }

    // ── Callback enregistré par Armesia-Scoreboard au démarrage ──────────────
    // Main n'importe rien d'Armesia-Scoreboard : pas de dépendance circulaire.
    private static Consumer<Player> scoreboardUpdateCallback = null;

    public static void registerScoreboardUpdateCallback(Consumer<Player> callback) {
        scoreboardUpdateCallback = callback;
    }

    // ── Déclenche la mise à jour immédiate de la sidebar d'un joueur ──────────
    public static void updatePlayerScoreboard(UUID uuid) {
        if (scoreboardUpdateCallback == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        scoreboardUpdateCallback.accept(player);
    }

    // ── Extraire la dernière couleur active dans une chaîne §X ─────────────────
    // Utilisé pour que team.setColor() corresponde à la couleur du nom en TAB.
    private static ChatColor getLastChatColor(String text) {
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '§') {
                ChatColor c = ChatColor.getByChar(text.charAt(i + 1));
                if (c != null && c.isColor()) {
                    return c;
                }
            }
        }
        return ChatColor.WHITE;
    }

}