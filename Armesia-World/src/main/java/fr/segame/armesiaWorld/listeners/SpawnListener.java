package fr.segame.armesiaWorld.listeners;

import fr.segame.armesiaWorld.MainWorld;
import fr.segame.armesiaWorld.SpawnManager;
import fr.segame.armesiaWorld.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnListener implements Listener {

    private final MainWorld plugin;
    private final SpawnManager spawnManager;
    private final TeleportManager teleportManager;

    public SpawnListener(MainWorld plugin, SpawnManager spawnManager, TeleportManager teleportManager) {
        this.plugin          = plugin;
        this.spawnManager    = spawnManager;
        this.teleportManager = teleportManager;
    }

    /** Connexion → tp direct au spawn après 1 tick. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!spawnManager.hasSpawn()) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) spawnManager.teleportToSpawn(player);
        }, 1L);
    }

    /** Mort → respawn au spawn global (sans compte à rebours). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!spawnManager.hasSpawn()) return;
        event.setRespawnLocation(spawnManager.getSpawn());
    }

    /** Mouvement → annule le compte à rebours si le joueur a bougé. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!teleportManager.hasPending(player.getUniqueId())) return;

        Location to    = event.getTo();
        Location start = teleportManager.getStartLocation(player.getUniqueId());
        if (to == null || start == null) return;

        if (Math.abs(to.getX() - start.getX()) > 0.15
                || Math.abs(to.getY() - start.getY()) > 0.2
                || Math.abs(to.getZ() - start.getZ()) > 0.15) {
            teleportManager.cancelDueToMovement(player);
        }
    }

    /** Déconnexion → nettoyage de la tâche en attente. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        teleportManager.cleanupOnQuit(event.getPlayer().getUniqueId());
    }
}
