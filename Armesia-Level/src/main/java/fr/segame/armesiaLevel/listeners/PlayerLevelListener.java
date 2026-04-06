package fr.segame.armesiaLevel.listeners;

import fr.segame.armesiaLevel.player.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLevelListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerLevelListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    /** Priorité MONITOR : s'exécute après les autres plugins, données déjà disponibles. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayer(event.getPlayer());
    }
}

