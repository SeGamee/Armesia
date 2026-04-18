package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    private ReloadListener reloadListener;
    private TrailManager trailManager;
    private ImpactManager impactManager;
    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // 🔥 Managers
        reloadListener = new ReloadListener();
        trailManager = new TrailManager();
        impactManager = new ImpactManager();
        zoneManager = new ZoneManager();

        // 🔥 Events
        getServer().getPluginManager().registerEvents(reloadListener, this);
        getServer().getPluginManager().registerEvents(new ShootListener(), this);
        getServer().getPluginManager().registerEvents(new AntiDropFixListener(), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(), this);

        getLogger().info("Armesia CrackShot Addon activé !");
    }

    public static Main getInstance() {
        return instance;
    }

    public ReloadListener getReloadListener() {
        return reloadListener;
    }

    public TrailManager getTrailManager() {
        return trailManager;
    }

    public ImpactManager getImpactManager() { return impactManager; }

    public ZoneManager getZoneManager() { return zoneManager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("armesiacsa"))
            return false;

        if (args.length == 0) {
            sender.sendMessage("§c/armesiacsa reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            // 🔄 reload config plugin
            reloadConfig();

            // 🧠 clear cache armes (important)
            reloadListener.clearCache();

            sender.sendMessage("§a✔ Armesia CrackShot Addon rechargé !");
            return true;
        }

        return true;
    }
}