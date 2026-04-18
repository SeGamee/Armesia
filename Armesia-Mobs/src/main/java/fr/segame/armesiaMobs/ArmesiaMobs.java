package fr.segame.armesiaMobs;

import fr.segame.armesiaMobs.api.MobStatsAPI;
import fr.segame.armesiaMobs.api.MobStatsImpl;
import fr.segame.armesiaMobs.commands.LootCommand;
import fr.segame.armesiaMobs.commands.MobCommand;
import fr.segame.armesiaMobs.commands.StatsCommand;
import fr.segame.armesiaMobs.commands.ZoneCommand;
import fr.segame.armesiaMobs.mobs.MobListener;
import fr.segame.armesiaMobs.zones.ZoneListener;
import fr.segame.armesiaMobs.config.LootConfig;
import fr.segame.armesiaMobs.config.MessagesConfig;
import fr.segame.armesiaMobs.config.MobConfig;
import fr.segame.armesiaMobs.config.ZoneConfig;
import fr.segame.armesiaMobs.loot.LootManager;
import fr.segame.armesiaMobs.managers.DebugManager;
import fr.segame.armesiaMobs.managers.StatsManager;
import fr.segame.armesiaMobs.mobs.MobInstance;
import fr.segame.armesiaMobs.mobs.MobManager;
import fr.segame.armesiaMobs.mobs.MobSpawner;
import fr.segame.armesiaMobs.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArmesiaMobs extends JavaPlugin {

    public static ArmesiaMobs instance;

    /** Clé PDC pour l'identifiant du mob (persiste dans les fichiers monde). */
    public static NamespacedKey MOB_ID_KEY;
    /** Clé PDC pour l'identifiant de la zone (persiste dans les fichiers monde). */
    public static NamespacedKey ZONE_ID_KEY;

    private MobManager mobManager;
    private MobSpawner mobSpawner;
    private LootManager lootManager;
    private ZoneManager zoneManager;
    private DebugManager debugManager;
    private StatsManager statsManager;
    private MessagesConfig messagesConfig;
    private MobConfig mobConfig;
    private LootConfig lootConfig;
    private ZoneConfig zoneConfig;

    public static ArmesiaMobs getInstance()          { return instance; }
    public MessagesConfig getMessages()              { return messagesConfig; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // Copie les fichiers par défaut s'ils n'existent pas encore
        saveResource("mobs.yml",  false);
        saveResource("zones.yml", false);

        // ─── Clés PDC ───────────────────────────────────────────────────────────
        MOB_ID_KEY  = new NamespacedKey(this, "mob_id");
        ZONE_ID_KEY = new NamespacedKey(this, "zone_id");

        mobManager     = new MobManager();
        mobSpawner     = new MobSpawner(mobManager);
        lootManager    = new LootManager();
        debugManager   = new DebugManager();
        statsManager   = new StatsManager(this);
        messagesConfig = new MessagesConfig(this);
        zoneManager    = new ZoneManager(this, mobSpawner, mobManager, debugManager);

        // ─── Chargement des configs YAML ────────────────────────────────────────
        mobConfig  = new MobConfig(this, mobManager);
        lootConfig = new LootConfig(this, lootManager);
        zoneConfig = new ZoneConfig(this, zoneManager);

        mobConfig.load();
        lootConfig.load();
        zoneConfig.load(); // après mobs car les zones référencent des mobs

        // ─── Nettoyage des mobs au démarrage ────────────────────────────────────
        // Utilise le PDC (persistant) pour distinguer les vrais custom mobs.
        // Les mobs sans tag PDC sont des vestiges vanilla ou des orphelins.
        // Les mobs avec tag PDC mais sans instance sont réenregistrés.
        Bukkit.getScheduler().runTask(this, () -> {
            int removed = 0, restored = 0;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity entity : world.getEntities()) {
                    if (!(entity instanceof Mob mob)) continue;
                    var pdc = mob.getPersistentDataContainer();

                    if (!pdc.has(MOB_ID_KEY, PersistentDataType.STRING)) {
                        mob.remove();
                        removed++;
                        continue;
                    }

                    // Mob custom PDC-taggé mais instance perdue (redémarrage serveur)
                    if (mobManager.getInstance(mob.getUniqueId()) == null) {
                        String mobId  = pdc.get(MOB_ID_KEY,  PersistentDataType.STRING);
                        String zoneId = pdc.get(ZONE_ID_KEY, PersistentDataType.STRING);
                        if (mobId != null && mobManager.getMob(mobId) != null) {
                            mobManager.addInstance(new MobInstance(
                                    mob.getUniqueId(), mobId,
                                    zoneId != null ? zoneId : "manual"));
                            mob.setCanPickupItems(false);
                            MobSpawner.applyMobName(mob, mobManager.getMob(mobId));
                            restored++;
                        } else {
                            mob.remove();
                            removed++;
                        }
                    }
                }
            }
            if (removed  > 0) getLogger().info("[Armesia] Démarrage : " + removed  + " mob(s) vanilla/orphelin(s) supprimé(s).");
            if (restored > 0) getLogger().info("[Armesia] Démarrage : " + restored + " mob(s) custom réenregistré(s) depuis le PDC.");
        });

        // ─── Enregistrement de l'API mobs dans le ServicesManager ───────────────
        Bukkit.getServicesManager().register(MobStatsAPI.class, new MobStatsImpl(statsManager), this, ServicePriority.Normal);

        fr.segame.armesia.Main armesiaCore = (fr.segame.armesia.Main) getServer().getPluginManager().getPlugin("Armesia");
        if (armesiaCore == null) getLogger().warning("[Armesia-Mobs] Plugin Armesia introuvable — l'économie ne sera pas disponible.");

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MobListener(mobManager, lootManager, zoneManager, debugManager, statsManager), this);
        pm.registerEvents(new ZoneListener(zoneManager, debugManager), this);

        MobCommand   mobCmd   = new MobCommand(mobManager, mobConfig, mobSpawner, lootManager);
        LootCommand  lootCmd  = new LootCommand(lootManager, lootConfig);
        ZoneCommand  zoneCmd  = new ZoneCommand(zoneManager, mobManager, zoneConfig, debugManager);
        StatsCommand statsCmd = new StatsCommand(statsManager, mobManager);

        getCommand("mob").setExecutor(mobCmd);
        getCommand("mob").setTabCompleter(mobCmd);
        getCommand("loot").setExecutor(lootCmd);
        getCommand("loot").setTabCompleter(lootCmd);
        getCommand("zone").setExecutor(zoneCmd);
        getCommand("zone").setTabCompleter(zoneCmd);
        getCommand("mobstats").setExecutor(statsCmd);
        getCommand("mobstats").setTabCompleter(statsCmd);
    }

}
