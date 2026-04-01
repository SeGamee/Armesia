package fr.segame.armesiaScoreboard;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainScoreboard extends JavaPlugin implements Listener {

    private ScoreboardManager scoreboardManager;
    private int taskId;

    @Override
    public void onEnable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Armesia");

        if (!(plugin instanceof Main core)) {
            getLogger().severe("Armesia non trouvé !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        scoreboardManager = new ScoreboardManager(core);

        // Enregistrer le callback dans Main : évite toute dépendance de compilation inverse
        Main.registerScoreboardUpdateCallback(scoreboardManager::updatePlayer);

        // Initialiser le scoreboard des joueurs déjà en ligne (reload à chaud)
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboardManager.init(player);
        }

        // 🔥 Refresh périodique toutes les minutes (1200 ticks)
        // Les mises à jour immédiates se font via updatePlayerScoreboard() sur événements
        taskId = Bukkit.getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(scoreboardManager::update),
                0L, 1200L
        ).getTaskId();

        Bukkit.getPluginManager().registerEvents(this, this);


    }

    @Override
    public void onDisable() {
        Main.registerScoreboardUpdateCallback(null); // désenregistrer le callback
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // runTaskLater pour laisser Armesia charger le joueur en premier
        Bukkit.getScheduler().runTaskLater(this, () ->
                scoreboardManager.init(event.getPlayer()), 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardManager.remove(event.getPlayer());
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}