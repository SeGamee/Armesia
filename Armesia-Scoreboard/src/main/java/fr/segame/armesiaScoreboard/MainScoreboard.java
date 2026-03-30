package fr.segame.armesiaScoreboard;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainScoreboard extends JavaPlugin {

    private Main core;
    private ScoreboardManager scoreboardManager;
    private int taskId;

    @Override
    public void onEnable() {

        Plugin plugin = Bukkit.getPluginManager().getPlugin("Armesia");

        if (!(plugin instanceof Main)) {
            getLogger().severe("Armesia non trouvé !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        core = (Main) plugin;
        scoreboardManager = new ScoreboardManager(core);

        // 🔥 Update automatique
        taskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getOnlinePlayers().forEach(scoreboardManager::update);
        }, 0L, 20L).getTaskId();

        getLogger().info("Scoreboard connecté à Armesia !");
    }

    @Override
    public void onDisable() {
        // 🔥 évite les tasks fantômes
        Bukkit.getScheduler().cancelTask(taskId);
    }
}