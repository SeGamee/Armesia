package fr.segame.armesiaWorld;

import fr.segame.armesia.Main;
import fr.segame.armesiaWorld.commands.MapZoneCommand;
import fr.segame.armesiaWorld.commands.PortalCommand;
import fr.segame.armesiaWorld.commands.SetSpawnCommand;
import fr.segame.armesiaWorld.commands.SpawnCommand;
import fr.segame.armesiaWorld.commands.WorldCommand;
import fr.segame.armesiaWorld.listeners.MapWandListener;
import fr.segame.armesiaWorld.listeners.PortalListener;
import fr.segame.armesiaWorld.listeners.SpawnListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainWorld extends JavaPlugin {

    private WorldManager     worldManager;
    private SpawnManager     spawnManager;
    private TeleportManager  teleportManager;
    private MapZoneManager   mapZoneManager;
    private ParachuteManager parachuteManager;
    private PortalManager    portalManager;

    @Override
    public void onEnable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Armesia");

        if (!(plugin instanceof Main)) {
            getLogger().severe("Armesia non trouvé ! Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        // 1. Mondes
        worldManager = new WorldManager(this);
        worldManager.loadAllSavedWorlds();

        // 2. Spawn & téléportation
        spawnManager     = new SpawnManager(this);
        teleportManager  = new TeleportManager(this);

        // 3. Zones de map & parachute
        mapZoneManager   = new MapZoneManager(this);
        parachuteManager = new ParachuteManager(this);

        // 4. Portails (doit être créé APRES mapZoneManager et parachuteManager)
        portalManager = new PortalManager(this);

        // ── Commandes ────────────────────────────────────────────────────────
        WorldCommand worldCmd = new WorldCommand(worldManager);
        getCommand("world").setExecutor(worldCmd);
        getCommand("world").setTabCompleter(worldCmd);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager, teleportManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));

        MapWandListener wandListener = new MapWandListener(this);

        MapZoneCommand mapZoneCmd = new MapZoneCommand(this, mapZoneManager, wandListener);
        getCommand("mapzone").setExecutor(mapZoneCmd);
        getCommand("mapzone").setTabCompleter(mapZoneCmd);

        PortalCommand portalCmd = new PortalCommand(mapZoneManager, portalManager, wandListener);
        getCommand("portal").setExecutor(portalCmd);
        getCommand("portal").setTabCompleter(portalCmd);

        // ── Listeners ─────────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(
                new SpawnListener(this, spawnManager, teleportManager), this);
        getServer().getPluginManager().registerEvents(wandListener, this);
        getServer().getPluginManager().registerEvents(
                new PortalListener(this, portalManager, mapZoneManager, parachuteManager), this);

        getLogger().info("Armesia-World activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (portalManager != null) portalManager.shutdown();
        getLogger().info("Armesia-World désactivé.");
    }

    public WorldManager     getWorldManager()     { return worldManager;     }
    public SpawnManager     getSpawnManager()      { return spawnManager;     }
    public TeleportManager  getTeleportManager()   { return teleportManager;  }
    public MapZoneManager   getMapZoneManager()    { return mapZoneManager;   }
    public ParachuteManager getParachuteManager()  { return parachuteManager; }
    public PortalManager    getPortalManager()     { return portalManager;    }
}


