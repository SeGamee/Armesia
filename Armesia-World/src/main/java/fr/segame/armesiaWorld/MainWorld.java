package fr.segame.armesiaWorld;

import fr.segame.armesia.Main;
import fr.segame.armesiaWorld.commands.SetSpawnCommand;
import fr.segame.armesiaWorld.commands.SpawnCommand;
import fr.segame.armesiaWorld.commands.WorldCommand;
import fr.segame.armesiaWorld.listeners.SpawnListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainWorld extends JavaPlugin {

    private WorldManager   worldManager;
    private SpawnManager   spawnManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Armesia");

        if (!(plugin instanceof Main core)) {
            getLogger().severe("Armesia non trouvé ! Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        // 1. Charger les mondes EN PREMIER pour que Bukkit.getWorld() soit disponible
        worldManager = new WorldManager(this);
        worldManager.loadAllSavedWorlds();

        // 2. Seulement après, charger le spawn (le monde est maintenant disponible)
        spawnManager    = new SpawnManager(this);
        teleportManager = new TeleportManager(this);

        // Commandes
        getCommand("world").setExecutor(new WorldCommand(worldManager));
        getCommand("world").setTabCompleter(new WorldCommand(worldManager));
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager, teleportManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));

        // Listeners
        getServer().getPluginManager().registerEvents(
                new SpawnListener(this, spawnManager, teleportManager), this);

        getLogger().info("Armesia-World activé avec succès !");
    }

    @Override
    public void onDisable() {
        getLogger().info("Armesia-World désactivé.");
    }

    public WorldManager    getWorldManager()    { return worldManager;    }
    public SpawnManager    getSpawnManager()    { return spawnManager;    }
    public TeleportManager getTeleportManager() { return teleportManager; }
}


